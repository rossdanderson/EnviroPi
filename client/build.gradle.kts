import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("uk.co.coroutines.enviropi.client.DemoMainKt")
}

kotlin {
    compilerOptions {
        jvmTarget.set(JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    implementation(project(":common"))
    implementation(libs.bundles.kotlinx.serialization)
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(libs.bundles.tinylog)
    implementation("com.diozero:diozero-core:1.4.0")
//    implementation("org.jetbrains.kotlinx:kandy-lets-plot:0.6.0")

    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
}
