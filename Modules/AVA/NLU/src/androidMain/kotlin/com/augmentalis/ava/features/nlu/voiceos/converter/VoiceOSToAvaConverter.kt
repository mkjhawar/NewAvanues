package com.augmentalis.ava.features.nlu.voiceos.converter

import com.augmentalis.ava.features.nlu.ava.model.AvaFile
import com.augmentalis.ava.features.nlu.ava.model.AvaFileMetadata
import com.augmentalis.ava.features.nlu.ava.model.AvaIntent
import com.augmentalis.ava.features.nlu.voiceos.model.VoiceOSCommand
import com.augmentalis.ava.features.nlu.voiceos.model.VoiceOSFile

/**
 * Converter: VoiceOSFile → AvaFile
 *
 * Pure data transformation - no I/O, no side effects
 * Converts VoiceOS .vos format to AVA .ava format
 */
object VoiceOSToAvaConverter {

    /**
     * Convert VoiceOSFile to AvaFile
     */
    fun convertVosToAva(vosFile: VoiceOSFile): AvaFile {
        val intents = vosFile.commands.map { command ->
            convertCommand(command, vosFile.locale)
        }

        val metadata = AvaFileMetadata(
            filename = vosFile.fileName.replace(".vos", ".ava"),
            category = vosFile.category,
            name = vosFile.category.capitalize(),
            description = "Imported from VoiceOS ${vosFile.fileName}",
            intentCount = intents.size
        )

        return AvaFile(
            schema = "ava-1.0",
            version = vosFile.version,
            locale = vosFile.locale,
            metadata = metadata,
            intents = intents,
            globalSynonyms = emptyMap()
        )
    }

    /**
     * Convert VoiceOSCommand to AvaIntent
     */
    fun convertCommand(command: VoiceOSCommand, locale: String): AvaIntent {
        val intentId = command.action.lowercase().replace("_", "_")
        val category = detectCategory(command.action)
        val tags = generateTags(command.cmd)

        return AvaIntent(
            id = intentId,
            canonical = command.cmd,
            synonyms = command.synonyms,
            category = category,
            priority = 1,
            tags = tags,
            locale = locale,
            source = "VOICEOS"
        )
    }

    /**
     * Detect category from action name
     */
    private fun detectCategory(action: String): String {
        return when {
            action.contains("VOLUME") || action.contains("AUDIO") || action.contains("MUSIC") -> "media_control"
            action.contains("LIGHT") || action.contains("DEVICE") || action.contains("SWITCH") -> "device_control"
            action.contains("CALL") || action.contains("MESSAGE") || action.contains("CONTACT") -> "communication"
            action.contains("NAVIGATE") || action.contains("OPEN") || action.contains("LAUNCH") -> "navigation"
            action.contains("SEARCH") || action.contains("QUERY") || action.contains("FIND") -> "information"
            else -> "general"
        }
    }

    /**
     * Generate tags from command text
     */
    private fun generateTags(cmdText: String): List<String> {
        val words = cmdText.lowercase().split(" ")
        return words.filter { it.length > 3 }.take(3)
    }

    /**
     * Capitalize first character
     */
    private fun String.capitalize(): String {
        return replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }
}
