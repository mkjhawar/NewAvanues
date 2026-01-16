/**
 * AndroidUIExecutor.kt - Android UI executor
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-01-15 - Migrated from VUID to AVID nomenclature
 *
 * Android implementation of UIExecutor using AccessibilityService.
 */
package com.augmentalis.voiceoscoreng.handlers

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscoreng.common.Bounds
import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.ElementFingerprint
import kotlinx.coroutines.delay

/**
 * Android implementation of [UIExecutor].
 *
 * Uses AccessibilityService to perform UI interactions.
 *
 * @param accessibilityServiceProvider Provider for the accessibility service instance
 * @param avidLookup Function to lookup element by AVID fingerprint
 */
class AndroidUIExecutor(
    private val accessibilityServiceProvider: () -> AccessibilityService?,
    private val avidLookup: (String) -> AccessibilityNodeInfo? = { null }
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
    // AVID-based Actions (primary path for dynamic commands)
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun clickByAvid(avid: String): Boolean {
        // Try provided lookup first (if caller has a cache)
        var node = avidLookup(avid)

        // Fallback: search accessibility tree by regenerating AVID fingerprints
        if (node == null) {
            node = findNodeByAvidSearch(avid)
        }

        if (node == null) return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun longClickByAvid(avid: String): Boolean {
        var node = avidLookup(avid)
        if (node == null) {
            node = findNodeByAvidSearch(avid)
        }
        if (node == null) return false
        return node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Text-based Actions (fallback path)
    // ═══════════════════════════════════════════════════════════════════════════

    override suspend fun clickByText(text: String): Boolean {
        val node = findNodeByText(text) ?: findNodeByDescription(text) ?: return false
        return node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    override suspend fun longClickByText(text: String): Boolean {
        val node = findNodeByText(text) ?: findNodeByDescription(text) ?: return false
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
    // AVID Search - Find element by regenerating AVID fingerprint from accessibility tree
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Find a node by searching the accessibility tree and matching AVID fingerprints.
     *
     * AVID fingerprints are deterministic hashes generated from:
     * - TypeCode (3-char code for element type, e.g., BTN, TXT, IMG)
     * - Element hash (8 hex chars from resourceId/contentDescription/text)
     *
     * Format: {TypeCode}:{hash8} (e.g., "BTN:a3f2e1c9")
     *
     * This method regenerates fingerprints for visible elements and finds the match.
     *
     * @param targetAvid The AVID fingerprint to search for
     * @return AccessibilityNodeInfo if found, null otherwise
     */
    private fun findNodeByAvidSearch(targetAvid: String): AccessibilityNodeInfo? {
        val service = accessibilityServiceProvider() ?: return null
        val rootNode = service.rootInActiveWindow ?: return null
        val packageName = rootNode.packageName?.toString() ?: return null

        return findNodeByAvidRecursive(rootNode, targetAvid, packageName)
    }

    /**
     * Recursively search for node matching the target AVID fingerprint.
     */
    private fun findNodeByAvidRecursive(
        node: AccessibilityNodeInfo,
        targetAvid: String,
        packageName: String
    ): AccessibilityNodeInfo? {
        // Only check actionable elements (same filter as command generation)
        if (node.isClickable || node.isLongClickable || node.isScrollable) {
            // Generate AVID fingerprint for this node using same algorithm as CommandGenerator
            val nodeAvid = generateAvidForNode(node, packageName)
            if (nodeAvid == targetAvid) {
                return node
            }
        }

        // Recurse into children
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                val result = findNodeByAvidRecursive(child, targetAvid, packageName)
                if (result != null) return result
            }
        }

        return null
    }

    /**
     * Generate AVID fingerprint for an AccessibilityNodeInfo using ElementFingerprint.
     *
     * Fingerprint format: {TypeCode}:{hash8}
     * Example: BTN:a3f2e1c9
     */
    private fun generateAvidForNode(node: AccessibilityNodeInfo, packageName: String): String {
        val className = node.className?.toString() ?: ""
        val resourceId = node.viewIdResourceName ?: ""
        val contentDescription = node.contentDescription?.toString() ?: ""
        val text = node.text?.toString() ?: ""

        return ElementFingerprint.generate(
            className = className,
            packageName = packageName,
            resourceId = resourceId,
            text = text,
            contentDesc = contentDescription
        )
    }
}
