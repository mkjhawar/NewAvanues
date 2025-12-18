/**
 * ExplorationStrategy.kt - Strategy pattern for app exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Defines exploration strategy for discovering app screens.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import com.augmentalis.voiceoscore.learnapp.models.ElementInfo

/**
 * Exploration Strategy Interface
 *
 * Strategy pattern for determining exploration order and priorities.
 */
interface ExplorationStrategy {
    /**
     * Get next element to explore from list
     */
    fun getNextElement(elements: List<ElementInfo>): ElementInfo?

    /**
     * Prioritize elements for exploration
     */
    fun prioritize(elements: List<ElementInfo>): List<ElementInfo>

    /**
     * Should continue exploring this screen
     */
    fun shouldContinue(explored: Int, total: Int): Boolean

    /**
     * Maximum depth to explore
     */
    val maxDepth: Int

    /**
     * Strategy name for logging
     */
    val name: String
}

/**
 * Depth-First Exploration Strategy
 *
 * Explores elements depth-first, prioritizing deeper navigation.
 */
class DFSExplorationStrategy(
    override val maxDepth: Int = 10
) : ExplorationStrategy {

    override val name: String = "DFS"

    override fun getNextElement(elements: List<ElementInfo>): ElementInfo? {
        return elements.firstOrNull()
    }

    override fun prioritize(elements: List<ElementInfo>): List<ElementInfo> {
        // Prioritize clickable elements, then by position
        return elements.sortedWith(
            compareByDescending<ElementInfo> { it.isClickable }
                .thenBy { it.bounds.top }
                .thenBy { it.bounds.left }
        )
    }

    override fun shouldContinue(explored: Int, total: Int): Boolean {
        return explored < total
    }
}

/**
 * Breadth-First Exploration Strategy
 *
 * Explores all elements on current screen before navigating deeper.
 */
class BFSExplorationStrategy(
    override val maxDepth: Int = 10
) : ExplorationStrategy {

    override val name: String = "BFS"

    override fun getNextElement(elements: List<ElementInfo>): ElementInfo? {
        return elements.firstOrNull()
    }

    override fun prioritize(elements: List<ElementInfo>): List<ElementInfo> {
        // Sort by visual position (top to bottom, left to right)
        return elements.sortedWith(
            compareBy<ElementInfo> { it.bounds.top }
                .thenBy { it.bounds.left }
        )
    }

    override fun shouldContinue(explored: Int, total: Int): Boolean {
        return explored < total
    }
}

/**
 * Hybrid Exploration Strategy
 *
 * Combines DFS and BFS based on screen complexity.
 */
class HybridExplorationStrategy(
    override val maxDepth: Int = 10,
    private val complexityThreshold: Int = 50
) : ExplorationStrategy {

    override val name: String = "Hybrid"

    override fun getNextElement(elements: List<ElementInfo>): ElementInfo? {
        return elements.firstOrNull()
    }

    override fun prioritize(elements: List<ElementInfo>): List<ElementInfo> {
        return if (elements.size > complexityThreshold) {
            // Use BFS-style for complex screens
            elements.sortedWith(
                compareBy<ElementInfo> { it.bounds.top }
                    .thenBy { it.bounds.left }
            )
        } else {
            // Use DFS-style for simpler screens
            elements.sortedWith(
                compareByDescending<ElementInfo> { it.isClickable }
                    .thenBy { it.bounds.top }
            )
        }
    }

    override fun shouldContinue(explored: Int, total: Int): Boolean {
        return explored < total
    }
}
