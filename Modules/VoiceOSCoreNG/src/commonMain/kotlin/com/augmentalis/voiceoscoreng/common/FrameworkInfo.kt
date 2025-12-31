package com.augmentalis.voiceoscoreng.common

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

/**
 * Detects the framework used by an application based on package name and class names.
 *
 * Detection is performed by pattern matching against known framework-specific class prefixes.
 * The detection follows a priority order to handle cases where multiple frameworks might
 * be detected (e.g., a Flutter app using WebView).
 *
 * Priority order (highest to lowest):
 * 1. Flutter (io.flutter.)
 * 2. Unity (com.unity3d.)
 * 3. Unreal Engine (com.epicgames.)
 * 4. React Native (com.facebook.react.)
 * 5. WebView (android.webkit., org.xwalk., org.chromium., org.apache.cordova.)
 * 6. Native (default when no other framework is detected)
 */
object FrameworkDetector {

    // Framework detection patterns - ordered by specificity
    private val FLUTTER_PREFIXES = listOf(
        "io.flutter."
    )

    private val UNITY_PREFIXES = listOf(
        "com.unity3d."
    )

    private val UNREAL_PREFIXES = listOf(
        "com.epicgames."
    )

    private val REACT_NATIVE_PREFIXES = listOf(
        "com.facebook.react."
    )

    private val WEBVIEW_PREFIXES = listOf(
        "android.webkit.WebView",
        "android.webkit.WebViewClient",
        "android.webkit.WebChromeClient",
        "org.xwalk.core.",
        "org.chromium.content.",
        "org.chromium.base.",
        "org.apache.cordova."
    )

    /**
     * Detects the framework used by an application.
     *
     * @param packageName The package name of the application
     * @param classNames List of class names found in the application
     * @return FrameworkInfo containing the detected framework type and matching indicators
     */
    fun detect(packageName: String, classNames: List<String>): FrameworkInfo {
        // Check for each framework in priority order

        // 1. Check for Flutter
        val flutterIndicators = findMatchingClasses(classNames, FLUTTER_PREFIXES)
        if (flutterIndicators.isNotEmpty()) {
            return FrameworkInfo(
                type = FrameworkType.FLUTTER,
                version = null, // Version detection would require additional analysis
                packageIndicators = flutterIndicators
            )
        }

        // 2. Check for Unity
        val unityIndicators = findMatchingClasses(classNames, UNITY_PREFIXES)
        if (unityIndicators.isNotEmpty()) {
            return FrameworkInfo(
                type = FrameworkType.UNITY,
                version = null,
                packageIndicators = unityIndicators
            )
        }

        // 3. Check for Unreal Engine
        val unrealIndicators = findMatchingClasses(classNames, UNREAL_PREFIXES)
        if (unrealIndicators.isNotEmpty()) {
            return FrameworkInfo(
                type = FrameworkType.UNREAL_ENGINE,
                version = null,
                packageIndicators = unrealIndicators
            )
        }

        // 4. Check for React Native
        val reactNativeIndicators = findMatchingClasses(classNames, REACT_NATIVE_PREFIXES)
        if (reactNativeIndicators.isNotEmpty()) {
            return FrameworkInfo(
                type = FrameworkType.REACT_NATIVE,
                version = null,
                packageIndicators = reactNativeIndicators
            )
        }

        // 5. Check for WebView-based apps
        val webViewIndicators = findMatchingClasses(classNames, WEBVIEW_PREFIXES)
        if (webViewIndicators.isNotEmpty()) {
            return FrameworkInfo(
                type = FrameworkType.WEBVIEW,
                version = null,
                packageIndicators = webViewIndicators
            )
        }

        // 6. Default to NATIVE for regular Android/iOS apps
        return FrameworkInfo(
            type = FrameworkType.NATIVE,
            version = null,
            packageIndicators = emptyList()
        )
    }

    /**
     * Finds all class names that match any of the given prefixes.
     *
     * @param classNames List of class names to search
     * @param prefixes List of prefixes to match against
     * @return List of class names that match at least one prefix
     */
    private fun findMatchingClasses(classNames: List<String>, prefixes: List<String>): List<String> {
        return classNames.filter { className ->
            prefixes.any { prefix ->
                // Check if the class name starts with the prefix (case-sensitive)
                className.startsWith(prefix)
            }
        }
    }
}
