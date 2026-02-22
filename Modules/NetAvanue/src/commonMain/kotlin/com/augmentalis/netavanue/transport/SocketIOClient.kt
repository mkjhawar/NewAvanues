package com.augmentalis.netavanue.transport

import com.avanues.logging.LoggerFactory
import com.augmentalis.httpavanue.websocket.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import kotlin.concurrent.Volatile

/**
 * Minimal Socket.IO v4 client built on HTTPAvanue's WebSocket.
 *
 * Handles the Engine.IO transport layer (handshake, ping/pong keepalive)
 * and Socket.IO protocol (namespace connect, event emit/receive, acks).
 *
 * Only supports WebSocket transport (no HTTP long-polling fallback) since
 * all Avanues clients have WebSocket support.
 */
class SocketIOClient(
    private val serverUrl: String,
    private val namespace: String = "/signaling",
    private val auth: JsonObject? = null,
    private val reconnectConfig: WebSocketReconnectConfig = WebSocketReconnectConfig(),
) {
    private val logger = LoggerFactory.getLogger("SocketIOClient")

    /** Connection state visible to consumers */
    sealed class State {
        data object Disconnected : State()
        data object Connecting : State()
        data object Connected : State()
        data class Error(val message: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Disconnected)
    val state: StateFlow<State> = _state.asStateFlow()

    private val _events = MutableSharedFlow<Pair<String, JsonElement>>(extraBufferCapacity = 64)
    /** Incoming events as (eventName, payload) pairs */
    val events: SharedFlow<Pair<String, JsonElement>> = _events.asSharedFlow()

    private var wsClient: WebSocketClient? = null
    private var scope: CoroutineScope? = null
    private var pingJob: Job? = null
    private var handshake: EioHandshake? = null
    private var ackCounter = 0
    private val ackMutex = Mutex()
    private val pendingAcks = mutableMapOf<Int, CompletableDeferred<JsonElement?>>()
    @Volatile private var connected = false

    /**
     * Connect to the Socket.IO server. The URL should be the base server URL
     * (e.g. "wss://api.avanues.com"). The Engine.IO path `/socket.io/` and
     * query parameters are appended automatically.
     */
    suspend fun connect(parentScope: CoroutineScope) {
        if (connected) return
        _state.value = State.Connecting
        scope = CoroutineScope(parentScope.coroutineContext + SupervisorJob())

        val wsUrl = buildWsUrl()
        logger.i { "Connecting to $wsUrl (namespace=$namespace)" }

        val config = WebSocketClientConfig(
            url = wsUrl,
            connectTimeout = 15_000,
            handshakeTimeout = 10_000,
            socketReadTimeout = 0, // We handle our own keepalive
            reconnectConfig = reconnectConfig,
        )

        wsClient = WebSocketClient(config)

        try {
            wsClient!!.connect(enableAutoReconnect = true)
            wsClient!!.start(scope!!)
            scope!!.launch { processMessages() }
        } catch (e: Exception) {
            logger.e({ "Connection failed: ${e.message}" }, e)
            _state.value = State.Error(e.message ?: "Connection failed")
            throw e
        }
    }

    /** Disconnect gracefully */
    suspend fun disconnect() {
        connected = false
        pingJob?.cancel()
        wsClient?.close()
        wsClient = null
        scope?.cancel()
        scope = null
        _state.value = State.Disconnected
        logger.i { "Disconnected from Socket.IO" }
    }

    /**
     * Emit an event to the server (fire-and-forget).
     */
    suspend fun emit(eventName: String, payload: JsonElement) {
        if (!connected) throw IllegalStateException("Not connected to Socket.IO")
        val frame = SocketIOCodec.encodeSioEvent(namespace, eventName, payload)
        wsClient?.sendText(frame)
    }

    /**
     * Emit an event and wait for the server's acknowledgement response.
     * Returns the response data, or null if the server responds with no data.
     */
    suspend fun emitWithAck(
        eventName: String,
        payload: JsonElement,
        timeout: Long = 10_000,
    ): JsonElement? {
        if (!connected) throw IllegalStateException("Not connected to Socket.IO")
        val deferred = CompletableDeferred<JsonElement?>()
        val ackId = ackMutex.withLock {
            val id = ackCounter++
            pendingAcks[id] = deferred
            id
        }

        val frame = SocketIOCodec.encodeSioEvent(namespace, eventName, payload, ackId)
        wsClient?.sendText(frame)

        return try {
            withTimeout(timeout) { deferred.await() }
        } catch (e: TimeoutCancellationException) {
            ackMutex.withLock { pendingAcks.remove(ackId) }
            throw SocketIOTimeoutException("Ack timeout for event '$eventName' (id=$ackId)")
        }
    }

    /** Build the WebSocket URL with Engine.IO query params */
    private fun buildWsUrl(): String {
        val base = serverUrl.trimEnd('/')
        val scheme = if (base.startsWith("https://") || base.startsWith("wss://")) "wss" else "ws"
        val host = base.removePrefix("https://").removePrefix("http://")
            .removePrefix("wss://").removePrefix("ws://")
        return "$scheme://$host/socket.io/?EIO=4&transport=websocket"
    }

    /** Main message processing loop */
    private suspend fun processMessages() {
        try {
            wsClient?.messages?.collect { message ->
                when (message) {
                    is WebSocketMessage.Text -> handleRawMessage(message.data)
                    is WebSocketMessage.Close -> {
                        logger.w { "WebSocket closed: ${message.code} ${message.reason}" }
                        connected = false
                        _state.value = State.Disconnected
                    }
                    is WebSocketMessage.Binary -> { /* ignore binary for signaling */ }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e({ "Message processing error: ${e.message}" }, e)
            connected = false
            _state.value = State.Error(e.message ?: "Processing error")
        }
    }

    /** Handle a raw text frame from the WebSocket */
    private suspend fun handleRawMessage(raw: String) {
        if (raw.isEmpty()) return
        val eioType = EioType.fromCode(raw[0]) ?: return

        when (eioType) {
            EioType.OPEN -> handleEioOpen(raw)
            EioType.PING -> wsClient?.sendText(EioType.PONG.code.toString())
            EioType.PONG -> { /* ignore server pong */ }
            EioType.CLOSE -> {
                connected = false
                _state.value = State.Disconnected
            }
            EioType.MESSAGE -> handleSioMessage(raw.substring(1))
            else -> { /* UPGRADE, NOOP — ignore */ }
        }
    }

    /** Handle Engine.IO open: parse handshake, start keepalive, connect to namespace */
    private suspend fun handleEioOpen(raw: String) {
        handshake = SocketIOCodec.parseEioOpen(raw)
        logger.i { "EIO handshake: sid=${handshake!!.sid}, ping=${handshake!!.pingInterval}ms" }
        startPingLoop(handshake!!.pingInterval)
        connectToNamespace()
    }

    /** Send Socket.IO CONNECT to the target namespace */
    private suspend fun connectToNamespace() {
        val frame = SocketIOCodec.encodeSioConnect(namespace, auth)
        wsClient?.sendText(frame)
    }

    /** Start the Engine.IO ping keepalive loop */
    private fun startPingLoop(intervalMs: Long) {
        pingJob?.cancel()
        pingJob = scope?.launch {
            while (isActive) {
                delay(intervalMs)
                try {
                    wsClient?.sendText(EioType.PING.code.toString())
                } catch (_: Exception) {
                    break
                }
            }
        }
    }

    /** Handle a Socket.IO packet (EIO MESSAGE prefix already stripped) */
    private suspend fun handleSioMessage(raw: String) {
        val packet = SocketIOCodec.parseSioPacket(raw)
        if (packet.namespace != namespace && packet.namespace != "/") return

        when (packet.type) {
            SioType.CONNECT -> {
                connected = true
                _state.value = State.Connected
                logger.i { "Connected to namespace $namespace" }
            }
            SioType.DISCONNECT -> {
                connected = false
                _state.value = State.Disconnected
                logger.w { "Disconnected from namespace $namespace" }
            }
            SioType.EVENT -> handleSioEvent(packet)
            SioType.ACK -> handleSioAck(packet)
            SioType.CONNECT_ERROR -> {
                val msg = packet.data?.jsonObject?.get("message")?.jsonPrimitive?.content ?: "Unknown"
                logger.e { "Socket.IO connect error: $msg" }
                _state.value = State.Error(msg)
            }
            else -> { /* BINARY_EVENT, BINARY_ACK — not used for signaling */ }
        }
    }

    /** Handle an incoming Socket.IO event */
    private fun handleSioEvent(packet: SioPacket) {
        val arr = packet.data?.jsonArray ?: return
        if (arr.isEmpty()) return
        val eventName = arr[0].jsonPrimitive.contentOrNull ?: return
        val payload = arr.getOrNull(1) ?: JsonNull
        _events.tryEmit(eventName to payload)
    }

    /** Handle a Socket.IO acknowledgement response */
    private suspend fun handleSioAck(packet: SioPacket) {
        val ackId = packet.ackId ?: return
        val deferred = ackMutex.withLock { pendingAcks.remove(ackId) } ?: return
        val data = packet.data?.let { element ->
            when (element) {
                is JsonArray -> element.firstOrNull()
                else -> element
            }
        }
        deferred.complete(data)
    }
}

class SocketIOTimeoutException(message: String) : Exception(message)
