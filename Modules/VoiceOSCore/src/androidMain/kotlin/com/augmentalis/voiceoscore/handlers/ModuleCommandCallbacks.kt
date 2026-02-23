/**
 * ModuleCommandCallbacks.kt - Central callback registry for module command handlers
 *
 * Each content module (NoteAvanue, Cockpit, AnnotationAvanue, ImageAvanue,
 * VideoAvanue, RemoteCast, AI) registers a command executor when its screen
 * becomes active. Handlers invoke these executors to dispatch commands to
 * the actual module controllers.
 *
 * Pattern follows VoiceControlCallbacks — static @Volatile lambdas, set by
 * the module UI layer (Compose DisposableEffect), cleared on deactivation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.voiceoscore.handlers

import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerResult
import kotlin.concurrent.Volatile

/**
 * Executor signature for module command dispatch.
 *
 * @param actionType The command action to execute
 * @param metadata Additional context from the voice command (e.g., frame type, filter name)
 * @return HandlerResult indicating success/failure of the command
 */
typealias ModuleCommandExecutor = suspend (
    actionType: CommandActionType,
    metadata: Map<String, String>
) -> HandlerResult

/**
 * Central registry for module command executors.
 *
 * Module screens set their executor when entering foreground (via Compose
 * DisposableEffect or Activity lifecycle). Handlers check for a non-null
 * executor before dispatching — if null, the module is not active and the
 * handler returns an honest failure.
 *
 * Usage from module screen (Compose):
 * ```
 * DisposableEffect(controller) {
 *     ModuleCommandCallbacks.noteExecutor = { actionType, metadata ->
 *         when (actionType) {
 *             CommandActionType.FORMAT_BOLD -> {
 *                 controller.toggleBold()
 *                 HandlerResult.success("Bold toggled")
 *             }
 *             // ... other actions
 *         }
 *     }
 *     onDispose { ModuleCommandCallbacks.noteExecutor = null }
 * }
 * ```
 *
 * Usage from handler:
 * ```
 * val executor = ModuleCommandCallbacks.noteExecutor
 *     ?: return HandlerResult.failure("NoteAvanue not active", recoverable = true)
 * return executor(command.actionType, extractMetadata(command))
 * ```
 */
object ModuleCommandCallbacks {

    // ═══════════════════════════════════════════════════════════════════
    // Module Executors
    // ═══════════════════════════════════════════════════════════════════

    /** NoteAvanue command executor — set when note editor is active. */
    @Volatile
    var noteExecutor: ModuleCommandExecutor? = null

    /** PDFAvanue command executor — set when PDF viewer is active. */
    @Volatile
    var pdfExecutor: ModuleCommandExecutor? = null

    /** PhotoAvanue command executor — set when camera preview is active. */
    @Volatile
    var cameraExecutor: ModuleCommandExecutor? = null

    /** Cockpit command executor — set when Cockpit multi-window is active. */
    @Volatile
    var cockpitExecutor: ModuleCommandExecutor? = null

    /** AnnotationAvanue command executor — set when annotation canvas is active. */
    @Volatile
    var annotationExecutor: ModuleCommandExecutor? = null

    /** ImageAvanue command executor — set when image viewer is active. */
    @Volatile
    var imageExecutor: ModuleCommandExecutor? = null

    /** VideoAvanue command executor — set when video player is active. */
    @Volatile
    var videoExecutor: ModuleCommandExecutor? = null

    /** RemoteCast command executor — set when cast session is active. */
    @Volatile
    var castExecutor: ModuleCommandExecutor? = null

    /** AI command executor — set when AI module is active. */
    @Volatile
    var aiExecutor: ModuleCommandExecutor? = null

    // ═══════════════════════════════════════════════════════════════════
    // Lifecycle
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Clear all module executors.
     * Call from service onDestroy() alongside VoiceControlCallbacks.clear().
     */
    fun clearAll() {
        noteExecutor = null
        pdfExecutor = null
        cameraExecutor = null
        cockpitExecutor = null
        annotationExecutor = null
        imageExecutor = null
        videoExecutor = null
        castExecutor = null
        aiExecutor = null
    }

    /**
     * Check which modules currently have active executors.
     * Useful for diagnostics and "what can I say" overlay.
     */
    fun activeModules(): List<String> = buildList {
        if (noteExecutor != null) add("note")
        if (pdfExecutor != null) add("pdf")
        if (cameraExecutor != null) add("camera")
        if (cockpitExecutor != null) add("cockpit")
        if (annotationExecutor != null) add("annotation")
        if (imageExecutor != null) add("image")
        if (videoExecutor != null) add("video")
        if (castExecutor != null) add("cast")
        if (aiExecutor != null) add("ai")
    }
}
