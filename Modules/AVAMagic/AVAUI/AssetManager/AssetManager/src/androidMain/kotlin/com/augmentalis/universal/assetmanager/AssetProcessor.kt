package com.augmentalis.universal.assetmanager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Android implementation of AssetProcessor
 *
 * Uses Android Bitmap APIs for image processing, resizing, and format conversion.
 * Optimized for Android platform with hardware acceleration where available.
 */
actual class AssetProcessor {

    /**
     * Process an icon file and extract icon data
     */
    actual suspend fun processIcon(
        fileData: ByteArray,
        fileName: String,
        iconId: String
    ): Icon = withContext(Dispatchers.IO) {
        val format = AssetProcessorUtils.detectImageFormat(fileName)

        when {
            format == ImageFormat.SVG -> {
                // Parse SVG
                val svgString = fileData.decodeToString()

                if (!AssetProcessorUtils.isValidSvg(svgString)) {
                    throw IllegalArgumentException("Invalid SVG content in file: $fileName")
                }

                Icon(
                    id = iconId,
                    name = AssetProcessorUtils.generateIdFromFileName(fileName),
                    svg = svgString,
                    png = null,
                    tags = AssetProcessorUtils.extractTagsFromFileName(fileName),
                    category = null,
                    keywords = emptyList()
                )
            }
            format in setOf(ImageFormat.PNG, ImageFormat.JPEG, ImageFormat.WEBP) -> {
                // Process raster image
                val bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.size)
                    ?: throw IllegalArgumentException("Failed to decode image: $fileName")

                val pngSizes = generateMultipleSizes(bitmap)

                Icon(
                    id = iconId,
                    name = AssetProcessorUtils.generateIdFromFileName(fileName),
                    svg = null,
                    png = pngSizes,
                    tags = AssetProcessorUtils.extractTagsFromFileName(fileName),
                    category = null,
                    keywords = emptyList()
                )
            }
            else -> {
                throw UnsupportedOperationException("Unsupported icon format: ${format?.extension}")
            }
        }
    }

    /**
     * Process an image file and extract image metadata
     */
    actual suspend fun processImage(
        fileData: ByteArray,
        fileName: String,
        imageId: String
    ): ImageAsset = withContext(Dispatchers.IO) {
        val format = AssetProcessorUtils.detectImageFormat(fileName)
            ?: throw IllegalArgumentException("Unknown image format: $fileName")

        val bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.size)
            ?: throw IllegalArgumentException("Failed to decode image: $fileName")

        val dimensions = Dimensions(bitmap.width, bitmap.height)

        // Generate thumbnail
        val thumbnail = generateThumbnail(
            fileData,
            AssetProcessorUtils.StandardSizes.THUMBNAIL_MEDIUM,
            AssetProcessorUtils.StandardSizes.THUMBNAIL_MEDIUM,
            maintainAspectRatio = true
        )

        ImageAsset(
            id = imageId,
            name = AssetProcessorUtils.generateIdFromFileName(fileName),
            path = "images/$imageId.${format.extension}",
            format = format,
            dimensions = dimensions,
            thumbnail = thumbnail,
            fileSize = fileData.size.toLong(),
            tags = AssetProcessorUtils.extractTagsFromFileName(fileName),
            category = null,
            metadata = emptyMap()
        )
    }

    /**
     * Generate a thumbnail for an image
     */
    actual suspend fun generateThumbnail(
        imageData: ByteArray,
        width: Int,
        height: Int,
        maintainAspectRatio: Boolean
    ): ByteArray = withContext(Dispatchers.IO) {
        val original = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: throw IllegalArgumentException("Failed to decode image for thumbnail")

        val targetDimensions = if (maintainAspectRatio) {
            AssetProcessorUtils.calculateThumbnailDimensions(
                original.width,
                original.height,
                width,
                height
            )
        } else {
            Dimensions(width, height)
        }

        val scaled = Bitmap.createScaledBitmap(
            original,
            targetDimensions.width,
            targetDimensions.height,
            true // Use filtering for better quality
        )

        // Compress as JPEG with high quality
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)

        // Clean up
        if (scaled != original) {
            scaled.recycle()
        }
        original.recycle()

        stream.toByteArray()
    }

    /**
     * Optimize an image (reduce file size while maintaining quality)
     */
    actual suspend fun optimizeImage(
        imageData: ByteArray,
        quality: Int
    ): ByteArray = withContext(Dispatchers.IO) {
        require(quality in 0..100) { "Quality must be between 0 and 100" }

        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: throw IllegalArgumentException("Failed to decode image for optimization")

        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        bitmap.recycle()

        stream.toByteArray()
    }

    /**
     * Extract image dimensions from image data without loading full bitmap
     */
    actual suspend fun extractDimensions(imageData: ByteArray): Dimensions =
        withContext(Dispatchers.IO) {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)

            Dimensions(options.outWidth, options.outHeight)
        }

    /**
     * Convert an image to a specific format
     */
    actual suspend fun convertFormat(
        imageData: ByteArray,
        targetFormat: ImageFormat
    ): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: throw IllegalArgumentException("Failed to decode image for format conversion")

        val stream = ByteArrayOutputStream()

        val compressFormat = when (targetFormat) {
            ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
            ImageFormat.PNG -> Bitmap.CompressFormat.PNG
            ImageFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Bitmap.CompressFormat.WEBP_LOSSY
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
            }
            ImageFormat.SVG -> {
                bitmap.recycle()
                throw UnsupportedOperationException("Cannot convert to SVG format")
            }
            ImageFormat.GIF -> {
                bitmap.recycle()
                throw UnsupportedOperationException("GIF encoding not supported on Android")
            }
            ImageFormat.BMP -> {
                bitmap.recycle()
                throw UnsupportedOperationException("BMP encoding not supported on Android")
            }
            ImageFormat.TIFF -> {
                bitmap.recycle()
                throw UnsupportedOperationException("TIFF encoding not supported on Android")
            }
        }

        val quality = if (targetFormat == ImageFormat.PNG) 100 else 90
        bitmap.compress(compressFormat, quality, stream)
        bitmap.recycle()

        stream.toByteArray()
    }

    /**
     * Generate multiple icon sizes from a bitmap
     */
    private fun generateMultipleSizes(bitmap: Bitmap): Map<Int, ByteArray> {
        val sizes = mutableMapOf<Int, ByteArray>()

        AssetProcessorUtils.StandardSizes.ICON_SIZES.forEach { size ->
            val scaled = Bitmap.createScaledBitmap(bitmap, size, size, true)
            val stream = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)

            sizes[size] = stream.toByteArray()

            if (scaled != bitmap) {
                scaled.recycle()
            }
        }

        return sizes
    }
}

/**
 * Android-specific processing utilities
 */
object AndroidAssetProcessorUtils {
    /**
     * Check if hardware acceleration is available
     */
    fun isHardwareAccelerationAvailable(): Boolean {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB
    }

    /**
     * Get optimal sample size for memory-efficient loading
     */
    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight
                && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Decode bitmap with sample size for memory efficiency
     */
    fun decodeSampledBitmapFromByteArray(
        data: ByteArray,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap? {
        return BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeByteArray(data, 0, data.size, this)

            inSampleSize = calculateInSampleSize(this, reqWidth, reqHeight)
            inJustDecodeBounds = false

            BitmapFactory.decodeByteArray(data, 0, data.size, this)
        }
    }
}
