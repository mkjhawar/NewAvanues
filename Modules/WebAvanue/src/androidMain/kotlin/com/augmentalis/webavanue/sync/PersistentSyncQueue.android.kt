package com.augmentalis.webavanue.sync

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private const val PREFS_NAME = "webavanue_sync_queue"
private const val KEY_QUEUE = "pending_operations"

/**
 * Android implementation of persistent sync queue using SharedPreferences
 *
 * For production use, consider using Room or DataStore for better
 * performance with large queues.
 */
actual class PersistentSyncQueue {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var prefs: SharedPreferences? = null

    /**
     * Initialize with Android context
     * Must be called before using save/load methods
     */
    fun initialize(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    actual suspend fun save(queue: List<QueuedSyncOperation>) {
        withContext(Dispatchers.IO) {
            val jsonString = json.encodeToString(queue)
            prefs?.edit()?.putString(KEY_QUEUE, jsonString)?.apply()
        }
    }

    actual suspend fun load(): List<QueuedSyncOperation> {
        return withContext(Dispatchers.IO) {
            val jsonString = prefs?.getString(KEY_QUEUE, null)
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
}

/**
 * Singleton holder for PersistentSyncQueue with context initialization
 */
object SyncQueueProvider {
    private var instance: PersistentSyncQueue? = null

    fun initialize(context: Context): PersistentSyncQueue {
        if (instance == null) {
            instance = PersistentSyncQueue().apply {
                initialize(context.applicationContext)
            }
        }
        return instance!!
    }

    fun getInstance(): PersistentSyncQueue {
        return instance ?: throw IllegalStateException(
            "SyncQueueProvider not initialized. Call initialize(context) first."
        )
    }
}
