/**
 * VosParser.kt - KMP parser for VOS (Voice OS) command files
 *
 * Parses VOS v2.1 JSON and v3.0 compact formats.
 * Auto-detects format: first non-whitespace char '{' = JSON, '#' or 'V' = compact.
 *
 * v3.0 compact format:
 *   # Comment lines
 *   VOS:3.0:en-US:en-US:app
 *   action_id|primary_text|synonym1,synonym2|description
 *
 * Maps (category, action, metadata) are compiled as constants since
 * they are identical across all locales. This eliminates ~25 KB of
 * per-file duplication.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.loader

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
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
 * Supports two formats:
 * - v2.1 JSON: Full JSON with embedded maps (backward compatible)
 * - v3.0 Compact: Pipe-delimited commands with compiled maps
 *
 * Auto-detects format by first non-whitespace character.
 */
object VosParser {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    // ═══════════════════════════════════════════════════════════════════
    // Compiled Maps — locale-independent, shared across all VOS files
    // ═══════════════════════════════════════════════════════════════════

    /** Maps action_id prefix to CommandCategory name. */
    val CATEGORY_MAP: Map<String, String> = mapOf(
        // App domain prefixes
        "nav" to "NAVIGATION",
        "media" to "MEDIA",
        "sys" to "SYSTEM",
        "voice" to "VOICE_CONTROL",
        "acc" to "ACCESSIBILITY",
        "text" to "TEXT",
        "input" to "INPUT",
        "appctl" to "APP_CONTROL",
        // Module domain prefixes
        "cam" to "CAMERA",
        "cockpit" to "COCKPIT",
        // Web domain prefixes
        "browser" to "BROWSER",
        "gesture" to "WEB_GESTURE"
    )

    /** Maps action_id to CommandActionType name. */
    val ACTION_MAP: Map<String, String> = mapOf(
        // ── Navigation ──
        "nav_back" to "BACK",
        "nav_home" to "HOME",
        "nav_recent" to "RECENT_APPS",
        "nav_drawer" to "APP_DRAWER",
        "nav_scroll_down" to "SCROLL_DOWN",
        "nav_scroll_up" to "SCROLL_UP",
        "nav_scroll_left" to "SCROLL_LEFT",
        "nav_scroll_right" to "SCROLL_RIGHT",
        // ── Media ──
        "media_play" to "MEDIA_PLAY",
        "media_pause" to "MEDIA_PAUSE",
        "media_next" to "MEDIA_NEXT",
        "media_prev" to "MEDIA_PREVIOUS",
        "media_vol_up" to "VOLUME_UP",
        "media_vol_down" to "VOLUME_DOWN",
        "media_mute" to "VOLUME_MUTE",
        // ── System ──
        "sys_settings" to "OPEN_SETTINGS",
        "sys_notifications" to "NOTIFICATIONS",
        "sys_clear_notif" to "CLEAR_NOTIFICATIONS",
        "sys_screenshot" to "SCREENSHOT",
        "sys_flash_on" to "FLASHLIGHT_ON",
        "sys_flash_off" to "FLASHLIGHT_OFF",
        "sys_bright_up" to "BRIGHTNESS_UP",
        "sys_bright_down" to "BRIGHTNESS_DOWN",
        "sys_lock" to "LOCK_SCREEN",
        "sys_rotate" to "ROTATE_SCREEN",
        "sys_wifi" to "TOGGLE_WIFI",
        "sys_bluetooth" to "TOGGLE_BLUETOOTH",
        // ── Voice Control ──
        "voice_mute" to "VOICE_MUTE",
        "voice_wake" to "VOICE_WAKE",
        "voice_dict_start" to "DICTATION_START",
        "voice_dict_stop" to "DICTATION_STOP",
        "voice_help" to "SHOW_COMMANDS",
        "voice_num_on" to "NUMBERS_ON",
        "voice_num_off" to "NUMBERS_OFF",
        "voice_num_auto" to "NUMBERS_AUTO",
        "voice_cursor_show" to "CURSOR_SHOW",
        "voice_cursor_hide" to "CURSOR_HIDE",
        "voice_cursor_click" to "CURSOR_CLICK",
        // ── Accessibility ──
        "acc_click" to "CLICK",
        "acc_long_click" to "LONG_CLICK",
        "acc_zoom_in" to "ZOOM_IN",
        "acc_zoom_out" to "ZOOM_OUT",
        "acc_read" to "READ_SCREEN",
        "acc_stop_read" to "STOP_READING",
        "acc_double_tap" to "DOUBLE_CLICK",
        "acc_swipe_left" to "SWIPE_LEFT",
        "acc_swipe_right" to "SWIPE_RIGHT",
        "acc_swipe_up" to "SWIPE_UP",
        "acc_swipe_down" to "SWIPE_DOWN",
        "acc_stroke_start" to "STROKE_START",
        "acc_stroke_end" to "STROKE_END",
        "acc_erase" to "ERASE",
        // ── Text ──
        "text_select_all" to "SELECT_ALL",
        "text_copy" to "COPY",
        "text_paste" to "PASTE",
        "text_cut" to "CUT",
        "text_undo" to "UNDO",
        "text_redo" to "REDO",
        "text_delete" to "DELETE",
        // ── Input ──
        "input_show_kb" to "SHOW_KEYBOARD",
        "input_hide_kb" to "HIDE_KEYBOARD",
        // ── App Control ──
        "appctl_close" to "CLOSE_APP",
        // ── Camera (PhotoAvanue) ──
        "cam_open" to "OPEN_MODULE",
        "cam_capture" to "CAPTURE_PHOTO",
        "cam_record_start" to "RECORD_START",
        "cam_record_stop" to "RECORD_STOP",
        "cam_record_pause" to "RECORD_PAUSE",
        "cam_record_resume" to "RECORD_RESUME",
        "cam_flip" to "SWITCH_LENS",
        "cam_flash_on" to "FLASH_ON",
        "cam_flash_off" to "FLASH_OFF",
        "cam_flash_auto" to "FLASH_AUTO",
        "cam_flash_torch" to "FLASH_TORCH",
        "cam_zoom_in" to "ZOOM_IN",
        "cam_zoom_out" to "ZOOM_OUT",
        "cam_exposure_up" to "EXPOSURE_UP",
        "cam_exposure_down" to "EXPOSURE_DOWN",
        "cam_mode_photo" to "MODE_PHOTO",
        "cam_mode_video" to "MODE_VIDEO",
        // ── Cockpit ──
        "cockpit_open" to "OPEN_MODULE",
        "cockpit_add_frame" to "ADD_FRAME",
        "cockpit_layout" to "LAYOUT_PICKER",
        "cockpit_layout_grid" to "LAYOUT_GRID",
        "cockpit_layout_split" to "LAYOUT_SPLIT",
        "cockpit_layout_freeform" to "LAYOUT_FREEFORM",
        "cockpit_layout_fullscreen" to "LAYOUT_FULLSCREEN",
        "cockpit_layout_workflow" to "LAYOUT_WORKFLOW",
        "cockpit_minimize" to "MINIMIZE_FRAME",
        "cockpit_maximize" to "MAXIMIZE_FRAME",
        "cockpit_close" to "CLOSE_FRAME",
        "cockpit_add_web" to "ADD_WEB",
        "cockpit_add_camera" to "ADD_CAMERA",
        "cockpit_add_note" to "ADD_NOTE",
        "cockpit_add_pdf" to "ADD_PDF",
        "cockpit_add_image" to "ADD_IMAGE",
        "cockpit_add_video" to "ADD_VIDEO",
        "cockpit_add_whiteboard" to "ADD_WHITEBOARD",
        "cockpit_add_terminal" to "ADD_TERMINAL",
        "cockpit_web_back" to "PAGE_BACK",
        "cockpit_web_forward" to "PAGE_FORWARD",
        "cockpit_web_refresh" to "PAGE_REFRESH",
        "cockpit_scroll_up" to "SCROLL_UP",
        "cockpit_scroll_down" to "SCROLL_DOWN",
        "cockpit_zoom_in" to "ZOOM_IN",
        "cockpit_zoom_out" to "ZOOM_OUT",
        // ── Browser ──
        "browser_retrain" to "RETRAIN_PAGE",
        "browser_back" to "PAGE_BACK",
        "browser_forward" to "PAGE_FORWARD",
        "browser_refresh" to "PAGE_REFRESH",
        "browser_top" to "SCROLL_TO_TOP",
        "browser_bottom" to "SCROLL_TO_BOTTOM",
        "browser_tab_next" to "TAB_NEXT",
        "browser_tab_prev" to "TAB_PREV",
        "browser_submit" to "SUBMIT_FORM",
        "browser_swipe_left" to "SWIPE_LEFT",
        "browser_swipe_right" to "SWIPE_RIGHT",
        "browser_swipe_up" to "SWIPE_UP",
        "browser_swipe_down" to "SWIPE_DOWN",
        "browser_grab" to "GRAB",
        "browser_release" to "RELEASE",
        "browser_rotate_left" to "ROTATE",
        "browser_rotate_right" to "ROTATE",
        "browser_double_tap" to "DOUBLE_CLICK",
        "browser_zoom_in" to "ZOOM_IN",
        "browser_zoom_out" to "ZOOM_OUT",
        "browser_scroll_up" to "SCROLL_UP",
        "browser_scroll_down" to "SCROLL_DOWN",
        "browser_scroll_left" to "SCROLL_LEFT",
        "browser_scroll_right" to "SCROLL_RIGHT",
        "browser_long_press" to "LONG_CLICK",
        "browser_tap" to "TAP",
        "browser_focus" to "FOCUS",
        // ── Web Gestures ──
        "gesture_pan" to "PAN",
        "gesture_pan_left" to "PAN",
        "gesture_pan_right" to "PAN",
        "gesture_pan_up" to "PAN",
        "gesture_pan_down" to "PAN",
        "gesture_tilt" to "TILT",
        "gesture_tilt_up" to "TILT",
        "gesture_tilt_down" to "TILT",
        "gesture_orbit" to "ORBIT",
        "gesture_orbit_left" to "ORBIT",
        "gesture_orbit_right" to "ORBIT",
        "gesture_rotate_x" to "ROTATE_X",
        "gesture_rotate_y" to "ROTATE_Y",
        "gesture_rotate_z" to "ROTATE_Z",
        "gesture_pinch_in" to "PINCH",
        "gesture_pinch_out" to "PINCH",
        "gesture_fling_up" to "FLING",
        "gesture_fling_down" to "FLING",
        "gesture_fling_left" to "FLING",
        "gesture_fling_right" to "FLING",
        "gesture_throw" to "THROW",
        "gesture_scale_up" to "SCALE",
        "gesture_scale_down" to "SCALE",
        "gesture_reset_zoom" to "RESET_ZOOM",
        "gesture_select_word" to "SELECT_WORD",
        "gesture_clear_sel" to "CLEAR_SELECTION",
        "gesture_hover_out" to "HOVER_OUT",
        "gesture_hover" to "HOVER",
        "gesture_drag_left" to "DRAG",
        "gesture_drag_right" to "DRAG",
        "gesture_drag_up" to "DRAG",
        "gesture_drag_down" to "DRAG",
        "gesture_stroke_start" to "STROKE_START",
        "gesture_stroke_end" to "STROKE_END",
        "gesture_erase" to "ERASE"
    )

    /** Maps action_id to metadata JSON string. Only entries with metadata are included. */
    val META_MAP: Map<String, String> = mapOf(
        "browser_rotate_left" to """{"direction":"left"}""",
        "browser_rotate_right" to """{"direction":"right"}""",
        "gesture_pan_left" to """{"direction":"left"}""",
        "gesture_pan_right" to """{"direction":"right"}""",
        "gesture_pan_up" to """{"direction":"up"}""",
        "gesture_pan_down" to """{"direction":"down"}""",
        "gesture_tilt_up" to """{"direction":"up"}""",
        "gesture_tilt_down" to """{"direction":"down"}""",
        "gesture_orbit_left" to """{"direction":"left"}""",
        "gesture_orbit_right" to """{"direction":"right"}""",
        "gesture_pinch_in" to """{"scale":"0.5"}""",
        "gesture_pinch_out" to """{"scale":"2.0"}""",
        "gesture_fling_up" to """{"direction":"up"}""",
        "gesture_fling_down" to """{"direction":"down"}""",
        "gesture_fling_left" to """{"direction":"left"}""",
        "gesture_fling_right" to """{"direction":"right"}""",
        "gesture_scale_up" to """{"factor":"1.5"}""",
        "gesture_scale_down" to """{"factor":"0.67"}""",
        "gesture_drag_left" to """{"direction":"left"}""",
        "gesture_drag_right" to """{"direction":"right"}""",
        "gesture_drag_up" to """{"direction":"up"}""",
        "gesture_drag_down" to """{"direction":"down"}"""
    )

    // ═══════════════════════════════════════════════════════════════════
    // Public API
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Parse VOS file contents into a list of [VosParsedCommand].
     * Auto-detects format: JSON v2.1 or compact v3.0.
     *
     * @param content Raw VOS file contents
     * @param isFallback Whether this is the fallback locale (en-US)
     * @return [VosParseResult] with parsed commands or error
     */
    fun parse(content: String, isFallback: Boolean = false): VosParseResult {
        val trimmed = content.trimStart()
        return when {
            trimmed.startsWith("{") -> parseJson(trimmed, isFallback)
            trimmed.startsWith("#") || trimmed.startsWith("VOS:") -> parseCompact(trimmed, isFallback)
            else -> VosParseResult.Error("Unknown VOS format: expected '{' (JSON) or '#'/'VOS:' (compact)")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // v2.1 JSON Parser (backward compatible)
    // ═══════════════════════════════════════════════════════════════════

    private fun parseJson(jsonString: String, isFallback: Boolean): VosParseResult {
        return try {
            val root = json.parseToJsonElement(jsonString).jsonObject

            val version = root["version"]?.jsonPrimitive?.content ?: "1.0"
            val locale = root["locale"]?.jsonPrimitive?.content ?: "en-US"
            val domain = root["domain"]?.jsonPrimitive?.content ?: "app"

            // v2.0+: Explicit mapping tables from file
            val categoryMap = root["category_map"]?.jsonObject
            val actionMap = root["action_map"]?.jsonObject
            val metaMap = root["meta_map"]?.jsonObject

            val commandsArray = root["commands"]?.jsonArray
                ?: return VosParseResult.Error("Missing 'commands' array")

            val commands = parseJsonCommandsArray(
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
            VosParseResult.Error("Failed to parse VOS JSON: ${e.message}")
        }
    }

    private fun parseJsonCommandsArray(
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
                val category = resolveCategoryFromJson(actionId, categoryMap)
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

    private fun resolveCategoryFromJson(actionId: String, categoryMap: JsonObject?): String {
        val prefix = actionId.substringBefore("_", "unknown")
        if (categoryMap != null) {
            val mapped = categoryMap[prefix]?.jsonPrimitive?.content
            if (!mapped.isNullOrEmpty()) return mapped
        }
        return prefix
    }

    // ═══════════════════════════════════════════════════════════════════
    // v3.0 Compact Parser
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Parse v3.0 compact format.
     *
     * Format:
     * ```
     * # Comment
     * VOS:3.0:en-US:en-US:app
     * action_id|primary_text|synonym1,synonym2|description
     * ```
     *
     * Maps are resolved from compiled constants [CATEGORY_MAP], [ACTION_MAP], [META_MAP].
     */
    private fun parseCompact(content: String, isFallback: Boolean): VosParseResult {
        return try {
            val lines = content.lines()
            var version = "3.0"
            var locale = "en-US"
            var fallback = "en-US"
            var domain = "app"
            var headerFound = false
            val commands = mutableListOf<VosParsedCommand>()

            for (line in lines) {
                val trimmed = line.trim()

                // Skip empty lines and comments
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

                // Parse header line: VOS:version:locale:fallback:domain
                if (trimmed.startsWith("VOS:")) {
                    val parts = trimmed.split(":")
                    if (parts.size >= 5) {
                        version = parts[1]
                        locale = parts[2]
                        fallback = parts[3]
                        domain = parts[4]
                        headerFound = true
                    }
                    continue
                }

                if (!headerFound) continue

                // Parse command line: action_id|primary_text|synonyms_csv|description
                val parts = trimmed.split("|")
                if (parts.size < 4) continue

                val actionId = parts[0].trim()
                val primaryText = parts[1].trim()
                val synonymsCsv = parts[2].trim()
                val description = parts[3].trim()

                val synonyms = if (synonymsCsv.isEmpty()) {
                    emptyList()
                } else {
                    synonymsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                }

                val prefix = actionId.substringBefore("_", "unknown")
                val category = CATEGORY_MAP[prefix] ?: prefix
                val actionType = ACTION_MAP[actionId] ?: ""
                val metadata = META_MAP[actionId] ?: ""

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
            }

            if (!headerFound) {
                return VosParseResult.Error("Missing VOS header line (expected VOS:version:locale:fallback:domain)")
            }

            VosParseResult.Success(
                commands = commands,
                locale = locale,
                version = version,
                domain = domain
            )
        } catch (e: Exception) {
            VosParseResult.Error("Failed to parse VOS compact format: ${e.message}")
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // Utilities
    // ═══════════════════════════════════════════════════════════════════

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
