package uk.co.coroutines.enviropi

import java.awt.image.BufferedImage

interface IDisplay : AutoCloseable {

  val width: Int

  val height: Int

  fun display(bufferedImage: BufferedImage)

  val isDiozero: Boolean
}
