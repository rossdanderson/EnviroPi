package uk.co.coroutines.enviropi.client

import com.diozero.util.Diozero
import io.ktor.http.*
import io.ktor.http.ContentType.Text.Html
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

data class AppState(
    val sensor: ISensor?,
    val display: IDisplay?,
)

fun Application.module() {

  val _appState = flow {
    val sensorFactory = ISensorFactory.mock
    val displayFactory = IDisplayFactory.swing

    val sensor = with(sensorFactory) { create(1.seconds) }
    val display = with(displayFactory) { create() }

    emit(AppState(sensor, display))

    try {
      awaitCancellation()
    } finally {
      sensor.close()
      display.close()
      if (displayFactory.isDiozero || sensorFactory.isDiozero) {
        Diozero.shutdown()
      }
    }
  }.stateIn(this, SharingStarted.Eagerly, AppState(null, null))

  launch {
    val appState = _appState.await()
    appState.sensor.dataFlow.outputTo(display = appState.display)
  }

  monitor.subscribe(ApplicationStopping) {
    state.sensor?.close()
    state.display?.close()

  }

  routing {
    get("/") {
      val state = state
      if (state.sensor == null) {
        call.respond(HttpStatusCode.ServiceUnavailable, "Sensor not available")
        return@get
      } else {

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
    <div class='wa-stack'>
        <h1>Temperature</h1>
        ${state.sensor.dataFlow.value.temperature}
    </div>
</wa-page>
</body>
</html>"""
              .trimIndent()
        }
      }
    }
  }
}
