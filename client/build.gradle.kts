import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.session.SessionHandler
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("com.ncorti.ktfmt.gradle")
  id("org.hidetake.ssh") version "2.11.2"
  application
}

application { mainClass.set("io.ktor.server.cio.EngineMain") }

kotlin {
  compilerOptions {
    jvmTarget.set(JVM_17)
  }
}

tasks.withType<Test> { useJUnitPlatform() }

dependencies {
  implementation(libs.bundles.kotlinx.datetime)
//  implementation(libs.bundles.kotlinx.serialization)
  implementation(libs.bundles.kotlinx.coroutines)
  implementation(libs.bundles.tinylog)
  implementation(libs.bundles.ktor.server)
  implementation("com.diozero:diozero-core:1.4.1")

  testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

val deploy by
    tasks.registering {
      dependsOn(tasks.distTar)

      val tarFile = tasks.distTar.map { it.outputs.files.singleFile }

      doLast {
        ssh.run(
            delegateClosureOf<RunHandler> {
              session(
                  Remote(
                      mutableMapOf<String, Any>(
                          "host" to "enviropi",
                          "user" to "pi",
                      )),
                  delegateClosureOf<SessionHandler> {
                    val tar = tarFile.get()
                    put(
                        hashMapOf(
                            "from" to tar.path.toString(),
                            "into" to "/home/pi",
                        ),
                    )
                    execute("tar -xvf /home/pi/${tar.name}")
                  },
              )
            },
        )
      }
    }
