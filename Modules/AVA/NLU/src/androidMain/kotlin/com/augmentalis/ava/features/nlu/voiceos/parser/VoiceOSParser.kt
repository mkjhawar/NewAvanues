package com.augmentalis.ava.features.nlu.voiceos.parser

import com.augmentalis.ava.features.nlu.voiceos.model.VoiceOSCommand
import com.augmentalis.ava.features.nlu.voiceos.model.VoiceOSFile
import com.augmentalis.ava.features.nlu.voiceos.model.VoiceOSFileInfo
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parser for .vos file format (VoiceOS commands)
 *
 * Pure functions - no I/O, no side effects
 * Receives JSON string, returns data objects
 */
object VoiceOSParser {

    /**
     * Parse complete .vos file from JSON string
     */
    fun parse(jsonString: String): VoiceOSFile {
        val json = JSONObject(jsonString)

        val fileInfo = json.getJSONObject("file_info")
        val commandsArray = json.getJSONArray("commands")
        val commands = parseCommands(commandsArray)

        return VoiceOSFile(
            schema = json.getString("schema"),
            version = json.getString("version"),
            locale = json.getString("locale"),
            fileName = fileInfo.getString("file_name"),
            category = fileInfo.getString("category"),
            commands = commands
        )
    }

    /**
     * Parse array of commands
     */
    fun parseCommands(commandsArray: JSONArray): List<VoiceOSCommand> {
        val result = mutableListOf<VoiceOSCommand>()
        for (i in 0 until commandsArray.length()) {
            val cmdJson = commandsArray.getJSONObject(i)
            val command = parseCommand(cmdJson)
            result.add(command)
        }
        return result
    }

    /**
     * Parse single command
     */
    fun parseCommand(cmdJson: JSONObject): VoiceOSCommand {
        return VoiceOSCommand(
            action = cmdJson.getString("action"),
            cmd = cmdJson.getString("cmd"),
            synonyms = cmdJson.getJSONArray("syn").toStringList()
        )
    }

    /**
     * Extension function to convert JSONArray to List<String>
     */
    private fun JSONArray.toStringList(): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until length()) {
            list.add(getString(i))
        }
        return list
    }
}
