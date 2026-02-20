package com.augmentalis.httpavanue.websocket

import okio.ByteString.Companion.toByteString
import kotlin.random.Random

/**
 * Client-side WebSocket handshake (RFC 6455).
 * Uses Okio's ByteString.sha1() instead of platform expect/actual.
 */
object WebSocketClientHandshake {
    private const val WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11"

    fun generateKey(): String = Random.nextBytes(16).toByteString().base64()

    fun createHandshakeRequest(config: WebSocketClientConfig, key: String): String {
        val url = config.parsedUrl
        return buildString {
            append("GET ${url.path} HTTP/1.1\r\n")
            append("Host: ${url.host}:${url.port}\r\n")
            append("Upgrade: websocket\r\n")
            append("Connection: Upgrade\r\n")
            append("Sec-WebSocket-Key: $key\r\n")
            append("Sec-WebSocket-Version: 13\r\n")
            if (config.subprotocols.isNotEmpty()) append("Sec-WebSocket-Protocol: ${config.subprotocols.joinToString(", ")}\r\n")
            config.headers.forEach { (name, value) -> append("$name: $value\r\n") }
            append("\r\n")
        }
    }

    fun validateHandshakeResponse(response: String, expectedKey: String): HandshakeResult {
        val lines = response.split("\r\n")
        val statusLine = lines.firstOrNull() ?: return HandshakeResult.Error("Empty response")
        if (!statusLine.startsWith("HTTP/1.1 101")) return HandshakeResult.Error("Expected 101, got: $statusLine")
        val headers = mutableMapOf<String, String>()
        for (i in 1 until lines.size) {
            val line = lines[i]; if (line.isEmpty()) break
            val colonIndex = line.indexOf(':')
            if (colonIndex > 0) headers[line.substring(0, colonIndex).trim().lowercase()] = line.substring(colonIndex + 1).trim()
        }
        if (headers["upgrade"]?.lowercase() != "websocket") return HandshakeResult.Error("Missing/invalid Upgrade header")
        if (headers["connection"]?.lowercase() != "upgrade") return HandshakeResult.Error("Missing/invalid Connection header")
        val serverAccept = headers["sec-websocket-accept"] ?: return HandshakeResult.Error("Missing Sec-WebSocket-Accept")
        val expectedAccept = generateExpectedAcceptKey(expectedKey)
        if (serverAccept != expectedAccept) return HandshakeResult.Error("Invalid Sec-WebSocket-Accept")
        return HandshakeResult.Success(headers["sec-websocket-protocol"])
    }

    private fun generateExpectedAcceptKey(clientKey: String): String {
        val concatenated = clientKey + WEBSOCKET_GUID
        return concatenated.encodeToByteArray().toByteString().sha1().base64()
    }
}

sealed class HandshakeResult {
    data class Success(val subprotocol: String?) : HandshakeResult()
    data class Error(val message: String) : HandshakeResult()
}
