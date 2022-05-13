package uk.co.coroutines.enviropi

import uk.co.coroutines.enviropi.MappingBitField.Companion.withMappings
import kotlin.reflect.KProperty

interface FieldMapping {
    val value: UByte
}

class BitField private constructor(private val mask: Int) {

    companion object {
        fun bitField(mask: Int) = BitField(mask)

        fun bitFlag(mask: Int): MappingBitField<Boolean> {
            check(mask.countOneBits() == 1)
            return BitField(mask)
                .withMappings(
                    1.toUByte() to true,
                    0.toUByte() to false
                )
        }
    }

    private val trailingZeros: Int = mask.countTrailingZeroBits()
    private val invMask = mask.inv()

    fun get(byte: UByte): UByte = (byte.toInt() and mask shr trailingZeros).toUByte()

    fun set(byte: UByte, bits: UByte): UByte = (byte.toInt() and invMask or (bits.toInt() shl trailingZeros and mask)).toUByte()

    operator fun getValue(register: Register, property: KProperty<*>): UByte = get(register.value)

    operator fun setValue(register: MutableRegister, property: KProperty<*>, bits: UByte) {
        register.value = set(register.value, bits)
    }
}

class MappingBitField<T : Any?> private constructor(
        private val bitField: BitField,
        pairs: Collection<Pair<UByte, T>>,
) {
    companion object {
        inline fun <reified T> BitField.withEnum(): MappingBitField<T>
                where T : FieldMapping, T : Enum<T> =
            withMappings(enumValues<T>().toList())

        fun <T : FieldMapping> BitField.withMappings(mappings: Collection<T>): MappingBitField<T> =
            MappingBitField(this, mappings.map { it.value to it})

        fun <T : Any?> BitField.withMappings(vararg pairs: Pair<UByte, T>): MappingBitField<T> =
            MappingBitField(this, pairs.toList())
    }

    init {
        check(pairs.map { it.first }.distinct().size == pairs.size) { "Duplicate keys" }
        check(pairs.map { it.second }.distinct().size == pairs.size) { "Duplicate values" }
    }

    private val map: Map<UByte, T> = pairs.toMap()
    private val inverseMap = pairs.associateBy({ it.second }, { it.first })

    fun get(byte: UByte): T = map.getValue(bitField.get(byte))

    fun set(byte: UByte, value: T): UByte = bitField.set(byte, inverseMap.getValue(value))

    operator fun getValue(register: Register, property: KProperty<*>): T = get(register.value)

    operator fun setValue(register: MutableRegister, property: KProperty<*>, value: T) {
        register.value = set(register.value, value)
    }
}
