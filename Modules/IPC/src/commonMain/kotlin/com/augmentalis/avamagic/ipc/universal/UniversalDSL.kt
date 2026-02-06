package com.augmentalis.avamagic.ipc.universal

import com.augmentalis.avamagic.ipc.*
import com.avanues.avu.codec.core.AvuEscape

/**
 * Avanues Universal DSL - Unified RPC Protocol
 *
 * Single protocol for all remote procedure communication across Avanues ecosystem.
 *
 * Features:
 * - 77 protocol message codes (3-letter mnemonic)
 * - UI component DSL integration (Type#id{props})
 * - 60-87% smaller than JSON
 * - Human-readable for debugging
 * - Platform-agnostic (KMP)
 *
 * Usage:
 * ```kotlin
 * // Send video call request
 * val request = VideoCallRequest("call1", "Pixel7", "Manoj")
 * ipcManager.send(request.serialize())
 *
 * // Parse incoming message
 * val parsed = UniversalDSL.parse(message)
 * when (parsed) {
 *     is ParseResult.Protocol -> handleProtocol(parsed.message)
 *     is ParseResult.UIComponent -> renderUI(parsed.dsl)
 *     is ParseResult.WrappedUI -> renderUI(parsed.dsl)
 * }
 * ```
 *
 * Specification: /Volumes/M-Drive/Coding/Avanues/docs/specifications/AVANUES-UNIVERSAL-DSL-SPEC.md
 *
 * @author Manoj Jhawar (manoj@ideahq.net)
 * @since 2.0.0
 */
object UniversalDSL {

    const val VERSION = "2.2.0"
    const val DELIMITER = ":"
    const val ESCAPE_CHAR = "%"

    /**
     * Parse message and detect type
     */
    fun parse(message: String): ParseResult {
        return try {
            when {
                // Check for JSN wrapper first
                message.startsWith("JSN:") && message.contains("{") -> {
                    val parts = message.split(DELIMITER, limit = 3)
                    if (parts.size >= 3) {
                        ParseResult.WrappedUI(
                            requestId = parts[1],
                            dsl = parts[2]
                        )
                    } else {
                        ParseResult.Unknown(message)
                    }
                }

                // Check for UI component
                message.contains("{") && !message.contains(DELIMITER) -> {
                    ParseResult.UIComponent(message)
                }

                // Check for protocol message
                message.contains(DELIMITER) -> {
                    val parsed = parseProtocol(message)
                    if (parsed != null) {
                        ParseResult.Protocol(parsed)
                    } else {
                        ParseResult.Unknown(message)
                    }
                }

                // Unknown format
                else -> ParseResult.Unknown(message)
            }
        } catch (e: Exception) {
            ParseResult.Error(message, e.message ?: "Parse error")
        }
    }

    /**
     * Parse protocol message (CODE:id:params)
     */
    private fun parseProtocol(message: String): UniversalMessage? {
        val parts = message.split(DELIMITER)
        if (parts.isEmpty()) return null

        val code = parts[0]
        val id = parts.getOrNull(1) ?: ""
        val params = parts.drop(2).map { unescape(it) }

        return parseByCode(code, id, params)
    }

    /**
     * Parse by 3-letter code
     */
    private fun parseByCode(code: String, id: String, params: List<String>): UniversalMessage? {
        return when (code) {
            // ────────────────────────────────────────────────────────────────
            // REQUESTS
            // ────────────────────────────────────────────────────────────────
            "VCA" -> VideoCallRequest(
                requestId = id,
                fromDevice = params.getOrNull(0) ?: "",
                fromName = params.getOrNull(1)
            )

            "FTR" -> FileTransferRequest(
                requestId = id,
                fileName = params.getOrNull(0) ?: "",
                fileSize = params.getOrNull(1)?.toLongOrNull() ?: 0L,
                fileCount = params.getOrNull(2)?.toIntOrNull() ?: 1
            )

            "SSO" -> ScreenShareRequest(
                requestId = id,
                direction = ScreenShareDirection.OUTGOING,
                width = params.getOrNull(0)?.toIntOrNull(),
                height = params.getOrNull(1)?.toIntOrNull(),
                fps = params.getOrNull(2)?.toIntOrNull()
            )

            "SSI" -> ScreenShareRequest(
                requestId = id,
                direction = ScreenShareDirection.INCOMING,
                width = params.getOrNull(0)?.toIntOrNull(),
                height = params.getOrNull(1)?.toIntOrNull(),
                fps = params.getOrNull(2)?.toIntOrNull()
            )

            "WBS" -> WhiteboardRequest(
                requestId = id,
                fromDevice = params.getOrNull(0) ?: ""
            )

            "RCO" -> RemoteControlRequest(
                requestId = id,
                direction = RemoteControlDirection.OUTGOING,
                fromDevice = params.getOrNull(0) ?: ""
            )

            "RCI" -> RemoteControlRequest(
                requestId = id,
                direction = RemoteControlDirection.INCOMING,
                fromDevice = params.getOrNull(0) ?: ""
            )

            // ────────────────────────────────────────────────────────────────
            // RESPONSES
            // ────────────────────────────────────────────────────────────────
            "ACC" -> AcceptResponse(requestId = id, metadata = null)

            "ACD" -> {
                val metadata = if (params.isNotEmpty()) {
                    params.chunked(2).associate { pair ->
                        pair[0] to (pair.getOrNull(1) ?: "")
                    }
                } else null
                AcceptResponse(requestId = id, metadata = metadata)
            }

            "DEC" -> DeclineResponse(requestId = id, reason = null)

            "DCR" -> DeclineResponse(
                requestId = id,
                reason = params.firstOrNull()
            )

            "BSY" -> BusyResponse(requestId = id, currentFeature = null)

            "BCF" -> BusyResponse(
                requestId = id,
                currentFeature = params.firstOrNull()
            )

            "ERR" -> ErrorResponse(
                requestId = id,
                errorMessage = params.firstOrNull() ?: "Unknown error"
            )

            // ────────────────────────────────────────────────────────────────
            // EVENTS
            // ────────────────────────────────────────────────────────────────
            "CON" -> ConnectedEvent(
                sessionId = id,
                ipAddress = params.firstOrNull()
            )

            "DIS" -> DisconnectedEvent(
                sessionId = id,
                reason = params.firstOrNull()
            )

            "ICE" -> ICEReadyEvent(sessionId = id)

            "DCO" -> DataChannelOpenEvent(sessionId = id)

            "DCC" -> DataChannelCloseEvent(sessionId = id)

            // ────────────────────────────────────────────────────────────────
            // STATE
            // ────────────────────────────────────────────────────────────────
            "MIC" -> MicrophoneState(
                sessionId = id,
                enabled = params.firstOrNull()?.toIntOrNull() == 1
            )

            "CAM" -> CameraState(
                sessionId = id,
                enabled = params.firstOrNull()?.toIntOrNull() == 1
            )

            "REC" -> RecordingState(
                sessionId = id,
                state = when (params.firstOrNull()?.lowercase()) {
                    "start" -> RecordingStateValue.START
                    "stop" -> RecordingStateValue.STOP
                    "pause" -> RecordingStateValue.PAUSE
                    "resume" -> RecordingStateValue.RESUME
                    else -> RecordingStateValue.STOP
                },
                fileName = params.getOrNull(1)
            )

            // ────────────────────────────────────────────────────────────────
            // CONTENT
            // ────────────────────────────────────────────────────────────────
            "CHT" -> ChatMessage(
                messageId = id.takeIf { it.isNotEmpty() },
                text = params.joinToString(DELIMITER)
            )

            "JSN" -> UIComponentMessage(
                requestId = id,
                componentDSL = params.joinToString(DELIMITER)
            )

            // ────────────────────────────────────────────────────────────────
            // VOICE
            // ────────────────────────────────────────────────────────────────
            "VCM" -> VoiceCommandMessage(
                commandId = id,
                command = params.joinToString(DELIMITER)
            )

            "STT" -> SpeechToTextMessage(
                sessionId = id,
                text = params.joinToString(DELIMITER)
            )

            // ────────────────────────────────────────────────────────────────
            // BROWSER
            // ────────────────────────────────────────────────────────────────
            "URL" -> URLShareMessage(
                sessionId = id,
                url = params.firstOrNull() ?: ""
            )

            "NAV" -> NavigateMessage(
                sessionId = id,
                url = params.firstOrNull() ?: ""
            )

            "TAB" -> TabEventMessage(
                sessionId = id,
                action = params.getOrNull(0) ?: "",
                tabId = params.getOrNull(1)
            )

            "PLD" -> PageLoadedMessage(
                sessionId = id,
                url = params.firstOrNull() ?: ""
            )

            // ────────────────────────────────────────────────────────────────
            // AI
            // ────────────────────────────────────────────────────────────────
            "AIQ" -> AIQueryMessage(
                queryId = id,
                query = params.joinToString(DELIMITER)
            )

            "AIR" -> AIResponseMessage(
                queryId = id,
                response = params.joinToString(DELIMITER)
            )

            // ────────────────────────────────────────────────────────────────
            // SYSTEM
            // ────────────────────────────────────────────────────────────────
            "HND" -> HandshakeMessage(
                protocolVersion = id,
                appVersion = params.getOrNull(0) ?: "",
                deviceId = params.getOrNull(1) ?: ""
            )

            "PNG" -> PingMessage(timestamp = id.toLongOrNull() ?: 0L)

            "PON" -> PongMessage(timestamp = id.toLongOrNull() ?: 0L)

            "CAP" -> CapabilityMessage(
                capabilities = params.firstOrNull()?.split(",") ?: emptyList()
            )

            // ────────────────────────────────────────────────────────────────
            // SERVER
            // ────────────────────────────────────────────────────────────────
            "PRO" -> PromotionMessage(
                deviceId = id,
                priority = params.getOrNull(0)?.toIntOrNull() ?: 0,
                timestamp = params.getOrNull(1)?.toLongOrNull() ?: 0L
            )

            "ROL" -> RoleChangeMessage(
                deviceId = id,
                role = when (params.firstOrNull()?.lowercase()) {
                    "server" -> Role.SERVER
                    else -> Role.CLIENT
                }
            )

            else -> null
        }
    }

    /**
     * Escape special characters.
     *
     * Delegates to [AvuEscape.escape] - the canonical implementation.
     *
     * @see AvuEscape.escape
     */
    fun escape(text: String): String = AvuEscape.escape(text)

    /**
     * Unescape URL-encoded text.
     *
     * Delegates to [AvuEscape.unescape] - the canonical implementation.
     *
     * @see AvuEscape.unescape
     */
    fun unescape(text: String): String = AvuEscape.unescape(text)
}

/**
 * Parse result wrapper
 */
sealed class ParseResult {
    data class Protocol(val message: UniversalMessage) : ParseResult()
    data class UIComponent(val dsl: String) : ParseResult()
    data class WrappedUI(val requestId: String, val dsl: String) : ParseResult()
    data class Unknown(val raw: String) : ParseResult()
    data class Error(val raw: String, val error: String) : ParseResult()
}
