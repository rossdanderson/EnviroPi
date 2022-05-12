package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class ProximitySensorCurrent(override val value: UByte) : FieldMapping {
    `5mA`(0b000.toUByte()),
    `10mA`(0b001.toUByte()),
    `20mA`(0b010.toUByte()),
    `50mA`(0b011.toUByte()),
    `100mA`(0b100.toUByte()),
}