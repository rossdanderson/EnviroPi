@file:OptIn(DelicateCoroutinesApi::class)

package uk.co.coroutines.enviropi.client

import com.diozero.util.Diozero
import io.ktor.http.ContentType.Text.Html
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.DelicateCoroutinesApi
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
          href='https://early.webawesome.com/webawesome@3.0.0-alpha.11/dist/styles/themes/default.css'/>
    <link rel='stylesheet' href='https://early.webawesome.com/webawesome@3.0.0-alpha.11/dist/styles/webawesome.css'/>
    <script type='module'
            src='https://early.webawesome.com/webawesome@3.0.0-alpha.11/dist/webawesome.loader.js'></script>
    <script src="https://unpkg.com/htmx.org@2.0.4"
            integrity="sha384-HGfztofotfshcF7+8n44JQL2oJmowVChPTg48S+jvZoztPfvwD79OC/LTtG6dMp+"
            crossorigin="anonymous"></script>
    <meta charset='UTF-8'>
    <meta name='viewport' content='width=device-width, initial-scale=1.0'>
    <title>EnviroPi</title>
</head>
<body>
<wa-page>
<h1 slot='header'>EnviroPi</h1>
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
    <div class='wa-stack' style="font-size: 32px;" hx-trigger='load delay:1s' hx-get='/table'>
        ${format(oneDayData.value, "%.2f °C", "temperature-high", Data::temperature)}
        ${format(oneDayData.value, "%.2f%%", "droplet", Data::humidity)}
        ${format(oneDayData.value, "%.2f", "sun", Data::lux)}
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
private fun format(data: List<Data>, format: String, icon: String, accessor: (Data) -> Double) = """
<div class='wa-flank wa-align-items-start wa-gap-3'>
    <wa-icon name="$icon"></wa-icon>
    <div class='wa-stack wa-gap-1'>
        <div class='wa-text-xl wa-text-center'>${format.format(accessor(data.last()))}</div>
        <div class='wa-flex wa-gap-2 wa-text-sm wa-text-secondary wa-items-center'>
            <div class='wa-min-w-16'>⬇ ${format.format(data.minOf { accessor(it) })}</div>
            <div class='wa-min-w-16'>⬆ ${format.format(data.maxOf { accessor(it) })}</div>
        </div>
    </div>
</div>
""".trimIndent()
