/**
 * IVosFileRegistryRepository.kt - Repository interface for VOS file registry
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.database.repositories

import com.augmentalis.database.dto.VosFileRegistryDTO

/**
 * Repository for VOS file registry entries.
 *
 * Manages the persistence layer of the VOS distribution system.
 * Tracks all VOS files with full provenance metadata for version
 * control, deduplication, and FTP sync.
 */
interface IVosFileRegistryRepository {

    /**
     * Insert or replace a VOS file registry entry.
     */
    suspend fun insert(entry: VosFileRegistryDTO)

    /**
     * Get all versions of a file by its file ID.
     */
    suspend fun getByFileId(fileId: String): List<VosFileRegistryDTO>

    /**
     * Get web VOS files for a specific domain.
     */
    suspend fun getByDomain(domain: String): List<VosFileRegistryDTO>

    /**
     * Get entries matching a content hash (dedup check).
     */
    suspend fun getByHash(contentHash: String): List<VosFileRegistryDTO>

    /**
     * Get all active files.
     */
    suspend fun getActive(): List<VosFileRegistryDTO>

    /**
     * Get all files of a specific type (app or web).
     */
    suspend fun getAllByType(fileType: String): List<VosFileRegistryDTO>

    /**
     * Get the latest version of a file.
     */
    suspend fun getLatestVersion(fileId: String, fileType: String): VosFileRegistryDTO?

    /**
     * Check if a content hash already exists (returns true if duplicate).
     */
    suspend fun existsByHash(contentHash: String): Boolean

    /**
     * Deactivate old versions of a file, keeping only the specified version active.
     */
    suspend fun deactivateOldVersions(fileId: String, fileType: String, keepVersion: Int)

    /**
     * Update the uploaded timestamp for FTP sync tracking.
     */
    suspend fun updateUploadedAt(id: Long, uploadedAt: Long)

    /**
     * Update the downloaded timestamp for FTP sync tracking.
     */
    suspend fun updateDownloadedAt(id: Long, downloadedAt: Long)

    /**
     * Delete a registry entry by ID.
     */
    suspend fun deleteById(id: Long)

    /**
     * Get all registry entries.
     */
    suspend fun getAll(): List<VosFileRegistryDTO>

    /**
     * Count total registry entries.
     */
    suspend fun count(): Long

    /**
     * Count active entries by type.
     */
    suspend fun countActiveByType(fileType: String): Long
}
