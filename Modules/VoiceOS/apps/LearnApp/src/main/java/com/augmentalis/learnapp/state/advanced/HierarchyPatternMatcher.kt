/**
 * HierarchyPatternMatcher.kt - Hierarchy-aware UI pattern analysis
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Analyzes UI hierarchy to distinguish between similar patterns at different
 * levels. For example: login dialog vs full-screen login, loading at root vs
 * loading in a specific section, progress indicator depth analysis.
 */
package com.augmentalis.learnapp.state.advanced

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.learnapp.state.AppState

/**
 * Hierarchy context for a detected pattern
 */
data class HierarchyContext(
    val depth: Int,
    val isRootLevel: Boolean,
    val parentClasses: List<String>,
    val siblingCount: Int,
    val hasDialogAncestor: Boolean,
    val hasScrollableAncestor: Boolean
)

/**
 * Hierarchy-aware pattern match result
 */
data class HierarchyPatternMatch(
    val state: AppState,
    val confidence: Float,
    val context: HierarchyContext,
    val scope: PatternScope,
    val indicators: List<String>
)

/**
 * Pattern scope in UI hierarchy
 */
enum class PatternScope {
    FULL_SCREEN,      // Root-level, affects entire screen
    DIALOG,           // Within dialog container
    SECTION,          // Within specific section/fragment
    COMPONENT,        // Individual component (e.g., button loading state)
    NESTED            // Nested within multiple containers
}

/**
 * Hierarchy analysis result
 */
data class HierarchyAnalysisResult(
    val maxDepth: Int,
    val totalNodes: Int,
    val dialogDepth: Int?,       // Depth of dialog if present
    val scrollableDepth: Int?,   // Depth of scrollable container
    val rootLevelComponents: Int
)

/**
 * Analyzes UI patterns with hierarchy awareness
 *
 * Distinguishes between patterns at different hierarchy levels to provide
 * more accurate state detection. For example, a loading indicator at root
 * suggests LOADING state, while deep in the tree suggests partial loading.
 */
class HierarchyPatternMatcher {

    companion object {
        private const val TAG = "HierarchyPatternMatcher"

        // Depth thresholds for pattern scope
        private const val ROOT_LEVEL_DEPTH = 3
        private const val SHALLOW_DEPTH = 5
        private const val MODERATE_DEPTH = 10

        // Dialog container class patterns
        private val DIALOG_CLASSES = setOf(
            "Dialog",
            "AlertDialog",
            "DialogFragment",
            "BottomSheet"
        )

        // Scrollable container patterns
        private val SCROLLABLE_CLASSES = setOf(
            "ScrollView",
            "NestedScrollView",
            "RecyclerView",
            "ListView",
            "ViewPager"
        )

        // Confidence adjustments based on scope
        private val SCOPE_CONFIDENCE_MULTIPLIERS = mapOf(
            PatternScope.FULL_SCREEN to 1.0f,
            PatternScope.DIALOG to 0.9f,
            PatternScope.SECTION to 0.7f,
            PatternScope.COMPONENT to 0.5f,
            PatternScope.NESTED to 0.6f
        )
    }

    /**
     * Analyze UI hierarchy structure
     *
     * @param rootNode Root of accessibility tree
     * @return Hierarchy analysis result
     */
    fun analyzeHierarchy(rootNode: AccessibilityNodeInfo?): HierarchyAnalysisResult {
        if (rootNode == null) {
            return HierarchyAnalysisResult(
                maxDepth = 0,
                totalNodes = 0,
                dialogDepth = null,
                scrollableDepth = null,
                rootLevelComponents = 0
            )
        }

        var maxDepth = 0
        var totalNodes = 0
        var dialogDepth: Int? = null
        var scrollableDepth: Int? = null
        var rootLevelComponents = 0

        fun traverse(node: AccessibilityNodeInfo, depth: Int) {
            totalNodes++
            maxDepth = maxOf(maxDepth, depth)

            if (depth <= ROOT_LEVEL_DEPTH) {
                rootLevelComponents++
            }

            val className = node.className?.toString() ?: ""

            // Detect dialog
            if (dialogDepth == null && DIALOG_CLASSES.any { className.contains(it) }) {
                dialogDepth = depth
            }

            // Detect scrollable container
            if (scrollableDepth == null && SCROLLABLE_CLASSES.any { className.contains(it) }) {
                scrollableDepth = depth
            }

            // Traverse children
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    traverse(child, depth + 1)
                }
            }
        }

        traverse(rootNode, 0)

        return HierarchyAnalysisResult(
            maxDepth = maxDepth,
            totalNodes = totalNodes,
            dialogDepth = dialogDepth,
            scrollableDepth = scrollableDepth,
            rootLevelComponents = rootLevelComponents
        )
    }

    /**
     * Get hierarchy context for a specific node
     *
     * @param node Node to analyze
     * @return Hierarchy context
     */
    fun getHierarchyContext(node: AccessibilityNodeInfo?): HierarchyContext {
        if (node == null) {
            return HierarchyContext(
                depth = 0,
                isRootLevel = false,
                parentClasses = emptyList(),
                siblingCount = 0,
                hasDialogAncestor = false,
                hasScrollableAncestor = false
            )
        }

        val depth = calculateNodeDepth(node)
        val parentClasses = collectParentClasses(node)
        val siblingCount = node.parent?.childCount ?: 0
        val hasDialogAncestor = hasAncestorOfType(node, DIALOG_CLASSES)
        val hasScrollableAncestor = hasAncestorOfType(node, SCROLLABLE_CLASSES)

        return HierarchyContext(
            depth = depth,
            isRootLevel = depth <= ROOT_LEVEL_DEPTH,
            parentClasses = parentClasses,
            siblingCount = siblingCount,
            hasDialogAncestor = hasDialogAncestor,
            hasScrollableAncestor = hasScrollableAncestor
        )
    }

    /**
     * Determine pattern scope based on hierarchy context
     *
     * @param context Hierarchy context
     * @return Pattern scope
     */
    fun determinePatternScope(context: HierarchyContext): PatternScope {
        return when {
            context.isRootLevel && !context.hasDialogAncestor -> PatternScope.FULL_SCREEN
            context.hasDialogAncestor -> PatternScope.DIALOG
            context.depth <= SHALLOW_DEPTH -> PatternScope.SECTION
            context.depth <= MODERATE_DEPTH -> PatternScope.COMPONENT
            else -> PatternScope.NESTED
        }
    }

    /**
     * Match pattern with hierarchy awareness
     *
     * @param node Node containing pattern
     * @param state Detected state
     * @param baseConfidence Base confidence before hierarchy adjustment
     * @param indicators Pattern indicators
     * @return Hierarchy-aware pattern match
     */
    fun matchPattern(
        node: AccessibilityNodeInfo?,
        state: AppState,
        baseConfidence: Float,
        indicators: List<String>
    ): HierarchyPatternMatch {
        val context = getHierarchyContext(node)
        val scope = determinePatternScope(context)
        val scopeMultiplier = SCOPE_CONFIDENCE_MULTIPLIERS[scope] ?: 1.0f
        val adjustedConfidence = (baseConfidence * scopeMultiplier).coerceIn(0f, 1f)

        return HierarchyPatternMatch(
            state = state,
            confidence = adjustedConfidence,
            context = context,
            scope = scope,
            indicators = indicators + "Scope: $scope (depth ${context.depth})"
        )
    }

    /**
     * Distinguish between full-screen and dialog patterns
     *
     * @param loginMatches List of login pattern matches
     * @return Classification of login context
     */
    fun distinguishLoginContext(loginMatches: List<HierarchyPatternMatch>): LoginContext {
        val hasFullScreen = loginMatches.any { it.scope == PatternScope.FULL_SCREEN }
        val hasDialog = loginMatches.any { it.scope == PatternScope.DIALOG }

        return when {
            hasFullScreen && !hasDialog -> LoginContext.FULL_SCREEN_LOGIN
            hasDialog && !hasFullScreen -> LoginContext.DIALOG_LOGIN
            hasFullScreen && hasDialog -> LoginContext.BOTH
            else -> LoginContext.UNKNOWN
        }
    }

    /**
     * Detect loading context (full app vs component)
     *
     * @param loadingNodes List of loading indicators with hierarchy context
     * @return Loading scope determination
     */
    fun detectLoadingContext(loadingNodes: List<Pair<AccessibilityNodeInfo, HierarchyContext>>): LoadingContext {
        if (loadingNodes.isEmpty()) return LoadingContext.NONE

        val rootLevelLoading = loadingNodes.any { it.second.isRootLevel }
        val deepLoading = loadingNodes.any { it.second.depth > MODERATE_DEPTH }
        val inDialog = loadingNodes.any { it.second.hasDialogAncestor }

        return when {
            rootLevelLoading && !inDialog -> LoadingContext.FULL_APP_LOADING
            inDialog -> LoadingContext.DIALOG_LOADING
            deepLoading -> LoadingContext.COMPONENT_LOADING
            else -> LoadingContext.SECTION_LOADING
        }
    }

    /**
     * Calculate depth of node in tree
     */
    private fun calculateNodeDepth(node: AccessibilityNodeInfo): Int {
        var depth = 0
        var current = node.parent

        while (current != null) {
            depth++
            val next = current.parent
            current = next
        }

        return depth
    }

    /**
     * Collect parent class names
     */
    private fun collectParentClasses(node: AccessibilityNodeInfo): List<String> {
        val classes = mutableListOf<String>()
        var current = node.parent

        while (current != null && classes.size < 5) {
            current.className?.toString()?.let { classes.add(it) }
            val next = current.parent
            current = next
        }

        return classes
    }

    /**
     * Check if node has ancestor of specific type
     */
    private fun hasAncestorOfType(node: AccessibilityNodeInfo, classPatterns: Set<String>): Boolean {
        var current = node.parent

        while (current != null) {
            val className = current.className?.toString() ?: ""
            if (classPatterns.any { className.contains(it) }) {
                return true
            }
            val next = current.parent
            current = next
        }

        return false
    }
}

/**
 * Login screen context
 */
enum class LoginContext {
    FULL_SCREEN_LOGIN,  // Dedicated login screen
    DIALOG_LOGIN,       // Login dialog over app
    BOTH,               // Both present (unusual)
    UNKNOWN
}

/**
 * Loading indicator context
 */
enum class LoadingContext {
    FULL_APP_LOADING,      // Entire app loading
    DIALOG_LOADING,        // Loading within dialog
    SECTION_LOADING,       // Section/fragment loading
    COMPONENT_LOADING,     // Individual component loading
    NONE
}
