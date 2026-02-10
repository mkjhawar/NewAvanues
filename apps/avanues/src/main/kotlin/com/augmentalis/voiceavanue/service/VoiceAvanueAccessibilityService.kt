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
import com.augmentalis.voiceoscore.WebCommandHandler
import com.augmentalis.voiceoscore.createForAndroid
import com.augmentalis.voiceavanue.MainActivity
import com.augmentalis.avanueui.theme.AvanueModuleAccents
import com.augmentalis.avanueui.theme.ModuleAccent
import com.augmentalis.voiceavanue.data.AvanuesSettingsRepository
import com.augmentalis.voiceavanue.data.DeveloperPreferencesRepository
import com.augmentalis.voiceavanue.data.DeveloperSettings
import com.augmentalis.voicecursor.core.CursorConfig
import com.augmentalis.voicecursor.core.FilterStrength
import com.augmentalis.voicecursor.overlay.CursorOverlayService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import com.augmentalis.webavanue.BrowserVoiceOSCallback
import com.augmentalis.webavanue.WebCommandExecutorImpl

private const val TAG = "VoiceAvanueService"

/**
 * Accessibility service wrapper with overlay badge support.
 * Core command generation is in VoiceOSCore; this adds overlay display.
 */
class VoiceAvanueAccessibilityService : VoiceOSAccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var voiceOSCore: VoiceOSCore? = null
    private var actionCoordinator: ActionCoordinator? = null
    private var boundsResolver: BoundsResolver? = null
    private var dynamicCommandGenerator: DynamicCommandGenerator? = null
    private var webCommandHandler: WebCommandHandler? = null
    private var speechCollectorJob: kotlinx.coroutines.Job? = null  // Cancelled in onDestroy
    private var webCommandCollectorJob: kotlinx.coroutines.Job? = null  // Cancelled in onDestroy
    private var cursorSettingsJob: kotlinx.coroutines.Job? = null  // Cancelled in onDestroy

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

        // Initialize the dynamic command generator for overlay badges
        dynamicCommandGenerator = DynamicCommandGenerator(resources)

        serviceScope.launch(Dispatchers.IO) {
            try {
                val db = VoiceOSDatabaseManager.getInstance(DatabaseDriverFactory(applicationContext))
                db.waitForInitialization()

                boundsResolver = BoundsResolver(this@VoiceAvanueAccessibilityService)

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

                // Register WebCommandHandler for web voice command routing
                val handler = WebCommandHandler()
                webCommandHandler = handler
                voiceOSCore?.actionCoordinator?.registerHandler(handler)
                Log.i(TAG, "WebCommandHandler registered with ActionCoordinator")

                // Force screen refresh so dynamic commands register on VoiceOSCore's
                // coordinator. Before init, commands were registered on a fallback
                // coordinator with a different CommandRegistry. The screen hash cache
                // prevents re-scanning the same screen, so we must clear it explicitly.
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    refreshScreen()
                }

                // Bridge: Collect speech recognition results → route to command execution
                // Without this, Vivoka emits recognized commands but nobody processes them
                val confidenceThreshold = devSettings.confidenceThreshold
                speechCollectorJob?.cancel()
                speechCollectorJob = serviceScope.launch {
                    voiceOSCore?.speechResults
                        ?.conflate()  // Drop intermediate results if processing is slow
                        ?.collect { result ->
                            if (result.isFinal && result.text.isNotBlank() && result.confidence >= confidenceThreshold) {
                                Log.d(TAG, "Voice recognized: '${result.text}' (conf: ${result.confidence})")
                                processVoiceCommand(result.text, result.confidence)
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
                                    val quantizedWebCommands = callbackInstance.getWebCommandsAsQuantized()
                                    voiceOSCore?.actionCoordinator?.updateDynamicCommands(quantizedWebCommands)

                                    // Wire the WebCommandExecutorImpl to WebCommandHandler
                                    // The executor delegates to BrowserVoiceOSCallback's JS executor
                                    // (which is set by WebViewContainer when the bridge attaches)
                                    webCommandHandler?.let { wch ->
                                        val executor = WebCommandExecutorImpl(callbackInstance)
                                        wch.setExecutor(executor)
                                    }

                                    Log.d(TAG, "Web commands dual-path: ${phrases.size} phrases, ${quantizedWebCommands.size} quantized")
                                } else if (phrases.isEmpty()) {
                                    // Clear dynamic commands when leaving browser
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

                // Wire cursor settings → CursorOverlayService lifecycle + config
                // Collects DataStore settings, starts/stops service, and builds CursorConfig
                cursorSettingsJob?.cancel()
                cursorSettingsJob = serviceScope.launch {
                    val cursorSettingsRepo = AvanuesSettingsRepository(applicationContext)
                    cursorSettingsRepo.settings.collectLatest { settings ->
                        // Start/stop CursorOverlayService based on toggle
                        val canOverlay = Settings.canDrawOverlays(applicationContext)
                        if (settings.cursorEnabled && canOverlay) {
                            if (CursorOverlayService.getInstance() == null) {
                                try {
                                    val intent = Intent(applicationContext, CursorOverlayService::class.java)
                                    applicationContext.startForegroundService(intent)
                                    Log.i(TAG, "CursorOverlayService started via settings toggle")
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to start CursorOverlayService", e)
                                }
                            }
                        } else if (!settings.cursorEnabled) {
                            CursorOverlayService.getInstance()?.let {
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
                            // Ensure ClickDispatcher is set so "cursor click" voice command works
                            cursorService.setClickDispatcher(AccessibilityClickDispatcher())
                        }
                    }
                }

                Log.i(TAG, "VoiceOSCore initialized with dev settings: engine=${devSettings.sttEngine}, lang=${devSettings.voiceLanguage}, confidence=${devSettings.confidenceThreshold}")
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
            }
        }
    }

    override fun onCommandsUpdated(commands: List<QuantizedCommand>) {
        serviceScope.launch {
            voiceOSCore?.updateCommands(commands.map { it.phrase })

            // Update overlay badges from accessibility tree
            try {
                val root = rootInActiveWindow ?: return@launch
                val packageName = root.packageName?.toString() ?: return@launch
                val isTargetApp = OverlayStateManager.TARGET_APPS.contains(packageName)

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
    }

    override fun onDestroy() {
        instance = null

        // Cancel speech result collection
        speechCollectorJob?.cancel()
        speechCollectorJob = null

        // Cancel web command collection and clear stale phrases
        webCommandCollectorJob?.cancel()
        webCommandCollectorJob = null
        BrowserVoiceOSCallback.clearActiveWebPhrases()

        // Cancel cursor settings collection
        cursorSettingsJob?.cancel()
        cursorSettingsJob = null

        // Stop overlay service
        try {
            CommandOverlayService.stop(applicationContext)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping CommandOverlayService", e)
        }

        try {
            runBlocking {
                withTimeout(3000L) {
                    voiceOSCore?.dispose()
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "VoiceOSCore dispose timed out or failed", e)
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
