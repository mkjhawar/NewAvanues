/*
 * Copyright (c) 2026 Augmentalis. All rights reserved.
 *
 * AvuExporter.kt - Exports exploration data to AVU format
 *
 * Generates .vos files in the compact AVU (Avanues Universal) format.
 * Format specification: VoiceOS-AVU-UNIVERSAL-FORMAT-SPEC-50312-V1.md
 *
 * Author: Manoj Jhawar
 * Created: 2026-01-16
 * Migrated from: LearnAppCore/export/AVUExporter.kt
 *
 * ## AVU Format Structure:
 * ```
 * # Avanues Universal Format v2.2
 * # Type: VOS
 * ---
 * schema: avu-2.2
 * version: 2.2.0
 * locale: en-US
 * project: voiceos
 * metadata:
 *   file: com.app.package.vos
 *   category: learned_app
 *   count: 87
 * ---
 * APP:package:name:timestamp
 * STA:screens:elements:commands:avg_depth:max_depth:coverage
 * SCR:hash:activity:timestamp:element_count
 * ELM:uuid:label:type:actions:bounds:category
 * NAV:from_hash:to_hash:trigger_uuid:trigger_label:timestamp
 * DNC:element_id:label:type:reason
 * DYN:screen_hash:region_id:change_type
 * MNU:menu_id:total_items:visible_items:menu_type
 * CMD:uuid:trigger:action:element_uuid:confidence
 * ---
 * synonyms:
 *   word: [syn1, syn2]
 * ```
 *
 * @since 2.0.0 (VoiceOSCoreNG)
 */

package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.AVUSerializer
import com.augmentalis.voiceoscore.ExplorationStats
import com.augmentalis.voiceoscore.QuantizedCommand
import com.augmentalis.voiceoscore.QuantizedContext
import com.augmentalis.voiceoscore.currentTimeMillis

/**
 * Export mode for AVU files.
 */
enum class ExportMode {
    /** User edition: Encrypted, minimal metadata */
    USER,

    /** Developer edition: Unencrypted, full debugging info */
    DEVELOPER
}

/**
 * Result of AVU export operation.
 */
data class ExportResult(
    val success: Boolean,
    val filePath: String?,
    val content: String,
    val lineCount: Int,
    val errorMessage: String? = null
)

/**
 * AVU export command representation (simplified for AVU format).
 * Different from ICommandGenerator.GeneratedCommand - this is for AVU export only.
 */
data class AvuExportCommand(
    val avid: String,
    val trigger: String,
    val action: String,
    val elementAvid: String,
    val confidence: Float
) {
    /**
     * Legacy alias for elementAvid.
     */
    @Deprecated("Use elementAvid instead", ReplaceWith("elementAvid"))
    val elementUuid: String get() = elementAvid

    /**
     * Legacy alias for avid.
     */
    @Deprecated("Use avid instead", ReplaceWith("avid"))
    val uuid: String get() = avid

    /**
     * Convert to AVU CMD line format.
     */
    fun toCmdLine(): String {
        val formattedConfidence = formatFloat(confidence)
        return "CMD:$avid:$trigger:$action:$elementAvid:$formattedConfidence"
    }

    /**
     * Convert to QuantizedCommand for integration with existing system.
     */
    fun toQuantizedCommand(packageName: String): QuantizedCommand {
        return QuantizedCommand.create(
            avid = avid,
            phrase = trigger,
            actionType = com.augmentalis.voiceoscore.CommandActionType.fromString(action),
            packageName = packageName,
            targetAvid = elementAvid.takeIf { it.isNotBlank() },
            confidence = confidence
        )
    }

    private fun formatFloat(value: Float): String {
        val rounded = (value * 100).toInt()
        val intPart = rounded / 100
        val decPart = rounded % 100
        return "$intPart.${decPart.toString().padStart(2, '0')}"
    }

    companion object {
        /**
         * Create from QuantizedCommand.
         */
        fun fromQuantizedCommand(cmd: QuantizedCommand): AvuExportCommand {
            return AvuExportCommand(
                avid = cmd.avid,
                trigger = cmd.phrase,
                action = cmd.actionType.name,
                elementAvid = cmd.targetAvid ?: "",
                confidence = cmd.confidence
            )
        }
    }
}

/**
 * Synonym set for AVU export.
 */
data class SynonymSet(
    val word: String,
    val synonyms: List<String>
) {
    /**
     * Convert to YAML-style line format.
     */
    fun toYamlLine(): String {
        val synList = synonyms.joinToString(", ") { it }
        return "  $word: [$synList]"
    }

    companion object {
        /**
         * Parse YAML-style line to SynonymSet.
         *
         * @param line Line in format "  word: [syn1, syn2]"
         * @return SynonymSet or null if parsing fails
         */
        fun fromYamlLine(line: String): SynonymSet? {
            if (!line.contains(":") || !line.contains("[")) return null

            return try {
                val word = line.substringBefore(":").trim()
                val synsStr = line.substringAfter("[").substringBefore("]")
                val synonyms = synsStr.split(",").map { it.trim() }.filter { it.isNotBlank() }
                SynonymSet(word, synonyms)
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * File writer interface for platform-specific file operations.
 * Implement this on each platform (Android, iOS, Desktop) to provide file writing.
 */
interface AvuFileWriter {
    /**
     * Write content to file.
     *
     * @param packageName Package name for filename generation
     * @param content AVU content to write
     * @param mode Export mode (affects directory selection)
     * @return Absolute file path where content was written
     */
    fun writeToFile(packageName: String, content: String, mode: ExportMode): String

    /**
     * Read content from file.
     *
     * @param filePath Absolute path to file
     * @return File content or null if file doesn't exist
     */
    fun readFromFile(filePath: String): String?

    /**
     * Check if file exists.
     *
     * @param filePath Absolute path to file
     * @return true if file exists
     */
    fun fileExists(filePath: String): Boolean
}

/**
 * AVU Exporter - Generates .vos files from exploration data.
 *
 * This is a KMP-compatible exporter that uses the existing AVUSerializer
 * for format generation and accepts a platform-specific file writer for I/O.
 *
 * @param mode Export mode (USER or DEVELOPER)
 * @param fileWriter Optional file writer for file operations
 */
class AvuExporter(
    private val mode: ExportMode = ExportMode.USER,
    private val fileWriter: AvuFileWriter? = null
) {
    companion object {
        private const val SCHEMA_VERSION = "avu-1.0"
        private const val FORMAT_VERSION = "1.0.0"
        private const val LOCALE = "en-US"
        private const val PROJECT = "voiceos"
    }

    /**
     * Export QuantizedContext to AVU format.
     *
     * Uses AVUSerializer for the core serialization, then adds
     * synonyms section if provided.
     *
     * @param context Context to export
     * @param commands Additional generated commands (optional)
     * @param synonyms Synonym sets (optional)
     * @param stats Exploration statistics (optional, calculated if not provided)
     * @return ExportResult with content and optional file path
     */
    fun export(
        context: QuantizedContext,
        commands: List<AvuExportCommand> = emptyList(),
        synonyms: List<SynonymSet> = emptyList(),
        stats: ExplorationStats? = null
    ): ExportResult {
        return try {
            val content = exportToString(context, commands, synonyms, stats)
            val lineCount = content.lines().count { it.isNotBlank() }

            val filePath = fileWriter?.writeToFile(context.packageName, content, mode)

            ExportResult(
                success = true,
                filePath = filePath,
                content = content,
                lineCount = lineCount
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                content = "",
                lineCount = 0,
                errorMessage = e.message ?: "Unknown error during export"
            )
        }
    }

    /**
     * Export to string only (no file I/O).
     *
     * @param context Context to export
     * @param commands Additional generated commands (optional)
     * @param synonyms Synonym sets (optional)
     * @param stats Exploration statistics (optional)
     * @return AVU format string
     */
    fun exportToString(
        context: QuantizedContext,
        commands: List<AvuExportCommand> = emptyList(),
        synonyms: List<SynonymSet> = emptyList(),
        stats: ExplorationStats? = null
    ): String = buildString {
        // Use AVUSerializer for core serialization, but we build custom header
        appendLine("# Avanues Universal Format v1.0")
        appendLine("# Type: VOS")
        appendLine("# Extension: .vos")
        appendLine("# Generated by: VoiceOSCoreNG v2.0")
        appendLine("# Mode: ${mode.name}")
        appendLine("# Timestamp: ${currentTimeMillis()}")

        // Schema section
        appendLine("---")
        appendLine("schema: $SCHEMA_VERSION")
        appendLine("version: $FORMAT_VERSION")
        appendLine("locale: $LOCALE")
        appendLine("project: $PROJECT")
        appendLine("metadata:")
        appendLine("  file: ${context.packageName}.vos")
        appendLine("  category: learned_app")
        appendLine("  count: ${countDataLines(context, commands)}")
        appendLine("  exploration_mode: ${if (mode == ExportMode.DEVELOPER) "developer" else "automated"}")
        appendLine("  duration_s: ${(stats?.durationMs ?: 0L) / 1000}")
        appendLine("  timestamp: ${currentTimeMillis()}")

        // Data section
        appendLine("---")

        // APP line
        appendLine(context.toAppLine())

        // STA line (statistics)
        val explorationStats = stats ?: calculateStats(context, commands)
        appendLine(explorationStats.toStaLine())

        // SCR and ELM lines
        for (screen in context.screens) {
            appendLine(screen.toScrLine())
            for (element in screen.elements) {
                appendLine(element.toElmLine())
            }
        }

        // NAV lines
        for (nav in context.navigation) {
            appendLine(nav.toNavLine())
        }

        // Existing commands from context
        for (cmd in context.knownCommands) {
            appendLine(cmd.toCmdLine())
        }

        // Additional generated commands
        for (cmd in commands) {
            appendLine(cmd.toCmdLine())
        }

        // Synonyms section (if any)
        if (synonyms.isNotEmpty()) {
            appendLine("---")
            appendLine("synonyms:")
            for (syn in synonyms) {
                appendLine(syn.toYamlLine())
            }
        }
    }

    /**
     * Parse AVU file to ParsedAvuData.
     *
     * @param filePath Path to AVU file
     * @return ParsedAvuData or null if parsing fails
     */
    fun parseAvuFile(filePath: String): ParsedAvuData? {
        val content = fileWriter?.readFromFile(filePath) ?: return null
        return parseAvuString(content)
    }

    /**
     * Parse AVU string to ParsedAvuData.
     *
     * @param avu AVU format string
     * @return ParsedAvuData or null if parsing fails
     */
    fun parseAvuString(avu: String): ParsedAvuData? {
        if (avu.isBlank()) return null

        return try {
            val lines = avu.lines()
            parseAvuLines(lines)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Convert ParsedAvuData to QuantizedContext.
     *
     * @param data Parsed AVU data
     * @return QuantizedContext or null if conversion fails
     */
    fun toQuantizedContext(data: ParsedAvuData): QuantizedContext? {
        val packageName = data.getPackageName()
        if (packageName.isBlank()) return null

        // Use AVUSerializer.parse for robust parsing
        val fullAvu = buildString {
            appendLine("# Avanues Universal Format v2.2")
            appendLine("---")
            appendLine("schema: avu-2.2")
            appendLine("---")
            appendLine(data.appLine)
            appendLine(data.statsLine)
            data.screenLines.forEach { appendLine(it) }
            data.elementLines.forEach { appendLine(it) }
            data.navigationLines.forEach { appendLine(it) }
            data.cmdLines.forEach { appendLine(it) }
        }

        return AVUSerializer.parse(fullAvu)
    }

    // ==================== Private Helpers ====================

    private fun countDataLines(context: QuantizedContext, commands: List<AvuExportCommand>): Int {
        var count = 1 // APP line
        count += 1 // STA line
        count += context.screens.size // SCR lines
        count += context.screens.sumOf { it.elements.size } // ELM lines
        count += context.navigation.size // NAV lines
        count += context.knownCommands.size // CMD lines from context
        count += commands.size // Additional CMD lines
        return count
    }

    private fun calculateStats(context: QuantizedContext, commands: List<AvuExportCommand>): ExplorationStats {
        return ExplorationStats(
            screenCount = context.screens.size,
            elementCount = context.screens.sumOf { it.elements.size },
            commandCount = context.knownCommands.size + commands.size,
            avgDepth = 2.0f, // Default
            maxDepth = 4, // Default
            coverage = 0.5f // Default
        )
    }

    private fun parseAvuLines(lines: List<String>): ParsedAvuData {
        val data = ParsedAvuData()
        var section = "header"

        for (line in lines) {
            val trimmed = line.trim()

            // Skip empty lines and comments
            if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

            // Section delimiter
            if (trimmed == "---") {
                section = when (section) {
                    "header" -> "schema"
                    "schema" -> "data"
                    "data" -> "synonyms"
                    else -> section
                }
                continue
            }

            // Parse based on section
            when (section) {
                "schema" -> parseSchemaLine(trimmed, data)
                "data" -> parseDataLine(trimmed, data)
                "synonyms" -> parseSynonymLine(trimmed, data)
            }
        }

        return data
    }

    private fun parseSchemaLine(line: String, data: ParsedAvuData) {
        val parts = line.split(":", limit = 2)
        if (parts.size == 2) {
            val key = parts[0].trim()
            val value = parts[1].trim()
            data.metadata[key] = value
        }
    }

    private fun parseDataLine(line: String, data: ParsedAvuData) {
        val code = line.substringBefore(":")

        when (code) {
            "APP" -> data.appLine = line
            "STA" -> data.statsLine = line
            "SCR" -> data.screenLines.add(line)
            "ELM" -> data.elementLines.add(line)
            "NAV" -> data.navigationLines.add(line)
            "DNC" -> data.dncLines.add(line)
            "DYN" -> data.dynLines.add(line)
            "MNU" -> data.mnuLines.add(line)
            "CMD" -> data.cmdLines.add(line)
            "CNT" -> data.contactLines.add(line)
            "SYN" -> data.synLines.add(line)
        }
    }

    private fun parseSynonymLine(line: String, data: ParsedAvuData) {
        SynonymSet.fromYamlLine(line)?.let { synSet ->
            data.synonyms[synSet.word] = synSet.synonyms
        }
    }
}

/**
 * Parsed AVU data structure.
 *
 * Contains all parsed lines from an AVU file organized by type.
 */
data class ParsedAvuData(
    val metadata: MutableMap<String, String> = mutableMapOf(),
    var appLine: String = "",
    var statsLine: String = "",
    val screenLines: MutableList<String> = mutableListOf(),
    val elementLines: MutableList<String> = mutableListOf(),
    val navigationLines: MutableList<String> = mutableListOf(),
    val dncLines: MutableList<String> = mutableListOf(),
    val dynLines: MutableList<String> = mutableListOf(),
    val mnuLines: MutableList<String> = mutableListOf(),
    val cmdLines: MutableList<String> = mutableListOf(),
    val contactLines: MutableList<String> = mutableListOf(),
    val synLines: MutableList<String> = mutableListOf(),
    val synonyms: MutableMap<String, List<String>> = mutableMapOf()
) {
    /**
     * Get package name from APP line.
     */
    fun getPackageName(): String {
        if (appLine.isEmpty()) return ""
        val parts = appLine.split(":")
        return if (parts.size > 1) parts[1] else ""
    }

    /**
     * Get app name from APP line.
     */
    fun getAppName(): String {
        if (appLine.isEmpty()) return ""
        val parts = appLine.split(":")
        return if (parts.size > 2) parts[2] else ""
    }

    /**
     * Get timestamp from APP line.
     */
    fun getTimestamp(): Long {
        if (appLine.isEmpty()) return 0L
        val parts = appLine.split(":")
        return if (parts.size > 3) parts[3].toLongOrNull() ?: 0L else 0L
    }

    /**
     * Parse exploration stats from STA line.
     */
    fun getStats(): ExplorationStats? {
        return ExplorationStats.fromStaLine(statsLine)
    }

    /**
     * Get screen count.
     */
    fun getScreenCount(): Int = screenLines.size

    /**
     * Get element count.
     */
    fun getElementCount(): Int = elementLines.size

    /**
     * Get command count.
     */
    fun getCommandCount(): Int = cmdLines.size

    /**
     * Get DNC (Do Not Click) count.
     */
    fun getDncCount(): Int = dncLines.size

    /**
     * Check if data is valid (has APP line at minimum).
     */
    fun isValid(): Boolean = appLine.isNotBlank()

    /**
     * Get all synonym sets.
     */
    fun getSynonymSets(): List<SynonymSet> {
        return synonyms.map { (word, syns) -> SynonymSet(word, syns) }
    }
}
