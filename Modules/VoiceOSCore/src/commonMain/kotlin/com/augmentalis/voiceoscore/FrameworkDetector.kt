@file:Suppress("UNUSED_PARAMETER") // packageName reserved for future use

/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * FrameworkDetector.kt - Cross-platform framework detection
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: LearnAppCore CrossPlatformDetector.kt
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

        val signals = mutableListOf<DetectionSignal>()

        try {
            // Check for Unity (HIGHEST PRIORITY - Unity games often lack ALL semantic labels)
            if (hasUnitySignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.UNITY,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for Unreal Engine (HIGH PRIORITY - similar to Unity)
            signals.clear()
            if (hasUnrealSignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.UNREAL,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for Flutter
            signals.clear()
            if (hasFlutterSignatures(rootNode, packageName, signals)) {
                val flutterVersion = detectFlutterVersion(rootNode, packageName)
                return FrameworkDetectionResult(
                    framework = AppFramework.FLUTTER,
                    confidence = calculateConfidence(signals),
                    flutterVersion = flutterVersion,
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for React Native
            signals.clear()
            if (hasReactNativeSignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.REACT_NATIVE,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for Compose (Android)
            signals.clear()
            if (hasComposeSignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.COMPOSE,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for SwiftUI (iOS)
            signals.clear()
            if (hasSwiftUISignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.SWIFTUI,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for Xamarin
            signals.clear()
            if (hasXamarinSignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.XAMARIN,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for Cordova/Ionic
            signals.clear()
            if (hasCordovaSignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.CORDOVA,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for Godot Engine
            signals.clear()
            if (hasGodotSignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.GODOT,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for Cocos2d-x Engine
            signals.clear()
            if (hasCocos2dSignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.COCOS2D,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            // Check for Defold Engine
            signals.clear()
            if (hasDefoldSignatures(rootNode, packageName, signals)) {
                return FrameworkDetectionResult(
                    framework = AppFramework.DEFOLD,
                    confidence = calculateConfidence(signals),
                    detectionSignals = signals,
                    packageName = packageName,
                    timestamp = currentTimeMillis
                )
            }

            return FrameworkDetectionResult.native(packageName, currentTimeMillis)

        } catch (e: Exception) {
            return FrameworkDetectionResult.native(packageName, currentTimeMillis)
        }
    }

    /**
     * Calculate confidence based on number and type of signals
     */
    private fun calculateConfidence(signals: List<DetectionSignal>): Float {
        if (signals.isEmpty()) return 0.0f

        // Weight different signal types
        var score = 0.0f
        for (signal in signals) {
            score += when (signal.type) {
                SignalType.CLASS_NAME -> 0.4f
                SignalType.PACKAGE_NAME -> 0.3f
                SignalType.RESOURCE_ID -> 0.2f
                SignalType.HIERARCHY_PATTERN -> 0.3f
                SignalType.CHILD_CLASS -> 0.2f
                SignalType.PARENT_CLASS -> 0.2f
                SignalType.SURFACE_TYPE -> 0.15f
                SignalType.ACTIVITY_NAME -> 0.25f
            }
        }

        return minOf(1.0f, score)
    }

    // ========================================================================
    // Framework Detection Methods
    // ========================================================================

    /**
     * Check if app uses Flutter framework
     */
    private fun hasFlutterSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Check for FlutterView class
        if (FrameworkPatterns.flutterClassPatterns.any { className.contains(it) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Check resource IDs for flutter patterns
        val resourceId = node.resourceId ?: ""
        if (FrameworkPatterns.flutterResourcePatterns.any { resourceId.contains(it) }) {
            signals.add(DetectionSignal(SignalType.RESOURCE_ID, resourceId, "root"))
            return true
        }

        // Recursively check children
        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (FrameworkPatterns.flutterClassPatterns.any { childClass.contains(it) }) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if app uses React Native framework
     */
    private fun hasReactNativeSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Check for ReactRootView class
        if (FrameworkPatterns.reactNativeClassPatterns.any { className.contains(it) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Check resource IDs for react patterns
        val resourceId = node.resourceId ?: ""
        if (FrameworkPatterns.reactNativeResourcePatterns.any { resourceId.contains(it) }) {
            signals.add(DetectionSignal(SignalType.RESOURCE_ID, resourceId, "root"))
            return true
        }

        // Recursively check children
        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (FrameworkPatterns.reactNativeClassPatterns.any { childClass.contains(it) }) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if app uses Jetpack Compose
     */
    private fun hasComposeSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        if (FrameworkPatterns.composeClassPatterns.any { className.contains(it) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (FrameworkPatterns.composeClassPatterns.any { childClass.contains(it) }) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if app uses SwiftUI
     */
    private fun hasSwiftUISignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        if (FrameworkPatterns.swiftUIClassPatterns.any { className.contains(it) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (FrameworkPatterns.swiftUIClassPatterns.any { childClass.contains(it) }) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if app uses Xamarin framework
     */
    private fun hasXamarinSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Check for mono.android package
        if (FrameworkPatterns.xamarinClassPatterns.any { className.contains(it, ignoreCase = true) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Check package name for Xamarin
        if (FrameworkPatterns.xamarinPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
            signals.add(DetectionSignal(SignalType.PACKAGE_NAME, packageName))
            return true
        }

        return false
    }

    /**
     * Check if app uses Cordova/Ionic framework
     */
    private fun hasCordovaSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Check for WebView classes (Cordova is WebView-based)
        if (className.contains("WebView")) {
            // Check if package name suggests Cordova/Ionic
            if (FrameworkPatterns.cordovaPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
                signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
                signals.add(DetectionSignal(SignalType.PACKAGE_NAME, packageName))
                return true
            }
        }

        if (FrameworkPatterns.cordovaClassPatterns.any { className.contains(it) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        return false
    }

    /**
     * Check if app uses Unity game engine
     */
    private fun hasUnitySignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Signature 1: UnityPlayer view
        if (FrameworkPatterns.unityClassPatterns.any { className.contains(it) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Signature 2: Package name patterns
        if (FrameworkPatterns.unityPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
            signals.add(DetectionSignal(SignalType.PACKAGE_NAME, packageName))
            return true
        }

        // Signature 3: Shallow view hierarchy (Unity renders to single surface)
        if (node.childCount <= 1 && className.contains("Player", ignoreCase = true)) {
            signals.add(DetectionSignal(SignalType.HIERARCHY_PATTERN, "shallow hierarchy with Player"))
            return true
        }

        // Signature 4: OpenGL/Vulkan rendering surface with Unity parent
        if (FrameworkPatterns.surfacePatterns.any { className.contains(it) }) {
            var parent = node.parent
            var depth = 0
            while (parent != null && depth < 5) {
                val parentClass = parent.className ?: ""
                if (parentClass.contains("Unity", ignoreCase = true)) {
                    signals.add(DetectionSignal(SignalType.SURFACE_TYPE, className))
                    signals.add(DetectionSignal(SignalType.PARENT_CLASS, parentClass))
                    return true
                }
                parent = parent.parent
                depth++
            }
        }

        // Signature 5: Check children recursively for Unity signatures
        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (FrameworkPatterns.unityClassPatterns.any { childClass.contains(it) } ||
                childClass.contains("Unity", ignoreCase = true)) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if app uses Unreal Engine
     */
    private fun hasUnrealSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Signature 1: UE4/UE5/Unreal class names
        if (FrameworkPatterns.unrealClassPatterns.any { className.contains(it, ignoreCase = true) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Signature 2: Package name patterns
        if (FrameworkPatterns.unrealPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
            signals.add(DetectionSignal(SignalType.PACKAGE_NAME, packageName))
            return true
        }

        // Signature 3: Characteristic Unreal view hierarchy (very few children + SurfaceView)
        if (hasUnrealHierarchyPattern(node)) {
            signals.add(DetectionSignal(SignalType.HIERARCHY_PATTERN, "Unreal shallow hierarchy"))
            return true
        }

        // Signature 4: SurfaceView with Unreal parent chain
        if (className.contains("SurfaceView")) {
            var parent = node.parent
            var depth = 0
            while (parent != null && depth < 5) {
                val parentClass = parent.className ?: ""
                if (parentClass.contains("UE", ignoreCase = false) ||
                    parentClass.contains("Game", ignoreCase = true) ||
                    parentClass.contains("Unreal", ignoreCase = true)) {
                    signals.add(DetectionSignal(SignalType.SURFACE_TYPE, className))
                    signals.add(DetectionSignal(SignalType.PARENT_CLASS, parentClass))
                    return true
                }
                parent = parent.parent
                depth++
            }
        }

        // Signature 5: Check children recursively for Unreal signatures
        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (FrameworkPatterns.unrealClassPatterns.any { childClass.contains(it, ignoreCase = true) }) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check for characteristic Unreal view hierarchy pattern
     */
    private fun hasUnrealHierarchyPattern(node: NodeInfo): Boolean {
        // Root should have very few children (Unreal uses minimal platform UI)
        if (node.childCount > 3) return false

        // Should contain SurfaceView (rendering surface)
        var hasSurface = false
        try {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                if (child.className?.contains("SurfaceView") == true) {
                    hasSurface = true
                    break
                }
            }
        } catch (e: Exception) {
            // Ignore errors during hierarchy check
        }

        // Unreal pattern: very few children + SurfaceView present
        return hasSurface && node.childCount <= 3
    }

    /**
     * Check if app uses Godot Engine
     */
    private fun hasGodotSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Signature 1: GodotView or GodotApp classes
        if (FrameworkPatterns.godotClassPatterns.any { className.contains(it) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Signature 2: Package name patterns
        if (FrameworkPatterns.godotPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
            signals.add(DetectionSignal(SignalType.PACKAGE_NAME, packageName))
            return true
        }

        // Signature 3: Check children recursively for Godot signatures
        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (childClass.contains("Godot", ignoreCase = true)) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if app uses Cocos2d-x Engine
     */
    private fun hasCocos2dSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Signature 1: Cocos2d-x view classes
        if (FrameworkPatterns.cocos2dClassPatterns.any { className.contains(it, ignoreCase = false) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Signature 2: Package name contains cocos
        if (FrameworkPatterns.cocos2dPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
            signals.add(DetectionSignal(SignalType.PACKAGE_NAME, packageName))
            return true
        }

        // Signature 3: Check children recursively for Cocos signatures
        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (childClass.contains("Cocos", ignoreCase = true)) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
    }

    /**
     * Check if app uses Defold Engine
     */
    private fun hasDefoldSignatures(
        node: NodeInfo,
        packageName: String,
        signals: MutableList<DetectionSignal>
    ): Boolean {
        val className = node.className ?: ""

        // Signature 1: DefoldActivity class
        if (className.contains("DefoldActivity")) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            return true
        }

        // Signature 2: NativeActivity + defold in package name
        if (className.contains("NativeActivity") &&
            FrameworkPatterns.defoldPackagePatterns.any { packageName.contains(it, ignoreCase = true) }) {
            signals.add(DetectionSignal(SignalType.CLASS_NAME, className, "root"))
            signals.add(DetectionSignal(SignalType.PACKAGE_NAME, packageName))
            return true
        }

        // Signature 3: Check children recursively for Defold signatures
        return checkChildrenForSignature(node, 0, MAX_RECURSION_DEPTH) { childNode ->
            val childClass = childNode.className ?: ""
            if (childClass.contains("Defold", ignoreCase = true)) {
                signals.add(DetectionSignal(SignalType.CHILD_CLASS, childClass))
                true
            } else {
                false
            }
        }
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
        if (!hasFlutterSignatures(rootNode, packageName, signals)) {
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
