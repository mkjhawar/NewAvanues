package com.augmentalis.commandmanager

/**
 * Screen types for classifying UI screens based on their element composition.
 * Used by the Hybrid Persistence system to determine caching strategies.
 *
 * Classification helps optimize:
 * - Static screens (SETTINGS, NAVIGATION): Cache aggressively, persist to database
 * - Dynamic screens (LIST): Keep in memory only, refresh frequently
 * - Mixed screens (HOME, DETAIL): Selective caching based on element stability
 */
enum class ScreenType {
    /** Settings, preferences - mostly static toggle switches and options */
    SETTINGS_SCREEN,

    /** Email list, chat list, feed - mostly dynamic scrollable content */
    LIST_SCREEN,

    /** Login, registration, data entry - static input fields */
    FORM_SCREEN,

    /** Email detail, profile view, article - mixed static/dynamic content */
    DETAIL_SCREEN,

    /** Menu, navigation drawer, tab bar - static navigation elements */
    NAVIGATION_SCREEN,

    /** App home/landing page - mixed content types */
    HOME_SCREEN,

    /** Default when classification is uncertain */
    UNKNOWN
}

/**
 * Statistics about UI elements on a screen.
 * Used for screen classification heuristics.
 *
 * @property totalElements Total number of elements analyzed
 * @property clickableCount Number of clickable/tappable elements
 * @property textInputCount Number of text input fields (EditText, TextField)
 * @property switchCount Number of switch/toggle/checkbox elements
 * @property listItemCount Number of elements detected as list items
 * @property scrollableContainerCount Number of scrollable containers (RecyclerView, ListView)
 */
data class ScreenStats(
    val totalElements: Int,
    val clickableCount: Int,
    val textInputCount: Int,
    val switchCount: Int,
    val listItemCount: Int,
    val scrollableContainerCount: Int
) {
    /** Ratio of clickable elements to total (0.0-1.0) */
    val clickableRatio: Float
        get() = if (totalElements > 0) clickableCount.toFloat() / totalElements else 0f

    /** Ratio of text inputs to total (0.0-1.0) */
    val textInputRatio: Float
        get() = if (totalElements > 0) textInputCount.toFloat() / totalElements else 0f

    /** Ratio of switches/toggles to total (0.0-1.0) */
    val switchRatio: Float
        get() = if (totalElements > 0) switchCount.toFloat() / totalElements else 0f

    /** Ratio of list items to total (0.0-1.0) */
    val listItemRatio: Float
        get() = if (totalElements > 0) listItemCount.toFloat() / totalElements else 0f

    /** Whether the screen has scrollable content */
    val hasScrollableContent: Boolean
        get() = scrollableContainerCount > 0 || listItemCount > 3

    companion object {
        /** Empty stats for screens with no elements */
        val EMPTY = ScreenStats(0, 0, 0, 0, 0, 0)
    }
}

/**
 * Classifies screens based on their UI element composition.
 *
 * The classifier analyzes element types, ratios, and patterns to determine
 * the most likely screen type. This information is used by the Hybrid
 * Persistence system to optimize caching and refresh strategies.
 *
 * ## Classification Heuristics
 *
 * - **SETTINGS_SCREEN**: High switch/checkbox ratio (>15%) OR package contains "settings"
 * - **LIST_SCREEN**: Has scrollable container + many list items (>5)
 * - **FORM_SCREEN**: High text input ratio (>20%)
 * - **NAVIGATION_SCREEN**: Drawer/menu indicators OR high clickable ratio with short labels
 * - **DETAIL_SCREEN**: Few inputs, moderate content, some scrollable elements
 * - **HOME_SCREEN**: Mixed content with navigation elements at root
 * - **UNKNOWN**: Default when no clear pattern is detected
 *
 * ## Usage
 *
 * ```kotlin
 * val elements = scrapeScreen()
 * val screenType = ScreenClassifier.classifyScreen(elements, "com.example.app")
 * val stats = ScreenClassifier.calculateScreenStats(elements)
 * ```
 */
object ScreenClassifier {

    // Pattern constants for element type detection
    private val SWITCH_PATTERNS = listOf("switch", "toggle", "checkbox", "compoundbutton")
    private val TEXT_INPUT_PATTERNS = listOf("edittext", "textfield", "input", "textinput", "autocomplete")
    private val LIST_ITEM_PATTERNS = listOf("item", "cell", "row", "entry")
    private val SCROLLABLE_CONTAINER_PATTERNS = listOf("recyclerview", "listview", "scrollview", "lazycolumn", "lazylist")
    private val NAVIGATION_PATTERNS = listOf("drawer", "menu", "navigation", "navbar", "tabbar", "bottombar")

    // Threshold constants for classification
    private const val SWITCH_RATIO_THRESHOLD = 0.15f       // 15% switches = settings screen
    private const val TEXT_INPUT_RATIO_THRESHOLD = 0.20f   // 20% inputs = form screen
    private const val LIST_ITEM_COUNT_THRESHOLD = 5        // 5+ list items = list screen
    private const val NAVIGATION_CLICKABLE_THRESHOLD = 0.6f // 60% clickable = possible navigation
    private const val NAVIGATION_LABEL_MAX_LENGTH = 25     // Short labels typical of nav items
    private const val DETAIL_SCROLLABLE_THRESHOLD = 1      // At least 1 scrollable for detail
    private const val HOME_MIXED_THRESHOLD = 0.3f          // Mixed ratios for home detection

    /**
     * Classifies a screen based on its UI elements and package name.
     *
     * @param elements List of UI elements scraped from the screen
     * @param packageName The package name of the host application
     * @return The detected [ScreenType] for caching strategy decisions
     */
    fun classifyScreen(elements: List<ElementInfo>, packageName: String): ScreenType {
        if (elements.isEmpty()) return ScreenType.UNKNOWN

        val stats = calculateScreenStats(elements)
        val lowerPackage = packageName.lowercase()

        // Priority 1: Package name hints (highest confidence)
        if (lowerPackage.contains("settings") || lowerPackage.contains("preferences")) {
            return ScreenType.SETTINGS_SCREEN
        }

        // Priority 2: High switch/toggle ratio indicates settings
        if (stats.switchRatio >= SWITCH_RATIO_THRESHOLD && stats.switchCount >= 3) {
            return ScreenType.SETTINGS_SCREEN
        }

        // Priority 3: High text input ratio indicates form
        if (stats.textInputRatio >= TEXT_INPUT_RATIO_THRESHOLD && stats.textInputCount >= 2) {
            return ScreenType.FORM_SCREEN
        }

        // Priority 4: Scrollable with many list items indicates list screen
        if (stats.hasScrollableContent && stats.listItemCount >= LIST_ITEM_COUNT_THRESHOLD) {
            return ScreenType.LIST_SCREEN
        }

        // Priority 5: Navigation detection
        if (isNavigationScreen(elements, stats, lowerPackage)) {
            return ScreenType.NAVIGATION_SCREEN
        }

        // Priority 6: Detail screen detection (has scrollable, few inputs, moderate content)
        if (isDetailScreen(elements, stats)) {
            return ScreenType.DETAIL_SCREEN
        }

        // Priority 7: Home screen detection (mixed content at root)
        if (isHomeScreen(elements, stats, lowerPackage)) {
            return ScreenType.HOME_SCREEN
        }

        // Default to UNKNOWN
        return ScreenType.UNKNOWN
    }

    /**
     * Calculates statistics about UI elements on the screen.
     *
     * @param elements List of UI elements to analyze
     * @return [ScreenStats] with counts and ratios for each element type
     */
    fun calculateScreenStats(elements: List<ElementInfo>): ScreenStats {
        if (elements.isEmpty()) return ScreenStats.EMPTY

        var clickableCount = 0
        var textInputCount = 0
        var switchCount = 0
        var listItemCount = 0
        var scrollableContainerCount = 0

        for (element in elements) {
            val lowerClassName = element.className.lowercase()
            val lowerRole = element.semanticsRole.lowercase()

            // Count clickable elements
            if (element.isClickable) {
                clickableCount++
            }

            // Count text inputs
            if (isTextInput(lowerClassName, lowerRole)) {
                textInputCount++
            }

            // Count switches/toggles/checkboxes
            if (isSwitch(lowerClassName, lowerRole, element.isChecked)) {
                switchCount++
            }

            // Count list items
            if (isListItem(element, lowerClassName)) {
                listItemCount++
            }

            // Count scrollable containers
            if (isScrollableContainer(element, lowerClassName)) {
                scrollableContainerCount++
            }
        }

        return ScreenStats(
            totalElements = elements.size,
            clickableCount = clickableCount,
            textInputCount = textInputCount,
            switchCount = switchCount,
            listItemCount = listItemCount,
            scrollableContainerCount = scrollableContainerCount
        )
    }

    /**
     * Checks if an element is a text input field.
     */
    private fun isTextInput(lowerClassName: String, lowerRole: String): Boolean {
        return TEXT_INPUT_PATTERNS.any { pattern ->
            lowerClassName.contains(pattern) || lowerRole.contains(pattern)
        } || lowerRole == "textfield" || lowerRole == "textbox"
    }

    /**
     * Checks if an element is a switch, toggle, or checkbox.
     */
    private fun isSwitch(lowerClassName: String, lowerRole: String, isChecked: Boolean?): Boolean {
        // If isChecked is not null, it's likely a checkable element
        if (isChecked != null) return true

        return SWITCH_PATTERNS.any { pattern ->
            lowerClassName.contains(pattern) || lowerRole.contains(pattern)
        } || lowerRole == "switch" || lowerRole == "checkbox"
    }

    /**
     * Checks if an element is a list item.
     */
    private fun isListItem(element: ElementInfo, lowerClassName: String): Boolean {
        // Explicit list index is most reliable
        if (element.listIndex >= 0) return true

        // Inside a dynamic container
        if (element.isInDynamicContainer) return true

        // Class name hints
        return LIST_ITEM_PATTERNS.any { pattern -> lowerClassName.contains(pattern) }
    }

    /**
     * Checks if an element is a scrollable container.
     */
    private fun isScrollableContainer(element: ElementInfo, lowerClassName: String): Boolean {
        // Explicit scrollable flag on container-like elements
        if (element.isScrollable && !element.isClickable) return true

        // Container type from scraper
        val lowerContainerType = element.containerType.lowercase()
        if (lowerContainerType.isNotEmpty()) {
            if (SCROLLABLE_CONTAINER_PATTERNS.any { lowerContainerType.contains(it) }) {
                return true
            }
        }

        // Class name patterns
        return SCROLLABLE_CONTAINER_PATTERNS.any { pattern -> lowerClassName.contains(pattern) }
    }

    /**
     * Determines if the screen is a navigation screen (drawer, menu, tab bar).
     */
    private fun isNavigationScreen(
        elements: List<ElementInfo>,
        stats: ScreenStats,
        lowerPackage: String
    ): Boolean {
        // Package hint
        if (NAVIGATION_PATTERNS.any { lowerPackage.contains(it) }) {
            return true
        }

        // Check for navigation-related elements in class names
        val hasNavigationElements = elements.any { element ->
            val lowerClass = element.className.lowercase()
            val lowerRole = element.semanticsRole.lowercase()
            NAVIGATION_PATTERNS.any { pattern ->
                lowerClass.contains(pattern) || lowerRole.contains(pattern)
            }
        }

        if (hasNavigationElements) return true

        // High clickable ratio with short labels (typical of nav menus)
        if (stats.clickableRatio >= NAVIGATION_CLICKABLE_THRESHOLD) {
            val clickableElements = elements.filter { it.isClickable }
            val avgLabelLength = clickableElements
                .map { it.voiceLabel.length }
                .average()
                .takeIf { !it.isNaN() } ?: 0.0

            if (avgLabelLength <= NAVIGATION_LABEL_MAX_LENGTH && avgLabelLength > 0) {
                return true
            }
        }

        return false
    }

    /**
     * Determines if the screen is a detail/content view screen.
     */
    private fun isDetailScreen(elements: List<ElementInfo>, stats: ScreenStats): Boolean {
        // Few inputs (not a form)
        if (stats.textInputCount > 1) return false

        // Has some scrollable content
        if (stats.scrollableContainerCount < DETAIL_SCROLLABLE_THRESHOLD &&
            !elements.any { it.isScrollable }
        ) {
            return false
        }

        // Has moderate text content (not empty, not a simple nav)
        val textElements = elements.filter { it.text.isNotBlank() || it.contentDescription.isNotBlank() }
        val avgTextLength = textElements
            .map { (it.text.length + it.contentDescription.length) }
            .average()
            .takeIf { !it.isNaN() } ?: 0.0

        // Detail screens typically have longer text content
        return avgTextLength > 20 && stats.clickableRatio < 0.5f
    }

    /**
     * Determines if the screen is a home/landing screen.
     */
    private fun isHomeScreen(
        elements: List<ElementInfo>,
        stats: ScreenStats,
        lowerPackage: String
    ): Boolean {
        // Package hints
        if (lowerPackage.contains("home") || lowerPackage.contains("main") ||
            lowerPackage.contains("launcher")
        ) {
            return true
        }

        // Mixed content: has some of everything but not dominated by any type
        val hasVariedContent = stats.switchRatio < HOME_MIXED_THRESHOLD &&
                stats.textInputRatio < HOME_MIXED_THRESHOLD &&
                stats.listItemRatio < HOME_MIXED_THRESHOLD

        // Has clickable elements (navigation/actions)
        val hasActions = stats.clickableCount >= 3

        // Has some text content
        val hasTextContent = elements.any {
            it.text.isNotBlank() || it.contentDescription.isNotBlank()
        }

        return hasVariedContent && hasActions && hasTextContent
    }

    /**
     * Returns a human-readable description of the screen type.
     * Useful for logging and debugging.
     *
     * @param screenType The screen type to describe
     * @return A descriptive string for the screen type
     */
    fun describeScreenType(screenType: ScreenType): String {
        return when (screenType) {
            ScreenType.SETTINGS_SCREEN -> "Settings/Preferences (static, cache aggressively)"
            ScreenType.LIST_SCREEN -> "List/Feed (dynamic, memory-only)"
            ScreenType.FORM_SCREEN -> "Form/Input (static fields, cache)"
            ScreenType.DETAIL_SCREEN -> "Detail/Content (mixed, selective cache)"
            ScreenType.NAVIGATION_SCREEN -> "Navigation/Menu (static, cache)"
            ScreenType.HOME_SCREEN -> "Home/Landing (mixed, selective cache)"
            ScreenType.UNKNOWN -> "Unknown (default behavior)"
        }
    }

    /**
     * Determines the recommended persistence strategy for a screen type.
     *
     * @param screenType The classified screen type
     * @return True if elements should be persisted to database, false for memory-only
     */
    fun shouldPersistScreen(screenType: ScreenType): Boolean {
        return when (screenType) {
            ScreenType.SETTINGS_SCREEN -> true   // Highly static, persist
            ScreenType.LIST_SCREEN -> false      // Dynamic, memory only
            ScreenType.FORM_SCREEN -> true       // Static fields, persist
            ScreenType.DETAIL_SCREEN -> false    // Content changes, memory only
            ScreenType.NAVIGATION_SCREEN -> true // Very static, persist
            ScreenType.HOME_SCREEN -> true       // Usually static layout, persist
            ScreenType.UNKNOWN -> false          // Conservative default
        }
    }
}
