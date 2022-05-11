@file:OptIn(ExperimentalTime::class, FlowPreview::class)

package uk.co.coroutines.enviropi

import com.diozero.devices.BME280
import com.diozero.util.Diozero
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import org.tinylog.Logger
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

data class SensorData(
    val temperature: Double,
    val pressure: Double,
    val humidity: Double,
)

fun main(): Unit = runBlocking {
    try {
        LTR559()
        combine(
            flow {
                BME280().use { bme280 ->
                    while (true) {
                        bme280.values
                            .map(Float::toDouble)
                            .also { (temperature, pressure, humidity) ->
                                emit(SensorData(temperature, pressure, humidity))
                            }
                    }
                }
            },
            // Light sensor,
            // Noise sensor,
        ) { it[0] }
            .sample(30.seconds)
            .onEach { sensorData ->
                Logger.info(
                    "Temperature: {0.##} C. Pressure: {0.##} hPa. Relative Humidity: {0.##}% RH",
                    sensorData.temperature,
                    sensorData.pressure,
                    sensorData.humidity
                )
            }
            .launchIn(this)
    } finally {
        // Required if there are non-daemon threads that will prevent the
        // built-in clean-up routines from running
        Diozero.shutdown()
    }
}
