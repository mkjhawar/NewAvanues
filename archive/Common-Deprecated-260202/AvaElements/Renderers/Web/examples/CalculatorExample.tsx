/**
 * Calculator Plugin Example
 * Demonstrates MEL Plugin System usage in React
 */

import React from 'react';
import { MELPlugin, MELPluginPreview, useMELPlugin } from '../src/mel';

// Example 1: YAML as string
const calculatorYAML = `
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

ui:
  Column:
    style: { padding: 16 }
    children:
      - Text:
          value: $state.display
          style:
            fontSize: 48
            textAlign: end
            fontFamily: monospace

      - Row:
          style: { gap: 8 }
          children:
            - Button: { label: "7", onTap: "appendDigit(7)", style: { flex: 1 } }
            - Button: { label: "8", onTap: "appendDigit(8)", style: { flex: 1 } }
            - Button: { label: "9", onTap: "appendDigit(9)", style: { flex: 1 } }
            - Button: { label: "÷", onTap: "setOperator('/')", variant: "outlined", style: { flex: 1 } }

      - Row:
          style: { gap: 8 }
          children:
            - Button: { label: "4", onTap: "appendDigit(4)", style: { flex: 1 } }
            - Button: { label: "5", onTap: "appendDigit(5)", style: { flex: 1 } }
            - Button: { label: "6", onTap: "appendDigit(6)", style: { flex: 1 } }
            - Button: { label: "×", onTap: "setOperator('*')", variant: "outlined", style: { flex: 1 } }

      - Row:
          style: { gap: 8 }
          children:
            - Button: { label: "1", onTap: "appendDigit(1)", style: { flex: 1 } }
            - Button: { label: "2", onTap: "appendDigit(2)", style: { flex: 1 } }
            - Button: { label: "3", onTap: "appendDigit(3)", style: { flex: 1 } }
            - Button: { label: "−", onTap: "setOperator('-')", variant: "outlined", style: { flex: 1 } }

      - Row:
          style: { gap: 8 }
          children:
            - Button: { label: "C", onTap: "clear", variant: "outlined", style: { flex: 1 } }
            - Button: { label: "0", onTap: "appendDigit(0)", style: { flex: 1 } }
            - Button: { label: "=", onTap: "calculate", variant: "contained", style: { flex: 1 } }
            - Button: { label: "+", onTap: "setOperator('+')", variant: "outlined", style: { flex: 1 } }
`;

// ============================================================================
// Example 1: Simple Usage
// ============================================================================

export function SimpleCalculatorExample() {
  return (
    <div>
      <h1>Simple Calculator</h1>
      <MELPlugin plugin={calculatorYAML} />
    </div>
  );
}

// ============================================================================
// Example 2: With Persistence
// ============================================================================

export function PersistentCalculatorExample() {
  return (
    <div>
      <h1>Calculator with State Persistence</h1>
      <MELPlugin
        plugin={calculatorYAML}
        options={{
          persist: true,
          storageKey: 'calculator-state',
        }}
      />
      <p>
        <small>Your calculator state is saved in localStorage</small>
      </p>
    </div>
  );
}

// ============================================================================
// Example 3: Using the Hook
// ============================================================================

export function HookCalculatorExample() {
  const { state, dispatch, runtime, error } = useMELPlugin(calculatorYAML);

  if (error) {
    return <div>Error: {error.message}</div>;
  }

  return (
    <div>
      <h1>Calculator (Using Hook)</h1>

      <div style={{ padding: 16, backgroundColor: '#f5f5f5', borderRadius: 8 }}>
        <h2>State Inspector</h2>
        <pre>{JSON.stringify(state, null, 2)}</pre>
      </div>

      <div style={{ marginTop: 16 }}>
        <h2>Plugin UI</h2>
        <MELPlugin plugin={calculatorYAML} />
      </div>

      <div style={{ marginTop: 16 }}>
        <h2>Custom Controls</h2>
        <button onClick={() => dispatch('clear')}>Clear</button>
        <button onClick={() => dispatch('appendDigit', { digit: '5' })}>
          Add 5
        </button>
      </div>
    </div>
  );
}

// ============================================================================
// Example 4: Plugin Preview (for marketplace)
// ============================================================================

export function CalculatorPreviewExample() {
  return (
    <div>
      <h1>Calculator Plugin Preview</h1>
      <MELPluginPreview
        plugin={calculatorYAML}
        height={500}
        showMetadata={true}
      />
    </div>
  );
}

// ============================================================================
// Example 5: Error Handling
// ============================================================================

export function ErrorHandlingExample() {
  const [errors, setErrors] = React.useState<string[]>([]);

  return (
    <div>
      <h1>Calculator with Error Handling</h1>

      <MELPlugin
        plugin={calculatorYAML}
        options={{
          onError: (error) => {
            setErrors((prev) => [...prev, error.message]);
          },
        }}
        errorComponent={(error) => (
          <div style={{ padding: 16, backgroundColor: '#ffe0e0', color: '#c00' }}>
            <strong>Plugin Error:</strong> {error.message}
          </div>
        )}
      />

      {errors.length > 0 && (
        <div style={{ marginTop: 16 }}>
          <h2>Error Log</h2>
          <ul>
            {errors.map((error, i) => (
              <li key={i}>{error}</li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
}

// ============================================================================
// Example 6: Loading from File
// ============================================================================

export function FileLoadedCalculatorExample() {
  const [pluginYAML, setPluginYAML] = React.useState<string | null>(null);

  React.useEffect(() => {
    // In a real app, you would fetch this from your plugin marketplace API
    fetch('/plugins/calculator.yaml')
      .then((res) => res.text())
      .then((yaml) => setPluginYAML(yaml))
      .catch((err) => console.error('Failed to load plugin:', err));
  }, []);

  if (!pluginYAML) {
    return <div>Loading plugin...</div>;
  }

  return (
    <div>
      <h1>Calculator Loaded from File</h1>
      <MELPlugin plugin={pluginYAML} />
    </div>
  );
}

// ============================================================================
// All Examples Component
// ============================================================================

export default function AllCalculatorExamples() {
  const [activeExample, setActiveExample] = React.useState(0);

  const examples = [
    { name: 'Simple', component: <SimpleCalculatorExample /> },
    { name: 'Persistent', component: <PersistentCalculatorExample /> },
    { name: 'Hook', component: <HookCalculatorExample /> },
    { name: 'Preview', component: <CalculatorPreviewExample /> },
    { name: 'Error Handling', component: <ErrorHandlingExample /> },
    { name: 'File Loaded', component: <FileLoadedCalculatorExample /> },
  ];

  return (
    <div style={{ padding: 32 }}>
      <h1>MEL Plugin System - Calculator Examples</h1>

      <div style={{ marginBottom: 16 }}>
        {examples.map((example, i) => (
          <button
            key={i}
            onClick={() => setActiveExample(i)}
            style={{
              marginRight: 8,
              padding: '8px 16px',
              backgroundColor: activeExample === i ? '#1976d2' : '#e0e0e0',
              color: activeExample === i ? 'white' : 'black',
              border: 'none',
              borderRadius: 4,
              cursor: 'pointer',
            }}
          >
            {example.name}
          </button>
        ))}
      </div>

      <div style={{ border: '1px solid #e0e0e0', borderRadius: 8, padding: 16 }}>
        {examples[activeExample].component}
      </div>
    </div>
  );
}
