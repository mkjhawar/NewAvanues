// Copyright (c) 2025 Augmentalis, Inc.
// Author: Manoj Jhawar
// Purpose: Public API for Bluetooth operations

package com.augmentalis.devicemanager.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
import com.augmentalis.devicemanager.network.BluetoothManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Public API for Bluetooth operations
 *
 * Provides a simplified facade for Bluetooth functionality:
 * - Checking if Bluetooth is enabled
 * - Getting list of connected devices
 * - Starting device discovery
 * - Getting device information (name, address, type)
 * - Monitoring Bluetooth state changes
 *
 * This class wraps the complex internal BluetoothManager to provide a clean,
 * easy-to-use public interface for consuming applications.
 *
 * Requirements:
 * - Android 12+: BLUETOOTH_SCAN, BLUETOOTH_CONNECT permissions
 * - Android <12: BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION permissions
 *
 * Usage:
 * ```kotlin
 * val btAPI = BluetoothPublicAPI(context)
 *
 * if (btAPI.isEnabled()) {
 *     val devices = btAPI.getConnectedDevices()
 *     btAPI.startDiscovery()
 * }
 * ```
 *
 * @param context Android application context
 * @param capabilities Device capabilities from DeviceDetector
 */
class BluetoothPublicAPI(
    private val context: Context,
    private val capabilities: DeviceDetector.DeviceCapabilities
) {

    companion object {
        private const val TAG = "BluetoothPublicAPI"
    }

    // Internal Bluetooth manager
    private val bluetoothManager: BluetoothManager by lazy {
        BluetoothManager(context, capabilities)
    }

    // Coroutine scope for API operations
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ========== PUBLIC DATA CLASSES ==========

    /**
     * Simplified device information
     */
    data class DeviceInfo(
        val name: String,
        val address: String,
        val type: DeviceType,
        val rssi: Int?,
        val isConnected: Boolean,
        val isPaired: Boolean
    )

    /**
     * Device type classification
     */
    enum class DeviceType {
        CLASSIC,    // Bluetooth Classic (BR/EDR)
        LE,         // Bluetooth Low Energy
        DUAL,       // Supports both Classic and LE
        UNKNOWN
    }

    /**
     * Bluetooth state
     */
    data class BluetoothStatus(
        val isEnabled: Boolean,
        val isDiscovering: Boolean,
        val connectedDeviceCount: Int,
        val bluetoothVersion: String,
        val supportsLE: Boolean
    )

    /**
     * Device class for categorization
     */
    enum class DeviceClass {
        PHONE,
        COMPUTER,
        AUDIO,          // Headphones, speakers
        WEARABLE,       // Smartwatches, fitness trackers
        HEALTH,         // Health monitoring devices
        PERIPHERAL,     // Keyboards, mice
        UNKNOWN
    }

    // ========== PUBLIC API METHODS ==========

    /**
     * Checks if Bluetooth is enabled on the device
     *
     * @return true if Bluetooth is enabled, false otherwise
     */
    fun isEnabled(): Boolean {
        return try {
            bluetoothManager.isBluetoothEnabled()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth enabled state", e)
            false
        }
    }

    /**
     * Checks if Bluetooth is supported on the device
     *
     * @return true if Bluetooth hardware is present
     */
    fun isSupported(): Boolean {
        return try {
            bluetoothManager.isBluetoothSupported()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking Bluetooth support", e)
            false
        }
    }

    /**
     * Checks if Bluetooth Low Energy (BLE) is supported
     *
     * @return true if BLE is supported
     */
    fun isBLESupported(): Boolean {
        return try {
            bluetoothManager.isBLESupported()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking BLE support", e)
            false
        }
    }

    /**
     * Gets list of currently connected Bluetooth devices
     *
     * Requires BLUETOOTH_CONNECT permission (Android 12+)
     * or BLUETOOTH permission (Android <12)
     *
     * @return List of connected devices, empty list if none or error
     */
    @RequiresPermission(
        anyOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH
        ]
    )
    fun getConnectedDevices(): List<DeviceInfo> {
        return try {
            bluetoothManager.connectedDevices.value.map { connected ->
                DeviceInfo(
                    name = connected.device.name ?: "Unknown",
                    address = connected.device.address,
                    type = mapDeviceType(connected.device.type),
                    rssi = connected.device.rssi,
                    isConnected = true,
                    isPaired = connected.device.bondState == com.augmentalis.devicemanager.network.BluetoothManager.BondState.BONDED
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting connected devices", e)
            emptyList()
        }
    }

    /**
     * Gets list of paired (bonded) Bluetooth devices
     *
     * Requires BLUETOOTH_CONNECT permission (Android 12+)
     * or BLUETOOTH permission (Android <12)
     *
     * @return List of paired devices, empty list if none or error
     */
    @SuppressLint("MissingPermission")
    @RequiresPermission(
        anyOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH
        ]
    )
    fun getPairedDevices(): List<DeviceInfo> {
        return try {
            bluetoothManager.getPairedDevices().map { device ->
                DeviceInfo(
                    name = device.name ?: "Unknown",
                    address = device.address,
                    type = mapDeviceType(device.type),
                    rssi = device.rssi,
                    isConnected = false, // Paired but not necessarily connected
                    isPaired = true
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting paired devices", e)
            emptyList()
        }
    }

    /**
     * Starts Bluetooth device discovery
     *
     * Discovers both Classic and BLE devices in the vicinity.
     * Discovery results are available via discoveredDevicesFlow.
     *
     * Requires:
     * - BLUETOOTH_SCAN permission (Android 12+)
     * - BLUETOOTH_ADMIN and ACCESS_FINE_LOCATION permissions (Android <12)
     *
     * @param duration Discovery duration in milliseconds (default 12 seconds)
     */
    @RequiresPermission(
        allOf = [
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        ]
    )
    fun startDiscovery(duration: Long = 12000) {
        try {
            bluetoothManager.startDiscovery(
                includeClassic = true,
                includeBLE = true,
                duration = duration
            )
            Log.i(TAG, "Bluetooth discovery started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Missing permission for Bluetooth discovery", e)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting Bluetooth discovery", e)
        }
    }

    /**
     * Stops ongoing Bluetooth device discovery
     *
     * Requires BLUETOOTH_SCAN permission (Android 12+)
     * or BLUETOOTH_ADMIN permission (Android <12)
     */
    @RequiresPermission(
        anyOf = [
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADMIN
        ]
    )
    fun stopDiscovery() {
        try {
            bluetoothManager.stopDiscovery()
            Log.i(TAG, "Bluetooth discovery stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping Bluetooth discovery", e)
        }
    }

    /**
     * Gets device information by address
     *
     * @param address Bluetooth device MAC address (e.g., "00:11:22:33:44:55")
     * @return Device information, or null if not found
     */
    @RequiresPermission(
        anyOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH
        ]
    )
    fun getDeviceInfo(address: String): DeviceInfo? {
        return try {
            // Check in connected devices first
            getConnectedDevices().find { it.address == address }
                ?: getPairedDevices().find { it.address == address }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting device info for $address", e)
            null
        }
    }

    /**
     * Gets current Bluetooth status
     *
     * @return Current Bluetooth status information
     */
    fun getStatus(): BluetoothStatus {
        return try {
            val state = bluetoothManager.bluetoothState.value
            BluetoothStatus(
                isEnabled = state.isEnabled,
                isDiscovering = state.isDiscovering,
                connectedDeviceCount = bluetoothManager.connectedDevices.value.size,
                bluetoothVersion = state.bluetoothVersion.name,
                supportsLE = capabilities.bluetooth?.hasBLE ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Bluetooth status", e)
            BluetoothStatus(
                isEnabled = false,
                isDiscovering = false,
                connectedDeviceCount = 0,
                bluetoothVersion = "UNKNOWN",
                supportsLE = false
            )
        }
    }

    /**
     * Gets the Bluetooth version supported by the device
     *
     * @return Bluetooth version string (e.g., "5.0", "4.2")
     */
    fun getBluetoothVersion(): String {
        return try {
            capabilities.bluetooth?.bluetoothVersion ?: "UNKNOWN"
        } catch (e: Exception) {
            Log.e(TAG, "Error getting Bluetooth version", e)
            "UNKNOWN"
        }
    }

    /**
     * Enables Bluetooth on the device
     *
     * Note: On Android 13+ (API 33), this requires user interaction.
     * The method will launch system settings instead of enabling directly.
     *
     * Requires BLUETOOTH_CONNECT permission (Android 12+)
     * or BLUETOOTH_ADMIN permission (Android <12)
     *
     * @return true if Bluetooth was enabled (or settings launched), false otherwise
     */
    @RequiresPermission(
        anyOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN
        ]
    )
    fun enableBluetooth(): Boolean {
        return try {
            bluetoothManager.enableBluetooth()
        } catch (e: Exception) {
            Log.e(TAG, "Error enabling Bluetooth", e)
            false
        }
    }

    /**
     * Disables Bluetooth on the device
     *
     * Note: On Android 13+ (API 33), this requires user interaction.
     *
     * Requires BLUETOOTH_CONNECT permission (Android 12+)
     * or BLUETOOTH_ADMIN permission (Android <12)
     *
     * @return true if Bluetooth was disabled, false otherwise
     */
    @RequiresPermission(
        anyOf = [
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN
        ]
    )
    fun disableBluetooth(): Boolean {
        return try {
            bluetoothManager.disableBluetooth()
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling Bluetooth", e)
            false
        }
    }

    // ========== REACTIVE STREAMS ==========

    /**
     * Flow of Bluetooth state changes
     *
     * Emits updates when Bluetooth state changes (enabled/disabled, discovering, etc.)
     *
     * Usage:
     * ```kotlin
     * btAPI.bluetoothStateFlow.collect { status ->
     *     println("Bluetooth enabled: ${status.isEnabled}")
     * }
     * ```
     */
    val bluetoothStateFlow: Flow<BluetoothStatus> = bluetoothManager.bluetoothState.map { state ->
        BluetoothStatus(
            isEnabled = state.isEnabled,
            isDiscovering = state.isDiscovering,
            connectedDeviceCount = bluetoothManager.connectedDevices.value.size,
            bluetoothVersion = state.bluetoothVersion.name,
            supportsLE = capabilities.bluetooth?.hasBLE ?: false
        )
    }

    /**
     * Flow of discovered Bluetooth devices
     *
     * Emits devices as they are discovered during a discovery scan.
     *
     * Usage:
     * ```kotlin
     * btAPI.discoveredDevicesFlow.collect { devices ->
     *     println("Found ${devices.size} devices")
     * }
     * ```
     */
    val discoveredDevicesFlow: Flow<List<DeviceInfo>> = bluetoothManager.discoveredDevices.map { devices ->
        devices.map { device ->
            DeviceInfo(
                name = device.name ?: "Unknown",
                address = device.address,
                type = mapDeviceType(device.type),
                rssi = device.rssi,
                isConnected = false,
                isPaired = device.bondState == com.augmentalis.devicemanager.network.BluetoothManager.BondState.BONDED
            )
        }
    }

    /**
     * Flow of connected Bluetooth devices
     *
     * Emits updates when devices connect or disconnect.
     *
     * Usage:
     * ```kotlin
     * btAPI.connectedDevicesFlow.collect { devices ->
     *     println("Connected devices: ${devices.size}")
     * }
     * ```
     */
    val connectedDevicesFlow: Flow<List<DeviceInfo>> = bluetoothManager.connectedDevices.map { devices ->
        devices.map { connected ->
            DeviceInfo(
                name = connected.device.name ?: "Unknown",
                address = connected.device.address,
                type = mapDeviceType(connected.device.type),
                rssi = connected.device.rssi,
                isConnected = true,
                isPaired = connected.device.bondState == com.augmentalis.devicemanager.network.BluetoothManager.BondState.BONDED
            )
        }
    }

    // ========== HELPER METHODS ==========

    /**
     * Maps internal device type to public API device type
     */
    private fun mapDeviceType(type: com.augmentalis.devicemanager.network.BluetoothManager.DeviceType): DeviceType {
        return when (type) {
            com.augmentalis.devicemanager.network.BluetoothManager.DeviceType.CLASSIC -> DeviceType.CLASSIC
            com.augmentalis.devicemanager.network.BluetoothManager.DeviceType.LE -> DeviceType.LE
            com.augmentalis.devicemanager.network.BluetoothManager.DeviceType.DUAL -> DeviceType.DUAL
            com.augmentalis.devicemanager.network.BluetoothManager.DeviceType.UNKNOWN -> DeviceType.UNKNOWN
        }
    }

    // ========== CLEANUP ==========

    /**
     * Cleans up resources
     *
     * Call this when the API instance is no longer needed.
     */
    fun dispose() {
        try {
            bluetoothManager.cleanup()
            scope.cancel()
            Log.d(TAG, "Bluetooth Public API disposed")
        } catch (e: Exception) {
            Log.e(TAG, "Error disposing Bluetooth Public API", e)
        }
    }

    // ========== UTILITY METHODS ==========

    /**
     * Returns a human-readable summary of Bluetooth status
     */
    fun getStatusSummary(): String = buildString {
        val status = getStatus()
        appendLine("Bluetooth Status:")
        appendLine("  Supported: ${isSupported()}")
        appendLine("  Enabled: ${status.isEnabled}")
        appendLine("  Version: ${status.bluetoothVersion}")
        appendLine("  BLE Supported: ${status.supportsLE}")
        appendLine("  Discovering: ${status.isDiscovering}")
        appendLine("  Connected Devices: ${status.connectedDeviceCount}")

        if (status.isEnabled) {
            try {
                val connected = getConnectedDevices()
                if (connected.isNotEmpty()) {
                    appendLine("\nConnected Devices:")
                    connected.forEach { device ->
                        appendLine("  - ${device.name} (${device.address})")
                        appendLine("    Type: ${device.type}, Paired: ${device.isPaired}")
                    }
                }
            } catch (e: SecurityException) {
                appendLine("\n(Permission required to list connected devices)")
            }
        }
    }
}
