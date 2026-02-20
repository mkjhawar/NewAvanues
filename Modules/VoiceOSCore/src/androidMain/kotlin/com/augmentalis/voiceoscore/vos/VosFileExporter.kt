/**
 * VosFileExporter.kt - Exports voice commands to .app.vos and .web.vos files
 *
 * Serializes VoiceCommandEntity lists to VOS v2.1 JSON format and saves
 * to the device's Downloads/commands/ directory. Registers exported files
 * in the VOS file registry for provenance tracking.
 *
 * Usage:
 *   val exporter = VosFileExporter(context, registry)
 *   val result = exporter.exportAppCommands("en-US", appCommands)
 *   val result = exporter.exportWebCommands("google.com", "Google", webCommands)
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.vos

import android.content.Context
import android.os.Environment
import android.util.Log
import com.augmentalis.database.dto.VosFileRegistryDTO
import com.augmentalis.database.repositories.IVosFileRegistryRepository
import com.augmentalis.voiceoscore.commandmanager.database.sqldelight.VoiceCommandEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

private const val TAG = "VosFileExporter"
private const val VOS_VERSION = "2.1"
private const val COMMANDS_DIR = "commands"

data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileName: String? = null,
    val commandCount: Int = 0,
    val contentHash: String? = null,
    val errorMessage: String? = null
)

class VosFileExporter(
    private val context: Context,
    private val registry: IVosFileRegistryRepository
) {

    /**
     * Export app commands for a locale as a .app.vos file.
     *
     * @param locale Locale code (e.g., "en-US")
     * @param commands Commands to export (should be app-domain commands)
     * @return ExportResult with file path and metadata
     */
    suspend fun exportAppCommands(
        locale: String,
        commands: List<VoiceCommandEntity>
    ): ExportResult {
        if (commands.isEmpty()) {
            return ExportResult(success = false, errorMessage = "No commands to export")
        }

        val fileName = "$locale.app.vos"
        val domain = "app"

        // Build VOS JSON
        val vosJson = buildVosJson(
            locale = locale,
            domain = domain,
            fallback = if (locale == "en-US") "en-US" else "en-US",
            commands = commands
        )

        val content = vosJson.toString(2)
        val hash = sha256(content)

        // Check for duplicate
        if (registry.existsByHash(hash)) {
            Log.i(TAG, "Skipping export — identical content already registered (hash=$hash)")
            return ExportResult(
                success = true,
                fileName = fileName,
                commandCount = commands.size,
                contentHash = hash,
                errorMessage = "Already exported (identical content)"
            )
        }

        // Save to Downloads/commands/
        val filePath = saveToLocal(fileName, content)
            ?: return ExportResult(success = false, errorMessage = "Failed to save file: $fileName")

        // Register in VOS file registry
        val latestDto = registry.getLatestVersion(locale, "app")
        val nextVersion = (latestDto?.version ?: 0) + 1

        // Deactivate old versions
        registry.deactivateOldVersions(locale, "app", nextVersion)

        val dto = VosFileRegistryDTO(
            id = 0,
            fileId = locale,
            fileType = "app",
            fileName = fileName,
            contentHash = hash,
            commandCount = commands.size,
            vosVersion = VOS_VERSION,
            domain = null,
            pageTitle = null,
            urlPatterns = null,
            uploaderDeviceId = null,
            userAgent = null,
            scrapeDurationMs = null,
            scrapedAt = System.currentTimeMillis(),
            uploadedAt = null,
            downloadedAt = null,
            source = "local",
            localPath = filePath,
            isActive = true,
            version = nextVersion
        )

        registry.insert(dto)
        Log.i(TAG, "Exported $fileName: ${commands.size} commands, version=$nextVersion")

        return ExportResult(
            success = true,
            filePath = filePath,
            fileName = fileName,
            commandCount = commands.size,
            contentHash = hash
        )
    }

    /**
     * Export web commands for a domain as a .web.vos file.
     *
     * @param domainName Website domain (e.g., "google.com")
     * @param pageTitle Page title at scrape time
     * @param commands Web commands to export
     * @param urlPatterns URL patterns covered by these commands
     * @param scrapeDurationMs How long the scrape took (for provenance)
     * @return ExportResult with file path and metadata
     */
    suspend fun exportWebCommands(
        domainName: String,
        pageTitle: String,
        commands: List<VoiceCommandEntity>,
        urlPatterns: List<String> = emptyList(),
        scrapeDurationMs: Long? = null
    ): ExportResult {
        if (commands.isEmpty()) {
            return ExportResult(success = false, errorMessage = "No commands to export")
        }

        val safeDomain = domainName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val fileName = "$safeDomain.web.vos"

        // Build VOS JSON with web domain metadata
        val vosJson = buildVosJson(
            locale = "universal",
            domain = "web",
            fallback = "en-US",
            commands = commands
        )
        // Add web-specific metadata
        vosJson.put("source_domain", domainName)
        vosJson.put("page_title", pageTitle)
        if (urlPatterns.isNotEmpty()) {
            vosJson.put("url_patterns", JSONArray(urlPatterns))
        }

        val content = vosJson.toString(2)
        val hash = sha256(content)

        // Check for duplicate
        if (registry.existsByHash(hash)) {
            Log.i(TAG, "Skipping web export — identical content already registered")
            return ExportResult(
                success = true,
                fileName = fileName,
                commandCount = commands.size,
                contentHash = hash,
                errorMessage = "Already exported (identical content)"
            )
        }

        val filePath = saveToLocal(fileName, content)
            ?: return ExportResult(success = false, errorMessage = "Failed to save file: $fileName")

        val latestDto = registry.getLatestVersion(domainName, "web")
        val nextVersion = (latestDto?.version ?: 0) + 1
        registry.deactivateOldVersions(domainName, "web", nextVersion)

        val urlPatternsJson = if (urlPatterns.isNotEmpty()) {
            JSONArray(urlPatterns).toString()
        } else null

        val dto = VosFileRegistryDTO(
            id = 0,
            fileId = domainName,
            fileType = "web",
            fileName = fileName,
            contentHash = hash,
            commandCount = commands.size,
            vosVersion = VOS_VERSION,
            domain = domainName,
            pageTitle = pageTitle,
            urlPatterns = urlPatternsJson,
            uploaderDeviceId = null,
            userAgent = null,
            scrapeDurationMs = scrapeDurationMs,
            scrapedAt = System.currentTimeMillis(),
            uploadedAt = null,
            downloadedAt = null,
            source = "local",
            localPath = filePath,
            isActive = true,
            version = nextVersion
        )

        registry.insert(dto)
        Log.i(TAG, "Exported $fileName: ${commands.size} commands for $domainName, version=$nextVersion")

        return ExportResult(
            success = true,
            filePath = filePath,
            fileName = fileName,
            commandCount = commands.size,
            contentHash = hash
        )
    }

    /**
     * Build VOS v2.1 JSON from a list of VoiceCommandEntity.
     * Reconstructs category_map, action_map, and meta_map from entity fields.
     */
    private fun buildVosJson(
        locale: String,
        domain: String,
        fallback: String,
        commands: List<VoiceCommandEntity>
    ): JSONObject {
        val root = JSONObject()
        root.put("version", VOS_VERSION)
        root.put("locale", locale)
        root.put("fallback", fallback)
        root.put("domain", domain)

        // Build category_map from commands
        val categoryMap = JSONObject()
        commands.forEach { cmd ->
            val prefix = cmd.id.substringBefore("_", "")
            if (prefix.isNotEmpty() && !categoryMap.has(prefix)) {
                categoryMap.put(prefix, cmd.category)
            }
        }
        root.put("category_map", categoryMap)

        // Build action_map from commands that have explicit actionType
        val actionMap = JSONObject()
        commands.forEach { cmd ->
            if (cmd.actionType.isNotEmpty()) {
                actionMap.put(cmd.id, cmd.actionType)
            }
        }
        root.put("action_map", actionMap)

        // Build meta_map from commands that have metadata
        val metaMap = JSONObject()
        commands.forEach { cmd ->
            if (cmd.metadata.isNotEmpty()) {
                try {
                    metaMap.put(cmd.id, JSONObject(cmd.metadata))
                } catch (_: Exception) {
                    // Skip malformed metadata
                }
            }
        }
        root.put("meta_map", metaMap)

        // Build commands array
        val commandsArray = JSONArray()
        commands.forEach { cmd ->
            val entry = JSONArray()
            entry.put(cmd.id)
            entry.put(cmd.primaryText)

            // Parse synonyms JSON string to array
            val synonyms = JSONArray()
            try {
                val synList = JSONArray(cmd.synonyms)
                for (i in 0 until synList.length()) {
                    synonyms.put(synList.getString(i))
                }
            } catch (_: Exception) {
                // No synonyms or invalid format
            }
            entry.put(synonyms)
            entry.put(cmd.description)
            commandsArray.put(entry)
        }
        root.put("commands", commandsArray)

        return root
    }

    /**
     * Save VOS content to Downloads/commands/ directory.
     * @return Absolute file path, or null if save failed.
     */
    private fun saveToLocal(fileName: String, content: String): String? {
        return try {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS
            )
            val commandsDir = File(downloadsDir, COMMANDS_DIR)
            if (!commandsDir.exists()) {
                commandsDir.mkdirs()
            }

            val file = File(commandsDir, fileName)
            file.writeText(content, Charsets.UTF_8)
            Log.d(TAG, "Saved VOS file: ${file.absolutePath}")
            file.absolutePath
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save VOS file: $fileName", e)
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
