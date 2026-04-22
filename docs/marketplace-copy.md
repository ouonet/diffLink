# Marketplace Copy - DiffLink

## Short description

Open IntelliJ diff directly from `@DiffLink` markers in comments and plain-text files.

## Long description (paste-ready)

DiffLink opens file comparisons directly from inline `@DiffLink` markers.

Add `@DiffLink:path/to/file` or `@DiffLink:source, destination` in a comment or plain-text line, then click the gutter icon to launch IntelliJ IDEA's built-in diff viewer.

DiffLink is designed for refactoring and migration workflows where you frequently compare old and new implementations.

### Highlights

- One-click diff from editor gutter markers
- Single-target and explicit two-file compare formats
- Works across common text-based files (Java, Kotlin, Python, JavaScript, Markdown, XML, shell, config)
- Clear error markers for missing or invalid paths
- Local-only behavior: no source upload, no remote diff processing

### Compatibility

- IntelliJ IDEA Community and Ultimate
- Build 232+ (2023.2.1 and later)
- No `until-build` cap in plugin metadata

### Marker examples

```text
@DiffLink:src/main/java/legacy/OldImplementation.java
@DiffLink:/Users/me/project/v1/File.kt, /Users/me/project/v2/File.kt
```

## Screenshot captions and upload order

Upload these files in this order in the Marketplace media section:

1. `src/main/resources/01-marker-in-editor.png`
   - Caption: Marker detected in editor with clickable gutter icon.
2. `src/main/resources/02-diff-viewer-result.png`
   - Caption: One-click launch of IntelliJ's built-in diff viewer.
3. `src/main/resources/03-invalid-path-feedback.png`
   - Caption: Clear error feedback for invalid or missing target paths.

If you want inline images in the Marketplace long description, use tag-pinned public URLs:

- `https://raw.githubusercontent.com/ouonet/diffLink/1.0.0/src/main/resources/01-marker-in-editor.png`
- `https://raw.githubusercontent.com/ouonet/diffLink/1.0.0/src/main/resources/02-diff-viewer-result.png`
- `https://raw.githubusercontent.com/ouonet/diffLink/1.0.0/src/main/resources/03-invalid-path-feedback.png`

## Listing tags (suggested)

`diff`, `refactoring`, `navigation`, `productivity`, `comparison`

