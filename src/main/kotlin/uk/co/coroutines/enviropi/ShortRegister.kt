package uk.co.coroutines.enviropi

import com.diozero.api.I2CDevice
import java.nio.ByteBuffer

abstract class ShortRegister(private val device: I2CDevice, private val register: Int) {
    protected val byteBuffer: ByteBuffer = ByteBuffer.allocate(2)

    protected var backingValue: UShort? = null
    open val value: UShort
        get() = backingValue ?: run {
            device.readI2CBlockData(register, byteBuffer.array())
            byteBuffer.short.toUShort().also { backingValue = it }
        }

    fun resetCache() {
        backingValue = null
    }
}

abstract class MutableShortRegister(private val device: I2CDevice, private val register: Int) :
    ShortRegister(device, register) {

    override var value: UShort
        get() = super.value
        set(value) {
            backingValue = value
        }

    fun flush() {
        backingValue?.let {
            device.writeI2CBlockData(register, *byteBuffer.putShort(value.toShort()).array())
        }
    }
}
