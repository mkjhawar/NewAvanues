package com.augmentalis.cockpit.model

import kotlinx.serialization.Serializable

/**
 * Visual and positional state of a Cockpit frame window.
 *
 * In FREEFORM mode, posX/posY/width/height are user-controlled via drag/resize.
 * In other layout modes, the LayoutEngine overrides position/size but preserves
 * the freeform values so switching back restores the user's arrangement.
 */
@Serializable
data class FrameState(
    /** X position in dp (relative to canvas origin) */
    val posX: Float = 0f,
    /** Y position in dp (relative to canvas origin) */
    val posY: Float = 0f,
    /** Frame width in dp */
    val width: Float = 400f,
    /** Frame height in dp */
    val height: Float = 300f,
    /** Z-order: higher values render on top */
    val zOrder: Int = 0,
    /** Collapsed to title bar only */
    val isMinimized: Boolean = false,
    /** Expanded to fill available space */
    val isMaximized: Boolean = false,
    /** Hidden from view (in minimized frames tray) */
    val isVisible: Boolean = true,
    /** Currently selected/focused frame */
    val isSelected: Boolean = false,
)
