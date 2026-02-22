package com.augmentalis.netavanue.peer

import kotlinx.cinterop.*
import platform.posix.*

/**
 * iOS implementation using getifaddrs() POSIX call.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun getLocalAddresses(): List<String> {
    val addresses = mutableListOf<String>()
    memScoped {
        val ifaddrs = alloc<CPointerVar<ifaddrs>>()
        if (getifaddrs(ifaddrs.ptr) == 0) {
            var current = ifaddrs.value
            while (current != null) {
                val addr = current.pointed
                val sa = addr.ifa_addr
                if (sa != null && sa.pointed.sa_family.toInt() == AF_INET) {
                    val sin = sa.reinterpret<sockaddr_in>()
                    val ipBytes = sin.pointed.sin_addr.s_addr
                    val ip = "${ipBytes and 0xFFu}.${(ipBytes shr 8) and 0xFFu}.${(ipBytes shr 16) and 0xFFu}.${(ipBytes shr 24) and 0xFFu}"
                    if (ip != "127.0.0.1" && ip != "0.0.0.0") {
                        addresses.add(ip)
                    }
                }
                current = addr.ifa_next
            }
            freeifaddrs(ifaddrs.value)
        }
    }
    return addresses.ifEmpty { listOf("0.0.0.0") }
}
