@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.augmentalis.httpavanue.mdns

import kotlinx.cinterop.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import platform.posix.*
import kotlin.coroutines.coroutineContext

/**
 * iOS mDNS advertiser using POSIX UDP multicast.
 * Sends mDNS announcements to 224.0.0.251:5353 via sendto().
 */
actual class MdnsAdvertiser actual constructor() {
    private var socketFd: Int = -1
    private var running = false

    actual suspend fun start(service: MdnsService) {
        val hostIpv4 = getLocalIpv4() ?: byteArrayOf(127.toByte(), 0, 0, 1)
        val message = DnsMessage.advertise(service, hostIpv4)
        val data = message.encode()

        socketFd = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)
        if (socketFd < 0) return

        running = true

        memScoped {
            val destAddr = alloc<sockaddr_in>()
            destAddr.sin_family = AF_INET.toUByte()
            destAddr.sin_port = htons(DnsMessage.MDNS_PORT.toUShort())
            inet_aton(DnsMessage.MDNS_ADDRESS, destAddr.sin_addr.ptr)

            // Initial burst
            repeat(3) {
                data.usePinned { pinned ->
                    sendto(socketFd, pinned.addressOf(0), data.size.toULong(), 0,
                        destAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt())
                }
                delay(1000)
            }

            // Periodic re-announcements
            while (coroutineContext.isActive && running) {
                delay(60_000)
                if (running) {
                    data.usePinned { pinned ->
                        sendto(socketFd, pinned.addressOf(0), data.size.toULong(), 0,
                            destAddr.ptr.reinterpret(), sizeOf<sockaddr_in>().toUInt())
                    }
                }
            }
        }
    }

    actual fun stop() {
        running = false
        if (socketFd >= 0) {
            close(socketFd)
            socketFd = -1
        }
    }

    private fun getLocalIpv4(): ByteArray? = memScoped {
        // Simple approach: resolve localhost
        val host = gethostbyname("localhost") ?: return null
        val addr = host.pointed.h_addr_list?.get(0) ?: return null
        val bytes = ByteArray(4)
        for (i in 0 until 4) {
            bytes[i] = addr[i]
        }
        bytes
    }

    private fun htons(value: UShort): UShort =
        ((value.toInt() and 0xFF) shl 8 or (value.toInt() shr 8 and 0xFF)).toUShort()
}
