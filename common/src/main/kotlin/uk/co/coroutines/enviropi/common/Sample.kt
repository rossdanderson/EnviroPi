package uk.co.coroutines.enviropi.common

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Sample(
    val time: Instant,
    val temperature: Double, // Degrees C
    val pressure: Double, // hPa
    val humidity: Double, // Relative humidity %
    val lux: Double,
)
