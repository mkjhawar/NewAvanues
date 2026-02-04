package com.augmentalis.websocket

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "websocket_sync_queue"
private const val KEY_QUEUE = "pending_operations"

/**
 * Android implementation of persistent sync queue using SharedPreferences
 */
actual class PersistentSyncQueue actual constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var prefs: SharedPreferences? = null

    /**
     * Initialize with Android context
     * Must be called before use
     */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual suspend fun save(operations: List<QueuedOperation>) {
        withContext(Dispatchers.IO) {
            val preferences = prefs ?: throw IllegalStateException("PersistentSyncQueue not initialized")
            val jsonString = json.encodeToString(operations)
            preferences.edit().putString(KEY_QUEUE, jsonString).apply()
        }
    }

    actual suspend fun load(): List<QueuedOperation> {
        return withContext(Dispatchers.IO) {
            val preferences = prefs ?: return@withContext emptyList()
            val jsonString = preferences.getString(KEY_QUEUE, null)
            if (jsonString != null) {
                try {
                    json.decodeFromString(jsonString)
                } catch (e: Exception) {
                    emptyList()
                }
            } else {
                emptyList()
            }
        }
    }

    actual suspend fun clear() {
        withContext(Dispatchers.IO) {
            prefs?.edit()?.remove(KEY_QUEUE)?.apply()
        }
    }

    companion object {
        @Volatile
        private var instance: PersistentSyncQueue? = null

        fun getInstance(context: Context): PersistentSyncQueue {
            return instance ?: synchronized(this) {
                instance ?: PersistentSyncQueue().also {
                    it.init(context.applicationContext)
                    instance = it
                }
            }
        }
    }
}
