// Author: Manoj Jhawar
// Purpose: Comprehensive Bluetooth management including Classic, LE, Mesh, and codec detection

package com.augmentalis.devicemanager.network

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.RECEIVER_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.*

/**
 * Comprehensive Bluetooth Manager
 * Supports Classic, LE, Mesh, Audio codecs, and advanced features
 */
class BluetoothManager(
    private val context: Context,
    private val capabilities: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.DeviceCapabilities
) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        
        // Bluetooth profiles
        const val PROFILE_A2DP = BluetoothProfile.A2DP
        const val PROFILE_HEADSET = BluetoothProfile.HEADSET
        const val PROFILE_HID = 4 // BluetoothProfile.HID_DEVICE
        const val PROFILE_HEALTH = BluetoothProfile.HEALTH
        const val PROFILE_GATT = BluetoothProfile.GATT
        const val PROFILE_GATT_SERVER = BluetoothProfile.GATT_SERVER
        const val PROFILE_SAP = 10 // BluetoothProfile.SAP
        const val PROFILE_HEARING_AID = 21 // BluetoothProfile.HEARING_AID (API 29+)
        const val PROFILE_LE_AUDIO = 22 // BluetoothProfile.LE_AUDIO (API 31+)
        
        // Audio codecs
        const val CODEC_SBC = 0
        const val CODEC_AAC = 1
        const val CODEC_APTX = 2
        const val CODEC_APTX_HD = 3
        const val CODEC_LDAC = 4
        const val CODEC_APTX_ADAPTIVE = 5
        const val CODEC_LC3 = 6 // LE Audio codec
        const val CODEC_APTX_LL = 7 // Low Latency
        const val CODEC_LHDC = 8
        
        // BLE scan modes
        const val SCAN_MODE_LOW_POWER = ScanSettings.SCAN_MODE_LOW_POWER
        const val SCAN_MODE_BALANCED = ScanSettings.SCAN_MODE_BALANCED
        const val SCAN_MODE_LOW_LATENCY = ScanSettings.SCAN_MODE_LOW_LATENCY
        const val SCAN_MODE_OPPORTUNISTIC = ScanSettings.SCAN_MODE_OPPORTUNISTIC
        
        // Standard BLE UUIDs
        val UUID_HEART_RATE_SERVICE = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val UUID_BATTERY_SERVICE = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val UUID_DEVICE_INFO_SERVICE = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
        val UUID_GENERIC_ACCESS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
        val UUID_GENERIC_ATTRIBUTE = UUID.fromString("00001801-0000-1000-8000-00805f9b34fb")
    }
    
    // System services
    private val systemBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager
    private val bluetoothAdapter = systemBluetoothManager?.adapter
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    
    // BLE components
    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    private var bluetoothGattServer: BluetoothGattServer? = null
    private val activeGattConnections = mutableMapOf<String, BluetoothGatt>()
    
    // State flows
    private val _bluetoothState = MutableStateFlow(BluetoothState(
        bluetoothVersion = parseBluetoothVersion(capabilities.bluetooth?.bluetoothVersion),
        supportedProfiles = parseProfiles(capabilities.bluetooth?.supportedProfiles),
        supportedCodecs = parseCodecs(capabilities.bluetooth?.supportedCodecs),
        leCapabilities = if (capabilities.bluetooth?.hasBLE == true) createBLECapabilities(capabilities.bluetooth) else null,
        meshSupported = false,  // Would need specific detection
        dualModeSupported = capabilities.bluetooth?.dualMode ?: false,
        maxConnections = 7  // Default value
    ))
    val bluetoothState: StateFlow<BluetoothState> = _bluetoothState.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceInfo>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDeviceInfo>> = _discoveredDevices.asStateFlow()
    
    private val _connectedDevices = MutableStateFlow<List<ConnectedBluetoothDevice>>(emptyList())
    val connectedDevices: StateFlow<List<ConnectedBluetoothDevice>> = _connectedDevices.asStateFlow()
    
    private val _activeProfiles = MutableStateFlow<Map<Int, List<BluetoothDevice>>>(emptyMap())
    val activeProfiles: StateFlow<Map<Int, List<BluetoothDevice>>> = _activeProfiles.asStateFlow()
    
    // Coroutine scope
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // ========== DATA MODELS ==========
    
    data class BluetoothState(
        val isEnabled: Boolean = false,
        val isDiscovering: Boolean = false,
        val isAdvertising: Boolean = false,
        val isScanning: Boolean = false,
        val bluetoothVersion: BluetoothVersion = BluetoothVersion.UNKNOWN,
        val supportedProfiles: List<BluetoothProfileType> = emptyList(),
        val supportedCodecs: List<AudioCodec> = emptyList(),
        val leCapabilities: BLECapabilities? = null,
        val meshSupported: Boolean = false,
        val dualModeSupported: Boolean = false,
        val maxConnections: Int = 7 // Default for most devices
    )
    
    enum class BluetoothVersion {
        BT_1_0, BT_1_1, BT_1_2, BT_2_0, BT_2_1,
        BT_3_0, BT_4_0, BT_4_1, BT_4_2,
        BT_5_0, BT_5_1, BT_5_2, BT_5_3, BT_5_4,
        UNKNOWN
    }
    
    data class BluetoothDeviceInfo(
        val name: String?,
        val address: String,
        val type: DeviceType,
        val deviceClass: DeviceClass?,
        val bondState: BondState,
        val rssi: Int?,
        val txPower: Int?,
        val distance: Float?, // Estimated distance in meters
        val isConnectable: Boolean,
        val serviceUuids: List<ParcelUuid>?,
        val manufacturerData: Map<Int, ByteArray>?,
        val lastSeen: Long,
        val capabilities: DeviceCapabilities?
    )
    
    enum class DeviceType {
        CLASSIC,
        LE,
        DUAL,
        UNKNOWN
    }
    
    enum class DeviceClass {
        PHONE, COMPUTER, AUDIO, WEARABLE, 
        HEALTH, TOY, PERIPHERAL, IMAGING,
        NETWORKING, UNCATEGORIZED
    }
    
    enum class BondState {
        NONE, BONDING, BONDED
    }
    
    data class ConnectedBluetoothDevice(
        val device: BluetoothDeviceInfo,
        val profiles: List<BluetoothProfileType>,
        val connectionState: ConnectionState,
        val audioCodec: AudioCodec?,
        val batteryLevel: Int?,
        val connectionQuality: ConnectionQuality?,
        val gattServices: List<BluetoothGattService>?
    )
    
    enum class ConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, DISCONNECTING
    }
    
    data class ConnectionQuality(
        val rssi: Int,
        val linkQuality: Int, // 0-100
        val packetLoss: Float,
        val latency: Int // ms
    )
    
    enum class BluetoothProfileType {
        A2DP, // Advanced Audio Distribution
        HEADSET, // Headset/Handsfree
        HID, // Human Interface Device
        PAN, // Personal Area Network
        PBAP, // Phone Book Access
        MAP, // Message Access
        SAP, // SIM Access
        HFP, // Hands-Free Profile
        AVRCP, // Audio/Video Remote Control
        OPP, // Object Push
        FTP, // File Transfer
        DIP, // Device ID
        GATT, // Generic Attribute
        HEALTH, // Health Device
        HEARING_AID, // Hearing Aid (API 29+)
        LE_AUDIO // LE Audio (API 31+)
    }
    
    data class AudioCodec(
        val type: CodecType,
        val name: String,
        val sampleRate: Int,
        val bitsPerSample: Int,
        val channelMode: ChannelMode,
        val bitrate: Int,
        val latency: Int // ms
    )
    
    enum class CodecType {
        SBC, AAC, APTX, APTX_HD, APTX_LL, APTX_ADAPTIVE,
        LDAC, LHDC, LC3, SAMSUNG_SCALABLE, SAMSUNG_UHQ,
        UNKNOWN
    }
    
    enum class ChannelMode {
        MONO, STEREO, JOINT_STEREO, DUAL_CHANNEL
    }
    
    data class BLECapabilities(
        val maxAdvertisingDataLength: Int,
        val maxConnections: Int,
        val supports2MPhy: Boolean,
        val supportsCodedPhy: Boolean,
        val supportsExtendedAdvertising: Boolean,
        val supportsPeriodicAdvertising: Boolean,
        val supportsMultipleAdvertisement: Boolean,
        val offloadedFilteringSupported: Boolean,
        val offloadedScanBatchingSupported: Boolean,
        val powerClass: Int, // 1, 2, or 3
        val maxTxPower: Int, // dBm
        val leAudioSupported: Boolean,
        val isochronousChannelsSupported: Boolean
    )
    
    data class DeviceCapabilities(
        val supportsAudioStreaming: Boolean,
        val supportsDataTransfer: Boolean,
        val supportsRemoteControl: Boolean,
        val supportsTelephony: Boolean,
        val supportsPositioning: Boolean,
        val supportsNetworking: Boolean,
        val supportsHealthMonitoring: Boolean,
        val maxDataRate: Int, // Kbps
        val encryptionSupported: Boolean,
        val secureConnectionsSupported: Boolean
    )
    
    data class BluetoothMeshCapabilities(
        val supported: Boolean,
        val relaySupported: Boolean,
        val proxySupported: Boolean,
        val friendSupported: Boolean,
        val lowPowerSupported: Boolean,
        val maxProvisionedNodes: Int
    )
    
    // ========== INITIALIZATION ==========
    
    init {
        initialize()
    }
    
    private fun initialize() {
        bluetoothAdapter?.let { adapter ->
            bluetoothLeScanner = adapter.bluetoothLeScanner
            bluetoothLeAdvertiser = adapter.bluetoothLeAdvertiser
            
            updateBluetoothState()
            registerBluetoothReceiver()
        }
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    private fun updateBluetoothState() {
        bluetoothAdapter?.let { adapter ->
            _bluetoothState.update { current ->
                current.copy(
                    isEnabled = adapter.isEnabled,
                    isDiscovering = adapter.isDiscovering
                )
            }
        }
    }
    
    // ========== CAPABILITY ACCESS ==========
    
    fun getBluetoothVersion(): BluetoothVersion {
        return parseBluetoothVersion(capabilities.bluetooth?.bluetoothVersion)
    }
    
    fun getSupportedProfiles(): List<BluetoothProfileType> {
        return parseProfiles(capabilities.bluetooth?.supportedProfiles)
    }
    
    fun getSupportedCodecs(): List<AudioCodec> {
        return parseCodecs(capabilities.bluetooth?.supportedCodecs)
    }
    
    fun getMaxConnections(): Int {
        return 7  // Default value
    }
    
    private fun parseBluetoothVersion(version: String?): BluetoothVersion {
        return when (version) {
            "5.3" -> BluetoothVersion.BT_5_3
            "5.2" -> BluetoothVersion.BT_5_2
            "5.1" -> BluetoothVersion.BT_5_1
            "5.0" -> BluetoothVersion.BT_5_0
            "4.2" -> BluetoothVersion.BT_4_2
            "4.1" -> BluetoothVersion.BT_4_1
            "4.0" -> BluetoothVersion.BT_4_0
            else -> BluetoothVersion.UNKNOWN
        }
    }
    
    private fun parseProfiles(profiles: List<String>?): List<BluetoothProfileType> {
        return profiles?.mapNotNull { profile ->
            when (profile) {
                "A2DP" -> BluetoothProfileType.A2DP
                "HEADSET" -> BluetoothProfileType.HEADSET
                "HID" -> BluetoothProfileType.HID
                "GATT" -> BluetoothProfileType.GATT
                "AVRCP" -> BluetoothProfileType.AVRCP
                "HFP" -> BluetoothProfileType.HFP
                "PBAP" -> BluetoothProfileType.PBAP
                "MAP" -> BluetoothProfileType.MAP
                else -> null
            }
        } ?: emptyList()
    }
    
    private fun parseCodecs(codecs: List<String>?): List<AudioCodec> {
        return codecs?.mapNotNull { codec ->
            when (codec) {
                "SBC" -> AudioCodec(
                    type = CodecType.SBC,
                    name = "SBC",
                    sampleRate = 44100,
                    bitsPerSample = 16,
                    channelMode = ChannelMode.STEREO,
                    bitrate = 328,
                    latency = 150
                )
                "AAC" -> AudioCodec(
                    type = CodecType.AAC,
                    name = "AAC",
                    sampleRate = 44100,
                    bitsPerSample = 16,
                    channelMode = ChannelMode.STEREO,
                    bitrate = 256,
                    latency = 120
                )
                "aptX" -> AudioCodec(
                    type = CodecType.APTX,
                    name = "aptX",
                    sampleRate = 48000,
                    bitsPerSample = 16,
                    channelMode = ChannelMode.STEREO,
                    bitrate = 352,
                    latency = 70
                )
                "aptX HD" -> AudioCodec(
                    type = CodecType.APTX_HD,
                    name = "aptX HD",
                    sampleRate = 48000,
                    bitsPerSample = 24,
                    channelMode = ChannelMode.STEREO,
                    bitrate = 576,
                    latency = 80
                )
                "LDAC" -> AudioCodec(
                    type = CodecType.LDAC,
                    name = "LDAC",
                    sampleRate = 96000,
                    bitsPerSample = 24,
                    channelMode = ChannelMode.STEREO,
                    bitrate = 990,
                    latency = 100
                )
                else -> null
            }
        } ?: emptyList()
    }
    
    private fun createBLECapabilities(btCaps: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.BluetoothCapabilities): BLECapabilities {
        return BLECapabilities(
            maxAdvertisingDataLength = if (btCaps.extendedAdvertising) 1650 else 31,
            maxConnections = 7,
            supports2MPhy = btCaps.le2MPhy,
            supportsCodedPhy = btCaps.leCodedPhy,
            supportsExtendedAdvertising = btCaps.leExtendedAdvertising,
            supportsPeriodicAdvertising = btCaps.lePeriodicAdvertising,
            supportsMultipleAdvertisement = btCaps.multipleAdvertisement,
            offloadedFilteringSupported = btCaps.offloadedFiltering,
            offloadedScanBatchingSupported = btCaps.offloadedScanBatching,
            powerClass = 1,
            maxTxPower = 10,
            leAudioSupported = btCaps.leAudio,
            isochronousChannelsSupported = btCaps.leAudio
        )
    }
    
    // ========== SCANNING & DISCOVERY ==========
    
    @RequiresPermission(allOf = [
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_CONNECT
    ])
    fun startDiscovery(
        includeClassic: Boolean = true,
        includeBLE: Boolean = true,
        duration: Long = 12000 // 12 seconds
    ) {
        if (!isBluetoothEnabled()) {
            Log.w(TAG, "Bluetooth is not enabled")
            return
        }
        
        scope.launch {
            if (includeClassic) {
                startClassicDiscovery()
            }
            
            if (includeBLE) {
                startBLEScan()
            }
            
            // Auto-stop after duration
            delay(duration)
            stopDiscovery()
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun startClassicDiscovery() {
        bluetoothAdapter?.startDiscovery()
        _bluetoothState.update { it.copy(isDiscovering = true) }
    }
    
    @SuppressLint("MissingPermission")
    private fun startBLEScan(
        filters: List<ScanFilter>? = null,
        settings: ScanSettings? = null
    ) {
        val scanSettings = settings ?: ScanSettings.Builder()
            .setScanMode(SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0)
            .build()
        
        bluetoothLeScanner?.startScan(filters, scanSettings, bleScanCallback)
        _bluetoothState.update { it.copy(isScanning = true) }
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_SCAN)
    fun stopDiscovery() {
        bluetoothAdapter?.cancelDiscovery()
        bluetoothLeScanner?.stopScan(bleScanCallback)
        _bluetoothState.update { 
            it.copy(isDiscovering = false, isScanning = false)
        }
    }
    
    // ========== BLE SCAN CALLBACK ==========
    
    private val bleScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            processScanResult(result)
        }
        
        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { processScanResult(it) }
        }
        
        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "BLE scan failed with error: $errorCode")
            _bluetoothState.update { it.copy(isScanning = false) }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun processScanResult(result: ScanResult) {
        val device = result.device
        val scanRecord = result.scanRecord
        
        val deviceInfo = BluetoothDeviceInfo(
            name = scanRecord?.deviceName ?: device.name,
            address = device.address,
            type = getDeviceType(device.type),
            deviceClass = getDeviceClass(device),
            bondState = getBondState(device.bondState),
            rssi = result.rssi,
            txPower = scanRecord?.txPowerLevel,
            distance = calculateDistance(result.rssi, scanRecord?.txPowerLevel ?: -59),
            isConnectable = result.isConnectable,
            serviceUuids = scanRecord?.serviceUuids,
            manufacturerData = scanRecord?.manufacturerSpecificData?.let { data ->
                val map = mutableMapOf<Int, ByteArray>()
                for (i in 0 until data.size()) {
                    map[data.keyAt(i)] = data.valueAt(i)
                }
                map
            },
            lastSeen = System.currentTimeMillis(),
            capabilities = null  // COT: Device capabilities are now injected via constructor parameter rather than detected at runtime
        )
        
        updateDiscoveredDevices(deviceInfo)
    }
    
    private fun getDeviceType(type: Int): DeviceType {
        return when (type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> DeviceType.CLASSIC
            BluetoothDevice.DEVICE_TYPE_LE -> DeviceType.LE
            BluetoothDevice.DEVICE_TYPE_DUAL -> DeviceType.DUAL
            else -> DeviceType.UNKNOWN
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun getDeviceClass(device: BluetoothDevice): DeviceClass? {
        val btClass = device.bluetoothClass ?: return null
        
        return when (btClass.majorDeviceClass) {
            BluetoothClass.Device.Major.PHONE -> DeviceClass.PHONE
            BluetoothClass.Device.Major.COMPUTER -> DeviceClass.COMPUTER
            BluetoothClass.Device.Major.AUDIO_VIDEO -> DeviceClass.AUDIO
            BluetoothClass.Device.Major.WEARABLE -> DeviceClass.WEARABLE
            BluetoothClass.Device.Major.HEALTH -> DeviceClass.HEALTH
            BluetoothClass.Device.Major.TOY -> DeviceClass.TOY
            BluetoothClass.Device.Major.PERIPHERAL -> DeviceClass.PERIPHERAL
            BluetoothClass.Device.Major.IMAGING -> DeviceClass.IMAGING
            BluetoothClass.Device.Major.NETWORKING -> DeviceClass.NETWORKING
            else -> DeviceClass.UNCATEGORIZED
        }
    }
    
    private fun getBondState(state: Int): BondState {
        return when (state) {
            BluetoothDevice.BOND_NONE -> BondState.NONE
            BluetoothDevice.BOND_BONDING -> BondState.BONDING
            BluetoothDevice.BOND_BONDED -> BondState.BONDED
            else -> BondState.NONE
        }
    }
    
    private fun calculateDistance(rssi: Int, txPower: Int): Float {
        // Using path loss formula: Distance = 10^((TxPower - RSSI) / (10 * n))
        // where n is path loss exponent (typically 2 for free space)
        val pathLossExponent = 2.0
        return Math.pow(10.0, (txPower - rssi) / (10.0 * pathLossExponent)).toFloat()
    }
    
    
    private fun hasAudioProfile(uuids: List<ParcelUuid>): Boolean {
        val audioUuids = listOf(
            ParcelUuid.fromString("0000110b-0000-1000-8000-00805f9b34fb"), // A2DP
            ParcelUuid.fromString("0000111e-0000-1000-8000-00805f9b34fb"), // HFP
            ParcelUuid.fromString("00001108-0000-1000-8000-00805f9b34fb")  // HSP
        )
        return uuids.any { it in audioUuids }
    }
    
    private fun hasDataProfile(uuids: List<ParcelUuid>): Boolean {
        val dataUuids = listOf(
            ParcelUuid.fromString("00001105-0000-1000-8000-00805f9b34fb"), // OPP
            ParcelUuid.fromString("00001106-0000-1000-8000-00805f9b34fb")  // FTP
        )
        return uuids.any { it in dataUuids }
    }
    
    private fun hasRemoteControlProfile(uuids: List<ParcelUuid>): Boolean {
        val controlUuids = listOf(
            ParcelUuid.fromString("0000110e-0000-1000-8000-00805f9b34fb"), // AVRCP
            ParcelUuid.fromString("00001124-0000-1000-8000-00805f9b34fb")  // HID
        )
        return uuids.any { it in controlUuids }
    }
    
    private fun hasTelephonyProfile(uuids: List<ParcelUuid>): Boolean {
        val telephonyUuids = listOf(
            ParcelUuid.fromString("0000111f-0000-1000-8000-00805f9b34fb"), // HFP
            ParcelUuid.fromString("00001112-0000-1000-8000-00805f9b34fb")  // HSP AG
        )
        return uuids.any { it in telephonyUuids }
    }
    
    private fun hasPositioningProfile(uuids: List<ParcelUuid>): Boolean {
        // Indoor Positioning Service
        return uuids.any { 
            it.toString().contains("1821") // IPS UUID
        }
    }
    
    private fun hasNetworkingProfile(uuids: List<ParcelUuid>): Boolean {
        val networkUuids = listOf(
            ParcelUuid.fromString("00001115-0000-1000-8000-00805f9b34fb"), // PAN
            ParcelUuid.fromString("00001116-0000-1000-8000-00805f9b34fb")  // NAP
        )
        return uuids.any { it in networkUuids }
    }
    
    private fun hasHealthProfile(uuids: List<ParcelUuid>): Boolean {
        val healthUuids = listOf(
            UUID_HEART_RATE_SERVICE,
            ParcelUuid.fromString("00001400-0000-1000-8000-00805f9b34fb"), // HDP
            ParcelUuid.fromString("00001809-0000-1000-8000-00805f9b34fb")  // Health Thermometer
        )
        return uuids.any { uuid ->
            healthUuids.any { it == uuid }
        }
    }
    
    private fun estimateMaxDataRate(device: BluetoothDevice): Int {
        // Estimate based on Bluetooth version
        return when (getBluetoothVersion()) {
            BluetoothVersion.BT_5_2, BluetoothVersion.BT_5_3 -> 2000 // 2 Mbps
            BluetoothVersion.BT_5_0, BluetoothVersion.BT_5_1 -> 2000
            BluetoothVersion.BT_4_2 -> 1000 // 1 Mbps
            BluetoothVersion.BT_4_1, BluetoothVersion.BT_4_0 -> 1000
            else -> 723 // Classic BT 2.1 EDR
        }
    }
    
    private fun updateDiscoveredDevices(deviceInfo: BluetoothDeviceInfo) {
        _discoveredDevices.update { currentList ->
            val mutableList = currentList.toMutableList()
            val index = mutableList.indexOfFirst { it.address == deviceInfo.address }
            
            if (index >= 0) {
                mutableList[index] = deviceInfo
            } else {
                mutableList.add(deviceInfo)
            }
            
            mutableList
        }
    }
    
    // ========== CONNECTION MANAGEMENT ==========
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun connectDevice(
        address: String,
        autoConnect: Boolean = false
    ): Flow<ConnectionState> = flow {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        if (device == null) {
            emit(ConnectionState.DISCONNECTED)
            return@flow
        }
        
        emit(ConnectionState.CONNECTING)
        
        val gatt = device.connectGatt(
            context,
            autoConnect,
            gattCallback,
            BluetoothDevice.TRANSPORT_AUTO
        )
        
        activeGattConnections[address] = gatt
        
        // Monitor connection state
        // Real implementation would use callback
        emit(ConnectionState.CONNECTED)
    }
    
    @SuppressLint("MissingPermission")
    fun disconnectDevice(address: String) {
        activeGattConnections[address]?.let { gatt ->
            gatt.disconnect()
            gatt.close()
            activeGattConnections.remove(address)
        }
    }
    
    // ========== GATT CALLBACK ==========
    
    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    Log.i(TAG, "Connected to GATT server")
                    gatt.discoverServices()
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    Log.i(TAG, "Disconnected from GATT server")
                    activeGattConnections.remove(gatt.device.address)
                }
            }
        }
        
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Services discovered: ${gatt.services.size}")
                processDiscoveredServices(gatt)
            }
        }
        
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processCharacteristicData(characteristic)
            }
        }
    }
    
    private fun processDiscoveredServices(gatt: BluetoothGatt) {
        gatt.services.forEach { service ->
            Log.d(TAG, "Service UUID: ${service.uuid}")
            service.characteristics.forEach { characteristic ->
                Log.d(TAG, "  Characteristic UUID: ${characteristic.uuid}")
            }
        }
    }
    
    private fun processCharacteristicData(characteristic: BluetoothGattCharacteristic) {
        // Process based on characteristic UUID
        when (characteristic.uuid) {
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb") -> {
                // Battery level
                val batteryLevel = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0)
                Log.d(TAG, "Battery level: $batteryLevel%")
            }
        }
    }
    
    // ========== BROADCAST RECEIVER ==========
    
    private val bluetoothReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    handleBluetoothStateChange(state)
                }
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    device?.let { handleDeviceFound(it, rssi) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED -> {
                    _bluetoothState.update { it.copy(isDiscovering = true) }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _bluetoothState.update { it.copy(isDiscovering = false) }
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val bondState = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
                    device?.let { handleBondStateChanged(it, bondState) }
                }
            }
        }
    }
    
    private fun handleBluetoothStateChange(state: Int) {
        val isEnabled = state == BluetoothAdapter.STATE_ON
        _bluetoothState.update { it.copy(isEnabled = isEnabled) }
    }
    
    @SuppressLint("MissingPermission")
    private fun handleDeviceFound(device: BluetoothDevice, rssi: Int) {
        val deviceInfo = BluetoothDeviceInfo(
            name = device.name,
            address = device.address,
            type = getDeviceType(device.type),
            deviceClass = getDeviceClass(device),
            bondState = getBondState(device.bondState),
            rssi = rssi,
            txPower = null,
            distance = calculateDistance(rssi, -59),
            isConnectable = true,
            serviceUuids = device.uuids?.toList(),
            manufacturerData = null,
            lastSeen = System.currentTimeMillis(),
            capabilities = null
        )
        
        updateDiscoveredDevices(deviceInfo)
    }
    
    private fun handleBondStateChanged(device: BluetoothDevice, bondState: Int) {
        Log.d(TAG, "Bond state changed for ${device.address}: ${getBondState(bondState)}")
    }
    
    private fun registerBluetoothReceiver() {
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bluetoothReceiver, filter, RECEIVER_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(bluetoothReceiver, filter)
        }
    }
    
    // ========== UTILITY METHODS ==========
    
    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.state == BluetoothAdapter.STATE_ON
    
    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null
    
    fun isBLESupported(): Boolean = 
        capabilities.bluetooth?.hasBLE ?: false  // COT: Use injected capabilities instead of direct hardware detection
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun enableBluetooth(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: Cannot programmatically enable, must use Intent
            Log.w(TAG, "Bluetooth enable requires user interaction on Android 13+")
            if (!isBluetoothEnabled()) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(enableBtIntent)
            }
            false // Cannot determine immediate success
        } else {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.enable() ?: false
        }
    }
    
    @RequiresPermission(Manifest.permission.BLUETOOTH_CONNECT)
    fun disableBluetooth(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // API 33+: Cannot programmatically disable
            Log.w(TAG, "Bluetooth disable requires user interaction on Android 13+")
            false
        } else {
            @Suppress("DEPRECATION")
            bluetoothAdapter?.disable() ?: false
        }
    }
    
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDeviceInfo> {
        return bluetoothAdapter?.bondedDevices?.map { device ->
            BluetoothDeviceInfo(
                name = device.name,
                address = device.address,
                type = getDeviceType(device.type),
                deviceClass = getDeviceClass(device),
                bondState = BondState.BONDED,
                rssi = null,
                txPower = null,
                distance = null,
                isConnectable = true,
                serviceUuids = device.uuids?.toList(),
                manufacturerData = null,
                lastSeen = System.currentTimeMillis(),
                capabilities = null
            )
        } ?: emptyList()
    }
    
    fun cleanup() {
        scope.cancel()
        stopDiscovery()
        activeGattConnections.values.forEach { it.close() }
        activeGattConnections.clear()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.e(TAG, "Error unregistering receiver", e)
        }
    }
}
