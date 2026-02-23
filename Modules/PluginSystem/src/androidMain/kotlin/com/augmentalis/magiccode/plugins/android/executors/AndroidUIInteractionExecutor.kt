/**
 * AndroidUIInteractionExecutor.kt - Android implementation of UIInteractionExecutor
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22 (Phase 4)
 *
 * Bridges the UIInteractionPlugin to Android AccessibilityService for
 * click, long-press, double-tap, and other UI interactions.
 */
package com.augmentalis.magiccode.plugins.android.executors

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.magiccode.plugins.android.ServiceRegistry
import com.augmentalis.magiccode.plugins.builtin.UIInteractionExecutor
import com.augmentalis.voiceoscore.Bounds
import com.augmentalis.voiceoscore.ElementFingerprint
import com.augmentalis.voiceoscore.ElementInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Android implementation of UIInteractionExecutor.
 *
 * Uses AccessibilityService for UI interactions:
 * - Direct ACTION_CLICK for standard clicks
 * - Gesture API for coordinates-based clicks, long press, double tap
 * - AVID-based element lookup for fast execution
 *
 * @param serviceRegistry Registry to retrieve AccessibilityService from
 */
// recycle() deprecated API 34+ (no-op on 34+, still needed for minSdk 29)
@Suppress("DEPRECATION")
class AndroidUIInteractionExecutor(
    private val serviceRegistry: ServiceRegistry
) : UIInteractionExecutor {

    private val accessibilityService: AccessibilityService?
        get() = serviceRegistry.getSync(ServiceRegistry.ACCESSIBILITY_SERVICE)

    // =========================================================================
    // Element Discovery
    // =========================================================================

    override suspend fun getScreenElements(): List<ElementInfo> {
        val service = accessibilityService ?: return emptyList()
        val rootNode = service.rootInActiveWindow ?: return emptyList()

        val elements = mutableListOf<ElementInfo>()
        extractElements(rootNode, elements)
        rootNode.recycle()

        return elements
    }

    private fun extractElements(node: AccessibilityNodeInfo, elements: MutableList<ElementInfo>) {
        if (node.isVisibleToUser) {
            val isClickable = node.isClickable
            val isEditable = node.isEditable
            val hasText = !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()

            if (isClickable || isEditable || hasText) {
                val bounds = Rect()
                node.getBoundsInScreen(bounds)

                val element = ElementInfo(
                    className = node.className?.toString() ?: "",
                    text = node.text?.toString() ?: "",
                    contentDescription = node.contentDescription?.toString() ?: "",
                    resourceId = node.viewIdResourceName ?: "",
                    packageName = node.packageName?.toString() ?: "",
                    bounds = Bounds(bounds.left, bounds.top, bounds.right, bounds.bottom),
                    isClickable = isClickable,
                    isLongClickable = node.isLongClickable,
                    isScrollable = node.isScrollable,
                    isEnabled = node.isEnabled,
                    isChecked = if (node.isCheckable) node.isChecked else null,
                    isSelected = node.isSelected
                )
                elements.add(element)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            try {
                extractElements(child, elements)
            } finally {
                child.recycle()
            }
        }
    }

    // =========================================================================
    // Direct Element Actions
    // =========================================================================

    override suspend fun clickElement(element: ElementInfo): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        // Find node by bounds (most reliable for disambiguation scenarios)
        val node = findNodeByBounds(rootNode, element.bounds)
        rootNode.recycle()

        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            result
        } else {
            // Fallback to gesture at center of bounds
            clickAtCoordinates(service, element.bounds.centerX, element.bounds.centerY)
        }
    }

    override suspend fun longClickElement(element: ElementInfo): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByBounds(rootNode, element.bounds)
        rootNode.recycle()

        return if (node != null && node.isLongClickable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            node.recycle()
            result
        } else {
            node?.recycle()
            // Use gesture for long press
            longClickAtCoordinates(service, element.bounds.centerX, element.bounds.centerY)
        }
    }

    override suspend fun doubleClickElement(element: ElementInfo): Boolean {
        val service = accessibilityService ?: return false
        return doubleClickAtCoordinates(service, element.bounds.centerX, element.bounds.centerY)
    }

    // =========================================================================
    // AVID-based Actions
    // =========================================================================

    override suspend fun clickByAvid(avid: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByAvid(rootNode, avid)
        rootNode.recycle()

        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun longClickByAvid(avid: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByAvid(rootNode, avid)
        rootNode.recycle()

        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            node.recycle()
            result
        } else {
            false
        }
    }

    // =========================================================================
    // Text-based Actions
    // =========================================================================

    override suspend fun clickByText(text: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByText(rootNode, text)
        rootNode.recycle()

        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun longClickByText(text: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByText(rootNode, text)
        rootNode.recycle()

        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
            node.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun doubleClickByText(text: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByText(rootNode, text)
        rootNode.recycle()

        return if (node != null) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            node.recycle()
            doubleClickAtCoordinates(service, bounds.centerX(), bounds.centerY())
        } else {
            false
        }
    }

    // =========================================================================
    // Expand/Collapse Actions
    // =========================================================================

    override suspend fun expand(target: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByText(rootNode, target)
        rootNode.recycle()

        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
            node.recycle()
            result
        } else {
            false
        }
    }

    override suspend fun collapse(target: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByText(rootNode, target)
        rootNode.recycle()

        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
            node.recycle()
            result
        } else {
            false
        }
    }

    // =========================================================================
    // Check/Toggle Actions
    // =========================================================================

    override suspend fun setChecked(target: String, checked: Boolean): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByText(rootNode, target) ?: findCheckableNode(rootNode, target)
        rootNode.recycle()

        return if (node != null && node.isCheckable) {
            val currentState = node.isChecked
            val result = if (currentState != checked) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            } else {
                true // Already in desired state
            }
            node.recycle()
            result
        } else {
            node?.recycle()
            false
        }
    }

    override suspend fun toggle(target: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByText(rootNode, target) ?: findCheckableNode(rootNode, target)
        rootNode.recycle()

        return if (node != null) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            node.recycle()
            result
        } else {
            false
        }
    }

    // =========================================================================
    // Focus/Dismiss Actions
    // =========================================================================

    override suspend fun focus(target: String): Boolean {
        val service = accessibilityService ?: return false
        val rootNode = service.rootInActiveWindow ?: return false

        val node = findNodeByText(rootNode, target) ?: findEditableNode(rootNode, target)
        rootNode.recycle()

        return if (node != null && node.isFocusable) {
            val result = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            node.recycle()
            result
        } else {
            node?.recycle()
            false
        }
    }

    override suspend fun dismiss(): Boolean {
        val service = accessibilityService ?: return false
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    // =========================================================================
    // Private Helpers - Node Finding
    // =========================================================================

    private fun findNodeByAvid(root: AccessibilityNodeInfo, avid: String): AccessibilityNodeInfo? {
        // Generate AVID for this node and compare
        val nodeAvid = generateNodeAvid(root)
        if (nodeAvid == avid) {
            return AccessibilityNodeInfo.obtain(root)
        }

        // Also check the hash portion only (for backwards compatibility)
        val hashPortion = avid.substringAfter(":", avid)
        if (nodeAvid?.endsWith(":$hashPortion") == true || nodeAvid?.contains(hashPortion) == true) {
            return AccessibilityNodeInfo.obtain(root)
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            try {
                val found = findNodeByAvid(child, avid)
                if (found != null) return found
            } finally {
                child.recycle()
            }
        }
        return null
    }

    private fun generateNodeAvid(node: AccessibilityNodeInfo): String? {
        if (!node.isVisibleToUser) return null

        val className = node.className?.toString() ?: ""
        val packageName = node.packageName?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""
        val resourceId = node.viewIdResourceName ?: ""

        return ElementFingerprint.generate(
            className = className,
            packageName = packageName,
            resourceId = resourceId,
            text = text,
            contentDesc = contentDesc
        )
    }

    private fun findNodeByText(root: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        val lowerText = text.lowercase()

        // Check this node
        val nodeText = root.text?.toString()?.lowercase() ?: ""
        val contentDesc = root.contentDescription?.toString()?.lowercase() ?: ""

        if ((nodeText.isNotEmpty() && nodeText.contains(lowerText)) ||
            (contentDesc.isNotEmpty() && contentDesc.contains(lowerText))) {
            return AccessibilityNodeInfo.obtain(root)
        }

        // Search children
        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            try {
                val found = findNodeByText(child, text)
                if (found != null) return found
            } finally {
                child.recycle()
            }
        }
        return null
    }

    private fun findNodeByBounds(root: AccessibilityNodeInfo, bounds: Bounds): AccessibilityNodeInfo? {
        val rect = Rect()
        root.getBoundsInScreen(rect)

        // Check if bounds match within tolerance
        if (boundsMatch(rect, bounds, tolerance = BOUNDS_MATCH_TOLERANCE)) {
            return AccessibilityNodeInfo.obtain(root)
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            try {
                val found = findNodeByBounds(child, bounds)
                if (found != null) return found
            } finally {
                child.recycle()
            }
        }
        return null
    }

    private fun boundsMatch(rect: Rect, bounds: Bounds, tolerance: Int): Boolean {
        return kotlin.math.abs(rect.left - bounds.left) <= tolerance &&
                kotlin.math.abs(rect.top - bounds.top) <= tolerance &&
                kotlin.math.abs(rect.right - bounds.right) <= tolerance &&
                kotlin.math.abs(rect.bottom - bounds.bottom) <= tolerance
    }

    private fun findCheckableNode(root: AccessibilityNodeInfo, hint: String): AccessibilityNodeInfo? {
        if (root.isCheckable && root.isVisibleToUser) {
            val text = root.text?.toString()?.lowercase() ?: ""
            val contentDesc = root.contentDescription?.toString()?.lowercase() ?: ""
            if (text.contains(hint.lowercase()) || contentDesc.contains(hint.lowercase())) {
                return AccessibilityNodeInfo.obtain(root)
            }
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            try {
                val found = findCheckableNode(child, hint)
                if (found != null) return found
            } finally {
                child.recycle()
            }
        }
        return null
    }

    private fun findEditableNode(root: AccessibilityNodeInfo, hint: String): AccessibilityNodeInfo? {
        if (root.isEditable && root.isVisibleToUser) {
            val text = root.text?.toString()?.lowercase() ?: ""
            val contentDesc = root.contentDescription?.toString()?.lowercase() ?: ""
            val hintText = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                root.hintText?.toString()?.lowercase() ?: ""
            } else ""

            if (text.contains(hint.lowercase()) ||
                contentDesc.contains(hint.lowercase()) ||
                hintText.contains(hint.lowercase())) {
                return AccessibilityNodeInfo.obtain(root)
            }
        }

        for (i in 0 until root.childCount) {
            val child = root.getChild(i) ?: continue
            try {
                val found = findEditableNode(child, hint)
                if (found != null) return found
            } finally {
                child.recycle()
            }
        }
        return null
    }

    // =========================================================================
    // Private Helpers - Gesture Actions
    // =========================================================================

    private suspend fun clickAtCoordinates(service: AccessibilityService, x: Int, y: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, CLICK_DURATION_MS))
            .build()

        return dispatchGesture(service, gesture)
    }

    private suspend fun longClickAtCoordinates(service: AccessibilityService, x: Int, y: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        val path = Path()
        path.moveTo(x.toFloat(), y.toFloat())

        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, LONG_CLICK_DURATION_MS))
            .build()

        return dispatchGesture(service, gesture)
    }

    private suspend fun doubleClickAtCoordinates(service: AccessibilityService, x: Int, y: Int): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false
        }

        // First click
        if (!clickAtCoordinates(service, x, y)) {
            return false
        }

        // Brief delay between clicks
        delay(DOUBLE_CLICK_INTERVAL_MS)

        // Second click
        return clickAtCoordinates(service, x, y)
    }

    private suspend fun dispatchGesture(
        service: AccessibilityService,
        gesture: GestureDescription
    ): Boolean {
        return withTimeoutOrNull(GESTURE_TIMEOUT_MS) {
            suspendCancellableCoroutine { continuation ->
                val callback = object : AccessibilityService.GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        continuation.resume(true)
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        continuation.resume(false)
                    }
                }

                if (!service.dispatchGesture(gesture, callback, null)) {
                    continuation.resume(false)
                }
            }
        } ?: false
    }

    companion object {
        private const val CLICK_DURATION_MS = 50L
        private const val LONG_CLICK_DURATION_MS = 800L
        private const val DOUBLE_CLICK_INTERVAL_MS = 100L
        private const val GESTURE_TIMEOUT_MS = 5000L
        /** Pixel tolerance for matching AccessibilityNodeInfo bounds to ElementInfo bounds. */
        private const val BOUNDS_MATCH_TOLERANCE = 5
    }
}
