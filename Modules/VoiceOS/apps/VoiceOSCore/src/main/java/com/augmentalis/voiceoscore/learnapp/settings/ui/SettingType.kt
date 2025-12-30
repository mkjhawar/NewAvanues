/**
 * SettingType.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Defines types of settings for UI rendering
 */
package com.augmentalis.voiceoscore.learnapp.settings.ui

/**
 * Type of setting for UI rendering
 */
enum class SettingType {
    /**
     * Boolean toggle (on/off switch)
     */
    TOGGLE,

    /**
     * Integer number input
     */
    NUMBER_INT,

    /**
     * Long number input
     */
    NUMBER_LONG,

    /**
     * Float number input
     */
    NUMBER_FLOAT,

    /**
     * Text input
     */
    TEXT,

    /**
     * Selection from options
     */
    SELECT,

    /**
     * Range slider
     */
    SLIDER,

    /**
     * Action button (launches an activity or performs an action)
     */
    ACTION
}
