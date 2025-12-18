package com.augmentalis.ava.features.nlu.ava.parser

import com.augmentalis.ava.features.nlu.ava.model.AvaFile
import com.augmentalis.ava.features.nlu.ava.model.AvaFileMetadata
import com.augmentalis.ava.features.nlu.ava.model.AvaIntent
import org.json.JSONObject

/**
 * Parser for .ava and .vos file formats
 *
 * Pure functions - no I/O, no side effects
 * Receives file content string, returns data objects
 *
 * Supported formats:
 * - Universal Format v2.0 (avu-1.0): VCM text format used by both AVA and VoiceOS
 * - VoiceOS JSON Format v1.0 (vos-1.0): Old JSON format with "commands" array
 */
object AvaFileParser {

    /**
     * Parse .ava or .vos file content
     *
     * Auto-detects format:
     * - Starts with # or --- → Universal Format v2.0
     * - Starts with { → JSON format (vos-1.0 or ava-1.0)
     */
    fun parse(content: String): AvaFile {
        val trimmed = content.trim()

        return when {
            trimmed.startsWith("#") || trimmed.startsWith("---") -> {
                // Universal Format v2.0 (VCM text format)
                parseUniversalFormat(trimmed)
            }
            trimmed.startsWith("{") -> {
                // JSON format - could be vos-1.0 or ava-1.0
                parseJsonFormat(trimmed)
            }
            else -> {
                throw IllegalArgumentException(
                    "Invalid file format: Must be Universal Format (start with # or ---) " +
                    "or JSON format (start with {)"
                )
            }
        }
    }

    /**
     * Parse JSON format (.vos or .ava)
     *
     * Supports:
     * - vos-1.0: {"schema": "vos-1.0", "commands": [{"action": "...", "cmd": "...", "syn": [...]}]}
     * - ava-1.0: {"s": "ava-1.0", "i": [{"id": "...", "c": "...", "s": [...]}]}
     */
    private fun parseJsonFormat(content: String): AvaFile {
        val json = JSONObject(content)

        // Detect schema type
        val schema = json.optString("schema", json.optString("s", ""))

        return when {
            schema.startsWith("vos") -> parseVosJsonFormat(json)
            schema.startsWith("ava") -> parseAvaJsonFormat(json)
            json.has("commands") -> parseVosJsonFormat(json) // Fallback for vos without schema
            json.has("i") -> parseAvaJsonFormat(json) // Fallback for ava without schema
            else -> throw IllegalArgumentException("Unknown JSON format: no recognized schema or structure")
        }
    }

    /**
     * Parse VoiceOS JSON format (vos-1.0)
     *
     * Format:
     * {
     *   "schema": "vos-1.0",
     *   "locale": "en-US",
     *   "file_info": { "filename": "...", "category": "...", ... },
     *   "commands": [
     *     { "action": "TURN_ON_WIFI", "cmd": "turn on wifi", "syn": ["wifi on", ...] }
     *   ]
     * }
     */
    private fun parseVosJsonFormat(json: JSONObject): AvaFile {
        val schema = json.optString("schema", "vos-1.0")
        val version = json.optString("version", "1.0.0")
        val locale = json.optString("locale", "en-US")

        // Parse file_info metadata
        val fileInfo = json.optJSONObject("file_info")
        val metadata = AvaFileMetadata(
            filename = fileInfo?.optString("filename", "unknown.vos") ?: "unknown.vos",
            category = fileInfo?.optString("category", "voiceos") ?: "voiceos",
            name = fileInfo?.optString("display_name", "VoiceOS Commands") ?: "VoiceOS Commands",
            description = fileInfo?.optString("description", "") ?: "",
            intentCount = 0 // Will be updated after parsing
        )

        // Parse commands array
        val commandsArray = json.optJSONArray("commands") ?: return AvaFile(
            schema = schema,
            version = version,
            locale = locale,
            metadata = metadata,
            intents = emptyList(),
            globalSynonyms = emptyMap()
        )

        val intents = mutableListOf<AvaIntent>()

        for (i in 0 until commandsArray.length()) {
            val cmd = commandsArray.getJSONObject(i)
            val action = cmd.optString("action", "")
            val canonical = cmd.optString("cmd", "")
            val synArray = cmd.optJSONArray("syn")

            if (action.isNotBlank() && canonical.isNotBlank()) {
                val synonyms = mutableListOf<String>()
                if (synArray != null) {
                    for (j in 0 until synArray.length()) {
                        synonyms.add(synArray.getString(j))
                    }
                }

                // Convert ACTION_NAME to action_name (lowercase with underscores)
                val intentId = action.lowercase()

                intents.add(
                    AvaIntent(
                        id = intentId,
                        canonical = canonical,
                        synonyms = synonyms,
                        category = metadata.category,
                        priority = 1,
                        tags = emptyList(),
                        locale = locale,
                        source = "VOS_JSON",
                        ipcCode = "VCM",
                        ipcTemplate = "VCM:$intentId:$canonical"
                    )
                )
            }
        }

        return AvaFile(
            schema = schema,
            version = version,
            locale = locale,
            metadata = metadata.copy(intentCount = intents.size),
            intents = intents,
            globalSynonyms = emptyMap()
        )
    }

    /**
     * Parse AVA JSON format (ava-1.0) - abbreviated field names
     */
    private fun parseAvaJsonFormat(json: JSONObject): AvaFile {
        val schema = json.optString("s", "ava-1.0")
        val version = json.optString("v", "1.0.0")
        val locale = json.optString("l", "en-US")

        // Parse metadata
        val metaJson = json.optJSONObject("m")
        val metadata = AvaFileMetadata(
            filename = metaJson?.optString("f", "unknown.ava") ?: "unknown.ava",
            category = metaJson?.optString("c", "voice_command") ?: "voice_command",
            name = metaJson?.optString("n", "Unknown") ?: "Unknown",
            description = metaJson?.optString("d", "") ?: "",
            intentCount = metaJson?.optInt("cnt", 0) ?: 0
        )

        // Parse intents array
        val intentsArray = json.optJSONArray("i") ?: return AvaFile(
            schema = schema,
            version = version,
            locale = locale,
            metadata = metadata,
            intents = emptyList(),
            globalSynonyms = emptyMap()
        )

        val intents = mutableListOf<AvaIntent>()

        for (i in 0 until intentsArray.length()) {
            val intent = intentsArray.getJSONObject(i)
            val id = intent.optString("id", "")
            val canonical = intent.optString("c", "")
            val synArray = intent.optJSONArray("s")

            if (id.isNotBlank()) {
                val synonyms = mutableListOf<String>()
                if (synArray != null) {
                    for (j in 0 until synArray.length()) {
                        synonyms.add(synArray.getString(j))
                    }
                }

                intents.add(
                    AvaIntent(
                        id = id,
                        canonical = canonical.ifBlank { synonyms.firstOrNull() ?: id },
                        synonyms = synonyms,
                        category = intent.optString("cat", metadata.category),
                        priority = intent.optInt("p", 1),
                        tags = emptyList(),
                        locale = locale,
                        source = "AVA_JSON",
                        ipcCode = "VCM",
                        ipcTemplate = "VCM:$id:$canonical"
                    )
                )
            }
        }

        // Parse global synonyms
        val synJson = json.optJSONObject("syn")
        val globalSynonyms = mutableMapOf<String, List<String>>()
        if (synJson != null) {
            for (key in synJson.keys()) {
                val synArray = synJson.optJSONArray(key)
                if (synArray != null) {
                    val syns = mutableListOf<String>()
                    for (i in 0 until synArray.length()) {
                        syns.add(synArray.getString(i))
                    }
                    globalSynonyms[key] = syns
                }
            }
        }

        return AvaFile(
            schema = schema,
            version = version,
            locale = locale,
            metadata = metadata.copy(intentCount = intents.size),
            intents = intents,
            globalSynonyms = globalSynonyms
        )
    }

    /**
     * Parse Universal Format v2.0 .ava file
     */
    private fun parseUniversalFormat(content: String): AvaFile {
        val sections = content.split("---").map { it.trim() }
        require(sections.size >= 3) {
            "Invalid Universal Format: expected at least 3 sections (header, metadata, entries)"
        }

        // Parse metadata section (section[1])
        val metadataLines = sections[1].lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        var schema = ""
        var version = ""
        var locale = ""
        val metadataMap = mutableMapOf<String, String>()
        var inMetadataBlock = false

        for (line in metadataLines) {
            when {
                line.startsWith("schema:") -> schema = line.substringAfter(":").trim()
                line.startsWith("version:") -> version = line.substringAfter(":").trim()
                line.startsWith("locale:") -> locale = line.substringAfter(":").trim()
                line == "metadata:" -> inMetadataBlock = true
                inMetadataBlock && line.contains(":") -> {
                    val key = line.substringBefore(":").trim()
                    val value = line.substringAfter(":").trim()
                    metadataMap[key] = value
                }
            }
        }

        // Parse entries section (section[2])
        val entries = sections[2].lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        val intents = mutableListOf<AvaIntent>()
        val intentGroups = mutableMapOf<String, MutableList<String>>()

        // Group entries by intent ID
        for (entry in entries) {
            val parts = entry.split(":", limit = 3)
            if (parts.size >= 3) {
                val code = parts[0]
                val id = parts[1]
                val text = parts[2]

                // Use getOrPut for thread-safe access
                intentGroups.getOrPut(id) { mutableListOf() }.add("$code:$text")
            }
        }

        // Convert groups to AvaIntent objects
        for ((id, examples) in intentGroups) {
            val firstExample = examples.first()
            val code = firstExample.substringBefore(":")
            val canonical = firstExample.substringAfter(":")
            val synonyms = examples.drop(1).map { it.substringAfter(":") }

            intents.add(
                AvaIntent(
                    id = id,
                    canonical = canonical,
                    synonyms = synonyms,
                    category = metadataMap["category"] ?: "voice_command",
                    priority = metadataMap["priority"]?.toIntOrNull() ?: 1,
                    tags = emptyList(),
                    locale = locale,
                    source = "UNIVERSAL_V2",
                    ipcCode = code,
                    ipcTemplate = "$code:$id:$canonical"
                )
            )
        }

        // Parse synonyms section if present (section[3])
        val globalSynonyms = if (sections.size > 3) {
            parseUniversalSynonyms(sections[3])
        } else {
            emptyMap()
        }

        val metadata = AvaFileMetadata(
            filename = metadataMap["file"] ?: "unknown.ava",
            category = metadataMap["category"] ?: "voice_command",
            name = metadataMap["name"] ?: "Unknown",
            description = metadataMap["description"] ?: "",
            intentCount = intents.size
        )

        return AvaFile(
            schema = schema,
            version = version,
            locale = locale,
            metadata = metadata,
            intents = intents,
            globalSynonyms = globalSynonyms
        )
    }

    /**
     * Parse synonyms from Universal Format
     */
    private fun parseUniversalSynonyms(section: String): Map<String, List<String>> {
        val lines = section.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        val synonyms = mutableMapOf<String, List<String>>()
        var inSynonymsBlock = false

        for (line in lines) {
            when {
                line.startsWith("synonyms:") -> inSynonymsBlock = true
                inSynonymsBlock && line.contains(":") -> {
                    val key = line.substringBefore(":").trim()
                    val valueStr = line.substringAfter(":").trim()

                    // Parse [word1, word2, word3]
                    if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                        val values = valueStr.substring(1, valueStr.length - 1)
                            .split(",")
                            .map { it.trim() }
                        synonyms[key] = values
                    }
                }
            }
        }

        return synonyms
    }
}
