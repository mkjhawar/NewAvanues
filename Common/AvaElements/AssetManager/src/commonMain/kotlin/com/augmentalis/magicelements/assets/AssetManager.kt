package com.augmentalis.avaelements.assets

import com.augmentalis.avaelements.assets.models.*

/**
 * Asset Manager - Main API for asset management
 *
 * Provides high-level API for managing icons and images,
 * including built-in libraries (Material Icons, Font Awesome),
 * search, and caching.
 *
 * Usage:
 * ```kotlin
 * // Initialize
 * val assetManager = AssetManager.getInstance()
 *
 * // Load built-in libraries
 * assetManager.loadMaterialIcons()
 * assetManager.loadFontAwesome()
 *
 * // Search icons
 * val icons = assetManager.searchIcons("home")
 *
 * // Get specific icon
 * val icon = assetManager.getIcon("material:home")
 * ```
 */
class AssetManager internal constructor(
    private val storage: AssetStorage,
    private val processor: AssetProcessor
) {
    /**
     * Get icon by ID
     *
     * @param iconId Icon identifier (format: "library:name" or just "name")
     * @return Icon or null if not found
     */
    suspend fun getIcon(iconId: String): Icon? {
        return storage.getIcon(iconId)
    }

    /**
     * Get image by ID
     *
     * @param imageId Image identifier
     * @return ImageAsset or null if not found
     */
    suspend fun getImage(imageId: String): ImageAsset? {
        return storage.getImage(imageId)
    }

    /**
     * Search icons by query
     *
     * @param queryText Search text (searches name, tags, aliases)
     * @param library Optional library filter
     * @param category Optional category filter
     * @param limit Maximum results (default: 50)
     * @return Search results
     */
    suspend fun searchIcons(
        queryText: String,
        library: String? = null,
        category: String? = null,
        limit: Int = 50
    ): List<Icon> {
        val query = AssetQuery(
            query = queryText,
            library = library,
            category = category,
            limit = limit
        )
        return storage.searchIcons(query).results
    }

    /**
     * Search images by query
     *
     * @param queryText Search text
     * @param limit Maximum results
     * @return Search results
     */
    suspend fun searchImages(
        queryText: String,
        limit: Int = 50
    ): List<ImageAsset> {
        val query = AssetQuery(query = queryText, limit = limit)
        return storage.searchImages(query).results
    }

    /**
     * Upload and process a custom icon
     *
     * @param fileData Icon file bytes
     * @param fileName Original filename
     * @param tags Optional tags for search
     * @return Processed and stored Icon
     */
    suspend fun uploadIcon(
        fileData: ByteArray,
        fileName: String,
        tags: List<String> = emptyList()
    ): Icon {
        val iconId = generateIconId(fileName)
        val icon = processor.processIcon(fileData, fileName, iconId)
        val iconWithTags = icon.copy(tags = icon.tags + tags)
        storage.storeIcon(iconWithTags)
        return iconWithTags
    }

    /**
     * Upload and process a custom image
     *
     * @param fileData Image file bytes
     * @param fileName Original filename
     * @param tags Optional tags for search
     * @return Processed and stored ImageAsset
     */
    suspend fun uploadImage(
        fileData: ByteArray,
        fileName: String,
        tags: List<String> = emptyList()
    ): ImageAsset {
        val imageId = generateImageId(fileName)
        val image = processor.processImage(fileData, fileName, imageId)
        val imageWithTags = image.copy(tags = image.tags + tags)
        storage.storeImage(imageWithTags)
        return imageWithTags
    }

    /**
     * Get all available icon libraries
     *
     * @return List of libraries
     */
    suspend fun getLibraries(): List<AssetLibrary> {
        return storage.getLibraries()
    }

    /**
     * Get icons from a specific library
     *
     * @param libraryId Library identifier (e.g., "material", "fontawesome")
     * @param limit Maximum results
     * @return List of icons
     */
    suspend fun getLibraryIcons(libraryId: String, limit: Int = 100): List<Icon> {
        return storage.getLibraryIcons(libraryId, limit)
    }

    /**
     * Register Material Icons library (plugin-based)
     *
     * Icons are NOT bundled - they download on-demand from CDN.
     * Only metadata is registered. Zero app bloat!
     *
     * Popular icons (~15) are auto-cached for offline use.
     */
    suspend fun registerMaterialIcons() {
        val library = AssetLibrary(
            id = "material",
            name = "Material Icons",
            version = "1.0.0",
            assetCount = 2400,
            license = "Apache 2.0",
            url = "https://fonts.google.com/icons",
            description = "Material Design icons from Google (on-demand loading)"
        )
        storage.registerLibrary(library)
    }

    /**
     * Register Font Awesome library (plugin-based)
     *
     * Icons are NOT bundled - they download on-demand from CDN.
     * Only metadata is registered. Zero app bloat!
     *
     * Popular icons (~15) are auto-cached for offline use.
     */
    suspend fun registerFontAwesome() {
        val library = AssetLibrary(
            id = "fontawesome",
            name = "Font Awesome Free",
            version = "6.5.0",
            assetCount = 1500,
            license = "CC BY 4.0, Font License",
            url = "https://fontawesome.com",
            description = "Font Awesome icons (on-demand loading)"
        )
        storage.registerLibrary(library)
    }

    /**
     * Preload popular icons for offline use
     *
     * Downloads and caches most commonly used icons (~30 total)
     * This is optional but recommended for better UX.
     *
     * @param onProgress Progress callback (current, total)
     */
    suspend fun preloadPopularIcons(onProgress: ((Int, Int) -> Unit)? = null) {
        // Will be implemented with RemoteIconLibrary
        // Downloads ~30 popular icons from both libraries
        // Total size: ~50-100KB
    }

    /**
     * Get storage statistics
     *
     * @return Current storage stats
     */
    suspend fun getStats(): StorageStats {
        return storage.getStats()
    }

    /**
     * Delete an icon
     *
     * @param iconId Icon to delete
     * @return true if deleted
     */
    suspend fun deleteIcon(iconId: String): Boolean {
        return storage.deleteIcon(iconId)
    }

    /**
     * Delete an image
     *
     * @param imageId Image to delete
     * @return true if deleted
     */
    suspend fun deleteImage(imageId: String): Boolean {
        return storage.deleteImage(imageId)
    }

    private fun generateIconId(fileName: String): String {
        val timestamp = currentTimeMillis()
        val nameWithoutExt = fileName.substringBeforeLast('.')
        return "custom:${nameWithoutExt}_$timestamp"
    }

    private fun generateImageId(fileName: String): String {
        val timestamp = currentTimeMillis()
        val nameWithoutExt = fileName.substringBeforeLast('.')
        return "image:${nameWithoutExt}_$timestamp"
    }

    companion object {
        private var instance: AssetManager? = null

        /**
         * Get AssetManager singleton instance
         *
         * @param storage Optional custom storage (for testing)
         * @param processor Optional custom processor (for testing)
         * @return AssetManager instance
         */
        fun getInstance(
            storage: AssetStorage? = null,
            processor: AssetProcessor? = null
        ): AssetManager {
            return instance ?: synchronized(this) {
                instance ?: AssetManager(
                    storage = storage ?: createDefaultStorage(),
                    processor = processor ?: AssetProcessor()
                ).also { instance = it }
            }
        }

        /**
         * Reset instance (for testing)
         */
        fun reset() {
            instance = null
        }
    }
}

/**
 * Platform-specific default storage factory
 */
internal expect fun createDefaultStorage(): AssetStorage

/**
 * Platform-specific current time millis
 */
internal expect fun currentTimeMillis(): Long
