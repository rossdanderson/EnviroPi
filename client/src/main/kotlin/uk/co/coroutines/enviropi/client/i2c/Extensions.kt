package uk.co.coroutines.enviropi.client.i2c

import kotlin.reflect.KProperty

interface FieldMapping {
    val bitValue: UInt
}

interface IBitField<N, O> {

    val mask: UInt
    val trailingZeros: Int

    val toUInt: O.() -> UInt
    val fromUInt: UInt.() -> O

    operator fun getValue(register: IRegister<N>, property: KProperty<*>): O

    operator fun setValue(register: IMutableRegister<N>, property: KProperty<*>, value: O)
}

class BitFieldMask<N> private constructor(
    mask: N,
    override val toUInt: N.() -> UInt,
    override val fromUInt: UInt.() -> N,
) : IBitField<N, N> {

    companion object {
        fun IRegister<UByte>.mask(mask: UByte): IBitField<UByte, UByte> =
            BitFieldMask(mask, UByte::toUInt, UInt::toUByte)

        fun IRegister<UShort>.mask(mask: UShort): IBitField<UShort, UShort> =
            BitFieldMask(mask, UShort::toUInt, UInt::toUShort)

        fun IRegister<UInt>.mask(mask: UInt): IBitField<UInt, UInt> =
            BitFieldMask(mask, UInt::toUInt, UInt::toUInt)
    }

    override val mask = mask.toUInt()
    private val invMask = this.mask.inv()
    override val trailingZeros: Int = mask.toUInt().countTrailingZeroBits()

    override operator fun getValue(register: IRegister<N>, property: KProperty<*>): N =
        get(register.value.toUInt()).fromUInt()

    override operator fun setValue(register: IMutableRegister<N>, property: KProperty<*>, value: N) {
        register.value = set(register.value.toUInt(), value.toUInt()).fromUInt()
    }

    fun get(int: UInt): UInt = int and mask shr trailingZeros

    fun set(int: UInt, bits: UInt): UInt = int and invMask or (bits shl trailingZeros and mask)
}

class FocussedBitField<N, O, T> private constructor(
    private val bitField: IBitField<N, O>,
    private val from: O.() -> T,
    private val to: T.() -> O,
) : IBitField<N, T> {

    companion object {
        @JvmName("focusByteUShortUByte")
        fun <N> IBitField<N, UShort>.asByte(): IBitField<N, UByte> =
            FocussedBitField(this, UShort::toUByte, UByte::toUShort)

        @JvmName("focusByteUIntUByte")
        fun <N> IBitField<N, UInt>.asByte(): IBitField<N, UByte> =
            FocussedBitField(this, UInt::toUByte, UByte::toUInt)

        fun <N> IBitField<N, UInt>.asShort(): IBitField<N, UShort> =
            FocussedBitField(this, UInt::toUShort, UShort::toUInt)
    }

    // TODO Check mask is the appropriate size
    override val mask by bitField::mask
    override val trailingZeros by bitField::trailingZeros

    override val toUInt: T.() -> UInt = { bitField.toUInt(to()) }
    override val fromUInt: UInt.() -> T = { bitField.fromUInt(this).from() }

    override fun getValue(register: IRegister<N>, property: KProperty<*>): T =
        bitField.getValue(register, property).from()

    override fun setValue(register: IMutableRegister<N>, property: KProperty<*>, value: T) {
        bitField.setValue(register, property, value.to())
    }
}

class ByteSwappingBitField<N> private constructor(private val bitField: IBitField<N, UShort>) :
    IBitField<N, UShort> by bitField {
    companion object {
        /**
         * Swap the byte order within a [UShort]
         * 1111111100000000 will become 0000000011111111
         */
        fun <N> IBitField<N, UShort>.swapBytes(): IBitField<N, UShort> = ByteSwappingBitField(this)
    }

    override fun getValue(register: IRegister<N>, property: KProperty<*>): UShort =
        swapBytes(bitField.getValue(register, property).toUInt()).toUShort()

    override fun setValue(register: IMutableRegister<N>, property: KProperty<*>, value: UShort) {
        bitField.setValue(register, property, swapBytes(value.toUInt()).toUShort())
    }

    private fun swapBytes(bytes: UInt): UInt = bytes shr 8 or (bytes and 0xFFu shl 8)
}

class LookupBitField<N, O, T : Any?> private constructor(
    private val bitField: IBitField<N, O>,
    pairs: Collection<Pair<UInt, T>>,
) : IBitField<N, T> {
    companion object {
        inline fun <N, O, reified T> IBitField<N, O>.lookup(): LookupBitField<N, O, T>
                where T : FieldMapping, T : Enum<T> =
            lookup(enumValues<T>().toList())

        fun <N, O, T : FieldMapping> IBitField<N, O>.lookup(mappings: Collection<T>): LookupBitField<N, O, T> =
            LookupBitField(this, mappings.map { it.bitValue to it })

        fun <N, O, T : Any?> IBitField<N, O>.lookup(vararg pairs: Pair<UInt, T>): LookupBitField<N, O, T> =
            LookupBitField(this, pairs.toList())

        fun <N, O> IBitField<N, O>.asBoolean(): LookupBitField<N, O, Boolean> {
            check(mask.countOneBits() == 1)
            return lookup(
                1u to true,
                0u to false
            )
        }
    }

    init {
        check(pairs.map { it.first }.distinct().size == pairs.size) { "Duplicate keys" }
        check(pairs.map { it.second }.distinct().size == pairs.size) { "Duplicate values" }
    }

    private val map: Map<UInt, T> = pairs.toMap()
    private val inverseMap = pairs.associateBy({ it.second }, { it.first })

    override val mask by bitField::mask
    override val trailingZeros by bitField::trailingZeros

    override val toUInt: T.() -> UInt = { inverseMap.getValue(this) }
    override val fromUInt: UInt.() -> T = { map.getValue(this) }

    override operator fun getValue(register: IRegister<N>, property: KProperty<*>): T {
        return map.getValue(bitField.toUInt(bitField.getValue(register, property)))
    }

    override operator fun setValue(register: IMutableRegister<N>, property: KProperty<*>, value: T) {
        bitField.setValue(register, property, bitField.fromUInt(inverseMap.getValue(value)))
    }
}
