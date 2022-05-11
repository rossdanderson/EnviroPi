package uk.co.coroutines.enviropi

import com.diozero.api.I2CDevice
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KMutableProperty0
import kotlin.reflect.KProperty
import kotlin.reflect.KProperty0

fun trailingZeros(value: Int, bitWidth: Int): Int {
    repeat(bitWidth) { i ->
        val mask = 1 shl i
        if (value and mask == mask) return i
    }
    return bitWidth
}

fun Register.bitField(mask: Int) = BitFieldDelegate(mask, ::byte)

fun MutableRegister.bitField(mask: Int) = MutableBitFieldDelegate(mask, ::byte)

class MutableBitFieldDelegate(private val mask: Int, private val backingProperty: KMutableProperty0<UByte>) :
    BitFieldDelegate(mask, backingProperty) {

    private val invMask = mask.inv()

    operator fun setValue(thisRef: Any?, property: KProperty<*>, uByte: UByte) {
        backingProperty.set(
            (backingProperty.get().toInt() and invMask or (uByte.toInt() shl trailingZeros and mask)).toUByte()
        )
    }
}

open class BitFieldDelegate(private val mask: Int, private val backingProperty: KProperty0<UByte>) {

    val trailingZeros: Int = trailingZeros(mask, 8)

    operator fun getValue(thisRef: Any?, property: KProperty<*>): UByte =
        (backingProperty.get().toInt() and mask shr trailingZeros).toUByte()
}

fun <T : Any?> MutableBitFieldDelegate.withMappings(
    vararg pairs: Pair<UByte, T>,
): ReadWriteProperty<Any?, T> {
    val map = mapOf(*pairs)
    val inverseMap = pairs.associateBy({ it.second }, { it.first })
    return object : ReadWriteProperty<Any?, T> {
        override fun getValue(thisRef: Any?, property: KProperty<*>): T =
            map.getValue(this@withMappings.getValue(thisRef, property))

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            setValue(thisRef, property, inverseMap.getValue(value))
        }
    }
}

fun <T : Any?> BitFieldDelegate.withMappings(
    vararg pairs: Pair<UByte, T>,
): ReadOnlyProperty<Any?, T> {
    val map = mapOf(*pairs)
    return ReadOnlyProperty { thisRef, property: KProperty<*> -> map.getValue(getValue(thisRef, property)) }
}

infix fun UByte.shl(bitCount: Int) = (toInt() shl bitCount).toUByte()
infix fun UByte.shr(bitCount: Int) = (toInt() shl bitCount).toUByte()
