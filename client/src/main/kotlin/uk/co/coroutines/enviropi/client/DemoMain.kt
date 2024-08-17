package uk.co.coroutines.enviropi.client

import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    exampleData.outputTo(
        display = SwingDisplay(),
        debug = false,
        imageHeight = 500,
        imageWidth = 500
    )
}
