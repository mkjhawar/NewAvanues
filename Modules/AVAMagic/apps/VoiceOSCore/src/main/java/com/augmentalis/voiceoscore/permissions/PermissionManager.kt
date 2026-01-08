/**
 * PermissionManager.kt - Storage permission management for VoiceOS
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-22
 *
 * Handles runtime storage permission requests and status checking for VoiceOS.
 * Manages permission state across API levels (23-34+) with proper fallback handling.
 *
 * Features:
 * - Runtime permission checks
 * - Permission request coordination
 * - Rationale handling
 * - Settings deep linking
 * - Persistent permission denial tracking
 */
package com.augmentalis.voiceoscore.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import androidx.core.content.ContextCompat

/**
 * Manages storage permissions for VoiceOS
 * Handles API level differences and permission request flow
 */
class PermissionManager(private val context: Context) {

    companion object {
        private const val TAG = "PermissionManager"
        private const val PREFS_NAME = "voiceos_permissions"
        private const val KEY_PERMISSION_DENIED_COUNT = "storage_denied_count"
        private const val KEY_DONT_ASK_AGAIN = "storage_dont_ask_again"

        // FIX: MANAGE_EXTERNAL_STORAGE required for API 30+ (Android 11+)
        // READ_EXTERNAL_STORAGE is deprecated on API 33+ and non-functional on API 36
        // MANAGE_EXTERNAL_STORAGE allows access to non-media files (like .voiceos folder)

        // Legacy permissions for API 23-29 (Android 6-10)
        private val REQUIRED_PERMISSIONS_LEGACY = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Modern permission for API 30+ (Android 11+)
        private val REQUIRED_PERMISSIONS_MODERN = arrayOf(
            Manifest.permission.MANAGE_EXTERNAL_STORAGE
        )

        /**
         * Get permission requirements for current API level
         * API 30+: MANAGE_EXTERNAL_STORAGE (for non-media files in shared storage)
         * API 23-29: READ/WRITE_EXTERNAL_STORAGE (legacy)
         */
        fun getRequiredPermissions(): Array<String> {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                REQUIRED_PERMISSIONS_MODERN
            } else {
                REQUIRED_PERMISSIONS_LEGACY
            }
        }
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Check if all required storage permissions are granted
     * API 30+: Checks Environment.isExternalStorageManager() for MANAGE_EXTERNAL_STORAGE
     * API 23-29: Checks runtime permissions via ContextCompat
     * @return true if all required permissions granted, false otherwise
     */
    fun hasStoragePermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Check if we have "All files access" permission
            // This is the correct way to check MANAGE_EXTERNAL_STORAGE
            Environment.isExternalStorageManager()
        } else {
            // API 23-29: Check legacy runtime permissions
            val permissions = getRequiredPermissions()
            permissions.all { permission ->
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        Log.d(TAG, "Storage permission check: $granted (API ${Build.VERSION.SDK_INT})")
        return granted
    }

    /**
     * Check if we should show permission rationale
     * Returns true if user previously denied permission but didn't select "Don't ask again"
     *
     * Note: This method requires an Activity context and should be called from Activity
     * For Fragment/Service contexts, use shouldRequestPermission() instead
     */
    fun shouldShowRationale(activity: android.app.Activity): Boolean {
        val permissions = getRequiredPermissions()

        return permissions.any { permission ->
            activity.shouldShowRequestPermissionRationale(permission)
        }
    }

    /**
     * Check if we should request permission
     * Takes into account "Don't ask again" flag and denial count
     *
     * @return true if permission should be requested, false if user selected "Don't ask again"
     */
    fun shouldRequestPermission(): Boolean {
        // If already granted, no need to request
        if (hasStoragePermission()) {
            return false
        }

        // Check if user selected "Don't ask again"
        val dontAskAgain = prefs.getBoolean(KEY_DONT_ASK_AGAIN, false)
        if (dontAskAgain) {
            Log.d(TAG, "Permission marked as 'Don't ask again' - skipping request")
            return false
        }

        return true
    }

    /**
     * Record that permission was denied
     * Tracks denial count and "Don't ask again" state
     *
     * @param dontAskAgain true if user selected "Don't ask again"
     */
    fun recordPermissionDenied(dontAskAgain: Boolean) {
        val deniedCount = prefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0) + 1

        prefs.edit().apply {
            putInt(KEY_PERMISSION_DENIED_COUNT, deniedCount)
            putBoolean(KEY_DONT_ASK_AGAIN, dontAskAgain)
            apply()
        }

        Log.d(TAG, "Permission denied (count: $deniedCount, don't ask again: $dontAskAgain)")
    }

    /**
     * Record that permission was granted
     * Resets denial tracking
     */
    fun recordPermissionGranted() {
        prefs.edit().apply {
            putInt(KEY_PERMISSION_DENIED_COUNT, 0)
            putBoolean(KEY_DONT_ASK_AGAIN, false)
            apply()
        }

        Log.i(TAG, "Storage permission granted")
    }

    /**
     * Get number of times permission was denied
     */
    fun getPermissionDeniedCount(): Int {
        return prefs.getInt(KEY_PERMISSION_DENIED_COUNT, 0)
    }

    /**
     * Check if user selected "Don't ask again"
     */
    fun isDontAskAgainSet(): Boolean {
        return prefs.getBoolean(KEY_DONT_ASK_AGAIN, false)
    }

    /**
     * Reset permission tracking
     * Useful when user manually grants permission via Settings
     */
    fun resetPermissionTracking() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Permission tracking reset")
    }

    /**
     * Create intent to request storage permission
     * API 30+: Opens "All files access" settings page (MANAGE_EXTERNAL_STORAGE)
     * API 23-29: Opens app settings page for legacy permissions
     */
    fun createStoragePermissionIntent(): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+: Direct to "All files access" settings
            Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            // API 23-29: Fallback to app settings
            createAppSettingsIntent()
        }
    }

    /**
     * Create intent to open app settings
     * Used when user needs to manually grant permissions
     */
    fun createAppSettingsIntent(): Intent {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.fromParts("package", context.packageName, null)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return intent
    }

    /**
     * Get permission status summary for display
     * @return Human-readable permission status
     */
    fun getPermissionStatusSummary(): String {
        return when {
            hasStoragePermission() -> "Granted"
            isDontAskAgainSet() -> "Denied (Don't ask again)"
            getPermissionDeniedCount() > 0 -> "Denied (${getPermissionDeniedCount()} times)"
            else -> "Not requested"
        }
    }

    /**
     * Get storage location based on permission status
     * @return "External" if permission granted, "Internal" otherwise
     */
    fun getStorageLocation(): String {
        return if (hasStoragePermission()) "External (Shared)" else "Internal (App-only)"
    }

    /**
     * Get user-friendly explanation for why storage permission is needed
     */
    fun getPermissionRationale(): String {
        return """
            VoiceOS needs storage access to:

            • Load speech recognition models from shared folders
            • Allow manual model deployment via ADB
            • Share models across app reinstalls
            • Reduce APK size by using external models

            Without this permission, VoiceOS will use internal storage (models must be downloaded or extracted from APK).
        """.trimIndent()
    }

    /**
     * Get permissions that are missing
     * @return Array of permission strings that are not granted
     */
    fun getMissingPermissions(): Array<String> {
        val permissions = getRequiredPermissions()
        return permissions.filter { permission ->
            ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
    }

    /**
     * Check if specific permission is granted
     * @param permission Permission to check
     * @return true if granted, false otherwise
     */
    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }
}
