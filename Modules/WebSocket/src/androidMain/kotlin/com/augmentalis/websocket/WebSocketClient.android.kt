package com.augmentalis.websocket

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import okhttp3.*
import io.github.aakira.napier.Napier
import java.util.concurrent.TimeUnit

private const val TAG = "AndroidWebSocket"

/**
 * Android WebSocket client implementation using OkHttp
 */
class AndroidWebSocketClient(
    private val config: WebSocketClientConfig
) : WebSocketClient {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(config.connectionTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
            .writeTimeout(config.writeTimeoutMs, TimeUnit.MILLISECONDS)
            .pingInterval(config.pingIntervalMs, TimeUnit.MILLISECONDS)
            .build()
    }

    private var webSocket: WebSocket? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val reconnectionManager = ReconnectionManager(config, scope)
    private val keepAliveManager = KeepAliveManager(config, scope)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _incomingMessages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 100
    )
    override val incomingMessages: Flow<String> = _incomingMessages.asSharedFlow()

    private val _incomingBinaryMessages = MutableSharedFlow<ByteArray>(
        replay = 0,
        extraBufferCapacity = 100
    )
    override val incomingBinaryMessages: Flow<ByteArray> = _incomingBinaryMessages.asSharedFlow()

    private var currentUrl: String? = null
    private var currentHeaders: Map<String, String> = emptyMap()

    override suspend fun connect(url: String, headers: Map<String, String>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                currentUrl = url
                currentHeaders = headers

                _connectionState.value = ConnectionState.Connecting

                val requestBuilder = Request.Builder().url(url)
                headers.forEach { (key, value) ->
                    requestBuilder.addHeader(key, value)
                }

                val request = requestBuilder.build()

                val listener = object : WebSocketListener() {
                    override fun onOpen(webSocket: WebSocket, response: Response) {
                        if (config.debugLogging) {
                            Napier.d("WebSocket opened: ${response.code}", tag = TAG)
                        }
                        val sessionId = response.header("X-Session-Id") ?: "session_${System.currentTimeMillis()}"
                        _connectionState.value = ConnectionState.Connected(sessionId)

                        // Start keep-alive (AVU format)
                        keepAliveManager.start(
                            sendPing = { webSocket.send(AvuSyncMessage.ping(sessionId)) },
                            onTimeout = {
                                scope.launch { handleDisconnect("Ping timeout") }
                            }
                        )
                    }

                    override fun onMessage(webSocket: WebSocket, text: String) {
                        if (config.debugLogging) {
                            Napier.d("Received text message: ${text.take(100)}...", tag = TAG)
                        }

                        // Handle pong for keep-alive (AVU format)
                        AvuSyncMessage.parse(text)?.let { message ->
                            if (message is ParsedAvuMessage.Pong) {
                                keepAliveManager.onPongReceived()
                                return
                            }
                        }

                        scope.launch {
                            _incomingMessages.emit(text)
                        }
                    }

                    override fun onMessage(webSocket: WebSocket, bytes: okio.ByteString) {
                        if (config.debugLogging) {
                            Napier.d("Received binary message: ${bytes.size} bytes", tag = TAG)
                        }
                        scope.launch {
                            _incomingBinaryMessages.emit(bytes.toByteArray())
                        }
                    }

                    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                        if (config.debugLogging) {
                            Napier.d("WebSocket closing: $code $reason", tag = TAG)
                        }
                        webSocket.close(code, reason)
                    }

                    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                        if (config.debugLogging) {
                            Napier.d("WebSocket closed: $code $reason", tag = TAG)
                        }
                        scope.launch {
                            handleDisconnect("Closed: $code $reason")
                        }
                    }

                    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                        Napier.e("WebSocket failure: ${t.message}", t, tag = TAG)
                        _connectionState.value = ConnectionState.Error(
                            t.message ?: "Unknown error",
                            t
                        )
                        scope.launch {
                            handleDisconnect("Failure: ${t.message}")
                        }
                    }
                }

                webSocket = client.newWebSocket(request, listener)
                Result.success(Unit)
            } catch (e: Exception) {
                Napier.e("Connection failed: ${e.message}", e, tag = TAG)
                _connectionState.value = ConnectionState.Error(e.message ?: "Connection failed", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun disconnect(code: Int, reason: String) {
        keepAliveManager.stop()
        reconnectionManager.cancel()
        webSocket?.close(code, reason)
        webSocket = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun send(message: String): Result<Unit> {
        return try {
            val ws = webSocket
            if (ws == null || _connectionState.value !is ConnectionState.Connected) {
                return Result.failure(IllegalStateException("Not connected"))
            }

            val success = ws.send(message)
            if (success) {
                if (config.debugLogging) {
                    Napier.d("Sent message: ${message.take(100)}...", tag = TAG)
                }
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to send message"))
            }
        } catch (e: Exception) {
            Napier.e("Send failed: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override suspend fun sendBinary(data: ByteArray): Result<Unit> {
        return try {
            val ws = webSocket
            if (ws == null || _connectionState.value !is ConnectionState.Connected) {
                return Result.failure(IllegalStateException("Not connected"))
            }

            val success = ws.send(okio.ByteString.of(*data))
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Failed to send binary message"))
            }
        } catch (e: Exception) {
            Napier.e("Binary send failed: ${e.message}", e, tag = TAG)
            Result.failure(e)
        }
    }

    override fun isConnected(): Boolean = _connectionState.value is ConnectionState.Connected

    private suspend fun handleDisconnect(reason: String) {
        keepAliveManager.stop()
        webSocket = null

        if (config.autoReconnect && currentUrl != null) {
            reconnectionManager.startReconnection {
                connect(currentUrl!!, currentHeaders).isSuccess
            }
        } else {
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}

/**
 * Factory function for Android
 */
actual fun createWebSocketClient(config: WebSocketClientConfig): WebSocketClient {
    return AndroidWebSocketClient(config)
}

/**
 * Platform name for Android
 */
actual fun getPlatformName(): String = "Android"

/**
 * Current time in milliseconds
 */
actual fun currentTimeMillis(): Long = System.currentTimeMillis()
