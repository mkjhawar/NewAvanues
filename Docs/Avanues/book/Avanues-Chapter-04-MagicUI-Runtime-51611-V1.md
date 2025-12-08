# Chapter 4: AvaUI Runtime System

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~15,000 words

---

## Table of Contents

1. [Introduction](#1-introduction)
2. [Runtime Architecture](#2-runtime-architecture)
3. [File-by-File Analysis](#3-file-by-file-analysis)
   - [AvaUIRuntime.kt](#31-avauiruntimekt)
   - [ComponentInstantiator.kt](#32-componentinstantiatorkt)
   - [EventBus.kt](#33-eventbuskt)
   - [VoiceCommandRouter.kt](#34-voicecommandrouterkt)
   - [ActionDispatcher.kt](#35-actiondispatcherkt)
   - [AppLifecycle.kt](#36-applifecyclekt)
   - [StateManager.kt](#37-statemanagerkt)
   - [ResourceManager.kt](#38-resourcemanagerkt)
   - [ComponentRegistry.kt](#39-componentregistrykt)
   - [VosParser.kt](#310-vosparserkt)
4. [Integration Examples](#4-integration-examples)
5. [Performance Considerations](#5-performance-considerations)
6. [Extension Points](#6-extension-points)
7. [Summary](#7-summary)

---

## 1. Introduction

The **AvaUI Runtime** is the heart of the IDEAMagic framework. It orchestrates the execution of AvaUI applications by integrating six core subsystems:

1. **Parser** - Tokenizes and parses DSL source into AST
2. **Registry** - Maintains component type metadata
3. **Instantiator** - Creates Kotlin objects from AST nodes
4. **Events** - Manages event bus and callback execution
5. **Voice** - Routes voice commands with fuzzy matching
6. **Lifecycle** - Controls app state and resource management

This chapter provides a **file-by-file, class-by-class** deep dive into the runtime system implementation.

### Key Files

Location: `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/`

```
AvaUI/
├── AvaUIRuntime.kt                 (523 lines) - Main orchestration
├── instantiation/
│   └── ComponentInstantiator.kt      (299 lines) - Creates instances from AST
├── events/
│   ├── EventBus.kt                   (298 lines) - Reactive event system
│   └── CallbackAdapter.kt            - Binds DSL callbacks to events
├── voice/
│   ├── VoiceCommandRouter.kt         (90 lines)  - Voice command matching
│   └── ActionDispatcher.kt           (73 lines)  - Action execution
├── lifecycle/
│   ├── AppLifecycle.kt               (121 lines) - State machine
│   ├── StateManager.kt               (100 lines) - State persistence
│   └── ResourceManager.kt            (61 lines)  - Resource cleanup
├── registry/
│   └── ComponentRegistry.kt          (70 lines)  - Component metadata
└── dsl/
    ├── VosParser.kt                  (561 lines)  - Recursive descent parser
    ├── VosTokenizer.kt               - Lexical analysis
    ├── VosAstNode.kt                 - AST node definitions
    └── VosValue.kt                   - Value type system
```

---

## 2. Runtime Architecture

### 2.1 The Six Subsystems

```
┌─────────────────────────────────────────────────────────────┐
│                     AvaUIRuntime                          │
│                  (Main Orchestrator)                        │
└─────────────────────────────────────────────────────────────┘
              ▲         ▲         ▲         ▲         ▲
              │         │         │         │         │
     ┌────────┴─────┐   │   ┌─────┴─────┐   │   ┌────┴─────┐
     │   Parser     │   │   │  Events   │   │   │Lifecycle │
     │              │   │   │  System   │   │   │ Manager  │
     │ VosParser    │   │   │ EventBus  │   │   │AppLifec. │
     │ VosTokenizer │   │   │Callbacks  │   │   │StateMgr  │
     │ VosAstNode   │   │   │           │   │   │Resource  │
     └──────────────┘   │   └───────────┘   │   └──────────┘
              │         │         │         │         │
     ┌────────┴─────┐   │   ┌─────┴─────┐   │
     │  Registry    │   │   │   Voice   │   │
     │              │   │   │  Router   │   │
     │ Component    │   │   │CommandMa. │   │
     │ Descriptor   │   │   │ActionDisp.│   │
     │ BuiltIns     │   │   │           │   │
     └──────────────┘   │   └───────────┘   │
                        │                    │
                 ┌──────┴─────────┐
                 │ Instantiator   │
                 │                │
                 │ComponentInstan.│
                 │PropertyMapper  │
                 │TypeCoercion    │
                 └────────────────┘
```

### 2.2 Execution Flow

**Phase 1: Loading** (Parser + Registry)
```kotlin
// 1. Tokenize DSL source
val tokens = tokenizer.tokenize(dslSource)

// 2. Parse into AST
val parser = VosParser(tokens)
val app = parser.parse()  // VosAstNode.App

// 3. Registry already populated with built-in components
registry.isRegistered("ColorPicker") // true
```

**Phase 2: Instantiation** (Instantiator)
```kotlin
// 4. Create Kotlin objects from AST
for (component in app.components) {
    val instance = instantiator.instantiate(component)
    runningApp.components[componentId] = instance
}
```

**Phase 3: Event Wiring** (Events)
```kotlin
// 5. Wire callbacks to event bus
for ((componentId, callbacks) in app.callbacks) {
    callbacks.forEach { (eventName, lambda) ->
        callbackAdapter.bindCallback(componentId, eventName, lambda)
    }
}
```

**Phase 4: Voice Registration** (Voice Router)
```kotlin
// 6. Register voice commands
app.voiceCommands.forEach { (trigger, action) ->
    voiceRouter.register(trigger, action, app.id)
}
```

**Phase 5: Lifecycle Start** (Lifecycle)
```kotlin
// 7. Bring app to life
lifecycle.create()
lifecycle.start()
lifecycle.resume()
```

---

## 3. File-by-File Analysis

### 3.1 AvaUIRuntime.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/AvaUIRuntime.kt`
**Lines:** 523
**Purpose:** Main orchestration class integrating all six subsystems

#### 3.1.1 Class Structure

```kotlin
/**
 * AvaUI DSL Runtime - Main orchestration class.
 *
 * The runtime integrates 6 core subsystems:
 * 1. Parser (Phase 1): Tokenizes and parses .vos source into AST
 * 2. Registry (Phase 2): Maintains component type metadata
 * 3. Instantiator (Phase 3): Creates Kotlin objects from AST nodes
 * 4. Events (Phase 4): Manages event bus and callback execution
 * 5. Voice (Phase 5): Routes voice commands with fuzzy matching
 * 6. Lifecycle (Phase 6): Controls app state and resource management
 */
class AvaUIRuntime(
    private val registry: ComponentRegistry = ComponentRegistry.getInstance(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {
    // Subsystems
    private val eventBus = EventBus()
    private val voiceRouter = VoiceCommandRouter()
    private val actionDispatcher: ActionDispatcher
    private val instantiator: ComponentInstantiator
    private val callbackAdapter: CallbackAdapter

    // Running apps
    private val runningApps = mutableMapOf<String, RunningApp>()

    // Key methods
    suspend fun loadApp(source: String): LoadedApp
    suspend fun start(app: VosAstNode.App): RunningApp
    suspend fun pause(appId: String)
    suspend fun resume(appId: String)
    suspend fun stop(appId: String)
    suspend fun handleVoiceCommand(voiceInput: String): CommandMatch?
}
```

#### 3.1.2 RunningApp Data Structure

```kotlin
/**
 * Represents a running AvaUI app instance.
 *
 * Contains all runtime state for an active application:
 * - Parsed AST definition
 * - Instantiated component instances
 * - Lifecycle state manager
 * - State persistence manager
 * - Resource cleanup manager
 */
data class RunningApp(
    val app: VosAstNode.App,
    val components: MutableMap<String, Any>,
    val lifecycle: AppLifecycle,
    val stateManager: StateManager,
    val resourceManager: ResourceManager
) {
    /**
     * Get a component by ID.
     *
     * @param id Component identifier from DSL
     * @return Component instance or null if not found
     */
    fun getComponent(id: String): Any? = components[id]

    /**
     * Get typed component by ID.
     *
     * Example:
     * ```kotlin
     * val colorPicker: ColorPickerView? = runningApp.getTypedComponent("picker1")
     * ```
     */
    inline fun <reified T> getTypedComponent(id: String): T? {
        return components[id] as? T
    }

    /**
     * Current lifecycle state.
     */
    val lifecycleState: LifecycleState
        get() = lifecycle.state.value
}
```

#### 3.1.3 Core Methods

##### loadApp()

```kotlin
/**
 * Load and parse a AvaUI app from DSL source.
 *
 * Steps:
 * 1. Tokenize DSL source code
 * 2. Parse tokens into AST
 * 3. Validate AST structure
 * 4. Return LoadedApp (not yet instantiated)
 *
 * This is Phase 1 - parsing only, no instantiation.
 *
 * Example:
 * ```kotlin
 * val dsl = """
 *     App {
 *         id: "colorPicker.demo"
 *         name: "Color Picker Demo"
 *
 *         ColorPicker {
 *             id: "picker1"
 *             initialColor: "#FF5733"
 *             showAlpha: true
 *         }
 *     }
 * """
 * val loaded = runtime.loadApp(dsl)
 * ```
 *
 * @param source DSL source code
 * @return LoadedApp containing parsed AST
 * @throws ParserException if DSL is malformed
 */
suspend fun loadApp(source: String): LoadedApp {
    // Tokenize
    val tokenizer = VosTokenizer()
    val tokens = tokenizer.tokenize(source)

    // Parse
    val parser = VosParser(tokens)
    val app = parser.parse()

    return LoadedApp(app)
}
```

##### start()

```kotlin
/**
 * Start a loaded app, creating instances and bringing it to life.
 *
 * This is Phases 2-5:
 * 1. Create lifecycle managers (AppLifecycle, StateManager, ResourceManager)
 * 2. Instantiate all components from AST
 * 3. Wire event callbacks
 * 4. Register voice commands
 * 5. Transition lifecycle: CREATED → STARTED → RESUMED
 *
 * Example:
 * ```kotlin
 * val loaded = runtime.loadApp(dslSource)
 * val running = runtime.start(loaded.app)
 *
 * // Now components are instantiated and active
 * val picker = running.getTypedComponent<ColorPickerView>("picker1")
 * ```
 *
 * @param app Parsed app AST from loadApp()
 * @return RunningApp with instantiated components
 * @throws InstantiationException if component creation fails
 */
suspend fun start(app: VosAstNode.App): RunningApp {
    // 1. Create lifecycle managers
    val lifecycle = AppLifecycle()
    val stateManager = StateManager()
    val resourceManager = ResourceManager()

    val runningApp = RunningApp(
        app = app,
        components = mutableMapOf(),
        lifecycle = lifecycle,
        stateManager = stateManager,
        resourceManager = resourceManager
    )

    lifecycle.create()

    // 2. Instantiate components
    for (component in app.components) {
        val componentId = component.id ?: generateComponentId(component)
        val instance = instantiator.instantiate(component)

        runningApp.components[componentId] = instance

        // Register as managed resource (for cleanup)
        if (instance is ManagedResource) {
            resourceManager.register(componentId, instance)
        }
    }

    // 3. Wire callbacks
    for (component in app.components) {
        val componentId = component.id ?: continue

        component.callbacks.forEach { (eventName, lambda) ->
            callbackAdapter.bindCallback(
                componentId = componentId,
                eventName = eventName,
                lambda = lambda,
                eventBus = eventBus
            )
        }
    }

    // 4. Register voice commands
    app.voiceCommands.forEach { (trigger, action) ->
        voiceRouter.register(trigger, action, app.id)
    }

    // 5. Start lifecycle
    lifecycle.start()
    lifecycle.resume()

    // Store running app
    runningApps[app.id] = runningApp

    return runningApp
}
```

##### handleVoiceCommand()

```kotlin
/**
 * Handle voice input from user.
 *
 * Steps:
 * 1. Match voice input to registered commands (fuzzy matching, 0.7 threshold)
 * 2. Dispatch action to appropriate component via event bus
 * 3. Return match result with confidence score
 *
 * Example:
 * ```kotlin
 * // User says "open color picker"
 * val match = runtime.handleVoiceCommand("open color picker")
 *
 * if (match != null) {
 *     println("Matched: ${match.command.trigger} (${match.confidence})")
 *     // Action dispatched automatically
 * }
 * ```
 *
 * @param voiceInput Raw voice input from speech recognition
 * @return CommandMatch if match found (confidence > 0.7), null otherwise
 */
suspend fun handleVoiceCommand(voiceInput: String): CommandMatch? {
    // Match command
    val match = voiceRouter.match(voiceInput) ?: return null

    // Dispatch action
    actionDispatcher.dispatch(match)

    return match
}
```

---

### 3.2 ComponentInstantiator.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/instantiation/ComponentInstantiator.kt`
**Lines:** 299
**Purpose:** Creates native Kotlin objects from DSL AST nodes

#### 3.2.1 Architecture

The `ComponentInstantiator` bridges the gap between **abstract syntax** (AST nodes) and **concrete runtime** (Kotlin objects).

**Pipeline:**

```
VosAstNode.Component        ComponentDescriptor         Kotlin Instance
       (AST)          →    (Metadata from Registry)  →   (Object)

Properties Map:            Property Mapping:            Constructor Args:
{                          "initialColor" → "color"     ColorPickerView(
  "initialColor": "#FF5733"  "showAlpha" → "alpha"        color = Color(0xFFFF5733),
  "showAlpha": true       Type Coercion:                  alpha = true
}                          String → Color                )
                           Boolean → Boolean
```

#### 3.2.2 Class Structure

```kotlin
/**
 * Creates native Kotlin objects from DSL AST nodes.
 *
 * The ComponentInstantiator performs:
 * 1. Component Resolution - Looks up metadata from registry
 * 2. Property Mapping - Maps DSL properties to Kotlin properties
 * 3. Type Coercion - Converts DSL values to appropriate Kotlin types
 * 4. Validation - Ensures required properties are present and valid
 * 5. Instantiation - Creates actual object instances
 * 6. Hierarchy Processing - Recursively instantiates child components
 */
class ComponentInstantiator(
    private val registry: ComponentRegistry,
    private val propertyMapper: PropertyMapper = PropertyMapper(),
    private val typeCoercion: TypeCoercion = TypeCoercion()
) {
    suspend fun instantiate(component: VosAstNode.Component): Any

    private fun createInstance(
        descriptor: ComponentDescriptor,
        properties: Map<String, Any?>
    ): Any

    private fun setChildren(instance: Any, children: List<Any>)
}
```

#### 3.2.3 The instantiate() Method

```kotlin
/**
 * Instantiate a component from AST node.
 *
 * This method orchestrates the entire instantiation process:
 * 1. Resolves component type from registry
 * 2. Maps and validates properties
 * 3. Applies type coercion
 * 4. Creates the instance
 * 5. Processes children (if applicable)
 *
 * The instantiation process is recursive - child components are instantiated
 * using the same process, allowing for deep component hierarchies.
 *
 * Example:
 * ```kotlin
 * val component = VosAstNode.Component(
 *     type = "ColorPicker",
 *     properties = mapOf(
 *         "initialColor" to VosValue.StringValue("#FF5733"),
 *         "showAlpha" to VosValue.BoolValue(true)
 *     )
 * )
 *
 * val instance = instantiator.instantiate(component)
 * // instance is ColorPickerView(color=Color(0xFFFF5733), alpha=true)
 * ```
 *
 * @param component AST component node to instantiate
 * @return Instantiated object (type depends on component type)
 * @throws InstantiationException if component cannot be instantiated
 */
suspend fun instantiate(component: VosAstNode.Component): Any {
    // 1. Look up component descriptor
    val descriptor = registry.get(component.type)
        ?: throw InstantiationException("Unknown component type: ${component.type}")

    // 2. Map DSL properties to Kotlin properties
    val mappedProperties = propertyMapper.mapProperties(
        component.properties,
        descriptor.properties
    )

    // 3. Apply type coercion
    val coercedProperties = mappedProperties.mapValues { (name, value) ->
        val propDescriptor = descriptor.properties[name]
            ?: throw InstantiationException("Unknown property: $name")
        typeCoercion.coerce(value, propDescriptor.type)
    }

    // 4. Create instance (using factory pattern)
    val instance = createInstance(descriptor, coercedProperties)

    // 5. Instantiate children (if supported)
    if (descriptor.supportsChildren && component.children.isNotEmpty()) {
        val children = component.children.map { instantiate(it) }
        setChildren(instance, children)
    }

    return instance
}
```

#### 3.2.4 Component Factory

```kotlin
/**
 * Create instance using factory pattern.
 *
 * Current implementation uses hardcoded factories for known types.
 * Future versions will use:
 * - KClass reflection for dynamic instantiation
 * - Dependency injection framework (Koin/Dagger)
 * - Plugin-based factory system
 *
 * @param descriptor Component metadata from registry
 * @param properties Validated and coerced property map
 * @return Instantiated component object
 */
private fun createInstance(
    descriptor: ComponentDescriptor,
    properties: Map<String, Any?>
): Any {
    return when (descriptor.type) {
        "ColorPicker" -> createColorPicker(properties)
        "Preferences" -> createPreferences(properties)
        "Text" -> createText(properties)
        "Button" -> createButton(properties)
        "Container" -> createContainer(properties)
        else -> throw InstantiationException("Cannot instantiate: ${descriptor.type}")
    }
}

/**
 * Creates a ColorPicker component instance.
 *
 * Expected properties:
 * - initialColor: String (hex format) or ColorRGBA
 * - showAlpha: Boolean
 * - showHex: Boolean
 * - allowEyedropper: Boolean
 */
private fun createColorPicker(properties: Map<String, Any?>): Any {
    // TODO: Import ColorPickerView and create actual instance
    // For now, return placeholder object
    return object {
        val type = "ColorPicker"
        val props = properties
        override fun toString() = "ColorPicker(props=$props)"
    }
}
```

**Note:** The current implementation uses placeholder objects. In production, this would create actual UI component instances:

```kotlin
// Future production implementation:
private fun createColorPicker(properties: Map<String, Any?>): ColorPickerView {
    return ColorPickerView(
        initialColor = properties["initialColor"] as? Color ?: Color.White,
        showAlpha = properties["showAlpha"] as? Boolean ?: true,
        showHex = properties["showHex"] as? Boolean ?: true,
        allowEyedropper = properties["allowEyedropper"] as? Boolean ?: false
    )
}
```

---

### 3.3 EventBus.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/events/EventBus.kt`
**Lines:** 298
**Purpose:** Central event bus for component events using Kotlin Flow

#### 3.3.1 Architecture

The `EventBus` provides a **reactive, thread-safe** event propagation system using Kotlin's `SharedFlow`.

**Key Features:**
- **Thread-safe**: All operations are concurrent-safe (Mutex-protected internally)
- **Backpressure**: Buffers up to 100 events without blocking emitters
- **Hot stream**: Events are broadcast to all active collectors (multicast)
- **No replay**: Events are not cached; only live events are delivered

**Flow:**

```
Producer 1                EventBus                Consumer 1
  (ColorPicker)            (SharedFlow)           (UI Handler)
     │                         │                       │
     │  emit(ColorChange)      │                       │
     ├─────────────────────────>│                       │
     │                         │  collect()             │
     │                         ├───────────────────────>│
     │                         │                       │
Producer 2                     │                  Consumer 2
  (Button)                     │                  (Logger)
     │                         │                       │
     │  emit(ButtonClick)      │                       │
     ├─────────────────────────>│  collect()           │
     │                         ├───────────────────────>│
```

#### 3.3.2 Class Structure

```kotlin
/**
 * Central event bus for AvaUI component events.
 *
 * Uses Kotlin Flow (SharedFlow) for reactive event delivery.
 */
class EventBus {
    private val _events = MutableSharedFlow<ComponentEvent>(
        replay = 0,              // No caching of past events
        extraBufferCapacity = 100 // Buffer up to 100 pending events
    )

    val events: SharedFlow<ComponentEvent> = _events.asSharedFlow()

    // Suspend (coroutine-based)
    suspend fun emit(event: ComponentEvent)

    // Non-suspend (returns false if buffer full)
    fun tryEmit(event: ComponentEvent): Boolean

    // Diagnostics
    val subscriptionCount: Int
    fun resetSubscriptionCount()
}
```

#### 3.3.3 ComponentEvent Data Class

```kotlin
/**
 * Represents a component event emitted through the EventBus.
 *
 * Encapsulates:
 * - Which component triggered the event (componentId)
 * - What type of event occurred (eventName)
 * - Event-specific data (parameters)
 */
data class ComponentEvent(
    val componentId: String,
    val eventName: String,
    val parameters: Map<String, Any?>
) {
    /**
     * Gets a parameter value with type casting.
     *
     * Example:
     * ```kotlin
     * val color: String? = event.getParameter("color")
     * val x: Int? = event.getParameter("x")
     * ```
     */
    inline fun <reified T> getParameter(name: String): T? {
        return parameters[name] as? T
    }

    /**
     * Gets a required parameter (throws if missing).
     *
     * Example:
     * ```kotlin
     * val color: String = event.requireParameter("color")
     * ```
     */
    inline fun <reified T> requireParameter(name: String): T {
        val value = parameters[name]
            ?: throw IllegalArgumentException("Required parameter '$name' not found")
        return value as? T
            ?: throw IllegalArgumentException("Parameter '$name' cannot be cast to ${T::class.simpleName}")
    }

    companion object {
        fun noParams(componentId: String, eventName: String): ComponentEvent
        fun singleParam(componentId: String, eventName: String, paramName: String, paramValue: Any?): ComponentEvent
    }
}
```

#### 3.3.4 Usage Examples

**Producer (Component emitting event):**

```kotlin
launch {
    eventBus.emit(ComponentEvent(
        componentId = "colorPicker1",
        eventName = "onColorChange",
        parameters = mapOf("color" to "#FF5733")
    ))
}
```

**Consumer (Handler collecting events):**

```kotlin
launch {
    eventBus.events
        .filter { it.componentId == "colorPicker1" }
        .collect { event ->
            val color = event.requireParameter<String>("color")
            println("Color changed to: $color")
        }
}
```

**Multiple Consumers (Broadcast):**

```kotlin
// Consumer 1: UI update
launch {
    eventBus.events.collect { event ->
        updateUI(event)
    }
}

// Consumer 2: Logging
launch {
    eventBus.events.collect { event ->
        log.info("Event: ${event.eventName} from ${event.componentId}")
    }
}

// Consumer 3: State persistence
launch {
    eventBus.events
        .filter { it.eventName == "onColorChange" }
        .collect { event ->
            stateManager.put("lastColor", event.getParameter<String>("color"))
        }
}
```

---

### 3.4 VoiceCommandRouter.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/voice/VoiceCommandRouter.kt`
**Lines:** 90
**Purpose:** Routes voice commands to app actions using fuzzy matching

#### 3.4.1 Architecture

The `VoiceCommandRouter` matches voice input to registered commands using **fuzzy string matching**.

**Matching Algorithm:**

```
Voice Input: "open color picker"
                    ↓
            Normalize & tokenize
                    ↓
        ["open", "color", "picker"]
                    ↓
         Compare with registered commands:

Command 1: "show color picker"     → ["show", "color", "picker"]
Intersection: ["color", "picker"]  → 2 words
Union: ["open", "show", "color", "picker"] → 4 words
Similarity: 2/4 = 0.5 (below 0.7 threshold, rejected)

Command 2: "open picker"           → ["open", "picker"]
Intersection: ["open", "picker"]   → 2 words
Union: ["open", "color", "picker"] → 3 words
Similarity: 2/3 = 0.667 (below 0.7 threshold, rejected)

Command 3: "open color selector"   → ["open", "color", "selector"]
Intersection: ["open", "color"]    → 2 words
Union: ["open", "color", "picker", "selector"] → 4 words
Similarity: 2/4 = 0.5 (rejected)

Command 4: "open the color picker" → ["open", "the", "color", "picker"]
Intersection: ["open", "color", "picker"] → 3 words
Union: ["open", "the", "color", "picker"] → 4 words
Similarity: 3/4 = 0.75 ✓ MATCH (confidence = 0.75)
```

#### 3.4.2 Class Structure

```kotlin
/**
 * Routes voice commands to app actions.
 *
 * Supports:
 * - Exact matching (1.0 confidence)
 * - Fuzzy matching (Jaccard similarity, 0.7+ threshold)
 * - Case-insensitive matching
 */
class VoiceCommandRouter {
    private val commands = mutableMapOf<String, VoiceCommand>()

    fun register(trigger: String, action: String, componentId: String? = null)
    fun unregister(trigger: String)
    fun match(voiceInput: String): CommandMatch?
    fun getAll(): List<VoiceCommand>
    fun clear()

    private fun calculateSimilarity(a: String, b: String): Float
}

data class VoiceCommand(
    val trigger: String,
    val action: String,
    val componentId: String?
)

data class CommandMatch(
    val command: VoiceCommand,
    val confidence: Float  // 0.7 to 1.0
)
```

#### 3.4.3 match() Method

```kotlin
/**
 * Match voice input to registered command.
 *
 * Steps:
 * 1. Normalize input (lowercase, trim)
 * 2. Try exact match first (confidence = 1.0)
 * 3. If no exact match, try fuzzy match (Jaccard similarity)
 * 4. Return best match above 0.7 threshold
 *
 * @param voiceInput Raw voice input from speech recognition
 * @return CommandMatch if confidence > 0.7, null otherwise
 */
fun match(voiceInput: String): CommandMatch? {
    val normalized = voiceInput.lowercase().trim()

    // Exact match
    commands[normalized]?.let {
        return CommandMatch(it, 1.0f)
    }

    // Fuzzy match
    val matches = commands.values.mapNotNull { command ->
        val similarity = calculateSimilarity(normalized, command.trigger)
        if (similarity > 0.7f) {
            CommandMatch(command, similarity)
        } else null
    }

    return matches.maxByOrNull { it.confidence }
}
```

#### 3.4.4 Jaccard Similarity

```kotlin
/**
 * Calculate Jaccard similarity between two strings.
 *
 * Jaccard Similarity = |A ∩ B| / |A ∪ B|
 *
 * Where:
 * - A = word set of string a
 * - B = word set of string b
 * - ∩ = intersection (words in both)
 * - ∪ = union (all unique words)
 *
 * Example:
 * a = "open color picker"  → ["open", "color", "picker"]
 * b = "show color picker"  → ["show", "color", "picker"]
 *
 * Intersection = ["color", "picker"] → 2 words
 * Union = ["open", "show", "color", "picker"] → 4 words
 * Similarity = 2/4 = 0.5
 */
private fun calculateSimilarity(a: String, b: String): Float {
    // Split into word sets
    val aWords = a.split(" ").toSet()
    val bWords = b.split(" ").toSet()

    // Calculate Jaccard coefficient
    val intersection = aWords.intersect(bWords).size
    val union = aWords.union(bWords).size

    return if (union > 0) {
        intersection.toFloat() / union.toFloat()
    } else {
        0f
    }
}
```

#### 3.4.5 Usage Example

```kotlin
// Register commands
router.register("open color picker", "ColorPicker.show")
router.register("close picker", "ColorPicker.hide")
router.register("reset color", "ColorPicker.reset")

// Match voice input
val match = router.match("open the color picker")

if (match != null) {
    println("Matched: ${match.command.trigger}")
    println("Confidence: ${match.confidence}")
    println("Action: ${match.command.action}")

    // Output:
    // Matched: open color picker
    // Confidence: 0.75
    // Action: ColorPicker.show
}
```

---

### 3.5 ActionDispatcher.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/voice/ActionDispatcher.kt`
**Lines:** 73
**Purpose:** Dispatches voice-triggered actions as events

#### 3.5.1 Architecture

The `ActionDispatcher` converts matched voice commands into `ComponentEvent` objects and emits them to the `EventBus`.

**Flow:**

```
Voice Input          VoiceCommandRouter       ActionDispatcher          EventBus
     │                       │                        │                     │
     │  "open picker"        │                        │                     │
     ├───────────────────────>│                        │                     │
     │                       │  match()               │                     │
     │                       │  CommandMatch(         │                     │
     │                       │    command=VoiceCommand│                     │
     │                       │      (trigger="open color picker",           │
     │                       │       action="ColorPicker.show"),            │
     │                       │    confidence=0.75)    │                     │
     │                       ├────────────────────────>│                     │
     │                       │                        │  dispatch()         │
     │                       │                        │  emit(ComponentEvent│
     │                       │                        │    (componentId="ColorPicker",
     │                       │                        │     eventName="voiceMethodCall",
     │                       │                        │     parameters={    │
     │                       │                        │       "target": "ColorPicker",
     │                       │                        │       "method": "show"
     │                       │                        │     }))             │
     │                       │                        ├─────────────────────>│
```

#### 3.5.2 Class Structure

```kotlin
/**
 * Dispatches voice-triggered actions.
 *
 * Converts CommandMatch → ComponentEvent → EventBus emission
 */
class ActionDispatcher(
    private val eventBus: EventBus
) {
    suspend fun dispatch(match: CommandMatch, context: Map<String, Any?> = emptyMap())

    private suspend fun dispatchSimpleAction(
        action: String,
        componentId: String?,
        context: Map<String, Any?>
    )

    private suspend fun dispatchMethodCall(
        target: String,
        method: String,
        componentId: String?,
        context: Map<String, Any?>
    )
}
```

#### 3.5.3 dispatch() Method

```kotlin
/**
 * Dispatch action from matched command.
 *
 * Parses action string into either:
 * 1. Simple action: "openColorPicker"
 * 2. Method call: "ColorPicker.show"
 *
 * Then emits appropriate ComponentEvent.
 */
suspend fun dispatch(match: CommandMatch, context: Map<String, Any?> = emptyMap()) {
    val command = match.command

    // Parse action
    val actionParts = command.action.split(".")

    when {
        actionParts.size == 1 -> {
            // Simple action like "openColorPicker"
            dispatchSimpleAction(command.action, command.componentId, context)
        }
        actionParts.size == 2 -> {
            // Method call like "ColorPicker.show"
            dispatchMethodCall(actionParts[0], actionParts[1], command.componentId, context)
        }
        else -> {
            throw ActionDispatchException("Invalid action format: ${command.action}")
        }
    }
}
```

#### 3.5.4 Event Emission

```kotlin
/**
 * Emit event for simple action.
 */
private suspend fun dispatchSimpleAction(
    action: String,
    componentId: String?,
    context: Map<String, Any?>
) {
    eventBus.emit(ComponentEvent(
        componentId = componentId ?: "app",
        eventName = "voiceAction",
        parameters = mapOf(
            "action" to action,
            "context" to context
        )
    ))
}

/**
 * Emit event for method call.
 */
private suspend fun dispatchMethodCall(
    target: String,
    method: String,
    componentId: String?,
    context: Map<String, Any?>
) {
    eventBus.emit(ComponentEvent(
        componentId = componentId ?: target,
        eventName = "voiceMethodCall",
        parameters = mapOf(
            "target" to target,
            "method" to method,
            "context" to context
        )
    ))
}
```

---

### 3.6 AppLifecycle.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/lifecycle/AppLifecycle.kt`
**Lines:** 121
**Purpose:** Manages app lifecycle state machine

#### 3.6.1 State Machine

```
┌─────────┐    create()    ┌─────────┐    start()     ┌─────────┐    resume()    ┌─────────┐
│ INITIAL │ ──────────────> │ CREATED │ ──────────────> │ STARTED │ ──────────────> │ RESUMED │
└─────────┘                 └─────────┘                 └─────────┘                 └─────────┘
                                                              ▲                           │
                                                              │         pause()           │
                                                              │ <─────────────────────────┘
                                                              │
                                                        ┌─────┴─────┐
                                                        │  PAUSED   │
                                                        └───────────┘
                                                              │
                                                              │  stop()
                                                              ▼
                                                        ┌───────────┐    destroy()   ┌───────────┐
                                                        │  STOPPED  │ ──────────────> │ DESTROYED │
                                                        └───────────┘                 └───────────┘
```

**State Descriptions:**

- **CREATED**: App instantiated, resources allocated
- **STARTED**: App visible (but not focused)
- **RESUMED**: App active, receiving input (RUNNING)
- **PAUSED**: App visible but not receiving input
- **STOPPED**: App not visible, background state
- **DESTROYED**: App terminated, all resources released

#### 3.6.2 Class Structure

```kotlin
/**
 * Manages app lifecycle state.
 *
 * State transitions:
 * - create()  : → CREATED
 * - start()   : CREATED → STARTED
 * - resume()  : STARTED → RESUMED (or PAUSED → RESUMED)
 * - pause()   : RESUMED → PAUSED
 * - stop()    : Any → STOPPED
 * - destroy() : Any → DESTROYED
 */
class AppLifecycle {
    private val _state = MutableStateFlow<LifecycleState>(LifecycleState.CREATED)
    val state: StateFlow<LifecycleState> = _state.asStateFlow()

    private val observers = mutableListOf<LifecycleObserver>()
    private var isInitialized = false

    suspend fun create()
    suspend fun start()
    suspend fun pause()
    suspend fun resume()
    suspend fun stop()
    suspend fun destroy()

    fun addObserver(observer: LifecycleObserver)
    fun removeObserver(observer: LifecycleObserver)
}

enum class LifecycleState {
    CREATED,
    STARTED,
    PAUSED,
    RESUMED,
    STOPPED,
    DESTROYED
}

interface LifecycleObserver {
    suspend fun onCreate() {}
    suspend fun onStart() {}
    suspend fun onPause() {}
    suspend fun onResume() {}
    suspend fun onStop() {}
    suspend fun onDestroy() {}
}
```

#### 3.6.3 Lifecycle Methods

```kotlin
/**
 * Initialize app (onCreate).
 */
suspend fun create() {
    if (isInitialized) return

    _state.value = LifecycleState.CREATED
    notifyObservers { it.onCreate() }
    isInitialized = true
}

/**
 * Start app (onStart).
 */
suspend fun start() {
    requireInitialized()
    _state.value = LifecycleState.STARTED
    notifyObservers { it.onStart() }
}

/**
 * Resume app (onResume).
 */
suspend fun resume() {
    requireInitialized()
    _state.value = LifecycleState.RESUMED
    notifyObservers { it.onResume() }
}

/**
 * Pause app (onPause).
 */
suspend fun pause() {
    requireInitialized()
    _state.value = LifecycleState.PAUSED
    notifyObservers { it.onPause() }
}

/**
 * Stop app (onStop).
 */
suspend fun stop() {
    requireInitialized()
    _state.value = LifecycleState.STOPPED
    notifyObservers { it.onStop() }
}

/**
 * Destroy app (onDestroy).
 */
suspend fun destroy() {
    requireInitialized()
    _state.value = LifecycleState.DESTROYED
    notifyObservers { it.onDestroy() }
    observers.clear()
    isInitialized = false
}
```

#### 3.6.4 Observer Pattern

```kotlin
/**
 * Add lifecycle observer.
 *
 * Observers receive callbacks for lifecycle events.
 *
 * Example:
 * ```kotlin
 * val observer = object : LifecycleObserver {
 *     override suspend fun onCreate() {
 *         println("App created")
 *     }
 *
 *     override suspend fun onDestroy() {
 *         println("App destroyed, cleaning up...")
 *     }
 * }
 *
 * lifecycle.addObserver(observer)
 * ```
 */
fun addObserver(observer: LifecycleObserver) {
    if (!observers.contains(observer)) {
        observers.add(observer)
    }
}

private suspend fun notifyObservers(action: suspend (LifecycleObserver) -> Unit) {
    observers.forEach { action(it) }
}
```

---

### 3.7 StateManager.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/lifecycle/StateManager.kt`
**Lines:** 100
**Purpose:** Manages app state persistence and restoration

#### 3.7.1 Architecture

The `StateManager` provides a **key-value store** for persisting app state across lifecycle events (pause/resume, configuration changes).

**Use Cases:**
- Save user input when app is paused
- Restore scroll position after configuration change
- Persist form data during app restart
- Cache last selected color in ColorPicker

#### 3.7.2 Class Structure

```kotlin
/**
 * Manages app state persistence and restoration.
 *
 * Provides:
 * - Key-value storage (in-memory)
 * - JSON serialization/deserialization
 * - Type-safe getters
 */
class StateManager(
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
) {
    internal val state = mutableMapOf<String, Any?>()

    fun put(key: String, value: Any?)
    fun get(key: String): Any?
    inline fun <reified T> getTyped(key: String): T?
    fun remove(key: String): Any?
    fun clear()

    fun serialize(): String
    fun deserialize(jsonString: String)

    fun keys(): Set<String>
    fun size(): Int
    fun contains(key: String): Boolean
}
```

#### 3.7.3 Usage Examples

**Saving State:**

```kotlin
// Save color picker state
stateManager.put("lastColor", "#FF5733")
stateManager.put("showAlpha", true)
stateManager.put("scrollPosition", 240)

// Serialize to JSON (for persistent storage)
val json = stateManager.serialize()
// {"lastColor":"#FF5733","showAlpha":true,"scrollPosition":240}

// Save to file or SharedPreferences
FileIO.writeFile("app_state.json", json)
```

**Restoring State:**

```kotlin
// Load from file
val json = FileIO.readFile("app_state.json")

// Deserialize into StateManager
stateManager.deserialize(json)

// Restore values
val lastColor = stateManager.getTyped<String>("lastColor") ?: "#FFFFFF"
val showAlpha = stateManager.getTyped<Boolean>("showAlpha") ?: true
val scrollPosition = stateManager.getTyped<Int>("scrollPosition") ?: 0
```

**Lifecycle Integration:**

```kotlin
val observer = object : LifecycleObserver {
    override suspend fun onPause() {
        // Save state when app is paused
        val json = stateManager.serialize()
        FileIO.writeFile("state.json", json)
    }

    override suspend fun onResume() {
        // Restore state when app is resumed
        val json = FileIO.readFile("state.json")
        stateManager.deserialize(json)
    }
}

lifecycle.addObserver(observer)
```

---

### 3.8 ResourceManager.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/lifecycle/ResourceManager.kt`
**Lines:** 61
**Purpose:** Manages app resources and cleanup

#### 3.8.1 Architecture

The `ResourceManager` tracks **managed resources** (file handles, network connections, database connections) and ensures proper cleanup when the app stops.

**Resource Lifecycle:**

```
register()        App Running         release() / releaseAll()
    │                  │                        │
    ▼                  ▼                        ▼
┌──────────┐     ┌──────────┐            ┌──────────┐
│ Register │ ──> │  Active  │ ────────>  │ Released │
│ Resource │     │ Resource │            │ (Cleanup)│
└──────────┘     └──────────┘            └──────────┘
```

#### 3.8.2 Class Structure

```kotlin
/**
 * Manages app resources and cleanup.
 *
 * Ensures proper resource cleanup when app stops or is destroyed.
 */
class ResourceManager {
    private val resources = mutableMapOf<String, ManagedResource>()

    fun register(id: String, resource: ManagedResource)
    fun get(id: String): ManagedResource?
    suspend fun release(id: String)
    suspend fun releaseAll()
    fun count(): Int
}

interface ManagedResource {
    suspend fun release()
}

/**
 * Simple resource wrapper.
 */
class SimpleResource(
    private val onRelease: suspend () -> Unit
) : ManagedResource {
    override suspend fun release() {
        onRelease()
    }
}
```

#### 3.8.3 Usage Examples

**Register Resources:**

```kotlin
// Register database connection
val db = Database.connect("app.db")
resourceManager.register("database", SimpleResource {
    db.close()
    println("Database connection closed")
})

// Register file handle
val logFile = File("app.log").openWriter()
resourceManager.register("logFile", SimpleResource {
    logFile.close()
    println("Log file closed")
})

// Register network connection
val socket = Socket("server.com", 8080)
resourceManager.register("socket", SimpleResource {
    socket.close()
    println("Socket closed")
})
```

**Cleanup on Destroy:**

```kotlin
val observer = object : LifecycleObserver {
    override suspend fun onDestroy() {
        // Release all resources
        resourceManager.releaseAll()
        println("All resources released")
    }
}

lifecycle.addObserver(observer)
```

---

### 3.9 ComponentRegistry.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/registry/ComponentRegistry.kt`
**Lines:** 70
**Purpose:** Central registry for DSL components

#### 3.9.1 Architecture

The `ComponentRegistry` maintains a **thread-safe map** of component type names to their descriptors.

**Component Registration Flow:**

```
Built-in Components         Custom Components
       │                           │
       └───────────┬───────────────┘
                   │
                   ▼
         ┌──────────────────┐
         │ ComponentRegistry│
         │  (Singleton)     │
         └──────────────────┘
                   │
                   ├─> "Button"      → ButtonDescriptor
                   ├─> "Text"        → TextDescriptor
                   ├─> "ColorPicker" → ColorPickerDescriptor
                   ├─> "Container"   → ContainerDescriptor
                   └─> "CustomCard"  → CustomCardDescriptor (user-defined)
```

#### 3.9.2 Class Structure

```kotlin
/**
 * Central registry for DSL components.
 *
 * Maps component type names to their descriptors and factory functions.
 * Thread-safe using Mutex for concurrent access.
 */
class ComponentRegistry {
    private val mutex = Mutex()
    private val components = mutableMapOf<String, ComponentDescriptor>()

    suspend fun register(descriptor: ComponentDescriptor)
    suspend fun get(type: String): ComponentDescriptor?
    suspend fun getAll(): List<ComponentDescriptor>
    suspend fun isRegistered(type: String): Boolean
    suspend fun unregister(type: String): Boolean

    companion object {
        private var instance: ComponentRegistry? = null

        fun getInstance(): ComponentRegistry {
            return instance ?: synchronized(this) {
                instance ?: ComponentRegistry().also { instance = it }
            }
        }
    }
}
```

#### 3.9.3 Usage Examples

**Register Built-in Components:**

```kotlin
// At startup, register all built-in components
suspend fun registerBuiltInComponents(registry: ComponentRegistry) {
    registry.register(ComponentDescriptor(
        type = "Button",
        properties = mapOf(
            "text" to PropertyDescriptor("text", PropertyType.STRING, required = true),
            "enabled" to PropertyDescriptor("enabled", PropertyType.BOOLEAN, required = false)
        ),
        supportsChildren = false
    ))

    registry.register(ComponentDescriptor(
        type = "ColorPicker",
        properties = mapOf(
            "initialColor" to PropertyDescriptor("initialColor", PropertyType.STRING, required = true),
            "showAlpha" to PropertyDescriptor("showAlpha", PropertyType.BOOLEAN, required = false),
            "showHex" to PropertyDescriptor("showHex", PropertyType.BOOLEAN, required = false)
        ),
        supportsChildren = false
    ))
}
```

**Lookup Component:**

```kotlin
// During instantiation
val descriptor = registry.get("ColorPicker")
if (descriptor != null) {
    println("Found component: ${descriptor.type}")
    println("Properties: ${descriptor.properties.keys}")
} else {
    throw InstantiationException("Unknown component type: ColorPicker")
}
```

---

### 3.10 VosParser.kt

**Location:** `Universal/IDEAMagic/AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/dsl/VosParser.kt`
**Lines:** 561
**Purpose:** Recursive descent parser for VoiceOS DSL

#### 3.10.1 Grammar

```
app        → "App" "{" appBody "}"
appBody    → property* component* voiceCommands?
component  → IDENTIFIER "{" componentBody "}"
componentBody → property* callback* component*
property   → IDENTIFIER ":" value
callback   → IDENTIFIER ":" lambda
lambda     → "(" params ")" "=>" "{" statements "}"
value      → STRING | NUMBER | BOOLEAN | IDENTIFIER | lambda | list
list       → "[" value ("," value)* "]"
```

#### 3.10.2 Example DSL

```kotlin
App {
    id: "colorPicker.demo"
    name: "Color Picker Demo"
    runtime: "AvaUI"

    Container {
        id: "mainContainer"
        layout: "vertical"
        padding: 16

        Text {
            text: "Choose a color:"
            size: 20
            weight: "bold"
        }

        ColorPicker {
            id: "picker1"
            initialColor: "#FF5733"
            showAlpha: true
            showHex: true
            onColorChange: (color) => {
                state.lastColor = color
                display.updatePreview(color)
            }
        }

        Button {
            text: "Reset"
            onClick: () => {
                picker1.reset()
            }
        }
    }

    VoiceCommands {
        "open color picker" => ColorPicker.show
        "close picker" => ColorPicker.hide
        "reset color" => ColorPicker.reset
    }
}
```

#### 3.10.3 AST Output

```kotlin
VosAstNode.App(
    id = "colorPicker.demo",
    name = "Color Picker Demo",
    runtime = "AvaUI",
    components = [
        VosAstNode.Component(
            type = "Container",
            id = "mainContainer",
            properties = {
                "layout": VosValue.StringValue("vertical"),
                "padding": VosValue.IntValue(16)
            },
            children = [
                VosAstNode.Component(
                    type = "Text",
                    properties = {
                        "text": VosValue.StringValue("Choose a color:"),
                        "size": VosValue.IntValue(20),
                        "weight": VosValue.StringValue("bold")
                    }
                ),
                VosAstNode.Component(
                    type = "ColorPicker",
                    id = "picker1",
                    properties = {
                        "initialColor": VosValue.StringValue("#FF5733"),
                        "showAlpha": VosValue.BoolValue(true),
                        "showHex": VosValue.BoolValue(true)
                    },
                    callbacks = {
                        "onColorChange": VosLambda(
                            params = ["color"],
                            statements = [...]
                        )
                    }
                ),
                VosAstNode.Component(
                    type = "Button",
                    properties = {
                        "text": VosValue.StringValue("Reset")
                    },
                    callbacks = {
                        "onClick": VosLambda(
                            params = [],
                            statements = [...]
                        )
                    }
                )
            ]
        )
    ],
    voiceCommands = {
        "open color picker": "ColorPicker.show",
        "close picker": "ColorPicker.hide",
        "reset color": "ColorPicker.reset"
    }
)
```

#### 3.10.4 Parser Methods

```kotlin
class VosParser(private val tokens: List<Token>) {
    private var current = 0

    fun parse(): VosAstNode.App {
        return parseApp()
    }

    private fun parseApp(): VosAstNode.App {
        expect(TokenType.IDENTIFIER, "App")
        expect(TokenType.LBRACE)
        // Parse app body...
        expect(TokenType.RBRACE)
        return VosAstNode.App(...)
    }

    private fun parseComponent(): VosAstNode.Component {
        val type = expect(TokenType.IDENTIFIER).value
        expect(TokenType.LBRACE)
        // Parse component body...
        expect(TokenType.RBRACE)
        return VosAstNode.Component(...)
    }

    private fun parseProperty(): Pair<String, VosValue> {
        val name = expect(TokenType.IDENTIFIER).value
        expect(TokenType.COLON)
        val value = parseValue()
        return name to value
    }

    private fun parseValue(): VosValue {
        return when {
            check(TokenType.STRING) -> VosValue.StringValue(advance().value)
            check(TokenType.NUMBER) -> { /* parse int/float */ }
            check(TokenType.TRUE) -> VosValue.BoolValue(true)
            check(TokenType.FALSE) -> VosValue.BoolValue(false)
            check(TokenType.LPAREN) -> parseLambdaAsValue()
            check(TokenType.LBRACKET) -> parseListValue()
            else -> throw ParserException("Unexpected value")
        }
    }

    private fun parseLambda(): VosLambda {
        expect(TokenType.LPAREN)
        // Parse parameters...
        expect(TokenType.RPAREN)
        expect(TokenType.ARROW)
        expect(TokenType.LBRACE)
        // Parse statements...
        expect(TokenType.RBRACE)
        return VosLambda(params, statements)
    }
}
```

---

## 4. Integration Examples

### 4.1 Complete Workflow

```kotlin
/**
 * Complete AvaUI app lifecycle.
 */
suspend fun runAvaUIApp(dslSource: String) {
    // 1. Create runtime
    val runtime = AvaUIRuntime()

    // 2. Load app (parse DSL)
    val loaded = runtime.loadApp(dslSource)
    println("App loaded: ${loaded.app.name}")

    // 3. Start app (instantiate components)
    val running = runtime.start(loaded.app)
    println("App started with ${running.components.size} components")

    // 4. Handle voice commands
    val match = runtime.handleVoiceCommand("open color picker")
    if (match != null) {
        println("Command matched: ${match.command.trigger} (${match.confidence})")
    }

    // 5. Collect events
    launch {
        runtime.eventBus.events
            .filter { it.componentId == "picker1" }
            .collect { event ->
                println("Event: ${event.eventName} from ${event.componentId}")
                when (event.eventName) {
                    "onColorChange" -> {
                        val color = event.requireParameter<String>("color")
                        println("Color changed to: $color")
                    }
                }
            }
    }

    // 6. Simulate user interaction
    delay(5000)

    // 7. Pause app
    runtime.pause(running.app.id)
    println("App paused")

    // 8. Resume app
    delay(2000)
    runtime.resume(running.app.id)
    println("App resumed")

    // 9. Stop app
    delay(5000)
    runtime.stop(running.app.id)
    println("App stopped, resources released")
}
```

---

## 5. Performance Considerations

### 5.1 EventBus Backpressure

The `EventBus` uses `extraBufferCapacity = 100` to prevent blocking:

```kotlin
private val _events = MutableSharedFlow<ComponentEvent>(
    replay = 0,              // No caching
    extraBufferCapacity = 100 // Buffer 100 events
)
```

**Trade-offs:**
- ✅ Fast emitters don't block slow collectors
- ✅ Burst traffic handled gracefully
- ❌ Events can be dropped if buffer overflows
- ❌ Memory usage: ~8KB per event × 100 = 800KB buffer

**Recommendation:** Monitor `subscriptionCount` and adjust buffer size if needed.

### 5.2 Parser Optimization

The `VosParser` is a **recursive descent parser** with O(n) complexity (n = token count).

**Optimization opportunities:**
1. **Memoization**: Cache parsed subtrees (e.g., repeated component definitions)
2. **Parallel parsing**: Parse independent components concurrently
3. **Lazy parsing**: Defer child component parsing until accessed

### 5.3 Registry Thread Safety

The `ComponentRegistry` uses a `Mutex` for thread safety:

```kotlin
suspend fun register(descriptor: ComponentDescriptor) {
    mutex.withLock {
        components[descriptor.type] = descriptor
    }
}
```

**Trade-offs:**
- ✅ Thread-safe concurrent access
- ❌ Lock contention on high registration rate
- **Solution**: Pre-register all components at startup

---

## 6. Extension Points

### 6.1 Custom Components

**Register custom component:**

```kotlin
suspend fun registerCustomComponents(registry: ComponentRegistry) {
    registry.register(ComponentDescriptor(
        type = "CustomCard",
        properties = mapOf(
            "title" to PropertyDescriptor("title", PropertyType.STRING, required = true),
            "subtitle" to PropertyDescriptor("subtitle", PropertyType.STRING, required = false),
            "elevation" to PropertyDescriptor("elevation", PropertyType.FLOAT, required = false)
        ),
        supportsChildren = true
    ))
}
```

**Use in DSL:**

```kotlin
App {
    CustomCard {
        title: "My Card"
        subtitle: "Custom component example"
        elevation: 8.0

        Text { text: "Card content" }
    }
}
```

### 6.2 Custom Lifecycle Observers

```kotlin
val analyticsObserver = object : LifecycleObserver {
    override suspend fun onCreate() {
        analytics.logEvent("app_created")
    }

    override suspend fun onResume() {
        analytics.logEvent("app_resumed")
    }

    override suspend fun onDestroy() {
        analytics.logEvent("app_destroyed")
        analytics.flush()
    }
}

lifecycle.addObserver(analyticsObserver)
```

### 6.3 Custom Event Handlers

```kotlin
// Filter and transform events
launch {
    eventBus.events
        .filter { it.eventName == "onColorChange" }
        .map { event ->
            val color = event.requireParameter<String>("color")
            Color.parseHex(color)
        }
        .collect { color ->
            // Handle parsed color
            updateTheme(color)
        }
}
```

---

## 7. Summary

The **AvaUI Runtime** is a comprehensive system that orchestrates:

1. **Parsing** (VosParser) - Transforms DSL source into AST
2. **Registration** (ComponentRegistry) - Manages component metadata
3. **Instantiation** (ComponentInstantiator) - Creates Kotlin objects from AST
4. **Events** (EventBus) - Reactive event propagation with backpressure
5. **Voice** (VoiceCommandRouter + ActionDispatcher) - Voice command matching and execution
6. **Lifecycle** (AppLifecycle + StateManager + ResourceManager) - State management and cleanup

**Key Characteristics:**
- **Reactive**: Kotlin Flow for event streaming
- **Thread-safe**: Mutex-protected registries, concurrent-safe event bus
- **Extensible**: Custom components, observers, event handlers
- **Coroutine-based**: Non-blocking suspend functions throughout
- **Type-safe**: Validated property types, coercion pipeline

**Next Chapter:** Chapter 5 will dive into the **CodeGen Pipeline**, documenting the AST-to-native-code generation process for Android, iOS, and Web platforms.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
