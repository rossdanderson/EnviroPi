@file:OptIn(ExperimentalTime::class)

package uk.co.coroutines.enviropi.client

import com.diozero.devices.BME280
import com.diozero.util.Diozero
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.http.ContentType.Application.Json
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.serialization.json.Json
import org.tinylog.Logger
import uk.co.coroutines.enviropi.common.Sample
import uk.co.coroutines.enviropi.common.serverHost
import uk.co.coroutines.enviropi.common.serverPort
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

fun main(): Unit = runBlocking {

    try {
        val client = HttpClient {
            expectSuccess = true
            install(Logging)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
        }

        BME280()
            .use { bme280 ->
                while (true) {
                    val start = Clock.System.now()
                    bme280.values
                        .map(Float::toDouble)
                        .let { (temperature, pressure, humidity) ->
                            Logger.info(
                                "Temperature: {0.##} C. Pressure: {0.##} hPa. Relative Humidity: {0.##}% RH",
                                temperature,
                                pressure,
                                humidity
                            )

                            runCatching {
                                client.post {
                                    url {
                                        host = serverHost
                                        port = serverPort
                                        path("/")
                                    }
                                    contentType(Json)
                                    setBody(
                                        Sample(
                                            time = Clock.System.now(),
                                            temperature = temperature,
                                            pressure = pressure,
                                            humidity = humidity
                                        )
                                    )
                                }
                            }.onFailure { Logger.warn(it) { "Unable to publish data" } }
                        }

                    val end = Clock.System.now()
                    delay(1.seconds - (end - start))
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
