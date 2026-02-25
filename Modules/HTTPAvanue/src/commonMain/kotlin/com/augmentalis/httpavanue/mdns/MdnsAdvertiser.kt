package com.augmentalis.httpavanue.mdns

import com.augmentalis.httpavanue.server.HttpServer

/**
 * mDNS service advertiser — broadcasts service presence on the local network
 * via multicast DNS (RFC 6762) at 224.0.0.251:5353.
 *
 * Platform implementations:
 * - JVM (Android + Desktop): java.net.MulticastSocket
 * - iOS: platform.posix.sendto to multicast address
 */
expect class MdnsAdvertiser() {
    /** Start advertising the service. Sends periodic announcements. */
    suspend fun start(service: MdnsService)

    /** Stop advertising and release network resources. */
    fun stop()
}

/** Extension: advertise this HTTP server on the local network via mDNS. */
fun HttpServer.advertise(
    name: String,
    txt: Map<String, String> = emptyMap(),
): MdnsAdvertiser {
    val service = MdnsService(
        name = name,
        type = "_http._tcp",
        port = this.port,
        txt = txt,
    )
    return MdnsAdvertiser().also { advertiser ->
        // Caller should launch { advertiser.start(service) } in their scope
        // The advertiser is returned so the caller can stop it later
    }
}
