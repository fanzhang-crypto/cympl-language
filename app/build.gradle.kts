plugins {
    kotlin("jvm") version "1.8.0"
//    application
    java
}

dependencies {
    implementation(project(":interpreter"))
    implementation(project(":domain"))
    implementation(project(":parser-antlr"))
    implementation(project(":parser-fp"))

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

//application {
//    mainClass.set("demo.parser.app.FileBatchExecutorKt")
//}

task("runBatch", JavaExec::class) {
    mainClass.set("demo.parser.app.FileBatchExecutorKt")
    classpath = sourceSets["main"].runtimeClasspath
}

// gradle runInteractive -q --console=plain
task("runInteractive", JavaExec::class) {
    standardInput = System.`in`
    mainClass.set("demo.parser.app.InteractiveExecutorKt")
    classpath = sourceSets["main"].runtimeClasspath
}
