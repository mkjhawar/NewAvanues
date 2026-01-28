/**
 * JsonUtils.kt - Cross-platform JSON utilities
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-11-17
 */
package com.augmentalis.voiceoscore

/**
 * Cross-platform JSON utilities
 *
 * Provides basic JSON manipulation without external dependencies.
 * For complex JSON operations, use kotlinx.serialization or platform-specific libraries.
 */
object JsonUtils {

    /**
     * Escape a string for use in JSON
     *
     * @param value The string to escape
     * @return JSON-escaped string
     */
    fun escapeJsonString(value: String): String {
        return value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\b", "\\b")
            .replace("\u000C", "\\f")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    /**
     * Quote a string for JSON
     *
     * @param value The string to quote
     * @return Quoted JSON string
     */
    fun quoteJsonString(value: String): String {
        return "\"${escapeJsonString(value)}\""
    }

    /**
     * Create a simple JSON object from key-value pairs
     *
     * @param pairs Key-value pairs
     * @return JSON object string
     */
    fun createJsonObject(vararg pairs: Pair<String, Any?>): String {
        val entries = pairs.joinToString(",\n  ") { (key, value) ->
            "${quoteJsonString(key)}: ${toJsonValue(value)}"
        }
        return "{\n  $entries\n}"
    }

    /**
     * Create a JSON array from values
     *
     * @param values Array values
     * @return JSON array string
     */
    fun createJsonArray(vararg values: Any?): String {
        val entries = values.joinToString(", ") { toJsonValue(it) }
        return "[$entries]"
    }

    /**
     * Convert a value to JSON representation
     *
     * @param value The value to convert
     * @return JSON string representation
     */
    fun toJsonValue(value: Any?): String {
        return when (value) {
            null -> "null"
            is String -> quoteJsonString(value)
            is Number -> value.toString()
            is Boolean -> value.toString()
            is List<*> -> createJsonArray(*value.toTypedArray())
            is Map<*, *> -> {
                val pairs = value.entries.map { (k, v) ->
                    k.toString() to v
                }.toTypedArray()
                createJsonObject(*pairs)
            }
            else -> quoteJsonString(value.toString())
        }
    }

    /**
     * Pretty print JSON string with basic formatting
     *
     * @param json The JSON string
     * @param indent The indentation string (default 2 spaces)
     * @return Pretty-printed JSON
     */
    fun prettyPrint(json: String, indent: String = "  "): String {
        val result = StringBuilder()
        var level = 0
        var inQuotes = false
        var escape = false

        for (char in json) {
            when {
                escape -> {
                    result.append(char)
                    escape = false
                }
                char == '\\' -> {
                    result.append(char)
                    escape = true
                }
                char == '"' -> {
                    result.append(char)
                    inQuotes = !inQuotes
                }
                !inQuotes -> {
                    when (char) {
                        '{', '[' -> {
                            result.append(char)
                            result.append('\n')
                            level++
                            result.append(indent.repeat(level))
                        }
                        '}', ']' -> {
                            result.append('\n')
                            level--
                            result.append(indent.repeat(level))
                            result.append(char)
                        }
                        ',' -> {
                            result.append(char)
                            result.append('\n')
                            result.append(indent.repeat(level))
                        }
                        ':' -> {
                            result.append(": ")
                        }
                        ' ', '\n', '\r', '\t' -> {
                            // Skip whitespace outside quotes
                        }
                        else -> result.append(char)
                    }
                }
                else -> result.append(char)
            }
        }

        return result.toString()
    }
}

/**
 * Extension function to convert Map to JSON
 */
fun Map<String, Any?>.toJson(): String {
    return JsonUtils.createJsonObject(*this.toList().toTypedArray())
}

/**
 * Extension function to convert List to JSON array
 */
fun List<Any?>.toJsonArray(): String {
    return JsonUtils.createJsonArray(*this.toTypedArray())
}