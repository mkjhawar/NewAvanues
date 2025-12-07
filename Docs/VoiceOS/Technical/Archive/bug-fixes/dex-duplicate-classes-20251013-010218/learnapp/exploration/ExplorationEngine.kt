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

package com.augmentalis.learnapp.exploration

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.learnapp.elements.ElementClassifier
import com.augmentalis.learnapp.fingerprinting.ScreenStateManager
import com.augmentalis.learnapp.models.ExplorationProgress
import com.augmentalis.learnapp.models.ExplorationState
import com.augmentalis.learnapp.models.ExplorationStats
import com.augmentalis.learnapp.navigation.NavigationGraphBuilder
import com.augmentalis.learnapp.scrolling.ScrollDetector
import com.augmentalis.learnapp.scrolling.ScrollExecutor
import com.augmentalis.uuidcreator.UUIDCreator
import com.augmentalis.uuidcreator.alias.UuidAliasManager
import com.augmentalis.uuidcreator.models.UUIDElement
import com.augmentalis.uuidcreator.models.UUIDMetadata
import com.augmentalis.uuidcreator.models.UUIDAccessibility
import com.augmentalis.uuidcreator.thirdparty.ThirdPartyUuidGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    private val accessibilityService: AccessibilityService,
    private val uuidCreator: UUIDCreator,
    private val thirdPartyGenerator: ThirdPartyUuidGenerator,
    private val aliasManager: UuidAliasManager,
    private val strategy: ExplorationStrategy = DFSExplorationStrategy()
) {

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
    private val screenExplorer = ScreenExplorer(
        screenStateManager = screenStateManager,
        elementClassifier = elementClassifier,
        scrollDetector = ScrollDetector(),
        scrollExecutor = ScrollExecutor()
    )

    /**
     * Navigation graph builder
     */
    private lateinit var navigationGraphBuilder: NavigationGraphBuilder

    /**
     * Exploration state flow
     */
    private val _explorationState = MutableStateFlow<ExplorationState>(ExplorationState.Idle)
    val explorationState: StateFlow<ExplorationState> = _explorationState.asStateFlow()

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
     * Start exploration
     *
     * Begins DFS exploration of app.
     *
     * @param packageName Package name to explore
     */
    fun startExploration(packageName: String) {
        scope.launch {
            try {
                // Initialize
                navigationGraphBuilder = NavigationGraphBuilder(packageName)
                screenStateManager.clear()
                startTimestamp = System.currentTimeMillis()
                dangerousElementsSkipped = 0
                loginScreensDetected = 0

                _explorationState.value = ExplorationState.Running(
                    packageName = packageName,
                    progress = ExplorationProgress(
                        appName = packageName,
                        screensExplored = 0,
                        estimatedTotalScreens = 20,
                        elementsDiscovered = 0,
                        currentDepth = 0,
                        currentScreen = "Starting...",
                        elapsedTimeMs = 0L
                    )
                )

                // Get root node
                val rootNode = accessibilityService.rootInActiveWindow
                if (rootNode == null) {
                    _explorationState.value = ExplorationState.Failed(
                        packageName = packageName,
                        error = IllegalStateException("Cannot get root node"),
                        partialProgress = null
                    )
                    return@launch
                }

                // Start DFS exploration
                exploreScreenRecursive(rootNode, packageName, depth = 0)

                // Exploration completed
                val stats = createExplorationStats(packageName)
                _explorationState.value = ExplorationState.Completed(
                    packageName = packageName,
                    stats = stats
                )

            } catch (e: Exception) {
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
     * Explore screen recursively (DFS)
     *
     * Core DFS algorithm.
     *
     * @param rootNode Root node of current screen
     * @param packageName Package name
     * @param depth Current depth
     */
    private suspend fun exploreScreenRecursive(
        rootNode: AccessibilityNodeInfo,
        packageName: String,
        depth: Int
    ) {
        // Check depth limit
        if (depth > strategy.getMaxDepth()) {
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
                // Login screen detected - pause and wait for user
                loginScreensDetected++
                _explorationState.value = ExplorationState.PausedForLogin(
                    packageName = packageName,
                    progress = getCurrentProgress(packageName, depth)
                )

                // Wait for user to login (screen change)
                waitForScreenChange(explorationResult.screenState.hash)

                // Resume exploration from new screen
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

                // Update dangerous elements count
                dangerousElementsSkipped += explorationResult.dangerousElements.size

                // Register elements with UUIDCreator
                val elementUuids = registerElements(
                    elements = explorationResult.safeClickableElements,
                    packageName = packageName
                )

                // Add screen to navigation graph
                navigationGraphBuilder.addScreen(
                    screenState = explorationResult.screenState,
                    elementUuids = elementUuids
                )

                // Update progress
                updateProgress(packageName, depth, explorationResult.screenState.hash)

                // 2. Order elements by strategy
                val orderedElements = strategy.orderElements(explorationResult.safeClickableElements)

                // 3. Explore each element (DFS)
                for (element in orderedElements) {
                    // Check if should explore
                    if (!strategy.shouldExplore(element)) {
                        continue
                    }

                    // Click element
                    val clicked = clickElement(element.node)
                    if (!clicked) {
                        continue
                    }

                    // Wait for screen transition
                    delay(1000)

                    // Get new screen
                    val newRootNode = accessibilityService.rootInActiveWindow
                    if (newRootNode == null) {
                        // Backtrack
                        pressBack()
                        delay(1000)
                        continue
                    }

                    // Capture new screen state
                    val newScreenState = screenStateManager.captureScreenState(
                        newRootNode,
                        packageName,
                        depth + 1
                    )

                    // Record navigation edge
                    element.uuid?.let { uuid ->
                        navigationGraphBuilder.addEdge(
                            fromScreenHash = explorationResult.screenState.hash,
                            clickedElementUuid = uuid,
                            toScreenHash = newScreenState.hash
                        )
                    }

                    // Recurse
                    exploreScreenRecursive(newRootNode, packageName, depth + 1)

                    // Backtrack
                    pressBack()
                    delay(1000)
                }
            }
        }
    }

    /**
     * Register elements with UUIDCreator
     *
     * @param elements List of elements
     * @param packageName Package name
     * @return List of UUIDs
     */
    private suspend fun registerElements(
        elements: List<com.augmentalis.learnapp.models.ElementInfo>,
        packageName: String
    ): List<String> {
        return elements.mapNotNull { element ->
            element.node?.let { node ->
                // Generate UUID
                val uuid = thirdPartyGenerator.generateUuid(node, packageName)

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

                // Register with UUIDCreator
                uuidCreator.registerElement(uuidElement)

                // Create alias
                aliasManager.createAutoAlias(
                    uuid = uuid,
                    elementName = uuidElement.name,
                    elementType = uuidElement.type
                )

                // Store UUID in element
                element.uuid = uuid

                uuid
            }
        }
    }

    /**
     * Click element
     *
     * @param node Node to click
     * @return true if clicked successfully
     */
    private suspend fun clickElement(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false

        return try {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Press back button
     */
    private suspend fun pressBack() {
        accessibilityService.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Wait for screen change
     *
     * @param previousHash Previous screen hash
     */
    private suspend fun waitForScreenChange(previousHash: String) {
        val timeout = 60000L  // 1 minute
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeout) {
            val currentHash = screenStateManager.getCurrentScreenHash()
            if (currentHash != null && currentHash != previousHash) {
                return
            }

            delay(500)
        }
    }

    /**
     * Wait for resume (when paused)
     */
    private suspend fun waitForResume() {
        while (_explorationState.value is ExplorationState.PausedByUser) {
            delay(100)
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
            elementsDiscovered = navigationGraphBuilder.getNodeCount(),
            currentDepth = depth,
            currentScreen = "Screen ${stats.totalScreensVisited}",
            elapsedTimeMs = elapsed
        )

        _explorationState.value = ExplorationState.Running(
            packageName = packageName,
            progress = progress
        )
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
            elementsDiscovered = navigationGraphBuilder.getNodeCount(),
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
    private fun createExplorationStats(packageName: String): ExplorationStats {
        val stats = screenStateManager.getStats()
        val graph = navigationGraphBuilder.build()
        val graphStats = graph.getStats()
        val elapsed = System.currentTimeMillis() - startTimestamp

        return ExplorationStats(
            packageName = packageName,
            appName = packageName,
            totalScreens = stats.totalScreensDiscovered,
            totalElements = graphStats.totalScreens,
            totalEdges = graphStats.totalEdges,
            durationMs = elapsed,
            maxDepth = graphStats.maxDepth,
            dangerousElementsSkipped = dangerousElementsSkipped,
            loginScreensDetected = loginScreensDetected,
            scrollableContainersFound = 0  // TODO: track this
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
     */
    fun stopExploration() {
        val currentState = _explorationState.value
        if (currentState is ExplorationState.Running) {
            val stats = createExplorationStats(currentState.packageName)
            _explorationState.value = ExplorationState.Completed(
                packageName = currentState.packageName,
                stats = stats
            )
        }
    }
}
