package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class ProximitySensorPulseFrequency(override val value: UByte) : FieldMapping {
    `30KHz`(0b000.toUByte()),
    `40KHz`(0b001.toUByte()),
    `50KHz`(0b010.toUByte()),
    `60KHz`(0b011.toUByte()),
    `70KHz`(0b100.toUByte()),
    `80KHz`(0b101.toUByte()),
    `90KHz`(0b110.toUByte()),
    `100KHz`(0b111.toUByte()),
}