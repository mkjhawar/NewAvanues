/*
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC.
 * All rights reserved.
 */

package com.augmentalis.foundation.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.prefs.Preferences
import kotlin.concurrent.Volatile

/**
 * Desktop (JVM) implementation of [ISettingsStore] using java.util.prefs.Preferences.
 *
 * This store manages generic settings of type [T] by using a [SettingsCodec] to encode/decode
 * the settings object to/from the Java preferences API. Changes are detected via preference change
 * listeners and emitted through a Flow.
 *
 * @param nodePath The preferences node path (e.g., "/com/augmentalis/myapp/settings")
 * @param codec The codec responsible for encoding/decoding settings objects
 */
class JavaPreferencesSettingsStore<T>(
    private val nodePath: String,
    private val codec: SettingsCodec<T>
) : ISettingsStore<T> {

    private val prefs: Preferences = Preferences.userRoot().node(nodePath)
    private val _settings = MutableStateFlow(loadSettings())
    override val settings: Flow<T> = _settings.asStateFlow()

    @Volatile
    private var isUpdating = false

    init {
        prefs.addPreferenceChangeListener {
            synchronized(this) {
                if (!isUpdating) {
                    _settings.value = loadSettings()
                }
            }
        }
    }

    private fun loadSettings(): T = codec.decode(JavaPreferencesReader(prefs))

    override suspend fun update(block: (T) -> T) = withContext(Dispatchers.IO) {
        synchronized(this@JavaPreferencesSettingsStore) {
            isUpdating = true
            try {
                val current = loadSettings()
                val updated = block(current)
                codec.encode(updated, JavaPreferencesWriter(prefs))
                prefs.flush()
                _settings.value = updated
            } finally {
                isUpdating = false
            }
        }
    }
}

/**
 * Reader adapter that implements [PreferenceReader] on top of java.util.prefs.Preferences.
 */
private class JavaPreferencesReader(private val prefs: Preferences) : PreferenceReader {
    override fun getString(key: String, default: String): String =
        prefs.get(key, default)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        prefs.getBoolean(key, default)

    override fun getInt(key: String, default: Int): Int =
        prefs.getInt(key, default)

    override fun getLong(key: String, default: Long): Long =
        prefs.getLong(key, default)

    override fun getFloat(key: String, default: Float): Float =
        prefs.getFloat(key, default)

    override fun getStringOrNull(key: String): String? =
        prefs.get(key, null)

    override fun getLongOrNull(key: String): Long? =
        prefs.get(key, null)?.toLongOrNull()
}

/**
 * Writer adapter that implements [PreferenceWriter] on top of java.util.prefs.Preferences.
 */
private class JavaPreferencesWriter(private val prefs: Preferences) : PreferenceWriter {
    override fun putString(key: String, value: String) {
        prefs.put(key, value)
    }

    override fun putBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
    }

    override fun putInt(key: String, value: Int) {
        prefs.putInt(key, value)
    }

    override fun putLong(key: String, value: Long) {
        prefs.putLong(key, value)
    }

    override fun putFloat(key: String, value: Float) {
        prefs.putFloat(key, value)
    }

    override fun remove(key: String) {
        prefs.remove(key)
    }
}
