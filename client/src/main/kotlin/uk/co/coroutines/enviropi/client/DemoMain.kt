package uk.co.coroutines.enviropi.client

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    exampleData.outputTo(SwingDisplay(), true)
}