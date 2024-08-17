package uk.co.coroutines.enviropi.client

import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.tinylog.kotlin.Logger
import uk.co.coroutines.enviropi.client.Resources.colors
import uk.co.coroutines.enviropi.client.Resources.fractions
import java.awt.BasicStroke
import java.awt.Color.decode
import java.awt.LinearGradientPaint
import java.awt.RenderingHints.KEY_ANTIALIASING
import java.awt.RenderingHints.VALUE_ANTIALIAS_OFF
import java.awt.RenderingHints.VALUE_ANTIALIAS_ON
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_USHORT_565_RGB
import kotlin.collections.first
import kotlin.collections.last
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

data class Point2D(
    val x: Double,
    val y: Double
) {
    operator fun get(i: Int) = when (i) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException()
    }
}

data class Data(
    val lux: Double,
    val temperature: Double,
    val pressure: Double,
    val humidity: Double,
    val instant: Instant,
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
                humidity,
                Clock.System.now(),
            )
        )
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
                humidity,
                Clock.System.now(),
            )
        )
    }
    awaitCancellation()
}

suspend fun Flow<Data>.outputTo(
    display: IDisplay,
    debug: Boolean = false,
    imageHeight: Int = display.height,
    imageWidth: Int = display.width
) {
    fun flip(n: Double): Double = display.height - n
    val halfHeight = display.height / 2

    val time = flow {
        while (true) {
            emit(
                Clock.System.now()
                    .let { it - it.nanosecondsOfSecond.nanoseconds }
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .time
            )
            delay(1000)
        }
    }

    val sampleTime = 250.milliseconds
    val historyTime = 1.days

    combine(
        time,
        runningFold(listOf<Data>()) { history, data ->
            println("${history.size} of ${(historyTime / sampleTime).toLong()}")
            history.dropWhile { data.instant - it.instant >= historyTime } + data
        }
            .filter { it.size >= 2 }
            .map { samples ->
                val take = minOf(samples.size, display.width)
                val step = samples.size.toDouble() / take
                (0 until take).map { i ->
                    val groupStart = (step * (i)).roundToInt().coerceAtMost(samples.size - 1)
                    val groupEnd = (step * (i + 1)).roundToInt().coerceAtMost(samples.size - 1)

                    samples.slice(groupStart..groupEnd)
                        .map { it.temperature }
                        .average()
                }
            }
            .transform {
                emit(it)
                delay(sampleTime)
            }
    ) { time, temperatures -> time to temperatures }
        .onEach { (time, temperatures) ->
            val min = temperatures.min()
            val max = temperatures.max()
            val mid = (max + min) / 2
            val degreePx = display.height.toDouble() / (max - min).coerceAtLeast(1.0)

            val stepPx = display.width.toDouble() / (temperatures.size - 1)

            display.display(
                BufferedImage(imageWidth, imageHeight, TYPE_USHORT_565_RGB).apply {
                    createGraphics().run {
                        setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
                        color = decode("#1D2B53")
                        fillRect(0, 0, display.width, display.height)

                        if (debug) {
                            transform = AffineTransform().apply {
                                translate(
                                    imageWidth.toDouble() / 2 - display.width / 2,
                                    imageHeight.toDouble() / 2 - display.height / 2
                                )
                            }
                            color = Resources.white
                            drawRect(-1, -1, display.width + 2, display.height + 2)
                        }

                        val midLine = flip(mid * degreePx)
                        val shift = -(midLine - halfHeight)

                        fun degreeToPixel(n: Double): Double = (flip(n * degreePx) + shift)

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

                        if (debug) drawLine(5, gradientBottom.roundToInt(), 5, gradientTop.roundToInt())

                        stroke = BasicStroke(3f)
                        val path = Path2D.Float()

                        val points = temperatures
                            .flatMapIndexed { index, y ->
                                listOf(Point2D(stepPx * index, degreeToPixel(y)))
                            }
                        path.moveTo(points.first()[0], points.first()[1])

                        for (i in 1 until points.size) {
                            val x0 = points[i - 1][0]
                            val y0 = points[i - 1][1]
                            val x1 = points[i][0]
                            val y1 = points[i][1]

                            val midX = (x0 + x1) / 2
                            val midY = (y0 + y1) / 2

                            path.quadTo(x0, y0, midX, midY)
                        }

                        // Connect to the last point
                        path.lineTo(points.last()[0], points.last()[1])

                        draw(path)

                        stroke = BasicStroke(1f)

                        if (debug) {
                            color = Resources.white
                            drawLine(0, degreeToPixel(mid).roundToInt(), display.width, degreeToPixel(mid).roundToInt())
                        }

                        setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_OFF)
                        font = Resources.font.deriveFont(12f)
                        color = Resources.peach
                        drawString("⬆%04.1f".format(max), 2, 12)
                        drawString("⬇%04.1f".format(min), 2, display.height - 2)
                        drawString(
                            "%04.1f∧".format(temperatures.last()),
                            display.width - 47,
                            display.height - 2
                        )
                        drawString(time.toString(), display.width - 63, 12)
                        dispose()
                    }
                }
            )
        }
        .onCompletion { if (it != null) Logger.error(it) }
        .collect()
}
