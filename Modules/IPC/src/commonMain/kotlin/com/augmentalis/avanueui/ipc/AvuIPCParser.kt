package com.augmentalis.avanueui.ipc

import com.avanues.avu.codec.core.AvuEscape

/**
 * AVU RPC Format Parser
 *
 * Parses AVU (Avanues Universal) format strings into IPC model objects.
 * Supports all IPC-specific prefixes for connection management, resilience,
 * method invocation, query operations, errors, metrics, and messages.
 *
 * AVU Format: PREFIX:field1:field2:field3:...
 *
 * Supported Prefixes:
 * - Connection: CON (connected event), CST (state change), CPL (protocol)
 * - Resilience: RCP (reconnection policy), CBK (circuit breaker), RLM (rate limit), RSL (resource limits)
 * - Method: MTH (method invocation), MRS (method result success), MRE (method result error)
 * - Query: QRY (query params), QRS (query result)
 * - Error: ERR (with error codes)
 * - Metrics: MET (full metrics)
 * - Messages: All message prefixes from IPCMessages
 *   - Request: VCA (video call), FTR (file transfer), SSO/SSI (screen share), WBS (whiteboard), RCO/RCI (remote control)
 *   - Response: ACC/ACD (accept), DEC/DCR (decline), BSY/BCF (busy), ERR (error)
 *   - Events: CON (connected), DIS (disconnected), ICE (ice ready), DCO/DCC (data channel)
 *   - State: MIC (microphone), CAM (camera), REC (recording)
 *   - Content: CHT (chat), JSN (UI component)
 *   - Voice: VCM (voice command), STT (speech to text)
 *   - Browser: URL (share), NAV (navigate), TAB (tab event), PLD (page loaded)
 *   - AI: AIQ (query), AIR (response)
 *   - System: HND (handshake), PNG (ping), PON (pong), CAP (capability)
 *   - Server: PRO (promotion), ROL (role change)
 *
 * @author Augmentalis Engineering
 * @since 1.0.0
 */
object AvuIPCParser {

    private const val DELIMITER = ":"

    /**
     * Parse AVU format string to an IPC model.
     *
     * Returns either:
     * - [UniversalMessage] for message types
     * - [AvuIPCModel] for IPC-specific types (connection, resilience, etc.)
     *
     * @param avu The AVU format string
     * @return Parsed model or null if invalid
     */
    fun parse(avu: String): Any? {
        if (avu.isBlank()) return null

        val parts = splitAvu(avu)
        if (parts.isEmpty()) return null

        val prefix = parts[0].uppercase()
        val fields = parts.drop(1).map { unescape(it) }

        return parseByPrefix(prefix, fields)
    }

    /**
     * Parse AVU format string expecting a UniversalMessage.
     *
     * @param avu The AVU format string
     * @return Parsed UniversalMessage or null if not a message type
     */
    fun parseMessage(avu: String): UniversalMessage? {
        return parse(avu) as? UniversalMessage
    }

    /**
     * Parse AVU format string expecting an AvuIPCModel.
     *
     * @param avu The AVU format string
     * @return Parsed AvuIPCModel or null if not an IPC model type
     */
    fun parseIPCModel(avu: String): AvuIPCModel? {
        return parse(avu) as? AvuIPCModel
    }

    /**
     * Parse multiple AVU lines.
     */
    fun parseAll(avuLines: String): List<Any> {
        return avuLines.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { parse(it) }
    }

    /**
     * Parse multiple AVU lines, returning only UniversalMessage types.
     */
    fun parseAllMessages(avuLines: String): List<UniversalMessage> {
        return parseAll(avuLines).filterIsInstance<UniversalMessage>()
    }

    /**
     * Split AVU string by delimiter, respecting escaped delimiters.
     */
    private fun splitAvu(avu: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0

        while (i < avu.length) {
            when {
                // Check for escaped delimiter (%3A)
                i + 2 < avu.length && avu.substring(i, i + 3) == "%3A" -> {
                    current.append("%3A")
                    i += 3
                }
                avu[i] == ':' -> {
                    result.add(current.toString())
                    current.clear()
                    i++
                }
                else -> {
                    current.append(avu[i])
                    i++
                }
            }
        }
        result.add(current.toString())
        return result
    }

    private fun parseByPrefix(prefix: String, fields: List<String>): Any? {
        return when (prefix) {
            // ════════════════════════════════════════════════════════════════
            // CONNECTION MANAGEMENT (AVU IPC specific)
            // ════════════════════════════════════════════════════════════════
            "CST" -> parseConnectionStateChange(fields)
            "CPL" -> parseProtocolTypeInfo(fields)

            // ════════════════════════════════════════════════════════════════
            // RESILIENCE (AVU IPC specific)
            // ════════════════════════════════════════════════════════════════
            "RCP" -> parseAvuReconnectionPolicy(fields)
            "CBK" -> parseAvuCircuitBreakerState(fields)
            "RLM" -> parseAvuRateLimitConfig(fields)
            "RSL" -> parseAvuResourceLimits(fields)

            // ════════════════════════════════════════════════════════════════
            // METHOD INVOCATION (AVU IPC specific)
            // ════════════════════════════════════════════════════════════════
            "MTH" -> parseAvuMethodInvoke(fields)
            "MRS" -> parseAvuMethodResultSuccess(fields)
            "MRE" -> parseAvuMethodResultError(fields)

            // ════════════════════════════════════════════════════════════════
            // QUERY OPERATIONS (AVU IPC specific)
            // ════════════════════════════════════════════════════════════════
            "QRY" -> parseAvuQueryParams(fields)
            "QRS" -> parseAvuQueryResult(fields)

            // ════════════════════════════════════════════════════════════════
            // ERRORS (AVU IPC specific - different from ERR message)
            // ════════════════════════════════════════════════════════════════
            // Note: ERR prefix handled in message section for ErrorResponse
            // Use ERRI for IPC error info
            "ERRI" -> parseAvuError(fields)

            // ════════════════════════════════════════════════════════════════
            // METRICS (AVU IPC specific)
            // ════════════════════════════════════════════════════════════════
            "MET" -> parseAvuMetrics(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - REQUEST (using existing IPCMessages classes)
            // ════════════════════════════════════════════════════════════════
            "VCA" -> parseVideoCallRequest(fields)
            "FTR" -> parseFileTransferRequest(fields)
            "SSO" -> parseScreenShareRequest(fields, ScreenShareDirection.OUTGOING)
            "SSI" -> parseScreenShareRequest(fields, ScreenShareDirection.INCOMING)
            "WBS" -> parseWhiteboardRequest(fields)
            "RCO" -> parseRemoteControlRequest(fields, RemoteControlDirection.OUTGOING)
            "RCI" -> parseRemoteControlRequest(fields, RemoteControlDirection.INCOMING)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - RESPONSE
            // ════════════════════════════════════════════════════════════════
            "ACC" -> parseAcceptResponse(fields, withMetadata = false)
            "ACD" -> parseAcceptResponse(fields, withMetadata = true)
            "DEC" -> parseDeclineResponse(fields, withReason = false)
            "DCR" -> parseDeclineResponse(fields, withReason = true)
            "BSY" -> parseBusyResponse(fields, withFeature = false)
            "BCF" -> parseBusyResponse(fields, withFeature = true)
            "ERR" -> parseErrorResponse(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - EVENTS
            // ════════════════════════════════════════════════════════════════
            "CON" -> parseConnectedEvent(fields)
            "DIS" -> parseDisconnectedEvent(fields)
            "ICE" -> parseICEReadyEvent(fields)
            "DCO" -> parseDataChannelOpenEvent(fields)
            "DCC" -> parseDataChannelCloseEvent(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - STATE
            // ════════════════════════════════════════════════════════════════
            "MIC" -> parseMicrophoneState(fields)
            "CAM" -> parseCameraState(fields)
            "REC" -> parseRecordingState(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - CONTENT
            // ════════════════════════════════════════════════════════════════
            "CHT" -> parseChatMessage(fields)
            "JSN" -> parseUIComponentMessage(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - VOICE
            // ════════════════════════════════════════════════════════════════
            "VCM" -> parseVoiceCommandMessage(fields)
            "STT" -> parseSpeechToTextMessage(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - BROWSER
            // ════════════════════════════════════════════════════════════════
            "URL" -> parseURLShareMessage(fields)
            "NAV" -> parseNavigateMessage(fields)
            "TAB" -> parseTabEventMessage(fields)
            "PLD" -> parsePageLoadedMessage(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - AI
            // ════════════════════════════════════════════════════════════════
            "AIQ" -> parseAIQueryMessage(fields)
            "AIR" -> parseAIResponseMessage(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - SYSTEM
            // ════════════════════════════════════════════════════════════════
            "HND" -> parseHandshakeMessage(fields)
            "PNG" -> parsePingMessage(fields)
            "PON" -> parsePongMessage(fields)
            "CAP" -> parseCapabilityMessage(fields)

            // ════════════════════════════════════════════════════════════════
            // MESSAGES - SERVER
            // ════════════════════════════════════════════════════════════════
            "PRO" -> parsePromotionMessage(fields)
            "ROL" -> parseRoleChangeMessage(fields)

            else -> null
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONNECTION MANAGEMENT PARSERS
    // ════════════════════════════════════════════════════════════════════════

    // CST:state
    private fun parseConnectionStateChange(fields: List<String>): AvuConnectionStateChange? {
        if (fields.isEmpty()) return null
        return AvuConnectionStateChange(
            state = parseConnectionStateValue(fields[0])
        )
    }

    // CPL:protocol
    private fun parseProtocolTypeInfo(fields: List<String>): AvuProtocolTypeInfo? {
        if (fields.isEmpty()) return null
        return AvuProtocolTypeInfo(
            protocol = parseIPCProtocolValue(fields[0])
        )
    }

    private fun parseConnectionStateValue(value: String): ConnectionState {
        return when (value.uppercase()) {
            "DISCONNECTED" -> ConnectionState.DISCONNECTED
            "CONNECTING" -> ConnectionState.CONNECTING
            "CONNECTED" -> ConnectionState.CONNECTED
            "RECONNECTING" -> ConnectionState.RECONNECTING
            "FAILED" -> ConnectionState.FAILED
            "DISCONNECTING" -> ConnectionState.DISCONNECTING
            else -> ConnectionState.DISCONNECTED
        }
    }

    private fun parseIPCProtocolValue(value: String): IPCProtocol {
        return when (value.uppercase()) {
            "AIDL" -> IPCProtocol.AIDL
            "CONTENT_PROVIDER" -> IPCProtocol.CONTENT_PROVIDER
            "WEBSOCKET" -> IPCProtocol.WEBSOCKET
            "URL_SCHEME" -> IPCProtocol.URL_SCHEME
            "XPC" -> IPCProtocol.XPC
            "NAMED_PIPE" -> IPCProtocol.NAMED_PIPE
            else -> IPCProtocol.WEBSOCKET
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESILIENCE PARSERS
    // ════════════════════════════════════════════════════════════════════════

    // RCP:enabled:retries:delay:max:mult
    private fun parseAvuReconnectionPolicy(fields: List<String>): AvuReconnectionPolicy? {
        if (fields.size < 5) return null
        return AvuReconnectionPolicy(
            enabled = fields[0].toBooleanStrictOrNull() ?: true,
            maxRetries = fields[1].toIntOrNull() ?: 3,
            initialDelayMs = fields[2].toLongOrNull() ?: 1000L,
            maxDelayMs = fields[3].toLongOrNull() ?: 30000L,
            multiplier = fields[4].toFloatOrNull() ?: 2.0f
        )
    }

    // CBK:failures:successes:timeout:state
    private fun parseAvuCircuitBreakerState(fields: List<String>): AvuCircuitBreakerState? {
        if (fields.size < 4) return null
        return AvuCircuitBreakerState(
            failureThreshold = fields[0].toIntOrNull() ?: 5,
            successThreshold = fields[1].toIntOrNull() ?: 2,
            timeoutMs = fields[2].toLongOrNull() ?: 60000L,
            state = parseCircuitStateValue(fields[3])
        )
    }

    private fun parseCircuitStateValue(value: String): CircuitState {
        return when (value.uppercase()) {
            "CLOSED" -> CircuitState.CLOSED
            "OPEN" -> CircuitState.OPEN
            "HALF_OPEN" -> CircuitState.HALF_OPEN
            else -> CircuitState.CLOSED
        }
    }

    // RLM:rps:burst
    private fun parseAvuRateLimitConfig(fields: List<String>): AvuRateLimitConfig? {
        if (fields.size < 2) return null
        return AvuRateLimitConfig(
            maxRequestsPerSecond = fields[0].toIntOrNull() ?: 10,
            burstSize = fields[1].toIntOrNull() ?: 20
        )
    }

    // RSL:conns:msgsize:ctimeout:mtimeout:qtimeout
    private fun parseAvuResourceLimits(fields: List<String>): AvuResourceLimits? {
        if (fields.size < 5) return null
        return AvuResourceLimits(
            maxConnections = fields[0].toIntOrNull() ?: 32,
            maxMessageSize = fields[1].toIntOrNull() ?: 1_048_576,
            connectionTimeoutMs = fields[2].toLongOrNull() ?: 5000L,
            methodTimeoutMs = fields[3].toLongOrNull() ?: 10000L,
            queryTimeoutMs = fields[4].toLongOrNull() ?: 5000L
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // METHOD INVOCATION PARSERS
    // ════════════════════════════════════════════════════════════════════════

    // MTH:name:timeout:params...
    private fun parseAvuMethodInvoke(fields: List<String>): AvuMethodInvoke? {
        if (fields.size < 2) return null
        return AvuMethodInvoke(
            methodName = fields[0],
            timeoutMs = fields[1].toLongOrNull() ?: 10000L,
            parameters = if (fields.size > 2) fields.drop(2) else emptyList()
        )
    }

    // MRS:value
    private fun parseAvuMethodResultSuccess(fields: List<String>): AvuMethodResultSuccess {
        return AvuMethodResultSuccess(
            value = fields.firstOrNull() ?: ""
        )
    }

    // MRE:code:message
    private fun parseAvuMethodResultError(fields: List<String>): AvuMethodResultError? {
        if (fields.size < 2) return null
        return AvuMethodResultError(
            errorCode = fields[0],
            errorMessage = fields[1]
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // QUERY OPERATION PARSERS
    // ════════════════════════════════════════════════════════════════════════

    // QRY:uri:proj:sel:args:sort:limit
    private fun parseAvuQueryParams(fields: List<String>): AvuQueryParams? {
        if (fields.isEmpty()) return null
        return AvuQueryParams(
            uri = fields[0],
            projection = fields.getOrNull(1)?.takeIf { it.isNotEmpty() }?.split(","),
            selection = fields.getOrNull(2)?.takeIf { it.isNotEmpty() },
            selectionArgs = fields.getOrNull(3)?.takeIf { it.isNotEmpty() }?.split(","),
            sortOrder = fields.getOrNull(4)?.takeIf { it.isNotEmpty() },
            limit = fields.getOrNull(5)?.toIntOrNull()
        )
    }

    // QRS:count:rows...
    private fun parseAvuQueryResult(fields: List<String>): AvuQueryResult? {
        if (fields.isEmpty()) return null
        val count = fields[0].toIntOrNull() ?: 0
        val rows = if (fields.size > 1) fields.drop(1) else emptyList()
        return AvuQueryResult(
            rowCount = count,
            rows = rows
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // ERROR PARSER
    // ════════════════════════════════════════════════════════════════════════

    // ERRI:code:message:details (IPC error info, distinct from ERR message response)
    private fun parseAvuError(fields: List<String>): AvuIPCErrorInfo? {
        if (fields.size < 2) return null
        return AvuIPCErrorInfo(
            errorCode = fields[0],
            errorMessage = fields[1],
            details = fields.getOrNull(2)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // METRICS PARSER
    // ════════════════════════════════════════════════════════════════════════

    // MET:active:total:failed:sent:recv:avg:p95:p99:rate:up
    private fun parseAvuMetrics(fields: List<String>): AvuIPCMetrics? {
        if (fields.size < 10) return null
        return AvuIPCMetrics(
            activeConnections = fields[0].toIntOrNull() ?: 0,
            totalRequests = fields[1].toLongOrNull() ?: 0L,
            failedRequests = fields[2].toLongOrNull() ?: 0L,
            bytesSent = fields[3].toLongOrNull() ?: 0L,
            bytesReceived = fields[4].toLongOrNull() ?: 0L,
            avgLatencyMs = fields[5].toDoubleOrNull() ?: 0.0,
            p95LatencyMs = fields[6].toLongOrNull() ?: 0L,
            p99LatencyMs = fields[7].toLongOrNull() ?: 0L,
            requestsPerSecond = fields[8].toDoubleOrNull() ?: 0.0,
            uptimeMs = fields[9].toLongOrNull() ?: 0L
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - REQUEST (returning existing IPCMessages classes)
    // ════════════════════════════════════════════════════════════════════════

    private fun parseVideoCallRequest(fields: List<String>): VideoCallRequest? {
        if (fields.isEmpty()) return null
        return VideoCallRequest(
            requestId = fields[0],
            fromDevice = fields.getOrNull(1) ?: "",
            fromName = fields.getOrNull(2)
        )
    }

    private fun parseFileTransferRequest(fields: List<String>): FileTransferRequest? {
        if (fields.size < 3) return null
        return FileTransferRequest(
            requestId = fields[0],
            fileName = fields[1],
            fileSize = fields[2].toLongOrNull() ?: 0L,
            fileCount = fields.getOrNull(3)?.toIntOrNull() ?: 1
        )
    }

    private fun parseScreenShareRequest(fields: List<String>, direction: ScreenShareDirection): ScreenShareRequest? {
        if (fields.isEmpty()) return null
        return ScreenShareRequest(
            requestId = fields[0],
            direction = direction,
            width = fields.getOrNull(1)?.toIntOrNull(),
            height = fields.getOrNull(2)?.toIntOrNull(),
            fps = fields.getOrNull(3)?.toIntOrNull()
        )
    }

    private fun parseWhiteboardRequest(fields: List<String>): WhiteboardRequest? {
        if (fields.size < 2) return null
        return WhiteboardRequest(
            requestId = fields[0],
            fromDevice = fields[1]
        )
    }

    private fun parseRemoteControlRequest(fields: List<String>, direction: RemoteControlDirection): RemoteControlRequest? {
        if (fields.size < 2) return null
        return RemoteControlRequest(
            requestId = fields[0],
            direction = direction,
            fromDevice = fields[1]
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - RESPONSE
    // ════════════════════════════════════════════════════════════════════════

    private fun parseAcceptResponse(fields: List<String>, withMetadata: Boolean): AcceptResponse? {
        if (fields.isEmpty()) return null
        val metadata = if (withMetadata && fields.size > 1) {
            fields.drop(1).chunked(2).associate { pair ->
                pair[0] to (pair.getOrNull(1) ?: "")
            }
        } else null
        return AcceptResponse(
            requestId = fields[0],
            metadata = metadata
        )
    }

    private fun parseDeclineResponse(fields: List<String>, withReason: Boolean): DeclineResponse? {
        if (fields.isEmpty()) return null
        return DeclineResponse(
            requestId = fields[0],
            reason = if (withReason) fields.getOrNull(1) else null
        )
    }

    private fun parseBusyResponse(fields: List<String>, withFeature: Boolean): BusyResponse? {
        if (fields.isEmpty()) return null
        return BusyResponse(
            requestId = fields[0],
            currentFeature = if (withFeature) fields.getOrNull(1) else null
        )
    }

    private fun parseErrorResponse(fields: List<String>): ErrorResponse? {
        if (fields.size < 2) return null
        return ErrorResponse(
            requestId = fields[0],
            errorMessage = fields.drop(1).joinToString(DELIMITER)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - EVENTS
    // ════════════════════════════════════════════════════════════════════════

    private fun parseConnectedEvent(fields: List<String>): ConnectedEvent? {
        if (fields.isEmpty()) return null
        return ConnectedEvent(
            sessionId = fields[0],
            ipAddress = fields.getOrNull(1)
        )
    }

    private fun parseDisconnectedEvent(fields: List<String>): DisconnectedEvent? {
        if (fields.isEmpty()) return null
        return DisconnectedEvent(
            sessionId = fields[0],
            reason = fields.getOrNull(1)
        )
    }

    private fun parseICEReadyEvent(fields: List<String>): ICEReadyEvent? {
        if (fields.isEmpty()) return null
        return ICEReadyEvent(sessionId = fields[0])
    }

    private fun parseDataChannelOpenEvent(fields: List<String>): DataChannelOpenEvent? {
        if (fields.isEmpty()) return null
        return DataChannelOpenEvent(sessionId = fields[0])
    }

    private fun parseDataChannelCloseEvent(fields: List<String>): DataChannelCloseEvent? {
        if (fields.isEmpty()) return null
        return DataChannelCloseEvent(sessionId = fields[0])
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - STATE
    // ════════════════════════════════════════════════════════════════════════

    private fun parseMicrophoneState(fields: List<String>): MicrophoneState? {
        if (fields.size < 2) return null
        return MicrophoneState(
            sessionId = fields[0],
            enabled = fields[1].toIntOrNull() == 1 || fields[1].toBooleanStrictOrNull() == true
        )
    }

    private fun parseCameraState(fields: List<String>): CameraState? {
        if (fields.size < 2) return null
        return CameraState(
            sessionId = fields[0],
            enabled = fields[1].toIntOrNull() == 1 || fields[1].toBooleanStrictOrNull() == true
        )
    }

    private fun parseRecordingState(fields: List<String>): RecordingState? {
        if (fields.size < 2) return null
        return RecordingState(
            sessionId = fields[0],
            state = parseRecordingStateValue(fields[1]),
            fileName = fields.getOrNull(2)
        )
    }

    private fun parseRecordingStateValue(value: String): RecordingStateValue {
        return when (value.lowercase()) {
            "start" -> RecordingStateValue.START
            "stop" -> RecordingStateValue.STOP
            "pause" -> RecordingStateValue.PAUSE
            "resume" -> RecordingStateValue.RESUME
            else -> RecordingStateValue.STOP
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - CONTENT
    // ════════════════════════════════════════════════════════════════════════

    private fun parseChatMessage(fields: List<String>): ChatMessage? {
        if (fields.size < 2) return null
        return ChatMessage(
            messageId = fields[0].takeIf { it.isNotEmpty() },
            text = fields.drop(1).joinToString(DELIMITER)
        )
    }

    private fun parseUIComponentMessage(fields: List<String>): UIComponentMessage? {
        if (fields.size < 2) return null
        return UIComponentMessage(
            requestId = fields[0],
            componentDSL = fields.drop(1).joinToString(DELIMITER)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - VOICE
    // ════════════════════════════════════════════════════════════════════════

    private fun parseVoiceCommandMessage(fields: List<String>): VoiceCommandMessage? {
        if (fields.size < 2) return null
        return VoiceCommandMessage(
            commandId = fields[0],
            command = fields.drop(1).joinToString(DELIMITER)
        )
    }

    private fun parseSpeechToTextMessage(fields: List<String>): SpeechToTextMessage? {
        if (fields.size < 2) return null
        return SpeechToTextMessage(
            sessionId = fields[0],
            text = fields.drop(1).joinToString(DELIMITER)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - BROWSER
    // ════════════════════════════════════════════════════════════════════════

    private fun parseURLShareMessage(fields: List<String>): URLShareMessage? {
        if (fields.size < 2) return null
        return URLShareMessage(
            sessionId = fields[0],
            url = fields[1]
        )
    }

    private fun parseNavigateMessage(fields: List<String>): NavigateMessage? {
        if (fields.size < 2) return null
        return NavigateMessage(
            sessionId = fields[0],
            url = fields[1]
        )
    }

    private fun parseTabEventMessage(fields: List<String>): TabEventMessage? {
        if (fields.size < 2) return null
        return TabEventMessage(
            sessionId = fields[0],
            action = fields[1],
            tabId = fields.getOrNull(2)
        )
    }

    private fun parsePageLoadedMessage(fields: List<String>): PageLoadedMessage? {
        if (fields.size < 2) return null
        return PageLoadedMessage(
            sessionId = fields[0],
            url = fields[1]
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - AI
    // ════════════════════════════════════════════════════════════════════════

    private fun parseAIQueryMessage(fields: List<String>): AIQueryMessage? {
        if (fields.size < 2) return null
        return AIQueryMessage(
            queryId = fields[0],
            query = fields.drop(1).joinToString(DELIMITER)
        )
    }

    private fun parseAIResponseMessage(fields: List<String>): AIResponseMessage? {
        if (fields.size < 2) return null
        return AIResponseMessage(
            queryId = fields[0],
            response = fields.drop(1).joinToString(DELIMITER)
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - SYSTEM
    // ════════════════════════════════════════════════════════════════════════

    private fun parseHandshakeMessage(fields: List<String>): HandshakeMessage? {
        if (fields.size < 3) return null
        return HandshakeMessage(
            protocolVersion = fields[0],
            appVersion = fields[1],
            deviceId = fields[2]
        )
    }

    private fun parsePingMessage(fields: List<String>): PingMessage? {
        if (fields.isEmpty()) return null
        return PingMessage(
            timestamp = fields[0].toLongOrNull() ?: 0L
        )
    }

    private fun parsePongMessage(fields: List<String>): PongMessage? {
        if (fields.isEmpty()) return null
        return PongMessage(
            timestamp = fields[0].toLongOrNull() ?: 0L
        )
    }

    private fun parseCapabilityMessage(fields: List<String>): CapabilityMessage {
        return CapabilityMessage(
            capabilities = fields.firstOrNull()?.split(",") ?: emptyList()
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // MESSAGE PARSERS - SERVER
    // ════════════════════════════════════════════════════════════════════════

    private fun parsePromotionMessage(fields: List<String>): PromotionMessage? {
        if (fields.size < 3) return null
        return PromotionMessage(
            deviceId = fields[0],
            priority = fields[1].toIntOrNull() ?: 0,
            timestamp = fields[2].toLongOrNull() ?: 0L
        )
    }

    private fun parseRoleChangeMessage(fields: List<String>): RoleChangeMessage? {
        if (fields.size < 2) return null
        return RoleChangeMessage(
            deviceId = fields[0],
            role = when (fields[1].lowercase()) {
                "server" -> Role.SERVER
                else -> Role.CLIENT
            }
        )
    }

    // ════════════════════════════════════════════════════════════════════════
    // ESCAPE UTILITIES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Unescape URL-encoded text from AVU format.
     *
     * Delegates to [AvuEscape.unescape] - the canonical implementation.
     */
    private fun unescape(text: String): String = AvuEscape.unescape(text)
}

// ════════════════════════════════════════════════════════════════════════════
// AVU IPC MODEL DEFINITIONS
// These are AVU-specific models that complement the existing IPCModels
// ════════════════════════════════════════════════════════════════════════════

/**
 * Base interface for AVU IPC-specific models.
 * These models are parsed from AVU format and are distinct from UniversalMessage types.
 */
sealed interface AvuIPCModel {
    val prefix: String
}

// ────────────────────────────────────────────────────────────────────────────
// CONNECTION STATE MODELS
// ────────────────────────────────────────────────────────────────────────────

/**
 * AVU format connection state change notification.
 * Format: CST:state
 */
data class AvuConnectionStateChange(
    val state: ConnectionState
) : AvuIPCModel {
    override val prefix = "CST"
}

/**
 * AVU format protocol type information.
 * Format: CPL:protocol
 */
data class AvuProtocolTypeInfo(
    val protocol: IPCProtocol
) : AvuIPCModel {
    override val prefix = "CPL"
}

// ────────────────────────────────────────────────────────────────────────────
// RESILIENCE MODELS (AVU format variants)
// ────────────────────────────────────────────────────────────────────────────

/**
 * AVU format reconnection policy.
 * Format: RCP:enabled:retries:delay:max:mult
 *
 * Can be converted to [ReconnectionPolicy] for use with IPC system.
 */
data class AvuReconnectionPolicy(
    val enabled: Boolean,
    val maxRetries: Int,
    val initialDelayMs: Long,
    val maxDelayMs: Long,
    val multiplier: Float
) : AvuIPCModel {
    override val prefix = "RCP"

    /** Convert to existing [ReconnectionPolicy] model */
    fun toReconnectionPolicy(): ReconnectionPolicy = ReconnectionPolicy(
        enabled = enabled,
        maxRetries = maxRetries,
        initialDelayMs = initialDelayMs,
        maxDelayMs = maxDelayMs,
        backoffMultiplier = multiplier
    )
}

/**
 * AVU format circuit breaker state.
 * Format: CBK:failures:successes:timeout:state
 */
data class AvuCircuitBreakerState(
    val failureThreshold: Int,
    val successThreshold: Int,
    val timeoutMs: Long,
    val state: CircuitState
) : AvuIPCModel {
    override val prefix = "CBK"

    /** Convert to existing [CircuitBreakerConfig] model */
    fun toCircuitBreakerConfig(): CircuitBreakerConfig = CircuitBreakerConfig(
        failureThreshold = failureThreshold,
        successThreshold = successThreshold,
        timeoutMs = timeoutMs
    )
}

/**
 * AVU format rate limit configuration.
 * Format: RLM:rps:burst
 */
data class AvuRateLimitConfig(
    val maxRequestsPerSecond: Int,
    val burstSize: Int
) : AvuIPCModel {
    override val prefix = "RLM"

    /** Convert to existing [RateLimitConfig] model */
    fun toRateLimitConfig(): RateLimitConfig = RateLimitConfig(
        maxRequestsPerSecond = maxRequestsPerSecond,
        burstSize = burstSize
    )
}

/**
 * AVU format resource limits.
 * Format: RSL:conns:msgsize:ctimeout:mtimeout:qtimeout
 */
data class AvuResourceLimits(
    val maxConnections: Int,
    val maxMessageSize: Int,
    val connectionTimeoutMs: Long,
    val methodTimeoutMs: Long,
    val queryTimeoutMs: Long
) : AvuIPCModel {
    override val prefix = "RSL"

    /** Convert to existing [ResourceLimits] model */
    fun toResourceLimits(): ResourceLimits = ResourceLimits(
        maxConnections = maxConnections,
        maxMessageSize = maxMessageSize,
        connectionTimeoutMs = connectionTimeoutMs,
        methodTimeoutMs = methodTimeoutMs,
        queryTimeoutMs = queryTimeoutMs
    )
}

// ────────────────────────────────────────────────────────────────────────────
// METHOD INVOCATION MODELS (AVU format)
// ────────────────────────────────────────────────────────────────────────────

/**
 * AVU format method invocation.
 * Format: MTH:name:timeout:params...
 */
data class AvuMethodInvoke(
    val methodName: String,
    val timeoutMs: Long,
    val parameters: List<String>
) : AvuIPCModel {
    override val prefix = "MTH"

    /** Convert to existing [MethodInvocation] model */
    fun toMethodInvocation(): MethodInvocation = MethodInvocation(
        methodName = methodName,
        parameters = parameters.withIndex().associate { "param${it.index}" to it.value },
        timeoutMs = timeoutMs
    )
}

/**
 * AVU format method success result.
 * Format: MRS:value
 */
data class AvuMethodResultSuccess(
    val value: String
) : AvuIPCModel {
    override val prefix = "MRS"
}

/**
 * AVU format method error result.
 * Format: MRE:code:message
 */
data class AvuMethodResultError(
    val errorCode: String,
    val errorMessage: String
) : AvuIPCModel {
    override val prefix = "MRE"
}

// ────────────────────────────────────────────────────────────────────────────
// QUERY OPERATION MODELS (AVU format)
// ────────────────────────────────────────────────────────────────────────────

/**
 * AVU format query parameters.
 * Format: QRY:uri:proj:sel:args:sort:limit
 */
data class AvuQueryParams(
    val uri: String,
    val projection: List<String>?,
    val selection: String?,
    val selectionArgs: List<String>?,
    val sortOrder: String?,
    val limit: Int?
) : AvuIPCModel {
    override val prefix = "QRY"

    /** Convert to existing [QueryParams] model */
    fun toQueryParams(): QueryParams = QueryParams(
        uri = uri,
        projection = projection,
        selection = selection,
        selectionArgs = selectionArgs,
        sortOrder = sortOrder,
        limit = limit
    )
}

/**
 * AVU format query result with row data as strings.
 * Format: QRS:count:rows...
 */
data class AvuQueryResult(
    val rowCount: Int,
    val rows: List<String>
) : AvuIPCModel {
    override val prefix = "QRS"
}

// ────────────────────────────────────────────────────────────────────────────
// ERROR MODEL (AVU format)
// ────────────────────────────────────────────────────────────────────────────

/**
 * AVU format error information.
 * Format: ERRI:code:message:details
 */
data class AvuIPCErrorInfo(
    val errorCode: String,
    val errorMessage: String,
    val details: String?
) : AvuIPCModel {
    override val prefix = "ERRI"
}

// ────────────────────────────────────────────────────────────────────────────
// METRICS MODEL (AVU format)
// ────────────────────────────────────────────────────────────────────────────

/**
 * AVU format IPC metrics for monitoring.
 * Format: MET:active:total:failed:sent:recv:avg:p95:p99:rate:up
 */
data class AvuIPCMetrics(
    val activeConnections: Int,
    val totalRequests: Long,
    val failedRequests: Long,
    val bytesSent: Long,
    val bytesReceived: Long,
    val avgLatencyMs: Double,
    val p95LatencyMs: Long,
    val p99LatencyMs: Long,
    val requestsPerSecond: Double,
    val uptimeMs: Long
) : AvuIPCModel {
    override val prefix = "MET"

    /** Convert to existing [IPCMetrics] model (partial mapping) */
    fun toIPCMetrics(): IPCMetrics = IPCMetrics(
        connectionsActive = activeConnections,
        connectionsTotal = totalRequests,
        connectionsFailed = failedRequests,
        messagesSent = bytesSent,
        messagesReceived = bytesReceived,
        averageLatencyMs = avgLatencyMs,
        p95LatencyMs = p95LatencyMs,
        p99LatencyMs = p99LatencyMs,
        uptime = uptimeMs
    )
}
