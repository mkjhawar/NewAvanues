/**
 * UnifiedCommandParser.kt - Unified parser supporting both .vos (JSON) and .avu formats
 *
 * Purpose: Parse voice command files in either legacy JSON format or new AVU format
 * Migration: During transition period, supports both formats transparently
 * Schema: Detects format automatically from file content
 *
 * @author Augmentalis Engineering
 * @since 2026-01-15
 */

package com.augmentalis.commandmanager.loader

import android.content.Context
import android.util.Log
import com.augmentalis.voiceos.command.VOSCommand
import com.augmentalis.commandmanager.database.sqldelight.VoiceCommandEntity
import org.json.JSONArray
import org.json.JSONObject

/**
 * Unified parser for voice command files supporting both JSON and AVU formats.
 *
 * Usage:
 * ```kotlin
 * val parser = UnifiedCommandParser(context)
 *
 * // Auto-detect format and parse
 * parser.parseCommandFile("commands/en-US/navigation-commands.avu").onSuccess { result ->
 *     val entities = parser.convertToEntities(result)
 * }
 *
 * // Parse all files (both .vos and .avu)
 * parser.parseAllCommandFiles().onSuccess { results ->
 *     results.forEach { result ->
 *         val entities = parser.convertToEntities(result)
 *     }
 * }
 * ```
 */
class UnifiedCommandParser(private val context: Context) {

    companion object {
        private const val TAG = "UnifiedCommandParser"
        private const val COMMANDS_BASE_DIR = "commands"
        private val LOCALE_FOLDERS = listOf("en-US", "es-ES", "de-DE", "fr-FR")
        private val SUPPORTED_EXTENSIONS = listOf(".avu", ".vos")
    }

    /**
     * Parsed command file result (format-agnostic)
     */
    data class CommandFileResult(
        val format: Format,
        val locale: String,
        val category: String,
        val displayName: String,
        val description: String,
        val commands: List<VOSCommand>
    )

    enum class Format { JSON, AVU }

    /**
     * Parse a command file (auto-detects format).
     */
    fun parseCommandFile(assetPath: String): Result<CommandFileResult> {
        return try {
            Log.d(TAG, "Parsing command file: $assetPath")
            val content = readAssetFile(assetPath)
            val format = detectFormat(content)

            val result = when (format) {
                Format.JSON -> parseJsonFormat(content, assetPath)
                Format.AVU -> parseAvuFormat(content, assetPath)
            }

            Log.d(TAG, "Parsed $assetPath: ${result.commands.size} commands (${format.name} format)")
            Result.success(result)

        } catch (e: Exception) {
            Log.e(TAG, "Error parsing $assetPath: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Parse all command files from all locale directories.
     */
    fun parseAllCommandFiles(): Result<List<CommandFileResult>> {
        return try {
            val results = mutableListOf<CommandFileResult>()
            var success = 0
            var failed = 0

            LOCALE_FOLDERS.forEach { locale ->
                val localeDir = "$COMMANDS_BASE_DIR/$locale"

                try {
                    val files = context.assets.list(localeDir) ?: emptyArray()
                    files.filter { file ->
                        SUPPORTED_EXTENSIONS.any { file.endsWith(it) }
                    }.forEach { filename ->
                        val fullPath = "$localeDir/$filename"
                        parseCommandFile(fullPath).onSuccess { result ->
                            results.add(result)
                            success++
                        }.onFailure {
                            failed++
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Could not access $localeDir: ${e.message}")
                }
            }

            Log.i(TAG, "Parsed $success files successfully, $failed failed")
            Result.success(results)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Convert parsed result to database entities.
     */
    fun convertToEntities(result: CommandFileResult): List<VoiceCommandEntity> {
        val isFallback = result.locale.equals("en-US", ignoreCase = true)
        val timestamp = System.currentTimeMillis()

        return result.commands.map { command ->
            VoiceCommandEntity(
                uid = 0,
                id = command.action.uppercase(),
                locale = result.locale,
                primaryText = command.cmd,
                synonyms = command.syn.joinToString(","),
                description = "",
                category = result.category,
                priority = 50,
                isFallback = isFallback,
                createdAt = timestamp
            )
        }
    }

    // --- Private Methods ---

    private fun detectFormat(content: String): Format {
        val trimmed = content.trim()
        return when {
            trimmed.startsWith("{") -> Format.JSON
            trimmed.startsWith("#") || trimmed.contains("\n---\n") -> Format.AVU
            else -> Format.JSON // Default fallback
        }
    }

    private fun parseJsonFormat(content: String, path: String): CommandFileResult {
        val json = JSONObject(content)

        val locale = json.getString("locale")
        val fileInfo = json.getJSONObject("file_info")
        val category = fileInfo.getString("category")
        val displayName = fileInfo.getString("display_name")
        val description = fileInfo.getString("description")

        val commandsArray = json.getJSONArray("commands")
        val commands = parseJsonCommands(commandsArray)

        return CommandFileResult(
            format = Format.JSON,
            locale = locale,
            category = category,
            displayName = displayName,
            description = description,
            commands = commands
        )
    }

    private fun parseJsonCommands(array: JSONArray): List<VOSCommand> {
        val commands = mutableListOf<VOSCommand>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val action = obj.getString("action")
            val cmd = obj.getString("cmd")
            val synArray = obj.getJSONArray("syn")
            val synonyms = (0 until synArray.length()).map { synArray.getString(it) }

            commands.add(VOSCommand(action, cmd, synonyms))
        }
        return commands
    }

    private fun parseAvuFormat(content: String, path: String): CommandFileResult {
        val lines = content.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("#") }

        // Find section delimiters
        val delimiterIndices = lines.mapIndexedNotNull { index, line ->
            if (line == "---") index else null
        }

        require(delimiterIndices.size >= 2) { "Invalid AVU format: missing delimiters" }

        // Parse metadata
        val metadataLines = lines.subList(delimiterIndices[0] + 1, delimiterIndices[1])
        var locale = "en-US"
        var category = "unknown"
        var displayName = "Unknown"
        var description = ""

        for (line in metadataLines) {
            when {
                line.startsWith("locale:") -> locale = line.substringAfter(":").trim()
                line.startsWith("  category:") -> category = line.substringAfter(":").trim()
                line.startsWith("  display_name:") -> displayName = line.substringAfter(":").trim()
                line.startsWith("  description:") -> description = line.substringAfter(":").trim()
            }
        }

        // Parse commands
        val contentStart = delimiterIndices[1] + 1
        val contentEnd = if (delimiterIndices.size > 2) delimiterIndices[2] else lines.size
        val contentLines = lines.subList(contentStart, contentEnd)

        val commands = mutableListOf<VOSCommand>()
        val synonymsMap = mutableMapOf<String, List<String>>()

        // If there's a synonyms section, parse it first
        if (delimiterIndices.size > 2 && delimiterIndices[2] + 1 < lines.size) {
            val synLines = lines.subList(delimiterIndices[2] + 1, lines.size)
            for (line in synLines) {
                if (line.startsWith("SYN:")) {
                    val data = line.substringAfter("SYN:")
                    val bracketStart = data.indexOf('[')
                    val bracketEnd = data.indexOf(']')
                    if (bracketStart > 0 && bracketEnd > bracketStart) {
                        val action = data.substring(0, bracketStart).trimEnd(':')
                        val syns = data.substring(bracketStart + 1, bracketEnd)
                            .split(",")
                            .map { it.trim() }
                        synonymsMap[action] = syns
                    }
                }
            }
        }

        // Parse CMD entries
        for (line in contentLines) {
            if (line.startsWith("CMD:")) {
                val parts = line.substringAfter("CMD:").split(":", limit = 2)
                if (parts.size >= 2) {
                    val action = parts[0]
                    val primaryText = parts[1]
                    val synonyms = synonymsMap[action] ?: emptyList()
                    commands.add(VOSCommand(action, primaryText, synonyms))
                }
            } else if (line.startsWith("CAT:")) {
                val parts = line.substringAfter("CAT:").split(":", limit = 3)
                if (parts.isNotEmpty()) {
                    category = parts[0]
                    displayName = parts.getOrNull(1) ?: category
                    description = parts.getOrNull(2) ?: ""
                }
            }
        }

        return CommandFileResult(
            format = Format.AVU,
            locale = locale,
            category = category,
            displayName = displayName,
            description = description,
            commands = commands
        )
    }

    private fun readAssetFile(assetPath: String): String {
        return context.assets.open(assetPath).use { input ->
            input.bufferedReader().use { it.readText() }
        }
    }
}
