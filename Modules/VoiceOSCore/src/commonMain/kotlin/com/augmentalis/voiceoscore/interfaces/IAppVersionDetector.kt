/**
 * IAppVersionDetector.kt - App version detection interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Platform-agnostic interface for detecting app version changes.
 * Version changes trigger command invalidation to ensure commands match current UI.
 */
package com.augmentalis.voiceoscore

/**
 * Represents an application version.
 */
data class AppVersion(
    val versionName: String,
    val versionCode: Long
) {
    companion object {
        val UNKNOWN = AppVersion("unknown", 0L)
    }
}

/**
 * Result of version change detection.
 */
sealed class VersionChange {
    /** App not installed or not accessible */
    data class NotFound(val packageName: String) : VersionChange()

    /** First time seeing this app */
    data class NewApp(val packageName: String, val version: AppVersion) : VersionChange()

    /** App was upgraded */
    data class Upgraded(
        val packageName: String,
        val oldVersion: AppVersion,
        val newVersion: AppVersion
    ) : VersionChange()

    /** App was downgraded */
    data class Downgraded(
        val packageName: String,
        val oldVersion: AppVersion,
        val newVersion: AppVersion
    ) : VersionChange()

    /** No version change */
    data class NoChange(val packageName: String, val version: AppVersion) : VersionChange()

    /** Error during detection */
    data class Error(val packageName: String, val message: String) : VersionChange()

    /** Check if this change requires command invalidation */
    val requiresCommandInvalidation: Boolean
        get() = this is Upgraded || this is Downgraded || this is NewApp
}

/**
 * Callback for version change events.
 */
fun interface VersionChangeListener {
    fun onVersionChange(change: VersionChange)
}

/**
 * App version detector interface for tracking app version changes.
 *
 * Version changes are important because:
 * - UI elements may change between versions
 * - Commands generated for old version may not match new UI
 * - Invalidating commands ensures fresh generation
 *
 * Usage:
 * ```kotlin
 * val detector: IAppVersionDetector = platformVersionDetector()
 *
 * // Check single app
 * val change = detector.detectVersionChange("com.example.app")
 * if (change.requiresCommandInvalidation) {
 *     commandRegistry.clearForPackage("com.example.app")
 * }
 *
 * // Or observe changes
 * detector.addListener { change ->
 *     if (change.requiresCommandInvalidation) {
 *         handleVersionChange(change)
 *     }
 * }
 * ```
 */
interface IAppVersionDetector {
    /**
     * Get current version of an installed app.
     *
     * @param packageName Package identifier
     * @return AppVersion or null if not installed
     */
    suspend fun getVersion(packageName: String): AppVersion?

    /**
     * Detect if app version has changed since last check.
     *
     * @param packageName Package identifier
     * @return VersionChange indicating what happened
     */
    suspend fun detectVersionChange(packageName: String): VersionChange

    /**
     * Check if version changed for current foreground app.
     */
    suspend fun checkForegroundApp(): VersionChange?

    /**
     * Add listener for version change events.
     */
    fun addListener(listener: VersionChangeListener)

    /**
     * Remove version change listener.
     */
    fun removeListener(listener: VersionChangeListener)

    /**
     * Start monitoring for app updates.
     */
    fun startMonitoring()

    /**
     * Stop monitoring for app updates.
     */
    fun stopMonitoring()
}

/**
 * Simple in-memory version tracker for platforms without persistent storage.
 */
class InMemoryVersionTracker {
    private val versions = mutableMapOf<String, AppVersion>()

    fun get(packageName: String): AppVersion? = versions[packageName]

    fun set(packageName: String, version: AppVersion) {
        versions[packageName] = version
    }

    fun clear() = versions.clear()

    fun remove(packageName: String) = versions.remove(packageName)
}

