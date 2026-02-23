package com.augmentalis.intentactions.actions

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import com.augmentalis.intentactions.EntityType
import com.augmentalis.intentactions.ExtractedEntities
import com.augmentalis.intentactions.IIntentAction
import com.augmentalis.intentactions.IntentCategory
import com.augmentalis.intentactions.IntentResult
import com.augmentalis.intentactions.PlatformContext

/**
 * Plays a video by searching YouTube or opening the YouTube app.
 *
 * If a query entity is provided, searches YouTube for that query.
 * Otherwise, opens the YouTube app directly.
 */
object PlayVideoAction : IIntentAction {
    private const val TAG = "PlayVideoAction"

    override val intentId = "play_video"
    override val category = IntentCategory.MEDIA_LAUNCH
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        Log.d(TAG, "Playing video ${entities.toSafeString()}")

        val query = entities.query

        return try {
            val youtubeIntent = if (query.isNullOrBlank()) {
                Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:")).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                val searchUri = Uri.parse("vnd.youtube://results?search_query=${Uri.encode(query)}")
                Intent(Intent.ACTION_VIEW, searchUri).apply {
                    setPackage("com.google.android.youtube")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }

            context.startActivity(youtubeIntent)

            val responseMessage = if (query != null) "Playing video: $query" else "Opening YouTube"
            Log.i(TAG, responseMessage)
            IntentResult.Success(message = responseMessage)
        } catch (e: Exception) {
            Log.e(TAG, "YouTube app not found, falling back to web", e)
            val fallbackUrl = if (!query.isNullOrBlank()) {
                "https://www.youtube.com/results?search_query=${Uri.encode(query)}"
            } else {
                "https://www.youtube.com"
            }

            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(fallbackUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                IntentResult.Success(message = "Opening YouTube in browser")
            } catch (fallbackError: Exception) {
                IntentResult.Failed(
                    reason = "Failed to play video: ${fallbackError.message}",
                    exception = fallbackError
                )
            }
        }
    }
}

/**
 * Resumes music playback or opens a music app.
 *
 * Tries to resume the last playing media session, then falls back to
 * opening the default music app (Spotify, YouTube Music, etc.).
 */
object ResumeMusicAction : IIntentAction {
    private const val TAG = "ResumeMusicAction"

    override val intentId = "resume_music"
    override val category = IntentCategory.MEDIA_LAUNCH
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Resuming music")

            // Try to open music apps in priority order
            val musicApps = listOf(
                "com.spotify.music",
                "com.google.android.apps.youtube.music",
                "com.apple.android.music",
                "com.amazon.mp3"
            )

            for (pkg in musicApps) {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(pkg)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    val appName = try {
                        val appInfo = context.packageManager.getApplicationInfo(pkg, 0)
                        context.packageManager.getApplicationLabel(appInfo).toString()
                    } catch (e: Exception) {
                        pkg.substringAfterLast(".")
                    }
                    Log.i(TAG, "Opened music app: $appName")
                    return IntentResult.Success(message = "Opening $appName")
                }
            }

            // Fallback: open generic music intent
            val musicIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_APP_MUSIC)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            try {
                context.startActivity(musicIntent)
                IntentResult.Success(message = "Opening music app")
            } catch (e: Exception) {
                IntentResult.Failed(
                    reason = "No music app found. Please install a music app like Spotify.",
                    exception = e
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to resume music", e)
            IntentResult.Failed(
                reason = "Failed to open music: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Opens the default web browser.
 */
object OpenBrowserAction : IIntentAction {
    private const val TAG = "OpenBrowserAction"

    override val intentId = "open_browser"
    override val category = IntentCategory.MEDIA_LAUNCH
    override val requiredEntities = emptyList<EntityType>()

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Opening browser")

            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(browserIntent)

            Log.i(TAG, "Opened browser")
            IntentResult.Success(message = "Opening browser")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open browser", e)
            IntentResult.Failed(
                reason = "Failed to open browser: ${e.message}",
                exception = e
            )
        }
    }
}

/**
 * Opens an app by name.
 *
 * Searches installed apps by label matching against a known package map
 * and the system package manager.
 */
object OpenAppAction : IIntentAction {
    private const val TAG = "OpenAppAction"

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

    override val intentId = "open_app"
    override val category = IntentCategory.MEDIA_LAUNCH
    override val requiredEntities = listOf(EntityType.APP_NAME)

    override suspend fun execute(context: PlatformContext, entities: ExtractedEntities): IntentResult {
        return try {
            Log.d(TAG, "Opening app ${entities.toSafeString()}")

            val appName = entities.appName
            if (appName.isNullOrBlank()) {
                return IntentResult.NeedsMoreInfo(
                    missingEntity = EntityType.APP_NAME,
                    prompt = "Which app would you like to open?"
                )
            }

            Log.d(TAG, "Looking for app: '$appName'")

            val packageName = findPackageName(context, appName)

            if (packageName == null) {
                Log.w(TAG, "App not found: $appName")
                return IntentResult.Failed(
                    reason = "I couldn't find an app called '$appName'. Please make sure it's installed."
                )
            }

            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)

                val displayName = getAppDisplayName(context, packageName)
                Log.i(TAG, "Launched app: $packageName ($displayName)")
                IntentResult.Success(message = "Opening $displayName")
            } else {
                Log.w(TAG, "No launch intent for package: $packageName")
                IntentResult.Failed(
                    reason = "I found '$appName' but couldn't launch it."
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open app", e)
            IntentResult.Failed(
                reason = "Failed to open app: ${e.message}",
                exception = e
            )
        }
    }

    private fun findPackageName(context: PlatformContext, appName: String): String? {
        val lowerName = appName.lowercase().trim()

        // Check known mappings first
        APP_PACKAGES[lowerName]?.let { pkg ->
            if (isPackageInstalled(context, pkg)) return pkg
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

        // Try partial matches in mappings
        for ((name, pkg) in APP_PACKAGES) {
            if (name.contains(lowerName) || lowerName.contains(name)) {
                if (isPackageInstalled(context, pkg)) return pkg
            }
        }

        return null
    }

    private fun isPackageInstalled(context: PlatformContext, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    private fun getAppDisplayName(context: PlatformContext, packageName: String): String {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            context.packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast(".")
        }
    }
}
