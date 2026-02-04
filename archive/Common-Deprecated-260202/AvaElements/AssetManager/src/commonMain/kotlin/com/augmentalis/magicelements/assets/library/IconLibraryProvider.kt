package com.augmentalis.avaelements.assets.library

import com.augmentalis.avaelements.assets.models.*

/**
 * Icon Library Provider - Plugin-based architecture
 *
 * Provides on-demand loading of icon libraries from remote sources
 * with local caching to avoid app bloat.
 *
 * Architecture:
 * - Metadata bundled in app (lightweight manifests)
 * - Icons loaded on-demand from CDN
 * - Cached locally for offline use
 * - No bloat - only download what you use
 */
interface IconLibraryProvider {
    /**
     * Library ID (e.g., "material", "fontawesome")
     */
    val libraryId: String

    /**
     * Library metadata
     */
    suspend fun getLibraryInfo(): AssetLibrary

    /**
     * Get icon manifest (metadata only, no icon data)
     *
     * @param limit Maximum icons to return
     * @param offset Pagination offset
     * @return List of icon metadata
     */
    suspend fun getIconManifest(
        limit: Int = 100,
        offset: Int = 0
    ): List<IconMetadata>

    /**
     * Search icon manifest
     *
     * @param query Search query
     * @return Matching icon metadata
     */
    suspend fun searchManifest(query: String): List<IconMetadata>

    /**
     * Load actual icon data (downloads if needed)
     *
     * @param iconId Icon identifier
     * @return Full Icon with SVG/PNG data
     */
    suspend fun loadIcon(iconId: String): Icon?

    /**
     * Preload a batch of icons (for offline use)
     *
     * @param iconIds Icons to preload
     * @param onProgress Progress callback (current, total)
     */
    suspend fun preloadIcons(
        iconIds: List<String>,
        onProgress: ((Int, Int) -> Unit)? = null
    )

    /**
     * Check if icon is cached locally
     *
     * @param iconId Icon to check
     * @return true if cached
     */
    suspend fun isCached(iconId: String): Boolean

    /**
     * Clear cached icons for this library
     */
    suspend fun clearCache()

    /**
     * Get cache statistics
     */
    suspend fun getCacheStats(): LibraryCacheStats
}

/**
 * Lightweight icon metadata (bundled in app)
 */
data class IconMetadata(
    /** Icon ID */
    val id: String,

    /** Icon name */
    val name: String,

    /** Search tags */
    val tags: List<String>,

    /** Category */
    val category: String? = null,

    /** Alternative names */
    val aliases: List<String> = emptyList(),

    /** Remote URL for icon data */
    val url: String? = null,

    /** File size (bytes) */
    val sizeBytes: Int = 0,

    /** Whether this icon is cached locally */
    var isCached: Boolean = false
)

/**
 * Library cache statistics
 */
data class LibraryCacheStats(
    /** Total icons in library */
    val totalIcons: Int,

    /** Cached icons count */
    val cachedIcons: Int,

    /** Cache size in bytes */
    val cacheSizeBytes: Long,

    /** Cache hit rate (0.0 to 1.0) */
    val hitRate: Float
) {
    val cachedPercentage: Float get() = if (totalIcons > 0) {
        (cachedIcons.toFloat() / totalIcons.toFloat()) * 100
    } else 0f
}

/**
 * Icon library configuration
 */
data class LibraryConfig(
    /** Library ID */
    val id: String,

    /** Library name */
    val name: String,

    /** Version */
    val version: String,

    /** Base CDN URL */
    val cdnBaseUrl: String,

    /** Manifest URL (JSON with all icon metadata) */
    val manifestUrl: String,

    /** License */
    val license: String,

    /** Whether to auto-cache popular icons */
    val autoCachePopular: Boolean = false,

    /** Popular icon IDs (will be auto-cached if enabled) */
    val popularIcons: List<String> = emptyList()
)

/**
 * Built-in library configurations
 */
object IconLibraries {
    /**
     * Material Icons (2,400+ icons)
     * CDN: Google Fonts / jsDelivr
     */
    val MATERIAL_ICONS = LibraryConfig(
        id = "material",
        name = "Material Icons",
        version = "1.0.0",
        cdnBaseUrl = "https://fonts.gstatic.com/s/i/materialicons",
        manifestUrl = "https://fonts.google.com/metadata/icons",
        license = "Apache 2.0",
        autoCachePopular = true,
        popularIcons = listOf(
            "home", "search", "settings", "menu", "close",
            "add", "remove", "edit", "delete", "check",
            "star", "favorite", "share", "person", "notifications"
        )
    )

    /**
     * Font Awesome (1,500+ free icons)
     * CDN: jsDelivr / cdnjs
     */
    val FONT_AWESOME = LibraryConfig(
        id = "fontawesome",
        name = "Font Awesome Free",
        version = "6.5.0",
        cdnBaseUrl = "https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.5.0/svgs",
        manifestUrl = "https://cdn.jsdelivr.net/npm/@fortawesome/fontawesome-free@6.5.0/metadata/icons.json",
        license = "CC BY 4.0, Font License",
        autoCachePopular = true,
        popularIcons = listOf(
            "house", "magnifying-glass", "gear", "bars", "xmark",
            "plus", "minus", "pen", "trash", "check",
            "star", "heart", "share", "user", "bell"
        )
    )

    /**
     * Custom icon library (user-uploaded)
     */
    fun custom(
        id: String,
        name: String,
        baseUrl: String
    ) = LibraryConfig(
        id = id,
        name = name,
        version = "1.0.0",
        cdnBaseUrl = baseUrl,
        manifestUrl = "$baseUrl/manifest.json",
        license = "Custom"
    )
}
