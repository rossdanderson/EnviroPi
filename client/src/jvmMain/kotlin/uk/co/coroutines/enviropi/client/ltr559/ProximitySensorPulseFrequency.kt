package uk.co.coroutines.enviropi.client.ltr559

import uk.co.coroutines.enviropi.client.FieldMapping

enum class ProximitySensorPulseFrequency(override val value: UInt) : FieldMapping {
    `30KHz`(0b000u),
    `40KHz`(0b001u),
    `50KHz`(0b010u),
    `60KHz`(0b011u),
    `70KHz`(0b100u),
    `80KHz`(0b101u),
    `90KHz`(0b110u),
    `100KHz`(0b111u),
}
