package com.augmentalis.httpavanue.mdns

/**
 * mDNS service descriptor — represents a service to advertise on the local network.
 *
 * @param name Human-readable service name (e.g., "My Web Server")
 * @param type Service type (e.g., "_http._tcp")
 * @param domain Domain (typically "local")
 * @param port Port the service listens on
 * @param txt TXT record key-value pairs for service metadata
 */
data class MdnsService(
    val name: String,
    val type: String = "_http._tcp",
    val domain: String = "local",
    val port: Int,
    val txt: Map<String, String> = emptyMap(),
) {
    /** Fully qualified service name: "My Web Server._http._tcp.local." */
    val fullName: String get() = "$name.$type.$domain."

    /** Service type domain: "_http._tcp.local." */
    val typeDomain: String get() = "$type.$domain."
}
