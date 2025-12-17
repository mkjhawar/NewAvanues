/**
 * StateMetadata.kt - Enhanced state detection metadata
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-10-13
 *
 * Provides rich metadata for state detections including UI framework detection,
 * element counts, hierarchy depth, duration tracking, and contextual information.
 * Enhances StateDetectionResult with comprehensive environmental context.
 */
package com.augmentalis.learnapp.state.advanced

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.learnapp.state.AppState

/**
 * UI Framework detection
 */
enum class UIFramework {
    NATIVE_ANDROID,       // Native Android Views
    JETPACK_COMPOSE,      // Jetpack Compose
    REACT_NATIVE,         // React Native
    FLUTTER,              // Flutter
    WEBVIEW,              // WebView-based
    UNITY,                // Unity game engine
    MIXED,                // Multiple frameworks
    UNKNOWN
}

/**
 * Material Design version detection
 */
enum class MaterialVersion {
    MATERIAL_2,           // Material Design 2
    MATERIAL_3,           // Material Design 3 (Material You)
    NONE,                 // Not using Material Design
    UNKNOWN
}

/**
 * Enhanced state metadata
 */
data class StateMetadata(
    // UI Framework information
    val uiFramework: UIFramework,
    val materialVersion: MaterialVersion,

    // Element counts
    val totalElements: Int,
    val interactiveElements: Int,
    val textElements: Int,
    val imageElements: Int,

    // Hierarchy information
    val hierarchyDepth: Int,
    val maxBranchingFactor: Int,

    // Duration tracking
    val detectionTimestamp: Long,
    val stateDuration: Long = 0L,

    // Contextual information
    val hasDialog: Boolean,
    val hasScrollableContent: Boolean,
    val hasInputFields: Boolean,
    val hasProgressIndicators: Boolean,

    // Material Design components
    val materialComponents: List<MaterialComponent>,

    // Screen orientation
    val isLandscape: Boolean = false,

    // Additional metadata
    val packageName: String = "",
    val activityName: String = ""
) {
    /**
     * Get complexity score based on element counts and hierarchy
     */
    fun getComplexityScore(): Float {
        val elementScore = (totalElements / 50f).coerceAtMost(1f) * 0.4f
        val hierarchyScore = (hierarchyDepth / 20f).coerceAtMost(1f) * 0.3f
        val interactivityScore = (interactiveElements / 20f).coerceAtMost(1f) * 0.3f

        return (elementScore + hierarchyScore + interactivityScore).coerceIn(0f, 1f)
    }

    /**
     * Check if UI appears to be stable
     */
    fun isUIStable(): Boolean {
        return stateDuration > 500L && totalElements > 0
    }

    /**
     * Get framework description
     */
    fun getFrameworkDescription(): String {
        val framework = when (uiFramework) {
            UIFramework.NATIVE_ANDROID -> "Native Android"
            UIFramework.JETPACK_COMPOSE -> "Jetpack Compose"
            UIFramework.REACT_NATIVE -> "React Native"
            UIFramework.FLUTTER -> "Flutter"
            UIFramework.WEBVIEW -> "WebView"
            UIFramework.UNITY -> "Unity"
            UIFramework.MIXED -> "Mixed"
            UIFramework.UNKNOWN -> "Unknown"
        }

        val material = when (materialVersion) {
            MaterialVersion.MATERIAL_2 -> " (Material 2)"
            MaterialVersion.MATERIAL_3 -> " (Material 3)"
            else -> ""
        }

        return framework + material
    }
}

/**
 * Extracts and builds state metadata from accessibility tree
 */
class StateMetadataExtractor {

    companion object {
        private const val TAG = "StateMetadataExtractor"

        // Framework detection patterns
        private val COMPOSE_CLASS_PATTERNS = setOf(
            "androidx.compose",
            "ComposeView"
        )

        private val REACT_NATIVE_PATTERNS = setOf(
            "com.facebook.react",
            "ReactRootView"
        )

        private val FLUTTER_PATTERNS = setOf(
            "io.flutter",
            "FlutterView"
        )

        private val UNITY_PATTERNS = setOf(
            "com.unity3d",
            "UnityPlayer"
        )

        private val WEBVIEW_PATTERNS = setOf(
            "android.webkit.WebView",
            "WebView"
        )

        // Material 3 component patterns
        private val MATERIAL_3_PATTERNS = setOf(
            "material3",
            "MaterialCardView",
            "ExtendedFloatingActionButton"
        )

        // Interactive element classes
        private val INTERACTIVE_CLASSES = setOf(
            "Button",
            "ImageButton",
            "EditText",
            "CheckBox",
            "RadioButton",
            "Switch",
            "Spinner",
            "SeekBar"
        )
    }

    /**
     * Extract metadata from accessibility tree
     *
     * @param rootNode Root of accessibility tree
     * @param state Detected state
     * @return State metadata
     */
    fun extractMetadata(
        rootNode: AccessibilityNodeInfo?,
        state: AppState
    ): StateMetadata {
        if (rootNode == null) {
            return createEmptyMetadata(state)
        }

        val classNames = mutableListOf<String>()
        val textElements = mutableListOf<AccessibilityNodeInfo>()
        val imageElements = mutableListOf<AccessibilityNodeInfo>()
        val interactiveElements = mutableListOf<AccessibilityNodeInfo>()
        var totalElements = 0
        var hierarchyDepth = 0
        var maxBranchingFactor = 0
        var hasDialog = false
        var hasScrollableContent = false
        var hasInputFields = false
        var hasProgressIndicators = false

        // Traverse tree and collect information
        fun traverse(node: AccessibilityNodeInfo, depth: Int) {
            totalElements++
            hierarchyDepth = maxOf(hierarchyDepth, depth)
            maxBranchingFactor = maxOf(maxBranchingFactor, node.childCount)

            val className = node.className?.toString() ?: ""
            classNames.add(className)

            // Categorize element
            if (node.text != null || node.contentDescription != null) {
                textElements.add(node)
            }

            if (className.contains("Image")) {
                imageElements.add(node)
            }

            if (node.isClickable || INTERACTIVE_CLASSES.any { className.contains(it) }) {
                interactiveElements.add(node)
            }

            // Check for specific patterns
            if (className.contains("Dialog")) hasDialog = true
            if (className.contains("ScrollView") || className.contains("RecyclerView")) {
                hasScrollableContent = true
            }
            if (className.contains("EditText")) hasInputFields = true
            if (className.contains("Progress")) hasProgressIndicators = true

            // Traverse children
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { child ->
                    traverse(child, depth + 1)
                }
            }
        }

        traverse(rootNode, 0)

        // Detect UI framework
        val uiFramework = detectFramework(classNames)
        val materialVersion = detectMaterialVersion(classNames)
        val materialComponents = detectMaterialComponents(classNames)

        // Get package and activity name
        val packageName = rootNode.packageName?.toString() ?: ""

        return StateMetadata(
            uiFramework = uiFramework,
            materialVersion = materialVersion,
            totalElements = totalElements,
            interactiveElements = interactiveElements.size,
            textElements = textElements.size,
            imageElements = imageElements.size,
            hierarchyDepth = hierarchyDepth,
            maxBranchingFactor = maxBranchingFactor,
            detectionTimestamp = System.currentTimeMillis(),
            stateDuration = 0L,
            hasDialog = hasDialog,
            hasScrollableContent = hasScrollableContent,
            hasInputFields = hasInputFields,
            hasProgressIndicators = hasProgressIndicators,
            materialComponents = materialComponents,
            packageName = packageName
        )
    }

    /**
     * Detect UI framework from class names
     */
    private fun detectFramework(classNames: List<String>): UIFramework {
        val frameworksDetected = mutableSetOf<UIFramework>()

        for (className in classNames) {
            when {
                COMPOSE_CLASS_PATTERNS.any { className.contains(it) } -> {
                    frameworksDetected.add(UIFramework.JETPACK_COMPOSE)
                }
                REACT_NATIVE_PATTERNS.any { className.contains(it) } -> {
                    frameworksDetected.add(UIFramework.REACT_NATIVE)
                }
                FLUTTER_PATTERNS.any { className.contains(it) } -> {
                    frameworksDetected.add(UIFramework.FLUTTER)
                }
                UNITY_PATTERNS.any { className.contains(it) } -> {
                    frameworksDetected.add(UIFramework.UNITY)
                }
                WEBVIEW_PATTERNS.any { className.contains(it) } -> {
                    frameworksDetected.add(UIFramework.WEBVIEW)
                }
                className.startsWith("android.widget") || className.startsWith("android.view") -> {
                    frameworksDetected.add(UIFramework.NATIVE_ANDROID)
                }
            }
        }

        return when {
            frameworksDetected.isEmpty() -> UIFramework.UNKNOWN
            frameworksDetected.size > 1 -> UIFramework.MIXED
            else -> frameworksDetected.first()
        }
    }

    /**
     * Detect Material Design version
     */
    private fun detectMaterialVersion(classNames: List<String>): MaterialVersion {
        val hasMaterial3 = classNames.any { className ->
            MATERIAL_3_PATTERNS.any { className.contains(it) }
        }

        val hasMaterial = classNames.any { className ->
            className.contains("material", ignoreCase = true)
        }

        return when {
            hasMaterial3 -> MaterialVersion.MATERIAL_3
            hasMaterial -> MaterialVersion.MATERIAL_2
            else -> MaterialVersion.NONE
        }
    }

    /**
     * Detect Material Design components
     */
    private fun detectMaterialComponents(classNames: List<String>): List<MaterialComponent> {
        val components = mutableSetOf<MaterialComponent>()

        for (className in classNames) {
            when {
                className.contains("MaterialButton") -> components.add(MaterialComponent.BUTTON)
                className.contains("FloatingActionButton") -> components.add(MaterialComponent.FAB)
                className.contains("TextInputLayout") || className.contains("TextInputEditText") -> {
                    components.add(MaterialComponent.TEXT_INPUT)
                }
                className.contains("LinearProgressIndicator") -> {
                    components.add(MaterialComponent.PROGRESS_LINEAR)
                }
                className.contains("CircularProgressIndicator") -> {
                    components.add(MaterialComponent.PROGRESS_CIRCULAR)
                }
                className.contains("Chip") -> components.add(MaterialComponent.CHIP)
                className.contains("AlertDialog") -> components.add(MaterialComponent.DIALOG)
                className.contains("Snackbar") -> components.add(MaterialComponent.SNACKBAR)
            }
        }

        return components.toList()
    }

    /**
     * Create empty metadata
     */
    private fun createEmptyMetadata(state: AppState): StateMetadata {
        return StateMetadata(
            uiFramework = UIFramework.UNKNOWN,
            materialVersion = MaterialVersion.UNKNOWN,
            totalElements = 0,
            interactiveElements = 0,
            textElements = 0,
            imageElements = 0,
            hierarchyDepth = 0,
            maxBranchingFactor = 0,
            detectionTimestamp = System.currentTimeMillis(),
            stateDuration = 0L,
            hasDialog = false,
            hasScrollableContent = false,
            hasInputFields = false,
            hasProgressIndicators = false,
            materialComponents = emptyList()
        )
    }
}
