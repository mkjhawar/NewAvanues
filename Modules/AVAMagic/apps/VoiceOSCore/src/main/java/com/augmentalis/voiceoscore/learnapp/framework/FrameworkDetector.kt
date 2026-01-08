/**
 * FrameworkDetector.kt - Detects UI framework used by app
 * Path: modules/apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/framework/FrameworkDetector.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-04
 *
 * Detects which UI framework an app is using (Native, React Native, Compose, Flutter, etc.)
 * Tier 1 Enhancement: Framework-aware exploration
 */

package com.augmentalis.voiceoscore.learnapp.framework

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Framework Detector
 *
 * Detects UI framework by analyzing accessibility tree node class names.
 *
 * ## Supported Frameworks
 *
 * - Native Android (View, ViewGroup, Button, etc.)
 * - React Native (ReactTextView, ReactViewGroup, etc.)
 * - Jetpack Compose (AndroidComposeView, LayoutNode, etc.)
 * - Flutter (FlutterView, FlutterSemanticsNode, etc.)
 * - Unity/Unreal (GLSurfaceView, SurfaceView with game engines)
 * - WebView (WebView-based hybrid apps)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val detector = FrameworkDetector()
 * val framework = detector.detectFramework(rootNode)
 *
 * when (framework) {
 *     UIFramework.NATIVE -> // Standard Android Views
 *     UIFramework.REACT_NATIVE -> // React Native with RN bridge
 *     UIFramework.COMPOSE -> // Jetpack Compose
 *     UIFramework.FLUTTER -> // Flutter (semantics enabled)
 *     else -> // Other/Mixed
 * }
 * ```
 *
 * @since 1.1.0 (Tier 1 Enhancement - 2025-12-04)
 */
class FrameworkDetector {

    /**
     * Detect framework from accessibility tree
     *
     * Analyzes node class names to determine primary UI framework.
     *
     * @param rootNode Root accessibility node
     * @return Detected UI framework
     */
    fun detectFramework(rootNode: AccessibilityNodeInfo?): UIFramework {
        if (rootNode == null) {
            return UIFramework.UNKNOWN
        }

        val classNames = mutableSetOf<String>()
        collectClassNames(rootNode, classNames, maxDepth = 10)

        // Count framework-specific class names
        var nativeCount = 0
        var reactNativeCount = 0
        var composeCount = 0
        var flutterCount = 0
        var webViewCount = 0
        var unityCount = 0

        classNames.forEach { className ->
            val lowerClassName = className.lowercase()

            when {
                // React Native detection
                lowerClassName.contains("react") -> reactNativeCount++

                // Compose detection
                lowerClassName.contains("compose") ||
                lowerClassName.contains("layoutnode") -> composeCount++

                // Flutter detection
                lowerClassName.contains("flutter") -> flutterCount++

                // WebView detection
                lowerClassName.contains("webview") -> webViewCount++

                // Unity/Unreal detection
                lowerClassName.contains("unity") ||
                lowerClassName.contains("unreal") ||
                (lowerClassName.contains("glsurfaceview") && className.contains("UnityPlayer")) -> unityCount++

                // Native Android detection
                lowerClassName.startsWith("android.widget.") ||
                lowerClassName.startsWith("android.view.") ||
                lowerClassName.startsWith("androidx.") ||
                lowerClassName.startsWith("com.google.android.material.") -> nativeCount++
            }
        }

        // Determine primary framework (highest count)
        return when {
            flutterCount > 0 -> UIFramework.FLUTTER
            composeCount > 0 -> UIFramework.COMPOSE
            reactNativeCount > 0 -> UIFramework.REACT_NATIVE
            webViewCount > 5 -> UIFramework.WEBVIEW // Threshold for hybrid apps
            unityCount > 0 -> UIFramework.UNITY
            nativeCount > 0 -> UIFramework.NATIVE
            else -> UIFramework.UNKNOWN
        }
    }

    /**
     * Detect mixed frameworks
     *
     * Many apps use multiple frameworks (e.g., Native + Compose, Native + WebView).
     *
     * @param rootNode Root accessibility node
     * @return List of detected frameworks
     */
    fun detectMixedFrameworks(rootNode: AccessibilityNodeInfo?): List<UIFramework> {
        if (rootNode == null) {
            return emptyList()
        }

        val classNames = mutableSetOf<String>()
        collectClassNames(rootNode, classNames, maxDepth = 10)

        val frameworks = mutableSetOf<UIFramework>()

        classNames.forEach { className ->
            val lowerClassName = className.lowercase()

            when {
                lowerClassName.contains("react") -> frameworks.add(UIFramework.REACT_NATIVE)
                lowerClassName.contains("compose") -> frameworks.add(UIFramework.COMPOSE)
                lowerClassName.contains("flutter") -> frameworks.add(UIFramework.FLUTTER)
                lowerClassName.contains("webview") -> frameworks.add(UIFramework.WEBVIEW)
                lowerClassName.contains("unity") || lowerClassName.contains("unreal") -> frameworks.add(UIFramework.UNITY)
                lowerClassName.startsWith("android.") || lowerClassName.startsWith("androidx.") -> frameworks.add(UIFramework.NATIVE)
            }
        }

        return frameworks.toList()
    }

    /**
     * Check if framework has accessibility support
     *
     * Tier 1 focuses on frameworks that expose accessibility information.
     *
     * @param framework UI framework
     * @return true if framework exposes accessibility tree
     */
    fun hasAccessibilitySupport(framework: UIFramework): Boolean {
        return when (framework) {
            UIFramework.NATIVE -> true          // Always supported
            UIFramework.REACT_NATIVE -> true    // Supported if RN accessibility enabled
            UIFramework.COMPOSE -> true         // Supported if semantics added
            UIFramework.FLUTTER -> true         // Supported if semantics enabled
            UIFramework.WEBVIEW -> true         // Supported (WebView exposes DOM)
            UIFramework.UNITY -> false          // No accessibility (requires Vision AI)
            UIFramework.UNREAL -> false         // No accessibility (requires Vision AI)
            UIFramework.UNKNOWN -> false
        }
    }

    /**
     * Collect class names from node tree
     *
     * Traverses tree and collects unique class names for analysis.
     *
     * @param node Current node
     * @param classNames Set to collect class names
     * @param maxDepth Maximum depth to traverse
     * @param currentDepth Current depth
     */
    private fun collectClassNames(
        node: AccessibilityNodeInfo,
        classNames: MutableSet<String>,
        maxDepth: Int,
        currentDepth: Int = 0
    ) {
        if (currentDepth >= maxDepth) {
            return
        }

        // Add class name
        node.className?.toString()?.let { classNames.add(it) }

        // Traverse children (limit to 20 per container)
        val maxChildren = minOf(node.childCount, 20)
        for (i in 0 until maxChildren) {
            node.getChild(i)?.let { child ->
                try {
                    collectClassNames(child, classNames, maxDepth, currentDepth + 1)
                } finally {
                    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        @Suppress("DEPRECATION")
                        child.recycle()
                    }
                }
            }
        }
    }
}

/**
 * UI Framework Enum
 *
 * Represents different UI frameworks apps can use.
 *
 * @since 1.1.0 (Tier 1 Enhancement - 2025-12-04)
 */
enum class UIFramework {
    /** Native Android Views (android.widget.*, android.view.*) */
    NATIVE,

    /** React Native (Facebook) */
    REACT_NATIVE,

    /** Jetpack Compose (Google) */
    COMPOSE,

    /** Flutter (Google) */
    FLUTTER,

    /** WebView-based hybrid apps */
    WEBVIEW,

    /** Unity game engine (3D games) */
    UNITY,

    /** Unreal Engine (3D games) */
    UNREAL,

    /** Unknown or multiple frameworks */
    UNKNOWN
}

/**
 * Framework Detection Result
 *
 * Contains framework detection details.
 *
 * @property primaryFramework Main UI framework detected
 * @property mixedFrameworks All frameworks detected
 * @property hasAccessibility Whether framework exposes accessibility tree
 * @property tierSupport Which tier is needed (Tier 1, 2, or 3)
 *
 * @since 1.1.0 (Tier 1 Enhancement - 2025-12-04)
 */
data class FrameworkDetectionResult(
    val primaryFramework: UIFramework,
    val mixedFrameworks: List<UIFramework>,
    val hasAccessibility: Boolean,
    val tierSupport: Int  // 1 = Tier 1 (accessibility), 2 = Tier 2 (Vision AI), 3 = Tier 3 (Game AI)
) {
    override fun toString(): String {
        return """
            Framework Detection:
            - Primary: $primaryFramework
            - Mixed: ${mixedFrameworks.joinToString(", ")}
            - Accessibility: $hasAccessibility
            - Tier: $tierSupport
        """.trimIndent()
    }
}
