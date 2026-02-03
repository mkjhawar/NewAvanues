package com.augmentalis.webavanue.repository

import com.augmentalis.webavanue.SitePermission
import com.augmentalis.webavanue.data.db.BrowserDatabase
import com.augmentalis.webavanue.toDomainModel
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock

/**
 * Repository interface for site permission operations.
 *
 * Handles all permission-related data persistence including:
 * - Permission CRUD operations per domain
 * - Permission type management (location, camera, microphone, etc.)
 */
interface SitePermissionRepository {
    /** Gets a specific permission for a domain */
    suspend fun getSitePermission(domain: String, permissionType: String): Result<SitePermission?>

    /** Inserts or updates a site permission */
    suspend fun insertSitePermission(domain: String, permissionType: String, granted: Boolean): Result<Unit>

    /** Deletes a specific permission for a domain */
    suspend fun deleteSitePermission(domain: String, permissionType: String): Result<Unit>

    /** Deletes all permissions for a domain */
    suspend fun deleteAllSitePermissions(domain: String): Result<Unit>

    /** Gets all site permissions */
    suspend fun getAllSitePermissions(): Result<List<SitePermission>>
}

/**
 * SQLDelight implementation of SitePermissionRepository.
 *
 * @param database SQLDelight database instance
 */
class SitePermissionRepositoryImpl(
    private val database: BrowserDatabase
) : SitePermissionRepository {

    private val queries = database.browserDatabaseQueries

    override suspend fun getSitePermission(
        domain: String,
        permissionType: String
    ): Result<SitePermission?> = withContext(Dispatchers.IO) {
        try {
            val permission = queries.getSitePermission(domain, permissionType)
                .executeAsOneOrNull()
                ?.toDomainModel()
            Result.success(permission)
        } catch (e: Exception) {
            Napier.e("Error getting site permission: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun insertSitePermission(
        domain: String,
        permissionType: String,
        granted: Boolean
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.insertSitePermission(
                domain = domain,
                permission_type = permissionType,
                granted = if (granted) 1L else 0L,
                timestamp = Clock.System.now().toEpochMilliseconds()
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error inserting site permission: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun deleteSitePermission(
        domain: String,
        permissionType: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteSitePermission(domain, permissionType)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting site permission: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun deleteAllSitePermissions(domain: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            queries.deleteAllSitePermissions(domain)
            Result.success(Unit)
        } catch (e: Exception) {
            Napier.e("Error deleting all site permissions: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun getAllSitePermissions(): Result<List<SitePermission>> = withContext(Dispatchers.IO) {
        try {
            val permissions = queries.getAllSitePermissions()
                .executeAsList()
                .map { it.toDomainModel() }
            Result.success(permissions)
        } catch (e: Exception) {
            Napier.e("Error getting all site permissions: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "SitePermissionRepository"
    }
}
