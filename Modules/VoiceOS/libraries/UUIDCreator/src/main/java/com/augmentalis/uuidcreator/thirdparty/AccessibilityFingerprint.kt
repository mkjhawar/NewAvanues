/**
 * AccessibilityFingerprint.kt - Stable fingerprint extraction from AccessibilityNodeInfo
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/uuidcreator/thirdparty/AccessibilityFingerprint.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Extracts stable, deterministic fingerprints from accessibility nodes for UUID generation
 */

package com.augmentalis.uuidcreator.thirdparty

import android.view.accessibility.AccessibilityNodeInfo
import java.security.MessageDigest

/**
 * Accessibility Fingerprint - Deterministic identifier for UI elements
 *
 * Extracts stable properties from AccessibilityNodeInfo to create deterministic
 * fingerprints for UUID generation. The same UI element should produce the same
 * fingerprint across app sessions (when app version unchanged).
 *
 * ## Fingerprint Components
 *
 * 1. **Resource ID** - Android resource identifier (e.g., "com.instagram:id/action_bar_button_action")
 * 2. **Class Name** - UI component class (e.g., "android.widget.Button")
 * 3. **Text** - Visible text content
 * 4. **Content Description** - Accessibility description
 * 5. **Hierarchy Path** - Position in UI tree (e.g., "/0/1/3")
 * 6. **Package Name** - App package (e.g., "com.instagram.android")
 * 7. **App Version** - Version code for namespace isolation
 *
 * ## Stability Guarantees
 *
 * - **Stable**: Resource ID, hierarchy path (if layout unchanged)
 * - **Semi-stable**: Text, content description (may change with localization)
 * - **Version-scoped**: Different app versions = different UUIDs
 *
 * ## Usage Example
 *
 * ```kotlin
 * val node: AccessibilityNodeInfo = ...
 * val fingerprint = AccessibilityFingerprint.fromNode(
 *     node = node,
 *     packageName = "com.instagram.android",
 *     appVersion = "12.0.0"
 * )
 *
 * // Generate deterministic hash
 * val hash = fingerprint.generateHash()
 *
 * // Format as UUID
 * val uuid = "com.instagram.android.v12.0.0.button-$hash"
 * ```
 *
 * @property resourceId Android resource identifier (nullable)
 * @property className UI component class name
 * @property text Visible text content (nullable)
 * @property contentDescription Accessibility description (nullable)
 * @property hierarchyPath Path in UI tree (e.g., "/0/1/3")
 * @property packageName App package name
 * @property appVersion App version string
 * @property viewIdHash Hash of view ID resource name (for additional uniqueness)
 *
 * @since 1.0.0
 */
data class AccessibilityFingerprint(
    val resourceId: String?,
    val className: String?,
    val text: String?,
    val contentDescription: String?,
    val hierarchyPath: String,
    val packageName: String,
    val appVersion: String,
    val viewIdHash: String? = null,
    val bounds: String? = null,  // Bounds as fallback (less stable)
    val isClickable: Boolean = false,
    val isEnabled: Boolean = true
) {

    companion object {
        /**
         * Create fingerprint from AccessibilityNodeInfo
         *
         * Extracts all relevant properties from the accessibility node to create
         * a stable, deterministic fingerprint.
         *
         * @param node AccessibilityNodeInfo to fingerprint
         * @param packageName App package name
         * @param appVersion App version string
         * @param calculateHierarchyPath Function to calculate hierarchy path (default provided)
         * @return AccessibilityFingerprint
         */
        fun fromNode(
            node: AccessibilityNodeInfo,
            packageName: String,
            appVersion: String,
            calculateHierarchyPath: ((AccessibilityNodeInfo) -> String)? = null
        ): AccessibilityFingerprint {
            return AccessibilityFingerprint(
                resourceId = node.viewIdResourceName?.toString(),
                className = node.className?.toString(),
                text = node.text?.toString()?.take(100), // Limit text length
                contentDescription = node.contentDescription?.toString()?.take(100),
                hierarchyPath = calculateHierarchyPath?.invoke(node)
                    ?: calculateDefaultHierarchyPath(node),
                packageName = packageName,
                appVersion = appVersion,
                viewIdHash = node.viewIdResourceName?.let { hashString(it.toString()) },
                bounds = node.getBoundsInScreen(),
                isClickable = node.isClickable,
                isEnabled = node.isEnabled
            )
        }

        /**
         * Calculate default hierarchy path
         *
         * Walks up the accessibility tree to build path string.
         * Format: "/parent_index/child_index/grandchild_index"
         *
         * Example: "/0/1/3" means root's 1st child's 2nd child's 4th child
         *
         * @param node Accessibility node
         * @return Hierarchy path string
         */
        private fun calculateDefaultHierarchyPath(node: AccessibilityNodeInfo): String {
            val path = mutableListOf<Int>()
            var current: AccessibilityNodeInfo? = node
<<<<<<< HEAD
            val nodesToRecycle = mutableListOf<AccessibilityNodeInfo>()

            try {
                // Walk up tree, collecting indices
                while (current != null) {
                    val parent = current.parent
                    if (parent != null) {
                        // Find current's index in parent
                        val index = findChildIndex(parent, current)
                        if (index >= 0) {
                            path.add(0, index) // Prepend to build path from root
                        }
                        nodesToRecycle.add(parent)
                        current = parent
                    } else {
                        break
                    }
                }

                return "/" + path.joinToString("/")
            } finally {
                // Always recycle nodes to prevent memory leaks
                nodesToRecycle.forEach { it.recycle() }
            }
=======

            // Walk up tree, collecting indices
            while (current != null) {
                val parent = current.parent
                if (parent != null) {
                    // Find current's index in parent
                    val index = findChildIndex(parent, current)
                    if (index >= 0) {
                        path.add(0, index) // Prepend to build path from root
                    }
                    current = parent
                } else {
                    break
                }
            }

            return "/" + path.joinToString("/")
>>>>>>> AVA-Development
        }

        /**
         * Find child index in parent
         *
         * @param parent Parent node
         * @param child Child node to find
         * @return Index of child, or -1 if not found
         */
        private fun findChildIndex(
            parent: AccessibilityNodeInfo,
            child: AccessibilityNodeInfo
        ): Int {
            for (i in 0 until parent.childCount) {
                val currentChild = parent.getChild(i)
<<<<<<< HEAD
                try {
                    if (currentChild != null && currentChild == child) {
                        return i
                    }
                } finally {
                    // Always recycle child node to prevent memory leaks
                    currentChild?.recycle()
=======
                if (currentChild != null && currentChild == child) {
                    return i
>>>>>>> AVA-Development
                }
            }
            return -1
        }

        /**
         * Get bounds in screen as string
         *
         * @return Bounds string (e.g., "100,200,300,400") or null
         */
        private fun AccessibilityNodeInfo.getBoundsInScreen(): String? {
            return try {
                val rect = android.graphics.Rect()
                this.getBoundsInScreen(rect)
                "${rect.left},${rect.top},${rect.right},${rect.bottom}"
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Hash string using SHA-256
         *
         * @param input String to hash
         * @return Hex string of hash (first 16 characters)
         */
        private fun hashString(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }.take(16)
        }
    }

    /**
     * Generate deterministic hash from fingerprint
     *
     * Combines all fingerprint properties and generates SHA-256 hash.
     * Same fingerprint = same hash = same UUID.
     *
     * ## Hash Components Priority
     *
     * 1. **Resource ID** (highest priority - most stable)
     * 2. **Hierarchy Path** (structural position)
     * 3. **Class Name** (component type)
     * 4. **Text + Content Description** (content-based)
     * 5. **Package + Version** (namespace)
     *
     * @return 12-character hex hash string
     */
    fun generateHash(): String {
        // Build canonical string representation
        val components = buildList {
            // Most stable components first
            add("pkg:$packageName")
            add("ver:$appVersion")

            resourceId?.let { add("res:$it") }
            className?.let { add("cls:$it") }
            add("path:$hierarchyPath")

            // Content-based (less stable, but useful fallback)
            text?.let { add("txt:$it") }
            contentDescription?.let { add("desc:$it") }

            // View ID hash for additional uniqueness
            viewIdHash?.let { add("vid:$it") }

            // Flags
            add("click:$isClickable")
            add("enabled:$isEnabled")
        }

        val canonical = components.joinToString("|")

        // SHA-256 hash
        val bytes = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        val hex = bytes.joinToString("") { "%02x".format(it) }

        // Return first 12 characters for compact UUID
        return hex.take(12)
    }

    /**
     * Serialize fingerprint to string
     *
     * Creates human-readable representation for debugging.
     *
     * @return Serialized fingerprint string
     */
    fun serialize(): String {
        return buildString {
            append("AccessibilityFingerprint(")
            append("pkg=$packageName, ")
            append("ver=$appVersion, ")
            append("res=$resourceId, ")
            append("cls=$className, ")
            append("path=$hierarchyPath, ")
            append("txt=$text, ")
            append("desc=$contentDescription")
            append(")")
        }
    }

    /**
     * Calculate stability score (0.0 - 1.0)
     *
     * Estimates how stable this fingerprint is across app sessions.
     * Higher score = more likely to generate same UUID after app restart.
     *
     * ## Scoring
     * - **1.0**: Has resource ID (very stable)
     * - **0.8**: Has hierarchy path + class name (stable)
     * - **0.6**: Has text/content description (semi-stable)
     * - **0.3**: Only has bounds (unstable)
     *
     * @return Stability score (0.0 - 1.0)
     */
    fun calculateStabilityScore(): Float {
        var score = 0f

        // Resource ID is most stable
        if (!resourceId.isNullOrBlank()) {
            score += 0.5f
        }

        // Hierarchy path is fairly stable (unless layout changes)
        if (hierarchyPath.isNotBlank() && hierarchyPath != "/") {
            score += 0.3f
        }

        // Class name helps
        if (!className.isNullOrBlank()) {
            score += 0.1f
        }

        // Text/description is less stable (localization changes)
        if (!text.isNullOrBlank() || !contentDescription.isNullOrBlank()) {
            score += 0.1f
        }

        return score.coerceIn(0f, 1f)
    }

    /**
     * Check if fingerprint is considered stable
     *
     * Stable fingerprints have high likelihood of producing same UUID
     * across app restarts.
     *
     * @return true if stability score >= 0.7
     */
    fun isStable(): Boolean {
        return calculateStabilityScore() >= 0.7f
    }

    /**
     * Get human-readable element type
     *
     * @return Element type (Button, Text, Image, etc.)
     */
    fun getElementType(): String {
        val cls = className ?: return "unknown"

        return when {
            cls.contains("Button", ignoreCase = true) -> "button"
            cls.contains("TextView", ignoreCase = true) -> "text"
            cls.contains("EditText", ignoreCase = true) -> "input"
            cls.contains("ImageView", ignoreCase = true) -> "image"
            cls.contains("ImageButton", ignoreCase = true) -> "imagebutton"
            cls.contains("CheckBox", ignoreCase = true) -> "checkbox"
            cls.contains("RadioButton", ignoreCase = true) -> "radio"
            cls.contains("Switch", ignoreCase = true) -> "switch"
            cls.contains("ViewGroup", ignoreCase = true) -> "container"
            cls.contains("Layout", ignoreCase = true) -> "layout"
            else -> "view"
        }
    }
}
