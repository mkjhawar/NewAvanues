/**
 * DirectionTypes.kt - Direction enums for handlers
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-15
 *
 * Common direction types used by navigation and device handlers.
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.Bounds

/**
 * Direction for scrolling operations.
 */
enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

/**
 * Direction for volume control operations.
 */
enum class VolumeDirection {
    UP,
    DOWN,
    MUTE,
    UNMUTE
}

/**
 * Type alias for element bounds.
 * Use [com.augmentalis.voiceoscore.Bounds] directly for new code.
 */
typealias ElementBounds = Bounds
