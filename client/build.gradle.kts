import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.21"
    kotlin("plugin.serialization") version "1.6.21"
}

val ktor_version = "2.0.1"

dependencies {
    implementation(project(":common"))
    implementation("io.ktor:ktor-client-core:$ktor_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
    implementation("io.ktor:ktor-client-logging:$ktor_version")
    implementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.kotlinx.serialization)
    implementation(libs.bundles.kotlinx.coroutines)
    implementation(platform("com.diozero:diozero-bom:1.3.1"))
    implementation("com.diozero:diozero-core")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
