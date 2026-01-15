package com.augmentalis.avamagic.ipc.universal

/**
 * Universal AVU Format Parser
 *
 * Parses all AVU (Avanues Universal) format files with auto-detection.
 * Supports: CONFIG, VOICE, THEME, STATE, IPC, HANDOVER types.
 *
 * AVU is a line-based, compact format (~50% smaller than JSON) designed
 * for cross-platform data exchange.
 *
 * @author Augmentalis Engineering
 * @since 1.0.0
 * @see AVU-Format-Specification-V1.md
 */
object UniversalAvuParser {

    private const val HEADER_DELIMITER = "---"
    private const val SCHEMA_PREFIX = "schema:"
    private const val AVU_SCHEMA_PREFIX = "avu-"

    /**
     * Parse any AVU format content with auto-detection.
     *
     * @param content The AVU format string content
     * @return Parsed AvuFile with detected type
     * @throws AvuParseException if format is invalid
     */
    fun parse(content: String): AvuFile {
        val lines = content.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }

        // Find section delimiters
        val delimiterIndices = lines.mapIndexedNotNull { index, line ->
            if (line == HEADER_DELIMITER) index else null
        }

        if (delimiterIndices.size < 2) {
            throw AvuParseException("Invalid AVU format: missing section delimiters (---)")
        }

        // Parse header section
        val headerLines = lines.subList(delimiterIndices[0] + 1, delimiterIndices[1])
        val metadata = parseMetadata(headerLines)

        // Detect type from schema
        val type = detectType(metadata.schema, lines)

        // Parse content section
        val contentStart = delimiterIndices[1] + 1
        val contentEnd = if (delimiterIndices.size > 2) delimiterIndices[2] else lines.size
        val contentLines = lines.subList(contentStart, contentEnd)
        val entries = parseEntries(contentLines)

        // Parse optional synonyms section
        val synonyms = if (delimiterIndices.size > 2 && delimiterIndices[2] + 1 < lines.size) {
            parseSynonyms(lines.subList(delimiterIndices[2] + 1, lines.size))
        } else {
            emptyMap()
        }

        return AvuFile(
            type = type,
            schema = metadata.schema,
            version = metadata.version,
            locale = metadata.locale,
            project = metadata.project,
            metadata = metadata.extra,
            entries = entries,
            synonyms = synonyms
        )
    }

    /**
     * Parse content and return as specific typed result.
     */
    inline fun <reified T : AvuTypedResult> parseAs(content: String): T {
        val file = parse(content)
        return when (T::class) {
            AvuVoiceCommands::class -> parseVoiceCommands(file) as T
            AvuConfig::class -> parseConfig(file) as T
            AvuTheme::class -> parseTheme(file) as T
            else -> throw AvuParseException("Unsupported type: ${T::class.simpleName}")
        }
    }

    /**
     * Convert voice commands AVU to structured result.
     */
    fun parseVoiceCommands(file: AvuFile): AvuVoiceCommands {
        require(file.type == AvuType.VOICE) { "Not a VOICE file: ${file.type}" }

        val commands = mutableListOf<AvuVoiceCommand>()
        val categories = mutableMapOf<String, AvuCategory>()

        for (entry in file.entries) {
            when (entry.prefix) {
                "CMD" -> {
                    val parts = entry.data.split(":", limit = 2)
                    if (parts.size >= 2) {
                        val synonyms = file.synonyms[parts[0]] ?: emptyList()
                        commands.add(AvuVoiceCommand(
                            action = parts[0],
                            primaryText = parts[1],
                            synonyms = synonyms
                        ))
                    }
                }
                "CAT" -> {
                    val parts = entry.data.split(":", limit = 3)
                    if (parts.size >= 2) {
                        categories[parts[0]] = AvuCategory(
                            id = parts[0],
                            displayName = parts[1],
                            description = parts.getOrNull(2) ?: ""
                        )
                    }
                }
            }
        }

        return AvuVoiceCommands(
            locale = file.locale,
            version = file.version,
            categories = categories,
            commands = commands
        )
    }

    /**
     * Convert config AVU to structured result.
     */
    fun parseConfig(file: AvuFile): AvuConfig {
        require(file.type == AvuType.CONFIG) { "Not a CONFIG file: ${file.type}" }

        val config = mutableMapOf<String, Any>()
        val modules = mutableListOf<AvuModule>()
        val paths = mutableMapOf<String, String>()
        val gates = mutableMapOf<String, AvuGate>()

        for (entry in file.entries) {
            when (entry.prefix) {
                "CFG" -> {
                    val parts = entry.data.split(":", limit = 3)
                    if (parts.size >= 2) {
                        val key = parts[0]
                        val value = parseTypedValue(parts[1], parts.getOrNull(2))
                        config[key] = value
                    }
                }
                "MOD" -> {
                    val parts = entry.data.split(":", limit = 3)
                    if (parts.size >= 3) {
                        modules.add(AvuModule(
                            name = parts[0],
                            path = parts[1],
                            status = parts[2]
                        ))
                    }
                }
                "PTH" -> {
                    val parts = entry.data.split(":", limit = 2)
                    if (parts.size >= 2) {
                        paths[parts[0]] = parts[1]
                    }
                }
                "GAT" -> {
                    val parts = entry.data.split(":", limit = 3)
                    if (parts.size >= 3) {
                        gates[parts[0]] = AvuGate(
                            name = parts[0],
                            threshold = parts[1].toIntOrNull() ?: 0,
                            enforce = parts[2].toBooleanStrictOrNull() ?: false
                        )
                    }
                }
            }
        }

        return AvuConfig(
            project = file.project,
            version = file.version,
            config = config,
            modules = modules,
            paths = paths,
            gates = gates
        )
    }

    /**
     * Convert theme AVU to structured result.
     */
    fun parseTheme(file: AvuFile): AvuTheme {
        require(file.type == AvuType.THEME) { "Not a THEME file: ${file.type}" }

        var name = "Unnamed Theme"
        val palette = mutableMapOf<String, String>()
        val typography = mutableMapOf<String, AvuTextStyle>()
        var spacing: AvuSpacing? = null
        var effects: AvuEffects? = null

        for (entry in file.entries) {
            when (entry.prefix) {
                "THM" -> {
                    val parts = entry.data.split(":", limit = 2)
                    name = parts[0]
                }
                "PAL" -> {
                    val parts = entry.data.split(":", limit = 2)
                    if (parts.size >= 2) {
                        palette[parts[0]] = parts[1]
                    }
                }
                "TYP" -> {
                    val parts = entry.data.split(":", limit = 4)
                    if (parts.size >= 4) {
                        typography[parts[0]] = AvuTextStyle(
                            size = parts[1].toFloatOrNull() ?: 16f,
                            weight = parts[2],
                            family = parts[3]
                        )
                    }
                }
                "SPC" -> {
                    spacing = parseSpacing(entry.data)
                }
                "EFX" -> {
                    effects = parseEffects(entry.data)
                }
            }
        }

        return AvuTheme(
            name = name,
            version = file.version,
            palette = palette,
            typography = typography,
            spacing = spacing ?: AvuSpacing(),
            effects = effects ?: AvuEffects()
        )
    }

    // --- Private Helpers ---

    private fun parseMetadata(lines: List<String>): ParsedMetadata {
        var schema = ""
        var version = ""
        var locale = ""
        var project = ""
        val extra = mutableMapOf<String, String>()

        for (line in lines) {
            when {
                line.startsWith("schema:") -> schema = line.substringAfter(":").trim()
                line.startsWith("version:") -> version = line.substringAfter(":").trim()
                line.startsWith("locale:") -> locale = line.substringAfter(":").trim()
                line.startsWith("project:") -> project = line.substringAfter(":").trim()
                line.contains(":") -> {
                    val key = line.substringBefore(":").trim()
                    val value = line.substringAfter(":").trim()
                    extra[key] = value
                }
            }
        }

        return ParsedMetadata(schema, version, locale, project, extra)
    }

    private fun parseEntries(lines: List<String>): List<AvuEntry> {
        return lines.mapNotNull { line ->
            val colonIndex = line.indexOf(':')
            if (colonIndex in 1..3) {
                val prefix = line.substring(0, colonIndex).uppercase()
                val data = line.substring(colonIndex + 1)
                AvuEntry(prefix, data)
            } else null
        }
    }

    private fun parseSynonyms(lines: List<String>): Map<String, List<String>> {
        val synonyms = mutableMapOf<String, List<String>>()

        for (line in lines) {
            if (line.startsWith("SYN:")) {
                val data = line.substringAfter("SYN:")
                val bracketStart = data.indexOf('[')
                val bracketEnd = data.indexOf(']')

                if (bracketStart > 0 && bracketEnd > bracketStart) {
                    val key = data.substring(0, bracketStart).trimEnd(':')
                    val values = data.substring(bracketStart + 1, bracketEnd)
                        .split(",")
                        .map { it.trim() }
                    synonyms[key] = values
                }
            }
        }

        return synonyms
    }

    private fun detectType(schema: String, lines: List<String>): AvuType {
        // Primary: detect from schema
        return when {
            schema.contains("cfg") || schema.contains("idc") -> AvuType.CONFIG
            schema.contains("vos") || schema.contains("voice") -> AvuType.VOICE
            schema.contains("thm") || schema.contains("theme") -> AvuType.THEME
            schema.contains("sta") || schema.contains("state") -> AvuType.STATE
            schema.contains("ipc") -> AvuType.IPC
            schema.contains("hov") || schema.contains("handover") -> AvuType.HANDOVER
            else -> detectTypeFromPrefixes(lines)
        }
    }

    private fun detectTypeFromPrefixes(lines: List<String>): AvuType {
        // Fallback: detect from first data prefix
        val firstPrefix = lines.firstOrNull { line ->
            val colonIndex = line.indexOf(':')
            colonIndex in 1..3 && line.substring(0, colonIndex).all { it.isUpperCase() }
        }?.substringBefore(":")?.uppercase()

        return when (firstPrefix) {
            "PRJ", "CFG", "PRF", "GAT", "THR", "SWM", "PTH", "REG", "FNM", "MOD" -> AvuType.CONFIG
            "CMD", "CAT", "SYN", "ACT", "LOC", "VAR" -> AvuType.VOICE
            "THM", "PAL", "TYP", "SPC", "EFX", "CMP" -> AvuType.THEME
            "APP", "STA", "SCR", "ELM", "NAV", "FCS" -> AvuType.STATE
            "REQ", "RES", "EVT", "ERR", "ACK", "BCT" -> AvuType.IPC
            "ARC", "WIP", "BLK", "NXT", "USR", "FIL", "DEC", "LEA", "TSK", "DEP", "API", "BUG", "CTX", "PRI" -> AvuType.HANDOVER
            else -> AvuType.DATA
        }
    }

    private fun parseTypedValue(value: String, type: String?): Any {
        return when (type?.lowercase()) {
            "bool", "boolean" -> value.toBooleanStrictOrNull() ?: value
            "int", "integer" -> value.toIntOrNull() ?: value
            "float", "double" -> value.toDoubleOrNull() ?: value
            "string" -> value
            else -> when {
                value == "true" || value == "false" -> value.toBoolean()
                value.toIntOrNull() != null -> value.toInt()
                value.toDoubleOrNull() != null -> value.toDouble()
                else -> value
            }
        }
    }

    private fun parseSpacing(data: String): AvuSpacing {
        val parts = data.split(":")
        val map = mutableMapOf<String, Float>()
        var i = 0
        while (i + 1 < parts.size) {
            map[parts[i]] = parts[i + 1].toFloatOrNull() ?: 0f
            i += 2
        }
        return AvuSpacing(
            xs = map["xs"] ?: 4f,
            sm = map["sm"] ?: 8f,
            md = map["md"] ?: 16f,
            lg = map["lg"] ?: 24f,
            xl = map["xl"] ?: 32f
        )
    }

    private fun parseEffects(data: String): AvuEffects {
        val parts = data.split(":")
        val map = mutableMapOf<String, String>()
        var i = 0
        while (i + 1 < parts.size) {
            map[parts[i]] = parts[i + 1]
            i += 2
        }
        return AvuEffects(
            shadowEnabled = map["shadow"]?.toBooleanStrictOrNull() ?: true,
            blurRadius = map["blur"]?.toFloatOrNull() ?: 8f,
            elevation = map["elevation"]?.toFloatOrNull() ?: 4f
        )
    }

    private data class ParsedMetadata(
        val schema: String,
        val version: String,
        val locale: String,
        val project: String,
        val extra: Map<String, String>
    )
}

// --- Data Classes ---

/**
 * AVU file type enumeration
 */
enum class AvuType {
    CONFIG,     // Configuration files (.idc → .avu)
    VOICE,      // Voice commands (.vos → .avu)
    THEME,      // Theme definitions (.amf → .avu)
    STATE,      // State exchange
    IPC,        // Inter-process communication
    HANDOVER,   // AI context handover (.hov → .avu)
    DATA        // Generic data (fallback)
}

/**
 * Parsed AVU file representation
 */
data class AvuFile(
    val type: AvuType,
    val schema: String,
    val version: String,
    val locale: String,
    val project: String,
    val metadata: Map<String, String>,
    val entries: List<AvuEntry>,
    val synonyms: Map<String, List<String>>
)

/**
 * Single AVU entry (prefix:data)
 */
data class AvuEntry(
    val prefix: String,
    val data: String
)

// --- Typed Results ---

sealed interface AvuTypedResult

data class AvuVoiceCommands(
    val locale: String,
    val version: String,
    val categories: Map<String, AvuCategory>,
    val commands: List<AvuVoiceCommand>
) : AvuTypedResult

data class AvuVoiceCommand(
    val action: String,
    val primaryText: String,
    val synonyms: List<String>
)

data class AvuCategory(
    val id: String,
    val displayName: String,
    val description: String
)

data class AvuConfig(
    val project: String,
    val version: String,
    val config: Map<String, Any>,
    val modules: List<AvuModule>,
    val paths: Map<String, String>,
    val gates: Map<String, AvuGate>
) : AvuTypedResult

data class AvuModule(
    val name: String,
    val path: String,
    val status: String
)

data class AvuGate(
    val name: String,
    val threshold: Int,
    val enforce: Boolean
)

data class AvuTheme(
    val name: String,
    val version: String,
    val palette: Map<String, String>,
    val typography: Map<String, AvuTextStyle>,
    val spacing: AvuSpacing,
    val effects: AvuEffects
) : AvuTypedResult

data class AvuTextStyle(
    val size: Float,
    val weight: String,
    val family: String
)

data class AvuSpacing(
    val xs: Float = 4f,
    val sm: Float = 8f,
    val md: Float = 16f,
    val lg: Float = 24f,
    val xl: Float = 32f
)

data class AvuEffects(
    val shadowEnabled: Boolean = true,
    val blurRadius: Float = 8f,
    val elevation: Float = 4f
)

/**
 * Exception thrown when AVU parsing fails
 */
class AvuParseException(message: String, cause: Throwable? = null) : Exception(message, cause)
