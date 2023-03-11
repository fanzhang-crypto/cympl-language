plugins {
    kotlin("jvm") version "1.8.0"
}

group = "org.example.parser-demo"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
//        maven { setUrl("https://repo.spring.io/libs-release") }
    }
}
