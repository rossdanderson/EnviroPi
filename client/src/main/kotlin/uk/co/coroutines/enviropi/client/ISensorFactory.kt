package uk.co.coroutines.enviropi.client

import com.diozero.devices.BMx280
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.plus
import kotlinx.datetime.Clock
import org.tinylog.kotlin.Logger.info
import uk.co.coroutines.enviropi.client.ltr559.LTR559
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface ISensorFactory {
  suspend fun CoroutineScope.create(sampleDelay: Duration): ISensor

  val isDiozero: Boolean

  companion object {
    val default = object : ISensorFactory {
      override suspend fun CoroutineScope.create(sampleDelay: Duration): ISensor {
        val sensorJob = SupervisorJob(coroutineContext[Job])
        val sensorScope = CoroutineScope(coroutineContext + Dispatchers.IO + sensorJob)

        val dataFlow =
            flow {
              LTR559().use { ltr559 ->
                BMx280.I2CBuilder.builder(1).build().use { bme280 ->
                  while (!bme280.isDataAvailable || !ltr559.dataAvailable) {
                    delay(10.milliseconds)
                  }

                  while (currentCoroutineContext().isActive) {
                    val lux = ltr559.getLux()
                    val (temperature, pressure, humidity) = bme280.values.map(Float::toDouble)

                    info {
                      "Lux: {0.##}. Temperature: {0.##} C. Pressure: {0.##} hPa. Relative Humidity: {0.##}% RH"
                          .format(lux, temperature, pressure, humidity)
                    }

                    val end = Clock.System.now()
                    emit(Data(lux, temperature, pressure, humidity, end))
                    delay(sampleDelay)
                  }
                }
              }
            }
                .stateIn(sensorScope)

        return object : ISensor {
          override val dataFlow = dataFlow

          override fun close() {
            sensorJob.cancel()
          }
        }
      }

      override val isDiozero: Boolean = true
    }

    val mock = object : ISensorFactory {
      override suspend fun CoroutineScope.create(sampleDelay: Duration): ISensor {
        val sensorJob = SupervisorJob(coroutineContext[Job])
        val dataFlow =
            flow {
              var lux = 1000.0
              var temperature = 20.0
              var pressure = 120.0
              var humidity = 60.0
              while (true) {
                lux += Random.nextDouble(-10.0, 10.0)

                temperature += Random.nextDouble(-4.0, 4.0)
                temperature = temperature.coerceAtMost(40.0).coerceAtLeast(0.0)

                pressure += Random.nextDouble(-10.0, 10.0)

                humidity += Random.nextDouble(-2.0, 2.0)
                humidity = humidity.coerceAtMost(90.0).coerceAtLeast(10.0)
                emit(
                    Data(
                        lux,
                        temperature,
                        pressure,
                        humidity,
                        Clock.System.now(),
                    )
                )
              }
            }
                .stateIn(this + sensorJob)

        return object : ISensor {
          override val dataFlow: StateFlow<Data> = dataFlow

          override fun close() {
            sensorJob.cancel()
          }
        }
      }

      override val isDiozero: Boolean = false
    }
  }
}
