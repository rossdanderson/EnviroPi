package uk.co.coroutines.enviropi.client.i2c

interface IMutableRegister<T>: IRegister<T> {
    override var value: T
}
