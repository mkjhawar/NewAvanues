/**
 * PermissionHelper.kt - Runtime permission checking utility
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-31
 *
 * Provides runtime checks for sensitive permissions required by VoiceOS.
 *
 * Phase 3B: Permission Hardening
 * - QUERY_ALL_PACKAGES permission check
 * - Foreground service permission checks
 * - Graceful degradation when permissions denied
 */
package com.augmentalis.voiceoscore.accessibility.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Permission Helper
 *
 * Utility class for checking runtime permissions required by VoiceOS.
 *
 * ## Permissions Checked:
 * 1. QUERY_ALL_PACKAGES (Android 11+) - Package visibility for launcher detection
 * 2. FOREGROUND_SERVICE - Background operation
 * 3. FOREGROUND_SERVICE_MICROPHONE (Android 14+) - Background voice recognition
 *
 * ## Usage:
 * ```kotlin
 * val hasPackagePermission = PermissionHelper.hasQueryAllPackagesPermission(context)
 * if (!hasPackagePermission) {
 *     // Use fallback launcher detection
 * }
 * ```
 */
object PermissionHelper {

    /**
     * Check if QUERY_ALL_PACKAGES permission is granted
     *
     * This permission is required for comprehensive launcher detection on Android 11+.
     * Without it, VoiceOS falls back to a known launchers list.
     *
     * @param context Application context
     * @return true if permission granted (or not required on Android <11)
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun hasQueryAllPackagesPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) ||
                context.checkSelfPermission("android.permission.QUERY_ALL_PACKAGES") == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                android.util.Log.w("PermissionHelper", "Error checking QUERY_ALL_PACKAGES", e)
                false
            }
        } else {
            // Android <11: Permission not required
            true
        }
    }

    /**
     * Check if FOREGROUND_SERVICE permission is granted
     *
     * Required for running VoiceOS in the background.
     *
     * @param context Application context
     * @return true if permission granted
     */
    fun hasForegroundServicePermission(context: Context): Boolean {
        return try {
            context.checkSelfPermission("android.permission.FOREGROUND_SERVICE") == PackageManager.PERMISSION_GRANTED
        } catch (e: Exception) {
            android.util.Log.w("PermissionHelper", "Error checking FOREGROUND_SERVICE", e)
            false
        }
    }

    /**
     * Check if FOREGROUND_SERVICE_MICROPHONE permission is granted
     *
     * Required on Android 14+ for foreground services that use the microphone.
     *
     * @param context Application context
     * @return true if permission granted (or not required on Android <14)
     */
    fun hasForegroundServiceMicrophonePermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            try {
                context.checkSelfPermission("android.permission.FOREGROUND_SERVICE_MICROPHONE") == PackageManager.PERMISSION_GRANTED
            } catch (e: Exception) {
                android.util.Log.w("PermissionHelper", "Error checking FOREGROUND_SERVICE_MICROPHONE", e)
                false
            }
        } else {
            // Android <14: Permission not required
            true
        }
    }

    /**
     * Check if all required permissions are granted
     *
     * Checks all permissions required for full VoiceOS functionality.
     *
     * @param context Application context
     * @return true if all permissions granted
     */
    fun hasAllRequiredPermissions(context: Context): Boolean {
        val hasQueryAllPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasQueryAllPackagesPermission(context)
        } else {
            true
        }

        val hasForegroundService = hasForegroundServicePermission(context)

        val hasForegroundMicrophone = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            hasForegroundServiceMicrophonePermission(context)
        } else {
            true
        }

        return hasQueryAllPackages && hasForegroundService && hasForegroundMicrophone
    }

    /**
     * Get list of missing permissions
     *
     * Returns human-readable list of permissions that are not granted.
     *
     * @param context Application context
     * @return List of missing permission names (empty if all granted)
     */
    fun getMissingPermissions(context: Context): List<String> {
        val missing = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasQueryAllPackagesPermission(context)) {
            missing.add("QUERY_ALL_PACKAGES (Package Visibility)")
        }

        if (!hasForegroundServicePermission(context)) {
            missing.add("FOREGROUND_SERVICE")
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !hasForegroundServiceMicrophonePermission(context)) {
            missing.add("FOREGROUND_SERVICE_MICROPHONE")
        }

        return missing
    }

    /**
     * Get permission status summary
     *
     * Returns detailed status of all required permissions for debugging.
     *
     * @param context Application context
     * @return Map of permission name to granted status
     */
    fun getPermissionStatus(context: Context): Map<String, Boolean> {
        return mapOf(
            "QUERY_ALL_PACKAGES" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                hasQueryAllPackagesPermission(context)
            } else {
                true // Not required on Android <11
            },
            "FOREGROUND_SERVICE" to hasForegroundServicePermission(context),
            "FOREGROUND_SERVICE_MICROPHONE" to if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                hasForegroundServiceMicrophonePermission(context)
            } else {
                true // Not required on Android <14
            }
        )
    }

    /**
     * Log permission status for debugging
     *
     * Logs detailed permission status to help diagnose permission issues.
     *
     * @param context Application context
     * @param tag Log tag (default: "PermissionHelper")
     */
    fun logPermissionStatus(context: Context, tag: String = "PermissionHelper") {
        val status = getPermissionStatus(context)
        android.util.Log.d(tag, "=== VoiceOS Permission Status ===")
        status.forEach { (permission, granted) ->
            android.util.Log.d(tag, "$permission: ${if (granted) "✓ GRANTED" else "✗ DENIED"}")
        }

        val missing = getMissingPermissions(context)
        if (missing.isNotEmpty()) {
            android.util.Log.w(tag, "Missing permissions: ${missing.joinToString(", ")}")
        } else {
            android.util.Log.i(tag, "All permissions granted ✓")
        }
    }

    /**
     * Get user-friendly explanation for missing permissions
     *
     * Returns explanations for why each permission is needed, suitable for showing to users.
     *
     * @param context Application context
     * @return Map of missing permission name to user-friendly explanation
     */
    fun getMissingPermissionExplanations(context: Context): Map<String, String> {
        val explanations = mutableMapOf<String, String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !hasQueryAllPackagesPermission(context)) {
            explanations["QUERY_ALL_PACKAGES"] =
                "VoiceOS needs to detect launcher apps to prevent system instability. " +
                "Without this permission, launcher detection will use a limited known launchers list."
        }

        if (!hasForegroundServicePermission(context)) {
            explanations["FOREGROUND_SERVICE"] =
                "VoiceOS needs to run in the background to respond to voice commands. " +
                "Without this permission, VoiceOS may be stopped by the system."
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE && !hasForegroundServiceMicrophonePermission(context)) {
            explanations["FOREGROUND_SERVICE_MICROPHONE"] =
                "VoiceOS needs microphone access while running in the background for voice recognition. " +
                "Without this permission, voice commands may not work reliably."
        }

        return explanations
    }
}
