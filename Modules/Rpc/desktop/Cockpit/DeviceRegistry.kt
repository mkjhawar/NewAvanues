/**
 * DeviceRegistry.kt - Track connected devices for Cockpit
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2025-12-28
 *
 * Manages device registration, status tracking, and cross-device communication.
 */
package com.augmentalis.rpc.desktop.cockpit

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap

/**
 * Device information stored in registry
 */
data class RegisteredDevice(
    val deviceId: String,
    val name: String,
    val platform: String,
    val version: String,
    var online: Boolean = true,
    var lastSeen: Long = System.currentTimeMillis(),
    val capabilities: MutableMap<String, String> = mutableMapOf()
) {
    fun updateLastSeen() {
        lastSeen = System.currentTimeMillis()
        online = true
    }

    fun markOffline() {
        online = false
    }
}

/**
 * Device event types
 */
enum class DeviceEventType {
    CONNECTED,
    DISCONNECTED,
    STATUS_CHANGE,
    CAPABILITY_UPDATE
}

/**
 * Device event for streaming
 */
data class DeviceEventData(
    val deviceId: String,
    val eventType: DeviceEventType,
    val device: RegisteredDevice?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Registry for managing connected devices across the Cockpit system.
 * Thread-safe implementation using ConcurrentHashMap and coroutine Mutex.
 */
class DeviceRegistry {
    private val devices = ConcurrentHashMap<String, RegisteredDevice>()
    private val mutex = Mutex()

    private val _deviceEvents = MutableSharedFlow<DeviceEventData>(
        extraBufferCapacity = 64,
        replay = 1
    )
    val deviceEvents: Flow<DeviceEventData> = _deviceEvents.asSharedFlow()

    private val _deviceCount = MutableStateFlow(0)
    val deviceCount: Flow<Int> = _deviceCount.asStateFlow()

    private val _onlineDevices = MutableStateFlow<List<RegisteredDevice>>(emptyList())
    val onlineDevices: Flow<List<RegisteredDevice>> = _onlineDevices.asStateFlow()

    /**
     * Register a new device or update existing device info.
     *
     * @param deviceId Unique device identifier
     * @param name Human-readable device name
     * @param platform Device platform (android, ios, desktop, web)
     * @param version App version on the device
     * @param capabilities Device capabilities map
     * @return The registered device
     */
    suspend fun registerDevice(
        deviceId: String,
        name: String,
        platform: String,
        version: String,
        capabilities: Map<String, String> = emptyMap()
    ): RegisteredDevice = mutex.withLock {
        val existingDevice = devices[deviceId]
        val device = if (existingDevice != null) {
            // Update existing device
            existingDevice.apply {
                updateLastSeen()
                this.capabilities.putAll(capabilities)
            }
        } else {
            // Create new device
            RegisteredDevice(
                deviceId = deviceId,
                name = name,
                platform = platform,
                version = version,
                capabilities = capabilities.toMutableMap()
            )
        }

        devices[deviceId] = device
        updateDeviceCounts()

        _deviceEvents.tryEmit(
            DeviceEventData(
                deviceId = deviceId,
                eventType = if (existingDevice != null) DeviceEventType.STATUS_CHANGE else DeviceEventType.CONNECTED,
                device = device
            )
        )

        device
    }

    /**
     * Unregister a device (mark as disconnected).
     *
     * @param deviceId Device to unregister
     * @return True if device was found and unregistered
     */
    suspend fun unregisterDevice(deviceId: String): Boolean = mutex.withLock {
        val device = devices[deviceId] ?: return@withLock false
        device.markOffline()
        updateDeviceCounts()

        _deviceEvents.tryEmit(
            DeviceEventData(
                deviceId = deviceId,
                eventType = DeviceEventType.DISCONNECTED,
                device = device
            )
        )

        true
    }

    /**
     * Remove a device completely from registry.
     *
     * @param deviceId Device to remove
     * @return True if device was found and removed
     */
    suspend fun removeDevice(deviceId: String): Boolean = mutex.withLock {
        val device = devices.remove(deviceId) ?: return@withLock false
        updateDeviceCounts()

        _deviceEvents.tryEmit(
            DeviceEventData(
                deviceId = deviceId,
                eventType = DeviceEventType.DISCONNECTED,
                device = device
            )
        )

        true
    }

    /**
     * Get a device by ID.
     *
     * @param deviceId Device ID to look up
     * @return The device or null if not found
     */
    fun getDevice(deviceId: String): RegisteredDevice? = devices[deviceId]

    /**
     * Get all registered devices.
     *
     * @param onlineOnly If true, only return online devices
     * @return List of devices
     */
    fun getAllDevices(onlineOnly: Boolean = false): List<RegisteredDevice> {
        return if (onlineOnly) {
            devices.values.filter { it.online }
        } else {
            devices.values.toList()
        }
    }

    /**
     * Get devices by platform.
     *
     * @param platform Platform filter (android, ios, desktop, web)
     * @return List of devices on the specified platform
     */
    fun getDevicesByPlatform(platform: String): List<RegisteredDevice> {
        return devices.values.filter { it.platform.equals(platform, ignoreCase = true) }
    }

    /**
     * Update device heartbeat (last seen time).
     *
     * @param deviceId Device to update
     * @return True if device was found and updated
     */
    suspend fun heartbeat(deviceId: String): Boolean = mutex.withLock {
        val device = devices[deviceId] ?: return@withLock false
        val wasOffline = !device.online
        device.updateLastSeen()

        if (wasOffline) {
            updateDeviceCounts()
            _deviceEvents.tryEmit(
                DeviceEventData(
                    deviceId = deviceId,
                    eventType = DeviceEventType.CONNECTED,
                    device = device
                )
            )
        }

        true
    }

    /**
     * Update device capabilities.
     *
     * @param deviceId Device to update
     * @param capabilities New capabilities to add/update
     * @return True if device was found and updated
     */
    suspend fun updateCapabilities(
        deviceId: String,
        capabilities: Map<String, String>
    ): Boolean = mutex.withLock {
        val device = devices[deviceId] ?: return@withLock false
        device.capabilities.putAll(capabilities)
        device.updateLastSeen()

        _deviceEvents.tryEmit(
            DeviceEventData(
                deviceId = deviceId,
                eventType = DeviceEventType.CAPABILITY_UPDATE,
                device = device
            )
        )

        true
    }

    /**
     * Check for stale devices and mark them offline.
     *
     * @param timeoutMs Timeout in milliseconds (default 30 seconds)
     * @return Number of devices marked offline
     */
    suspend fun pruneStaleDevices(timeoutMs: Long = 30_000L): Int = mutex.withLock {
        val now = System.currentTimeMillis()
        var prunedCount = 0

        devices.values.forEach { device ->
            if (device.online && (now - device.lastSeen) > timeoutMs) {
                device.markOffline()
                prunedCount++

                _deviceEvents.tryEmit(
                    DeviceEventData(
                        deviceId = device.deviceId,
                        eventType = DeviceEventType.DISCONNECTED,
                        device = device
                    )
                )
            }
        }

        if (prunedCount > 0) {
            updateDeviceCounts()
        }

        prunedCount
    }

    /**
     * Get device count statistics.
     */
    fun getDeviceStats(): DeviceStats {
        val all = devices.values.toList()
        return DeviceStats(
            total = all.size,
            online = all.count { it.online },
            offline = all.count { !it.online },
            byPlatform = all.groupBy { it.platform }.mapValues { it.value.size }
        )
    }

    /**
     * Clear all devices from registry.
     */
    suspend fun clear() = mutex.withLock {
        devices.clear()
        updateDeviceCounts()
    }

    private fun updateDeviceCounts() {
        _deviceCount.value = devices.size
        _onlineDevices.value = devices.values.filter { it.online }
    }

    companion object {
        const val PLATFORM_ANDROID = "android"
        const val PLATFORM_IOS = "ios"
        const val PLATFORM_DESKTOP = "desktop"
        const val PLATFORM_WEB = "web"

        const val DEFAULT_HEARTBEAT_TIMEOUT_MS = 30_000L
        const val DEFAULT_PRUNE_INTERVAL_MS = 10_000L
    }
}

/**
 * Device statistics summary
 */
data class DeviceStats(
    val total: Int,
    val online: Int,
    val offline: Int,
    val byPlatform: Map<String, Int>
)
