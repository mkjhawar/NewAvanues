/**
 * AVUQuantizer.kt - Real-time quantization engine for UI elements
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-08
 *
 * Performs real-time quantization of UI elements during exploration:
 * - Converts raw accessibility data to quantized format
 * - Precomputes semantic types, importance scores, quadrants
 * - Maintains incremental state for efficient updates
 * - Thread-safe for concurrent exploration
 *
 * Part of LearnApp NLU Integration feature
 */
package com.augmentalis.voiceoscore.learnapp.ai.quantized

import android.graphics.Rect
import android.util.Log
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ExplorationBehavior
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * AVU Quantizer
 *
 * Real-time quantization engine that converts raw UI data to quantized format
 * during exploration. Designed for incremental updates with minimal overhead.
 *
 * ## Features:
 * - Real-time element quantization
 * - Screen type detection
 * - Semantic element classification
 * - Importance scoring
 * - Quadrant calculation
 * - Action clustering
 * - Incremental updates
 *
 * ## Usage:
 * ```kotlin
 * val quantizer = AVUQuantizer(screenWidth = 1080, screenHeight = 2340)
 *
 * // During exploration
 * quantizer.onScreenExplored(screenHash, activityName, elements)
 * quantizer.onNavigationDiscovered(fromScreen, toScreen, triggerId, triggerLabel)
 *
 * // Get quantized context
 * val context = quantizer.buildQuantizedContext(packageName, appName)
 * ```
 */
class AVUQuantizer(
    private val screenWidth: Int = 1080,
    private val screenHeight: Int = 2340
) {
    companion object {
        private const val TAG = "AVUQuantizer"

        // Importance weights
        private const val WEIGHT_CLICKABLE = 20
        private const val WEIGHT_VISIBLE_TEXT = 15
        private const val WEIGHT_PRIMARY_ACTION = 25
        private const val WEIGHT_NAVIGATION = 20
        private const val WEIGHT_TOP_POSITION = 10
        private const val WEIGHT_LARGE_SIZE = 10

        // Grid configuration (3x3)
        private const val GRID_COLS = 3
        private const val GRID_ROWS = 3
    }

    // State storage
    private val screens = ConcurrentHashMap<String, QuantizedScreen>()
    private val screenElements = ConcurrentHashMap<String, MutableList<QuantizedElement>>()
    private val navigation = ConcurrentHashMap<String, QuantizedNavigation>()
    private val actionCandidates = ConcurrentHashMap<String, QuantizedAction>()
    private val semanticClusters = ConcurrentHashMap<String, MutableSet<String>>() // clusterName -> elementIds

    // Thread safety
    private val mutex = Mutex()

    // Statistics
    private var totalElementsProcessed = 0
    private var totalScreensProcessed = 0

    /**
     * Process a newly explored screen
     *
     * @param screenHash Unique screen identifier
     * @param activityName Activity name
     * @param elements List of discovered elements
     * @param parentScreenHash Parent screen (for depth calculation)
     * @param scrollDirection Detected scroll direction
     */
    suspend fun onScreenExplored(
        screenHash: String,
        activityName: String?,
        elements: List<ElementInfo>,
        parentScreenHash: String? = null,
        scrollDirection: ScrollDirection = ScrollDirection.NONE
    ) = mutex.withLock {
        val startTime = System.currentTimeMillis()

        // Calculate depth from parent
        val depth = if (parentScreenHash == null) 0 else {
            (screens[parentScreenHash]?.depth ?: 0) + 1
        }

        // Detect screen type
        val screenType = detectScreenType(activityName, elements)

        // Detect screen purpose
        val purpose = detectScreenPurpose(activityName, elements, screenType)

        // Quantize elements
        val quantizedElements = elements.mapNotNull { element ->
            quantizeElement(element, screenHash)
        }.sortedByDescending { it.importance }

        // Store screen
        val shortHash = screenHash.take(8)
        val quantizedScreen = QuantizedScreen(
            screenId = shortHash,
            screenType = screenType,
            purpose = purpose,
            elementCount = quantizedElements.size,
            primaryElements = quantizedElements.take(5),
            scrollDirection = scrollDirection,
            depth = depth,
            parentScreenId = parentScreenHash?.take(8)
        )

        screens[shortHash] = quantizedScreen
        screenElements[shortHash] = quantizedElements.toMutableList()

        // Update action candidates
        updateActionCandidates(shortHash, quantizedElements)

        // Update semantic clusters
        updateSemanticClusters(shortHash, quantizedElements)

        totalScreensProcessed++
        totalElementsProcessed += elements.size

        val elapsed = System.currentTimeMillis() - startTime
        Log.d(TAG, "Quantized screen $shortHash: ${quantizedElements.size} elements in ${elapsed}ms")
    }

    /**
     * Process a navigation discovery
     *
     * @param fromScreenHash Source screen
     * @param toScreenHash Destination screen
     * @param triggerElementId Element that triggered navigation
     * @param triggerLabel Label of trigger element
     */
    suspend fun onNavigationDiscovered(
        fromScreenHash: String,
        toScreenHash: String,
        triggerElementId: String,
        triggerLabel: String
    ) = mutex.withLock {
        val fromShort = fromScreenHash.take(8)
        val toShort = toScreenHash.take(8)
        val pathId = "${fromShort}_${toShort}"

        // Detect navigation type
        val pathType = detectNavigationPathType(fromShort, toShort, triggerLabel)

        // Check for existing path (increment frequency)
        val existing = navigation[pathId]
        val frequency = (existing?.frequency ?: 0) + 1

        val quantizedNav = QuantizedNavigation(
            pathId = pathId,
            fromScreenId = fromShort,
            toScreenId = toShort,
            triggerElementId = triggerElementId.take(8),
            triggerLabel = triggerLabel,
            pathType = pathType,
            frequency = frequency,
            avgLatency = 0 // Would be calculated from actual measurements
        )

        navigation[pathId] = quantizedNav
        Log.d(TAG, "Recorded navigation: $fromShort -> $toShort via '$triggerLabel'")
    }

    /**
     * Build complete quantized context
     *
     * @param packageName App package
     * @param appName App display name
     * @return Complete quantized context
     */
    suspend fun buildQuantizedContext(
        packageName: String,
        appName: String
    ): QuantizedContext = mutex.withLock {
        val startTime = System.currentTimeMillis()

        // Calculate navigation complexity (1-5)
        val navComplexity = calculateNavigationComplexity()

        // Calculate coverage
        val coverage = calculateCoverage()

        // Build app context
        val appType = AppType.fromPackage(packageName)
        val primaryActions = actionCandidates.values
            .sortedByDescending { it.confidence }
            .take(10)
            .toList()

        val clusters = buildSemanticClusters()

        val appContext = QuantizedAppContext(
            packageName = packageName,
            appName = appName,
            appType = appType,
            screenCount = screens.size,
            primaryActions = primaryActions,
            semanticClusters = clusters,
            navigationComplexity = navComplexity,
            coverage = coverage,
            lastUpdated = System.currentTimeMillis()
        )

        val elapsed = System.currentTimeMillis() - startTime
        Log.i(TAG, "Built quantized context in ${elapsed}ms: ${screens.size} screens, ${totalElementsProcessed} elements")

        QuantizedContext(
            appContext = appContext,
            screens = screens.values.sortedBy { it.depth },
            elements = screenElements.toMap(),
            navigation = navigation.values.sortedByDescending { it.frequency },
            clusters = clusters,
            actions = primaryActions
        )
    }

    /**
     * Quantize a single element
     */
    private fun quantizeElement(element: ElementInfo, screenHash: String): QuantizedElement? {
        // Check actionability using ElementInfo properties
        val isLongClickable = element.explorationBehavior == ExplorationBehavior.LONG_CLICKABLE
        val isEditable = element.isEditText()

        // Skip non-actionable elements
        if (!element.isClickable && !isLongClickable &&
            !isEditable && !element.isScrollable) {
            return null
        }

        // Get label (prefer text, then contentDescription, then resourceId)
        val label = normalizeLabel(
            element.text.ifEmpty { element.contentDescription.ifEmpty { element.resourceId } }
        )

        // Skip elements without usable label
        if (label.isBlank() || label.length < 2) {
            return null
        }

        // Detect semantic type
        val semanticType = detectSemanticType(element, label)

        // Encode actions
        val actions = QuantizedElement.encodeActions(
            clickable = element.isClickable,
            longClickable = isLongClickable,
            editable = isEditable,
            scrollable = element.isScrollable
        )

        // Calculate importance
        val importance = calculateImportance(element, semanticType, label)

        // Calculate quadrant - bounds is already a Rect
        val quadrant = calculateQuadrantFromRect(element.bounds)

        return QuantizedElement(
            elementId = element.stableId().take(8),
            label = label.take(30), // Truncate for compactness
            semanticType = semanticType,
            actions = actions,
            importance = importance,
            quadrant = quadrant
        )
    }

    /**
     * Detect screen type from activity name and elements
     */
    private fun detectScreenType(activityName: String?, elements: List<ElementInfo>): ScreenType {
        val activityLower = activityName?.lowercase() ?: ""
        val labels = elements.mapNotNull { it.text.ifEmpty { it.contentDescription }.ifEmpty { null } }.map { it.lowercase() }

        return when {
            // Login/Auth screens
            activityLower.contains("login") || activityLower.contains("signin") ||
            activityLower.contains("auth") ||
            labels.any { it.contains("sign in") || it.contains("log in") || it.contains("password") } ->
                ScreenType.LOGIN

            // Settings screens
            activityLower.contains("setting") || activityLower.contains("preference") ||
            labels.count { it.contains("setting") || it.contains("privacy") || it.contains("account") } > 2 ->
                ScreenType.SETTINGS

            // Profile screens
            activityLower.contains("profile") || activityLower.contains("account") ||
            labels.any { it.contains("edit profile") || it.contains("my account") } ->
                ScreenType.PROFILE

            // Search screens
            activityLower.contains("search") ||
            elements.any { it.isEditText() && (it.contentDescription.contains("search", true) ||
                    it.resourceId.contains("search", true)) } ->
                ScreenType.SEARCH

            // Compose/Create screens
            activityLower.contains("compose") || activityLower.contains("create") ||
            activityLower.contains("new") ||
            labels.any { it.contains("post") || it.contains("compose") || it.contains("create") } ->
                ScreenType.COMPOSE

            // Chat screens
            activityLower.contains("chat") || activityLower.contains("message") ||
            activityLower.contains("conversation") ||
            elements.any { it.isEditText() && (it.contentDescription.contains("message", true) ||
                    it.resourceId.contains("input", true)) } ->
                ScreenType.CHAT

            // Media player screens
            activityLower.contains("player") || activityLower.contains("video") ||
            labels.any { it.contains("play") || it.contains("pause") || it.contains("seek") } ->
                ScreenType.PLAYER

            // List/Feed screens (multiple similar clickable items)
            elements.count { it.isClickable }.let { clickableCount ->
                clickableCount > 10 && elements.any { it.isScrollable }
            } -> ScreenType.LIST

            // Detail screens (fewer clickables, more text)
            elements.count { it.isClickable } < 8 &&
            elements.count { it.text.isNotBlank() } > elements.count { it.isClickable } ->
                ScreenType.DETAIL

            // Home/Main screen (first screen, many navigation elements)
            activityLower.contains("main") || activityLower.contains("home") ||
            activityLower.contains("launcher") ->
                ScreenType.HOME

            // Navigation screens (many tab-like elements)
            elements.count {
                it.resourceId.contains("tab", true) ||
                it.resourceId.contains("nav", true)
            } > 3 -> ScreenType.NAVIGATION

            // Dialog screens (small element count, modal-like)
            elements.size < 10 && labels.any { it.contains("ok") || it.contains("cancel") } ->
                ScreenType.DIALOG

            // Form screens (multiple input fields)
            elements.count { it.isEditText() } > 3 ->
                ScreenType.FORM

            else -> ScreenType.UNKNOWN
        }
    }

    /**
     * Detect screen purpose (human-readable description)
     */
    private fun detectScreenPurpose(
        activityName: String?,
        elements: List<ElementInfo>,
        screenType: ScreenType
    ): String {
        val activityLower = activityName?.lowercase() ?: ""
        val labels = elements.mapNotNull { it.text.ifEmpty { it.contentDescription }.ifEmpty { null } }
            .map { it.lowercase() }
            .filter { it.length > 2 }

        // Try to extract meaningful purpose from activity name
        val activityPurpose = activityName?.let {
            it.substringAfterLast(".")
                .replace("Activity", "")
                .replace("Fragment", "")
                .replace(Regex("([A-Z])"), " $1")
                .trim()
                .take(30)
        }

        // Use screen type as base
        val typePurpose = when (screenType) {
            ScreenType.HOME -> "Main screen"
            ScreenType.LIST -> "Browse content"
            ScreenType.DETAIL -> "View details"
            ScreenType.SETTINGS -> "App settings"
            ScreenType.PROFILE -> "User profile"
            ScreenType.SEARCH -> "Search content"
            ScreenType.COMPOSE -> "Create content"
            ScreenType.NAVIGATION -> "Navigate"
            ScreenType.LOGIN -> "Sign in"
            ScreenType.DIALOG -> "Confirm action"
            ScreenType.PLAYER -> "Play media"
            ScreenType.CHAT -> "Chat messages"
            ScreenType.FORM -> "Enter information"
            ScreenType.UNKNOWN -> activityPurpose ?: "Unknown"
        }

        // Enhance with context from labels if meaningful activity name exists
        return if (!activityPurpose.isNullOrBlank() && activityPurpose != "Main") {
            activityPurpose
        } else {
            typePurpose
        }
    }

    /**
     * Detect semantic element type
     */
    private fun detectSemanticType(element: ElementInfo, label: String): SemanticElementType {
        val className = element.className.lowercase()
        val resourceId = element.resourceId.lowercase()
        val labelLower = label.lowercase()
        val isEditable = element.isEditText()

        return when {
            // Navigation elements
            labelLower == "back" || resourceId.contains("back") ||
            resourceId.contains("navigate_up") -> SemanticElementType.NAV_BACK

            resourceId.contains("tab") || className.contains("tab") ->
                SemanticElementType.NAV_TAB

            resourceId.contains("menu") || labelLower.contains("menu") ->
                SemanticElementType.NAV_MENU

            resourceId.contains("nav") || labelLower.contains("navigation") ->
                SemanticElementType.NAV_BUTTON

            element.isClickable && (labelLower.startsWith("http") || resourceId.contains("link")) ->
                SemanticElementType.NAV_LINK

            // Input elements
            isEditable && resourceId.contains("search") ->
                SemanticElementType.INP_SEARCH

            isEditable && (resourceId.contains("password") || labelLower.contains("password")) ->
                SemanticElementType.INP_PASSWORD

            isEditable -> SemanticElementType.INP_TEXT

            className.contains("checkbox") -> SemanticElementType.INP_CHECKBOX
            className.contains("radio") -> SemanticElementType.INP_RADIO
            className.contains("spinner") || className.contains("dropdown") ->
                SemanticElementType.INP_SELECT

            // Action elements
            resourceId.contains("fab") || className.contains("floatingactionbutton") ->
                SemanticElementType.SPL_FAB

            labelLower.contains("submit") || labelLower.contains("send") ||
            labelLower.contains("post") || labelLower.contains("save") ->
                SemanticElementType.ACT_SUBMIT

            labelLower.contains("cancel") || labelLower.contains("dismiss") ||
            labelLower.contains("close") -> SemanticElementType.ACT_CANCEL

            labelLower.contains("delete") || labelLower.contains("remove") ->
                SemanticElementType.ACT_DELETE

            className.contains("switch") || className.contains("toggle") ->
                SemanticElementType.ACT_TOGGLE

            // Determine if primary or secondary action based on position/importance
            element.isClickable && className.contains("button") -> {
                if (resourceId.contains("primary") || labelLower.length <= 10) {
                    SemanticElementType.ACT_PRIMARY
                } else {
                    SemanticElementType.ACT_SECONDARY
                }
            }

            // Container elements
            element.isScrollable && className.contains("recycler") ->
                SemanticElementType.CTR_LIST

            element.isScrollable && className.contains("viewpager") ->
                SemanticElementType.CTR_PAGER

            element.isScrollable -> SemanticElementType.CTR_LIST

            // Content elements
            className.contains("imageview") && resourceId.contains("avatar") ->
                SemanticElementType.CON_AVATAR

            className.contains("card") -> SemanticElementType.CON_CARD

            className.contains("image") || className.contains("video") ->
                SemanticElementType.CON_MEDIA

            element.isClickable -> SemanticElementType.CON_ITEM

            else -> SemanticElementType.UNKNOWN
        }
    }

    /**
     * Calculate element importance score (0-100)
     */
    private fun calculateImportance(
        element: ElementInfo,
        semanticType: SemanticElementType,
        label: String
    ): Int {
        var score = 0

        // Clickable elements are more important
        if (element.isClickable) score += WEIGHT_CLICKABLE

        // Visible text increases importance
        if (element.text.isNotBlank()) score += WEIGHT_VISIBLE_TEXT

        // Primary actions are highly important
        if (semanticType == SemanticElementType.ACT_PRIMARY ||
            semanticType == SemanticElementType.SPL_FAB ||
            semanticType == SemanticElementType.ACT_SUBMIT) {
            score += WEIGHT_PRIMARY_ACTION
        }

        // Navigation elements are important
        if (semanticType.code.startsWith("N")) {
            score += WEIGHT_NAVIGATION
        }

        // Top-positioned elements (header area) are more important
        // bounds is already a Rect
        val rect = element.bounds
        if (rect.top < screenHeight / 4) {
            score += WEIGHT_TOP_POSITION
        }

        // Larger elements are more important
        val area = rect.width() * rect.height()
        if (area > (screenWidth * screenHeight) / 20) {
            score += WEIGHT_LARGE_SIZE
        }

        // Boost for common important labels
        val labelLower = label.lowercase()
        if (labelLower in listOf("settings", "search", "home", "profile", "menu", "send", "post")) {
            score += 15
        }

        return score.coerceIn(0, 100)
    }

    /**
     * Calculate quadrant (1-9 grid position)
     *
     * Grid layout:
     * 1 | 2 | 3
     * 4 | 5 | 6
     * 7 | 8 | 9
     */
    private fun calculateQuadrant(bounds: String?): Int {
        if (bounds == null) return 5 // Default to center

        val rect = parseBounds(bounds) ?: return 5

        val centerX = rect.centerX()
        val centerY = rect.centerY()

        val colWidth = screenWidth / GRID_COLS
        val rowHeight = screenHeight / GRID_ROWS

        val col = (centerX / colWidth).coerceIn(0, GRID_COLS - 1)
        val row = (centerY / rowHeight).coerceIn(0, GRID_ROWS - 1)

        return row * GRID_COLS + col + 1
    }

    /**
     * Calculate quadrant from Rect directly (1-9 grid position)
     *
     * Grid layout:
     * 1 | 2 | 3
     * 4 | 5 | 6
     * 7 | 8 | 9
     */
    private fun calculateQuadrantFromRect(rect: Rect): Int {
        val centerX = rect.centerX()
        val centerY = rect.centerY()

        val colWidth = screenWidth / GRID_COLS
        val rowHeight = screenHeight / GRID_ROWS

        val col = (centerX / colWidth).coerceIn(0, GRID_COLS - 1)
        val row = (centerY / rowHeight).coerceIn(0, GRID_ROWS - 1)

        return row * GRID_COLS + col + 1
    }

    /**
     * Parse bounds string to Rect
     */
    private fun parseBounds(bounds: String): Rect? {
        return try {
            val parts = bounds.split(",")
            if (parts.size == 4) {
                Rect(
                    parts[0].toInt(),
                    parts[1].toInt(),
                    parts[2].toInt(),
                    parts[3].toInt()
                )
            } else null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Normalize label for consistency
     */
    private fun normalizeLabel(label: String): String {
        return label
            .trim()
            .replace(Regex("\\s+"), " ") // Collapse whitespace
            .replace(Regex("[^a-zA-Z0-9\\s]"), "") // Remove special chars
            .take(50) // Limit length
    }

    /**
     * Detect navigation path type
     */
    private fun detectNavigationPathType(
        fromScreen: String,
        toScreen: String,
        triggerLabel: String
    ): NavigationPathType {
        val labelLower = triggerLabel.lowercase()

        return when {
            labelLower.contains("back") || labelLower.contains("return") ||
            labelLower.contains("close") || labelLower.contains("dismiss") ->
                NavigationPathType.BACK

            labelLower.contains("tab") || fromScreen == toScreen ->
                NavigationPathType.LATERAL

            screens[toScreen]?.screenType == ScreenType.DIALOG ->
                NavigationPathType.MODAL

            else -> NavigationPathType.FORWARD
        }
    }

    /**
     * Update action candidates from elements
     */
    private fun updateActionCandidates(screenId: String, elements: List<QuantizedElement>) {
        for (element in elements) {
            // Only track highly important elements as action candidates
            if (element.importance >= 50) {
                val key = "${element.label}_${element.semanticType.code}"
                val keywords = generateKeywords(element.label)

                val action = QuantizedAction(
                    label = element.label,
                    elementId = element.elementId,
                    screenId = screenId,
                    confidence = element.importance,
                    keywords = keywords
                )

                // Keep highest confidence version
                val existing = actionCandidates[key]
                if (existing == null || existing.confidence < action.confidence) {
                    actionCandidates[key] = action
                }
            }
        }
    }

    /**
     * Generate keywords for action matching
     */
    private fun generateKeywords(label: String): List<String> {
        val keywords = mutableListOf<String>()
        val labelLower = label.lowercase()

        // Add the label itself
        keywords.add(labelLower)

        // Add individual words
        keywords.addAll(labelLower.split(" ").filter { it.length > 2 })

        // Add common synonyms
        when {
            labelLower.contains("setting") -> keywords.addAll(listOf("preferences", "options", "config"))
            labelLower.contains("search") -> keywords.addAll(listOf("find", "lookup", "browse"))
            labelLower.contains("home") -> keywords.addAll(listOf("main", "start", "dashboard"))
            labelLower.contains("profile") -> keywords.addAll(listOf("account", "user", "me"))
            labelLower.contains("send") -> keywords.addAll(listOf("submit", "post", "share"))
            labelLower.contains("back") -> keywords.addAll(listOf("return", "previous", "go back"))
        }

        return keywords.distinct()
    }

    /**
     * Update semantic clusters
     */
    private fun updateSemanticClusters(screenId: String, elements: List<QuantizedElement>) {
        for (element in elements) {
            val clusterName = when {
                element.semanticType.code.startsWith("N") -> "navigation"
                element.semanticType.code.startsWith("A") -> "actions"
                element.semanticType.code.startsWith("I") -> "input"
                element.semanticType.code.startsWith("C") && element.semanticType != SemanticElementType.CTR_LIST ->
                    "content"
                else -> null
            }

            clusterName?.let {
                semanticClusters.getOrPut(it) { mutableSetOf() }.add(element.elementId)
            }
        }
    }

    /**
     * Build semantic clusters from accumulated data
     */
    private fun buildSemanticClusters(): List<SemanticCluster> {
        return semanticClusters.map { (name, elementIds) ->
            val clusterType = when (name) {
                "navigation" -> ClusterType.NAVIGATION
                "actions" -> ClusterType.ACTIONS
                "input" -> ClusterType.INPUT
                "content" -> ClusterType.CONTENT
                else -> ClusterType.CONTENT
            }

            // Find screens containing these elements
            val containingScreens = screenElements.filter { (_, elements) ->
                elements.any { it.elementId in elementIds }
            }.keys.toList()

            SemanticCluster(
                name = name,
                type = clusterType,
                elements = elementIds.toList(),
                screens = containingScreens
            )
        }
    }

    /**
     * Calculate navigation complexity (1-5)
     */
    private fun calculateNavigationComplexity(): Int {
        if (screens.isEmpty()) return 1

        val avgDepth = screens.values.map { it.depth }.average()
        val pathsPerScreen = if (screens.size > 0) {
            navigation.size.toFloat() / screens.size
        } else 0f

        // Complexity based on depth and path density
        return when {
            avgDepth > 4 || pathsPerScreen > 3 -> 5
            avgDepth > 3 || pathsPerScreen > 2 -> 4
            avgDepth > 2 || pathsPerScreen > 1.5 -> 3
            avgDepth > 1 || pathsPerScreen > 1 -> 2
            else -> 1
        }
    }

    /**
     * Calculate exploration coverage
     */
    private fun calculateCoverage(): Float {
        if (screens.isEmpty()) return 0f

        // Coverage based on:
        // - Number of screens explored
        // - Navigation paths discovered
        // - Element discovery rate

        val screenFactor = (screens.size.toFloat() / 20f).coerceAtMost(1f) * 40f // Max 40%
        val navFactor = (navigation.size.toFloat() / screens.size).coerceAtMost(2f) * 30f // Max 30%
        val elementFactor = if (totalElementsProcessed > 0) {
            (totalElementsProcessed.toFloat() / (screens.size * 20)).coerceAtMost(1f) * 30f // Max 30%
        } else 0f

        return (screenFactor + navFactor + elementFactor).coerceIn(0f, 100f)
    }

    /**
     * Get current statistics
     */
    fun getStats(): QuantizerStats = QuantizerStats(
        screensProcessed = totalScreensProcessed,
        elementsProcessed = totalElementsProcessed,
        navigationPaths = navigation.size,
        actionCandidates = actionCandidates.size,
        semanticClusters = semanticClusters.size
    )

    /**
     * Clear all state
     */
    suspend fun reset() = mutex.withLock {
        screens.clear()
        screenElements.clear()
        navigation.clear()
        actionCandidates.clear()
        semanticClusters.clear()
        totalElementsProcessed = 0
        totalScreensProcessed = 0
        Log.i(TAG, "Quantizer state reset")
    }
}

/**
 * Quantizer statistics
 */
data class QuantizerStats(
    val screensProcessed: Int,
    val elementsProcessed: Int,
    val navigationPaths: Int,
    val actionCandidates: Int,
    val semanticClusters: Int
)
