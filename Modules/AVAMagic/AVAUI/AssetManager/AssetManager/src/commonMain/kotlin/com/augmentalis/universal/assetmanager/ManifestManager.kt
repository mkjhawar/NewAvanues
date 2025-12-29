package com.augmentalis.universal.assetmanager

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.datetime.Clock

/**
 * Manages library manifests (metadata files)
 *
 * Handles saving and loading manifest files that describe icon and image libraries.
 * Manifests are stored as JSON files in the library directories.
 */
class ManifestManager(
    private val repository: AssetRepository = LocalAssetRepository()
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Save an icon library manifest
     *
     * @param library The icon library to save
     * @return Result indicating success or failure
     */
    suspend fun saveIconManifest(library: IconLibrary): Result<Unit> {
        return try {
            repository.saveIconLibrary(library)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Save an image library manifest
     *
     * @param library The image library to save
     * @return Result indicating success or failure
     */
    suspend fun saveImageManifest(library: ImageLibrary): Result<Unit> {
        return try {
            repository.saveImageLibrary(library)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Load an icon library manifest
     *
     * @param libraryId The library ID
     * @return The icon library if found, null otherwise
     */
    suspend fun loadIconLibrary(libraryId: String): IconLibrary? {
        return repository.loadIconLibrary(libraryId).getOrNull()
    }

    /**
     * Load an image library manifest
     *
     * @param libraryId The library ID
     * @return The image library if found, null otherwise
     */
    suspend fun loadImageLibrary(libraryId: String): ImageLibrary? {
        return repository.loadImageLibrary(libraryId).getOrNull()
    }

    /**
     * List all icon libraries
     *
     * @return List of all icon libraries
     */
    suspend fun listIconLibraries(): List<IconLibrary> {
        return repository.loadAllIconLibraries()
    }

    /**
     * List all image libraries
     *
     * @return List of all image libraries
     */
    suspend fun listImageLibraries(): List<ImageLibrary> {
        return repository.loadAllImageLibraries()
    }

    /**
     * Check if a library exists
     *
     * @param libraryId The library ID
     * @param type The library type
     * @return True if the library exists
     */
    suspend fun libraryExists(libraryId: String, type: LibraryType): Boolean {
        return repository.libraryExists(libraryId, type)
    }

    /**
     * Delete a library manifest
     *
     * @param libraryId The library ID
     * @param type The library type
     * @return Result indicating success or failure
     */
    suspend fun deleteLibrary(libraryId: String, type: LibraryType): Result<Unit> {
        return when (type) {
            LibraryType.ICON -> repository.deleteIconLibrary(libraryId)
            LibraryType.IMAGE -> repository.deleteImageLibrary(libraryId)
        }
    }

    /**
     * Validate a manifest file
     *
     * @param libraryId The library ID
     * @param type The library type
     * @return Validation result
     */
    suspend fun validateManifest(libraryId: String, type: LibraryType): ManifestValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        try {
            when (type) {
                LibraryType.ICON -> {
                    val library = loadIconLibrary(libraryId)
                    if (library == null) {
                        errors.add("Library not found: $libraryId")
                        return ManifestValidationResult(false, errors, warnings)
                    }

                    // Validate library
                    if (library.id.isBlank()) errors.add("Library ID is empty")
                    if (library.name.isBlank()) errors.add("Library name is empty")
                    if (library.version.isBlank()) errors.add("Library version is empty")
                    if (library.icons.isEmpty()) warnings.add("Library contains no icons")

                    // Validate icons
                    library.icons.forEachIndexed { index, icon ->
                        if (icon.id.isBlank()) errors.add("Icon at index $index has empty ID")
                        if (icon.name.isBlank()) errors.add("Icon ${icon.id} has empty name")
                        if (icon.svg == null && icon.png.isNullOrEmpty()) {
                            errors.add("Icon ${icon.id} has no SVG or PNG data")
                        }
                    }
                }
                LibraryType.IMAGE -> {
                    val library = loadImageLibrary(libraryId)
                    if (library == null) {
                        errors.add("Library not found: $libraryId")
                        return ManifestValidationResult(false, errors, warnings)
                    }

                    // Validate library
                    if (library.id.isBlank()) errors.add("Library ID is empty")
                    if (library.name.isBlank()) errors.add("Library name is empty")
                    if (library.images.isEmpty()) warnings.add("Library contains no images")

                    // Validate images
                    library.images.forEachIndexed { index, image ->
                        if (image.id.isBlank()) errors.add("Image at index $index has empty ID")
                        if (image.name.isBlank()) errors.add("Image ${image.id} has empty name")
                        if (image.path.isBlank()) errors.add("Image ${image.id} has empty path")
                        if (image.dimensions.width <= 0 || image.dimensions.height <= 0) {
                            errors.add("Image ${image.id} has invalid dimensions")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            errors.add("Validation error: ${e.message}")
        }

        return ManifestValidationResult(
            valid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }

    /**
     * Export a library to JSON string
     *
     * @param libraryId The library ID
     * @param type The library type
     * @return JSON string or null if not found
     */
    suspend fun exportToJson(libraryId: String, type: LibraryType): String? {
        return when (type) {
            LibraryType.ICON -> {
                val library = loadIconLibrary(libraryId) ?: return null
                val manifest = ManifestConverter.iconLibraryToManifest(library)
                json.encodeToString(manifest)
            }
            LibraryType.IMAGE -> {
                val library = loadImageLibrary(libraryId) ?: return null
                val manifest = ManifestConverter.imageLibraryToManifest(library)
                json.encodeToString(manifest)
            }
        }
    }

    /**
     * Import a library from JSON string
     *
     * @param jsonString The JSON string
     * @param type The library type
     * @return Result with the imported library
     */
    suspend fun importFromJson(jsonString: String, type: LibraryType): Result<Any> {
        return try {
            when (type) {
                LibraryType.ICON -> {
                    val manifest = json.decodeFromString<IconLibraryManifest>(jsonString)
                    val library = ManifestConverter.manifestToIconLibrary(manifest, emptyList())
                    Result.success(library)
                }
                LibraryType.IMAGE -> {
                    val manifest = json.decodeFromString<ImageLibraryManifest>(jsonString)
                    val library = ManifestConverter.manifestToImageLibrary(manifest, emptyList())
                    Result.success(library)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get manifest metadata
     *
     * @param libraryId The library ID
     * @param type The library type
     * @return Manifest metadata or null if not found
     */
    suspend fun getManifestMetadata(libraryId: String, type: LibraryType): ManifestMetadata? {
        return when (type) {
            LibraryType.ICON -> {
                val library = loadIconLibrary(libraryId) ?: return null
                ManifestMetadata(
                    libraryId = library.id,
                    libraryName = library.name,
                    version = library.version,
                    type = LibraryType.ICON,
                    assetCount = library.icons.size,
                    metadata = library.metadata
                )
            }
            LibraryType.IMAGE -> {
                val library = loadImageLibrary(libraryId) ?: return null
                ManifestMetadata(
                    libraryId = library.id,
                    libraryName = library.name,
                    version = null,
                    type = LibraryType.IMAGE,
                    assetCount = library.images.size,
                    metadata = library.metadata
                )
            }
        }
    }

    /**
     * Update library metadata
     *
     * @param libraryId The library ID
     * @param type The library type
     * @param metadata New metadata to merge
     * @return Result indicating success or failure
     */
    suspend fun updateMetadata(
        libraryId: String,
        type: LibraryType,
        metadata: Map<String, String>
    ): Result<Unit> {
        return try {
            when (type) {
                LibraryType.ICON -> {
                    val library = loadIconLibrary(libraryId)
                        ?: return Result.failure(IllegalArgumentException("Library not found"))
                    val updated = library.copy(metadata = library.metadata + metadata)
                    saveIconManifest(updated)
                }
                LibraryType.IMAGE -> {
                    val library = loadImageLibrary(libraryId)
                        ?: return Result.failure(IllegalArgumentException("Library not found"))
                    val updated = library.copy(metadata = library.metadata + metadata)
                    saveImageManifest(updated)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Manifest validation result
 */
data class ManifestValidationResult(
    val valid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
) {
    fun hasErrors(): Boolean = errors.isNotEmpty()
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    fun getReport(): String {
        val report = StringBuilder()

        if (valid) {
            report.appendLine("Manifest is valid")
        } else {
            report.appendLine("Manifest validation failed")
        }

        if (errors.isNotEmpty()) {
            report.appendLine("\nErrors:")
            errors.forEach { report.appendLine("  - $it") }
        }

        if (warnings.isNotEmpty()) {
            report.appendLine("\nWarnings:")
            warnings.forEach { report.appendLine("  - $it") }
        }

        return report.toString()
    }
}

/**
 * Manifest metadata summary
 */
data class ManifestMetadata(
    val libraryId: String,
    val libraryName: String,
    val version: String?,
    val type: LibraryType,
    val assetCount: Int,
    val metadata: Map<String, String>
)

/**
 * Manifest migration utilities for version upgrades
 */
object ManifestMigration {
    /**
     * Migrate manifest to a newer version
     *
     * @param libraryId The library ID
     * @param fromVersion Source version
     * @param toVersion Target version
     * @return Result indicating success or failure
     */
    suspend fun migrateManifest(
        libraryId: String,
        fromVersion: String,
        toVersion: String
    ): Result<Unit> {
        // Implementation would handle version-specific migrations
        // For example: v1 -> v2 adds new fields, v2 -> v3 changes structure
        return Result.success(Unit)
    }

    /**
     * Check if migration is needed
     *
     * @param currentVersion Current manifest version
     * @param targetVersion Target version
     * @return True if migration is needed
     */
    fun needsMigration(currentVersion: String, targetVersion: String): Boolean {
        return currentVersion != targetVersion
    }

    /**
     * Get migration path (list of versions to migrate through)
     *
     * @param fromVersion Source version
     * @param toVersion Target version
     * @return List of versions in migration path
     */
    fun getMigrationPath(fromVersion: String, toVersion: String): List<String> {
        // Implementation would return intermediate versions
        return listOf(fromVersion, toVersion)
    }
}

/**
 * Manifest backup utilities
 */
object ManifestBackup {
    /**
     * Create a backup of a manifest
     *
     * @param libraryId The library ID
     * @param type The library type
     * @return Result with backup path
     */
    suspend fun backupManifest(libraryId: String, type: LibraryType): Result<String> {
        // Implementation would copy manifest to backup location
        val timestamp = Clock.System.now().toEpochMilliseconds()
        val backupPath = "backups/${type.name.lowercase()}/$libraryId-$timestamp.json"
        return Result.success(backupPath)
    }

    /**
     * Restore a manifest from backup
     *
     * @param backupPath Path to backup file
     * @param type The library type
     * @return Result indicating success or failure
     */
    suspend fun restoreManifest(backupPath: String, type: LibraryType): Result<Unit> {
        // Implementation would restore manifest from backup
        return Result.success(Unit)
    }

    /**
     * List available backups for a library
     *
     * @param libraryId The library ID
     * @param type The library type
     * @return List of backup information
     */
    suspend fun listBackups(libraryId: String, type: LibraryType): List<BackupInfo> {
        // Implementation would scan backup directory
        return emptyList()
    }
}

/**
 * Backup information
 */
data class BackupInfo(
    val path: String,
    val timestamp: Long,
    val sizeBytes: Long,
    val libraryId: String,
    val type: LibraryType
)

/**
 * Manifest comparison utilities
 */
object ManifestComparison {
    /**
     * Compare two manifests and report differences
     *
     * @param manifest1 First manifest
     * @param manifest2 Second manifest
     * @return Comparison result
     */
    internal fun compareIconManifests(
        manifest1: IconLibraryManifest,
        manifest2: IconLibraryManifest
    ): ManifestDiff {
        val addedIcons = manifest2.icons.filter { icon2 ->
            manifest1.icons.none { it.id == icon2.id }
        }.map { it.id }

        val removedIcons = manifest1.icons.filter { icon1 ->
            manifest2.icons.none { it.id == icon1.id }
        }.map { it.id }

        val modifiedIcons = manifest2.icons.filter { icon2 ->
            val icon1 = manifest1.icons.find { it.id == icon2.id }
            icon1 != null && icon1 != icon2
        }.map { it.id }

        return ManifestDiff(
            added = addedIcons,
            removed = removedIcons,
            modified = modifiedIcons
        )
    }

    /**
     * Compare two image manifests
     */
    internal fun compareImageManifests(
        manifest1: ImageLibraryManifest,
        manifest2: ImageLibraryManifest
    ): ManifestDiff {
        val addedImages = manifest2.images.filter { image2 ->
            manifest1.images.none { it.id == image2.id }
        }.map { it.id }

        val removedImages = manifest1.images.filter { image1 ->
            manifest2.images.none { it.id == image1.id }
        }.map { it.id }

        val modifiedImages = manifest2.images.filter { image2 ->
            val image1 = manifest1.images.find { it.id == image2.id }
            image1 != null && image1 != image2
        }.map { it.id }

        return ManifestDiff(
            added = addedImages,
            removed = removedImages,
            modified = modifiedImages
        )
    }
}

/**
 * Manifest difference result
 */
data class ManifestDiff(
    val added: List<String>,
    val removed: List<String>,
    val modified: List<String>
) {
    fun hasChanges(): Boolean = added.isNotEmpty() || removed.isNotEmpty() || modified.isNotEmpty()

    fun getTotalChanges(): Int = added.size + removed.size + modified.size
}

/**
 * Serializable manifest structures for JSON export/import
 */
@Serializable
internal data class SerializableIconLibrary(
    val id: String,
    val name: String,
    val version: String,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val icons: List<SerializableIcon> = emptyList()
)

@Serializable
internal data class SerializableIcon(
    val id: String,
    val name: String,
    val svg: String? = null,
    val pngSizes: List<Int> = emptyList(),
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val keywords: List<String> = emptyList()
)

@Serializable
internal data class SerializableImageLibrary(
    val id: String,
    val name: String,
    val description: String? = null,
    val metadata: Map<String, String> = emptyMap(),
    val images: List<SerializableImage> = emptyList()
)

@Serializable
internal data class SerializableImage(
    val id: String,
    val name: String,
    val path: String,
    val format: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val hasThumbnail: Boolean,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val metadata: Map<String, String> = emptyMap()
)
