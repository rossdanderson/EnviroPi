import org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}


kotlin {
    compilerOptions {
        jvmTarget.set(JVM_17)
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named("compileJava", JavaCompile::class.java) {
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf("--patch-module", "EnviroPi.common=${sourceSets["main"].output.asPath}")
    })
}

dependencies {
    api(libs.bundles.kotlinx.datetime)
    api(libs.bundles.kotlinx.serialization)
}
