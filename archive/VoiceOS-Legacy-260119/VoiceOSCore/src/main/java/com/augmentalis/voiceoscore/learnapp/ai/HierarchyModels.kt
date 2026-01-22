/**
 * HierarchyModels.kt - Data models for visual hierarchy map generation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: AI-Assisted Implementation
 * Created: 2025-12-30
 *
 * Data structures for representing app navigation hierarchies in various
 * export formats (ASCII tree, JSON, Mermaid diagrams).
 *
 * Part of Voice Command AI Integration feature
 */
package com.augmentalis.voiceoscore.learnapp.ai

/**
 * App Hierarchy
 *
 * Complete hierarchical representation of an app's navigation structure.
 * Generated from learned app data including screens, elements, and navigation edges.
 *
 * ## Usage:
 * ```kotlin
 * val hierarchy = AppHierarchy(
 *     packageName = "com.android.settings",
 *     appName = "Settings",
 *     nodes = listOf(homeNode, networkNode, wifiNode),
 *     edges = listOf(homeToNetwork, networkToWifi),
 *     stats = HierarchyStats(...)
 * )
 *
 * // Export to different formats
 * val ascii = generator.toAsciiTree(hierarchy)
 * val mermaid = generator.toMermaid(hierarchy)
 * ```
 *
 * @property packageName Package name (e.g., com.android.settings)
 * @property appName Human-readable app name (e.g., Settings)
 * @property nodes List of all screen nodes in the hierarchy
 * @property edges List of navigation edges between screens
 * @property stats Statistics about the hierarchy
 * @property generatedAt Timestamp when hierarchy was generated
 */
data class AppHierarchy(
    val packageName: String,
    val appName: String,
    val nodes: List<HierarchyNode>,
    val edges: List<HierarchyEdge>,
    val stats: HierarchyStats,
    val generatedAt: Long = System.currentTimeMillis()
)

/**
 * Hierarchy Node
 *
 * Represents a single screen in the hierarchy with its elements and metadata.
 *
 * @property screenHash Unique screen identifier (SHA-256 hash)
 * @property activityName Android activity name (e.g., MainActivity)
 * @property displayName Human-readable screen name for display
 * @property elements List of actionable elements on this screen
 * @property depth Navigation depth from root (0 = home screen)
 */
data class HierarchyNode(
    val screenHash: String,
    val activityName: String?,
    val displayName: String,
    val elements: List<HierarchyElement>,
    val depth: Int
)

/**
 * Hierarchy Element
 *
 * Represents an actionable element on a screen within the hierarchy.
 *
 * @property uuid Stable element identifier (UUID)
 * @property label User-visible label (text or contentDescription)
 * @property type Element type (button, textField, imageButton, etc.)
 * @property isNavigational Whether this element triggers screen navigation
 */
data class HierarchyElement(
    val uuid: String,
    val label: String,
    val type: String,
    val isNavigational: Boolean
)

/**
 * Hierarchy Edge
 *
 * Represents a navigation transition between two screens.
 *
 * @property fromScreen Source screen hash
 * @property toScreen Destination screen hash
 * @property viaElement UUID of element that triggers navigation
 * @property label Human-readable label for the transition
 */
data class HierarchyEdge(
    val fromScreen: String,
    val toScreen: String,
    val viaElement: String,
    val label: String
)

/**
 * Hierarchy Stats
 *
 * Statistics about the generated hierarchy.
 *
 * @property totalScreens Total number of screens in the hierarchy
 * @property totalElements Total number of actionable elements across all screens
 * @property totalEdges Total number of navigation edges
 * @property maxDepth Maximum navigation depth from root
 * @property avgElementsPerScreen Average number of elements per screen
 */
data class HierarchyStats(
    val totalScreens: Int,
    val totalElements: Int,
    val totalEdges: Int,
    val maxDepth: Int,
    val avgElementsPerScreen: Float
) {
    override fun toString(): String {
        return """
            Hierarchy Stats:
            - Screens: $totalScreens
            - Elements: $totalElements
            - Edges: $totalEdges
            - Max Depth: $maxDepth
            - Avg Elements/Screen: ${"%.1f".format(avgElementsPerScreen)}
        """.trimIndent()
    }
}

/**
 * Export Format
 *
 * Supported export formats for hierarchy visualization.
 */
enum class ExportFormat {
    /** ASCII tree format (text-based hierarchy) */
    ASCII,
    /** JSON format (machine-readable) */
    JSON,
    /** Mermaid diagram format (for documentation) */
    MERMAID
}
