/**
 * VUIDCreationDebugOverlay.kt - Real-time VUID creation stats overlay
 * Path: VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/metrics/VUIDCreationDebugOverlay.kt
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-08
 * Feature: LearnApp VUID Creation Fix - Phase 3 (Observability)
 *
 * Purpose:
 * - Display real-time VUID creation metrics during exploration
 * - Show detection rate, creation rate, and filtering stats
 * - Update every 1 second without blocking user interaction
 * - Provide visual feedback for debugging VUID creation issues
 *
 * Part of: LearnApp-VUID-Creation-Fix-Spec-5081218-V1.md (Phase 3)
 */

package com.augmentalis.voiceoscore.learnapp.metrics

import android.content.Context
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.augmentalis.voiceoscore.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug overlay showing real-time VUID creation metrics
 *
 * Displays a floating overlay during exploration that shows:
 * - Elements detected
 * - VUIDs created
 * - Creation rate (percentage)
 * - Filtered count
 * - Status icon (⏳ running, ✅ good, ⚠️ warning, ❌ error)
 *
 * ## Features
 * - Non-blocking overlay (FLAG_NOT_FOCUSABLE)
 * - Auto-updates every 1 second
 * - Color-coded stats (green=detected, blue=created, yellow=rate)
 * - Status emoji based on creation rate threshold
 *
 * ## Usage
 * ```kotlin
 * val overlay = VUIDCreationDebugOverlay(context, windowManager)
 * overlay.show()
 *
 * // During exploration (auto-updates every 1 second)
 * // ... exploration happens ...
 *
 * // Manual update
 * overlay.updateStats(metrics)
 *
 * // When done
 * overlay.hide()
 * ```
 *
 * @param context Android context
 * @param windowManager Window manager for overlay display
 *
 * @since 2025-12-08 (Phase 3: Observability)
 */
class VUIDCreationDebugOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {

    /**
     * Overlay view (inflated from XML with proper parent)
     */
    private val overlayView: View by lazy {
        val parent = android.widget.FrameLayout(context)
        LayoutInflater.from(context).inflate(
            R.layout.learnapp_overlay_vuid_creation,
            parent,
            false
        )
    }

    /**
     * View references
     */
    private val txtAppName: TextView by lazy { overlayView.findViewById(R.id.txt_app_name) }
    private val txtDetected: TextView by lazy { overlayView.findViewById(R.id.txt_detected) }
    private val txtCreated: TextView by lazy { overlayView.findViewById(R.id.txt_created) }
    private val txtRate: TextView by lazy { overlayView.findViewById(R.id.txt_rate) }
    private val txtStatus: TextView by lazy { overlayView.findViewById(R.id.txt_status) }
    private val txtFiltered: TextView by lazy { overlayView.findViewById(R.id.txt_filtered) }
    private val txtTopFiltered: TextView by lazy { overlayView.findViewById(R.id.txt_top_filtered) }
    private val txtTimestamp: TextView by lazy { overlayView.findViewById(R.id.txt_timestamp) }

    /**
     * Auto-update handler
     */
    private val updateHandler = Handler(Looper.getMainLooper())
    private var metricsCollector: VUIDCreationMetricsCollector? = null
    private var isShowing = false

    /**
     * Date formatter for timestamp
     */
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    /**
     * Auto-update runnable (runs every 1 second)
     */
    private val autoUpdateRunnable = object : Runnable {
        override fun run() {
            if (isShowing) {
                metricsCollector?.let { collector ->
                    val (detected, created, rate) = collector.getCurrentStats()
                    updateStatsInternal(detected, created, rate)
                }
                updateHandler.postDelayed(this, UPDATE_INTERVAL_MS)
            }
        }
    }

    /**
     * Show overlay on screen
     *
     * @param packageName Optional package name to display
     */
    fun show(packageName: String? = null) {
        if (isShowing) return

        // Set app name if provided
        if (packageName != null) {
            txtAppName.text = "App: ${getAppName(packageName)}"
        }

        // Window parameters (non-blocking overlay)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, // Non-interactive
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = 16 // 16dp from right edge
            y = 100 // 100dp from top
        }

        // Add view to window
        try {
            windowManager.addView(overlayView, params)
            isShowing = true

            // Initial state
            txtStatus.text = "⏳"
            updateTimestamp()

            // Start auto-updates
            updateHandler.post(autoUpdateRunnable)

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to show overlay", e)
        }
    }

    /**
     * Hide overlay from screen
     */
    fun hide() {
        if (!isShowing) return

        try {
            // Stop auto-updates
            updateHandler.removeCallbacks(autoUpdateRunnable)

            // Remove view
            windowManager.removeView(overlayView)
            isShowing = false

        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to hide overlay", e)
        }
    }

    /**
     * Set metrics collector for auto-updates
     *
     * @param collector Metrics collector to read from
     */
    fun setMetricsCollector(collector: VUIDCreationMetricsCollector) {
        this.metricsCollector = collector
    }

    /**
     * Update stats display with metrics object
     *
     * @param metrics Complete metrics object
     */
    fun updateStats(metrics: VUIDCreationMetrics) {
        if (!isShowing) return

        updateStatsInternal(
            metrics.elementsDetected,
            metrics.vuidsCreated,
            metrics.creationRate
        )

        // Update app name if changed
        txtAppName.text = "App: ${getAppName(metrics.packageName)}"

        // Update filtered stats
        txtFiltered.text = metrics.filteredCount.toString()

        // Show top filtered type if significant
        if (metrics.filteredByType.isNotEmpty()) {
            val topType = metrics.filteredByType.maxByOrNull { it.value }
            if (topType != null && topType.value > 5) {
                txtTopFiltered.text = "Most: ${shortenClassName(topType.key)} (${topType.value})"
                txtTopFiltered.visibility = View.VISIBLE
            } else {
                txtTopFiltered.visibility = View.GONE
            }
        }
    }

    /**
     * Internal stats update (shared logic)
     */
    private fun updateStatsInternal(detected: Int, created: Int, rate: Double) {
        if (!isShowing) return

        // Update text
        txtDetected.text = detected.toString()
        txtCreated.text = created.toString()
        txtRate.text = "${(rate * 100).toInt()}%"

        // Update status icon based on creation rate
        txtStatus.text = when {
            detected == 0 -> "⏳" // Loading
            rate >= 0.95 -> "✅"  // Excellent (95%+)
            rate >= 0.80 -> "⚠️"  // Warning (80-95%)
            else -> "❌"          // Error (<80%)
        }

        // Update timestamp
        updateTimestamp()
    }

    /**
     * Update last-updated timestamp
     */
    private fun updateTimestamp() {
        txtTimestamp.text = "Updated: ${timeFormat.format(Date())}"
    }

    /**
     * Get short app name from package
     */
    private fun getAppName(packageName: String): String {
        return try {
            val pm = context.packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    /**
     * Shorten class names for display
     */
    private fun shortenClassName(className: String): String {
        return className.substringAfterLast('.').removeSuffix("Layout").removeSuffix("View")
    }

    /**
     * Check if overlay is currently showing
     */
    fun isShowing(): Boolean = isShowing

    companion object {
        private const val TAG = "VUIDCreationDebugOverlay"

        /**
         * Update interval in milliseconds (1 second)
         */
        private const val UPDATE_INTERVAL_MS = 1000L
    }
}

/**
 * Extension function to create overlay from context
 */
fun Context.createVUIDDebugOverlay(): VUIDCreationDebugOverlay {
    val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    return VUIDCreationDebugOverlay(this, windowManager)
}
