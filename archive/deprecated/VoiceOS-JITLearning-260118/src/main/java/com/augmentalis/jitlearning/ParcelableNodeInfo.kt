/**
 * ParcelableNodeInfo.kt - Parcelable wrapper for AccessibilityNodeInfo
 *
 * Parcelable data class representing accessibility node information.
 * Used to pass node data across process boundaries via AIDL.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.jitlearning

import android.graphics.Rect
import android.os.Parcelable
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.parcelize.Parcelize

/**
 * Parcelable Node Info
 *
 * Serializable representation of AccessibilityNodeInfo for IPC.
 * Contains all relevant properties for UI element identification.
 *
 * ## Why This Exists
 *
 * AccessibilityNodeInfo cannot be directly passed via AIDL:
 * 1. It holds native resources that don't serialize
 * 2. It must be recycled (lifecycle issues)
 * 3. It's tied to a specific process
 *
 * ParcelableNodeInfo extracts the relevant data for safe IPC transfer.
 *
 * @property className Android class name (e.g., "android.widget.Button")
 * @property text Visible text content
 * @property contentDescription Accessibility description
 * @property resourceId View resource ID (e.g., "com.app:id/button")
 * @property boundsLeft Screen bounds left coordinate
 * @property boundsTop Screen bounds top coordinate
 * @property boundsRight Screen bounds right coordinate
 * @property boundsBottom Screen bounds bottom coordinate
 * @property isClickable Whether element is clickable
 * @property isLongClickable Whether element supports long click
 * @property isEnabled Whether element is enabled
 * @property isPassword Whether element is a password field
 * @property isScrollable Whether element is scrollable
 * @property isEditable Whether element is editable
 * @property isCheckable Whether element is checkable
 * @property isChecked Whether element is checked
 * @property isFocusable Whether element is focusable
 * @property children Child nodes (for tree structure)
 * @property uuid Assigned UUID (if registered)
 * @property depth Depth in view hierarchy
 */
@Parcelize
data class ParcelableNodeInfo(
    val className: String,
    val text: String,
    val contentDescription: String,
    val resourceId: String,
    val boundsLeft: Int,
    val boundsTop: Int,
    val boundsRight: Int,
    val boundsBottom: Int,
    val isClickable: Boolean,
    val isLongClickable: Boolean,
    val isEnabled: Boolean,
    val isPassword: Boolean,
    val isScrollable: Boolean,
    val isEditable: Boolean,
    val isCheckable: Boolean,
    val isChecked: Boolean,
    val isFocusable: Boolean,
    val children: List<ParcelableNodeInfo> = emptyList(),
    val uuid: String = "",
    val depth: Int = 0
) : Parcelable {

    /**
     * Get bounds as Rect.
     */
    fun getBounds(): Rect {
        return Rect(boundsLeft, boundsTop, boundsRight, boundsBottom)
    }

    /**
     * Get display name (text or contentDescription).
     */
    fun getDisplayName(): String {
        return when {
            text.isNotBlank() -> text
            contentDescription.isNotBlank() -> contentDescription
            resourceId.isNotBlank() -> resourceId.substringAfterLast("/")
            else -> "Unknown"
        }
    }

    /**
     * Get short class name (without package).
     */
    fun getShortClassName(): String {
        return className.substringAfterLast(".")
    }

    /**
     * Generate stable ID for tracking.
     */
    fun stableId(): String = when {
        resourceId.isNotEmpty() -> "res:$resourceId"
        text.isNotEmpty() -> "txt:$className|$text"
        contentDescription.isNotEmpty() -> "cd:$className|$contentDescription"
        else -> "pos:$className|${(boundsLeft + boundsRight) / 2}:${(boundsTop + boundsBottom) / 2}"
    }

    /**
     * Check if this is an input field.
     */
    fun isInputField(): Boolean {
        return className.contains("EditText", ignoreCase = true) ||
               className.contains("TextField", ignoreCase = true) ||
               isEditable
    }

    /**
     * Check if this is a button.
     */
    fun isButton(): Boolean {
        return className.contains("Button", ignoreCase = true)
    }

    /**
     * Get total node count (including children).
     */
    fun getTotalNodeCount(): Int {
        return 1 + children.sumOf { it.getTotalNodeCount() }
    }

    /**
     * Convert to ELM IPC line.
     *
     * Format: ELM:uuid:label:type:actions:bounds:category
     */
    fun toElmLine(assignedUuid: String? = null, category: String = "UNK"): String {
        val id = assignedUuid ?: uuid.ifEmpty { stableId().take(20) }
        val label = getDisplayName().take(30).replace(":", "_")
        val type = getShortClassName()
        val actions = buildActionString()
        val bounds = "$boundsLeft,$boundsTop,$boundsRight,$boundsBottom"
        return "ELM:$id:$label:$type:$actions:$bounds:$category"
    }

    /**
     * Build action string for ELM line.
     */
    private fun buildActionString(): String {
        val actions = mutableListOf<String>()
        if (isClickable) actions.add("click")
        if (isLongClickable) actions.add("longClick")
        if (isEditable) actions.add("edit")
        if (isScrollable) actions.add("scroll")
        if (isCheckable) actions.add("check")
        return if (actions.isEmpty()) "none" else actions.joinToString("+")
    }

    companion object {
        /**
         * Create from AccessibilityNodeInfo.
         *
         * Extracts all relevant properties and optionally recurses to children.
         *
         * @param node AccessibilityNodeInfo to convert
         * @param includeChildren Whether to include child nodes
         * @param maxDepth Maximum recursion depth
         * @param currentDepth Current depth (for internal use)
         * @return ParcelableNodeInfo
         */
        fun fromAccessibilityNode(
            node: AccessibilityNodeInfo,
            includeChildren: Boolean = false,
            maxDepth: Int = 5,
            currentDepth: Int = 0
        ): ParcelableNodeInfo {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            val children = if (includeChildren && currentDepth < maxDepth) {
                (0 until node.childCount).mapNotNull { index ->
                    node.getChild(index)?.let { child ->
                        try {
                            fromAccessibilityNode(child, true, maxDepth, currentDepth + 1)
                        } finally {
                            // Recycle on older API levels
                            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                @Suppress("DEPRECATION")
                                child.recycle()
                            }
                        }
                    }
                }
            } else {
                emptyList()
            }

            return ParcelableNodeInfo(
                className = node.className?.toString() ?: "",
                text = node.text?.toString() ?: "",
                contentDescription = node.contentDescription?.toString() ?: "",
                resourceId = node.viewIdResourceName ?: "",
                boundsLeft = bounds.left,
                boundsTop = bounds.top,
                boundsRight = bounds.right,
                boundsBottom = bounds.bottom,
                isClickable = node.isClickable,
                isLongClickable = node.isLongClickable,
                isEnabled = node.isEnabled,
                isPassword = node.isPassword,
                isScrollable = node.isScrollable,
                isEditable = node.isEditable,
                isCheckable = node.isCheckable,
                isChecked = node.isChecked,
                isFocusable = node.isFocusable,
                children = children,
                depth = currentDepth
            )
        }

        /**
         * Create from ELM IPC line.
         *
         * @param line ELM IPC line
         * @return ParcelableNodeInfo or null if invalid
         */
        fun fromElmLine(line: String): ParcelableNodeInfo? {
            if (!line.startsWith("ELM:")) return null
            val parts = line.split(":")
            if (parts.size < 7) return null

            val uuid = parts[1]
            val label = parts[2].replace("_", " ")
            val type = parts[3]
            val actions = parts[4].split("+").toSet()
            val boundsParts = parts[5].split(",")
            val bounds = if (boundsParts.size == 4) {
                listOf(
                    boundsParts[0].toIntOrNull() ?: 0,
                    boundsParts[1].toIntOrNull() ?: 0,
                    boundsParts[2].toIntOrNull() ?: 0,
                    boundsParts[3].toIntOrNull() ?: 0
                )
            } else {
                listOf(0, 0, 0, 0)
            }

            return ParcelableNodeInfo(
                className = "android.widget.$type",
                text = label,
                contentDescription = "",
                resourceId = "",
                boundsLeft = bounds[0],
                boundsTop = bounds[1],
                boundsRight = bounds[2],
                boundsBottom = bounds[3],
                isClickable = actions.contains("click"),
                isLongClickable = actions.contains("longClick"),
                isEnabled = true,
                isPassword = false,
                isScrollable = actions.contains("scroll"),
                isEditable = actions.contains("edit"),
                isCheckable = actions.contains("check"),
                isChecked = false,
                isFocusable = false,
                uuid = uuid
            )
        }
    }
}
