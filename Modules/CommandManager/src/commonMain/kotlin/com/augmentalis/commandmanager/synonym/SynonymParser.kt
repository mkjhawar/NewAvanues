/**
 * SynonymParser.kt - Parser for .syn text format
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Parses human-readable .syn synonym files into SynonymMap objects.
 */
package com.augmentalis.commandmanager

/**
 * Parser for .syn synonym file format.
 *
 * File Format:
 * ```
 * # Comment line
 * @meta
 * language = en
 * version = 1.0
 * script = latin
 *
 * @synonyms
 * click | tap, press, push, hit
 * scroll_up | swipe up, go up
 * back | go back, return
 * ```
 */
object SynonymParser {

    /**
     * Parse a .syn file content into a SynonymMap.
     *
     * @param content The file content to parse
     * @param defaultLanguage Language code to use if not specified in file
     * @return Parsed SynonymMap
     * @throws SynonymParseException if parsing fails
     */
    fun parse(content: String, defaultLanguage: String = "en"): SynonymMap {
        val lines = content.lines()

        var language = defaultLanguage
        var version = "1.0"
        var script = ScriptType.LATIN
        var tokenizer = TokenizerType.WHITESPACE
        var isRtl = false

        val entries = mutableListOf<SynonymEntry>()
        var inSynonymsSection = false
        var lineNumber = 0

        for (line in lines) {
            lineNumber++
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isBlank() || trimmed.startsWith("#")) {
                continue
            }

            // Section markers
            when {
                trimmed == "@meta" -> {
                    inSynonymsSection = false
                    continue
                }
                trimmed == "@synonyms" -> {
                    inSynonymsSection = true
                    continue
                }
            }

            // Metadata parsing
            if (!inSynonymsSection && trimmed.startsWith("@") || trimmed.contains("=")) {
                parseMetadataLine(trimmed)?.let { (key, value) ->
                    when (key) {
                        "language" -> language = value
                        "version" -> version = value
                        "script" -> script = parseScript(value)
                        "tokenizer" -> tokenizer = parseTokenizer(value)
                        "direction" -> isRtl = value.lowercase() == "rtl"
                        "rtl" -> isRtl = value.lowercase() == "true"
                    }
                }
                continue
            }

            // Synonym entry parsing
            if (inSynonymsSection || trimmed.contains("|")) {
                SynonymEntry.parse(trimmed)?.let { entry ->
                    entries.add(entry)
                }
            }
        }

        if (entries.isEmpty()) {
            throw SynonymParseException("No synonym entries found in file")
        }

        val metadata = LanguageMetadata(
            languageCode = language,
            languageName = LanguageMetadata.DEFAULTS[language]?.languageName ?: language.uppercase(),
            script = script,
            tokenizer = tokenizer,
            isRtl = isRtl,
            version = version
        )

        return SynonymMap.Builder(language)
            .metadata(metadata)
            .apply { entries.forEach { add(it) } }
            .build()
    }

    /**
     * Parse a metadata line (key = value format).
     */
    private fun parseMetadataLine(line: String): Pair<String, String>? {
        // Handle @key value format
        if (line.startsWith("@") && !line.contains("=")) {
            val parts = line.removePrefix("@").split(Regex("\\s+"), limit = 2)
            if (parts.size == 2) {
                return parts[0].lowercase() to parts[1].trim()
            }
            return null
        }

        // Handle key = value format
        val parts = line.split("=", limit = 2)
        if (parts.size != 2) return null

        val key = parts[0].trim().removePrefix("@").lowercase()
        val value = parts[1].trim()

        return key to value
    }

    /**
     * Parse script type from string.
     */
    private fun parseScript(value: String): ScriptType {
        return when (value.lowercase()) {
            "latin" -> ScriptType.LATIN
            "cyrillic" -> ScriptType.CYRILLIC
            "arabic" -> ScriptType.ARABIC
            "devanagari" -> ScriptType.DEVANAGARI
            "cjk", "chinese", "cjk_chinese" -> ScriptType.CJK_CHINESE
            "japanese", "cjk_japanese" -> ScriptType.CJK_JAPANESE
            "korean", "cjk_korean", "hangul" -> ScriptType.CJK_KOREAN
            "thai" -> ScriptType.THAI
            "hebrew" -> ScriptType.HEBREW
            "greek" -> ScriptType.GREEK
            else -> ScriptType.OTHER
        }
    }

    /**
     * Parse tokenizer type from string.
     */
    private fun parseTokenizer(value: String): TokenizerType {
        return when (value.lowercase()) {
            "whitespace", "space" -> TokenizerType.WHITESPACE
            "morphological", "morpheme" -> TokenizerType.MORPHOLOGICAL
            "character", "character_boundary", "char" -> TokenizerType.CHARACTER_BOUNDARY
            "syllable" -> TokenizerType.SYLLABLE
            "none", "no_boundary" -> TokenizerType.NO_BOUNDARY
            else -> TokenizerType.WHITESPACE
        }
    }

    /**
     * Serialize a SynonymMap back to .syn format.
     *
     * @param map The SynonymMap to serialize
     * @return .syn format string
     */
    fun serialize(map: SynonymMap): String {
        return buildString {
            appendLine("# VoiceOS Synonym Pack")
            appendLine("# Language: ${map.metadata.languageName}")
            appendLine("# Generated: ${currentTimestamp()}")
            appendLine()

            // Metadata section
            appendLine("@meta")
            appendLine("language = ${map.metadata.languageCode}")
            appendLine("version = ${map.metadata.version}")
            appendLine("script = ${map.metadata.script.name.lowercase()}")
            appendLine("tokenizer = ${map.metadata.tokenizer.name.lowercase()}")
            if (map.metadata.isRtl) {
                appendLine("direction = rtl")
            }
            appendLine()

            // Synonyms section
            appendLine("@synonyms")
            for (entry in map.getAllEntries()) {
                val synonymsStr = entry.synonyms.joinToString(", ")
                appendLine("${entry.canonical} | $synonymsStr")
            }
        }
    }

    /**
     * Validate .syn content without fully parsing.
     *
     * @param content The content to validate
     * @return List of validation errors, empty if valid
     */
    fun validate(content: String): List<String> {
        val errors = mutableListOf<String>()
        val lines = content.lines()
        var lineNumber = 0
        var hasLanguage = false
        var hasSynonyms = false

        for (line in lines) {
            lineNumber++
            val trimmed = line.trim()

            if (trimmed.isBlank() || trimmed.startsWith("#")) continue

            // Check for language metadata
            if (trimmed.contains("language") && trimmed.contains("=")) {
                hasLanguage = true
            }

            // Check synonym entries
            if (trimmed.contains("|")) {
                hasSynonyms = true
                val parts = trimmed.split("|")
                if (parts.size != 2) {
                    errors.add("Line $lineNumber: Invalid synonym format (expected 'canonical | synonyms')")
                } else if (parts[0].trim().isBlank()) {
                    errors.add("Line $lineNumber: Empty canonical action")
                } else if (parts[1].trim().isBlank()) {
                    errors.add("Line $lineNumber: No synonyms provided")
                }
            }
        }

        if (!hasLanguage) {
            errors.add("Missing 'language' metadata")
        }
        if (!hasSynonyms) {
            errors.add("No synonym entries found")
        }

        return errors
    }

    /**
     * Get current timestamp for file generation.
     */
    private fun currentTimestamp(): String {
        // Simple timestamp without external dependencies
        return "2026-01-08" // TODO: Use kotlinx-datetime if needed
    }
}

/**
 * Exception thrown when synonym parsing fails.
 */
class SynonymParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
