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

//    js(BOTH) {
//        browser {
//            commonWebpackConfig {
//                cssSupport.enabled = true
//            }
//        }
//    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(libs.bundles.kotlinx.datetime)
                api(libs.bundles.kotlinx.serialization)
            }
        }
    }
}
