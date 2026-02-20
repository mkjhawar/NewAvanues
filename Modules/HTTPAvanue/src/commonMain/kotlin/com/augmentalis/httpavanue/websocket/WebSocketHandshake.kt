package com.augmentalis.httpavanue.websocket

import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Server-side WebSocket handshake utilities (RFC 6455)
 */
object WebSocketHandshake {
    private const val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

    fun isWebSocketRequest(request: HttpRequest): Boolean {
        return request.header("Upgrade")?.lowercase() == "websocket" &&
            request.header("Connection")?.lowercase()?.contains("upgrade") == true &&
            request.header("Sec-WebSocket-Version") == "13" &&
            request.header("Sec-WebSocket-Key") != null
    }

    fun createHandshakeResponse(request: HttpRequest): HttpResponse {
        val key = request.header("Sec-WebSocket-Key")
            ?: throw IllegalArgumentException("Missing Sec-WebSocket-Key")
        val acceptKey = generateAcceptKey(key)
        val headers = mutableMapOf(
            "Upgrade" to "websocket", "Connection" to "Upgrade",
            "Sec-WebSocket-Accept" to acceptKey,
        )
        request.header("Sec-WebSocket-Protocol")?.let { protocols ->
            protocols.split(",").firstOrNull()?.trim()?.let { headers["Sec-WebSocket-Protocol"] = it }
        }
        return HttpResponse(status = 101, statusMessage = "Switching Protocols", headers = headers)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun generateAcceptKey(clientKey: String): String {
        val concatenated = clientKey + WEBSOCKET_GUID
        val sha1Hash = sha1(concatenated.encodeToByteArray())
        return Base64.encode(sha1Hash)
    }
}

/**
 * Platform-specific SHA-1 hash function
 */
expect fun sha1(data: ByteArray): ByteArray
