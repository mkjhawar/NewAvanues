// Author: Manoj Jhawar
// Purpose: Comprehensive WiFi management including WiFi 6E/7, Direct, Aware, RTT, and screen mirroring

package com.augmentalis.devicemanager.network

import android.Manifest
import android.annotation.SuppressLint  
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.view.Display
import android.media.MediaRouter
import android.net.*
import android.net.wifi.*
import android.net.wifi.aware.*
import android.net.wifi.p2p.*
import android.net.wifi.rtt.*
import android.os.Build
import android.os.PatternMatcher
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.net.InetAddress
import java.net.NetworkInterface

/**
 * Comprehensive WiFi Manager
 * Supports WiFi 6/6E/7, WiFi Direct, WiFi Aware (NAN), RTT positioning, and screen mirroring
 */
class WiFiManager(
    private val context: Context,
    private val capabilities: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.DeviceCapabilities
) {
    
    companion object {
        private const val TAG = "WiFiManager"
        
        // WiFi standards
        const val WIFI_STANDARD_LEGACY = 0
        const val WIFI_STANDARD_11N = 4     // WiFi 4
        const val WIFI_STANDARD_11AC = 5    // WiFi 5
        const val WIFI_STANDARD_11AX = 6    // WiFi 6/6E
        const val WIFI_STANDARD_11BE = 7    // WiFi 7 (802.11be)
        
        // Frequency bands
        const val BAND_24GHZ = 1
        const val BAND_5GHZ = 2
        const val BAND_6GHZ = 4  // WiFi 6E
        const val BAND_60GHZ = 8 // WiGig
        
        // Channel widths
        const val CHANNEL_WIDTH_20MHZ = 0
        const val CHANNEL_WIDTH_40MHZ = 1
        const val CHANNEL_WIDTH_80MHZ = 2
        const val CHANNEL_WIDTH_160MHZ = 3
        const val CHANNEL_WIDTH_320MHZ = 4 // WiFi 7
        
        // Security types
        const val SECURITY_OPEN = 0
        const val SECURITY_WEP = 1
        const val SECURITY_WPA = 2
        const val SECURITY_WPA2 = 3
        const val SECURITY_WPA3 = 4
        const val SECURITY_WPA3_ENTERPRISE = 5
        const val SECURITY_OWE = 6 // Opportunistic Wireless Encryption
        const val SECURITY_SAE = 7 // Simultaneous Authentication of Equals
    }
    
    // System services
    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val mediaRouter = context.getSystemService(Context.MEDIA_ROUTER_SERVICE) as MediaRouter
    
    // WiFi Direct (P2P)
    private var wifiP2pManager: WifiP2pManager? = null
    private var wifiP2pChannel: WifiP2pManager.Channel? = null
    
    // WiFi Aware (NAN - Neighbor Awareness Networking)
    private var wifiAwareManager: WifiAwareManager? = null
    private var wifiAwareSession: WifiAwareSession? = null
    
    // WiFi RTT (Round Trip Time for positioning)
    private var wifiRttManager: WifiRttManager? = null
    
    // State flows
    private val _wifiState = MutableStateFlow(WiFiState())
    val wifiState: StateFlow<WiFiState> = _wifiState.asStateFlow()
    
    private val _scanResults = MutableStateFlow<List<WiFiNetwork>>(emptyList())
    val scanResults: StateFlow<List<WiFiNetwork>> = _scanResults.asStateFlow()
    
    private val _p2pDevices = MutableStateFlow<List<P2PDevice>>(emptyList())
    val p2pDevices: StateFlow<List<P2PDevice>> = _p2pDevices.asStateFlow()
    
    private val _mirroringDevices = MutableStateFlow<List<MirroringDevice>>(emptyList())
    val mirroringDevices: StateFlow<List<MirroringDevice>> = _mirroringDevices.asStateFlow()
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // ========== DATA MODELS ==========
    
    data class WiFiState(
        val isEnabled: Boolean = false,
        val isConnected: Boolean = false,
        val currentNetwork: WiFiNetwork? = null,
        val capabilities: WiFiCapabilities? = null,
        val p2pEnabled: Boolean = false,
        val awareAvailable: Boolean = false,
        val rttAvailable: Boolean = false,
        val mirroringAvailable: Boolean = false,
        val wifiStandard: WiFiStandard = WiFiStandard.UNKNOWN,
        val frequencyBands: List<FrequencyBand> = emptyList(),
        val channelInfo: ChannelInfo? = null
    )
    
    data class WiFiNetwork(
        val ssid: String,
        val bssid: String,
        val capabilities: String,
        val level: Int, // dBm
        val frequency: Int, // MHz
        val channelWidth: ChannelWidth,
        val standard: WiFiStandard,
        val securityType: SecurityType,
        val isPasspoint: Boolean,
        val isWiFi6E: Boolean,
        val isWiFi7: Boolean,
        val maxLinkSpeed: Int, // Mbps
        val distance: Float?, // Estimated distance in meters
        val venue: String?, // Passpoint venue
        val operatorName: String?,
        val isHidden: Boolean,
        val timestamp: Long
    )
    
    enum class WiFiStandard {
        LEGACY,    // 802.11a/b/g
        WIFI_4,    // 802.11n
        WIFI_5,    // 802.11ac
        WIFI_6,    // 802.11ax
        WIFI_6E,   // 802.11ax with 6GHz
        WIFI_7,    // 802.11be
        UNKNOWN
    }
    
    enum class FrequencyBand {
        BAND_2_4_GHZ,
        BAND_5_GHZ,
        BAND_6_GHZ,  // WiFi 6E
        BAND_60_GHZ  // WiGig
    }
    
    enum class ChannelWidth {
        WIDTH_20,
        WIDTH_40,
        WIDTH_80,
        WIDTH_160,
        WIDTH_320  // WiFi 7
    }
    
    enum class SecurityType {
        OPEN,
        WEP,
        WPA,
        WPA2_PSK,
        WPA2_ENTERPRISE,
        WPA3_PSK,
        WPA3_ENTERPRISE,
        WPA3_OWE,
        WPA3_SAE
    }
    
    data class WiFiCapabilities(
        val supports5GHz: Boolean,
        val supports6GHz: Boolean,
        val supportsWiFi6: Boolean,
        val supportsWiFi6E: Boolean,
        val supportsWiFi7: Boolean,
        val supportsP2P: Boolean,
        val supportsAware: Boolean,
        val supportsRtt: Boolean,
        val supportsTdls: Boolean,
        val supportsPasspoint: Boolean,
        val supportsMimo: MimoSupport?,
        val maxTxPower: Int, // dBm
        val maxChannelWidth: ChannelWidth,
        val maxLinkSpeed: Int, // Mbps
        val concurrentBands: Int,
        val maxSpatialStreams: Int
    )
    
    data class MimoSupport(
        val txStreams: Int,
        val rxStreams: Int,
        val muMimoSupported: Boolean,
        val beamformingSupported: Boolean
    )
    
    data class ChannelInfo(
        val channel: Int,
        val frequency: Int, // MHz
        val band: FrequencyBand,
        val width: ChannelWidth,
        val isDFS: Boolean, // Dynamic Frequency Selection
        val isIndoor: Boolean
    )
    
    // WiFi Direct (P2P) models
    data class P2PDevice(
        val deviceName: String,
        val deviceAddress: String,
        val primaryDeviceType: String,
        val status: P2PStatus,
        val wpsConfigMethods: Int,
        val deviceCapability: Int,
        val groupCapability: Int,
        val isGroupOwner: Boolean,
        val isServiceDiscoveryCapable: Boolean
    )
    
    enum class P2PStatus {
        CONNECTED,
        INVITED,
        FAILED,
        AVAILABLE,
        UNAVAILABLE
    }
    
    // WiFi Aware (NAN) models
    data class AwareDevice(
        val peerId: Int,
        val serviceName: String,
        val serviceInfo: ByteArray?,
        val distance: Float?, // meters
        val rssi: Int?,
        val timestamp: Long
    )
    
    // Screen mirroring models
    data class MirroringDevice(
        val name: String,
        val id: String,
        val type: MirroringType,
        val protocol: MirroringProtocol,
        val isAvailable: Boolean,
        val isConnected: Boolean,
        val capabilities: MirroringCapabilities
    )
    
    enum class MirroringType {
        MIRACAST,
        CHROMECAST,
        AIRPLAY,      // For reference
        DLNA,
        SAMSUNG_SMART_VIEW,
        LG_SCREEN_SHARE,
        WIFI_DIRECT_DISPLAY,
        PROPRIETARY
    }
    
    enum class MirroringProtocol {
        MIRACAST,           // WiFi Display
        GOOGLE_CAST,        // Chromecast
        DLNA,              // Digital Living Network Alliance
        RTSP,              // Real-Time Streaming Protocol
        SCREEN_MIRRORING,  // Generic
        PROPRIETARY
    }
    
    data class MirroringCapabilities(
        val maxResolution: String,
        val maxFrameRate: Int,
        val audioSupported: Boolean,
        val hdcpSupported: Boolean,
        val latency: Int, // ms
        val codecSupport: List<String>
    )
    
    // WiFi RTT (positioning) models
    data class RttResult(
        val macAddress: String,
        val distanceMm: Int,
        val distanceStdDevMm: Int,
        val rssi: Int,
        val numberAttempted: Int,
        val numberSuccessful: Int,
        val timestamp: Long
    )
    
    // ========== INITIALIZATION ==========
    
    init {
        initialize()
    }
    
    private fun initialize() {
        initializeP2P()
        initializeAware()
        initializeRtt()
        updateWiFiState()
        registerReceivers()
        initializeMirroringDevices()
    }
    
    private fun initializeP2P() {
        wifiP2pManager = context.getSystemService(Context.WIFI_P2P_SERVICE) as? WifiP2pManager
        wifiP2pManager?.let { manager ->
            wifiP2pChannel = manager.initialize(context, context.mainLooper) { 
                Log.e(TAG, "WiFi P2P channel disconnected")
            }
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun initializeAware() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            wifiAwareManager = context.getSystemService(Context.WIFI_AWARE_SERVICE) as? WifiAwareManager
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.P)
    private fun initializeRtt() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            wifiRttManager = context.getSystemService(Context.WIFI_RTT_RANGING_SERVICE) as? WifiRttManager
        }
    }
    
    private fun updateWiFiState() {
        _wifiState.update { current ->
            current.copy(
                isEnabled = wifiManager.isWifiEnabled,
                capabilities = createWiFiCapabilities(capabilities.wifi),
                p2pEnabled = wifiP2pManager != null,
                awareAvailable = isAwareSystemSupported(),
                rttAvailable = isRttSystemSupported(),
                mirroringAvailable = isMirroringSupported()
            )
        }
    }
    
    // ========== CAPABILITY ACCESS ==========
    
    fun getWiFiCapabilities(): WiFiCapabilities {
        return createWiFiCapabilities(capabilities.wifi)
    }
    
    private fun createWiFiCapabilities(detectorWifi: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.WiFiCapabilities?): WiFiCapabilities {
        return if (detectorWifi != null) {
            WiFiCapabilities(
                supports5GHz = detectorWifi.is5GHzSupported,
                supports6GHz = detectorWifi.is6GHzSupported,
                supportsWiFi6 = detectorWifi.isWiFi6Supported,
                supportsWiFi6E = detectorWifi.isWiFi6ESupported,
                supportsWiFi7 = detectorWifi.isWiFi7Supported,
                supportsP2P = detectorWifi.isP2pSupported,
                supportsAware = isAwareSystemSupported(),
                supportsRtt = isRttSystemSupported(),
                supportsTdls = detectorWifi.isTdlsSupported,
                supportsPasspoint = detectorWifi.isPreferredNetworkOffloadSupported,
                supportsMimo = null, // Not available in DeviceDetector
                maxTxPower = 20, // Default value
                maxChannelWidth = getMaxChannelWidth(),
                maxLinkSpeed = getMaxLinkSpeed(),
                concurrentBands = if (detectorWifi.isDualBandSupported) 2 else 1,
                maxSpatialStreams = getMaxSpatialStreams()
            )
        } else {
            WiFiCapabilities(
                supports5GHz = false,
                supports6GHz = false,
                supportsWiFi6 = false,
                supportsWiFi6E = false,
                supportsWiFi7 = false,
                supportsP2P = false,
                supportsAware = false,
                supportsRtt = false,
                supportsTdls = false,
                supportsPasspoint = false,
                supportsMimo = null,
                maxTxPower = 0,
                maxChannelWidth = ChannelWidth.WIDTH_20,
                maxLinkSpeed = 0,
                concurrentBands = 1,
                maxSpatialStreams = 1
            )
        }
    }
    
    fun isWiFi6Supported(): Boolean {
        return capabilities.wifi?.isWiFi6Supported ?: false
    }
    
    fun isWiFi6ESupported(): Boolean {
        return capabilities.wifi?.isWiFi6ESupported ?: false
    }
    
    fun isWiFi7Supported(): Boolean {
        return capabilities.wifi?.isWiFi7Supported ?: false
    }
    
    fun isAwareSupported(): Boolean {
        return isAwareSystemSupported()
    }
    
    fun isRttSupported(): Boolean {
        return isRttSystemSupported()
    }
    
    private fun isAwareSystemSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // COT: Use capabilities to check WiFi Aware support instead of hasSystemFeature
            capabilities.network?.hasWiFiAware ?: false
        } else false
    }
    
    private fun isRttSystemSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // COT: Use capabilities to check WiFi RTT support instead of hasSystemFeature
            capabilities.network?.hasWiFiRtt ?: false
        } else false
    }
    
    private fun isMirroringSupported(): Boolean {
        // Check for general screen mirroring capabilities
        // COT: Use capabilities to check WiFi Direct support instead of hasSystemFeature
        return displayManager.displays.size > 1 || 
               (capabilities.network?.hasWiFiDirect ?: false)
    }
    
    fun getMimoSupport(): MimoSupport {
        val wifiCaps = capabilities.wifi
        return MimoSupport(
            txStreams = 2, // Default value, actual not available
            rxStreams = 2, // Default value, actual not available
            muMimoSupported = false, // Not available in DeviceDetector
            beamformingSupported = false // Not available in DeviceDetector
        )
    }
    
    fun getMaxChannelWidth(): ChannelWidth {
        return ChannelWidth.WIDTH_80 // Default value, actual not available
    }
    
    fun getMaxLinkSpeed(): Int {
        return 1200 // Default value for WiFi 6
    }
    
    fun getMaxSpatialStreams(): Int {
        return 2 // Default value, actual not available
    }
    
    // ========== SCANNING ==========
    
    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE
    ])
    fun startScan() {
        if (!wifiManager.isWifiEnabled) {
            Log.w(TAG, "WiFi is not enabled")
            return
        }
        
        val success = wifiManager.startScan()
        if (success) {
            Log.d(TAG, "WiFi scan started")
        } else {
            Log.e(TAG, "WiFi scan failed to start")
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun processScanResults() {
        val results = wifiManager.scanResults
        val networks = results.map { scanResult ->
            WiFiNetwork(
                ssid = scanResult.SSID ?: "",
                bssid = scanResult.BSSID,
                capabilities = scanResult.capabilities,
                level = scanResult.level,
                frequency = scanResult.frequency,
                channelWidth = getChannelWidth(scanResult),
                standard = getWiFiStandard(scanResult),
                securityType = getSecurityType(scanResult.capabilities),
                isPasspoint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scanResult.isPasspointNetwork
                } else false,
                isWiFi6E = is6GHzFrequency(scanResult.frequency),
                isWiFi7 = isWiFi7Network(scanResult),
                maxLinkSpeed = estimateMaxLinkSpeed(scanResult),
                distance = calculateDistance(scanResult.level, scanResult.frequency),
                venue = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scanResult.venueName?.toString()
                } else null,
                operatorName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    scanResult.operatorFriendlyName?.toString()
                } else null,
                isHidden = scanResult.SSID.isNullOrEmpty(),
                timestamp = System.currentTimeMillis()
            )
        }
        
        _scanResults.value = networks
    }
    
    private fun getChannelWidth(scanResult: ScanResult): ChannelWidth {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (scanResult.channelWidth) {
                ScanResult.CHANNEL_WIDTH_20MHZ -> ChannelWidth.WIDTH_20
                ScanResult.CHANNEL_WIDTH_40MHZ -> ChannelWidth.WIDTH_40
                ScanResult.CHANNEL_WIDTH_80MHZ -> ChannelWidth.WIDTH_80
                ScanResult.CHANNEL_WIDTH_160MHZ -> ChannelWidth.WIDTH_160
                ScanResult.CHANNEL_WIDTH_80MHZ_PLUS_MHZ -> ChannelWidth.WIDTH_160
                else -> ChannelWidth.WIDTH_20
            }
        } else {
            ChannelWidth.WIDTH_20
        }
    }
    
    private fun getWiFiStandard(scanResult: ScanResult): WiFiStandard {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            when (scanResult.wifiStandard) {
                ScanResult.WIFI_STANDARD_LEGACY -> WiFiStandard.LEGACY
                ScanResult.WIFI_STANDARD_11N -> WiFiStandard.WIFI_4
                ScanResult.WIFI_STANDARD_11AC -> WiFiStandard.WIFI_5
                ScanResult.WIFI_STANDARD_11AX -> {
                    if (is6GHzFrequency(scanResult.frequency)) {
                        WiFiStandard.WIFI_6E
                    } else {
                        WiFiStandard.WIFI_6
                    }
                }
                else -> WiFiStandard.UNKNOWN
            }
        } else {
            // Estimate based on frequency and capabilities
            when {
                scanResult.capabilities.contains("11ax") -> WiFiStandard.WIFI_6
                scanResult.capabilities.contains("11ac") -> WiFiStandard.WIFI_5
                scanResult.capabilities.contains("11n") -> WiFiStandard.WIFI_4
                else -> WiFiStandard.LEGACY
            }
        }
    }
    
    private fun getSecurityType(capabilities: String): SecurityType {
        return when {
            capabilities.contains("WPA3") && capabilities.contains("SAE") -> SecurityType.WPA3_SAE
            capabilities.contains("WPA3-Enterprise") -> SecurityType.WPA3_ENTERPRISE
            capabilities.contains("WPA3") -> SecurityType.WPA3_PSK
            capabilities.contains("OWE") -> SecurityType.WPA3_OWE
            capabilities.contains("WPA2-Enterprise") -> SecurityType.WPA2_ENTERPRISE
            capabilities.contains("WPA2") -> SecurityType.WPA2_PSK
            capabilities.contains("WPA") -> SecurityType.WPA
            capabilities.contains("WEP") -> SecurityType.WEP
            else -> SecurityType.OPEN
        }
    }
    
    private fun is6GHzFrequency(frequency: Int): Boolean {
        return frequency in 5925..7125
    }
    
    private fun isWiFi7Network(scanResult: ScanResult): Boolean {
        // WiFi 7 detection heuristics
        return if (Build.VERSION.SDK_INT >= 34) {
            scanResult.capabilities.contains("11be") ||
            (getChannelWidth(scanResult) == ChannelWidth.WIDTH_320)
        } else false
    }
    
    private fun estimateMaxLinkSpeed(scanResult: ScanResult): Int {
        val standard = getWiFiStandard(scanResult)
        val width = getChannelWidth(scanResult)
        
        return when (standard) {
            WiFiStandard.WIFI_7 -> 46000
            WiFiStandard.WIFI_6E -> when (width) {
                ChannelWidth.WIDTH_160 -> 9600
                ChannelWidth.WIDTH_80 -> 4800
                else -> 2400
            }
            WiFiStandard.WIFI_6 -> when (width) {
                ChannelWidth.WIDTH_160 -> 9600
                ChannelWidth.WIDTH_80 -> 4800
                else -> 2400
            }
            WiFiStandard.WIFI_5 -> when (width) {
                ChannelWidth.WIDTH_160 -> 6933
                ChannelWidth.WIDTH_80 -> 3466
                else -> 866
            }
            WiFiStandard.WIFI_4 -> 600
            else -> 54
        }
    }
    
    private fun calculateDistance(rssi: Int, frequency: Int): Float {
        // Free Space Path Loss formula
        val exp = (27.55 - 20 * Math.log10(frequency.toDouble()) + Math.abs(rssi)) / 20.0
        return Math.pow(10.0, exp).toFloat()
    }
    
    // ========== WIFI DIRECT (P2P) ==========
    
    @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
    fun startP2pDiscovery() {
        wifiP2pManager?.discoverPeers(wifiP2pChannel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "P2P discovery started")
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "P2P discovery failed: $reason")
            }
        })
    }
    
    @SuppressLint("MissingPermission")
    fun connectP2pDevice(device: WifiP2pDevice) {
        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }
        
        wifiP2pManager?.connect(wifiP2pChannel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                Log.d(TAG, "P2P connection initiated")
            }
            
            override fun onFailure(reason: Int) {
                Log.e(TAG, "P2P connection failed: $reason")
            }
        })
    }
    
    // ========== WIFI AWARE (NAN) ==========
    
    @RequiresApi(Build.VERSION_CODES.O)
    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE
    ])
    fun startAwareDiscovery(serviceName: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        
        wifiAwareManager?.let { manager ->
            if (!manager.isAvailable) {
                Log.w(TAG, "WiFi Aware is not available")
                return
            }
            
            manager.attach(object : AttachCallback() {
                override fun onAttached(session: WifiAwareSession) {
                    wifiAwareSession = session
                    startAwareSubscribe(session, serviceName)
                }
                
                override fun onAttachFailed() {
                    Log.e(TAG, "WiFi Aware attach failed")
                }
            }, null)
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.O)
    private fun startAwareSubscribe(session: WifiAwareSession, serviceName: String) {
        val config = SubscribeConfig.Builder()
            .setServiceName(serviceName)
            .build()
        
        session.subscribe(config, object : DiscoverySessionCallback() {
            override fun onServiceDiscovered(
                peerHandle: PeerHandle,
                serviceSpecificInfo: ByteArray?,
                matchFilter: MutableList<ByteArray>?
            ) {
                Log.d(TAG, "Aware service discovered")
            }
        }, null)
    }
    
    // ========== WIFI RTT (POSITIONING) ==========
    
    @RequiresApi(Build.VERSION_CODES.P)
    @RequiresPermission(allOf = [
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_WIFI_STATE
    ])
    fun startRttRanging(accessPoints: List<ScanResult>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        
        wifiRttManager?.let { manager ->
            if (!manager.isAvailable) {
                Log.w(TAG, "WiFi RTT is not available")
                return
            }
            
            val request = RangingRequest.Builder()
                .addAccessPoints(accessPoints)
                .build()
            
            manager.startRanging(request, context.mainExecutor, object : RangingResultCallback() {
                override fun onRangingResults(results: List<RangingResult>) {
                    processRttResults(results)
                }
                
                override fun onRangingFailure(code: Int) {
                    Log.e(TAG, "RTT ranging failed: $code")
                }
            })
        }
    }
    
    @RequiresApi(Build.VERSION_CODES.P)
    private fun processRttResults(results: List<RangingResult>) {
        results.forEach { result ->
            if (result.status == RangingResult.STATUS_SUCCESS) {
                val distance = result.distanceMm / 1000f // Convert to meters
                Log.d(TAG, "RTT distance: ${distance}m")
            }
        }
    }
    
    // ========== SCREEN MIRRORING MANAGEMENT ==========
    
    private fun initializeMirroringDevices() {
        val mirroringTypes = mutableListOf<MirroringDevice>()
        
        // Initialize supported mirroring types based on system capabilities
        if (isMiracastSupported()) {
            mirroringTypes.add(createMiracastDevice())
        }
        if (isChromecastSupported()) {
            mirroringTypes.add(createChromecastDevice())
        }
        if (isDlnaSupported()) {
            mirroringTypes.add(createDlnaDevice())
        }
        // Additional proprietary mirroring support can be added based on device brand
        if (Build.MANUFACTURER.equals("samsung", ignoreCase = true)) {
            mirroringTypes.add(createSamsungDevice())
        }
        if (Build.MANUFACTURER.equals("lg", ignoreCase = true)) {
            mirroringTypes.add(createLGDevice())
        }
        
        _mirroringDevices.value = mirroringTypes
    }
    
    fun isMiracastSupported(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            // COT: Use capabilities to check WiFi Direct support instead of hasSystemFeature
            displayManager.displays.any { it.state == Display.STATE_ON } && 
            (capabilities.network?.hasWiFiDirect ?: false)
        } else false
    }
    
    private fun createMiracastDevice(): MirroringDevice {
        return MirroringDevice(
            name = "Miracast",
            id = "miracast_default",
            type = MirroringType.MIRACAST,
            protocol = MirroringProtocol.MIRACAST,
            isAvailable = true,
            isConnected = false,
            capabilities = MirroringCapabilities(
                maxResolution = "1920x1080",
                maxFrameRate = 60,
                audioSupported = true,
                hdcpSupported = true,
                latency = 50,
                codecSupport = listOf("H.264", "H.265")
            )
        )
    }
    
    fun isChromecastSupported(): Boolean {
        return try {
            // Check if Google Play Services is available (needed for Chromecast)
            context.packageManager.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun createChromecastDevice(): MirroringDevice {
        return MirroringDevice(
            name = "Chromecast",
            id = "chromecast_default",
            type = MirroringType.CHROMECAST,
            protocol = MirroringProtocol.GOOGLE_CAST,
            isAvailable = true,
            isConnected = false,
            capabilities = MirroringCapabilities(
                maxResolution = "3840x2160",
                maxFrameRate = 60,
                audioSupported = true,
                hdcpSupported = true,
                latency = 100,
                codecSupport = listOf("H.264", "H.265", "VP8", "VP9")
            )
        )
    }
    
    fun isDlnaSupported(): Boolean {
        // DLNA support is generally available on most Android devices with WiFi
        return capabilities.wifi?.isEnabled ?: false
    }
    
    private fun createDlnaDevice(): MirroringDevice {
        return MirroringDevice(
            name = "DLNA",
            id = "dlna_default",
            type = MirroringType.DLNA,
            protocol = MirroringProtocol.DLNA,
            isAvailable = true,
            isConnected = false,
            capabilities = MirroringCapabilities(
                maxResolution = "1920x1080",
                maxFrameRate = 30,
                audioSupported = true,
                hdcpSupported = false,
                latency = 200,
                codecSupport = listOf("H.264", "MPEG-4")
            )
        )
    }
    
    private fun createSamsungDevice(): MirroringDevice {
        return MirroringDevice(
            name = "Smart View",
            id = "samsung_smart_view",
            type = MirroringType.SAMSUNG_SMART_VIEW,
            protocol = MirroringProtocol.PROPRIETARY,
            isAvailable = true,
            isConnected = false,
            capabilities = MirroringCapabilities(
                maxResolution = "3840x2160",
                maxFrameRate = 60,
                audioSupported = true,
                hdcpSupported = true,
                latency = 50,
                codecSupport = listOf("H.264", "H.265")
            )
        )
    }
    
    private fun createLGDevice(): MirroringDevice {
        return MirroringDevice(
            name = "Screen Share",
            id = "lg_screen_share",
            type = MirroringType.LG_SCREEN_SHARE,
            protocol = MirroringProtocol.PROPRIETARY,
            isAvailable = true,
            isConnected = false,
            capabilities = MirroringCapabilities(
                maxResolution = "1920x1080",
                maxFrameRate = 60,
                audioSupported = true,
                hdcpSupported = true,
                latency = 75,
                codecSupport = listOf("H.264")
            )
        )
    }
    
    // ========== BROADCAST RECEIVERS ==========
    
    private val wifiReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiManager.WIFI_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, WifiManager.WIFI_STATE_UNKNOWN)
                    handleWifiStateChanged(state)
                }
                WifiManager.SCAN_RESULTS_AVAILABLE_ACTION -> {
                    processScanResults()
                }
                WifiManager.NETWORK_STATE_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiManager.EXTRA_NETWORK_INFO)
                    handleNetworkStateChanged(networkInfo)
                }
            }
        }
    }
    
    private val p2pReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                    val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                    _wifiState.update { it.copy(p2pEnabled = state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) }
                }
                WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                    requestP2pPeers()
                }
                WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                    val networkInfo = intent.getParcelableExtra<NetworkInfo>(WifiP2pManager.EXTRA_NETWORK_INFO)
                    handleP2pConnectionChanged(networkInfo)
                }
            }
        }
    }
    
    private fun handleWifiStateChanged(state: Int) {
        val isEnabled = state == WifiManager.WIFI_STATE_ENABLED
        _wifiState.update { it.copy(isEnabled = isEnabled) }
    }
    
    private fun handleNetworkStateChanged(networkInfo: NetworkInfo?) {
        val isConnected = networkInfo?.isConnected == true
        _wifiState.update { it.copy(isConnected = isConnected) }
        
        if (isConnected) {
            updateCurrentNetwork()
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun updateCurrentNetwork() {
        val wifiInfo = wifiManager.connectionInfo
        wifiInfo?.let { info ->
            val network = WiFiNetwork(
                ssid = info.ssid?.removeSurrounding("\"") ?: "",
                bssid = info.bssid ?: "",
                capabilities = "",
                level = info.rssi,
                frequency = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    info.frequency
                } else 0,
                channelWidth = ChannelWidth.WIDTH_20,
                standard = WiFiStandard.LEGACY,
                securityType = SecurityType.OPEN,
                isPasspoint = false,
                isWiFi6E = false,
                isWiFi7 = false,
                maxLinkSpeed = info.linkSpeed,
                distance = null,
                venue = null,
                operatorName = null,
                isHidden = info.hiddenSSID,
                timestamp = System.currentTimeMillis()
            )
            
            _wifiState.update { it.copy(currentNetwork = network) }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun requestP2pPeers() {
        wifiP2pManager?.requestPeers(wifiP2pChannel) { peers ->
            val deviceList = peers.deviceList.map { device ->
                P2PDevice(
                    deviceName = device.deviceName,
                    deviceAddress = device.deviceAddress,
                    primaryDeviceType = device.primaryDeviceType,
                    status = when (device.status) {
                        WifiP2pDevice.CONNECTED -> P2PStatus.CONNECTED
                        WifiP2pDevice.INVITED -> P2PStatus.INVITED
                        WifiP2pDevice.FAILED -> P2PStatus.FAILED
                        WifiP2pDevice.AVAILABLE -> P2PStatus.AVAILABLE
                        else -> P2PStatus.UNAVAILABLE
                    },
                    wpsConfigMethods = 0, // wpsConfigMethodsSupported not available in current API
                    deviceCapability = 0, // deviceCapability not available in current API
                    groupCapability = 0, // groupCapability not available in current API
                    isGroupOwner = device.isGroupOwner,
                    isServiceDiscoveryCapable = device.isServiceDiscoveryCapable
                )
            }
            _p2pDevices.value = deviceList
        }
    }
    
    private fun handleP2pConnectionChanged(networkInfo: NetworkInfo?) {
        if (networkInfo?.isConnected == true) {
            Log.d(TAG, "P2P connection established")
        }
    }
    
    private fun registerReceivers() {
        // WiFi receiver
        val wifiFilter = IntentFilter().apply {
            addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
            addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
            addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION)
        }
        context.registerReceiver(wifiReceiver, wifiFilter)
        
        // P2P receiver
        val p2pFilter = IntentFilter().apply {
            addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
            addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
        context.registerReceiver(p2pReceiver, p2pFilter)
    }
    
    // ========== UTILITY METHODS ==========
    
    fun isWifiEnabled(): Boolean = wifiManager.isWifiEnabled
    
    @RequiresPermission(Manifest.permission.CHANGE_WIFI_STATE)
    fun setWifiEnabled(enabled: Boolean): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ requires user to enable WiFi through settings
            false
        } else {
            @Suppress("DEPRECATION")
            wifiManager.isWifiEnabled = enabled
            true
        }
    }
    
    fun cleanup() {
        scope.cancel()
        try {
            context.unregisterReceiver(wifiReceiver)
            context.unregisterReceiver(p2pReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receivers", e)
        }
        
        wifiAwareSession?.close()
    }
}
