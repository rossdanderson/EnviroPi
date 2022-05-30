package uk.co.coroutines.enviropi.client.i2c

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface IRegister<T> {
    val value: T

    fun resetCache()
}

@OptIn(ExperimentalContracts::class)
inline fun <T, R: IRegister<T>, O> R.read(block: R.() -> O): O {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    resetCache()
    return block()
}
