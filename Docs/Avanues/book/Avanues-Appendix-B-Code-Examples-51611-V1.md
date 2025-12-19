# Appendix B: Code Examples

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net

---

## Example 1: Login Screen

### JSON DSL
```json
{
  "name": "LoginScreen",
  "stateVariables": [
    { "name": "email", "type": "String", "initialValue": "" },
    { "name": "password", "type": "String", "initialValue": "" }
  ],
  "root": {
    "type": "COLUMN",
    "properties": { "spacing": 16, "padding": 24 },
    "children": [
      {
        "type": "TEXT",
        "properties": { "content": "Welcome Back", "variant": "H1" }
      },
      {
        "type": "TEXT_FIELD",
        "properties": { "label": "Email", "type": "email" },
        "eventHandlers": { "onValueChange": "{ email = it }" }
      },
      {
        "type": "TEXT_FIELD",
        "properties": { "label": "Password", "type": "password" },
        "eventHandlers": { "onValueChange": "{ password = it }" }
      },
      {
        "type": "BUTTON",
        "properties": { "text": "Sign In", "variant": "primary" },
        "eventHandlers": { "onClick": "{ handleLogin(email, password) }" }
      }
    ]
  }
}
```

### Generated Android (Kotlin + Compose)
```kotlin
@Composable
fun LoginScreen() {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge
        )

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = { handleLogin(email, password) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign In")
        }
    }
}
```

---

## Example 2: Dashboard with Cards

### JSON DSL
```json
{
  "name": "Dashboard",
  "root": {
    "type": "SCROLL_VIEW",
    "children": [
      {
        "type": "COLUMN",
        "properties": { "spacing": 16, "padding": 16 },
        "children": [
          {
            "type": "CARD",
            "properties": { "elevation": 4 },
            "children": [
              {
                "type": "TEXT",
                "properties": { "content": "Total Users", "variant": "CAPTION" }
              },
              {
                "type": "TEXT",
                "properties": { "content": "12,543", "variant": "H2" }
              }
            ]
          },
          {
            "type": "CARD",
            "properties": { "elevation": 4 },
            "children": [
              {
                "type": "TEXT",
                "properties": { "content": "Revenue", "variant": "CAPTION" }
              },
              {
                "type": "TEXT",
                "properties": { "content": "$45,678", "variant": "H2" }
              }
            ]
          }
        ]
      }
    ]
  }
}
```

---

## Example 3: Form with Validation

### JSON DSL
```json
{
  "name": "ContactForm",
  "stateVariables": [
    { "name": "name", "type": "String", "initialValue": "" },
    { "name": "email", "type": "String", "initialValue": "" },
    { "name": "message", "type": "String", "initialValue": "" },
    { "name": "errors", "type": "Map<String, String>", "initialValue": {} }
  ],
  "root": {
    "type": "COLUMN",
    "properties": { "spacing": 16 },
    "children": [
      {
        "type": "TEXT_FIELD",
        "properties": { "label": "Name", "errorText": "errors['name']" },
        "eventHandlers": { "onValueChange": "{ name = it; validateName() }" }
      },
      {
        "type": "TEXT_FIELD",
        "properties": { "label": "Email", "type": "email", "errorText": "errors['email']" },
        "eventHandlers": { "onValueChange": "{ email = it; validateEmail() }" }
      },
      {
        "type": "TEXT_FIELD",
        "properties": { "label": "Message", "multiline": true, "rows": 4 },
        "eventHandlers": { "onValueChange": "{ message = it }" }
      },
      {
        "type": "BUTTON",
        "properties": { "text": "Submit" },
        "eventHandlers": { "onClick": "{ handleSubmit() }" }
      }
    ]
  }
}
```

---

## Example 4: Voice Command Integration

### Kotlin
```kotlin
// Register voice commands
val runtime = AvaUIRuntime()
val bridge = VoiceOSBridge.getInstance()

bridge.registerVoiceCommand(VoiceCommand(
    id = "open-settings",
    trigger = "open settings",
    action = "navigate.settings",
    appId = "com.augmentalis.avanue.app"
))

// Handle voice input
launch {
    val match = runtime.handleVoiceCommand("open settings")
    if (match != null) {
        println("Matched: ${match.command.trigger} (${match.confidence})")
        // Navigate to settings
    }
}
```

---

## Example 5: Theme Customization

### JSON
```json
{
  "name": "CustomTheme",
  "colors": {
    "primary": "#0066CC",
    "secondary": "#9C27B0",
    "background": "#F5F5F5",
    "surface": "#FFFFFF",
    "error": "#D32F2F"
  },
  "typography": {
    "h1": { "fontSize": 32, "fontWeight": "bold" },
    "body1": { "fontSize": 16, "fontWeight": "normal" }
  },
  "spacing": {
    "small": 8,
    "medium": 16,
    "large": 24
  },
  "shapes": {
    "button": { "cornerRadius": 8 },
    "card": { "cornerRadius": 12, "elevation": 4 }
  }
}
```

---

## Example 6: Complex List

### JSON DSL
```json
{
  "name": "UserList",
  "stateVariables": [
    { "name": "users", "type": "List<User>", "initialValue": [] }
  ],
  "root": {
    "type": "COLUMN",
    "children": [
      {
        "type": "SEARCH_BAR",
        "properties": { "placeholder": "Search users..." },
        "eventHandlers": { "onSearch": "{ filterUsers(it) }" }
      },
      {
        "type": "SCROLL_VIEW",
        "children": "{{ users.map(user => ListItem(user)) }}"
      }
    ]
  }
}
```

---

**For more examples, see the `/examples` directory in the repository.**

---

**Created by Manoj Jhawar, manoj@ideahq.net**
