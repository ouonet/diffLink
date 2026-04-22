import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

plugins {
    kotlin("jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.16.0"
}

fun prop(name: String) = providers.gradleProperty(name)

fun requiredProp(name: String): String =
    prop(name).orNull ?: error("Missing Gradle property: $name")

fun csv(value: String): List<String> =
    value.split(',').map(String::trim).filter(String::isNotEmpty)

fun escapeXml(value: String): String = buildString {
    value.forEach { ch ->
        append(
            when (ch) {
                '&' -> "&amp;"
                '<' -> "&lt;"
                '>' -> "&gt;"
                '"' -> "&quot;"
                '\'' -> "&#39;"
                else -> ch
            }
        )
    }
}

fun renderChangeNotes(file: File, version: String): String {
    if (!file.exists()) return "<p>No release notes available.</p>"

    val lines = file.readLines()
    val start = lines.indexOfFirst { it.startsWith("## [$version]") || it.startsWith("## $version") }
    if (start == -1) return "<p>No release notes available.</p>"

    val end = lines.subList(start + 1, lines.size)
        .indexOfFirst { it.startsWith("## [") || it.startsWith("## ") }
        .let { relativeEnd -> if (relativeEnd == -1) lines.size else start + 1 + relativeEnd }

    val section = lines.subList(start + 1, end)

    val html = StringBuilder()
    var inList = false

    fun closeList() {
        if (inList) {
            html.appendLine("</ul>")
            inList = false
        }
    }

    for (rawLine in section) {
        val line = rawLine.trim()
        when {
            line.isBlank() -> closeList()
            line.startsWith("### ") -> {
                closeList()
                html.appendLine("<h3>${escapeXml(line.removePrefix("### "))}</h3>")
            }
            line.startsWith("- ") -> {
                if (!inList) {
                    html.appendLine("<ul>")
                    inList = true
                }
                html.appendLine("<li>${escapeXml(line.removePrefix("- "))}</li>")
            }
            else -> {
                closeList()
                html.appendLine("<p>${escapeXml(line)}</p>")
            }
        }
    }

    closeList()
    return html.toString().trim().ifEmpty { "<p>No release notes available.</p>" }
}

group = requiredProp("pluginGroup")
version = requiredProp("pluginVersion")

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://www.jetbrains.com/intellij-repository/releases") }
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.2.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
}

kotlin {
    jvmToolchain(17)
}

intellij {
    version.set(requiredProp("platformVersion"))
    type.set(requiredProp("platformType"))
    plugins.set(csv(requiredProp("platformPlugins")))
    updateSinceUntilBuild.set(false)
}

tasks {
    withType<JavaCompile>().configureEach {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    withType<KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        version.set(requiredProp("pluginVersion"))
        sinceBuild.set(requiredProp("pluginSinceBuild"))
        untilBuild.set(provider { "" })
        changeNotes.set(provider {
            renderChangeNotes(project.file("CHANGELOG.md"), requiredProp("pluginVersion"))
        })
    }

    runPluginVerifier {
        ideVersions.set(csv(requiredProp("pluginVerifierIdeVersions")))
        doFirst {
            delete(layout.buildDirectory.dir("reports/pluginVerifier"))
        }
    }

    signPlugin {
        certificateChain.set(providers.environmentVariable("CERTIFICATE_CHAIN"))
        privateKey.set(providers.environmentVariable("PRIVATE_KEY"))
        password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(providers.environmentVariable("PUBLISH_TOKEN"))
        channels.set(csv(requiredProp("pluginReleaseChannels")))
        dependsOn(signPlugin)
    }

    test {
        useJUnit()
    }
}
