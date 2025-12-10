# Chapter 57: Web Action Scraping System

**Status:** IMPLEMENTED
**Last Updated:** 2025-12-01
**Module:** WebAvanue (KMP), Universal/AVA/Features/Actions
**Purpose:** Voice control of web pages via automatic element extraction

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [KMP Structure](#kmp-structure)
4. [JavaScript Library](#javascript-library)
5. [WebActionExtractor](#webactionextractor)
6. [WebVoiceCommandIntegration](#webvoicecommandintegration)
7. [BrowserActionHandler](#browseractionhandler)
8. [Platform Utilities](#platform-utilities)
9. [Voice Commands](#voice-commands)
10. [Usage Examples](#usage-examples)
11. [References](#references)

---

## Overview

The Web Action Scraping System enables voice control of any web page by automatically extracting interactive elements (buttons, links, inputs) and converting them to voice commands.

### Key Features

| Feature | Description |
|---------|-------------|
| **Automatic extraction** | JavaScript scans page for clickable elements |
| **Voice-friendly mapping** | Elements converted to natural voice commands |
| **Fuzzy matching** | "click submit button" matches "submit" element |
| **KMP shared code** | Business logic shared across Android/iOS |
| **WebAvanue integration** | Works with WebAvanue custom browser |

### User Experience

```
User: "What can I click?"
AVA: "3 buttons including submit, cancel, login. 5 links including home, about..."

User: "Click submit"
AVA: "Clicking submit button"
[Button is clicked]

User: "Type hello world"
AVA: "Typed: hello world"
```

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Voice Input                                  │
│                   "Click submit"                                 │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    AVA NLU                                       │
│              Intent: browser.click                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                BrowserActionHandler (Android)                    │
│              - Parse voice command                               │
│              - Route to WebAvanue                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│           WebVoiceCommandIntegration (KMP commonMain)            │
│              - Find matching element                             │
│              - Fuzzy match scoring                               │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│              WebActionExtractor (KMP commonMain)                 │
│              - Inject webactions.js                              │
│              - Parse extraction results                          │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  WebView (Platform)                              │
│              - evaluateJavaScript()                              │
│              - Execute click/type                                │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                webactions.js (JavaScript)                        │
│              - Extract buttons, links, inputs                    │
│              - Generate voice commands                           │
│              - Execute actions                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## KMP Structure

### File Organization

```
WebAvanue/universal/src/
├── commonMain/kotlin/com/augmentalis/Avanues/web/universal/
│   ├── actions/
│   │   ├── WebAction.kt              # Data models (KMP)
│   │   ├── WebActionExtractor.kt     # Extraction logic (KMP)
│   │   └── WebVoiceCommandIntegration.kt  # Voice mapping (KMP)
│   └── platform/
│       └── Platform.kt               # expect declarations
├── commonMain/resources/
│   └── webactions.js                 # JS extraction library
├── androidMain/kotlin/.../platform/
│   └── Platform.android.kt           # actual implementations
└── iosMain/kotlin/.../platform/
    └── Platform.ios.kt               # actual implementations

AVA/Universal/AVA/Features/Actions/src/main/kotlin/
└── handlers/
    └── BrowserActionHandler.kt       # Android action handler
```

### Shared vs Platform-Specific

| Component | Location | Platform |
|-----------|----------|----------|
| WebAction data models | commonMain | KMP Shared |
| WebActionExtractor | commonMain | KMP Shared |
| WebVoiceCommandIntegration | commonMain | KMP Shared |
| Platform.kt (expect) | commonMain | KMP Shared |
| Platform.android.kt | androidMain | Android |
| Platform.ios.kt | iosMain | iOS |
| BrowserActionHandler | AVA/Actions | Android |
| webactions.js | resources | JavaScript |

---

## JavaScript Library

### webactions.js

**Location:** `WebAvanue/universal/src/commonMain/resources/webactions.js`

### API Methods

| Method | Description |
|--------|-------------|
| `extractAll()` | Extract all actionable elements |
| `getVoiceCommands()` | Get simplified voice command list |
| `clickByCommand(cmd)` | Click element matching command |
| `clickAt(x, y)` | Click at coordinates |
| `typeText(text, selector)` | Type into input field |
| `getElementAt(x, y)` | Get element info at coordinates |

### Element Extraction

```javascript
// Extracted element types
- button, input[type="button"], [role="button"], .btn
- a[href] (links)
- input, textarea, select (form elements)
- [role="menuitem"], [role="tab"] (navigation)
- video, audio controls (media)
```

### Voice Command Generation

```javascript
// Label extraction priority:
1. aria-label
2. title attribute
3. innerText
4. placeholder
5. Associated <label>
6. name attribute
7. Class-based inference (btn-submit -> "submit")
```

---

## WebActionExtractor

**Location:** `WebAvanue/universal/src/commonMain/kotlin/.../actions/WebActionExtractor.kt`

### Class Definition

```kotlin
class WebActionExtractor(
    private val webView: WebView
) {
    suspend fun extractAll(): WebActionsResult?

    suspend fun extractVoiceCommands(): List<VoiceCommand>

    suspend fun clickByCommand(command: String): ClickResult

    suspend fun clickAt(x: Int, y: Int): ClickResult

    suspend fun typeText(text: String, selector: String? = null): ClickResult

    suspend fun findBestMatch(query: String): VoiceCommand?

    suspend fun executeVoiceCommand(query: String): Pair<VoiceCommand?, ClickResult>

    fun reset()  // Call after page navigation
}
```

### Data Models

```kotlin
data class WebAction(
    val type: String,           // "button", "link", "input"
    val label: String?,         // Human-readable label
    val voiceCommand: String,   // Generated voice command
    val selector: String,       // CSS selector
    val coordinates: ElementCoordinates,
    val attributes: Map<String, String>
)

data class VoiceCommand(
    val command: String,        // "submit", "login"
    val type: String,
    val label: String?,
    val selector: String,
    val coordinates: ElementCoordinates
)

data class ClickResult(
    val success: Boolean,
    val error: String? = null,
    val command: String? = null,
    val label: String? = null
)
```

---

## WebVoiceCommandIntegration

**Location:** `WebAvanue/universal/src/commonMain/kotlin/.../actions/WebVoiceCommandIntegration.kt`

### Features

| Feature | Description |
|---------|-------------|
| Fuzzy matching | "click submit button" -> "submit" |
| Synonym support | "press" = "click" = "tap" |
| Score-based ranking | Best match wins |
| TTS feedback | "Clicking submit button" |

### Matching Score Algorithm

```kotlin
fun matchScore(query: String): Int = when {
    command == normalizedQuery -> 100
    variants.any { it == normalizedQuery } -> 95
    label == normalizedQuery -> 90
    command.startsWith(normalizedQuery) -> 80
    command.contains(normalizedQuery) -> 60
    else -> 0
}
```

### Action Synonyms

```kotlin
val ACTION_SYNONYMS = mapOf(
    "click" to listOf("press", "tap", "select", "choose"),
    "open" to listOf("go to", "navigate to", "visit"),
    "enter" to listOf("type", "input", "fill in"),
    "submit" to listOf("send", "confirm", "done"),
    "search" to listOf("find", "look for"),
    "play" to listOf("start", "begin"),
    "pause" to listOf("stop", "hold")
)
```

---

## BrowserActionHandler

**Location:** `AVA/Universal/AVA/Features/Actions/src/main/kotlin/.../handlers/BrowserActionHandler.kt`

### Supported Intents

| Intent | Example Commands |
|--------|------------------|
| `browser.click` | "click submit", "press login" |
| `browser.scroll` | "scroll down", "scroll to top" |
| `browser.navigate` | "go back", "refresh" |
| `browser.type` | "type hello world" |
| `browser.list_actions` | "what can I click" |

### Interface for Browser Control

```kotlin
interface BrowserControllerInterface {
    suspend fun clickByCommand(command: String): Boolean
    suspend fun typeText(text: String, selector: String?): Boolean
    suspend fun scrollUp(): Boolean
    suspend fun scrollDown(): Boolean
    suspend fun scrollToTop(): Boolean
    suspend fun scrollToBottom(): Boolean
    suspend fun goBack(): Boolean
    suspend fun goForward(): Boolean
    suspend fun refresh(): Boolean
    suspend fun getAvailableCommands(): List<String>
    fun getCurrentUrl(): String
    fun getCurrentTitle(): String
}
```

---

## Platform Utilities

### expect/actual Pattern

**commonMain - Platform.kt:**
```kotlin
expect fun platformLog(level: LogLevel, tag: String, message: String, throwable: Throwable?)

expect fun loadResourceAsText(resourcePath: String): String?
```

**androidMain - Platform.android.kt:**
```kotlin
actual fun platformLog(...) {
    android.util.Log.d(tag, message)
}

actual fun loadResourceAsText(resourcePath: String): String? {
    return Thread.currentThread().contextClassLoader
        ?.getResourceAsStream(resourcePath)
        ?.bufferedReader()?.readText()
}
```

**iosMain - Platform.ios.kt:**
```kotlin
actual fun platformLog(...) {
    NSLog("[$level/$tag] $message")
}

actual fun loadResourceAsText(resourcePath: String): String? {
    val path = NSBundle.mainBundle.pathForResource(name, ext)
    return NSString.stringWithContentsOfFile(path, NSUTF8StringEncoding, null)
}
```

---

## Voice Commands

### Command Extraction Patterns

| Pattern | Example | Extracted |
|---------|---------|-----------|
| `click\|press\|tap (.+)` | "click submit" | "submit" |
| `type\|enter (.+)` | "type hello" | "hello" |
| `scroll (up\|down\|...)` | "scroll down" | Direction |
| `go back\|forward` | "go back" | Navigation |

### Feedback Generation

```kotlin
fun getFeedback(): String = when (type) {
    BUTTON -> "Clicking ${label ?: command}"
    LINK -> "Opening ${label ?: command}"
    INPUT -> "Focus on ${label ?: command}"
    MEDIA -> "${command}ing"
    MENU -> "Selecting ${label ?: command}"
}
```

---

## Usage Examples

### Extract Page Actions

```kotlin
val extractor = WebActionExtractor(webView)
val commands = extractor.extractVoiceCommands()

commands.forEach { cmd ->
    println("Say '${cmd.command}' to ${cmd.type} ${cmd.label}")
}
// Output:
// Say 'submit' to button Submit Form
// Say 'login' to link Login
// Say 'search' to input Search field
```

### Execute Voice Command

```kotlin
val (matched, result) = extractor.executeVoiceCommand("click submit")

if (result.success) {
    speak("Clicked ${matched?.label ?: "element"}")
} else {
    speak("Could not find submit on this page")
}
```

### List Available Actions

```kotlin
val integration = WebVoiceCommandIntegration()
integration.updateFromVoiceCommands(url, commands)

val summary = integration.getSpokenSummary()
// "3 buttons including submit, cancel, login. 5 links including home, about."
```

---

## References

| Resource | Link |
|----------|------|
| WebAvanue Module | MainAvanues/modules/WebAvanue |
| Chapter 49 | Action Handlers |
| Chapter 36 | VoiceOS Command Delegation |
| WebView KMP Interface | BrowserCoreData/platform/WebView.kt |
