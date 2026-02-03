package com.augmentalis.webavanue.sync

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.*

private const val KEY_QUEUE = "webavanue_pending_sync_operations"

/**
 * iOS implementation of persistent sync queue using NSUserDefaults
 *
 * For production use, consider using Core Data or file-based storage
 * for better performance with large queues.
 */
actual class PersistentSyncQueue actual constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    actual suspend fun save(queue: List<QueuedSyncOperation>) {
        withContext(Dispatchers.Main) {
            val jsonString = json.encodeToString(queue)
            defaults.setObject(jsonString, forKey = KEY_QUEUE)
            defaults.synchronize()
        }
    }

    actual suspend fun load(): List<QueuedSyncOperation> {
        return withContext(Dispatchers.Main) {
            val jsonString = defaults.stringForKey(KEY_QUEUE)
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
        withContext(Dispatchers.Main) {
            defaults.removeObjectForKey(KEY_QUEUE)
            defaults.synchronize()
        }
    }
}

/**
 * Singleton accessor for iOS PersistentSyncQueue
 */
object SyncQueueProvider {
    private val instance = PersistentSyncQueue()

    fun getInstance(): PersistentSyncQueue = instance
}
