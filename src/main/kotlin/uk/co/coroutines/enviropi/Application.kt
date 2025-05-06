package uk.co.coroutines.enviropi

import com.diozero.util.Diozero
import gg.jte.ContentType.Html
import gg.jte.TemplateEngine
import gg.jte.resolve.DirectoryCodeResolver
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.jte.*
import kotlinx.collections.immutable.mutate
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.runningFold
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.tinylog.Logger.info
import uk.co.coroutines.enviropi.ui.routing
import java.nio.file.Paths
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

suspend fun main(args: Array<String>): Unit = coroutineScope {
  val mode = args.getOrNull(0)
  val sensorFactory: ISensorFactory
  val displayFactory: IDisplayFactory
  val templateEngine: TemplateEngine
  when (mode) {
    "mock" -> {
      System.setProperty("io.ktor.development", "true")
      sensorFactory = ISensorFactory.mock
      displayFactory = IDisplayFactory.swing
      templateEngine = TemplateEngine.create(DirectoryCodeResolver(Paths.get("src/main/jte")), Html)
    }

    else -> {
      sensorFactory = ISensorFactory.default
      displayFactory = IDisplayFactory.default
      templateEngine = TemplateEngine.createPrecompiled(Html)
    }
  }

  val sensor = with(sensorFactory) { create(this@coroutineScope, 1.seconds) }
  val display = with(displayFactory) { create() }.also { info { "Display created" } }

  info { "Launching server" }

  val data =
      sensor.dataFlow
          .runningFold(persistentListOf<Data>()) { data, value ->
            val now = Clock.System.now()
            val earlier = now - 1.days
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
        install(Jte) { this.templateEngine = templateEngine }

        routing(data)
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
