rootProject.name = "EnviroPi"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("ktor", "2.0.1")
            version("kotlinx-serialization", "1.3.3")
            version("kotlinx-coroutines", "1.6.1")
            version("kotlinx-datetime", "0.3.3")

            library("kotlinx-datetime", "org.jetbrains.kotlinx", "kotlinx-datetime")
                .versionRef("kotlinx-datetime")

            library("kotlinx-serialization-json", "org.jetbrains.kotlinx", "kotlinx-serialization-json")
                .versionRef("kotlinx-serialization")

            library("ktor-serialization-kotlinx-json", "io.ktor", "ktor-serialization-kotlinx-json")
                .versionRef("ktor")

            library("ktor-server-core", "io.ktor", "ktor-server-core")
                .versionRef("ktor")
            library("ktor-server-cio", "io.ktor", "ktor-server-cio")
                .versionRef("ktor")
            library("ktor-server-content-negotiation", "io.ktor", "ktor-server-content-negotiation")
                .versionRef("ktor")

            library("ktor-client-core", "io.ktor", "ktor-client-core")
                .versionRef("ktor")
            library("ktor-client-cio", "io.ktor", "ktor-client-cio")
                .versionRef("ktor")
            library("ktor-client-logging", "io.ktor", "ktor-client-logging")
                .versionRef("ktor")
            library("ktor-client-content-negotiation", "io.ktor", "ktor-client-content-negotiation")
                .versionRef("ktor")

            library("kotlinx-coroutines-core", "org.jetbrains.kotlinx", "kotlinx-coroutines-core")
                .versionRef("kotlinx-coroutines")

            bundle(
                "ktor-client",
                listOf(
                    "ktor-serialization-kotlinx-json",
                    "ktor-client-core",
                    "ktor-client-cio",
                    "ktor-client-logging",
                    "ktor-client-content-negotiation",
                )
            )
            bundle(
                "ktor-server", listOf(
                    "ktor-serialization-kotlinx-json",
                    "ktor-server-core",
                    "ktor-server-cio",
                    "ktor-server-content-negotiation",
                )
            )
            bundle("kotlinx-datetime", listOf("kotlinx-datetime"))
            bundle("kotlinx-coroutines", listOf("kotlinx-coroutines-core"))
            bundle("kotlinx-serialization", listOf("kotlinx-serialization-json"))
        }
    }
}

include(
    ":common",
    ":client",
    ":server"
)

