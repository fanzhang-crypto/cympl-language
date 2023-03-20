plugins {
    kotlin("jvm")
}

dependencies {
    implementation(project(":language"))
    api("guru.nidi:graphviz-kotlin:0.18.1")
    implementation("ch.qos.logback:logback-classic:1.4.6")

    testImplementation(project(":parser-antlr"))
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.5.5")
    testImplementation("org.springframework.boot:spring-boot-starter-test:3.0.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

kotlin {
    jvmToolchain(17)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}
