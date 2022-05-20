package uk.co.coroutines.enviropi.common

import kotlinx.serialization.json.Json

const val serverHost = ""
const val serverPort = 443

val jsonConfig = Json {
    prettyPrint = true
}
