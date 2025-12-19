package com.augmentalis.avaelements.assets

import com.augmentalis.avaelements.assets.models.*

/**
 * Platform-specific asset processing
 *
 * Handles image/icon processing, thumbnail generation,
 * format conversion, and optimization.
 */
expect class AssetProcessor() {
    /**
     * Process icon from raw file data
     *
     * @param fileData Raw file bytes
     * @param fileName Original filename (for format detection)
     * @param iconId Unique ID to assign
     * @return Processed Icon
     */
    suspend fun processIcon(
        fileData: ByteArray,
        fileName: String,
        iconId: String
    ): Icon

    /**
     * Process image from raw file data
     *
     * @param fileData Raw file bytes
     * @param fileName Original filename
     * @param imageId Unique ID to assign
     * @return Processed ImageAsset
     */
    suspend fun processImage(
        fileData: ByteArray,
        fileName: String,
        imageId: String
    ): ImageAsset

    /**
     * Generate thumbnail for image
     *
     * @param imageData Original image bytes
     * @param width Target width
     * @param height Target height
     * @param maintainAspectRatio Whether to maintain aspect ratio
     * @return Thumbnail bytes (JPEG format)
     */
    suspend fun generateThumbnail(
        imageData: ByteArray,
        width: Int,
        height: Int,
        maintainAspectRatio: Boolean = true
    ): ByteArray

    /**
     * Optimize image (compress, reduce size)
     *
     * @param imageData Original image bytes
     * @param quality Quality level (0-100)
     * @return Optimized image bytes
     */
    suspend fun optimizeImage(
        imageData: ByteArray,
        quality: Int = 85
    ): ByteArray

    /**
     * Extract image dimensions without fully loading
     *
     * @param imageData Image bytes
     * @return Dimensions
     */
    suspend fun extractDimensions(imageData: ByteArray): Dimensions

    /**
     * Convert image to different format
     *
     * @param imageData Original image bytes
     * @param targetFormat Target format
     * @return Converted image bytes
     */
    suspend fun convertFormat(
        imageData: ByteArray,
        targetFormat: ImageFormat
    ): ByteArray
}
