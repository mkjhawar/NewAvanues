package com.augmentalis.universal.assetmanager

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Central asset management system for icons and images
 *
 * Provides a registry-based system for managing multiple icon and image libraries,
 * with search and retrieval capabilities.
 */
object AssetManager {
    private val iconLibraries = mutableMapOf<String, IconLibrary>()
    private val imageLibraries = mutableMapOf<String, ImageLibrary>()
    private val mutex = Mutex()

    // Default repository - can be replaced for testing or custom storage
    var repository: AssetRepository = LocalAssetRepository()

    /**
     * Register an icon library
     *
     * @param library The icon library to register
     * @param persist Whether to persist the library to storage
     */
    suspend fun registerIconLibrary(library: IconLibrary, persist: Boolean = true) {
        mutex.withLock {
            iconLibraries[library.id] = library
        }

        if (persist) {
            repository.saveIconLibrary(library)
        }
    }

    /**
     * Register an image library
     *
     * @param library The image library to register
     * @param persist Whether to persist the library to storage
     */
    suspend fun registerImageLibrary(library: ImageLibrary, persist: Boolean = true) {
        mutex.withLock {
            imageLibraries[library.id] = library
        }

        if (persist) {
            repository.saveImageLibrary(library)
        }
    }

    /**
     * Unregister an icon library
     */
    suspend fun unregisterIconLibrary(libraryId: String) {
        mutex.withLock {
            iconLibraries.remove(libraryId)
        }
    }

    /**
     * Unregister an image library
     */
    suspend fun unregisterImageLibrary(libraryId: String) {
        mutex.withLock {
            imageLibraries.remove(libraryId)
        }
    }

    /**
     * Get an icon library by ID
     */
    suspend fun getIconLibrary(libraryId: String): IconLibrary? {
        return mutex.withLock {
            iconLibraries[libraryId]
        }
    }

    /**
     * Get an image library by ID
     */
    suspend fun getImageLibrary(libraryId: String): ImageLibrary? {
        return mutex.withLock {
            imageLibraries[libraryId]
        }
    }

    /**
     * Get all registered icon libraries
     */
    suspend fun getAllIconLibraries(): List<IconLibrary> {
        return mutex.withLock {
            iconLibraries.values.toList()
        }
    }

    /**
     * Get all registered image libraries
     */
    suspend fun getAllImageLibraries(): List<ImageLibrary> {
        return mutex.withLock {
            imageLibraries.values.toList()
        }
    }

    /**
     * Get an icon by reference
     *
     * @param reference Icon reference in format "library:icon-id"
     * @return The icon if found, null otherwise
     */
    suspend fun getIcon(reference: String): Icon? {
        val ref = IconReference.parse(reference) ?: return null
        return getIcon(ref.libraryId, ref.iconId)
    }

    /**
     * Get an icon by library ID and icon ID
     */
    suspend fun getIcon(libraryId: String, iconId: String): Icon? {
        val library = getIconLibrary(libraryId) ?: return null
        return library.icons.find { it.id == iconId }
    }

    /**
     * Get an image by reference
     *
     * @param reference Image reference in format "library:image-id"
     * @return The image asset if found, null otherwise
     */
    suspend fun getImage(reference: String): ImageAsset? {
        val ref = ImageReference.parse(reference) ?: return null
        return getImage(ref.libraryId, ref.imageId)
    }

    /**
     * Get an image by library ID and image ID
     */
    suspend fun getImage(libraryId: String, imageId: String): ImageAsset? {
        val library = getImageLibrary(libraryId) ?: return null
        return library.images.find { it.id == imageId }
    }

    /**
     * Search for icons across all libraries
     *
     * @param query Search query (searches name, id, tags, keywords, category)
     * @param libraryIds Optional set of library IDs to search within
     * @param tags Optional set of tags to filter by
     * @param category Optional category to filter by
     * @return List of matching icons with their library IDs
     */
    suspend fun searchIcons(
        query: String? = null,
        libraryIds: Set<String>? = null,
        tags: Set<String>? = null,
        category: String? = null
    ): List<IconSearchResult> {
        val libraries = mutex.withLock {
            if (libraryIds != null) {
                iconLibraries.filterKeys { it in libraryIds }.values
            } else {
                iconLibraries.values
            }
        }

        return libraries.flatMap { library ->
            library.icons
                .filter { icon ->
                    // Query filter
                    (query == null || icon.matchesQuery(query)) &&
                    // Tags filter
                    (tags == null || tags.any { tag -> icon.tags.contains(tag) }) &&
                    // Category filter
                    (category == null || icon.category == category)
                }
                .map { icon ->
                    IconSearchResult(
                        libraryId = library.id,
                        libraryName = library.name,
                        icon = icon,
                        reference = "${library.id}:${icon.id}"
                    )
                }
        }
    }

    /**
     * Search for images across all libraries
     *
     * @param filter Image filter criteria
     * @param libraryIds Optional set of library IDs to search within
     * @return List of matching images with their library IDs
     */
    suspend fun searchImages(
        filter: ImageFilter? = null,
        libraryIds: Set<String>? = null
    ): List<ImageSearchResult> {
        val libraries = mutex.withLock {
            if (libraryIds != null) {
                imageLibraries.filterKeys { it in libraryIds }.values
            } else {
                imageLibraries.values
            }
        }

        return libraries.flatMap { library ->
            library.images
                .filter { image ->
                    matchesImageFilter(image, filter)
                }
                .map { image ->
                    ImageSearchResult(
                        libraryId = library.id,
                        libraryName = library.name,
                        image = image,
                        reference = "${library.id}:${image.id}"
                    )
                }
        }
    }

    private fun matchesImageFilter(image: ImageAsset, filter: ImageFilter?): Boolean {
        if (filter == null) return true

        return (filter.query == null || image.matchesQuery(filter.query)) &&
               (filter.formats == null || image.format in filter.formats) &&
               (filter.minWidth == null || image.dimensions.width >= filter.minWidth) &&
               (filter.maxWidth == null || image.dimensions.width <= filter.maxWidth) &&
               (filter.minHeight == null || image.dimensions.height >= filter.minHeight) &&
               (filter.maxHeight == null || image.dimensions.height <= filter.maxHeight) &&
               (filter.tags == null || filter.tags.any { tag -> image.tags.contains(tag) }) &&
               (filter.category == null || image.category == filter.category) &&
               (filter.orientation == null || matchesOrientation(image, filter.orientation))
    }

    private fun matchesOrientation(image: ImageAsset, orientation: ImageOrientation): Boolean {
        return when (orientation) {
            ImageOrientation.LANDSCAPE -> image.isLandscape()
            ImageOrientation.PORTRAIT -> image.isPortrait()
            ImageOrientation.SQUARE -> image.isSquare()
        }
    }

    /**
     * Load all libraries from the repository
     */
    suspend fun loadAllLibraries() {
        val iconLibs = repository.loadAllIconLibraries()
        val imageLibs = repository.loadAllImageLibraries()

        mutex.withLock {
            iconLibs.forEach { iconLibraries[it.id] = it }
            imageLibs.forEach { imageLibraries[it.id] = it }
        }
    }

    /**
     * Clear all registered libraries (useful for testing)
     */
    suspend fun clearAll() {
        mutex.withLock {
            iconLibraries.clear()
            imageLibraries.clear()
        }
    }
}

/**
 * Icon search result with library information
 */
data class IconSearchResult(
    val libraryId: String,
    val libraryName: String,
    val icon: Icon,
    val reference: String
)

/**
 * Image search result with library information
 */
data class ImageSearchResult(
    val libraryId: String,
    val libraryName: String,
    val image: ImageAsset,
    val reference: String
)
