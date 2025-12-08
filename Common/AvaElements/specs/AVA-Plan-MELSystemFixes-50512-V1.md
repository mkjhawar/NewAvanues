# Implementation Plan: MEL System Critical Fixes

**Version:** 1.0.0 | **Date:** 2025-12-05 | **Status:** Ready for YOLO+SWARM

---

## Overview

| Property | Value |
|----------|-------|
| Platforms | KMP Core, Android, iOS, Desktop, Web |
| Total Tasks | 17 |
| Critical Blockers | 11 |
| Swarm Recommended | **YES** (5 platforms, 17 tasks) |

---

## CoT Reasoning: Phase Ordering

### Why This Order?

```
Phase 1 (Core) → Must compile before any rendering works
    ↓
Phase 2 (Rendering) → UI won't show until Phase 1 complete
    ↓
Phase 3 (Features) → Enhancements need working rendering
    ↓
Phase 4 (Optimization) → Polish after features complete
```

### Dependency Analysis

| Task | Depends On | Blocks |
|------|------------|--------|
| ReactiveRenderer constructor | None | All rendering |
| onStateChanged() | ReactiveRenderer | UI updates |
| UINode/Reducer constructors | None | Plugin loading |
| Missing imports | None | Compilation |
| ReducerResult merge | None | Reducer execution |
| getRootUINode() | Phase 1 complete | Platform rendering |
| EventBinder wiring | Phase 1 complete | User interactions |
| parseExpression() | Phase 1 complete | State bindings |
| Named params | Phase 1 complete | Reducer calls |
| array.filter/map | None (parallel) | Advanced plugins |
| Icon/Image | getRootUINode() | Visual components |
| Button onClick | EventBinder | Button functionality |
| Component types | getRootUINode() | Full component support |
| EffectExecutor | Reducer execution | Tier 2 effects |
| Selective re-render | onStateChanged() | Performance |
| StateObserver cleanup | Phase 2 complete | Code quality |
| Integration tests | All fixes | Validation |

---

## Swarm Configuration

### Agent Distribution

| Agent | Phase | Tasks | Files |
|-------|-------|-------|-------|
| **Agent 1** | Phase 1 | 1, 2, 5 | ReactiveRenderer.kt, PluginRuntime.kt, Reducer.kt |
| **Agent 2** | Phase 1 | 3, 4 | CompactMELParser.kt, UINode.kt |
| **Agent 3** | Phase 2 | 6, 7 | Android/Desktop MELPluginRenderer.kt, ReactiveRenderer.kt |
| **Agent 4** | Phase 2 | 8, 9 | BindingResolver.kt, EventBinder.kt, platform factories |
| **Agent 5** | Phase 3 | 10, 14 | ArrayFunctions.kt, EffectExecutor (ReducerEngine.kt) |
| **Agent 6** | Phase 3 | 11, 12, 13 | Android MELComponentFactory.kt, platform factories |

### Parallel Execution Strategy

```
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 1 (Parallel - 2 agents)                                   │
├─────────────────────────────────────────────────────────────────┤
│ Agent 1: ReactiveRenderer + onStateChanged + ReducerResult      │
│ Agent 2: CompactMELParser + imports                             │
└─────────────────────────────────────────────────────────────────┘
          ↓ (wait for completion)
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 2 (Parallel - 2 agents)                                   │
├─────────────────────────────────────────────────────────────────┤
│ Agent 3: getRootUINode + EventBinder wiring                     │
│ Agent 4: parseExpression + named params                         │
└─────────────────────────────────────────────────────────────────┘
          ↓ (wait for completion)
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 3 (Parallel - 2 agents)                                   │
├─────────────────────────────────────────────────────────────────┤
│ Agent 5: array.filter/map + EffectExecutor                      │
│ Agent 6: Icon/Image + Button + components                       │
└─────────────────────────────────────────────────────────────────┘
          ↓ (wait for completion)
┌─────────────────────────────────────────────────────────────────┐
│ PHASE 4 (Sequential - main agent)                               │
├─────────────────────────────────────────────────────────────────┤
│ Selective re-render + StateObserver + Integration tests         │
└─────────────────────────────────────────────────────────────────┘
```

---

## Phase 1: Unblock Compilation

### Task 1.1: Fix ReactiveRenderer Constructor

**File:** `Core/src/commonMain/kotlin/.../mel/ReactiveRenderer.kt`

**Current (line 43):**
```kotlin
class ReactiveRenderer(
    private val state: PluginState,
    private val resolver: BindingResolver,
    private val componentFactory: ComponentFactory
)
```

**Required:**
```kotlin
class ReactiveRenderer(
    private val uiRoot: UINode,
    private var state: PluginState,
    private val reducerEngine: ReducerEngine,
    private val tier: PluginTier,
    private val resolver: BindingResolver,
    private val componentFactory: ComponentFactory,
    private val onDispatch: (String, Map<String, Any>) -> Unit
)
```

**Also update:** PluginRuntime.kt lines 107-113 to match

---

### Task 1.2: Add onStateChanged() Method

**File:** `Core/src/commonMain/kotlin/.../mel/ReactiveRenderer.kt`

**Add after line 130:**
```kotlin
/**
 * Called when state changes. Computes affected nodes and triggers re-render.
 */
fun onStateChanged(newState: PluginState, changedPaths: Set<String>) {
    state = newState
    if (changedPaths.isEmpty()) {
        rerender()
    } else {
        rerenderAffected(changedPaths)
    }
}
```

---

### Task 1.3: Fix UINode/Reducer Constructors in CompactMELParser

**File:** `Core/src/commonMain/kotlin/.../mel/CompactMELParser.kt`

**UINode fix (line 728-735):**
```kotlin
// BEFORE:
return UINode(
    type = fullType,
    id = id,
    properties = properties,  // WRONG
    style = style,            // WRONG
    callbacks = callbacks,    // WRONG
    children = children
)

// AFTER:
return UINode(
    type = fullType,
    props = properties + style.mapValues { JsonPrimitive(it.value.toString()) },
    bindings = properties.filterValues { it is String && it.startsWith("$") }
        .mapValues { it.value.toString() },
    events = callbacks,
    children = children,
    id = id
)
```

**Reducer fix (line 431-435):**
```kotlin
// BEFORE:
Reducer(
    name = name,
    params = params,
    assignments = assignments
)

// AFTER:
Reducer(
    params = params,
    next_state = assignments,
    effects = emptyList()
)
```

---

### Task 1.4: Add Missing Imports

**File:** `Core/src/commonMain/kotlin/.../mel/PluginRuntime.kt`

**Add imports:**
```kotlin
import com.augmentalis.magicelements.core.mel.functions.Platform
import com.augmentalis.magicelements.core.mel.TierDetector
import com.augmentalis.magicelements.core.mel.PluginValidator
```

**Create if missing:** `PluginValidator.kt`, `Platform.kt` (expect/actual)

---

### Task 1.5: Merge Duplicate ReducerResult

**Action:** Keep definition in `Reducer.kt`, remove from `PluginRuntime.kt`

**Reducer.kt (keep):**
```kotlin
data class ReducerResult(
    val stateUpdates: Map<String, JsonElement>,
    val effects: List<Effect> = emptyList()
)
```

**PluginRuntime.kt (delete lines 458-461):** Remove duplicate class

---

## Phase 2: Unblock Rendering

### Task 2.1: Fix Android/Desktop getRootUINode()

**Android File:** `Renderers/Android/.../MELPluginRenderer.kt`
**Desktop File:** `Renderers/Desktop/.../MELPluginRenderer.kt`

**Replace (line 124-131):**
```kotlin
// BEFORE:
private fun getRootUINode(): UINode {
    return UINode(
        type = "Text",
        props = mapOf(),
        bindings = mapOf("content" to "Plugin rendering...")
    )
}

// AFTER:
private fun getRootUINode(): UINode {
    return runtime.getUIRoot()
}
```

---

### Task 2.2: Wire EventBinder into renderNode()

**File:** `Core/src/commonMain/kotlin/.../mel/ReactiveRenderer.kt`

**Update renderNode() method:**
```kotlin
private fun renderNode(node: UINode): Component {
    val resolvedProps = resolver.resolve(node)
    val eventHandlers = eventBinder.bind(node)  // ADD THIS
    val childComponents = node.children?.map { renderNode(it) }

    return componentFactory.create(
        type = node.type,
        props = resolvedProps,
        callbacks = eventHandlers,  // ADD THIS
        children = childComponents,
        id = node.id
    )
}
```

**Add field to ReactiveRenderer:**
```kotlin
private val eventBinder = EventBinder(reducerEngine, onDispatch)
```

---

### Task 2.3: Fix BindingResolver.parseExpression()

**File:** `Core/src/commonMain/kotlin/.../mel/BindingResolver.kt`

**Fix line 93:**
```kotlin
// BEFORE:
val ast = parser.parse(expression)  // Wrong - parser expects tokens

// AFTER:
val lexer = ExpressionLexer()
val tokens = lexer.tokenize(expression)
val ast = parser.parse(tokens)
```

**Add import:**
```kotlin
import com.augmentalis.magicelements.core.mel.ExpressionLexer
```

---

### Task 2.4: Fix Parameter Parsing for Named Params

**Files:**
- iOS: `Renderers/iOS/.../MELPluginRenderer.kt` (line 288)
- Android: `Renderers/Android/.../MELPluginRenderer.kt` (line 189-203)
- Web: `Renderers/Web/src/mel/MELComponentFactory.tsx` (line 94-106)

**Fix pattern:**
```kotlin
// BEFORE:
"param$index" to value  // Creates: param0, param1

// AFTER:
// Get reducer param names from runtime
val reducer = runtime.getReducer(reducerName)
val paramNames = reducer?.params ?: emptyList()
if (index < paramNames.size) {
    paramNames[index] to value
} else {
    "param$index" to value  // Fallback
}
```

---

## Phase 3: Complete Features

### Task 3.1: Implement array.filter and array.map

**File:** `Core/src/commonMain/kotlin/.../mel/functions/ArrayFunctions.kt`

**Add implementations:**
```kotlin
// array.filter
"filter" to MELFunction(
    name = "array.filter",
    tier = PluginTier.DATA,
    minArgs = 2,
    maxArgs = 2
) { args ->
    val array = TypeCoercion.toList(args[0])
    val predicate = args[1] as? String
        ?: throw MELFunctionException("array.filter requires predicate expression")

    // For Tier 1, predicate must be a property path
    array.filter { item ->
        val value = getNestedValue(item, predicate)
        TypeCoercion.toBoolean(value)
    }
}

// array.map
"map" to MELFunction(
    name = "array.map",
    tier = PluginTier.LOGIC,  // Tier 2 - requires expression evaluation
    minArgs = 2,
    maxArgs = 2
) { args ->
    val array = TypeCoercion.toList(args[0])
    val mapper = args[1] as? String
        ?: throw MELFunctionException("array.map requires mapper expression")

    array.map { item ->
        // Evaluate mapper expression with $item bound
        evaluateMapper(item, mapper)
    }
}
```

---

### Task 3.2: Replace Icon/Image Placeholders

**File:** `Renderers/Android/.../MELComponentFactory.kt`

**Icon (line 261-266):**
```kotlin
@Composable
private fun CreateIcon(props: Map<String, JsonElement>) {
    val iconName = props["name"]?.jsonPrimitive?.contentOrNull ?: "star"
    val size = props["size"]?.jsonPrimitive?.intOrNull ?: 24
    val color = props["color"]?.jsonPrimitive?.contentOrNull?.let { parseColor(it) }
        ?: MaterialTheme.colorScheme.onSurface

    Icon(
        imageVector = getIconByName(iconName),
        contentDescription = iconName,
        modifier = Modifier.size(size.dp),
        tint = color
    )
}

private fun getIconByName(name: String): ImageVector {
    return when (name) {
        "star" -> Icons.Default.Star
        "delete" -> Icons.Default.Delete
        "add" -> Icons.Default.Add
        "close" -> Icons.Default.Close
        "check" -> Icons.Default.Check
        "favorite" -> Icons.Default.Favorite
        "home" -> Icons.Default.Home
        "settings" -> Icons.Default.Settings
        "search" -> Icons.Default.Search
        "menu" -> Icons.Default.Menu
        else -> Icons.Default.HelpOutline
    }
}
```

**Image (line 270-277):**
```kotlin
@Composable
private fun CreateImage(props: Map<String, JsonElement>) {
    val src = props["src"]?.jsonPrimitive?.contentOrNull ?: ""
    val width = props["width"]?.jsonPrimitive?.intOrNull
    val height = props["height"]?.jsonPrimitive?.intOrNull
    val contentDescription = props["alt"]?.jsonPrimitive?.contentOrNull ?: ""

    AsyncImage(
        model = src,
        contentDescription = contentDescription,
        modifier = Modifier
            .then(if (width != null) Modifier.width(width.dp) else Modifier)
            .then(if (height != null) Modifier.height(height.dp) else Modifier),
        contentScale = ContentScale.Fit
    )
}
```

---

### Task 3.3: Fix Button onClick Handler

**File:** `Renderers/Android/.../MELComponentFactory.kt`

**Fix (line 118-122):**
```kotlin
"Button" -> {
    val label = props["label"]?.jsonPrimitive?.contentOrNull ?: "Button"
    val variant = props["variant"]?.jsonPrimitive?.contentOrNull ?: "filled"
    val enabled = props["enabled"]?.jsonPrimitive?.booleanOrNull ?: true

    // Get event handler from callbacks
    val onClick = callbacks["onTap"] ?: callbacks["onClick"]

    Button(
        onClick = { onClick?.invoke() },
        enabled = enabled,
        colors = getButtonColors(variant)
    ) {
        Text(label)
    }
}
```

---

### Task 3.4: Add Missing Component Types

**Add to all platform factories:**
- Accordion
- Carousel
- DataGrid
- List/LazyColumn
- Stepper
- Timeline
- TreeView
- Badge (Android/Web)
- Tooltip (Android/Web)
- Spinner (Android/Web)

---

### Task 3.5: Complete EffectExecutor

**File:** `Core/src/commonMain/kotlin/.../mel/ReducerEngine.kt`

**Replace TODOs (lines 456-489):**
```kotlin
class EffectExecutor(
    private val effectHandlers: Map<String, (Effect) -> Unit> = emptyMap()
) {
    fun executeEffect(effect: Effect) {
        val handler = effectHandlers[effect.type]
        if (handler != null) {
            handler(effect)
        } else {
            // Platform-specific default implementations
            when (effect.type) {
                "log" -> println("[MEL] ${effect.payload}")
                "navigate" -> navigateHandler?.invoke(effect.payload)
                "haptic" -> hapticHandler?.invoke(effect.payload)
                "storage.set" -> storageSetHandler?.invoke(effect.payload)
                "storage.get" -> storageGetHandler?.invoke(effect.payload)
                else -> println("[MEL] Unknown effect: ${effect.type}")
            }
        }
    }

    companion object {
        var navigateHandler: ((Any?) -> Unit)? = null
        var hapticHandler: ((Any?) -> Unit)? = null
        var storageSetHandler: ((Any?) -> Unit)? = null
        var storageGetHandler: ((Any?) -> Unit)? = null
    }
}
```

---

## Phase 4: Optimization

### Task 4.1: Implement Selective Re-rendering

**File:** `Core/src/commonMain/kotlin/.../mel/ReactiveRenderer.kt`

**Replace line 125 TODO:**
```kotlin
override fun rerenderAffected(changedPaths: Set<String>) {
    val affectedNodeIds = mutableSetOf<String>()

    for (path in changedPaths) {
        dependencies[path]?.let { nodeIds ->
            affectedNodeIds.addAll(nodeIds)
        }
    }

    if (affectedNodeIds.isEmpty()) {
        return
    }

    // Selective re-render: only update affected nodes
    for (nodeId in affectedNodeIds) {
        val node = findNodeById(nodeId)
        if (node != null) {
            val resolvedProps = resolver.resolve(node)
            componentFactory.update(nodeId, resolvedProps)
        }
    }

    notifyListeners(changedPaths)
}

private fun findNodeById(id: String): UINode? {
    return findNodeByIdRecursive(uiRoot, id)
}

private fun findNodeByIdRecursive(node: UINode, id: String): UINode? {
    if (node.id == id) return node
    return node.children?.firstNotNullOfOrNull { findNodeByIdRecursive(it, id) }
}
```

---

### Task 4.2: Clean Up StateObserver

**Decision:** Remove StateObserver.kt (dead code, StateFlow is sufficient)

**Files to delete:**
- `Core/src/commonMain/kotlin/.../mel/StateObserver.kt`

**Update any imports that reference it.**

---

### Task 4.3: Add Integration Tests

**File:** `Core/src/commonTest/kotlin/.../mel/MELIntegrationTest.kt`

**Test cases:**
1. Load counter.mel → dispatch increment → verify state.count = 1
2. Load calculator.mel → digit(7) → digit(+) → digit(3) → calc → verify display = "10"
3. Load todo-list.mel → addTask("Test") → verify items.length = 1
4. Verify Tier 1 functions work on iOS platform
5. Verify Tier 2 functions blocked on iOS platform
6. Test event handler parameter passing
7. Test UI binding resolution
8. Test re-render cycle

---

## Execution Command

```bash
# Run with YOLO + SWARM
/iimplement .yolo .swarm AVA-Plan-MELSystemFixes-50512-V1.md
```

---

## Success Criteria

| Metric | Target |
|--------|--------|
| Compilation | 0 errors |
| Unit Tests | 100% pass |
| Integration Tests | 100% pass |
| Counter Plugin | Works on all platforms |
| Calculator Plugin | Works on all platforms |
| Tier Enforcement | iOS = Tier 1 only |

---

**Author:** IDEACODE v10.2 | **License:** Proprietary
