# VoiceAvanue App Architecture

## Overview

VoiceAvanue is the unified voice control application that combines VoiceOSCore and WebAvanue modules. The app follows a **thin wrapper** pattern - all core logic resides in KMP modules, with the app providing only Android-specific wiring.

## Design Principles

1. **No Duplication** - All core logic in VoiceOSCore module
2. **Thin Wrappers** - App-level classes only provide Android manifest requirements
3. **Composition over Inheritance** - Use VoiceOSCore components, don't recreate them

## Component Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    VoiceOSCore Module (KMP)                        │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  VoiceOSAccessibilityService (abstract)                      │  │
│  │  ├── AndroidScreenExtractor     → extracts UI elements       │  │
│  │  ├── CommandGenerator           → generates voice commands   │  │
│  │  ├── AndroidGestureDispatcher   → executes actions           │  │
│  │  ├── ActionCoordinator          → processes voice input      │  │
│  │  └── BoundsResolver             → scroll offset tracking     │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
                                  │
                                  │ extends
                                  ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    voiceavanue App                                  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  VoiceAvanueAccessibilityService                             │  │
│  │  ├── getActionCoordinator()  → required abstract impl        │  │
│  │  ├── onServiceReady()        → initializes VoiceOSCore       │  │
│  │  └── onCommandsUpdated()     → updates speech engine         │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  VoiceRecognitionService                                     │  │
│  │  └── Foreground notification only (keeps process alive)      │  │
│  └──────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────┐  │
│  │  AppModule (Hilt DI)                                         │  │
│  │  ├── DatabaseDriverFactory                                   │  │
│  │  ├── VoiceOSDatabaseManager                                  │  │
│  │  ├── VoiceOSServerConfig                                     │  │
│  │  └── WebAvanueServerConfig                                   │  │
│  └──────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

## Key Classes

### VoiceAvanueAccessibilityService

**Location**: `service/VoiceAvanueAccessibilityService.kt`
**Lines**: ~117 (minimal)
**Extends**: `VoiceOSAccessibilityService` from VoiceOSCore

**Purpose**: Android requires accessibility services to be declared in the app's manifest with a concrete class. This class extends the abstract `VoiceOSAccessibilityService` and provides the required implementations.

**Key Methods**:
```kotlin
// Required abstract implementation
override fun getActionCoordinator(): ActionCoordinator

// Optional overrides for app-specific behavior
override fun getBoundsResolver(): BoundsResolver?
override fun onServiceReady()
override fun onCommandsUpdated(commands: List<QuantizedCommand>)
override fun onCommandExecuted(command: QuantizedCommand, success: Boolean)
```

### VoiceRecognitionService

**Location**: `service/VoiceRecognitionService.kt`
**Lines**: ~104 (minimal)
**Extends**: `android.app.Service`

**Purpose**: Foreground service that keeps the app process alive during continuous voice recognition. Actual speech recognition is handled by VoiceOSCore.

**Key Responsibilities**:
- Create and manage foreground notification
- Delegate speech recognition to VoiceOSCore via AccessibilityService

### AppModule

**Location**: `di/AppModule.kt`
**Lines**: ~69 (minimal)
**Type**: Hilt DI Module

**Provides**:
- `DatabaseDriverFactory` - Android SQLite driver
- `VoiceOSDatabaseManager` - Shared database singleton
- `VoiceOSServerConfig` - RPC server config (port 50051)
- `WebAvanueServerConfig` - WebAvanue RPC config (port 50055)
- `IoDispatcher` / `MainDispatcher` - Coroutine dispatchers

## Data Flow

### Voice Command Processing

```
1. Speech Recognition (VoiceOSCore ISpeechEngine)
   │
   ▼
2. VoiceOSAccessibilityService.processVoiceCommand()
   │
   ▼
3. ActionCoordinator.processVoiceCommand()
   │
   ├─► CommandRegistry (static commands)
   ├─► Dynamic commands (from screen scraping)
   └─► NLU IntentClassifier (fuzzy matching)
   │
   ▼
4. AndroidGestureDispatcher.execute()
   │
   ▼
5. onCommandExecuted() callback
```

### Screen Scraping

```
1. Accessibility Event (TYPE_WINDOW_STATE_CHANGED)
   │
   ▼
2. VoiceOSAccessibilityService.handleScreenChange()
   │
   ▼
3. AndroidScreenExtractor.extract()
   │
   ▼
4. CommandGenerator.fromElement() / generateListIndexCommands()
   │
   ▼
5. ActionCoordinator.updateDynamicCommands()
   │
   ▼
6. onCommandsUpdated() callback → update speech engine
```

## Dependencies

### Module Dependencies (build.gradle.kts)

```kotlin
// Core KMP modules
implementation(project(":Modules:VoiceOSCore"))  // Voice commands, accessibility
implementation(project(":Modules:WebAvanue"))    // Browser control
implementation(project(":Modules:Database"))     // SQLDelight database
implementation(project(":Modules:VoiceCursor"))  // Cursor control

// Unified module (combines above)
implementation(project(":Modules:VoiceAvanue"))

// AI modules
implementation(project(":Modules:AI:NLU"))       // Intent classification
implementation(project(":Modules:AI:LLM"))       // Language model
```

### Key Third-Party Dependencies

- **Hilt** - Dependency injection
- **Jetpack Compose** - UI framework
- **Kotlinx Coroutines** - Async programming
- **Kotlinx Serialization** - JSON parsing

## Configuration

### ServiceConfiguration (created in AccessibilityService)

```kotlin
ServiceConfiguration(
    speechEngine = SpeechEngine.ANDROID_STT.name,
    voiceLanguage = "en-US",
    confidenceThreshold = 0.7f,
    autoStartListening = false,
    synonymsEnabled = true,
    debugMode = true
)
```

## Extending the App

### Adding New Features

1. **New voice commands**: Add to VoiceOSCore's CommandRegistry
2. **New UI screens**: Add Compose screens in `ui/` directory
3. **New services**: Prefer adding to VoiceOSCore module, not app

### Custom Initialization

Override `onServiceReady()` in `VoiceAvanueAccessibilityService`:

```kotlin
override fun onServiceReady() {
    super.onServiceReady()
    // Your custom initialization
}
```

### Custom Command Handling

Override `onCommandExecuted()` for custom feedback:

```kotlin
override fun onCommandExecuted(command: QuantizedCommand, success: Boolean) {
    super.onCommandExecuted(command, success)
    // Custom TTS feedback, haptics, etc.
}
```

## Related Documentation

- [RECOVERY_ROADMAP.md](../../RECOVERY_ROADMAP.md) - Module consolidation progress
- [VoiceOSCore docs](../../Modules/VoiceOSCore/README.md) - Core module documentation
- [Database module](../../Modules/Database/README.md) - Database schema and queries

---
*Last updated: 2026-02-05*
*Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC*
