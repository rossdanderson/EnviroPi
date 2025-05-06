package uk.co.coroutines.enviropi.client

import com.diozero.util.Diozero
import io.ktor.http.ContentType.Text.Html
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.intellij.lang.annotations.Language
import org.tinylog.Logger.info
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

suspend fun main(args: Array<String>): Unit = coroutineScope {
  val mode = args.getOrNull(0)
  val sensorFactory: ISensorFactory
  val displayFactory: IDisplayFactory
  when (mode) {
    "mock" -> {
      sensorFactory = ISensorFactory.mock
      displayFactory = IDisplayFactory.swing
    }

    else -> {
      sensorFactory = ISensorFactory.default
      displayFactory = IDisplayFactory.default
    }
  }

  val sensor = with(sensorFactory) { create(this@coroutineScope, 1.seconds) }
  val display = with(displayFactory) { create() }.also { info { "Display created" } }

  info { "Launching server" }

  val oneDayData =
      sensor.dataFlow
          .runningFold(persistentListOf<Data>()) { data, value ->
            val earlier = Clock.System.now() - 1.days
            data.mutate {
              if (it.isNotEmpty()) while (it.first().instant < earlier) it.removeFirst()
              it.add(value)
            }
          }
          .drop(1)
          .stateIn(this@coroutineScope)

  launch {
    info { "Outputting sensor to display" }
    sensor.dataFlow.outputTo(display = display)
  }

  embeddedServer(CIO, 8080) {
        routing {
          get("/") {
            call.respondText(contentType = Html) {
              // language=HTML
              """
<!DOCTYPE html>
<html lang='en'>
<head>
    <link rel='stylesheet'
          href='https://early.webawesome.com/webawesome@3.0.0-alpha.12/dist/styles/themes/matter.css'/>
    <link rel='stylesheet'
          href='https://early.webawesome.com/webawesome@3.0.0-alpha.12/dist/styles/utilities.css'/>
    <script type='module'
            src='https://early.webawesome.com/webawesome@3.0.0-alpha.12/dist/webawesome.loader.js'></script>
    <script src='https://unpkg.com/htmx.org@2.0.4'
            integrity='sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+'
            crossorigin='anonymous'></script>

    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>EnviroPi</title>
</head>
<body>
<wa-page disable-navigation-toggle>
    <h1 class='wa-heading-2xl' slot='header'>EnviroPi</h1>
    <div hx-trigger='load' hx-get='/table' hx-swap='outerHTML'></div>
</wa-page>
</body>
</html>"""
                  .trimIndent()
            }
          }
          get("/table") {
            call.respondText(contentType = Html) {
              // language=HTML
              """
<div class='wa-grid' style='--min-column-size: 14rem' hx-trigger='load delay:1s' hx-get='/table' hx-swap='outerHTML'>
    ${format(oneDayData.value, "temperature-half", "--wa-color-red-20", "Temperature", "%.2f" ,"°C", Data::temperature)}
    ${format(oneDayData.value, "droplet", "--wa-color-blue-20", "Humidity", "%.2f","%", Data::humidity)}
    ${format(oneDayData.value, "sun", "--wa-color-yellow-20", "Light", "%.2f", "lux", Data::lux)}
    ${format(oneDayData.value, "gauge", "--wa-color-green-20", "Pressure", "%.2f", "hPa", Data::pressure)}
</div>
          """
                  .trimIndent()
            }
          }
        }
      }
      .apply {
        addShutdownHook {
          runCatching { sensor.close() }
          runCatching { display.close() }
          if (displayFactory.isDiozero || sensorFactory.isDiozero) {
            Diozero.shutdown()
          }
        }
      }
      .startSuspend()
}

@Language("HTML")
private fun format(
  data: List<Data>,
  icon: String,
  colour: String,
  title: String,
  format: String,
  unit: String,
  accessor: (Data) -> Double
) = """
<wa-card with-footer class='card-overview' style='max-width: 20rem'>
<wa-icon name='$icon' style='$colour'></wa-icon>
<h2 class='wa-heading-l'>$title</h2>
<p class='wa-body-xl'>${format.format(accessor(data.last()))}$unit</p>

<div slot='footer' class='wa-split'>
<p class='wa-body-s'>⬇ ${format.format(data.minOf { accessor(it) })}$unit</p>
<p class='wa-body-s'>⬆ ${format.format(data.maxOf { accessor(it) })}$unit</p>
</div>
</wa-card>
"""
        .trimIndent()
