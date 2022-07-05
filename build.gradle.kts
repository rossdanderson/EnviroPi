plugins {
    kotlin("multiplatform") version "1.7.0" apply false
    kotlin("plugin.serialization") version "1.7.0" apply false
}

allprojects {
    group = "uk.co.coroutines"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
