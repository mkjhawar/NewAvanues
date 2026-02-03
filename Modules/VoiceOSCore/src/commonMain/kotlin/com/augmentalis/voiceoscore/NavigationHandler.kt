/**
 * NavigationHandler.kt - Handles UI navigation actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 * Updated: 2026-02-03 - Refactored to use HandlerUtilities
 *
 * KMP handler for navigation actions (scroll, swipe, page navigation).
 */
package com.augmentalis.voiceoscore

/**
 * Handler for navigation actions.
 *
 * Supports:
 * - Scroll actions: scroll up/down/left/right
 * - Swipe actions: swipe up/down/left/right
 * - Page navigation: page up/down, next/previous
 */
class NavigationHandler(
    private val executor: NavigationExecutor
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.NAVIGATION

    override val supportedActions: List<String> = listOf(
        "scroll up", "scroll down",
        "scroll left", "scroll right",
        "swipe up", "swipe down",
        "swipe left", "swipe right",
        "next", "previous",
        "page up", "page down"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult = commandRouter(command.phrase) {
        // Scroll/Page actions
        on("scroll up", "page up") {
            executor.scrollUp().toHandlerResult("Scrolled up", "Could not scroll up - no scrollable content")
        }
        on("scroll down", "page down") {
            executor.scrollDown().toHandlerResult("Scrolled down", "Could not scroll down - no scrollable content")
        }
        on("scroll left") {
            executor.scrollLeft().toHandlerResult("Scrolled left", "Could not scroll left - no scrollable content")
        }
        on("scroll right") {
            executor.scrollRight().toHandlerResult("Scrolled right", "Could not scroll right - no scrollable content")
        }

        // Swipe actions (reversed scroll direction)
        on("swipe up") {
            executor.scrollDown().toHandlerResult("Swiped up", "Could not swipe up")
        }
        on("swipe down") {
            executor.scrollUp().toHandlerResult("Swiped down", "Could not swipe down")
        }
        on("swipe left") {
            executor.scrollRight().toHandlerResult("Swiped left", "Could not swipe left")
        }
        on("swipe right") {
            executor.scrollLeft().toHandlerResult("Swiped right", "Could not swipe right")
        }

        // Navigation
        on("next") {
            executor.next().toHandlerResult("Moved to next", "Could not move to next")
        }
        on("previous") {
            executor.previous().toHandlerResult("Moved to previous", "Could not move to previous")
        }

        otherwise { HandlerResult.notHandled() }
    }
}

/**
 * Platform-specific executor for navigation actions.
 */
interface NavigationExecutor {
    suspend fun scrollUp(): Boolean
    suspend fun scrollDown(): Boolean
    suspend fun scrollLeft(): Boolean
    suspend fun scrollRight(): Boolean
    suspend fun next(): Boolean
    suspend fun previous(): Boolean
}
