package com.augmentalis.httpavanue.websocket

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.platform.Socket
import com.augmentalis.httpavanue.platform.currentTimeMillis
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

enum class WebSocketState { CONNECTING, OPEN, CLOSING, CLOSED }

sealed class WebSocketMessage {
    data class Text(val data: String) : WebSocketMessage()
    data class Binary(val data: ByteArray) : WebSocketMessage() {
        override fun equals(other: Any?) = other is Binary && data.contentEquals(other.data)
        override fun hashCode() = data.contentHashCode()
    }
    data class Close(val code: WebSocketCloseCode, val reason: String) : WebSocketMessage()
}

/**
 * WebSocket connection with fragmentation, ping/pong, and bounded channels
 */
class WebSocket(
    private val socket: Socket,
    private val isServer: Boolean = true,
    private val fragmentTimeout: Long = 30000,
    private val maxMessageSize: Long = 10 * 1024 * 1024,
) {
    private val logger = LoggerFactory.getLogger("WebSocket")
    private val stateMutex = Mutex()
    private var _state = WebSocketState.OPEN
    private val incomingChannel = Channel<WebSocketMessage>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val outgoingChannel = Channel<WebSocketFrame>(capacity = 100, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var receiveJob: Job? = null
    private var sendJob: Job? = null
    private var fragmentedMessageType: WebSocketOpcode? = null
    private val fragmentBuffer = mutableListOf<ByteArray>()
    private var fragmentStartTime: Long = 0

    private suspend fun getState() = stateMutex.withLock { _state }
    private suspend fun setState(newState: WebSocketState) { stateMutex.withLock { _state = newState } }

    val messages: Flow<WebSocketMessage> = incomingChannel.receiveAsFlow()

    fun start(scope: CoroutineScope) {
        receiveJob = scope.launch {
            try {
                while (isActive && getState() == WebSocketState.OPEN) {
                    val frame = WebSocketParser.parseFrame(socket.source(), maxMessageSize)
                    handleIncomingFrame(frame)
                }
            } catch (e: WebSocketMessageTooLargeException) {
                logger.w { "Message too large: ${e.message}" }
                close(WebSocketCloseCode.MESSAGE_TOO_BIG, e.message ?: "Message too large")
            } catch (e: CancellationException) { logger.d { "WebSocket receive cancelled" }
            } catch (e: Exception) { logger.e({ "Error receiving WebSocket frame" }, e); close(WebSocketCloseCode.INTERNAL_ERROR, "Receive error") }
        }
        sendJob = scope.launch {
            try {
                for (frame in outgoingChannel) {
                    socket.sink().apply { write(WebSocketParser.frameToBytes(frame)); flush() }
                }
            } catch (e: CancellationException) { logger.d { "WebSocket send cancelled" }
            } catch (e: Exception) { logger.e({ "Error sending WebSocket frame" }, e) }
        }
    }

    suspend fun sendText(text: String) { if (getState() != WebSocketState.OPEN) throw WebSocketException("WebSocket is not open"); outgoingChannel.send(WebSocketFrame.text(text)) }
    suspend fun sendBinary(data: ByteArray) { if (getState() != WebSocketState.OPEN) throw WebSocketException("WebSocket is not open"); outgoingChannel.send(WebSocketFrame.binary(data)) }
    suspend fun sendPing(payload: ByteArray = byteArrayOf()) { outgoingChannel.send(WebSocketFrame.ping(payload)) }

    suspend fun close(code: WebSocketCloseCode = WebSocketCloseCode.NORMAL, reason: String = "") {
        val currentState = getState()
        if (currentState == WebSocketState.CLOSED || currentState == WebSocketState.CLOSING) return
        setState(WebSocketState.CLOSING)
        try { outgoingChannel.send(WebSocketFrame.close(code, reason)); delay(100) }
        finally { setState(WebSocketState.CLOSED); receiveJob?.cancel(); sendJob?.cancel(); outgoingChannel.close(); incomingChannel.close(); socket.close() }
    }

    private suspend fun handleIncomingFrame(frame: WebSocketFrame) {
        stateMutex.withLock {
            if (fragmentedMessageType != null && currentTimeMillis() - fragmentStartTime > fragmentTimeout) {
                fragmentedMessageType = null; fragmentBuffer.clear(); fragmentStartTime = 0
            }
        }
        when (frame.opcode) {
            WebSocketOpcode.TEXT, WebSocketOpcode.BINARY -> {
                if (stateMutex.withLock { fragmentedMessageType } != null) { close(WebSocketCloseCode.PROTOCOL_ERROR, "Unexpected message during fragmentation"); return }
                if (frame.isFinal) deliverMessage(frame.opcode, frame.payload)
                else stateMutex.withLock { fragmentedMessageType = frame.opcode; fragmentBuffer.add(frame.payload); fragmentStartTime = currentTimeMillis() }
            }
            WebSocketOpcode.CONTINUATION -> {
                val messageType = stateMutex.withLock { fragmentedMessageType } ?: run { close(WebSocketCloseCode.PROTOCOL_ERROR, "Unexpected CONTINUATION"); return }
                stateMutex.withLock { fragmentBuffer.add(frame.payload) }
                if (frame.isFinal) {
                    val (payload, msgType) = stateMutex.withLock {
                        val totalSize = fragmentBuffer.sumOf { it.size.toLong() }
                        if (totalSize > maxMessageSize) { close(WebSocketCloseCode.MESSAGE_TOO_BIG, "Fragmented message too large"); return }
                        val result = ByteArray(totalSize.toInt()); var offset = 0
                        fragmentBuffer.forEach { it.copyInto(result, offset); offset += it.size }
                        val type = fragmentedMessageType!!
                        fragmentedMessageType = null; fragmentBuffer.clear(); fragmentStartTime = 0
                        result to type
                    }
                    deliverMessage(msgType, payload)
                }
            }
            WebSocketOpcode.PING -> outgoingChannel.send(WebSocketFrame.pong(frame.payload))
            WebSocketOpcode.PONG -> logger.d { "Received PONG" }
            WebSocketOpcode.CLOSE -> {
                val code = if (frame.payload.size >= 2) {
                    val codeValue = ((frame.payload[0].toInt() and 0xFF) shl 8) or (frame.payload[1].toInt() and 0xFF)
                    WebSocketCloseCode.from(codeValue) ?: WebSocketCloseCode.NORMAL
                } else WebSocketCloseCode.NORMAL
                val reason = if (frame.payload.size > 2) frame.payload.copyOfRange(2, frame.payload.size).decodeToString() else ""
                if (getState() == WebSocketState.OPEN) close(code, reason)
                incomingChannel.send(WebSocketMessage.Close(code, reason))
            }
        }
    }

    private suspend fun deliverMessage(opcode: WebSocketOpcode, payload: ByteArray) {
        when (opcode) {
            WebSocketOpcode.TEXT -> incomingChannel.send(WebSocketMessage.Text(payload.decodeToString()))
            WebSocketOpcode.BINARY -> incomingChannel.send(WebSocketMessage.Binary(payload))
            else -> logger.w { "Unexpected opcode for delivery: $opcode" }
        }
    }

    fun isOpen() = _state == WebSocketState.OPEN
}
