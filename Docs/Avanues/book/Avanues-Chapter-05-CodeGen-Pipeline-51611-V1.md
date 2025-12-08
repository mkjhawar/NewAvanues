# Chapter 5: CodeGen Pipeline

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~12,000 words

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Pipeline Architecture](#2-pipeline-architecture)
3. [AST Definition](#3-ast-definition)
4. [Code Generators](#4-code-generators)
5. [Platform-Specific Generation](#5-platform-specific-generation)
6. [CLI Tool](#6-cli-tool)
7. [Summary](#7-summary)

---

## 1. Introduction

The **CodeGen Pipeline** transforms JSON DSL into native platform code. This is the **Layer 4** of the IDEAMagic architecture (see Chapter 2).

**Pipeline Flow:**

```
JSON DSL           Parser              AST               Generator          Native Code
  (Input)    →   (Layer 2-3)    →   (Layer 3)    →    (Layer 4)    →      (Output)

{                JsonDSLParser      AvaUINode      KotlinCompose    @Composable fun
  "type": "Button",  ↓               ComponentNode    Generator        HelloScreen() {
  "text": "Click"    parse()          ↓                ↓                  Button(
}                                   ComponentType    generate()           onClick = {}
                                    .BUTTON                             ) {
                                                                          Text("Click")
                                                                        }
                                                                      }
```

### Key Components

1. **AST (Abstract Syntax Tree)** - `AvaUINode.kt` - Platform-agnostic representation
2. **Generators** - Platform-specific code generators (Kotlin/Swift/TypeScript)
3. **CodeGeneratorFactory** - Factory pattern for generator selection
4. **CLI Tool** - Command-line interface for code generation

---

## 2. Pipeline Architecture

### 2.1 The Five-Stage Pipeline

```
Stage 1: INPUT               Stage 2: PARSE            Stage 3: AST
┌─────────────┐             ┌──────────────┐          ┌──────────────┐
│ JSON DSL    │────────────>│ JsonDSLParser│────────>│ ScreenNode   │
│ File        │             │              │          │   ├─ imports │
│             │             │ parseScreen()│          │   ├─ state   │
└─────────────┘             └──────────────┘          │   └─ root    │
                                                       │   ComponentNode
                                                       └──────────────┘
                                                              │
                            ┌─────────────────────────────────┘
                            │
Stage 4: GENERATE           ▼                         Stage 5: OUTPUT
┌─────────────┐        ┌──────────────┐          ┌──────────────┐
│ Platform    │───────>│ Code         │────────>│ Native Code  │
│ Selection   │        │ Generator    │          │ File         │
│             │        │              │          │              │
│ ANDROID     │───────>│Kotlin        │────────>│ .kt file     │
│ IOS         │───────>│Compose       │────────>│              │
│ WEB         │        │Generator     │          └──────────────┘
└─────────────┘        └──────────────┘
```

### 2.2 Generator Factory Pattern

```kotlin
object CodeGeneratorFactory {
    fun create(platform: Platform, language: Language? = null): CodeGenerator {
        return when (platform) {
            Platform.ANDROID -> KotlinComposeGenerator()
            Platform.IOS -> SwiftUIGenerator()
            Platform.WEB -> ReactTypeScriptGenerator()
            Platform.DESKTOP -> KotlinComposeGenerator() // Compose Desktop
        }
    }
}
```

---

## 3. AST Definition

**Location:** `Universal/IDEAMagic/CodeGen/AST/src/commonMain/kotlin/net/ideahq/avamagic/codegen/ast/AvaUINode.kt`

### 3.1 Core Node Types

```kotlin
/**
 * AvaUINode - Base sealed class for all AST nodes
 */
sealed class AvaUINode {
    abstract val id: String
    abstract val type: ComponentType
    abstract val properties: Map<String, Any>
    abstract val children: List<AvaUINode>
}

/**
 * ComponentNode - Represents a UI component
 */
data class ComponentNode(
    override val id: String,
    override val type: ComponentType,
    override val properties: Map<String, Any> = emptyMap(),
    override val children: List<AvaUINode> = emptyList(),
    val eventHandlers: Map<String, String> = emptyMap()
) : AvaUINode()

/**
 * ScreenNode - Represents a complete screen/view
 */
data class ScreenNode(
    val name: String,
    val root: ComponentNode,
    val stateVariables: List<StateVariable> = emptyList(),
    val imports: List<String> = emptyList()
)
```

### 3.2 Component Types (48 total)

```kotlin
enum class ComponentType {
    // Foundation (9)
    BUTTON, CARD, CHECKBOX, CHIP, DIVIDER,
    IMAGE, LIST_ITEM, TEXT, TEXT_FIELD,

    // Core (2)
    COLOR_PICKER, ICON_PICKER,

    // Basic (6)
    ICON, LABEL, CONTAINER, ROW, COLUMN, SPACER,

    // Advanced (18)
    SWITCH, SLIDER, PROGRESS_BAR, SPINNER,
    ALERT, DIALOG, TOAST, TOOLTIP,
    RADIO, DROPDOWN, DATE_PICKER, TIME_PICKER,
    SEARCH_BAR, RATING, BADGE, FILE_UPLOAD,
    APP_BAR, BOTTOM_NAV,

    // Layout (5)
    DRAWER, PAGINATION, TABS, BREADCRUMB, ACCORDION,
    STACK, GRID, SCROLL_VIEW,

    // Custom
    CUSTOM
}
```

### 3.3 Property Value System

```kotlin
sealed class PropertyValue {
    data class StringValue(val value: String) : PropertyValue()
    data class IntValue(val value: Int) : PropertyValue()
    data class DoubleValue(val value: Double) : PropertyValue()
    data class BoolValue(val value: Boolean) : PropertyValue()
    data class EnumValue(val type: String, val value: String) : PropertyValue()
    data class ListValue(val items: List<PropertyValue>) : PropertyValue()
    data class MapValue(val items: Map<String, PropertyValue>) : PropertyValue()
    data class ReferenceValue(val ref: String) : PropertyValue()
}
```

**Example Usage:**

```kotlin
// JSON: { "text": "Hello", "enabled": true, "count": 5 }
// AST:
ComponentNode(
    id = "button1",
    type = ComponentType.BUTTON,
    properties = mapOf(
        "text" to PropertyValue.StringValue("Hello"),
        "enabled" to PropertyValue.BoolValue(true),
        "count" to PropertyValue.IntValue(5)
    )
)
```

---

## 4. Code Generators

### 4.1 CodeGenerator Interface

**Location:** `Universal/IDEAMagic/CodeGen/Generators/src/commonMain/kotlin/net/ideahq/avamagic/codegen/generators/CodeGenerator.kt`

```kotlin
/**
 * CodeGenerator - Base interface for all code generators
 */
interface CodeGenerator {
    /**
     * Generate complete screen code
     */
    fun generate(screen: ScreenNode): GeneratedCode

    /**
     * Generate single component code
     */
    fun generateComponent(component: ComponentNode): String
}

/**
 * GeneratedCode - Result of code generation
 */
data class GeneratedCode(
    val code: String,
    val language: Language,
    val platform: Platform,
    val imports: List<String> = emptyList(),
    val dependencies: List<String> = emptyList()
)

enum class Language {
    KOTLIN, SWIFT, TYPESCRIPT, JAVASCRIPT
}

enum class Platform {
    ANDROID, IOS, WEB, DESKTOP
}
```

### 4.2 KotlinComposeGenerator

**Location:** `Universal/IDEAMagic/CodeGen/Generators/Kotlin/src/commonMain/kotlin/.../KotlinComposeGenerator.kt`

#### 4.2.1 Main Generation Method

```kotlin
class KotlinComposeGenerator : CodeGenerator {

    override fun generate(screen: ScreenNode): GeneratedCode {
        val code = StringBuilder()

        // 1. Package declaration
        code.appendLine("package com.augmentalis.voiceos.ui.screens")
        code.appendLine()

        // 2. Imports
        generateImports(screen, code)
        code.appendLine()

        // 3. Composable function
        code.appendLine("@Composable")
        code.appendLine("fun ${screen.name}Screen() {")

        // 4. State variables
        screen.stateVariables.forEach { stateVar ->
            generateStateVariable(stateVar, code)
        }
        if (screen.stateVariables.isNotEmpty()) {
            code.appendLine()
        }

        // 5. Root component
        generateComponent(screen.root, code, indent = 1)

        code.appendLine("}")

        return GeneratedCode(
            code = code.toString(),
            language = Language.KOTLIN,
            platform = Platform.ANDROID
        )
    }
}
```

#### 4.2.2 Component Generation Examples

**Button:**

```kotlin
private fun generateButton(component: ComponentNode, code: StringBuilder, indent: String) {
    val text = component.properties["text"] ?: "Button"
    val onClick = component.eventHandlers["onClick"] ?: "{}"

    code.append("${indent}Button(")
    code.appendLine("onClick = $onClick) {")
    code.appendLine("$indent    Text(\"$text\")")
    code.appendLine("$indent}")
}

// Input AST:
ComponentNode(
    type = ComponentType.BUTTON,
    properties = mapOf("text" to "Click Me"),
    eventHandlers = mapOf("onClick" to "{ handleClick() }")
)

// Generated Code:
Button(onClick = { handleClick() }) {
    Text("Click Me")
}
```

**TextField:**

```kotlin
private fun generateTextField(component: ComponentNode, code: StringBuilder, indent: String) {
    val value = component.properties["value"] ?: ""
    val label = component.properties["label"] ?: ""
    val onValueChange = component.eventHandlers["onValueChange"] ?: "{}"

    code.appendLine("${indent}OutlinedTextField(")
    code.appendLine("$indent    value = $value,")
    code.appendLine("$indent    onValueChange = $onValueChange,")
    if (label.toString().isNotEmpty()) {
        code.appendLine("$indent    label = { Text(\"$label\") },")
    }
    code.appendLine("$indent    modifier = Modifier.fillMaxWidth()")
    code.appendLine("$indent)")
}

// Generated Code:
OutlinedTextField(
    value = username,
    onValueChange = { username = it },
    label = { Text("Username") },
    modifier = Modifier.fillMaxWidth()
)
```

**Column (with children):**

```kotlin
private fun generateColumn(component: ComponentNode, code: StringBuilder, indent: String, indentLevel: Int) {
    code.appendLine("${indent}Column(")
    code.appendLine("$indent    modifier = Modifier.fillMaxWidth()")
    code.appendLine("$indent) {")

    // Recursively generate children
    component.children.forEach { child ->
        generateComponent(child, code, indentLevel + 1)
    }

    code.appendLine("$indent}")
}

// Generated Code:
Column(
    modifier = Modifier.fillMaxWidth()
) {
    Text("Title")
    Button(onClick = {}) { Text("Submit") }
}
```

#### 4.2.3 State Variable Generation

```kotlin
private fun generateStateVariable(stateVar: StateVariable, code: StringBuilder) {
    val modifier = if (stateVar.mutable) "var" else "val"
    val initialValue = stateVar.initialValue?.let { formatPropertyValue(it) } ?: "null"

    code.append("    $modifier ${stateVar.name} by ")
    if (stateVar.mutable) {
        code.appendLine("remember { mutableStateOf($initialValue) }")
    } else {
        code.appendLine("remember { $initialValue }")
    }
}

// Input:
StateVariable(
    name = "username",
    type = "String",
    initialValue = PropertyValue.StringValue(""),
    mutable = true
)

// Generated Code:
var username by remember { mutableStateOf("") }
```

#### 4.2.4 Complete Example

**Input JSON:**

```json
{
  "name": "LoginScreen",
  "stateVariables": [
    {
      "name": "username",
      "type": "String",
      "initialValue": "",
      "mutable": true
    },
    {
      "name": "password",
      "type": "String",
      "initialValue": "",
      "mutable": true
    }
  ],
  "root": {
    "type": "COLUMN",
    "children": [
      {
        "type": "TEXT",
        "properties": {
          "content": "Login",
          "variant": "H1"
        }
      },
      {
        "type": "TEXT_FIELD",
        "properties": {
          "value": "username",
          "label": "Username"
        },
        "eventHandlers": {
          "onValueChange": "{ username = it }"
        }
      },
      {
        "type": "TEXT_FIELD",
        "properties": {
          "value": "password",
          "label": "Password"
        },
        "eventHandlers": {
          "onValueChange": "{ password = it }"
        }
      },
      {
        "type": "BUTTON",
        "properties": {
          "text": "Login"
        },
        "eventHandlers": {
          "onClick": "{ handleLogin(username, password) }"
        }
      }
    ]
  }
}
```

**Generated Kotlin Code:**

```kotlin
package com.augmentalis.voiceos.ui.screens

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreenScreen() {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Login",
            style = MaterialTheme.typography.headlineLarge
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { handleLogin(username, password) }) {
            Text("Login")
        }
    }
}
```

---

## 5. Platform-Specific Generation

### 5.1 Android (Kotlin + Jetpack Compose)

**Generator:** `KotlinComposeGenerator`
**Output:** `.kt` files with `@Composable` functions
**UI Framework:** Material3 (Material Design 3)

**Key Features:**
- State management: `remember { mutableStateOf() }`
- Modifiers: `Modifier.fillMaxWidth()`, `Modifier.padding(16.dp)`
- Layout: `Column`, `Row`, `Box`, `LazyColumn`
- Components: `Button`, `Text`, `TextField`, `Card`, `Checkbox`

**Import Structure:**

```kotlin
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
```

### 5.2 iOS (Swift + SwiftUI)

**Generator:** `SwiftUIGenerator`
**Output:** `.swift` files with `View` structs
**UI Framework:** SwiftUI (Apple)

**Example Output:**

```swift
import SwiftUI

struct LoginScreenView: View {
    @State private var username: String = ""
    @State private var password: String = ""

    var body: some View {
        VStack {
            Text("Login")
                .font(.largeTitle)

            TextField("Username", text: $username)
                .textFieldStyle(RoundedBorderTextFieldStyle())

            SecureField("Password", text: $password)
                .textFieldStyle(RoundedBorderTextFieldStyle())

            Button("Login") {
                handleLogin(username: username, password: password)
            }
            .buttonStyle(.borderedProminent)
        }
        .padding()
    }
}
```

**Key Features:**
- State: `@State private var`
- Binding: `$username` (two-way binding)
- Layout: `VStack`, `HStack`, `ZStack`, `List`
- Modifiers: `.padding()`, `.font()`, `.buttonStyle()`

### 5.3 Web (TypeScript + React)

**Generator:** `ReactTypeScriptGenerator`
**Output:** `.tsx` files with React functional components
**UI Framework:** Material-UI (MUI)

**Example Output:**

```typescript
import React, { useState } from 'react';
import {
  Typography,
  TextField,
  Button,
  Stack
} from '@mui/material';

export const LoginScreen: React.FC = () => {
  const [username, setUsername] = useState<string>('');
  const [password, setPassword] = useState<string>('');

  return (
    <Stack spacing={2}>
      <Typography variant="h1">
        Login
      </Typography>

      <TextField
        label="Username"
        value={username}
        onChange={(e) => setUsername(e.target.value)}
        fullWidth
      />

      <TextField
        label="Password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        fullWidth
      />

      <Button
        variant="contained"
        onClick={() => handleLogin(username, password)}
      >
        Login
      </Button>
    </Stack>
  );
};
```

**Key Features:**
- State: `useState<T>()` hook
- Events: `onChange`, `onClick`
- Layout: `<Stack>`, `<Grid>`, `<Box>`
- Components: Material-UI components

### 5.4 Platform Comparison

| Feature | Android (Compose) | iOS (SwiftUI) | Web (React) |
|---------|------------------|---------------|-------------|
| Language | Kotlin | Swift | TypeScript |
| State | `mutableStateOf()` | `@State` | `useState()` |
| Binding | Variable | `$variable` | Setter function |
| Layout | `Column`, `Row` | `VStack`, `HStack` | `<Stack>`, `<Grid>` |
| Styling | Modifiers | Modifiers | CSS-in-JS |
| Navigation | Navigation Compose | NavigationView | React Router |

---

## 6. CLI Tool

**Location:** `Universal/IDEAMagic/CodeGen/CLI/src/commonMain/kotlin/.../AvaCodeCLI.kt`

### 6.1 Command Structure

```bash
avacode generate \
  --input screen.json \
  --output LoginScreen.kt \
  --platform android \
  --language kotlin
```

### 6.2 CLI Implementation

```kotlin
class AvaCodeCLIImpl(
    private val parser: JsonDSLParser,
    private val fileIO: FileIO
) {
    fun generate(
        inputFile: String,
        outputFile: String?,
        platform: Platform,
        language: Language?
    ): Int {
        // 1. Read input file
        if (!fileIO.fileExists(inputFile)) {
            println("Error: Input file not found: $inputFile")
            return 1
        }

        val dslContent = fileIO.readFile(inputFile)

        // 2. Parse JSON DSL → AST
        val screenResult = parser.parseScreen(dslContent)
        if (screenResult.isFailure) {
            println("Error: ${screenResult.exceptionOrNull()?.message}")
            return 1
        }
        val screen = screenResult.getOrThrow()

        // 3. Select generator
        val generator = CodeGeneratorFactory.create(platform, language)

        // 4. Generate code
        val generated = generator.generate(screen)

        // 5. Determine output file
        val output = outputFile ?: determineOutputFile(screen.name, platform)

        // 6. Write output
        fileIO.writeFile(output, generated.code)

        println("✅ Generated: $output")
        println("   Platform: ${generated.platform}")
        println("   Language: ${generated.language}")

        return 0
    }

    private fun determineOutputFile(screenName: String, platform: Platform): String {
        return when (platform) {
            Platform.ANDROID -> "${screenName}Screen.kt"
            Platform.IOS -> "${screenName}View.swift"
            Platform.WEB -> "${screenName}.tsx"
            Platform.DESKTOP -> "${screenName}Screen.kt"
        }
    }
}
```

### 6.3 CLI Usage Examples

**Generate Android Compose:**

```bash
avacode generate \
  --input screens/login.json \
  --output src/LoginScreen.kt \
  --platform android
```

**Generate iOS SwiftUI:**

```bash
avacode generate \
  --input screens/login.json \
  --output Views/LoginView.swift \
  --platform ios
```

**Generate React TypeScript:**

```bash
avacode generate \
  --input screens/login.json \
  --output components/LoginScreen.tsx \
  --platform web \
  --language typescript
```

**Batch Generation (All Platforms):**

```bash
# Android
avacode generate --input screens/*.json --platform android --output android/src/

# iOS
avacode generate --input screens/*.json --platform ios --output ios/Views/

# Web
avacode generate --input screens/*.json --platform web --output web/src/components/
```

---

## 7. Summary

The **CodeGen Pipeline** transforms JSON DSL into native platform code through a 5-stage process:

1. **Input** - JSON DSL file
2. **Parse** - `JsonDSLParser` creates `ScreenNode` AST
3. **AST** - Platform-agnostic `AvaUINode` representation
4. **Generate** - Platform-specific `CodeGenerator` (Kotlin/Swift/TypeScript)
5. **Output** - Native code file (.kt/.swift/.tsx)

**Key Components:**
- **AvaUINode** - 48 component types (Foundation, Core, Basic, Advanced, Layout)
- **CodeGenerator** - Interface with `generate()` and `generateComponent()` methods
- **KotlinComposeGenerator** - Android Jetpack Compose code generation
- **SwiftUIGenerator** - iOS SwiftUI code generation
- **ReactTypeScriptGenerator** - Web React + TypeScript generation
- **CodeGeneratorFactory** - Factory pattern for generator selection
- **AvaCodeCLI** - Command-line interface for batch generation

**Code Generation Characteristics:**
- **Type-safe** - Generates properly typed platform code
- **Native performance** - No runtime overhead, compiles to native
- **Idiomatic** - Follows platform conventions (Compose/SwiftUI/React)
- **Maintainable** - Clean, readable generated code
- **Extensible** - Easy to add new component types

**Next Chapter:** Chapter 6 will document all 48 components in the Component Library with complete specifications, properties, and generated code examples.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
