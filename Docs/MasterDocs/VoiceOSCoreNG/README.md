# VoiceOSCoreNG - Voice Command Processing Engine

**Version:** 1.0.0 | **Platform:** Kotlin Multiplatform (KMP) | **Last Updated:** 2026-01-11

---

## Executive Summary

VoiceOSCoreNG is the core voice command processing engine powering the VoiceOS accessibility platform. Built with Kotlin Multiplatform (KMP), it provides a unified architecture for voice command recognition, dynamic UI element discovery, and action execution across Android, iOS, and Desktop platforms.

### Key Capabilities

| Capability | Description |
|------------|-------------|
| **Voice Command Processing** | 5-level priority system for command matching and execution |
| **Dynamic UI Discovery** | Real-time screen scanning and element identification |
| **Multi-Engine Speech** | Support for 6 speech recognition engines |
| **NLU Integration** | BERT-based natural language understanding |
| **LLM Fallback** | Natural language interpretation via Claude/OpenAI |
| **Cross-Platform** | Single codebase for Android, iOS, Desktop |

---

## Table of Contents

1. [Architecture Overview](#architecture-overview)
2. [Quick Start Guide](#quick-start-guide)
3. [Core Components](#core-components)
4. [Command Processing Pipeline](#command-processing-pipeline)
5. [Handler System](#handler-system)
6. [Speech Engine Integration](#speech-engine-integration)
7. [API Reference](#api-reference)
8. [Configuration](#configuration)
9. [For Marketing](#for-marketing)

---

## Architecture Overview

### High-Level System Diagram

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           VoiceOSCoreNG Facade                               │
│  ┌─────────────────────────────────────────────────────────────────────┐    │
│  │  Entry Point: VoiceOSCoreNG.processCommand(text)                    │    │
│  │  Builder Pattern | Lifecycle Management | State Flows               │    │
│  └─────────────────────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
          ┌───────────────────────────┼───────────────────────────┐
          ▼                           ▼                           ▼
┌──────────────────┐     ┌──────────────────────┐     ┌──────────────────┐
│ ActionCoordinator│     │   HandlerRegistry    │     │   SpeechEngine   │
│                  │     │                      │     │                  │
│ • Routes commands│     │ • Priority-based     │     │ • Voice input    │
│ • 5-level match  │     │ • Category lookup    │     │ • Multi-engine   │
│ • NLU/LLM bridge │     │ • Handler lifecycle  │     │ • Grammar update │
└──────────────────┘     └──────────────────────┘     └──────────────────┘
          │                           │                           │
          ▼                           ▼                           ▼
┌──────────────────┐     ┌──────────────────────┐     ┌──────────────────┐
│  NLU Processor   │     │      Handlers        │     │  Engine Types    │
│  LLM Processor   │     │                      │     │                  │
│                  │     │ • SystemHandler      │     │ • Android STT    │
│ • BERT classify  │     │ • NavigationHandler  │     │ • Google Cloud   │
│ • Claude/OpenAI  │     │ • UIHandler          │     │ • Azure Speech   │
│ • Intent mapping │     │ • InputHandler       │     │ • Vivoka SDK     │
└──────────────────┘     │ • AppHandler         │     │ • Vosk (offline) │
                         └──────────────────────┘     │ • Apple Speech   │
                                      │               └──────────────────┘
                                      ▼
                         ┌──────────────────────┐
                         │  Platform Executors  │
                         │                      │
                         │ Android:             │
                         │ • AccessibilityService│
                         │ • PackageManager     │
                         │ • WindowManager      │
                         └──────────────────────┘
```

### Module Structure

```
VoiceOSCoreNG/
├── src/
│   ├── commonMain/kotlin/com/augmentalis/voiceoscoreng/
│   │   ├── VoiceOSCoreNG.kt          # Main facade (698 lines)
│   │   ├── handlers/                  # Command handlers
│   │   │   ├── ActionCoordinator.kt   # Routes commands (662 lines)
│   │   │   ├── HandlerRegistry.kt     # Handler management (283 lines)
│   │   │   ├── IHandler.kt            # Handler interface (140 lines)
│   │   │   ├── SystemHandler.kt       # System commands (119 lines)
│   │   │   ├── NavigationHandler.kt   # Scroll/swipe (142 lines)
│   │   │   ├── UIHandler.kt           # Click/tap actions
│   │   │   ├── InputHandler.kt        # Text input
│   │   │   └── AppHandler.kt          # App launching
│   │   ├── common/                    # Shared models
│   │   │   ├── CommandRegistry.kt     # Dynamic commands (139 lines)
│   │   │   ├── QuantizedCommand.kt    # Command model (98 lines)
│   │   │   ├── CommandActionType.kt   # 30+ action types (219 lines)
│   │   │   └── StaticCommandRegistry.kt # 50+ predefined (521 lines)
│   │   ├── features/                  # Speech engines
│   │   │   ├── ISpeechEngine.kt       # Engine interface (235 lines)
│   │   │   └── SpeechConfig.kt        # Configuration
│   │   ├── nlu/                       # NLU integration
│   │   └── llm/                       # LLM integration
│   │
│   ├── androidMain/kotlin/com/augmentalis/voiceoscoreng/
│   │   ├── core/VoiceOSCoreNG.kt      # Android initialization
│   │   ├── AndroidHandlerFactory.kt   # Creates Android handlers
│   │   ├── handlers/                  # Android executors
│   │   │   ├── AndroidSystemExecutor.kt
│   │   │   ├── AndroidNavigationExecutor.kt
│   │   │   ├── AndroidUIExecutor.kt
│   │   │   ├── AndroidInputExecutor.kt
│   │   │   └── AndroidAppLauncher.kt
│   │   └── features/                  # Android speech engines
│   │
│   ├── iosMain/                       # iOS implementations
│   └── desktopMain/                   # Desktop implementations
│
├── build.gradle.kts                   # KMP build configuration
└── docs/                              # Module documentation
```

---

## Quick Start Guide

### For Novice Developers

#### What is VoiceOSCoreNG?

VoiceOSCoreNG is like a "brain" that listens to voice commands and decides what to do with them. When a user says "scroll down" or "click button 3", this module:

1. **Hears** the command (via speech recognition)
2. **Understands** what the user wants (via NLU/matching)
3. **Executes** the action (via platform-specific handlers)

#### Basic Setup (Android)

```kotlin
// Step 1: Get reference to your AccessibilityService
class MyAccessibilityService : AccessibilityService() {

    private lateinit var voiceCore: VoiceOSCoreNG

    override fun onServiceConnected() {
        super.onServiceConnected()

        // Step 2: Create VoiceOSCoreNG instance
        voiceCore = VoiceOSCoreNG.createForAndroid(this)

        // Step 3: Initialize (this is async)
        lifecycleScope.launch {
            voiceCore.initialize()
            // Now ready to process commands!
        }
    }

    // Step 4: Process voice commands
    fun handleVoiceInput(spokenText: String) {
        lifecycleScope.launch {
            val result = voiceCore.processCommand(spokenText)
            when (result) {
                is HandlerResult.Success -> showFeedback("Done: ${result.message}")
                is HandlerResult.Failure -> showError(result.reason)
                else -> { /* Handle other cases */ }
            }
        }
    }
}
```

### For Expert Developers

#### Advanced Setup with Full Configuration

```kotlin
// Create with full configuration and persistence
val voiceCore = VoiceOSCoreNG.Builder()
    // Handler factory creates platform-specific executors
    .withHandlerFactory(AndroidHandlerFactory.create(accessibilityService))

    // Custom configuration
    .withConfiguration(ServiceConfiguration(
        voiceLanguage = "en-US",
        confidenceThreshold = 0.7f,
        enableWakeWord = true,
        wakeWord = "hey voice",
        speechEngine = "VIVOKA",
        autoStartListening = true,
        synonymsEnabled = true,
        debugMode = BuildConfig.DEBUG
    ))

    // Optional: Share command registry with UI layer
    .withCommandRegistry(sharedCommandRegistry)

    // Optional: Add synonym matching
    .withSynonymProvider(englishSynonymProvider)

    // Optional: Add NLU processor (BERT-based)
    .withNluProcessor(
        androidNluProcessor,
        NluConfig(confidenceThreshold = 0.8f, maxAlternatives = 3)
    )

    // Optional: Add LLM fallback (Claude/OpenAI)
    .withLlmProcessor(
        claudeLlmProcessor,
        LlmConfig(modelId = "claude-3-haiku", temperature = 0.3f)
    )

    // Optional: Persist static commands to database
    .withStaticCommandPersistence(staticCommandPersistence)

    .build()

// Initialize with coroutine scope
coroutineScope.launch {
    voiceCore.initialize()

    // Observe state changes
    voiceCore.state.collect { state ->
        when (state) {
            is ServiceState.Ready -> enableVoiceUI()
            is ServiceState.Listening -> showListeningIndicator()
            is ServiceState.Processing -> showProcessingIndicator()
            is ServiceState.Error -> handleError(state.exception)
            else -> { }
        }
    }
}
```

---

## Core Components

### 1. VoiceOSCoreNG (Main Facade)

The primary entry point for all voice command operations.

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/VoiceOSCoreNG.kt`

```kotlin
class VoiceOSCoreNG private constructor(builder: Builder) {

    // ═══════════════════════════════════════════════════════════════
    // LIFECYCLE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Initialize all subsystems (handlers, speech engine, NLU, LLM)
     * Must be called before processing commands
     */
    suspend fun initialize()

    /**
     * Clean up all resources
     * Call when service is being destroyed
     */
    suspend fun dispose()

    // ═══════════════════════════════════════════════════════════════
    // COMMAND PROCESSING
    // ═══════════════════════════════════════════════════════════════

    /**
     * Process a voice command (primary entry point)
     *
     * @param text The spoken text to process
     * @param confidence Recognition confidence (0.0-1.0)
     * @return HandlerResult indicating success/failure
     *
     * Example: processCommand("scroll down", 0.95f)
     */
    suspend fun processCommand(text: String, confidence: Float = 1.0f): HandlerResult

    /**
     * Process a structured command directly
     * Use when you've already matched the command
     */
    suspend fun processCommand(command: QuantizedCommand): HandlerResult

    // ═══════════════════════════════════════════════════════════════
    // VOICE RECOGNITION CONTROL
    // ═══════════════════════════════════════════════════════════════

    /**
     * Start listening for voice input
     * Requires speech engine to be initialized
     */
    suspend fun startListening(): Result<Unit>

    /**
     * Stop listening for voice input
     */
    suspend fun stopListening()

    /**
     * Update speech engine vocabulary with new commands
     * Call after screen changes to add dynamic commands
     */
    suspend fun updateCommands(commands: List<String>): Result<Unit>

    // ═══════════════════════════════════════════════════════════════
    // DYNAMIC COMMAND MANAGEMENT
    // ═══════════════════════════════════════════════════════════════

    /**
     * Update screen-specific commands (called on navigation)
     *
     * @param commands List of commands for current screen
     * @param updateSpeechEngine Whether to update speech grammar
     */
    suspend fun updateDynamicCommands(
        commands: List<QuantizedCommand>,
        updateSpeechEngine: Boolean = true
    ): Result<Unit>

    /**
     * Clear all dynamic commands (call on screen exit)
     */
    fun clearDynamicCommands()

    /**
     * Number of dynamic commands currently registered
     */
    val dynamicCommandCount: Int

    // ═══════════════════════════════════════════════════════════════
    // STATE & OBSERVABLES
    // ═══════════════════════════════════════════════════════════════

    /**
     * Current service state (Uninitialized, Ready, Listening, etc.)
     */
    val state: StateFlow<ServiceState>

    /**
     * Stream of command execution results
     */
    val commandResults: SharedFlow<CommandResult>

    /**
     * Stream of speech recognition results
     */
    val speechResults: Flow<SpeechResult>
}
```

### 2. ActionCoordinator (Command Router)

Routes voice commands through a 5-level priority matching system.

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/ActionCoordinator.kt`

#### Command Processing Flow Diagram

```
                        ┌─────────────────────────────┐
                        │   Voice Input Received      │
                        │   "click number 4"          │
                        └─────────────┬───────────────┘
                                      │
                        ┌─────────────▼───────────────┐
                        │   LEVEL 1: Exact Match      │
                        │   CommandRegistry.findBy    │
                        │   Phrase("4")               │
                        └─────────────┬───────────────┘
                                      │
                    ┌─────Found───────┴───────Not Found──────┐
                    │                                        │
                    ▼                                        ▼
        ┌───────────────────┐               ┌─────────────────────────┐
        │ Execute via VUID  │               │   LEVEL 2: Fuzzy Match  │
        │ targetVuid: btn_4 │               │   Threshold: 0.7        │
        └───────────────────┘               └─────────────┬───────────┘
                                                          │
                                      ┌─────Found─────────┴───────Not Found──────┐
                                      │                                          │
                                      ▼                                          ▼
                          ┌───────────────────┐               ┌───────────────────────────┐
                          │ Execute matched   │               │   LEVEL 3: Static Handler │
                          │ (confidence ≥0.85)│               │   HandlerRegistry.find    │
                          └───────────────────┘               └─────────────┬─────────────┘
                                                                            │
                                                        ┌─────Found─────────┴───────Not Found──────┐
                                                        │                                          │
                                                        ▼                                          ▼
                                            ┌───────────────────┐               ┌───────────────────────────┐
                                            │ Execute handler   │               │   LEVEL 4: NLU (BERT)     │
                                            │ e.g., "scroll up" │               │   Semantic classification │
                                            └───────────────────┘               └─────────────┬─────────────┘
                                                                                              │
                                                                          ┌─────Match────────┴───────No Match──────┐
                                                                          │                                        │
                                                                          ▼                                        ▼
                                                              ┌───────────────────┐               ┌─────────────────────────┐
                                                              │ Execute NLU match │               │   LEVEL 5: LLM Fallback │
                                                              └───────────────────┘               │   Natural language      │
                                                                                                  └─────────────┬───────────┘
                                                                                                                │
                                                                                            ┌─────Interpreted───┴───────Failed───────┐
                                                                                            │                                        │
                                                                                            ▼                                        ▼
                                                                                ┌───────────────────┐               ┌───────────────────────────┐
                                                                                │ Execute LLM      │               │ HandlerResult.Failure     │
                                                                                │ interpretation   │               │ "Command not recognized"  │
                                                                                └───────────────────┘               └───────────────────────────┘
```

### 3. HandlerRegistry (Handler Management)

Priority-based registry for action handlers.

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/handlers/HandlerRegistry.kt`

#### Handler Priority Order

```
Priority 1:  SYSTEM       ─► back, home, recents, notifications
Priority 2:  NAVIGATION   ─► scroll up/down/left/right, swipe
Priority 3:  APP          ─► open app, launch, switch app
Priority 4:  GAZE         ─► eye tracking actions (future)
Priority 5:  GESTURE      ─► pinch, zoom, drag (future)
Priority 6:  UI           ─► click, tap, long press, focus
Priority 7:  DEVICE       ─► volume, brightness, flashlight
Priority 8:  INPUT        ─► type text, paste, clear
Priority 9:  MEDIA        ─► play, pause, next, previous
Priority 10: ACCESSIBILITY─► announce, describe, magnify
Priority 11: CUSTOM       ─► user-defined handlers
```

### 4. StaticCommandRegistry (Predefined Commands)

Contains 50+ predefined voice commands organized by category.

**File:** `src/commonMain/kotlin/com/augmentalis/voiceoscoreng/common/StaticCommandRegistry.kt`

#### Available Commands by Category

```
┌────────────────────────────────────────────────────────────────────────────┐
│                         STATIC COMMAND REGISTRY                            │
├────────────────┬───────────────────────────────────────────────────────────┤
│ NAVIGATION     │ "go back", "back", "home", "go home", "show recents",    │
│ (12 phrases)   │ "recent apps", "open notifications", "notification panel"│
├────────────────┼───────────────────────────────────────────────────────────┤
│ MEDIA          │ "play", "pause", "stop", "next", "previous", "skip",     │
│ (22 phrases)   │ "volume up", "volume down", "mute", "unmute",            │
│                │ "play music", "pause music", "next track"                 │
├────────────────┼───────────────────────────────────────────────────────────┤
│ SYSTEM         │ "open settings", "settings", "take screenshot",          │
│ (20 phrases)   │ "screenshot", "flashlight on", "flashlight off",         │
│                │ "torch on", "torch off", "lock screen", "power off"      │
├────────────────┼───────────────────────────────────────────────────────────┤
│ VOICEOS        │ "stop listening", "start listening", "voice off",        │
│ (23 phrases)   │ "voice on", "show numbers", "hide numbers",              │
│                │ "numbers on", "numbers off", "show commands",            │
│                │ "what can I say", "dictation mode", "start dictation"    │
├────────────────┼───────────────────────────────────────────────────────────┤
│ APP LAUNCH     │ "open [app]", "launch [app]", "start [app]",             │
│ (16 phrases)   │ "open camera", "open browser", "open messages",          │
│                │ "open phone", "open email", "open calendar"              │
├────────────────┼───────────────────────────────────────────────────────────┤
│ ACCESSIBILITY  │ "describe screen", "what's on screen", "read aloud",     │
│ (8 phrases)    │ "announce", "magnify", "zoom in", "zoom out"             │
└────────────────┴───────────────────────────────────────────────────────────┘
```

---

## Handler System

### Handler Interface

Every handler implements the `IHandler` interface:

```kotlin
interface IHandler {
    /**
     * Category this handler belongs to (for priority routing)
     */
    val category: ActionCategory

    /**
     * List of actions this handler supports
     * Example: ["back", "home", "recents"] for SystemHandler
     */
    val supportedActions: List<String>

    /**
     * Execute a command
     *
     * @param command The structured command to execute
     * @param params Additional parameters (optional)
     * @return HandlerResult indicating outcome
     */
    suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any> = emptyMap()
    ): HandlerResult

    /**
     * Check if this handler can handle the given action
     */
    fun canHandle(action: String): Boolean

    /**
     * Initialize the handler (called once on startup)
     */
    suspend fun initialize()

    /**
     * Clean up resources (called on shutdown)
     */
    suspend fun dispose()
}
```

### Android Handlers

#### SystemHandler + AndroidSystemExecutor

```kotlin
// Handles: back, home, recents, notifications, quick settings
class SystemHandler(
    private val executor: SystemExecutor
) : BaseHandler(ActionCategory.SYSTEM) {

    override val supportedActions = listOf(
        "back", "go back",
        "home", "go home",
        "recents", "recent apps", "show recents",
        "notifications", "notification panel", "open notifications",
        "quick settings"
    )

    override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
        return when (command.actionType) {
            CommandActionType.BACK -> executor.goBack()
            CommandActionType.HOME -> executor.goHome()
            CommandActionType.RECENT_APPS -> executor.showRecents()
            CommandActionType.NOTIFICATIONS -> executor.openNotifications()
            else -> HandlerResult.NotHandled()
        }
    }
}

// Android implementation using AccessibilityService
class AndroidSystemExecutor(
    private val serviceProvider: () -> AccessibilityService?
) : SystemExecutor {

    override suspend fun goBack(): HandlerResult {
        val service = serviceProvider() ?: return HandlerResult.Failure("Service not available")
        val success = service.performGlobalAction(GLOBAL_ACTION_BACK)
        return if (success) HandlerResult.Success("Navigated back")
               else HandlerResult.Failure("Back action failed")
    }

    override suspend fun goHome(): HandlerResult {
        val service = serviceProvider() ?: return HandlerResult.Failure("Service not available")
        val success = service.performGlobalAction(GLOBAL_ACTION_HOME)
        return if (success) HandlerResult.Success("Navigated home")
               else HandlerResult.Failure("Home action failed")
    }

    // ... other implementations
}
```

#### NavigationHandler + AndroidNavigationExecutor

```kotlin
// Handles: scroll, swipe, page navigation
class NavigationHandler(
    private val executor: NavigationExecutor
) : BaseHandler(ActionCategory.NAVIGATION) {

    override val supportedActions = listOf(
        "scroll up", "scroll down", "scroll left", "scroll right",
        "swipe up", "swipe down", "swipe left", "swipe right",
        "page up", "page down", "next page", "previous page"
    )

    override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
        return when (command.actionType) {
            CommandActionType.SCROLL_UP -> executor.scrollUp()
            CommandActionType.SCROLL_DOWN -> executor.scrollDown()
            CommandActionType.SCROLL_LEFT -> executor.scrollLeft()
            CommandActionType.SCROLL_RIGHT -> executor.scrollRight()
            else -> HandlerResult.NotHandled()
        }
    }
}

// Android implementation
class AndroidNavigationExecutor(
    private val serviceProvider: () -> AccessibilityService?
) : NavigationExecutor {

    override suspend fun scrollDown(): HandlerResult {
        val service = serviceProvider() ?: return HandlerResult.Failure("Service not available")

        // Find scrollable node in current window
        val rootNode = service.rootInActiveWindow ?: return HandlerResult.Failure("No active window")
        val scrollableNode = findScrollableNode(rootNode)

        return if (scrollableNode != null) {
            val success = scrollableNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            scrollableNode.recycle()
            if (success) HandlerResult.Success("Scrolled down")
            else HandlerResult.Failure("Scroll action failed")
        } else {
            // Fallback: gesture-based scroll
            performScrollGesture(service, ScrollDirection.DOWN)
        }
    }

    private fun findScrollableNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        // BFS to find first scrollable node
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (node.isScrollable) return node

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }
}
```

#### UIHandler + AndroidUIExecutor

```kotlin
// Handles: click, tap, long press, focus
class UIHandler(
    private val executor: UIExecutor
) : BaseHandler(ActionCategory.UI) {

    override val supportedActions = listOf(
        "click", "tap", "press",
        "long click", "long press", "hold",
        "focus", "select"
    )

    override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
        val targetVuid = command.targetVuid
            ?: return HandlerResult.Failure("No target element specified")

        return when (command.actionType) {
            CommandActionType.CLICK -> executor.click(targetVuid)
            CommandActionType.LONG_CLICK -> executor.longClick(targetVuid)
            CommandActionType.FOCUS -> executor.focus(targetVuid)
            else -> HandlerResult.NotHandled()
        }
    }
}

// Android implementation
class AndroidUIExecutor(
    private val serviceProvider: () -> AccessibilityService?,
    private val vuidResolver: VuidResolver
) : UIExecutor {

    override suspend fun click(vuid: String): HandlerResult {
        val service = serviceProvider() ?: return HandlerResult.Failure("Service not available")
        val rootNode = service.rootInActiveWindow ?: return HandlerResult.Failure("No active window")

        // Resolve VUID to AccessibilityNodeInfo
        val targetNode = vuidResolver.resolve(rootNode, vuid)
            ?: return HandlerResult.Failure("Element not found: $vuid")

        return try {
            val success = targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            if (success) {
                HandlerResult.Success("Clicked element")
            } else {
                // Fallback: click at element center coordinates
                val bounds = Rect()
                targetNode.getBoundsInScreen(bounds)
                performTapGesture(service, bounds.centerX(), bounds.centerY())
            }
        } finally {
            targetNode.recycle()
        }
    }
}
```

#### AppHandler + AndroidAppLauncher

```kotlin
// Handles: app launch, switch, close
class AppHandler(
    private val launcher: AppLauncher
) : BaseHandler(ActionCategory.APP) {

    override val supportedActions = listOf(
        "open", "launch", "start", "run",
        "switch to", "go to"
    )

    override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
        val appName = extractAppName(command.phrase)
            ?: return HandlerResult.Failure("No app name specified")

        return launcher.launchApp(appName)
    }
}

// Android implementation with smart app discovery
class AndroidAppLauncher(
    private val context: Context
) : AppLauncher {

    // Cache of installed apps with aliases
    private val appCache: MutableMap<String, ResolvedApp> = mutableMapOf()

    init {
        // Discover installed apps on initialization
        discoverInstalledApps()
    }

    private fun discoverInstalledApps() {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        pm.queryIntentActivities(intent, 0).forEach { resolveInfo ->
            val appName = resolveInfo.loadLabel(pm).toString()
            val packageName = resolveInfo.activityInfo.packageName

            // Generate aliases (lowercase, without spaces, common names)
            val aliases = generateAliases(appName)

            aliases.forEach { alias ->
                appCache[alias] = ResolvedApp(packageName, appName)
            }
        }
    }

    private fun generateAliases(appName: String): List<String> {
        return listOf(
            appName.lowercase(),
            appName.lowercase().replace(" ", ""),
            // Common substitutions
            appName.lowercase().replace("google ", ""),
            // ... more alias generation
        )
    }

    override suspend fun launchApp(appName: String): HandlerResult {
        val resolved = appCache[appName.lowercase()]
            ?: return HandlerResult.Failure("App not found: $appName")

        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(resolved.packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                HandlerResult.Success("Launched ${resolved.displayName}")
            } else {
                HandlerResult.Failure("Cannot launch ${resolved.displayName}")
            }
        } catch (e: Exception) {
            HandlerResult.Failure("Failed to launch: ${e.message}")
        }
    }
}
```

---

## Speech Engine Integration

### Supported Engines

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        SPEECH ENGINE COMPARISON                             │
├─────────────────┬───────────┬─────────────┬────────────┬───────────────────┤
│ Engine          │ Offline   │ Accuracy    │ Languages  │ Best For          │
├─────────────────┼───────────┼─────────────┼────────────┼───────────────────┤
│ Android STT     │ Partial*  │ Good        │ 100+       │ General use       │
│ Google Cloud    │ No        │ Excellent   │ 125+       │ High accuracy     │
│ Azure Speech    │ No        │ Excellent   │ 100+       │ Enterprise        │
│ Vivoka SDK      │ Yes       │ Very Good   │ 25+        │ Embedded/offline  │
│ Vosk            │ Yes       │ Good        │ 20+        │ Privacy/offline   │
│ Apple Speech    │ Partial*  │ Very Good   │ 60+        │ iOS native        │
└─────────────────┴───────────┴─────────────┴────────────┴───────────────────┘
* Requires model download for offline use
```

### Speech Engine Interface

```kotlin
interface ISpeechEngine {
    /**
     * Current engine state
     */
    val state: StateFlow<EngineState>

    /**
     * Stream of recognition results
     */
    val results: Flow<SpeechResult>

    /**
     * Stream of errors
     */
    val errors: Flow<SpeechError>

    /**
     * Initialize the engine with configuration
     */
    suspend fun initialize(config: SpeechConfig): Result<Unit>

    /**
     * Start listening for speech
     */
    suspend fun startListening(): Result<Unit>

    /**
     * Stop listening
     */
    suspend fun stopListening()

    /**
     * Update vocabulary/grammar with new commands
     * Improves recognition accuracy for app-specific terms
     */
    suspend fun updateCommands(commands: List<String>): Result<Unit>

    /**
     * Check if engine supports a feature
     */
    fun getSupportedFeatures(): Set<EngineFeature>

    /**
     * Clean up resources
     */
    suspend fun destroy()
}

// Engine states
sealed class EngineState {
    object Uninitialized : EngineState()
    object Initializing : EngineState()
    object Ready : EngineState()
    object Listening : EngineState()
    object Processing : EngineState()
    data class Error(val message: String) : EngineState()
    object Destroyed : EngineState()
}

// Recognition result
data class SpeechResult(
    val text: String,              // Recognized text
    val confidence: Float,         // 0.0 - 1.0
    val isFinal: Boolean,          // Is this the final result?
    val alternatives: List<String> // Alternative interpretations
)

// Engine features
enum class EngineFeature {
    OFFLINE_MODE,              // Works without internet
    CONTINUOUS_RECOGNITION,    // Listen continuously
    WAKE_WORD,                 // "Hey Voice" trigger
    PUNCTUATION,               // Automatic punctuation
    SPEAKER_DIARIZATION,       // Identify speakers
    CUSTOM_VOCABULARY          // Add custom words
}
```

---

## API Reference

### Core Classes

| Class | Purpose | Key Methods |
|-------|---------|-------------|
| `VoiceOSCoreNG` | Main facade | `processCommand()`, `initialize()`, `dispose()` |
| `ActionCoordinator` | Command routing | `processVoiceCommand()`, `updateDynamicCommands()` |
| `HandlerRegistry` | Handler management | `register()`, `findHandler()`, `getAllHandlers()` |
| `CommandRegistry` | Dynamic commands | `update()`, `findByPhrase()`, `findByVuid()` |
| `StaticCommandRegistry` | Predefined commands | `all()`, `findByPhrase()`, `byCategory()` |

### Data Classes

| Class | Purpose | Key Fields |
|-------|---------|------------|
| `QuantizedCommand` | Voice command model | `phrase`, `actionType`, `targetVuid`, `confidence` |
| `StaticCommand` | Predefined command | `phrases`, `actionType`, `category`, `description` |
| `SpeechResult` | Recognition result | `text`, `confidence`, `isFinal`, `alternatives` |
| `HandlerResult` | Execution outcome | Sealed: `Success`, `Failure`, `NotHandled`, etc. |
| `ServiceConfiguration` | Engine config | `voiceLanguage`, `speechEngine`, `confidenceThreshold` |

### Enums

| Enum | Values |
|------|--------|
| `CommandActionType` | CLICK, SCROLL_UP, BACK, HOME, OPEN_APP, VOICE_MUTE, ... (30+) |
| `ActionCategory` | SYSTEM, NAVIGATION, APP, UI, INPUT, MEDIA, ACCESSIBILITY, CUSTOM |
| `CommandCategory` | NAVIGATION, MEDIA, SYSTEM, VOICE_CONTROL, APP_LAUNCH, ACCESSIBILITY |
| `SpeechEngine` | ANDROID_STT, GOOGLE_CLOUD, AZURE, VIVOKA, VOSK, APPLE_SPEECH |

---

## Configuration

### ServiceConfiguration

```kotlin
data class ServiceConfiguration(
    // Voice recognition
    val voiceLanguage: String = "en-US",
    val confidenceThreshold: Float = 0.7f,
    val speechEngine: String = "ANDROID_STT",

    // Wake word
    val enableWakeWord: Boolean = true,
    val wakeWord: String = "hey voice",

    // Feedback
    val enableHapticFeedback: Boolean = true,
    val enableAudioFeedback: Boolean = true,

    // Features
    val fingerprintGesturesEnabled: Boolean = false,
    val autoStartListening: Boolean = true,
    val synonymsEnabled: Boolean = true,
    val synonymLanguage: String? = null,

    // Debug
    val debugMode: Boolean = false
) {
    companion object {
        val DEFAULT = ServiceConfiguration()

        val HIGH_ACCURACY = ServiceConfiguration(
            confidenceThreshold = 0.85f,
            speechEngine = "GOOGLE_CLOUD"
        )

        val OFFLINE = ServiceConfiguration(
            speechEngine = "VOSK",
            enableWakeWord = false
        )
    }
}
```

---

## For Marketing

### Feature Highlights

#### Voice-First Accessibility
- **50+ built-in commands** for navigation, media, and system control
- **Dynamic command discovery** - automatically detects clickable elements
- **Natural language understanding** - say commands naturally, not robotically

#### Multi-Platform
- **Single codebase** for Android, iOS, and Desktop
- **Consistent experience** across all devices
- **Native performance** on each platform

#### Intelligent Recognition
- **5-level command matching** for highest accuracy
- **NLU-powered** understanding with BERT models
- **LLM fallback** for complex natural language

#### Privacy-Focused Options
- **Offline mode** available with Vosk engine
- **On-device processing** - voice never leaves your device
- **No cloud dependency** for basic commands

### Use Cases

1. **Accessibility** - Hands-free device control for users with motor impairments
2. **Smart Home** - Voice control for IoT devices
3. **Automotive** - Safe hands-free phone operation while driving
4. **Industrial** - Voice commands in manufacturing environments
5. **Medical** - Hands-free documentation in healthcare settings

### Technical Specifications

| Specification | Value |
|---------------|-------|
| Min Android Version | API 28 (Android 9) |
| Min iOS Version | iOS 14+ |
| Languages | 100+ (engine dependent) |
| Offline Support | Yes (with Vosk/Vivoka) |
| Command Latency | < 100ms (local), < 500ms (cloud) |
| Memory Footprint | ~50MB (base), +100MB (offline models) |

---

## Related Documentation

- [VoiceOS Android App](../VoiceOS/README.md)
- [NLU Module](../NLU/README.md)
- [LLM Module](../LLM/README.md)
- [Common Libraries](../Common/README.md)

---

**Author:** VoiceOS Team | **Last Updated:** 2026-01-11 | **Version:** 1.0.0
