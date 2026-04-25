# Changelog

## [1.1.1] - 2026-04-25

### Fixed
- Allowed git refs containing `~` in `git://` expressions, so history targets like `@DiffLink: git://HEAD~1:path/to/File.java` resolve correctly.
- Restored `@DiffLink` detection inside multiline block comments using `/* ... */`, not only JavaDoc-style `/** ... */` comments.

## [1.1.0] - 2026-04-24

### Added
- Git history comparison via `git://` syntax.
  Use `@DiffLink: git://ref:path` to compare the current file against any git revision,
  branch, or tag without leaving the editor.
  - Single target: `@DiffLink: git://HEAD~1:src/main/kotlin/Foo.kt`
  - Two-target: `@DiffLink: git://main:src/Foo.kt, git://dev:src/Foo.kt`
  - Short SHA: `@DiffLink: git://abc1234:src/Foo.kt`
  Requires `git` on the system `PATH`; runs `git show ref:path` via a subprocess.

## [1.0.7] - 2026-04-23

### Fixed
- Eliminated duplicate gutter icons in HTML files caused by `HTMLLanguage` extending `XMLLanguage`.
  The XML provider now skips files whose language is an XML sub-dialect, deferring to the
  more specific (HTML) provider instead.
- Restored gutter icon visibility on first file open after the language-hierarchy fix.
  Per-batch deduplication state is no longer persisted across render cycles.

## [1.0.6] - 2026-04-23

### Added
- Support for 30+ languages via explicit `codeInsight.lineMarkerProvider` registrations:
  Java, Kotlin, Groovy, Scala, Python, Ruby, PHP, Lua, C, C++, Go, Rust, Objective-C,
  Swift, JavaScript, TypeScript, HTML, CSS, SCSS, Vue, XML, JSON, YAML, TOML, Properties,
  INI, Dockerfile, plain text, Markdown, Shell Script, SQL.

### Fixed
- Plugin installation crash (`PluginException: No key specified for extension`) caused by
  missing or invalid `language` attribute on `lineMarkerProvider` registrations.
- Duplicate gutter icons from IntelliJ's two-phase line marker rendering (visible vs.
  non-visible element batches).
- Duplicate gutter icons from language injection (e.g. JavaScript inside HTML `<script>`
  tags); injected PSI fragments are now skipped via `InjectedLanguageManager`.
- Gutter icons not appearing in Go files (GoLand), Python files, and other non-JVM
  languages due to PSI comment type filtering; replaced with language-agnostic raw
  line-text scan.

## [1.0.5] - 2026-04-23

### Changed
- Prefer file-backed diff content so files opened by DiffLink are editable directly in IntelliJ's diff viewer.
- Keep a text-based fallback when file-backed content cannot be created, preserving compatibility for edge-case paths.
- Updated Plugin Verifier targets to include latest available IntelliJ IDEA 2025.2.6.1 builds (IC/IU).

## [1.0.2] - 2026-04-22

### Fixed
- Removed `until-build` upper bound so the plugin is compatible with IntelliJ IDEA 2023.2 and all later versions (including 2025.2+).

## [1.0.1] - 2026-04-22

### Changed
- Expanded plugin description metadata for Marketplace with richer usage details.
- Embedded public screenshot URLs in plugin description so local ZIP installs can preview images.
- Bumped plugin version to 1.0.1 for republishing.

## [1.0.0] - 2026-04-22

### Added
- Initial Marketplace-ready release pipeline for DiffLink.
- Community-compatible IntelliJ IDEA packaging and plugin verification targets.
- Plugin icon assets and GitHub Actions workflows for CI and release publication.

### Changed
- Standardized the plugin ID and published metadata to `com.neo.difflink`.
- Centralized version, platform, and release settings in `gradle.properties`.
- Updated the documentation to match the current package structure and release process.
- Switched release automation and media references to semver tag `1.0.0`.
