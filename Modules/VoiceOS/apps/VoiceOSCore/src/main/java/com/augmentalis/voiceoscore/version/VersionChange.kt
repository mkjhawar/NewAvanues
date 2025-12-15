/**
 * VersionChange.kt - Sealed class representing app version change states
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Represents different types of version changes detected for an app.
 * Used for version-aware command lifecycle management.
 */

package com.augmentalis.voiceoscore.version

/**
 * Represents the type of version change detected for an app.
 *
 * ## Usage:
 * ```kotlin
 * when (versionChange) {
 *     is VersionChange.FirstInstall -> {
 *         // App newly installed
 *         logger.info("New app: ${versionChange.current}")
 *     }
 *     is VersionChange.Updated -> {
 *         // App updated - mark old commands for verification
 *         markOldCommandsDeprecated(versionChange.previous.versionCode)
 *         logger.info("Updated: ${versionChange.previous} → ${versionChange.current}")
 *     }
 *     is VersionChange.NoChange -> {
 *         // Same version - no action needed
 *     }
 *     is VersionChange.Downgraded -> {
 *         // User downgraded app (rare but possible)
 *         logger.warn("Downgrade: ${versionChange.previous} → ${versionChange.current}")
 *     }
 *     is VersionChange.AppNotInstalled -> {
 *         // App no longer installed - cleanup commands
 *         deleteAllCommands(versionChange.packageName)
 *     }
 * }
 * ```
 */
sealed class VersionChange {

    /**
     * App is newly installed (no previous version in database).
     *
     * This is the first time VoiceOS has seen this app.
     * No commands exist yet - ready for initial scraping.
     *
     * @property packageName App package name (e.g., "com.google.android.gm")
     * @property current Currently installed version
     */
    data class FirstInstall(
        val packageName: String,
        val current: AppVersion
    ) : VersionChange() {
        override fun toString(): String = "FirstInstall($packageName, $current)"
    }

    /**
     * App was updated to a newer version.
     *
     * Old commands should be marked for re-verification.
     * After 30-day grace period, unverified commands will be cleaned up.
     *
     * @property packageName App package name
     * @property previous Previously stored version
     * @property current Currently installed version (newer)
     */
    data class Updated(
        val packageName: String,
        val previous: AppVersion,
        val current: AppVersion
    ) : VersionChange() {
        override fun toString(): String = "Updated($packageName, $previous → $current)"

        /**
         * Get the number of version increments.
         * Example: versionCode 100 → 105 = 5 increments
         */
        fun getVersionDelta(): Long = current.versionCode - previous.versionCode
    }

    /**
     * App was downgraded to an older version.
     *
     * This is rare but can happen if user manually installs older APK.
     * Treat similar to update - mark current commands for verification.
     *
     * @property packageName App package name
     * @property previous Previously stored version (newer)
     * @property current Currently installed version (older)
     */
    data class Downgraded(
        val packageName: String,
        val previous: AppVersion,
        val current: AppVersion
    ) : VersionChange() {
        override fun toString(): String = "Downgraded($packageName, $previous → $current)"

        /**
         * Get the number of version decrements.
         * Example: versionCode 105 → 100 = -5 decrements
         */
        fun getVersionDelta(): Long = current.versionCode - previous.versionCode
    }

    /**
     * App version unchanged - same as database.
     *
     * No action needed. Commands are already current.
     *
     * @property packageName App package name
     * @property version Current version (same as stored)
     */
    data class NoChange(
        val packageName: String,
        val version: AppVersion
    ) : VersionChange() {
        override fun toString(): String = "NoChange($packageName, $version)"
    }

    /**
     * App is not installed on the device.
     *
     * Either:
     * 1. App was uninstalled (had commands in DB)
     * 2. App never existed (checking proactively)
     *
     * All commands for this app can be safely deleted.
     *
     * @property packageName App package name
     */
    data class AppNotInstalled(
        val packageName: String
    ) : VersionChange() {
        override fun toString(): String = "AppNotInstalled($packageName)"
    }

    /**
     * Get the current version (if app is installed).
     * Returns null for AppNotInstalled.
     */
    fun getCurrentVersion(): AppVersion? = when (this) {
        is FirstInstall -> current
        is Updated -> current
        is Downgraded -> current
        is NoChange -> version
        is AppNotInstalled -> null
    }

    /**
     * Check if this change requires command re-verification.
     *
     * True for: Updated, Downgraded
     * False for: FirstInstall, NoChange, AppNotInstalled
     */
    fun requiresVerification(): Boolean = when (this) {
        is Updated, is Downgraded -> true
        is FirstInstall, is NoChange, is AppNotInstalled -> false
    }

    /**
     * Check if this change requires command cleanup.
     *
     * True for: AppNotInstalled
     * False for: all others
     */
    fun requiresCleanup(): Boolean = this is AppNotInstalled
}
