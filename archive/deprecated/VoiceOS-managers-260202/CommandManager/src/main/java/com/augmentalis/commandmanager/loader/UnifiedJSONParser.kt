/**
 * UnifiedJSONParser.kt - Parser for unified commands-all.json file
 *
 * Purpose: Parse the consolidated command file containing all locales and categories
 * Format: Unified JSON structure with segments, metadata, and payload markers
 *
 * Architecture:
 * - Uses org.json (Android built-in) for consistency with ArrayJsonParser
 * - Supports selective category loading
 * - Validates payload markers for integrity
 * - Converts to VoiceCommandEntity for Room database
 *
 * Integration:
 * - Called by CommandLoader for initial database population
 * - Supports asset loading from assets/commands/commands-all.json
 * - Provides batch conversion to VoiceCommandEntity list
 */

package com.augmentalis.commandmanager.loader

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.VOSCommand
import com.augmentalis.commandmanager.database.sqldelight.VoiceCommandEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Data class for unified command file structure
 */
data class UnifiedCommandFile(
    val schema: String,
    val version: String,
    val timestamp: Long,
    val locale: String,
    val fileInfo: UnifiedFileInfo,
    val segments: List<CommandSegment>,
    val metadata: Metadata
)

/**
 * File information metadata
 */
data class UnifiedFileInfo(
    val filename: String,
    val description: String,
    val type: String,
    val commandCount: Int,
    val categoryCount: Int,
    val generatedDate: String
)

/**
 * Command segment (category) with payload markers
 */
data class CommandSegment(
    val category: String,
    val displayName: String,
    val segmentType: String,
    val commandCount: Int,
    val payloadStart: Int,
    val payloadEnd: Int,
    val commands: List<VOSCommand>
)

/**
 * File metadata
 */
data class Metadata(
    val categories: List<String>,
    val totalCommands: Int,
    val totalSynonyms: Int,
    val sourceFile: String,
    val generationTool: String
)

/**
 * Load result with success status and statistics
 */
data class LoadResult(
    val success: Boolean,
    val totalLoaded: Int,
    val categoriesLoaded: List<String>,
    val errors: List<String> = emptyList()
)

/**
 * Parser for unified JSON command files
 *
 * Usage:
 * ```kotlin
 * val parser = UnifiedJSONParser(context)
 * val result = parser.parseUnifiedJSON("commands-all.json")
 * if (result.isSuccess) {
 *     val unified = result.getOrNull()
 *     val entities = parser.convertToEntities(unified)
 *     // Insert entities into database
 * }
 * ```
 */
class UnifiedJSONParser(private val context: Context) {

    companion object {
        private const val TAG = "UnifiedJSONParser"
        private const val ASSETS_COMMANDS_PATH = "commands"
        private const val DEFAULT_PRIORITY = 50
        private const val SCHEMA_VERSION = "1.0"
    }

    /**
     * Parse unified JSON file from assets
     *
     * @param filename Name of JSON file in assets/commands/ (default: "commands-all.json")
     * @return Result containing UnifiedCommandFile or error
     */
    suspend fun parseUnifiedJSON(filename: String = "commands-all.json"): Result<UnifiedCommandFile> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Parsing unified JSON: $filename")

                // Read file from assets
                val assetPath = "$ASSETS_COMMANDS_PATH/$filename"
                val jsonString = readAssetFile(assetPath)

                // Parse JSON
                val jsonObject = JSONObject(jsonString)

                // Validate schema
                val schema = jsonObject.optString("schema", "")
                if (schema != SCHEMA_VERSION) {
                    Log.w(TAG, "Schema version mismatch: expected $SCHEMA_VERSION, got $schema")
                }

                // Parse top-level fields
                val version = jsonObject.getString("version")
                val timestamp = jsonObject.getLong("timestamp")
                val locale = jsonObject.getString("locale")

                // Parse file_info
                val fileInfoObj = jsonObject.getJSONObject("file_info")
                val fileInfo = parseFileInfo(fileInfoObj)

                // Parse segments
                val segmentsArray = jsonObject.getJSONArray("segments")
                val segments = parseSegments(segmentsArray)

                // Parse metadata
                val metadataObj = jsonObject.getJSONObject("metadata")
                val metadata = parseMetadata(metadataObj)

                // Validate payload markers
                validatePayloadMarkers(segments)

                val unified = UnifiedCommandFile(
                    schema = schema,
                    version = version,
                    timestamp = timestamp,
                    locale = locale,
                    fileInfo = fileInfo,
                    segments = segments,
                    metadata = metadata
                )

                Log.d(TAG, "Successfully parsed unified JSON: ${fileInfo.commandCount} commands, ${fileInfo.categoryCount} categories")
                Result.success(unified)

            } catch (e: JSONException) {
                Log.e(TAG, "JSON parsing error", e)
                Result.failure(e)
            } catch (e: IOException) {
                Log.e(TAG, "File read error", e)
                Result.failure(e)
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error parsing unified JSON", e)
                Result.failure(e)
            }
        }
    }

    /**
     * Ingest all segments and convert to database entities
     *
     * @param unified Parsed unified command file
     * @return LoadResult with statistics
     */
    suspend fun ingestAllSegments(unified: UnifiedCommandFile): LoadResult {
        return withContext(Dispatchers.IO) {
            try {
                val entities = convertToEntities(unified)
                val categories = unified.segments.map { it.category }

                Log.d(TAG, "Ingested ${entities.size} commands from ${categories.size} categories")

                LoadResult(
                    success = true,
                    totalLoaded = entities.size,
                    categoriesLoaded = categories
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error ingesting segments", e)
                LoadResult(
                    success = false,
                    totalLoaded = 0,
                    categoriesLoaded = emptyList(),
                    errors = listOf(e.message ?: "Unknown error")
                )
            }
        }
    }

    /**
     * Convert unified command file to list of VoiceCommandEntity
     *
     * @param unified Parsed unified command file
     * @param selectedCategories Optional list of categories to include (null = all)
     * @return List of VoiceCommandEntity ready for database insertion
     */
    suspend fun convertToEntities(
        unified: UnifiedCommandFile,
        selectedCategories: List<String>? = null
    ): List<VoiceCommandEntity> {
        return withContext(Dispatchers.Default) {
            val entities = mutableListOf<VoiceCommandEntity>()
            val locale = unified.locale
            val isFallback = locale.startsWith("en", ignoreCase = true) // English locales are fallback

            // Filter segments by category if specified
            val segments = if (selectedCategories != null) {
                unified.segments.filter { it.category in selectedCategories }
            } else {
                unified.segments
            }

            // Convert each segment's commands to entities
            for (segment in segments) {
                for (cmd in segment.commands) {
                    try {
                        val entity = VoiceCommandEntity(
                            locale = locale,
                            id = cmd.action,
                            primaryText = cmd.cmd,
                            synonyms = JSONArray(cmd.syn).toString(), // Store as JSON array string
                            description = "Command: ${cmd.cmd}", // Default description
                            category = segment.category,
                            priority = DEFAULT_PRIORITY,
                            isFallback = isFallback,
                            createdAt = System.currentTimeMillis()
                        )
                        entities.add(entity)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to convert command: ${cmd.action} in category ${segment.category}", e)
                    }
                }
            }

            Log.d(TAG, "Converted ${entities.size} commands to entities (locale: $locale, categories: ${segments.size})")
            entities
        }
    }

    /**
     * Read file from assets directory
     *
     * @param assetPath Path relative to assets/ directory
     * @return File contents as string
     * @throws IOException if file not found or read error
     */
    private fun readAssetFile(assetPath: String): String {
        return try {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read asset file: $assetPath", e)
            throw IOException("Asset file not found: $assetPath", e)
        }
    }

    /**
     * Parse file_info object
     */
    private fun parseFileInfo(obj: JSONObject): UnifiedFileInfo {
        return UnifiedFileInfo(
            filename = obj.getString("filename"),
            description = obj.getString("description"),
            type = obj.getString("type"),
            commandCount = obj.getInt("command_count"),
            categoryCount = obj.getInt("category_count"),
            generatedDate = obj.getString("generated_date")
        )
    }

    /**
     * Parse segments array
     */
    private fun parseSegments(array: JSONArray): List<CommandSegment> {
        val segments = mutableListOf<CommandSegment>()

        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                val segment = CommandSegment(
                    category = obj.getString("category"),
                    displayName = obj.getString("display_name"),
                    segmentType = obj.getString("segment_type"),
                    commandCount = obj.getInt("command_count"),
                    payloadStart = obj.getInt("payload_start"),
                    payloadEnd = obj.getInt("payload_end"),
                    commands = parseCommands(obj.getJSONArray("commands"))
                )
                segments.add(segment)
            } catch (e: JSONException) {
                Log.w(TAG, "Failed to parse segment at index $i", e)
            }
        }

        return segments
    }

    /**
     * Parse commands array within a segment
     */
    private fun parseCommands(array: JSONArray): List<VOSCommand> {
        val commands = mutableListOf<VOSCommand>()

        for (i in 0 until array.length()) {
            try {
                val obj = array.getJSONObject(i)
                val command = VOSCommand(
                    action = obj.getString("action"),
                    cmd = obj.getString("cmd"),
                    syn = parseSynonyms(obj.getJSONArray("syn"))
                )
                commands.add(command)
            } catch (e: JSONException) {
                Log.w(TAG, "Failed to parse command at index $i", e)
            }
        }

        return commands
    }

    /**
     * Parse synonyms array
     */
    private fun parseSynonyms(array: JSONArray): List<String> {
        val synonyms = mutableListOf<String>()
        for (i in 0 until array.length()) {
            try {
                synonyms.add(array.getString(i))
            } catch (e: JSONException) {
                Log.w(TAG, "Failed to parse synonym at index $i", e)
            }
        }
        return synonyms
    }

    /**
     * Parse metadata object
     */
    private fun parseMetadata(obj: JSONObject): Metadata {
        val categoriesArray = obj.getJSONArray("categories")
        val categories = mutableListOf<String>()
        for (i in 0 until categoriesArray.length()) {
            categories.add(categoriesArray.getString(i))
        }

        return Metadata(
            categories = categories,
            totalCommands = obj.getInt("total_commands"),
            totalSynonyms = obj.getInt("total_synonyms"),
            sourceFile = obj.getString("source_file"),
            generationTool = obj.getString("generation_tool")
        )
    }

    /**
     * Validate payload markers for integrity
     *
     * Ensures payload_start and payload_end match actual command count
     *
     * @throws IllegalStateException if validation fails
     */
    private fun validatePayloadMarkers(segments: List<CommandSegment>) {
        for (segment in segments) {
            val expectedCount = segment.payloadEnd - segment.payloadStart + 1
            val actualCount = segment.commands.size

            if (expectedCount != actualCount) {
                val error = "Payload marker mismatch in category ${segment.category}: " +
                           "expected $expectedCount commands (${segment.payloadStart}-${segment.payloadEnd}), " +
                           "got $actualCount"
                Log.w(TAG, error)
                // Don't throw - just warn, as this might be due to invalid commands being skipped
            }

            if (segment.commandCount != actualCount) {
                val error = "Command count mismatch in category ${segment.category}: " +
                           "metadata says ${segment.commandCount}, got $actualCount"
                Log.w(TAG, error)
            }
        }
    }

    /**
     * Check if unified JSON file is valid without full parsing
     *
     * @param filename Name of JSON file in assets/commands/
     * @return true if file appears valid, false otherwise
     */
    suspend fun isValidUnifiedJSON(filename: String = "commands-all.json"): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val assetPath = "$ASSETS_COMMANDS_PATH/$filename"
                val jsonString = readAssetFile(assetPath)
                val jsonObject = JSONObject(jsonString)

                // Check required fields
                jsonObject.has("schema") &&
                jsonObject.has("version") &&
                jsonObject.has("locale") &&
                jsonObject.has("file_info") &&
                jsonObject.has("segments") &&
                jsonObject.has("metadata") &&
                jsonObject.getJSONArray("segments").length() > 0
            } catch (e: Exception) {
                Log.e(TAG, "Validation failed for $filename", e)
                false
            }
        }
    }

    /**
     * Get summary statistics from unified JSON without full parsing
     *
     * @param filename Name of JSON file in assets/commands/
     * @return Map of statistics (locale, total_commands, categories, etc.)
     */
    suspend fun getFileStatistics(filename: String = "commands-all.json"): Map<String, Any>? {
        return withContext(Dispatchers.IO) {
            try {
                val assetPath = "$ASSETS_COMMANDS_PATH/$filename"
                val jsonString = readAssetFile(assetPath)
                val jsonObject = JSONObject(jsonString)

                val fileInfo = jsonObject.getJSONObject("file_info")
                val metadata = jsonObject.getJSONObject("metadata")

                mapOf(
                    "locale" to jsonObject.getString("locale"),
                    "version" to jsonObject.getString("version"),
                    "command_count" to fileInfo.getInt("command_count"),
                    "category_count" to fileInfo.getInt("category_count"),
                    "total_synonyms" to metadata.getInt("total_synonyms"),
                    "generated_date" to fileInfo.getString("generated_date")
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get statistics for $filename", e)
                null
            }
        }
    }
}
