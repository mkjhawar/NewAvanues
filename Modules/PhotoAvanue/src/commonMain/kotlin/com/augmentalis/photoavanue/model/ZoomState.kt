package com.augmentalis.photoavanue.model

import kotlinx.serialization.Serializable

@Serializable
data class ZoomState(
    val currentRatio: Float = 1.0f,
    val minRatio: Float = 1.0f,
    val maxRatio: Float = 1.0f
) {
    /** Discrete step size for 5-level zoom control. */
    val stepSize: Float get() = if (maxRatio > minRatio) (maxRatio - minRatio) / 5f else 0f

    /** Current zoom as a discrete level (1..5). */
    val currentLevel: Int get() {
        if (stepSize <= 0f) return 1
        return ((currentRatio - minRatio) / stepSize).toInt().coerceIn(0, 4) + 1
    }
}
