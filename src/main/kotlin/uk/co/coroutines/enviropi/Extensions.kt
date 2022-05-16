package uk.co.coroutines.enviropi

import uk.co.coroutines.enviropi.LookupBitField.Companion.withLookup
import kotlin.reflect.KProperty

interface FieldMapping {
    val value: UInt
}

interface IBitField {
    operator fun getValue(register: ByteRegister, property: KProperty<*>): UByte

    operator fun setValue(register: MutableByteRegister, property: KProperty<*>, bits: UByte)

    operator fun getValue(register: ShortRegister, property: KProperty<*>): UShort

    operator fun setValue(register: MutableShortRegister, property: KProperty<*>, bits: UShort)

    operator fun getValue(register: IntRegister, property: KProperty<*>): UInt

    operator fun setValue(register: MutableIntRegister, property: KProperty<*>, bits: UInt)
}

class BitField private constructor(private val mask: UInt) : IBitField {

    companion object {

        fun ByteRegister.bitField(mask: UByte) = BitField(mask.toUInt())

        fun ShortRegister.bitField(mask: UShort) = BitField(mask.toUInt())

        fun IntRegister.bitField(mask: UInt) = BitField(mask)

        fun bitFlag(mask: UInt): LookupBitField<Boolean> {
            check(mask.countOneBits() == 1)
            return BitField(mask)
                .withLookup(
                    1u to true,
                    0u to false
                )
        }
    }

    private val trailingZeros: Int = mask.countTrailingZeroBits()
    private val invMask = mask.inv()

    override operator fun getValue(register: ByteRegister, property: KProperty<*>): UByte = get(register.value)

    override operator fun setValue(register: MutableByteRegister, property: KProperty<*>, bits: UByte) {
        register.value = set(register.value, bits)
    }

    override operator fun getValue(register: ShortRegister, property: KProperty<*>): UShort = get(register.value)

    override operator fun setValue(register: MutableShortRegister, property: KProperty<*>, bits: UShort) {
        register.value = set(register.value, bits)
    }

    override operator fun getValue(register: IntRegister, property: KProperty<*>): UInt = get(register.value)

    override operator fun setValue(register: MutableIntRegister, property: KProperty<*>, bits: UInt) {
        register.value = set(register.value, bits)
    }

    fun get(byte: UByte): UByte = get(byte.toUInt()).toUByte()

    fun set(byte: UByte, bits: UByte): UByte = set(byte.toUInt(), bits.toUInt()).toUByte()

    fun get(short: UShort): UShort = get(short.toUInt()).toUShort()

    fun set(short: UShort, bits: UShort): UShort = set(short.toUInt(), bits.toUInt()).toUShort()

    fun get(int: UInt): UInt = int and mask shr trailingZeros

    fun set(int: UInt, bits: UInt): UInt = int and invMask or (bits shl trailingZeros and mask)
}

class ByteSwappingBitField private constructor(private val bitField: BitField) : IBitField {
    companion object {
        fun BitField.swapBytes(): ByteSwappingBitField = ByteSwappingBitField(this)
    }

    override fun getValue(register: ByteRegister, property: KProperty<*>): UByte =
        bitField.getValue(register, property)

    override fun setValue(register: MutableByteRegister, property: KProperty<*>, bits: UByte) {
        bitField.setValue(register, property, bits)
    }

    override operator fun getValue(register: ShortRegister, property: KProperty<*>): UShort = get(register.value)

    override operator fun setValue(register: MutableShortRegister, property: KProperty<*>, bits: UShort) {
        TODO()
    }

    override operator fun getValue(register: IntRegister, property: KProperty<*>): UInt = get(register.value)

    override operator fun setValue(register: MutableIntRegister, property: KProperty<*>, bits: UInt) {
        TODO()
    }

    private fun get(bytes: UShort): UShort = get(bytes.toUInt()).toUShort()

    private fun get(bytes: UInt): UInt = bytes shr 8 or (bytes and 0xFFu shl 8)
}

class LookupBitField<T : Any?> private constructor(
    private val bitField: IByteBitField,
    pairs: Collection<Pair<UInt, T>>,
) {
    companion object {
        inline fun <reified T> IByteBitField.withEnum(): LookupBitField<T>
                where T : FieldMapping, T : Enum<T> =
            withLookup(enumValues<T>().toList())

        fun <T : FieldMapping> IByteBitField.withLookup(mappings: Collection<T>): LookupBitField<T> =
            LookupBitField(this, mappings.map { it.value to it })

        fun <T : Any?> IByteBitField.withLookup(vararg pairs: Pair<UInt, T>): LookupBitField<T> =
            LookupBitField(this, pairs.toList())
    }

    init {
        check(pairs.map { it.first }.distinct().size == pairs.size) { "Duplicate keys" }
        check(pairs.map { it.second }.distinct().size == pairs.size) { "Duplicate values" }
    }

    private val map: Map<UInt, T> = pairs.toMap()
    private val inverseMap = pairs.associateBy({ it.second }, { it.first })

    operator fun getValue(register: ByteRegister, property: KProperty<*>): T =
        map.getValue(bitField.getValue(register, property).toUInt())

    operator fun setValue(register: MutableByteRegister, property: KProperty<*>, value: T) {
        bitField.setValue(register, property, inverseMap.getValue(value).toUByte())
    }

    operator fun getValue(register: ShortRegister, property: KProperty<*>): T =
        map.getValue(bitField.getValue(register, property).toUInt())

    operator fun setValue(register: MutableShortRegister, property: KProperty<*>, value: T) {
        bitField.setValue(register, property, inverseMap.getValue(value).toUShort())
    }

    operator fun getValue(register: IntRegister, property: KProperty<*>): T =
        map.getValue(bitField.getValue(register, property))

    operator fun setValue(register: MutableIntRegister, property: KProperty<*>, value: T) {
        bitField.setValue(register, property, inverseMap.getValue(value))
    }
}
