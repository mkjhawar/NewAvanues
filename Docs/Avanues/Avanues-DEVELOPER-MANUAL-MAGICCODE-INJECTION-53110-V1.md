# AvaCode Content Injection - Developer Manual

**Version**: 1.0.0
**Date**: 2025-10-31 21:16 PDT
**Platform**: Avanues Ecosystem
**Created by**: Manoj Jhawar, manoj@ideahq.net

---

## Table of Contents

1. [Overview](#overview)
2. [AvaCode Content Injection Architecture](#avacode-content-injection-architecture)
3. [AvaUI Interface System](#avaui-interface-system)
4. [DSL Syntax Reference](#dsl-syntax-reference)
5. [YAML Format Reference](#yaml-format-reference)
6. [Kotlin/Java/KMP Integration](#kotlinjavakmp-integration)
7. [IPC Communication Protocols](#ipc-communication-protocols)
8. [Security & Sandboxing](#security--sandboxing)
9. [Examples & Best Practices](#examples--best-practices)
10. [API Reference](#api-reference)

---

## 1. Overview

### What is AvaCode?

**AvaCode** is a declarative UI description language for the Avanues ecosystem that allows:
- **Content Injection**: External apps can inject UI into host apps (like AvanueLaunch)
- **Cross-Platform**: Write once, render on Android, iOS, Web, Desktop
- **Type-Safe**: Validated DSL with compile-time checking
- **IPC-Ready**: Transmitted via Intent, AIDL, ContentProvider, WebSocket
- **Sandboxed**: Runs in isolated environment with permission controls

### AvaUI vs AvaCode

| Aspect | AvaUI | AvaCode |
|--------|---------|-----------|
| **Purpose** | Programmatic UI building | Declarative UI description |
| **Language** | Kotlin DSL | String DSL or YAML |
| **Usage** | Internal app code | External injection via IPC |
| **Validation** | Compile-time | Runtime parsing |
| **Security** | Full app permissions | Sandboxed, limited permissions |

---

## 2. AvaCode Content Injection Architecture

### 2.1 System Overview

```
┌─────────────────────┐
│  External App       │
│  (Content Provider) │
└──────────┬──────────┘
           │ AvaCode DSL String
           │ or YAML Document
           ▼
┌─────────────────────┐
│  IPC Layer          │
│  (Intent/AIDL/etc)  │
└──────────┬──────────┘
           │
           ▼
┌─────────────────────┐
│  Host App           │
│  (AvanueLaunch)     │
│  ┌───────────────┐  │
│  │AvaCodeParser│  │
│  └───────┬───────┘  │
│          │          │
│          ▼          │
│  ┌───────────────┐  │
│  │SecuritySandbox│  │
│  └───────┬───────┘  │
│          │          │
│          ▼          │
│  ┌───────────────┐  │
│  │Platform       │  │
│  │Renderer       │  │
│  │(Compose/etc)  │  │
│  └───────────────┘  │
└─────────────────────┘
```

### 2.2 Injection Flow

1. **External App** creates AvaCode (DSL or YAML)
2. **IPC Transport** sends to host app via Intent/AIDL/etc
3. **AvaCodeParser** validates and parses the code
4. **SecuritySandbox** checks permissions and validates content
5. **Platform Renderer** converts to native UI (Compose/SwiftUI/React)
6. **Panel Container** displays the injected UI

### 2.3 Supported Panels

AvanueLaunch exposes 6 panels for content injection:

| Panel ID | Location | Default Content | AvaCode Support |
|----------|----------|-----------------|-------------------|
| `search` | Top bar | Search input + AVA button | ✅ Full |
| `topApps` | Center-top | 5x2 app grid | ✅ Full |
| `pinnedApps` | Bottom-left | Pinned favorites list | ✅ Full |
| `continueSessions` | Bottom-right | Recent sessions | ✅ Full |
| `voiceBar` | Bottom bar | Voice status + system icons | ✅ Partial |
| `commandBar` | Left side | 5 command buttons | ✅ Limited |

---

## 3. AvaUI Interface System

### 3.1 Programming Interfaces

AvaUI can be used in **3 ways**:

#### A. Kotlin DSL (Internal App Code)

```kotlin
import com.augmentalis.avaelements.dsl.*

val ui = AvaUI {
    theme = GlassAvanue.Light

    Column(spacing = 16) {
        Text("Hello World", style = Typography.headlineLarge)
        Button("Click Me") {
            onClick = { println("Clicked!") }
        }
    }
}

// Render with platform renderer
val renderer = ComposeRenderer()
ui.render(renderer)
```

#### B. AvaCode DSL String (IPC Injection)

```kotlin
val magicCode = """
    AvaUI {
        theme = "GlassAvanue.Light"

        Column(spacing = 16) {
            Text("Hello World", style = "headlineLarge")
            Button("Click Me", onClick = "showToast('Clicked!')")
        }
    }
"""

// Parse and render
val parser = AvaCodeParser()
val ui = parser.parse(magicCode)
ui.render(renderer)
```

#### C. YAML Format (Configuration-Based)

```yaml
avaui:
  version: "1.0"
  theme: "GlassAvanue.Light"

  root:
    type: "Column"
    spacing: 16
    children:
      - type: "Text"
        content: "Hello World"
        style: "headlineLarge"

      - type: "Button"
        label: "Click Me"
        onClick:
          action: "showToast"
          message: "Clicked!"
```

### 3.2 Interface Comparison

| Feature | Kotlin DSL | AvaCode DSL | YAML |
|---------|------------|---------------|------|
| **Type Safety** | ✅ Compile-time | ⚠️ Runtime | ⚠️ Runtime |
| **IDE Support** | ✅ Full autocomplete | ⏳ Basic | ⏳ Schema-based |
| **Learning Curve** | Medium | Easy | Easy |
| **Flexibility** | ✅ Full Kotlin power | ⚠️ Sandboxed functions | ❌ Limited |
| **IPC Compatible** | ❌ No | ✅ Yes | ✅ Yes |
| **File Size** | N/A (compiled) | Small (string) | Smallest (YAML) |
| **Best For** | Internal app UI | Dynamic injection | Configuration |

---

## 4. DSL Syntax Reference

### 4.1 Basic Structure

```kotlin
AvaUI {
    // Theme (optional)
    theme = "GlassAvanue.Light"  // or GlassAvanue.Dark, GlassAvanue.Auto

    // Root component
    Column {
        // Child components...
    }
}
```

### 4.2 Layout Components

#### Column - Vertical Layout

```kotlin
Column(
    spacing = 16,              // Gap between children (dp)
    alignment = "center",      // start|center|end
    padding = Padding(16)      // All sides
) {
    Text("Item 1")
    Text("Item 2")
    Text("Item 3")
}
```

#### Row - Horizontal Layout

```kotlin
Row(
    spacing = 12,
    alignment = "center",      // top|center|bottom
    arrangement = "spaceBetween"  // start|center|end|spaceBetween|spaceAround
) {
    Icon("home")
    Text("Home")
}
```

#### Container - Single Child Box

```kotlin
Container(
    width = "fill",            // fill|auto|<number>
    height = 200,              // dp
    padding = Padding(top = 8, bottom = 8),
    background = Color("#46CBFF", alpha = 0.75),
    cornerRadius = 24
) {
    Text("Content")
}
```

#### ScrollView - Scrollable Content

```kotlin
ScrollView(
    direction = "vertical"     // vertical|horizontal|both
) {
    Column {
        repeat(20) { i ->
            Text("Item $i")
        }
    }
}
```

#### Card - Glass Panel

```kotlin
Card(
    cornerRadius = 24,
    glassOpacity = 0.75,
    blurRadius = 25,
    elevation = 8
) {
    Column(padding = 16) {
        Text("Card Title")
        Text("Card content...")
    }
}
```

### 4.3 Basic Components

#### Text - Display Text

```kotlin
Text(
    text = "Hello World",
    style = "headlineLarge",   // displayLarge|headlineLarge|titleMedium|bodyMedium|labelSmall
    color = "#46CBFF",
    maxLines = 2,
    overflow = "ellipsis"      // clip|ellipsis|visible
)
```

#### Button - Clickable Button

```kotlin
Button(
    label = "Click Me",
    onClick = "handleClick()",  // Function name to call
    enabled = true,
    style = "filled",          // filled|outlined|text
    leadingIcon = "check"
)
```

#### TextField - Text Input

```kotlin
TextField(
    value = state.searchQuery,
    onValueChange = "updateSearch()",
    placeholder = "Search...",
    leadingIcon = "search",
    maxLines = 1,
    keyboardType = "text"      // text|number|email|phone|url
)
```

#### Icon - Display Icon

```kotlin
Icon(
    name = "home",             // Material Icons name or SF Symbol
    size = 24,
    color = "#46CBFF",
    tint = "primary"           // primary|secondary|tertiary|onSurface
)
```

#### Image - Display Image

```kotlin
Image(
    source = "https://example.com/image.jpg",  // URL or asset://path
    contentMode = "cover",     // cover|contain|fill|fitWidth|fitHeight
    width = 100,
    height = 100,
    cornerRadius = 12
)
```

### 4.4 State Management

```kotlin
AvaUI {
    // Declare state
    state {
        var counter by remember { 0 }
        var text by remember { "" }
    }

    Column {
        Text("Count: $counter")

        Button("Increment") {
            onClick = "counter++"
        }

        TextField(
            value = text,
            onValueChange = "text = newValue"
        )
    }
}
```

### 4.5 Conditional Rendering

```kotlin
Column {
    if (state.showDetail) {
        Card {
            Text("Detail view")
        }
    }

    when (state.status) {
        "loading" -> Spinner()
        "error" -> Text("Error occurred", color = "error")
        "success" -> Text("Success!", color = "primary")
    }
}
```

### 4.6 Lists and Iteration

```kotlin
LazyColumn {
    items(state.appList) { app ->
        AppItem(
            name = app.name,
            icon = app.icon,
            onClick = "launchApp('${app.id}')"
        )
    }
}

// Or simple repeat
Column {
    repeat(5) { index ->
        Text("Item $index")
    }
}
```

---

## 5. YAML Format Reference

### 5.1 Basic Structure

```yaml
avaui:
  version: "1.0"
  theme: "GlassAvanue.Light"

  # Optional state declarations
  state:
    counter: 0
    searchQuery: ""
    showDetail: false

  # Root component
  root:
    type: "Column"
    spacing: 16
    children:
      # ... child components
```

### 5.2 Component Format

Every component follows this structure:

```yaml
- type: "ComponentName"
  # Properties
  property1: value1
  property2: value2

  # Children (for layout components)
  children:
    - type: "ChildComponent1"
      # ...
    - type: "ChildComponent2"
      # ...
```

### 5.3 Examples

#### Simple Card with Text

```yaml
root:
  type: "Card"
  cornerRadius: 24
  glassOpacity: 0.75
  elevation: 8
  children:
    - type: "Column"
      padding: 16
      spacing: 8
      children:
        - type: "Text"
          content: "Welcome to AvanueLaunch"
          style: "headlineMedium"
          color: "#46CBFF"

        - type: "Text"
          content: "Glassmorphic launcher"
          style: "bodyMedium"
```

#### Interactive Search Bar

```yaml
root:
  type: "Row"
  spacing: 12
  padding: 16
  children:
    - type: "Icon"
      name: "search"
      size: 24
      color: "#46CBFF"

    - type: "TextField"
      value: "${state.searchQuery}"
      onValueChange:
        action: "setState"
        key: "searchQuery"
        value: "${newValue}"
      placeholder: "Search or ask AVA..."
      flex: 1

    - type: "Button"
      label: ""
      icon: "mic"
      style: "text"
      onClick:
        action: "custom"
        function: "activateVoiceSearch"
```

#### App Grid

```yaml
root:
  type: "LazyColumn"
  children:
    - type: "Text"
      content: "Top Apps"
      style: "titleMedium"
      color: "#46CBFF"

    - type: "Grid"
      columns: 5
      spacing: 12
      items:
        forEach: "${state.topApps}"
        template:
          type: "Column"
          alignment: "center"
          children:
            - type: "Icon"
              name: "${item.icon}"
              size: 48
              onClick:
                action: "custom"
                function: "launchApp"
                params:
                  appId: "${item.id}"

            - type: "Text"
              content: "${item.name}"
              style: "labelSmall"
              maxLines: 1
```

### 5.4 State Binding

Use `${expression}` syntax for state bindings:

```yaml
- type: "Text"
  content: "${state.counter}"  # Read state

- type: "Button"
  label: "Increment"
  onClick:
    action: "setState"           # Write state
    key: "counter"
    value: "${state.counter + 1}"
```

### 5.5 Actions

Supported action types:

```yaml
onClick:
  action: "navigate"             # Navigate to route
  route: "/details"

onClick:
  action: "showToast"            # Show toast message
  message: "Hello!"

onClick:
  action: "custom"               # Call custom function
  function: "handleClick"
  params:
    id: 123
    name: "example"

onClick:
  action: "setState"             # Update state
  key: "showDetail"
  value: true

onClick:
  action: "broadcast"            # Send broadcast intent
  intentAction: "com.example.ACTION"
  extras:
    key1: "value1"
```

---

## 6. Kotlin/Java/KMP Integration

### 6.1 Creating AvaUI Programmatically (Kotlin)

```kotlin
import com.augmentalis.avaelements.dsl.*
import com.augmentalis.avaelements.themes.GlassAvanue

// Build UI using DSL
val launcherUI = AvaUI {
    theme = GlassAvanue.Light

    Column(spacing = 16.dp) {
        // Search panel
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp),
            cornerRadius = 24.dp
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { state.searchQuery = it },
                placeholder = "Search or ask AVA..."
            )
        }

        // App grid
        LazyGrid(
            columns = 5,
            spacing = 12.dp
        ) {
            items(topApps) { app ->
                AppIcon(
                    name = app.name,
                    icon = app.icon,
                    onClick = { launchApp(app.id) }
                )
            }
        }
    }
}

// Render
val renderer = ComposeRenderer()
launcherUI.render(renderer)
```

### 6.2 Parsing AvaCode DSL (Kotlin)

```kotlin
import com.augmentalis.avaelements.parser.AvaCodeParser

val magicCodeString = """
    AvaUI {
        theme = "GlassAvanue.Light"
        Column {
            Text("Dynamic UI from external app!")
            Button("Action") { onClick = "handleAction()" }
        }
    }
"""

// Parse
val parser = AvaCodeParser()
val ui = parser.parse(magicCodeString)

// Render
ui.render(ComposeRenderer())
```

### 6.3 Parsing YAML (Kotlin)

```kotlin
import com.augmentalis.avaelements.parser.YAMLParser

val yamlString = """
    avaui:
      version: "1.0"
      theme: "GlassAvanue.Light"
      root:
        type: "Column"
        children:
          - type: "Text"
            content: "From YAML"
"""

// Parse
val parser = YAMLParser()
val ui = parser.parse(yamlString)

// Render
ui.render(ComposeRenderer())
```

### 6.4 Java Integration

```java
import com.augmentalis.avaelements.AvaUI;
import com.augmentalis.avaelements.parser.AvaCodeParser;
import com.augmentalis.avaelements.renderer.ComposeRenderer;

// Parse AvaCode
AvaCodeParser parser = new AvaCodeParser();
AvaUI ui = parser.parse(magicCodeString);

// Render
ComposeRenderer renderer = new ComposeRenderer();
ui.render(renderer);
```

### 6.5 KMP (Kotlin Multiplatform)

```kotlin
// Common code (shared across platforms)
expect class PlatformRenderer() : Renderer

// Android
actual class PlatformRenderer actual constructor() : Renderer {
    override fun render(ui: AvaUI) {
        // Use ComposeRenderer
    }
}

// iOS
actual class PlatformRenderer actual constructor() : Renderer {
    override fun render(ui: AvaUI) {
        // Use SwiftUIRenderer
    }
}

// Usage (common code)
val ui = AvaUI { /* ... */ }
val renderer = PlatformRenderer()
ui.render(renderer)
```

---

## 7. IPC Communication Protocols

### 7.1 Android Intent-Based Injection

#### Sending App (External):

```kotlin
// Create AvaCode
val magicCode = """
    AvaUI {
        Column {
            Text("Injected from MyApp!")
            Button("Back to MyApp") { onClick = "returnToSender()" }
        }
    }
"""

// Send via broadcast
val intent = Intent("com.augmentalis.avanuelaunch.INJECT_UI")
intent.putExtra("panel", "topApps")
intent.putExtra("magicCode", magicCode)
intent.putExtra("format", "dsl")  // or "yaml"
intent.putExtra("senderId", "com.example.myapp")
sendBroadcast(intent)
```

#### Receiving App (AvanueLaunch):

```kotlin
class AvaCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val panel = intent.getStringExtra("panel") ?: return
        val magicCode = intent.getStringExtra("magicCode") ?: return
        val format = intent.getStringExtra("format") ?: "dsl"
        val senderId = intent.getStringExtra("senderId")

        // Security check
        if (!isAuthorizedSender(senderId)) {
            Log.w("AvaCode", "Unauthorized sender: $senderId")
            return
        }

        // Parse and inject
        val parser = when (format) {
            "yaml" -> YAMLParser()
            else -> AvaCodeParser()
        }

        val ui = parser.parse(magicCode)
        injectToPanel(panel, ui)
    }
}
```

### 7.2 AIDL Service (Advanced)

#### AIDL Interface:

```aidl
// IAvaCodeInjector.aidl
package com.augmentalis.avanuelaunch;

interface IAvaCodeInjector {
    boolean injectUI(String panel, String magicCode, String format);
    boolean clearPanel(String panel);
    List<String> getAvailablePanels();
}
```

#### Service Implementation:

```kotlin
class AvaCodeService : Service() {
    private val binder = object : IAvaCodeInjector.Stub() {
        override fun injectUI(panel: String, magicCode: String, format: String): Boolean {
            // Validate caller
            val callingUid = Binder.getCallingUid()
            if (!isAuthorized(callingUid)) return false

            // Parse and inject
            val parser = if (format == "yaml") YAMLParser() else AvaCodeParser()
            val ui = parser.parse(magicCode)

            return injectToPanel(panel, ui)
        }

        override fun clearPanel(panel: String): Boolean {
            return clearPanelContent(panel)
        }

        override fun getAvailablePanels(): List<String> {
            return listOf("search", "topApps", "pinnedApps", "continueSessions")
        }
    }

    override fun onBind(intent: Intent): IBinder = binder
}
```

#### Client Usage:

```kotlin
// Bind to service
val serviceConnection = object : ServiceConnection {
    override fun onServiceConnected(name: ComponentName, service: IBinder) {
        val injector = IAvaCodeInjector.Stub.asInterface(service)

        val magicCode = "AvaUI { Text(\"Hello!\") }"
        val success = injector.injectUI("topApps", magicCode, "dsl")
    }

    override fun onServiceDisconnected(name: ComponentName) {}
}

bindService(
    Intent("com.augmentalis.avanuelaunch.MAGIC_CODE_SERVICE"),
    serviceConnection,
    Context.BIND_AUTO_CREATE
)
```

### 7.3 Content Provider

#### URI Structure:

```
content://com.augmentalis.avanuelaunch/panels/{panelId}/ui
```

#### Provider Implementation:

```kotlin
class AvaCodeProvider : ContentProvider() {
    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        val panelId = uri.lastPathSegment ?: return null
        val magicCode = values?.getAsString("magicCode") ?: return null
        val format = values?.getAsString("format") ?: "dsl"

        // Parse and inject
        val parser = if (format == "yaml") YAMLParser() else AvaCodeParser()
        val ui = parser.parse(magicCode)
        injectToPanel(panelId, ui)

        return uri
    }
}
```

#### Client Usage:

```kotlin
val values = ContentValues().apply {
    putString("magicCode", """AvaUI { Text("Hello!") }""")
    putString("format", "dsl")
}

contentResolver.insert(
    Uri.parse("content://com.augmentalis.avanuelaunch/panels/topApps/ui"),
    values
)
```

### 7.4 WebSocket (Web/Desktop)

#### Server (AvanueLaunch):

```kotlin
val server = WebSocketServer(8080)

server.onMessage { message ->
    val json = JSONObject(message)
    val panel = json.getString("panel")
    val magicCode = json.getString("magicCode")
    val format = json.getString("format")

    val parser = if (format == "yaml") YAMLParser() else AvaCodeParser()
    val ui = parser.parse(magicCode)
    injectToPanel(panel, ui)
}
```

#### Client (External App):

```javascript
const ws = new WebSocket("ws://localhost:8080");

ws.send(JSON.stringify({
    panel: "topApps",
    magicCode: "AvaUI { Text('From Web!') }",
    format: "dsl"
}));
```

---

## 8. Security & Sandboxing

### 8.1 Permission Model

AvaCode permissions are determined by a **trust hierarchy** based on the source app's signing certificate and package namespace.

#### Trust Levels

| Trust Level | Package Namespace | Permission Inheritance | User Prompts | Certification |
|-------------|-------------------|------------------------|--------------|---------------|
| **Trusted (First-Party)** | `com.augmentalis.*` | ✅ Full inheritance from host app | ❌ No prompts | N/A - Internal |
| **Trusted (OEM Partner)** | `com.intelligentdevices.*` | ✅ Full inheritance from host app | ❌ No prompts | N/A - Partnership |
| **Certified Developer** | Any package | ✅ Granted permissions from license | ⚠️ One-time approval | ✅ Required |
| **Third-Party** | All other packages | ❌ Sandboxed, restricted | ✅ Per-use prompts | ❌ Not certified |

#### Permission Matrix

| Permission | Trusted Apps | Certified Developers | Third-Party Apps | Description |
|------------|--------------|---------------------|------------------|-------------|
| `READ_STATE` | ✅ Inherited | ✅ Granted by license | ⚠️ Prompt required | Read panel state |
| `WRITE_STATE` | ✅ Inherited | ✅ Granted by license | ⚠️ Prompt required | Modify panel state |
| `NETWORK_ACCESS` | ✅ Inherited | ✅ If licensed | ❌ Denied | Make network requests |
| `FILE_ACCESS` | ✅ Inherited | ✅ If licensed | ❌ Denied | Access local files |
| `CAMERA` | ✅ Inherited | ✅ If licensed | ❌ Denied | Use camera |
| `LOCATION` | ✅ Inherited | ✅ If licensed | ❌ Denied | Access location |
| `MICROPHONE` | ✅ Inherited | ✅ If licensed | ❌ Denied | Record audio |
| `CONTACTS` | ✅ Inherited | ✅ If licensed | ❌ Denied | Access contacts |
| `BROADCAST` | ✅ Inherited | ✅ If licensed | ⚠️ Prompt required | Send broadcasts |
| `CUSTOM_FUNCTIONS` | ✅ Inherited | ✅ If licensed | ⚠️ Prompt required | Call custom functions |

#### Developer Certification Program

Third-party developers can apply for **Certified Developer** status through an app review process. Certified apps receive extended permissions without per-use prompts.

##### Certification Process

1. **Developer Registration**
   - Register at `https://developer.augmentalis.com`
   - Submit app for review
   - Provide privacy policy, terms of service
   - Describe required permissions and use cases

2. **App Review**
   - Manual code review by Augmentalis security team
   - Automated security scanning
   - Privacy compliance check
   - User experience evaluation
   - Review timeline: 3-5 business days

3. **License Issuance**
   - Upon approval, developer receives a **Developer License Key**
   - License is cryptographically signed by Augmentalis
   - License specifies granted permissions
   - Valid for 1 year (renewable)

4. **License Integration**
   - Developer adds `augmentalis_license.json` to app's `assets/` folder
   - License is validated at runtime
   - Permissions are granted based on license scope

##### License File Format

```json
{
  "version": "1.0",
  "licenseId": "DEV-2025-ABC123-XYZ789",
  "issuedTo": {
    "developerName": "Acme Corporation",
    "developerId": "dev_12345",
    "email": "developer@acme.com",
    "packageName": "com.acme.weatherapp"
  },
  "issuedBy": "Augmentalis",
  "issuedDate": "2025-11-01T00:00:00Z",
  "expiryDate": "2026-11-01T00:00:00Z",
  "grantedPermissions": [
    "LOCATION",
    "NETWORK_ACCESS",
    "READ_STATE",
    "WRITE_STATE"
  ],
  "tier": "STANDARD",  // FREE, STANDARD, PREMIUM, ENTERPRISE
  "revoked": false,
  "signature": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8A..."  // RSA signature
}
```

##### License Tiers

| Tier | Cost | Permissions | Support | Review Priority |
|------|------|-------------|---------|----------------|
| **FREE** | $0 | Basic (READ_STATE, WRITE_STATE) | Community | Standard (5 days) |
| **STANDARD** | $99/year | + LOCATION, NETWORK_ACCESS | Email | Fast (3 days) |
| **PREMIUM** | $299/year | + CAMERA, MICROPHONE, STORAGE | Priority email | Priority (24h) |
| **ENTERPRISE** | Custom | All permissions + custom APIs | Dedicated support | Immediate |

##### License Validation

```kotlin
enum class TrustLevel {
    TRUSTED_FIRST_PARTY,   // Augmentalis apps
    TRUSTED_OEM_PARTNER,   // Intelligent Devices apps
    CERTIFIED_DEVELOPER,   // Licensed third-party developers
    THIRD_PARTY            // Unlicensed third-party apps
}

fun getTrustLevel(packageName: String, context: Context): TrustLevel {
    // Check package namespace first
    return when {
        packageName.startsWith("com.augmentalis.") -> {
            // Verify signing certificate matches Augmentalis
            if (verifySignature(packageName, AUGMENTALIS_CERT_SHA256)) {
                TrustLevel.TRUSTED_FIRST_PARTY
            } else {
                TrustLevel.THIRD_PARTY  // Spoofed package name
            }
        }

        packageName.startsWith("com.intelligentdevices.") -> {
            // Verify signing certificate matches Intelligent Devices
            if (verifySignature(packageName, INTELLIGENT_DEVICES_CERT_SHA256)) {
                TrustLevel.TRUSTED_OEM_PARTNER
            } else {
                TrustLevel.THIRD_PARTY  // Spoofed package name
            }
        }

        else -> {
            // Check for developer license
            val license = loadDeveloperLicense(packageName, context)
            if (license != null && validateLicense(license, packageName, context)) {
                TrustLevel.CERTIFIED_DEVELOPER
            } else {
                TrustLevel.THIRD_PARTY
            }
        }
    }
}

private fun loadDeveloperLicense(packageName: String, context: Context): DeveloperLicense? {
    return try {
        // Get the external app's assets
        val externalContext = context.createPackageContext(
            packageName,
            Context.CONTEXT_IGNORE_SECURITY
        )

        // Read license file from assets
        val inputStream = externalContext.assets.open("augmentalis_license.json")
        val licenseJson = inputStream.bufferedReader().use { it.readText() }

        // Parse license
        Json.decodeFromString<DeveloperLicense>(licenseJson)
    } catch (e: Exception) {
        Log.w("AvaCode", "No valid license found for $packageName: ${e.message}")
        null
    }
}

private fun validateLicense(
    license: DeveloperLicense,
    packageName: String,
    context: Context
): Boolean {
    // 1. Check package name matches
    if (license.issuedTo.packageName != packageName) {
        Log.w("AvaCode", "License package mismatch: ${license.issuedTo.packageName} != $packageName")
        return false
    }

    // 2. Check expiry date
    val now = Instant.now()
    val expiryDate = Instant.parse(license.expiryDate)
    if (now > expiryDate) {
        Log.w("AvaCode", "License expired: ${license.expiryDate}")
        return false
    }

    // 3. Check revocation status
    if (license.revoked) {
        Log.w("AvaCode", "License revoked: ${license.licenseId}")
        return false
    }

    // 4. Verify cryptographic signature
    if (!verifyLicenseSignature(license)) {
        Log.w("AvaCode", "Invalid license signature: ${license.licenseId}")
        return false
    }

    // 5. Check online revocation list (cached, updated daily)
    if (isLicenseRevokedOnline(license.licenseId)) {
        Log.w("AvaCode", "License revoked online: ${license.licenseId}")
        return false
    }

    return true
}

private fun verifyLicenseSignature(license: DeveloperLicense): Boolean {
    try {
        // Create payload (all fields except signature)
        val payload = buildString {
            append(license.licenseId)
            append(license.issuedTo.packageName)
            append(license.issuedDate)
            append(license.expiryDate)
            append(license.grantedPermissions.sorted().joinToString(","))
        }

        // Verify RSA signature using Augmentalis public key
        val publicKey = getAugmentalisPublicKey()
        val signature = Base64.decode(license.signature, Base64.DEFAULT)

        val verifier = Signature.getInstance("SHA256withRSA")
        verifier.initVerify(publicKey)
        verifier.update(payload.toByteArray())

        return verifier.verify(signature)
    } catch (e: Exception) {
        Log.e("AvaCode", "License signature verification failed", e)
        return false
    }
}

data class DeveloperLicense(
    val version: String,
    val licenseId: String,
    val issuedTo: IssuedTo,
    val issuedBy: String,
    val issuedDate: String,
    val expiryDate: String,
    val grantedPermissions: List<String>,
    val tier: String,
    val revoked: Boolean,
    val signature: String
) {
    data class IssuedTo(
        val developerName: String,
        val developerId: String,
        val email: String,
        val packageName: String
    )
}

private fun verifySignature(packageName: String, expectedCertSha256: String): Boolean {
    val packageInfo = packageManager.getPackageInfo(
        packageName,
        PackageManager.GET_SIGNING_CERTIFICATES
    )

    val signatures = packageInfo.signingInfo.apkContentsSigners
    return signatures.any { signature ->
        val certSha256 = MessageDigest.getInstance("SHA-256")
            .digest(signature.toByteArray())
            .toHexString()
        certSha256 == expectedCertSha256
    }
}
```

#### Permission Inheritance Example

**Scenario 1: AIAvanue (Trusted) injects UI into AvanueLaunch**
```kotlin
// AIAvanue package: com.augmentalis.avanue.ai
// Trust Level: TRUSTED_FIRST_PARTY
// Result: All permissions inherited from AvanueLaunch

val magicCode = """
    AvaUI {
        Button("Scan Document") {
            onClick = "capturePhoto()"  // ✅ Allowed - Camera inherited
        }
    }
"""
// No user prompt - permission inherited from host app
```

**Scenario 2: Third-Party App injects UI**
```kotlin
// External app package: com.example.weatherapp
// Trust Level: THIRD_PARTY
// Result: Must request permissions

val magicCode = """
    AvaUI {
        Button("Get Location") {
            onClick = "getCurrentLocation()"  // ❌ Denied - Must prompt
        }
    }
"""
// User sees: "WeatherApp wants to access your location through AvanueLaunch"
```

#### Permission Enforcement in IPC Layer

The IPC receiver checks trust level before processing:

```kotlin
class AvaCodeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val senderId = intent.getStringExtra("senderId") ?: return
        val magicCode = intent.getStringExtra("magicCode") ?: return
        val panel = intent.getStringExtra("panel") ?: return

        // Determine trust level
        val trustLevel = getTrustLevel(senderId, context)

        // Create security context based on trust
        val securityContext = when (trustLevel) {
            TrustLevel.TRUSTED_FIRST_PARTY,
            TrustLevel.TRUSTED_OEM_PARTNER -> {
                // Inherit all permissions from host app
                SecurityContext(
                    trustLevel = trustLevel,
                    inheritedPermissions = getHostAppPermissions(context),
                    requireUserPrompt = false
                )
            }

            TrustLevel.THIRD_PARTY -> {
                // Restricted sandbox
                SecurityContext(
                    trustLevel = trustLevel,
                    inheritedPermissions = emptySet(),
                    requireUserPrompt = true,
                    allowedComponents = SAFE_COMPONENTS_WHITELIST,
                    allowedFunctions = SAFE_FUNCTIONS_WHITELIST
                )
            }
        }

        // Parse and validate with security context
        val parser = AvaCodeParser(securityContext)
        val ui = parser.parse(magicCode)

        // Inject with appropriate permissions
        injectToPanel(panel, ui, securityContext)
    }

    private fun getHostAppPermissions(context: Context): Set<String> {
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )
        return packageInfo.requestedPermissions?.toSet() ?: emptySet()
    }
}

data class SecurityContext(
    val trustLevel: TrustLevel,
    val inheritedPermissions: Set<String>,
    val requireUserPrompt: Boolean,
    val allowedComponents: Set<String> = ALL_COMPONENTS,
    val allowedFunctions: Set<String> = ALL_FUNCTIONS
)

// Component whitelists
private val ALL_COMPONENTS = setOf(
    "Column", "Row", "Box", "Text", "Button", "Icon", "Image",
    "Card", "Dialog", "TextField", "Checkbox", "Switch", etc.
)

private val SAFE_COMPONENTS_WHITELIST = setOf(
    "Column", "Row", "Box", "Text", "Button", "Icon"  // Basic display only
)

private val ALL_FUNCTIONS = setOf(
    "setState", "showToast", "navigate", "capturePhoto", "getLocation",
    "recordAudio", "sendBroadcast", "readFile", "writeFile", etc.
)

private val SAFE_FUNCTIONS_WHITELIST = setOf(
    "setState", "showToast", "navigate"  // No dangerous operations
)
```

#### Certificate Storage

Trusted certificates are hardcoded in the app (can't be spoofed):

```kotlin
object TrustedCertificates {
    // Augmentalis signing certificate (SHA-256)
    const val AUGMENTALIS_CERT_SHA256 =
        "A1B2C3D4E5F6... (actual cert hash)"

    // Intelligent Devices OEM signing certificate (SHA-256)
    const val INTELLIGENT_DEVICES_CERT_SHA256 =
        "F6E5D4C3B2A1... (actual cert hash)"

    // Revocation list (apps that lost trust)
    val REVOKED_PACKAGES = setOf(
        "com.augmentalis.oldapp"  // Example: deprecated app
    )
}
```

### 8.2 Validation Rules

Before rendering, AvaCode is validated:

1. **Syntax Validation**: Parse DSL/YAML syntax
2. **Component Whitelist**: Only allowed components
3. **Property Validation**: Type-check all properties
4. **Function Whitelist**: Only allowed function calls
5. **Resource Limits**: Max component count, nesting depth
6. **Content Policy**: No malicious URLs, scripts

Example validator:

```kotlin
class AvaCodeValidator {
    fun validate(magicCode: String): ValidationResult {
        // 1. Parse syntax
        val ast = parseToAST(magicCode)

        // 2. Check component whitelist
        val allowedComponents = setOf("Column", "Row", "Text", "Button", "Icon")
        ast.components.forEach { component ->
            if (component.type !in allowedComponents) {
                return ValidationResult.Error("Component '${component.type}' not allowed")
            }
        }

        // 3. Check function whitelist
        val allowedFunctions = setOf("setState", "showToast", "navigate")
        ast.functions.forEach { func ->
            if (func.name !in allowedFunctions) {
                return ValidationResult.Error("Function '${func.name}' not allowed")
            }
        }

        // 4. Resource limits
        if (ast.componentCount > 100) {
            return ValidationResult.Error("Too many components (max 100)")
        }

        if (ast.maxNestingDepth > 10) {
            return ValidationResult.Error("Nesting too deep (max 10 levels)")
        }

        return ValidationResult.Success
    }
}
```

### 8.3 Sandboxed Execution

```kotlin
class SandboxedExecutor {
    private val allowedFunctions = mapOf<String, (Map<String, Any?>) -> Any?>(
        "showToast" to { params ->
            val message = params["message"] as? String ?: return@to null
            // Show toast safely
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        },

        "setState" to { params ->
            val key = params["key"] as? String ?: return@to null
            val value = params["value"]
            // Update state safely
            state[key] = value
        },

        "navigate" to { params ->
            val route = params["route"] as? String ?: return@to null
            // Navigate safely (validate route first)
            if (route.startsWith("/") && route.length < 100) {
                navController.navigate(route)
            }
        }
    )

    fun executeFunction(name: String, params: Map<String, Any?>): Any? {
        val func = allowedFunctions[name]
            ?: throw SecurityException("Function '$name' not allowed")

        return func(params)
    }
}
```

### 8.4 Content Security Policy

```kotlin
data class ContentSecurityPolicy(
    val allowedImageSources: List<String> = listOf("https://"),
    val allowedScriptSources: List<String> = emptyList(),  // No scripts
    val maxImageSize: Long = 5 * 1024 * 1024,  // 5MB
    val allowedDomains: List<String> = emptyList()
)

fun validateImageSource(url: String, policy: ContentSecurityPolicy): Boolean {
    // Check protocol
    val isHttps = url.startsWith("https://")
    if (!isHttps && "https://" in policy.allowedImageSources) {
        return false
    }

    // Check domain whitelist
    if (policy.allowedDomains.isNotEmpty()) {
        val domain = extractDomain(url)
        if (domain !in policy.allowedDomains) {
            return false
        }
    }

    return true
}
```

---

## 9. Examples & Best Practices

### 9.1 Complete Example: Custom Weather Widget

#### AvaCode DSL:

```kotlin
AvaUI {
    theme = "GlassAvanue.Light"

    state {
        var temperature by remember { 72 }
        var condition by remember { "Sunny" }
        var loading by remember { false }
    }

    Card(
        cornerRadius = 24,
        glassOpacity = 0.75,
        elevation = 8
    ) {
        Column(padding = 16, spacing = 12) {
            Row(spacing = 8, alignment = "center") {
                Icon("wb_sunny", size = 32, color = "#FFB300")
                Text("$temperature°F", style = "displayMedium")
            }

            Text(condition, style = "titleMedium", color = "#46CBFF")

            Button("Refresh") {
                onClick = "refreshWeather()"
                enabled = !loading
            }
        }
    }
}
```

#### YAML Equivalent:

```yaml
avaui:
  version: "1.0"
  theme: "GlassAvanue.Light"

  state:
    temperature: 72
    condition: "Sunny"
    loading: false

  root:
    type: "Card"
    cornerRadius: 24
    glassOpacity: 0.75
    elevation: 8
    children:
      - type: "Column"
        padding: 16
        spacing: 12
        children:
          - type: "Row"
            spacing: 8
            alignment: "center"
            children:
              - type: "Icon"
                name: "wb_sunny"
                size: 32
                color: "#FFB300"

              - type: "Text"
                content: "${state.temperature}°F"
                style: "displayMedium"

          - type: "Text"
            content: "${state.condition}"
            style: "titleMedium"
            color: "#46CBFF"

          - type: "Button"
            label: "Refresh"
            enabled: "${!state.loading}"
            onClick:
              action: "custom"
              function: "refreshWeather"
```

### 9.2 Best Practices

#### ✅ DO:

1. **Keep It Simple**: Limit component count (< 50 per injection)
2. **Use State**: Leverage state for dynamic updates
3. **Validate Input**: Always validate user input before setState
4. **Error Handling**: Provide fallback UI for errors
5. **Test Thoroughly**: Test on multiple screen sizes
6. **Optimize Images**: Use appropriately sized images
7. **Follow Theme**: Use GlassAvanue design tokens
8. **Document Intent**: Add comments explaining complex logic

#### ❌ DON'T:

1. **Nested Too Deep**: Max 10 levels of nesting
2. **Inline Complex Logic**: Extract to custom functions
3. **Hardcode Values**: Use theme colors and spacing
4. **Ignore Permissions**: Request needed permissions
5. **Block UI Thread**: Use async for heavy operations
6. **Expose Secrets**: Never include API keys in AvaCode
7. **Assume Platform**: Write platform-agnostic code
8. **Skip Validation**: Always validate before rendering

### 9.3 Performance Tips

1. **Lazy Loading**: Use LazyColumn/LazyGrid for long lists
2. **Memoization**: Use `remember` for computed values
3. **Image Caching**: Pre-load and cache images
4. **Minimize Recomposition**: Avoid unnecessary state changes
5. **Batch Updates**: Group multiple setState calls
6. **Virtualization**: Only render visible items

```kotlin
// Good: Lazy loading with virtualization
LazyColumn {
    items(state.bigList) { item ->
        ItemView(item)
    }
}

// Bad: Eager rendering all items
Column {
    state.bigList.forEach { item ->
        ItemView(item)  // All rendered upfront!
    }
}
```

---

## 10. API Reference

### 10.1 AvaCodeParser

```kotlin
class AvaCodeParser {
    /**
     * Parse AvaCode DSL string to AvaUI
     *
     * @param magicCode DSL string
     * @return Parsed AvaUI instance
     * @throws ParseException if syntax is invalid
     */
    fun parse(magicCode: String): AvaUI

    /**
     * Validate AvaCode without rendering
     *
     * @param magicCode DSL string
     * @return Validation result
     */
    fun validate(magicCode: String): ValidationResult
}
```

### 10.2 YAMLParser

```kotlin
class YAMLParser {
    /**
     * Parse YAML document to AvaUI
     *
     * @param yaml YAML string
     * @return Parsed AvaUI instance
     * @throws ParseException if YAML is invalid
     */
    fun parse(yaml: String): AvaUI

    /**
     * Validate YAML schema
     *
     * @param yaml YAML string
     * @return Validation result
     */
    fun validate(yaml: String): ValidationResult
}
```

### 10.3 AvaCodeContainer

```kotlin
@Composable
fun AvaCodeContainer(
    modifier: Modifier = Modifier,
    magicCode: String? = null,
    format: String = "dsl",  // "dsl" or "yaml"
    fallback: @Composable BoxScope.() -> Unit
) {
    // Renders AvaCode if provided, otherwise shows fallback
}
```

### 10.4 SecuritySandbox

```kotlin
class SecuritySandbox {
    /**
     * Check if sender is authorized
     *
     * @param senderId Package name or UID
     * @return true if authorized
     */
    fun isAuthorized(senderId: String): Boolean

    /**
     * Execute function in sandbox
     *
     * @param functionName Function to execute
     * @param params Function parameters
     * @return Function result
     * @throws SecurityException if not allowed
     */
    fun executeFunction(functionName: String, params: Map<String, Any?>): Any?

    /**
     * Validate content against security policy
     *
     * @param magicCode Code to validate
     * @return Validation result
     */
    fun validateContent(magicCode: String): ValidationResult
}
```

---

## Appendix A: Component Reference

See [MAGICUI-COMPONENT-REFERENCE.md](MAGICUI-COMPONENT-REFERENCE.md) for complete component documentation.

## Appendix B: Theme Reference

See [GLASSAVANUE-THEME-SPEC-251031-1633.md](GLASSAVANUE-THEME-SPEC-251031-1633.md) for GlassAvanue theme documentation.

## Appendix C: IPC Protocol Specification

See [IPC-PROTOCOL-SPEC.md](IPC-PROTOCOL-SPEC.md) for detailed IPC protocol documentation.

---

**Document Version**: 1.0.0
**Last Updated**: 2025-10-31 21:16 PDT
**Created by**: Manoj Jhawar, manoj@ideahq.net
**Methodology**: IDEACODE 5.0
