package uk.co.coroutines.enviropi.client.st7735

import java.awt.Color.BLUE
import java.awt.Color.RED
import java.awt.image.BufferedImage
import java.awt.image.BufferedImage.TYPE_USHORT_565_RGB

fun createTest(width: Int, height: Int) = BufferedImage(width, height, TYPE_USHORT_565_RGB).apply {
    createGraphics().run {
        color = RED
        drawLine(0, height, width / 2, 0)
        drawLine(width / 2, 0, width, height)
        color = BLUE
        drawLine(width / 2, 0, width / 2, height / 2)
        drawLine(width / 2 - 1, 0, width / 2 - 1, height / 2)
        drawLine(width / 2 + 1, 0, width / 2 + 1, height / 2)
        dispose()
    }
}

fun BufferedImage.format(rotation: Int): BufferedImage {
    val r180 = rotation == 180
    require(!(rotation != 90 && !r180 && rotation != 270)) { "Invalid angle." }
    val newWidth = if (r180) width else height
    val newHeight = if (r180) height else width
    return BufferedImage(newWidth, newHeight, TYPE_USHORT_565_RGB).apply {
        createGraphics().run {
            rotate(Math.toRadians(rotation.toDouble()), newWidth / 2.0, newHeight / 2.0)
            val offset = if (r180) 0 else (newWidth - newHeight) / 2
            drawImage(this@format, null, offset, -offset)
            dispose()
        }
    }
}