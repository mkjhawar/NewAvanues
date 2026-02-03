# MagicUI Expression Language (MEL) Parser

**Phase 1 Implementation - Dual-Tier Plugin System**

## Overview

MEL is a domain-specific language for reactive state bindings and declarative state transitions in the AvaElements plugin system. It supports two tiers:

- **Tier 1 (DATA):** Apple-compliant mode with whitelisted functions
- **Tier 2 (LOGIC):** Full expression engine for Android, Desktop, and Web

## Architecture

### Components

| File | Purpose | Lines |
|------|---------|-------|
| `ExpressionNode.kt` | AST node types and operators | 164 |
| `ExpressionLexer.kt` | Tokenizer for MEL syntax | 358 |
| `ExpressionParser.kt` | Recursive descent parser | 420 |
| `ExpressionEvaluator.kt` | Interpreter with tier enforcement | 468 |
| `MELExample.kt` | Usage examples | 234 |

### Key Features

#### 1. State References
```kotlin
$state.count              // Simple property
$state.user.name          // Nested property
$state.items[0]           // Array indexing
```

#### 2. Function Calls
```kotlin
$math.add(1, 2)                          // Tier 1 allowed
$string.concat("Hello", " ", "World")    // Tier 1 allowed
$http.get("https://api.example.com")     // Tier 2 only
```

#### 3. Binary Operations
```kotlin
$state.count + 1          // Arithmetic
$state.x > 5              // Comparison
$state.enabled && $state.count > 0  // Logical
```

#### 4. Unary Operations
```kotlin
!$state.enabled           // Logical NOT
-$state.value             // Negation
```

#### 5. Literals
```kotlin
42                        // Number
"Hello World"             // String (single or double quotes)
true                      // Boolean
null                      // Null
[1, 2, 3]                 // Array
{ x: 10, y: 20 }         // Object
```

#### 6. Parameter References (Reducers)
```kotlin
$increment                // In reducer with params: [increment]
$math.add($state.count, $increment)
```

## Usage Examples

### Basic Expression Evaluation

```kotlin
val expression = "$math.add($state.count, 1)"
val lexer = ExpressionLexer(expression)
val tokens = lexer.tokenize()
val parser = ExpressionParser(tokens)
val ast = parser.parse()

val state = mapOf("count" to 5)
val evaluator = ExpressionEvaluator(
    state = state,
    params = emptyMap(),
    tier = PluginTier.DATA
)

val result = evaluator.evaluate(ast) // Returns 6.0
```

### Tier Enforcement

```kotlin
// Tier 1: Only whitelisted functions allowed
val tier1Evaluator = ExpressionEvaluator(
    state = state,
    tier = PluginTier.DATA  // Throws SecurityException for non-whitelisted functions
)

// Tier 2: All functions allowed
val tier2Evaluator = ExpressionEvaluator(
    state = state,
    tier = PluginTier.LOGIC
)
```

### Reducer with Parameters

```kotlin
val expression = "$math.add($state.count, $increment)"
val state = mapOf("count" to 5)
val params = mapOf("increment" to 3)

val evaluator = ExpressionEvaluator(
    state = state,
    params = params,
    tier = PluginTier.DATA
)

val result = evaluator.evaluate(ast) // Returns 8.0
```

## Tier 1 Whitelisted Functions

### Math Functions
- `$math.add`, `$math.subtract`, `$math.multiply`, `$math.divide`, `$math.mod`
- `$math.abs`, `$math.round`, `$math.floor`, `$math.ceil`
- `$math.min`, `$math.max`

### String Functions
- `$string.concat`, `$string.length`, `$string.substring`
- `$string.uppercase`, `$string.lowercase`, `$string.trim`
- `$string.replace`, `$string.split`, `$string.join`

### Array Functions
- `$array.length`, `$array.get`, `$array.first`, `$array.last`
- `$array.append`, `$array.prepend`, `$array.remove`
- `$array.filter`, `$array.map`, `$array.sort`

### Object Functions
- `$object.get`, `$object.set`, `$object.keys`, `$object.values`, `$object.merge`

### Date Functions
- `$date.now`, `$date.format`, `$date.parse`
- `$date.add`, `$date.subtract`, `$date.diff`

### Logic Functions
- `$logic.if`, `$logic.and`, `$logic.or`, `$logic.not`
- `$logic.equals`, `$logic.gt`, `$logic.lt`, `$logic.gte`, `$logic.lte`

## Grammar

```
expression     → logicalOr
logicalOr      → logicalAnd ( "||" logicalAnd )*
logicalAnd     → equality ( "&&" equality )*
equality       → comparison ( ( "==" | "!=" ) comparison )*
comparison     → term ( ( ">" | ">=" | "<" | "<=" ) term )*
term           → factor ( ( "+" | "-" ) factor )*
factor         → unary ( ( "*" | "/" | "%" ) unary )*
unary          → ( "!" | "-" ) unary | primary
primary        → literal | stateRef | functionCall | paramRef | arrayLiteral | objectLiteral | "(" expression ")"

literal        → NUMBER | STRING | BOOLEAN | NULL
stateRef       → "$state" ( "." IDENTIFIER | "[" NUMBER "]" )*
functionCall   → "$" IDENTIFIER "." IDENTIFIER "(" arguments? ")"
paramRef       → "$" IDENTIFIER
arrayLiteral   → "[" ( expression ( "," expression )* )? "]"
objectLiteral  → "{" ( IDENTIFIER ":" expression ( "," IDENTIFIER ":" expression )* )? "}"
arguments      → expression ( "," expression )*
```

## Operator Precedence

| Precedence | Operators | Associativity |
|------------|-----------|---------------|
| 1 (lowest) | `||` | Left |
| 2 | `&&` | Left |
| 3 | `==`, `!=`, `>`, `<`, `>=`, `<=` | Left |
| 4 | `+`, `-` | Left |
| 5 (highest) | `*`, `/`, `%` | Left |

Unary operators (`!`, `-`) have higher precedence than binary operators.

## Error Handling

### LexerException
Thrown when tokenization fails:
- Unexpected characters
- Unterminated strings
- Invalid escape sequences

### ParserException
Thrown when parsing fails:
- Unexpected tokens
- Missing delimiters
- Invalid syntax

### EvaluationException
Thrown during evaluation:
- Type mismatches (e.g., adding string + boolean)
- Division by zero
- Array index out of bounds
- Undefined state/parameter references

### SecurityException
Thrown when tier enforcement is violated:
- Tier 1 (DATA) attempting to use non-whitelisted functions

## Performance Considerations

1. **Lazy Evaluation:** Logical operators (`&&`, `||`) use short-circuit evaluation
2. **Number Coercion:** All numbers are converted to `Double` internally
3. **Immutable State:** State references return immutable copies
4. **Function Registry:** Uses hash-based lookup for O(1) function dispatch

## Next Steps

### Phase 2: State Management (Implemented)
- `PluginState.kt` - State container with path access
- `StateSchema.kt` - Type validation
- `StateObserver.kt` - Change notifications

### Phase 3: Reducer Engine (Implemented)
- `Reducer.kt` - Reducer definitions
- `ReducerParser.kt` - Parse reducer YAML/JSON
- Effect runner for side effects (Tier 2)

### Phase 4: Built-in Functions (Pending)
- Full implementations in `functions/` directory
- `MathFunctions.kt`, `StringFunctions.kt`, etc.
- Extended Tier 2 functions (`$http`, `$storage`, etc.)

### Phase 5: UI Binding System (Pending)
- `BindingResolver.kt` - Resolve `$state` refs in UI props
- `ReactiveRenderer.kt` - Re-render on state changes
- `EventBinder.kt` - Wire events to reducers

## Testing

Run examples:
```kotlin
MELExample.example1()  // Simple arithmetic
MELExample.example2()  // State reference
MELExample.example3()  // Function call
MELExample.example4()  // Conditional logic
MELExample.example5()  // Nested state
MELExample.example6()  // Parameters
MELExample.example7()  // Array literal
MELExample.example8()  // Object literal
MELExample.example9()  // Logical operations
MELExample.example10() // String concatenation
```

## License

Proprietary - Augmentalis 2025

## Author

Manoj Jhawar

---

**Version:** 1.0
**Phase:** Phase 1 - Expression Parser
**Status:** Complete
**Created:** 2025-12-05
