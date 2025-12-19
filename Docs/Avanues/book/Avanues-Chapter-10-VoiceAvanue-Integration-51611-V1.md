# Chapter 10: Avanues Ecosystem Integration

**Version:** 5.3.0
**Date:** 2025-11-02
**Author:** Manoj Jhawar, manoj@ideahq.net
**Word Count:** ~5,000 words

---

## Ecosystem Overview

The Avanues ecosystem consists of:
1. **VoiceOS** - Core accessibility service (FREE)
2. **Avanues** - Platform core (FREE)
3. **AIAvanue** - AI capabilities ($9.99)
4. **BrowserAvanue** - Voice browser ($4.99)
5. **NoteAvanue** - Voice notes (FREE/$2.99 premium)

## Architecture

```
┌──────────────────────────────────────────────────────────┐
│                      VoiceOS                             │
│              (Accessibility Service)                     │
│  - Voice input capture                                   │
│  - System-wide commands                                  │
│  - App discovery                                         │
└─────────┬────────────────────────────────────────────────┘
          │ IPC (Intents/URL Schemes)
          │
┌─────────▼──────────────┬──────────────────────────┬──────▼──────┐
│   Avanues Core     │    AIAvanue              │ BrowserAvanue│
│   - AvaUI runtime    │    - GPT integration     │ - Voice nav  │
│   - Component library  │    - Voice assistant     │ - Page reader│
│   - Theme system       │    - Smart suggestions   │              │
└────────────────────────┴──────────────────────────┴──────────────┘
```

## VoiceOSBridge

**Critical Gap:** Currently EMPTY (only build.gradle.kts exists)

**Required Implementation:**

```kotlin
// avanues/core/voiceosbridge/src/commonMain/kotlin/VoiceOSBridge.kt

interface VoiceOSBridge {
    // Capability discovery
    suspend fun registerCapability(capability: AppCapability)
    suspend fun queryCapabilities(filter: CapabilityFilter): List<AppCapability>

    // Voice command routing
    suspend fun registerVoiceCommand(command: VoiceCommand, handler: CommandHandler)
    suspend fun routeCommand(voiceInput: String): CommandResult

    // Inter-app communication
    suspend fun sendMessage(targetApp: String, message: AppMessage): MessageResult
    suspend fun subscribeToMessages(messageType: String, handler: MessageHandler)

    // State sharing
    suspend fun publishState(key: String, value: Any)
    suspend fun subscribeToState(key: String, observer: StateObserver)
}

data class AppCapability(
    val id: String,
    val name: String,
    val voiceCommands: List<String>,
    val actions: List<String>
)

data class VoiceCommand(
    val trigger: String,
    val action: String,
    val appId: String
)
```

## App Communication Flow

### 1. Capability Registration

```kotlin
// AIAvanue registers capabilities on launch
val bridge = VoiceOSBridge.getInstance()

bridge.registerCapability(AppCapability(
    id = "com.augmentalis.avanue.ai",
    name = "AIAvanue",
    voiceCommands = listOf(
        "ask AI",
        "generate text",
        "summarize document"
    ),
    actions = listOf(
        "ai.query",
        "ai.generate",
        "ai.summarize"
    )
))
```

### 2. Voice Command Routing

```
User: "Ask AI to summarize this page"
      ↓
VoiceOS captures input
      ↓
VoiceOSBridge.routeCommand("ask AI to summarize this page")
      ↓
Matches "ask AI" → AIAvanue
      ↓
AIAvanue receives: ai.summarize { content: <current page> }
      ↓
AIAvanue processes and displays result
```

### 3. Inter-App Communication

```kotlin
// BrowserAvanue → AIAvanue
bridge.sendMessage(
    targetApp = "com.augmentalis.avanue.ai",
    message = AppMessage(
        type = "ai.summarize",
        payload = mapOf(
            "content" to pageContent,
            "language" to "en",
            "length" to "short"
        )
    )
)

// AIAvanue receives message
bridge.subscribeToMessages("ai.summarize") { message ->
    val content = message.payload["content"] as String
    val summary = aiService.summarize(content)

    // Send result back
    bridge.sendMessage(
        targetApp = message.senderId,
        message = AppMessage(
            type = "ai.result",
            payload = mapOf("summary" to summary)
        )
    )
}
```

## AvaUI in Avanues Apps

### Example: AIAvanue Chat Screen

```json
{
  "name": "ChatScreen",
  "stateVariables": [
    { "name": "messages", "type": "List<Message>", "initialValue": [] },
    { "name": "inputText", "type": "String", "initialValue": "" }
  ],
  "root": {
    "type": "COLUMN",
    "children": [
      {
        "type": "APP_BAR",
        "properties": {
          "title": "AIAvanue Chat",
          "showBackButton": true
        }
      },
      {
        "type": "SCROLL_VIEW",
        "properties": { "weight": 1 },
        "children": [
          {
            "type": "COLUMN",
            "children": "{{ messages.map(msg => MessageBubble(msg)) }}"
          }
        ]
      },
      {
        "type": "ROW",
        "children": [
          {
            "type": "TEXT_FIELD",
            "properties": {
              "value": "inputText",
              "placeholder": "Ask me anything...",
              "weight": 1
            }
          },
          {
            "type": "BUTTON",
            "properties": { "text": "Send" },
            "eventHandlers": {
              "onClick": "{ handleSend() }"
            }
          }
        ]
      }
    ]
  }
}
```

## Theme Consistency

All Avanue apps share the VoiceOS theme:

```kotlin
// avanues/core/themebridge/VoiceOSTheme.kt

object VoiceOSTheme {
    val colors = ThemeColors(
        primary = Color(0xFF0066CC),
        secondary = Color(0xFF9C27B0),
        background = Color(0xFFF5F5F5),
        surface = Color(0xFFFFFFFF),
        error = Color(0xFFD32F2F)
    )

    val typography = ThemeTypography(
        h1 = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
        body1 = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal)
    )

    val spacing = ThemeSpacing(
        small = 8.dp,
        medium = 16.dp,
        large = 24.dp
    )
}
```

## State Sharing

Apps can share state through VoiceOSBridge:

```kotlin
// Avanues publishes user preferences
bridge.publishState("user.theme", "dark")
bridge.publishState("user.language", "en")
bridge.publishState("user.textSize", 16)

// AIAvanue subscribes to preferences
bridge.subscribeToState("user.theme") { value ->
    val theme = value as String
    updateTheme(theme)
}
```

## App Discovery

```kotlin
// Query available AI capabilities
val aiApps = bridge.queryCapabilities(
    CapabilityFilter(
        category = "ai",
        actions = listOf("ai.query", "ai.generate")
    )
)

// Display to user
aiApps.forEach { app ->
    println("${app.name}: ${app.voiceCommands.joinToString()}")
}

// Output:
// AIAvanue: ask AI, generate text, summarize document
```

## Integration Example: Voice Browser → AI

**Scenario:** User browses a news article and says "Summarize this page"

```kotlin
// 1. BrowserAvanue captures command
val command = "summarize this page"

// 2. Route through VoiceOSBridge
val result = bridge.routeCommand(command)

// 3. If AIAvanue installed, forward content
if (result.handled) {
    bridge.sendMessage(
        targetApp = "com.augmentalis.avanue.ai",
        message = AppMessage(
            type = "ai.summarize",
            payload = mapOf(
                "content" to currentPageText,
                "url" to currentUrl,
                "title" to currentTitle
            )
        )
    )
}

// 4. AIAvanue processes and responds
bridge.subscribeToMessages("ai.result") { message ->
    val summary = message.payload["summary"] as String
    displaySummary(summary)
}
```

## Platform-Specific IPC

### Android (Intents + AIDL)

```kotlin
// AndroidManifest.xml
<service
    android:name=".VoiceOSBridgeService"
    android:exported="true">
    <intent-filter>
        <action android:name="com.augmentalis.voiceos.BRIDGE" />
    </intent-filter>
</service>

// VoiceOSBridgeService.kt
class VoiceOSBridgeService : Service() {
    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    private val binder = object : IVoiceOSBridge.Stub() {
        override fun routeCommand(command: String): String {
            // Handle command
        }

        override fun sendMessage(targetApp: String, message: String): Boolean {
            // Send message
        }
    }
}
```

### iOS (URL Schemes + XPC)

```swift
// Info.plist
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>voiceos</string>
        </array>
    </dict>
</array>

// AppDelegate.swift
func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
    guard url.scheme == "voiceos" else { return false }

    // Parse URL: voiceos://command?text=summarize%20page
    let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
    let command = components?.queryItems?.first(where: { $0.name == "text" })?.value

    VoiceOSBridge.shared.routeCommand(command ?? "")
    return true
}
```

## Summary

Avanues ecosystem integration requires:
- **VoiceOSBridge** implementation (currently EMPTY)
- Capability discovery and registration
- Voice command routing
- Inter-app communication (IPC)
- State sharing
- Theme consistency

**Next:** Chapter 11 details VoiceOSBridge architecture and implementation plan.

---

**Created by Manoj Jhawar, manoj@ideahq.net**
