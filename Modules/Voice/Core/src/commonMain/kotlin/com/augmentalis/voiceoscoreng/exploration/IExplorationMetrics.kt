/**
 * IExplorationMetrics.kt - Metrics tracking interface
 *
 * Defines the contract for tracking exploration metrics.
 * Platform implementations handle UI overlays and persistence.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 * Updated: 2026-01-15 - Migrated from VUID to AVID nomenclature
 */

package com.augmentalis.voiceoscoreng.exploration

import com.augmentalis.voiceoscoreng.common.ElementInfo

/**
 * Interface for exploration metrics collection.
 *
 * Implementations track:
 * - Element detection counts
 * - AVID creation counts
 * - Filter reasons
 * - Debug overlay display
 */
interface IExplorationMetrics {

    /**
     * Start a new metrics collection session.
     *
     * @param packageName Package being explored
     */
    fun startSession(packageName: String)

    /**
     * Track element detection event.
     */
    fun onElementDetected()

    /**
     * Track AVID creation event.
     */
    fun onAvidCreated()

    /**
     * Legacy method - use onAvidCreated instead.
     */
    @Deprecated("Use onAvidCreated instead", ReplaceWith("onAvidCreated()"))
    fun onVUIDCreated() = onAvidCreated()

    /**
     * Track element filtering event.
     *
     * @param element The element that was filtered
     * @param reason Reason for filtering
     */
    fun onElementFiltered(element: ElementInfo, reason: String)

    /**
     * Update progress display.
     *
     * @param progressPercent Progress percentage (0-100)
     */
    fun updateProgress(progressPercent: Int)

    /**
     * End the metrics session and persist results.
     *
     * @return Generated metrics report string, or null if no session active
     */
    fun endSession(): String?

    /**
     * Hide any visible debug overlay.
     */
    fun hideOverlay()

    /**
     * Check if developer mode is enabled.
     */
    fun isDeveloperModeEnabled(): Boolean
}
