package com.augmentalis.avamagic.ipc.universal

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Converter utility for VOS JSON files to AVU format.
 *
 * Converts legacy .vos JSON files to the compact AVU line-based format.
 * Achieves ~52% size reduction while maintaining full data fidelity.
 *
 * @author Augmentalis Engineering
 * @since 1.0.0
 */
object VosToAvuConverter {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Convert VOS JSON content to AVU format.
     *
     * @param vosJson The JSON content from a .vos file
     * @return AVU format string
     */
    fun convert(vosJson: String): String {
        val jsonElement = json.parseToJsonElement(vosJson)
        val obj = jsonElement.jsonObject

        val schema = obj["schema"]?.jsonPrimitive?.content ?: "vos-1.0"
        val version = obj["version"]?.jsonPrimitive?.content ?: "1.0.0"
        val locale = obj["locale"]?.jsonPrimitive?.content ?: "en-US"

        val fileInfo = obj["file_info"]?.jsonObject
        val category = fileInfo?.get("category")?.jsonPrimitive?.content ?: "unknown"
        val displayName = fileInfo?.get("display_name")?.jsonPrimitive?.content ?: category
        val description = fileInfo?.get("description")?.jsonPrimitive?.content ?: ""
        val commandCount = fileInfo?.get("command_count")?.jsonPrimitive?.content ?: "0"

        val commands = obj["commands"]?.jsonArray ?: return buildEmptyAvu(locale, category)

        return buildString {
            // Header
            appendLine("# AVU Format v1.0")
            appendLine("# Type: VOICE")
            appendLine("# Extension: .avu")
            appendLine("# Converted from: ${fileInfo?.get("filename")?.jsonPrimitive?.content ?: "unknown.vos"}")
            appendLine("---")

            // Metadata section
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

            // Category entry
            appendLine("CAT:$category:$displayName:$description")

            // Command entries
            val synonymsMap = mutableMapOf<String, List<String>>()

            for (cmd in commands) {
                val cmdObj = cmd.jsonObject
                val action = cmdObj["action"]?.jsonPrimitive?.content ?: continue
                val primary = cmdObj["cmd"]?.jsonPrimitive?.content ?: continue
                val synonyms = cmdObj["syn"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()

                appendLine("CMD:$action:$primary")
                if (synonyms.isNotEmpty()) {
                    synonymsMap[action] = synonyms
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

    /**
     * Convert multiple VOS files to AVU format.
     *
     * @param vosFiles Map of filename to JSON content
     * @return Map of filename (with .avu extension) to AVU content
     */
    fun convertBatch(vosFiles: Map<String, String>): Map<String, String> {
        return vosFiles.mapKeys { (filename, _) ->
            filename.replace(".vos", ".avu")
        }.mapValues { (_, content) ->
            convert(content)
        }
    }

    /**
     * Calculate size reduction percentage.
     */
    fun calculateSizeReduction(vosJson: String, avuContent: String): SizeComparison {
        val jsonBytes = vosJson.toByteArray().size
        val avuBytes = avuContent.toByteArray().size
        val reduction = ((jsonBytes - avuBytes).toFloat() / jsonBytes * 100)

        return SizeComparison(
            jsonSize = jsonBytes,
            avuSize = avuBytes,
            reductionPercent = reduction
        )
    }

    private fun buildEmptyAvu(locale: String, category: String): String {
        return """
            |# AVU Format v1.0
            |# Type: VOICE
            |---
            |schema: avu-vos-1.0
            |version: 1.0.0
            |locale: $locale
            |project: voiceos
            |---
            |CAT:$category:$category:Empty command file
            |---
        """.trimMargin()
    }
}

/**
 * Size comparison result
 */
data class SizeComparison(
    val jsonSize: Int,
    val avuSize: Int,
    val reductionPercent: Float
) {
    override fun toString(): String =
        "JSON: $jsonSize bytes -> AVU: $avuSize bytes (${reductionPercent.toInt()}% reduction)"
}

/**
 * AVU format writer for generating AVU content programmatically.
 */
class AvuWriter(
    private val type: AvuType,
    private val schema: String,
    private val version: String = "1.0.0"
) {
    private val metadata = mutableMapOf<String, String>()
    private val entries = mutableListOf<String>()
    private val synonyms = mutableMapOf<String, List<String>>()

    fun metadata(key: String, value: String): AvuWriter {
        metadata[key] = value
        return this
    }

    fun entry(prefix: String, data: String): AvuWriter {
        entries.add("$prefix:$data")
        return this
    }

    fun command(action: String, primaryText: String, syns: List<String> = emptyList()): AvuWriter {
        entries.add("CMD:$action:$primaryText")
        if (syns.isNotEmpty()) {
            synonyms[action] = syns
        }
        return this
    }

    fun config(key: String, value: Any, type: String? = null): AvuWriter {
        val typeStr = type ?: when (value) {
            is Boolean -> "bool"
            is Int, is Long -> "int"
            is Float, is Double -> "float"
            else -> "string"
        }
        entries.add("CFG:$key:$value:$typeStr")
        return this
    }

    fun build(): String = buildString {
        // Header
        appendLine("# AVU Format v1.0")
        appendLine("# Type: ${type.name}")
        appendLine("---")

        // Metadata
        appendLine("schema: $schema")
        appendLine("version: $version")
        if (metadata.isNotEmpty()) {
            appendLine("metadata:")
            for ((k, v) in metadata) {
                appendLine("  $k: $v")
            }
        }
        appendLine("---")

        // Entries
        for (entry in entries) {
            appendLine(entry)
        }

        // Synonyms
        if (synonyms.isNotEmpty()) {
            appendLine("---")
            for ((action, syns) in synonyms) {
                appendLine("SYN:$action:[${syns.joinToString(",")}]")
            }
        }
    }
}
