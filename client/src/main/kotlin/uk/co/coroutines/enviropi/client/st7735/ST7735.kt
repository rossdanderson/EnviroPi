// Copyright (c) 2024 - Ross Anderson
// Copyright (c) 2014 Adafruit Industries
// Author: Tony DiCola
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
@file:Suppress("PyUnresolvedReferences", "PyInterpreter", "PyStatementEffect")

package uk.co.coroutines.enviropi.client.st7735

import com.diozero.api.DigitalOutputDevice
import com.diozero.api.SpiClockMode
import com.diozero.api.SpiDevice
import kotlinx.coroutines.delay
import org.tinylog.Logger.info
import uk.co.coroutines.enviropi.client.IDisplay
import java.awt.image.BufferedImage
import java.awt.image.DataBufferUShort
import java.lang.Math.PI
import java.nio.ByteBuffer


/**
 * Adapted from https://github.com/pimoroni/st7735-python
 */
class ST7735 private constructor(
    private val device: SpiDevice,
    private val backlight: DigitalOutputDevice,
    private val dc: DigitalOutputDevice,
    private val deviceWidth: Int = ST7735_TFTWIDTH,
    private val deviceHeight: Int = ST7735_TFTHEIGHT,
    private val rotation: Int = 90,
    private val invert: Boolean = true,
    private val bgr: Boolean = true,
    private val offsetLeft: Int = (ST7735_COLS - deviceWidth).floorDiv(2),
    private val offsetTop: Int = (ST7735_ROWS - deviceHeight).floorDiv(2),
) : IDisplay {

    companion object {

        val BG_SPI_CS_BACK = 0
        val BG_SPI_CS_FRONT = 1

        val SPI_CLOCK_HZ = 16000000

        //    // Constants for interacting with display registers.
        val ST7735_TFTWIDTH = 80
        val ST7735_TFTHEIGHT = 160

        val ST7735_COLS = 132
        val ST7735_ROWS = 162

        val ST7735_NOP: UByte = 0x00u
        val ST7735_SWRESET: UByte = 0x01u
        val ST7735_RDDID: UByte = 0x04u
        val ST7735_RDDST: UByte = 0x09u

        val ST7735_SLPIN: UByte = 0x10u
        val ST7735_SLPOUT: UByte = 0x11u
        val ST7735_PTLON: UByte = 0x12u
        val ST7735_NORON: UByte = 0x13u

        val ST7735_INVOFF: UByte = 0x20u
        val ST7735_INVON: UByte = 0x21u
        val ST7735_DISPOFF: UByte = 0x28u
        val ST7735_DISPON: UByte = 0x29u

        val ST7735_CASET: UByte = 0x2Au
        val ST7735_RASET: UByte = 0x2Bu
        val ST7735_RAMWR: UByte = 0x2Cu
        val ST7735_RAMRD: UByte = 0x2Eu

        val ST7735_PTLAR: UByte = 0x30u
        val ST7735_MADCTL: UByte = 0x36u
        val ST7735_COLMOD: UByte = 0x3Au

        val ST7735_FRMCTR1: UByte = 0xB1u
        val ST7735_FRMCTR2: UByte = 0xB2u
        val ST7735_FRMCTR3: UByte = 0xB3u
        val ST7735_INVCTR: UByte = 0xB4u
        val ST7735_DISSET5: UByte = 0xB6u

        val ST7735_PWCTR1: UByte = 0xC0u
        val ST7735_PWCTR2: UByte = 0xC1u
        val ST7735_PWCTR3: UByte = 0xC2u
        val ST7735_PWCTR4: UByte = 0xC3u
        val ST7735_PWCTR5: UByte = 0xC4u
        val ST7735_VMCTR1: UByte = 0xC5u

        val ST7735_RDID1: UByte = 0xDAu
        val ST7735_RDID2: UByte = 0xDBu
        val ST7735_RDID3: UByte = 0xDCu
        val ST7735_RDID4: UByte = 0xDDu

        val ST7735_GMCTRP1: UByte = 0xE0u
        val ST7735_GMCTRN1: UByte = 0xE1u

        val ST7735_PWCTR6: UByte = 0xFCu

        //    // Colours for convenience
        val ST7735_BLACK: UShort = 0x0000u  // 0b 00000 000000 00000
        val ST7735_BLUE: UShort = 0x001Fu  // 0b 00000 000000 11111
        val ST7735_GREEN: UShort = 0x07E0u  // 0b 00000 111111 00000
        val ST7735_RED: UShort = 0xF800u  // 0b 11111 000000 00000
        val ST7735_CYAN: UShort = 0x07FFu  // 0b 00000 111111 11111
        val ST7735_MAGENTA: UShort = 0xF81Fu  // 0b 11111 000000 11111
        val ST7735_YELLOW: UShort = 0xFFE0u  // 0b 11111 111111 00000
        val ST7735_WHITE: UShort = 0xFFFFu  // 0b 11111 111111 11111

        suspend operator fun invoke(): ST7735 {

            val device = SpiDevice.builder(0)
                .setChipSelect(1)
                .setClockMode(SpiClockMode.MODE_0)
                .setLsbFirst(false)
                .setFrequency(10000000)
                .build()

            try {
                val backlight = DigitalOutputDevice.Builder.builder(12).build()
                try {
                    val dc: DigitalOutputDevice = DigitalOutputDevice.Builder.builder(9).build()
                    try {
                        return ST7735(device, backlight, dc).apply { initialise() }
                    } catch (e: Exception) {
                        dc.close()
                        throw e
                    }
                } catch (e: Exception) {
                    backlight.close()
                    throw e
                }
            } catch (e: Exception) {
                device.close()
                throw e
            }
        }
    }

    private val rotationRadians = PI / 2 * (rotation / 90)

    override val width: Int = if (rotation == 0 || rotation == 180) deviceWidth else deviceHeight

    override val height: Int = if (rotation == 0 || rotation == 180) deviceHeight else deviceWidth

    private suspend fun initialise() {
        info { "ST7735 device initialising" }

        backlight.setValue(false)
        delay(100)
        backlight.setValue(true)

//      Initialize the display.
        command(ST7735_SWRESET)    // Software reset
        delay(150)

        command(ST7735_SLPOUT)     // Out of sleep mode
        delay(500)

        command(ST7735_FRMCTR1)    // Frame rate ctrl - normal mode
        data(0x01u)                 // Rate = fosc/(1x2+40) * (LINE+2C+2D)
        data(0x2Cu)
        data(0x2Du)

        command(ST7735_FRMCTR2)    // Frame rate ctrl - idle mode
        data(0x01u)                 // Rate = fosc/(1x2+40) * (LINE+2C+2D)
        data(0x2Cu)
        data(0x2Du)

        command(ST7735_FRMCTR3)    // Frame rate ctrl - partial mode
        data(0x01u)                 // Dot inversion mode
        data(0x2Cu)
        data(0x2Du)
        data(0x01u)                 // Line inversion mode
        data(0x2Cu)
        data(0x2Du)

        command(ST7735_INVCTR)     // Display inversion ctrl
        data(0x07u)                 // No inversion

        command(ST7735_PWCTR1)     // Power control
        data(0xA2u)
        data(0x02u)                 // -4.6V
        data(0x84u)                 // auto mode

        command(ST7735_PWCTR2)     // Power control
        data(0x0Au)                 // Opamp current small
        data(0x00u)                 // Boost frequency

        command(ST7735_PWCTR4)     // Power control
        data(0x8Au)                 // BCLK/2, Opamp current small & Medium low
        data(0x2Au)

        command(ST7735_PWCTR5)     // Power control
        data(0x8Au)
        data(0xEEu)

        command(ST7735_VMCTR1)     // Power control
        data(0x0Eu)

        if (invert) command(ST7735_INVON)   // Invert display
        else command(ST7735_INVOFF)  // Don't invert display

        command(ST7735_MADCTL)     // Memory access control (directions)

        if (bgr) data(0xC8u)             // row addr/col addr, bottom to top refresh; Set D3 RGB Bit to 1 for format BGR
        else data(0xC0u)             // row addr/col addr, bottom to top refresh; Set D3 RGB Bit to 0 for format RGB

        command(ST7735_COLMOD)     // set color mode
        data(0x05u)                 // 16-bit color

        command(ST7735_CASET)      // Column addr set
        data(0x00u)                 // XSTART = 0
        data(offsetLeft.toUByte())
        data(0x00u)                 // XEND = ROWS - height
        data((deviceWidth + offsetLeft - 1).toUByte())

        command(ST7735_RASET)      // Row addr set
        data(0x00u)                 // XSTART = 0
        data(offsetTop.toUByte())
        data(0x00u)                 // XEND = COLS - width
        data((deviceHeight + offsetTop - 1).toUByte())

        command(ST7735_GMCTRP1)    // Set Gamma
        data(0x02u)
        data(0x1cu)
        data(0x07u)
        data(0x12u)
        data(0x37u)
        data(0x32u)
        data(0x29u)
        data(0x2du)
        data(0x29u)
        data(0x25u)
        data(0x2Bu)
        data(0x39u)
        data(0x00u)
        data(0x01u)
        data(0x03u)
        data(0x10u)

        command(ST7735_GMCTRN1)    // Set Gamma
        data(0x03u)
        data(0x1du)
        data(0x07u)
        data(0x06u)
        data(0x2Eu)
        data(0x2Cu)
        data(0x29u)
        data(0x2Du)
        data(0x2Eu)
        data(0x2Eu)
        data(0x37u)
        data(0x3Fu)
        data(0x00u)
        data(0x00u)
        data(0x02u)
        data(0x10u)

        command(ST7735_NORON)      // Normal display on
        delay(10)                // 10 ms

        displayOn()
        delay(100)               // 100 ms
    }

    fun displayOn() {
        command(ST7735_DISPON)
    }

    fun displayOff() {
        command(ST7735_DISPOFF)
    }

    fun sleep() {
        command(ST7735_SLPIN)
    }

    fun wake() {
        command(ST7735_SLPOUT)
    }

    /**
     * Set the pixel address window for proceeding drawing commands. x0 and
     *     x1 should define the minimum and maximum x pixel bounds.  y0 and y1
     *     should define the minimum and maximum y pixel bound.  If no parameters
     *     are specified the default will be to update the entire display from 0,0
     *     to width-1,height-1.
     */
    fun setWindow(x0: Int = 0, y0: Int = 0, x1: Int = deviceWidth - 1, y1: Int = deviceHeight - 1) {
        val offsetY0 = y0 + offsetTop
        val offsetY1 = y1 + offsetTop

        val offsetX0 = x0 + offsetLeft
        val offsetX1 = x1 + offsetLeft
        ByteBuffer.allocate(2).putShort(offsetX0.toShort())
        command(ST7735_CASET)       // Column addr set
        data((offsetX0 shr 8).toUByte())
        data(offsetX0.toUByte())                    // XSTART
        data((offsetX1 shr 8).toUByte())
        data(offsetX1.toUByte())                    // XEND
        command(ST7735_RASET)       // Row addr set
        data((offsetY0 shr 8).toUByte())
        data(offsetY0.toUByte())                    // YSTART
        data((offsetY1 shr 8).toUByte())
        data(offsetY1.toUByte())                    // YEND
        command(ST7735_RAMWR)       // write to RAM
    }

    private fun command(data: UByte) {
        dc.setValue(false)
        device.write(data.toByte())
    }

    private fun data(data: UByte) {
        dc.setValue(true)
        device.write(data.toByte())
    }

    private fun data(data: ShortArray) {
        dc.setValue(true)
        device.write(*data.flatMap { listOf((it.toInt() shr 8).toByte(), it.toByte()) }.toByteArray())
    }

    /**
     * Should be the correct width & height.
     */
    override fun display(bufferedImage: BufferedImage) {
        val drawImage = bufferedImage.format(rotation)
        setWindow()
        data((drawImage.raster.dataBuffer as DataBufferUShort).data)
    }

    override fun close() {
        runCatching { backlight.close() }
        runCatching { dc.close() }
        runCatching { device.close() }
    }
}