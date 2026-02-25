package com.augmentalis.netavanue.ice

/**
 * JS/Browser actual for [UdpSocket].
 *
 * Raw UDP sockets are not available in browser environments. Browsers handle
 * ICE connectivity checks internally through the RTCPeerConnection API —
 * the browser's ICE agent manages STUN/TURN communication automatically.
 *
 * On the JS target, ICE is delegated entirely to the browser's WebRTC engine.
 * This actual exists only to satisfy the KMP compilation contract.
 */
actual class UdpSocket actual constructor() {

    actual val localPort: Int
        get() = -1

    actual val isClosed: Boolean
        get() = true

    actual suspend fun bind(port: Int) {
        throw UnsupportedOperationException(
            "Raw UDP sockets are not available in browser/JS environments. " +
                "The browser's WebRTC ICE agent handles STUN/TURN communication automatically."
        )
    }

    actual suspend fun send(data: ByteArray, host: String, port: Int) {
        throw UnsupportedOperationException(
            "Raw UDP send is not available in browser/JS environments."
        )
    }

    actual suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket? {
        throw UnsupportedOperationException(
            "Raw UDP receive is not available in browser/JS environments."
        )
    }

    actual fun close() {
        // No-op: nothing to close
    }
}
