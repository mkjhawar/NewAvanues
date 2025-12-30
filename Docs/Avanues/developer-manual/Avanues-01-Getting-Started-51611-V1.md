# AvaElements Developer Manual - Getting Started

**Version:** 2.0.0
**Last Updated:** 2025-11-13
**Audience:** Android/iOS/Web developers building with AvaElements

---

## Table of Contents

1. [Introduction](#introduction)
2. [Prerequisites](#prerequisites)
3. [Project Setup](#project-setup)
4. [Architecture Overview](#architecture-overview)
5. [Your First Component](#your-first-component)
6. [Running the Project](#running-the-project)

---

## Introduction

**AvaElements** is a cross-platform UI component framework built with Kotlin Multiplatform that provides:

- **67 pre-built components** (13 foundation + 54 advanced)
- **Platform-specific renderers** for Android, iOS, and Web
- **Theme system** with dynamic color schemes
- **Type-safe DSL** for declarative UI building
- **Zero boilerplate** - components render automatically

### What Makes AvaElements Different?

Unlike traditional UI frameworks:
- ✅ **Define once, render anywhere** - Components are platform-agnostic
- ✅ **No XML/SwiftUI/JSX** - Pure Kotlin with type safety
- ✅ **Automatic theming** - Components inherit theme automatically
- ✅ **Professional Material3** - Android uses latest Material Design
- ✅ **Extensible** - Add custom components easily

---

## Prerequisites

### Required Tools

**Development Environment:**
- **JDK 17+** (Java Development Kit)
- **Android Studio Hedgehog (2023.1.1) or later**
- **Kotlin 1.9.24+**
- **Gradle 8.10.2**

**Platform-Specific:**
- **Android SDK 24+** (Android 7.0 Nougat or later)
- **Xcode 15+** (for iOS development, macOS only)
- **Node.js 18+** (for Web renderer, optional)

### Knowledge Prerequisites

**Required:**
- Kotlin programming basics
- Understanding of Compose UI (for Android)
- Familiarity with Material Design

**Helpful:**
- Kotlin Multiplatform concepts
- SwiftUI (for iOS development)
- React (for Web development)

---

## Project Setup

### Clone the Repository

```bash
# Clone the Avanues project
git clone https://github.com/yourusername/Avanues.git
cd Avanues

# Checkout the correct branch
git checkout avanues-migration
```

### Project Structure

```
Avanues/
├── Universal/
│   └── Libraries/
│       └── AvaElements/
│           ├── Core/                    # Core types and interfaces
│           ├── components/
│           │   ├── phase1/             # 13 foundation components
│           │   └── phase3/             # 54 advanced components
│           └── renderers/
│               ├── android/            # Android Compose renderers
│               ├── ios/                # iOS SwiftUI renderers (planned)
│               └── web/                # React renderers (planned)
│
├── modules/
│   └── MagicIdea/
│       ├── UI/                         # Legacy UI framework
│       └── Components/                 # Component builders & tools
│
├── android/                            # Android-specific implementations
├── docs/                              # Documentation
└── settings.gradle.kts                # Module configuration
```

### Gradle Configuration

The project is already configured in `settings.gradle.kts`:

```kotlin
// AvaElements modules are included
include(":Universal:Libraries:AvaElements:Core")
include(":Universal:Libraries:AvaElements:components:phase1")
include(":Universal:Libraries:AvaElements:components:phase3")
include(":Universal:Libraries:AvaElements:renderers:android")
```

### Build the Project

```bash
# Build all modules
./gradlew build

# Build Android renderer specifically
./gradlew :Universal:Libraries:AvaElements:renderers:android:build

# Run tests
./gradlew test
```

**Expected Output:**
```
BUILD SUCCESSFUL in 15s
41 actionable tasks: 26 executed, 15 from cache
```

---

## Architecture Overview

### Component Model

AvaElements uses a **renderer pattern**:

```
┌─────────────────────────────────────────┐
│         Component Definition            │
│  (Platform-agnostic Kotlin data class)  │
└─────────────────┬───────────────────────┘
                  │
                  │ render(renderer)
                  ▼
┌─────────────────────────────────────────┐
│            Renderer                     │
│  (Platform-specific implementation)     │
└─────────────────┬───────────────────────┘
                  │
        ┌─────────┴──────────┐
        │                    │
        ▼                    ▼
┌───────────────┐   ┌───────────────┐
│Android Compose│   │  iOS SwiftUI  │
│  (Material3)  │   │  (Planned)    │
└───────────────┘   └───────────────┘
```

### Three-Layer Architecture

**1. Core Layer** (`AvaElements:Core`)
```kotlin
// Defines base types
interface Component {
    val id: String?
    val style: ComponentStyle?
    val modifiers: List<Modifier>
    fun render(renderer: Renderer)
}

interface Renderer {
    val theme: Theme
    fun render(component: Component): @Composable (() -> Unit)
}
```

**2. Component Layer** (`components:phase1`, `components:phase3`)
```kotlin
// Platform-agnostic component definitions
data class Button(
    override val id: String? = null,
    val text: String,
    val onClick: (() -> Unit)? = null,
    val enabled: Boolean = true,
    override val style: ComponentStyle? = null,
    @Transient override val modifiers: List<Modifier> = emptyList()
) : Component {
    override fun render(renderer: Renderer) = renderer.render(this)
}
```

**3. Renderer Layer** (`renderers:android`, `renderers:ios`, etc.)
```kotlin
// Platform-specific rendering
@Composable
fun RenderButton(c: Button, theme: Theme) = Button(
    onClick = c.onClick ?: {},
    enabled = c.enabled,
    colors = ButtonDefaults.buttonColors(
        containerColor = theme.colorScheme.primary.toCompose()
    )
) { Text(text = c.text) }
```

### Theme System

Every renderer receives a `Theme` object:

```kotlin
data class Theme(
    val colorScheme: ColorScheme,
    val typography: Typography,
    val spacing: Spacing,
    val shapes: Shapes
)

// Components automatically use theme colors
colors = ButtonDefaults.buttonColors(
    containerColor = theme.colorScheme.primary.toCompose()
)
```

---

## Your First Component

### Step 1: Create a Component Instance

```kotlin
import com.augmentalis.avaelements.components.phase1.form.Button

val myButton = Button(
    id = "submit-btn",
    text = "Click Me!",
    onClick = { println("Button clicked!") },
    enabled = true
)
```

### Step 2: Render with Android Renderer

```kotlin
import com.augmentalis.avaelements.renderers.android.ComposeRenderer
import com.augmentalis.avaelements.core.ThemeProvider

@Composable
fun MyScreen() {
    val renderer = ComposeRenderer(theme = ThemeProvider.getCurrentTheme())

    // Render the button
    val buttonComposable = myButton.render(renderer)
    buttonComposable()
}
```

### Step 3: Use in Compose UI

```kotlin
@Composable
fun MyApp() {
    MaterialTheme {
        Surface {
            Column(modifier = Modifier.padding(16.dp)) {
                // Create and render multiple components
                val button = Button(text = "Submit", onClick = { /* action */ })
                val textField = TextField(
                    value = "Hello",
                    onChange = { /* update */ },
                    label = "Name"
                )

                val renderer = ComposeRenderer()

                button.render(renderer)()
                Spacer(modifier = Modifier.height(8.dp))
                textField.render(renderer)()
            }
        }
    }
}
```

---

## Running the Project

### Android

**Option 1: Android Studio**
1. Open project in Android Studio
2. Select Android module in run configuration
3. Click Run (▶️)

**Option 2: Command Line**
```bash
# Build APK
./gradlew :android:apps:voiceos:assembleDebug

# Install on device
./gradlew :android:apps:voiceos:installDebug

# Run on connected device
adb shell am start -n com.augmentalis.voiceos/.MainActivity
```

### iOS (Planned)

```bash
# Build iOS framework
./gradlew :Universal:Libraries:AvaElements:renderers:ios:linkDebugFrameworkIosArm64

# Open Xcode project
open ios/VoiceAvanue.xcworkspace
```

### Web (Planned)

```bash
# Build web bundle
./gradlew :Universal:Libraries:AvaElements:renderers:web:browserDevelopmentWebpack

# Start dev server
npm run dev
```

---

## Next Steps

📖 **Continue to:**
- [Chapter 02 - Component Guide](./02-Component-Guide.md) - Learn about all 67 components
- [Chapter 03 - Android Renderer](./03-Android-Renderer.md) - Deep dive into Android implementation
- [Chapter 04 - Theme System](./04-Theme-System.md) - Customize colors and styles
- [Chapter 05 - Building Custom Components](./05-Custom-Components.md) - Extend the framework

📚 **Tutorials:**
- [Tutorial 01 - Building a Login Screen](../tutorials/01-Login-Screen.md)
- [Tutorial 02 - Form Validation](../tutorials/02-Form-Validation.md)
- [Tutorial 03 - Navigation Patterns](../tutorials/03-Navigation-Patterns.md)

---

**Questions?** See [FAQ.md](./FAQ.md) or file an issue on GitHub.

**Contributing?** Read [CONTRIBUTING.md](../../CONTRIBUTING.md) for guidelines.

---

**Version:** 2.0.0
**Framework:** AvaElements on Kotlin Multiplatform
**Methodology:** IDEACODE 7.2.0
**Author:** Manoj Jhawar (manoj@ideahq.net)
