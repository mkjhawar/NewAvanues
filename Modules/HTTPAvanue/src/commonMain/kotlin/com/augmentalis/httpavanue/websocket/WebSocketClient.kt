package com.augmentalis.httpavanue.websocket

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.platform.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okio.Buffer
import kotlin.math.min
import kotlin.math.pow

enum class WebSocketConnectionState { DISCONNECTED, CONNECTING, CONNECTED, RECONNECTING, FAILED }

/**
 * WebSocket client with automatic reconnection and exponential backoff
 */
open class WebSocketClient(private val config: WebSocketClientConfig) {
    private val logger = LoggerFactory.getLogger("WebSocketClient")
    private var socket: Socket? = null
    private var webSocket: WebSocket? = null
    private var state = WebSocketState.CLOSED
    private var connectedSubprotocol: String? = null
    private var reconnectAttempt = 0
    private var shouldReconnect = false
    private var reconnectJob: Job? = null
    private val _connectionState = MutableStateFlow(WebSocketConnectionState.DISCONNECTED)
    val connectionState: StateFlow<WebSocketConnectionState> = _connectionState.asStateFlow()
    val messages: Flow<WebSocketMessage> get() = webSocket?.messages ?: throw WebSocketException("Not connected")

    suspend fun connect(enableAutoReconnect: Boolean = true) {
        if (state != WebSocketState.CLOSED) throw WebSocketException("Already connected or connecting")
        shouldReconnect = enableAutoReconnect; reconnectAttempt = 0
        _connectionState.value = WebSocketConnectionState.CONNECTING
        performConnection()
    }

    private suspend fun performConnection() {
        state = WebSocketState.CONNECTING
        logger.i { "Connecting to ${config.url} (attempt ${reconnectAttempt + 1})" }
        try {
            val url = config.parsedUrl
            socket = withTimeout(config.connectTimeout) { Socket.connect(url.host, url.port, config.socketConfig) }
            performHandshake()
            socket!!.setReadTimeout(config.socketReadTimeout)
            webSocket = WebSocket(socket!!, isServer = false, fragmentTimeout = config.fragmentTimeout, maxMessageSize = config.maxMessageSize)
            state = WebSocketState.OPEN; reconnectAttempt = 0
            _connectionState.value = WebSocketConnectionState.CONNECTED
            logger.i { "Connected to ${config.url}" }
        } catch (e: Exception) {
            state = WebSocketState.CLOSED; socket?.close(); socket = null
            _connectionState.value = WebSocketConnectionState.DISCONNECTED
            if (shouldReconnect && reconnectAttempt < config.reconnectConfig.maxRetries) {
                reconnectAttempt++
                logger.w { "Connection failed, will retry (${reconnectAttempt}/${config.reconnectConfig.maxRetries}): ${e.message}" }
            } else { logger.e({ "Connection failed: ${e.message}" }, e); throw WebSocketException("Connection failed: ${e.message}", e) }
        }
    }

    fun start(scope: CoroutineScope) {
        if (state != WebSocketState.OPEN) throw WebSocketException("Not connected")
        webSocket?.start(scope)
        scope.launch {
            try { while (state == WebSocketState.OPEN) delay(1000)
                if (shouldReconnect && state == WebSocketState.CLOSED) { reconnectAttempt = 0; startReconnection(scope) }
            } catch (_: CancellationException) { }
        }
    }

    private fun startReconnection(scope: CoroutineScope) {
        reconnectJob?.cancel()
        if (!shouldReconnect || reconnectAttempt >= config.reconnectConfig.maxRetries) { _connectionState.value = WebSocketConnectionState.FAILED; return }
        _connectionState.value = WebSocketConnectionState.RECONNECTING
        reconnectJob = scope.launch {
            delay(calculateReconnectDelay(reconnectAttempt))
            try { performConnection(); if (state == WebSocketState.OPEN) webSocket?.start(scope) }
            catch (e: Exception) { if (shouldReconnect && reconnectAttempt < config.reconnectConfig.maxRetries) startReconnection(scope) else _connectionState.value = WebSocketConnectionState.FAILED }
        }
    }

    private fun calculateReconnectDelay(attempt: Int): Long {
        val cappedDelay = min((config.reconnectConfig.baseDelayMs * 2.0.pow(attempt)).toLong(), config.reconnectConfig.maxDelayMs)
        return cappedDelay + (cappedDelay * kotlin.random.Random.nextDouble() * 0.5).toLong()
    }

    open suspend fun sendText(text: String) { checkConnected(); webSocket?.sendText(text) }
    open suspend fun sendBinary(data: ByteArray) { checkConnected(); webSocket?.sendBinary(data) }
    open suspend fun sendPing(payload: ByteArray = byteArrayOf()) { checkConnected(); webSocket?.sendPing(payload) }

    suspend fun close(code: WebSocketCloseCode = WebSocketCloseCode.NORMAL, reason: String = "") {
        if (state == WebSocketState.CLOSED) return
        shouldReconnect = false; reconnectJob?.cancel(); reconnectJob = null
        webSocket?.close(code, reason); socket?.close()
        state = WebSocketState.CLOSED; _connectionState.value = WebSocketConnectionState.DISCONNECTED
    }

    fun isConnected() = state == WebSocketState.OPEN
    fun getSubprotocol() = connectedSubprotocol

    private suspend fun performHandshake() {
        val key = WebSocketClientHandshake.generateKey()
        val request = WebSocketClientHandshake.createHandshakeRequest(config, key)
        withTimeout(config.handshakeTimeout) {
            socket!!.sink().apply { writeUtf8(request); flush() }
            val response = readHandshakeResponse()
            when (val result = WebSocketClientHandshake.validateHandshakeResponse(response, key)) {
                is HandshakeResult.Success -> connectedSubprotocol = result.subprotocol
                is HandshakeResult.Error -> throw WebSocketException("Handshake failed: ${result.message}")
            }
        }
    }

    private suspend fun readHandshakeResponse(): String {
        val source = socket!!.source()
        val buffer = Buffer()
        val endMarker = byteArrayOf(13, 10, 13, 10)
        var foundEnd = false
        while (!foundEnd && buffer.size < 8192) {
            if (source.buffer.size == 0L) source.require(1)
            val toRead = minOf(512L, source.buffer.size, 8192 - buffer.size)
            if (toRead <= 0) break
            val chunk = source.readByteArray(toRead)
            buffer.write(chunk)
            if (buffer.size >= 4) {
                val bytes = buffer.snapshot().toByteArray()
                val startScan = maxOf(0, bytes.size - chunk.size - 3)
                for (i in startScan until bytes.size - 3) {
                    if (bytes[i] == endMarker[0] && bytes[i+1] == endMarker[1] && bytes[i+2] == endMarker[2] && bytes[i+3] == endMarker[3]) { foundEnd = true; break }
                }
            }
        }
        if (!foundEnd) throw WebSocketException("Handshake response too large or malformed")
        return buffer.readUtf8()
    }

    private fun checkConnected() { if (state != WebSocketState.OPEN) throw WebSocketException("Not connected") }
}
