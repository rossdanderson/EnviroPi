package uk.co.coroutines.enviropi.ui

import kotlin.time.Duration
import kotlinx.datetime.Instant
import org.jetbrains.kotlinx.dataframe.api.aggregate
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.expr
import org.jetbrains.kotlinx.dataframe.api.first
import org.jetbrains.kotlinx.dataframe.api.groupBy
import org.jetbrains.kotlinx.dataframe.api.last
import org.jetbrains.kotlinx.dataframe.api.max
import org.jetbrains.kotlinx.dataframe.api.mean
import org.jetbrains.kotlinx.dataframe.api.min
import org.jetbrains.kotlinx.dataframe.api.toColumn
import org.jetbrains.kotlinx.kandy.dsl.plot
import org.jetbrains.kotlinx.kandy.letsplot.export.toSVG
import org.jetbrains.kotlinx.kandy.letsplot.feature.layout
import org.jetbrains.kotlinx.kandy.letsplot.layers.line
import uk.co.coroutines.enviropi.Data

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
        data: List<Data>,
        icon: String,
        colour: String,
        title: String,
        unit: String,
        accessor: (Data) -> Double
    ): Any {
      val current = "%.2f".format(accessor(data.last()))
      val min = "%.2f".format(data.minOf { accessor(it) })
      val max = "%.2f".format(data.maxOf { accessor(it) })

      val instants = data.map { it.instant }

      val time = instants.toColumn("time")
      val titleColumn = data.map { accessor(it) }.toColumn(title)
      val df =
          dataFrameOf(time, titleColumn)
              .groupBy {
                expr("timeKey") { get(time).roundDownTo((instants.max() - instants.max()) / 100) }
              }
              .aggregate {
                get(titleColumn).first() into "open"
                get(titleColumn).max() into "high"
                get(titleColumn).min() into "low"
                get(titleColumn).last() into "close"
                get(titleColumn).mean() into "mean"
              }

      val svg =
          df.plot {
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
