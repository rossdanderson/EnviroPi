package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class LightSensorGain(override val value: Int) : FieldMapping {
    `1`(0b000),
    `2`(0b001),
    `4`(0b010),
    `8`(0b011),
    `48`(0b110),
    `96`(0b111),
}