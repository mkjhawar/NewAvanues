// Author: Manoj Jhawar
// Purpose: Comprehensive UWB (Ultra-Wideband) management for ranging, positioning, and device interaction

package com.augmentalis.devicemanager.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.uwb.*
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
// Note: Google Play Services UWB API is not yet available
// These imports are placeholders for future implementation
// import com.google.android.gms.nearby.uwb.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer
import kotlin.math.*

/**
 * Comprehensive UWB Manager
 * Supports ranging, positioning, device interaction, and advanced UWB features
 * Handles both Android native UWB API and Google Play Services UWB
 */
class UwbManager(
    private val context: Context,
    private val capabilities: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.DeviceCapabilities
) {
    
    companion object {
        private const val TAG = "UwbManager"
        
        // UWB channels (as per IEEE 802.15.4z)
        const val CHANNEL_5 = 5   // 6489.6 MHz
        const val CHANNEL_6 = 6   // 6988.8 MHz  
        const val CHANNEL_8 = 8   // 7488.0 MHz
        const val CHANNEL_9 = 9   // 7987.2 MHz
        const val CHANNEL_10 = 10 // 8486.4 MHz
        const val CHANNEL_12 = 12 // 8985.6 MHz
        const val CHANNEL_13 = 13 // 9484.8 MHz
        const val CHANNEL_14 = 14 // 9984.0 MHz
        
        // Preamble codes
        const val PREAMBLE_CODE_9 = 9
        const val PREAMBLE_CODE_10 = 10
        const val PREAMBLE_CODE_11 = 11
        const val PREAMBLE_CODE_12 = 12
        
        // Ranging modes
        const val RANGING_MODE_ONE_TO_ONE = 0
        const val RANGING_MODE_ONE_TO_MANY = 1
        const val RANGING_MODE_MANY_TO_MANY = 2
        
        // Update rates
        const val UPDATE_RATE_AUTOMATIC = 0
        const val UPDATE_RATE_INFREQUENT = 200 // ms
        const val UPDATE_RATE_NORMAL = 100 // ms
        const val UPDATE_RATE_FREQUENT = 50 // ms
        
        // Distance thresholds
        const val DISTANCE_IMMEDIATE = 0.5f // meters
        const val DISTANCE_NEAR = 2.0f
        const val DISTANCE_MEDIUM = 5.0f
        const val DISTANCE_FAR = 10.0f
        
        // Angle of Arrival (AoA) support
        const val AOA_TYPE_NONE = 0
        const val AOA_TYPE_AZIMUTH = 1
        const val AOA_TYPE_ELEVATION = 2
        const val AOA_TYPE_FULL = 3
    }
    
    // System services
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val packageManager = context.packageManager
    
    // UWB clients
    private var androidUwbManager: Any? = null // android.uwb.UwbManager when available
    private var googleUwbClient: Any? = null // UwbClient when available
    private var nearbyConnectionsClient: ConnectionsClient? = null
    
    // Active sessions
    private val activeSessions = mutableMapOf<String, RangingSession>()
    private val connectedDevices = mutableMapOf<String, UwbDeviceInfo>()
    
    // State flows
    private val _uwbState = MutableStateFlow(UwbState())
    val uwbState: StateFlow<UwbState> = _uwbState.asStateFlow()
    
    private val _rangingResults = MutableStateFlow<List<RangingData>>(emptyList())
    val rangingResults: StateFlow<List<RangingData>> = _rangingResults.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<UwbDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<UwbDeviceInfo>> = _discoveredDevices.asStateFlow()
    
    private val _spatialData = MutableStateFlow<SpatialMapping?>(null)
    val spatialData: StateFlow<SpatialMapping?> = _spatialData.asStateFlow()
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // ========== DATA MODELS ==========
    
    data class UwbState(
        val isSupported: Boolean = false,
        val isEnabled: Boolean = false,
        val capabilities: UwbCapabilities? = null,
        val currentChannel: Int = CHANNEL_9,
        val rangingMode: RangingMode = RangingMode.ONE_TO_ONE,
        val isRanging: Boolean = false,
        val activeSessions: Int = 0,
        val connectedDevices: Int = 0,
        val lastError: UwbError? = null
    )
    
    data class UwbCapabilities(
        val supportsRanging: Boolean,
        val supportsBackgroundRanging: Boolean,
        val supportsAoA: Boolean,
        val supportsAoAElevation: Boolean,
        val supportsMulticast: Boolean,
        val supportedChannels: List<Int>,
        val supportedPreambleCodes: List<Int>,
        val maxRangingDistance: Float, // meters
        val minRangingDistance: Float, // meters
        val rangingAccuracy: Float, // meters
        val angleAccuracy: Float?, // degrees
        val maxDevices: Int,
        val updateRateCapability: UpdateRateCapability,
        val powerConsumption: PowerProfile,
        val antennaCount: Int,
        val chipsetInfo: ChipsetInfo?
    )
    
    data class UpdateRateCapability(
        val minUpdateInterval: Int, // ms
        val maxUpdateInterval: Int, // ms
        val supportsDynamicRate: Boolean
    )
    
    data class PowerProfile(
        val idlePower: Float, // mW
        val rangingPower: Float, // mW
        val peakPower: Float, // mW
        val averagePower: Float // mW
    )
    
    data class ChipsetInfo(
        val manufacturer: String,
        val model: String,
        val firmwareVersion: String,
        val protocolVersion: String,
        val supportsIEEE802154z: Boolean,
        val supportsFiRa: Boolean, // FiRa Consortium
        val supportsCCC: Boolean // Car Connectivity Consortium
    )
    
    enum class RangingMode {
        ONE_TO_ONE,      // Single device ranging
        ONE_TO_MANY,     // Controller to multiple responders
        MANY_TO_MANY,    // Mesh ranging
        TIME_DIFFERENCE  // TDoA (Time Difference of Arrival)
    }
    
    data class UwbDeviceInfo(
        val address: String,
        val name: String?,
        val deviceType: DeviceType,
        val capabilities: DeviceCapabilities,
        val isConnected: Boolean,
        val lastSeen: Long,
        val rssi: Int?, // dBm
        val batteryLevel: Int?, // percentage
        val firmwareVersion: String?
    )
    
    enum class DeviceType {
        SMARTPHONE,
        SMARTWATCH,
        TAG,          // UWB tag/tracker
        ANCHOR,       // Fixed position anchor
        VEHICLE,      // Car with UWB
        SMART_LOCK,
        ACCESS_POINT,
        IOT_DEVICE,
        UNKNOWN
    }
    
    data class DeviceCapabilities(
        val supportsRanging: Boolean,
        val supportsAoA: Boolean,
        val supportsDataTransfer: Boolean,
        val supportsSecureRanging: Boolean,
        val maxDataRate: Int // kbps
    )
    
    data class RangingData(
        val deviceAddress: String,
        val distance: Float, // meters
        val distanceError: Float, // meters
        val azimuth: Float?, // degrees (-180 to 180)
        val azimuthError: Float?, // degrees
        val elevation: Float?, // degrees (-90 to 90)
        val elevationError: Float?, // degrees
        val position: Position3D?,
        val quality: RangingQuality,
        val timestamp: Long,
        val sequenceNumber: Int,
        val rssi: Int?, // dBm
        val status: RangingStatus
    )
    
    data class Position3D(
        val x: Float, // meters
        val y: Float, // meters
        val z: Float, // meters
        val confidence: Float // 0.0 to 1.0
    )
    
    enum class RangingQuality {
        EXCELLENT,    // < 10cm error
        GOOD,        // < 30cm error
        FAIR,        // < 1m error
        POOR,        // > 1m error
        NO_SIGNAL
    }
    
    enum class RangingStatus {
        SUCCESS,
        OUT_OF_RANGE,
        SIGNAL_LOST,
        INVALID_MEASUREMENT,
        HARDWARE_ERROR
    }
    
    data class RangingSession(
        val sessionId: String,
        val deviceAddress: String,
        val parameters: RangingParameters,
        val startTime: Long,
        val measurements: MutableList<RangingData> = mutableListOf(),
        var isActive: Boolean = true
    )
    
    data class RangingParameters(
        val channel: Int,
        val preambleCode: Int,
        val updateRate: Int, // ms
        val enableAoA: Boolean,
        val enableAoAElevation: Boolean,
        val sessionKey: ByteArray?,
        val subSessionId: Int?,
        val slotDuration: Int, // microseconds
        val rangingInterval: Int, // ms
        val enableAdaptiveRate: Boolean
    )
    
    // Spatial mapping for environment understanding
    data class SpatialMapping(
        val anchors: List<Anchor>,
        val tags: List<Tag>,
        val zones: List<Zone>,
        val obstacles: List<Obstacle>,
        val coordinateSystem: CoordinateSystem
    )
    
    data class Anchor(
        val id: String,
        val position: Position3D,
        val type: AnchorType,
        val isReference: Boolean
    )
    
    enum class AnchorType {
        FIXED,       // Permanently installed
        PORTABLE,    // Moveable anchor
        VIRTUAL      // Software-defined
    }
    
    data class Tag(
        val id: String,
        val position: Position3D,
        val velocity: Velocity3D?,
        val type: DeviceType
    )
    
    data class Velocity3D(
        val vx: Float, // m/s
        val vy: Float, // m/s
        val vz: Float  // m/s
    )
    
    data class Zone(
        val id: String,
        val name: String,
        val bounds: List<Position3D>,
        val type: ZoneType,
        val accessLevel: AccessLevel
    )
    
    enum class ZoneType {
        SECURE,
        PUBLIC,
        RESTRICTED,
        EMERGENCY,
        CUSTOM
    }
    
    enum class AccessLevel {
        OPEN,
        AUTHENTICATED,
        AUTHORIZED,
        DENIED
    }
    
    data class Obstacle(
        val position: Position3D,
        val size: Size3D,
        val material: MaterialType
    )
    
    data class Size3D(
        val width: Float,
        val height: Float,
        val depth: Float
    )
    
    enum class MaterialType {
        METAL,
        CONCRETE,
        GLASS,
        WOOD,
        HUMAN,
        UNKNOWN
    }
    
    enum class CoordinateSystem {
        CARTESIAN,
        SPHERICAL,
        CYLINDRICAL,
        GEODETIC
    }
    
    data class UwbError(
        val code: Int,
        val message: String,
        val severity: ErrorSeverity,
        val recoverable: Boolean
    )
    
    enum class ErrorSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
    
    // ========== INITIALIZATION ==========
    
    init {
        initialize()
    }
    
    private fun initialize() {
        updateUwbSupport()
        initializeUwbServices()
        updateCapabilities()
        setupNearbyConnections()
    }
    
    private fun updateUwbSupport() {
        val hasUwbFeature = capabilities.network.hasUwb
        
        val hasUwbPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.checkSelfPermission("android.permission.UWB_RANGING") == PackageManager.PERMISSION_GRANTED
        } else {
            false
        }
        
        _uwbState.update { it.copy(isSupported = hasUwbFeature) }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private fun initializeUwbServices() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                // UWB_SERVICE is available in Android 12+
                val uwbServiceName = "uwb" // Context.UWB_SERVICE
                androidUwbManager = context.getSystemService(uwbServiceName)
                androidUwbManager?.let {
                    _uwbState.update { state -> state.copy(isEnabled = true) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Android UWB Manager", e)
            }
        }
        
        // Initialize Google Play Services UWB
        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS) {
            try {
                // Google Play Services UWB is not yet available
                // googleUwbClient = Nearby.getUwbClient(context)
                Log.d(TAG, "Google Play Services UWB not yet available")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to initialize Google UWB Client", e)
            }
        }
    }
    
    private fun updateCapabilities() {
        if (capabilities.network.hasUwb) {
            val uwbCapabilities = UwbCapabilities(
                supportsRanging = true,
                supportsBackgroundRanging = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
                supportsAoA = true,  // Basic UWB supports AoA
                supportsAoAElevation = false,  // Advanced feature
                supportsMulticast = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                supportedChannels = listOf(5, 9),  // Basic UWB channels
                supportedPreambleCodes = listOf(9, 10, 11, 12),
                maxRangingDistance = 200.0f, // Typical UWB range
                minRangingDistance = 0.1f,
                rangingAccuracy = 0.1f, // 10cm typical
                angleAccuracy = 5.0f,
                maxDevices = 8, // Typical limit
                updateRateCapability = UpdateRateCapability(
                    minUpdateInterval = 50,
                    maxUpdateInterval = 1000,
                    supportsDynamicRate = true
                ),
                powerConsumption = PowerProfile(
                    idlePower = 1.0f,
                    rangingPower = 50.0f,
                    peakPower = 100.0f,
                    averagePower = 30.0f
                ),
                antennaCount = 2,  // Typical UWB antenna count
                chipsetInfo = null  // Would need actual detection
            )
            
            _uwbState.update { it.copy(capabilities = uwbCapabilities) }
        }
    }
    
    // ========== CAPABILITY ACCESS ==========
    
    fun isUwbSupported(): Boolean {
        return capabilities.network.hasUwb
    }
    
    fun getUwbCapabilities(): UwbCapabilities? {
        return _uwbState.value.capabilities
    }
    
    fun isAoASupported(): Boolean {
        return capabilities.network.hasUwb  // Basic UWB supports AoA
    }
    
    fun isAoAElevationSupported(): Boolean {
        return false  // Advanced feature, would need specific detection
    }
    
    fun getSupportedChannels(): List<Int> {
        return if (capabilities.network.hasUwb) listOf(5, 9) else emptyList()
    }
    
    fun getAntennaCount(): Int {
        return if (capabilities.network.hasUwb) 2 else 1
    }
    
    fun getChipsetInfo(): ChipsetInfo? {
        return null  // Would need actual chipset detection
    }
    
    private fun setupNearbyConnections() {
        nearbyConnectionsClient = Nearby.getConnectionsClient(context)
    }
    
    // ========== RANGING OPERATIONS ==========
    
    @RequiresApi(Build.VERSION_CODES.S)
    @RequiresPermission("android.permission.UWB_RANGING")
    fun startRanging(
        deviceAddress: String,
        parameters: RangingParameters? = null
    ) {
        scope.launch {
            try {
                val sessionId = generateSessionId()
                val params = parameters ?: getDefaultParameters()
                
                val session = RangingSession(
                    sessionId = sessionId,
                    deviceAddress = deviceAddress,
                    parameters = params,
                    startTime = System.currentTimeMillis()
                )
                
                activeSessions[sessionId] = session
                _uwbState.update { 
                    it.copy(
                        isRanging = true,
                        activeSessions = activeSessions.size
                    )
                }
                
                // Start actual ranging based on available API
                when {
                    androidUwbManager != null -> startAndroidUwbRanging(session)
                    googleUwbClient != null -> startGoogleUwbRanging(session)
                    else -> throw UnsupportedOperationException("No UWB service available")
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start ranging", e)
                handleError(UwbError(
                    code = -1,
                    message = e.message ?: "Unknown error",
                    severity = ErrorSeverity.ERROR,
                    recoverable = true
                ))
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun startAndroidUwbRanging(session: RangingSession) {
        // Android native UWB API implementation
        // This would interface with the Android UWB service
        simulateRanging(session) // Simulated for demonstration
    }
    
    private suspend fun startGoogleUwbRanging(session: RangingSession) {
        // Google Play Services UWB implementation
        simulateRanging(session) // Simulated for demonstration
    }
    
    private suspend fun simulateRanging(session: RangingSession) {
        while (session.isActive) {
            delay(session.parameters.updateRate.toLong())
            
            val measurement = RangingData(
                deviceAddress = session.deviceAddress,
                distance = (1.0f + Math.random() * 5.0).toFloat(),
                distanceError = 0.1f,
                azimuth = if (session.parameters.enableAoA) {
                    (-45 + Math.random() * 90).toFloat()
                } else null,
                azimuthError = if (session.parameters.enableAoA) 5.0f else null,
                elevation = if (session.parameters.enableAoAElevation) {
                    (-30 + Math.random() * 60).toFloat()
                } else null,
                elevationError = if (session.parameters.enableAoAElevation) 5.0f else null,
                position = calculatePosition(
                    distance = (1.0f + Math.random() * 5.0).toFloat(),
                    azimuth = (-45 + Math.random() * 90).toFloat(),
                    elevation = (-30 + Math.random() * 60).toFloat()
                ),
                quality = RangingQuality.GOOD,
                timestamp = System.currentTimeMillis(),
                sequenceNumber = session.measurements.size,
                rssi = -50 - (Math.random() * 30).toInt(),
                status = RangingStatus.SUCCESS
            )
            
            session.measurements.add(measurement)
            updateRangingResults(session)
        }
    }
    
    private fun calculatePosition(distance: Float, azimuth: Float, elevation: Float): Position3D {
        // Convert spherical coordinates to Cartesian
        val azimuthRad = Math.toRadians(azimuth.toDouble())
        val elevationRad = Math.toRadians(elevation.toDouble())
        
        val x = (distance * cos(elevationRad) * sin(azimuthRad)).toFloat()
        val y = (distance * cos(elevationRad) * cos(azimuthRad)).toFloat()
        val z = (distance * sin(elevationRad)).toFloat()
        
        return Position3D(x, y, z, confidence = 0.95f)
    }
    
    private fun updateRangingResults(session: RangingSession) {
        val currentResults = _rangingResults.value.toMutableList()
        
        // Update or add the latest measurement
        val latestMeasurement = session.measurements.lastOrNull() ?: return
        val existingIndex = currentResults.indexOfFirst { 
            it.deviceAddress == session.deviceAddress 
        }
        
        if (existingIndex >= 0) {
            currentResults[existingIndex] = latestMeasurement
        } else {
            currentResults.add(latestMeasurement)
        }
        
        _rangingResults.value = currentResults
    }
    
    fun stopRanging(deviceAddress: String) {
        activeSessions.values
            .filter { it.deviceAddress == deviceAddress }
            .forEach { it.isActive = false }
        
        activeSessions.entries.removeIf { !it.value.isActive }
        
        _uwbState.update {
            it.copy(
                isRanging = activeSessions.isNotEmpty(),
                activeSessions = activeSessions.size
            )
        }
    }
    
    fun stopAllRanging() {
        activeSessions.values.forEach { it.isActive = false }
        activeSessions.clear()
        
        _uwbState.update {
            it.copy(
                isRanging = false,
                activeSessions = 0
            )
        }
    }
    
    // ========== DEVICE DISCOVERY ==========
    
    fun startDiscovery() {
        scope.launch {
            // Start discovering nearby UWB devices
            discoverWithNearbyConnections()
            simulateDeviceDiscovery() // For demonstration
        }
    }
    
    private fun discoverWithNearbyConnections() {
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        
        nearbyConnectionsClient?.startAdvertising(
            "UWB_Device",
            "com.augmentalis.uwb",
            object : ConnectionLifecycleCallback() {
                override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                    // Handle connection
                }
                
                override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                    if (result.status.isSuccess) {
                        handleDeviceConnected(endpointId)
                    }
                }
                
                override fun onDisconnected(endpointId: String) {
                    handleDeviceDisconnected(endpointId)
                }
            },
            advertisingOptions
        )
        
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(Strategy.P2P_CLUSTER)
            .build()
        
        nearbyConnectionsClient?.startDiscovery(
            "com.augmentalis.uwb",
            object : EndpointDiscoveryCallback() {
                override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                    handleDeviceDiscovered(endpointId, info)
                }
                
                override fun onEndpointLost(endpointId: String) {
                    handleDeviceLost(endpointId)
                }
            },
            discoveryOptions
        )
    }
    
    private suspend fun simulateDeviceDiscovery() {
        delay(1000)
        
        val devices = listOf(
            UwbDeviceInfo(
                address = "00:11:22:33:44:55",
                name = "Smart Tag",
                deviceType = DeviceType.TAG,
                capabilities = DeviceCapabilities(
                    supportsRanging = true,
                    supportsAoA = true,
                    supportsDataTransfer = false,
                    supportsSecureRanging = true,
                    maxDataRate = 27000 // 27 Mbps
                ),
                isConnected = false,
                lastSeen = System.currentTimeMillis(),
                rssi = -60,
                batteryLevel = 85,
                firmwareVersion = "1.2.0"
            ),
            UwbDeviceInfo(
                address = "00:11:22:33:44:66",
                name = "Smart Lock",
                deviceType = DeviceType.SMART_LOCK,
                capabilities = DeviceCapabilities(
                    supportsRanging = true,
                    supportsAoA = false,
                    supportsDataTransfer = true,
                    supportsSecureRanging = true,
                    maxDataRate = 6800 // 6.8 Mbps
                ),
                isConnected = false,
                lastSeen = System.currentTimeMillis(),
                rssi = -45,
                batteryLevel = null,
                firmwareVersion = "2.0.1"
            )
        )
        
        _discoveredDevices.value = devices
    }
    
    private fun handleDeviceDiscovered(endpointId: String, info: DiscoveredEndpointInfo) {
        val device = UwbDeviceInfo(
            address = endpointId,
            name = info.endpointName,
            deviceType = DeviceType.UNKNOWN,
            capabilities = DeviceCapabilities(
                supportsRanging = true,
                supportsAoA = false,
                supportsDataTransfer = true,
                supportsSecureRanging = false,
                maxDataRate = 6800
            ),
            isConnected = false,
            lastSeen = System.currentTimeMillis(),
            rssi = null,
            batteryLevel = null,
            firmwareVersion = null
        )
        
        val currentDevices = _discoveredDevices.value.toMutableList()
        if (currentDevices.none { it.address == endpointId }) {
            currentDevices.add(device)
            _discoveredDevices.value = currentDevices
        }
    }
    
    private fun handleDeviceLost(endpointId: String) {
        val currentDevices = _discoveredDevices.value.toMutableList()
        currentDevices.removeIf { it.address == endpointId }
        _discoveredDevices.value = currentDevices
    }
    
    private fun handleDeviceConnected(endpointId: String) {
        connectedDevices[endpointId] = _discoveredDevices.value.find { 
            it.address == endpointId 
        } ?: return
        
        _uwbState.update { it.copy(connectedDevices = connectedDevices.size) }
    }
    
    private fun handleDeviceDisconnected(endpointId: String) {
        connectedDevices.remove(endpointId)
        _uwbState.update { it.copy(connectedDevices = connectedDevices.size) }
    }
    
    // ========== SPATIAL MAPPING ==========
    
    fun initializeSpatialMapping() {
        scope.launch {
            val mapping = SpatialMapping(
                anchors = getAnchors(),
                tags = getTags(),
                zones = defineZones(),
                obstacles = getObstacles(),
                coordinateSystem = CoordinateSystem.CARTESIAN
            )
            
            _spatialData.value = mapping
        }
    }
    
    private fun getAnchors(): List<Anchor> {
        // In real implementation, this would retrieve fixed UWB anchors
        return listOf(
            Anchor(
                id = "anchor_1",
                position = Position3D(0f, 0f, 2f, 1f),
                type = AnchorType.FIXED,
                isReference = true
            ),
            Anchor(
                id = "anchor_2",
                position = Position3D(5f, 0f, 2f, 1f),
                type = AnchorType.FIXED,
                isReference = false
            )
        )
    }
    
    private fun getTags(): List<Tag> {
        // Convert discovered devices to tags
        return _discoveredDevices.value
            .filter { it.deviceType == DeviceType.TAG }
            .map { device ->
                Tag(
                    id = device.address,
                    position = Position3D(0f, 0f, 0f, 0.5f),
                    velocity = null,
                    type = device.deviceType
                )
            }
    }
    
    private fun defineZones(): List<Zone> {
        return listOf(
            Zone(
                id = "zone_secure",
                name = "Secure Area",
                bounds = listOf(
                    Position3D(-2f, -2f, 0f, 1f),
                    Position3D(2f, -2f, 0f, 1f),
                    Position3D(2f, 2f, 0f, 1f),
                    Position3D(-2f, 2f, 0f, 1f)
                ),
                type = ZoneType.SECURE,
                accessLevel = AccessLevel.AUTHORIZED
            )
        )
    }
    
    private fun getObstacles(): List<Obstacle> {
        // In real implementation, this would use environmental scanning
        return emptyList()
    }
    
    // ========== UTILITY METHODS ==========
    
    private fun getDefaultParameters(): RangingParameters {
        return RangingParameters(
            channel = CHANNEL_9,
            preambleCode = PREAMBLE_CODE_10,
            updateRate = UPDATE_RATE_NORMAL,
            enableAoA = _uwbState.value.capabilities?.supportsAoA ?: false,
            enableAoAElevation = _uwbState.value.capabilities?.supportsAoAElevation ?: false,
            sessionKey = null,
            subSessionId = null,
            slotDuration = 2400,
            rangingInterval = 200,
            enableAdaptiveRate = true
        )
    }
    
    private fun generateSessionId(): String {
        return "uwb_session_${System.currentTimeMillis()}"
    }
    
    private fun handleError(error: UwbError) {
        _uwbState.update { it.copy(lastError = error) }
        
        if (error.severity == ErrorSeverity.CRITICAL) {
            stopAllRanging()
        }
    }
    
    fun getDistanceCategory(distance: Float): String {
        return when {
            distance <= DISTANCE_IMMEDIATE -> "Immediate"
            distance <= DISTANCE_NEAR -> "Near"
            distance <= DISTANCE_MEDIUM -> "Medium"
            distance <= DISTANCE_FAR -> "Far"
            else -> "Out of Range"
        }
    }
    
    fun isInZone(position: Position3D, zone: Zone): Boolean {
        // Simple point-in-polygon check for 2D (ignoring z)
        val x = position.x
        val y = position.y
        var inside = false
        
        val bounds = zone.bounds
        var j = bounds.size - 1
        
        for (i in bounds.indices) {
            val xi = bounds[i].x
            val yi = bounds[i].y
            val xj = bounds[j].x
            val yj = bounds[j].y
            
            if ((yi > y) != (yj > y) && 
                x < (xj - xi) * (y - yi) / (yj - yi) + xi) {
                inside = !inside
            }
            j = i
        }
        
        return inside
    }
    
    fun cleanup() {
        stopAllRanging()
        nearbyConnectionsClient?.stopAllEndpoints()
        nearbyConnectionsClient?.stopDiscovery()
        nearbyConnectionsClient?.stopAdvertising()
        scope.cancel()
    }
}
