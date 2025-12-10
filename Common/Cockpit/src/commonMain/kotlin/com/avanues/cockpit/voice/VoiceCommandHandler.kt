package com.avanues.cockpit.voice

import com.avanues.cockpit.core.window.AppWindow
import com.avanues.cockpit.core.workspace.Vector3D
import com.avanues.cockpit.core.workspace.WorkspaceManager

/**
 * Voice Command Handler
 *
 * Parses natural language voice commands and executes them using WorkspaceManager.
 * Acts as the bridge between VoiceOS and Cockpit's core functionality.
 *
 * **Command Flow:**
 * 1. User speaks: "Open Gmail"
 * 2. VoiceOS recognizes → VoiceCommand(intent=OPEN_WINDOW, params={window="gmail"})
 * 3. VoiceCommandHandler.handle() parses and executes
 * 4. WorkspaceManager adds Gmail window
 * 5. VoiceOS announces: "Opened Gmail window"
 *
 * **Supported Commands:**
 * - Window: "Open [app]", "Close [window]", "Focus [window]"
 * - Layout: "Linear mode", "Arc mode", "Grid mode"
 * - Workspace: "Load [workspace]", "Save workspace as [name]"
 * - Navigation: "Next window", "Previous workspace"
 * - Manipulation: "Move [window] left/right/up/down", "Make [window] bigger/smaller"
 * - Zoom: "Zoom in", "Zoom out", "Reset zoom"
 * - Queries: "What workspace?", "What layout?", "How many windows?"
 *
 * **Usage:**
 * ```kotlin
 * val handler = VoiceCommandHandler(workspaceManager, voiceBridge)
 * handler.handle(VoiceCommand(
 *   rawText = "open gmail",
 *   intent = VoiceIntent.OPEN_WINDOW,
 *   parameters = mapOf("window" to "gmail")
 * ))
 * ```
 */
class VoiceCommandHandler(
    private val workspaceManager: WorkspaceManager,
    private val voiceBridge: VoiceOSBridge
) {
    /**
     * Handles a voice command
     *
     * Main entry point for voice command execution.
     * Parses intent, executes action, provides feedback.
     *
     * @param command Voice command to handle
     */
    suspend fun handle(command: VoiceCommand) {
        try {
            when (command.intent) {
                // Window Management
                VoiceIntent.OPEN_WINDOW -> handleOpenWindow(command)
                VoiceIntent.CLOSE_WINDOW -> handleCloseWindow(command)
                VoiceIntent.FOCUS_WINDOW -> handleFocusWindow(command)
                VoiceIntent.SHOW_WINDOW -> handleShowWindow(command)
                VoiceIntent.HIDE_WINDOW -> handleHideWindow(command)

                // Window Manipulation
                VoiceIntent.MOVE_WINDOW -> handleMoveWindow(command)
                VoiceIntent.RESIZE_WINDOW -> handleResizeWindow(command)
                VoiceIntent.PIN_WINDOW -> handlePinWindow(command)
                VoiceIntent.UNPIN_WINDOW -> handleUnpinWindow(command)

                // Layout Control
                VoiceIntent.SWITCH_LAYOUT -> handleSwitchLayout(command)
                VoiceIntent.MOVE_WORKSPACE -> handleMoveWorkspace(command)

                // Workspace Management
                VoiceIntent.LOAD_WORKSPACE -> handleLoadWorkspace(command)
                VoiceIntent.SAVE_WORKSPACE -> handleSaveWorkspace(command)
                VoiceIntent.NEXT_WORKSPACE -> handleNextWorkspace()
                VoiceIntent.PREV_WORKSPACE -> handlePreviousWorkspace()

                // Window Content
                VoiceIntent.READ_WINDOW -> handleReadWindow(command)
                VoiceIntent.SCROLL_WINDOW -> handleScrollWindow(command)
                VoiceIntent.ZOOM_WINDOW -> handleZoomWindow(command)
                VoiceIntent.CLICK_ELEMENT -> handleClickElement(command)

                // Queries
                VoiceIntent.QUERY_WORKSPACE -> handleQueryWorkspace()
                VoiceIntent.QUERY_LAYOUT -> handleQueryLayout()
                VoiceIntent.QUERY_WINDOWS -> handleQueryWindows()
                VoiceIntent.QUERY_CAPACITY -> handleQueryCapacity()

                // Unknown
                VoiceIntent.UNKNOWN -> handleUnknown(command)
            }
        } catch (e: Exception) {
            voiceBridge.announceAction("Error executing command: ${e.message}")
        }
    }

    // ==================== Window Management ====================

    private suspend fun handleOpenWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: command.parameters["app"] ?: return
        val url = command.parameters["url"] ?: "https://www.google.com/search?q=$windowName"

        val window = AppWindow.webApp(
            id = generateWindowId(),
            title = windowName.replaceFirstChar { it.uppercase() },
            url = url,
            voiceName = windowName.lowercase()
        )

        workspaceManager.addWindowToActive(window)
        voiceBridge.announceWindow(window)
    }

    private suspend fun handleCloseWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: return
        val window = workspaceManager.getWindowByVoiceName(windowName)

        if (window != null) {
            workspaceManager.removeWindowFromActive(window.id)
            voiceBridge.announceAction("Closed ${window.title} window")
        } else {
            voiceBridge.announceAction("Window not found: $windowName")
        }
    }

    private suspend fun handleFocusWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: return
        val window = workspaceManager.getWindowByVoiceName(windowName)

        if (window != null) {
            workspaceManager.updateWindowInActive(window.id) { it.bringToFront() }
            voiceBridge.announceWindow(window)
        } else {
            voiceBridge.announceAction("Window not found: $windowName")
        }
    }

    private suspend fun handleShowWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: return
        val window = workspaceManager.getWindowByVoiceName(windowName)

        if (window != null) {
            workspaceManager.updateWindowInActive(window.id) { it.copy(visible = true) }
            voiceBridge.announceAction("Showing ${window.title} window")
        }
    }

    private suspend fun handleHideWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: return
        val window = workspaceManager.getWindowByVoiceName(windowName)

        if (window != null) {
            workspaceManager.updateWindowInActive(window.id) { it.copy(visible = false) }
            voiceBridge.announceAction("Hiding ${window.title} window")
        }
    }

    // ==================== Window Manipulation ====================

    private suspend fun handleMoveWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: return
        val direction = command.parameters["direction"] ?: return
        val window = workspaceManager.getWindowByVoiceName(windowName)

        if (window != null) {
            val offset = when (direction.lowercase()) {
                "left" -> Vector3D(-0.2f, 0f, 0f)
                "right" -> Vector3D(0.2f, 0f, 0f)
                "up" -> Vector3D(0f, 0.2f, 0f)
                "down" -> Vector3D(0f, -0.2f, 0f)
                "forward" -> Vector3D(0f, 0f, -0.2f)
                "back" -> Vector3D(0f, 0f, 0.2f)
                else -> return
            }

            workspaceManager.updateWindowInActive(window.id) { it.moveBy(offset) }
            voiceBridge.announceAction("Moved ${window.title} $direction")
        }
    }

    private suspend fun handleResizeWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: return
        val action = command.parameters["action"] ?: return
        val window = workspaceManager.getWindowByVoiceName(windowName)

        if (window != null) {
            val updated = when (action.lowercase()) {
                "bigger" -> window.makeBigger()
                "smaller" -> window.makeSmaller()
                else -> return
            }

            workspaceManager.updateWindowInActive(window.id) { updated }
            voiceBridge.announceAction("Made ${window.title} $action")
        }
    }

    private suspend fun handlePinWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: "current"
        val window = if (windowName == "current") {
            workspaceManager.activeWorkspace.value.windows.firstOrNull()
        } else {
            workspaceManager.getWindowByVoiceName(windowName)
        }

        if (window != null) {
            workspaceManager.updateWindowInActive(window.id) { it.copy(pinned = true) }
            voiceBridge.announceAction("Pinned ${window.title} window")
        }
    }

    private suspend fun handleUnpinWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: "current"
        val window = if (windowName == "current") {
            workspaceManager.activeWorkspace.value.windows.firstOrNull { it.pinned }
        } else {
            workspaceManager.getWindowByVoiceName(windowName)
        }

        if (window != null) {
            workspaceManager.updateWindowInActive(window.id) { it.copy(pinned = false) }
            voiceBridge.announceAction("Unpinned ${window.title} window")
        }
    }

    // ==================== Layout Control ====================

    private suspend fun handleSwitchLayout(command: VoiceCommand) {
        val layoutCommand = command.parameters["layout"] ?: command.rawText

        val success = workspaceManager.applyLayoutByVoice(layoutCommand)
        if (success) {
            val description = workspaceManager.getLayoutDescription()
            voiceBridge.announceLayout(description)
        } else {
            voiceBridge.announceAction("Layout not found: $layoutCommand")
        }
    }

    private suspend fun handleMoveWorkspace(command: VoiceCommand) {
        val direction = command.parameters["direction"] ?: return

        val offset = when (direction.lowercase()) {
            "left" -> Vector3D(-0.3f, 0f, 0f)
            "right" -> Vector3D(0.3f, 0f, 0f)
            "up" -> Vector3D(0f, 0.3f, 0f)
            "down" -> Vector3D(0f, -0.3f, 0f)
            "forward" -> Vector3D(0f, 0f, -0.3f)
            "back" -> Vector3D(0f, 0f, 0.3f)
            else -> return
        }

        workspaceManager.moveWorkspace(offset)
        voiceBridge.announceAction("Moved workspace $direction")
    }

    // ==================== Workspace Management ====================

    private suspend fun handleLoadWorkspace(command: VoiceCommand) {
        val workspaceName = command.parameters["workspace"] ?: return

        val workspace = workspaceManager.loadWorkspaceByVoiceName(workspaceName)
        if (workspace != null) {
            voiceBridge.announceWorkspace(workspace)
        } else {
            voiceBridge.announceAction("Workspace not found: $workspaceName")
        }
    }

    private suspend fun handleSaveWorkspace(command: VoiceCommand) {
        val name = command.parameters["name"] ?: "Unnamed Workspace"
        val current = workspaceManager.activeWorkspace.value

        workspaceManager.saveWorkspace(current, name)
        voiceBridge.announceAction("Saved workspace as $name")
    }

    private suspend fun handleNextWorkspace() {
        workspaceManager.nextWorkspace()
        val workspace = workspaceManager.activeWorkspace.value
        voiceBridge.announceWorkspace(workspace)
    }

    private suspend fun handlePreviousWorkspace() {
        workspaceManager.previousWorkspace()
        val workspace = workspaceManager.activeWorkspace.value
        voiceBridge.announceWorkspace(workspace)
    }

    // ==================== Window Content ====================

    private suspend fun handleReadWindow(command: VoiceCommand) {
        val windowName = command.parameters["window"] ?: "current"
        val window = if (windowName == "current") {
            workspaceManager.activeWorkspace.value.windows.firstOrNull()
        } else {
            workspaceManager.getWindowByVoiceName(windowName)
        }

        if (window != null) {
            // Request accessibility info and read content
            val accessibilityInfo = voiceBridge.requestAccessibilityInfo(window.id)
            if (accessibilityInfo != null) {
                val content = extractTextFromAccessibilityNode(accessibilityInfo)
                voiceBridge.announceAction(content)
            } else {
                voiceBridge.announceAction("Cannot read window content")
            }
        }
    }

    private suspend fun handleScrollWindow(command: VoiceCommand) {
        val direction = command.parameters["direction"] ?: "down"

        // This would integrate with WebView JavaScript injection
        val windowName = command.parameters["window"] ?: "current"
        voiceBridge.announceAction("Scrolling $direction")
    }

    private suspend fun handleZoomWindow(command: VoiceCommand) {
        val action = command.parameters["action"] ?: return
        val windowName = command.parameters["window"] ?: "current"
        val window = if (windowName == "current") {
            workspaceManager.activeWorkspace.value.windows.firstOrNull()
        } else {
            workspaceManager.getWindowByVoiceName(windowName)
        }

        if (window != null) {
            val newState = when (action.lowercase()) {
                "in" -> window.state.zoomIn()
                "out" -> window.state.zoomOut()
                "reset" -> window.state.resetZoom()
                else -> return
            }

            workspaceManager.updateWindowInActive(window.id) { it.withState(newState) }
            val zoomPercent = (newState.zoomLevel * 100).toInt()
            voiceBridge.announceAction("Zoom $zoomPercent percent")
        }
    }

    private suspend fun handleClickElement(command: VoiceCommand) {
        val element = command.parameters["element"] ?: return

        // This would integrate with JavaScript injection
        voiceBridge.announceAction("Clicking $element")
    }

    // ==================== Queries ====================

    private suspend fun handleQueryWorkspace() {
        val description = workspaceManager.getActiveWorkspaceDescription()
        voiceBridge.announceAction(description)
    }

    private suspend fun handleQueryLayout() {
        val description = workspaceManager.getLayoutDescription()
        voiceBridge.announceAction(description)
    }

    private suspend fun handleQueryWindows() {
        val windows = workspaceManager.activeWorkspace.value.windows
        val windowList = windows.joinToString(", ") { it.voiceName }
        val message = if (windows.isEmpty()) {
            "No windows open"
        } else {
            "Open windows: $windowList"
        }
        voiceBridge.announceAction(message)
    }

    private suspend fun handleQueryCapacity() {
        val capacity = workspaceManager.getRemainingCapacity()
        val message = if (capacity > 0) {
            "You can add $capacity more window${if (capacity != 1) "s" else ""}"
        } else {
            "Workspace is at maximum capacity"
        }
        voiceBridge.announceAction(message)
    }

    // ==================== Unknown ====================

    private suspend fun handleUnknown(command: VoiceCommand) {
        voiceBridge.announceAction("Sorry, I didn't understand: ${command.rawText}")
    }

    // ==================== Utilities ====================

    /**
     * Extracts text content from accessibility node tree
     */
    private fun extractTextFromAccessibilityNode(node: AccessibilityNode): String {
        val parts = mutableListOf<String>()

        if (!node.text.isNullOrEmpty()) {
            parts.add(node.text)
        }

        if (!node.contentDescription.isNullOrEmpty()) {
            parts.add(node.contentDescription)
        }

        node.children.forEach { child ->
            val childText = extractTextFromAccessibilityNode(child)
            if (childText.isNotEmpty()) {
                parts.add(childText)
            }
        }

        return parts.joinToString(". ")
    }

    /**
     * Generates unique window ID
     */
    private fun generateWindowId(): String {
        return "window_${System.currentTimeMillis()}"
    }

    companion object {
        /**
         * Creates a VoiceCommandHandler with default configuration
         */
        fun createDefault(
            workspaceManager: WorkspaceManager,
            voiceBridge: VoiceOSBridge
        ): VoiceCommandHandler {
            return VoiceCommandHandler(workspaceManager, voiceBridge)
        }
    }
}
