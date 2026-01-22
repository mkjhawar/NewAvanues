package com.augmentalis.universal.assetmanager

/**
 * Asset processing pipeline for icons and images
 *
 * Handles processing, optimization, and thumbnail generation for asset files.
 * Platform-specific implementations should be provided for actual image processing.
 */
expect class AssetProcessor() {
    /**
     * Process an icon file and extract icon data
     *
     * @param fileData Raw file bytes
     * @param fileName Original file name
     * @param iconId ID to assign to the icon
     * @return Processed Icon object
     */
    suspend fun processIcon(
        fileData: ByteArray,
        fileName: String,
        iconId: String
    ): Icon

    /**
     * Process an image file and extract image metadata
     *
     * @param fileData Raw file bytes
     * @param fileName Original file name
     * @param imageId ID to assign to the image
     * @return Processed ImageAsset object
     */
    suspend fun processImage(
        fileData: ByteArray,
        fileName: String,
        imageId: String
    ): ImageAsset

    /**
     * Generate a thumbnail for an image
     *
     * @param imageData Raw image bytes
     * @param width Target thumbnail width
     * @param height Target thumbnail height
     * @param maintainAspectRatio Whether to maintain aspect ratio
     * @return Thumbnail as JPEG bytes
     */
    suspend fun generateThumbnail(
        imageData: ByteArray,
        width: Int,
        height: Int,
        maintainAspectRatio: Boolean = true
    ): ByteArray

    /**
     * Optimize an image (reduce file size while maintaining quality)
     *
     * @param imageData Raw image bytes
     * @param quality Quality level (0-100)
     * @return Optimized image bytes
     */
    suspend fun optimizeImage(
        imageData: ByteArray,
        quality: Int = 85
    ): ByteArray

    /**
     * Extract image dimensions from image data
     *
     * @param imageData Raw image bytes
     * @return Dimensions of the image
     */
    suspend fun extractDimensions(imageData: ByteArray): Dimensions

    /**
     * Convert an image to a specific format
     *
     * @param imageData Raw image bytes
     * @param targetFormat Target image format
     * @return Converted image bytes
     */
    suspend fun convertFormat(
        imageData: ByteArray,
        targetFormat: ImageFormat
    ): ByteArray
}

/**
 * Common processing utilities that work across all platforms
 */
object AssetProcessorUtils {
    /**
     * Generate a slug-style ID from a file name
     */
    fun generateIdFromFileName(fileName: String): String {
        return fileName
            .substringBeforeLast('.')
            .lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
    }

    /**
     * Extract file extension
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }

    /**
     * Detect image format from file name
     */
    fun detectImageFormat(fileName: String): ImageFormat? {
        val extension = getFileExtension(fileName)
        return ImageFormat.fromExtension(extension)
    }

    /**
     * Validate icon name
     */
    fun isValidIconName(name: String): Boolean {
        return name.isNotBlank() && name.length <= 255
    }

    /**
     * Validate image name
     */
    fun isValidImageName(name: String): Boolean {
        return name.isNotBlank() && name.length <= 255
    }

    /**
     * Calculate thumbnail dimensions maintaining aspect ratio
     */
    fun calculateThumbnailDimensions(
        originalWidth: Int,
        originalHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Dimensions {
        val widthRatio = targetWidth.toDouble() / originalWidth
        val heightRatio = targetHeight.toDouble() / originalHeight
        val ratio = minOf(widthRatio, heightRatio)

        return Dimensions(
            width = (originalWidth * ratio).toInt(),
            height = (originalHeight * ratio).toInt()
        )
    }

    /**
     * Extract tags from file name
     * Example: "user-avatar-profile.png" -> ["user", "avatar", "profile"]
     */
    fun extractTagsFromFileName(fileName: String): List<String> {
        val nameWithoutExt = fileName.substringBeforeLast('.')
        return nameWithoutExt
            .split(Regex("[-_\\s]+"))
            .map { it.lowercase() }
            .filter { it.isNotBlank() && it.length > 2 }
    }

    /**
     * Validate SVG content (basic check)
     */
    fun isValidSvg(content: String): Boolean {
        val trimmed = content.trim()
        return trimmed.startsWith("<svg") && trimmed.endsWith("</svg>")
    }

    /**
     * Standard thumbnail sizes
     */
    object StandardSizes {
        const val THUMBNAIL_SMALL = 64
        const val THUMBNAIL_MEDIUM = 128
        const val THUMBNAIL_LARGE = 256

        val ICON_SIZES = listOf(16, 24, 32, 48, 64, 128, 256)
    }
}

/**
 * Asset processing configuration
 */
data class ProcessingConfig(
    val generateThumbnails: Boolean = true,
    val thumbnailWidth: Int = AssetProcessorUtils.StandardSizes.THUMBNAIL_MEDIUM,
    val thumbnailHeight: Int = AssetProcessorUtils.StandardSizes.THUMBNAIL_MEDIUM,
    val optimizeImages: Boolean = true,
    val optimizationQuality: Int = 85,
    val extractTags: Boolean = true,
    val generateMultipleSizes: Boolean = true, // For icons
    val iconSizes: List<Int> = AssetProcessorUtils.StandardSizes.ICON_SIZES
)

/**
 * Processing result with metadata
 */
sealed class ProcessingResult<out T> {
    data class Success<T>(val data: T, val metadata: Map<String, String> = emptyMap()) : ProcessingResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : ProcessingResult<Nothing>()
}

/**
 * Batch processing statistics
 */
data class BatchProcessingStats(
    val totalFiles: Int,
    val successCount: Int,
    val errorCount: Int,
    val skippedCount: Int,
    val totalSizeBytes: Long,
    val processingTimeMs: Long,
    val errors: List<String> = emptyList()
)
