package com.augmentalis.cockpit.mvp.rendering

import android.app.ActivityManager
import android.content.Context
import android.os.Build

/**
 * Adaptive quality manager for depth-of-field blur
 *
 * Detects device performance tier and adjusts blur quality:
 * - High-end: Full depth-of-field blur (24dp max)
 * - Mid-range: Reduced blur (12dp max)
 * - Low-end: Opacity fade only (0dp blur)
 */
class AdaptiveQualityManager(context: Context) {

    enum class QualityTier {
        HIGH,    // Full blur with GPU acceleration
        MEDIUM,  // Reduced blur radius
        LOW      // Opacity only, no blur
    }

    val qualityTier: QualityTier

    init {
        qualityTier = detectQualityTier(context)
    }

    private fun detectQualityTier(context: Context): QualityTier {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE)
            as ActivityManager

        // Get memory class (MB)
        val memoryClass = activityManager.memoryClass

        // High-end: 512MB+ RAM, Android 10+, high perf
        val isHighEnd = memoryClass >= 512 &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                        activityManager.isLowRamDevice.not()

        // Mid-range: 256-512MB RAM, Android 8+
        val isMidRange = memoryClass >= 256 &&
                         Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

        return when {
            isHighEnd -> QualityTier.HIGH
            isMidRange -> QualityTier.MEDIUM
            else -> QualityTier.LOW
        }
    }

    /**
     * Get max blur radius for current device tier
     */
    fun getMaxBlurRadius(): Float = when (qualityTier) {
        QualityTier.HIGH -> 24f      // Full blur
        QualityTier.MEDIUM -> 12f    // Half blur
        QualityTier.LOW -> 0f        // No blur
    }

    /**
     * Apply adaptive blur to calculated blur radius
     */
    fun applyAdaptiveBlur(blurRadius: Float): Float {
        val maxBlur = getMaxBlurRadius()
        return (blurRadius.coerceIn(0f, 24f) * (maxBlur / 24f))
    }
}
