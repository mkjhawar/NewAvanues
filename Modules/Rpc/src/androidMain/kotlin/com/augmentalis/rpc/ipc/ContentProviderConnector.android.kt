package com.augmentalis.rpc.ipc

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android Content Provider Connector
 *
 * Queries and modifies data via Android Content Providers.
 * Uses AvuIPCParser for serializing query results.
 *
 * @since 1.0.0
 * @author Avanues Platform Team
 */
actual class ContentProviderConnector {
    private var context: Context? = null
    private val observers = mutableMapOf<String, ContentObserver>()

    /**
     * Set Android context (required before operations)
     *
     * @param ctx Android application or activity context
     */
    fun setContext(ctx: Context) {
        this.context = ctx
    }

    /**
     * Query a Content Provider
     */
    actual suspend fun query(
        endpoint: ContentProviderEndpoint,
        params: QueryParams
    ): Result<QueryResult> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext Result.failure(
            Exception("Context not set. Call setContext() first.")
        )

        try {
            val uri = Uri.parse(params.uri)
            val projection = params.projection?.toTypedArray()
            val selectionArgs = params.selectionArgs?.toTypedArray()

            val cursor: Cursor? = ctx.contentResolver.query(
                uri,
                projection,
                params.selection,
                selectionArgs,
                params.sortOrder
            )

            cursor?.use {
                val rows = mutableListOf<Map<String, Any>>()

                while (it.moveToNext()) {
                    val row = mutableMapOf<String, Any>()

                    for (i in 0 until it.columnCount) {
                        val columnName = it.getColumnName(i)
                        val value: Any? = when (it.getType(i)) {
                            Cursor.FIELD_TYPE_STRING -> it.getString(i)
                            Cursor.FIELD_TYPE_INTEGER -> it.getLong(i)
                            Cursor.FIELD_TYPE_FLOAT -> it.getDouble(i)
                            Cursor.FIELD_TYPE_BLOB -> it.getBlob(i)
                            Cursor.FIELD_TYPE_NULL -> null
                            else -> it.getString(i)
                        }

                        if (value != null) {
                            row[columnName] = value
                        }
                    }

                    rows.add(row)

                    // Apply limit if specified
                    if (params.limit != null && rows.size >= params.limit) {
                        break
                    }
                }

                // Convert to string map for QueryResult
                val stringRows = rows.map { row ->
                    row.mapValues { (_, v) -> v.toString() }
                }

                Result.success(QueryResult(stringRows, stringRows.size))
            } ?: Result.failure(Exception("Query returned null cursor"))

        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Insert data into Content Provider
     */
    actual suspend fun insert(
        endpoint: ContentProviderEndpoint,
        uri: String,
        values: Map<String, Any>
    ): Result<String> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext Result.failure(
            Exception("Context not set. Call setContext() first.")
        )

        try {
            val contentUri = Uri.parse(uri)
            val contentValues = ContentValues().apply {
                values.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Long -> put(key, value)
                        is Float -> put(key, value)
                        is Double -> put(key, value)
                        is Boolean -> put(key, value)
                        is ByteArray -> put(key, value)
                        else -> put(key, value.toString())
                    }
                }
            }

            val insertedUri = ctx.contentResolver.insert(contentUri, contentValues)
            if (insertedUri != null) {
                Result.success(insertedUri.toString())
            } else {
                Result.failure(Exception("Insert returned null URI"))
            }

        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update data in Content Provider
     */
    actual suspend fun update(
        endpoint: ContentProviderEndpoint,
        uri: String,
        values: Map<String, Any>,
        selection: String?,
        selectionArgs: List<String>?
    ): Result<Int> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext Result.failure(
            Exception("Context not set. Call setContext() first.")
        )

        try {
            val contentUri = Uri.parse(uri)
            val contentValues = ContentValues().apply {
                values.forEach { (key, value) ->
                    when (value) {
                        is String -> put(key, value)
                        is Int -> put(key, value)
                        is Long -> put(key, value)
                        is Float -> put(key, value)
                        is Double -> put(key, value)
                        is Boolean -> put(key, value)
                        is ByteArray -> put(key, value)
                        else -> put(key, value.toString())
                    }
                }
            }

            val count = ctx.contentResolver.update(
                contentUri,
                contentValues,
                selection,
                selectionArgs?.toTypedArray()
            )

            Result.success(count)

        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Delete data from Content Provider
     */
    actual suspend fun delete(
        endpoint: ContentProviderEndpoint,
        uri: String,
        selection: String?,
        selectionArgs: List<String>?
    ): Result<Int> = withContext(Dispatchers.IO) {
        val ctx = context ?: return@withContext Result.failure(
            Exception("Context not set. Call setContext() first.")
        )

        try {
            val contentUri = Uri.parse(uri)

            val count = ctx.contentResolver.delete(
                contentUri,
                selection,
                selectionArgs?.toTypedArray()
            )

            Result.success(count)

        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Register observer for data changes
     */
    actual fun registerObserver(uri: String, callback: (String) -> Unit) {
        val ctx = context ?: return

        val contentUri = Uri.parse(uri)

        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                callback(uri)
            }

            override fun onChange(selfChange: Boolean, uri: Uri?) {
                callback(uri?.toString() ?: "")
            }
        }

        ctx.contentResolver.registerContentObserver(contentUri, true, observer)
        observers[uri] = observer
    }

    /**
     * Unregister observer
     */
    actual fun unregisterObserver(uri: String) {
        val ctx = context ?: return

        val observer = observers.remove(uri)
        if (observer != null) {
            ctx.contentResolver.unregisterContentObserver(observer)
        }
    }

    /**
     * Cleanup all observers
     */
    fun cleanup() {
        val ctx = context ?: return

        observers.values.forEach { observer ->
            ctx.contentResolver.unregisterContentObserver(observer)
        }
        observers.clear()
    }
}
