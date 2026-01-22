package com.augmentalis.avamagic.layout.loaders

import com.augmentalis.avamagic.layout.LayoutConfig

/**
 * Loads AvaUI layouts from AMF (AvaMagic Format) files.
 *
 * AMF is the preferred format for AvaMagic layouts due to:
 * - **Token Efficiency:** ~50% more compact than YAML/JSON
 * - **Parsing Speed:** Line-based format is faster to parse
 * - **AI-Friendly:** Compact format uses fewer tokens for LLM processing
 * - **Nesting Support:** Clean indentation-based hierarchy
 *
 * ## Usage Example
 *
 * ```kotlin
 * val amfContent = File("layout.amf").readText()
 * val result = AmfLayoutLoader.load(amfContent)
 *
 * result.fold(
 *     onSuccess = { layout ->
 *         println("Loaded layout: ${layout.name} with ${layout.totalComponentCount()} components")
 *     },
 *     onFailure = { error ->
 *         println("Failed to load: ${error.message}")
 *     }
 * )
 * ```
 *
 * ## File Extension
 *
 * AMF files use the `.amf` extension. The schema header indicates the content type:
 * - `schema: amf-thm-1.0` - Theme definition
 * - `schema: amf-lyt-1.0` - Layout definition
 *
 * ## Error Handling
 *
 * Returns [Result.failure] with [AmfLayoutParseException] if:
 * - File format is invalid (missing delimiters)
 * - Schema is unsupported or missing
 * - Record format is malformed
 * - Indentation is inconsistent
 *
 * @since 3.2.0
 * @see AmfLayoutParser for format specification
 * @see AmfLayoutSerializer for serialization
 */
object AmfLayoutLoader {

    /**
     * Load layout from AMF format string.
     *
     * @param amfString The AMF content to parse
     * @return Result containing LayoutConfig or error
     */
    fun load(amfString: String): Result<LayoutConfig> {
        return try {
            Result.success(AmfLayoutParser.parse(amfString))
        } catch (e: AmfLayoutParseException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(AmfLayoutParseException("Unexpected error: ${e.message}", e))
        }
    }

    /**
     * Load layout from AMF format string, throwing on error.
     *
     * @param amfString The AMF content to parse
     * @return Parsed LayoutConfig
     * @throws AmfLayoutParseException if AMF format is invalid
     */
    fun loadOrThrow(amfString: String): LayoutConfig {
        return AmfLayoutParser.parse(amfString)
    }

    /**
     * Check if content appears to be valid AMF layout format.
     *
     * Performs a quick validation without full parsing.
     *
     * @param content The content to check
     * @return true if content appears to be valid AMF layout format
     */
    fun isValidAmfFormat(content: String): Boolean {
        val lines = content.lines().map { it.trim() }
        val hasDelimiters = lines.count { it == "---" } >= 2
        val hasSchema = lines.any { it.startsWith("schema:") && it.contains("amf-lyt") }
        val hasLayoutRecord = lines.any { it.startsWith("LYT:") }
        return hasDelimiters && hasSchema && hasLayoutRecord
    }

    /**
     * Validate AMF layout content and return errors.
     *
     * @param content The content to validate
     * @return List of validation errors (empty if valid)
     */
    fun validate(content: String): List<String> {
        return AmfLayoutParser.validate(content)
    }

    /**
     * Detect layout format from file extension.
     *
     * @param fileName The file name or path
     * @return The detected format or null if unknown
     */
    fun detectFormat(fileName: String): LayoutFileFormat? {
        return when {
            fileName.endsWith(".amf") -> LayoutFileFormat.AMF
            fileName.endsWith(".yaml") || fileName.endsWith(".yml") -> LayoutFileFormat.YAML
            fileName.endsWith(".json") -> LayoutFileFormat.JSON
            fileName.endsWith(".dsl") -> LayoutFileFormat.DSL
            else -> null
        }
    }

    /**
     * Detect layout format from content.
     *
     * @param content The content to analyze
     * @return The detected format
     */
    fun detectFormatFromContent(content: String): LayoutFileFormat {
        val trimmed = content.trimStart()
        return when {
            // AMF: has --- delimiters and schema
            content.contains("---") && content.contains("schema:") && content.contains("amf-lyt") -> {
                LayoutFileFormat.AMF
            }
            // JSON: starts with { or [
            trimmed.startsWith("{") || trimmed.startsWith("[") -> {
                LayoutFileFormat.JSON
            }
            // DSL: single line with component syntax
            !content.contains("\n") && (content.contains(":") || content.contains("[")) -> {
                LayoutFileFormat.DSL
            }
            // YAML: has indentation with dashes or colons
            else -> {
                LayoutFileFormat.YAML
            }
        }
    }
}

/**
 * Supported layout file formats.
 */
enum class LayoutFileFormat {
    /** AvaMagic Format - compact line-based format with nesting (preferred) */
    AMF,

    /** YAML format - verbose but human-readable (legacy) */
    YAML,

    /** JSON format - machine-readable (legacy) */
    JSON,

    /** DSL format - ultra-compact single-line format */
    DSL
}
