plugins {
    kotlin("jvm")
    id("org.springframework.boot") version "3.0.1"
    id("org.graalvm.buildtools.native") version "0.9.17"
}

dependencies {
    implementation(project(":analyzer"))
    implementation(project(":interpreter"))
    implementation(project(":domain"))
    implementation(project(":parser-antlr"))
    implementation(project(":parser-fp"))

    implementation("org.springframework.shell:spring-shell-starter:3.0.1")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.1.0")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.0.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

kotlin {
    jvmToolchain(17)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
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
