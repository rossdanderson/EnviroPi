package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.i2c.FieldMapping

enum class LightSensorRepeatRate(override val value: UInt) : FieldMapping {
    `50ms`(0b000u),
    `100ms`(0b001u),
    `200ms`(0b010u),
    `500ms`(0b011u),
    `1000ms`(0b100u),
    `2000ms`(0b101u),
}
