package com.augmentalis.avaelements.common.properties

/**
 * Basic Property Extractor
 *
 * Handles extraction of primitive types and enums from property maps.
 * Part of the SOLID refactoring of PropertyExtractor.
 *
 * Responsibilities:
 * - String extraction (nullable and non-nullable)
 * - Boolean extraction with flexible type coercion
 * - Numeric extraction (Int, Long, Float, Double)
 * - Enum extraction with case-insensitive matching
 *
 * SRP: Single responsibility - basic type extraction only
 * ISP: Clients depend only on basic type extraction methods
 */
object BasicPropertyExtractor {

    // ─────────────────────────────────────────────────────────────
    // String Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract string property with default
     */
    fun getString(
        props: Map<String, Any?>,
        key: String,
        default: String = ""
    ): String {
        return props[key]?.toString() ?: default
    }

    /**
     * Extract nullable string
     */
    fun getStringOrNull(props: Map<String, Any?>, key: String): String? {
        return props[key]?.toString()
    }

    // ─────────────────────────────────────────────────────────────
    // Boolean Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract boolean property with default
     * Supports: Boolean, String ("true"/"false"), Number (0/non-zero)
     */
    fun getBoolean(
        props: Map<String, Any?>,
        key: String,
        default: Boolean = false
    ): Boolean {
        return when (val value = props[key]) {
            is Boolean -> value
            is String -> value.equals("true", ignoreCase = true)
            is Number -> value.toInt() != 0
            else -> default
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Numeric Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract int property with default
     * Supports: Number (converted to Int), String (parsed)
     */
    fun getInt(
        props: Map<String, Any?>,
        key: String,
        default: Int = 0
    ): Int {
        return when (val value = props[key]) {
            is Number -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
    }

    /**
     * Extract long property with default
     * Supports: Number (converted to Long), String (parsed)
     */
    fun getLong(
        props: Map<String, Any?>,
        key: String,
        default: Long = 0L
    ): Long {
        return when (val value = props[key]) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull() ?: default
            else -> default
        }
    }

    /**
     * Extract float property with default
     * Supports: Number (converted to Float), String (parsed)
     */
    fun getFloat(
        props: Map<String, Any?>,
        key: String,
        default: Float = 0f
    ): Float {
        return when (val value = props[key]) {
            is Number -> value.toFloat()
            is String -> value.toFloatOrNull() ?: default
            else -> default
        }
    }

    /**
     * Extract double property with default
     * Supports: Number (converted to Double), String (parsed)
     */
    fun getDouble(
        props: Map<String, Any?>,
        key: String,
        default: Double = 0.0
    ): Double {
        return when (val value = props[key]) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() ?: default
            else -> default
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Enum Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract enum property with default
     * Supports:
     * - Direct enum value
     * - String with case-insensitive matching
     * - Automatic conversion of hyphens and spaces to underscores
     *
     * @param T The enum type to extract
     * @param props Property map
     * @param key Property key
     * @param default Default value if not found or invalid
     * @return The extracted enum value or default
     */
    inline fun <reified T : Enum<T>> getEnum(
        props: Map<String, Any?>,
        key: String,
        default: T
    ): T {
        val value = props[key] ?: return default
        return when (value) {
            is T -> value
            is String -> try {
                enumValueOf<T>(value.uppercase().replace("-", "_").replace(" ", "_"))
            } catch (e: IllegalArgumentException) {
                // Try case-insensitive match
                enumValues<T>().find {
                    it.name.equals(value.replace("-", "_").replace(" ", "_"), ignoreCase = true)
                } ?: default
            }
            else -> default
        }
    }
}
