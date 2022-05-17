package uk.co.coroutines.enviropi.common

import kotlinx.serialization.json.Json

const val serverHost = "192.168.178.80"
const val serverPort = 8989

val jsonConfig = Json {
    prettyPrint = true
}
