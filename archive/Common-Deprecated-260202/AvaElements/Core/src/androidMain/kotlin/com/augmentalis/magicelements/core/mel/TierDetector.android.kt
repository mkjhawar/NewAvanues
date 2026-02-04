package com.augmentalis.magicelements.core.mel

/**
 * Android platform implementation for tier detection.
 *
 * Android supports both Tier 1 (DATA) and Tier 2 (LOGIC) plugins.
 * There are no app store restrictions on interpreted expressions on Android.
 *
 * @return Platform.ANDROID indicating full Tier 2 support
 */
actual fun getCurrentPlatform(): Platform {
    return Platform.ANDROID
}
