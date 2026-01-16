/**
 * JitStateModel.kt - JIT service state data model
 *
 * Cross-platform data class representing the current state of JIT learning service.
 * Migrated from JITLearning library for KMP compatibility.
 *
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: JITLearning/JITState.kt
 *
 * @since 3.0.0 (KMP Migration)
 */

package com.augmentalis.voiceoscoreng.jit

/**
 * JIT State Model
 *
 * Represents the current state of the JIT learning service.
 * Cross-platform model for use across Android, iOS, and Desktop.
 *
 * ## Usage:
 * ```kotlin
 * val state = JitStateModel(
 *     isActive = true,
 *     currentPackage = "com.example.app",
 *     screensLearned = 5,
 *     elementsDiscovered = 42,
 *     lastCaptureTime = currentTimeMillis()
 * )
 * if (state.isActive) {
 *     showStatus(state.getStatusMessage())
 * }
 * ```
 *
 * @property isActive Whether JIT is currently capturing (not paused)
 * @property currentPackage Package name currently being learned (null if idle)
 * @property screensLearned Total number of unique screens learned
 * @property elementsDiscovered Total number of UI elements discovered
 * @property lastCaptureTime Timestamp of last screen capture (milliseconds)
 */
data class JitStateModel(
    val isActive: Boolean,
    val currentPackage: String?,
    val screensLearned: Int,
    val elementsDiscovered: Int,
    val lastCaptureTime: Long
) {

    /**
     * Check if JIT is idle (no package being learned)
     *
     * @return true if currentPackage is null
     */
    fun isIdle(): Boolean = currentPackage == null

    /**
     * Get human-readable status string
     *
     * @return Status message for UI display
     */
    fun getStatusMessage(): String {
        return when {
            !isActive -> "JIT learning paused"
            isIdle() -> "JIT idle ($screensLearned screens, $elementsDiscovered elements learned)"
            else -> "JIT learning $currentPackage ($screensLearned screens, $elementsDiscovered elements)"
        }
    }

    /**
     * Get time since last capture in seconds
     *
     * Note: Uses expect/actual for currentTimeMillis() in KMP
     *
     * @param currentTimeMillis Current time in milliseconds
     * @return Seconds elapsed since last capture (0 if never captured)
     */
    fun getSecondsSinceLastCapture(currentTimeMillis: Long): Long {
        if (lastCaptureTime == 0L) return 0
        return (currentTimeMillis - lastCaptureTime) / 1000
    }

    companion object {
        /**
         * Create an idle state
         */
        fun idle(): JitStateModel = JitStateModel(
            isActive = false,
            currentPackage = null,
            screensLearned = 0,
            elementsDiscovered = 0,
            lastCaptureTime = 0L
        )

        /**
         * Create an active state for a package
         */
        fun active(
            packageName: String,
            screensLearned: Int = 0,
            elementsDiscovered: Int = 0,
            lastCaptureTime: Long = 0L
        ): JitStateModel = JitStateModel(
            isActive = true,
            currentPackage = packageName,
            screensLearned = screensLearned,
            elementsDiscovered = elementsDiscovered,
            lastCaptureTime = lastCaptureTime
        )
    }
}
