package com.augmentalis.websocket

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.NSUserDefaults

private const val KEY_QUEUE = "websocket_pending_sync_operations"

/**
 * iOS implementation of persistent sync queue using NSUserDefaults
 */
actual class PersistentSyncQueue actual constructor() {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val defaults: NSUserDefaults = NSUserDefaults.standardUserDefaults

    actual suspend fun save(operations: List<QueuedOperation>) {
        withContext(Dispatchers.Main) {
            val jsonString = json.encodeToString(operations)
            defaults.setObject(jsonString, forKey = KEY_QUEUE)
            defaults.synchronize()
        }
    }

    actual suspend fun load(): List<QueuedOperation> {
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

    companion object {
        private val instance by lazy { PersistentSyncQueue() }
        fun getInstance(): PersistentSyncQueue = instance
    }
}
