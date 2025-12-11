/**
 * CrossPlatformDetector.kt - Detects cross-platform app frameworks
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-08
 * Moved to LearnAppCore: 2025-12-11
 * Related: Cross-Platform Fallback Label Generation (Phase 3)
 *
 * Detects if app is built with cross-platform framework (Flutter, React Native, etc.)
 * to enable enhanced fallback label generation for unlabeled elements.
 *
 * ## Supported Frameworks
 *
 * - **Flutter**: Google's UI toolkit (FlutterView, io.flutter packages)
 * - **React Native**: Facebook's React framework (ReactRootView)
 * - **Xamarin**: Microsoft's .NET framework (mono.android packages)
 * - **Native**: Standard Android SDK
 *
 * ## Detection Strategy
 *
 * Framework detection uses multiple signals:
 * 1. View hierarchy class names (FlutterView, ReactRootView)
 * 2. Package names (io.flutter, com.facebook.react)
 * 3. Resource ID patterns (flutter_, react_native_)
 *
 * @since 1.2.0 (Cross-Platform Support)
 */

package com.augmentalis.learnappcore.detection

import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Cross-Platform Framework Detector
 *
 * Analyzes app view hierarchy to detect underlying framework.
 * Used to enable framework-specific optimizations for label generation.
 */
object CrossPlatformDetector {
    private const val TAG = "CrossPlatformDetector"

    /**
     * Detect if app is built with cross-platform framework
     *
     * Analyzes root node and package name to identify framework.
     * Detection is cached per package for performance.
     *
     * @param packageName App package name
     * @param rootNode Root accessibility node (may be null)
     * @return Detected framework type
     */
    fun detectFramework(packageName: String, rootNode: AccessibilityNodeInfo?): AppFramework {
        if (rootNode == null) {
            Log.w(TAG, "Root node is null, assuming NATIVE framework")
            return AppFramework.NATIVE
        }

        try {
            // Check for Unity (HIGHEST PRIORITY - Unity games often lack ALL semantic labels)
            if (hasUnitySignatures(rootNode, packageName)) {
                Log.i(TAG, "Detected UNITY framework for $packageName")
                return AppFramework.UNITY
            }

            // Check for Unreal Engine (HIGH PRIORITY - similar to Unity)
            if (hasUnrealSignatures(rootNode, packageName)) {
                Log.i(TAG, "Detected UNREAL framework for $packageName")
                return AppFramework.UNREAL
            }

            // Check for Flutter
            if (hasFlutterSignatures(rootNode, packageName)) {
                Log.i(TAG, "Detected FLUTTER framework for $packageName")
                return AppFramework.FLUTTER
            }

            // Check for React Native
            if (hasReactNativeSignatures(rootNode, packageName)) {
                Log.i(TAG, "Detected REACT_NATIVE framework for $packageName")
                return AppFramework.REACT_NATIVE
            }

            // Check for Xamarin
            if (hasXamarinSignatures(rootNode, packageName)) {
                Log.i(TAG, "Detected XAMARIN framework for $packageName")
                return AppFramework.XAMARIN
            }

            // Check for Cordova/Ionic
            if (hasCordovaSignatures(rootNode, packageName)) {
                Log.i(TAG, "Detected CORDOVA framework for $packageName")
                return AppFramework.CORDOVA
            }

            Log.d(TAG, "No cross-platform framework detected for $packageName, assuming NATIVE")
            return AppFramework.NATIVE

        } catch (e: Exception) {
            Log.e(TAG, "Error detecting framework for $packageName", e)
            return AppFramework.NATIVE
        }
    }

    /**
     * Check if app uses Flutter framework
     *
     * Detection signals:
     * - FlutterView in view hierarchy
     * - io.flutter package classes
     * - flutter_ resource IDs
     *
     * @param node Root or current accessibility node
     * @param packageName App package name
     * @return true if Flutter detected
     */
    private fun hasFlutterSignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
        // Check for FlutterView class
        if (node.className?.contains("FlutterView") == true) {
            return true
        }

        // Check for io.flutter package
        if (node.className?.contains("io.flutter") == true) {
            return true
        }

        // Check resource IDs for flutter patterns
        if (node.viewIdResourceName?.contains("flutter") == true) {
            return true
        }

        // Recursively check children (up to depth 3)
        return checkChildrenForSignature(node, depth = 0, maxDepth = 3) { childNode ->
            childNode.className?.contains("FlutterView") == true ||
                    childNode.className?.contains("io.flutter") == true
        }
    }

    /**
     * Check if app uses React Native framework
     *
     * Detection signals:
     * - ReactRootView in view hierarchy
     * - com.facebook.react package classes
     * - react resource IDs
     *
     * @param node Root or current accessibility node
     * @param packageName App package name
     * @return true if React Native detected
     */
    private fun hasReactNativeSignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
        // Check for ReactRootView class
        if (node.className?.contains("ReactRootView") == true) {
            return true
        }

        // Check for com.facebook.react package
        if (node.className?.contains("com.facebook.react") == true) {
            return true
        }

        // Check resource IDs for react patterns
        if (node.viewIdResourceName?.contains("react") == true) {
            return true
        }

        // Recursively check children (up to depth 3)
        return checkChildrenForSignature(node, depth = 0, maxDepth = 3) { childNode ->
            childNode.className?.contains("ReactRootView") == true ||
                    childNode.className?.contains("com.facebook.react") == true
        }
    }

    /**
     * Check if app uses Xamarin framework
     *
     * Detection signals:
     * - mono.android package classes
     * - Xamarin in package name
     *
     * @param node Root or current accessibility node
     * @param packageName App package name
     * @return true if Xamarin detected
     */
    private fun hasXamarinSignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
        // Check for mono.android package
        if (node.className?.contains("mono.android") == true) {
            return true
        }

        // Check package name for Xamarin
        if (packageName.contains("xamarin", ignoreCase = true)) {
            return true
        }

        return false
    }

    /**
     * Check if app uses Cordova/Ionic framework
     *
     * Detection signals:
     * - WebView-based with cordova in package
     * - SystemWebView or WebViewEngine classes
     *
     * @param node Root or current accessibility node
     * @param packageName App package name
     * @return true if Cordova detected
     */
    private fun hasCordovaSignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
        // Check for WebView classes (Cordova is WebView-based)
        val className = node.className?.toString() ?: ""
        if (className.contains("WebView")) {
            // Check if package name suggests Cordova/Ionic
            if (packageName.contains("cordova", ignoreCase = true) ||
                packageName.contains("ionic", ignoreCase = true)) {
                return true
            }
        }

        return false
    }

    /**
     * Check if app uses Unity game engine
     *
     * Unity apps are particularly challenging because:
     * 1. Most UI elements are drawn directly to OpenGL/Vulkan surface
     * 2. Accessibility framework sees single "UnityPlayer" view
     * 3. Individual buttons/menus are NOT exposed to accessibility
     * 4. No contentDescription, text, or resourceId
     *
     * Detection signals:
     * - className contains "UnityPlayer"
     * - packageName patterns (com.*.unity, *.unity3d.*)
     * - View hierarchy has single child with "Player" in class name
     * - GLSurfaceView or SurfaceView with Unity parent
     *
     * @param node Root or current accessibility node
     * @param packageName App package name
     * @return true if Unity detected
     */
    private fun hasUnitySignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
        // Signature 1: UnityPlayer view
        if (node.className?.contains("UnityPlayer") == true) {
            return true
        }

        // Signature 2: Package name patterns
        val unityPackagePatterns = listOf(
            ".unity3d.",
            ".unity.",
            "com.unity3d.",
            "com.unity."
        )
        if (unityPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
            return true
        }

        // Signature 3: Shallow view hierarchy (Unity renders to single surface)
        val childCount = node.childCount
        if (childCount <= 1 && node.className?.contains("Player", ignoreCase = true) == true) {
            return true
        }

        // Signature 4: OpenGL/Vulkan rendering surface
        if (node.className?.contains("GLSurfaceView") == true ||
            node.className?.contains("SurfaceView") == true) {
            // Check if parent/root is Unity
            var parent = node.parent
            while (parent != null) {
                if (parent.className?.contains("Unity", ignoreCase = true) == true) {
                    return true
                }
                parent = parent.parent
            }
        }

        // Signature 5: Check children recursively for Unity signatures
        return checkChildrenForSignature(node, depth = 0, maxDepth = 3) { childNode ->
            childNode.className?.contains("UnityPlayer") == true ||
                    childNode.className?.contains("Unity", ignoreCase = true) == true
        }
    }

    /**
     * Check if app uses Unreal Engine
     *
     * Unreal Engine apps are similar to Unity:
     * 1. UI rendered directly to graphics surface (OpenGL/Vulkan)
     * 2. Accessibility sees minimal view hierarchy
     * 3. Individual UI elements not exposed to accessibility
     * 4. Uses Slate UI framework (not Android native views)
     *
     * Detection signatures:
     * - className contains "UE4", "UE5", "UnrealEngine", or "GameActivity"
     * - packageName contains "epicgames", "unrealengine", ".ue4.", ".ue5."
     * - View hierarchy has GameActivity with single rendering surface
     * - Characteristic shallow hierarchy (Activity → FrameLayout → SurfaceView)
     *
     * @param node Root or current accessibility node
     * @param packageName App package name
     * @return true if Unreal Engine detected
     */
    private fun hasUnrealSignatures(node: AccessibilityNodeInfo, packageName: String): Boolean {
        // Signature 1: UE4/UE5/Unreal class names
        val unrealClassPatterns = listOf(
            "UE4",
            "UE5",
            "UnrealEngine",
            "UEActivity",
            "GameActivity"  // Common Unreal base activity
        )

        val className = node.className?.toString() ?: ""
        if (unrealClassPatterns.any { className.contains(it, ignoreCase = true) }) {
            return true
        }

        // Signature 2: Package name patterns
        val unrealPackagePatterns = listOf(
            "epicgames",
            "unrealengine",
            ".ue4.",
            ".ue5.",
            "com.epicgames.",
            "com.unrealengine."
        )
        if (unrealPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
            return true
        }

        // Signature 3: Characteristic Unreal view hierarchy
        // Unreal typically has: Activity → FrameLayout → SurfaceView
        if (hasUnrealHierarchyPattern(node)) {
            return true
        }

        // Signature 4: SurfaceView with specific characteristics
        if (node.className?.contains("SurfaceView") == true) {
            // Check parent chain for Unreal indicators
            var parent = node.parent
            var depth = 0
            while (parent != null && depth < 5) {
                val parentClass = parent.className?.toString() ?: ""
                if (parentClass.contains("UE", ignoreCase = false) ||  // UE4, UE5
                        parentClass.contains("Game", ignoreCase = true) ||
                        parentClass.contains("Unreal", ignoreCase = true)) {
                    return true
                }
                parent = parent.parent
                depth++
            }
        }

        // Signature 5: Check children recursively for Unreal signatures
        return checkChildrenForSignature(node, depth = 0, maxDepth = 3) { childNode ->
            val childClassName = childNode.className?.toString() ?: ""
            unrealClassPatterns.any { childClassName.contains(it, ignoreCase = true) }
        }
    }

    /**
     * Check for characteristic Unreal view hierarchy pattern
     *
     * Unreal apps typically have very shallow hierarchy:
     * - Activity root with 1-3 children
     * - Single SurfaceView for rendering
     * - Minimal Android UI framework usage
     *
     * @param node Root node to check
     * @return true if matches Unreal hierarchy pattern
     */
    private fun hasUnrealHierarchyPattern(node: AccessibilityNodeInfo): Boolean {
        // Root should have very few children (Unreal uses minimal Android UI)
        if (node.childCount > 3) return false

        // Should contain SurfaceView (rendering surface)
        var hasSurface = false
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    if (child.className?.contains("SurfaceView") == true) {
                        hasSurface = true
                    }
                } finally {
                    // Recycle child node
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking Unreal hierarchy pattern", e)
        }

        // Unreal pattern: very few children + SurfaceView present
        return hasSurface && node.childCount <= 3
    }

    /**
     * Recursively check children for framework signatures
     *
     * Limits depth to avoid performance impact.
     *
     * @param node Parent node to check
     * @param depth Current recursion depth
     * @param maxDepth Maximum depth to search
     * @param predicate Function to check if child matches signature
     * @return true if any child matches signature
     */
    private fun checkChildrenForSignature(
        node: AccessibilityNodeInfo,
        depth: Int,
        maxDepth: Int,
        predicate: (AccessibilityNodeInfo) -> Boolean
    ): Boolean {
        if (depth >= maxDepth) return false

        try {
            val childCount = node.childCount
            for (i in 0 until childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    // Check if child matches
                    if (predicate(child)) {
                        return true
                    }

                    // Recursively check child's children
                    if (checkChildrenForSignature(child, depth + 1, maxDepth, predicate)) {
                        return true
                    }
                } finally {
                    // Recycle child node
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking children for signatures", e)
        }

        return false
    }
}

/**
 * App Framework Types
 *
 * Represents different cross-platform frameworks and native Android.
 * Used to enable framework-specific optimizations.
 */
enum class AppFramework {
    /**
     * Native Android (standard Android SDK)
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
     * React Native framework (Facebook)
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
     * Minimal accessibility support, elements not exposed to Android framework.
     * Uses 4x4 spatial grid (finer than Unity) due to more complex UI.
     * Common in AAA mobile games (PUBG Mobile, Fortnite, Dead by Daylight Mobile).
     */
    UNREAL;

    /**
     * Check if framework needs aggressive fallback label generation
     *
     * @return true if framework typically lacks semantic labels
     */
    fun needsAggressiveFallback(): Boolean {
        return this == FLUTTER || this == CORDOVA || this == UNITY || this == UNREAL
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
            FLUTTER, CORDOVA, UNITY, UNREAL -> 1  // Accept even single-character labels
            REACT_NATIVE, XAMARIN -> maxOf(1, defaultLength - 1)  // Slightly lower threshold
            NATIVE -> defaultLength  // Use standard threshold
        }
    }

    /**
     * Check if framework needs spatial coordinate-based interaction
     *
     * Unity renders to OpenGL surface with no accessible elements,
     * requiring coordinate-based tapping instead of accessibility actions.
     *
     * @return true if framework requires coordinate tapping
     */
    fun needsCoordinateTapping(): Boolean {
        return this == UNITY
    }
}
