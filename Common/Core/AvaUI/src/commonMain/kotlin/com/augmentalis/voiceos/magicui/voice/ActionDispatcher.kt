package com.augmentalis.avanues.avaui.voice

import com.augmentalis.avanues.avaui.events.EventBus
import com.augmentalis.avanues.avaui.events.ComponentEvent

/**
 * Dispatches voice-triggered actions.
 */
class ActionDispatcher(
    private val eventBus: EventBus
) {

    /**
     * Dispatch action from matched command.
     */
    suspend fun dispatch(match: CommandMatch, context: Map<String, Any?> = emptyMap()) {
        val command = match.command

        // Parse action
        val actionParts = command.action.split(".")

        when {
            actionParts.size == 1 -> {
                // Simple action like "openColorPicker"
                dispatchSimpleAction(command.action, command.componentId, context)
            }
            actionParts.size == 2 -> {
                // Method call like "ColorPicker.show"
                dispatchMethodCall(actionParts[0], actionParts[1], command.componentId, context)
            }
            else -> {
                throw ActionDispatchException("Invalid action format: ${command.action}")
            }
        }
    }

    private suspend fun dispatchSimpleAction(
        action: String,
        componentId: String?,
        context: Map<String, Any?>
    ) {
        // Emit event for simple action
        eventBus.emit(ComponentEvent(
            componentId = componentId ?: "app",
            eventName = "voiceAction",
            parameters = mapOf(
                "action" to action,
                "context" to context
            )
        ))
    }

    private suspend fun dispatchMethodCall(
        target: String,
        method: String,
        componentId: String?,
        context: Map<String, Any?>
    ) {
        // Emit event for method call
        eventBus.emit(ComponentEvent(
            componentId = componentId ?: target,
            eventName = "voiceMethodCall",
            parameters = mapOf(
                "target" to target,
                "method" to method,
                "context" to context
            )
        ))
    }
}

class ActionDispatchException(message: String) : Exception(message)
