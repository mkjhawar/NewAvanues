package com.augmentalis.ava.platform

import android.content.Context
import android.content.SharedPreferences

/**
 * Android implementation of Settings using SharedPreferences.
 */
actual class Settings actual constructor(private val name: String) {
    private lateinit var prefs: SharedPreferences

    internal fun init(context: Context) {
        prefs = context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    actual fun getString(key: String, defaultValue: String): String {
        return prefs.getString(key, defaultValue) ?: defaultValue
    }

    actual fun putString(key: String, value: String) {
        prefs.edit().putString(key, value).apply()
    }

    actual fun getInt(key: String, defaultValue: Int): Int {
        return prefs.getInt(key, defaultValue)
    }

    actual fun putInt(key: String, value: Int) {
        prefs.edit().putInt(key, value).apply()
    }

    actual fun getLong(key: String, defaultValue: Long): Long {
        return prefs.getLong(key, defaultValue)
    }

    actual fun putLong(key: String, value: Long) {
        prefs.edit().putLong(key, value).apply()
    }

    actual fun getFloat(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }

    actual fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    actual fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    actual fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    actual fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    actual fun clear() {
        prefs.edit().clear().apply()
    }

    actual fun contains(key: String): Boolean {
        return prefs.contains(key)
    }

    companion object {
        private var appContext: Context? = null

        fun initialize(context: Context) {
            appContext = context.applicationContext
        }

        internal fun getContext(): Context {
            return appContext ?: throw IllegalStateException(
                "Settings not initialized. Call Settings.initialize(context) in Application.onCreate()"
            )
        }
    }
}

/**
 * Android factory for Settings.
 */
actual object SettingsFactory {
    actual fun create(name: String): Settings {
        return Settings(name).apply {
            init(Settings.getContext())
        }
    }
}
