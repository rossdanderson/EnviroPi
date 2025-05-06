package uk.co.coroutines.enviropi.ui

import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
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

      val plot = dataFrameOf("time" to data.map { it.instant }, title to data.map { accessor(it) })

      val svg =
          plot
              .plot {
                line {
                  x("time")
                  y(title)
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
