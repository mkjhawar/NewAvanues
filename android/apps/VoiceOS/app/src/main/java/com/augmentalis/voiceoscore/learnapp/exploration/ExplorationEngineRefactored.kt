/**
 * ExplorationEngineRefactored.kt - Main exploration engine (SOLID refactored)
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ExplorationEngineRefactored.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 * Refactored: 2026-01-15 (SOLID extraction - orchestration only)
 *
 * This is the SOLID-refactored version of ExplorationEngine.kt.
 * All major responsibilities have been extracted into focused classes:
 *
 * - DFSExplorer: DFS algorithm, stack management, loop prevention
 * - ElementClicker: Click operations, retry logic, gesture fallback
 * - ElementRegistrar: UUID generation, alias management, voice commands
 * - ExplorationNotifier: Notifications, sound feedback
 * - ExplorationMetrics: VUID metrics, debug overlay
 * - DangerDetector: Dangerous element detection
 *
 * This orchestration class coordinates all components and manages the
 * exploration lifecycle (start, pause, resume, stop).
 */

@file:Suppress("UNNECESSARY_SAFE_CALL")  // Element properties can be null at runtime

package com.augmentalis.voiceoscore.learnapp.exploration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import com.augmentalis.voiceoscore.learnapp.database.repository.LearnAppRepository
import com.augmentalis.voiceoscore.learnapp.detection.ExpandableControlDetector
import com.augmentalis.voiceoscore.learnapp.detection.LauncherDetector
import com.augmentalis.voiceoscore.learnapp.elements.ElementClassifier
import com.augmentalis.voiceoscore.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.voiceoscore.learnapp.models.ExplorationProgress
import com.augmentalis.voiceoscore.learnapp.models.ExplorationState
import com.augmentalis.voiceoscore.learnapp.models.ExplorationStats
import com.augmentalis.voiceoscore.learnapp.navigation.NavigationGraphBuilder
import com.augmentalis.voiceoscore.learnapp.scrolling.ScrollDetector
import com.augmentalis.voiceoscore.learnapp.scrolling.ScrollExecutor
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import com.augmentalis.voiceoscore.learnapp.tracking.ElementClickTracker
import com.augmentalis.voiceoscore.learnapp.ui.ChecklistManager
import com.augmentalis.voiceoscore.learnapp.window.WindowManager
import com.augmentalis.voiceoscore.learnapp.core.LearnAppCore
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Exploration Engine (SOLID Refactored)
 *
 * Main engine orchestrating DFS exploration of entire app.
 * This class coordinates all extracted components and manages
 * the exploration lifecycle.
 *
 * ## Architecture (SOLID Principles)
 *
 * - **Single Responsibility**: Each extracted class has one job
 * - **Open/Closed**: New strategies via interfaces
 * - **Liskov Substitution**: Components are interchangeable
 * - **Interface Segregation**: Focused interfaces
 * - **Dependency Inversion**: Depends on abstractions
 *
 * ## Components
 *
 * | Component | Responsibility |
 * |-----------|---------------|
 * | DFSExplorer | DFS algorithm, stack, backtracking |
 * | ElementClicker | Click operations, retry, fallback |
 * | ElementRegistrar | UUID, aliases, voice commands |
 * | ExplorationNotifier | Notifications, sound |
 * | ExplorationMetrics | Metrics, overlay |
 * | DangerDetector | Dangerous element detection |
 *
 * ## Usage Example
 *
 * ```kotlin
 * val engine = ExplorationEngineRefactored(...)
 *
 * engine.startExploration("com.instagram.android")
 *
 * engine.explorationState.collect { state ->
 *     when (state) {
 *         is ExplorationState.Running -> println("Progress: ${state.progress}")
 *         is ExplorationState.Completed -> println("Done! ${state.stats}")
 *     }
 * }
 *
 * engine.stopExploration()
 * ```
 *
 * @property context Android context
 * @property accessibilityService Accessibility service for UI actions
 * @property uuidCreator UUID creator
 * @property thirdPartyGenerator Third-party UUID generator
 * @property aliasManager Alias manager
 * @property repository Database repository
 * @property databaseManager Database manager
 * @property strategy Exploration strategy (default: DFS)
 * @property learnAppCore Optional LearnAppCore for voice commands
 */
class ExplorationEngineRefactored(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val uuidCreator: UUIDCreator,
    private val thirdPartyGenerator: ThirdPartyUuidGenerator,
    private val aliasManager: UuidAliasManager,
    private val repository: LearnAppRepository,
    private val databaseManager: VoiceOSDatabaseManager,
    private val strategy: ExplorationStrategy = DFSExplorationStrategy(),
    private val learnAppCore: LearnAppCore? = null
) {
    // ═══════════════════════════════════════════════════════════════════
    // SETTINGS & CONFIGURATION
    // ═══════════════════════════════════════════════════════════════════

    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    // ═══════════════════════════════════════════════════════════════════
    // CORE COMPONENTS (Extracted via SOLID)
    // ═══════════════════════════════════════════════════════════════════

    private val screenStateManager = ScreenStateManager()
    private val elementClassifier = ElementClassifier(context)
    private val scrollDetector = ScrollDetector()
    private val scrollExecutor = ScrollExecutor(context)
    private val screenExplorer = ScreenExplorer(context, screenStateManager, elementClassifier, scrollDetector, scrollExecutor)
    private val launcherDetector = LauncherDetector(context)
    private val windowManager = WindowManager(accessibilityService)
    private val clickTracker = ElementClickTracker()
    private val expandableDetector = ExpandableControlDetector(context)
    private val checklistManager = ChecklistManager(context)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Extracted SOLID components
    private val notifier = ExplorationNotifier(context, scope)
    private val metrics = ExplorationMetrics(context)
    private val dangerDetector = DangerDetector()
    private val clicker = ElementClicker(context, accessibilityService)
    private val registrar = ElementRegistrar(
        context, uuidCreator, thirdPartyGenerator, aliasManager,
        learnAppCore, notifier, metrics
    )
    private val dfsExplorer = DFSExplorer(
        context, accessibilityService, screenStateManager, screenExplorer,
        clickTracker, checklistManager, clicker, registrar, dangerDetector, metrics
    )

    // ═══════════════════════════════════════════════════════════════════
    // NAVIGATION & TRACKING
    // ═══════════════════════════════════════════════════════════════════

    private lateinit var navigationGraphBuilder: NavigationGraphBuilder

    // ═══════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════

    private val _explorationState = MutableStateFlow<ExplorationState>(ExplorationState.Idle)
    val explorationState: StateFlow<ExplorationState> = _explorationState.asStateFlow()

    private val _pauseState = MutableStateFlow(ExplorationPauseState.RUNNING)
    val pauseState: StateFlow<ExplorationPauseState> = _pauseState.asStateFlow()

    private var explorationJob: Job? = null
    private var startTimestamp: Long = 0L
    private var dangerousElementsSkipped = 0
    private var loginScreensDetected = 0
    private var scrollableContainersFound = 0
    private var currentSessionId: String? = null
    private var terminationReason: TerminationReason? = null
    private var debugCallback: ExplorationDebugCallback? = null

    // ═══════════════════════════════════════════════════════════════════
    // PAUSE STATE
    // ═══════════════════════════════════════════════════════════════════

    enum class ExplorationPauseState {
        RUNNING,
        PAUSED_BY_USER,
        PAUSED_AUTO
    }

    enum class TerminationReason {
        COMPLETED,
        TIMEOUT,
        RECOVERY_FAILED,
        CONSECUTIVE_FAILURES,
        STACK_EXHAUSTED,
        MAX_DURATION,
        USER_STOPPED
    }

    // ═══════════════════════════════════════════════════════════════════
    // PUBLIC API
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Set the debug callback for exploration events
     */
    fun setDebugCallback(callback: ExplorationDebugCallback?) {
        debugCallback = callback
        dfsExplorer.setDebugCallback(callback)
        android.util.Log.d(TAG, "Debug callback ${if (callback != null) "enabled" else "disabled"}")
    }

    /**
     * Start exploration of an app
     *
     * @param packageName Package name to explore
     * @param sessionId Optional session ID for database persistence
     */
    fun startExploration(packageName: String, sessionId: String? = null) {
        this.currentSessionId = sessionId

        // Cancel any existing exploration
        explorationJob?.cancel()

        explorationJob = scope.launch {
            try {
                // Initialize
                cleanup()
                initializeExploration(packageName)

                // Detect launchers
                android.util.Log.i(TAG, "Detecting launchers on device...")
                val launchers = launcherDetector.detectLauncherPackages()
                android.util.Log.i(TAG, "Detected ${launchers.size} launcher(s): $launchers")

                // Get windows
                val windows = windowManager.getAppWindowsWithRetry(packageName, launcherDetector)
                if (windows.isEmpty()) {
                    handleNoWindowsError(packageName)
                    return@launch
                }

                android.util.Log.i(TAG, "Found ${windows.size} window(s) for package: $packageName")

                // Update state to Running
                _explorationState.value = ExplorationState.Running(
                    packageName = packageName,
                    progress = createInitialProgress(packageName)
                )

                // Get root node and initialize DFS
                val rootNode = accessibilityService.rootInActiveWindow
                if (rootNode == null) {
                    android.util.Log.e(TAG, "Root node null at start")
                    return@launch
                }

                val dfsState = dfsExplorer.initializeState(rootNode, packageName)
                if (dfsState == null) {
                    android.util.Log.e(TAG, "Failed to initialize DFS state")
                    return@launch
                }

                // Run DFS exploration loop
                runExplorationLoop(packageName, dfsState)

                // Exploration completed
                handleExplorationComplete(packageName)

            } catch (e: Exception) {
                android.util.Log.e(TAG, "Exploration failed for $packageName: ${e.message}", e)
                handleExplorationError(packageName, e)
            }
        }
    }

    /**
     * Pause exploration
     */
    suspend fun pause(reason: String = "User paused") {
        android.util.Log.i(TAG, "Pausing exploration: $reason")

        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            _pauseState.value = if (reason.contains("User", ignoreCase = true)) {
                ExplorationPauseState.PAUSED_BY_USER
            } else {
                ExplorationPauseState.PAUSED_AUTO
            }

            _explorationState.value = ExplorationState.Paused(
                packageName = currentState.packageName,
                progress = currentState.progress,
                reason = reason
            )
        }
    }

    /**
     * Resume exploration
     */
    suspend fun resume() {
        android.util.Log.i(TAG, "Resuming exploration")

        val currentState = _explorationState.value
        if (currentState is ExplorationState.Paused) {
            _pauseState.value = ExplorationPauseState.RUNNING
            _explorationState.value = ExplorationState.Running(
                packageName = currentState.packageName,
                progress = currentState.progress
            )
        }
    }

    /**
     * Stop exploration
     */
    fun stopExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            explorationJob?.cancel()
            explorationJob = null

            scope.coroutineContext[Job]?.cancelChildren()

            android.util.Log.i(TAG, "Stopping exploration - canceling all coroutines")

            scope.launch {
                try {
                    val stats = createExplorationStats(currentState.packageName)
                    _explorationState.value = ExplorationState.Completed(
                        packageName = currentState.packageName,
                        stats = stats
                    )

                    // Generate and save metrics
                    metrics.endSession()

                    cleanup()
                    android.util.Log.i(TAG, "Exploration stopped successfully. Stats: $stats")
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error during exploration stop", e)
                }
            }
        }
    }

    fun pauseExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            _explorationState.value = ExplorationState.PausedByUser(
                packageName = currentState.packageName,
                progress = currentState.progress
            )
        }
    }

    fun resumeExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.PausedByUser) {
            _explorationState.value = ExplorationState.Running(
                packageName = currentState.packageName,
                progress = currentState.progress
            )
        }
    }

    fun isPaused(): Boolean = _pauseState.value != ExplorationPauseState.RUNNING

    // ═══════════════════════════════════════════════════════════════════
    // EXPLORATION LOOP
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun runExplorationLoop(packageName: String, dfsState: DFSState) {
        val startTime = System.currentTimeMillis()
        val maxDuration = developerSettings.getExplorationTimeoutMs()
        val maxDepth = developerSettings.getMaxExplorationDepth()

        android.util.Log.i(TAG, "Starting ITERATIVE DFS exploration of $packageName")

        while (dfsState.explorationStack.isNotEmpty()) {
            // Check pause state
            if (!handlePauseState()) {
                terminationReason = TerminationReason.TIMEOUT
                break
            }

            // Check timeout
            if (System.currentTimeMillis() - startTime > maxDuration) {
                terminationReason = TerminationReason.MAX_DURATION
                android.util.Log.w(TAG, "Exploration timeout reached")
                break
            }

            val currentFrame = dfsState.explorationStack.peek()

            // Progress logging
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d(TAG,
                    "Stack depth: ${dfsState.explorationStack.size}, " +
                    "Screen: ${currentFrame.screenHash.take(8)}..., " +
                    "Elements: ${currentFrame.currentElementIndex}/${currentFrame.elements.size}")
            }

            // Explore current screen with fresh-scrape
            val clickedOnScreen = dfsExplorer.exploreScreenWithFreshScrape(
                currentFrame, packageName, dfsState
            )

            // Check for navigation
            val postExploreRoot = accessibilityService.rootInActiveWindow
            if (postExploreRoot != null) {
                val postExplorePackage = postExploreRoot.packageName?.toString()

                // Handle external app navigation
                if (postExplorePackage != packageName) {
                    val recovered = handleExternalAppNavigation(packageName, dfsState)
                    if (!recovered) continue
                }

                // Check for new screen
                val postExploreState = screenStateManager.captureScreenState(
                    postExploreRoot, packageName, currentFrame.depth + 1
                )

                if (postExploreState.hash != currentFrame.screenHash) {
                    val pushed = dfsExplorer.handleNewScreen(
                        dfsState, currentFrame, packageName, postExploreState.hash
                    )
                    if (pushed) continue
                }
            }

            // Current screen fully explored - register and pop
            registerScreenElements(currentFrame, packageName, dfsState)
            dfsExplorer.popAndNavigateBack(dfsState)
        }

        // Set termination reason if not already set
        if (terminationReason == null) {
            terminationReason = TerminationReason.STACK_EXHAUSTED
        }

        android.util.Log.i(TAG, "DFS complete. Explored ${dfsState.visitedScreens.size} screens")
        android.util.Log.i(TAG, "TERMINATION_REASON: $terminationReason")
        android.util.Log.i(TAG, dfsExplorer.getExplorationStats())

        // Export checklist
        exportChecklist(packageName)
    }

    private suspend fun handlePauseState(): Boolean {
        if (_pauseState.value == ExplorationPauseState.RUNNING) return true

        val pauseState = _pauseState.value
        android.util.Log.i(TAG, "Exploration paused - waiting for resume (state: $pauseState)")

        val timeout = if (pauseState == ExplorationPauseState.PAUSED_AUTO) {
            600_000L  // 10 minutes for auto-pause
        } else {
            Long.MAX_VALUE  // Infinite for manual pause
        }

        val resumed = withTimeoutOrNull(timeout) {
            _pauseState.first { it == ExplorationPauseState.RUNNING }
            true
        } ?: false

        if (!resumed) {
            android.util.Log.w(TAG, "Pause timeout reached - terminating exploration")
            return false
        }

        android.util.Log.i(TAG, "Exploration resumed - continuing DFS loop")
        return true
    }

    private suspend fun handleExternalAppNavigation(
        packageName: String,
        dfsState: DFSState
    ): Boolean {
        android.util.Log.w(TAG, "Navigated to external app")

        val result = recoverToTargetApp(packageName)

        return when (result) {
            RecoveryResult.FAILED -> {
                android.util.Log.e(TAG, "Failed to recover to $packageName")
                dfsState.explorationStack.pop()
                if (dfsState.explorationStack.isNotEmpty()) {
                    clicker.pressBack()
                    delay(developerSettings.getScreenProcessingDelayMs())
                }
                false
            }
            RecoveryResult.RECOVERED_VIA_INTENT -> {
                android.util.Log.w(TAG, "Intent relaunch - clearing stale DFS stack")
                handleIntentRecovery(packageName, dfsState)
                true
            }
            RecoveryResult.RECOVERED_VIA_BACK -> {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d(TAG, "Recovered via BACK - DFS stack preserved")
                }
                true
            }
        }
    }

    private suspend fun handleIntentRecovery(packageName: String, dfsState: DFSState) {
        dfsState.explorationStack.clear()
        clickTracker.clear()

        val freshRoot = accessibilityService.rootInActiveWindow ?: return
        val freshState = screenStateManager.captureScreenState(freshRoot, packageName, 0)
        val freshExploration = screenExplorer.exploreScreen(freshRoot, packageName, 0)

        if (freshExploration is ScreenExplorationResult.Success &&
            freshState.hash !in dfsState.visitedScreens) {

            dfsState.visitedScreens.add(freshState.hash)
            val freshElements = registrar.preGenerateUuids(
                freshExploration.safeClickableElements, packageName
            )

            val freshClickableUuids = freshElements
                .filter { !dangerDetector.isCriticalDangerous(it) }
                .mapNotNull { it.uuid }
            clickTracker.registerScreen(freshState.hash, freshClickableUuids)
            dfsExplorer.cumulativeTracking.discoveredVuids.addAll(freshClickableUuids)

            val freshFrame = ExplorationFrame(
                screenHash = freshState.hash,
                screenState = freshState,
                elements = freshElements.toMutableList(),
                currentElementIndex = 0,
                depth = 0,
                parentScreenHash = null
            )

            dfsState.explorationStack.push(freshFrame)
            android.util.Log.i(TAG, "Restarted exploration from app entry point")
        }
    }

    private suspend fun registerScreenElements(
        frame: ExplorationFrame,
        packageName: String,
        dfsState: DFSState
    ) {
        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d(TAG,
                "Registering ${frame.elements.size} elements for screen ${frame.screenHash.take(8)}...")
        }

        val elementUuids = registrar.registerElements(
            frame.elements, packageName, dfsState.registeredElementUuids
        )

        navigationGraphBuilder.addScreen(
            screenState = frame.screenState,
            elementUuids = elementUuids
        )

        kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                repository.saveScreenState(frame.screenState)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Failed to persist screen state: ${frame.screenHash}", e)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // RECOVERY
    // ═══════════════════════════════════════════════════════════════════

    private enum class RecoveryResult {
        RECOVERED_VIA_BACK,
        RECOVERED_VIA_INTENT,
        FAILED
    }

    private suspend fun recoverToTargetApp(packageName: String): RecoveryResult {
        clicker.dismissKeyboard()
        delay(developerSettings.getClickDelayMs())

        // Try BACK presses
        repeat(5) { attempt ->
            clicker.pressBack()
            delay(developerSettings.getClickDelayMs())

            val currentPackage = accessibilityService.rootInActiveWindow?.packageName?.toString()
            if (currentPackage == packageName) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d(TAG, "Recovered to $packageName after ${attempt + 1} BACK attempts")
                }
                return RecoveryResult.RECOVERED_VIA_BACK
            }
        }

        // Fallback: relaunch via intent
        android.util.Log.w(TAG, "BACK presses failed, attempting to relaunch $packageName via intent")

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(launchIntent)
                delay(developerSettings.getScreenProcessingDelayMs() * 2)

                val currentPackage = accessibilityService.rootInActiveWindow?.packageName?.toString()
                if (currentPackage == packageName) {
                    android.util.Log.i(TAG, "Recovered to $packageName via intent relaunch")
                    RecoveryResult.RECOVERED_VIA_INTENT
                } else {
                    android.util.Log.e(TAG, "Intent relaunch failed, current package: $currentPackage")
                    RecoveryResult.FAILED
                }
            } else {
                android.util.Log.e(TAG, "No launch intent found for $packageName")
                RecoveryResult.FAILED
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to relaunch $packageName: ${e.message}", e)
            RecoveryResult.FAILED
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // INITIALIZATION & CLEANUP
    // ═══════════════════════════════════════════════════════════════════

    private fun initializeExploration(packageName: String) {
        navigationGraphBuilder = NavigationGraphBuilder(packageName)
        screenStateManager.clear()
        clickTracker.clear()
        registrar.clearCounters()
        terminationReason = null
        startTimestamp = System.currentTimeMillis()
        dangerousElementsSkipped = 0
        loginScreensDetected = 0
        scrollableContainersFound = 0

        metrics.startSession(packageName)

        android.util.Log.i(TAG, "Starting exploration of: $packageName")

        if (isSystemApp(packageName)) {
            android.util.Log.w(TAG,
                "System app detected: $packageName. Limited support (read-only).")
        }
    }

    private suspend fun cleanup() {
        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d(TAG, "Cleaning up resources...")
        }

        metrics.hideOverlay()
        screenStateManager.clear()
        clickTracker.clear()
        registrar.clearCounters()

        android.util.Log.i(TAG, "Resource cleanup complete")
    }

    // ═══════════════════════════════════════════════════════════════════
    // COMPLETION & STATS
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun handleExplorationComplete(packageName: String) {
        val completeness = dfsExplorer.cumulativeTracking.getCompleteness()

        android.util.Log.i(TAG, "Exploration Statistics (CUMULATIVE):")
        android.util.Log.i(TAG, dfsExplorer.getExplorationStats())

        if (completeness >= developerSettings.getCompletenessThresholdPercent()) {
            android.util.Log.i(TAG, "App fully learned (${completeness.toInt()}%)!")
            repository.markAppAsFullyLearned(packageName, System.currentTimeMillis())
        } else {
            android.util.Log.w(TAG, "App partially learned (${completeness.toInt()}%)")
        }

        val stats = createExplorationStats(packageName)
        _explorationState.value = ExplorationState.Completed(
            packageName = packageName,
            stats = stats
        )

        metrics.endSession()
    }

    private fun handleExplorationError(packageName: String, error: Exception) {
        val currentState = _explorationState.value
        val partialProgress = if (currentState is ExplorationState.Running) {
            currentState.progress
        } else null

        _explorationState.value = ExplorationState.Failed(
            packageName = packageName,
            error = error,
            partialProgress = partialProgress
        )
    }

    private fun handleNoWindowsError(packageName: String) {
        android.util.Log.e(TAG, "No windows found for package: $packageName after retry")
        _explorationState.value = ExplorationState.Failed(
            packageName = packageName,
            error = IllegalStateException("No windows found for package: $packageName"),
            partialProgress = null
        )
    }

    private suspend fun createExplorationStats(packageName: String): ExplorationStats {
        val stats = screenStateManager.getStats()
        val graph = if (::navigationGraphBuilder.isInitialized) {
            navigationGraphBuilder.build()
        } else {
            NavigationGraphBuilder(packageName).build()
        }
        val graphStats = graph.getStats()
        val elapsed = System.currentTimeMillis() - startTimestamp
        val completeness = dfsExplorer.cumulativeTracking.getCompleteness()

        // Generate AI context
        try {
            val aiSerializer = com.augmentalis.voiceoscore.learnapp.ai.AIContextSerializer(context, databaseManager)
            val aiContext = aiSerializer.generateContext(graph)
            val vosFile = aiSerializer.saveToFile(aiContext)
            if (vosFile != null) {
                android.util.Log.i(TAG, "AI context saved to: ${vosFile.absolutePath}")
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to generate AI context", e)
        }

        return ExplorationStats(
            packageName = packageName,
            appName = packageName,
            totalScreens = stats.totalScreensDiscovered,
            totalElements = graphStats.totalElements,
            totalEdges = graphStats.totalEdges,
            durationMs = elapsed,
            maxDepth = graphStats.maxDepth,
            dangerousElementsSkipped = dangerousElementsSkipped,
            loginScreensDetected = loginScreensDetected,
            scrollableContainersFound = scrollableContainersFound,
            completeness = completeness
        )
    }

    private fun createInitialProgress(packageName: String): ExplorationProgress {
        return ExplorationProgress(
            appName = packageName,
            screensExplored = 0,
            estimatedTotalScreens = developerSettings.getEstimatedInitialScreenCount(),
            elementsDiscovered = 0,
            currentDepth = 0,
            currentScreen = "Starting...",
            elapsedTimeMs = 0L
        )
    }

    private fun exportChecklist(packageName: String) {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        )
        val checklistPath = "${downloadsDir.absolutePath}/learnapp-checklist-" +
                           "${packageName.substringAfterLast('.')}-${System.currentTimeMillis()}.md"
        checklistManager.exportToFile(checklistPath)

        val overallProgress = checklistManager.getOverallProgress()
        android.util.Log.i(TAG, "Checklist exported to: $checklistPath (Progress: $overallProgress%)")
    }

    // ═══════════════════════════════════════════════════════════════════
    // UTILITIES
    // ═══════════════════════════════════════════════════════════════════

    private fun isSystemApp(packageName: String): Boolean {
        try {
            val systemPrefixes = listOf(
                "com.android.",
                "com.google.android.apps.messaging",
                "com.google.android.dialer",
                "com.google.android.contacts",
                "com.google.android.deskclock"
            )

            if (systemPrefixes.any { packageName.startsWith(it) }) {
                return true
            }

            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            return (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        } catch (e: Exception) {
            android.util.Log.w(TAG, "Failed to check if $packageName is system app: ${e.message}")
            return false
        }
    }

    companion object {
        private const val TAG = "ExplorationEngine"
    }
}
