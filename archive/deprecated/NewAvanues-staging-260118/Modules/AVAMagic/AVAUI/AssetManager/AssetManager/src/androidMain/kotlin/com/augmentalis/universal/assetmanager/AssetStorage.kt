package com.augmentalis.universal.assetmanager

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Android implementation of LocalAssetStorage
 *
 * Uses Android internal storage for asset persistence.
 * Storage structure mirrors the common interface design.
 */
actual class LocalAssetStorage actual constructor(
    private val basePath: String
) : AssetStorage {

    private lateinit var context: Context
    internal lateinit var assetDir: File

    /**
     * Initialize storage with Android context
     * Must be called before using storage operations
     */
    fun initialize(context: Context) {
        this.context = context
        this.assetDir = File(context.filesDir, basePath)
    }

    /**
     * Save an icon to storage
     */
    actual override suspend fun saveIcon(
        libraryId: String,
        icon: Icon
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            val libraryDir = File(assetDir, "Icons/$libraryId")
            libraryDir.mkdirs()

            // Save SVG if present
            icon.svg?.let { svg ->
                val svgDir = File(libraryDir, "svg")
                svgDir.mkdirs()
                val svgFile = File(svgDir, "${icon.id}.svg")
                svgFile.writeText(svg)
            }

            // Save PNG sizes if present
            icon.png?.forEach { (size, bytes) ->
                val pngDir = File(libraryDir, "png/$size")
                pngDir.mkdirs()
                val pngFile = File(pngDir, "${icon.id}.png")
                pngFile.writeBytes(bytes)
            }

            Result.success("file://${libraryDir.absolutePath}/${icon.id}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save an image to storage
     */
    actual override suspend fun saveImage(
        libraryId: String,
        image: ImageAsset
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            val libraryDir = File(assetDir, "Images/$libraryId")
            libraryDir.mkdirs()

            // Save thumbnail if present
            image.thumbnail?.let { thumb ->
                val thumbDir = File(libraryDir, "thumbnails")
                thumbDir.mkdirs()
                val thumbFile = File(thumbDir, "${image.id}.jpg")
                thumbFile.writeBytes(thumb)
            }

            // Note: Actual image data would need to be provided separately
            // This saves metadata and thumbnail only

            Result.success("file://${libraryDir.absolutePath}/${image.id}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load an icon from storage
     */
    actual override suspend fun loadIcon(
        libraryId: String,
        iconId: String
    ): Icon? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            val libraryDir = File(assetDir, "Icons/$libraryId")
            if (!libraryDir.exists()) return@withContext null

            // Load SVG
            val svgFile = File(libraryDir, "svg/$iconId.svg")
            val svg = if (svgFile.exists()) svgFile.readText() else null

            // Load PNG sizes
            val pngSizes = mutableMapOf<Int, ByteArray>()
            AssetProcessorUtils.StandardSizes.ICON_SIZES.forEach { size ->
                val pngFile = File(libraryDir, "png/$size/$iconId.png")
                if (pngFile.exists()) {
                    pngSizes[size] = pngFile.readBytes()
                }
            }

            if (svg != null || pngSizes.isNotEmpty()) {
                Icon(
                    id = iconId,
                    name = iconId.replace("-", " ").replaceFirstChar { it.uppercase() },
                    svg = svg,
                    png = pngSizes.ifEmpty { null },
                    tags = emptyList(),
                    category = null,
                    keywords = emptyList()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Load an image from storage
     */
    actual override suspend fun loadImage(
        libraryId: String,
        imageId: String
    ): ImageAsset? = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            val libraryDir = File(assetDir, "Images/$libraryId")
            if (!libraryDir.exists()) return@withContext null

            // Load thumbnail
            val thumbFile = File(libraryDir, "thumbnails/$imageId.jpg")
            val thumbnail = if (thumbFile.exists()) thumbFile.readBytes() else null

            // Note: This is a simplified version
            // Full implementation would load complete image metadata
            thumbnail?.let {
                ImageAsset(
                    id = imageId,
                    name = imageId,
                    path = "images/$imageId",
                    format = ImageFormat.JPEG,
                    dimensions = Dimensions(0, 0), // Would need to be stored/calculated
                    thumbnail = it,
                    fileSize = 0L,
                    tags = emptyList(),
                    category = null,
                    metadata = emptyMap()
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Delete an icon from storage
     */
    actual override suspend fun deleteIcon(
        libraryId: String,
        iconId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            val libraryDir = File(assetDir, "Icons/$libraryId")

            // Delete SVG
            val svgFile = File(libraryDir, "svg/$iconId.svg")
            if (svgFile.exists()) svgFile.delete()

            // Delete PNG sizes
            AssetProcessorUtils.StandardSizes.ICON_SIZES.forEach { size ->
                val pngFile = File(libraryDir, "png/$size/$iconId.png")
                if (pngFile.exists()) pngFile.delete()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete an image from storage
     */
    actual override suspend fun deleteImage(
        libraryId: String,
        imageId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()

            val libraryDir = File(assetDir, "Images/$libraryId")

            // Delete thumbnail
            val thumbFile = File(libraryDir, "thumbnails/$imageId.jpg")
            if (thumbFile.exists()) thumbFile.delete()

            // Delete image file (would need format info)
            ImageFormat.entries.forEach { format ->
                val imageFile = File(libraryDir, "images/$imageId.${format.extension}")
                if (imageFile.exists()) {
                    imageFile.delete()
                    return@withContext Result.success(Unit)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List all icons in a library
     */
    actual override suspend fun listIcons(libraryId: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                ensureInitialized()

                val svgDir = File(assetDir, "Icons/$libraryId/svg")
                if (!svgDir.exists()) return@withContext emptyList()

                svgDir.listFiles { file -> file.extension == "svg" }
                    ?.map { it.nameWithoutExtension }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * List all images in a library
     */
    actual override suspend fun listImages(libraryId: String): List<String> =
        withContext(Dispatchers.IO) {
            try {
                ensureInitialized()

                val thumbDir = File(assetDir, "Images/$libraryId/thumbnails")
                if (!thumbDir.exists()) return@withContext emptyList()

                thumbDir.listFiles { file -> file.extension == "jpg" }
                    ?.map { it.nameWithoutExtension }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    /**
     * Check if storage directory exists
     */
    actual override suspend fun storageExists(): Boolean = withContext(Dispatchers.IO) {
        try {
            ensureInitialized()
            assetDir.exists()
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Initialize storage (create necessary directories)
     */
    actual override suspend fun initializeStorage(): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                ensureInitialized()

                // Create base directory
                assetDir.mkdirs()

                // Create icon directories
                File(assetDir, "Icons").mkdirs()

                // Create image directories
                File(assetDir, "Images").mkdirs()

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Get storage statistics
     */
    actual override suspend fun getStorageStats(): StorageStats =
        withContext(Dispatchers.IO) {
            try {
                ensureInitialized()

                val iconDir = File(assetDir, "Icons")
                val imageDir = File(assetDir, "Images")

                val iconLibraries = iconDir.listFiles()?.size ?: 0
                val imageLibraries = imageDir.listFiles()?.size ?: 0

                var totalIcons = 0
                var totalImages = 0
                var totalSize = 0L

                // Count icons
                iconDir.walk().forEach { file ->
                    if (file.isFile) {
                        totalSize += file.length()
                        if (file.extension == "svg" || file.extension == "png") {
                            totalIcons++
                        }
                    }
                }

                // Count images
                imageDir.walk().forEach { file ->
                    if (file.isFile) {
                        totalSize += file.length()
                        if (file.parentFile?.name == "thumbnails") {
                            totalImages++
                        }
                    }
                }

                // Get available space
                val availableSpace = assetDir.usableSpace

                StorageStats(
                    totalIconLibraries = iconLibraries,
                    totalImageLibraries = imageLibraries,
                    totalIcons = totalIcons / AssetProcessorUtils.StandardSizes.ICON_SIZES.size, // Approximate
                    totalImages = totalImages,
                    totalSizeBytes = totalSize,
                    availableSpaceBytes = availableSpace
                )
            } catch (e: Exception) {
                StorageStats(
                    totalIconLibraries = 0,
                    totalImageLibraries = 0,
                    totalIcons = 0,
                    totalImages = 0,
                    totalSizeBytes = 0L,
                    availableSpaceBytes = null
                )
            }
        }

    /**
     * Ensure storage is initialized with context
     */
    internal fun ensureInitialized() {
        if (!::context.isInitialized) {
            throw IllegalStateException(
                "LocalAssetStorage not initialized. Call initialize(context) first."
            )
        }
    }
}

/**
 * Android-specific storage utilities
 */
object AndroidStorageUtils {
    /**
     * Get external storage path if available
     */
    fun getExternalStoragePath(context: Context): File? {
        return context.getExternalFilesDir(null)
    }

    /**
     * Check if external storage is available and writable
     */
    fun isExternalStorageWritable(): Boolean {
        return android.os.Environment.getExternalStorageState() ==
                android.os.Environment.MEDIA_MOUNTED
    }

    /**
     * Get cache directory for temporary storage
     */
    fun getCacheDir(context: Context): File {
        return context.cacheDir
    }

    /**
     * Clear cache directory
     */
    fun clearCache(context: Context): Boolean {
        return context.cacheDir.deleteRecursively()
    }

    /**
     * Calculate directory size
     */
    fun getDirectorySize(directory: File): Long {
        var size = 0L
        directory.walk().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    /**
     * Format bytes to human-readable string
     */
    fun formatBytes(bytes: Long): String {
        val kb = 1024
        val mb = kb * 1024
        val gb = mb * 1024

        return when {
            bytes >= gb -> String.format("%.2f GB", bytes.toDouble() / gb)
            bytes >= mb -> String.format("%.2f MB", bytes.toDouble() / mb)
            bytes >= kb -> String.format("%.2f KB", bytes.toDouble() / kb)
            else -> "$bytes B"
        }
    }
}
