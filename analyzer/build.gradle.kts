plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":domain"))
    api("guru.nidi:graphviz-kotlin:0.18.1")
    implementation("ch.qos.logback:logback-classic:1.2.9")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

kotlin {
    jvmToolchain(17)
}
