# Testing Requirements & Phase Clarification

**Date**: 2025-10-27
**Status**: Action Required - Complete Tests & Understand Architecture

---

## 🧪 Unfinished Tests - Complete Breakdown

### Current Test Status

| Component | Current Tests | Needed Tests | Estimate | Priority |
|-----------|--------------|--------------|----------|----------|
| **ColorPicker** | 126 ✅ | 0 (complete) | 0h | ✅ Done |
| **Preferences** | 16 ✅ | 0 (complete) | 0h | ✅ Done |
| **AvaUI DSL Runtime** | 0 ❌ | ~200 | 10h | 🔴 High |
| **Theme Migration Bridge** | 0 ❌ | ~90 | 5h | 🔴 High |
| **Theme Loaders** | 0 ❌ | ~60 | 4h | 🟡 Medium |
| **Integration Tests** | 0 ❌ | ~40 | 3h | 🟡 Medium |
| **TOTAL** | **142** | **~390** | **22h** | - |

---

## 📋 Test Suite Breakdown

### 1. AvaUI DSL Runtime Tests (~200 tests, 10 hours)

**Location**: `runtime/libraries/AvaUI/src/commonTest/kotlin/com/augmentalis/voiceos/avaui/`

#### Phase 1: Parser Tests (~60 tests, 3h)

**File**: `dsl/VosTokenizerTest.kt` (~20 tests)
```kotlin
class VosTokenizerTest {
    @Test fun `tokenize simple component`()
    @Test fun `tokenize nested components`()
    @Test fun `tokenize string with escape sequences`()
    @Test fun `tokenize numbers (int and float)`()
    @Test fun `tokenize colors (hex format)`()
    @Test fun `tokenize arrays`()
    @Test fun `tokenize objects`()
    @Test fun `skip single line comments`()
    @Test fun `skip multi-line comments`()
    @Test fun `handle malformed strings`()
    @Test fun `handle unexpected characters`()
    @Test fun `tokenize callbacks with arrow syntax`()
    // ... 8 more tests
}
```

**File**: `dsl/VosParserTest.kt` (~25 tests)
```kotlin
class VosParserTest {
    @Test fun `parse simple app`()
    @Test fun `parse app with components`()
    @Test fun `parse nested components`()
    @Test fun `parse component properties`()
    @Test fun `parse component callbacks`()
    @Test fun `parse voice commands`()
    @Test fun `parse lifecycle hooks`()
    @Test fun `parse theme definition`()
    @Test fun `parse metadata`()
    @Test fun `handle missing required properties`()
    @Test fun `handle invalid syntax`()
    @Test fun `handle unclosed braces`()
    @Test fun `handle invalid property values`()
    // ... 12 more tests
}
```

**File**: `dsl/VosAstNodeTest.kt` (~15 tests)
```kotlin
class VosAstNodeTest {
    @Test fun `App node creation`()
    @Test fun `Component node creation`()
    @Test fun `VosValue string creation`()
    @Test fun `VosValue number creation`()
    @Test fun `VosValue color creation`()
    @Test fun `VosValue array creation`()
    @Test fun `VosValue object creation`()
    @Test fun `VosLambda creation`()
    @Test fun `VosStatement creation`()
    // ... 6 more tests
}
```

---

#### Phase 2: Registry Tests (~30 tests, 1.5h)

**File**: `registry/ComponentRegistryTest.kt` (~15 tests)
```kotlin
class ComponentRegistryTest {
    @Test fun `register component`()
    @Test fun `retrieve component`()
    @Test fun `register duplicate component overwrites`()
    @Test fun `retrieve non-existent component returns null`()
    @Test fun `list all components`()
    @Test fun `thread safety - concurrent registration`()
    @Test fun `thread safety - concurrent retrieval`()
    // ... 8 more tests
}
```

**File**: `registry/ComponentDescriptorTest.kt` (~10 tests)
```kotlin
class ComponentDescriptorTest {
    @Test fun `create descriptor with properties`()
    @Test fun `create descriptor with callbacks`()
    @Test fun `property descriptor with defaults`()
    @Test fun `property descriptor validation`()
    @Test fun `callback descriptor with parameters`()
    // ... 5 more tests
}
```

**File**: `registry/BuiltInComponentsTest.kt` (~5 tests)
```kotlin
class BuiltInComponentsTest {
    @Test fun `ColorPicker descriptor registered`()
    @Test fun `Preferences descriptor registered`()
    @Test fun `all built-in components registered`()
    @Test fun `built-in component properties correct`()
    @Test fun `built-in component callbacks correct`()
}
```

---

#### Phase 3: Instantiation Tests (~40 tests, 2h)

**File**: `instantiation/ComponentInstantiatorTest.kt` (~15 tests)
```kotlin
class ComponentInstantiatorTest {
    @Test fun `instantiate simple component`()
    @Test fun `instantiate component with properties`()
    @Test fun `instantiate component with callbacks`()
    @Test fun `instantiate nested components`()
    @Test fun `handle missing component type`()
    @Test fun `handle invalid property type`()
    @Test fun `use default values for missing properties`()
    // ... 8 more tests
}
```

**File**: `instantiation/TypeCoercionTest.kt` (~20 tests)
```kotlin
class TypeCoercionTest {
    @Test fun `coerce string to string`()
    @Test fun `coerce string to int`()
    @Test fun `coerce string to float`()
    @Test fun `coerce string to boolean`()
    @Test fun `coerce string to color (hex)`()
    @Test fun `coerce int to color (ARGB)`()
    @Test fun `coerce string to enum`()
    @Test fun `coerce array`()
    @Test fun `coerce object`()
    @Test fun `handle invalid int conversion`()
    @Test fun `handle invalid float conversion`()
    @Test fun `handle invalid boolean conversion`()
    @Test fun `handle invalid color format`()
    @Test fun `handle invalid enum value`()
    // ... 6 more tests
}
```

**File**: `instantiation/PropertyMapperTest.kt` (~5 tests)
```kotlin
class PropertyMapperTest {
    @Test fun `map properties with defaults`()
    @Test fun `map properties without defaults`()
    @Test fun `map required properties`()
    @Test fun `handle missing required property`()
    @Test fun `handle extra properties`()
}
```

---

#### Phase 4: Event/Callback Tests (~25 tests, 1.5h)

**File**: `events/EventBusTest.kt` (~10 tests)
```kotlin
class EventBusTest {
    @Test fun `emit and collect event`()
    @Test fun `multiple subscribers receive event`()
    @Test fun `event buffer handles overflow`()
    @Test fun `events delivered in order`()
    @Test fun `handle rapid event emission`()
    // ... 5 more tests
}
```

**File**: `events/CallbackAdapterTest.kt` (~12 tests)
```kotlin
class CallbackAdapterTest {
    @Test fun `create callback from lambda`()
    @Test fun `execute callback with parameters`()
    @Test fun `execute function call statement`()
    @Test fun `execute assignment statement`()
    @Test fun `execute if statement`()
    @Test fun `handle undefined variable`()
    @Test fun `handle undefined function`()
    @Test fun `nested function calls`()
    // ... 4 more tests
}
```

**File**: `events/EventContextTest.kt` (~3 tests)
```kotlin
class EventContextTest {
    @Test fun `create context with variables`()
    @Test fun `create child context`()
    @Test fun `variable scope resolution`()
}
```

---

#### Phase 5: Voice Command Tests (~20 tests, 1h)

**File**: `voice/VoiceCommandRouterTest.kt` (~10 tests)
```kotlin
class VoiceCommandRouterTest {
    @Test fun `exact match command`()
    @Test fun `fuzzy match similar command (85%)`()
    @Test fun `fuzzy match threshold (70%)`()
    @Test fun `no match below threshold`()
    @Test fun `multiple commands - best match wins`()
    @Test fun `register duplicate command overwrites`()
    // ... 4 more tests
}
```

**File**: `voice/CommandMatcherTest.kt` (~10 tests)
```kotlin
class CommandMatcherTest {
    @Test fun `levenshtein distance calculation`()
    @Test fun `word overlap calculation`()
    @Test fun `similarity score exact match`()
    @Test fun `similarity score partial match`()
    @Test fun `similarity score with extra words`()
    @Test fun `similarity case insensitive`()
    @Test fun `handle empty strings`()
    // ... 3 more tests
}
```

---

#### Phase 6: Lifecycle Tests (~15 tests, 1h)

**File**: `lifecycle/AppLifecycleTest.kt` (~10 tests)
```kotlin
class AppLifecycleTest {
    @Test fun `lifecycle state transitions (CREATED to STARTED)`()
    @Test fun `lifecycle state transitions (STARTED to RESUMED)`()
    @Test fun `lifecycle state transitions (RESUMED to PAUSED)`()
    @Test fun `lifecycle state transitions (PAUSED to RESUMED)`()
    @Test fun `lifecycle state transitions (PAUSED to STOPPED)`()
    @Test fun `lifecycle state transitions (STOPPED to DESTROYED)`()
    @Test fun `observers notified on create`()
    @Test fun `observers notified on destroy`()
    @Test fun `invalid state transition rejected`()
    @Test fun `state flow emits current state`()
}
```

**File**: `lifecycle/ResourceManagerTest.kt` (~3 tests)
```kotlin
class ResourceManagerTest {
    @Test fun `register resource`()
    @Test fun `release all resources`()
    @Test fun `release called on each resource`()
}
```

**File**: `lifecycle/StateManagerTest.kt` (~2 tests)
```kotlin
class StateManagerTest {
    @Test fun `save and restore state`()
    @Test fun `restore non-existent state returns empty`()
}
```

---

#### Phase 7: Runtime Integration Tests (~10 tests, 1h)

**File**: `AvaUIRuntimeTest.kt` (~10 tests)
```kotlin
class AvaUIRuntimeTest {
    @Test fun `load simple app`()
    @Test fun `start app creates running instance`()
    @Test fun `handle voice command triggers action`()
    @Test fun `pause app transitions to paused state`()
    @Test fun `resume app transitions to resumed state`()
    @Test fun `stop app destroys resources`()
    @Test fun `lifecycle hooks called in order`()
    @Test fun `multiple apps can run simultaneously`()
    @Test fun `invalid DSL throws parse error`()
    @Test fun `component instantiation failure handled`()
}
```

---

### 2. Theme Migration Bridge Tests (~90 tests, 5 hours)

**Location**: `runtime/libraries/ThemeBridge/src/commonTest/kotlin/com/augmentalis/voiceos/themebridge/`

#### ColorConversionUtils Tests (~30 tests, 1.5h)

**File**: `ColorConversionUtilsTest.kt`
```kotlin
class ColorConversionUtilsTest {
    // Int ↔ Hex Conversion (10 tests)
    @Test fun `intToHex with alpha`()
    @Test fun `intToHex without alpha`()
    @Test fun `intToHex uppercase`()
    @Test fun `intToHex lowercase`()
    @Test fun `hexToInt with alpha (#AARRGGBB)`()
    @Test fun `hexToInt without alpha (#RRGGBB)`()
    @Test fun `hexToInt short format (#RGB)`()
    @Test fun `hexToInt with hash prefix`()
    @Test fun `hexToInt without hash prefix`()
    @Test fun `roundtrip conversion (int → hex → int)`()

    // Color Manipulation (10 tests)
    @Test fun `darken color by 20%`()
    @Test fun `darken color by 50%`()
    @Test fun `lighten color by 20%`()
    @Test fun `lighten color by 50%`()
    @Test fun `darken already dark color doesn't go below 0`()
    @Test fun `lighten already light color doesn't exceed 255`()
    @Test fun `adjustSaturation increase`()
    @Test fun `adjustSaturation decrease`()
    @Test fun `adjustBrightness increase`()
    @Test fun `adjustBrightness decrease`()

    // Contrast & Accessibility (10 tests)
    @Test fun `calculateContrast white on black`()
    @Test fun `calculateContrast black on white`()
    @Test fun `meetsWCAGAA valid contrast`()
    @Test fun `meetsWCAGAA invalid contrast`()
    @Test fun `meetsWCAGAAA valid contrast`()
    @Test fun `meetsWCAGAAA invalid contrast`()
    @Test fun `adjustForContrast improves contrast`()
    @Test fun `adjustForContrast handles already valid`()
    @Test fun `findContrastingColor for background`()
    @Test fun `findContrastingColor extremes (black/white)`()
}
```

---

#### ThemeConverter Tests (~30 tests, 1.5h)

**File**: `ThemeConverterTest.kt`
```kotlin
class ThemeConverterTest {
    // Avanue4 → AvaUI (15 tests)
    @Test fun `convert Avanue4 to AvaUI all components`()
    @Test fun `convert Avanue4 primary color`()
    @Test fun `convert Avanue4 secondary color`()
    @Test fun `convert Avanue4 background colors`()
    @Test fun `convert Avanue4 text colors`()
    @Test fun `convert Avanue4 with missing components uses defaults`()
    @Test fun `convert Avanue4 with invalid types throws error`()
    @Test fun `convert Avanue4 preserves alpha channel`()
    @Test fun `convert Avanue4 roundtrip (Avanue4 → AvaUI → Avanue4)`()
    @Test fun `convert empty Avanue4 theme`()
    @Test fun `convert Avanue4 with null values`()
    @Test fun `convert Avanue4 dark theme`()
    @Test fun `convert Avanue4 light theme`()
    @Test fun `convert Avanue4 high contrast theme`()
    @Test fun `convert Avanue4 color mapping complete`()

    // AvaUI → Avanue4 (15 tests)
    @Test fun `convert AvaUI to Avanue4 all components`()
    @Test fun `convert AvaUI primary color`()
    @Test fun `convert AvaUI secondary color`()
    @Test fun `convert AvaUI background colors`()
    @Test fun `convert AvaUI text colors`()
    @Test fun `convert AvaUI preserves alpha channel`()
    @Test fun `convert AvaUI with defaults`()
    @Test fun `convert AvaUI roundtrip (AvaUI → Avanue4 → AvaUI)`()
    @Test fun `convert AvaUI invalid hex format throws error`()
    @Test fun `convert AvaUI dark theme`()
    @Test fun `convert AvaUI light theme`()
    @Test fun `convert AvaUI complete palette`()
    @Test fun `convert AvaUI typography data`()
    @Test fun `convert AvaUI spacing data`()
    @Test fun `convert AvaUI effects data`()
}
```

---

#### ThemeMigrationBridge Tests (~30 tests, 2h)

**File**: `ThemeMigrationBridgeTest.kt`
```kotlin
class ThemeMigrationBridgeTest {
    // Initialization (5 tests)
    @Test fun `initialize loads current theme from legacy`()
    @Test fun `initialize with null legacy theme`()
    @Test fun `initialize registers as observer`()
    @Test fun `initialize converts theme correctly`()
    @Test fun `multiple initialize calls don't duplicate observers`()

    // Bidirectional Sync (10 tests)
    @Test fun `update AvaUI theme syncs to legacy`()
    @Test fun `update AvaUI theme emits StateFlow`()
    @Test fun `legacy theme change syncs to AvaUI`()
    @Test fun `bidirectional sync enabled updates both ways`()
    @Test fun `bidirectional sync disabled only updates AvaUI`()
    @Test fun `sync does not create infinite loop`()
    @Test fun `sync flag prevents recursion (AvaUI → Legacy)`()
    @Test fun `sync flag prevents recursion (Legacy → AvaUI)`()
    @Test fun `rapid updates handled correctly`()
    @Test fun `concurrent updates thread-safe`()

    // Component Updates (10 tests)
    @Test fun `updateComponent primary color`()
    @Test fun `updateComponent secondary color`()
    @Test fun `updateComponent background color`()
    @Test fun `updateComponent syncs to legacy`()
    @Test fun `updateComponent emits StateFlow`()
    @Test fun `updateComponent with invalid value throws error`()
    @Test fun `updateComponent with unknown component ignored`()
    @Test fun `batch component updates`()
    @Test fun `updateComponent preserves other components`()
    @Test fun `updateComponent triggers observer notification`()

    // Edge Cases (5 tests)
    @Test fun `dispose removes observer`()
    @Test fun `dispose stops syncing`()
    @Test fun `legacy manager null theme handled`()
    @Test fun `StateFlow collects all updates`()
    @Test fun `bridge works without bidirectional sync`()
}
```

---

### 3. Theme Loaders Tests (~60 tests, 4 hours)

**Location**: `runtime/libraries/AvaUI/src/commonTest/kotlin/com/augmentalis/voiceos/avaui/theme/loaders/`

#### YAML Theme Tests (~15 tests, 1h)

**File**: `YamlThemeLoaderTest.kt` & `YamlThemeSerializerTest.kt`
```kotlin
class YamlThemeLoaderTest {
    @Test fun `load complete YAML theme`()
    @Test fun `load YAML with missing palette uses defaults`()
    @Test fun `load YAML with missing typography uses defaults`()
    @Test fun `load YAML with custom colors`()
    @Test fun `load YAML with invalid format throws error`()
    @Test fun `load YAML with comments`()
    @Test fun `roundtrip (YAML → ThemeConfig → YAML)`()
    // ... 8 more tests
}

class YamlThemeSerializerTest {
    @Test fun `serialize complete theme to YAML`()
    @Test fun `serialize with all properties`()
    @Test fun `serialized YAML is valid`()
    @Test fun `serialize preserves color format`()
    // ... 4 more tests
}
```

---

#### JSON Theme Tests (~15 tests, 1h)

**File**: `JsonThemeLoaderTest.kt` & `JsonThemeSerializerTest.kt`
```kotlin
class JsonThemeLoaderTest {
    @Test fun `load complete JSON theme`()
    @Test fun `load JSON with missing properties uses defaults`()
    @Test fun `load JSON with null values handled`()
    @Test fun `load JSON with invalid format throws error`()
    @Test fun `roundtrip (JSON → ThemeConfig → JSON)`()
    // ... 10 more tests
}

class JsonThemeSerializerTest {
    @Test fun `serialize complete theme to JSON`()
    @Test fun `serialize with pretty print`()
    @Test fun `serialized JSON is valid`()
    @Test fun `serialize preserves structure`()
    // ... 4 more tests
}
```

---

#### Compose Theme Tests (~15 tests, 1h) - androidTest

**File**: `ComposeThemeImporterTest.kt` & `ComposeThemeExporterTest.kt` (androidTest)
```kotlin
class ComposeThemeImporterTest {
    @Test fun `import Material3 ColorScheme`()
    @Test fun `import Material3 Typography`()
    @Test fun `import complete Material3 theme`()
    @Test fun `import converts ARGB to hex correctly`()
    @Test fun `roundtrip (Compose → AvaUI → Compose)`()
    // ... 10 more tests
}

class ComposeThemeExporterTest {
    @Test fun `export to Material3 ColorScheme`()
    @Test fun `export to Material3 Typography`()
    @Test fun `export preserves colors`()
    @Test fun `exported theme works in Compose`()
    // ... 4 more tests
}
```

---

#### XML Theme Tests (~15 tests, 1h) - androidTest

**File**: `XmlThemeImporterTest.kt` & `XmlThemeExporterTest.kt` (androidTest)
```kotlin
class XmlThemeImporterTest {
    @Test fun `import from colors xml`()
    @Test fun `import from dimens xml`()
    @Test fun `import from styles xml`()
    @Test fun `import with resource IDs`()
    @Test fun `import missing resources uses defaults`()
    // ... 10 more tests
}

class XmlThemeExporterTest {
    @Test fun `export colors xml`()
    @Test fun `export dimens xml`()
    @Test fun `export styles xml`()
    @Test fun `exported XML is valid`()
    @Test fun `exported resources can be parsed`()
    // ... 5 more tests
}
```

---

### 4. Integration Tests (~40 tests, 3 hours)

**Location**: `runtime/libraries/AvaUI/src/commonTest/kotlin/com/augmentalis/voiceos/avaui/integration/`

#### End-to-End DSL Tests (~20 tests, 1.5h)

**File**: `EndToEndDslTest.kt`
```kotlin
class EndToEndDslTest {
    @Test fun `load and run simple ColorPicker app`()
    @Test fun `load and run app with voice commands`()
    @Test fun `load and run app with lifecycle hooks`()
    @Test fun `load and run app with theme`()
    @Test fun `voice command triggers component action`()
    @Test fun `callback updates preferences`()
    @Test fun `lifecycle hooks execute in order`()
    @Test fun `nested components render correctly`()
    @Test fun `multiple apps run independently`()
    @Test fun `app state persists across pause/resume`()
    // ... 10 more tests
}
```

---

#### Theme Bridge Integration Tests (~10 tests, 1h)

**File**: `ThemeBridgeIntegrationTest.kt`
```kotlin
class ThemeBridgeIntegrationTest {
    @Test fun `Avanue4 app uses AvaUI theme via bridge`()
    @Test fun `theme update in Avanue4 reflects in AvaUI`()
    @Test fun `theme update in AvaUI reflects in Avanue4`()
    @Test fun `theme change during app lifecycle`()
    @Test fun `multiple apps share same theme bridge`()
    @Test fun `theme import from YAML applies to bridge`()
    @Test fun `theme export to JSON from bridge`()
    @Test fun `bridge handles rapid theme changes`()
    @Test fun `bridge cleanup on app destroy`()
    @Test fun `theme persistence across restarts`()
}
```

---

#### Theme Loader Integration Tests (~10 tests, 0.5h)

**File**: `ThemeLoaderIntegrationTest.kt`
```kotlin
class ThemeLoaderIntegrationTest {
    @Test fun `load YAML theme and apply to app`()
    @Test fun `load JSON theme and apply to app`()
    @Test fun `convert YAML → ThemeConfig → JSON`()
    @Test fun `convert JSON → ThemeConfig → YAML`()
    @Test fun `import Compose theme and export to XML`()
    @Test fun `import XML theme and export to Compose`()
    @Test fun `theme chain (YAML → AvaUI → Avanue4)`()
    @Test fun `theme chain (Compose → AvaUI → JSON)`()
    @Test fun `all loaders produce compatible ThemeConfig`()
    @Test fun `theme conversion preserves all data`()
}
```

---

## 🎯 Phase 6 Clarification: AvaCode Codegen

### What is Phase 6?

**Phase 6** is **NOT** in the main task list (tasks.md). It's a **NEW infrastructure component** that was identified during this session.

**Official Name**: AvaCode Codegen
**Status**: Not started (0%)
**Priority**: Medium (not blocking)
**Estimate**: 20-30 hours

### What Does Phase 6 Do?

**AvaCode Codegen** generates native Kotlin/Swift/JS source code from .vos files.

#### Difference from AvaUI:

| Feature | AvaUI (Phase 4.5) | AvaCode (Phase 6) |
|---------|---------------------|---------------------|
| **File Mode** | `#!vos:D` | `#!vos:K` |
| **Purpose** | Runtime interpretation | Code generation |
| **Execution** | Interpreted at runtime | Compiled native code |
| **Performance** | Good (interpreted) | Excellent (compiled) |
| **Hot Reload** | ✅ Yes | ❌ No (rebuild required) |
| **Use Case** | User apps, prototypes | Production apps |
| **Output** | Runs in AvaUI Runtime | Kotlin/Swift/JS files |

### Phase 6 Sub-Phases (5 phases, 20-30h)

#### Sub-Phase 1: Design Specification (2h)
- Create codegen architecture document
- Define code templates
- Specify target languages (Kotlin Compose, SwiftUI, React)
- Define annotation system (@Generate, @Imports, etc.)

#### Sub-Phase 2: Kotlin Compose Generator (8h)
- Parse .vos files (reuse AvaUI parser)
- Generate Kotlin Compose code
- Handle component mapping (ColorPicker → ColorPickerView)
- Generate state management (remember, mutableStateOf)
- Generate callbacks (DSL lambdas → Kotlin lambdas)
- Generate imports

**Example Input** (.vos):
```
#!vos:K
@Generate(target: "kotlin-compose", package: "com.example.generated")
App {
  id: "com.example.app"
  ColorPicker {
    initialColor: "#FF5722"
    onConfirm: (color) => {
      preferencesManager.set("theme", color)
    }
  }
}
```

**Example Output** (Kotlin):
```kotlin
package com.example.generated

import androidx.compose.runtime.*
import com.augmentalis.voiceos.colorpicker.ColorPickerView

@Composable
fun App() {
    var selectedColor by remember { mutableStateOf("#FF5722") }

    ColorPickerView(
        initialColor = selectedColor,
        onConfirm = { color ->
            preferencesManager.set("theme", color)
        }
    )
}
```

#### Sub-Phase 3: SwiftUI Generator (8h)
- Generate SwiftUI code
- Handle iOS component mapping
- Generate SwiftUI state management (@State, @Binding)
- Generate SwiftUI views

#### Sub-Phase 4: Template Engine (4h)
- Create reusable templates for components
- Handle code formatting
- Support custom templates
- Plugin system for new components

#### Sub-Phase 5: Integration & Testing (5h)
- Integration with AvaUI parser
- CLI tool for code generation
- Gradle plugin for build integration
- Integration tests
- Documentation

### When to Build Phase 6?

**Recommended Timeline**:
1. ✅ Complete all tests first (22h) - ensures infrastructure quality
2. ⏳ Complete library migrations (13 libraries, 2-4 weeks) - ensures platform completeness
3. 🔮 Then build AvaCode Codegen (20-30h) - enables production app generation

**Why This Order?**:
- Tests validate infrastructure works correctly
- Library migrations complete the platform
- Codegen is optimization, not requirement
- DSL apps can run without codegen

---

## 🏗️ Phase 5 Explanation: VoiceOS Application Restructure

### What is Phase 5?

**Phase 5** restructures **VoiceOS** from an embedded app to a **standalone application** that uses Avanues as a platform via **composite build**.

### Current Architecture (Before Phase 5)

```
Avanues/
├── runtime/libraries/        # Platform libraries
│   ├── AvaUI/
│   ├── ColorPicker/
│   ├── Preferences/
│   └── ... (13 more)
└── apps/                     # Embedded apps (inside platform)
    ├── examples/
    └── user-apps/
```

**Problem**: VoiceOS is embedded in the platform repository.

### Target Architecture (After Phase 5)

```
/Volumes/M Drive/Coding/
├── Avanues/              # Platform (libraries only)
│   └── runtime/libraries/
│       ├── AvaUI/
│       ├── ColorPicker/
│       └── ... (15 libraries)
│
├── VoiceOS/                  # Standalone app (separate repo)
│   ├── settings.gradle.kts   # Composite build: includeBuild("../Avanues")
│   ├── app/
│   │   ├── build.gradle.kts  # Dependencies: implementation(project(":runtime:libraries:AvaUI"))
│   │   └── src/main/
│   │       ├── kotlin/       # VoiceOS app code
│   │       └── res/          # Android resources
│   └── config/
│       ├── voiceos.yaml      # App definition
│       └── layouts/
│           ├── main.dsl
│           ├── settings.dsl
│           ├── keyboard.dsl
│           └── browser.dsl
│
└── AVA AI/                   # Separate app (already exists)
    ├── settings.gradle.kts   # Composite build: includeBuild("../Avanues")
    └── app/
        └── build.gradle.kts  # Dependencies: implementation(project(":runtime:libraries:AvaUI"))
```

### What is Composite Build?

**Composite Build** allows one Gradle project to include another project as a dependency **without publishing to Maven**.

**Example**: `VoiceOS/settings.gradle.kts`
```kotlin
pluginManagement {
    includeBuild("../Avanues")
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "VoiceOS"
include(":app")
```

**Example**: `VoiceOS/app/build.gradle.kts`
```kotlin
dependencies {
    // These resolve via composite build (no Maven publishing needed)
    implementation(project(":runtime:libraries:AvaUI"))
    implementation(project(":runtime:libraries:SpeechRecognition"))
    implementation(project(":runtime:libraries:VoiceKeyboard"))
    implementation(project(":runtime:libraries:Accessibility"))
    implementation(project(":runtime:libraries:ColorPicker"))
    implementation(project(":runtime:libraries:Preferences"))
    // ... etc
}
```

### Benefits of Phase 5

1. **Separation of Concerns**
   - Platform (Avanues) = libraries only
   - Applications (VoiceOS, AVA AI) = separate repos

2. **Independent Development**
   - VoiceOS team works independently
   - Platform team works independently
   - No merge conflicts

3. **Clean Dependencies**
   - VoiceOS depends ON platform
   - Platform does NOT depend on VoiceOS

4. **Version Control**
   - Each app has own Git repository
   - Each app has own versioning
   - Platform has own versioning

5. **Composite Build = No Publishing**
   - No Maven/GitHub Packages needed
   - Fast local development
   - Live updates during development

### Phase 5 Tasks (26 tasks, 5-7 days)

**Dependency**: Requires **Phase 4 (US2) complete** (all 13 libraries migrated)

**Key Tasks**:
1. Create `/Volumes/M Drive/Coding/VoiceOS/` directory (separate from Avanues)
2. Create `settings.gradle.kts` with `includeBuild("../Avanues")`
3. Create VoiceOS app code using AvaUI
4. Create config files (voiceos.yaml, layouts/*.dsl)
5. Build and verify VoiceOS runs as standalone app

**When to Start**: After all library migrations complete (Phase 4)

---

## 📊 Summary: What to Do Next

### Priority 1: Complete All Tests (22 hours) 🔴

**Why First?**:
- Validates all infrastructure works correctly
- Catches bugs early
- Ensures production quality
- Required before library migrations

**Breakdown**:
1. AvaUI DSL Runtime tests (~200 tests, 10h)
2. Theme Migration Bridge tests (~90 tests, 5h)
3. Theme Loaders tests (~60 tests, 4h)
4. Integration tests (~40 tests, 3h)

---

### Priority 2: Library Migrations (13 libraries, 2-4 weeks) 🟡

**After tests pass**, migrate remaining libraries:
1. Notepad (1-2 days)
2. Browser (2-3 days)
3. CloudStorage (2-3 days)
4. FileManager (3-4 days)
5. RemoteControl (2-3 days)
6. Keyboard (3-4 days)
7. CommandBar (2-3 days)
8. Logger (2-3 days)
9. Storage (2-3 days)
10. Task (2-3 days)
11. VoskModels (2-3 days)
12. Merge DeviceManager (3-7 days)
13. Merge SpeechRecognition (3-7 days)

---

### Priority 3: Phase 5 - VoiceOS Restructure (5-7 days) 🟢

**After library migrations complete**, restructure VoiceOS as standalone app.

---

### Priority 4: Phase 6 - AvaCode Codegen (20-30 hours) 🔵

**Optional** - Build when ready for production app generation.

---

## 🎯 Immediate Action Plan

### Step 1: Write AvaUI DSL Runtime Tests (10 hours)
Start with parser tests, then registry, instantiation, events, voice, lifecycle, runtime.

### Step 2: Write Theme Bridge Tests (5 hours)
Test color conversion, theme conversion, and bridge integration.

### Step 3: Write Theme Loader Tests (4 hours)
Test YAML, JSON, Compose, XML loaders.

### Step 4: Write Integration Tests (3 hours)
Test end-to-end DSL apps, theme bridge integration, theme loader integration.

### Step 5: Verify All Tests Pass (1 hour)
Run `./gradlew test` and verify **~532 tests passing** (142 current + 390 new).

---

**Once all tests pass, infrastructure is 100% production-ready and we can start library migrations with confidence!**

---

**Created by**: Manoj Jhawar, manoj@ideahq.net
**Date**: 2025-10-27
**Version**: 1.0.0
