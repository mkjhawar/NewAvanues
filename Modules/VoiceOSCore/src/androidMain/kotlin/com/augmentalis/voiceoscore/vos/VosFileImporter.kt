/**
 * VosFileImporter.kt - Imports .app.vos and .web.vos files into the voice command DB
 *
 * Parses VOS files using VosParser (KMP, auto-detects JSON v2.1 and compact v3.0),
 * deduplicates by content hash via the VOS file registry, and batch-inserts
 * commands using VoiceCommandDaoAdapter.
 *
 * Usage:
 *   val importer = VosFileImporter(registry, commandDao)
 *   val result = importer.importFromFile("/path/to/google.com.web.vos")
 *   val result = importer.importFromContent(vosContent, "downloaded")
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.vos

import android.util.Log
import com.augmentalis.database.dto.VosFileRegistryDTO
import com.augmentalis.database.repositories.IVosFileRegistryRepository
import com.augmentalis.voiceoscore.loader.VosParsedCommand
import com.augmentalis.voiceoscore.loader.VosParseResult
import com.augmentalis.voiceoscore.loader.VosParser
import com.augmentalis.voiceoscore.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
import com.augmentalis.voiceoscore.commandmanager.database.sqldelight.VoiceCommandEntity
import java.io.File
import java.security.MessageDigest

private const val TAG = "VosFileImporter"

data class ImportResult(
    val success: Boolean,
    val commandCount: Int = 0,
    val insertedIds: List<Long> = emptyList(),
    val contentHash: String? = null,
    val isDuplicate: Boolean = false,
    val errorMessage: String? = null
)

class VosFileImporter(
    private val registry: IVosFileRegistryRepository,
    private val commandDao: VoiceCommandDaoAdapter
) {

    /**
     * Import a VOS file from a local file path.
     *
     * @param filePath Absolute path to the .app.vos or .web.vos file
     * @return ImportResult with insertion details
     */
    suspend fun importFromFile(filePath: String): ImportResult {
        val file = File(filePath)
        if (!file.exists()) {
            return ImportResult(success = false, errorMessage = "File not found: $filePath")
        }
        if (!file.canRead()) {
            return ImportResult(success = false, errorMessage = "Cannot read file: $filePath")
        }

        val content = try {
            file.readText(Charsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read file: $filePath", e)
            return ImportResult(success = false, errorMessage = "Read error: ${e.message}")
        }

        val fileName = file.name
        val source = when {
            filePath.contains("Downloads") -> "downloaded"
            filePath.contains("assets") -> "bundled"
            else -> "local"
        }

        return importFromContentInternal(content, source, fileName, filePath)
    }

    /**
     * Import VOS commands from a JSON string.
     *
     * @param content VOS JSON content
     * @param source Provenance source: "local", "downloaded", "bundled"
     * @return ImportResult with insertion details
     */
    suspend fun importFromContent(content: String, source: String): ImportResult {
        return importFromContentInternal(content, source, null, null)
    }

    /**
     * Check if a file with the given content hash has already been imported.
     */
    suspend fun isDuplicate(contentHash: String): Boolean {
        return registry.existsByHash(contentHash)
    }

    /**
     * Core import logic: parse, dedup, insert, register.
     */
    private suspend fun importFromContentInternal(
        content: String,
        source: String,
        fileName: String?,
        filePath: String?
    ): ImportResult {
        val hash = sha256(content)

        // Dedup check
        if (registry.existsByHash(hash)) {
            Log.i(TAG, "Skipping import — duplicate content (hash=$hash)")
            return ImportResult(
                success = true,
                contentHash = hash,
                isDuplicate = true,
                errorMessage = "Already imported (identical content)"
            )
        }

        // Parse VOS file (auto-detects JSON v2.1 or compact v3.0)
        val parseResult = VosParser.parse(content, isFallback = false)
        if (parseResult is VosParseResult.Error) {
            Log.e(TAG, "Parse failed: ${parseResult.message}")
            return ImportResult(success = false, errorMessage = "Parse error: ${parseResult.message}")
        }

        val parsed = parseResult as VosParseResult.Success
        val entities = parsed.commands.map { it.toEntity() }

        if (entities.isEmpty()) {
            return ImportResult(success = false, errorMessage = "No commands in VOS file")
        }

        // Batch insert into commands DB
        val insertedIds = try {
            commandDao.insertBatch(entities)
        } catch (e: Exception) {
            Log.e(TAG, "Batch insert failed", e)
            return ImportResult(success = false, errorMessage = "Insert error: ${e.message}")
        }

        // Determine file type and fileId from parsed result or filename
        val fileType = detectFileType(fileName, parsed.domain)
        val fileId = detectFileId(fileName, parsed.locale, parsed.domain)

        // Register in VOS file registry
        val latestDto = registry.getLatestVersion(fileId, fileType)
        val nextVersion = (latestDto?.version ?: 0) + 1
        registry.deactivateOldVersions(fileId, fileType, nextVersion)

        val dto = VosFileRegistryDTO(
            id = 0,
            fileId = fileId,
            fileType = fileType,
            fileName = fileName ?: "$fileId.$fileType.vos",
            contentHash = hash,
            commandCount = entities.size,
            vosVersion = parsed.version,
            domain = if (fileType == "web") fileId else null,
            pageTitle = extractPageTitle(content),
            urlPatterns = extractUrlPatterns(content),
            uploaderDeviceId = null,
            userAgent = null,
            scrapeDurationMs = null,
            scrapedAt = System.currentTimeMillis(),
            uploadedAt = null,
            downloadedAt = if (source == "downloaded") System.currentTimeMillis() else null,
            source = source,
            localPath = filePath,
            isActive = true,
            version = nextVersion
        )

        try {
            registry.insert(dto)
        } catch (e: Exception) {
            Log.w(TAG, "Registry insert failed (commands still imported): ${e.message}")
        }

        Log.i(TAG, "Imported ${entities.size} commands from ${fileName ?: "content"} (type=$fileType, version=$nextVersion)")

        return ImportResult(
            success = true,
            commandCount = entities.size,
            insertedIds = insertedIds,
            contentHash = hash
        )
    }

    /**
     * Detect file type from filename or parsed domain.
     * Returns "app" or "web".
     */
    private fun detectFileType(fileName: String?, parsedDomain: String): String {
        if (fileName != null) {
            if (fileName.endsWith(".app.vos")) return "app"
            if (fileName.endsWith(".web.vos")) return "web"
        }
        return parsedDomain
    }

    /**
     * Detect fileId from filename, locale, or parsed domain.
     * For app files: locale (e.g., "en-US")
     * For web files: domain name (e.g., "google.com")
     */
    private fun detectFileId(fileName: String?, locale: String, parsedDomain: String): String {
        if (fileName != null) {
            // "en-US.app.vos" → "en-US"
            if (fileName.endsWith(".app.vos")) {
                return fileName.removeSuffix(".app.vos")
            }
            // "google.com.web.vos" → "google.com"
            if (fileName.endsWith(".web.vos")) {
                return fileName.removeSuffix(".web.vos")
            }
        }
        // Fall back to domain name for web files, locale for app files
        return if (parsedDomain == "web") parsedDomain else locale
    }

    /**
     * Extract page_title from VOS JSON if present (web files).
     */
    private fun extractPageTitle(content: String): String? {
        return try {
            val json = org.json.JSONObject(content)
            val title = json.optString("page_title", "")
            title.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extract url_patterns from VOS JSON if present (web files).
     */
    private fun extractUrlPatterns(content: String): String? {
        return try {
            val json = org.json.JSONObject(content)
            val patterns = json.optJSONArray("url_patterns")
            patterns?.toString()
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Compute SHA-256 hash of content string.
     */
    private fun sha256(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(content.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Map KMP [VosParsedCommand] to Android [VoiceCommandEntity].
     */
    private fun VosParsedCommand.toEntity() = VoiceCommandEntity(
        id = id,
        locale = locale,
        primaryText = primaryText,
        synonyms = VosParser.synonymsToJson(synonyms),
        description = description,
        category = category,
        actionType = actionType,
        metadata = metadata,
        priority = 50,
        isFallback = isFallback,
        resourceId = resourceId,
        elementHash = elementHash,
        className = className
    )
}
