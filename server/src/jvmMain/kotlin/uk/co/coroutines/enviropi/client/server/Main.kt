@file:OptIn(DelicateCoroutinesApi::class, FlowPreview::class)

package uk.co.coroutines.enviropi.client.server

import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uk.co.coroutines.enviropi.common.Sample
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Serializable
data class State(
    val temperature: DoubleStateValue,
    val pressure: DoubleStateValue,
    val humidity: DoubleStateValue,
)

@Serializable
data class Info(
    val lastSample: Sample,
    val currentState: State,
)

@Serializable
data class DoubleStateValue(
    val lowValue: Double,
    val lowTime: LocalDateTime,
    val highValue: Double,
    val highTime: LocalDateTime,
) {
    constructor(value: Double, time: LocalDateTime) : this(value, time, value, time)

    fun compareAndSwap(value: Double, time: LocalDateTime): DoubleStateValue =
        when {
            value < this.lowValue -> copy(lowValue = value, lowTime = time)
            value > this.highValue -> copy(highValue = value, highTime = time)
            else -> this
        }
}

private val sampleFlow = MutableSharedFlow<Sample>()

private val runningState = sampleFlow
    .runningFold(null) { state: State?, sample: Sample ->
        val sampleTime = sample.time.toLocalDateTime(TimeZone.currentSystemDefault())
        if (state == null) State(
            temperature = DoubleStateValue(sample.temperature, sampleTime),
            pressure = DoubleStateValue(sample.pressure, sampleTime),
            humidity = DoubleStateValue(sample.humidity, sampleTime),
        )
        else State(
            temperature = state.temperature.compareAndSwap(sample.temperature, sampleTime),
            pressure = state.pressure.compareAndSwap(sample.pressure, sampleTime),
            humidity = state.humidity.compareAndSwap(sample.humidity, sampleTime),
        )
    }
    .stateIn(GlobalScope, started = Eagerly, initialValue = null)

fun main() {
    combine(
        sampleFlow,
        runningState.filterNotNull()
    ) { sample, state -> sample to state }
        .debounce(100.milliseconds)
        .onEach { (sample, state) -> println(Info(sample, state)) }
        .launchIn(GlobalScope)

    embeddedServer(CIO, port = 8989) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
            })
        }
        routing {
            post("/") {
                val receive = call.receive<Sample>()
                sampleFlow.emit(receive)
                call.response.status(OK)
            }
            get {
                withTimeoutOrNull(1.seconds) {
                    call.respond(
                        Info(
                            sampleFlow.first(),
                            runningState.filterNotNull().first()
                        )
                    )
                } ?: call.respond("Not yet available")
            }
        }
    }.start(wait = true)
}
