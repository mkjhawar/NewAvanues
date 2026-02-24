/**
 * JsonConverters.kt - Specialized JSON converters
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-17
 */
package com.augmentalis.voiceoscore

/**
 * Specialized JSON converters for common data types
 */
object JsonConverters {

    /**
     * Convert Rect-like bounds to JSON
     *
     * @param left Left coordinate
     * @param top Top coordinate
     * @param right Right coordinate
     * @param bottom Bottom coordinate
     * @return JSON string representation of bounds
     */
    fun boundsToJson(left: Int, top: Int, right: Int, bottom: Int): String {
        return JsonUtils.createJsonObject(
            "left" to left,
            "top" to top,
            "right" to right,
            "bottom" to bottom
        )
    }

    /**
     * Convert point to JSON
     *
     * @param x X coordinate
     * @param y Y coordinate
     * @return JSON string representation of point
     */
    fun pointToJson(x: Int, y: Int): String {
        return JsonUtils.createJsonObject(
            "x" to x,
            "y" to y
        )
    }

    /**
     * Convert size to JSON
     *
     * @param width Width value
     * @param height Height value
     * @return JSON string representation of size
     */
    fun sizeToJson(width: Int, height: Int): String {
        return JsonUtils.createJsonObject(
            "width" to width,
            "height" to height
        )
    }

    /**
     * Parse synonyms from JSON array string
     *
     * Simple parser that extracts string values from a JSON array.
     * For complex parsing, use a full JSON library.
     *
     * @param jsonArray JSON array string like ["word1", "word2"]
     * @return List of strings
     */
    fun parseSynonyms(jsonArray: String): List<String> {
        val trimmed = jsonArray.trim()
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return emptyList()
        }

        val content = trimmed.substring(1, trimmed.length - 1)
        if (content.isBlank()) {
            return emptyList()
        }

        return content.split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { item ->
                if (item.startsWith("\"") && item.endsWith("\"")) {
                    item.substring(1, item.length - 1)
                } else {
                    item
                }
            }
    }

    /**
     * Create action JSON for UI commands
     *
     * @param action Action type
     * @param target Target element
     * @param params Additional parameters
     * @return JSON string for action
     */
    fun createActionJson(
        action: String,
        target: String? = null,
        params: Map<String, Any?>? = null
    ): String {
        val pairs = mutableListOf<Pair<String, Any?>>()
        pairs.add("action" to action)

        target?.let { pairs.add("target" to it) }
        params?.let { pairs.add("params" to it) }

        return JsonUtils.createJsonObject(*pairs.toTypedArray())
    }

    /**
     * Create command JSON
     *
     * @param command Command text
     * @param type Command type
     * @param metadata Additional metadata
     * @return JSON string for command
     */
    fun createCommandJson(
        command: String,
        type: String? = null,
        metadata: Map<String, Any?>? = null
    ): String {
        val pairs = mutableListOf<Pair<String, Any?>>()
        pairs.add("command" to command)

        type?.let { pairs.add("type" to it) }
        metadata?.let { pairs.add("metadata" to it) }

        return JsonUtils.createJsonObject(*pairs.toTypedArray())
    }
}