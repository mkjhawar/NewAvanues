# CommandManager → VoiceOSService Integration - Implementation Instructions

**Document Type:** Implementation Instructions (User → Developer)
**Created:** 2025-10-10 17:34 PDT
**Target:** CommandManager Integration with VoiceOSService
**Based On:** Q&A Session (12 questions, all answered)
**Status:** Ready for Implementation

---

## CRITICAL: Read This First

**VoiceCursor Refactoring is BLOCKING.** Do NOT start other implementation work until VoiceCursor deep dive and refactoring is complete. This is the critical path.

**Implementation Order:**
1. VoiceCursor deep dive and refactoring (Q5 - BLOCKS CursorActions)
2. Core infrastructure (Service Monitor, Caching, Routing)
3. Action types (8 new action classes)
4. Intelligence layer (Learning, Context, Macros)
5. Extensibility (Plugin System)
6. Testing (500 tests, 80% coverage)

**Timeline:** 4-6 weeks (1 developer full-time)

---

## Phase 0: VoiceCursor Deep Dive & Refactoring (CRITICAL PATH) ⚠️

### Task 0.1: Audit VoiceCursor Architecture

**Objective:** Understand current VoiceCursor implementation before refactoring.

**Actions:**
1. Read all files in `/modules/apps/VoiceCursor/` directory
2. Identify all command handling logic (likely `onVoiceCommand()`, `handleCommand()`, etc.)
3. Map dependencies:
   - What does VoiceCursor need from VoiceOSService?
   - What accessibility events does it handle?
   - What permissions does it require?
4. Document current architecture:
   - Create diagram showing current flow
   - List all public methods/APIs
   - Identify tangled concerns (command logic mixed with cursor mechanics)

**Deliverable:**
- `/docs/modules/VoiceCursor/architecture/Current-Architecture-Analysis-YYMMDD-HHMM.md`
- Include findings, diagrams, and refactoring recommendations

### Task 0.2: Design VoiceCursor API (Separation of Concerns)

**Objective:** Define clean VoiceCursor API that ONLY handles cursor mechanics, NOT command parsing.

**Design Principle:**
- VoiceCursor = Pure cursor service (mechanics + UI)
- CommandManager/CursorActions = Command handling (voice logic)

**Required API Methods:**
```kotlin
interface VoiceCursorAPI {
    // Lifecycle
    fun initialize(context: Context, serviceConnection: AccessibilityService)
    fun shutdown()

    // Cursor visibility
    fun showCursor(): CursorResult
    fun hideCursor(): CursorResult
    fun isCursorVisible(): Boolean

    // Cursor movement
    fun moveTo(x: Int, y: Int): CursorResult
    fun moveByOffset(dx: Int, dy: Int): CursorResult
    fun snapToElement(nodeInfo: AccessibilityNodeInfo): CursorResult

    // Cursor interaction
    fun clickAtCursor(): CursorResult
    fun longClickAtCursor(): CursorResult
    fun doubleClickAtCursor(): CursorResult

    // Cursor state
    fun getCursorPosition(): Point
    fun getCursorState(): CursorState

    // Customization (Q5 Enhancements)
    fun setCursorTheme(theme: CursorTheme): CursorResult  // Enhancement 2
    fun enableMultiCursor(count: Int): CursorResult       // Enhancement 1
}

data class CursorResult(
    val success: Boolean,
    val message: String? = null,
    val error: Throwable? = null
)

enum class CursorState {
    HIDDEN, VISIBLE, MOVING, CLICKING
}
```

**Actions:**
1. Create `VoiceCursorAPI.kt` interface in `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voicecursor/api/`
2. Define all required methods (see above)
3. Add comprehensive KDoc documentation
4. Get user approval for API design before proceeding

**Deliverable:**
- `/modules/apps/VoiceCursor/src/main/java/com/augmentalis/voicecursor/api/VoiceCursorAPI.kt`
- Updated architecture documentation

### Task 0.3: Extract Command Logic to CommandManager

**Objective:** Move all voice command handling from VoiceCursor to CommandManager/CursorActions.

**Actions:**
1. Create `CursorActions.kt` in `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/`
2. Identify all voice commands currently in VoiceCursor (e.g., "show cursor", "hide cursor", "click", etc.)
3. Move command recognition logic to CursorActions
4. CursorActions should:
   - Receive voice command text
   - Parse intent
   - Call VoiceCursorAPI methods
   - Announce results to user (TTS/audio feedback)
   - Track telemetry (Q2 Enhancement 4)

**Example Implementation:**
```kotlin
class CursorActions(
    private val context: Context,
    private val cursorAPI: VoiceCursorAPI,
    private val tts: TextToSpeech
) : BaseAction() {

    override val category = "cursor"
    override val supportedCommands = listOf(
        "show cursor", "hide cursor", "click", "move up",
        "move down", "move left", "move right", "snap to button"
    )

    override suspend fun execute(command: VoiceCommand): ActionResult {
        return when (command.primaryText.lowercase()) {
            "show cursor" -> handleShowCursor()
            "hide cursor" -> handleHideCursor()
            "click" -> handleClick()
            "move up" -> handleMove(Direction.UP)
            // ... etc
            else -> ActionResult.error("Unknown cursor command")
        }
    }

    private suspend fun handleShowCursor(): ActionResult {
        // VOICE-SPECIFIC LOGIC (belongs in CommandManager)
        announceToUser("Activating cursor")

        // DELEGATE TO VOICECURSOR (pure mechanics)
        val result = cursorAPI.showCursor()

        // VOICE-SPECIFIC LOGIC (belongs in CommandManager)
        if (result.success) {
            announceToUser("Cursor activated")
            telemetry.trackCursorCommand("show_cursor", success = true)
            return ActionResult.success("Cursor shown")
        } else {
            announceToUser("Failed to activate cursor")
            telemetry.trackCursorCommand("show_cursor", success = false)
            return ActionResult.error(result.message ?: "Unknown error")
        }
    }

    private suspend fun handleMove(direction: Direction): ActionResult {
        val offset = getMovementOffset(direction)  // e.g., (0, -50) for UP
        val result = cursorAPI.moveByOffset(offset.x, offset.y)

        if (result.success) {
            // No announcement for movement (too chatty)
            return ActionResult.success()
        } else {
            announceToUser("Cannot move cursor ${direction.name.lowercase()}")
            return ActionResult.error(result.message ?: "Movement failed")
        }
    }
}
```

**Actions:**
4. Remove command handling logic from VoiceCursor
5. Ensure VoiceCursor ONLY implements VoiceCursorAPI (pure mechanics)
6. Test both modules independently:
   - VoiceCursor: Can show/hide/move cursor via API calls
   - CursorActions: Can parse voice commands and call API

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/CursorActions.kt`
- Refactored VoiceCursor (API implementation only)
- Updated tests for both modules

### Task 0.4: Integration Testing

**Objective:** Verify VoiceCursor refactoring works end-to-end.

**Test Cases:**
1. Voice command "show cursor" → CursorActions calls API → Cursor appears
2. Voice command "click" → CursorActions calls API → Click performed
3. Voice command "hide cursor" → CursorActions calls API → Cursor disappears
4. API can be called directly (not just via voice) - proves separation of concerns

**Deliverable:**
- `/modules/managers/CommandManager/src/test/java/com/augmentalis/commandmanager/actions/CursorActionsTest.kt`
- Integration test passing before proceeding to Phase 1

---

## Phase 1: Core Infrastructure (Weeks 1-2)

### Task 1.1: Service Monitor Implementation (Q1)

**Objective:** Implement service lifetime management with reconnection callbacks.

**Reference Decision:** Q1 - Option D (Service Monitor with Reconnection Callback)

**Actions:**
1. Create `ServiceMonitor.kt` in `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/monitor/`

2. Implement health monitoring:
```kotlin
class ServiceMonitor(private val service: VoiceOSService) {
    private var commandManager: CommandManager? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    fun bindCommandManager(manager: CommandManager) {
        commandManager = manager
        manager.setServiceCallback(object : ServiceCallback {
            override fun onServiceBound() {
                Log.i(TAG, "CommandManager bound to VoiceOSService")
                // Q1 Enhancement 3: Update connection state UI
                updateConnectionStateUI(ConnectionState.CONNECTED)
            }

            override fun onServiceDisconnected() {
                Log.w(TAG, "CommandManager disconnected from VoiceOSService")
                updateConnectionStateUI(ConnectionState.DISCONNECTED)
                // Attempt reconnection
                scheduleReconnection()
            }
        })
    }

    // Q1 Enhancement 1: Service Health Monitoring
    fun startHealthCheck() {
        scope.launch {
            while (isActive) {
                delay(30_000)  // Check every 30s
                val isHealthy = commandManager?.healthCheck() ?: false
                if (!isHealthy) {
                    Log.w(TAG, "CommandManager health check failed")
                    // Q1 Enhancement 4: Lifecycle logging
                    logLifecycleEvent("health_check_failed")
                    attemptRecovery()
                }
            }
        }
    }

    // Q1 Enhancement 2: Graceful Degradation
    private suspend fun attemptRecovery() {
        // Try to restore CommandManager
        try {
            commandManager?.restart()
            updateConnectionStateUI(ConnectionState.RECOVERING)
        } catch (e: Exception) {
            Log.e(TAG, "Recovery failed, falling back to basic commands", e)
            updateConnectionStateUI(ConnectionState.DEGRADED)
            // Enable fallback mode (basic commands only)
            service.enableFallbackMode()
        }
    }
}
```

3. Update VoiceOSService:
```kotlin
class VoiceOSService : AccessibilityService() {
    private lateinit var commandManager: CommandManager
    private lateinit var serviceMonitor: ServiceMonitor

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Initialize CommandManager
        commandManager = CommandManager(this)

        // Initialize ServiceMonitor
        serviceMonitor = ServiceMonitor(this)
        serviceMonitor.bindCommandManager(commandManager)
        serviceMonitor.startHealthCheck()

        Log.i(TAG, "VoiceOSService connected with CommandManager")
    }

    // Q1: Handle voice commands from speech recognition
    fun handleVoiceCommand(recognizedText: String) {
        lifecycleScope.launch {
            val result = commandManager.executeCommand(recognizedText)
            when (result) {
                is ActionResult.Success -> Log.i(TAG, "Command executed: $recognizedText")
                is ActionResult.Error -> Log.e(TAG, "Command failed: ${result.message}")
            }
        }
    }
}
```

4. **Q1 Enhancement 3:** Connection State UI
   - Create notification showing service status
   - Green: Connected, Yellow: Recovering, Red: Degraded

5. **Q1 Enhancement 4:** Lifecycle Logging
   - Log all lifecycle events to file for debugging
   - Exportable logs for support tickets

6. **Q1 Enhancement 5:** Configuration Persistence
   - Save CommandManager state to SharedPreferences
   - Restore state on service reconnection

**Deliverables:**
- `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/monitor/ServiceMonitor.kt`
- Updated `/modules/apps/VoiceAccessibility/src/main/java/com/augmentalis/voiceos/accessibility/VoiceOSService.kt`
- Connection state notification UI
- Lifecycle logging infrastructure
- Configuration persistence

**Test Coverage:** Unit tests for ServiceMonitor, integration tests for VoiceOSService ↔ CommandManager

---

### Task 1.2: Tiered Caching Implementation (Q3)

**Objective:** Implement 3-tier caching for <100ms command resolution.

**Reference Decision:** Q3 - Option D (Tiered Caching: Tier 1/2/3)

**Actions:**
1. Create `CommandCache.kt` in `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/cache/`

2. Implement 3-tier cache:
```kotlin
class CommandCache(private val database: VoiceCommandDao) {
    // Tier 1: Preloaded top 20 commands (~10KB, <0.5ms)
    private val tier1Cache: Map<String, VoiceCommand> = loadTop20Commands()

    // Tier 2: LRU cache for recently used (max 50, ~25KB, <0.5ms)
    private val tier2LRUCache = LRUCache<String, VoiceCommand>(50)

    // Tier 3: Database fallback (5-15ms)
    private val databaseCache = database

    suspend fun resolveCommand(text: String, context: String?): VoiceCommand? {
        // Tier 1: Check preloaded cache (instant)
        tier1Cache[text]?.let {
            trackCacheHit(CacheTier.TIER_1)
            return it
        }

        // Tier 2: Check LRU cache (fast)
        tier2LRUCache[text]?.let {
            trackCacheHit(CacheTier.TIER_2)
            return it
        }

        // Tier 3: Query database (slower but acceptable)
        val command = database.queryCommand(text, context)
        command?.let {
            trackCacheHit(CacheTier.TIER_3)
            tier2LRUCache.put(text, it)  // Promote to Tier 2
        }

        return command
    }

    // Load only English + device locale (Q3 user clarification)
    private fun loadTop20Commands(): Map<String, VoiceCommand> {
        val deviceLocale = Locale.getDefault().toLanguageTag()
        val locales = setOf("en-US", deviceLocale)

        return database.getTopCommandsByLocale(locales, limit = 20)
            .associateBy { it.primaryText }
    }
}
```

3. **Q3 Enhancement 1 (STUB):** Predictive Preloading
```kotlin
// TODO: Implement predictive preloading based on user patterns
// See Q11 learning system for usage data
// Preload commands likely to be used next based on context
fun predictivePreload(context: String) {
    // STUB: Add to master TODO/backlog
    TODO("Implement predictive preloading - see Q3 Enhancement 1")
}
```

4. **Q3 Enhancement 2 (STUB):** Cache Warming
```kotlin
// TODO: Warm cache on service start with user's frequent commands
fun warmCache() {
    // STUB: Add to master TODO/backlog
    TODO("Implement cache warming - see Q3 Enhancement 2")
}
```

5. **Q3 Enhancement 3 (STUB):** Memory Pressure Monitoring
```kotlin
// TODO: Monitor memory usage and adjust cache size dynamically
fun monitorMemoryPressure() {
    // STUB: Add to master TODO/backlog
    TODO("Implement memory pressure monitoring - see Q3 Enhancement 3")
}
```

6. **Q3 Enhancement 4 (STUB):** Performance Analytics
```kotlin
// TODO: Track cache hit rates, query times, memory usage
fun trackPerformanceMetrics() {
    // STUB: Add to master TODO/backlog
    TODO("Implement performance analytics - see Q3 Enhancement 4")
}
```

7. **Q3 Enhancement 5 (STUB):** Adaptive Cache Sizing
```kotlin
// TODO: Adjust Tier 1/2 sizes based on device capabilities
fun adaptiveCacheSize() {
    // STUB: Add to master TODO/backlog
    TODO("Implement adaptive cache sizing - see Q3 Enhancement 5")
}
```

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/cache/CommandCache.kt`
- Tier 1/2/3 caching implemented
- All 5 enhancements stubbed with TODO comments
- Performance benchmark showing <100ms resolution

**Test Coverage:** Unit tests for cache lookups, LRU eviction, database fallback

---

### Task 1.3: Context-Aware Routing (Q4)

**Objective:** Implement smart dispatcher that routes commands based on intent analysis.

**Reference Decision:** Q4 - Option D (Context-Aware Routing with Smart Dispatcher)

**Actions:**
1. Create `IntentDispatcher.kt` in `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/routing/`

2. Implement intent detection:
```kotlin
class IntentDispatcher(
    private val context: Context,
    private val actionRegistry: Map<String, BaseAction>
) {
    // Q4 Enhancement 1: Confidence Scoring
    suspend fun routeCommand(command: VoiceCommand): ActionResult {
        val candidates = findCandidateHandlers(command)

        if (candidates.isEmpty()) {
            return ActionResult.error("No handler found for command")
        }

        // Score each candidate
        val scored = candidates.map { handler ->
            handler to calculateConfidence(handler, command)
        }.sortedByDescending { it.second }

        // Try handlers in order of confidence
        for ((handler, confidence) in scored) {
            if (confidence < 0.3) {
                // Q4 Enhancement 4: Track fallback analytics
                trackFallbackAttempt(command, handler, confidence)
                continue  // Too low confidence, skip
            }

            try {
                val result = handler.execute(command)
                if (result is ActionResult.Success) {
                    // Q4 Enhancement 2: User feedback loop
                    trackSuccess(command, handler)
                    return result
                }
            } catch (e: Exception) {
                Log.w(TAG, "Handler failed: ${handler.category}", e)
                continue  // Try next handler
            }
        }

        // All handlers failed, use generic fallback
        return useGenericFallback(command)
    }

    // Q4 Enhancement 1: Calculate confidence score (0.0-1.0)
    private fun calculateConfidence(
        handler: BaseAction,
        command: VoiceCommand
    ): Double {
        var confidence = 0.0

        // Category match
        if (handler.category == command.category) {
            confidence += 0.4
        }

        // Current context match (Q3 user clarification: app context)
        val currentApp = getCurrentApp()
        if (handler.supportsApp(currentApp)) {
            confidence += 0.3
        }

        // Q4 Enhancement 3: Historical success in this context
        val historyScore = getContextHistory(handler, currentApp)
        confidence += historyScore * 0.3

        return confidence.coerceIn(0.0, 1.0)
    }

    // Q4 Enhancement 3: Context history tracking
    private fun getContextHistory(handler: BaseAction, app: String): Double {
        // Query learning database for this handler's success rate in this app
        return learningService.getSuccessRate(handler.category, app)
    }
}
```

3. **Q4 Enhancement 2:** User Feedback Loop
```kotlin
// Track when user explicitly corrects a command
fun recordUserCorrection(
    originalCommand: String,
    correctedCommand: String,
    context: String
) {
    database.insertFeedback(UserFeedback(
        originalCommand = originalCommand,
        correctedCommand = correctedCommand,
        context = context,
        timestamp = System.currentTimeMillis()
    ))
}
```

4. **Q4 Enhancement 4:** Fallback Analytics
```kotlin
// Track how often fallback is used
fun trackFallbackUsage() {
    // Export fallback metrics: which commands fail most often
    // Helps identify commands that need better handlers
}
```

5. **Q4 Enhancement 5:** Dynamic Fallback Rules
```kotlin
// Adjust routing rules based on patterns
// e.g., if "click" fails in browser 90% of time, route to different handler
fun updateRoutingRules() {
    // Analyze patterns and adjust confidence scoring
}
```

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/routing/IntentDispatcher.kt`
- Confidence scoring algorithm
- User feedback tracking
- Context history integration (requires Q11 learning system)
- Fallback analytics

**Test Coverage:** Unit tests for confidence calculation, routing logic, fallback behavior

---

## Phase 2: Action Types Implementation (Weeks 3-4)

### Task 2.1: DictationActions (Q6)

**Objective:** Implement speech-to-text dictation with settings-driven engine selection.

**Reference Decision:** Q6 - Custom Hybrid (Settings-Driven Engine Selection)

**Actions:**
1. Create `DictationActions.kt` in `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/`

2. Implement engine selection:
```kotlin
class DictationActions(
    private val context: Context,
    private val settings: VoiceSettings
) : BaseAction() {

    override val category = "dictation"
    override val supportedCommands = listOf(
        "start dictation", "stop dictation", "dictate"
    )

    override suspend fun execute(command: VoiceCommand): ActionResult {
        return when (command.primaryText) {
            "start dictation" -> startDictation()
            "stop dictation" -> stopDictation()
            else -> ActionResult.error("Unknown dictation command")
        }
    }

    private suspend fun startDictation(): ActionResult {
        // Q6: Settings-driven engine selection
        val engine = selectEngine()

        try {
            engine.initialize()
            engine.startListening(object : DictationCallback {
                override fun onResult(text: String) {
                    insertText(text)
                }

                override fun onError(error: String) {
                    announceToUser("Dictation error: $error")
                }
            })

            announceToUser("Dictation started")
            return ActionResult.success("Dictation active")
        } catch (e: Exception) {
            return ActionResult.error("Failed to start dictation: ${e.message}")
        }
    }

    // Q6: Smart engine selection
    private fun selectEngine(): DictationEngine {
        // 1. Check user's setting (primary)
        val userPreference = settings.getDictationEngine()
        if (userPreference != null) {
            return createEngine(userPreference)
        }

        // 2. Smart defaults based on device type
        val deviceType = detectDeviceType()
        return when (deviceType) {
            DeviceType.AOSP, DeviceType.SMART_GLASSES -> {
                // Default to Vivoka for AOSP/SmartGlasses
                VivokaEngine(context)
            }
            DeviceType.GOOGLE_PLAY -> {
                // Default to Android SpeechRecognizer for Play devices
                AndroidSpeechEngine(context)
            }
            else -> {
                // Fallback to Vivoka
                VivokaEngine(context)
            }
        }
    }
}
```

3. **Q6 Enhancement 1:** Punctuation Commands
```kotlin
// Voice commands for punctuation: "period", "comma", "question mark"
fun handlePunctuationCommand(command: String): String {
    return when (command.lowercase()) {
        "period" -> "."
        "comma" -> ","
        "question mark" -> "?"
        "exclamation point" -> "!"
        "new line" -> "\n"
        "new paragraph" -> "\n\n"
        else -> ""
    }
}
```

4. **Q6 Enhancement 2:** Dictation Shortcuts
```kotlin
// Quick phrases: "new paragraph", "undo that"
```

5. **Q6 Enhancement 3:** Language Switching
```kotlin
// "Switch to Spanish" -> change dictation language mid-session
```

6. **Q6 Enhancement 4:** Dictation History
```kotlin
// Recall previous dictations: "Repeat last dictation"
```

7. **Q6 Enhancement 5:** Cloud Sync
```kotlin
// Sync custom vocabulary across devices
```

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/DictationActions.kt`
- Settings integration for engine selection
- All 5 enhancements implemented
- Tests for Vivoka and Android engines

**LOC Estimate:** ~150 lines

---

### Task 2.2: EditingActions

**Objective:** Implement text manipulation commands (copy, paste, cut, select, undo, redo).

**Actions:**
1. Create `EditingActions.kt`
2. Implement commands:
   - "copy" → Copy selected text to clipboard
   - "paste" → Insert clipboard content
   - "cut" → Copy and delete selected text
   - "select all" → Select all text in field
   - "undo" → Undo last edit
   - "redo" → Redo last undo

**Implementation:**
```kotlin
class EditingActions(
    private val accessibilityService: AccessibilityService
) : BaseAction() {

    override suspend fun execute(command: VoiceCommand): ActionResult {
        return when (command.primaryText) {
            "copy" -> performCopy()
            "paste" -> performPaste()
            "cut" -> performCut()
            "select all" -> performSelectAll()
            "undo" -> performUndo()
            "redo" -> performRedo()
            else -> ActionResult.error("Unknown editing command")
        }
    }

    private suspend fun performCopy(): ActionResult {
        // Use AccessibilityService to perform COPY action
        val success = accessibilityService.performGlobalAction(
            AccessibilityService.GLOBAL_ACTION_COPY
        )

        return if (success) {
            announceToUser("Copied")
            ActionResult.success("Text copied")
        } else {
            ActionResult.error("Copy failed")
        }
    }
}
```

**Deliverable:** `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/EditingActions.kt`

**LOC Estimate:** ~120 lines

---

### Task 2.3: AppActions

**Objective:** Implement app launching and switching commands.

**Actions:**
1. Create `AppActions.kt`
2. Implement commands:
   - "open [app name]" → Launch app
   - "switch to [app name]" → Switch to running app
   - "close app" → Close current app
   - "recent apps" → Show recent apps

**Implementation:**
```kotlin
class AppActions(private val context: Context) : BaseAction() {

    override suspend fun execute(command: VoiceCommand): ActionResult {
        return when {
            command.text.startsWith("open") -> launchApp(command.parameters["app"])
            command.text.startsWith("switch to") -> switchToApp(command.parameters["app"])
            command.text == "close app" -> closeCurrentApp()
            command.text == "recent apps" -> showRecentApps()
            else -> ActionResult.error("Unknown app command")
        }
    }

    private fun launchApp(appName: String?): ActionResult {
        if (appName == null) return ActionResult.error("App name required")

        // Resolve app name to package name
        val packageName = resolvePackageName(appName)

        // Launch app
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        context.startActivity(intent)

        return ActionResult.success("Launched $appName")
    }
}
```

**Deliverable:** `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/AppActions.kt`

**LOC Estimate:** ~80 lines

---

### Task 2.4: GestureActions

**Objective:** Implement gesture commands (swipe, pinch, scroll, drag).

**Actions:**
1. Create `GestureActions.kt`
2. Implement commands:
   - "swipe up/down/left/right"
   - "scroll up/down"
   - "pinch to zoom"
   - "drag [direction]"

**Implementation:**
```kotlin
class GestureActions(
    private val accessibilityService: AccessibilityService
) : BaseAction() {

    override suspend fun execute(command: VoiceCommand): ActionResult {
        return when {
            command.text.startsWith("swipe") -> performSwipe(command.parameters["direction"])
            command.text.startsWith("scroll") -> performScroll(command.parameters["direction"])
            command.text == "pinch to zoom" -> performPinch()
            else -> ActionResult.error("Unknown gesture command")
        }
    }

    private fun performSwipe(direction: String?): ActionResult {
        val gestureBuilder = GestureDescription.Builder()

        val path = when (direction) {
            "up" -> createSwipeUpPath()
            "down" -> createSwipeDownPath()
            "left" -> createSwipeLeftPath()
            "right" -> createSwipeRightPath()
            else -> return ActionResult.error("Invalid direction")
        }

        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 250))

        val success = accessibilityService.dispatchGesture(
            gestureBuilder.build(),
            null,
            null
        )

        return if (success) ActionResult.success("Swiped $direction")
               else ActionResult.error("Swipe failed")
    }
}
```

**Deliverable:** `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/GestureActions.kt`

**LOC Estimate:** ~150 lines

---

### Task 2.5: OverlayActions

**Objective:** Implement UI overlay management commands.

**Actions:**
1. Create `OverlayActions.kt`
2. Implement commands:
   - "show overlay" → Display custom overlay
   - "hide overlay" → Remove overlay
   - "show HUD" → Display heads-up display
   - "hide HUD" → Remove HUD

**Integration with Q7:** These actions must work in BOTH visual mode (overlay granted) and audio mode (overlay denied).

**Deliverable:** `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/OverlayActions.kt`

**LOC Estimate:** ~100 lines

---

### Task 2.6: NotificationActions

**Objective:** Implement notification interaction commands.

**Actions:**
1. Create `NotificationActions.kt`
2. Implement commands:
   - "read notifications" → Announce notifications via TTS
   - "open notification" → Open notification
   - "dismiss notification" → Dismiss notification
   - "dismiss all notifications" → Clear all

**Deliverable:** `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/NotificationActions.kt`

**LOC Estimate:** ~80 lines

---

### Task 2.7: ShortcutActions

**Objective:** Implement accessibility shortcut commands.

**Actions:**
1. Create `ShortcutActions.kt`
2. Implement commands:
   - "accessibility menu" → Open accessibility menu
   - "quick settings" → Open quick settings
   - "home" → Go to home screen
   - "back" → Go back

**Deliverable:** `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/ShortcutActions.kt`

**LOC Estimate:** ~60 lines

---

## Phase 3: Intelligence Layer (Week 5)

### Task 3.1: Learning System Implementation (Q11)

**Objective:** Implement hybrid learning (frequency + context + feedback) with multi-app tracking.

**Reference Decision:** Q11 - Option D (Hybrid Learning)

**Actions:**
1. Create `CommandLearningEntity.kt`:
```kotlin
@Entity(
    tableName = "command_learning",
    indices = [
        Index(value = ["command_id", "context"], unique = true),
        Index(value = ["context"]),
        Index(value = ["last_used_at"])
    ]
)
data class CommandLearningEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commandId: String,
    val context: String?,            // null = global, or "com.android.chrome"
    val usageCount: Int = 0,
    val successCount: Int = 0,
    val errorCount: Int = 0,
    val lastUsedAt: Long,
    val createdAt: Long
)
```

2. Create `HybridLearningService.kt`:
```kotlin
class HybridLearningService(private val database: LearningDao) {

    // Q11: Multi-app tracking with context rotation
    private val appCommandQueues = mutableMapOf<String, List<VoiceCommand>>()
    private var foregroundApp: String? = null

    // Q11: Track global usage (Tier 1)
    suspend fun trackGlobalUsage(commandId: String, success: Boolean) {
        val learning = database.getLearning(commandId, context = null)
            ?: CommandLearningEntity(
                commandId = commandId,
                context = null,
                lastUsedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

        database.updateLearning(learning.copy(
            usageCount = learning.usageCount + 1,
            successCount = if (success) learning.successCount + 1 else learning.successCount,
            errorCount = if (!success) learning.errorCount + 1 else learning.errorCount,
            lastUsedAt = System.currentTimeMillis()
        ))
    }

    // Q11: Track context-aware usage (Tier 2)
    suspend fun trackContextUsage(
        commandId: String,
        appPackage: String,
        success: Boolean
    ) {
        val learning = database.getLearning(commandId, context = appPackage)
            ?: CommandLearningEntity(
                commandId = commandId,
                context = appPackage,
                lastUsedAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )

        database.updateLearning(learning.copy(
            usageCount = learning.usageCount + 1,
            successCount = if (success) learning.successCount + 1 else learning.successCount,
            errorCount = if (!success) learning.errorCount + 1 else learning.errorCount,
            lastUsedAt = System.currentTimeMillis()
        ))
    }

    // Q11: User clarification - multi-app command queue rotation
    suspend fun preloadAppCommands(packageName: String) {
        if (!appCommandQueues.containsKey(packageName)) {
            val commands = database.getCommandsForApp(packageName)
            appCommandQueues[packageName] = commands
            Log.i(TAG, "Preloaded ${commands.size} commands for $packageName")
        }
    }

    fun onAppForeground(packageName: String) {
        foregroundApp = packageName

        // Rotate context: prioritize foreground app's commands
        val foregroundCommands = appCommandQueues[packageName] ?: emptyList()
        // Update cache priority (integrate with Q3 tiered caching)
        cache.setPriorityCommands(foregroundCommands)

        Log.i(TAG, "Rotated context to $packageName")
    }

    // Q11: Smart scoring algorithm
    suspend fun getCommandScore(
        commandId: String,
        currentContext: String?,
        baseScore: Int
    ): Int {
        // Context-specific score (if available)
        val contextLearning = currentContext?.let {
            database.getLearning(commandId, it)
        }

        // Global score (always available)
        val globalLearning = database.getLearning(commandId, null)

        // Prefer context-specific if usage count > 5
        val learning = if (contextLearning != null && contextLearning.usageCount > 5) {
            contextLearning
        } else {
            globalLearning ?: return baseScore
        }

        // Calculate score boost
        val frequencyBoost = min(learning.usageCount / 10, 20)
        val successRateBoost = if (learning.usageCount > 10) {
            val successRate = learning.successCount.toFloat() / learning.usageCount
            (successRate * 10).toInt()  // 0-10 boost
        } else {
            0
        }
        val recencyBoost = calculateRecencyBoost(learning.lastUsedAt)

        return baseScore + frequencyBoost + successRateBoost + recencyBoost
    }

    private fun calculateRecencyBoost(lastUsedAt: Long): Int {
        val hoursSinceLastUse = (System.currentTimeMillis() - lastUsedAt) / (1000 * 60 * 60)
        return when {
            hoursSinceLastUse < 1 -> 5    // Last hour
            hoursSinceLastUse < 24 -> 3   // Today
            hoursSinceLastUse < 168 -> 1  // This week
            else -> 0
        }
    }
}
```

3. **Q11 Enhancement 1:** User Pattern Analysis Dashboard
```kotlin
// UI showing:
// - Top 10 most used commands
// - Usage trends over time (chart)
// - Context breakdown (commands per app)
// - Success rate metrics
```

4. **Q11 Enhancement 2:** Adaptive Command Ranking
```kotlin
// Adjust Tier 1 cache weekly based on learned patterns
fun updateTier1Cache() {
    // Get top 20 commands from last week
    val topCommands = learningService.getTopCommands(
        since = System.currentTimeMillis() - 7.days(),
        limit = 20
    )

    // Update Tier 1 cache
    cache.setTier1Commands(topCommands)
}
```

5. **Q11 Enhancement 3:** Error Prediction & Auto-Correction
```kotlin
// Track common misrecognitions and suggest corrections
// "onto" → "Did you mean 'undo'?"
```

6. **Q11 Enhancement 4:** Context-Aware Learning Insights
```kotlin
// "You often use 'copy' after 'select all' - create a macro?"
```

7. **Q11 Enhancement 5:** Export/Import User Learning Profiles
```kotlin
// Backup learning database to JSON
// Restore on new device
fun exportLearningProfile(): String {
    val allLearning = database.getAllLearning()
    return Json.encodeToString(allLearning)
}

fun importLearningProfile(json: String) {
    val learning = Json.decodeFromString<List<CommandLearningEntity>>(json)
    database.insertAll(learning)
}
```

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/learning/CommandLearningEntity.kt`
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/learning/HybridLearningService.kt`
- All 5 enhancements implemented
- Multi-app tracking with context rotation
- Tests for scoring algorithm, context rotation

**LOC Estimate:** ~950 lines (300 core + 650 enhancements)

---

### Task 3.2: Context-Aware Commands (Q10)

**Objective:** Implement multi-app command loading with hierarchical screen support.

**Reference Decision:** Q10 - Option D (Hybrid: App Detection + Simple Content Hints)

**User Clarification:**
> "all global commands should always be available (lazy loading based on context), when an app is loaded all commands associated with its package name i.e. com.microsoft.teams for example will be loaded from database to cache and hierarchical screens"

**Actions:**
1. Create `CommandContextManager.kt`:
```kotlin
class CommandContextManager(
    private val database: VoiceCommandDao,
    private val cache: CommandCache,
    private val learningService: HybridLearningService
) {
    // Global commands: ALWAYS in cache
    private val globalCommands: Set<VoiceCommand> = loadGlobalCommands()

    // App-specific command queue (multiple apps can be loaded)
    private val appCommandQueues = mutableMapOf<String, List<VoiceCommand>>()

    // Hierarchical screen tracking
    private val screenCommands = mutableMapOf<String, Map<String, List<VoiceCommand>>>()
    // screenCommands["com.microsoft.teams"]["chat_screen"] = [list of commands]

    // Current context
    private var foregroundApp: String? = null
    private var currentScreen: String? = null

    // Preload commands for an app (background or foreground)
    suspend fun preloadAppCommands(packageName: String) {
        if (!appCommandQueues.containsKey(packageName)) {
            // Load app-level commands
            val appCommands = database.getCommandsForApp(packageName)
            appCommandQueues[packageName] = appCommands

            // Load screen-level commands (hierarchical)
            val screens = database.getScreensForApp(packageName)
            val screenMap = screens.associateWith { screen ->
                database.getCommandsForScreen(packageName, screen)
            }
            screenCommands[packageName] = screenMap

            Log.i(TAG, "Preloaded ${appCommands.size} commands for $packageName")
        }
    }

    // Rotate context when app comes to foreground
    fun onAppForeground(packageName: String, screen: String? = null) {
        foregroundApp = packageName
        currentScreen = screen

        // Prioritize foreground app's commands in cache
        val foregroundCommands = appCommandQueues[packageName] ?: emptyList()

        // Add screen-specific commands if screen is known
        val screenSpecificCommands = screen?.let {
            screenCommands[packageName]?.get(it) ?: emptyList()
        } ?: emptyList()

        val allForegroundCommands = foregroundCommands + screenSpecificCommands

        // Update cache priority
        cache.setPriorityCommands(allForegroundCommands)

        // Apply learning-based ranking
        learningService.onAppForeground(packageName)

        Log.i(TAG, "Rotated context to $packageName${screen?.let { " / $it" } ?: ""}")
    }

    // Resolve command: Global → Foreground screen → Foreground app → Other apps → Database
    suspend fun resolveCommand(text: String): VoiceCommand? {
        // 1. Check global commands (always available)
        globalCommands.find { it.matches(text) }?.let { return it }

        // 2. Check foreground screen commands (highest priority)
        currentScreen?.let { screen ->
            foregroundApp?.let { app ->
                screenCommands[app]?.get(screen)?.find { it.matches(text) }?.let { return it }
            }
        }

        // 3. Check foreground app commands
        foregroundApp?.let { pkg ->
            appCommandQueues[pkg]?.find { it.matches(text) }?.let { return it }
        }

        // 4. Check other loaded app commands
        appCommandQueues.values.forEach { commands ->
            commands.find { it.matches(text) }?.let { return it }
        }

        // 5. Fallback to database
        return database.queryCommand(text)
    }
}
```

2. **Q10 Enhancement 1:** Context Confidence Scoring
```kotlin
// How confident are we that we detected context correctly?
fun getContextConfidence(): Double {
    // Based on screen detection accuracy, app accessibility info quality
}
```

3. **Q10 Enhancement 2:** Context History
```kotlin
// Remember recent contexts to predict next context
```

4. **Q10 Enhancement 3:** Context Switching Optimization
```kotlin
// Pre-load likely next contexts (e.g., if in email app, likely to open browser)
```

5. **Q10 Enhancement 4:** Context Override
```kotlin
// User manual override: "Use browser commands"
```

6. **Q10 Enhancement 5:** Context Analytics
```kotlin
// Track context detection accuracy
```

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/context/CommandContextManager.kt`
- Hierarchical screen support
- All 5 enhancements implemented
- Tests for context rotation, screen detection

**LOC Estimate:** ~250 lines

---

### Task 3.3: Macro Support (Q9)

**Objective:** Implement pre-defined macros now, stub user-created macros for later.

**Reference Decision:** Q9 - Option D (Hybrid: Pre-defined now, user-created later)

**Actions:**
1. Create `MacroActions.kt`:
```kotlin
class MacroActions(private val commandManager: CommandManager) : BaseAction() {

    override val category = "macro"

    // Q9: Pre-defined macros (MVP)
    private val predefinedMacros = mapOf(
        "select all and copy" to listOf("select all", "copy"),
        "select all and cut" to listOf("select all", "cut"),
        "paste and enter" to listOf("paste", "press enter"),
        "take screenshot and share" to listOf("screenshot", "share")
    )

    override suspend fun execute(command: VoiceCommand): ActionResult {
        val macroSteps = predefinedMacros[command.primaryText]
            ?: return ActionResult.error("Unknown macro")

        // Execute each step in sequence
        for (step in macroSteps) {
            val result = commandManager.executeCommand(step)
            if (result is ActionResult.Error) {
                return ActionResult.error("Macro failed at step: $step")
            }
            delay(200)  // Brief pause between steps
        }

        return ActionResult.success("Macro executed")
    }
}
```

2. **Q9 Enhancement 1 (Implemented):** Macro Categories
```kotlin
// Organize macros by use case: Editing, Navigation, Accessibility
enum class MacroCategory {
    EDITING, NAVIGATION, ACCESSIBILITY, PRODUCTIVITY
}

data class Macro(
    val name: String,
    val steps: List<String>,
    val category: MacroCategory,
    val description: String
)
```

3. **Q9 Enhancement 2 (STUB):** Macro Sharing
```kotlin
// TODO: Implement macro sharing with other users
fun shareMacro(macro: Macro) {
    TODO("Implement macro sharing - see Q9 Enhancement 2 - Add to master TODO/backlog")
}
```

4. **Q9 Enhancement 3 (Implemented):** Macro Variables
```kotlin
// Parameterized macros: "Open [app] and search [query]"
data class MacroParameter(
    val name: String,
    val type: ParameterType,  // APP, TEXT, NUMBER
    val defaultValue: String? = null
)
```

5. **Q9 Enhancement 4 (STUB):** Macro Conditions
```kotlin
// TODO: Implement if/then logic in macros
fun executeConditionalMacro(macro: ConditionalMacro) {
    TODO("Implement macro conditions - see Q9 Enhancement 4 - Add to master TODO/backlog")
}
```

6. **Q9 Enhancement 5 (STUB):** Macro Marketplace
```kotlin
// TODO: Implement community macro marketplace
fun browseMacroMarketplace() {
    TODO("Implement macro marketplace - see Q9 Enhancement 5 - Add to master TODO/backlog")
}
```

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/actions/MacroActions.kt`
- Pre-defined macros implemented
- Enhancements 1, 3 implemented
- Enhancements 2, 4, 5 stubbed with TODO
- Tests for macro execution

**LOC Estimate:** ~250 lines (150 implemented + 100 stubs)

---

## Phase 4: Extensibility & Testing (Week 6)

### Task 4.1: Plugin System Implementation (Q12)

**Objective:** Implement full plugin system for third-party extensions.

**Reference Decision:** Q12 - Option B (Plugin System - APK/JAR Loading)

**CRITICAL SECURITY REQUIREMENTS:**
- Signature verification for all plugins
- Sandboxed execution with timeout enforcement
- Permission model to limit plugin capabilities
- Malware scanning before loading

**Actions:**
1. Create `ActionPlugin.kt` interface:
```kotlin
interface ActionPlugin {
    val pluginId: String
    val version: String
    val supportedCommands: List<String>

    fun initialize(context: Context, permissions: PluginPermissions)
    suspend fun execute(command: VoiceCommand): ActionResult
    fun shutdown()
}

data class PluginPermissions(
    val canAccessNetwork: Boolean = false,
    val canAccessStorage: Boolean = false,
    val canAccessLocation: Boolean = false,
    val canPerformGestures: Boolean = true,
    val canLaunchApps: Boolean = true
)
```

2. Create `PluginManager.kt`:
```kotlin
class PluginManager(private val context: Context) {
    private val loadedPlugins = mutableMapOf<String, ActionPlugin>()

    fun loadPlugins() {
        val pluginDir = File(context.filesDir, "plugins")
        pluginDir.listFiles { file ->
            file.extension == "apk" || file.extension == "jar"
        }?.forEach { pluginFile ->
            try {
                // Security: Verify signature
                if (!verifyPluginSignature(pluginFile)) {
                    Log.w(TAG, "Plugin signature verification failed: ${pluginFile.name}")
                    return@forEach
                }

                val plugin = loadPluginFromFile(pluginFile)

                // Security: Validate plugin
                validatePlugin(plugin)

                // Calculate permissions based on manifest
                val permissions = calculatePermissions(plugin)
                plugin.initialize(context, permissions)

                loadedPlugins[plugin.pluginId] = plugin
                Log.i(TAG, "Loaded plugin: ${plugin.pluginId} v${plugin.version}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load plugin: ${pluginFile.name}", e)
            }
        }
    }

    private fun loadPluginFromFile(file: File): ActionPlugin {
        val classLoader = DexClassLoader(
            file.absolutePath,
            context.cacheDir.absolutePath,
            null,
            javaClass.classLoader
        )

        // Load plugin class from manifest
        val pluginClassName = readPluginManifest(file)
        val pluginClass = classLoader.loadClass(pluginClassName)
        return pluginClass.newInstance() as ActionPlugin
    }

    private fun validatePlugin(plugin: ActionPlugin) {
        // Signature verification (already done)
        // Version compatibility check
        val minVersion = plugin.getMinimumVOSVersion()
        if (minVersion > getCurrentVOSVersion()) {
            throw IncompatiblePluginException("Plugin requires VOS $minVersion")
        }

        // Permission validation
        // Security scan (basic malware detection)
    }

    suspend fun executePluginCommand(
        pluginId: String,
        command: VoiceCommand
    ): ActionResult {
        val plugin = loadedPlugins[pluginId]
            ?: return ActionResult.error("Plugin not found")

        // Security: Timeout enforcement (5s max)
        return withTimeout(5000) {
            try {
                plugin.execute(command)
            } catch (e: Exception) {
                Log.e(TAG, "Plugin execution failed: $pluginId", e)
                ActionResult.error("Plugin error: ${e.message}")
            }
        }
    }

    fun unloadPlugin(pluginId: String) {
        loadedPlugins[pluginId]?.shutdown()
        loadedPlugins.remove(pluginId)
    }
}
```

3. **Q12 Enhancement 1:** Action Discovery API
```kotlin
// Programmatic way to query available actions
interface ActionDiscoveryAPI {
    fun getAvailableActions(): List<ActionMetadata>
    fun searchActions(query: String): List<ActionMetadata>
    fun getActionMetadata(actionId: String): ActionMetadata?
    fun isActionAvailable(actionId: String): Boolean
}

data class ActionMetadata(
    val actionId: String,
    val category: String,
    val supportedCommands: List<String>,
    val description: String,
    val version: String,
    val isPlugin: Boolean
)
```

4. **Q12 Enhancement 2:** Action Telemetry & Analytics (WITH PRIVACY TOGGLE)
```kotlin
// CRITICAL: Must have user setting to enable/disable telemetry
class ActionTelemetry(private val settings: VoiceSettings) {

    fun trackActionExecution(
        actionId: String,
        commandText: String,
        success: Boolean,
        executionTime: Long
    ) {
        // Check user's privacy setting
        if (!settings.isTelemetryEnabled()) {
            return  // User opted out, don't track
        }

        database.insertTelemetry(TelemetryEvent(
            actionId = actionId,
            commandText = commandText,  // NOTE: May contain sensitive data
            success = success,
            executionTime = executionTime,
            timestamp = System.currentTimeMillis()
        ))
    }

    fun exportTelemetryReport(): String {
        // Export analytics (CSV/JSON)
    }
}
```

**IMPORTANT:** Add Settings UI toggle:
```kotlin
// In Settings app
<SwitchPreference
    android:key="telemetry_enabled"
    android:title="Action Usage Analytics"
    android:summary="Help improve VOS by sharing anonymized usage data"
    android:defaultValue="false" />  <!-- Default OFF for privacy -->
```

5. **Q12 Enhancement 3:** Hot-Reload Development Mode
```kotlin
// Developer mode: Reload plugins without restart
class PluginHotReload(private val pluginManager: PluginManager) {

    private val fileWatcher = FileObserver(pluginDirectory)

    fun enableHotReload() {
        fileWatcher.startWatching()
        fileWatcher.onEvent { event ->
            if (event.type == FileObserver.MODIFY) {
                // Reload plugin
                val pluginFile = event.file
                pluginManager.unloadPlugin(pluginFile.nameWithoutExtension)
                pluginManager.loadPlugin(pluginFile)
                Log.i(TAG, "Hot-reloaded plugin: ${pluginFile.name}")
            }
        }
    }
}
```

6. **Q12 Enhancement 4:** Action Versioning & Migration
```kotlin
// Support for action API evolution
interface MigratableAction {
    val apiVersion: Int

    fun migrateFrom(oldVersion: Int, data: Map<String, Any>): Map<String, Any>
}
```

7. **Q12 Enhancement 5:** Action Composition Framework
```kotlin
// Combine multiple actions into workflows
class ActionComposer {

    fun chain(actions: List<ActionPlugin>): CompositeAction {
        // Sequential execution
    }

    fun parallel(actions: List<ActionPlugin>): CompositeAction {
        // Parallel execution
    }

    fun conditional(
        condition: (Context) -> Boolean,
        ifTrue: ActionPlugin,
        ifFalse: ActionPlugin
    ): CompositeAction {
        // Conditional logic
    }
}
```

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/plugins/ActionPlugin.kt`
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/plugins/PluginManager.kt`
- All 5 enhancements implemented
- **Settings UI toggle for telemetry (Q12 Enhancement 2)**
- Security validation (signature verification, sandbox, timeout)
- Tests for plugin loading, execution, security

**LOC Estimate:** ~2300 lines (1500 core + 800 enhancements)

---

### Task 4.2: Dual Mode Permissions (Q7)

**Objective:** Implement hybrid overlay permissions with dual mode support.

**Reference Decision:** Q7 - Option D (Hybrid: Proactive Request + Graceful Degradation)

**CRITICAL REQUIREMENT:** ALL features must work in BOTH modes (visual + audio)

**Actions:**
1. Create `PermissionManager.kt`:
```kotlin
class PermissionManager(private val context: Context) {

    enum class OverlayMode {
        VISUAL,     // Overlays granted
        AUDIO       // Overlays denied, audio-only
    }

    private var currentMode: OverlayMode = detectMode()

    fun detectMode(): OverlayMode {
        return if (Settings.canDrawOverlays(context)) {
            OverlayMode.VISUAL
        } else {
            OverlayMode.AUDIO
        }
    }

    // Q7: Proactive permission request
    fun requestOverlayPermission() {
        if (currentMode == OverlayMode.AUDIO) {
            // Show education UI (Q7 Enhancement 1)
            showPermissionEducationDialog()

            // Request permission
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            )
            context.startActivity(intent)
        }
    }

    // Q7 Enhancement 1: Permission Education UI
    private fun showPermissionEducationDialog() {
        // Explain why overlay is needed
        // Show examples of visual features
        // Emphasize that app works without it
    }

    // Q7 Enhancement 3: Permission Recovery
    fun checkPermissionRecovery() {
        val newMode = detectMode()
        if (newMode != currentMode) {
            currentMode = newMode
            if (newMode == OverlayMode.VISUAL) {
                announceToUser("Visual mode enabled. Cursor and HUD now available.")
            }
        }
    }
}
```

2. Implement dual mode for all features:
```kotlin
// Example: Cursor feedback in both modes
class CursorActions(
    private val cursorAPI: VoiceCursorAPI,
    private val permissionManager: PermissionManager,
    private val tts: TextToSpeech,
    private val vibrator: Vibrator
) : BaseAction() {

    override suspend fun execute(command: VoiceCommand): ActionResult {
        return when (permissionManager.currentMode) {
            OverlayMode.VISUAL -> executeVisualMode(command)
            OverlayMode.AUDIO -> executeAudioMode(command)
        }
    }

    private suspend fun executeVisualMode(command: VoiceCommand): ActionResult {
        // Use visual cursor
        val result = cursorAPI.showCursor()
        if (result.success) {
            // Brief audio confirmation
            tts.speak("Cursor shown", TextToSpeech.QUEUE_FLUSH)
        }
        return ActionResult.success()
    }

    private suspend fun executeAudioMode(command: VoiceCommand): ActionResult {
        // No visual cursor, use audio + vibration feedback
        tts.speak("Cursor mode unavailable. Using direct touch.", TextToSpeech.QUEUE_FLUSH)
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))

        // Fallback: Perform action directly without cursor
        // e.g., "click" -> find nearest clickable element and click it
        return performDirectTouch(command)
    }
}
```

3. **Q7 Enhancement 2:** Fallback Mode Selector
```kotlin
// User can choose preference: visual-preferred, audio-preferred, auto
```

4. **Q7 Enhancement 4:** Mode Testing
```kotlin
// Developer tools to test both modes easily
fun enableTestMode(mode: OverlayMode) {
    // Force mode regardless of actual permission
}
```

5. **Q7 Enhancement 5:** Accessibility Announcements
```kotlin
// TalkBack-compatible audio feedback for all actions
```

**Deliverables:**
- `/modules/managers/CommandManager/src/main/java/com/augmentalis/commandmanager/permissions/PermissionManager.kt`
- Dual mode implementation for ALL action types
- Permission education UI
- All 5 enhancements implemented
- Tests for both visual and audio modes

**LOC Estimate:** ~200 lines

---

### Task 4.3: Complete Test Suite (Q8)

**Objective:** Implement test pyramid with 80% coverage.

**Reference Decision:** Q8 - Option D (Test Pyramid: 70/25/5)

**Test Distribution:**
- **Unit Tests (70%):** ~350 tests
- **Integration Tests (25%):** ~125 tests
- **E2E Tests (5%):** ~25 tests

**Actions:**

1. **Unit Tests (~350 tests):**
   - Service Monitor: 30 tests
   - Tiered Caching: 40 tests
   - Intent Dispatcher: 40 tests
   - All Action Types (11 types × 15 tests): 165 tests
   - Learning System: 40 tests
   - Context Manager: 30 tests
   - Plugin Manager: 50 tests (including security tests)
   - Permissions: 20 tests

Example unit test:
```kotlin
@Test
fun `test tiered cache lookup - tier 1 hit`() = runTest {
    // Arrange
    val cache = CommandCache(mockDatabase)
    val command = VoiceCommand("forward", "navigation")

    // Act
    val result = cache.resolveCommand("forward", null)

    // Assert
    assertEquals(command, result)
    verify(mockDatabase, never()).queryCommand(any(), any())  // Didn't hit database
}
```

2. **Integration Tests (~125 tests):**
   - VoiceOSService ↔ CommandManager: 40 tests
   - CommandManager ↔ VoiceCursor: 20 tests
   - CommandManager ↔ Plugin System: 30 tests
   - Learning System ↔ Context Manager: 20 tests
   - Database ↔ Cache: 15 tests

Example integration test:
```kotlin
@Test
fun `test voice command end-to-end flow`() = runTest {
    // Arrange
    val service = VoiceOSService()
    service.onCreate()
    service.onServiceConnected()

    // Act
    service.handleVoiceCommand("forward")

    // Assert
    // Verify navigation action was executed
    // Verify telemetry was recorded
    // Verify learning was updated
}
```

3. **E2E Tests (~25 tests):**
   - Full voice recognition → command execution flows
   - Multi-app context switching scenarios
   - Plugin loading and execution
   - Dual mode (visual/audio) scenarios
   - Learning and adaptation over time

Example E2E test:
```kotlin
@Test
fun `test complete voice command flow with context switching`() {
    // Scenario: User switches from browser to email app, uses context-specific commands

    // 1. User is in browser
    activityScenario.launch<BrowserActivity>()

    // 2. User says "scroll down" (browser command)
    speakCommand("scroll down")
    onView(withId(R.id.browser_content)).check(matches(isScrolledDown()))

    // 3. User switches to email app
    activityScenario.launch<EmailActivity>()

    // 4. Context rotates to email app commands
    waitForContextRotation()

    // 5. User says "send email" (email-specific command)
    speakCommand("send email")
    onView(withId(R.id.send_button)).check(matches(isClicked()))
}
```

4. **Q8 Enhancement 1:** Mocking Framework
```kotlin
// Mock AccessibilityService for unit tests
class MockAccessibilityService : AccessibilityService() {
    val dispatchedGestures = mutableListOf<GestureDescription>()

    override fun dispatchGesture(...): Boolean {
        dispatchedGestures.add(gesture)
        return true
    }
}
```

5. **Q8 Enhancement 3:** CI/CD Integration
```yaml
# .github/workflows/test.yml
name: Test Suite

on: [push, pull_request]

jobs:
  unit-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run unit tests
        run: ./gradlew testDebugUnitTest

  integration-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Run integration tests
        run: ./gradlew connectedDebugAndroidTest

  e2e-tests:
    runs-on: ubuntu-latest
    # Only on PR and nightly
    if: github.event_name == 'pull_request' || github.event_name == 'schedule'
    steps:
      - uses: actions/checkout@v2
      - name: Run E2E tests
        run: ./gradlew e2eDebugAndroidTest
```

6. **Q8 Enhancement 4:** Test Coverage Tracking
```kotlin
// Jacoco configuration for 80% coverage target
jacoco {
    toolVersion = "0.8.8"
}

tasks.jacocoTestReport {
    reports {
        xml.enabled = true
        html.enabled = true
    }

    classDirectories.setFrom(
        fileTree(project.buildDir) {
            include("**/classes/**/com/augmentalis/**/*.class")
            exclude("**/*Test*.class", "**/*\$*.class")
        }
    )
}

tasks.jacocoTestCoverageVerification {
    violationRules {
        rule {
            limit {
                minimum = 0.80  // 80% coverage minimum
            }
        }
    }
}
```

7. **Q8 Enhancement 5:** Performance Benchmarks
```kotlin
@RunWith(AndroidJUnit4::class)
class CommandManagerBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmark_command_resolution() {
        benchmarkRule.measureRepeated {
            commandManager.executeCommand("forward")
        }
    }

    @Test
    fun benchmark_tier1_cache_lookup() {
        benchmarkRule.measureRepeated {
            cache.resolveCommand("forward", null)
        }
    }
}
```

**Deliverables:**
- 350 unit tests across all modules
- 125 integration tests
- 25 E2E tests
- CI/CD pipeline configuration
- 80% test coverage achieved
- Performance benchmarks

**LOC Estimate:** ~500 test lines

---

## Post-Implementation Tasks

### Task 5.1: Documentation Updates

**Objective:** Update all affected documentation BEFORE committing code.

**Actions:**
1. Update module changelogs:
   - `/docs/modules/CommandManager/changelog/CHANGELOG.md`
   - `/docs/modules/voice-accessibility/changelog/CHANGELOG.md`
   - `/docs/modules/VoiceCursor/changelog/CHANGELOG.md`

2. Update API documentation:
   - `/docs/modules/CommandManager/reference/api/`
   - Document all new public methods, interfaces

3. Update architecture documentation:
   - `/docs/modules/CommandManager/architecture/`
   - Add diagrams showing integration with VoiceOSService
   - Document plugin architecture

4. Update user manual:
   - `/docs/modules/CommandManager/user-manual/`
   - Document all new voice commands
   - Add examples and use cases

5. Update developer manual:
   - `/docs/modules/CommandManager/developer-manual/`
   - Document how to create plugins
   - Document how to add new action types

**Format for changelog entries:**
```markdown
## [Version X.Y.Z] - 2025-10-DD

### Added
- **Service Monitor:** Lifecycle management with health checks and reconnection callbacks (Q1)
- **Tiered Caching:** 3-tier cache (Tier 1/2/3) for <100ms command resolution (Q3)
- **8 New Action Types:** DictationActions, CursorActions, EditingActions, AppActions, GestureActions, OverlayActions, NotificationActions, ShortcutActions (Q2)
- **Learning System:** Hybrid learning with frequency tracking, context awareness, and user feedback (Q11)
- **Context-Aware Commands:** Multi-app command loading with hierarchical screen support (Q10)
- **Plugin System:** Full extensibility with APK/JAR plugin loading and security sandboxing (Q12)
- **Dual Mode Permissions:** Visual (overlay granted) and audio (overlay denied) modes for all features (Q7)
- **Macro Support:** Pre-defined macros with categories and parameters (Q9)
- **Intent Dispatcher:** Context-aware routing with confidence scoring and fallback strategies (Q4)

### Changed
- **VoiceCursor:** Refactored to separate command logic (moved to CursorActions) from cursor mechanics (Q5)
- **VoiceOSService:** Now binds to CommandManager for voice command processing (Q1)

### Security
- **Plugin Signature Verification:** All plugins must be signed and verified before loading
- **Plugin Sandboxing:** Plugins execute with timeout enforcement (5s max) and limited permissions
- **Telemetry Privacy:** User setting to enable/disable action usage analytics (Q12 Enhancement 2)

### Performance
- **Command Resolution:** <100ms target achieved via tiered caching
- **Cache Memory:** ~35KB total (Tier 1: 10KB, Tier 2: 25KB)
- **Test Coverage:** 80% achieved (350 unit + 125 integration + 25 E2E tests)
```

---

### Task 5.2: Master TODO/Backlog Update

**Objective:** Add all stubbed/deferred enhancements to master TODO list.

**File:** `/coding/TODO/VOS4-TODO-Master-YYMMDD-HHMM.md` (create new timestamped file)

**Items to Add:**

**Q3: Performance Optimization (All Stubbed for V2)**
- [ ] Q3 Enhancement 1: Predictive Preloading - Preload commands based on user patterns
- [ ] Q3 Enhancement 2: Cache Warming - Warm cache on service start
- [ ] Q3 Enhancement 3: Memory Pressure Monitoring - Adjust cache size dynamically
- [ ] Q3 Enhancement 4: Performance Analytics - Track cache hit rates, query times
- [ ] Q3 Enhancement 5: Adaptive Cache Sizing - Adjust Tier 1/2 sizes based on device

**Q9: Macro Support (Deferred for V2)**
- [ ] Q9 Enhancement 2: Macro Sharing - Share macros with other users
- [ ] Q9 Enhancement 4: Macro Conditions - If/then logic in macros
- [ ] Q9 Enhancement 5: Macro Marketplace - Community macro repository

**Q2: Action Priority (Deferred for V2)**
- [ ] Q2 Enhancement 1: Action Chaining - Chain multiple actions automatically
- [ ] Q2 Enhancement 2: Action Undo/Redo - Undo/redo any action
- [ ] Q2 Enhancement 3: Action Batching - Execute multiple actions in batch

**Future Enhancements:**
- [ ] Scripting Support (Q12 Option C) - Lua/JavaScript for custom commands
- [ ] Continuous Dictation Mode (Q6) - Always-listening dictation
- [ ] ML-Based Prediction (Q11 Option C) - TensorFlow Lite model for command prediction

---

### Task 5.3: Commit with Proper Staging

**Objective:** Commit all changes following VOS4-COMMIT-PROTOCOL.md

**CRITICAL RULES:**
- Stage by category: docs → code → tests (NEVER mix)
- NO AI/Claude/Anthropic references in commit messages
- ALL documentation updated BEFORE code commits

**Commit Sequence:**

**Commit 1: Documentation**
```bash
# Stage documentation files
git add docs/modules/CommandManager/
git add docs/modules/voice-accessibility/
git add docs/modules/VoiceCursor/
git add coding/TODO/
git add coding/STATUS/

# Commit
git commit -m "$(cat <<'EOF'
docs: add CommandManager integration documentation

- Add comprehensive architecture documentation for CommandManager → VoiceOSService integration
- Document all 12 Q&A decisions and architectural choices
- Add API documentation for plugin system, learning system, context management
- Update changelogs for CommandManager, VoiceAccessibility, VoiceCursor
- Add implementation instructions and status reports
- Update master TODO with deferred enhancements

Covers: Service Monitor, Tiered Caching, Intent Routing, 8 Action Types,
Learning System, Context-Aware Commands, Plugin System, Dual Mode Permissions,
Macro Support, VoiceCursor Refactoring
EOF
)"
```

**Commit 2: CommandManager Code**
```bash
# Stage CommandManager files
git add modules/managers/CommandManager/

# Commit
git commit -m "$(cat <<'EOF'
feat(commandmanager): implement voice command integration with VoiceOSService

Service Infrastructure:
- Add ServiceMonitor with health checks and reconnection callbacks
- Implement 3-tier caching (Tier 1: top 20, Tier 2: LRU 50, Tier 3: database)
- Add IntentDispatcher with confidence scoring and context-aware routing

Action Types (8 new):
- Add DictationActions with settings-driven engine selection
- Add CursorActions (delegates to VoiceCursor API)
- Add EditingActions (copy, paste, cut, select, undo, redo)
- Add AppActions (launch, switch, close apps)
- Add GestureActions (swipe, scroll, pinch, drag)
- Add OverlayActions (show/hide overlays and HUD)
- Add NotificationActions (read, open, dismiss notifications)
- Add ShortcutActions (accessibility menu, quick settings, home, back)

Intelligence:
- Add HybridLearningService with multi-app tracking and context rotation
- Add CommandContextManager with hierarchical screen support
- Add MacroActions with pre-defined macros and parameter support

Extensibility:
- Add PluginManager with APK/JAR loading and security sandboxing
- Add ActionPlugin interface with permissions model
- Add telemetry with user privacy toggle

Performance:
- Achieve <100ms command resolution via tiered caching
- Memory footprint: ~35KB for caches

Security:
- Plugin signature verification
- Timeout enforcement (5s max per plugin)
- Permission sandboxing for untrusted code
EOF
)"
```

**Commit 3: VoiceAccessibility Code**
```bash
# Stage VoiceAccessibility files
git add modules/apps/VoiceAccessibility/

# Commit
git commit -m "$(cat <<'EOF'
feat(voiceaccessibility): integrate CommandManager for voice command processing

- Bind CommandManager in VoiceOSService.onServiceConnected()
- Add handleVoiceCommand() method for speech recognition integration
- Add ServiceMonitor for lifecycle management
- Add connection state UI (notification indicator)
- Add graceful degradation for CommandManager unavailability
- Add configuration persistence for state restoration
EOF
)"
```

**Commit 4: VoiceCursor Refactoring**
```bash
# Stage VoiceCursor files
git add modules/apps/VoiceCursor/

# Commit
git commit -m "$(cat <<'EOF'
refactor(voicecursor): separate command logic from cursor mechanics

Breaking Changes:
- Extract all voice command handling to CommandManager/CursorActions
- Refactor VoiceCursor to implement VoiceCursorAPI (pure mechanics + UI)

API Changes:
- Add VoiceCursorAPI interface: showCursor(), hideCursor(), moveTo(), clickAtCursor()
- Remove command parsing logic (now in CursorActions)
- Add support for multi-cursor and cursor themes

This maintains 100% functional equivalency while achieving separation of concerns.
Voice command logic now resides in CommandManager (consistent with other action types).
EOF
)"
```

**Commit 5: Settings Integration**
```bash
# Stage Settings files (if Settings module exists)
git add modules/apps/VoiceSettings/

# Commit
git commit -m "$(cat <<'EOF'
feat(settings): add telemetry privacy toggle and dictation engine selection

- Add telemetry_enabled preference (default: OFF for privacy)
- Add dictation_engine_preference for user engine selection
- Add permission education UI for overlay requests
EOF
)"
```

**Commit 6: Tests**
```bash
# Stage all test files
git add modules/managers/CommandManager/src/test/
git add modules/managers/CommandManager/src/androidTest/
git add modules/apps/VoiceAccessibility/src/test/
git add modules/apps/VoiceAccessibility/src/androidTest/
git add modules/apps/VoiceCursor/src/test/

# Commit
git commit -m "$(cat <<'EOF'
test: add comprehensive test suite for CommandManager integration

Unit Tests (350):
- ServiceMonitor: 30 tests (health checks, reconnection)
- Tiered Caching: 40 tests (cache lookups, LRU eviction)
- Intent Dispatcher: 40 tests (confidence scoring, routing)
- Action Types: 165 tests (11 action types × 15 tests each)
- Learning System: 40 tests (scoring, context rotation)
- Context Manager: 30 tests (multi-app loading, screen detection)
- Plugin Manager: 50 tests (loading, security, timeout)
- Permissions: 20 tests (dual mode, detection)

Integration Tests (125):
- VoiceOSService ↔ CommandManager: 40 tests
- CommandManager ↔ VoiceCursor: 20 tests
- CommandManager ↔ Plugin System: 30 tests
- Learning ↔ Context: 20 tests
- Database ↔ Cache: 15 tests

E2E Tests (25):
- Full voice command flows
- Multi-app context switching
- Plugin loading and execution
- Dual mode scenarios
- Learning adaptation

Coverage: 80% achieved
Performance: <100ms command resolution validated
EOF
)"
```

---

### Task 5.4: Push to Remote

```bash
# Push all commits
git push origin vos4-legacyintegration

# Create pull request if ready
gh pr create --title "feat: CommandManager → VoiceOSService Integration" \
             --body "$(cat <<'EOF'
## Summary

Comprehensive integration of CommandManager with VoiceOSService for voice command processing in VOS4.

### Key Features
- **Service Monitor:** Lifecycle management with health checks and automatic recovery
- **3-Tier Caching:** <100ms command resolution (Tier 1/2/3 architecture)
- **8 New Action Types:** Dictation, Cursor, Editing, Apps, Gestures, Overlays, Notifications, Shortcuts
- **Learning System:** Hybrid learning with multi-app tracking and context rotation
- **Plugin System:** Full extensibility with security sandboxing
- **Dual Mode Support:** All features work with or without overlay permissions

### Testing
- ✅ 500 tests (350 unit + 125 integration + 25 E2E)
- ✅ 80% test coverage achieved
- ✅ Performance benchmarks passing (<100ms)
- ✅ Security validation (signature verification, sandboxing)

### Breaking Changes
- **VoiceCursor:** Command logic moved to CommandManager/CursorActions (100% functionally equivalent)

### Documentation
- ✅ All changelogs updated
- ✅ API documentation complete
- ✅ Architecture diagrams added
- ✅ User manual updated with new commands
- ✅ Developer manual updated with plugin guide

### Timeline
Implemented over 6 weeks following comprehensive Q&A session (12 architectural decisions).

### Next Steps
- Monitor performance metrics in production
- Gather user feedback on new action types
- Iterate on learning system effectiveness
- Promote Q3 stubbed enhancements (predictive preloading, cache warming) to implementation

---

Based on Q&A session documented in:
- `/coding/STATUS/CommandManager-QA-Session-Precompaction-Context-251010-1707.md`
- `/coding/STATUS/CommandManager-VoiceOSService-Integration-QA-Status-251010-1731.md`
EOF
)"
```

---

## Summary Checklist

**Before Starting Implementation:**
- [ ] Read this entire document
- [ ] Review Q&A session documents:
  - [ ] `/coding/STATUS/CommandManager-QA-Session-Precompaction-Context-251010-1707.md`
  - [ ] `/coding/STATUS/CommandManager-VoiceOSService-Integration-QA-Status-251010-1731.md`
- [ ] Confirm VoiceCursor refactoring is critical path (MUST do first)

**Phase 0: VoiceCursor (Week 1) - BLOCKING**
- [ ] Audit VoiceCursor architecture
- [ ] Design VoiceCursorAPI
- [ ] Extract command logic to CursorActions
- [ ] Integration testing
- [ ] User approval before proceeding

**Phase 1: Core Infrastructure (Week 2)**
- [ ] Service Monitor (Q1)
- [ ] Tiered Caching (Q3)
- [ ] Intent Dispatcher (Q4)

**Phase 2: Action Types (Weeks 3-4)**
- [ ] DictationActions (Q6)
- [ ] EditingActions
- [ ] AppActions
- [ ] GestureActions
- [ ] OverlayActions
- [ ] NotificationActions
- [ ] ShortcutActions

**Phase 3: Intelligence (Week 5)**
- [ ] Learning System (Q11)
- [ ] Context Manager (Q10)
- [ ] Macro Support (Q9)

**Phase 4: Extensibility & Testing (Week 6)**
- [ ] Plugin System (Q12)
- [ ] Dual Mode Permissions (Q7)
- [ ] Complete Test Suite (Q8)

**Post-Implementation:**
- [ ] Update all documentation
- [ ] Update master TODO/backlog
- [ ] Commit with proper staging (docs → code → tests)
- [ ] Push to remote
- [ ] Create pull request

---

## Questions or Blockers?

If you encounter any issues during implementation:

1. **VoiceCursor Refactoring Blockers:** If VoiceCursor architecture is more complex than expected, STOP and consult before proceeding. Do not make assumptions.

2. **Security Concerns (Plugin System):** If you identify security vulnerabilities in the plugin system design, STOP and document them. Security review is mandatory before plugin marketplace.

3. **Performance Issues:** If <100ms target cannot be achieved, document bottlenecks and propose optimizations. Do not proceed without addressing performance.

4. **Test Coverage Issues:** If 80% coverage cannot be achieved, identify gaps and propose additional tests. Do not merge without coverage target.

**Contact:** Refer to user for clarification on any architectural decisions not covered in Q&A session.

---

**Document Status:** ✅ COMPLETE AND READY FOR IMPLEMENTATION
**Last Updated:** 2025-10-10 17:34 PDT
**Estimated Timeline:** 4-6 weeks (1 developer full-time)
**Total LOC Estimate:** ~6,400 lines production + ~500 test lines

**GOOD LUCK WITH IMPLEMENTATION! 🚀**
