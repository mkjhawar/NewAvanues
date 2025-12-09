package com.avanues.cockpit.layout

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.core.workspace.Workspace
import com.avanues.cockpit.layout.presets.LinearHorizontalLayout

/**
 * Layout Engine
 *
 * Manages layout presets and applies them to workspaces.
 * Handles window positioning, rotation, and dimension calculations.
 *
 * **Voice-First Integration:**
 * - "Linear mode" → Switches layout and repositions windows
 * - "Arc mode" → Switches to arc layout (when implemented)
 * - "How many windows can I have?" → Checks maxWindows for current layout
 *
 * **Usage:**
 * ```kotlin
 * val engine = LayoutEngine()
 * val workspace = loadWorkspace()
 * val updatedWorkspace = engine.applyLayout(workspace, "LINEAR_HORIZONTAL")
 * ```
 */
class LayoutEngine {
    /**
     * Registry of available layout presets
     * Maps layout ID to LayoutPreset implementation
     */
    private val presets: MutableMap<String, LayoutPreset> = mutableMapOf()

    init {
        // Register built-in layouts
        registerPreset(LinearHorizontalLayout)

        // Additional presets will be registered in Phase 4:
        // registerPreset(Arc3FrontLayout)
        // registerPreset(Grid2x2Layout)
        // registerPreset(StackCenterLayout)
        // registerPreset(TheaterLayout)
    }

    /**
     * Registers a layout preset
     *
     * @param preset LayoutPreset implementation to register
     */
    fun registerPreset(preset: LayoutPreset) {
        presets[preset.id] = preset
    }

    /**
     * Unregisters a layout preset
     *
     * @param presetId Layout preset ID to remove
     */
    fun unregisterPreset(presetId: String) {
        presets.remove(presetId)
    }

    /**
     * Gets a layout preset by ID
     *
     * @param presetId Layout preset identifier
     * @return LayoutPreset or null if not found
     */
    fun getPreset(presetId: String): LayoutPreset? {
        return presets[presetId]
    }

    /**
     * Gets a layout preset by voice command
     *
     * Voice: "Linear mode" → Returns LinearHorizontalLayout
     *
     * @param voiceCommand Voice command string
     * @return LayoutPreset or null if not found
     */
    fun getPresetByVoiceCommand(voiceCommand: String): LayoutPreset? {
        return presets.values.find {
            it.voiceCommand.equals(voiceCommand, ignoreCase = true)
        }
    }

    /**
     * Gets all registered layout presets
     *
     * Voice: "What layouts are available?" → Lists all presets
     *
     * @return List of available presets
     */
    fun getAllPresets(): List<LayoutPreset> {
        return presets.values.toList()
    }

    /**
     * Applies a layout preset to a workspace
     *
     * Repositions all windows according to the layout preset's algorithm.
     * Updates window positions, rotations, and dimensions.
     *
     * Voice: "Linear mode" → applyLayout(workspace, "LINEAR_HORIZONTAL")
     *
     * @param workspace Workspace to update
     * @param presetId Layout preset ID to apply
     * @param centerPoint Optional center point override
     * @return Updated workspace with new window positions
     */
    fun applyLayout(
        workspace: Workspace,
        presetId: String,
        centerPoint: Vector3D? = null
    ): Workspace {
        val preset = presets[presetId]
            ?: return workspace // No preset found, return unchanged

        // Check if layout can accommodate window count
        if (!preset.canAccommodate(workspace.windows.size)) {
            // Too many or too few windows for this layout
            // In .yolo mode, we'll trim or allow it anyway
            // Could log a warning here
        }

        // Use provided center point or workspace's current center
        val center = centerPoint ?: workspace.centerPoint

        // Calculate positions for all windows
        val positions = preset.calculatePositions(workspace.windows, center)

        // Create position lookup map
        val positionMap = positions.associateBy { it.windowId }

        // Update each window with new position, rotation, and dimensions
        val updatedWindows = workspace.windows.mapIndexed { index, window ->
            val windowPos = positionMap[window.id]
            val dimensions = preset.calculateDimensions(window, index, workspace.windows.size)

            if (windowPos != null) {
                window.copy(
                    position = windowPos.position,
                    widthMeters = dimensions.widthMeters,
                    heightMeters = dimensions.heightMeters
                    // Note: rotationX/Y/Z would be stored in window if we add those fields
                )
            } else {
                window // No position calculated, keep as-is
            }
        }

        // Return updated workspace
        return workspace.copy(
            windows = updatedWindows,
            layoutPresetId = presetId,
            centerPoint = center,
            lastModified = System.currentTimeMillis()
        )
    }

    /**
     * Applies layout using voice command
     *
     * Voice: "Linear mode" → Resolves command and applies layout
     *
     * @param workspace Workspace to update
     * @param voiceCommand Voice command for layout
     * @param centerPoint Optional center point override
     * @return Updated workspace or null if command not recognized
     */
    fun applyLayoutByVoice(
        workspace: Workspace,
        voiceCommand: String,
        centerPoint: Vector3D? = null
    ): Workspace? {
        val preset = getPresetByVoiceCommand(voiceCommand)
            ?: return null

        return applyLayout(workspace, preset.id, centerPoint)
    }

    /**
     * Adds a window to workspace and re-applies layout
     *
     * Automatically repositions all windows to accommodate the new window.
     *
     * Voice: "Add Gmail window" → Adds window and re-layouts
     *
     * @param workspace Current workspace
     * @param window Window to add
     * @return Updated workspace with new window and adjusted layout
     */
    fun addWindowWithLayout(
        workspace: Workspace,
        window: AppWindow
    ): Workspace {
        // Add window to workspace
        val workspaceWithNewWindow = workspace.addWindow(window)

        // Re-apply current layout to reposition all windows
        return applyLayout(
            workspaceWithNewWindow,
            workspaceWithNewWindow.layoutPresetId
        )
    }

    /**
     * Removes a window from workspace and re-applies layout
     *
     * Automatically repositions remaining windows after removal.
     *
     * Voice: "Remove browser window" → Removes window and re-layouts
     *
     * @param workspace Current workspace
     * @param windowId Window ID to remove
     * @return Updated workspace with window removed and adjusted layout
     */
    fun removeWindowWithLayout(
        workspace: Workspace,
        windowId: String
    ): Workspace {
        // Remove window from workspace
        val workspaceWithoutWindow = workspace.removeWindow(windowId)

        // Re-apply current layout to reposition remaining windows
        return applyLayout(
            workspaceWithoutWindow,
            workspaceWithoutWindow.layoutPresetId
        )
    }

    /**
     * Checks if current layout can accommodate more windows
     *
     * Voice: "Can I add another window?" → Checks capacity
     *
     * @param workspace Current workspace
     * @return True if more windows can be added
     */
    fun canAddWindow(workspace: Workspace): Boolean {
        val preset = presets[workspace.layoutPresetId] ?: return false
        return workspace.windows.size < preset.maxWindows
    }

    /**
     * Gets remaining window capacity for current layout
     *
     * Voice: "How many more windows can I add?" → Returns capacity
     *
     * @param workspace Current workspace
     * @return Number of additional windows that can be added
     */
    fun getRemainingCapacity(workspace: Workspace): Int {
        val preset = presets[workspace.layoutPresetId] ?: return 0
        return (preset.maxWindows - workspace.windows.size).coerceAtLeast(0)
    }

    /**
     * Moves workspace center point and re-applies layout
     *
     * Voice: "Move workspace forward/back/left/right"
     *
     * @param workspace Current workspace
     * @param offset Vector to move by
     * @return Updated workspace with new center point
     */
    fun moveWorkspace(
        workspace: Workspace,
        offset: Vector3D
    ): Workspace {
        val newCenter = workspace.centerPoint + offset
        return applyLayout(workspace, workspace.layoutPresetId, newCenter)
    }

    /**
     * Gets voice description of current layout
     *
     * Voice: "What layout am I using?" → VoiceOS announces this
     *
     * @param workspace Current workspace
     * @return Voice-friendly layout description
     */
    fun getLayoutDescription(workspace: Workspace): String {
        val preset = presets[workspace.layoutPresetId]
        return if (preset != null) {
            "${preset.description} with ${workspace.windows.size} window${if (workspace.windows.size != 1) "s" else ""}"
        } else {
            "Unknown layout with ${workspace.windows.size} windows"
        }
    }

    /**
     * Lists all available voice commands for layouts
     *
     * Voice: "What layout modes are available?" → VoiceOS reads this list
     *
     * @return List of voice commands
     */
    fun getAvailableVoiceCommands(): List<String> {
        return presets.values.map { it.voiceCommand }
    }

    companion object {
        /** Default layout preset ID */
        const val DEFAULT_PRESET = "LINEAR_HORIZONTAL"

        /**
         * Creates a LayoutEngine with default configuration
         */
        fun createDefault(): LayoutEngine {
            return LayoutEngine()
        }
    }
}
