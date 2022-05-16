package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.FieldMapping

enum class LightSensorGain(override val value: UInt) : FieldMapping {
    `1`(0b000u),
    `2`(0b001u),
    `4`(0b010u),
    `8`(0b011u),
    `48`(0b110u),
    `96`(0b111u),
}
