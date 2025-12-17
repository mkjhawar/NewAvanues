/**
 * SettingModels.kt - Data models for LearnApp settings UI
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-16
 *
 * Data models used by SettingsAdapter for the LearnApp settings screen.
 * Supports toggle, number input, and slider settings.
 */
package com.augmentalis.voiceoscore.learnapp.settings.ui

/**
 * Type of setting control.
 */
enum class SettingType {
    /** Toggle switch (boolean) */
    TOGGLE,

    /** Number input field (generic) */
    NUMBER,

    /** Integer number input field */
    NUMBER_INT,

    /** Long number input field (for timestamps/durations in ms) */
    NUMBER_LONG,

    /** Slider control */
    SLIDER
}

/**
 * Setting item data class for RecyclerView adapter.
 *
 * @property key Unique identifier for the setting
 * @property label Display label
 * @property description Optional description text
 * @property type Type of setting control
 * @property value Current value (type depends on SettingType)
 * @property minValue Minimum value for NUMBER/SLIDER types
 * @property maxValue Maximum value for NUMBER/SLIDER types
 * @property unit Unit label for NUMBER/SLIDER types (e.g., "ms", "%")
 * @property enabled Whether the setting is enabled
 * @property category Optional category for grouping
 */
data class SettingItem(
    val key: String,
    val label: String,
    val description: String? = null,
    val type: SettingType,
    val value: Any,
    val minValue: Number? = null,
    val maxValue: Number? = null,
    val unit: String? = null,
    val enabled: Boolean = true,
    val category: String? = null
) {
    /**
     * Get value as Boolean (for TOGGLE type).
     */
    fun getBooleanValue(): Boolean {
        return when (value) {
            is Boolean -> value
            is String -> value.toBoolean()
            is Number -> value.toInt() != 0
            else -> false
        }
    }

    /**
     * Get value as Int (for NUMBER/SLIDER type).
     */
    fun getIntValue(): Int {
        return when (value) {
            is Int -> value
            is Long -> value.toInt()
            is Float -> value.toInt()
            is Double -> value.toInt()
            is String -> value.toIntOrNull() ?: 0
            else -> 0
        }
    }

    /**
     * Get value as Float (for SLIDER type).
     */
    fun getFloatValue(): Float {
        return when (value) {
            is Float -> value
            is Double -> value.toFloat()
            is Int -> value.toFloat()
            is Long -> value.toFloat()
            is String -> value.toFloatOrNull() ?: 0f
            else -> 0f
        }
    }

    /**
     * Get value as Long (for NUMBER type).
     */
    fun getLongValue(): Long {
        return when (value) {
            is Long -> value
            is Int -> value.toLong()
            is Float -> value.toLong()
            is Double -> value.toLong()
            is String -> value.toLongOrNull() ?: 0L
            else -> 0L
        }
    }

    /**
     * Create a copy with updated value.
     */
    fun withValue(newValue: Any): SettingItem {
        return copy(value = newValue)
    }

    /**
     * Create a copy with enabled state.
     */
    fun withEnabled(isEnabled: Boolean): SettingItem {
        return copy(enabled = isEnabled)
    }

    companion object {
        /**
         * Create a toggle setting.
         */
        fun toggle(
            key: String,
            label: String,
            description: String? = null,
            value: Boolean = false,
            category: String? = null
        ) = SettingItem(
            key = key,
            label = label,
            description = description,
            type = SettingType.TOGGLE,
            value = value,
            category = category
        )

        /**
         * Create a number input setting (Long).
         */
        fun number(
            key: String,
            label: String,
            description: String? = null,
            value: Long,
            minValue: Long? = null,
            maxValue: Long? = null,
            unit: String? = null,
            category: String? = null
        ) = SettingItem(
            key = key,
            label = label,
            description = description,
            type = SettingType.NUMBER_LONG,
            value = value,
            minValue = minValue,
            maxValue = maxValue,
            unit = unit,
            category = category
        )

        /**
         * Create an integer number input setting.
         */
        fun numberInt(
            key: String,
            label: String,
            description: String? = null,
            value: Int,
            minValue: Int? = null,
            maxValue: Int? = null,
            unit: String? = null,
            category: String? = null
        ) = SettingItem(
            key = key,
            label = label,
            description = description,
            type = SettingType.NUMBER_INT,
            value = value,
            minValue = minValue,
            maxValue = maxValue,
            unit = unit,
            category = category
        )

        /**
         * Create a slider setting.
         */
        fun slider(
            key: String,
            label: String,
            description: String? = null,
            value: Float,
            minValue: Float = 0f,
            maxValue: Float = 100f,
            unit: String? = null,
            category: String? = null
        ) = SettingItem(
            key = key,
            label = label,
            description = description,
            type = SettingType.SLIDER,
            value = value,
            minValue = minValue,
            maxValue = maxValue,
            unit = unit,
            category = category
        )
    }
}

/**
 * Callback interface for setting value changes.
 */
interface SettingChangeListener {
    /**
     * Called when a setting value changes.
     *
     * @param settingId The ID of the changed setting
     * @param newValue The new value
     */
    fun onSettingChanged(settingId: String, newValue: Any)
}

/**
 * Settings group for categorized display.
 */
data class SettingGroup(
    val category: String,
    val title: String,
    val settings: List<SettingItem>
)
