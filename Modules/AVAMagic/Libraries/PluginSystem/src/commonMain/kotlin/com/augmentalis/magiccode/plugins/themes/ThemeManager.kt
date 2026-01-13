package com.augmentalis.avacode.plugins.themes

import com.augmentalis.avacode.plugins.assets.AssetResolver
import com.augmentalis.avacode.plugins.assets.readText
import com.augmentalis.avacode.plugins.core.PluginLog
import com.augmentalis.avacode.plugins.core.PluginRegistry
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.decodeFromString
import net.mamoe.yamlkt.Yaml

/**
 * Theme manager for loading and managing plugin themes.
 *
 * Handles theme YAML parsing, validation, asset resolution, and hot-reloading.
 * Implements FR-005 (Theme definition) and FR-014 (Hot-reloading support).
 */
class ThemeManager(
    private val registry: PluginRegistry,
    private val assetResolver: AssetResolver,
    private val validator: ThemeValidator = ThemeValidator(),
    private val fontLoader: FontLoader = FontLoader()
) {
    private val mutex = Mutex()
    private val loadedThemes = mutableMapOf<String, LoadedTheme>()

    companion object {
        private const val TAG = "ThemeManager"
    }

    /**
     * Loaded theme with metadata.
     */
    data class LoadedTheme(
        val definition: ThemeDefinition,
        val pluginId: String,
        val loadedAt: Long,
        val assetReferences: Map<String, String> = emptyMap(),
        val loadedFonts: List<String> = emptyList()
    )

    /**
     * Theme load result.
     */
    sealed class LoadResult {
        data class Success(val theme: ThemeDefinition) : LoadResult()
        data class Failure(val reason: String, val errors: List<ThemeValidator.ValidationError> = emptyList()) : LoadResult()
    }

    /**
     * Load theme from plugin asset.
     *
     * @param pluginId Plugin identifier
     * @param themeFilename Theme YAML filename (e.g., "dark-theme.yaml")
     * @return LoadResult
     */
    suspend fun loadTheme(pluginId: String, themeFilename: String): LoadResult {
        PluginLog.i(TAG, "Loading theme: $pluginId/$themeFilename")

        try {
            // Step 1: Verify plugin exists
            val pluginInfo = registry.getPlugin(pluginId)
            if (pluginInfo == null) {
                val error = "Plugin not found: $pluginId"
                PluginLog.e(TAG, error)
                return LoadResult.Failure(error)
            }

            // Step 2: Resolve theme asset
            val themeUri = "plugin://$pluginId/themes/$themeFilename"
            val resolveResult = assetResolver.resolveAsset(themeUri, useFallback = false)

            val themeHandle = when (resolveResult) {
                is AssetResolver.ResolutionResult.Success -> resolveResult.assetHandle
                is AssetResolver.ResolutionResult.Failure -> {
                    val error = "Failed to resolve theme asset: ${resolveResult.reason}"
                    PluginLog.e(TAG, error)
                    return LoadResult.Failure(error)
                }
            }

            // Step 3: Read theme YAML content
            val yamlContent = try {
                themeHandle.readText()
            } catch (e: Exception) {
                val error = "Failed to read theme file: ${e.message}"
                PluginLog.e(TAG, error, e)
                return LoadResult.Failure(error)
            }

            // Step 4: Parse YAML into ThemeDefinition
            val theme = try {
                parseThemeYaml(yamlContent)
            } catch (e: Exception) {
                val error = "Failed to parse theme YAML: ${e.message}"
                PluginLog.e(TAG, error, e)
                return LoadResult.Failure(error)
            }

            // Step 5: Validate theme
            when (val validationResult = validator.validate(theme)) {
                is ThemeValidator.ValidationResult.Valid -> {
                    PluginLog.d(TAG, "Theme validation passed: ${theme.name}")
                }
                is ThemeValidator.ValidationResult.Invalid -> {
                    val errorMessages = validationResult.errors.joinToString("; ") {
                        "${it.field}: ${it.message}"
                    }
                    PluginLog.e(TAG, "Theme validation failed: $errorMessages")
                    return LoadResult.Failure(
                        "Theme validation failed",
                        validationResult.errors
                    )
                }
            }

            // Step 6: Resolve theme asset references (custom fonts, images)
            val assetReferences = resolveThemeAssets(theme, pluginId)

            // Step 7: Get list of successfully loaded fonts
            val loadedFontsList = theme.typography.customFonts.keys.filter { fontFamily ->
                fontLoader.isFontLoaded(fontFamily)
            }

            // Step 8: Register loaded theme
            mutex.withLock {
                val themeId = theme.getThemeId()
                loadedThemes[themeId] = LoadedTheme(
                    definition = theme,
                    pluginId = pluginId,
                    loadedAt = System.currentTimeMillis(),
                    assetReferences = assetReferences,
                    loadedFonts = loadedFontsList
                )
                PluginLog.i(TAG, "Theme loaded successfully: $themeId from plugin $pluginId (${loadedFontsList.size} custom fonts)")
            }

            return LoadResult.Success(theme)

        } catch (e: Exception) {
            val error = "Unexpected error loading theme: ${e.message}"
            PluginLog.e(TAG, error, e)
            return LoadResult.Failure(error)
        }
    }

    /**
     * Parse theme YAML content.
     *
     * @param yamlContent YAML string
     * @return ThemeDefinition
     * @throws Exception if parsing fails
     */
    private fun parseThemeYaml(yamlContent: String): ThemeDefinition {
        return try {
            Yaml.Default.decodeFromString(ThemeDefinition.serializer(), yamlContent)
        } catch (e: Exception) {
            throw Exception("Invalid theme YAML format: ${e.message}", e)
        }
    }

    /**
     * Resolve theme asset references (custom fonts, preview images, etc.).
     *
     * @param theme Theme definition
     * @param pluginId Plugin identifier
     * @return Map of asset key to resolved path
     */
    private suspend fun resolveThemeAssets(
        theme: ThemeDefinition,
        pluginId: String
    ): Map<String, String> {
        val assetReferences = mutableMapOf<String, String>()

        // Resolve preview image if specified
        theme.metadata?.preview?.let { previewPath ->
            val uri = "plugin://$pluginId/images/$previewPath"
            when (val result = assetResolver.resolveAsset(uri, useFallback = false)) {
                is AssetResolver.ResolutionResult.Success -> {
                    assetReferences["preview"] = result.assetHandle.absolutePath
                    PluginLog.d(TAG, "Resolved theme preview: $previewPath")
                }
                is AssetResolver.ResolutionResult.Failure -> {
                    PluginLog.w(TAG, "Failed to resolve theme preview: ${result.reason}")
                }
            }
        }

        // Resolve and load custom fonts if specified in typography
        val customFonts = theme.typography.customFonts
        if (customFonts.isNotEmpty()) {
            PluginLog.d(TAG, "Loading ${customFonts.size} custom fonts for theme: ${theme.name}")

            for ((fontFamily, fontFileName) in customFonts) {
                val fontUri = "plugin://$pluginId/fonts/$fontFileName"

                when (val result = assetResolver.resolveAsset(fontUri, useFallback = false)) {
                    is AssetResolver.ResolutionResult.Success -> {
                        val fontPath = result.assetHandle.absolutePath
                        assetReferences["font:$fontFamily"] = fontPath

                        // Load the font using FontLoader
                        val loadResult = fontLoader.loadFont(fontFamily, fontPath)
                        FontLoaderUtils.logLoadResult(loadResult)

                        when (loadResult) {
                            is FontLoadResult.Success -> {
                                PluginLog.i(TAG, "Custom font loaded: $fontFamily for theme ${theme.name}")
                            }
                            is FontLoadResult.Failure -> {
                                PluginLog.w(TAG, "Failed to load custom font $fontFamily: ${loadResult.reason}")
                            }
                        }
                    }
                    is AssetResolver.ResolutionResult.Failure -> {
                        PluginLog.w(TAG, "Failed to resolve font asset $fontFileName: ${result.reason}")
                    }
                }
            }
        }

        return assetReferences
    }

    /**
     * Get loaded theme by ID.
     *
     * @param themeId Theme identifier
     * @return LoadedTheme or null if not found
     */
    suspend fun getTheme(themeId: String): LoadedTheme? {
        return mutex.withLock {
            loadedThemes[themeId]
        }
    }

    /**
     * Get all loaded themes.
     *
     * @return List of loaded themes
     */
    suspend fun getAllThemes(): List<LoadedTheme> {
        return mutex.withLock {
            loadedThemes.values.toList()
        }
    }

    /**
     * Get themes for a specific plugin.
     *
     * @param pluginId Plugin identifier
     * @return List of themes from this plugin
     */
    suspend fun getThemesByPlugin(pluginId: String): List<LoadedTheme> {
        return mutex.withLock {
            loadedThemes.values.filter { it.pluginId == pluginId }
        }
    }

    /**
     * Reload theme (for hot-reloading support - FR-014).
     *
     * @param themeId Theme identifier
     * @return LoadResult
     */
    suspend fun reloadTheme(themeId: String): LoadResult {
        PluginLog.i(TAG, "Reloading theme: $themeId")

        val existingTheme = mutex.withLock {
            loadedThemes[themeId]
        }

        if (existingTheme == null) {
            return LoadResult.Failure("Theme not found: $themeId")
        }

        // Find theme filename from loaded theme
        val themeFilename = "${themeId.split(".").last()}.yaml"

        // Reload from plugin
        return loadTheme(existingTheme.pluginId, themeFilename)
    }

    /**
     * Unload theme.
     *
     * @param themeId Theme identifier
     * @return true if unloaded, false if not found
     */
    suspend fun unloadTheme(themeId: String): Boolean {
        return mutex.withLock {
            val removed = loadedThemes.remove(themeId)
            if (removed != null) {
                // Unload custom fonts if they were loaded for this theme
                for (fontFamily in removed.loadedFonts) {
                    val unloaded = fontLoader.unloadFont(fontFamily)
                    if (unloaded) {
                        PluginLog.d(TAG, "Unloaded font: $fontFamily from theme $themeId")
                    }
                }

                PluginLog.i(TAG, "Theme unloaded: $themeId (${removed.loadedFonts.size} custom fonts unloaded)")
                true
            } else {
                PluginLog.w(TAG, "Theme not found for unload: $themeId")
                false
            }
        }
    }

    /**
     * Unload all themes from a plugin.
     *
     * @param pluginId Plugin identifier
     * @return Number of themes unloaded
     */
    suspend fun unloadPluginThemes(pluginId: String): Int {
        return mutex.withLock {
            val themesToRemove = loadedThemes.filter { it.value.pluginId == pluginId }
            themesToRemove.keys.forEach { themeId ->
                loadedThemes.remove(themeId)
            }
            if (themesToRemove.isNotEmpty()) {
                PluginLog.i(TAG, "Unloaded ${themesToRemove.size} themes from plugin: $pluginId")
            }
            themesToRemove.size
        }
    }

    /**
     * Clear all loaded themes.
     */
    suspend fun clearAllThemes() {
        mutex.withLock {
            val count = loadedThemes.size
            loadedThemes.clear()
            PluginLog.i(TAG, "Cleared all themes: $count themes unloaded")
        }
    }

    /**
     * Get theme manager statistics.
     *
     * @return Map of statistics
     */
    suspend fun getStats(): Map<String, Any> {
        return mutex.withLock {
            val allLoadedFonts = fontLoader.getAllLoadedFonts()
            mapOf(
                "totalThemes" to loadedThemes.size,
                "themesByPlugin" to loadedThemes.values.groupBy { it.pluginId }.mapValues { it.value.size },
                "totalCustomFonts" to allLoadedFonts.size,
                "fontFormats" to allLoadedFonts.groupBy { it.format }.mapValues { it.value.size }
            )
        }
    }

    /**
     * Get font loader instance.
     * Useful for direct font operations or querying font information.
     *
     * @return FontLoader instance
     */
    fun getFontLoader(): FontLoader {
        return fontLoader
    }

    /**
     * Get font fallback chain for a theme.
     *
     * @param themeId Theme identifier
     * @return List of font families in fallback order, or empty list if theme not found
     */
    suspend fun getFontFallbackChain(themeId: String): List<String> {
        val theme = getTheme(themeId) ?: return emptyList()
        val typography = theme.definition.typography

        // Build fallback chain using primary font, custom fallbacks, and platform defaults
        val platformDefaults = listOf("sans-serif", "serif", "monospace")
        return FontLoaderUtils.buildFallbackChain(
            typography.fontFamily,
            typography.fontFallback,
            platformDefaults
        )
    }

    /**
     * Check if all custom fonts for a theme are loaded successfully.
     *
     * @param themeId Theme identifier
     * @return true if all fonts loaded, false if any failed or theme not found
     */
    suspend fun areAllFontsLoaded(themeId: String): Boolean {
        val theme = getTheme(themeId) ?: return false
        val customFonts = theme.definition.typography.customFonts

        if (customFonts.isEmpty()) {
            return true  // No custom fonts to load
        }

        return customFonts.keys.all { fontFamily ->
            fontLoader.isFontLoaded(fontFamily)
        }
    }
}
