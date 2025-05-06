package uk.co.coroutines.enviropi.ui

import io.ktor.server.application.Application
import io.ktor.server.jte.JteContent
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.StateFlow
import uk.co.coroutines.enviropi.Data

fun Application.routing(state: StateFlow<List<Data>>) {
  routing {
    get("/") { call.respond(JteContent("index.kte", emptyMap())) }
    get("/table") {
      call.respond(
          JteContent(
              "card-grid.kte",
              mapOf(
                  "cards" to
                      listOf(
                          Card(
                              state.value,
                              "temperature-half",
                              "--wa-color-red-50",
                              "Temperature",
                              "°C",
                              Data::temperature),
                          Card(
                              state.value,
                              "droplet",
                              "--wa-color-blue-60",
                              "Humidity",
                              "%",
                              Data::humidity),
                          Card(
                              state.value,
                              "sun",
                              "--wa-color-yellow-80",
                              "Light",
                              "lux",
                              Data::lux),
                          Card(
                              state.value,
                              "gauge",
                              "--wa-color-green-60",
                              "Pressure",
                              "hPa",
                              Data::pressure)))))
    }
  }
}
