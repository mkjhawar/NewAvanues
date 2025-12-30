// Author: Manoj Jhawar
// Purpose: Monitor USB device connections and trigger rescans

package com.augmentalis.devicemanager.usb

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * USB Device Monitor
 * Monitors USB device connections and disconnections
 */
class USBDeviceMonitor(private val context: Context) {
    
    companion object {
        private const val TAG = "USBDeviceMonitor"
        private const val DEBOUNCE_DELAY_MS = 500L
    }
    
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var debounceJob: Job? = null
    
    // State flow for USB events
    private val _usbEventFlow = MutableStateFlow<USBEvent?>(null)
    val usbEventFlow: StateFlow<USBEvent?> = _usbEventFlow.asStateFlow()
    
    // Connected USB devices
    private val _connectedDevices = MutableStateFlow<List<USBDeviceInfo>>(emptyList())
    val connectedDevices: StateFlow<List<USBDeviceInfo>> = _connectedDevices.asStateFlow()
    
    // Callbacks for USB events
    private val eventCallbacks = mutableListOf<USBEventCallback>()
    
    /**
     * USB Event data class
     */
    data class USBEvent(
        val type: EventType,
        val device: USBDeviceInfo?,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        enum class EventType {
            DEVICE_ATTACHED,
            DEVICE_DETACHED,
            PERMISSION_GRANTED,
            PERMISSION_DENIED,
            SCAN_COMPLETED
        }
    }
    
    /**
     * USB Device Info
     */
    data class USBDeviceInfo(
        val deviceName: String,
        val vendorId: Int,
        val productId: Int,
        val deviceClass: Int,
        val deviceSubclass: Int,
        val deviceProtocol: Int,
        val manufacturerName: String?,
        val productName: String?,
        val serialNumber: String?,
        val version: String?,
        val interfaceCount: Int,
        val hasPermission: Boolean
    )
    
    /**
     * Callback interface for USB events
     */
    interface USBEventCallback {
        fun onUSBDeviceAttached(device: USBDeviceInfo)
        fun onUSBDeviceDetached(device: USBDeviceInfo)
        fun onUSBPermissionGranted(device: USBDeviceInfo)
        fun onUSBPermissionDenied(device: USBDeviceInfo)
        fun onUSBScanCompleted(devices: List<USBDeviceInfo>)
    }
    
    /**
     * Broadcast receiver for USB events
     */
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let {
                        Log.d(TAG, "USB device attached: ${it.deviceName}")
                        handleDeviceAttached(it)
                    }
                }
                
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    device?.let {
                        Log.d(TAG, "USB device detached: ${it.deviceName}")
                        handleDeviceDetached(it)
                    }
                }
                
                "com.augmentalis.devicemanager.USB_PERMISSION" -> {
                    val device = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    }
                    val permissionGranted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    device?.let {
                        if (permissionGranted) {
                            Log.d(TAG, "USB permission granted for: ${it.deviceName}")
                            handlePermissionGranted(it)
                        } else {
                            Log.d(TAG, "USB permission denied for: ${it.deviceName}")
                            handlePermissionDenied(it)
                        }
                    }
                }
            }
        }
    }
    
    private var isMonitoring = false
    
    /**
     * Start monitoring USB devices
     */
    fun startMonitoring() {
        if (isMonitoring) {
            Log.w(TAG, "Already monitoring USB devices")
            return
        }
        
        // Register broadcast receiver
        val filter = IntentFilter().apply {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            addAction("com.augmentalis.devicemanager.USB_PERMISSION")
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(usbReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(usbReceiver, filter)
        }
        
        isMonitoring = true
        Log.d(TAG, "Started monitoring USB devices")
        
        // Initial scan
        scanConnectedDevices()
    }
    
    /**
     * Stop monitoring USB devices
     */
    fun stopMonitoring() {
        if (!isMonitoring) {
            return
        }
        
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
        
        isMonitoring = false
        debounceJob?.cancel()
        scope.cancel()
        Log.d(TAG, "Stopped monitoring USB devices")
    }
    
    /**
     * Scan currently connected USB devices
     */
    fun scanConnectedDevices() {
        scope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
                    val devices = usbManager.deviceList.values.map { device ->
                        convertToUSBDeviceInfo(device, usbManager)
                    }
                    
                    _connectedDevices.value = devices
                    
                    val event = USBEvent(
                        type = USBEvent.EventType.SCAN_COMPLETED,
                        device = null
                    )
                    _usbEventFlow.value = event
                    
                    // Notify callbacks
                    eventCallbacks.forEach { callback ->
                        callback.onUSBScanCompleted(devices)
                    }
                    
                    Log.d(TAG, "Found ${devices.size} connected USB devices")
                } catch (e: Exception) {
                    Log.e(TAG, "Error scanning USB devices", e)
                }
            }
        }
    }
    
    /**
     * Handle device attached event with debouncing
     */
    private fun handleDeviceAttached(device: UsbDevice) {
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_DELAY_MS)
            
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceInfo = convertToUSBDeviceInfo(device, usbManager)
            
            // Update connected devices list
            val currentDevices = _connectedDevices.value.toMutableList()
            if (!currentDevices.any { it.deviceName == deviceInfo.deviceName }) {
                currentDevices.add(deviceInfo)
                _connectedDevices.value = currentDevices
            }
            
            // Emit event
            val event = USBEvent(
                type = USBEvent.EventType.DEVICE_ATTACHED,
                device = deviceInfo
            )
            _usbEventFlow.value = event
            
            // Notify callbacks
            eventCallbacks.forEach { callback ->
                callback.onUSBDeviceAttached(deviceInfo)
            }
        }
    }
    
    /**
     * Handle device detached event
     */
    private fun handleDeviceDetached(device: UsbDevice) {
        scope.launch {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceInfo = convertToUSBDeviceInfo(device, usbManager)
            
            // Update connected devices list
            val currentDevices = _connectedDevices.value.toMutableList()
            currentDevices.removeAll { it.deviceName == deviceInfo.deviceName }
            _connectedDevices.value = currentDevices
            
            // Emit event
            val event = USBEvent(
                type = USBEvent.EventType.DEVICE_DETACHED,
                device = deviceInfo
            )
            _usbEventFlow.value = event
            
            // Notify callbacks
            eventCallbacks.forEach { callback ->
                callback.onUSBDeviceDetached(deviceInfo)
            }
        }
    }
    
    /**
     * Handle permission granted event
     */
    private fun handlePermissionGranted(device: UsbDevice) {
        scope.launch {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceInfo = convertToUSBDeviceInfo(device, usbManager)
            
            // Emit event
            val event = USBEvent(
                type = USBEvent.EventType.PERMISSION_GRANTED,
                device = deviceInfo
            )
            _usbEventFlow.value = event
            
            // Notify callbacks
            eventCallbacks.forEach { callback ->
                callback.onUSBPermissionGranted(deviceInfo)
            }
        }
    }
    
    /**
     * Handle permission denied event
     */
    private fun handlePermissionDenied(device: UsbDevice) {
        scope.launch {
            val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
            val deviceInfo = convertToUSBDeviceInfo(device, usbManager)
            
            // Emit event
            val event = USBEvent(
                type = USBEvent.EventType.PERMISSION_DENIED,
                device = deviceInfo
            )
            _usbEventFlow.value = event
            
            // Notify callbacks
            eventCallbacks.forEach { callback ->
                callback.onUSBPermissionDenied(deviceInfo)
            }
        }
    }
    
    /**
     * Convert UsbDevice to USBDeviceInfo
     */
    private fun convertToUSBDeviceInfo(device: UsbDevice, usbManager: UsbManager): USBDeviceInfo {
        return USBDeviceInfo(
            deviceName = device.deviceName,
            vendorId = device.vendorId,
            productId = device.productId,
            deviceClass = device.deviceClass,
            deviceSubclass = device.deviceSubclass,
            deviceProtocol = device.deviceProtocol,
            manufacturerName = if (android.os.Build.VERSION.SDK_INT >= 21) device.manufacturerName else null,
            productName = if (android.os.Build.VERSION.SDK_INT >= 21) device.productName else null,
            serialNumber = if (android.os.Build.VERSION.SDK_INT >= 21) device.serialNumber else null,
            version = if (android.os.Build.VERSION.SDK_INT >= 23) device.version else null,
            interfaceCount = device.interfaceCount,
            hasPermission = usbManager.hasPermission(device)
        )
    }
    
    /**
     * Register callback for USB events
     */
    fun registerCallback(callback: USBEventCallback) {
        if (!eventCallbacks.contains(callback)) {
            eventCallbacks.add(callback)
            Log.d(TAG, "Registered USB event callback")
        }
    }
    
    /**
     * Unregister callback for USB events
     */
    fun unregisterCallback(callback: USBEventCallback) {
        eventCallbacks.remove(callback)
        Log.d(TAG, "Unregistered USB event callback")
    }
    
    /**
     * Get list of connected USB-C devices
     */
    fun getUSBCDevices(): List<USBDeviceInfo> {
        return _connectedDevices.value.filter { device ->
            // USB-C devices typically use USB 3.x protocols
            // Check for SuperSpeed USB (USB 3.0+) characteristics
            device.deviceProtocol >= 3 || 
            device.deviceClass == 9 || // Hub class, often USB-C hubs
            device.productName?.contains("USB-C", ignoreCase = true) == true ||
            device.productName?.contains("Type-C", ignoreCase = true) == true
        }
    }
    
    /**
     * Check if any USB-C devices are connected
     */
    fun hasUSBCDevices(): Boolean {
        return getUSBCDevices().isNotEmpty()
    }
}
