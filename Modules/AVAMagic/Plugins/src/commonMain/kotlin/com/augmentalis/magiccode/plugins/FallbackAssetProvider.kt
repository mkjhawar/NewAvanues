package com.augmentalis.avacode.plugins

import com.augmentalis.avacode.plugins.AssetCategory
import com.augmentalis.avacode.plugins.PluginLog

/**
 * Fallback asset provider for default assets.
 *
 * Provides default/fallback assets when plugin assets cannot be resolved.
 * Implements FR-013 (Fallback mechanism for missing assets).
 */
class FallbackAssetProvider(
    private val fallbackAssetsDir: String = "/system/fallback-assets"
) {
    companion object {
        private const val TAG = "FallbackAssetProvider"

        // Default fallback filenames by category
        private val DEFAULT_FALLBACKS = mapOf(
            AssetCategory.ICONS to "default-icon.svg",
            AssetCategory.IMAGES to "placeholder-image.png",
            AssetCategory.FONTS to "default-font.ttf",
            AssetCategory.THEMES to "default-theme.yaml",
            AssetCategory.CUSTOM to null // No default for custom assets
        )
    }

    /**
     * Custom fallback registry for runtime-registered fallbacks.
     *
     * Maps AssetCategory to a priority-ordered list of fallback paths.
     * Higher priority (lower index) fallbacks are tried first.
     */
    private val customFallbacks = mutableMapOf<AssetCategory, MutableList<FallbackEntry>>()

    /**
     * Fallback entry with priority and metadata.
     *
     * @property path Absolute path to fallback asset
     * @property priority Priority level (0 = highest, higher values = lower priority)
     * @property description Optional description of this fallback
     */
    data class FallbackEntry(
        val path: String,
        val priority: Int = 0,
        val description: String? = null
    )

    /**
     * Get fallback asset path for a category and filename.
     *
     * Tries custom fallbacks first (in priority order), then falls back to
     * the default system fallback.
     *
     * @param category Asset category
     * @param filename Original filename (used for type matching)
     * @return Absolute path to fallback asset, or null if no fallback available
     */
    fun getFallbackAsset(category: AssetCategory, filename: String): String? {
        PluginLog.d(TAG, "Looking for fallback: $category/$filename")

        // Try custom fallbacks first (in priority order)
        val customFallbackList = customFallbacks[category]
        if (customFallbackList != null && customFallbackList.isNotEmpty()) {
            // Return highest priority custom fallback
            val highestPriority = customFallbackList.first()
            PluginLog.d(TAG, "Using custom fallback (priority ${highestPriority.priority}): ${highestPriority.path}")
            return highestPriority.path
        }

        // Fall back to default system fallback
        val defaultFallback = DEFAULT_FALLBACKS[category]
        if (defaultFallback == null) {
            PluginLog.w(TAG, "No fallback available for category: $category")
            return null
        }

        // Construct fallback path
        val fallbackPath = "$fallbackAssetsDir/${category.getSubdirectoryPath()}/$defaultFallback"

        PluginLog.d(TAG, "Fallback asset: $fallbackPath")
        return fallbackPath
    }

    /**
     * Get all available fallback assets for a category.
     *
     * Returns custom fallbacks (in priority order) followed by default fallback.
     *
     * @param category Asset category
     * @return List of fallback asset paths (ordered by priority)
     */
    fun getAllFallbackAssets(category: AssetCategory): List<String> {
        val allFallbacks = mutableListOf<String>()

        // Add custom fallbacks (already sorted by priority)
        customFallbacks[category]?.let { entries ->
            allFallbacks.addAll(entries.map { it.path })
        }

        // Add default fallback
        val defaultFallback = DEFAULT_FALLBACKS[category]
        if (defaultFallback != null) {
            allFallbacks.add("$fallbackAssetsDir/${category.getSubdirectoryPath()}/$defaultFallback")
        }

        return allFallbacks
    }

    /**
     * Check if fallback is available for a category.
     *
     * Returns true if either custom fallbacks or default fallback exists.
     *
     * @param category Asset category
     * @return true if fallback is available
     */
    fun hasFallback(category: AssetCategory): Boolean {
        return customFallbacks[category]?.isNotEmpty() == true ||
               DEFAULT_FALLBACKS[category] != null
    }

    /**
     * Register custom fallback for a category.
     *
     * This allows runtime registration of fallback assets with priority-based
     * selection. Lower priority values are tried first.
     *
     * ## Example
     * ```kotlin
     * val provider = FallbackAssetProvider()
     *
     * // Register high-priority custom icon fallback
     * provider.registerCustomFallback(
     *     category = AssetCategory.ICONS,
     *     fallbackPath = "/custom/high-res-icon.svg",
     *     priority = 0,
     *     description = "High-resolution custom icon"
     * )
     *
     * // Register lower-priority fallback
     * provider.registerCustomFallback(
     *     category = AssetCategory.ICONS,
     *     fallbackPath = "/custom/low-res-icon.png",
     *     priority = 10,
     *     description = "Low-resolution fallback"
     * )
     * ```
     *
     * @param category Asset category
     * @param fallbackPath Absolute path to fallback asset
     * @param priority Priority level (default: 0 = highest priority)
     * @param description Optional description of this fallback
     */
    fun registerCustomFallback(
        category: AssetCategory,
        fallbackPath: String,
        priority: Int = 0,
        description: String? = null
    ) {
        PluginLog.i(TAG, "Registering custom fallback: $category -> $fallbackPath (priority=$priority)")

        // Get or create the list for this category
        val fallbackList = customFallbacks.getOrPut(category) { mutableListOf() }

        // Create fallback entry
        val entry = FallbackEntry(
            path = fallbackPath,
            priority = priority,
            description = description
        )

        // Add and sort by priority (ascending)
        fallbackList.add(entry)
        fallbackList.sortBy { it.priority }

        PluginLog.d(TAG, "Custom fallback registered. Total fallbacks for $category: ${fallbackList.size}")
    }

    /**
     * Unregister a custom fallback by path.
     *
     * @param category Asset category
     * @param fallbackPath Absolute path to fallback asset to remove
     * @return true if fallback was found and removed, false otherwise
     */
    fun unregisterCustomFallback(category: AssetCategory, fallbackPath: String): Boolean {
        val fallbackList = customFallbacks[category] ?: return false

        val removed = fallbackList.removeAll { it.path == fallbackPath }
        if (removed) {
            PluginLog.i(TAG, "Unregistered custom fallback: $category -> $fallbackPath")

            // Clean up empty list
            if (fallbackList.isEmpty()) {
                customFallbacks.remove(category)
            }
        }

        return removed
    }

    /**
     * Clear all custom fallbacks for a category.
     *
     * @param category Asset category (if null, clears all categories)
     */
    fun clearCustomFallbacks(category: AssetCategory? = null) {
        if (category != null) {
            customFallbacks.remove(category)
            PluginLog.i(TAG, "Cleared all custom fallbacks for category: $category")
        } else {
            customFallbacks.clear()
            PluginLog.i(TAG, "Cleared all custom fallbacks")
        }
    }

    /**
     * Get all custom fallback entries for a category.
     *
     * @param category Asset category
     * @return List of fallback entries (in priority order)
     */
    fun getCustomFallbacks(category: AssetCategory): List<FallbackEntry> {
        return customFallbacks[category]?.toList() ?: emptyList()
    }

    /**
     * Get count of custom fallbacks for a category.
     *
     * @param category Asset category
     * @return Number of registered custom fallbacks
     */
    fun getCustomFallbackCount(category: AssetCategory): Int {
        return customFallbacks[category]?.size ?: 0
    }

    /**
     * Get fallback asset directory.
     *
     * @return Absolute path to fallback assets directory
     */
    fun getFallbackAssetsDir(): String {
        return fallbackAssetsDir
    }
}
