package uk.co.coroutines.enviropi.client.i2c

import org.junit.jupiter.api.Test
import uk.co.coroutines.enviropi.client.i2c.BitFieldMask.Companion.mask
import uk.co.coroutines.enviropi.client.i2c.ByteSwappingBitField.Companion.swapBytes

internal class BitFieldTest {

    @Test
    fun `Swap bytes`() {
        val register = object : IMutableRegister<UShort> {
            override var value: UShort = 0x1221u

            var swapped: UShort by mask(0xFFFFu).swapBytes()

            override fun resetCache() {
                TODO("Not yet implemented")
            }

            override fun flush() {
                TODO("Not yet implemented")
            }
        }

        check(register.swapped == 0x2112u.toUShort())

        register.swapped = 0x3289u
        check(register.value == 0x8932u.toUShort())
    }
}
