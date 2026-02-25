package com.augmentalis.netavanue.ice

import com.avanues.logging.LoggerFactory
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.convert
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import platform.posix.AF_INET
import platform.posix.INADDR_ANY
import platform.posix.IPPROTO_UDP
import platform.posix.SOCK_DGRAM
import platform.posix.SOL_SOCKET
import platform.posix.SO_RCVTIMEO
import platform.posix.bind
import platform.posix.close
import platform.posix.errno
import platform.posix.getsockname
import platform.posix.htons
import platform.posix.inet_ntoa
import platform.posix.ntohs
import platform.posix.recvfrom
import platform.posix.sendto
import platform.posix.setsockopt
import platform.posix.sockaddr_in
import platform.posix.socklen_tVar
import platform.posix.socket
import platform.posix.timeval
import platform.posix.inet_addr

/**
 * iOS implementation of [UdpSocket] using POSIX BSD sockets via `kotlinx.cinterop`.
 *
 * POSIX `recvfrom` / `sendto` are blocking calls. They are dispatched onto
 * [Dispatchers.Default] (Kotlin/Native does not provide Dispatchers.IO).
 * The receive timeout is configured via `SO_RCVTIMEO` so `recvfrom` returns
 * with `EAGAIN`/`EWOULDBLOCK` instead of blocking indefinitely.
 */
@OptIn(ExperimentalForeignApi::class)
actual class UdpSocket actual constructor() {

    private val logger = LoggerFactory.getLogger("UdpSocket")

    /** POSIX file descriptor. -1 means the socket has not been created yet. */
    private var fd: Int = -1

    private var _localPort: Int = -1
    private var _isClosed: Boolean = true

    actual val localPort: Int get() = _localPort
    actual val isClosed: Boolean get() = _isClosed

    actual suspend fun bind(port: Int) {
        withContext(Dispatchers.Default) {
            val sockFd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
            if (sockFd < 0) {
                val err = errno
                logger.e { "socket() failed with errno=$err" }
                throw RuntimeException("Failed to create UDP socket: errno=$err")
            }

            memScoped {
                val addr = alloc<sockaddr_in>()
                addr.sin_family = AF_INET.convert()
                addr.sin_port = htons(port.toUShort())
                addr.sin_addr.s_addr = INADDR_ANY

                val bindResult = bind(
                    sockFd,
                    addr.ptr.reinterpret(),
                    sizeOf<sockaddr_in>().convert(),
                )
                if (bindResult < 0) {
                    val err = errno
                    close(sockFd)
                    logger.e { "bind() failed on port $port with errno=$err" }
                    throw RuntimeException("Failed to bind UDP socket on port $port: errno=$err")
                }

                // Read back the actual bound port (important when port == 0)
                val boundAddr = alloc<sockaddr_in>()
                val addrLen = alloc<socklen_tVar>()
                addrLen.value = sizeOf<sockaddr_in>().convert()
                getsockname(sockFd, boundAddr.ptr.reinterpret(), addrLen.ptr)
                _localPort = ntohs(boundAddr.sin_port).toInt()
            }

            fd = sockFd
            _isClosed = false
            logger.d { "Bound UDP socket on port $_localPort (fd=$fd)" }
        }
    }

    actual suspend fun send(data: ByteArray, host: String, port: Int) {
        val sockFd = fd
        if (sockFd < 0 || _isClosed) {
            throw IllegalStateException("Socket not bound. Call bind() first.")
        }

        withContext(Dispatchers.Default) {
            memScoped {
                val destAddr = alloc<sockaddr_in>()
                destAddr.sin_family = AF_INET.convert()
                destAddr.sin_port = htons(port.toUShort())
                destAddr.sin_addr.s_addr = inet_addr(host)

                val sent = data.usePinned { pinned ->
                    sendto(
                        sockFd,
                        pinned.addressOf(0),
                        data.size.convert(),
                        0,
                        destAddr.ptr.reinterpret(),
                        sizeOf<sockaddr_in>().convert(),
                    )
                }

                if (sent < 0) {
                    val err = errno
                    logger.e { "sendto() failed to $host:$port with errno=$err" }
                    throw RuntimeException("sendto() failed: errno=$err")
                }
            }
        }
    }

    actual suspend fun receive(buffer: ByteArray, timeoutMs: Long): UdpPacket? {
        val sockFd = fd
        if (sockFd < 0 || _isClosed) {
            throw IllegalStateException("Socket not bound. Call bind() first.")
        }

        return withContext(Dispatchers.Default) {
            memScoped {
                // Configure receive timeout via SO_RCVTIMEO
                val tv = alloc<timeval>()
                tv.tv_sec = (timeoutMs / 1000).convert()
                tv.tv_usec = ((timeoutMs % 1000) * 1000).convert()
                setsockopt(
                    sockFd,
                    SOL_SOCKET,
                    SO_RCVTIMEO,
                    tv.ptr,
                    sizeOf<timeval>().convert(),
                )

                val srcAddr = alloc<sockaddr_in>()
                val addrLen = alloc<socklen_tVar>()
                addrLen.value = sizeOf<sockaddr_in>().convert()

                val bytesRead = buffer.usePinned { pinned ->
                    recvfrom(
                        sockFd,
                        pinned.addressOf(0),
                        buffer.size.convert(),
                        0,
                        srcAddr.ptr.reinterpret(),
                        addrLen.ptr,
                    )
                }

                if (bytesRead <= 0) {
                    // Timeout (EAGAIN/EWOULDBLOCK) or error -- return null
                    if (bytesRead < 0 && !_isClosed) {
                        val err = errno
                        // EAGAIN (35 on Darwin) and EWOULDBLOCK are normal timeouts
                        if (err != 35 && err != 11) {
                            logger.w { "recvfrom() returned errno=$err" }
                        }
                    }
                    null
                } else {
                    val remoteIp = inet_ntoa(srcAddr.sin_addr)?.toKString() ?: "0.0.0.0"

                    val remotePort = ntohs(srcAddr.sin_port).toInt()

                    UdpPacket(
                        data = buffer,
                        length = bytesRead.toInt(),
                        remoteHost = remoteIp,
                        remotePort = remotePort,
                    )
                }
            }
        }
    }

    actual fun close() {
        val sockFd = fd
        if (sockFd >= 0 && !_isClosed) {
            close(sockFd)
            _isClosed = true
            fd = -1
            _localPort = -1
            logger.d { "UDP socket closed (fd=$sockFd)" }
        }
    }
}
