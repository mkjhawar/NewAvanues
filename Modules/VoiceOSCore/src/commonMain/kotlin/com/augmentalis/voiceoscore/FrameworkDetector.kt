/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * FrameworkDetector.kt - Cross-platform framework detection
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Refactored: 2026-02-02 - Consolidated redundant detection methods into generic pattern
 *
 * Detects if app is built with cross-platform framework (Flutter, React Native, etc.)
 * to enable enhanced fallback label generation for unlabeled elements.
 *
 * ## Detection Strategy
 *
 * Framework detection uses multiple signals:
 * 1. View hierarchy class names (FlutterView, ReactRootView)
 * 2. Package names (io.flutter, com.facebook.react)
 * 3. Resource ID patterns (flutter_, react_native_)
 *
 * ## KMP Architecture
 *
 * This detector uses a platform-agnostic approach:
 * - Detection logic is shared in commonMain
 * - Platform-specific node traversal via [NodeInfo] interface
 * - Each platform implements [NodeInfo] adapter
 *
 * @since 1.0.0 (Cross-Platform Support)
 */

package com.augmentalis.voiceoscore

/**
 * Platform-agnostic node information interface
 *
 * Abstracts accessibility node info for cross-platform detection.
 * Each platform (Android, iOS, Desktop) provides its own implementation.
 */
interface NodeInfo {
    /** Class name of the view/element */
    val className: String?

    /** Resource ID or accessibility identifier */
    val resourceId: String?

    /** Number of child nodes */
    val childCount: Int

    /** Get child node at index, null if not available */
    fun getChild(index: Int): NodeInfo?

    /** Get parent node, null if root */
    val parent: NodeInfo?
}

/**
 * Configuration for detecting a specific framework.
 *
 * @property framework The framework to detect
 * @property classPatterns Class name patterns to match
 * @property resourcePatterns Resource ID patterns to match (optional)
 * @property packagePatterns Package name patterns to match (optional)
 * @property ignoreCase Whether to ignore case in pattern matching
 * @property checkHierarchy Whether to check parent chain for surface/framework patterns
 * @property hierarchyKeywords Keywords to look for in parent chain
 */
private data class FrameworkDetectionConfig(
    val framework: AppFramework,
    val classPatterns: List<String>,
    val resourcePatterns: List<String> = emptyList(),
    val packagePatterns: List<String> = emptyList(),
    val ignoreCase: Boolean = false,
    val checkHierarchy: Boolean = false,
    val hierarchyKeywords: List<String> = emptyList()
)

/**
 * Cross-Platform Framework Detector
 *
 * Analyzes app view hierarchy to detect underlying framework.
 * Used to enable framework-specific optimizations for voice commands.
 *
 * ## Usage
 *
 * ```kotlin
 * // Android: wrap AccessibilityNodeInfo in AndroidNodeInfo adapter
 * val nodeInfo: NodeInfo = AndroidNodeInfo(accessibilityNode)
 * val result = FrameworkDetector.detectFramework(packageName, nodeInfo)
 *
 * // iOS: wrap accessibility element in iOSNodeInfo adapter
 * val nodeInfo: NodeInfo = iOSNodeInfo(accessibilityElement)
 * val result = FrameworkDetector.detectFramework(bundleId, nodeInfo)
 * ```
 */
object FrameworkDetector {
    private const val MAX_RECURSION_DEPTH = 3
    private const val MAX_FLUTTER_VERSION_DEPTH = 5
    private const val MAX_PARENT_DEPTH = 5

    /**
     * Framework detection configurations in priority order.
     * Higher priority frameworks (game engines that lack accessibility) are checked first.
     */
    private val detectionConfigs = listOf(
        // Game engines (highest priority - typically lack ALL semantic labels)
        FrameworkDetectionConfig(
            framework = AppFramework.UNITY,
            classPatterns = FrameworkPatterns.unityClassPatterns,
            packagePatterns = FrameworkPatterns.unityPackagePatterns,
            checkHierarchy = true,
            hierarchyKeywords = listOf("Unity")
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.UNREAL,
            classPatterns = FrameworkPatterns.unrealClassPatterns,
            packagePatterns = FrameworkPatterns.unrealPackagePatterns,
            ignoreCase = true,
            checkHierarchy = true,
            hierarchyKeywords = listOf("UE", "Game", "Unreal")
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.GODOT,
            classPatterns = FrameworkPatterns.godotClassPatterns,
            packagePatterns = FrameworkPatterns.godotPackagePatterns,
            ignoreCase = true
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.COCOS2D,
            classPatterns = FrameworkPatterns.cocos2dClassPatterns,
            packagePatterns = FrameworkPatterns.cocos2dPackagePatterns,
            ignoreCase = true
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.DEFOLD,
            classPatterns = FrameworkPatterns.defoldClassPatterns,
            packagePatterns = FrameworkPatterns.defoldPackagePatterns,
            ignoreCase = true
        ),
        // Cross-platform UI frameworks
        FrameworkDetectionConfig(
            framework = AppFramework.FLUTTER,
            classPatterns = FrameworkPatterns.flutterClassPatterns,
            resourcePatterns = FrameworkPatterns.flutterResourcePatterns
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.REACT_NATIVE,
            classPatterns = FrameworkPatterns.reactNativeClassPatterns,
            resourcePatterns = FrameworkPatterns.reactNativeResourcePatterns
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.COMPOSE,
            classPatterns = FrameworkPatterns.composeClassPatterns
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.SWIFTUI,
            classPatterns = FrameworkPatterns.swiftUIClassPatterns
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.XAMARIN,
            classPatterns = FrameworkPatterns.xamarinClassPatterns,
            packagePatterns = FrameworkPatterns.xamarinPackagePatterns,
            ignoreCase = true
        ),
        FrameworkDetectionConfig(
            framework = AppFramework.CORDOVA,
            classPatterns = FrameworkPatterns.cordovaClassPatterns,
            packagePatterns = FrameworkPatterns.cordovaPackagePatterns,
            ignoreCase = true
        )
    )

    /**
     * Detect if app is built with cross-platform framework
     *
     * Analyzes root node and package name to identify framework.
     *
     * @param packageName App package name/bundle identifier
     * @param rootNode Root node (may be null)
     * @param currentTimeMillis Current timestamp for result
     * @return Detection result with framework, confidence, and signals
     */
    fun detectFramework(
        packageName: String,
        rootNode: NodeInfo?,
        currentTimeMillis: Long = 0L
    ): FrameworkDetectionResult {
        if (rootNode == null) {
            return FrameworkDetectionResult.native(packageName, currentTimeMillis)
        }

        try {
            // Check each framework configuration in priority order
            for (config in detectionConfigs) {
                val signals = mutableListOf<DetectionSignal>()

                if (hasFrameworkSignatures(rootNode, packageName, config, signals)) {
                    // Special handling for Flutter version detection
                    val flutterVersion = if (config.framework == AppFramework.FLUTTER) {
                        detectFlutterVersion(rootNode, packageName)
                    } else {
                        FlutterVersion.NOT_FLUTTER
                    }

                    return FrameworkDetectionResult(
                        framework = config.framework,
                        confidence = calculateConfidence(signals),
                        flutterVersion = flutterVersion,
                        detectionSignals = signals,
                        packageName = packageName,
                        timestamp = currentTimeMillis
                    )
                }
            }

            return FrameworkDetectionResult.native(packageName, currentTimeMillis)

        } catch (e: Exception) {
            return FrameworkDetectionResult.native(packageName, currentTimeMillis)
        }
    }

    /**
     * Generic framework signature detection using configuration.
     *
     * Checks class names, resource IDs, package names, and optionally
     * traverses the parent hierarchy for framework-specific patterns.
     *
     * @param node The node to check
     * @param packageName App package name
     * @param config Framework detection configuration
     * @param signals List to add detection signals to
     * @return true if framework signatures are found
     */
    private fun hasFrameworkSignatures(
        node: NodeInfo,
        packageName: String,
        config: FrameworkDetectionConfig,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""
        val resourceId = node.resourceId ?: ""

        // Check class name patterns
        if (matchesPatterns(className, config.classPatterns, config.ignoreCase)) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Check resource ID patterns
        if (config.resourcePatterns.isNotEmpty() &&
            matchesPatterns(resourceId, config.resourcePatterns, config.ignoreCase)) {
            signals.add(DetectionSignal(SignalType.RESOURCE_ID, resourceId, "root"))
            return true
        }

        // Check package name patterns
        if (config.packagePatterns.isNotEmpty() &&
            matchesPatterns(packageName, config.packagePatterns, config.ignoreCase)) {
            signals.add(DetectionSignal(SignalType.PACKAGE_NAME, packageName))
            return true
        }

        // Check parent hierarchy for surface/framework patterns (for game engines)
        if (config.checkHierarchy && config.hierarchyKeywords.isNotEmpty()) {
            if (checkParentHierarchy(node, config.hierarchyKeywords, signals)) {
                return true
            }
        }

        // Recursively check children
        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (matchesPatterns(childClass, config.classPatterns, config.ignoreCase)) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if text matches any pattern in the list.
     */
    private fun matchesPatterns(text: String, patterns: List<String>, ignoreCase: Boolean): Boolean {
        return patterns.any { pattern ->
            text.contains(pattern, ignoreCase = ignoreCase)
        }
    }

    /**
     * Check parent hierarchy for framework keywords (used for game engine detection).
     */
    private fun checkParentHierarchy(
        node: NodeInfo,
        keywords: List<String>,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Check if current node is a rendering surface
        if (FrameworkPatterns.surfacePatterns.any { className.contains(it) }) {
            var parent = node.parent
            var depth = 0

            while (parent != null && depth < MAX_PARENT_DEPTH) {
                val parentClass = parent.className ?: ""
                if (keywords.any { parentClass.contains(it, ignoreCase = true) }) {
                    signals.add(DetectionSignal(SignalType.SURFACE_TYPE, className))
                    signals.add(DetectionSignal(SignalType.PARENT_CLASS, parentClass))
                    return true
                }
                parent = parent.parent
                depth++
            }
        }

        return false
    }

    /**
     * Calculate confidence based on number and type of signals
     */
    private fun calculateConfidence(signals: List<DetectionSignal>): Float {
        if (signals.isEmpty()) return 0.0f

        val score = signals.sumOf { signal ->
            when (signal.type) {
                SignalType.CLASS_NAME -> 0.4
                SignalType.PACKAGE_NAME -> 0.3
                SignalType.RESOURCE_ID -> 0.2
                SignalType.HIERARCHY_PATTERN -> 0.3
                SignalType.CHILD_CLASS -> 0.2
                SignalType.PARENT_CLASS -> 0.2
                SignalType.SURFACE_TYPE -> 0.15
                SignalType.ACTIVITY_NAME -> 0.25
            }
        }

        return minOf(1.0f, score.toFloat())
    }

    // ========================================================================
    // Flutter Version Detection
    // ========================================================================

    /**
     * Detect Flutter version based on accessibility node characteristics
     *
     * Flutter 3.19+ (Feb 2024) introduced SemanticsProperties.identifier which
     * maps to platform's accessibility identifier. Apps using this feature will have
     * resource IDs with patterns like "flutter_semantics_<id>".
     *
     * @param rootNode Root node to analyze
     * @param packageName App package name
     * @return Detected Flutter version
     */
    fun detectFlutterVersion(rootNode: NodeInfo?, packageName: String): FlutterVersion {
        if (rootNode == null) return FlutterVersion.NOT_FLUTTER

        // First check if this is even a Flutter app
        val signals = mutableListOf<DetectionSignal>()
        val flutterConfig = detectionConfigs.find { it.framework == AppFramework.FLUTTER }
            ?: return FlutterVersion.NOT_FLUTTER

        if (!hasFrameworkSignatures(rootNode, packageName, flutterConfig, signals)) {
            return FlutterVersion.NOT_FLUTTER
        }

        // Check for Flutter 3.19+ identifier patterns
        val hasStableIdentifiers = checkForFlutter319Identifiers(rootNode, 0, MAX_FLUTTER_VERSION_DEPTH)

        return if (hasStableIdentifiers) {
            FlutterVersion.FLUTTER_319_PLUS
        } else {
            FlutterVersion.FLUTTER_LEGACY
        }
    }

    /**
     * Check if node tree contains Flutter 3.19+ stable identifiers
     */
    private fun checkForFlutter319Identifiers(
        node: NodeInfo,
        depth: Int,
        maxDepth: Int
    ): Boolean {
        if (depth >= maxDepth) return false

        try {
            // Check current node's resource ID for Flutter 3.19+ patterns
            val resourceId = node.resourceId
            if (resourceId != null) {
                if (resourceId.contains("flutter_semantics_") ||
                    resourceId.contains("flutter_id_") ||
                    resourceId.contains(":id/flutter_semantics_")) {
                    return true
                }
            }

            // Check children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (checkForFlutter319Identifiers(child, depth + 1, maxDepth)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore errors during version check
        }

        return false
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Recursively check children for framework signatures
     */
    private fun checkChildrenForSignature(
        node: NodeInfo,
        depth: Int,
        maxDepth: Int,
        predicate: (NodeInfo) -> Boolean
    ): Boolean {
        if (depth >= maxDepth) return false

        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue

                // Check if child matches
                if (predicate(child)) {
                    return true
                }

                // Recursively check child's children
                if (checkChildrenForSignature(child, depth + 1, maxDepth, predicate)) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Ignore errors during child traversal
        }

        return false
    }
}
