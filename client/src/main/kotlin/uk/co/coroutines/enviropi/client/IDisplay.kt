package uk.co.coroutines.enviropi.client

import java.awt.image.BufferedImage

interface IDisplay : AutoCloseable {

    val width: Int

    val height: Int

    fun display(bufferedImage: BufferedImage)
}