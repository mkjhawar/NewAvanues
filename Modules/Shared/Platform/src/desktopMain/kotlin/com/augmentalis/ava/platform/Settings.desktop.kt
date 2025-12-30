package com.augmentalis.ava.platform

import java.util.prefs.Preferences

/**
 * Desktop (JVM) implementation of Settings using java.util.prefs.Preferences.
 *
 * Uses the user preference tree with a node path based on the settings name.
 */
actual class Settings actual constructor(name: String) {

    private val prefs: Preferences = Preferences.userRoot().node("com.augmentalis.ava.$name")

    actual fun getString(key: String, defaultValue: String): String {
        return prefs.get(key, defaultValue)
    }

    actual fun putString(key: String, value: String) {
        prefs.put(key, value)
        prefs.flush()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    actual fun putInt(key: String, value: Int) {
        prefs.putInt(key, value)
        prefs.flush()
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    actual fun putLong(key: String, value: Long) {
        prefs.putLong(key, value)
        prefs.flush()
    }

    actual fun getFloat(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }

    actual fun putFloat(key: String, value: Float) {
        prefs.putFloat(key, value)
        prefs.flush()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        prefs.putBoolean(key, value)
        prefs.flush()
    }

    actual fun remove(key: String) {
        prefs.remove(key)
        prefs.flush()
    }

    actual fun clear() {
        prefs.clear()
        prefs.flush()
    }

    actual fun contains(key: String): Boolean {
        return prefs.get(key, null) != null
    }
}

/**
 * Factory for creating Settings instances on Desktop.
 */
actual object SettingsFactory {
    actual fun create(name: String): Settings {
        return Settings(name)
    }
}
