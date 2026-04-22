# DiffLink

DiffLink lets you open IntelliJ's diff viewer from an inline `@DiffLink` marker.
It is built for refactoring and migration workflows where you need to compare old and new files quickly without leaving the editor.

## Quick start (30 seconds)

1. Add a marker to a comment or plain-text line.
2. Click the gutter icon next to the marker.
3. Review the comparison in IntelliJ's built-in diff viewer.

```java
// @DiffLink:src/main/java/legacy/OldImplementation.java
public class NewImplementation {
}
```

### Two-file compare

```kotlin
// @DiffLink:/Users/me/project/v1/Feature.kt, /Users/me/project/v2/Feature.kt
class Feature
```

## Compatibility

- **IDE products:** IntelliJ IDEA Community and Ultimate
- **Minimum build:** `232` (IntelliJ IDEA 2023.2.1)
- **Upper build limit:** none (`since-build="232"`, no `until-build`)
- **Current verifier targets:** `IC-232.9559.62` and `IU-252.26830.84`

## Marker format

- Single target: `@DiffLink:path/to/file`
- Explicit source + destination: `@DiffLink:source/path, destination/path`

Path behavior:
- Relative paths resolve from project root.
- Absolute paths are treated as filesystem paths.
- Leading and trailing whitespace is trimmed.
- Unsafe path traversal (`..`, `~`) and external URLs are rejected.

## FAQ

### Why does install fail with a build compatibility error?

This plugin now publishes with `since-build="232"` and no upper cap.
If you still see a `232.*` compatibility error, you are likely installing an older ZIP build.
Rebuild or download the latest artifact.

### Does this plugin send code or file content to external services?

No. DiffLink resolves local paths and opens IntelliJ's local diff viewer.
It does not include telemetry or remote content upload.

### Which file types are supported?

Any text-based file where `@DiffLink` can be detected, including Java, Kotlin, Python, JavaScript, Markdown, XML, shell scripts, and config files.

## Local validation

```bash
./gradlew --no-daemon test
./gradlew --no-daemon buildPlugin
./gradlew --no-daemon runPluginVerifier
./gradlew --no-daemon runPluginVerifier -PpluginVerifierIdeVersions=IU-252.26830.84
```

The plugin ZIP is generated in `build/distributions/`.

## Release workflow

- `patchPluginXml` injects version, since-build, and release notes from `CHANGELOG.md`.
- `signPlugin` uses Marketplace signing secrets.
- `publishPlugin` uploads to JetBrains Marketplace.
- CI and release jobs live in `.github/workflows/`.

Required environment variables:

```bash
export CERTIFICATE_CHAIN='-----BEGIN CERTIFICATE-----...'
export PRIVATE_KEY='-----BEGIN PRIVATE KEY-----...'
export PRIVATE_KEY_PASSWORD='your-password'
export PUBLISH_TOKEN='your-marketplace-token'
```

## Marketplace assets and copy

- Listing copy template: `docs/marketplace-copy.md`
- Screenshot files prepared for listing upload:
  - `src/main/resources/01-marker-in-editor.png`
  - `src/main/resources/02-diff-viewer-result.png`
  - `src/main/resources/03-invalid-path-feedback.png`
- Public image URLs (tag `1.0.0`) for Marketplace inline description:
  - `https://raw.githubusercontent.com/ouonet/diffLink/1.0.0/src/main/resources/01-marker-in-editor.png`
  - `https://raw.githubusercontent.com/ouonet/diffLink/1.0.0/src/main/resources/02-diff-viewer-result.png`
  - `https://raw.githubusercontent.com/ouonet/diffLink/1.0.0/src/main/resources/03-invalid-path-feedback.png`

Note: these screenshots are intended for the Marketplace listing media gallery and are not loaded by the plugin at runtime.

## Support

- Report issues via your repository issue tracker.
- For release approval workflows, use your internal support channel.

## License

See `LICENSE`.

