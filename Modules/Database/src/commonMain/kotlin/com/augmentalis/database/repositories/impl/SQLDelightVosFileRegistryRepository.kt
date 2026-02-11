/**
 * SQLDelightVosFileRegistryRepository.kt - SQLDelight implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.database.repositories.impl

import com.augmentalis.database.VoiceOSDatabase
import com.augmentalis.database.dto.VosFileRegistryDTO
import com.augmentalis.database.dto.toVosFileRegistryDTO
import com.augmentalis.database.repositories.IVosFileRegistryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SQLDelightVosFileRegistryRepository(
    private val database: VoiceOSDatabase
) : IVosFileRegistryRepository {

    private val queries = database.vosFileRegistryQueries

    override suspend fun insert(entry: VosFileRegistryDTO) = withContext(Dispatchers.Default) {
        queries.insert(
            file_id = entry.fileId,
            file_type = entry.fileType,
            file_name = entry.fileName,
            content_hash = entry.contentHash,
            command_count = entry.commandCount.toLong(),
            vos_version = entry.vosVersion,
            domain = entry.domain,
            page_title = entry.pageTitle,
            url_patterns = entry.urlPatterns,
            uploader_device_id = entry.uploaderDeviceId,
            user_agent = entry.userAgent,
            scrape_duration_ms = entry.scrapeDurationMs,
            scraped_at = entry.scrapedAt,
            uploaded_at = entry.uploadedAt,
            downloaded_at = entry.downloadedAt,
            source = entry.source,
            local_path = entry.localPath,
            is_active = if (entry.isActive) 1L else 0L,
            version = entry.version.toLong()
        )
    }

    override suspend fun getByFileId(fileId: String): List<VosFileRegistryDTO> = withContext(Dispatchers.Default) {
        queries.getByFileId(fileId).executeAsList().map { it.toVosFileRegistryDTO() }
    }

    override suspend fun getByDomain(domain: String): List<VosFileRegistryDTO> = withContext(Dispatchers.Default) {
        queries.getByDomain(domain).executeAsList().map { it.toVosFileRegistryDTO() }
    }

    override suspend fun getByHash(contentHash: String): List<VosFileRegistryDTO> = withContext(Dispatchers.Default) {
        queries.getByHash(contentHash).executeAsList().map { it.toVosFileRegistryDTO() }
    }

    override suspend fun getActive(): List<VosFileRegistryDTO> = withContext(Dispatchers.Default) {
        queries.getActive().executeAsList().map { it.toVosFileRegistryDTO() }
    }

    override suspend fun getAllByType(fileType: String): List<VosFileRegistryDTO> = withContext(Dispatchers.Default) {
        queries.getAllByType(fileType).executeAsList().map { it.toVosFileRegistryDTO() }
    }

    override suspend fun getLatestVersion(fileId: String, fileType: String): VosFileRegistryDTO? = withContext(Dispatchers.Default) {
        queries.getLatestVersion(fileId, fileType).executeAsOneOrNull()?.toVosFileRegistryDTO()
    }

    override suspend fun existsByHash(contentHash: String): Boolean = withContext(Dispatchers.Default) {
        queries.existsByHash(contentHash).executeAsOne() > 0
    }

    override suspend fun deactivateOldVersions(fileId: String, fileType: String, keepVersion: Int) = withContext(Dispatchers.Default) {
        queries.deactivateOldVersions(fileId, fileType, keepVersion.toLong())
    }

    override suspend fun updateUploadedAt(id: Long, uploadedAt: Long) = withContext(Dispatchers.Default) {
        queries.updateUploadedAt(uploadedAt, id)
    }

    override suspend fun updateDownloadedAt(id: Long, downloadedAt: Long) = withContext(Dispatchers.Default) {
        queries.updateDownloadedAt(downloadedAt, id)
    }

    override suspend fun deleteById(id: Long) = withContext(Dispatchers.Default) {
        queries.deleteById(id)
    }

    override suspend fun getAll(): List<VosFileRegistryDTO> = withContext(Dispatchers.Default) {
        queries.getAll().executeAsList().map { it.toVosFileRegistryDTO() }
    }

    override suspend fun count(): Long = withContext(Dispatchers.Default) {
        queries.count().executeAsOne()
    }

    override suspend fun countActiveByType(fileType: String): Long = withContext(Dispatchers.Default) {
        queries.countActiveByType(fileType).executeAsOne()
    }

    override suspend fun getNotUploaded(): List<VosFileRegistryDTO> = withContext(Dispatchers.Default) {
        queries.getNotUploaded().executeAsList().map { it.toVosFileRegistryDTO() }
    }
}
