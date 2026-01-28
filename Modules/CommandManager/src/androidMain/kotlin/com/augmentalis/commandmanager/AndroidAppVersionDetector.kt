/**
 * AndroidAppVersionDetector.kt - Android app version detection
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-19
 *
 * Android-specific implementation using PackageManager for version detection.
 */
package com.augmentalis.commandmanager

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Android implementation of IAppVersionDetector.
 *
 * Uses PackageManager for version info and BroadcastReceiver for update monitoring.
 */
class AndroidAppVersionDetector(
    private val context: Context
) : IAppVersionDetector {

    companion object {
        private const val TAG = "AppVersionDetector"
    }

    private val versionTracker = InMemoryVersionTracker()
    private val listeners = mutableListOf<VersionChangeListener>()
    private var packageReceiver: BroadcastReceiver? = null
    private var isMonitoring = false
    private var monitoringScope: CoroutineScope? = null

    override suspend fun getVersion(packageName: String): AppVersion? = withContext(Dispatchers.IO) {
        try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            AppVersion(
                versionName = packageInfo.versionName ?: "unknown",
                versionCode = versionCode
            )
        } catch (e: PackageManager.NameNotFoundException) {
            Log.v(TAG, "Package not found: $packageName")
            null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting version for $packageName", e)
            null
        }
    }

    override suspend fun detectVersionChange(packageName: String): VersionChange = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getVersion(packageName)
            if (currentVersion == null) {
                versionTracker.remove(packageName)
                return@withContext VersionChange.NotFound(packageName)
            }

            val storedVersion = versionTracker.get(packageName)
            if (storedVersion == null) {
                versionTracker.set(packageName, currentVersion)
                return@withContext VersionChange.NewApp(packageName, currentVersion)
            }

            val change = when {
                currentVersion.versionCode > storedVersion.versionCode -> {
                    versionTracker.set(packageName, currentVersion)
                    VersionChange.Upgraded(packageName, storedVersion, currentVersion)
                }
                currentVersion.versionCode < storedVersion.versionCode -> {
                    versionTracker.set(packageName, currentVersion)
                    VersionChange.Downgraded(packageName, storedVersion, currentVersion)
                }
                else -> {
                    VersionChange.NoChange(packageName, currentVersion)
                }
            }

            if (change.requiresCommandInvalidation) {
                notifyListeners(change)
            }

            change
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting version change for $packageName", e)
            VersionChange.Error(packageName, e.message ?: "Unknown error")
        }
    }

    override suspend fun checkForegroundApp(): VersionChange? {
        // Would need ActivityManager.getRunningTasks which requires permission
        // Return null - caller should provide the package name
        return null
    }

    override fun addListener(listener: VersionChangeListener) {
        synchronized(listeners) {
            if (!listeners.contains(listener)) {
                listeners.add(listener)
            }
        }
    }

    override fun removeListener(listener: VersionChangeListener) {
        synchronized(listeners) {
            listeners.remove(listener)
        }
    }

    override fun startMonitoring() {
        if (isMonitoring) return

        // Create a dedicated scope for monitoring with SupervisorJob for fault tolerance
        monitoringScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val packageName = intent?.data?.schemeSpecificPart ?: return
                Log.d(TAG, "Package changed: $packageName, action: ${intent.action}")

                // Check for version change using the monitoring scope
                monitoringScope?.launch {
                    val change = detectVersionChange(packageName)
                    Log.d(TAG, "Version change detected: $change")
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addDataScheme("package")
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.registerReceiver(packageReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                context.registerReceiver(packageReceiver, filter)
            }
            isMonitoring = true
            Log.i(TAG, "Started monitoring package updates")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register package receiver", e)
        }
    }

    override fun stopMonitoring() {
        if (!isMonitoring) return

        try {
            // Cancel all pending coroutines in the monitoring scope
            monitoringScope?.cancel()
            monitoringScope = null

            packageReceiver?.let {
                context.unregisterReceiver(it)
            }
            packageReceiver = null
            isMonitoring = false
            Log.i(TAG, "Stopped monitoring package updates")
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering package receiver", e)
        }
    }

    private fun notifyListeners(change: VersionChange) {
        synchronized(listeners) {
            listeners.forEach { listener ->
                try {
                    listener.onVersionChange(change)
                } catch (e: Exception) {
                    Log.e(TAG, "Error notifying version change listener", e)
                }
            }
        }
    }
}

/**
 * Factory function to create Android version detector.
 */
fun createAppVersionDetector(context: Context): IAppVersionDetector {
    return AndroidAppVersionDetector(context)
}
