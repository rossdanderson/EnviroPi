package uk.co.coroutines.enviropi.client

import com.diozero.util.Diozero
import kotlinx.coroutines.runBlocking
import org.tinylog.kotlin.Logger.warn
import uk.co.coroutines.enviropi.client.st7735.ST7735

fun main(): Unit = runBlocking {
    try {
        ST7735().use { EnviroSensor.dataFlow.outputTo(display = it) }
    } catch (t: Throwable) {
        warn(t)
    } finally {
        Diozero.shutdown()
    }
}
