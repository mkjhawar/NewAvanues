# Universal IPC Implementation Specification

**Version:** 1.0.0
**Created:** 2025-11-22
**Author:** Manoj Jhawar
**Target:** VoiceOS ecosystem apps (WebAvanue, AVA AI, AvaConnect)

---

## Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Sender Implementation (VoiceOS)](#sender-implementation-voiceos)
4. [Receiver Implementation (External Apps)](#receiver-implementation-external-apps)
5. [Protocol Contract](#protocol-contract)
6. [Testing Requirements](#testing-requirements)
7. [Security Considerations](#security-considerations)

---

## Overview

This specification defines how to implement cross-app communication using the Universal IPC Protocol v2.0.0 in the VoiceOS ecosystem.

### Scope

- **Sender:** VoiceOS CommandManager → External Apps
- **Receiver:** WebAvanue, AVA AI, AvaConnect, etc.
- **Protocol:** Universal IPC Protocol v2.0.0
- **Transport:** Android Intent Broadcast
- **Primary Use Case:** Voice commands (VCM code)

### Goals

✅ Enable VoiceOS to send voice commands to external apps
✅ Provide bi-directional communication (request → response)
✅ Maintain 87% size reduction vs JSON
✅ Keep latency under 200ms
✅ Support 41 browser commands (WebAvanue)
✅ Extensible for AI queries (AVA AI) and other use cases

---

## Architecture

### Communication Flow

```
┌─────────────────┐                    ┌─────────────────┐
│   VoiceOS       │                    │   WebAvanue     │
│  CommandManager │                    │    Browser      │
└────────┬────────┘                    └────────▲────────┘
         │                                      │
         │ 1. encodeVoiceCommand()              │
         │    "VCM:scroll_top:SCROLL_TOP"       │
         │                                      │
         │ 2. Intent Broadcast                  │
         │    Action: voiceos.IPC.COMMAND       │
         │    Extra: message, source_app        │
         ├──────────────────────────────────────┤
         │                                      │
         │                              3. BroadcastReceiver
         │                                 onReceive()
         │                                      │
         │                              4. Parse message
         │                                 extractCode()
         │                                      │
         │                              5. Execute action
         │                                 webView.scrollTo()
         │                                      │
         │ 6. Response (Optional)               │
         │    "ACC:scroll_top"                  │
         ◄──────────────────────────────────────┤
```

### Component Responsibilities

| Component | Responsibility |
|-----------|----------------|
| **UniversalIPCEncoder** | Encode/decode IPC messages |
| **CommandManager** | Send commands to external apps |
| **BroadcastReceiver** | Receive IPC commands in external app |
| **Action Mapper** | Map IPC commands to app-specific actions |
| **Response Sender** | Send acknowledgment back to VoiceOS (optional) |

---

## Sender Implementation (VoiceOS)

### Step 1: Add Dependency

**File:** `modules/managers/CommandManager/build.gradle.kts`

```kotlin
dependencies {
    implementation(project(":modules:libraries:UniversalIPC"))
}
```

### Step 2: Create Encoder Instance

**File:** `CommandManager.kt`

```kotlin
import com.augmentalis.universalipc.UniversalIPCEncoder

class CommandManager(private val context: Context) {
    private val ipcEncoder = UniversalIPCEncoder()
}
```

### Step 3: Implement Send Method

**File:** `CommandManager.kt`

```kotlin
/**
 * Execute command in external app via Universal IPC Protocol
 */
suspend fun executeExternalCommand(
    command: Command,
    targetApp: String,
    params: Map<String, Any> = emptyMap()
) {
    try {
        // 1. Encode command to Universal IPC format
        val ipcMessage = ipcEncoder.encodeVoiceCommand(
            commandId = command.id,
            action = command.id.uppercase(),  // "scroll_top" → "SCROLL_TOP"
            params = params
        )
        // Result: "VCM:scroll_top:SCROLL_TOP"

        // 2. Create Intent broadcast
        val intent = Intent(UniversalIPCEncoder.IPC_ACTION).apply {
            setPackage(targetApp)  // "com.augmentalis.webavanue"
            putExtra(UniversalIPCEncoder.EXTRA_SOURCE_APP, context.packageName)
            putExtra(UniversalIPCEncoder.EXTRA_MESSAGE, ipcMessage)
        }

        // 3. Send broadcast to target app
        context.sendBroadcast(intent)

        Log.d(TAG, "✓ Sent IPC command to $targetApp: $ipcMessage")
    } catch (e: Exception) {
        Log.e(TAG, "✗ Failed to send IPC command to $targetApp", e)
        throw e
    }
}
```

### Step 4: Usage Example

```kotlin
// In VoiceOSService or CommandManager
scope.launch {
    val command = Command(
        id = "scroll_top",
        text = "scroll to top",
        confidence = 0.95f
    )

    commandManager.executeExternalCommand(
        command = command,
        targetApp = "com.augmentalis.webavanue"
    )
}
```

### Target App Package Names

| App | Package Name |
|-----|--------------|
| WebAvanue | `com.augmentalis.webavanue` |
| AVA AI | `com.augmentalis.avaai` |
| AvaConnect | `com.augmentalis.avaconnect` |
| BrowserAvanue | `com.augmentalis.browseravanue` |

---

## Receiver Implementation (External Apps)

### Required Files

1. `UniversalIPCReceiver.kt` - BroadcastReceiver
2. `ActionMapper.kt` - Command → Action mapping
3. `AndroidManifest.xml` - Receiver registration

### Step 1: Add Dependency

**File:** `app/build.gradle.kts` (in WebAvanue, AVA AI, etc.)

```kotlin
dependencies {
    // Option A: If UniversalIPC is published to Maven/JitPack
    implementation("com.augmentalis:universalipc:1.0.0")

    // Option B: If using local VoiceOS project
    implementation(project(":modules:libraries:UniversalIPC"))
}
```

### Step 2: Create BroadcastReceiver

**File:** `UniversalIPCReceiver.kt`

```kotlin
package com.augmentalis.webavanue.ipc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.universalipc.UniversalIPCEncoder

/**
 * Receives Universal IPC commands from VoiceOS
 */
class UniversalIPCReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "UniversalIPCReceiver"
    }

    private val encoder = UniversalIPCEncoder()

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Verify action
        if (intent.action != UniversalIPCEncoder.IPC_ACTION) {
            Log.w(TAG, "Ignoring intent with action: ${intent.action}")
            return
        }

        // 2. Extract message and source
        val message = intent.getStringExtra(UniversalIPCEncoder.EXTRA_MESSAGE)
        val sourceApp = intent.getStringExtra(UniversalIPCEncoder.EXTRA_SOURCE_APP)

        if (message == null) {
            Log.e(TAG, "Received IPC with null message")
            return
        }

        Log.d(TAG, "Received IPC from $sourceApp: $message")

        // 3. Validate message format
        if (!encoder.isValidMessage(message)) {
            Log.e(TAG, "Invalid IPC message format: $message")
            sendErrorResponse(context, sourceApp, "INVALID_FORMAT", message)
            return
        }

        // 4. Extract protocol code
        val code = encoder.extractCode(message)

        // 5. Route to appropriate handler
        when (code) {
            UniversalIPCEncoder.CODE_VOICE_COMMAND -> {
                handleVoiceCommand(context, sourceApp, message)
            }
            UniversalIPCEncoder.CODE_AI_QUERY -> {
                handleAIQuery(context, sourceApp, message)
            }
            UniversalIPCEncoder.CODE_NAV -> {
                handleNavigation(context, sourceApp, message)
            }
            UniversalIPCEncoder.CODE_URL -> {
                handleUrlShare(context, sourceApp, message)
            }
            else -> {
                Log.w(TAG, "Unsupported protocol code: $code")
                sendErrorResponse(context, sourceApp, "UNSUPPORTED_CODE", message)
            }
        }
    }

    /**
     * Handle voice command (VCM)
     * Format: VCM:commandId:action:param1:param2
     */
    private fun handleVoiceCommand(context: Context, sourceApp: String?, message: String) {
        try {
            // Parse message: "VCM:scroll_top:SCROLL_TOP"
            val parts = message.split(":", limit = 4)

            if (parts.size < 3) {
                Log.e(TAG, "Invalid VCM format: $message")
                sendErrorResponse(context, sourceApp, "INVALID_VCM_FORMAT", message)
                return
            }

            val commandId = encoder.unescape(parts[1])  // "scroll_top"
            val action = encoder.unescape(parts[2])     // "SCROLL_TOP"
            val params = if (parts.size > 3) encoder.unescape(parts[3]) else ""

            Log.d(TAG, "VCM: commandId=$commandId, action=$action, params=$params")

            // Execute action via ActionMapper
            val actionMapper = BrowserActionMapper.getInstance(context)
            val success = actionMapper.execute(action, params)

            if (success) {
                sendAcceptResponse(context, sourceApp, commandId)
            } else {
                sendErrorResponse(context, sourceApp, "EXECUTION_FAILED", commandId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling VCM: $message", e)
            sendErrorResponse(context, sourceApp, "EXCEPTION", message)
        }
    }

    /**
     * Handle navigation command (NAV)
     * Format: NAV:sessionId:url
     */
    private fun handleNavigation(context: Context, sourceApp: String?, message: String) {
        try {
            val parts = message.split(":", limit = 3)

            if (parts.size < 3) {
                sendErrorResponse(context, sourceApp, "INVALID_NAV_FORMAT", message)
                return
            }

            val sessionId = encoder.unescape(parts[1])
            val url = encoder.unescape(parts[2])

            Log.d(TAG, "NAV: sessionId=$sessionId, url=$url")

            // Navigate to URL
            val actionMapper = BrowserActionMapper.getInstance(context)
            val success = actionMapper.navigate(url)

            if (success) {
                sendAcceptResponse(context, sourceApp, sessionId)
            } else {
                sendErrorResponse(context, sourceApp, "NAVIGATION_FAILED", sessionId)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling NAV: $message", e)
            sendErrorResponse(context, sourceApp, "EXCEPTION", message)
        }
    }

    /**
     * Handle URL share (URL)
     * Format: URL:sessionId:url
     */
    private fun handleUrlShare(context: Context, sourceApp: String?, message: String) {
        try {
            val parts = message.split(":", limit = 3)

            if (parts.size < 3) {
                sendErrorResponse(context, sourceApp, "INVALID_URL_FORMAT", message)
                return
            }

            val sessionId = encoder.unescape(parts[1])
            val url = encoder.unescape(parts[2])

            Log.d(TAG, "URL: sessionId=$sessionId, url=$url")

            // Share URL (e.g., add to shared session)
            // Implementation depends on app functionality

            sendAcceptResponse(context, sourceApp, sessionId)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling URL: $message", e)
            sendErrorResponse(context, sourceApp, "EXCEPTION", message)
        }
    }

    /**
     * Handle AI query (AIQ)
     * Format: AIQ:queryId:query_text
     */
    private fun handleAIQuery(context: Context, sourceApp: String?, message: String) {
        try {
            val parts = message.split(":", limit = 3)

            if (parts.size < 3) {
                sendErrorResponse(context, sourceApp, "INVALID_AIQ_FORMAT", message)
                return
            }

            val queryId = encoder.unescape(parts[1])
            val query = encoder.unescape(parts[2])

            Log.d(TAG, "AIQ: queryId=$queryId, query=$query")

            // Process AI query (for AVA AI app)
            // Send response via AIR code

            sendAcceptResponse(context, sourceApp, queryId)

        } catch (e: Exception) {
            Log.e(TAG, "Error handling AIQ: $message", e)
            sendErrorResponse(context, sourceApp, "EXCEPTION", message)
        }
    }

    /**
     * Send accept response (ACC) back to VoiceOS
     */
    private fun sendAcceptResponse(context: Context, targetApp: String?, requestId: String) {
        if (targetApp == null) return

        val response = encoder.encodeAccept(requestId)
        val intent = Intent(UniversalIPCEncoder.IPC_ACTION).apply {
            setPackage(targetApp)
            putExtra(UniversalIPCEncoder.EXTRA_SOURCE_APP, context.packageName)
            putExtra(UniversalIPCEncoder.EXTRA_MESSAGE, response)
        }

        context.sendBroadcast(intent)
        Log.d(TAG, "Sent ACC response: $response")
    }

    /**
     * Send error response (ERR) back to VoiceOS
     */
    private fun sendErrorResponse(
        context: Context,
        targetApp: String?,
        errorCode: String,
        requestId: String
    ) {
        if (targetApp == null) return

        val response = encoder.encodeError(requestId, errorCode)
        val intent = Intent(UniversalIPCEncoder.IPC_ACTION).apply {
            setPackage(targetApp)
            putExtra(UniversalIPCEncoder.EXTRA_SOURCE_APP, context.packageName)
            putExtra(UniversalIPCEncoder.EXTRA_MESSAGE, response)
        }

        context.sendBroadcast(intent)
        Log.d(TAG, "Sent ERR response: $response")
    }
}
```

### Step 3: Create Action Mapper

**File:** `BrowserActionMapper.kt` (for WebAvanue)

```kotlin
package com.augmentalis.webavanue.ipc

import android.content.Context
import android.util.Log
import android.webkit.WebView

/**
 * Maps Universal IPC commands to WebView actions
 * Handles all 41 browser commands from browser-commands.vos
 */
class BrowserActionMapper private constructor(private val context: Context) {

    companion object {
        private const val TAG = "BrowserActionMapper"

        @Volatile
        private var instance: BrowserActionMapper? = null

        fun getInstance(context: Context): BrowserActionMapper {
            return instance ?: synchronized(this) {
                instance ?: BrowserActionMapper(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }

    // Reference to current WebView (set by MainActivity or WebViewFragment)
    var webView: WebView? = null

    /**
     * Execute browser action from IPC command
     *
     * @param action Action name (e.g., "SCROLL_TOP", "ZOOM_IN")
     * @param params Optional parameters (e.g., "url=https://google.com")
     * @return true if executed successfully, false otherwise
     */
    fun execute(action: String, params: String = ""): Boolean {
        val currentWebView = webView

        if (currentWebView == null) {
            Log.e(TAG, "WebView not set - cannot execute action: $action")
            return false
        }

        Log.d(TAG, "Executing action: $action with params: $params")

        return try {
            when (action) {
                // Navigation commands
                "GO_BACK" -> {
                    if (currentWebView.canGoBack()) {
                        currentWebView.goBack()
                        true
                    } else {
                        Log.w(TAG, "Cannot go back")
                        false
                    }
                }
                "GO_FORWARD" -> {
                    if (currentWebView.canGoForward()) {
                        currentWebView.goForward()
                        true
                    } else {
                        Log.w(TAG, "Cannot go forward")
                        false
                    }
                }
                "RELOAD" -> {
                    currentWebView.reload()
                    true
                }
                "STOP_LOADING" -> {
                    currentWebView.stopLoading()
                    true
                }

                // Scrolling commands
                "SCROLL_TOP" -> {
                    currentWebView.scrollTo(0, 0)
                    true
                }
                "SCROLL_BOTTOM" -> {
                    currentWebView.scrollTo(0, Int.MAX_VALUE)
                    true
                }
                "SCROLL_UP" -> {
                    currentWebView.scrollBy(0, -500)
                    true
                }
                "SCROLL_DOWN" -> {
                    currentWebView.scrollBy(0, 500)
                    true
                }
                "PAGE_UP" -> {
                    currentWebView.pageUp(false)
                    true
                }
                "PAGE_DOWN" -> {
                    currentWebView.pageDown(false)
                    true
                }

                // Zoom commands
                "ZOOM_IN" -> {
                    currentWebView.zoomIn()
                    true
                }
                "ZOOM_OUT" -> {
                    currentWebView.zoomOut()
                    true
                }
                "RESET_ZOOM" -> {
                    currentWebView.settings.textZoom = 100
                    true
                }

                // URL navigation
                "NAVIGATE" -> {
                    val url = extractParam(params, "url")
                    if (url != null) {
                        navigate(url)
                    } else {
                        Log.e(TAG, "NAVIGATE requires url parameter")
                        false
                    }
                }

                // Search
                "SEARCH" -> {
                    val query = extractParam(params, "query")
                    if (query != null) {
                        val searchUrl = "https://www.google.com/search?q=${query}"
                        navigate(searchUrl)
                    } else {
                        Log.e(TAG, "SEARCH requires query parameter")
                        false
                    }
                }

                // Tab management (if multi-tab support exists)
                "NEW_TAB" -> {
                    // Implementation depends on tab architecture
                    Log.w(TAG, "NEW_TAB not implemented yet")
                    false
                }
                "CLOSE_TAB" -> {
                    Log.w(TAG, "CLOSE_TAB not implemented yet")
                    false
                }

                // Find in page
                "FIND_IN_PAGE" -> {
                    val query = extractParam(params, "query")
                    if (query != null) {
                        currentWebView.findAllAsync(query)
                        true
                    } else {
                        Log.e(TAG, "FIND_IN_PAGE requires query parameter")
                        false
                    }
                }
                "FIND_NEXT" -> {
                    currentWebView.findNext(true)
                    true
                }
                "FIND_PREVIOUS" -> {
                    currentWebView.findNext(false)
                    true
                }
                "CLEAR_FIND" -> {
                    currentWebView.clearMatches()
                    true
                }

                // Bookmarks
                "ADD_BOOKMARK" -> {
                    val url = currentWebView.url ?: ""
                    val title = currentWebView.title ?: "Untitled"
                    // Save to bookmarks database
                    Log.d(TAG, "Adding bookmark: $title - $url")
                    true
                }

                // History
                "CLEAR_HISTORY" -> {
                    currentWebView.clearHistory()
                    true
                }

                else -> {
                    Log.w(TAG, "Unknown action: $action")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing action: $action", e)
            false
        }
    }

    /**
     * Navigate to URL
     */
    fun navigate(url: String): Boolean {
        val currentWebView = webView ?: return false

        return try {
            var finalUrl = url

            // Add https:// if no protocol specified
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                finalUrl = "https://$url"
            }

            currentWebView.loadUrl(finalUrl)
            Log.d(TAG, "Navigated to: $finalUrl")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to: $url", e)
            false
        }
    }

    /**
     * Extract parameter from params string
     * Format: "key1=value1,key2=value2" or "url=https://google.com"
     */
    private fun extractParam(params: String, key: String): String? {
        if (params.isEmpty()) return null

        // Simple key=value extraction
        val regex = Regex("$key=([^,]+)")
        val match = regex.find(params)
        return match?.groupValues?.getOrNull(1)
    }
}
```

### Step 4: Register Receiver in AndroidManifest.xml

**File:** `AndroidManifest.xml` (in WebAvanue, AVA AI, etc.)

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.augmentalis.webavanue">

    <application
        android:name=".WebAvanueApplication"
        android:label="@string/app_name"
        android:icon="@mipmap/ic_launcher"
        android:theme="@style/AppTheme">

        <!-- Universal IPC Receiver -->
        <receiver
            android:name=".ipc.UniversalIPCReceiver"
            android:exported="true"
            android:enabled="true">
            <intent-filter>
                <action android:name="com.augmentalis.voiceos.IPC.COMMAND" />
            </intent-filter>
        </receiver>

        <!-- Other components... -->

    </application>

</manifest>
```

### Step 5: Initialize WebView Reference

**File:** `MainActivity.kt` or `WebViewFragment.kt`

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webView)

        // Set WebView reference in ActionMapper
        BrowserActionMapper.getInstance(this).webView = webView

        // Configure WebView
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            // ... other settings
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clear reference
        BrowserActionMapper.getInstance(this).webView = null
    }
}
```

---

## Protocol Contract

### Message Format Rules

1. **Code must be 3 uppercase letters**
   - Valid: `VCM`, `ACC`, `ERR`
   - Invalid: `vcm`, `VC`, `VOICE`

2. **Delimiter is colon (`:`)**
   - URL-encoded as `%3A` when inside parameters

3. **Special characters must be escaped**
   - `:` → `%3A`
   - `%` → `%25`
   - `\n` → `%0A`
   - `\r` → `%0D`

4. **Minimum format:** `CODE:id`
   - Example: `ACC:cmd123`

5. **Maximum recommended size:** 4KB
   - Prevents Intent size limits

### Required Intent Extras

| Extra Key | Type | Required | Description |
|-----------|------|----------|-------------|
| `message` | String | ✅ Yes | Encoded IPC message |
| `source_app` | String | ✅ Yes | Package name of sender |

### Protocol Codes

See [README.md Protocol Codes Reference](README.md#protocol-codes-reference) for complete list.

### Response Codes

Receivers SHOULD send response for all requests:

| Request Code | Success Response | Error Response |
|--------------|------------------|----------------|
| `VCM` | `ACC:commandId` | `ERR:commandId:error_message` |
| `AIQ` | `AIR:queryId:response` | `ERR:queryId:error_message` |
| `NAV` | `ACC:sessionId` | `ERR:sessionId:error_message` |
| `URL` | `ACC:sessionId` | `ERR:sessionId:error_message` |

---

## Testing Requirements

### Unit Tests

1. **Encoder Tests**
   ```kotlin
   @Test
   fun testEncodeVoiceCommand() {
       val encoder = UniversalIPCEncoder()
       val result = encoder.encodeVoiceCommand("cmd1", "SCROLL_TOP")
       assertEquals("VCM:cmd1:SCROLL_TOP", result)
   }

   @Test
   fun testEscapeSpecialCharacters() {
       val encoder = UniversalIPCEncoder()
       val result = encoder.encodeNavigate("s1", "https://google.com")
       assertEquals("NAV:s1:https%3A%2F%2Fgoogle.com", result)
   }
   ```

2. **Decoder Tests**
   ```kotlin
   @Test
   fun testExtractCode() {
       val encoder = UniversalIPCEncoder()
       val code = encoder.extractCode("VCM:cmd1:SCROLL_TOP")
       assertEquals("VCM", code)
   }

   @Test
   fun testUnescapeParameters() {
       val encoder = UniversalIPCEncoder()
       val result = encoder.unescape("https%3A%2F%2Fgoogle.com")
       assertEquals("https://google.com", result)
   }
   ```

### Integration Tests

1. **Send/Receive Test**
   ```kotlin
   @Test
   fun testSendReceiveVoiceCommand() {
       // Send from VoiceOS
       commandManager.executeExternalCommand(command, targetApp)

       // Wait for broadcast
       Thread.sleep(100)

       // Verify receiver called
       verify(mockActionMapper).execute("SCROLL_TOP", "")
   }
   ```

2. **Response Test**
   ```kotlin
   @Test
   fun testReceiveResponseFromApp() {
       // Send command
       commandManager.executeExternalCommand(command, targetApp)

       // Wait for response
       val response = responseChannel.receive()

       // Verify ACC response
       assertTrue(response.startsWith("ACC:"))
   }
   ```

### End-to-End Tests

1. **Real App Communication**
   - Install VoiceOS and WebAvanue on device
   - Send voice command: "scroll to top"
   - Verify WebView scrolls to top
   - Verify ACC response received

2. **Error Handling**
   - Send invalid command
   - Verify ERR response received
   - Verify error message is descriptive

3. **Performance Test**
   - Send 100 commands in sequence
   - Measure average latency (target: <200ms)
   - Verify no message loss

---

## Security Considerations

### 1. Package Verification

**ALWAYS verify source app:**

```kotlin
override fun onReceive(context: Context, intent: Intent) {
    val sourceApp = intent.getStringExtra(UniversalIPCEncoder.EXTRA_SOURCE_APP)

    // Whitelist trusted apps
    val trustedApps = listOf(
        "com.augmentalis.voiceos",
        "com.augmentalis.avaai",
        "com.augmentalis.avaconnect"
    )

    if (sourceApp !in trustedApps) {
        Log.w(TAG, "Ignoring IPC from untrusted app: $sourceApp")
        return
    }
}
```

### 2. Input Validation

**ALWAYS validate message format:**

```kotlin
if (!encoder.isValidMessage(message)) {
    Log.e(TAG, "Invalid message format: $message")
    return
}

// Validate length
if (message.length > 4096) {
    Log.e(TAG, "Message too large: ${message.length} bytes")
    return
}
```

### 3. URL Validation

**For NAV and URL commands:**

```kotlin
private fun isValidUrl(url: String): Boolean {
    return try {
        val uri = Uri.parse(url)
        val scheme = uri.scheme

        // Only allow http/https
        scheme == "http" || scheme == "https"
    } catch (e: Exception) {
        false
    }
}
```

### 4. Permission Checks

**Receiver must be exported:**

```xml
<receiver
    android:name=".UniversalIPCReceiver"
    android:exported="true">  <!-- Required for cross-app IPC -->
</receiver>
```

### 5. Rate Limiting

**Prevent spam:**

```kotlin
private val lastReceiveTime = mutableMapOf<String, Long>()
private const val MIN_INTERVAL_MS = 100L  // 100ms between commands

override fun onReceive(context: Context, intent: Intent) {
    val sourceApp = intent.getStringExtra(UniversalIPCEncoder.EXTRA_SOURCE_APP)
    val now = System.currentTimeMillis()
    val lastTime = lastReceiveTime[sourceApp] ?: 0L

    if (now - lastTime < MIN_INTERVAL_MS) {
        Log.w(TAG, "Rate limit exceeded from $sourceApp")
        return
    }

    lastReceiveTime[sourceApp] = now

    // Process message...
}
```

---

## Implementation Checklist

### Sender (VoiceOS)

- [ ] Add UniversalIPC library dependency
- [ ] Create UniversalIPCEncoder instance
- [ ] Implement `executeExternalCommand()` method
- [ ] Add target app package names
- [ ] Test encoding with special characters
- [ ] Test broadcast sending
- [ ] Implement response listener (optional)
- [ ] Add error handling
- [ ] Add logging

### Receiver (WebAvanue/External App)

- [ ] Add UniversalIPC library dependency
- [ ] Create `UniversalIPCReceiver` class
- [ ] Create `ActionMapper` class
- [ ] Register receiver in AndroidManifest.xml
- [ ] Implement command parsing
- [ ] Implement action execution
- [ ] Add package whitelist security
- [ ] Add input validation
- [ ] Add URL validation (if applicable)
- [ ] Add rate limiting
- [ ] Implement response sending
- [ ] Add unit tests
- [ ] Add integration tests
- [ ] Test end-to-end on device

---

## Troubleshooting

### Issue: Message not received

**Solution:**
1. Check logcat: `adb logcat | grep UniversalIPC`
2. Verify receiver is registered in manifest
3. Verify receiver is `android:exported="true"`
4. Check package name: `adb shell pm list packages | grep webavanue`

### Issue: WebView is null

**Solution:**
1. Set WebView reference in ActionMapper after WebView creation
2. Check WebView lifecycle (onCreate vs onStart)
3. Clear reference in onDestroy

### Issue: Special characters corrupted

**Solution:**
1. Always use `encoder.escape()` for parameters
2. Always use `encoder.unescape()` when parsing
3. Never manually build IPC strings

### Issue: Latency too high (>200ms)

**Solution:**
1. Move heavy processing off main thread
2. Use coroutines for async operations
3. Minimize logging in production
4. Profile with Android Profiler

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-11-22 | Initial specification |

---

## References

- [Universal IPC Protocol v2.0.0 Spec](../../../../../../Coding/AvaConnect/Docs/UNIVERSAL-IPC-SPEC.md)
- [UniversalIPC README](README.md)
- [VoiceOS Command System](../../../managers/CommandManager/README.md)
- [Browser Commands Definition](../../../managers/CommandManager/src/main/assets/commands/en-US/browser-commands.vos)

---

**Author:** Manoj Jhawar
**Email:** manoj@ideahq.net
**License:** Proprietary - Augmentalis Inc
**Project:** VoiceOS v4
