/**
 * ScreenCaptureHandler.kt - Implementation of screen capture operations
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

import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.jitlearning.ScreenChangeEvent
import java.security.MessageDigest

/**
 * Screen Capture Handler
 *
 * Captures screen information and traverses accessibility hierarchy.
 *
 * Features:
 * - Screen hash generation
 * - Hierarchy traversal
 * - Element counting
 * - ScreenChangeEvent creation
 *
 * Thread Safety: Not thread-safe, caller must synchronize
 */
class ScreenCaptureHandler : IScreenCapture {

    companion object {
        private const val TAG = "ScreenCaptureHandler"
    }

    override fun captureCurrentScreen(
        rootNode: AccessibilityNodeInfo,
        packageName: String,
        activityName: String
    ): ScreenChangeEvent {
        val screenHash = generateScreenHash(rootNode)
        val elementCount = countElements(rootNode)

        return ScreenChangeEvent.create(
            screenHash = screenHash,
            activityName = activityName,
            packageName = packageName,
            elementCount = elementCount,
            isNewScreen = true
        )
    }

    override fun traverseHierarchy(root: AccessibilityNodeInfo): List<AccessibilityNodeInfo> {
        val nodes = mutableListOf<AccessibilityNodeInfo>()
        traverseRecursive(root, nodes)
        return nodes
    }

    private fun traverseRecursive(node: AccessibilityNodeInfo, result: MutableList<AccessibilityNodeInfo>) {
        result.add(node)

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            traverseRecursive(child, result)
        }
    }

    override fun generateScreenHash(rootNode: AccessibilityNodeInfo): String {
        return try {
            // Build fingerprint from hierarchy structure
            val fingerprint = buildHierarchyFingerprint(rootNode)

            // Generate MD5 hash
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(fingerprint.toByteArray())

            // Convert to hex string (first 12 characters)
            hashBytes.joinToString("") { "%02x".format(it) }.take(12)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate screen hash", e)
            // Fallback to simple hash
            rootNode.hashCode().toString(16)
        }
    }

    private fun buildHierarchyFingerprint(node: AccessibilityNodeInfo): String {
        val sb = StringBuilder()
        appendNodeFingerprint(node, sb)
        return sb.toString()
    }

    private fun appendNodeFingerprint(node: AccessibilityNodeInfo, sb: StringBuilder) {
        // Append node properties
        sb.append(node.className ?: "")
        sb.append("|")
        sb.append(node.viewIdResourceName ?: "")
        sb.append("|")

        // Append bounds
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        sb.append(bounds.width())
        sb.append("x")
        sb.append(bounds.height())
        sb.append("|")

        // Append children (recursive)
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            appendNodeFingerprint(child, sb)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }
    }

    override fun countElements(root: AccessibilityNodeInfo): Int {
        var count = 1 // Count root

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            count += countElements(child)

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                child.recycle()
            }
        }

        return count
    }
}
