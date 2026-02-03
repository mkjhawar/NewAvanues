# MEL Parser Architecture Diagram

## Component Flow

```
┌─────────────────────────────────────────────────────────────────┐
│                        INPUT EXPRESSION                         │
│                  "$math.add($state.count, 1)"                   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                    LEXER (ExpressionLexer.kt)                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Tokenization:                                            │   │
│  │ "$" → DOLLAR("math")                                     │   │
│  │ "." → DOT                                                │   │
│  │ "add" → IDENTIFIER("add")                                │   │
│  │ "(" → LPAREN                                             │   │
│  │ "$" → DOLLAR("state")                                    │   │
│  │ "." → DOT                                                │   │
│  │ "count" → IDENTIFIER("count")                            │   │
│  │ "," → COMMA                                              │   │
│  │ "1" → NUMBER("1")                                        │   │
│  │ ")" → RPAREN                                             │   │
│  │ EOF → EOF                                                │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PARSER (ExpressionParser.kt)                  │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ AST Construction (Recursive Descent):                    │   │
│  │                                                          │   │
│  │   FunctionCall(                                          │   │
│  │     category = "math",                                   │   │
│  │     name = "add",                                        │   │
│  │     args = [                                             │   │
│  │       StateRef(path = ["count"]),                        │   │
│  │       Literal(NumberValue(1.0))                          │   │
│  │     ]                                                    │   │
│  │   )                                                      │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                 EVALUATOR (ExpressionEvaluator.kt)              │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │ Step 1: Tier Enforcement                                 │   │
│  │   ✓ Check "math.add" is in Tier 1 whitelist             │   │
│  │                                                          │   │
│  │ Step 2: Evaluate Arguments                               │   │
│  │   - StateRef(["count"]) → state["count"] → 5            │   │
│  │   - Literal(1.0) → 1.0                                   │   │
│  │                                                          │   │
│  │ Step 3: Execute Function                                 │   │
│  │   - FunctionRegistry.execute("math.add", [5, 1.0])       │   │
│  │   - Result: 6.0                                          │   │
│  └──────────────────────────────────────────────────────────┘   │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│                          RESULT: 6.0                            │
└─────────────────────────────────────────────────────────────────┘
```

## Component Interaction

```
┌────────────────┐
│ Plugin YAML    │
│ Definition     │
└───────┬────────┘
        │
        ▼
┌────────────────────────────────────────────────────────────┐
│              PluginDefinitionParser                        │
│  (Parses YAML → PluginDefinition)                          │
└───────┬────────────────────────────────────────────────────┘
        │
        ▼
┌────────────────────────────────────────────────────────────┐
│                    PluginRuntime                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  • Initializes PluginState from schema               │  │
│  │  • Detects platform tier (iOS → Tier 1)             │  │
│  │  • Creates ExpressionEvaluator with tier enforcement │  │
│  └──────────────────────────────────────────────────────┘  │
└───────┬──────────────────────────────────┬─────────────────┘
        │                                  │
        ▼                                  ▼
┌────────────────┐              ┌──────────────────────┐
│  State Update  │              │   UI Rendering       │
│  (Reducers)    │              │   (Bindings)         │
└────────┬───────┘              └──────────┬───────────┘
         │                                 │
         ▼                                 ▼
  ┌─────────────┐                 ┌─────────────────┐
  │ Expression  │                 │  Expression     │
  │ Evaluator   │                 │  Evaluator      │
  │ (with state)│                 │  (resolve refs) │
  └─────────────┘                 └─────────────────┘
```

## Tier Enforcement Flow

```
┌─────────────────────┐
│  Function Call:     │
│  $http.get(url)     │
└──────────┬──────────┘
           │
           ▼
┌──────────────────────────────────────────────────┐
│          ExpressionEvaluator                     │
│  ┌────────────────────────────────────────────┐  │
│  │ 1. Parse function key: "http.get"          │  │
│  │                                            │  │
│  │ 2. Check tier:                             │  │
│  │    - Current tier: DATA (Tier 1)           │  │
│  │    - Is "http.get" in Tier 1 whitelist?    │  │
│  │      → NO                                  │  │
│  │                                            │  │
│  │ 3. Throw SecurityException:                │  │
│  │    "Function 'http.get' is not allowed     │  │
│  │     in Tier 1 (DATA mode)"                 │  │
│  └────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────┘
```

## Grammar Hierarchy (Operator Precedence)

```
expression (entry point)
    │
    └─→ logicalOr                    Precedence: 1 (lowest)
            │                         Operator: ||
            └─→ logicalAnd            Precedence: 2
                    │                 Operator: &&
                    └─→ equality      Precedence: 3
                            │         Operators: ==, !=
                            └─→ comparison      Precedence: 3
                                    │           Operators: >, <, >=, <=
                                    └─→ term    Precedence: 4
                                            │   Operators: +, -
                                            └─→ factor    Precedence: 5
                                                    │     Operators: *, /, %
                                                    └─→ unary
                                                            │
                                                            └─→ primary (literals, refs, calls)
```

## Data Flow Example: Calculator Button Press

```
┌─────────────────────────────────────────────────────────────────┐
│  USER ACTION: Taps "7" button in calculator                     │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  EVENT: onTap → "appendDigit(7)"                                │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  RUNTIME: Dispatch reducer "appendDigit" with params = {7}      │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  REDUCER DEFINITION:                                            │
│    params: [digit]                                              │
│    next_state:                                                  │
│      display: $logic.if(                                        │
│        $logic.equals($state.display, "0"),                      │
│        $digit,                                                  │
│        $string.concat($state.display, $digit)                   │
│      )                                                          │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  EVALUATOR: Evaluate expression for "display" field            │
│                                                                 │
│  Current State: { display: "123" }                              │
│  Params: { digit: "7" }                                         │
│                                                                 │
│  Expression Tree:                                               │
│    FunctionCall("logic", "if", [                                │
│      FunctionCall("logic", "equals", [                          │
│        StateRef(["display"]),    → "123"                        │
│        Literal("0")              → "0"                          │
│      ]),                         → false                        │
│      ParamRef("digit"),          → "7"                          │
│      FunctionCall("string", "concat", [                         │
│        StateRef(["display"]),    → "123"                        │
│        ParamRef("digit")         → "7"                          │
│      ])                          → "1237"                       │
│    ])                            → "1237"                       │
│                                                                 │
│  Result: "1237"                                                 │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  STATE UPDATE: { display: "1237" }                              │
└────────────────────────────┬────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────┐
│  UI RE-RENDER: Text component shows "1237"                      │
└─────────────────────────────────────────────────────────────────┘
```

## Error Handling Chain

```
┌──────────────────┐
│  Input String    │
└────────┬─────────┘
         │
         ▼
┌────────────────────────────┐
│  LEXER                     │
│  ┌──────────────────────┐  │
│  │ Invalid syntax?      │  │
│  │ → LexerException     │  │
│  └──────────────────────┘  │
└────────┬───────────────────┘
         │ [valid tokens]
         ▼
┌────────────────────────────┐
│  PARSER                    │
│  ┌──────────────────────┐  │
│  │ Invalid grammar?     │  │
│  │ → ParserException    │  │
│  └──────────────────────┘  │
└────────┬───────────────────┘
         │ [valid AST]
         ▼
┌────────────────────────────┐
│  EVALUATOR                 │
│  ┌──────────────────────┐  │
│  │ Tier violation?      │  │
│  │ → SecurityException  │  │
│  │                      │  │
│  │ Type error?          │  │
│  │ → EvaluationException│  │
│  │                      │  │
│  │ Undefined ref?       │  │
│  │ → EvaluationException│  │
│  └──────────────────────┘  │
└────────┬───────────────────┘
         │ [result value]
         ▼
┌──────────────────┐
│  Success         │
└──────────────────┘
```

## Platform Tier Detection

```
┌─────────────────────────────────────────────────────────────┐
│                     Plugin Definition                       │
│                    tier: LOGIC                              │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                     TierDetector                            │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ Detect platform:                                      │  │
│  │  - isApple? → YES                                     │  │
│  │  - Requested tier: LOGIC (Tier 2)                     │  │
│  │                                                       │  │
│  │ Decision:                                             │  │
│  │  - Downgrade to DATA (Tier 1)                         │  │
│  │  - Log warning: "Downgrading plugin to Tier 1         │  │
│  │                  on Apple platform"                   │  │
│  └───────────────────────────────────────────────────────┘  │
└────────────────────────────┬────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────┐
│                  ExpressionEvaluator                        │
│                  tier: DATA (enforced)                      │
└─────────────────────────────────────────────────────────────┘
```

---

**Created:** 2025-12-05
**Author:** Manoj Jhawar
**Purpose:** Visual documentation of MEL parser architecture
