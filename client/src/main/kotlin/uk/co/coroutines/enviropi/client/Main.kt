@file:OptIn(ExperimentalTime::class)

package uk.co.coroutines.enviropi.client

import com.diozero.util.Diozero
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.tinylog.kotlin.Logger.error
import org.tinylog.kotlin.Logger.warn
import uk.co.coroutines.enviropi.client.st7735.ST7735
import java.awt.Color
import java.awt.Color.BLUE
import java.awt.Color.WHITE
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_USHORT_565_RGB
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.ExperimentalTime

fun main(): Unit = runBlocking {
    try {
        ST7735().use { EnviroSensor.dataFlow.outputTo(it) }
    } catch (t: Throwable) {
        warn(t)
    } finally {
        Diozero.shutdown()
    }
}
