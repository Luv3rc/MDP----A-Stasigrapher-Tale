plugins {
    id("java")
    id("application")
    id("org.openjfx.javafxplugin") version "0.1.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

group = "it.unicam.cs.mpgc.rpg130268"
version = "1.0-SNAPSHOT"

// 1. Forza Gradle a scaricare e usare il JDK 25 per la compilazione
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

javafx {
    version = "25"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.media")
}

application {
    mainClass.set("it.unicam.cs.mpgc.rpg130268.Main")
}

// Preview functionality is enabled for compilation and execution, including tests
tasks.withType<JavaCompile> {
    options.compilerArgs.add("--enable-preview")
}

// Preview Flags for tests
tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}

// Force the use of JDK 25 for running JavaExec tasks (like the application)
val java25Toolchain = javaToolchains.launcherFor {
    languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaExec> {
    javaLauncher.set(java25Toolchain)
    jvmArgs("--enable-preview")
    standardInput = System.`in`
}