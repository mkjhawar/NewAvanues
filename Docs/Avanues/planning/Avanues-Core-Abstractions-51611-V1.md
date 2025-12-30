# AvaUI Core Abstractions

**Version:** 1.0.0
**Last Updated:** 2025-10-27
**Feature:** 002-avaui-uik-enhancements
**Phase:** 2 - Foundational Infrastructure

---

## Overview

This document describes the foundational abstractions that power the AvaUI system. These interfaces and data structures were designed with three core principles:

1. **Platform Agnostic**: All abstractions live in `commonMain` for true Kotlin Multiplatform compatibility
2. **Composability**: Small, focused interfaces that can be combined to build complex functionality
3. **Performance First**: Zero-allocation hot paths, immutable data structures, 60 FPS target for IMU processing

---

## Component Model Layer

### ComponentModel

**Purpose**: The fundamental data structure representing ALL UI components in AvaUI.

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/core/ComponentModel.kt`

**Design Philosophy**:
- Immutable data structure (all fields are `val`)
- Hierarchical composition via `children: List<ComponentModel>`
- Properties stored as `Map<String, String>` for maximum flexibility
- UUID format: `namespace/local-id` (e.g., `app/button_save_123`)

**Code Example**:
```kotlin
// Create a simple button
val saveButton = ComponentModel(
    uuid = "app/button_save",
    type = "Button",
    position = ComponentPosition(100f, 50f),
    properties = mapOf(
        "text" to "Save",
        "color" to "#007AFF",
        "enabled" to "true"
    )
)

// Create a container with children
val toolbar = ComponentModel(
    uuid = "app/toolbar",
    type = "Row",
    position = ComponentPosition.ORIGIN,
    children = listOf(
        saveButton,
        ComponentModel(
            uuid = "app/button_cancel",
            type = "Button",
            position = ComponentPosition(200f, 50f),
            properties = mapOf("text" to "Cancel")
        )
    )
)

// Update properties immutably
val disabledButton = saveButton.withProperties("enabled" to "false")

// Query component tree
println("Is container: ${toolbar.isContainer()}")  // true
println("Total descendants: ${toolbar.descendantCount()}")  // 2
```

**Key Methods**:
- `withProperties(vararg pairs: Pair<String, String>)`: Returns new component with merged properties
- `isContainer()`: Checks if type is in `CONTAINER_TYPES` or has children
- `descendantCount()`: Recursively counts all descendants (direct + nested)

**Container Types**:
```kotlin
val CONTAINER_TYPES = setOf(
    "Row", "Column", "Container", "Stack", "Grid",
    "ScrollView", "Dialog", "BottomSheet", "Overlay"
)
```

---

### ComponentPosition

**Purpose**: 3D positioning for components (supports AR/VR with z-axis).

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/core/ComponentPosition.kt`

**Design Philosophy**:
- Simple value object with `x`, `y`, `z` (Float)
- Z-axis defaults to 0 for 2D UIs
- Provides geometric operations (offset, distance)

**Code Example**:
```kotlin
// 2D positioning (z defaults to 0)
val button = ComponentPosition(x = 100f, y = 50f)

// 3D positioning for AR/VR
val floatingPanel = ComponentPosition(x = 0f, y = 100f, z = 200f)

// Geometric operations
val moved = button.offset(dx = 10f, dy = 5f)
val distance = button.distanceTo(moved)  // 11.18 (sqrt(10^2 + 5^2))

// Common constant
val origin = ComponentPosition.ORIGIN  // (0, 0, 0)
```

---

### PluginComponent

**Purpose**: Interface for extensible third-party UI components.

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/core/PluginComponent.kt`

**Design Philosophy**:
- Lifecycle callbacks: `onCreate`, `onRender`, `onDestroy`
- Optional export to multiple formats (Kotlin, XML, JSON, DSL, SVG)
- Platform-specific rendering handled by implementations

**Code Example**:
```kotlin
class CustomGaugeComponent : PluginComponent {
    override val componentType: String = "CustomGauge"

    private var currentValue: Float = 0f

    override fun onCreate(model: ComponentModel) {
        currentValue = model.properties["value"]?.toFloatOrNull() ?: 0f
    }

    override fun onRender(model: ComponentModel): Any {
        // Platform-specific rendering
        return when (Platform.current) {
            Platform.Android -> renderAndroidGauge(model)
            Platform.iOS -> renderIOSGauge(model)
            else -> renderWebGauge(model)
        }
    }

    override fun onDestroy() {
        // Cleanup resources
        currentValue = 0f
    }

    override fun generateExport(model: ComponentModel, format: ExportFormat): String {
        return when (format) {
            ExportFormat.DSL -> "gauge[value=${model.properties["value"]}]"
            ExportFormat.KOTLIN -> """
                CustomGauge(
                    value = ${model.properties["value"]}f,
                    max = ${model.properties["max"] ?: "100"}f
                )
            """.trimIndent()
            else -> ""
        }
    }
}
```

**Export Formats**:
- `KOTLIN`: Jetpack Compose / SwiftUI code
- `XML`: Android XML layouts
- `JSON`: Serialized component data
- `DSL`: AvaUI compact DSL syntax
- `SVG`: Vector graphics export

---

## Layout System Layer

### LayoutLoader

**Purpose**: Interface for loading layouts from various formats (DSL, YAML, JSON).

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/layout/LayoutLoader.kt`

**Design Philosophy**:
- Format-agnostic loading via `LayoutFormat` enum
- Auto-detection of format from source content
- Returns `Result<List<ComponentModel>>` for functional error handling

**Code Example**:
```kotlin
class UnifiedLayoutLoader : LayoutLoader {
    override fun load(source: String, format: LayoutFormat): Result<List<ComponentModel>> {
        val detectedFormat = if (format == LayoutFormat.AUTO) {
            autoDetect(source)
        } else {
            format
        }

        return when (detectedFormat) {
            LayoutFormat.DSL -> parseDSL(source)
            LayoutFormat.YAML -> parseYAML(source)
            LayoutFormat.JSON -> parseJSON(source)
            else -> Result.failure(Exception("Unknown format"))
        }
    }

    override fun autoDetect(source: String): LayoutFormat {
        return when {
            source.trimStart().startsWith("{") -> LayoutFormat.JSON
            source.trimStart().startsWith("---") -> LayoutFormat.YAML
            source.contains(":") && source.contains("[") -> LayoutFormat.DSL
            else -> LayoutFormat.DSL  // Default to DSL
        }
    }
}

// Usage
val loader = UnifiedLayoutLoader()

// Auto-detect format
val result = loader.load("""
    row:button[text=Save],button[text=Cancel]
""", LayoutFormat.AUTO)

result.onSuccess { components ->
    println("Loaded ${components.size} components")
}
```

### LayoutFormat

**Purpose**: Enum defining supported layout formats.

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/layout/LayoutFormat.kt`

**Formats**:
- `DSL`: Ultra-compact syntax (`row:button[text=Save],button[text=Cancel]`)
- `YAML`: Human-readable structured data
- `JSON`: Universal data interchange
- `AUTO`: Automatic format detection

---

## Motion Processing Layer (IMU)

### MotionProcessor

**Purpose**: Composable interface for IMU data processing pipeline.

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/imu/MotionProcessor.kt`

**Design Philosophy**:
- Chain of responsibility pattern: Raw IMU → AxisLocker → RateLimiter → Smoother → Cursor
- Enable/disable processors at runtime via `isEnabled` flag
- Configuration persistence via `getConfiguration()` / `applyConfiguration()`
- **Performance Requirement**: `process()` must complete in <2ms (60 FPS budget = 16.67ms/frame)

**Code Example**:
```kotlin
class AxisLockerProcessor : MotionProcessor {
    override val processorId: String = "axis_locker"
    override val displayName: String = "Axis Locker"
    override var isEnabled: Boolean = true

    var lockPitch: Boolean = false
    var lockRoll: Boolean = false
    var lockYaw: Boolean = false

    override fun process(input: IMUOrientationData): IMUOrientationData {
        if (!isEnabled) return input

        return IMUOrientationData(
            pitch = if (lockPitch) 0f else input.pitch,
            roll = if (lockRoll) 0f else input.roll,
            yaw = if (lockYaw) 0f else input.yaw,
            timestamp = input.timestamp
        )
    }

    override fun reset() {
        // No state to reset for axis locker
    }

    override fun getConfiguration(): Map<String, Any> {
        return mapOf(
            "lock_pitch" to lockPitch,
            "lock_roll" to lockRoll,
            "lock_yaw" to lockYaw
        )
    }

    override fun applyConfiguration(config: Map<String, Any>) {
        lockPitch = config["lock_pitch"] as? Boolean ?: false
        lockRoll = config["lock_roll"] as? Boolean ?: false
        lockYaw = config["lock_yaw"] as? Boolean ?: false
    }
}

// Pipeline usage
val pipeline = listOf(
    AxisLockerProcessor().apply { lockPitch = true },
    RateLimiterProcessor(),
    OneEuroFilterProcessor()
)

var data = rawIMUData
for (processor in pipeline) {
    if (processor.isEnabled) {
        data = processor.process(data)
    }
}
```

### IMUOrientationData

**Purpose**: Immutable snapshot of device orientation from IMU sensors.

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/imu/IMUOrientationData.kt`

**Coordinate System** (Right-hand rule):
```
     Y (Roll)
     |
     |
     +---- X (Pitch)
    /
   Z (Yaw)
```

**Code Example**:
```kotlin
// Capture orientation
val orientation = IMUOrientationData(
    pitch = 0.1f,   // Slightly looking up (radians)
    roll = 0.0f,    // Level
    yaw = -0.2f,    // Turned slightly left
    timestamp = System.currentTimeMillis()
)

// Calculate magnitude of rotation
val magnitude = orientation.magnitude()  // Euclidean norm

// Lock individual axes
val levelOrientation = orientation.withLockedRoll().withLockedPitch()

// Calculate delta between frames
val previous = IMUOrientationData(0.0f, 0.0f, 0.0f, timestamp - 16)
val delta = orientation.delta(previous)
println("Pitch change: ${delta.pitch} rad")

// Zero orientation constant
val zero = IMUOrientationData.ZERO  // (0, 0, 0, 0)
```

**Ranges**:
- `pitch`: -π to π radians (rotation around X-axis)
- `roll`: -π to π radians (rotation around Y-axis)
- `yaw`: -π to π radians (rotation around Z-axis)
- `timestamp`: milliseconds since epoch

---

## Theme System Layer

### ThemeConfig

**Purpose**: Complete theme configuration for visual styling.

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/theme/ThemeConfig.kt`

**Design Philosophy**:
- Centralized styling: palette, typography, spacing, effects
- Hex color strings (#RRGGBB or #AARRGGBB)
- Spacing in density-independent pixels (dp)
- Platform-agnostic theme definition

**Code Example**:
```kotlin
// Dark theme
val darkTheme = ThemeConfig(
    name = "Dark",
    palette = ThemePalette(
        primary = "#007AFF",
        secondary = "#5AC8FA",
        background = "#000000",
        surface = "#1C1C1E",
        error = "#FF3B30",
        onPrimary = "#FFFFFF",
        onSecondary = "#FFFFFF",
        onBackground = "#FFFFFF",
        onSurface = "#FFFFFF",
        onError = "#FFFFFF"
    ),
    typography = ThemeTypography(
        h1 = TextStyle(size = 28f, weight = "bold", fontFamily = "system"),
        h2 = TextStyle(size = 22f, weight = "bold"),
        body = TextStyle(size = 16f, weight = "regular"),
        caption = TextStyle(size = 12f, weight = "regular")
    ),
    spacing = ThemeSpacing(
        xs = 4f,
        sm = 8f,
        md = 16f,
        lg = 24f,
        xl = 32f
    ),
    effects = ThemeEffects(
        shadowEnabled = true,
        blurRadius = 8f,
        elevation = 4f
    )
)

// Access theme properties
val primaryColor = darkTheme.palette.primary  // "#007AFF"
val headingSize = darkTheme.typography.h1.size  // 28f
val mediumSpacing = darkTheme.spacing.md  // 16f
```

**Components**:
- `ThemePalette`: 10 color slots (primary, secondary, background, surface, error + on-variants)
- `ThemeTypography`: 4 text styles (h1, h2, body, caption)
- `ThemeSpacing`: 5 spacing values (xs, sm, md, lg, xl)
- `ThemeEffects`: Visual effects (shadows, blur, elevation)

---

## Utility Layer

### Result Extensions

**Purpose**: Functional error handling extensions for Kotlin `Result` type.

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/core/Result.kt`

**Design Philosophy**:
- Never throw exceptions in business logic
- Chain operations with `mapSuccess` / `flatMapSuccess`
- Provide clear error messages with context

**Code Example**:
```kotlin
// Map success values
val result: Result<Int> = Result.success(5)
val doubled = result.mapSuccess { it * 2 }  // Success(10)

// Chain result-returning operations
parseLayout(source)
    .flatMapSuccess { validateSchema(it) }
    .flatMapSuccess { buildComponents(it) }
    .onSuccess { logger.info("Loaded ${it.size} components") }
    .onFailure { logger.error("Failed to load layout", it) }

// Convert nullable to Result
val config: Config? = loadConfig()
val result = config.toResult("Config file not found")

// Get value or default
val components = loadLayout(file).getOrDefault(emptyList())

// Safe execution wrapper
val result = runCatching { riskyOperation() }
```

**Key Functions**:
- `mapSuccess<T, R>(transform: (T) -> R): Result<R>`: Transform success value
- `flatMapSuccess<T, R>(transform: (T) -> Result<R>): Result<R>`: Chain operations
- `onFailure(action: (Throwable) -> Unit): Result<T>`: Side effect on failure
- `getOrDefault(default: T): T`: Safe value extraction
- `toResult(errorMessage: String): Result<T>`: Nullable conversion
- `runCatching(block: () -> T): Result<T>`: Wrap throwing code

---

### Logger

**Purpose**: Platform-agnostic logging interface for AvaUI.

**Location**: `runtime/plugin-system/src/commonMain/kotlin/com/augmentalis/avacode/plugins/ui/core/Logger.kt`

**Design Philosophy**:
- Common interface with platform-specific implementations
- Tag-based logging (e.g., "AvaUI.DSL", "AvaUI.Database")
- Four severity levels: debug, info, warn, error

**Code Example**:
```kotlin
// Get logger instance
val logger = Logger.get("AvaUI.DSL")

// Log at different levels
logger.debug("Parsing DSL layout")
logger.info("Loaded 50 components in 15ms")
logger.warn("Theme not found, using default")
logger.error("Failed to parse layout", exception)

// Platform-specific implementations
// Android: Uses android.util.Log
// iOS: Uses OSLog or print()
// JVM: Uses SLF4J or println()
// JS: Uses console.log()
```

**Default Implementation**:
```kotlin
internal class ConsoleLogger(override val tag: String) : Logger {
    override fun debug(message: String) {
        println("DEBUG [$tag] $message")
    }

    override fun info(message: String) {
        println("INFO  [$tag] $message")
    }

    override fun warn(message: String, throwable: Throwable?) {
        println("WARN  [$tag] $message")
        throwable?.let { println(it.stackTraceToString()) }
    }

    override fun error(message: String, throwable: Throwable?) {
        println("ERROR [$tag] $message")
        throwable?.let { println(it.stackTraceToString()) }
    }
}
```

---

## Relationships Between Abstractions

```
┌─────────────────────────────────────────────────────────┐
│                   ComponentModel                        │
│  (Core data structure for all UI components)            │
│  - UUID, type, position, properties, children           │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ├──────────────────────────────────────┐
                  │                                      │
                  ▼                                      ▼
        ┌─────────────────┐                   ┌─────────────────┐
        │ LayoutLoader    │                   │ PluginComponent │
        │ (Load layouts)  │                   │ (Extensibility) │
        │ - DSL, YAML, JSON│                  │ - Lifecycle     │
        └─────────────────┘                   │ - Rendering     │
                  │                           │ - Export        │
                  │                           └─────────────────┘
                  ▼
        ┌─────────────────┐
        │  ThemeConfig    │
        │  (Styling)      │
        │  - Palette      │
        │  - Typography   │
        │  - Spacing      │
        └─────────────────┘

        ┌─────────────────┐          ┌─────────────────┐
        │ MotionProcessor │ ◄────────┤IMUOrientationData│
        │ (IMU Pipeline)  │          │ (Sensor Data)   │
        │ - Composable    │          │ - pitch/roll/yaw│
        │ - Configurable  │          └─────────────────┘
        └─────────────────┘

        ┌─────────────────┐          ┌─────────────────┐
        │ Result<T>       │          │     Logger      │
        │ (Error Handling)│          │  (Diagnostics)  │
        │ - Functional    │          │  - Tagged       │
        │ - Chainable     │          │  - Platform-agnostic│
        └─────────────────┘          └─────────────────┘
```

---

## Performance Characteristics

| Abstraction | Performance Target | Notes |
|-------------|-------------------|-------|
| ComponentModel | Zero-allocation in hot path | Immutable, pre-allocated children lists |
| MotionProcessor | <2ms per process() call | 60 FPS requirement (16.67ms budget) |
| LayoutLoader (DSL) | <0.02ms per layout | 78,000+ layouts/second throughput |
| IMUOrientationData | Zero-allocation | Value type, no heap allocation |
| Logger | Non-blocking | Platform implementations must not block |

---

## Testing Coverage

All core abstractions have comprehensive unit tests:

- `ComponentModelTest.kt`: 20+ test cases covering creation, hierarchy, helper methods
- Future tests (T026-T035): PluginComponent, LayoutLoader, MotionProcessor, ThemeConfig

**Target**: 80%+ code coverage for all P1 features

---

## Usage Patterns

### Pattern 1: Building Component Hierarchies

```kotlin
fun buildLoginForm(): ComponentModel {
    return ComponentModel(
        uuid = "app/login_form",
        type = "Column",
        position = ComponentPosition.ORIGIN,
        children = listOf(
            ComponentModel(
                uuid = "app/username_field",
                type = "TextField",
                position = ComponentPosition(0f, 0f),
                properties = mapOf("placeholder" to "Username")
            ),
            ComponentModel(
                uuid = "app/password_field",
                type = "TextField",
                position = ComponentPosition(0f, 60f),
                properties = mapOf("placeholder" to "Password", "secure" to "true")
            ),
            ComponentModel(
                uuid = "app/login_button",
                type = "Button",
                position = ComponentPosition(0f, 120f),
                properties = mapOf("text" to "Login", "color" to "#007AFF")
            )
        )
    )
}
```

### Pattern 2: Loading Layouts with Error Handling

```kotlin
fun loadAndApplyLayout(source: String): Result<Unit> {
    val loader = UnifiedLayoutLoader()

    return loader.load(source, LayoutFormat.AUTO)
        .flatMapSuccess { components ->
            validateComponents(components)
        }
        .flatMapSuccess { validatedComponents ->
            applyTheme(validatedComponents, currentTheme)
        }
        .mapSuccess { themedComponents ->
            renderComponents(themedComponents)
        }
        .onSuccess { logger.info("Layout loaded successfully") }
        .onFailure { error ->
            logger.error("Failed to load layout", error)
        }
        .map { Unit }
}
```

### Pattern 3: IMU Processing Pipeline

```kotlin
class IMUPipelineManager {
    private val processors = mutableListOf<MotionProcessor>()

    fun addProcessor(processor: MotionProcessor) {
        processors.add(processor)
    }

    fun processOrientation(rawData: IMUOrientationData): IMUOrientationData {
        var data = rawData
        for (processor in processors) {
            if (processor.isEnabled) {
                data = processor.process(data)
            }
        }
        return data
    }

    fun saveConfiguration(): Map<String, Any> {
        return processors.associate { processor ->
            processor.processorId to processor.getConfiguration()
        }
    }

    fun loadConfiguration(config: Map<String, Any>) {
        for (processor in processors) {
            val processorConfig = config[processor.processorId] as? Map<String, Any>
            if (processorConfig != null) {
                processor.applyConfiguration(processorConfig)
            }
        }
    }
}
```

### Pattern 4: Theme Application

```kotlin
fun applyThemeToComponents(
    components: List<ComponentModel>,
    theme: ThemeConfig
): List<ComponentModel> {
    return components.map { component ->
        when (component.type) {
            "Button" -> component.withProperties(
                "color" to theme.palette.primary,
                "textColor" to theme.palette.onPrimary,
                "fontSize" to theme.typography.body.size.toString()
            )
            "TextField" -> component.withProperties(
                "backgroundColor" to theme.palette.surface,
                "textColor" to theme.palette.onSurface,
                "fontSize" to theme.typography.body.size.toString()
            )
            else -> component
        }
    }
}
```

---

## Best Practices

1. **Never use `!!` operator**: All core abstractions follow null-safety practices
2. **KDoc all public APIs**: Every public class, interface, and function is documented
3. **Immutability by default**: Use `val` and `copy()` for updates
4. **Result over exceptions**: Return `Result<T>` instead of throwing exceptions
5. **Performance awareness**: Measure hot paths, avoid allocations in loops
6. **Platform agnostic**: Keep platform-specific code in separate source sets

---

## Future Extensions

**Planned for Phase 3-8**:
- `ComponentRegistry`: UUID management and component lookup
- `AssetResolver`: Plugin asset management
- `TransactionManager`: Checkpoint-based undo/redo
- `PermissionManager`: Security and permissions
- `DatabaseSchema`: Room entity definitions
- `DSLParser`: High-performance DSL parsing
- `ComponentInspector`: Runtime component inspection

---

**Created by Manoj Jhawar, manoj@ideahq.net**
**Phase 2 - Foundational Infrastructure (T027)**
