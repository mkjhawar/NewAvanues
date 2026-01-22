# MagicUI Language Server

Language Server Protocol (LSP) implementation for MagicUI DSL files.

## Features

### 🎯 Core LSP Features
- **Incremental text document sync** - Efficient document updates
- **Completion provider** - Context-aware autocomplete with snippets
- **Hover provider** - Rich markdown documentation
- **Definition provider** - Go-to-definition for VUIDs
- **Diagnostic provider** - Real-time error checking and validation
- **Document formatting** - Code formatting support
- **Code actions** - Quick fixes and refactorings
- **Execute commands** - Custom commands for theme generation, validation, etc.

### 🔍 Intelligent Features
- **Context-aware completion**: 10 components with snippets (Button, TextField, Card, Text, Image, Column, Row, Container, Checkbox, Switch)
- **Property completions**: Common + type-specific properties
- **Value completions**: 9 colors, 7 alignments, booleans, 5 event handlers
- **Rich hover documentation**: Markdown-formatted docs with examples

### ✅ Semantic Validation
- **Component rules**: Button must have text/icon, Image must have src, etc.
- **Property validation**: Color values, size units, alignment values
- **Required fields**: VUID, event handlers for interactive components
- **Hierarchy validation**: ScrollView nesting prevention, depth checks

### 🎨 Theme Generation
- **5 export formats**: Kotlin DSL, YAML, JSON, CSS, Android XML
- **Command**: `magicui.generateTheme`

### 🔗 VUID Utilities
- **Format validation**: Lowercase, hyphens, 3-64 chars
- **Go-to-definition**: Navigate to VUID declarations
- **Generation**: Auto-suggest VUIDs based on component type

## Supported File Types
- `.magic.yaml` - MagicUI YAML DSL
- `.magic.json` - MagicUI JSON DSL
- `.magicui` - MagicUI compact syntax
- `.ucd` - UI component definition

## Installation

### Prerequisites
- Java 17 or higher
- Gradle 8.0+ (for building from source)

### Build from Source

```bash
# Navigate to Language Server directory
cd Modules/AVAMagic/MagicTools/LanguageServer

# Build the JAR
./scripts/package.sh

# Verify build
ls -lh build/libs/LanguageServer-1.0.0.jar
```

## Usage

### Standalone Server (stdio)
```bash
./scripts/launch.sh
```

### Socket Mode (for debugging)
```bash
./scripts/launch.sh socket 9999
```

### Direct JAR Execution
```bash
java -jar build/libs/LanguageServer-1.0.0.jar
```

With port:
```bash
java -jar build/libs/LanguageServer-1.0.0.jar --socket 9999
```

## IDE Integration

### VS Code

1. **Install the extension**:
   ```bash
   cd vscode
   npm install
   npm run compile
   code --install-extension .
   ```

2. **Configure JAR path** in VS Code settings:
   ```json
   {
     "magicui.server.jarPath": "/path/to/LanguageServer-1.0.0.jar"
   }
   ```

3. **Open a MagicUI file** (`.magic.yaml`, `.magic.json`, or `.magicui`)

### IntelliJ IDEA / Android Studio

1. **Install LSP Support plugin** from JetBrains Marketplace

2. **Configure Language Server**:
   - Go to: Settings → Languages & Frameworks → Language Server Protocol → Server Definitions
   - Add new server:
     - **Extension**: `magicui`
     - **Command**: `java -jar /path/to/LanguageServer-1.0.0.jar`

3. **Associate file patterns**:
   - `*.magic.yaml`
   - `*.magic.json`
   - `*.magicui`

## Custom Commands

### Theme Generation
```typescript
workspace.executeCommand('magicui.generateTheme', 'dsl', themeJson)
```

**Formats**: `dsl`, `yaml`, `json`, `css`, `xml`

### Component Validation
```typescript
workspace.executeCommand('magicui.validateComponent', componentData)
```

### Document Formatting
```typescript
workspace.executeCommand('magicui.formatDocument', documentUri)
```

### Code Generation
```typescript
workspace.executeCommand('magicui.generateCode', 'kotlin', dslContent)
```

## Development

### Project Structure
```
LanguageServer/
├── src/main/kotlin/com/augmentalis/magicui/lsp/
│   ├── MagicUILanguageServerLauncher.kt  # Entry point
│   ├── MagicUILanguageServer.kt          # Main server
│   ├── MagicUITextDocumentService.kt     # Document features
│   ├── MagicUIWorkspaceService.kt        # Workspace features
│   └── stubs/ParserStubs.kt              # Temporary stubs
├── src/test/kotlin/                      # Unit tests (30 tests)
├── scripts/
│   ├── package.sh                        # Build script
│   └── launch.sh                         # Launch script
├── vscode/                               # VS Code extension
└── build.gradle.kts                      # Build configuration
```

### Running Tests
```bash
./gradlew test
```

### Code Coverage
```bash
./gradlew test jacocoTestReport
open build/reports/jacoco/test/html/index.html
```

## Configuration

### Server Options
- `--socket <port>` - Use socket transport instead of stdio
- `--help` - Show help message

### Client Configuration (VS Code)
```json
{
  "magicui.server.jarPath": "/path/to/jar",
  "magicui.trace.server": "verbose"  // off | messages | verbose
}
```

## Logging

Logs are written to `magicui-lsp.log` in the current directory.

**Log levels**:
- `com.augmentalis.magicui.lsp` - DEBUG
- `org.eclipse.lsp4j` - WARN
- Root - INFO

## Architecture

### LSP Protocol Flow
```
Client (VS Code/IntelliJ)
    ↕ LSP Protocol (JSON-RPC)
MagicUILanguageServer
    ├── MagicUITextDocumentService (completion, hover, diagnostics)
    └── MagicUIWorkspaceService (commands, file watching)
```

### Key Components
1. **Launcher** - stdio/socket transport setup
2. **Server** - Capability negotiation
3. **TextDocumentService** - Editor features
4. **WorkspaceService** - Workspace-level operations

## Troubleshooting

### Server not starting
- **Check Java version**: `java -version` (must be 17+)
- **Check JAR path**: Verify file exists and is readable
- **Check logs**: `tail -f magicui-lsp.log`

### No completions appearing
- **Check file extension**: Must be `.magic.yaml`, `.magic.json`, or `.magicui`
- **Check server status**: VS Code → Output → MagicUI Language Server
- **Restart client**: VS Code Command Palette → "Reload Window"

### Diagnostics not showing
- **Check content**: File must have valid YAML/JSON structure
- **Check logs**: Look for validation errors in `magicui-lsp.log`

## Performance

- **Startup time**: <2 seconds
- **Completion response**: <100ms
- **Validation**: Real-time (on-type)
- **Memory usage**: ~50MB (idle), ~100MB (active)

## License

Proprietary - Augmentalis ES

## Version

**1.0.0** - Initial release (2025-12-24)

### Features
- Complete LSP implementation
- 10 components with snippets
- Semantic validation
- Theme generation (5 formats)
- VUID utilities
- 30 comprehensive tests

## Documentation

### Complete Documentation Set

- **[User Manual](../../../../Docs/AVAMagic/Manuals/User/MagicUI-LSP-User-Manual-251224-V1.md)** - Installation, getting started, features, troubleshooting
- **[Developer Manual](../../../../Docs/AVAMagic/Manuals/Developer/MagicUI-LSP-Developer-Manual-251224-V1.md)** - Architecture, implementation details, extending the server
- **[Examples](./EXAMPLES.md)** - Practical code examples for common use cases
- **[README](./README.md)** - This file (quick reference)

### Quick Links

| Documentation | Purpose | Audience |
|---------------|---------|----------|
| User Manual | Installation, usage, configuration | End users, UI developers |
| Developer Manual | Architecture, testing, extending | Contributors, advanced users |
| Examples | Copy-paste code snippets | All users |
| README | Quick reference, feature overview | Everyone |

## Support

For issues and questions:
- Create an issue in the project repository
- Contact: Manoj Jhawar <manoj@ideahq.net>

**Before requesting support:**
- Check the [User Manual](../../../../Docs/AVAMagic/Manuals/User/MagicUI-LSP-User-Manual-251224-V1.md) for troubleshooting
- Review [Examples](./EXAMPLES.md) for similar use cases
- Consult the [Developer Manual](../../../../Docs/AVAMagic/Manuals/Developer/MagicUI-LSP-Developer-Manual-251224-V1.md) for technical details
