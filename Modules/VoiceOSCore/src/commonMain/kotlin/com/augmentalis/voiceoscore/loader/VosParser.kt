/**
 * VosParser.kt - KMP parser for VOS (Voice OS) command files
 *
 * Parses VOS v2.0/v2.1 format using kotlinx.serialization.json (no Android deps).
 * Replaces ArrayJsonParser (androidMain, org.json) with a cross-platform implementation.
 *
 * VOS v2.1 format:
 * {
 *   "version": "2.1",
 *   "locale": "en-US",
 *   "domain": "app",
 *   "category_map": { "nav": "NAVIGATION", ... },
 *   "action_map": { "nav_back": "BACK", ... },
 *   "meta_map": { "app_browser": {"app_type": "browser"}, ... },
 *   "commands": [["action_id", "primary_text", ["syn1", "syn2"], "description"], ...]
 * }
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.loader

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Parsed command from a VOS file, platform-independent.
 */
data class VosParsedCommand(
    val id: String,
    val locale: String,
    val primaryText: String,
    val synonyms: List<String>,
    val description: String,
    val category: String,
    val actionType: String,
    val metadata: String,
    val isFallback: Boolean
)

/**
 * Result of parsing a VOS file.
 */
sealed class VosParseResult {
    data class Success(
        val commands: List<VosParsedCommand>,
        val locale: String,
        val version: String,
        val domain: String
    ) : VosParseResult()

    data class Error(val message: String) : VosParseResult()
}

/**
 * KMP parser for VOS command files.
 *
 * Uses kotlinx.serialization.json for cross-platform JSON parsing.
 * Handles both v1.0 (prefix-derived categories) and v2.0+ (explicit maps).
 */
object VosParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    /**
     * Parse VOS file contents into a list of [VosParsedCommand].
     *
     * @param jsonString Raw VOS file contents
     * @param isFallback Whether this is the fallback locale (en-US)
     * @return [VosParseResult] with parsed commands or error
     */
    fun parse(jsonString: String, isFallback: Boolean = false): VosParseResult {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject

            val version = root["version"]?.jsonPrimitive?.content ?: "1.0"
            val locale = root["locale"]?.jsonPrimitive?.content ?: "en-US"
            val domain = root["domain"]?.jsonPrimitive?.content ?: "app"

            // v2.0+: Explicit mapping tables
            val categoryMap = root["category_map"]?.jsonObject
            val actionMap = root["action_map"]?.jsonObject
            val metaMap = root["meta_map"]?.jsonObject

            val commandsArray = root["commands"]?.jsonArray
                ?: return VosParseResult.Error("Missing 'commands' array")

            val commands = parseCommandsArray(
                commandsArray, locale, isFallback,
                categoryMap, actionMap, metaMap
            )

            VosParseResult.Success(
                commands = commands,
                locale = locale,
                version = version,
                domain = domain
            )
        } catch (e: Exception) {
            VosParseResult.Error("Failed to parse VOS file: ${e.message}")
        }
    }

    /**
     * Parse the commands array. Each entry is:
     * `["action_id", "primary_text", ["synonym1", "synonym2"], "description"]`
     */
    private fun parseCommandsArray(
        commandsArray: JsonArray,
        locale: String,
        isFallback: Boolean,
        categoryMap: JsonObject?,
        actionMap: JsonObject?,
        metaMap: JsonObject?
    ): List<VosParsedCommand> {
        val commands = mutableListOf<VosParsedCommand>()

        for (element in commandsArray) {
            try {
                val cmdArray = element.jsonArray
                if (cmdArray.size != 4) continue

                val actionId = cmdArray[0].jsonPrimitive.content
                val primaryText = cmdArray[1].jsonPrimitive.content
                val synonymsArray = cmdArray[2].jsonArray
                val description = cmdArray[3].jsonPrimitive.content

                val synonyms = synonymsArray.map { it.jsonPrimitive.content }
                val category = resolveCategory(actionId, categoryMap)
                val actionType = actionMap?.get(actionId)?.jsonPrimitive?.content ?: ""
                val metadata = metaMap?.get(actionId)?.toString() ?: ""

                commands.add(
                    VosParsedCommand(
                        id = actionId,
                        locale = locale,
                        primaryText = primaryText,
                        synonyms = synonyms,
                        description = description,
                        category = category,
                        actionType = actionType,
                        metadata = metadata,
                        isFallback = isFallback
                    )
                )
            } catch (_: Exception) {
                // Skip malformed entries, continue parsing
            }
        }

        return commands
    }

    /**
     * Resolve category from action_id prefix using the category map.
     * v2.0+: Looks up prefix in categoryMap (e.g., "nav" -> "NAVIGATION")
     * v1.0:  Uses prefix directly (e.g., "nav_back" -> "nav")
     */
    private fun resolveCategory(actionId: String, categoryMap: JsonObject?): String {
        val prefix = actionId.substringBefore("_", "unknown")
        if (categoryMap != null) {
            val mapped = categoryMap[prefix]?.jsonPrimitive?.content
            if (!mapped.isNullOrEmpty()) return mapped
        }
        return prefix
    }

    /**
     * Serialize a list of synonyms as a JSON array string.
     * Used when inserting into the database (synonyms column stores JSON).
     */
    fun synonymsToJson(synonyms: List<String>): String {
        return buildString {
            append('[')
            synonyms.forEachIndexed { index, syn ->
                if (index > 0) append(',')
                append('"')
                append(syn.replace("\"", "\\\""))
                append('"')
            }
            append(']')
        }
    }

    /**
     * Parse a JSON array string of synonyms back to a list.
     */
    fun parseSynonymsJson(synonymsJson: String): List<String> {
        if (synonymsJson.isBlank() || synonymsJson == "[]") return emptyList()
        return try {
            val array = json.parseToJsonElement(synonymsJson).jsonArray
            array.map { it.jsonPrimitive.content }
        } catch (_: Exception) {
            // Fallback: manual parse for simple JSON arrays
            synonymsJson
                .removePrefix("[")
                .removeSuffix("]")
                .split(",")
                .map { it.trim().removeSurrounding("\"") }
                .filter { it.isNotEmpty() }
        }
    }
}
