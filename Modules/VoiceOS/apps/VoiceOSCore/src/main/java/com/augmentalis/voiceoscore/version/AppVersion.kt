/**
 * AppVersion.kt - Data class representing app version information
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Represents version information for installed Android apps.
 * Used for version-aware command lifecycle management.
 */

package com.augmentalis.voiceoscore.version

/**
 * Represents version information for an installed app.
 *
 * ## Version Code vs Version Name:
 * - **versionCode**: Integer for programmatic comparison (e.g., 82024)
 * - **versionName**: Human-readable string (e.g., "8.2024.11.123")
 *
 * ## Usage:
 * ```kotlin
 * val version = AppVersion(
 *     versionName = "8.2024.11.123",
 *     versionCode = 82024
 * )
 * println(version)  // "8.2024.11.123 (82024)"
 * ```
 *
 * @property versionName Human-readable version string (e.g., "8.2024.11.123")
 * @property versionCode Integer version for comparison (e.g., 82024)
 */
data class AppVersion(
    val versionName: String,
    val versionCode: Long
) {
    /**
     * String representation for logging and debugging.
     * Format: "versionName (versionCode)"
     * Example: "8.2024.11.123 (82024)"
     */
    override fun toString(): String = "$versionName ($versionCode)"

    companion object {
        /**
         * Special version representing "app not installed".
         * Used when PackageManager cannot find the app.
         */
        val NOT_INSTALLED = AppVersion(
            versionName = "NOT_INSTALLED",
            versionCode = -1L
        )

        /**
         * Special version representing "unknown" state.
         * Used for error handling when version cannot be determined.
         */
        val UNKNOWN = AppVersion(
            versionName = "UNKNOWN",
            versionCode = 0L
        )
    }

    /**
     * Check if this version is newer than another version.
     *
     * @param other Version to compare against
     * @return true if this version code is greater than other's version code
     */
    fun isNewerThan(other: AppVersion): Boolean = this.versionCode > other.versionCode

    /**
     * Check if this version is older than another version.
     *
     * @param other Version to compare against
     * @return true if this version code is less than other's version code
     */
    fun isOlderThan(other: AppVersion): Boolean = this.versionCode < other.versionCode

    /**
     * Check if this is the same version as another.
     *
     * @param other Version to compare against
     * @return true if version codes match
     */
    fun isSameAs(other: AppVersion): Boolean = this.versionCode == other.versionCode

    /**
     * Check if this represents a valid installed app.
     *
     * @return true if not NOT_INSTALLED or UNKNOWN
     */
    fun isValid(): Boolean = this != NOT_INSTALLED && this != UNKNOWN
}
