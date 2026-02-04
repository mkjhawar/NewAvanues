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
 * codes:
 *   SCR: Sync Create (msgId:entityType:entityId:version:data)
 *   SUP: Sync Update (msgId:entityType:entityId:version:data)
 * ---
 * <messages>
 * ```
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
     */
    data class HeaderData(
        val formatVersion: String = "2.2",
        val type: String = "",
        val schema: String = SCHEMA_VERSION,
        val version: String = "1.0.0",
        val project: String? = null,
        val metadata: Map<String, String> = emptyMap(),
        val codes: Map<String, String> = emptyMap()  // code -> format/description
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
     * @return Formatted header string
     */
    fun generate(
        type: String,
        version: String = "1.0.0",
        project: String? = null,
        metadata: Map<String, String> = emptyMap(),
        codes: Set<String>? = null,
        includeDescriptions: Boolean = false
    ): String = buildString {
        // Comment header
        appendLine("# Avanues Universal Format v2.2")
        appendLine("# Type: $type")
        appendLine(HEADER_SEPARATOR)

        // Schema and version
        appendLine("schema: $SCHEMA_VERSION")
        appendLine("version: $version")

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
     * @param content The full AVU file content
     * @return Parsed header data and the index where body starts
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

        var inMetadata = false
        var inCodes = false
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
                inMetadata = false
                inCodes = false
                continue
            }

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

            // Parse key-value lines
            when {
                trimmed.startsWith("schema:") -> {
                    schema = trimmed.substringAfter("schema:").trim()
                }
                trimmed.startsWith("version:") -> {
                    version = trimmed.substringAfter("version:").trim()
                }
                trimmed.startsWith("project:") -> {
                    project = trimmed.substringAfter("project:").trim()
                }
                trimmed == "metadata:" -> {
                    inMetadata = true
                    inCodes = false
                }
                trimmed == "codes:" -> {
                    inCodes = true
                    inMetadata = false
                }
                inMetadata && trimmed.contains(":") -> {
                    val key = trimmed.substringBefore(":").trim()
                    val value = trimmed.substringAfter(":").trim()
                    metadata[key] = value
                }
                inCodes && trimmed.contains(":") -> {
                    val code = trimmed.substringBefore(":").trim()
                    val description = trimmed.substringAfter(":").trim()
                    codes[code] = description
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
            codes = codes
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
