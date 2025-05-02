import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
  kotlin("jvm")
  kotlin("plugin.serialization")
  id("com.ncorti.ktfmt.gradle")
  id("com.github.johnrengelman.shadow") version "8.1.1"
  application
}

application { mainClass.set("uk.co.coroutines.enviropi.client.MainKt") }

kotlin {
  compilerOptions {
    jvmTarget.set(JVM_17)
    moduleName = "EnviroPi.client.main"
  }
}

tasks.withType<Test> { useJUnitPlatform() }

dependencies {
  implementation(libs.bundles.kotlinx.datetime)
  implementation(libs.bundles.kotlinx.serialization)
  implementation(libs.bundles.kotlinx.coroutines)
  implementation(libs.bundles.tinylog)
  implementation("com.diozero:diozero-core:1.4.1")

  testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}
