/**
 * ActionCategory.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Categorizes action handlers
 */
package com.augmentalis.voiceoscore.accessibility.handlers

/**
 * Categories of actions that can be handled
 */
enum class ActionCategory {
    /**
     * System-level actions (settings, power, etc.)
     */
    SYSTEM,

    /**
     * App-level actions (launch, switch, close, etc.)
     */
    APP,

    /**
     * Device control actions (volume, brightness, etc.)
     */
    DEVICE,

    /**
     * Input actions (type, paste, etc.)
     */
    INPUT,

    /**
     * Navigation actions (back, home, recents, etc.)
     */
    NAVIGATION,

    /**
     * UI interaction actions (click, scroll, swipe, etc.)
     */
    UI,

    /**
     * Gesture actions (pinch, zoom, drag, etc.)
     */
    GESTURE,

    /**
     * Gaze-based actions (for eye tracking)
     */
    GAZE,

    /**
     * Media control actions (play, pause, skip, etc.)
     */
    MEDIA,

    /**
     * Accessibility actions (speak, describe, etc.)
     */
    ACCESSIBILITY,

    /**
     * Custom actions (user-defined, extensions, etc.)
     */
    CUSTOM
}
