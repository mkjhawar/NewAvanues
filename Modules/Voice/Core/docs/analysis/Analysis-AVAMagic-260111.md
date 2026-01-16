# AVAMagic & MAGIC Modules - Complete Analysis

**Date:** 2026-01-11
**Branch:** Refactor-Investigation
**Author:** Claude Analysis

---

## 1. Module Structure Overview

```
Modules/AVAMagic/
├── AVAUI/                    # Core UI framework (KMP)
│   ├── Core/                 # Base DSL & components
│   ├── StateManagement/      # Reactive state (MagicState)
│   ├── DesignSystem/         # Theme system (MagicTheme)
│   ├── Foundation/           # Foundation components
│   ├── Renderers/            # Platform renderers
│   │   ├── Android/
│   │   ├── Desktop/
│   │   └── iOS/
│   ├── Theme/                # Theme management
│   ├── Layout/               # Layout components
│   ├── Input/                # Input components
│   ├── Feedback/             # Feedback components
│   ├── Navigation/           # Navigation components
│   └── VoiceCommandRouter/   # Voice routing
│
├── MagicUI/                  # Extended UI system
│   ├── Components/           # Component library
│   │   ├── Core/             # MagicUI DSL
│   │   ├── Foundation/       # Base components
│   │   ├── StateManagement/  # State management
│   │   └── Renderers/        # Platform renderers
│   ├── DesignSystem/         # Design system
│   ├── Theme/                # Theme parser
│   └── StateManagement/      # State (MagicState)
│
├── MagicCode/                # Code generation
│   ├── CLI/                  # AvaCode CLI
│   ├── AST/                  # Abstract Syntax Tree
│   ├── Forms/                # Form builder
│   ├── Templates/            # Code templates
│   └── Workflows/            # Workflow system
│
├── MagicTools/               # Development tools
│   └── LanguageServer/       # LSP implementation
│
├── Core/                     # Core utilities
│   ├── accessibility-types/
│   ├── command-models/
│   ├── constants/
│   ├── database/
│   ├── exceptions/
│   ├── hash/
│   ├── json-utils/
│   ├── result/
│   ├── text-utils/
│   ├── validation/
│   └── voiceos-logging/
│
├── Libraries/                # Shared libraries
│   ├── SpeechRecognition/
│   ├── VivokaSDK/
│   ├── VoiceKeyboard/
│   ├── PluginSystem/
│   ├── JITLearning/
│   └── VoiceUIElements/
│
├── apps/                     # Application modules
│   ├── VoiceOS/
│   ├── VoiceOSCore/
│   ├── VoiceUI/
│   ├── VoiceCursor/
│   └── VoiceRecognition/
│
└── managers/                 # Manager classes
    ├── CommandManager/
    └── HUDManager/
```

---

## 2. Key Classes with File Paths & Line Numbers

### 2.1 State Management Classes

#### MagicState (StateManagement)
**File:** `AVAUI/StateManagement/StateManagement/src/commonMain/kotlin/com/augmentalis/ideamagic/components/state/MagicState.kt`

| Class | Lines | Purpose |
|-------|-------|---------|
| `MagicState<T>` | 18-53 | Abstract base for reactive state |
| `MutableMagicState<T>` | 64-88 | Mutable state implementation |
| `ImmutableMagicState<T>` | 93-95 | Read-only state wrapper |

**Public Functions:**
```kotlin
// MagicState<T>
fun current(): T                                           // :27
fun <R> map(transform: (T) -> R): StateFlow<R>            // :32-38
fun <T2, R> combine(other: MagicState<T2>, transform: (T, T2) -> R): StateFlow<R>  // :43-52

// MutableMagicState<T>
fun setValue(newValue: T)                                  // :71-73
fun update(transform: (T) -> T)                           // :78-80
fun reset(initialValue: T)                                // :85-87

// Factory functions
fun <T> mutableStateOf(initialValue: T): MutableMagicState<T>  // :100-102
fun <T> stateOf(flow: StateFlow<T>): ImmutableMagicState<T>    // :107-109
fun <T> derivedStateOf(computation: () -> T): StateFlow<T>     // :114-122
```

---

### 2.2 AvaUIRuntime (DSL Runtime)

**File:** `Common/Core/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/magicui/MagicUIRuntime.kt`

| Class | Lines | Purpose |
|-------|-------|---------|
| `AvaUIRuntime` | 101-492 | Main DSL orchestration |
| `RunningApp` | 514-522 | Running app data class |

**Public Functions:**
```kotlin
// AvaUIRuntime
fun loadApp(dslSource: String): VosAstNode.App            // :175-191
suspend fun start(app: VosAstNode.App): RunningApp        // :219-277
suspend fun pause(appId: String)                          // :300-303
suspend fun resume(appId: String)                         // :321-324
suspend fun stop(appId: String)                           // :349-371
suspend fun handleVoiceCommand(appId: String, voiceInput: String): Boolean  // :403-413
fun getApp(appId: String): RunningApp?                    // :433-435
fun getAllApps(): List<RunningApp>                        // :454-456
suspend fun shutdown()                                     // :478-492
```

**Lifecycle States:**
```
CREATED → STARTED → RESUMED ⇄ PAUSED → STOPPED → DESTROYED
```

---

### 2.3 AvaUI DSL (Component DSL)

**File:** `AVAUI/Core/src/commonMain/kotlin/com/augmentalis/avamagic/base/MagicUI.kt`

| Class | Lines | Purpose |
|-------|-------|---------|
| `AvaUI` | 29-48 | DSL entry point |
| `AvaUIScope` | 53-358 | Top-level DSL scope |
| `ColumnScope` | 362-426 | Column layout scope |
| `RowScope` | 428-480 | Row layout scope |
| `ContainerScope` | 482-520 | Container scope |
| `ScrollViewScope` | 522-548 | ScrollView scope |
| `CardScope` | 550-582 | Card component scope |
| `TextScope` | 586-616 | Text component scope |
| `ButtonScope` | 618-648 | Button component scope |
| `ImageScope` | 650-670 | Image component scope |
| `CheckboxScope` | 672-690 | Checkbox scope |
| `TextFieldScope` | 692-724 | TextField scope |
| `SwitchScope` | 726-742 | Switch scope |
| `IconScope` | 744-760 | Icon scope |
| `RadioScope` | 764-784 | Radio group scope |
| `SliderScope` | 786-808 | Slider scope |
| `DropdownScope` | 810-830 | Dropdown scope |
| `DatePickerScope` | 832-852 | Date picker scope |
| `TimePickerScope` | 854-872 | Time picker scope |
| `FileUploadScope` | 874-894 | File upload scope |
| `SearchBarScope` | 896-918 | Search bar scope |
| `RatingScope` | 920-942 | Rating scope |
| `DialogScope` | 946-980 | Dialog scope |
| `ToastScope` | 982-1002 | Toast scope |
| `AlertScope` | 1004-1026 | Alert scope |
| `ProgressBarScope` | 1028-1046 | Progress bar scope |
| `SpinnerScope` | 1048-1062 | Spinner scope |
| `BadgeScope` | 1064-1080 | Badge scope |
| `TooltipScope` | 1082-1126 | Tooltip scope |

**DSL Component Functions (AvaUIScope):**

| Category | Components |
|----------|------------|
| Layout | `Column()`, `Row()`, `Container()`, `ScrollView()` |
| Basic | `Text()`, `Button()`, `Image()`, `Checkbox()`, `TextField()`, `Switch()`, `Card()`, `Icon()` |
| Form | `Radio()`, `Slider()`, `Dropdown()`, `DatePicker()`, `TimePicker()`, `FileUpload()`, `SearchBar()`, `Rating()` |
| Feedback | `Dialog()`, `Toast()`, `Alert()`, `ProgressBar()`, `Spinner()`, `Badge()`, `Tooltip()` |

---

### 2.4 AvaCode CLI (Code Generator)

**File:** `MagicCode/CLI/src/commonMain/kotlin/net/ideahq/ideamagic/codegen/cli/MagicCodeCLI.kt`

| Class | Lines | Purpose |
|-------|-------|---------|
| `AvaCodeCLI` | 12-246 | CLI entry point |

**Public Functions:**
```kotlin
fun execute(args: Array<String>): Int                     // :19-38
```

**CLI Commands:**
| Command | Aliases | Description |
|---------|---------|-------------|
| `generate` | `gen`, `g` | Generate code from DSL |
| `validate` | `val`, `v` | Validate DSL syntax |
| `version` | - | Show version info |
| `help` | `--help`, `-h` | Show help |

**CLI Options:**
| Option | Short | Description |
|--------|-------|-------------|
| `--input` | `-i` | Input DSL file (required) |
| `--output` | `-o` | Output code file |
| `--platform` | `-p` | Target: android/ios/web/desktop |
| `--language` | `-l` | Output: kotlin/swift/typescript/javascript |

**Example Usage:**
```bash
# Generate Android Compose
avacode gen -i screen.json -p android -o Screen.kt

# Generate SwiftUI
avacode gen -i screen.json -p ios -o ScreenView.swift

# Generate React TypeScript
avacode gen -i screen.json -p web -l typescript -o Screen.tsx
```

---

### 2.5 MagicUI Language Server

**File:** `MagicTools/LanguageServer/src/main/kotlin/com/augmentalis/magicui/lsp/`

| File | Class | Purpose |
|------|-------|---------|
| `MagicUILanguageServer.kt` | `MagicUILanguageServer` | LSP server implementation |
| `MagicUITextDocumentService.kt` | `MagicUITextDocumentService` | Document operations |
| `MagicUIWorkspaceService.kt` | `MagicUIWorkspaceService` | Workspace operations |
| `MagicUILanguageServerLauncher.kt` | - | Server launcher |

---

### 2.6 VoiceUI Components

**File:** `apps/VoiceUI/src/main/java/com/augmentalis/voiceui/`

| File | Class | Purpose |
|------|-------|---------|
| `core/MagicEngine.kt` | `MagicEngine` | Core voice engine |
| `core/MagicVUIDIntegration.kt` | `MagicVUIDIntegration` | VUID integration |
| `api/MagicComponents.kt` | `MagicComponents` | Component API |
| `dsl/MagicScreen.kt` | `MagicScreen` | Screen DSL |
| `theme/MagicDreamTheme.kt` | `MagicDreamTheme` | Theme implementation |
| `theme/MagicThemeCustomizer.kt` | `MagicThemeCustomizer` | Theme customization |
| `windows/MagicWindowSystem.kt` | `MagicWindowSystem` | Window management |
| `widgets/MagicButton.kt` | `MagicButton` | Button widget |
| `widgets/MagicCard.kt` | `MagicCard` | Card widget |
| `widgets/MagicFloatingActionButton.kt` | `MagicFloatingActionButton` | FAB widget |
| `widgets/MagicIconButton.kt` | `MagicIconButton` | Icon button |
| `widgets/MagicRow.kt` | `MagicRow` | Row layout |

---

## 3. Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                        Applications                              │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────────┐  │
│  │ VoiceOS  │ │ VoiceUI  │ │VoiceCursor│ │VoiceRecognition   │  │
│  └────┬─────┘ └────┬─────┘ └────┬─────┘ └─────────┬──────────┘  │
└───────┼────────────┼────────────┼─────────────────┼─────────────┘
        │            │            │                 │
┌───────▼────────────▼────────────▼─────────────────▼─────────────┐
│                     AvaUIRuntime (DSL Runtime)                   │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ loadApp() → start() → handleVoiceCommand() → pause/stop()   ││
│  └─────────────────────────────────────────────────────────────┘│
└───────────────────────────┬─────────────────────────────────────┘
                            │
    ┌───────────────────────┼───────────────────────┐
    │                       │                       │
┌───▼───────────────┐ ┌─────▼───────────────┐ ┌─────▼───────────────┐
│   AvaUI DSL       │ │   MagicState        │ │ VoiceCommandRouter  │
│  ┌─────────────┐  │ │  ┌───────────────┐  │ │  ┌───────────────┐  │
│  │ Component   │  │ │  │MutableMagic   │  │ │  │ register()    │  │
│  │ Scopes      │  │ │  │State<T>       │  │ │  │ match()       │  │
│  │ (30+ types) │  │ │  │               │  │ │  │ unregister()  │  │
│  └─────────────┘  │ │  │ setValue()    │  │ │  └───────────────┘  │
│                   │ │  │ update()      │  │ │                     │
│  Layout:          │ │  │ map()         │  │ │  Fuzzy matching     │
│  - Column         │ │  │ combine()     │  │ │  (threshold: 0.7)   │
│  - Row            │ │  └───────────────┘  │ │                     │
│  - Container      │ │                     │ │                     │
│  - ScrollView     │ │  StateFlow-based    │ │                     │
│                   │ │  reactive updates   │ │                     │
│  Basic:           │ │                     │ │                     │
│  - Text, Button   │ │                     │ │                     │
│  - Image, Card    │ │                     │ │                     │
│  - TextField      │ │                     │ │                     │
│                   │ │                     │ │                     │
│  Form:            │ │                     │ │                     │
│  - Radio, Slider  │ │                     │ │                     │
│  - Dropdown       │ │                     │ │                     │
│  - DatePicker     │ │                     │ │                     │
│                   │ │                     │ │                     │
│  Feedback:        │ │                     │ │                     │
│  - Dialog, Toast  │ │                     │ │                     │
│  - Alert, Spinner │ │                     │ │                     │
└───────────────────┘ └─────────────────────┘ └─────────────────────┘
        │                       │                       │
        └───────────────────────┼───────────────────────┘
                                │
┌───────────────────────────────▼─────────────────────────────────┐
│                    Platform Renderers                            │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐ │
│  │  Android   │  │    iOS     │  │   Desktop  │  │    Web     │ │
│  │  (Compose) │  │  (SwiftUI) │  │  (Compose) │  │   (React)  │ │
│  └────────────┘  └────────────┘  └────────────┘  └────────────┘ │
└─────────────────────────────────────────────────────────────────┘
```

---

## 4. Key Data Flows

### 4.1 DSL App Lifecycle Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  .vos DSL    │────▶│   Tokenizer  │────▶│   Parser     │
│  Source      │     │              │     │              │
└──────────────┘     └──────────────┘     └──────────────┘
                                                 │
                                                 ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Running     │◀────│  Component   │◀────│   AST        │
│  App         │     │  Instantiator│     │   (App Node) │
└──────────────┘     └──────────────┘     └──────────────┘
        │
        ▼
┌──────────────────────────────────────────────────────┐
│                  Lifecycle States                     │
│  CREATED ─▶ STARTED ─▶ RESUMED ◀─▶ PAUSED ─▶ STOPPED │
└──────────────────────────────────────────────────────┘
```

### 4.2 Voice Command Processing Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  Voice       │────▶│  Normalize   │────▶│  Fuzzy       │
│  Input       │     │  (lowercase) │     │  Match       │
└──────────────┘     └──────────────┘     └──────────────┘
                                                 │
                            ┌────────────────────┴───────┐
                            │                            │
                    Match found?                 No match
                            │                            │
                            ▼                            ▼
                    ┌──────────────┐             ┌──────────────┐
                    │  Action      │             │  Return      │
                    │  Dispatcher  │             │  false       │
                    └──────────────┘             └──────────────┘
                            │
                            ▼
                    ┌──────────────┐
                    │  Event Bus   │
                    │  (Callbacks) │
                    └──────────────┘
```

### 4.3 State Change Flow

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  User        │────▶│  MutableMagic│────▶│  StateFlow   │
│  Action      │     │  State.set() │     │  .emit()     │
└──────────────┘     └──────────────┘     └──────────────┘
                                                 │
                                                 ▼
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  UI          │◀────│  Component   │◀────│  Flow        │
│  Updates     │     │  Recompose   │     │  .collect()  │
└──────────────┘     └──────────────┘     └──────────────┘
```

---

## 5. Dependencies and Interfaces

### 5.1 Module Dependencies (93 build.gradle.kts files)

| Module Group | Count | Key Modules |
|--------------|-------|-------------|
| **AVAUI** | 29 | Core, StateManagement, Renderers, Theme |
| **MagicUI** | 20 | Components, DesignSystem, Foundation |
| **MagicCode** | 4 | CLI, Forms, Templates, Workflows |
| **Core** | 12 | constants, database, hash, validation |
| **Libraries** | 14 | SpeechRecognition, VivokaSDK, PluginSystem |
| **Apps** | 6 | VoiceOS, VoiceUI, VoiceCursor |
| **Managers** | 2 | CommandManager, HUDManager |

### 5.2 Core Dependencies

```kotlin
// KMP Dependencies
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json")

// Platform-specific
android {
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}

ios {
    // SwiftUI bridging via KMP
}

desktop {
    implementation("org.jetbrains.compose.desktop:desktop")
}
```

### 5.3 Key Interfaces

```kotlin
// State Management
interface MagicState<T> {
    val value: StateFlow<T>
    fun current(): T
    fun <R> map(transform: (T) -> R): StateFlow<R>
}

// Component Rendering
interface Renderer {
    fun applyTheme(theme: Theme)
    fun render(component: Component): Any
}

// Code Generation
interface CodeGenerator {
    fun generate(screen: Screen): GeneratedCode
}

// Voice Routing
interface VoiceCommandRouter {
    fun register(trigger: String, action: String, appId: String)
    fun match(voiceInput: String): MatchResult?
    fun clear()
}

// Lifecycle
interface AppLifecycle {
    val state: StateFlow<LifecycleState>
    fun create()
    fun start()
    fun resume()
    fun pause()
    fun stop()
    fun destroy()
}
```

---

## 6. Platform Support Matrix

| Platform | Renderer | Language | UI Framework |
|----------|----------|----------|--------------|
| Android | Android Renderer | Kotlin | Jetpack Compose |
| iOS | iOS Renderer | Swift (via KMP) | SwiftUI |
| Desktop | Desktop Renderer | Kotlin | Compose Desktop |
| Web | Web Renderer | TypeScript | React |

---

## 7. Summary Statistics

| Metric | Count |
|--------|-------|
| Total Kotlin files in AVAMagic | ~2,000+ |
| Build modules | 93 |
| Component types (DSL) | 30+ |
| State management classes | 3 |
| Lifecycle states | 6 |
| CLI commands | 4 |
| Supported platforms | 4 |
| Output languages | 4 |

---

## 8. Related Modules (Common)

| Path | Purpose |
|------|---------|
| `Common/Core/AvaUI/` | AvaUIRuntime |
| `Common/AvaElements/Core/` | Core elements |
| `Common/AvaElements/StateManagement/` | State management |
| `Common/AvaElements/components/` | Unified components |

---

*Generated by Claude Analysis on 2026-01-11*
