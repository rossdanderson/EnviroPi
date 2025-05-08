package uk.co.coroutines.enviropi.ui

import io.ktor.server.application.Application
import io.ktor.server.jte.JteContent
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.max
import org.jetbrains.kotlinx.dataframe.api.min
import org.jetbrains.kotlinx.dataframe.api.toColumnOf
import org.jetbrains.kotlinx.dataframe.api.toDataFrame
import org.tinylog.kotlin.Logger.info
import uk.co.coroutines.enviropi.Data

val timeKey = "timeKey".toColumnOf<Instant>()

fun Application.routing(state: StateFlow<List<Data>>) {
  routing {
    get("/") { call.respond(JteContent("index.kte", emptyMap())) }
    get("/table") {
      val dataList = state.value.toDataFrame()

      val time = dataList[Data::instant]
      val duration = (time.max() - dataList[Data::instant].min()) / 100
      val df = dataList.add(timeKey) { get(time).roundDownTo(duration) }

      call.respond(
          JteContent(
              "card-grid.kte",
              mapOf(
                  "cards" to
                      listOf(
                          Card(
                              "temperature-half",
                              "--wa-color-red-50",
                              "Temperature",
                              "°C",
                              df,
                              dataList[Data::temperature]),
                          Card(
                              "droplet",
                              "--wa-color-blue-60",
                              "Humidity",
                              "%",
                              df,
                              dataList[Data::humidity]),
                          Card(
                              "sun",
                              "--wa-color-yellow-80",
                              "Light",
                              "lux",
                              df,
                              dataList[Data::lux]),
                          Card(
                              "gauge",
                              "--wa-color-green-60",
                              "Pressure",
                              "hPa",
                              df,
                              dataList[Data::pressure])))))
    }
  }
}
