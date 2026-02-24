# NewAvanues Developer Quick Start Guide

Welcome to NewAvanues, a voice-first operating system layer for Android, iOS, and Desktop platforms. This guide will help you get started with building, understanding, and contributing to the project.

## 1. What is NewAvanues?

NewAvanues is a **Kotlin Multiplatform (KMP) monorepo** that creates a voice-controlled operating system layer on top of native platforms. The architecture follows a city metaphor:

- **VoiceOS** = The operating system city
- **Avanues** = Individual avenues/modules for exploring features (NoteAvanue, PhotoAvanue, WebAvanue, etc.)
- **Cockpit** = The intersection hub where avenues meet for multi-window session management

### Core Architecture Flow

```
User Speech
    ↓
SpeechRecognition (Whisper, Vivoka, Google Cloud STT)
    ↓
VoiceOSCore (NLU, intent matching)
    ↓
Command Dispatch (route to handler based on category)
    ↓
Handler (Media, Navigation, Text, Input, AppControl, Reading, VoiceControl)
    ↓
Overlay Update (visual feedback, AVID voice labels)
```

### Project Statistics

- **43+ KMP modules** in `Modules/`
- **9 apps** (primary: `apps/avanues` consolidated app)
- **5 locales** supported (en-US, es-ES, fr-FR, de-DE, hi-IN)
- **107 voice commands** per locale (62 app + 45 web)
- **395 Kotlin source files** in core modules
- **67 SQLDelight database tables**

---

## 2. Prerequisites

Before building NewAvanues, ensure you have:

### Required Tools

| Tool | Version | Why |
|------|---------|-----|
| Android Studio | Latest stable | IDE for development and debugging |
| JDK | 17+ | Kotlin 2.1.0 requires Java 17 minimum |
| Kotlin | 2.1.0 | Version locked in gradle/libs.versions.toml |
| Gradle | 8.x | Build system (wrapper provided) |
| KSP | 2.1.0-1.0.29 | Annotation processor (NOT KSP2) |

### Hardware Requirements

- **RAM**: 8GB minimum, 16GB recommended
- **Disk**: 20GB for full build cache and emulator
- **CPU**: Multi-core processor (builds run in parallel)

### Environment Setup

No manual environment variables needed. The build system auto-detects:
- `ANDROID_HOME` via Android Studio
- JDK location via Android Studio
- Gradle wrapper handles version management

---

## 3. Building and Running

### Quick Build Commands

```bash
# Build the primary consolidated app (VoiceOS + WebAvanue)
./gradlew :apps:avanues:assembleDebug

# Build a specific module
./gradlew :Modules:VoiceOSCore:build

# Run all tests in a module
./gradlew :Modules:Foundation:allTests

# Build and install on Android emulator
./gradlew :apps:avanues:installDebug
```

### Key Gradle Properties

Edit `gradle.properties` to configure the build:

```properties
# MANDATORY: Use KSP1, NOT KSP2 (KSP2 breaks KMP type resolution)
ksp.useKSP2=false

# Parallel builds (default: true)
org.gradle.parallel=true

# JVM heap size for build
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m

# Enable build cache
org.gradle.caching=true
```

### Common Build Issues

**Issue**: `error.NonExistentClass` during KSP annotation processing
- **Root cause**: `ksp.useKSP2=true` (KSP2 can't resolve types from KMP modules)
- **Fix**: Set `ksp.useKSP2=false` in `gradle.properties`

**Issue**: Out of memory during build
- **Fix**: Increase `org.gradle.jvmargs` to `-Xmx6144m` in `gradle.properties`

**Issue**: Module not found in build
- **Fix**: Ensure module is included in `settings.gradle.kts` (all 43 are pre-configured)

---

## 4. Project Structure

### Directory Layout

```
NewAvanues/
├── settings.gradle.kts          # 43 module includes + repo config
├── gradle.properties            # Build config (ksp.useKSP2=false!)
├── gradle/libs.versions.toml    # Dependency versions (single source of truth)
├── Modules/                     # 43 KMP library modules
│   ├── VoiceOSCore/            # Core voice OS engine (14k LOC)
│   ├── Database/               # SQLDelight persistence layer
│   ├── Foundation/             # KMP platform abstractions (settings, files, permissions)
│   ├── AvanueUI/               # Design system + theme (v5.1)
│   ├── WebAvanue/              # Voice-controlled browser
│   ├── AI/                     # NLU, RAG, LLM, Memory, Chat
│   ├── SpeechRecognition/      # Whisper, Vivoka, Google Cloud STT
│   ├── Cockpit/                # Multi-window display & session management
│   ├── Localization/           # Multi-language runtime switching
│   ├── AvanueUI:Core/          # Base types for design system
│   ├── AvanueUI:Theme/         # Theme engine (3 axes: palette, material, appearance)
│   ├── AvanueUI:StateManagement/
│   ├── AvanueUI:{Input,Display,Feedback,Layout,Navigation,Floating,Data,Voice}/
│   ├── Rpc/                    # gRPC + Proto definitions
│   ├── DeviceManager/          # Device info, sensors, audio, network
│   ├── Localization/           # Multi-language support
│   ├── VoiceDataManager/       # Voice data persistence
│   ├── AVU/                    # AVU format: codec (wire protocol) + DSL (interpreter)
│   ├── AVID/                   # Avanues Voice ID (unified element identifiers)
│   ├── PluginSystem/           # Generic DSL plugin framework
│   ├── PhotoAvanue/            # KMP camera + video capture
│   ├── NoteAvanue/             # KMP rich notes with voice transcription
│   ├── PDFAvanue/              # KMP PDF viewer
│   ├── VideoAvanue/            # KMP video player
│   ├── ImageAvanue/            # KMP image viewer
│   ├── AnnotationAvanue/       # KMP whiteboard/drawing canvas
│   ├── RemoteCast/             # Smart glasses screen casting (MJPEG/WebRTC)
│   ├── HTTPAvanue/             # KMP HTTP/1.1 + HTTP/2 server
│   ├── VoiceCursor/            # KMP cursor control + dwell click
│   ├── Gaze/                   # KMP eye tracking + calibration
│   ├── Logging/                # Unified KMP logging
│   ├── VoiceKeyboard/          # Voice keyboard input
│   ├── VoiceIsolation/         # Audio preprocessing
│   ├── Voice:WakeWord/         # Wake word detection
│   ├── Actions/                # Intent handlers, action execution
│   ├── LicenseManager/         # License validation
│   ├── AvanuesShared/          # iOS umbrella framework via CocoaPods
│   ├── Utilities/              # Platform utilities
│   ├── Rpc/                    # gRPC definitions
│   └── ...                     # 15+ more modules
├── apps/
│   └── avanues/                # CONSOLIDATED app (VoiceOS + WebAvanue + Gaze + Cursor)
│       └── src/main/AndroidManifest.xml
├── android/apps/               # Additional Android-specific apps
│   ├── VoiceRecognition/       # Speech recognition testing
│   ├── VoiceCursor/
│   ├── VoiceOSIPCTest/         # IPC testing
│   └── VoiceUI/
├── Docs/
│   ├── manual/                 # User manuals, guides, deployment docs
│   └── AVA/ideacode/guides/    # Module-specific dev guides (Chapters 28-90)
├── docs/
│   ├── plans/                  # Implementation plans ({Module}-Plan-{Desc}-{YYMMDD}-V{N}.md)
│   ├── fixes/                  # Bug fix documentation
│   ├── analysis/               # Code analysis investigations
│   ├── appstructure/           # Architecture Decision Records (ADRs)
│   └── reviews/                # Code reviews, security audits
└── Archive/                    # Deprecated modules and legacy code
```

### Module Tiers

**Tier 1: Core Infrastructure**
- `VoiceOSCore` — Voice command processing, overlay management, handler dispatch
- `Database` — SQLDelight persistence, 67 tables, migrations
- `SpeechRecognition` — Whisper, Vivoka, Google Cloud STT integration
- `AI` — NLU, RAG, LLM providers, memory, chat
- `Foundation` — Cross-platform utilities (settings, files, permissions)

**Tier 2: Feature Infrastructure**
- `AvanueUI` — Design system, unified components, theme (v5.1)
- `WebAvanue` — Voice-controlled browser with DOM scraping
- `PluginSystem` — DSL plugin framework (.avp text files)
- `Cockpit` — Multi-window display and session management
- `HTTPAvanue` — KMP HTTP/2 server library

**Tier 3: Content Avenues** (standalone content viewers/editors)
- `NoteAvanue` — Rich text notes with voice transcription
- `PhotoAvanue` — Camera + photo capture (CameraX on Android, AVCaptureSession on iOS)
- `PDFAvanue` — PDF viewer (PdfRenderer on Android, PDFKit on iOS)
- `VideoAvanue` — Video player (Media3 on Android, AVPlayer on iOS)
- `ImageAvanue` — Image viewer with zoom/pan/gallery
- `AnnotationAvanue` — Whiteboard/signature drawing canvas
- `RemoteCast` — Smart glasses screen casting

---

## 5. Module Architecture and KMP Patterns

### KMP Source Sets (Mandatory Convention)

All modules follow this structure:

```
Modules/ModuleName/
├── build.gradle.kts              # Dependencies, Kotlin config
└── src/
    ├── commonMain/kotlin/        # Shared code (all platforms)
    │   ├── models/               # Data classes, enums, DTOs
    │   ├── interfaces/           # expect declarations for platform code
    │   ├── implementations/       # Shared logic using interfaces
    │   └── ...
    ├── commonTest/kotlin/        # Shared unit tests
    ├── androidMain/kotlin/       # Android implementations (actual declarations)
    │   └── ...
    ├── androidUnitTest/          # JUnit tests
    ├── androidInstrumentedTest/  # Espresso/Robolectric tests
    ├── iosMain/kotlin/           # iOS implementations
    ├── iosTest/                  # iOS unit tests
    ├── desktopMain/kotlin/       # Desktop/JVM implementations
    └── desktopTest/              # Desktop unit tests
```

**MANDATORY RULE**: Platform-specific code (MediaStore, ExoPlayer, UIKit) NEVER goes in `commonMain`. Always use:

```kotlin
// commonMain/InterfaceDeclaration.kt
expect interface IMediaPlayer {
    fun play()
    fun pause()
}

// androidMain/ActualMediaPlayer.kt
actual class MediaPlayerImpl : IMediaPlayer {
    override fun play() {
        // Android ExoPlayer code
    }
}

// iosMain/ActualMediaPlayer.kt
actual class MediaPlayerImpl : IMediaPlayer {
    override fun play() {
        // iOS AVPlayer code
    }
}
```

### Shared Logic First (KMP Score = Functionality-Based)

When adding a feature:

1. **Define interface in commonMain** (abstract the platform-specific behavior)
2. **Implement shared logic in commonMain** (orchestration, state management, algorithms)
3. **Implement platform-specific code in {platform}Main** (native APIs, hardware access)
4. **KMP Score** = % of feature logic shared, NOT file count
   - Example: PhotoAvanue = 75% KMP (shared UI, state, file I/O via abstraction; 25% platform = CameraX vs AVCaptureSession)

---

## 6. Key Patterns and Conventions

### Pattern 1: Voice Command Handler Architecture

All voice commands route through handlers that implement `IHandler`:

```kotlin
// VoiceOSCore/src/commonMain/handlers/IHandler.kt
interface IHandler {
    fun canHandle(category: ActionCategory): Boolean
    fun handle(command: StaticCommand): HandlerResult
}

// VoiceOSCore/src/androidMain/handlers/MediaHandler.kt
class MediaHandler : BaseHandler() {
    override val category = ActionCategory.MEDIA

    override fun handle(command: StaticCommand): HandlerResult {
        return when (command.phrase) {
            "play" -> {
                mediaPlayer.play()
                HandlerResult.Success("Playing audio")
            }
            "pause" -> {
                mediaPlayer.pause()
                HandlerResult.Success("Paused")
            }
            else -> HandlerResult.Unmatched
        }
    }
}
```

**Registration** (AndroidHandlerFactory.kt):

```kotlin
fun createHandlers(): List<IHandler> = listOf(
    MediaHandler(),           // 13 commands
    ScreenHandler(),          // 20 commands
    TextHandler(),            // 8 commands
    InputHandler(),           // 6 commands
    AppControlHandler(),      // 4 commands
    ReadingHandler(),         // 7 commands
    VoiceControlHandler(),    // 16 commands
    // + 4 pre-existing handlers = 11 total
)
```

**Best practices**:
- Prefix commands with module name to avoid collisions: `"voicecursor show"` not just `"show"`
- Handler phrase → command text is 1:1 mapping (exact phrase match)
- Return `HandlerResult.Success()` with visual feedback message
- Return `HandlerResult.Unmatched` if handler doesn't recognize phrase

### Pattern 2: AvanueUI Theme v5.1 (MANDATORY)

Every Compose UI uses the **three-axis theme system**:

```kotlin
// Three Independent Axes
AvanueColorPalette    // SOL, LUNA, TERRA, HYDRA (4 color palettes)
MaterialMode          // Glass, Water, Cupertino, MountainView (4 material styles)
AppearanceMode        // Light, Dark, Auto (light/dark appearance)
// = 32 valid combinations (4 x 4 x 2)
```

**MANDATORY rule**: Use `AvanueTheme.colors.*`, NEVER `MaterialTheme.colorScheme.*`

```kotlin
@Composable
fun MyButton() {
    val isDark = AvanueTheme.isDark  // Appearance-aware
    val backgroundColor = AvanueTheme.colors.primary

    Button(
        colors = ButtonDefaults.buttonColors(
            containerColor = backgroundColor
        )
    ) {
        Text("Save")
    }
}
```

**Color access patterns**:

```kotlin
// For static colors
val color = AvanueTheme.colors.primary

// For appearance-aware colors (must track isDark state)
val palette = AvanueColorPalette.HYDRA
val lightColors = palette.colors(isDark = false)
val darkColors = palette.colors(isDark = true)

// For glass effect colors
val glassColor = palette.glass(isDark = true)

// For water effect colors
val waterColor = palette.water(isDark = false)
```

**DataStore keys** (settings persistence):

```kotlin
// theme_palette: "SOL", "LUNA", "TERRA", "HYDRA"
// theme_style: "Glass", "Water", "Cupertino", "MountainView"
// theme_appearance: "Light", "Dark", "Auto"
dataStore.edit { preferences ->
    preferences[stringPreferencesKey("theme_palette")] = "HYDRA"
    preferences[stringPreferencesKey("theme_style")] = "Water"
    preferences[stringPreferencesKey("theme_appearance")] = "Auto"
}
```

### Pattern 3: AVID Voice Identifiers (MANDATORY on ALL Interactive Elements)

Every button, input, checkbox, link, dialog, and interactive element MUST have AVID voice semantics:

```kotlin
// AVID format: "Voice: {action} {label}"
// Used for voice accessibility and voice command mapping

Button(
    modifier = Modifier.semantics {
        contentDescription = "Voice: click Save"
    }
) {
    Text("Save")
}

TextField(
    modifier = Modifier.semantics {
        contentDescription = "Voice: input Name"
    }
)

CheckBox(
    modifier = Modifier.semantics {
        contentDescription = "Voice: toggle Remember me"
    }
)
```

**AVID Type Codes** (predefined):

```
BTN = Button         INP = Input          SEL = Select
SWT = Switch         CHK = CheckBox       RDO = RadioButton
LNK = Link           NAV = Navigation     TAB = TabBar
DIA = Dialog         MNU = Menu           FAB = FloatingActionButton
```

**Hash format** (unified across overlay + command system):

```
BTN:7c4d2a1e  (type:hash8)
// Hash = SHA-256(packageName + className + resourceId + text + contentDescription)
```

### Pattern 4: Database Tables and Migrations

All database schemas live in SQLDelight `.sq` files:

```sql
-- Modules/Database/src/commonMain/sqldelight/tables/Command.sq
CREATE TABLE staticCommand (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  phrase TEXT NOT NULL UNIQUE,
  description TEXT,
  category TEXT NOT NULL,
  handler TEXT NOT NULL,
  locale TEXT NOT NULL,
  actionType TEXT,
  createdAt INTEGER,
  isActive INTEGER DEFAULT 1
);

-- Create named queries
selectByPhrase:
SELECT * FROM staticCommand WHERE phrase = ?;

selectByCategory:
SELECT * FROM staticCommand WHERE category = ? ORDER BY phrase;

selectAllActive:
SELECT * FROM staticCommand WHERE isActive = 1;
```

**Adding a new table**:

1. Create `.sq` file in `Modules/Database/src/commonMain/sqldelight/tables/`
2. Create repository interface in `commonMain/repositories/I{TableName}Repository.kt`
3. Implement in `commonMain/repositories/{TableName}RepositoryImpl.kt`
4. Bump `CURRENT_SCHEMA_VERSION` in `DatabaseMigrations.kt`:

```kotlin
// DatabaseMigrations.kt
object DatabaseMigrations {
    const val CURRENT_SCHEMA_VERSION = 67  // Increment by 1

    fun migrate(db: VoiceOSDatabase, oldVersion: Int, newVersion: Int) {
        when {
            oldVersion < 67 -> {
                // Migration from v66 to v67: CREATE TABLE newTable (...)
                db.exec("""
                    CREATE TABLE newTable (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }
    }
}
```

5. SQLDelight auto-generates queries from named queries in `.sq` file

**Best practices**:
- Always use prepared statements with `?` placeholders (prevents SQL injection)
- Include `createdAt INTEGER`, `updatedAt INTEGER` timestamps
- Include `isActive INTEGER DEFAULT 1` flag for soft deletes
- Name queries explicitly (`selectByX`, `insertOrUpdate`, etc.)

### Pattern 5: Plugin System — AVU DSL Format

Custom plugins use `.avp` text files (NOT compiled bytecode):

```avp
# MyPlugin.avp
plugin {
  name: "My Custom Action"
  version: "1.0"
  author: "Your Name"
}

action "open_document" {
  trigger: voice_command "open my documents"
  target: app "com.example.app"
  condition: file_exists "/sdcard/Documents"
  effect: launch_app
}

state_machine {
  state "idle" {
    transition "busy" on_command "process_data"
  }

  state "busy" {
    action: show_progress
    transition "idle" on_event "complete"
  }
}
```

**AVU interpreter** (Modules/AVU/) parses `.avp` files at runtime → no compilation needed.

---

## 7. How to Add Features

### Adding a New Voice Command Handler

**Step 1**: Create handler class in `Modules/VoiceOSCore/src/androidMain/.../handlers/{FeatureName}Handler.kt`

```kotlin
package com.augmentalis.voiceoscore.handlers

import com.augmentalis.voiceoscore.handlers.BaseHandler
import com.augmentalis.voiceoscore.models.*

class MyFeatureHandler : BaseHandler() {
    override val category = ActionCategory.CUSTOM  // Or existing category

    override fun handle(command: StaticCommand): HandlerResult {
        return when (command.phrase) {
            "myfeature do action" -> {
                // Perform action
                HandlerResult.Success("Action completed")
            }
            "myfeature show status" -> {
                HandlerResult.Success("Status: ready")
            }
            else -> HandlerResult.Unmatched
        }
    }
}
```

**Step 2**: Register in `AndroidHandlerFactory.createHandlers()`

```kotlin
fun createHandlers(): List<IHandler> = listOf(
    // ... existing handlers ...
    MyFeatureHandler(),
)
```

**Step 3**: Add voice commands to database seed or `.vos` profile

```sql
-- Modules/Database/src/commonMain/sqldelight/tables/Command.sq
INSERT INTO staticCommand (phrase, description, category, handler, locale, isActive)
VALUES ('myfeature do action', 'Do something', 'CUSTOM', 'MyFeatureHandler', 'en-US', 1);
```

**Step 4**: Add AVID semantics to any UI elements

```kotlin
Button(
    modifier = Modifier.semantics {
        contentDescription = "Voice: click Do Action"
    }
) { Text("Do Action") }
```

### Adding a New Content Module (Avenue)

**Step 1**: Create module directory structure

```bash
mkdir -p Modules/MyNewAvanue/src/{commonMain,androidMain,iosMain,desktopMain}/kotlin
```

**Step 2**: Create `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":Modules:Foundation"))
                implementation(project(":Modules:AvanueUI"))
            }
        }
    }
}
```

**Step 3**: Include module in `settings.gradle.kts`

```kotlin
include(":Modules:MyNewAvanue")
```

**Step 4**: Create content viewer/editor in `commonMain`

```kotlin
// Modules/MyNewAvanue/src/commonMain/MyAvenueScreen.kt
@Composable
fun MyAvenueScreen(contentPath: String) {
    AvanueTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AvanueTheme.colors.background)
        ) {
            Button(
                modifier = Modifier.semantics {
                    contentDescription = "Voice: click Open"
                }
            ) {
                Text("Open Content")
            }
        }
    }
}
```

**Step 5**: Wire into Cockpit or main app

```kotlin
// In app: add intent filter for content type
// Cockpit: add pane route for MyAvenueScreen
```

### Adding a New Database Table

**Step 1**: Create `.sq` file in `Modules/Database/src/commonMain/sqldelight/`

```sql
-- Modules/Database/src/commonMain/sqldelight/tables/MyTable.sq
CREATE TABLE myTable (
  id INTEGER PRIMARY KEY AUTOINCREMENT,
  name TEXT NOT NULL,
  description TEXT,
  createdAt INTEGER,
  isActive INTEGER DEFAULT 1
);

selectById:
SELECT * FROM myTable WHERE id = ?;

selectAll:
SELECT * FROM myTable WHERE isActive = 1 ORDER BY name;

insert:
INSERT INTO myTable (name, description, createdAt, isActive)
VALUES (?, ?, ?, 1);

deleteById:
DELETE FROM myTable WHERE id = ?;
```

**Step 2**: Bump schema version in `DatabaseMigrations.kt`

```kotlin
const val CURRENT_SCHEMA_VERSION = 68  // Was 67
```

**Step 3**: Add migration function

```kotlin
when {
    oldVersion < 68 -> {
        db.exec("""
            CREATE TABLE myTable (
              id INTEGER PRIMARY KEY AUTOINCREMENT,
              name TEXT NOT NULL,
              description TEXT,
              createdAt INTEGER,
              isActive INTEGER DEFAULT 1
            )
        """.trimIndent())
    }
}
```

**Step 4**: Create repository interface in `commonMain`

```kotlin
// Modules/Database/src/commonMain/repositories/IMyTableRepository.kt
interface IMyTableRepository {
    suspend fun getById(id: Long): MyTableEntity?
    suspend fun getAll(): List<MyTableEntity>
    suspend fun insert(entity: MyTableEntity)
    suspend fun deleteById(id: Long)
}
```

**Step 5**: Implement repository (SQLDelight queries auto-generated)

```kotlin
class MyTableRepositoryImpl(private val db: VoiceOSDatabase) : IMyTableRepository {
    override suspend fun getById(id: Long): MyTableEntity? = withContext(Dispatchers.IO) {
        db.myTableQueries.selectById(id).executeAsOneOrNull()?.let { it.toEntity() }
    }
    // ... other methods
}
```

---

## 8. Testing

### Test Structure

Tests use `kotlin.test` (KMP-compatible) not JUnit:

```
Modules/MyModule/
├── src/commonTest/kotlin/       # Tests run on all platforms
│   ├── models/                  # Data class serialization tests
│   └── logic/                   # Algorithm/logic tests
├── src/androidUnitTest/         # Android-only unit tests
├── src/androidInstrumentedTest/ # Android integration tests (emulator/device)
└── src/desktopTest/             # Desktop/JVM tests
```

### Running Tests

```bash
# Run all tests in a module
./gradlew :Modules:Foundation:allTests

# Run only commonTest
./gradlew :Modules:Foundation:commonTest

# Run Android instrumented tests
./gradlew :Modules:VoiceOSCore:connectedAndroidTest

# Run with coverage
./gradlew :Modules:Foundation:test --info
```

### Test Example (commonTest)

```kotlin
// Modules/Foundation/src/commonTest/kotlin/NumberToWordsTest.kt
import kotlin.test.Test
import kotlin.test.assertEquals

class NumberToWordsTest {
    @Test
    fun testSimpleNumber() {
        val result = 42.toWords(Locale.EN_US)
        assertEquals("forty-two", result)
    }

    @Test
    fun testZero() {
        val result = 0.toWords(Locale.EN_US)
        assertEquals("zero", result)
    }
}
```

### Current Coverage

Modules with **commonTest** coverage:
- Foundation
- AVID
- AVU
- VoiceOSCore
- Database

---

## 9. Code Quality Rules (Zero Tolerance)

All NewAvanues code follows these **mandatory rules**:

### Rule 1: No Stubs, No Placeholders
- Every file is fully working code
- NO `throw NotImplementedError()` or empty function bodies
- NO `TODO()` comments without implementation
- If blocked, STOP and ask for help before writing stubs

### Rule 2: Minimize Indirection
- Avoid unnecessary wrappers, proxies, bridges, aliases
- Keep real polymorphism (interfaces with multiple implementations)
- Keep KMP expect/actual (necessary cross-platform abstraction)
- Keep testing boundaries (mocks, test doubles)
- **Test**: If removing a layer breaks nothing, remove it

### Rule 3: Errors Found = Errors Fixed
- ALL errors (yours, previous sessions, pre-existing) MUST be addressed
- Present options via AskUserQuestion before fixing
- Document fixes in commit messages and docs

### Rule 4: No Quick Fixes
- Investigate root cause first
- Choose long-term optimal solution, not fast workaround
- Document WHY you chose the solution in comments

### Rule 5: Theme ONLY via AvanueUI v5.1
- NEVER use `MaterialTheme.colorScheme.*`
- ALWAYS use `AvanueTheme.colors.*`
- ALWAYS check current theme config before applying colors
- NEVER use deprecated `AvanueThemeVariant`

### Rule 6: AVID on ALL Interactive Elements
- Every Button, Input, CheckBox, Toggle, Link, Dialog needs AVID semantics
- Format: `"Voice: {action} {label}"`
- No UI ships without voice support

### Rule 7: Author Attribution
- Author: "Manoj Jhawar" or omit entirely
- NEVER write "Generated by Claude", "AI-assisted", or any AI attribution
- NEVER include "Co-Authored-By: Claude" in commit messages

---

## 10. Key Files Reference

### Build Configuration

| File | Purpose |
|------|---------|
| `settings.gradle.kts` | Includes all 43 modules + repo config |
| `gradle.properties` | Build config, `ksp.useKSP2=false` (CRITICAL) |
| `gradle/libs.versions.toml` | Single source of truth for all dependency versions |
| `build.gradle.kts` (root) | Root build configuration |

### Module Dependencies

| Module | What It Provides |
|--------|------------------|
| `Modules/VoiceOSCore` | Voice command processing, handlers, overlay management |
| `Modules/Database` | SQLDelight persistence, 67 tables, migrations |
| `Modules/Foundation` | Cross-platform utilities (settings, files, permissions, logging) |
| `Modules/AvanueUI` | Design system, theme v5.1, unified components |
| `Modules/SpeechRecognition` | Speech recognition engines (Whisper, Vivoka, Google Cloud STT) |
| `Modules/AI/*` | NLU, RAG, LLM, Memory, Chat subsystems |
| `Modules/Localization` | Multi-language support, runtime locale switching |
| `Modules/Rpc` | gRPC + Protocol Buffer definitions |
| `Modules/DeviceManager` | Device info, sensors, audio, network |

### App Entry Points

| App | Location | Purpose |
|-----|----------|---------|
| **Consolidated App** | `apps/avanues/` | Primary VoiceOS + WebAvanue + Cursor + Gaze app |
| VoiceRecognition | `android/apps/VoiceRecognition/` | Speech recognition testing app |
| VoiceUI | `android/apps/VoiceUI/` | Voice UI testing |
| VoiceOSIPCTest | `android/apps/VoiceOSIPCTest/` | IPC protocol testing |

### Database Schema

| File | Contains |
|------|----------|
| `Modules/Database/src/commonMain/sqldelight/` | All 67 `.sq` schema files |
| `Modules/Database/src/commonMain/...Queries.kt` | Generated SQLDelight queries |
| `Modules/Database/src/commonMain/migrations/DatabaseMigrations.kt` | Schema version tracking + migration functions |

### Voice Handlers

| File | Handlers |
|------|----------|
| `Modules/VoiceOSCore/src/androidMain/.../handlers/` | MediaHandler, ScreenHandler, TextHandler, InputHandler, AppControlHandler, ReadingHandler, VoiceControlHandler, + 4 pre-existing |

### Manifest and Services

| File | Purpose |
|------|---------|
| `apps/avanues/src/main/AndroidManifest.xml` | App manifest with all services, receivers, permissions |
| VoiceOSAccessibilityService | Voice-controlled screen scraping via accessibility APIs |
| VoiceOSForegroundService | Foreground audio processing service |

---

## 11. Next Steps

### For Your First Commit

1. **Set up Android Studio**:
   - Open `/Volumes/M-Drive/Coding/NewAvanues` as a project
   - Gradle will auto-sync all 43 modules
   - Wait for indexing to complete (~5 minutes)

2. **Build the project**:
   ```bash
   ./gradlew :apps:avanues:assembleDebug
   ```

3. **Read the modules relevant to your task**:
   - Architecture: Start with `Modules/Foundation` → `Modules/VoiceOSCore` → `Modules/AvanueUI`
   - Voice commands: Start with `Modules/VoiceOSCore/src/androidMain/.../handlers/`
   - Database: Start with `Modules/Database/src/commonMain/sqldelight/`

4. **Check memory files**:
   - `/Volumes/M-Drive/Coding/NewAvanues/.claude/agent-memory/doc-writer/MEMORY.md` for cross-session patterns
   - Read recent handover docs in `docs/handover/` for active work context

### Resources

- **Developer Manuals**: `Docs/MasterDocs/` (Chapters 91+)
- **Module Guides**: `Docs/AVA/ideacode/guides/` (Chapters 28-90)
- **Implementation Plans**: `docs/plans/` (format: `{Module}-Plan-{Desc}-{YYMMDD}-V{N}.md`)
- **Fix Documentation**: `docs/fixes/` (bug fixes + deep investigations)
- **Architecture Records**: `docs/appstructure/` (ADRs, class registries)

### Key Contacts

- **Project Owner**: Manoj Jhawar
- **Repository**: `/Volumes/M-Drive/Coding/NewAvanues`
- **Active Branch**: `IosVoiceOS-Development` (primary), synced with 3 other branches

---

## Quick Reference Cheat Sheet

```bash
# Build commands
./gradlew :apps:avanues:assembleDebug         # Debug build
./gradlew :Modules:VoiceOSCore:build          # Module build
./gradlew :Modules:Foundation:allTests        # Run tests

# Gradle properties (edit gradle.properties)
ksp.useKSP2=false                             # MANDATORY: Use KSP1
org.gradle.parallel=true                      # Parallel builds
org.gradle.jvmargs=-Xmx4096m                  # Heap size

# Test commands
./gradlew :Modules:Foundation:commonTest      # CommonTest only
./gradlew :Modules:VoiceOSCore:connectedAndroidTest  # Device tests

# Git
git status                                    # Check branch: IosVoiceOS-Development
git log --oneline -5                          # Recent commits
git diff                                      # Uncommitted changes

# Documentation
# Location: docs/plans/, docs/fixes/, docs/analysis/
# Naming: {Module}-{Type}-{Desc}-{YYMMDD}-V{N}.md
# Always save to docs/, NEVER to .claude/
```

---

**Document Version**: 1.0
**Last Updated**: 2026-02-22
**Author**: Manoj Jhawar
**Repository**: NewAvanues KMP Monorepo
**Main Branch**: `main` | **Active Branch**: `IosVoiceOS-Development`
