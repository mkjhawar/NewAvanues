/**
 * JITState.kt - JIT service state data class
 *
 * Parcelable data class representing the current state of JIT learning service.
 * Passed across process boundaries via AIDL interface.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: JIT-LearnApp Separation (Phase 2)
 *
 * @since 2.0.0 (JIT-LearnApp Separation)
 */

package com.augmentalis.jitlearning

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * JIT State
 *
 * Represents the current state of the JIT learning service.
 * Used for IPC communication between JIT service and LearnApp.
 *
 * ## Usage - LearnApp Query:
 * ```kotlin
 * val state = jitBinder.queryState()
 * if (state.isActive) {
 *     showStatus("JIT learning ${state.currentPackage}: ${state.elementsDiscovered} elements")
 * }
 * ```
 *
 * ## Usage - JIT Service:
 * ```kotlin
 * override fun queryState(): JITState {
 *     return JITState(
 *         isActive = !isPaused,
 *         currentPackage = lastPackageName,
 *         screensLearned = screenCount,
 *         elementsDiscovered = elementCount,
 *         lastCaptureTime = lastCaptureTimestamp
 *     )
 * }
 * ```
 *
 * @property isActive Whether JIT is currently capturing (not paused)
 * @property currentPackage Package name currently being learned (null if idle)
 * @property screensLearned Total number of unique screens learned
 * @property elementsDiscovered Total number of UI elements discovered
 * @property lastCaptureTime Timestamp of last screen capture (milliseconds)
 */
@Parcelize
data class JITState(
    val isActive: Boolean,
    val currentPackage: String?,
    val screensLearned: Int,
    val elementsDiscovered: Int,
    val lastCaptureTime: Long
) : Parcelable {

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
     * @return Seconds elapsed since last capture (0 if never captured)
     */
    fun getSecondsSinceLastCapture(): Long {
        if (lastCaptureTime == 0L) return 0
        return (System.currentTimeMillis() - lastCaptureTime) / 1000
    }
}
