package uk.co.coroutines.enviropi.i2c

import com.diozero.api.I2CDevice
import java.nio.ByteBuffer

abstract class IntRegister(private val device: I2CDevice, private val register: Int) :
    uk.co.coroutines.enviropi.i2c.IRegister<UInt> {
  protected val byteBuffer: ByteBuffer = ByteBuffer.allocate(4)

  protected var backingValue: UInt? = null
  override val value: UInt
    get() =
        backingValue
            ?: run {
              byteBuffer.clear()
              device.readI2CBlockData(register, byteBuffer.array())
              byteBuffer.int.toUInt().also { backingValue = it }
            }

  override fun resetCache() {
    backingValue = null
  }
}

abstract class MutableIntRegister(private val device: I2CDevice, private val register: Int) :
    uk.co.coroutines.enviropi.i2c.IntRegister(device, register),
    uk.co.coroutines.enviropi.i2c.IMutableRegister<UInt> {

  override var value: UInt
    get() = super.value
    set(value) {
      backingValue = value
    }

  override fun flush() {
    backingValue?.let {
      byteBuffer.clear()
      device.writeI2CBlockData(register, *byteBuffer.putInt(value.toInt()).array())
    }
  }
}
