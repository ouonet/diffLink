# DiffLink

DiffLink is an IntelliJ IDEA plugin that opens file comparisons directly from `@DiffLink` markers in source files, plain text, and documentation.

## Highlights

- Compare the current file against another file with a single marker
- Compare two explicit files with a two-argument marker
- Open IntelliJ IDEA's built-in diff viewer from the gutter
- Show clear error markers for invalid or missing paths
- Work across common text-based formats including Java, Kotlin, Python, JavaScript, Markdown, XML, shell, and config files

## Compatibility

- **Target IDEs:** IntelliJ IDEA Community and Ultimate
- **Verified baseline:** 2023.2.1
- **Marketplace build range:** `232` to `232.*`
- **JDK for builds/tests:** 17

## Usage

### Single target

```java
// @DiffLink:src/main/java/legacy/OldImplementation.java
public class NewImplementation {
}
```

This compares the current file against the target path.

### Explicit source and destination

```kotlin
// @DiffLink:/Users/me/project/v1/Feature.kt, /Users/me/project/v2/Feature.kt
class Feature
```

This compares the two explicit paths instead of using the current file as the source.

### Path rules

- Relative paths are resolved from the current project root
- Absolute paths are used as filesystem paths
- Leading and trailing whitespace is trimmed
- Path traversal (`..`, `~`) and external URLs are rejected

## Local development

```bash
./gradlew test
./gradlew buildPlugin
./gradlew runPluginVerifier
```

The distributable ZIP is generated under `build/distributions/`.

## Release workflow

The Gradle build is wired for Marketplace publication:

- `patchPluginXml` injects version and compatibility metadata
- `runPluginVerifier` checks Community and Ultimate IDE targets
- `signPlugin` reads signing credentials from environment variables
- `publishPlugin` reads the Marketplace token from environment variables
- GitHub Actions workflows in `.github/workflows/` run CI and tagged releases

### Required environment variables

```bash
export CERTIFICATE_CHAIN='-----BEGIN CERTIFICATE-----...'
export PRIVATE_KEY='-----BEGIN PRIVATE KEY-----...'
export PRIVATE_KEY_PASSWORD='your-password'
export PUBLISH_TOKEN='your-marketplace-token'
```

### Manual publication

```bash
./gradlew buildPlugin
./gradlew signPlugin
./gradlew publishPlugin
```

## Project layout

```text
src/main/kotlin/com/neo/difflink/
├── actions/CompareActionHandler.kt
├── markers/CompareMarkerProvider.kt
└── utils/ComparePathResolver.kt

src/main/resources/META-INF/
├── plugin.xml
├── pluginIcon.svg
└── pluginIcon_dark.svg
```

## License

See `LICENSE`.

## Support

Use your repository issue tracker or internal support channel for bug reports and release approval.
