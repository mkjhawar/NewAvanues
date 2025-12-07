/**
 * ElementClassifier.kt - Classifies UI elements for exploration
 * Path: libraries/UUIDCreator/src/main/java/com/augmentalis/learnapp/elements/ElementClassifier.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-08
 *
 * Main classifier for determining which elements are safe to explore
 */

package com.augmentalis.voiceoscore.learnapp.elements

import android.content.Context
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.models.ElementClassification
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.models.ExplorationBehavior
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings

/**
 * Element Classifier
 *
 * Classifies UI elements into categories to determine exploration strategy.
 * Uses dangerous element detector and login screen detector.
 *
 * ## Classification Priority
 *
 * 1. Check if disabled → Disabled
 * 2. Check if EditText → EditText
 * 3. Check if dangerous → Dangerous
 * 4. Check if login field → LoginField
 * 5. Check if non-clickable → NonClickable
 * 6. Otherwise → SafeClickable
 *
 * ## Usage Example
 *
 * ```kotlin
 * val classifier = ElementClassifier()
 *
 * val element = ElementInfo(...)
 * val classification = classifier.classify(element)
 *
 * when (classification) {
 *     is ElementClassification.SafeClickable -> {
 *         // Click and explore
 *     }
 *     is ElementClassification.Dangerous -> {
 *         // Skip
 *         println("Skipped: ${classification.reason}")
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
class ElementClassifier(
    private val context: Context
) {

    /**
     * Developer settings
     */
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    /**
     * Dangerous element detector
     */
    private val dangerousDetector = DangerousElementDetector()

    /**
     * Login screen detector
     */
    private val loginDetector = LoginScreenDetector()

    /**
     * Classify single element
     *
     * AGGRESSIVE MODE: Clicks ALL potentially interactive elements, not just those marked isClickable.
     * This ensures we discover overflow menus, bottom tabs, and other navigation that may not
     * have the clickable flag set correctly.
     *
     * @param element Element to classify
     * @return Classification result
     */
    fun classify(element: ElementInfo): ElementClassification {
        // 1. Check if disabled
        if (!element.isEnabled) {
            return ElementClassification.Disabled(element)
        }

        // 2. Check if EditText (skip text input fields)
        if (element.isEditText()) {
            return ElementClassification.EditText(element)
        }

        // 3. Check if dangerous
        val (isDangerous, reason) = dangerousDetector.isDangerous(element)
        if (isDangerous) {
            return ElementClassification.Dangerous(element, reason)
        }

        // 4. Check if login field
        val loginFieldType = loginDetector.classifyLoginField(element)
        if (loginFieldType != null) {
            return ElementClassification.LoginField(element, loginFieldType)
        }

        // 5. AGGRESSIVE MODE: Check if element is potentially clickable
        // Don't rely solely on isClickable flag - many navigation elements don't set it correctly
        if (!isAggressivelyClickable(element)) {
            return ElementClassification.NonClickable(element)
        }

        // 6. Safe to click
        return ElementClassification.SafeClickable(element)
    }

    /**
     * Classify exploration behavior
     *
     * Determines how the exploration engine should interact with a UI element.
     * Uses element properties and class names to identify interaction patterns.
     *
     * Priority 1 (Direct Interactions):
     * - CLICKABLE: Standard clickable elements
     * - MENU_TRIGGER: Opens menus/overlays (3-dot, hamburger)
     * - TAB: Tab switchers
     *
     * Priority 2 (Expandable UI):
     * - DRAWER: Navigation drawers
     * - DROPDOWN: Dropdowns, spinners
     * - BOTTOM_SHEET: Bottom sheets
     *
     * Priority 3 (Content Discovery):
     * - SCROLLABLE: Scrollable containers
     * - CHIP_GROUP: Horizontal scrollable chips
     * - COLLAPSING_TOOLBAR: Collapsing toolbars
     *
     * Priority 4-7 (Secondary/Special):
     * - EXPANDABLE: Expandable list items
     * - LONG_CLICKABLE: Long press interactions
     * - CONTAINER: Container with children
     * - SKIP: Non-interactive decorative elements
     *
     * @param node AccessibilityNodeInfo to classify
     * @return ExplorationBehavior
     * @since 1.1.0 (Tier 1 Enhancement - 2025-12-04)
     */
    fun classifyExplorationBehavior(node: AccessibilityNodeInfo): ExplorationBehavior {
        val className = node.className?.toString()?.lowercase() ?: ""
        val resourceId = node.viewIdResourceName?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
        val text = node.text?.toString()?.lowercase() ?: ""

        // Priority 1: Menu triggers (3-dot, hamburger, overflow)
        val menuTriggerPatterns = listOf(
            "more_options", "overflow", "menu", "navigation_menu", "hamburger",
            "three_dot", "action_menu", "options_menu"
        )
        if (menuTriggerPatterns.any { resourceId.contains(it) || contentDesc.contains(it) }) {
            return ExplorationBehavior.MENU_TRIGGER
        }

        // Check class name for menu triggers
        if (className.contains("actionmenuview") || className.contains("overflowmenubutton")) {
            return ExplorationBehavior.MENU_TRIGGER
        }

        // Priority 1: Tab switchers
        val tabPatterns = listOf(
            "tab", "tablayout", "bottomnavigationitemview", "navigationbaritemview"
        )
        if (tabPatterns.any { className.contains(it) }) {
            return ExplorationBehavior.TAB
        }

        // Priority 2: Navigation drawer
        if (className.contains("drawerlayout") || className.contains("navigationview")) {
            return ExplorationBehavior.DRAWER
        }

        // Priority 2: Dropdown menus
        val dropdownPatterns = listOf(
            "spinner", "autocompletetextview", "dropdownmenu", "popupmenu"
        )
        if (dropdownPatterns.any { className.contains(it) }) {
            return ExplorationBehavior.DROPDOWN
        }

        // Priority 2: Bottom sheets
        if (className.contains("bottomsheet") || resourceId.contains("bottom_sheet")) {
            return ExplorationBehavior.BOTTOM_SHEET
        }

        // Priority 3: Scrollable containers
        val scrollablePatterns = listOf(
            "recyclerview", "listview", "scrollview", "horizontalscrollview",
            "nestedscrollview", "viewpager", "viewpager2"
        )
        if (scrollablePatterns.any { className.contains(it) }) {
            return ExplorationBehavior.SCROLLABLE
        }

        // Alternative: Check isScrollable flag
        if (node.isScrollable) {
            return ExplorationBehavior.SCROLLABLE
        }

        // Priority 3: Chip groups
        if (className.contains("chipgroup") || (className.contains("chip") && node.childCount > 1)) {
            return ExplorationBehavior.CHIP_GROUP
        }

        // Priority 3: Collapsing toolbar
        if (className.contains("collapsingtoolbarlayout") || className.contains("appbarlayout")) {
            return ExplorationBehavior.COLLAPSING_TOOLBAR
        }

        // Priority 4: Expandable list items
        if (className.contains("expandablelistview") ||
            (node.isClickable && node.childCount > 0 &&
             (resourceId.contains("expand") || contentDesc.contains("expand")))) {
            return ExplorationBehavior.EXPANDABLE
        }

        // Priority 5: Long clickable (but not clickable)
        if (node.isLongClickable && !node.isClickable) {
            return ExplorationBehavior.LONG_CLICKABLE
        }

        // Priority 1: Standard clickable elements (after special patterns)
        if (node.isClickable && node.isEnabled) {
            // Skip edit text fields (dangerous for exploration)
            val element = ElementInfo.fromNode(node)
            if (element.isEditText()) {
                return ExplorationBehavior.SKIP
            }

            // Check if dangerous
            val (isDangerous, _) = dangerousDetector.isDangerous(element)
            if (isDangerous) {
                return ExplorationBehavior.SKIP
            }

            return ExplorationBehavior.CLICKABLE
        }

        // Priority 6: Containers with children (for recursive exploration)
        if (node.childCount > 0) {
            return ExplorationBehavior.CONTAINER
        }

        // Priority 7: Skip everything else (decorative/non-interactive)
        return ExplorationBehavior.SKIP
    }

    /**
     * Aggressive clickability check
     *
     * Determines if element should be clicked in aggressive exploration mode.
     * Includes:
     * - Explicitly clickable elements (isClickable=true)
     * - Bottom navigation tabs
     * - Overflow menu icons (3-dot, hamburger)
     * - Large enough elements (>= 48dp min touch target)
     * - Elements with meaningful metadata (text/contentDescription)
     * - ImageViews (icons often clickable even without flag)
     * - Buttons, tabs, toolbars
     *
     * FIX (2025-11-23): Enhanced bottom nav detection for Google Clock/Calculator
     * - Added more class name patterns (frameLayout, linearLayout with nav keywords)
     * - Detect elements at bottom of screen (Y > 80% height)
     * - Lowered size threshold for elements with content (40dp)
     * - Check for sibling navigation elements
     *
     * @param element Element to check
     * @return true if should be clicked in aggressive mode
     */
    private fun isAggressivelyClickable(element: ElementInfo): Boolean {
        // Already marked clickable
        if (element.isClickable) {
            return true
        }

        val className = element.className.lowercase()
        val text = element.text.lowercase()
        val contentDesc = element.contentDescription.lowercase()

        // Navigation elements (often not marked clickable but ARE clickable)
        val navigationTypes = listOf(
            "bottomnavigationitemview",  // Bottom nav tabs
            "navigationbaritemview",
            "navigationbar",              // Generic navigation bar
            "tabview",
            "tab",
            "actionmenuitemview",        // Overflow menu items
            "toolbar",
            "appbarlayout",
            "tablayout"                   // Material TabLayout
        )

        if (navigationTypes.any { className.contains(it) }) {
            return true
        }

        // FIX: Detect bottom navigation by position and content
        // Many Google apps use FrameLayout or LinearLayout for bottom nav items
        // without setting clickable flag
        val isBottomPositioned = isInBottomRegion(element)
        if (isBottomPositioned && (text.isNotBlank() || contentDesc.isNotBlank())) {
            // Check if content suggests navigation (alarm, timer, clock, etc.)
            val navKeywords = listOf(
                "alarm", "clock", "timer", "stopwatch", "bedtime",
                "home", "search", "profile", "settings", "menu",
                "tab", "button", "navigation"
            )

            if (navKeywords.any { text.contains(it) || contentDesc.contains(it) }) {
                return true
            }

            // Or if it's in bottom region with siblings (likely bottom nav)
            // This is a heuristic - bottom navs typically have 3-5 items
            // We can't check siblings in ElementInfo, but we can check bounds
            val width = element.bounds.width()
            val height = element.bounds.height()

            // If width is narrow (< 25% screen width) and has content,
            // likely a bottom nav item
            if (width < element.bounds.right / 4 && (text.isNotBlank() || contentDesc.isNotBlank())) {
                return true
            }
        }

        // Icon types (click to see if they're buttons)
        val iconTypes = listOf(
            "imageview",
            "imagebutton",
            "iconview"
        )

        if (iconTypes.any { className.contains(it) }) {
            // Only click icons if they have description or are large enough
            if (element.contentDescription.isNotBlank()) {
                return true
            }

            // Check if icon is large enough to be clickable (>= minimum touch target)
            val bounds = element.bounds
            val width = bounds.width()
            val height = bounds.height()
            val minTouchTarget = developerSettings.getMinTouchTargetSizePixels()

            if (width >= minTouchTarget && height >= minTouchTarget) {
                return true
            }
        }

        // Button types (should always be clickable)
        val buttonTypes = listOf(
            "button",
            "floatingactionbutton",
            "fab"
        )

        if (buttonTypes.any { className.contains(it) }) {
            return true
        }

        // Elements with meaningful content (likely interactive)
        if (element.text.isNotBlank() || element.contentDescription.isNotBlank()) {
            // Check if element is large enough to be a button
            val bounds = element.bounds
            val width = bounds.width()
            val height = bounds.height()
            val minTouchTarget = developerSettings.getMinTouchTargetSizePixels()

            if (width >= minTouchTarget && height >= minTouchTarget) {
                // Exclude pure text containers (TextViews with no click behavior)
                if (!className.contains("textview") || className.contains("button")) {
                    return true
                }
            }
        }

        // Default: not clickable
        return false
    }

    /**
     * Check if element is in bottom region of screen
     *
     * FIX (2025-11-23): Helper for detecting bottom navigation bars
     *
     * Bottom region is defined as Y position > 80% of screen height
     * This helps identify bottom nav bars that aren't marked clickable
     *
     * @param element Element to check
     * @return true if in bottom 20% of screen
     */
    private fun isInBottomRegion(element: ElementInfo): Boolean {
        val bounds = element.bounds
        // Approximate screen height from element's parent bounds
        // Since we don't have direct access to screen metrics in ElementInfo,
        // use heuristic based on configurable threshold
        val estimatedBottomThreshold = developerSettings.getBottomScreenRegionThreshold()

        return bounds.top >= estimatedBottomThreshold
    }

    /**
     * Classify multiple elements
     *
     * @param elements List of elements
     * @return List of classifications
     */
    fun classifyAll(elements: List<ElementInfo>): List<ElementClassification> {
        return elements.map { classify(it) }
    }

    /**
     * Check if screen is login screen
     *
     * Convenience method delegating to loginDetector.
     *
     * @param elements All elements on screen
     * @return true if login screen
     */
    fun isLoginScreen(elements: List<ElementInfo>): Boolean {
        return loginDetector.isLoginScreen(elements)
    }

    /**
     * Filter to safe clickable elements only
     *
     * @param elements List of elements
     * @return List of safe clickable elements
     */
    fun filterSafeClickable(elements: List<ElementInfo>): List<ElementInfo> {
        return elements
            .map { classify(it) }
            .filterIsInstance<ElementClassification.SafeClickable>()
            .map { it.element }
    }

    /**
     * Get classification statistics
     *
     * @param elements List of elements
     * @return Classification stats
     */
    fun getStats(elements: List<ElementInfo>): ClassificationStats {
        val classifications = classifyAll(elements)

        var safeClickable = 0
        var dangerous = 0
        var editText = 0
        var loginFields = 0
        var nonClickable = 0
        var disabled = 0

        classifications.forEach { classification ->
            when (classification) {
                is ElementClassification.SafeClickable -> safeClickable++
                is ElementClassification.Dangerous -> dangerous++
                is ElementClassification.EditText -> editText++
                is ElementClassification.LoginField -> loginFields++
                is ElementClassification.NonClickable -> nonClickable++
                is ElementClassification.Disabled -> disabled++
            }
        }

        return ClassificationStats(
            total = elements.size,
            safeClickable = safeClickable,
            dangerous = dangerous,
            editText = editText,
            loginFields = loginFields,
            nonClickable = nonClickable,
            disabled = disabled
        )
    }

    /**
     * Get dangerous elements with reasons
     *
     * @param elements List of elements
     * @return List of (element, reason) pairs
     */
    fun getDangerousElements(elements: List<ElementInfo>): List<Pair<ElementInfo, String>> {
        return elements
            .map { classify(it) }
            .filterIsInstance<ElementClassification.Dangerous>()
            .map { it.element to it.reason }
    }
}

/**
 * Classification Statistics
 *
 * @property total Total elements classified
 * @property safeClickable Safe clickable elements
 * @property dangerous Dangerous elements
 * @property editText EditText fields
 * @property loginFields Login screen fields
 * @property nonClickable Non-clickable elements
 * @property disabled Disabled elements
 */
data class ClassificationStats(
    val total: Int,
    val safeClickable: Int,
    val dangerous: Int,
    val editText: Int,
    val loginFields: Int,
    val nonClickable: Int,
    val disabled: Int
) {
    /**
     * Calculate percentage of safe clickable elements
     *
     * @return Percentage (0.0-1.0)
     */
    fun safeClickablePercentage(): Float {
        if (total == 0) return 0f
        return safeClickable.toFloat() / total.toFloat()
    }

    override fun toString(): String {
        return """
            Classification Stats:
            - Total: $total
            - Safe Clickable: $safeClickable (${(safeClickablePercentage() * 100).toInt()}%)
            - Dangerous: $dangerous
            - EditText: $editText
            - Login Fields: $loginFields
            - Non-Clickable: $nonClickable
            - Disabled: $disabled
        """.trimIndent()
    }
}
