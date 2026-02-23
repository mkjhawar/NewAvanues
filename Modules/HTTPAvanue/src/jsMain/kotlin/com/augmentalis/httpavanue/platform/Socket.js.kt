package com.augmentalis.httpavanue.platform

import com.augmentalis.httpavanue.io.AvanueSource
import com.augmentalis.httpavanue.io.AvanueSink

/**
 * JS actual for [Socket] — raw TCP sockets are not available in browser environments.
 *
 * Browsers communicate via the WebSocket API (handled by the browser engine)
 * or HTTP fetch. Raw TCP is inaccessible from JavaScript for security reasons.
 *
 * For WebSocket communication in the browser, use the browser-native WebSocket
 * API directly rather than going through HTTPAvanue's Socket abstraction.
 */
actual class Socket private constructor() {
    actual companion object {
        actual suspend fun connect(host: String, port: Int, config: SocketConfig): Socket {
            throw UnsupportedOperationException(
                "Raw TCP sockets are not available in browser/JS environments. " +
                    "Use the browser-native WebSocket API for real-time communication."
            )
        }
    }

    actual fun source(): AvanueSource = throw UnsupportedOperationException("No TCP in browser")
    actual fun sink(): AvanueSink = throw UnsupportedOperationException("No TCP in browser")
    actual fun close() {}
    actual fun isConnected(): Boolean = false
    actual fun remoteAddress(): String = ""
    actual fun setReadTimeout(timeoutMs: Long) {}
}

/**
 * JS actual for [SocketServer] — browsers cannot act as network servers.
 *
 * Browsers are clients by design. Server functionality requires Node.js
 * or a dedicated server runtime.
 */
actual class SocketServer actual constructor(config: SocketConfig) {
    actual fun bind(port: Int, backlog: Int) {
        throw UnsupportedOperationException(
            "TCP server sockets are not available in browser/JS environments. " +
                "Run HTTPAvanue server on Node.js or a JVM target instead."
        )
    }

    actual suspend fun accept(): Socket {
        throw UnsupportedOperationException("Cannot accept connections in browser")
    }

    actual fun close() {}
    actual fun isBound(): Boolean = false
    actual fun localPort(): Int = -1
}
