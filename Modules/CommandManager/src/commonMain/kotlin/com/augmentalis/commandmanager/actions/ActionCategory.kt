/**
 * ActionCategory.kt - Categories of action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * Categorizes action handlers for priority-based dispatch.
 */
package com.augmentalis.commandmanager.actions

import kotlinx.serialization.Serializable

/**
 * Categories of actions that can be handled.
 *
 * Used by HandlerRegistry to prioritize handler lookup.
 */
@Serializable
enum class ActionCategory {
    /**
     * System-level actions (settings, power, etc.)
     * Priority: 1 (highest)
     */
    SYSTEM,

    /**
     * Navigation actions (back, home, recents, scroll, swipe, etc.)
     * Priority: 2
     */
    NAVIGATION,

    /**
     * App-level actions (launch, switch, close, etc.)
     * Priority: 3
     */
    APP,

    /**
     * Gaze-based actions (for eye tracking)
     * Priority: 4
     */
    GAZE,

    /**
     * Gesture actions (pinch, zoom, drag, etc.)
     * Priority: 5
     */
    GESTURE,

    /**
     * UI interaction actions (click, tap, press, etc.)
     * Priority: 6
     */
    UI,

    /**
     * Device control actions (volume, brightness, etc.)
     * Priority: 7
     */
    DEVICE,

    /**
     * Input actions (type, paste, etc.)
     * Priority: 8
     */
    INPUT,

    /**
     * Media control actions (play, pause, skip, etc.)
     * Priority: 9
     */
    MEDIA,

    /**
     * Accessibility actions (speak, describe, etc.)
     * Priority: 10
     */
    ACCESSIBILITY,

    /**
     * Custom actions (user-defined, extensions, etc.)
     * Priority: 11 (lowest)
     */
    CUSTOM;

    companion object {
        /**
         * Priority order for handler lookup.
         * System commands have highest priority, custom commands lowest.
         */
        val PRIORITY_ORDER: List<ActionCategory> = listOf(
            SYSTEM,
            NAVIGATION,
            APP,
            GAZE,
            GESTURE,
            UI,
            DEVICE,
            INPUT,
            MEDIA,
            ACCESSIBILITY,
            CUSTOM
        )
    }
}
