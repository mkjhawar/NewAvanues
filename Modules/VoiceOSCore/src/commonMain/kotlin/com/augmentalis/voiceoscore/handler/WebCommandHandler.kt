/**
 * WebCommandHandler.kt - Handler for web voice commands
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-02-10
 *
 * Routes web-sourced voice commands to the IWebCommandExecutor
 * for JavaScript execution in the browser WebView.
 */
package com.augmentalis.voiceoscore

import kotlin.concurrent.Volatile

/**
 * Handler that processes web page voice commands.
 *
 * When the speech engine recognizes a web phrase (e.g., "click login"),
 * the ActionCoordinator routes it here. This handler:
 * 1. Extracts the CSS selector and action type from the command metadata
 * 2. Maps to a [WebAction] with the appropriate [WebActionType]
 * 3. Delegates to [IWebCommandExecutor] for JavaScript execution
 *
 * The executor is set at runtime when the browser becomes active.
 * If no executor is set, commands fail gracefully.
 */
class WebCommandHandler : BaseHandler() {

    override val category: ActionCategory = ActionCategory.BROWSER

    override val supportedActions: List<String> = buildList {
        // Element interaction verbs (for dynamically generated web element commands)
        addAll(listOf("click", "tap", "press", "focus", "type", "toggle", "select",
            "long press", "double tap", "double click", "hover"))
        // Browser commands from registry
        StaticCommandRegistry.byCategory(CommandCategory.BROWSER)
            .flatMap { it.phrases }.forEach { add(it) }
        // Web gesture commands from registry
        StaticCommandRegistry.byCategory(CommandCategory.WEB_GESTURE)
            .flatMap { it.phrases }.forEach { add(it) }
        // Text/clipboard commands from registry
        StaticCommandRegistry.byCategory(CommandCategory.TEXT)
            .flatMap { it.phrases }.forEach { add(it) }
    }.distinct()

    @Volatile
    private var executor: IWebCommandExecutor? = null

    /**
     * Set the web command executor.
     * Called when the browser becomes active and the WebView bridge is available.
     */
    fun setExecutor(executor: IWebCommandExecutor?) {
        this.executor = executor
    }

    override fun canHandle(command: QuantizedCommand): Boolean {
        // Web-sourced commands have metadata["source"] == "web"
        if (command.metadata["source"] == "web") return true
        // Commands with a CSS selector are web commands
        if (command.metadata.containsKey("selector")) return true
        // Fall back to phrase matching for static browser commands
        return canHandle(command.phrase)
    }

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val exec = executor
            ?: return HandlerResult.failure("Web executor not available — browser may not be active")

        if (!exec.isWebContextActive()) {
            return HandlerResult.failure("No active web page", recoverable = true)
        }

        val selector = command.metadata["selector"] ?: ""
        val xpath = command.metadata["xpath"] ?: ""
        val actionType = resolveWebActionType(command)

        val webAction = WebAction(
            actionType = actionType,
            selector = selector,
            xpath = xpath,
            text = params["text"]?.toString() ?: command.metadata["text"] ?: "",
            params = buildMap {
                command.metadata["angle"]?.let { put("angle", it) }
                command.metadata["direction"]?.let { put("direction", it) }
                command.metadata["distance"]?.let { put("distance", it) }
                command.metadata["dx"]?.let { put("dx", it) }
                command.metadata["dy"]?.let { put("dy", it) }
                command.metadata["deltaX"]?.let { put("deltaX", it) }
                command.metadata["deltaY"]?.let { put("deltaY", it) }
                command.metadata["velocityX"]?.let { put("velocityX", it) }
                command.metadata["velocityY"]?.let { put("velocityY", it) }
                command.metadata["velocity"]?.let { put("velocity", it) }
                command.metadata["factor"]?.let { put("factor", it) }
                command.metadata["scale"]?.let { put("scale", it) }
                // Extract direction/distance from phrase for pan/fling/tilt/orbit
                extractDirectionParams(command.phrase, actionType)?.forEach { (k, v) -> put(k, v) }
            }
        )

        return try {
            val result = exec.executeWebAction(webAction)
            if (result.success) {
                HandlerResult.success(result.message, result.data.mapValues { it.value as Any? })
            } else {
                HandlerResult.failure(result.message.ifEmpty { "Web action failed" }, recoverable = true)
            }
        } catch (e: Exception) {
            HandlerResult.failure("Web action error: ${e.message}", recoverable = true)
        }
    }

    /**
     * Map a QuantizedCommand to the appropriate WebActionType.
     *
     * Uses command.actionType first (set during QuantizedCommand creation),
     * then falls back to phrase parsing for static browser commands.
     */
    private fun resolveWebActionType(command: QuantizedCommand): WebActionType {
        // First: use the CommandActionType if it maps directly
        return when (command.actionType) {
            CommandActionType.CLICK, CommandActionType.TAP,
            CommandActionType.EXECUTE -> WebActionType.CLICK
            CommandActionType.FOCUS -> WebActionType.FOCUS
            CommandActionType.TYPE -> WebActionType.INPUT
            CommandActionType.SCROLL -> WebActionType.SCROLL_TO
            CommandActionType.LONG_CLICK -> WebActionType.LONG_PRESS

            // Browser-specific action types (added in Phase 3)
            CommandActionType.PAGE_BACK -> WebActionType.PAGE_BACK
            CommandActionType.PAGE_FORWARD -> WebActionType.PAGE_FORWARD
            CommandActionType.PAGE_REFRESH -> WebActionType.PAGE_REFRESH
            CommandActionType.SCROLL_TO_TOP -> WebActionType.SCROLL_TO_TOP
            CommandActionType.SCROLL_TO_BOTTOM -> WebActionType.SCROLL_TO_BOTTOM
            CommandActionType.TAB_NEXT -> WebActionType.TAB_NEXT
            CommandActionType.TAB_PREV -> WebActionType.TAB_PREV
            CommandActionType.SUBMIT_FORM -> WebActionType.SUBMIT_FORM
            CommandActionType.SWIPE_LEFT -> WebActionType.SWIPE_LEFT
            CommandActionType.SWIPE_RIGHT -> WebActionType.SWIPE_RIGHT
            CommandActionType.SWIPE_UP -> WebActionType.SWIPE_UP
            CommandActionType.SWIPE_DOWN -> WebActionType.SWIPE_DOWN
            CommandActionType.GRAB -> WebActionType.GRAB
            CommandActionType.RELEASE -> WebActionType.RELEASE
            CommandActionType.ROTATE -> WebActionType.ROTATE
            CommandActionType.DRAG -> WebActionType.DRAG
            CommandActionType.DOUBLE_CLICK -> WebActionType.DOUBLE_CLICK
            CommandActionType.HOVER -> WebActionType.HOVER

            // Existing scroll directions map to page scroll in web context
            CommandActionType.SCROLL_UP -> WebActionType.SCROLL_PAGE_UP
            CommandActionType.SCROLL_DOWN -> WebActionType.SCROLL_PAGE_DOWN
            CommandActionType.SCROLL_LEFT -> WebActionType.SCROLL_PAGE_LEFT
            CommandActionType.SCROLL_RIGHT -> WebActionType.SCROLL_PAGE_RIGHT

            // Text/clipboard
            CommandActionType.SELECT_ALL -> WebActionType.SELECT_ALL
            CommandActionType.COPY -> WebActionType.COPY
            CommandActionType.CUT -> WebActionType.CUT
            CommandActionType.PASTE -> WebActionType.PASTE

            // Zoom
            CommandActionType.ZOOM_IN -> WebActionType.ZOOM_IN
            CommandActionType.ZOOM_OUT -> WebActionType.ZOOM_OUT

            // Advanced gestures
            CommandActionType.PAN -> WebActionType.PAN
            CommandActionType.TILT -> WebActionType.TILT
            CommandActionType.ORBIT -> WebActionType.ORBIT
            CommandActionType.ROTATE_X -> WebActionType.ROTATE_X
            CommandActionType.ROTATE_Y -> WebActionType.ROTATE_Y
            CommandActionType.ROTATE_Z -> WebActionType.ROTATE_Z
            CommandActionType.PINCH -> WebActionType.PINCH
            CommandActionType.FLING -> WebActionType.FLING
            CommandActionType.THROW -> WebActionType.THROW
            CommandActionType.SCALE -> WebActionType.SCALE
            CommandActionType.RESET_ZOOM -> WebActionType.RESET_ZOOM
            CommandActionType.SELECT_WORD -> WebActionType.SELECT_WORD
            CommandActionType.CLEAR_SELECTION -> WebActionType.CLEAR_SELECTION
            CommandActionType.HOVER_OUT -> WebActionType.HOVER_OUT

            // Page retrain
            CommandActionType.RETRAIN_PAGE -> WebActionType.RETRAIN_PAGE

            // Drawing/annotation
            CommandActionType.STROKE_START -> WebActionType.STROKE_START
            CommandActionType.STROKE_END -> WebActionType.STROKE_END
            CommandActionType.ERASE -> WebActionType.ERASE

            // Fallback: parse from phrase
            else -> resolveFromPhrase(command.phrase)
        }
    }

    /**
     * Parse action type from voice phrase for static browser commands.
     */
    private fun resolveFromPhrase(phrase: String): WebActionType {
        val normalized = phrase.lowercase().trim()
        return when {
            normalized.startsWith("click") || normalized.startsWith("tap") || normalized.startsWith("press") -> WebActionType.CLICK
            normalized.startsWith("focus") -> WebActionType.FOCUS
            normalized.startsWith("type") -> WebActionType.INPUT
            normalized.startsWith("toggle") -> WebActionType.TOGGLE
            normalized.startsWith("select") && !normalized.contains("all") -> WebActionType.SELECT
            normalized == "retrain page" || normalized == "rescan page" || normalized == "rescan" -> WebActionType.RETRAIN_PAGE
            normalized == "go back" || normalized == "back" -> WebActionType.PAGE_BACK
            normalized == "go forward" || normalized == "forward" -> WebActionType.PAGE_FORWARD
            normalized == "refresh" || normalized == "reload" -> WebActionType.PAGE_REFRESH
            normalized == "page up" -> WebActionType.SCROLL_PAGE_UP
            normalized == "page down" -> WebActionType.SCROLL_PAGE_DOWN
            normalized == "go to top" || normalized == "scroll to top" || normalized == "top of page" -> WebActionType.SCROLL_TO_TOP
            normalized == "go to bottom" || normalized == "scroll to bottom" || normalized == "bottom of page" -> WebActionType.SCROLL_TO_BOTTOM
            normalized == "next field" || normalized == "tab" -> WebActionType.TAB_NEXT
            normalized == "previous field" -> WebActionType.TAB_PREV
            normalized == "submit" || normalized == "submit form" -> WebActionType.SUBMIT_FORM
            normalized == "swipe left" -> WebActionType.SWIPE_LEFT
            normalized == "swipe right" -> WebActionType.SWIPE_RIGHT
            normalized == "swipe up" -> WebActionType.SWIPE_UP
            normalized == "swipe down" -> WebActionType.SWIPE_DOWN
            normalized == "grab" || normalized == "lock" || normalized == "lock element" -> WebActionType.GRAB
            normalized == "release" || normalized == "let go" -> WebActionType.RELEASE
            normalized.startsWith("rotate x") -> WebActionType.ROTATE_X
            normalized.startsWith("rotate y") -> WebActionType.ROTATE_Y
            normalized.startsWith("rotate z") -> WebActionType.ROTATE_Z
            normalized.startsWith("rotate") -> WebActionType.ROTATE
            normalized.startsWith("long press") -> WebActionType.LONG_PRESS
            normalized.startsWith("double") -> WebActionType.DOUBLE_CLICK
            normalized == "hover out" || normalized == "stop hovering" -> WebActionType.HOVER_OUT
            normalized.startsWith("hover") -> WebActionType.HOVER
            normalized == "scroll up" || normalized == "page up" -> WebActionType.SCROLL_PAGE_UP
            normalized == "scroll down" || normalized == "page down" -> WebActionType.SCROLL_PAGE_DOWN
            normalized == "scroll left" -> WebActionType.SCROLL_PAGE_LEFT
            normalized == "scroll right" -> WebActionType.SCROLL_PAGE_RIGHT
            normalized == "zoom in" || normalized == "zoom closer" || normalized == "magnify" -> WebActionType.ZOOM_IN
            normalized == "zoom out" || normalized == "zoom away" -> WebActionType.ZOOM_OUT
            normalized == "reset zoom" -> WebActionType.RESET_ZOOM
            normalized == "long click" || normalized == "press and hold" -> WebActionType.LONG_PRESS
            normalized == "mouse over" -> WebActionType.HOVER
            normalized == "tap here" -> WebActionType.CLICK
            normalized == "focus on" -> WebActionType.FOCUS
            normalized.startsWith("drag ") -> WebActionType.DRAG
            normalized == "start drawing" || normalized == "begin stroke" || normalized == "draw" -> WebActionType.STROKE_START
            normalized == "stop drawing" || normalized == "finish drawing" || normalized == "end stroke" -> WebActionType.STROKE_END
            normalized == "eraser" || normalized == "erase mode" || normalized == "toggle eraser" -> WebActionType.ERASE
            normalized == "pan" || normalized.startsWith("pan ") -> WebActionType.PAN
            normalized == "tilt" || normalized.startsWith("tilt ") -> WebActionType.TILT
            normalized == "orbit" || normalized.startsWith("orbit ") -> WebActionType.ORBIT
            normalized.startsWith("pinch") -> WebActionType.PINCH
            normalized.startsWith("fling") -> WebActionType.FLING
            normalized == "throw" || normalized == "toss" -> WebActionType.THROW
            normalized.startsWith("scale") -> WebActionType.SCALE
            normalized == "select word" -> WebActionType.SELECT_WORD
            normalized == "clear selection" || normalized == "deselect" -> WebActionType.CLEAR_SELECTION
            normalized == "select all" -> WebActionType.SELECT_ALL
            normalized.startsWith("copy") -> WebActionType.COPY
            normalized == "cut" -> WebActionType.CUT
            normalized == "paste" -> WebActionType.PASTE
            else -> WebActionType.CLICK
        }
    }

    /**
     * Extract directional parameters from voice phrase for gestures that
     * encode direction in the phrase itself (e.g., "pan left", "fling down").
     *
     * Returns null if no directional info found in the phrase.
     * Note: Phrases are English-only for now; localized phrase tables
     * (Vivoka/Whisper/Google STT) will replace these string matches.
     */
    private fun extractDirectionParams(phrase: String, actionType: WebActionType): Map<String, String>? {
        val normalized = phrase.lowercase().trim()
        return when (actionType) {
            WebActionType.PAN -> {
                val dist = "200"
                when {
                    normalized.contains("left") -> mapOf("dx" to "-$dist", "dy" to "0")
                    normalized.contains("right") -> mapOf("dx" to dist, "dy" to "0")
                    normalized.contains("up") -> mapOf("dx" to "0", "dy" to "-$dist")
                    normalized.contains("down") -> mapOf("dx" to "0", "dy" to dist)
                    else -> null
                }
            }
            WebActionType.TILT -> {
                when {
                    normalized.contains("up") -> mapOf("angle" to "15")
                    normalized.contains("down") -> mapOf("angle" to "-15")
                    else -> null
                }
            }
            WebActionType.ORBIT -> {
                when {
                    normalized.contains("left") -> mapOf("deltaX" to "-30", "deltaY" to "0")
                    normalized.contains("right") -> mapOf("deltaX" to "30", "deltaY" to "0")
                    else -> null
                }
            }
            WebActionType.FLING -> {
                when {
                    normalized.contains("up") -> mapOf("direction" to "up", "velocity" to "1500")
                    normalized.contains("down") -> mapOf("direction" to "down", "velocity" to "1500")
                    normalized.contains("left") -> mapOf("direction" to "left", "velocity" to "1500")
                    normalized.contains("right") -> mapOf("direction" to "right", "velocity" to "1500")
                    else -> null
                }
            }
            WebActionType.PINCH -> {
                when {
                    normalized.contains("in") -> mapOf("scale" to "0.5")
                    normalized.contains("out") -> mapOf("scale" to "2.0")
                    else -> null
                }
            }
            WebActionType.SCALE -> {
                when {
                    normalized.contains("up") -> mapOf("factor" to "1.5")
                    normalized.contains("down") -> mapOf("factor" to "0.67")
                    else -> null
                }
            }
            WebActionType.DRAG -> {
                val dist = "100"
                when {
                    normalized.contains("left") -> mapOf("direction" to "left", "endX" to "-$dist", "endY" to "0")
                    normalized.contains("right") -> mapOf("direction" to "right", "endX" to dist, "endY" to "0")
                    normalized.contains("up") -> mapOf("direction" to "up", "endX" to "0", "endY" to "-$dist")
                    normalized.contains("down") -> mapOf("direction" to "down", "endX" to "0", "endY" to dist)
                    else -> null
                }
            }
            else -> null
        }
    }
}
