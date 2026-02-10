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

    override val supportedActions: List<String> = listOf(
        // Element actions (with target)
        "click", "tap", "press", "focus", "type", "toggle", "select",
        "long press", "double tap", "double click", "hover",
        // Page navigation
        "go back", "go forward", "refresh", "reload",
        // Page scrolling
        "page up", "page down", "go to top", "go to bottom",
        "scroll to top", "scroll to bottom",
        // Form navigation
        "next field", "tab", "previous field", "submit", "submit form",
        // Gestures
        "swipe left", "swipe right", "swipe up", "swipe down",
        "grab", "release", "let go", "rotate left", "rotate right",
        "zoom in", "zoom out",
        // Text/Clipboard
        "select all", "copy", "copy that", "cut", "paste"
    )

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

            // Text/clipboard
            CommandActionType.SELECT_ALL -> WebActionType.SELECT_ALL
            CommandActionType.COPY -> WebActionType.COPY
            CommandActionType.CUT -> WebActionType.CUT
            CommandActionType.PASTE -> WebActionType.PASTE

            // Zoom
            CommandActionType.ZOOM_IN -> WebActionType.ZOOM_IN
            CommandActionType.ZOOM_OUT -> WebActionType.ZOOM_OUT

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
            normalized == "grab" -> WebActionType.GRAB
            normalized == "release" || normalized == "let go" -> WebActionType.RELEASE
            normalized.startsWith("rotate") -> WebActionType.ROTATE
            normalized.startsWith("long press") -> WebActionType.LONG_PRESS
            normalized.startsWith("double") -> WebActionType.DOUBLE_CLICK
            normalized.startsWith("hover") -> WebActionType.HOVER
            normalized == "zoom in" -> WebActionType.ZOOM_IN
            normalized == "zoom out" -> WebActionType.ZOOM_OUT
            normalized == "select all" -> WebActionType.SELECT_ALL
            normalized.startsWith("copy") -> WebActionType.COPY
            normalized == "cut" -> WebActionType.CUT
            normalized == "paste" -> WebActionType.PASTE
            else -> WebActionType.CLICK
        }
    }
}
