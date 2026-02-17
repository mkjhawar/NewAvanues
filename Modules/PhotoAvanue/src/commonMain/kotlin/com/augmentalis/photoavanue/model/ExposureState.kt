package com.augmentalis.photoavanue.model

import kotlinx.serialization.Serializable

@Serializable
data class ExposureState(
    val currentIndex: Int = 0,
    val minIndex: Int = 0,
    val maxIndex: Int = 0
) {
    /** Discrete step size for 5-level exposure control. */
    val stepSize: Int get() = if (maxIndex > minIndex) (maxIndex - minIndex) / 5 else 0

    /** Current exposure as a discrete level (1..5). */
    val currentLevel: Int get() {
        if (stepSize <= 0) return 3
        return ((currentIndex - minIndex) / stepSize).coerceIn(0, 4) + 1
    }
}
