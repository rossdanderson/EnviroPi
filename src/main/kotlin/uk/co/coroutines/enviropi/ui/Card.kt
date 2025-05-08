package uk.co.coroutines.enviropi.ui

import kotlin.time.Duration
import kotlinx.datetime.Instant
import org.jetbrains.kotlinx.dataframe.DataColumn
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.aggregate
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.groupBy
import org.jetbrains.kotlinx.dataframe.api.last
import org.jetbrains.kotlinx.dataframe.api.max
import org.jetbrains.kotlinx.dataframe.api.min
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.toSVG
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.line

data class Card(
    val title: String,
    val colour: String,
    val current: String,
    val min: String,
    val max: String,
    val unit: String,
    val icon: String,
    val plot: String,
) {
  companion object {
    operator fun invoke(
      icon: String,
      colour: String,
      title: String,
      unit: String,
      df: DataFrame<*>,
      column: DataColumn<Double>
    ): Any {
      val svg =
          df.groupBy { get("timeKey") }
              .aggregate { get(column).first() into "open" }
              .plot {
                line {
                  x("timeKey")
                  y("open")
                }
                layout {
                  size = 214 to 100
                  style {
                    global.line { blank = true }
                    blankAxes()
                  }
                }
              }
              .toSVG()

      val current = "%.2f".format(df.last()[column])
      val min = "%.2f".format(df.min(column))
      val max = "%.2f".format(df.max(column))
      return Card(title, colour, current, min, max, unit, icon, svg)
    }
  }
}

fun Instant.roundDownTo(duration: Duration): Instant {
  val durationMillis = duration.inWholeMilliseconds
  return if (durationMillis == 0L) this
  else {
    val epochMillis = toEpochMilliseconds()
    if (epochMillis % durationMillis == 0L) this
    else Instant.fromEpochMilliseconds(epochMillis - epochMillis % durationMillis)
  }
}
