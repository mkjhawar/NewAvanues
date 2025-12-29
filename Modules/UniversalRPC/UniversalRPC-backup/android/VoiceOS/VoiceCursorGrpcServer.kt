/*
 * VoiceCursorGrpcServer.kt - gRPC Server for VoiceCursor Service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Implements VoiceCursorService from cursor.proto using grpc-kotlin.
 * Handles cursor positioning, movement, and action execution.
 */

package com.augmentalis.universalrpc.android.voiceos

import com.augmentalis.universalrpc.cursor.*
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
 * VoiceCursor gRPC Server implementation.
 *
 * Provides gRPC endpoints for cursor control operations.
 * Supports both UDS (local) and TCP (remote) transports.
 */
class VoiceCursorGrpcServer(
    private val delegate: VoiceCursorServiceDelegate,
    private val serverConfig: ServerConfig = ServerConfig()
) {
    private var server: Server? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    data class ServerConfig(
        val port: Int = 50052,
        val udsPath: String? = null,
        val maxConcurrentStreams: Int = 100,
        val keepAliveTimeSeconds: Long = 60
    )

    fun start() {
        val serviceImpl = VoiceCursorServiceImpl(delegate, scope)

        server = if (serverConfig.udsPath != null) {
            createUdsServer(serviceImpl)
        } else {
            createTcpServer(serviceImpl)
        }

        server?.start()
    }

    private fun createUdsServer(serviceImpl: VoiceCursorServiceGrpc.VoiceCursorServiceImplBase): Server {
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

    private fun createTcpServer(serviceImpl: VoiceCursorServiceGrpc.VoiceCursorServiceImplBase): Server {
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
 * gRPC service implementation for VoiceCursor.
 */
internal class VoiceCursorServiceImpl(
    private val delegate: VoiceCursorServiceDelegate,
    private val scope: CoroutineScope
) : VoiceCursorServiceGrpc.VoiceCursorServiceImplBase() {

    override fun getPosition(
        request: GetPositionRequest,
        responseObserver: StreamObserver<CursorPosition>
    ) {
        scope.launch {
            try {
                val position = delegate.getPosition(request.request_id)
                responseObserver.onNext(position)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun moveTo(
        request: MoveCursorRequest,
        responseObserver: StreamObserver<CursorResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.moveTo(
                    requestId = request.request_id,
                    targetX = request.target_x,
                    targetY = request.target_y,
                    animate = request.animate,
                    durationMs = request.duration_ms
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun executeAction(
        request: CursorActionRequest,
        responseObserver: StreamObserver<CursorResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.executeAction(
                    requestId = request.request_id,
                    action = request.action,
                    params = request.params
                )
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun configure(
        request: CursorConfig,
        responseObserver: StreamObserver<CursorResponse>
    ) {
        scope.launch {
            try {
                val response = delegate.configure(request)
                responseObserver.onNext(response)
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }

    override fun streamPosition(
        request: StreamPositionRequest,
        responseObserver: StreamObserver<CursorPosition>
    ) {
        scope.launch {
            try {
                delegate.streamPosition(
                    requestId = request.request_id,
                    intervalMs = request.interval_ms
                ).collect { position ->
                    responseObserver.onNext(position)
                }
                responseObserver.onCompleted()
            } catch (e: Exception) {
                responseObserver.onError(e)
            }
        }
    }
}

/**
 * Delegate interface for VoiceCursor service operations.
 * Implement this to connect gRPC to actual cursor functionality.
 */
interface VoiceCursorServiceDelegate {
    suspend fun getPosition(requestId: String): CursorPosition

    suspend fun moveTo(
        requestId: String,
        targetX: Int,
        targetY: Int,
        animate: Boolean,
        durationMs: Int
    ): CursorResponse

    suspend fun executeAction(
        requestId: String,
        action: String,
        params: Map<String, String>
    ): CursorResponse

    suspend fun configure(config: CursorConfig): CursorResponse

    suspend fun streamPosition(
        requestId: String,
        intervalMs: Int
    ): Flow<CursorPosition>
}

/**
 * Stub implementation for testing and development.
 */
class VoiceCursorServiceDelegateStub : VoiceCursorServiceDelegate {

    private var currentPosition = CursorPosition(x = 0, y = 0, timestamp = System.currentTimeMillis())
    private var currentConfig = CursorConfig(
        size = 24,
        color = 0xFF0000FF.toInt(),
        speed = 1.0f,
        visible = true,
        mode = "touch"
    )

    override suspend fun getPosition(requestId: String): CursorPosition {
        return currentPosition.copy(timestamp = System.currentTimeMillis())
    }

    override suspend fun moveTo(
        requestId: String,
        targetX: Int,
        targetY: Int,
        animate: Boolean,
        durationMs: Int
    ): CursorResponse {
        currentPosition = CursorPosition(
            x = targetX,
            y = targetY,
            timestamp = System.currentTimeMillis()
        )
        return CursorResponse(
            request_id = requestId,
            success = true,
            message = "Cursor moved to ($targetX, $targetY)",
            position = currentPosition
        )
    }

    override suspend fun executeAction(
        requestId: String,
        action: String,
        params: Map<String, String>
    ): CursorResponse {
        return CursorResponse(
            request_id = requestId,
            success = true,
            message = "Action executed: $action",
            position = currentPosition
        )
    }

    override suspend fun configure(config: CursorConfig): CursorResponse {
        currentConfig = config
        return CursorResponse(
            request_id = "",
            success = true,
            message = "Cursor configuration updated",
            position = currentPosition
        )
    }

    override suspend fun streamPosition(
        requestId: String,
        intervalMs: Int
    ): Flow<CursorPosition> = callbackFlow {
        // Stub: Emits current position periodically
        // In real implementation, this would emit actual cursor updates
        awaitClose { }
    }
}
