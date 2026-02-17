package com.augmentalis.webavanue

import platform.Network.*
import platform.SystemConfiguration.*
import kotlinx.cinterop.*

/**
 * iOS implementation of NetworkChecker using Network.framework
 *
 * Provides network connectivity checking for WiFi/cellular detection
 */
actual class NetworkChecker {

    actual fun isWiFiConnected(): Boolean {
        return getNetworkType() == NetworkType.WIFI
    }

    actual fun isCellularConnected(): Boolean {
        return getNetworkType() == NetworkType.CELLULAR
    }

    actual fun isConnected(): Boolean {
        return getNetworkType() != NetworkType.NONE
    }

    actual fun getWiFiRequiredMessage(): String? {
        return when (getNetworkType()) {
            NetworkType.CELLULAR -> "WiFi required. Currently on Cellular."
            NetworkType.NONE -> "WiFi required. No network connection."
            NetworkType.WIFI -> null
            NetworkType.OTHER -> "WiFi required. Currently on non-WiFi network."
        }
    }

    private enum class NetworkType {
        WIFI, CELLULAR, OTHER, NONE
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun getNetworkType(): NetworkType {
        memScoped {
            val zeroAddress = alloc<sockaddr_in>()
            zeroAddress.sin_len = sizeOf<sockaddr_in>().toUByte()
            zeroAddress.sin_family = AF_INET.toUByte()

            val reachability = SCNetworkReachabilityCreateWithAddress(
                null,
                zeroAddress.ptr.reinterpret()
            ) ?: return NetworkType.NONE

            val flags = alloc<SCNetworkReachabilityFlagsVar>()
            val success = SCNetworkReachabilityGetFlags(reachability, flags.ptr)

            CFRelease(reachability)

            if (!success) {
                return NetworkType.NONE
            }

            val flagValue = flags.value

            // Check if reachable
            val isReachable = (flagValue and kSCNetworkReachabilityFlagsReachable.toUInt()) != 0u
            if (!isReachable) {
                return NetworkType.NONE
            }

            // Check if on cellular
            val isWWAN = (flagValue and kSCNetworkReachabilityFlagsIsWWAN.toUInt()) != 0u
            if (isWWAN) {
                return NetworkType.CELLULAR
            }

            // Check if WiFi (not requiring connection and not on cellular)
            val needsConnection = (flagValue and kSCNetworkReachabilityFlagsConnectionRequired.toUInt()) != 0u
            if (!needsConnection) {
                return NetworkType.WIFI
            }

            return NetworkType.OTHER
        }
    }
}
