package com.augmentalis.magicdata

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.doubleOrNull

/**
 * Represents a document in a database collection.
 *
 * A document is a key-value structure with a unique ID. Provides typed getter methods
 * for accessing field values with proper type conversion.
 *
 * Example:
 * ```kotlin
 * val doc = Document(
 *     id = "task-123",
 *     data = mapOf(
 *         "title" to "Buy groceries",
 *         "completed" to false,
 *         "priority" to 5
 *     )
 * )
 *
 * val title = doc.getString("title") // "Buy groceries"
 * val completed = doc.getBoolean("completed") // false
 * val priority = doc.getInt("priority") // 5
 * ```
 *
 * @property id Unique identifier for the document
 * @property data Key-value pairs representing document fields
 * @since 1.0.0
 */
@Serializable
data class Document(
    val id: String,
    val data: Map<String, String>
) {
    /**
     * Gets a string value from the document.
     *
     * @param key The field name
     * @return The string value, or null if the field doesn't exist
     */
    fun getString(key: String): String? {
        return data[key]
    }

    /**
     * Gets an integer value from the document.
     *
     * @param key The field name
     * @return The integer value, or null if the field doesn't exist or can't be converted
     */
    fun getInt(key: String): Int? {
        return data[key]?.toIntOrNull()
    }

    /**
     * Gets a long value from the document.
     *
     * @param key The field name
     * @return The long value, or null if the field doesn't exist or can't be converted
     */
    fun getLong(key: String): Long? {
        return data[key]?.toLongOrNull()
    }

    /**
     * Gets a float value from the document.
     *
     * @param key The field name
     * @return The float value, or null if the field doesn't exist or can't be converted
     */
    fun getFloat(key: String): Float? {
        return data[key]?.toFloatOrNull()
    }

    /**
     * Gets a double value from the document.
     *
     * @param key The field name
     * @return The double value, or null if the field doesn't exist or can't be converted
     */
    fun getDouble(key: String): Double? {
        return data[key]?.toDoubleOrNull()
    }

    /**
     * Gets a boolean value from the document.
     *
     * @param key The field name
     * @return The boolean value, or null if the field doesn't exist or can't be converted
     */
    fun getBoolean(key: String): Boolean? {
        return data[key]?.toBooleanStrictOrNull()
    }

    /**
     * Gets a typed value from the document.
     *
     * @param T The expected type
     * @param key The field name
     * @return The value cast to type T, or null if the field doesn't exist
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        return data[key] as? T
    }

    /**
     * Checks if a field exists in the document.
     *
     * @param key The field name
     * @return true if the field exists, false otherwise
     */
    fun containsKey(key: String): Boolean {
        return data.containsKey(key)
    }

    /**
     * Returns all field names in the document.
     *
     * @return Set of field names
     */
    fun keys(): Set<String> {
        return data.keys
    }
}
