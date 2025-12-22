/**
 * ScreenChangeEvent.kt - Screen change event data class
 *
 * Parcelable data class representing a screen change event.
 * Passed across process boundaries via AIDL interface.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * IPC Format: SCH:hash:activity:timestamp:element_count
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.jitlearning

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Screen Change Event
 *
 * Represents a screen change detected by the accessibility service.
 * Sent to LearnApp via IAccessibilityEventListener.onScreenChanged().
 *
 * ## Usage - Receiving event:
 * ```kotlin
 * override fun onScreenChanged(event: ScreenChangeEvent) {
 *     Log.d(TAG, "Screen changed: ${event.activityName}")
 *     // Request full screen info if needed
 *     val screenInfo = jitService.getCurrentScreenInfo()
 * }
 * ```
 *
 * @property screenHash Unique hash identifying this screen (MD5 of structure)
 * @property activityName Activity class name (e.g., "MainActivity")
 * @property packageName Package name (e.g., "com.microsoft.teams")
 * @property timestamp When the screen change was detected
 * @property elementCount Number of accessible elements on screen
 * @property isNewScreen Whether this is a newly discovered screen
 * @property previousScreenHash Hash of the previous screen (for navigation tracking)
 */
@Parcelize
data class ScreenChangeEvent(
    val screenHash: String,
    val activityName: String,
    val packageName: String,
    val timestamp: Long,
    val elementCount: Int,
    val isNewScreen: Boolean = true,
    val previousScreenHash: String = ""
) : Parcelable {

    /**
     * Convert to IPC format string.
     *
     * Format: SCH:hash:activity:timestamp:element_count
     *
     * @return IPC-formatted string
     */
    fun toIpcString(): String {
        return "SCH:$screenHash:$activityName:$timestamp:$elementCount"
    }

    /**
     * Get short activity name (without package prefix).
     */
    fun getShortActivityName(): String {
        return activityName.substringAfterLast(".")
    }

    companion object {
        /**
         * Parse from IPC format string.
         *
         * @param ipc IPC string (e.g., "SCH:abc123:MainActivity:1733931600:15")
         * @return ScreenChangeEvent or null if invalid
         */
        fun fromIpcString(ipc: String): ScreenChangeEvent? {
            if (!ipc.startsWith("SCH:")) return null
            val parts = ipc.split(":")
            if (parts.size < 5) return null

            return ScreenChangeEvent(
                screenHash = parts[1],
                activityName = parts[2],
                packageName = "", // Not in IPC format
                timestamp = parts[3].toLongOrNull() ?: System.currentTimeMillis(),
                elementCount = parts[4].toIntOrNull() ?: 0
            )
        }

        /**
         * Create event from screen capture data.
         */
        fun create(
            screenHash: String,
            activityName: String,
            packageName: String,
            elementCount: Int,
            isNewScreen: Boolean = true,
            previousScreenHash: String = ""
        ): ScreenChangeEvent {
            return ScreenChangeEvent(
                screenHash = screenHash,
                activityName = activityName,
                packageName = packageName,
                timestamp = System.currentTimeMillis(),
                elementCount = elementCount,
                isNewScreen = isNewScreen,
                previousScreenHash = previousScreenHash
            )
        }
    }
}
