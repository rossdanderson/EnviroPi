import org.hidetake.groovy.ssh.core.Remote
import org.hidetake.groovy.ssh.core.RunHandler
import org.hidetake.groovy.ssh.session.SessionHandler
import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
  kotlin("jvm") version "2.1.20"
  kotlin("plugin.serialization") version "2.1.20"
  id("com.ncorti.ktfmt.gradle") version "0.22.0"
  id("org.hidetake.ssh") version "2.11.2"
  id("gg.jte.gradle") version ("3.2.1")
  application
}

application { mainClass.set("uk.co.coroutines.enviropi.ApplicationKt")

}

kotlin { compilerOptions { jvmTarget.set(JVM_17) } }

tasks.withType<Test> { useJUnitPlatform() }

group = "uk.co.coroutines"

version = "1.0"

repositories { mavenCentral() }

dependencies {
  implementation(libs.bundles.kotlinx.datetime)
  //  implementation(libs.bundles.kotlinx.serialization)
  implementation(libs.bundles.kotlinx.coroutines)
  implementation(libs.bundles.tinylog)
  implementation(libs.bundles.ktor.server)
  implementation("com.diozero:diozero-core:1.4.1")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:0.3.8")
  implementation("gg.jte:jte-kotlin:3.2.1")

  testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

jte {
  generate()
  targetDirectory.set(layout.buildDirectory.dir("generated-sources/jte").get().asFile.toPath())
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
