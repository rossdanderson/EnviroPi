package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.i2c.FieldMapping

enum class LightSensorGain(override val bitValue: UInt, val value: Double) : FieldMapping {
    `1`(0b000u, 1.0),
    `2`(0b001u, 2.0),
    `4`(0b010u, 4.0),
    `8`(0b011u, 8.0),
    `48`(0b110u, 48.0),
    `96`(0b111u, 96.0),
}
