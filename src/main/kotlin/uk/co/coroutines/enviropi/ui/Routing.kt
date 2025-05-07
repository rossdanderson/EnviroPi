package uk.co.coroutines.enviropi.ui

import io.ktor.server.application.Application
import io.ktor.server.jte.JteContent
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.toColumn
import org.tinylog.kotlin.Logger.info
import uk.co.coroutines.enviropi.Data

fun Application.routing(state: StateFlow<List<Data>>) {
  routing {
    get("/") { call.respond(JteContent("index.kte", emptyMap())) }
    get("/table") {
      val instants = state.value.map { it.instant }
      val time = instants.toColumn("time")

      val temperature = state.value.map { it.temperature }.toColumn("temperature")
      val humidity = state.value.map { it.humidity }.toColumn("humidity")
      val lux = state.value.map { it.lux }.toColumn("lux")
      val pressure = state.value.map { it.pressure }.toColumn("pressure")

      val duration = (instants.max() - instants.min()) / 100

      info { "Rounding to $duration" }

      val df =
          dataFrameOf(time, temperature, humidity, lux, pressure).add {
            "timeKey" from { get(time).roundDownTo(duration) }
          }

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
                              df,
                              temperature),
                          Card(
                              state.value,
                              "droplet",
                              "--wa-color-blue-60",
                              "Humidity",
                              "%",
                              df,
                              humidity),
                          Card(state.value, "sun", "--wa-color-yellow-80", "Light", "lux", df, lux),
                          Card(
                              state.value,
                              "gauge",
                              "--wa-color-green-60",
                              "Pressure",
                              "hPa",
                              df,
                              pressure)))))
    }
  }
}
