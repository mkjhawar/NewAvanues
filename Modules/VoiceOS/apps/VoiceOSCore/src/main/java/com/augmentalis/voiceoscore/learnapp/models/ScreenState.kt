/**
 * ScreenState.kt - Screen state data model
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Data model representing unique screen state
 */

package com.augmentalis.voiceoscore.learnapp.models

/**
 * Screen State
 *
 * Represents unique state of a screen during exploration.
 * Uses SHA-256 hash as fingerprint for state comparison.
 */
data class ScreenState(
    val hash: String,
    val packageName: String,
    val activityName: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val elementCount: Int = 0,
    val isVisited: Boolean = false,
    val depth: Int = 0
) {

    /**
     * Mark screen as visited
     */
    fun markAsVisited(): ScreenState {
        return copy(isVisited = true)
    }

    /**
     * Check if screen matches hash (same screen)
     */
    fun matches(otherHash: String): Boolean {
        return hash == otherHash
    }

    /**
     * Calculate age in milliseconds
     */
    fun ageMs(): Long {
        return System.currentTimeMillis() - timestamp
    }

    override fun toString(): String {
        return "ScreenState(hash=${hash.take(16)}..., pkg=$packageName, elements=$elementCount, visited=$isVisited)"
    }
}
