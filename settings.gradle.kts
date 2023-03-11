rootProject.name = "parser-demo"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
    }
}

include(
    "cli",
    "interpreter",
    "domain",
    "parser-antlr",
    "parser-fp",
    "antlr-sample"
)
include("analyzer")
