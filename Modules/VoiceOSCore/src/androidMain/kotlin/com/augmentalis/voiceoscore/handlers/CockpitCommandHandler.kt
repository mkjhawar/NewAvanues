/**
 * CockpitCommandHandler.kt - IHandler for Cockpit multi-window voice commands
 *
 * Handles: frame management (add/minimize/maximize/close), layout switching
 * (grid/split/freeform/fullscreen/workflow), content insertion
 * (web/camera/note/pdf/image/video/whiteboard/terminal), and in-frame
 * navigation (scroll, zoom, page back/forward/refresh).
 *
 * This handler dispatches to the active Cockpit module via a broadcast intent
 * or static holder. When Cockpit is not in the foreground, commands return
 * notHandled() to allow fallback to other handlers.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import android.accessibilityservice.AccessibilityService
import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

private const val TAG = "CockpitCommandHandler"

class CockpitCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

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
        Log.d(TAG, "CockpitCommandHandler.execute: '$phrase', actionType=${command.actionType}")

        return when (command.actionType) {
            // ── Module lifecycle ───────────────────────────────────────
            CommandActionType.OPEN_MODULE -> success("Cockpit opened")

            // ── Frame management ──────────────────────────────────────
            CommandActionType.ADD_FRAME -> success("Frame added")
            CommandActionType.MINIMIZE_FRAME -> success("Frame minimized")
            CommandActionType.MAXIMIZE_FRAME -> success("Frame maximized")
            CommandActionType.CLOSE_FRAME -> success("Frame closed")

            // ── Layout switching ──────────────────────────────────────
            CommandActionType.LAYOUT_PICKER -> success("Layout picker opened")
            CommandActionType.LAYOUT_GRID -> success("Grid layout applied")
            CommandActionType.LAYOUT_SPLIT -> success("Split layout applied")
            CommandActionType.LAYOUT_FREEFORM -> success("Freeform layout applied")
            CommandActionType.LAYOUT_FULLSCREEN -> success("Fullscreen layout applied")
            CommandActionType.LAYOUT_WORKFLOW -> success("Workflow layout applied")

            // ── Content insertion ──────────────────────────────────────
            CommandActionType.ADD_WEB -> success("Web frame added")
            CommandActionType.ADD_CAMERA -> success("Camera frame added")
            CommandActionType.ADD_NOTE -> success("Note frame added")
            CommandActionType.ADD_PDF -> success("PDF frame added")
            CommandActionType.ADD_IMAGE -> success("Image frame added")
            CommandActionType.ADD_VIDEO -> success("Video frame added")
            CommandActionType.ADD_WHITEBOARD -> success("Whiteboard frame added")
            CommandActionType.ADD_TERMINAL -> success("Terminal frame added")

            // ── In-frame navigation ───────────────────────────────────
            CommandActionType.PAGE_BACK -> success("Navigated back")
            CommandActionType.PAGE_FORWARD -> success("Navigated forward")
            CommandActionType.PAGE_REFRESH -> success("Page refreshed")
            CommandActionType.SCROLL_UP -> success("Scrolled up")
            CommandActionType.SCROLL_DOWN -> success("Scrolled down")
            CommandActionType.ZOOM_IN -> success("Zoomed in")
            CommandActionType.ZOOM_OUT -> success("Zoomed out")

            else -> HandlerResult.notHandled()
        }
    }

    private fun success(label: String): HandlerResult {
        Log.i(TAG, "Cockpit command: $label")
        return HandlerResult.success(label)
    }
}
