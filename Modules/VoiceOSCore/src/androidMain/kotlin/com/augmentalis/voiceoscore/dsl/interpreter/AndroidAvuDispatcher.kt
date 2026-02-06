package com.augmentalis.voiceoscore.dsl.interpreter

import com.avanues.avu.dsl.interpreter.DispatchResult
import com.avanues.avu.dsl.interpreter.IAvuDispatcher
import com.avanues.avu.dsl.interpreter.IAvuEnvironment
import com.avanues.avu.dsl.interpreter.StubEnvironment
import com.augmentalis.voiceoscore.CommandActionType
import com.augmentalis.voiceoscore.HandlerRegistry
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.IHandler
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Android-specific AVU code dispatcher.
 *
 * Bridges AVU DSL code invocations to the existing [HandlerRegistry] and
 * [IAvuEnvironment] for screen queries. This is the primary production dispatcher
 * for Android devices.
 *
 * ## Code Routing
 * - **VCM** (Voice Command): Routes to handler matching the action
 * - **AAC** (Accessibility Action): Routes to accessibility handler
 * - **CHT** (Chat Message): Routes to chat/TTS handler
 * - **QRY** (Query): Delegates to [IAvuEnvironment] for screen state queries
 * - **SCR** (Screen): Routes to screen management handler
 * - **TTS** (Text-to-Speech): Routes to speech handler
 * - **APP** (App Control): Routes to app management handler
 * - All other codes: Attempts handler lookup by code name
 *
 * ## Usage
 * ```kotlin
 * val dispatcher = AndroidAvuDispatcher(handlerRegistry, environment)
 * val interpreter = AvuInterpreter(dispatcher)
 * ```
 */
class AndroidAvuDispatcher(
    private val handlerRegistry: HandlerRegistry,
    private val environment: IAvuEnvironment = StubEnvironment()
) : IAvuDispatcher {

    companion object {
        /** Codes handled through handler registry lookup. */
        private val HANDLER_CODES = setOf(
            "VCM", "AAC", "CHT", "SCR", "TTS", "APP", "NAV", "SYS",
            "GES", "DRG", "EDT", "TBL", "VOL", "DIC", "SHC", "OVR",
            "TIM", "SCL", "TXT", "CAM", "MIC", "NET", "BLE", "NFC",
            "GPS", "SNS", "NOT", "CAL", "SMS", "PHN", "CTX", "PRF",
            "LOG", "DBG", "ANL", "CFG", "UPD", "SEC", "BAT", "MEM",
            "CPU", "DSK", "WIF", "BLT", "USB", "AUD", "VID", "IMG",
            "FIL", "ZIP", "CLP", "KBD"
        )
    }

    override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult {
        return try {
            when (code) {
                "QRY" -> handleQuery(arguments)
                in HANDLER_CODES -> handleViaRegistry(code, arguments)
                else -> handleViaRegistry(code, arguments)
            }
        } catch (e: Exception) {
            DispatchResult.Error("Dispatch failed for $code: ${e.message}", e)
        }
    }

    override fun canDispatch(code: String): Boolean = true

    // =========================================================================
    // QUERY HANDLING
    // =========================================================================

    private suspend fun handleQuery(arguments: Map<String, Any?>): DispatchResult {
        val query = arguments["query"] as? String
            ?: return DispatchResult.Error("QRY requires 'query' argument")
        val target = arguments["target"] as? String ?: ""
        val method = arguments["method"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val args = arguments["args"] as? List<Any?> ?: emptyList()

        return when {
            target == "screen" && method == "contains" -> {
                val text = args.firstOrNull()?.toString() ?: ""
                DispatchResult.Success(environment.screenContains(text))
            }
            query == "screen_contains" -> {
                val text = args.firstOrNull()?.toString() ?: ""
                DispatchResult.Success(environment.screenContains(text))
            }
            query == "element_visible" -> {
                val elementId = args.firstOrNull()?.toString() ?: ""
                DispatchResult.Success(environment.isElementVisible(elementId))
            }
            query == "foreground_app" -> {
                DispatchResult.Success(environment.getForegroundApp())
            }
            query == "screen_text" -> {
                DispatchResult.Success(environment.getScreenText())
            }
            else -> {
                DispatchResult.Success(environment.getProperty(query))
            }
        }
    }

    // =========================================================================
    // HANDLER REGISTRY ROUTING
    // =========================================================================

    private suspend fun handleViaRegistry(
        code: String,
        arguments: Map<String, Any?>
    ): DispatchResult {
        val action = buildActionString(code, arguments)
        val handler = handlerRegistry.findHandler(action)
            ?: return DispatchResult.Error("No handler found for code: $code (action: $action)")

        return executeHandler(handler, code, arguments)
    }

    private suspend fun executeHandler(
        handler: IHandler,
        code: String,
        arguments: Map<String, Any?>
    ): DispatchResult {
        val action = arguments["action"]?.toString() ?: code.lowercase()
        val actionType = arguments["actionType"]?.toString()?.let {
            CommandActionType.fromString(it)
        } ?: CommandActionType.EXECUTE
        val targetAvid = arguments["target"]?.toString()

        val command = QuantizedCommand(
            phrase = action,
            actionType = actionType,
            targetAvid = targetAvid,
            confidence = 1.0f
        )

        // Convert Map<String, Any?> to Map<String, Any> by filtering nulls
        val handlerParams: Map<String, Any> = arguments.entries
            .mapNotNull { (k, v) -> v?.let { k to it } }
            .toMap()

        return try {
            when (val result = handler.execute(command, handlerParams)) {
                is HandlerResult.Success -> DispatchResult.Success(result.data)
                is HandlerResult.Failure -> DispatchResult.Error(result.reason)
                is HandlerResult.NotHandled -> DispatchResult.Error(
                    "Handler did not handle code: $code"
                )
                is HandlerResult.RequiresInput -> DispatchResult.Error(
                    "Handler requires input: ${result.prompt}"
                )
                is HandlerResult.InProgress -> DispatchResult.Success(
                    mapOf("status" to "in_progress", "progress" to result.progress)
                )
                is HandlerResult.AwaitingSelection -> DispatchResult.Error(
                    "Handler awaiting selection: ${result.message}"
                )
            }
        } catch (e: Exception) {
            DispatchResult.Error("Handler threw exception for $code: ${e.message}", e)
        }
    }

    /**
     * Build an action string for handler lookup from code + arguments.
     * Maps AVU codes to action patterns that HandlerRegistry can find.
     */
    private fun buildActionString(code: String, arguments: Map<String, Any?>): String {
        val action = arguments["action"]?.toString()
        return when {
            action != null -> action
            code == "VCM" -> arguments["command"]?.toString() ?: "voice_command"
            code == "AAC" -> arguments["actionType"]?.toString() ?: "accessibility_action"
            code == "CHT" -> "chat_message"
            code == "TTS" -> "speak"
            code == "SCR" -> "screen_action"
            code == "APP" -> arguments["package"]?.toString() ?: "app_control"
            code == "NAV" -> "navigate"
            code == "SYS" -> arguments["command"]?.toString() ?: "system"
            else -> code.lowercase()
        }
    }
}
