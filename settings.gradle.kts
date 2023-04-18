rootProject.name = "cympl-language"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
    }
}

include(
    "cli",
    "compiler",
    "analyzer",
    "interpreter",
    "language",
    "parser-antlr",
    "parser-fp",
    "antlr-sample"
)
include("parser-common")
