/**
 * AndroidAppLauncher.kt - Android implementation of IAppLauncher
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-27
 * Updated: 2026-01-27 - Added locale-aware alias generation
 *
 * Provides app discovery and launching capabilities on Android.
 * Used by AppHandler to execute "open app", "launch app" commands.
 */
package com.augmentalis.voiceoscore

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Build
import android.util.Log
import java.util.Locale

private const val TAG = "AndroidAppLauncher"

/**
 * Interface for providing locale-specific app aliases.
 *
 * Implementations can provide additional aliases for common apps
 * in different languages. For example, "Kamera" for Camera in German.
 */
interface IAppAliasProvider {
    /**
     * Get additional aliases for an app based on its package name.
     *
     * @param packageName The app's package identifier
     * @param displayName The app's localized display name
     * @return List of additional aliases for this app
     */
    fun getAliases(packageName: String, displayName: String): List<String>
}

/**
 * Android implementation of IAppLauncher.
 *
 * Uses PackageManager for app discovery and Intent for launching.
 * Discovers all launchable apps and generates voice-friendly aliases.
 * App names are automatically localized based on device locale.
 *
 * @param service The accessibility service context for launching apps
 * @param aliasProvider Optional provider for locale-specific aliases
 */
class AndroidAppLauncher(
    private val service: AccessibilityService,
    private val aliasProvider: IAppAliasProvider? = null
) : IAppLauncher {

    /**
     * Current device locale for alias generation.
     */
    private val currentLocale: Locale
        get() = Locale.getDefault()

    private val packageManager: PackageManager
        get() = service.packageManager

    /**
     * Launch an app by package name.
     *
     * Uses PackageManager.getLaunchIntentForPackage to get the launch intent.
     *
     * @param packageName The package identifier of the app to launch
     * @return true if launch succeeded, false otherwise
     */
    override fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(intent)
                Log.d(TAG, "Launched app: $packageName")
                true
            } else {
                Log.w(TAG, "No launch intent for package: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
            false
        }
    }

    /**
     * Get list of installed launchable apps.
     *
     * Queries PackageManager for all apps with a launcher activity.
     * Generates aliases for voice-friendly app recognition.
     *
     * @return List of installed app information with aliases
     */
    override fun getInstalledApps(): List<AppInfo> {
        return try {
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

            val activities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
            } else {
                @Suppress("DEPRECATION")
                packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            }

            activities.mapNotNull { resolveInfo ->
                try {
                    val appInfo = resolveInfo.activityInfo.applicationInfo
                    // App name is automatically localized based on device locale
                    val displayName = packageManager.getApplicationLabel(appInfo).toString()
                    val packageName = appInfo.packageName

                    // Generate aliases from display name + custom provider
                    val aliases = mutableListOf<String>()
                    aliases.addAll(generateAliases(displayName))
                    aliasProvider?.getAliases(packageName, displayName)?.let {
                        aliases.addAll(it)
                    }

                    AppInfo(
                        packageName = packageName,
                        displayName = displayName,
                        aliases = aliases.distinct()
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get app info for ${resolveInfo.activityInfo?.packageName}", e)
                    null
                }
            }.distinctBy { it.packageName }.also {
                Log.d(TAG, "Discovered ${it.size} launchable apps (locale: ${currentLocale.language})")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get installed apps", e)
            emptyList()
        }
    }

    /**
     * Generate voice-friendly aliases for an app name.
     *
     * Creates variations that users might say:
     * - Lowercase version
     * - First/last word for multi-word names
     * - Common app-specific aliases (maps, mail, camera, etc.)
     *
     * @param displayName The app's display name
     * @return List of aliases for voice recognition
     */
    private fun generateAliases(displayName: String): List<String> {
        val aliases = mutableListOf<String>()
        val lowerName = displayName.lowercase()

        // Add lowercase version if different
        if (lowerName != displayName) {
            aliases.add(lowerName)
        }

        // Add single-word version if multi-word
        val words = displayName.split(" ", "-", "_")
        if (words.size > 1) {
            // Add first word if meaningful (>= 3 chars)
            val firstWord = words.first()
            if (firstWord.length >= 3) {
                aliases.add(firstWord.lowercase())
            }
            // Add last word if meaningful and different
            val lastWord = words.last()
            if (lastWord.length >= 3 && lastWord.lowercase() != firstWord.lowercase()) {
                aliases.add(lastWord.lowercase())
            }
        }

        // Handle common app name patterns for better voice recognition
        when {
            lowerName.startsWith("google ") -> {
                aliases.add(lowerName.removePrefix("google "))
            }
            lowerName.contains("maps") -> {
                aliases.add("maps")
                aliases.add("navigation")
            }
            lowerName.contains("mail") || lowerName.contains("email") -> {
                aliases.add("mail")
                aliases.add("email")
            }
            lowerName.contains("calendar") -> {
                aliases.add("calendar")
            }
            lowerName.contains("camera") -> {
                aliases.add("camera")
            }
            lowerName.contains("phone") || lowerName.contains("dialer") -> {
                aliases.add("phone")
                aliases.add("dialer")
                aliases.add("call")
            }
            lowerName.contains("message") || lowerName.contains("sms") -> {
                aliases.add("messages")
                aliases.add("sms")
                aliases.add("text")
                aliases.add("texting")
            }
            lowerName.contains("settings") -> {
                aliases.add("settings")
            }
            lowerName.contains("clock") -> {
                aliases.add("clock")
                aliases.add("alarm")
                aliases.add("timer")
                aliases.add("stopwatch")
            }
            lowerName.contains("calculator") -> {
                aliases.add("calculator")
                aliases.add("calc")
            }
            lowerName.contains("chrome") -> {
                aliases.add("chrome")
                aliases.add("browser")
                aliases.add("web")
            }
            lowerName.contains("youtube") -> {
                aliases.add("youtube")
                aliases.add("videos")
            }
            lowerName.contains("photos") || lowerName.contains("gallery") -> {
                aliases.add("photos")
                aliases.add("gallery")
                aliases.add("pictures")
            }
            lowerName.contains("music") || lowerName.contains("spotify") -> {
                aliases.add("music")
            }
            lowerName.contains("weather") -> {
                aliases.add("weather")
            }
            lowerName.contains("notes") || lowerName.contains("keep") -> {
                aliases.add("notes")
            }
            lowerName.contains("files") || lowerName.contains("file manager") -> {
                aliases.add("files")
                aliases.add("file manager")
            }
        }

        return aliases.distinct()
    }
}
