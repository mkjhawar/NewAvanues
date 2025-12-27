/**
 * LearnAppIntegration.kt - Integration adapter for LearnApp into VoiceOS
 * Path: modules/apps/LearnApp/src/main/java/com/augmentalis/learnapp/integration/LearnAppIntegration.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Integration adapter for wiring LearnApp into VoiceOS.
 * Provides unified API for all LearnApp functionality including app learning,
 * consent management, and exploration coordination.
 */

package com.augmentalis.voiceoscore.learnapp.integration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import com.augmentalis.voiceoscore.accessibility.IVoiceOSServiceInternal
import com.augmentalis.voiceoscore.learnapp.database.repository.AppMetadataProvider
import com.augmentalis.voiceoscore.learnapp.database.repository.LearnAppRepository
import com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult
import com.augmentalis.voiceoscore.learnapp.database.repository.SessionCreationResult
import com.augmentalis.voiceoscore.learnapp.detection.AppLaunchDetector
import com.augmentalis.voiceoscore.learnapp.detection.LearnedAppTracker
import com.augmentalis.voiceoscore.learnapp.exploration.DFSExplorationStrategy
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationDebugCallback
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationStrategy
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ExplorationState
import com.augmentalis.voiceoscore.learnapp.overlays.LoginPromptAction
import com.augmentalis.voiceoscore.learnapp.overlays.LoginPromptConfig
import com.augmentalis.voiceoscore.learnapp.overlays.LoginPromptOverlay
import com.augmentalis.voiceoscore.learnapp.jit.JustInTimeLearner
import com.augmentalis.jitlearning.JITLearnerProvider
import com.augmentalis.jitlearning.JITEventCallback
import com.augmentalis.jitlearning.JITLearningService
import com.augmentalis.jitlearning.ExplorationProgressCallback
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppPreferences
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.learnapp.ui.ConsentDialogManager
import com.augmentalis.voiceoscore.learnapp.ui.FloatingProgressWidget
import com.augmentalis.voiceoscore.scraping.AccessibilityScrapingIntegration
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
// TEMP DISABLED (Room migration): import com.augmentalis.uuidcreator.database.UUIDCreatorDatabase
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.version.AppVersionDetector
import com.augmentalis.voiceoscore.version.ScreenHashCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.time.Duration.Companion.milliseconds
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Blocked state enum for exploration auto-pause
 *
 * Represents different types of blocked states that require manual intervention.
 *
 * @since 1.0.0 (Phase 4: Auto-pause on blocked states)
 */
enum class BlockedState {
    /** Permission dialog detected */
    PERMISSION_REQUIRED,
    /** Login screen detected */
    LOGIN_REQUIRED
}

/**
 * LearnApp Integration
 *
 * Central integration adapter for wiring LearnApp into VoiceOS.
 * Provides unified API for all LearnApp functionality.
 *
 * ## Integration Pattern
 *
 * ```kotlin
 * // In VoiceOSService (AccessibilityService)
 * class VoiceOSService : AccessibilityService() {
 *     private var learnAppIntegration: LearnAppIntegration? = null
 *
 *     override fun onServiceConnected() {
 *         super.onServiceConnected()
 *         learnAppIntegration = LearnAppIntegration.initialize(applicationContext, this)
 *     }
 *
 *     override fun onAccessibilityEvent(event: AccessibilityEvent) {
 *         learnAppIntegration?.onAccessibilityEvent(event)
 *     }
 *
 *     override fun onDestroy() {
 *         learnAppIntegration?.cleanup()
 *         super.onDestroy()
 *     }
 * }
 * ```
 *
 * @property context Application context
 * @property accessibilityService Accessibility service instance
 *
 * @since 1.0.0
 */
class LearnAppIntegration private constructor(
    private val context: Context,
    private val accessibilityService: AccessibilityService
) : JITLearnerProvider {

    /**
     * Coroutine scope
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Core components
     */
    private val preferences: LearnAppPreferences
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }
    private val learnedAppTracker: LearnedAppTracker
    private val appLaunchDetector: AppLaunchDetector
    private val consentDialogManager: ConsentDialogManager
    private var floatingProgressWidget: FloatingProgressWidget? = null
    private val explorationEngine: ExplorationEngine
    private val scrapingIntegration: AccessibilityScrapingIntegration
    private val justInTimeLearner: JustInTimeLearner

    /**
     * Database repository and metadata provider
     */
    private val repository: LearnAppRepository
    private val metadataProvider: AppMetadataProvider

    /**
     * Login prompt overlay (created on-demand)
     */
    private var loginPromptOverlay: LoginPromptOverlay? = null

    /**
     * UUIDCreator components
     */
    private val uuidCreator: UUIDCreator
    private val thirdPartyGenerator: ThirdPartyUuidGenerator
    private val aliasManager: UuidAliasManager

    /**
     * Exploration strategy
     */
    private val explorationStrategy: ExplorationStrategy = DFSExplorationStrategy()

    /**
     * AVU Quantizer Integration (2025-12-08)
     *
     * Real-time quantization of UI elements during exploration for NLU/LLM integration.
     * Captures elements as they're discovered and builds compact, tokenized representations.
     */
    private val avuQuantizerIntegration: com.augmentalis.voiceoscore.learnapp.ai.quantized.AVUQuantizerIntegration

    /**
     * Database manager reference for direct screen context queries
     * FIX (2025-12-11): Stored for getLearnedScreenHashes() implementation
     */
    private lateinit var databaseManager: com.augmentalis.database.VoiceOSDatabaseManager

    /**
     * Manual command integration for VOS-META-001 (Phase 2 UI Layer)
     * Optional - set by VoiceOSService after initialization
     */
    var manualCommandIntegration: com.augmentalis.voiceoscore.commands.integration.ManualCommandIntegration? = null

    init {
        // Initialize preferences
        preferences = LearnAppPreferences(context)

        // Initialize database manager (singleton for SQLDelight)
        // FIX (2025-12-11): Store reference for getLearnedScreenHashes()
        databaseManager = com.augmentalis.database.VoiceOSDatabaseManager.getInstance(
            com.augmentalis.database.DatabaseDriverFactory(context)
        )

        // Initialize repository with database manager (SQLDelight)
        // Note: ScrapedAppMetadataSource implementation is optional and can be added later
        repository = LearnAppRepository(databaseManager, context)
        metadataProvider = AppMetadataProvider(context)

        // Initialize UUIDCreator components
        uuidCreator = UUIDCreator.getInstance()
        thirdPartyGenerator = ThirdPartyUuidGenerator(context)
        aliasManager = UuidAliasManager(databaseManager.uuids)

        // Version-aware command management (2025-12-14)
        // Create AppVersionDetector for tracking app versions in generated commands
        val versionDetector = AppVersionDetector(
            context = context,
            repository = databaseManager.appVersions
        )

        // Phase 3 (2025-12-04): Initialize LearnAppCore for unified element processing
        // Create LearnAppCore with database and UUID generator (reuse thirdPartyGenerator)
        // Version-aware: Pass versionDetector for version tracking in commands
        val learnAppCore = com.augmentalis.voiceoscore.learnapp.core.LearnAppCore(
            context = context,
            database = databaseManager,
            uuidGenerator = thirdPartyGenerator,
            versionDetector = versionDetector  // Version-aware command creation
        )

        // Initialize LearnApp components
        learnedAppTracker = LearnedAppTracker(context)
        appLaunchDetector = AppLaunchDetector(context, learnedAppTracker)
        consentDialogManager = ConsentDialogManager(accessibilityService, learnedAppTracker)
        // FloatingProgressWidget initialized lazily when exploration starts

        // Initialize exploration engine
        // Phase 3 (2025-12-04): Pass LearnAppCore for voice command generation
        explorationEngine = ExplorationEngine(
            context = context,
            accessibilityService = accessibilityService,
            uuidCreator = uuidCreator,
            thirdPartyGenerator = thirdPartyGenerator,
            aliasManager = aliasManager,
            repository = repository,
            databaseManager = databaseManager,
            strategy = explorationStrategy,
            learnAppCore = learnAppCore  // Phase 3: Enable voice command generation
        )

        // FloatingProgressWidget is initialized when exploration starts (lazy init)

        // Initialize scraping integration for potential future use
        scrapingIntegration = AccessibilityScrapingIntegration(
            context = context,
            accessibilityService = accessibilityService
        )

        // Initialize just-in-time learner
        // FIX (2025-11-30): Pass voiceOSService for command registration (P1-H4)
        // Phase 2 (2025-12-04): Pass LearnAppCore for unified command generation
        // Version-aware (2025-12-14): Pass versionDetector for version tracking
        justInTimeLearner = JustInTimeLearner(
            context = context,
            databaseManager = databaseManager,
            repository = repository,
            voiceOSService = accessibilityService as? IVoiceOSServiceInternal,
            learnAppCore = learnAppCore,  // Phase 2: Use LearnAppCore
            versionDetector = versionDetector,  // Version-aware command creation
            screenHashCalculator = ScreenHashCalculator  // P2 Task 1.1: Hash-based rescan optimization
        )

        // FIX (2025-12-01): Initialize JIT element capture for Voice Command Element Persistence
        // This enables JIT learning to capture elements and generate voice commands
        justInTimeLearner.initializeElementCapture(accessibilityService)

        // Initialize AVU Quantizer Integration (2025-12-08)
        // Real-time quantization for NLU/LLM integration
        avuQuantizerIntegration = com.augmentalis.voiceoscore.learnapp.ai.quantized.AVUQuantizerIntegration(context)

        // Set up event listeners
        setupEventListeners()

        // FIX (2025-12-11): Wire JITLearningService to this provider
        // This connects the AIDL service to the actual JustInTimeLearner
        wireJITLearningService()
    }

    /**
     * Setup event listeners
     * FIX (2025-11-30): Added error handling to prevent silent failures
     * FIX (2025-11-30): Added withContext(Dispatchers.Main) for UI operations
     * FIX (2025-11-30): Changed collect to collectLatest to cancel pending operations on new events
     * FIX (2025-12-06): Added blocked state monitoring and pause state wiring (v1.8 integration)
     */
    private fun setupEventListeners() {
        // Listen for app launch events with throttling
        // FR-004: Implement event debouncing to prevent continuous events from causing flickering
        // FR-010: Handle apps generating 10+ events/sec without freezing
        // FR-011: Prevent consent dialog from showing more than once per app per session
        scope.launch {
            try {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    Log.d(TAG, "setupEventListeners() - Starting Flow collector for appLaunchEvents")
                }
                appLaunchDetector.appLaunchEvents
                    .debounce(500.milliseconds) // Wait 500ms of event silence before processing
                    .distinctUntilChanged { old, new ->
                        // Only emit if package name actually changed
                        when {
                            old is com.augmentalis.voiceoscore.learnapp.detection.AppLaunchEvent.NewAppDetected &&
                            new is com.augmentalis.voiceoscore.learnapp.detection.AppLaunchEvent.NewAppDetected ->
                                old.packageName == new.packageName
                            else -> false
                        }
                    }
                    .flowOn(Dispatchers.Default) // Process filtering on background thread
                    .catch { e ->
                        Log.e(TAG, "Flow error in appLaunchEvents", e)
                    }
                    .collectLatest { event ->  // FIX: collectLatest cancels pending operations on new events
                        try {
                            if (developerSettings.isVerboseLoggingEnabled()) {
                                Log.d(TAG, "Flow collector received event: $event")
                            }
                            when (event) {
                                is com.augmentalis.voiceoscore.learnapp.detection.AppLaunchEvent.NewAppDetected -> {
                                    if (developerSettings.isVerboseLoggingEnabled()) {
                                        Log.d(TAG, "Processing NewAppDetected: packageName=${event.packageName}, appName=${event.appName}")
                                    }

                                    // FIX (2025-12-02): Don't interrupt active exploration with consent dialogs
                                    // Issue: When exploring Photos, clicking input field triggers Gboard appearance
                                    // This fires NewAppDetected → consent dialog → blocks Photos exploration
                                    // Solution: Ignore NewAppDetected events during active exploration
                                    val currentState = explorationEngine.explorationState.value
                                    if (currentState is ExplorationState.Running) {
                                        Log.i(TAG, "BLOCKED NewAppDetected during exploration: ${event.packageName} " +
                                            "(currently exploring ${currentState.progress.appName})")
                                        return@collectLatest
                                    }

                                    // Check if auto-detect mode is enabled
                                    if (preferences.isAutoDetectEnabled()) {
                                        if (developerSettings.isVerboseLoggingEnabled()) {
                                            Log.d(TAG, "AUTO_DETECT enabled - showing consent dialog for ${event.packageName}")
                                        }
                                        // FIX: Show consent dialog on Main thread
                                        withContext(Dispatchers.Main) {
                                            consentDialogManager.showConsentDialog(
                                                packageName = event.packageName,
                                                appName = event.appName
                                            )
                                        }
                                    } else {
                                        // Manual mode - don't show dialog automatically
                                        if (developerSettings.isVerboseLoggingEnabled()) {
                                            Log.d(TAG, "Manual mode enabled - skipping consent dialog for ${event.packageName}")
                                        }
                                    }
                                }
                                else -> {
                                    if (developerSettings.isVerboseLoggingEnabled()) {
                                        Log.d(TAG, "Received other event type: ${event.javaClass.simpleName}")
                                    }
                                    // Handle other events
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling app launch event: $event", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Flow collector for appLaunchEvents crashed", e)
            }
        }

        // Listen for consent responses
        scope.launch {
            try {
                consentDialogManager.consentResponses
                    .catch { e ->
                        Log.e(TAG, "Flow error in consentResponses", e)
                    }
                    .collect { response ->
                        try {
                            when (response) {
                                is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Approved -> {
                                    // Start exploration
                                    startExplorationInternal(response.packageName)
                                }

                                is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Declined -> {
                                    // Do nothing
                                }

                                is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Skipped -> {
                                    // Activate just-in-time learning mode
                                    justInTimeLearner.activate(response.packageName)
                                }

                                is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Dismissed -> {
                                    // Dialog dismissed - no action
                                    if (developerSettings.isVerboseLoggingEnabled()) {
                                        Log.d(TAG, "Consent dialog dismissed for ${response.packageName}")
                                    }
                                }

                                is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Timeout -> {
                                    // Timeout - treat as dismissed
                                    if (developerSettings.isVerboseLoggingEnabled()) {
                                        Log.d(TAG, "Consent dialog timed out for ${response.packageName}")
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling consent response: $response", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Flow collector for consentResponses crashed", e)
            }
        }

        // Listen for exploration state changes
        scope.launch {
            try {
                explorationEngine.explorationState
                    .catch { e ->
                        Log.e(TAG, "Flow error in explorationState", e)
                    }
                    .collect { state ->
                        try {
                            handleExplorationStateChange(state)
                        } catch (e: Exception) {
                            Log.e(TAG, "Error handling exploration state: $state", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Flow collector for explorationState crashed", e)
            }
        }

        // Phase 4: Monitor for blocked states during exploration
        scope.launch {
            try {
                explorationEngine.explorationState
                    .catch { e ->
                        Log.e(TAG, "Flow error in blocked state monitor", e)
                    }
                    .collect { state ->
                        try {
                            when (state) {
                                is ExplorationState.Running -> {
                                    // Check for blocked states every time we're running
                                    val currentScreen = accessibilityService.rootInActiveWindow
                                    if (currentScreen != null) {
                                        val blockedState = detectBlockedState(currentScreen)

                                        if (blockedState != null) {
                                            val reason = when (blockedState) {
                                                BlockedState.PERMISSION_REQUIRED ->
                                                    "Permission required - Paused for manual intervention"
                                                BlockedState.LOGIN_REQUIRED ->
                                                    "Login required - Paused for manual login"
                                            }

                                            Log.i(TAG, "Blocked state detected: $blockedState - Auto-pausing exploration")
                                            explorationEngine.pauseExploration()

                                            withContext(Dispatchers.Main) {
                                                showToastNotification(
                                                    "Exploration Paused",
                                                    "$reason\nTap Resume when ready"
                                                )
                                            }
                                        }
                                        currentScreen.recycle()
                                    }
                                }
                                else -> {
                                    // Not running, no need to check for blocked states
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in blocked state monitor: $state", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Blocked state monitor crashed", e)
            }
        }

        // Phase 4: Poll for blocked state resolution when paused
        scope.launch {
            try {
                explorationEngine.explorationState
                    .catch { e ->
                        Log.e(TAG, "Flow error in resolution poller", e)
                    }
                    .collect { state ->
                        try {
                            if (state is ExplorationState.PausedByUser) {
                                // Poll every 2 seconds while paused
                                while (explorationEngine.explorationState.value is ExplorationState.PausedByUser) {
                                    delay(2000)
                                    val currentScreen = accessibilityService.rootInActiveWindow
                                    if (currentScreen != null) {
                                        if (detectBlockedState(currentScreen) == null) {
                                            // Blocked state resolved!
                                            Log.i(TAG, "Blocked state resolved - Ready to resume")
                                            withContext(Dispatchers.Main) {
                                                showToastNotification(
                                                    "Ready to Resume",
                                                    "Blocked state resolved. You can now resume exploration."
                                                )
                                            }
                                            break
                                        }
                                        currentScreen.recycle()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error in resolution poller", e)
                        }
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Resolution poller crashed", e)
            }
        }
    }

    /**
     * Handle accessibility event
     *
     * Call this from VoiceOSService.onAccessibilityEvent()
     *
     * @param event Accessibility event
     */
    fun onAccessibilityEvent(event: AccessibilityEvent) {
        appLaunchDetector.onAccessibilityEvent(event)

        // Forward events to JIT learner (if active)
        justInTimeLearner.onAccessibilityEvent(event)
    }

    /**
     * Start exploration of app (internal)
     *
     * FIX (2025-12-02): Added timeout protection to prevent infinite spinning
     * FIX (2025-12-11): Renamed to startExplorationInternal to avoid conflict with JITLearnerProvider
     *
     * @param packageName Package name to explore
     */
    private fun startExplorationInternal(packageName: String) {
        scope.launch {
            try {
                // FIX: Add timeout protection (30 seconds max for initialization)
                withTimeout(30_000) {
                    // Create session using safe auto-create pattern
                    when (val result = repository.createExplorationSessionSafe(packageName)) {
                        is SessionCreationResult.Created -> {
                            // Session created successfully
                            if (result.appWasCreated) {
                                // Log that parent app was auto-created
                                if (developerSettings.isVerboseLoggingEnabled()) {
                                    android.util.Log.d(
                                        "LearnAppIntegration",
                                        "Auto-created LearnedApp for $packageName from ${result.metadataSource}"
                                    )
                                }
                            }

                            // AVU Quantizer (2025-12-08): Start quantization before exploration
                            avuQuantizerIntegration.startQuantization(packageName)

                            // Start exploration engine with session ID for database persistence
                            explorationEngine.startExploration(packageName, result.sessionId)
                        }

                        is SessionCreationResult.Failed -> {
                            // Handle failure
                            android.util.Log.e(
                                "LearnAppIntegration",
                                "Failed to create session for $packageName: ${result.reason}", result.cause
                            )

                            // Dismiss floating widget
                            floatingProgressWidget?.dismiss()

                            // Show error notification
                            showToastNotification(
                                title = "Failed to Start Learning",
                                message = "Could not start learning $packageName: ${result.reason}"
                            )
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                // FIX: Handle timeout explicitly
                Log.e("LearnAppIntegration", "Exploration initialization timed out for $packageName", e)

                // Dismiss floating widget
                floatingProgressWidget?.dismiss()

                // Show timeout notification
                showToastNotification(
                    title = "Learning Timed Out",
                    message = "Failed to start learning $packageName (timeout after 30 seconds). " +
                            "Try ensuring the app is fully launched and in foreground."
                )
            }
        }
    }

    /**
     * Handle exploration state change
     * FIX (2025-12-06): Updated to use new command bar instead of old overlay (v1.8 integration)
     *
     * @param state New exploration state
     */
    private fun handleExplorationStateChange(state: ExplorationState) {
        when (state) {
            is ExplorationState.Idle -> {
                // Dismiss floating widget
                floatingProgressWidget?.dismiss()
            }

            is ExplorationState.Running -> {
                // FIX (2025-12-07): Use new FloatingProgressWidget (draggable, transparent)
                val progress = calculateProgress(state)
                val statusMsg = "Learning ${state.progress.appName}..."
                val statsMsg = "${state.progress.screensExplored} screens, ${state.progress.elementsDiscovered} elements"

                // Create widget if needed (lazy init)
                if (floatingProgressWidget == null) {
                    floatingProgressWidget = FloatingProgressWidget(
                        context = accessibilityService,
                        onPauseClick = { explorationEngine.pauseExploration() },
                        onResumeClick = { explorationEngine.resumeExploration() },
                        onStopClick = { explorationEngine.stopExploration() }
                    )

                    // DEBUG (2025-12-08): Set up debug callback to wire overlay to exploration engine
                    setupDebugOverlayCallback()
                }

                if (floatingProgressWidget?.isShowing() != true) {
                    // Show widget for first time
                    floatingProgressWidget?.show()
                    Log.i(TAG, "🚀 Started learning ${state.packageName} - Floating widget shown")

                    // DEBUG (2025-12-08): Show debug overlay by default when exploration starts
                    floatingProgressWidget?.enableDebugOverlay()

                    // Show toast notification
                    scope.launch {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "🚀 Started learning ${state.progress.appName}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                // Update progress
                floatingProgressWidget?.updateProgress(progress, statusMsg, statsMsg)
            }

            is ExplorationState.PausedForLogin -> {
                // Show login prompt overlay
                showLoginPromptOverlay(state.packageName, state.progress.appName)
                // Update widget to show paused state
                floatingProgressWidget?.updatePauseState(true)
            }

            is ExplorationState.PausedByUser -> {
                // Update widget to show paused state
                val progress = calculateProgress(state)
                floatingProgressWidget?.updateProgress(
                    progress,
                    "Paused by user",
                    "Tap Resume to continue"
                )
                floatingProgressWidget?.updatePauseState(true)
            }

            is ExplorationState.Paused -> {
                // FIX (2025-12-07): Handle unified Paused state
                // This state is used by the new pause() function in ExplorationEngine
                val progress = calculateProgress(state)
                floatingProgressWidget?.updateProgress(
                    progress,
                    "Paused: ${state.reason}",
                    "Tap Resume to continue"
                )
                floatingProgressWidget?.updatePauseState(true)
            }

            is ExplorationState.Completed -> {
                // DEBUG (2025-12-08): Clear debug callback and overlay
                clearDebugOverlayCallback()

                // AVU Quantizer (2025-12-08): Stop quantization and save results
                val quantizedContext = avuQuantizerIntegration.stopQuantization()
                if (quantizedContext != null) {
                    val stats = avuQuantizerIntegration.getQuantizerStats()
                    Log.i(TAG, "📊 Quantized context saved: ${stats.screensProcessed} screens, " +
                        "${stats.elementsProcessed} elements, ${stats.actionCandidates} actions")
                }

                // Dismiss floating widget
                floatingProgressWidget?.dismiss()

                // Hide login prompt overlay if showing
                hideLoginPromptOverlay()

                // Save results with actual version info from PackageManager
                scope.launch {
                    val metadata = metadataProvider.getMetadata(state.packageName)
                    repository.saveLearnedApp(
                        packageName = state.packageName,
                        appName = state.stats.appName,
                        versionCode = metadata?.versionCode ?: 1L,
                        versionName = metadata?.versionName ?: "unknown",
                        stats = state.stats
                    )

                    // FIX (2025-11-30): Signal speech engine to register new commands (P1-H5)
                    // Without this, learned commands aren't available for voice recognition
                    withContext(Dispatchers.Main) {
                        (accessibilityService as? IVoiceOSServiceInternal)?.onNewCommandsGenerated()
                        Log.i("LearnAppIntegration", "Signaled speech engine after exploration complete")
                    }

                    // FIX (2025-12-06): Enhanced completion notification with completeness
                    // FIX (2025-12-27): Use available ExplorationStats fields (clickedElements/nonBlockedElements/blockedElements don't exist)
                    val completeness = state.stats.completeness
                    val totalElements = state.stats.totalElements
                    val totalScreens = state.stats.totalScreens
                    val dangerousSkipped = state.stats.dangerousElementsSkipped

                    val message = if (completeness >= 95) {
                        "Learning complete! (${completeness.toInt()}%)\n" +
                        "$totalScreens screens, $totalElements elements discovered" +
                        if (dangerousSkipped > 0) ", $dangerousSkipped dangerous skipped" else ""
                    } else {
                        "${completeness.toInt()}% complete\n" +
                        "$totalScreens screens, $totalElements elements" +
                        if (dangerousSkipped > 0) ", $dangerousSkipped dangerous elements skipped" else ""
                    }

                    // Show success notification
                    showToastNotification(
                        title = "Learning Complete",
                        message = message
                    )

                    // VOS-META-001: Trigger manual command overlay if elements need commands
                    manualCommandIntegration?.onExplorationCompleted(state)
                }
            }

            is ExplorationState.Failed -> {
                // DEBUG (2025-12-08): Clear debug callback and overlay
                clearDebugOverlayCallback()

                // Dismiss floating widget
                floatingProgressWidget?.dismiss()

                // Hide login prompt overlay if showing
                hideLoginPromptOverlay()

                // Show error notification
                showToastNotification(
                    title = "Learning Failed",
                    message = "Failed to learn ${state.packageName}: ${state.error.message}"
                )
            }

            else -> {
                // Handle other states
            }
        }
    }

    /**
     * Pause exploration
     * FIX (2025-12-11): Added override for JITLearnerProvider interface
     */
    override fun pauseExploration() {
        Log.d(TAG, "Pause exploration requested")
        explorationEngine.pauseExploration()
    }

    /**
     * Resume exploration
     * FIX (2025-12-11): Added override for JITLearnerProvider interface
     */
    override fun resumeExploration() {
        Log.d(TAG, "Resume exploration requested")
        explorationEngine.resumeExploration()
    }

    /**
     * Stop exploration
     * FIX (2025-12-11): Added override for JITLearnerProvider interface
     */
    override fun stopExploration() {
        Log.d(TAG, "Stop exploration requested")
        explorationEngine.stopExploration()
    }

    /**
     * Get exploration state flow
     *
     * @return Exploration state flow
     */
    fun getExplorationState(): StateFlow<ExplorationState> {
        return explorationEngine.explorationState
    }

    /**
     * Get current foreground package name
     *
     * Returns the package name of the currently focused app.
     * Used by RelearnAppCommand to detect "relearn this app" target.
     *
     * @return Package name of foreground app, or null if not available
     */
    fun getCurrentForegroundPackage(): String? {
        return try {
            accessibilityService.rootInActiveWindow?.packageName?.toString()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get foreground package", e)
            null
        }
    }

    // ========== Debug Overlay Methods ==========

    /**
     * Set up debug overlay callback to wire FloatingProgressWidget's debug overlay
     * to the ExplorationEngine's screen and element events.
     *
     * This enables real-time visualization of:
     * - Elements discovered on each screen (with VUID, learning source)
     * - Navigation links (which elements lead to which screens)
     * - Exploration progress updates
     *
     * @since 2025-12-08 (Debug Overlay Feature)
     */
    private fun setupDebugOverlayCallback() {
        val widget = floatingProgressWidget ?: return

        // REWRITTEN (2025-12-08): Use new scrollable debug overlay
        // Create callback that forwards events to the debug overlay manager
        val callback = object : ExplorationDebugCallback {
            override fun onScreenExplored(
                elements: List<ElementInfo>,
                screenHash: String,
                activityName: String,
                packageName: String,
                parentScreenHash: String?
            ) {
                // Get debug overlay manager from widget and update it
                // Always track items, even if overlay is hidden
                val debugManager = widget.getDebugOverlayManager()
                debugManager.onScreenExplored(
                    elements = elements,
                    screenHash = screenHash,
                    activityName = activityName,
                    packageName = packageName,
                    parentScreenHash = parentScreenHash
                )
                Log.d(TAG, "📊 Debug tracker updated: ${elements.size} elements on $activityName")
            }

            override fun onElementNavigated(elementKey: String, destinationScreenHash: String) {
                // Record navigation link in debug overlay
                // Parse elementKey which is "screenHash:stableId"
                val debugManager = widget.getDebugOverlayManager()
                val parts = elementKey.split(":", limit = 2)
                if (parts.size == 2) {
                    debugManager.recordNavigation(parts[1], parts[0], destinationScreenHash)
                }
            }

            override fun onProgressUpdated(progress: Int) {
                // Progress handled by floating widget, not debug overlay
                Log.d(TAG, "📊 Progress: $progress%")
            }

            override fun onElementClicked(stableId: String, screenHash: String, vuid: String?) {
                // Track clicked item in debug overlay
                val debugManager = widget.getDebugOverlayManager()
                debugManager.markItemClicked(stableId, screenHash, null)
            }

            override fun onElementBlocked(stableId: String, screenHash: String, reason: String) {
                // Track blocked item in debug overlay
                val debugManager = widget.getDebugOverlayManager()
                debugManager.markItemBlocked(stableId, screenHash, reason)
            }
        }

        // AVU Quantizer Integration (2025-12-08): Chain the quantizer with the debug overlay callback
        // This enables real-time quantization during exploration while preserving debug overlay functionality
        avuQuantizerIntegration.setChainedCallback(callback)

        // Set the quantizer integration as the primary callback (it will chain to debug overlay)
        explorationEngine.setDebugCallback(avuQuantizerIntegration)
        Log.i(TAG, "📊 Debug overlay + AVU quantizer callbacks configured")
    }

    /**
     * Clear debug callback when exploration completes
     */
    private fun clearDebugOverlayCallback() {
        explorationEngine.setDebugCallback(null)
        avuQuantizerIntegration.setChainedCallback(null)
        floatingProgressWidget?.getDebugOverlayManager()?.reset()
    }

    // ========== App Management Methods ==========

    /**
     * Reset learned app for re-exploration
     *
     * Clears all exploration data (sessions, screen states, navigation edges)
     * and sets exploration status to PARTIAL, allowing the app to be re-learned.
     *
     * Use this when you want to re-explore an app (e.g., after testing fixes).
     *
     * @param packageName Package name of app to reset
     * @param callback Callback with success/failure result and message
     *
     * Example:
     * ```kotlin
     * learnAppIntegration.resetLearnedApp("com.realwear.testcomp") { success, message ->
     *     if (success) {
     *         Log.d("VoiceOS", "App reset: $message")
     *     } else {
     *         Log.e("VoiceOS", "Reset failed: $message")
     *     }
     * }
     * ```
     */
    fun resetLearnedApp(
        packageName: String,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        scope.launch {
            val result = repository.resetAppForRelearning(packageName)

            val success = when (result) {
                is com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult.Success -> true
                is com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult.Failure -> false
            }

            val message = when (result) {
                is com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult.Success ->
                    "App '$packageName' reset successfully. Launch the app again to re-learn it."
                is com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult.Failure ->
                    "Failed to reset app: ${result.reason}"
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    showToastNotification("App Reset", message)
                } else {
                    showToastNotification("Reset Failed", message)
                }
                callback(success, message)
            }
        }
    }

    /**
     * Delete learned app completely
     *
     * Removes the app entry AND all associated exploration data.
     * More thorough than resetLearnedApp() - completely removes the app from database.
     *
     * @param packageName Package name of app to delete
     * @param callback Callback with success/failure result and message
     *
     * Example:
     * ```kotlin
     * learnAppIntegration.deleteLearnedApp("com.realwear.testcomp") { success, message ->
     *     Log.d("VoiceOS", if (success) "Deleted" else "Failed: $message")
     * }
     * ```
     */
    fun deleteLearnedApp(
        packageName: String,
        callback: (success: Boolean, message: String) -> Unit
    ) {
        scope.launch {
            val result = repository.deleteAppCompletely(packageName)

            val success = when (result) {
                is com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult.Success -> true
                is com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult.Failure -> false
            }

            val message = when (result) {
                is com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult.Success ->
                    "App '$packageName' deleted completely. Launch the app again to learn it from scratch."
                is com.augmentalis.voiceoscore.learnapp.database.repository.RepositoryResult.Failure ->
                    "Failed to delete app: ${result.reason}"
            }

            withContext(Dispatchers.Main) {
                if (success) {
                    showToastNotification("App Deleted", message)
                } else {
                    showToastNotification("Delete Failed", message)
                }
                callback(success, message)
            }
        }
    }

    /**
     * Get list of all learned apps
     *
     * Returns list of package names for all apps that have been learned.
     * Useful for displaying a list of apps that can be reset/deleted.
     *
     * @param callback Callback with list of package names
     *
     * Example:
     * ```kotlin
     * learnAppIntegration.getLearnedApps { apps ->
     *     apps.forEach { packageName ->
     *         Log.d("VoiceOS", "Learned app: $packageName")
     *     }
     * }
     * ```
     */
    fun getLearnedApps(callback: (apps: List<String>) -> Unit) {
        scope.launch {
            val apps = repository.getAllLearnedApps()
            val packageNames = apps.map { it.packageName }

            withContext(Dispatchers.Main) {
                callback(packageNames)
            }
        }
    }

    /**
     * Check if app is learned
     *
     * @param packageName Package name to check
     * @param callback Callback with boolean result
     *
     * Example:
     * ```kotlin
     * learnAppIntegration.isAppLearned("com.realwear.testcomp") { isLearned ->
     *     Log.d("VoiceOS", "App learned: $isLearned")
     * }
     * ```
     */
    fun isAppLearned(packageName: String, callback: (isLearned: Boolean) -> Unit) {
        scope.launch {
            val isLearned = repository.isAppLearned(packageName)

            withContext(Dispatchers.Main) {
                callback(isLearned)
            }
        }
    }

    // ========== End App Management Methods ==========

    /**
     * Show login prompt overlay
     *
     * @param packageName Package name of app with login screen
     * @param appName Human-readable app name
     */
    private fun showLoginPromptOverlay(packageName: String, appName: String) {
        // Dismiss existing overlay if any
        hideLoginPromptOverlay()

        // Create and show new overlay
        val config = LoginPromptConfig(
            appName = appName,
            packageName = packageName,
            message = "LearnApp detected a login screen in $appName",
            showVoiceHints = true
        )

        loginPromptOverlay = LoginPromptOverlay(accessibilityService, config) { action ->
            when (action) {
                is LoginPromptAction.Skip -> {
                    // Resume exploration, skip this screen
                    resumeExploration()
                }

                is LoginPromptAction.Continue -> {
                    // User will login manually, then resume
                    // Do nothing, user will manually resume after login
                }

                is LoginPromptAction.Dismiss -> {
                    // Stop exploration
                    stopExploration()
                }

                is LoginPromptAction.Pause -> {
                    // Pause exploration for manual login
                    pauseExploration()
                }

                is LoginPromptAction.Stop -> {
                    // Stop exploration completely
                    stopExploration()
                }

                else -> {
                    // Handle any future LoginPromptAction types
                    Log.w(TAG, "Unhandled LoginPromptAction: $action")
                }
            }
        }

        loginPromptOverlay?.show()
    }

    /**
     * Hide login prompt overlay
     */
    private fun hideLoginPromptOverlay() {
        loginPromptOverlay?.hide()
        loginPromptOverlay = null
    }

    /**
     * Calculate current progress percentage from exploration state
     * FIX (2025-12-06): Helper method for v1.8 command bar integration
     *
     * @param state Current exploration state
     * @return Progress percentage (0-100)
     */
    private fun calculateProgress(state: ExplorationState): Int {
        return try {
            when (state) {
                is ExplorationState.Running -> {
                    // Calculate from screens explored
                    val percentage = state.progress.calculatePercentage()
                    (percentage * 100).toInt().coerceIn(0, 100)
                }
                is ExplorationState.Completed -> {
                    state.stats.completeness.toInt()
                }
                is ExplorationState.PausedByUser -> {
                    // Calculate from screens explored
                    val percentage = state.progress.calculatePercentage()
                    (percentage * 100).toInt().coerceIn(0, 100)
                }
                is ExplorationState.Paused -> {
                    // FIX (2025-12-07): Handle unified Paused state
                    val percentage = state.progress.calculatePercentage()
                    (percentage * 100).toInt().coerceIn(0, 100)
                }
                else -> 0
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to calculate progress", e)
            0
        }
    }

    /**
     * Extract all text from accessibility node tree
     * FIX (2025-12-06): Helper method for blocked state detection
     *
     * Recursively traverses the accessibility tree and extracts all text
     * from text fields and content descriptions.
     *
     * @receiver Root accessibility node
     * @return All text content concatenated with spaces
     */
    private fun AccessibilityNodeInfo.getAllText(): String {
        val builder = StringBuilder()

        fun extract(node: AccessibilityNodeInfo?) {
            if (node == null) return

            // Add node's text
            node.text?.let { builder.append(it).append(" ") }

            // Add content description
            node.contentDescription?.let { builder.append(it).append(" ") }

            // Recursively extract from children
            for (i in 0 until node.childCount) {
                try {
                    val child = node.getChild(i)
                    extract(child)
                    child?.recycle()
                } catch (e: Exception) {
                    // Child may have been detached, continue
                }
            }
        }

        extract(this)
        return builder.toString()
    }

    /**
     * Detect blocked state from accessibility tree
     *
     * Analyzes the current screen to detect permission dialogs or login screens
     * that require manual intervention before exploration can continue.
     *
     * Phase 4: Blocked state detection for auto-pause logic
     *
     * @param screen Root accessibility node
     * @return Blocked state type, or null if not blocked
     */
    private fun detectBlockedState(screen: AccessibilityNodeInfo): BlockedState? {
        val text = screen.getAllText()
        val packageName = screen.packageName?.toString() ?: ""

        // Permission dialog detection
        if (text.contains("needs permission", ignoreCase = true) ||
            text.contains("allow", ignoreCase = true) ||
            text.contains("deny", ignoreCase = true) ||
            packageName == "com.android.permissioncontroller" ||
            packageName.contains("permission")) {
            return BlockedState.PERMISSION_REQUIRED
        }

        // Login screen detection
        if (text.contains("sign in", ignoreCase = true) ||
            text.contains("log in", ignoreCase = true) ||
            text.contains("username", ignoreCase = true) ||
            text.contains("password", ignoreCase = true) ||
            text.contains("email", ignoreCase = true) && text.contains("password", ignoreCase = true)) {
            return BlockedState.LOGIN_REQUIRED
        }

        return null
    }

    /**
     * Show toast notification
     *
     * @param title Notification title
     * @param message Error message
     */
    private fun showToastNotification(title: String, message: String) {
        val toastMessage =  "$title : $message"
        Log.i("LearnAppIntegration", "showToastNotification: $toastMessage")
        scope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    accessibilityService,
                    toastMessage,
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    /**
     * Get ExplorationEngine instance for external integration
     *
     * PHASE 3 (2025-12-08): Added for CommandDiscoveryIntegration
     * Allows external components to observe exploration state via StateFlow
     *
     * ## Usage:
     * ```kotlin
     * val engine = learnAppIntegration.getExplorationEngine()
     * engine.state()
     *     .filterIsInstance<ExplorationState.Completed>()
     *     .collect { state ->
     *         // Handle exploration completion
     *     }
     * ```
     *
     * @return ExplorationEngine instance
     * @since 1.0.0 (Phase 3: Command Discovery integration)
     */
    fun getExplorationEngine(): ExplorationEngine = explorationEngine

    /**
     * Get JustInTimeLearner instance for JITLearningService integration
     *
     * FIX (2025-12-11): Added for JITLearningService AIDL integration
     * Allows JITLearningService to access the actual learning engine for:
     * - Pause/resume control
     * - State queries (screens learned, elements discovered)
     * - Event callback registration
     *
     * @return JustInTimeLearner instance
     * @since 2.0.0 (JIT-LearnApp Separation)
     */
    fun getJustInTimeLearner(): JustInTimeLearner = justInTimeLearner

    /**
     * Get current root accessibility node
     *
     * FIX (2025-12-11): Added for JITLearningService getCurrentScreenInfo()
     * Provides access to the current screen's accessibility tree.
     *
     * @return Root AccessibilityNodeInfo, or null if not available
     */
    override fun getCurrentRootNode(): android.view.accessibility.AccessibilityNodeInfo? {
        return try {
            accessibilityService.rootInActiveWindow
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get root node", e)
            null
        }
    }

    // ========== JITLearnerProvider Implementation (2025-12-11) ==========

    /** JIT event callback storage */
    private var jitEventCallback: JITEventCallback? = null

    /**
     * Pause JIT learning
     */
    override fun pauseLearning() {
        justInTimeLearner.pause()
    }

    /**
     * Resume JIT learning
     */
    override fun resumeLearning() {
        justInTimeLearner.resume()
    }

    /**
     * Check if learning is paused
     */
    override fun isLearningPaused(): Boolean {
        return justInTimeLearner.isPausedState()
    }

    /**
     * Check if learning is actively running
     */
    override fun isLearningActive(): Boolean {
        return justInTimeLearner.isLearningActive()
    }

    /**
     * Get stats: screens learned count
     */
    override fun getScreensLearnedCount(): Int {
        return justInTimeLearner.getStats().screensLearned
    }

    /**
     * Get stats: elements discovered count
     */
    override fun getElementsDiscoveredCount(): Int {
        return justInTimeLearner.getStats().elementsDiscovered
    }

    /**
     * Get current package being learned
     */
    override fun getCurrentPackage(): String? {
        return justInTimeLearner.getStats().currentPackage
    }

    /**
     * Check if screen has been learned
     * FIX L-P1-2 (2025-12-22): Converted to suspend function to eliminate runBlocking ANR risk
     * Now uses proper suspend pattern with withContext(Dispatchers.IO)
     */
    override suspend fun hasScreen(screenHash: String): Boolean {
        return withContext(Dispatchers.IO) {
            justInTimeLearner.hasScreen(screenHash)
        }
    }

    /**
     * Get all learned screen hashes for a package
     * FIX (2025-12-11): P2 feature implementation
     * FIX L-P1-2 (2025-12-22): Converted to suspend function to eliminate runBlocking ANR risk
     * Now uses proper suspend pattern with withContext(Dispatchers.IO)
     */
    override suspend fun getLearnedScreenHashes(packageName: String): List<String> {
        return withContext(Dispatchers.IO) {
            try {
                databaseManager.screenContexts.getByPackage(packageName)
                    .map { it.screenHash }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting learned screen hashes for $packageName", e)
                emptyList()
            }
        }
    }

    /**
     * Start automated exploration (v2.1 - P2 Feature)
     * Note: pauseExploration, resumeExploration, stopExploration are defined above
     */
    override fun startExploration(packageName: String): Boolean {
        Log.i(TAG, "Start exploration requested via IPC for: $packageName")
        return try {
            scope.launch {
                explorationEngine.startExploration(packageName, null)
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start exploration", e)
            false
        }
    }

    /**
     * Get current exploration progress (v2.1 - P2 Feature)
     */
    override fun getExplorationProgress(): com.augmentalis.jitlearning.ExplorationProgress {
        val state = explorationEngine.explorationState.value
        return when (state) {
            is ExplorationState.Idle -> com.augmentalis.jitlearning.ExplorationProgress.idle()
            is ExplorationState.Running -> com.augmentalis.jitlearning.ExplorationProgress.running(
                packageName = state.packageName,
                screensExplored = state.progress.screensExplored,
                elementsDiscovered = state.progress.elementsDiscovered,
                currentDepth = state.progress.currentDepth,
                progressPercent = (state.progress.calculatePercentage() * 100).toInt(),
                elapsedMs = state.progress.elapsedTimeMs
            )
            is ExplorationState.Paused -> com.augmentalis.jitlearning.ExplorationProgress.paused(
                packageName = state.packageName,
                screensExplored = state.progress.screensExplored,
                elementsDiscovered = state.progress.elementsDiscovered,
                pauseReason = state.reason
            )
            is ExplorationState.PausedForLogin -> com.augmentalis.jitlearning.ExplorationProgress.paused(
                packageName = state.packageName,
                screensExplored = state.progress.screensExplored,
                elementsDiscovered = state.progress.elementsDiscovered,
                pauseReason = "Login screen detected"
            )
            is ExplorationState.PausedByUser -> com.augmentalis.jitlearning.ExplorationProgress.paused(
                packageName = state.packageName,
                screensExplored = state.progress.screensExplored,
                elementsDiscovered = state.progress.elementsDiscovered,
                pauseReason = "User paused"
            )
            is ExplorationState.Completed -> com.augmentalis.jitlearning.ExplorationProgress.completed(
                packageName = state.packageName,
                screensExplored = state.stats.totalScreens,
                elementsDiscovered = state.stats.totalElements
            )
            is ExplorationState.Failed -> com.augmentalis.jitlearning.ExplorationProgress(
                packageName = state.packageName,
                state = "failed",
                screensExplored = state.partialProgress?.screensExplored ?: 0,
                elementsDiscovered = state.partialProgress?.elementsDiscovered ?: 0
            )
            else -> com.augmentalis.jitlearning.ExplorationProgress.idle()
        }
    }

    /**
     * Set exploration progress callback (v2.1 - P2 Feature)
     */
    override fun setExplorationCallback(callback: ExplorationProgressCallback?) {
        explorationProgressCallback = callback

        if (callback != null) {
            // Observe exploration state changes and forward to callback
            scope.launch {
                explorationEngine.explorationState.collect { state ->
                    val progress = getExplorationProgress()
                    when (state) {
                        is ExplorationState.Completed -> callback.onCompleted(progress)
                        is ExplorationState.Failed -> callback.onFailed(progress, state.error.message ?: "Unknown error")
                        else -> callback.onProgressUpdate(progress)
                    }
                }
            }
        }
    }

    // Exploration progress callback reference
    private var explorationProgressCallback: ExplorationProgressCallback? = null

    /**
     * Set event callback for JIT events
     */
    override fun setEventCallback(callback: JITEventCallback?) {
        jitEventCallback = callback

        // Wire callback to JustInTimeLearner
        if (callback != null) {
            justInTimeLearner.setEventCallback(object : JustInTimeLearner.JITEventCallback {
                override fun onScreenLearned(packageName: String, screenHash: String, elementCount: Int) {
                    callback.onScreenLearned(packageName, screenHash, elementCount)
                }

                override fun onElementDiscovered(stableId: String, vuid: String?) {
                    callback.onElementDiscovered(stableId, vuid)
                }

                override fun onLoginDetected(packageName: String, screenHash: String) {
                    callback.onLoginDetected(packageName, screenHash)
                }
            })
        } else {
            justInTimeLearner.setEventCallback(null)
        }
    }

    /**
     * Wire JITLearningService to this provider
     *
     * Call this after LearnAppIntegration is initialized to connect
     * JITLearningService to JustInTimeLearner via this provider interface.
     */
    private fun wireJITLearningService() {
        try {
            val service = JITLearningService.getInstance()
            if (service != null) {
                service.setLearnerProvider(this)
                Log.i(TAG, "JITLearningService wired to LearnAppIntegration")
            } else {
                Log.d(TAG, "JITLearningService not running yet")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to wire JITLearningService", e)
        }
    }

    // ========== End JITLearnerProvider Implementation ==========

    /**
     * Shutdown integration and cancel all background jobs
     * FIX (2025-12-10): Added shutdown() method per spec Section 2.5
     *
     * Immediately cancels all coroutines without waiting.
     * Use shutdownGracefully() if you need to wait for pending operations.
     */
    fun shutdown() {
        Log.i(TAG, "Shutting down LearnApp integration")
        scope.cancel()
    }

    /**
     * Gracefully shutdown integration with timeout
     * FIX (2025-12-10): Added per spec Section 2.5
     *
     * Waits for current operations to complete before cancelling.
     *
     * @param timeoutMs Maximum time to wait for operations (default 5000ms)
     */
    suspend fun shutdownGracefully(timeoutMs: Long = 5000) {
        Log.i(TAG, "Graceful shutdown initiated")

        try {
            withTimeout(timeoutMs) {
                // Wait for current operations
                scope.coroutineContext[kotlinx.coroutines.Job]?.children?.forEach { job ->
                    try {
                        job.join()
                    } catch (e: CancellationException) {
                        // Expected during cancellation
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.w(TAG, "Graceful shutdown timed out after ${timeoutMs}ms, forcing cancellation")
        }

        scope.cancel()
        Log.i(TAG, "Shutdown complete")
    }

    /**
     * Cleanup (call in onDestroy)
     * FIX (2025-11-30): Added scope.cancel() to prevent coroutine leaks
     * FIX (2025-12-04): Enhanced cleanup to fix overlay memory leak
     * FIX (2025-12-07): Updated to use FloatingProgressWidget instead of ProgressOverlayManager
     * FIX (2025-12-10): Now calls shutdown() to cancel scope
     *
     * Root cause: Memory leak chain:
     *   VoiceOSService → learnAppIntegration → floatingProgressWidget → widgetView (retained)
     *
     * Solution:
     *   1. Cancel coroutines first to stop any pending operations
     *   2. Cleanup all managers in proper order
     *   3. Clear manager references to break leak chain
     *
     * Leak verification:
     *   - LeakCanary should show zero leaks after this cleanup
     *   - Memory profiler should show FloatingProgressWidget GC'd
     */
    fun cleanup() {
        if (developerSettings.isVerboseLoggingEnabled()) {
            Log.d(TAG, "Cleaning up LearnAppIntegration")
        }

        try {
            // 1. Cancel all coroutines first to stop pending operations
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Cancelling coroutine scope...")
            }
            shutdown()  // FIX (2025-12-10): Use shutdown() method
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✓ Coroutine scope cancelled")
            }

            // 2. Hide login prompt overlay
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Hiding login prompt overlay...")
            }
            hideLoginPromptOverlay()
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✓ Login prompt overlay hidden")
            }

            // 3. Cleanup consent dialog manager
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Cleaning up consent dialog manager...")
            }
            consentDialogManager.cleanup()
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✓ Consent dialog manager cleaned up")
            }

            // 4. CRITICAL: Cleanup floating progress widget (fixes memory leak)
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Cleaning up floating progress widget...")
            }
            floatingProgressWidget?.cleanup()
            floatingProgressWidget = null
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✓ Floating progress widget cleaned up (leak chain broken)")
            }

            // 5. Cleanup just-in-time learner
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Destroying just-in-time learner...")
            }
            justInTimeLearner.destroy()
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✓ Just-in-time learner destroyed")
            }

            // 6. CRITICAL: Cleanup exploration engine (FIX 2025-12-04 - memory leak fix)
            // Issue: ExplorationEngine holds references to:
            //   - screenStateManager (ConcurrentHashMap with AccessibilityNodeInfo refs)
            //   - clickTracker (ConcurrentHashMap)
            //   - navigationGraphBuilder (navigation graph data)
            //   - scope (coroutines with node references)
            // Without cleanup, these leak 30+ KB per exploration session
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Stopping and cleaning up exploration engine...")
            }
            explorationEngine.stopExploration()  // stopExploration() calls cleanup() internally
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✓ Exploration engine stopped and cleaned up (memory leak fixed)")
            }

            // 7. Clear singleton reference to allow GC
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "Clearing singleton instance reference...")
            }
            INSTANCE = null
            if (developerSettings.isVerboseLoggingEnabled()) {
                Log.d(TAG, "✓ Singleton instance cleared")
            }

            Log.i(TAG, "✓ LearnAppIntegration cleanup complete - all resources released")

        } catch (e: Exception) {
            Log.e(TAG, "Error during LearnAppIntegration cleanup", e)
            Log.e(TAG, "Cleanup error type: ${e.javaClass.simpleName}")
            Log.e(TAG, "Cleanup error message: ${e.message}")
            // Still clear singleton on error to prevent partial leak
            INSTANCE = null
        }
    }

    /**
     * Trigger LearnApp exploration programmatically
     *
     * This method allows external components (like broadcast receivers)
     * to trigger LearnApp exploration for a specific package.
     *
     * @param packageName Package name of the app to learn
     *
     * Example usage from broadcast receiver:
     * ```kotlin
     * LearnAppIntegration.getInstance()?.triggerLearning("com.google.android.gm")
     * ```
     */
    fun triggerLearning(packageName: String) {
        if (developerSettings.isVerboseLoggingEnabled()) {
            Log.d("LearnAppIntegration", "Triggering LearnApp for package: $packageName")
        }
        startExplorationInternal(packageName)
    }

    // ========== AVU Quantized Context API (2025-12-08) ==========

    /**
     * Get quantized context for a learned app
     *
     * Returns the compact, NLU-optimized representation of the app's UI structure.
     * Use this for LLM context loading, action prediction, or voice command matching.
     *
     * @param packageName Package name of the learned app
     * @return QuantizedContext if available, null if app not learned or no quantized data
     */
    suspend fun getQuantizedContext(packageName: String): com.augmentalis.voiceoscore.learnapp.ai.quantized.QuantizedContext? {
        return avuQuantizerIntegration.getQuantizedContext(packageName)
    }

    /**
     * Generate LLM prompt for a user goal
     *
     * Creates a prompt suitable for LLM consumption based on the app's quantized context.
     * Choose format based on token budget:
     * - COMPACT: ~50-100 tokens (quick context)
     * - HTML: ~200 tokens (research-backed format)
     * - FULL: ~500+ tokens (complete context)
     *
     * @param packageName Package name of the learned app
     * @param userGoal User's goal (e.g., "open settings", "send message")
     * @param format Prompt format (COMPACT, HTML, or FULL)
     * @return LLM-ready prompt string, or null if no context available
     */
    suspend fun generateLLMPrompt(
        packageName: String,
        userGoal: String,
        format: com.augmentalis.voiceoscore.learnapp.ai.LLMPromptFormat =
            com.augmentalis.voiceoscore.learnapp.ai.LLMPromptFormat.COMPACT
    ): String? {
        return avuQuantizerIntegration.generateLLMPrompt(packageName, userGoal, format)
    }

    /**
     * Generate action prediction prompt for current screen
     *
     * Creates a prompt optimized for predicting the next action based on user intent.
     * Shows available elements and navigation options for the current screen.
     *
     * @param packageName Package name of the app
     * @param currentScreenHash Hash of the current screen
     * @param userIntent User's intent (e.g., "go back", "tap search")
     * @return Action prediction prompt, or null if no context available
     */
    suspend fun generateActionPredictionPrompt(
        packageName: String,
        currentScreenHash: String,
        userIntent: String
    ): String? {
        return avuQuantizerIntegration.generateActionPredictionPrompt(packageName, currentScreenHash, userIntent)
    }

    /**
     * Check if quantized context exists for an app
     *
     * @param packageName Package name to check
     * @return true if quantized context is available
     */
    suspend fun hasQuantizedContext(packageName: String): Boolean {
        return avuQuantizerIntegration.hasQuantizedContext(packageName)
    }

    /**
     * List all apps with quantized contexts
     *
     * @return List of package names with available quantized contexts
     */
    suspend fun listQuantizedApps(): List<String> {
        return avuQuantizerIntegration.listQuantizedPackages()
    }

    companion object {
        private const val TAG = "LearnAppIntegration"

        @Volatile
        private var INSTANCE: LearnAppIntegration? = null

        /**
         * Initialize integration
         *
         * Call this from VoiceOSService.onServiceConnected()
         *
         * @param context Application context
         * @param accessibilityService Accessibility service instance
         * @return Integration instance
         */
        fun initialize(
            context: Context,
            accessibilityService: AccessibilityService
        ): LearnAppIntegration {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: LearnAppIntegration(
                    context.applicationContext,
                    accessibilityService
                ).also {
                    INSTANCE = it
                }
            }
        }

        /**
         * Get integration instance
         *
         * @return Integration instance
         * @throws IllegalStateException if not initialized
         */
        fun getInstance(): LearnAppIntegration {
            return INSTANCE ?: throw IllegalStateException(
                "LearnAppIntegration not initialized. Call initialize(context, service) first."
            )
        }

        /**
         * Open developer settings activity
         *
         * Launches DeveloperSettingsActivity to configure all LearnApp settings.
         *
         * @param context Context to launch activity from
         *
         * Example usage:
         * ```kotlin
         * // From VoiceOS main menu
         * LearnAppIntegration.openDeveloperSettings(context)
         * ```
         */
        fun openDeveloperSettings(context: Context) {
            val intent = android.content.Intent(
                context,
                com.augmentalis.voiceoscore.learnapp.settings.ui.DeveloperSettingsActivity::class.java
            ).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
