package uk.co.coroutines.enviropi

import uk.co.coroutines.enviropi.MappingBitField.Companion.withMappings
import kotlin.reflect.KProperty

interface FieldMapping {
    val value: Int
}

class BitField private constructor(private val mask: Int) {

    companion object {
        fun bitField(mask: Int) = BitField(mask)

        fun bitFlag(mask: Int): MappingBitField<Boolean> {
            check(mask.countOneBits() == 1)
            return BitField(mask)
                .withMappings(
                    1 to true,
                    0 to false
                )
        }
    }

    private val trailingZeros: Int = mask.countTrailingZeroBits()
    private val invMask = mask.inv()

    fun get(byte: UByte): UByte = get(byte.toInt()).toUByte()

    fun set(byte: UByte, bits: UByte): UByte = set(byte.toInt(), bits.toInt()).toUByte()

    fun get(short: UShort): UShort = get(short.toInt()).toUShort()

    fun set(short: UShort, bits: UShort): UShort = set(short.toInt(), bits.toInt()).toUShort()

    fun get(int: UInt): UInt = get(int.toInt()).toUInt()

    fun set(int: UInt, bits: UInt): UInt = set(int.toInt(), bits.toInt()).toUInt()

    fun get(int: Int): Int = int and mask shr trailingZeros

    fun set(int: Int, bits: Int): Int = int and invMask or (bits shl trailingZeros and mask)

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
    pairs: Collection<Pair<Int, T>>,
) {
    companion object {
        inline fun <reified T> BitField.withEnum(): MappingBitField<T>
                where T : FieldMapping, T : Enum<T> =
            withMappings(enumValues<T>().toList())

        fun <T : FieldMapping> BitField.withMappings(mappings: Collection<T>): MappingBitField<T> =
            MappingBitField(this, mappings.map { it.value to it })

        fun <T : Any?> BitField.withMappings(vararg pairs: Pair<Int, T>): MappingBitField<T> =
            MappingBitField(this, pairs.toList())
    }

    init {
        check(pairs.map { it.first }.distinct().size == pairs.size) { "Duplicate keys" }
        check(pairs.map { it.second }.distinct().size == pairs.size) { "Duplicate values" }
    }

    private val map: Map<Int, T> = pairs.toMap()
    private val inverseMap = pairs.associateBy({ it.second }, { it.first })

    fun get(byte: Int): T = map.getValue(bitField.get(byte))

    fun set(byte: Int, value: T): Int = bitField.set(byte, inverseMap.getValue(value))

    fun get(byte: UByte): T = get(byte.toInt())

    fun set(byte: UByte, value: T): UByte = set(byte.toInt(), value).toUByte()

    fun get(byte: UShort): T = get(byte.toInt())

    fun set(byte: UShort, value: T): UShort = set(byte.toInt(), value).toUShort()

    fun get(int: UInt): T = get(int.toInt())

    fun set(int: UInt, value: T): UInt = set(int.toInt(), value).toUInt()

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
