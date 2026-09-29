plugins {
    kotlin("jvm") version "1.9.24"
    application
}

group = "com.aihm"
version = "1.0"

repositories { mavenCentral() }

// No external dependencies — pure Kotlin/JVM, builds offline.
dependencies {
    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.aihm.dataprep.MainKt")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

tasks.jar {
    manifest { attributes["Main-Class"] = "com.aihm.dataprep.MainKt" }
}
