/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */
package com.augmentalis.foundation.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSUserDefaultsDidChangeNotification

/**
 * iOS settings store backed by NSUserDefaults.
 *
 * This implementation uses NSUserDefaults (iOS UserDefaults) as the persistent
 * storage layer and emits changes via Kotlin Flow. The [SettingsCodec] is responsible
 * for translating between the generic type T and the key-value operations provided
 * by the [PreferenceReader] and [PreferenceWriter] interfaces.
 *
 * NSUserDefaults notifications are observed to keep the Flow state synchronized
 * with external changes (e.g., when settings are modified from another process
 * or the system resets them).
 */
class UserDefaultsSettingsStore<T>(
    suiteName: String? = null,
    private val codec: SettingsCodec<T>
) : ISettingsStore<T> {
    private val defaults: NSUserDefaults = if (suiteName != null)
        NSUserDefaults(suiteName = suiteName) else NSUserDefaults.standardUserDefaults

    private val reader = UserDefaultsReader()
    private val writer = UserDefaultsWriter()
    private val _settings = MutableStateFlow(codec.decode(reader))
    override val settings: Flow<T> = _settings.asStateFlow()
    private var observer: Any? = null

    init {
        observer = NSNotificationCenter.defaultCenter.addObserverForName(
            name = NSUserDefaultsDidChangeNotification,
            `object` = defaults,
            queue = null
        ) { _ ->
            _settings.value = codec.decode(reader)
        }
    }

    /** Remove the NSNotificationCenter observer. Call when the store is no longer needed. */
    fun close() {
        observer?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        observer = null
    }

    private val updateMutex = Mutex()

    override suspend fun update(block: (T) -> T) = updateMutex.withLock {
        val current = codec.decode(reader)
        val updated = block(current)
        codec.encode(updated, writer)
        defaults.synchronize()
        _settings.value = updated
    }

    /**
     * Bridge between NSUserDefaults and the [PreferenceReader] interface.
     * Handles type conversions and default value fallbacks.
     */
    private inner class UserDefaultsReader : PreferenceReader {
        override fun getString(key: String, default: String): String =
            defaults.stringForKey(key) ?: default

        override fun getBoolean(key: String, default: Boolean): Boolean =
            if (defaults.objectForKey(key) != null) defaults.boolForKey(key) else default

        override fun getInt(key: String, default: Int): Int =
            if (defaults.objectForKey(key) != null) defaults.integerForKey(key).toInt() else default

        override fun getLong(key: String, default: Long): Long =
            if (defaults.objectForKey(key) != null) defaults.integerForKey(key) else default

        override fun getFloat(key: String, default: Float): Float =
            if (defaults.objectForKey(key) != null) defaults.floatForKey(key) else default

        override fun getStringOrNull(key: String): String? = defaults.stringForKey(key)

        override fun getLongOrNull(key: String): Long? =
            if (defaults.objectForKey(key) != null) defaults.integerForKey(key) else null
    }

    /**
     * Bridge between the [PreferenceWriter] interface and NSUserDefaults.
     * Persists all updates immediately.
     */
    private inner class UserDefaultsWriter : PreferenceWriter {
        override fun putString(key: String, value: String) {
            defaults.setObject(value, forKey = key)
        }

        override fun putBoolean(key: String, value: Boolean) {
            defaults.setBool(value, forKey = key)
        }

        override fun putInt(key: String, value: Int) {
            defaults.setInteger(value.toLong(), forKey = key)
        }

        override fun putLong(key: String, value: Long) {
            defaults.setInteger(value, forKey = key)
        }

        override fun putFloat(key: String, value: Float) {
            defaults.setFloat(value, forKey = key)
        }

        override fun remove(key: String) {
            defaults.removeObjectForKey(key)
        }
    }
}
