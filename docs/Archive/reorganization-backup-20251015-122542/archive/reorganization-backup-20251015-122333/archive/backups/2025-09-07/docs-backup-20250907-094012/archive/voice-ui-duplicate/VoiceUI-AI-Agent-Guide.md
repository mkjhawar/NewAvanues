# VoiceUI AI Agent Guide

## Instructions for AI Assistants Using VoiceUI

This guide is specifically written for AI agents (like Claude, GPT, etc.) to understand how to create and modify VoiceUI screens effectively.

---

## Core Principles for AI Agents

### 1. Always Use the DSL Pattern
VoiceUI uses a Domain Specific Language. Never create UI using raw Compose components when inside a VoiceScreen.

✅ **CORRECT:**
```kotlin
VoiceScreen("login") {
    text("Welcome")
    input("Email")
    button("Login") { }
}
```

❌ **INCORRECT:**
```kotlin
VoiceScreen("login") {
    Text("Welcome")  // Wrong - use text()
    TextField(...)    // Wrong - use input()
    Button(...)       // Wrong - use button()
}
```

### 2. State Management Rules

Always declare state variables OUTSIDE the VoiceScreen block:

✅ **CORRECT:**
```kotlin
@Composable
fun MyScreen() {
    var email by remember { mutableStateOf("") }  // State here
    
    VoiceScreen("my_screen") {
        input("Email", value = email, onValueChange = { email = it })
    }
}
```

❌ **INCORRECT:**
```kotlin
@Composable
fun MyScreen() {
    VoiceScreen("my_screen") {
        var email by remember { mutableStateOf("") }  // Wrong location
        input("Email", value = email, onValueChange = { email = it })
    }
}
```

---

## Step-by-Step Screen Creation

### When Asked: "Create a login screen"

```kotlin
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit = {},
    onForgotPassword: () -> Unit = {}
) {
    // Step 1: Declare state variables
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Step 2: Create VoiceScreen with meaningful name
    VoiceScreen(
        name = "login",
        aiContext = AIContext(
            purpose = "User authentication screen",
            userIntent = "User wants to sign in to their account",
            workflow = "Entry point to authenticated app experience"
        )
    ) {
        // Step 3: Add title/header
        text("Welcome Back", aiContext = AIContext(
            purpose = "Greeting for returning users"
        ))
        
        // Step 4: Add input fields with proper labels
        input(
            label = "Email Address",
            value = email,
            locale = null,  // Uses system locale
            onValueChange = { email = it },
            aiContext = AIContext(
                purpose = "Collect user email for authentication",
                expectedUserActions = listOf("type", "voice_dictation", "paste"),
                commonUserErrors = listOf("typos", "wrong_domain", "missing_@")
            )
        )
        
        password(
            label = "Password",
            value = password,
            onValueChange = { password = it },
            aiContext = AIContext(
                purpose = "Collect user password securely",
                expectedUserActions = listOf("type"),  // No voice for security
                commonUserErrors = listOf("caps_lock", "forgotten")
            )
        )
        
        // Step 5: Show error if exists
        errorMessage?.let {
            text(it, aiContext = AIContext(
                purpose = "Display authentication error to user"
            ))
        }
        
        // Step 6: Add primary action
        button(
            text = if (isLoading) "Signing in..." else "Sign In",
            onClick = {
                if (!isLoading && email.isNotEmpty() && password.isNotEmpty()) {
                    isLoading = true
                    // Simulate login
                    onLoginSuccess()
                }
            }
        )
        
        // Step 7: Add secondary actions
        spacer(16)
        button(
            text = "Forgot Password?",
            onClick = onForgotPassword
        )
    }
}
```

---

## Common Request Patterns

### Request: "Add a settings screen with toggles"

```kotlin
@Composable
fun SettingsScreen() {
    // State for each setting
    var notificationsEnabled by remember { mutableStateOf(true) }
    var darkModeEnabled by remember { mutableStateOf(false) }
    var autoSaveEnabled by remember { mutableStateOf(true) }
    var language by remember { mutableStateOf("en") }
    
    VoiceScreen("settings") {
        text("Settings")
        
        section("Preferences") {
            toggle(
                label = "Push Notifications",
                checked = notificationsEnabled,
                onCheckedChange = { notificationsEnabled = it }
            )
            
            toggle(
                label = "Dark Mode",
                checked = darkModeEnabled,
                onCheckedChange = { darkModeEnabled = it }
            )
            
            toggle(
                label = "Auto-Save",
                checked = autoSaveEnabled,
                onCheckedChange = { autoSaveEnabled = it }
            )
        }
        
        section("Localization") {
            dropdown(
                label = "Language",
                options = listOf("en", "es", "fr", "de", "zh"),
                selected = language,
                onSelectionChange = { language = it }
            )
        }
        
        spacer(32)
        button("Save Settings") {
            // Save logic here
        }
    }
}
```

### Request: "Create a product listing"

```kotlin
@Composable
fun ProductListScreen(
    products: List<Product>,
    onProductClick: (Product) -> Unit,
    onAddToCart: (Product) -> Unit
) {
    VoiceScreen("product_list") {
        text("Our Products")
        
        list(products) { product ->
            card(title = product.name) {
                text(product.description)
                text("Price: $${product.price}")
                
                row {
                    button("View Details") {
                        onProductClick(product)
                    }
                    button("Add to Cart") {
                        onAddToCart(product)
                    }
                }
            }
        }
    }
}

data class Product(
    val id: String,
    val name: String,
    val description: String,
    val price: Double
)
```

### Request: "Make a form with validation"

```kotlin
@Composable
fun RegistrationForm(
    onSubmit: (UserData) -> Unit
) {
    // Form state
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var age by remember { mutableStateOf(18) }
    var country by remember { mutableStateOf("USA") }
    var agreeToTerms by remember { mutableStateOf(false) }
    
    // Validation state
    var nameError by remember { mutableStateOf<String?>(null) }
    var emailError by remember { mutableStateOf<String?>(null) }
    
    VoiceScreen("registration") {
        text("Create Account")
        
        // Name input with validation
        input(
            label = if (nameError != null) "Name - $nameError" else "Full Name",
            value = name,
            onValueChange = { 
                name = it
                nameError = when {
                    it.isEmpty() -> "Name is required"
                    it.length < 2 -> "Name too short"
                    else -> null
                }
            }
        )
        
        // Email with validation
        input(
            label = if (emailError != null) "Email - $emailError" else "Email Address",
            value = email,
            onValueChange = {
                email = it
                emailError = when {
                    it.isEmpty() -> "Email is required"
                    !it.contains("@") -> "Invalid email format"
                    else -> null
                }
            }
        )
        
        // Age stepper
        stepper(
            label = "Age",
            value = age,
            min = 13,
            max = 120,
            onValueChange = { age = it }
        )
        
        // Country selection
        dropdown(
            label = "Country",
            options = listOf("USA", "Canada", "UK", "Australia", "Other"),
            selected = country,
            onSelectionChange = { country = it }
        )
        
        // Terms agreement
        toggle(
            label = "I agree to the Terms and Conditions",
            checked = agreeToTerms,
            onCheckedChange = { agreeToTerms = it }
        )
        
        // Submit button with validation
        button("Create Account") {
            val isValid = nameError == null && 
                         emailError == null && 
                         agreeToTerms
            
            if (isValid) {
                onSubmit(UserData(name, email, age, country))
            }
        }
    }
}

data class UserData(
    val name: String,
    val email: String,
    val age: Int,
    val country: String
)
```

---

## Component Selection Matrix

Use this table to choose the right component:

| User Need | Component | Example |
|-----------|-----------|---------|
| Display text | `text()` | `text("Hello World")` |
| Text input | `input()` | `input("Name")` |
| Password | `password()` | `password("Password")` |
| Yes/No choice | `toggle()` | `toggle("Enable notifications")` |
| Select one from many | `radioGroup()` or `dropdown()` | `dropdown("Country", countries)` |
| Select multiple | `chipGroup()` | `chipGroup("Tags", tags, multiSelect = true)` |
| Number selection | `stepper()` or `slider()` | `slider("Volume", 0f..100f)` |
| Action/Submit | `button()` | `button("Save") { }` |
| Group related items | `section()` | `section("Personal Info") { }` |
| Container with border | `card()` | `card("Product") { }` |
| Horizontal layout | `row()` | `row { button("Yes") button("No") }` |
| Vertical layout | `column()` | `column { text("1") text("2") }` |
| Spacing | `spacer()` | `spacer(16)` |
| Dynamic list | `list()` | `list(items) { item -> text(item) }` |

---

## AI Context Guidelines

### When to Add AIContext

Add AIContext when:
1. Element has business logic
2. User might make errors
3. Element is part of a workflow
4. Accessibility is important

### AIContext Template

```kotlin
aiContext = AIContext(
    // What is this element for?
    purpose = "Clear description of element purpose",
    
    // What does the user want to do?
    userIntent = "What the user is trying to accomplish",
    
    // What can go wrong?
    commonUserErrors = listOf("error1", "error2"),
    
    // How can AI help?
    aiSuggestions = listOf("suggestion1", "suggestion2"),
    
    // How do users interact?
    expectedUserActions = listOf("tap", "voice", "type"),
    
    // Accessibility needs
    accessibility = AccessibilityContext(
        screenReaderText = "Description for screen readers",
        voiceCommandPriority = 8  // 1-10, higher = more important
    )
)
```

---

## Localization Integration

### Always Support Multiple Languages

```kotlin
@Composable
fun InternationalScreen() {
    val context = LocalContext.current
    val currentLocale = LocalizationModule.getInstance(context)
        .languageState.collectAsState()
    
    VoiceScreen("international") {
        // Auto-translated based on system locale
        text("Welcome", locale = currentLocale.value)
        
        // Explicit locale
        text("Bonjour", locale = "fr")
        
        // With fallback
        val greeting = when(currentLocale.value) {
            "es" -> "Hola"
            "fr" -> "Bonjour"
            "de" -> "Hallo"
            else -> "Hello"
        }
        text(greeting)
    }
}
```

---

## Error Handling Patterns

### Always Include Error States

```kotlin
@Composable
fun ScreenWithErrors() {
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var data by remember { mutableStateOf<String?>(null) }
    
    VoiceScreen("data_screen") {
        when {
            isLoading -> {
                text("Loading...")
            }
            error != null -> {
                text("Error: $error")
                button("Retry") {
                    error = null
                    loadData()
                }
            }
            data != null -> {
                text(data)
            }
            else -> {
                button("Load Data") {
                    loadData()
                }
            }
        }
    }
}
```

---

## Voice Command Best Practices

### 1. Use Clear Action Words
```kotlin
// Good
button("Search Products") { }
button("Add to Cart") { }
button("Complete Purchase") { }

// Bad
button("Go") { }
button("OK") { }
button("Submit") { }
```

### 2. Provide Synonyms
```kotlin
button(
    text = "Sign In",
    synonyms = listOf("login", "log in", "authenticate", "enter")
) { }
```

### 3. Voice-Friendly Input Labels
```kotlin
// Good - clear pronunciation
input("Email Address")
input("Phone Number")
input("Zip Code")

// Bad - ambiguous
input("Email")
input("Phone")
input("ZIP")
```

---

## Common Mistakes to Avoid

### ❌ Don't Create State Inside VoiceScreen
```kotlin
// WRONG
VoiceScreen("bad") {
    var value by remember { mutableStateOf("") }  // Don't do this
}
```

### ❌ Don't Use Compose Components Directly
```kotlin
// WRONG
VoiceScreen("bad") {
    Text("Hello")      // Use text() instead
    Button(...)        // Use button() instead
    TextField(...)     // Use input() instead
}
```

### ❌ Don't Forget Voice Commands
```kotlin
// WRONG
button("") { }  // Empty text means no voice command

// RIGHT
button("Submit Form") { }  // Clear voice target
```

### ❌ Don't Ignore Accessibility
```kotlin
// WRONG
input("")  // No label

// RIGHT
input("Email Address")  // Clear label for screen readers
```

---

## Quick Reference for AI Agents

When creating VoiceUI screens:

1. **Start with VoiceScreen wrapper**
2. **Declare state variables first**
3. **Use DSL functions only** (text, input, button, etc.)
4. **Add meaningful labels** to all inputs
5. **Include AIContext** for complex elements
6. **Support localization** with locale parameter
7. **Provide voice synonyms** for important actions
8. **Group related elements** with section/card
9. **Add proper spacing** with spacer()
10. **Handle loading and error states**

Remember: VoiceUI is designed to be simple, accessible, and voice-enabled by default. Every element you create is automatically voice-commanded, localized, and accessible.