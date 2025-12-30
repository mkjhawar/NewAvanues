package com.augmentalis.universal.assetmanager

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Android implementation of LocalAssetRepository
 *
 * Uses LocalAssetStorage for actual persistence operations.
 * Stores library manifests as JSON files alongside assets.
 */
actual class LocalAssetRepository actual constructor() : AssetRepository {

    private val storage = LocalAssetStorage("assets")
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    // ==================== Icon Library Operations ====================

    actual override suspend fun saveIconLibrary(library: IconLibrary): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val libraryDir = File(storage.assetDir, "Icons/${library.id}")
                libraryDir.mkdirs()

                // Save manifest
                val manifestFile = File(libraryDir, "manifest.json")
                val manifest = ManifestConverter.iconLibraryToManifest(library)
                manifestFile.writeText(json.encodeToString<IconLibraryManifest>(manifest))

                // Save each icon
                library.icons.forEach { icon ->
                    storage.saveIcon(library.id, icon)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual override suspend fun loadIconLibrary(id: String): Result<IconLibrary?> =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val manifestFile = File(storage.assetDir, "Icons/$id/manifest.json")
                if (!manifestFile.exists()) {
                    return@withContext Result.success(null)
                }

                val manifest = json.decodeFromString<IconLibraryManifest>(
                    manifestFile.readText()
                )

                // Load all icons
                val iconIds = storage.listIcons(id)
                val icons = iconIds.mapNotNull { iconId ->
                    storage.loadIcon(id, iconId)
                }

                Result.success(ManifestConverter.manifestToIconLibrary(manifest, icons))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual override suspend fun loadAllIconLibraries(): List<IconLibrary> =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val iconsDir = File(storage.assetDir, "Icons")
                if (!iconsDir.exists()) return@withContext emptyList()

                iconsDir.listFiles()?.mapNotNull { libraryDir ->
                    if (libraryDir.isDirectory) {
                        loadIconLibrary(libraryDir.name).getOrNull()
                    } else null
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    actual override suspend fun deleteIconLibrary(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val libraryDir = File(storage.assetDir, "Icons/$id")
                if (libraryDir.exists()) {
                    libraryDir.deleteRecursively()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== Image Library Operations ====================

    actual override suspend fun saveImageLibrary(library: ImageLibrary): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val libraryDir = File(storage.assetDir, "Images/${library.id}")
                libraryDir.mkdirs()

                // Save manifest
                val manifestFile = File(libraryDir, "manifest.json")
                val manifest = ManifestConverter.imageLibraryToManifest(library)
                manifestFile.writeText(json.encodeToString<ImageLibraryManifest>(manifest))

                // Save each image
                library.images.forEach { image ->
                    storage.saveImage(library.id, image)
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual override suspend fun loadImageLibrary(id: String): Result<ImageLibrary?> =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val manifestFile = File(storage.assetDir, "Images/$id/manifest.json")
                if (!manifestFile.exists()) {
                    return@withContext Result.success(null)
                }

                val manifest = json.decodeFromString<ImageLibraryManifest>(
                    manifestFile.readText()
                )

                // Load all images
                val imageIds = storage.listImages(id)
                val images = imageIds.mapNotNull { imageId ->
                    storage.loadImage(id, imageId)
                }

                Result.success(ManifestConverter.manifestToImageLibrary(manifest, images))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    actual override suspend fun loadAllImageLibraries(): List<ImageLibrary> =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val imagesDir = File(storage.assetDir, "Images")
                if (!imagesDir.exists()) return@withContext emptyList()

                imagesDir.listFiles()?.mapNotNull { libraryDir ->
                    if (libraryDir.isDirectory) {
                        loadImageLibrary(libraryDir.name).getOrNull()
                    } else null
                } ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

    actual override suspend fun deleteImageLibrary(id: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val libraryDir = File(storage.assetDir, "Images/$id")
                if (libraryDir.exists()) {
                    libraryDir.deleteRecursively()
                }

                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    // ==================== Individual Asset Operations ====================

    actual override suspend fun saveIconData(
        libraryId: String,
        iconId: String,
        format: IconFormat,
        data: ByteArray,
        size: Int?
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            storage.ensureInitialized()

            val libraryDir = File(storage.assetDir, "Icons/$libraryId")
            libraryDir.mkdirs()

            val path = when (format) {
                IconFormat.SVG -> {
                    val svgDir = File(libraryDir, "svg")
                    svgDir.mkdirs()
                    val file = File(svgDir, "$iconId.svg")
                    file.writeBytes(data)
                    file.absolutePath
                }
                IconFormat.PNG -> {
                    if (size == null) throw IllegalArgumentException("Size required for PNG format")
                    val pngDir = File(libraryDir, "png/$size")
                    pngDir.mkdirs()
                    val file = File(pngDir, "$iconId.png")
                    file.writeBytes(data)
                    file.absolutePath
                }
            }

            Result.success("file://$path")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun loadIconData(
        libraryId: String,
        iconId: String,
        format: IconFormat,
        size: Int?
    ): Result<ByteArray?> = withContext(Dispatchers.IO) {
        try {
            storage.ensureInitialized()

            val libraryDir = File(storage.assetDir, "Icons/$libraryId")
            if (!libraryDir.exists()) return@withContext Result.success(null)

            val data = when (format) {
                IconFormat.SVG -> {
                    val file = File(libraryDir, "svg/$iconId.svg")
                    if (file.exists()) file.readBytes() else null
                }
                IconFormat.PNG -> {
                    if (size == null) return@withContext Result.success(null)
                    val file = File(libraryDir, "png/$size/$iconId.png")
                    if (file.exists()) file.readBytes() else null
                }
            }

            Result.success(data)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun saveImageData(
        libraryId: String,
        imageId: String,
        data: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            storage.ensureInitialized()

            val libraryDir = File(storage.assetDir, "Images/$libraryId")
            val imagesDir = File(libraryDir, "images")
            imagesDir.mkdirs()

            val file = File(imagesDir, "$imageId.jpg")
            file.writeBytes(data)

            Result.success("file://${file.absolutePath}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun loadImageData(
        libraryId: String,
        imageId: String
    ): Result<ByteArray?> = withContext(Dispatchers.IO) {
        try {
            storage.ensureInitialized()

            val libraryDir = File(storage.assetDir, "Images/$libraryId/images")
            if (!libraryDir.exists()) return@withContext Result.success(null)

            // Try common image formats
            val formats = listOf("jpg", "png", "gif", "webp")
            for (ext in formats) {
                val file = File(libraryDir, "$imageId.$ext")
                if (file.exists()) {
                    return@withContext Result.success(file.readBytes())
                }
            }

            Result.success(null)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun saveThumbnail(
        libraryId: String,
        imageId: String,
        thumbnailData: ByteArray
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            storage.ensureInitialized()

            val libraryDir = File(storage.assetDir, "Images/$libraryId")
            val thumbDir = File(libraryDir, "thumbnails")
            thumbDir.mkdirs()

            val file = File(thumbDir, "$imageId.jpg")
            file.writeBytes(thumbnailData)

            Result.success("file://${file.absolutePath}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    actual override suspend fun libraryExists(id: String, type: LibraryType): Boolean =
        withContext(Dispatchers.IO) {
            try {
                storage.ensureInitialized()

                val dir = when (type) {
                    LibraryType.ICON -> File(storage.assetDir, "Icons/$id")
                    LibraryType.IMAGE -> File(storage.assetDir, "Images/$id")
                }

                dir.exists() && dir.isDirectory
            } catch (e: Exception) {
                false
            }
        }
}
