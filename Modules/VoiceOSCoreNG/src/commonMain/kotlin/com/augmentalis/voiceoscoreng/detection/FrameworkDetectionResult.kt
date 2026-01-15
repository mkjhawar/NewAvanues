/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * FrameworkDetectionResult.kt - Framework detection result data class
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: LearnAppCore CrossPlatformDetector.kt
 *
 * Contains the result of framework detection including the detected framework,
 * confidence level, and additional metadata.
 */

package com.augmentalis.voiceoscoreng.detection

/**
 * Result of framework detection
 *
 * Contains the detected framework type, confidence level, and additional
 * metadata about how the detection was made.
 *
 * @property framework The detected app framework
 * @property confidence Confidence level of the detection (0.0 to 1.0)
 * @property flutterVersion For Flutter apps, the detected version
 * @property detectionSignals List of signals that led to the detection
 * @property packageName The package/bundle identifier of the app
 * @property timestamp When the detection was performed (epoch millis)
 */
data class FrameworkDetectionResult(
    val framework: AppFramework,
    val confidence: Float = 1.0f,
    val flutterVersion: FlutterVersion = FlutterVersion.NOT_FLUTTER,
    val detectionSignals: List<DetectionSignal> = emptyList(),
    val packageName: String = "",
    val timestamp: Long = 0L
) {
    /**
     * Check if the detection is high confidence
     *
     * @return true if confidence >= 0.8
     */
    fun isHighConfidence(): Boolean = confidence >= 0.8f

    /**
     * Check if the detection is medium confidence
     *
     * @return true if confidence >= 0.5 and < 0.8
     */
    fun isMediumConfidence(): Boolean = confidence >= 0.5f && confidence < 0.8f

    /**
     * Check if the detection is low confidence
     *
     * @return true if confidence < 0.5
     */
    fun isLowConfidence(): Boolean = confidence < 0.5f

    /**
     * Check if the app is a game
     *
     * @return true if the detected framework is a game engine
     */
    fun isGame(): Boolean = framework.isGameEngine()

    /**
     * Check if the app needs special accessibility handling
     *
     * @return true if the framework typically needs fallback handling
     */
    fun needsSpecialHandling(): Boolean =
        framework.needsAggressiveFallback() || framework.needsModerateFallback()

    /**
     * Get a human-readable summary of the detection
     *
     * @return Summary string
     */
    fun toSummary(): String {
        val confidenceStr = when {
            isHighConfidence() -> "high"
            isMediumConfidence() -> "medium"
            else -> "low"
        }
        val flutterStr = if (framework == AppFramework.FLUTTER) {
            " (${flutterVersion.name.lowercase().replace("_", " ")})"
        } else ""
        return "$framework$flutterStr detected with $confidenceStr confidence"
    }

    companion object {
        /**
         * Create a native framework result (default)
         */
        fun native(packageName: String = "", timestamp: Long = 0L): FrameworkDetectionResult {
            return FrameworkDetectionResult(
                framework = AppFramework.NATIVE,
                confidence = 1.0f,
                packageName = packageName,
                timestamp = timestamp
            )
        }

        /**
         * Create an unknown framework result
         */
        fun unknown(packageName: String = "", timestamp: Long = 0L): FrameworkDetectionResult {
            return FrameworkDetectionResult(
                framework = AppFramework.UNKNOWN,
                confidence = 0.0f,
                packageName = packageName,
                timestamp = timestamp
            )
        }
    }
}

/**
 * Detection signal indicating what triggered the framework detection
 *
 * @property type The type of signal
 * @property value The actual value that matched
 * @property source Where the signal was found
 */
data class DetectionSignal(
    val type: SignalType,
    val value: String,
    val source: String = ""
) {
    override fun toString(): String = "$type: '$value'" + if (source.isNotEmpty()) " in $source" else ""
}

/**
 * Types of detection signals
 */
enum class SignalType {
    /** Class name match (e.g., FlutterView, UnityPlayer) */
    CLASS_NAME,

    /** Package/bundle identifier match */
    PACKAGE_NAME,

    /** Resource ID pattern match */
    RESOURCE_ID,

    /** View hierarchy pattern match */
    HIERARCHY_PATTERN,

    /** Child view class match */
    CHILD_CLASS,

    /** Parent view class match */
    PARENT_CLASS,

    /** Rendering surface type */
    SURFACE_TYPE,

    /** Activity name match */
    ACTIVITY_NAME
}

/**
 * Framework detection patterns used for matching
 *
 * Contains the string patterns used to identify each framework.
 * These patterns are platform-agnostic and work across different accessibility APIs.
 */
object FrameworkPatterns {
    // Flutter patterns
    val flutterClassPatterns = listOf(
        "FlutterView",
        "FlutterSurfaceView",
        "FlutterTextureView",
        "io.flutter"
    )
    val flutterResourcePatterns = listOf(
        "flutter_",
        "flutter_semantics_",
        "flutter_id_"
    )

    // React Native patterns
    val reactNativeClassPatterns = listOf(
        "ReactRootView",
        "ReactViewGroup",
        "com.facebook.react"
    )
    val reactNativeResourcePatterns = listOf(
        "react_",
        "rn_"
    )

    // Xamarin patterns
    val xamarinClassPatterns = listOf(
        "mono.android",
        "Xamarin"
    )
    val xamarinPackagePatterns = listOf(
        "xamarin"
    )

    // Cordova/Ionic patterns
    val cordovaClassPatterns = listOf(
        "SystemWebView",
        "CordovaWebView"
    )
    val cordovaPackagePatterns = listOf(
        "cordova",
        "ionic"
    )

    // Unity patterns
    val unityClassPatterns = listOf(
        "UnityPlayer",
        "UnityPlayerActivity"
    )
    val unityPackagePatterns = listOf(
        ".unity3d.",
        ".unity.",
        "com.unity3d.",
        "com.unity."
    )

    // Unreal Engine patterns
    val unrealClassPatterns = listOf(
        "UE4",
        "UE5",
        "UnrealEngine",
        "UEActivity",
        "GameActivity"
    )
    val unrealPackagePatterns = listOf(
        "epicgames",
        "unrealengine",
        ".ue4.",
        ".ue5.",
        "com.epicgames.",
        "com.unrealengine."
    )

    // Godot patterns
    val godotClassPatterns = listOf(
        "GodotView",
        "GodotApp",
        "GodotActivity"
    )
    val godotPackagePatterns = listOf(
        ".godot.",
        "org.godotengine.",
        "godot_"
    )

    // Cocos2d-x patterns
    val cocos2dClassPatterns = listOf(
        "Cocos2dxGLSurfaceView",
        "Cocos2dxActivity",
        "Cocos2d"
    )
    val cocos2dPackagePatterns = listOf(
        "cocos"
    )

    // Defold patterns
    val defoldClassPatterns = listOf(
        "DefoldActivity",
        "NativeActivity"  // Combined with package check
    )
    val defoldPackagePatterns = listOf(
        "defold"
    )

    // Compose patterns (Android)
    val composeClassPatterns = listOf(
        "AndroidComposeView",
        "ComposeView",
        "androidx.compose"
    )

    // SwiftUI patterns (iOS)
    val swiftUIClassPatterns = listOf(
        "SwiftUI",
        "_UIHostingView"
    )

    // Surface/rendering patterns (for game engine detection)
    val surfacePatterns = listOf(
        "GLSurfaceView",
        "SurfaceView",
        "TextureView"
    )
}
