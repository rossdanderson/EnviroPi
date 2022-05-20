package uk.co.coroutines.enviropi.client.i2c

interface IMutableRegister<T> : IRegister<T> {
    override var value: T

    fun flush()
}

fun <T, R : IMutableRegister<T>> R.update(block: R.() -> Unit) {
    block()
    flush()
}
