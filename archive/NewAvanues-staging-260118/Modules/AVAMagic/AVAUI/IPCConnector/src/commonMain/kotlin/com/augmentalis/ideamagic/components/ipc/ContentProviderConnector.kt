package com.augmentalis.avamagic.components.ipc

import com.augmentalis.avamagic.components.argscanner.ContentProviderEndpoint

/**
 * Content Provider Connector
 *
 * Platform-agnostic interface for querying Content Providers.
 *
 * ## Usage
 * ```kotlin
 * val connector = ContentProviderConnector()
 *
 * val result = connector.query(
 *     endpoint,
 *     QueryParams(
 *         uri = "content://com.app.provider/notes",
 *         projection = listOf("id", "title", "content"),
 *         selection = "title LIKE ?",
 *         selectionArgs = listOf("%meeting%")
 *     )
 * )
 *
 * when (result) {
 *     is QueryResult -> {
 *         result.rows.forEach { row ->
 *             println("${row["title"]}: ${row["content"]}")
 *         }
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 */
expect class ContentProviderConnector {

    /**
     * Query a Content Provider
     *
     * @param endpoint Content Provider endpoint
     * @param params Query parameters
     * @return QueryResult with rows or error
     */
    suspend fun query(
        endpoint: ContentProviderEndpoint,
        params: QueryParams
    ): Result<QueryResult>

    /**
     * Insert data into Content Provider
     *
     * @param endpoint Content Provider endpoint
     * @param uri Content URI
     * @param values Values to insert
     * @return Result with inserted URI or error
     */
    suspend fun insert(
        endpoint: ContentProviderEndpoint,
        uri: String,
        values: Map<String, Any>
    ): Result<String>

    /**
     * Update data in Content Provider
     *
     * @param endpoint Content Provider endpoint
     * @param uri Content URI
     * @param values Values to update
     * @param selection Selection clause
     * @param selectionArgs Selection arguments
     * @return Result with number of rows updated
     */
    suspend fun update(
        endpoint: ContentProviderEndpoint,
        uri: String,
        values: Map<String, Any>,
        selection: String? = null,
        selectionArgs: List<String>? = null
    ): Result<Int>

    /**
     * Delete data from Content Provider
     *
     * @param endpoint Content Provider endpoint
     * @param uri Content URI
     * @param selection Selection clause
     * @param selectionArgs Selection arguments
     * @return Result with number of rows deleted
     */
    suspend fun delete(
        endpoint: ContentProviderEndpoint,
        uri: String,
        selection: String? = null,
        selectionArgs: List<String>? = null
    ): Result<Int>

    /**
     * Register observer for data changes
     *
     * @param uri Content URI to observe
     * @param callback Callback for changes
     */
    fun registerObserver(uri: String, callback: (String) -> Unit)

    /**
     * Unregister observer
     *
     * @param uri Content URI
     */
    fun unregisterObserver(uri: String)
}
