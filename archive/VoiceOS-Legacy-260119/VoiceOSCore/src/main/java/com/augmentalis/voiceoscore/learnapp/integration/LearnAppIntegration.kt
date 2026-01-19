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
import com.augmentalis.voiceoscore.learnapp.database.LearnAppDatabaseAdapter
import com.augmentalis.voiceoscore.learnapp.database.repository.AppMetadataProvider
import com.augmentalis.voiceoscore.learnapp.database.repository.LearnAppRepository
import com.augmentalis.voiceoscore.learnapp.database.repository.SessionCreationResult
import com.augmentalis.voiceoscore.learnapp.detection.AppLaunchDetector
import com.augmentalis.voiceoscore.learnapp.detection.LearnedAppTracker
import com.augmentalis.voiceoscore.learnapp.exploration.DFSExplorationStrategy
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine
import com.augmentalis.voiceoscore.learnapp.exploration.ExplorationStrategy
import com.augmentalis.voiceoscore.learnapp.models.ExplorationState
import com.augmentalis.voiceoscore.learnapp.overlays.LoginPromptAction
import com.augmentalis.voiceoscore.learnapp.overlays.LoginPromptConfig
import com.augmentalis.voiceoscore.learnapp.overlays.LoginPromptOverlay
import com.augmentalis.voiceoscore.learnapp.ui.ConsentDialogManager
import com.augmentalis.voiceoscore.learnapp.ui.ProgressOverlayManager
import com.augmentalis.avidcreator.AvidCreator
import com.augmentalis.avidcreator.alias.AvidAliasManager
import com.augmentalis.avidcreator.database.AvidCreatorDatabase
import com.augmentalis.avidcreator.thirdparty.ThirdPartyAvidGenerator
import com.augmentalis.jitlearning.ExplorationProgress
import com.augmentalis.jitlearning.ExplorationProgressCallback
import com.augmentalis.jitlearning.JITEventCallback
import com.augmentalis.jitlearning.JITLearnerProvider
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

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
    private val learnedAppTracker: LearnedAppTracker
    private val appLaunchDetector: AppLaunchDetector
    private val consentDialogManager: ConsentDialogManager
    private val progressOverlayManager: ProgressOverlayManager
    private val explorationEngine: ExplorationEngine

    /**
     * Database adapter and repository
     */
    private val databaseAdapter: LearnAppDatabaseAdapter
    private val repository: LearnAppRepository
    private val metadataProvider: AppMetadataProvider

    /**
     * Login prompt overlay (created on-demand)
     */
    private var loginPromptOverlay: LoginPromptOverlay? = null

    /**
     * AvidCreator components
     */
    private val uuidCreator: AvidCreator
    private val thirdPartyGenerator: ThirdPartyAvidGenerator
    private val aliasManager: AvidAliasManager

    /**
     * Exploration strategy
     */
    private val explorationStrategy: ExplorationStrategy = DFSExplorationStrategy()

    init {
        // Initialize database adapter (bridges Room API to SQLDelight)
        databaseAdapter = LearnAppDatabaseAdapter.getInstance(context)
        // Get VoiceOSDatabaseManager from adapter for repository
        val databaseManager = com.augmentalis.database.VoiceOSDatabaseManager.getInstance(
            com.augmentalis.database.DatabaseDriverFactory(context)
        )
        repository = LearnAppRepository(databaseManager, context)
        metadataProvider = AppMetadataProvider(context)

        // Initialize AvidCreator components
        uuidCreator = AvidCreator.getInstance()
        thirdPartyGenerator = ThirdPartyAvidGenerator(context)
        val uuidCreatorDatabase = AvidCreatorDatabase.getInstance(context)
        aliasManager = AvidAliasManager(uuidCreatorDatabase)

        // Initialize LearnApp components
        learnedAppTracker = LearnedAppTracker(context)
        appLaunchDetector = AppLaunchDetector(context, learnedAppTracker)
        consentDialogManager = ConsentDialogManager(accessibilityService, learnedAppTracker)
        progressOverlayManager = ProgressOverlayManager(accessibilityService)

        // Initialize exploration engine
        explorationEngine = ExplorationEngine(
            context = context,
            accessibilityService = accessibilityService,
            uuidCreator = uuidCreator,
            thirdPartyGenerator = thirdPartyGenerator,
            aliasManager = aliasManager,
            repository = repository,
            databaseManager = databaseManager,
            strategy = explorationStrategy
        )

        // Set up event listeners
        setupEventListeners()
    }

    /**
     * Setup event listeners
     */
    private fun setupEventListeners() {
        // Listen for app launch events with throttling
        // FR-004: Implement event debouncing to prevent continuous events from causing flickering
        // FR-010: Handle apps generating 10+ events/sec without freezing
        // FR-011: Prevent consent dialog from showing more than once per app per session
        scope.launch {
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
                .collect { event ->
                    when (event) {
                        is com.augmentalis.voiceoscore.learnapp.detection.AppLaunchEvent.NewAppDetected -> {
                            // Show consent dialog
                            consentDialogManager.showConsentDialog(
                                packageName = event.packageName,
                                appName = event.appName
                            )
                        }
                        else -> {
                            // Handle other events
                        }
                    }
                }
        }

        // Listen for consent responses
        scope.launch {
            consentDialogManager.consentResponses.collect { response ->
                when (response) {
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Approved -> {
                        // Start exploration
                        startExplorationInternal(response.packageName)
                    }

                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Declined,
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Dismissed,
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Skipped,
                    is com.augmentalis.voiceoscore.learnapp.ui.ConsentResponse.Timeout -> {
                        // Do nothing for declined/dismissed/skipped/timeout
                    }
                }
            }
        }

        // Listen for exploration state changes
        scope.launch {
            explorationEngine.explorationState.collect { state ->
                handleExplorationStateChange(state)
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
    }

    /**
     * Get the exploration engine instance.
     * Used by CommandDiscoveryIntegration to observe exploration state.
     *
     * @return ExplorationEngine instance
     */
    fun getExplorationEngine(): ExplorationEngine = explorationEngine

    /**
     * Start exploration of app (internal implementation)
     *
     * @param packageName Package name to explore
     */
    private fun startExplorationInternal(packageName: String) {
        scope.launch {
            // Create session using safe auto-create pattern
            when (val result = repository.createExplorationSessionSafe(packageName)) {
                is SessionCreationResult.Created -> {
                    // Session created successfully
                    if (result.appWasCreated) {
                        // Log that parent app was auto-created
                        android.util.Log.d(
                            "LearnAppIntegration",
                            "Auto-created LearnedApp for $packageName from ${result.metadataSource}"
                        )
                    }
                    // Start exploration engine with session ID for database persistence
                    explorationEngine.startExploration(packageName, result.sessionId)
                }

                is SessionCreationResult.Failed -> {
                    // Handle failure
                    android.util.Log.e(
                        "LearnAppIntegration",
                        "Failed to create session for $packageName: ${result.reason}", result.cause
                    )

                    // Hide progress overlay
                    progressOverlayManager.hideProgressOverlay()

                    // Show error notification
                    showToastNotification(
                        title = "Failed to Start Learning",
                        message = "Could not start learning $packageName: ${result.reason}"
                    )
                }
            }
        }
    }

    /**
     * Handle exploration state change
     *
     * @param state New exploration state
     */
    private fun handleExplorationStateChange(state: ExplorationState) {
        when (state) {
            is ExplorationState.Idle -> {
                // Hide progress overlay
                progressOverlayManager.hideProgressOverlay()
            }

            is ExplorationState.Running -> {
                // Show/update progress overlay
                val message = "Learning ${state.progress.appName}... (${state.progress.screensExplored} screens)"
                if (!progressOverlayManager.isOverlayShowing()) {
                    progressOverlayManager.showProgressOverlay(message)
                } else {
                    progressOverlayManager.updateMessage(message)
                }
            }

            is ExplorationState.PausedForLogin -> {
                // Show login prompt overlay
                showLoginPromptOverlay(state.packageName, state.progress.appName)
            }

            is ExplorationState.PausedByUser -> {
                // Update UI to show paused state
            }

            is ExplorationState.Completed -> {
                // Hide progress overlay
                progressOverlayManager.hideProgressOverlay()

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

                    // Show success notification
                    showToastNotification(
                        title = "Learning Complete",
                        message = "${state.stats.appName} learned successfully! " +
                                "${state.stats.totalScreens} screens, ${state.stats.totalElements} elements."
                    )
                }
            }

            is ExplorationState.Failed -> {
                // Hide progress overlay
                progressOverlayManager.hideProgressOverlay()

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

    // ========== JITLearnerProvider Interface Methods ==========

    /**
     * Start exploration (interface implementation)
     */
    override fun startExploration(packageName: String): Boolean {
        startExplorationInternal(packageName)
        return true
    }

    /**
     * Pause exploration
     */
    override fun pauseExploration() {
        explorationEngine.pauseExploration()
    }

    /**
     * Resume exploration
     */
    override fun resumeExploration() {
        explorationEngine.resumeExploration()
    }

    /**
     * Stop exploration
     */
    override fun stopExploration() {
        explorationEngine.stopExploration()
    }

    /**
     * Get current root accessibility node
     */
    override fun getCurrentRootNode(): AccessibilityNodeInfo? {
        return accessibilityService.rootInActiveWindow
    }

    /**
     * Check if screen has been learned
     */
    override suspend fun hasScreen(screenHash: String): Boolean {
        return repository.getScreenState(screenHash) != null
    }

    /**
     * Get all learned screen hashes for a package
     */
    override suspend fun getLearnedScreenHashes(packageName: String): List<String> {
        return repository.getScreenStatesByPackage(packageName).map { it.screenHash }
    }

    private var eventCallback: JITEventCallback? = null

    /**
     * Set event callback for JIT events
     */
    override fun setEventCallback(callback: JITEventCallback?) {
        eventCallback = callback
    }

    /**
     * Get current exploration progress
     */
    override fun getExplorationProgress(): ExplorationProgress {
        val explorationState = explorationEngine.explorationState.value
        return when (explorationState) {
            is ExplorationState.Running -> convertProgressToJIT(explorationState.progress, explorationState.packageName)
            is ExplorationState.Completed -> convertStatsToJIT(explorationState.stats)
            else -> ExplorationProgress.idle()
        }
    }

    private var explorationCallback: ExplorationProgressCallback? = null

    /**
     * Set exploration progress callback
     */
    override fun setExplorationCallback(callback: ExplorationProgressCallback?) {
        explorationCallback = callback
    }

    /**
     * Convert models ExplorationProgress to JIT ExplorationProgress
     */
    private fun convertProgressToJIT(progress: com.augmentalis.voiceoscore.learnapp.models.ExplorationProgress, packageName: String): com.augmentalis.jitlearning.ExplorationProgress {
        return com.augmentalis.jitlearning.ExplorationProgress(
            screensExplored = progress.screensExplored,
            elementsDiscovered = progress.elementsDiscovered,
            currentDepth = progress.currentDepth,
            packageName = packageName,
            state = "running",
            pauseReason = null,
            progressPercent = (progress.calculatePercentage() * 100).toInt(),
            elapsedMs = progress.elapsedTimeMs
        )
    }

    /**
     * Convert ExplorationStats to JIT ExplorationProgress
     */
    private fun convertStatsToJIT(stats: com.augmentalis.voiceoscore.learnapp.models.ExplorationStats): com.augmentalis.jitlearning.ExplorationProgress {
        return com.augmentalis.jitlearning.ExplorationProgress(
            screensExplored = stats.totalScreens,
            elementsDiscovered = stats.totalElements,
            currentDepth = stats.maxDepth,
            packageName = stats.packageName,
            state = "completed",
            pauseReason = null,
            progressPercent = 100,
            elapsedMs = stats.durationMs
        )
    }

    /**
     * Get exploration state flow
     *
     * @return Exploration state flow
     */
    fun getExplorationState(): StateFlow<ExplorationState> {
        return explorationEngine.explorationState
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

                is LoginPromptAction.Pause -> {
                    // Pause exploration temporarily
                    pauseExploration()
                }

                is LoginPromptAction.Stop,
                is LoginPromptAction.Dismiss -> {
                    // Stop exploration
                    stopExploration()
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

    // ==================== JITLearnerProvider Implementation ====================

    /**
     * Pause JIT learning
     */
    override fun pauseLearning() {
        pauseExploration()
    }

    /**
     * Resume JIT learning
     */
    override fun resumeLearning() {
        resumeExploration()
    }

    /**
     * Check if learning is paused
     */
    override fun isLearningPaused(): Boolean {
        val state = explorationEngine.explorationState.value
        return state is ExplorationState.PausedForLogin
    }

    /**
     * Check if learning is actively running
     */
    override fun isLearningActive(): Boolean {
        val state = explorationEngine.explorationState.value
        return state is ExplorationState.Running
    }

    /**
     * Get screens learned count
     */
    override fun getScreensLearnedCount(): Int {
        val state = explorationEngine.explorationState.value
        return when (state) {
            is ExplorationState.Running -> state.progress.screensExplored
            is ExplorationState.PausedForLogin -> state.progress.screensExplored
            is ExplorationState.Completed -> state.stats.totalScreens
            else -> 0
        }
    }

    /**
     * Get elements discovered count
     */
    override fun getElementsDiscoveredCount(): Int {
        val state = explorationEngine.explorationState.value
        return when (state) {
            is ExplorationState.Running -> state.progress.elementsDiscovered
            is ExplorationState.PausedForLogin -> state.progress.elementsDiscovered
            is ExplorationState.Completed -> state.stats.totalElements
            else -> 0
        }
    }

    /**
     * Get current package being learned
     */
    override fun getCurrentPackage(): String? {
        val state = explorationEngine.explorationState.value
        return when (state) {
            is ExplorationState.Running -> state.packageName
            is ExplorationState.PausedForLogin -> state.packageName
            is ExplorationState.Completed -> state.packageName
            else -> null
        }
    }

    /**
     * Cleanup (call in onDestroy)
     */
    fun cleanup() {
        hideLoginPromptOverlay()
        consentDialogManager.cleanup()
        progressOverlayManager.cleanup()
    }

    companion object {
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
    }
}
