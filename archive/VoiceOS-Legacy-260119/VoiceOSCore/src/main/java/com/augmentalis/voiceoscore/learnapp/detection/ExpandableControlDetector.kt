package com.augmentalis.voiceoscore.learnapp.detection

import android.content.Context
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings

/**
 * Detects UI controls that contain hidden child elements requiring expansion.
 *
 * Many Android UI elements hide content until the user interacts with them:
 * - **Dropdown menus** (Spinner) - Options appear in overlay after clicking
 * - **Overflow menus** (⋮ button) - Menu items hidden until tapped
 * - **Hamburger menus** (☰ icon) - Navigation drawer slides in
 * - **Accordion lists** - Sections collapsed by default
 * - **Tab layouts** - Content in inactive tabs not visible
 *
 * ## Problem This Solves (Issue #1 - Remaining)
 *
 * **Before Phase 2:**
 * ```
 * Teams App - Settings Button
 *   ↓ Click
 * Settings Menu appears (overlay window)
 *   - Profile
 *   - Notifications
 *   - Privacy          } NEVER DISCOVERED
 *   - Help
 *   - Logout
 *
 * Result: Marked "complete" with only 1 element when actually has 6
 * ```
 *
 * **After Phase 2:**
 * ```
 * ExpandableControlDetector identifies: "Settings button is expandable"
 * ExplorationEngine:
 *   1. Clicks button
 *   2. Waits 500ms for animation
 *   3. Detects new overlay window
 *   4. Explores 5 menu items
 *   5. Registers all elements
 *
 * Result: Accurate discovery of all 6 elements
 * ```
 *
 * ## Detection Strategy
 *
 * Uses multiple heuristics in priority order:
 * 1. **Class name patterns** (highest confidence)
 * 2. **Resource ID patterns** (high confidence)
 * 3. **Content description patterns** (medium confidence)
 *
 * ## Usage Example
 *
 * ```kotlin
 * val detector = ExpandableControlDetector()
 *
 * for (element in clickableElements) {
 *     if (detector.isExpandableControl(element.node)) {
 *         Log.d(TAG, "📋 Expandable: ${element.alias}")
 *
 *         // Special handling for expansion
 *         val expansion = detector.classifyExpansionType(element.node)
 *         handleExpansion(element, expansion)
 *     } else {
 *         // Regular click
 *         clickElement(element)
 *     }
 * }
 * ```
 *
 * ## Thread Safety
 * This class is stateless and thread-safe. All methods are pure functions.
 *
 * @see com.augmentalis.voiceoscore.learnapp.exploration.ExplorationEngine Primary consumer
 */
class ExpandableControlDetector(
    private val context: Context
) {

    private val TAG = "ExpandableControlDetector"

    /**
     * Developer settings
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    /**
     * Types of expandable controls with different expansion behaviors.
     */
    enum class ExpansionType {
        /**
         * Creates a new overlay window (most common).
         * Examples: Spinner, Overflow menu, Dialog picker
         * Detection: New AccessibilityWindowInfo appears
         */
        OVERLAY,

        /**
         * Expands in-place within the same window.
         * Examples: Accordion list, ExpandableListView
         * Detection: Element count increases in same window
         */
        IN_PLACE,

        /**
         * Opens a new screen/activity.
         * Examples: Navigation drawer with screen transition
         * Detection: Package stays same, but screen hash changes significantly
         */
        NAVIGATION,

        /**
         * Unknown/ambiguous expansion behavior.
         * Use conservative approach (check both overlay and in-place)
         */
        UNKNOWN
    }

    /**
     * Detailed information about an expandable control.
     *
     * @property isExpandable Whether this control is expandable
     * @property expansionType Type of expansion behavior
     * @property confidence Confidence level (0.0 to 1.0)
     * @property matchedPattern Which detection pattern matched
     * @property reason Human-readable explanation of why it's expandable
     */
    data class ExpansionInfo(
        val isExpandable: Boolean,
        val expansionType: ExpansionType = ExpansionType.UNKNOWN,
        val confidence: Float = 0.0f,
        val matchedPattern: String = "",
        val reason: String = ""
    ) {
        companion object {
            fun notExpandable() = ExpansionInfo(
                isExpandable = false,
                confidence = 0.0f,
                reason = "No expandable patterns matched"
            )
        }
    }

    /**
     * Checks if an element is an expandable control (simple boolean check).
     *
     * This is a convenience method for quick checks. Use [getExpansionInfo]
     * for detailed information about expansion behavior.
     *
     * @param node AccessibilityNodeInfo to check
     * @return true if expandable, false otherwise
     */
    fun isExpandableControl(node: AccessibilityNodeInfo?): Boolean {
        if (node == null) return false
        return getExpansionInfo(node).isExpandable
    }

    /**
     * Gets detailed information about whether and how an element is expandable.
     *
     * This method performs comprehensive pattern matching and returns detailed
     * information useful for implementing expansion strategies.
     *
     * ## Detection Patterns (Priority Order)
     *
     * ### 1. Class Name Patterns (Confidence: 0.9+)
     * - `android.widget.Spinner` → OVERLAY
     * - `androidx.appcompat.widget.AppCompatSpinner` → OVERLAY
     * - `android.widget.ExpandableListView` → IN_PLACE
     * - `androidx.drawerlayout.widget.DrawerLayout` → NAVIGATION
     *
     * ### 2. Resource ID Patterns (Confidence: 0.8+)
     * - Contains "overflow" → OVERLAY
     * - Contains "menu" → OVERLAY
     * - Contains "dropdown" → OVERLAY
     * - Contains "expand" → IN_PLACE
     * - Contains "drawer" → NAVIGATION
     *
     * ### 3. Content Description Patterns (Confidence: 0.7+)
     * - Contains "menu" → OVERLAY
     * - Contains "more options" → OVERLAY
     * - Contains "expand" → IN_PLACE
     * - Contains "open navigation" → NAVIGATION
     *
     * @param node AccessibilityNodeInfo to analyze
     * @return ExpansionInfo with detailed classification
     */
    fun getExpansionInfo(node: AccessibilityNodeInfo): ExpansionInfo {
        // Pattern 1: Class name matching (highest confidence)
        val className = node.className?.toString()?.lowercase() ?: ""

        // Spinners (dropdowns) - very high confidence
        if (className.contains("spinner")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.OVERLAY,
                confidence = 0.95f,
                matchedPattern = "CLASS:Spinner",
                reason = "Android Spinner class detected (dropdown menu)"
            )
        }

        // ExpandableListView - very high confidence
        if (className.contains("expandablelistview")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.IN_PLACE,
                confidence = 0.95f,
                matchedPattern = "CLASS:ExpandableListView",
                reason = "ExpandableListView class detected (accordion list)"
            )
        }

        // DrawerLayout - high confidence
        if (className.contains("drawerlayout")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.NAVIGATION,
                confidence = 0.90f,
                matchedPattern = "CLASS:DrawerLayout",
                reason = "DrawerLayout class detected (navigation drawer)"
            )
        }

        // Pattern 2: Resource ID matching (high confidence)
        val resourceId = try {
            node.viewIdResourceName?.lowercase() ?: ""
        } catch (e: Exception) {
            ""
        }

        if (resourceId.contains("overflow") || resourceId.contains("more_options")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.OVERLAY,
                confidence = 0.85f,
                matchedPattern = "RESOURCE_ID:overflow",
                reason = "Overflow menu resource ID detected"
            )
        }

        if (resourceId.contains("menu") && !resourceId.contains("item")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.OVERLAY,
                confidence = 0.85f,
                matchedPattern = "RESOURCE_ID:menu",
                reason = "Menu resource ID detected"
            )
        }

        if (resourceId.contains("dropdown") || resourceId.contains("spinner")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.OVERLAY,
                confidence = 0.85f,
                matchedPattern = "RESOURCE_ID:dropdown",
                reason = "Dropdown resource ID detected"
            )
        }

        if (resourceId.contains("expand") || resourceId.contains("collapse")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.IN_PLACE,
                confidence = 0.80f,
                matchedPattern = "RESOURCE_ID:expand",
                reason = "Expandable control resource ID detected"
            )
        }

        if (resourceId.contains("drawer")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.NAVIGATION,
                confidence = 0.80f,
                matchedPattern = "RESOURCE_ID:drawer",
                reason = "Drawer resource ID detected"
            )
        }

        // Pattern 3: Content description matching (medium confidence)
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        if (contentDesc.contains("menu") || contentDesc.contains("more options")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.OVERLAY,
                confidence = 0.75f,
                matchedPattern = "CONTENT_DESC:menu",
                reason = "Menu content description detected"
            )
        }

        if (contentDesc.contains("expand") || contentDesc.contains("show more")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.IN_PLACE,
                confidence = 0.70f,
                matchedPattern = "CONTENT_DESC:expand",
                reason = "Expand content description detected"
            )
        }

        if (contentDesc.contains("open navigation") || contentDesc.contains("open drawer")) {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.NAVIGATION,
                confidence = 0.70f,
                matchedPattern = "CONTENT_DESC:navigation",
                reason = "Navigation content description detected"
            )
        }

        // Pattern 4: Text content matching (lower confidence, fallback)
        val text = node.text?.toString()?.lowercase() ?: ""

        if (text == "⋮" || text == "..." || text == "•••") {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.OVERLAY,
                confidence = 0.65f,
                matchedPattern = "TEXT:overflow_icon",
                reason = "Overflow menu icon detected in text"
            )
        }

        if (text == "☰" || text == "≡") {
            return ExpansionInfo(
                isExpandable = true,
                expansionType = ExpansionType.NAVIGATION,
                confidence = 0.65f,
                matchedPattern = "TEXT:hamburger_icon",
                reason = "Hamburger menu icon detected in text"
            )
        }

        // No patterns matched
        return ExpansionInfo.notExpandable()
    }

    /**
     * Gets diagnostic information about why an element was classified as expandable (or not).
     *
     * Useful for debugging pattern matching and understanding false positives/negatives.
     *
     * @param node AccessibilityNodeInfo to diagnose
     * @return Map containing all available information
     */
    fun getDiagnostics(node: AccessibilityNodeInfo): Map<String, Any> {
        val info = getExpansionInfo(node)
        return mapOf(
            "isExpandable" to info.isExpandable,
            "expansionType" to info.expansionType.name,
            "confidence" to info.confidence,
            "matchedPattern" to info.matchedPattern,
            "reason" to info.reason,
            "className" to (node.className?.toString() ?: "null"),
            "resourceId" to (try { node.viewIdResourceName ?: "null" } catch (e: Exception) { "error" }),
            "contentDescription" to (node.contentDescription?.toString() ?: "null"),
            "text" to (node.text?.toString() ?: "null")
        )
    }

    /**
     * Get expansion wait time from settings
     *
     * This accounts for:
     * - Overlay window animation (200-500ms typical)
     * - In-place expansion animation (200-400ms typical)
     * - Slow devices (RealWear: 500-800ms)
     */
    fun getExpansionWaitMs(): Long = developerSettings.getExpansionWaitDelayMs()

    /**
     * Get minimum confidence threshold from settings
     *
     * Elements with confidence below this threshold are treated as regular clickable elements.
     */
    fun getMinConfidenceThreshold(): Float = developerSettings.getExpansionConfidenceThreshold()

    companion object {
        /**
         * Wait time after clicking expandable control before checking for expansion.
         *
         * This accounts for:
         * - Overlay window animation (200-500ms typical)
         * - In-place expansion animation (200-400ms typical)
         * - Slow devices (RealWear: 500-800ms)
         *
         * ## Tuning Guidance
         * - Fast devices (Pixel): 300ms may suffice
         * - Average devices: 500ms (default)
         * - Slow devices (RealWear): 700ms may be needed
         * - System animations disabled: 100ms suffices
         *
         * ## Future Enhancement
         * TODO: Make adaptive based on:
         * - System animation scale settings
         * - Device performance characteristics
         * - Historical expansion timing data
         *
         * @deprecated Use getExpansionWaitMs() instance method instead
         */
        @Deprecated("Use getExpansionWaitMs() instance method", ReplaceWith("getExpansionWaitMs()"))
        const val EXPANSION_WAIT_MS = 500L

        /**
         * Minimum confidence threshold for treating element as expandable.
         *
         * Elements with confidence below this threshold are treated as regular clickable elements.
         * Current threshold (0.65) allows text-based icon detection while filtering noise.
         *
         * @deprecated Use getMinConfidenceThreshold() instance method instead
         */
        @Deprecated("Use getMinConfidenceThreshold() instance method", ReplaceWith("getMinConfidenceThreshold()"))
        const val MIN_CONFIDENCE_THRESHOLD = 0.65f
    }
}
