package com.augmentalis.magicelements.core.resources

/**
 * Platform-agnostic interface for loading and managing icons
 *
 * Implementations handle:
 * - Icon loading from different sources
 * - Caching (memory and disk)
 * - Icon tinting/coloring
 * - Size variants
 *
 * @since 3.0.0-flutter-parity
 */
interface IconResourceManager {
    /**
     * Load an icon resource
     *
     * @param resource Icon resource to load
     * @param size Desired icon size
     * @param tint Optional tint color (hex string)
     * @return Platform-specific icon representation
     */
    suspend fun loadIcon(
        resource: IconResource,
        size: IconSize = IconSize.Standard,
        tint: String? = null
    ): Any

    /**
     * Preload icons into cache
     *
     * @param resources List of icons to preload
     */
    suspend fun preloadIcons(resources: List<IconResource>)

    /**
     * Clear icon cache
     *
     * @param memoryOnly If true, only clear memory cache
     */
    fun clearCache(memoryOnly: Boolean = false)

    /**
     * Get cache statistics
     *
     * @return Cache statistics (memory size, disk size, hit rate)
     */
    fun getCacheStats(): CacheStats

    /**
     * Check if an icon is available in cache
     *
     * @param resource Icon resource to check
     * @return True if icon is cached
     */
    fun isCached(resource: IconResource): Boolean

    /**
     * Cache statistics
     */
    data class CacheStats(
        val memorySizeBytes: Long,
        val diskSizeBytes: Long,
        val memoryHitRate: Float,
        val diskHitRate: Float,
        val totalRequests: Long,
        val cacheHits: Long,
        val cacheMisses: Long
    )
}

/**
 * Icon loading state for async operations
 */
sealed class IconLoadState {
    /** Icon is loading */
    object Loading : IconLoadState()

    /** Icon loaded successfully */
    data class Success(val icon: Any) : IconLoadState()

    /** Icon loading failed */
    data class Error(val message: String, val fallbackIcon: Any? = null) : IconLoadState()
}
