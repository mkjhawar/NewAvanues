/**
 * VoiceOSJsonRpcServer.kt - JSON-RPC server for VoiceOS (Android)
 *
 * Simple HTTP/JSON-based RPC server that doesn't require protobuf.
 * Uses raw sockets for transport with AVU 2.1 protocol support.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.rpc

import android.util.Log
import com.augmentalis.voiceoscore.rpc.messages.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

private const val TAG = "VoiceOSJsonRpc"

/**
 * Simple JSON-RPC server for VoiceOS
 *
 * Protocol:
 * - Request: {"method": "executeCommand", "params": {...}, "id": "req-1"}
 * - Response: {"result": {...}, "id": "req-1"} or {"error": {...}, "id": "req-1"}
 */
class VoiceOSJsonRpcServer(
    private val delegate: IVoiceOSServiceDelegate,
    private val config: VoiceOSServerConfig = VoiceOSServerConfig()
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
                Log.i(TAG, "VoiceOS JSON-RPC server started on port ${config.port}")

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
        Log.i(TAG, "VoiceOS JSON-RPC server stopped")
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
                "getStatus" -> handleGetStatus(request)
                "executeCommand" -> handleExecuteCommand(request)
                "executeAction" -> handleExecuteAction(request)
                "scrapeScreen" -> handleScrapeScreen(request)
                "startRecognition" -> handleStartRecognition(request)
                "stopRecognition" -> handleStopRecognition(request)
                "learnApp" -> handleLearnApp(request)
                "getLearnedApps" -> handleGetLearnedApps(request)
                "getCommands" -> handleGetCommands(request)
                "registerCommand" -> handleRegisterCommand(request)
                "unregisterCommand" -> handleUnregisterCommand(request)
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

    private suspend fun handleGetStatus(request: JsonRpcRequest): JsonRpcResponse {
        val status = delegate.getServiceStatus()
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(status)
        )
    }

    private suspend fun handleExecuteCommand(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<VoiceCommandRequest>(request.params ?: "{}")
        val response = delegate.executeVoiceCommand(params.commandText, params.context)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(response)
        )
    }

    private suspend fun handleExecuteAction(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<AccessibilityActionRequest>(request.params ?: "{}")
        val success = delegate.performAction(params.actionType, params.targetAvid, params.params)
        val actionResult = delegate.getActionResult()
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(
                AccessibilityActionResponse(request.id, success, actionResult)
            )
        )
    }

    private suspend fun handleScrapeScreen(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<ScrapeScreenRequest>(request.params ?: "{}")
        val response = delegate.scrapeCurrentScreen(params.includeInvisible, params.maxDepth)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(response)
        )
    }

    private suspend fun handleStartRecognition(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<StartRecognitionRequest>(request.params ?: "{}")
        val success = delegate.startVoiceRecognition(params.language, params.continuous)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(VoiceOSResponse(request.id, success))
        )
    }

    private suspend fun handleStopRecognition(request: JsonRpcRequest): JsonRpcResponse {
        val success = delegate.stopVoiceRecognition()
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(VoiceOSResponse(request.id, success))
        )
    }

    private suspend fun handleLearnApp(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<LearnAppRequest>(request.params ?: "{}")
        val appInfo = delegate.learnCurrentApp(params.packageName)
        return JsonRpcResponse(
            id = request.id,
            result = if (appInfo != null) json.encodeToString(appInfo) else null,
            error = if (appInfo == null) JsonRpcError(-32000, "Failed to learn app") else null
        )
    }

    private suspend fun handleGetLearnedApps(request: JsonRpcRequest): JsonRpcResponse {
        val apps = delegate.getLearnedApps()
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(LearnedAppsResponse(request.id, apps))
        )
    }

    private suspend fun handleGetCommands(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<GetCommandsRequest>(request.params ?: "{}")
        val commands = delegate.getCommandsForApp(params.packageName)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(AppCommandsResponse(request.id, commands))
        )
    }

    private suspend fun handleRegisterCommand(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<RegisterCommandRequest>(request.params ?: "{}")
        val success = delegate.registerDynamicCommand(
            params.phrase,
            params.actionType,
            params.params,
            params.appPackage
        )
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(VoiceOSResponse(request.id, success))
        )
    }

    private suspend fun handleUnregisterCommand(request: JsonRpcRequest): JsonRpcResponse {
        val params = json.decodeFromString<UnregisterCommandRequest>(request.params ?: "{}")
        val success = delegate.unregisterDynamicCommand(params.phrase, params.appPackage)
        return JsonRpcResponse(
            id = request.id,
            result = json.encodeToString(VoiceOSResponse(request.id, success))
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
