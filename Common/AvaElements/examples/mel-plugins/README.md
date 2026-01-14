# MEL Plugin Examples

Example plugins demonstrating the MagicUI Expression Language (MEL) and Dual-Tier Plugin System.

## Overview

These plugins showcase different MEL features and tier capabilities:

| Plugin | Tier | Description | Key Features |
|--------|------|-------------|--------------|
| `counter.yaml` | Data | Simple counter (minimal example) | Basic state, math operations, conditional styling |
| `calculator-tier1.yaml` | Data | Apple-safe calculator | All Tier 1 functions, basic arithmetic |
| `calculator.yaml` | Logic | Full calculator with history | History feature (Tier 2), backspace, decimal support |
| `quiz-game.yaml` | Data | Trivia quiz game | Array operations, progress tracking, conditional rendering |
| `unit-converter.yaml` | Data | Unit conversion tool | Complex calculations, dropdowns, bidirectional conversion |
| `todo-list.yaml` | Logic | Todo list with filtering | Array manipulation, filtering, ForEach loops |
| `stopwatch.yaml` | Logic | Stopwatch with lap recording | Time operations, lap recording, dynamic lists |

---

## Plugin Tier System

### Tier 1 (Data Mode) - Apple-Safe

**Capabilities:**
- State bindings: `$state.count`
- Predefined reducers with declarative state transitions
- Built-in function whitelist: `$math.*`, `$string.*`, `$array.*`, `$logic.*`
- No arbitrary expressions or loops
- No network access

**Examples:** `counter.yaml`, `calculator-tier1.yaml`, `quiz-game.yaml`, `unit-converter.yaml`

### Tier 2 (Logic Mode) - Full Capability

**Additional Capabilities:**
- Full expression evaluation
- Custom scripts with imperative logic
- Extended function access: `$http.*`, `$storage.*`, `$nav.*`
- Advanced array operations and filtering

**Examples:** `calculator.yaml`, `todo-list.yaml`, `stopwatch.yaml`

**Platform Behavior:**
- iOS: Tier 2 plugins auto-downgrade to Tier 1, advanced features disabled
- Android/Desktop/Web: Full Tier 2 support

---

## Plugin Examples

### 1. counter.yaml

**Purpose:** Minimal example for learning MEL basics

**State:**
```yaml
count: 0
```

**Reducers:**
- `increment` - Add 1
- `decrement` - Subtract 1
- `reset` - Set to 0
- `addAmount(amount)` - Add custom value

**Demonstrates:**
- Basic state management
- Math functions (`$math.add`, `$math.subtract`)
- Conditional styling based on state
- Parameterized reducers

---

### 2. calculator-tier1.yaml

**Purpose:** Apple-compliant calculator using Tier 1 only

**State:**
```yaml
display: "0"
buffer: ""
operator: null
```

**Reducers:**
- `appendDigit(digit)` - Append digit to display
- `setOperator(op)` - Set operation (+, -, ×, ÷)
- `calculate` - Perform calculation
- `clear` - Reset all state

**Demonstrates:**
- Nested `$logic.if` expressions
- String concatenation
- Math operations
- Tier 1 compliance (works on iOS)

---

### 3. calculator.yaml

**Purpose:** Full-featured calculator with Tier 2 enhancements

**State:**
```yaml
display: "0"
buffer: ""
operator: null
history: []  # Tier 2 only
```

**Reducers:**
- All from Tier 1 calculator
- `appendDecimal` - Add decimal point
- `backspace` - Remove last digit

**Scripts (Tier 2):**
- `saveToHistory` - Save calculations to history

**Demonstrates:**
- Graceful degradation (history disabled on iOS)
- String manipulation (`$string.substring`, `$string.length`)
- Storage API (`$storage.set`)
- Decimal point handling

---

### 4. quiz-game.yaml

**Purpose:** Multiple choice trivia game

**State:**
```yaml
questions: [...]
currentIndex: 0
score: 0
answers: []
isComplete: false
selectedAnswer: null
```

**Reducers:**
- `selectAnswer(answerIndex)` - Choose an answer
- `nextQuestion` - Submit and advance
- `restart` - Reset quiz

**Demonstrates:**
- Complex nested state (array of objects)
- Array access (`$array.get`)
- Object property access (`$object.get`)
- Progress calculation
- Conditional UI visibility
- State-driven UI updates

---

### 5. unit-converter.yaml

**Purpose:** Convert between units of measurement

**State:**
```yaml
value: "1"
fromUnit: "m"
toUnit: "ft"
result: "3.28084"
units: { length: [...], weight: [...], temperature: [...] }
```

**Reducers:**
- `setValue(newValue)` - Update input
- `setFromUnit(unit)` - Change source unit
- `setToUnit(unit)` - Change target unit
- `swapUnits` - Swap source/target

**Demonstrates:**
- Complex calculations
- Dropdown integration
- Nested object structures
- Real-time calculation updates
- Bidirectional conversion

---

### 6. todo-list.yaml

**Purpose:** Task management with filtering

**State:**
```yaml
items: []
newItemText: ""
filter: "all"  # all | active | completed
nextId: 1
```

**Reducers:**
- `addItem` - Create new todo
- `removeItem(id)` - Delete todo
- `toggleItem(id)` - Toggle completed status
- `setFilter(filter)` - Change view filter
- `clearCompleted` - Remove completed items

**Demonstrates:**
- Array manipulation (`$array.append`, `$array.filter`, `$array.map`)
- ForEach loops for dynamic lists
- Object creation (`$object.create`)
- Filtered rendering
- Complex state updates

---

### 7. stopwatch.yaml

**Purpose:** Timer with lap recording

**State:**
```yaml
elapsed: 0
isRunning: false
laps: []
startTime: 0
lapStartTime: 0
```

**Reducers:**
- `start` - Start timer
- `stop` - Pause timer
- `reset` - Clear everything
- `lap` - Record lap time

**Scripts (Tier 2):**
- `formatTime(ms)` - Format milliseconds to MM:SS.ms

**Demonstrates:**
- Time operations (`$date.now`, `$date.diff`)
- Dynamic lap list with ForEach
- Conditional button visibility
- Array reverse for newest-first display
- Time formatting

---

## MEL Syntax Reference

### State Bindings

```yaml
Text:
  value: $state.count              # Simple binding
  visible: $logic.gt($state.count, 0)  # Expression binding
```

### Built-in Functions (Tier 1)

**Math:**
```yaml
$math.add($state.a, $state.b)
$math.subtract($state.a, $state.b)
$math.multiply($state.a, $state.b)
$math.divide($state.a, $state.b)
$math.mod($state.a, $state.b)
$math.abs($state.value)
$math.round($state.value)
$math.floor($state.value)
$math.ceil($state.value)
$math.min($state.a, $state.b)
$math.max($state.a, $state.b)
$math.parse($state.stringValue)
```

**String:**
```yaml
$string.concat($state.firstName, " ", $state.lastName)
$string.length($state.text)
$string.substring($state.text, 0, 5)
$string.uppercase($state.text)
$string.lowercase($state.text)
$string.trim($state.text)
$string.contains($state.text, "search")
```

**Array:**
```yaml
$array.length($state.items)
$array.get($state.items, 0)
$array.first($state.items)
$array.last($state.items)
$array.append($state.items, $newItem)
$array.prepend($state.items, $newItem)
$array.filter($state.items, "item", $condition)
$array.map($state.items, "item", $transformation)
$array.reverse($state.items)
```

**Object:**
```yaml
$object.get($state.user, "name")
$object.set($state.user, "name", "John")
$object.keys($state.user)
$object.values($state.user)
$object.create("key1", "value1", "key2", "value2")
```

**Logic:**
```yaml
$logic.if($condition, $ifTrue, $ifFalse)
$logic.and($state.a, $state.b)
$logic.or($state.a, $state.b)
$logic.not($state.value)
$logic.equals($state.a, $state.b)
$logic.gt($state.a, $state.b)  # greater than
$logic.lt($state.a, $state.b)  # less than
$logic.gte($state.a, $state.b) # greater than or equal
$logic.lte($state.a, $state.b) # less than or equal
```

**Date (Tier 1):**
```yaml
$date.now()
$date.diff($date1, $date2)
$date.format($state.timestamp, "YYYY-MM-DD")
```

### Extended Functions (Tier 2 Only)

```yaml
$http.get($url)
$storage.set("key", $value)
$storage.get("key")
$nav.push("/route")
$clipboard.copy($text)
$haptics.light()
```

---

## UI Components

### Layout Components

```yaml
Column:
  style: { gap: 16, padding: 16 }
  children: [...]

Row:
  style: { gap: 8, justifyContent: "space-between" }
  children: [...]

Container:
  style: { backgroundColor: "#f5f5f5", borderRadius: 8, padding: 16 }
  children: [...]
```

### Display Components

```yaml
Text:
  value: $state.message
  style: { fontSize: 18, fontWeight: "bold" }

Divider: {}

ProgressBar:
  value: $math.divide($state.current, $state.total)
```

### Input Components

```yaml
TextField:
  value: $state.text
  placeholder: "Enter text"
  onChange: "updateText"

Checkbox:
  checked: $state.isChecked
  onToggle: "toggle"

Dropdown:
  value: $state.selectedValue
  options: $state.options
  optionLabel: "name"
  optionValue: "id"
  onChange: "selectOption"
```

### Interactive Components

```yaml
Button:
  label: "Click Me"
  onTap: "handleClick"
  variant: "primary"  # primary | secondary | outline | ghost | danger
  disabled: $logic.equals($state.value, "")

ForEach:
  data: $state.items
  itemKey: "id"
  template:
    Row:
      children: [...]
```

---

## Style Properties

```yaml
style:
  # Layout
  flex: 1
  gap: 16
  padding: 16
  paddingHorizontal: 16
  paddingVertical: 8
  margin: 8
  width: 200
  height: 100
  minWidth: 100
  maxHeight: 300

  # Alignment
  alignItems: "center"       # flex-start | center | flex-end | stretch
  justifyContent: "center"   # flex-start | center | flex-end | space-between | space-around
  textAlign: "center"        # left | center | right | justify

  # Appearance
  backgroundColor: "#f5f5f5"
  color: "#333333"
  borderRadius: 8
  fontSize: 16
  fontWeight: "bold"         # normal | medium | bold
  fontFamily: "monospace"
  fontStyle: "italic"

  # Other
  overflow: "scroll"         # visible | hidden | scroll
  overflowY: "scroll"
  textDecoration: "line-through"
```

---

## Development Tips

1. **Start Simple:** Begin with `counter.yaml` to understand basic concepts
2. **Tier Selection:** Use Tier 1 (`tier: data`) for maximum compatibility
3. **State Design:** Keep state flat and simple when possible
4. **Reducer Purity:** Reducers should only compute next state, no side effects
5. **Validation:** Test plugins on all target platforms
6. **Performance:** Avoid deep nesting in expressions
7. **Debugging:** Use simple state values for easier debugging

---

## Testing Plugins

### Manual Testing

1. Load plugin definition
2. Verify state initialization
3. Test each reducer
4. Check UI bindings
5. Verify tier compliance

### Platform Testing

- **iOS:** Test Tier 2 plugins downgrade gracefully
- **Android:** Test full Tier 2 capabilities
- **Web:** Test in-browser rendering
- **Desktop:** Test native rendering

---

## Plugin File Structure

```yaml
# Plugin metadata
plugin:
  id: "com.company.plugin-id"
  name: "Plugin Name"
  version: "1.0.0"
  description: "Brief description"
  tier: data  # or logic

# State schema
state:
  variableName: defaultValue

# State reducers
reducers:
  actionName:
    params: [param1, param2]  # Optional
    next_state:
      variableName: $expression

# Scripts (Tier 2 only)
scripts:
  functionName:
    params: [param1]
    body: |
      // JavaScript-like syntax

# User interface
ui:
  ComponentName:
    style: { ... }
    children: [...]
```

---

## References

- **Specification:** `/Universal/Libraries/AvaElements/specs/AVA-Spec-DualTierPluginSystem-50512-V1.md`
- **MEL Documentation:** Coming soon
- **Component Library:** `/Universal/Libraries/AvaElements/components/`

---

**Created:** 2025-12-05
**Version:** 1.0.0
**Status:** Phase 8 Implementation
