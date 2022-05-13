package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class ProximitySensorMeasureRate(override val value: Int): FieldMapping {
    `10ms`(0b1000),
    `50ms`(0b0000),
    `70ms`(0b0001),
    `100ms`(0b0010),
    `200ms`(0b0011),
    `500ms`(0b0100),
    `1000ms`(0b0101),
    `2000ms`(0b0110),
}