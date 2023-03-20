plugins {
    kotlin("jvm")
    id("org.springframework.boot") version "3.0.1"
    id("org.graalvm.buildtools.native") version "0.9.17"
}

dependencies {
    implementation(project(":analyzer"))
    implementation(project(":interpreter"))
    implementation(project(":compiler"))
    implementation(project(":language"))
    implementation(project(":parser-antlr"))
    implementation(project(":parser-fp"))

    implementation("org.apache.commons:commons-text:1.10.0")
    implementation("org.springframework.shell:spring-shell-starter:3.0.1")
}

kotlin {
    jvmToolchain(17)
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
            "Implementation-Version" to parent?.version
        )
    }
}

// gradle runInteractive -q --console=plain
tasks.bootRun {
    standardInput = System.`in`
}
