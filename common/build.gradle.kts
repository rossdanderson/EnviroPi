plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

dependencies {
    api(libs.bundles.kotlinx.datetime)
    api(libs.bundles.kotlinx.serialization)
}
