package com.augmentalis.httpavanue.http2

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.hpack.HpackDecoder
import com.augmentalis.httpavanue.hpack.HpackEncoder
import com.augmentalis.httpavanue.http.*
import com.augmentalis.httpavanue.platform.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * HTTP/2 connection handler — manages the connection lifecycle, stream multiplexing,
 * SETTINGS exchange, PING/GOAWAY, and frame dispatch.
 */
class Http2Connection(
    private val socket: Socket,
    private val localSettings: Http2Settings = Http2Settings(),
    private val requestHandler: suspend (HttpRequest) -> HttpResponse,
) {
    private val logger = LoggerFactory.getLogger("Http2Connection")
    private val streams = mutableMapOf<Int, Http2Stream>()
    private var remoteSettings = Http2Settings()
    private val hpackDecoder = HpackDecoder(localSettings.headerTableSize)
    private val hpackEncoder = HpackEncoder(remoteSettings.headerTableSize)
    private val flowControl = Http2FlowControl(localSettings.initialWindowSize)
    private var lastStreamId = 0
    private var goawaySent = false
    /** Protects all sink writes — HTTP/2 frames must not interleave across streams */
    private val sinkMutex = Mutex()

    /** Run the HTTP/2 connection loop */
    suspend fun run() = coroutineScope {
        val source = socket.source()
        val sink = socket.sink()

        // Read and verify client connection preface
        val preface = source.readByteArray(Http2FrameCodec.CONNECTION_PREFACE.size.toLong())
        if (!preface.contentEquals(Http2FrameCodec.CONNECTION_PREFACE)) {
            logger.e { "Invalid connection preface" }
            socket.close()
            return@coroutineScope
        }

        // Send server SETTINGS
        sinkMutex.withLock { Http2FrameCodec.writeSettings(sink, localSettings) }
        logger.d { "Sent server SETTINGS" }

        // Frame dispatch loop
        try {
            while (isActive && !goawaySent) {
                val frame = Http2FrameCodec.readFrame(source, remoteSettings.maxFrameSize)
                when (frame.type) {
                    Http2FrameType.SETTINGS -> handleSettings(sink, frame)
                    Http2FrameType.HEADERS -> handleHeaders(sink, frame, this)
                    Http2FrameType.DATA -> handleData(sink, frame)
                    Http2FrameType.WINDOW_UPDATE -> handleWindowUpdate(frame)
                    Http2FrameType.PING -> handlePing(sink, frame)
                    Http2FrameType.GOAWAY -> { logger.i { "Received GOAWAY" }; break }
                    Http2FrameType.RST_STREAM -> handleRstStream(frame)
                    Http2FrameType.PRIORITY -> { /* Advisory only, ignored */ }
                    Http2FrameType.CONTINUATION -> handleContinuation(sink, frame, this)
                    Http2FrameType.PUSH_PROMISE -> {
                        sinkMutex.withLock { Http2FrameCodec.writeGoaway(sink, lastStreamId, Http2ErrorCode.PROTOCOL_ERROR) }
                        goawaySent = true
                    }
                    null -> {
                        // Unknown frame type — MUST be ignored per spec (RFC 7540 Section 4.1)
                        logger.d { "Ignoring unknown frame type: ${frame.typeValue}" }
                    }
                }
            }
        } catch (e: Http2Exception) {
            logger.e({ "HTTP/2 error: ${e.message}" }, e)
            try { sinkMutex.withLock { Http2FrameCodec.writeGoaway(sink, lastStreamId, e.errorCode) } } catch (_: Exception) {}
        } catch (e: CancellationException) {
            logger.d { "HTTP/2 connection cancelled" }
        } catch (e: Exception) {
            logger.e({ "HTTP/2 connection error" }, e)
            try { sinkMutex.withLock { Http2FrameCodec.writeGoaway(sink, lastStreamId, Http2ErrorCode.INTERNAL_ERROR) } } catch (_: Exception) {}
        } finally {
            streams.values.forEach { it.dataChannel.close() }
            streams.clear()
        }
    }

    private suspend fun handleSettings(sink: okio.BufferedSink, frame: Http2Frame) {
        if (frame.hasFlag(Http2Flags.ACK)) {
            logger.d { "Received SETTINGS ACK" }
            return
        }
        if (frame.streamId != 0) throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "SETTINGS on non-zero stream")
        remoteSettings = Http2Settings.decode(frame.payload)
        logger.d { "Received SETTINGS: maxConcurrent=${remoteSettings.maxConcurrentStreams}, windowSize=${remoteSettings.initialWindowSize}" }
        sinkMutex.withLock { Http2FrameCodec.writeSettings(sink, Http2Settings(), ack = true) }
    }

    private suspend fun handleHeaders(sink: okio.BufferedSink, frame: Http2Frame, scope: CoroutineScope) {
        val streamId = frame.streamId
        if (streamId == 0) throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "HEADERS on stream 0")
        if (streamId % 2 == 0) throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "Client initiated even stream $streamId")
        if (streamId <= lastStreamId) throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "Stream $streamId <= last $lastStreamId")
        lastStreamId = streamId

        if (streams.size >= localSettings.maxConcurrentStreams) {
            sinkMutex.withLock { Http2FrameCodec.writeRstStream(sink, streamId, Http2ErrorCode.REFUSED_STREAM) }
            return
        }

        val stream = Http2Stream(streamId, localSettings.initialWindowSize)
        stream.state = Http2StreamState.OPEN
        streams[streamId] = stream

        val headers = hpackDecoder.decode(frame.payload)
        stream.headers.addAll(headers)

        if (frame.hasFlag(Http2Flags.END_STREAM)) {
            stream.endStreamReceived = true
            stream.state = Http2StreamState.HALF_CLOSED_REMOTE
        }

        if (frame.hasFlag(Http2Flags.END_HEADERS)) {
            // Full headers received, dispatch request
            scope.launch { dispatchRequest(sink, stream) }
        }
    }

    private fun handleContinuation(sink: okio.BufferedSink, frame: Http2Frame, scope: CoroutineScope) {
        val stream = streams[frame.streamId] ?: throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "CONTINUATION for unknown stream ${frame.streamId}")
        val headers = hpackDecoder.decode(frame.payload)
        stream.headers.addAll(headers)
        if (frame.hasFlag(Http2Flags.END_HEADERS)) {
            scope.launch { dispatchRequest(sink, stream) }
        }
    }

    private suspend fun handleData(sink: okio.BufferedSink, frame: Http2Frame) {
        val stream = streams[frame.streamId] ?: throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "DATA for unknown stream ${frame.streamId}")
        if (!stream.isOpen()) throw Http2Exception(Http2ErrorCode.STREAM_CLOSED, "DATA on closed stream ${frame.streamId}", frame.streamId)

        val dataLength = frame.payload.size
        if (!flowControl.consumeConnectionReceiveWindow(dataLength))
            throw Http2Exception(Http2ErrorCode.FLOW_CONTROL_ERROR, "Connection receive window exceeded")
        if (!stream.consumeReceiveWindow(dataLength))
            throw Http2Exception(Http2ErrorCode.FLOW_CONTROL_ERROR, "Stream ${frame.streamId} receive window exceeded", frame.streamId)

        stream.dataChannel.trySend(frame.payload)

        if (frame.hasFlag(Http2Flags.END_STREAM)) {
            stream.endStreamReceived = true
            stream.dataChannel.close()
            if (stream.endStreamSent) { stream.state = Http2StreamState.CLOSED; streams.remove(frame.streamId) }
            else stream.state = Http2StreamState.HALF_CLOSED_REMOTE
        }

        // Send WINDOW_UPDATE if window is getting low
        if (flowControl.connectionReceiveWindow < localSettings.initialWindowSize / 2) {
            val increment = localSettings.initialWindowSize - flowControl.connectionReceiveWindow
            sinkMutex.withLock { flowControl.sendWindowUpdate(sink, 0, increment) }
        }
    }

    private fun handleWindowUpdate(frame: Http2Frame) {
        if (frame.payload.size != 4) throw Http2Exception(Http2ErrorCode.FRAME_SIZE_ERROR, "WINDOW_UPDATE payload must be 4 bytes")
        val increment = ((frame.payload[0].toInt() and 0x7F) shl 24) or
            ((frame.payload[1].toInt() and 0xFF) shl 16) or
            ((frame.payload[2].toInt() and 0xFF) shl 8) or
            (frame.payload[3].toInt() and 0xFF)
        if (increment == 0) throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "Zero WINDOW_UPDATE increment")
        if (frame.streamId == 0) flowControl.updateConnectionSendWindow(increment)
        else streams[frame.streamId]?.increaseSendWindow(increment)
    }

    private suspend fun handlePing(sink: okio.BufferedSink, frame: Http2Frame) {
        if (frame.streamId != 0) throw Http2Exception(Http2ErrorCode.PROTOCOL_ERROR, "PING on non-zero stream")
        if (frame.payload.size != 8) throw Http2Exception(Http2ErrorCode.FRAME_SIZE_ERROR, "PING payload must be 8 bytes")
        if (!frame.hasFlag(Http2Flags.ACK)) sinkMutex.withLock { Http2FrameCodec.writePing(sink, ack = true, opaqueData = frame.payload) }
    }

    private fun handleRstStream(frame: Http2Frame) {
        val stream = streams.remove(frame.streamId) ?: return
        stream.state = Http2StreamState.CLOSED
        stream.dataChannel.close()
        logger.d { "Stream ${frame.streamId} reset" }
    }

    private suspend fun dispatchRequest(sink: okio.BufferedSink, stream: Http2Stream) {
        try {
            val headerMap = mutableMapOf<String, String>()
            var method = HttpMethod.GET; var path = "/"; var scheme = "http"; var authority = ""
            for ((name, value) in stream.headers) {
                when (name) {
                    ":method" -> method = HttpMethod.from(value)
                    ":path" -> path = value
                    ":scheme" -> scheme = value
                    ":authority" -> authority = value
                    else -> if (!name.startsWith(":")) headerMap[name] = value
                }
            }
            if (authority.isNotEmpty()) headerMap["Host"] = authority

            val request = HttpRequest(method = method, uri = path, version = "HTTP/2", headers = headerMap)
            val response = requestHandler(request)
            sinkMutex.withLock { sendResponse(sink, stream, response) }
        } catch (e: Exception) {
            logger.e({ "Error dispatching HTTP/2 request on stream ${stream.id}" }, e)
            sinkMutex.withLock { Http2FrameCodec.writeRstStream(sink, stream.id, Http2ErrorCode.INTERNAL_ERROR) }
            stream.state = Http2StreamState.CLOSED
            streams.remove(stream.id)
        }
    }

    private fun sendResponse(sink: okio.BufferedSink, stream: Http2Stream, response: HttpResponse) {
        val responseHeaders = mutableListOf<Pair<String, String>>()
        responseHeaders.add(":status" to response.status.toString())
        for ((name, value) in response.headers) {
            responseHeaders.add(name.lowercase() to value)
        }

        val headerBlock = hpackEncoder.encode(responseHeaders)
        val hasBody = response.body != null && response.body.isNotEmpty()

        Http2FrameCodec.writeHeaders(sink, stream.id, headerBlock, endStream = !hasBody, endHeaders = true)

        if (hasBody) {
            val body = response.body!!
            var offset = 0
            while (offset < body.size) {
                val chunkSize = minOf(body.size - offset, remoteSettings.maxFrameSize)
                val isLast = offset + chunkSize >= body.size
                val chunk = body.copyOfRange(offset, offset + chunkSize)
                Http2FrameCodec.writeData(sink, stream.id, chunk, endStream = isLast)
                offset += chunkSize
            }
        }

        stream.endStreamSent = true
        if (stream.endStreamReceived) { stream.state = Http2StreamState.CLOSED; streams.remove(stream.id) }
        else stream.state = Http2StreamState.HALF_CLOSED_LOCAL
    }
}
