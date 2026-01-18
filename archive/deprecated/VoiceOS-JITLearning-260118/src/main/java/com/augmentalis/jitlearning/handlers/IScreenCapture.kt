/**
 * IScreenCapture.kt - Interface for screen capture operations
 *
 * Handles screen capture, tree traversal, and element discovery.
 * Extracted from JITLearningService as part of SOLID refactoring.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-12
 * Related: VoiceOS-Plan-JITLearnApp-Fixes-51212-V1.md (Track C)
 *
 * @since 2.2.0 (SOLID Refactoring)
 */

package com.augmentalis.jitlearning.handlers

import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.jitlearning.ScreenChangeEvent

/**
 * Screen Capture Handler Interface
 *
 * Responsibilities:
 * - Capture current screen information
 * - Traverse accessibility node hierarchy
 * - Discover and extract element information
 * - Generate screen hashes
 *
 * Single Responsibility: Screen data capture and traversal
 */
interface IScreenCapture {
    /**
     * Capture current screen as ScreenChangeEvent.
     *
     * @param rootNode Root accessibility node
     * @param packageName Current package name
     * @param activityName Current activity name
     * @return ScreenChangeEvent with captured data
     */
    fun captureCurrentScreen(
        rootNode: AccessibilityNodeInfo,
        packageName: String,
        activityName: String
    ): ScreenChangeEvent

    /**
     * Traverse accessibility hierarchy and extract elements.
     *
     * @param root Root node to traverse from
     * @return List of discovered accessibility nodes
     */
    fun traverseHierarchy(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo>

    /**
     * Generate screen hash from root node.
     *
     * @param rootNode Root accessibility node
     * @return Screen hash string
     */
    fun generateScreenHash(rootNode: AccessibilityNodeInfo): String

    /**
     * Count elements in hierarchy.
     *
     * @param root Root node
     * @return Total element count
     */
    fun countElements(root: AccessibilityNodeInfo): Int
}
