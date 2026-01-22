// Author: Manoj Jhawar
// Purpose: Desktop (JVM) actual implementation of DeviceIdentityFactory

package com.augmentalis.devicemanager

import java.net.InetAddress
import java.net.NetworkInterface
import java.util.UUID

/**
 * Desktop (JVM) implementation of DeviceIdentityFactory
 */
actual object DeviceIdentityFactory {
    private var provider: DeviceIdentityProvider? = null

    /**
     * Create desktop device identity provider
     */
    actual fun create(): DeviceIdentityProvider {
        provider?.let { return it }

        val newProvider = DesktopDeviceIdentityProvider()
        provider = newProvider
        return newProvider
    }
}

/**
 * Desktop (JVM) implementation of DeviceIdentityProvider
 */
internal class DesktopDeviceIdentityProvider : DeviceIdentityProvider {

    override fun getDeviceId(): String {
        // Create stable device ID from hardware characteristics
        val idComponents = mutableListOf<String>()

        // Add hostname
        idComponents.add(getHostname())

        // Add MAC address if available (most stable identifier)
        getMacAddress()?.let { idComponents.add(it) }

        // Add OS info
        idComponents.add(System.getProperty("os.name") ?: "")
        idComponents.add(System.getProperty("user.name") ?: "")

        val combinedString = idComponents.joinToString("-")
        return UUID.nameUUIDFromBytes(combinedString.toByteArray()).toString()
    }

    override fun getFingerprint(): DeviceFingerprint {
        val components = listOf(
            System.getProperty("os.name") ?: "",
            System.getProperty("os.version") ?: "",
            System.getProperty("os.arch") ?: "",
            Runtime.getRuntime().availableProcessors().toString(),
            System.getProperty("user.name") ?: "",
            getHostname()
        )

        val fingerprint = components.joinToString("-").hashCode().toString(16)

        return DeviceFingerprint(
            value = fingerprint,
            type = "system",
            components = listOf("os", "version", "arch", "cores", "user", "hostname"),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun getHostname(): String {
        return try {
            InetAddress.getLocalHost().hostName
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getMacAddress(): String? {
        return try {
            val network = NetworkInterface.getByInetAddress(InetAddress.getLocalHost())
            val mac = network?.hardwareAddress ?: return null

            mac.joinToString(":") { byte ->
                String.format("%02X", byte)
            }
        } catch (e: Exception) {
            null
        }
    }
}
