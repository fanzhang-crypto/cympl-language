plugins {
    kotlin("jvm") version "1.8.0"
}

group = "example.cympl"
version = "1.0-SNAPSHOT"

allprojects {
    repositories {
        mavenLocal()
        mavenCentral()
//        maven { setUrl("https://repo.spring.io/libs-release") }
    }
}
