import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform") version "1.6.21" apply false
    kotlin("plugin.serialization") version "1.6.21" apply false
}

allprojects {
    group = "uk.co.coroutines"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
