package com.augmentalis.voiceoscore.dsl.interpreter

/**
 * Desktop (JVM) AVU code dispatcher.
 *
 * Provides AVU DSL execution on desktop platforms. Handles a subset of codes natively
 * via JVM APIs and delegates screen queries to [IAvuEnvironment].
 *
 * ## Natively Supported Codes
 * - **QRY**: Environment queries via [IAvuEnvironment]
 * - **LOG**: Console logging
 * - **CHT**: Console output (chat simulation)
 * - **TTS**: Console output (speech simulation)
 * - **CLP**: Clipboard operations via java.awt.Toolkit
 * - **SYS**: System property queries
 *
 * ## Stub Codes
 * All other standard AVU codes are accepted but return platform-unavailable errors.
 * Desktop automation (window management, keyboard simulation) can be added via
 * java.awt.Robot and platform-specific accessibility APIs in future phases.
 */
class DesktopAvuDispatcher(
    private val environment: IAvuEnvironment = StubEnvironment()
) : IAvuDispatcher {

    companion object {
        private val SUPPORTED_CODES = setOf(
            "VCM", "AAC", "CHT", "SCR", "TTS", "APP", "NAV", "SYS",
            "QRY", "GES", "LOG", "CFG", "NOT", "AUD", "CLP", "FIL"
        )
    }

    override suspend fun dispatch(code: String, arguments: Map<String, Any?>): DispatchResult {
        return when (code) {
            "QRY" -> handleQuery(arguments)
            "LOG" -> handleLog(arguments)
            "CHT" -> handleChat(arguments)
            "TTS" -> handleTts(arguments)
            "SYS" -> handleSystem(arguments)
            "CLP" -> handleClipboard(arguments)
            in SUPPORTED_CODES -> DispatchResult.Error(
                "Code $code is recognized but not yet implemented on Desktop"
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
            arguments["query"] == "foreground_app" -> {
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
        println("[AVU Desktop] $message")
        return DispatchResult.Success()
    }

    private fun handleChat(arguments: Map<String, Any?>): DispatchResult {
        val text = arguments["text"]?.toString() ?: ""
        println("[AVU Desktop CHT] $text")
        return DispatchResult.Success(text)
    }

    private fun handleTts(arguments: Map<String, Any?>): DispatchResult {
        val text = arguments["text"]?.toString() ?: ""
        println("[AVU Desktop TTS] $text")
        return DispatchResult.Success(text)
    }

    private fun handleSystem(arguments: Map<String, Any?>): DispatchResult {
        val command = arguments["command"]?.toString()
            ?: return DispatchResult.Error("SYS requires 'command' argument")
        return when (command) {
            "platform" -> DispatchResult.Success("Desktop/JVM")
            "os_name" -> DispatchResult.Success(System.getProperty("os.name"))
            "os_version" -> DispatchResult.Success(System.getProperty("os.version"))
            "java_version" -> DispatchResult.Success(System.getProperty("java.version"))
            "user_home" -> DispatchResult.Success(System.getProperty("user.home"))
            else -> DispatchResult.Error("Unknown system command: $command")
        }
    }

    private fun handleClipboard(arguments: Map<String, Any?>): DispatchResult {
        val action = arguments["action"]?.toString()
            ?: return DispatchResult.Error("CLP requires 'action' argument")
        return when (action) {
            "get" -> {
                try {
                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                    val content = clipboard.getData(java.awt.datatransfer.DataFlavor.stringFlavor) as? String
                    DispatchResult.Success(content)
                } catch (e: Exception) {
                    DispatchResult.Error("Clipboard read failed: ${e.message}")
                }
            }
            "set" -> {
                try {
                    val text = arguments["text"]?.toString() ?: ""
                    val clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(
                        java.awt.datatransfer.StringSelection(text), null
                    )
                    DispatchResult.Success()
                } catch (e: Exception) {
                    DispatchResult.Error("Clipboard write failed: ${e.message}")
                }
            }
            else -> DispatchResult.Error("Unknown clipboard action: $action")
        }
    }
}
