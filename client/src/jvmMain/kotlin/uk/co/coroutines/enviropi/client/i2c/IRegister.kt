package uk.co.coroutines.enviropi.client.i2c

interface IRegister<T> {
    val value: T

    fun resetCache()
}

fun <T, R: IRegister<T>, O> R.read(block: R.() -> O): O {
    resetCache()
    return block()
}
