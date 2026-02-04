package com.augmentalis.avaelements.plugins

/**
 * Plugin Configuration System
 *
 * Defines which plugins are automatically included in builds.
 * Apps can customize this to control bundle size vs functionality.
 */

/**
 * Plugin bundle configuration
 */
data class PluginConfig(
    /** Bundle name */
    val name: String,

    /** Components to include */
    val components: ComponentSet,

    /** Themes to include */
    val themes: ThemeSet,

    /** Asset libraries to include */
    val assets: AssetSet,

    /** Whether to enable CDN loading for missing plugins */
    val enableCDN: Boolean = true,

    /** Auto-cache popular items */
    val autoCachePopular: Boolean = true
)

/**
 * Component set configurations
 */
enum class ComponentSet(val description: String, val components: List<String>) {
    /**
     * Minimal - Only the absolute essentials (5 components)
     * Use case: Embedded systems, watch apps, very simple UIs
     * Size: ~40 KB
     */
    MINIMAL(
        "Minimal components for basic UIs",
        listOf(
            // Phase 1 - Basic only
            "button",
            "text",
            "column",
            "row",
            "container"
        )
    ),

    /**
     * Essentials - Most commonly used components (15 components)
     * Use case: 80% of apps, standard CRUD apps
     * Size: ~120 KB
     */
    ESSENTIALS(
        "Essential components for typical apps",
        listOf(
            // Phase 1 - All basic
            "button",
            "textfield",
            "text",
            "checkbox",
            "switch",
            "icon",
            "image",
            "column",
            "row",
            "container",
            "card",
            "scrollview",

            // Phase 3 - Most common
            "spinner",
            "progressbar",
            "alert"
        )
    ),

    /**
     * Standard - All Phase 1 + common Phase 3 (28 components)
     * Use case: Full-featured apps
     * Size: ~250 KB
     */
    STANDARD(
        "All Phase 1 + common Phase 3 components",
        listOf(
            // All Phase 1 (13)
            "button", "textfield", "text", "checkbox", "switch",
            "icon", "image", "column", "row", "container",
            "card", "scrollview", "list",

            // Phase 3 - Common (15)
            "slider", "datepicker", "dropdown", "searchbar",
            "badge", "chip", "avatar", "divider", "spinner",
            "progressbar", "alert", "snackbar", "modal",
            "appbar", "bottomnav"
        )
    ),

    /**
     * Complete - All components (48 components)
     * Use case: Complex apps, component showcase
     * Size: ~500 KB
     */
    COMPLETE(
        "All Phase 1 + Phase 3 components",
        listOf(
            // All Phase 1 (13)
            "button", "textfield", "text", "checkbox", "switch",
            "icon", "image", "column", "row", "container",
            "card", "scrollview", "list",

            // All Phase 3 (35)
            // Input (12)
            "slider", "rangeslider", "datepicker", "timepicker",
            "radiobutton", "radiogroup", "dropdown", "autocomplete",
            "fileupload", "imagepicker", "rating", "searchbar",

            // Display (8)
            "badge", "chip", "avatar", "divider",
            "skeleton", "spinner", "progressbar", "tooltip",

            // Layout (5)
            "grid", "stack", "spacer", "drawer", "tabs",

            // Navigation (4)
            "appbar", "bottomnav", "breadcrumb", "pagination",

            // Feedback (6)
            "alert", "snackbar", "modal", "toast", "confirm", "contextmenu"
        )
    ),

    /**
     * Custom - Specify your own list
     */
    CUSTOM("Custom component selection", emptyList());

    val count: Int get() = components.size
}

/**
 * Theme set configurations
 */
enum class ThemeSet(val description: String, val themes: List<String>) {
    /**
     * None - No bundled themes (minimal size)
     * Themes loaded from CDN on-demand
     */
    NONE("No bundled themes", emptyList()),

    /**
     * Single - One theme for your primary platform
     * Size: ~20 KB
     */
    SINGLE_MATERIAL3(
        "Material Design 3 only",
        listOf("material3-light", "material3-dark")
    ),

    SINGLE_IOS26(
        "iOS 26 Liquid Glass only",
        listOf("ios26-light", "ios26-dark")
    ),

    /**
     * Multi-Platform - Themes for Android + iOS
     * Size: ~50 KB
     */
    MULTI_PLATFORM(
        "Material 3 + iOS 26",
        listOf(
            "material3-light", "material3-dark",
            "ios26-light", "ios26-dark"
        )
    ),

    /**
     * All - All available themes
     * Size: ~100 KB
     */
    ALL(
        "All themes",
        listOf(
            "material3-light", "material3-dark",
            "ios26-light", "ios26-dark",
            "visionos2-light", "visionos2-dark",
            "fluent-light", "fluent-dark",
            "custom-default"
        )
    ),

    /**
     * Custom theme list
     */
    CUSTOM("Custom theme selection", emptyList());

    val count: Int get() = themes.size
}

/**
 * Asset library configurations
 */
enum class AssetSet(val description: String, val libraries: List<AssetLibraryConfig>) {
    /**
     * None - No bundled assets
     * All loaded from CDN on-demand
     */
    NONE("No bundled assets", emptyList()),

    /**
     * Popular Only - Just the most common icons cached
     * Size: ~50 KB (30 icons)
     */
    POPULAR_ONLY(
        "Popular icons only (cached)",
        listOf(
            AssetLibraryConfig(
                id = "material",
                mode = AssetLoadMode.POPULAR_CACHED,
                popularCount = 15
            ),
            AssetLibraryConfig(
                id = "fontawesome",
                mode = AssetLoadMode.POPULAR_CACHED,
                popularCount = 15
            )
        )
    ),

    /**
     * Metadata Only - Icon manifests bundled, icons from CDN
     * Size: ~10 KB
     */
    METADATA_ONLY(
        "Metadata bundled, icons from CDN",
        listOf(
            AssetLibraryConfig(
                id = "material",
                mode = AssetLoadMode.CDN_ONLY
            ),
            AssetLibraryConfig(
                id = "fontawesome",
                mode = AssetLoadMode.CDN_ONLY
            )
        )
    ),

    /**
     * Essential Pack - ~200 most common icons bundled
     * Size: ~400 KB
     */
    ESSENTIAL_PACK(
        "200 most common icons bundled",
        listOf(
            AssetLibraryConfig(
                id = "material",
                mode = AssetLoadMode.ESSENTIAL_BUNDLED,
                essentialCount = 100
            ),
            AssetLibraryConfig(
                id = "fontawesome",
                mode = AssetLoadMode.ESSENTIAL_BUNDLED,
                essentialCount = 100
            )
        )
    ),

    /**
     * Full Bundle - All icons bundled (NOT RECOMMENDED)
     * Size: ~8 MB
     */
    FULL_BUNDLE(
        "All icons bundled (not recommended)",
        listOf(
            AssetLibraryConfig(
                id = "material",
                mode = AssetLoadMode.FULL_BUNDLED
            ),
            AssetLibraryConfig(
                id = "fontawesome",
                mode = AssetLoadMode.FULL_BUNDLED
            )
        )
    ),

    /**
     * Custom configuration
     */
    CUSTOM("Custom asset configuration", emptyList());
}

/**
 * Asset library load modes
 */
enum class AssetLoadMode {
    /** No bundling, all from CDN */
    CDN_ONLY,

    /** Popular icons cached (~15), rest from CDN */
    POPULAR_CACHED,

    /** Essential icons bundled (~100), rest from CDN */
    ESSENTIAL_BUNDLED,

    /** All icons bundled (not recommended) */
    FULL_BUNDLED
}

/**
 * Asset library configuration
 */
data class AssetLibraryConfig(
    val id: String,
    val mode: AssetLoadMode,
    val popularCount: Int = 15,
    val essentialCount: Int = 100
)

/**
 * Predefined plugin configurations
 */
object PluginConfigs {
    /**
     * Ultra Minimal - For watch apps, widgets, embedded
     * Components: 5 (minimal)
     * Themes: None (CDN)
     * Assets: None (CDN)
     * Total Size: ~90 KB
     */
    val ULTRA_MINIMAL = PluginConfig(
        name = "Ultra Minimal",
        components = ComponentSet.MINIMAL,
        themes = ThemeSet.NONE,
        assets = AssetSet.NONE,
        enableCDN = true,
        autoCachePopular = false
    )

    /**
     * Minimal - For simple apps with CDN fallback
     * Components: 15 (essentials)
     * Themes: Single platform
     * Assets: Popular cached
     * Total Size: ~180 KB
     */
    val MINIMAL = PluginConfig(
        name = "Minimal",
        components = ComponentSet.ESSENTIALS,
        themes = ThemeSet.SINGLE_MATERIAL3,
        assets = AssetSet.POPULAR_ONLY,
        enableCDN = true,
        autoCachePopular = true
    )

    /**
     * Standard - Recommended for most apps
     * Components: 28 (standard)
     * Themes: Multi-platform
     * Assets: Popular cached
     * Total Size: ~350 KB
     */
    val STANDARD = PluginConfig(
        name = "Standard",
        components = ComponentSet.STANDARD,
        themes = ThemeSet.MULTI_PLATFORM,
        assets = AssetSet.POPULAR_ONLY,
        enableCDN = true,
        autoCachePopular = true
    )

    /**
     * Complete - For feature-rich apps
     * Components: 48 (all)
     * Themes: All
     * Assets: Essential pack
     * Total Size: ~1 MB
     */
    val COMPLETE = PluginConfig(
        name = "Complete",
        components = ComponentSet.COMPLETE,
        themes = ThemeSet.ALL,
        assets = AssetSet.ESSENTIAL_PACK,
        enableCDN = true,
        autoCachePopular = true
    )

    /**
     * Offline First - Maximum offline capability
     * Components: 48 (all)
     * Themes: All
     * Assets: Essential pack
     * Total Size: ~1.5 MB
     */
    val OFFLINE_FIRST = PluginConfig(
        name = "Offline First",
        components = ComponentSet.COMPLETE,
        themes = ThemeSet.ALL,
        assets = AssetSet.ESSENTIAL_PACK,
        enableCDN = false,
        autoCachePopular = false
    )

    /**
     * CDN Only - Absolute minimum bundle
     * Components: 5 (minimal)
     * Themes: None
     * Assets: None
     * Everything else loaded on-demand
     * Total Size: ~90 KB
     */
    val CDN_ONLY = PluginConfig(
        name = "CDN Only",
        components = ComponentSet.MINIMAL,
        themes = ThemeSet.NONE,
        assets = AssetSet.NONE,
        enableCDN = true,
        autoCachePopular = false
    )

    /**
     * Custom - Build your own
     */
    fun custom(
        components: ComponentSet = ComponentSet.ESSENTIALS,
        themes: ThemeSet = ThemeSet.SINGLE_MATERIAL3,
        assets: AssetSet = AssetSet.POPULAR_ONLY,
        enableCDN: Boolean = true,
        autoCachePopular: Boolean = true
    ) = PluginConfig(
        name = "Custom",
        components = components,
        themes = themes,
        assets = assets,
        enableCDN = enableCDN,
        autoCachePopular = autoCachePopular
    )
}

/**
 * Plugin configuration builder for Gradle
 */
class PluginConfigBuilder {
    private var config = PluginConfigs.STANDARD

    fun preset(preset: PluginConfig) = apply {
        config = preset
    }

    fun components(set: ComponentSet) = apply {
        config = config.copy(components = set)
    }

    fun themes(set: ThemeSet) = apply {
        config = config.copy(themes = set)
    }

    fun assets(set: AssetSet) = apply {
        config = config.copy(assets = set)
    }

    fun enableCDN(enable: Boolean) = apply {
        config = config.copy(enableCDN = enable)
    }

    fun autoCachePopular(enable: Boolean) = apply {
        config = config.copy(autoCachePopular = enable)
    }

    fun build(): PluginConfig = config
}

/**
 * Extension function for easy configuration
 */
fun magicElementsConfig(block: PluginConfigBuilder.() -> Unit): PluginConfig {
    return PluginConfigBuilder().apply(block).build()
}
