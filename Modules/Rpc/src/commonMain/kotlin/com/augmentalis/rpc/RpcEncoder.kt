/**
 * RpcEncoder.kt - Cross-platform RPC Protocol Encoder
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-28
 * Renamed: 2026-02-02 (UniversalIPCEncoder → RpcEncoder)
 * Updated: 2026-02-03 (Use shared AvuEscape from AVUCodec)
 *
 * Encodes voice commands and messages into RPC Protocol format
 * for cross-app and cross-device communication.
 *
 * Protocol Specification: Avanues RPC Protocol v2.2.0
 * Format: CODE:id:param1:param2:...
 *
 * Voice Command Format: VCM:commandId:action:param1:param2
 * Example: VCM:cmd123:SCROLL_TOP
 *          VCM:cmd456:ZOOM_IN
 *          VCM:cmd789:NAVIGATE:https%3A%2F%2Fgoogle.com
 */
package com.augmentalis.rpc

import com.avanues.avu.codec.core.AvuEscape

/**
 * Cross-platform encoder for RPC Protocol
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
 */
class RpcEncoder {

    companion object {
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

        // Intent extras for RPC broadcast
        const val EXTRA_SOURCE_APP = "com.augmentalis.rpc.EXTRA_SOURCE_APP"
        const val EXTRA_MESSAGE = "com.augmentalis.rpc.EXTRA_MESSAGE"

        // Reserved characters for escaping
        private const val CHAR_DELIMITER = ':'
        private const val CHAR_ESCAPE = '%'
    }

    /**
     * Encode voice command to RPC format
     *
     * @param commandId Unique command identifier (e.g., "cmd123")
     * @param action Command action (e.g., "SCROLL_TOP", "ZOOM_IN")
     * @param params Optional parameters as key-value map
     * @return Encoded RPC message (e.g., "VCM:cmd123:SCROLL_TOP")
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
            append(CHAR_DELIMITER)
            append(escape(commandId))
            append(CHAR_DELIMITER)
            append(escape(action))

            params.forEach { (key, value) ->
                append(CHAR_DELIMITER)
                append(escape("$key=$value"))
            }
        }
    }

    /**
     * Encode accept response
     */
    fun encodeAccept(requestId: String): String {
        require(requestId.isNotBlank()) { "requestId cannot be blank" }
        return "$CODE_ACCEPT:${escape(requestId)}"
    }

    /**
     * Encode decline response
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
     */
    fun encodeError(requestId: String, errorMessage: String): String {
        return "$CODE_ERROR:${escape(requestId)}:${escape(errorMessage)}"
    }

    /**
     * Encode chat message
     */
    fun encodeChat(messageId: String = "", text: String): String {
        require(text.isNotBlank()) { "text cannot be blank" }
        return "$CODE_CHAT:$messageId:${escape(text)}"
    }

    /**
     * Encode URL share
     */
    fun encodeUrlShare(sessionId: String, url: String): String {
        return "$CODE_URL:${escape(sessionId)}:${escape(url)}"
    }

    /**
     * Encode navigate command
     */
    fun encodeNavigate(sessionId: String, url: String): String {
        require(sessionId.isNotBlank()) { "sessionId cannot be blank" }
        require(url.isNotBlank()) { "url cannot be blank" }
        return "$CODE_NAV:${escape(sessionId)}:${escape(url)}"
    }

    /**
     * Encode AI query
     */
    fun encodeAIQuery(queryId: String, query: String): String {
        return "$CODE_AI_QUERY:${escape(queryId)}:${escape(query)}"
    }

    /**
     * Encode AI response
     */
    fun encodeAIResponse(queryId: String, response: String): String {
        return "$CODE_AI_RESPONSE:${escape(queryId)}:${escape(response)}"
    }

    /**
     * Encode JSON/UI DSL wrapper
     */
    fun encodeJson(requestId: String, jsonOrDsl: String): String {
        return "$CODE_JSON:${escape(requestId)}:${escape(jsonOrDsl)}"
    }

    /**
     * Encode generic protocol message
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
     * Escape special characters per RPC Protocol specification.
     *
     * Delegates to [AvuEscape.escape] - the canonical implementation.
     *
     * @see AvuEscape.escape
     */
    private fun escape(text: String): String = AvuEscape.escape(text)

    /**
     * Unescape special characters.
     *
     * Delegates to [AvuEscape.unescape] - the canonical implementation.
     *
     * @see AvuEscape.unescape
     */
    fun unescape(text: String): String = AvuEscape.unescape(text)

    /**
     * Validate RPC message format
     */
    fun isValidMessage(message: String): Boolean {
        if (message.length < 4) return false

        val parts = message.split(CHAR_DELIMITER, limit = 2)
        if (parts.isEmpty()) return false

        val code = parts[0]
        return code.length == 3 && code.all { it.isUpperCase() }
    }

    /**
     * Extract protocol code from message
     */
    fun extractCode(message: String): String? {
        val parts = message.split(CHAR_DELIMITER, limit = 2)
        val code = parts.getOrNull(0) ?: return null
        return if (code.length == 3 && code.all { it.isUpperCase() }) code else null
    }

    /**
     * Calculate message size reduction vs JSON
     */
    fun calculateSizeReduction(rpcMessage: String, jsonEquivalent: String): Int {
        val rpcSize = rpcMessage.encodeToByteArray().size
        val jsonSize = jsonEquivalent.encodeToByteArray().size

        if (jsonSize == 0) return 0

        val reduction = ((jsonSize - rpcSize).toFloat() / jsonSize * 100).toInt()
        return reduction.coerceIn(0, 100)
    }
}

// Type alias for backward compatibility (can be removed after migration)
@Deprecated("Use RpcEncoder instead", ReplaceWith("RpcEncoder"))
typealias UniversalIPCEncoder = RpcEncoder
