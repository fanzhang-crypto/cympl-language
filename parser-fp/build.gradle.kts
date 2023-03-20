plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":language"))
    implementation("com.github.h0tk3y.betterParse:better-parse:0.4.4")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.5")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

kotlin {
    jvmToolchain(17)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
