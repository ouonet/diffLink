plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.ideaplugins"
version = "1.0.0"

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
}

intellij {
    version.set("2023.2")
    type.set("IU")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
