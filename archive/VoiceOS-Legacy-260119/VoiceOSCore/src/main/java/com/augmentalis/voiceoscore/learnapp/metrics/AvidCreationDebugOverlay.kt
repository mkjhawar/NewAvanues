/**
 * AvidCreationDebugOverlay.kt - Debug overlay for Avid creation
 *
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.learnapp.metrics

import android.content.Context
import android.view.WindowManager

/**
 * Avid Creation Debug Overlay
 *
 * Shows debug information during Avid creation
 */
class AvidCreationDebugOverlay(
    private val context: Context,
    private val windowManager: WindowManager
) {

    /**
     * Set metrics collector
     */
    fun setMetricsCollector(collector: AvidCreationMetricsCollector) {
        // Stub implementation
    }

    /**
     * Show overlay for specific package
     */
    fun show(packageName: String) {
        // Stub implementation
    }

    /**
     * Hide overlay
     */
    fun hide() {
        // Stub implementation
    }

    /**
     * Update overlay with progress
     */
    fun updateProgress(progress: Float) {
        // Stub implementation
    }
}
