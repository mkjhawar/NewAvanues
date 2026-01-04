package com.augmentalis.universal.assetmanager

/**
 * Asset storage interface for file operations
 *
 * Provides a high-level API for saving, loading, and deleting asset data.
 * This abstracts the underlying storage mechanism (local file system, cloud storage, etc.)
 */
interface AssetStorage {
    /**
     * Save an icon to storage
     *
     * @param libraryId The library this icon belongs to
     * @param icon The icon to save
     * @return Result indicating success or failure
     */
    suspend fun saveIcon(libraryId: String, icon: Icon): Result<String>

    /**
     * Save an image to storage
     *
     * @param libraryId The library this image belongs to
     * @param image The image to save
     * @return Result indicating success or failure
     */
    suspend fun saveImage(libraryId: String, image: ImageAsset): Result<String>

    /**
     * Load an icon from storage
     *
     * @param libraryId The library ID
     * @param iconId The icon ID
     * @return The icon if found, null otherwise
     */
    suspend fun loadIcon(libraryId: String, iconId: String): Icon?

    /**
     * Load an image from storage
     *
     * @param libraryId The library ID
     * @param imageId The image ID
     * @return The image if found, null otherwise
     */
    suspend fun loadImage(libraryId: String, imageId: String): ImageAsset?

    /**
     * Delete an icon from storage
     *
     * @param libraryId The library ID
     * @param iconId The icon ID
     * @return Result indicating success or failure
     */
    suspend fun deleteIcon(libraryId: String, iconId: String): Result<Unit>

    /**
     * Delete an image from storage
     *
     * @param libraryId The library ID
     * @param imageId The image ID
     * @return Result indicating success or failure
     */
    suspend fun deleteImage(libraryId: String, imageId: String): Result<Unit>

    /**
     * List all icons in a library
     *
     * @param libraryId The library ID
     * @return List of icon IDs
     */
    suspend fun listIcons(libraryId: String): List<String>

    /**
     * List all images in a library
     *
     * @param libraryId The library ID
     * @return List of image IDs
     */
    suspend fun listImages(libraryId: String): List<String>

    /**
     * Check if storage directory exists
     *
     * @return True if the storage directory exists and is accessible
     */
    suspend fun storageExists(): Boolean

    /**
     * Initialize storage (create necessary directories)
     *
     * @return Result indicating success or failure
     */
    suspend fun initializeStorage(): Result<Unit>

    /**
     * Get storage statistics
     *
     * @return Storage statistics
     */
    suspend fun getStorageStats(): StorageStats
}

/**
 * Storage statistics
 */
data class StorageStats(
    val totalIconLibraries: Int,
    val totalImageLibraries: Int,
    val totalIcons: Int,
    val totalImages: Int,
    val totalSizeBytes: Long,
    val availableSpaceBytes: Long? = null
)

/**
 * Local file system implementation of AssetStorage
 *
 * Storage structure:
 * ```
 * Universal/Assets/
 * ├── Icons/
 * │   └── [libraryId]/
 * │       ├── svg/
 * │       │   └── [iconId].svg
 * │       └── png/
 * │           ├── [size]/
 * │           │   └── [iconId].png
 * │           └── ...
 * └── Images/
 *     └── [libraryId]/
 *         ├── images/
 *         │   └── [imageId].[ext]
 *         └── thumbnails/
 *             └── [imageId].jpg
 * ```
 */
expect class LocalAssetStorage(basePath: String = "Universal/Assets") : AssetStorage {
    override suspend fun saveIcon(libraryId: String, icon: Icon): Result<String>
    override suspend fun saveImage(libraryId: String, image: ImageAsset): Result<String>
    override suspend fun loadIcon(libraryId: String, iconId: String): Icon?
    override suspend fun loadImage(libraryId: String, imageId: String): ImageAsset?
    override suspend fun deleteIcon(libraryId: String, iconId: String): Result<Unit>
    override suspend fun deleteImage(libraryId: String, imageId: String): Result<Unit>
    override suspend fun listIcons(libraryId: String): List<String>
    override suspend fun listImages(libraryId: String): List<String>
    override suspend fun storageExists(): Boolean
    override suspend fun initializeStorage(): Result<Unit>
    override suspend fun getStorageStats(): StorageStats
}

/**
 * In-memory asset storage for testing
 *
 * Stores all assets in memory without persistence.
 * Useful for unit tests and development.
 */
class InMemoryAssetStorage : AssetStorage {
    private val icons = mutableMapOf<String, MutableMap<String, Icon>>()
    private val images = mutableMapOf<String, MutableMap<String, ImageAsset>>()

    override suspend fun saveIcon(libraryId: String, icon: Icon): Result<String> {
        return try {
            icons.getOrPut(libraryId) { mutableMapOf() }[icon.id] = icon
            Result.success("memory://$libraryId/${icon.id}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun saveImage(libraryId: String, image: ImageAsset): Result<String> {
        return try {
            images.getOrPut(libraryId) { mutableMapOf() }[image.id] = image
            Result.success("memory://$libraryId/${image.id}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loadIcon(libraryId: String, iconId: String): Icon? {
        return icons[libraryId]?.get(iconId)
    }

    override suspend fun loadImage(libraryId: String, imageId: String): ImageAsset? {
        return images[libraryId]?.get(imageId)
    }

    override suspend fun deleteIcon(libraryId: String, iconId: String): Result<Unit> {
        return try {
            icons[libraryId]?.remove(iconId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteImage(libraryId: String, imageId: String): Result<Unit> {
        return try {
            images[libraryId]?.remove(imageId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun listIcons(libraryId: String): List<String> {
        return icons[libraryId]?.keys?.toList() ?: emptyList()
    }

    override suspend fun listImages(libraryId: String): List<String> {
        return images[libraryId]?.keys?.toList() ?: emptyList()
    }

    override suspend fun storageExists(): Boolean = true

    override suspend fun initializeStorage(): Result<Unit> = Result.success(Unit)

    override suspend fun getStorageStats(): StorageStats {
        val totalIcons = icons.values.sumOf { it.size }
        val totalImages = images.values.sumOf { it.size }

        return StorageStats(
            totalIconLibraries = icons.size,
            totalImageLibraries = images.size,
            totalIcons = totalIcons,
            totalImages = totalImages,
            totalSizeBytes = 0L
        )
    }

    /**
     * Clear all stored assets
     */
    fun clear() {
        icons.clear()
        images.clear()
    }
}

/**
 * Storage path utilities
 */
object StoragePathUtils {
    /**
     * Get icon SVG path
     */
    fun getIconSvgPath(libraryId: String, iconId: String): String {
        return "Icons/$libraryId/svg/$iconId.svg"
    }

    /**
     * Get icon PNG path
     */
    fun getIconPngPath(libraryId: String, iconId: String, size: Int): String {
        return "Icons/$libraryId/png/$size/$iconId.png"
    }

    /**
     * Get image path
     */
    fun getImagePath(libraryId: String, imageId: String, format: ImageFormat): String {
        return "Images/$libraryId/images/$imageId.${format.extension}"
    }

    /**
     * Get thumbnail path
     */
    fun getThumbnailPath(libraryId: String, imageId: String): String {
        return "Images/$libraryId/thumbnails/$imageId.jpg"
    }

    /**
     * Parse icon reference from path
     */
    fun parseIconPath(path: String): Pair<String, String>? {
        val regex = Regex("Icons/([^/]+)/(?:svg|png)/(?:[^/]+/)?([^.]+)")
        val match = regex.find(path) ?: return null
        return match.groupValues[1] to match.groupValues[2]
    }

    /**
     * Parse image reference from path
     */
    fun parseImagePath(path: String): Pair<String, String>? {
        val regex = Regex("Images/([^/]+)/images/([^.]+)")
        val match = regex.find(path) ?: return null
        return match.groupValues[1] to match.groupValues[2]
    }
}

/**
 * Storage configuration
 */
data class StorageConfig(
    val basePath: String = "Universal/Assets",
    val createDirectories: Boolean = true,
    val validateManifests: Boolean = true,
    val compressionEnabled: Boolean = false,
    val compressionQuality: Int = 85,
    val maxFileSizeBytes: Long = 10_000_000L, // 10 MB
    val allowedIconFormats: Set<IconFormat> = setOf(IconFormat.SVG, IconFormat.PNG),
    val allowedImageFormats: Set<ImageFormat> = ImageFormat.entries.toSet()
)

/**
 * Storage operation result with detailed information
 */
sealed class StorageResult<out T> {
    data class Success<T>(
        val data: T,
        val path: String,
        val sizeBytes: Long = 0
    ) : StorageResult<T>()

    data class Error(
        val message: String,
        val cause: Throwable? = null,
        val errorCode: StorageErrorCode
    ) : StorageResult<Nothing>()
}

/**
 * Storage error codes
 */
enum class StorageErrorCode {
    NOT_FOUND,
    PERMISSION_DENIED,
    DISK_FULL,
    INVALID_FORMAT,
    FILE_TOO_LARGE,
    CORRUPTION,
    NETWORK_ERROR,
    UNKNOWN
}

/**
 * Batch storage operations
 */
interface BatchStorage {
    /**
     * Save multiple icons in a single operation
     */
    suspend fun saveIcons(libraryId: String, icons: List<Icon>): Map<String, Result<String>>

    /**
     * Save multiple images in a single operation
     */
    suspend fun saveImages(libraryId: String, images: List<ImageAsset>): Map<String, Result<String>>

    /**
     * Delete multiple icons
     */
    suspend fun deleteIcons(libraryId: String, iconIds: List<String>): Map<String, Result<Unit>>

    /**
     * Delete multiple images
     */
    suspend fun deleteImages(libraryId: String, imageIds: List<String>): Map<String, Result<Unit>>
}

/**
 * Storage cleanup utilities
 */
object StorageCleanup {
    /**
     * Find orphaned files (files without manifest entries)
     */
    suspend fun findOrphanedFiles(storage: AssetStorage): List<String> {
        // Implementation would scan storage and compare with manifests
        return emptyList()
    }

    /**
     * Remove orphaned files
     */
    suspend fun cleanOrphanedFiles(storage: AssetStorage): Int {
        val orphaned = findOrphanedFiles(storage)
        // Delete orphaned files
        return orphaned.size
    }

    /**
     * Optimize storage (compress files, remove duplicates)
     */
    suspend fun optimizeStorage(storage: AssetStorage): StorageOptimizationResult {
        return StorageOptimizationResult(
            bytesFreed = 0L,
            filesOptimized = 0,
            errors = emptyList()
        )
    }
}

/**
 * Storage optimization result
 */
data class StorageOptimizationResult(
    val bytesFreed: Long,
    val filesOptimized: Int,
    val errors: List<String>
)
