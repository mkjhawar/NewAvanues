/*
 * VoiceOSClient.kt - Client for calling VoiceOS accessibility service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * This client provides suspend functions for AVA to interact with VoiceOS
 * accessibility services, including command execution, screen scraping,
 * and app learning.
 */
package com.augmentalis.universalrpc.android.ava

import com.augmentalis.universalrpc.voiceos.AccessibilityActionRequest
import com.augmentalis.universalrpc.voiceos.AppCommandsResponse
import com.augmentalis.universalrpc.voiceos.CommandRequest
import com.augmentalis.universalrpc.voiceos.CommandResponse
import com.augmentalis.universalrpc.voiceos.DynamicCommandRequest
import com.augmentalis.universalrpc.voiceos.GetCommandsRequest
import com.augmentalis.universalrpc.voiceos.GetLearnedAppsRequest
import com.augmentalis.universalrpc.voiceos.IsReadyRequest
import com.augmentalis.universalrpc.voiceos.LearnAppRequest
import com.augmentalis.universalrpc.voiceos.LearnedApp
import com.augmentalis.universalrpc.voiceos.LearnedAppsResponse
import com.augmentalis.universalrpc.voiceos.ScrapeScreenRequest
import com.augmentalis.universalrpc.voiceos.ScrapeScreenResponse
import com.augmentalis.universalrpc.voiceos.ServiceStatus
import com.augmentalis.universalrpc.voiceos.StartVoiceRequest
import com.augmentalis.universalrpc.voiceos.StopVoiceRequest
import com.augmentalis.universalrpc.voiceos.StreamEventsRequest
import com.augmentalis.universalrpc.voiceos.VoiceOSEvent
import com.augmentalis.universalrpc.voiceos.VoiceOSServiceGrpcKt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.UUID

/**
 * Client for interacting with VoiceOS accessibility service.
 *
 * Provides high-level suspend functions for all VoiceOS operations including:
 * - Service status checks
 * - Voice command execution
 * - Accessibility actions (back, home, etc.)
 * - Screen scraping
 * - App learning
 * - Dynamic command registration
 * - Event streaming
 *
 * @param grpcClient The base gRPC client for connection management
 * @param dispatcher The coroutine dispatcher for async operations
 */
class VoiceOSClient(
    private val grpcClient: AvaGrpcClient,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : Closeable {

    private var stub: VoiceOSServiceGrpcKt.VoiceOSServiceCoroutineStub? = null

    /**
     * Get or create the gRPC stub for VoiceOS service.
     */
    private suspend fun getStub(): VoiceOSServiceGrpcKt.VoiceOSServiceCoroutineStub {
        stub?.let { return it }

        val channel = grpcClient.getChannel()
        return VoiceOSServiceGrpcKt.VoiceOSServiceCoroutineStub(channel).also {
            stub = it
        }
    }

    // =========================================================================
    // Service Status Operations
    // =========================================================================

    /**
     * Check if VoiceOS service is ready and running.
     *
     * @return ServiceStatus containing readiness and version info
     */
    suspend fun isReady(): ServiceStatus = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = IsReadyRequest(request_id = generateRequestId())
            getStub().IsReady(request)
        }
    }

    /**
     * Check if service is connected and ready.
     *
     * @return true if service is ready
     */
    suspend fun ping(): Boolean = withContext(dispatcher) {
        try {
            isReady().ready
        } catch (e: Exception) {
            false
        }
    }

    // =========================================================================
    // Command Execution
    // =========================================================================

    /**
     * Execute a voice command through VoiceOS.
     *
     * @param commandText The command text to execute
     * @param context Optional context map for the command
     * @return CommandResponse with success status and result
     */
    suspend fun executeCommand(
        commandText: String,
        context: Map<String, String> = emptyMap()
    ): CommandResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = CommandRequest(
                request_id = generateRequestId(),
                command_text = commandText,
                context = context
            )
            getStub().ExecuteCommand(request)
        }
    }

    /**
     * Execute an accessibility action (back, home, recent, notifications).
     *
     * @param actionType The type of action: "back", "home", "recent", "notifications"
     * @param params Optional parameters for the action
     * @return CommandResponse with success status
     */
    suspend fun executeAccessibilityAction(
        actionType: String,
        params: Map<String, String> = emptyMap()
    ): CommandResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = AccessibilityActionRequest(
                request_id = generateRequestId(),
                action_type = actionType,
                params = params
            )
            getStub().ExecuteAccessibilityAction(request)
        }
    }

    /**
     * Navigate back.
     */
    suspend fun pressBack(): CommandResponse = executeAccessibilityAction("back")

    /**
     * Navigate home.
     */
    suspend fun pressHome(): CommandResponse = executeAccessibilityAction("home")

    /**
     * Open recent apps.
     */
    suspend fun openRecents(): CommandResponse = executeAccessibilityAction("recent")

    /**
     * Open notifications panel.
     */
    suspend fun openNotifications(): CommandResponse = executeAccessibilityAction("notifications")

    // =========================================================================
    // Screen Scraping
    // =========================================================================

    /**
     * Scrape the current screen for accessibility information.
     *
     * @return ScrapeScreenResponse containing screen JSON data
     */
    suspend fun scrapeCurrentScreen(): ScrapeScreenResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = ScrapeScreenRequest(request_id = generateRequestId())
            getStub().ScrapeCurrentScreen(request)
        }
    }

    // =========================================================================
    // Voice Recognition Control
    // =========================================================================

    /**
     * Start voice recognition.
     *
     * @param language The target language code (e.g., "en-US")
     * @param recognizerType The recognizer type: "google", "vivoka", "whisper"
     * @return CommandResponse with success status
     */
    suspend fun startVoiceRecognition(
        language: String = "en-US",
        recognizerType: String = "google"
    ): CommandResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = StartVoiceRequest(
                request_id = generateRequestId(),
                language = language,
                recognizer_type = recognizerType
            )
            getStub().StartVoiceRecognition(request)
        }
    }

    /**
     * Stop voice recognition.
     *
     * @return CommandResponse with success status
     */
    suspend fun stopVoiceRecognition(): CommandResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = StopVoiceRequest(request_id = generateRequestId())
            getStub().StopVoiceRecognition(request)
        }
    }

    // =========================================================================
    // App Learning
    // =========================================================================

    /**
     * Learn the currently focused app.
     *
     * @param packageName Optional package name (defaults to current app)
     * @return LearnedApp with app info and learned commands
     */
    suspend fun learnCurrentApp(packageName: String? = null): LearnedApp = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = LearnAppRequest(
                request_id = generateRequestId(),
                package_name = packageName ?: ""
            )
            getStub().LearnCurrentApp(request)
        }
    }

    /**
     * Get all previously learned apps.
     *
     * @return LearnedAppsResponse with list of learned apps
     */
    suspend fun getLearnedApps(): LearnedAppsResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = GetLearnedAppsRequest(request_id = generateRequestId())
            getStub().GetLearnedApps(request)
        }
    }

    /**
     * Get learned commands for a specific app.
     *
     * @param packageName The app's package name
     * @return AppCommandsResponse with list of commands
     */
    suspend fun getCommandsForApp(packageName: String): AppCommandsResponse =
        withContext(dispatcher) {
            grpcClient.withRetry { _ ->
                val request = GetCommandsRequest(
                    request_id = generateRequestId(),
                    package_name = packageName
                )
                getStub().GetCommandsForApp(request)
            }
        }

    // =========================================================================
    // Dynamic Commands
    // =========================================================================

    /**
     * Register a dynamic command.
     *
     * @param commandText The trigger text for the command
     * @param actionJson JSON definition of the action to perform
     * @return CommandResponse with success status
     */
    suspend fun registerDynamicCommand(
        commandText: String,
        actionJson: String
    ): CommandResponse = withContext(dispatcher) {
        grpcClient.withRetry { _ ->
            val request = DynamicCommandRequest(
                request_id = generateRequestId(),
                command_text = commandText,
                action_json = actionJson
            )
            getStub().RegisterDynamicCommand(request)
        }
    }

    // =========================================================================
    // Event Streaming
    // =========================================================================

    /**
     * Stream VoiceOS events.
     *
     * This provides a Flow of events including:
     * - Command results
     * - Status changes
     * - Screen changes
     * - Accessibility events
     *
     * @param eventTypes Optional filter for specific event types
     * @return Flow of VoiceOSEvent
     */
    suspend fun streamEvents(
        eventTypes: List<String> = emptyList()
    ): Flow<VoiceOSEvent> {
        return grpcClient.withStreaming { _ ->
            val request = StreamEventsRequest(
                request_id = generateRequestId(),
                event_types = eventTypes
            )
            getStub().StreamEvents(request)
        }
    }

    // =========================================================================
    // Utility
    // =========================================================================

    private fun generateRequestId(): String = UUID.randomUUID().toString()

    override fun close() {
        grpcClient.close()
    }

    companion object {
        /**
         * Create a client for local UDS connection.
         */
        fun forLocalConnection(
            socketPath: String = "/data/local/tmp/voiceos.sock"
        ): VoiceOSClient {
            return VoiceOSClient(AvaGrpcClient.forLocalConnection(socketPath))
        }

        /**
         * Create a client for remote TCP connection.
         */
        fun forRemoteConnection(
            host: String,
            port: Int = 50051,
            useTls: Boolean = false
        ): VoiceOSClient {
            return VoiceOSClient(AvaGrpcClient.forRemoteConnection(host, port, useTls))
        }
    }
}
