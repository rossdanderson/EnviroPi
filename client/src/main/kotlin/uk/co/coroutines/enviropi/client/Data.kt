package uk.co.coroutines.enviropi.client

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.tinylog.kotlin.Logger
import java.awt.Color.*
import java.awt.LinearGradientPaint
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_USHORT_565_RGB
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.nanoseconds


data class Data(
    val lux: Double,
    val temperature: Double,
    val pressure: Double,
    val humidity: Double,
)

val exampleData = flow {
    var lux = 1000.0
    var temperature = 20.0
    var pressure = 120.0
    var humidity = 60.0
    while (true) {
        lux += Random.nextDouble(-10.0, 10.0)

        temperature += Random.nextDouble(-4.0, 4.0)
        temperature = temperature.coerceAtMost(40.0).coerceAtLeast(0.0)

        pressure += Random.nextDouble(-10.0, 10.0)

        humidity += Random.nextDouble(-2.0, 2.0)
        humidity = humidity.coerceAtMost(90.0).coerceAtLeast(10.0)
        emit(
            Data(
                lux,
                temperature,
                pressure,
                humidity
            )
        )
        delay(100)
    }
}

val exampleData2 = flow {
    var lux = 1000.0
    var temperature = 0.0
    var pressure = 120.0
    var humidity = 60.0
    (0..20).forEach {
        lux += Random.nextDouble(-10.0, 10.0)

        pressure += Random.nextDouble(-10.0, 10.0)

        humidity += Random.nextDouble(-2.0, 2.0)
        humidity = humidity.coerceAtMost(90.0).coerceAtLeast(10.0)
        emit(
            Data(
                lux,
                temperature++,
                pressure,
                humidity
            )
        )
    }
    awaitCancellation()
}

suspend fun Flow<Data>.outputTo(display: IDisplay, debug: Boolean = false) {
    fun flip(n: Double): Double = display.height - n

    val halfHeight = display.height / 2

    runningFold(listOf<Data>()) { acc, data -> (acc + data).takeLast(display.width) }
        .filter { it.size >= 2 }
        .onEach { dataPoints ->
            val temperatures = dataPoints.map(Data::temperature)
            val min = temperatures.min()
            val max = temperatures.max()
            val mid = (max + min) / 2
            val degreePx = display.height.toDouble() / (max - min).coerceAtLeast(1.0)

            val stepPx = display.width.toDouble() / (temperatures.size - 1)

            val imageHeight = if (debug) 500 else display.height
            val imageWidth = if (debug) 500 else display.width
            display.display(
                BufferedImage(imageHeight, 500, TYPE_USHORT_565_RGB).apply {
                    createGraphics().run {
                        if (debug) {
                            transform = AffineTransform().apply {
                                translate(imageWidth.toDouble() / 2 - display.width / 2, imageHeight.toDouble() / 2 - display.height / 2)
                            }
                            color = WHITE
                            drawRect(-1, -1, display.width + 2, display.height + 2)
                        }

                        val colors = arrayOf(RED, YELLOW, GREEN, BLUE, WHITE)
                        val fractions = floatArrayOf(0f, 0.25f, 0.5f, 0.650f, 1f)

                        val midLine = flip(mid * degreePx)
                        val shift = -(midLine - halfHeight)

                        fun degreeToPixel(n: Double): Int = (flip(n * degreePx) + shift).roundToInt()

                        val gradientBottom = degreeToPixel(0.0)
                        val gradientTop = degreeToPixel(40.0)

                        setPaint(
                            LinearGradientPaint(
                                0f,
                                gradientTop.toFloat(),
                                0f,
                                gradientBottom.toFloat(),
                                fractions,
                                colors
                            )
                        )
                        drawLine(5, gradientBottom, 5, gradientTop)

                        drawLine(0, degreeToPixel(mid), display.width, degreeToPixel(mid))

                        temperatures.singleOrNull()
                            ?.let {
                                drawLine(
                                    0,
                                    (it * degreePx).roundToInt(),
                                    display.width,
                                    (it * degreePx).roundToInt()
                                )
                            }
                            ?: temperatures
                                .windowed(2, 1)
                                .forEachIndexed { index, (y1, y2) ->
                                    val x1 = (stepPx * index).roundToInt()
                                    val x2 = (x1 + stepPx).roundToInt()
                                    drawLine(
                                        x1,
                                        degreeToPixel(y1),
                                        x2,
                                        degreeToPixel(y2)
                                    )
                                }

                        color = MAGENTA
                        drawString("%.2f".format(max), 5, 10)
                        drawString("%.2f".format(min), 5, display.height - 5)
                        color = WHITE
                        drawString(
                            "%.2f".format(temperatures.last()),
                            display.width - 30,
                            display.height / 2 + 10
                        )
                        val time = Clock.System.now()
                            .let { it - it.nanosecondsOfSecond.nanoseconds }
                            .toLocalDateTime(TimeZone.currentSystemDefault())
                            .time
                        drawString(time.toString(), display.width - 47, 10)
                        dispose()
                    }
                }
            )
        }
        .onCompletion { if (it != null) Logger.error(it) }
        .collect()
}
