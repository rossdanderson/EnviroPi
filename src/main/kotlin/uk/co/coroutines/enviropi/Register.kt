package uk.co.coroutines.enviropi

import com.diozero.api.I2CDevice

abstract class Register(private val device: I2CDevice, private val register: Int) {

    protected var backingByte: UByte? = null
    open val byte: UByte
        get() = backingByte ?: device.readByteData(register).toUByte().also { backingByte = it }

    fun resetCache() {
        backingByte = null
    }
}

abstract class MutableRegister(private val device: I2CDevice, private val register: Int) : Register(device, register) {

    override var byte: UByte
        get() = super.byte
        set(value) {
            backingByte = value
        }

    fun flush() {
        backingByte?.let { device.writeByteData(register, byte.toInt()) }
    }
}
