package uk.co.coroutines.enviropi.client

import com.diozero.devices.BMx280
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.datetime.Clock
import org.tinylog.kotlin.Logger.info
import uk.co.coroutines.enviropi.client.ltr559.LTR559

object EnviroSensor {
    val dataFlow = flow {
        LTR559().use { ltr559 ->
            BMx280.I2CBuilder.builder(1).build().use { bme280 ->
                while (!bme280.isDataAvailable || !ltr559.dataAvailable) {
                    delay(100)
                }

                while (true) {
                    val start = Clock.System.now()

                    val lux = ltr559.getLux()
                    val (temperature, pressure, humidity) = bme280.values.map(Float::toDouble)

                    info(
                        "Lux: {0.##}. Temperature: {0.##} C. Pressure: {0.##} hPa. Relative Humidity: {0.##}% RH",
                        lux,
                        temperature,
                        pressure,
                        humidity
                    )

                    val end = Clock.System.now()
                    emit(Data(lux, temperature, pressure, humidity, end))
                }
            }
        }
    }.flowOn(IO)
}
