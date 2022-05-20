package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.i2c.FieldMapping

enum class LightSensorIntegrationTime(override val value: UInt, val intVal : Int) : FieldMapping {
    `100ms`(0b000u, 100),
    `50ms`(0b001u, 50),
    `200ms`(0b010u, 200),
    `400ms`(0b011u, 400),
    `150ms`(0b100u, 150),
    `250ms`(0b101u, 250),
    `300ms`(0b110u, 300),
    `350ms`(0b111u, 350),
}
