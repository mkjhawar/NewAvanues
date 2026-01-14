package com.augmentalis.universal.assetmanager

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Represents a version of an asset library
 *
 * @property version Version string (semantic versioning recommended: "1.0.0")
 * @property changelog Description of changes in this version
 * @property publishedAt Timestamp when this version was published (milliseconds since epoch)
 * @property assetIds List of asset IDs included in this version
 * @property metadata Additional version metadata
 */
data class AssetVersion(
    val version: String,
    val changelog: String,
    val publishedAt: Long,
    val assetIds: List<String>,
    val metadata: Map<String, String> = emptyMap()
) {
    /**
     * Compare versions for sorting
     * Attempts semantic version comparison, falls back to string comparison
     */
    fun isNewerThan(other: AssetVersion): Boolean {
        return try {
            val thisParts = version.split(".").map { it.toIntOrNull() ?: 0 }
            val otherParts = other.version.split(".").map { it.toIntOrNull() ?: 0 }

            for (i in 0 until maxOf(thisParts.size, otherParts.size)) {
                val thisPart = thisParts.getOrNull(i) ?: 0
                val otherPart = otherParts.getOrNull(i) ?: 0

                if (thisPart != otherPart) {
                    return thisPart > otherPart
                }
            }
            publishedAt > other.publishedAt
        } catch (e: Exception) {
            version > other.version
        }
    }
}

/**
 * Version history for a library
 */
data class VersionHistory(
    val libraryId: String,
    val libraryType: LibraryType,
    val versions: List<AssetVersion> = emptyList()
) {
    /**
     * Get the current (latest) version
     */
    fun getCurrentVersion(): AssetVersion? {
        return versions.maxByOrNull { it.publishedAt }
    }

    /**
     * Get a specific version by version string
     */
    fun getVersion(versionString: String): AssetVersion? {
        return versions.find { it.version == versionString }
    }

    /**
     * Get all versions sorted by date (newest first)
     */
    fun getSortedVersions(): List<AssetVersion> {
        return versions.sortedByDescending { it.publishedAt }
    }
}

/**
 * Manages versioning for asset libraries
 *
 * Provides version control functionality including publishing new versions,
 * viewing version history, and rolling back to previous versions.
 */
class AssetVersionManager(
    private val repository: AssetRepository = LocalAssetRepository()
) {
    private val versionHistories = mutableMapOf<String, VersionHistory>()
    private val mutex = Mutex()

    /**
     * Publish a new version of a library
     *
     * @param libraryId The library ID
     * @param libraryType Type of library (ICON or IMAGE)
     * @param version Version string (e.g., "1.0.0")
     * @param changelog Description of changes
     * @param assetIds List of asset IDs included in this version
     * @param metadata Additional version metadata
     * @return Result indicating success or failure
     */
    suspend fun publishVersion(
        libraryId: String,
        libraryType: LibraryType,
        version: String,
        changelog: String,
        assetIds: List<String>,
        metadata: Map<String, String> = emptyMap()
    ): Result<AssetVersion> {
        return try {
            mutex.withLock {
                // Validate version doesn't already exist
                val history = getVersionHistory(libraryId, libraryType)
                if (history.versions.any { it.version == version }) {
                    return Result.failure(IllegalArgumentException("Version $version already exists"))
                }

                // Create new version
                val assetVersion = AssetVersion(
                    version = version,
                    changelog = changelog,
                    publishedAt = System.currentTimeMillis(),
                    assetIds = assetIds,
                    metadata = metadata
                )

                // Update history
                val updatedHistory = history.copy(
                    versions = history.versions + assetVersion
                )
                versionHistories["$libraryId:${libraryType.name}"] = updatedHistory

                // Persist to storage
                saveVersionHistory(updatedHistory)

                Result.success(assetVersion)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get version history for a library
     *
     * @param libraryId The library ID
     * @param libraryType Type of library
     * @return Version history
     */
    suspend fun getVersionHistory(
        libraryId: String,
        libraryType: LibraryType
    ): VersionHistory {
        return mutex.withLock {
            val key = "$libraryId:${libraryType.name}"
            versionHistories.getOrPut(key) {
                loadVersionHistory(libraryId, libraryType) ?: VersionHistory(
                    libraryId = libraryId,
                    libraryType = libraryType,
                    versions = emptyList()
                )
            }
        }
    }

    /**
     * Get the current (latest) version of a library
     *
     * @param libraryId The library ID
     * @param libraryType Type of library
     * @return The current version, or null if no versions exist
     */
    suspend fun getCurrentVersion(
        libraryId: String,
        libraryType: LibraryType
    ): AssetVersion? {
        val history = getVersionHistory(libraryId, libraryType)
        return history.getCurrentVersion()
    }

    /**
     * Get a specific version
     *
     * @param libraryId The library ID
     * @param libraryType Type of library
     * @param version Version string
     * @return The version, or null if not found
     */
    suspend fun getVersion(
        libraryId: String,
        libraryType: LibraryType,
        version: String
    ): AssetVersion? {
        val history = getVersionHistory(libraryId, libraryType)
        return history.getVersion(version)
    }

    /**
     * Rollback to a previous version
     *
     * This creates a new version that restores the asset IDs from a previous version.
     *
     * @param libraryId The library ID
     * @param libraryType Type of library
     * @param targetVersion Version to rollback to
     * @param changelog Changelog for the rollback version
     * @return Result with the new version
     */
    suspend fun rollback(
        libraryId: String,
        libraryType: LibraryType,
        targetVersion: String,
        changelog: String = "Rollback to version $targetVersion"
    ): Result<AssetVersion> {
        return try {
            val history = getVersionHistory(libraryId, libraryType)
            val target = history.getVersion(targetVersion)
                ?: return Result.failure(IllegalArgumentException("Version $targetVersion not found"))

            // Get current version to determine new version number
            val currentVersion = history.getCurrentVersion()?.version ?: "0.0.0"
            val newVersion = incrementVersion(currentVersion)

            // Create new version with assets from target version
            publishVersion(
                libraryId = libraryId,
                libraryType = libraryType,
                version = newVersion,
                changelog = changelog,
                assetIds = target.assetIds,
                metadata = mapOf("rollbackFrom" to currentVersion, "rollbackTo" to targetVersion)
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete a version from history
     *
     * Note: This doesn't delete the actual assets, just the version record
     *
     * @param libraryId The library ID
     * @param libraryType Type of library
     * @param version Version to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteVersion(
        libraryId: String,
        libraryType: LibraryType,
        version: String
    ): Result<Unit> {
        return try {
            mutex.withLock {
                val history = getVersionHistory(libraryId, libraryType)
                val updatedVersions = history.versions.filter { it.version != version }

                if (updatedVersions.size == history.versions.size) {
                    return Result.failure(IllegalArgumentException("Version $version not found"))
                }

                val updatedHistory = history.copy(versions = updatedVersions)
                versionHistories["$libraryId:${libraryType.name}"] = updatedHistory

                saveVersionHistory(updatedHistory)

                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Compare two versions and get the differences
     *
     * @param libraryId The library ID
     * @param libraryType Type of library
     * @param fromVersion Starting version
     * @param toVersion Target version
     * @return Version diff showing added, removed, and unchanged assets
     */
    suspend fun compareVersions(
        libraryId: String,
        libraryType: LibraryType,
        fromVersion: String,
        toVersion: String
    ): VersionDiff? {
        val history = getVersionHistory(libraryId, libraryType)
        val from = history.getVersion(fromVersion) ?: return null
        val to = history.getVersion(toVersion) ?: return null

        val fromSet = from.assetIds.toSet()
        val toSet = to.assetIds.toSet()

        return VersionDiff(
            fromVersion = fromVersion,
            toVersion = toVersion,
            addedAssets = (toSet - fromSet).toList(),
            removedAssets = (fromSet - toSet).toList(),
            unchangedAssets = (fromSet intersect toSet).toList()
        )
    }

    /**
     * Load all version histories from storage
     */
    suspend fun loadAllHistories() {
        // Implementation would load from repository
        // For now, this is a placeholder
    }

    private fun incrementVersion(version: String): String {
        return try {
            val parts = version.split(".").map { it.toIntOrNull() ?: 0 }
            val major = parts.getOrNull(0) ?: 0
            val minor = parts.getOrNull(1) ?: 0
            val patch = parts.getOrNull(2) ?: 0

            "$major.$minor.${patch + 1}"
        } catch (e: Exception) {
            "1.0.0"
        }
    }

    private fun saveVersionHistory(history: VersionHistory): Result<Unit> {
        // TODO: Implement persistence to repository
        // This would save the version history to a versions.json file
        return Result.success(Unit)
    }

    private fun loadVersionHistory(libraryId: String, libraryType: LibraryType): VersionHistory? {
        // TODO: Implement loading from repository
        // This would load the version history from a versions.json file
        return null
    }
}

/**
 * Represents the difference between two versions
 */
data class VersionDiff(
    val fromVersion: String,
    val toVersion: String,
    val addedAssets: List<String>,
    val removedAssets: List<String>,
    val unchangedAssets: List<String>
) {
    /**
     * Get the total number of changes
     */
    fun getTotalChanges(): Int = addedAssets.size + removedAssets.size

    /**
     * Check if there are any changes
     */
    fun hasChanges(): Boolean = addedAssets.isNotEmpty() || removedAssets.isNotEmpty()
}
