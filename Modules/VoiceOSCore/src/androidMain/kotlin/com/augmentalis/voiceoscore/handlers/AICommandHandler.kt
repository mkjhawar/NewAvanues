/**
 * AICommandHandler.kt - IHandler for AI voice commands
 *
 * Handles: summarize, chat, RAG search, teach, clear context.
 * Routes to AI:Chat module coordinators for actual LLM operations.
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

private const val TAG = "AICmdHandler"

class AICommandHandler(
    private val service: AccessibilityService
) : BaseHandler() {

    override val category: ActionCategory = ActionCategory.AI

    override val supportedActions: List<String> = listOf(
        "summarize", "ai chat", "ai search",
        "teach ai", "clear ai context"
    )

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        Log.d(TAG, "execute: '${command.phrase}', actionType=${command.actionType}")

        return when (command.actionType) {
            CommandActionType.AI_SUMMARIZE ->
                failure("Summarize requires AI:Chat module integration", recoverable = true)
            CommandActionType.AI_CHAT ->
                failure("AI Chat requires AI:Chat module integration", recoverable = true)
            CommandActionType.AI_RAG_SEARCH ->
                failure("RAG search requires AI:Chat module integration", recoverable = true)
            CommandActionType.AI_TEACH ->
                failure("Teaching flow requires AI:Chat module integration", recoverable = true)
            CommandActionType.AI_CLEAR_CONTEXT ->
                failure("Clear context requires AI:Chat module integration", recoverable = true)
            else -> HandlerResult.notHandled()
        }
    }

    private fun failure(message: String, recoverable: Boolean): HandlerResult {
        Log.w(TAG, message)
        return HandlerResult.failure(message, recoverable = recoverable)
    }
}
