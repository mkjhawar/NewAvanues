# VOSWebView - Voice Command WebView

## Overview

VOSWebView is a custom Android WebView with a JavaScript interface that enables voice command execution on web pages. It provides a bridge between voice commands and web page interactions, allowing users to control web content using voice.

## Features

- **JavaScript Interface**: Exposed as `window.VOS` for web-to-native communication
- **Command Execution**: Support for CLICK, FOCUS, SCROLL_TO, FILL_INPUT, SUBMIT, SELECT, CHECK, RADIO
- **XPath Targeting**: Use XPath expressions to target any element on the page
- **Command Discovery**: Automatically discover interactive elements on the page
- **Security**: XPath sanitization, input validation, disabled file access
- **Event Handling**: Callbacks for command execution, errors, and discovery
- **Modern Web Support**: Works with React, Vue, Angular (triggers appropriate events)

## Architecture

```
VOSWebView (WebView)
├── VOSWebInterface (JavaScript Bridge)
│   └── @JavascriptInterface methods
├── WebCommandExecutor (Command Execution)
│   └── JavaScript generation for each command type
├── VOSWebViewClient (Page Navigation)
│   └── Command discovery on page load
└── VOSWebChromeClient (Console & Dialogs)
    └── Console message logging
```

## Files

- **VOSWebView.kt**: Main WebView class with command execution interface
- **VOSWebInterface.kt**: JavaScript interface exposed as window.VOS
- **WebCommandExecutor.kt**: Generates and executes JavaScript for commands
- **VOSWebViewSample.kt**: Comprehensive usage examples

## JavaScript Interface (window.VOS)

The following methods are exposed to JavaScript:

```javascript
// Execute generic command
window.VOS.executeCommand(command, xpath, value)

// Get available commands (JSON array string)
window.VOS.getAvailableCommands()

// Click element
window.VOS.clickElement(xpath)

// Focus element
window.VOS.focusElement(xpath)

// Scroll to element
window.VOS.scrollToElement(xpath)

// Fill input field
window.VOS.fillInput(xpath, value)

// Log event from JavaScript
window.VOS.logEvent(message)

// Report discovered commands
window.VOS.reportDiscoveredCommands(commandsJson)
```

## Helper Object (window.VOSCommands)

A helper object is automatically injected on page load:

```javascript
// Execute command
window.VOSCommands.execute(cmd, xpath, value)

// Get available commands (parsed JSON)
window.VOSCommands.getAvailable()

// Convenience methods
window.VOSCommands.click(xpath)
window.VOSCommands.focus(xpath)
window.VOSCommands.scroll(xpath)
window.VOSCommands.fill(xpath, value)
```

## Supported Commands

### CLICK
Click an element (button, link, etc.)
```kotlin
webView.clickElement("//button[@id='submit']")
```
```javascript
window.VOS.clickElement("//button[@id='submit']")
```

### FOCUS
Focus an input element
```kotlin
webView.focusElement("//input[@name='username']")
```
```javascript
window.VOS.focusElement("//input[@name='username']")
```

### SCROLL_TO
Scroll to an element (smooth scroll, centered)
```kotlin
webView.scrollToElement("//div[@id='content']")
```
```javascript
window.VOS.scrollToElement("//div[@id='content']")
```

### FILL_INPUT
Fill an input field with text
```kotlin
webView.fillInput("//input[@name='username']", "john.doe")
```
```javascript
window.VOS.fillInput("//input[@name='username']", "john.doe")
```

### SUBMIT
Submit a form
```kotlin
webView.executeCommand("SUBMIT", "//form[@id='login-form']")
```

### SELECT
Select an option from dropdown (by value or text)
```kotlin
webView.executeCommand("SELECT", "//select[@name='country']", "USA")
```

### CHECK
Check/uncheck a checkbox
```kotlin
webView.executeCommand("CHECK", "//input[@type='checkbox']", "true")
```

### RADIO
Select a radio button
```kotlin
webView.executeCommand("RADIO", "//input[@type='radio' and @value='option1']")
```

## XPath Examples

### By ID
```xpath
//button[@id='submit']
//input[@id='username']
//div[@id='content']
```

### By Name
```xpath
//input[@name='username']
//select[@name='country']
```

### By Text
```xpath
//button[contains(text(), 'Submit')]
//a[text()='Login']
```

### By Class
```xpath
//button[@class='btn btn-primary']
//div[contains(@class, 'container')]
```

### By Attribute
```xpath
//input[@type='text']
//button[@data-action='submit']
```

### Complex XPath
```xpath
//form[@id='login']//input[@name='username']
//div[@class='content']//button[contains(text(), 'Submit')]
```

## Basic Usage

### 1. Create WebView

```kotlin
val webView = VOSWebView(context)
webView.layoutParams = FrameLayout.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT
)
```

### 2. Set Command Listener

```kotlin
webView.setCommandListener(object : VOSWebView.CommandListener {
    override fun onCommandExecuted(command: String, success: Boolean) {
        Log.d(TAG, "Command $command executed: $success")
    }

    override fun onCommandError(command: String, error: String) {
        Log.e(TAG, "Command $command failed: $error")
    }

    override fun onCommandsDiscovered(commands: List<String>) {
        Log.d(TAG, "Discovered ${commands.size} commands")
    }
})
```

### 3. Load URL

```kotlin
webView.loadUrl("https://example.com")
```

### 4. Execute Commands

```kotlin
// Click submit button
webView.clickElement("//button[@id='submit']")

// Fill username field
webView.fillInput("//input[@name='username']", "john.doe")

// Scroll to content
webView.scrollToElement("//div[@id='content']")
```

## Advanced Usage

### Voice Command Integration

```kotlin
fun onVoiceCommand(webView: VOSWebView, voiceCommand: String) {
    when {
        voiceCommand.startsWith("click") -> {
            val target = extractTarget(voiceCommand)
            val xpath = "//button[contains(text(), '$target')]"
            webView.clickElement(xpath)
        }

        voiceCommand.startsWith("fill") -> {
            val (field, value) = extractFieldAndValue(voiceCommand)
            val xpath = "//input[@name='$field']"
            webView.fillInput(xpath, value)
        }
    }
}
```

### Form Filling

```kotlin
fun fillLoginForm(webView: VOSWebView, username: String, password: String) {
    webView.fillInput("//input[@name='username']", username)

    Handler(Looper.getMainLooper()).postDelayed({
        webView.fillInput("//input[@name='password']", password)
    }, 500)

    Handler(Looper.getMainLooper()).postDelayed({
        webView.clickElement("//button[@type='submit']")
    }, 1000)
}
```

### Command Discovery

```kotlin
webView.setCommandListener(object : VOSWebView.CommandListener {
    override fun onCommandsDiscovered(commands: List<String>) {
        // Build voice command map
        val voiceCommandMap = commands.associate { buttonText ->
            val voiceCommand = normalizeForVoice(buttonText)
            val xpath = "//button[contains(text(), '$buttonText')]"
            voiceCommand to xpath
        }

        // Store for voice recognition
        saveVoiceCommandMap(voiceCommandMap)
    }

    // Other methods...
})
```

## Security Considerations

### 1. XPath Sanitization
- Removes script tags
- Removes event handlers
- Escapes dangerous characters

### 2. Input Validation
- Command type validation
- XPath validation (non-empty)
- Value escaping for JavaScript injection

### 3. WebView Security Settings
```kotlin
settings.apply {
    allowFileAccess = false
    allowContentAccess = false
    safeBrowsingEnabled = true
    mixedContentMode = MIXED_CONTENT_NEVER_ALLOW
}
```

### 4. JavaScript Interface
- All methods annotated with `@JavascriptInterface` (required API 17+)
- Only exposes necessary methods
- Input validation on all parameters

## Event Flow

### Page Load
```
1. User loads URL
2. WebViewClient.onPageFinished() called
3. Command listener JavaScript injected
4. Commands discovered from page
5. onCommandsDiscovered() callback fired
```

### Command Execution
```
1. Kotlin calls executeCommand()
2. WebCommandExecutor generates JavaScript
3. evaluateJavascript() executes code
4. JavaScript returns result
5. onCommandExecuted() or onCommandError() callback fired
```

## Error Handling

### Common Errors

**Element Not Found**
```kotlin
override fun onCommandError(command: String, error: String) {
    if (error.contains("Element not found")) {
        // Try alternative XPath
        val alternativeXPath = "//button[contains(text(), 'Submit')]"
        webView.clickElement(alternativeXPath)
    }
}
```

**Timeout**
```kotlin
override fun onCommandError(command: String, error: String) {
    if (error.contains("timeout")) {
        // Retry after delay
        Handler(Looper.getMainLooper()).postDelayed({
            retryCommand(command)
        }, 1000)
    }
}
```

**JavaScript Error**
```kotlin
override fun onCommandError(command: String, error: String) {
    Log.e(TAG, "JavaScript error: $error")
    // Show user-friendly error message
    showErrorToast("Command failed: $command")
}
```

## Integration with Voice Recognition

### Step 1: Discover Commands
```kotlin
var availableCommands = listOf<String>()

webView.setCommandListener(object : VOSWebView.CommandListener {
    override fun onCommandsDiscovered(commands: List<String>) {
        availableCommands = commands
        updateVoiceRecognitionGrammar(commands)
    }
    // Other methods...
})
```

### Step 2: Map Voice Commands
```kotlin
fun mapVoiceToCommand(voiceInput: String): Pair<String, String>? {
    // Simple mapping - in production use NLP
    val normalized = voiceInput.lowercase()

    return when {
        normalized.startsWith("click") -> {
            val target = extractTarget(normalized)
            val xpath = "//button[contains(text(), '$target')]"
            Pair("CLICK", xpath)
        }
        normalized.startsWith("fill") -> {
            val (field, value) = extractFieldAndValue(normalized)
            val xpath = "//input[@name='$field']"
            Pair("FILL_INPUT", xpath)
        }
        else -> null
    }
}
```

### Step 3: Execute Command
```kotlin
fun executeVoiceCommand(voiceInput: String) {
    val mapped = mapVoiceToCommand(voiceInput)
    if (mapped != null) {
        val (command, xpath) = mapped
        webView.executeCommand(command, xpath)
    } else {
        Log.w(TAG, "Unknown voice command: $voiceInput")
    }
}
```

## Performance Considerations

### 1. Command Discovery
- Runs once per page load
- Lightweight JavaScript execution
- Async callback (doesn't block UI)

### 2. Command Execution
- JavaScript injection is fast (< 100ms typically)
- Smooth scrolling may take longer (animation)
- Form submission may trigger navigation

### 3. Memory Management
- Call `webView.destroy()` in `onDestroy()`
- Remove JavaScript interface on destroy
- Clear references to avoid leaks

## Testing

### Unit Tests
```kotlin
@Test
fun testCommandExecution() {
    val webView = VOSWebView(context)
    val result = webView.clickElement("//button[@id='test']")
    assertTrue(result)
}
```

### Integration Tests
```kotlin
@Test
fun testFormFilling() {
    val webView = VOSWebView(context)
    webView.loadUrl("file:///android_asset/test_form.html")

    // Wait for page load
    Thread.sleep(1000)

    // Fill form
    webView.fillInput("//input[@name='username']", "test")
    webView.fillInput("//input[@name='password']", "test123")
    webView.clickElement("//button[@type='submit']")

    // Verify submission
    // ...
}
```

### Manual Testing
1. Create test HTML page with various elements
2. Load in VOSWebView
3. Execute commands manually
4. Verify results in console and UI

## Troubleshooting

### Command Not Executing
- Check XPath is correct (test in browser console)
- Verify element is visible and interactable
- Check WebView console logs
- Enable JavaScript in settings

### Element Not Found
- Page may not be fully loaded
- Try alternative XPath (by text, class, etc.)
- Check for iframes (XPath doesn't cross iframe boundaries)
- Verify element exists on current page

### JavaScript Errors
- Check console logs
- Verify JavaScript is enabled
- Test JavaScript in browser console first
- Check for XPath escaping issues

## Future Enhancements

1. **AI-Powered Element Detection**: Use ML to find elements by natural language
2. **Voice Command Training**: Learn user's command patterns
3. **Multi-Language Support**: Localized command recognition
4. **Gesture Integration**: Combine voice with touch gestures
5. **Accessibility Features**: Screen reader integration
6. **Command Macros**: Record and replay command sequences
7. **Cloud Sync**: Sync command history across devices

## References

- [Android WebView Documentation](https://developer.android.com/reference/android/webkit/WebView)
- [JavaScript Interface Security](https://developer.android.com/guide/webapps/webview#BindingJavaScript)
- [XPath Syntax](https://www.w3.org/TR/xpath/)

## License

Copyright (c) 2025 Augmentalis. All rights reserved.
