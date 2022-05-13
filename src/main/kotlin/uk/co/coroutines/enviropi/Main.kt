@file:OptIn(ExperimentalTime::class)

package uk.co.coroutines.enviropi

import com.diozero.devices.BME280
import com.diozero.util.Diozero
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.tinylog.Logger
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

fun main(): Unit = runBlocking {
    try {
        BME280()
            .use { bme280 ->
                while (true) {
                    Logger.info("Sampling", "")
                    bme280.values
                        .map(Float::toDouble)
                        .let { (temperature, pressure, humidity) ->
                            Logger.info(
                                "Temperature: {0.##} C. Pressure: {0.##} hPa. Relative Humidity: {0.##}% RH",
                                temperature,
                                pressure,
                                humidity
                            )
                        }
                    delay(1.seconds)
                }
            }
    } catch (t: Throwable) {
        Logger.warn(t)
    } finally {
        // Required if there are non-daemon threads that will prevent the
        // built-in clean-up routines from running
        Diozero.shutdown()
    }
}
