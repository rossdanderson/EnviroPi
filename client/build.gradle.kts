plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("uk.co.coroutines.enviropi.client.MainKt")
}

kotlin {
    targets.all {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
                }
            }
        }
    }

    jvm {
        withJava()
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
                implementation(libs.bundles.tinylog)
                implementation("com.diozero:diozero-core:1.4.0")
                implementation("org.jetbrains.kotlinx:kandy-api:0.6.0")
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter:5.8.1")
            }
        }
    }
}
