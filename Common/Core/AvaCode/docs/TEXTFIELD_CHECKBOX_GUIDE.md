# TextField & Checkbox Component Guide

## Quick Start

### TextField Usage
```kotlin
TextField {
    id = "username"
    placeholder = "Enter your username"
    maxLength = 50
}
```

### Checkbox Usage
```kotlin
Checkbox {
    id = "terms"
    label = "I agree to the terms"
    checked = false
}
```

---

## Generated Code Examples

### Kotlin Compose
```kotlin
// TextField
var usernameText by remember { mutableStateOf("") }
TextField(value = usernameText, onValueChange = { usernameText = it })

// Checkbox
var termsChecked by remember { mutableStateOf(false) }
Checkbox(checked = termsChecked, onCheckedChange = { termsChecked = it })
```

### SwiftUI
```swift
// TextField
@State private var usernameText: String = ""
TextField("placeholder", text: $usernameText)

// Checkbox (Toggle)
@State private var termsChecked: Bool = false
Toggle("label", isOn: $termsChecked)
```

### React TypeScript
```tsx
// TextField
const [usernameText, setUsernameText] = useState<string>("");
<TextField value={usernameText} onChange={(e) => setUsernameText(e.target.value)} />

// Checkbox
const [termsChecked, setTermsChecked] = useState<boolean>(false);
<Checkbox checked={termsChecked} onChange={(e) => setTermsChecked(e.target.checked)} />
```

---

## Full Documentation

See also:
- **test_output_examples.md** - Detailed output examples
- **UPDATE_SUMMARY.md** - Technical implementation details
- **COMPLETION_REPORT.md** - Full project report
