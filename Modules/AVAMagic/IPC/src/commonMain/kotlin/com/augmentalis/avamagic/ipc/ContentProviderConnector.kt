package com.augmentalis.avamagic.ipc

/**
 * Content Provider Connector
 *
 * Platform-agnostic interface for querying Content Providers.
 * Uses AvuIPCParser for serializing query results and data exchange.
 *
 * ## Usage
 * ```kotlin
 * val connector = ContentProviderConnector()
 * connector.setContext(context)  // Android only
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
 *     is Result.Success -> {
 *         result.getOrNull()?.rows?.forEach { row ->
 *             println("${row["title"]}: ${row["content"]}")
 *         }
 *     }
 *     is Result.Failure -> {
 *         println("Error: ${result.exceptionOrNull()?.message}")
 *     }
 * }
 * ```
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
expect class ContentProviderConnector() {

    /**
     * Query a Content Provider
     *
     * Executes a query and returns results serialized via AvuIPCParser format.
     *
     * @param endpoint Content Provider endpoint from ARG registry
     * @param params Query parameters including URI, projection, selection, etc.
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
     * @param uri Content URI to insert into
     * @param values Values to insert as key-value pairs
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
     * @param uri Content URI to update
     * @param values Values to update as key-value pairs
     * @param selection Optional SQL WHERE clause (without WHERE keyword)
     * @param selectionArgs Optional values for ? placeholders in selection
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
     * @param uri Content URI to delete from
     * @param selection Optional SQL WHERE clause (without WHERE keyword)
     * @param selectionArgs Optional values for ? placeholders in selection
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
     * @param uri Content URI to observe for changes
     * @param callback Callback invoked when data at URI changes
     */
    fun registerObserver(uri: String, callback: (String) -> Unit)

    /**
     * Unregister observer
     *
     * @param uri Content URI to stop observing
     */
    fun unregisterObserver(uri: String)
}

/**
 * Content Provider endpoint definition.
 *
 * Contains information needed to query a Content Provider.
 *
 * @property id Unique identifier for the provider (typically package.provider format)
 * @property authority The Content Provider authority string
 * @property uris List of available content URIs
 * @property permissions Required permissions to access this provider
 */
data class ContentProviderEndpoint(
    val id: String,
    val authority: String,
    val uris: List<String> = emptyList(),
    val permissions: List<String> = emptyList()
)
