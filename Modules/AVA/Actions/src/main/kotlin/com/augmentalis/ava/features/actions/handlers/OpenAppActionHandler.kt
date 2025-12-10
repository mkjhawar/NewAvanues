package com.augmentalis.ava.features.actions.handlers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.ava.features.actions.ActionResult
import com.augmentalis.ava.features.actions.IntentActionHandler

/**
 * Action handler for opening apps by name.
 *
 * Launches apps based on the app name extracted from the user's utterance.
 * Supports common app names with aliases (e.g., "Gmail" -> "com.google.android.gm").
 *
 * Behavior:
 * - Searches for the app by name in installed packages
 * - Uses common app package mappings for popular apps
 * - Falls back to package manager search if not in mappings
 * - Uses FLAG_ACTIVITY_NEW_TASK for launching from non-activity context
 *
 * Intent classification examples:
 * - "Open Gmail"
 * - "Open app Gmail"
 * - "Launch Chrome"
 * - "Start YouTube"
 *
 * @see IntentActionHandler
 */
class OpenAppActionHandler : IntentActionHandler {

    companion object {
        private const val TAG = "OpenAppHandler"

        // Common app package mappings
        private val APP_PACKAGES = mapOf(
            "gmail" to "com.google.android.gm",
            "mail" to "com.google.android.gm",
            "email" to "com.google.android.gm",
            "chrome" to "com.android.chrome",
            "browser" to "com.android.chrome",
            "youtube" to "com.google.android.youtube",
            "maps" to "com.google.android.apps.maps",
            "google maps" to "com.google.android.apps.maps",
            "play store" to "com.android.vending",
            "store" to "com.android.vending",
            "camera" to "com.android.camera",
            "photos" to "com.google.android.apps.photos",
            "gallery" to "com.google.android.apps.photos",
            "messages" to "com.google.android.apps.messaging",
            "sms" to "com.google.android.apps.messaging",
            "phone" to "com.android.dialer",
            "dialer" to "com.android.dialer",
            "contacts" to "com.android.contacts",
            "calendar" to "com.google.android.calendar",
            "drive" to "com.google.android.apps.docs",
            "google drive" to "com.google.android.apps.docs",
            "keep" to "com.google.android.keep",
            "notes" to "com.google.android.keep",
            "translate" to "com.google.android.apps.translate",
            "clock" to "com.google.android.deskclock",
            "alarm" to "com.google.android.deskclock",
            "calculator" to "com.google.android.calculator",
            "files" to "com.google.android.apps.nbu.files",
            "file manager" to "com.google.android.apps.nbu.files",
            "whatsapp" to "com.whatsapp",
            "telegram" to "org.telegram.messenger",
            "facebook" to "com.facebook.katana",
            "instagram" to "com.instagram.android",
            "twitter" to "com.twitter.android",
            "x" to "com.twitter.android",
            "spotify" to "com.spotify.music",
            "netflix" to "com.netflix.mediaclient",
            "amazon" to "com.amazon.mShop.android.shopping",
            "uber" to "com.ubercab",
            "lyft" to "me.lyft.android",
            "slack" to "com.Slack",
            "zoom" to "us.zoom.videomeetings",
            "teams" to "com.microsoft.teams",
            "outlook" to "com.microsoft.office.outlook",
            "word" to "com.microsoft.office.word",
            "excel" to "com.microsoft.office.excel",
            "powerpoint" to "com.microsoft.office.powerpoint"
        )
    }

    override val intent = "open_app"

    override suspend fun execute(context: Context, utterance: String): ActionResult {
        return try {
            Log.d(TAG, "Opening app for utterance: '$utterance'")

            // Extract app name from utterance
            val appName = extractAppName(utterance)

            if (appName.isNullOrBlank()) {
                return ActionResult.Failure(
                    message = "Could not determine which app to open. Please say something like 'Open Gmail' or 'Launch Chrome'."
                )
            }

            Log.d(TAG, "Extracted app name: '$appName'")

            // Try to find package name
            val packageName = findPackageName(context, appName)

            if (packageName == null) {
                Log.w(TAG, "App not found: $appName")
                return ActionResult.Failure(
                    message = "I couldn't find an app called '$appName'. Please make sure it's installed."
                )
            }

            // Launch the app
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)

                Log.i(TAG, "Launched app: $packageName")
                ActionResult.Success(
                    message = "Opening ${getAppDisplayName(context, packageName)}"
                )
            } else {
                Log.w(TAG, "No launch intent for package: $packageName")
                ActionResult.Failure(
                    message = "I found '$appName' but couldn't launch it."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app", e)
            ActionResult.Failure(
                message = "Failed to open app: ${e.message}",
                exception = e
            )
        }
    }

    /**
     * Extract app name from the utterance.
     */
    private fun extractAppName(utterance: String): String? {
        val lower = utterance.lowercase().trim()

        // Common patterns to extract app name
        val patterns = listOf(
            "open app ",
            "open the ",
            "open ",
            "launch ",
            "start ",
            "run "
        )

        for (pattern in patterns) {
            if (lower.startsWith(pattern)) {
                val remaining = lower.removePrefix(pattern).trim()
                if (remaining.isNotEmpty()) {
                    return remaining
                }
            }
        }

        // Check if the utterance is just the app name
        return if (lower.isNotBlank() && !lower.contains(" ")) {
            lower
        } else {
            null
        }
    }

    /**
     * Find the package name for an app name.
     */
    private fun findPackageName(context: Context, appName: String): String? {
        val lowerName = appName.lowercase().trim()

        // Check our known mappings first
        APP_PACKAGES[lowerName]?.let { pkg ->
            if (isPackageInstalled(context, pkg)) {
                return pkg
            }
        }

        // Search installed apps by label
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

        for (app in installedApps) {
            val label = pm.getApplicationLabel(app).toString().lowercase()
            if (label == lowerName || label.contains(lowerName)) {
                return app.packageName
            }
        }

        // Try partial matches in our mappings
        for ((name, pkg) in APP_PACKAGES) {
            if (name.contains(lowerName) || lowerName.contains(name)) {
                if (isPackageInstalled(context, pkg)) {
                    return pkg
                }
            }
        }

        return null
    }

    /**
     * Check if a package is installed.
     */
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Get the display name for an app package.
     */
    private fun getAppDisplayName(context: Context, packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }
}
