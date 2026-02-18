/**
 * IosAppLauncher.kt - iOS implementation of IAppLauncher
 *
 * Launches apps via URL schemes using UIApplication.shared.openURL().
 * Provides a curated list of well-known iOS apps with their URL schemes,
 * filtered by canOpenURL() availability on the device.
 *
 * Note: iOS requires URL schemes to be declared in Info.plist under
 * LSApplicationQueriesSchemes for canOpenURL() to return true.
 * Without declaration, canOpenURL() returns false even if the app is installed.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import platform.Foundation.NSLog
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosAppLauncher : IAppLauncher {

    /**
     * Known iOS apps with their URL schemes and common aliases.
     * Ordered by usage frequency for faster matching.
     */
    private val knownApps: List<AppInfo> = listOf(
        AppInfo("maps://", "Maps", listOf("apple maps", "navigation", "directions")),
        AppInfo("tel://", "Phone", listOf("call", "dialer", "telephone")),
        AppInfo("sms://", "Messages", listOf("text", "imessage", "message")),
        AppInfo("mailto://", "Mail", listOf("email", "inbox")),
        AppInfo("x-apple-settings://", "Settings", listOf("preferences", "system settings")),
        AppInfo("photos-redirect://", "Photos", listOf("photo library", "camera roll", "gallery")),
        AppInfo("music://", "Music", listOf("apple music", "songs", "playlist")),
        AppInfo("calshow://", "Calendar", listOf("events", "schedule")),
        AppInfo("x-apple-reminder://", "Reminders", listOf("todos", "tasks")),
        AppInfo("facetime://", "FaceTime", listOf("video call")),
        AppInfo("shortcuts://", "Shortcuts", listOf("automations", "siri shortcuts")),
        AppInfo("App-prefs://", "Preferences", listOf("app settings")),
        AppInfo("https://www.youtube.com", "YouTube", listOf("videos", "youtube")),
        AppInfo("spotify://", "Spotify", listOf("spotify music")),
        AppInfo("whatsapp://", "WhatsApp", listOf("whats app")),
        AppInfo("fb://", "Facebook", listOf("meta")),
        AppInfo("instagram://", "Instagram", listOf("insta")),
        AppInfo("twitter://", "Twitter", listOf("x app")),
        AppInfo("slack://", "Slack", listOf("slack app")),
        AppInfo("googlechromes://", "Chrome", listOf("google chrome", "browser")),
    )

    override fun launchApp(packageName: String): Boolean {
        val url = NSURL(string = packageName) ?: run {
            NSLog("IosAppLauncher: Invalid URL scheme: $packageName")
            return false
        }

        return if (UIApplication.sharedApplication.canOpenURL(url)) {
            UIApplication.sharedApplication.openURL(url)
            NSLog("IosAppLauncher: Launched $packageName")
            true
        } else {
            NSLog("IosAppLauncher: Cannot open URL scheme: $packageName (app not installed or scheme not in LSApplicationQueriesSchemes)")
            false
        }
    }

    override fun getInstalledApps(): List<AppInfo> {
        return knownApps.filter { appInfo ->
            val url = NSURL(string = appInfo.packageName)
            UIApplication.sharedApplication.canOpenURL(url)
        }
    }
}
