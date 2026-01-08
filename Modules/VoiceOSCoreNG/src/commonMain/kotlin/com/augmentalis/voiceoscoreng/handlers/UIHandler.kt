/**
 * UIHandler.kt - Handles UI element interaction
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-06
 *
 * KMP handler for UI element interactions (click, tap, press, etc.).
 */
package com.augmentalis.voiceoscoreng.handlers

import com.augmentalis.voiceoscoreng.common.QuantizedCommand

/**
 * Handler for UI element interactions.
 *
 * Supports:
 * - Click actions: click, tap, press
 * - Long click: long click, long press
 * - Double tap: double tap, double click
 * - Toggle actions: expand, collapse, check, uncheck, toggle
 * - Focus/dismiss: focus, dismiss, close
 */
class UIHandler(
    private val executor: UIExecutor
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        "click", "tap", "press",
        "long click", "long press",
        "double tap", "double click",
        "expand", "collapse",
        "check", "uncheck", "toggle",
        "focus", "dismiss", "close"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        return when {
            // Click/Tap actions with target
            normalizedAction.startsWith("click ") ||
            normalizedAction.startsWith("tap ") ||
            normalizedAction.startsWith("press ") -> {
                val target = normalizedAction
                    .removePrefix("click ")
                    .removePrefix("tap ")
                    .removePrefix("press ")
                    .trim()

                // Check if target is a VUID
                val vuid = command.targetVuid ?: extractVuid(target)
                if (vuid != null) {
                    if (executor.clickByVuid(vuid)) {
                        HandlerResult.success("Clicked element")
                    } else {
                        HandlerResult.failure("Could not click element with VUID: $vuid")
                    }
                } else if (executor.clickByText(target)) {
                    HandlerResult.success("Clicked $target")
                } else {
                    HandlerResult.failure("Could not find element: $target")
                }
            }

            // Long click
            normalizedAction.startsWith("long click ") ||
            normalizedAction.startsWith("long press ") -> {
                val target = normalizedAction
                    .removePrefix("long click ")
                    .removePrefix("long press ")
                    .trim()

                val vuid = command.targetVuid ?: extractVuid(target)
                if (vuid != null) {
                    if (executor.longClickByVuid(vuid)) {
                        HandlerResult.success("Long clicked element")
                    } else {
                        HandlerResult.failure("Could not long click element with VUID: $vuid")
                    }
                } else if (executor.longClickByText(target)) {
                    HandlerResult.success("Long clicked $target")
                } else {
                    HandlerResult.failure("Could not find element: $target")
                }
            }

            // Double tap
            normalizedAction.startsWith("double tap ") ||
            normalizedAction.startsWith("double click ") -> {
                val target = normalizedAction
                    .removePrefix("double tap ")
                    .removePrefix("double click ")
                    .trim()

                if (executor.doubleClickByText(target)) {
                    HandlerResult.success("Double clicked $target")
                } else {
                    HandlerResult.failure("Could not find element: $target")
                }
            }

            // Expand/Collapse
            normalizedAction.startsWith("expand ") -> {
                val target = normalizedAction.removePrefix("expand ").trim()
                if (executor.expand(target)) {
                    HandlerResult.success("Expanded $target")
                } else {
                    HandlerResult.failure("Could not expand: $target")
                }
            }

            normalizedAction.startsWith("collapse ") -> {
                val target = normalizedAction.removePrefix("collapse ").trim()
                if (executor.collapse(target)) {
                    HandlerResult.success("Collapsed $target")
                } else {
                    HandlerResult.failure("Could not collapse: $target")
                }
            }

            // Check/Uncheck/Toggle
            normalizedAction.startsWith("check ") -> {
                val target = normalizedAction.removePrefix("check ").trim()
                if (executor.setChecked(target, true)) {
                    HandlerResult.success("Checked $target")
                } else {
                    HandlerResult.failure("Could not check: $target")
                }
            }

            normalizedAction.startsWith("uncheck ") -> {
                val target = normalizedAction.removePrefix("uncheck ").trim()
                if (executor.setChecked(target, false)) {
                    HandlerResult.success("Unchecked $target")
                } else {
                    HandlerResult.failure("Could not uncheck: $target")
                }
            }

            normalizedAction.startsWith("toggle ") -> {
                val target = normalizedAction.removePrefix("toggle ").trim()
                if (executor.toggle(target)) {
                    HandlerResult.success("Toggled $target")
                } else {
                    HandlerResult.failure("Could not toggle: $target")
                }
            }

            // Focus
            normalizedAction.startsWith("focus ") -> {
                val target = normalizedAction.removePrefix("focus ").trim()
                if (executor.focus(target)) {
                    HandlerResult.success("Focused $target")
                } else {
                    HandlerResult.failure("Could not focus: $target")
                }
            }

            // Dismiss/Close
            normalizedAction == "dismiss" || normalizedAction == "close" -> {
                if (executor.dismiss()) {
                    HandlerResult.success("Dismissed")
                } else {
                    HandlerResult.failure("Could not dismiss")
                }
            }

            else -> HandlerResult.notHandled()
        }
    }

    /**
     * Extract VUID from target string.
     * VUIDs start with "vuid:" or are 8-character hex strings.
     */
    private fun extractVuid(target: String): String? {
        return when {
            target.startsWith("vuid:") -> target.removePrefix("vuid:")
            target.matches(Regex("^[a-f0-9]{8}$")) -> target
            else -> null
        }
    }
}

/**
 * Platform-specific executor for UI actions.
 */
interface UIExecutor {
    suspend fun clickByText(text: String): Boolean
    suspend fun clickByVuid(vuid: String): Boolean
    suspend fun longClickByText(text: String): Boolean
    suspend fun longClickByVuid(vuid: String): Boolean
    suspend fun doubleClickByText(text: String): Boolean
    suspend fun expand(target: String): Boolean
    suspend fun collapse(target: String): Boolean
    suspend fun setChecked(target: String, checked: Boolean): Boolean
    suspend fun toggle(target: String): Boolean
    suspend fun focus(target: String): Boolean
    suspend fun dismiss(): Boolean
}
