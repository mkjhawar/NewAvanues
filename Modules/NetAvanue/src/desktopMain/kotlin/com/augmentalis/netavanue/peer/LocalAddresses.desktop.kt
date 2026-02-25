package com.augmentalis.netavanue.peer

import java.net.NetworkInterface

actual fun getLocalAddresses(): List<String> {
    return try {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .flatMap { iface ->
                iface.inetAddresses.toList()
                    .filter { !it.isLoopbackAddress && it is java.net.Inet4Address }
                    .map { it.hostAddress ?: "" }
            }
            .filter { it.isNotEmpty() }
    } catch (_: Exception) {
        listOf("0.0.0.0")
    }
}
