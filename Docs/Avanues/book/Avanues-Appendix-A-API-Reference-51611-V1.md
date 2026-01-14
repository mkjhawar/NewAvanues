# Appendix A: Complete API Reference

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net

---

## AvaUI Runtime API

### AvaUIRuntime

```kotlin
class AvaUIRuntime(
    private val registry: ComponentRegistry,
    private val scope: CoroutineScope
) {
    suspend fun loadApp(source: String): LoadedApp
    suspend fun start(app: VosAstNode.App): RunningApp
    suspend fun pause(appId: String)
    suspend fun resume(appId: String)
    suspend fun stop(appId: String)
    suspend fun handleVoiceCommand(voiceInput: String): CommandMatch?
}
```

### Component Registry

```kotlin
class ComponentRegistry {
    suspend fun register(descriptor: ComponentDescriptor)
    suspend fun get(type: String): ComponentDescriptor?
    suspend fun getAll(): List<ComponentDescriptor>
    suspend fun isRegistered(type: String): Boolean
    suspend fun unregister(type: String): Boolean
}
```

### Event Bus

```kotlin
class EventBus {
    suspend fun emit(event: ComponentEvent)
    fun tryEmit(event: ComponentEvent): Boolean
    val events: SharedFlow<ComponentEvent>
    val subscriptionCount: Int
}

data class ComponentEvent(
    val componentId: String,
    val eventName: String,
    val parameters: Map<String, Any?>
)
```

---

## Code Generation API

### CodeGenerator

```kotlin
interface CodeGenerator {
    fun generate(screen: ScreenNode): GeneratedCode
    fun generateComponent(component: ComponentNode): String
}

object CodeGeneratorFactory {
    fun create(platform: Platform, language: Language?): CodeGenerator
}
```

### Platform Types

```kotlin
enum class Platform {
    ANDROID, IOS, WEB, DESKTOP
}

enum class Language {
    KOTLIN, SWIFT, TYPESCRIPT, JAVASCRIPT
}
```

---

## Component Types

### All 48 Components

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

    // Layout (8)
    DRAWER, PAGINATION, TABS, BREADCRUMB, ACCORDION,
    STACK, GRID, SCROLL_VIEW,

    // Navigation (5)
    // Included in Advanced/Layout

    CUSTOM
}
```

---

## VoiceOSBridge API

### Core Interface

```kotlin
interface VoiceOSBridge {
    suspend fun registerCapability(capability: AppCapability): Result<Unit>
    suspend fun unregisterCapability(appId: String): Result<Unit>
    suspend fun queryCapabilities(filter: CapabilityFilter): Result<List<AppCapability>>

    suspend fun registerVoiceCommand(command: VoiceCommand): Result<Unit>
    suspend fun routeCommand(voiceInput: String): Result<CommandResult>

    suspend fun sendMessage(message: AppMessage): Result<MessageResult>
    suspend fun subscribeToMessages(filter: MessageFilter, handler: MessageHandler): Subscription

    suspend fun publishState(key: String, value: Any, scope: StateScope): Result<Unit>
    suspend fun getState(key: String): Result<Any?>
}
```

---

## CLI Commands

### avacode

```bash
# Generate code
avacode generate \
  --input screen.json \
  --output Screen.kt \
  --platform android \
  --language kotlin

# Validate DSL
avacode validate --input screen.json

# Format DSL
avacode format --input screen.json --output formatted.json

# Convert between formats
avacode convert --input screen.yaml --output screen.json
```

---

## Type Definitions

### PropertyValue

```kotlin
sealed class PropertyValue {
    data class StringValue(val value: String)
    data class IntValue(val value: Int)
    data class DoubleValue(val value: Double)
    data class BoolValue(val value: Boolean)
    data class EnumValue(val type: String, val value: String)
    data class ListValue(val items: List<PropertyValue>)
    data class MapValue(val items: Map<String, PropertyValue>)
    data class ReferenceValue(val ref: String)
}
```

---

**For complete API documentation, see source code KDoc comments.**

---

**Created by Manoj Jhawar, manoj@ideahq.net**
