package com.augmentalis.avamagic.ipc

import com.augmentalis.avucodec.core.AvuEscape

/**
 * AVU RPC Format Serializer
 *
 * Converts IPC model objects to AVU (Avanues Universal) format strings.
 * Inverse operation of AvuIPCParser.
 *
 * AVU Format: PREFIX:field1:field2:field3:...
 *
 * Features:
 * - Serializes all [UniversalMessage] types from IPCMessages
 * - Serializes all [AvuIPCModel] types
 * - Serializes core IPCModels types (Connection, MethodInvocation, etc.)
 * - Serializes IPCError types
 * - Automatic escaping of special characters (: % newlines)
 * - Batch serialization support
 * - Human-readable output for debugging
 *
 * Supported Types:
 * - Connection: Connection (CON), ConnectionState (CST), IPCProtocol (CPL)
 * - Resilience: ReconnectionPolicy (RCP), CircuitBreakerConfig (CBK), RateLimitConfig (RLM), ResourceLimits (RSL)
 * - Method: MethodInvocation (MTH), MethodResult.Success (MRS), MethodResult.Error (MRE)
 * - Query: QueryParams (QRY), QueryResult (QRS)
 * - Error: IPCError (ERRI)
 * - Metrics: IPCMetrics (MET)
 * - Messages: All UniversalMessage subclasses (VCA, FTR, ACC, CON, CHT, etc.)
 *
 * @author Augmentalis Engineering
 * @since 1.0.0
 */
object AvuIPCSerializer {

    private const val DELIMITER = ":"

    // ════════════════════════════════════════════════════════════════════════
    // GENERIC SERIALIZATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Serialize any supported model to AVU format string.
     *
     * Supports:
     * - [UniversalMessage] types (from IPCMessages)
     * - [AvuIPCModel] types (from AvuIPCParser)
     * - Core IPCModels types (Connection, ReconnectionPolicy, etc.)
     * - [IPCError] types
     * - [IPCMetrics]
     *
     * @param model The model to serialize
     * @return AVU format string or null if unsupported type
     */
    fun serialize(model: Any): String? {
        return when (model) {
            // UniversalMessage types already have serialize()
            is UniversalMessage -> serializeMessage(model)

            // AVU IPC Model types
            is AvuIPCModel -> serializeIPCModel(model)

            // Core IPCModels.kt types
            is Connection -> serializeConnection(model)
            is ReconnectionPolicy -> serializeFromReconnectionPolicy(model)
            is CircuitBreakerConfig -> serializeFromCircuitBreakerConfig(model)
            is RateLimitConfig -> serializeFromRateLimitConfig(model)
            is ResourceLimits -> serializeFromResourceLimits(model)
            is MethodInvocation -> serializeMethodInvocation(model)
            is QueryParams -> serializeFromQueryParams(model)
            is QueryResult -> serializeFromQueryResult(model)
            is IPCMetrics -> serializeFromIPCMetrics(model)

            // IPCError types
            is IPCError -> serializeError(model)

            // Enum types
            is ConnectionState -> serializeFromConnectionState(model)
            is IPCProtocol -> serializeFromIPCProtocol(model)

            else -> null
        }
    }

    /**
     * Serialize a [UniversalMessage] to AVU format.
     * Uses the built-in serialize() method from IPCMessages.
     */
    fun serializeMessage(message: UniversalMessage): String {
        return message.serialize()
    }

    /**
     * Serialize an [AvuIPCModel] to AVU format.
     */
    fun serializeIPCModel(model: AvuIPCModel): String {
        return when (model) {
            // Connection Management
            is AvuConnectionStateChange -> serializeConnectionStateChange(model)
            is AvuProtocolTypeInfo -> serializeProtocolTypeInfo(model)

            // Resilience
            is AvuReconnectionPolicy -> serializeReconnectionPolicy(model)
            is AvuCircuitBreakerState -> serializeCircuitBreakerState(model)
            is AvuRateLimitConfig -> serializeRateLimitConfig(model)
            is AvuResourceLimits -> serializeResourceLimits(model)

            // Method Invocation
            is AvuMethodInvoke -> serializeMethodInvoke(model)
            is AvuMethodResultSuccess -> serializeMethodResultSuccess(model)
            is AvuMethodResultError -> serializeMethodResultError(model)

            // Query Operations
            is AvuQueryParams -> serializeQueryParams(model)
            is AvuQueryResult -> serializeQueryResult(model)

            // Errors
            is AvuIPCErrorInfo -> serializeIPCErrorInfo(model)

            // Metrics
            is AvuIPCMetrics -> serializeIPCMetrics(model)
        }
    }

    /**
     * Serialize multiple models to AVU format, one per line.
     */
    fun serializeAll(models: List<Any>): String {
        return models.mapNotNull { serialize(it) }.joinToString("\n")
    }

    /**
     * Serialize multiple messages to AVU format, one per line.
     */
    fun serializeAllMessages(messages: List<UniversalMessage>): String {
        return messages.joinToString("\n") { it.serialize() }
    }

    /**
     * Serialize multiple IPC models to AVU format, one per line.
     */
    fun serializeAllIPCModels(models: List<AvuIPCModel>): String {
        return models.joinToString("\n") { serializeIPCModel(it) }
    }

    /**
     * Serialize with AVU file header for file output.
     */
    fun serializeWithHeader(
        models: List<Any>,
        schema: String = "avu-ipc-1.0",
        version: String = "1.0.0"
    ): String {
        return buildString {
            appendLine("---")
            appendLine("schema: $schema")
            appendLine("version: $version")
            appendLine("---")
            models.forEach { model ->
                serialize(model)?.let { appendLine(it) }
            }
            appendLine("---")
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONNECTION SERIALIZATION
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Serialize a [Connection] to AVU format.
     * Format: CON:id:package:service:state:protocol:timestamp
     *
     * @param conn The connection to serialize
     * @return AVU format string
     */
    fun serializeConnection(conn: Connection): String {
        return buildString {
            append("CON$DELIMITER${conn.id}")
            append("$DELIMITER${escape(conn.packageName)}")
            append("$DELIMITER${escape(conn.serviceId)}")
            append("$DELIMITER${conn.state.name}")
            append("$DELIMITER${conn.protocol.name}")
            append("$DELIMITER${conn.connectedAt}")
        }
    }

    // CST:state
    private fun serializeConnectionStateChange(model: AvuConnectionStateChange): String {
        return "${model.prefix}$DELIMITER${model.state.name}"
    }

    // CPL:protocol
    private fun serializeProtocolTypeInfo(model: AvuProtocolTypeInfo): String {
        return "${model.prefix}$DELIMITER${model.protocol.name}"
    }

    // ════════════════════════════════════════════════════════════════════════
    // RESILIENCE SERIALIZERS
    // ════════════════════════════════════════════════════════════════════════

    // RCP:enabled:retries:delay:max:mult
    private fun serializeReconnectionPolicy(model: AvuReconnectionPolicy): String {
        return buildString {
            append(model.prefix)
            append(DELIMITER)
            append(model.enabled)
            append(DELIMITER)
            append(model.maxRetries)
            append(DELIMITER)
            append(model.initialDelayMs)
            append(DELIMITER)
            append(model.maxDelayMs)
            append(DELIMITER)
            append(model.multiplier)
        }
    }

    // CBK:failures:successes:timeout:state
    private fun serializeCircuitBreakerState(model: AvuCircuitBreakerState): String {
        return buildString {
            append(model.prefix)
            append(DELIMITER)
            append(model.failureThreshold)
            append(DELIMITER)
            append(model.successThreshold)
            append(DELIMITER)
            append(model.timeoutMs)
            append(DELIMITER)
            append(model.state.name)
        }
    }

    // RLM:rps:burst
    private fun serializeRateLimitConfig(model: AvuRateLimitConfig): String {
        return "${model.prefix}$DELIMITER${model.maxRequestsPerSecond}$DELIMITER${model.burstSize}"
    }

    // RSL:conns:msgsize:ctimeout:mtimeout:qtimeout
    private fun serializeResourceLimits(model: AvuResourceLimits): String {
        return buildString {
            append(model.prefix)
            append(DELIMITER)
            append(model.maxConnections)
            append(DELIMITER)
            append(model.maxMessageSize)
            append(DELIMITER)
            append(model.connectionTimeoutMs)
            append(DELIMITER)
            append(model.methodTimeoutMs)
            append(DELIMITER)
            append(model.queryTimeoutMs)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // METHOD INVOCATION SERIALIZERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Serialize a [MethodInvocation] to AVU format.
     * Format: MTH:methodName:timeout:param1:param2:...
     *
     * @param inv The method invocation to serialize
     * @return AVU format string
     */
    fun serializeMethodInvocation(inv: MethodInvocation): String {
        return buildString {
            append("MTH$DELIMITER${escape(inv.methodName)}")
            append("$DELIMITER${inv.timeoutMs}")
            inv.parameters.values.forEach { value ->
                append("$DELIMITER${escape(value)}")
            }
        }
    }

    // MTH:name:timeout:params...
    private fun serializeMethodInvoke(model: AvuMethodInvoke): String {
        return buildString {
            append(model.prefix)
            append(DELIMITER)
            append(escape(model.methodName))
            append(DELIMITER)
            append(model.timeoutMs)
            model.parameters.forEach { param ->
                append(DELIMITER)
                append(escape(param))
            }
        }
    }

    // MRS:value
    private fun serializeMethodResultSuccess(model: AvuMethodResultSuccess): String {
        return "${model.prefix}$DELIMITER${escape(model.value)}"
    }

    // MRE:code:message
    private fun serializeMethodResultError(model: AvuMethodResultError): String {
        return "${model.prefix}$DELIMITER${escape(model.errorCode)}$DELIMITER${escape(model.errorMessage)}"
    }

    // ════════════════════════════════════════════════════════════════════════
    // QUERY OPERATION SERIALIZERS
    // ════════════════════════════════════════════════════════════════════════

    // QRY:uri:proj:sel:args:sort:limit
    private fun serializeQueryParams(model: AvuQueryParams): String {
        return buildString {
            append(model.prefix)
            append(DELIMITER)
            append(escape(model.uri))
            append(DELIMITER)
            append(model.projection?.joinToString(",") ?: "")
            append(DELIMITER)
            append(escape(model.selection ?: ""))
            append(DELIMITER)
            append(model.selectionArgs?.joinToString(",") ?: "")
            append(DELIMITER)
            append(escape(model.sortOrder ?: ""))
            append(DELIMITER)
            append(model.limit?.toString() ?: "")
        }
    }

    // QRS:count:rows...
    private fun serializeQueryResult(model: AvuQueryResult): String {
        return buildString {
            append(model.prefix)
            append(DELIMITER)
            append(model.rowCount)
            model.rows.forEach { row ->
                append(DELIMITER)
                append(escape(row))
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // ERROR SERIALIZERS
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Serialize an [IPCError] to AVU format.
     * Format: ERRI:errorCode:message:details
     *
     * Error codes:
     * - E001: ServiceUnavailable
     * - E002: Timeout
     * - E003: NetworkFailure
     * - E004: SendFailed
     * - E101: PermissionDenied
     * - E102: ServiceNotFound
     * - E103: InvalidResponse
     * - E104: ParseError
     * - E105: NotRegistered
     * - E201: AuthenticationFailed
     * - E202: SignatureVerificationFailed
     * - E301: ResourceExhausted
     * - E302: RateLimitExceeded
     *
     * @param error The IPC error to serialize
     * @return AVU format string
     */
    fun serializeError(error: IPCError): String {
        val code = getErrorCode(error)
        val message = error.message ?: "Unknown error"
        val details = getErrorDetails(error)

        return buildString {
            append("ERRI$DELIMITER$code")
            append("$DELIMITER${escape(message)}")
            details?.let { append("$DELIMITER${escape(it)}") }
        }
    }

    /**
     * Get a standardized error code for an IPCError.
     */
    private fun getErrorCode(error: IPCError): String {
        return when (error) {
            is IPCError.ServiceUnavailable -> "E001"
            is IPCError.Timeout -> "E002"
            is IPCError.NetworkFailure -> "E003"
            is IPCError.SendFailed -> "E004"
            is IPCError.PermissionDenied -> "E101"
            is IPCError.ServiceNotFound -> "E102"
            is IPCError.InvalidResponse -> "E103"
            is IPCError.ParseError -> "E104"
            is IPCError.NotRegistered -> "E105"
            is IPCError.AuthenticationFailed -> "E201"
            is IPCError.SignatureVerificationFailed -> "E202"
            is IPCError.ResourceExhausted -> "E301"
            is IPCError.RateLimitExceeded -> "E302"
        }
    }

    /**
     * Get additional details for an IPCError.
     */
    private fun getErrorDetails(error: IPCError): String? {
        return when (error) {
            is IPCError.ServiceUnavailable -> error.reason
            is IPCError.Timeout -> "${error.durationMs}ms"
            is IPCError.NetworkFailure -> error.cause?.message
            is IPCError.SendFailed -> error.reason
            is IPCError.PermissionDenied -> error.permission
            is IPCError.ServiceNotFound -> error.target
            is IPCError.InvalidResponse -> error.details
            is IPCError.ParseError -> error.parseMessage
            is IPCError.NotRegistered -> error.appId
            is IPCError.AuthenticationFailed -> error.reason
            is IPCError.SignatureVerificationFailed -> error.packageName
            is IPCError.ResourceExhausted -> error.resource
            is IPCError.RateLimitExceeded -> "${error.retryAfterMs}ms"
        }
    }

    // ERRI:code:message:details
    private fun serializeIPCErrorInfo(model: AvuIPCErrorInfo): String {
        return buildString {
            append(model.prefix)
            append(DELIMITER)
            append(escape(model.errorCode))
            append(DELIMITER)
            append(escape(model.errorMessage))
            model.details?.let {
                append(DELIMITER)
                append(escape(it))
            }
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // METRICS SERIALIZER
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Serialize [IPCMetrics] to AVU format.
     * Format: MET:active:total:failed:sent:recv:avgLatency:p95:p99:rate:uptime
     *
     * @param metrics The IPC metrics to serialize
     * @return AVU format string
     */
    fun serializeMetrics(metrics: IPCMetrics): String {
        return buildString {
            append("MET$DELIMITER${metrics.connectionsActive}")
            append("$DELIMITER${metrics.connectionsTotal}")
            append("$DELIMITER${metrics.connectionsFailed}")
            append("$DELIMITER${metrics.messagesSent}")
            append("$DELIMITER${metrics.messagesReceived}")
            append("$DELIMITER${metrics.averageLatencyMs}")
            append("$DELIMITER${metrics.p95LatencyMs}")
            append("$DELIMITER${metrics.p99LatencyMs}")
            append("$DELIMITER${metrics.errorRate}")
            append("$DELIMITER${metrics.uptime}")
        }
    }

    // MET:active:total:failed:sent:recv:avg:p95:p99:rate:up
    private fun serializeIPCMetrics(model: AvuIPCMetrics): String {
        return buildString {
            append(model.prefix)
            append(DELIMITER)
            append(model.activeConnections)
            append(DELIMITER)
            append(model.totalRequests)
            append(DELIMITER)
            append(model.failedRequests)
            append(DELIMITER)
            append(model.bytesSent)
            append(DELIMITER)
            append(model.bytesReceived)
            append(DELIMITER)
            append(model.avgLatencyMs)
            append(DELIMITER)
            append(model.p95LatencyMs)
            append(DELIMITER)
            append(model.p99LatencyMs)
            append(DELIMITER)
            append(model.requestsPerSecond)
            append(DELIMITER)
            append(model.uptimeMs)
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    // CONVERSION HELPERS
    // These helpers convert from existing IPCModels to Avu format models
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Convert existing [ReconnectionPolicy] to AVU format and serialize.
     */
    fun serializeFromReconnectionPolicy(policy: ReconnectionPolicy): String {
        return serializeReconnectionPolicy(
            AvuReconnectionPolicy(
                enabled = policy.enabled,
                maxRetries = policy.maxRetries,
                initialDelayMs = policy.initialDelayMs,
                maxDelayMs = policy.maxDelayMs,
                multiplier = policy.backoffMultiplier
            )
        )
    }

    /**
     * Convert existing [CircuitBreakerConfig] to AVU format and serialize.
     * Note: Requires a [CircuitState] since config doesn't include current state.
     */
    fun serializeFromCircuitBreakerConfig(
        config: CircuitBreakerConfig,
        state: CircuitState = CircuitState.CLOSED
    ): String {
        return serializeCircuitBreakerState(
            AvuCircuitBreakerState(
                failureThreshold = config.failureThreshold,
                successThreshold = config.successThreshold,
                timeoutMs = config.timeoutMs,
                state = state
            )
        )
    }

    /**
     * Convert existing [RateLimitConfig] to AVU format and serialize.
     */
    fun serializeFromRateLimitConfig(config: RateLimitConfig): String {
        return serializeRateLimitConfig(
            AvuRateLimitConfig(
                maxRequestsPerSecond = config.maxRequestsPerSecond,
                burstSize = config.burstSize
            )
        )
    }

    /**
     * Convert existing [ResourceLimits] to AVU format and serialize.
     */
    fun serializeFromResourceLimits(limits: ResourceLimits): String {
        return serializeResourceLimits(
            AvuResourceLimits(
                maxConnections = limits.maxConnections,
                maxMessageSize = limits.maxMessageSize,
                connectionTimeoutMs = limits.connectionTimeoutMs,
                methodTimeoutMs = limits.methodTimeoutMs,
                queryTimeoutMs = limits.queryTimeoutMs
            )
        )
    }

    /**
     * Convert existing [MethodInvocation] to AVU format and serialize.
     */
    fun serializeFromMethodInvocation(invocation: MethodInvocation): String {
        return serializeMethodInvoke(
            AvuMethodInvoke(
                methodName = invocation.methodName,
                timeoutMs = invocation.timeoutMs,
                parameters = invocation.parameters.values.toList()
            )
        )
    }

    /**
     * Convert existing [QueryParams] to AVU format and serialize.
     */
    fun serializeFromQueryParams(params: QueryParams): String {
        return serializeQueryParams(
            AvuQueryParams(
                uri = params.uri,
                projection = params.projection,
                selection = params.selection,
                selectionArgs = params.selectionArgs,
                sortOrder = params.sortOrder,
                limit = params.limit
            )
        )
    }

    /**
     * Convert existing [QueryResult] to AVU format and serialize.
     */
    fun serializeFromQueryResult(result: QueryResult): String {
        // Convert rows from Map<String, String> to serialized strings
        val rows = result.rows.map { row ->
            row.entries.joinToString(",") { "${it.key}=${it.value}" }
        }
        return serializeQueryResult(
            AvuQueryResult(
                rowCount = result.count,
                rows = rows
            )
        )
    }

    /**
     * Convert existing [IPCMetrics] to AVU format and serialize.
     * Note: Some fields are mapped with best-effort approximation.
     */
    fun serializeFromIPCMetrics(metrics: IPCMetrics): String {
        return serializeIPCMetrics(
            AvuIPCMetrics(
                activeConnections = metrics.connectionsActive,
                totalRequests = metrics.connectionsTotal,
                failedRequests = metrics.connectionsFailed,
                bytesSent = metrics.messagesSent,
                bytesReceived = metrics.messagesReceived,
                avgLatencyMs = metrics.averageLatencyMs,
                p95LatencyMs = metrics.p95LatencyMs,
                p99LatencyMs = metrics.p99LatencyMs,
                requestsPerSecond = 0.0, // Not available in IPCMetrics
                uptimeMs = metrics.uptime
            )
        )
    }

    /**
     * Convert existing [ConnectionState] to AVU format and serialize.
     */
    fun serializeFromConnectionState(state: ConnectionState): String {
        return serializeConnectionStateChange(AvuConnectionStateChange(state))
    }

    /**
     * Convert existing [IPCProtocol] to AVU format and serialize.
     */
    fun serializeFromIPCProtocol(protocol: IPCProtocol): String {
        return serializeProtocolTypeInfo(AvuProtocolTypeInfo(protocol))
    }

    // ════════════════════════════════════════════════════════════════════════
    // ESCAPE UTILITIES
    // ════════════════════════════════════════════════════════════════════════

    /**
     * Escape special characters for AVU format.
     * Escapes: % : \n \r
     *
     * Delegates to [AvuEscape.escape] - the canonical implementation.
     *
     * @param text The text to escape
     * @return Escaped text safe for AVU format
     */
    fun escape(text: String): String = AvuEscape.escape(text)

    /**
     * Unescape AVU format text.
     *
     * Delegates to [AvuEscape.unescape] - the canonical implementation.
     *
     * @param text The escaped text
     * @return Original text with escape sequences decoded
     */
    fun unescape(text: String): String = AvuEscape.unescape(text)
}
