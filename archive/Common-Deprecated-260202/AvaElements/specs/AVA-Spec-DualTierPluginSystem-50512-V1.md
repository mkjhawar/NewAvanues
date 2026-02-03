# AVA-Spec-DualTierPluginSystem-50512-V1

**Feature:** Dual-Tier Plugin System with MagicUI Expression Language
**Version:** 1.0
**Created:** 2025-12-05
**Status:** APPROVED FOR IMPLEMENTATION
**Mode:** YOLO SWARM

---

## Executive Summary

Implement a dual-tier plugin system that provides:
1. **Tier 1 (Data Mode):** Apple-compliant reactive templates with state bindings
2. **Tier 2 (Logic Mode):** Full expression engine for Android, Desktop, Web

This enables self-contained plugins (calculators, forms, games) while maintaining app store compliance.

**Total Effort:** 12-15 days
**Priority:** P0 (Enables plugin marketplace)

---

## Strategic Rationale

### App Store Compliance Analysis

| Store | Key Rule | Our Approach |
|-------|----------|--------------|
| Apple | No code that changes app behavior | Tier 1: Declarative templates, predefined functions |
| Google | No external code execution | Both tiers safe (interpreted internally) |
| Desktop | No restrictions | Full Tier 2 capability |
| Web | No restrictions | Full Tier 2 capability |

### Precedent Apps (Apple-Approved)

| App | Feature | Why Allowed |
|-----|---------|-------------|
| Notion | Formula language | "Database formulas" |
| Shortcuts | Visual programming | "Automation tool" |
| Airtable | Complex expressions | "Spreadsheet formulas" |
| Figma | Auto-layout rules | "Design constraints" |

### Our Positioning

- **Marketing:** "No-code reactive templates"
- **Technical:** "State bindings with predefined reducers"
- **Perception:** Configuration, not scripting

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     Plugin Definition (YAML/JSON)               │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐  │
│  │  Metadata   │  │    State    │  │          UI             │  │
│  │  id, name   │  │  variables  │  │  components + bindings  │  │
│  └─────────────┘  └─────────────┘  └─────────────────────────┘  │
│  ┌─────────────────────────┐  ┌───────────────────────────────┐ │
│  │   Reducers (Tier 1)     │  │    Scripts (Tier 2 only)      │ │
│  │   Declarative state     │  │    Full expressions           │ │
│  │   transitions           │  │    Imperative logic           │ │
│  └─────────────────────────┘  └───────────────────────────────┘ │
└─────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
        ┌──────────┐    ┌──────────┐    ┌──────────┐
        │   iOS    │    │ Android  │    │   Web    │
        │ Tier 1   │    │ Tier 1+2 │    │ Tier 1+2 │
        │ only     │    │          │    │          │
        └──────────┘    └──────────┘    └──────────┘
```

---

## Tier Definitions

### Tier 1: Data Mode (Apple-Safe)

| Feature | Description | Example |
|---------|-------------|---------|
| State Schema | Typed state variables | `state: { count: 0 }` |
| Bindings | Reference state in UI | `$state.count` |
| Reducers | Predefined state transitions | `increment: { next: { count: $state.count + 1 } }` |
| Built-in Functions | Whitelisted utilities | `$math.add`, `$string.concat` |
| Events | Trigger reducers | `onTap: increment` |

**Constraints:**
- No arbitrary expressions
- No imperative statements
- No loops/conditionals in reducers
- Predefined function whitelist only

### Tier 2: Logic Mode (Non-Apple)

| Feature | Description | Example |
|---------|-------------|---------|
| Full Expressions | Any computation | `$state.a * $state.b + 10` |
| Conditionals | if/else logic | `if ($state.x > 5) { ... }` |
| Loops | Iteration | `for item in $state.items { ... }` |
| Custom Functions | User-defined | `fn calculate() { ... }` |
| Extended APIs | More capabilities | `$http.get`, `$storage.save` |

---

## MagicUI Expression Language (MEL)

### Syntax Overview

```yaml
# Comments with hash
state:
  count: 0                    # Number
  name: "Calculator"          # String
  items: []                   # Array
  config: { theme: "dark" }   # Object

# Bindings use $ prefix
ui:
  Text:
    value: $state.name        # Simple binding
    visible: $state.count > 0 # Expression binding (Tier 2)

# Reducers define state transitions
reducers:
  increment:
    next_state:
      count: $math.add($state.count, 1)

  reset:
    next_state:
      count: 0
      items: []

# Events bind UI actions to reducers
ui:
  Button:
    label: "Add"
    onTap: increment          # Reducer reference
```

### Built-in Functions (Tier 1 Whitelist)

| Category | Functions |
|----------|-----------|
| `$math` | `add`, `subtract`, `multiply`, `divide`, `mod`, `abs`, `round`, `floor`, `ceil`, `min`, `max` |
| `$string` | `concat`, `length`, `substring`, `uppercase`, `lowercase`, `trim`, `replace`, `split`, `join` |
| `$array` | `length`, `get`, `first`, `last`, `append`, `prepend`, `remove`, `filter`, `map`, `sort` |
| `$object` | `get`, `set`, `keys`, `values`, `merge` |
| `$date` | `now`, `format`, `parse`, `add`, `subtract`, `diff` |
| `$logic` | `if`, `and`, `or`, `not`, `equals`, `gt`, `lt`, `gte`, `lte` |

### Extended Functions (Tier 2 Only)

| Category | Functions |
|----------|-----------|
| `$http` | `get`, `post`, `put`, `delete` |
| `$storage` | `get`, `set`, `remove`, `clear` |
| `$nav` | `push`, `pop`, `replace`, `reset` |
| `$clipboard` | `copy`, `paste` |
| `$haptics` | `light`, `medium`, `heavy` |

---

## Functional Requirements

### FR-1: Plugin Parser

| Requirement | Description |
|-------------|-------------|
| FR-1.1 | Parse YAML/JSON plugin definitions |
| FR-1.2 | Validate state schema |
| FR-1.3 | Parse reducer definitions |
| FR-1.4 | Parse UI with bindings |
| FR-1.5 | Detect tier requirements |

### FR-2: State Management

| Requirement | Description |
|-------------|-------------|
| FR-2.1 | Initialize state from schema |
| FR-2.2 | State immutability (copy-on-write) |
| FR-2.3 | State change notifications |
| FR-2.4 | State persistence (optional) |

### FR-3: Expression Evaluator

| Requirement | Description |
|-------------|-------------|
| FR-3.1 | Parse `$state.x` bindings |
| FR-3.2 | Evaluate `$func.name()` calls |
| FR-3.3 | Tier 1: Whitelist enforcement |
| FR-3.4 | Tier 2: Full expression parsing |

### FR-4: Reducer Engine

| Requirement | Description |
|-------------|-------------|
| FR-4.1 | Execute reducers on events |
| FR-4.2 | Compute next state |
| FR-4.3 | Apply state updates atomically |
| FR-4.4 | Support async reducers (Tier 2) |

### FR-5: UI Binding System

| Requirement | Description |
|-------------|-------------|
| FR-5.1 | Resolve `$state.x` in UI props |
| FR-5.2 | Re-render on state change |
| FR-5.3 | Bind events to reducers |
| FR-5.4 | Support conditional rendering |

### FR-6: Platform Tier Detection

| Requirement | Description |
|-------------|-------------|
| FR-6.1 | Detect platform at runtime |
| FR-6.2 | Downgrade Tier 2 plugins on Apple |
| FR-6.3 | Warn on unsupported features |
| FR-6.4 | Graceful degradation |

---

## Technical Design

### Data Models

```kotlin
@Serializable
data class PluginDefinition(
    val metadata: PluginMetadata,
    val tier: PluginTier = PluginTier.DATA,
    val state: Map<String, StateVariable>,
    val reducers: Map<String, Reducer>,
    val scripts: Map<String, Script>? = null,  // Tier 2 only
    val ui: UINode
)

@Serializable
enum class PluginTier {
    DATA,   // Tier 1: Apple-safe
    LOGIC   // Tier 2: Full expressions
}

@Serializable
data class StateVariable(
    val type: StateType,
    val default: JsonElement,
    val persist: Boolean = false
)

@Serializable
data class Reducer(
    val params: List<String> = emptyList(),
    val next_state: Map<String, Expression>,
    val effects: List<Effect>? = null  // Tier 2 only
)

@Serializable
data class Expression(
    val raw: String,
    val parsed: ExpressionNode? = null
)

@Serializable
data class UINode(
    val type: String,
    val props: Map<String, JsonElement>,
    val bindings: Map<String, Expression>,
    val events: Map<String, String>,  // event -> reducer name
    val children: List<UINode>? = null
)
```

### Expression Parser

```kotlin
sealed class ExpressionNode {
    data class Literal(val value: Any) : ExpressionNode()
    data class StateRef(val path: List<String>) : ExpressionNode()
    data class FunctionCall(
        val category: String,  // math, string, etc.
        val name: String,
        val args: List<ExpressionNode>
    ) : ExpressionNode()
    data class BinaryOp(
        val op: String,
        val left: ExpressionNode,
        val right: ExpressionNode
    ) : ExpressionNode()
}

class ExpressionEvaluator(
    private val tier: PluginTier,
    private val state: PluginState
) {
    private val tier1Functions = setOf(
        "math.add", "math.subtract", "math.multiply", "math.divide",
        "string.concat", "string.length", "string.substring",
        "array.length", "array.get", "array.append",
        "logic.if", "logic.and", "logic.or", "logic.not"
    )

    fun evaluate(expr: ExpressionNode): Any {
        return when (expr) {
            is Literal -> expr.value
            is StateRef -> state.get(expr.path)
            is FunctionCall -> {
                val funcKey = "${expr.category}.${expr.name}"
                if (tier == PluginTier.DATA && funcKey !in tier1Functions) {
                    throw SecurityException("Function $funcKey not allowed in Tier 1")
                }
                executeFunction(expr)
            }
            is BinaryOp -> evaluateBinaryOp(expr)
        }
    }
}
```

### Runtime Architecture

```kotlin
class PluginRuntime(
    private val definition: PluginDefinition,
    private val platform: Platform
) {
    private val effectiveTier = when {
        platform.isApple && definition.tier == PluginTier.LOGIC -> {
            Log.warn("Downgrading plugin to Tier 1 on Apple platform")
            PluginTier.DATA
        }
        else -> definition.tier
    }

    private val state = PluginState(definition.state)
    private val evaluator = ExpressionEvaluator(effectiveTier, state)
    private val renderer = PluginRenderer(evaluator)

    fun dispatch(action: String, params: Map<String, Any> = emptyMap()) {
        val reducer = definition.reducers[action]
            ?: throw IllegalArgumentException("Unknown reducer: $action")

        val nextState = reducer.next_state.mapValues { (_, expr) ->
            evaluator.evaluate(expr.parsed!!)
        }

        state.update(nextState)
        renderer.rerender()
    }

    fun render(): Component {
        return renderer.render(definition.ui)
    }
}
```

---

## Implementation Plan

### Phase 1: Expression Parser (3 days)

**Files to Create:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/core/mel/
├── ExpressionLexer.kt       # Tokenizer
├── ExpressionParser.kt      # AST builder
├── ExpressionNode.kt        # AST types
└── ExpressionEvaluator.kt   # Interpreter
```

**Tasks:**
1. Implement lexer for MEL syntax
2. Build recursive descent parser
3. Create AST node types
4. Implement evaluator with tier enforcement

### Phase 2: State Management (2 days)

**Files to Create:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/core/mel/
├── PluginState.kt           # State container
├── StateSchema.kt           # Type validation
└── StateObserver.kt         # Change notifications
```

**Tasks:**
1. Implement state container with path access
2. Add type validation
3. Implement change observation
4. Add persistence support

### Phase 3: Reducer Engine (2 days)

**Files to Create:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/core/mel/
├── ReducerEngine.kt         # Reducer execution
├── ReducerParser.kt         # Parse reducer definitions
└── EffectRunner.kt          # Side effects (Tier 2)
```

**Tasks:**
1. Parse reducer definitions
2. Execute state transitions
3. Handle parameterized reducers
4. Implement effects (Tier 2)

### Phase 4: Built-in Functions (2 days)

**Files to Create:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/core/mel/functions/
├── MathFunctions.kt         # $math.*
├── StringFunctions.kt       # $string.*
├── ArrayFunctions.kt        # $array.*
├── ObjectFunctions.kt       # $object.*
├── DateFunctions.kt         # $date.*
├── LogicFunctions.kt        # $logic.*
└── FunctionRegistry.kt      # Registration + tier enforcement
```

**Tasks:**
1. Implement math functions
2. Implement string functions
3. Implement collection functions
4. Implement logic functions
5. Create registry with tier whitelist

### Phase 5: UI Binding System (3 days)

**Files to Create:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/core/mel/
├── BindingResolver.kt       # Resolve $state refs in UI
├── ReactiveRenderer.kt      # Re-render on state change
└── EventBinder.kt           # Bind onTap etc. to reducers
```

**Tasks:**
1. Parse bindings in UI props
2. Resolve bindings to values
3. Subscribe to state changes
4. Trigger re-renders efficiently
5. Wire events to reducer dispatch

### Phase 6: Plugin Runtime (2 days)

**Files to Create:**
```
Core/src/commonMain/kotlin/com/augmentalis/magicelements/core/mel/
├── PluginRuntime.kt         # Main runtime coordinator
├── PluginDefinitionParser.kt # Parse full plugin YAML/JSON
├── TierDetector.kt          # Platform tier detection
└── PluginValidator.kt       # Validate plugin definitions
```

**Tasks:**
1. Implement full plugin parser
2. Create runtime coordinator
3. Implement tier detection/downgrade
4. Add validation

### Phase 7: Platform Integration (2 days)

**Files to Update:**
```
Renderers/iOS/src/iosMain/kotlin/.../PluginRenderer.kt
Renderers/Android/src/main/kotlin/.../PluginRenderer.kt
Renderers/Web/src/plugins/PluginRenderer.tsx
```

**Tasks:**
1. Integrate MEL runtime with iOS renderer
2. Integrate with Android renderer
3. Integrate with Web renderer
4. Platform-specific tier enforcement

### Phase 8: Testing & Examples (2 days)

**Files to Create:**
```
Core/src/commonTest/kotlin/.../mel/
├── ExpressionParserTest.kt
├── ExpressionEvaluatorTest.kt
├── ReducerEngineTest.kt
├── StateManagementTest.kt
└── IntegrationTest.kt

examples/plugins/
├── calculator.yaml
├── todo-list.yaml
├── quiz-game.yaml
└── unit-converter.yaml
```

---

## Swarm Agent Assignment

| Agent | Phase | Files | Effort |
|-------|-------|-------|--------|
| **Agent 1: Expression Parser** | Phase 1 | Lexer, Parser, AST | 3 days |
| **Agent 2: State Management** | Phase 2 | State, Schema, Observer | 2 days |
| **Agent 3: Reducer Engine** | Phase 3 | Engine, Parser, Effects | 2 days |
| **Agent 4: Built-in Functions** | Phase 4 | All function files | 2 days |
| **Agent 5: UI Bindings** | Phase 5 | Resolver, Renderer, Events | 3 days |
| **Agent 6: Plugin Runtime** | Phase 6 | Runtime, Validator | 2 days |
| **Agent 7: Platform Integration** | Phase 7 | All renderers | 2 days |
| **Agent 8: Testing** | Phase 8 | Tests + examples | 2 days |

---

## Example Plugin: Calculator

```yaml
# calculator.yaml - Works on all platforms
# Tier 2 features auto-disabled on Apple

plugin:
  id: "calculator"
  name: "Calculator"
  version: "1.0.0"
  tier: logic  # Request Tier 2, will downgrade on Apple

state:
  display: "0"
  buffer: ""
  operator: null
  history: []  # Tier 2 feature

reducers:
  appendDigit:
    params: [digit]
    next_state:
      display: $logic.if(
        $logic.equals($state.display, "0"),
        $digit,
        $string.concat($state.display, $digit)
      )

  setOperator:
    params: [op]
    next_state:
      buffer: $state.display
      operator: $op
      display: "0"

  calculate:
    next_state:
      display: $math.eval($state.buffer, $state.operator, $state.display)
      buffer: ""
      operator: null

  clear:
    next_state:
      display: "0"
      buffer: ""
      operator: null

# Tier 2 only - ignored on Apple
scripts:
  saveToHistory:
    body: |
      let expr = $state.buffer + $state.operator + $state.display
      let result = $math.eval($state.buffer, $state.operator, $state.display)
      $state.history = $array.append($state.history, { expr, result })
      $storage.set("history", $state.history)

ui:
  Column:
    style: { padding: 16 }
    children:
      - Text:
          value: $state.display
          style: { fontSize: 48, textAlign: "end", fontFamily: "monospace" }

      - Divider: {}

      # Row 1: 7 8 9 ÷
      - Row:
          style: { gap: 8 }
          children:
            - Button: { label: "7", onTap: "appendDigit(7)", style: { flex: 1 } }
            - Button: { label: "8", onTap: "appendDigit(8)", style: { flex: 1 } }
            - Button: { label: "9", onTap: "appendDigit(9)", style: { flex: 1 } }
            - Button: { label: "÷", onTap: "setOperator('/')", variant: "secondary", style: { flex: 1 } }

      # Row 2: 4 5 6 ×
      - Row:
          style: { gap: 8 }
          children:
            - Button: { label: "4", onTap: "appendDigit(4)", style: { flex: 1 } }
            - Button: { label: "5", onTap: "appendDigit(5)", style: { flex: 1 } }
            - Button: { label: "6", onTap: "appendDigit(6)", style: { flex: 1 } }
            - Button: { label: "×", onTap: "setOperator('*')", variant: "secondary", style: { flex: 1 } }

      # Row 3: 1 2 3 −
      - Row:
          style: { gap: 8 }
          children:
            - Button: { label: "1", onTap: "appendDigit(1)", style: { flex: 1 } }
            - Button: { label: "2", onTap: "appendDigit(2)", style: { flex: 1 } }
            - Button: { label: "3", onTap: "appendDigit(3)", style: { flex: 1 } }
            - Button: { label: "−", onTap: "setOperator('-')", variant: "secondary", style: { flex: 1 } }

      # Row 4: C 0 = +
      - Row:
          style: { gap: 8 }
          children:
            - Button: { label: "C", onTap: "clear", variant: "danger", style: { flex: 1 } }
            - Button: { label: "0", onTap: "appendDigit(0)", style: { flex: 1 } }
            - Button: { label: "=", onTap: "calculate", variant: "primary", style: { flex: 1 } }
            - Button: { label: "+", onTap: "setOperator('+')", variant: "secondary", style: { flex: 1 } }
```

---

## Success Criteria

| Criterion | Verification |
|-----------|--------------|
| Calculator plugin renders on all platforms | Visual test |
| State updates reflect in UI | Interactive test |
| Tier 1 plugins work on iOS | iOS simulator |
| Tier 2 features disabled on Apple | Runtime check |
| No Apple review rejection | App Store submission |
| Expression parser handles all MEL syntax | Unit tests |
| Built-in functions work correctly | Unit tests |

---

## Risk Assessment

| Risk | Mitigation |
|------|------------|
| Apple rejection | Conservative Tier 1 whitelist, "templates" framing |
| Expression parser bugs | Comprehensive test suite |
| Performance on complex plugins | Lazy evaluation, memoization |
| State management complexity | Immutable state, clear update paths |

---

## Security Considerations

| Concern | Mitigation |
|---------|------------|
| Arbitrary code execution | Tier 1 whitelist, no eval() |
| Resource exhaustion | Expression depth limits, timeout |
| Data exfiltration | No network in Tier 1 |
| State tampering | Immutable state copies |

---

## Dependencies

| Dependency | Version | Purpose |
|------------|---------|---------|
| kotlinx-serialization | 1.6.0 | Plugin parsing |
| kotlinx-coroutines | 1.7.3 | Async operations |

---

## Timeline

| Phase | Duration | Parallel |
|-------|----------|----------|
| Phase 1-4: Core MEL | 4 days | Yes (with swarm) |
| Phase 5-6: Runtime | 3 days | Yes |
| Phase 7: Integration | 2 days | Yes |
| Phase 8: Testing | 2 days | No |
| **Total** | **8-12 days** | - |

---

**Approved By:** Engineering Team
**Implementation Mode:** YOLO SWARM

