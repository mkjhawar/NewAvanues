/**
 * SettingItem.kt - Data class for settings UI
 *
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-12-05
 *
 * Represents a single setting item for display in the Developer Settings UI.
 */

package com.augmentalis.voiceoscore.learnapp.settings.ui

/**
 * Types of setting input controls
 */
enum class SettingType {
    /** Integer number input */
    NUMBER_INT,
    /** Long number input (for milliseconds) */
    NUMBER_LONG,
    /** Boolean toggle switch */
    TOGGLE,
    /** Slider for percentages/thresholds (0.0-1.0) */
    SLIDER
}

/**
 * Represents a single setting for UI display
 *
 * @property key Setting key in SharedPreferences
 * @property label Human-readable label
 * @property description Help text explaining the setting
 * @property value Current value
 * @property type Type of input control to display
 * @property minValue Minimum allowed value
 * @property maxValue Maximum allowed value
 */
data class SettingItem(
    val key: String,
    val label: String,
    val description: String,
    val value: Any,
    val type: SettingType,
    val minValue: Number = 0,
    val maxValue: Number = 100
) {
    /**
     * Get value as Int (for NUMBER_INT type)
     */
    fun getIntValue(): Int = when (value) {
        is Int -> value
        is Long -> value.toInt()
        is Float -> value.toInt()
        is Double -> value.toInt()
        else -> 0
    }

    /**
     * Get value as Long (for NUMBER_LONG type)
     */
    fun getLongValue(): Long = when (value) {
        is Long -> value
        is Int -> value.toLong()
        is Float -> value.toLong()
        is Double -> value.toLong()
        else -> 0L
    }

    /**
     * Get value as Float (for SLIDER type)
     */
    fun getFloatValue(): Float = when (value) {
        is Float -> value
        is Double -> value.toFloat()
        is Int -> value.toFloat()
        is Long -> value.toFloat()
        else -> 0f
    }

    /**
     * Get value as Boolean (for TOGGLE type)
     */
    fun getBooleanValue(): Boolean = when (value) {
        is Boolean -> value
        is Int -> value != 0
        else -> false
    }

    /**
     * Format value for display
     */
    fun getDisplayValue(): String = when (type) {
        SettingType.NUMBER_INT -> getIntValue().toString()
        SettingType.NUMBER_LONG -> formatMilliseconds(getLongValue())
        SettingType.TOGGLE -> if (getBooleanValue()) "On" else "Off"
        SettingType.SLIDER -> formatPercentage(getFloatValue())
    }

    private fun formatMilliseconds(ms: Long): String {
        return when {
            ms >= 60_000 -> "${ms / 60_000}m ${(ms % 60_000) / 1000}s"
            ms >= 1_000 -> "${ms / 1_000}s"
            else -> "${ms}ms"
        }
    }

    private fun formatPercentage(value: Float): String {
        return if (value <= 1f) {
            "${(value * 100).toInt()}%"
        } else {
            "${value.toInt()}%"
        }
    }
}
