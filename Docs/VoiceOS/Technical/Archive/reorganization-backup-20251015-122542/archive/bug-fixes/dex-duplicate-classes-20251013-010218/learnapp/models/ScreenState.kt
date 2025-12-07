/**
 * ScreenState.kt - Screen state data model
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/models/ScreenState.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Data model representing unique screen state
 */

package com.augmentalis.learnapp.models

/**
 * Screen State
 *
 * Represents unique state of a screen during exploration.
 * Uses SHA-256 hash as fingerprint for state comparison.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val screenState = ScreenState(
 *     hash = "abc123def456...",  // SHA-256 hash
 *     packageName = "com.instagram.android",
 *     activityName = ".MainActivity",
 *     timestamp = System.currentTimeMillis(),
 *     elementCount = 45,
 *     isVisited = false
 * )
 *
 * // Mark as visited
 * val visitedState = screenState.markAsVisited()
 * ```
 *
 * @property hash SHA-256 fingerprint of screen
 * @property packageName Package name of app
 * @property activityName Activity name (if available)
 * @property timestamp When screen was first discovered
 * @property elementCount Number of UI elements on screen
 * @property isVisited Whether screen has been explored
 * @property depth DFS depth when discovered
 *
 * @since 1.0.0
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
     *
     * @return Copy with isVisited = true
     */
    fun markAsVisited(): ScreenState {
        return copy(isVisited = true)
    }

    /**
     * Check if screen matches hash (same screen)
     *
     * @param otherHash Hash to compare
     * @return true if hashes match
     */
    fun matches(otherHash: String): Boolean {
        return hash == otherHash
    }

    /**
     * Calculate age in milliseconds
     *
     * @return Age since discovery
     */
    fun ageMs(): Long {
        return System.currentTimeMillis() - timestamp
    }

    override fun toString(): String {
        return """
            ScreenState:
            - Hash: ${hash.take(16)}...
            - Package: $packageName
            - Activity: ${activityName ?: "unknown"}
            - Elements: $elementCount
            - Visited: $isVisited
            - Depth: $depth
        """.trimIndent()
    }
}
