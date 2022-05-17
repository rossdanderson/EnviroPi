package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.FieldMapping

enum class ProximitySensorCurrent(override val value: UInt) : FieldMapping {
    `5mA`(0b000u),
    `10mA`(0b001u),
    `20mA`(0b010u),
    `50mA`(0b011u),
    `100mA`(0b100u),
}
