/**
 * RenameCommandHandler.kt - Handles voice-activated command renaming
 * Path: apps/VoiceOSCore/src/main/java/com/augmentalis/voiceoscore/learnapp/commands/RenameCommandHandler.kt
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: Claude Code (IDEACODE v10.3)
 * Created: 2025-12-08
 *
 * Implements "Rename [old name] to [new name]" voice command processing.
 * Adds new names as synonyms to existing commands, preserving original labels.
 *
 * ## Features
 * - Parses multiple rename command formats
 * - Fuzzy matching for command names (handles with/without action prefix)
 * - Adds synonyms without replacing original command text
 * - Updates database immediately
 * - Provides TTS voice feedback
 *
 * ## Supported Formats
 * - "Rename Button 1 to Save"
 * - "Rename Button 1 as Save"
 * - "Change Button 1 to Save"
 *
 * ## Usage Example
 * ```kotlin
 * val handler = RenameCommandHandler(context, database, tts)
 * val result = handler.processRenameCommand("Rename Button 1 to Save", "com.example.app")
 *
 * when (result) {
 *     is RenameResult.Success -> {
 *         // TTS feedback: "Renamed to Save. You can now say Save or Button 1."
 *         Log.i(TAG, "Command renamed: ${result.oldName} -> ${result.newName}")
 *     }
 *     is RenameResult.Error -> {
 *         // TTS feedback: "Could not find command 'Button 1'"
 *         Log.e(TAG, "Rename failed: ${result.message}")
 *     }
 * }
 * ```
 */

package com.augmentalis.voiceoscore.learnapp.commands

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Parsed rename command
 */
data class ParsedRename(
    val oldName: String,
    val newName: String
)

/**
 * Rename command result
 */
sealed class RenameResult {
    /**
     * Success: Command renamed with synonym added
     *
     * @param oldName Original command name (for reference)
     * @param newName New synonym added
     * @param command Updated command with new synonym
     */
    data class Success(
        val oldName: String,
        val newName: String,
        val command: GeneratedCommandDTO
    ) : RenameResult()

    /**
     * Error: Command not found or parsing failed
     *
     * @param message Error description
     */
    data class Error(val message: String) : RenameResult()
}

/**
 * Rename Command Handler
 *
 * Handles "Rename [old name] to [new name]" voice commands with intelligent
 * fuzzy matching and synonym management.
 *
 * ## Implementation Details
 * - Parsing: Supports 3 command formats (rename/change, to/as)
 * - Matching: Fuzzy match handles "button 1", "click button 1", "Button 1"
 * - Synonyms: Stored as comma-separated string, original label always preserved
 * - Database: Updates immediately via generatedCommands repository
 * - Feedback: TTS confirms rename with both old and new names
 *
 * ## Thread Safety
 * All public methods use withContext(Dispatchers.IO) for safe database access.
 */
class RenameCommandHandler(
    private val context: Context,
    private val database: VoiceOSDatabaseManager,
    private val tts: TextToSpeech
) {
    companion object {
        private const val TAG = "RenameCommandHandler"
    }

    /**
     * Process rename command from voice input
     *
     * Main entry point for rename command processing. Handles parsing,
     * matching, database update, and TTS feedback.
     *
     * @param voiceInput Raw voice input (e.g., "Rename Button 1 to Save")
     * @param packageName Current app package (for command lookup)
     * @return RenameResult indicating success or error
     */
    suspend fun processRenameCommand(
        voiceInput: String,
        packageName: String
    ): RenameResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing rename: '$voiceInput' for $packageName")

            // Parse command
            val parsed = parseRenameCommand(voiceInput)
                ?: return@withContext RenameResult.Error("Could not understand rename command")

            Log.d(TAG, "Parsed: oldName='${parsed.oldName}', newName='${parsed.newName}'")

            // Find command by old name
            val command = findCommandByName(parsed.oldName, packageName)
                ?: return@withContext RenameResult.Error("Could not find command '${parsed.oldName}'")

            Log.d(TAG, "Found command: ${command.commandText}")

            // Add new name as synonym
            val updatedCommand = addSynonym(command, parsed.newName)

            // Update database
            database.generatedCommands.update(updatedCommand)

            Log.i(TAG, "✅ Renamed '${parsed.oldName}' → '${parsed.newName}'")

            // Voice feedback
            withContext(Dispatchers.Main) {
                val synonymList = buildSynonymList(updatedCommand)
                val feedbackText = buildFeedbackText(parsed.newName, synonymList)

                tts.speak(
                    feedbackText,
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "rename_success"
                )
            }

            RenameResult.Success(
                oldName = parsed.oldName,
                newName = parsed.newName,
                command = updatedCommand
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error processing rename command", e)

            // Provide error feedback via TTS
            withContext(Dispatchers.Main) {
                tts.speak(
                    "Failed to rename command. Please try again.",
                    TextToSpeech.QUEUE_FLUSH,
                    null,
                    "rename_error"
                )
            }

            RenameResult.Error("Failed to rename: ${e.message}")
        }
    }

    /**
     * Parse rename command from voice input
     *
     * Supports multiple patterns:
     * - "rename X to Y"
     * - "rename X as Y"
     * - "change X to Y"
     *
     * @param voiceInput Raw voice command
     * @return ParsedRename or null if parsing failed
     */
    internal fun parseRenameCommand(voiceInput: String): ParsedRename? {
        val normalized = voiceInput.trim().lowercase()

        // Pattern 1: "rename X to Y"
        val pattern1 = Regex("rename (.+) to (.+)")
        pattern1.find(normalized)?.let { match ->
            return ParsedRename(
                oldName = match.groupValues[1].trim(),
                newName = match.groupValues[2].trim()
            )
        }

        // Pattern 2: "rename X as Y"
        val pattern2 = Regex("rename (.+) as (.+)")
        pattern2.find(normalized)?.let { match ->
            return ParsedRename(
                oldName = match.groupValues[1].trim(),
                newName = match.groupValues[2].trim()
            )
        }

        // Pattern 3: "change X to Y"
        val pattern3 = Regex("change (.+) to (.+)")
        pattern3.find(normalized)?.let { match ->
            return ParsedRename(
                oldName = match.groupValues[1].trim(),
                newName = match.groupValues[2].trim()
            )
        }

        return null
    }

    /**
     * Find command by name with fuzzy matching
     *
     * Matches commands flexibly:
     * - "button 1" matches "click button 1"
     * - "Button 1" matches "click button 1" (case-insensitive)
     * - "click button 1" matches "click button 1" (exact)
     *
     * Matching strategy:
     * 1. Try exact match (full command text)
     * 2. Try match without action prefix (click/type/scroll)
     * 3. Try match with "click" prefix added
     *
     * @param name Command name from voice input
     * @param packageName App package to search in
     * @return Matching GeneratedCommandDTO or null if not found
     */
    internal suspend fun findCommandByName(
        name: String,
        packageName: String
    ): GeneratedCommandDTO? = withContext(Dispatchers.IO) {
        val normalized = name.lowercase()

        // Get all commands for package
        val commands = database.generatedCommands.getByPackage(packageName)
        Log.d(TAG, "Searching ${commands.size} commands for '$name'")

        // Try exact match first
        commands.firstOrNull { cmd ->
            cmd.commandText.lowercase() == normalized
        }?.let {
            Log.d(TAG, "Exact match found: ${it.commandText}")
            return@withContext it
        }

        // Try match without "click" prefix
        val withoutAction = normalized.removePrefix("click ").removePrefix("type ").removePrefix("scroll ")
        commands.firstOrNull { cmd ->
            val cmdWithoutAction = cmd.commandText.lowercase()
                .removePrefix("click ")
                .removePrefix("type ")
                .removePrefix("scroll ")
            cmdWithoutAction == withoutAction
        }?.let {
            Log.d(TAG, "Partial match found: ${it.commandText}")
            return@withContext it
        }

        // Try match with "click" prefix added
        val withAction = "click $normalized"
        commands.firstOrNull { cmd ->
            cmd.commandText.lowercase() == withAction
        }?.let {
            Log.d(TAG, "Match with 'click' prefix: ${it.commandText}")
            return@withContext it
        }

        Log.w(TAG, "No match found for '$name'")
        null
    }

    /**
     * Add synonym to command
     *
     * Synonyms are stored as comma-separated string: "save,submit,send"
     * Original command text (without action) is always included as synonym.
     *
     * Example:
     * - Command: "click button 1"
     * - Add synonym: "save"
     * - Result: synonyms = "button 1,save"
     *
     * @param command Original command
     * @param newName New synonym to add
     * @return Updated command with new synonym
     */
    internal fun addSynonym(
        command: GeneratedCommandDTO,
        newName: String
    ): GeneratedCommandDTO {
        val existingSynonyms = command.synonyms
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toMutableSet()
            ?: mutableSetOf()

        // Add new synonym
        existingSynonyms.add(newName.lowercase())

        // Add original command text as synonym (if not already)
        val originalLabel = command.commandText.removePrefix("${command.actionType} ")
        existingSynonyms.add(originalLabel.lowercase())

        Log.d(TAG, "Updated synonyms: ${existingSynonyms.joinToString(",")}")

        return command.copy(
            synonyms = existingSynonyms.joinToString(",")
        )
    }

    /**
     * Build list of all synonyms for TTS feedback
     *
     * Returns up to 3 synonyms to avoid verbose feedback.
     *
     * @param command Command with synonyms
     * @return List of synonym strings
     */
    private fun buildSynonymList(command: GeneratedCommandDTO): List<String> {
        return command.synonyms
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.take(3) // Limit to 3 for brevity
            ?: emptyList()
    }

    /**
     * Build TTS feedback text
     *
     * Examples:
     * - "Renamed to Save. You can now say Save or Button 1."
     * - "Renamed to Save. You can now say Save, Button 1, or Submit."
     *
     * @param newName Primary new name
     * @param synonymList All available synonyms
     * @return Feedback text for TTS
     */
    private fun buildFeedbackText(newName: String, synonymList: List<String>): String {
        return when {
            synonymList.isEmpty() -> "Renamed to $newName."
            synonymList.size == 1 -> "Renamed to $newName. You can now say $newName."
            synonymList.size == 2 -> "Renamed to $newName. You can now say ${synonymList.joinToString(" or ")}."
            else -> {
                val lastSynonym = synonymList.last()
                val otherSynonyms = synonymList.dropLast(1).joinToString(", ")
                "Renamed to $newName. You can now say $otherSynonyms, or $lastSynonym."
            }
        }
    }
}
