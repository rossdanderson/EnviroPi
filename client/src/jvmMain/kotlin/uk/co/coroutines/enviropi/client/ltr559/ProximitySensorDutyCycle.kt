package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.FieldMapping

enum class ProximitySensorDutyCycle(override val value: UInt) : FieldMapping {
    `0_25`(0b00u),
    `0_50`(0b01u),
    `0_75`(0b10u),
    `1_00`(0b11u),
}
