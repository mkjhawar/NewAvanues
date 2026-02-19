package com.augmentalis.httpavanue.http2

import kotlinx.coroutines.channels.Channel

/**
 * HTTP/2 stream states (RFC 7540 Section 5.1)
 */
enum class Http2StreamState {
    IDLE, RESERVED_LOCAL, RESERVED_REMOTE, OPEN, HALF_CLOSED_LOCAL, HALF_CLOSED_REMOTE, CLOSED
}

/**
 * HTTP/2 stream — represents a single request/response exchange within a connection
 */
class Http2Stream(
    val id: Int,
    initialWindowSize: Int = 65535,
) {
    var state: Http2StreamState = Http2StreamState.IDLE
        internal set
    var sendWindowSize: Int = initialWindowSize
        internal set
    var receiveWindowSize: Int = initialWindowSize
        internal set

    val headers = mutableListOf<Pair<String, String>>()
    val dataChannel = Channel<ByteArray>(Channel.BUFFERED)
    var endStreamReceived = false
        internal set
    var endStreamSent = false
        internal set

    /** Consume send window for outgoing DATA frames */
    fun consumeSendWindow(bytes: Int): Boolean {
        if (bytes > sendWindowSize) return false
        sendWindowSize -= bytes
        return true
    }

    /** Consume receive window for incoming DATA frames */
    fun consumeReceiveWindow(bytes: Int): Boolean {
        if (bytes > receiveWindowSize) return false
        receiveWindowSize -= bytes
        return true
    }

    /** Increase send window (from WINDOW_UPDATE) */
    fun increaseSendWindow(increment: Int) {
        val newWindow = sendWindowSize.toLong() + increment
        if (newWindow > Int.MAX_VALUE) throw Http2Exception(Http2ErrorCode.FLOW_CONTROL_ERROR,
            "Window size overflow on stream $id", id)
        sendWindowSize = newWindow.toInt()
    }

    /** Increase receive window */
    fun increaseReceiveWindow(increment: Int) {
        receiveWindowSize += increment
    }

    fun isOpen() = state == Http2StreamState.OPEN || state == Http2StreamState.HALF_CLOSED_LOCAL || state == Http2StreamState.HALF_CLOSED_REMOTE
}
