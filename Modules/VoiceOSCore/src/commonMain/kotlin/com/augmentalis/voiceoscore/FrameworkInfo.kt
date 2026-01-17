package com.augmentalis.voiceoscore

/**
 * Represents the type of framework used by an application.
 * Used for determining how to interact with and scrape UI from different app types.
 */
enum class FrameworkType {
    /** Native Android (Kotlin/Java) or iOS (Swift/Objective-C) app */
    NATIVE,

    /** Flutter cross-platform framework (Dart) */
    FLUTTER,

    /** Unity game engine */
    UNITY,

    /** Unreal Engine game engine */
    UNREAL_ENGINE,

    /** React Native cross-platform framework (JavaScript/TypeScript) */
    REACT_NATIVE,

    /** Jetpack Compose declarative UI framework (Kotlin) */
    COMPOSE,

    /** WebView-based hybrid app (Cordova, Capacitor, PWA, etc.) */
    WEBVIEW,

    /** Framework could not be determined */
    UNKNOWN
}

/**
 * Contains information about the detected framework of an application.
 *
 * @property type The detected framework type
 * @property version The detected version of the framework (if available)
 * @property packageIndicators List of class names that indicate this framework was detected
 */
data class FrameworkInfo(
    val type: FrameworkType,
    val version: String? = null,
    val packageIndicators: List<String> = emptyList()
)

// Note: FrameworkDetector is defined in FrameworkDetector.kt with NodeInfo-based detection
// This file only contains FrameworkType enum and FrameworkInfo data class for simple detection results
