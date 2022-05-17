plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
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
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {

                implementation(project(":common"))
                implementation(libs.bundles.ktor.client)
                implementation(libs.bundles.kotlinx.serialization)
                implementation(libs.bundles.kotlinx.coroutines)
                implementation("com.diozero:diozero-core:1.3.1")
            }
        }
    }
}
