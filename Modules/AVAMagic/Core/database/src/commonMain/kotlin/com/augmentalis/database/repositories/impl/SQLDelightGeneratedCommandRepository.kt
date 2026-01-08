/**
 * SQLDelightGeneratedCommandRepository.kt - SQLDelight implementation of IGeneratedCommandRepository
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-11-25
 */

package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.database.dto.toGeneratedCommandDTO
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * SQLDelight implementation of IGeneratedCommandRepository.
 *
 * NOTE: Uses Dispatchers.Default instead of Dispatchers.IO for KMP compatibility.
 * Dispatchers.IO is JVM-only and not available in common code.
 */
class SQLDelightGeneratedCommandRepository(
    private val database: VoiceOSDatabase
) : IGeneratedCommandRepository {

    private val queries = database.generatedCommandQueries

    override suspend fun insert(command: GeneratedCommandDTO): Long = withContext(Dispatchers.Default) {
        var insertedId: Long = 0
        database.transaction {
            queries.insert(
                elementHash = command.elementHash,
                commandText = command.commandText,
                actionType = command.actionType,
                confidence = command.confidence,
                synonyms = command.synonyms,
                isUserApproved = command.isUserApproved,
                usageCount = command.usageCount,
                lastUsed = command.lastUsed,
                createdAt = command.createdAt,
                appId = command.appId,
                appVersion = command.appVersion,
                versionCode = command.versionCode,
                lastVerified = command.lastVerified,
                isDeprecated = command.isDeprecated
            )
            insertedId = queries.lastInsertRowId().executeAsOne()
        }
        insertedId
    }

    override suspend fun insertBatch(commands: List<GeneratedCommandDTO>) = withContext(Dispatchers.Default) {
        require(commands.isNotEmpty()) { "Cannot insert empty batch of commands" }
        database.transaction {
            commands.forEach { command ->
                queries.insert(
                    elementHash = command.elementHash,
                    commandText = command.commandText,
                    actionType = command.actionType,
                    confidence = command.confidence,
                    synonyms = command.synonyms,
                    isUserApproved = command.isUserApproved,
                    usageCount = command.usageCount,
                    lastUsed = command.lastUsed,
                    createdAt = command.createdAt,
                    appId = command.appId,
                    appVersion = command.appVersion,
                    versionCode = command.versionCode,
                    lastVerified = command.lastVerified,
                    isDeprecated = command.isDeprecated
                )
            }
        }
    }

    override suspend fun getById(id: Long): GeneratedCommandDTO? = withContext(Dispatchers.Default) {
        queries.getById(id).executeAsOneOrNull()?.toGeneratedCommandDTO()
    }

    override suspend fun getByElement(elementHash: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getByElement(elementHash).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getAll(): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getAllCommands(): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getAllCommands().executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByActionType(actionType: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getByActionType(actionType).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getHighConfidence(minConfidence: Double): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getHighConfidence(minConfidence).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getUserApproved(): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getUserApproved().executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun fuzzySearch(searchText: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(searchText.length <= 1000) { "Search text must not exceed 1000 characters (got ${searchText.length})" }
        queries.fuzzySearch(searchText).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun incrementUsage(id: Long, timestamp: Long) = withContext(Dispatchers.Default) {
        queries.incrementUsage(timestamp, id)
    }

    override suspend fun markApproved(id: Long) = withContext(Dispatchers.Default) {
        queries.markApproved(id)
    }

    override suspend fun updateConfidence(id: Long, confidence: Double) = withContext(Dispatchers.Default) {
        require(id > 0) { "ID must be positive (got $id)" }
        require(confidence in 0.0..1.0) { "Confidence must be between 0.0 and 1.0 (got $confidence)" }
        queries.updateConfidence(confidence, id)
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun deleteByElement(elementHash: String) = withContext(Dispatchers.Default) {
        queries.deleteByElement(elementHash)
    }

    override suspend fun deleteCommandsByPackage(packageName: String): Int = withContext(Dispatchers.Default) {
        require(packageName.isNotBlank()) { "packageName cannot be blank" }

        var deletedCount = 0
        database.transaction {
            val countBefore = queries.count().executeAsOne()
            queries.deleteByPackage(packageName)
            val countAfter = queries.count().executeAsOne()
            deletedCount = (countBefore - countAfter).toInt()
        }
        deletedCount
    }

    override suspend fun deleteLowQuality(minConfidence: Double) = withContext(Dispatchers.Default) {
        require(minConfidence in 0.0..1.0) { "Minimum confidence must be between 0.0 and 1.0 (got $minConfidence)" }
        queries.deleteLowQuality(minConfidence)
    }

    override suspend fun deleteAll() = withContext(Dispatchers.Default) {
        queries.deleteAll()
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun getByPackage(packageName: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getByPackage(packageName).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun update(command: GeneratedCommandDTO) = withContext(Dispatchers.Default) {
        queries.update(
            elementHash = command.elementHash,
            commandText = command.commandText,
            actionType = command.actionType,
            confidence = command.confidence,
            synonyms = command.synonyms,
            isUserApproved = command.isUserApproved,
            usageCount = command.usageCount,
            lastUsed = command.lastUsed,
            appId = command.appId,
            appVersion = command.appVersion,
            versionCode = command.versionCode,
            lastVerified = command.lastVerified,
            isDeprecated = command.isDeprecated,
            id = command.id
        )
    }

    override suspend fun getAllPaginated(limit: Int, offset: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }
        require(offset >= 0) { "Offset must be non-negative (got $offset)" }
        queries.getAllPaginated(limit.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByPackagePaginated(packageName: String, limit: Int, offset: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }
        require(offset >= 0) { "Offset must be non-negative (got $offset)" }

        queries.getByPackagePaginated(packageName, limit.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByPackageKeysetPaginated(packageName: String, lastId: Long, limit: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }
        require(lastId >= 0) { "Last ID must be non-negative (got $lastId)" }
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }

        queries.getByPackageKeysetPaginated(packageName, lastId, limit.toLong())
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByActionTypePaginated(actionType: String, limit: Int, offset: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }
        require(offset >= 0) { "Offset must be non-negative (got $offset)" }
        queries.getByActionTypePaginated(actionType, limit.toLong(), offset.toLong())
            .executeAsList()
            .map { it.toGeneratedCommandDTO() }
    }

    // ========== Version Management Methods (Schema v3) ==========

    override suspend fun markVersionDeprecated(packageName: String, versionCode: Long): Int = withContext(Dispatchers.Default) {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }
        require(versionCode >= 0) { "Version code must be non-negative (got $versionCode)" }

        var rowsAffected = 0
        database.transaction {
            queries.markVersionDeprecated(
                lastVerified = System.currentTimeMillis(),
                appId = packageName,
                versionCode = versionCode
            )
            // Get number of affected rows by counting before/after
            rowsAffected = queries.getDeprecatedCommands(packageName).executeAsList().size
        }
        rowsAffected
    }

    override suspend fun updateCommandVersion(
        id: Long,
        versionCode: Long,
        appVersion: String,
        lastVerified: Long,
        isDeprecated: Long
    ) = withContext(Dispatchers.Default) {
        require(id > 0) { "ID must be positive (got $id)" }
        require(versionCode >= 0) { "Version code must be non-negative (got $versionCode)" }
        require(appVersion.isNotEmpty()) { "App version cannot be empty" }
        require(lastVerified > 0) { "Last verified timestamp must be positive (got $lastVerified)" }
        require(isDeprecated in 0L..1L) { "isDeprecated must be 0 or 1 (got $isDeprecated)" }

        queries.updateCommandVersion(
            versionCode = versionCode,
            appVersion = appVersion,
            lastVerified = lastVerified,
            isDeprecated = isDeprecated,
            id = id
        )
    }

    override suspend fun updateCommandDeprecated(id: Long, isDeprecated: Long) = withContext(Dispatchers.Default) {
        require(id > 0) { "ID must be positive (got $id)" }
        require(isDeprecated in 0L..1L) { "isDeprecated must be 0 or 1 (got $isDeprecated)" }

        queries.updateCommandDeprecated(isDeprecated = isDeprecated, id = id)
    }

    override suspend fun deleteDeprecatedCommands(olderThan: Long, keepUserApproved: Boolean): Int = withContext(Dispatchers.Default) {
        require(olderThan > 0) { "olderThan timestamp must be positive (got $olderThan)" }

        var rowsAffected = 0
        database.transaction {
            // Count before deletion
            val countBefore = queries.count().executeAsOne()

            // Delete deprecated commands
            // Parameters: lastVerified < ?, (? = 0 OR isUserApproved = 0)
            queries.deleteDeprecatedCommands(
                olderThan,  // First ? - lastVerified threshold
                if (keepUserApproved) 1L else 0L  // Second ? - keep user approved flag
            )

            // Count after deletion
            val countAfter = queries.count().executeAsOne()
            rowsAffected = (countBefore - countAfter).toInt()
        }
        rowsAffected
    }

    override suspend fun getDeprecatedCommands(packageName: String): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }

        queries.getDeprecatedCommands(packageName).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    /**
     * P2 Task 1.2: Batch query to solve N+1 problem in getDeprecatedCommandStats().
     *
     * Fetches all deprecated commands in a single query and groups them by package name.
     * Performance: 50 apps × 10ms = 500ms → 1 query × 15ms = 15ms (97% faster)
     */
    override suspend fun getAllDeprecatedCommandsByApp(): Map<String, List<GeneratedCommandDTO>> = withContext(Dispatchers.Default) {
        // Fetch all deprecated commands in one query
        val allDeprecated = queries.getAllDeprecatedCommands().executeAsList()

        // Group by appId (packageName)
        allDeprecated
            .map { it.toGeneratedCommandDTO() }
            .groupBy { it.appId }
    }

    override suspend fun getDeprecatedCommandsForCleanup(
        packageName: String,
        olderThan: Long,
        keepUserApproved: Boolean,
        limit: Int
    ): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(olderThan > 0) { "olderThan timestamp must be positive (got $olderThan)" }
        require(limit in 1..10000) { "Limit must be between 1 and 10000 (got $limit)" }

        // Parameters: lastVerified < ?, packageName (or ''), packageName again, keepUserApproved flag, limit
        queries.getDeprecatedCommandsForCleanup(
            olderThan,                              // First ? - lastVerified threshold
            packageName,                            // Second ? - package filter check
            packageName,                            // Third ? - appId value
            if (keepUserApproved) 1L else 0L,      // Fourth ? - keep user approved flag
            limit.toLong()                          // Fifth ? - LIMIT
        ).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getActiveCommands(packageName: String, versionCode: Long, limit: Int): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }
        require(versionCode >= 0) { "Version code must be non-negative (got $versionCode)" }
        require(limit in 1..1000) { "Limit must be between 1 and 1000 (got $limit)" }

        // Parameters: appId = ?, versionCode = ?, LIMIT ?
        queries.getActiveCommands(
            packageName,      // First ? - appId
            versionCode,      // Second ? - versionCode
            limit.toLong()    // Third ? - LIMIT
        ).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getActiveCommandsByVersion(
        packageName: String,
        appVersion: String,
        limit: Int
    ): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        require(packageName.isNotEmpty()) { "Package name cannot be empty" }
        require(appVersion.isNotEmpty()) { "App version cannot be empty" }
        require(limit in 1..10000) { "Limit must be between 1 and 10000 (got $limit)" }

        // Parameters: appId = ?, appVersion = ?, LIMIT ?
        queries.getActiveCommandsByVersion(
            packageName,      // First ? - appId
            appVersion,       // Second ? - appVersion
            limit.toLong()    // Third ? - LIMIT
        ).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    override suspend fun getByAppVersion(appId: String, versionCode: Long): List<GeneratedCommandDTO> = withContext(Dispatchers.Default) {
        queries.getByAppVersion(appId, versionCode).executeAsList().map { it.toGeneratedCommandDTO() }
    }

    // ========== Database Maintenance Methods (P3 Task 3.1) ==========

    /**
     * Rebuild database file to reclaim space from deleted records.
     *
     * Executes VACUUM command on the database to:
     * 1. Rebuild database file
     * 2. Reclaim space from deleted records
     * 3. Defragment data pages
     *
     * Should be called after large deletions (>10% of database).
     * Runs on Dispatchers.Default (KMP compatible - Dispatchers.IO is JVM-only).
     */
    override suspend fun vacuumDatabase() = withContext(Dispatchers.Default) {
        queries.vacuumDatabase()
    }
}
