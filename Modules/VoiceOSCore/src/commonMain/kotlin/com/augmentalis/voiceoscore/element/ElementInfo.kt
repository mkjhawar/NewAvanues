package com.augmentalis.voiceoscore

/**
 * Represents the bounds of a UI element
 */
data class Bounds(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val width: Int get() = right - left
    val height: Int get() = bottom - top
    val centerX: Int get() = left + width / 2
    val centerY: Int get() = top + height / 2

    companion object {
        val EMPTY = Bounds(0, 0, 0, 0)

        fun fromString(boundsStr: String): Bounds? {
            return try {
                val parts = boundsStr.split(",").map { it.trim().toInt() }
                if (parts.size == 4) {
                    Bounds(parts[0], parts[1], parts[2], parts[3])
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }

    override fun toString(): String = "$left,$top,$right,$bottom"
}

/**
 * Contains information about a UI element for voice accessibility processing.
 *
 * This is the core data class used across VoiceOSCoreNG for representing
 * scraped UI elements that can be targeted by voice commands.
 *
 * @property className The class name of the UI element (e.g., "Button", "EditText")
 * @property resourceId The resource ID if available (e.g., "com.app:id/submit_btn")
 * @property text The text content displayed by the element
 * @property contentDescription The accessibility content description
 * @property bounds The screen coordinates of the element
 * @property isClickable Whether the element responds to click events
 * @property isScrollable Whether the element is scrollable
 * @property isEnabled Whether the element is currently enabled
 * @property packageName The package name of the host application
 */
data class ElementInfo(
    val className: String,
    val resourceId: String = "",
    val text: String = "",
    val contentDescription: String = "",
    val bounds: Bounds = Bounds.EMPTY,
    val isClickable: Boolean = false,
    val isLongClickable: Boolean = false,
    val isScrollable: Boolean = false,
    val isEnabled: Boolean = true,
    val packageName: String = "",
    // AVID fingerprint for tracking across sessions (format: {TypeCode}:{hash8})
    val avid: String? = null,
    // Platform-specific node reference (Any? for KMP compatibility)
    val node: Any? = null,
    // Compose semantics support (P2)
    val semanticsRole: String = "",           // e.g., "Button", "Checkbox", "Tab"
    val stateDescription: String = "",        // e.g., "Checked", "Unchecked", "Selected"
    val isSelected: Boolean = false,          // For tabs, chips, list items
    val isChecked: Boolean? = null,           // For checkboxes, switches (null = not applicable)
    val testTag: String = "",                 // Compose testTag for identification
    // Dynamic content detection (for static/dynamic UI separation)
    val isInDynamicContainer: Boolean = false, // True if inside RecyclerView/ListView
    val containerType: String = "",           // Container class (RecyclerView, ListView, etc.)
    val listIndex: Int = -1,                  // Position in list (-1 if not in list)
    // NAV-500 Fix #2: Scroll tracking for accurate click coordinates
    val containerResourceId: String = "",     // Resource ID of parent scrollable container
    val scrollOffsetX: Int = 0,               // Scroll offset X at time of extraction
    val scrollOffsetY: Int = 0                // Scroll offset Y at time of extraction
) {
    /**
     * Legacy alias for avid (deprecated, use avid directly).
     */
    @Deprecated("Use avid instead", ReplaceWith("avid"))
    val uuid: String? get() = avid

    /**
     * Generate a stable identifier for this element across scrapes.
     * Uses structural properties that don't change (class, resourceId, position).
     */
    fun stableId(): String {
        return "$className|$resourceId|${bounds.left},${bounds.top}".hashCode().toString(16)
    }

    /**
     * Calculate stability score for prioritizing click order.
     * Higher score = more stable/reliable element.
     */
    fun stabilityScore(): Int {
        var score = 0

        // Resource ID is most stable
        if (resourceId.isNotBlank()) score += 30

        // Text is fairly stable
        if (text.isNotBlank()) score += 20

        // Content description
        if (contentDescription.isNotBlank()) score += 15

        // Elements in dynamic containers are less stable
        if (isInDynamicContainer) score -= 20

        // Large bounds usually means main content (less stable)
        if (bounds.width > 500 && bounds.height > 500) score -= 10

        return score
    }
    /**
     * Get the best available label for voice recognition
     * Priority: text > contentDescription > resourceId simple name
     */
    val voiceLabel: String
        get() = when {
            text.isNotBlank() -> text
            contentDescription.isNotBlank() -> contentDescription
            resourceId.isNotBlank() -> resourceId.substringAfterLast("/").replace("_", " ")
            else -> className.substringAfterLast(".")
        }

    /**
     * Check if this element has meaningful content for voice targeting
     */
    val hasVoiceContent: Boolean
        get() = text.isNotBlank() || contentDescription.isNotBlank() || resourceId.isNotBlank()

    /**
     * Check if this element is actionable (clickable or scrollable)
     */
    val isActionable: Boolean
        get() = isClickable || isScrollable

    /**
     * Check if this element is dynamic content (list items, chat messages, etc.)
     * Dynamic elements should be kept in memory only, not persisted to database.
     *
     * Detection criteria:
     * 1. Inside a dynamic container (RecyclerView, ListView)
     * 2. Very long text (>100 chars) - likely message preview
     * 3. Email-like patterns ("Unread, , ,")
     *
     * @deprecated Use [PersistenceDecisionEngine.decideForElement] for 4-layer persistence decision.
     * This legacy check only considers container type and text patterns.
     * The new 4-layer system considers:
     * - Layer 1: App category (EMAIL, SETTINGS, etc.)
     * - Layer 2: Container type (RecyclerView vs ScrollView)
     * - Layer 3: Content signals (text length, stability score)
     * - Layer 4: Screen type (settings screen, form, list)
     */
    @Deprecated(
        message = "Use PersistenceDecisionEngine.decideForElement() for 4-layer persistence decision",
        level = DeprecationLevel.WARNING
    )
    val isDynamicContent: Boolean
        get() {
            // In dynamic container (most reliable)
            if (isInDynamicContainer) return true

            // Long text indicates message/email preview
            val textLen = text.length + contentDescription.length
            if (textLen > 100) return true

            // Email-like patterns
            val combined = "$text $contentDescription"
            if (combined.startsWith("Unread,")) return true
            if (combined.contains(" at \\d+:\\d+\\s*(AM|PM)".toRegex(RegexOption.IGNORE_CASE))) return true

            return false
        }

    /**
     * Check if this element should be persisted to database.
     * Static UI elements (menus, buttons) are persisted.
     * Dynamic content (list items) is kept in memory only.
     *
     * @deprecated Use [PersistenceDecisionEngine.decideForElement] for 4-layer persistence decision.
     */
    @Suppress("DEPRECATION")
    @Deprecated(
        message = "Use PersistenceDecisionEngine.decideForElement() for 4-layer persistence decision",
        level = DeprecationLevel.WARNING
    )
    val shouldPersist: Boolean
        get() = !isDynamicContent && hasVoiceContent && isActionable

    companion object {
        /**
         * Create an empty ElementInfo (useful for testing)
         */
        val EMPTY = ElementInfo(className = "")

        /**
         * Migration helper: Get persistence decision for an element.
         *
         * This is a convenience method that calls [PersistenceDecisionEngine.decideForElement].
         * Requires additional context (packageName, allElements) for accurate decision.
         *
         * @param element The element to evaluate
         * @param packageName The app package name for category-based rules
         * @param allElements All elements on the current screen for context analysis
         * @return A [PersistenceDecision] indicating whether and how to persist the element
         */
        fun getPersistenceDecision(
            element: ElementInfo,
            packageName: String,
            allElements: List<ElementInfo>
        ): PersistenceDecision {
            return PersistenceDecisionEngine.decideForElement(element, packageName, allElements)
        }

        /**
         * Create ElementInfo for a simple button
         */
        fun button(
            text: String,
            resourceId: String = "",
            bounds: Bounds = Bounds.EMPTY,
            packageName: String = ""
        ) = ElementInfo(
            className = "Button",
            text = text,
            resourceId = resourceId,
            bounds = bounds,
            isClickable = true,
            isEnabled = true,
            packageName = packageName
        )

        /**
         * Create ElementInfo for a text input field
         */
        fun input(
            hint: String = "",
            resourceId: String = "",
            bounds: Bounds = Bounds.EMPTY,
            packageName: String = ""
        ) = ElementInfo(
            className = "EditText",
            contentDescription = hint,
            resourceId = resourceId,
            bounds = bounds,
            isClickable = true,
            isEnabled = true,
            packageName = packageName
        )
    }
}
