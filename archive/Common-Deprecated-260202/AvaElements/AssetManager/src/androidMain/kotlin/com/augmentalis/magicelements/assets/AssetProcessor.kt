package com.augmentalis.avaelements.assets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import com.augmentalis.avaelements.assets.models.*
import com.augmentalis.avaelements.assets.utils.AssetUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * Android implementation of AssetProcessor
 *
 * Uses Android's BitmapFactory and graphics APIs for image processing.
 */
actual class AssetProcessor {
    /**
     * Process icon from raw file data
     */
    actual suspend fun processIcon(
        fileData: ByteArray,
        fileName: String,
        iconId: String
    ): Icon = withContext(Dispatchers.IO) {
        val format = AssetUtils.detectImageFormat(fileName)

        when {
            format == ImageFormat.SVG -> {
                // SVG - store as string
                val svgString = fileData.decodeToString()
                Icon(
                    id = iconId,
                    name = fileName.substringBeforeLast('.'),
                    svg = svgString,
                    tags = AssetUtils.extractTagsFromFileName(fileName)
                )
            }
            else -> {
                // PNG/JPEG/other raster - generate multiple sizes
                val bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.size)
                    ?: throw IllegalArgumentException("Failed to decode image: $fileName")

                val pngSizes = mutableMapOf<IconSize, ByteArray>()

                // Generate standard icon sizes
                IconSize.values().forEach { size ->
                    val resizedBitmap = Bitmap.createScaledBitmap(
                        bitmap,
                        size.pixels,
                        size.pixels,
                        true
                    )
                    pngSizes[size] = resizedBitmap.toPngBytes()
                    if (resizedBitmap != bitmap) {
                        resizedBitmap.recycle()
                    }
                }

                bitmap.recycle()

                Icon(
                    id = iconId,
                    name = fileName.substringBeforeLast('.'),
                    png = pngSizes,
                    tags = AssetUtils.extractTagsFromFileName(fileName)
                )
            }
        }
    }

    /**
     * Process image from raw file data
     */
    actual suspend fun processImage(
        fileData: ByteArray,
        fileName: String,
        imageId: String
    ): ImageAsset = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(fileData, 0, fileData.size)
            ?: throw IllegalArgumentException("Failed to decode image: $fileName")

        val dimensions = Dimensions(bitmap.width, bitmap.height)

        // Generate thumbnail
        val thumbnail = generateThumbnail(
            fileData,
            128,
            128,
            maintainAspectRatio = true
        )

        bitmap.recycle()

        ImageAsset(
            id = imageId,
            name = fileName.substringBeforeLast('.'),
            path = "images/$imageId",
            format = AssetUtils.detectImageFormat(fileName) ?: ImageFormat.PNG,
            dimensions = dimensions,
            thumbnail = thumbnail,
            fileSize = fileData.size.toLong(),
            tags = AssetUtils.extractTagsFromFileName(fileName)
        )
    }

    /**
     * Generate thumbnail for image
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
            AssetUtils.calculateThumbnailDimensions(
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
            true
        )

        val result = scaled.toJpegBytes(85)

        if (scaled != original) scaled.recycle()
        original.recycle()

        result
    }

    /**
     * Optimize image (compress)
     */
    actual suspend fun optimizeImage(
        imageData: ByteArray,
        quality: Int
    ): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: throw IllegalArgumentException("Failed to decode image for optimization")

        val result = bitmap.toJpegBytes(quality)
        bitmap.recycle()
        result
    }

    /**
     * Extract image dimensions without fully loading
     */
    actual suspend fun extractDimensions(imageData: ByteArray): Dimensions {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeByteArray(imageData, 0, imageData.size, options)
        return Dimensions(options.outWidth, options.outHeight)
    }

    /**
     * Convert image to different format
     */
    actual suspend fun convertFormat(
        imageData: ByteArray,
        targetFormat: ImageFormat
    ): ByteArray = withContext(Dispatchers.IO) {
        val bitmap = BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            ?: throw IllegalArgumentException("Failed to decode image for format conversion")

        val result = when (targetFormat) {
            ImageFormat.JPEG -> bitmap.toJpegBytes(90)
            ImageFormat.PNG -> bitmap.toPngBytes()
            ImageFormat.WEBP -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    bitmap.toWebpBytes(90)
                } else {
                    // Fallback to PNG on older Android versions
                    bitmap.toPngBytes()
                }
            }
            else -> bitmap.toPngBytes() // Default to PNG
        }

        bitmap.recycle()
        result
    }

    // Extension functions for Bitmap conversion

    private fun Bitmap.toPngBytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.PNG, 100, stream)
        return stream.toByteArray()
    }

    private fun Bitmap.toJpegBytes(quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, quality, stream)
        return stream.toByteArray()
    }

    private fun Bitmap.toWebpBytes(quality: Int): ByteArray {
        val stream = ByteArrayOutputStream()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, stream)
        } else {
            @Suppress("DEPRECATION")
            compress(Bitmap.CompressFormat.WEBP, quality, stream)
        }
        return stream.toByteArray()
    }
}
