rootProject.name = "EnviroPi"

dependencyResolutionManagement {
  versionCatalogs {
    create("libs") {
      version("ktor", "3.1.2")
      version("kotlinx-coroutines", "1.10.2")
      version("kotlinx-datetime", "0.6.2")
      version("tinylog", "2.7.0")

      library("slf4j-tinylog", "org.tinylog", "slf4j-tinylog").versionRef("tinylog")
      library("tinylog-api-kotlin", "org.tinylog", "tinylog-api-kotlin").versionRef("tinylog")
      library("tinylog-impl", "org.tinylog", "tinylog-impl").versionRef("tinylog")

      library("kotlinx-datetime", "org.jetbrains.kotlinx", "kotlinx-datetime")
          .versionRef("kotlinx-datetime")

      library("ktor-server-core", "io.ktor", "ktor-server-core").versionRef("ktor")
      library("ktor-server-cio", "io.ktor", "ktor-server-cio").versionRef("ktor")
      library("ktor-server-jte", "io.ktor", "ktor-server-jte").versionRef("ktor")

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
          "ktor-server",
          listOf(
              "ktor-server-core",
              "ktor-server-cio",
              "ktor-server-jte",
          ))
      bundle("kotlinx-datetime", listOf("kotlinx-datetime"))
      bundle("kotlinx-coroutines", listOf("kotlinx-coroutines-core"))
    }
  }
}
