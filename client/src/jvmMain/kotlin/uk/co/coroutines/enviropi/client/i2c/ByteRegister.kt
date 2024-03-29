package uk.co.coroutines.enviropi.client.i2c

import com.diozero.api.I2CDevice

abstract class ByteRegister(private val device: I2CDevice, private val register: Int) :
    IRegister<UByte> {

    protected var backingValue: UByte? = null
    override val value: UByte
        get() = backingValue ?: device.readByteData(register).toUByte().also { backingValue = it }

    override fun resetCache() {
        backingValue = null
    }
}

abstract class MutableByteRegister(private val device: I2CDevice, private val register: Int) :
    ByteRegister(device, register),
    IMutableRegister<UByte> {

    override var value: UByte
        get() = super.value
        set(value) {
            backingValue = value
        }

    override fun flush() {
        backingValue?.let { device.writeByteData(register, value.toInt()) }
    }
}
