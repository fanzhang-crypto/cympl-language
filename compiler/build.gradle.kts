plugins {
    kotlin("jvm")
//    id("org.graalvm.plugin.truffle-language") version "0.1.0-alpha2"
}

dependencies {
    implementation(project(":language"))
    implementation("org.ow2.asm:asm-commons:9.4")
    implementation("org.ow2.asm:asm-util:9.4")

//    implementation("org.graalvm.truffle:truffle-api:22.3.1")
//    compileOnly("org.graalvm.truffle:truffle-dsl-processor:22.3.1")
//    implementation("org.graalvm.sdk:graal-sdk:22.3.1")

    testImplementation(project(":parser-antlr"))
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
