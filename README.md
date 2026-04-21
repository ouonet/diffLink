# Compare Navigator

An IntelliJ IDEA plugin that enables developers to navigate and compare files during refactoring using special comments.

## Features

- **Quick File Comparison**: Mark code sections with `#COMPARE:/path/to/file.java` comments
- **Gutter Icons**: Click icons in the editor gutter to instantly open file comparisons
- **Built-in Diff Viewer**: Uses IntelliJ's native diff viewer for seamless integration
- **Error Handling**: Visual feedback (error icons) for missing or invalid file paths
- **Java Support**: Optimized for Java files (initial release)

## Installation

### From Source

1. Clone this repository
2. Open in IntelliJ IDEA
3. Build the plugin:
   ```bash
   ./gradlew buildPlugin
   ```
4. The JAR will be created at: `build/libs/compare-navigator-0.0.1.jar`
5. Install via: Settings → Plugins → Install Plugin from Disk

## Usage

### Basic Workflow

1. **Mark Files to Compare**: Add a comment in your Java code:
   ```java
   // #COMPARE:/src/main/java/OldImplementation.java
   public class NewImplementation {
       // Your new implementation
   }
   ```

2. **Click the Gutter Icon**: A clickable icon appears in the editor margin next to the comment

3. **View Comparison**: Click the icon to open the diff viewer comparing the current file with the destination

### Comment Format

```
#COMPARE:/path/to/destination/file.java
```

**Path Rules:**
- Paths are relative to the project root
- Must start with `/` (or it will be normalized to start with `/`)
- Supports absolute project paths: `/src/main/java/Example.java`
- Whitespace is trimmed automatically: `#COMPARE:  /path/to/file  ` works fine

### Valid Examples

```java
// Single line comment
// #COMPARE:/src/main/java/OldVersion.java

/* Block comment style */
/* #COMPARE:/src/test/java/TestCase.java */

// Multiple comments in same file
// #COMPARE:/version1/Implementation.java
public class MyClass {
    // #COMPARE:/version2/Implementation.java
    public void method() {}
}
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
compare-navigator/
├── src/main/kotlin/com/lianggong/comparenavigator/
│   ├── markers/
│   │   └── CompareMarkerProvider.kt      # LineMarkerProvider implementation
│   ├── actions/
│   │   └── CompareActionHandler.kt       # Diff viewer launcher
│   ├── utils/
│   │   └── ComparePathResolver.kt        # Path validation and resolution
│   └── CompareNavigatorBundle.properties # Localization strings
├── src/main/resources/META-INF/
│   └── plugin.xml                        # Plugin descriptor
├── src/test/kotlin/com/lianggong/comparenavigator/
│   ├── CompareNavigatorIntegrationTest.kt
│   ├── CompareMarkerProviderTest.kt
│   ├── CompareActionHandlerTest.kt
│   └── ComparePathResolverTest.kt
├── build.gradle.kts                      # Gradle build configuration
└── gradle.properties                     # Project properties
```

### Architecture

**Core Components:**

1. **CompareMarkerProvider** (LineMarkerProvider)
   - Scans Java files for `#COMPARE:` comments
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
unzip -t build/libs/compare-navigator-0.0.1.jar META-INF/plugin.xml
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

- **CompareNavigatorIntegrationTest** (3 tests)
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
Comment: #COMPARE:/src/main/java/Old.java
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

- Support for additional file types (Python, Go, JavaScript, etc.)
- Custom comparison options (ignore whitespace, specific sections)
- Quick action to generate `#COMPARE:` comments
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
**Last Updated**: April 2026
