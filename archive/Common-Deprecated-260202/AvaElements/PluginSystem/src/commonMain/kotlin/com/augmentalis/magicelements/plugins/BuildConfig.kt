package com.augmentalis.avaelements.plugins

/**
 * Build-time plugin configuration
 *
 * This is how apps configure which plugins to include in builds.
 *
 * Usage in build.gradle.kts:
 * ```kotlin
 * magicElements {
 *     preset = PluginConfigs.STANDARD
 *     // OR customize
 *     components = ComponentSet.ESSENTIALS
 *     themes = ThemeSet.MULTI_PLATFORM
 *     assets = AssetSet.POPULAR_ONLY
 * }
 * ```
 */

/**
 * Global plugin configuration (set at build time)
 */
object AvaElementsConfig {
    /**
     * Current configuration
     * Set by Gradle plugin or manually
     */
    var current: PluginConfig = PluginConfigs.STANDARD
        private set

    /**
     * Apply configuration
     */
    fun configure(config: PluginConfig) {
        current = config
        println("""
            ╔═══════════════════════════════════════════════════════════════╗
            ║          AvaElements Plugin Configuration                  ║
            ╠═══════════════════════════════════════════════════════════════╣
            ║  Preset: ${config.name.padEnd(51)} ║
            ║  Components: ${config.components.name} (${config.components.count} components)${" ".repeat(maxOf(0, 30 - config.components.name.length - config.components.count.toString().length))} ║
            ║  Themes: ${config.themes.name} (${config.themes.count} themes)${" ".repeat(maxOf(0, 37 - config.themes.name.length - config.themes.count.toString().length))} ║
            ║  Assets: ${config.assets.name}${" ".repeat(maxOf(0, 51 - config.assets.name.length))} ║
            ║  CDN Enabled: ${if (config.enableCDN) "Yes" else "No "}${" ".repeat(45)} ║
            ║  Auto-cache Popular: ${if (config.autoCachePopular) "Yes" else "No "}${" ".repeat(37)} ║
            ╠═══════════════════════════════════════════════════════════════╣
            ║  Estimated Bundle Size: ${estimateBundleSize(config).padEnd(39)} ║
            ╚═══════════════════════════════════════════════════════════════╝
        """.trimIndent())
    }

    /**
     * Check if component is bundled
     */
    fun isComponentBundled(componentId: String): Boolean {
        return when (current.components) {
            ComponentSet.CUSTOM -> false // Custom handled separately
            else -> current.components.components.contains(componentId)
        }
    }

    /**
     * Check if theme is bundled
     */
    fun isThemeBundled(themeId: String): Boolean {
        return when (current.themes) {
            ThemeSet.CUSTOM -> false
            else -> current.themes.themes.contains(themeId)
        }
    }

    /**
     * Get asset library configuration
     */
    fun getAssetLibraryConfig(libraryId: String): AssetLibraryConfig? {
        return when (current.assets) {
            AssetSet.CUSTOM -> null
            else -> current.assets.libraries.find { it.id == libraryId }
        }
    }

    /**
     * Estimate bundle size
     */
    private fun estimateBundleSize(config: PluginConfig): String {
        var totalKB = 90 // Core + State Management

        // Components
        totalKB += when (config.components) {
            ComponentSet.MINIMAL -> 40
            ComponentSet.ESSENTIALS -> 120
            ComponentSet.STANDARD -> 250
            ComponentSet.COMPLETE -> 500
            ComponentSet.CUSTOM -> 0
        }

        // Themes
        totalKB += when (config.themes) {
            ThemeSet.NONE -> 0
            ThemeSet.SINGLE_MATERIAL3, ThemeSet.SINGLE_IOS26 -> 20
            ThemeSet.MULTI_PLATFORM -> 50
            ThemeSet.ALL -> 100
            ThemeSet.CUSTOM -> 0
        }

        // Assets
        totalKB += when (config.assets) {
            AssetSet.NONE -> 0
            AssetSet.POPULAR_ONLY -> 50
            AssetSet.METADATA_ONLY -> 10
            AssetSet.ESSENTIAL_PACK -> 400
            AssetSet.FULL_BUNDLE -> 8000
            AssetSet.CUSTOM -> 0
        }

        return when {
            totalKB < 1024 -> "$totalKB KB"
            else -> String.format("%.1f MB", totalKB / 1024.0)
        }
    }
}

/**
 * Plugin preset recommendations based on app type
 */
object PluginPresets {
    /**
     * Get recommended preset for app type
     */
    fun forAppType(appType: AppType): PluginConfig {
        return when (appType) {
            AppType.WATCH_APP -> PluginConfigs.ULTRA_MINIMAL
            AppType.WIDGET -> PluginConfigs.ULTRA_MINIMAL
            AppType.SIMPLE_UTILITY -> PluginConfigs.MINIMAL
            AppType.STANDARD_APP -> PluginConfigs.STANDARD
            AppType.FEATURE_RICH_APP -> PluginConfigs.COMPLETE
            AppType.OFFLINE_FIRST -> PluginConfigs.OFFLINE_FIRST
            AppType.COMPONENT_SHOWCASE -> PluginConfigs.COMPLETE
        }
    }

    /**
     * Get recommendations
     */
    fun recommendations(appType: AppType): String {
        val config = forAppType(appType)
        return """
            Recommended configuration for ${appType.displayName}:

            build.gradle.kts:
            ```kotlin
            magicElements {
                preset = PluginConfigs.${config.name.uppercase().replace(" ", "_")}

                // This gives you:
                // - ${config.components.count} components (${config.components.name})
                // - ${config.themes.count} themes (${config.themes.name})
                // - ${config.assets.name} assets
                // - CDN: ${if (config.enableCDN) "Enabled" else "Disabled"}
                // - Bundle size: ~${AvaElementsConfig.estimateBundleSize(config)}
            }
            ```

            ${getAppTypeSpecificTips(appType)}
        """.trimIndent()
    }

    private fun getAppTypeSpecificTips(appType: AppType): String {
        return when (appType) {
            AppType.WATCH_APP -> """
                Tips for Watch Apps:
                - Use ULTRA_MINIMAL to keep under 10 MB
                - Enable CDN for additional components
                - Pre-cache only icons you'll definitely use
            """.trimIndent()

            AppType.SIMPLE_UTILITY -> """
                Tips for Simple Utilities:
                - MINIMAL preset is usually enough
                - Add specific components you need
                - Let CDN handle the rest
            """.trimIndent()

            AppType.OFFLINE_FIRST -> """
                Tips for Offline-First Apps:
                - Use OFFLINE_FIRST preset
                - Bundle all components you might need
                - Consider ESSENTIAL_PACK for icons
                - Disable CDN if truly offline
            """.trimIndent()

            AppType.FEATURE_RICH_APP -> """
                Tips for Feature-Rich Apps:
                - COMPLETE is recommended
                - You'll use most components anyway
                - CDN provides flexibility for future features
            """.trimIndent()

            else -> ""
        }
    }
}

/**
 * App type classifications
 */
enum class AppType(val displayName: String) {
    WATCH_APP("Watch App / Wearable"),
    WIDGET("Home Screen Widget"),
    SIMPLE_UTILITY("Simple Utility App"),
    STANDARD_APP("Standard Mobile App"),
    FEATURE_RICH_APP("Feature-Rich App"),
    OFFLINE_FIRST("Offline-First App"),
    COMPONENT_SHOWCASE("Component Showcase / Demo")
}
