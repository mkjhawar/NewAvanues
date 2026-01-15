package com.augmentalis.avamagic.ipc

/**
 * IPC Encoder - Encodes voice commands and messages into AVU IPC Protocol format.
 *
 * This is a convenience wrapper around [AvuIPCSerializer] that provides
 * a simpler API for common encoding operations.
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
object IPCEncoder {

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
     *
     * Examples:
     * - encodeVoiceCommand("cmd1", "SCROLL_TOP") → "VCM:cmd1:SCROLL_TOP"
     * - encodeVoiceCommand("cmd2", "ZOOM_IN") → "VCM:cmd2:ZOOM_IN"
     * - encodeVoiceCommand("cmd3", "NAVIGATE", mapOf("url" to "https://google.com"))
     *   → "VCM:cmd3:NAVIGATE:url=https%3A%2F%2Fgoogle.com"
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
     *
     * @param requestId Request ID to accept
     * @param data Optional data to include in response
     * @return Encoded IPC message (e.g., "ACC:cmd123" or "ACD:cmd123:data")
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
     *
     * @param requestId Request ID to decline
     * @param reason Optional decline reason
     * @return Encoded IPC message (e.g., "DEC:cmd123" or "DCR:cmd123:User cancelled")
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
     *
     * @param requestId Request ID
     * @param callbackUrl Optional callback URL for retry
     * @return Encoded IPC message (e.g., "BSY:cmd123" or "BCF:cmd123:callback://retry")
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
     *
     * @param requestId Request ID that failed
     * @param errorCode Error code
     * @param errorMessage Error message
     * @return Encoded IPC message (e.g., "ERR:cmd123:404:Command not found")
     */
    fun encodeError(requestId: String, errorCode: Int, errorMessage: String): String {
        return "$CODE_ERROR:${escape(requestId)}:$errorCode:${escape(errorMessage)}"
    }

    /**
     * Encode error response (simple version).
     *
     * @param requestId Request ID that failed
     * @param errorMessage Error message
     * @return Encoded IPC message (e.g., "ERR:cmd123:0:Error message")
     */
    fun encodeError(requestId: String, errorMessage: String): String {
        return encodeError(requestId, 0, errorMessage)
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONTENT
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode chat message.
     *
     * @param messageId Message identifier (can be empty for broadcasts)
     * @param senderId Sender identifier
     * @param text Chat text
     * @return Encoded IPC message (e.g., "CHT:msg1:user1:Hello World")
     */
    fun encodeChat(messageId: String = "", senderId: String = "", text: String): String {
        require(text.isNotBlank()) { "text cannot be blank" }
        return "$CODE_CHAT:$messageId:$senderId:${escape(text)}"
    }

    /**
     * Encode URL share.
     *
     * @param sessionId Session identifier
     * @param url URL to share
     * @return Encoded IPC message (e.g., "URL:session1:https%3A%2F%2Fgoogle.com")
     */
    fun encodeUrlShare(sessionId: String, url: String): String {
        return "$CODE_URL:${escape(sessionId)}:${escape(url)}"
    }

    /**
     * Encode navigate command.
     *
     * @param sessionId Session identifier
     * @param url URL to navigate to
     * @return Encoded IPC message (e.g., "NAV:session1:https%3A%2F%2Fgoogle.com")
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
     *
     * @param queryId Query identifier
     * @param query Query text
     * @param context Optional context
     * @return Encoded IPC message (e.g., "AIQ:q1:What's the weather?")
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
     *
     * @param queryId Query identifier being answered
     * @param response Response text
     * @param confidence Optional confidence score (0.0-1.0)
     * @return Encoded IPC message (e.g., "AIR:q1:It's sunny and 72°F")
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
     *
     * @param requestId Request identifier
     * @param jsonOrDsl JSON or UI DSL payload
     * @return Encoded IPC message (e.g., "JSN:ui1:Col{Text{text:\"Hello\"}}")
     */
    fun encodeJson(requestId: String, jsonOrDsl: String): String {
        return "$CODE_JSON:${escape(requestId)}:${escape(jsonOrDsl)}"
    }

    // ════════════════════════════════════════════════════════════════════════
    // VOICE
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Encode speech-to-text result.
     *
     * @param sessionId Session identifier
     * @param transcript Transcribed text
     * @param confidence Confidence score (0.0-1.0)
     * @param isFinal Whether this is a final result
     * @return Encoded IPC message (e.g., "STT:session1:hello world:0.95:true")
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
     *
     * @param sessionId Session identifier
     * @param appId Application identifier
     * @param version Protocol version
     * @return Encoded IPC message (e.g., "HND:session1:com.app:2.0.0")
     */
    fun encodeHandshake(sessionId: String, appId: String, version: String): String {
        return "$CODE_HANDSHAKE:${escape(sessionId)}:${escape(appId)}:${escape(version)}"
    }

    /**
     * Encode ping message.
     *
     * @param sessionId Session identifier
     * @param timestamp Timestamp in milliseconds
     * @return Encoded IPC message (e.g., "PNG:session1:1234567890")
     */
    fun encodePing(sessionId: String, timestamp: Long): String {
        return "$CODE_PING:${escape(sessionId)}:$timestamp"
    }

    /**
     * Encode pong message.
     *
     * @param sessionId Session identifier
     * @param timestamp Original ping timestamp
     * @return Encoded IPC message (e.g., "PON:session1:1234567890")
     */
    fun encodePong(sessionId: String, timestamp: Long): String {
        return "$CODE_PONG:${escape(sessionId)}:$timestamp"
    }

    /**
     * Encode capability announcement.
     *
     * @param sessionId Session identifier
     * @param capabilities List of capabilities
     * @return Encoded IPC message (e.g., "CAP:session1:voice,ai,screen")
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
     * - ':' (delimiter) → %3A
     * - '%' (escape) → %25
     * - '\n' (newline) → %0A
     * - '\r' (carriage return) → %0D
     *
     * @param text Text to escape
     * @return Escaped text safe for IPC transmission
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
     *
     * @param text Escaped text
     * @return Unescaped text
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
     *
     * @param message IPC message to validate
     * @return true if message follows AVU IPC Protocol format
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
     *
     * @param message IPC message
     * @return Protocol code (e.g., "VCM", "ACC") or null if invalid
     */
    fun extractCode(message: String): String? {
        val parts = message.split(DELIMITER, limit = 2)
        val code = parts.getOrNull(0) ?: return null
        return if (code.length == 3 && code.all { it.isUpperCase() }) code else null
    }

    /**
     * Calculate message size reduction vs JSON.
     *
     * @param ipcMessage AVU IPC message
     * @param jsonEquivalent Equivalent JSON message
     * @return Size reduction percentage (0-100)
     */
    fun calculateSizeReduction(ipcMessage: String, jsonEquivalent: String): Int {
        val ipcSize = ipcMessage.encodeToByteArray().size
        val jsonSize = jsonEquivalent.encodeToByteArray().size

        if (jsonSize == 0) return 0

        val reduction = ((jsonSize - ipcSize).toFloat() / jsonSize * 100).toInt()
        return reduction.coerceIn(0, 100)
    }
}

/**
 * Type alias for backward compatibility with UniversalIPCEncoder.
 */
typealias UniversalIPCEncoder = IPCEncoder
