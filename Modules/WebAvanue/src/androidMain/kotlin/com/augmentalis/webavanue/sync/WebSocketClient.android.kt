package com.augmentalis.webavanue.sync

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.*
import okio.ByteString
import java.util.concurrent.TimeUnit

private const val TAG = "AndroidWebSocket"

/**
 * Android implementation of WebSocket client using OkHttp
 */
class AndroidWebSocketClient(
    private val config: WebSocketConfig
) : WebSocketClient {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private var webSocket: WebSocket? = null
    private var messageCallback: ((WebSocketMessage) -> Unit)? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(config.connectionTimeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
        .pingInterval(config.pingIntervalMs, TimeUnit.MILLISECONDS)
        .build()

    override suspend fun connect(onMessage: (WebSocketMessage) -> Unit) {
        messageCallback = onMessage

        val request = Request.Builder()
            .url(config.serverUrl)
            .apply {
                config.authToken?.let { token ->
                    addHeader("Authorization", "Bearer $token")
                }
                addHeader("X-Device-Id", config.deviceId)
                config.userId?.let { userId ->
                    addHeader("X-User-Id", userId)
                }
            }
            .build()

        withContext(Dispatchers.IO) {
            webSocket = client.newWebSocket(request, object : WebSocketListener() {

                override fun onOpen(webSocket: WebSocket, response: Response) {
                    if (config.debugLogging) {
                        Log.d(TAG, "WebSocket connected")
                    }
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    try {
                        val message = json.decodeFromString<WebSocketMessage>(text)
                        messageCallback?.invoke(message)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse message: ${e.message}")
                    }
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    // Handle binary messages if needed
                    val text = bytes.utf8()
                    onMessage(webSocket, text)
                }

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    if (config.debugLogging) {
                        Log.d(TAG, "WebSocket closing: $code - $reason")
                    }
                    webSocket.close(1000, null)
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    if (config.debugLogging) {
                        Log.d(TAG, "WebSocket closed: $code - $reason")
                    }
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    Log.e(TAG, "WebSocket failure: ${t.message}", t)
                    // Connection failure will trigger reconnection logic in service
                }
            })
        }
    }

    override suspend fun send(message: WebSocketMessage) {
        withContext(Dispatchers.IO) {
            val messageJson = json.encodeToString(message)
            val success = webSocket?.send(messageJson) ?: false

            if (!success && config.debugLogging) {
                Log.w(TAG, "Failed to send message: ${message.type}")
            }
        }
    }

    override suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            webSocket?.close(1000, "Client disconnect")
            webSocket = null
            messageCallback = null
        }
    }
}

/**
 * Create Android WebSocket client
 */
actual fun createWebSocketClient(config: WebSocketConfig): WebSocketClient {
    return AndroidWebSocketClient(config)
}

/**
 * Get Android platform name
 */
actual fun getPlatformName(): String = "Android"
