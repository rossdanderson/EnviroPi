package uk.co.coroutines.enviropi

import uk.co.coroutines.enviropi.MappingBitField.Companion.withMappings
import kotlin.reflect.KProperty

interface FieldMapping {
    val value: UInt
}

class BitField private constructor(private val mask: UInt) {

    companion object {

        fun bitField(mask: UInt) = BitField(mask)

        fun bitFlag(mask: UInt): MappingBitField<Boolean> {
            check(mask.countOneBits() == 1)
            return BitField(mask)
                .withMappings(
                    1u to true,
                    0u to false
                )
        }
    }

    private val trailingZeros: Int = mask.countTrailingZeroBits()
    private val invMask = mask.inv()

    fun get(byte: UByte): UByte = get(byte.toUInt()).toUByte()

    fun set(byte: UByte, bits: UByte): UByte = set(byte.toUInt(), bits.toUInt()).toUByte()

    fun get(short: UShort): UShort = get(short.toUInt()).toUShort()

    fun set(short: UShort, bits: UShort): UShort = set(short.toUInt(), bits.toUInt()).toUShort()

    fun get(int: UInt): UInt = int and mask shr trailingZeros

    fun set(int: UInt, bits: UInt): UInt = int and invMask or (bits shl trailingZeros and mask)

    operator fun getValue(register: ByteRegister, property: KProperty<*>): UByte = get(register.value)

    operator fun setValue(register: MutableByteRegister, property: KProperty<*>, bits: UByte) {
        register.value = set(register.value, bits)
    }

    operator fun getValue(register: ShortRegister, property: KProperty<*>): UShort = get(register.value)

    operator fun setValue(register: MutableShortRegister, property: KProperty<*>, bits: UShort) {
        register.value = set(register.value, bits)
    }

    operator fun getValue(register: IntRegister, property: KProperty<*>): UInt = get(register.value)

    operator fun setValue(register: MutableIntRegister, property: KProperty<*>, bits: UInt) {
        register.value = set(register.value, bits)
    }
}

class MappingBitField<T : Any?> private constructor(
    private val bitField: BitField,
    pairs: Collection<Pair<UInt, T>>,
) {
    companion object {
        inline fun <reified T> BitField.withEnum(): MappingBitField<T>
                where T : FieldMapping, T : Enum<T> =
            withMappings(enumValues<T>().toList())

        fun <T : FieldMapping> BitField.withMappings(mappings: Collection<T>): MappingBitField<T> =
            MappingBitField(this, mappings.map { it.value to it })

        fun <T : Any?> BitField.withMappings(vararg pairs: Pair<UInt, T>): MappingBitField<T> =
            MappingBitField(this, pairs.toList())
    }

    init {
        check(pairs.map { it.first }.distinct().size == pairs.size) { "Duplicate keys" }
        check(pairs.map { it.second }.distinct().size == pairs.size) { "Duplicate values" }
    }

    private val map: Map<UInt, T> = pairs.toMap()
    private val inverseMap = pairs.associateBy({ it.second }, { it.first })

    fun get(byte: UByte): T = get(byte.toUInt())

    fun set(byte: UByte, value: T): UByte = set(byte.toUInt(), value).toUByte()

    fun get(byte: UShort): T = get(byte.toUInt())

    fun set(byte: UShort, value: T): UShort = set(byte.toUInt(), value).toUShort()

    fun get(int: UInt): T = map.getValue(bitField.get(int))

    fun set(int: UInt, value: T): UInt = bitField.set(int, inverseMap.getValue(value))

    operator fun getValue(register: ByteRegister, property: KProperty<*>): T = get(register.value)

    operator fun setValue(register: MutableByteRegister, property: KProperty<*>, value: T) {
        register.value = set(register.value, value)
    }

    operator fun getValue(register: ShortRegister, property: KProperty<*>): T = get(register.value)

    operator fun setValue(register: MutableShortRegister, property: KProperty<*>, value: T) {
        register.value = set(register.value, value)
    }

    operator fun getValue(register: IntRegister, property: KProperty<*>): T = get(register.value)

    operator fun setValue(register: MutableIntRegister, property: KProperty<*>, value: T) {
        register.value = set(register.value, value)
    }
}
