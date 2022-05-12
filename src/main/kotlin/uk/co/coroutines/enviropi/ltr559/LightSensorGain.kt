package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class LightSensorGain(override val value: UByte) : FieldMapping {
    `1`(0b000.toUByte()),
    `2`(0b000.toUByte()),
    `4`(0b010.toUByte()),
    `8`(0b011.toUByte()),
    `48`(0b110.toUByte()),
    `96`(0b111.toUByte()),
}