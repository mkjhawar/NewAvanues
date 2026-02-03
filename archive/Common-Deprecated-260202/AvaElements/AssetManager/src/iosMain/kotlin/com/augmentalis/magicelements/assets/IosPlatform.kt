package com.augmentalis.avaelements.assets

import com.augmentalis.avaelements.assets.models.*
import platform.Foundation.NSDate

/**
 * iOS platform implementation for AssetManager
 *
 * TODO: Implement iOS-specific storage (CoreData or SQLite.swift)
 */

/**
 * Temporary in-memory storage for iOS (placeholder)
 */
private class IosInMemoryStorage : AssetStorage {
    private val icons = mutableMapOf<String, Icon>()
    private val images = mutableMapOf<String, ImageAsset>()
    private val libraries = mutableListOf<AssetLibrary>()

    override suspend fun storeIcon(icon: Icon) {
        icons[icon.id] = icon
    }

    override suspend fun storeIcons(icons: List<Icon>) {
        icons.forEach { storeIcon(it) }
    }

    override suspend fun getIcon(iconId: String): Icon? = icons[iconId]

    override suspend fun storeImage(image: ImageAsset) {
        images[image.id] = image
    }

    override suspend fun getImage(imageId: String): ImageAsset? = images[imageId]

    override suspend fun searchIcons(query: AssetQuery): AssetSearchResult<Icon> {
        val results = icons.values.filter { icon ->
            query.query == null ||
            icon.name.contains(query.query, ignoreCase = true) ||
            icon.tags.any { it.contains(query.query, ignoreCase = true) }
        }.take(query.limit)

        return AssetSearchResult(results, results.size, query)
    }

    override suspend fun searchImages(query: AssetQuery): AssetSearchResult<ImageAsset> {
        val results = images.values.filter { image ->
            query.query == null ||
            image.name.contains(query.query, ignoreCase = true) ||
            image.tags.any { it.contains(query.query, ignoreCase = true) }
        }.take(query.limit)

        return AssetSearchResult(results, results.size, query)
    }

    override suspend fun getLibraries(): List<AssetLibrary> = libraries.toList()

    override suspend fun registerLibrary(library: AssetLibrary) {
        libraries.add(library)
    }

    override suspend fun getLibraryIcons(
        libraryId: String,
        limit: Int,
        offset: Int
    ): List<Icon> {
        return icons.values
            .filter { it.library == libraryId }
            .drop(offset)
            .take(limit)
    }

    override suspend fun deleteIcon(iconId: String): Boolean {
        return icons.remove(iconId) != null
    }

    override suspend fun deleteImage(imageId: String): Boolean {
        return images.remove(imageId) != null
    }

    override suspend fun clearLibrary(libraryId: String) {
        icons.values.removeAll { it.library == libraryId }
    }

    override suspend fun getStats(): StorageStats {
        return StorageStats(
            totalIcons = icons.size,
            totalImages = images.size,
            libraryCount = libraries.size,
            totalSizeBytes = images.values.sumOf { it.fileSize }
        )
    }
}

/**
 * Create default iOS asset storage
 */
internal actual fun createDefaultStorage(): AssetStorage {
    // TODO: Implement persistent iOS storage (CoreData or SQLite)
    return IosInMemoryStorage()
}

/**
 * Get current time in milliseconds
 */
internal actual fun currentTimeMillis(): Long {
    return (NSDate().timeIntervalSince1970 * 1000).toLong()
}
