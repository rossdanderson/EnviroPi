package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.i2c.FieldMapping

enum class LightSensorIntegrationTime(override val value: UInt) : FieldMapping {
    `100ms`(0b000u),
    `50ms`(0b001u),
    `200ms`(0b010u),
    `400ms`(0b011u),
    `150ms`(0b100u),
    `250ms`(0b101u),
    `300ms`(0b110u),
    `350ms`(0b111u),
}
