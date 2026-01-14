/*
 * VoiceOSGrpcServer.kt - gRPC Server for VoiceOS Service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Implements VoiceOSService from voiceos.proto using grpc-kotlin.
 * Supports both UDS (Unix Domain Socket) for local IPC and TCP for remote connections.
 */

package com.augmentalis.universalrpc.android.voiceos

import com.augmentalis.universalrpc.voiceos.*
import io.grpc.Server
import io.grpc.ServerBuilder
import io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.TimeUnit

/**
 * VoiceOS gRPC Server implementation.
 *
 * Provides gRPC endpoints for VoiceOS accessibility service operations.
 * Delegates actual operations to VoiceOSServiceDelegate interface.
 */
class VoiceOSGrpcServer(
    private val delegate: VoiceOSServiceDelegate,
    private val serverConfig: ServerConfig = ServerConfig()
) {
    private var server: Server? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Server configuration for transport options.
     */
    data class ServerConfig(
        val port: Int = 50051,
        val udsPath: String? = null, // Unix Domain Socket path for local IPC
        val useTls: Boolean = false,
        val maxConcurrentStreams: Int = 100,
        val keepAliveTimeSeconds: Long = 60
    )

    /**
     * Starts the gRPC server with configured transport.
     */
    fun start() {
        val serviceImpl = VoiceOSServiceImpl(delegate, scope)

        server = if (serverConfig.udsPath != null) {
            // UDS transport for local IPC (Android to Android)
            createUdsServer(serviceImpl)
        } else {
            // TCP transport for remote connections
            createTcpServer(serviceImpl)
        }

        server?.start()
    }

    /**
     * Creates a UDS-based server for local IPC.
     * More efficient than TCP for same-device communication.
     */
    private fun createUdsServer(serviceImpl: VoiceOSServiceGrpc.VoiceOSServiceImplBase): Server {
        val socketFile = File(serverConfig.udsPath!!)
        if (socketFile.exists()) {
            socketFile.delete()
        }

        return NettyServerBuilder
            .forAddress(io.netty.channel.unix.DomainSocketAddress(socketFile))
            .addService(serviceImpl)
            .maxConcurrentCallsPerConnection(serverConfig.maxConcurrentStreams)
            .build()
    }

    /**
     * Creates a TCP-based server for remote connections.
     */
    private fun createTcpServer(serviceImpl: VoiceOSServiceGrpc.VoiceOSServiceImplBase): Server {
        return NettyServerBuilder
            .forAddress(InetSocketAddress(serverConfig.port))
            .addService(serviceImpl)
            .maxConcurrentCallsPerConnection(serverConfig.maxConcurrentStreams)
            .keepAliveTime(serverConfig.keepAliveTimeSeconds, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Stops the gRPC server gracefully.
     */
    fun stop() {
        scope.cancel()
        server?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
    }

    /**
     * Blocks until the server is terminated.
     */
    fun blockUntilShutdown() {
        server?.awaitTermination()
    }

    /**
     * Returns true if the server is currently running.
     */
    fun isRunning(): Boolean = server?.isShutdown == false

    /**
     * Returns the port the server is listening on.
     */
    fun getPort(): Int = server?.port ?: -1
}

/**
 * gRPC service implementation that delegates to VoiceOSServiceDelegate.
 */
internal class VoiceOSServiceImpl(
    private val delegate: VoiceOSServiceDelegate,
    private val scope: CoroutineScope
) : VoiceOSServiceGrpc.VoiceOSServiceImplBase() {

    override fun isReady(
        request: IsReadyRequest,
        responseObserver: StreamObserver<ServiceStatus>
    ) {
        scope.launch {
            try {
                val status = delegate.isReady()
                responseObserver.onNext(status)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun executeCommand(
        request: CommandRequest,
        responseObserver: StreamObserver<CommandResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.executeCommand(
                    requestId = request.request_id,
                    commandText = request.command_text,
                    context = request.context
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun executeAccessibilityAction(
        request: AccessibilityActionRequest,
        responseObserver: StreamObserver<CommandResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.executeAccessibilityAction(
                    requestId = request.request_id,
                    actionType = request.action_type,
                    params = request.params
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun scrapeCurrentScreen(
        request: ScrapeScreenRequest,
        responseObserver: StreamObserver<ScrapeScreenResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.scrapeCurrentScreen(request.request_id)
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun startVoiceRecognition(
        request: StartVoiceRequest,
        responseObserver: StreamObserver<CommandResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.startVoiceRecognition(
                    requestId = request.request_id,
                    language = request.language,
                    recognizerType = request.recognizer_type
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun stopVoiceRecognition(
        request: StopVoiceRequest,
        responseObserver: StreamObserver<CommandResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.stopVoiceRecognition(request.request_id)
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun learnCurrentApp(
        request: LearnAppRequest,
        responseObserver: StreamObserver<LearnedApp>
    ) {
        scope.launch {
            try {
                val response = delegate.learnCurrentApp(
                    requestId = request.request_id,
                    packageName = request.package_name.takeIf { it.isNotEmpty() }
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun getLearnedApps(
        request: GetLearnedAppsRequest,
        responseObserver: StreamObserver<LearnedAppsResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.getLearnedApps(request.request_id)
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun getCommandsForApp(
        request: GetCommandsRequest,
        responseObserver: StreamObserver<AppCommandsResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.getCommandsForApp(
                    requestId = request.request_id,
                    packageName = request.package_name
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun registerDynamicCommand(
        request: DynamicCommandRequest,
        responseObserver: StreamObserver<CommandResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.registerDynamicCommand(
                    requestId = request.request_id,
                    commandText = request.command_text,
                    actionJson = request.action_json
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun streamEvents(
        request: StreamEventsRequest,
        responseObserver: StreamObserver<VoiceOSEvent>
    ) {
        scope.launch {
            try {
                delegate.streamEvents(
                    requestId = request.request_id,
                    eventTypes = request.event_types
                ).collect { event ->
                    responseObserver.onNext(event)
                }
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }
}

/**
 * Delegate interface for VoiceOS service operations.
 * Implement this interface to connect gRPC to actual VoiceOS functionality.
 */
interface VoiceOSServiceDelegate {
    suspend fun isReady(): ServiceStatus

    suspend fun executeCommand(
        requestId: String,
        commandText: String,
        context: Map<String, String>
    ): CommandResponse

    suspend fun executeAccessibilityAction(
        requestId: String,
        actionType: String,
        params: Map<String, String>
    ): CommandResponse

    suspend fun scrapeCurrentScreen(requestId: String): ScrapeScreenResponse

    suspend fun startVoiceRecognition(
        requestId: String,
        language: String,
        recognizerType: String
    ): CommandResponse

    suspend fun stopVoiceRecognition(requestId: String): CommandResponse

    suspend fun learnCurrentApp(
        requestId: String,
        packageName: String?
    ): LearnedApp

    suspend fun getLearnedApps(requestId: String): LearnedAppsResponse

    suspend fun getCommandsForApp(
        requestId: String,
        packageName: String
    ): AppCommandsResponse

    suspend fun registerDynamicCommand(
        requestId: String,
        commandText: String,
        actionJson: String
    ): CommandResponse

    suspend fun streamEvents(
        requestId: String,
        eventTypes: List<String>
    ): Flow<VoiceOSEvent>
}

/**
 * Stub implementation for testing and development.
 * Replace with actual VoiceOS service implementation.
 */
class VoiceOSServiceDelegateStub : VoiceOSServiceDelegate {

    override suspend fun isReady(): ServiceStatus {
        return ServiceStatus(
            ready = true,
            running = true,
            version = "1.0.0",
            enabled_features = listOf("voice_recognition", "screen_scraping", "app_learning")
        )
    }

    override suspend fun executeCommand(
        requestId: String,
        commandText: String,
        context: Map<String, String>
    ): CommandResponse {
        return CommandResponse(
            request_id = requestId,
            success = true,
            message = "Command executed: $commandText",
            result_json = "{}"
        )
    }

    override suspend fun executeAccessibilityAction(
        requestId: String,
        actionType: String,
        params: Map<String, String>
    ): CommandResponse {
        return CommandResponse(
            request_id = requestId,
            success = true,
            message = "Accessibility action executed: $actionType"
        )
    }

    override suspend fun scrapeCurrentScreen(requestId: String): ScrapeScreenResponse {
        return ScrapeScreenResponse(
            request_id = requestId,
            success = true,
            screen_json = "{\"elements\": []}"
        )
    }

    override suspend fun startVoiceRecognition(
        requestId: String,
        language: String,
        recognizerType: String
    ): CommandResponse {
        return CommandResponse(
            request_id = requestId,
            success = true,
            message = "Voice recognition started"
        )
    }

    override suspend fun stopVoiceRecognition(requestId: String): CommandResponse {
        return CommandResponse(
            request_id = requestId,
            success = true,
            message = "Voice recognition stopped"
        )
    }

    override suspend fun learnCurrentApp(
        requestId: String,
        packageName: String?
    ): LearnedApp {
        return LearnedApp(
            package_name = packageName ?: "com.example.app",
            app_name = "Example App",
            commands = listOf("open", "close", "scroll"),
            learned_at = System.currentTimeMillis()
        )
    }

    override suspend fun getLearnedApps(requestId: String): LearnedAppsResponse {
        return LearnedAppsResponse(
            apps = emptyList()
        )
    }

    override suspend fun getCommandsForApp(
        requestId: String,
        packageName: String
    ): AppCommandsResponse {
        return AppCommandsResponse(
            package_name = packageName,
            commands = listOf("open", "close", "scroll")
        )
    }

    override suspend fun registerDynamicCommand(
        requestId: String,
        commandText: String,
        actionJson: String
    ): CommandResponse {
        return CommandResponse(
            request_id = requestId,
            success = true,
            message = "Dynamic command registered: $commandText"
        )
    }

    override suspend fun streamEvents(
        requestId: String,
        eventTypes: List<String>
    ): Flow<VoiceOSEvent> = callbackFlow {
        // Stub: No events emitted
        awaitClose { }
    }
}
