package com.augmentalis.httpavanue.mdns

/**
 * JS actual for [MdnsAdvertiser] — mDNS is not available in browser environments.
 *
 * Multicast DNS requires raw UDP multicast socket access (224.0.0.251:5353),
 * which browsers cannot perform. Service discovery in web contexts uses
 * alternative mechanisms (WebRTC, server-side discovery, etc.).
 */
actual class MdnsAdvertiser actual constructor() {
    actual suspend fun start(service: MdnsService) {
        throw UnsupportedOperationException(
            "mDNS advertising is not available in browser/JS environments. " +
                "Browsers cannot send multicast UDP packets."
        )
    }

    actual fun stop() {
        // No-op: nothing to stop since start() always throws
    }
}
