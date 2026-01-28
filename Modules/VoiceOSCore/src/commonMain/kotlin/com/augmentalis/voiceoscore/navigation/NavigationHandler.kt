/**
 * NavigationHandler.kt - Handles UI navigation actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP handler for navigation actions (scroll, swipe, page navigation).
 */
package com.augmentalis.voiceoscore

import com.augmentalis.voiceoscore.QuantizedCommand

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
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when (normalizedAction) {
            "scroll up", "page up" -> {
                if (executor.scrollUp()) {
                    HandlerResult.success("Scrolled up")
                } else {
                    HandlerResult.failure("Could not scroll up - no scrollable content")
                }
            }

            "scroll down", "page down" -> {
                if (executor.scrollDown()) {
                    HandlerResult.success("Scrolled down")
                } else {
                    HandlerResult.failure("Could not scroll down - no scrollable content")
                }
            }

            "scroll left" -> {
                if (executor.scrollLeft()) {
                    HandlerResult.success("Scrolled left")
                } else {
                    HandlerResult.failure("Could not scroll left - no scrollable content")
                }
            }

            "scroll right" -> {
                if (executor.scrollRight()) {
                    HandlerResult.success("Scrolled right")
                } else {
                    HandlerResult.failure("Could not scroll right - no scrollable content")
                }
            }

            "swipe up" -> {
                // Swipe up = scroll down (content moves up)
                if (executor.scrollDown()) {
                    HandlerResult.success("Swiped up")
                } else {
                    HandlerResult.failure("Could not swipe up")
                }
            }

            "swipe down" -> {
                // Swipe down = scroll up (content moves down)
                if (executor.scrollUp()) {
                    HandlerResult.success("Swiped down")
                } else {
                    HandlerResult.failure("Could not swipe down")
                }
            }

            "swipe left" -> {
                if (executor.scrollRight()) {
                    HandlerResult.success("Swiped left")
                } else {
                    HandlerResult.failure("Could not swipe left")
                }
            }

            "swipe right" -> {
                if (executor.scrollLeft()) {
                    HandlerResult.success("Swiped right")
                } else {
                    HandlerResult.failure("Could not swipe right")
                }
            }

            "next" -> {
                if (executor.next()) {
                    HandlerResult.success("Moved to next")
                } else {
                    HandlerResult.failure("Could not move to next")
                }
            }

            "previous" -> {
                if (executor.previous()) {
                    HandlerResult.success("Moved to previous")
                } else {
                    HandlerResult.failure("Could not move to previous")
                }
            }

            else -> HandlerResult.notHandled()
        }
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
