plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.ideaplugins"
version = "0.0.1"

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
}

intellij {
    version.set("2023.2.1")
    type.set("IU")
    plugins.set(listOf("java"))
}

tasks {
    test {
        useJUnitPlatform()
    }
}
