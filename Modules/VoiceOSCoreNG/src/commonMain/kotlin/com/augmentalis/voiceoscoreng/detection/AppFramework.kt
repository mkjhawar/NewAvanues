/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * AppFramework.kt - Cross-platform app framework enumeration
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: LearnAppCore CrossPlatformDetector.kt
 *
 * Represents different cross-platform frameworks and native platforms.
 * Used to enable framework-specific optimizations for voice command processing.
 */

package com.augmentalis.voiceoscoreng.detection

/**
 * App Framework Types
 *
 * Represents different cross-platform frameworks and native platforms.
 * Used to enable framework-specific optimizations for voice interaction.
 *
 * ## Supported Frameworks
 *
 * - **Native**: Standard platform SDK (Android, iOS, Desktop)
 * - **Flutter**: Google's UI toolkit (FlutterView, io.flutter packages)
 * - **React Native**: Meta's React framework (ReactRootView)
 * - **Xamarin**: Microsoft's .NET framework (mono.android packages)
 * - **Cordova**: Apache's WebView-based framework
 * - **Unity**: Unity game engine (UnityPlayer)
 * - **Unreal**: Unreal Engine (UE4/UE5, GameActivity)
 * - **Godot**: Godot Engine (GodotView, org.godotengine packages)
 * - **Cocos2D**: Cocos2d-x Engine (Cocos2dxGLSurfaceView)
 * - **Defold**: Defold Engine (DefoldActivity, NativeActivity)
 * - **Compose**: Jetpack Compose (AndroidComposeView)
 * - **SwiftUI**: Apple's declarative UI framework
 *
 * @since 1.0.0 (Cross-Platform Support)
 */
enum class AppFramework {
    /**
     * Native platform SDK (Android, iOS, Desktop)
     *
     * Usually has good semantic labels (text, contentDescription, resourceId)
     */
    NATIVE,

    /**
     * Flutter framework (Google)
     *
     * Often lacks semantic labels unless Semantics widget is used.
     * Requires aggressive fallback label generation.
     */
    FLUTTER,

    /**
     * React Native framework (Meta)
     *
     * Mixed label quality - some elements labeled, many unlabeled.
     * Requires moderate fallback label generation.
     */
    REACT_NATIVE,

    /**
     * Xamarin framework (Microsoft)
     *
     * Usually has decent semantic labels.
     * May need minor fallback label generation.
     */
    XAMARIN,

    /**
     * Cordova/Ionic framework (Apache)
     *
     * WebView-based, often lacks semantic labels.
     * Requires aggressive fallback label generation.
     */
    CORDOVA,

    /**
     * Unity game engine
     *
     * Renders to OpenGL/Vulkan surface with NO accessibility tree.
     * Most UI elements are drawn directly and invisible to accessibility.
     * Requires spatial coordinate-based labeling and interaction.
     */
    UNITY,

    /**
     * Unreal Engine (Epic Games)
     *
     * Similar to Unity - renders UI via Slate framework to graphics surface.
     * Minimal accessibility support, elements not exposed to platform framework.
     * Uses 4x4 spatial grid (finer than Unity) due to more complex UI.
     * Common in AAA mobile games (PUBG Mobile, Fortnite, Dead by Daylight Mobile).
     */
    UNREAL,

    /**
     * Godot Engine
     *
     * Open-source game engine similar to Unity.
     * Renders UI to graphics surface with minimal accessibility support.
     * Uses 3x3 spatial grid (same as Unity) for coordinate-based interaction.
     */
    GODOT,

    /**
     * Cocos2d-x Engine
     *
     * Popular 2D game engine, especially for mobile games.
     * Renders to OpenGL surface with minimal accessibility support.
     * Uses 3x3 spatial grid for spatial labeling.
     */
    COCOS2D,

    /**
     * Defold Engine
     *
     * Lightweight game engine optimized for mobile and web.
     * Uses NativeActivity with direct graphics rendering.
     * Uses 3x3 spatial grid for coordinate-based interaction.
     */
    DEFOLD,

    /**
     * Jetpack Compose (Android)
     *
     * Android's modern declarative UI toolkit.
     * Generally has good accessibility support with semantics.
     */
    COMPOSE,

    /**
     * SwiftUI (iOS/macOS)
     *
     * Apple's declarative UI framework.
     * Generally has good accessibility support with accessibility modifiers.
     */
    SWIFTUI,

    /**
     * Unknown framework
     *
     * Framework could not be determined.
     * Fallback to default behavior.
     */
    UNKNOWN;

    /**
     * Check if framework needs aggressive fallback label generation
     *
     * @return true if framework typically lacks semantic labels
     */
    fun needsAggressiveFallback(): Boolean {
        return this == FLUTTER || this == CORDOVA || this == UNITY || this == UNREAL ||
                this == GODOT || this == COCOS2D || this == DEFOLD
    }

    /**
     * Check if framework needs moderate fallback label generation
     *
     * @return true if framework sometimes lacks semantic labels
     */
    fun needsModerateFallback(): Boolean {
        return this == REACT_NATIVE || this == XAMARIN
    }

    /**
     * Get minimum label length threshold for this framework
     *
     * Cross-platform apps may have very short auto-generated labels,
     * so we use lower thresholds.
     *
     * @param defaultLength Default minimum length
     * @return Adjusted minimum length
     */
    fun getMinLabelLength(defaultLength: Int): Int {
        return when (this) {
            FLUTTER, CORDOVA, UNITY, UNREAL, GODOT, COCOS2D, DEFOLD -> 1  // Accept even single-character labels
            REACT_NATIVE, XAMARIN -> maxOf(1, defaultLength - 1)  // Slightly lower threshold
            else -> defaultLength  // Use standard threshold
        }
    }

    /**
     * Check if framework needs spatial coordinate-based interaction
     *
     * Game engines render to OpenGL/Vulkan surface with no accessible elements,
     * requiring coordinate-based tapping instead of accessibility actions.
     *
     * @return true if framework requires coordinate tapping
     */
    fun needsCoordinateTapping(): Boolean {
        return this == UNITY || this == UNREAL || this == GODOT ||
                this == COCOS2D || this == DEFOLD
    }

    /**
     * Check if framework may support stable identifiers (Flutter 3.19+)
     *
     * Only Flutter apps can have stable identifiers from SemanticsProperties.identifier.
     * Use FrameworkDetector.detectFlutterVersion() for actual version detection.
     *
     * @return true if framework is Flutter (may support stable IDs)
     */
    fun mayHaveStableIdentifiers(): Boolean {
        return this == FLUTTER
    }

    /**
     * Check if framework is a game engine
     *
     * Game engines typically have minimal or no accessibility support
     * and require special handling for voice commands.
     *
     * @return true if framework is a game engine
     */
    fun isGameEngine(): Boolean {
        return this == UNITY || this == UNREAL || this == GODOT ||
                this == COCOS2D || this == DEFOLD
    }

    /**
     * Check if framework is a cross-platform UI framework
     *
     * Cross-platform UI frameworks typically have varying levels
     * of accessibility support depending on implementation.
     *
     * @return true if framework is a cross-platform UI framework
     */
    fun isCrossPlatformUI(): Boolean {
        return this == FLUTTER || this == REACT_NATIVE || this == XAMARIN || this == CORDOVA
    }

    /**
     * Check if framework is a native declarative UI framework
     *
     * Native declarative frameworks generally have good accessibility support.
     *
     * @return true if framework is a native declarative UI framework
     */
    fun isNativeDeclarative(): Boolean {
        return this == COMPOSE || this == SWIFTUI
    }

    /**
     * Get recommended spatial grid size for coordinate-based interaction
     *
     * Different game engines may need different grid resolutions
     * based on typical UI complexity.
     *
     * @return Grid size (e.g., 3 for 3x3, 4 for 4x4)
     */
    fun getRecommendedGridSize(): Int {
        return when (this) {
            UNREAL -> 4  // 4x4 grid for complex Unreal UIs
            UNITY, GODOT, COCOS2D, DEFOLD -> 3  // 3x3 grid for typical game UIs
            else -> 0  // Not applicable for non-game frameworks
        }
    }

    companion object {
        /**
         * Get all game engine frameworks
         */
        val gameEngines: Set<AppFramework> = setOf(UNITY, UNREAL, GODOT, COCOS2D, DEFOLD)

        /**
         * Get all cross-platform UI frameworks
         */
        val crossPlatformUI: Set<AppFramework> = setOf(FLUTTER, REACT_NATIVE, XAMARIN, CORDOVA)

        /**
         * Get all frameworks that need fallback label generation
         */
        val needsFallback: Set<AppFramework> = setOf(
            FLUTTER, REACT_NATIVE, XAMARIN, CORDOVA,
            UNITY, UNREAL, GODOT, COCOS2D, DEFOLD
        )
    }
}

/**
 * Flutter version detection result
 *
 * Flutter 3.19+ (Feb 2024) introduced SemanticsProperties.identifier
 * which provides stable element identification via resource-id.
 */
enum class FlutterVersion {
    /** Not a Flutter app */
    NOT_FLUTTER,

    /** Flutter < 3.19 (no stable identifier support) */
    FLUTTER_LEGACY,

    /** Flutter 3.19+ (has stable identifier support) */
    FLUTTER_319_PLUS
}
