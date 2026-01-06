# VoiceOSCoreNG

> Unified KMP module consolidating VoiceOSCore/learnapp, LearnAppCore, and JITLearning into a single, cross-platform voice accessibility library.

**Version:** 2.0.0
**Platforms:** Android, iOS, Desktop (JVM), Web
**Min SDK:** Android 24+ | iOS 14+ | JVM 17+

---

## Overview

VoiceOSCoreNG is the next-generation core module for VoiceOS, providing:

- **Voice Unique Identifier (VUID)** - Compact 16-character element identification replacing UUID
- **Framework Detection** - Automatic detection of Flutter, Unity, React Native, WebView, and native apps
- **Element Processing** - Efficient UI element extraction and voice command generation
- **Speech Engine Abstraction** - Unified interface for multiple speech recognition engines
- **Feature Tiers** - Lite (free) and Dev (premium) feature sets with runtime switching

---

## Architecture

```
+─────────────────────────────────────────────────────────────────────────────+
│                           VoiceOSCoreNG Module                              │
+─────────────────────────────────────────────────────────────────────────────+
│                                                                             │
│  ┌─────────────────────────────────────────────────────────────────────┐   │
│  │                         commonMain (KMP)                             │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐  │   │
│  │  │   common/   │  │    avu/     │  │  command/   │  │ features/  │  │   │
│  │  │ ElementInfo │  │ Quantized*  │  │ Generator   │  │ LearnApp*  │  │   │
│  │  │ FrameworkInfo│ │ Serializer  │  │ Registry    │  │ DevToggle  │  │   │
│  │  │ VUIDGenerator│ │ ElementType │  │ Matcher     │  │ Config     │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌────────────┐  │   │
│  │  │  handlers/  │  │ extraction/ │  │ execution/  │  │ repository/│  │   │
│  │  │ Framework*  │  │ ElementParser│ │IActionExec  │  │ ICommand*  │  │   │
│  │  │ Flutter*    │  │ Extraction* │  │ ActionResult│  │ IVuid*     │  │   │
│  │  │ WebView*    │  │ Platform*   │  │ Dispatcher  │  │ Adapters   │  │   │
│  │  └─────────────┘  └─────────────┘  └─────────────┘  └────────────┘  │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │   │
│  │  │   speech/   │  │   safety/   │  │fingerprint/ │                  │   │
│  │  │ ISpeechEngine│ │ Dangerous*  │  │ Screen*    │                  │   │
│  │  │ SpeechConfig │  │ Detector   │  │ Sha256     │                  │   │
│  │  │ SpeechResult │  └─────────────┘  └─────────────┘                  │   │
│  │  └─────────────┘                                                     │   │
│  └─────────────────────────────────────────────────────────────────────┘   │
│                                                                             │
│  ┌────────────────┐  ┌────────────────┐  ┌────────────────┐                │
│  │  androidMain   │  │    iosMain     │  │  desktopMain   │                │
│  │ VoiceOSCoreNG  │  │ PlatformExtractor│ │ PlatformExtractor│             │
│  │ AndroidSTT*    │  │ IOSActionExec  │  │ DesktopAction* │               │
│  │ AndroidAction* │  │ SpeechFactory  │  │ SpeechFactory  │               │
│  │ UI Components  │  │ Time Utils     │  │ Time Utils     │               │
│  └────────────────┘  └────────────────┘  └────────────────┘                │
│                                                                             │
+─────────────────────────────────────────────────────────────────────────────+
                                    │
                                    ▼
+─────────────────────────────────────────────────────────────────────────────+
│                            External Systems                                 │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐   │
│  │ Common/     │  │ Accessibility│  │ Speech     │  │ SQLDelight      │   │
│  │ Database    │  │ Services    │  │ Engines    │  │ (persistence)   │   │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────────┘   │
+─────────────────────────────────────────────────────────────────────────────+
```

---

## Feature Comparison

| Feature | Lite | Dev |
|---------|------|-----|
| Element Scraping | Basic (100/scan) | Extended (500/scan) |
| VUID Generation | Yes | Yes |
| Native App Detection | Yes | Yes |
| Voice Commands | Basic | Advanced |
| JIT Processing | Yes | Yes |
| Batch Processing | No | Yes |
| Framework Detection | Native only | Flutter, Unity, RN, WebView |
| AI Classification | Basic | Full |
| NLU Features | Basic | Full |
| Exploration Mode | No | Yes |
| Screen Caching | No | Yes |
| Debug Overlay | No | Yes |
| Analytics | No | Yes |
| Max Apps Learned | 25 | Unlimited |

---

## Quick Start

### 1. Add Dependency

```kotlin
// settings.gradle.kts
include(":Modules:VoiceOSCoreNG")

// build.gradle.kts (app module)
dependencies {
    implementation(project(":Modules:VoiceOSCoreNG"))
}
```

### 2. Initialize (Android)

```kotlin
// Application.onCreate()
VoiceOSCoreNG.initialize(
    tier = LearnAppDevToggle.Tier.LITE,
    isDebug = BuildConfig.DEBUG
)

// Optional: Enable test mode for development
VoiceOSCoreNG.enableTestMode()

// Optional: Configure custom limits
VoiceOSCoreNG.configureLimits(
    maxElementsPerScan = 200,
    maxAppsLearned = 50
)
```

### 3. Generate VUIDs

```kotlin
// Generate VUID for a UI element
val vuid = VUIDGenerator.generate(
    packageName = "com.example.app",
    typeCode = VUIDTypeCode.BUTTON,
    elementHash = "submit_btn"
)
// Result: "a3f2e1-b917cc9dc" (16 chars)

// Detect type from class name
val typeCode = VUIDGenerator.getTypeCode("MaterialButton")
// Result: VUIDTypeCode.BUTTON

// Parse existing VUID
val components = VUIDGenerator.parseVUID("a3f2e1-b917cc9dc")
// Result: VUIDComponents(packageHash="a3f2e1", typeCode=BUTTON, elementHash="917cc9dc")
```

### 4. Process Elements

```kotlin
// Create ElementInfo from UI element
val element = ElementInfo(
    className = "Button",
    text = "Submit",
    resourceId = "com.app:id/submit_btn",
    bounds = Bounds(0, 0, 200, 50),
    isClickable = true,
    packageName = "com.example.app"
)

// Generate voice command
val command = CommandGenerator.fromElement(element, "com.example.app")
// Result: QuantizedCommand(phrase="click Submit", actionType=CLICK, ...)
```

### 5. Detect Framework

```kotlin
val framework = FrameworkDetector.detect(
    packageName = "com.flutter.app",
    classNames = listOf("io.flutter.embedding.FlutterActivity")
)
// Result: FrameworkInfo(type=FLUTTER, ...)
```

---

## Project Structure

```
Modules/VoiceOSCoreNG/
├── build.gradle.kts              # KMP build configuration
├── README.md                     # This file
├── src/
│   ├── commonMain/kotlin/com/augmentalis/voiceoscoreng/
│   │   ├── avu/                  # AVU serialization format
│   │   │   ├── AVUSerializer.kt
│   │   │   ├── CommandActionType.kt
│   │   │   ├── ElementType.kt
│   │   │   ├── QuantizedCommand.kt
│   │   │   ├── QuantizedContext.kt
│   │   │   ├── QuantizedElement.kt
│   │   │   ├── QuantizedNavigation.kt
│   │   │   └── QuantizedScreen.kt
│   │   ├── command/              # Command generation
│   │   │   ├── CommandGenerator.kt
│   │   │   ├── CommandMatcher.kt
│   │   │   ├── CommandRegistry.kt
│   │   │   └── StaticCommandRegistry.kt
│   │   ├── common/               # Core data types
│   │   │   ├── ElementInfo.kt
│   │   │   ├── ElementProcessingResult.kt
│   │   │   ├── FrameworkInfo.kt
│   │   │   ├── ProcessingMode.kt
│   │   │   └── VUIDGenerator.kt
│   │   ├── execution/            # Action execution
│   │   │   ├── ActionResult.kt
│   │   │   ├── CommandDispatcher.kt
│   │   │   └── IActionExecutor.kt
│   │   ├── exploration/          # App exploration
│   │   │   └── ExplorationBridge.kt
│   │   ├── extraction/           # Element extraction
│   │   │   ├── ElementParser.kt
│   │   │   ├── ExtractionBundle.kt
│   │   │   └── PlatformExtractor.kt
│   │   ├── features/             # Feature tiers
│   │   │   ├── LearnAppConfig.kt
│   │   │   └── LearnAppDevToggle.kt
│   │   ├── fingerprinting/       # Screen fingerprinting
│   │   │   └── ScreenFingerprinter.kt
│   │   ├── functions/            # Utilities
│   │   │   ├── ElementFilterUtils.kt
│   │   │   ├── HashUtils.kt
│   │   │   ├── LoggingUtils.kt
│   │   │   └── ValidationUtils.kt
│   │   ├── handlers/             # Framework handlers
│   │   │   ├── FlutterHandler.kt
│   │   │   ├── FrameworkHandler.kt
│   │   │   ├── NativeHandler.kt
│   │   │   ├── ReactNativeHandler.kt
│   │   │   ├── UnityHandler.kt
│   │   │   └── WebViewHandler.kt
│   │   ├── database/             # Database integration
│   │   │   └── DatabaseConverters.kt   # Domain ↔ DTO conversion
│   │   ├── safety/               # Safety features
│   │   │   └── DangerousElementDetector.kt
│   │   └── speech/               # Speech recognition
│   │       ├── ISpeechEngine.kt
│   │       ├── ISpeechEngineFactory.kt
│   │       ├── SpeechConfig.kt
│   │       ├── SpeechEngine.kt
│   │       └── SpeechMode.kt
│   ├── commonTest/               # Shared tests
│   ├── androidMain/              # Android implementation
│   │   └── kotlin/com/augmentalis/voiceoscoreng/
│   │       ├── core/VoiceOSCoreNG.kt
│   │       ├── execution/AndroidActionExecutor.kt
│   │       ├── exploration/ExplorationEngine.kt
│   │       ├── jit/JitProcessor.kt
│   │       ├── speech/AndroidSTTEngineImpl.kt
│   │       └── ui/               # Compose UI components
│   ├── androidUnitTest/          # Android-specific tests
│   ├── iosMain/                  # iOS implementation
│   ├── iosTest/                  # iOS-specific tests
│   ├── desktopMain/              # Desktop (JVM) implementation
│   └── desktopTest/              # Desktop-specific tests
└── Common/                       # Legacy structure (being migrated)
```

---

## Key Concepts

### VUID Format

Voice Unique Identifier - A compact 16-character format replacing UUID:

```
Format: {pkgHash6}-{typeCode}{hash8}
Example: a3f2e1-b917cc9dc (16 chars total)

Components:
- pkgHash6: 6-character hex hash of package name
- typeCode: Single character for element type
- hash8: 8-character hex hash of element identifier
```

Type codes:
- `b` = Button
- `i` = Input/TextField
- `s` = Scroll
- `t` = Text
- `e` = Element (generic)
- `c` = Card
- `l` = Layout
- `m` = Menu
- `d` = Dialog
- `g` = Image
- `k` = Checkbox
- `w` = Switch
- `z` = List
- `r` = Slider
- `a` = Tab

### Simple VUID Format

For internal entities (messages, documents, etc.):

```
Format: {module}:{type}:{hash8}
Example: ava:msg:a7f3e2c1

Modules: vos, ava, web, nlu, cpt, cmn
```

### Processing Modes

- **IMMEDIATE**: Process elements as encountered (default for Lite)
- **BATCH**: Collect and process in batches (Dev only)
- **HYBRID**: Immediate for common elements, batch for complex (Dev default)

---

## Thread Safety

VoiceOSCoreNG follows these threading guidelines:

1. **Immutable Data Classes**: `ElementInfo`, `FrameworkInfo`, `QuantizedCommand`, etc. are immutable
2. **Object Singletons**: `VUIDGenerator`, `CommandGenerator` are stateless and thread-safe
3. **Configuration Objects**: `LearnAppDevToggle`, `LearnAppConfig` use internal synchronization
4. **Coroutine-Based APIs**: Repository and execution interfaces use `suspend` functions
5. **Flow-Based Streams**: Speech results and state use Kotlin `Flow`

**Important**: Always access `LearnAppDevToggle` state from the main thread or use proper synchronization.

---

## API Reference

See: `Docs/VoiceOS/manuals/developer/VoiceOSCoreNG-API-Reference-60106-V1.md`

---

## Migration Guide

For migrating from VoiceOSCore/learnapp, LearnAppCore, or JITLearning:

See: `Docs/VoiceOS/manuals/developer/VoiceOSCoreNG-Migration-Guide-60106-V1.md`

---

## Build Instructions

### Prerequisites

- JDK 17 (required - JDK 24 incompatible with Gradle)
- Android SDK 34
- Xcode 15+ (for iOS)
- Gradle 8.x (via wrapper)

### Build Commands

```bash
# Build all targets
./gradlew :Modules:VoiceOSCoreNG:build

# Android only
./gradlew :Modules:VoiceOSCoreNG:assembleRelease

# Run tests
./gradlew :Modules:VoiceOSCoreNG:allTests

# Run common tests only
./gradlew :Modules:VoiceOSCoreNG:jvmTest
```

### Configuration

```properties
# gradle.properties
kotlin.mpp.stability.nowarn=true
kotlin.native.cacheKind=none
```

---

## Testing

```bash
# Run all tests
./gradlew :Modules:VoiceOSCoreNG:allTests

# Run specific platform tests
./gradlew :Modules:VoiceOSCoreNG:androidUnitTest
./gradlew :Modules:VoiceOSCoreNG:iosX64Test
./gradlew :Modules:VoiceOSCoreNG:desktopTest
```

Test coverage areas:
- VUID generation and parsing
- Element processing and filtering
- Command generation
- Framework detection
- Feature tier switching
- Repository adapters

---

## Dependencies

```kotlin
// Coroutines
org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0

// Serialization
org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3

// Android-specific
androidx.core:core-ktx:1.12.0
androidx.compose:compose-bom:2024.02.00
```

---

## Related Documentation

- Plan: `Docs/VoiceOS/plans/VoiceOS-Plan-VoiceOSCoreNG-Consolidation-51231-V1.md`
- API Reference: `Docs/VoiceOS/manuals/developer/VoiceOSCoreNG-API-Reference-60106-V1.md`
- Migration Guide: `Docs/VoiceOS/manuals/developer/VoiceOSCoreNG-Migration-Guide-60106-V1.md`

---

## Changelog

### 2.0.0 (2026-01-06)
- Initial consolidated release
- Unified VoiceOSCore/learnapp, LearnAppCore, JITLearning
- New VUID format (16 chars vs 36 char UUID)
- KMP support for Android, iOS, Desktop
- Feature tier system (Lite/Dev)
- Framework detection (Flutter, Unity, React Native, WebView)

---

## License

Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
All rights reserved.
