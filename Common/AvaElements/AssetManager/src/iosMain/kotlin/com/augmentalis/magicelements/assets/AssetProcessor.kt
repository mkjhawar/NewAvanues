package com.augmentalis.avaelements.assets

import com.augmentalis.avaelements.assets.models.*
import com.augmentalis.avaelements.assets.utils.AssetUtils
import kotlinx.cinterop.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.CoreGraphics.*
import platform.Foundation.*
import platform.UIKit.*

/**
 * iOS implementation of AssetProcessor
 *
 * Uses iOS UIImage and Core Graphics APIs for image processing.
 */
@OptIn(ExperimentalForeignApi::class)
actual class AssetProcessor {
    /**
     * Process icon from raw file data
     */
    actual suspend fun processIcon(
        fileData: ByteArray,
        fileName: String,
        iconId: String
    ): Icon = withContext(Dispatchers.Default) {
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
                val nsData = fileData.toNSData()
                val image = UIImage.imageWithData(nsData)
                    ?: throw IllegalArgumentException("Failed to decode image: $fileName")

                val pngSizes = mutableMapOf<IconSize, ByteArray>()

                // Generate standard icon sizes
                IconSize.values().forEach { size ->
                    val resizedImage = image.resizeToSize(size.pixels.toDouble(), size.pixels.toDouble())
                    pngSizes[size] = resizedImage.toPngBytes()
                }

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
    ): ImageAsset = withContext(Dispatchers.Default) {
        val nsData = fileData.toNSData()
        val image = UIImage.imageWithData(nsData)
            ?: throw IllegalArgumentException("Failed to decode image: $fileName")

        val dimensions = Dimensions(
            image.size.useContents { width.toInt() },
            image.size.useContents { height.toInt() }
        )

        // Generate thumbnail
        val thumbnail = generateThumbnail(
            fileData,
            128,
            128,
            maintainAspectRatio = true
        )

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
    ): ByteArray = withContext(Dispatchers.Default) {
        val nsData = imageData.toNSData()
        val original = UIImage.imageWithData(nsData)
            ?: throw IllegalArgumentException("Failed to decode image for thumbnail")

        val targetDimensions = if (maintainAspectRatio) {
            val originalWidth = original.size.useContents { width.toInt() }
            val originalHeight = original.size.useContents { height.toInt() }
            AssetUtils.calculateThumbnailDimensions(
                originalWidth,
                originalHeight,
                width,
                height
            )
        } else {
            Dimensions(width, height)
        }

        val resized = original.resizeToSize(
            targetDimensions.width.toDouble(),
            targetDimensions.height.toDouble()
        )

        resized.toJpegBytes(0.85)
    }

    /**
     * Optimize image (compress)
     */
    actual suspend fun optimizeImage(
        imageData: ByteArray,
        quality: Int
    ): ByteArray = withContext(Dispatchers.Default) {
        val nsData = imageData.toNSData()
        val image = UIImage.imageWithData(nsData)
            ?: throw IllegalArgumentException("Failed to decode image for optimization")

        image.toJpegBytes(quality / 100.0)
    }

    /**
     * Extract image dimensions without fully loading
     */
    actual suspend fun extractDimensions(imageData: ByteArray): Dimensions {
        val nsData = imageData.toNSData()
        val image = UIImage.imageWithData(nsData)
            ?: throw IllegalArgumentException("Failed to decode image for dimension extraction")

        return Dimensions(
            image.size.useContents { width.toInt() },
            image.size.useContents { height.toInt() }
        )
    }

    /**
     * Convert image to different format
     */
    actual suspend fun convertFormat(
        imageData: ByteArray,
        targetFormat: ImageFormat
    ): ByteArray = withContext(Dispatchers.Default) {
        val nsData = imageData.toNSData()
        val image = UIImage.imageWithData(nsData)
            ?: throw IllegalArgumentException("Failed to decode image for format conversion")

        when (targetFormat) {
            ImageFormat.JPEG -> image.toJpegBytes(0.90)
            ImageFormat.PNG -> image.toPngBytes()
            ImageFormat.WEBP -> {
                // WebP not natively supported on iOS, fallback to PNG
                image.toPngBytes()
            }
            else -> image.toPngBytes() // Default to PNG
        }
    }

    // Extension functions for UIImage conversion

    private fun UIImage.toPngBytes(): ByteArray {
        val data = UIImagePNGRepresentation(this)
            ?: throw IllegalStateException("Failed to convert image to PNG")
        return data.toByteArray()
    }

    private fun UIImage.toJpegBytes(quality: Double): ByteArray {
        val data = UIImageJPEGRepresentation(this, quality)
            ?: throw IllegalStateException("Failed to convert image to JPEG")
        return data.toByteArray()
    }

    private fun UIImage.resizeToSize(targetWidth: Double, targetHeight: Double): UIImage {
        val size = CGSizeMake(targetWidth, targetHeight)

        UIGraphicsBeginImageContextWithOptions(size, false, 0.0)
        this.drawInRect(CGRectMake(0.0, 0.0, targetWidth, targetHeight))
        val resizedImage = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return resizedImage ?: this
    }

    // Helper functions for ByteArray <-> NSData conversion

    private fun ByteArray.toNSData(): NSData {
        return this.usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
        }
    }

    private fun NSData.toByteArray(): ByteArray {
        return ByteArray(this.length.toInt()).apply {
            usePinned { pinned ->
                memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
            }
        }
    }
}
