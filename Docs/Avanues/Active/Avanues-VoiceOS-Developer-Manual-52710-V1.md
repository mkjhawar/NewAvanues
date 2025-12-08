# VoiceOS Platform Developer Manual

**Version**: 3.1.0
**Date**: 2025-10-27
**Target Audience**: Platform developers, library authors, infrastructure maintainers

---

## Table of Contents

1. [Platform Overview](#platform-overview)
2. [Architecture](#architecture)
3. [AvaUI DSL Runtime](#avaui-dsl-runtime)
4. [Theme System](#theme-system)
5. [Component Development](#component-development)
6. [Library Development](#library-development)
7. [Testing](#testing)
8. [Deployment](#deployment)
9. [API Reference](#api-reference)
10. [Best Practices](#best-practices)

---

## Platform Overview

### What is VoiceOS?

VoiceOS is a voice-first, cross-platform application framework built on Kotlin Multiplatform (KMP). It provides:

- **AvaUI DSL Runtime**: Interpret and execute .vos applications at runtime
- **AvaCode Codegen**: Generate native Kotlin/Swift/JS code from .vos files (future)
- **Theme System**: Unified theme model with 6 import/export formats
- **Component Library**: Reusable UI components (ColorPicker, Preferences, etc.)
- **Voice Commands**: First-class voice interaction support

### Platform Components

```
VoiceOS Platform
├── AvaUI DSL Runtime (Runtime interpretation)
├── AvaCode Codegen (Code generation - future)
├── Theme System (Unified theming with format converters)
├── Runtime Libraries (ColorPicker, Preferences, Notepad, etc.)
└── Platform Utilities (Lifecycle, Events, Voice)
```

### Technology Stack

- **Language**: Kotlin Multiplatform (KMP)
- **Platforms**: Android, iOS, Desktop (JVM)
- **UI**: Jetpack Compose (Android), SwiftUI (iOS), Compose Desktop (JVM)
- **Serialization**: kotlinx.serialization, YAML (yamlkt)
- **Async**: Kotlin Coroutines + Flow
- **Testing**: kotlin.test, mockk

---

## Architecture

### Three-Layer Architecture

```
┌─────────────────────────────────────────────────────┐
│                 Application Layer                   │
│  - Settings App (.vos DSL)                         │
│  - Launcher App (.vos DSL)                         │
│  - User-generated apps (AVA AI)                    │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│              AvaUI DSL Runtime                    │
│  - Parser (Tokenizer → Parser → AST)               │
│  - Registry (Component metadata)                    │
│  - Instantiator (AST → Native objects)             │
│  - Events (EventBus + Callbacks)                   │
│  - Voice (Command router + fuzzy matching)         │
│  - Lifecycle (6-state management)                  │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│             Runtime Libraries (KMP)                 │
│  - ColorPicker (RGBA/HSV/HSL + palettes)           │
│  - Preferences (Cross-platform key-value storage)  │
│  - Notepad, Browser, CloudStorage, etc.            │
└────────────────┬────────────────────────────────────┘
                 │
┌────────────────▼────────────────────────────────────┐
│              Platform Layer                         │
│  - Android (Jetpack Compose)                       │
│  - iOS (SwiftUI)                                   │
│  - Desktop (Compose Desktop)                       │
└─────────────────────────────────────────────────────┘
```

### Kotlin Multiplatform Structure

```
runtime/libraries/MyLibrary/
├── src/
│   ├── commonMain/kotlin/           # Shared logic (all platforms)
│   │   └── com/augmentalis/voiceos/mylibrary/
│   │       ├── MyLibrary.kt         # Core logic
│   │       └── models/              # Data models
│   ├── androidMain/kotlin/          # Android-specific
│   │   └── com/augmentalis/voiceos/mylibrary/
│   │       └── MyLibraryAndroid.kt  # expect/actual implementation
│   ├── iosMain/kotlin/              # iOS-specific
│   │   └── com/augmentalis/voiceos/mylibrary/
│   │       └── MyLibraryIos.kt      # expect/actual implementation
│   ├── jvmMain/kotlin/              # Desktop-specific
│   │   └── com/augmentalis/voiceos/mylibrary/
│   │       └── MyLibraryJvm.kt      # expect/actual implementation
│   └── commonTest/kotlin/           # Shared tests
│       └── MyLibraryTest.kt
└── build.gradle.kts
```

---

## AvaUI DSL Runtime

### Overview

The AvaUI DSL Runtime interprets .vos files at runtime without code generation.

**Location**: `runtime/libraries/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/`

**7 Phases**:
1. Parser Foundation (Tokenizer, Parser, AST)
2. Component Registry (Metadata system)
3. Instantiation Engine (AST → Native objects)
4. Event/Callback System (DSL callbacks → Kotlin)
5. Voice Command Router (Fuzzy matching)
6. Lifecycle Management (6-state lifecycle)
7. Runtime Integration (AvaUIRuntime orchestrator)

### Quick Start

```kotlin
import com.augmentalis.voiceos.avaui.AvaUIRuntime

// 1. Create runtime instance
val runtime = AvaUIRuntime()

// 2. Load .vos app
val app = runtime.loadApp("""
    #!vos:D
    App {
      id: "com.example.myapp"
      name: "My App"

      ColorPicker {
        id: "picker1"
        initialColor: "#FF5722"
        mode: "DESIGNER"

        onConfirm: (color) => {
          Preferences.set("theme.primary", color)
          VoiceOS.speak("Color saved!")
        }
      }

      VoiceCommands {
        "change color" => "picker1.show"
        "reset" => "picker1.reset"
      }
    }
""")

// 3. Start app
val runningApp = runtime.start(app)

// 4. Handle voice commands
runtime.handleVoiceCommand(app.id, "change color")

// 5. Lifecycle management
runtime.pause(app.id)    // Pause app
runtime.resume(app.id)   // Resume app
runtime.stop(app.id)     // Stop app
```

### Phase 1: Parser Foundation

#### VosTokenizer

Converts DSL text into tokens.

**File**: `dsl/VosTokenizer.kt`

```kotlin
val tokenizer = VosTokenizer(dslSource)
val tokens = tokenizer.tokenize()

// Token types:
// - IDENTIFIER: App, ColorPicker, id, name
// - STRING: "com.example.app", "#FF5722"
// - NUMBER: 123, 45.67
// - LBRACE/RBRACE: { }
// - LPAREN/RPAREN: ( )
// - ARROW: =>
// - COLON: :
// - COMMA: ,
// - COMMENT: # or //
```

**Supported Escape Sequences**:
- `\n` - Newline
- `\t` - Tab
- `\r` - Carriage return
- `\\` - Backslash
- `\"` - Quote

#### VosParser

Builds AST from tokens using recursive descent parsing.

**File**: `dsl/VosParser.kt`

```kotlin
val parser = VosParser(tokens)
val ast = parser.parse()

// AST Node Types:
// - App: Root application node
// - Component: UI component (ColorPicker, Button, etc.)
// - VosValue: Property values (String, Int, Float, Boolean, Color, Array, Object)
// - VosLambda: Callback functions
// - VosStatement: Statements (FunctionCall, Assignment, IfStatement)
```

**Example AST**:
```kotlin
VosAstNode.App(
    id = "com.example.app",
    name = "My App",
    runtime = "AvaUI",
    components = listOf(
        VosAstNode.Component(
            type = "ColorPicker",
            id = "picker1",
            properties = mapOf(
                "initialColor" to VosValue.StringValue("#FF5722"),
                "mode" to VosValue.StringValue("DESIGNER")
            ),
            callbacks = mapOf(
                "onConfirm" to VosLambda(
                    parameters = listOf("color"),
                    body = listOf(...)
                )
            )
        )
    ),
    voiceCommands = mapOf("change color" to "picker1.show")
)
```

### Phase 2: Component Registry

Thread-safe registry mapping component names to metadata.

**File**: `registry/ComponentRegistry.kt`

```kotlin
val registry = ComponentRegistry.getInstance()

// Register custom component
registry.register(
    ComponentDescriptor(
        type = "CustomButton",
        displayName = "Custom Button",
        description = "A customizable button component",
        properties = mapOf(
            "text" to PropertyDescriptor(
                name = "text",
                type = PropertyType.STRING,
                defaultValue = "Click Me",
                required = false
            ),
            "enabled" to PropertyDescriptor(
                name = "enabled",
                type = PropertyType.BOOLEAN,
                defaultValue = true
            )
        ),
        callbacks = mapOf(
            "onClick" to CallbackDescriptor(
                name = "onClick",
                description = "Called when button is clicked",
                parameters = emptyList()
            )
        )
    )
)

// Retrieve component descriptor
val descriptor = registry.get("CustomButton")
```

**Built-in Components**:
- ColorPicker
- Preferences
- Text
- Button
- Container

### Phase 3: Instantiation Engine

Creates native Kotlin objects from AST nodes.

**File**: `instantiation/ComponentInstantiator.kt`

```kotlin
val instantiator = ComponentInstantiator(registry, propertyMapper, typeCoercion)

// Instantiate component from AST
val component: VosAstNode.Component = ...
val instance: Any = instantiator.instantiate(component)

// Example: ColorPicker instance
val colorPicker = instance as ColorPickerConfig
println(colorPicker.initialColor)  // ColorRGBA(255, 87, 34, 255)
println(colorPicker.mode)           // ColorPickerMode.DESIGNER
```

**Type Coercion** (`instantiation/TypeCoercion.kt`):

```kotlin
// String → ColorRGBA
"#FF5722" → ColorRGBA(255, 87, 34, 255)

// String → Enum
"DESIGNER" → ColorPickerMode.DESIGNER

// String → Int
"123" → 123

// String → Float
"45.67" → 45.67f

// String → Boolean
"true" → true
```

### Phase 4: Event/Callback System

Converts DSL lambdas to Kotlin callbacks with runtime execution.

**File**: `events/EventBus.kt`

```kotlin
val eventBus = EventBus()

// Emit event
eventBus.emit(ComponentEvent(
    componentId = "picker1",
    eventName = "onConfirm",
    parameters = mapOf("color" to ColorRGBA(255, 0, 0, 255))
))

// Observe events
eventBus.events.collect { event ->
    println("Event: ${event.eventName} from ${event.componentId}")
}
```

**Callback Adapter** (`events/CallbackAdapter.kt`):

```kotlin
val adapter = CallbackAdapter(eventBus, context)

// Convert DSL lambda to Kotlin callback
val lambda = VosLambda(
    parameters = listOf("color"),
    body = listOf(
        VosStatement.FunctionCall(
            receiver = "Preferences",
            functionName = "set",
            arguments = listOf(
                VosValue.StringValue("theme.primary"),
                VosValue.IdentifierValue("color")
            )
        )
    )
)

val callback: (Map<String, Any?>) -> Unit = adapter.createCallback(
    lambda = lambda,
    componentId = "picker1",
    eventName = "onConfirm"
)

// Invoke callback
callback(mapOf("color" to ColorRGBA(255, 0, 0, 255)))
```

**Supported Statements**:
- `FunctionCall`: `Preferences.set("key", value)`
- `Assignment`: `color = "#FF0000"`
- `IfStatement`: `if (enabled) { ... }`

### Phase 5: Voice Command Router

Fuzzy matching for voice commands.

**File**: `voice/VoiceCommandRouter.kt`

```kotlin
val router = VoiceCommandRouter()

// Register commands
router.registerCommand(VoiceCommand(
    trigger = "change color",
    action = "picker1.show"
))

router.registerCommand(VoiceCommand(
    trigger = "reset theme",
    action = "picker1.reset"
))

// Match voice input (exact match)
val match = router.match("change color")
println(match?.confidence)  // 1.0 (exact match)

// Match voice input (fuzzy match)
val fuzzyMatch = router.match("change colour")
println(fuzzyMatch?.confidence)  // 0.85 (similarity threshold: 0.7)

// No match (too different)
val noMatch = router.match("open settings")
println(noMatch)  // null
```

**Fuzzy Matching Algorithms** (`voice/CommandMatcher.kt`):

1. **Levenshtein Distance**: Character-level edit distance
2. **Word Overlap**: Shared words between input and trigger

```kotlin
// Levenshtein distance
CommandMatcher.levenshteinDistance("color", "colour")  // 1

// Similarity score
CommandMatcher.calculateSimilarity("change color", "change colour")  // 0.92
```

### Phase 6: Lifecycle Management

Android-style 6-state lifecycle.

**File**: `lifecycle/AppLifecycle.kt`

```kotlin
val lifecycle = AppLifecycle()

// State transitions
lifecycle.create()    // CREATED
lifecycle.start()     // STARTED
lifecycle.resume()    // RESUMED
lifecycle.pause()     // PAUSED
lifecycle.resume()    // RESUMED
lifecycle.pause()     // PAUSED
lifecycle.stop()      // STOPPED
lifecycle.destroy()   // DESTROYED

// Observe state changes
lifecycle.state.collect { state ->
    when (state) {
        LifecycleState.CREATED -> println("App created")
        LifecycleState.STARTED -> println("App started")
        LifecycleState.RESUMED -> println("App resumed")
        LifecycleState.PAUSED -> println("App paused")
        LifecycleState.STOPPED -> println("App stopped")
        LifecycleState.DESTROYED -> println("App destroyed")
    }
}

// Register lifecycle observer
lifecycle.addObserver(object : LifecycleObserver {
    override fun onCreate() {
        println("onCreate called")
    }

    override fun onDestroy() {
        println("onDestroy called - cleanup resources")
    }
})
```

**Resource Management** (`lifecycle/ResourceManager.kt`):

```kotlin
val resourceManager = ResourceManager()

// Register managed resource
resourceManager.register(object : ManagedResource {
    override val id = "database"

    override fun release() {
        println("Closing database connection")
    }
})

// Release all resources
resourceManager.releaseAll()
```

**State Persistence** (`lifecycle/StateManager.kt`):

```kotlin
val stateManager = StateManager()

// Save state
stateManager.saveState("app.id", mapOf(
    "selectedColor" to "#FF5722",
    "mode" to "DESIGNER"
))

// Restore state
val state = stateManager.restoreState("app.id")
println(state["selectedColor"])  // "#FF5722"
```

### Phase 7: Runtime Integration

Unified orchestration class.

**File**: `AvaUIRuntime.kt`

```kotlin
class AvaUIRuntime(
    private val registry: ComponentRegistry = ComponentRegistry.getInstance(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    fun loadApp(dslSource: String): VosAstNode.App {
        // Phase 1: Parse DSL
        val tokenizer = VosTokenizer(dslSource)
        val tokens = tokenizer.tokenize()
        val parser = VosParser(tokens)
        return parser.parse()
    }

    suspend fun start(app: VosAstNode.App): RunningApp {
        val lifecycle = AppLifecycle()
        lifecycle.create()

        // Phase 3: Instantiate components
        val instances = app.components.map { component ->
            component.id ?: component.type to instantiator.instantiate(component)
        }.toMap()

        // Phase 5: Register voice commands
        app.voiceCommands.forEach { (trigger, action) ->
            voiceRouter.registerCommand(VoiceCommand(trigger, action))
        }

        lifecycle.start()
        lifecycle.resume()

        return RunningApp(app, lifecycle, instances, voiceRouter)
    }

    suspend fun handleVoiceCommand(appId: String, voiceInput: String): Boolean {
        val runningApp = runningApps[appId] ?: return false
        val match = runningApp.voiceRouter.match(voiceInput) ?: return false
        actionDispatcher.dispatch(match, runningApp)
        return true
    }

    suspend fun pause(appId: String) {
        runningApps[appId]?.lifecycle?.pause()
    }

    suspend fun resume(appId: String) {
        runningApps[appId]?.lifecycle?.resume()
    }

    suspend fun stop(appId: String) {
        runningApps[appId]?.let { app ->
            app.lifecycle.stop()
            app.lifecycle.destroy()
            runningApps.remove(appId)
        }
    }
}
```

---

## Theme System

### Overview

Unified theme system with 6 import/export formats:

| Format | Import | Export | Platform |
|--------|--------|--------|----------|
| **YAML** | YamlThemeLoader | YamlThemeSerializer | commonMain |
| **JSON** | JsonThemeLoader | JsonThemeSerializer | commonMain |
| **Jetpack Compose** | ComposeThemeImporter | ComposeThemeExporter | androidMain |
| **Android XML** | XmlThemeImporter | XmlThemeExporter | androidMain |
| **Avanue4 Legacy** | ThemeConverter | ThemeConverter | ThemeBridge |
| **iOS** | Pending | Pending | iosMain |

### Core Theme Model

**File**: `theme/ThemeConfig.kt`

```kotlin
data class ThemeConfig(
    val name: String,
    val palette: ThemePalette,
    val typography: ThemeTypography,
    val spacing: ThemeSpacing,
    val effects: ThemeEffects
)

data class ThemePalette(
    val primary: String,           // "#007AFF"
    val secondary: String,         // "#5AC8FA"
    val background: String,        // "#000000"
    val surface: String,           // "#1C1C1E"
    val error: String,             // "#FF3B30"
    val onPrimary: String,         // "#FFFFFF"
    val onSecondary: String,       // "#FFFFFF"
    val onBackground: String,      // "#FFFFFF"
    val onSurface: String,         // "#FFFFFF"
    val onError: String            // "#FFFFFF"
)

data class ThemeTypography(
    val h1: TextStyle,
    val h2: TextStyle,
    val body1: TextStyle,
    val body2: TextStyle,
    val button: TextStyle,
    val caption: TextStyle
)

data class TextStyle(
    val size: Float,
    val weight: String,
    val fontFamily: String?
)

data class ThemeSpacing(
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 16f,
    val lg: Float = 24f,
    val xl: Float = 32f
)

data class ThemeEffects(
    val cornerRadius: Float = 8f,
    val elevation: Float = 4f,
    val shadowOpacity: Float = 0.2f
)
```

### YAML Theme Loader

**File**: `theme/loaders/YamlThemeLoader.kt`

```kotlin
import com.augmentalis.voiceos.avaui.theme.*
import net.mamoe.yamlkt.Yaml

object YamlThemeLoader {
    fun load(yamlString: String): ThemeConfig {
        val yaml = Yaml { ignoreUnknownKeys = true }
        val yamlMap = yaml.decodeYamlFromString(yamlString) as Map<String, Any?>

        return ThemeConfig(
            name = yamlMap["name"] as? String ?: "Unnamed Theme",
            palette = loadPalette(yamlMap["palette"] as? Map<String, Any?>),
            typography = loadTypography(yamlMap["typography"] as? Map<String, Any?>),
            spacing = loadSpacing(yamlMap["spacing"] as? Map<String, Any?>),
            effects = loadEffects(yamlMap["effects"] as? Map<String, Any?>)
        )
    }
}

// Example YAML theme:
"""
name: "Dark Theme"
palette:
  primary: "#007AFF"
  secondary: "#5AC8FA"
  background: "#000000"
  surface: "#1C1C1E"
typography:
  h1:
    size: 28
    weight: "bold"
    fontFamily: "Roboto"
"""
```

### JSON Theme Loader

**File**: `theme/loaders/JsonThemeLoader.kt`

```kotlin
import kotlinx.serialization.json.Json

object JsonThemeLoader {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    fun load(jsonString: String): ThemeConfig {
        return json.decodeFromString(ThemeConfig.serializer(), jsonString)
    }
}

// Example JSON theme:
"""
{
  "name": "Dark Theme",
  "palette": {
    "primary": "#007AFF",
    "secondary": "#5AC8FA",
    "background": "#000000",
    "surface": "#1C1C1E"
  },
  "typography": {
    "h1": {
      "size": 28.0,
      "weight": "bold",
      "fontFamily": "Roboto"
    }
  }
}
"""
```

### Jetpack Compose Theme Import/Export

**File**: `theme/loaders/ComposeThemeImporter.kt` (androidMain)

```kotlin
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.toArgb
import com.augmentalis.voiceos.avaui.theme.*

object ComposeThemeImporter {
    fun import(
        colorScheme: ColorScheme,
        typography: Typography,
        name: String = "Imported from Compose"
    ): ThemeConfig {
        val colorUtils = ColorConversionUtils()

        return ThemeConfig(
            name = name,
            palette = ThemePalette(
                primary = colorUtils.intToHex(colorScheme.primary.toArgb(), includeAlpha = false),
                secondary = colorUtils.intToHex(colorScheme.secondary.toArgb(), includeAlpha = false),
                background = colorUtils.intToHex(colorScheme.background.toArgb(), includeAlpha = false),
                surface = colorUtils.intToHex(colorScheme.surface.toArgb(), includeAlpha = false),
                error = colorUtils.intToHex(colorScheme.error.toArgb(), includeAlpha = false),
                onPrimary = colorUtils.intToHex(colorScheme.onPrimary.toArgb(), includeAlpha = false),
                onSecondary = colorUtils.intToHex(colorScheme.onSecondary.toArgb(), includeAlpha = false),
                onBackground = colorUtils.intToHex(colorScheme.onBackground.toArgb(), includeAlpha = false),
                onSurface = colorUtils.intToHex(colorScheme.onSurface.toArgb(), includeAlpha = false),
                onError = colorUtils.intToHex(colorScheme.onError.toArgb(), includeAlpha = false)
            ),
            typography = importTypography(typography)
        )
    }
}

// Usage:
val composeTheme = MaterialTheme.colorScheme
val magicTheme = ComposeThemeImporter.import(composeTheme, MaterialTheme.typography, "My Theme")
```

**File**: `theme/loaders/ComposeThemeExporter.kt` (androidMain)

```kotlin
import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color

object ComposeThemeExporter {
    fun export(theme: ThemeConfig): ColorScheme {
        val colorUtils = ColorConversionUtils()

        return darkColorScheme(
            primary = Color(colorUtils.hexToInt(theme.palette.primary)),
            secondary = Color(colorUtils.hexToInt(theme.palette.secondary)),
            background = Color(colorUtils.hexToInt(theme.palette.background)),
            surface = Color(colorUtils.hexToInt(theme.palette.surface)),
            error = Color(colorUtils.hexToInt(theme.palette.error)),
            onPrimary = Color(colorUtils.hexToInt(theme.palette.onPrimary)),
            onSecondary = Color(colorUtils.hexToInt(theme.palette.onSecondary)),
            onBackground = Color(colorUtils.hexToInt(theme.palette.onBackground)),
            onSurface = Color(colorUtils.hexToInt(theme.palette.onSurface)),
            onError = Color(colorUtils.hexToInt(theme.palette.onError))
        )
    }
}

// Usage:
val magicTheme = ThemeConfig(...)
val composeColorScheme = ComposeThemeExporter.export(magicTheme)

MaterialTheme(colorScheme = composeColorScheme) {
    // Your compose UI
}
```

### Android XML Theme Import/Export

**File**: `theme/loaders/XmlThemeImporter.kt` (androidMain)

```kotlin
import android.content.Context

class XmlThemeImporter(private val context: Context) {

    fun import(styleResId: Int, name: String): ThemeConfig {
        val attrs = intArrayOf(
            android.R.attr.colorPrimary,
            android.R.attr.colorSecondary,
            android.R.attr.colorBackground,
            // ... other attrs
        )

        val typedArray = context.theme.obtainStyledAttributes(styleResId, attrs)

        try {
            val colorUtils = ColorConversionUtils()
            return ThemeConfig(
                name = name,
                palette = ThemePalette(
                    primary = colorUtils.intToHex(
                        typedArray.getColor(0, 0xFF007AFF.toInt()),
                        includeAlpha = false
                    ),
                    // ... other colors
                )
            )
        } finally {
            typedArray.recycle()
        }
    }

    fun importFromColors(name: String): ThemeConfig {
        return ThemeConfig(
            name = name,
            palette = ThemePalette(
                primary = getColorHex("colorPrimary", "#007AFF"),
                secondary = getColorHex("colorSecondary", "#5AC8FA"),
                background = getColorHex("colorBackground", "#000000"),
                // ... other colors
            )
        )
    }

    private fun getColorHex(resourceName: String, defaultHex: String): String {
        val resourceId = context.resources.getIdentifier(resourceName, "color", context.packageName)
        return if (resourceId != 0) {
            val color = context.getColor(resourceId)
            ColorConversionUtils().intToHex(color, includeAlpha = false)
        } else {
            defaultHex
        }
    }
}
```

**File**: `theme/loaders/XmlThemeExporter.kt` (androidMain)

```kotlin
object XmlThemeExporter {

    fun exportColors(theme: ThemeConfig): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <color name="colorPrimary">${theme.palette.primary}</color>
                <color name="colorSecondary">${theme.palette.secondary}</color>
                <color name="colorBackground">${theme.palette.background}</color>
                <color name="colorSurface">${theme.palette.surface}</color>
                <color name="colorError">${theme.palette.error}</color>
                <color name="colorOnPrimary">${theme.palette.onPrimary}</color>
                <color name="colorOnSecondary">${theme.palette.onSecondary}</color>
                <color name="colorOnBackground">${theme.palette.onBackground}</color>
                <color name="colorOnSurface">${theme.palette.onSurface}</color>
                <color name="colorOnError">${theme.palette.onError}</color>
            </resources>
        """.trimIndent()
    }

    fun exportDimens(theme: ThemeConfig): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <dimen name="spacing_xs">${theme.spacing.xs}dp</dimen>
                <dimen name="spacing_sm">${theme.spacing.sm}dp</dimen>
                <dimen name="spacing_md">${theme.spacing.md}dp</dimen>
                <dimen name="spacing_lg">${theme.spacing.lg}dp</dimen>
                <dimen name="spacing_xl">${theme.spacing.xl}dp</dimen>
                <dimen name="corner_radius">${theme.effects.cornerRadius}dp</dimen>
                <dimen name="elevation">${theme.effects.elevation}dp</dimen>
            </resources>
        """.trimIndent()
    }

    fun exportStyles(theme: ThemeConfig): String {
        return """
            <?xml version="1.0" encoding="utf-8"?>
            <resources>
                <style name="${theme.name}" parent="Theme.MaterialComponents.DayNight">
                    <item name="colorPrimary">@color/colorPrimary</item>
                    <item name="colorSecondary">@color/colorSecondary</item>
                    <item name="android:colorBackground">@color/colorBackground</item>
                </style>
            </resources>
        """.trimIndent()
    }
}

// Usage:
val colorsXml = XmlThemeExporter.exportColors(theme)
val dimensXml = XmlThemeExporter.exportDimens(theme)
val stylesXml = XmlThemeExporter.exportStyles(theme)

// Write to res/values/
File("res/values/colors.xml").writeText(colorsXml)
File("res/values/dimens.xml").writeText(dimensXml)
File("res/values/styles.xml").writeText(stylesXml)
```

### Theme Migration Bridge (Avanue4 ↔ AvaUI)

**Location**: `runtime/libraries/ThemeBridge/src/commonMain/kotlin/com/augmentalis/voiceos/themebridge/`

**File**: `ThemeMigrationBridge.kt`

```kotlin
class ThemeMigrationBridge(
    private val legacyThemeManager: LegacyThemeManager,
    private val enableBidirectionalSync: Boolean = true,
    private val converter: ThemeConverter = ThemeConverter(),
    private val mapper: ThemeStructureMapper = ThemeStructureMapper()
) : LegacyThemeObserver {

    private val _magicUiTheme = MutableStateFlow<ThemeConfig?>(null)
    val magicUiTheme: StateFlow<ThemeConfig?> = _magicUiTheme.asStateFlow()

    private var isSyncingToLegacy = false
    private var isSyncingFromLegacy = false

    fun initialize() {
        legacyThemeManager.addObserver(this)

        // Initial sync
        legacyThemeManager.getCurrentTheme()?.let { legacyTheme ->
            val magicTheme = converter.convertLegacyToAvaUI(legacyTheme)
            _magicUiTheme.value = magicTheme
        }
    }

    fun updateMagicUiTheme(theme: ThemeConfig) {
        _magicUiTheme.value = theme

        if (enableBidirectionalSync && !isSyncingFromLegacy) {
            try {
                isSyncingToLegacy = true
                val legacyTheme = converter.convertAvaUIToLegacy(theme)
                legacyThemeManager.applyTheme(legacyTheme)
            } finally {
                isSyncingToLegacy = false
            }
        }
    }

    override fun onThemeChanged(theme: LegacyTheme) {
        if (isSyncingToLegacy) return  // Loop prevention

        try {
            isSyncingFromLegacy = true
            val magicTheme = converter.convertLegacyToAvaUI(theme)
            _magicUiTheme.value = magicTheme
        } finally {
            isSyncingFromLegacy = false
        }
    }

    fun updateComponent(component: ThemeComponent, value: Any) {
        _magicUiTheme.value?.let { currentTheme ->
            val updatedTheme = mapper.updateComponent(currentTheme, component, value)
            updateMagicUiTheme(updatedTheme)
        }
    }
}

// Usage:
val bridge = ThemeMigrationBridge(legacyThemeManager)
bridge.initialize()

// Observe AvaUI theme
bridge.magicUiTheme.collect { theme ->
    println("AvaUI theme updated: ${theme?.name}")
}

// Update AvaUI theme (syncs to legacy)
val newTheme = ThemeConfig(...)
bridge.updateMagicUiTheme(newTheme)

// Update specific component
bridge.updateComponent(ThemeComponent.PRIMARY_COLOR, 0xFF007AFF.toInt())
```

---

## Component Development

### Creating a Custom Component

**Step 1**: Define component configuration (commonMain)

```kotlin
// MyCustomComponent.kt
package com.augmentalis.voiceos.mycustomcomponent

data class MyCustomComponentConfig(
    val text: String = "Default Text",
    val color: ColorRGBA = ColorRGBA(0, 122, 255, 255),
    val enabled: Boolean = true,
    val onClickCallback: ((Map<String, Any?>) -> Unit)? = null
)
```

**Step 2**: Register component with AvaUI Runtime

```kotlin
// MyCustomComponentDescriptor.kt
import com.augmentalis.voiceos.avaui.registry.*

fun registerMyCustomComponent() {
    val registry = ComponentRegistry.getInstance()

    registry.register(
        ComponentDescriptor(
            type = "MyCustomComponent",
            displayName = "My Custom Component",
            description = "A custom component for demonstration",
            properties = mapOf(
                "text" to PropertyDescriptor(
                    name = "text",
                    type = PropertyType.STRING,
                    defaultValue = "Default Text",
                    required = false
                ),
                "color" to PropertyDescriptor(
                    name = "color",
                    type = PropertyType.COLOR,
                    defaultValue = "#007AFF",
                    required = false
                ),
                "enabled" to PropertyDescriptor(
                    name = "enabled",
                    type = PropertyType.BOOLEAN,
                    defaultValue = true,
                    required = false
                )
            ),
            callbacks = mapOf(
                "onClick" to CallbackDescriptor(
                    name = "onClick",
                    description = "Called when component is clicked",
                    parameters = emptyList()
                )
            ),
            factory = { properties, callbacks ->
                MyCustomComponentConfig(
                    text = properties["text"] as? String ?: "Default Text",
                    color = properties["color"] as? ColorRGBA ?: ColorRGBA(0, 122, 255, 255),
                    enabled = properties["enabled"] as? Boolean ?: true,
                    onClickCallback = callbacks["onClick"]
                )
            }
        )
    )
}
```

**Step 3**: Implement platform-specific UI (androidMain)

```kotlin
// MyCustomComponentAndroid.kt (androidMain)
package com.augmentalis.voiceos.mycustomcomponent

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun MyCustomComponentView(config: MyCustomComponentConfig) {
    Button(
        onClick = {
            config.onClickCallback?.invoke(emptyMap())
        },
        enabled = config.enabled
    ) {
        Text(
            text = config.text,
            color = Color(
                config.color.red,
                config.color.green,
                config.color.blue,
                config.color.alpha
            )
        )
    }
}
```

**Step 4**: Use in .vos DSL

```
#!vos:D
App {
  id: "com.example.customapp"
  name: "Custom Component Demo"

  MyCustomComponent {
    id: "customButton"
    text: "Click Me!"
    color: "#FF5722"
    enabled: true

    onClick: () => {
      VoiceOS.speak("Button clicked!")
    }
  }
}
```

---

## Library Development

### Library Structure (KMP)

```
runtime/libraries/MyLibrary/
├── build.gradle.kts
├── src/
│   ├── commonMain/kotlin/
│   │   └── com/augmentalis/voiceos/mylibrary/
│   │       ├── MyLibrary.kt              # Main API
│   │       ├── models/                   # Data models
│   │       └── internal/                 # Internal implementation
│   ├── androidMain/kotlin/
│   │   └── com/augmentalis/voiceos/mylibrary/
│   │       └── MyLibraryAndroid.kt       # Android expect/actual
│   ├── iosMain/kotlin/
│   │   └── com/augmentalis/voiceos/mylibrary/
│   │       └── MyLibraryIos.kt           # iOS expect/actual
│   ├── jvmMain/kotlin/
│   │   └── com/augmentalis/voiceos/mylibrary/
│   │       └── MyLibraryJvm.kt           # Desktop expect/actual
│   └── commonTest/kotlin/
│       └── MyLibraryTest.kt              # Shared tests
└── README.md
```

### build.gradle.kts Template

```kotlin
plugins {
    kotlin("multiplatform")
    id("com.android.library")
    kotlin("plugin.serialization")
}

kotlin {
    androidTarget()

    ios()
    iosSimulatorArm64()

    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":runtime:libraries:ColorPicker"))
                implementation(project(":runtime:libraries:Preferences"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.test)
            }
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.compose.runtime)
            }
        }

        val iosMain by getting {
            dependencies {
                // iOS-specific dependencies
            }
        }

        val jvmMain by getting {
            dependencies {
                // Desktop-specific dependencies
            }
        }
    }
}

android {
    namespace = "com.augmentalis.voiceos.mylibrary"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
}
```

### expect/actual Pattern

**commonMain** (API definition):

```kotlin
// MyLibrary.kt (commonMain)
package com.augmentalis.voiceos.mylibrary

expect class MyLibrary() {
    fun doSomething(): String
    suspend fun doSomethingAsync(): Result<String>
}
```

**androidMain** (Android implementation):

```kotlin
// MyLibraryAndroid.kt (androidMain)
package com.augmentalis.voiceos.mylibrary

import android.content.Context

actual class MyLibrary {
    actual fun doSomething(): String {
        return "Android implementation"
    }

    actual suspend fun doSomethingAsync(): Result<String> {
        return withContext(Dispatchers.IO) {
            Result.success("Android async result")
        }
    }
}
```

**iosMain** (iOS implementation):

```kotlin
// MyLibraryIos.kt (iosMain)
package com.augmentalis.voiceos.mylibrary

import platform.Foundation.NSLog

actual class MyLibrary {
    actual fun doSomething(): String {
        NSLog("iOS implementation")
        return "iOS implementation"
    }

    actual suspend fun doSomethingAsync(): Result<String> {
        return withContext(Dispatchers.Default) {
            Result.success("iOS async result")
        }
    }
}
```

**jvmMain** (Desktop implementation):

```kotlin
// MyLibraryJvm.kt (jvmMain)
package com.augmentalis.voiceos.mylibrary

actual class MyLibrary {
    actual fun doSomething(): String {
        println("JVM implementation")
        return "JVM implementation"
    }

    actual suspend fun doSomethingAsync(): Result<String> {
        return withContext(Dispatchers.IO) {
            Result.success("JVM async result")
        }
    }
}
```

---

## Testing

### Unit Testing (commonTest)

```kotlin
// MyLibraryTest.kt (commonTest)
import kotlin.test.*

class MyLibraryTest {

    private lateinit var library: MyLibrary

    @BeforeTest
    fun setup() {
        library = MyLibrary()
    }

    @Test
    fun testDoSomething() {
        val result = library.doSomething()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun testDoSomethingAsync() = runTest {
        val result = library.doSomethingAsync()
        assertTrue(result.isSuccess)
        assertEquals("Expected value", result.getOrNull())
    }
}
```

### Running Tests

```bash
# All platforms
./gradlew test

# Android only
./gradlew :runtime:libraries:MyLibrary:testDebugUnitTest

# iOS simulator
./gradlew :runtime:libraries:MyLibrary:iosSimulatorArm64Test

# JVM/Desktop
./gradlew :runtime:libraries:MyLibrary:jvmTest
```

---

## Deployment

### Building for Production

```bash
# Build all platforms
./gradlew build

# Build Android AAR
./gradlew :runtime:libraries:MyLibrary:assembleRelease

# Build iOS framework
./gradlew :runtime:libraries:MyLibrary:linkReleaseFrameworkIos

# Build JVM JAR
./gradlew :runtime:libraries:MyLibrary:jvmJar
```

### Publishing to Maven

```kotlin
// build.gradle.kts
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.augmentalis.voiceos"
            artifactId = "mylibrary"
            version = "1.0.0"

            from(components["release"])
        }
    }

    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/augmentalis/voiceos")
            credentials {
                username = System.getenv("GITHUB_USERNAME")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
```

---

## API Reference

### AvaUIRuntime

```kotlin
class AvaUIRuntime {
    fun loadApp(dslSource: String): VosAstNode.App
    suspend fun start(app: VosAstNode.App): RunningApp
    suspend fun pause(appId: String)
    suspend fun resume(appId: String)
    suspend fun stop(appId: String)
    suspend fun handleVoiceCommand(appId: String, voiceInput: String): Boolean
}
```

### ComponentRegistry

```kotlin
class ComponentRegistry {
    suspend fun register(descriptor: ComponentDescriptor)
    suspend fun get(type: String): ComponentDescriptor?
    suspend fun list(): List<ComponentDescriptor>
}
```

### ThemeMigrationBridge

```kotlin
class ThemeMigrationBridge {
    fun initialize()
    fun updateMagicUiTheme(theme: ThemeConfig)
    fun updateComponent(component: ThemeComponent, value: Any)
    val magicUiTheme: StateFlow<ThemeConfig?>
}
```

### Theme Loaders

```kotlin
object YamlThemeLoader {
    fun load(yamlString: String): ThemeConfig
}

object JsonThemeLoader {
    fun load(jsonString: String): ThemeConfig
}

object ComposeThemeImporter {
    fun import(colorScheme: ColorScheme, typography: Typography, name: String): ThemeConfig
}

class XmlThemeImporter(context: Context) {
    fun import(styleResId: Int, name: String): ThemeConfig
    fun importFromColors(name: String): ThemeConfig
}
```

---

## Best Practices

### 1. Follow IDEACODE Principles

- **Spec-Driven Development**: Document before implementation
- **User Story Prioritization**: Focus on P1 infrastructure first
- **Modular Architecture**: Keep components independent
- **Configuration Over Code**: Prefer DSL definitions
- **Documentation-First**: Write KDoc for all public APIs

### 2. Cross-Platform Development

- **Minimize platform-specific code**: Keep commonMain as comprehensive as possible
- **Use expect/actual sparingly**: Only for truly platform-specific logic
- **Test on all platforms**: Don't assume cross-platform parity

### 3. DSL Component Design

- **Keep properties simple**: Use primitives and serializable types
- **Provide sensible defaults**: All properties should be optional
- **Document callbacks clearly**: Specify parameter types and return values
- **Use consistent naming**: Follow component naming conventions

### 4. Theme System

- **Use hex strings for colors**: `"#RRGGBB"` or `"#RRGGBBAA"` format
- **Validate theme structure**: Use ThemeValidator before applying
- **Preserve alpha channel**: Use lossless color conversion (ColorRGBA)
- **Support bidirectional sync**: Enable seamless legacy migration

### 5. Performance

- **Minimize re-composition**: Use remember and derivedStateOf in Compose
- **Batch event emissions**: Use debounce for rapid state changes
- **Cache parsed DSL**: Don't re-parse identical DSL sources
- **Release resources properly**: Implement lifecycle observers

### 6. Error Handling

- **Use Result types**: `Result<T>` for operations that can fail
- **Log errors verbosely**: Include context for debugging
- **Provide fallbacks**: Gracefully degrade functionality
- **Validate DSL at parse time**: Catch errors early

---

## Appendix

### File Paths Reference

| Component | Location |
|-----------|----------|
| AvaUI Runtime | `runtime/libraries/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/` |
| Theme Bridge | `runtime/libraries/ThemeBridge/src/commonMain/kotlin/com/augmentalis/voiceos/themebridge/` |
| ColorPicker | `runtime/libraries/ColorPicker/src/commonMain/kotlin/com/augmentalis/voiceos/colorpicker/` |
| Preferences | `runtime/libraries/Preferences/src/commonMain/kotlin/com/augmentalis/voiceos/preferences/` |
| Documentation | `/Volumes/M Drive/Coding/Avanues/docs/Active/` |

### Additional Resources

- **.vos File Format Specification**: `/docs/Active/VOS-File-Format-Specification-251027-1300.md`
- **Theme Architecture**: `/docs/Active/Theme-Systems-Architecture-Clarification-251027-1245.md`
- **Infrastructure Summary**: `/docs/Active/Infrastructure-Complete-Summary-251027-1305.md`
- **Session Context**: `/.claude/session_context.md`

---

**Document Version**: 3.1.0
**Last Updated**: 2025-10-27
**Maintained by**: Manoj Jhawar, manoj@ideahq.net
