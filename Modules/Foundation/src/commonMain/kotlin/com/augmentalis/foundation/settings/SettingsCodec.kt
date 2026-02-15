/**
 * SettingsCodec.kt - Generic settings serialization/deserialization
 *
 * Defines how a settings data class maps to/from key-value pairs
 * for platform-specific persistence backends (UserDefaults, Preferences, etc.).
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.settings

/**
 * Abstraction for reading typed values from a platform preference store.
 *
 * Implementations wrap NSUserDefaults, java.util.prefs.Preferences,
 * DataStore, etc. — exposing a uniform get-by-key API.
 */
interface PreferenceReader {
    fun getString(key: String, default: String): String
    fun getBoolean(key: String, default: Boolean): Boolean
    fun getInt(key: String, default: Int): Int
    fun getLong(key: String, default: Long): Long
    fun getFloat(key: String, default: Float): Float
    fun getStringOrNull(key: String): String?
    fun getLongOrNull(key: String): Long?
}

/**
 * Abstraction for writing typed values to a platform preference store.
 *
 * Implementations wrap NSUserDefaults, java.util.prefs.Preferences,
 * DataStore, etc. — exposing a uniform put-by-key API.
 */
interface PreferenceWriter {
    fun putString(key: String, value: String)
    fun putBoolean(key: String, value: Boolean)
    fun putInt(key: String, value: Int)
    fun putLong(key: String, value: Long)
    fun putFloat(key: String, value: Float)
    fun remove(key: String)
}

/**
 * Codec that maps a settings data class [T] to/from key-value pairs.
 *
 * Each settings model (AvanuesSettings, DeveloperSettings) implements
 * this interface to define its persistence mapping once. Platform-specific
 * ISettingsStore implementations use the codec to read/write settings
 * without knowing the data class structure.
 *
 * @param T The settings data class type
 */
interface SettingsCodec<T> {

    /**
     * The default value when no settings have been persisted yet.
     */
    val defaultValue: T

    /**
     * Decode a settings instance from the preference store.
     *
     * @param reader Platform-specific preference reader
     * @return Decoded settings instance
     */
    fun decode(reader: PreferenceReader): T

    /**
     * Encode a settings instance into the preference store.
     *
     * @param value The settings instance to persist
     * @param writer Platform-specific preference writer
     */
    fun encode(value: T, writer: PreferenceWriter)
}
