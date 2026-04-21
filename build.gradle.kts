plugins {
    kotlin("jvm") version property("kotlinVersion").get()
    id("org.jetbrains.intellij") version "1.16.0"
}

group = "com.ideaplugins"
version = property("pluginVersion").get()

repositories {
    mavenCentral()
    google()
}

dependencies {
    testImplementation(kotlin("test"))
}

intellij {
    version.set(property("ideaVersion").get())
    type.set("IU")
}

tasks {
    test {
        useJUnitPlatform()
    }
}
