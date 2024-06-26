package uk.co.coroutines.enviropi.client.i2c

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface IMutableRegister<T> : IRegister<T> {
    override var value: T

    fun flush()
}

@OptIn(ExperimentalContracts::class)
fun <T, R : IMutableRegister<T>> R.write(block: R.() -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
    flush()
}
