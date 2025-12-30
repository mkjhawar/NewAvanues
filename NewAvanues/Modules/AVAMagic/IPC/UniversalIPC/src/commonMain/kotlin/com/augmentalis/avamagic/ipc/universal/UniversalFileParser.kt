package com.augmentalis.avamagic.ipc.universal

/**
 * Universal File Parser for Avanues Ecosystem
 *
 * Parses all file types: .ava, .vos, .avc, .avw, .avn, .avs
 * All share same structure but different extensions for clear ownership
 *
 * @author Manoj Jhawar (manoj@ideahq.net)
 */
object UniversalFileParser {

    /**
     * Parse any Avanues Universal Format file
     *
     * @param content File content as string
     * @return Parsed UniversalFile
     */
    fun parse(content: String): UniversalFile {
        val sections = content.split("---").map { it.trim() }

        require(sections.size >= 3) {
            "Invalid file format: expected at least 3 sections (header, metadata, entries), got ${sections.size}"
        }

        val header = parseHeader(sections[0])
        val metadata = parseMetadata(sections[1])
        val entries = parseEntries(sections[2])
        val synonyms = if (sections.size > 3) parseSynonyms(sections[3]) else emptyMap()

        return UniversalFile(
            type = header.type,
            extension = header.extension,
            schema = metadata.schema,
            version = metadata.version,
            locale = metadata.locale,
            project = metadata.project,
            metadata = metadata.metadataBlock,
            entries = entries,
            synonyms = synonyms
        )
    }

    private fun parseHeader(section: String): ParsedHeader {
        val lines = section.lines().map { it.trim() }

        val typeLine = lines.find { it.startsWith("# Type:") }
            ?: throw IllegalArgumentException("Missing '# Type:' in header")

        val extLine = lines.find { it.startsWith("# Extension:") }
            ?: throw IllegalArgumentException("Missing '# Extension:' in header")

        val typeStr = typeLine.substringAfter("# Type:").trim()
        val type = FileType.valueOf(typeStr.uppercase())

        val extension = extLine.substringAfter("# Extension:").trim()

        return ParsedHeader(type, extension)
    }

    private fun parseMetadata(section: String): ParsedMetadata {
        val lines = section.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        var schema = ""
        var version = ""
        var locale = ""
        var project = ""
        val metadataBlock = mutableMapOf<String, Any>()

        var inMetadataBlock = false
        var currentKey = ""

        for (line in lines) {
            when {
                line.startsWith("schema:") -> schema = line.substringAfter(":").trim()
                line.startsWith("version:") -> version = line.substringAfter(":").trim()
                line.startsWith("locale:") -> locale = line.substringAfter(":").trim()
                line.startsWith("project:") -> project = line.substringAfter(":").trim()
                line == "metadata:" -> inMetadataBlock = true
                inMetadataBlock && line.contains(":") -> {
                    val key = line.substringBefore(":").trim()
                    val value = line.substringAfter(":").trim()
                    currentKey = key
                    metadataBlock[key] = parseValue(value)
                }
            }
        }

        return ParsedMetadata(schema, version, locale, project, metadataBlock)
    }

    private fun parseEntries(section: String): List<UniversalEntry> {
        return section.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }
            .map { line ->
                val parts = line.split(":", limit = 3)
                require(parts.size >= 2) { "Invalid entry format: $line" }

                UniversalEntry(
                    code = parts[0],
                    id = parts[1],
                    data = if (parts.size > 2) parts[2] else ""
                )
            }
    }

    private fun parseSynonyms(section: String): Map<String, List<String>> {
        val lines = section.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") }

        val synonyms = mutableMapOf<String, List<String>>()
        var inSynonymsBlock = false

        for (line in lines) {
            when {
                line.startsWith("synonyms:") -> inSynonymsBlock = true
                inSynonymsBlock && line.contains(":") -> {
                    val key = line.substringBefore(":").trim()
                    val valueStr = line.substringAfter(":").trim()

                    // Parse [word1, word2, word3]
                    if (valueStr.startsWith("[") && valueStr.endsWith("]")) {
                        val values = valueStr.substring(1, valueStr.length - 1)
                            .split(",")
                            .map { it.trim() }
                        synonyms[key] = values
                    }
                }
            }
        }

        return synonyms
    }

    private fun parseValue(value: String): Any {
        return when {
            value == "true" -> true
            value == "false" -> false
            value.toIntOrNull() != null -> value.toInt()
            value.toLongOrNull() != null -> value.toLong()
            value.startsWith("[") && value.endsWith("]") -> {
                value.substring(1, value.length - 1)
                    .split(",")
                    .map { it.trim() }
            }
            else -> value
        }
    }

    private data class ParsedHeader(
        val type: FileType,
        val extension: String
    )

    private data class ParsedMetadata(
        val schema: String,
        val version: String,
        val locale: String,
        val project: String,
        val metadataBlock: Map<String, Any>
    )
}

/**
 * Parsed universal file
 */
data class UniversalFile(
    val type: FileType,
    val extension: String,
    val schema: String,
    val version: String,
    val locale: String,
    val project: String,
    val metadata: Map<String, Any>,
    val entries: List<UniversalEntry>,
    val synonyms: Map<String, List<String>>
) {
    /**
     * Filter entries by IPC code
     */
    fun filterByCode(code: String): List<UniversalEntry> {
        return entries.filter { it.code == code }
    }

    /**
     * Get entry by ID
     */
    fun getEntryById(id: String): UniversalEntry? {
        return entries.find { it.id == id }
    }

    /**
     * Convert all entries to IPC messages
     */
    fun toIPCMessages(): List<UniversalMessage> {
        return entries.mapNotNull { entry ->
            try {
                entry.toIPCMessage()
            } catch (e: Exception) {
                null // Skip invalid entries
            }
        }
    }
}

/**
 * Single entry from file
 */
data class UniversalEntry(
    val code: String,
    val id: String,
    val data: String
) {
    /**
     * Convert to IPC message with runtime request ID
     */
    fun toIPCMessage(requestId: String? = null): UniversalMessage {
        val finalId = requestId ?: generateRequestId()
        val ipcString = "$code:$finalId:$data"

        val result = UniversalDSL.parse(ipcString)
        return when (result) {
            is ParseResult.Protocol -> result.message
            else -> throw IllegalArgumentException("Entry cannot be converted to IPC message: $ipcString")
        }
    }

    private fun generateRequestId(): String {
        return "${code.lowercase()}_${System.currentTimeMillis()}"
    }
}

/**
 * File type enum - FINAL CORRECTED EXTENSIONS
 */
enum class FileType {
    AVA,    // AVA voice intents
    VOS,    // VoiceOS system commands
    AVC,    // AvaConnect device communication
    AWB,    // WebAvanue/BrowserAvanue browser commands (Ava Web Browser)
    AMI,    // MagicUI components (Ava MagicUI)
    AMC,    // MagicCode generators (Ava MagicCode)
    HOV,    // Handover files (AI context continuity)
    IDC;    // IDEACODE config files

    fun toExtension(): String = ".${name.lowercase()}"

    fun toProjectName(): String = when(this) {
        AVA -> "ava"
        VOS -> "voiceos"
        AVC -> "avaconnect"
        AWB -> "browseravanue"
        AMI -> "magicui"
        AMC -> "magiccode"
        HOV -> "handover"
        IDC -> "ideacode"
    }
}

/**
 * Project-specific file readers
 */
class AvaFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AVA) { "Not an AVA file: got ${file.type}" }
        return file
    }
}

class VosFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.VOS) { "Not a VOS file: got ${file.type}" }
        return file
    }
}

class AvcFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AVC) { "Not an AVC file: got ${file.type}" }
        return file
    }
}

class AwbFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AWB) { "Not an AWB file: got ${file.type}" }
        return file
    }
}

class AmiFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AMI) { "Not an AMI file: got ${file.type}" }
        return file
    }
}

class AmcFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.AMC) { "Not an AMC file: got ${file.type}" }
        return file
    }
}

class HovFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.HOV) { "Not a HOV file: got ${file.type}" }
        return file
    }
}

class IdcFileReader {
    fun load(content: String): UniversalFile {
        val file = UniversalFileParser.parse(content)
        require(file.type == FileType.IDC) { "Not an IDC file: got ${file.type}" }
        return file
    }
}

/**
 * Handover Entry Codes (HOV files)
 *
 * Used for AI context continuity and session handovers.
 * Each code represents a specific type of handover information.
 */
object HandoverCodes {
    const val ARC = "ARC"  // Architecture - patterns, structure, design decisions
    const val STA = "STA"  // State - current status, progress
    const val WIP = "WIP"  // Work in Progress - active tasks, partially complete
    const val BLK = "BLK"  // Blocker - issues preventing progress
    const val DEC = "DEC"  // Decision - key decisions made with rationale
    const val FIL = "FIL"  // File - file references, paths
    const val MOD = "MOD"  // Module - module-specific context
    const val LEA = "LEA"  // Learning - insights, mistakes, corrections
    const val TSK = "TSK"  // Task - pending tasks, todos
    const val DEP = "DEP"  // Dependency - module/package dependencies
    const val CFG = "CFG"  // Config - configuration state
    const val API = "API"  // API - interface changes, contracts
    const val BUG = "BUG"  // Bug - known issues, workarounds
    const val REF = "REF"  // Reference - links, related docs
    const val CTX = "CTX"  // Context - session context, recent work
    const val PRI = "PRI"  // Priority - P0/P1/P2 items

    val ALL = listOf(ARC, STA, WIP, BLK, DEC, FIL, MOD, LEA, TSK, DEP, CFG, API, BUG, REF, CTX, PRI)

    fun description(code: String): String = when(code) {
        ARC -> "Architecture (patterns, structure)"
        STA -> "State (current status)"
        WIP -> "Work in Progress"
        BLK -> "Blocker (preventing progress)"
        DEC -> "Decision (with rationale)"
        FIL -> "File reference"
        MOD -> "Module context"
        LEA -> "Learning/insight"
        TSK -> "Task/todo"
        DEP -> "Dependency"
        CFG -> "Configuration"
        API -> "API/interface"
        BUG -> "Known bug"
        REF -> "Reference/link"
        CTX -> "Session context"
        PRI -> "Priority item"
        else -> "Unknown"
    }
}
