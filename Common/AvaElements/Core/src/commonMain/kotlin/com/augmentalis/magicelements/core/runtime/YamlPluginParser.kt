package com.augmentalis.avaelements.core.runtime

import com.augmentalis.avaelements.core.*

/**
 * YAML Plugin Parser
 *
 * Parses plugin definitions from YAML format by converting to JSON
 * and delegating to JsonPluginParser.
 *
 * Supported YAML format:
 * ```yaml
 * metadata:
 *   id: "my-plugin"
 *   name: "My Plugin"
 *   version: "1.0.0"
 *   author: "Author Name"
 *   description: "Plugin description"
 *   minSdkVersion: 1
 *   permissions: []
 *   dependencies: []
 *
 * components:
 *   - type: "CustomButton"
 *     schema:
 *       properties:
 *         label:
 *           type: STRING
 *           description: "Button label"
 *       requiredProperties:
 *         - label
 *     defaultProps:
 *       variant: "filled"
 *
 * themes:
 *   - id: "custom-theme"
 *     name: "Custom Theme"
 *     colors:
 *       primary: "#007AFF"
 * ```
 */
class YamlPluginParser {

    private val jsonParser = JsonPluginParser()

    /**
     * Parse YAML plugin definition
     *
     * @param yaml YAML string
     * @return Result containing plugin or error
     */
    fun parse(yaml: String): Result<MagicElementPlugin> {
        return try {
            val json = yamlToJson(yaml)
            jsonParser.parse(json)
        } catch (e: Exception) {
            Result.failure(PluginException.LoadException("Failed to parse YAML plugin: ${e.message}", e))
        }
    }

    /**
     * Convert YAML to JSON
     *
     * Simple YAML parser supporting:
     * - Key-value pairs
     * - Nested objects (indentation-based)
     * - Arrays (- prefix)
     * - Quoted and unquoted strings
     * - Numbers and booleans
     */
    private fun yamlToJson(yaml: String): String {
        val lines = yaml.lines()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() && !it.trimStart().startsWith("#") }

        if (lines.isEmpty()) {
            return "{}"
        }

        return parseYamlObject(lines, 0).first
    }

    private fun parseYamlObject(lines: List<String>, startIndex: Int): Pair<String, Int> {
        val result = StringBuilder("{")
        var index = startIndex
        var isFirst = true
        val baseIndent = getIndent(lines.getOrNull(startIndex) ?: "")

        while (index < lines.size) {
            val line = lines[index]
            val currentIndent = getIndent(line)

            // Stop if we've dedented past our base level
            if (currentIndent < baseIndent && index > startIndex) {
                break
            }

            // Skip if we're at a deeper level than expected
            if (currentIndent > baseIndent && !line.trimStart().startsWith("-")) {
                index++
                continue
            }

            val trimmed = line.trimStart()

            // Handle array item
            if (trimmed.startsWith("-")) {
                break // Arrays are handled separately
            }

            // Handle key-value pair
            val colonIndex = trimmed.indexOf(':')
            if (colonIndex > 0) {
                val key = trimmed.substring(0, colonIndex).trim()
                val valueStr = trimmed.substring(colonIndex + 1).trim()

                if (!isFirst) result.append(",")
                isFirst = false

                result.append("\"$key\":")

                if (valueStr.isEmpty()) {
                    // Check if next line is an array or object
                    val nextIndex = index + 1
                    if (nextIndex < lines.size) {
                        val nextLine = lines[nextIndex]
                        val nextTrimmed = nextLine.trimStart()
                        val nextIndent = getIndent(nextLine)

                        if (nextTrimmed.startsWith("-") && nextIndent > currentIndent) {
                            // It's an array
                            val (arrayJson, newIndex) = parseYamlArray(lines, nextIndex, nextIndent)
                            result.append(arrayJson)
                            index = newIndex
                            continue
                        } else if (nextIndent > currentIndent) {
                            // It's a nested object
                            val (objectJson, newIndex) = parseYamlObject(lines, nextIndex)
                            result.append(objectJson)
                            index = newIndex
                            continue
                        }
                    }
                    result.append("null")
                } else {
                    result.append(parseYamlValue(valueStr))
                }
            }

            index++
        }

        result.append("}")
        return Pair(result.toString(), index)
    }

    private fun parseYamlArray(lines: List<String>, startIndex: Int, baseIndent: Int): Pair<String, Int> {
        val result = StringBuilder("[")
        var index = startIndex
        var isFirst = true

        while (index < lines.size) {
            val line = lines[index]
            val currentIndent = getIndent(line)
            val trimmed = line.trimStart()

            // Stop if we've dedented
            if (currentIndent < baseIndent) {
                break
            }

            if (!trimmed.startsWith("-")) {
                // Not an array item, might be continuation
                if (currentIndent > baseIndent) {
                    index++
                    continue
                }
                break
            }

            if (!isFirst) result.append(",")
            isFirst = false

            // Get the value after the dash
            val afterDash = trimmed.substring(1).trimStart()

            if (afterDash.isEmpty()) {
                // Multi-line object
                val nextIndex = index + 1
                if (nextIndex < lines.size) {
                    val (objectJson, newIndex) = parseYamlObject(lines, nextIndex)
                    result.append(objectJson)
                    index = newIndex
                    continue
                }
                result.append("null")
            } else if (afterDash.contains(":")) {
                // Inline object starting with key
                val inlineResult = StringBuilder("{")
                val colonPos = afterDash.indexOf(':')
                val key = afterDash.substring(0, colonPos).trim()
                val value = afterDash.substring(colonPos + 1).trim()

                inlineResult.append("\"$key\":")
                if (value.isEmpty()) {
                    // Check for nested content
                    val nextIndex = index + 1
                    if (nextIndex < lines.size) {
                        val nextLine = lines[nextIndex]
                        val nextIndent = getIndent(nextLine)
                        if (nextIndent > currentIndent) {
                            val (nestedJson, newIndex) = parseYamlObject(lines, nextIndex)
                            inlineResult.append(nestedJson)

                            // Continue parsing siblings at same level
                            var siblingIndex = newIndex
                            while (siblingIndex < lines.size) {
                                val siblingLine = lines[siblingIndex]
                                val siblingIndent = getIndent(siblingLine)
                                if (siblingIndent <= currentIndent) break
                                if (siblingIndent == getIndent(lines[nextIndex])) {
                                    val siblingTrimmed = siblingLine.trimStart()
                                    val siblingColon = siblingTrimmed.indexOf(':')
                                    if (siblingColon > 0) {
                                        val siblingKey = siblingTrimmed.substring(0, siblingColon).trim()
                                        val siblingVal = siblingTrimmed.substring(siblingColon + 1).trim()
                                        inlineResult.append(",\"$siblingKey\":")
                                        if (siblingVal.isEmpty()) {
                                            val (subJson, subIndex) = parseYamlObject(lines, siblingIndex + 1)
                                            inlineResult.append(subJson)
                                            siblingIndex = subIndex
                                            continue
                                        } else {
                                            inlineResult.append(parseYamlValue(siblingVal))
                                        }
                                    }
                                }
                                siblingIndex++
                            }
                            index = siblingIndex
                            inlineResult.append("}")
                            result.append(inlineResult)
                            continue
                        }
                    }
                    inlineResult.append("null")
                } else {
                    inlineResult.append(parseYamlValue(value))
                }
                inlineResult.append("}")
                result.append(inlineResult)
            } else {
                // Simple value
                result.append(parseYamlValue(afterDash))
            }

            index++
        }

        result.append("]")
        return Pair(result.toString(), index)
    }

    private fun parseYamlValue(value: String): String {
        val trimmed = value.trim()

        // Handle quoted strings
        if ((trimmed.startsWith("\"") && trimmed.endsWith("\"")) ||
            (trimmed.startsWith("'") && trimmed.endsWith("'"))) {
            val unquoted = trimmed.substring(1, trimmed.length - 1)
            return "\"${escapeJson(unquoted)}\""
        }

        // Handle booleans
        if (trimmed.lowercase() == "true") return "true"
        if (trimmed.lowercase() == "false") return "false"

        // Handle null
        if (trimmed.lowercase() == "null" || trimmed == "~") return "null"

        // Handle numbers
        trimmed.toDoubleOrNull()?.let {
            return if (trimmed.contains('.')) trimmed else trimmed.toIntOrNull()?.toString() ?: "\"$trimmed\""
        }

        // Handle empty arrays/objects
        if (trimmed == "[]") return "[]"
        if (trimmed == "{}") return "{}"

        // Default to string
        return "\"${escapeJson(trimmed)}\""
    }

    private fun getIndent(line: String): Int {
        var count = 0
        for (char in line) {
            when (char) {
                ' ' -> count++
                '\t' -> count += 2
                else -> break
            }
        }
        return count
    }

    private fun escapeJson(s: String): String {
        return s.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }
}
