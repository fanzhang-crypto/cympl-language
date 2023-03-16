rootProject.name = "cympl-parser"

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
    "domain",
    "parser-antlr",
    "parser-fp",
    "antlr-sample"
)
