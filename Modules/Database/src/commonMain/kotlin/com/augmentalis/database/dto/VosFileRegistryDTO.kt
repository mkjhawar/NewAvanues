/**
 * VosFileRegistryDTO.kt - Data Transfer Object for VOS file registry entries
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-02-11
 */
package com.augmentalis.database.dto

import com.augmentalis.database.Vos_file_registry

/**
 * DTO for VOS file registry entries.
 *
 * Tracks all VOS files (bundled, locally exported, downloaded from FTP)
 * with full provenance metadata for version control and deduplication.
 */
data class VosFileRegistryDTO(
    val id: Long = 0,
    val fileId: String,
    val fileType: String,
    val fileName: String,
    val contentHash: String,
    val commandCount: Int,
    val vosVersion: String,
    val domain: String? = null,
    val pageTitle: String? = null,
    val urlPatterns: String? = null,
    val uploaderDeviceId: String? = null,
    val userAgent: String? = null,
    val scrapeDurationMs: Long? = null,
    val scrapedAt: Long,
    val uploadedAt: Long? = null,
    val downloadedAt: Long? = null,
    val source: String = "local",
    val localPath: String? = null,
    val isActive: Boolean = true,
    val version: Int = 1
)

/**
 * Extension to convert SQLDelight generated type to DTO.
 */
fun Vos_file_registry.toVosFileRegistryDTO(): VosFileRegistryDTO {
    return VosFileRegistryDTO(
        id = id,
        fileId = file_id,
        fileType = file_type,
        fileName = file_name,
        contentHash = content_hash,
        commandCount = command_count.toInt(),
        vosVersion = vos_version,
        domain = domain,
        pageTitle = page_title,
        urlPatterns = url_patterns,
        uploaderDeviceId = uploader_device_id,
        userAgent = user_agent,
        scrapeDurationMs = scrape_duration_ms,
        scrapedAt = scraped_at,
        uploadedAt = uploaded_at,
        downloadedAt = downloaded_at,
        source = source,
        localPath = local_path,
        isActive = is_active == 1L,
        version = version.toInt()
    )
}
