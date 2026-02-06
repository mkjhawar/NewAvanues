package com.augmentalis.voiceoscore.dsl.interpreter

/**
 * iOS-specific AVU code dispatcher.
 *
 * Provides a foundation for iOS AVU DSL execution. Currently returns stub responses
 * for most codes while properly handling QRY codes through [IAvuEnvironment].
 *
 * ## Future Implementation
 * When iOS platform features are built out, this dispatcher will bridge to:
 * - UIAccessibility APIs for screen queries
 * - Shortcuts/Intents for app automation
 * - Speech synthesis for TTS
 * - iOS-specific system commands
 *
 * ## Supported Codes (Stub)
 * All standard AVU codes are accepted but return platform-unavailable errors
 * except for QRY (environment queries) and LOG (logging).
 */
class IosAvuDispatcher(
    private val environment: IAvuEnvironment = StubEnvironment()
) : IAvuDispatcher {

    companion object {
        private val SUPPORTED_CODES = setOf(
            "VCM", "AAC", "CHT", "SCR", "TTS", "APP", "NAV", "SYS",
            "QRY", "GES", "LOG", "CFG", "NOT", "AUD", "CLP"
        )
    }

    override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult {
        return when (code) {
            "QRY" -> handleQuery(arguments)
            "LOG" -> handleLog(arguments)
            "CHT" -> handleChat(arguments)
            "TTS" -> handleTts(arguments)
            in SUPPORTED_CODES -> DispatchResult.Error(
                "Code $code is recognized but not yet implemented on iOS"
            )
            else -> DispatchResult.Error("Unknown code: $code")
        }
    }

    override fun canDispatch(code: String): Boolean = code in SUPPORTED_CODES

    private suspend fun handleQuery(arguments: Map<String, Any?>): DispatchResult {
        val target = arguments["target"] as? String ?: ""
        val method = arguments["method"] as? String ?: ""
        @Suppress("UNCHECKED_CAST")
        val args = arguments["args"] as? List<Any?> ?: emptyList()

        return when {
            target == "screen" && method == "contains" -> {
                val text = args.firstOrNull()?.toString() ?: ""
                DispatchResult.Success(environment.screenContains(text))
            }
            method == "foreground_app" || arguments["query"] == "foreground_app" -> {
                DispatchResult.Success(environment.getForegroundApp())
            }
            else -> {
                val result = environment.getProperty(arguments["query"]?.toString() ?: "")
                DispatchResult.Success(result)
            }
        }
    }

    private fun handleLog(arguments: Map<String, Any?>): DispatchResult {
        val message = arguments["message"]?.toString() ?: arguments.values.firstOrNull()?.toString() ?: ""
        println("[AVU iOS] $message")
        return DispatchResult.Success()
    }

    private fun handleChat(arguments: Map<String, Any?>): DispatchResult {
        val text = arguments["text"]?.toString() ?: ""
        println("[AVU iOS CHT] $text")
        return DispatchResult.Success(text)
    }

    private fun handleTts(arguments: Map<String, Any?>): DispatchResult {
        val text = arguments["text"]?.toString() ?: ""
        // TODO: Bridge to AVSpeechSynthesizer when iOS audio module is ready
        println("[AVU iOS TTS] $text")
        return DispatchResult.Success(text)
    }
}
