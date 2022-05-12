package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class ProximitySensorMeasureRate(override val value: UByte): FieldMapping {
    `10ms`(0b1000.toUByte()),
    `50ms`(0b0000.toUByte()),
    `70ms`(0b0001.toUByte()),
    `100ms`(0b0010.toUByte()),
    `200ms`(0b0011.toUByte()),
    `500ms`(0b0100.toUByte()),
    `1000ms`(0b0101.toUByte()),
    `2000ms`(0b0110.toUByte()),
}