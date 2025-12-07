/**
 * UniversalIPCEncoder.kt - Encoder for VoiceOS Universal IPC Protocol
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-11-22
 *
 * Encodes voice commands and messages into Universal IPC Protocol format
 * for cross-app communication with WebAvanue, AVA AI, AvaConnect, etc.
 *
 * VoiceOS IPC Action: com.augmentalis.voiceos.IPC.COMMAND
 *
 * Protocol Specification: Avanues Universal IPC Protocol v2.0.0
 * Format: CODE:id:param1:param2:...
 *
 * Voice Command Format: VCM:commandId:action:param1:param2
 * Example: VCM:cmd123:SCROLL_TOP
 *          VCM:cmd456:ZOOM_IN
 *          VCM:cmd789:NAVIGATE:https%3A%2F%2Fgoogle.com
 */
package com.augmentalis.universalipc

import android.util.Log

/**
 * Encoder for Universal IPC Protocol
 *
 * Supports all 77 protocol codes across 10 categories:
 * - Requests (VCA, ACA, FTR, SSO, SSI, WBS, RCO, RCI, BRS, MSG, AIQ, UIC)
 * - Responses (ACC, ACD, DEC, DCR, BSY, BCF, ERR, TMO, RDR)
 * - Events (CON, DIS, RCN, ICE, ICS, DCO, DCC, PJN, PLF)
 * - State (MIC, CAM, SPK, REC, NET, BAT, VOL, ORI)
 * - Content (CHT, FCH, WBD, ANN, TYP, RCP, DLV, BIN, JSN)
 * - Voice (VCM, STT, TTS, WWD, ART, VRS, VRP)
 * - Browser (URL, NAV, BMK, TAB, PLD, DWN, HST)
 * - AI (AIQ, AIR, CTX, SUG, TSK, LRN)
 * - System (HND, PNG, PON, CAP, MET, DBG, WRN)
 * - Server (PRO, PRI, ROL)
 *
 * For VoiceOS → WebAvanue, primarily uses VCM (Voice Command) code.
 */
class UniversalIPCEncoder {

    companion object {
        private const val TAG = "UniversalIPCEncoder"

        // VoiceOS IPC action for Intent broadcasts
        const val IPC_ACTION = "com.augmentalis.voiceos.IPC.COMMAND"
        const val EXTRA_MESSAGE = "message"
        const val EXTRA_SOURCE_APP = "source_app"

        // Protocol codes
        const val CODE_VOICE_COMMAND = "VCM"  // Voice Command
        const val CODE_ACCEPT = "ACC"         // Accept response
        const val CODE_DECLINE = "DEC"        // Decline response
        const val CODE_ERROR = "ERR"          // Error response
        const val CODE_CHAT = "CHT"           // Chat message
        const val CODE_URL = "URL"            // URL share
        const val CODE_NAV = "NAV"            // Navigate to URL
        const val CODE_AI_QUERY = "AIQ"       // AI query
        const val CODE_AI_RESPONSE = "AIR"    // AI response
        const val CODE_JSON = "JSN"           // JSON/UI DSL wrapper

        // Reserved characters for escaping
        private const val CHAR_DELIMITER = ':'
        private const val CHAR_ESCAPE = '%'
        private const val CHAR_NEWLINE = '\n'
        private const val CHAR_RETURN = '\r'
    }

    /**
     * Encode voice command to Universal IPC format
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
            append(CHAR_DELIMITER)
            append(escape(commandId))
            append(CHAR_DELIMITER)
            append(escape(action))

            // Add optional parameters
            params.forEach { (key, value) ->
                append(CHAR_DELIMITER)
                append(escape("$key=$value"))
            }
        }.also { message ->
            Log.d(TAG, "Encoded voice command: $message")
        }
    }

    /**
     * Encode accept response
     *
     * @param requestId Request ID to accept
     * @return Encoded IPC message (e.g., "ACC:cmd123")
     * @throws IllegalArgumentException if requestId is blank
     */
    fun encodeAccept(requestId: String): String {
        require(requestId.isNotBlank()) { "requestId cannot be blank" }
        return "$CODE_ACCEPT:${escape(requestId)}"
    }

    /**
     * Encode decline response
     *
     * @param requestId Request ID to decline
     * @param reason Optional decline reason
     * @return Encoded IPC message (e.g., "DEC:cmd123" or "DCR:cmd123:User cancelled")
     */
    fun encodeDecline(requestId: String, reason: String? = null): String {
        return if (reason != null) {
            "DCR:${escape(requestId)}:${escape(reason)}"
        } else {
            "$CODE_DECLINE:${escape(requestId)}"
        }
    }

    /**
     * Encode error response
     *
     * @param requestId Request ID that failed
     * @param errorMessage Error message
     * @return Encoded IPC message (e.g., "ERR:cmd123:Command not found")
     */
    fun encodeError(requestId: String, errorMessage: String): String {
        return "$CODE_ERROR:${escape(requestId)}:${escape(errorMessage)}"
    }

    /**
     * Encode chat message
     *
     * @param messageId Message identifier (can be empty for broadcasts)
     * @param text Chat text
     * @return Encoded IPC message (e.g., "CHT:msg1:Hello World")
     * @throws IllegalArgumentException if text is blank
     */
    fun encodeChat(messageId: String = "", text: String): String {
        require(text.isNotBlank()) { "text cannot be blank" }
        return "$CODE_CHAT:$messageId:${escape(text)}"
    }

    /**
     * Encode URL share
     *
     * @param sessionId Session identifier
     * @param url URL to share
     * @return Encoded IPC message (e.g., "URL:session1:https%3A%2F%2Fgoogle.com")
     */
    fun encodeUrlShare(sessionId: String, url: String): String {
        return "$CODE_URL:${escape(sessionId)}:${escape(url)}"
    }

    /**
     * Encode navigate command
     *
     * @param sessionId Session identifier
     * @param url URL to navigate to
     * @return Encoded IPC message (e.g., "NAV:session1:https%3A%2F%2Fgoogle.com")
     * @throws IllegalArgumentException if sessionId or url is blank
     */
    fun encodeNavigate(sessionId: String, url: String): String {
        require(sessionId.isNotBlank()) { "sessionId cannot be blank" }
        require(url.isNotBlank()) { "url cannot be blank" }
        return "$CODE_NAV:${escape(sessionId)}:${escape(url)}"
    }

    /**
     * Encode AI query
     *
     * @param queryId Query identifier
     * @param query Query text
     * @return Encoded IPC message (e.g., "AIQ:q1:What's the weather?")
     */
    fun encodeAIQuery(queryId: String, query: String): String {
        return "$CODE_AI_QUERY:${escape(queryId)}:${escape(query)}"
    }

    /**
     * Encode AI response
     *
     * @param queryId Query identifier being answered
     * @param response Response text
     * @return Encoded IPC message (e.g., "AIR:q1:It's sunny and 72°F")
     */
    fun encodeAIResponse(queryId: String, response: String): String {
        return "$CODE_AI_RESPONSE:${escape(queryId)}:${escape(response)}"
    }

    /**
     * Encode JSON/UI DSL wrapper
     *
     * @param requestId Request identifier
     * @param jsonOrDsl JSON or UI DSL payload
     * @return Encoded IPC message (e.g., "JSN:ui1:Col{Text{text:\"Hello\"}}")
     */
    fun encodeJson(requestId: String, jsonOrDsl: String): String {
        return "$CODE_JSON:${escape(requestId)}:${escape(jsonOrDsl)}"
    }

    /**
     * Encode generic protocol message
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
            append(CHAR_DELIMITER)
            append(escape(id))

            params.forEach { param ->
                append(CHAR_DELIMITER)
                append(escape(param))
            }
        }
    }

    /**
     * Escape special characters per Universal IPC Protocol specification
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
    private fun escape(text: String): String {
        // Must escape '%' first to avoid double-escaping
        return text
            .replace("%", "%25")   // Escape character
            .replace(":", "%3A")   // Parameter delimiter
            .replace("\n", "%0A")  // Newline
            .replace("\r", "%0D")  // Carriage return
    }

    /**
     * Unescape special characters (for decoding, if needed)
     *
     * @param text Escaped text
     * @return Unescaped text
     */
    fun unescape(text: String): String {
        // Must unescape '%' last to avoid double-unescaping
        return text
            .replace("%0D", "\r")  // Carriage return
            .replace("%0A", "\n")  // Newline
            .replace("%3A", ":")   // Parameter delimiter
            .replace("%25", "%")   // Escape character
    }

    /**
     * Validate IPC message format
     *
     * @param message IPC message to validate
     * @return true if message follows Universal IPC Protocol format
     */
    fun isValidMessage(message: String): Boolean {
        if (message.length < 4) return false // Minimum: "ABC:"

        val parts = message.split(CHAR_DELIMITER, limit = 2)
        if (parts.isEmpty()) return false

        val code = parts[0]
        return code.length == 3 && code.all { it.isUpperCase() }
    }

    /**
     * Extract protocol code from message
     *
     * @param message IPC message
     * @return Protocol code (e.g., "VCM", "ACC") or null if invalid
     */
    fun extractCode(message: String): String? {
        val parts = message.split(CHAR_DELIMITER, limit = 2)
        val code = parts.getOrNull(0) ?: return null
        return if (code.length == 3 && code.all { it.isUpperCase() }) code else null
    }

    /**
     * Calculate message size reduction vs JSON
     *
     * @param ipcMessage Universal IPC message
     * @param jsonEquivalent Equivalent JSON message
     * @return Size reduction percentage (0-100)
     */
    fun calculateSizeReduction(ipcMessage: String, jsonEquivalent: String): Int {
        val ipcSize = ipcMessage.toByteArray().size
        val jsonSize = jsonEquivalent.toByteArray().size

        // Prevent division by zero
        if (jsonSize == 0) {
            Log.d(TAG, "Size: IPC=$ipcSize bytes, JSON=0 bytes (empty)")
            return 0
        }

        val reduction = ((jsonSize - ipcSize).toFloat() / jsonSize * 100).toInt()
        Log.d(TAG, "Size: IPC=$ipcSize bytes, JSON=$jsonSize bytes, Reduction=$reduction%")
        return reduction.coerceIn(0, 100)
    }
}
