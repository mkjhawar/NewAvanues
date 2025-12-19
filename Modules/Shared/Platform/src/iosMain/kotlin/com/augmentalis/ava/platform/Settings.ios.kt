package com.augmentalis.ava.platform

import platform.Foundation.NSUserDefaults

/**
 * iOS implementation of Settings using NSUserDefaults.
 */
actual class Settings actual constructor(private val name: String) {
    private val defaults = NSUserDefaults(suiteName = name)
        ?: NSUserDefaults.standardUserDefaults

    actual fun getString(key: String, defaultValue: String): String {
        return defaults.stringForKey(key) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        defaults.setObject(value, key)
        defaults.synchronize()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return if (defaults.objectForKey(key) != null) {
            defaults.integerForKey(key).toInt()
        } else {
            defaultValue
        }
    }

    actual fun putInt(key: String, value: Int) {
        defaults.setInteger(value.toLong(), key)
        defaults.synchronize()
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return if (defaults.objectForKey(key) != null) {
            defaults.integerForKey(key)
        } else {
            defaultValue
        }
    }

    actual fun putLong(key: String, value: Long) {
        defaults.setInteger(value, key)
        defaults.synchronize()
    }

    actual fun getFloat(key: String, defaultValue: Float): Float {
        return if (defaults.objectForKey(key) != null) {
            defaults.floatForKey(key)
        } else {
            defaultValue
        }
    }

    actual fun putFloat(key: String, value: Float) {
        defaults.setFloat(value, key)
        defaults.synchronize()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return if (defaults.objectForKey(key) != null) {
            defaults.boolForKey(key)
        } else {
            defaultValue
        }
    }

    actual fun putBoolean(key: String, value: Boolean) {
        defaults.setBool(value, key)
        defaults.synchronize()
    }

    actual fun remove(key: String) {
        defaults.removeObjectForKey(key)
        defaults.synchronize()
    }

    actual fun clear() {
        val dictionary = defaults.dictionaryRepresentation()
        dictionary.keys.forEach { key ->
            defaults.removeObjectForKey(key as String)
        }
        defaults.synchronize()
    }

    actual fun contains(key: String): Boolean {
        return defaults.objectForKey(key) != null
    }
}

/**
 * iOS factory for Settings.
 */
actual object SettingsFactory {
    actual fun create(name: String): Settings {
        return Settings(name)
    }
}
