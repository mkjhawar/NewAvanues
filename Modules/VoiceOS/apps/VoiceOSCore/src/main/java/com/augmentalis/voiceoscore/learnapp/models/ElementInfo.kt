/**
 * ElementInfo.kt - UI element information model
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/models/ElementInfo.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Data model for UI element properties
 */

package com.augmentalis.voiceoscore.learnapp.models

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Exploration Behavior Enum
 *
 * Defines how the exploration engine should interact with UI elements.
 * Elements are classified by their interaction patterns and exploration priority.
 *
 * Priority levels guide exploration order:
 * - Priority 1: Direct interactions (clicks, taps)
 * - Priority 2: Expandable UI (drawers, dropdowns)
 * - Priority 3: Content discovery (scrolling)
 * - Priority 4: Secondary interactions (expand)
 * - Priority 5: Special interactions (long press)
 * - Priority 6: Container exploration
 * - Priority 7: Skip (non-interactive)
 *
 * @since 1.1.0 (Tier 1 Enhancement - 2025-12-04)
 */
enum class ExplorationBehavior(val priority: Int) {
    /** Direct click interaction (Button, MenuItem, etc.) - Priority 1 */
    CLICKABLE(1),

    /** Opens menu, dialog, or overlay (3-dot menu, hamburger) - Priority 1 */
    MENU_TRIGGER(1),

    /** Tab switcher (switches content panes) - Priority 1 */
    TAB(1),

    /** Navigation drawer (swipe from edge) - Priority 2 */
    DRAWER(2),

    /** Dropdown menu (Spinner, AutoComplete) - Priority 2 */
    DROPDOWN(2),

    /** Bottom sheet (swipe up panel) - Priority 2 */
    BOTTOM_SHEET(2),

    /** Scrollable content (ListView, RecyclerView, ScrollView) - Priority 3 */
    SCROLLABLE(3),

    /** Horizontal chip group (scrollable chips) - Priority 3 */
    CHIP_GROUP(3),

    /** Collapsing toolbar (scroll to reveal) - Priority 3 */
    COLLAPSING_TOOLBAR(3),

    /** Expandable list item (expands to show children) - Priority 4 */
    EXPANDABLE(4),

    /** Long press interaction (context menu) - Priority 5 */
    LONG_CLICKABLE(5),

    /** Container with children to explore - Priority 6 */
    CONTAINER(6),

    /** Non-interactive element (decorative) - Priority 7 */
    SKIP(7)
}

/**
 * Element Info
 *
 * Represents complete information about a UI element.
 * Extracted from AccessibilityNodeInfo during exploration.
 *
 * ## Usage Example
 *
 * ```kotlin
 * val element = ElementInfo(
 *     className = "android.widget.Button",
 *     text = "Like",
 *     contentDescription = "Like button",
 *     resourceId = "com.instagram.android:id/like_btn",
 *     isClickable = true,
 *     isEnabled = true,
 *     bounds = Rect(100, 200, 300, 400),
 *     node = accessibilityNode
 * )
 * ```
 *
 * @property className Android class name (e.g., "android.widget.Button")
 * @property text Visible text content
 * @property contentDescription Accessibility content description
 * @property resourceId Resource ID (e.g., "com.app:id/button")
 * @property isClickable Whether element is clickable
 * @property isEnabled Whether element is enabled
 * @property isPassword Whether element is a password field
 * @property isScrollable Whether element is scrollable
 * @property bounds Screen bounds (x, y, width, height)
 * @property node Reference to AccessibilityNodeInfo (for actions)
 * @property uuid Generated UUID (set after registration)
 * @property explorationBehavior How the exploration engine should interact with this element
 *
 * @since 1.0.0
 */
data class ElementInfo(
    val className: String,
    val text: String = "",
    val contentDescription: String = "",
    val resourceId: String = "",
    val isClickable: Boolean = false,
    val isEnabled: Boolean = false,
    val isPassword: Boolean = false,
    val isScrollable: Boolean = false,
    val bounds: Rect = Rect(),
    val node: AccessibilityNodeInfo? = null,
    var uuid: String? = null,
    var classification: String? = null,
    val explorationBehavior: ExplorationBehavior = ExplorationBehavior.SKIP,
    val screenWidth: Int = 0,
    val screenHeight: Int = 0,
    val parent: ElementInfo? = null,
    val children: List<ElementInfo>? = null,
    val index: Int = 0
) {

    /**
     * Get display name (text or contentDescription)
     *
     * @return Human-readable name
     */
    fun getDisplayName(): String {
        return when {
            text.isNotBlank() -> text
            contentDescription.isNotBlank() -> contentDescription
            resourceId.isNotBlank() -> resourceId.substringAfterLast('/')
            else -> "Unknown"
        }
    }

    /**
     * Check if element is EditText field
     *
     * FIX (2025-12-02): Expanded detection to catch Material Design and AppCompat input fields
     * Issue: Google Photos uses TextInputEditText, which wasn't detected
     * Result: Input fields were being clicked, triggering Gboard and consent interruptions
     *
     * @return true if EditText or any text input field variant
     */
    fun isEditText(): Boolean {
        // Detect ALL text input field types (Android, Material, AppCompat, Compose)
        val inputFieldTypes = listOf(
            "EditText",                  // android.widget.EditText
            "TextInputEditText",         // com.google.android.material (Google Photos, Gmail)
            "AppCompatEditText",         // androidx.appcompat.widget
            "AutoCompleteTextView",      // android.widget
            "MultiAutoCompleteTextView", // android.widget
            "TextField"                  // Jetpack Compose
        )
        return inputFieldTypes.any { className.contains(it, ignoreCase = true) }
    }

    /**
     * Check if element is Button
     *
     * @return true if Button
     */
    fun isButton(): Boolean {
        return className.contains("Button", ignoreCase = true)
    }

    /**
     * Check if element is ImageView
     *
     * @return true if ImageView
     */
    fun isImageView(): Boolean {
        return className.contains("ImageView", ignoreCase = true) ||
               className.contains("ImageButton", ignoreCase = true)
    }

    /**
     * Check if element has meaningful content
     *
     * @return true if has text or contentDescription
     */
    fun hasMeaningfulContent(): Boolean {
        return text.isNotBlank() || contentDescription.isNotBlank()
    }

    /**
     * Extract element type string
     *
     * @return Element type (button, text, input, image, etc.)
     */
    fun extractElementType(): String {
        val lowerClassName = className.lowercase()

        return when {
            lowerClassName.contains("button") -> "button"
            lowerClassName.contains("textview") -> "text"
            lowerClassName.contains("edittext") -> "input"
            lowerClassName.contains("imageview") || lowerClassName.contains("imagebutton") -> "image"
            lowerClassName.contains("checkbox") -> "checkbox"
            lowerClassName.contains("radiobutton") -> "radio"
            lowerClassName.contains("switch") -> "switch"
            lowerClassName.contains("seekbar") || lowerClassName.contains("slider") -> "slider"
            lowerClassName.contains("viewgroup") || lowerClassName.contains("layout") -> "container"
            else -> "view"
        }
    }

    /**
     * Create copy without node reference (for serialization)
     *
     * @return Copy without node
     */
    fun withoutNode(): ElementInfo {
        return copy(node = null)
    }

    /**
     * Recycle the AccessibilityNodeInfo reference to prevent memory leaks
     *
     * FIX (2025-12-03): Properly recycle AccessibilityNodeInfo after use
     *
     * AccessibilityNodeInfo holds native resources and MUST be recycled when done.
     * Call this method after using the element (after clicking, after registration, etc.)
     *
     * Note: On Android U+ (API 34+), recycle() is deprecated and automatic,
     * so we only call it on older versions.
     */
    fun recycleNode() {
        node?.let { nodeRef ->
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                nodeRef.recycle()
            }
        }
    }

    override fun toString(): String {
        return """
            ElementInfo:
            - Class: $className
            - Text: $text
            - Description: $contentDescription
            - ResourceId: $resourceId
            - Clickable: $isClickable
            - Enabled: $isEnabled
            - Type: ${extractElementType()}
        """.trimIndent()
    }

    /**
     * Generate a stable identifier for element tracking.
     *
     * Used by Hybrid C-Lite exploration strategy to track which elements
     * have been clicked across fresh scrapes.
     *
     * Priority order (most stable first):
     * 1. resourceId - Most stable, app-defined
     * 2. text + className - Good for labeled UI
     * 3. contentDescription + className - Good for icons
     * 4. bounds center + className - Fallback for unlabeled elements
     *
     * @return Stable identifier string with prefix indicating type
     * @since 2025-12-04 (Hybrid C-Lite)
     */
    fun stableId(): String = when {
        resourceId.isNotEmpty() -> "res:$resourceId"
        text.isNotEmpty() -> "txt:$className|$text"
        contentDescription.isNotEmpty() -> "cd:$className|$contentDescription"
        else -> "pos:$className|${bounds.centerX()}:${bounds.centerY()}"
    }

    /**
     * Calculate stability score for sorting.
     *
     * Used by Hybrid C-Lite exploration strategy to prioritize clicking
     * more stable elements first, maximizing success before UI destabilizes.
     *
     * Higher score = more stable = click first.
     *
     * Scoring:
     * - 100: Has resourceId (most stable, rarely changes)
     * - 80: Has both text and contentDescription
     * - 60: Has text only
     * - 40: Has contentDescription only
     * - 0: Bounds only (least stable, may shift)
     *
     * @return Stability score 0-100
     * @since 2025-12-04 (Hybrid C-Lite)
     */
    fun stabilityScore(): Int = when {
        resourceId.isNotEmpty() -> 100
        text.isNotEmpty() && contentDescription.isNotEmpty() -> 80
        text.isNotEmpty() -> 60
        contentDescription.isNotEmpty() -> 40
        else -> 0
    }

    companion object {
        /**
         * Create ElementInfo from AccessibilityNodeInfo
         *
         * @param node Accessibility node
         * @param classifier Optional element classifier (for exploration behavior)
         * @return ElementInfo
         */
        fun fromNode(
            node: AccessibilityNodeInfo,
            classifier: com.augmentalis.voiceoscore.learnapp.elements.ElementClassifier? = null
        ): ElementInfo {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)

            // Determine exploration behavior if classifier is provided
            val explorationBehavior = classifier?.classifyExplorationBehavior(node)
                ?: ExplorationBehavior.SKIP

            return ElementInfo(
                className = node.className?.toString() ?: "",
                text = node.text?.toString() ?: "",
                contentDescription = node.contentDescription?.toString() ?: "",
                resourceId = node.viewIdResourceName ?: "",
                isClickable = node.isClickable,
                isEnabled = node.isEnabled,
                isPassword = node.isPassword,
                isScrollable = node.isScrollable,
                bounds = bounds,
                node = node,
                explorationBehavior = explorationBehavior
            )
        }
    }
}
