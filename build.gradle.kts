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
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

intellij {
    version.set("2023.2.1")
    type.set("IU")
    plugins.set(listOf("java"))
    updateSinceUntilBuild.set(false)
}

tasks {
    test {
        useJUnitPlatform()
    }
}
