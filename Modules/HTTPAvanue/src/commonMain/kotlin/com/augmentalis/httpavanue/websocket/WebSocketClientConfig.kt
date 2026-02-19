package com.augmentalis.httpavanue.websocket

import com.augmentalis.httpavanue.platform.SocketConfig

data class WebSocketClientConfig(
    val url: String,
    val subprotocols: List<String> = emptyList(),
    val headers: Map<String, String> = emptyMap(),
    val connectTimeout: Long = 10_000,
    val handshakeTimeout: Long = 10_000,
    val maxMessageSize: Long = 10 * 1024 * 1024,
    val fragmentTimeout: Long = 30_000,
    val socketReadTimeout: Long = 60_000,
    val socketConfig: SocketConfig = SocketConfig(),
    val reconnectConfig: WebSocketReconnectConfig = WebSocketReconnectConfig(),
) {
    val parsedUrl: WebSocketUrl by lazy { parseWebSocketUrl(url) }
    init { require(url.startsWith("ws://") || url.startsWith("wss://")) { "URL must start with ws:// or wss://" } }
}

data class WebSocketReconnectConfig(val maxRetries: Int = 5, val baseDelayMs: Long = 2000, val maxDelayMs: Long = 60000)
data class WebSocketUrl(val scheme: String, val host: String, val port: Int, val path: String, val isSecure: Boolean)

private fun parseWebSocketUrl(url: String): WebSocketUrl {
    val isSecure = url.startsWith("wss://")
    val defaultPort = if (isSecure) 443 else 80
    val withoutScheme = url.substringAfter("://")
    val hostPortAndPath = withoutScheme.split("/", limit = 2)
    val hostPort = hostPortAndPath[0]
    val path = "/" + (hostPortAndPath.getOrNull(1) ?: "")
    val (host, port) = if (hostPort.contains(":")) { val parts = hostPort.split(":"); parts[0] to parts[1].toInt() } else hostPort to defaultPort
    return WebSocketUrl(if (isSecure) "wss" else "ws", host, port, path, isSecure)
}
