/**
 * OverlayType.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 *
 * Defines types of overlays
 */
package com.augmentalis.voiceoscore.accessibility.ui.overlays

/**
 * Types of overlays that can be displayed
 */
enum class OverlayType {
    /**
     * Fullscreen overlay covering the entire screen
     */
    FULLSCREEN,

    /**
     * Dialog-style overlay (centered, partial screen)
     */
    DIALOG,

    /**
     * Small badge overlay (corner placement)
     */
    BADGE,

    /**
     * Toast-style temporary overlay
     */
    TOAST
}
