package com.augmentalis.avucodec.core

/**
 * AVU Header - Self-documenting file header support
 *
 * AVU files can include a header section that documents the codes used
 * in the file. This makes files self-documenting for both humans and AI.
 *
 * Header format:
 * ```
 * # Avanues Universal Format v2.2
 * # Type: <type>
 * ---
 * schema: avu-2.2
 * version: <version>
 * type: workflow
 * codes:
 *   SCR: Sync Create (msgId:entityType:entityId:version:data)
 *   SUP: Sync Update (msgId:entityType:entityId:version:data)
 * permissions:
 *   GESTURES
 *   APPS
 * triggers:
 *   open {app}
 * ---
 * <body>
 * ```
 *
 * Core fields (schema, version, type, project, metadata, codes) are parsed into
 * dedicated properties. Any additional sections (permissions, triggers, imports,
 * dependencies, etc.) are captured generically in [HeaderData.sections] so that
 * consumers can extract what they need without requiring AVUCodec changes.
 *
 * @author Augmentalis Engineering
 * @since AVU 2.2
 */
object AvuHeader {

    const val SCHEMA_VERSION = "avu-2.2"
    const val HEADER_SEPARATOR = "---"
    const val COMMENT_PREFIX = "#"

    /**
     * Parsed header data from an AVU file.
     *
     * Core fields have dedicated properties. Any other section encountered in the
     * header (e.g., permissions, triggers, imports) is captured in [sections] as
     * a map of section name to list of trimmed line values.
     */
    data class HeaderData(
        val formatVersion: String = "2.2",
        val type: String = "",
        val schema: String = SCHEMA_VERSION,
        val version: String = "1.0.0",
        val project: String? = null,
        val metadata: Map<String, String> = emptyMap(),
        val codes: Map<String, String> = emptyMap(),
        val sections: Map<String, List<String>> = emptyMap()
    )

    /**
     * Generate a complete AVU file header.
     *
     * @param type Document type (e.g., "WebSocket Sync", "Plugin Manifest")
     * @param version Document version
     * @param project Optional project name
     * @param metadata Additional metadata key-value pairs
     * @param codes Set of code names to include in legend (looked up from registry)
     * @param includeDescriptions Whether to include code descriptions
     * @param sections Additional named sections to include (e.g., "permissions" to listOf("GESTURES"))
     * @return Formatted header string
     */
    fun generate(
        type: String,
        version: String = "1.0.0",
        project: String? = null,
        metadata: Map<String, String> = emptyMap(),
        codes: Set<String>? = null,
        includeDescriptions: Boolean = false,
        sections: Map<String, List<String>> = emptyMap()
    ): String = buildString {
        // Comment header
        appendLine("# Avanues Universal Format v2.2")
        appendLine("# Type: $type")
        appendLine(HEADER_SEPARATOR)

        // Schema and version
        appendLine("schema: $SCHEMA_VERSION")
        appendLine("version: $version")

        // Type as key-value (for DSL files)
        if (type.isNotEmpty()) {
            appendLine("type: $type")
        }

        // Optional project
        project?.let { appendLine("project: $it") }

        // Metadata
        if (metadata.isNotEmpty()) {
            appendLine("metadata:")
            metadata.forEach { (key, value) ->
                appendLine("  $key: $value")
            }
        }

        // Codes legend
        if (codes != null && codes.isNotEmpty()) {
            append(AvuCodeRegistry.generateLegend(codes, includeDescriptions))
        }

        // Generic sections
        for ((sectionName, entries) in sections) {
            appendLine("$sectionName:")
            entries.forEach { entry ->
                appendLine("  $entry")
            }
        }

        appendLine(HEADER_SEPARATOR)
    }

    /**
     * Generate a minimal header without codes legend.
     *
     * Use for wire protocol messages where overhead matters.
     *
     * @param type Document type
     * @param version Document version
     * @return Minimal header string
     */
    fun generateMinimal(
        type: String,
        version: String = "1.0.0"
    ): String = buildString {
        appendLine("# AVU $type v$version")
        appendLine(HEADER_SEPARATOR)
    }

    /**
     * Parse an AVU file header.
     *
     * Recognizes core fields (schema, version, type, project, metadata, codes)
     * into dedicated properties. Any other section (a line ending with `:` followed
     * by indented lines) is captured in [HeaderData.sections].
     *
     * @param content The full AVU file content
     * @return Parsed header data and the character index where body starts
     */
    fun parse(content: String): Pair<HeaderData, Int> {
        val lines = content.lines()
        var formatVersion = "2.2"
        var type = ""
        var schema = SCHEMA_VERSION
        var version = "1.0.0"
        var project: String? = null
        val metadata = mutableMapOf<String, String>()
        val codes = mutableMapOf<String, String>()
        val sections = mutableMapOf<String, MutableList<String>>()

        var currentSection: String? = null
        var headerEndIndex = 0
        var separatorCount = 0

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()

            // Track separator count
            if (trimmed == HEADER_SEPARATOR) {
                separatorCount++
                if (separatorCount == 2) {
                    headerEndIndex = lines.take(index + 1).sumOf { it.length + 1 }
                    break
                }
                currentSection = null
                continue
            }

            // Only parse between separators
            if (separatorCount < 1) continue

            // Skip empty lines in header
            if (trimmed.isEmpty()) continue

            // Parse comment lines
            if (trimmed.startsWith(COMMENT_PREFIX)) {
                val comment = trimmed.removePrefix(COMMENT_PREFIX).trim()
                when {
                    comment.startsWith("Avanues Universal Format v") -> {
                        formatVersion = comment.substringAfter("v").trim()
                    }
                    comment.startsWith("Type:") -> {
                        type = comment.substringAfter("Type:").trim()
                    }
                }
                continue
            }

            val isIndented = line.startsWith("  ") || line.startsWith("\t")

            // Indented line belongs to current section
            if (isIndented && currentSection != null) {
                when (currentSection) {
                    "metadata" -> {
                        if (trimmed.contains(":")) {
                            val key = trimmed.substringBefore(":").trim()
                            val value = trimmed.substringAfter(":").trim()
                            metadata[key] = value
                        }
                    }
                    "codes" -> {
                        if (trimmed.contains(":")) {
                            val code = trimmed.substringBefore(":").trim()
                            val description = trimmed.substringAfter(":").trim()
                            codes[code] = description
                        }
                    }
                    else -> {
                        // Generic section: store trimmed line
                        sections.getOrPut(currentSection) { mutableListOf() }.add(trimmed)
                    }
                }
                continue
            }

            // Non-indented line with colon: top-level key or section header
            if (trimmed.contains(":")) {
                val key = trimmed.substringBefore(":").trim()
                val value = trimmed.substringAfter(":").trim()

                if (value.isEmpty()) {
                    // Section header (e.g., "metadata:", "codes:", "permissions:")
                    currentSection = key
                } else {
                    // Top-level key: value
                    currentSection = null
                    when (key) {
                        "schema" -> schema = value
                        "version" -> version = value
                        "type" -> type = value
                        "project" -> project = value
                        else -> {
                            // Unknown key with value on same line - store as single-entry section
                            sections.getOrPut(key) { mutableListOf() }.add(value)
                        }
                    }
                }
            }
        }

        return HeaderData(
            formatVersion = formatVersion,
            type = type,
            schema = schema,
            version = version,
            project = project,
            metadata = metadata,
            codes = codes,
            sections = sections
        ) to headerEndIndex
    }

    /**
     * Extract just the body content (after header) from an AVU file.
     *
     * @param content The full AVU file content
     * @return The body content without header
     */
    fun extractBody(content: String): String {
        val (_, bodyStart) = parse(content)
        return if (bodyStart > 0 && bodyStart < content.length) {
            content.substring(bodyStart).trimStart()
        } else {
            content
        }
    }

    /**
     * Check if content has a valid AVU header.
     *
     * @param content The content to check
     * @return true if it starts with an AVU header
     */
    fun hasHeader(content: String): Boolean {
        val firstLine = content.lines().firstOrNull()?.trim() ?: return false
        return firstLine.startsWith("# Avanues") ||
               firstLine.startsWith("# AVU") ||
               firstLine == HEADER_SEPARATOR
    }
}
