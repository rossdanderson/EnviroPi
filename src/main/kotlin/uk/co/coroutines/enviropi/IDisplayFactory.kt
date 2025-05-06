package uk.co.coroutines.enviropi

import org.tinylog.kotlin.Logger.info
import uk.co.coroutines.enviropi.st7735.ST7735

interface IDisplayFactory {
  suspend fun create(): IDisplay

  val isDiozero: Boolean

  companion object {
    val default =
        object : IDisplayFactory {
          override suspend fun create(): IDisplay {
            info { "Creating default display" }
            return ST7735()
          }

          override val isDiozero: Boolean = true
        }

    val swing =
        object : IDisplayFactory {
          override suspend fun create(): IDisplay {
            info { "Creating swing display" }
            return SwingDisplay()
          }

          override val isDiozero: Boolean = false
        }
  }
}
