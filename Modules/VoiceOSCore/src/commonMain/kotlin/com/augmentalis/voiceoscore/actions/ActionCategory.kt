/**
 * ActionCategory.kt - Categories of action handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2026-01-06
 *
 * Categorizes action handlers for priority-based dispatch.
 */
package com.augmentalis.voiceoscore

/**
 * Categories of actions that can be handled.
 *
 * Used by [HandlerRegistry] to prioritize handler lookup.
 */
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
     * Browser/web-specific actions (web element interaction, page navigation, etc.)
     * Priority: 11
     */
    BROWSER,

    /**
     * Note editing actions (formatting, navigation, dictation, etc.)
     * Priority: 12
     */
    NOTE,

    /**
     * Cockpit multi-window actions (frames, layouts, content management)
     * Priority: 13
     */
    COCKPIT,

    /**
     * Camera/photo actions (capture, flash, zoom, switch lens, etc.)
     * Priority: 14
     */
    CAMERA,

    /**
     * Annotation/drawing actions (pen, shapes, color, whiteboard tools)
     * Priority: 15
     */
    ANNOTATION,

    /**
     * Image viewing/editing actions (gallery, filters, rotate, crop)
     * Priority: 16
     */
    IMAGE,

    /**
     * Video playback/editing actions (play, pause, seek, speed, loop)
     * Priority: 17
     */
    VIDEO,

    /**
     * Screen casting actions (start, stop, connect, disconnect, quality)
     * Priority: 18
     */
    CAST,

    /**
     * Custom actions (user-defined, extensions, etc.)
     * Priority: 19 (lowest)
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
            BROWSER,
            NOTE,
            COCKPIT,
            CAMERA,
            ANNOTATION,
            IMAGE,
            VIDEO,
            CAST,
            CUSTOM
        )
    }
}
