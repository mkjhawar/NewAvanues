/**
 * IosCockpitCommandHandler.kt - iOS handler for Cockpit multi-window voice commands
 *
 * Handles: frame management (add/minimize/maximize/close), layout switching
 * (grid/split/freeform/fullscreen/workflow), content insertion
 * (web/camera/note/pdf/image/video/whiteboard/terminal), and in-frame
 * navigation (scroll, zoom, page back/forward/refresh).
 *
 * Dispatches to the active Cockpit module controller via a static holder.
 * When Cockpit is not in the foreground, commands return notHandled()
 * to allow fallback to other handlers.
 *
 * No AccessibilityService needed — uses platform-agnostic controller interface.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore

import kotlin.concurrent.Volatile
import platform.Foundation.NSLog

private const val TAG = "IosCockpitCommandHandler"

/**
 * Static holder for the active Cockpit controller on iOS.
 * Set by the Cockpit screen when it becomes active, cleared when dismissed.
 */
object IosCockpitControllerHolder {
    /**
     * Callback to execute a cockpit command by its CommandActionType.
     * Returns true if the command was handled successfully.
     */
    @Volatile
    var onCockpitCommand: ((CommandActionType) -> Boolean)? = null
}

class IosCockpitCommandHandler : BaseHandler() {

    override val category: ActionCategory = ActionCategory.COCKPIT

    override val supportedActions: List<String> = listOf(
        // Module lifecycle
        "open cockpit", "open hub",
        // Frame management
        "add frame", "minimize frame", "maximize frame", "close frame",
        // Layout switching
        "layout picker", "grid layout", "split layout",
        "freeform layout", "fullscreen layout", "workflow layout",
        // Content insertion
        "add web", "add camera", "add note", "add pdf",
        "add image", "add video", "add whiteboard", "add terminal",
        // In-frame navigation
        "page back", "page forward", "page refresh",
        "scroll up", "scroll down", "zoom in", "zoom out"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val phrase = command.phrase.lowercase().trim()
        NSLog("$TAG.execute: '$phrase', actionType=${command.actionType}")

        val handler = IosCockpitControllerHolder.onCockpitCommand
        if (handler == null) {
            NSLog("$TAG: Cockpit not active — returning notHandled")
            return HandlerResult.notHandled()
        }

        return when (command.actionType) {
            // Module lifecycle
            CommandActionType.OPEN_MODULE,
            // Frame management
            CommandActionType.ADD_FRAME,
            CommandActionType.MINIMIZE_FRAME,
            CommandActionType.MAXIMIZE_FRAME,
            CommandActionType.CLOSE_FRAME,
            // Layout switching
            CommandActionType.LAYOUT_PICKER,
            CommandActionType.LAYOUT_GRID,
            CommandActionType.LAYOUT_SPLIT,
            CommandActionType.LAYOUT_FREEFORM,
            CommandActionType.LAYOUT_FULLSCREEN,
            CommandActionType.LAYOUT_WORKFLOW,
            // Content insertion
            CommandActionType.ADD_WEB,
            CommandActionType.ADD_CAMERA,
            CommandActionType.ADD_NOTE,
            CommandActionType.ADD_PDF,
            CommandActionType.ADD_IMAGE,
            CommandActionType.ADD_VIDEO,
            CommandActionType.ADD_WHITEBOARD,
            CommandActionType.ADD_TERMINAL,
            // In-frame navigation
            CommandActionType.PAGE_BACK,
            CommandActionType.PAGE_FORWARD,
            CommandActionType.PAGE_REFRESH,
            CommandActionType.SCROLL_UP,
            CommandActionType.SCROLL_DOWN,
            CommandActionType.ZOOM_IN,
            CommandActionType.ZOOM_OUT -> {
                dispatch(handler, command.actionType)
            }
            else -> HandlerResult.notHandled()
        }
    }

    private fun dispatch(handler: (CommandActionType) -> Boolean, actionType: CommandActionType): HandlerResult {
        return if (handler.invoke(actionType)) {
            NSLog("$TAG: Cockpit command dispatched: $actionType")
            HandlerResult.success("$actionType")
        } else {
            NSLog("$TAG: Cockpit command not handled by controller: $actionType")
            HandlerResult.failure("Cockpit command failed: $actionType", recoverable = true)
        }
    }
}
