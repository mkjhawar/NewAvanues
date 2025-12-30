package com.avanues.cockpit.presets

import com.avanues.cockpit.core.window.WindowContent
import com.avanues.cockpit.core.window.WindowType
import kotlinx.serialization.Serializable

/**
 * Window preset for quick window creation
 *
 * Stores window configuration (title, type, content, group) for reuse.
 * Persisted to SharedPreferences as JSON via WindowPresetManager.
 *
 * @param id Unique preset identifier (UUID)
 * @param title Window title
 * @param type Window type (WEB_APP, ANDROID_APP, etc.)
 * @param content Window content configuration (URL, package name, etc.)
 * @param groupName Group name for organization (e.g., "Work", "Personal")
 * @param color Accent color for window (#RRGGBB hex)
 * @param createdAt Creation timestamp (milliseconds since epoch)
 */
@Serializable
data class WindowPreset(
    val id: String,
    val title: String,
    val type: WindowType,
    val content: WindowContent,
    val groupName: String = "default",
    val color: String = "#4ECDC4",
    val createdAt: Long = 0L
)
