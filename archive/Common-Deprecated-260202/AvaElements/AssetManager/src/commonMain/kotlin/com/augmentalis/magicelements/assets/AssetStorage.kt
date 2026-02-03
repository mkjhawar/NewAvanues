package com.augmentalis.avaelements.assets

import com.augmentalis.avaelements.assets.models.*

/**
 * Asset storage interface
 *
 * Provides persistent storage for icons and images with
 * efficient retrieval, caching, and search capabilities.
 */
interface AssetStorage {
    /**
     * Store an icon
     *
     * @param icon Icon to store
     */
    suspend fun storeIcon(icon: Icon)

    /**
     * Store multiple icons (batch operation)
     *
     * @param icons Icons to store
     */
    suspend fun storeIcons(icons: List<Icon>)

    /**
     * Get icon by ID
     *
     * @param iconId Icon identifier
     * @return Icon or null if not found
     */
    suspend fun getIcon(iconId: String): Icon?

    /**
     * Store an image
     *
     * @param image Image to store
     */
    suspend fun storeImage(image: ImageAsset)

    /**
     * Get image by ID
     *
     * @param imageId Image identifier
     * @return ImageAsset or null if not found
     */
    suspend fun getImage(imageId: String): ImageAsset?

    /**
     * Search icons
     *
     * @param query Search query
     * @return Search results
     */
    suspend fun searchIcons(query: AssetQuery): AssetSearchResult<Icon>

    /**
     * Search images
     *
     * @param query Search query
     * @return Search results
     */
    suspend fun searchImages(query: AssetQuery): AssetSearchResult<ImageAsset>

    /**
     * Get all libraries
     *
     * @return List of available libraries
     */
    suspend fun getLibraries(): List<AssetLibrary>

    /**
     * Register a library
     *
     * @param library Library to register
     */
    suspend fun registerLibrary(library: AssetLibrary)

    /**
     * Get icons from a specific library
     *
     * @param libraryId Library identifier
     * @param limit Maximum results
     * @param offset Pagination offset
     * @return List of icons
     */
    suspend fun getLibraryIcons(
        libraryId: String,
        limit: Int = 100,
        offset: Int = 0
    ): List<Icon>

    /**
     * Delete icon
     *
     * @param iconId Icon to delete
     * @return true if deleted, false if not found
     */
    suspend fun deleteIcon(iconId: String): Boolean

    /**
     * Delete image
     *
     * @param imageId Image to delete
     * @return true if deleted, false if not found
     */
    suspend fun deleteImage(imageId: String): Boolean

    /**
     * Clear all icons from a library
     *
     * @param libraryId Library to clear
     */
    suspend fun clearLibrary(libraryId: String)

    /**
     * Get storage statistics
     *
     * @return Storage stats
     */
    suspend fun getStats(): StorageStats
}

/**
 * Storage statistics
 */
data class StorageStats(
    val totalIcons: Int,
    val totalImages: Int,
    val libraryCount: Int,
    val totalSizeBytes: Long,
    val cacheHitRate: Float? = null
)
