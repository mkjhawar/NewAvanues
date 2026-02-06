package com.avanues.avu.codec.core

/**
 * AVU Code Information - Metadata for self-documenting messages
 *
 * Each 3-letter code in the AVU format can be registered with metadata
 * that describes its purpose, format, and fields. This enables:
 * - Self-documenting file headers with code legends
 * - Runtime code validation
 * - Documentation generation
 * - AI/LLM understanding of message formats
 *
 * @author Augmentalis Engineering
 * @since AVU 2.2
 */

/**
 * Code category for grouping related codes.
 */
enum class AvuCodeCategory(val displayName: String) {
    CORE("Core"),
    VOICE("Voice"),
    SYNC("Sync"),
    RPC("RPC"),
    MEDIA("Media"),
    PLUGIN("Plugin"),
    BROWSER("Browser"),
    AI("AI"),
    SYSTEM("System"),
    HANDOVER("Handover"),
    CUSTOM("Custom")
}

/**
 * Field type for structured code definitions.
 */
enum class AvuFieldType {
    STRING,
    INT,
    LONG,
    FLOAT,
    BOOLEAN,
    TIMESTAMP,
    ENUM,
    JSON,
    DATA
}

/**
 * Field definition within a code format.
 *
 * @property name Field name (e.g., "messageId", "entityType")
 * @property type Field data type
 * @property required Whether the field is required
 * @property description Human-readable description
 * @property enumValues Possible values if type is ENUM
 */
data class AvuFieldDef(
    val name: String,
    val type: AvuFieldType,
    val required: Boolean = true,
    val description: String = "",
    val enumValues: List<String>? = null
)

/**
 * Complete metadata for an AVU 3-letter code.
 *
 * @property code The 3-letter uppercase code (e.g., "SCR", "VCM")
 * @property name Human-readable name (e.g., "Sync Create")
 * @property category Code category for grouping
 * @property format Format string showing field order (e.g., "msgId:entityType:entityId:version:data")
 * @property description Detailed description of the code's purpose
 * @property fields Structured field definitions
 * @property example Example message using this code
 * @property since Version when this code was introduced
 */
data class AvuCodeInfo(
    val code: String,
    val name: String,
    val category: AvuCodeCategory,
    val format: String,
    val description: String = "",
    val fields: List<AvuFieldDef> = emptyList(),
    val example: String? = null,
    val since: String = "1.0"
) {
    init {
        require(code.length == 3) { "Code must be exactly 3 characters: $code" }
        require(code.all { it.isUpperCase() }) { "Code must be uppercase: $code" }
    }

    /**
     * Generate a legend entry for this code.
     *
     * @param includeDescription Whether to include the description
     * @return Formatted legend line
     */
    fun toLegendEntry(includeDescription: Boolean = false): String {
        return if (includeDescription && description.isNotEmpty()) {
            "$code: $name ($format) - $description"
        } else {
            "$code: $name ($format)"
        }
    }

    /**
     * Generate a compact legend entry (code and format only).
     */
    fun toCompactLegend(): String = "$code: $format"
}
