plugins {
    kotlin("jvm")
    antlr
}

dependencies {
    implementation(project(":domain"))
    implementation("org.antlr:antlr4:4.11.1")
    antlr("org.antlr:antlr4:4.11.1")

    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core-jvm:5.1.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

kotlin {
    jvmToolchain(17)
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
tasks.compileTestKotlin {
    dependsOn(tasks.generateGrammarSource)
}
tasks.generateGrammarSource {
    arguments = arguments + listOf("-visitor", "-long-messages")
}
