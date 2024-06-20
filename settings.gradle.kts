rootProject.name = "EnviroPi"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            version("ktor", "2.3.11")
            version("kotlinx-serialization", "1.7.0")
            version("kotlinx-coroutines", "1.9.0-RC")
            version("kotlinx-datetime", "0.6.0")
            version("tinylog", "2.4.1")

            library("slf4j-tinylog", "org.tinylog", "slf4j-tinylog")
                .versionRef("tinylog")
            library("tinylog-api-kotlin", "org.tinylog", "tinylog-api-kotlin")
                .versionRef("tinylog")
            library("tinylog-impl", "org.tinylog", "tinylog-impl")
                .versionRef("tinylog")

            library("ktor-healthCheck", "com.github.zensum", "ktor-health-check")
                .version("4ad25dd")

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
            library("ktor-server-cors", "io.ktor", "ktor-server-cors")
                .versionRef("ktor")
            library("ktor-server-metrics-micrometer", "io.ktor", "ktor-server-metrics-micrometer")
                .versionRef("ktor")
            library("ktor-server-content-negotiation", "io.ktor", "ktor-server-content-negotiation")
                .versionRef("ktor")
            library("micrometer-registry-prometheus", "io.micrometer", "micrometer-registry-prometheus")
                .version("1.9.0")

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
                "tinylog",
                listOf(
                    "slf4j-tinylog",
                    "tinylog-api-kotlin",
                    "tinylog-impl",
                ),
            )
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
                    "ktor-server-metrics-micrometer",
                    "ktor-server-core",
                    "ktor-server-cio",
                    "ktor-server-cors",
                    "ktor-server-content-negotiation",
                    "micrometer-registry-prometheus",
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
//    ":server"
)

