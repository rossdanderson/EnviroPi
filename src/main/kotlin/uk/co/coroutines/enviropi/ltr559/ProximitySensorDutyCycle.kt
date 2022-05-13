package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class ProximitySensorDutyCycle(override val value: Int) : FieldMapping {
    `0_25`(0b00),
    `0_50`(0b01),
    `0_75`(0b10),
    `1_00`(0b11),
}