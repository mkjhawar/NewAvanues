# .vos File Format Specification

**Date**: 2025-10-27 13:00 PDT
**Version**: 3.1.0
**Purpose**: Define the complete .vos file format for VoiceOS applications

---

## Overview

`.vos` files are VoiceOS application definition files that combine:
1. **AvaUI DSL** - UI components and layouts
2. **AvaCode** - Code generation instructions
3. **Plugin Metadata** - Plugin configuration

**File Extension**: `.vos`
**MIME Type**: `application/x-voiceos`
**Character Encoding**: UTF-8

---

## File Header

Every .vos file MUST start with a shebang-style header:

```
#!vos:MODE
```

### Mode Flags

| Mode | Description | Use Case |
|------|-------------|----------|
| `D` | **DSL Mode** (AvaUI) | Runtime-interpreted UI apps |
| `K` | **Kotlin Mode** (AvaCode) | Generate Kotlin Compose code |
| `Y` | **YAML Mode** | Legacy plugin configs |
| `J` | **JSON Mode** | Data-driven configs |
| `X` | **Mixed Mode** | Combine multiple modes |

**Examples**:
```
#!vos:D          # Pure DSL (runtime interpretation)
#!vos:K          # Pure Kotlin codegen
#!vos:X          # Mixed DSL + Kotlin
```

---

## File Structure

### Structure 1: Pure AvaUI DSL (Mode D)

```
#!vos:D
# VoiceOS DSL App

App {
  id: "com.example.app"
  name: "My App"
  runtime: "AvaUI"

  # UI Components (AvaUI)
  ColorPicker {
    id: "picker1"
    initialColor: "#FF5722"
    mode: "DESIGNER"

    onConfirm: (color) => {
      Preferences.set("theme.primary", color)
      VoiceOS.speak("Color saved!")
    }
  }

  # Voice Commands
  VoiceCommands {
    "change color" => "picker1.show"
    "reset theme" => "picker1.reset"
  }
}
```

**Interpretation**: AvaUI Runtime executes this directly (no code generation)

---

### Structure 2: AvaCode Generation (Mode K)

```
#!vos:K
# Generate Kotlin Compose Code

@Generate(
  target: "kotlin-compose",
  package: "com.example.generated",
  output: "app/src/main/kotlin/generated/"
)

App {
  id: "com.example.app"
  name: "My App"

  ColorPicker {
    id: "picker1"
    initialColor: "#FF5722"
    mode: "DESIGNER"

    onConfirm: (color) => {
      // This will be generated as Kotlin code
      preferencesManager.set("theme.primary", color)
    }
  }
}
```

**Code Generation**: AvaCode Codegen produces:
```kotlin
// Generated file: GeneratedMyApp.kt
package com.example.generated

@Composable
fun MyApp() {
    var selectedColor by remember { mutableStateOf("#FF5722") }

    ColorPickerView(
        initialColor = selectedColor,
        mode = ColorPickerMode.DESIGNER,
        onConfirm = { color ->
            preferencesManager.set("theme.primary", color)
        }
    )
}
```

---

### Structure 3: Plugin Definition (Mode Y)

```
#!vos:Y
# Plugin Configuration (YAML-based)

plugin:
  id: "com.example.plugin"
  name: "My Plugin"
  version: "1.0.0"
  author: "Developer Name"

theme:
  name: "Dark Theme"
  palette:
    primary: "#007AFF"
    secondary: "#5AC8FA"

components:
  - name: "CustomButton"
    type: "Button"
    properties:
      text: "Click Me"
```

**Usage**: Plugin system loads this as plugin metadata + theme

---

### Structure 4: Mixed Mode (Mode X)

```
#!vos:X
# Mixed DSL + Codegen

@Runtime(mode: "DSL")
App {
  id: "com.example.hybrid"
  name: "Hybrid App"

  # This part runs in DSL runtime
  @DSL
  ColorPicker {
    id: "picker1"
    initialColor: "#FF5722"
  }

  # This part generates Kotlin code
  @Codegen(target: "kotlin-compose")
  CustomComponent {
    id: "custom1"
    // Will be generated as Kotlin
  }
}
```

---

## AvaUI vs AvaCode: The Difference

### AvaUI (DSL Interpreter)

**Purpose**: Runtime interpretation of UI definitions

**Mode**: `#!vos:D`

**How it works**:
1. Parse .vos file → AST
2. Component Registry → Instantiate native objects
3. Execute callbacks → Runtime
4. No code generation

**When to use**:
- User-created apps (AVA AI generates these)
- Dynamic UI that changes at runtime
- Rapid prototyping
- Plugins with hot-reload

**Example**:
```
#!vos:D
App {
  ColorPicker { ... }  # Interpreted at runtime
}
```

---

### AvaCode (Code Generator)

**Purpose**: Generate native Kotlin/Swift/JS code from DSL

**Mode**: `#!vos:K`

**How it works**:
1. Parse .vos file → AST
2. Code Generator → Kotlin/Swift/JS source
3. Developer compiles generated code
4. Runs as native app (no runtime overhead)

**When to use**:
- Production apps (best performance)
- Apps that need compile-time safety
- Apps for App Store distribution
- Developer-written apps (not user-generated)

**Example**:
```
#!vos:K
@Generate(target: "kotlin-compose")
App {
  ColorPicker { ... }  # Generates Kotlin Compose code
}
```

---

## Comparison: AvaUI vs AvaCode

| Feature | AvaUI (DSL) | AvaCode (Codegen) |
|---------|---------------|---------------------|
| **File Header** | `#!vos:D` | `#!vos:K` |
| **Execution** | Runtime interpretation | Compiled native code |
| **Performance** | Slower (interpreted) | Faster (compiled) |
| **Dynamic** | ✅ Yes (hot-reload) | ❌ No (rebuild required) |
| **User-Generated** | ✅ Yes (AVA AI) | ❌ No (developer-only) |
| **Type Safety** | Runtime errors | Compile-time errors |
| **Output** | Runs in AvaUI Runtime | Kotlin/Swift/JS files |
| **Use Case** | Plugins, user apps, prototypes | Production apps |
| **Debugging** | DSL debugger | Native debugger |

---

## Complete .vos File Example (Mode D)

```
#!vos:D
# Settings App
# Created: 2025-10-27
# Author: AVA AI

App {
  id: "com.voiceos.settings"
  name: "VoiceOS Settings"
  version: "3.1.0"
  runtime: "AvaUI"

  # Metadata
  metadata: {
    description: "System settings app"
    icon: "settings_icon.png"
    category: "System"
  }

  # Theme
  theme: {
    name: "Dark Theme"
    palette: {
      primary: "#007AFF"
      secondary: "#5AC8FA"
      background: "#000000"
      surface: "#1C1C1E"
    }
  }

  # Layout
  Container {
    id: "mainContainer"
    orientation: "vertical"

    Text {
      id: "title"
      text: "Settings"
      size: 28
      weight: "bold"
    }

    ColorPicker {
      id: "themePicker"
      initialColor: "#007AFF"
      mode: "DESIGNER"
      showAlpha: true

      onColorChanged: (color) => {
        Preferences.set("current.color", color)
      }

      onConfirm: (color) => {
        Preferences.set("theme.primary", color)
        VoiceOS.speak("Theme color updated!")
        VoiceOS.vibrate(50)
      }

      onCancel: () => {
        VoiceOS.speak("Cancelled")
      }
    }

    Button {
      id: "resetButton"
      text: "Reset Theme"
      enabled: true

      onClick: () => {
        themePicker.reset()
        Preferences.remove("theme.primary")
        VoiceOS.speak("Theme reset to default")
      }
    }
  }

  # Voice Commands
  VoiceCommands {
    "change color" => "themePicker.show"
    "change theme" => "themePicker.show"
    "reset theme" => "resetButton.click"
    "go back" => "App.finish"
  }

  # Lifecycle Hooks
  onCreate: () => {
    VoiceOS.log("Settings app created")

    # Load saved theme
    savedColor = Preferences.get("theme.primary")
    if (savedColor != null) {
      themePicker.setColor(savedColor)
    }
  }

  onStart: () => {
    VoiceOS.log("Settings app started")
  }

  onPause: () => {
    VoiceOS.log("Settings app paused")
    # Save state
    Preferences.set("last.opened", Date.now())
  }

  onDestroy: () => {
    VoiceOS.log("Settings app destroyed")
  }
}
```

---

## Complete .vos File Example (Mode K - Codegen)

```
#!vos:K
# Settings App - Code Generation
# Output: Kotlin Compose

@Generate(
  target: "kotlin-compose",
  package: "com.voiceos.settings.generated",
  output: "app/src/main/kotlin/generated/",
  style: "material3"
)

@Imports(
  "androidx.compose.material3.*",
  "androidx.compose.runtime.*",
  "androidx.compose.ui.Modifier"
)

App {
  id: "com.voiceos.settings"
  name: "VoiceOS Settings"

  Container {
    orientation: "vertical"

    Text {
      text: "Settings"
      size: 28
      weight: "bold"
    }

    ColorPicker {
      id: "themePicker"
      initialColor: "#007AFF"
      mode: "DESIGNER"

      onConfirm: (color) => {
        preferencesManager.set("theme.primary", color)
        ttsEngine.speak("Theme color updated!")
      }
    }
  }
}
```

**Generated Code** (GeneratedSettingsApp.kt):
```kotlin
package com.voiceos.settings.generated

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.augmentalis.voiceos.colorpicker.ColorPickerView
import com.augmentalis.voiceos.colorpicker.ColorRGBA

@Composable
fun VoiceOSSettings() {
    var selectedColor by remember { mutableStateOf(ColorRGBA.fromHexString("#007AFF")) }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.headlineLarge
        )

        ColorPickerView(
            initialColor = selectedColor,
            mode = ColorPickerMode.DESIGNER,
            onConfirm = { color ->
                preferencesManager.set("theme.primary", color.toHexString())
                ttsEngine.speak("Theme color updated!")
            }
        )
    }
}
```

---

## Syntax Reference

### Comments
```
# Single line comment
// Also supported
/* Multi-line
   comment */
```

### Data Types
```
String: "text"
Int: 123
Float: 45.67
Boolean: true, false
Color: "#FF5722"
Array: [item1, item2, item3]
Object: { key: value }
```

### Property Syntax
```
propertyName: value
propertyName: "string value"
propertyName: 123
```

### Callback Syntax
```
onEvent: (param) => {
  statement1
  statement2
}

onEvent: (param1, param2) => {
  statement
}
```

### Component Syntax
```
ComponentType {
  id: "uniqueId"
  property: value

  onCallback: (params) => {
    statements
  }

  NestedComponent {
    ...
  }
}
```

---

## Runtime APIs Available in Callbacks

### VoiceOS API
```kotlin
VoiceOS.speak(text: String)
VoiceOS.vibrate(duration: Int)
VoiceOS.log(message: String)
VoiceOS.toast(message: String)
VoiceOS.dialog(title: String, message: String)
```

### Preferences API
```kotlin
Preferences.set(key: String, value: Any)
Preferences.get(key: String): Any?
Preferences.remove(key: String)
Preferences.clear()
```

### Date API
```kotlin
Date.now(): Long
Date.format(timestamp: Long, format: String): String
```

---

## Code Generation Annotations

### @Generate
```kotlin
@Generate(
  target: "kotlin-compose" | "swiftui" | "react",
  package: "com.example.package",
  output: "path/to/output/",
  style: "material3" | "cupertino" | "fluent"
)
```

### @Imports
```kotlin
@Imports(
  "import.statement.one",
  "import.statement.two"
)
```

### @Runtime
```kotlin
@Runtime(
  mode: "DSL" | "Codegen" | "Hybrid"
)
```

### @DSL
```kotlin
@DSL  # This component runs in DSL runtime
```

### @Codegen
```kotlin
@Codegen(target: "kotlin-compose")  # This component generates code
```

---

## File Naming Conventions

| Pattern | Description | Example |
|---------|-------------|---------|
| `*.vos` | VoiceOS app definition | `settings.vos` |
| `*.vos.d` | DSL mode (explicit) | `settings.vos.d` |
| `*.vos.k` | Kotlin codegen (explicit) | `settings.vos.k` |
| `*.vos.yaml` | YAML mode (explicit) | `theme.vos.yaml` |
| `*.vos.json` | JSON mode (explicit) | `config.vos.json` |

**Note**: The mode flag in the header takes precedence over file extension.

---

## Plugin Structure with .vos

```
my-plugin/
├── plugin.vos              # Plugin metadata (mode Y)
├── theme.vos               # Plugin theme (mode D)
├── components/
│   ├── button.vos          # Custom component (mode D)
│   └── input.vos           # Custom component (mode D)
├── apps/
│   ├── settings.vos        # Plugin app (mode D)
│   └── dashboard.vos       # Plugin app (mode D)
└── generated/              # AvaCode output (mode K)
    ├── Button.kt
    └── Input.kt
```

---

## Best Practices

### 1. Use DSL (Mode D) for:
- User-generated apps (AVA AI)
- Plugins with hot-reload
- Prototypes and demos
- Apps that need runtime updates

### 2. Use Codegen (Mode K) for:
- Production apps
- Apps requiring maximum performance
- Apps for app store distribution
- Developer-written apps

### 3. Use YAML (Mode Y) for:
- Plugin configurations
- Theme definitions
- Simple configs

### 4. Use Mixed (Mode X) for:
- Hybrid apps (some DSL, some generated)
- Progressive migration (DSL → Codegen)
- Advanced use cases

---

## Migration Path: DSL → Codegen

```
Step 1: Write app in DSL
┌──────────────┐
│ app.vos (D)  │
│ #!vos:D      │
│ App { ... }  │
└──────────────┘

Step 2: Test in DSL Runtime
┌──────────────┐
│ AvaUI      │
│ Runtime      │
└──────────────┘

Step 3: Generate Kotlin code
┌──────────────┐
│ app.vos (K)  │
│ #!vos:K      │
│ @Generate    │
└──────────────┘

Step 4: Compile & Deploy
┌──────────────┐
│ app.apk      │
│ (Native)     │
└──────────────┘
```

---

## Summary

**AvaUI** = DSL Runtime (interprets .vos files)
**AvaCode** = Code Generator (generates Kotlin/Swift from .vos files)

**Both use .vos files**, but:
- AvaUI executes them directly (mode `D`)
- AvaCode generates native code from them (mode `K`)

**Think of it like**:
- **AvaUI** = JavaScript (interpreted at runtime)
- **AvaCode** = TypeScript → JavaScript (compiled to native)

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**Version**: 3.1.0
**Date**: 2025-10-27 13:00 PDT
