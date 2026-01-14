package com.augmentalis.magicdata

import android.content.Context
import android.content.SharedPreferences
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.UUID

/**
 * Android implementation of Collection using SharedPreferences for simple storage.
 *
 * This is a temporary implementation using SharedPreferences for document storage.
 * Future versions will integrate Room Database for proper SQL-based collection storage.
 *
 * @property name The name of the collection
 * @property context Android application context
 * @property dbName The parent database name
 * @since 1.0.0
 */
actual class Collection(
    private val context: Context,
    actual val name: String,
    private val dbName: String
) {
    private val preferences: SharedPreferences by lazy {
        context.getSharedPreferences("${dbName}_collection_${name}", Context.MODE_PRIVATE)
    }

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Inserts a new document into the collection.
     *
     * @param document The document to insert
     * @return The ID of the inserted document
     */
    actual fun insert(document: Document): String {
        val id = if (document.id.isEmpty()) UUID.randomUUID().toString() else document.id
        val docToStore = document.copy(id = id)
        val jsonString = json.encodeToString(docToStore)
        preferences.edit().putString(id, jsonString).apply()
        return id
    }

    /**
     * Finds all documents matching the query.
     *
     * @param query The query to filter and sort documents
     * @return List of matching documents
     */
    actual fun find(query: Query): List<Document> {
        val allDocs = getAllDocuments()

        // Filter
        var filtered = if (query.filters.isEmpty()) {
            allDocs
        } else {
            allDocs.filter { doc ->
                query.filters.all { (key, value) ->
                    doc.data[key] == value
                }
            }
        }

        // Sort
        query.orderBy?.let { orderField ->
            filtered = if (query.ascending) {
                filtered.sortedBy { it.data[orderField] }
            } else {
                filtered.sortedByDescending { it.data[orderField] }
            }
        }

        // Offset
        query.offset?.let { offset ->
            filtered = filtered.drop(offset)
        }

        // Limit
        query.limit?.let { limit ->
            filtered = filtered.take(limit)
        }

        return filtered
    }

    /**
     * Finds a single document by its ID.
     *
     * @param id The document ID
     * @return The document if found, null otherwise
     */
    actual fun findById(id: String): Document? {
        val jsonString = preferences.getString(id, null) ?: return null
        return try {
            json.decodeFromString<Document>(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Finds the first document matching the query.
     *
     * @param query The query to filter and sort documents
     * @return The first matching document, or null if no matches
     */
    actual fun findOne(query: Query): Document? {
        return find(query.copy(limit = 1)).firstOrNull()
    }

    /**
     * Updates all documents matching the query.
     *
     * @param query The query to filter documents
     * @param updates Map of field names to new values
     * @return Number of documents updated
     */
    actual fun update(query: Query, updates: Map<String, String>): Int {
        val matches = find(query)
        matches.forEach { doc ->
            val newData = doc.data.toMutableMap()
            newData.putAll(updates)
            val updated = doc.copy(data = newData)
            val jsonString = json.encodeToString(updated)
            preferences.edit().putString(doc.id, jsonString).apply()
        }
        return matches.size
    }

    /**
     * Updates a single document by its ID.
     *
     * @param id The document ID
     * @param updates Map of field names to new values
     * @return true if the document was updated, false if not found
     */
    actual fun updateById(id: String, updates: Map<String, String>): Boolean {
        val doc = findById(id) ?: return false
        val newData = doc.data.toMutableMap()
        newData.putAll(updates)
        val updated = doc.copy(data = newData)
        val jsonString = json.encodeToString(updated)
        preferences.edit().putString(id, jsonString).apply()
        return true
    }

    /**
     * Deletes all documents matching the query.
     *
     * @param query The query to filter documents
     * @return Number of documents deleted
     */
    actual fun delete(query: Query): Int {
        val matches = find(query)
        val editor = preferences.edit()
        matches.forEach { doc ->
            editor.remove(doc.id)
        }
        editor.apply()
        return matches.size
    }

    /**
     * Deletes a single document by its ID.
     *
     * @param id The document ID
     * @return true if the document was deleted, false if not found
     */
    actual fun deleteById(id: String): Boolean {
        if (!preferences.contains(id)) return false
        preferences.edit().remove(id).apply()
        return true
    }

    /**
     * Counts the number of documents matching the query.
     *
     * @param query The query to filter documents (null = count all)
     * @return Number of matching documents
     */
    actual fun count(query: Query?): Int {
        return if (query == null) {
            preferences.all.size
        } else {
            find(query).size
        }
    }

    /**
     * Creates an index on a field for faster queries.
     *
     * NOTE: Stub implementation - indexing not supported in SharedPreferences.
     *
     * @param field The field name to index
     */
    actual fun createIndex(field: String) {
        // Stub - indexing will be implemented with Room Database
    }

    /**
     * Drops an index on a field.
     *
     * NOTE: Stub implementation - indexing not supported in SharedPreferences.
     *
     * @param field The field name to remove the index from
     */
    actual fun dropIndex(field: String) {
        // Stub - indexing will be implemented with Room Database
    }

    private fun getAllDocuments(): List<Document> {
        return preferences.all.values.mapNotNull { value ->
            if (value is String) {
                try {
                    json.decodeFromString<Document>(value)
                } catch (e: Exception) {
                    null
                }
            } else {
                null
            }
        }
    }
}
