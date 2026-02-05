/**
 * VoiceOSAvuRpcServer.kt - AVU 2.1 Protocol RPC server for VoiceOS (Android)
 *
 * Native AVU 2.1 protocol server - compact, efficient, line-based.
 * Each message is a single line: CODE:field1:field2:...
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.voiceoscore.rpc

import android.util.Log
import com.augmentalis.voiceoscore.rpc.messages.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

private const val TAG = "VoiceOSAvuRpc"

/**
 * AVU 2.1 Protocol RPC server for VoiceOS
 *
 * Protocol:
 * - Request: CODE:field1:field2:field3...
 * - Response: CODE:field1:field2:field3...
 * - See VoiceOSAvuCodes for operation codes
 * - Escape: %3A for :, %25 for %, %0A for newline
 */
class VoiceOSAvuRpcServer(
    private val delegate: IVoiceOSServiceDelegate,
    private val config: VoiceOSServerConfig = VoiceOSServerConfig()
) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val eventClients = mutableListOf<PrintWriter>()
    private val eventClientsLock = Any()

    /**
     * Start the AVU RPC server
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
                Log.i(TAG, "VoiceOS AVU 2.1 server started on port ${config.port}")

                // Start event broadcaster
                launch { broadcastEvents() }

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
        synchronized(eventClientsLock) {
            eventClients.clear()
        }
        serverSocket?.close()
        serverSocket = null
        Log.i(TAG, "VoiceOS AVU 2.1 server stopped")
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
        var writer: PrintWriter? = null
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            writer = PrintWriter(socket.getOutputStream(), true)

            // Read request (single line for AVU 2.1)
            val requestLine = reader.readLine()
            if (!requestLine.isNullOrBlank()) {
                val response = processRequest(requestLine, writer)
                if (response != null) {
                    writer.println(response)
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error handling client", e)
        } finally {
            // Remove from event clients if subscribed
            writer?.let { w ->
                synchronized(eventClientsLock) {
                    eventClients.remove(w)
                }
            }
            socket.close()
        }
    }

    private suspend fun processRequest(requestLine: String, writer: PrintWriter): String? {
        val message = VoiceOSAvuDecoder.parse(requestLine)
        if (message == null) {
            return "${VoiceOSAvuCodes.NAK}::Parse error"
        }

        return when (message.code) {
            VoiceOSAvuCodes.GST -> handleGetStatus(message)
            VoiceOSAvuCodes.VCM -> handleVoiceCommand(message)
            VoiceOSAvuCodes.AAC -> handleAccessibilityAction(message)
            VoiceOSAvuCodes.SSC -> handleScrapeScreen(message)
            VoiceOSAvuCodes.SRC -> handleStartRecognition(message)
            VoiceOSAvuCodes.PRC -> handleStopRecognition(message)
            VoiceOSAvuCodes.LAP -> handleLearnApp(message)
            VoiceOSAvuCodes.GAP -> handleGetLearnedApps(message)
            VoiceOSAvuCodes.GCM -> handleGetCommands(message)
            VoiceOSAvuCodes.RGC -> handleRegisterCommand(message)
            VoiceOSAvuCodes.URC -> handleUnregisterCommand(message)
            "SUB" -> { // Subscribe to events
                handleSubscribe(writer)
                null // No immediate response, events will be pushed
            }
            else -> "${VoiceOSAvuCodes.NAK}:${message.fields.getOrElse(0) { "" }}:Unknown operation ${message.code}"
        }
    }

    // Handler methods using AVU encoding

    private suspend fun handleGetStatus(message: AvuMessage): String {
        val request = VoiceOSAvuDecoder.decodeStatusRequest(message)
        if (request == null) {
            return "${VoiceOSAvuCodes.NAK}::Invalid status request"
        }

        val status = delegate.getServiceStatus()
        return VoiceOSAvuEncoder.encodeStatusResponse(
            ServiceStatus(
                requestId = request.requestId,
                isActive = status.isActive,
                currentApp = status.currentApp,
                isRecognizing = status.isRecognizing,
                recognizedCommands = status.recognizedCommands
            )
        )
    }

    private suspend fun handleVoiceCommand(message: AvuMessage): String {
        val request = VoiceOSAvuDecoder.decodeVoiceCommand(message)
        if (request == null) {
            return "${VoiceOSAvuCodes.NAK}::Invalid voice command"
        }

        val response = delegate.executeVoiceCommand(request.commandText, request.context)
        return VoiceOSAvuEncoder.encodeVoiceCommandResponse(
            VoiceCommandResponse(
                requestId = request.requestId,
                success = response.success,
                action = response.action,
                result = response.result,
                error = response.error
            )
        )
    }

    private suspend fun handleAccessibilityAction(message: AvuMessage): String {
        val request = VoiceOSAvuDecoder.decodeAccessibilityAction(message)
        if (request == null) {
            return "${VoiceOSAvuCodes.NAK}::Invalid action request"
        }

        val success = delegate.performAction(request.actionType, request.targetAvid, request.params)
        val result = delegate.getActionResult()

        return VoiceOSAvuEncoder.encodeAccessibilityActionResponse(
            AccessibilityActionResponse(
                requestId = request.requestId,
                success = success,
                result = result
            )
        )
    }

    private suspend fun handleScrapeScreen(message: AvuMessage): String {
        val request = VoiceOSAvuDecoder.decodeScrapeScreenRequest(message)
        if (request == null) {
            return "${VoiceOSAvuCodes.NAK}::Invalid scrape request"
        }

        val response = delegate.scrapeCurrentScreen(request.includeInvisible, request.maxDepth)

        // Build multi-line response: header + elements
        val lines = mutableListOf<String>()

        // Header: SCR:requestId:packageName:activityName:elementCount:timestamp
        lines.add(buildString {
            append(VoiceOSAvuCodes.SCR)
            append(":").append(VoiceOSAvuEncoder.escape(request.requestId))
            append(":").append(VoiceOSAvuEncoder.escape(response.packageName))
            append(":").append(VoiceOSAvuEncoder.escape(response.activityName))
            append(":").append(response.elements.size)
            append(":").append(response.timestamp)
        })

        // Elements
        response.elements.forEach { element ->
            lines.add(VoiceOSAvuEncoder.encodeScrapedElement(element))
        }

        // End marker
        lines.add("END:${request.requestId}")

        return lines.joinToString("\n")
    }

    private suspend fun handleStartRecognition(message: AvuMessage): String {
        val request = VoiceOSAvuDecoder.decodeStartRecognition(message)
        if (request == null) {
            return "${VoiceOSAvuCodes.NAK}::Invalid recognition request"
        }

        val success = delegate.startVoiceRecognition(request.language, request.continuous)
        return VoiceOSAvuEncoder.encodeResponse(
            VoiceOSResponse(
                requestId = request.requestId,
                success = success,
                message = if (success) "Recognition started" else null,
                error = if (!success) "Failed to start recognition" else null
            )
        )
    }

    private suspend fun handleStopRecognition(message: AvuMessage): String {
        val requestId = message.fields.getOrElse(0) { "" }
        val success = delegate.stopVoiceRecognition()
        return VoiceOSAvuEncoder.encodeResponse(
            VoiceOSResponse(
                requestId = requestId,
                success = success,
                message = if (success) "Recognition stopped" else null,
                error = if (!success) "Failed to stop recognition" else null
            )
        )
    }

    private suspend fun handleLearnApp(message: AvuMessage): String {
        val requestId = message.fields.getOrElse(0) { "" }
        val packageName = message.fields.getOrNull(1)?.takeIf { it.isNotEmpty() }

        val appInfo = delegate.learnCurrentApp(packageName)
        return if (appInfo != null) {
            // APR:requestId:packageName:appName:commandCount:learnedAt
            buildString {
                append(VoiceOSAvuCodes.APR)
                append(":").append(VoiceOSAvuEncoder.escape(requestId))
                append(":").append(VoiceOSAvuEncoder.escape(appInfo.packageName))
                append(":").append(VoiceOSAvuEncoder.escape(appInfo.appName))
                append(":").append(appInfo.commandCount)
                append(":").append(appInfo.learnedAt)
            }
        } else {
            "${VoiceOSAvuCodes.NAK}:${requestId}:Failed to learn app"
        }
    }

    private suspend fun handleGetLearnedApps(message: AvuMessage): String {
        val requestId = message.fields.getOrElse(0) { "" }
        val apps = delegate.getLearnedApps()

        val lines = mutableListOf<String>()

        // Header: APR:requestId:count
        lines.add("${VoiceOSAvuCodes.APR}:${VoiceOSAvuEncoder.escape(requestId)}:${apps.size}")

        // Each app
        apps.forEach { app ->
            lines.add(buildString {
                append("APP")
                append(":").append(VoiceOSAvuEncoder.escape(app.packageName))
                append(":").append(VoiceOSAvuEncoder.escape(app.appName))
                append(":").append(app.commandCount)
                append(":").append(app.learnedAt)
            })
        }

        lines.add("END:${requestId}")
        return lines.joinToString("\n")
    }

    private suspend fun handleGetCommands(message: AvuMessage): String {
        val requestId = message.fields.getOrElse(0) { "" }
        val packageName = message.fields.getOrElse(1) { "" }

        val commands = delegate.getCommandsForApp(packageName)

        val lines = mutableListOf<String>()

        // Header
        lines.add("${VoiceOSAvuCodes.CMD}:${VoiceOSAvuEncoder.escape(requestId)}:${commands.size}")

        // Each command
        commands.forEach { cmd ->
            lines.add(buildString {
                append("CMD")
                append(":").append(VoiceOSAvuEncoder.escape(cmd.phrase))
                append(":").append(VoiceOSAvuEncoder.escape(cmd.actionType))
                append(":").append(VoiceOSAvuEncoder.escape(cmd.targetAvid ?: ""))
                append(":").append(
                    cmd.params.entries.joinToString(",") {
                        "${VoiceOSAvuEncoder.escape(it.key)}=${VoiceOSAvuEncoder.escape(it.value)}"
                    }
                )
            })
        }

        lines.add("END:${requestId}")
        return lines.joinToString("\n")
    }

    private suspend fun handleRegisterCommand(message: AvuMessage): String {
        // RGC:requestId:phrase:actionType:params:appPackage
        val requestId = message.fields.getOrElse(0) { "" }
        val phrase = message.fields.getOrElse(1) { "" }
        val actionType = message.fields.getOrElse(2) { "" }
        val paramsStr = message.fields.getOrNull(3) ?: ""
        val appPackage = message.fields.getOrNull(4)?.takeIf { it.isNotEmpty() }

        val params = if (paramsStr.isNotBlank()) {
            paramsStr.split(",")
                .mapNotNull { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) {
                        VoiceOSAvuDecoder.unescape(parts[0]) to VoiceOSAvuDecoder.unescape(parts[1])
                    } else null
                }
                .toMap()
        } else emptyMap()

        val success = delegate.registerDynamicCommand(phrase, actionType, params, appPackage)
        return VoiceOSAvuEncoder.encodeResponse(
            VoiceOSResponse(
                requestId = requestId,
                success = success,
                message = if (success) "Command registered" else null,
                error = if (!success) "Failed to register command" else null
            )
        )
    }

    private suspend fun handleUnregisterCommand(message: AvuMessage): String {
        // URC:requestId:phrase:appPackage
        val requestId = message.fields.getOrElse(0) { "" }
        val phrase = message.fields.getOrElse(1) { "" }
        val appPackage = message.fields.getOrNull(2)?.takeIf { it.isNotEmpty() }

        val success = delegate.unregisterDynamicCommand(phrase, appPackage)
        return VoiceOSAvuEncoder.encodeResponse(
            VoiceOSResponse(
                requestId = requestId,
                success = success,
                message = if (success) "Command unregistered" else null,
                error = if (!success) "Failed to unregister command" else null
            )
        )
    }

    private fun handleSubscribe(writer: PrintWriter) {
        synchronized(eventClientsLock) {
            eventClients.add(writer)
        }
        // Send ACK for subscription
        writer.println("${VoiceOSAvuCodes.ACK}:SUB:Subscribed to events")
    }

    private suspend fun broadcastEvents() {
        delegate.getEventFlow()
            .catch { e -> Log.e(TAG, "Event flow error", e) }
            .collect { event ->
                val encoded = VoiceOSAvuEncoder.encodeEvent(event)
                synchronized(eventClientsLock) {
                    val iterator = eventClients.iterator()
                    while (iterator.hasNext()) {
                        val client = iterator.next()
                        try {
                            client.println(encoded)
                        } catch (e: Exception) {
                            iterator.remove()
                        }
                    }
                }
            }
    }
}
