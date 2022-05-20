plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.cloud.tools.jib") version "3.2.1"
}

kotlin {
    targets.all {
        compilations.all {
            kotlinOptions {
                freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlin.RequiresOptIn"
            }
        }
    }

    jvm {
        withJava()
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(libs.bundles.kotlinx.serialization)
                implementation(libs.bundles.kotlinx.datetime)
                implementation(libs.bundles.kotlinx.coroutines)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.bundles.ktor.server)
                implementation(libs.bundles.ktor.server)
                implementation(libs.bundles.tinylog)
            }
        }
    }

    jib {
        to {
            image = "rossdanderson/enviropi-server:latest"
        }
        container {
            ports = listOf("8080", "9091")
        }
    }
}
