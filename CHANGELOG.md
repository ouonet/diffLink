# Changelog

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

