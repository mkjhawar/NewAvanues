/**
 * AndroidTransport.kt - Android Unix Domain Socket transport for UniversalRPC
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Provides fast local IPC on Android using Unix Domain Sockets,
 * matching AIDL latency while supporting cross-platform RPC.
 *
 * Features:
 * - Unix Domain Sockets for same-device communication (sub-millisecond latency)
 * - TCP fallback for cross-device communication
 * - Automatic reconnection with exponential backoff
 * - Thread-safe concurrent operations
 * - Connection lifecycle management
 */
package com.augmentalis.universalrpc.transport

import android.net.LocalSocket
import android.net.LocalSocketAddress
import android.net.LocalServerSocket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket
import java.net.ServerSocket
import java.nio.ByteBuffer
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Base implementation for Android transports with common functionality
 */
abstract class BaseAndroidTransport(
    override val config: TransportConfig
) : Transport {

    protected val _state = MutableStateFlow(TransportState.DISCONNECTED)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    protected val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    protected val listeners = CopyOnWriteArrayList<TransportListener>()
    protected val writeMutex = Mutex()
    protected val readMutex = Mutex()
    protected val connectionMutex = Mutex()

    protected val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    protected val isClosing = AtomicBoolean(false)
    protected val retryCount = AtomicInteger(0)

    protected var inputStream: InputStream? = null
    protected var outputStream: OutputStream? = null

    override fun addListener(listener: TransportListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: TransportListener) {
        listeners.remove(listener)
    }

    protected fun setState(newState: TransportState) {
        val oldState = _state.value
        if (oldState != newState) {
            _state.value = newState
            scope.launch {
                _events.emit(TransportEvent.StateChanged(oldState, newState))
            }
            listeners.forEach { it.onStateChanged(oldState, newState) }
        }
    }

    protected fun emitError(error: Throwable) {
        scope.launch {
            _events.emit(TransportEvent.Error(error))
        }
        listeners.forEach { it.onError(error) }
    }

    protected fun notifyDataReceived(data: ByteArray) {
        scope.launch {
            _events.emit(TransportEvent.DataReceived(data.size))
        }
        listeners.forEach { it.onDataReceived(data) }
    }

    override suspend fun send(data: ByteArray) = writeMutex.withLock {
        if (!isConnected) {
            throw TransportException("Transport not connected", isRecoverable = true)
        }

        try {
            withContext(Dispatchers.IO) {
                val output = outputStream ?: throw TransportException("Output stream not available")

                // Write length-prefixed message
                val lengthBuffer = ByteBuffer.allocate(4).putInt(data.size).array()
                output.write(lengthBuffer)
                output.write(data)
                output.flush()
            }

            _events.emit(TransportEvent.DataSent(data.size))
        } catch (e: IOException) {
            handleConnectionError(e)
            throw TransportException("Failed to send data", e)
        }
    }

    override suspend fun receive(): ByteArray = readMutex.withLock {
        if (!isConnected) {
            throw TransportException("Transport not connected", isRecoverable = true)
        }

        try {
            return withContext(Dispatchers.IO) {
                val input = inputStream ?: throw TransportException("Input stream not available")

                // Read length prefix
                val lengthBuffer = ByteArray(4)
                var bytesRead = 0
                while (bytesRead < 4) {
                    val read = input.read(lengthBuffer, bytesRead, 4 - bytesRead)
                    if (read == -1) {
                        throw TransportException("Connection closed by peer")
                    }
                    bytesRead += read
                }

                val length = ByteBuffer.wrap(lengthBuffer).int
                if (length <= 0 || length > config.bufferSize * 1024) {
                    throw TransportException("Invalid message length: $length")
                }

                // Read message data
                val data = ByteArray(length)
                bytesRead = 0
                while (bytesRead < length) {
                    val read = input.read(data, bytesRead, length - bytesRead)
                    if (read == -1) {
                        throw TransportException("Connection closed while reading data")
                    }
                    bytesRead += read
                }

                notifyDataReceived(data)
                data
            }
        } catch (e: IOException) {
            handleConnectionError(e)
            throw TransportException("Failed to receive data", e)
        }
    }

    protected open suspend fun handleConnectionError(error: Throwable) {
        if (isClosing.get()) return

        setState(TransportState.RECONNECTING)

        if (config.autoReconnect && retryCount.get() < config.maxRetryAttempts) {
            scope.launch {
                attemptReconnect()
            }
        } else {
            setState(TransportState.FAILED)
            emitError(error)
        }
    }

    protected open suspend fun attemptReconnect() {
        val attempt = retryCount.incrementAndGet()
        _events.emit(TransportEvent.ReconnectAttempt(attempt, config.maxRetryAttempts))

        // Exponential backoff
        val delay = config.retryDelayMs * (1L shl (attempt - 1).coerceAtMost(5))
        delay(delay)

        try {
            connect()
            retryCount.set(0)
        } catch (e: Exception) {
            if (attempt >= config.maxRetryAttempts) {
                setState(TransportState.FAILED)
                emitError(TransportException("Max retry attempts reached", e))
            } else {
                attemptReconnect()
            }
        }
    }

    override suspend fun close() {
        isClosing.set(true)
        disconnect(graceful = false)
        scope.cancel()
    }

    protected fun closeStreams() {
        try {
            inputStream?.close()
        } catch (_: IOException) {}
        try {
            outputStream?.close()
        } catch (_: IOException) {}
        inputStream = null
        outputStream = null
    }
}

/**
 * Unix Domain Socket client transport for Android
 *
 * Uses Android's LocalSocket API for fast same-device IPC.
 * Supports both abstract namespace and filesystem-based sockets.
 */
class UnixDomainSocketTransport(
    private val address: TransportAddress.UnixSocket,
    config: TransportConfig = TransportConfig()
) : BaseAndroidTransport(config) {

    private var socket: LocalSocket? = null
    private var keepAliveJob: Job? = null

    override suspend fun connect() = connectionMutex.withLock {
        if (isConnected) return

        setState(TransportState.CONNECTING)

        try {
            withContext(Dispatchers.IO) {
                withTimeout(config.connectionTimeoutMs) {
                    val localSocket = LocalSocket()

                    val socketAddress = LocalSocketAddress(
                        address.path,
                        if (address.abstract) {
                            LocalSocketAddress.Namespace.ABSTRACT
                        } else {
                            LocalSocketAddress.Namespace.FILESYSTEM
                        }
                    )

                    localSocket.connect(socketAddress)
                    localSocket.soTimeout = config.readTimeoutMs.toInt()

                    socket = localSocket
                    inputStream = BufferedInputStream(localSocket.inputStream, config.bufferSize)
                    outputStream = BufferedOutputStream(localSocket.outputStream, config.bufferSize)
                }
            }

            setState(TransportState.CONNECTED)
            startKeepAlive()
        } catch (e: Exception) {
            setState(TransportState.FAILED)
            closeStreams()
            socket?.close()
            socket = null
            throw TransportException("Failed to connect to Unix socket: ${address.path}", e)
        }
    }

    override suspend fun disconnect(graceful: Boolean) {
        stopKeepAlive()

        if (graceful) {
            // Allow pending operations to complete
            delay(100)
        }

        setState(TransportState.CLOSING)
        closeStreams()

        try {
            socket?.close()
        } catch (_: IOException) {}
        socket = null

        setState(TransportState.DISCONNECTED)
    }

    private fun startKeepAlive() {
        if (config.keepAliveIntervalMs <= 0) return

        keepAliveJob = scope.launch {
            while (isActive && isConnected) {
                delay(config.keepAliveIntervalMs)
                try {
                    // Send keep-alive ping (empty message with special length)
                    writeMutex.withLock {
                        outputStream?.let {
                            it.write(ByteBuffer.allocate(4).putInt(0).array())
                            it.flush()
                        }
                    }
                    _events.emit(TransportEvent.KeepAlive)
                } catch (e: Exception) {
                    // Connection lost
                    handleConnectionError(e)
                    break
                }
            }
        }
    }

    private fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }
}

/**
 * TCP Socket client transport for Android
 *
 * Used for cross-device communication when Unix Domain Sockets
 * are not available (different devices on network).
 */
class TcpSocketTransport(
    private val address: TransportAddress.TcpSocket,
    config: TransportConfig = TransportConfig()
) : BaseAndroidTransport(config) {

    private var socket: Socket? = null
    private var keepAliveJob: Job? = null

    override suspend fun connect() = connectionMutex.withLock {
        if (isConnected) return

        setState(TransportState.CONNECTING)

        try {
            withContext(Dispatchers.IO) {
                withTimeout(config.connectionTimeoutMs) {
                    val tcpSocket = Socket()
                    tcpSocket.connect(
                        InetSocketAddress(address.host, address.port),
                        config.connectionTimeoutMs.toInt()
                    )
                    tcpSocket.soTimeout = config.readTimeoutMs.toInt()
                    tcpSocket.tcpNoDelay = true // Disable Nagle's algorithm for low latency
                    tcpSocket.keepAlive = true

                    socket = tcpSocket
                    inputStream = BufferedInputStream(tcpSocket.getInputStream(), config.bufferSize)
                    outputStream = BufferedOutputStream(tcpSocket.getOutputStream(), config.bufferSize)
                }
            }

            setState(TransportState.CONNECTED)
            startKeepAlive()
        } catch (e: Exception) {
            setState(TransportState.FAILED)
            closeStreams()
            socket?.close()
            socket = null
            throw TransportException("Failed to connect to TCP socket: ${address.host}:${address.port}", e)
        }
    }

    override suspend fun disconnect(graceful: Boolean) {
        stopKeepAlive()

        if (graceful) {
            delay(100)
        }

        setState(TransportState.CLOSING)
        closeStreams()

        try {
            socket?.close()
        } catch (_: IOException) {}
        socket = null

        setState(TransportState.DISCONNECTED)
    }

    private fun startKeepAlive() {
        if (config.keepAliveIntervalMs <= 0) return

        keepAliveJob = scope.launch {
            while (isActive && isConnected) {
                delay(config.keepAliveIntervalMs)
                try {
                    writeMutex.withLock {
                        outputStream?.let {
                            it.write(ByteBuffer.allocate(4).putInt(0).array())
                            it.flush()
                        }
                    }
                    _events.emit(TransportEvent.KeepAlive)
                } catch (e: Exception) {
                    handleConnectionError(e)
                    break
                }
            }
        }
    }

    private fun stopKeepAlive() {
        keepAliveJob?.cancel()
        keepAliveJob = null
    }
}

/**
 * Unix Domain Socket server transport for Android
 *
 * Listens for incoming client connections on a Unix Domain Socket.
 */
class UnixDomainSocketServerTransport(
    private val address: TransportAddress.UnixSocket,
    private val config: TransportConfig = TransportConfig()
) : ServerTransport {

    private val _state = MutableStateFlow(TransportState.DISCONNECTED)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    private var serverSocket: LocalServerSocket? = null
    private val connections = CopyOnWriteArrayList<Transport>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isClosing = AtomicBoolean(false)

    override suspend fun start() {
        if (isListening) return

        _state.value = TransportState.CONNECTING

        try {
            withContext(Dispatchers.IO) {
                serverSocket = LocalServerSocket(address.path)
            }
            _state.value = TransportState.CONNECTED
            _events.emit(TransportEvent.StateChanged(TransportState.CONNECTING, TransportState.CONNECTED))
        } catch (e: Exception) {
            _state.value = TransportState.FAILED
            throw TransportException("Failed to start Unix socket server: ${address.path}", e)
        }
    }

    override suspend fun stop(graceful: Boolean) {
        if (!isListening) return

        isClosing.set(true)
        _state.value = TransportState.CLOSING

        if (graceful) {
            // Close all connections gracefully
            connections.forEach {
                try {
                    it.disconnect(graceful = true)
                } catch (_: Exception) {}
            }
        }

        connections.clear()

        try {
            serverSocket?.close()
        } catch (_: IOException) {}
        serverSocket = null

        _state.value = TransportState.DISCONNECTED
    }

    override suspend fun accept(): Transport {
        if (!isListening) {
            throw TransportException("Server not listening")
        }

        return withContext(Dispatchers.IO) {
            val clientSocket = serverSocket?.accept()
                ?: throw TransportException("Server socket closed")

            val clientTransport = AcceptedUnixSocketTransport(clientSocket, config)
            connections.add(clientTransport)

            // Remove from list when disconnected
            scope.launch {
                clientTransport.state.collect { state ->
                    if (state == TransportState.DISCONNECTED || state == TransportState.FAILED) {
                        connections.remove(clientTransport)
                    }
                }
            }

            clientTransport
        }
    }

    override fun getConnections(): List<Transport> = connections.toList()

    override suspend fun close() {
        stop(graceful = false)
        scope.cancel()
    }
}

/**
 * Transport wrapper for accepted client connections
 */
internal class AcceptedUnixSocketTransport(
    private val socket: LocalSocket,
    config: TransportConfig
) : BaseAndroidTransport(config) {

    init {
        try {
            socket.soTimeout = config.readTimeoutMs.toInt()
            inputStream = BufferedInputStream(socket.inputStream, config.bufferSize)
            outputStream = BufferedOutputStream(socket.outputStream, config.bufferSize)
            _state.value = TransportState.CONNECTED
        } catch (e: IOException) {
            _state.value = TransportState.FAILED
        }
    }

    override suspend fun connect() {
        // Already connected via accept()
        if (state.value != TransportState.CONNECTED) {
            throw TransportException("Connection not established")
        }
    }

    override suspend fun disconnect(graceful: Boolean) {
        setState(TransportState.CLOSING)
        closeStreams()

        try {
            socket.close()
        } catch (_: IOException) {}

        setState(TransportState.DISCONNECTED)
    }
}

/**
 * TCP Socket server transport for Android
 *
 * Listens for incoming client connections on a TCP socket.
 * Used for cross-device communication.
 */
class TcpSocketServerTransport(
    private val address: TransportAddress.TcpSocket,
    private val config: TransportConfig = TransportConfig()
) : ServerTransport {

    private val _state = MutableStateFlow(TransportState.DISCONNECTED)
    override val state: StateFlow<TransportState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<TransportEvent>(extraBufferCapacity = 64)
    override val events: Flow<TransportEvent> = _events.asSharedFlow()

    private var serverSocket: ServerSocket? = null
    private val connections = CopyOnWriteArrayList<Transport>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isClosing = AtomicBoolean(false)

    override suspend fun start() {
        if (isListening) return

        _state.value = TransportState.CONNECTING

        try {
            withContext(Dispatchers.IO) {
                val server = ServerSocket()
                server.reuseAddress = true
                server.bind(InetSocketAddress(address.host, address.port))
                serverSocket = server
            }
            _state.value = TransportState.CONNECTED
            _events.emit(TransportEvent.StateChanged(TransportState.CONNECTING, TransportState.CONNECTED))
        } catch (e: Exception) {
            _state.value = TransportState.FAILED
            throw TransportException("Failed to start TCP server: ${address.host}:${address.port}", e)
        }
    }

    override suspend fun stop(graceful: Boolean) {
        if (!isListening) return

        isClosing.set(true)
        _state.value = TransportState.CLOSING

        if (graceful) {
            connections.forEach {
                try {
                    it.disconnect(graceful = true)
                } catch (_: Exception) {}
            }
        }

        connections.clear()

        try {
            serverSocket?.close()
        } catch (_: IOException) {}
        serverSocket = null

        _state.value = TransportState.DISCONNECTED
    }

    override suspend fun accept(): Transport {
        if (!isListening) {
            throw TransportException("Server not listening")
        }

        return withContext(Dispatchers.IO) {
            val clientSocket = serverSocket?.accept()
                ?: throw TransportException("Server socket closed")

            clientSocket.tcpNoDelay = true
            clientSocket.keepAlive = true

            val clientTransport = AcceptedTcpSocketTransport(clientSocket, config)
            connections.add(clientTransport)

            scope.launch {
                clientTransport.state.collect { state ->
                    if (state == TransportState.DISCONNECTED || state == TransportState.FAILED) {
                        connections.remove(clientTransport)
                    }
                }
            }

            clientTransport
        }
    }

    override fun getConnections(): List<Transport> = connections.toList()

    override suspend fun close() {
        stop(graceful = false)
        scope.cancel()
    }
}

/**
 * Transport wrapper for accepted TCP client connections
 */
internal class AcceptedTcpSocketTransport(
    private val socket: Socket,
    config: TransportConfig
) : BaseAndroidTransport(config) {

    init {
        try {
            socket.soTimeout = config.readTimeoutMs.toInt()
            inputStream = BufferedInputStream(socket.getInputStream(), config.bufferSize)
            outputStream = BufferedOutputStream(socket.getOutputStream(), config.bufferSize)
            _state.value = TransportState.CONNECTED
        } catch (e: IOException) {
            _state.value = TransportState.FAILED
        }
    }

    override suspend fun connect() {
        if (state.value != TransportState.CONNECTED) {
            throw TransportException("Connection not established")
        }
    }

    override suspend fun disconnect(graceful: Boolean) {
        setState(TransportState.CLOSING)
        closeStreams()

        try {
            socket.close()
        } catch (_: IOException) {}

        setState(TransportState.DISCONNECTED)
    }
}
