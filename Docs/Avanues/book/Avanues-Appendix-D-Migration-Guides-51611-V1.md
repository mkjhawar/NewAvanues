# Appendix D: Migration Guides

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net

---

## Migrating from v4.0 to v5.0

### Breaking Changes

#### 1. Namespace Change
**Old:** `com.augmentalis.voiceos.avaui`
**New:** `net.ideahq.avamagic`

**Migration:**
```kotlin
// Old
import com.augmentalis.voiceos.avaui.AvaUIRuntime

// New
import net.ideahq.avamagic.runtime.AvaUIRuntime
```

#### 2. Component Types Enum
**Old:** String-based types
**New:** `ComponentType` enum

**Migration:**
```kotlin
// Old
val component = ComponentNode(type = "button", ...)

// New
val component = ComponentNode(type = ComponentType.BUTTON, ...)
```

#### 3. Event Bus API
**Old:** Callback-based
**New:** Kotlin Flow-based

**Migration:**
```kotlin
// Old
eventBus.register("onClick") { event ->
    handleClick(event)
}

// New
launch {
    eventBus.events
        .filter { it.eventName == "onClick" }
        .collect { event ->
            handleClick(event)
        }
}
```

---

## Migrating from v3.x to v5.0

### Major Changes

1. **JSON DSL required** - No more Kotlin DSL
2. **Code generation** - No runtime interpretation
3. **KMP architecture** - Shared code across platforms
4. **Material3** - Material Design 3 (Android)
5. **SwiftUI** - iOS native rendering

### Step-by-Step Migration

#### Step 1: Convert Kotlin DSL to JSON

**Old (Kotlin DSL):**
```kotlin
screen("Login") {
    column {
        text("Welcome")
        textField(label = "Email")
        button("Sign In") {
            onClick = ::handleLogin
        }
    }
}
```

**New (JSON DSL):**
```json
{
  "name": "Login",
  "root": {
    "type": "COLUMN",
    "children": [
      {
        "type": "TEXT",
        "properties": { "content": "Welcome" }
      },
      {
        "type": "TEXT_FIELD",
        "properties": { "label": "Email" }
      },
      {
        "type": "BUTTON",
        "properties": { "text": "Sign In" },
        "eventHandlers": { "onClick": "{ handleLogin() }" }
      }
    ]
  }
}
```

#### Step 2: Generate Platform Code

```bash
# Generate for each platform
avacode generate --input Login.json --platform android --output LoginScreen.kt
avacode generate --input Login.json --platform ios --output LoginView.swift
avacode generate --input Login.json --platform web --output Login.tsx
```

#### Step 3: Update Build Configuration

**build.gradle.kts:**
```kotlin
plugins {
    id("org.jetbrains.kotlin.multiplatform") version "1.9.20"
    id("com.android.library")
}

kotlin {
    android()
    ios()
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("net.ideahq.avamagic:core:5.0.0")
            }
        }
    }
}
```

---

## Migrating Components

### Custom Components

**Old:**
```kotlin
class CustomCard : MagicComponent() {
    override fun render() {
        // Custom rendering
    }
}
```

**New:**
```kotlin
// 1. Register component
registry.register(ComponentDescriptor(
    type = "CustomCard",
    properties = mapOf(
        "title" to PropertyDescriptor("title", PropertyType.STRING)
    ),
    supportsChildren = true
))

// 2. Use in JSON
{
  "type": "CustomCard",
  "properties": { "title": "My Card" }
}
```

---

## Migrating Themes

**Old:**
```kotlin
MagicTheme(
    primaryColor = Color.Blue,
    secondaryColor = Color.Purple
) {
    // Content
}
```

**New:**
```json
{
  "name": "MyTheme",
  "colors": {
    "primary": "#0066CC",
    "secondary": "#9C27B0"
  }
}
```

---

## Migrating Voice Commands

**Old:**
```kotlin
voiceCommand("open settings") {
    navigate(SettingsScreen)
}
```

**New:**
```kotlin
bridge.registerVoiceCommand(VoiceCommand(
    id = "open-settings",
    trigger = "open settings",
    action = "navigate.settings",
    appId = "com.myapp"
))
```

---

## Platform-Specific Migrations

### Android: Material2 → Material3

**Old:**
```kotlin
MaterialTheme {
    Button(onClick = {}) {
        Text("Click")
    }
}
```

**New:**
```kotlin
MaterialTheme {
    Button(onClick = {}) {
        Text("Click")
    }
}
```
*(API same, but colors/typography from Material3)*

### iOS: UIKit → SwiftUI

**Old:**
```swift
class LoginViewController: UIViewController {
    let emailField = UITextField()
    let button = UIButton()
}
```

**New:**
```swift
struct LoginView: View {
    @State private var email: String = ""

    var body: some View {
        VStack {
            TextField("Email", text: $email)
            Button("Sign In", action: handleLogin)
        }
    }
}
```

---

## Data Migration

### State Persistence

**Old:**
```kotlin
preferences.putString("user_email", email)
```

**New:**
```kotlin
stateManager.publish("user.email", email, StateScope.GLOBAL)
```

### Database

**Old:** Room (Android-only)
**New:** SQLDelight (KMP)

```kotlin
// Old (Room)
@Entity
data class User(
    @PrimaryKey val id: Long,
    val name: String
)

// New (SQLDelight)
CREATE TABLE User (
    id INTEGER PRIMARY KEY,
    name TEXT NOT NULL
);
```

---

## Testing Migrations

### Old Tests
```kotlin
@Test
fun testButton() {
    val component = Button("Click")
    assertTrue(component.isRendered)
}
```

### New Tests
```kotlin
@Test
fun testButtonGeneration() {
    val component = ComponentNode(
        type = ComponentType.BUTTON,
        properties = mapOf("text" to "Click")
    )
    val code = generator.generateComponent(component)
    assertTrue(code.contains("Button"))
}
```

---

## Rollback Instructions

If migration fails:

1. **Restore v4.0 code** from backup
2. **Revert dependencies** in build.gradle.kts
3. **Use v4.0 documentation**
4. **Report issues**: https://github.com/augmentalis/avamagic/issues

---

## Migration Checklist

- [ ] Backup current codebase
- [ ] Update dependencies to v5.0
- [ ] Convert Kotlin DSL to JSON DSL
- [ ] Generate platform code
- [ ] Update imports (namespace change)
- [ ] Migrate custom components
- [ ] Update themes
- [ ] Migrate voice commands
- [ ] Update tests
- [ ] Verify all platforms build
- [ ] Test app functionality
- [ ] Deploy to staging
- [ ] Production deployment

---

**Need help? Contact: support@ideahq.net**

---

**Created by Manoj Jhawar, manoj@ideahq.net**
