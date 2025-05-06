package uk.co.coroutines.enviropi

data class Point2D(val x: Double, val y: Double) {
  operator fun get(i: Int) =
      when (i) {
        0 -> x
        1 -> y
        else -> throw IndexOutOfBoundsException()
      }
}
