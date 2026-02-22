package com.augmentalis.httpavanue.websocket

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * Type-safe WebSocket wrapper — automatically serializes/deserializes
 * messages using kotlinx.serialization.
 *
 * Wraps an existing [WebSocket] and provides a typed [Flow] for
 * incoming messages and a typed [send] for outgoing messages.
 */
class TypedWebSocket<Req, Resp>(
    private val socket: WebSocket,
    private val reqSerializer: KSerializer<Req>,
    private val respSerializer: KSerializer<Resp>,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /**
     * Incoming messages deserialized to [Req] type.
     * Binary and Close messages are filtered out.
     * Malformed JSON messages are silently dropped.
     */
    val messages: Flow<Req> = socket.messages.mapNotNull { message ->
        when (message) {
            is WebSocketMessage.Text -> {
                try {
                    json.decodeFromString(reqSerializer, message.data)
                } catch (_: Exception) {
                    null
                }
            }
            else -> null
        }
    }

    /** Send a typed response — serialized to JSON text frame. */
    suspend fun send(response: Resp) {
        val text = json.encodeToString(respSerializer, response)
        socket.sendText(text)
    }

    /** Send raw binary data (pass-through, no serialization). */
    suspend fun sendBinary(data: ByteArray) {
        socket.sendBinary(data)
    }

    /** Close the underlying WebSocket connection. */
    suspend fun close(code: WebSocketCloseCode = WebSocketCloseCode.NORMAL, reason: String = "") {
        socket.close(code, reason)
    }
}

/**
 * Create a typed WebSocket from an existing WebSocket connection.
 * Uses reified type parameters for automatic serializer resolution.
 */
inline fun <reified Req, reified Resp> WebSocket.typed(
    json: Json = Json { ignoreUnknownKeys = true },
): TypedWebSocket<Req, Resp> {
    return TypedWebSocket(
        socket = this,
        reqSerializer = serializer<Req>(),
        respSerializer = serializer<Resp>(),
        json = json,
    )
}
