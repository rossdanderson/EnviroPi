package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.i2c.FieldMapping

enum class LightSensorGain(override val value: UInt, val intVal: Int) : FieldMapping {
    `1`(0b000u, 1),
    `2`(0b001u, 2),
    `4`(0b010u, 4),
    `8`(0b011u, 8),
    `48`(0b110u, 48),
    `96`(0b111u, 96),
}
