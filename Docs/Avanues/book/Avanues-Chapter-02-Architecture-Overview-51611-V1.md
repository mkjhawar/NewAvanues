# The IDEAMagic Developer Manual

## Chapter 2: Architecture Overview

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net

---

## Table of Contents

- [2.1 System Architecture](#21-system-architecture)
- [2.2 Architectural Layers](#22-architectural-layers)
- [2.3 Module Organization](#23-module-organization)
- [2.4 Data Flow](#24-data-flow)
- [2.5 Component Lifecycle](#25-component-lifecycle)
- [2.6 Platform Abstraction](#26-platform-abstraction)
- [2.7 Cross-Platform Communication](#27-cross-platform-communication)
- [2.8 Dependency Graph](#28-dependency-graph)
- [2.9 Architectural Patterns](#29-architectural-patterns)
- [2.10 Chapter Summary](#210-chapter-summary)

---

## 2.1 System Architecture

### High-Level Overview

The IDEAMagic framework follows a **layered architecture** with clear separation between user input, parsing, code generation, and platform-specific rendering:

```
┌─────────────────────────────────────────────────────────────────┐
│                        LAYER 1: INPUT                            │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐        │
│  │   CLI    │  │  Manual  │  │   Web    │  │   IDE    │        │
│  │   Tool   │  │   JSON   │  │  Editor  │  │  Plugin  │        │
│  │ (Works)  │  │  Editor  │  │(Planned) │  │(Planned) │        │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘        │
└───────┼─────────────┼─────────────┼─────────────┼──────────────┘
        │             │             │             │
        └─────────────┴─────────────┴─────────────┘
                      │
                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                    LAYER 2: PARSING                              │
│  ┌───────────────────────────────────────────────────────┐      │
│  │           JsonDSLParser (kotlinx.serialization)       │      │
│  │  - Parses JSON DSL to data classes                    │      │
│  │  - Validates structure and types                      │      │
│  │  - Handles errors gracefully                          │      │
│  └─────────────────────┬─────────────────────────────────┘      │
└────────────────────────┼────────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────────┐
│                      LAYER 3: AST                                │
│  ┌───────────────────────────────────────────────────────┐      │
│  │                 AvaUINode AST                       │      │
│  │  ScreenNode                                           │      │
│  │  ├─ name: String                                      │      │
│  │  ├─ root: ComponentNode                               │      │
│  │  ├─ stateVariables: List<StateVariable>              │      │
│  │  └─ imports: List<String>                             │      │
│  │                                                        │      │
│  │  ComponentNode                                         │      │
│  │  ├─ id: String                                         │      │
│  │  ├─ type: ComponentType (48 types)                    │      │
│  │  ├─ properties: Map<String, Any>                      │      │
│  │  ├─ children: List<ComponentNode>                     │      │
│  │  └─ eventHandlers: Map<String, String>                │      │
│  └─────────────────────┬─────────────────────────────────┘      │
└────────────────────────┼────────────────────────────────────────┘
                         │
        ┌────────────────┼────────────────┐
        │                │                │
        ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   LAYER 4:   │ │   LAYER 4:   │ │   LAYER 4:   │
│  GENERATOR   │ │  GENERATOR   │ │  GENERATOR   │
│   Android    │ │     iOS      │ │     Web      │
│   Kotlin     │ │    Swift     │ │  TypeScript  │
│   Compose    │ │   SwiftUI    │ │    React     │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  Generated   │ │  Generated   │ │  Generated   │
│   .kt file   │ │ .swift file  │ │  .tsx file   │
└──────┬───────┘ └──────┬───────┘ └──────┬───────┘
       │                │                │
       ▼                ▼                ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│   LAYER 5:   │ │   LAYER 5:   │ │   LAYER 5:   │
│   RUNTIME    │ │   RUNTIME    │ │   RUNTIME    │
│   Android    │ │     iOS      │ │     Web      │
│   Compose    │ │   SwiftUI    │ │    React     │
│   Renderer   │ │  C-Interop   │ │  Component   │
│   (✅Works)   │ │ (⚠️TODOs)    │ │   Loader     │
│              │ │              │ │   (✅Works)   │
└──────────────┘ └──────────────┘ └──────────────┘
```

### Key Characteristics

1. **Unidirectional Data Flow:** JSON → Parser → AST → Generator → Native Code → Runtime
2. **Platform Independence:** Layers 1-3 are platform-agnostic (Kotlin Multiplatform)
3. **Platform Specialization:** Layer 4-5 are platform-specific
4. **Clean Separation:** Each layer has clear responsibilities and interfaces

---

## 2.2 Architectural Layers

### Layer 1: Input Layer

**Purpose:** Accept UI definitions from various sources

**Components:**

1. **CLI Tool** (`AvaCodeCLIImpl.kt`)
   - Command-line interface
   - File I/O operations
   - Validation and code generation
   - **Status:** ✅ Complete

2. **Manual JSON Editing**
   - Developers write JSON in text editor
   - IDE support via JSON Schema (optional)
   - **Status:** ✅ Works

3. **Web Editor** (Planned)
   - Visual drag-and-drop builder
   - Live preview
   - Property editor
   - **Status:** 🔴 Not implemented (see Chapter 13)

4. **IDE Plugins** (Planned)
   - Android Studio plugin
   - Xcode extension
   - VS Code extension
   - **Status:** 🔴 Not implemented

**Interface:**

```kotlin
// All input sources produce JSON string
val jsonDsl: String = """
{
  "name": "LoginScreen",
  "root": { ... }
}
"""
```

---

### Layer 2: Parsing Layer

**Purpose:** Convert JSON DSL to type-safe data structures

**Component:** `JsonDSLParser` (`Universal/IDEAMagic/CodeGen/Parser/`)

**Technology:** kotlinx.serialization

**Key Classes:**

```kotlin
// Data models for deserialization
@Serializable
data class ScreenDefinition(
    val name: String,
    val imports: List<String> = emptyList(),
    val state: List<StateVariableDefinition> = emptyList(),
    val root: ComponentDefinition
)

@Serializable
data class ComponentDefinition(
    val id: String? = null,
    val type: String,
    val properties: Map<String, JsonElement> = emptyMap(),
    val children: List<ComponentDefinition> = emptyList(),
    val events: Map<String, String> = emptyMap()
)

// Parser class
class JsonDSLParser {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parseScreen(jsonString: String): Result<ScreenNode> {
        return try {
            val definition = json.decodeFromString<ScreenDefinition>(jsonString)
            Result.success(buildScreenNode(definition))
        } catch (e: SerializationException) {
            Result.failure(ParseException("Parsing error: ${e.message}", e))
        }
    }

    fun validate(jsonString: String): ValidationResult {
        // Validates JSON structure without full parsing
        // Returns errors and warnings
    }
}
```

**Responsibilities:**

1. **Deserialize JSON** to `ScreenDefinition`, `ComponentDefinition`, etc.
2. **Validate structure** (required fields, type checking)
3. **Build AST** from definitions
4. **Handle errors** gracefully with clear messages

**Error Handling:**

```kotlin
val result = parser.parseScreen(jsonString)

when {
    result.isSuccess -> {
        val screen = result.getOrThrow()
        // Proceed to code generation
    }
    result.isFailure -> {
        val error = result.exceptionOrNull()
        println("Parse error: ${error?.message}")
        // Show error to user
    }
}
```

---

### Layer 3: Abstract Syntax Tree (AST)

**Purpose:** Platform-agnostic representation of UI

**Location:** `Universal/IDEAMagic/CodeGen/AST/`

**Key Data Structures:**

```kotlin
// Root node representing entire screen
data class ScreenNode(
    override val id: String = generateId(),
    val name: String,
    val root: ComponentNode,
    val stateVariables: List<StateVariable> = emptyList(),
    val imports: List<String> = emptyList()
) : AvaUINode()

// Component node (recursive tree structure)
data class ComponentNode(
    override val id: String,
    val type: ComponentType,
    val properties: Map<String, Any> = emptyMap(),
    val children: List<ComponentNode> = emptyList(),
    val eventHandlers: Map<String, String> = emptyMap()
) : AvaUINode()

// State variable
data class StateVariable(
    val name: String,
    val type: String,
    val initialValue: PropertyValue? = null,
    val mutable: Boolean = true
)

// Component types (48 total)
enum class ComponentType {
    // Foundation (9)
    BUTTON, CARD, CHECKBOX, CHIP, DIVIDER, IMAGE,
    LIST_ITEM, TEXT, TEXT_FIELD,

    // Core (2)
    COLOR_PICKER, ICON_PICKER,

    // Basic (6)
    ICON, LABEL, CONTAINER, ROW, COLUMN, SPACER,

    // Advanced (18)
    SWITCH, SLIDER, PROGRESS_BAR, SPINNER, ALERT,
    DIALOG, TOAST, TOOLTIP, RADIO, DROPDOWN,
    DATE_PICKER, TIME_PICKER, SEARCH_BAR, RATING,
    BADGE, FILE_UPLOAD, APP_BAR, BOTTOM_NAV,

    // Navigation (5)
    DRAWER, PAGINATION, TABS, BREADCRUMB, ACCORDION,

    // Custom
    CUSTOM
}

// Property values (type-safe)
sealed class PropertyValue {
    data class StringValue(val value: String) : PropertyValue()
    data class IntValue(val value: Int) : PropertyValue()
    data class DoubleValue(val value: Double) : PropertyValue()
    data class BoolValue(val value: Boolean) : PropertyValue()
    data class EnumValue(val value: String) : PropertyValue()
    data class ListValue(val items: List<PropertyValue>) : PropertyValue()
    data class MapValue(val items: Map<String, PropertyValue>) : PropertyValue()
    data class ReferenceValue(val ref: String) : PropertyValue()
}
```

**Why AST?**

1. **Platform-Agnostic:** Same AST used for all platforms
2. **Type-Safe:** Compile-time checking, not runtime strings
3. **Immutable:** Easy to reason about, thread-safe
4. **Transformable:** Can optimize, validate, or modify before generation
5. **Serializable:** Can save/load, transmit over network

**Tree Structure Example:**

```kotlin
ScreenNode(
    name = "LoginScreen",
    root = ComponentNode(
        id = "root",
        type = ComponentType.CARD,
        children = listOf(
            ComponentNode(
                id = "column",
                type = ComponentType.COLUMN,
                children = listOf(
                    ComponentNode(
                        id = "title",
                        type = ComponentType.TEXT,
                        properties = mapOf("text" to "Welcome Back")
                    ),
                    ComponentNode(
                        id = "emailField",
                        type = ComponentType.TEXT_FIELD,
                        properties = mapOf(
                            "label" to "Email",
                            "inputType" to "email"
                        ),
                        eventHandlers = mapOf("onValueChange" to "{ email = it }")
                    ),
                    ComponentNode(
                        id = "loginButton",
                        type = ComponentType.BUTTON,
                        properties = mapOf("text" to "Sign In"),
                        eventHandlers = mapOf("onClick" to "handleLogin")
                    )
                )
            )
        )
    ),
    stateVariables = listOf(
        StateVariable("email", "String", PropertyValue.StringValue(""), mutable = true),
        StateVariable("password", "String", PropertyValue.StringValue(""), mutable = true)
    )
)
```

---

### Layer 4: Code Generation Layer

**Purpose:** Transform AST to platform-specific native code

**Generators:**

1. **KotlinComposeGenerator** → Android (Jetpack Compose)
2. **SwiftUIGenerator** → iOS (SwiftUI)
3. **ReactTypeScriptGenerator** → Web (React + TypeScript)

**Location:** `Universal/IDEAMagic/CodeGen/Generators/`

**Interface:**

```kotlin
interface CodeGenerator {
    /**
     * Generate native code from screen AST
     * @param screen Screen definition
     * @return Generated code with metadata
     */
    fun generate(screen: ScreenNode): GeneratedCode
}

data class GeneratedCode(
    val code: String,               // Generated source code
    val language: Language,         // KOTLIN, SWIFT, TYPESCRIPT
    val platform: Platform,         // ANDROID, IOS, WEB
    val dependencies: List<String>  // Required libraries
)

enum class Platform {
    ANDROID, IOS, WEB, DESKTOP
}

enum class Language {
    KOTLIN, SWIFT, TYPESCRIPT, JAVASCRIPT
}
```

**Example: KotlinComposeGenerator**

```kotlin
class KotlinComposeGenerator : CodeGenerator {

    override fun generate(screen: ScreenNode): GeneratedCode {
        val code = buildString {
            // Package declaration
            appendLine("package com.augmentalis.avaui.generated")
            appendLine()

            // Imports
            appendLine("import androidx.compose.runtime.*")
            appendLine("import androidx.compose.material3.*")
            appendLine("import androidx.compose.foundation.layout.*")
            appendLine("import androidx.compose.ui.Modifier")
            appendLine("import androidx.compose.ui.unit.dp")
            screen.imports.forEach { imp ->
                appendLine("import $imp")
            }
            appendLine()

            // Screen composable
            appendLine("@Composable")
            appendLine("fun ${screen.name}() {")

            // State variables
            screen.stateVariables.forEach { stateVar ->
                if (stateVar.mutable) {
                    appendLine("    var ${stateVar.name} by remember { mutableStateOf(${stateVar.initialValue?.toKotlin()}) }")
                } else {
                    appendLine("    val ${stateVar.name} = ${stateVar.initialValue?.toKotlin()}")
                }
            }

            if (screen.stateVariables.isNotEmpty()) {
                appendLine()
            }

            // Root component
            append(generateComponent(screen.root, indent = 1))

            appendLine("}")
        }

        return GeneratedCode(
            code = code,
            language = Language.KOTLIN,
            platform = Platform.ANDROID,
            dependencies = listOf(
                "androidx.compose.ui:ui",
                "androidx.compose.material3:material3",
                "androidx.compose.ui:ui-tooling-preview"
            )
        )
    }

    private fun generateComponent(component: ComponentNode, indent: Int): String {
        return when (component.type) {
            ComponentType.BUTTON -> generateButton(component, indent)
            ComponentType.TEXT -> generateText(component, indent)
            ComponentType.COLUMN -> generateColumn(component, indent)
            // ... 45 more component types
            else -> "// Unsupported component: ${component.type}"
        }
    }

    private fun generateButton(component: ComponentNode, indent: Int): String {
        val indentStr = "    ".repeat(indent)
        val text = component.properties["text"] as? String ?: ""
        val onClick = component.eventHandlers["onClick"] ?: "{ }"

        return buildString {
            appendLine("${indentStr}Button(")
            appendLine("$indentStr    onClick = $onClick")
            appendLine("$indentStr) {")
            appendLine("$indentStr    Text(\"$text\")")
            appendLine("$indentStr}")
        }
    }
}
```

**Output Example:**

```kotlin
// Generated by KotlinComposeGenerator
package com.augmentalis.avaui.generated

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Card {
        Column {
            Text("Welcome Back")
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") }
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation()
            )
            Button(onClick = handleLogin) {
                Text("Sign In")
            }
        }
    }
}
```

---

### Layer 5: Runtime Layer

**Purpose:** Execute generated code and render UI on platform

**Components:**

#### Android Runtime

**Location:** `Universal/IDEAMagic/Components/Adapters/src/androidMain/`

**Key File:** `ComposeUIImplementation.kt`

**Implementation:**

```kotlin
@Composable
fun MagicButtonCompose(
    text: String,
    onClick: () -> Unit,
    variant: ButtonVariant = ButtonVariant.PRIMARY,
    size: ButtonSize = ButtonSize.MEDIUM,
    enabled: Boolean = true,
    fullWidth: Boolean = false,
    icon: String? = null,
    iconPosition: IconPosition = IconPosition.START,
    modifier: Modifier = Modifier
) {
    val buttonModifier = if (fullWidth) {
        modifier.fillMaxWidth()
    } else {
        modifier
    }

    when (variant) {
        ButtonVariant.PRIMARY -> {
            Button(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier
            ) {
                ButtonContent(text, icon, iconPosition)
            }
        }
        ButtonVariant.SECONDARY -> {
            OutlinedButton(
                onClick = onClick,
                enabled = enabled,
                modifier = buttonModifier
            ) {
                ButtonContent(text, icon, iconPosition)
            }
        }
        // ... other variants
    }
}
```

**Status:** ✅ Complete (11 Foundation/Core components)

#### iOS Runtime

**Location:** `Universal/IDEAMagic/Components/Adapters/src/iosMain/`

**Key Files:**
- `SwiftUIInterop.kt` (Kotlin/Native bridge)
- `swift/AvaUI/*.swift` (35 SwiftUI views)

**Implementation:**

```kotlin
// Kotlin bridge
@OptIn(ExperimentalForeignApi::class)
class AvaUIHostingController(private val viewData: Map<String, Any?>) {
    fun createViewController(): UIViewController {
        val dict = viewData.toNSDictionary()
        return createSwiftUIHostingController(dict)
    }

    private external fun createSwiftUIHostingController(data: NSDictionary): UIViewController
}
```

```swift
// SwiftUI view
struct MagicButtonView: View {
    let id: String
    let text: String
    let variant: ButtonVariant
    let onClick: () -> Void

    var body: some View {
        Button(action: onClick) {
            Text(text)
        }
        .buttonStyle(variantStyle)
    }

    private var variantStyle: some PrimitiveButtonStyle {
        switch variant {
        case .primary: return FilledButtonStyle()
        case .secondary: return OutlinedButtonStyle()
        // ...
        }
    }
}
```

**Status:** ⚠️ Partial (35 SwiftUI views exist, but 27 Kotlin bridge TODOs)

#### Web Runtime

**Location:** `Universal/IDEAMagic/Components/Adapters/src/jsMain/`

**Key Files:**
- `ReactComponentLoader.kt` (Dynamic loading)
- `typescript/components/*.tsx` (35 React components)

**Implementation:**

```kotlin
// Dynamic component loader
@JsExport
object ReactComponentLoader {
    fun loadComponent(componentName: String): Promise<dynamic> {
        // Check cache
        if (componentCache.containsKey(componentName)) {
            return Promise.resolve(componentCache[componentName])
        }

        // Dynamic import
        return js("import(componentPath)").then { module ->
            val component = module.default ?: module[componentName]
            componentCache[componentName] = component
            component
        }
    }

    fun renderComponent(
        componentName: String,
        props: dynamic,
        containerId: String
    ): Promise<Unit> {
        return loadComponent(componentName).then { component ->
            val container = document.getElementById(containerId)
            val React = js("require('react')")
            val ReactDOM = js("require('react-dom/client')")

            val root = ReactDOM.createRoot(container)
            root.render(React.createElement(component, props))
        }
    }
}
```

```typescript
// React component
export const MagicButton: React.FC<MagicButtonProps> = ({
  text,
  onClick,
  variant = 'primary',
  size = 'medium',
  enabled = true,
  fullWidth = false,
  icon,
  iconPosition = 'start'
}) => {
  return (
    <Button
      variant={variant === 'danger' ? 'contained' : variant}
      size={size}
      disabled={!enabled}
      fullWidth={fullWidth}
      onClick={onClick}
      startIcon={icon && iconPosition === 'start' ? icon : undefined}
      endIcon={icon && iconPosition === 'end' ? icon : undefined}
    >
      {text}
    </Button>
  );
};
```

**Status:** ✅ Complete (35 React components + loader)

---

## 2.3 Module Organization

### Directory Structure

```
Universal/IDEAMagic/
├── CodeGen/                    # Code generation pipeline
│   ├── AST/                   # Abstract Syntax Tree
│   │   └── src/commonMain/kotlin/
│   │       └── AvaUINode.kt
│   ├── CLI/                   # Command-line interface
│   │   ├── src/commonMain/kotlin/
│   │   │   ├── AvaCodeCLIImpl.kt
│   │   │   └── FileIO.kt
│   │   └── src/jvmMain/kotlin/
│   │       └── FileIO.jvm.kt
│   ├── Generators/            # Platform code generators
│   │   ├── Kotlin/            # Android generator
│   │   ├── Swift/             # iOS generator
│   │   └── React/             # Web generator
│   └── Parser/                # JSON DSL parser
│       └── src/commonMain/kotlin/
│           └── JsonDSLParser.kt
│
├── Components/                 # UI Component library
│   ├── Adapters/              # Platform bridges
│   │   ├── src/androidMain/kotlin/
│   │   │   ├── ComposeUIImplementation.kt
│   │   │   └── ComposeRenderer.kt
│   │   ├── src/iosMain/kotlin/
│   │   │   ├── SwiftUIInterop.kt
│   │   │   └── iOSRenderer.kt
│   │   ├── src/iosMain/swift/AvaUI/
│   │   │   ├── MagicButtonView.swift
│   │   │   ├── MagicCardView.swift
│   │   │   └── ... (35 SwiftUI views)
│   │   ├── src/jsMain/kotlin/
│   │   │   └── ReactComponentLoader.kt
│   │   └── src/jsMain/typescript/components/
│   │       ├── foundation/
│   │       ├── core/
│   │       ├── basic/
│   │       └── advanced/
│   ├── Foundation/            # 9 Foundation components (Kotlin models)
│   │   └── src/commonMain/kotlin/
│   │       ├── MagicButton.kt
│   │       ├── MagicCard.kt
│   │       └── ...
│   ├── Core/                  # 2 Core components
│   │   └── src/commonMain/kotlin/
│   │       ├── MagicColorPicker.kt
│   │       └── MagicIconPicker.kt
│   ├── AssetManager/          # Icon/image/font management
│   ├── StateManagement/       # State handling
│   ├── TemplateLibrary/       # Pre-built templates
│   ├── ThemeBuilder/          # Theme creation UI
│   └── Renderers/             # Platform renderers
│
├── AvaUI/                    # AvaUI Runtime System
│   ├── CoreTypes/             # Base types & interfaces
│   ├── DesignSystem/          # Design tokens & theming
│   │   ├── src/commonMain/kotlin/
│   │   │   ├── DesignTokens.kt
│   │   │   └── MagicTheme.kt
│   │   ├── src/androidMain/kotlin/
│   │   │   └── MagicTheme.android.kt
│   │   └── src/iosMain/kotlin/
│   │       └── MagicTheme.ios.kt
│   ├── StateManagement/       # State management
│   ├── ThemeBridge/           # Theme conversion
│   ├── ThemeManager/          # Theme persistence & sync
│   │   └── src/commonMain/kotlin/
│   │       ├── ThemeManager.kt
│   │       ├── ThemeRepository.kt
│   │       └── ThemeSync.kt
│   ├── UIConvertor/           # Cross-platform conversion
│   └── src/commonMain/kotlin/ # AvaUI runtime core
│       ├── AvaUIRuntime.kt
│       ├── core/
│       ├── instantiation/
│       ├── lifecycle/
│       ├── layout/
│       └── voice/
│
├── AvaCode/                  # DSL & Code Generation (older implementation)
│   ├── src/commonMain/kotlin/
│   │   └── generators/
│   │       ├── kotlin/
│   │       ├── swift/
│   │       └── react/
│   └── docs/
│
├── Database/                   # Data persistence layer
│   └── src/commonMain/kotlin/
│       ├── Database.kt
│       ├── Collection.kt
│       └── Document.kt
│
├── Libraries/                  # Shared libraries
│   └── Preferences/           # Settings storage
│
├── VoiceOSBridge/             # VoiceOS integration
│   └── build.gradle.kts       # ⚠️ EMPTY (only build file)
│
└── Examples/                   # Example screens & themes
    ├── components/
    ├── screens/
    │   ├── LoginScreen.json
    │   ├── ProfileScreen.json
    │   └── ProductCard.json
    └── themes/
        └── DarkTheme.json
```

### Module Dependencies

```
┌─────────────────────────────────────────────────────────┐
│                     Dependency Graph                     │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  CLI ────────────► Parser ────────► AST                 │
│   │                                   │                  │
│   └──────────────────────────────────┤                  │
│                                       │                  │
│                                       ▼                  │
│                                  Generators              │
│                            ┌──────────┼──────────┐       │
│                            │          │          │       │
│                            ▼          ▼          ▼       │
│                        Kotlin     Swift      React       │
│                                                           │
│  Components/Foundation ───────────┐                      │
│  Components/Core                  │                      │
│                                    ▼                      │
│                          Components/Adapters             │
│                            ┌──────────┼──────────┐       │
│                            │          │          │       │
│                            ▼          ▼          ▼       │
│                        Android     iOS        Web        │
│                                                           │
│  AvaUI/DesignSystem ──────► AvaUI/ThemeManager      │
│                                                           │
│  VoiceOSBridge (EMPTY)                                   │
│                                                           │
└─────────────────────────────────────────────────────────┘
```

---

## 2.4 Data Flow

### Complete Data Flow Example

Let's trace a complete flow from JSON input to rendered UI:

**Step 1: User Creates JSON**

```json
{
  "name": "HelloScreen",
  "root": {
    "type": "Column",
    "children": [
      {
        "type": "Text",
        "properties": { "text": "Hello, World!" }
      },
      {
        "type": "Button",
        "properties": { "text": "Click Me" },
        "events": { "onClick": "handleClick" }
      }
    ]
  }
}
```

**Step 2: CLI Reads File**

```kotlin
// AvaCodeCLIImpl.kt
val dslContent = FileIO.readFile("HelloScreen.json")
```

**Step 3: Parser Deserializes JSON**

```kotlin
// JsonDSLParser.kt
val screenResult = parser.parseScreen(dslContent)
val screenDef = json.decodeFromString<ScreenDefinition>(dslContent)

// ScreenDefinition {
//   name = "HelloScreen",
//   root = ComponentDefinition {
//     type = "Column",
//     children = [
//       ComponentDefinition { type = "Text", ... },
//       ComponentDefinition { type = "Button", ... }
//     ]
//   }
// }
```

**Step 4: AST Built**

```kotlin
val screen = buildScreenNode(screenDef)

// ScreenNode(
//   name = "HelloScreen",
//   root = ComponentNode(
//     type = ComponentType.COLUMN,
//     children = [
//       ComponentNode(type = ComponentType.TEXT, ...),
//       ComponentNode(type = ComponentType.BUTTON, ...)
//     ]
//   )
// )
```

**Step 5: Code Generated (Android)**

```kotlin
// KotlinComposeGenerator.kt
val generator = KotlinComposeGenerator()
val generatedCode = generator.generate(screen)

// GeneratedCode(
//   code = """
//     @Composable
//     fun HelloScreen() {
//       Column {
//         Text("Hello, World!")
//         Button(onClick = handleClick) {
//           Text("Click Me")
//         }
//       }
//     }
//   """,
//   language = Language.KOTLIN,
//   platform = Platform.ANDROID
// )
```

**Step 6: Code Written to File**

```kotlin
FileIO.writeFile("HelloScreen.kt", generatedCode.code)
```

**Step 7: Developer Compiles & Runs**

```kotlin
// In Android app
@Composable
fun App() {
    HelloScreen()  // Generated composable
}
```

**Step 8: Runtime Renders UI**

```
┌─────────────────────────┐
│    Hello, World!        │
│                         │
│    ┌──────────┐         │
│    │ Click Me │         │
│    └──────────┘         │
└─────────────────────────┘
```

### Alternative: Runtime Interpretation

Instead of generating code, AvaUI can **interpret DSL at runtime**:

```kotlin
// Load DSL at runtime
val screen = parser.parseScreen(FileIO.readFile("HelloScreen.json")).getOrThrow()

// Render directly
AvaUIRuntime.render(screen)
```

**Advantages:**
- No code generation step
- Hot reload without recompiling
- Dynamic UI updates

**Disadvantages:**
- Runtime overhead (parsing, reflection)
- No compile-time type checking
- Harder to debug

**Current Status:** Code generation preferred (App Store compliance).

---

## 2.5 Component Lifecycle

### Component States

A component goes through several lifecycle states:

```
DEFINED → PARSED → VALIDATED → GENERATED → COMPILED → RENDERED
```

**1. DEFINED (JSON DSL)**

```json
{
  "type": "Button",
  "properties": { "text": "Click Me" }
}
```

**2. PARSED (Data Class)**

```kotlin
ComponentDefinition(
    type = "Button",
    properties = mapOf("text" to JsonPrimitive("Click Me"))
)
```

**3. VALIDATED**

```kotlin
// Check required properties
if (!properties.containsKey("text")) {
    throw ValidationException("Button requires 'text' property")
}

// Check type
if (properties["text"] !is JsonPrimitive) {
    throw ValidationException("'text' must be a string")
}
```

**4. GENERATED (Native Code)**

```kotlin
Button(onClick = { }) {
    Text("Click Me")
}
```

**5. COMPILED**

- Kotlin compiler generates bytecode (.class files)
- Swift compiler generates machine code
- TypeScript compiler generates JavaScript

**6. RENDERED (UI)**

```
┌──────────┐
│ Click Me │
└──────────┘
```

### State Management Lifecycle

For **stateful components** (e.g., TextField with value), state goes through:

```
INITIAL → CHANGED → UPDATED → RE-RENDERED
```

**Example:**

```kotlin
// INITIAL
var email by remember { mutableStateOf("") }

// User types "h"
// CHANGED (user input event)
onValueChange = { email = it }

// UPDATED (state variable changed)
email = "h"

// RE-RENDERED (Compose recomposes)
OutlinedTextField(value = "h", ...)
```

### Mounting & Unmounting

**Component Mounting:**

```
JSON → Parse → AST → Generate → Compile → Mount → Render
```

**Component Unmounting:**

```
User navigates away → Unmount → Cleanup → GC
```

**Android (Compose):**

```kotlin
@Composable
fun MyScreen() {
    DisposableEffect(Unit) {
        // On mount
        println("Component mounted")

        onDispose {
            // On unmount
            println("Component unmounted")
        }
    }
}
```

**iOS (SwiftUI):**

```swift
struct MyScreen: View {
    var body: some View {
        Text("Hello")
            .onAppear {
                print("Component mounted")
            }
            .onDisappear {
                print("Component unmounted")
            }
    }
}
```

**Web (React):**

```typescript
useEffect(() => {
    // On mount
    console.log('Component mounted');

    return () => {
        // On unmount
        console.log('Component unmounted');
    };
}, []);
```

---

## 2.6 Platform Abstraction

### Expect/Actual Mechanism

Kotlin Multiplatform uses **expect/actual** for platform-specific implementations:

**Common (expect):**

```kotlin
// Universal/IDEAMagic/CodeGen/CLI/src/commonMain/kotlin/FileIO.kt
expect object FileIO {
    fun readFile(path: String): String
    fun writeFile(path: String, content: String)
    fun fileExists(path: String): Boolean
}
```

**Android/JVM (actual):**

```kotlin
// Universal/IDEAMagic/CodeGen/CLI/src/jvmMain/kotlin/FileIO.jvm.kt
actual object FileIO {
    actual fun readFile(path: String): String {
        return File(path).readText()
    }

    actual fun writeFile(path: String, content: String) {
        File(path).writeText(content)
    }

    actual fun fileExists(path: String): Boolean {
        return File(path).exists()
    }
}
```

**iOS (actual):**

```kotlin
// Universal/IDEAMagic/CodeGen/CLI/src/iosMain/kotlin/FileIO.ios.kt
actual object FileIO {
    actual fun readFile(path: String): String {
        val fileHandle = NSFileManager.defaultManager.fileHandleForReadingAtPath(path)
        val data = fileHandle?.readDataToEndOfFile()
        return NSString.create(data, NSUTF8StringEncoding).toString()
    }

    actual fun writeFile(path: String, content: String) {
        content.writeToFile(path, atomically = true, encoding = NSUTF8StringEncoding)
    }

    actual fun fileExists(path: String): Boolean {
        return NSFileManager.defaultManager.fileExistsAtPath(path)
    }
}
```

### Platform-Specific Themes

**Common:**

```kotlin
// AvaUI/DesignSystem/src/commonMain/kotlin/MagicTheme.kt
data class MagicTheme(
    val colors: ColorPalette,
    val typography: Typography,
    val spacing: Spacing,
    val shapes: Shapes
)
```

**Android:**

```kotlin
// AvaUI/DesignSystem/src/androidMain/kotlin/MagicTheme.android.kt
@Composable
fun MagicTheme(
    theme: MagicTheme,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = theme.colors.toMaterial3ColorScheme(),
        typography = theme.typography.toMaterial3Typography(),
        content = content
    )
}
```

**iOS:**

```kotlin
// AvaUI/DesignSystem/src/iosMain/kotlin/MagicTheme.ios.kt
fun applyTheme(theme: MagicTheme) {
    UIColor.setAppearanceProxy(theme.colors.toUIColors())
    UIFont.setPreferredFonts(theme.typography.toUIFonts())
}
```

---

## 2.7 Cross-Platform Communication

### Data Serialization

All communication between platforms uses **JSON serialization**:

**Kotlin → Swift:**

```kotlin
// Kotlin side
val componentData = mapOf(
    "id" to "btn1",
    "type" to "Button",
    "text" to "Click Me"
)

val dict = componentData.toNSDictionary()
createSwiftUIHostingController(dict)
```

```swift
// Swift side
func createHostingController(_ data: NSDictionary) -> UIViewController {
    let id = data["id"] as! String
    let type = data["type"] as! String
    let text = data["text"] as! String

    let view = MagicButtonView(id: id, text: text)
    return UIHostingController(rootView: view)
}
```

**Kotlin → TypeScript:**

```kotlin
// Kotlin/JS side
val props = js("{}")
props.id = "btn1"
props.text = "Click Me"
props.onClick = { println("Clicked") }

ReactComponentLoader.renderComponent("MagicButton", props, "container")
```

```typescript
// TypeScript side
const MagicButton = (props: MagicButtonProps) => {
    return <Button onClick={props.onClick}>{props.text}</Button>;
};
```

### Event Propagation

Events flow from **UI → Runtime → Handler**:

**Android:**

```kotlin
// Generated code
Button(onClick = handleClick) { ... }

// Handler (in app)
val handleClick: () -> Unit = {
    println("Button clicked")
}
```

**iOS:**

```swift
// SwiftUI view
Button(action: handleClick) { ... }

// Handler (passed from Kotlin)
let handleClick: () -> Void = {
    print("Button clicked")
}
```

**Web:**

```typescript
// React component
<Button onClick={handleClick}>...</Button>

// Handler
const handleClick = () => {
    console.log('Button clicked');
};
```

---

## 2.8 Dependency Graph

### Build-Time Dependencies

```
CLI depends on:
  ├─ Parser
  └─ Generators (Kotlin, Swift, React)

Parser depends on:
  ├─ AST
  └─ kotlinx.serialization

Generators depend on:
  └─ AST

Components/Adapters depend on:
  ├─ Components/Foundation
  ├─ Components/Core
  └─ AvaUI/DesignSystem

AvaUI/ThemeManager depends on:
  ├─ AvaUI/DesignSystem
  └─ Database
```

### Runtime Dependencies

**Android:**

```gradle
dependencies {
    implementation("androidx.compose.ui:ui:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation("io.coil-kt:coil-compose:2.4.0")
}
```

**iOS:**

```swift
// No external dependencies (SwiftUI built-in)
```

**Web:**

```json
{
  "dependencies": {
    "react": "^18.2.0",
    "@mui/material": "^5.14.0",
    "@emotion/react": "^11.11.0"
  }
}
```

---

## 2.9 Architectural Patterns

### Pattern 1: Repository Pattern

**Used in:** ThemeRepository, AssetManager

```kotlin
interface Repository<T> {
    suspend fun save(item: T): Result<Unit>
    suspend fun load(id: String): Result<T>
    suspend fun delete(id: String): Result<Unit>
    suspend fun loadAll(): Result<List<T>>
}

class ThemeRepository : Repository<MagicTheme> {
    override suspend fun save(item: MagicTheme): Result<Unit> {
        // Save to local storage
        localStorage.set("theme_${item.name}", item.toJson())
        return Result.success(Unit)
    }

    override suspend fun load(id: String): Result<MagicTheme> {
        val json = localStorage.get("theme_$id")
            ?: return Result.failure(Exception("Theme not found"))
        return Result.success(MagicTheme.fromJson(json))
    }
}
```

### Pattern 2: Factory Pattern

**Used in:** CodeGeneratorFactory, VoiceOSBridgeFactory

```kotlin
object CodeGeneratorFactory {
    fun create(platform: Platform, language: Language? = null): CodeGenerator {
        return when (platform) {
            Platform.ANDROID -> KotlinComposeGenerator()
            Platform.IOS -> SwiftUIGenerator()
            Platform.WEB -> when (language) {
                Language.TYPESCRIPT -> ReactTypeScriptGenerator()
                Language.JAVASCRIPT -> ReactJavaScriptGenerator()
                else -> ReactTypeScriptGenerator()
            }
            Platform.DESKTOP -> KotlinComposeGenerator()
        }
    }
}
```

### Pattern 3: Visitor Pattern

**Used in:** AST traversal, code generation

```kotlin
interface AvaUIVisitor {
    fun visitScreen(screen: ScreenNode)
    fun visitComponent(component: ComponentNode)
    fun visitState(state: StateVariable)
}

class CodeGeneratorVisitor : AvaUIVisitor {
    private val code = StringBuilder()

    override fun visitScreen(screen: ScreenNode) {
        code.appendLine("@Composable")
        code.appendLine("fun ${screen.name}() {")
        screen.root.accept(this)
        code.appendLine("}")
    }

    override fun visitComponent(component: ComponentNode) {
        when (component.type) {
            ComponentType.BUTTON -> generateButton(component)
            ComponentType.TEXT -> generateText(component)
            // ...
        }
    }
}
```

### Pattern 4: Builder Pattern

**Used in:** Component creation, code generation

```kotlin
class ScreenBuilder {
    private var name: String = ""
    private var root: ComponentNode? = null
    private val stateVariables = mutableListOf<StateVariable>()

    fun name(name: String) = apply { this.name = name }
    fun root(root: ComponentNode) = apply { this.root = root }
    fun state(name: String, type: String, initial: Any? = null) = apply {
        stateVariables.add(StateVariable(name, type, initial))
    }

    fun build(): ScreenNode {
        require(name.isNotEmpty()) { "Name is required" }
        require(root != null) { "Root component is required" }
        return ScreenNode(name, root!!, stateVariables)
    }
}

// Usage
val screen = ScreenBuilder()
    .name("LoginScreen")
    .state("email", "String", "")
    .state("password", "String", "")
    .root(
        ComponentNode(
            type = ComponentType.CARD,
            children = listOf(...)
        )
    )
    .build()
```

### Pattern 5: Strategy Pattern

**Used in:** Platform-specific rendering

```kotlin
interface RenderStrategy {
    fun render(component: ComponentNode): String
}

class ComposeRenderStrategy : RenderStrategy {
    override fun render(component: ComponentNode): String {
        return when (component.type) {
            ComponentType.BUTTON -> renderComposeButton(component)
            ComponentType.TEXT -> renderComposeText(component)
            // ...
        }
    }
}

class SwiftUIRenderStrategy : RenderStrategy {
    override fun render(component: ComponentNode): String {
        return when (component.type) {
            ComponentType.BUTTON -> renderSwiftUIButton(component)
            ComponentType.TEXT -> renderSwiftUIText(component)
            // ...
        }
    }
}

// Usage
val strategy: RenderStrategy = when (platform) {
    Platform.ANDROID -> ComposeRenderStrategy()
    Platform.IOS -> SwiftUIRenderStrategy()
    Platform.WEB -> ReactRenderStrategy()
}

val code = strategy.render(component)
```

---

## 2.10 Chapter Summary

In this chapter, you learned:

✅ **System Architecture:** 5-layer architecture (Input → Parsing → AST → Generation → Runtime)

✅ **Module Organization:** Directory structure, dependency graph, and component relationships

✅ **Data Flow:** Complete flow from JSON DSL to rendered UI

✅ **Component Lifecycle:** States (Defined → Parsed → Validated → Generated → Compiled → Rendered)

✅ **Platform Abstraction:** Expect/actual mechanism for cross-platform code

✅ **Cross-Platform Communication:** JSON serialization, event propagation

✅ **Architectural Patterns:** Repository, Factory, Visitor, Builder, Strategy

**Key Takeaways:**

1. **Clean Separation:** Each layer has clear responsibilities and minimal coupling
2. **Platform Independence:** Layers 1-3 are 100% platform-agnostic
3. **Type Safety:** Kotlin Multiplatform ensures compile-time checking
4. **Flexibility:** Generators and renderers can be replaced independently

**Next Chapter:** Chapter 3 - Design Decisions (ADRs)

We'll explore the **why** behind major architectural decisions, alternatives considered, and trade-offs made.

---

**Chapter 2 Complete**

**Created by Manoj Jhawar, manoj@ideahq.net**
**Date: 2025-11-02**
