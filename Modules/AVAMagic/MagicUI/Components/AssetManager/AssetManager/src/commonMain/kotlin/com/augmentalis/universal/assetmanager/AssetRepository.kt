package com.augmentalis.universal.assetmanager

/**
 * Interface for asset persistence
 *
 * Defines methods for saving and loading icon and image libraries.
 * Implementations can use different storage backends (local file system, database, cloud storage, etc.)
 */
interface AssetRepository {
    /**
     * Save an icon library to persistent storage
     */
    suspend fun saveIconLibrary(library: IconLibrary): Result<Unit>

    /**
     * Load an icon library by ID
     */
    suspend fun loadIconLibrary(id: String): Result<IconLibrary?>

    /**
     * Load all icon libraries
     */
    suspend fun loadAllIconLibraries(): List<IconLibrary>

    /**
     * Delete an icon library
     */
    suspend fun deleteIconLibrary(id: String): Result<Unit>

    /**
     * Save an image library to persistent storage
     */
    suspend fun saveImageLibrary(library: ImageLibrary): Result<Unit>

    /**
     * Load an image library by ID
     */
    suspend fun loadImageLibrary(id: String): Result<ImageLibrary?>

    /**
     * Load all image libraries
     */
    suspend fun loadAllImageLibraries(): List<ImageLibrary>

    /**
     * Delete an image library
     */
    suspend fun deleteImageLibrary(id: String): Result<Unit>

    /**
     * Save icon data (SVG or PNG bytes)
     */
    suspend fun saveIconData(
        libraryId: String,
        iconId: String,
        format: IconFormat,
        data: ByteArray,
        size: Int? = null // For PNG sizes
    ): Result<String> // Returns path

    /**
     * Load icon data
     */
    suspend fun loadIconData(
        libraryId: String,
        iconId: String,
        format: IconFormat,
        size: Int? = null
    ): Result<ByteArray?>

    /**
     * Save image data
     */
    suspend fun saveImageData(
        libraryId: String,
        imageId: String,
        data: ByteArray
    ): Result<String> // Returns path

    /**
     * Load image data
     */
    suspend fun loadImageData(
        libraryId: String,
        imageId: String
    ): Result<ByteArray?>

    /**
     * Save thumbnail data
     */
    suspend fun saveThumbnail(
        libraryId: String,
        imageId: String,
        thumbnailData: ByteArray
    ): Result<String> // Returns path

    /**
     * Check if a library exists
     */
    suspend fun libraryExists(id: String, type: LibraryType): Boolean
}

/**
 * Library type enum
 */
enum class LibraryType {
    ICON,
    IMAGE
}

/**
 * Local file system implementation of AssetRepository
 *
 * Stores assets in Universal/Assets/ directory with the following structure:
 * - Universal/Assets/Icons/[LibraryId]/manifest.json
 * - Universal/Assets/Icons/[LibraryId]/svg/[iconId].svg
 * - Universal/Assets/Icons/[LibraryId]/png/[iconId]_[size].png
 * - Universal/Assets/Images/[LibraryId]/manifest.json
 * - Universal/Assets/Images/[LibraryId]/images/[imageId].[ext]
 * - Universal/Assets/Images/[LibraryId]/thumbnails/[imageId].jpg
 */
expect class LocalAssetRepository() : AssetRepository {
    override suspend fun saveIconLibrary(library: IconLibrary): Result<Unit>
    override suspend fun loadIconLibrary(id: String): Result<IconLibrary?>
    override suspend fun loadAllIconLibraries(): List<IconLibrary>
    override suspend fun deleteIconLibrary(id: String): Result<Unit>

    override suspend fun saveImageLibrary(library: ImageLibrary): Result<Unit>
    override suspend fun loadImageLibrary(id: String): Result<ImageLibrary?>
    override suspend fun loadAllImageLibraries(): List<ImageLibrary>
    override suspend fun deleteImageLibrary(id: String): Result<Unit>

    override suspend fun saveIconData(
        libraryId: String,
        iconId: String,
        format: IconFormat,
        data: ByteArray,
        size: Int?
    ): Result<String>

    override suspend fun loadIconData(
        libraryId: String,
        iconId: String,
        format: IconFormat,
        size: Int?
    ): Result<ByteArray?>

    override suspend fun saveImageData(
        libraryId: String,
        imageId: String,
        data: ByteArray
    ): Result<String>

    override suspend fun loadImageData(
        libraryId: String,
        imageId: String
    ): Result<ByteArray?>

    override suspend fun saveThumbnail(
        libraryId: String,
        imageId: String,
        thumbnailData: ByteArray
    ): Result<String>

    override suspend fun libraryExists(id: String, type: LibraryType): Boolean
}

/**
 * Manifest file format for libraries
 *
 * These are serialized to JSON and stored as manifest.json files
 */
internal data class IconLibraryManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String?,
    val metadata: Map<String, String>,
    val icons: List<IconManifestEntry>
)

internal data class IconManifestEntry(
    val id: String,
    val name: String,
    val hasSvg: Boolean,
    val pngSizes: List<Int>,
    val tags: List<String>,
    val category: String?,
    val keywords: List<String>
)

internal data class ImageLibraryManifest(
    val id: String,
    val name: String,
    val description: String?,
    val metadata: Map<String, String>,
    val images: List<ImageManifestEntry>
)

internal data class ImageManifestEntry(
    val id: String,
    val name: String,
    val fileName: String,
    val format: ImageFormat,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val hasThumbnail: Boolean,
    val tags: List<String>,
    val category: String?,
    val metadata: Map<String, String>
)

/**
 * Conversion utilities between library objects and manifest entries
 */
internal object ManifestConverter {
    fun iconLibraryToManifest(library: IconLibrary): IconLibraryManifest {
        return IconLibraryManifest(
            id = library.id,
            name = library.name,
            version = library.version,
            description = library.description,
            metadata = library.metadata,
            icons = library.icons.map { iconToManifestEntry(it) }
        )
    }

    fun iconToManifestEntry(icon: Icon): IconManifestEntry {
        return IconManifestEntry(
            id = icon.id,
            name = icon.name,
            hasSvg = icon.svg != null,
            pngSizes = icon.png?.keys?.sorted() ?: emptyList(),
            tags = icon.tags,
            category = icon.category,
            keywords = icon.keywords
        )
    }

    fun manifestToIconLibrary(manifest: IconLibraryManifest, icons: List<Icon>): IconLibrary {
        return IconLibrary(
            id = manifest.id,
            name = manifest.name,
            version = manifest.version,
            description = manifest.description,
            icons = icons,
            metadata = manifest.metadata
        )
    }

    fun imageLibraryToManifest(library: ImageLibrary): ImageLibraryManifest {
        return ImageLibraryManifest(
            id = library.id,
            name = library.name,
            description = library.description,
            metadata = library.metadata,
            images = library.images.map { imageToManifestEntry(it) }
        )
    }

    fun imageToManifestEntry(image: ImageAsset): ImageManifestEntry {
        return ImageManifestEntry(
            id = image.id,
            name = image.name,
            fileName = image.path.substringAfterLast('/'),
            format = image.format,
            width = image.dimensions.width,
            height = image.dimensions.height,
            fileSize = image.fileSize,
            hasThumbnail = image.thumbnail != null,
            tags = image.tags,
            category = image.category,
            metadata = image.metadata
        )
    }

    fun manifestToImageLibrary(manifest: ImageLibraryManifest, images: List<ImageAsset>): ImageLibrary {
        return ImageLibrary(
            id = manifest.id,
            name = manifest.name,
            description = manifest.description,
            images = images,
            metadata = manifest.metadata
        )
    }
}
