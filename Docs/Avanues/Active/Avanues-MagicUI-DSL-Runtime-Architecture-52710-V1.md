# AvaUI DSL Runtime - Architecture Design

**Date**: 2025-10-27 11:45 PDT
**Status**: Design Phase
**Purpose**: Define architecture for AvaUI DSL interpreter/runtime engine

## Overview

AvaUI Runtime is the **Layer 2** interpreter that bridges **Layer 3** (MagicDSL apps) and **Layer 1** (Native KMP libraries).

```
.vos file (DSL) → AvaUI Runtime → Native Libraries → Platform UI
```

## Core Responsibilities

1. **Parse** .vos files into executable structure
2. **Instantiate** native library components
3. **Map** DSL properties to native object properties
4. **Route** voice commands to DSL actions
5. **Manage** app lifecycle (init/start/pause/resume/stop/destroy)
6. **Handle** events and callbacks
7. **Validate** DSL syntax and runtime behavior

## Architecture Components

### 1. DSL Parser

**Purpose**: Convert .vos file text → Abstract Syntax Tree (AST)

**Input**: String content from .vos file
**Output**: `VosAst` data structure

**Syntax Design** (YAML-like, but custom):

```yaml
#!vos:D
# Comments start with #

# Block structure
ComponentName {
  property: value
  property: "string value"
  property: 123
  property: true

  # Nested blocks
  ChildComponent {
    property: value
  }

  # Callbacks/Lambdas
  onEvent: (param) => {
    action(param)
  }

  # Arrays
  items: [item1, item2, item3]
}
```

**Classes Needed**:

```kotlin
// AST Node types
sealed class VosAstNode {
    data class Component(
        val name: String,
        val properties: Map<String, VosValue>,
        val children: List<Component>,
        val callbacks: Map<String, VosLambda>,
        val id: String? = null
    ) : VosAstNode()
}

sealed class VosValue {
    data class StringValue(val value: String) : VosValue()
    data class IntValue(val value: Int) : VosValue()
    data class FloatValue(val value: Float) : VosValue()
    data class BoolValue(val value: Boolean) : VosValue()
    data class ArrayValue(val items: List<VosValue>) : VosValue()
    data class ComponentRef(val id: String) : VosValue() // Reference to another component
}

data class VosLambda(
    val parameters: List<String>,
    val body: List<VosStatement>
)

sealed class VosStatement {
    data class FunctionCall(
        val target: String, // e.g., "VoiceOS.speak" or "noteEditor.append"
        val arguments: List<VosValue>
    ) : VosStatement()

    data class Assignment(
        val target: String,
        val value: VosValue
    ) : VosStatement()
}

// Parser result
data class VosParseResult(
    val root: VosAstNode.Component,
    val errors: List<VosParseError> = emptyList()
)

data class VosParseError(
    val line: Int,
    val column: Int,
    val message: String
)
```

**Parser Implementation** (Simplified):

```kotlin
class VosDslParser {
    fun parse(content: String): VosParseResult {
        val lines = content.lines()

        // Skip #!vos:D header
        val contentLines = lines.dropWhile { it.trim().startsWith("#!vos:") }

        // Tokenize
        val tokens = tokenize(contentLines)

        // Parse into AST
        val ast = parseComponent(tokens)

        return VosParseResult(ast)
    }

    private fun tokenize(lines: List<String>): List<Token> {
        // Lex the input into tokens
        // Handle: identifiers, strings, numbers, braces, colons, etc.
    }

    private fun parseComponent(tokens: List<Token>): VosAstNode.Component {
        // Recursive descent parser
        // Parse component structure
    }
}
```

### 2. Component Registry

**Purpose**: Map DSL component names to native KMP classes

**Responsibilities**:
- Register native library classes
- Provide factory methods for instantiation
- Handle versioning and compatibility
- Support plugin discovery

**Classes**:

```kotlin
// Registration entry
data class ComponentRegistration(
    val dslName: String,              // "ColorPicker"
    val factory: ComponentFactory,     // How to create it
    val version: String = "1.0.0"
)

// Factory interface
interface ComponentFactory {
    /**
     * Create a native component instance from DSL properties.
     */
    fun create(
        properties: Map<String, VosValue>,
        runtime: VosRuntime
    ): Any
}

// Registry
object ComponentRegistry {
    private val components = mutableMapOf<String, ComponentRegistration>()

    fun register(registration: ComponentRegistration) {
        components[registration.dslName] = registration
    }

    fun get(dslName: String): ComponentRegistration? {
        return components[dslName]
    }

    fun isRegistered(dslName: String): Boolean {
        return components.containsKey(dslName)
    }
}
```

**Example Registration**:

```kotlin
// In ColorPicker library initialization
ComponentRegistry.register(
    ComponentRegistration(
        dslName = "ColorPicker",
        factory = object : ComponentFactory {
            override fun create(
                properties: Map<String, VosValue>,
                runtime: VosRuntime
            ): Any {
                // Extract properties
                val initialColor = properties["initialColor"]?.let {
                    when (it) {
                        is VosValue.StringValue -> ColorRGBA.fromHexString(it.value)
                        else -> ColorRGBA.WHITE
                    }
                } ?: ColorRGBA.WHITE

                val mode = properties["mode"]?.let {
                    when (it) {
                        is VosValue.StringValue -> ColorPickerMode.valueOf(it.value)
                        else -> ColorPickerMode.FULL
                    }
                } ?: ColorPickerMode.FULL

                val showAlpha = properties["showAlpha"]?.let {
                    (it as? VosValue.BoolValue)?.value
                } ?: true

                // Create config
                val config = ColorPickerConfig(
                    mode = mode,
                    showAlpha = showAlpha
                )

                // Create picker
                return ColorPickerFactory.create(initialColor, config)
            }
        }
    )
)
```

### 3. Instantiation Engine

**Purpose**: Create native objects from parsed AST

**Responsibilities**:
- Traverse AST
- Instantiate components via registry
- Set properties
- Wire up callbacks
- Build component tree

**Classes**:

```kotlin
class VosInstantiator(
    private val registry: ComponentRegistry,
    private val runtime: VosRuntime
) {
    /**
     * Instantiate a component tree from AST.
     */
    fun instantiate(ast: VosAstNode.Component): VosComponentInstance {
        // Get factory from registry
        val registration = registry.get(ast.name)
            ?: throw VosRuntimeException("Unknown component: ${ast.name}")

        // Create native instance
        val nativeInstance = registration.factory.create(ast.properties, runtime)

        // Wire up callbacks
        val callbacks = wireCallbacks(ast.callbacks, nativeInstance)

        // Instantiate children
        val children = ast.children.map { instantiate(it) }

        return VosComponentInstance(
            id = ast.id ?: generateId(),
            dslName = ast.name,
            nativeInstance = nativeInstance,
            callbacks = callbacks,
            children = children
        )
    }

    private fun wireCallbacks(
        dslCallbacks: Map<String, VosLambda>,
        nativeInstance: Any
    ): Map<String, () -> Unit> {
        // Map DSL callbacks to Kotlin lambdas
        // Use reflection or explicit mapping
        return dslCallbacks.mapValues { (name, lambda) ->
            {
                // Execute lambda in runtime
                runtime.executeLambda(lambda, nativeInstance)
            }
        }
    }
}

data class VosComponentInstance(
    val id: String,
    val dslName: String,
    val nativeInstance: Any,
    val callbacks: Map<String, () -> Unit>,
    val children: List<VosComponentInstance>
)
```

### 4. Runtime Execution Engine

**Purpose**: Execute DSL code (lambdas, statements)

**Responsibilities**:
- Execute lambda bodies
- Resolve component references
- Call functions on native objects
- Handle variables and scope

**Classes**:

```kotlin
class VosRuntime {
    private val components = mutableMapOf<String, VosComponentInstance>()
    private val variables = mutableMapOf<String, Any?>()

    fun registerComponent(instance: VosComponentInstance) {
        components[instance.id] = instance
    }

    fun getComponent(id: String): VosComponentInstance? {
        return components[id]
    }

    /**
     * Execute a DSL lambda.
     */
    fun executeLambda(lambda: VosLambda, context: Any? = null) {
        // Execute each statement in lambda body
        lambda.body.forEach { statement ->
            executeStatement(statement, context)
        }
    }

    private fun executeStatement(statement: VosStatement, context: Any?) {
        when (statement) {
            is VosStatement.FunctionCall -> {
                executeFunctionCall(statement, context)
            }
            is VosStatement.Assignment -> {
                executeAssignment(statement)
            }
        }
    }

    private fun executeFunctionCall(call: VosStatement.FunctionCall, context: Any?) {
        // Parse target: "VoiceOS.speak", "noteEditor.append", etc.
        val parts = call.target.split(".")
        val target = parts[0]
        val method = parts[1]

        when (target) {
            "VoiceOS" -> {
                // Built-in system functions
                when (method) {
                    "speak" -> {
                        val text = (call.arguments[0] as? VosValue.StringValue)?.value
                        // TODO: Call actual TTS
                        println("VoiceOS.speak: $text")
                    }
                }
            }
            else -> {
                // Component method call
                val component = getComponent(target)?.nativeInstance
                if (component != null) {
                    // Use reflection or explicit mapping
                    callMethod(component, method, call.arguments)
                }
            }
        }
    }

    private fun executeAssignment(assignment: VosStatement.Assignment) {
        variables[assignment.target] = resolveValue(assignment.value)
    }

    private fun resolveValue(value: VosValue): Any? {
        return when (value) {
            is VosValue.StringValue -> value.value
            is VosValue.IntValue -> value.value
            is VosValue.FloatValue -> value.value
            is VosValue.BoolValue -> value.value
            is VosValue.ArrayValue -> value.items.map { resolveValue(it) }
            is VosValue.ComponentRef -> getComponent(value.id)
        }
    }

    private fun callMethod(target: Any, method: String, arguments: List<VosValue>) {
        // TODO: Reflection or explicit mapping
        // For now, handle known cases
        when (target) {
            is ColorPickerView -> {
                when (method) {
                    "show" -> target.show()
                    "hide" -> target.hide()
                }
            }
            // Add more types as needed
        }
    }
}
```

### 5. Voice Command Router

**Purpose**: Map voice input to DSL actions

**Classes**:

```kotlin
data class VoiceCommand(
    val trigger: String,              // "change color", "save note"
    val action: VosStatement          // What to do
)

class VoiceCommandRouter(private val runtime: VosRuntime) {
    private val commands = mutableListOf<VoiceCommand>()

    fun register(command: VoiceCommand) {
        commands.add(command)
    }

    /**
     * Handle voice input from user.
     */
    fun handleVoiceInput(input: String): Boolean {
        // Find matching command
        val command = commands.firstOrNull { cmd ->
            input.lowercase().contains(cmd.trigger.lowercase())
        }

        if (command != null) {
            runtime.executeStatement(command.action, null)
            return true
        }

        return false
    }
}
```

### 6. Lifecycle Manager

**Purpose**: Manage VoiceOS app lifecycle

**Classes**:

```kotlin
enum class VosAppState {
    CREATED,
    STARTED,
    PAUSED,
    RESUMED,
    STOPPED,
    DESTROYED
}

class VosAppLifecycle(
    private val rootComponent: VosComponentInstance,
    private val runtime: VosRuntime
) {
    private var state = VosAppState.CREATED

    fun start() {
        if (state == VosAppState.CREATED) {
            // Initialize all components
            initializeComponents(rootComponent)
            state = VosAppState.STARTED
        }
    }

    fun pause() {
        if (state == VosAppState.STARTED || state == VosAppState.RESUMED) {
            // Pause components (e.g., hide ColorPicker)
            state = VosAppState.PAUSED
        }
    }

    fun resume() {
        if (state == VosAppState.PAUSED) {
            state = VosAppState.RESUMED
        }
    }

    fun stop() {
        if (state != VosAppState.DESTROYED) {
            state = VosAppState.STOPPED
        }
    }

    fun destroy() {
        // Cleanup all components
        destroyComponents(rootComponent)
        state = VosAppState.DESTROYED
    }

    private fun initializeComponents(instance: VosComponentInstance) {
        // Initialize native instance if needed
        // Recursively initialize children
        instance.children.forEach { initializeComponents(it) }
    }

    private fun destroyComponents(instance: VosComponentInstance) {
        // Dispose native instance
        when (val native = instance.nativeInstance) {
            is ColorPickerView -> native.dispose()
            // Add more types
        }

        // Recursively destroy children
        instance.children.forEach { destroyComponents(it) }
    }
}
```

## Complete Example

**Input**: colorNotes.vos

```yaml
#!vos:D

App {
  id: "colorNotesApp"
  name: "Color Notes"
  version: "1.0.0"

  Screen {
    id: "mainScreen"
    title: "My Notes"

    ColorPicker {
      id: "themePicker"
      initialColor: "#FF5722"
      mode: "DESIGNER"
      showAlpha: true

      onConfirm: (color) => {
        VoiceOS.speak("Color updated!")
        noteEditor.setBackgroundColor(color)
      }
    }

    Notepad {
      id: "noteEditor"
      placeholder: "Type or speak..."

      onVoiceInput: (text) => {
        noteEditor.append(text)
      }
    }

    Button {
      id: "saveButton"
      text: "Save Note"

      onClick: () => {
        saveNote(noteEditor.getText())
        VoiceOS.speak("Note saved!")
      }
    }
  }

  VoiceCommands {
    "change color" => themePicker.show()
    "save note" => saveButton.click()
    "clear note" => noteEditor.clear()
  }
}
```

**Processing**:

```kotlin
fun main() {
    // 1. Read file
    val vosFile = VosFile.read("colorNotes.vos")
    val content = vosFile.contentWithoutHeader

    // 2. Parse
    val parser = VosDslParser()
    val parseResult = parser.parse(content)

    if (parseResult.errors.isNotEmpty()) {
        parseResult.errors.forEach { println("Error at ${it.line}:${it.column} - ${it.message}") }
        return
    }

    // 3. Create runtime
    val runtime = VosRuntime()

    // 4. Instantiate components
    val instantiator = VosInstantiator(ComponentRegistry, runtime)
    val appInstance = instantiator.instantiate(parseResult.root)

    runtime.registerComponent(appInstance)

    // 5. Setup voice commands
    val voiceRouter = VoiceCommandRouter(runtime)
    val voiceCommandsNode = parseResult.root.children.find { it.name == "VoiceCommands" }
    voiceCommandsNode?.properties?.forEach { (trigger, action) ->
        voiceRouter.register(VoiceCommand(trigger, action as VosStatement))
    }

    // 6. Start lifecycle
    val lifecycle = VosAppLifecycle(appInstance, runtime)
    lifecycle.start()

    // App is now running!
    // Voice input: "change color" → Opens ColorPicker
    voiceRouter.handleVoiceInput("change color")
}
```

## Implementation Plan

### Phase 1: Parser Foundation (~3 hours)
- [ ] Define AST data classes
- [ ] Build tokenizer
- [ ] Build recursive descent parser
- [ ] Handle basic syntax (components, properties)
- [ ] Unit tests for parser

### Phase 2: Component Registry (~2 hours)
- [ ] Create ComponentRegistration
- [ ] Create ComponentFactory interface
- [ ] Build ComponentRegistry
- [ ] Register ColorPicker
- [ ] Register Preferences
- [ ] Unit tests

### Phase 3: Instantiation Engine (~3 hours)
- [ ] Create VosInstantiator
- [ ] Property mapping logic
- [ ] Callback wiring
- [ ] Component tree building
- [ ] Integration tests

### Phase 4: Runtime Execution (~2 hours)
- [ ] Create VosRuntime
- [ ] Statement execution
- [ ] Function call resolution
- [ ] Variable scope
- [ ] Tests

### Phase 5: Voice Commands (~2 hours)
- [ ] Create VoiceCommandRouter
- [ ] Command registration
- [ ] Input matching
- [ ] Action execution
- [ ] Tests

### Phase 6: Lifecycle (~1 hour)
- [ ] Create VosAppLifecycle
- [ ] State management
- [ ] Component init/destroy
- [ ] Tests

### Phase 7: Integration (~2 hours)
- [ ] End-to-end test with ColorPicker
- [ ] Example .vos apps
- [ ] Performance testing
- [ ] Documentation

**Total: ~15 hours** spread across 2-3 sessions

## Testing Strategy

**Unit Tests**:
- Parser: 20+ tests
- Registry: 10+ tests
- Instantiator: 15+ tests
- Runtime: 20+ tests
- Voice router: 10+ tests
- Lifecycle: 8+ tests

**Integration Tests**:
- Parse + Instantiate ColorPicker
- Parse + Instantiate Notepad
- Full app execution
- Voice command handling
- Lifecycle transitions

**Example Apps** (for testing):
1. Simple ColorPicker app
2. ColorPicker + Notepad app
3. Multi-screen app
4. Voice-heavy app

## Next Steps

1. **Create directory structure**:
   ```
   AvaUI/src/commonMain/kotlin/com/augmentalis/voiceos/avaui/
   ├── dsl/
   │   ├── ast/          # AST data classes
   │   ├── parser/       # Parser implementation
   │   └── lexer/        # Tokenizer
   ├── runtime/
   │   ├── VosRuntime.kt
   │   ├── VosInstantiator.kt
   │   ├── ComponentRegistry.kt
   │   ├── VoiceCommandRouter.kt
   │   └── VosAppLifecycle.kt
   └── factory/
       ├── ComponentFactory.kt
       └── BuiltinFactories.kt
   ```

2. **Start with Phase 1**: AST + Parser

3. **Test incrementally**: Each phase has working tests

4. **Integrate early**: Use ColorPicker to validate design

---

**Ready to implement!** This architecture provides a solid foundation for the AvaUI DSL runtime.

**Created by Manoj Jhawar, manoj@ideahq.net**
