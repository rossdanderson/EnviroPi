package uk.co.coroutines.enviropi.client

import java.awt.Color.decode
import java.awt.Font
import java.awt.Font.TRUETYPE_FONT
import java.awt.Font.createFont
import kotlin.collections.windowed

object Resources {
    val font: Font = createFont(
        TRUETYPE_FONT,
        checkNotNull(Resources::class.java.getResourceAsStream("/pico-8.ttf")) { "Unable to find font" }
    )

    val red = decode("#FF004D")
    val yellow = decode("#FFEC27")
    val green = decode("#00E436")
    val blue = decode("#29ADFF")
    val white = decode("#FFF1E8")
    val peach = decode("#FFCCAA")

    val colors = listOf(red, yellow, green, blue, white)
        .windowed(2, 1, true)
        .flatMap { listOf(it[0], it[0]) }
        .toTypedArray()

    val fractions = listOf(0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f)
        .windowed(2, 1)
        .flatMap { listOf(it[0], it[1] - 0.05f) }
        .toFloatArray()
        .also { println(it.joinToString()) }
}
