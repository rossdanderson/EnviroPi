package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.FieldMapping

enum class ProximitySensorMeasureRate(override val value: UInt): FieldMapping {
    `10ms`(0b1000u),
    `50ms`(0b0000u),
    `70ms`(0b0001u),
    `100ms`(0b0010u),
    `200ms`(0b0011u),
    `500ms`(0b0100u),
    `1000ms`(0b0101u),
    `2000ms`(0b0110u),
}
