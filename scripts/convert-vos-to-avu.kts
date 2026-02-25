#!/usr/bin/env kotlin

/**
 * Batch VOS to AVU Conversion Script
 *
 * Converts all .vos JSON files to .avu format.
 * Run from project root: kotlin scripts/convert-vos-to-avu.kts
 *
 * @author Augmentalis Engineering
 * @since 2026-01-15
 */

import java.io.File
import kotlin.system.exitProcess

// Configuration
val PROJECT_ROOT = File(".").canonicalFile
val COMMANDS_DIRS = listOf(
    "Modules/VoiceOSCore/src/commonMain/resources/commands",
    "Modules/VoiceOS/managers/CommandManager/src/main/assets/commands"
)
val LOCALES = listOf("en-US", "es-ES", "de-DE", "fr-FR")

data class ConversionStats(
    var totalFiles: Int = 0,
    var converted: Int = 0,
    var skipped: Int = 0,
    var errors: Int = 0,
    var totalJsonBytes: Long = 0,
    var totalAvuBytes: Long = 0
)

fun main() {
    println("=".repeat(60))
    println("VOS to AVU Batch Converter")
    println("=".repeat(60))
    println()

    val stats = ConversionStats()

    for (baseDir in COMMANDS_DIRS) {
        val commandsDir = File(PROJECT_ROOT, baseDir)
        if (!commandsDir.exists()) {
            println("Directory not found: $baseDir")
            continue
        }

        println("Processing: $baseDir")

        for (locale in LOCALES) {
            val localeDir = File(commandsDir, locale)
            if (!localeDir.exists()) continue

            val vosFiles = localeDir.listFiles { f -> f.extension == "vos" } ?: continue

            for (vosFile in vosFiles) {
                stats.totalFiles++
                val avuFile = File(vosFile.parentFile, vosFile.nameWithoutExtension + ".avu")

                if (avuFile.exists()) {
                    println("  [SKIP] ${vosFile.name} (AVU exists)")
                    stats.skipped++
                    continue
                }

                try {
                    val jsonContent = vosFile.readText()
                    val avuContent = convertVosToAvu(jsonContent, vosFile.name)

                    avuFile.writeText(avuContent)

                    val jsonSize = jsonContent.length
                    val avuSize = avuContent.length
                    val reduction = ((jsonSize - avuSize).toFloat() / jsonSize * 100).toInt()

                    stats.totalJsonBytes += jsonSize
                    stats.totalAvuBytes += avuSize
                    stats.converted++

                    println("  [OK] ${vosFile.name} -> ${avuFile.name} ($reduction% smaller)")

                } catch (e: Exception) {
                    println("  [ERR] ${vosFile.name}: ${e.message}")
                    stats.errors++
                }
            }
        }
        println()
    }

    // Summary
    println("=".repeat(60))
    println("Conversion Summary")
    println("=".repeat(60))
    println("Total files:    ${stats.totalFiles}")
    println("Converted:      ${stats.converted}")
    println("Skipped:        ${stats.skipped}")
    println("Errors:         ${stats.errors}")

    if (stats.totalJsonBytes > 0) {
        val overallReduction = ((stats.totalJsonBytes - stats.totalAvuBytes).toFloat() / stats.totalJsonBytes * 100).toInt()
        println()
        println("Total JSON size: ${stats.totalJsonBytes / 1024} KB")
        println("Total AVU size:  ${stats.totalAvuBytes / 1024} KB")
        println("Overall reduction: $overallReduction%")
    }

    exitProcess(if (stats.errors > 0) 1 else 0)
}

fun convertVosToAvu(jsonContent: String, filename: String): String {
    // Simple JSON parsing without external dependencies
    val json = parseSimpleJson(jsonContent)

    val schema = json["schema"] as? String ?: "vos-1.0"
    val version = json["version"] as? String ?: "1.0.0"
    val locale = json["locale"] as? String ?: "en-US"

    @Suppress("UNCHECKED_CAST")
    val fileInfo = json["file_info"] as? Map<String, Any> ?: emptyMap()
    val category = fileInfo["category"] as? String ?: "unknown"
    val displayName = fileInfo["display_name"] as? String ?: category
    val description = fileInfo["description"] as? String ?: ""
    val commandCount = fileInfo["command_count"]?.toString() ?: "0"

    @Suppress("UNCHECKED_CAST")
    val commands = json["commands"] as? List<Map<String, Any>> ?: emptyList()

    return buildString {
        // Header
        appendLine("# AVU Format v1.0")
        appendLine("# Type: VOICE")
        appendLine("# Extension: .avu")
        appendLine("# Converted from: $filename")
        appendLine("---")

        // Metadata
        appendLine("schema: avu-vos-1.0")
        appendLine("version: $version")
        appendLine("locale: $locale")
        appendLine("project: voiceos")
        appendLine("metadata:")
        appendLine("  category: $category")
        appendLine("  display_name: $displayName")
        appendLine("  description: $description")
        appendLine("  command_count: $commandCount")
        appendLine("---")

        // Category
        appendLine("CAT:$category:$displayName:$description")

        // Commands
        val synonymsMap = mutableMapOf<String, List<String>>()

        for (cmd in commands) {
            val action = cmd["action"] as? String ?: continue
            val primary = cmd["cmd"] as? String ?: continue
            @Suppress("UNCHECKED_CAST")
            val syns = cmd["syn"] as? List<String> ?: emptyList()

            appendLine("CMD:$action:$primary")
            if (syns.isNotEmpty()) {
                synonymsMap[action] = syns
            }
        }

        // Synonyms section
        if (synonymsMap.isNotEmpty()) {
            appendLine("---")
            for ((action, syns) in synonymsMap) {
                appendLine("SYN:$action:[${syns.joinToString(",")}]")
            }
        }
    }
}

// Simple JSON parser (no external dependencies)
fun parseSimpleJson(json: String): Map<String, Any> {
    val result = mutableMapOf<String, Any>()
    var i = json.indexOf('{') + 1

    while (i < json.length) {
        // Skip whitespace
        while (i < json.length && json[i].isWhitespace()) i++
        if (i >= json.length || json[i] == '}') break

        // Parse key
        if (json[i] == '"') {
            val keyEnd = json.indexOf('"', i + 1)
            val key = json.substring(i + 1, keyEnd)
            i = keyEnd + 1

            // Skip to colon
            while (i < json.length && json[i] != ':') i++
            i++

            // Skip whitespace
            while (i < json.length && json[i].isWhitespace()) i++

            // Parse value
            val (value, newIndex) = parseValue(json, i)
            result[key] = value
            i = newIndex
        }

        // Skip comma
        while (i < json.length && (json[i] == ',' || json[i].isWhitespace())) i++
    }

    return result
}

fun parseValue(json: String, start: Int): Pair<Any, Int> {
    var i = start
    while (i < json.length && json[i].isWhitespace()) i++

    return when {
        json[i] == '"' -> {
            val end = findStringEnd(json, i + 1)
            val value = json.substring(i + 1, end).replace("\\\"", "\"")
            value to (end + 1)
        }
        json[i] == '{' -> {
            val end = findMatchingBrace(json, i, '{', '}')
            val inner = json.substring(i, end + 1)
            parseSimpleJson(inner) to (end + 1)
        }
        json[i] == '[' -> {
            val end = findMatchingBrace(json, i, '[', ']')
            val inner = json.substring(i + 1, end)
            parseArray(inner) to (end + 1)
        }
        json.substring(i).startsWith("true") -> true to (i + 4)
        json.substring(i).startsWith("false") -> false to (i + 5)
        json.substring(i).startsWith("null") -> "null" to (i + 4)
        else -> {
            // Number
            var end = i
            while (end < json.length && (json[end].isDigit() || json[end] == '.' || json[end] == '-')) end++
            val numStr = json.substring(i, end)
            val num = numStr.toDoubleOrNull() ?: numStr.toIntOrNull() ?: 0
            num to end
        }
    }
}

fun findStringEnd(json: String, start: Int): Int {
    var i = start
    while (i < json.length) {
        if (json[i] == '"' && json[i - 1] != '\\') return i
        i++
    }
    return json.length
}

fun findMatchingBrace(json: String, start: Int, open: Char, close: Char): Int {
    var depth = 1
    var i = start + 1
    var inString = false

    while (i < json.length && depth > 0) {
        when {
            json[i] == '"' && json[i - 1] != '\\' -> inString = !inString
            !inString && json[i] == open -> depth++
            !inString && json[i] == close -> depth--
        }
        i++
    }
    return i - 1
}

fun parseArray(content: String): List<Any> {
    val result = mutableListOf<Any>()
    var i = 0

    while (i < content.length) {
        while (i < content.length && (content[i].isWhitespace() || content[i] == ',')) i++
        if (i >= content.length) break

        val (value, newIndex) = parseValue(content, i)
        result.add(value)
        i = newIndex
    }

    return result
}

// Run main
main()
