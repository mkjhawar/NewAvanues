package com.augmentalis.voiceoscore.dsl.interpreter

/**
 * Platform abstraction for querying the device environment during AVU DSL execution.
 *
 * The interpreter delegates built-in calls like `screen.contains("text")` to QRY code
 * dispatches. This interface provides a higher-level API that platform dispatchers use
 * to answer those queries.
 *
 * ## Platform Implementations
 * - **Android**: AccessibilityService node tree inspection
 * - **iOS**: UIAccessibility element queries (future)
 * - **Desktop**: Window/process inspection (future)
 */
interface IAvuEnvironment {

    /**
     * Check if the current screen contains the given text.
     * Used by `screen.contains("text")` expressions.
     */
    suspend fun screenContains(text: String): Boolean

    /**
     * Get all visible text on the current screen.
     * Returns a list of text strings found on screen elements.
     */
    suspend fun getScreenText(): List<String>

    /**
     * Check if a UI element with the given identifier is visible.
     *
     * @param elementId View ID, accessibility ID, or content description
     */
    suspend fun isElementVisible(elementId: String): Boolean

    /**
     * Get the current foreground app identifier.
     * Returns package name (Android), bundle ID (iOS), or process name (Desktop).
     */
    suspend fun getForegroundApp(): String?

    /**
     * Get a named environment property.
     * Supports platform-specific properties like battery level, connectivity, etc.
     *
     * @param key Property key (e.g., "battery_level", "is_connected")
     * @return Property value or null if not available
     */
    suspend fun getProperty(key: String): Any?
}

/**
 * Current screen state snapshot.
 * Platform dispatchers create these from accessibility tree inspection.
 */
data class ScreenState(
    val visibleTexts: List<String> = emptyList(),
    val visibleElements: List<ScreenElement> = emptyList(),
    val foregroundApp: String? = null
) {
    fun containsText(text: String): Boolean =
        visibleTexts.any { it.contains(text, ignoreCase = true) }

    fun hasElement(id: String): Boolean =
        visibleElements.any { it.id == id || it.contentDescription == id }
}

/**
 * A UI element visible on screen.
 */
data class ScreenElement(
    val id: String?,
    val contentDescription: String?,
    val text: String?,
    val className: String?,
    val isClickable: Boolean = false,
    val isEnabled: Boolean = true
)

/**
 * Stub environment that returns empty/default values.
 * Used on platforms where environment queries are not yet implemented.
 */
class StubEnvironment : IAvuEnvironment {
    override suspend fun screenContains(text: String): Boolean = false
    override suspend fun getScreenText(): List<String> = emptyList()
    override suspend fun isElementVisible(elementId: String): Boolean = false
    override suspend fun getForegroundApp(): String? = null
    override suspend fun getProperty(key: String): Any? = null
}
