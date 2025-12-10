/**
 * ExplorationStrategy.kt - Defines exploration strategy
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/exploration/ExplorationStrategy.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Strategy pattern for app exploration algorithms
 */

package com.augmentalis.learnapp.exploration

import com.augmentalis.learnapp.models.ElementInfo

/**
 * Exploration Strategy
 *
 * Defines strategy for ordering and selecting elements during exploration.
 * Default implementation uses Depth-First Search (DFS).
 *
 * ## Usage Example
 *
 * ```kotlin
 * val strategy = DFSExplorationStrategy()
 *
 * val elements = listOf(...) // Safe clickable elements
 * val ordered = strategy.orderElements(elements)
 *
 * // Explore in order
 * ordered.forEach { element ->
 *     clickAndExplore(element)
 * }
 * ```
 *
 * @since 1.0.0
 */
interface ExplorationStrategy {

    /**
     * Order elements for exploration
     *
     * Determines the order in which elements should be explored.
     *
     * @param elements List of safe clickable elements
     * @return Ordered list of elements
     */
    fun orderElements(elements: List<ElementInfo>): List<ElementInfo>

    /**
     * Check if element should be explored
     *
     * Additional filtering beyond safe/dangerous classification.
     *
     * @param element Element to check
     * @return true if should explore
     */
    fun shouldExplore(element: ElementInfo): Boolean {
        return true  // Default: explore all safe elements
    }

    /**
     * Get max depth limit
     *
     * @return Max DFS depth
     */
    fun getMaxDepth(): Int {
        return 50
    }

    /**
     * Get max exploration time (milliseconds)
     *
     * @return Max time in milliseconds
     */
    fun getMaxExplorationTime(): Long {
        return 30 * 60 * 1000L  // 30 minutes
    }
}

/**
 * DFS Exploration Strategy
 *
 * Depth-First Search strategy - explores elements in order found,
 * going deep before exploring siblings.
 *
 * @since 1.0.0
 */
class DFSExplorationStrategy : ExplorationStrategy {

    /**
     * Order elements (DFS keeps original order)
     *
     * @param elements List of elements
     * @return Ordered list (unchanged for DFS)
     */
    override fun orderElements(elements: List<ElementInfo>): List<ElementInfo> {
        // DFS explores elements in the order they appear
        // Prioritize buttons over other elements
        val buttons = elements.filter { it.isButton() }
        val others = elements.filter { !it.isButton() }

        return buttons + others
    }

    override fun getMaxDepth(): Int = 50

    override fun getMaxExplorationTime(): Long = 30 * 60 * 1000L  // 30 minutes
}

/**
 * BFS Exploration Strategy
 *
 * Breadth-First Search strategy - explores all elements at current level
 * before going deeper.
 *
 * @since 1.0.0
 */
class BFSExplorationStrategy : ExplorationStrategy {

    /**
     * Order elements (BFS would require queue-based exploration)
     *
     * Note: True BFS requires different traversal algorithm,
     * this just orders elements by importance.
     *
     * @param elements List of elements
     * @return Ordered list
     */
    override fun orderElements(elements: List<ElementInfo>): List<ElementInfo> {
        // Prioritize by element type
        return elements.sortedBy { element ->
            when {
                element.isButton() -> 0
                element.hasMeaningfulContent() -> 1
                else -> 2
            }
        }
    }

    override fun getMaxDepth(): Int = 50

    override fun getMaxExplorationTime(): Long = 30 * 60 * 1000L
}

/**
 * Prioritized Exploration Strategy
 *
 * Explores elements based on priority heuristics:
 * 1. Buttons with meaningful text
 * 2. Clickable images (icons)
 * 3. Other clickable elements
 *
 * @since 1.0.0
 */
class PrioritizedExplorationStrategy : ExplorationStrategy {

    /**
     * Order elements by priority
     *
     * @param elements List of elements
     * @return Ordered list
     */
    override fun orderElements(elements: List<ElementInfo>): List<ElementInfo> {
        return elements.sortedBy { calculatePriority(it) }
    }

    /**
     * Calculate priority score (lower = higher priority)
     *
     * @param element Element to score
     * @return Priority score
     */
    private fun calculatePriority(element: ElementInfo): Int {
        var priority = 100

        // Buttons have high priority
        if (element.isButton()) {
            priority -= 50
        }

        // Elements with text have higher priority
        if (element.text.isNotBlank()) {
            priority -= 20
        }

        // Elements with content description
        if (element.contentDescription.isNotBlank()) {
            priority -= 10
        }

        return priority
    }

    override fun getMaxDepth(): Int = 50

    override fun getMaxExplorationTime(): Long = 30 * 60 * 1000L
}
