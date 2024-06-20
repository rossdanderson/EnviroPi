@file:OptIn(ExperimentalTime::class)

package uk.co.coroutines.enviropi.client

import com.diozero.devices.BME280
import com.diozero.util.Diozero
import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import org.tinylog.kotlin.Logger.info
import org.tinylog.kotlin.Logger.warn
import uk.co.coroutines.enviropi.client.ltr559.LTR559
import uk.co.coroutines.enviropi.common.Sample
import uk.co.coroutines.enviropi.common.jsonConfig
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = runBlocking {

    val server = args[0]
    info { "Publishing to $server:443" }
    try {
        val client = HttpClient {
            expectSuccess = true
            install(Logging)
            install(ContentNegotiation) { json(jsonConfig) }
            install(HttpTimeout) {
                requestTimeoutMillis = 10.seconds.inWholeMilliseconds
            }
        }

        LTR559().use { ltr559 ->
            BME280().use { bme280 ->
                while (!bme280.isDataAvailable || !ltr559.dataAvailable) { delay(100) }

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

                    runCatching {
                        client.post {
                            url("$server:443")
                            contentType(Json)
                            setBody(
                                Sample(
                                    time = Clock.System.now(),
                                    temperature = temperature,
                                    pressure = pressure,
                                    humidity = humidity,
                                    lux = lux,
                                )
                            )
                        }
                    }.onFailure { warn(it) { "Unable to publish data" } }

                    val end = Clock.System.now()
                    delay(1.seconds - (end - start))
                }
            }
        }
    } catch (t: Throwable) {
        warn(t)
    } finally {
        // Required if there are non-daemon threads that will prevent the
        // built-in clean-up routines from running
        Diozero.shutdown()
    }
}
