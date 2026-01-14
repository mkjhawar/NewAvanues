# IDEAMagic System - Existing Features Reference
**Document Type:** Static Reference Document
**Created:** 2025-11-01 15:56 PDT
**Purpose:** Complete inventory of existing code to reference BEFORE creating new solutions
**Status:** ACTIVE - Prevent recreating functionality that already exists

---

## 🚨 CRITICAL: Read This BEFORE Coding

**This document exists to prevent recreating functionality that already exists in the codebase.**

Before implementing ANY new feature:
1. **Search this document** for related functionality
2. **Reference existing code** instead of creating from scratch
3. **Extend existing modules** rather than creating new ones
4. **Update this document** when adding new features

---

## Table of Contents

1. [Voice & UUID System](#1-voice--uuid-system)
2. [AvaUI Runtime](#2-avaui-runtime)
3. [AvaElements Components](#3-avaelements-components)
4. [AvaCode DSL Parser](#4-avacode-dsl-parser)
5. [Theme System](#5-theme-system)
6. [Database Layer](#6-database-layer)
7. [Platform Libraries](#7-platform-libraries)
8. [Renderer System](#8-renderer-system)

---

## 1. Voice & UUID System

### Location
`android/standalone-libraries/uuidcreator/`

### Status
✅ **COMPLETE** - 442 lines of production code with Room persistence

### DO NOT RECREATE
This is the most important existing system. The VoiceUI SDK should **wrap** this, NOT recreate it.

### Features Already Implemented

#### Core UUID Management (`UUIDCreator.kt`)
```kotlin
package com.augmentalis.uuidcreator

class UUIDCreator(private val context: Context) : IUUIDManager {
    // ✅ ALREADY EXISTS - DO NOT RECREATE

    // UUID Generation
    fun generateUUID(): String
    fun create(context: Context): UUIDCreator
    fun generate(): String  // Static method

    // Element Registration
    fun registerElement(element: UUIDElement): String
    fun unregisterElement(uuid: String): Boolean
    fun registerWithAutoUUID(...): String

    // Element Finding
    fun findByUUID(uuid: String): UUIDElement?
    fun findByName(name: String): List<UUIDElement>
    fun findByType(type: String): List<UUIDElement>
    fun findByPosition(position: Int): UUIDElement?
    fun findInDirection(fromUUID: String, direction: String): UUIDElement?
    fun findNearest(fromUUID: String): UUIDElement?

    // Voice Command Processing
    suspend fun processVoiceCommand(command: String): UUIDCommandResult

    // Action Execution
    suspend fun executeAction(uuid: String, action: String, parameters: Map<String, Any>): Boolean

    // Spatial Navigation
    fun navigate(fromUUID: String, direction: String): UUIDElement?

    // Context Management
    fun setContext(context: String?)
    fun clearTargets()
    fun clearAll()

    // Statistics
    fun getStats(): RegistryStats
    fun getAllElements(): List<UUIDElement>
}
```

#### Voice Command Parser (Built-in)
**Location:** `UUIDCreator.kt:328-382`

Supports patterns:
- **Direct UUID**: "click element uuid abc-123"
- **Position**: "click first", "select second", "click last"
- **Direction**: "move left", "go right", "move up", "go down", "next", "previous"
- **Name**: "click settings", "open menu"
- **Context**: Fallback for unmatched commands

#### Spatial Navigator
**Location:** `uuidcreator/spatial/SpatialNavigator.kt`

Directions supported:
- LEFT, RIGHT, UP, DOWN (2D navigation)
- FORWARD, BACKWARD (depth/z-axis)
- NEXT, PREVIOUS (sequential)
- FIRST, LAST (absolute positioning)

#### Database Persistence
**Technology:** Room Database (Android)

**Location:** `uuidcreator/database/`

Files:
- `UUIDCreatorDatabase.kt` - Room database instance
- `repository/UUIDRepository.kt` - Data access layer
- `dao/` - Data access objects (UUIDElementDao, UUIDHierarchyDao, UUIDAnalyticsDao, UUIDAliasDao)
- `entity/` - Room entities
- `schemas/` - Database schemas for migration

Features:
- ✅ Persistent UUID storage across app restarts
- ✅ Hierarchical relationships (parent/child)
- ✅ Analytics tracking (command usage, performance)
- ✅ Alias support (multiple names for one element)
- ✅ Migration support (schema versioning)
- ✅ Background loading with mutex locking
- ✅ In-memory cache for fast access

#### Target Resolver
**Location:** `uuidcreator/targeting/TargetResolver.kt`

Target types:
- `UUID` - Direct UUID lookup
- `NAME` - Fuzzy name matching
- `TYPE` - Type-based filtering
- `POSITION` - Position-based selection
- `CONTEXT` - Context-aware resolution

#### Legacy Voice Command System Integration
**Lines:** 125-432 in `UUIDCreator.kt`

Maintains backwards compatibility with:
- `VoiceTarget` registration
- `VoiceCommand` history
- `CommandResult` events
- Active context flow
- Command event streams (SharedFlow)

### What Needs to Be Added

**VoiceUI SDK Wrapper** (Universal/IDEAMagic/VoiceUI/)
```kotlin
// NEW - Simple wrapper around existing uuidcreator
class VoiceUI {
    var voiceRoutingEnabled: Boolean = false  // License flag

    fun enableVoiceRouting(licenseKey: String) {
        if (validateLicense(licenseKey, tier = PRO_OR_HIGHER)) {
            voiceRoutingEnabled = true
        }
    }

    // Delegates to existing UUIDCreator
    private val uuidCreator = UUIDCreator.getInstance()
}
```

**NOT NEEDED:**
- ❌ New UUID generation system
- ❌ New voice command parser
- ❌ New spatial navigator
- ❌ New database persistence
- ❌ New target resolver

---

## 2. AvaUI Runtime

### Location
`Universal/Core/AvaUI/`

### Status
✅ **COMPLETE** - Full runtime system for interpreting DSL/YAML/JSON as data

### Features Already Implemented

#### Core Runtime (`AvaUIRuntime.kt`)
```kotlin
package com.augmentalis.voiceos.avaui

class AvaUIRuntime {
    // ✅ ALREADY EXISTS

    // Layout Loading
    fun loadLayout(format: LayoutFormat, content: String): Result<ComponentModel>

    // Component Instantiation
    fun instantiate(component: ComponentModel): PluginComponent

    // Security
    fun validateComponent(component: ComponentModel): SecurityIndicator

    // Registry Access
    val registry: ComponentRegistry
}
```

#### Component Registry (`registry/ComponentRegistry.kt`)
```kotlin
// ✅ ALREADY EXISTS
class ComponentRegistry {
    fun register(descriptor: ComponentDescriptor)
    fun findByName(name: String): ComponentDescriptor?
    fun findByType(type: String): List<ComponentDescriptor>
    fun getAllComponents(): List<ComponentDescriptor>
}
```

Built-in components registered in `registry/BuiltInComponents.kt`:
- Checkbox
- TextField
- ColorPicker
- Dialog
- ListView
- (More to be added)

#### Layout Loaders (`layout/LayoutLoader.kt`)
Supports 3 formats:
1. **DSL** (VOS format) - Primary, most concise
2. **YAML** - Secondary, human-readable
3. **JSON** - Tertiary, compact arrays

Loaders:
- `VosParser.kt` - DSL parser (tokenizer + AST)
- `YamlThemeLoader.kt` - YAML parser
- `JsonThemeLoader.kt` - JSON parser

#### Component Instantiation System
**Location:** `instantiation/`

Files:
- `ComponentInstantiator.kt` - Creates runtime components from models
- `PropertyMapper.kt` - Maps data properties to component properties
- `TypeCoercion.kt` - Automatic type conversions (String → Int, "true" → Boolean, etc.)
- `DefaultValueProvider.kt` - Provides smart defaults

#### Event System
**Location:** `events/`

Files:
- `EventBus.kt` - Pub/sub event system
- `EventContext.kt` - Event context with component metadata
- `CallbackAdapter.kt` - Adapts DSL callbacks to platform callbacks

#### Lifecycle Management
**Location:** `lifecycle/`

Files:
- `AppLifecycle.kt` - Lifecycle event handling
- `StateManager.kt` - Component state management
- `ResourceManager.kt` - Resource loading and caching

#### Voice Integration
**Location:** `voice/`

Files:
- `VoiceCommandRouter.kt` - Routes voice commands to components
- `ActionDispatcher.kt` - Dispatches actions to components
- `CommandMatcher.kt` - Matches voice commands to component actions

#### IMU/Motion Processing
**Location:** `imu/`

Files:
- `IMUOrientationData.kt` - IMU sensor data models
- `MotionProcessor.kt` - Process motion events for UI control

#### Security System
**Location:** `security/`

Files:
- `SecurityIndicator.kt` - Security level indicators (UNTRUSTED, VERIFIED, TRUSTED)

#### Core Models
**Location:** `core/`

Files:
- `ComponentModel.kt` - Data model for components (from DSL/YAML/JSON)
- `PluginComponent.kt` - Runtime component instance
- `ComponentPosition.kt` - Position and layout data
- `PluginEnums.kt` - Component type enums
- `VosFile.kt` - File abstraction for plugin resources
- `Result.kt` - Result type for error handling
- `Logger.kt` - Logging abstraction

---

## 3. AvaElements Components

### Location
`Universal/Libraries/AvaElements/`

### Status
✅ **5 COMPONENTS COMPLETE** (Checkbox, TextField, ColorPicker, Dialog, ListView)
⏳ **3 MODULES IN PROGRESS** (AssetManager, ThemeBuilder, Phase3Components)

### Completed Components

#### 1. Checkbox
**Location:** `AvaElements/Checkbox/`

**Common API:** `src/commonMain/kotlin/com/augmentalis/voiceos/checkbox/`
- `Checkbox.kt` - Expect class (383 lines)
- `CheckboxConfig.kt` - Configuration data class
- `CheckboxStyle.kt` - Style enums and presets

**iOS Implementation:** `src/iosMain/kotlin/com/augmentalis/voiceos/checkbox/Checkbox.ios.kt` (414 lines)

Features:
- ✅ Binary mode (checked/unchecked)
- ✅ Tri-state mode (checked/unchecked/indeterminate)
- ✅ Label positioning (left/right/top/bottom)
- ✅ Validation support
- ✅ Accessibility support
- ✅ Builder pattern API
- ✅ Material, iOS, Minimal, Custom styles
- ✅ Small, Medium, Large sizes
- ✅ Animations (NONE, FADE, BOUNCE, SPRING)

Usage:
```kotlin
val checkbox = CheckboxFactory.create(
    label = "Accept Terms",
    config = CheckboxConfig.material()
)

checkbox.onCheckedChange = { isChecked ->
    println("Checked: $isChecked")
}
```

#### 2. TextField
**Location:** `AvaElements/TextField/`

**Common API:** `src/commonMain/kotlin/com/augmentalis/voiceos/textfield/`
- `TextField.kt` - Expect class (367 lines)
- `TextFieldConfig.kt` - Configuration
- `TextFieldStyle.kt` - Styles

**iOS Implementation:** `src/iosMain/kotlin/com/augmentalis/voiceos/textfield/TextField.ios.kt` (389 lines)

Features:
- ✅ Single-line and multi-line modes
- ✅ Placeholder text
- ✅ Input validation
- ✅ Character limits
- ✅ Input type (text, password, email, number, phone, URL)
- ✅ Keyboard type control
- ✅ Auto-capitalization
- ✅ Auto-correction
- ✅ Clear button
- ✅ Prefix/suffix icons
- ✅ Error messages
- ✅ Disabled state

#### 3. ColorPicker
**Location:** `AvaElements/ColorPicker/`

**Common API:** `src/commonMain/kotlin/com/augmentalis/voiceos/colorpicker/`
- `ColorPicker.kt` - Expect class (264 lines)
- `ColorPickerConfig.kt` - Configuration
- `ColorPickerStyle.kt` - Styles

**iOS Implementation:** `src/iosMain/kotlin/com/augmentalis/voiceos/colorpicker/ColorPicker.ios.kt` (237 lines)

Features:
- ✅ RGB/HSB/HSL color models
- ✅ Hex input support
- ✅ Alpha channel control
- ✅ Color presets
- ✅ Recent colors
- ✅ Custom color palettes
- ✅ Eyedropper tool (platform-dependent)
- ✅ Material, iOS, Wheel, Gradient, Palette styles

#### 4. Dialog
**Location:** `AvaElements/Dialog/`

**Common API:** `src/commonMain/kotlin/com/augmentalis/voiceos/dialog/`
- `Dialog.kt` - Expect class (204 lines)
- `DialogConfig.kt` - Configuration
- `DialogStyle.kt` - Styles

**iOS Implementation:** `src/iosMain/kotlin/com/augmentalis/voiceos/dialog/Dialog.ios.kt` (258 lines)

Features:
- ✅ Alert dialogs
- ✅ Confirmation dialogs
- ✅ Custom content dialogs
- ✅ Modal and non-modal modes
- ✅ Dismiss on background click
- ✅ Title, message, custom content
- ✅ 1-3 buttons (positive, negative, neutral)
- ✅ Icon support
- ✅ Animation options
- ✅ Lifecycle callbacks (onShow, onDismiss)

#### 5. ListView
**Location:** `AvaElements/ListView/`

**Common API:** `src/commonMain/kotlin/com/augmentalis/voiceos/listview/`
- `ListView.kt` - Expect class (348 lines)
- `ListViewConfig.kt` - Configuration
- `ListViewStyle.kt` - Styles

**iOS Implementation:** `src/iosMain/kotlin/com/augmentalis/voiceos/listview/ListView.ios.kt` (377 lines)

Features:
- ✅ Vertical and horizontal orientations
- ✅ Single and multiple selection modes
- ✅ Item click, long-click callbacks
- ✅ Item swipe actions (delete, archive, etc.)
- ✅ Pull-to-refresh
- ✅ Load more (infinite scroll)
- ✅ Empty state view
- ✅ Section headers
- ✅ Separators (customizable)
- ✅ Item animations

### Renderer System

**Location:** `AvaElements/Renderers/`

#### Android Renderer
**Location:** `Renderers/Android/`

Maps AvaElements to Jetpack Compose:
- Checkbox → `androidx.compose.material3.Checkbox`
- TextField → `androidx.compose.material3.TextField`
- ColorPicker → Custom Compose UI
- Dialog → `androidx.compose.material3.AlertDialog`
- ListView → `androidx.compose.foundation.lazy.LazyColumn`

#### iOS Renderer
**Location:** `Renderers/iOS/`

Maps AvaElements to SwiftUI (via Kotlin/Native):
- Checkbox → `Toggle` or custom checkbox
- TextField → `TextField` / `TextEditor`
- ColorPicker → `ColorPicker` (iOS 14+)
- Dialog → `Alert` / `Sheet`
- ListView → `List` / `LazyVStack`

### State Management
**Location:** `AvaElements/StateManagement/`

**Status:** ✅ Complete

Provides reactive state for components:
- MutableState wrapper
- Two-way binding
- State persistence (via DataStore)
- State synchronization across platforms

### Core Library
**Location:** `AvaElements/Core/`

Shared component infrastructure:
- Base component classes
- Type definitions (Size, Color, Padding, etc.)
- Utilities
- Validation helpers

### In-Progress Modules

#### AssetManager
**Location:** `AvaElements/AssetManager/`

**Status:** ⏳ 30% Complete

**TODO:**
- Android AssetProcessor implementation
- Android LocalAssetStorage implementation
- Material Icons library (~2,400 icons)
- Font Awesome library (~1,500 icons)
- Search functionality with relevance scoring

#### ThemeBuilder
**Location:** `AvaElements/ThemeBuilder/`

**Status:** ⏳ 20% Complete

**TODO:**
- Compose Desktop application
- Live preview canvas
- Property editors (colors, typography, spacing, shapes)
- Export system (JSON/DSL/YAML)
- Import existing themes

#### Phase3Components
**Location:** `AvaElements/Phase3Components/`

**Status:** ⏳ Planned (0/35 components)

**Components to Add:**
- **Input** (12): Slider, DatePicker, TimePicker, RadioButton, RadioGroup, Dropdown, Autocomplete, FileUpload, RangeSlider, ToggleButton, Rating, SearchBar
- **Display** (8): Badge, Chip, Avatar, Tooltip, ProgressBar, Spinner, Skeleton, Divider
- **Layout** (5): Grid, Stack, Drawer, Tabs, Accordion
- **Navigation** (4): AppBar, BottomNav, Breadcrumb, Stepper
- **Feedback** (6): Alert, Snackbar, Toast, Modal, Popover, ContextMenu

---

## 4. AvaCode DSL Parser

### Location
`Universal/Core/AvaCode/`

### Status
✅ **PARTIAL** - Runtime parser exists, KSP compiler planned

### Existing Runtime Parser

**Location:** `Universal/Core/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/dsl/`

Files:
- `VosTokenizer.kt` - Lexical analysis (tokens from text)
- `VosParser.kt` - Syntax analysis (AST from tokens)
- `VosAstNode.kt` - AST node definitions
- `VosValue.kt` - Value types (String, Int, Boolean, Color, etc.)
- `VosLambda.kt` - Lambda/callback representations

**DSL Syntax Supported:**
```kotlin
// VOS DSL format (interpreted as data)
Checkbox(
    label: "Accept Terms"
    checked: false
    onCheckedChange: { println("Changed!") }
)

TextField(
    placeholder: "Enter name"
    value: ""
)

Button(
    text: "Click Me"
    onClick: { submit() }
)
```

**Key Features:**
- ✅ Tokenization (lexer)
- ✅ Parsing (parser)
- ✅ AST generation
- ✅ Type coercion (String → typed values)
- ✅ Lambda parsing (callbacks)
- ✅ Nested components (trailing lambdas)

### Planned KSP Compiler

**Status:** ⏳ Not Started

**Purpose:** Compile-time code generation for zero overhead

**Plan:**
- Use Kotlin Symbol Processor (KSP)
- Generate inline functions
- Generate value classes
- Smart default inference
- Target: <1ms overhead, 96% code reduction

**Location (planned):** `Universal/IDEAMagic/AvaCode/compiler/`

**NOT NEEDED YET** - Runtime parser works for MVP

---

## 5. Theme System

### Location (Multiple)
- `Universal/Core/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/theme/`
- `Universal/Core/ThemeManager/`
- `Universal/Core/ThemeBridge/`

### Status
✅ **COMPLETE** - Multi-format theme loading with repository

### Features Already Implemented

#### Theme Configuration
**Location:** `AvaUI/theme/ThemeConfig.kt`

Defines theme structure:
- Colors (primary, secondary, background, surface, error, etc.)
- Typography (font families, sizes, weights, line heights)
- Spacing (padding, margins, gaps)
- Shapes (corner radius, borders)
- Shadows/elevation
- Animation durations

#### Theme Loaders
**Location:** `AvaUI/theme/loaders/`

Files:
- `YamlThemeLoader.kt` - Load themes from YAML
- `YamlThemeSerializer.kt` - Serialize themes to YAML
- `JsonThemeLoader.kt` - Load themes from JSON
- `JsonThemeSerializer.kt` - Serialize themes to JSON

Supports all 3 formats (DSL/YAML/JSON) per constitution.

#### Theme Manager
**Location:** `Universal/Core/ThemeManager/`

Files:
- `ThemeManager.kt` - Theme switching, active theme management
- `ThemeRepository.kt` - Theme storage and retrieval
- `ThemeOverride.kt` - Component-level theme overrides

Features:
- ✅ Multiple themes per app
- ✅ Runtime theme switching
- ✅ Theme persistence
- ✅ Light/dark mode support
- ✅ Component overrides
- ✅ Theme inheritance

#### Theme Bridge
**Location:** `Universal/Core/ThemeBridge/`

**Purpose:** Bridge between AvaUI themes and platform themes (Material3, iOS, etc.)

**Status:** ✅ Implemented

---

## 6. Database Layer

### Location
`Universal/Core/Database/`

### Status
✅ **COMPLETE** - Schema-based document database

### Features Already Implemented

**Location:** `Database/src/commonMain/kotlin/com/augmentalis/voiceos/database/`

Files:
- `Database.kt` - Main database interface
- `DatabaseFactory.kt` - Platform-specific database creation
- `Collection.kt` - Collection (table) abstraction
- `Document.kt` - Document (row) abstraction
- `CollectionSchema.kt` - Schema definition
- `FieldType.kt` - Supported field types (String, Int, Boolean, Double, DateTime, etc.)
- `Query.kt` - Query builder

**Usage:**
```kotlin
val db = DatabaseFactory.create("myapp.db")

// Define schema
val schema = CollectionSchema(
    name = "users",
    fields = mapOf(
        "name" to FieldType.STRING,
        "age" to FieldType.INT,
        "email" to FieldType.STRING
    ),
    indexes = listOf("email")
)

// Create collection
val users = db.createCollection(schema)

// Insert document
users.insert(mapOf(
    "name" to "John",
    "age" to 30,
    "email" to "john@example.com"
))

// Query
val results = users.query()
    .where("age", ">", 18)
    .orderBy("name")
    .limit(10)
    .execute()
```

**Features:**
- ✅ Schema-based collections
- ✅ CRUD operations
- ✅ Query builder
- ✅ Indexes
- ✅ Transactions (platform-dependent)
- ✅ KMP support (commonMain)

**Platform Implementations:**
- Android: SQLite
- iOS: CoreData or SQLite
- Desktop: SQLite or JDBC

---

## 7. Platform Libraries

### Location
`avanues/libraries/` (to be migrated from android/standalone-libraries/)

### Existing Standalone Libraries

#### 1. uuidcreator
**Location:** `android/standalone-libraries/uuidcreator/`
**Status:** ✅ Complete (covered in Section 1)

#### 2. Speech Recognition
**Expected Location:** `avanues/libraries/speechrecognition/`
**Status:** ⏳ Needs migration from old codebase

Features (expected):
- Speech-to-text
- Continuous listening
- Offline recognition
- Language detection

#### 3. Voice Keyboard
**Expected Location:** `avanues/libraries/voicekeyboard/`
**Status:** ⏳ Needs migration

Features (expected):
- Voice typing
- Dictation mode
- Punctuation commands

#### 4. Device Manager
**Expected Location:** `avanues/libraries/devicemanager/`
**Status:** ⏳ Needs migration

Features (expected):
- Device info (model, OS version, screen size)
- Capabilities detection
- Hardware sensors

#### 5. Preferences
**Expected Location:** `avanues/libraries/preferences/`
**Status:** ⏳ Needs migration

Features (expected):
- Key-value storage
- Encrypted preferences
- Multi-platform support (DataStore)

#### 6. Translation
**Expected Location:** `avanues/libraries/translation/`
**Status:** ⏳ Needs migration

Features (expected):
- i18n support
- String resources
- Language switching
- Pluralization

#### 7. Logging
**Expected Location:** `avanues/libraries/logging/`
**Status:** ⏳ Needs migration

Features (expected):
- Structured logging
- Log levels (DEBUG, INFO, WARN, ERROR)
- Platform-specific loggers
- Log rotation

---

## 8. Renderer System

### Status
✅ **ANDROID COMPLETE** (Jetpack Compose)
⏳ **iOS IN PROGRESS** (SwiftUI bridge)
⏳ **WEB PLANNED** (React)

### Android Renderer

**Location:** `Universal/Libraries/AvaElements/Renderers/Android/`

**Technology:** Jetpack Compose + Material 3

**Completed Components:**
- Checkbox → `androidx.compose.material3.Checkbox`
- TextField → `androidx.compose.material3.TextField` / `OutlinedTextField`
- ColorPicker → Custom Compose implementation
- Dialog → `androidx.compose.material3.AlertDialog`
- ListView → `androidx.compose.foundation.lazy.LazyColumn` / `LazyRow`

**Features:**
- ✅ Material 3 Design
- ✅ Dark mode support
- ✅ Accessibility (TalkBack)
- ✅ Animation support
- ✅ State hoisting
- ✅ Compose Previews

### iOS Renderer

**Location:** `Universal/Libraries/AvaElements/Renderers/iOS/`

**Technology:** SwiftUI (via Kotlin/Native bridge)

**Status:** ⏳ 40% Complete (5 components with expect/actual)

**Completed Components:**
- Checkbox → `Toggle` or custom checkbox
- TextField → `TextField` / `TextEditor`
- ColorPicker → `ColorPicker` (iOS 14+)
- Dialog → `Alert` / `.sheet()`
- ListView → `List` / `LazyVStack`

**Challenges:**
- Kotlin/Native ↔ SwiftUI interop
- State synchronization
- Memory management (ARC vs Kotlin)

### Web Renderer

**Status:** ⏳ Planned (0% complete)

**Technology:** React + Material-UI

**Plan:**
- React component wrappers
- Theme converter (AvaUI → Material-UI)
- State management with hooks
- WebSocket IPC integration

**NOT STARTED YET**

---

## 9. Documentation System

### Location
`docs/`

### Completed Documentation

#### IDEAMagic System (Created Today)
- `MAGICIDEA-CONSTITUTION-251101-1412.md` (v1.1.0) - **AUTHORITATIVE GOVERNANCE**
- `CONTEXT-SUMMARY-251101-1550.md` - Complete context reference
- `Master-TODO-IDEAMagic.md` - Master task tracking (this session)
- `EXISTING-FEATURES-REFERENCE-251101-1556.md` - **THIS DOCUMENT**

#### AvaUI System (Previous)
- `MAGICUI-COMPETITIVE-ANALYSIS-251101-0110.md` - Feature comparison
- `MAGICUI-ENTERPRISE-SYSTEM-SPEC-251101-0150.md` - Technical spec (1,680 lines)
- `MAGICUI-IMPLEMENTATION-PLAN-251101-0420.md` - 40-week roadmap
- `MAGICUI-TASKS-PHASE1-251101-0426.md` - Phase 1 task breakdown (1,857 lines)
- `MAGICUI-SNIPPET-LIBRARY-251030-0352.md` - 50+ UI patterns
- `MAGICUI-CONSTITUTION-251101-1155.md` - **SUPERSEDED by MAGICIDEA-CONSTITUTION**

#### Migration Documentation
- `VOS4-Ecosystem-Migration-Plan-251028-1914.md`
- `COMPLETE-MIGRATION-AND-ROADMAP.md`
- `VoiceOS-Branding-Architecture.md`

#### Component Documentation
**Location:** `Universal/Libraries/AvaElements/docs/components/`

**Expected files (per component):**
- `[Component]-API.md` - Public API reference
- `[Component]-Examples.md` - Usage examples
- `[Component]-Implementation.md` - Platform implementation details

**Status:** ⏳ Needs creation for 5 completed components

---

## 10. Build System

### Technology
- **Gradle** (Kotlin DSL)
- **Kotlin Multiplatform (KMP)**
- **Compose Multiplatform**

### Key Files

#### Root Configuration
- `settings.gradle.kts` - Project structure, module includes
- `build.gradle.kts` - Root build configuration
- `gradle.properties` - Global properties
- `gradle/libs.versions.toml` - Dependency version catalog

#### Platform Targets
Current targets:
- `android` - Android apps (Jetpack Compose)
- `iosArm64` - iOS devices
- `iosSimulatorArm64` - iOS simulator (M1+ Macs)
- `jvm` - Desktop (Compose Desktop)
- `js` - Web (Kotlin/JS) - Planned

### Module Structure

**Universal modules:**
```
Universal/
├── Core/
│   ├── AvaUI/build.gradle.kts
│   ├── AvaCode/build.gradle.kts
│   ├── Database/build.gradle.kts
│   ├── ThemeManager/build.gradle.kts
│   └── ThemeBridge/build.gradle.kts
│
└── Libraries/
    └── AvaElements/
        ├── Core/build.gradle.kts
        ├── Checkbox/build.gradle.kts
        ├── TextField/build.gradle.kts
        ├── ColorPicker/build.gradle.kts
        ├── Dialog/build.gradle.kts
        └── ListView/build.gradle.kts
```

**Each module follows KMP structure:**
```kotlin
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    jvm()  // Desktop

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Shared dependencies
            }
        }
        val androidMain by getting {
            dependencies {
                // Android-specific
            }
        }
        val iosMain by creating {
            dependencies {
                // iOS-specific
            }
        }
        val jvmMain by getting {
            dependencies {
                // Desktop-specific
            }
        }
    }
}
```

---

## 11. Testing Infrastructure

### Status
⏳ **PARTIAL** - Some component tests exist, need expansion

### Existing Tests

**Location:** `*/src/commonTest/`, `*/src/androidTest/`, `*/src/iosTest/`

**Test Types:**
1. **Unit Tests** - Business logic, utilities
2. **Component Tests** - Individual component behavior
3. **Integration Tests** - Component interactions
4. **Snapshot Tests** - UI regression (Android with Paparazzi)

### Testing Tools

**Common:**
- `kotlin.test` - KMP testing framework
- `kotlinx-coroutines-test` - Coroutine testing

**Android:**
- `androidx.compose.ui.test` - Compose UI testing
- `Paparazzi` - Snapshot testing (planned)
- `Robolectric` - Unit testing (optional)

**iOS:**
- `XCTest` - Native iOS testing
- SwiftUI Previews

### Coverage Target
**Goal:** 80%+ coverage (per constitution)

**Status:** ⏳ Needs baseline measurement

---

## 12. CI/CD Pipeline

### Status
⏳ **PLANNED** - Not yet implemented

### Planned Tools
- GitHub Actions
- JaCoCo (code coverage)
- Detekt (static analysis)
- ktlint (code formatting)
- Slack notifications

**NOT STARTED YET**

---

## Summary: What Exists vs What's Needed

### ✅ COMPLETE (Don't Recreate)
1. **Voice & UUID System** (uuidcreator) - 442 lines, Room persistence, spatial nav
2. **AvaUI Runtime** - DSL/YAML/JSON interpretation as data
3. **5 AvaElements Components** - Checkbox, TextField, ColorPicker, Dialog, ListView
4. **Theme System** - Multi-format themes with repository
5. **Database Layer** - Schema-based document database
6. **Android Renderer** - Jetpack Compose integration
7. **DSL Parser** - Runtime parser (tokenizer + AST)

### ⏳ IN PROGRESS (Extend, Don't Replace)
1. **iOS Renderer** (40% complete) - 5 components, needs more
2. **AssetManager** (30% complete) - Needs icon libraries
3. **ThemeBuilder** (20% complete) - Needs Compose Desktop UI
4. **Phase3Components** (0% complete) - 35 components planned

### 🆕 NEEDS TO BE CREATED
1. **VoiceUI SDK Wrapper** - License flag + delegation to uuidcreator
2. **KSP Compiler** - Compile-time code generation (optional, later phase)
3. **Web Renderer** - React components
4. **CI/CD Pipeline** - GitHub Actions, coverage, linting
5. **Component Tests** - 80%+ coverage
6. **Component Documentation** - API docs, examples

---

## How to Use This Document

### Before Implementing a Feature

1. **Search this document** for keywords related to your feature
2. **Check if it exists** - If yes, use/extend it
3. **Check if it's in progress** - If yes, contribute to existing work
4. **Only create new** if it doesn't exist and isn't planned

### When Extending Existing Code

1. **Read the existing code first**
2. **Follow the existing patterns**
3. **Add tests for new functionality**
4. **Update documentation**

### When Creating New Code

1. **Verify it doesn't exist** (search this doc + codebase)
2. **Follow KMP structure** (commonMain, androidMain, iosMain, jvmMain)
3. **Follow naming conventions** (per MAGICIDEA-CONSTITUTION)
4. **Add to this document** when done

---

## Naming Conventions (Critical)

### UUID → VUID Migration

**CHANGE:**
- Voice-related UUID references → VUID
- `UVUID` → `VUID`
- `@VoiceAction(uvuid=...)` → `@VoiceAction(vuid=...)`
- Documentation references to "Universal Voice UUID"

**KEEP:**
- Generic UUID libraries (java.util.UUID, UUIDCreator class name)
- Non-voice UUID usage (element IDs, database IDs)

### File Structure Naming

**Current (scattered):**
```
Universal/Core/AvaCode/
Universal/Core/AvaUI/
Universal/Libraries/AvaElements/
```

**Target (consolidated):**
```
Universal/IDEAMagic/
├── AvaCode/
├── AvaUI/
├── VoiceUI/              # NEW
├── IDEACode/             # Future
└── IDEAFlow/             # Future
```

---

## Next Actions (From Master TODO)

### Immediate (Today)
1. ✅ Create this document
2. ⏳ Consolidate files into `Universal/IDEAMagic/` structure
3. ⏳ Update UUID → VUID references in code

### Phase 1 (Weeks 1-8)
- See `Master-TODO-IDEAMagic.md` for complete task list

---

**Document Status:** ✅ Complete
**Created by:** Manoj Jhawar, manoj@ideahq.net
**IDEAMagic System** ✨💡
**Next Review:** Weekly (Fridays)
