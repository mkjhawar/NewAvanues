/**
 * VoiceAvanueAccessibilityService.kt - App-level accessibility service
 *
 * Required because:
 * 1. VoiceOSAccessibilityService is abstract (requires getActionCoordinator())
 * 2. Android manifest must declare a concrete service class in the app
 *
 * All core logic is in VoiceOSCore module.
 * This wrapper adds:
 * - CommandOverlayService lifecycle management
 * - DynamicCommandGenerator for numbered badge overlays
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceavanue.service

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.VoiceOSAccessibilityService
import com.augmentalis.voiceoscore.VoiceOSCore
import com.augmentalis.voiceoscore.ActionCoordinator
import com.augmentalis.voiceoscore.BoundsResolver
import com.augmentalis.voiceoscore.CommandRegistry
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.ServiceConfiguration
import com.augmentalis.voiceoscore.SpeechEngine
import com.augmentalis.voiceoscore.NumbersOverlayHandler
import com.augmentalis.voiceoscore.StaticCommandRegistry
import com.augmentalis.voiceoscore.CommandCategory
import com.augmentalis.voiceoscore.WebCommandHandler
import com.augmentalis.voiceoscore.createForAndroid
import com.augmentalis.voiceoscore.NumbersOverlayMode
import com.augmentalis.voiceoscore.OverlayNumberingExecutor
import com.augmentalis.voiceoscore.OverlayStateManager
import com.augmentalis.voiceoscore.SpeechMode
import com.augmentalis.voiceoscore.TARGET_APPS
import com.augmentalis.voiceoscore.wireCursorDependencies
import com.augmentalis.voiceavanue.MainActivity
import com.augmentalis.avanueui.theme.AvanueModuleAccents
import com.augmentalis.avanueui.theme.ModuleAccent
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.data.DeveloperPreferencesRepository
import com.augmentalis.foundation.settings.models.DeveloperSettings
import com.augmentalis.voiceoscore.commandmanager.CommandManager
import com.augmentalis.voicecursor.core.CursorConfig
import com.augmentalis.voicecursor.core.FilterStrength
import com.augmentalis.voicecursor.overlay.CursorOverlayService
import com.augmentalis.devicemanager.DeviceManager
import com.augmentalis.devicemanager.imu.IMUManager
import com.augmentalis.voiceoscore.handlers.ModuleCommandCallbacks
import com.augmentalis.voiceoscore.handlers.VoiceControlCallbacks
import com.augmentalis.voiceoscore.vos.VosFileImporter
import com.augmentalis.voiceoscore.vos.sync.VosSyncManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import com.augmentalis.webavanue.BrowserVoiceOSCallback
import com.augmentalis.webavanue.WebCommandExecutorImpl

private const val TAG = "VoiceAvanueService"

/**
 * Accessibility service wrapper with overlay badge support.
 * Core command generation is in VoiceOSCore; this adds overlay display.
 */
class VoiceAvanueAccessibilityService : VoiceOSAccessibilityService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SyncEntryPoint {
        fun vosSyncManager(): VosSyncManager
    }


    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var voiceOSCore: VoiceOSCore? = null
    private var actionCoordinator: ActionCoordinator? = null
    private var boundsResolver: BoundsResolver? = null
    private var overlayNumberingExecutor: OverlayNumberingExecutor? = null
    private var dynamicCommandGenerator: DynamicCommandGenerator? = null
    private var webCommandHandler: WebCommandHandler? = null
    private var speechCollectorJob: kotlinx.coroutines.Job? = null  // Cancelled in onDestroy
    private var webCommandCollectorJob: kotlinx.coroutines.Job? = null  // Cancelled in onDestroy
    private var cursorSettingsJob: kotlinx.coroutines.Job? = null  // Cancelled in onDestroy
    private var numbersOverlayModeJob: kotlinx.coroutines.Job? = null  // Cancelled in onDestroy

    override fun getActionCoordinator(): ActionCoordinator {
        // Prefer VoiceOSCore's coordinator — it has handlers registered
        // (AndroidGestureHandler, SystemHandler, AppHandler).
        // The bare actionCoordinator created at startup has NO handlers,
        // so findHandler() would always return null.
        voiceOSCore?.actionCoordinator?.let { return it }

        // Fallback for events arriving before VoiceOSCore initializes.
        // Dynamic commands registered here will be re-registered when the
        // next screen change fires after VoiceOSCore is ready.
        return actionCoordinator ?: ActionCoordinator(commandRegistry = CommandRegistry()).also {
            actionCoordinator = it
        }
    }

    override fun getBoundsResolver(): BoundsResolver? = boundsResolver

    override fun onServiceReady() {
        instance = this

        // Start the overlay service
        try {
            if (Settings.canDrawOverlays(applicationContext)) {
                CommandOverlayService.start(applicationContext)
                Log.i(TAG, "CommandOverlayService started")
            } else {
                Log.w(TAG, "Overlay permission not granted, skipping overlay service")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start CommandOverlayService", e)
        }

        // Initialize the numbering executor and dynamic command generator for overlay badges
        val executor = OverlayNumberingExecutor()
        overlayNumberingExecutor = executor
        dynamicCommandGenerator = DynamicCommandGenerator(resources, executor)

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(applicationContext))
                db.waitForInitialization()

                boundsResolver = BoundsResolver(this@VoiceAvanueAccessibilityService)

                // Ensure IMU capabilities are injected before any cursor/sensor code.
                // DeviceManager.imu triggers lazy injectCapabilities() on IMUManager,
                // so sensor properties resolve correctly on first access.
                try {
                    DeviceManager.getInstance(applicationContext).imu
                } catch (e: Exception) {
                    Log.w(TAG, "DeviceManager IMU init failed (non-fatal): ${e.message}")
                }

                // Read developer settings from DataStore
                val devPrefs = DeveloperPreferencesRepository(applicationContext)
                val devSettings: DeveloperSettings = devPrefs.settings.first()

                val registry = CommandRegistry()
                actionCoordinator = ActionCoordinator(commandRegistry = registry)

                voiceOSCore = VoiceOSCore.createForAndroid(
                    service = this@VoiceAvanueAccessibilityService,
                    configuration = ServiceConfiguration(
                        speechEngine = devSettings.sttEngine,
                        voiceLanguage = devSettings.voiceLanguage,
                        confidenceThreshold = devSettings.confidenceThreshold,
                        autoStartListening = devSettings.autoStartListening,
                        synonymsEnabled = devSettings.synonymsEnabled,
                        debugMode = devSettings.debugMode
                    ),
                    commandRegistry = registry
                )
                voiceOSCore?.initialize()

                // Initialize CommandManager to build pattern matching cache + populate
                // StaticCommandRegistry from DB. VoiceOSCore.initialize() already loaded
                // commands to DB via StaticCommandPersistenceImpl; CommandManager's version
                // check (v2.1) will skip re-loading since persistence already loaded.
                CommandManager.getInstance(applicationContext).initialize()

                // Register WebCommandHandler for web voice command routing
                val handler = WebCommandHandler()
                webCommandHandler = handler
                voiceOSCore?.actionCoordinator?.registerHandler(handler)
                Log.i(TAG, "WebCommandHandler registered with ActionCoordinator")

                // Register NumbersOverlayHandler for "numbers on/off/auto" voice commands
                overlayNumberingExecutor?.let { exec ->
                    val numbersHandler = NumbersOverlayHandler(exec)
                    voiceOSCore?.actionCoordinator?.registerHandler(numbersHandler)
                    Log.i(TAG, "NumbersOverlayHandler registered with ActionCoordinator")
                }

                // Force screen refresh so dynamic commands register on VoiceOSCore's
                // coordinator. Before init, commands were registered on a fallback
                // coordinator with a different CommandRegistry. The screen hash cache
                // prevents re-scanning the same screen, so we must clear it explicitly.
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    refreshScreen()
                }

                // Bridge: Collect speech recognition results → route to command execution
                // Without this, Vivoka emits recognized commands but nobody processes them.
                //
                // Mode-aware routing:
                // - COMBINED/STATIC/DYNAMIC: route to processVoiceCommand() (normal)
                // - MUTED: only wake commands pass through (defense-in-depth — grammar already restricted)
                // - DICTATION: non-exit text injected into focused input field; exit commands route normally
                val confidenceThreshold = devSettings.confidenceThreshold
                speechCollectorJob?.cancel()
                speechCollectorJob = serviceScope.launch {
                    voiceOSCore?.speechResults
                        ?.conflate()  // Drop intermediate results if processing is slow
                        ?.collect { result ->
                            if (result.isFinal && result.text.isNotBlank() && result.confidence >= confidenceThreshold) {
                                Log.d(TAG, "Voice recognized: '${result.text}' (conf: ${result.confidence})")

                                val currentMode = voiceOSCore?.speechMode
                                when (currentMode) {
                                    SpeechMode.MUTED -> {
                                        // In MUTED mode, only wake commands should arrive (grammar is restricted).
                                        // Route them to processVoiceCommand so VoiceControlHandler triggers onWakeVoice.
                                        Log.d(TAG, "MUTED mode — routing wake command: '${result.text}'")
                                        processVoiceCommand(result.text, result.confidence)
                                    }
                                    SpeechMode.DICTATION -> {
                                        // Check if this is an exit command (e.g., "stop dictation")
                                        val isExitCommand = StaticCommandRegistry.findById("voice_dict_stop")
                                            ?.let { cmd ->
                                                cmd.phrases.any { phrase -> phrase.equals(result.text, ignoreCase = true) }
                                            } ?: false

                                        if (isExitCommand) {
                                            Log.d(TAG, "DICTATION exit command detected: '${result.text}'")
                                            processVoiceCommand(result.text, result.confidence)
                                        } else {
                                            // Inject recognized text into focused input field
                                            Log.d(TAG, "DICTATION text injection: '${result.text}'")
                                            injectDictationText(result.text)
                                        }
                                    }
                                    null -> {
                                        // VoiceOSCore not initialized yet — ignore speech results
                                        Log.w(TAG, "VoiceOSCore not initialized, ignoring speech result: '${result.text}'")
                                    }
                                    else -> {
                                        // Normal command mode — route all recognized speech to command processing
                                        processVoiceCommand(result.text, result.confidence)
                                    }
                                }
                            }
                        }
                }

                // Bridge: Collect web DOM voice commands → route to VoiceOSCore speech grammar
                // AND register as QuantizedCommands in CommandRegistry (dual-path).
                // Path 1: Phrases in speech grammar (recognition)
                // Path 2: QuantizedCommands in CommandRegistry (routing to WebCommandHandler)
                webCommandCollectorJob?.cancel()
                webCommandCollectorJob = serviceScope.launch {
                    BrowserVoiceOSCallback.activeWebPhrases
                        .collect { phrases ->
                            try {
                                // Path 1: Update speech grammar with web phrases
                                voiceOSCore?.updateWebCommands(phrases)

                                // Path 2: Register QuantizedCommands for ActionCoordinator routing
                                val callbackInstance = BrowserVoiceOSCallback.activeInstance
                                if (callbackInstance != null && phrases.isNotEmpty()) {
                                    // DOM-scraped element commands (truly dynamic)
                                    val quantizedWebCommands = callbackInstance.getWebCommandsAsQuantized()
                                    voiceOSCore?.actionCoordinator?.updateDynamicCommandsBySource("web", quantizedWebCommands)

                                    // Wire the WebCommandExecutorImpl to WebCommandHandler
                                    webCommandHandler?.let { wch ->
                                        val executor = WebCommandExecutorImpl(callbackInstance)
                                        wch.setExecutor(executor)
                                    }

                                    // Activate web domain — static BROWSER + WEB_GESTURE commands
                                    // now route via the domain activation system. No re-registration needed.
                                    voiceOSCore?.actionCoordinator?.activateModule("web")

                                    Log.d(TAG, "Web active: ${phrases.size} phrases, ${quantizedWebCommands.size} dynamic")
                                } else if (phrases.isEmpty()) {
                                    // Deactivate web domain + clear dynamic commands
                                    voiceOSCore?.actionCoordinator?.deactivateModule("web")
                                    voiceOSCore?.actionCoordinator?.clearDynamicCommandsBySource("web")
                                    webCommandHandler?.setExecutor(null)
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to update web commands", e)
                            }
                        }
                }

                voiceOSCore?.onSystemAction = { action ->
                    when (action) {
                        "OPEN_DEVELOPER_SETTINGS" -> {
                            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                putExtra("navigate_to", "developer_console")
                            }
                            startActivity(intent)
                        }
                        "RETRAIN_PAGE" -> {
                            // Invalidate caches + trigger fresh DOM scrape via static signal
                            BrowserVoiceOSCallback.requestRetrain()
                            Log.i(TAG, "Retrain page requested via voice command")
                        }
                    }
                }

                // Wire VoiceControlCallbacks for VoiceControlHandler
                // Enables: mute/wake voice, dictation toggle, show commands, numbers overlay
                //
                // IMPORTANT: Callbacks are invoked from the speech collector coroutine
                // running on Dispatchers.Main. Using runBlocking inside these lambdas
                // would deadlock the Main thread if the speech engine's stop/start
                // methods dispatch to Main (which Android's SpeechRecognizer requires).
                // Instead, we launch coroutines via serviceScope for async operations.
                try {
                    val core = voiceOSCore
                    VoiceControlCallbacks.onMuteVoice = {
                        OverlayStateManager.showFeedback("Voice Muted")
                        serviceScope.launch {
                            try {
                                // Build locale-aware wake commands from StaticCommandRegistry
                                // so the user can say "wake up voice" (or localized equivalent)
                                // while muted. Fallback to English if registry not loaded.
                                val wakeCommands = StaticCommandRegistry.findById("voice_wake")?.phrases
                                    ?: listOf("wake up voice", "start listening", "voice on")

                                core?.setSpeechMode(SpeechMode.MUTED, exitCommands = wakeCommands)
                                Log.i(TAG, "Voice muted via MUTED mode (wake commands: ${wakeCommands.size})")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to mute voice", e)
                            }
                        }
                        true // Return immediately; action executes async
                    }
                    VoiceControlCallbacks.onWakeVoice = {
                        OverlayStateManager.showFeedback("Voice Activated")
                        serviceScope.launch {
                            try {
                                // Restore full command grammar by switching back to COMBINED_COMMAND
                                core?.setSpeechMode(SpeechMode.COMBINED_COMMAND)
                                Log.i(TAG, "Voice woke via setSpeechMode(COMBINED_COMMAND)")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to wake voice", e)
                            }
                        }
                        true
                    }
                    VoiceControlCallbacks.onStartDictation = {
                        // Switch to dictation mode with locale-aware exit grammar.
                        // The speech engine stays active but only recognizes exit commands
                        // so the user can return to command mode via voice.
                        OverlayStateManager.showFeedback("Dictation Mode")
                        serviceScope.launch {
                            try {
                                // Build locale-aware exit commands from StaticCommandRegistry
                                val exitCommands = StaticCommandRegistry.findById("voice_dict_stop")?.phrases
                                    ?: listOf("stop dictation", "end dictation", "command mode")

                                core?.setSpeechMode(SpeechMode.DICTATION, exitCommands = exitCommands)
                                Log.i(TAG, "Dictation mode: switched with ${exitCommands.size} exit commands")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to start dictation", e)
                            }
                        }
                        true
                    }
                    VoiceControlCallbacks.onStopDictation = {
                        // Resume full command recognition
                        OverlayStateManager.showFeedback("Command Mode")
                        serviceScope.launch {
                            try {
                                core?.setSpeechMode(SpeechMode.COMBINED_COMMAND)
                                Log.i(TAG, "Command mode: switched back to COMBINED_COMMAND")
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to stop dictation", e)
                            }
                        }
                        true
                    }
                    VoiceControlCallbacks.onShowCommands = {
                        // Show numbered badges on all interactive elements
                        OverlayStateManager.setNumbersOverlayMode(
                            NumbersOverlayMode.ON
                        )
                        OverlayStateManager.showFeedback("Showing Numbers")
                        Log.i(TAG, "Showing commands: numbers overlay set to ON")
                        true
                    }
                    VoiceControlCallbacks.onListCommands = {
                        // Show available voice command categories via feedback
                        // A full VoiceCommandsPanel composable would be the ideal long-term
                        // solution, but for now show a summary of available command categories.
                        val categories = core?.actionCoordinator?.getHandlerCategories()
                            ?.joinToString(", ") ?: "commands"
                        OverlayStateManager.showFeedback("Commands: $categories")
                        Log.i(TAG, "List commands: showing category summary")
                        true
                    }
                    // Numbers overlay: handled by NumbersOverlayHandler registered below,
                    // using NumbersOverlayExecutor with proper assignment clearing.
                    Log.i(TAG, "VoiceControlCallbacks wired successfully")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to wire VoiceControlCallbacks", e)
                }

                // Observe numbers overlay mode changes to trigger re-scan.
                // When mode changes (via NumbersOverlayHandler, VoiceControlCallbacks,
                // or UI settings), we must invalidate the screen hash and refresh
                // badges immediately — otherwise the user sees "no action" because
                // badges only appear after the next accessibility event.
                numbersOverlayModeJob?.cancel()
                numbersOverlayModeJob = serviceScope.launch {
                    var previousMode: NumbersOverlayMode? = null
                    OverlayStateManager.numbersOverlayMode.collect { mode ->
                        if (previousMode != null && previousMode != mode) {
                            Log.d(TAG, "Numbers overlay mode changed: $previousMode → $mode")
                            dynamicCommandGenerator?.invalidateScreenHash()
                            refreshOverlayBadges()
                        }
                        previousMode = mode
                    }
                }

                // Wire VosFileImporter into VosSyncManager (late-binding).
                // VoiceCommandDaoAdapter is created via CommandDatabase singleton,
                // which requires VoiceOSDatabase — only available after DB init above.
                try {
                    val commandDatabase = com.augmentalis.voiceoscore.commandmanager.database.CommandDatabase
                        .getInstance(applicationContext)
                    val commandDao = commandDatabase.voiceCommandDao()
                    val vosRegistry = db.vosFileRegistry
                    val vosImporter = VosFileImporter(vosRegistry, commandDao)
                    val entryPoint = EntryPointAccessors.fromApplication(
                        applicationContext,
                        SyncEntryPoint::class.java
                    )
                    entryPoint.vosSyncManager().setImporter(vosImporter)
                    Log.i(TAG, "VosFileImporter wired to VosSyncManager")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to wire VosFileImporter: ${e.message}")
                }

                // Wire cursor settings + voice locale → CursorOverlayService lifecycle + config
                // Collects DataStore settings, starts/stops service, builds CursorConfig,
                // and switches voice command locale when changed.
                cursorSettingsJob?.cancel()
                var previousVoiceLocale: String? = null
                cursorSettingsJob = serviceScope.launch {
                    val cursorSettingsRepo = AvanuesSettingsRepository(applicationContext)
                    cursorSettingsRepo.settings.collectLatest { settings ->
                        // Voice command locale switching
                        val newLocale = settings.voiceLocale
                        if (previousVoiceLocale != null && previousVoiceLocale != newLocale) {
                            Log.i(TAG, "Voice locale changed: $previousVoiceLocale → $newLocale")
                            try {
                                val cm = CommandManager.getInstance(applicationContext)
                                val switched = cm.switchLocale(newLocale)
                                if (switched) {
                                    Log.i(TAG, "✅ Voice commands switched to $newLocale")
                                } else {
                                    Log.w(TAG, "⚠️ Failed to switch voice commands to $newLocale")
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Locale switch failed", e)
                            }
                        }
                        previousVoiceLocale = newLocale

                        // Wake word settings → VoiceOSCore lifecycle
                        try {
                            val wakePhrase = when (settings.wakeWordKeyword) {
                                "HEY_AVA" -> "hey ava"
                                "OK_AVA" -> "ok ava"
                                "COMPUTER" -> "computer"
                                else -> "hey ava"
                            }
                            voiceOSCore?.updateWakeWordSettings(
                                enabled = settings.wakeWordEnabled,
                                wakePhrase = wakePhrase,
                                sensitivity = settings.wakeWordSensitivity
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "Wake word settings update failed", e)
                        }

                        // Start/stop CursorOverlayService based on toggle
                        val canOverlay = Settings.canDrawOverlays(applicationContext)
                        if (settings.cursorEnabled && canOverlay) {
                            if (CursorOverlayService.getInstance() == null) {
                                try {
                                    val intent = Intent(applicationContext, CursorOverlayService::class.java)
                                    applicationContext.startForegroundService(intent)
                                    Log.i(TAG, "CursorOverlayService started via settings toggle")
                                    // Wire IMU + CursorActions + ClickDispatcher to the overlay service
                                    wireCursorDependencies(this@VoiceAvanueAccessibilityService)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to start CursorOverlayService", e)
                                }
                            }
                        } else if (!settings.cursorEnabled) {
                            CursorOverlayService.getInstance()?.let {
                                // Stop IMU tracking before stopping the service to release sensor resources
                                IMUManager.getInstance(applicationContext).stopIMUTracking("cursor_settings")
                                applicationContext.stopService(Intent(applicationContext, CursorOverlayService::class.java))
                                Log.i(TAG, "CursorOverlayService stopped via settings toggle")
                            }
                        }

                        // Restore custom accent override from persisted value
                        val accentOverride = settings.cursorAccentOverride
                        if (accentOverride != null) {
                            val color = androidx.compose.ui.graphics.Color(accentOverride.toInt())
                            AvanueModuleAccents.setOverride(
                                "voicecursor",
                                ModuleAccent(
                                    accent = color,
                                    onAccent = androidx.compose.ui.graphics.Color.White,
                                    accentMuted = color.copy(alpha = 0.6f),
                                    isCustom = true
                                )
                            )
                        } else {
                            AvanueModuleAccents.clearOverride("voicecursor")
                        }

                        // Build CursorConfig from settings + accent colors
                        val accentArgb = AvanueModuleAccents.getAccentArgb("voicecursor").toLong() and 0xFFFFFFFFL
                        val onAccentArgb = AvanueModuleAccents.getOnAccentArgb("voicecursor").toLong() and 0xFFFFFFFFL

                        val config = CursorConfig(
                            dwellClickEnabled = settings.dwellClickEnabled,
                            dwellClickDelayMs = settings.dwellClickDelayMs.toLong(),
                            jitterFilterEnabled = settings.cursorSmoothing,
                            filterStrength = FilterStrength.Medium,
                            size = settings.cursorSize,
                            speed = settings.cursorSpeed,
                            showCoordinates = settings.showCoordinates,
                            color = accentArgb,
                            borderColor = onAccentArgb,
                            dwellRingColor = accentArgb
                        )

                        CursorOverlayService.getInstance()?.let { cursorService ->
                            cursorService.updateConfig(config)
                            // ClickDispatcher is wired by wireCursorDependencies() when cursor
                            // is first enabled — no need to re-wire on every settings change
                        }
                    }
                }

                Log.i(TAG, "VoiceOSCore initialized with dev settings: engine=${devSettings.sttEngine}, lang=${devSettings.voiceLanguage}, confidence=${devSettings.confidenceThreshold}")
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
            }
        }
    }

    /**
     * Inject dictation text into the currently focused input field.
     * Uses AccessibilityNodeInfo.ACTION_SET_TEXT to append recognized speech.
     */
    @Suppress("DEPRECATION") // AccessibilityNodeInfo.recycle() deprecated in API 34+ but needed for compat
    private fun injectDictationText(text: String) {
        var focusedNode: android.view.accessibility.AccessibilityNodeInfo? = null
        try {
            focusedNode = rootInActiveWindow?.findFocus(
                android.view.accessibility.AccessibilityNodeInfo.FOCUS_INPUT
            )
            if (focusedNode == null) {
                Log.w(TAG, "Dictation: no focused input field — cannot inject text")
                OverlayStateManager.showFeedback("No text field focused")
                return
            }

            // Append to existing text (with space separator)
            val existingText = focusedNode.text?.toString() ?: ""
            val newText = if (existingText.isNotEmpty()) "$existingText $text" else text

            val args = android.os.Bundle().apply {
                putCharSequence(
                    android.view.accessibility.AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }
            val success = focusedNode.performAction(
                android.view.accessibility.AccessibilityNodeInfo.ACTION_SET_TEXT,
                args
            )
            if (success) {
                Log.d(TAG, "Dictation: injected '${text}' into focused field")
            } else {
                Log.w(TAG, "Dictation: ACTION_SET_TEXT failed")
                OverlayStateManager.showFeedback("Text injection failed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Dictation text injection error", e)
        } finally {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                focusedNode?.recycle()
            }
        }
    }

    override fun onCommandsUpdated(commands: List<QuantizedCommand>) {
        serviceScope.launch {
            voiceOSCore?.updateCommands(commands.map { it.phrase })

            // Update overlay badges from accessibility tree
            refreshOverlayBadges()
        }
    }

    /**
     * Called after scroll events settle (debounced 300ms).
     *
     * Bypasses the command generation pipeline to refresh overlay badges directly.
     * Without this, scrolling in Gmail (or any app) would never update overlays because:
     * - TYPE_VIEW_SCROLLED doesn't trigger handleScreenChange → onCommandsUpdated
     * - Even TYPE_WINDOW_CONTENT_CHANGED gates on KMP fingerprint which may not change
     */
    override fun onInAppNavigation(packageName: String) {
        Log.d(TAG, "In-app navigation detected: $packageName, clearing overlay")
        // Reset numbering so new screen starts from 1
        overlayNumberingExecutor?.resetForNavigation()
        // Clear overlay items + hash + signatures for fresh scan
        dynamicCommandGenerator?.clearCache()
    }

    override fun onScrollSettled(packageName: String) {
        serviceScope.launch {
            Log.d(TAG, "Scroll settled, refreshing overlay for $packageName")
            // Invalidate screen hash so processScreen() always runs after scroll.
            // We KNOW content changed (user scrolled), so skip hash-based deduplication.
            // Without this, the content-aware hash might still match if the depth limit
            // is too shallow to reach actual text nodes inside RecyclerView children.
            dynamicCommandGenerator?.invalidateScreenHash()
            refreshOverlayBadges()
        }
    }

    /**
     * Common overlay refresh logic used by both onCommandsUpdated (screen change)
     * and onScrollSettled (scroll). Gets fresh rootInActiveWindow and runs
     * DynamicCommandGenerator.processScreen().
     *
     * Navigation detection is handled by structural-change-ratio in handleScreenContext(),
     * not by event source. This works for both Activity and Fragment transitions.
     */
    private fun refreshOverlayBadges() {
        try {
            val root = rootInActiveWindow ?: return
            val packageName = root.packageName?.toString() ?: return
            val isTargetApp = TARGET_APPS.contains(packageName)

            // Browser-scope: clear web commands when foreground app is not the browser.
            // Prevents web-scraped phrases from polluting grammar in other apps.
            val isBrowser = packageName == applicationContext.packageName
            if (!isBrowser) {
                BrowserVoiceOSCallback.clearActiveWebPhrases()
            }

            dynamicCommandGenerator?.processScreen(root, packageName, isTargetApp)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overlay", e)
        }
    }

    override fun onDestroy() {
        instance = null

        // Clear VoiceControlCallbacks to prevent stale references
        VoiceControlCallbacks.clear()
        ModuleCommandCallbacks.clearAll()

        // Cancel speech result collection
        speechCollectorJob?.cancel()
        speechCollectorJob = null

        // Cancel web command collection and clear stale phrases
        webCommandCollectorJob?.cancel()
        webCommandCollectorJob = null
        BrowserVoiceOSCallback.clearActiveWebPhrases()

        // Stop IMU tracking for any active cursor consumers before service teardown
        try {
            IMUManager.getInstance(applicationContext).stopIMUTracking("cursor_voice")
            IMUManager.getInstance(applicationContext).stopIMUTracking("cursor_settings")
        } catch (_: Exception) { /* best-effort cleanup */ }

        // Cancel cursor settings collection
        cursorSettingsJob?.cancel()
        cursorSettingsJob = null

        // Cancel numbers overlay mode observer
        numbersOverlayModeJob?.cancel()
        numbersOverlayModeJob = null

        // Stop overlay service
        try {
            CommandOverlayService.stop(applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping CommandOverlayService", e)
        }

        // Dispose VoiceOSCore on a background thread to avoid blocking (and deadlocking)
        // the accessibility service Main thread. A dedicated scope with SupervisorJob
        // ensures the disposal runs to completion even after serviceScope is cancelled.
        val disposeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        disposeScope.launch {
            try {
                withTimeout(3000L) {
                    voiceOSCore?.dispose()
                }
            } catch (e: Exception) {
                Log.w(TAG, "VoiceOSCore dispose timed out or failed", e)
            } finally {
                disposeScope.cancel()
            }
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        @Volatile private var instance: VoiceAvanueAccessibilityService? = null
        fun getInstance(): VoiceAvanueAccessibilityService? = instance

        fun isEnabled(context: Context): Boolean {
            val services = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            return services.contains("${context.packageName}/${VoiceAvanueAccessibilityService::class.java.canonicalName}")
        }
    }
}
