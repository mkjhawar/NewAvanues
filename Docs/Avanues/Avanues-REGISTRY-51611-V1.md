# PROJECT REGISTRY - Avanues Ecosystem

**Project:** Avanues Ecosystem (VoiceOS + Avanue Platform)
**Version:** 5.0.0
**Branch:** universal-restructure
**Detected Structure:** Kotlin Multiplatform (KMP) + Multi-Platform Framework
**Last Scanned:** 2025-11-02 01:21 PDT (2511020121)

---

## Overview

**Avanues** is a world-class cross-platform UI framework ecosystem combining:
- **IDEAMagic Framework** - Cross-platform UI component system
- **VoiceOS Apps** - Accessibility-focused applications
- **Avanue Platform** - Feature apps (Avanues, AIAvanue, BrowserAvanue, NoteAvanue)

**Architecture:**
- Core component definitions (platform-agnostic data models)
- Native renderers for each platform (Android/iOS/Web/Desktop)
- AvaCode DSL for declarative UI
- Foundation components (production-ready Compose implementations)

---

## Project Statistics

### Code Metrics
- **Total Kotlin Files:** 234
- **Total Lines of Code:** 56,243
- **Total Classes:** 580
- **Total Functions:** 1,297
- **Total Modules:** 27

### Platforms
- ✅ **Android** - Jetpack Compose + Material 3
- ✅ **Desktop** - Compose Desktop
- 🔄 **iOS** - Compose Multiplatform (in progress)
- 🔄 **Web** - React (planned)

---

## Module Registry

### 1. IDEAMagic Framework (Universal/IDEAMagic)

**Purpose:** Core cross-platform UI framework with DSL, components, and renderers

#### 1.1 AvaCode (`Universal/IDEAMagic/AvaCode`)

**Type:** DSL Compiler & Code Generator
**Language:** Kotlin Multiplatform
**Entry Point:** `src/commonMain/kotlin/`
**Purpose:** Parses `.vos` DSL files and generates platform-specific code

**Key Components:**
- `VosTokenizer` - Lexical analysis
- `VosParser` - Syntax analysis & AST generation
- `KotlinComposeGenerator` - Generates Kotlin Compose code
- `SwiftUIGenerator` - Generates SwiftUI code (planned)
- `ReactTypeScriptGenerator` - Generates React/TypeScript code (planned)

**Dependencies:**
- Kotlin stdlib
- Kotlin coroutines

**Status:** ✅ Parser implemented, generators in progress
**Documentation:** `Universal/IDEAMagic/AvaCode/docs/`

---

#### 1.2 AvaUI Runtime (`Universal/IDEAMagic/AvaUI`)

**Type:** UI Runtime System
**Language:** Kotlin Multiplatform
**Entry Point:** `src/commonMain/kotlin/`
**Purpose:** Runtime system for rendering AvaCode DSL

**Sub-Modules:**

##### 1.2.1 DesignSystem (`Universal/IDEAMagic/AvaUI/DesignSystem`)
- **Files:** 3 Kotlin files
- **Lines:** ~600
- **Purpose:** Material 3 design tokens and theme system
- **Key Components:**
  - `DesignTokens.kt` - Color, Typography, Spacing, Shape, Elevation, Size, Animation tokens
  - `MagicTheme.kt` - Theme composable with light/dark/dynamic color support
- **Status:** ✅ Complete

##### 1.2.2 CoreTypes (`Universal/IDEAMagic/AvaUI/CoreTypes`)
- **Files:** 1 Kotlin file
- **Lines:** ~263
- **Purpose:** Type-safe value classes for dimensions and colors
- **Key Components:**
  - `MagicDp`, `MagicSp`, `MagicPx` - Type-safe dimensions
  - `MagicColor` - Type-safe color with hex/RGB constructors
  - `MagicSize`, `MagicPadding`, `MagicBorderRadius` - Composite types
- **Status:** ✅ Complete

##### 1.2.3 StateManagement (`Universal/IDEAMagic/AvaUI/StateManagement`)
- **Files:** 1 Kotlin file
- **Lines:** ~288
- **Purpose:** Reactive state management with two-way binding
- **Key Components:**
  - `MagicState<T>` - Reactive state interface
  - `DerivedMagicState<T>` - Computed state
  - `MagicStateList<T>`, `MagicStateMap<K,V>` - Collection state
  - `rememberMagicState()` - Compose integration
- **Status:** ✅ Complete

##### 1.2.4 ThemeManager (`Universal/IDEAMagic/AvaUI/ThemeManager`)
- **Purpose:** Theme management and switching
- **Status:** 🔄 In development

##### 1.2.5 ThemeBridge (`Universal/IDEAMagic/AvaUI/ThemeBridge`)
- **Purpose:** Bridge between Core themes and platform themes
- **Status:** 🔄 In development

##### 1.2.6 UIConvertor (`Universal/IDEAMagic/AvaUI/UIConvertor`)
- **Purpose:** Convert between UI formats (DSL ↔ Platform)
- **Status:** 🔄 In development

---

#### 1.3 Components (`Universal/IDEAMagic/Components`)

**Purpose:** UI component library with Core definitions + Foundation implementations

##### 1.3.1 Core (`Universal/IDEAMagic/Components/Core`)
- **Type:** Component Definitions (Platform-Agnostic)
- **Files:** 44 Kotlin files
- **Lines:** ~8,000+
- **Purpose:** Platform-agnostic component data models

**Key Components:**
- `Component.kt` - Base component interface
- `ComponentStyle.kt` - Styling system
- `Renderer.kt` - Platform renderer interface
- **32+ Component Models:**
  - Basic: `ButtonComponent`, `TextComponent`, `ImageComponent`, `IconComponent`
  - Containers: `CardComponent`, `ChipComponent`, `DividerComponent`, `BadgeComponent`
  - Layouts: `ColumnComponent`, `RowComponent`, `ContainerComponent`, `ScrollViewComponent`
  - Lists: `ListComponent`, `ListItemComponent`
  - Forms: `TextFieldComponent`, `CheckboxComponent`, `SwitchComponent`, `RadioComponent`, `SliderComponent`, `DropdownComponent`, `DatePickerComponent`, `TimePickerComponent`, `FileUploadComponent`, `SearchBarComponent`, `RatingComponent`
  - Feedback: `DialogComponent`, `ToastComponent`, `AlertComponent`, `ProgressBarComponent`, `SpinnerComponent`, `TooltipComponent`

**Pattern:**
```kotlin
data class ButtonComponent(...) : Component {
    override fun render(renderer: Renderer): Any {
        return renderer.render(this)
    }
}
```

**Status:** ✅ 32+ components defined, all with `render()` interface
**Documentation:** `Universal/IDEAMagic/Components/docs/`

##### 1.3.2 Foundation (`Universal/IDEAMagic/Components/Foundation`)
- **Type:** Native Compose Implementations
- **Files:** 9 Kotlin files
- **Lines:** ~1,800
- **Purpose:** Production-ready Compose components for Android/Desktop

**Implemented Components:**
1. `MagicButton.kt` - Material 3 button (4 variants: Filled, Outlined, Text, Tonal)
2. `MagicText.kt` - Material 3 text (15 typography styles + 6 presets)
3. `MagicTextField.kt` - Text input with state binding (6 presets: Email, Password, Number, Phone, Search, TextArea)
4. `MagicIcon.kt` - Icon display with tint support
5. `MagicImage.kt` - Image display (3 presets: Avatar, Cover, Thumbnail)
6. `MagicCard.kt` - Container cards (3 variants: Filled, Elevated, Outlined + 3 presets)
7. `MagicLayouts.kt` - Layout primitives (V/Column, H/Row, Box, Scroll)
8. `MagicContainers.kt` - Additional containers (Surface, Divider, Badge, Chip)
9. `MagicListItem.kt` - Structured data display (6 presets)

**Features:**
- ✅ Material 3 integration
- ✅ Type-safe (MagicColor, MagicDp, MagicState)
- ✅ Presets for common patterns
- ✅ Two-way state binding
- ✅ Composable modifiers
- ✅ Light/dark theme support
- ✅ Dynamic color (Material You)

**Status:** ✅ 15 production-ready components
**Target Platforms:** Android, Desktop (JVM)
**Next:** iOS via Compose Multiplatform

##### 1.3.3 StateManagement (`Universal/IDEAMagic/Components/StateManagement`)
- **Purpose:** Component-level state management
- **Status:** 🔄 In development

##### 1.3.4 ThemeBuilder (`Universal/IDEAMagic/Components/ThemeBuilder`)
- **Purpose:** Visual theme builder tool (Compose Desktop app)
- **Status:** 🔄 20% complete
- **Planned Features:**
  - Live preview canvas
  - Property editors (colors, typography, spacing, shapes)
  - Export system (JSON/DSL/YAML)
  - Import existing themes

##### 1.3.5 AssetManager (`Universal/IDEAMagic/Components/AssetManager`)
- **Purpose:** Centralized asset management (icons, images, fonts)
- **Status:** 🔄 30% complete
- **Planned Features:**
  - Material Icons library (~2,400 icons)
  - Font Awesome library (~1,500 icons)
  - Local asset storage
  - Search with relevance scoring

##### 1.3.6 TemplateLibrary (`Universal/IDEAMagic/Components/TemplateLibrary`)
- **Purpose:** Pre-built UI templates
- **Status:** 📋 Planned
- **Planned Templates:**
  - Authentication (5 templates)
  - Dashboards (5 templates)
  - E-commerce (5 templates)
  - Social media (3 templates)
  - Utilities (2 templates)

##### 1.3.7 Phase3Components (`Universal/IDEAMagic/Components/Phase3Components`)
- **Purpose:** Advanced components (35 planned)
- **Status:** 📋 Planned
- **Components:**
  - 12 Input components (Slider, DatePicker, RadioButton, etc.)
  - 8 Display components (Badge, Chip, Avatar, etc.)
  - 5 Layout components (Grid, Stack, Drawer, etc.)
  - 4 Navigation components (AppBar, BottomNav, etc.)
  - 6 Feedback components (Alert, Modal, Toast, etc.)

##### 1.3.8 Individual Component Modules

**Checkbox** (`Universal/IDEAMagic/Components/Checkbox`)
- **Purpose:** Checkbox component (legacy/migration)
- **Status:** ⚠️ Superseded by Foundation

**TextField** (`Universal/IDEAMagic/Components/TextField`)
- **Purpose:** TextField component (legacy/migration)
- **Status:** ⚠️ Superseded by Foundation

**ColorPicker** (`Universal/IDEAMagic/Components/ColorPicker`)
- **Purpose:** Color picker component
- **Status:** 🔄 In development

**Dialog** (`Universal/IDEAMagic/Components/Dialog`)
- **Purpose:** Dialog/Modal component
- **Status:** 📋 Planned

**ListView** (`Universal/IDEAMagic/Components/ListView`)
- **Purpose:** List view component
- **Status:** 📋 Planned

---

#### 1.4 Renderers (`Universal/IDEAMagic/Components/Renderers`)

**Purpose:** Platform-specific renderers that bridge Core → Native implementations

##### 1.4.1 Android Renderer (`Universal/IDEAMagic/Components/Renderers/Android`)
- **Type:** ComposeRenderer
- **Purpose:** Renders Core components using Foundation Compose components
- **Status:** 📋 Planned (see `docs/WORLD-CLASS-ARCHITECTURE-251102-0110.md`)
- **Target:** Android + Desktop

##### 1.4.2 iOS Renderer (`Universal/IDEAMagic/Components/Renderers/iOS`)
- **Type:** SwiftUI Renderer
- **Purpose:** Renders Core components as native SwiftUI views
- **Status:** 📋 Planned
- **Target:** iOS + macOS

---

#### 1.5 Database (`Universal/IDEAMagic/Database`)

**Type:** Schema-Based Document Database
**Language:** Kotlin Multiplatform
**Purpose:** Cross-platform database abstraction

**Status:** 🔄 In development
**Platforms:** Android (SQLDelight), iOS (Core Data bridge), Desktop (SQLite)

---

#### 1.6 VoiceOSBridge (`Universal/IDEAMagic/VoiceOSBridge`)

**Type:** Legacy Bridge
**Purpose:** Bridge to VoiceOS 4.x legacy code
**Status:** ⚠️ Legacy compatibility layer

---

#### 1.7 Libraries

##### Preferences (`Universal/IDEAMagic/Libraries/Preferences`)
- **Purpose:** Cross-platform preferences/settings storage
- **Status:** 🔄 In development
- **Platforms:** Android (SharedPreferences), iOS (UserDefaults), Desktop (Preferences API)

---

### 2. Web Renderers (`Universal/Renderers`)

#### WebRenderer (`Universal/Renderers/WebRenderer`)
- **Type:** React Renderer (Kotlin/JS)
- **Purpose:** Renders Core components as React components
- **Status:** 📋 Planned
- **Technology:** Kotlin/JS + React + Material-UI

---

### 3. Tools (`Universal/Tools`)

#### AndroidStudioPlugin (`Universal/Tools/AndroidStudioPlugin`)
- **Type:** IDE Plugin
- **Purpose:** Android Studio/IntelliJ IDEA plugin for AvaCode DSL
- **Status:** 📋 Planned (60 hours)
- **Features:**
  - AvaUI visual editor
  - Code generator (DSL → Compose)
  - Component preview
  - Syntax highlighting
  - Code completion
  - Project templates

---

### 4. Assets (`Universal/Assets`)

#### Icons (`Universal/Assets/Icons`)
- **MaterialIcons** - Material Design icons
- **CustomLibrary** - Custom icon library

#### Images (`Universal/Assets/Images`)
- **Backgrounds** - Background images
- **Photos** - Photo assets

---

### 5. Applications (`apps`)

#### AvanueLaunch (`apps/avanuelaunch`)
- **Type:** Android Application
- **Purpose:** Launcher app for Avanue platform
- **Location:** `apps/avanuelaunch/android`
- **Status:** 🔄 In development

---

## Architecture Patterns

### Component System Architecture

**Three-Tier System:**

```
Tier 1: Direct Compose Usage (Fastest)
  └─ @Composable fun App() { MagicButton("Click") }
     Uses Foundation components directly

Tier 2: DSL-Driven Cross-Platform
  └─ val ui = AvaUI { Button("Click") }
     Uses Core + Renderers for cross-platform

Tier 3: AvaCode Code Generation
  └─ Button "Click" { style Filled }
     Parses DSL, generates platform code
```

### Renderer Pattern

```
Core Component Definition (1 file)
         ↓
    ┌────┴────┬────────┬────────┐
    ↓         ↓        ↓        ↓
 Android    iOS      Web    Desktop
 Renderer  Renderer Renderer Renderer
    ↓         ↓        ↓        ↓
Compose    SwiftUI   React    Compose
Material3  Native    MUI      Material3
```

---

## Dependencies

### External Dependencies
- **Kotlin** 1.9.20+
- **Compose Multiplatform** 1.5.10+
- **Material 3** (Compose)
- **Coroutines** 1.7.3+

### Internal Dependencies

**Foundation → Core:**
- Uses Core types for interop
- Not required for standalone usage

**Renderers → Core + Foundation:**
- ComposeRenderer: Core → Foundation
- iOSRenderer: Core → SwiftUI
- WebRenderer: Core → React

**AvaCode → Core:**
- Generates code that uses Core components
- Or generates direct Compose/SwiftUI/React

---

## Cross-Project Dependencies

### Related Projects
- **IDEACODE:** `/Volumes/M Drive/Coding/ideacode/` - Framework protocols
- **AVAConnect:** `/Volumes/M Drive/Coding/AVAConnect/` - Related ecosystem
- **VOS4:** `/Volumes/M Drive/Coding/Warp/vos4/` - Legacy VoiceOS

### None Detected
This project has no runtime dependencies on other projects.

---

## Duplicates Detected

### ⚠️ Potential Duplicates

**1. StateManagement**
- `Universal/IDEAMagic/AvaUI/StateManagement/` (Design system level)
- `Universal/IDEAMagic/Components/StateManagement/` (Component level)
- **Recommendation:** Clarify separation or merge

**2. Individual Component Modules**
- `Universal/IDEAMagic/Components/Checkbox/` (legacy)
- `Universal/IDEAMagic/Components/TextField/` (legacy)
- **Status:** Superseded by Foundation components
- **Recommendation:** Remove or mark as deprecated

---

## Documentation Coverage

### ✅ Complete Documentation
- `docs/COMPONENT-MERGE-ANALYSIS-251102-0015.md` - Component merge strategy
- `docs/PROTOCOL-CONFORMANCE-STRATEGY-251102-0040.md` - Protocol conformance
- `docs/WORLD-CLASS-ARCHITECTURE-251102-0110.md` - World-class architecture design
- `CLAUDE.md` - Project instructions for AI
- `README.md` - Project overview (if exists)

### 📋 Module Documentation Needed
- `docs/AvaCode/REGISTRY.md` - AvaCode module registry
- `docs/AvaUI/REGISTRY.md` - AvaUI module registry
- `docs/Components/REGISTRY.md` - Components module registry
- `docs/Foundation/REGISTRY.md` - Foundation components registry
- `docs/Core/REGISTRY.md` - Core components registry

### 📋 Constitution Files Needed
Most modules lack `CONSTITUTION.md` files defining their principles and standards.

---

## Build Configuration

**Root Build:** `build.gradle.kts`
**Settings:** `settings.gradle.kts` (27 modules included)
**Gradle Wrapper:** 8.4
**Java Version:** 17

### Quality Tools
- ✅ **Detekt** - Static analysis (v1.23.3)
- ✅ **ktlint** - Code formatting (v11.6.1)
- ✅ **JaCoCo** - Code coverage (v0.8.11, 80% target)
- ✅ **GitLab CI** - CI/CD pipeline

---

## Development Status

### Phase 1: Foundation (Weeks 1-2) - ✅ COMPLETE
- ✅ Infrastructure (CI/CD, quality tools)
- ✅ Design system (tokens, theme, types, state)
- ✅ Foundation components (15 production-ready)

### Phase 2: Core + Renderers (Weeks 3-4) - 🔄 IN PROGRESS
- ✅ Core component definitions (32+ components)
- 📋 ComposeRenderer implementation (planned)
- 📋 Update all Core `render()` methods

### Phase 3: iOS Native (Weeks 5-8) - 📋 PLANNED
- 📋 SwiftUI views for all components (32+)
- 📋 iOSRenderer (Kotlin/Native bridge)
- 📋 iOS Human Interface Guidelines compliance

### Phase 4: Web Native (Weeks 9-12) - 📋 PLANNED
- 📋 React components for all (32+)
- 📋 WebRenderer (Kotlin/JS bridge)
- 📋 Material-UI integration
- 📋 Responsive design

### Phase 5: AvaCode Generators (Weeks 13-16) - 📋 PLANNED
- 📋 Complete VosParser (DSL → AST)
- 📋 KotlinComposeGenerator
- 📋 SwiftUIGenerator
- 📋 ReactTypeScriptGenerator
- 📋 CLI tool + VS Code extension

---

## Next Steps

1. **Complete ComposeRenderer** (8-12 hours)
   - Bridge Core components → Foundation components
   - Implement all 32+ component renderers

2. **Update Core render() Methods** (4-6 hours)
   - Replace `TODO()` with `renderer.render(this)`

3. **Enhance Foundation Components** (2-4 hours)
   - Add Core features (selectable chips, divider text, list selection)

4. **Create Module Registries**
   - `docs/AvaCode/REGISTRY.md`
   - `docs/AvaUI/REGISTRY.md`
   - `docs/Components/REGISTRY.md`
   - `docs/Foundation/REGISTRY.md`
   - `docs/Core/REGISTRY.md`

5. **Create Constitution Files**
   - Define principles and standards for each module

---

## Commit History (Recent)

**Latest Commits:**
- `412bdfb` - feat(IDEAMagic): YOLO Round 2 - Complete Foundation components + fixes
- `51aadb1` - feat(IDEAMagic): Phase 1 Foundation - Infrastructure + Design System + Components
- `9915402` - refactor: Rename MagicIDEA to IDEAMagic and remove VoiceUI module
- `a5e8856` - build: Update Gradle paths for MagicIDEA structure
- `0c3265d` - docs: Add MagicIDEA system context and planning documents

---

## Version History

**Current:** 5.0.0 (IDEACODE 5.0 + IDEAMagic Framework)
**Previous:** 4.0.0 (VoiceOS 4.x legacy)

---

**Last Updated:** 2025-11-02 01:21 PDT
**Created by:** Manoj Jhawar, manoj@ideahq.net
**Framework:** IDEACODE 5.0
**Methodology:** Zero-Tolerance Protocols

---

**IDEAMagic System** ✨💡
