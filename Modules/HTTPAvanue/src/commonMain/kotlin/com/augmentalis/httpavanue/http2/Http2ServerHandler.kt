package com.augmentalis.httpavanue.http2

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.http.HttpRequest
import com.augmentalis.httpavanue.http.HttpResponse
import com.augmentalis.httpavanue.platform.Socket

/**
 * Detects HTTP/2 upgrade (h2c) in an HTTP/1.1 request and branches into HTTP/2 handling.
 *
 * Supports two modes:
 * 1. **h2c Upgrade**: Client sends HTTP/1.1 request with `Upgrade: h2c` header
 * 2. **Prior Knowledge (h2c)**: Client sends HTTP/2 connection preface directly (PRI *)
 */
object Http2ServerHandler {
    private val logger = LoggerFactory.getLogger("Http2ServerHandler")

    /** Check if an HTTP/1.1 request has an Upgrade: h2c header */
    fun isH2cUpgradeRequest(request: HttpRequest): Boolean {
        return request.header("Upgrade")?.lowercase() == "h2c" &&
            request.header("HTTP2-Settings") != null
    }

    /**
     * Check if raw bytes look like an HTTP/2 connection preface (prior knowledge).
     * The preface starts with "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n" (24 bytes).
     */
    fun isPriorKnowledgePreface(data: ByteArray): Boolean {
        if (data.size < Http2FrameCodec.CONNECTION_PREFACE.size) return false
        return data.copyOfRange(0, Http2FrameCodec.CONNECTION_PREFACE.size)
            .contentEquals(Http2FrameCodec.CONNECTION_PREFACE)
    }

    /**
     * Handle h2c upgrade from HTTP/1.1 to HTTP/2.
     * Sends 101 Switching Protocols, then starts HTTP/2 connection.
     */
    suspend fun handleH2cUpgrade(
        socket: Socket,
        request: HttpRequest,
        settings: Http2Settings = Http2Settings(),
        requestHandler: suspend (HttpRequest) -> HttpResponse,
    ) {
        logger.i { "HTTP/2 h2c upgrade for ${request.method} ${request.uri}" }

        // Send 101 Switching Protocols response
        val upgradeResponse = HttpResponse(
            status = 101,
            statusMessage = "Switching Protocols",
            headers = mapOf("Connection" to "Upgrade", "Upgrade" to "h2c"),
        )
        socket.sink().apply { write(upgradeResponse.toBytes()); flush() }

        // Start HTTP/2 connection (client will send preface next)
        val connection = Http2Connection(socket, settings, requestHandler)
        connection.run()
    }

    /**
     * Handle HTTP/2 with prior knowledge (no upgrade, client already speaks HTTP/2).
     * Connection preface has already been detected by the caller.
     */
    suspend fun handlePriorKnowledge(
        socket: Socket,
        settings: Http2Settings = Http2Settings(),
        requestHandler: suspend (HttpRequest) -> HttpResponse,
    ) {
        logger.i { "HTTP/2 prior knowledge connection" }
        val connection = Http2Connection(socket, settings, requestHandler)
        connection.run()
    }
}
