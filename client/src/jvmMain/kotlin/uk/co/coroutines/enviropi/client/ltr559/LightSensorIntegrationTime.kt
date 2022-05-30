package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.i2c.FieldMapping

enum class LightSensorIntegrationTime(override val bitValue: UInt, val value : Double) : FieldMapping {
    `100ms`(0b000u, 100.0),
    `50ms`(0b001u, 50.0),
    `200ms`(0b010u, 200.0),
    `400ms`(0b011u, 400.0),
    `150ms`(0b100u, 150.0),
    `250ms`(0b101u, 250.0),
    `300ms`(0b110u, 300.0),
    `350ms`(0b111u, 350.0),
}
