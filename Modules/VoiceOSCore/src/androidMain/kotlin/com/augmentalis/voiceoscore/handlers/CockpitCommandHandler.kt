/**
 * CockpitCommandHandler.kt - IHandler for Cockpit multi-window voice commands
 *
 * Handles: frame management (add/minimize/maximize/close), layout switching
 * (grid/split/freeform/fullscreen/workflow), content insertion
 * (web/camera/note/pdf/image/video/whiteboard/terminal), and in-frame
 * navigation (scroll, zoom, page back/forward/refresh).
 *
 * Dispatches to the active Cockpit module via ModuleCommandCallbacks.cockpitExecutor.
 * When Cockpit is not in the foreground, commands return failure.
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
private const val MODULE_NAME = "Cockpit"

class CockpitCommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.COCKPIT

    override val supportedActions: List<String> = listOf(
        // Module lifecycle
        "open cockpit", "open hub",
        // Frame management
        "add frame", "minimize frame", "maximize frame", "close frame",
        // Layout switching (raw modes — legacy, still supported)
        "layout picker", "grid layout", "split layout",
        "freeform layout", "fullscreen layout", "workflow layout",
        // Arrangement intents (simplified — auto-selects best LayoutMode)
        "focus", "compare", "overview", "present",
        // Shell mode switching
        "cockpit mode", "map mode", "search mode", "space mode",
        "classic mode", "stream mode", "lens mode", "canvas mode",  // backward compat
        // Shell-specific navigation
        "next card", "previous card",          // MapViews
        "space zoom in", "space zoom out",     // SpaceAvanue
        // Content insertion
        "add web", "add camera", "add note", "add pdf",
        "add image", "add video", "add whiteboard", "add terminal",
        // In-frame navigation — prefixed to avoid collision with NAVIGATION handler (priority 2)
        "page back", "page forward", "page refresh",
        "frame scroll up", "frame scroll down", "frame zoom in", "frame zoom out"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        Log.d(TAG, "CockpitCommandHandler.execute: '${command.phrase}', actionType=${command.actionType}")

        val executor = ModuleCommandCallbacks.cockpitExecutor
            ?: return moduleNotActive(command.actionType)

        return try {
            executor(command.actionType, extractMetadata(command))
        } catch (e: Exception) {
            Log.e(TAG, "Cockpit command failed: ${command.actionType}", e)
            HandlerResult.failure("Cockpit command failed: ${e.message}", recoverable = true)
        }
    }

    private fun moduleNotActive(action: CommandActionType): HandlerResult {
        Log.w(TAG, "$MODULE_NAME not active for: $action")
        return HandlerResult.failure(
            "$MODULE_NAME not active — open Cockpit to use this command",
            recoverable = true,
            suggestedAction = "Say 'open cockpit' first"
        )
    }

    private fun extractMetadata(command: QuantizedCommand): Map<String, String> =
        command.metadata.mapValues { it.value.toString() }
}
