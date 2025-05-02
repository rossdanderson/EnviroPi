plugins {
    kotlin("multiplatform") version "2.1.20" apply false
    kotlin("jvm") version "2.1.20" apply false
    kotlin("plugin.serialization") version "2.1.20" apply false
    id("com.ncorti.ktfmt.gradle") version "0.22.0" apply false
}

allprojects {
    group = "uk.co.coroutines"
    version = "1.0"

    repositories {
        mavenCentral()
    }
}
