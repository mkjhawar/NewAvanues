/**
 * IosExternalAppResolver.kt — iOS implementation of IExternalAppResolver
 *
 * On iOS, external apps can be launched via URL schemes (e.g., "safari://",
 * "maps://", custom deep links). App installation status is checked via
 * UIApplication.canOpenURL().
 *
 * For embedded app views (like Android's FLAG_ACTIVITY_LAUNCH_ADJACENT),
 * iOS does not have a direct equivalent. Future: SFSafariViewController,
 * MFMailComposeViewController, or UIActivityViewController for specific content types.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.cockpit

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

/**
 * iOS implementation of [IExternalAppResolver].
 *
 * Checks app availability via URL schemes and launches apps via
 * [UIApplication.openURL]. Package names are mapped to iOS URL schemes
 * using a static registry.
 */
class IosExternalAppResolver : IExternalAppResolver {

    /**
     * Maps Android-style package names to iOS URL schemes.
     * Extend this map as needed for new integrations.
     */
    private val urlSchemeMap = mapOf(
        "com.android.chrome" to "googlechrome://",
        "com.google.android.apps.maps" to "comgooglemaps://",
        "com.google.android.youtube" to "youtube://",
        "com.spotify.music" to "spotify://",
        "com.apple.mobilesafari" to "https://",
    )

    override fun resolveApp(packageName: String): ExternalAppStatus {
        val scheme = urlSchemeMap[packageName]
            ?: return ExternalAppStatus.NOT_INSTALLED

        val url = NSURL.URLWithString(scheme)
            ?: return ExternalAppStatus.NOT_INSTALLED

        return if (UIApplication.sharedApplication.canOpenURL(url)) {
            ExternalAppStatus.INSTALLED
        } else {
            ExternalAppStatus.NOT_INSTALLED
        }
    }

    override fun launchAdjacent(packageName: String, activityName: String) {
        val scheme = urlSchemeMap[packageName] ?: return
        val url = NSURL.URLWithString(scheme) ?: return

        UIApplication.sharedApplication.openURL(
            url,
            options = emptyMap<Any?, Any>(),
            completionHandler = null
        )
    }
}
