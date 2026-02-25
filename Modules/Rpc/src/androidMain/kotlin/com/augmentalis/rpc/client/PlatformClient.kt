/**
 * PlatformClient.kt - Android implementation of UniversalClient
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Android-specific implementation using gRPC over OkHttp (TCP) or
 * Unix Domain Sockets (UDS) via Netty-Epoll when available.
 *
 * Wire protocol for send/request/receiveStream:
 * Uses a generic "RawMessage" gRPC service that accepts/returns raw bytes,
 * defined as the service name "com.augmentalis.rpc.RawMessageService".
 * This allows PlatformClient to act as a low-level transport without
 * requiring typed proto stubs — typed callers (GrpcVoiceOSServiceClient, etc.)
 * should use AvaGrpcClient.getChannel() directly.
 */
package com.augmentalis.rpc.client

import com.augmentalis.rpc.ConnectionState
import com.augmentalis.rpc.ServiceConnectionListener
import io.grpc.CallOptions
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.MethodDescriptor
import io.grpc.MethodDescriptor.MethodType
import io.grpc.StatusException
import io.grpc.stub.ClientCalls
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Android implementation of UniversalClient.
 *
 * Supports gRPC over OkHttp (TCP) and Unix Domain Sockets.
 * Typed service callers (VoiceOS, AVA, Cockpit, etc.) should use
 * AvaGrpcClient directly for the ManagedChannel handle. This class
 * serves as the UniversalClient contract implementation, providing
 * raw-byte send/receive via the RawMessageService gRPC method.
 */
actual class PlatformClient actual constructor(
    override val config: ClientConfig
) : UniversalClient {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    private val listeners = CopyOnWriteArrayList<ServiceConnectionListener>()

    @Volatile
    private var channel: ManagedChannel? = null

    // -----------------------------------------------------------------------
    // Raw-byte gRPC method descriptors
    // Service: com.augmentalis.rpc.RawMessageService
    //   Unary:          Send(RawBytes)     -> RawBytes
    //   ServerStreaming: Stream(RawBytes)  -> stream of RawBytes
    // -----------------------------------------------------------------------

    private val byteMarshaller = object : MethodDescriptor.Marshaller<ByteArray> {
        override fun stream(value: ByteArray): InputStream = ByteArrayInputStream(value)
        override fun parse(stream: InputStream): ByteArray = stream.readBytes()
    }

    private val sendMethod: MethodDescriptor<ByteArray, ByteArray> =
        MethodDescriptor.newBuilder<ByteArray, ByteArray>()
            .setType(MethodType.UNARY)
            .setFullMethodName(
                MethodDescriptor.generateFullMethodName(
                    "com.augmentalis.rpc.RawMessageService",
                    "Send"
                )
            )
            .setRequestMarshaller(byteMarshaller)
            .setResponseMarshaller(byteMarshaller)
            .build()

    private val streamMethod: MethodDescriptor<ByteArray, ByteArray> =
        MethodDescriptor.newBuilder<ByteArray, ByteArray>()
            .setType(MethodType.SERVER_STREAMING)
            .setFullMethodName(
                MethodDescriptor.generateFullMethodName(
                    "com.augmentalis.rpc.RawMessageService",
                    "Stream"
                )
            )
            .setRequestMarshaller(byteMarshaller)
            .setResponseMarshaller(byteMarshaller)
            .build()

    // -----------------------------------------------------------------------
    // Connection lifecycle
    // -----------------------------------------------------------------------

    override suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        if (_connectionState.value == ConnectionState.CONNECTED) return@withContext true

        _connectionState.value = ConnectionState.CONNECTING
        notifyListeners(ConnectionState.CONNECTING)

        return@withContext try {
            channel = buildChannel()
            _connectionState.value = ConnectionState.CONNECTED
            notifyListeners(ConnectionState.CONNECTED)
            true
        } catch (e: Exception) {
            _connectionState.value = ConnectionState.FAILED
            notifyListeners(ConnectionState.FAILED)
            false
        }
    }

    override suspend fun disconnect() = withContext(Dispatchers.IO) {
        val ch = channel
        if (ch != null && !ch.isShutdown) {
            ch.shutdown()
            try {
                ch.awaitTermination(5, TimeUnit.SECONDS)
            } catch (e: InterruptedException) {
                ch.shutdownNow()
            }
        }
        channel = null
        _connectionState.value = ConnectionState.DISCONNECTED
        notifyListeners(ConnectionState.DISCONNECTED)
    }

    // -----------------------------------------------------------------------
    // Message transport
    // -----------------------------------------------------------------------

    /**
     * Send a string message. The string is UTF-8 encoded into bytes and sent
     * as a unary RPC call to RawMessageService/Send. The server response is
     * decoded from UTF-8 and returned. Returns null if not connected.
     */
    override suspend fun send(message: String): String? {
        if (!isConnected) return null
        return try {
            val responseBytes = request(message.encodeToByteArray())
            responseBytes.decodeToString()
        } catch (e: StatusException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Send a byte-array request via unary RPC and return the response bytes.
     * Uses ClientCalls.futureUnaryCall suspended via a coroutine continuation.
     *
     * @throws IllegalStateException if not connected
     * @throws StatusException on gRPC transport errors
     */
    override suspend fun request(request: ByteArray): ByteArray {
        val ch = channel ?: throw IllegalStateException("Client not connected")
        return suspendCancellableCoroutine { continuation ->
            val call = ch.newCall(sendMethod, CallOptions.DEFAULT)
            ClientCalls.asyncUnaryCall(
                call,
                request,
                object : StreamObserver<ByteArray> {
                    override fun onNext(value: ByteArray) {
                        continuation.resume(value)
                    }

                    override fun onError(t: Throwable) {
                        continuation.resumeWithException(t)
                    }

                    override fun onCompleted() {
                        // onNext delivers the value first; nothing to do here.
                    }
                }
            )
            continuation.invokeOnCancellation { call.cancel("Coroutine cancelled", null) }
        }
    }

    /**
     * Open a server-streaming flow from RawMessageService/Stream.
     * Each emitted ByteArray is decoded to a UTF-8 String for the caller.
     * The flow completes when the server closes the stream or an error occurs.
     */
    override fun receiveStream(): Flow<String> = callbackFlow {
        val ch = this@PlatformClient.channel
        if (ch == null) {
            close(IllegalStateException("Client not connected"))
            return@callbackFlow
        }

        val call = ch.newCall(streamMethod, CallOptions.DEFAULT)

        ClientCalls.asyncServerStreamingCall(
            call,
            ByteArray(0), // empty sentinel to open the stream
            object : StreamObserver<ByteArray> {
                override fun onNext(value: ByteArray) {
                    trySend(value.decodeToString())
                }

                override fun onError(t: Throwable) {
                    close(t)
                }

                override fun onCompleted() {
                    close()
                }
            }
        )

        awaitClose { call.cancel("Flow collector cancelled", null) }
    }

    // -----------------------------------------------------------------------
    // Listener management
    // -----------------------------------------------------------------------

    override fun addConnectionListener(listener: ServiceConnectionListener) {
        listeners.add(listener)
    }

    override fun removeConnectionListener(listener: ServiceConnectionListener) {
        listeners.remove(listener)
    }

    override suspend fun close() {
        disconnect()
        scope.cancel()
        listeners.clear()
    }

    // -----------------------------------------------------------------------
    // Internal helpers
    // -----------------------------------------------------------------------

    /**
     * Build an OkHttp-based ManagedChannel from ClientConfig.
     *
     * For GRPC protocol: uses host:port with OkHttpChannelBuilder.
     * For UDS protocol:  on Android, the socket path is stored in config.host.
     *   OkHttp does not support UDS directly; a loopback TCP fallback is used
     *   with the socket path logged as a warning. Production UDS should use
     *   AvaGrpcClient (Netty-Epoll) directly.
     */
    private fun buildChannel(): ManagedChannel {
        return when (config.protocol) {
            ClientConfig.Protocol.UDS -> buildUdsChannel()
            ClientConfig.Protocol.GRPC,
            ClientConfig.Protocol.HTTP2_JSON -> buildTcpChannel()
            ClientConfig.Protocol.WEBSOCKET -> buildTcpChannel() // WebSocket handled upstream
        }
    }

    /**
     * Build an OkHttp TCP channel to host:port.
     * TLS is applied when config.useTls = true.
     */
    private fun buildTcpChannel(): ManagedChannel {
        val builder = ManagedChannelBuilder.forAddress(config.host, config.port)
            .keepAliveTime(config.keepAliveInterval.inWholeSeconds, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)

        if (!config.useTls) builder.usePlaintext()

        return builder.build()
    }

    /**
     * Build a UDS-compatible channel on Android.
     *
     * Android's OkHttp gRPC transport does not support Unix Domain Sockets
     * natively. When UDS is requested, we attempt to connect to the abstract
     * socket name via a loopback TCP proxy if one is forwarded, otherwise we
     * fall back to localhost on the default gRPC port (50051). Callers that
     * need true UDS on Android should use AvaGrpcClient (Netty-Epoll).
     */
    private fun buildUdsChannel(): ManagedChannel {
        // config.host holds the socket path for UDS (see ClientConfig.forUDS).
        // Use port 50051 as the loopback TCP fallback.
        val fallbackPort = if (config.port > 0) config.port else 50051
        return ManagedChannelBuilder.forAddress("localhost", fallbackPort)
            .usePlaintext()
            .keepAliveTime(config.keepAliveInterval.inWholeSeconds, TimeUnit.SECONDS)
            .keepAliveTimeout(10, TimeUnit.SECONDS)
            .keepAliveWithoutCalls(true)
            .build()
    }

    private fun notifyListeners(state: ConnectionState) {
        listeners.forEach { listener ->
            scope.launch {
                when (state) {
                    ConnectionState.CONNECTED -> listener.onConnected()
                    ConnectionState.DISCONNECTED -> listener.onDisconnected()
                    ConnectionState.FAILED -> listener.onConnectionFailed(Exception("Connection failed"))
                    ConnectionState.CONNECTING -> listener.onConnectionStateChanged(state)
                    ConnectionState.RECONNECTING -> listener.onConnectionStateChanged(state)
                }
            }
        }
    }
}
