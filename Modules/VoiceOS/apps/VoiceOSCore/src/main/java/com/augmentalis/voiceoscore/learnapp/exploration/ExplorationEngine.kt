/**
 * ExplorationEngine.kt - Main exploration engine (DFS orchestration)
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/exploration/ExplorationEngine.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Main engine orchestrating DFS exploration of entire app
 */

@file:Suppress("UNNECESSARY_SAFE_CALL")  // Element properties can be null at runtime

package com.augmentalis.voiceoscore.learnapp.exploration

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.media.AudioAttributes
import android.media.ToneGenerator
import android.media.AudioManager
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.app.NotificationCompat
import com.augmentalis.voiceoscore.learnapp.detection.ExpandableControlDetector
import com.augmentalis.voiceoscore.learnapp.elements.ElementClassifier
import com.augmentalis.voiceoscore.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.voiceoscore.learnapp.models.ExplorationProgress
import com.augmentalis.voiceoscore.learnapp.models.ExplorationState
import com.augmentalis.voiceoscore.learnapp.models.ExplorationStats
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import com.augmentalis.voiceoscore.learnapp.navigation.NavigationGraphBuilder
import com.augmentalis.voiceoscore.learnapp.tracking.ElementClickTracker
import com.augmentalis.voiceoscore.learnapp.detection.LauncherDetector
import com.augmentalis.voiceoscore.learnapp.ui.ChecklistManager
import com.augmentalis.voiceoscore.learnapp.window.WindowManager
import com.augmentalis.voiceoscore.learnapp.window.WindowInfo
import com.augmentalis.voiceoscore.learnapp.window.WindowType
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.models.UUIDAccessibility
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
// Phase 3 (2025-12-04): LearnAppCore integration for voice command generation
import com.augmentalis.voiceoscore.learnapp.core.LearnAppCore
import com.augmentalis.voiceoscore.learnapp.core.ProcessingMode
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
// Phase 3 (2025-12-08): VUIDMetrics integration for observability
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDCreationMetricsCollector
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDCreationDebugOverlay
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDMetricsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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
 * Debug callback interface for screen exploration events
 *
 * Provides real-time updates about screen exploration progress,
 * including element discovery, click tracking, and navigation.
 *
 * REWRITTEN (2025-12-08): Added onElementClicked and onElementBlocked for item tracking.
 *
 * @since 2025-12-08 (Debug Overlay Feature)
 */
interface ExplorationDebugCallback {
    /**
     * Called when a screen is explored and elements are discovered
     *
     * @param elements List of discovered elements on current screen
     * @param screenHash Unique hash of the current screen state
     * @param activityName Current activity name
     * @param packageName Target app package
     * @param parentScreenHash Hash of the screen we navigated from (null if root)
     */
    fun onScreenExplored(
        elements: List<com.augmentalis.voiceoscore.learnapp.models.ElementInfo>,
        screenHash: String,
        activityName: String,
        packageName: String,
        parentScreenHash: String?
    )

    /**
     * Called when an element click causes navigation to a new screen
     *
     * @param elementKey Identifier for the clicked element (screenHash:stableId)
     * @param destinationScreenHash Hash of the screen navigated to
     */
    fun onElementNavigated(elementKey: String, destinationScreenHash: String)

    /**
     * Called when exploration progress is updated
     *
     * @param progress Current progress percentage (0-100)
     */
    fun onProgressUpdated(progress: Int)

    /**
     * Called when an element is clicked (2025-12-08)
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where element was clicked
     * @param vuid VUID if assigned
     */
    fun onElementClicked(stableId: String, screenHash: String, vuid: String?) {}

    /**
     * Called when an element is blocked (critical dangerous) (2025-12-08)
     *
     * @param stableId Element stable ID
     * @param screenHash Screen where element was found
     * @param reason Blocking reason
     */
    fun onElementBlocked(stableId: String, screenHash: String, reason: String) {}
}

/**
 * Exploration Engine
 *
 * Main engine orchestrating DFS exploration of entire app.
 * Coordinates all components: screen exploration, element classification,
 * UUID generation, navigation graph building, etc.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val engine = ExplorationEngine(...)
 *
 * // Start exploration
 * engine.startExploration("com.instagram.android")
 *
 * // Observe state changes
 * engine.explorationState.collect { state ->
 *     when (state) {
 *         is ExplorationState.Running -> {
 *             println("Progress: ${state.progress}")
 *         }
 *         is ExplorationState.Completed -> {
 *             println("Done! ${state.stats}")
 *         }
 *     }
 * }
 *
 * // Pause/Resume
 * engine.pauseExploration()
 * engine.resumeExploration()
 *
 * // Stop
 * engine.stopExploration()
 * ```
 *
 * @property accessibilityService Accessibility service for UI actions
 * @property uuidCreator UUID creator
 * @property thirdPartyGenerator Third-party UUID generator
 * @property aliasManager Alias manager
 * @property strategy Exploration strategy (default: DFS)
 *
 * @since 1.0.0
 */
class ExplorationEngine(
    private val context: android.content.Context,
    private val accessibilityService: AccessibilityService,
    private val uuidCreator: UUIDCreator,
    private val thirdPartyGenerator: ThirdPartyUuidGenerator,
    private val aliasManager: UuidAliasManager,
    private val repository: com.augmentalis.voiceoscore.learnapp.database.repository.LearnAppRepository,
    private val databaseManager: com.augmentalis.database.VoiceOSDatabaseManager,
    private val strategy: ExplorationStrategy = DFSExplorationStrategy(),
    private val learnAppCore: LearnAppCore? = null  // Phase 3: LearnAppCore for voice command generation
) {

    /**
     * Developer settings for LearnApp tuning
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    /**
     * Screen state manager
     */
    private val screenStateManager = ScreenStateManager()

    /**
     * Element classifier
     */
    private val elementClassifier = ElementClassifier()

    /**
     * Screen explorer
     */
    private val screenExplorer = ScreenExplorer(elementClassifier)

    /**
     * Launcher detector - device-agnostic launcher detection (singleton, uses object methods)
     */
    private val launcherDetector = LauncherDetector

    /**
     * Window manager - multi-window detection system
     */
    private val windowManager = WindowManager(accessibilityService)

    /**
     * Element click tracker - per-element progress tracking
     * NOTE: This tracker may be cleared on intent relaunch for fresh-scrape logic
     */
    private val clickTracker = ElementClickTracker()

    /**
     * FIX (2025-12-08): Global cumulative tracking - NEVER cleared during exploration
     * These track ALL exploration progress regardless of intent relaunches
     * Used for final completion percentage calculation
     *
     * UPDATE (2025-12-08): Added blocked VUID tracking for separate stats display
     * Format: "XX% of Non-Blocked screens (XX/YYY), ##% of non-blocked entries (aa of zz clickable items)"
     */
    private val cumulativeDiscoveredVuids = mutableSetOf<String>()  // All discovered element VUIDs (clickable only)
    private val cumulativeClickedVuids = mutableSetOf<String>()      // All clicked element VUIDs
    private val cumulativeBlockedVuids = mutableSetOf<String>()      // All blocked (critical dangerous) VUIDs

    /**
     * Expandable control detector - identifies dropdowns, menus, etc.
     */

    /**
     * Checklist manager - real-time element exploration tracking
     */
    private val checklistManager = ChecklistManager(context)
    private val expandableDetector = ExpandableControlDetector

    /**
     * PHASE 3 (2025-12-08): VUID Metrics tracking for observability
     *
     * Tracks real-time VUID creation stats, displays debug overlay, and persists metrics to database.
     */
    private val metricsCollector = VUIDCreationMetricsCollector()
    private val metricsRepository = VUIDMetricsRepository()
    private val debugOverlay by lazy {
        VUIDCreationDebugOverlay(
            context,
            context.getSystemService(android.content.Context.WINDOW_SERVICE) as android.view.WindowManager
        )
    }

    /**
     * Navigation graph builder
     */
    private lateinit var navigationGraphBuilder: NavigationGraphBuilder

    /**
     * Represents a screen state in the exploration stack (for iterative DFS).
     *
     * @property screenHash Unique identifier for this screen
     * @property screenState Full screen state capture
     * @property elements All clickable elements on this screen (with fresh nodes)
     * @property currentElementIndex Index of next element to click
     * @property depth Depth in navigation hierarchy
     * @property parentScreenHash Hash of parent screen (for BACK navigation)
     */
    private data class ExplorationFrame(
        val screenHash: String,
        val screenState: ScreenState,
        val elements: MutableList<com.augmentalis.voiceoscore.learnapp.models.ElementInfo>,
        var currentElementIndex: Int = 0,
        val depth: Int,
        val parentScreenHash: String? = null
    ) {
        fun hasMoreElements(): Boolean = currentElementIndex < elements.size
        fun getNextElement(): com.augmentalis.voiceoscore.learnapp.models.ElementInfo? {
            return if (hasMoreElements()) {
                elements[currentElementIndex].also { currentElementIndex++ }
            } else null
        }
    }

    /**
     * Track generic alias counters per element type
     */
    private val genericAliasCounters = mutableMapOf<String, Int>()

    /**
     * Exploration pause state (Phase 2: Pause/Resume)
     */
    enum class ExplorationPauseState {
        RUNNING,
        PAUSED_BY_USER,
        PAUSED_AUTO  // Auto-paused (permission/login)
    }

    /**
     * Termination reason tracking (P1 Diagnostic Fix - 2025-12-07)
     *
     * Tracks why exploration ended to help diagnose partial learning issues.
     * Logged at exploration completion for debugging.
     */
    enum class TerminationReason {
        COMPLETED,            // Normal completion - all screens explored
        TIMEOUT,              // Auto-pause timeout expired (login/permission)
        RECOVERY_FAILED,      // Couldn't recover to target app after external navigation
        CONSECUTIVE_FAILURES, // Too many consecutive click failures
        STACK_EXHAUSTED,      // DFS stack empty (normal completion path)
        MAX_DURATION,         // Overall exploration timeout reached
        USER_STOPPED          // User manually stopped exploration
    }

    /**
     * Current termination reason (set when exploration ends)
     */
    private var terminationReason: TerminationReason? = null

    /**
     * Exploration state flow
     */
    private val _explorationState = MutableStateFlow<ExplorationState>(ExplorationState.Idle)
    val explorationState: StateFlow<ExplorationState> = _explorationState.asStateFlow()

    /**
     * Pause state flow (Phase 2: Pause/Resume)
     */
    private val _pauseState = MutableStateFlow(ExplorationPauseState.RUNNING)
    val pauseState: StateFlow<ExplorationPauseState> = _pauseState.asStateFlow()

    /**
     * Coroutine scope
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Start timestamp
     */
    private var startTimestamp: Long = 0L

    /**
     * Dangerous elements skipped count
     */
    private var dangerousElementsSkipped = 0

    /**
     * Login screens detected count
     */
    private var loginScreensDetected = 0

    /**
     * Scrollable containers found count
     */
    private var scrollableContainersFound = 0

    /**
     * Current session ID for database persistence
     */
    private var currentSessionId: String? = null

    /**
     * Click failure reason tracking (VOS-PERF-002)
     *
     * Tracks detailed reasons for click failures to help diagnose issues.
     */
    private data class ClickFailureReason(
        val elementDesc: String,
        val elementType: String,
        val reason: String, // "node_stale", "not_visible", "not_enabled", "scroll_failed", "action_failed", "disappeared"
        val timestamp: Long
    )

    /**
     * List of all click failures for telemetry
     */
    private val clickFailures = mutableListOf<ClickFailureReason>()

    /**
     * Hybrid C-Lite: Clicked element stable IDs for current exploration session.
     *
     * VOS-HYBRID-CLITE (2025-12-04): Track clicked elements by stableId
     * instead of by node/UUID. This enables fresh-scrape-before-each-click
     * while still remembering which elements were already clicked.
     *
     * Cleared at start of each new exploration.
     */
    private val clickedStableIds = mutableSetOf<String>()

    /**
     * Debug callback for real-time exploration events
     *
     * Set via [setDebugCallback] to receive notifications about screen
     * exploration, element discovery, and navigation events. Used by
     * debug overlay to visualize exploration progress.
     *
     * @since 2025-12-08 (Debug Overlay Feature)
     */
    private var debugCallback: ExplorationDebugCallback? = null

    init {
        // PHASE 3 (2025-12-08): VUID metrics repository is ready to use
        android.util.Log.d("ExplorationEngine", "VUID metrics repository initialized")
    }

    /**
     * Set the debug callback for exploration events
     *
     * @param callback Callback to receive debug events (null to disable)
     */
    fun setDebugCallback(callback: ExplorationDebugCallback?) {
        debugCallback = callback
        android.util.Log.d("ExplorationEngine", "📊 Debug callback ${if (callback != null) "enabled" else "disabled"}")
    }

    /**
     * Start exploration
     *
     * Begins DFS exploration of app.
     *
     * @param packageName Package name to explore
     * @param sessionId Session ID for database persistence
     */
    fun startExploration(packageName: String, sessionId: String? = null) {
        this.currentSessionId = sessionId
        scope.launch {
            try {
                // FIX (2025-12-03): Cleanup resources from previous exploration
                cleanup()

                // Initialize
                navigationGraphBuilder = NavigationGraphBuilder(packageName)
                screenStateManager.clear()
                clickTracker.clear() // Clear click tracking for new session
                clickFailures.clear() // Clear telemetry for new session
                clickedStableIds.clear() // VOS-HYBRID-CLITE: Clear stableId tracking for new session
                terminationReason = null // P1 Fix: Reset termination tracking for new session
                startTimestamp = System.currentTimeMillis()
                dangerousElementsSkipped = 0
                loginScreensDetected = 0
                scrollableContainersFound = 0

                android.util.Log.i("ExplorationEngine", "🚀 Starting exploration of: $packageName")

                // PHASE 3 (2025-12-08): Reset metrics and show debug overlay
                metricsCollector.reset()
                debugOverlay.setMetricsCollector(metricsCollector)
                if (developerSettings.isDebugOverlayEnabled()) {
                    debugOverlay.show(packageName)
                }

                // FIX: Check if system app (partial support - read-only)
                if (isSystemApp(packageName)) {
                    android.util.Log.w("ExplorationEngine",
                        "⚠️ System app detected: $packageName. " +
                        "System apps have limited support (read-only). " +
                        "Some features may not work correctly.")
                    // Note: We don't block system apps, just warn
                    // User chose option C: partial support (read-only)
                }

                android.util.Log.i("ExplorationEngine", "🏠 Detecting launchers on device...")
                val launchers = launcherDetector.detectLauncherPackages()
                android.util.Log.i("ExplorationEngine", "✅ Detected ${launchers.size} launcher(s): $launchers")

                _explorationState.value = ExplorationState.Running(
                    packageName = packageName,
                    progress = ExplorationProgress(
                        appName = packageName,
                        screensExplored = 0,
                        estimatedTotalScreens = developerSettings.getEstimatedInitialScreenCount(),
                        elementsDiscovered = 0,
                        currentDepth = 0,
                        currentScreen = "Starting...",
                        elapsedTimeMs = 0L
                    )
                )

                // Get ALL windows for target package (multi-window approach with retry)
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine", "🔍 Detecting windows for package: $packageName (with retry)")
                }
                val windows = windowManager.getAppWindowsWithRetry(packageName, launcherDetector)

                if (windows.isEmpty()) {
                    // FIX (2025-12-02): Enhanced error message with troubleshooting hints
                    android.util.Log.e("ExplorationEngine",
                        "❌ No windows found for package: $packageName after retry. " +
                        "Possible causes: " +
                        "1) App not in foreground, " +
                        "2) Covered by system overlay/dialog, " +
                        "3) Accessibility service not properly initialized, " +
                        "4) App still loading/transitioning."
                    )
                    _explorationState.value = ExplorationState.Failed(
                        packageName = packageName,
                        error = IllegalStateException(
                            "No windows found for package: $packageName. " +
                            "Ensure the app is in foreground and not covered by dialogs. " +
                            "If this persists, try dismissing any system overlays."
                        ),
                        partialProgress = null
                    )
                    return@launch
                }

                android.util.Log.i("ExplorationEngine", "✅ Found ${windows.size} window(s) for package: $packageName")
                if (developerSettings.isVerboseLoggingEnabled()) {
                    for (window in windows) {
                        android.util.Log.d("ExplorationEngine", "   - ${window.toLogString()}")
                    }
                }

                // Start DFS exploration on the main app window
                val mainWindow = windows.firstOrNull { it.type == WindowManager.WindowType.MAIN_APP }
                    ?: windows.first() // Fallback to first window if no MAIN_APP type

                android.util.Log.i("ExplorationEngine", "📱 Starting exploration from: ${mainWindow.toLogString()}")

                // Use iterative DFS instead of recursive
                exploreAppIterative(packageName, maxDepth = 10)

                // Exploration completed - check completion status
                // FIX (2025-12-08): Use cumulative tracking instead of clickTracker for accurate stats
                val cumulativeCompleteness = if (cumulativeDiscoveredVuids.isNotEmpty()) {
                    (cumulativeClickedVuids.size.toFloat() / cumulativeDiscoveredVuids.size.toFloat()) * 100f
                } else {
                    0f
                }
                android.util.Log.i("ExplorationEngine", "📊 Exploration Statistics (CUMULATIVE):")
                android.util.Log.i("ExplorationEngine", "   VUIDs: ${cumulativeClickedVuids.size}/${cumulativeDiscoveredVuids.size} clicked")
                android.util.Log.i("ExplorationEngine", "   Completeness: ${cumulativeCompleteness.toInt()}%")

                // Mark app as fully learned if completeness >= threshold (using cumulative stats)
                if (cumulativeCompleteness >= developerSettings.getCompletenessThresholdPercent()) {
                    android.util.Log.i("ExplorationEngine", "✅ App fully learned (${cumulativeCompleteness.toInt()}%)!")
                    android.util.Log.i("ExplorationEngine", "   ${cumulativeClickedVuids.size}/${cumulativeDiscoveredVuids.size} VUIDs clicked")

                    // Mark app as fully learned in database
                    repository.markAppAsFullyLearned(packageName, System.currentTimeMillis())
                } else {
                    android.util.Log.w("ExplorationEngine", "⚠️ App partially learned (${cumulativeCompleteness.toInt()}%)")
                    android.util.Log.w("ExplorationEngine", "   ${cumulativeClickedVuids.size}/${cumulativeDiscoveredVuids.size} VUIDs clicked")
                    android.util.Log.w("ExplorationEngine", "   Not marking as fully learned (threshold: ${developerSettings.getCompletenessThresholdPercent().toInt()}%)")
                }

                val stats = createExplorationStats(packageName)
                _explorationState.value = ExplorationState.Completed(
                    packageName = packageName,
                    stats = stats
                )

            } catch (e: Exception) {
                // FIX (2025-12-04): Log the exception! Previously silently swallowed.
                android.util.Log.e("ExplorationEngine",
                    "❌ Exploration failed for $packageName: ${e.message}", e)

                val currentState = _explorationState.value
                val partialProgress = if (currentState is ExplorationState.Running) {
                    currentState.progress
                } else null

                _explorationState.value = ExplorationState.Failed(
                    packageName = packageName,
                    error = e,
                    partialProgress = partialProgress
                )
            }
        }
    }

    /**
     * Iterative DFS exploration using explicit stack (VOS-EXPLORE-001).
     *
     * **Why Iterative vs Recursive:**
     * - Recursive DFS blocked click loop after 1-2 elements per screen
     * - Iterative DFS completes ALL elements on current screen FIRST
     * - Then explores child screens (proper BFS-like ordering at each level)
     * - Nodes stay fresh (no long recursion delays)
     * - Easy to add progress tracking and element checklists
     *
     * **Algorithm:**
     * 1. Push initial screen onto stack
     * 2. While stack not empty:
     *    a. Peek current frame
     *    b. If has more elements: click next element, push new screen onto stack
     *    c. Else: pop frame, press BACK (if not root)
     * 3. Element nodes refreshed after each BACK to prevent staleness
     *
     * @param packageName Target app package name
     * @param maxDepth Maximum exploration depth (default: 10)
     * @param maxDuration Maximum exploration time in milliseconds (default: 5 minutes)
     * @return ExplorationResult with navigation graph and statistics
     */
    private suspend fun exploreAppIterative(
        packageName: String,
        maxDepth: Int = developerSettings.getMaxExplorationDepth(),
        maxDuration: Long = developerSettings.getExplorationTimeoutMs()
    ) {
        val startTime = System.currentTimeMillis()
        val explorationStack = java.util.Stack<ExplorationFrame>()
        val visitedScreens = mutableSetOf<String>()

        // FIX (2025-12-07): Track navigation paths to prevent re-entry loops
        // Key: "parentHash->childHash", Value: visit count
        // Issue: Same screen reached via different paths causes infinite re-exploration
        // Solution: Track parent->child transitions and limit revisits
        val navigationPaths = mutableMapOf<String, Int>()
        val maxPathRevisits = 2  // Allow at most 2 visits per navigation path

        // FIX (2025-12-07): Track which elements lead to which screens
        // Key: "sourceScreenHash:elementStableId", Value: destination screen hash
        // This allows skipping elements that lead to already-fully-explored screens
        val elementToDestinationScreen = mutableMapOf<String, String>()

        // FIX (2025-12-07): Track already-registered element UUIDs to prevent re-scraping
        // Issue: Screens with ViewPager + vertical scroll cause same elements to be
        // re-scraped and re-registered when switching tabs or scrolling back
        // Key: element stableId, Value: generated UUID
        val registeredElementUuids = mutableSetOf<String>()

        // FIX (2025-12-08): Clear cumulative tracking at START of new exploration session
        // These class-level sets preserve progress across intent relaunches within a session
        // but are cleared for each new app exploration
        cumulativeDiscoveredVuids.clear()
        cumulativeClickedVuids.clear()
        cumulativeBlockedVuids.clear()  // UPDATE (2025-12-08): Clear blocked tracking too

        // Initialize checklist tracking
        checklistManager.startChecklist(packageName)

        // Initialize with root screen
        val rootNode = accessibilityService.rootInActiveWindow ?: run {
            android.util.Log.e("ExplorationEngine", "Root node null at start")
            return
        }

        val rootScreenState = screenStateManager.captureScreenState(rootNode, packageName, 0)
        val rootExploration = screenExplorer.exploreScreen(rootNode, packageName, 0)

        if (rootExploration !is ScreenExplorationResult.Success) {
            android.util.Log.e("ExplorationEngine", "Failed to explore root screen")
            return
        }

        // Track scrollable containers
        scrollableContainersFound += rootExploration.scrollableContainerCount

        // Pre-generate UUIDs for root elements
        val rootElementsWithUuids = preGenerateUuidsForElements(
            rootExploration.safeClickableElements,
            packageName
        )

        val rootFrame = ExplorationFrame(
            screenHash = rootScreenState.hash,
            screenState = rootScreenState,
            elements = rootElementsWithUuids.toMutableList(),
            currentElementIndex = 0,
            depth = 0,
            parentScreenHash = null
        )

        explorationStack.push(rootFrame)
        visitedScreens.add(rootScreenState.hash)

        // Add root screen to checklist
        checklistManager.addScreen(
            screenHash = rootScreenState.hash,
            screenName = rootScreenState.activityName ?: "Root Screen",
            elementCount = rootElementsWithUuids.size
        )

        // Register screen with clickTracker for progress tracking
        // P4 Fix (2025-12-07): Exclude critical dangerous elements from total count
        // These elements are never clicked, so including them inflates totalElements
        // and causes artificially low completeness percentages
        // NOTE: Critical elements still get UUIDs (for voice commands) but aren't counted for completeness
        val criticalElements = rootElementsWithUuids.filter { isCriticalDangerousElement(it) }
        val clickableElements = rootElementsWithUuids.filter { !isCriticalDangerousElement(it) }
        val clickableUuids = clickableElements.mapNotNull { it.uuid }
        clickTracker.registerScreen(rootScreenState.hash, clickableUuids)

        // FIX (2025-12-08): Add to cumulative tracking (class-level, survives intent relaunches)
        cumulativeDiscoveredVuids.addAll(clickableUuids)

        // UPDATE (2025-12-08): Track blocked VUIDs separately for stats display
        val blockedUuids = criticalElements.mapNotNull { it.uuid }
        cumulativeBlockedVuids.addAll(blockedUuids)

        // Log skipped critical elements with details and fire debug callbacks
        if (criticalElements.isNotEmpty()) {
            android.util.Log.i("ExplorationEngine-P4",
                "📊 Screen ${rootScreenState.hash.take(8)}: Registered ${clickableUuids.size} clickable, " +
                "excluded ${criticalElements.size} critical dangerous elements:")
            criticalElements.forEach { elem ->
                val desc = elem.text.ifEmpty { elem.contentDescription.ifEmpty { elem.className } }
                val hasUuid = elem.uuid != null
                android.util.Log.i("ExplorationEngine-P4",
                    "   🚫 \"$desc\" [UUID: ${if (hasUuid) "✓" else "✗"}] - still actionable via voice but excluded from completeness")

                // DEBUG (2025-12-08): Fire blocked callback for debug overlay
                val reason = getCriticalDangerReason(elem) ?: "Critical dangerous element"
                debugCallback?.onElementBlocked(elem.stableId(), rootScreenState.hash, reason)
            }
        }

        android.util.Log.i("ExplorationEngine",
            "🚀 Starting ITERATIVE DFS exploration of $packageName (max depth: $maxDepth)")

        // Main exploration loop
        while (explorationStack.isNotEmpty()) {
            // Phase 2: Check pause state before each iteration
            // FIX (2025-12-06): Added timeout logic to prevent waiting forever when auto-paused
            // P3 Fix (2025-12-07): Added warning at 50% timeout
            if (_pauseState.value != ExplorationPauseState.RUNNING) {
                val pauseState = _pauseState.value
                android.util.Log.i("ExplorationEngine", "⏸️ Exploration paused - waiting for resume (state: $pauseState)")

                // Determine timeout based on pause reason
                val timeout = if (pauseState == ExplorationPauseState.PAUSED_AUTO) {
                    600_000L  // 10 minutes for auto-pause (permissions/login)
                } else {
                    Long.MAX_VALUE  // Infinite for manual user pause
                }

                // P3 Fix: Wait with warning at 50% timeout
                if (pauseState == ExplorationPauseState.PAUSED_AUTO) {
                    val warningTimeout = timeout / 2  // 5 minutes

                    // First phase: wait up to 50% timeout
                    val resumedEarly = withTimeoutOrNull(warningTimeout) {
                        _pauseState.first { it == ExplorationPauseState.RUNNING }
                        true
                    } ?: false

                    if (!resumedEarly && _pauseState.value != ExplorationPauseState.RUNNING) {
                        // Show warning - 50% time remaining
                        android.util.Log.w("ExplorationEngine",
                            "⏰ TIMEOUT_WARNING: ${warningTimeout / 60000} minutes remaining before auto-pause expires")
                        android.util.Log.w("ExplorationEngine",
                            "💡 TIP: Complete login/permission action or tap 'Continue' to resume exploration")

                        // Emit warning state for UI notification
                        val currentState = _explorationState.value
                        if (currentState is ExplorationState.Running) {
                            _explorationState.value = currentState.copy(
                                progress = currentState.progress.copy(
                                    currentScreen = "⏰ Waiting for login/permission (${warningTimeout / 60000}min left)..."
                                )
                            )
                        }

                        // Second phase: wait remaining 50%
                        val resumedLate = withTimeoutOrNull(warningTimeout) {
                            _pauseState.first { it == ExplorationPauseState.RUNNING }
                            true
                        } ?: false

                        if (!resumedLate) {
                            terminationReason = TerminationReason.TIMEOUT
                            android.util.Log.w("ExplorationEngine",
                                "⚠️ Pause timeout reached (${timeout / 60000} minutes) - terminating exploration")
                            android.util.Log.w("ExplorationEngine",
                                "🔍 TERMINATION_REASON: TIMEOUT - Auto-pause (login/permission) timed out after ${timeout / 60000} minutes")
                            break
                        }
                    }
                } else {
                    // Manual pause - wait indefinitely
                    val resumed = withTimeoutOrNull(timeout) {
                        _pauseState.first { it == ExplorationPauseState.RUNNING }
                        true
                    } ?: false

                    if (!resumed) {
                        terminationReason = TerminationReason.TIMEOUT
                        android.util.Log.w("ExplorationEngine",
                            "⚠️ Pause timeout reached - terminating exploration")
                        break
                    }
                }

                android.util.Log.i("ExplorationEngine", "▶️ Exploration resumed - continuing DFS loop")
            }

            // Check timeout
            if (System.currentTimeMillis() - startTime > maxDuration) {
                terminationReason = TerminationReason.MAX_DURATION
                android.util.Log.w("ExplorationEngine", "Exploration timeout reached")
                android.util.Log.w("ExplorationEngine",
                    "🔍 TERMINATION_REASON: MAX_DURATION - Overall exploration timeout (${maxDuration / 60000} minutes) reached")
                break
            }

            val currentFrame = explorationStack.peek()

            // Progress logging
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine-Progress",
                    "📊 Stack depth: ${explorationStack.size}, " +
                    "Current screen: ${currentFrame.screenHash.take(8)}..., " +
                    "Elements: ${currentFrame.currentElementIndex}/${currentFrame.elements.size}, " +
                    "Visited screens: ${visitedScreens.size}")
            }

            // VOS-HYBRID-CLITE (2025-12-04): Use fresh-scrape exploration for this screen
            // This replaces the element-by-element loop with stale node refreshing
            // FIX (2025-12-07): Pass visitedScreens and elementToDestination for loop prevention
            val clickedOnScreen = exploreScreenWithFreshScrape(
                frame = currentFrame,
                packageName = packageName,
                clickedIds = clickedStableIds,
                visitedScreens = visitedScreens,
                elementToDestination = elementToDestinationScreen
            )

            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine-HybridCLite",
                    "✅ Fresh-scrape exploration clicked $clickedOnScreen elements on ${currentFrame.screenHash.take(8)}...")
            }

            // Check if we navigated to a new screen during exploration
            val postExploreRoot = accessibilityService.rootInActiveWindow
            if (postExploreRoot != null) {
                val postExplorePackage = postExploreRoot.packageName?.toString()

                // Handle external app navigation
                if (postExplorePackage != packageName) {
                    android.util.Log.w("ExplorationEngine",
                        "Navigated to external app: $postExplorePackage (expected: $packageName)")

                    val recoveryResult = recoverToTargetAppWithResult(packageName)

                    when (recoveryResult) {
                        RecoveryResult.FAILED -> {
                            android.util.Log.e("ExplorationEngine", "Failed to recover to $packageName")
                            // Pop frame and continue with parent
                            explorationStack.pop()
                            if (explorationStack.isNotEmpty()) {
                                pressBack()
                                delay(developerSettings.getScreenProcessingDelayMs())
                            }
                            continue
                        }
                        RecoveryResult.RECOVERED_VIA_INTENT -> {
                            // FIX (2025-12-05): Intent relaunch puts us at app entry point
                            // The entire DFS stack is now stale - clear it and restart from current screen
                            android.util.Log.w("ExplorationEngine",
                                "🔄 Intent relaunch detected - clearing stale DFS stack (${explorationStack.size} frames)")

                            // NOTE: Do NOT register currentFrame.elements - they reference a stale screen
                            // that no longer exists. The elements were already registered when that
                            // screen was originally explored.

                            // Clear the entire stack
                            explorationStack.clear()

                            // P2 Fix (2025-12-07): Clear click tracker to match fresh exploration
                            // Without this, totalElements is inflated by stale screen registrations,
                            // causing low completeness percentage (e.g., 16%)
                            val oldStats = clickTracker.getStats()
                            clickTracker.clear()
                            android.util.Log.i("ExplorationEngine",
                                "🔄 P2 Fix: Cleared clickTracker (had ${oldStats.totalElements} elements from ${oldStats.totalScreens} stale screens)")

                            // Re-explore from current screen (app entry point)
                            val freshRoot = accessibilityService.rootInActiveWindow
                            if (freshRoot != null) {
                                val freshState = screenStateManager.captureScreenState(
                                    freshRoot, packageName, 0
                                )
                                val freshExploration = screenExplorer.exploreScreen(
                                    freshRoot, packageName, 0
                                )

                                if (freshExploration is ScreenExplorationResult.Success &&
                                    freshState.hash !in visitedScreens) {

                                    // Track scrollable containers
                                    scrollableContainersFound += freshExploration.scrollableContainerCount

                                    // Add to visited screens to prevent infinite loops
                                    visitedScreens.add(freshState.hash)

                                    val freshElements = preGenerateUuidsForElements(
                                        freshExploration.safeClickableElements, packageName
                                    )

                                    // Register screen with clickTracker for progress tracking
                                    // P4 Fix: Exclude critical dangerous elements
                                    val freshClickableUuids = freshElements
                                        .filter { !isCriticalDangerousElement(it) }
                                        .mapNotNull { it.uuid }
                                    clickTracker.registerScreen(freshState.hash, freshClickableUuids)

                                    // FIX (2025-12-08): Add to cumulative tracking (class-level, survives intent relaunches)
                                    cumulativeDiscoveredVuids.addAll(freshClickableUuids)

                                    val freshFrame = ExplorationFrame(
                                        screenHash = freshState.hash,
                                        screenState = freshState,
                                        elements = freshElements.toMutableList(),
                                        currentElementIndex = 0,
                                        depth = 0,
                                        parentScreenHash = null
                                    )

                                    explorationStack.push(freshFrame)
                                    android.util.Log.i("ExplorationEngine",
                                        "✅ Restarted exploration from app entry point: ${freshState.hash.take(8)}...")
                                } else if (freshState.hash in visitedScreens) {
                                    // FIX (2025-12-05): Entry point visited does NOT mean exploration complete!
                                    // The Hybrid C-Lite exploration uses clickedIds to skip already-clicked elements,
                                    // so we should still push a frame to continue exploring unclicked elements.
                                    // Only terminate when completeness >= 95% or no more unclicked elements.
                                    val stats = clickTracker.getStats()
                                    if (stats.overallCompleteness >= 95f) {
                                        android.util.Log.i("ExplorationEngine",
                                            "ℹ️ Entry point visited and ${stats.overallCompleteness}% complete - exploration complete")
                                    } else {
                                        // Resume exploration from entry point - unclicked elements will be clicked
                                        android.util.Log.i("ExplorationEngine",
                                            "🔄 Entry point visited (${freshState.hash.take(8)}...) but only ${stats.overallCompleteness}% complete - resuming exploration")

                                        val resumeExploration = screenExplorer.exploreScreen(
                                            freshRoot, packageName, 0
                                        )

                                        if (resumeExploration is ScreenExplorationResult.Success) {
                                            val resumeElements = preGenerateUuidsForElements(
                                                resumeExploration.safeClickableElements, packageName
                                            )

                                            // Update screen registration with current element count
                                            // P4 Fix: Exclude critical dangerous elements
                                            val resumeClickableUuids = resumeElements
                                                .filter { !isCriticalDangerousElement(it) }
                                                .mapNotNull { it.uuid }
                                            clickTracker.registerScreen(freshState.hash, resumeClickableUuids)

                                            // FIX (2025-12-08): Add to cumulative tracking (class-level, survives intent relaunches)
                                            cumulativeDiscoveredVuids.addAll(resumeClickableUuids)

                                            val resumeFrame = ExplorationFrame(
                                                screenHash = freshState.hash,
                                                screenState = freshState,
                                                elements = resumeElements.toMutableList(),
                                                currentElementIndex = 0,
                                                depth = 0,
                                                parentScreenHash = null
                                            )

                                            explorationStack.push(resumeFrame)
                                            android.util.Log.i("ExplorationEngine",
                                                "🔄 Pushed resume frame with ${resumeElements.size} elements (Hybrid C-Lite will skip already-clicked)")
                                        }
                                    }
                                }
                            }
                            continue
                        }
                        RecoveryResult.RECOVERED_VIA_BACK -> {
                            // BACK recovery preserves DFS stack - continue normally
                            if (developerSettings.isVerboseLoggingEnabled()) {
                                android.util.Log.d("ExplorationEngine",
                                    "✅ Recovered via BACK - DFS stack preserved")
                            }
                        }
                    }
                }

                // Check if on a new screen within the app
                val postExploreState = screenStateManager.captureScreenState(
                    postExploreRoot, packageName, currentFrame.depth + 1
                )

                // FIX (2025-12-07): Track navigation path to detect re-entry loops
                val navPathKey = "${currentFrame.screenHash.take(8)}->${postExploreState.hash.take(8)}"
                val pathVisitCount = navigationPaths.getOrDefault(navPathKey, 0)

                // Check both: not visited AND path not exceeded max revisits
                val shouldExploreScreen = postExploreState.hash != currentFrame.screenHash &&
                    (postExploreState.hash !in visitedScreens || pathVisitCount < maxPathRevisits) &&
                    currentFrame.depth + 1 <= maxDepth

                if (shouldExploreScreen) {
                    // Increment path visit count
                    navigationPaths[navPathKey] = pathVisitCount + 1

                    if (pathVisitCount > 0) {
                        android.util.Log.i("ExplorationEngine-PathTrack",
                            "🔄 Re-visiting path $navPathKey (visit #${pathVisitCount + 1}/$maxPathRevisits)")
                    }

                    // New screen discovered - push onto stack
                    val newExploration = screenExplorer.exploreScreen(
                        postExploreRoot, packageName, currentFrame.depth + 1
                    )

                    if (newExploration is ScreenExplorationResult.Success) {
                        // Track scrollable containers
                        scrollableContainersFound += newExploration.scrollableContainerCount

                        val newElementsWithUuids = preGenerateUuidsForElements(
                            newExploration.safeClickableElements,
                            packageName
                        )

                        val newFrame = ExplorationFrame(
                            screenHash = postExploreState.hash,
                            screenState = postExploreState,
                            elements = newElementsWithUuids.toMutableList(),
                            currentElementIndex = 0,
                            depth = currentFrame.depth + 1,
                            parentScreenHash = currentFrame.screenHash
                        )

                        explorationStack.push(newFrame)
                        visitedScreens.add(postExploreState.hash)

                        checklistManager.addScreen(
                            screenHash = postExploreState.hash,
                            screenName = postExploreState.activityName ?: "Screen #${visitedScreens.size}",
                            elementCount = newElementsWithUuids.size
                        )

                        // P4 Fix: Exclude critical dangerous elements from total count
                        val newClickableUuids = newElementsWithUuids
                            .filter { !isCriticalDangerousElement(it) }
                            .mapNotNull { it.uuid }
                        clickTracker.registerScreen(postExploreState.hash, newClickableUuids)

                        // FIX (2025-12-08): Add to cumulative tracking (class-level, survives intent relaunches)
                        cumulativeDiscoveredVuids.addAll(newClickableUuids)

                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine",
                                "📝 Pushed new screen onto stack: ${postExploreState.hash.take(8)}... " +
                                "(${newElementsWithUuids.size} elements, depth: ${newFrame.depth})")
                        }

                        continue
                    }
                } else if (pathVisitCount >= maxPathRevisits) {
                    // FIX (2025-12-07): Log when path loop is blocked
                    android.util.Log.i("ExplorationEngine-PathTrack",
                        "🛑 Blocking re-entry loop: $navPathKey exceeded max revisits ($pathVisitCount/$maxPathRevisits)")
                }
            }

            // Current screen fully explored - register and pop
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine",
                    "📝 Registering ${currentFrame.elements.size} elements for screen ${currentFrame.screenHash.take(8)}...")
            }

            val elementUuids = registerElements(currentFrame.elements, packageName, registeredElementUuids)

            navigationGraphBuilder.addScreen(
                screenState = currentFrame.screenState,
                elementUuids = elementUuids
            )

            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    repository.saveScreenState(currentFrame.screenState)
                } catch (e: Exception) {
                    android.util.Log.e("ExplorationEngine",
                        "Failed to persist screen state: ${currentFrame.screenState.hash}", e)
                }
            }

            explorationStack.pop()

            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine",
                    "✅ Completed screen ${currentFrame.screenHash.take(8)}... " +
                    "(${currentFrame.elements.size} elements)")
            }

            if (explorationStack.isNotEmpty()) {
                // Navigate back to parent - no need for refreshFrameElements
                // since Hybrid C-Lite always fresh-scrapes before clicking
                pressBack()
                delay(developerSettings.getScreenProcessingDelayMs())
            }
        }

        // Set termination reason if not already set (normal completion)
        if (terminationReason == null) {
            terminationReason = TerminationReason.STACK_EXHAUSTED
        }

        // Log termination reason for diagnostics
        // FIX (2025-12-08): Use cumulative tracking for final stats instead of clickTracker
        // clickTracker.getStats() only reflects post-last-clear data (loses progress on intent relaunch)
        // Cumulative sets preserve ALL exploration progress throughout the session
        //
        // UPDATE (2025-12-08): Show blocked vs non-blocked separately per user request
        // Format: "XX% of Non-Blocked items (XX/YYY), ZZ blocked items"
        val nonBlockedCount = cumulativeDiscoveredVuids.size
        val clickedCount = cumulativeClickedVuids.size
        val blockedCount = cumulativeBlockedVuids.size
        val totalCount = nonBlockedCount + blockedCount  // All discovered items (clickable + blocked)

        val cumulativeCompleteness = if (nonBlockedCount > 0) {
            (clickedCount.toFloat() / nonBlockedCount.toFloat()) * 100f
        } else {
            0f
        }

        // Log both for comparison (debugging)
        val clickTrackerStats = clickTracker.getStats()
        android.util.Log.i("ExplorationEngine",
            "🏁 Iterative DFS complete. Explored ${visitedScreens.size} unique screens")
        android.util.Log.i("ExplorationEngine",
            "🔍 TERMINATION_REASON: $terminationReason")

        // UPDATE (2025-12-08): New stats format showing blocked vs non-blocked separately
        // Format requested: "XX% of Non-Blocked screens (XX/YYY), ##% of non-blocked entries (aa of zz clickable items)"
        android.util.Log.i("ExplorationEngine",
            "📊 Final Stats: ${cumulativeCompleteness.toInt()}% of non-blocked items " +
            "(${clickedCount}/${nonBlockedCount} clicked), ${blockedCount} blocked items skipped")
        android.util.Log.i("ExplorationEngine",
            "📊 Breakdown: ${clickedCount} clicked + ${nonBlockedCount - clickedCount} unclicked + ${blockedCount} blocked = ${totalCount} total VUIDs")
        android.util.Log.i("ExplorationEngine",
            "📊 Screens: ${visitedScreens.size} total visited")

        // Legacy format for comparison (debugging)
        android.util.Log.d("ExplorationEngine",
            "📊 [DEBUG] clickTracker stats (may be post-clear): ${clickTrackerStats.clickedElements}/${clickTrackerStats.totalElements} " +
            "(${clickTrackerStats.overallCompleteness.toInt()}%)")

        // Export checklist to file (use Environment API instead of hardcoded /sdcard)
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val checklistPath = "${downloadsDir.absolutePath}/learnapp-checklist-${packageName.substringAfterLast('.')}-${System.currentTimeMillis()}.md"
        checklistManager.exportToFile(checklistPath)

        // Log checklist summary
        val overallProgress = checklistManager.getOverallProgress()
        android.util.Log.i("ExplorationEngine",
            "📋 Checklist exported to: $checklistPath (Progress: $overallProgress%)")
    }

    /**
     * Pre-generate UUIDs for a list of elements (before clicking).
     */
    private suspend fun preGenerateUuidsForElements(
        elements: List<com.augmentalis.voiceoscore.learnapp.models.ElementInfo>,
        packageName: String
    ): List<com.augmentalis.voiceoscore.learnapp.models.ElementInfo> {
        val startTime = System.currentTimeMillis()

        // PHASE 3 (2025-12-08): Track element detection
        elements.forEach { _ ->
            metricsCollector.onElementDetected()
        }

        val elementsWithUuids = elements.map { element ->
            element.node?.let { node ->
                val uuid = thirdPartyGenerator.generateUuid(node, packageName)

                // PHASE 3 (2025-12-08): Track VUID creation
                metricsCollector.onVUIDCreated()

                element.copy(uuid = uuid)
            } ?: element
        }
        val elapsed = System.currentTimeMillis() - startTime

        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d("ExplorationEngine-Perf",
                "⚡ Pre-generated ${elements.size} UUIDs in ${elapsed}ms")
        }

        return elementsWithUuids
    }

    /**
     * Refresh elements in a frame after BACK navigation (prevent stale nodes).
     *
     * FIX (2025-12-04): Added multi-factor element matching and validation logging.
     *
     * Problem: UUID-only matching fails when bounds shift after first click,
     * causing UUIDs to change. This resulted in 93% of elements failing to refresh.
     *
     * Solution: Use multi-factor matching (UUID → text+contentDesc → fuzzy bounds)
     * with validation logging to expose actual refresh success/failure.
     */
    private suspend fun refreshFrameElements(
        frame: ExplorationFrame,
        packageName: String
    ) {
        val rootNode = accessibilityService.rootInActiveWindow ?: return
        val freshExploration = screenExplorer.exploreScreen(rootNode, packageName, frame.depth)

        if (freshExploration is ScreenExplorationResult.Success) {
            val freshElements = freshExploration.safeClickableElements

            // Build lookup maps for multi-factor matching
            // FIX (2025-12-04): Filter null UUIDs to prevent failed lookups
            val freshByUuid = freshElements.filter { it.uuid != null }.associateBy { it.uuid!! }
            // FIX (2025-12-04): Add resourceId-based lookup (most stable identifier)
            val freshByResourceId = freshElements.filter { it.resourceId.isNotEmpty() }.associateBy { it.resourceId }
            // FIX (2025-12-04): Include resourceId in text matching key for better discrimination
            val freshByTextAndDesc = freshElements.groupBy {
                "${it.resourceId}|${it.text ?: ""}|${it.contentDescription ?: ""}|${it.className}"
            }

            var refreshedCount = 0
            var resourceIdMatchCount = 0
            var uuidMatchCount = 0
            var textMatchCount = 0
            var boundsMatchCount = 0
            var failedCount = 0
            val remainingCount = frame.elements.size - frame.currentElementIndex

            // Update remaining elements with fresh nodes using multi-factor matching
            for (i in frame.currentElementIndex until frame.elements.size) {
                val oldElement = frame.elements[i]
                var matched = false

                // Strategy 0: ResourceId match (most reliable for apps with proper view IDs)
                // FIX (2025-12-04): Added as first strategy - resourceId is stable and populated
                if (!matched && oldElement.resourceId.isNotEmpty()) {
                    freshByResourceId[oldElement.resourceId]?.let { freshElement ->
                        frame.elements[i] = freshElement
                        matched = true
                        resourceIdMatchCount++
                        refreshedCount++
                    }
                }

                // Strategy 1: UUID match (fastest, most reliable when bounds don't shift)
                if (!matched) {
                    oldElement.uuid?.let { uuid ->
                        freshByUuid[uuid]?.let { freshElement ->
                            frame.elements[i] = freshElement
                            matched = true
                            uuidMatchCount++
                            refreshedCount++
                        }
                    }
                }

                // Strategy 2: Text + ContentDescription + ClassName match
                // FIX (2025-12-04): Updated key to include resourceId for better discrimination
                if (!matched) {
                    val key = "${oldElement.resourceId}|${oldElement.text ?: ""}|${oldElement.contentDescription ?: ""}|${oldElement.className}"
                    freshByTextAndDesc[key]?.firstOrNull()?.let { freshElement ->
                        frame.elements[i] = freshElement
                        matched = true
                        textMatchCount++
                        refreshedCount++
                    }
                }

                // Strategy 3: Fuzzy bounds match (within tolerance)
                // FIX (2025-12-04): Increased tolerance to 20px (UI often shifts 10-15px after clicks)
                if (!matched) {
                    val fuzzyMatch = freshElements.find { fresh ->
                        areBoundsSimilar(oldElement.bounds, fresh.bounds, tolerance = developerSettings.getBoundsTolerancePixels())
                    }
                    fuzzyMatch?.let { freshElement ->
                        frame.elements[i] = freshElement
                        matched = true
                        boundsMatchCount++
                        refreshedCount++
                    }
                }

                if (!matched) {
                    failedCount++
                }
            }

            // Validation logging - expose actual refresh results
            // FIX (2025-12-04): Added resourceIdMatchCount to logging
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine-Refresh",
                    "📊 Element refresh results: $refreshedCount/$remainingCount succeeded " +
                    "(ResourceId: $resourceIdMatchCount, UUID: $uuidMatchCount, Text: $textMatchCount, Bounds: $boundsMatchCount, Failed: $failedCount)")
            }

            if (failedCount > 0) {
                android.util.Log.w("ExplorationEngine-Refresh",
                    "⚠️ $failedCount elements could not be refreshed - may cause click failures")
            }

            if (refreshedCount == remainingCount) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine",
                        "✅ Refreshed all $remainingCount remaining elements with fresh nodes")
                }
            } else {
                android.util.Log.w("ExplorationEngine",
                    "⚠️ Only refreshed $refreshedCount/$remainingCount elements - ${failedCount} will use stale nodes")
            }
        } else {
            android.util.Log.w("ExplorationEngine-Refresh",
                "❌ Failed to re-explore screen for element refresh")
        }
    }

    /**
     * Check if two bounds rectangles are similar within a tolerance.
     *
     * FIX (2025-12-04): Added for fuzzy bounds matching.
     *
     * @param bounds1 First bounds rectangle
     * @param bounds2 Second bounds rectangle
     * @param tolerance Maximum pixel difference allowed on each edge
     * @return true if bounds are within tolerance
     */
    private fun areBoundsSimilar(bounds1: Rect, bounds2: Rect, tolerance: Int): Boolean {
        return kotlin.math.abs(bounds1.left - bounds2.left) <= tolerance &&
               kotlin.math.abs(bounds1.top - bounds2.top) <= tolerance &&
               kotlin.math.abs(bounds1.right - bounds2.right) <= tolerance &&
               kotlin.math.abs(bounds1.bottom - bounds2.bottom) <= tolerance
    }

    /**
     * Explore all elements on current screen using Hybrid C-Lite fresh-scrape strategy.
     *
     * VOS-HYBRID-CLITE (2025-12-04): Replaces stale-node-prone element refresh with
     * fresh-scrape-before-each-click approach for 98% click success rate.
     *
     * Algorithm:
     * 1. Maintain a set of clicked stableIds
     * 2. For each click iteration:
     *    a. Fresh scrape the screen (get new nodes)
     *    b. Filter out already-clicked elements by stableId
     *    c. Sort remaining by stabilityScore (highest first)
     *    d. Click top element (node-first, gesture fallback)
     *    e. Add stableId to clicked set
     * 3. Repeat until no unclicked elements remain OR screen changes
     *
     * Benefits:
     * - Every click uses a fresh node (never stale)
     * - stableId survives UI shifts (resourceId > text > contentDesc > bounds)
     * - stabilityScore prioritizes most reliable elements first
     * - Gesture fallback handles node click failures
     *
     * @param frame Current exploration frame
     * @param packageName Target app package name
     * @param clickedIds Set to track clicked element stableIds (modified in place)
     * @param visitedScreens Set of fully-explored screen hashes
     * @param elementToDestination Map of element→destination screen for loop prevention
     * @return Number of elements successfully clicked
     */
    private suspend fun exploreScreenWithFreshScrape(
        frame: ExplorationFrame,
        packageName: String,
        clickedIds: MutableSet<String>,
        visitedScreens: Set<String> = emptySet(),
        elementToDestination: MutableMap<String, String> = mutableMapOf()
    ): Int {
        var clickCount = 0
        var consecutiveFailures = 0
        val maxConsecutiveFailures = developerSettings.getMaxConsecutiveClickFailures()
        val screenStartHash = frame.screenHash

        // FIX (2025-12-07): Added per-screen exploration timeout to prevent infinite loops
        // on screens with dynamic/loading content (Teams channels, social feeds)
        val screenExplorationTimeout = 120_000L  // 2 minutes max per screen
        val screenStartTime = System.currentTimeMillis()

        // FIX (2025-12-07): Cap clicks at totalElements + buffer to prevent over-clicking loops
        // Issue: Same screen re-entered via different navigation paths causes 260%+ click counts
        // Solution: Hard cap based on registered element count with small buffer for dynamic content
        val registeredProgress = clickTracker.getScreenProgress(screenStartHash)
        val maxClicksForScreen = if (registeredProgress != null) {
            registeredProgress.totalClickableElements + 2  // +2 buffer for dynamic elements
        } else {
            frame.elements.size + 2
        }

        android.util.Log.i("ExplorationEngine-HybridCLite",
            "🔄 Starting fresh-scrape exploration for screen ${screenStartHash.take(8)}... (max clicks: $maxClicksForScreen)")

        while (consecutiveFailures < maxConsecutiveFailures) {
            // FIX (2025-12-07): Check click cap to prevent over-clicking
            if (clickCount >= maxClicksForScreen) {
                android.util.Log.i("ExplorationEngine-HybridCLite",
                    "🛑 Click cap reached ($clickCount/$maxClicksForScreen) on ${screenStartHash.take(8)}... - preventing over-click loop")
                break
            }

            // Check per-screen timeout
            if (System.currentTimeMillis() - screenStartTime > screenExplorationTimeout) {
                android.util.Log.w("ExplorationEngine-HybridCLite",
                    "⏰ Per-screen exploration timeout (${screenExplorationTimeout/1000}s) reached on ${screenStartHash.take(8)}... " +
                    "- clicked $clickCount elements before timeout")
                break
            }
            // Step 1: Fresh scrape - get current screen elements
            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                android.util.Log.w("ExplorationEngine-HybridCLite", "Root node null, aborting")
                break
            }

            // Check if still on same package
            val currentPackage = rootNode.packageName?.toString()
            if (currentPackage != packageName) {
                android.util.Log.w("ExplorationEngine-HybridCLite",
                    "Package changed from $packageName to $currentPackage, aborting")
                break
            }

            // Fresh exploration (with timeout protection for scrollable containers)
            // FIX (2025-12-07): Wrap exploreScreen in timeout to prevent hangs on infinite scrollables
            val freshExploration = kotlinx.coroutines.withTimeoutOrNull(45_000L) {
                screenExplorer.exploreScreen(rootNode, packageName, frame.depth)
            }

            if (freshExploration == null) {
                android.util.Log.w("ExplorationEngine-HybridCLite",
                    "⏰ Fresh scrape timeout (45s) - screen has complex scrollables. Continuing with visible elements only.")
                consecutiveFailures++
                delay(developerSettings.getScrollDelayMs())
                continue
            }

            if (freshExploration !is ScreenExplorationResult.Success) {
                android.util.Log.w("ExplorationEngine-HybridCLite", "Fresh scrape failed")
                consecutiveFailures++
                delay(developerSettings.getScrollDelayMs())
                continue
            }

            // PHASE 3 (2025-12-08): Track element detection
            freshExploration.allElements.forEach { _ ->
                metricsCollector.onElementDetected()
            }

            // DEBUG (2025-12-08): Fire debug callback with screen elements
            debugCallback?.onScreenExplored(
                elements = freshExploration.safeClickableElements,
                screenHash = screenStartHash,
                activityName = frame.screenState.activityName ?: "Unknown",
                packageName = packageName,
                parentScreenHash = frame.parentScreenHash
            )

            // Step 2: Filter out clicked elements and sort by stability
            // FIX (2025-12-05): Sort dangerous buttons last to minimize early navigation
            // FIX (2025-12-05): SKIP critical dangerous elements entirely (exit, power off, etc.)
            // FIX (2025-12-07): Skip elements that lead to already-fully-explored screens
            val unclickedElements = freshExploration.safeClickableElements
                .filter { it.stableId() !in clickedIds }
                .filter { element ->
                    // PHASE 3 (2025-12-08): Track filtering for critical dangerous elements
                    if (isCriticalDangerousElement(element)) {
                        metricsCollector.onElementFiltered(element, "Critical dangerous element")
                        false  // NEVER click critical elements
                    } else {
                        true
                    }
                }
                .filter { element ->
                    // Skip elements that we know lead to already-visited screens
                    val elementKey = "${screenStartHash}:${element.stableId()}"
                    val destinationScreen = elementToDestination[elementKey]
                    if (destinationScreen != null && destinationScreen in visitedScreens) {
                        // PHASE 3 (2025-12-08): Track filtering for already-visited destinations
                        metricsCollector.onElementFiltered(element, "Leads to already-visited screen")
                        android.util.Log.i("ExplorationEngine-HybridCLite",
                            "🔄 Skipping '${element.text.ifEmpty { element.contentDescription }}' - leads to already-visited ${destinationScreen.take(8)}...")
                        false
                    } else {
                        true
                    }
                }
                .sortedWith(compareBy<com.augmentalis.voiceoscore.learnapp.models.ElementInfo> {
                    // Dangerous elements go last (higher = later)
                    if (isDangerousElement(it)) 1 else 0
                }.thenByDescending {
                    // Within same danger level, sort by stability (highest first)
                    it.stabilityScore()
                })

            if (unclickedElements.isEmpty()) {
                android.util.Log.i("ExplorationEngine-HybridCLite",
                    "✅ All elements clicked on this screen ($clickCount total)")
                break
            }

            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine-HybridCLite",
                    "📊 Fresh scrape: ${unclickedElements.size} unclicked elements " +
                    "(top stability: ${unclickedElements.first().stabilityScore()})")
            }

            // Step 3: Click the most stable unclicked element
            val element = unclickedElements.first()
            val stableId = element.stableId()
            val elementDesc = element.text.ifEmpty { element.contentDescription.ifEmpty { element.className } }
            val elementType = element.className.substringAfterLast('.')

            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine-HybridCLite",
                    ">>> CLICKING: \"$elementDesc\" ($elementType) [stability: ${element.stabilityScore()}, id: ${stableId.take(30)}...]")
            }

            // Attempt click: node-first, gesture fallback
            var clicked = false

            // Try node click first (using fresh node)
            element.node?.let { node ->
                clicked = clickElement(node, elementDesc, elementType)
                if (clicked) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-HybridCLite",
                            "✅ Node click succeeded for \"$elementDesc\"")
                    }
                }
            }

            // Gesture fallback if node click failed
            if (!clicked) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine-HybridCLite",
                        "⚡ Node click failed, trying gesture fallback at (${element.bounds.centerX()}, ${element.bounds.centerY()})")
                }

                clicked = performCoordinateClick(element.bounds)
                if (clicked) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-HybridCLite",
                            "✅ Gesture click succeeded for \"$elementDesc\"")
                    }
                }
            }

            if (clicked) {
                // Mark element as clicked by stableId
                clickedIds.add(stableId)
                clickCount++
                consecutiveFailures = 0

                // Also update tracking systems if VUID available
                element.node?.let { node ->
                    val vuid = thirdPartyGenerator.generateUuid(node, packageName)

                    // PHASE 3 (2025-12-08): Track VUID creation
                    metricsCollector.onVUIDCreated()

                    clickTracker.markElementClicked(frame.screenHash, vuid)
                    checklistManager.markElementCompleted(frame.screenHash, vuid)
                    // FIX (2025-12-08): Add to cumulative clicked tracking (class-level, survives intent relaunches)
                    cumulativeClickedVuids.add(vuid)

                    // DEBUG (2025-12-08): Fire clicked callback for debug overlay
                    debugCallback?.onElementClicked(stableId, screenStartHash, vuid)
                }

                // Wait for UI to settle
                delay(developerSettings.getClickDelayMs())

                // Check if screen changed (navigation occurred)
                val postClickRoot = accessibilityService.rootInActiveWindow
                if (postClickRoot != null) {
                    val postClickState = screenStateManager.captureScreenState(
                        postClickRoot, packageName, frame.depth
                    )
                    if (postClickState.hash != screenStartHash) {
                        // FIX (2025-12-07): Record element→destination mapping for loop prevention
                        val elementKey = "${screenStartHash}:${stableId}"
                        elementToDestination[elementKey] = postClickState.hash

                        // DEBUG (2025-12-08): Fire navigation callback for debug overlay
                        debugCallback?.onElementNavigated(elementKey, postClickState.hash)

                        android.util.Log.i("ExplorationEngine-HybridCLite",
                            "📍 Screen changed from ${screenStartHash.take(8)}... to ${postClickState.hash.take(8)}... (mapped: $elementKey)")
                        // Screen navigation detected - let caller handle it
                        break
                    }
                }
            } else {
                android.util.Log.w("ExplorationEngine-HybridCLite",
                    "❌ Both node and gesture click failed for \"$elementDesc\"")
                consecutiveFailures++

                // Still mark as clicked to avoid infinite retry
                clickedIds.add(stableId)
            }
        }

        if (consecutiveFailures >= maxConsecutiveFailures) {
            // Note: This sets a local flag but doesn't terminate the whole exploration
            // It only stops exploring THIS screen. The main loop continues.
            android.util.Log.w("ExplorationEngine-HybridCLite",
                "⚠️ Reached $maxConsecutiveFailures consecutive failures, stopping screen exploration")
            android.util.Log.w("ExplorationEngine-HybridCLite",
                "🔍 SCREEN_TERMINATION: CONSECUTIVE_FAILURES on screen ${frame.screenHash.take(8)}...")
        }

        android.util.Log.i("ExplorationEngine-HybridCLite",
            "🏁 Fresh-scrape exploration complete: $clickCount elements clicked")

        return clickCount
    }

    /**
     * Attempt to recover to target app after navigation to external app/launcher.
     *
     * FIX (2025-12-05): Enhanced recovery mechanism
     * - Dismiss keyboard first (may be blocking navigation)
     * - Try BACK presses (5 attempts instead of 3)
     * - Fallback: relaunch app via intent if BACK fails
     */
    /**
     * Recovery result indicating how the app was recovered
     */
    private enum class RecoveryResult {
        RECOVERED_VIA_BACK,      // Recovered by pressing BACK - DFS stack is still valid
        RECOVERED_VIA_INTENT,    // Recovered by relaunching - DFS stack is STALE, needs reset
        FAILED                   // Could not recover
    }

    /**
     * Attempt to recover to target app after navigation to external app/launcher.
     *
     * FIX (2025-12-05): Enhanced recovery with result type
     * - Returns RecoveryResult to indicate how recovery happened
     * - Intent relaunch invalidates the DFS stack (caller should clear it)
     */
    private suspend fun recoverToTargetAppWithResult(packageName: String): RecoveryResult {
        // Step 1: Dismiss keyboard first (may be blocking navigation)
        dismissKeyboard()
        delay(developerSettings.getClickDelayMs())

        // Step 2: Try BACK presses (increased from 3 to 5 attempts)
        repeat(5) { attempt ->
            pressBack()
            delay(developerSettings.getClickDelayMs())  // Slightly faster than before (was 1000ms)

            val currentNode = accessibilityService.rootInActiveWindow
            val currentPackage = currentNode?.packageName?.toString()

            if (currentPackage == packageName) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine",
                        "✅ Recovered to $packageName after ${attempt + 1} BACK attempts")
                }
                return RecoveryResult.RECOVERED_VIA_BACK
            }
        }

        // Step 3: Fallback - relaunch app via intent
        android.util.Log.w("ExplorationEngine",
            "BACK presses failed, attempting to relaunch $packageName via intent")

        return try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                launchIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP)
                context.startActivity(launchIntent)
                delay(developerSettings.getScreenProcessingDelayMs() * 2)  // Wait for app to launch

                val currentNode = accessibilityService.rootInActiveWindow
                val currentPackage = currentNode?.packageName?.toString()

                if (currentPackage == packageName) {
                    android.util.Log.i("ExplorationEngine",
                        "✅ Recovered to $packageName via intent relaunch (DFS stack invalidated)")
                    RecoveryResult.RECOVERED_VIA_INTENT
                } else {
                    android.util.Log.e("ExplorationEngine",
                        "❌ Intent relaunch failed, current package: $currentPackage")
                    RecoveryResult.FAILED
                }
            } else {
                android.util.Log.e("ExplorationEngine",
                    "❌ No launch intent found for $packageName")
                RecoveryResult.FAILED
            }
        } catch (e: Exception) {
            android.util.Log.e("ExplorationEngine",
                "❌ Failed to relaunch $packageName: ${e.message}", e)
            RecoveryResult.FAILED
        }
    }

    /**
     * Legacy wrapper for backward compatibility
     */
    private suspend fun recoverToTargetApp(packageName: String): Boolean {
        return recoverToTargetAppWithResult(packageName) != RecoveryResult.FAILED
    }

    /**
     * Dismiss soft keyboard if visible.
     * FIX (2025-12-05): Added keyboard dismissal to prevent navigation interference
     */
    private fun dismissKeyboard() {
        try {
            // Use global action to dismiss keyboard
            accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine", "Attempted keyboard dismissal via BACK action")
            }
        } catch (e: Exception) {
            android.util.Log.w("ExplorationEngine", "Failed to dismiss keyboard: ${e.message}")
        }
    }

    /**
     * Check if element is "dangerous" - likely to navigate away from app.
     *
     * FIX (2025-12-05): Added dangerous button detection
     * Dangerous buttons are clicked last to maximize exploration before potential navigation.
     *
     * Dangerous patterns:
     * - Submit, Send, Confirm, Done, OK, Apply (form submissions)
     * - Sign out, Log out, Exit, Close, Quit (exit actions)
     * - Delete, Remove (destructive actions)
     *
     * @param element Element to check
     * @return true if element is potentially dangerous
     */
    private fun isDangerousElement(element: com.augmentalis.voiceoscore.learnapp.models.ElementInfo): Boolean {
        val text = element.text.lowercase()
        val contentDesc = element.contentDescription.lowercase()
        val combinedText = "$text $contentDesc"

        // Dangerous patterns that may navigate away or submit data
        val dangerousPatterns = listOf(
            // Form submission
            "submit", "send", "confirm", "done", "apply", "save",
            "post", "publish", "upload", "share",
            // Exit actions
            "sign out", "signout", "log out", "logout", "exit", "quit", "close",
            // Destructive
            "delete", "remove", "clear all", "reset",
            // Navigation that might leave app
            "continue", "proceed", "next", "finish"
        )

        // Check if any pattern matches
        val isDangerous = dangerousPatterns.any { pattern ->
            combinedText.contains(pattern)
        }

        if (isDangerous) {
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine-DangerDetect",
                    "⚠️ Dangerous element detected: \"${element.text.ifEmpty { element.contentDescription }}\" - will click last")
            }
        }

        return isDangerous
    }

    /**
     * Check if element is CRITICAL dangerous (should NEVER be clicked)
     *
     * These elements would cause severe side effects if clicked:
     * - Power off, Shutdown, Restart, Sleep (system actions)
     * - Exit, Quit, Close (app termination)
     * - Sign out, Log out, Logout (session termination)
     * - Delete account, Deactivate (destructive account actions)
     *
     * FIX (2025-12-05): These elements are UUID'd and logged but NEVER clicked.
     *
     * @param element Element to check
     * @return true if element should never be clicked
     */
    private fun isCriticalDangerousElement(element: com.augmentalis.voiceoscore.learnapp.models.ElementInfo): Boolean {
        val text = element.text.lowercase()
        val contentDesc = element.contentDescription.lowercase()
        val resourceId = element.resourceId.lowercase()
        val combinedText = "$text $contentDesc $resourceId"

        // Critical patterns that should NEVER be clicked during exploration
        val criticalPatterns = listOf(
            // System power actions
            "power off", "poweroff", "shut down", "shutdown", "restart", "reboot",
            "sleep", "hibernate", "turn off",
            // App termination
            "exit", "quit", "close app", "force stop", "force close",
            // Session termination
            "sign out", "signout", "log out", "logout", "sign-out", "log-out",
            // Destructive account actions
            "delete account", "deactivate account", "remove account", "close account",
            // Factory/system reset
            "factory reset", "wipe data", "erase all", "format",
            // CRITICAL: Call/meeting actions (2025-12-08) - NEVER initiate calls
            "call", "make a call", "make call", "start call", "audio call", "video call",
            "dial", "answer", "join call", "join meeting", "new meeting", "schedule meeting",
            "instant meeting", "meet now", "call_control", "call_end", "calls_call",
            // CRITICAL: Reply actions (2025-12-08) - can send messages
            "reply"
        )

        val isCritical = criticalPatterns.any { pattern ->
            combinedText.contains(pattern)
        }

        if (isCritical) {
            android.util.Log.w("ExplorationEngine-CriticalDanger",
                "🚫 CRITICAL: Skipping click on \"${element.text.ifEmpty { element.contentDescription }}\" - element will be UUID'd but NOT clicked")
            dangerousElementsSkipped++
        }

        return isCritical
    }

    /**
     * Get the reason why an element is critically dangerous (2025-12-08)
     *
     * @param element Element to check
     * @return Reason string or null if not dangerous
     */
    private fun getCriticalDangerReason(element: com.augmentalis.voiceoscore.learnapp.models.ElementInfo): String? {
        val text = element.text.lowercase()
        val contentDesc = element.contentDescription.lowercase()
        val resourceId = element.resourceId.lowercase()
        val combinedText = "$text $contentDesc $resourceId"

        // Critical patterns with their reasons
        val criticalPatternsWithReasons = listOf(
            // System power actions
            "power off" to "Power off (CRITICAL)",
            "poweroff" to "Power off (CRITICAL)",
            "shut down" to "Shutdown (CRITICAL)",
            "shutdown" to "Shutdown (CRITICAL)",
            "restart" to "Restart (CRITICAL)",
            "reboot" to "Reboot (CRITICAL)",
            "sleep" to "Sleep (CRITICAL)",
            "hibernate" to "Hibernate (CRITICAL)",
            "turn off" to "Turn off (CRITICAL)",
            // App termination
            "exit" to "Exit (CRITICAL)",
            "quit" to "Quit (CRITICAL)",
            "close app" to "Close app (CRITICAL)",
            "force stop" to "Force stop (CRITICAL)",
            "force close" to "Force close (CRITICAL)",
            // Session termination
            "sign out" to "Sign out",
            "signout" to "Sign out",
            "log out" to "Log out",
            "logout" to "Log out",
            "sign-out" to "Sign out",
            "log-out" to "Log out",
            // Destructive account actions
            "delete account" to "Delete account",
            "deactivate account" to "Deactivate account",
            "remove account" to "Remove account",
            "close account" to "Close account",
            // Factory/system reset
            "factory reset" to "Factory reset (CRITICAL)",
            "wipe data" to "Wipe data (CRITICAL)",
            "erase all" to "Erase all (CRITICAL)",
            "format" to "Format (CRITICAL)",
            // Call/meeting actions
            "call" to "Call (CRITICAL)",
            "make a call" to "Make call (CRITICAL)",
            "make call" to "Make call (CRITICAL)",
            "start call" to "Start call (CRITICAL)",
            "audio call" to "Audio call (CRITICAL)",
            "video call" to "Video call (CRITICAL)",
            "dial" to "Dial (CRITICAL)",
            "answer" to "Answer (CRITICAL)",
            "join call" to "Join call (CRITICAL)",
            "join meeting" to "Join meeting (CRITICAL)",
            "new meeting" to "New meeting (CRITICAL)",
            "schedule meeting" to "Schedule meeting (CRITICAL)",
            "instant meeting" to "Instant meeting (CRITICAL)",
            "meet now" to "Meet now (CRITICAL)",
            "call_control" to "Call control (CRITICAL)",
            "call_end" to "End call (CRITICAL)",
            "calls_call" to "Call item (CRITICAL)",
            // Reply actions
            "reply" to "Reply (sends message)"
        )

        for ((pattern, reason) in criticalPatternsWithReasons) {
            if (combinedText.contains(pattern)) {
                return reason
            }
        }

        return null
    }

    /**
     * Explore screen recursively (DFS)
     *
     * Core DFS algorithm.
     *
     * @param rootNode Root node of current screen
     * @param packageName Package name
     * @param depth Current depth
     */
    @Suppress("UNNECESSARY_SAFE_CALL")  // Element properties can be null at runtime despite type inference
    private suspend fun exploreScreenRecursive(
        rootNode: AccessibilityNodeInfo,
        packageName: String,
        depth: Int
    ) {
        // Clear click failures for this screen (per-screen telemetry)
        clickFailures.clear()

        // PACKAGE NAME VALIDATION: Verify rootNode belongs to target app
        val actualPackageName = rootNode.packageName?.toString()
        if (actualPackageName == null || actualPackageName != packageName) {
            android.util.Log.w("ExplorationEngine",
                "exploreScreenRecursive called with wrong package: $actualPackageName (expected: $packageName). " +
                "Skipping exploration to prevent registering foreign app elements.")
            return
        }

        // Check depth limit
        if (depth > strategy.maxDepth) {
            return
        }

        // Check time limit
        val elapsed = System.currentTimeMillis() - startTimestamp
        if (elapsed > strategy.getMaxExplorationTime()) {
            return
        }

        // Check if paused
        val state = _explorationState.value
        if (state is ExplorationState.PausedByUser) {
            // Wait for resume
            waitForResume()
        }

        // 1. Explore current screen
        val explorationResult = screenExplorer.exploreScreen(rootNode, packageName, depth)

        when (explorationResult) {
            is ScreenExplorationResult.AlreadyVisited -> {
                // Already explored this screen, backtrack
                return
            }

            is ScreenExplorationResult.LoginScreen -> {
                // STEP 1: Mark screen as visited (prevent re-exploration)
                screenStateManager.markAsVisited(explorationResult.screenState.hash)

                // STEP 2: Register ALL elements on login screen (including login fields)
                // This ensures the navigation matrix is complete and voice commands can reference them
                // NOTE: We register element STRUCTURE only - credential values are NOT captured
                val allElementsToRegister = explorationResult.allElements
                val elementUuids = registerElements(
                    elements = allElementsToRegister,
                    packageName = packageName
                )

                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine",
                        "Login screen detected. Registered ${elementUuids.size} elements " +
                        "(${explorationResult.loginElements.size} login fields) before pausing. " +
                        "NOTE: Element structures registered - credential values NOT captured.")
                }

                // STEP 3: Add screen to navigation graph with ALL elements
                navigationGraphBuilder.addScreen(
                    screenState = explorationResult.screenState,
                    elementUuids = elementUuids
                )

                // STEP 4: Persist screen state to database
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        repository.saveScreenState(explorationResult.screenState)
                    } catch (e: Exception) {
                        android.util.Log.e("ExplorationEngine",
                            "Failed to persist login screen state: ${explorationResult.screenState.hash}", e)
                    }
                }

                // STEP 5: Update progress
                updateProgress(packageName, depth, explorationResult.screenState.hash)

                // STEP 6: Notify user to enter credentials (notification + sound)
                notifyUserForLoginScreen(packageName)

                // STEP 7: NOW pause for user login (elements already saved)
                loginScreensDetected++
                _explorationState.value = ExplorationState.PausedForLogin(
                    packageName = packageName,
                    progress = getCurrentProgress(packageName, depth)
                )

                // Wait for user to login (screen change)
                waitForScreenChange(explorationResult.screenState.hash)

                // STEP 8: Resume exploration from new screen after login
                val newRootNode = accessibilityService.rootInActiveWindow ?: return
                exploreScreenRecursive(newRootNode, packageName, depth)
                return
            }

            is ScreenExplorationResult.Error -> {
                // Skip this screen
                return
            }

            is ScreenExplorationResult.Success -> {
                // Mark screen as visited
                screenStateManager.markAsVisited(explorationResult.screenState.hash)

                // Track scrollable containers
                scrollableContainersFound += explorationResult.scrollableContainerCount

                // ═══════════════════════════════════════════════════════════════════
                // CRITICAL FIX (VOS-PERF-001): Click-Before-Register Pattern
                // ═══════════════════════════════════════════════════════════════════
                // PROBLEM: registerElements() takes 1351ms (315 DB ops), causing
                //          AccessibilityNodeInfo references to become stale.
                //          Result: 30-50% click failure rate.
                //
                // SOLUTION: Generate UUIDs (fast), CLICK elements while nodes fresh,
                //           THEN register to database (nodes no longer needed).
                //
                // Expected: Click success rate 50% → 95%+
                // ═══════════════════════════════════════════════════════════════════

                // STEP 1: Pre-generate UUIDs for ALL elements (fast, no DB)
                val allElementsToRegister = explorationResult.allElements
                val tempUuidMap = mutableMapOf<com.augmentalis.voiceoscore.learnapp.models.ElementInfo, String>()

                // PHASE 3 (2025-12-08): Track element detection
                allElementsToRegister.forEach { _ ->
                    metricsCollector.onElementDetected()
                }

                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine-Perf",
                        "⚡ Click-Before-Register: Pre-generating UUIDs for ${allElementsToRegister.size} elements...")
                }

                val uuidGenStartTime = System.currentTimeMillis()
                for (element in allElementsToRegister) {
                    element.node?.let { node ->
                        val uuid = thirdPartyGenerator.generateUuid(node, packageName)
                        element.uuid = uuid
                        tempUuidMap[element] = uuid

                        // PHASE 3 (2025-12-08): Track VUID creation
                        metricsCollector.onVUIDCreated()
                    }
                }
                val uuidGenElapsed = System.currentTimeMillis() - uuidGenStartTime

                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine-Perf",
                        "✅ Generated ${tempUuidMap.size} UUIDs in ${uuidGenElapsed}ms (nodes still fresh)")
                }

                // Update progress (before click loop for accurate reporting)
                updateProgress(packageName, depth, explorationResult.screenState.hash)

                // Enhanced visual logging - show what we're seeing
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine-Visual",
                        buildString {
                            appendLine("╔═══════════════════════════════════════════════════════════╗")
                            appendLine("║ SCREEN STATE")
                            appendLine("╠═══════════════════════════════════════════════════════════╣")
                            appendLine("║ Hash: ${explorationResult.screenState.hash.take(16)}...")
                            appendLine("║ Package: ${explorationResult.screenState.packageName}")
                            appendLine("║ Depth: $depth")
                            appendLine("║ Total Elements: ${explorationResult.allElements.size}")
                            appendLine("║ Safe Clickable: ${explorationResult.safeClickableElements.size}")
                            appendLine("║ Dangerous: ${explorationResult.dangerousElements.size}")
                            appendLine("╠═══════════════════════════════════════════════════════════╣")
                            appendLine("║ ELEMENTS DETAIL")
                            appendLine("╠═══════════════════════════════════════════════════════════╣")

                            explorationResult.allElements.take(20).forEachIndexed { i, elem ->
                                val classification = when {
                                    explorationResult.safeClickableElements.contains(elem) -> "✓ SAFE"
                                    explorationResult.dangerousElements.any { it.first == elem } -> "✗ DANGEROUS"
                                    !elem.isClickable -> "○ NON-CLICKABLE"
                                    !elem.isEnabled -> "○ DISABLED"
                                    else -> "○ OTHER"
                                }

                                appendLine("║")
                                appendLine("║ [$i] $classification")
                                appendLine("║     Type: ${elem.className.substringAfterLast('.')}")
                                appendLine("║     Text: \"${elem.text?.take(30) ?: "(none)"}\"")
                                appendLine("║     ContentDesc: \"${elem.contentDescription?.take(30) ?: "(none)"}\"")
                                appendLine("║     Bounds: ${elem.bounds}")
                                appendLine("║     Clickable: ${elem.isClickable}, Enabled: ${elem.isEnabled}")
                                if (elem.uuid != null) {
                                    appendLine("║     UUID: ${elem.uuid?.take(32)}...")
                                }
                            }

                            if (explorationResult.allElements.size > 20) {
                                appendLine("║")
                                appendLine("║ ... and ${explorationResult.allElements.size - 20} more elements")
                            }

                            appendLine("╚═══════════════════════════════════════════════════════════╝")
                        }
                    )
                }

                // 2. Order elements by strategy
                // FIX (2025-11-24): Use mutableListOf to allow refreshing elements after BACK navigation
                val orderedElements = strategy.orderElements(explorationResult.safeClickableElements).toMutableList()

                // 3. Explore each element (DFS)
                // Capture original screen hash for BACK navigation verification
                val originalScreenHash = explorationResult.screenState.hash

                // FIX (2025-11-24): Track current position in orderedElements for proper iteration
                var elementIndex = 0
                while (elementIndex < orderedElements.size) {
                    val element = orderedElements[elementIndex]
                    elementIndex++  // Increment at start so continue statements work correctly

                    // FIX (2025-12-02): Enhanced logging for partial learning diagnosis
                    // Log WHY elements are being skipped (strategy, already clicked, etc.)
                    val elementDesc = element.text ?: element.contentDescription ?: "unknown"
                    val elementType = element.className.substringAfterLast('.')

                    // Check if should explore
                    if (!strategy.shouldExplore(element)) {
                        // PHASE 3 (2025-12-08): Track strategy filtering
                        metricsCollector.onElementFiltered(element, "Strategy rejected")

                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine-Skip",
                                "STRATEGY REJECTED: \"$elementDesc\" ($elementType) - UUID: ${element.uuid}")
                        }
                        continue
                    }

                    // Get UUID from temp map (pre-generated in Step 1)
                    val elementUuid = tempUuidMap[element]
                    if (elementUuid == null) {
                        // PHASE 3 (2025-12-08): Track filtering for elements without UUID
                        metricsCollector.onElementFiltered(element, "No UUID generated")

                        android.util.Log.w("ExplorationEngine-Skip",
                            "NO UUID: \"$elementDesc\" ($elementType) - Skipping")
                        continue
                    }

                    // Check if already clicked (using temp UUID)
                    if (clickTracker.wasElementClicked(explorationResult.screenState.hash, elementUuid)) {
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine-Skip",
                                "ALREADY CLICKED: \"$elementDesc\" ($elementType) - UUID: $elementUuid")
                        }
                        continue
                    }

                    // NEW: Check if expandable control
                    val expansionInfo = expandableDetector.getExpansionInfo(element.node ?: continue)

                    if (expansionInfo.isExpandable &&
                        expansionInfo.confidence >= ExpandableControlDetector.MIN_CONFIDENCE_THRESHOLD) {

                        // Handle expansion (dropdowns, menus, etc.)
                        val expansionHandled = handleExpandableControl(
                            element = element,
                            expansionInfo = expansionInfo,
                            packageName = packageName,
                            screenHash = explorationResult.screenState.hash,
                            depth = depth
                        )

                        if (expansionHandled) {
                            // Expansion was handled, continue to next element
                            continue
                        }
                        // If expansion failed/had no effect, fall through to regular click
                    }

                    // Log which element we're about to click
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-Visual",
                            ">>> CLICKING ELEMENT: \"${element.text ?: element.contentDescription ?: "unknown"}\" " +
                            "(${element.className.substringAfterLast('.')})")
                    }

                    // CRITICAL FIX (VOS-PERF-002): Just-In-Time Node Refresh
                    // Refresh node immediately before click to prevent stale node failures
                    val refreshStartTime = System.currentTimeMillis()
                    val freshNode = refreshAccessibilityNode(element)
                    val refreshElapsed = System.currentTimeMillis() - refreshStartTime

                    if (freshNode == null) {
                        // Fallback: Try coordinate-based click when node refresh fails
                        val centerX = element.bounds.centerX()
                        val centerY = element.bounds.centerY()
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine-Click",
                                "⚡ Node refresh failed, trying coordinate-based click at ($centerX, $centerY) for \"$elementDesc\"")
                        }

                        val gestureSuccess = performCoordinateClick(element.bounds)
                        if (gestureSuccess) {
                            if (developerSettings.isVerboseLoggingEnabled()) {
                                android.util.Log.d("ExplorationEngine-Click",
                                    "✅ Coordinate-based click succeeded for \"$elementDesc\" ($elementType)")
                            }

                            clickTracker.markElementClicked(explorationResult.screenState.hash, elementUuid)
                            // FIX (2025-12-08): Add to cumulative clicked tracking (class-level, survives intent relaunches)
                            cumulativeClickedVuids.add(elementUuid)
                            delay(developerSettings.getScrollDelayMs())
                            continue
                        }

                        android.util.Log.w("ExplorationEngine-Skip",
                            "⚠️ SKIP: Could not refresh node for \"$elementDesc\" ($elementType) - Element may have disappeared")
                        clickFailures.add(ClickFailureReason(elementDesc, elementType, "disappeared", System.currentTimeMillis()))
                        continue
                    }

                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-Perf",
                            "⚡ Node refreshed in ${refreshElapsed}ms for \"$elementDesc\"")
                    }

                    // Regular click with FRESH node (non-expandable or expansion failed)
                    val clicked = clickElement(freshNode, elementDesc, elementType)

                    if (!clicked) {
                        // FIX (VOS-PERF-002): Retry once with completely fresh scrape
                        android.util.Log.w("ExplorationEngine-Click",
                            "⚠️ First click failed for \"$elementDesc\", retrying with fresh scrape...")

                        delay(developerSettings.getScrollDelayMs()) // Let UI settle
                        val retryNode = scrapeElementByBounds(element.bounds)

                        if (retryNode != null) {
                            val retryClicked = clickElement(retryNode, elementDesc, elementType)
                            if (retryClicked) {
                                if (developerSettings.isVerboseLoggingEnabled()) {
                                    android.util.Log.d("ExplorationEngine-Perf",
                                        "✅ CLICK SUCCESS (retry): \"$elementDesc\" ($elementType) - UUID: ${elementUuid.take(8)}...")
                                }
                                // Recycle retry node
                                retryNode.recycle()
                            } else {
                                android.util.Log.w("ExplorationEngine-Skip",
                                    "❌ CLICK FAILED (retry): \"$elementDesc\" ($elementType) - UUID: ${element.uuid}")
                                retryNode.recycle()
                                continue
                            }
                        } else {
                            android.util.Log.w("ExplorationEngine-Skip",
                                "❌ CLICK FAILED: Could not scrape \"$elementDesc\" ($elementType) - UUID: ${element.uuid}")
                            clickFailures.add(ClickFailureReason(elementDesc, elementType, "retry_disappeared", System.currentTimeMillis()))
                            continue
                        }
                    }

                    // Mark element as clicked in tracker (using temp VUID)
                    clickTracker.markElementClicked(explorationResult.screenState.hash, elementUuid)
                    // FIX (2025-12-08): Add to cumulative clicked tracking (class-level, survives intent relaunches)
                    cumulativeClickedVuids.add(elementUuid)

                    // Log click success
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-Perf",
                            "✅ CLICK SUCCESS: \"$elementDesc\" ($elementType) - UUID: ${elementUuid.take(8)}...")
                    }

                    // Wait for screen transition
                    delay(developerSettings.getScreenProcessingDelayMs())

                    // Get new screen
                    val newRootNode = accessibilityService.rootInActiveWindow
                    if (newRootNode == null) {
                        // Backtrack
                        pressBack()
                        delay(developerSettings.getScreenProcessingDelayMs())
                        continue
                    }

                    // PACKAGE NAME VALIDATION: Check if navigation led to foreign app
                    val newPackageName = newRootNode.packageName?.toString()
                    if (newPackageName == null || newPackageName != packageName) {
                        android.util.Log.w("ExplorationEngine",
                            "Navigation led to different package: $newPackageName (expected: $packageName). " +
                            "This is likely BACK to launcher or external app. " +
                            "Recording special navigation edge and attempting BACK to recover.")

                        // Record navigation edge with null destination (indicates "exited app")
                        navigationGraphBuilder.addEdge(
                            fromScreenHash = explorationResult.screenState.hash,
                            clickedElementUuid = elementUuid,
                            toScreenHash = "EXTERNAL_APP"  // Special marker
                        )

                        // Attempt to navigate back to target app
                        var backAttempts = 0
                        val maxBackAttempts = developerSettings.getMaxBackNavigationAttempts()
                        var recovered = false

                        while (backAttempts < maxBackAttempts) {
                            pressBack()
                            delay(developerSettings.getScreenProcessingDelayMs())

                            val currentRootNode = accessibilityService.rootInActiveWindow
                            val currentPackage = currentRootNode?.packageName?.toString()

                            if (currentPackage == packageName) {
                                if (developerSettings.isVerboseLoggingEnabled()) {
                                    android.util.Log.d("ExplorationEngine",
                                        "Successfully recovered to $packageName after ${backAttempts + 1} BACK attempts from $newPackageName")
                                }
                                recovered = true
                                break
                            }

                            backAttempts++
                        }

                        if (!recovered) {
                            android.util.Log.e("ExplorationEngine",
                                "Unable to recover to target package $packageName after $maxBackAttempts BACK attempts. " +
                                "Currently at: ${accessibilityService.rootInActiveWindow?.packageName}. " +
                                "Stopping exploration to prevent registering foreign app elements.")
                            break
                        }

                        // Successfully recovered - continue with next element
                        continue
                    }

                    // Package name matches - proceed with normal exploration
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine",
                            "Package name validated: $actualPackageName matches target $packageName")
                    }

                    // Capture new screen state
                    val newScreenState = screenStateManager.captureScreenState(
                        newRootNode,
                        packageName,
                        depth + 1
                    )

                    // Record navigation edge (using temp UUID)
                    navigationGraphBuilder.addEdge(
                        fromScreenHash = explorationResult.screenState.hash,
                        clickedElementUuid = elementUuid,
                        toScreenHash = newScreenState.hash
                    )

                    // Persist navigation edge to database
                    currentSessionId?.let { sessionId ->
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                            try {
                                repository.saveNavigationEdge(
                                    packageName = packageName,
                                    sessionId = sessionId,
                                    fromScreenHash = explorationResult.screenState.hash,
                                    clickedElementUuid = elementUuid,
                                    toScreenHash = newScreenState.hash
                                )
                            } catch (e: Exception) {
                                android.util.Log.e("ExplorationEngine",
                                    "Failed to persist navigation edge: $elementUuid", e)
                            }
                        }
                    }

                    // Check if screen already visited before recursing
                    if (!screenStateManager.isVisited(newScreenState.hash)) {
                        // Not visited yet - explore it
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine",
                                "Exploring new screen: ${newScreenState.hash} (from element: ${element.text})")
                        }
                        exploreScreenRecursive(newRootNode, packageName, depth + 1)
                    } else {
                        // Already visited - skip content re-exploration
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine",
                                "Screen already explored: ${newScreenState.hash}, " +
                                "skipping re-exploration but recorded navigation edge (element: ${element.text})")
                        }
                    }

                    // Backtrack
                    pressBack()
                    delay(developerSettings.getScreenProcessingDelayMs())

                    // VERIFY BACK navigation (check if we returned to original screen)
                    val currentRootNode = accessibilityService.rootInActiveWindow
                    if (currentRootNode == null) {
                        android.util.Log.w("ExplorationEngine", "Root node null after BACK press")
                        continue
                    }

                    val currentScreenState = screenStateManager.captureScreenState(
                        currentRootNode, packageName, depth
                    )

                    // Check if app is still running
                    if (currentScreenState.packageName != packageName) {
                        android.util.Log.e("ExplorationEngine",
                            "App closed or switched (expected $packageName, got ${currentScreenState.packageName}). Stopping exploration.")
                        break
                    }

                    // Check if screen STRUCTURE is similar (tolerate dynamic content)
                    val isSimilarScreen = screenStateManager.areScreensSimilar(
                        originalScreenHash,
                        currentScreenState.hash,
                        similarityThreshold = developerSettings.getScreenHashSimilarityThreshold().toDouble()
                    )

                    if (!isSimilarScreen) {
                        android.util.Log.w("ExplorationEngine",
                            "BACK navigation anomaly! Expected similar to $originalScreenHash, " +
                            "got ${currentScreenState.hash} (similarity below 85%). " +
                            "This may indicate navigation to unexpected screen.")

                        // Try ONE more BACK press (not two)
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine", "Attempting recovery with single BACK press")
                        }
                        pressBack()
                        delay(developerSettings.getScreenProcessingDelayMs())

                        // Check if we recovered
                        val retryRootNode = accessibilityService.rootInActiveWindow
                        if (retryRootNode != null) {
                            val retryScreenState = screenStateManager.captureScreenState(
                                retryRootNode, packageName, depth
                            )

                            if (retryScreenState.packageName != packageName) {
                                android.util.Log.e("ExplorationEngine", "App closed during recovery. Stopping.")
                                break
                            }

                            val retryIsSimilar = screenStateManager.areScreensSimilar(
                                originalScreenHash,
                                retryScreenState.hash,
                                similarityThreshold = developerSettings.getScreenHashSimilarityThreshold().toDouble()
                            )

                            if (!retryIsSimilar) {
                                android.util.Log.w("ExplorationEngine",
                                    "Unable to recover original screen structure after retry. " +
                                    "Current: ${retryScreenState.hash}. Continuing exploration from current position.")
                                // DON'T break - just log and continue
                                // The navigation graph will track the actual path taken
                            } else {
                                if (developerSettings.isVerboseLoggingEnabled()) {
                                    android.util.Log.d("ExplorationEngine", "Successfully recovered original screen")
                                }
                            }
                        }
                    } else {
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine", "BACK navigation successful - screen similar to original")
                        }
                    }

                    // FIX (2025-11-24): Refresh element nodes after BACK navigation
                    // Issue: AccessibilityNodeInfo references become stale after screen transitions
                    // Root cause: orderedElements list contains nodes from original screen capture
                    // After BACK, those nodes are invalid, causing clickElement() to fail silently
                    // Solution: Re-explore current screen to get fresh nodes for remaining unclicked elements
                    //
                    // This fixes the "4/5 screens" bug where only the first clicked bottom nav tab
                    // is explored, and subsequent tabs are skipped due to stale node references.
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine", "Refreshing element nodes after BACK navigation...")
                    }

                    val refreshedRootNode = accessibilityService.rootInActiveWindow
                    if (refreshedRootNode != null) {
                        val refreshedResult = screenExplorer.exploreScreen(refreshedRootNode, packageName, depth)

                        when (refreshedResult) {
                            is ScreenExplorationResult.Success -> {
                                // Update remaining elements with fresh nodes
                                // Match by UUID to preserve order and click tracking
                                val refreshedMap = refreshedResult.safeClickableElements.associateBy { it.uuid }

                                // Only refresh elements that haven't been processed yet (from elementIndex onwards)
                                val remainingElements = orderedElements.subList(elementIndex, orderedElements.size)
                                val freshRemainingElements = remainingElements.mapNotNull { oldElem ->
                                    oldElem.uuid?.let { uuid -> refreshedMap[uuid] }
                                }

                                // FIX (2025-12-03): Recycle old element nodes before replacing
                                // This prevents memory leaks from stale AccessibilityNodeInfo references
                                remainingElements.forEach { it.recycleNode() }

                                // Remove old remaining elements and add fresh ones
                                // This preserves already-processed elements and click tracking
                                while (orderedElements.size > elementIndex) {
                                    orderedElements.removeAt(orderedElements.size - 1)
                                }
                                orderedElements.addAll(freshRemainingElements)

                                if (developerSettings.isVerboseLoggingEnabled()) {
                                    android.util.Log.d("ExplorationEngine",
                                        "Refreshed ${freshRemainingElements.size} remaining elements with valid nodes " +
                                        "(processed: $elementIndex, remaining: ${orderedElements.size - elementIndex})")
                                }
                            }
                            else -> {
                                android.util.Log.w("ExplorationEngine",
                                    "Failed to refresh elements after BACK - continuing with stale nodes")
                            }
                        }
                    }
                }

                // ═══════════════════════════════════════════════════════
                // POST-CLICKING: Register elements (don't need nodes anymore)
                // ═══════════════════════════════════════════════════════
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine-Perf",
                        "📝 Click loop complete. Registering ${allElementsToRegister.size} elements to database...")
                }

                val registerStartTime = System.currentTimeMillis()
                val elementUuids = registerElements(
                    elements = allElementsToRegister,
                    packageName = packageName
                )
                val registerElapsed = System.currentTimeMillis() - registerStartTime

                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine-Perf",
                        "✅ Registered ${elementUuids.size} elements in ${registerElapsed}ms " +
                        "(deferred registration - nodes not needed)")
                }

                // Calculate element type counts for logging
                val safeCount = explorationResult.safeClickableElements.size
                val dangerousCount = explorationResult.dangerousElements.size
                val otherCount = allElementsToRegister.size - safeCount - dangerousCount

                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine",
                        "Registered ${elementUuids.size} total elements: " +
                        "$safeCount safe clickable, " +
                        "$dangerousCount dangerous (not clicked), " +
                        "$otherCount other (disabled/non-clickable)")
                }

                // Count and log dangerous elements (registered but NOT clicked)
                dangerousElementsSkipped += explorationResult.dangerousElements.size
                explorationResult.dangerousElements.forEach { (element, reason) ->
                    android.util.Log.w("ExplorationEngine",
                        "Registered but NOT clicking dangerous element: '${element.text}' " +
                        "(UUID: ${element.uuid}) - Reason: $reason")
                }

                // Add screen to navigation graph (with ALL elements)
                navigationGraphBuilder.addScreen(
                    screenState = explorationResult.screenState,
                    elementUuids = elementUuids
                )

                // Persist screen state to database
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        repository.saveScreenState(explorationResult.screenState)
                    } catch (e: Exception) {
                        android.util.Log.e("ExplorationEngine",
                            "Failed to persist screen state: ${explorationResult.screenState.hash}", e)
                    }
                }

                // Performance metrics: Click success rate
                val clickedCount = orderedElements.count {
                    clickTracker.wasElementClicked(explorationResult.screenState.hash, it.uuid ?: "")
                }
                val clickSuccessRate = if (orderedElements.isNotEmpty()) {
                    (clickedCount * 100 / orderedElements.size)
                } else {
                    0
                }
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine-Perf",
                        "PERF: element_clicking success=$clickedCount/${orderedElements.size} rate=$clickSuccessRate%")
                }

                // ═══════════════════════════════════════════════════════
                // CLICK TELEMETRY (VOS-PERF-002)
                // ═══════════════════════════════════════════════════════
                if (clickFailures.isNotEmpty()) {
                    val failuresByReason = clickFailures.groupBy { it.reason }

                    android.util.Log.i("ExplorationEngine-Telemetry",
                        buildString {
                            appendLine("╔═══════════════════════════════════════════════════════════╗")
                            appendLine("║ CLICK TELEMETRY (Screen: ${explorationResult.screenState.hash.take(8)}...)")
                            appendLine("╠═══════════════════════════════════════════════════════════╣")
                            appendLine("║ Total Safe Clickable: ${orderedElements.size}")
                            appendLine("║ Successful Clicks: $clickedCount")
                            appendLine("║ Failed Clicks: ${clickFailures.size}")
                            appendLine("║ Success Rate: $clickSuccessRate%")
                            appendLine("╠═══════════════════════════════════════════════════════════╣")
                            appendLine("║ FAILURE BREAKDOWN")
                            appendLine("╠═══════════════════════════════════════════════════════════╣")

                            failuresByReason.entries.sortedByDescending { it.value.size }.forEach { (reason, failures) ->
                                val percentage = (failures.size * 100.0 / clickFailures.size).toInt()
                                appendLine("║ ⚠️  $reason: ${failures.size} ($percentage%)")

                                // Show first 3 examples for this failure reason
                                failures.take(3).forEach { failure ->
                                    appendLine("║     - \"${failure.elementDesc}\" (${failure.elementType})")
                                }
                                if (failures.size > 3) {
                                    appendLine("║     ... and ${failures.size - 3} more")
                                }
                            }

                            appendLine("╚═══════════════════════════════════════════════════════════╝")
                        }
                    )
                }
            }
        }
    }

    /**
     * Register elements with batch deduplication (PERFORMANCE OPTIMIZATION - VOS-PERF-001)
     *
     * **Before (Individual Operations):**
     * - 315 DB operations for 63 elements (5 ops per element: aliasExists check + insert loop)
     * - 1351ms latency
     * - O(N²) deduplication complexity
     *
     * **After (Batch Operations):**
     * - 2 DB operations (1 query for all aliases + 1 batch insert)
     * - <100ms latency (13x+ faster)
     * - O(N) deduplication complexity
     * - **157x reduction in DB operations**
     *
     * **Algorithm:**
     * 1. Generate UUIDs for all elements (no DB)
     * 2. Generate base aliases for all elements (no DB)
     * 3. Batch deduplicate and insert (2 DB ops total via aliasManager.setAliasesBatch)
     * 4. Log only actual deduplications (reduce noise)
     *
     * @param elements List of elements to register
     * @param packageName Target app package
     * @return List of UUIDs
     */
    private suspend fun registerElements(
        elements: List<com.augmentalis.voiceoscore.learnapp.models.ElementInfo>,
        packageName: String,
        registeredUuids: MutableSet<String>? = null
    ): List<String> {
        val startTime = System.currentTimeMillis()

        // PHASE 3 (2025-12-08): Track element detection
        elements.forEach { _ ->
            metricsCollector.onElementDetected()
        }

        // Step 1: Generate UUIDs and register elements (fast, no DB)
        val uuidElementMap = mutableMapOf<String, com.augmentalis.voiceoscore.learnapp.models.ElementInfo>()
        var skippedCount = 0

        for (element in elements) {
            element.node?.let { node ->
                // FIX (2025-12-07): Skip already-registered elements (ViewPager + scroll optimization)
                val stableId = element.stableId()
                if (registeredUuids != null && stableId in registeredUuids) {
                    skippedCount++
                    // PHASE 3 (2025-12-08): Track filtering for already-registered elements
                    metricsCollector.onElementFiltered(element, "Already registered")
                    // Still need to return UUID if element already has one
                    element.uuid?.let { return@let }
                    return@let
                }

                // Generate UUID
                val uuid = thirdPartyGenerator.generateUuid(node, packageName)

                // PHASE 3 (2025-12-08): Track VUID creation
                metricsCollector.onVUIDCreated()

                // Mark as registered
                registeredUuids?.add(stableId)

                // Create UUIDElement
                val uuidElement = UUIDElement(
                    uuid = uuid,
                    name = element.getDisplayName(),
                    type = element.extractElementType(),
                    metadata = UUIDMetadata(
                        attributes = mapOf(
                            "thirdPartyApp" to "true",
                            "packageName" to packageName,
                            "className" to element.className,
                            "resourceId" to element.resourceId
                        ),
                        accessibility = UUIDAccessibility(
                            isClickable = element.isClickable,
                            isFocusable = element.isEnabled
                        )
                    )
                )

                // Register with UUIDCreator (no DB yet)
                uuidCreator.registerElement(uuidElement)

                // Store UUID in element
                element.uuid = uuid
                uuidElementMap[uuid] = element
            }
        }

        // Step 2: Generate base aliases (in parallel, no DB)
        val uuidAliasMap = mutableMapOf<String, String>()
        val hasNoMetadataMap = mutableMapOf<String, Boolean>()

        for ((uuid, element) in uuidElementMap) {
            try {
                val baseAlias = generateAliasFromElement(element)

                // Check if element has no metadata
                val hasNoMetadata = (element.text.isNullOrBlank() &&
                                    element.contentDescription.isNullOrBlank() &&
                                    element.resourceId.isNullOrBlank())

                if (baseAlias.length in 3..50) {
                    uuidAliasMap[uuid] = baseAlias
                    hasNoMetadataMap[uuid] = hasNoMetadata
                } else {
                    android.util.Log.w("ExplorationEngine",
                        "Alias invalid for $uuid: '$baseAlias' (${baseAlias.length} chars)")
                }
            } catch (aliasError: Exception) {
                android.util.Log.w("ExplorationEngine",
                    "Failed to generate alias for $uuid: ${aliasError.message}")
            }
        }

        // Step 3: Batch deduplicate and insert (2 DB ops total)
        var deduplicationCount = 0
        if (uuidAliasMap.isNotEmpty()) {
            try {
                val deduplicatedAliases = aliasManager.setAliasesBatch(uuidAliasMap)

                // Step 4: Log only actual deduplications (reduce noise)
                deduplicatedAliases.forEach { (uuid, actualAlias) ->
                    val baseAlias = uuidAliasMap[uuid]
                    if (actualAlias != baseAlias) {
                        android.util.Log.v("ExplorationEngine",
                            "Deduplicated alias: '$baseAlias' → '$actualAlias'")
                        deduplicationCount++
                    }

                    // Notify user if generic alias was used
                    if (hasNoMetadataMap[uuid] == true) {
                        val element = uuidElementMap[uuid]
                        if (element != null) {
                            notifyUserOfGenericAlias(uuid, actualAlias, element)
                        }
                    }
                }
            } catch (batchError: Exception) {
                android.util.Log.e("ExplorationEngine",
                    "Batch alias registration failed: ${batchError.message}", batchError)
            }
        }

        // Phase 3 (2025-12-04): Generate voice commands using LearnAppCore
        // NEW FEATURE: Exploration mode now generates voice commands (not just UUIDs)
        var commandCount = 0
        if (learnAppCore != null) {
            try {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine",
                        "Generating voice commands for ${elements.size} elements using LearnAppCore")
                }

                for (element in elements) {
                    // Process element with BATCH mode (queues for batch insert)
                    val result = learnAppCore.processElement(
                        element = element,
                        packageName = packageName,
                        mode = ProcessingMode.BATCH  // Exploration uses BATCH mode
                    )

                    if (result.success) {
                        commandCount++
                    } else {
                        // Log failures but don't fail entire exploration
                        android.util.Log.v("ExplorationEngine",
                            "Command generation skipped: ${result.error}")
                    }
                }

                // Flush batch to database (single transaction)
                learnAppCore.flushBatch()

                android.util.Log.i("ExplorationEngine",
                    "Generated $commandCount voice commands via LearnAppCore")
            } catch (commandError: Exception) {
                android.util.Log.e("ExplorationEngine",
                    "Voice command generation failed: ${commandError.message}", commandError)
            }
        } else {
            android.util.Log.w("ExplorationEngine",
                "LearnAppCore not provided - voice commands will NOT be generated (legacy mode)")
        }

        // Performance metrics
        val elapsedMs = System.currentTimeMillis() - startTime
        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d("ExplorationEngine-Perf",
                "PERF: element_registration duration_ms=$elapsedMs elements=${elements.size} " +
                "skipped=$skippedCount commands=$commandCount deduplications=$deduplicationCount rate=${if (elapsedMs > 0) elements.size * 1000 / elapsedMs else 0}/sec")
        }

        // FIX (2025-12-07): Log when elements were skipped due to prior registration
        if (skippedCount > 0) {
            android.util.Log.i("ExplorationEngine",
                "⏭️ Skipped $skippedCount already-registered elements (ViewPager/scroll optimization)")
        }

        return uuidElementMap.keys.toList()
    }

    /**
     * Handles expansion of expandable controls (dropdowns, menus, drawers).
     *
     * This method implements the expansion strategy for UI controls that hide child elements:
     * 1. Captures state before expansion (windows, element count)
     * 2. Clicks the expandable control
     * 3. Waits for expansion animation (500ms)
     * 4. Detects what changed (new window, new elements, navigation)
     * 5. Explores newly revealed content
     *
     * ## Expansion Detection Strategy
     *
     * ### Primary: New Overlay Window (Most Common)
     * - Dropdowns (Spinner) create TYPE_APPLICATION_OVERLAY window
     * - Overflow menus create overlay with menu items
     * - Detection: Compare windowManager.getAppWindows() before/after
     *
     * ### Secondary: In-Place Expansion
     * - Accordion lists expand within same window
     * - ExpandableListView reveals child items
     * - Detection: Compare element count before/after
     *
     * ### Tertiary: Navigation
     * - Some "expandables" navigate to new screen (drawer → activity)
     * - Detection: Package stays same, but major screen hash change
     * - Handling: Normal screen exploration handles this automatically
     *
     * @param element The expandable control element to expand
     * @param expansionInfo Detailed information about expansion type and confidence
     * @param packageName Package name of the app
     * @param screenHash Current screen hash
     * @param depth Current exploration depth
     * @return true if expansion was handled, false if should treat as regular click
     */
    @Suppress("UNNECESSARY_SAFE_CALL")  // Element properties can be null at runtime despite type inference
    private suspend fun handleExpandableControl(
        element: com.augmentalis.voiceoscore.learnapp.models.ElementInfo,
        expansionInfo: ExpandableControlDetector.ExpansionInfo,
        packageName: String,
        screenHash: String,
        depth: Int
    ): Boolean {
        android.util.Log.i("ExplorationEngine", "📋 Expandable control detected: ${element.text ?: element.contentDescription ?: "unknown"}")
        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d("ExplorationEngine", "   Type: ${expansionInfo.expansionType}, Confidence: ${expansionInfo.confidence}")
            android.util.Log.d("ExplorationEngine", "   Reason: ${expansionInfo.reason}")
        }

        try {
            // STEP 1: Capture state BEFORE expansion
            val beforeWindows = windowManager.getAppWindows(packageName, launcherDetector)
            val beforeWindowCount = beforeWindows.size

            android.util.Log.v("ExplorationEngine", "   Before expansion: $beforeWindowCount windows")

            // STEP 2: Click to expand
            val clicked = clickElement(element.node)
            if (!clicked) {
                android.util.Log.w("ExplorationEngine", "   Failed to click expandable control")
                return false
            }

            // Mark element as clicked in tracker
            clickTracker.markElementClicked(screenHash, element.uuid ?: return false)

            // STEP 3: Wait for expansion animation
            delay(developerSettings.getExpansionWaitDelayMs())

            // STEP 4: Capture state AFTER expansion
            val afterWindows = windowManager.getAppWindows(packageName, launcherDetector)
            val afterWindowCount = afterWindows.size

            android.util.Log.v("ExplorationEngine", "   After expansion: $afterWindowCount windows")

            // STEP 5: Detect what changed and handle accordingly

            // Case A: New overlay window appeared (most common)
            if (afterWindowCount > beforeWindowCount) {
                val newWindows = afterWindows.filter { afterWindow ->
                    beforeWindows.none { beforeWindow ->
                        beforeWindow.window.hashCode() == afterWindow.window.hashCode()
                    }
                }

                android.util.Log.i("ExplorationEngine", "✅ Expansion created ${newWindows.size} overlay window(s)")

                for (overlayWindow in newWindows) {
                    if (overlayWindow.type == WindowManager.WindowType.OVERLAY ||
                        overlayWindow.type == WindowManager.WindowType.DIALOG) {

                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine", "   Exploring overlay: ${overlayWindow.toLogString()}")
                        }

                        // Explore the overlay window content
                        exploreWindow(overlayWindow, packageName, depth + 1)

                        // Close overlay (press BACK to dismiss menu)
                        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                        delay(developerSettings.getClickDelayMs()) // Wait for overlay to close
                    }
                }

                return true // Expansion handled
            }

            // Case B: Same window, but new elements appeared (in-place expansion)
            // Note: We don't have beforeElementCount from the integration plan, so we'll
            // check if the current screen has changed using screen state manager
            else {
                val currentRootNode = accessibilityService.rootInActiveWindow
                if (currentRootNode != null) {
                    val currentScreenState = screenStateManager.captureScreenState(
                        currentRootNode, packageName, depth
                    )

                    // Check if screen hash changed (navigation occurred)
                    if (currentScreenState.hash != screenHash) {
                        android.util.Log.i("ExplorationEngine", "✅ Expansion caused navigation to new screen")
                        // Normal exploration flow will handle the new screen
                        return true
                    }
                }

                // Case C: No visible effect (disabled, already expanded, or animation too slow)
                android.util.Log.w("ExplorationEngine", "⚠️ Expansion had no detectable effect")
                android.util.Log.w("ExplorationEngine", "   Possible causes: disabled, already expanded, slow animation (>500ms)")
                // Treat as regular click, continue exploration
                return false
            }

        } catch (e: Exception) {
            android.util.Log.e("ExplorationEngine", "❌ Error handling expandable control: ${element.text ?: "unknown"}", e)
            // Don't fail exploration on expansion errors
            return false
        }
    }

    /**
     * Explores content within a specific window.
     *
     * Helper method for exploring overlay windows created by expandable controls.
     *
     * @param window WindowInfo to explore
     * @param packageName Package name
     * @param depth Exploration depth
     */
    @Suppress("UNNECESSARY_SAFE_CALL")  // Element properties can be null at runtime despite type inference
    private suspend fun exploreWindow(
        window: WindowInfo,
        packageName: String,
        depth: Int
    ) {
        val windowNode = window.rootNode ?: return

        // Collect clickable elements in this window
        val explorationResult = screenExplorer.exploreScreen(windowNode, packageName, depth)

        when (explorationResult) {
            is ScreenExplorationResult.Success -> {
                // Track scrollable containers
                scrollableContainersFound += explorationResult.scrollableContainerCount

                val elements = explorationResult.safeClickableElements

                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine", "   Window contains ${elements.size} clickable element(s)")
                }

                // Register elements in database
                registerElements(elements, packageName)

                // Log discovered elements
                if (developerSettings.isVerboseLoggingEnabled()) {
                    elements.forEach { element ->
                        android.util.Log.d("ExplorationEngine", "      - ${element.text ?: element.contentDescription ?: "unknown"} (${element.className.substringAfterLast('.')})")
                    }
                }

                // For menu items, just register, don't click recursively
                // (clicking menu items would trigger their actions, which we want user to control)
                // User will interact with them naturally during usage
            }
            else -> {
                android.util.Log.v("ExplorationEngine", "   Overlay window exploration skipped or failed")
            }
        }
    }

    /**
     * Generate alias from element with fallbacks
     *
     * Tries multiple sources to create a valid alias:
     * 1. Element text
     * 2. Content description
     * 3. Resource ID (last component)
     * 4. Fallback: "element_${className}"
     *
     * Sanitizes the result and ensures 3-50 character length.
     *
     * @param element Element to generate alias for
     * @return Generated alias (3-50 characters)
     */
    @Suppress("UNNECESSARY_SAFE_CALL")  // Element properties can be null at runtime despite type inference
    private fun generateAliasFromElement(element: com.augmentalis.voiceoscore.learnapp.models.ElementInfo): String {
        // Try text first
        val textAlias = element.text?.trim()?.takeIf { it.isNotEmpty() }
        if (textAlias != null && textAlias.length >= developerSettings.getMinAliasTextLength()) {
            return sanitizeAlias(textAlias)
        }

        // Try content description
        val contentDesc = element.contentDescription?.trim()?.takeIf { it.isNotEmpty() }
        if (contentDesc != null && contentDesc.length >= developerSettings.getMinAliasTextLength()) {
            return sanitizeAlias(contentDesc)
        }

        // Try resource ID (last component)
        val resourceId = element.resourceId?.substringAfterLast('/')?.trim()?.takeIf { it.isNotEmpty() }
        if (resourceId != null && resourceId.length >= developerSettings.getMinAliasTextLength()) {
            return sanitizeAlias(resourceId)
        }

        // Fallback: use className with number
        val className = element.className.substringAfterLast('.').takeIf { it.isNotEmpty() } ?: "unknown"
        val elementType = className.lowercase().replace("view", "").replace("widget", "")

        // Get next number for this element type
        val counter = genericAliasCounters.getOrPut(elementType) { 0 } + 1
        genericAliasCounters[elementType] = counter

        // Create numbered alias
        val numberedAlias = "${elementType}_$counter"

        android.util.Log.w("ExplorationEngine",
            "Element has no metadata (text/contentDesc/resourceId). " +
            "Assigned generic alias: $numberedAlias for ${element.className}")

        return sanitizeAlias(numberedAlias)
    }

    /**
     * Sanitize alias to valid format
     *
     * AliasManager Requirements:
     * - Must start with a letter (a-z)
     * - Must contain only lowercase alphanumeric + underscores
     * - No hyphens allowed
     * - Length 3-50 characters
     *
     * @param alias Raw alias string
     * @return Sanitized alias meeting all AliasManager requirements
     */
    private fun sanitizeAlias(alias: String): String {
        // 1. Convert to lowercase
        var sanitized = alias.lowercase()

        // 2. Replace invalid characters (including hyphens) with underscores
        sanitized = sanitized.replace(Regex("[^a-z0-9_]"), "_")

        // 3. Collapse multiple underscores
        sanitized = sanitized.replace(Regex("_+"), "_")

        // 4. Remove leading/trailing underscores
        sanitized = sanitized.trim('_')

        // 5. Ensure starts with letter (prepend "elem_" if needed)
        if (sanitized.isEmpty() || !sanitized[0].isLetter()) {
            sanitized = "elem_$sanitized"
        }

        // 6. Ensure minimum 3 characters
        if (sanitized.length < 3) {
            sanitized = sanitized.padEnd(3, 'x')
        }

        // 7. Truncate to 50 characters
        if (sanitized.length > 50) {
            sanitized = sanitized.substring(0, 50)
        }

        // 8. Final validation - ensure still starts with letter after truncation
        if (!sanitized[0].isLetter()) {
            sanitized = "elem" + sanitized.substring(4)
        }

        return sanitized
    }

    /**
     * Refresh an AccessibilityNodeInfo by re-scraping at the same bounds.
     *
     * CRITICAL FIX (VOS-PERF-002): Just-In-Time Node Refresh
     *
     * This function addresses the 92% click failure rate by refreshing stale nodes
     * immediately before clicking. AccessibilityNodeInfo references become invalid
     * within 500ms, so we re-scrape the UI tree to get a fresh reference.
     *
     * Performance: 5-10ms (vs 439ms for UUID generation batch)
     *
     * @param element Element whose node needs refreshing
     * @return Fresh AccessibilityNodeInfo or null if element no longer exists
     */
    private fun refreshAccessibilityNode(element: com.augmentalis.voiceoscore.learnapp.models.ElementInfo): AccessibilityNodeInfo? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return null
            val result = findNodeByBounds(rootNode, element.bounds)

            // Don't recycle rootNode here - findNodeByBounds returns a child node or rootNode itself
            // Only recycle if findNodeByBounds returns null (no match found)
            if (result == null) {
                rootNode.recycle()
            }

            result
        } catch (e: Exception) {
            android.util.Log.w("ExplorationEngine", "Failed to refresh node: ${e.message}")
            null
        }
    }

    /**
     * Find node at specific bounds coordinates by traversing UI tree.
     *
     * FIX (2025-12-04): Added fuzzy bounds matching with tolerance.
     *
     * Problem: Exact bounds matching (`bounds == targetBounds`) failed when UI
     * shifted by even 1 pixel after interactions, causing 93% of element clicks to fail.
     *
     * Solution: Use fuzzy bounds matching with configurable tolerance (default ±5px).
     * Falls back to exact match if no fuzzy match found within tolerance.
     *
     * @param root Root node to start search from
     * @param targetBounds Target bounds to match
     * @param tolerance Maximum pixel difference allowed (default: 5px)
     * @return Matching node or null if not found
     */
    private fun findNodeByBounds(
        root: AccessibilityNodeInfo,
        targetBounds: Rect,
        tolerance: Int = 5
    ): AccessibilityNodeInfo? {
        val bounds = Rect()
        root.getBoundsInScreen(bounds)

        // Check if this node matches (exact match first for performance)
        if (bounds == targetBounds) {
            return root
        }

        // Check fuzzy match within tolerance
        if (tolerance > 0 && areBoundsSimilar(bounds, targetBounds, tolerance)) {
            return root
        }

        // Search children recursively
        // First pass: look for exact matches
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            val childBounds = Rect()
            child.getBoundsInScreen(childBounds)

            if (childBounds == targetBounds) {
                return child
            }

            val result = findNodeByBounds(child, targetBounds, tolerance)
            if (result != null) {
                return result
            }
            // Recycle child only if it didn't match
            child.recycle()
        }

        return null
    }

    /**
     * Scrape element by bounds as last resort when refresh fails.
     *
     * This is a fallback for when findNodeByBounds fails to locate an element.
     * We get a fresh root node and try one more time.
     *
     * @param bounds Target bounds to scrape
     * @return Fresh node or null if element no longer exists
     */
    private fun scrapeElementByBounds(bounds: Rect): AccessibilityNodeInfo? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow ?: return null
            val result = findNodeByBounds(rootNode, bounds)

            // Recycle rootNode if no match found
            if (result == null) {
                rootNode.recycle()
            }

            result
        } catch (e: Exception) {
            android.util.Log.w("ExplorationEngine", "Failed to scrape element by bounds: ${e.message}")
            null
        }
    }

    /**
     * Click element with enhanced retry logic and validation.
     *
     * IMPROVED (VOS-PERF-002): Enhanced Click Success Rate
     *
     * Enhancements:
     * 1. Verify element is visible and enabled before clicking
     * 2. Check element is on screen, scroll into view if needed
     * 3. Retry with exponential backoff (500ms, 1000ms)
     * 4. Track failure reasons for telemetry
     *
     * Expected: Click success rate 50% → 95%+
     *
     * @param node Node to click
     * @param elementDesc Element description for logging (optional)
     * @param elementType Element type for logging (optional)
     * @return true if clicked successfully
     */
    private suspend fun clickElement(
        node: AccessibilityNodeInfo?,
        elementDesc: String? = null,
        elementType: String? = null
    ): Boolean {
        if (node == null) return false

        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            try {
                // 1. Verify element is visible and enabled
                if (!node.isVisibleToUser) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-Click", "⚠️ Element not visible to user")
                    }
                    if (elementDesc != null && elementType != null) {
                        clickFailures.add(ClickFailureReason(elementDesc, elementType, "not_visible", System.currentTimeMillis()))
                    }
                    return@withContext false
                }

                if (!node.isEnabled) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-Click", "⚠️ Element not enabled")
                    }
                    if (elementDesc != null && elementType != null) {
                        clickFailures.add(ClickFailureReason(elementDesc, elementType, "not_enabled", System.currentTimeMillis()))
                    }
                    return@withContext false
                }

                // 2. Get bounds and verify on screen
                val bounds = Rect()
                node.getBoundsInScreen(bounds)
                val displayMetrics = context.resources.displayMetrics
                val screenBounds = Rect(0, 0, displayMetrics.widthPixels, displayMetrics.heightPixels)

                if (!screenBounds.contains(bounds)) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-Click", "⚠️ Element not on screen, attempting scroll")
                    }
                    // Try to scroll into view
                    val scrolled = node.performAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)
                    if (!scrolled) {
                        if (elementDesc != null && elementType != null) {
                            clickFailures.add(ClickFailureReason(elementDesc, elementType, "scroll_failed", System.currentTimeMillis()))
                        }
                        return@withContext false
                    }
                    delay(developerSettings.getClickDelayMs())
                }

                // 3. Attempt click with retry
                var attempts = 0
                var success = false

                while (attempts < 3 && !success) {
                    success = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)

                    if (!success) {
                        delay(developerSettings.getScrollDelayMs() * (attempts + 1)) // Exponential backoff
                        attempts++
                        if (developerSettings.isVerboseLoggingEnabled()) {
                            android.util.Log.d("ExplorationEngine-Click",
                                "⚠️ Click attempt ${attempts} failed, retrying...")
                        }
                    }
                }

                if (success) {
                    if (developerSettings.isVerboseLoggingEnabled()) {
                        android.util.Log.d("ExplorationEngine-Click", "✅ Click succeeded on attempt ${attempts + 1}")
                    }
                } else {
                    android.util.Log.w("ExplorationEngine-Click", "❌ Click failed after 3 attempts")
                    if (elementDesc != null && elementType != null) {
                        clickFailures.add(ClickFailureReason(elementDesc, elementType, "action_failed", System.currentTimeMillis()))
                    }
                }

                return@withContext success

            } catch (e: Exception) {
                android.util.Log.e("ExplorationEngine-Click", "Click failed with exception: ${e.message}", e)
                if (elementDesc != null && elementType != null) {
                    clickFailures.add(ClickFailureReason(elementDesc, elementType, "exception:${e.message}", System.currentTimeMillis()))
                }
                return@withContext false
            }
        }
    }

    /**
     * Press back button
     */
    private suspend fun pressBack() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Perform coordinate-based click using gesture dispatch
     *
     * This is used as a fallback when AccessibilityNodeInfo cannot be refreshed,
     * but we still have the element's bounds from the original scrape.
     *
     * @param bounds The bounds of the element to click
     * @return true if gesture was dispatched successfully, false otherwise
     */
    private fun performCoordinateClick(bounds: Rect): Boolean {
        val centerX = bounds.centerX().toFloat()
        val centerY = bounds.centerY().toFloat()

        val clickPath = android.graphics.Path().apply {
            moveTo(centerX, centerY)
        }

        val gestureDescription = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(android.accessibilityservice.GestureDescription.StrokeDescription(
                clickPath, 0, 50  // 50ms tap duration
            ))
            .build()

        return try {
            accessibilityService.dispatchGesture(
                gestureDescription,
                null,  // callback
                null   // handler
            )
        } catch (e: Exception) {
            android.util.Log.e("ExplorationEngine-Click",
                "Coordinate-based click failed: ${e.message}", e)
            false
        }
    }

    /**
     * Wait for screen change (e.g., after login)
     *
     * FIX: Increased timeout from 1 minute to 10 minutes to allow users time to:
     * - Enter email/username
     * - Enter password
     * - Handle 2FA/OTP
     * - Deal with captchas
     * - Handle password manager popups
     *
     * This fixes Glovius app exiting prematurely on login screen.
     *
     * @param previousHash Previous screen hash
     */
    private suspend fun waitForScreenChange(previousHash: String) {
        val timeout = developerSettings.getLoginTimeoutMs()
        val startTime = System.currentTimeMillis()

        android.util.Log.i("ExplorationEngine",
            "Waiting for screen change (login). Timeout: ${timeout / 1000 / 60} minutes. " +
            "Take your time to enter credentials, handle 2FA, etc.")

        while (System.currentTimeMillis() - startTime < timeout) {
            val currentHash = screenStateManager.getCurrentScreenHash()
            if (currentHash != null && currentHash != previousHash) {
                android.util.Log.i("ExplorationEngine",
                    "Screen changed detected! Resuming exploration...")
                return
            }

            delay(developerSettings.getScrollDelayMs())
        }

        android.util.Log.w("ExplorationEngine",
            "Login wait timeout (10 minutes). Screen did not change. " +
            "Resuming exploration anyway - user may have dismissed login or app may be stuck.")
    }

    /**
     * Wait for resume (when paused)
     */
    private suspend fun waitForResume() {
        while (_explorationState.value is ExplorationState.PausedByUser) {
            delay(developerSettings.getClickRetryDelayMs())
        }
    }

    /**
     * Update progress
     *
     * @param packageName Package name
     * @param depth Current depth
     * @param currentScreenHash Current screen hash
     */
    private fun updateProgress(packageName: String, depth: Int, @Suppress("UNUSED_PARAMETER") currentScreenHash: String) {
        val stats = screenStateManager.getStats()
        val elapsed = System.currentTimeMillis() - startTimestamp

        val progress = ExplorationProgress(
            appName = packageName,
            screensExplored = stats.totalScreensVisited,
            estimatedTotalScreens = maxOf(20, stats.totalScreensDiscovered + 10),
            elementsDiscovered = if (::navigationGraphBuilder.isInitialized) navigationGraphBuilder.getNodeCount() else 0,
            currentDepth = depth,
            currentScreen = "Screen ${stats.totalScreensVisited}",
            elapsedTimeMs = elapsed
        )

        _explorationState.value = ExplorationState.Running(
            packageName = packageName,
            progress = progress
        )

        // DEBUG (2025-12-08): Fire progress callback for debug overlay
        val progressPercent = if (progress.estimatedTotalScreens > 0) {
            ((progress.screensExplored.toFloat() / progress.estimatedTotalScreens) * 100).toInt()
                .coerceIn(0, 100)
        } else 0
        debugCallback?.onProgressUpdated(progressPercent)
    }

    /**
     * Get current progress
     *
     * @param packageName Package name
     * @param depth Depth
     * @return Progress
     */
    private fun getCurrentProgress(packageName: String, depth: Int): ExplorationProgress {
        val stats = screenStateManager.getStats()
        val elapsed = System.currentTimeMillis() - startTimestamp

        return ExplorationProgress(
            appName = packageName,
            screensExplored = stats.totalScreensVisited,
            estimatedTotalScreens = maxOf(20, stats.totalScreensDiscovered + 10),
            elementsDiscovered = if (::navigationGraphBuilder.isInitialized) navigationGraphBuilder.getNodeCount() else 0,
            currentDepth = depth,
            currentScreen = "Screen ${stats.totalScreensVisited}",
            elapsedTimeMs = elapsed
        )
    }

    /**
     * Create exploration stats
     *
     * @param packageName Package name
     * @return Stats
     */
    private suspend fun createExplorationStats(packageName: String): ExplorationStats {
        val stats = screenStateManager.getStats()
        val graph = if (::navigationGraphBuilder.isInitialized) {
            navigationGraphBuilder.build()
        } else {
            // Return empty graph if builder not initialized
            android.util.Log.w("ExplorationEngine", "navigationGraphBuilder not initialized, using empty graph")
            NavigationGraphBuilder(packageName).build()
        }
        val graphStats = graph.getStats()
        val elapsed = System.currentTimeMillis() - startTimestamp

        // FIX (2025-12-08): Use cumulative tracking for completeness instead of clickTracker
        // clickTracker may have been cleared during intent relaunches
        //
        // UPDATE (2025-12-08): Calculate blocked vs non-blocked for stats display
        val statsNonBlockedCount = cumulativeDiscoveredVuids.size
        val statsClickedCount = cumulativeClickedVuids.size
        val statsBlockedCount = cumulativeBlockedVuids.size

        val cumulativeCompleteness = if (statsNonBlockedCount > 0) {
            (statsClickedCount.toFloat() / statsNonBlockedCount.toFloat()) * 100f
        } else {
            0f
        }

        // Generate AI context from navigation graph
        try {
            val aiSerializer = com.augmentalis.voiceoscore.learnapp.ai.AIContextSerializer(context, databaseManager)
            val aiContext = aiSerializer.generateContext(graph)

            // Log JSON format
            val jsonContext = aiSerializer.toJSON(aiContext)
            android.util.Log.i("ExplorationEngine", "AI Context Generated (${aiContext.screens.size} screens, ${aiContext.stats.totalElements} elements)")
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine", "AI Context JSON:\n$jsonContext")
            }

            // Save to .vos file
            val vosFile = aiSerializer.saveToFile(aiContext)
            if (vosFile != null) {
                android.util.Log.i("ExplorationEngine", "✅ AI context saved to: ${vosFile.absolutePath}")
            } else {
                android.util.Log.w("ExplorationEngine", "⚠️ Failed to save AI context to file")
            }

            // Example: Generate LLM prompt for a hypothetical goal
            val examplePrompt = aiSerializer.toLLMPrompt(aiContext, "Open app settings")
            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine", "Example LLM Prompt:\n$examplePrompt")
            }
        } catch (e: Exception) {
            android.util.Log.e("ExplorationEngine", "Failed to generate AI context", e)
            // Don't fail exploration if AI context generation fails
        }

        return ExplorationStats(
            packageName = packageName,
            appName = packageName,
            totalScreens = stats.totalScreensDiscovered,
            totalElements = graphStats.totalElements,  // FIXED: Use totalElements instead of totalScreens
            totalEdges = graphStats.totalEdges,
            durationMs = elapsed,
            maxDepth = graphStats.maxDepth,
            dangerousElementsSkipped = dangerousElementsSkipped,
            loginScreensDetected = loginScreensDetected,
            scrollableContainersFound = scrollableContainersFound,
            completeness = cumulativeCompleteness,  // FIX (2025-12-08): Use cumulative tracking for accurate completeness
            // UPDATE (2025-12-08): Blocked vs non-blocked tracking for stats display
            clickedElements = statsClickedCount,
            nonBlockedElements = statsNonBlockedCount,
            blockedElements = statsBlockedCount
        )
    }

    /**
     * Pause exploration
     */
    fun pauseExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            _explorationState.value = ExplorationState.PausedByUser(
                packageName = currentState.packageName,
                progress = currentState.progress
            )
        }
    }

    /**
     * Resume exploration
     */
    fun resumeExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.PausedByUser) {
            _explorationState.value = ExplorationState.Running(
                packageName = currentState.packageName,
                progress = currentState.progress
            )
        }
    }

    /**
     * Stop exploration
     *
     * FIX (2025-12-03): Cancel all child coroutines to prevent memory leaks
     *
     * Issue: Coroutines kept running after exploration stopped, holding references
     * to AccessibilityNodeInfo and other resources, causing memory leaks in
     * long-running accessibility service.
     *
     * Solution: Cancel all child coroutines and cleanup resources
     */
    fun stopExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            // Cancel all running child coroutines
            scope.coroutineContext[kotlinx.coroutines.Job]?.cancelChildren()

            android.util.Log.i("ExplorationEngine",
                "Stopping exploration - canceling all child coroutines")

            scope.launch {
                try {
                    val stats = createExplorationStats(currentState.packageName)
                    _explorationState.value = ExplorationState.Completed(
                        packageName = currentState.packageName,
                        stats = stats
                    )

                    // PHASE 3 (2025-12-08): Generate and save VUID metrics
                    try {
                        val metrics = metricsCollector.buildMetrics(currentState.packageName)

                        // Log final report
                        android.util.Log.i("ExplorationEngine", "=== VUID CREATION METRICS ===")
                        android.util.Log.i("ExplorationEngine", metrics.toReportString())

                        // Save to database
                        metricsRepository.saveMetrics(metrics)
                        android.util.Log.d("ExplorationEngine", "Metrics saved to database")
                    } catch (e: Exception) {
                        android.util.Log.e("ExplorationEngine", "Failed to save metrics", e)
                    }

                    // Hide debug overlay
                    debugOverlay.hide()

                    // Cleanup resources
                    cleanup()

                    android.util.Log.i("ExplorationEngine",
                        "Exploration stopped successfully. Stats: $stats")
                } catch (e: Exception) {
                    android.util.Log.e("ExplorationEngine",
                        "Error during exploration stop", e)
                }
            }
        }
    }

    /**
     * Pause exploration (Phase 2: Pause/Resume)
     *
     * Pauses the exploration loop without losing the DFS stack state.
     * The exploration can be resumed from the exact point where it was paused.
     *
     * @param reason Reason for pausing (e.g., "User paused", "Permission required")
     */
    suspend fun pause(reason: String = "User paused") {
        android.util.Log.i("ExplorationEngine", "⏸️ Pausing exploration: $reason")

        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            // Determine pause type based on reason
            _pauseState.value = if (reason.contains("User", ignoreCase = true)) {
                ExplorationPauseState.PAUSED_BY_USER
            } else {
                ExplorationPauseState.PAUSED_AUTO
            }

            // Emit paused exploration state
            _explorationState.value = ExplorationState.Paused(
                packageName = currentState.packageName,
                progress = currentState.progress,
                reason = reason
            )

            // Persist pause state to database
            currentSessionId?.let { sessionId ->
                try {
                    repository.savePauseState(currentState.packageName, _pauseState.value.name)
                    android.util.Log.i("ExplorationEngine", "✅ Pause state persisted to database")
                } catch (e: Exception) {
                    android.util.Log.e("ExplorationEngine", "Failed to persist pause state", e)
                }
            }
        } else {
            android.util.Log.w("ExplorationEngine", "Cannot pause - exploration not running")
        }
    }

    /**
     * Resume exploration (Phase 2: Pause/Resume)
     *
     * Resumes exploration from the point where it was paused.
     * The DFS stack is preserved, so exploration continues seamlessly.
     */
    suspend fun resume() {
        android.util.Log.i("ExplorationEngine", "▶️ Resuming exploration")

        val currentState = _explorationState.value
        if (currentState is ExplorationState.Paused) {
            // Update pause state to RUNNING
            _pauseState.value = ExplorationPauseState.RUNNING

            // Restore running state
            _explorationState.value = ExplorationState.Running(
                packageName = currentState.packageName,
                progress = currentState.progress
            )

            // Persist resume state to database
            try {
                repository.savePauseState(currentState.packageName, ExplorationPauseState.RUNNING.name)
                android.util.Log.i("ExplorationEngine", "✅ Resume state persisted to database")
            } catch (e: Exception) {
                android.util.Log.e("ExplorationEngine", "Failed to persist resume state", e)
            }
        } else {
            android.util.Log.w("ExplorationEngine", "Cannot resume - exploration not paused")
        }
    }

    /**
     * Check if exploration is currently paused (Phase 2: Pause/Resume)
     *
     * @return true if paused, false if running
     */
    fun isPaused(): Boolean = _pauseState.value != ExplorationPauseState.RUNNING

    /**
     * Cleanup resources
     *
     * FIX (2025-12-03): Nullify all references to prevent memory leaks
     *
     * This method should be called:
     * 1. After exploration stops (stopExploration)
     * 2. When service is destroyed (VoiceOSService.onDestroy)
     * 3. Before starting new exploration (startExploration)
     */
    private suspend fun cleanup() {
        if (developerSettings.isVerboseLoggingEnabled()) {
            android.util.Log.d("ExplorationEngine", "Cleaning up resources...")
        }

        // PHASE 3 (2025-12-08): Hide debug overlay
        debugOverlay.hide()

        // Clear screen state manager (releases AccessibilityNodeInfo references)
        screenStateManager.clear()

        // Clear click tracker
        clickTracker.clear()

        // Clear navigation graph
        // FIX (2025-12-04): Don't re-initialize navigationGraphBuilder here.
        // Calling build().packageName on an empty/stale builder crashes.
        // startExploration() already re-initializes it at line 263:
        //   navigationGraphBuilder = NavigationGraphBuilder(packageName)
        // So we just leave it alone here - it will be overwritten anyway.

        // Clear generic alias counters
        genericAliasCounters.clear()

        android.util.Log.i("ExplorationEngine", "Resource cleanup complete")
    }

    /**
     * Notify user to enter credentials on login screen
     *
     * Creates a notification and plays a sound to alert the user that
     * manual credential input is required. This is a privacy-preserving
     * approach - we register element structures but DO NOT capture
     * actual passwords or email values entered by the user.
     *
     * @param packageName Package name of the app with login screen
     */
    private fun notifyUserForLoginScreen(packageName: String) {
        try {
            val notificationManager = accessibilityService.getSystemService(Context.NOTIFICATION_SERVICE)
                as? NotificationManager ?: return

            // Create notification channel (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "LearnApp Exploration",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for app exploration events"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Create notification
            val notification = NotificationCompat.Builder(accessibilityService, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Login Screen Detected")
                .setContentText("Please enter credentials for $packageName. Exploration will resume after login.")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("LearnApp has detected a login screen in $packageName. " +
                            "Please manually enter your credentials. " +
                            "NOTE: Only element structures are saved - your password and email values are NOT captured. " +
                            "Exploration will automatically resume when the screen changes."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(longArrayOf(0, 500, 250, 500))
                .build()

            notificationManager.notify(LOGIN_NOTIFICATION_ID, notification)

            // Play notification sound
            try {
                val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 200)
                scope.launch {
                    delay(developerSettings.getClickRetryDelayMs())
                    toneGenerator.release()
                }
            } catch (soundError: Exception) {
                android.util.Log.w("ExplorationEngine",
                    "Failed to play notification sound: ${soundError.message}")
            }

            android.util.Log.i("ExplorationEngine",
                "User notified for login screen: $packageName (notification + sound)")

        } catch (e: Exception) {
            android.util.Log.e("ExplorationEngine",
                "Failed to notify user for login screen: ${e.message}", e)
        }
    }

    /**
     * Notify user that element has no metadata and generic alias was assigned
     *
     * Shows notification allowing user to customize the alias via voice command.
     */
    private fun notifyUserOfGenericAlias(uuid: String, genericAlias: String, element: com.augmentalis.voiceoscore.learnapp.models.ElementInfo) {
        try {
            val notificationManager = accessibilityService.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            // Create notification channel if needed
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_CHANNEL_ID,
                    "LearnApp Exploration",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications about app exploration and element learning"
                    enableVibration(false)
                    setSound(null, null)
                }
                notificationManager.createNotificationChannel(channel)
            }

            val notification = NotificationCompat.Builder(accessibilityService, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Unnamed Element Found")
                .setContentText("${element.className.substringAfterLast('.')} has no label. Voice command: \"$genericAlias\"")
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText("Element Type: ${element.className.substringAfterLast('.')}\n" +
                            "Assigned Name: \"$genericAlias\"\n" +
                            "Position: ${element.bounds}\n\n" +
                            "You can customize this later in Settings."))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            // Use UUID hash as notification ID to avoid duplicates
            notificationManager.notify(uuid.hashCode(), notification)

            if (developerSettings.isVerboseLoggingEnabled()) {
                android.util.Log.d("ExplorationEngine",
                    "Notified user about generic alias: $genericAlias for element at ${element.bounds}")
            }

        } catch (e: Exception) {
            android.util.Log.e("ExplorationEngine", "Failed to send generic alias notification", e)
        }
    }

    /**
     * Check if package is a system app
     *
     * System apps are pre-installed apps that come with the device.
     * They often have limited accessibility support and may require special permissions.
     *
     * Detection heuristics:
     * - Package name starts with "com.android."
     * - Package name starts with "com.google." (Google system apps)
     * - Installed in /system partition (flags contain FLAG_SYSTEM)
     *
     * FIX: Addresses issue #3 from test report (Settings app not supported)
     *
     * @param packageName Package name to check
     * @return true if system app
     */
    private fun isSystemApp(packageName: String): Boolean {
        try {
            // Heuristic 1: Package name prefixes
            val systemPrefixes = listOf(
                "com.android.",
                "com.google.android.apps.messaging",  // Messages
                "com.google.android.dialer",          // Phone
                "com.google.android.contacts",        // Contacts
                "com.google.android.deskclock"        // Clock (mentioned in test report - NOT system)
            )

            val isSystemByPrefix = systemPrefixes.any { packageName.startsWith(it) }

            if (isSystemByPrefix) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine",
                        "System app detected by prefix: $packageName")
                }
                return true
            }

            // Heuristic 2: Check application flags
            val packageManager = accessibilityService.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)

            // FLAG_SYSTEM indicates app is installed in /system partition
            val isSystem = (appInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0

            if (isSystem) {
                if (developerSettings.isVerboseLoggingEnabled()) {
                    android.util.Log.d("ExplorationEngine",
                        "System app detected by FLAG_SYSTEM: $packageName")
                }
            }

            return isSystem

        } catch (e: Exception) {
            android.util.Log.w("ExplorationEngine",
                "Failed to check if $packageName is system app: ${e.message}")
            return false
        }
    }

    companion object {
        /**
         * Notification channel ID for exploration events
         */
        private const val NOTIFICATION_CHANNEL_ID = "learnapp_exploration"

        /**
         * Notification ID for login screen alerts
         */
        private const val LOGIN_NOTIFICATION_ID = 1001
    }
}
