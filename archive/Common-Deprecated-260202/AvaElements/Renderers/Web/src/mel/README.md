# MEL Plugin System - Web Renderer

Implementation of the MagicUI Expression Language (MEL) Plugin Runtime for the Web/React renderer.

## Overview

The MEL Plugin System enables self-contained, reactive plugins (calculators, forms, games) with full **Tier 2** support on Web.

### Tier Support

| Tier | Description | Web Support |
|------|-------------|-------------|
| **Tier 1 (Data)** | Apple-safe reactive templates with state bindings | ✅ Full Support |
| **Tier 2 (Logic)** | Full expression engine with conditionals, loops, custom functions | ✅ Full Support |

Web platform supports **both tiers** without restrictions.

## Quick Start

### Basic Usage

```tsx
import { MELPlugin } from '@augmentalis/avaelements-web';

const calculatorYaml = `
plugin:
  id: "calculator"
  name: "Calculator"
  version: "1.0.0"
  tier: logic

state:
  display: "0"
  buffer: ""
  operator: null

reducers:
  appendDigit:
    params: [digit]
    next_state:
      display: $logic.if(
        $logic.equals($state.display, "0"),
        $digit,
        $string.concat($state.display, $digit)
      )

  calculate:
    next_state:
      display: $math.eval($state.buffer, $state.operator, $state.display)
      buffer: ""
      operator: null

ui:
  Column:
    children:
      - Text:
          value: $state.display
      - Button:
          label: "1"
          onTap: "appendDigit(1)"
`;

function Calculator() {
  return <MELPlugin plugin={calculatorYaml} />;
}
```

### Using the Hook

```tsx
import { useMELPlugin } from '@augmentalis/avaelements-web';

function CustomCalculator() {
  const { state, dispatch, runtime } = useMELPlugin(calculatorYaml);

  return (
    <div>
      <h1>{runtime.definition.metadata.name}</h1>
      <div>Display: {state.display}</div>
      <button onClick={() => dispatch('appendDigit', { digit: '5' })}>
        5
      </button>
    </div>
  );
}
```

## Architecture

```
┌─────────────────────────────────────────────┐
│         MEL Plugin Definition (YAML)        │
│  Metadata | State | Reducers | UI           │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│           useMELPlugin Hook                 │
│  • Parse definition                         │
│  • Initialize state                         │
│  • Create evaluator                         │
│  • Manage re-renders                        │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│      MELExpressionEvaluator                 │
│  • Parse expressions                        │
│  • Evaluate state bindings                  │
│  • Execute built-in functions               │
│  • Enforce tier permissions                 │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│        MELComponentFactory                  │
│  • Map UINode to React components           │
│  • Resolve bindings                         │
│  • Wire events to dispatch                  │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
                React DOM
```

## Files

| File | Purpose |
|------|---------|
| `types.ts` | TypeScript type definitions |
| `MELExpressionEvaluator.ts` | Expression parser and evaluator |
| `useMELPlugin.ts` | React hook for state management |
| `MELComponentFactory.tsx` | UINode to React component mapper |
| `MELPluginRenderer.tsx` | Main plugin renderer component |
| `index.ts` | Public API exports |

## Expression Language

### State References

```yaml
ui:
  Text:
    value: $state.count  # Simple reference
    visible: $state.count > 0  # Expression binding (Tier 2)
```

### Built-in Functions (Tier 1)

```yaml
reducers:
  calculate:
    next_state:
      result: $math.add($state.a, $state.b)
      name: $string.concat($state.firstName, " ", $state.lastName)
      hasItems: $logic.gt($array.length($state.items), 0)
```

### Full Expressions (Tier 2)

```yaml
reducers:
  validate:
    next_state:
      isValid: $state.age >= 18 && $state.name.length > 0
      discount: $logic.if($state.total > 100, $state.total * 0.1, 0)
```

## Built-in Functions

### Math Functions

- `$math.add(a, b)` - Addition
- `$math.subtract(a, b)` - Subtraction
- `$math.multiply(a, b)` - Multiplication
- `$math.divide(a, b)` - Division
- `$math.mod(a, b)` - Modulo
- `$math.abs(a)` - Absolute value
- `$math.round(a)` - Round to nearest integer
- `$math.floor(a)` - Round down
- `$math.ceil(a)` - Round up
- `$math.min(...args)` - Minimum value
- `$math.max(...args)` - Maximum value
- `$math.pow(a, b)` - Power
- `$math.sqrt(a)` - Square root
- `$math.eval(a, op, b)` - Evaluate operator

### String Functions

- `$string.concat(...args)` - Concatenate strings
- `$string.length(str)` - String length
- `$string.substring(str, start, end)` - Extract substring
- `$string.uppercase(str)` - Convert to uppercase
- `$string.lowercase(str)` - Convert to lowercase
- `$string.trim(str)` - Trim whitespace
- `$string.replace(str, search, replacement)` - Replace text
- `$string.split(str, separator)` - Split into array
- `$string.join(arr, separator)` - Join array into string

### Array Functions

- `$array.length(arr)` - Array length
- `$array.get(arr, index)` - Get element at index
- `$array.first(arr)` - First element
- `$array.last(arr)` - Last element
- `$array.append(arr, item)` - Append item
- `$array.prepend(arr, item)` - Prepend item
- `$array.remove(arr, index)` - Remove at index
- `$array.sort(arr)` - Sort array
- `$array.reverse(arr)` - Reverse array
- `$array.slice(arr, start, end)` - Extract slice

### Logic Functions

- `$logic.if(condition, thenValue, elseValue)` - Conditional
- `$logic.and(...args)` - Logical AND
- `$logic.or(...args)` - Logical OR
- `$logic.not(value)` - Logical NOT
- `$logic.equals(a, b)` - Equality check
- `$logic.gt(a, b)` - Greater than
- `$logic.lt(a, b)` - Less than
- `$logic.gte(a, b)` - Greater than or equal
- `$logic.lte(a, b)` - Less than or equal

### Storage Functions (Tier 2)

- `$storage.get(key)` - Get from localStorage
- `$storage.set(key, value)` - Set in localStorage
- `$storage.remove(key)` - Remove from localStorage
- `$storage.clear()` - Clear all storage

## Supported Components

The MEL Component Factory maps UINode types to Material-UI components:

| MEL Type | React Component | Props |
|----------|-----------------|-------|
| `Text` | `Typography` | value, variant, style |
| `Button` | `Button` | label, onTap, enabled, variant |
| `TextField` | `TextField` | value, label, placeholder, onChange |
| `Checkbox` | `Checkbox` | checked, onChange |
| `Switch` | `Switch` | checked, onChange |
| `Column` | `Stack` | children, spacing, style |
| `Row` | `Stack` | children, spacing, style |
| `Container` | `Box` | children, padding, style |
| `Card` | `Card` | children, elevation |
| `Divider` | `Divider` | margin |

### Extending Components

```tsx
import { registerComponent } from '@augmentalis/avaelements-web';

registerComponent('MyButton', (node, runtime, children) => {
  return (
    <button onClick={() => runtime.dispatch(node.events.onTap)}>
      {children}
    </button>
  );
});
```

## Event Handling

Events are bound to reducers using the `onTap` (and similar) properties:

```yaml
ui:
  Button:
    label: "Click Me"
    onTap: "myReducer"  # Simple dispatch

  Button:
    label: "Add 5"
    onTap: "increment(5)"  # With parameters

  Button:
    label: "Submit"
    onTap: "submit"
```

Parameters are extracted and passed to the reducer:

```yaml
reducers:
  increment:
    params: [arg0]  # arg0 will be 5 from "increment(5)"
    next_state:
      count: $math.add($state.count, $arg0)
```

## State Persistence

Enable localStorage persistence:

```tsx
<MELPlugin
  plugin={definition}
  options={{
    persist: true,
    storageKey: 'my-plugin-state', // optional
  }}
/>
```

## Error Handling

```tsx
<MELPlugin
  plugin={definition}
  options={{
    onError: (error) => {
      console.error('MEL Error:', error.message);
    },
  }}
  errorComponent={(error) => (
    <div className="error">
      <h3>Plugin Error</h3>
      <p>{error.message}</p>
    </div>
  )}
/>
```

## Components

### MELPlugin

Main plugin renderer component.

```tsx
<MELPlugin
  plugin={yamlOrJson}
  options={{ persist: true }}
  onError={(error) => console.error(error)}
  loadingComponent={<Spinner />}
  errorComponent={(error) => <ErrorDisplay error={error} />}
  sx={{ padding: 2 }}
/>
```

### StatefulMELPlugin

Plugin with external state control.

```tsx
const [externalState, setExternalState] = useState({});

<StatefulMELPlugin
  plugin={definition}
  state={externalState}
  onStateChange={setExternalState}
/>
```

### MELPluginPreview

Preview component for plugin marketplace.

```tsx
<MELPluginPreview
  plugin={definition}
  height={400}
  showMetadata={true}
/>
```

## API Reference

### useMELPlugin(input, options)

React hook for MEL plugin state management.

**Parameters:**
- `input`: Plugin definition (YAML string, JSON string, or object)
- `options`: Configuration options

**Returns:**
```typescript
{
  state: PluginState;           // Current state
  dispatch: (action, params?) => void;  // Dispatch reducer
  runtime: PluginRuntime;       // Runtime instance
  definition: PluginDefinition; // Parsed definition
  tier: PluginTier;            // Effective tier
  loading: boolean;            // Loading state
  error: MELError | null;      // Error state
}
```

### MELExpressionEvaluator

Expression parser and evaluator.

```typescript
const evaluator = new MELExpressionEvaluator(tier, state, params);
const result = evaluator.evaluate({ raw: '$math.add(1, 2)' });
```

## Security

### Tier 1 Restrictions

- Only whitelisted functions allowed
- No arbitrary expressions
- No loops or conditionals in reducers

### Tier 2 Permissions

- Full expression support
- Conditional logic
- Custom functions
- Extended APIs (storage, etc.)

Web platform supports both tiers without restriction.

## Performance

### Optimizations

- Memoized plugin definition parsing
- Lazy expression parsing (parse on first use)
- Immutable state updates (copy-on-write)
- React.memo for component factory

### Best Practices

- Keep UI tree shallow
- Minimize bindings in hot paths
- Use state persistence for large state
- Batch multiple state updates into single reducer

## Examples

See `/Volumes/M-Drive/Coding/Avanues/Universal/Libraries/AvaElements/examples/plugins/` for:

- `calculator.yaml` - Basic calculator
- `todo-list.yaml` - Todo list app
- `quiz-game.yaml` - Quiz game
- `unit-converter.yaml` - Unit converter

## Version

- **MEL Version:** 1.0.0
- **Web Renderer Version:** 3.3.0
- **Tier Support:** Tier 1 + Tier 2

## Dependencies

- `react` >= 18.0.0
- `@mui/material` >= 5.0.0
- `yaml` (for YAML parsing)

## License

Proprietary - Augmentalis Inc.
