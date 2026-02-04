/**
 * SQLDelightAppVersionRepository.kt - SQLDelight implementation of IAppVersionRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-12-14
 *
 * Implements app version tracking using SQLDelight.
 * Thread-safe, KMP-compatible implementation.
 */

package com.augmentalis.database.repositories

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.AppVersionDTO
import com.augmentalis.database.repositories.IAppVersionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of app version repository.
 *
 * ## Thread Safety:
 * - All database operations use Dispatchers.Default
 * - SQLDelight handles connection pooling and thread safety
 * - Transactions are atomic and isolated
 *
 * ## Performance:
 * - Primary key lookups: O(log N) via B-tree index
 * - Batch operations: Single transaction for atomicity
 * - getAllAppVersions: O(N) but typically <50 apps
 *
 * @property database SQLDelight database instance
 */
class SQLDelightAppVersionRepository(
    private val database: VoiceOSDatabase
) : IAppVersionRepository {

    private val queries = database.appVersionQueries

    override suspend fun getAppVersion(packageName: String): AppVersionDTO? = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }

        queries.getAppVersion(packageName)
            .executeAsOneOrNull()
            ?.let { entity ->
                AppVersionDTO(
                    packageName = entity.package_name,
                    versionName = entity.version_name,
                    versionCode = entity.version_code,
                    lastChecked = entity.last_checked
                )
            }
    }

    override suspend fun getAllAppVersions(): Map<String, AppVersionDTO> = withContext(Dispatchers.Default) {
        queries.getAllAppVersions()
            .asFlow()
            .mapToList(Dispatchers.Default)
            .first()
            .associate { entity ->
                entity.package_name to AppVersionDTO(
                    packageName = entity.package_name,
                    versionName = entity.version_name,
                    versionCode = entity.version_code,
                    lastChecked = entity.last_checked
                )
            }
    }

    override suspend fun upsertAppVersion(packageName: String, versionName: String, versionCode: Long) = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }
        require(versionName.isNotBlank()) { "versionName cannot be blank" }
        require(versionCode >= 0) { "versionCode must be non-negative" }

        val now = System.currentTimeMillis()

        // Manual UPSERT: try to update first, insert if not exists
        database.transaction {
            val existing = queries.getAppVersion(packageName).executeAsOneOrNull()

            if (existing != null) {
                // Update existing record
                queries.updateAppVersion(
                    version_name = versionName,
                    version_code = versionCode,
                    last_checked = now,
                    package_name = packageName
                )
            } else {
                // Insert new record
                queries.insertAppVersion(
                    package_name = packageName,
                    version_name = versionName,
                    version_code = versionCode,
                    last_checked = now
                )
            }
        }
    }

    override suspend fun updateAppVersion(packageName: String, versionName: String, versionCode: Long): Boolean = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }
        require(versionName.isNotBlank()) { "versionName cannot be blank" }
        require(versionCode >= 0) { "versionCode must be non-negative" }

        // Check if exists
        queries.getAppVersion(packageName).executeAsOneOrNull()
            ?: return@withContext false

        val now = System.currentTimeMillis()

        queries.updateAppVersion(
            version_name = versionName,
            version_code = versionCode,
            last_checked = now,
            package_name = packageName
        )

        true
    }

    override suspend fun deleteAppVersion(packageName: String): Boolean = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }

        // Check if exists
        queries.getAppVersion(packageName).executeAsOneOrNull()
            ?: return@withContext false

        queries.deleteAppVersion(packageName)
        true
    }

    override suspend fun updateLastChecked(packageName: String) = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }

        val now = System.currentTimeMillis()
        queries.updateLastChecked(now, packageName)
    }

    override suspend fun getStaleAppVersions(olderThan: Long): Map<String, AppVersionDTO> = withContext(Dispatchers.Default) {
        require(olderThan > 0) { "olderThan timestamp must be positive" }

        queries.getAppsCheckedBefore(olderThan)
            .asFlow()
            .mapToList(Dispatchers.Default)
            .first()
            .associate { entity ->
                entity.package_name to AppVersionDTO(
                    packageName = entity.package_name,
                    versionName = entity.version_name,
                    versionCode = entity.version_code,
                    lastChecked = entity.last_checked
                )
            }
    }

    override suspend fun getCount(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }
}
