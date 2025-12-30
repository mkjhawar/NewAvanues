package com.augmentalis.voiceos.preferences

import kotlinx.serialization.Serializable

/**
 * Type-safe preference value wrapper.
 */
sealed class PreferenceValue {
    data class StringValue(val value: String) : PreferenceValue()
    data class IntValue(val value: Int) : PreferenceValue()
    data class LongValue(val value: Long) : PreferenceValue()
    data class FloatValue(val value: Float) : PreferenceValue()
    data class BooleanValue(val value: Boolean) : PreferenceValue()
    data class StringSetValue(val value: Set<String>) : PreferenceValue()

    fun asString(): String? = (this as? StringValue)?.value
    fun asInt(): Int? = (this as? IntValue)?.value
    fun asLong(): Long? = (this as? LongValue)?.value
    fun asFloat(): Float? = (this as? FloatValue)?.value
    fun asBoolean(): Boolean? = (this as? BooleanValue)?.value
    fun asStringSet(): Set<String>? = (this as? StringSetValue)?.value
}

/**
 * Configuration for preference storage.
 */
@Serializable
data class PreferenceConfig(
    val preferenceName: String = "app_preferences",
    val encrypted: Boolean = false,
    val migrateFromLegacy: Boolean = false
)

/**
 * Preference key with type information.
 */
sealed class PreferenceKey<T>(val key: String, val defaultValue: T) {
    // Common app preferences
    object Language : PreferenceKey<String>("language", "en")
    object ThemeStyle : PreferenceKey<String>("theme_style", "1")
    object ConfidenceValue : PreferenceKey<Int>("confidence_value", 50)
    object TimeoutValue : PreferenceKey<Int>("timeout_value", 5000)
    object IsSuccessEnabled : PreferenceKey<Boolean>("is_success_enabled", true)
    object IsErrorEnabled : PreferenceKey<Boolean>("is_error_enabled", true)
    object DownloadedLanguageResource : PreferenceKey<String>("downloaded_language_resource", "")
    object AllInstalledApps : PreferenceKey<String>("all_installed_apps", "")

    // Custom preference key
    class Custom<T>(key: String, defaultValue: T) : PreferenceKey<T>(key, defaultValue)
}

/**
 * Preference change listener.
 */
fun interface PreferenceChangeListener {
    fun onPreferenceChanged(key: String, newValue: PreferenceValue?)
}

/**
 * Result of preference operations.
 */
sealed class PreferenceResult<out T> {
    data class Success<T>(val value: T) : PreferenceResult<T>()
    data class Error(val message: String, val cause: Throwable? = null) : PreferenceResult<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> value
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> value
        is Error -> throw cause ?: IllegalStateException(message)
    }
}

fun <T> PreferenceResult<T>.getOrDefault(default: T): T = when (this) {
    is PreferenceResult.Success -> value
    is PreferenceResult.Error -> default
}
