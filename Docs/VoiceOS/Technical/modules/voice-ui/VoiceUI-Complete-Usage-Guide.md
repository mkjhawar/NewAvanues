# VoiceUI Complete Usage Guide

## Table of Contents
1. [Quick Start](#quick-start)
2. [Developer Guide](#developer-guide)
3. [AI Agent Instructions](#ai-agent-instructions)
4. [API Reference](#api-reference)
5. [Best Practices](#best-practices)
6. [Common Patterns](#common-patterns)

---

## Quick Start

### Setup
```kotlin
// Add to build.gradle.kts
dependencies {
    implementation(project(":apps:VoiceUI"))
}

// Initialize in your Application class
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        VoiceUIModule.initialize(this)
    }
}
```

### Create Your First Voice Screen
```kotlin
@Composable
fun MyFirstVoiceScreen() {
    VoiceScreen("home") {
        text("Welcome to VoiceUI")
        button("Get Started") {
            // Action here
        }
    }
}
```

---

## Developer Guide

### 1. Basic Screen Creation

VoiceUI uses a DSL (Domain Specific Language) to make UI creation intuitive and concise.

#### Simple Login Screen
```kotlin
@Composable
fun LoginScreen() {
    VoiceScreen("login") {
        // Title
        text("Welcome Back")
        
        // Input fields
        input("Email")
        password("Password")
        
        // Actions
        button("Login") {
            performLogin()
        }
        
        button("Forgot Password?") {
            navigateToPasswordReset()
        }
    }
}
```

#### With State Management
```kotlin
@Composable
fun LoginScreenWithState() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    
    VoiceScreen("login") {
        text("Sign In")
        
        input(
            label = "Email",
            value = email,
            onValueChange = { email = it }
        )
        
        password(
            label = "Password",
            value = password,
            onValueChange = { password = it }
        )
        
        button("Login") {
            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            }
        }
    }
}
```

### 2. Using Localization

VoiceUI integrates with the system-wide LocalizationManager supporting 42+ languages.

```kotlin
@Composable
fun MultilingualScreen() {
    val currentLocale = LocalizationModule.getInstance(LocalContext.current).languageState.collectAsState()
    
    VoiceScreen("settings") {
        // Text will be automatically translated based on locale
        text("Settings", locale = currentLocale.value)
        
        // Explicit locale override
        text("Bonjour", locale = "fr")
        
        // Dynamic locale switching
        dropdown(
            label = "Language",
            options = listOf("en", "es", "fr", "de", "zh", "ja"),
            selected = currentLocale.value,
            onSelectionChange = { newLocale ->
                LocalizationModule.getInstance(context).setLanguage(newLocale)
            }
        )
    }
}
```

### 3. AI Context Integration

Provide context to help AI understand and assist with your UI.

```kotlin
@Composable
fun SmartFormScreen() {
    VoiceScreen(
        name = "user_registration",
        aiContext = AIContext(
            purpose = "New user registration and onboarding",
            userIntent = "User wants to create an account",
            workflow = "Step 1 of 3 in registration process",
            expectedUserActions = listOf("fill_form", "voice_input", "submit"),
            commonUserErrors = listOf("invalid_email", "weak_password", "missing_fields")
        )
    ) {
        // Form with smart AI assistance
        text("Create Your Account", aiContext = AIContext(
            purpose = "Welcome message for new users"
        ))
        
        input("Full Name", aiContext = AIContext(
            purpose = "Collect user's legal name for account creation",
            expectedUserActions = listOf("type", "voice_dictation"),
            aiSuggestions = listOf("auto_capitalize", "suggest_from_contacts")
        ))
        
        input("Email", aiContext = AIContext(
            purpose = "Primary contact and login credential",
            commonUserErrors = listOf("missing @", "invalid domain"),
            aiSuggestions = listOf("validate_format", "check_availability")
        ))
        
        password(aiContext = AIContext(
            purpose = "Secure account access",
            aiSuggestions = listOf("show_strength_meter", "suggest_strong_password"),
            accessibility = AccessibilityContext(
                screenReaderText = "Password field, 8 characters minimum",
                voiceCommandPriority = 5 // Lower for security
            )
        ))
        
        button("Create Account", aiContext = AIContext(
            purpose = "Submit registration form",
            businessLogic = "Validate all fields, create user account, send verification email",
            successPatterns = listOf("All fields valid", "Email not already registered")
        ))
    }
}
```

### 4. Advanced Components

#### Lists and Dynamic Content
```kotlin
@Composable
fun ProductListScreen(products: List<Product>) {
    VoiceScreen("products") {
        text("Our Products")
        
        list(products) { product ->
            card(title = product.name) {
                text(product.description)
                text("$${product.price}")
                button("Add to Cart") {
                    cartManager.add(product)
                }
            }
        }
    }
}
```

#### Complex Forms
```kotlin
@Composable
fun SettingsScreen() {
    var notifications by remember { mutableStateOf(true) }
    var theme by remember { mutableStateOf("light") }
    var fontSize by remember { mutableStateOf(16f) }
    
    VoiceScreen("settings") {
        section("Preferences") {
            toggle(
                label = "Push Notifications",
                checked = notifications,
                onCheckedChange = { notifications = it }
            )
            
            radioGroup(
                label = "Theme",
                options = listOf("Light", "Dark", "Auto"),
                selected = theme,
                onSelectionChange = { theme = it }
            )
            
            slider(
                label = "Font Size",
                value = fontSize,
                range = 12f..24f,
                onValueChange = { fontSize = it }
            )
        }
        
        section("Account") {
            button("Change Password") { /* ... */ }
            button("Sign Out") { /* ... */ }
        }
    }
}
```

### 5. Voice Commands

Every UI element automatically gets voice commands generated.

```kotlin
@Composable
fun VoiceEnabledScreen() {
    VoiceScreen("shopping") {
        // Auto-generates: "tap search", "click search", "search button"
        button("Search") { /* ... */ }
        
        // Custom voice commands
        button(
            text = "Checkout",
            synonyms = listOf("pay now", "complete purchase", "buy items")
        ) { /* ... */ }
        
        // Voice input with dictation
        input(
            label = "Product name",
            voiceCommands = listOf("search for", "find product", "look for")
        )
    }
}
```

---

## AI Agent Instructions

### For AI Assistants Creating VoiceUI Screens

When asked to create a UI screen using VoiceUI, follow these patterns:

#### 1. Basic Structure
Always wrap UI in a VoiceScreen with a meaningful name:
```kotlin
@Composable
fun ScreenName() {
    VoiceScreen("screen_identifier") {
        // UI elements here
    }
}
```

#### 2. Component Selection Guide

**For Text Display:**
- `text("content")` - Simple text display
- `text("content", aiContext = ...)` - Text with AI context

**For User Input:**
- `input("label")` - Text input field
- `password("label")` - Secure password field
- `dropdown("label", options)` - Selection from list
- `radioGroup("label", options)` - Single selection
- `chipGroup("label", chips)` - Multiple selection
- `slider("label", range)` - Numeric value selection
- `toggle("label")` - Boolean on/off

**For Actions:**
- `button("text") { action }` - Primary actions
- Use descriptive text that explains the action

**For Layout:**
- `section("title") { }` - Group related elements
- `card(title) { }` - Contained content
- `row { }` - Horizontal layout
- `column { }` - Vertical layout
- `spacer(height)` - Add spacing

#### 3. AI Context Best Practices

Always add AIContext when:
- The element has business logic
- User might make errors
- Accessibility is important
- The element is part of a workflow

Example context for a payment form:
```kotlin
input("Card Number", aiContext = AIContext(
    purpose = "Collect payment card number",
    businessLogic = "Validate card format, check card type",
    commonUserErrors = listOf("wrong_length", "invalid_format"),
    aiSuggestions = listOf("auto_format_with_spaces", "detect_card_type"),
    accessibility = AccessibilityContext(
        screenReaderText = "Credit card number input",
        voiceCommandPriority = 8
    )
))
```

#### 4. Localization

For multi-language support:
```kotlin
text("Welcome", locale = "en")  // Explicit locale
text("Welcome", locale = userLocale)  // Dynamic locale
text("Welcome")  // Uses system default
```

#### 5. State Management

Always use proper state management:
```kotlin
var fieldValue by remember { mutableStateOf("") }

input(
    label = "Field",
    value = fieldValue,
    onValueChange = { fieldValue = it }
)
```

### For AI Agents Modifying Existing Screens

When asked to modify VoiceUI screens:

1. **Preserve existing structure** - Don't change the VoiceScreen name unless necessary
2. **Maintain state management** - Keep existing state variables
3. **Add rather than replace** - Add new functionality without removing existing
4. **Update AI context** - Add context to new elements
5. **Consider voice commands** - Ensure new elements have appropriate voice triggers

---

## API Reference

### VoiceScreen
```kotlin
@Composable
fun VoiceScreen(
    name: String,                    // Unique identifier
    locale: String? = null,          // Override locale
    aiContext: AIContext? = null,    // Screen-level AI context
    content: @Composable VoiceScreenScope.() -> Unit
)
```

### VoiceScreenScope Functions

#### Display Components
```kotlin
fun text(content: String, locale: String? = null, aiContext: AIContext? = null)
fun spacer(height: Int = 16)
```

#### Input Components
```kotlin
fun input(label: String, value: String = "", locale: String? = null, onValueChange: ((String) -> Unit)? = null)
fun password(label: String = "Password", value: String = "", locale: String? = null, onValueChange: ((String) -> Unit)? = null)
fun dropdown(label: String, options: List<String>, selected: String? = null, locale: String? = null, onSelectionChange: ((String) -> Unit)? = null)
fun slider(label: String, value: Float = 0.5f, range: ClosedFloatingPointRange<Float> = 0f..1f, locale: String? = null, onValueChange: ((Float) -> Unit)? = null)
fun toggle(label: String, checked: Boolean = false, locale: String? = null, onCheckedChange: ((Boolean) -> Unit)? = null)
fun radioGroup(label: String, options: List<String>, selected: String? = null, locale: String? = null, onSelectionChange: ((String) -> Unit)? = null)
fun chipGroup(label: String, chips: List<String>, selected: Set<String> = emptySet(), multiSelect: Boolean = false, locale: String? = null, onSelectionChange: ((Set<String>) -> Unit)? = null)
fun stepper(label: String, value: Int = 0, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE, locale: String? = null, onValueChange: ((Int) -> Unit)? = null)
```

#### Action Components
```kotlin
fun button(text: String, locale: String? = null, onClick: (() -> Unit)? = null)
```

#### Container Components
```kotlin
fun card(title: String? = null, locale: String? = null, aiContext: AIContext? = null, content: @Composable () -> Unit)
fun section(title: String, locale: String? = null, aiContext: AIContext? = null, content: @Composable () -> Unit)
fun row(locale: String? = null, aiContext: AIContext? = null, content: @Composable RowScope.() -> Unit)
fun column(locale: String? = null, aiContext: AIContext? = null, content: @Composable ColumnScope.() -> Unit)
fun list(items: List<Any>, locale: String? = null, itemContent: @Composable (Any) -> Unit)
```

---

## Best Practices

### 1. Screen Naming
- Use descriptive, unique names: "user_profile", "checkout_payment"
- Avoid generic names: "screen1", "page"
- Use underscores for multi-word: "password_reset"

### 2. Component Organization
```kotlin
VoiceScreen("organized_screen") {
    // 1. Title/Headers
    text("Screen Title")
    
    // 2. Input fields
    section("User Information") {
        input("Name")
        input("Email")
    }
    
    // 3. Options/Settings
    section("Preferences") {
        toggle("Notifications")
        dropdown("Theme", listOf("Light", "Dark"))
    }
    
    // 4. Actions at bottom
    spacer(32)
    button("Save") { }
    button("Cancel") { }
}
```

### 3. Voice Command Optimization
- Use clear, action-oriented button text
- Provide synonyms for common actions
- Test voice commands with different accents
- Consider international users

### 4. Accessibility
- Always provide AIContext for complex interactions
- Set appropriate voiceCommandPriority
- Include screenReaderText for important elements
- Test with accessibility tools

### 5. Performance
- Use remember for expensive computations
- Lazy load lists with many items
- Minimize recompositions
- Profile with Android Studio

---

## Common Patterns

### Login Flow
```kotlin
@Composable
fun LoginFlow() {
    var screen by remember { mutableStateOf("login") }
    
    when (screen) {
        "login" -> LoginScreen(
            onSuccess = { screen = "home" },
            onForgotPassword = { screen = "reset" }
        )
        "reset" -> PasswordResetScreen(
            onBack = { screen = "login" }
        )
        "home" -> HomeScreen()
    }
}
```

### Form Validation
```kotlin
@Composable
fun ValidatedForm() {
    var email by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf<String?>(null) }
    
    VoiceScreen("form") {
        input(
            label = if (emailError != null) "Email - $emailError" else "Email",
            value = email,
            onValueChange = { 
                email = it
                emailError = if (it.contains("@")) null else "Invalid email"
            }
        )
        
        button("Submit") {
            if (emailError == null) {
                submitForm(email)
            }
        }
    }
}
```

### Dynamic Lists
```kotlin
@Composable
fun TodoList() {
    var todos by remember { mutableStateOf(listOf<String>()) }
    var newTodo by remember { mutableStateOf("") }
    
    VoiceScreen("todos") {
        input(
            label = "New Todo",
            value = newTodo,
            onValueChange = { newTodo = it }
        )
        
        button("Add") {
            if (newTodo.isNotEmpty()) {
                todos = todos + newTodo
                newTodo = ""
            }
        }
        
        list(todos) { todo ->
            row {
                text(todo)
                button("Delete") {
                    todos = todos - todo
                }
            }
        }
    }
}
```

---

## Troubleshooting

### Common Issues

**Issue: Locale not working**
```kotlin
// Ensure LocalizationManager is initialized
LocalizationModule.getInstance(context).initialize()
```

**Issue: Voice commands not recognized**
```kotlin
// Check if VoiceUI module is initialized
VoiceUIModule.initialize(context)
```

**Issue: AI context not providing suggestions**
```kotlin
// Ensure AIContext has sufficient detail
aiContext = AIContext(
    purpose = "specific purpose",
    expectedUserActions = listOf("action1", "action2"),
    aiSuggestions = listOf("suggestion1", "suggestion2")
)
```

---

## Summary

VoiceUI makes creating voice-enabled, accessible, and internationalized UIs simple and intuitive. Key benefits:

1. **Simple DSL** - Write less code, get more functionality
2. **Automatic Voice Commands** - Every element is voice-enabled
3. **Built-in Localization** - 42+ languages supported
4. **AI Integration** - Smart assistance and suggestions
5. **Accessibility First** - Designed for all users
6. **Cross-Platform** - Works on all Android devices

Start with simple screens and gradually add advanced features as needed. The API is designed to be discoverable and intuitive.