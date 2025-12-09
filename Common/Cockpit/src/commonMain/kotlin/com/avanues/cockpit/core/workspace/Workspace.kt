package com.avanues.cockpit.core.workspace

import com.avanues.cockpit.core.window.AppWindow
import kotlinx.serialization.Serializable

/**
 * Workspace - Collection of windows with a layout preset
 *
 * Represents a saved workspace configuration that can be restored.
 * Similar to desktop virtual desktops, but with 3D spatial positioning.
 *
 * **Voice-First Integration:**
 * - "Save workspace as work setup" → Creates named workspace
 * - "Load evening layout" → Restores saved workspace
 * - "Switch to workspace 2" → Activates different workspace
 * - "What's in this workspace?" → VoiceOS lists all windows
 *
 * @property id Unique identifier
 * @property name User-friendly name (e.g., "Work Setup", "Evening Reading")
 * @property voiceName Short name for voice commands (e.g., "work", "reading")
 * @property windows List of windows in this workspace
 * @property layoutPresetId Active layout preset (e.g., "LINEAR_HORIZONTAL")
 * @property centerPoint 3D center point for layout positioning
 * @property createdAt Creation timestamp
 * @property lastModified Last modification timestamp
 */
@Serializable
data class Workspace(
    val id: String,
    val name: String,
    val voiceName: String = name.lowercase(),
    val windows: List<AppWindow> = emptyList(),
    val layoutPresetId: String = "LINEAR_HORIZONTAL",
    val centerPoint: Vector3D = Vector3D(0f, 0f, -2f),
    val createdAt: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
) {
    companion object {
        /** Empty workspace template */
        val EMPTY = Workspace(
            id = "empty",
            name = "Empty Workspace",
            voiceName = "empty"
        )
    }

    /**
     * Adds a window to this workspace
     * Voice command: "Add [window] to this workspace"
     */
    fun addWindow(window: AppWindow): Workspace = copy(
        windows = windows + window,
        lastModified = System.currentTimeMillis()
    )

    /**
     * Removes a window from this workspace
     * Voice command: "Remove [window] from workspace"
     */
    fun removeWindow(windowId: String): Workspace = copy(
        windows = windows.filterNot { it.id == windowId },
        lastModified = System.currentTimeMillis()
    )

    /**
     * Updates a window in this workspace
     */
    fun updateWindow(windowId: String, update: (AppWindow) -> AppWindow): Workspace = copy(
        windows = windows.map { if (it.id == windowId) update(it) else it },
        lastModified = System.currentTimeMillis()
    )

    /**
     * Changes the layout preset
     * Voice commands: "Linear mode", "Arc mode", "Grid mode"
     */
    fun withLayoutPreset(presetId: String): Workspace = copy(
        layoutPresetId = presetId,
        lastModified = System.currentTimeMillis()
    )

    /**
     * Moves the workspace center point
     * Voice command: "Move workspace forward/back/left/right"
     */
    fun withCenterPoint(newCenter: Vector3D): Workspace = copy(
        centerPoint = newCenter,
        lastModified = System.currentTimeMillis()
    )

    /**
     * Renames the workspace
     * Voice command: "Rename workspace to [new name]"
     */
    fun rename(newName: String, newVoiceName: String = newName.lowercase()): Workspace = copy(
        name = newName,
        voiceName = newVoiceName,
        lastModified = System.currentTimeMillis()
    )

    /**
     * Gets window by ID
     */
    fun getWindow(windowId: String): AppWindow? = windows.find { it.id == windowId }

    /**
     * Gets window by voice name
     * Used for voice commands: "Focus [voiceName]"
     */
    fun getWindowByVoiceName(voiceName: String): AppWindow? =
        windows.find { it.voiceName.equals(voiceName, ignoreCase = true) }

    /**
     * Generates voice description for VoiceOS announcements
     *
     * Example output:
     * "Work Setup workspace with 5 windows: Gmail, browser, calculator, notes, music player"
     */
    fun toVoiceDescription(): String {
        val windowCount = windows.size
        val windowNames = windows.take(5).joinToString(", ") { it.voiceName }
        val moreText = if (windowCount > 5) " and ${windowCount - 5} more" else ""

        return "$name workspace with $windowCount window${if (windowCount != 1) "s" else ""}: $windowNames$moreText"
    }
}
