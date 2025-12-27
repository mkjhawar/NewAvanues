/**
 * DynamicPackageConfig.kt - Configuration for device-specific package detection
 *
 * YOLO Phase 2 - High Priority Issue #11: Replace Hardcoded Package Names
 *
 * Provides flexible package configuration to support multiple device manufacturers:
 * - RealWear devices
 * - Standard Android devices
 * - Custom OEM devices
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-09
 */
package com.augmentalis.voiceoscore.config

import android.content.Context
import android.util.Log

/**
 * Manages device-specific package configurations
 *
 * Supports:
 * - Static default packages (built-in)
 * - Dynamic package detection (runtime)
 * - Configuration override via SharedPreferences
 */
object DynamicPackageConfig {
    private const val TAG = "DynamicPackageConfig"
    private const val PREF_NAME = "voiceos_package_config"
    private const val PREF_CUSTOM_PACKAGES = "custom_window_change_packages"

    /**
     * Default packages for window content change events
     *
     * These packages typically have dynamic content that benefits from
     * accessibility event monitoring.
     */
    private val DEFAULT_PACKAGES = setOf(
        // RealWear-specific packages
        "com.realwear.deviceinfo",
        "com.realwear.sysinfo",
        // Standard Android packages
        "com.android.systemui",
        "com.android.settings"
    )

    /**
     * Get valid packages for window content change events
     *
     * Priority order:
     * 1. Custom packages from SharedPreferences (if set)
     * 2. Device-detected packages (if manufacturer known)
     * 3. Default packages
     *
     * @param context Android context for accessing preferences
     * @return Set of package names to monitor
     */
    fun getValidWindowChangePackages(context: Context): Set<String> {
        // Check for custom configuration first
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val customPackages = prefs.getStringSet(PREF_CUSTOM_PACKAGES, null)

        if (customPackages != null && customPackages.isNotEmpty()) {
            Log.i(TAG, "Using ${customPackages.size} custom-configured packages")
            return customPackages
        }

        // Detect device manufacturer and add manufacturer-specific packages
        val detectedPackages = detectManufacturerPackages()

        if (detectedPackages.isNotEmpty()) {
            val combined = DEFAULT_PACKAGES + detectedPackages
            Log.i(TAG, "Using ${combined.size} packages (${DEFAULT_PACKAGES.size} default + ${detectedPackages.size} detected)")
            return combined
        }

        // Fall back to defaults
        Log.i(TAG, "Using ${DEFAULT_PACKAGES.size} default packages")
        return DEFAULT_PACKAGES
    }

    /**
     * Detect manufacturer-specific packages based on device
     *
     * @return Set of detected packages for this device
     */
    private fun detectManufacturerPackages(): Set<String> {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val model = android.os.Build.MODEL.lowercase()

        Log.d(TAG, "Detecting packages for manufacturer: $manufacturer, model: $model")

        return when {
            // RealWear devices
            manufacturer.contains("realwear") || model.contains("realwear") -> {
                Log.i(TAG, "Detected RealWear device - including RealWear-specific packages")
                setOf(
                    "com.realwear.deviceinfo",
                    "com.realwear.sysinfo",
                    "com.realwear.wearhf"
                )
            }
            // Google Pixel devices
            manufacturer.contains("google") -> {
                Log.i(TAG, "Detected Google device")
                setOf(
                    "com.google.android.apps.nexuslauncher",
                    "com.android.systemui"
                )
            }
            // Samsung devices
            manufacturer.contains("samsung") -> {
                Log.i(TAG, "Detected Samsung device")
                setOf(
                    "com.samsung.android.app.settings",
                    "com.android.systemui"
                )
            }
            // No manufacturer-specific packages detected
            else -> {
                Log.d(TAG, "No manufacturer-specific packages for: $manufacturer")
                emptySet()
            }
        }
    }

    /**
     * Set custom package configuration
     *
     * Allows runtime configuration of packages to monitor.
     * Useful for testing or device-specific customization.
     *
     * @param context Android context
     * @param packages Set of package names to monitor
     */
    fun setCustomPackages(context: Context, packages: Set<String>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putStringSet(PREF_CUSTOM_PACKAGES, packages)
            .apply()

        Log.i(TAG, "Set ${packages.size} custom packages: ${packages.joinToString(", ")}")
    }

    /**
     * Reset to default package configuration
     *
     * @param context Android context
     */
    fun resetToDefaults(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .remove(PREF_CUSTOM_PACKAGES)
            .apply()

        Log.i(TAG, "Reset to default packages")
    }

    /**
     * Check if a package should monitor window content change events
     *
     * @param context Android context
     * @param packageName Package name to check
     * @return true if package should be monitored
     */
    fun shouldMonitorPackage(context: Context, packageName: String): Boolean {
        return getValidWindowChangePackages(context).contains(packageName)
    }
}
