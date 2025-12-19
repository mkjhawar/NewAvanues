# MEL Plugin System - Quick Start Guide

Get started with the MagicUI Expression Language (MEL) Plugin System in 5 minutes.

---

## Installation

```bash
npm install @augmentalis/avaelements-web
# or
yarn add @augmentalis/avaelements-web
```

Dependencies automatically installed:
- `yaml` (YAML parsing)
- `@mui/material` (UI components)
- `react` (framework)

---

## Your First Plugin (3 Steps)

### Step 1: Create a Plugin Definition

Create `counter.yaml`:

```yaml
plugin:
  id: "counter"
  name: "Simple Counter"
  version: "1.0.0"
  tier: data  # Tier 1 (works everywhere)

state:
  count: 0

reducers:
  increment:
    next_state:
      count: $math.add($state.count, 1)

  decrement:
    next_state:
      count: $math.subtract($state.count, 1)

  reset:
    next_state:
      count: 0

ui:
  Column:
    style: { padding: 16, gap: 16 }
    children:
      - Text:
          value: $state.count
          style:
            fontSize: 48
            textAlign: center

      - Row:
          style: { gap: 8 }
          children:
            - Button:
                label: "-"
                onTap: "decrement"
                variant: "outlined"
                style: { flex: 1 }

            - Button:
                label: "Reset"
                onTap: "reset"
                variant: "outlined"
                style: { flex: 1 }

            - Button:
                label: "+"
                onTap: "increment"
                variant: "contained"
                style: { flex: 1 }
```

### Step 2: Import and Use

```tsx
import React from 'react';
import { MELPlugin } from '@augmentalis/avaelements-web';
import counterYaml from './counter.yaml?raw'; // Vite raw import

function App() {
  return (
    <div style={{ maxWidth: 400, margin: '0 auto', padding: 32 }}>
      <h1>Counter Plugin</h1>
      <MELPlugin plugin={counterYaml} />
    </div>
  );
}

export default App;
```

### Step 3: Run

```bash
npm run dev
```

That's it! You now have a working counter with state management.

---

## Common Patterns

### Pattern 1: Inline YAML

```tsx
const counterYaml = `
plugin:
  id: "counter"
  name: "Counter"
  version: "1.0.0"
  tier: data

state:
  count: 0

reducers:
  increment:
    next_state:
      count: $math.add($state.count, 1)

ui:
  Column:
    children:
      - Text: { value: $state.count }
      - Button: { label: "+", onTap: "increment" }
`;

<MELPlugin plugin={counterYaml} />
```

### Pattern 2: JSON Definition

```tsx
const counterJson = {
  plugin: {
    id: "counter",
    name: "Counter",
    version: "1.0.0",
    tier: "data"
  },
  state: {
    count: { type: "number", default: 0 }
  },
  reducers: {
    increment: {
      next_state: {
        count: { raw: "$math.add($state.count, 1)" }
      }
    }
  },
  ui: {
    type: "Column",
    children: [
      { type: "Text", bindings: { value: { raw: "$state.count" } } },
      { type: "Button", props: { label: "+" }, events: { onTap: "increment" } }
    ]
  }
};

<MELPlugin plugin={counterJson} />
```

### Pattern 3: With Persistence

```tsx
<MELPlugin
  plugin={counterYaml}
  options={{
    persist: true,
    storageKey: 'my-counter-state'
  }}
/>
```

### Pattern 4: Using the Hook

```tsx
import { useMELPlugin } from '@augmentalis/avaelements-web';

function Counter() {
  const { state, dispatch } = useMELPlugin(counterYaml);

  return (
    <div>
      <h1>Count: {state.count}</h1>
      <button onClick={() => dispatch('increment')}>+</button>
      <button onClick={() => dispatch('decrement')}>-</button>
      <button onClick={() => dispatch('reset')}>Reset</button>
    </div>
  );
}
```

---

## Built-in Functions Cheat Sheet

### Math
```yaml
$math.add(1, 2)           # 3
$math.subtract(5, 2)      # 3
$math.multiply(2, 3)      # 6
$math.divide(10, 2)       # 5
$math.round(3.7)          # 4
$math.max(1, 5, 3)        # 5
```

### String
```yaml
$string.concat("Hello", " ", "World")  # "Hello World"
$string.uppercase("hello")             # "HELLO"
$string.length("test")                 # 4
$string.substring("hello", 0, 2)       # "he"
```

### Array
```yaml
$array.length([1, 2, 3])            # 3
$array.append([1, 2], 3)            # [1, 2, 3]
$array.first([1, 2, 3])             # 1
$array.last([1, 2, 3])              # 3
```

### Logic
```yaml
$logic.if($state.x > 5, "big", "small")
$logic.and(true, false)              # false
$logic.equals($state.a, $state.b)
$logic.gt($state.count, 10)          # count > 10
```

---

## Supported Components

| Component | Props | Events |
|-----------|-------|--------|
| `Text` | value, variant, style | - |
| `Button` | label, enabled, variant, style | onTap |
| `TextField` | value, label, placeholder | onChange |
| `Checkbox` | checked | onChange |
| `Switch` | checked | onChange |
| `Column` | spacing, style | - |
| `Row` | spacing, style | - |
| `Container` | padding, style | - |
| `Card` | elevation | - |
| `Divider` | margin | - |

---

## Event Syntax

```yaml
# Simple dispatch
Button: { label: "Click", onTap: "myAction" }

# With parameters
Button: { label: "Add 5", onTap: "increment(5)" }

# Multiple parameters
Button: { label: "Set", onTap: "setValue(10, true)" }
```

---

## Common Recipes

### Recipe 1: Form with Validation

```yaml
state:
  name: ""
  email: ""
  isValid: false

reducers:
  setName:
    params: [value]
    next_state:
      name: $value
      isValid: $logic.and(
        $logic.gt($string.length($value), 0),
        $logic.gt($string.length($state.email), 0)
      )

  setEmail:
    params: [value]
    next_state:
      email: $value
      isValid: $logic.and(
        $logic.gt($string.length($state.name), 0),
        $logic.gt($string.length($value), 0)
      )

ui:
  Column:
    children:
      - TextField:
          value: $state.name
          label: "Name"
          onChange: "setName"
      - TextField:
          value: $state.email
          label: "Email"
          onChange: "setEmail"
      - Button:
          label: "Submit"
          enabled: $state.isValid
          onTap: "submit"
```

### Recipe 2: Todo List

```yaml
state:
  todos: []
  newTodo: ""

reducers:
  addTodo:
    next_state:
      todos: $array.append($state.todos, $state.newTodo)
      newTodo: ""

  removeTodo:
    params: [index]
    next_state:
      todos: $array.remove($state.todos, $index)

  updateNewTodo:
    params: [value]
    next_state:
      newTodo: $value

ui:
  Column:
    children:
      - Row:
          children:
            - TextField:
                value: $state.newTodo
                placeholder: "Enter todo"
                onChange: "updateNewTodo"
            - Button:
                label: "Add"
                onTap: "addTodo"
      # List rendering would need custom component
```

### Recipe 3: Settings Panel

```yaml
state:
  darkMode: false
  notifications: true
  soundEnabled: true

reducers:
  toggleDarkMode:
    next_state:
      darkMode: $logic.not($state.darkMode)

  toggleNotifications:
    next_state:
      notifications: $logic.not($state.notifications)

  toggleSound:
    next_state:
      soundEnabled: $logic.not($state.soundEnabled)

ui:
  Column:
    children:
      - Row:
          children:
            - Text: { value: "Dark Mode" }
            - Switch:
                checked: $state.darkMode
                onChange: "toggleDarkMode"

      - Row:
          children:
            - Text: { value: "Notifications" }
            - Switch:
                checked: $state.notifications
                onChange: "toggleNotifications"

      - Row:
          children:
            - Text: { value: "Sound" }
            - Switch:
                checked: $state.soundEnabled
                onChange: "toggleSound"
```

---

## Debugging Tips

### 1. Inspect State

```tsx
const { state, runtime } = useMELPlugin(plugin);

console.log('Current state:', state);
console.log('Plugin tier:', runtime.tier);
```

### 2. Error Handling

```tsx
<MELPlugin
  plugin={definition}
  options={{
    onError: (error) => {
      console.error('MEL Error:', error.message);
      console.error('Error context:', error.context);
    }
  }}
/>
```

### 3. Expression Testing

```tsx
const { evaluate } = useMELPlugin(plugin);

const result = evaluate({ raw: '$math.add(1, 2)' });
console.log('Expression result:', result); // 3
```

---

## Performance Tips

1. **Keep expressions simple** - Complex expressions are re-evaluated on every render
2. **Use memoization** - Wrap `<MELPlugin>` in `React.memo` if parent re-renders often
3. **Minimize bindings** - Only bind what needs to be reactive
4. **Batch updates** - Combine multiple state changes into one reducer

---

## Next Steps

- Read the [full documentation](./src/mel/README.md)
- Explore [examples](./examples/)
- Check out the [specification](../specs/AVA-Spec-DualTierPluginSystem-50512-V1.md)
- Build your own plugins!

---

## Need Help?

- API Reference: `/src/mel/README.md`
- Examples: `/examples/`
- Type Definitions: `/src/mel/types.ts`

---

**Version:** 1.0.0
**Last Updated:** 2025-12-05
