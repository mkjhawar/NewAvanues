package com.augmentalis.voiceoscoreng.persistence

/**
 * Data class representing cached screen information.
 *
 * @property hash Unique hash identifying this screen layout
 * @property packageName App package name
 * @property activityName Optional activity name
 * @property appVersion App version string
 * @property elementCount Total elements on screen
 * @property actionableCount Clickable + scrollable elements
 * @property commandCount Generated commands count
 * @property scannedAt Timestamp when screen was scanned
 * @property isCached Whether this was loaded from cache
 */
data class ScreenInfo(
    val hash: String,
    val packageName: String,
    val activityName: String?,
    val appVersion: String,
    val elementCount: Int,
    val actionableCount: Int,
    val commandCount: Int,
    val scannedAt: Long,
    val isCached: Boolean
)
