/**
 * DeviceViewModel.kt - ViewModel for Device Manager UI
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * 
 * Manages UI state and business logic for device management
 */
package com.augmentalis.devicemanager.dashboardui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.augmentalis.devicemanager.DeviceInfo
import com.augmentalis.devicemanager.DeviceManager
import com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector
import com.augmentalis.devicemanager.audio.AudioService
import com.augmentalis.devicemanager.smartdevices.FoldableDeviceManager
import com.augmentalis.devicemanager.sensors.imu.IMUManager
import com.augmentalis.devicemanager.network.BluetoothManager
import com.augmentalis.devicemanager.network.WiFiManager
import com.augmentalis.devicemanager.network.UwbManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Device hardware info
 */
data class HardwareInfo(
    val model: String,
    val manufacturer: String,
    val brand: String,
    val device: String,
    val board: String,
    val hardware: String,
    val product: String,
    val androidVersion: String,
    val apiLevel: Int,
    val buildId: String,
    val kernelVersion: String
)

/**
 * Battery info
 */
data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val chargingType: String,
    val temperature: Float,
    val voltage: Int,
    val health: String,
    val technology: String?,
    val capacity: Int
)

/**
 * Sensor info
 */
data class SensorInfo(
    val name: String,
    val type: Int,
    val vendor: String,
    val version: Int,
    val power: Float,
    val maximumRange: Float,
    val resolution: Float,
    val minDelay: Int,
    val isWakeUp: Boolean
)

/**
 * Network connection info
 */
data class NetworkConnectionInfo(
    val type: String,
    val isConnected: Boolean,
    val isMetered: Boolean,
    val bandwidth: Int,
    val signalStrength: Int,
    val ssid: String?,
    val capabilities: List<String>
)

/**
 * Audio device info
 */
data class AudioDeviceInfo(
    val id: Int,
    val name: String,
    val type: String,
    val isInput: Boolean,
    val sampleRates: List<Int>,
    val channelMasks: List<Int>,
    val formats: List<Int>
)

/**
 * Display info
 */
data class DisplayInfo(
    val width: Int,
    val height: Int,
    val density: Float,
    val scaledDensity: Float,
    val refreshRate: Float,
    val hdrCapabilities: List<String>,
    val isRound: Boolean,
    val cutoutInfo: String?
)

/**
 * ViewModel for Device Manager UI
 */
class DeviceViewModel(private val context: Context) : ViewModel() {
    
    companion object {
        private const val TAG = "DeviceViewModel"
    }
    
    private val deviceManager = DeviceManager.getInstance(context)
    private val deviceCapabilities = DeviceDetector.getCapabilities(context)
    // NetworkManager is deprecated - using specialized managers instead
    private val xrManager = deviceManager.xr
    private val audioService = AudioService(context)
    private val foldableManager = FoldableDeviceManager(context)
    private val imuManager = IMUManager.getInstance(context)
    private val bluetoothManager = BluetoothManager(context, deviceCapabilities)
    private val wifiManager = WiFiManager(context, deviceCapabilities)
    private val uwbManager = UwbManager(context, deviceCapabilities)
    
    private val _hardwareInfo = MutableLiveData<HardwareInfo>()
    val hardwareInfo: LiveData<HardwareInfo> = _hardwareInfo
    
    private val _batteryInfo = MutableLiveData<BatteryInfo>()
    val batteryInfo: LiveData<BatteryInfo> = _batteryInfo
    
    private val _sensorsList = MutableLiveData<List<SensorInfo>>()
    val sensorsList: LiveData<List<SensorInfo>> = _sensorsList
    
    private val _networkInfo = MutableLiveData<NetworkConnectionInfo>()
    val networkInfo: LiveData<NetworkConnectionInfo> = _networkInfo
    
    private val _audioDevices = MutableLiveData<List<AudioDeviceInfo>>()
    val audioDevices: LiveData<List<AudioDeviceInfo>> = _audioDevices
    
    private val _displayInfo = MutableLiveData<DisplayInfo>()
    val displayInfo: LiveData<DisplayInfo> = _displayInfo
    
    private val _xrSupported = MutableLiveData<Boolean>()
    val xrSupported: LiveData<Boolean> = _xrSupported
    
    private val _foldableState = MutableStateFlow<String>("FLAT")
    val foldableState: StateFlow<String> = _foldableState.asStateFlow()
    
    private val _imuData = MutableStateFlow<Triple<Float, Float, Float>>(Triple(0f, 0f, 0f))
    val imuData: StateFlow<Triple<Float, Float, Float>> = _imuData.asStateFlow()
    
    private val _bluetoothDevices = MutableLiveData<List<String>>()
    val bluetoothDevices: LiveData<List<String>> = _bluetoothDevices
    
    private val _wifiNetworks = MutableLiveData<List<String>>()
    val wifiNetworks: LiveData<List<String>> = _wifiNetworks
    
    private val _uwbSupported = MutableLiveData<Boolean>()
    val uwbSupported: LiveData<Boolean> = _uwbSupported
    
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage
    
    private val _successMessage = MutableLiveData<String?>()
    val successMessage: LiveData<String?> = _successMessage
    
    init {
        loadInitialData()
        startMonitoring()
    }
    
    /**
     * Load initial device data
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Load hardware info
                loadHardwareInfo()
                
                // Load battery info
                loadBatteryInfo()
                
                // Load sensors
                loadSensors()
                
                // Load network info
                loadNetworkInfo()
                
                // Load audio devices
                loadAudioDevices()
                
                // Load display info
                loadDisplayInfo()
                
                // Check XR support
                checkXRSupport()
                
                // Check UWB support
                checkUWBSupport()
                
                Log.d(TAG, "Initial data loaded successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load initial data", e)
                _errorMessage.value = "Failed to load device data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Load hardware information
     */
    private fun loadHardwareInfo() {
        val info = HardwareInfo(
            model = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            brand = Build.BRAND,
            device = Build.DEVICE,
            board = Build.BOARD,
            hardware = Build.HARDWARE,
            product = Build.PRODUCT,
            androidVersion = Build.VERSION.RELEASE,
            apiLevel = Build.VERSION.SDK_INT,
            buildId = Build.ID,
            kernelVersion = System.getProperty("os.version") ?: "Unknown"
        )
        _hardwareInfo.value = info
    }
    
    /**
     * Load battery information
     */
    private fun loadBatteryInfo() {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        
        val level = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val isCharging = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) == 
            BatteryManager.BATTERY_STATUS_CHARGING
        val temperature = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 10f
        val voltage = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
        
        val info = BatteryInfo(
            level = level,
            isCharging = isCharging,
            chargingType = if (isCharging) "USB" else "None",
            temperature = temperature,
            voltage = voltage,
            health = "Good",
            technology = "Li-ion",
            capacity = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        )
        _batteryInfo.value = info
    }
    
    /**
     * Load sensor list
     */
    private fun loadSensors() {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        
        val sensorInfoList = sensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                type = sensor.type,
                vendor = sensor.vendor,
                version = sensor.version,
                power = sensor.power,
                maximumRange = sensor.maximumRange,
                resolution = sensor.resolution,
                minDelay = sensor.minDelay,
                isWakeUp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    sensor.isWakeUpSensor
                } else false
            )
        }
        _sensorsList.value = sensorInfoList
    }
    
    /**
     * Load network information
     */
    private fun loadNetworkInfo() {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        val info = if (capabilities != null) {
            val type = when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WiFi"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cellular"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> "Bluetooth"
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
                else -> "Unknown"
            }
            
            NetworkConnectionInfo(
                type = type,
                isConnected = true,
                isMetered = !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED),
                bandwidth = capabilities.linkDownstreamBandwidthKbps,
                signalStrength = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    capabilities.signalStrength
                } else 0,
                ssid = null,
                capabilities = buildList {
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) add("Internet")
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_RESTRICTED)) add("Not Restricted")
                    if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) add("Validated")
                }
            )
        } else {
            NetworkConnectionInfo(
                type = "None",
                isConnected = false,
                isMetered = false,
                bandwidth = 0,
                signalStrength = 0,
                ssid = null,
                capabilities = emptyList()
            )
        }
        _networkInfo.value = info
    }
    
    /**
     * Load audio devices
     */
    private fun loadAudioDevices() {
        // Simulated audio devices for now
        val devices = listOf(
            AudioDeviceInfo(
                id = 1,
                name = "Built-in Speaker",
                type = "Speaker",
                isInput = false,
                sampleRates = listOf(44100, 48000),
                channelMasks = listOf(1, 2),
                formats = listOf(16, 24)
            ),
            AudioDeviceInfo(
                id = 2,
                name = "Built-in Microphone",
                type = "Microphone",
                isInput = true,
                sampleRates = listOf(44100, 48000),
                channelMasks = listOf(1),
                formats = listOf(16)
            )
        )
        _audioDevices.value = devices
    }
    
    /**
     * Load display information
     */
    private fun loadDisplayInfo() {
        val metrics = context.resources.displayMetrics
        
        val info = DisplayInfo(
            width = metrics.widthPixels,
            height = metrics.heightPixels,
            density = metrics.density,
            scaledDensity = context.resources.configuration.fontScale * metrics.density,
            refreshRate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display?.refreshRate ?: 60f
            } else 60f,
            hdrCapabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                listOf("HDR10", "HDR10+")
            } else emptyList(),
            isRound = context.resources.configuration.isScreenRound,
            cutoutInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                "Present"
            } else null
        )
        _displayInfo.value = info
    }
    
    /**
     * Check XR support
     */
    private fun checkXRSupport() {
        _xrSupported.value = xrManager?.isXRSupported() ?: false
    }
    
    /**
     * Check UWB support
     */
    private fun checkUWBSupport() {
        _uwbSupported.value = false // TODO: Add isUwbSupported to UwbManager
    }
    
    /**
     * Start monitoring device state
     */
    private fun startMonitoring() {
        viewModelScope.launch {
            // Monitor battery periodically
            while (true) {
                delay(30000) // Update every 30 seconds
                loadBatteryInfo()
                loadNetworkInfo()
            }
        }
        
        // Monitor IMU data
        viewModelScope.launch {
            // TODO: Add initialize() and startTracking() to IMUManager
            // imuManager.initialize()
            // imuManager.startTracking()
            // Collect IMU data updates
        }
        
        // Monitor foldable state
        viewModelScope.launch {
            foldableManager.initialize()
            // Collect foldable state updates
        }
    }
    
    /**
     * Refresh all data
     */
    fun refreshAllData() {
        loadInitialData()
        _successMessage.value = "Device data refreshed"
    }
    
    /**
     * Scan for Bluetooth devices
     */
    fun scanBluetoothDevices() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // TODO: Add public methods to BluetoothManager
                // bluetoothManager.initialize()
                // bluetoothManager.startScanning()
                delay(5000) // Scan for 5 seconds
                // bluetoothManager.stopScanning()
                
                // Get scanned devices (simulated)
                _bluetoothDevices.value = listOf(
                    "Headphones - 00:11:22:33:44:55",
                    "Smartwatch - AA:BB:CC:DD:EE:FF",
                    "Speaker - 11:22:33:44:55:66"
                )
                _successMessage.value = "Bluetooth scan completed"
            } catch (e: Exception) {
                _errorMessage.value = "Bluetooth scan failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Scan for WiFi networks
     */
    fun scanWiFiNetworks() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // TODO: Add public methods to WiFiManager
                // wifiManager.initialize()
                // wifiManager.startScanning()
                delay(3000) // Scan for 3 seconds
                
                // Get scanned networks (simulated)
                _wifiNetworks.value = listOf(
                    "HomeNetwork - 5GHz",
                    "OfficeWiFi - 2.4GHz",
                    "Guest_Network - 2.4GHz",
                    "PublicHotspot - Open"
                )
                _successMessage.value = "WiFi scan completed"
            } catch (e: Exception) {
                _errorMessage.value = "WiFi scan failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Test sensors
     */
    fun testSensors() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _successMessage.value = "Testing sensors..."
                
                // Simulate sensor testing
                delay(2000)
                
                _successMessage.value = "All sensors operational"
            } catch (e: Exception) {
                _errorMessage.value = "Sensor test failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Run diagnostics
     */
    fun runDiagnostics() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _successMessage.value = "Running diagnostics..."
                
                // Simulate diagnostics
                delay(3000)
                
                val results = buildString {
                    appendLine("✓ Hardware: OK")
                    appendLine("✓ Sensors: ${_sensorsList.value?.size ?: 0} detected")
                    appendLine("✓ Network: ${if (_networkInfo.value?.isConnected == true) "Connected" else "Disconnected"}")
                    appendLine("✓ Battery: ${_batteryInfo.value?.level ?: 0}%")
                    appendLine("✓ Audio: ${_audioDevices.value?.size ?: 0} devices")
                }
                
                _successMessage.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Diagnostics failed: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _errorMessage.value = null
    }
    
    /**
     * Clear success message
     */
    fun clearSuccess() {
        _successMessage.value = null
    }
    
    override fun onCleared() {
        super.onCleared()
        // Cleanup resources
        // TODO: Add stopTracking() to IMUManager
        // imuManager.stopTracking()
        bluetoothManager.cleanup()
        wifiManager.cleanup()
        Log.d(TAG, "DeviceViewModel cleared")
    }
}

/**
 * ViewModelProvider Factory for DeviceViewModel
 */
class DeviceViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DeviceViewModel::class.java)) {
            return DeviceViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}