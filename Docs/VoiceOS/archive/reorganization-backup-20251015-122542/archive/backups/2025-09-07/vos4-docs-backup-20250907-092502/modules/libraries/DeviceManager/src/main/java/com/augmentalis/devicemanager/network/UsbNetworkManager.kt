// Author: Manoj Jhawar
// Purpose: USB network and tethering management for VOS4

package com.augmentalis.devicemanager.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * USB Network Manager Component
 * Handles USB tethering, USB-C networking, and USB network interfaces
 * 
 * Architecture:
 * - Receives capabilities from DeviceDetector (centralized detection)
 * - Does NOT perform hardware detection directly
 * - Enables conditional loading based on detected USB capabilities
 * - Provides runtime state management for USB network features
 * 
 * Benefits:
 * - Reduces redundant system calls
 * - Ensures consistent detection logic
 * - Supports conditional instantiation
 * - Improves testability
 */
class UsbNetworkManager(
    private val context: Context,
    private val capabilities: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.DeviceCapabilities
) {
    
    /**
     * Backward compatibility constructor
     * @deprecated Use constructor with DeviceCapabilities parameter
     */
    @Deprecated("Use constructor with DeviceCapabilities parameter for better architecture")
    constructor(context: Context) : this(
        context, 
        com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.getCapabilities(context)
    )
    
    companion object {
        private const val TAG = "UsbNetworkManager"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val _usbNetworkState = MutableStateFlow(UsbNetworkState())
    val usbNetworkState: StateFlow<UsbNetworkState> = _usbNetworkState.asStateFlow()
    
    /**
     * USB Network state
     */
    data class UsbNetworkState(
        val isTetheringActive: Boolean = false,
        val usbNetworkInfo: UsbNetworkInfo? = null,
        val hasUsbCapability: Boolean = false
    )
    
    /**
     * USB Network information
     */
    data class UsbNetworkInfo(
        val interfaceName: String? = null,
        val isConnected: Boolean = false,
        val linkSpeed: Int? = null,
        val ipAddress: String? = null,
        val gateway: String? = null,
        val dns: List<String> = emptyList()
    )
    
    init {
        updateUsbNetworkState()
    }
    
    /**
     * Check if USB tethering is active
     */
    fun isUsbTetheringActive(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networks = connectivityManager.allNetworks
            networks.any { network ->
                val caps = connectivityManager.getNetworkCapabilities(network)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_USB) == true
            }
        } else {
            false
        }
    }
    
    /**
     * Get USB network information
     */
    fun getUsbNetworkInfo(): UsbNetworkInfo? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val networks = connectivityManager.allNetworks
            val usbNetwork = networks.firstOrNull { network ->
                val caps = connectivityManager.getNetworkCapabilities(network)
                caps?.hasTransport(NetworkCapabilities.TRANSPORT_USB) == true
            }
            
            usbNetwork?.let { network ->
                val linkProps = connectivityManager.getLinkProperties(network)
                val caps = connectivityManager.getNetworkCapabilities(network)
                
                UsbNetworkInfo(
                    interfaceName = linkProps?.interfaceName,
                    isConnected = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ?: false,
                    linkSpeed = caps?.linkDownstreamBandwidthKbps?.div(1000), // Convert to Mbps
                    ipAddress = linkProps?.linkAddresses?.firstOrNull()?.address?.hostAddress,
                    gateway = linkProps?.routes?.firstOrNull { it.gateway != null }?.gateway?.hostAddress,
                    dns = linkProps?.dnsServers?.mapNotNull { it.hostAddress } ?: emptyList()
                )
            }
        } else {
            null
        }
    }
    
    /**
     * Update USB network state
     */
    private fun updateUsbNetworkState() {
        try {
            _usbNetworkState.value = UsbNetworkState(
                isTetheringActive = isUsbTetheringActive(),
                usbNetworkInfo = getUsbNetworkInfo(),
                hasUsbCapability = hasUsbNetworkCapability()
            )
            Log.d(TAG, "USB network state updated: ${_usbNetworkState.value}")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating USB network state", e)
        }
    }
    
    /**
     * Check if device has USB network capability using detected capabilities
     * 
     * COT: Uses centralized capability detection instead of direct system calls
     * - Reduces redundant PackageManager queries
     * - Ensures consistent detection logic across components
     * - Supports conditional loading and testing
     */
    private fun hasUsbNetworkCapability(): Boolean {
        return capabilities.hardware.hasUsb || capabilities.hardware.hasUsbAccessory
    }
    
    /**
     * Monitor USB network changes
     */
    fun startMonitoring() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }
    }
    
    /**
     * Stop monitoring USB network changes
     */
    fun stopMonitoring() {
        try {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Network callback was not registered")
        }
    }
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            updateUsbNetworkState()
        }
        
        override fun onLost(network: Network) {
            updateUsbNetworkState()
        }
        
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_USB)) {
                updateUsbNetworkState()
            }
        }
        
        override fun onLinkPropertiesChanged(
            network: Network,
            linkProperties: LinkProperties
        ) {
            updateUsbNetworkState()
        }
    }
    
    /**
     * Get USB interface names
     */
    fun getUsbInterfaces(): List<String> {
        val interfaces = mutableListOf<String>()
        try {
            val networks = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                connectivityManager.allNetworks
            } else {
                emptyArray()
            }
            
            networks.forEach { network ->
                val caps = connectivityManager.getNetworkCapabilities(network)
                if (caps?.hasTransport(NetworkCapabilities.TRANSPORT_USB) == true) {
                    connectivityManager.getLinkProperties(network)?.interfaceName?.let {
                        interfaces.add(it)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting USB interfaces", e)
        }
        return interfaces
    }
    
    /**
     * Refresh USB network state
     */
    fun refresh() {
        updateUsbNetworkState()
        Log.d(TAG, "USB network state refreshed")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        stopMonitoring()
        Log.d(TAG, "USB network manager cleanup completed")
    }
}