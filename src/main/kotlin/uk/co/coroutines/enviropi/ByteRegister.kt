package uk.co.coroutines.enviropi

import com.diozero.api.I2CDevice

abstract class ByteRegister(private val device: I2CDevice, private val register: Int) {

    protected var backingValue: UByte? = null
    open val value: UByte
        get() = backingValue ?: device.readByteData(register).toUByte().also { backingValue = it }

    fun resetCache() {
        backingValue = null
    }
}

abstract class MutableByteRegister(private val device: I2CDevice, private val register: Int) : ByteRegister(device, register) {

    override var value: UByte
        get() = super.value
        set(value) {
            backingValue = value
        }

    fun flush() {
        backingValue?.let { device.writeByteData(register, value.toInt()) }
    }
}
