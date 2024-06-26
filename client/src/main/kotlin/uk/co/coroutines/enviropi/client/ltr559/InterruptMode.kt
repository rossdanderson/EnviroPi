package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.i2c.FieldMapping

enum class InterruptMode(override val bitValue: UInt) : FieldMapping {
    OFF(0b00u),
    PROXIMITY_SENSOR(0b01u),
    LIGHT_SENSOR(0b10u),
    PROXIMITY_AND_LIGHT_SENSORS(0b11u),
}
