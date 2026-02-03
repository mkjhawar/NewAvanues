package com.augmentalis.websocket

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import io.github.aakira.napier.Napier
import platform.Foundation.*
import platform.darwin.NSObject

private const val TAG = "iOSWebSocket"

/**
 * iOS WebSocket client implementation using NSURLSessionWebSocketTask
 */
class IOSWebSocketClient(
    private val config: WebSocketClientConfig
) : WebSocketClient {

    private var webSocketTask: NSURLSessionWebSocketTask? = null
    private var urlSession: NSURLSession? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

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
        return withContext(Dispatchers.Default) {
            try {
                currentUrl = url
                currentHeaders = headers

                _connectionState.value = ConnectionState.Connecting

                val nsUrl = NSURL.URLWithString(url)
                    ?: return@withContext Result.failure(IllegalArgumentException("Invalid URL: $url"))

                val request = NSMutableURLRequest.requestWithURL(nsUrl).apply {
                    setTimeoutInterval(config.connectionTimeoutMs / 1000.0)
                    headers.forEach { (key, value) ->
                        setValue(value, forHTTPHeaderField = key)
                    }
                }

                val sessionConfig = NSURLSessionConfiguration.defaultSessionConfiguration
                urlSession = NSURLSession.sessionWithConfiguration(
                    sessionConfig,
                    delegate = null,
                    delegateQueue = NSOperationQueue.mainQueue
                )

                webSocketTask = urlSession?.webSocketTaskWithRequest(request)
                webSocketTask?.resume()

                // Start receiving messages
                startReceiving()

                val sessionId = "session_${NSDate().timeIntervalSince1970.toLong()}"
                _connectionState.value = ConnectionState.Connected(sessionId)

                // Start keep-alive (AVU format)
                keepAliveManager.start(
                    sendPing = { send(AvuSyncMessage.ping(sessionId)) },
                    onTimeout = {
                        scope.launch { handleDisconnect("Ping timeout") }
                    }
                )

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
        webSocketTask?.cancelWithCloseCode(code.toLong(), reason.encodeToByteArray().toNSData())
        webSocketTask = null
        urlSession?.invalidateAndCancel()
        urlSession = null
        _connectionState.value = ConnectionState.Disconnected
    }

    override suspend fun send(message: String): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val task = webSocketTask
                if (task == null || _connectionState.value !is ConnectionState.Connected) {
                    return@withContext Result.failure(IllegalStateException("Not connected"))
                }

                val wsMessage = NSURLSessionWebSocketMessage(message)

                suspendCancellableCoroutine<Result<Unit>> { continuation ->
                    task.sendMessage(wsMessage) { error ->
                        if (error != null) {
                            Napier.e("Send failed: ${error.localizedDescription}", tag = TAG)
                            continuation.resume(Result.failure(Exception(error.localizedDescription)))
                        } else {
                            if (config.debugLogging) {
                                Napier.d("Sent message: ${message.take(100)}...", tag = TAG)
                            }
                            continuation.resume(Result.success(Unit))
                        }
                    }
                }
            } catch (e: Exception) {
                Napier.e("Send failed: ${e.message}", e, tag = TAG)
                Result.failure(e)
            }
        }
    }

    override suspend fun sendBinary(data: ByteArray): Result<Unit> {
        return withContext(Dispatchers.Default) {
            try {
                val task = webSocketTask
                if (task == null || _connectionState.value !is ConnectionState.Connected) {
                    return@withContext Result.failure(IllegalStateException("Not connected"))
                }

                val wsMessage = NSURLSessionWebSocketMessage(data.toNSData())

                suspendCancellableCoroutine<Result<Unit>> { continuation ->
                    task.sendMessage(wsMessage) { error ->
                        if (error != null) {
                            continuation.resume(Result.failure(Exception(error.localizedDescription)))
                        } else {
                            continuation.resume(Result.success(Unit))
                        }
                    }
                }
            } catch (e: Exception) {
                Napier.e("Binary send failed: ${e.message}", e, tag = TAG)
                Result.failure(e)
            }
        }
    }

    override fun isConnected(): Boolean = _connectionState.value is ConnectionState.Connected

    private fun startReceiving() {
        scope.launch {
            while (isActive && webSocketTask != null) {
                receiveMessage()
            }
        }
    }

    private suspend fun receiveMessage() {
        val task = webSocketTask ?: return

        suspendCancellableCoroutine<Unit> { continuation ->
            task.receiveMessageWithCompletionHandler { message, error ->
                if (error != null) {
                    Napier.e("Receive error: ${error.localizedDescription}", tag = TAG)
                    scope.launch { handleDisconnect("Receive error: ${error.localizedDescription}") }
                    continuation.resume(Unit)
                    return@receiveMessageWithCompletionHandler
                }

                message?.let { msg ->
                    when (msg.type) {
                        NSURLSessionWebSocketMessageTypeString -> {
                            msg.string?.let { text ->
                                if (config.debugLogging) {
                                    Napier.d("Received: ${text.take(100)}...", tag = TAG)
                                }

                                // Handle pong (AVU format)
                                AvuSyncMessage.parse(text)?.let { syncMsg ->
                                    if (syncMsg is ParsedAvuMessage.Pong) {
                                        keepAliveManager.onPongReceived()
                                        continuation.resume(Unit)
                                        return@receiveMessageWithCompletionHandler
                                    }
                                }

                                scope.launch { _incomingMessages.emit(text) }
                            }
                        }
                        NSURLSessionWebSocketMessageTypeData -> {
                            msg.data?.let { data ->
                                scope.launch { _incomingBinaryMessages.emit(data.toByteArray()) }
                            }
                        }
                    }
                }
                continuation.resume(Unit)
            }
        }
    }

    private suspend fun handleDisconnect(reason: String) {
        keepAliveManager.stop()
        webSocketTask = null

        if (config.autoReconnect && currentUrl != null) {
            reconnectionManager.startReconnection {
                connect(currentUrl!!, currentHeaders).isSuccess
            }
        } else {
            _connectionState.value = ConnectionState.Disconnected
        }
    }
}

// Extension functions for NSData conversion
private fun ByteArray.toNSData(): NSData {
    return this.usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), this.size.toULong())
    }
}

private fun NSData.toByteArray(): ByteArray {
    return ByteArray(this.length.toInt()).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
        }
    }
}

/**
 * Factory function for iOS
 */
actual fun createWebSocketClient(config: WebSocketClientConfig): WebSocketClient {
    return IOSWebSocketClient(config)
}

/**
 * Platform name for iOS
 */
actual fun getPlatformName(): String = "iOS"

/**
 * Current time in milliseconds
 */
actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
