package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class LightSensorRepeatRate(override val value: Int) : FieldMapping {
    `50ms`(0b000),
    `100ms`(0b001),
    `200ms`(0b010),
    `500ms`(0b011),
    `1000ms`(0b100),
    `2000ms`(0b101),
}