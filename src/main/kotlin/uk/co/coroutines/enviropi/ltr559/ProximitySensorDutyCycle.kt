package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class ProximitySensorDutyCycle(override val value: UByte) : FieldMapping {
    `0_25`(0b00.toUByte()),
    `0_50`(0b01.toUByte()),
    `0_75`(0b10.toUByte()),
    `1_00`(0b11.toUByte()),
}