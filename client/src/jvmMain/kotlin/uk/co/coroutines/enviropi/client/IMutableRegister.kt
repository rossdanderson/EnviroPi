package uk.co.coroutines.enviropi.client

interface IMutableRegister<T>: IRegister<T> {
    override var value: T
}
