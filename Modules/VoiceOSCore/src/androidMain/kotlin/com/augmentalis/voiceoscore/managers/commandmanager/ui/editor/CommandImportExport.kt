/**
 * CommandImportExport.kt - JSON import/export for voice commands
 *
 * Handles serialization and deserialization of voice commands to/from JSON format
 */

package com.augmentalis.voiceoscore.managers.commandmanager.ui.editor

import android.content.Context
import android.net.Uri
import android.util.Log
import com.augmentalis.voiceoscore.managers.commandmanager.registry.ActionType
import com.augmentalis.voiceoscore.managers.commandmanager.registry.VoiceCommand
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * Import/Export manager for voice commands
 */
class CommandImportExport {

    companion object {
        private const val TAG = "CommandImportExport"
        private const val JSON_VERSION = "1.0"
        private const val FILE_EXTENSION = ".json"
    }

    /**
     * Export commands to JSON string
     */
    fun exportToJson(commands: List<VoiceCommand>, includeMetadata: Boolean = true): String {
        val jsonObject = JSONObject()

        // Add metadata
        if (includeMetadata) {
            jsonObject.put("version", JSON_VERSION)
            jsonObject.put("exportDate", System.currentTimeMillis())
            jsonObject.put("commandCount", commands.size)
        }

        // Add commands array
        val commandsArray = JSONArray()
        commands.forEach { command ->
            commandsArray.put(commandToJson(command))
        }
        jsonObject.put("commands", commandsArray)

        return jsonObject.toString(2) // Pretty print with 2-space indent
    }

    /**
     * Export single command to JSON
     */
    fun exportCommandToJson(command: VoiceCommand): String {
        return commandToJson(command).toString(2)
    }

    /**
     * Import commands from JSON string
     */
    fun importFromJson(jsonString: String): ImportResult {
        return try {
            val jsonObject = JSONObject(jsonString)

            // Validate JSON structure
            if (!jsonObject.has("commands")) {
                return ImportResult.Failure("Invalid JSON: missing 'commands' array")
            }

            // Parse version (if present)
            val version = jsonObject.optString("version", "unknown")
            Log.i(TAG, "Importing commands from JSON version: $version")

            // Parse commands
            val commandsArray = jsonObject.getJSONArray("commands")
            val commands = mutableListOf<VoiceCommand>()
            val errors = mutableListOf<String>()

            for (i in 0 until commandsArray.length()) {
                try {
                    val commandJson = commandsArray.getJSONObject(i)
                    val command = jsonToCommand(commandJson)
                    commands.add(command)
                } catch (e: Exception) {
                    errors.add("Command $i: ${e.message}")
                    Log.e(TAG, "Failed to parse command $i", e)
                }
            }

            if (commands.isEmpty() && errors.isNotEmpty()) {
                ImportResult.Failure("Failed to import any commands: ${errors.joinToString(", ")}")
            } else {
                ImportResult.Success(commands, errors)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to import commands from JSON", e)
            ImportResult.Failure("JSON parsing error: ${e.message}")
        }
    }

    /**
     * Export commands to file
     */
    fun exportToFile(
        context: Context,
        commands: List<VoiceCommand>,
        fileName: String = "voice_commands_${System.currentTimeMillis()}$FILE_EXTENSION"
    ): ExportFileResult {
        return try {
            val jsonString = exportToJson(commands, includeMetadata = true)

            // Save to external files directory
            val file = File(context.getExternalFilesDir(null), fileName)
            FileOutputStream(file).use { output ->
                output.write(jsonString.toByteArray())
            }

            Log.i(TAG, "Exported ${commands.size} commands to: ${file.absolutePath}")
            ExportFileResult.Success(file.absolutePath, commands.size)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to export commands to file", e)
            ExportFileResult.Failure("File write error: ${e.message}")
        }
    }

    /**
     * Import commands from file
     */
    fun importFromFile(context: Context, uri: Uri): ImportResult {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                return ImportResult.Failure("Failed to open file")
            }

            val jsonString = inputStream.bufferedReader().use { it.readText() }
            inputStream.close()

            importFromJson(jsonString)

        } catch (e: IOException) {
            Log.e(TAG, "Failed to import commands from file", e)
            ImportResult.Failure("File read error: ${e.message}")
        }
    }

    /**
     * Validate JSON structure without importing
     */
    fun validateJson(jsonString: String): ValidationResult {
        return try {
            val jsonObject = JSONObject(jsonString)

            val errors = mutableListOf<String>()

            // Check required fields
            if (!jsonObject.has("commands")) {
                errors.add("Missing 'commands' array")
            }

            // Validate commands array
            if (jsonObject.has("commands")) {
                val commandsArray = jsonObject.getJSONArray("commands")
                for (i in 0 until commandsArray.length()) {
                    val commandJson = commandsArray.getJSONObject(i)
                    val commandErrors = validateCommandJson(commandJson)
                    if (commandErrors.isNotEmpty()) {
                        errors.add("Command $i: ${commandErrors.joinToString(", ")}")
                    }
                }
            }

            ValidationResult(errors.isEmpty(), errors)

        } catch (e: Exception) {
            ValidationResult(false, listOf("JSON parsing error: ${e.message}"))
        }
    }

    // Private helper methods

    /**
     * Convert VoiceCommand to JSON
     */
    private fun commandToJson(command: VoiceCommand): JSONObject {
        val json = JSONObject()

        json.put("id", command.id)
        json.put("phrases", JSONArray(command.phrases))
        json.put("priority", command.priority)
        json.put("namespace", command.namespace)
        json.put("actionType", command.actionType.name)
        json.put("enabled", command.enabled)

        // Action params
        if (command.actionParams.isNotEmpty()) {
            val paramsJson = JSONObject()
            command.actionParams.forEach { (key, value) ->
                paramsJson.put(key, value)
            }
            json.put("actionParams", paramsJson)
        }

        // Metadata
        if (command.metadata.isNotEmpty()) {
            val metadataJson = JSONObject()
            command.metadata.forEach { (key, value) ->
                metadataJson.put(key, value)
            }
            json.put("metadata", metadataJson)
        }

        return json
    }

    /**
     * Convert JSON to VoiceCommand
     */
    private fun jsonToCommand(json: JSONObject): VoiceCommand {
        // Required fields
        val id = json.getString("id")
        val phrasesArray = json.getJSONArray("phrases")
        val phrases = mutableListOf<String>()
        for (i in 0 until phrasesArray.length()) {
            phrases.add(phrasesArray.getString(i))
        }

        // Optional fields with defaults
        val priority = json.optInt("priority", 50)
        val namespace = json.optString("namespace", "default")
        val actionTypeString = json.optString("actionType", ActionType.CUSTOM_ACTION.name)
        val actionType = try {
            ActionType.valueOf(actionTypeString)
        } catch (e: IllegalArgumentException) {
            ActionType.CUSTOM_ACTION
        }
        val enabled = json.optBoolean("enabled", true)

        // Action params
        val actionParams = mutableMapOf<String, Any>()
        if (json.has("actionParams")) {
            val paramsJson = json.getJSONObject("actionParams")
            paramsJson.keys().forEach { key ->
                actionParams[key] = paramsJson.get(key)
            }
        }

        // Metadata
        val metadata = mutableMapOf<String, Any>()
        if (json.has("metadata")) {
            val metadataJson = json.getJSONObject("metadata")
            metadataJson.keys().forEach { key ->
                metadata[key] = metadataJson.get(key)
            }
        }

        return VoiceCommand(
            id = id,
            phrases = phrases,
            priority = priority,
            namespace = namespace,
            actionType = actionType,
            actionParams = actionParams,
            enabled = enabled,
            metadata = metadata
        )
    }

    /**
     * Validate command JSON structure
     */
    private fun validateCommandJson(json: JSONObject): List<String> {
        val errors = mutableListOf<String>()

        // Required fields
        if (!json.has("id")) errors.add("Missing 'id'")
        if (!json.has("phrases")) errors.add("Missing 'phrases'")

        // Validate phrases array
        if (json.has("phrases")) {
            val phrasesArray = json.getJSONArray("phrases")
            if (phrasesArray.length() == 0) {
                errors.add("'phrases' array is empty")
            }
        }

        // Validate priority range
        if (json.has("priority")) {
            val priority = json.getInt("priority")
            if (priority !in 1..100) {
                errors.add("'priority' must be between 1 and 100")
            }
        }

        // Validate action type
        if (json.has("actionType")) {
            val actionTypeString = json.getString("actionType")
            try {
                ActionType.valueOf(actionTypeString)
            } catch (e: IllegalArgumentException) {
                errors.add("Invalid 'actionType': $actionTypeString")
            }
        }

        return errors
    }
}

/**
 * Import result
 */
sealed class ImportResult {
    data class Success(
        val commands: List<VoiceCommand>,
        val warnings: List<String> = emptyList()
    ) : ImportResult()

    data class Failure(val error: String) : ImportResult()
}

/**
 * Export file result
 */
sealed class ExportFileResult {
    data class Success(val filePath: String, val commandCount: Int) : ExportFileResult()
    data class Failure(val error: String) : ExportFileResult()
}

/**
 * Validation result
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>
)
