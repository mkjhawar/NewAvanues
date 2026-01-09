/**
 * AndroidUIExecutor.kt - Android UI executor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Android implementation of UIExecutor using AccessibilityService.
 */
package com.augmentalis.voiceoscoreng.handlers

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.VUIDGenerator
import kotlinx.coroutines.delay

/**
 * Android implementation of [UIExecutor].
 *
 * Uses AccessibilityService to perform UI interactions.
 *
 * @param accessibilityServiceProvider Provider for the accessibility service instance
 * @param vuidLookup Function to lookup element by VUID
 */
class AndroidUIExecutor(
    private val accessibilityServiceProvider: () -> AccessibilityService?,
    private val vuidLookup: (String) -> AccessibilityNodeInfo? = { null }
) : UIExecutor {

    // ═══════════════════════════════════════════════════════════════════════════
    // Element Discovery (for disambiguation)
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun getScreenElements(): List<ElementInfo> {
        val service = accessibilityServiceProvider() ?: return emptyList()
        val rootNode = service.rootInActiveWindow ?: return emptyList()

        val elements = mutableListOf<ElementInfo>()
        collectElementsRecursive(rootNode, elements)
        return elements
    }

    private fun collectElementsRecursive(
        node: AccessibilityNodeInfo,
        elements: MutableList<ElementInfo>
    ) {
        // Only collect actionable elements with content
        if (node.isClickable || node.isLongClickable || node.isScrollable) {
            val text = node.text?.toString() ?: ""
            val contentDescription = node.contentDescription?.toString() ?: ""
            val resourceId = node.viewIdResourceName ?: ""

            // Only add if has meaningful content for voice targeting
            if (text.isNotBlank() || contentDescription.isNotBlank() || resourceId.isNotBlank()) {
                val rect = android.graphics.Rect()
                node.getBoundsInScreen(rect)

                elements.add(
                    ElementInfo(
                        className = node.className?.toString() ?: "",
                        resourceId = resourceId,
                        text = text,
                        contentDescription = contentDescription,
                        bounds = Bounds(rect.left, rect.top, rect.right, rect.bottom),
                        isClickable = node.isClickable,
                        isLongClickable = node.isLongClickable,
                        isScrollable = node.isScrollable,
                        isEnabled = node.isEnabled,
                        packageName = node.packageName?.toString() ?: ""
                    )
                )
            }
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                collectElementsRecursive(child, elements)
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Direct Element Actions (used after disambiguation)
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun clickElement(element: ElementInfo): Boolean {
        val node = findNodeByElementInfo(element) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun longClickElement(element: ElementInfo): Boolean {
        val node = findNodeByElementInfo(element) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override suspend fun doubleClickElement(element: ElementInfo): Boolean {
        val node = findNodeByElementInfo(element) ?: return false
        val firstClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        delay(50)
        val secondClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return firstClick && secondClick
    }

    private fun findNodeByElementInfo(element: ElementInfo): AccessibilityNodeInfo? {
        val service = accessibilityServiceProvider() ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        return findNodeByElementInfoRecursive(rootNode, element)
    }

    private fun findNodeByElementInfoRecursive(
        node: AccessibilityNodeInfo,
        element: ElementInfo
    ): AccessibilityNodeInfo? {
        // Match by text, resourceId, and bounds
        val nodeText = node.text?.toString() ?: ""
        val nodeResourceId = node.viewIdResourceName ?: ""
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)

        if (nodeText == element.text &&
            nodeResourceId == element.resourceId &&
            rect.left == element.bounds.left &&
            rect.top == element.bounds.top
        ) {
            return node
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByElementInfoRecursive(child, element)
                if (result != null) return result
            }
        }

        return null
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Legacy Text/VUID Actions
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun clickByText(text: String): Boolean {
        val node = findNodeByText(text) ?: findNodeByDescription(text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun clickByVuid(vuid: String): Boolean {
        // Try provided lookup first (if caller has a cache)
        var node = vuidLookup(vuid)

        // Fallback: search accessibility tree by regenerating VUIDs
        if (node == null) {
            node = findNodeByVuidSearch(vuid)
        }

        if (node == null) return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun longClickByText(text: String): Boolean {
        val node = findNodeByText(text) ?: findNodeByDescription(text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override suspend fun longClickByVuid(vuid: String): Boolean {
        var node = vuidLookup(vuid)
        if (node == null) {
            node = findNodeByVuidSearch(vuid)
        }
        if (node == null) return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    override suspend fun doubleClickByText(text: String): Boolean {
        val node = findNodeByText(text) ?: findNodeByDescription(text) ?: return false
        val firstClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        delay(50) // Small delay between clicks
        val secondClick = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        return firstClick && secondClick
    }

    override suspend fun expand(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_EXPAND)
    }

    override suspend fun collapse(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_COLLAPSE)
    }

    override suspend fun setChecked(target: String, checked: Boolean): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false

        // Check current state
        val isChecked = node.isChecked
        if (isChecked == checked) return true // Already in desired state

        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun toggle(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun focus(target: String): Boolean {
        val node = findNodeByText(target) ?: findNodeByDescription(target) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
    }

    override suspend fun dismiss(): Boolean {
        val service = accessibilityServiceProvider() ?: return false

        // Try to find and click dismiss/close button
        val dismissNode = findNodeByText("dismiss")
            ?: findNodeByText("close")
            ?: findNodeByText("cancel")
            ?: findNodeByText("ok")
            ?: findNodeByDescription("dismiss")
            ?: findNodeByDescription("close")

        if (dismissNode != null) {
            return dismissNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        }

        // Fallback to back action
        return service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
    }

    /**
     * Find a node by its text content.
     */
    private fun findNodeByText(text: String): AccessibilityNodeInfo? {
        val service = accessibilityServiceProvider() ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        return findNodeByTextRecursive(rootNode, text.lowercase())
    }

    /**
     * Recursively search for node by text.
     */
    private fun findNodeByTextRecursive(
        node: AccessibilityNodeInfo,
        searchText: String
    ): AccessibilityNodeInfo? {
        // Check this node's text
        node.text?.toString()?.lowercase()?.let { nodeText ->
            if (nodeText.contains(searchText)) {
                return node
            }
        }

        // Check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByTextRecursive(child, searchText)
                if (result != null) return result
            }
        }

        return null
    }

    /**
     * Find a node by its content description.
     */
    private fun findNodeByDescription(description: String): AccessibilityNodeInfo? {
        val service = accessibilityServiceProvider() ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        return findNodeByDescriptionRecursive(rootNode, description.lowercase())
    }

    /**
     * Recursively search for node by content description.
     */
    private fun findNodeByDescriptionRecursive(
        node: AccessibilityNodeInfo,
        searchDescription: String
    ): AccessibilityNodeInfo? {
        // Check this node's content description
        node.contentDescription?.toString()?.lowercase()?.let { nodeDescription ->
            if (nodeDescription.contains(searchDescription)) {
                return node
            }
        }

        // Check children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByDescriptionRecursive(child, searchDescription)
                if (result != null) return result
            }
        }

        return null
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // VUID Search - Find element by regenerating VUID from accessibility tree
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a node by searching the accessibility tree and matching VUIDs.
     *
     * VUIDs are deterministic hashes generated from:
     * - Package name (first 6 hex chars)
     * - Type code (single char for element type)
     * - Element hash (8 hex chars from resourceId/contentDescription/text/bounds)
     *
     * This method regenerates VUIDs for visible elements and finds the match.
     *
     * @param targetVuid The VUID to search for
     * @return AccessibilityNodeInfo if found, null otherwise
     */
    private fun findNodeByVuidSearch(targetVuid: String): AccessibilityNodeInfo? {
        val service = accessibilityServiceProvider() ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        val packageName = rootNode.packageName?.toString() ?: return null

        return findNodeByVuidRecursive(rootNode, targetVuid, packageName)
    }

    /**
     * Recursively search for node matching the target VUID.
     */
    private fun findNodeByVuidRecursive(
        node: AccessibilityNodeInfo,
        targetVuid: String,
        packageName: String
    ): AccessibilityNodeInfo? {
        // Only check actionable elements (same filter as command generation)
        if (node.isClickable || node.isLongClickable || node.isScrollable) {
            // Generate VUID for this node using same algorithm as CommandGenerator
            val nodeVuid = generateVuidForNode(node, packageName)
            if (nodeVuid == targetVuid) {
                return node
            }
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByVuidRecursive(child, targetVuid, packageName)
                if (result != null) return result
            }
        }

        return null
    }

    /**
     * Generate VUID for an AccessibilityNodeInfo using same algorithm as CommandGenerator.
     *
     * VUID format: {pkgHash6}-{typeCode}{hash8}
     * Example: a3f2e1-b917cc9dc
     */
    private fun generateVuidForNode(node: AccessibilityNodeInfo, packageName: String): String {
        val className = node.className?.toString() ?: ""
        val resourceId = node.viewIdResourceName ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""
        val rect = android.graphics.Rect()
        node.getBoundsInScreen(rect)
        val bounds = "${rect.left},${rect.top},${rect.right},${rect.bottom}"

        // Get type code from class name (same as VUIDGenerator.getTypeCode)
        val typeCode = VUIDGenerator.getTypeCode(className)

        // Create element hash from most stable identifier (same priority as CommandGenerator)
        val elementHash = when {
            resourceId.isNotBlank() -> resourceId
            contentDescription.isNotBlank() -> contentDescription
            text.isNotBlank() -> text
            else -> "$className:$bounds"
        }

        return VUIDGenerator.generate(
            packageName = packageName,
            typeCode = typeCode,
            elementHash = elementHash
        )
    }
}
