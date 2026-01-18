package com.augmentalis.magicdata

/**
 * Schema definition for a database collection.
 *
 * Defines the structure of documents in a collection by specifying field names and their types.
 *
 * Example:
 * ```kotlin
 * val schema = CollectionSchema(
 *     fields = mapOf(
 *         "title" to FieldType.STRING,
 *         "completed" to FieldType.BOOLEAN,
 *         "createdAt" to FieldType.TIMESTAMP
 *     )
 * )
 * ```
 *
 * @property fields Map of field names to their types
 * @since 1.0.0
 */
data class CollectionSchema(
    val fields: Map<String, FieldType>
)
