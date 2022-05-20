@file:OptIn(DelicateCoroutinesApi::class, FlowPreview::class)

package uk.co.coroutines.enviropi.server

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.metrics.micrometer.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheus.PrometheusConfig
import io.micrometer.prometheus.PrometheusMeterRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import uk.co.coroutines.enviropi.common.Sample
import uk.co.coroutines.enviropi.common.jsonConfig
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Serializable
data class State(
    val samples: Int,
    val temperature: DoubleStateValue,
    val pressure: DoubleStateValue,
    val humidity: DoubleStateValue,
)

@Serializable
data class Info(
    val lastSample: Sample,
    val runningState: State,
)

@Serializable
data class DoubleStateValue(
    val lowValue: Double,
    val lowTime: LocalDateTime,
    val highValue: Double,
    val highTime: LocalDateTime,
) {
    constructor(value: Double, time: LocalDateTime) : this(value, time, value, time)

    fun update(value: Double, time: LocalDateTime): DoubleStateValue =
        when {
            value < this.lowValue -> copy(lowValue = value, lowTime = time)
            value > this.highValue -> copy(highValue = value, highTime = time)
            else -> this
        }
}

private val sampleFlow = MutableSharedFlow<Sample>()

private val infoState = sampleFlow
    .runningFold(null) { accumulator: Info?, sample: Sample ->
        val sampleTime = sample.time.toLocalDateTime(TimeZone.currentSystemDefault())
        val state = when (val state = accumulator?.runningState) {
            null -> State(
                samples = 0,
                temperature = DoubleStateValue(sample.temperature, sampleTime),
                pressure = DoubleStateValue(sample.pressure, sampleTime),
                humidity = DoubleStateValue(sample.humidity, sampleTime),
            )
            else -> State(
                samples = state.samples + 1,
                temperature = state.temperature.update(sample.temperature, sampleTime),
                pressure = state.pressure.update(sample.pressure, sampleTime),
                humidity = state.humidity.update(sample.humidity, sampleTime),
            )
        }
        Info(sample, state)
    }
    .stateIn(GlobalScope, started = Eagerly, initialValue = null)

fun main(): Unit = runBlocking {
    infoState.filterNotNull()
        .sample(10.seconds)
        .onEach { info -> println(jsonConfig.encodeToString(info)) }
        .launchIn(GlobalScope)

    val appMicrometerRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    launch {
        infoState.filterNotNull().first()
        appMicrometerRegistry.gauge("enviropi_temperature", infoState) { info ->
            info.value?.lastSample
                ?.takeIf { sample -> (Clock.System.now() - sample.time) < 10.minutes }!!
                .temperature
        }
        appMicrometerRegistry.gauge("enviropi_humidity", infoState) { info ->
            info.value?.lastSample
                ?.takeIf { sample -> (Clock.System.now() - sample.time) < 10.minutes }!!
                .humidity
        }
        appMicrometerRegistry.gauge("enviropi_pressure", infoState) { info ->
            info.value?.lastSample
                ?.takeIf { sample -> (Clock.System.now() - sample.time) < 10.minutes }!!
                .pressure
        }
        appMicrometerRegistry.gauge("enviropi_light", infoState) { info ->
            info.value?.lastSample
                ?.takeIf { sample -> (Clock.System.now() - sample.time) < 10.minutes }!!
                .lux
        }
    }

    embeddedServer(CIO, port = 8080) {
        install(MicrometerMetrics) { registry = appMicrometerRegistry }
        install(CORS)
        install(ContentNegotiation) { json(jsonConfig) }
        routing {
            post("/") {
                val receive = call.receive<Sample>()
                sampleFlow.emit(receive)
                call.response.status(OK)
            }
            get {
                infoState.value?.let { call.respondText(jsonConfig.encodeToString(it)) }
                    ?: call.respond(ServiceUnavailable, "Data not yet available")
            }
            get("/api") {
                infoState.value?.let { call.respond(it) }
                    ?: call.respond(ServiceUnavailable, "Data not yet available")
            }
        }
    }.start()

    embeddedServer(CIO, port = 9091) {
        routing {
            get("/metrics") {
                call.respond(appMicrometerRegistry.scrape())
            }
        }
    }.start(wait = true)
}
