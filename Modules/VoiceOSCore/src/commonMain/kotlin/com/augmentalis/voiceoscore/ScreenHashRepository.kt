package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Repository interface for screen hash caching and comparison.
 *
 * Manages the screen fingerprint cache to avoid re-scanning known screens.
 * Screens are identified by SHA-256 hash of their content/structure.
 */
interface ScreenHashRepository {

    /**
     * Check if a screen with the given hash exists in the cache.
     *
     * @param hash SHA-256 hash of the screen fingerprint
     * @return true if screen is known, false otherwise
     */
    suspend fun hasScreen(hash: String): Boolean

    /**
     * Save a new screen hash to the cache.
     *
     * @param hash SHA-256 hash of the screen fingerprint
     * @param packageName The app package name (e.g., com.instagram.android)
     * @param activityName The activity name if available
     * @param appVersion The app version string for version-based invalidation
     * @param elementCount Number of elements on the screen
     */
    suspend fun saveScreen(
        hash: String,
        packageName: String,
        activityName: String?,
        appVersion: String,
        elementCount: Int
    )

    /**
     * Get the stored app version for a screen hash.
     * Used to detect app updates requiring rescan.
     *
     * @param hash SHA-256 hash of the screen fingerprint
     * @return The stored app version, or null if not found
     */
    suspend fun getAppVersion(hash: String): String?

    /**
     * Get cached commands for a known screen.
     *
     * @param hash SHA-256 hash of the screen fingerprint
     * @return List of cached commands for this screen
     */
    suspend fun getCommandsForScreen(hash: String): List<QuantizedCommand>

    /**
     * Save commands for a screen hash.
     *
     * @param hash SHA-256 hash of the screen fingerprint
     * @param commands List of generated commands to cache
     */
    suspend fun saveCommandsForScreen(hash: String, commands: List<QuantizedCommand>)

    /**
     * Clear a specific screen from the cache.
     *
     * @param hash SHA-256 hash of the screen to clear
     */
    suspend fun clearScreen(hash: String)

    /**
     * Clear all cached screens for a specific package.
     * Used by "Rescan Current App" developer action.
     *
     * @param packageName The app package name to clear
     * @return Number of screens cleared
     */
    suspend fun clearScreensForPackage(packageName: String): Int

    /**
     * Clear ALL cached screens across all apps.
     * Used by "Rescan Everything" developer action.
     *
     * @return Number of screens cleared
     */
    suspend fun clearAllScreens(): Int

    /**
     * Get total count of cached screens.
     *
     * @return Total number of cached screen hashes
     */
    suspend fun getScreenCount(): Int

    /**
     * Get count of cached screens for a specific package.
     *
     * @param packageName The app package name
     * @return Number of cached screens for this package
     */
    suspend fun getScreenCountForPackage(packageName: String): Int

    /**
     * Get screen info for display in debug panel.
     *
     * @param hash SHA-256 hash of the screen fingerprint
     * @return ScreenInfo if found, null otherwise
     */
    suspend fun getScreenInfo(hash: String): ScreenInfo?
}

/**
 * Data class representing cached screen metadata.
 * Used for display in the hierarchy/debug panel.
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
