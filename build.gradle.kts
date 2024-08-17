plugins {
    kotlin("multiplatform") version "2.0.10" apply false
    kotlin("jvm") version "2.0.10" apply false
    kotlin("plugin.serialization") version "2.0.10" apply false
}

allprojects {
    group = "uk.co.coroutines"
    version = "1.0"

    repositories {
        mavenCentral()
    }
}
