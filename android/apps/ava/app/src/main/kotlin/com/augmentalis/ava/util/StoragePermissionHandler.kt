// filename: android/ava/src/main/kotlin/com/augmentalis/ava/util/StoragePermissionHandler.kt
// author: Manoj Jhawar
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Handles storage permission requests for different API levels.
 *
 * - API 23-29: Uses READ_EXTERNAL_STORAGE runtime permission
 * - API 30+: Uses MANAGE_EXTERNAL_STORAGE (requires Settings intent)
 *
 * @author Manoj Jhawar
 */
class StoragePermissionHandler(
    private val activity: ComponentActivity,
    private val onPermissionResult: (Boolean) -> Unit
) {
    private var permissionLauncher: ActivityResultLauncher<String>? = null
    private var settingsLauncher: ActivityResultLauncher<Intent>? = null

    init {
        // Register permission launcher for API 23-29
        permissionLauncher = activity.registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            Timber.d("READ_EXTERNAL_STORAGE permission result: $isGranted")
            onPermissionResult(isGranted)
        }

        // Register settings launcher for API 30+
        settingsLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Check if permission was granted in settings
            val granted = hasStoragePermission()
            Timber.d("MANAGE_EXTERNAL_STORAGE settings result: $granted")
            onPermissionResult(granted)
        }
    }

    /**
     * Check if storage permission is granted.
     */
    fun hasStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ (Android 11+): Check MANAGE_EXTERNAL_STORAGE
            Environment.isExternalStorageManager()
        } else {
            // API 23-29: Check READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request storage permission.
     * For API 30+, this opens the Settings app.
     * For API 23-29, this shows a runtime permission dialog.
     */
    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API 30+ (Android 11+): Need to open Settings
            Timber.d("Requesting MANAGE_EXTERNAL_STORAGE via Settings intent")
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                    data = Uri.parse("package:${activity.packageName}")
                }
                settingsLauncher?.launch(intent)
            } catch (e: Exception) {
                Timber.w(e, "Failed to open app-specific settings, trying general settings")
                // Fallback to general settings
                val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                settingsLauncher?.launch(intent)
            }
        } else {
            // API 23-29: Request runtime permission
            Timber.d("Requesting READ_EXTERNAL_STORAGE runtime permission")
            permissionLauncher?.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    /**
     * Check if we should show rationale for the permission request.
     */
    fun shouldShowRationale(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // For MANAGE_EXTERNAL_STORAGE, always show explanation first
            !hasStoragePermission()
        } else {
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        }
    }

    companion object {
        /**
         * Static check if storage permission is granted (can be called without handler).
         */
        fun hasStoragePermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Environment.isExternalStorageManager()
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        /**
         * Get the model storage path for this device.
         * Uses external files dir which doesn't require special permissions.
         */
        fun getAppModelPath(context: Context): String {
            return context.getExternalFilesDir("models")?.absolutePath
                ?: "${context.filesDir}/models"
        }

        /**
         * Get the shared storage model path (requires MANAGE_EXTERNAL_STORAGE on API 30+).
         */
        fun getSharedModelPath(): String {
            return "/sdcard/ava-ai-models"
        }
    }
}
