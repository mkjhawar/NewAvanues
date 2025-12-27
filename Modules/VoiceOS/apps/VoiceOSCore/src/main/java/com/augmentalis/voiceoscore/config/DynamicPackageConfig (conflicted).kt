/**
 * DynamicPackageConfig.kt - Dynamic package monitoring configuration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Provides device-specific configuration for which packages should be monitored
 * for accessibility events. Replaces hardcoded package lists with runtime detection.
 */
package com.augmentalis.voiceoscore.config

import android.content.Context
import android.os.Build

/**
 * Dynamic Package Configuration
 *
 * Determines which packages should be monitored based on device manufacturer
 * and Android version.
 */
object DynamicPackageConfig {

    /**
     * List of system packages that should always be monitored.
     */
    private val ALWAYS_MONITOR = setOf(
        "com.android.systemui",
        "com.android.launcher3",
        "com.google.android.apps.nexuslauncher"
    )

    /**
     * List of packages that should never be monitored.
     */
    private val NEVER_MONITOR = setOf(
        "android",
        "com.android.phone"
    )

    /**
     * Check if a package should be monitored for accessibility events.
     *
     * @param context Application context
     * @param packageName Package name to check
     * @return true if package should be monitored
     */
    fun shouldMonitorPackage(context: Context, packageName: String): Boolean {
        // Never monitor blocked packages
        if (packageName in NEVER_MONITOR) {
            return false
        }

        // Always monitor system packages
        if (packageName in ALWAYS_MONITOR) {
            return true
        }

        // For other packages, check device-specific rules
        val manufacturer = Build.MANUFACTURER.lowercase()

        return when {
            manufacturer.contains("samsung") -> shouldMonitorSamsungPackage(packageName)
            manufacturer.contains("google") -> shouldMonitorGooglePackage(packageName)
            manufacturer.contains("xiaomi") -> shouldMonitorXiaomiPackage(packageName)
            else -> shouldMonitorDefaultPackage(packageName)
        }
    }

    private fun shouldMonitorSamsungPackage(packageName: String): Boolean {
        return when {
            packageName.startsWith("com.samsung.") -> true
            packageName.startsWith("com.sec.") -> true
            else -> shouldMonitorDefaultPackage(packageName)
        }
    }

    private fun shouldMonitorGooglePackage(packageName: String): Boolean {
        return when {
            packageName.startsWith("com.google.") -> true
            else -> shouldMonitorDefaultPackage(packageName)
        }
    }

    private fun shouldMonitorXiaomiPackage(packageName: String): Boolean {
        return when {
            packageName.startsWith("com.miui.") -> true
            packageName.startsWith("com.xiaomi.") -> true
            else -> shouldMonitorDefaultPackage(packageName)
        }
    }

    private fun shouldMonitorDefaultPackage(packageName: String): Boolean {
        // Monitor most third-party apps by default
        return !packageName.startsWith("com.android.") ||
               packageName == "com.android.chrome" ||
               packageName == "com.android.vending"
    }
}
