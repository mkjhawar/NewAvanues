# ARG Scanner - Avanue Registry File Scanner

## Overview

The ARG Scanner module discovers, parses, and indexes Avanue Registry (ARG) files from the VoiceOS ecosystem. ARG files describe app capabilities, voice commands, IPC endpoints, and intent filters for seamless inter-app communication.

## Features

### ✅ ARG File Format (v1.0)
- JSON-based registry format
- App metadata (ID, name, version, category)
- Capability declarations with voice commands
- Android Intent filter declarations
- AIDL service endpoints
- Content Provider endpoints
- Comprehensive validation

### ✅ ARG Parser
- Deserializes ARG files from JSON
- Validates file format and structure
- Checks parameter consistency in voice commands
- Serializes back to JSON

### ✅ ARG Registry
- In-memory capability index
- Fast capability search by keywords
- Intent-based app discovery
- Category-based filtering
- Real-time updates via StateFlow

### ✅ ARG Scanner
- Platform-agnostic file discovery
- Multiple search locations (internal, external, user)
- Batch scanning with error reporting
- Watch mode for file system changes (TODO)

## Usage

### Parse an ARG File

```kotlin
val parser = ARGParser()
val argFile = parser.parse(jsonString)

// Validate
val errors = parser.validate(argFile)
if (errors.isEmpty()) {
    println("✅ Valid ARG file")
} else {
    errors.forEach { println("❌ ${it.message}") }
}
```

### Register and Search Capabilities

```kotlin
val registry = ARGRegistry()
registry.register(argFile)

// Search by keywords
val results = registry.searchCapabilities("browse web")
results.forEach { result ->
    println("${result.app.name}: ${result.capability.name} (score: ${result.score})")
}

// Find by Intent
val apps = registry.findByIntent(
    action = "android.intent.action.VIEW",
    dataUri = "https://example.com"
)
```

### Scan File System

```kotlin
val scanner = ARGScanner(parser, registry)

// Scan all default locations
val results = scanner.scanAll()
println("✅ Discovered ${results.success.size} apps")
println("❌ Failed ${results.failed.size} apps")

// Scan specific path
val pathResults = scanner.scan("/sdcard/Avanue/registry/")
```

## ARG File Format

### Example: BrowserAvanue

```json
{
  "version": "1.0",
  "app": {
    "id": "com.augmentalis.avanue.browser",
    "name": "BrowserAvanue",
    "version": "1.0.0",
    "description": "Voice-controlled web browser",
    "packageName": "com.augmentalis.avanue.browser",
    "category": "PRODUCTIVITY"
  },
  "capabilities": [
    {
      "id": "capability.browse_web",
      "name": "Browse Web",
      "description": "Open web pages with voice commands",
      "type": "ACTION",
      "voiceCommands": [
        "open {url}",
        "browse to {url}"
      ],
      "params": [
        {
          "name": "url",
          "type": "URL",
          "required": true
        }
      ],
      "requiresPermissions": ["android.permission.INTERNET"]
    }
  ],
  "intentFilters": [
    {
      "action": "android.intent.action.VIEW",
      "categories": ["android.intent.category.BROWSABLE"],
      "dataSchemes": ["http", "https"],
      "priority": 100
    }
  ],
  "services": [
    {
      "id": "service.browser_control",
      "name": "Browser Control Service",
      "aidlInterface": "com.augmentalis.avanue.browser.IBrowserService",
      "methods": [
        {
          "name": "openUrl",
          "params": [{"name": "url", "type": "String"}],
          "returnType": "boolean"
        }
      ]
    }
  ]
}
```

## Architecture

```
ARGScanner
├── ARGModels.kt          - Data models (ARGFile, Capability, etc.)
├── ARGParser.kt          - JSON parser and validator
├── ARGRegistry.kt        - In-memory capability index
├── ARGScanner.kt         - File system scanner
└── Platform*.kt          - Platform-specific implementations
```

## Platform Support

- ✅ **Android** - Full implementation
- ⏳ **iOS** - TODO (expect/actual stubs ready)
- ⏳ **macOS** - TODO (expect/actual stubs ready)
- ⏳ **Windows** - TODO (expect/actual stubs ready)

## Discovery Locations

| Platform | Primary Path | Secondary Path | User Path |
|----------|-------------|----------------|-----------|
| Android | `/data/data/{package}/files/arg/` | `/sdcard/Avanue/registry/` | `/sdcard/Documents/Avanue/registry/` |
| iOS | `Documents/Avanue/registry/` | - | - |
| macOS | `~/Library/Application Support/Avanue/registry/` | - | - |
| Windows | `%APPDATA%/Avanue/registry/` | - | - |

## Testing

Run unit tests:

```bash
./gradlew :modules:MagicIdea:Components:ARGScanner:test
```

## Integration

Add to `settings.gradle.kts`:

```kotlin
include(":modules:MagicIdea:Components:ARGScanner")
```

Add dependency in `build.gradle.kts`:

```kotlin
implementation(project(":modules:MagicIdea:Components:ARGScanner"))
```

## Version

**1.0.0** - Initial implementation

## Author

Created by Manoj Jhawar, manoj@ideahq.net
