/**
 * ProgressTracker.kt - Exploration progress and coverage tracking
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/tracking/ProgressTracker.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 *
 * Tracks exploration progress and coverage metrics in real-time
 */

package com.augmentalis.voiceoscore.learnapp.tracking

import com.augmentalis.voiceoscore.learnapp.models.ExplorationProgress
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Progress Tracker
 *
 * Tracks exploration progress and coverage in real-time.
 * Provides metrics on:
 * - Coverage percentage (% of app explored)
 * - Completion status (screens, features discovered)
 * - Unexplored areas (missed sections)
 * - Progress visualization data (for UI)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val tracker = ProgressTracker("com.instagram.android")
 *
 * // Start tracking
 * tracker.startTracking()
 *
 * // Record screen visit
 * tracker.recordScreenVisit(screenState)
 *
 * // Record element discovery
 * tracker.recordElementDiscovery(elementInfo)
 *
 * // Get coverage metrics
 * val coverage = tracker.getCoverageMetrics()
 * println("Coverage: ${coverage.coveragePercentage}%")
 *
 * // Get unexplored areas
 * val unexplored = tracker.getUnexploredAreas()
 *
 * // Observe progress in real-time
 * tracker.progress.collect { progress ->
 *     updateUI(progress)
 * }
 * ```
 *
 * @property packageName Package name being tracked
 *
 * @since 1.0.0
 */
class ProgressTracker(
    private val packageName: String
) {

    /**
     * Tracking state
     */
    private val _isTracking = MutableStateFlow(false)
    val isTracking: StateFlow<Boolean> = _isTracking.asStateFlow()

    /**
     * Current progress
     */
    private val _progress = MutableStateFlow(
        ExplorationProgress(
            appName = packageName,
            screensExplored = 0,
            estimatedTotalScreens = 0,
            elementsDiscovered = 0,
            currentDepth = 0,
            currentScreen = "",
            elapsedTimeMs = 0L
        )
    )
    val progress: StateFlow<ExplorationProgress> = _progress.asStateFlow()

    /**
     * Coverage metrics
     */
    private val _coverageMetrics = MutableStateFlow(CoverageMetrics())
    val coverageMetrics: StateFlow<CoverageMetrics> = _coverageMetrics.asStateFlow()

    /**
     * Tracking start time
     */
    private var startTime: Long = 0L

    /**
     * Visited screens (screen hash -> ScreenState)
     */
    private val visitedScreens = mutableMapOf<String, ScreenState>()

    /**
     * Discovered elements (element UUID -> count)
     */
    private val discoveredElements = mutableMapOf<String, Int>()

    /**
     * Unexplored areas (screen hash -> list of unexplored element UUIDs)
     */
    private val unexploredAreas = mutableMapOf<String, MutableList<String>>()

    /**
     * Navigation depth tracking (screen hash -> depth)
     */
    private val screenDepths = mutableMapOf<String, Int>()

    /**
     * Screen visit timestamps (screen hash -> timestamp)
     */
    private val visitTimestamps = mutableMapOf<String, Long>()

    /**
     * Feature discovery log (feature name -> timestamp)
     */
    private val featureDiscoveries = mutableMapOf<String, Long>()

    /**
     * Start tracking
     */
    fun startTracking() {
        _isTracking.value = true
        startTime = System.currentTimeMillis()

        // Reset state
        visitedScreens.clear()
        discoveredElements.clear()
        unexploredAreas.clear()
        screenDepths.clear()
        visitTimestamps.clear()
        featureDiscoveries.clear()
    }

    /**
     * Stop tracking
     */
    fun stopTracking() {
        _isTracking.value = false
    }

    /**
     * Record screen visit
     *
     * @param screenState Screen state
     * @param depth Navigation depth
     */
    fun recordScreenVisit(screenState: ScreenState, depth: Int = 0) {
        if (!_isTracking.value) return

        val screenHash = screenState.hash

        // Record visit
        visitedScreens[screenHash] = screenState
        screenDepths[screenHash] = depth
        visitTimestamps[screenHash] = System.currentTimeMillis()

        // Update progress
        updateProgress(depth, screenState.activityName ?: "Unknown")
    }

    /**
     * Record element discovery
     *
     * @param elementUuid Element UUID
     * @param screenHash Screen hash where element was found
     */
    fun recordElementDiscovery(elementUuid: String, screenHash: String = "") {
        if (!_isTracking.value) return

        // Increment discovery count
        val currentCount = discoveredElements[elementUuid] ?: 0
        discoveredElements[elementUuid] = currentCount + 1

        // Remove from unexplored (if present)
        if (screenHash.isNotBlank()) {
            unexploredAreas[screenHash]?.remove(elementUuid)
        }

        // Update progress
        updateProgress()
    }

    /**
     * Record unexplored element
     *
     * @param elementUuid Element UUID
     * @param screenHash Screen hash
     */
    fun recordUnexploredElement(elementUuid: String, screenHash: String) {
        if (!_isTracking.value) return

        val unexploredList = unexploredAreas.getOrPut(screenHash) { mutableListOf() }
        if (!unexploredList.contains(elementUuid)) {
            unexploredList.add(elementUuid)
        }

        updateCoverageMetrics()
    }

    /**
     * Record feature discovery
     *
     * @param featureName Feature name
     */
    fun recordFeatureDiscovery(featureName: String) {
        if (!_isTracking.value) return

        if (!featureDiscoveries.containsKey(featureName)) {
            featureDiscoveries[featureName] = System.currentTimeMillis()
        }

        updateCoverageMetrics()
    }

    /**
     * Update progress
     *
     * @param currentDepth Current navigation depth
     * @param currentScreen Current screen name
     */
    private fun updateProgress(currentDepth: Int = 0, currentScreen: String = "") {
        val elapsed = System.currentTimeMillis() - startTime

        val newProgress = ExplorationProgress(
            appName = packageName,
            screensExplored = visitedScreens.size,
            estimatedTotalScreens = estimateTotalScreens(),
            elementsDiscovered = discoveredElements.size,
            currentDepth = currentDepth,
            currentScreen = currentScreen.ifBlank { "Unknown" },
            elapsedTimeMs = elapsed
        )

        _progress.value = newProgress
        updateCoverageMetrics()
    }

    /**
     * Update coverage metrics
     */
    private fun updateCoverageMetrics() {
        val totalUnexplored = unexploredAreas.values.sumOf { it.size }
        val totalDiscovered = discoveredElements.size
        val totalElements = totalDiscovered + totalUnexplored

        val coveragePercentage = if (totalElements > 0) {
            (totalDiscovered.toFloat() / totalElements.toFloat()) * 100f
        } else {
            0f
        }

        val screenCoverage = if (visitedScreens.isNotEmpty()) {
            val fullyExploredScreens = unexploredAreas.count { it.value.isEmpty() }
            (fullyExploredScreens.toFloat() / visitedScreens.size.toFloat()) * 100f
        } else {
            0f
        }

        val completionPercentage = calculateCompletionPercentage()

        _coverageMetrics.value = CoverageMetrics(
            coveragePercentage = coveragePercentage,
            screenCoveragePercentage = screenCoverage,
            completionPercentage = completionPercentage,
            totalScreensVisited = visitedScreens.size,
            totalElementsDiscovered = totalDiscovered,
            totalUnexploredElements = totalUnexplored,
            featuresDiscovered = featureDiscoveries.size,
            maxDepthReached = screenDepths.values.maxOrNull() ?: 0
        )
    }

    /**
     * Calculate completion percentage
     *
     * Uses heuristic based on:
     * - Screen coverage
     * - Element discovery rate
     * - Depth exploration
     * - Time elapsed
     *
     * @return Completion percentage (0-100)
     */
    private fun calculateCompletionPercentage(): Float {
        val screenWeight = 0.4f
        val elementWeight = 0.3f
        val depthWeight = 0.2f
        val timeWeight = 0.1f

        // Screen completion (visited vs estimated)
        val estimatedScreens = estimateTotalScreens()
        val screenCompletion = if (estimatedScreens > 0) {
            (visitedScreens.size.toFloat() / estimatedScreens.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }

        // Element completion (discovered vs total)
        val totalUnexplored = unexploredAreas.values.sumOf { it.size }
        val totalElements = discoveredElements.size + totalUnexplored
        val elementCompletion = if (totalElements > 0) {
            (discoveredElements.size.toFloat() / totalElements.toFloat())
        } else {
            0f
        }

        // Depth completion (current depth vs max expected depth)
        val maxExpectedDepth = 10  // Reasonable max depth
        val currentMaxDepth = screenDepths.values.maxOrNull() ?: 0
        val depthCompletion = (currentMaxDepth.toFloat() / maxExpectedDepth.toFloat()).coerceIn(0f, 1f)

        // Time completion (elapsed vs expected duration)
        val expectedDuration = 30 * 60 * 1000L  // 30 minutes
        val elapsed = System.currentTimeMillis() - startTime
        val timeCompletion = (elapsed.toFloat() / expectedDuration.toFloat()).coerceIn(0f, 1f)

        // Weighted average
        val completion = (
            screenCompletion * screenWeight +
            elementCompletion * elementWeight +
            depthCompletion * depthWeight +
            timeCompletion * timeWeight
        ) * 100f

        return completion.coerceIn(0f, 100f)
    }

    /**
     * Estimate total screens in app
     *
     * Uses heuristic based on current exploration.
     *
     * @return Estimated total screens
     */
    private fun estimateTotalScreens(): Int {
        if (visitedScreens.isEmpty()) {
            return 20  // Initial estimate
        }

        // Heuristic: Use discovery rate to estimate
        val avgElementsPerScreen = if (visitedScreens.isNotEmpty()) {
            discoveredElements.size / visitedScreens.size
        } else {
            10
        }

        // Assume app has 20-50 screens based on complexity
        val baseEstimate = when {
            avgElementsPerScreen < 5 -> 20   // Simple app
            avgElementsPerScreen < 15 -> 35  // Medium complexity
            else -> 50                        // Complex app
        }

        // Adjust based on current discoveries
        val adjustedEstimate = maxOf(
            baseEstimate,
            visitedScreens.size + 10  // At least 10 more screens
        )

        return adjustedEstimate
    }

    /**
     * Get coverage metrics
     *
     * @return Current coverage metrics
     */
    fun getCoverageMetrics(): CoverageMetrics {
        return _coverageMetrics.value
    }

    /**
     * Get unexplored areas
     *
     * @return Map of screen hash -> unexplored elements
     */
    fun getUnexploredAreas(): Map<String, List<String>> {
        return unexploredAreas.mapValues { it.value.toList() }
    }

    /**
     * Get visited screens
     *
     * @return List of visited screen states
     */
    fun getVisitedScreens(): List<ScreenState> {
        return visitedScreens.values.toList()
    }

    /**
     * Get screen by hash
     *
     * @param screenHash Screen hash
     * @return ScreenState (or null if not found)
     */
    fun getScreenByHash(screenHash: String): ScreenState? {
        return visitedScreens[screenHash]
    }

    /**
     * Get most visited elements
     *
     * @param limit Maximum number of elements to return
     * @return List of element UUIDs sorted by visit count
     */
    fun getMostVisitedElements(limit: Int = 10): List<Pair<String, Int>> {
        return discoveredElements
            .entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key to it.value }
    }

    /**
     * Get exploration timeline
     *
     * @return List of screen visits sorted by timestamp
     */
    fun getExplorationTimeline(): List<ScreenVisit> {
        return visitTimestamps
            .map { (hash, timestamp) ->
                ScreenVisit(
                    screenHash = hash,
                    screenState = visitedScreens[hash],
                    timestamp = timestamp,
                    depth = screenDepths[hash] ?: 0
                )
            }
            .sortedBy { it.timestamp }
    }

    /**
     * Get feature discoveries
     *
     * @return Map of feature name -> discovery timestamp
     */
    fun getFeatureDiscoveries(): Map<String, Long> {
        return featureDiscoveries.toMap()
    }

    /**
     * Check if screen is fully explored
     *
     * @param screenHash Screen hash
     * @return true if all elements in screen have been explored
     */
    fun isScreenFullyExplored(screenHash: String): Boolean {
        val unexplored = unexploredAreas[screenHash]
        return unexplored == null || unexplored.isEmpty()
    }

    /**
     * Get exploration statistics
     *
     * @return Exploration statistics
     */
    fun getStats(): ExplorationStats {
        val elapsed = System.currentTimeMillis() - startTime
        val avgTimePerScreen = if (visitedScreens.isNotEmpty()) {
            elapsed / visitedScreens.size
        } else {
            0L
        }

        return ExplorationStats(
            totalScreensVisited = visitedScreens.size,
            totalElementsDiscovered = discoveredElements.size,
            totalUnexploredElements = unexploredAreas.values.sumOf { it.size },
            featuresDiscovered = featureDiscoveries.size,
            maxDepthReached = screenDepths.values.maxOrNull() ?: 0,
            totalTimeElapsed = elapsed,
            averageTimePerScreen = avgTimePerScreen,
            fullyExploredScreens = unexploredAreas.count { it.value.isEmpty() }
        )
    }

    /**
     * Export progress data for visualization
     *
     * @return Visualization data
     */
    fun exportVisualizationData(): VisualizationData {
        val timeline = getExplorationTimeline()
        val coverage = getCoverageMetrics()
        val stats = getStats()

        return VisualizationData(
            packageName = packageName,
            timeline = timeline,
            coverageMetrics = coverage,
            stats = stats,
            unexploredAreas = getUnexploredAreas(),
            featureDiscoveries = getFeatureDiscoveries()
        )
    }

    /**
     * Reset tracker
     */
    fun reset() {
        visitedScreens.clear()
        discoveredElements.clear()
        unexploredAreas.clear()
        screenDepths.clear()
        visitTimestamps.clear()
        featureDiscoveries.clear()

        _progress.value = ExplorationProgress(
            appName = packageName,
            screensExplored = 0,
            estimatedTotalScreens = 0,
            elementsDiscovered = 0,
            currentDepth = 0,
            currentScreen = "",
            elapsedTimeMs = 0L
        )

        _coverageMetrics.value = CoverageMetrics()
    }
}

/**
 * Coverage metrics
 */
data class CoverageMetrics(
    val coveragePercentage: Float = 0f,
    val screenCoveragePercentage: Float = 0f,
    val completionPercentage: Float = 0f,
    val totalScreensVisited: Int = 0,
    val totalElementsDiscovered: Int = 0,
    val totalUnexploredElements: Int = 0,
    val featuresDiscovered: Int = 0,
    val maxDepthReached: Int = 0
)

/**
 * Screen visit
 */
data class ScreenVisit(
    val screenHash: String,
    val screenState: ScreenState?,
    val timestamp: Long,
    val depth: Int
)

/**
 * Exploration statistics
 */
data class ExplorationStats(
    val totalScreensVisited: Int,
    val totalElementsDiscovered: Int,
    val totalUnexploredElements: Int,
    val featuresDiscovered: Int,
    val maxDepthReached: Int,
    val totalTimeElapsed: Long,
    val averageTimePerScreen: Long,
    val fullyExploredScreens: Int
)

/**
 * Visualization data
 */
data class VisualizationData(
    val packageName: String,
    val timeline: List<ScreenVisit>,
    val coverageMetrics: CoverageMetrics,
    val stats: ExplorationStats,
    val unexploredAreas: Map<String, List<String>>,
    val featureDiscoveries: Map<String, Long>
)
