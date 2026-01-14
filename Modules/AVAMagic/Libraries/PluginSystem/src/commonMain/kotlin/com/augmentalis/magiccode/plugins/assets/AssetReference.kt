package com.augmentalis.magiccode.plugins.assets

import com.augmentalis.magiccode.plugins.core.AssetCategory

/**
 * Reference to a plugin asset with URI parsing and resolution tracking.
 *
 * AssetReference is an immutable data class representing a structured reference
 * to an asset within a plugin. It encapsulates the plugin ID, asset category,
 * and filename, with optional resolved filesystem path.
 *
 * ## URI Format
 * AssetReference can be created from plugin:// URIs with the format:
 * ```
 * plugin://<plugin-id>/<category>/<filename>
 * ```
 *
 * ## Example URIs
 * ```
 * plugin://com.augmentalis.theme-pack/themes/dark-theme.yaml
 * plugin://com.example.icons/icons/home.png
 * plugin://my.plugin/fonts/custom-font.ttf
 * plugin://demo.plugin/images/logo/banner.jpg  // Nested paths supported
 * ```
 *
 * ## Categories
 * Valid asset categories (case-insensitive in URIs):
 * - **FONTS**: Font files (.ttf, .otf, .woff, .woff2)
 * - **ICONS**: Icon files (.png, .svg, .ico)
 * - **IMAGES**: Image files (.png, .jpg, .svg, .webp)
 * - **THEMES**: Theme configuration (.yaml, .json)
 * - **CUSTOM**: Custom asset types
 *
 * ## Resolution Lifecycle
 * 1. **Unresolved**: Created from URI or components, resolvedPath is null
 * 2. **Resolved**: After resolution by AssetResolver, resolvedPath contains absolute filesystem path
 *
 * ## Example Usage
 * ```kotlin
 * // Parse from URI
 * val ref = AssetReference.fromUri("plugin://my.plugin/icons/app.png")
 * println(ref?.pluginId) // "my.plugin"
 * println(ref?.category) // AssetCategory.ICONS
 * println(ref?.filename) // "app.png"
 * println(ref?.isResolved()) // false
 *
 * // After resolution
 * val resolved = ref?.withResolvedPath("/path/to/plugins/my.plugin/assets/icons/app.png")
 * println(resolved?.isResolved()) // true
 *
 * // Convert back to URI
 * println(resolved?.toUri()) // "plugin://my.plugin/icons/app.png"
 * ```
 *
 * ## Immutability
 * AssetReference is immutable. Use [withResolvedPath] to create a new instance
 * with a resolved path.
 *
 * @property pluginId Plugin identifier that owns this asset (e.g., "com.augmentalis.theme-pack")
 * @property category Asset category classification
 * @property filename Filename within the category directory (may include subdirectories)
 * @property resolvedPath Absolute filesystem path after resolution, null if unresolved
 * @since 1.0.0
 * @see AssetResolver
 * @see AssetCategory
 */
data class AssetReference(
    /**
     * Plugin identifier that owns this asset.
     * Example: "com.augmentalis.theme-pack"
     */
    val pluginId: String,

    /**
     * Asset category (fonts, icons, images, themes, custom).
     */
    val category: AssetCategory,

    /**
     * Filename within the category directory.
     * Example: "dark-theme.yaml" or "logo.png"
     */
    val filename: String,

    /**
     * Full asset path within plugin namespace.
     * Example: "/path/to/plugins/com.augmentalis.theme-pack/assets/themes/dark-theme.yaml"
     * Null until resolved.
     */
    val resolvedPath: String? = null
) {
    companion object {
        /**
         * Create AssetReference from plugin:// URI.
         *
         * Parses a plugin asset URI into structured AssetReference components.
         * Returns null if the URI format is invalid or category is unrecognized.
         *
         * ## Format
         * ```
         * plugin://plugin-id/category/filename
         * ```
         *
         * ## Examples
         * ```kotlin
         * val ref1 = AssetReference.fromUri("plugin://com.augmentalis.theme-pack/themes/dark-theme.yaml")
         * // pluginId: "com.augmentalis.theme-pack"
         * // category: AssetCategory.THEMES
         * // filename: "dark-theme.yaml"
         *
         * val ref2 = AssetReference.fromUri("plugin://my.plugin/images/logo/banner.jpg")
         * // pluginId: "my.plugin"
         * // category: AssetCategory.IMAGES
         * // filename: "logo/banner.jpg" (nested path)
         *
         * val invalid = AssetReference.fromUri("https://example.com/file.png")
         * // Returns null (not a plugin:// URI)
         * ```
         *
         * @param uri Plugin asset URI to parse
         * @return AssetReference if valid, null if invalid format or unknown category
         * @since 1.0.0
         */
        fun fromUri(uri: String): AssetReference? {
            if (!uri.startsWith("plugin://")) {
                return null
            }

            // Remove plugin:// prefix
            val path = uri.substring(9) // "plugin://".length == 9

            // Split into components: plugin-id/category/filename
            val parts = path.split("/")
            if (parts.size < 3) {
                return null
            }

            val pluginId = parts[0]
            val categoryStr = parts[1]
            val filename = parts.drop(2).joinToString("/") // Handle filenames with slashes

            // Parse category
            val category = try {
                AssetCategory.valueOf(categoryStr.uppercase())
            } catch (e: IllegalArgumentException) {
                return null
            }

            return AssetReference(
                pluginId = pluginId,
                category = category,
                filename = filename
            )
        }
    }

    /**
     * Convert to plugin:// URI.
     *
     * Reconstructs the URI from components. The URI is independent of
     * resolution state and always uses the original plugin ID, category,
     * and filename.
     *
     * @return Plugin asset URI (e.g., "plugin://my.plugin/icons/app.png")
     * @since 1.0.0
     */
    fun toUri(): String {
        return "plugin://$pluginId/${category.name.lowercase()}/$filename"
    }

    /**
     * Check if asset has been resolved.
     *
     * Indicates whether this reference has been resolved to an absolute
     * filesystem path by AssetResolver.
     *
     * @return true if resolvedPath is non-null, false otherwise
     * @since 1.0.0
     * @see withResolvedPath
     */
    fun isResolved(): Boolean {
        return resolvedPath != null
    }

    /**
     * Create resolved copy with path.
     *
     * Returns a new AssetReference instance with the resolvedPath set.
     * Does not modify the original instance (immutable).
     *
     * ## Example
     * ```kotlin
     * val unresolved = AssetReference.fromUri("plugin://my.plugin/icons/app.png")
     * val resolved = unresolved?.withResolvedPath("/path/to/plugins/my.plugin/assets/icons/app.png")
     * println(resolved?.isResolved()) // true
     * ```
     *
     * @param path Resolved absolute filesystem path
     * @return New AssetReference with resolvedPath set
     * @since 1.0.0
     * @see isResolved
     */
    fun withResolvedPath(path: String): AssetReference {
        return copy(resolvedPath = path)
    }

    override fun toString(): String {
        return if (resolvedPath != null) {
            "AssetReference(${toUri()} -> $resolvedPath)"
        } else {
            "AssetReference(${toUri()})"
        }
    }
}
