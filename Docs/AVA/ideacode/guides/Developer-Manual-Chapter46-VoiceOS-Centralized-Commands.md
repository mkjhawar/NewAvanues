# Chapter 46: VoiceOS Centralized Voice Command Architecture

> **SUPERSEDED (260216):** This chapter documents the v1.0/v2.0 architecture using `ArrayJsonParser` and monolithic `.json` files. The current architecture uses `VosParser` (KMP) with split `.app.vos` / `.web.vos` files in compact v3.0 format. See **Chapter 93** (Voice Command Pipeline) and **Chapter 95** (VOS Distribution) for current documentation. `ArrayJsonParser` was deleted in the dead code audit (260216).

**Version:** 2.0 (Corrected)
**Date:** 2025-11-21
**Audience:** Avanues Ecosystem Developers
**Related:** ADR-007 (Centralized Voice Command Architecture)

---

## Overview

This chapter documents the **centralized voice command architecture** used across the Avanues ecosystem. VoiceOS CommandManager loads ALL voice commands system-wide, while individual apps (WebAvanue, NewAvanue, etc.) implement ActionMappers to execute app-specific actions.

**Key Principle:** **VoiceOS = Voice Layer, Apps = Action Layer**

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [VoiceOS CommandManager (System Layer)](#voiceos-commandmanager-system-layer)
3. [App Integration (Application Layer)](#app-integration-application-layer)
4. [Command Flow](#command-flow)
5. [Implementation Guide](#implementation-guide)
6. [Testing](#testing)
7. [Best Practices](#best-practices)
8. [Troubleshooting](#troubleshooting)

---

## Architecture Overview

### Centralized vs Distributed

```
❌ WRONG (Distributed):
WebAvanue loads browser commands
EmailApp loads email commands
Calendar loads calendar commands
→ Commands scattered, duplicate logic, sync issues

✅ CORRECT (Centralized):
VoiceOS loads ALL commands (browser + email + calendar + system)
Apps register handlers to execute their actions
→ Single source of truth, minimal app code
```

### System Layers

| Layer | Responsibility | Examples |
|-------|---------------|----------|
| **Voice Layer (VoiceOS)** | Command loading, recognition, routing | CommandManager, IntentDispatcher |
| **Action Layer (Apps)** | Command execution | WebAvanueActionMapper, EmailActionMapper |

**Analogy:**
- Android Framework handles touch gestures → Apps respond to touch events
- VoiceOS handles voice commands → Apps respond to voice events

---

## VoiceOS CommandManager (System Layer)

### Responsibilities

1. ✅ Load ALL commands from centralized JSON files
2. ✅ Provide multi-locale support (en-US, es-ES, fr-FR, etc.)
3. ✅ Store commands in Room database
4. ✅ Route commands to appropriate apps
5. ✅ Handle confidence scoring and fallbacks

### Command Loading

**CommandLoader loads commands at VoiceOS startup:**

**File:** `VoiceOS/managers/CommandManager/src/main/java/.../loader/CommandLoader.kt`

```kotlin
class CommandLoader(
    private val context: Context,
    private val commandDao: VoiceCommandDao,
    private val versionDao: DatabaseVersionDao
) {
    suspend fun initializeCommands(): LoadResult {
        // 1. Check if already loaded (persistence check)
        val existingVersion = versionDao.getVersion()
        if (existingVersion != null && existingVersion.jsonVersion == requiredVersion) {
            // Skip reload - already loaded
            return LoadResult.Success(
                commandCount = existingVersion.commandCount,
                locales = existingVersion.getLocaleList()
            )
        }

        // 2. Load English fallback (always first)
        loadLocale("en-US", isFallback = true)

        // 3. Load user's system locale
        val systemLocale = getSystemLocale()
        if (systemLocale != "en-US") {
            loadLocale(systemLocale, isFallback = false)
        }

        // 4. Save version to prevent reload on next startup
        versionDao.setVersion(versionEntity)

        return LoadResult.Success(totalCommands, locales)
    }

    private suspend fun loadLocale(locale: String, isFallback: Boolean): LoadResult {
        // Load from: assets/localization/commands/{locale}.json
        val jsonFile = "localization/commands/$locale.json"
        val jsonString = context.assets.open(jsonFile).bufferedReader().use { it.readText() }

        // Parse JSON
        val parseResult = ArrayJsonParser.parseCommandsJson(jsonString, isFallback)

        // Insert into database
        commandDao.insertBatch(parseResult.commands)

        return LoadResult.Success(parseResult.commands.size, listOf(locale))
    }
}
```

**Key Points:**
- ✅ Runs ONCE at VoiceOS startup
- ✅ Loads commands from `assets/localization/commands/{locale}.json`
- ✅ Caches in Room database (SQLite)
- ✅ Skips reload if already loaded (persistence check)
- ✅ Supports fallback (en-US always loaded first)

### Command JSON Format

**File:** `VoiceOS/managers/CommandManager/src/main/assets/localization/commands/en-US.json`

```json
{
  "version": "1.0",
  "locale": "en-US",
  "commands": [
    {
      "id": "SCROLL_TOP",
      "category": "browser",
      "text": "scroll to top",
      "synonyms": [
        "top of page",
        "jump to top",
        "page top",
        "go to top",
        "scroll all the way up"
      ],
      "description": "Scroll to top of web page"
    },
    {
      "id": "SCROLL_BOTTOM",
      "category": "browser",
      "text": "scroll to bottom",
      "synonyms": [
        "bottom of page",
        "jump to bottom",
        "page bottom",
        "go to bottom"
      ],
      "description": "Scroll to bottom of web page"
    },
    {
      "id": "NEW_TAB",
      "category": "browser",
      "text": "new tab",
      "synonyms": [
        "open new tab",
        "create tab",
        "add tab",
        "new browser tab"
      ],
      "description": "Open new browser tab"
    },
    {
      "id": "SEND_EMAIL",
      "category": "email",
      "text": "send email",
      "synonyms": [
        "compose email",
        "new email",
        "write email"
      ],
      "description": "Compose new email"
    },
    {
      "id": "VOLUME_UP",
      "category": "system",
      "text": "volume up",
      "synonyms": [
        "louder",
        "increase volume",
        "turn up"
      ],
      "description": "Increase system volume"
    }
  ]
}
```

**Schema:**
- `id`: Unique command identifier (SCREAMING_SNAKE_CASE)
- `category`: Command category for routing (browser, email, system, etc.)
- `text`: Primary command phrase (lowercase)
- `synonyms`: Alternative phrases (array of lowercase strings)
- `description`: Human-readable description

**Categories Used:**
- `browser`: WebAvanue, browser apps
- `email`: Email clients
- `calendar`: Calendar apps
- `system`: System-wide commands
- `launcher`: Home screen, app drawer
- `files`: File manager
- (Add more as needed)

### Command Routing

**IntentDispatcher routes commands to registered handlers:**

**File:** `VoiceOS/managers/CommandManager/src/main/java/.../routing/IntentDispatcher.kt`

```kotlin
class IntentDispatcher(private val context: Context) {
    // Action handlers registered by apps
    private val actionHandlers = mutableMapOf<String, suspend (Command) -> CommandResult>()

    /**
     * Apps register handlers for categories
     */
    fun registerHandler(category: String, handler: suspend (Command) -> CommandResult) {
        actionHandlers[category] = handler
        Log.d(TAG, "Registered handler for category: $category")
    }

    /**
     * Route command to appropriate handler
     */
    suspend fun routeCommand(command: Command, routingContext: RoutingContext): CommandResult {
        // Find candidate handlers based on:
        // - Command category
        // - Current app context
        // - Historical success rates
        val candidates = findCandidateHandlers(command, routingContext)

        // Score each candidate (confidence scoring)
        val scored = candidates.map { (category, handler) ->
            Triple(category, handler, calculateConfidence(category, command, routingContext))
        }.sortedByDescending { it.third }

        // Try handlers in order of confidence
        for ((category, handler, confidence) in scored) {
            if (confidence < MIN_CONFIDENCE_THRESHOLD) continue

            try {
                val result = handler(command)
                if (result.success) return result
            } catch (e: Exception) {
                Log.w(TAG, "Handler $category threw exception", e)
                continue // Try next handler
            }
        }

        // All handlers failed
        return CommandResult.error("No suitable handler found")
    }

    /**
     * Calculate confidence score (0.0-1.0)
     * Factors: category match, context match, historical success
     */
    private fun calculateConfidence(
        category: String,
        command: Command,
        routingContext: RoutingContext
    ): Float {
        var confidence = 0f

        // Category match (40%)
        if (command.id.startsWith(category, ignoreCase = true)) {
            confidence += 0.4f
        }

        // Current context match (30%)
        if (routingContext.hasAppContext()) {
            val supportsApp = handlerSupportsApp(category, routingContext.currentApp!!)
            if (supportsApp) confidence += 0.3f
        }

        // Historical success in this context (30%)
        val historyScore = getContextHistory(category, routingContext)
        confidence += historyScore * 0.3f

        return confidence.coerceIn(0f, 1f)
    }
}
```

**Routing Context:**
```kotlin
data class RoutingContext(
    val currentApp: String? = null,           // e.g., "com.augmentalis.webavanue"
    val screenState: String? = null,          // e.g., "browser_tab_view"
    val userContext: Map<String, Any> = emptyMap()
)
```

**Key Features:**
- ✅ Context-aware routing (knows which app is active)
- ✅ Confidence scoring (0.0-1.0 for each handler)
- ✅ Fallback chain (tries multiple handlers)
- ✅ Historical learning (tracks success rates)

---

## App Integration (Application Layer)

### Responsibilities

1. ✅ Implement ActionMapper (maps command IDs → actions)
2. ✅ Register handler with IntentDispatcher
3. ❌ Do NOT load commands
4. ❌ Do NOT manage VOS files
5. ❌ Do NOT interact with database directly

### Required Code: ~60 Lines

**Two files per app:**
1. `{App}ActionMapper.kt` (~50 lines)
2. Handler registration in `Application.onCreate()` (~10 lines)

---

## Command Flow

### Complete Flow Diagram

```
[User says: "scroll to top"]
         ↓
┌─────────────────────────────────────────┐
│ VoiceOS Speech Recognition              │
│ - Converts speech to text               │
│ - Text: "scroll to top"                 │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ VoiceOS CommandManager                  │
│ - Creates Command object                │
│ - Command(id="SCROLL_TOP",              │
│           text="scroll to top",         │
│           category="browser")           │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ CommandLoader (already loaded commands) │
│ - Database lookup: "scroll to top"      │
│ - Finds: VoiceCommandEntity(            │
│     id="SCROLL_TOP",                    │
│     category="browser",                 │
│     primaryText="scroll to top")        │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ IntentDispatcher.routeCommand()         │
│ - RoutingContext(currentApp=            │
│     "com.augmentalis.webavanue")        │
│ - Finds handler for "browser" category  │
│ - Confidence score: 0.95 (HIGH)         │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ WebAvanue Browser Handler               │
│ - Registered by WebAvanueApp.onCreate() │
│ - Calls: browserHandler(command)        │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ WebAvanueActionMapper.executeAction()   │
│ - Receives: commandId = "SCROLL_TOP"    │
│ - Routes to: webViewController.         │
│              scrollTop()                 │
└─────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────┐
│ WebViewController                        │
│ - Executes: scrollTop()                 │
│ - WebView scrolls to top of page        │
│ - Returns: ActionResult.success()       │
└─────────────────────────────────────────┘
         ↓
[User sees page scroll to top]
```

**Time:** ~500ms - 5s total (speech recognition + execution)
**Routing overhead:** ~6ms (negligible)

---

## Implementation Guide

### Step-by-Step: Integrate WebAvanue Browser

#### Step 1: Create ActionMapper

**File:** `WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/commands/WebAvanueActionMapper.kt`

```kotlin
package com.augmentalis.Avanues.web.universal.commands

import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabViewModel
import com.augmentalis.Avanues.web.universal.presentation.controller.WebViewController
import android.util.Log

class WebAvanueActionMapper(
    private val tabViewModel: TabViewModel,
    private val webViewController: WebViewController
) {
    companion object {
        private const val TAG = "WebAvanueActionMapper"
    }

    /**
     * Execute browser action for command ID
     *
     * @param commandId VoiceOS command ID (e.g., "SCROLL_TOP")
     * @param parameters Optional parameters from voice input
     * @return ActionResult with success/failure status
     */
    suspend fun executeAction(
        commandId: String,
        parameters: Map<String, Any> = emptyMap()
    ): ActionResult {
        Log.d(TAG, "Executing command: $commandId")

        return when (commandId) {
            // Scrolling
            "SCROLL_UP" -> webViewController.scrollUp()
            "SCROLL_DOWN" -> webViewController.scrollDown()
            "SCROLL_TOP" -> webViewController.scrollTop()
            "SCROLL_BOTTOM" -> webViewController.scrollBottom()

            // Navigation
            "GO_BACK" -> webViewController.goBack()
            "GO_FORWARD" -> webViewController.goForward()
            "RELOAD_PAGE" -> webViewController.reload()

            // Zoom
            "ZOOM_IN", "PINCH_OPEN" -> webViewController.zoomIn()
            "ZOOM_OUT", "PINCH_CLOSE" -> webViewController.zoomOut()
            "RESET_ZOOM" -> webViewController.resetZoom()
            "SET_ZOOM_LEVEL" -> {
                val level = parameters["level"] as? Int ?: 100
                webViewController.setZoomLevel(level)
                ActionResult.success("Zoom set to $level%")
            }

            // Desktop mode
            "DESKTOP_MODE" -> {
                webViewController.setDesktopMode(true)
                ActionResult.success("Desktop mode enabled")
            }
            "MOBILE_MODE" -> {
                webViewController.setDesktopMode(false)
                ActionResult.success("Mobile mode enabled")
            }

            // Page control
            "FREEZE_PAGE" -> webViewController.toggleFreeze()
            "CLEAR_COOKIES" -> webViewController.clearCookies()

            // Tabs
            "NEW_TAB" -> {
                tabViewModel.createTab()
                ActionResult.success("New tab created")
            }
            "CLOSE_TAB" -> {
                val activeTabId = tabViewModel.activeTab.value?.tab?.id
                if (activeTabId != null) {
                    tabViewModel.closeTab(activeTabId)
                    ActionResult.success("Tab closed")
                } else {
                    ActionResult.error("No active tab to close")
                }
            }

            // Bookmarks
            "ADD_BOOKMARK" -> {
                val url = webViewController.getCurrentUrl()
                val title = webViewController.getCurrentTitle()
                // TODO: Implement bookmark logic
                ActionResult.success("Bookmark added: $title")
            }

            // Gestures
            "SINGLE_CLICK" -> webViewController.performClick()
            "DOUBLE_CLICK" -> webViewController.performDoubleClick()
            "DRAG_START" -> webViewController.startDrag()
            "DRAG_STOP" -> webViewController.stopDrag()
            "SELECT" -> webViewController.select()

            else -> {
                Log.w(TAG, "Unknown command: $commandId")
                ActionResult.error("Unknown command: $commandId")
            }
        }
    }
}

/**
 * Result of action execution
 */
data class ActionResult(
    val success: Boolean,
    val message: String? = null,
    val data: Any? = null
) {
    companion object {
        fun success(message: String? = null, data: Any? = null) =
            ActionResult(true, message, data)

        fun error(message: String) =
            ActionResult(false, message, null)
    }
}
```

**Lines:** ~95 (with comments and error handling)

#### Step 2: Register Handler on App Startup

**File:** `WebAvanue/app/src/main/kotlin/.../WebAvanueApp.kt` (or MainActivity)

```kotlin
package com.augmentalis.Avanues.web

import android.app.Application
import android.util.Log
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.commandmanager.models.CommandResult
import com.augmentalis.commandmanager.models.CommandError
import com.augmentalis.commandmanager.models.ErrorCode
import com.augmentalis.Avanues.web.universal.commands.WebAvanueActionMapper

class WebAvanueApp : Application() {
    companion object {
        private const val TAG = "WebAvanueApp"
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize dependencies (your DI/factory)
        val tabViewModel = getTabViewModel()
        val webViewController = getWebViewController()

        // Create ActionMapper
        val actionMapper = WebAvanueActionMapper(tabViewModel, webViewController)

        // Register voice commands
        registerVoiceCommands(actionMapper)
    }

    private fun registerVoiceCommands(actionMapper: WebAvanueActionMapper) {
        try {
            // Get VoiceOS CommandManager
            val commandManager = CommandManager.getInstance(this)

            // Get IntentDispatcher
            val dispatcher = commandManager.getIntentDispatcher()

            // Register handler for "browser" category
            dispatcher.registerHandler("browser") { command ->
                // Execute action via ActionMapper
                val result = actionMapper.executeAction(command.id, emptyMap())

                // Convert to CommandResult
                CommandResult(
                    success = result.success,
                    command = command,
                    error = if (!result.success) {
                        CommandError(
                            ErrorCode.EXECUTION_FAILED,
                            result.message ?: "Unknown error"
                        )
                    } else null
                )
            }

            Log.i(TAG, "✅ Browser voice commands registered with VoiceOS")

        } catch (e: Exception) {
            Log.e(TAG, "⚠️ VoiceOS not available - voice commands disabled", e)
            // Graceful degradation - WebAvanue works without voice (touch/keyboard)
        }
    }

    // Your DI/factory methods
    private fun getTabViewModel(): TabViewModel { ... }
    private fun getWebViewController(): WebViewController { ... }
}
```

**Lines:** ~50 (with error handling and logging)

#### Step 3: Add Commands to VoiceOS JSON

**File:** `VoiceOS/managers/CommandManager/src/main/assets/localization/commands/en-US.json`

Add browser commands to existing JSON:

```json
{
  "version": "1.0",
  "locale": "en-US",
  "commands": [
    // ... existing system commands ...

    // Browser commands
    {
      "id": "SCROLL_TOP",
      "category": "browser",
      "text": "scroll to top",
      "synonyms": ["top of page", "jump to top", "page top", "go to top"],
      "description": "Scroll to top of web page"
    },
    {
      "id": "SCROLL_BOTTOM",
      "category": "browser",
      "text": "scroll to bottom",
      "synonyms": ["bottom of page", "jump to bottom", "page bottom"],
      "description": "Scroll to bottom of web page"
    },
    {
      "id": "NEW_TAB",
      "category": "browser",
      "text": "new tab",
      "synonyms": ["open new tab", "create tab", "add tab"],
      "description": "Open new browser tab"
    },
    {
      "id": "CLOSE_TAB",
      "category": "browser",
      "text": "close tab",
      "synonyms": ["close this tab", "remove tab", "delete tab"],
      "description": "Close current browser tab"
    },
    {
      "id": "ZOOM_IN",
      "category": "browser",
      "text": "zoom in",
      "synonyms": ["enlarge", "magnify", "bigger"],
      "description": "Zoom in on web page"
    }
    // ... add all 13 browser commands ...
  ]
}
```

**Note:** This is the ONLY place browser commands are defined. WebAvanue does NOT have command files.

#### Step 4: Test

1. Build and install VoiceOS (loads commands from JSON)
2. Build and install WebAvanue (registers handler)
3. Open WebAvanue
4. Say: "scroll to top"
5. Verify page scrolls to top

**Expected log output:**
```
VoiceOS CommandManager: ✅ Loaded 100 commands from en-US.json
WebAvanueApp: ✅ Browser voice commands registered with VoiceOS
IntentDispatcher: Routing command SCROLL_TOP to browser handler
WebAvanueActionMapper: Executing command: SCROLL_TOP
WebViewController: Scrolling to top
```

---

## Testing

### Unit Tests (App-level)

Test ActionMapper in isolation:

```kotlin
@Test
fun `SCROLL_TOP executes scrollTop()`() = runTest {
    val mockTabViewModel = mock<TabViewModel>()
    val mockWebViewController = mock<WebViewController>()

    val actionMapper = WebAvanueActionMapper(mockTabViewModel, mockWebViewController)

    val result = actionMapper.executeAction("SCROLL_TOP")

    verify(mockWebViewController).scrollTop()
    assertTrue(result.success)
}

@Test
fun `unknown command returns error`() = runTest {
    val actionMapper = WebAvanueActionMapper(mockTabViewModel, mockWebViewController)

    val result = actionMapper.executeAction("UNKNOWN_COMMAND")

    assertFalse(result.success)
    assertTrue(result.message?.contains("Unknown command") == true)
}

@Test
fun `CLOSE_TAB with no active tab returns error`() = runTest {
    val mockTabViewModel = mock<TabViewModel>().apply {
        whenever(activeTab).thenReturn(MutableStateFlow(null))
    }

    val actionMapper = WebAvanueActionMapper(mockTabViewModel, mockWebViewController)

    val result = actionMapper.executeAction("CLOSE_TAB")

    assertFalse(result.success)
    assertTrue(result.message?.contains("No active tab") == true)
}
```

### Integration Tests (VoiceOS-level)

Test command loading and routing:

```kotlin
@Test
fun `CommandLoader loads browser commands`() = runTest {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val loader = CommandLoader.create(context)

    val result = loader.initializeCommands()

    assertTrue(result is LoadResult.Success)

    val dao = CommandDatabase.getInstance(context).voiceCommandDao()
    val browserCommands = dao.getCommandsForCategory("browser")

    assertTrue(browserCommands.size >= 13) // At least 13 browser commands
    assertTrue(browserCommands.any { it.id == "SCROLL_TOP" })
}

@Test
fun `IntentDispatcher routes SCROLL_TOP to browser handler`() = runTest {
    val dispatcher = IntentDispatcher(context)
    var receivedCommandId: String? = null

    dispatcher.registerHandler("browser") { command ->
        receivedCommandId = command.id
        CommandResult(success = true, command = command)
    }

    val command = Command(id = "SCROLL_TOP", text = "scroll to top", category = "browser")
    val routingContext = RoutingContext(currentApp = "com.augmentalis.webavanue")

    val result = dispatcher.routeCommand(command, routingContext)

    assertTrue(result.success)
    assertEquals("SCROLL_TOP", receivedCommandId)
}
```

### E2E Tests

Test complete flow:

```kotlin
@Test
fun `voice input "scroll to top" executes scrollTop()`() = runTest {
    // Setup
    val commandManager = CommandManager.getInstance(context)
    commandManager.initialize()

    val mockWebViewController = mock<WebViewController>()
    val actionMapper = WebAvanueActionMapper(mockTabViewModel, mockWebViewController)

    commandManager.getIntentDispatcher().registerHandler("browser") { command ->
        val result = actionMapper.executeAction(command.id)
        CommandResult(success = result.success, command = command)
    }

    // Execute
    val command = Command(id = "SCROLL_TOP", text = "scroll to top", category = "browser")
    val result = commandManager.executeCommand(command)

    // Verify
    assertTrue(result.success)
    verify(mockWebViewController).scrollTop()
}
```

---

## Best Practices

### 1. Graceful Degradation

Always handle VoiceOS unavailability:

```kotlin
private fun registerVoiceCommands(actionMapper: WebAvanueActionMapper) {
    try {
        val commandManager = CommandManager.getInstance(this)
        // Register handler...
        Log.i(TAG, "✅ Voice commands enabled")
    } catch (e: Exception) {
        Log.w(TAG, "⚠️ VoiceOS not available - voice disabled", e)
        // App works fine without voice (touch/keyboard)
    }
}
```

### 2. Clear Error Messages

Return helpful error messages:

```kotlin
"CLOSE_TAB" -> {
    val activeTabId = tabViewModel.activeTab.value?.tab?.id
    if (activeTabId != null) {
        tabViewModel.closeTab(activeTabId)
        ActionResult.success("Tab closed")
    } else {
        ActionResult.error("No active tab to close") // ✅ Clear message
    }
}
```

### 3. Logging

Log at key points for debugging:

```kotlin
Log.d(TAG, "Executing command: $commandId")
Log.i(TAG, "✅ Browser voice commands registered")
Log.w(TAG, "Unknown command: $commandId")
Log.e(TAG, "VoiceOS not available", e)
```

### 4. Parameter Validation

Validate parameters for commands that take arguments:

```kotlin
"SET_ZOOM_LEVEL" -> {
    val level = parameters["level"] as? Int
    if (level == null || level !in 50..200) {
        return ActionResult.error("Invalid zoom level: must be 50-200")
    }
    webViewController.setZoomLevel(level)
    ActionResult.success("Zoom set to $level%")
}
```

### 5. Idempotent Actions

Make actions safe to call multiple times:

```kotlin
"DESKTOP_MODE" -> {
    // ✅ Safe to call even if already in desktop mode
    webViewController.setDesktopMode(true)
    ActionResult.success("Desktop mode enabled")
}
```

### 6. Action Feedback

Provide user feedback on success:

```kotlin
"NEW_TAB" -> {
    tabViewModel.createTab()
    ActionResult.success("New tab created") // ✅ User knows action succeeded
}
```

---

## Troubleshooting

### Commands Not Recognized

**Problem:** User says "scroll to top" but nothing happens

**Debug Steps:**
1. Check VoiceOS logs: `adb logcat | grep CommandLoader`
   - Verify commands loaded: "✅ Loaded 100 commands"
2. Check command exists: Query database
   ```kotlin
   val command = commandDao.searchCommands("en-US", "scroll to top")
   Log.d(TAG, "Found: ${command?.id}") // Should be "SCROLL_TOP"
   ```
3. Check handler registered: `adb logcat | grep WebAvanueApp`
   - Should see: "✅ Browser voice commands registered"

**Common Causes:**
- VoiceOS didn't load commands (check JSON syntax)
- Handler not registered (exception during registration)
- Wrong locale (user's locale doesn't have command)

### Handler Not Called

**Problem:** Command recognized but handler not called

**Debug Steps:**
1. Check IntentDispatcher logs: `adb logcat | grep IntentDispatcher`
   - Should see: "Routing command SCROLL_TOP"
2. Check confidence score
   ```kotlin
   Log.d(TAG, "Confidence: $confidence") // Should be >= 0.3
   ```
3. Check current app context
   ```kotlin
   Log.d(TAG, "Current app: ${routingContext.currentApp}")
   // Should be "com.augmentalis.webavanue" when WebAvanue active
   ```

**Common Causes:**
- Low confidence score (category mismatch)
- Wrong app context (command for browser but email app active)
- Handler threw exception (check logs)

### Action Execution Failed

**Problem:** Handler called but action failed

**Debug Steps:**
1. Check ActionMapper logs: `adb logcat | grep WebAvanueActionMapper`
   - Should see: "Executing command: SCROLL_TOP"
2. Check WebViewController logs
   - Verify scrollTop() was called
3. Check for exceptions

**Common Causes:**
- WebView not initialized
- Tab not active
- Invalid state (e.g., CLOSE_TAB but no tabs)

### VoiceOS Not Available

**Problem:** App crashes when VoiceOS not installed

**Solution:** Always wrap in try-catch:

```kotlin
try {
    val commandManager = CommandManager.getInstance(this)
    // ...
} catch (e: Exception) {
    Log.w(TAG, "VoiceOS not available - graceful fallback", e)
    // App works without voice
}
```

---

## Summary

### What You Learned

1. ✅ **VoiceOS loads ALL commands centrally** (system + browser + app)
2. ✅ **Apps only implement ActionMappers** (~50 lines)
3. ✅ **Register handler with IntentDispatcher** (~10 lines)
4. ✅ **Total code per app: ~60 lines** (vs 150+ for distributed)
5. ✅ **Single source of truth** (commands in VoiceOS JSON)

### Key Takeaways

| Concept | Implementation |
|---------|---------------|
| **Command Loading** | VoiceOS CommandLoader (centralized) |
| **Command Storage** | VoiceOS Room database |
| **Command Routing** | VoiceOS IntentDispatcher |
| **Action Execution** | App ActionMapper |
| **Registration** | Application.onCreate() |

### Next Steps

1. ✅ Read ADR-007 for architectural rationale
2. ⏳ Implement WebAvanueActionMapper.kt
3. ⏳ Register handler in WebAvanueApp.onCreate()
4. ⏳ Add browser commands to VoiceOS en-US.json
5. ⏳ Test voice commands with WebAvanue
6. ⏳ Add Spanish/French commands (optional)

---

**Version:** 2.0
**Last Updated:** 2025-11-21
**Status:** Corrected (Centralized Architecture)
**Author:** AVA AI Team

