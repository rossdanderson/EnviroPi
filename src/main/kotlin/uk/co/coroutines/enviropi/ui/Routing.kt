package uk.co.coroutines.enviropi.ui

import io.ktor.server.application.Application
import io.ktor.server.jte.JteContent
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.Instant
import org.jetbrains.kotlinx.dataframe.api.add
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf
import org.jetbrains.kotlinx.dataframe.api.max
import org.jetbrains.kotlinx.dataframe.api.min
import org.jetbrains.kotlinx.dataframe.api.toColumn
import org.tinylog.kotlin.Logger.info
import uk.co.coroutines.enviropi.Data

fun Application.routing(state: StateFlow<List<Data>>) {
  routing {
    get("/") { call.respond(JteContent("index.kte", emptyMap())) }
    get("/table") {
      val dataList = state.value

      val _time = mutableListOf<Instant>()
      val _temperature = mutableListOf<Double>()
      val _humidity = mutableListOf<Double>()
      val _lux = mutableListOf<Double>()
      val _pressure = mutableListOf<Double>()

      dataList.forEach {
        _time.add(it.instant)
        _temperature.add(it.temperature)
        _humidity.add(it.humidity)
        _lux.add(it.lux)
        _pressure.add(it.pressure)
      }

      val time = _time.toColumn("time")
      val temperature = _temperature.toColumn("temperature")
      val humidity = _humidity.toColumn("humidity")
      val lux = _lux.toColumn("lux")
      val pressure = _pressure.toColumn("pressure")

      val duration = (time.max() - time.min()) / 100

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
                              "temperature-half",
                              "--wa-color-red-50",
                              "Temperature",
                              "°C",
                              df,
                              temperature
                          ),
                          Card(
                              "droplet",
                              "--wa-color-blue-60",
                              "Humidity",
                              "%",
                              df,
                              humidity
                          ),
                          Card("sun", "--wa-color-yellow-80", "Light", "lux", df, lux),
                          Card(
                              "gauge",
                              "--wa-color-green-60",
                              "Pressure",
                              "hPa",
                              df,
                              pressure
                          )
                      ))))
    }
  }
}
