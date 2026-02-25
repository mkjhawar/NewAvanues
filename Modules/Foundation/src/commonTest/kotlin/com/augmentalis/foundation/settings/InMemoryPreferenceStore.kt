/**
 * InMemoryPreferenceStore.kt - In-memory test double for codec roundtrip tests
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

/**
 * Minimal in-memory implementation of [PreferenceReader] and [PreferenceWriter]
 * for use in commonTest. Stores all types in a single String map via toString/parse.
 */
class InMemoryPreferenceStore : PreferenceReader, PreferenceWriter {

    private val store = mutableMapOf<String, String>()

    // ---- PreferenceReader --------------------------------------------------

    override fun getString(key: String, default: String): String = store[key] ?: default
    override fun getStringOrNull(key: String): String? = store[key]
    override fun getBoolean(key: String, default: Boolean): Boolean =
        store[key]?.toBooleanStrictOrNull() ?: default
    override fun getInt(key: String, default: Int): Int =
        store[key]?.toIntOrNull() ?: default
    override fun getLong(key: String, default: Long): Long =
        store[key]?.toLongOrNull() ?: default
    override fun getLongOrNull(key: String): Long? = store[key]?.toLongOrNull()
    override fun getFloat(key: String, default: Float): Float =
        store[key]?.toFloatOrNull() ?: default

    // ---- PreferenceWriter --------------------------------------------------

    override fun putString(key: String, value: String) { store[key] = value }
    override fun putBoolean(key: String, value: Boolean) { store[key] = value.toString() }
    override fun putInt(key: String, value: Int) { store[key] = value.toString() }
    override fun putLong(key: String, value: Long) { store[key] = value.toString() }
    override fun putFloat(key: String, value: Float) { store[key] = value.toString() }
    override fun remove(key: String) { store.remove(key) }
}
