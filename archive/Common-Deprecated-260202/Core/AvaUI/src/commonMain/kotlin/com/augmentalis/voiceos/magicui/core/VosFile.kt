package com.augmentalis.avanues.avaui.core

/**
 * VoiceOS Plugin File (.vos) format support.
 *
 * The .vos file format is a unified extension for all VoiceOS plugin files with type flags
 * indicating the actual content format (YAML, DSL, Kotlin, JSON).
 *
 * @since 1.0.0
 */

/**
 * VoiceOS file content types.
 *
 * Each type corresponds to a type flag in the .vos file header.
 */
enum class VosFileType(
    val flag: String,
    val description: String,
    val appStoreCompliant: Boolean
) {
    /** YAML configuration format - App Store compliant */
    YAML(
        flag = "Y",
        description = "YAML configuration data",
        appStoreCompliant = true
    ),

    /** Custom DSL format - App Store compliant */
    DSL(
        flag = "D",
        description = "DSL layout definitions",
        appStoreCompliant = true
    ),

    /** Kotlin source code - Must be pre-compiled */
    KOTLIN(
        flag = "K",
        description = "Kotlin source code (generated)",
        appStoreCompliant = false // Must be compiled to binary
    ),

    /** JSON data format - App Store compliant */
    JSON(
        flag = "J",
        description = "JSON structured data",
        appStoreCompliant = true
    );

    companion object {
        /**
         * Parse type flag from .vos file header.
         *
         * @param flag Single character type flag (Y, D, K, or J)
         * @return Corresponding VosFileType
         * @throws IllegalArgumentException if flag is invalid
         */
        fun fromFlag(flag: String): VosFileType {
            return values().find { it.flag == flag }
                ?: throw IllegalArgumentException("Invalid .vos file type flag: $flag")
        }

        /**
         * Detect file type from first line of .vos file.
         *
         * Expected format: `#!vos:X` where X is the type flag.
         *
         * @param firstLine First line of the .vos file
         * @return Detected VosFileType
         * @throws IllegalArgumentException if header is invalid
         */
        fun detectFromHeader(firstLine: String): VosFileType {
            val trimmed = firstLine.trim()

            if (!trimmed.startsWith("#!vos:")) {
                throw IllegalArgumentException(
                    "Invalid .vos file header. Expected '#!vos:X' format, got: $firstLine"
                )
            }

            val flag = trimmed.removePrefix("#!vos:")
            return fromFlag(flag)
        }
    }
}

/**
 * Represents a parsed .vos file with its type and content.
 *
 * @property type The content type of the file
 * @property rawContent The complete file content including header
 * @property contentWithoutHeader File content with header lines removed
 */
data class VosFile(
    val type: VosFileType,
    val rawContent: String,
    val contentWithoutHeader: String
) {
    /**
     * Check if this file type is safe for App Store distribution.
     */
    val isAppStoreCompliant: Boolean
        get() = type.appStoreCompliant

    companion object {
        /**
         * Parse a .vos file from its content.
         *
         * @param content Complete file content as string
         * @return Parsed VosFile instance
         * @throws IllegalArgumentException if file format is invalid
         */
        fun parse(content: String): VosFile {
            if (content.isBlank()) {
                throw IllegalArgumentException("Cannot parse empty .vos file")
            }

            val lines = content.lines()
            val firstLine = lines.firstOrNull()
                ?: throw IllegalArgumentException("Empty .vos file")

            val type = VosFileType.detectFromHeader(firstLine)

            // Skip header and comment lines
            val contentLines = lines.drop(1)
                .dropWhile { line ->
                    val trimmed = line.trim()
                    trimmed.isEmpty() ||
                            trimmed.startsWith("#") ||
                            trimmed.startsWith("//")
                }

            val contentWithoutHeader = contentLines.joinToString("\n")

            return VosFile(
                type = type,
                rawContent = content,
                contentWithoutHeader = contentWithoutHeader
            )
        }

        /**
         * Create a .vos file header for the given type.
         *
         * @param type File type to create header for
         * @param description Optional description comment
         * @return Formatted header string
         */
        fun createHeader(type: VosFileType, description: String? = null): String {
            val header = buildString {
                appendLine("#!vos:${type.flag}")
                appendLine("# VoiceOS Plugin File - ${type.description}")
                appendLine("# Type Flag: Y = YAML, D = DSL, K = Kotlin, J = JSON")
                if (description != null) {
                    appendLine("# Description: $description")
                }
            }
            return header
        }
    }
}

/**
 * Exception thrown when .vos file parsing fails.
 */
class VosFileParseException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
