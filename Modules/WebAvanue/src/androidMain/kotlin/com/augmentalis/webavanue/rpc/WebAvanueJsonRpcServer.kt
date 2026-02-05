/**
 * WebAvanueJsonRpcServer.kt - JSON-RPC server for WebAvanue (Android)
 *
 * Simple HTTP/JSON-based RPC server that doesn't require protobuf.
 * Uses Ktor or raw sockets for transport.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.webavanue.rpc

import android.util.Log
import com.augmentalis.webavanue.rpc.messages.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

private const val TAG = "WebAvanueJsonRpc"

/**
 * Simple JSON-RPC server for WebAvanue
 *
 * Protocol:
 * - Request: {"method": "getTabs", "params": {...}, "id": "req-1"}
 * - Response: {"result": {...}, "id": "req-1"} or {"error": {...}, "id": "req-1"}
 */
class WebAvanueJsonRpcServer(
    private val delegate: IWebAvanueServiceDelegate,
    private val config: WebAvanueServerConfig = WebAvanueServerConfig()
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        isLenient = true
    }

    /**
     * Start the JSON-RPC server
     */
    fun start() {
        if (isRunning) {
            Log.w(TAG, "Server already running")
            return
        }

        scope.launch {
            try {
                serverSocket = ServerSocket(config.port)
                isRunning = true
                Log.i(TAG, "WebAvanue JSON-RPC server started on port ${config.port}")

                while (isRunning) {
                    try {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch { handleClient(clientSocket) }
                    } catch (e: Exception) {
                        if (isRunning) {
                            Log.e(TAG, "Error accepting connection", e)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Server error", e)
            }
        }
    }

    /**
     * Stop the server
     */
    fun stop() {
        isRunning = false
        scope.cancel()
        serverSocket?.close()
        serverSocket = null
        Log.i(TAG, "WebAvanue JSON-RPC server stopped")
    }

    /**
     * Check if server is running
     */
    fun isRunning(): Boolean = isRunning

    /**
     * Get server port
     */
    fun getPort(): Int = config.port

    private suspend fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            // Read request line by line until we get a complete JSON
            val requestBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line.isNullOrBlank()) break
                requestBuilder.append(line)
            }

            val request = requestBuilder.toString()
            if (request.isNotEmpty()) {
                val response = processRequest(request)
                writer.println(response)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        } finally {
            socket.close()
        }
    }

    private suspend fun processRequest(requestJson: String): String {
        return try {
            val request = json.decodeFromString<JsonRpcRequest>(requestJson)
            val result = when (request.method) {
                "getTabs" -> handleGetTabs(request)
                "createTab" -> handleCreateTab(request)
                "closeTab" -> handleCloseTab(request)
                "switchTab" -> handleSwitchTab(request)
                "navigate" -> handleNavigate(request)
                "goBack" -> handleGoBack(request)
                "goForward" -> handleGoForward(request)
                "reload" -> handleReload(request)
                "scroll" -> handleScroll(request)
                "clickElement" -> handleClickElement(request)
                "typeText" -> handleTypeText(request)
                "findElements" -> handleFindElements(request)
                "getPageContent" -> handleGetPageContent(request)
                "executeVoiceCommand" -> handleVoiceCommand(request)
                else -> JsonRpcResponse(
                    id = request.id,
                    error = JsonRpcError(-32601, "Method not found: ${request.method}")
                )
            }
            json.encodeToString(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error processing request", e)
            json.encodeToString(
                JsonRpcResponse(
                    id = "unknown",
                    error = JsonRpcError(-32700, "Parse error: ${e.message}")
                )
            )
        }
    }

    // Handler methods

    private suspend fun handleGetTabs(request: JsonRpcRequest): JsonRpcResponse {
        val tabs = delegate.getTabs()
        val activeTabId = delegate.getActiveTabId()
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(GetTabsResponse(request.id, tabs, activeTabId))
        )
    }

    private suspend fun handleCreateTab(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<CreateTabRequest>(request.params ?: "{}")
        val tab = delegate.createTab(params.url, params.makeActive)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(CreateTabResponse(request.id, tab != null, tab))
        )
    }

    private suspend fun handleCloseTab(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<CloseTabRequest>(request.params ?: "{}")
        val success = delegate.closeTab(params.tabId)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(WebAvanueResponse(request.id, success))
        )
    }

    private suspend fun handleSwitchTab(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<SwitchTabRequest>(request.params ?: "{}")
        val success = delegate.switchTab(params.tabId)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(WebAvanueResponse(request.id, success))
        )
    }

    private suspend fun handleNavigate(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<NavigateRequest>(request.params ?: "{}")
        val success = delegate.navigate(params.tabId, params.url)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(NavigationResponse(request.id, success, params.url))
        )
    }

    private suspend fun handleGoBack(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<GoBackRequest>(request.params ?: "{}")
        val success = delegate.goBack(params.tabId)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(NavigationResponse(request.id, success))
        )
    }

    private suspend fun handleGoForward(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<GoForwardRequest>(request.params ?: "{}")
        val success = delegate.goForward(params.tabId)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(NavigationResponse(request.id, success))
        )
    }

    private suspend fun handleReload(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<ReloadRequest>(request.params ?: "{}")
        val success = delegate.reload(params.tabId, params.hardReload)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(NavigationResponse(request.id, success))
        )
    }

    private suspend fun handleScroll(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<ScrollRequest>(request.params ?: "{}")
        val success = delegate.scroll(params.tabId, params.direction, params.amount)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(WebAvanueResponse(request.id, success))
        )
    }

    private suspend fun handleClickElement(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<ClickElementRequest>(request.params ?: "{}")
        val success = delegate.clickElement(params.tabId, params.selector)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(WebAvanueResponse(request.id, success))
        )
    }

    private suspend fun handleTypeText(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<TypeTextRequest>(request.params ?: "{}")
        val success = delegate.typeText(params.tabId, params.selector, params.text, params.clearFirst)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(WebAvanueResponse(request.id, success))
        )
    }

    private suspend fun handleFindElements(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<FindElementRequest>(request.params ?: "{}")
        val elements = delegate.findElements(params.tabId, params.selector, params.includeHidden)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(FindElementResponse(request.id, elements))
        )
    }

    private suspend fun handleGetPageContent(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<GetPageContentRequest>(request.params ?: "{}")
        val content = delegate.getPageContent(params.tabId, params.includeHtml, params.includeText)
        return JsonRpcResponse(
            id = request.id,
            result = if (content != null) json.encodeToString(content) else null,
            error = if (content == null) JsonRpcError(-32000, "Tab not found") else null
        )
    }

    private suspend fun handleVoiceCommand(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<VoiceCommandRequest>(request.params ?: "{}")
        val response = delegate.executeVoiceCommand(params.command, params.tabId, params.params)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(response)
        )
    }
}

/**
 * JSON-RPC request format
 */
@kotlinx.serialization.Serializable
data class JsonRpcRequest(
    val jsonrpc: String = "2.0",
    val method: String,
    val params: String? = null,
    val id: String
)

/**
 * JSON-RPC response format
 */
@kotlinx.serialization.Serializable
data class JsonRpcResponse(
    val jsonrpc: String = "2.0",
    val result: String? = null,
    val error: JsonRpcError? = null,
    val id: String
)

/**
 * JSON-RPC error format
 */
@kotlinx.serialization.Serializable
data class JsonRpcError(
    val code: Int,
    val message: String,
    val data: String? = null
)
