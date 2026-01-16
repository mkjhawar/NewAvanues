/**
 * ExplorationMetrics.kt - Android exploration metrics
 *
 * Manages AVID creation metrics, debug overlay updates,
 * and exploration statistics collection.
 *
 * @author Manoj Jhawar
 * @since 2026-01-15
 * Updated: 2026-01-15 - Migrated from VUID to AVID nomenclature
 */

package com.augmentalis.voiceoscoreng.exploration

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscoreng.common.ElementInfo
import java.util.concurrent.atomic.AtomicInteger

/**
 * Android implementation of exploration metrics.
 *
 * Tracks:
 * - Element detection counts
 * - AVID creation counts
 * - Filter reasons
 *
 * @property context Android context for overlays
 * @property config Exploration configuration
 */
class ExplorationMetrics(
    private val context: Context,
    private val config: ExplorationConfig = ExplorationConfig.DEFAULT
) : IExplorationMetrics {

    private var currentPackage: String? = null
    private var sessionStartTime: Long = 0L

    private val elementsDetected = AtomicInteger(0)
    private val avidsCreated = AtomicInteger(0)
    private val elementsFiltered = AtomicInteger(0)
    private val filterReasons = mutableMapOf<String, Int>()

    override fun startSession(packageName: String) {
        currentPackage = packageName
        sessionStartTime = System.currentTimeMillis()

        // Reset counters
        elementsDetected.set(0)
        avidsCreated.set(0)
        elementsFiltered.set(0)
        filterReasons.clear()

        Log.d(TAG, "Metrics session started for: $packageName")
    }

    override fun onElementDetected() {
        elementsDetected.incrementAndGet()
    }

    override fun onAvidCreated() {
        avidsCreated.incrementAndGet()
    }

    @Deprecated("Use onAvidCreated instead", ReplaceWith("onAvidCreated()"))
    override fun onVUIDCreated() = onAvidCreated()

    override fun onElementFiltered(element: ElementInfo, reason: String) {
        elementsFiltered.incrementAndGet()
        filterReasons[reason] = filterReasons.getOrDefault(reason, 0) + 1
    }

    override fun updateProgress(progressPercent: Int) {
        // In full implementation, update debug overlay
        if (config.developerMode) {
            Log.d(TAG, "Progress: $progressPercent%")
        }
    }

    override fun endSession(): String? {
        val packageName = currentPackage ?: return null
        val duration = System.currentTimeMillis() - sessionStartTime

        val report = buildString {
            appendLine("=== EXPLORATION METRICS ===")
            appendLine("Package: $packageName")
            appendLine("Duration: ${duration / 1000}s")
            appendLine("Elements Detected: ${elementsDetected.get()}")
            appendLine("AVIDs Created: ${avidsCreated.get()}")
            appendLine("Elements Filtered: ${elementsFiltered.get()}")
            if (filterReasons.isNotEmpty()) {
                appendLine("Filter Reasons:")
                filterReasons.forEach { (reason, count) ->
                    appendLine("  - $reason: $count")
                }
            }
        }

        Log.i(TAG, report)

        hideOverlay()
        currentPackage = null

        return report
    }

    override fun hideOverlay() {
        // In full implementation, hide debug overlay
    }

    override fun isDeveloperModeEnabled(): Boolean = config.developerMode

    /**
     * Get current metrics snapshot
     */
    fun getSnapshot(): MetricsSnapshot {
        return MetricsSnapshot(
            packageName = currentPackage ?: "",
            durationMs = if (sessionStartTime > 0) System.currentTimeMillis() - sessionStartTime else 0,
            elementsDetected = elementsDetected.get(),
            avidsCreated = avidsCreated.get(),
            elementsFiltered = elementsFiltered.get(),
            filterReasons = filterReasons.toMap()
        )
    }

    companion object {
        private const val TAG = "ExplorationMetrics"
    }
}

/**
 * Snapshot of metrics at a point in time
 */
data class MetricsSnapshot(
    val packageName: String,
    val durationMs: Long,
    val elementsDetected: Int,
    val avidsCreated: Int,
    val elementsFiltered: Int,
    val filterReasons: Map<String, Int>
)
