/**
 * VOSFileParser.kt - Parser for .vos (VoiceOS) command files
 *
 * Purpose: Parse individual .vos JSON files from assets directory and convert to database entities
 * Format: VOS files contain localized voice commands with action IDs, primary text, and synonyms
 * Schema: v1.0 - Initial implementation with schema validation
 *
 * @author VOS4 Agent
 * @since 2025-10-13
 */

package com.augmentalis.voiceoscore.commandmanager.loader

import android.content.Context
import android.util.Log
import com.augmentalis.voiceoscore.VOSCommand
import com.augmentalis.voiceoscore.commandmanager.database.sqldelight.VoiceCommandEntity
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

/**
 * Data class representing a .vos file structure
 *
 * Schema:
 * - schema: Schema version identifier (e.g., "vos-commands-v1")
 * - version: File format version (e.g., "1.0")
 * - fileInfo: Metadata about the file and its contents
 * - locale: Locale code (e.g., "en-US", "es-ES", "fr-FR")
 * - commands: List of voice commands with actions and synonyms
 */
data class VOSFile(
    val schema: String,
    val version: String,
    val fileInfo: FileInfo,
    val locale: String,
    val commands: List<VOSCommand>
)

/**
 * File metadata information
 *
 * Fields:
 * - filename: Original filename (e.g., "navigation_commands.vos")
 * - category: Command category (e.g., "navigation", "system", "input")
 * - displayName: Human-readable category name (e.g., "Navigation Commands")
 * - description: Brief description of commands in this file
 * - commandCount: Total number of commands in file
 */
data class FileInfo(
    val filename: String,
    val category: String,
    val displayName: String,
    val description: String,
    val commandCount: Int
)

/**
 * Individual voice command definition
 *
 * Fields:
 * - action: Unique action ID (e.g., "NAVIGATE_FORWARD", "VOLUME_UP")
 * - cmd: Primary command text (e.g., "forward", "increase volume")
 * - syn: List of synonyms (e.g., ["next", "advance", "go forward"])
 */

/**
 * Parser for .vos (VoiceOS) command files
 *
 * Usage:
 * ```kotlin
 * val parser = VOSFileParser(context)
 *
 * // Parse single file
 * parser.parseVOSFile("commands/vos/navigation_en_us.vos").onSuccess { vosFile ->
 *     val entities = parser.convertToEntities(vosFile)
 *     // Insert entities into database
 * }
 *
 * // Parse all files
 * parser.parseAllVOSFiles().onSuccess { vosFiles ->
 *     vosFiles.forEach { vosFile ->
 *         val entities = parser.convertToEntities(vosFile)
 *         // Insert entities into database
 *     }
 * }
 * ```
 *
 * Error Handling:
 * - Returns Result.failure() for file not found, invalid JSON, or missing fields
 * - Logs errors with tag "VOSFileParser" for debugging
 * - Validates schema version against expected format
 */
class VOSFileParser(private val context: Context) {

    companion object {
        private const val TAG = "VOSFileParser"
        private const val COMMANDS_BASE_DIR = "commands"
        private const val EXPECTED_SCHEMA = "vos-commands-v1"
        private const val SUPPORTED_VERSION = "1.0"

        // Supported locale folders (language-REGION format)
        private val LOCALE_FOLDERS = listOf("en-US", "es-ES", "de-DE", "fr-FR")
    }

    /**
     * Parse a single .vos file from assets directory
     *
     * @param assetPath Path to .vos file in assets (e.g., "commands/vos/navigation_en_us.vos")
     * @return Result<VOSFile> - Success with parsed file or Failure with exception
     *
     * Error Conditions:
     * - IOException: File not found or cannot be read
     * - JSONException: Invalid JSON format
     * - IllegalStateException: Missing required fields or invalid schema
     */
    fun parseVOSFile(assetPath: String): Result<VOSFile> {
        return try {
            Log.d(TAG, "Parsing VOS file: $assetPath")

            // Read file contents from assets
            val jsonContent = readAssetFile(assetPath)

            // Parse JSON to VOSFile data class
            val jsonObject = JSONObject(jsonContent)
            val vosFile = parseVOSFileFromJSON(jsonObject)

            // Validate schema and version
            validateVOSFile(vosFile, assetPath)

            // Validate command count matches actual commands
            if (vosFile.fileInfo.commandCount != vosFile.commands.size) {
                Log.w(TAG, "Command count mismatch in $assetPath: " +
                        "declared=${vosFile.fileInfo.commandCount}, actual=${vosFile.commands.size}")
            }

            Log.d(TAG, "Successfully parsed $assetPath: " +
                    "${vosFile.commands.size} commands, locale=${vosFile.locale}")

            Result.success(vosFile)

        } catch (e: IOException) {
            Log.e(TAG, "File not found or cannot be read: $assetPath", e)
            Result.failure(e)
        } catch (e: JSONException) {
            Log.e(TAG, "Invalid JSON format in $assetPath", e)
            Result.failure(e)
        } catch (e: IllegalStateException) {
            Log.e(TAG, "Validation error in $assetPath: ${e.message}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error parsing $assetPath", e)
            Result.failure(e)
        }
    }

    /**
     * Parse all .vos files from all locale-specific directories
     *
     * @return Result<List<VOSFile>> - Success with list of parsed files or Failure with exception
     *
     * Note: Skips files that fail to parse and logs errors, but continues with remaining files
     * Scans all locale folders: en-US, es-ES, de-DE, fr-FR
     */
    fun parseAllVOSFiles(): Result<List<VOSFile>> {
        return try {
            Log.d(TAG, "Parsing all VOS files from locale-specific directories")

            val parsedFiles = mutableListOf<VOSFile>()
            var successCount = 0
            var failureCount = 0

            // Scan each locale folder
            LOCALE_FOLDERS.forEach { locale ->
                val localeDir = "$COMMANDS_BASE_DIR/$locale"
                Log.d(TAG, "Scanning locale directory: $localeDir")

                try {
                    val vosFiles = context.assets.list(localeDir) ?: emptyArray()
                    Log.d(TAG, "Found ${vosFiles.size} files in $localeDir")

                    // Parse each .vos file in this locale
                    vosFiles.filter { it.endsWith(".vos") }.forEach { filename ->
                        val fullPath = "$localeDir/$filename"
                        parseVOSFile(fullPath).onSuccess { vosFile ->
                            parsedFiles.add(vosFile)
                            successCount++
                        }.onFailure { error ->
                            Log.e(TAG, "Failed to parse $fullPath: ${error.message}")
                            failureCount++
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not access locale directory $localeDir: ${e.message}")
                    // Continue with next locale
                }
            }

            Log.i(TAG, "Parsing complete: $successCount succeeded, $failureCount failed across ${LOCALE_FOLDERS.size} locales")

            if (parsedFiles.isEmpty()) {
                Result.failure(IllegalStateException("No valid .vos files found in any locale directory"))
            } else {
                Result.success(parsedFiles)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing VOS files from assets", e)
            Result.failure(e)
        }
    }

    /**
     * Convert VOSFile to list of VoiceCommandEntity objects for Room database
     *
     * @param vosFile Parsed .vos file
     * @return List<VoiceCommandEntity> - Database entities ready for insertion
     *
     * Conversion Rules:
     * - locale: Taken from vosFile.locale
     * - id: action (uppercase, e.g., "NAVIGATE_FORWARD")
     * - primaryText: cmd (e.g., "forward")
     * - synonyms: syn joined with comma (e.g., "next,advance,go forward")
     * - description: Empty string (not provided in VOS format)
     * - category: Taken from fileInfo.category
     * - priority: 0 (default for static commands)
     * - isFallback: true if locale is "en-US" (English fallback)
     * - createdAt: Current timestamp
     */
    fun convertToEntities(vosFile: VOSFile): List<VoiceCommandEntity> {
        val isFallback = vosFile.locale.equals("en-US", ignoreCase = true)
        val timestamp = System.currentTimeMillis()

        return vosFile.commands.map { command ->
            VoiceCommandEntity(
                uid = 0, // Auto-generated by Room
                id = command.action.uppercase(), // Normalize to uppercase
                locale = vosFile.locale,
                primaryText = command.cmd,
                synonyms = command.syn.joinToString(","), // Join synonyms with comma
                description = "", // Not provided in VOS format
                category = vosFile.fileInfo.category,
                priority = 50, // Default medium priority
                isFallback = isFallback,
                createdAt = timestamp
            )
        }
    }

    /**
     * Read file contents from assets directory
     *
     * @param assetPath Path to file in assets
     * @return String - File contents
     * @throws IOException if file cannot be read
     */
    private fun readAssetFile(assetPath: String): String {
        return try {
            context.assets.open(assetPath).use { inputStream ->
                inputStream.bufferedReader().use { reader ->
                    reader.readText()
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Failed to read asset file: $assetPath", e)
            throw e
        }
    }

    /**
     * Validate VOSFile structure and schema version
     *
     * @param vosFile Parsed VOS file
     * @param assetPath File path (for error messages)
     * @throws IllegalStateException if validation fails
     */
    private fun validateVOSFile(vosFile: VOSFile, assetPath: String) {
        // Validate schema
        if (vosFile.schema != EXPECTED_SCHEMA) {
            val message = "Invalid schema in $assetPath: " +
                    "expected='$EXPECTED_SCHEMA', found='${vosFile.schema}'"
            Log.w(TAG, message)
            // Warning only - don't fail for schema mismatch (future compatibility)
        }

        // Validate version (allow newer versions, fail on incompatible)
        if (vosFile.version < SUPPORTED_VERSION) {
            throw IllegalStateException("Unsupported version in $assetPath: " +
                    "minimum='$SUPPORTED_VERSION', found='${vosFile.version}'")
        }

        // Validate locale format (basic check)
        if (!vosFile.locale.matches(Regex("[a-z]{2}-[A-Z]{2}"))) {
            Log.w(TAG, "Non-standard locale format in $assetPath: '${vosFile.locale}' " +
                    "(expected format: xx-XX, e.g., en-US)")
        }

        // Validate commands not empty (allow empty if explicitly marked with command_count = 0)
        if (vosFile.commands.isEmpty()) {
            if (vosFile.fileInfo.commandCount == 0) {
                // Empty placeholder file - allowed but log warning
                Log.w(TAG, "Empty VOS file (placeholder): $assetPath - " +
                        "description: ${vosFile.fileInfo.description}")
            } else {
                // Command count mismatch - should have commands but doesn't
                throw IllegalStateException("No commands found in $assetPath " +
                        "(expected ${vosFile.fileInfo.commandCount} commands)")
            }
        }

        // Validate each command has required fields
        vosFile.commands.forEachIndexed { index, command ->
            if (command.action.isBlank()) {
                throw IllegalStateException("Empty action ID at command index $index in $assetPath")
            }
            if (command.cmd.isBlank()) {
                throw IllegalStateException("Empty primary text at command index $index in $assetPath")
            }
        }
    }

    /**
     * Parse JSONObject to VOSFile data class
     *
     * @param jsonObject Root JSON object
     * @return VOSFile parsed from JSON
     * @throws JSONException if required fields are missing
     */
    private fun parseVOSFileFromJSON(jsonObject: JSONObject): VOSFile {
        val schema = jsonObject.getString("schema")
        val version = jsonObject.getString("version")
        val locale = jsonObject.getString("locale")

        val fileInfoObj = jsonObject.getJSONObject("file_info")
        val fileInfo = parseFileInfo(fileInfoObj)

        val commandsArray = jsonObject.getJSONArray("commands")
        val commands = parseCommands(commandsArray)

        return VOSFile(
            schema = schema,
            version = version,
            fileInfo = fileInfo,
            locale = locale,
            commands = commands
        )
    }

    /**
     * Parse FileInfo from JSON object
     *
     * @param jsonObject file_info JSON object
     * @return FileInfo data class
     * @throws JSONException if required fields are missing
     */
    private fun parseFileInfo(jsonObject: JSONObject): FileInfo {
        return FileInfo(
            filename = jsonObject.getString("filename"),
            category = jsonObject.getString("category"),
            displayName = jsonObject.getString("display_name"),
            description = jsonObject.getString("description"),
            commandCount = jsonObject.getInt("command_count")
        )
    }

    /**
     * Parse commands array from JSON
     *
     * @param jsonArray commands JSON array
     * @return List of VOSCommand objects
     * @throws JSONException if required fields are missing
     */
    private fun parseCommands(jsonArray: JSONArray): List<VOSCommand> {
        val commands = mutableListOf<VOSCommand>()

        for (i in 0 until jsonArray.length()) {
            val commandObj = jsonArray.getJSONObject(i)

            val action = commandObj.getString("action")
            val cmd = commandObj.getString("cmd")
            val synArray = commandObj.getJSONArray("syn")

            // Parse synonyms array
            val synonyms = mutableListOf<String>()
            for (j in 0 until synArray.length()) {
                synonyms.add(synArray.getString(j))
            }

            commands.add(
                VOSCommand(
                    action = action,
                    cmd = cmd,
                    syn = synonyms
                )
            )
        }

        return commands
    }
}
