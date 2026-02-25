package com.augmentalis.avanueui.ipc.dsl
import com.augmentalis.avanueui.ipc.currentTimeMillis

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * UI IPC Protocol for transferring UI components between apps
 *
 * Defines the protocol for:
 * - Rendering UI in another app
 * - Updating component properties
 * - Handling component events
 * - Synchronizing state
 *
 * Protocol Actions:
 * - ui.render: Render a full component tree
 * - ui.update: Update specific component properties
 * - ui.event: Send component event (onClick, onChange, etc.)
 * - ui.state: Sync component state
 * - ui.dispose: Dispose rendered components
 * - ui.query: Query component state
 *
 * Usage:
 * ```kotlin
 * val protocol = UIIPCProtocol()
 *
 * // Create render request
 * val request = protocol.createRenderRequest(
 *     component = myComponent,
 *     targetAppId = "com.avanue.renderer"
 * )
 *
 * // Send via IPC
 * ipcManager.send(request.toAppMessage())
 *
 * // Handle response
 * protocol.handleResponse(response) { result ->
 *     when (result) {
 *         is UIIPCResult.Success -> // Component rendered
 *         is UIIPCResult.Error -> // Handle error
 *     }
 * }
 * ```
 *
 * Created by Manoj Jhawar, manoj@ideahq.net
 * Date: 2025-11-19
 */
class UIIPCProtocol {

    private val serializer = DSLSerializer()
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    /**
     * Create render request
     */
    fun createRenderRequest(
        component: UIComponent,
        targetAppId: String,
        sourceAppId: String,
        options: RenderOptions = RenderOptions()
    ): UIIPCRequest {
        return UIIPCRequest(
            id = generateRequestId(),
            action = "ui.render",
            sourceAppId = sourceAppId,
            targetAppId = targetAppId,
            payload = RenderPayload(
                dsl = serializer.serialize(component),
                options = options
            ),
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create update request for property changes
     */
    fun createUpdateRequest(
        componentId: String,
        updates: Map<String, Any>,
        targetAppId: String,
        sourceAppId: String
    ): UIIPCRequest {
        return UIIPCRequest(
            id = generateRequestId(),
            action = "ui.update",
            sourceAppId = sourceAppId,
            targetAppId = targetAppId,
            payload = UpdatePayload(
                componentId = componentId,
                properties = updates.mapValues { serializePropertyValue(it.value) }
            ),
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create event message
     */
    fun createEventMessage(
        componentId: String,
        eventType: String,
        eventData: Map<String, Any>,
        targetAppId: String,
        sourceAppId: String
    ): UIIPCRequest {
        return UIIPCRequest(
            id = generateRequestId(),
            action = "ui.event",
            sourceAppId = sourceAppId,
            targetAppId = targetAppId,
            payload = EventPayload(
                componentId = componentId,
                eventType = eventType,
                data = eventData.mapValues { serializePropertyValue(it.value) }
            ),
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create state sync request
     */
    fun createStateSyncRequest(
        componentId: String,
        state: Map<String, Any>,
        targetAppId: String,
        sourceAppId: String
    ): UIIPCRequest {
        return UIIPCRequest(
            id = generateRequestId(),
            action = "ui.state",
            sourceAppId = sourceAppId,
            targetAppId = targetAppId,
            payload = StatePayload(
                componentId = componentId,
                state = state.mapValues { serializePropertyValue(it.value) }
            ),
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create dispose request
     */
    fun createDisposeRequest(
        componentIds: List<String>,
        targetAppId: String,
        sourceAppId: String
    ): UIIPCRequest {
        return UIIPCRequest(
            id = generateRequestId(),
            action = "ui.dispose",
            sourceAppId = sourceAppId,
            targetAppId = targetAppId,
            payload = DisposePayload(componentIds = componentIds),
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create query request
     */
    fun createQueryRequest(
        componentId: String,
        queryType: QueryType,
        targetAppId: String,
        sourceAppId: String
    ): UIIPCRequest {
        return UIIPCRequest(
            id = generateRequestId(),
            action = "ui.query",
            sourceAppId = sourceAppId,
            targetAppId = targetAppId,
            payload = QueryPayload(
                componentId = componentId,
                queryType = queryType.name
            ),
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create success response
     */
    fun createSuccessResponse(
        requestId: String,
        data: Map<String, Any>? = null
    ): UIIPCResponse {
        return UIIPCResponse(
            requestId = requestId,
            success = true,
            data = data?.mapValues { serializePropertyValue(it.value) },
            error = null,
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Create error response
     */
    fun createErrorResponse(
        requestId: String,
        errorCode: String,
        errorMessage: String
    ): UIIPCResponse {
        return UIIPCResponse(
            requestId = requestId,
            success = false,
            data = null,
            error = ErrorInfo(code = errorCode, message = errorMessage),
            timestamp = currentTimeMillis()
        )
    }

    /**
     * Parse incoming request
     */
    fun parseRequest(json: String): UIIPCRequest? {
        return try {
            this.json.decodeFromString<UIIPCRequest>(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Parse incoming response
     */
    fun parseResponse(json: String): UIIPCResponse? {
        return try {
            this.json.decodeFromString<UIIPCResponse>(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Handle request and generate response
     */
    fun handleRequest(
        request: UIIPCRequest,
        handler: RequestHandler
    ): UIIPCResponse {
        return try {
            when (request.action) {
                "ui.render" -> {
                    val payload = request.payload as? RenderPayload
                        ?: return createErrorResponse(request.id, "INVALID_PAYLOAD", "Invalid render payload")

                    val component = serializer.deserialize(payload.dsl)
                        ?: return createErrorResponse(request.id, "PARSE_ERROR", "Failed to parse DSL")

                    handler.onRender(component, payload.options)
                    createSuccessResponse(request.id, mapOf("rendered" to true))
                }

                "ui.update" -> {
                    val payload = request.payload as? UpdatePayload
                        ?: return createErrorResponse(request.id, "INVALID_PAYLOAD", "Invalid update payload")

                    handler.onUpdate(payload.componentId, payload.properties)
                    createSuccessResponse(request.id)
                }

                "ui.event" -> {
                    val payload = request.payload as? EventPayload
                        ?: return createErrorResponse(request.id, "INVALID_PAYLOAD", "Invalid event payload")

                    handler.onEvent(payload.componentId, payload.eventType, payload.data)
                    createSuccessResponse(request.id)
                }

                "ui.state" -> {
                    val payload = request.payload as? StatePayload
                        ?: return createErrorResponse(request.id, "INVALID_PAYLOAD", "Invalid state payload")

                    handler.onStateSync(payload.componentId, payload.state)
                    createSuccessResponse(request.id)
                }

                "ui.dispose" -> {
                    val payload = request.payload as? DisposePayload
                        ?: return createErrorResponse(request.id, "INVALID_PAYLOAD", "Invalid dispose payload")

                    handler.onDispose(payload.componentIds)
                    createSuccessResponse(request.id)
                }

                "ui.query" -> {
                    val payload = request.payload as? QueryPayload
                        ?: return createErrorResponse(request.id, "INVALID_PAYLOAD", "Invalid query payload")

                    val result = handler.onQuery(payload.componentId, QueryType.valueOf(payload.queryType))
                    createSuccessResponse(request.id, result)
                }

                else -> createErrorResponse(request.id, "UNKNOWN_ACTION", "Unknown action: ${request.action}")
            }
        } catch (e: Exception) {
            createErrorResponse(request.id, "HANDLER_ERROR", e.message ?: "Unknown error")
        }
    }

    private fun generateRequestId(): String {
        return "req_${currentTimeMillis()}_${(kotlin.random.Random.nextDouble() * 10000).toInt()}"
    }

    private fun serializePropertyValue(value: Any): String {
        return when (value) {
            is String -> value
            is Number -> value.toString()
            is Boolean -> value.toString()
            else -> value.toString()
        }
    }
}

/**
 * IPC Request
 */
@Serializable
data class UIIPCRequest(
    val id: String,
    val action: String,
    val sourceAppId: String,
    val targetAppId: String,
    val payload: Payload,
    val timestamp: Long
) {
    fun toJson(): String = Json.encodeToString(this)
}

/**
 * IPC Response
 */
@Serializable
data class UIIPCResponse(
    val requestId: String,
    val success: Boolean,
    val data: Map<String, String>? = null,
    val error: ErrorInfo? = null,
    val timestamp: Long
) {
    fun toJson(): String = Json.encodeToString(this)
}

/**
 * Error info
 */
@Serializable
data class ErrorInfo(
    val code: String,
    val message: String
)

/**
 * Base payload interface
 */
@Serializable
sealed interface Payload

/**
 * Render payload
 */
@Serializable
data class RenderPayload(
    val dsl: String,
    val options: RenderOptions = RenderOptions()
) : Payload

/**
 * Update payload
 */
@Serializable
data class UpdatePayload(
    val componentId: String,
    val properties: Map<String, String>
) : Payload

/**
 * Event payload
 */
@Serializable
data class EventPayload(
    val componentId: String,
    val eventType: String,
    val data: Map<String, String>
) : Payload

/**
 * State payload
 */
@Serializable
data class StatePayload(
    val componentId: String,
    val state: Map<String, String>
) : Payload

/**
 * Dispose payload
 */
@Serializable
data class DisposePayload(
    val componentIds: List<String>
) : Payload

/**
 * Query payload
 */
@Serializable
data class QueryPayload(
    val componentId: String,
    val queryType: String
) : Payload

/**
 * Render options
 */
@Serializable
data class RenderOptions(
    val animate: Boolean = true,
    val cacheEnabled: Boolean = true,
    val theme: String? = null,
    val rootId: String? = null
)

/**
 * Query types
 */
enum class QueryType {
    STATE,
    BOUNDS,
    VISIBILITY,
    ACCESSIBILITY,
    CHILDREN
}

/**
 * Request handler interface
 */
interface RequestHandler {
    fun onRender(component: UIComponent, options: RenderOptions)
    fun onUpdate(componentId: String, properties: Map<String, String>)
    fun onEvent(componentId: String, eventType: String, data: Map<String, String>)
    fun onStateSync(componentId: String, state: Map<String, String>)
    fun onDispose(componentIds: List<String>)
    fun onQuery(componentId: String, queryType: QueryType): Map<String, Any>
}
