package com.augmentalis.universal.assetmanager

/**
 * Represents an image library containing multiple images
 *
 * @property id Unique identifier for this library
 * @property name Display name of the library
 * @property description Optional description
 * @property images List of images in this library
 * @property metadata Additional metadata
 */
data class ImageLibrary(
    val id: String,
    val name: String,
    val description: String? = null,
    val images: List<ImageAsset> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
)

/**
 * Represents a single image asset
 *
 * @property id Unique identifier within the library
 * @property name Human-readable name
 * @property path Relative path to the image file
 * @property format Image format
 * @property dimensions Image dimensions
 * @property thumbnail Thumbnail data (JPEG format)
 * @property fileSize File size in bytes
 * @property tags Searchable tags
 * @property category Category for organization
 * @property metadata Additional metadata (EXIF, etc.)
 */
data class ImageAsset(
    val id: String,
    val name: String,
    val path: String,
    val format: ImageFormat,
    val dimensions: Dimensions,
    val thumbnail: ByteArray? = null,
    val fileSize: Long = 0,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Get aspect ratio
     */
    fun getAspectRatio(): Double = dimensions.width.toDouble() / dimensions.height.toDouble()

    /**
     * Check if image is landscape
     */
    fun isLandscape(): Boolean = dimensions.width > dimensions.height

    /**
     * Check if image is portrait
     */
    fun isPortrait(): Boolean = dimensions.height > dimensions.width

    /**
     * Check if image is square
     */
    fun isSquare(): Boolean = dimensions.width == dimensions.height

    /**
     * Search helper to check if image matches query
     */
    fun matchesQuery(query: String): Boolean {
        val lowerQuery = query.lowercase()
        return name.lowercase().contains(lowerQuery) ||
               id.lowercase().contains(lowerQuery) ||
               tags.any { it.lowercase().contains(lowerQuery) } ||
               category?.lowercase()?.contains(lowerQuery) == true
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as ImageAsset

        if (id != other.id) return false
        if (name != other.name) return false
        if (path != other.path) return false
        if (format != other.format) return false
        if (dimensions != other.dimensions) return false
        if (thumbnail != null) {
            if (other.thumbnail == null) return false
            if (!thumbnail.contentEquals(other.thumbnail)) return false
        } else if (other.thumbnail != null) return false
        if (fileSize != other.fileSize) return false
        if (tags != other.tags) return false
        if (category != other.category) return false
        if (metadata != other.metadata) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + dimensions.hashCode()
        result = 31 * result + (thumbnail?.contentHashCode() ?: 0)
        result = 31 * result + fileSize.hashCode()
        result = 31 * result + tags.hashCode()
        result = 31 * result + (category?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }
}

/**
 * Image dimensions
 */
data class Dimensions(
    val width: Int,
    val height: Int
)

/**
 * Supported image formats
 */
enum class ImageFormat(val extension: String, val mimeType: String) {
    JPEG("jpg", "image/jpeg"),
    PNG("png", "image/png"),
    GIF("gif", "image/gif"),
    WEBP("webp", "image/webp"),
    SVG("svg", "image/svg+xml"),
    BMP("bmp", "image/bmp"),
    TIFF("tiff", "image/tiff");

    companion object {
        /**
         * Get format from file extension
         */
        fun fromExtension(extension: String): ImageFormat? {
            val ext = extension.lowercase().removePrefix(".")
            return entries.find { it.extension == ext }
        }

        /**
         * Get format from MIME type
         */
        fun fromMimeType(mimeType: String): ImageFormat? {
            return entries.find { it.mimeType == mimeType }
        }
    }
}

/**
 * Image reference for use in components
 *
 * Format: "library:image-id" (e.g., "Backgrounds:gradient-1", "Photos:hero-image")
 */
data class ImageReference(
    val libraryId: String,
    val imageId: String
) {
    companion object {
        /**
         * Parse an image reference string
         * Format: "library:image-id"
         */
        fun parse(reference: String): ImageReference? {
            val parts = reference.split(":", limit = 2)
            if (parts.size != 2) return null
            return ImageReference(parts[0], parts[1])
        }
    }

    override fun toString(): String = "$libraryId:$imageId"
}

/**
 * Image filter criteria for searching
 */
data class ImageFilter(
    val query: String? = null,
    val formats: Set<ImageFormat>? = null,
    val minWidth: Int? = null,
    val maxWidth: Int? = null,
    val minHeight: Int? = null,
    val maxHeight: Int? = null,
    val tags: Set<String>? = null,
    val category: String? = null,
    val orientation: ImageOrientation? = null
)

/**
 * Image orientation filter
 */
enum class ImageOrientation {
    LANDSCAPE,
    PORTRAIT,
    SQUARE
}
