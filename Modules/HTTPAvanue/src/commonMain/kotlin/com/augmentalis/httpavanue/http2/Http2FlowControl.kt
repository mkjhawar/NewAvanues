package com.augmentalis.httpavanue.http2

import okio.BufferedSink

/**
 * HTTP/2 flow control manager — connection-level + stream-level window management (RFC 7540 Section 6.9)
 */
class Http2FlowControl(
    initialWindowSize: Int = 65535,
) {
    var connectionSendWindow: Int = initialWindowSize
        private set
    var connectionReceiveWindow: Int = initialWindowSize
        private set

    /** Consume connection-level send window */
    fun consumeConnectionSendWindow(bytes: Int): Boolean {
        if (bytes > connectionSendWindow) return false
        connectionSendWindow -= bytes
        return true
    }

    /** Consume connection-level receive window */
    fun consumeConnectionReceiveWindow(bytes: Int): Boolean {
        if (bytes > connectionReceiveWindow) return false
        connectionReceiveWindow -= bytes
        return true
    }

    /** Handle incoming WINDOW_UPDATE for connection (stream 0) */
    fun updateConnectionSendWindow(increment: Int) {
        val newWindow = connectionSendWindow.toLong() + increment
        if (newWindow > Int.MAX_VALUE) throw Http2Exception(Http2ErrorCode.FLOW_CONTROL_ERROR, "Connection window overflow")
        connectionSendWindow = newWindow.toInt()
    }

    /** Send WINDOW_UPDATE to peer for connection or stream */
    fun sendWindowUpdate(sink: BufferedSink, streamId: Int, increment: Int) {
        if (increment <= 0) return
        Http2FrameCodec.writeWindowUpdate(sink, streamId, increment)
        if (streamId == 0) connectionReceiveWindow += increment
    }

    /** Calculate how many DATA bytes we can send, respecting both connection and stream windows */
    fun availableSendBytes(stream: Http2Stream, maxFrameSize: Int): Int {
        return minOf(connectionSendWindow, stream.sendWindowSize, maxFrameSize)
    }
}
