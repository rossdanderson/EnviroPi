package uk.co.coroutines.enviropi.ltr559

import uk.co.coroutines.enviropi.FieldMapping

enum class ProximitySensorPulseFrequency(override val value: Int) : FieldMapping {
    `30KHz`(0b000),
    `40KHz`(0b001),
    `50KHz`(0b010),
    `60KHz`(0b011),
    `70KHz`(0b100),
    `80KHz`(0b101),
    `90KHz`(0b110),
    `100KHz`(0b111),
}
