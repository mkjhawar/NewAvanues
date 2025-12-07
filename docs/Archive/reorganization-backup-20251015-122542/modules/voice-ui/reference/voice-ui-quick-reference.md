# VoiceUI Quick Reference Card

## 🚀 Quick Start (3 Steps)

### 1. Add Dependency
```kotlin
implementation(project(":apps:VoiceUI"))
```

### 2. Initialize
```kotlin
VoiceUIModule.initialize(context)
```

### 3. Create Screen
```kotlin
@Composable
fun MyScreen() {
    VoiceScreen("my_screen") {
        text("Hello VoiceUI!")
        button("Click Me") { println("Clicked!") }
    }
}
```

---

## 📱 Component Cheat Sheet

### Display
```kotlin
text("Hello World")                    // Simple text
text("Localized", locale = "es")      // Spanish text
spacer(16)                             // 16dp spacing
```

### Input
```kotlin
input("Email")                         // Text input
password("Password")                   // Secure input
```

### Selection
```kotlin
toggle("Enable", checked = true)       // On/off switch
dropdown("Color", listOf("Red", "Blue")) // Dropdown menu
radioGroup("Size", listOf("S", "M", "L")) // Radio buttons
chipGroup("Tags", listOf("A", "B", "C")) // Chip selection
slider("Volume", 0f..100f)             // Slider
stepper("Quantity", 1, min = 0, max = 10) // Number stepper
```

### Actions
```kotlin
button("Submit") { submitForm() }      // Action button
```

### Containers
```kotlin
card(title = "Title") { content() }    // Card container
section("Title") { content() }         // Section grouping
row { content() }                      // Horizontal layout
column { content() }                   // Vertical layout
list(items) { item -> content(item) }  // Dynamic list
```

---

## 🎯 Common Patterns

### Form with State
```kotlin
@Composable
fun FormScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    VoiceScreen("form") {
        input("Email", value = email, onValueChange = { email = it })
        password("Password", value = password, onValueChange = { password = it })
        button("Submit") { 
            login(email, password)
        }
    }
}
```

### List with Actions
```kotlin
@Composable
fun ListScreen(items: List<Item>) {
    VoiceScreen("list") {
        list(items) { item ->
            card(title = item.name) {
                text(item.description)
                button("Select") { selectItem(item) }
            }
        }
    }
}
```

### Settings Screen
```kotlin
@Composable
fun SettingsScreen() {
    var darkMode by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    
    VoiceScreen("settings") {
        section("Appearance") {
            toggle("Dark Mode", darkMode) { darkMode = it }
        }
        section("Notifications") {
            toggle("Push Notifications", notifications) { notifications = it }
        }
    }
}
```

---

## 🌍 Localization

```kotlin
// Get current locale
val locale = LocalizationModule.getInstance(context).languageState.collectAsState()

// Use locale in components
text("Welcome", locale = locale.value)
input("Name", locale = "fr")  // Force French

// Supported languages: 42+ including
// en, es, fr, de, it, pt, ru, zh, ja, ko, ar, nl, pl, tr, hi, th...
```

---

## 🤖 AI Context

```kotlin
// Add context to any component
text("Welcome", aiContext = AIContext(
    purpose = "Greeting message",
    userIntent = "User opened the app"
))

input("Email", aiContext = AIContext(
    purpose = "Collect email for login",
    commonUserErrors = listOf("missing @", "typos"),
    aiSuggestions = listOf("validate format", "auto-complete domain")
))

button("Submit", aiContext = AIContext(
    purpose = "Submit form data",
    businessLogic = "Validate and send to server",
    successPatterns = listOf("All fields valid")
))
```

---

## 🎤 Voice Commands

Every component automatically gets voice commands:

| Component | Auto-Generated Commands |
|-----------|------------------------|
| `button("Save")` | "tap save", "click save", "save button" |
| `input("Email")` | "enter email", "email field", "type email" |
| `toggle("Dark Mode")` | "toggle dark mode", "enable dark mode", "turn on dark mode" |

### Custom Voice Commands
```kotlin
button(
    text = "Purchase",
    synonyms = listOf("buy", "checkout", "pay now")
) { }
```

---

## ⚡ State Management

### Always Outside VoiceScreen
```kotlin
@Composable
fun MyScreen() {
    // ✅ State declarations here
    var value by remember { mutableStateOf("") }
    
    VoiceScreen("screen") {
        // ❌ NOT here
        input("Field", value = value, onValueChange = { value = it })
    }
}
```

---

## 🎨 Complete Example

```kotlin
@Composable
fun CompleteLoginScreen(
    onLogin: (String, String) -> Unit,
    onRegister: () -> Unit
) {
    // State management
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var rememberMe by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Localization
    val locale = LocalizationModule.getInstance(LocalContext.current)
        .languageState.collectAsState()
    
    VoiceScreen(
        name = "login",
        locale = locale.value,
        aiContext = AIContext(
            purpose = "User authentication",
            workflow = "App entry point"
        )
    ) {
        // Header
        text("Welcome Back", locale = locale.value)
        spacer(24)
        
        // Form fields
        section("Sign In") {
            input(
                label = "Email",
                value = email,
                locale = locale.value,
                onValueChange = { 
                    email = it
                    error = null
                },
                aiContext = AIContext(
                    purpose = "User email for authentication",
                    commonUserErrors = listOf("invalid format")
                )
            )
            
            password(
                label = "Password",
                value = password,
                locale = locale.value,
                onValueChange = { 
                    password = it
                    error = null
                }
            )
            
            toggle(
                label = "Remember Me",
                checked = rememberMe,
                onCheckedChange = { rememberMe = it }
            )
        }
        
        // Error display
        error?.let {
            text(it, aiContext = AIContext(
                purpose = "Show authentication error"
            ))
        }
        
        // Actions
        spacer(32)
        button("Sign In") {
            when {
                email.isEmpty() -> error = "Email required"
                password.isEmpty() -> error = "Password required"
                else -> onLogin(email, password)
            }
        }
        
        button("Create Account") {
            onRegister()
        }
    }
}
```

---

## 📝 Remember

1. **DSL only** inside VoiceScreen
2. **State outside** VoiceScreen
3. **Labels required** for all inputs
4. **Voice enabled** automatically
5. **Locale supported** on all text
6. **AIContext optional** but recommended
7. **Accessibility built-in**

---

## 🔗 Links

- [Complete Usage Guide](VoiceUI-Complete-Usage-Guide.md)
- [AI Agent Guide](VoiceUI-AI-Agent-Guide.md)
- [API Reference](VoiceUI-Complete-API-Reference.md)
- [Architecture](VoiceUI-Architecture.md)