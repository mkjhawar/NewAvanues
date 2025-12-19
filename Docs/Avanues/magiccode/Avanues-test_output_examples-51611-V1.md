# TextField and Checkbox Generation Examples

This document shows example output from all three AvaCode generators for TextField and Checkbox components.

## Example Input DSL

```kotlin
App("UserForm") {
    TextField {
        id = "username"
        placeholder = "Enter your name"
        maxLength = 50
    }

    TextField {
        id = "email"
        placeholder = "Enter your email"
    }

    Checkbox {
        id = "terms"
        label = "I agree to the terms"
        checked = false
    }

    Checkbox {
        id = "newsletter"
        label = "Subscribe to newsletter"
        checked = true
    }
}
```

---

## 1. Kotlin Compose Generator Output

### TextField with maxLength:
```kotlin
var usernameText by remember { mutableStateOf("") }
TextField(
    value = usernameText,
    onValueChange = { newText ->
        if (newText.length <= 50) {
            usernameText = newText
        }
    },
    placeholder = { Text("Enter your name") },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true
)
```

### TextField without maxLength:
```kotlin
var emailText by remember { mutableStateOf("") }
TextField(
    value = emailText,
    onValueChange = { newText ->
        emailText = newText
    },
    placeholder = { Text("Enter your email") },
    modifier = Modifier.fillMaxWidth(),
    singleLine = true
)
```

### Checkbox (unchecked):
```kotlin
var termsChecked by remember { mutableStateOf(false) }
Row(
    verticalAlignment = Alignment.CenterVertically
) {
    Checkbox(
        checked = termsChecked,
        onCheckedChange = { termsChecked = it }
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text("I agree to the terms")
}
```

### Checkbox (checked):
```kotlin
var newsletterChecked by remember { mutableStateOf(true) }
Row(
    verticalAlignment = Alignment.CenterVertically
) {
    Checkbox(
        checked = newsletterChecked,
        onCheckedChange = { newsletterChecked = it }
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text("Subscribe to newsletter")
}
```

---

## 2. SwiftUI Generator Output

### State Variable Declarations:
```swift
@State private var usernameText: String = ""
@State private var emailText: String = ""
@State private var termsChecked: Bool = false
@State private var newsletterChecked: Bool = true
```

### TextField Components:
```swift
TextField("Enter your name", text: $usernameText)
    .textFieldStyle(.roundedBorder)
    .padding()

TextField("Enter your email", text: $emailText)
    .textFieldStyle(.roundedBorder)
    .padding()
```

### Checkbox Components (using Toggle):
```swift
Toggle("I agree to the terms", isOn: $termsChecked)
    .padding()

Toggle("Subscribe to newsletter", isOn: $newsletterChecked)
    .padding()
```

---

## 3. React TypeScript Generator Output

### State Declarations:
```typescript
const [usernameText, setUsernameText] = useState<string>("");
const [emailText, setEmailText] = useState<string>("");
const [termsChecked, setTermsChecked] = useState<boolean>(false);
const [newsletterChecked, setNewsletterChecked] = useState<boolean>(true);
```

### TextField with maxLength:
```tsx
<TextField
    value={usernameText}
    onChange={(e) => setUsernameText(e.target.value)}
    placeholder="Enter your name"
    inputProps={{ maxLength: 50 }}
    fullWidth
/>
```

### TextField without maxLength:
```tsx
<TextField
    value={emailText}
    onChange={(e) => setEmailText(e.target.value)}
    placeholder="Enter your email"
    fullWidth
/>
```

### Checkbox Components:
```tsx
<FormControlLabel
    control={
        <Checkbox
            checked={termsChecked}
            onChange={(e) => setTermsChecked(e.target.checked)}
        />
    }
    label="I agree to the terms"
/>

<FormControlLabel
    control={
        <Checkbox
            checked={newsletterChecked}
            onChange={(e) => setNewsletterChecked(e.target.checked)}
        />
    }
    label="Subscribe to newsletter"
/>
```

---

## Summary of Features

### TextField Support:
- **All Platforms**:
  - Placeholder text
  - Two-way data binding with state
  - Custom ID for state variable naming

- **Kotlin Compose Only**:
  - maxLength validation (inline validation in onValueChange)

- **React Only**:
  - maxLength via inputProps

### Checkbox Support:
- **All Platforms**:
  - Label text
  - Initial checked state
  - Two-way data binding
  - Custom ID for state variable naming

### Component Types by Platform:
- **Kotlin Compose**: TextField, Checkbox (with Row layout for label)
- **SwiftUI**: TextField, Toggle (native iOS checkbox alternative)
- **React**: TextField (MUI), Checkbox + FormControlLabel (MUI)
