plugins {
    kotlin("jvm") version "1.9.20"
    application
}

group = "com.aihm"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
}

application {
    mainClass.set("com.aihm.dataprep.MainKt")
}

kotlin {
    jvmToolchain(11)
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "com.aihm.dataprep.MainKt"
    }
}
