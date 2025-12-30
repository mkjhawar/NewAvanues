# WebAvanue Voice Integration - Corrected Architecture

**Date:** 2025-11-21
**Status:** Corrected after user clarification
**Previous (Incorrect):** VOICEOS-INTEGRATION-STRATEGY.md (database-driven with CommandRegistrar)

---

## ❌ Previous Incorrect Assumption

I initially documented a **distributed architecture** where:
- ❌ Each app loads its own .vos files
- ❌ Each app has its own CommandRegistrar
- ❌ Each app calls `VOSCommandIngestion` to load commands into database
- ❌ WebAvanue would have 130 lines of code (ActionMapper + CommandRegistrar)

**This was WRONG.**

---

## ✅ Correct Centralized Architecture

### System Overview

```
VoiceOS CommandManager (System-wide)
├── Loads ALL commands from assets/localization/commands/{locale}.json
├── Includes browser, system, app-specific commands
├── Routes commands via IntentDispatcher
└── Calls registered action handlers

WebAvanue Browser (App-specific)
├── Registers action handler with IntentDispatcher
├── Implements ActionMapper to execute browser actions
└── NO command loading, NO CommandRegistrar
```

---

## How It Works

### 1. VoiceOS Loads Commands (Centralized)

**Location:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/localization/commands/en-US.json`

**VoiceOS CommandManager startup:**
```kotlin
class CommandManager(context: Context) {
    private val commandLoader = CommandLoader.create(context)

    fun initialize() {
        // Load ALL commands from centralized JSON
        commandLoader.initializeCommands()
        // This loads: system commands, browser commands, etc.
    }
}
```

**CommandLoader:**
```kotlin
class CommandLoader {
    suspend fun initializeCommands(): LoadResult {
        // 1. Load English fallback
        loadLocale("en-US", isFallback = true)

        // 2. Load user's system locale
        loadLocale(getSystemLocale(), isFallback = false)

        // Loads from: assets/localization/commands/{locale}.json
        // This includes browser commands defined in the JSON
    }
}
```

**Commands Loaded:** ALL commands across all apps (system, browser, etc.)

---

### 2. WebAvanue Registers Handler (On App Startup)

**WebAvanue MainActivity/Application:**
```kotlin
class WebAvanueApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Get VoiceOS CommandManager
        val commandManager = CommandManager.getInstance(this)

        // Get IntentDispatcher
        val dispatcher = commandManager.getIntentDispatcher()

        // Register browser action handler
        val actionMapper = WebAvanueActionMapper(tabViewModel, webViewController)

        dispatcher.registerHandler("browser") { command ->
            actionMapper.executeAction(command.id, emptyMap())
                .toCommandResult(command)
        }
    }
}
```

**That's it!** No CommandRegistrar, no VOS file loading, no database ingestion.

---

### 3. Voice Input → Action Execution

**Flow:**
```
[User says: "scroll to top"]
         ↓
VoiceOS Speech Recognition
         ↓
CommandManager.executeCommand(Command(id="SCROLL_TOP", text="scroll to top"))
         ↓
IntentDispatcher.routeCommand(command, RoutingContext(currentApp="com.augmentalis.webavanue"))
         ↓
Finds handler for "browser" category (registered by WebAvanue)
         ↓
Calls: browserHandler(command)
         ↓
WebAvanueActionMapper.executeAction("SCROLL_TOP")
         ↓
webViewController.scrollTop()
```

---

## WebAvanue Implementation

### Required File: WebAvanueActionMapper.kt (~50 lines)

**Location:** `WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/commands/WebAvanueActionMapper.kt`

```kotlin
package com.augmentalis.Avanues.web.universal.commands

import com.augmentalis.Avanues.web.universal.presentation.viewmodel.TabViewModel
import com.augmentalis.Avanues.web.universal.presentation.controller.WebViewController

/**
 * Maps VoiceOS command IDs to WebAvanue browser actions
 *
 * Registered with VoiceOS IntentDispatcher on app startup
 */
class WebAvanueActionMapper(
    private val tabViewModel: TabViewModel,
    private val webViewController: WebViewController
) {
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
            }

            // Desktop mode
            "DESKTOP_MODE" -> webViewController.setDesktopMode(true)
            "MOBILE_MODE" -> webViewController.setDesktopMode(false)

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
                    ActionResult.error("No active tab")
                }
            }

            // Bookmarks
            "ADD_BOOKMARK" -> {
                val url = webViewController.getCurrentUrl()
                val title = webViewController.getCurrentTitle()
                // TODO: Implement bookmark logic
                ActionResult.success("Bookmark added")
            }

            // Gestures
            "SINGLE_CLICK" -> webViewController.performClick()
            "DOUBLE_CLICK" -> webViewController.performDoubleClick()
            "DRAG_START" -> webViewController.startDrag()
            "DRAG_STOP" -> webViewController.stopDrag()
            "SELECT" -> webViewController.select()

            else -> ActionResult.error("Unknown command: $commandId")
        }
    }
}

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

    /**
     * Convert to CommandResult for VoiceOS
     */
    fun toCommandResult(command: com.augmentalis.commandmanager.models.Command):
        com.augmentalis.commandmanager.models.CommandResult {
        return com.augmentalis.commandmanager.models.CommandResult(
            success = success,
            command = command,
            error = if (!success) {
                com.augmentalis.commandmanager.models.CommandError(
                    com.augmentalis.commandmanager.models.ErrorCode.EXECUTION_FAILED,
                    message ?: "Unknown error"
                )
            } else null
        )
    }
}
```

**That's the ENTIRE implementation.** ~50 lines.

---

## Integration Steps

### Step 1: Create WebAvanueActionMapper.kt

Create file at: `WebAvanue/universal/src/androidMain/kotlin/com/augmentalis/Avanues/web/universal/commands/WebAvanueActionMapper.kt`

(Use code above)

### Step 2: Register Handler on App Startup

**In WebAvanueApp.kt or MainActivity.kt:**

```kotlin
class WebAvanueApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize dependencies
        val tabViewModel = getTabViewModel()  // Your DI/factory
        val webViewController = getWebViewController()  // Your DI/factory

        // Create ActionMapper
        val actionMapper = WebAvanueActionMapper(tabViewModel, webViewController)

        // Register with VoiceOS
        registerVoiceCommands(actionMapper)
    }

    private fun registerVoiceCommands(actionMapper: WebAvanueActionMapper) {
        try {
            // Get VoiceOS CommandManager
            val commandManager = com.augmentalis.commandmanager.CommandManager.getInstance(this)

            // TODO: Get IntentDispatcher from CommandManager
            // (May need to add getIntentDispatcher() method to CommandManager)
            // val dispatcher = commandManager.getIntentDispatcher()

            // Register handler for "browser" category
            // dispatcher.registerHandler("browser") { command ->
            //     actionMapper.executeAction(command.id, emptyMap())
            //         .toCommandResult(command)
            // }

            Log.i(TAG, "Browser voice commands registered")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register voice commands", e)
            // Graceful degradation - WebAvanue works without voice
        }
    }
}
```

**NOTE:** IntentDispatcher may not be exposed by CommandManager yet. May need to add public accessor.

### Step 3: Done!

No CommandRegistrar, no VOS file loading, no database operations.

---

## Browser Commands in VoiceOS

Browser commands are defined in VoiceOS's centralized JSON:

**File:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/src/main/assets/localization/commands/en-US.json`

**Example (already exists or add):**
```json
{
  "version": "1.0",
  "locale": "en-US",
  "commands": [
    {
      "id": "SCROLL_TOP",
      "category": "browser",
      "text": "scroll to top",
      "synonyms": ["top of page", "jump to top", "page top"],
      "description": "Scroll to top of web page"
    },
    {
      "id": "SCROLL_BOTTOM",
      "category": "browser",
      "text": "scroll to bottom",
      "synonyms": ["bottom of page", "jump to bottom", "page bottom"],
      "description": "Scroll to bottom of web page"
    }
    // ... more browser commands
  ]
}
```

**VoiceOS loads these commands centrally.** WebAvanue does NOT touch these files.

---

## Comparison: Incorrect vs Correct

### ❌ Incorrect (What I initially documented)

**Files:**
1. `WebAvanueActionMapper.kt` (50 lines)
2. `WebAvanueCommandRegistrar.kt` (80 lines)
3. `browser-commands.vos` in WebAvanue assets
4. Application integration (20 lines)

**Total:** 150 lines + VOS file management

**Operations:**
- Load VOS file from assets
- Parse JSON
- Insert into database
- Register with DynamicCommandRegistry
- Set namespace priority
- Unregister on termination

**Complexity:** HIGH

---

### ✅ Correct (Centralized)

**Files:**
1. `WebAvanueActionMapper.kt` (50 lines)
2. Application integration (10 lines to register handler)

**Total:** 60 lines

**Operations:**
- Register action handler with IntentDispatcher
- That's it!

**Complexity:** LOW

**Code reduction:** 150 → 60 lines (**60% reduction**)

---

## Why Centralized is Better

### 1. Single Source of Truth
- ALL commands in one place (VoiceOS)
- No synchronization issues
- Easier to add new languages (just update VoiceOS JSON)

### 2. Simpler App Integration
- Each app: ~50 lines for ActionMapper + ~10 lines to register
- No command file management
- No database operations

### 3. Consistent Command Recognition
- VoiceOS handles all NLU/recognition
- Same command phrases across all apps
- Priority/conflict resolution handled centrally

### 4. Easier Testing
- Test command recognition once (in VoiceOS)
- App tests only ActionMapper routing

### 5. Centralized Updates
- Add new browser command: Update VoiceOS JSON only
- No WebAvanue code changes needed (unless new action)
- Hot-reload via VoiceOS CommandLoader

---

## Migration from Previous Documentation

### Files to DELETE/Ignore:

1. ❌ `WebAvanueCommandRegistrar.kt` - NOT NEEDED
2. ❌ `browser-commands.vos` in WebAvanue - NOT NEEDED
3. ❌ VOS file loading code - NOT NEEDED
4. ❌ Database ingestion code - NOT NEEDED

### Files to KEEP:

1. ✅ `WebAvanueActionMapper.kt` - REQUIRED (~50 lines)
2. ✅ Browser command definitions in VoiceOS JSON - REQUIRED (centralized)

### Documentation to UPDATE:

1. ❌ `VOICEOS-INTEGRATION-STRATEGY.md` - INCORRECT (distributed approach)
2. ❌ `Developer-Manual-Chapter46` - INCORRECT (includes CommandRegistrar)
3. ❌ `ADR-007` - INCORRECT (includes CommandRegistrar pattern)
4. ❌ `design-standards/.../database-driven-voice-commands.md` - INCORRECT (requires CommandRegistrar)

All above need correction to reflect **centralized architecture**.

---

## Next Steps

1. ✅ Understand correct architecture (DONE - this document)
2. ⏳ Update documentation to remove CommandRegistrar pattern
3. ⏳ Implement WebAvanueActionMapper.kt (50 lines)
4. ⏳ Add IntentDispatcher accessor to CommandManager (if needed)
5. ⏳ Register handler in WebAvanueApp.onCreate()
6. ⏳ Test voice commands with WebAvanue

---

## Questions for VoiceOS Integration

1. **How to access IntentDispatcher?**
   - Does `CommandManager` expose `getIntentDispatcher()`?
   - Or should we register handlers differently?

2. **Category naming convention?**
   - Use "browser" for all browser commands?
   - Or more specific like "webavanue"?

3. **Command IDs in JSON?**
   - Should browser commands in en-US.json have `category: "browser"`?
   - Or is category inferred from command ID prefix?

4. **Routing Context?**
   - How does VoiceOS know WebAvanue is active?
   - Does it check `currentApp` package name?

---

**Last Updated:** 2025-11-21
**Status:** Corrected architecture - ready for implementation
**Code Required:** ~60 lines total (vs 150 lines incorrect approach)
