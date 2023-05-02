plugins {
    kotlin("jvm")
    antlr
}

dependencies {
    implementation(project(":parser-common"))
    implementation("org.antlr:antlr4:4.12.0")
    antlr("org.antlr:antlr4:4.12.0")

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

tasks.compileKotlin {
    dependsOn(tasks.generateGrammarSource)
}
tasks.compileTestKotlin {
    dependsOn(tasks.generateGrammarSource)
}
tasks.generateGrammarSource {
    val generatedSrcRoot = "$buildDir/generated-src/main"
    sourceSets["main"].java.srcDir(generatedSrcRoot)
    outputDirectory = File("$generatedSrcRoot/cympl/parser/antlr")

    arguments = arguments + listOf("-visitor", "-long-messages")
}
