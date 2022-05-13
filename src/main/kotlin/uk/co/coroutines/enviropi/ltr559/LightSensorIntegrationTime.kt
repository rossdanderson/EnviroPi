package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class LightSensorIntegrationTime(override val value: Int) : FieldMapping {
    `100ms`(0b000),
    `50ms`(0b001),
    `200ms`(0b010),
    `400ms`(0b011),
    `150ms`(0b100),
    `250ms`(0b101),
    `300ms`(0b110),
    `350ms`(0b111),
}