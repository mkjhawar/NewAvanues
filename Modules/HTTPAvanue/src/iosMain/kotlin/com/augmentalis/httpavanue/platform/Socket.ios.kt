@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.augmentalis.httpavanue.platform

import kotlinx.cinterop.*
import kotlinx.coroutines.*
import okio.*
import platform.posix.*

@OptIn(ExperimentalForeignApi::class)
private fun htons(value: UShort): UShort = ((value.toInt() and 0xFF) shl 8 or (value.toInt() shr 8 and 0xFF)).toUShort()
@OptIn(ExperimentalForeignApi::class)
private fun ntohs(value: UShort): UShort = ((value.toInt() and 0xFF) shl 8 or (value.toInt() shr 8 and 0xFF)).toUShort()

@OptIn(ExperimentalForeignApi::class)
actual class Socket internal constructor(
    private val socketFd: Int, private val config: SocketConfig, private val remoteHost: String, private val remotePort: Int,
) {
    private val bufferedSource: BufferedSource
    private val bufferedSink: BufferedSink
    private var closed = false
    init {
        val optval = nativeHeap.alloc<IntVar>(); optval.value = 1
        if (config.keepAlive) setsockopt(socketFd, SOL_SOCKET, SO_KEEPALIVE, optval.ptr, sizeOf<IntVar>().toUInt())
        if (config.tcpNoDelay) setsockopt(socketFd, IPPROTO_TCP, TCP_NODELAY, optval.ptr, sizeOf<IntVar>().toUInt())
        nativeHeap.free(optval)
        bufferedSource = SocketSource(socketFd).buffer()
        bufferedSink = SocketSink(socketFd).buffer()
    }
    actual companion object {
        actual suspend fun connect(host: String, port: Int, config: SocketConfig): Socket = withContext(Dispatchers.Default) {
            memScoped {
                val fd = socket(AF_INET, SOCK_STREAM, 0)
                if (fd < 0) error("Failed to create socket: ${strerror(errno)?.toKString()}")
                try {
                    val serverAddr = alloc<sockaddr_in>()
                    serverAddr.sin_family = AF_INET.toUByte()
                    serverAddr.sin_port = htons(port.toUShort())
                    val hostEntry = gethostbyname(host) ?: error("Failed to resolve host: $host")
                    memcpy(serverAddr.sin_addr.ptr, hostEntry.pointed.h_addr_list!![0], hostEntry.pointed.h_length.toULong())
                    val result = connect(fd, serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt())
                    if (result < 0) error("Failed to connect to $host:$port: ${strerror(errno)?.toKString()}")
                    Socket(fd, config, host, port)
                } catch (e: Exception) { close(fd); throw e }
            }
        }
    }
    actual fun source() = bufferedSource
    actual fun sink() = bufferedSink
    actual fun close() { if (!closed) { closed = true; try { bufferedSource.close() } catch (_: Exception) {}; try { bufferedSink.close() } catch (_: Exception) {}; close(socketFd) } }
    actual fun isConnected() = !closed && socketFd >= 0
    actual fun remoteAddress() = "$remoteHost:$remotePort"
    actual fun setReadTimeout(timeoutMs: Long) {
        memScoped { val tv = alloc<timeval>(); tv.tv_sec = (timeoutMs / 1000).convert(); tv.tv_usec = ((timeoutMs % 1000) * 1000).convert(); setsockopt(socketFd, SOL_SOCKET, SO_RCVTIMEO, tv.ptr, sizeOf<timeval>().toUInt()) }
    }
}

@OptIn(ExperimentalForeignApi::class)
private class SocketSource(private val fd: Int) : Source {
    private var closed = false
    override fun read(sink: Buffer, byteCount: Long): Long {
        if (closed) return -1; if (byteCount == 0L) return 0
        val buffer = ByteArray(minOf(byteCount, 8192).toInt())
        val bytesRead = buffer.usePinned { pinned -> recv(fd, pinned.addressOf(0), buffer.size.toULong(), 0) }
        return when { bytesRead > 0 -> { sink.write(buffer, 0, bytesRead.toInt()); bytesRead }; bytesRead == 0L -> -1; else -> throw Exception("Read failed: ${strerror(errno)?.toKString()}") }
    }
    override fun timeout() = Timeout.NONE
    override fun close() { closed = true }
}

@OptIn(ExperimentalForeignApi::class)
private class SocketSink(private val fd: Int) : Sink {
    private var closed = false
    override fun write(source: Buffer, byteCount: Long) {
        if (closed) throw Exception("Socket closed")
        var remaining = byteCount
        while (remaining > 0) {
            val toWrite = minOf(remaining, 8192).toInt()
            val buffer = source.readByteArray(toWrite.toLong())
            val sent = buffer.usePinned { pinned -> send(fd, pinned.addressOf(0), buffer.size.toULong(), 0) }
            if (sent < 0) throw Exception("Write failed: ${strerror(errno)?.toKString()}")
            remaining -= sent
        }
    }
    override fun flush() {}
    override fun timeout() = Timeout.NONE
    override fun close() { closed = true }
}

@OptIn(ExperimentalForeignApi::class)
actual class SocketServer actual constructor(private val config: SocketConfig) {
    private var serverFd: Int = -1; private var boundPort: Int = -1
    actual fun bind(port: Int, backlog: Int) {
        memScoped {
            serverFd = socket(AF_INET, SOCK_STREAM, 0)
            if (serverFd < 0) error("Failed to create server socket: ${strerror(errno)?.toKString()}")
            val reuseAddr = alloc<IntVar>(); reuseAddr.value = 1; setsockopt(serverFd, SOL_SOCKET, SO_REUSEADDR, reuseAddr.ptr, sizeOf<IntVar>().toUInt())
            val serverAddr = alloc<sockaddr_in>(); serverAddr.sin_family = AF_INET.toUByte(); serverAddr.sin_port = htons(port.toUShort()); serverAddr.sin_addr.s_addr = INADDR_ANY
            if (bind(serverFd, serverAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt()) < 0) { close(serverFd); serverFd = -1; error("Failed to bind to port $port: ${strerror(errno)?.toKString()}") }
            if (listen(serverFd, backlog) < 0) { close(serverFd); serverFd = -1; error("Failed to listen: ${strerror(errno)?.toKString()}") }
            boundPort = port
        }
    }
    actual suspend fun accept(): Socket = withContext(Dispatchers.Default) {
        if (serverFd < 0) error("Server not bound")
        memScoped {
            val clientAddr = alloc<sockaddr_in>(); val addrLen = alloc<socklen_tVar>(); addrLen.value = sizeOf<sockaddr_in>().toUInt()
            val clientFd = accept(serverFd, clientAddr.ptr.reinterpret(), addrLen.ptr)
            if (clientFd < 0) error("Failed to accept connection: ${strerror(errno)?.toKString()}")
            val addr = clientAddr.sin_addr.s_addr
            val clientHost = "${addr and 0xFFu}.${(addr shr 8) and 0xFFu}.${(addr shr 16) and 0xFFu}.${(addr shr 24) and 0xFFu}"
            Socket(clientFd, config, clientHost, ntohs(clientAddr.sin_port).toInt())
        }
    }
    actual fun close() { if (serverFd >= 0) { close(serverFd); serverFd = -1 }; boundPort = -1 }
    actual fun isBound() = serverFd >= 0
    actual fun localPort() = boundPort
}
