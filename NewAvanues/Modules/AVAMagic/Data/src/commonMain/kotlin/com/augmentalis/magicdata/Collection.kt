package com.augmentalis.magicdata

/**
 * Represents a collection of documents in the database.
 *
 * A collection is similar to a table in SQL databases. It stores multiple documents
 * and provides CRUD operations, querying, and indexing capabilities.
 *
 * Platform implementations:
 * - Android: Uses Room Database (stub implementation for now)
 * - iOS: Uses Core Data (stub implementation for now)
 * - JVM: Uses SQLite (stub implementation for now)
 *
 * Example:
 * ```kotlin
 * val db = DatabaseFactory.default()
 * db.createCollection("tasks")
 * val tasks = db.getCollection("tasks")
 *
 * // Insert a document
 * val id = tasks?.insert(Document(
 *     id = "",
 *     data = mapOf("title" to "Buy milk", "completed" to "false")
 * ))
 *
 * // Find documents
 * val completed = tasks?.find(Query.where("completed", "true"))
 *
 * // Update a document
 * tasks?.updateById(id, mapOf("completed" to "true"))
 *
 * // Delete a document
 * tasks?.deleteById(id)
 * ```
 *
 * @property name The name of the collection
 * @since 1.0.0
 */
expect class Collection {
    val name: String

    /**
     * Inserts a new document into the collection.
     *
     * If the document ID is empty, a unique ID will be generated automatically.
     *
     * @param document The document to insert
     * @return The ID of the inserted document
     */
    fun insert(document: Document): String

    /**
     * Finds all documents matching the query.
     *
     * @param query The query to filter and sort documents
     * @return List of matching documents
     */
    fun find(query: Query): List<Document>

    /**
     * Finds a single document by its ID.
     *
     * @param id The document ID
     * @return The document if found, null otherwise
     */
    fun findById(id: String): Document?

    /**
     * Finds the first document matching the query.
     *
     * @param query The query to filter and sort documents
     * @return The first matching document, or null if no matches
     */
    fun findOne(query: Query): Document?

    /**
     * Updates all documents matching the query.
     *
     * @param query The query to filter documents
     * @param updates Map of field names to new values
     * @return Number of documents updated
     */
    fun update(query: Query, updates: Map<String, String>): Int

    /**
     * Updates a single document by its ID.
     *
     * @param id The document ID
     * @param updates Map of field names to new values
     * @return true if the document was updated, false if not found
     */
    fun updateById(id: String, updates: Map<String, String>): Boolean

    /**
     * Deletes all documents matching the query.
     *
     * @param query The query to filter documents
     * @return Number of documents deleted
     */
    fun delete(query: Query): Int

    /**
     * Deletes a single document by its ID.
     *
     * @param id The document ID
     * @return true if the document was deleted, false if not found
     */
    fun deleteById(id: String): Boolean

    /**
     * Counts the number of documents matching the query.
     *
     * @param query The query to filter documents (null = count all)
     * @return Number of matching documents
     */
    fun count(query: Query? = null): Int

    /**
     * Creates an index on a field for faster queries.
     *
     * @param field The field name to index
     */
    fun createIndex(field: String)

    /**
     * Drops an index on a field.
     *
     * @param field The field name to remove the index from
     */
    fun dropIndex(field: String)
}
