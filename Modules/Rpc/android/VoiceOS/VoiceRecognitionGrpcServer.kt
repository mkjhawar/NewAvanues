/*
 * VoiceRecognitionGrpcServer.kt - gRPC Server for Voice Recognition Service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Implements VoiceRecognitionService from recognition.proto using grpc-kotlin.
 * Handles voice recognition start/stop, state management, and result streaming.
 */

package com.augmentalis.rpc.android.voiceos

import com.augmentalis.rpc.recognition.*
import io.grpc.Server
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
 * Voice Recognition gRPC Server implementation.
 *
 * Provides gRPC endpoints for voice recognition operations.
 * Supports both UDS (local) and TCP (remote) transports.
 */
class VoiceRecognitionGrpcServer(
    private val delegate: VoiceRecognitionServiceDelegate,
    private val serverConfig: ServerConfig = ServerConfig()
) {
    private var server: Server? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class ServerConfig(
        val port: Int = 50053,
        val udsPath: String? = null,
        val maxConcurrentStreams: Int = 100,
        val keepAliveTimeSeconds: Long = 60
    )

    fun start() {
        val serviceImpl = VoiceRecognitionServiceImpl(delegate, scope)

        server = if (serverConfig.udsPath != null) {
            createUdsServer(serviceImpl)
        } else {
            createTcpServer(serviceImpl)
        }

        server?.start()
    }

    private fun createUdsServer(serviceImpl: VoiceRecognitionServiceGrpc.VoiceRecognitionServiceImplBase): Server {
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

    private fun createTcpServer(serviceImpl: VoiceRecognitionServiceGrpc.VoiceRecognitionServiceImplBase): Server {
        return NettyServerBuilder
            .forAddress(InetSocketAddress(serverConfig.port))
            .addService(serviceImpl)
            .maxConcurrentCallsPerConnection(serverConfig.maxConcurrentStreams)
            .keepAliveTime(serverConfig.keepAliveTimeSeconds, TimeUnit.SECONDS)
            .build()
    }

    fun stop() {
        scope.cancel()
        server?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
    }

    fun blockUntilShutdown() {
        server?.awaitTermination()
    }

    fun isRunning(): Boolean = server?.isShutdown == false

    fun getPort(): Int = server?.port ?: -1
}

/**
 * gRPC service implementation for Voice Recognition.
 */
internal class VoiceRecognitionServiceImpl(
    private val delegate: VoiceRecognitionServiceDelegate,
    private val scope: CoroutineScope
) : VoiceRecognitionServiceGrpc.VoiceRecognitionServiceImplBase() {

    override fun start(
        request: StartRecognitionRequest,
        responseObserver: StreamObserver<RecognitionResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.start(
                    requestId = request.request_id,
                    config = request.config
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun stop(
        request: StopRecognitionRequest,
        responseObserver: StreamObserver<RecognitionResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.stop(request.request_id)
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun getState(
        request: GetStateRequest,
        responseObserver: StreamObserver<RecognitionStatus>
    ) {
        scope.launch {
            try {
                val status = delegate.getState(request.request_id)
                responseObserver.onNext(status)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun streamResults(
        request: StartRecognitionRequest,
        responseObserver: StreamObserver<RecognitionEvent>
    ) {
        scope.launch {
            try {
                delegate.streamResults(
                    requestId = request.request_id,
                    config = request.config
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
 * Delegate interface for Voice Recognition service operations.
 * Implement this to connect gRPC to actual voice recognition functionality.
 */
interface VoiceRecognitionServiceDelegate {
    suspend fun start(
        requestId: String,
        config: RecognitionConfig?
    ): RecognitionResponse

    suspend fun stop(requestId: String): RecognitionResponse

    suspend fun getState(requestId: String): RecognitionStatus

    suspend fun streamResults(
        requestId: String,
        config: RecognitionConfig?
    ): Flow<RecognitionEvent>
}

/**
 * Stub implementation for testing and development.
 */
class VoiceRecognitionServiceDelegateStub : VoiceRecognitionServiceDelegate {

    private var currentState = RecognitionStatus(
        state = "stopped",
        volume_level = 0.0f
    )

    override suspend fun start(
        requestId: String,
        config: RecognitionConfig?
    ): RecognitionResponse {
        currentState = RecognitionStatus(
            state = "listening",
            volume_level = 0.0f
        )
        return RecognitionResponse(
            request_id = requestId,
            success = true,
            message = "Recognition started with language: ${config?.language ?: "en-US"}"
        )
    }

    override suspend fun stop(requestId: String): RecognitionResponse {
        currentState = RecognitionStatus(
            state = "stopped",
            volume_level = 0.0f
        )
        return RecognitionResponse(
            request_id = requestId,
            success = true,
            message = "Recognition stopped"
        )
    }

    override suspend fun getState(requestId: String): RecognitionStatus {
        return currentState
    }

    override suspend fun streamResults(
        requestId: String,
        config: RecognitionConfig?
    ): Flow<RecognitionEvent> = callbackFlow {
        // Stub: Start recognition and emit status
        val sessionId = requestId

        // Emit initial status
        trySend(
            RecognitionEvent(
                session_id = sessionId,
                status = RecognitionStatus(
                    state = "listening",
                    volume_level = 0.0f
                )
            )
        )

        // In real implementation, this would:
        // 1. Start the actual voice recognizer
        // 2. Emit RecognitionData events as speech is recognized
        // 3. Emit RecognitionError events on errors
        // 4. Emit RecognitionStatus updates on state changes

        awaitClose {
            // Clean up recognition resources
            currentState = RecognitionStatus(
                state = "stopped",
                volume_level = 0.0f
            )
        }
    }
}

/**
 * Extension to create a combined server for all VoiceOS services.
 * Useful for running all services on a single port.
 */
class CombinedVoiceOSServer(
    private val voiceOSDelegate: VoiceOSServiceDelegate,
    private val cursorDelegate: VoiceCursorServiceDelegate,
    private val recognitionDelegate: VoiceRecognitionServiceDelegate,
    private val serverConfig: CombinedServerConfig = CombinedServerConfig()
) {
    private var server: Server? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class CombinedServerConfig(
        val port: Int = 50050,
        val udsPath: String? = null,
        val maxConcurrentStreams: Int = 100,
        val keepAliveTimeSeconds: Long = 60
    )

    fun start() {
        val voiceOSService = VoiceOSServiceImpl(voiceOSDelegate, scope)
        val cursorService = VoiceCursorServiceImpl(cursorDelegate, scope)
        val recognitionService = VoiceRecognitionServiceImpl(recognitionDelegate, scope)

        val builder = if (serverConfig.udsPath != null) {
            val socketFile = File(serverConfig.udsPath!!)
            if (socketFile.exists()) socketFile.delete()
            NettyServerBuilder.forAddress(io.netty.channel.unix.DomainSocketAddress(socketFile))
        } else {
            NettyServerBuilder.forAddress(InetSocketAddress(serverConfig.port))
        }

        server = builder
            .addService(voiceOSService)
            .addService(cursorService)
            .addService(recognitionService)
            .maxConcurrentCallsPerConnection(serverConfig.maxConcurrentStreams)
            .keepAliveTime(serverConfig.keepAliveTimeSeconds, TimeUnit.SECONDS)
            .build()
            .start()
    }

    fun stop() {
        scope.cancel()
        server?.shutdown()?.awaitTermination(5, TimeUnit.SECONDS)
    }

    fun blockUntilShutdown() {
        server?.awaitTermination()
    }

    fun isRunning(): Boolean = server?.isShutdown == false

    fun getPort(): Int = server?.port ?: -1
}
