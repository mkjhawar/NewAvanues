package com.augmentalis.httpavanue.mdns

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import kotlin.coroutines.coroutineContext

/**
 * JVM mDNS advertiser using [MulticastSocket].
 * Sends periodic announcements to 224.0.0.251:5353.
 */
actual class MdnsAdvertiser actual constructor() {
    private var socket: MulticastSocket? = null
    private var running = false

    actual suspend fun start(service: MdnsService) = withContext(Dispatchers.IO) {
        val group = InetAddress.getByName(DnsMessage.MDNS_ADDRESS)
        val localAddress = InetAddress.getLocalHost()
        val hostIpv4 = localAddress.address

        val message = DnsMessage.advertise(service, hostIpv4)
        val data = message.encode()
        val packet = DatagramPacket(data, data.size, group, DnsMessage.MDNS_PORT)

        socket = MulticastSocket(DnsMessage.MDNS_PORT).apply {
            reuseAddress = true
            joinGroup(group)
        }
        running = true

        // Send initial announcement burst (3 times, 1 second apart per RFC 6762)
        repeat(3) {
            socket?.send(packet)
            delay(1000)
        }

        // Then periodic re-announcements every 60 seconds
        while (coroutineContext.isActive && running) {
            delay(60_000)
            if (running) socket?.send(packet)
        }
    }

    actual fun stop() {
        running = false
        try {
            socket?.leaveGroup(InetAddress.getByName(DnsMessage.MDNS_ADDRESS))
            socket?.close()
        } catch (_: Exception) {}
        socket = null
    }
}
