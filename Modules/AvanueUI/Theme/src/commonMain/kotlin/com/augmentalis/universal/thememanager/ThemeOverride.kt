package com.augmentalis.universal.thememanager

import com.augmentalis.avamagic.components.core.Theme
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable

/**
 * Per-app theme override configuration
 *
 * Supports two override modes:
 * 1. FULL - Complete theme replacement (app uses entirely different theme)
 * 2. PARTIAL - Selective property override (app inherits some properties from universal theme)
 *
 * Example (Full Override):
 * ```kotlin
 * ThemeOverride(
 *     appId = "com.augmentalis.voiceos",
 *     overrideType = OverrideType.FULL,
 *     theme = Themes.iOS26LiquidGlass,
 *     inheritedProperties = emptyList()
 * )
 * ```
 *
 * Example (Partial Override):
 * ```kotlin
 * ThemeOverride(
 *     appId = "com.augmentalis.noteavanue",
 *     overrideType = OverrideType.PARTIAL,
 *     theme = customTheme, // Only colorScheme is different
 *     inheritedProperties = listOf(
 *         "typography",
 *         "shapes",
 *         "spacing",
 *         "elevation"
 *     )
 * )
 * ```
 */
@Serializable
data class ThemeOverride(
    /**
     * App identifier (e.g., "com.augmentalis.voiceos")
     */
    val appId: String,

    /**
     * Override type - FULL or PARTIAL
     */
    val overrideType: OverrideType,

    /**
     * The theme to use for this app
     * For FULL overrides: Complete theme replacement
     * For PARTIAL overrides: Theme with customized properties
     */
    val theme: Theme,

    /**
     * List of properties to inherit from universal theme (PARTIAL mode only)
     * Top-level properties: "colorScheme", "typography", "shapes", "spacing", "elevation", "material", "animation"
     * Nested properties: "colorScheme.primary", "typography.displayLarge", etc.
     *
     * For FULL overrides, this list should be empty.
     */
    val inheritedProperties: List<String> = emptyList(),

    /**
     * Optional description of why this override exists
     */
    val description: String? = null,

    /**
     * Timestamp when this override was created
     */
    val createdAt: Long = Clock.System.now().toEpochMilliseconds(),

    /**
     * Timestamp when this override was last modified
     */
    val modifiedAt: Long = Clock.System.now().toEpochMilliseconds()
) {
    /**
     * Check if a specific property is inherited from universal theme
     */
    fun isPropertyInherited(propertyPath: String): Boolean {
        if (overrideType == OverrideType.FULL) return false
        return inheritedProperties.contains(propertyPath)
    }

    /**
     * Check if all properties are overridden (no inheritance)
     */
    fun isFullOverride(): Boolean {
        return overrideType == OverrideType.FULL || inheritedProperties.isEmpty()
    }

    /**
     * Create a copy with updated timestamp
     */
    fun withUpdatedTimestamp(): ThemeOverride {
        return copy(modifiedAt = Clock.System.now().toEpochMilliseconds())
    }
}

/**
 * Override type enumeration
 */
@Serializable
enum class OverrideType {
    /**
     * FULL - Complete theme replacement
     * The app uses its own theme entirely, no inheritance from universal theme
     */
    FULL,

    /**
     * PARTIAL - Selective property override
     * The app inherits specified properties from universal theme
     * and overrides others with its custom values
     */
    PARTIAL
}

/**
 * Builder for creating theme overrides with fluent API
 */
class ThemeOverrideBuilder(private val appId: String) {
    private var overrideType: OverrideType = OverrideType.FULL
    private lateinit var theme: Theme
    private var inheritedProperties: MutableList<String> = mutableListOf()
    private var description: String? = null

    /**
     * Set this as a full override (complete theme replacement)
     */
    fun fullOverride(theme: Theme): ThemeOverrideBuilder {
        this.overrideType = OverrideType.FULL
        this.theme = theme
        this.inheritedProperties.clear()
        return this
    }

    /**
     * Set this as a partial override (selective property override)
     */
    fun partialOverride(theme: Theme): ThemeOverrideBuilder {
        this.overrideType = OverrideType.PARTIAL
        this.theme = theme
        return this
    }

    /**
     * Inherit specific properties from universal theme
     */
    fun inherit(vararg properties: String): ThemeOverrideBuilder {
        inheritedProperties.addAll(properties)
        return this
    }

    /**
     * Inherit typography from universal theme
     */
    fun inheritTypography(): ThemeOverrideBuilder {
        inheritedProperties.add("typography")
        return this
    }

    /**
     * Inherit color scheme from universal theme
     */
    fun inheritColorScheme(): ThemeOverrideBuilder {
        inheritedProperties.add("colorScheme")
        return this
    }

    /**
     * Inherit shapes from universal theme
     */
    fun inheritShapes(): ThemeOverrideBuilder {
        inheritedProperties.add("shapes")
        return this
    }

    /**
     * Inherit spacing from universal theme
     */
    fun inheritSpacing(): ThemeOverrideBuilder {
        inheritedProperties.add("spacing")
        return this
    }

    /**
     * Inherit elevation from universal theme
     */
    fun inheritElevation(): ThemeOverrideBuilder {
        inheritedProperties.add("elevation")
        return this
    }

    /**
     * Inherit material system from universal theme
     */
    fun inheritMaterial(): ThemeOverrideBuilder {
        inheritedProperties.add("material")
        return this
    }

    /**
     * Inherit animation config from universal theme
     */
    fun inheritAnimation(): ThemeOverrideBuilder {
        inheritedProperties.add("animation")
        return this
    }

    /**
     * Set a description for this override
     */
    fun withDescription(description: String): ThemeOverrideBuilder {
        this.description = description
        return this
    }

    /**
     * Build the theme override
     */
    fun build(): ThemeOverride {
        require(::theme.isInitialized) { "Theme must be set" }

        return ThemeOverride(
            appId = appId,
            overrideType = overrideType,
            theme = theme,
            inheritedProperties = inheritedProperties.toList(),
            description = description
        )
    }
}

/**
 * Extension function to create a theme override builder
 */
fun String.createThemeOverride(): ThemeOverrideBuilder {
    return ThemeOverrideBuilder(this)
}

/**
 * Predefined property paths for inheritance
 */
object ThemeProperties {
    const val COLOR_SCHEME = "colorScheme"
    const val TYPOGRAPHY = "typography"
    const val SHAPES = "shapes"
    const val SPACING = "spacing"
    const val ELEVATION = "elevation"
    const val MATERIAL = "material"
    const val ANIMATION = "animation"

    // Nested color scheme properties
    const val COLOR_SCHEME_PRIMARY = "colorScheme.primary"
    const val COLOR_SCHEME_SECONDARY = "colorScheme.secondary"
    const val COLOR_SCHEME_TERTIARY = "colorScheme.tertiary"
    const val COLOR_SCHEME_ERROR = "colorScheme.error"
    const val COLOR_SCHEME_SURFACE = "colorScheme.surface"
    const val COLOR_SCHEME_BACKGROUND = "colorScheme.background"

    // Nested typography properties
    const val TYPOGRAPHY_DISPLAY = "typography.display"
    const val TYPOGRAPHY_HEADLINE = "typography.headline"
    const val TYPOGRAPHY_TITLE = "typography.title"
    const val TYPOGRAPHY_BODY = "typography.body"
    const val TYPOGRAPHY_LABEL = "typography.label"
}
