/**
 * ExplorationEngine.kt - Main exploration engine for VoiceOSCoreNG
 *
 * SOLID-refactored orchestration engine that coordinates DFS exploration
 * of Android apps. Implements IExplorationEngine interface for KMP compatibility.
 *
 * Components:
 * - DFSExplorer: DFS algorithm, stack management, loop prevention
 * - ElementClicker: Click operations, retry logic, gesture fallback
 * - ElementRegistrar: UUID generation, alias management
 * - ExplorationNotifier: Notifications, sound feedback
 * - ExplorationMetrics: VUID metrics, progress tracking
 * - DangerDetector: Dangerous element detection
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 */

package com.augmentalis.voiceoscoreng.exploration

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.functions.IScreenFingerprinter
import com.augmentalis.voiceoscoreng.functions.ScreenFingerprinter
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
 * Main exploration engine implementing IExplorationEngine.
 *
 * This engine coordinates all extracted SOLID components and manages
 * the exploration lifecycle (start, pause, resume, stop).
 *
 * ## Architecture (SOLID Principles)
 *
 * - **Single Responsibility**: Each component has one job
 * - **Open/Closed**: New strategies via interfaces
 * - **Interface Segregation**: Focused interfaces
 * - **Dependency Inversion**: Depends on abstractions
 *
 * ## Usage Example
 *
 * ```kotlin
 * val engine = ExplorationEngine(context, accessibilityService)
 * engine.startExploration("com.instagram.android")
 *
 * engine.state.collect { state ->
 *     when (state) {
 *         is ExplorationEngineState.Running -> println("Progress: ${state.progress}")
 *         is ExplorationEngineState.Completed -> println("Done! ${state.stats}")
 *     }
 * }
 *
 * engine.stopExploration()
 * ```
 *
 * @property context Android context
 * @property accessibilityService Accessibility service for UI actions
 * @property explorationConfig Configuration for exploration
 */
class ExplorationEngine(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    override val config: ExplorationConfig = ExplorationConfig.DEFAULT
) : IExplorationEngine {

    // ═══════════════════════════════════════════════════════════════════
    // STATE MANAGEMENT
    // ═══════════════════════════════════════════════════════════════════

    private val _state = MutableStateFlow<ExplorationEngineState>(ExplorationEngineState.Idle)
    override val state: StateFlow<ExplorationEngineState> = _state.asStateFlow()

    private val _pauseState = MutableStateFlow(PauseState.RUNNING)

    override val tracking = CumulativeTracking()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var explorationJob: Job? = null
    private var startTimestamp: Long = 0L
    private var debugCallback: ExplorationDebugCallback? = null
    private var terminationReason: TerminationReason? = null

    // Statistics
    private var dangerousElementsSkipped = 0
    private var loginScreensDetected = 0
    private var scrollableContainersFound = 0

    // ═══════════════════════════════════════════════════════════════════
    // CORE COMPONENTS (Extracted via SOLID)
    // ═══════════════════════════════════════════════════════════════════

    private val fingerprinter: IScreenFingerprinter = ScreenFingerprinter()
    private val dangerDetector = DangerDetector()
    private val clicker = ElementClicker(accessibilityService, config)
    private val registrar = ElementRegistrar(context, config)
    private val notifier = ExplorationNotifier(context, scope, config)
    private val metrics = ExplorationMetrics(context, config)

    private val dfsExplorer = DFSExplorer(
        accessibilityService = accessibilityService,
        clicker = clicker,
        registrar = registrar,
        fingerprinter = fingerprinter,
        dangerDetector = dangerDetector,
        metrics = metrics,
        config = config
    )

    // ═══════════════════════════════════════════════════════════════════
    // PAUSE STATE
    // ═══════════════════════════════════════════════════════════════════

    private enum class PauseState {
        RUNNING,
        PAUSED_BY_USER,
        PAUSED_AUTO
    }

    private enum class TerminationReason {
        COMPLETED,
        TIMEOUT,
        RECOVERY_FAILED,
        CONSECUTIVE_FAILURES,
        STACK_EXHAUSTED,
        MAX_DURATION,
        USER_STOPPED
    }

    // ═══════════════════════════════════════════════════════════════════
    // PUBLIC API (IExplorationEngine)
    // ═══════════════════════════════════════════════════════════════════

    override fun setDebugCallback(callback: ExplorationDebugCallback?) {
        debugCallback = callback
        dfsExplorer.setDebugCallback(callback)
        Log.d(TAG, "Debug callback ${if (callback != null) "enabled" else "disabled"}")
    }

    override fun startExploration(packageName: String, sessionId: String?) {
        // Cancel any existing exploration
        explorationJob?.cancel()

        explorationJob = scope.launch {
            try {
                // Initialize
                cleanup()
                initializeExploration(packageName)

                // Update state to Initializing
                _state.value = ExplorationEngineState.Initializing(packageName)

                // Get root node
                val rootNode = clicker.getRootNode()
                if (rootNode == null) {
                    Log.e(TAG, "Root node null at start")
                    handleExplorationError(packageName, IllegalStateException("No root node"))
                    return@launch
                }

                // Check package
                val currentPackage = rootNode.packageName?.toString()
                if (currentPackage != packageName) {
                    Log.w(TAG, "Target app not in foreground: expected $packageName, got $currentPackage")
                }

                // Initialize DFS state
                val dfsState = dfsExplorer.initializeState(rootNode, packageName)
                if (dfsState == null) {
                    Log.e(TAG, "Failed to initialize DFS state")
                    handleExplorationError(packageName, IllegalStateException("DFS init failed"))
                    return@launch
                }

                // Update state to Running
                _state.value = ExplorationEngineState.Running(
                    packageName = packageName,
                    progress = createInitialProgress(packageName)
                )

                // Run DFS exploration loop
                runExplorationLoop(packageName, dfsState)

                // Exploration completed
                handleExplorationComplete(packageName)

            } catch (e: Exception) {
                Log.e(TAG, "Exploration failed for $packageName: ${e.message}", e)
                handleExplorationError(packageName, e)
            }
        }
    }

    override fun stopExploration() {
        val currentState = _state.value
        if (currentState is ExplorationEngineState.Running ||
            currentState is ExplorationEngineState.Paused) {

            terminationReason = TerminationReason.USER_STOPPED
            explorationJob?.cancel()
            explorationJob = null
            scope.coroutineContext[Job]?.cancelChildren()

            Log.i(TAG, "Stopping exploration - canceling all coroutines")

            scope.launch {
                try {
                    val packageName = when (currentState) {
                        is ExplorationEngineState.Running -> currentState.packageName
                        is ExplorationEngineState.Paused -> currentState.packageName
                        else -> "unknown"
                    }

                    val stats = createExplorationStats(packageName)
                    _state.value = ExplorationEngineState.Completed(
                        packageName = packageName,
                        stats = stats
                    )

                    notifier.notifyComplete(packageName, stats)
                    metrics.endSession()
                    cleanup()

                    Log.i(TAG, "Exploration stopped successfully. Stats: $stats")
                } catch (e: Exception) {
                    Log.e(TAG, "Error during exploration stop", e)
                }
            }
        }
    }

    override suspend fun pause(reason: String) {
        Log.i(TAG, "Pausing exploration: $reason")

        val currentState = _state.value
        if (currentState is ExplorationEngineState.Running) {
            _pauseState.value = if (reason.contains("User", ignoreCase = true)) {
                PauseState.PAUSED_BY_USER
            } else {
                PauseState.PAUSED_AUTO
            }

            _state.value = ExplorationEngineState.Paused(
                packageName = currentState.packageName,
                progress = currentState.progress,
                reason = reason
            )
        }
    }

    override suspend fun resume() {
        Log.i(TAG, "Resuming exploration")

        val currentState = _state.value
        if (currentState is ExplorationEngineState.Paused) {
            _pauseState.value = PauseState.RUNNING
            _state.value = ExplorationEngineState.Running(
                packageName = currentState.packageName,
                progress = currentState.progress
            )
        }
    }

    override fun isPaused(): Boolean = _pauseState.value != PauseState.RUNNING

    override fun isRunning(): Boolean {
        val currentState = _state.value
        return currentState is ExplorationEngineState.Running ||
               currentState is ExplorationEngineState.Paused
    }

    override fun getStats(): String = dfsExplorer.getStats()

    // ═══════════════════════════════════════════════════════════════════
    // EXPLORATION LOOP
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun runExplorationLoop(packageName: String, dfsState: DFSState) {
        val startTime = System.currentTimeMillis()
        val maxDuration = config.explorationTimeoutMs

        Log.i(TAG, "Starting ITERATIVE DFS exploration of $packageName " +
                  "(max depth: ${config.maxExplorationDepth})")

        while (dfsState.isNotEmpty()) {
            // Check pause state
            if (!handlePauseState()) {
                terminationReason = TerminationReason.TIMEOUT
                break
            }

            // Check timeout
            if (System.currentTimeMillis() - startTime > maxDuration) {
                terminationReason = TerminationReason.MAX_DURATION
                Log.w(TAG, "Exploration timeout reached")
                break
            }

            val currentFrame = dfsState.peek() ?: break

            // Progress logging
            if (config.verboseLogging) {
                Log.d(TAG,
                    "Stack depth: ${dfsState.stackDepth()}, " +
                    "Screen: ${currentFrame.screenHash.take(8)}..., " +
                    "Elements: ${currentFrame.currentElementIndex}/${currentFrame.elements.size}")
            }

            // Update progress
            updateProgress(packageName, dfsState, currentFrame)

            // Explore current screen
            val result = dfsExplorer.exploreScreen(currentFrame, packageName, dfsState)

            when (result) {
                is ExploreScreenResult.Navigated -> {
                    // Handle new screen
                    val pushed = dfsExplorer.handleNewScreen(
                        dfsState, currentFrame, packageName, result.newScreenHash
                    )
                    if (!pushed) {
                        // Could not push (depth limit or loop), continue exploring current
                    }
                }

                is ExploreScreenResult.ExternalApp -> {
                    // Handle external app navigation
                    val recovered = handleExternalAppNavigation(packageName, dfsState)
                    if (!recovered) {
                        terminationReason = TerminationReason.RECOVERY_FAILED
                        break
                    }
                }

                is ExploreScreenResult.Stop -> {
                    // Stop exploration due to error or termination
                    Log.w(TAG, "Exploration stopped: ${result.reason}")
                    terminationReason = TerminationReason.USER_STOPPED
                    break
                }

                is ExploreScreenResult.ScreenComplete,
                is ExploreScreenResult.Continue,
                is ExploreScreenResult.Timeout -> {
                    // Current screen fully explored - pop and go back
                    dfsExplorer.popAndNavigateBack(dfsState)
                }
            }
        }

        // Set termination reason if not already set
        if (terminationReason == null) {
            terminationReason = TerminationReason.STACK_EXHAUSTED
        }

        Log.i(TAG, "DFS complete. Explored ${dfsState.visitedScreens.size} screens")
        Log.i(TAG, "TERMINATION_REASON: $terminationReason")
        Log.i(TAG, dfsExplorer.getStats())
    }

    private suspend fun handlePauseState(): Boolean {
        if (_pauseState.value == PauseState.RUNNING) return true

        val pauseState = _pauseState.value
        Log.i(TAG, "Exploration paused - waiting for resume (state: $pauseState)")

        val timeout = if (pauseState == PauseState.PAUSED_AUTO) {
            600_000L  // 10 minutes for auto-pause
        } else {
            Long.MAX_VALUE  // Infinite for manual pause
        }

        val resumed = withTimeoutOrNull(timeout) {
            _pauseState.first { it == PauseState.RUNNING }
            true
        } ?: false

        if (!resumed) {
            Log.w(TAG, "Pause timeout reached - terminating exploration")
            return false
        }

        Log.i(TAG, "Exploration resumed - continuing DFS loop")
        return true
    }

    private suspend fun handleExternalAppNavigation(
        packageName: String,
        dfsState: DFSState
    ): Boolean {
        Log.w(TAG, "Navigated to external app")

        // Try BACK presses to return to target app
        repeat(config.maxBackPressAttempts) { attempt ->
            clicker.pressBack()
            delay(config.clickDelayMs)

            val currentPackage = clicker.getRootNode()?.packageName?.toString()
            if (currentPackage == packageName) {
                if (config.verboseLogging) {
                    Log.d(TAG, "Recovered to $packageName after ${attempt + 1} BACK attempts")
                }
                return true
            }
        }

        // Fallback: try to relaunch via intent
        Log.w(TAG, "BACK presses failed, attempting to relaunch $packageName via intent")

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(launchIntent)
                delay(config.screenProcessingDelayMs * 2)

                val currentPackage = clicker.getRootNode()?.packageName?.toString()
                if (currentPackage == packageName) {
                    Log.i(TAG, "Recovered to $packageName via intent relaunch")

                    // Clear stale state after intent relaunch
                    dfsState.clear()
                    val freshRoot = clicker.getRootNode()
                    if (freshRoot != null) {
                        dfsExplorer.initializeState(freshRoot, packageName)?.let { newState ->
                            // Transfer new state
                            dfsState.visitedScreens.addAll(newState.visitedScreens)
                        }
                    }
                    true
                } else {
                    Log.e(TAG, "Intent relaunch failed, current package: $currentPackage")
                    false
                }
            } else {
                Log.e(TAG, "No launch intent found for $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to relaunch $packageName: ${e.message}", e)
            false
        }
    }

    private fun updateProgress(packageName: String, dfsState: DFSState, frame: ExplorationFrame) {
        val currentState = _state.value
        if (currentState is ExplorationEngineState.Running) {
            val trackingStats = tracking.getStats()

            val progress = ExplorationProgress(
                appName = packageName,
                screensExplored = dfsState.visitedScreens.size,
                estimatedTotalScreens = maxOf(10, dfsState.visitedScreens.size + 5),
                elementsDiscovered = trackingStats.discovered,
                elementsClicked = trackingStats.clicked,
                currentDepth = frame.depth,
                currentScreen = frame.screenHash.take(8) + "...",
                elapsedTimeMs = System.currentTimeMillis() - startTimestamp
            )

            _state.value = ExplorationEngineState.Running(
                packageName = packageName,
                progress = progress
            )

            // Notify progress
            notifier.notifyProgress(packageName, progress.progressPercent.toInt(), dfsState.visitedScreens.size)
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // INITIALIZATION & CLEANUP
    // ═══════════════════════════════════════════════════════════════════

    private fun initializeExploration(packageName: String) {
        tracking.clear()
        terminationReason = null
        startTimestamp = System.currentTimeMillis()
        dangerousElementsSkipped = 0
        loginScreensDetected = 0
        scrollableContainersFound = 0

        metrics.startSession(packageName)
        registrar.clearCounters()

        Log.i(TAG, "Starting exploration of: $packageName")
    }

    private suspend fun cleanup() {
        if (config.verboseLogging) {
            Log.d(TAG, "Cleaning up resources...")
        }

        metrics.hideOverlay()
        registrar.clearCounters()
        notifier.cancelAllNotifications()

        Log.i(TAG, "Resource cleanup complete")
    }

    // ═══════════════════════════════════════════════════════════════════
    // COMPLETION & STATS
    // ═══════════════════════════════════════════════════════════════════

    private suspend fun handleExplorationComplete(packageName: String) {
        val trackingStats = tracking.getStats()
        val completeness = trackingStats.completeness

        Log.i(TAG, "Exploration Statistics (CUMULATIVE):")
        Log.i(TAG, dfsExplorer.getStats())

        val stats = createExplorationStats(packageName)
        _state.value = ExplorationEngineState.Completed(
            packageName = packageName,
            stats = stats
        )

        notifier.notifyComplete(packageName, stats)
        metrics.endSession()
    }

    private fun handleExplorationError(packageName: String, error: Throwable) {
        val currentState = _state.value
        val partialProgress = when (currentState) {
            is ExplorationEngineState.Running -> currentState.progress
            is ExplorationEngineState.Paused -> currentState.progress
            else -> null
        }

        _state.value = ExplorationEngineState.Failed(
            packageName = packageName,
            error = error,
            partialProgress = partialProgress
        )

        notifier.notifyError(packageName, error)
    }

    private fun createExplorationStats(packageName: String): ExplorationStats {
        val elapsed = System.currentTimeMillis() - startTimestamp
        val trackingStats = tracking.getStats()

        return ExplorationStats(
            packageName = packageName,
            appName = packageName,
            totalScreens = trackingStats.discovered / 10, // Approximate
            totalElements = trackingStats.discovered,
            totalEdges = trackingStats.clicked,
            durationMs = elapsed,
            maxDepth = config.maxExplorationDepth,
            dangerousElementsSkipped = dangerousElementsSkipped,
            loginScreensDetected = loginScreensDetected,
            scrollableContainersFound = scrollableContainersFound,
            completeness = trackingStats.completeness
        )
    }

    private fun createInitialProgress(packageName: String): ExplorationProgress {
        return ExplorationProgress(
            appName = packageName,
            screensExplored = 0,
            estimatedTotalScreens = 10,
            elementsDiscovered = 0,
            elementsClicked = 0,
            currentDepth = 0,
            currentScreen = "Starting...",
            elapsedTimeMs = 0L
        )
    }

    companion object {
        private const val TAG = "ExplorationEngine"

        /**
         * Factory method to create ExplorationEngine with default config.
         */
        fun create(
            context: Context,
            accessibilityService: AccessibilityService,
            config: ExplorationConfig = ExplorationConfig.DEFAULT
        ): ExplorationEngine {
            return ExplorationEngine(context, accessibilityService, config)
        }
    }
}
