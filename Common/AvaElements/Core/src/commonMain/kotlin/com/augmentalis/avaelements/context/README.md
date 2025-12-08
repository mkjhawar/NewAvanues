# Universal Screen Hierarchy System

**Version:** 1.0.0
**Created:** 2025-12-06
**Status:** ACTIVE

---

## Overview

The Universal Screen Hierarchy System provides **context-aware voice command processing** and **AI integration** for MagicUI applications. It captures the complete screen structure, quantizes information for AI/NLU systems, and enables intelligent command disambiguation.

### Key Features

| Feature | Description |
|---------|-------------|
| **Cross-Platform** | Works on Android, iOS, Web, Desktop |
| **AI-Ready** | Optimized context for NLU and LLM systems |
| **Voice-First** | Automatic voice command extraction and resolution |
| **Hierarchical** | Preserves component relationships for complex UIs |
| **Context-Aware** | Disambiguates commands using screen state |
| **Serializable** | AVU format export for storage and transmission |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    MagicUI Component Tree                    │
│              (Button, TextField, Container, etc.)            │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│           MagicUIHierarchyCapture                            │
│  • Recursive tree traversal                                 │
│  • Voice label extraction                                   │
│  • Screen type inference                                    │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              ScreenHierarchy (Core Data Model)               │
│  • ComponentNode tree                                        │
│  • CommandableElements (voice targets)                      │
│  • FormFields, Actions, DataElements                        │
└──────────────────────────┬──────────────────────────────────┘
                           ▼
         ┌─────────────────┼─────────────────┐
         ▼                 ▼                 ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ ScreenQuant  │  │ AIContext    │  │ Serializer   │
│ izer         │  │ Provider     │  │              │
├──────────────┤  ├──────────────┤  ├──────────────┤
│ NLU Context  │  │ Command      │  │ AVU Export   │
│ LLM Summary  │  │ Resolution   │  │ Storage      │
│ Entity Graph │  │ History      │  │ Network      │
└──────────────┘  └──────────────┘  └──────────────┘
```

---

## Core Components

### 1. ScreenHierarchy

Complete representation of screen state.

```kotlin
data class ScreenHierarchy(
    val screenId: String,
    val screenType: ScreenType,
    val root: ComponentNode,
    val commandableElements: List<CommandableElement>,
    val formFields: List<FormField>,
    val actions: List<ActionElement>,
    val complexity: ComplexityScore
)
```

### 2. ComponentNode

Hierarchical component tree node.

```kotlin
data class ComponentNode(
    val id: String,
    val type: String,
    val role: ComponentRole,
    val voiceLabel: String?,
    val voiceCommands: List<String>,
    val children: List<ComponentNode>,
    val isInteractive: Boolean
)
```

### 3. CommandableElement

Voice-targetable UI element.

```kotlin
data class CommandableElement(
    val id: String,
    val voiceLabel: String,
    val componentType: String,
    val primaryCommand: String,
    val alternateCommands: List<String>,
    val priority: Int
)
```

---

## Quick Start

### Basic Usage

```kotlin
import com.augmentalis.avaelements.context.*

// 1. Initialize capture
val capture = MagicUIHierarchyCapture()
val aiProvider = AIContextProvider()

// 2. Define app context
val appContext = AppContext(
    appId = "com.myapp",
    appName = "My App",
    packageName = "com.myapp"
)

// 3. Capture screen hierarchy (in your Composable)
@Composable
fun MyScreen() {
    Column {
        Text("Welcome")
        TextField(value = email, placeholder = "Email")
        Button(text = "Submit", onClick = { submit() })
    }

    // Capture hierarchy after composition
    LaunchedEffect(Unit) {
        val rootComponent = getCurrentRootComponent()
        val hierarchy = capture.capture(rootComponent, appContext)
        aiProvider.updateScreen(hierarchy)
    }
}

// 4. Process voice commands
fun processVoiceCommand(spokenText: String) {
    val nluContext = aiProvider.getContextForNLU()
    val resolution = aiProvider.resolveCommand(
        command = "click",
        parameters = mapOf("target" to "submit")
    )

    when (resolution) {
        is CommandResolution.Success -> {
            executeCommand(resolution.elementId)
        }
        is CommandResolution.Ambiguous -> {
            askUserToClarify(resolution.suggestions)
        }
        is CommandResolution.Failed -> {
            handleFailure(resolution.reason)
        }
    }
}
```

---

## Usage Examples

### Example 1: Voice Command Resolution

```kotlin
// Setup
val aiProvider = AIContextProvider()

// Voice command: "click submit button"
val resolution = aiProvider.resolveCommand(
    command = "click",
    parameters = mapOf("target" to "submit button")
)

when (resolution) {
    is CommandResolution.Success -> {
        println("Resolved to: ${resolution.elementId}")
        println("Confidence: ${resolution.confidence}")
        // Execute: findComponentById(resolution.elementId).click()
    }

    is CommandResolution.Ambiguous -> {
        println("Multiple matches found:")
        resolution.suggestions.forEach {
            println("  - ${it.label} (${it.confidence})")
        }
        println("Best match: ${resolution.bestMatch}")
    }

    is CommandResolution.Failed -> {
        println("Failed: ${resolution.reason}")
    }
}
```

### Example 2: NLU Integration

```kotlin
// Get NLU context for intent recognition
val nluContext = aiProvider.getContextForNLU()

// NLU context includes:
// - screen: ScreenInfo
// - commands: List<Command>
// - entities: List<Entity>
// - formMode: Boolean
// - intents: List<Intent>

// Send to NLU service in AVU format
val nluAvu = ScreenHierarchySerializer.toNLUAvu(nluContext)
val intent = nluService.recognizeIntent(spokenText, nluAvu)

// Process intent
when (intent.name) {
    "submit_form" -> submitForm()
    "navigate_back" -> navigateBack()
    "fill_field" -> fillField(intent.slots["field_name"])
}
```

### Example 3: LLM Integration

```kotlin
// Get LLM context for AI assistance
val llmContext = aiProvider.getContextForLLM(maxTokens = 200)

// Example output:
// "Login screen with 2 text fields (email field, password field),
//  and 2 action buttons. Purpose: User authentication.
//  Voice commands: 'email field', 'password field', 'submit button'."

// Use with LLM
val response = llm.query("""
    Context: $llmContext

    User question: What can I do on this screen?
""")

println(response)
// Output: "You can enter your email and password, then click
//          the submit button to log in."
```

### Example 4: Screen Analysis

```kotlin
val hierarchy = currentHierarchy.value ?: return

// Analyze screen
println("Screen Type: ${hierarchy.screenType.displayName}")
println("Purpose: ${hierarchy.screenPurpose}")
println("Complexity: ${hierarchy.complexity.getLevel()}")

// Get statistics
println("Total Components: ${hierarchy.complexity.totalComponents}")
println("Interactive: ${hierarchy.getInteractiveCount()}")
println("Form Fields: ${hierarchy.formFields.size}")
println("Actions: ${hierarchy.actions.size}")

// Get available commands
val commands = hierarchy.getAvailableCommands()
println("Voice Commands: ${commands.joinToString(", ")}")
```

### Example 5: Export to AVU Format

```kotlin
// Export full hierarchy as AVU
val avu = ScreenHierarchySerializer.toAvu(hierarchy, pretty = true)
saveToFile("screen_hierarchy.avu", avu)

// Export quantized (AI-optimized)
val quantizedAvu = ScreenHierarchySerializer.toQuantizedAvu(
    hierarchy,
    pretty = false
)
sendToServer(quantizedAvu)

// Export component tree only
val treeAvu = ScreenHierarchySerializer.componentTreeToAvu(
    hierarchy.root,
    pretty = true
)
```

---

## VoiceOS Integration

For Android VoiceOS integration:

```kotlin
import com.augmentalis.voiceos.screenhierarchy.*

// Initialize adapter
val adapter = VoiceOSScreenHierarchyAdapter(
    accessibilityService = myAccessibilityService,
    defaultAppContext = AppContext(
        appId = "com.voiceos.app",
        appName = "VoiceOS",
        packageName = "com.voiceos"
    )
)

// Capture from MagicUI
adapter.captureFromMagicUI(rootComponent)

// Get contexts
val nluContext = adapter.getNLUContext()
val llmContext = adapter.getLLMContext()

// Resolve commands
val resolution = adapter.resolveVoiceCommand("click submit")

// Export to AVU format
val avu = adapter.exportToAVU()

// Statistics
val stats = adapter.getStatistics()
println("Screen: ${stats.screenType}")
println("Complexity: ${stats.complexity}")
println("Commands: ${stats.commandableElements}")
```

---

## API Reference

### ScreenQuantizer

| Method | Purpose | Returns |
|--------|---------|---------|
| `quantize(hierarchy)` | Generate AI context | `QuantizedScreen` |
| `generateSummary(hierarchy)` | Natural language summary | `String` |
| `generateCompactSummary(hierarchy)` | Compact summary | `String` |
| `extractEntities(hierarchy)` | Extract entities for NLU | `List<Entity>` |
| `generateIntentSchema(hierarchy)` | Generate intent definitions | `IntentSchema` |

### AIContextProvider

| Method | Purpose | Returns |
|--------|---------|---------|
| `updateScreen(hierarchy)` | Update current screen | `Unit` |
| `getContextForNLU()` | Get NLU context | `NLUContext` |
| `getContextForLLM(maxTokens)` | Get LLM context | `String` |
| `getCompactContext()` | Get compact context | `String` |
| `resolveCommand(command, params)` | Resolve voice command | `CommandResolution` |
| `getHistory()` | Get screen history | `List<ScreenHierarchy>` |
| `getStatistics()` | Get statistics | `ContextStatistics` |

### MagicUIHierarchyCapture

| Method | Purpose | Returns |
|--------|---------|---------|
| `capture(component, appContext)` | Capture hierarchy | `ScreenHierarchy` |

---

## Best Practices

### 1. Update Frequency

```kotlin
// Update on significant changes only
LaunchedEffect(screenState) {
    if (screenState.isSignificantChange()) {
        val hierarchy = capture.capture(root, appContext)
        aiProvider.updateScreen(hierarchy)
    }
}
```

### 2. Voice Label Quality

```kotlin
// Good: Descriptive voice labels
TextField(
    value = email,
    placeholder = "Email",
    contentDescription = "email address field"  // ✓ Clear
)

// Bad: Generic labels
TextField(
    value = email,
    contentDescription = "input"  // ✗ Too vague
)
```

### 3. Command Resolution

```kotlin
// Always provide fallback
val resolution = aiProvider.resolveCommand("click", params)

when (resolution) {
    is CommandResolution.Success -> execute(resolution.elementId)
    is CommandResolution.Ambiguous -> {
        // Use best match if confidence high enough
        if (resolution.bestConfidence > 0.8f) {
            execute(resolution.bestMatch)
        } else {
            askUserToClarify(resolution.suggestions)
        }
    }
    is CommandResolution.Failed -> {
        // Fallback to LLM
        val llmResponse = llm.query("$llmContext\nUser said: $command")
        handleLLMResponse(llmResponse)
    }
}
```

---

## Performance

| Metric | Typical Value |
|--------|---------------|
| Hierarchy Capture | 5-20ms |
| Quantization | 1-5ms |
| Command Resolution | <1ms |
| AVU Serialization | 2-10ms |

### Optimization Tips

1. **Cache quantized context**: Re-quantize only on screen change
2. **Limit tree depth**: Set `maxDepth` in capture (default: 20)
3. **Use compact context**: For token-constrained scenarios
4. **Batch updates**: Don't capture on every keystroke

---

## Troubleshooting

| Issue | Solution |
|-------|----------|
| Voice commands not resolving | Check `contentDescription` on components |
| Incorrect screen type | Add keywords to component text/labels |
| High complexity score | Reduce nesting depth or component count |
| Missing commandable elements | Ensure interactive components have `voiceLabel` |

---

## Related Documentation

- **MagicUI Component System**: `Universal/Libraries/AvaElements/docs/AVA-MAGICUI-SYSTEM-ARCHITECTURE.md`
- **VoiceOS ScreenContext**: `android/apps/voiceos/docs/modules/VoiceOSCore/reference/api/ScreenContext-API-Reference-251018-2252.md`
- **Voice Integration**: `modules/AVAMagic/VoiceIntegration/README.md`

---

**Version:** 1.0.0
**Last Updated:** 2025-12-06
**Author:** IDEACODE v10.3
**Status:** Production Ready
