/**
 * IVuidRepository.kt - VUID storage repository interface
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Repository interface for VUID (Voice Universal ID) persistence.
 * VUIDs are deterministic identifiers for UI elements.
 */
package com.augmentalis.voiceoscoreng.repository

import com.augmentalis.voiceoscoreng.speech.currentTimeMillis
import kotlinx.coroutines.flow.Flow

/**
 * Repository for VUID (Voice Universal ID) storage.
 *
 * VUIDs are deterministic hashes of UI element properties:
 * - Package name
 * - Activity/screen name
 * - Element bounds or position
 * - Element type and text
 */
interface IVuidRepository {

    /**
     * Save a VUID entry.
     */
    suspend fun save(entry: VuidEntry): Result<Unit>

    /**
     * Save multiple VUID entries in batch.
     */
    suspend fun saveAll(entries: List<VuidEntry>): Result<Unit>

    /**
     * Get VUID entry by ID.
     */
    suspend fun getById(vuid: String): VuidEntry?

    /**
     * Get all VUIDs for a package.
     */
    suspend fun getByPackage(packageName: String): List<VuidEntry>

    /**
     * Get VUIDs for a specific screen.
     */
    suspend fun getByScreen(packageName: String, activityName: String): List<VuidEntry>

    /**
     * Update alias (voice label) for a VUID.
     */
    suspend fun updateAlias(vuid: String, alias: String): Result<Unit>

    /**
     * Delete VUID entry.
     */
    suspend fun delete(vuid: String): Result<Unit>

    /**
     * Delete all VUIDs for a package.
     */
    suspend fun deleteByPackage(packageName: String): Result<Unit>

    /**
     * Check if VUID exists.
     */
    suspend fun exists(vuid: String): Boolean

    /**
     * Observe VUIDs for a screen.
     */
    fun observeByScreen(packageName: String, activityName: String): Flow<List<VuidEntry>>

    /**
     * Get VUID by hash (for deduplication).
     */
    suspend fun getByHash(hash: String): VuidEntry?
}

/**
 * VUID entry data model.
 */
data class VuidEntry(
    /**
     * Unique VUID identifier
     */
    val vuid: String,

    /**
     * App package name
     */
    val packageName: String,

    /**
     * Screen/activity name
     */
    val activityName: String,

    /**
     * Element content hash
     */
    val contentHash: String,

    /**
     * Element type (button, text, etc.)
     */
    val elementType: String,

    /**
     * User-assigned voice alias
     */
    val alias: String? = null,

    /**
     * Element text content
     */
    val text: String? = null,

    /**
     * Element content description
     */
    val contentDescription: String? = null,

    /**
     * Element bounds [left, top, right, bottom]
     */
    val bounds: IntArray? = null,

    /**
     * Creation timestamp
     */
    val createdAt: Long = currentTimeMillis(),

    /**
     * Last updated timestamp
     */
    val updatedAt: Long = currentTimeMillis(),

    /**
     * Additional metadata
     */
    val metadata: Map<String, String> = emptyMap()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as VuidEntry

        if (vuid != other.vuid) return false
        if (packageName != other.packageName) return false
        if (activityName != other.activityName) return false
        if (contentHash != other.contentHash) return false

        return true
    }

    override fun hashCode(): Int {
        var result = vuid.hashCode()
        result = 31 * result + packageName.hashCode()
        result = 31 * result + activityName.hashCode()
        result = 31 * result + contentHash.hashCode()
        return result
    }
}

/**
 * In-memory implementation for testing.
 */
class InMemoryVuidRepository : IVuidRepository {
    private val entries = mutableMapOf<String, VuidEntry>()

    override suspend fun save(entry: VuidEntry): Result<Unit> {
        entries[entry.vuid] = entry
        return Result.success(Unit)
    }

    override suspend fun saveAll(entries: List<VuidEntry>): Result<Unit> {
        entries.forEach { this.entries[it.vuid] = it }
        return Result.success(Unit)
    }

    override suspend fun getById(vuid: String): VuidEntry? = entries[vuid]

    override suspend fun getByPackage(packageName: String): List<VuidEntry> {
        return entries.values.filter { it.packageName == packageName }
    }

    override suspend fun getByScreen(packageName: String, activityName: String): List<VuidEntry> {
        return entries.values.filter {
            it.packageName == packageName && it.activityName == activityName
        }
    }

    override suspend fun updateAlias(vuid: String, alias: String): Result<Unit> {
        entries[vuid]?.let {
            entries[vuid] = it.copy(alias = alias, updatedAt = currentTimeMillis())
        }
        return Result.success(Unit)
    }

    override suspend fun delete(vuid: String): Result<Unit> {
        entries.remove(vuid)
        return Result.success(Unit)
    }

    override suspend fun deleteByPackage(packageName: String): Result<Unit> {
        val keysToRemove = entries.entries
            .filter { it.value.packageName == packageName }
            .map { it.key }
        keysToRemove.forEach { entries.remove(it) }
        return Result.success(Unit)
    }

    override suspend fun exists(vuid: String): Boolean = entries.containsKey(vuid)

    override fun observeByScreen(packageName: String, activityName: String): Flow<List<VuidEntry>> {
        return kotlinx.coroutines.flow.flowOf(
            entries.values.filter {
                it.packageName == packageName && it.activityName == activityName
            }
        )
    }

    override suspend fun getByHash(hash: String): VuidEntry? {
        return entries.values.find { it.contentHash == hash }
    }
}
