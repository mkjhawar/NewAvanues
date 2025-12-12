/**
 * AVUFileParser.kt - Parser for AVU format .vos files
 *
 * Parses the compact AVU (Avanues Universal) format exported by LearnApp.
 * AVU format uses IPC-style codes instead of verbose JSON.
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-11
 * Related: VoiceOS-AVU-UNIVERSAL-FORMAT-SPEC-50312-V1.md
 *         VoiceOS-LearnApp-DualEdition-Spec-51211-V1.md
 *
 * ## AVU Format Structure:
 * ```
 * # Avanues Universal Format v1.0
 * # Type: VOS
 * ---
 * schema: avu-1.0
 * version: 1.0.0
 * locale: en-US
 * metadata:
 *   file: com.app.package.vos
 *   category: learned_app
 *   count: 87
 * ---
 * APP:package:name:timestamp
 * STA:screens:elements:commands:avg_depth:max_depth:coverage
 * SCR:hash:activity:timestamp:element_count
 * ELM:uuid:label:type:actions:bounds:category
 * NAV:from_hash:to_hash:trigger_uuid:trigger_label:timestamp
 * CMD:uuid:trigger:action:element_uuid:confidence
 * ---
 * synonyms:
 *   word: [syn1, syn2]
 * ```
 *
 * @since 2.0.0 (LearnApp Dual-Edition)
 */

package com.augmentalis.commandmanager.loader

import android.content.Context
import android.util.Log
import com.augmentalis.commandmanager.database.sqldelight.VoiceCommandEntity
import java.io.File
import java.io.FileInputStream
import java.io.IOException

/**
 * Parsed AVU file data
 */
data class ParsedAVUFile(
    val schema: String = "",
    val version: String = "",
    val locale: String = "",
    val packageName: String = "",
    val appName: String = "",
    val category: String = "learned_app",
    val commands: List<AVUCommand> = emptyList(),
    val synonyms: Map<String, List<String>> = emptyMap(),
    val stats: AVUStats? = null
)

/**
 * AVU command from CMD line
 */
data class AVUCommand(
    val uuid: String,
    val trigger: String,
    val action: String,
    val elementUuid: String,
    val confidence: Float
)

/**
 * AVU statistics from STA line
 */
data class AVUStats(
    val screens: Int,
    val elements: Int,
    val commands: Int,
    val avgDepth: Float,
    val maxDepth: Int,
    val coverage: Float
)

/**
 * AVU File Parser
 *
 * Parses .vos files in the compact AVU format exported by LearnApp.
 *
 * Usage:
 * ```kotlin
 * val parser = AVUFileParser(context)
 *
 * // Parse single file
 * parser.parseFile("/path/to/app.vos").onSuccess { avuFile ->
 *     val entities = parser.convertToEntities(avuFile)
 *     // Insert into database
 * }
 *
 * // Watch folder for new files
 * parser.parseFolder("/path/to/learned_apps").onSuccess { files ->
 *     files.forEach { avuFile ->
 *         val entities = parser.convertToEntities(avuFile)
 *     }
 * }
 * ```
 */
class AVUFileParser(private val context: Context) {

    companion object {
        private const val TAG = "AVUFileParser"
        private const val EXPECTED_SCHEMA = "avu-1.0"

        // Default folder for learned app exports
        fun getLearnedAppsFolder(context: Context): File {
            return File(context.getExternalFilesDir(null), "learned_apps")
        }
    }

    /**
     * Parse a single AVU .vos file
     *
     * @param filePath Path to .vos file
     * @return Result<ParsedAVUFile>
     */
    fun parseFile(filePath: String): Result<ParsedAVUFile> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(IOException("File not found: $filePath"))
            }

            val content = file.readText()
            parseContent(content, filePath)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AVU file: $filePath", e)
            Result.failure(e)
        }
    }

    /**
     * Parse AVU content string
     */
    fun parseContent(content: String, sourceName: String = "unknown"): Result<ParsedAVUFile> {
        return try {
            val lines = content.lines()
            val parsed = ParsedAVUFile()

            var section = "header"
            val metadata = mutableMapOf<String, String>()
            val commands = mutableListOf<AVUCommand>()
            val synonyms = mutableMapOf<String, List<String>>()
            var packageName = ""
            var appName = ""
            var stats: AVUStats? = null

            for (line in lines) {
                val trimmed = line.trim()

                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                // Section delimiter
                if (trimmed == "---") {
                    section = when (section) {
                        "header" -> "schema"
                        "schema" -> "data"
                        "data" -> "synonyms"
                        else -> section
                    }
                    continue
                }

                when (section) {
                    "schema" -> {
                        // Parse YAML-like schema lines
                        if (trimmed.contains(":") && !trimmed.startsWith(" ")) {
                            val parts = trimmed.split(":", limit = 2)
                            if (parts.size == 2) {
                                metadata[parts[0].trim()] = parts[1].trim()
                            }
                        } else if (trimmed.startsWith(" ") && trimmed.contains(":")) {
                            // Nested metadata
                            val parts = trimmed.split(":", limit = 2)
                            if (parts.size == 2) {
                                metadata[parts[0].trim()] = parts[1].trim()
                            }
                        }
                    }
                    "data" -> {
                        // Parse IPC data lines
                        when {
                            trimmed.startsWith("APP:") -> {
                                val parts = trimmed.split(":")
                                if (parts.size >= 3) {
                                    packageName = parts[1]
                                    appName = parts[2]
                                }
                            }
                            trimmed.startsWith("STA:") -> {
                                stats = parseStaLine(trimmed)
                            }
                            trimmed.startsWith("CMD:") -> {
                                parseCommandLine(trimmed)?.let { commands.add(it) }
                            }
                            // Other IPC lines (SCR, ELM, NAV, etc.) are captured but not converted to commands
                        }
                    }
                    "synonyms" -> {
                        // Parse YAML synonym format: word: [syn1, syn2]
                        if (trimmed.contains(":") && trimmed.contains("[")) {
                            val word = trimmed.substringBefore(":").trim()
                            val synsStr = trimmed.substringAfter("[").substringBefore("]")
                            val syns = synsStr.split(",").map { it.trim() }
                            synonyms[word] = syns
                        }
                    }
                }
            }

            val result = ParsedAVUFile(
                schema = metadata["schema"] ?: "",
                version = metadata["version"] ?: "",
                locale = metadata["locale"] ?: "en-US",
                packageName = packageName,
                appName = appName,
                category = metadata["category"] ?: "learned_app",
                commands = commands,
                synonyms = synonyms,
                stats = stats
            )

            Log.i(TAG, "Parsed AVU file $sourceName: ${commands.size} commands, ${synonyms.size} synonyms")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse AVU content from $sourceName", e)
            Result.failure(e)
        }
    }

    /**
     * Parse STA line
     */
    private fun parseStaLine(line: String): AVUStats? {
        val parts = line.split(":")
        if (parts.size < 7) return null

        return try {
            AVUStats(
                screens = parts[1].toIntOrNull() ?: 0,
                elements = parts[2].toIntOrNull() ?: 0,
                commands = parts[3].toIntOrNull() ?: 0,
                avgDepth = parts[4].toFloatOrNull() ?: 0f,
                maxDepth = parts[5].toIntOrNull() ?: 0,
                coverage = parts[6].toFloatOrNull() ?: 0f
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse STA line: $line", e)
            null
        }
    }

    /**
     * Parse CMD line
     *
     * Format: CMD:uuid:trigger:action:element_uuid:confidence
     */
    private fun parseCommandLine(line: String): AVUCommand? {
        val parts = line.split(":")
        if (parts.size < 6) return null

        return try {
            AVUCommand(
                uuid = parts[1],
                trigger = parts[2],
                action = parts[3],
                elementUuid = parts[4],
                confidence = parts[5].toFloatOrNull() ?: 0.5f
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse CMD line: $line", e)
            null
        }
    }

    /**
     * Parse all .vos files from a folder
     *
     * @param folderPath Path to folder containing .vos files
     * @return Result<List<ParsedAVUFile>>
     */
    fun parseFolder(folderPath: String): Result<List<ParsedAVUFile>> {
        return try {
            val folder = File(folderPath)
            if (!folder.exists() || !folder.isDirectory) {
                return Result.failure(IOException("Invalid folder: $folderPath"))
            }

            val vosFiles = folder.listFiles { file ->
                file.isFile && file.extension == "vos"
            } ?: emptyArray()

            Log.i(TAG, "Found ${vosFiles.size} .vos files in $folderPath")

            val parsedFiles = mutableListOf<ParsedAVUFile>()
            var successCount = 0
            var failureCount = 0

            vosFiles.forEach { file ->
                parseFile(file.absolutePath).onSuccess { parsed ->
                    parsedFiles.add(parsed)
                    successCount++
                }.onFailure { error ->
                    Log.w(TAG, "Failed to parse ${file.name}: ${error.message}")
                    failureCount++
                }
            }

            Log.i(TAG, "Parsed $successCount/$vosFiles.size files successfully")

            Result.success(parsedFiles)

        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse folder: $folderPath", e)
            Result.failure(e)
        }
    }

    /**
     * Parse all .vos files from the default learned apps folder
     */
    fun parseLearnedAppsFolder(): Result<List<ParsedAVUFile>> {
        val folder = getLearnedAppsFolder(context)
        if (!folder.exists()) {
            folder.mkdirs()
            Log.i(TAG, "Created learned apps folder: ${folder.absolutePath}")
        }
        return parseFolder(folder.absolutePath)
    }

    /**
     * Convert parsed AVU file to database entities
     *
     * @param avuFile Parsed AVU file
     * @return List of VoiceCommandEntity for database insertion
     */
    fun convertToEntities(avuFile: ParsedAVUFile): List<VoiceCommandEntity> {
        val timestamp = System.currentTimeMillis()
        val entities = mutableListOf<VoiceCommandEntity>()

        // Convert commands
        avuFile.commands.forEach { cmd ->
            // Create primary command
            val entity = VoiceCommandEntity(
                uid = 0, // Auto-generated
                id = "LEARNED_${cmd.uuid.uppercase()}",
                locale = avuFile.locale,
                primaryText = cmd.trigger,
                synonyms = generateSynonyms(cmd.trigger, avuFile.synonyms),
                description = "Learned command for ${avuFile.appName}",
                category = avuFile.category,
                priority = calculatePriority(cmd.confidence),
                isFallback = false,
                createdAt = timestamp
            )
            entities.add(entity)
        }

        Log.d(TAG, "Converted ${entities.size} entities from ${avuFile.packageName}")
        return entities
    }

    /**
     * Generate synonyms string from command trigger and synonym map
     */
    private fun generateSynonyms(trigger: String, synonymMap: Map<String, List<String>>): String {
        val words = trigger.lowercase().split(" ")
        val allSynonyms = mutableSetOf<String>()

        words.forEach { word ->
            synonymMap[word]?.let { synonyms ->
                synonyms.forEach { syn ->
                    val altTrigger = trigger.lowercase().replace(word, syn)
                    if (altTrigger != trigger.lowercase()) {
                        allSynonyms.add(altTrigger)
                    }
                }
            }
        }

        return allSynonyms.joinToString(",")
    }

    /**
     * Calculate priority from confidence
     *
     * Confidence 0.0-1.0 maps to priority 0-100
     */
    private fun calculatePriority(confidence: Float): Int {
        return (confidence * 100).toInt().coerceIn(0, 100)
    }

    /**
     * Get list of .vos files in a folder without parsing
     */
    fun listVosFiles(folderPath: String): List<File> {
        val folder = File(folderPath)
        if (!folder.exists() || !folder.isDirectory) {
            return emptyList()
        }

        return folder.listFiles { file ->
            file.isFile && file.extension == "vos"
        }?.toList() ?: emptyList()
    }

    /**
     * Get list of .vos files in the default learned apps folder
     */
    fun listLearnedVosFiles(): List<File> {
        return listVosFiles(getLearnedAppsFolder(context).absolutePath)
    }
}
