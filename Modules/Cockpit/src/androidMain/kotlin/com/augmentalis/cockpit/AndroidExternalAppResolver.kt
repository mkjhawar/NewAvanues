package com.augmentalis.cockpit

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build

/**
 * Android implementation of [IExternalAppResolver].
 *
 * Uses [PackageManager] to check if apps are installed and whether they
 * support activity embedding (API 33+ `allowUntrustedActivityEmbedding`).
 * Launches apps adjacent using [Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT].
 *
 * Activity embedding detection:
 * - On Android 12L+ (API 32): checks `ActivityInfo.FLAG_ALLOW_UNTRUSTED_ACTIVITY_EMBEDDING`
 * - On older APIs: always returns [ExternalAppStatus.INSTALLED_NO_EMBED]
 *
 * The adjacent launch mode places the target app in a split-screen arrangement
 * next to the Cockpit, leveraging the system's multi-window support.
 *
 * @param context Application or activity context for PackageManager access
 */
class AndroidExternalAppResolver(private val context: Context) : IExternalAppResolver {

    override fun resolveApp(packageName: String): ExternalAppStatus {
        if (packageName.isBlank()) return ExternalAppStatus.NOT_INSTALLED

        val pm = context.packageManager
        val appInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getApplicationInfo(packageName, PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getApplicationInfo(packageName, 0)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            return ExternalAppStatus.NOT_INSTALLED
        }

        // Check if the app opts into untrusted activity embedding (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val launchIntent = pm.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                val activityName = launchIntent.component?.className ?: ""
                if (activityName.isNotBlank()) {
                    try {
                        val activityInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pm.getActivityInfo(
                                launchIntent.component!!,
                                PackageManager.ComponentInfoFlags.of(0)
                            )
                        } else {
                            @Suppress("DEPRECATION")
                            pm.getActivityInfo(launchIntent.component!!, 0)
                        }
                        // FLAG_ALLOW_UNTRUSTED_ACTIVITY_EMBEDDING = 0x00000800 (API 33+)
                        if (Build.VERSION.SDK_INT >= 33) {
                            val embedFlag = 0x00000800 // ActivityInfo constant
                            if (activityInfo.flags and embedFlag != 0) {
                                return ExternalAppStatus.EMBEDDABLE
                            }
                        }
                    } catch (_: PackageManager.NameNotFoundException) {
                        // Activity not found, fall through to NO_EMBED
                    }
                }
            }
        }

        return ExternalAppStatus.INSTALLED_NO_EMBED
    }

    override fun launchAdjacent(packageName: String, activityName: String) {
        if (packageName.isBlank()) return

        val intent = if (activityName.isNotBlank()) {
            Intent().apply {
                setClassName(packageName, activityName)
            }
        } else {
            context.packageManager.getLaunchIntentForPackage(packageName) ?: return
        }

        intent.addFlags(
            Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        )

        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // App may have been uninstalled between resolve and launch
        }
    }
}
