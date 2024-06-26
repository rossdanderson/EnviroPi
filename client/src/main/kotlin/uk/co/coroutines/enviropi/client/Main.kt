@file:OptIn(ExperimentalTime::class)

package uk.co.coroutines.enviropi.client

import com.diozero.devices.BMx280
import com.diozero.util.Diozero
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.tinylog.kotlin.Logger.error
import org.tinylog.kotlin.Logger.info
import org.tinylog.kotlin.Logger.warn
import uk.co.coroutines.enviropi.client.ltr559.LTR559
import uk.co.coroutines.enviropi.client.st7735.ST7735
import uk.co.coroutines.enviropi.client.st7735.createTest
import java.awt.Color
import java.awt.Color.BLUE
import java.awt.Color.WHITE
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_USHORT_565_RGB
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

data class Data(
    val lux: Double,
    val temperature: Double,
    val pressure: Double,
    val humidity: Double,
)


fun main(args: Array<String>): Unit = runBlocking {
    try {
        ST7735().use { st7735 ->
            val halfHeight = st7735.height / 2

            val dataFlow = MutableSharedFlow<Data>()

            dataFlow
                .runningFold(listOf<Data>()) { acc, data -> (acc + data).take(st7735.width) }
//                .sample(5.seconds)
                .onEach { dataPoints ->
                    if (dataPoints.isEmpty()) return@onEach

                    val temperatures = dataPoints.map { it.temperature }
                    val average = temperatures.average()
                    val min = temperatures.minOrNull() ?: 0.0
                    val max = temperatures.maxOrNull() ?: 0.0
                    val diff = (max - min).coerceAtLeast(5.0)
                    val halfDiff = diff / 2
                    val low = minOf(average - halfDiff, min)
                    val high = maxOf(average + halfDiff, max)
                    val yStep = st7735.height.toDouble() / (high - low)
                    val xStep = st7735.width.toDouble() / temperatures.size

                    println("Low: $low - High: $high - Diff: $diff - yStep: $yStep - xStep: $xStep")

                    st7735.display(
                        BufferedImage(st7735.width, st7735.height, TYPE_USHORT_565_RGB).apply {
                            createGraphics().run {
                                color = Color.GREEN

                                val yHeights = temperatures.map { point -> -((point - average) * yStep).roundToInt() + halfHeight }

                                yHeights.singleOrNull()?.let { drawLine(0, it, st7735.width, it) }
                                    ?: yHeights
                                        .windowed(2, 1)
                                        .forEachIndexed { index, (y1, y2) ->
                                            drawLine((xStep * index).roundToInt(), y1, (xStep * (index + 1)).roundToInt(), y2)
                                        }

                                color = BLUE
                                drawString("%.2f".format(high), 5,  10)
                                drawString("%.2f".format(low), 5, st7735.height - 5)
                                color = WHITE
                                drawString("%.2f".format(temperatures.last()), st7735.width - 40, st7735.height / 2 + 10)
                                val time = Clock.System.now()
                                    .let { it - it.nanosecondsOfSecond.nanoseconds }
                                    .toLocalDateTime(TimeZone.currentSystemDefault())
                                    .time
                                drawString(time.toString(), st7735.width - 60, 10)
                                dispose()
                            }
                        }
                    )
                }
                .onCompletion { if (it != null) error(it) }
                .launchIn(GlobalScope)


            with(st7735) {
                display(createTest(width, height))
            }

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

                        dataFlow.emit(Data(lux, temperature, pressure, humidity))

                        val end = Clock.System.now()
                        delay(1.seconds - (end - start))
                    }
                }
            }
        }
    } catch (t: Throwable) {
        warn(t)
    } finally {
        Diozero.shutdown()
    }
}
