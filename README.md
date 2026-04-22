# DiffLink

An IntelliJ IDEA plugin that enables developers to navigate and compare files during refactoring using special comments.

## Features

- **Quick File Comparison**: Mark code sections with `#DiffLink:/path/to/file.java` comments
- **Gutter Icons**: Click icons in the editor gutter to instantly open file comparisons
- **Built-in Diff Viewer**: Uses IntelliJ's native diff viewer for seamless integration
- **Error Handling**: Visual feedback (error icons) for missing or invalid file paths
- **Multi-Language Support**: Works with Java, Python, Kotlin, JavaScript, Markdown, config files, and all text file types
- **Two-Parameter Markers**: Compare explicit source and destination files: `#DiffLink:/src, /dst`

## Installation

### From Source

1. Clone this repository
2. Open in IntelliJ IDEA
3. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```
4. The JAR will be created at: `build/libs/difflink-0.0.1.jar`
5. Install via: Settings → Plugins → Install Plugin from Disk

## Usage

### Basic Workflow

1. **Mark Files to Compare**: Add a comment in your Java code:
   ```java
   // #DiffLink:/src/main/java/OldImplementation.java
   public class NewImplementation {
       // Your new implementation
   }
   ```

2. **Click the Gutter Icon**: A clickable icon appears in the editor margin next to the comment

3. **View Comparison**: Click the icon to open the diff viewer comparing the current file with the destination

### Comment Format

```
#DiffLink:/path/to/destination/file.java
```

**Path Rules:**
- **Relative path** (no leading `/`): resolved from the project root — `src/main/java/Example.java`
- **Absolute path** (leading `/`): used as a computer filesystem path — `/Users/neo/projects/repo/src/Example.java`
- Whitespace is trimmed automatically: `#DiffLink:  src/main/java/File.java  ` works fine

### Two-Parameter Syntax

In addition to comparing the current file with a destination, you can explicitly specify both source and destination files:

**Single Parameter (existing):**
```
#DiffLink:/path/to/destination/file
```
Compares the current file with the specified destination.

**Two Parameters (new):**
```
#DiffLink:/path/to/source/file, /path/to/destination/file
#DiffLink:  /v1/file.py  ,  /v2/file.py   (whitespace is trimmed)
```
Compares two explicitly specified files.

**Examples:**
```java
// Compare different versions of the same file
// #DiffLink:/implementation/v1.java, /implementation/v2.java

// Compare original with refactored version
// #DiffLink:/old/CodeSample.java, /new/CodeSample.java
```

### Valid Examples

```java
// Single line comment
// #DiffLink:/src/main/java/OldVersion.java

/* Block comment style */
/* #DiffLink:/src/test/java/TestCase.java */

// Multiple comments in same file
// #DiffLink:/version1/Implementation.java
public class MyClass {
    // #DiffLink:/version2/Implementation.java
    public void method() {}
}
```

### Multi-Language Examples

The plugin works with any text file type. Here are examples for different languages:

**Java:**
```java
// Single parameter - compares current file with destination
// #DiffLink:/old/Implementation.java

// Two parameters - compares two explicit files
// #DiffLink:/v1/Implementation.java, /v2/Implementation.java
```

**Python:**
```python
# #DiffLink:/version1/script.py, /version2/script.py
def process():
    pass
```

**Kotlin:**
```kotlin
// #DiffLink:/src/main/kotlin/OldClass.kt, /src/main/kotlin/NewClass.kt
class MyClass {}
```

**JavaScript:**
```javascript
// #DiffLink:/src/utils/old.js, /src/utils/new.js
function helper() {}
```

**Markdown/Documentation:**
```markdown
#DiffLink:/docs/old-guide.md, /docs/new-guide.md

This document was updated in the new version.
```

**XML/Configuration:**
```xml
<!-- #DiffLink:/config/prod.xml, /config/dev.xml -->
<configuration>
    <!-- Your config -->
</configuration>
```

**Bash/Shell:**
```bash
#!/bin/bash
# #DiffLink:/scripts/v1/deploy.sh, /scripts/v2/deploy.sh
echo "Deploy script"
```

### Error Handling

If the destination file doesn't exist or the path is invalid:
- An **error icon** (red) appears in the gutter instead of the normal icon
- Hovering shows the error message: "File not found: /path/to/file"
- Clicking the error icon does nothing (non-clickable)

**Common Issues:**
- **Path not found**: Verify the file exists and path is correct relative to project root
- **Path traversal rejected**: Paths like `/../etc/passwd` or `~/files` are blocked for security
- **External URLs rejected**: HTTP/HTTPS URLs are not supported

## Development

### Project Structure

```
difflink/
├── src/main/kotlin/com/lianggong/difflink/
│   ├── markers/
│   │   └── CompareMarkerProvider.kt      # LineMarkerProvider implementation
│   ├── actions/
│   │   └── CompareActionHandler.kt       # Diff viewer launcher
│   ├── utils/
│   │   └── ComparePathResolver.kt        # Path validation and resolution
│   └── DiffLinkBundle.properties # Localization strings
├── src/main/resources/META-INF/
│   └── plugin.xml                        # Plugin descriptor
├── src/test/kotlin/com/lianggong/difflink/
│   ├── DiffLinkIntegrationTest.kt
│   ├── CompareMarkerProviderTest.kt
│   ├── CompareActionHandlerTest.kt
│   └── ComparePathResolverTest.kt
├── build.gradle.kts                      # Gradle build configuration
└── gradle.properties                     # Project properties
```

### Architecture

**Core Components:**

1. **CompareMarkerProvider** (LineMarkerProvider)
   - Scans Java files for `#DiffLink:` comments
   - Creates clickable gutter icons for valid references
   - Shows error icons for invalid paths

2. **ComparePathResolver**
   - Validates and resolves file paths relative to project root
   - Prevents path traversal attacks
   - Rejects external URLs
   - Returns type-safe `ResolveResult` (Success or Error)

3. **CompareActionHandler** (AnAction)
   - Handles gutter icon clicks
   - Launches IntelliJ's DiffManager with selected files
   - Provides error notifications for failures

4. **plugin.xml**
   - Declares LineMarkerProvider extension
   - Registers ShowDiff action
   - Defines notification group for error messages

### Building

```bash
# Build the plugin
./gradlew buildPlugin

# Run tests
./gradlew test

# Build and run tests
./gradlew build

# Verify plugin descriptor
unzip -t build/libs/difflink-0.0.1.jar META-INF/plugin.xml
```

### Testing

The project includes comprehensive test coverage:

- **ComparePathResolverTest** (8 tests)
  - Valid path resolution
  - Missing file handling
  - Directory traversal prevention
  - URL rejection
  - Whitespace trimming
  - Path normalization

- **CompareMarkerProviderTest** (6 tests)
  - Comment detection
  - Multiple comment handling
  - Error marker creation
  - Block comment support
  - Non-compare comment filtering

- **CompareActionHandlerTest** (4 tests)
  - Successful diff opening
  - Error handling
  - Action invocation

- **DiffLinkIntegrationTest** (3 tests)
  - End-to-end workflow
  - Multiple file comparisons
  - Missing file error handling

**Run tests:**
```bash
./gradlew test -v
```

Expected: 21 tests passing

## Requirements

- **IDE**: IntelliJ IDEA 2023.1+ (Community or Ultimate)
- **Java**: JDK 11 or later
- **Gradle**: 8.5+ (via gradle wrapper)

## Technical Details

### Path Resolution

The plugin resolves paths relative to the project root:

```
Project Root: /path/to/my/project/
Comment: #DiffLink:/src/main/java/Old.java
Resolved to: /path/to/my/project/src/main/java/Old.java
```

### Error Prevention

The plugin includes multiple security and validation checks:

- **Empty paths**: Rejected immediately
- **Directory traversal** (`..` or `~`): Blocked
- **External URLs** (http://, file://): Rejected
- **Missing files**: Error icon shown, no action on click
- **Exceptions**: Caught and reported via notification

### Diff Viewer Integration

Uses IntelliJ's built-in `DiffManager` API:
- `FileContent` for file representation
- `SimpleDiffRequest` for comparison setup
- Native side-by-side or unified diff view
- Automatic syntax highlighting

## Keyboard Shortcuts

- **Ctrl+Shift+C**: Trigger ShowDiff action (configurable via keymap)

## Future Enhancements

- Custom comparison options (ignore whitespace, specific sections)
- Quick action to generate `#DiffLink:` comments
- Integration with version control (Git history comparison)
- Smart path suggestions/autocomplete

## Contributing

Contributions are welcome! Please ensure:
- Tests pass: `./gradlew test`
- Code compiles: `./gradlew compileKotlin`
- Plugin builds: `./gradlew buildPlugin`

## License

[Add your license here]

## Author

Liang Gong (liang.gong@gmail.com)

## Support

For issues, questions, or suggestions, please open an issue in the repository.

---

**Version**: 0.0.1  
**IDE Compatibility**: IntelliJ IDEA 2023.1+  
**Kotlin**: 1.9.20  
**Last Updated**: 2026-04-22
