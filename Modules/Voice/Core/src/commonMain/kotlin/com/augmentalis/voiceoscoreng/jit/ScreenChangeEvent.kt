/**
 * ScreenChangeEvent.kt - Screen change event data class
 *
 * Cross-platform data class representing a screen change event.
 * Migrated from JITLearning library for KMP compatibility.
 *
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: JITLearning/ScreenChangeEvent.kt
 *
 * IPC Format: SCH:hash:activity:timestamp:element_count
 *
 * @since 3.0.0 (KMP Migration)
 */

package com.augmentalis.voiceoscoreng.jit

/**
 * Screen Change Event
 *
 * Represents a screen change detected by the accessibility service.
 * Cross-platform model for screen navigation tracking.
 *
 * ## Usage - Creating event:
 * ```kotlin
 * val event = ScreenChangeEvent.create(
 *     screenHash = "abc123",
 *     activityName = "MainActivity",
 *     packageName = "com.example.app",
 *     elementCount = 15
 * )
 * ```
 *
 * ## Usage - IPC serialization:
 * ```kotlin
 * val ipcString = event.toIpcString()
 * val parsed = ScreenChangeEvent.fromIpcString(ipcString)
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
data class ScreenChangeEvent(
    val screenHash: String,
    val activityName: String,
    val packageName: String,
    val timestamp: Long,
    val elementCount: Int,
    val isNewScreen: Boolean = true,
    val previousScreenHash: String = ""
) {

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

    /**
     * Get human-readable description of the screen change.
     */
    fun getDescription(): String {
        val shortName = getShortActivityName()
        return if (isNewScreen) {
            "New screen: $shortName ($elementCount elements)"
        } else {
            "Screen: $shortName ($elementCount elements)"
        }
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
                timestamp = parts[3].toLongOrNull() ?: 0L,
                elementCount = parts[4].toIntOrNull() ?: 0
            )
        }

        /**
         * Create event from screen capture data.
         *
         * @param screenHash Unique screen identifier hash
         * @param activityName Activity class name
         * @param packageName Package name
         * @param elementCount Number of elements on screen
         * @param isNewScreen Whether this is a newly discovered screen
         * @param previousScreenHash Hash of previous screen
         * @param timestamp Event timestamp (defaults to current time placeholder)
         */
        fun create(
            screenHash: String,
            activityName: String,
            packageName: String,
            elementCount: Int,
            isNewScreen: Boolean = true,
            previousScreenHash: String = "",
            timestamp: Long = 0L
        ): ScreenChangeEvent {
            return ScreenChangeEvent(
                screenHash = screenHash,
                activityName = activityName,
                packageName = packageName,
                timestamp = timestamp,
                elementCount = elementCount,
                isNewScreen = isNewScreen,
                previousScreenHash = previousScreenHash
            )
        }
    }
}
