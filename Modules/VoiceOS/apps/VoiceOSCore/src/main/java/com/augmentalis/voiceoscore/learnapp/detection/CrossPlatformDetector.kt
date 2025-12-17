/**
 * CrossPlatformDetector.kt - Cross-platform app framework detector
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Detects the framework used to build an app for framework-specific handling.
 */

package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import java.util.concurrent.ConcurrentHashMap

/**
 * Cross-Platform Detector
 *
 * Detects the framework used to build an application.
 * Uses multiple heuristics including package inspection, class names, and view hierarchy.
 *
 * ## Detection Methods:
 * 1. Package manifest analysis (native libraries, activities)
 * 2. View hierarchy class name inspection
 * 3. Accessibility node structure patterns
 *
 * ## Caching:
 * Detection results are cached per package name to avoid repeated detection.
 */
object CrossPlatformDetector {
    private const val TAG = "CrossPlatformDetector"

    // Cache for detected frameworks (packageName -> AppFramework)
    private val frameworkCache = ConcurrentHashMap<String, AppFramework>()

    // Flutter detection patterns
    private val FLUTTER_PATTERNS = listOf(
        "io.flutter",
        "FlutterView",
        "FlutterSemanticsNode",
        "SemanticsNode"
    )

    // React Native detection patterns
    private val REACT_NATIVE_PATTERNS = listOf(
        "com.facebook.react",
        "ReactRootView",
        "ReactViewGroup",
        "RCTView"
    )

    // Unity detection patterns
    private val UNITY_PATTERNS = listOf(
        "com.unity3d",
        "UnityPlayer",
        "UnityPlayerActivity"
    )

    // Unreal detection patterns
    private val UNREAL_PATTERNS = listOf(
        "com.epicgames",
        "UnrealEngine",
        "GameActivity"
    )

    // WebView detection patterns
    private val WEBVIEW_PATTERNS = listOf(
        "WebView",
        "WebViewClient",
        "CordovaWebView",
        "IonicWebView"
    )

    // Xamarin detection patterns
    private val XAMARIN_PATTERNS = listOf(
        "mono.",
        "xamarin.",
        "Microsoft.Maui"
    )

    /**
     * Detect the framework for a given package.
     *
     * @param context Application context for package inspection
     * @param packageName Package name to analyze
     * @param rootNode Optional root accessibility node for view hierarchy analysis
     * @return Detected AppFramework
     */
    fun detectFramework(
        context: Context,
        packageName: String,
        rootNode: AccessibilityNodeInfo? = null
    ): AppFramework {
        // Check cache first
        frameworkCache[packageName]?.let { cached ->
            return cached
        }

        val detected = detectFrameworkInternal(context, packageName, rootNode)
        frameworkCache[packageName] = detected

        Log.d(TAG, "Detected framework for $packageName: $detected")
        return detected
    }

    /**
     * Internal detection logic.
     */
    private fun detectFrameworkInternal(
        context: Context,
        packageName: String,
        rootNode: AccessibilityNodeInfo?
    ): AppFramework {
        // Try package manifest analysis first
        try {
            val packageInfo = context.packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES or PackageManager.GET_META_DATA
            )

            // Check activities for framework patterns
            packageInfo.activities?.forEach { activity ->
                val activityName = activity.name ?: return@forEach

                when {
                    FLUTTER_PATTERNS.any { activityName.contains(it, ignoreCase = true) } ->
                        return AppFramework.FLUTTER
                    REACT_NATIVE_PATTERNS.any { activityName.contains(it, ignoreCase = true) } ->
                        return AppFramework.REACT_NATIVE
                    UNITY_PATTERNS.any { activityName.contains(it, ignoreCase = true) } ->
                        return AppFramework.UNITY
                    UNREAL_PATTERNS.any { activityName.contains(it, ignoreCase = true) } ->
                        return AppFramework.UNREAL
                    XAMARIN_PATTERNS.any { activityName.contains(it, ignoreCase = true) } ->
                        return AppFramework.XAMARIN
                }
            }

            // Check meta-data for framework hints
            packageInfo.applicationInfo?.metaData?.let { metaData ->
                if (metaData.containsKey("io.flutter.embedding.engine.FlutterEngine")) {
                    return AppFramework.FLUTTER
                }
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Package not found: $packageName")
        }

        // Try view hierarchy analysis if root node available
        rootNode?.let { node ->
            val detected = analyzeViewHierarchy(node)
            if (detected != AppFramework.UNKNOWN) {
                return detected
            }
        }

        // Default to native if no framework detected
        return AppFramework.NATIVE
    }

    /**
     * Analyze view hierarchy for framework patterns.
     */
    private fun analyzeViewHierarchy(node: AccessibilityNodeInfo): AppFramework {
        val className = node.className?.toString() ?: ""

        // Check current node
        when {
            FLUTTER_PATTERNS.any { className.contains(it, ignoreCase = true) } ->
                return AppFramework.FLUTTER
            REACT_NATIVE_PATTERNS.any { className.contains(it, ignoreCase = true) } ->
                return AppFramework.REACT_NATIVE
            UNITY_PATTERNS.any { className.contains(it, ignoreCase = true) } ->
                return AppFramework.UNITY
            UNREAL_PATTERNS.any { className.contains(it, ignoreCase = true) } ->
                return AppFramework.UNREAL
            WEBVIEW_PATTERNS.any { className.contains(it, ignoreCase = true) } ->
                return AppFramework.WEB_BASED
            XAMARIN_PATTERNS.any { className.contains(it, ignoreCase = true) } ->
                return AppFramework.XAMARIN
        }

        // Check children (limited depth to avoid performance issues)
        for (i in 0 until minOf(node.childCount, 10)) {
            val child = node.getChild(i) ?: continue
            val childClassName = child.className?.toString() ?: ""

            when {
                FLUTTER_PATTERNS.any { childClassName.contains(it, ignoreCase = true) } -> {
                    child.recycle()
                    return AppFramework.FLUTTER
                }
                REACT_NATIVE_PATTERNS.any { childClassName.contains(it, ignoreCase = true) } -> {
                    child.recycle()
                    return AppFramework.REACT_NATIVE
                }
                WEBVIEW_PATTERNS.any { childClassName.contains(it, ignoreCase = true) } -> {
                    child.recycle()
                    return AppFramework.WEB_BASED
                }
            }
            child.recycle()
        }

        return AppFramework.UNKNOWN
    }

    /**
     * Check if package is a cross-platform app.
     */
    fun isCrossPlatform(context: Context, packageName: String): Boolean {
        val framework = detectFramework(context, packageName)
        return framework != AppFramework.NATIVE && framework != AppFramework.UNKNOWN
    }

    /**
     * Check if package is a game engine app.
     */
    fun isGameEngine(context: Context, packageName: String): Boolean {
        val framework = detectFramework(context, packageName)
        return framework == AppFramework.UNITY || framework == AppFramework.UNREAL
    }

    /**
     * Clear framework cache for a specific package.
     */
    fun invalidateCache(packageName: String) {
        frameworkCache.remove(packageName)
    }

    /**
     * Clear entire framework cache.
     */
    fun clearCache() {
        frameworkCache.clear()
    }
}
