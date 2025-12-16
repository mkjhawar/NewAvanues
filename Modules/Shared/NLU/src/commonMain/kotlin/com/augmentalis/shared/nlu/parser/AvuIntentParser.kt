/**
 * AvuIntentParser - Parse AVU .aai files into UnifiedIntent objects
 *
 * Implements the AVU (Avanues Universal Format) parser for NLU-specific
 * IPC codes: INT, PAT, SYN, EMB, ACT.
 *
 * Created: 2025-12-07
 */

package com.augmentalis.shared.nlu.parser

import com.augmentalis.shared.nlu.model.IntentSource
import com.augmentalis.shared.nlu.model.UnifiedIntent
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Parser for AVU .aai (AVA AI Intent) files.
 *
 * File format:
 * ```
 * # Avanues Universal Format v1.0
 * ---
 * schema: avu-1.0
 * version: 1.0.0
 * locale: en-US
 * project: shared
 * metadata:
 *   file: navigation.aai
 *   category: nlu_intents
 * ---
 * INT:nav_back:go back:navigation:10:GLOBAL_ACTION_BACK
 * PAT:nav_back:go back
 * PAT:nav_back:navigate back
 * SYN:nav_back:return
 * EMB:nav_back:mobilebert-384:384:base64encodedvector
 * ---
 * synonyms:
 *   back: [return, previous]
 * ```
 */
class AvuIntentParser {

    /**
     * Parse AVU content into a list of UnifiedIntent objects
     *
     * @param content Raw AVU file content
     * @return List of parsed intents
     */
    fun parse(content: String): ParseResult {
        val sections = content.split("---").map { it.trim() }

        if (sections.size < 3) {
            return ParseResult(
                intents = emptyList(),
                errors = listOf("Invalid AVU format: expected at least 3 sections separated by ---")
            )
        }

        // Parse header section
        val header = parseHeader(sections[1])

        // Parse data section
        val dataSection = sections[2]
        val builders = mutableMapOf<String, IntentBuilder>()
        val errors = mutableListOf<String>()

        dataSection.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .forEach { line ->
                try {
                    parseLine(line, builders, header)
                } catch (e: Exception) {
                    errors.add("Error parsing line '$line': ${e.message}")
                }
            }

        // Build intents from builders
        val intents = builders.values.mapNotNull { builder ->
            try {
                builder.build(header.locale, header.source)
            } catch (e: Exception) {
                errors.add("Error building intent '${builder.id}': ${e.message}")
                null
            }
        }

        return ParseResult(intents, errors)
    }

    /**
     * Parse the header section to extract metadata
     */
    private fun parseHeader(headerSection: String): AvuHeader {
        var locale = "en-US"
        var source = IntentSource.CORE

        headerSection.lines().forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("locale:") -> {
                    locale = trimmed.substringAfter(":").trim()
                }
                trimmed.startsWith("project:") -> {
                    source = when (trimmed.substringAfter(":").trim().lowercase()) {
                        "voiceos" -> IntentSource.VOICEOS
                        "ava" -> IntentSource.AVA
                        "user" -> IntentSource.USER
                        else -> IntentSource.CORE
                    }
                }
            }
        }

        return AvuHeader(locale, source)
    }

    /**
     * Parse a single data line based on IPC code
     */
    private fun parseLine(line: String, builders: MutableMap<String, IntentBuilder>, header: AvuHeader) {
        val parts = line.split(":")
        if (parts.size < 2) return

        val code = parts[0].uppercase()
        val intentId = parts.getOrNull(1) ?: return

        when (code) {
            "INT" -> {
                // INT:id:canonical:category:priority:action
                if (parts.size >= 6) {
                    builders[intentId] = IntentBuilder(
                        id = intentId,
                        canonical = parts[2],
                        category = parts[3],
                        priority = parts[4].toIntOrNull() ?: 1,
                        action = parts[5]
                    )
                }
            }
            "PAT" -> {
                // PAT:intent_id:pattern_text
                if (parts.size >= 3) {
                    builders[intentId]?.patterns?.add(parts[2])
                }
            }
            "SYN" -> {
                // SYN:intent_id:synonym_text
                if (parts.size >= 3) {
                    builders[intentId]?.synonyms?.add(parts[2])
                }
            }
            "EMB" -> {
                // EMB:intent_id:model:dimension:base64_vector
                if (parts.size >= 5) {
                    val dimension = parts[3].toIntOrNull() ?: 384
                    val base64Vector = parts[4]
                    builders[intentId]?.embedding = decodeEmbedding(base64Vector, dimension)
                }
            }
            "ACT" -> {
                // ACT:intent_id:action_type:params (override action from INT)
                if (parts.size >= 3) {
                    builders[intentId]?.action = parts.drop(2).joinToString(":")
                }
            }
        }
    }

    /**
     * Decode base64-encoded embedding vector
     */
    @OptIn(ExperimentalEncodingApi::class)
    private fun decodeEmbedding(base64String: String, dimension: Int): FloatArray? {
        return try {
            val bytes = Base64.decode(base64String)
            if (bytes.size != dimension * 4) {
                return null // Invalid size
            }

            // Convert bytes to floats (little-endian)
            FloatArray(dimension) { i ->
                val offset = i * 4
                val bits = (bytes[offset].toInt() and 0xFF) or
                        ((bytes[offset + 1].toInt() and 0xFF) shl 8) or
                        ((bytes[offset + 2].toInt() and 0xFF) shl 16) or
                        ((bytes[offset + 3].toInt() and 0xFF) shl 24)
                Float.fromBits(bits)
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Generate AVU content from a list of intents
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun generate(intents: List<UnifiedIntent>, locale: String = "en-US", project: String = "shared"): String {
        val sb = StringBuilder()

        // Header comments
        sb.appendLine("# Avanues Universal Format v1.0")
        sb.appendLine("# Type: AVA")
        sb.appendLine("# Extension: .aai")
        sb.appendLine("---")

        // Schema section
        sb.appendLine("schema: avu-1.0")
        sb.appendLine("version: 1.0.0")
        sb.appendLine("locale: $locale")
        sb.appendLine("project: $project")
        sb.appendLine("metadata:")
        sb.appendLine("  file: generated.aai")
        sb.appendLine("  category: nlu_intents")
        sb.appendLine("  count: ${intents.size}")
        sb.appendLine("---")

        // Data section
        for (intent in intents.sortedBy { it.category }) {
            // INT line
            sb.appendLine("INT:${intent.id}:${intent.canonicalPhrase}:${intent.category}:${intent.priority}:${intent.actionId}")

            // PAT lines
            for (pattern in intent.patterns) {
                sb.appendLine("PAT:${intent.id}:$pattern")
            }

            // SYN lines
            for (synonym in intent.synonyms) {
                sb.appendLine("SYN:${intent.id}:$synonym")
            }

            // EMB line (if embedding exists)
            intent.embedding?.let { embedding ->
                val bytes = ByteArray(embedding.size * 4)
                for (i in embedding.indices) {
                    val bits = embedding[i].toRawBits()
                    bytes[i * 4] = (bits and 0xFF).toByte()
                    bytes[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
                    bytes[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
                    bytes[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
                }
                val base64 = Base64.encode(bytes)
                sb.appendLine("EMB:${intent.id}:mobilebert-384:${embedding.size}:$base64")
            }

            sb.appendLine() // Blank line between intents
        }

        sb.appendLine("---")
        sb.appendLine("synonyms:")

        // Collect global synonyms
        val synonymMap = mutableMapOf<String, MutableSet<String>>()
        for (intent in intents) {
            for (synonym in intent.synonyms) {
                val key = intent.canonicalPhrase.split(" ").firstOrNull() ?: continue
                synonymMap.getOrPut(key) { mutableSetOf() }.add(synonym)
            }
        }

        for ((word, syns) in synonymMap) {
            sb.appendLine("  $word: [${syns.joinToString(", ")}]")
        }

        return sb.toString()
    }

    /**
     * Header data extracted from AVU file
     */
    private data class AvuHeader(
        val locale: String,
        val source: String
    )

    /**
     * Builder for constructing UnifiedIntent from parsed lines
     */
    private class IntentBuilder(
        val id: String,
        val canonical: String,
        val category: String,
        val priority: Int,
        var action: String
    ) {
        val patterns = mutableListOf<String>()
        val synonyms = mutableListOf<String>()
        var embedding: FloatArray? = null

        fun build(locale: String, source: String): UnifiedIntent {
            return UnifiedIntent(
                id = id,
                canonicalPhrase = canonical,
                patterns = patterns.toList(),
                synonyms = synonyms.toList(),
                embedding = embedding,
                category = category,
                actionId = action,
                priority = priority,
                locale = locale,
                source = source
            )
        }
    }
}

/**
 * Result of parsing an AVU file
 */
data class ParseResult(
    val intents: List<UnifiedIntent>,
    val errors: List<String>
) {
    val isSuccess: Boolean get() = errors.isEmpty()
    val intentCount: Int get() = intents.size
}
