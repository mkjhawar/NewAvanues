/**
 * AndroidAppLauncher.kt - Android app launcher implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-09
 *
 * Android implementation of IAppLauncher using AccessibilityService context.
 */
package com.augmentalis.voiceoscoreng.handlers

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo

/**
 * Android implementation of [IAppLauncher].
 *
 * Uses AccessibilityService context to launch apps and query installed apps.
 *
 * @param accessibilityServiceProvider Provider for the accessibility service instance
 */
class AndroidAppLauncher(
    private val accessibilityServiceProvider: () -> AccessibilityService?
) : IAppLauncher {

    /**
     * Launch an app by package name.
     *
     * @param packageName The package identifier of the app to launch
     * @return true if launch succeeded, false otherwise
     */
    override fun launchApp(packageName: String): Boolean {
        val service = accessibilityServiceProvider() ?: return false

        return try {
            val launchIntent = service.packageManager
                .getLaunchIntentForPackage(packageName)
                ?: return false

            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(launchIntent)
            true
        } catch (e: Exception) {
            println("[AndroidAppLauncher] Failed to launch $packageName: ${e.message}")
            false
        }
    }

    /**
     * Get list of installed apps on the device.
     *
     * Returns launchable apps with their display names and package names.
     * Also generates common aliases based on app names.
     *
     * @return List of installed app information
     */
    override fun getInstalledApps(): List<AppInfo> {
        val service = accessibilityServiceProvider() ?: return emptyList()

        return try {
            val intent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }

            val resolveInfoList: List<ResolveInfo> = service.packageManager
                .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)

            resolveInfoList.mapNotNull { resolveInfo ->
                val packageName = resolveInfo.activityInfo.packageName
                val displayName = resolveInfo.loadLabel(service.packageManager).toString()

                // Generate aliases from display name
                val aliases = generateAliases(displayName, packageName)

                AppInfo(
                    packageName = packageName,
                    displayName = displayName,
                    aliases = aliases
                )
            }.distinctBy { it.packageName }
        } catch (e: Exception) {
            println("[AndroidAppLauncher] Failed to get installed apps: ${e.message}")
            emptyList()
        }
    }

    /**
     * Generate common aliases for an app based on its display name.
     *
     * For example:
     * - "Google Maps" -> ["maps", "google maps"]
     * - "Clock" -> ["clock", "alarm", "timer", "stopwatch"]
     * - "Calculator" -> ["calculator", "calc"]
     */
    private fun generateAliases(displayName: String, packageName: String): List<String> {
        val aliases = mutableListOf<String>()
        val lowerName = displayName.lowercase()

        // Add the display name itself (lowercase)
        aliases.add(lowerName)

        // Add last word if multi-word (e.g., "Google Maps" -> "maps")
        val words = lowerName.split(" ")
        if (words.size > 1) {
            aliases.add(words.last())
        }

        // Add common aliases for known app types
        when {
            // Clock/Time apps
            lowerName.contains("clock") || packageName.contains("deskclock") -> {
                aliases.addAll(listOf("clock", "alarm", "timer", "stopwatch", "alarms", "timers"))
            }
            // Calculator
            lowerName.contains("calculator") || lowerName.contains("calc") -> {
                aliases.addAll(listOf("calculator", "calc"))
            }
            // Camera
            lowerName.contains("camera") -> {
                aliases.addAll(listOf("camera", "photo", "photos"))
            }
            // Calendar
            lowerName.contains("calendar") -> {
                aliases.addAll(listOf("calendar", "schedule", "events"))
            }
            // Phone/Dialer
            lowerName.contains("phone") || lowerName.contains("dialer") -> {
                aliases.addAll(listOf("phone", "dialer", "call", "calls"))
            }
            // Messages/SMS
            lowerName.contains("message") || lowerName.contains("sms") -> {
                aliases.addAll(listOf("messages", "sms", "text", "texting"))
            }
            // Contacts
            lowerName.contains("contact") -> {
                aliases.addAll(listOf("contacts", "people"))
            }
            // Browser
            lowerName.contains("browser") || lowerName.contains("chrome") ||
            lowerName.contains("firefox") || lowerName.contains("edge") -> {
                aliases.addAll(listOf("browser", "internet", "web"))
            }
            // Email
            lowerName.contains("mail") || lowerName.contains("gmail") -> {
                aliases.addAll(listOf("email", "mail", "gmail"))
            }
            // Maps/Navigation
            lowerName.contains("map") || lowerName.contains("navigation") -> {
                aliases.addAll(listOf("maps", "map", "navigation", "navigate", "directions"))
            }
            // Music
            lowerName.contains("music") || lowerName.contains("spotify") -> {
                aliases.addAll(listOf("music", "songs", "playlist"))
            }
            // Gallery/Photos
            lowerName.contains("gallery") || lowerName.contains("photos") -> {
                aliases.addAll(listOf("gallery", "photos", "pictures", "images"))
            }
            // Settings
            lowerName.contains("setting") -> {
                aliases.addAll(listOf("settings", "preferences", "options"))
            }
            // Files/File Manager
            lowerName.contains("file") -> {
                aliases.addAll(listOf("files", "file manager", "documents"))
            }
            // Notes
            lowerName.contains("note") -> {
                aliases.addAll(listOf("notes", "notepad", "memo"))
            }
            // Weather
            lowerName.contains("weather") -> {
                aliases.addAll(listOf("weather", "forecast"))
            }
        }

        return aliases.distinct()
    }

    companion object {
        /**
         * Create launcher from accessibility service.
         */
        fun create(service: AccessibilityService): AndroidAppLauncher {
            return AndroidAppLauncher { service }
        }

        /**
         * Create launcher from service provider.
         */
        fun create(serviceProvider: () -> AccessibilityService?): AndroidAppLauncher {
            return AndroidAppLauncher(serviceProvider)
        }
    }
}
