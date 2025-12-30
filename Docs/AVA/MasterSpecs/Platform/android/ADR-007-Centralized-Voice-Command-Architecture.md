# ADR-007: Centralized Voice Command Architecture

**Status:** Accepted (Revised)
**Date:** 2025-11-21
**Authors:** AVA AI Team
**Related:** ADR-006 (VoiceOS Command Delegation)
**Supersedes:** Previous version with distributed CommandRegistrar pattern

---

## Context

After implementing browser voice command integration for WebAvanue, we discovered a critical architectural question: **Should each app load its own voice commands, or should VoiceOS load all commands centrally?**

### Problem Statement

**User Requirement:**
> "Create voice command integration for WebAvanue browser with browser-specific commands (scroll, zoom, navigation, tabs, etc.)"

**Initial Approach #1 - Static Handler (REJECTED):**
```kotlin
class WebAvanueCommandHandler : CommandHandler {
    override fun handleCommand(command: Command): CommandResult {
        return when (command.action) {
            "SCROLL_UP" -> webViewController.scrollUp()
            // ... 27 more hardcoded cases
        }
    }
}
```
**Problems:** No runtime updates, no localization, tight coupling

**Initial Approach #2 - Distributed Database (REJECTED):**
```kotlin
// Each app loads its own commands
class WebAvanueCommandRegistrar {
    suspend fun registerCommands() {
        ingestion.ingestVOSFile("commands/vos/browser-commands.vos")
        registry.registerActionHandler("com.augmentalis.webavanue") { ... }
    }
}
```
**Problems:** Duplicate loading logic, commands scattered across apps, synchronization issues

**Final Clarification:**
> "Why does WebAvanue need CommandRegistrar if it's linked to the VoiceOS CommandManager system?"

This revealed the correct architecture: **VoiceOS loads ALL commands centrally**.

---

## Decision

**VoiceOS CommandManager loads ALL voice commands centrally. Apps only implement ActionMappers to execute commands.**

### Centralized Architecture

**VoiceOS (System-wide):**
- ✅ Loads ALL commands from `assets/localization/commands/{locale}.json`
- ✅ Includes system, browser, app-specific commands
- ✅ Handles command recognition via CommandLoader
- ✅ Routes commands to apps via IntentDispatcher
- ✅ Single source of truth for all voice commands

**Apps (e.g., WebAvanue):**
- ✅ Register action handler with IntentDispatcher
- ✅ Implement ActionMapper to execute app-specific actions
- ❌ Do NOT load commands
- ❌ Do NOT manage VOS files
- ❌ Do NOT interact with VoiceCommandDao directly

### Data Flow

```
User voice input: "scroll to top"
    ↓
VoiceOS Speech Recognition
    ↓
VoiceOS CommandManager
    ↓
CommandLoader (already loaded commands from JSON at startup)
    ↓
Command recognized: id="SCROLL_TOP", category="browser"
    ↓
IntentDispatcher.routeCommand(command, RoutingContext(currentApp="com.augmentalis.webavanue"))
    ↓
Finds registered handler for "browser" category
    ↓
Calls: browserHandler(command)
    ↓
WebAvanueActionMapper.executeAction("SCROLL_TOP")
    ↓
webViewController.scrollTop()
```

---

## Rationale

### Why Centralized is Superior

#### 1. Single Source of Truth

**Without Centralization (Distributed):**
```
VoiceOS/assets/commands/en-US.json     → "scroll to top"
WebAvanue/assets/commands/browser.vos  → "scroll to top"
BrowserLite/assets/commands/lite.vos   → "scroll to top"

Problem: 3 copies of same command → sync nightmare
```

**With Centralization:**
```
VoiceOS/assets/commands/en-US.json     → "scroll to top"

All browser apps use same command definition
```

#### 2. Follows OS Architecture Principles

**VoiceOS = Voice Layer (analogous to Android Framework)**
- Android Framework: Touch gestures, lifecycle, permissions
- VoiceOS: Voice commands, speech recognition, command routing

**Apps = Application Layer**
- Apps don't duplicate Android touch gestures
- Apps don't duplicate VoiceOS voice commands
- Apps only implement **what to do** when command received

#### 3. Deployment Independence

**Command Updates:**
```
❌ Distributed: Add Spanish commands → Update 15 apps
✅ Centralized: Add Spanish commands → Update VoiceOS only
```

**Bug Fixes:**
```
❌ Distributed: Fix command typo → Update 15 apps
✅ Centralized: Fix command typo → Update VoiceOS only
```

#### 4. Code Reduction

**Per-App Code:**
```
❌ Distributed Approach:
- ActionMapper: 50 lines
- CommandRegistrar: 80 lines
- VOS file management: 20 lines
- Total: 150 lines per app

✅ Centralized Approach:
- ActionMapper: 50 lines
- Handler registration: 10 lines
- Total: 60 lines per app

Reduction: 60% less code per app
```

**Ecosystem-wide:**
```
15 apps × 90 lines savings = 1,350 lines saved
```

#### 5. Multi-App Command Sharing

**Scenario:** User has 3 browser apps
- WebAvanue
- WebAvanue Lite
- Enterprise Browser

**Centralized:** All use same "scroll to top" command (defined once in VoiceOS)
**Distributed:** Each defines own version → potential conflicts, inconsistency

#### 6. Graceful App Uninstall

```
User uninstalls WebAvanue
    ↓
❌ Distributed: Browser voice commands disappear (were in WebAvanue)
✅ Centralized: Browser voice commands still available (in VoiceOS)
    → Other browser apps can still use them
```

---

## Implementation

### VoiceOS Changes (Already Implemented)

**CommandLoader loads all commands at startup:**

**File:** `CommandManager/src/main/java/com/augmentalis/commandmanager/loader/CommandLoader.kt`

```kotlin
class CommandLoader(
    private val context: Context,
    private val commandDao: VoiceCommandDao
) {
    suspend fun initializeCommands(): LoadResult {
        // Load English fallback
        loadLocale("en-US", isFallback = true)

        // Load user's system locale
        loadLocale(getSystemLocale(), isFallback = false)

        // Loads from: assets/localization/commands/{locale}.json
        // This includes: system, browser, app commands
    }
}
```

**Commands JSON Structure:**

**File:** `CommandManager/src/main/assets/localization/commands/en-US.json`

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
      "synonyms": ["bottom of page", "jump to bottom"],
      "description": "Scroll to bottom of web page"
    },
    {
      "id": "ZOOM_IN",
      "category": "browser",
      "text": "zoom in",
      "synonyms": ["enlarge", "magnify", "bigger"],
      "description": "Zoom in on web page"
    }
    // ... all browser commands defined here
  ]
}
```

**IntentDispatcher routes commands:**

**File:** `CommandManager/src/main/java/com/augmentalis/commandmanager/routing/IntentDispatcher.kt`

```kotlin
class IntentDispatcher(private val context: Context) {
    private val actionHandlers = mutableMapOf<String, suspend (Command) -> CommandResult>()

    fun registerHandler(category: String, handler: suspend (Command) -> CommandResult) {
        actionHandlers[category] = handler
    }

    suspend fun routeCommand(command: Command, routingContext: RoutingContext): CommandResult {
        val candidates = findCandidateHandlers(command, routingContext)
        // Routes based on category, confidence scoring, context
    }
}
```

### App Changes (Per-App Implementation)

**Required:** ~60 lines of code per app

#### 1. Create ActionMapper

**File:** `WebAvanue/universal/src/androidMain/kotlin/.../commands/WebAvanueActionMapper.kt`

```kotlin
package com.augmentalis.Avanues.web.universal.commands

class WebAvanueActionMapper(
    private val tabViewModel: TabViewModel,
    private val webViewController: WebViewController
) {
    suspend fun executeAction(
        commandId: String,
        parameters: Map<String, Any> = emptyMap()
    ): ActionResult {
        return when (commandId) {
            "SCROLL_TOP" -> webViewController.scrollTop()
            "SCROLL_BOTTOM" -> webViewController.scrollBottom()
            "ZOOM_IN" -> webViewController.zoomIn()
            "ZOOM_OUT" -> webViewController.zoomOut()
            "NEW_TAB" -> {
                tabViewModel.createTab()
                ActionResult.success("New tab created")
            }
            "CLOSE_TAB" -> {
                tabViewModel.closeActiveTab()
                ActionResult.success("Tab closed")
            }
            else -> ActionResult.error("Unknown command: $commandId")
        }
    }
}

data class ActionResult(
    val success: Boolean,
    val message: String? = null
) {
    companion object {
        fun success(msg: String? = null) = ActionResult(true, msg)
        fun error(msg: String) = ActionResult(false, msg)
    }
}
```

**Lines:** ~50

#### 2. Register Handler on App Startup

**File:** `WebAvanue/app/src/main/kotlin/.../WebAvanueApp.kt`

```kotlin
class WebAvanueApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize dependencies
        val tabViewModel = getTabViewModel()
        val webViewController = getWebViewController()

        // Create ActionMapper
        val actionMapper = WebAvanueActionMapper(tabViewModel, webViewController)

        // Register with VoiceOS
        registerVoiceCommands(actionMapper)
    }

    private fun registerVoiceCommands(actionMapper: WebAvanueActionMapper) {
        try {
            val commandManager = CommandManager.getInstance(this)
            val dispatcher = commandManager.getIntentDispatcher()

            dispatcher.registerHandler("browser") { command ->
                val result = actionMapper.executeAction(command.id, emptyMap())
                CommandResult(
                    success = result.success,
                    command = command,
                    error = if (!result.success) {
                        CommandError(ErrorCode.EXECUTION_FAILED, result.message ?: "")
                    } else null
                )
            }

            Log.i(TAG, "Browser voice commands registered")
        } catch (e: Exception) {
            Log.e(TAG, "VoiceOS not available - voice disabled", e)
            // Graceful degradation - WebAvanue works without voice
        }
    }
}
```

**Lines:** ~30 (includes error handling)

**Total per app:** ~80 lines (ActionMapper + registration)

---

## Alternatives Considered

### Alternative 1: Static Command Handler (REJECTED)

**Code:**
```kotlin
class WebAvanueCommandHandler {
    fun handleCommand(action: String) = when (action) {
        "SCROLL_UP" -> scrollUp()
        // ... 27 more hardcoded cases
    }
}
```

**Pros:**
- ✅ Simple to understand
- ✅ No external dependencies

**Cons:**
- ❌ No runtime updates
- ❌ No localization
- ❌ Requires recompilation for changes
- ❌ No usage analytics
- ❌ Tight coupling

**Rejected:** Doesn't meet voice-first OS requirements

### Alternative 2: Distributed Database (REJECTED)

**Code:**
```kotlin
// Each app loads own commands
class WebAvanueCommandRegistrar {
    suspend fun registerCommands() {
        ingestion.ingestVOSFile("browser-commands.vos")
        registry.registerActionHandler("com.augmentalis.webavanue") { ... }
    }
}
```

**Pros:**
- ✅ Runtime updates
- ✅ Multi-locale support
- ✅ Apps are self-contained

**Cons:**
- ❌ Duplicate command definitions across apps
- ❌ Synchronization issues (browser commands in 3 apps)
- ❌ More code per app (150 lines vs 60 lines)
- ❌ Command updates require app updates
- ❌ Violates single source of truth

**Rejected:** User clarified VoiceOS should load commands centrally

### Alternative 3: Centralized Loading (CHOSEN) ⭐

**Code:**
```kotlin
// VoiceOS loads all commands
CommandLoader.initializeCommands() // Loads browser, system, app commands

// Apps only register handlers
dispatcher.registerHandler("browser") { command ->
    actionMapper.executeAction(command.id)
}
```

**Pros:**
- ✅ Single source of truth (VoiceOS)
- ✅ Runtime updates (update VoiceOS only)
- ✅ Multi-locale support (in VoiceOS)
- ✅ Minimal app code (60 lines vs 150 lines)
- ✅ Command sharing across apps
- ✅ Follows OS architecture principles
- ✅ Graceful app uninstall
- ✅ Deployment independence

**Cons:**
- ⚠️ VoiceOS dependency (already exists for speech recognition)
- ⚠️ Must expose IntentDispatcher API (easy to add)

**Chosen:** Aligns with voice-first OS architecture, minimal cons

---

## Consequences

### Positive

✅ **60% Code Reduction Per App**
- Distributed: 150 lines
- Centralized: 60 lines
- Savings: 90 lines per app

✅ **Single Source of Truth**
- All commands in VoiceOS
- No synchronization issues
- Easy to audit/modify

✅ **Deployment Independence**
- Add/modify commands → Update VoiceOS only
- No app rebuilds required
- Faster iteration cycles

✅ **Multi-App Support**
- WebAvanue, WebAvanue Lite, Enterprise Browser
- All use same browser commands
- Consistent UX across apps

✅ **Easier Localization**
- Add Spanish → Update VoiceOS JSON only
- All apps get new locale automatically
- No app code changes

✅ **Graceful Degradation**
- Apps work without VoiceOS (touch/keyboard)
- Clear error handling
- No hard dependency on voice

✅ **Follows Android Patterns**
- VoiceOS = Framework layer
- Apps = Application layer
- Standard OS architecture

### Negative

⚠️ **VoiceOS Dependency**
- Apps require VoiceOS for voice functionality
- Mitigation: Already required for speech recognition
- Graceful fallback if VoiceOS unavailable

⚠️ **IntentDispatcher API Exposure**
- CommandManager must expose getIntentDispatcher()
- Mitigation: Simple public method addition
- Low effort change

### Neutral

◼️ **Centralized Control**
- VoiceOS controls all voice commands
- This is by design for voice-first OS

---

## Migration Path

### Phase 1: ✅ COMPLETE (VoiceOS Infrastructure)
- CommandLoader implementation
- IntentDispatcher implementation
- Database schema (VoiceCommandEntity)
- VoiceCommandDao

### Phase 2: 🔄 IN PROGRESS (WebAvanue Integration)
- Create WebAvanueActionMapper.kt
- Add IntentDispatcher accessor to CommandManager
- Register handler in WebAvanueApp.onCreate()
- Test voice commands

### Phase 3: TODO (Other Apps)
- NewAvanue Launcher
- Email client
- Calendar app
- File manager

### Phase 4: TODO (Documentation Cleanup)
- Remove CommandRegistrar references
- Update examples to show centralized pattern
- Create migration guide for other teams

---

## File Structure

### VoiceOS (System Commands)

```
VoiceOS/
└── managers/CommandManager/
    ├── src/main/assets/localization/commands/
    │   ├── en-US.json          ← ALL commands (system + browser)
    │   ├── es-ES.json          ← Spanish commands
    │   └── fr-FR.json          ← French commands
    └── src/main/java/.../
        ├── CommandManager.kt
        ├── loader/CommandLoader.kt
        └── routing/IntentDispatcher.kt
```

### WebAvanue (App Implementation)

```
WebAvanue/
└── universal/src/androidMain/
    └── .../commands/
        └── WebAvanueActionMapper.kt    ← ~50 lines

(NO VOS files, NO CommandRegistrar, NO command definitions)
```

---

## Testing Strategy

### VoiceOS Tests (System-level)

**Test command loading:**
```kotlin
@Test
fun `CommandLoader loads browser commands`() = runTest {
    val loader = CommandLoader.create(context)
    val result = loader.initializeCommands()

    assertTrue(result is LoadResult.Success)
    val commands = commandDao.getCommandsForCategory("browser")
    assertTrue(commands.size >= 13) // Browser commands loaded
}
```

**Test command routing:**
```kotlin
@Test
fun `IntentDispatcher routes browser commands correctly`() = runTest {
    val dispatcher = IntentDispatcher(context)
    var receivedCommand: Command? = null

    dispatcher.registerHandler("browser") { command ->
        receivedCommand = command
        CommandResult(success = true, command = command)
    }

    val command = Command(id = "SCROLL_TOP", text = "scroll to top")
    val result = dispatcher.routeCommand(
        command,
        RoutingContext(currentApp = "com.augmentalis.webavanue")
    )

    assertTrue(result.success)
    assertEquals("SCROLL_TOP", receivedCommand?.id)
}
```

### WebAvanue Tests (App-level)

**Test ActionMapper:**
```kotlin
@Test
fun `ActionMapper executes SCROLL_TOP correctly`() = runTest {
    val actionMapper = WebAvanueActionMapper(mockTabViewModel, mockWebViewController)
    val result = actionMapper.executeAction("SCROLL_TOP")

    verify(mockWebViewController).scrollTop()
    assertTrue(result.success)
}

@Test
fun `ActionMapper handles unknown commands`() = runTest {
    val actionMapper = WebAvanueActionMapper(mockTabViewModel, mockWebViewController)
    val result = actionMapper.executeAction("UNKNOWN_COMMAND")

    assertFalse(result.success)
    assertTrue(result.message?.contains("Unknown command") == true)
}
```

---

## Performance Considerations

### Centralized Loading Overhead

**Startup Time:**
```
Load ALL commands (system + browser + apps): ~50-100ms (first launch)
Database cached: ~5ms (subsequent launches)

Impact: Negligible - happens once at VoiceOS startup
```

**Memory Usage:**
```
100 commands × 200 bytes = 20 KB
5 locales × 20 KB = 100 KB

Impact: Negligible on modern Android devices
```

### Command Routing Overhead

**Per-Command:**
```
Database query: ~5ms
IntentDispatcher routing: ~1ms
Total: ~6ms

Voice command execution: 500ms - 5s
Overhead: 0.1-1% (imperceptible)
```

---

## Security Considerations

### Command Access Control

**IntentDispatcher validates:**
- App package name (RoutingContext.currentApp)
- Category permissions
- Command eligibility

**Example:**
```kotlin
// Only browser apps can execute browser commands
if (command.category == "browser" && !isBrowserApp(routingContext.currentApp)) {
    return CommandResult.error("Unauthorized")
}
```

### Centralized Audit

**Benefits:**
- All voice commands in one place (VoiceOS)
- Easy to audit for security issues
- Single update point for security patches

---

## References

- **ADR-006:** VoiceOS Command Delegation Pattern
- **CommandManager:** `/Volumes/M-Drive/Coding/Avanues/android/apps/voiceos/managers/CommandManager/`
- **IntentDispatcher:** Context-aware command routing
- **CommandLoader:** JSON-based command loading
- **CORRECTED-VOICEOS-INTEGRATION.md:** Detailed integration guide

---

## Decision Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2025-11-21 | Rejected static handler | No runtime updates, tight coupling |
| 2025-11-21 | Initially chose distributed database | Thought each app loads own commands |
| 2025-11-21 | User questioned: "Why CommandRegistrar?" | Clarified VoiceOS loads centrally |
| 2025-11-21 | **Finalized: Centralized architecture** | Single source of truth, 60% code reduction |

---

**Status:** Accepted (Revised)
**Implementation:** In Progress (WebAvanue)
**Estimated Time:** 2 hours (ActionMapper + registration)
**Code Required:** ~60 lines per app
