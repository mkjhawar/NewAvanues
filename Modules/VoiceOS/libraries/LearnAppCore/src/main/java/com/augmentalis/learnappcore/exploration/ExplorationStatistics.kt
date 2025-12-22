/**
 * ExplorationStatistics.kt - Statistics calculator for exploration state
 *
 * Calculates exploration metrics and statistics.
 * Extracted from ExplorationState as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 2.1.0 (SOLID Refactoring)
 */

package com.augmentalis.learnappcore.exploration

import com.augmentalis.learnappcore.models.ElementInfo
import com.augmentalis.learnappcore.safety.DoNotClickReason

/**
 * Exploration Statistics Calculator
 *
 * Calculates various metrics from exploration state data.
 *
 * Responsibilities:
 * - Calculate exploration coverage
 * - Calculate average depth
 * - Generate ExplorationStats summary
 * - Compute progress percentage
 *
 * Single Responsibility: Statistics calculation
 *
 * Usage:
 * ```kotlin
 * val calculator = ExplorationStatistics()
 * val stats = calculator.calculateStats(explorationState)
 * val coverage = calculator.calculateCoverage(discoveredElements, clickedElements, dangerousElements)
 * ```
 */
class ExplorationStatistics {

    /**
     * Calculate complete exploration statistics.
     *
     * @param startTimestamp Exploration start time
     * @param discoveredElements Map of discovered elements (uuid -> element)
     * @param clickedElements Set of clicked element stable IDs
     * @param dangerousElements List of dangerous elements
     * @param screenFingerprints Map of screen fingerprints
     * @param navigationHistory List of navigation records
     * @param dynamicRegions List of dynamic regions
     * @param generatedCommands Map of generated commands
     * @param currentDepth Current exploration depth
     * @param maxDepth Maximum depth reached
     * @param depthHistory History of depth values
     * @return ExplorationStats summary
     */
    fun calculateStats(
        startTimestamp: Long,
        discoveredElements: Map<String, ElementInfo>,
        clickedElements: Set<String>,
        dangerousElements: List<Pair<ElementInfo, DoNotClickReason>>,
        screenFingerprints: Map<String, *>,
        navigationHistory: List<NavigationRecord>,
        dynamicRegions: List<*>,
        generatedCommands: Map<String, String>,
        currentDepth: Int,
        maxDepth: Int,
        depthHistory: List<Int>
    ): ExplorationStats {
        val duration = if (startTimestamp > 0) {
            System.currentTimeMillis() - startTimestamp
        } else 0

        val avgDepth = if (depthHistory.isNotEmpty()) {
            depthHistory.average().toFloat()
        } else {
            currentDepth.toFloat()
        }

        val coverage = calculateCoverage(
            discoveredElements.values.toList(),
            clickedElements,
            dangerousElements
        )

        return ExplorationStats(
            screensExplored = screenFingerprints.size,
            elementsDiscovered = discoveredElements.size,
            elementsClicked = clickedElements.size,
            commandsGenerated = generatedCommands.size,
            navigationCount = navigationHistory.size,
            dangerousElementsSkipped = dangerousElements.size,
            dynamicRegionsDetected = dynamicRegions.size,
            avgDepth = avgDepth,
            maxDepth = maxDepth,
            durationMs = duration,
            coverage = coverage
        )
    }

    /**
     * Calculate exploration coverage percentage.
     *
     * Coverage = (clicked elements / clickable elements) × 100
     * Excludes dangerous elements from denominator.
     *
     * @param discoveredElements List of all discovered elements
     * @param clickedElements Set of clicked element stable IDs
     * @param dangerousElements List of dangerous elements to exclude
     * @return Coverage percentage (0.0 - 100.0)
     */
    fun calculateCoverage(
        discoveredElements: List<ElementInfo>,
        clickedElements: Set<String>,
        dangerousElements: List<Pair<ElementInfo, DoNotClickReason>>
    ): Float {
        if (discoveredElements.isEmpty()) return 0f

        val dangerousStableIds = dangerousElements.map { it.first.stableId() }.toSet()

        val clickedCount = clickedElements.size.toFloat()
        val clickableCount = discoveredElements.count {
            it.isClickable && it.stableId() !in dangerousStableIds
        }.toFloat()

        return if (clickableCount > 0) {
            (clickedCount / clickableCount) * 100f
        } else {
            100f // If no clickable elements, consider 100% covered
        }
    }

    /**
     * Calculate exploration progress percentage.
     *
     * Progress considers multiple factors:
     * - Screen discovery (40%)
     * - Element coverage (40%)
     * - Command generation (20%)
     *
     * @param stats Current exploration stats
     * @param estimatedTotalScreens Estimated total screens in app
     * @return Progress percentage (0.0 - 100.0)
     */
    fun calculateProgress(
        stats: ExplorationStats,
        estimatedTotalScreens: Int = 20
    ): Float {
        // Screen discovery progress (capped at 100%)
        val screenProgress = (stats.screensExplored.toFloat() / estimatedTotalScreens.toFloat())
            .coerceAtMost(1.0f) * 40f

        // Coverage progress (already 0-100%)
        val coverageProgress = (stats.coverage / 100f) * 40f

        // Command generation progress (relative to elements)
        val commandProgress = if (stats.elementsDiscovered > 0) {
            (stats.commandsGenerated.toFloat() / stats.elementsDiscovered.toFloat())
                .coerceAtMost(1.0f) * 20f
        } else {
            0f
        }

        return screenProgress + coverageProgress + commandProgress
    }

    /**
     * Calculate average elements per screen.
     *
     * @param stats Exploration stats
     * @return Average elements per screen
     */
    fun calculateAvgElementsPerScreen(stats: ExplorationStats): Float {
        return if (stats.screensExplored > 0) {
            stats.elementsDiscovered.toFloat() / stats.screensExplored.toFloat()
        } else {
            0f
        }
    }

    /**
     * Calculate exploration efficiency.
     *
     * Efficiency = (commands generated / elements discovered) × 100
     *
     * @param stats Exploration stats
     * @return Efficiency percentage (0.0 - 100.0)
     */
    fun calculateEfficiency(stats: ExplorationStats): Float {
        return if (stats.elementsDiscovered > 0) {
            (stats.commandsGenerated.toFloat() / stats.elementsDiscovered.toFloat()) * 100f
        } else {
            0f
        }
    }

    /**
     * Calculate exploration rate (commands per minute).
     *
     * @param stats Exploration stats
     * @return Commands generated per minute
     */
    fun calculateRate(stats: ExplorationStats): Float {
        return if (stats.durationMs > 0) {
            val minutes = stats.durationMs / 60000f
            stats.commandsGenerated.toFloat() / minutes
        } else {
            0f
        }
    }

    /**
     * Estimate time remaining (minutes).
     *
     * Based on current rate and estimated remaining work.
     *
     * @param stats Current stats
     * @param estimatedTotalScreens Estimated total screens
     * @return Estimated minutes remaining
     */
    fun estimateTimeRemaining(
        stats: ExplorationStats,
        estimatedTotalScreens: Int = 20
    ): Float {
        val progress = calculateProgress(stats, estimatedTotalScreens)
        if (progress >= 100f || stats.durationMs == 0L) return 0f

        val remainingPercent = 100f - progress
        val elapsedMinutes = stats.durationMs / 60000f

        return (elapsedMinutes / progress) * remainingPercent
    }
}
