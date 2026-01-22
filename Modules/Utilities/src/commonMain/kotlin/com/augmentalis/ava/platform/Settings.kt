package com.augmentalis.ava.platform

/**
 * Cross-platform key-value storage abstraction.
 *
 * Platform implementations:
 * - Android: SharedPreferences
 * - iOS: NSUserDefaults
 * - Desktop: java.util.prefs.Preferences
 */
expect class Settings(name: String) {

    fun getString(key: String, defaultValue: String = ""): String

    fun putString(key: String, value: String)

    fun getInt(key: String, defaultValue: Int = 0): Int

    fun putInt(key: String, value: Int)

    fun getLong(key: String, defaultValue: Long = 0L): Long

    fun putLong(key: String, value: Long)

    fun getFloat(key: String, defaultValue: Float = 0f): Float

    fun putFloat(key: String, value: Float)

    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean

    fun putBoolean(key: String, value: Boolean)

    fun remove(key: String)

    fun clear()

    fun contains(key: String): Boolean
}

/**
 * Factory for creating Settings instances.
 * Required because expect classes cannot have secondary constructors with Context.
 */
expect object SettingsFactory {
    fun create(name: String): Settings
}
