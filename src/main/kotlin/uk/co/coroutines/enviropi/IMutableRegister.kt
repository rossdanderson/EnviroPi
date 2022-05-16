package uk.co.coroutines.enviropi

interface IMutableRegister<T>: IRegister<T> {
    override var value: T
}