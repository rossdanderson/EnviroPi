import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
        testRuns["test"].executionTask.configure {
            useJUnitPlatform()
        }
    }

    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(project(":common"))
                implementation(libs.bundles.ktor.server)
                implementation(libs.bundles.kotlinx.serialization)
                implementation(libs.bundles.kotlinx.coroutines)
            }
        }
    }
}
