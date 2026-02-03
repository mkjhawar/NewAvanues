package com.augmentalis.webavanue.sync

import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.Foundation.*
import platform.darwin.NSObject

/**
 * iOS implementation of WebSocket client using NSURLSessionWebSocketTask
 */
class IOSWebSocketClient(
    private val config: WebSocketConfig
) : WebSocketClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var webSocketTask: NSURLSessionWebSocketTask? = null
    private var session: NSURLSession? = null
    private var messageCallback: ((WebSocketMessage) -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override suspend fun connect(onMessage: (WebSocketMessage) -> Unit) {
        messageCallback = onMessage

        withContext(Dispatchers.Main) {
            val url = NSURL.URLWithString(config.serverUrl)
                ?: throw IllegalArgumentException("Invalid URL: ${config.serverUrl}")

            val request = NSMutableURLRequest.requestWithURL(url).apply {
                config.authToken?.let { token ->
                    setValue("Bearer $token", forHTTPHeaderField = "Authorization")
                }
                setValue(config.deviceId, forHTTPHeaderField = "X-Device-Id")
                config.userId?.let { userId ->
                    setValue(userId, forHTTPHeaderField = "X-User-Id")
                }
                setTimeoutInterval(config.connectionTimeoutMs / 1000.0)
            }

            val configuration = NSURLSessionConfiguration.defaultSessionConfiguration
            session = NSURLSession.sessionWithConfiguration(
                configuration = configuration,
                delegate = null,
                delegateQueue = NSOperationQueue.mainQueue
            )

            webSocketTask = session?.webSocketTaskWithRequest(request)
            webSocketTask?.resume()

            // Start receiving messages
            receiveNextMessage()
        }
    }

    private fun receiveNextMessage() {
        webSocketTask?.receiveMessageWithCompletionHandler { message, error ->
            if (error != null) {
                if (config.debugLogging) {
                    println("WebSocket error: ${error.localizedDescription}")
                }
                return@receiveMessageWithCompletionHandler
            }

            message?.let { msg ->
                when (msg.type) {
                    NSURLSessionWebSocketMessageTypeString -> {
                        val text = msg.string ?: return@let
                        try {
                            val wsMessage = json.decodeFromString<WebSocketMessage>(text)
                            messageCallback?.invoke(wsMessage)
                        } catch (e: Exception) {
                            println("Failed to parse message: ${e.message}")
                        }
                    }
                    NSURLSessionWebSocketMessageTypeData -> {
                        val data = msg.data ?: return@let
                        val text = NSString.create(data, NSUTF8StringEncoding) as? String ?: return@let
                        try {
                            val wsMessage = json.decodeFromString<WebSocketMessage>(text)
                            messageCallback?.invoke(wsMessage)
                        } catch (e: Exception) {
                            println("Failed to parse message: ${e.message}")
                        }
                    }
                    else -> {}
                }
            }

            // Continue receiving
            receiveNextMessage()
        }
    }

    override suspend fun send(message: WebSocketMessage) {
        withContext(Dispatchers.Main) {
            val messageJson = json.encodeToString(message)
            val wsMessage = NSURLSessionWebSocketMessage(messageJson)
            webSocketTask?.sendMessage(wsMessage) { error ->
                if (error != null && config.debugLogging) {
                    println("Failed to send message: ${error.localizedDescription}")
                }
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.Main) {
            webSocketTask?.cancelWithCloseCode(
                closeCode = 1000,
                reason = "Client disconnect".encodeToByteArray().toNSData()
            )
            webSocketTask = null
            session?.invalidateAndCancel()
            session = null
            messageCallback = null
            scope.cancel()
        }
    }
}

/**
 * Extension to convert ByteArray to NSData
 */
private fun ByteArray.toNSData(): NSData {
    return NSData.create(bytes = this.toUByteArray().toCValues(), length = this.size.toULong())
}

/**
 * Create iOS WebSocket client
 */
actual fun createWebSocketClient(config: WebSocketConfig): WebSocketClient {
    return IOSWebSocketClient(config)
}

/**
 * Get iOS platform name
 */
actual fun getPlatformName(): String = "iOS"
