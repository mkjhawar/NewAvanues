package com.augmentalis.avaelements.common.properties

/**
 * Collection Property Extractor
 *
 * Handles extraction of lists and maps from property maps.
 * Part of the SOLID refactoring of PropertyExtractor.
 *
 * Responsibilities:
 * - String list extraction with CSV support
 * - Numeric list extraction (Int, Float)
 * - Generic list extraction with custom transformers
 * - Map extraction (generic and string maps)
 *
 * SRP: Single responsibility - collection type extraction only
 * ISP: Clients depend only on collection extraction methods
 */
object CollectionPropertyExtractor {

    // ─────────────────────────────────────────────────────────────
    // List Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract string list
     * Supports:
     * - List<*> (elements converted to strings)
     * - Array<*> (elements converted to strings)
     * - String (comma-separated values)
     */
    @Suppress("UNCHECKED_CAST")
    fun getStringList(
        props: Map<String, Any?>,
        key: String,
        default: List<String> = emptyList()
    ): List<String> {
        return when (val value = props[key]) {
            is List<*> -> value.mapNotNull { it?.toString() }
            is Array<*> -> value.mapNotNull { it?.toString() }
            is String -> value.split(",").map { it.trim() }
            else -> default
        }
    }

    /**
     * Extract int list
     * Supports:
     * - List<*> (numeric elements converted to Int)
     * - Array<*> (numeric elements converted to Int)
     * - String (comma-separated numeric values)
     */
    @Suppress("UNCHECKED_CAST")
    fun getIntList(
        props: Map<String, Any?>,
        key: String,
        default: List<Int> = emptyList()
    ): List<Int> {
        return when (val value = props[key]) {
            is List<*> -> value.mapNotNull { (it as? Number)?.toInt() }
            is Array<*> -> value.mapNotNull { (it as? Number)?.toInt() }
            is String -> value.split(",").mapNotNull { it.trim().toIntOrNull() }
            else -> default
        }
    }

    /**
     * Extract float list
     * Supports:
     * - List<*> (numeric elements converted to Float)
     * - Array<*> (numeric elements converted to Float)
     * - String (comma-separated numeric values)
     */
    @Suppress("UNCHECKED_CAST")
    fun getFloatList(
        props: Map<String, Any?>,
        key: String,
        default: List<Float> = emptyList()
    ): List<Float> {
        return when (val value = props[key]) {
            is List<*> -> value.mapNotNull { (it as? Number)?.toFloat() }
            is Array<*> -> value.mapNotNull { (it as? Number)?.toFloat() }
            is String -> value.split(",").mapNotNull { it.trim().toFloatOrNull() }
            else -> default
        }
    }

    /**
     * Extract generic list with transformer
     *
     * Allows custom transformation of list elements to desired type.
     * Supports List<*> and Array<*>.
     *
     * @param T The target element type
     * @param props Property map
     * @param key Property key
     * @param transform Function to transform each element
     * @param default Default value if not found or invalid
     * @return The extracted and transformed list or default
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> getList(
        props: Map<String, Any?>,
        key: String,
        transform: (Any) -> T?,
        default: List<T> = emptyList()
    ): List<T> {
        return when (val value = props[key]) {
            is List<*> -> value.mapNotNull { it?.let(transform) }
            is Array<*> -> value.mapNotNull { it?.let(transform) }
            else -> default
        }
    }

    // ─────────────────────────────────────────────────────────────
    // Map Extraction
    // ─────────────────────────────────────────────────────────────

    /**
     * Extract nested map
     * Returns a map with String keys and Any? values
     */
    @Suppress("UNCHECKED_CAST")
    fun getMap(
        props: Map<String, Any?>,
        key: String,
        default: Map<String, Any?> = emptyMap()
    ): Map<String, Any?> {
        return (props[key] as? Map<String, Any?>) ?: default
    }

    /**
     * Extract string map
     * Returns a map with String keys and String values.
     * All values are converted to strings.
     */
    @Suppress("UNCHECKED_CAST")
    fun getStringMap(
        props: Map<String, Any?>,
        key: String,
        default: Map<String, String> = emptyMap()
    ): Map<String, String> {
        return when (val value = props[key]) {
            is Map<*, *> -> value.entries
                .filter { it.key is String && it.value != null }
                .associate { (it.key as String) to it.value.toString() }
            else -> default
        }
    }
}
