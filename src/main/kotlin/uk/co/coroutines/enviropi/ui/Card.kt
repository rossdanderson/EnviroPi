package uk.co.coroutines.enviropi.ui

import uk.co.coroutines.enviropi.Data

data class Card(
    val title: String,
    val colour: String,
    val current: String,
    val min: String,
    val max: String,
    val unit: String,
    val icon: String,
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

      return Card(title, colour, current, min, max, unit, icon)
    }
  }
}
