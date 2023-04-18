plugins {
    kotlin("jvm")
    id("com.gorylenko.gradle-git-properties") version "2.4.1"
    id("org.springframework.boot") version "3.0.1"
    id("org.graalvm.buildtools.native") version "0.9.17"
}

group = parent!!.group
version = parent!!.version

dependencies {
    implementation(project(":analyzer"))
    implementation(project(":interpreter"))
    implementation(project(":compiler"))
    implementation(project(":language"))
    implementation(project(":parser-common"))
    implementation(project(":parser-antlr"))
    implementation(project(":parser-fp"))

    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.springframework.shell:spring-shell-starter:3.0.1")
}

kotlin {
    jvmToolchain(17)
}

springBoot {
    buildInfo()
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("cympl")
        }
    }
}

tasks.bootJar {
    manifest {
        attributes(
            "Implementation-Version" to version
        )
    }
    archiveFileName.set("cli.jar")
}

// gradle runInteractive -q --console=plain
tasks.bootRun {
    standardInput = System.`in`
}

