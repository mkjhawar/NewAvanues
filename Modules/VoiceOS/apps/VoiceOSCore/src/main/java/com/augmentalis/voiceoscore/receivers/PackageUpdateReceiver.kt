/**
 * PackageUpdateReceiver.kt - Broadcast receiver for app install/update/remove detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Listens for Android system broadcasts when apps are installed, updated, or uninstalled.
 * Triggers version-aware command lifecycle management by delegating to AppVersionManager.
 *
 * ## Registered Actions:
 * - ACTION_PACKAGE_ADDED: App installed or updated
 * - ACTION_PACKAGE_REPLACED: App explicitly updated
 * - ACTION_PACKAGE_REMOVED: App uninstalled
 *
 * ## Architecture:
 * This receiver is lightweight (< 10 seconds execution) and delegates all heavy work
 * to AppVersionManager via coroutines. Database operations run on Dispatchers.Default.
 *
 * ## Integration:
 * Declared in AndroidManifest.xml with intent-filter for package actions.
 * Automatically invoked by Android when any app on the system changes.
 */

package com.augmentalis.voiceoscore.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.voiceoscore.version.AppVersionDetector
import com.augmentalis.voiceoscore.version.AppVersionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Broadcast receiver for detecting app package changes.
 *
 * ## Workflow:
 * 1. Receive package broadcast from Android system
 * 2. Extract package name from intent data
 * 3. Determine event type (install/update/remove)
 * 4. Launch coroutine to handle version check via AppVersionManager
 * 5. Log result for monitoring
 *
 * ## BroadcastReceiver Rules:
 * - Must complete within 10 seconds (Android enforces this)
 * - Heavy work must run in background (coroutine/service)
 * - Cannot block main thread
 * - Should handle errors gracefully
 *
 * ## Example Broadcasts:
 * ```
 * # App installed
 * Intent { act=android.intent.action.PACKAGE_ADDED dat=package:com.google.android.gm }
 *
 * # App updated
 * Intent { act=android.intent.action.PACKAGE_REPLACED dat=package:com.google.android.gm }
 *
 * # App removed
 * Intent { act=android.intent.action.PACKAGE_REMOVED dat=package:com.google.android.gm }
 * ```
 */
class PackageUpdateReceiver : BroadcastReceiver() {

    /**
     * Handle package broadcast from Android system.
     *
     * ## Implementation Notes:
     * - Extracts package name from intent.data.schemeSpecificPart
     * - Validates package name is not null/blank
     * - Routes to appropriate handler based on action
     * - Launches coroutine for async work (BroadcastReceiver constraint)
     *
     * @param context Android context for database/PackageManager access
     * @param intent Broadcast intent with action and package data
     */
    override fun onReceive(context: Context, intent: Intent) {
        // Extract package name from intent data (format: package:com.example.app)
        val packageName = intent.data?.schemeSpecificPart
        if (packageName.isNullOrBlank()) {
            Log.w(TAG, "Received package event with null or blank package name. Intent: ${intent.action}")
            return
        }

        // Route to handler based on action type
        when (intent.action) {
            Intent.ACTION_PACKAGE_ADDED -> handlePackageAdded(context, packageName, intent)
            Intent.ACTION_PACKAGE_REPLACED -> handlePackageReplaced(context, packageName)
            Intent.ACTION_PACKAGE_REMOVED -> handlePackageRemoved(context, packageName)
            else -> {
                Log.w(TAG, "Received unknown action: ${intent.action} for package: $packageName")
            }
        }
    }

    /**
     * Handle app installation.
     *
     * ## Important:
     * ACTION_PACKAGE_ADDED fires for both:
     * 1. Fresh install (EXTRA_REPLACING = false)
     * 2. Update install (EXTRA_REPLACING = true)
     *
     * For updates, we skip processing here because ACTION_PACKAGE_REPLACED
     * will also fire and handle it properly.
     *
     * @param context Android context
     * @param packageName Package identifier of installed app
     * @param intent Original intent with extras
     */
    private fun handlePackageAdded(context: Context, packageName: String, intent: Intent) {
        val isUpdate = intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)

        if (isUpdate) {
            // This is an update, ACTION_PACKAGE_REPLACED will handle it
            Log.d(TAG, "Package added (update): $packageName - will be handled by ACTION_PACKAGE_REPLACED")
            return
        }

        // Fresh install (not an update)
        Log.i(TAG, "App installed: $packageName")
        triggerVersionCheck(context, packageName, "INSTALL")
    }

    /**
     * Handle app update.
     *
     * Called when an existing app is updated to a new version.
     * This is the canonical event for app updates.
     *
     * @param context Android context
     * @param packageName Package identifier of updated app
     */
    private fun handlePackageReplaced(context: Context, packageName: String) {
        Log.i(TAG, "App updated: $packageName")
        triggerVersionCheck(context, packageName, "UPDATE")
    }

    /**
     * Handle app uninstallation.
     *
     * Called when an app is removed from the device.
     * AppVersionManager will detect AppNotInstalled and cleanup all commands.
     *
     * @param context Android context
     * @param packageName Package identifier of removed app
     */
    private fun handlePackageRemoved(context: Context, packageName: String) {
        Log.i(TAG, "App uninstalled: $packageName")
        triggerVersionCheck(context, packageName, "REMOVE")
    }

    /**
     * Trigger version check via AppVersionManager.
     *
     * ## Architecture:
     * This method launches a coroutine to handle the version check because:
     * 1. BroadcastReceiver.onReceive() must complete quickly (< 10 seconds)
     * 2. Database operations are suspend functions
     * 3. AppVersionManager may take several seconds for complex apps
     *
     * ## Error Handling:
     * Catches all exceptions to prevent receiver crashes.
     * Logs errors for debugging and monitoring.
     *
     * ## Lifecycle:
     * Uses CoroutineScope(Dispatchers.Default) which is NOT tied to any
     * lifecycle component. Work continues even if receiver is garbage collected.
     * This is safe because database operations are atomic and idempotent.
     *
     * @param context Android context for database access
     * @param packageName Package to check
     * @param eventType Event type for logging (INSTALL/UPDATE/REMOVE)
     */
    private fun triggerVersionCheck(context: Context, packageName: String, eventType: String) {
        // Launch coroutine for async database work
        // Note: CoroutineScope(Dispatchers.Default) is safe for BroadcastReceivers
        // because AppVersionManager operations are atomic and idempotent
        CoroutineScope(Dispatchers.Default).launch {
            try {
                // Get database instance (singleton pattern)
                val driverFactory = DatabaseDriverFactory(context.applicationContext)
                val databaseManager = VoiceOSDatabaseManager.getInstance(driverFactory)

                // Get repositories from database manager
                val versionRepo = databaseManager.appVersions
                val commandRepo = databaseManager.generatedCommands

                // Create version detector and manager
                val detector = AppVersionDetector(context.applicationContext, versionRepo)
                val manager = AppVersionManager(
                    context = context.applicationContext,
                    detector = detector,
                    versionRepo = versionRepo,
                    commandRepo = commandRepo
                )

                // Check and update app
                val result = manager.checkAndUpdateApp(packageName)

                // Log result for monitoring
                Log.d(TAG, "[$eventType] Version check result for $packageName: ${result.javaClass.simpleName}")

            } catch (e: Exception) {
                // Log error but don't crash receiver
                Log.e(TAG, "[$eventType] Failed to handle package event for $packageName", e)
            }
        }
    }

    companion object {
        private const val TAG = "PackageUpdateReceiver"
    }
}
