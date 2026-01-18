package com.augmentalis.avucodec

/**
 * Universal IPC Encoder - Encodes voice commands and messages into AVU IPC Protocol format.
 *
 * This is a platform-agnostic encoder for the Avanues Universal IPC Protocol.
 *
 * Protocol Specification: Avanues Universal IPC Protocol v2.0.0
 * Format: CODE:id:param1:param2:...
 *
 * Voice Command Format: VCM:commandId:action:param1:param2
 * Example: VCM:cmd123:SCROLL_TOP
 *          VCM:cmd456:ZOOM_IN
 *          VCM:cmd789:NAVIGATE:https%3A%2F%2Fgoogle.com
 *
 * @author Augmentalis Engineering
 * @since 1.0.0
 */
object AVUEncoder {

    // VoiceOS IPC action for Intent broadcasts
    const val IPC_ACTION = "com.augmentalis.voiceos.IPC.COMMAND"
    const val EXTRA_MESSAGE = "message"
    const val EXTRA_SOURCE_APP = "source_app"

    // Protocol codes
    const val CODE_VOICE_COMMAND = "VCM"
    const val CODE_ACCEPT = "ACC"
    const val CODE_ACCEPT_DATA = "ACD"
    const val CODE_DECLINE = "DEC"
    const val CODE_DECLINE_REASON = "DCR"
    const val CODE_BUSY = "BSY"
    const val CODE_BUSY_CALLBACK = "BCF"
    const val CODE_ERROR = "ERR"
    const val CODE_CHAT = "CHT"
    const val CODE_URL = "URL"
    const val CODE_NAV = "NAV"
    const val CODE_AI_QUERY = "AIQ"
    const val CODE_AI_RESPONSE = "AIR"
    const val CODE_JSON = "JSN"
    const val CODE_SPEECH_TO_TEXT = "STT"
    const val CODE_CONNECTED = "CON"
    const val CODE_DISCONNECTED = "DIS"
    const val CODE_HANDSHAKE = "HND"
    const val CODE_PING = "PNG"
    const val CODE_PONG = "PON"
    const val CODE_CAPABILITY = "CAP"

    private const val DELIMITER = ':'

    // ════════════════════════════════════════════════════════════════════════
    // VOICE COMMANDS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode voice command to AVU IPC format.
     *
     * @param commandId Unique command identifier (e.g., "cmd123")
     * @param action Command action (e.g., "SCROLL_TOP", "ZOOM_IN")
     * @param params Optional parameters as key-value map
     * @return Encoded IPC message (e.g., "VCM:cmd123:SCROLL_TOP")
     * @throws IllegalArgumentException if commandId or action is blank
     */
    fun encodeVoiceCommand(
        commandId: String,
        action: String,
        params: Map<String, Any> = emptyMap()
    ): String {
        require(commandId.isNotBlank()) { "commandId cannot be blank" }
        require(action.isNotBlank()) { "action cannot be blank" }

        return buildString {
            append(CODE_VOICE_COMMAND)
            append(DELIMITER)
            append(escape(commandId))
            append(DELIMITER)
            append(escape(action))

            params.forEach { (key, value) ->
                append(DELIMITER)
                append(escape("$key=$value"))
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESPONSES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode accept response.
     */
    fun encodeAccept(requestId: String, data: String? = null): String {
        require(requestId.isNotBlank()) { "requestId cannot be blank" }
        return if (data != null) {
            "$CODE_ACCEPT_DATA:${escape(requestId)}:${escape(data)}"
        } else {
            "$CODE_ACCEPT:${escape(requestId)}"
        }
    }

    /**
     * Encode decline response.
     */
    fun encodeDecline(requestId: String, reason: String? = null): String {
        return if (reason != null) {
            "$CODE_DECLINE_REASON:${escape(requestId)}:${escape(reason)}"
        } else {
            "$CODE_DECLINE:${escape(requestId)}"
        }
    }

    /**
     * Encode busy response.
     */
    fun encodeBusy(requestId: String, callbackUrl: String? = null): String {
        return if (callbackUrl != null) {
            "$CODE_BUSY_CALLBACK:${escape(requestId)}:${escape(callbackUrl)}"
        } else {
            "$CODE_BUSY:${escape(requestId)}"
        }
    }

    /**
     * Encode error response.
     */
    fun encodeError(requestId: String, errorCode: Int, errorMessage: String): String {
        return "$CODE_ERROR:${escape(requestId)}:$errorCode:${escape(errorMessage)}"
    }

    /**
     * Encode error response (simple version).
     */
    fun encodeError(requestId: String, errorMessage: String): String {
        return encodeError(requestId, 0, errorMessage)
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONTENT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode chat message.
     */
    fun encodeChat(messageId: String = "", senderId: String = "", text: String): String {
        require(text.isNotBlank()) { "text cannot be blank" }
        return "$CODE_CHAT:$messageId:$senderId:${escape(text)}"
    }

    /**
     * Encode URL share.
     */
    fun encodeUrlShare(sessionId: String, url: String): String {
        return "$CODE_URL:${escape(sessionId)}:${escape(url)}"
    }

    /**
     * Encode navigate command.
     */
    fun encodeNavigate(sessionId: String, url: String): String {
        require(sessionId.isNotBlank()) { "sessionId cannot be blank" }
        require(url.isNotBlank()) { "url cannot be blank" }
        return "$CODE_NAV:${escape(sessionId)}:${escape(url)}"
    }

    // ════════════════════════════════════════════════════════════════════════
    // AI
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode AI query.
     */
    fun encodeAIQuery(queryId: String, query: String, context: String? = null): String {
        return if (context != null) {
            "$CODE_AI_QUERY:${escape(queryId)}:${escape(query)}:${escape(context)}"
        } else {
            "$CODE_AI_QUERY:${escape(queryId)}:${escape(query)}"
        }
    }

    /**
     * Encode AI response.
     */
    fun encodeAIResponse(queryId: String, response: String, confidence: Float? = null): String {
        return if (confidence != null) {
            "$CODE_AI_RESPONSE:${escape(queryId)}:${escape(response)}:$confidence"
        } else {
            "$CODE_AI_RESPONSE:${escape(queryId)}:${escape(response)}"
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // JSON/DSL
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode JSON/UI DSL wrapper.
     */
    fun encodeJson(requestId: String, jsonOrDsl: String): String {
        return "$CODE_JSON:${escape(requestId)}:${escape(jsonOrDsl)}"
    }

    // ════════════════════════════════════════════════════════════════════════
    // VOICE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode speech-to-text result.
     */
    fun encodeSpeechToText(
        sessionId: String,
        transcript: String,
        confidence: Float,
        isFinal: Boolean
    ): String {
        return "$CODE_SPEECH_TO_TEXT:${escape(sessionId)}:${escape(transcript)}:$confidence:$isFinal"
    }

    // ════════════════════════════════════════════════════════════════════════
    // SYSTEM
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode handshake message.
     */
    fun encodeHandshake(sessionId: String, appId: String, version: String): String {
        return "$CODE_HANDSHAKE:${escape(sessionId)}:${escape(appId)}:${escape(version)}"
    }

    /**
     * Encode ping message.
     */
    fun encodePing(sessionId: String, timestamp: Long): String {
        return "$CODE_PING:${escape(sessionId)}:$timestamp"
    }

    /**
     * Encode pong message.
     */
    fun encodePong(sessionId: String, timestamp: Long): String {
        return "$CODE_PONG:${escape(sessionId)}:$timestamp"
    }

    /**
     * Encode capability announcement.
     */
    fun encodeCapabilities(sessionId: String, capabilities: List<String>): String {
        return "$CODE_CAPABILITY:${escape(sessionId)}:${capabilities.joinToString(",") { escape(it) }}"
    }

    // ════════════════════════════════════════════════════════════════════════
    // GENERIC
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode generic protocol message.
     *
     * @param code 3-letter protocol code (e.g., "VCA", "ACC", "CHT")
     * @param id Request/session identifier
     * @param params Variable parameters
     * @return Encoded IPC message
     */
    fun encodeGeneric(code: String, id: String, vararg params: String): String {
        require(code.length == 3 && code.all { it.isUpperCase() }) {
            "Protocol code must be 3 uppercase letters, got: $code"
        }

        return buildString {
            append(code)
            append(DELIMITER)
            append(escape(id))

            params.forEach { param ->
                append(DELIMITER)
                append(escape(param))
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // UTILITY
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Escape special characters per AVU IPC Protocol specification.
     *
     * Reserved characters:
     * - ':' (delimiter) -> %3A
     * - '%' (escape) -> %25
     * - '\n' (newline) -> %0A
     * - '\r' (carriage return) -> %0D
     */
    fun escape(text: String): String {
        return text
            .replace("%", "%25")
            .replace(":", "%3A")
            .replace("\n", "%0A")
            .replace("\r", "%0D")
    }

    /**
     * Unescape special characters.
     */
    fun unescape(text: String): String {
        return text
            .replace("%0D", "\r")
            .replace("%0A", "\n")
            .replace("%3A", ":")
            .replace("%25", "%")
    }

    /**
     * Validate IPC message format.
     */
    fun isValidMessage(message: String): Boolean {
        if (message.length < 4) return false

        val parts = message.split(DELIMITER, limit = 2)
        if (parts.isEmpty()) return false

        val code = parts[0]
        return code.length == 3 && code.all { it.isUpperCase() }
    }

    /**
     * Extract protocol code from message.
     */
    fun extractCode(message: String): String? {
        val parts = message.split(DELIMITER, limit = 2)
        val code = parts.getOrNull(0) ?: return null
        return if (code.length == 3 && code.all { it.isUpperCase() }) code else null
    }

    /**
     * Calculate message size reduction vs JSON.
     */
    fun calculateSizeReduction(ipcMessage: String, jsonEquivalent: String): Int {
        val ipcSize = ipcMessage.encodeToByteArray().size
        val jsonSize = jsonEquivalent.encodeToByteArray().size

        if (jsonSize == 0) return 0

        val reduction = ((jsonSize - ipcSize).toFloat() / jsonSize * 100).toInt()
        return reduction.coerceIn(0, 100)
    }
}
