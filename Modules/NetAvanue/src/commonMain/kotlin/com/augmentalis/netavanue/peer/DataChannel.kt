package com.augmentalis.netavanue.peer

import com.avanues.logging.LoggerFactory
import com.augmentalis.netavanue.ice.UdpSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Bidirectional data channel over an ICE-negotiated UDP path.
 *
 * Provides a simple message-based API for sending and receiving data between
 * two connected peers. Messages are framed with a 4-byte length prefix for
 * boundary detection over UDP.
 *
 * Frame format:
 * ```
 * [2 bytes: channel ID] [2 bytes: flags] [4 bytes: length] [N bytes: payload]
 * ```
 *
 * Flags:
 * - 0x01 = TEXT (payload is UTF-8)
 * - 0x02 = BINARY (raw bytes)
 * - 0x04 = CLOSE (channel closing)
 */
enum class DataChannelState { CONNECTING, OPEN, CLOSING, CLOSED }

data class DataChannelMessage(
    val data: ByteArray,
    val isText: Boolean,
) {
    val text: String get() = data.decodeToString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataChannelMessage) return false
        return data.contentEquals(other.data) && isText == other.isText
    }

    override fun hashCode(): Int = data.contentHashCode() * 31 + isText.hashCode()
}

class DataChannel(
    val label: String,
    val channelId: Int,
    private val socket: UdpSocket,
    private val remoteHost: String,
    private val remotePort: Int,
) {
    private val logger = LoggerFactory.getLogger("DataChannel[$label]")

    private val _state = MutableStateFlow(DataChannelState.CONNECTING)
    val state: StateFlow<DataChannelState> = _state.asStateFlow()

    private val _messages = MutableSharedFlow<DataChannelMessage>(extraBufferCapacity = 64)
    val messages: SharedFlow<DataChannelMessage> = _messages.asSharedFlow()

    /** Text messages only (filtered convenience flow) */
    val textMessages: Flow<String> = messages.filter { it.isText }.map { it.text }

    /** Binary messages only */
    val binaryMessages: Flow<ByteArray> = messages.filter { !it.isText }.map { it.data }

    private var receiveJob: Job? = null

    /** Open the channel and start receiving messages */
    fun open(scope: CoroutineScope) {
        _state.value = DataChannelState.OPEN
        receiveJob = scope.launch { receiveLoop() }
        logger.i { "DataChannel '$label' opened (id=$channelId)" }
    }

    /** Send a text message */
    suspend fun sendText(text: String) {
        if (_state.value != DataChannelState.OPEN) throw IllegalStateException("Channel not open")
        val payload = text.encodeToByteArray()
        sendFrame(payload, FLAG_TEXT)
    }

    /** Send binary data */
    suspend fun send(data: ByteArray) {
        if (_state.value != DataChannelState.OPEN) throw IllegalStateException("Channel not open")
        sendFrame(data, FLAG_BINARY)
    }

    /** Close the channel */
    suspend fun close() {
        if (_state.value == DataChannelState.CLOSED) return
        _state.value = DataChannelState.CLOSING
        try {
            sendFrame(byteArrayOf(), FLAG_CLOSE)
        } catch (_: Exception) { /* best effort */ }
        receiveJob?.cancel()
        _state.value = DataChannelState.CLOSED
        logger.i { "DataChannel '$label' closed" }
    }

    private suspend fun sendFrame(payload: ByteArray, flags: Int) {
        val frame = ByteArray(HEADER_SIZE + payload.size)
        // Channel ID (2 bytes, big-endian)
        frame[0] = (channelId shr 8).toByte()
        frame[1] = channelId.toByte()
        // Flags (2 bytes)
        frame[2] = (flags shr 8).toByte()
        frame[3] = flags.toByte()
        // Length (4 bytes, big-endian)
        frame[4] = (payload.size shr 24).toByte()
        frame[5] = (payload.size shr 16).toByte()
        frame[6] = (payload.size shr 8).toByte()
        frame[7] = payload.size.toByte()
        // Payload
        payload.copyInto(frame, HEADER_SIZE)
        socket.send(frame, remoteHost, remotePort)
    }

    private suspend fun receiveLoop() {
        val buffer = ByteArray(65536)
        while (_state.value == DataChannelState.OPEN) {
            val packet = socket.receive(buffer, timeoutMs = 1000) ?: continue
            if (packet.length < HEADER_SIZE) continue

            val data = buffer.copyOf(packet.length)
            val recvChannelId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
            if (recvChannelId != channelId) continue // Not for this channel

            val flags = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
            val payloadLen = ((data[4].toInt() and 0xFF) shl 24) or
                ((data[5].toInt() and 0xFF) shl 16) or
                ((data[6].toInt() and 0xFF) shl 8) or
                (data[7].toInt() and 0xFF)

            if (flags and FLAG_CLOSE != 0) {
                _state.value = DataChannelState.CLOSED
                receiveJob?.cancel()
                logger.i { "DataChannel '$label' closed by remote" }
                break
            }

            if (payloadLen < 0 || payloadLen > MAX_PAYLOAD_SIZE) {
                logger.w { "Invalid payload length: $payloadLen" }
                continue
            }

            if (payloadLen > 0 && HEADER_SIZE + payloadLen <= data.size) {
                val payload = data.copyOfRange(HEADER_SIZE, HEADER_SIZE + payloadLen)
                val isText = flags and FLAG_TEXT != 0
                _messages.tryEmit(DataChannelMessage(payload, isText))
            }
        }
    }

    companion object {
        const val HEADER_SIZE = 8
        const val FLAG_TEXT = 0x01
        const val FLAG_BINARY = 0x02
        const val FLAG_CLOSE = 0x04
        const val MAX_PAYLOAD_SIZE = 65528 // 64KB - header
    }
}
