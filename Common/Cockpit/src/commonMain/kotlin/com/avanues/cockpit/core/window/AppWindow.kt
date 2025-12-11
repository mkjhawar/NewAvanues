package com.avanues.cockpit.core.window

import com.avanues.cockpit.core.workspace.Vector3D
import kotlinx.serialization.Serializable

/**
 * Core domain model for a floating spatial window
 */
data class AppWindow(
    val id: String,
    val title: String,
    val type: WindowType,
    val sourceId: String,
    val position: Vector3D,
    val widthMeters: Float,
    val heightMeters: Float,
    val zLayer: Int = 0,
    val pinned: Boolean = false,
    val visible: Boolean = true,
    val voiceName: String = title,
    val voiceDescription: String = title,
    val voiceShortcuts: List<String> = emptyList(),
    val spatialAudioEnabled: Boolean = true,
    val content: WindowContent = WindowContent.MockContent,

    // Window control state (Phase 1)
    val isHidden: Boolean = false,           // Minimize state (collapsed to title bar)
    val isLarge: Boolean = false,            // Maximize state (2x size)
    val createdAt: Long = 0L,                // Creation timestamp (set by platform)
    val updatedAt: Long = 0L                 // Last update timestamp (set by platform)
)

@Serializable
enum class WindowType {
    ANDROID_APP,
    WEB_APP,
    REMOTE_DESKTOP,
    WIDGET
}
