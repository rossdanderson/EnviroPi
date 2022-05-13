package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class ProximitySensorCurrent(override val value: Int) : FieldMapping {
    `5mA`(0b000),
    `10mA`(0b001),
    `20mA`(0b010),
    `50mA`(0b011),
    `100mA`(0b100),
}