/*
 * AvaGrpcClient.kt - Base gRPC client for AVA to connect to VoiceOS services
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * This client provides the foundation for AVA to communicate with VoiceOS
 * services via gRPC, supporting both Unix Domain Socket (local) and TCP (remote)
 * connections.
 */
package com.augmentalis.rpc.android.ava

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Status
import io.grpc.StatusException
import io.grpc.netty.NettyChannelBuilder
import io.netty.channel.epoll.EpollDomainSocketChannel
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.unix.DomainSocketAddress
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Connection configuration for gRPC clients
 */
data class GrpcConnectionConfig(
    /** Connection mode: UDS for local, TCP for remote */
    val mode: ConnectionMode,
    /** Host for TCP connections (ignored for UDS) */
    val host: String = "localhost",
    /** Port for TCP connections (ignored for UDS) */
    val port: Int = 50051,
    /** Unix domain socket path for UDS connections */
    val socketPath: String = "/data/local/tmp/voiceos.sock",
    /** Connection timeout in milliseconds */
    val connectionTimeoutMs: Long = 5000,
    /** Request timeout in milliseconds */
    val requestTimeoutMs: Long = 30000,
    /** Enable automatic reconnection */
    val autoReconnect: Boolean = true,
    /** Maximum retry attempts for failed calls */
    val maxRetries: Int = 3,
    /** Initial backoff delay in milliseconds */
    val initialBackoffMs: Long = 100,
    /** Maximum backoff delay in milliseconds */
    val maxBackoffMs: Long = 10000,
    /** Backoff multiplier for exponential backoff */
    val backoffMultiplier: Double = 2.0,
    /** Enable TLS for TCP connections */
    val useTls: Boolean = false,
    /** Keep-alive interval in seconds */
    val keepAliveIntervalSec: Long = 30,
    /** Keep-alive timeout in seconds */
    val keepAliveTimeoutSec: Long = 10
) {
    enum class ConnectionMode {
        /** Unix Domain Socket - for local on-device communication */
        UDS,
        /** TCP - for remote or cross-device communication */
        TCP
    }
}

/**
 * Connection state for monitoring
 */
sealed class ConnectionState {
    object Disconnected : ConnectionState()
    object Connecting : ConnectionState()
    object Connected : ConnectionState()
    data class Reconnecting(val attempt: Int, val maxAttempts: Int) : ConnectionState()
    data class Error(val message: String, val cause: Throwable? = null) : ConnectionState()
}

/**
 * Base gRPC client providing connection management, retry logic, and error handling.
 *
 * This client serves as the foundation for AVA's communication with VoiceOS services.
 * It supports both local (UDS) and remote (TCP) connections with automatic reconnection
 * and exponential backoff retry logic.
 */
open class AvaGrpcClient(
    private val config: GrpcConnectionConfig,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : Closeable {

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)
    private val channelMutex = Mutex()
    private var channel: ManagedChannel? = null
    private var reconnectJob: Job? = null
    private val retryCounter = AtomicInteger(0)

    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    /**
     * Connect to the gRPC server.
     *
     * @return true if connection successful, false otherwise
     */
    suspend fun connect(): Boolean = withContext(dispatcher) {
        channelMutex.withLock {
            if (channel != null && !channel!!.isShutdown) {
                return@withContext true
            }

            _connectionState.value = ConnectionState.Connecting

            try {
                channel = createChannel()
                _connectionState.value = ConnectionState.Connected
                _isConnected.value = true
                retryCounter.set(0)
                true
            } catch (e: Exception) {
                _connectionState.value = ConnectionState.Error("Connection failed: ${e.message}", e)
                _isConnected.value = false

                if (config.autoReconnect) {
                    startReconnection()
                }
                false
            }
        }
    }

    /**
     * Disconnect from the gRPC server.
     */
    suspend fun disconnect() = withContext(dispatcher) {
        channelMutex.withLock {
            reconnectJob?.cancel()
            reconnectJob = null

            channel?.let { ch ->
                if (!ch.isShutdown) {
                    ch.shutdown()
                    try {
                        ch.awaitTermination(5, TimeUnit.SECONDS)
                    } catch (e: InterruptedException) {
                        ch.shutdownNow()
                    }
                }
            }
            channel = null
            _connectionState.value = ConnectionState.Disconnected
            _isConnected.value = false
        }
    }

    /**
     * Get the managed channel for making gRPC calls.
     * Automatically connects if not connected.
     *
     * @return The managed channel
     * @throws IllegalStateException if connection fails
     */
    suspend fun getChannel(): ManagedChannel = withContext(dispatcher) {
        channelMutex.withLock {
            channel?.let {
                if (!it.isShutdown) return@withContext it
            }
        }

        if (!connect()) {
            throw IllegalStateException("Failed to connect to gRPC server")
        }

        channel ?: throw IllegalStateException("Channel is null after connect")
    }

    /**
     * Execute a gRPC call with automatic retry logic.
     *
     * @param block The suspend function to execute
     * @return The result of the call
     */
    suspend fun <T> withRetry(block: suspend (ManagedChannel) -> T): T = withContext(dispatcher) {
        var lastException: Exception? = null
        var currentDelay = config.initialBackoffMs

        repeat(config.maxRetries) { attempt ->
            try {
                val ch = getChannel()
                return@withContext block(ch)
            } catch (e: StatusException) {
                lastException = e

                // Don't retry on certain status codes
                when (e.status.code) {
                    Status.Code.INVALID_ARGUMENT,
                    Status.Code.NOT_FOUND,
                    Status.Code.ALREADY_EXISTS,
                    Status.Code.PERMISSION_DENIED,
                    Status.Code.UNAUTHENTICATED,
                    Status.Code.UNIMPLEMENTED -> throw e
                    else -> {
                        // Retryable error
                        if (attempt < config.maxRetries - 1) {
                            delay(currentDelay)
                            currentDelay = (currentDelay * config.backoffMultiplier)
                                .toLong()
                                .coerceAtMost(config.maxBackoffMs)
                        }
                    }
                }
            } catch (e: Exception) {
                lastException = e
                if (attempt < config.maxRetries - 1) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * config.backoffMultiplier)
                        .toLong()
                        .coerceAtMost(config.maxBackoffMs)
                }
            }
        }

        throw lastException ?: IllegalStateException("Retry failed with unknown error")
    }

    /**
     * Execute a streaming gRPC call.
     * Note: Streaming calls don't have automatic retry - the caller should
     * handle reconnection at the application level.
     *
     * @param block The function that returns a Flow
     * @return The Flow of responses
     */
    suspend fun <T> withStreaming(block: suspend (ManagedChannel) -> Flow<T>): Flow<T> =
        withContext(dispatcher) {
            val ch = getChannel()
            block(ch)
        }

    private fun createChannel(): ManagedChannel {
        return when (config.mode) {
            GrpcConnectionConfig.ConnectionMode.UDS -> createUdsChannel()
            GrpcConnectionConfig.ConnectionMode.TCP -> createTcpChannel()
        }
    }

    private fun createUdsChannel(): ManagedChannel {
        val eventLoopGroup = EpollEventLoopGroup()

        return NettyChannelBuilder
            .forAddress(DomainSocketAddress(config.socketPath))
            .channelType(EpollDomainSocketChannel::class.java)
            .eventLoopGroup(eventLoopGroup)
            .usePlaintext()
            .keepAliveTime(config.keepAliveIntervalSec, TimeUnit.SECONDS)
            .keepAliveTimeout(config.keepAliveTimeoutSec, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .build()
    }

    private fun createTcpChannel(): ManagedChannel {
        val builder = ManagedChannelBuilder
            .forAddress(config.host, config.port)
            .keepAliveTime(config.keepAliveIntervalSec, TimeUnit.SECONDS)
            .keepAliveTimeout(config.keepAliveTimeoutSec, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)

        if (!config.useTls) {
            builder.usePlaintext()
        }

        return builder.build()
    }

    private fun startReconnection() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            var currentDelay = config.initialBackoffMs
            val maxAttempts = 10 // Maximum reconnection attempts

            repeat(maxAttempts) { attempt ->
                _connectionState.value = ConnectionState.Reconnecting(attempt + 1, maxAttempts)

                delay(currentDelay)

                try {
                    channelMutex.withLock {
                        channel = createChannel()
                        _connectionState.value = ConnectionState.Connected
                        _isConnected.value = true
                    }
                    return@launch
                } catch (e: Exception) {
                    currentDelay = (currentDelay * config.backoffMultiplier)
                        .toLong()
                        .coerceAtMost(config.maxBackoffMs)
                }
            }

            _connectionState.value = ConnectionState.Error("Max reconnection attempts reached")
            _isConnected.value = false
        }
    }

    override fun close() {
        scope.launch { disconnect() }
    }

    companion object {
        /**
         * Create a client configured for local UDS connection.
         */
        fun forLocalConnection(
            socketPath: String = "/data/local/tmp/voiceos.sock"
        ): AvaGrpcClient {
            return AvaGrpcClient(
                GrpcConnectionConfig(
                    mode = GrpcConnectionConfig.ConnectionMode.UDS,
                    socketPath = socketPath
                )
            )
        }

        /**
         * Create a client configured for remote TCP connection.
         */
        fun forRemoteConnection(
            host: String,
            port: Int = 50051,
            useTls: Boolean = false
        ): AvaGrpcClient {
            return AvaGrpcClient(
                GrpcConnectionConfig(
                    mode = GrpcConnectionConfig.ConnectionMode.TCP,
                    host = host,
                    port = port,
                    useTls = useTls
                )
            )
        }
    }
}
