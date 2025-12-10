package com.avanues.cockpit.core.window

import com.avanues.cockpit.core.workspace.Vector3D

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
    val spatialAudioEnabled: Boolean = true
)

enum class WindowType {
    ANDROID_APP,
    WEB_APP,
    REMOTE_DESKTOP,
    WIDGET
}
