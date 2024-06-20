package uk.co.coroutines.enviropi.client.ltr559

import com.diozero.api.I2CConstants
import com.diozero.api.I2CDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.tinylog.Logger.info
import uk.co.coroutines.enviropi.client.i2c.*
import uk.co.coroutines.enviropi.client.i2c.BitFieldMask.Companion.mask
import uk.co.coroutines.enviropi.client.i2c.ByteSwappingBitField.Companion.swapBytes
import uk.co.coroutines.enviropi.client.i2c.FocussedBitField.Companion.asShort
import uk.co.coroutines.enviropi.client.i2c.LookupBitField.Companion.asBoolean
import uk.co.coroutines.enviropi.client.i2c.LookupBitField.Companion.lookup
import uk.co.coroutines.enviropi.client.ltr559.LTR559.Bit12Adapter.Companion.bit12Adapter
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.seconds

/**
 * Adapted from https://github.com/pimoroni/ltr559-python
 *
 * https://optoelectronics.liteon.com/upload/download/ds86-2013-0003/ltr-559als-01_ds_v1.pdf
 */
class LTR559 private constructor() : AutoCloseable {
    companion object {
        const val I2C_ADDR = 0x23
        val PART_ID = 0x09.toUByte()
        val REVISION_ID = 0x02.toUByte()

        suspend operator fun invoke() = LTR559().apply { initialise() }
    }

    private val lightSensor = object {
        var lux: Double = 0.0

        val channel0Weight = listOf(17743, 42785, 5926, 0)
        val channel1Weight = listOf(-11059, 19548, -1185, 0)

        // Non default
        var gain = LightSensorGain.`4`  // 4x gain = 0.25 to 16k lux
        var integrationTime = LightSensorIntegrationTime.`50ms`
    }

    private val proximitySensor = object {
        var channel0: Double = 0.0
    }

    private val device =
        I2CDevice.builder(I2C_ADDR)
            .setController(I2CConstants.CONTROLLER_1)
            .build()

    private val lightSensorControl = object : MutableByteRegister(device, 0x80) {
        var gain: LightSensorGain by mask(0b00011100u).lookup()
        var softwareReset: Boolean by mask(0b00000010u).asBoolean()
        var mode: Boolean by mask(0b00000001u).asBoolean()
    }

    private val proximitySensorControl = object : MutableByteRegister(device, 0x81) {
        var saturationIndicatorEnable: Boolean by mask(0b00100000u).asBoolean()

        var active: Boolean by mask(0b00000011u)
            .lookup(
                0b00u to false,
                0b11u to true,
            )
    }

    private val proximitySensorLED = object : MutableByteRegister(device, 0x82) {
        var pulseFrequency: ProximitySensorPulseFrequency by mask(0b11100000u).lookup()
        var dutyCycle: ProximitySensorDutyCycle by mask(0b00011000u).lookup()
        var current: ProximitySensorCurrent by mask(0b00000111u).lookup()
    }

    private val proximitySensorPulsesCount = object : MutableByteRegister(device, 0x83) {
        var count: UByte by mask(0b00001111u)
    }

    private val proximitySensorMeasureRate = object : MutableByteRegister(device, 0x84) {
        var rate: ProximitySensorMeasureRate by mask(0b00001111u).lookup()
    }

    private val lightSensorMeasureRate = object : MutableByteRegister(device, 0x85) {
        var integrationTime: LightSensorIntegrationTime by mask(0b00111000u).lookup()
        var repeatRate: LightSensorRepeatRate by mask(0b00000111u).lookup()
    }

    private val deviceInfo = object : ByteRegister(device, 0x86) {

        val partId: UByte by mask(0b11110000u)

        val revisionId: UByte by mask(0b00001111u)
    }

    private val manufacturerInfo = object : ByteRegister(device, 0x87) {

        val manufacturerId: UByte by mask(0b11111111u)
    }

    private val lightSensorData = object : IntRegister(device, 0x88) {
        val channel1: UShort by mask(0xFFFF0000u).asShort().swapBytes()
        val channel0: UShort by mask(0x0000FFFFu).asShort().swapBytes()
    }

    private val status = object : ByteRegister(device, 0x8C) {
        val lightSensorDataValid: Boolean by mask(0b10000000u).asBoolean()
        val lightSensorGain: LightSensorGain by mask(0b01110000u).lookup()
        val lightSensorInterrupt: Boolean by mask(0b00001000u).asBoolean()
        val lightSensorData: Boolean by mask(0b00000100u).asBoolean()
        val proximitySensorInterrupt: Boolean by mask(0b00000010u).asBoolean()
        val proximitySensorData: Boolean by mask(0b00000001u).asBoolean()
    }

    private val proximitySensorData = object : ShortRegister(device, 0x8D) {
        val channel0 by mask(0xFF0Fu).bit12Adapter()
        val saturation by mask(0x0080u)
    }

    private val proximitySensorThreshold = object : MutableIntRegister(device, 0x90) {
        var upper by mask(0xFF0F0000u).asShort().bit12Adapter()
        var lower by mask(0x0000FF0Fu).asShort().bit12Adapter()
    }

    private val proximitySensorOffset = object : MutableShortRegister(device, 0x94) {
        var offset by mask(0x03FFu) // Last two bits of 0x94, full 8 bits of 0x95
    }

    private val lightSensorThreshold = object : MutableIntRegister(device, 0x97) {
        var upper by mask(0xFFFF0000u).asShort().swapBytes()
        var lower by mask(0x0000FFFFu).asShort().swapBytes()
    }

    suspend fun initialise() {
        check(deviceInfo.partId == PART_ID)
        check(deviceInfo.revisionId == REVISION_ID)
        check(manufacturerInfo.manufacturerId == 0x05.toUByte())

        lightSensorControl.write { softwareReset = true }
        withTimeout(5.seconds) {
            do {
                val resetting = lightSensorControl.read { softwareReset }
                delay(5)
            } while (resetting)
        }
        info { "LTR559 initialised" }

        proximitySensorLED.write {
            current = ProximitySensorCurrent.`50mA`
            dutyCycle = ProximitySensorDutyCycle.`1_00`
            pulseFrequency = ProximitySensorPulseFrequency.`30KHz`
        }

//        # 1 pulse is the default value
        proximitySensorPulsesCount.write { count = 1u }

        lightSensorControl.write {
            mode = true
            gain = lightSensor.gain
        }

        proximitySensorControl.write {
            active = true
            saturationIndicatorEnable = true
        }

        proximitySensorMeasureRate.write { rate = ProximitySensorMeasureRate.`100ms` }

        lightSensorMeasureRate.write {
            integrationTime = lightSensor.integrationTime
            repeatRate = LightSensorRepeatRate.`50ms`
        }

        lightSensorThreshold.write {
            lower = 0x0000u
            upper = 0xFFFFu
        }

        proximitySensorThreshold.write {
            lower = 0x0000u
            upper = 0xFFFFu
        }

        proximitySensorOffset.write { offset = 0u }
    }

    val dataAvailable
        get() = status.read {
            (status.lightSensorInterrupt || status.lightSensorData) &&
                    (status.proximitySensorInterrupt || status.proximitySensorData)
        }

    fun getLux(passive: Boolean = false): Double {
        if (!passive) {
            val channel0: Double
            val channel1: Double
            lightSensorData.read {
                channel0 = this.channel0.toDouble()
                channel1 = this.channel1.toDouble()
            }

            val ratio =
                if (channel0 + channel1 > 0.0) channel1 * 100.0 / (channel0 + channel1)
                else 101.0

            val index = when {
                ratio < 45 -> 0
                ratio < 64 -> 1
                ratio < 85 -> 2
                else -> 3
            }

            lightSensor.lux = try {
                var lux = (channel0 * lightSensor.channel0Weight[index]) -
                        (channel1 * lightSensor.channel1Weight[index])
                lux /= (lightSensor.integrationTime.value / 100.0)
                lux /= lightSensor.gain.value
                lux /= 10000.0
                lux
            } catch (t: Throwable) {
                0.0
            }
        }

        return lightSensor.lux
    }

    fun getProximity(passive: Boolean = false): Double {
        if (!passive) proximitySensor.channel0 = proximitySensorData.read { channel0 }.toDouble()

        return proximitySensor.channel0
    }

    private class Bit12Adapter<N> private constructor(private val bitField: IBitField<N, UShort>) :
        IBitField<N, UShort> by bitField {
        companion object {
            fun <N> IBitField<N, UShort>.bit12Adapter(): IBitField<N, UShort> = Bit12Adapter(this)
        }

        override fun getValue(register: IRegister<N>, property: KProperty<*>): UShort =
            decode(bitField.getValue(register, property))

        override fun setValue(register: IMutableRegister<N>, property: KProperty<*>, value: UShort) {
            bitField.setValue(register, property, encode(value))
        }

        /**
         * Convert the 12-bit input into the correct format for the registers,
         * the low byte followed by 4 empty bits and the high nibble:
         * 0bHHHHLLLLLLLL -> 0bLLLLLLLLXXXXHHHH
         */
        private fun encode(bytes: UShort): UShort {
            val iBytes = bytes.toUInt()
            return (iBytes and 0xFFu shl 8 or (iBytes and 0xF00u shr 8)).toUShort()
        }

        /**
         * Convert the 16-bit output into the correct format for reading:
         * 0bLLLLLLLLXXXXHHHH -> 0bHHHHLLLLLLLL
         */
        private fun decode(bytes: UShort): UShort {
            val iBytes = bytes.toUInt()
            return (iBytes and 0xFF00u shr 8 or (iBytes and 0x000Fu shl 8)).toUShort()
        }
    }

    override fun close() {
        device.close()
    }
}
