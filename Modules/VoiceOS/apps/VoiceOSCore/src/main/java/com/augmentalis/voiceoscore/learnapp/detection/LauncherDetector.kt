/**
 * LauncherDetector.kt - Detects home screen/launcher
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Detects when user is on home screen/launcher to pause exploration
 */

package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

/**
 * Launcher Detector
 *
 * Detects when the user navigates to the home screen/launcher.
 * Used to pause exploration when user exits the target app.
 */
object LauncherDetector {
    private const val TAG = "LauncherDetector"

    // Common launcher package name prefixes
    private val LAUNCHER_PACKAGES = listOf(
        "com.google.android.apps.nexuslauncher",
        "com.android.launcher",
        "com.sec.android.app.launcher",
        "com.miui.home",
        "com.huawei.android.launcher",
        "com.oppo.launcher",
        "com.vivo.launcher",
        "com.oneplus.launcher",
        "com.motorola.launcher",
        "com.nothing.launcher",
        "com.tcl.home",
        "com.asus.launcher",
        "com.lge.launcher",
        "com.sony.home",
        "com.sonyericsson.home"
    )

    // Cache detected launcher package
    private var cachedLauncherPackage: String? = null

    /**
     * Check if package is the system launcher
     *
     * @param context Application context
     * @param packageName Package to check
     * @return true if package is the launcher
     */
    fun isLauncher(context: Context, packageName: String?): Boolean {
        if (packageName.isNullOrBlank()) return false

        // Check against cached launcher
        val launcher = getLauncherPackage(context)
        if (packageName == launcher) return true

        // Check against known launchers
        return LAUNCHER_PACKAGES.any { packageName.startsWith(it) }
    }

    /**
     * Get the default launcher package name
     *
     * @param context Application context
     * @return Default launcher package name
     */
    fun getLauncherPackage(context: Context): String? {
        // Return cached value if available
        cachedLauncherPackage?.let { return it }

        try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
            }

            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            val launcherPackage = resolveInfo?.activityInfo?.packageName
            cachedLauncherPackage = launcherPackage

            Log.d(TAG, "Detected default launcher: $launcherPackage")
            return launcherPackage
        } catch (e: Exception) {
            Log.e(TAG, "Failed to detect launcher", e)
            return null
        }
    }

    /**
     * Clear cached launcher package (call if launcher might have changed)
     */
    fun clearCache() {
        cachedLauncherPackage = null
    }
}
