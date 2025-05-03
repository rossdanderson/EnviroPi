@file:OptIn(DelicateCoroutinesApi::class)

package uk.co.coroutines.enviropi.client

import com.diozero.util.Diozero
import io.ktor.http.ContentType.Text.Html
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.tinylog.Logger.info
import kotlin.time.Duration.Companion.seconds

fun main(args: Array<String>) {

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

  val sensor = GlobalScope.async { with(sensorFactory) { create(GlobalScope, 1.seconds) } }
  val display =
      GlobalScope.async { with(displayFactory) { create() }.also { info { "Display created" } } }

  GlobalScope.launch {
    info { "Outputting sensor to display" }
    sensor.await().dataFlow.outputTo(display = display.await())
  }

  info { "Launching server" }

  embeddedServer(CIO, 8080) {
        routing {
          get("/") {
            info { "Got request" }

            call.respondText(contentType = Html) {
              // language=HTML
              """<!DOCTYPE html>
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
    <div class='wa-stack' style="font-size: 32px;">
        <div class='wa-flank wa-align-items-start'>
            <wa-icon name="temperature-high"></wa-icon>
            <p>${"%.2f °C".format(sensor.await().dataFlow.value.temperature)}</p>
        </div>
        <div class='wa-flank wa-align-items-start'>
            <wa-icon name="droplet"></wa-icon>
            <p>${"%.2f%%".format(sensor.await().dataFlow.value.humidity)}</p>
        </div>
    </div>
</wa-page>
</body>
</html>"""
                  .trimIndent()
            }
          }
        }
      }
      .apply {
        addShutdownHook {
          runBlocking(NonCancellable) {
            runCatching { sensor.await().close() }
            runCatching { display.await().close() }
          }
          if (displayFactory.isDiozero || sensorFactory.isDiozero) {
            Diozero.shutdown()
          }
        }
      }
      .start(wait = true)
}
