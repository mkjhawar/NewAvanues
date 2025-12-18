/**
 * AppFramework.kt - App framework detection enum
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Enum representing detected app frameworks for cross-platform support.
 */

package com.augmentalis.voiceoscore.learnapp.detection

/**
 * App Framework
 *
 * Represents the detected framework/technology used to build an app.
 * Used for framework-specific element processing and label generation.
 */
enum class AppFramework {
    /**
     * Native Android app (standard Android SDK/Jetpack Compose)
     */
    NATIVE,

    /**
     * Flutter app (Google's cross-platform framework)
     * Detected by: SemanticsNode class, flutter package prefix
     */
    FLUTTER,

    /**
     * React Native app (Facebook's cross-platform framework)
     * Detected by: ReactRoot, ReactViewGroup classes
     */
    REACT_NATIVE,

    /**
     * Unity game engine
     * Detected by: UnityPlayer, com.unity3d package
     */
    UNITY,

    /**
     * Unreal Engine
     * Detected by: com.epicgames package, Unreal class names
     */
    UNREAL,

    /**
     * Web-based app (WebView, PWA, Cordova, Ionic)
     * Detected by: WebView containing web content
     */
    WEB_BASED,

    /**
     * Xamarin/MAUI (.NET cross-platform)
     * Detected by: Xamarin namespace, MAUI components
     */
    XAMARIN,

    /**
     * Unknown framework (could not be detected)
     */
    UNKNOWN;

    /**
     * Get minimum label length adjusted for framework
     * Cross-platform frameworks often have shorter labels
     */
    fun getMinLabelLength(defaultLength: Int): Int {
        return when (this) {
            UNITY, UNREAL -> 1  // Game engines often have icon-only buttons
            FLUTTER, REACT_NATIVE -> 2  // Cross-platform may have short labels
            WEB_BASED -> 2
            else -> defaultLength
        }
    }

    /**
     * Whether this framework needs aggressive fallback label generation
     * (e.g., game engines with no semantic information)
     */
    fun needsAggressiveFallback(): Boolean {
        return this == UNITY || this == UNREAL
    }

    /**
     * Whether this framework needs moderate fallback label generation
     */
    fun needsModerateFallback(): Boolean {
        return this == FLUTTER || this == REACT_NATIVE || this == WEB_BASED
    }
}
