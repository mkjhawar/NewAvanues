/**
 * ArrayJsonParser.kt - Parser for VOS (Voice OS) command files
 *
 * Purpose: Parse compact array-format VOS commands
 * v1.0 Format: ["action_id", "primary_text", ["synonym1", "synonym2"], "description"]
 * v2.0 Format: Same array format + root-level category_map, action_map, meta_map
 * File Extension: .VOS (uppercase)
 *
 * v2.0 additions:
 * - category_map: Maps command_id prefix → CommandCategory name (e.g., "nav" → "NAVIGATION")
 * - action_map: Maps command_id → CommandActionType name (e.g., "nav_back" → "BACK")
 * - meta_map: Maps command_id → metadata JSON object (e.g., "gesture_pan_left" → {"direction":"left"})
 */

package com.augmentalis.voiceoscore.managers.commandmanager.loader

import com.augmentalis.voiceoscore.managers.commandmanager.database.sqldelight.VoiceCommandEntity
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log

/**
 * Parser for VOS (Voice OS) command files.
 * Reads .VOS files containing compact array-format commands.
 * Supports v1.0 (prefix-derived category) and v2.0 (explicit maps).
 */
class ArrayJsonParser {

    companion object {
        private const val TAG = "ArrayJsonParser"

        /**
         * Parse VOS file contents into list of VoiceCommandEntity
         *
         * @param jsonString Raw VOS file contents (compact JSON format)
         * @param isFallback Whether this is the fallback locale (en-US)
         * @return ParseResult with commands or error
         * @throws JSONException if VOS format is malformed
         */
        fun parseCommandsJson(jsonString: String, isFallback: Boolean = false): ParseResult {
            return try {
                val jsonObject = JSONObject(jsonString)

                // Extract metadata
                val version = jsonObject.optString("version", "1.0")
                val locale = jsonObject.optString("locale", "en-US")
                val fallbackLocale = jsonObject.optString("fallback", "en-US")

                // v2.0: Read explicit mapping tables
                val categoryMap = jsonObject.optJSONObject("category_map")
                val actionMap = jsonObject.optJSONObject("action_map")
                val metaMap = jsonObject.optJSONObject("meta_map")

                // Get commands array
                val commandsArray = jsonObject.getJSONArray("commands")
                val commands = parseCommandsArray(
                    commandsArray, locale, isFallback,
                    categoryMap, actionMap, metaMap
                )

                Log.d(TAG, "Parsed ${commands.size} commands for locale: $locale (v$version, fallback: $isFallback)")

                ParseResult.Success(
                    commands = commands,
                    locale = locale,
                    version = version,
                    fallbackLocale = fallbackLocale
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse commands JSON", e)
                ParseResult.Error("Failed to parse JSON: ${e.message}")
            }
        }

        /**
         * Parse commands array into entities.
         *
         * Array structure: `["action_id", "primary_text", ["synonyms"], "description"]`
         * Positions:
         *   0: action_id (String) — unique command identifier
         *   1: primary_text (String) — trigger phrase
         *   2: synonyms (JSONArray) — alternative phrases
         *   3: description (String) — human-readable description
         *
         * v2.0: Uses categoryMap/actionMap/metaMap for explicit resolution instead of
         * deriving category from action_id prefix.
         */
        private fun parseCommandsArray(
            commandsArray: JSONArray,
            locale: String,
            isFallback: Boolean,
            categoryMap: JSONObject? = null,
            actionMap: JSONObject? = null,
            metaMap: JSONObject? = null
        ): List<VoiceCommandEntity> {
            val commands = mutableListOf<VoiceCommandEntity>()

            for (i in 0 until commandsArray.length()) {
                try {
                    val commandArray = commandsArray.getJSONArray(i)

                    // Validate array length (must be 4)
                    if (commandArray.length() != 4) {
                        Log.w(TAG, "Skipping command at index $i: expected 4 elements, got ${commandArray.length()}")
                        continue
                    }

                    // Parse command data
                    val actionId = commandArray.getString(0)
                    val primaryText = commandArray.getString(1)
                    val synonymsArray = commandArray.getJSONArray(2)
                    val description = commandArray.getString(3)

                    // Parse synonyms
                    val synonyms = mutableListOf<String>()
                    for (j in 0 until synonymsArray.length()) {
                        synonyms.add(synonymsArray.getString(j))
                    }

                    // Resolve category: v2.0 uses map, v1.0 derives from prefix
                    val category = resolveCategory(actionId, categoryMap)

                    // Resolve action type: v2.0 uses map, v1.0 uses actionId
                    val actionType = actionMap?.optString(actionId, "") ?: ""

                    // Resolve metadata: v2.0 provides explicit metadata per command
                    val metadata = metaMap?.optJSONObject(actionId)?.toString() ?: ""

                    // Create entity
                    val command = VoiceCommandEntity(
                        id = actionId,
                        locale = locale,
                        primaryText = primaryText,
                        synonyms = JSONArray(synonyms).toString(),
                        description = description,
                        category = category,
                        actionType = actionType,
                        metadata = metadata,
                        priority = 50,
                        isFallback = isFallback
                    )

                    commands.add(command)

                } catch (e: Exception) {
                    Log.w(TAG, "Failed to parse command at index $i", e)
                    // Continue parsing other commands
                }
            }

            return commands
        }

        /**
         * Resolve category from action_id.
         * v2.0: Looks up prefix in categoryMap (e.g., "nav" → "NAVIGATION")
         * v1.0: Uses prefix directly (e.g., "nav_back" → "nav")
         */
        private fun resolveCategory(actionId: String, categoryMap: JSONObject?): String {
            val prefix = actionId.substringBefore("_", "unknown")
            if (categoryMap != null) {
                val mapped = categoryMap.optString(prefix, "")
                if (mapped.isNotEmpty()) return mapped
            }
            return prefix
        }

        /**
         * Parse UI strings JSON (array format)
         *
         * Format: ["string_id", "string_value"]
         *
         * @param jsonString Raw JSON file contents
         * @return Map of string ID to value
         */
        fun parseUiStringsJson(jsonString: String): Map<String, String> {
            return try {
                val jsonObject = JSONObject(jsonString)
                val stringsArray = jsonObject.getJSONArray("strings")
                val strings = mutableMapOf<String, String>()

                for (i in 0 until stringsArray.length()) {
                    val stringArray = stringsArray.getJSONArray(i)
                    if (stringArray.length() == 2) {
                        val id = stringArray.getString(0)
                        val value = stringArray.getString(1)
                        strings[id] = value
                    }
                }

                Log.d(TAG, "Parsed ${strings.size} UI strings")
                strings

            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse UI strings JSON", e)
                emptyMap()
            }
        }

        /**
         * Validate JSON structure without full parsing
         *
         * @return true if JSON appears valid, false otherwise
         */
        fun isValidCommandsJson(jsonString: String): Boolean {
            return try {
                val jsonObject = JSONObject(jsonString)
                jsonObject.has("version") &&
                jsonObject.has("locale") &&
                jsonObject.has("commands") &&
                jsonObject.getJSONArray("commands").length() > 0
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Parse result sealed class
     */
    sealed class ParseResult {
        data class Success(
            val commands: List<VoiceCommandEntity>,
            val locale: String,
            val version: String,
            val fallbackLocale: String
        ) : ParseResult()

        data class Error(val message: String) : ParseResult()
    }
}
