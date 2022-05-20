package uk.co.coroutines.enviropi.client.ltr559

import com.diozero.api.I2CConstants
import com.diozero.api.I2CDevice
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.tinylog.Logger
import uk.co.coroutines.enviropi.client.i2c.*
import uk.co.coroutines.enviropi.client.i2c.BitFieldMask.Companion.mask
import uk.co.coroutines.enviropi.client.i2c.ByteSwappingBitField.Companion.swapBytes
import uk.co.coroutines.enviropi.client.i2c.FocussedBitField.Companion.asShort
import uk.co.coroutines.enviropi.client.i2c.LookupBitField.Companion.asBoolean
import uk.co.coroutines.enviropi.client.i2c.LookupBitField.Companion.lookup
import uk.co.coroutines.enviropi.client.ltr559.LTR559.Bit12Adapter.Companion.bit12Adapter
import java.nio.ByteOrder
import kotlin.reflect.KProperty
import kotlin.time.Duration.Companion.seconds

class LTR559 private constructor(): AutoCloseable {
    companion object {
        const val I2C_ADDR = 0x23
        val PART_ID = 0x09.toUByte()
        val REVISION_ID = 0x02.toUByte()

        suspend operator fun invoke() = LTR559().apply { initialise() }
    }

    private var lightSensor0 = 0.0
    private var lightSensor1 = 0.0
    private var lux = 0.0
    private var ratio = 100.0

    private var proximitySensor0 = 0.0

    // Non default
    private var gain = LightSensorGain.`4`  // 4x gain = 0.25 to 16k lux
    private var integrationTime = LightSensorIntegrationTime.`50ms`

    private val _ch0_c = arrayOf(17743, 42785, 5926, 0)
    private val _ch1_c = arrayOf(-11059, 19548, -1185, 0)

    private val device =
        I2CDevice.builder(I2C_ADDR)
            .setController(I2CConstants.CONTROLLER_1)
            .setByteOrder(ByteOrder.LITTLE_ENDIAN)
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
        val ch1: UShort by mask(0xFFFF0000u).asShort().swapBytes()
        val ch0: UShort by mask(0x0000FFFFu).asShort().swapBytes()
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
        val ch0 by mask(0xFF0Fu).bit12Adapter()
        val saturation by mask(0x0080u)
    }

    // allows the interrupt pin and function behaviour to be configured.
    private val interrupt = object : MutableByteRegister(device, 0x8F) {
        var polarity by mask(0b00000100u).asBoolean()
        var mode: InterruptMode by mask(0b00000011u).lookup()
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

    private val interruptPersist = object : MutableByteRegister(device, 0x9E) {
        var proximitySensor = mask(0xF0u)
        var lightSensor = mask(0x0Fu)
    }

    suspend fun initialise() {
        check(deviceInfo.partId == PART_ID)
        check(deviceInfo.revisionId == REVISION_ID)
        check(manufacturerInfo.manufacturerId == 0x05.toUByte())

        with(lightSensorControl) {
            softwareReset = true
            flush()
            withTimeout(5.seconds) {
                do {
                    delay(5)
                    resetCache()
                } while (softwareReset)
            }
        }
        Logger.info { "LTR559 initialised" }
//
//        # Interrupt register must be set before device is switched to active mode
//        # see datasheet page 12/40, note #2.
//        if enable_interrupts:
//            self._ltr559.set('INTERRUPT',
//                             mode='als+ps',
//                             polarity=interrupt_pin_polarity)

        proximitySensorLED.update {
            current = ProximitySensorCurrent.`50mA`
            dutyCycle = ProximitySensorDutyCycle.`1_00`
            pulseFrequency = ProximitySensorPulseFrequency.`30KHz`
        }

//        # 1 pulse is the default value
        proximitySensorPulsesCount.update {
            count = 1u
        }

        lightSensorControl.update {
            mode = true
            gain = this@LTR559.gain
        }

        proximitySensorControl.update {
            active = true
            saturationIndicatorEnable = true
        }

        proximitySensorMeasureRate.update {
            rate = ProximitySensorMeasureRate.`100ms`
        }

        lightSensorMeasureRate.update {
            integrationTime = this@LTR559.integrationTime
            repeatRate = LightSensorRepeatRate.`50ms`
        }

        lightSensorThreshold.update {
            lower = 0x0000u
            upper = 0xFFFFu
        }

        proximitySensorThreshold.update {
            lower = 0x0000u
            upper = 0xFFFFu
        }

        proximitySensorOffset.update {
            offset = 0u
        }
    }

    private fun updateSensor() {

//        Update the sensor lux and proximity values.
//        Will perform a read of the status register and determine if either an interrupt
//        has triggered or the new data flag for the light or proximity sensor is flipped.
//        If new data is present it will be calculated and stored for later retrieval.
//        Proximity data is stored in `self._ps0` and can be retrieved with `get_proximity`.
//        Light sensor data is stored in `self._lux` and can be retrieved with `get_lux`.
//        Raw light sensor data is also stored in `self._als0` and self._als1` which store
//        the ch0 and ch1 values respectively. These can be retrieved with `get_raw_als`.

        var proximitySensorInt = false
        var lightSensorInt = false
        status.read {
            proximitySensorInt = status.proximitySensorInterrupt || status.proximitySensorData
            lightSensorInt = status.lightSensorInterrupt || status.lightSensorData
        }

        if (proximitySensorInt) proximitySensor0 = proximitySensorData.read { ch0 }.toDouble()

        if (lightSensorInt) {
            lightSensorData.read {
                lightSensor0 = lightSensorData.ch0.toDouble()
                lightSensor1 = lightSensorData.ch1.toDouble()
            }


            ratio = if (lightSensor0 + lightSensor1 > 0) lightSensor1 * 100.0 / (lightSensor1 + lightSensor0) else 101.0

            val index = when {
                ratio < 45 -> 0
                ratio < 64 -> 1
                ratio < 85 -> 2
                else -> 3
            }

            lux = try {
                var lux = (lightSensor0 * _ch0_c[index]) - (lightSensor1 * _ch1_c[index])
                lux /= (integrationTime.intVal.toDouble() / 100.0)
                lux /= gain.intVal.toDouble()
                lux /= 10000.0
                lux
            } catch (t: Throwable) { 0.0 }
        }
    }

    fun getLux(passive: Boolean = false): Double {
        if (!passive) updateSensor()
        return lux
    }

    fun getProximity(passive: Boolean = false): Double {
        if (!passive) updateSensor()
        return proximitySensor0
    }
//
//    def get_part_id(self):
//        """Get part number.
//        Returns the Part Number ID portion of the PART_ID register.
//        This should always equal 0x09,
//        differences may indicate a sensor incompatible with this library.
//        """
//        return self.part_id.part_number
//
//    def get_revision(self):
//        """Get revision ID.
//        Returns the Revision ID portion of the PART_ID register.
//        This library was tested against revision 0x02,
//        differences may indicate a sensor incompatible with this library.
//        """
//        return self.part_id.revision
//
//    def set_light_threshold(self, lower, upper):
//        """Set light interrupt threshold.
//        Set the upper and lower threshold for the light sensor interrupt.
//        An interrupt is triggered for readings *outside* of this range.
//        Since the light threshold is specified in raw counts and applies to
//        both ch0 and ch1 it's not possible to interrupt at a specific Lux value.
//        :param lower: Lower threshold in raw ADC counts
//        :param upper: Upper threshold in raw ADC counts
//        """
//        self._ltr559.set('ALS_THRESHOLD',
//                         lower=lower,
//                         upper=upper)
//
//    def set_proximity_threshold(self, lower, upper):
//        """Set proximity interrupt threshold.
//        Set the upper and lower threshold for the proximity interrupt.
//        An interrupt is triggered for readings *outside* of this range.
//        :param lower: Lower threshold
//        :param upper: Upper threshold
//        """
//        self._ltr559.set('PS_THRESHOLD',
//                         lower=lower,
//                         upper=upper)
//
//    def set_proximity_rate_ms(self, rate_ms):
//        """Set proximity measurement repeat rate in milliseconds.
//        This is the rate at which the proximity sensor is measured. For example:
//        A rate of 100ms would result in ten proximity measurements every second.
//        :param rate_ms: Time in milliseconds- one of 10, 50, 70, 100, 200, 500, 1000 or 2000
//        """
//        self._ltr559.set('PS_MEAS_RATE', rate_ms=rate_ms)
//
//    def set_light_integration_time_ms(self, time_ms):
//        """Set light integration time in milliseconds.
//        This is the measurement time for each individual light sensor measurement,
//        it must be equal to or less than the repeat rate.
//        :param time_ms: Time in milliseconds- one of 50, 100, 150, 200, 300, 350, 400
//        """
//        self._integration_time = time_ms
//        self._ltr559.set('ALS_MEAS_RATE', integration_time_ms=time_ms)
//
//    def set_light_repeat_rate_ms(self, rate_ms=100):
//        """Set light measurement repeat rate in milliseconds.
//        This is the rate at which light measurements are repeated. For example:
//        A repeat rate of 1000ms would result in one light measurement every second.
//        The repeat rate must be equal to or larger than the integration time, if a lower
//        value is picked then it is automatically reset by the LTR559 to match the chosen
//        integration time.
//        :param rate_ms: Rate in milliseconds- one of 50, 100, 200, 500, 1000 or 2000
//        """
//        self._ltr559.set('ALS_MEAS_RATE', repeat_rate_ms=rate_ms)
//
//    def set_interrupt_mode(self, enable_light=True, enable_proximity=True):
//        """Set the intterupt mode
//        :param enable_light: Enable the light sensor interrupt
//        :param enable_proximity: Enable the proximity sensor interrupt
//        """
//        mode = []
//
//        if enable_light:
//            mode.append('als')
//
//        if enable_proximity:
//            mode.append('ps')
//
//        self._ltr559.set('INTERRUPT', mode='+'.join(mode))
//
//    def set_proximity_active(self, active=True):
//        """Enable/disable proximity sensor
//        :param active: True for enabled, False for disabled
//        """
//        self._ltr559.set('PS_CONTROL', active=active)
//
//    def set_proximity_saturation_indictator(self, enabled=True):
//        """Enable/disable the proximity saturation indicator
//        :param enabled: True for enabled, False for disabled
//        """
//        self._ltr559.set('PS_CONTROL', saturation_indicator_enable=enabled)
//
//    def set_proximity_offset(self, offset):
//        """Setup the proximity compensation offset
//        :param offset: Offset value from 0 to 1023
//        """
//        return self._ltr559.set('PS_OFFSET', offset=offset)
//
//    def set_proximity_led(self, current_ma=50, duty_cycle=1.0, pulse_freq_khz=30, num_pulses=1):
//        """Setup the proximity led current and properties.
//        :param current_ma: LED current in milliamps- one of 5, 10, 20, 50 or 100
//        :param duty_cycle: LED duty cucle- one of 0.25, 0.5, 0.75 or 1.0 (25%, 50%, 75% or 100%)
//        :param pulse_freq_khz: LED pulse frequency- one of 30, 40, 50, 60, 70, 80, 90 or 100
//        :param num_pulse: Number of LED pulses to be emitted- 1 to 15
//        """
//        self._ltr559.set('PS_LED',
//                         current_ma=current_ma,
//                         duty_cycle=duty_cycle,
//                         set_pulse_freq_khz=pulse_freq_khz)
//
//        self._ltr559.set('PS_N_PULSES', num_pulses)
//
//    def set_light_options(self, active=True, gain=4):
//        """Set the mode and gain for the light sensor.
//        By default the sensor is active with a gain of 4x (0.25 to 16k lux).
//        :param active: True for Active Mode, False for Stand-by Mode
//        :param gain: Light sensor gain x- one of 1, 2, 4, 8, 48 or 96
//        1x = 1 to 64k lux
//        2x = 0.5 to 32k lux
//        4x = 0.25 to 16k lux
//        8x = 0.125 to 8k lux
//        48x = 0.02 to 1.3k lux
//        96x = 0.01 to 600 lux
//        """
//        self._gain = gain
//        self._ltr559.set('ALS_CONTROL',
//                         mode=active,
//                         gain=gain)
//

//
//    def get_gain(self):
//        """Return gain used in lux calculation."""
//        return self._gain
//
//    def get_integration_time(self):
//        """Return integration time used in lux calculation.
//        Integration time directly affects the raw ch0 and ch1 readings and is required
//        to correctly calculate a lux value.
//        """
//        return self._integration_time
//
//    get_intt = get_integration_time
//
//    def get_raw_als(self, passive=True):
//        """Return raw ALS channel data ch0, ch1.
//        The two channels measure Visible + IR and just IR respectively.
//        """
//        if not passive:
//            self.update_sensor()
//        return self._als0, self._als1
//
//    def get_ratio(self, passive=True):
//        """Return the ambient light ratio between ALS channels.
//        Uses the IR-only channel to discount the portion of IR from the unfiltered channel.
//        """
//        if not passive:
//            self.update_sensor()
//        return self._ratio
//
//    def get_lux(self, passive=False):
//        """Return the ambient light value in lux."""
//        if not passive:
//            self.update_sensor()
//        return self._lux
//
//    def get_interrupt(self):
//        """Return the light and proximity sensor interrupt status.
//        An interrupt is asserted when the light or proximity sensor reading is *outside*
//        the range set by the upper and lower thresholds.
//        """
//        interrupt = self._ltr559.get('ALS_PS_STATUS')
//        return interrupt.als_interrupt, interrupt.ps_interrupt
//
//    def get_proximity(self, passive=False):
//        """Return the proximity.
//        Returns the raw proximity reading from the sensor.
//        A closer object produces a larger value.
//        """
//        if not passive:
//            self.update_sensor()
//        return self._ps0

    private class Bit12Adapter<N> private constructor(private val bitField: IBitField<N, UShort>) :
        IBitField<N, UShort> by bitField {
        companion object {
            fun <N> IBitField<N, UShort>.bit12Adapter(): IBitField<N, UShort> = Bit12Adapter(this)
        }

        override fun getValue(register: IRegister<N>, property: KProperty<*>): UShort =
            decode(bitField.getValue(register, property).toUInt()).toUShort()

        override fun setValue(register: IMutableRegister<N>, property: KProperty<*>, value: UShort) {
            bitField.setValue(register, property, encode(value.toUInt()).toUShort())
        }

        /**
         * Convert the 12-bit input into the correct format for the registers,
         * the low byte followed by 4 empty bits and the high nibble:
         * 0bHHHHLLLLLLLL -> 0bLLLLLLLLXXXXHHHH
         */
        private fun encode(bytes: UInt): UInt =
            bytes and 0xFFu shl 8 or (bytes and 0xF00u shr 8)

        /**
         * Convert the 16-bit output into the correct format for reading:
         * 0bLLLLLLLLXXXXHHHH -> 0bHHHHLLLLLLLL
         */
        private fun decode(bytes: UInt): UInt =
            bytes and 0xFF00u shr 8 or (bytes and 0x000Fu shl 8)
    }

    override fun close() {
        device.close()
    }
}
