/**
 * ExplorationMetrics.kt - VUID metrics collection and tracking
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/exploration/ExplorationMetrics.kt
 *
 * Author: Manoj Jhawar (refactored by Claude)
 * Created: 2025-12-08
 * Refactored: 2026-01-15 (SOLID extraction from ExplorationEngine.kt)
 *
 * Single Responsibility: Manages VUID creation metrics, debug overlay updates,
 * and exploration statistics collection.
 *
 * Extracted from ExplorationEngine.kt to improve maintainability and testability.
 */

package com.augmentalis.voiceoscore.learnapp.exploration

import android.content.Context
import android.view.WindowManager
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDCreationMetricsCollector
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDCreationDebugOverlay
import com.augmentalis.voiceoscore.learnapp.metrics.VUIDMetricsRepository
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings

/**
 * Manages metrics collection and debug overlay for VUID exploration.
 *
 * This class is responsible for:
 * - Tracking VUID creation statistics
 * - Managing the debug overlay display
 * - Recording element filtering events
 * - Persisting metrics to the repository
 *
 * ## Usage Example
 *
 * ```kotlin
 * val metrics = ExplorationMetrics(context)
 *
 * // Start tracking for an app
 * metrics.startSession("com.instagram.android")
 *
 * // Track events
 * metrics.onElementDetected()
 * metrics.onVUIDCreated()
 * metrics.onElementFiltered(element, "Critical dangerous")
 *
 * // End session and save
 * metrics.endSession()
 * ```
 *
 * @property context Android context
 */
class ExplorationMetrics(
    private val context: Context
) {
    /**
     * Developer settings for conditional behavior
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    /**
     * VUID metrics collector for tracking creation stats
     */
    private val metricsCollector = VUIDCreationMetricsCollector()

    /**
     * Repository for persisting metrics
     */
    private val metricsRepository = VUIDMetricsRepository()

    /**
     * Debug overlay for visual metrics display
     */
    private val debugOverlay by lazy {
        VUIDCreationDebugOverlay(
            context,
            context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        )
    }

    /**
     * Current package being explored
     */
    private var currentPackage: String? = null

    /**
     * Start a new metrics collection session
     *
     * @param packageName Package being explored
     */
    fun startSession(packageName: String) {
        currentPackage = packageName
        metricsCollector.reset()
        debugOverlay.setMetricsCollector(metricsCollector)

        if (developerSettings.isDeveloperModeEnabled()) {
            debugOverlay.show(packageName)
        }

        android.util.Log.d(TAG, "Metrics session started for: $packageName")
    }

    /**
     * Track element detection event
     */
    fun onElementDetected() {
        metricsCollector.onElementDetected()
    }

    /**
     * Track VUID creation event
     */
    fun onVUIDCreated() {
        metricsCollector.onVUIDCreated()
    }

    /**
     * Track element filtering event
     *
     * @param element The element that was filtered
     * @param reason Reason for filtering
     */
    fun onElementFiltered(element: ElementInfo, reason: String) {
        metricsCollector.onElementFiltered(element, reason)
    }

    /**
     * Update progress display
     *
     * @param progressPercent Progress percentage (0-100)
     */
    fun updateProgress(progressPercent: Int) {
        debugOverlay.updateProgress(progressPercent.toFloat())
    }

    /**
     * End the metrics session and persist results
     *
     * @return Generated metrics report string, or null if no session active
     */
    fun endSession(): String? {
        val packageName = currentPackage ?: return null

        try {
            val metrics = metricsCollector.buildMetrics(packageName)

            // Log final report
            val report = metrics.toReportString()
            android.util.Log.i(TAG, "=== VUID CREATION METRICS ===")
            android.util.Log.i(TAG, report)

            // Save to database
            metricsRepository.saveMetrics(metrics)
            android.util.Log.d(TAG, "Metrics saved to database")

            return report
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to save metrics", e)
            return null
        } finally {
            hideOverlay()
            currentPackage = null
        }
    }

    /**
     * Hide the debug overlay
     */
    fun hideOverlay() {
        debugOverlay.hide()
    }

    /**
     * Get the underlying metrics collector for direct access
     *
     * @return The VUIDCreationMetricsCollector instance
     */
    fun getCollector(): VUIDCreationMetricsCollector = metricsCollector

    /**
     * Get the underlying metrics repository
     *
     * @return The VUIDMetricsRepository instance
     */
    fun getRepository(): VUIDMetricsRepository = metricsRepository

    /**
     * Check if developer mode is enabled
     *
     * @return true if debug overlay should be shown
     */
    fun isDeveloperModeEnabled(): Boolean = developerSettings.isDeveloperModeEnabled()

    companion object {
        private const val TAG = "ExplorationMetrics"
    }
}
