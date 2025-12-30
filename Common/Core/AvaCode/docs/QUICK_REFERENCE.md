# AvaCode Codegen Quick Reference

**One-page reference for common tasks**

---

## Component Mappings Cheat Sheet

### Text Component
```kotlin
// DSL
Text { text = "Hello"; size = 24f; color = "#007AFF" }

// Kotlin
Text(text = "Hello", fontSize = 24.sp, color = Color(0xFF007AFF))

// Swift
Text("Hello").font(.system(size: 24)).foregroundColor(Color(hex: "#007AFF"))

// React
<span style={{ fontSize: '24px', color: '#007AFF' }}>Hello</span>
```

### Button Component
```kotlin
// DSL
Button { text = "Click"; onClick = { action() } }

// Kotlin
Button(onClick = { action() }) { Text("Click") }

// Swift
Button("Click") { action() }

// React
<button onClick={() => action()}>Click</button>
```

### Container Component
```kotlin
// DSL
Container { orientation = "vertical"; children = [...] }

// Kotlin
Column { /* children */ }

// Swift
VStack { /* children */ }

// React
<div style={{ display: 'flex', flexDirection: 'column' }}>/* children */</div>
```

---

## State Management Cheat Sheet

### Local State
```kotlin
// Kotlin
var count by remember { mutableStateOf(0) }

// Swift
@State private var count = 0

// React
const [count, setCount] = useState(0);
```

### Derived State
```kotlin
// Kotlin
val isValid by remember { derivedStateOf { count > 0 } }

// Swift
var isValid: Bool { count > 0 }

// React
const isValid = useMemo(() => count > 0, [count]);
```

### Side Effects
```kotlin
// Kotlin
LaunchedEffect(key) { /* effect */ }

// Swift
.onChange(of: key) { /* effect */ }

// React
useEffect(() => { /* effect */ }, [key]);
```

---

## Type Conversions

| Type | Kotlin | Swift | TypeScript |
|------|--------|-------|------------|
| String | `String` | `String` | `string` |
| Number | `Int` / `Float` | `Int` / `Double` | `number` |
| Boolean | `Boolean` | `Bool` | `boolean` |
| Color | `Color(0xFFRRGGBB)` | `Color(hex: "#RRGGBB")` | `"#RRGGBB"` |

---

## Callback Signatures

| DSL | Kotlin | Swift | React |
|-----|--------|-------|-------|
| `() => void` | `() -> Unit` | `() -> Void` | `() => void` |
| `(value: T) => void` | `(T) -> Unit` | `(T) -> Void` | `(value: T) => void` |

---

## Common Imports

### Kotlin Compose
```kotlin
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
```

### SwiftUI
```swift
import SwiftUI
import Foundation
```

### React
```typescript
import React, { useState, useEffect } from 'react';
```

---

## File Templates

### Kotlin Component
```kotlin
@Composable
fun ComponentName(
    modifier: Modifier = Modifier,
    prop: Type = default,
    callback: () -> Unit = {}
) {
    var state by remember { mutableStateOf(initial) }

    Container(modifier) {
        // Content
    }
}
```

### Swift Component
```swift
struct ComponentName: View {
    let prop: Type
    var callback: () -> Void

    @State private var state = initial

    var body: some View {
        Container {
            // Content
        }
    }
}
```

### React Component
```typescript
export const ComponentName: React.FC<Props> = ({
    prop = default,
    callback
}) => {
    const [state, setState] = useState(initial);

    return (
        <Container>
            {/* Content */}
        </Container>
    );
};
```

---

## Component Properties

### ColorPicker
- `initialColor: COLOR` (default: "#FFFFFF")
- `mode: ENUM` (FULL, COMPACT, etc.)
- `showAlpha: BOOLEAN` (default: true)
- Callbacks: `onColorChanged`, `onConfirm`, `onCancel`

### Text
- `text: STRING` (required)
- `size: FLOAT` (default: 16f)
- `color: COLOR` (default: "#000000")

### Button
- `text: STRING` (required)
- `enabled: BOOLEAN` (default: true)
- Callbacks: `onClick`

### Container
- `orientation: ENUM` (vertical, horizontal, default: vertical)
- Supports children: Yes

### Preferences
- API component (no UI)
- Methods: `getString`, `putString`, `getInt`, etc.

---

## Error Handling

```kotlin
sealed class CodeGenError {
    data class InvalidComponent(type: String, message: String)
    data class InvalidProperty(name: String, expectedType: PropertyType)
    data class MissingRequiredProperty(componentType: String, propertyName: String)
}
```

---

## Validation

```kotlin
fun validate(component: ComponentModel): ValidationResult {
    // Check required properties
    // Validate property types
    // Check enum values
    // Validate children support
}
```

---

## Generation Pipeline

```
DSL Input → Parser → ComponentModel → Validator → Mapper → Template → Formatter → Output
```

---

## Useful Commands

```bash
# Generate code
avacode generate --target kotlin-compose

# Watch mode
avacode generate --watch

# Validate only
avacode validate input.magic

# Format output
avacode format generated/
```

---

## Common Patterns

### Modal Presentation
```kotlin
// Kotlin
var showDialog by remember { mutableStateOf(false) }
if (showDialog) { Dialog { /* content */ } }

// Swift
@State var showSheet = false
.sheet(isPresented: $showSheet) { /* content */ }

// React
const [showModal, setShowModal] = useState(false);
{showModal && <Modal>/* content */</Modal>}
```

### List Rendering
```kotlin
// Kotlin
items.forEach { item -> ItemView(item) }

// Swift
ForEach(items) { item in ItemView(item) }

// React
{items.map(item => <ItemView key={item.id} item={item} />)}
```

---

## Documentation Links

- **Full Design**: [TARGET_FRAMEWORK_MAPPINGS.md](./TARGET_FRAMEWORK_MAPPINGS.md)
- **Implementation**: [CODE_GENERATION_UTILITIES.md](./CODE_GENERATION_UTILITIES.md)
- **Summary**: [CODEGEN_DESIGN_SUMMARY.md](./CODEGEN_DESIGN_SUMMARY.md)
- **Index**: [README_CODEGEN_DESIGN.md](./README_CODEGEN_DESIGN.md)

---

**Version**: 1.0.0
**Last Updated**: 2025-10-28
