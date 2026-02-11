/**
 * VosFileImporter.kt - Imports .app.vos and .web.vos files into the voice command DB
 *
 * Parses VOS files using ArrayJsonParser, deduplicates by content hash via the
 * VOS file registry, and batch-inserts commands using VoiceCommandDaoAdapter.
 *
 * Usage:
 *   val importer = VosFileImporter(registry, commandDao)
 *   val result = importer.importFromFile("/path/to/google.com.web.vos")
 *   val result = importer.importFromContent(vosJsonString, "downloaded")
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.vos

import android.util.Log
import com.augmentalis.database.dto.VosFileRegistryDTO
import com.augmentalis.database.repositories.IVosFileRegistryRepository
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandDaoAdapter
import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandEntity
import com.augmentalis.voiceoscore.managers.commandmanager.loader.ArrayJsonParser
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

        // Parse VOS JSON
        val parseResult = ArrayJsonParser.parseCommandsJson(content, isFallback = false)
        if (parseResult is ArrayJsonParser.ParseResult.Error) {
            Log.e(TAG, "Parse failed: ${parseResult.message}")
            return ImportResult(success = false, errorMessage = "Parse error: ${parseResult.message}")
        }

        val parsed = parseResult as ArrayJsonParser.ParseResult.Success
        val commands = parsed.commands

        if (commands.isEmpty()) {
            return ImportResult(success = false, errorMessage = "No commands in VOS file")
        }

        // Batch insert into commands DB
        val insertedIds = try {
            commandDao.insertBatch(commands)
        } catch (e: Exception) {
            Log.e(TAG, "Batch insert failed", e)
            return ImportResult(success = false, errorMessage = "Insert error: ${e.message}")
        }

        // Determine file type and fileId from content
        val fileType = detectFileType(fileName, content)
        val fileId = detectFileId(fileName, parsed.locale, content)

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
            commandCount = commands.size,
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

        Log.i(TAG, "Imported ${commands.size} commands from ${fileName ?: "content"} (type=$fileType, version=$nextVersion)")

        return ImportResult(
            success = true,
            commandCount = commands.size,
            insertedIds = insertedIds,
            contentHash = hash
        )
    }

    /**
     * Detect file type from filename or content.
     * Returns "app" or "web".
     */
    private fun detectFileType(fileName: String?, content: String): String {
        if (fileName != null) {
            if (fileName.endsWith(".app.vos")) return "app"
            if (fileName.endsWith(".web.vos")) return "web"
        }
        // Fall back to checking content for domain field
        return try {
            val json = org.json.JSONObject(content)
            json.optString("domain", "app")
        } catch (_: Exception) {
            "app"
        }
    }

    /**
     * Detect fileId from filename, locale, or content.
     * For app files: locale (e.g., "en-US")
     * For web files: domain name (e.g., "google.com")
     */
    private fun detectFileId(fileName: String?, locale: String, content: String): String {
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
        // Fall back to parsed locale or content domain
        return try {
            val json = org.json.JSONObject(content)
            json.optString("source_domain", locale)
        } catch (_: Exception) {
            locale
        }
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
}
