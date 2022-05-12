package uk.co.coroutines.enviropi

import uk.co.coroutines.enviropi.MappingDelegate.Companion.withMappings
import kotlin.reflect.KProperty

interface FieldMapping {
    val value: UByte
}

class BitFieldDelegate private constructor(private val mask: Int) {

    companion object {
        fun Register.bitField(mask: Int) = BitFieldDelegate(mask)

        fun Register.bitFlag(mask: Int): MappingDelegate<Boolean> {
            check(mask.countOneBits() == 1)
            return BitFieldDelegate(mask)
                .withMappings(
                    1.toUByte() to true,
                    0.toUByte() to false
                )
        }
    }

    private val trailingZeros: Int = mask.countTrailingZeroBits()
    private val invMask = mask.inv()

    operator fun getValue(register: Register, property: KProperty<*>): UByte =
        (register.value.toInt() and mask shr trailingZeros).toUByte()

    operator fun setValue(register: MutableRegister, property: KProperty<*>, uByte: UByte) {
        register.value = (register.value.toInt() and invMask or (uByte.toInt() shl trailingZeros and mask)).toUByte()
    }

}

class MappingDelegate<T : Any?> private constructor(
    private val bitFieldDelegate: BitFieldDelegate,
    private val pairs: Collection<Pair<UByte, T>>,
) {
    companion object {
        inline fun <reified T> BitFieldDelegate.withEnum(): MappingDelegate<T>
                where T : FieldMapping, T : Enum<T> =
            withMappings(enumValues<T>().toList())

        fun <T : FieldMapping> BitFieldDelegate.withMappings(mappings: Collection<T>): MappingDelegate<T> =
            MappingDelegate(this, mappings.map { it.value to it})

        fun <T : Any?> BitFieldDelegate.withMappings(vararg pairs: Pair<UByte, T>): MappingDelegate<T> =
            MappingDelegate(this, pairs.toList())
    }

    init {
        check(pairs.map { it.first }.distinct().size == pairs.size) { "Duplicate keys" }
        check(pairs.map { it.second }.distinct().size == pairs.size) { "Duplicate values" }
    }


    private val map: Map<UByte, T> = pairs.toMap()
    private val inverseMap = pairs.associateBy({ it.second }, { it.first })

    operator fun getValue(thisRef: Register, property: KProperty<*>): T =
        map.getValue(bitFieldDelegate.getValue(thisRef, property))

    operator fun setValue(thisRef: MutableRegister, property: KProperty<*>, value: T) {
        bitFieldDelegate.setValue(thisRef, property, inverseMap.getValue(value))
    }
}

infix fun UByte.shl(bitCount: Int) = (toInt() shl bitCount).toUByte()
infix fun UByte.shr(bitCount: Int) = (toInt() shl bitCount).toUByte()
