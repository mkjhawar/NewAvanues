/**
 * PermissionRequestActivity.kt - Handles storage permission requests
 *
 * Copyright (C) Augmentalis Inc, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-22
 *
 * Transparent activity that handles storage permission requests using ActivityResultContracts API.
 * Shows rationale dialog if needed, requests permissions, and reports results via broadcast.
 *
 * Usage:
 *   val intent = Intent(context, PermissionRequestActivity::class.java)
 *   startActivity(intent)
 *
 * Results broadcast with action: com.augmentalis.voiceoscore.PERMISSION_RESULT
 *   Extra: "granted" (Boolean) - true if permission granted, false otherwise
 */
package com.augmentalis.voiceoscore.permissions

import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts

class PermissionRequestActivity : ComponentActivity() {

    companion object {
        private const val TAG = "PermissionRequestActivity"
        const val ACTION_PERMISSION_RESULT = "com.augmentalis.voiceoscore.PERMISSION_RESULT"
        const val EXTRA_GRANTED = "granted"
    }

    private lateinit var permissionManager: PermissionManager

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }

        Log.i(TAG, "Permission request result: granted=$allGranted")

        if (allGranted) {
            permissionManager.recordPermissionGranted()
            broadcastResult(granted = true)
        } else {
            // Check if user selected "Don't ask again"
            val dontAskAgain = !shouldShowRationale()
            permissionManager.recordPermissionDenied(dontAskAgain)
            broadcastResult(granted = false)
        }

        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        permissionManager = PermissionManager(this)

        // Check if permission already granted
        if (permissionManager.hasStoragePermission()) {
            Log.d(TAG, "Permission already granted")
            broadcastResult(granted = true)
            finish()
            return
        }

        // API 30+: MANAGE_EXTERNAL_STORAGE requires manual settings navigation
        // Cannot be requested via runtime dialog
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "API 30+: Showing settings dialog for MANAGE_EXTERNAL_STORAGE")
            showManageStorageDialog()
            return
        }

        // API 23-29: Use legacy runtime permission flow
        // FIX: Check if "don't ask again" was previously set
        if (permissionManager.isDontAskAgainSet()) {
            Log.d(TAG, "Don't ask again set - showing settings dialog")
            showSettingsDialog()
            return
        }

        // Check if we should show rationale
        if (shouldShowRationale()) {
            showRationaleDialog()
        } else {
            requestPermissions()
        }
    }

    private fun shouldShowRationale(): Boolean {
        return permissionManager.shouldShowRationale(this)
    }

    private fun showRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Needed")
            .setMessage(permissionManager.getPermissionRationale())
            .setPositiveButton("Allow") { dialog, _ ->
                dialog.dismiss()
                requestPermissions()
            }
            .setNegativeButton("Deny") { dialog, _ ->
                dialog.dismiss()
                permissionManager.recordPermissionDenied(dontAskAgain = false)
                broadcastResult(granted = false)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showManageStorageDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Access Required")
            .setMessage(
                permissionManager.getPermissionRationale() + "\n\n" +
                "VoiceOS needs \"All files access\" permission to read speech recognition models from shared storage.\n\n" +
                "You'll be taken to system settings to enable this permission."
            )
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                // Open "All files access" settings page
                startActivity(permissionManager.createStoragePermissionIntent())
                finish()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                broadcastResult(granted = false)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSettingsDialog() {
        AlertDialog.Builder(this)
            .setTitle("Storage Permission Required")
            .setMessage(
                "You previously denied storage permission with \"Don't ask again\".\n\n" +
                permissionManager.getPermissionRationale() + "\n\n" +
                "Please grant storage permission in Settings."
            )
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                // Open app settings
                startActivity(permissionManager.createAppSettingsIntent())
                // Don't finish - user might return and we should check again
                finish()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                broadcastResult(granted = false)
                finish()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestPermissions() {
        val permissions = PermissionManager.getRequiredPermissions()
        Log.d(TAG, "Requesting permissions: ${permissions.joinToString()}")
        permissionLauncher.launch(permissions)
    }

    private fun broadcastResult(granted: Boolean) {
        val intent = Intent(ACTION_PERMISSION_RESULT)
        intent.putExtra(EXTRA_GRANTED, granted)
        intent.setPackage(packageName)
        sendBroadcast(intent)

        Log.d(TAG, "Broadcast permission result: granted=$granted")
    }
}
