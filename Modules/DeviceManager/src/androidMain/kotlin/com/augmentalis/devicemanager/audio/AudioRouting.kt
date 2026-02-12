package com.augmentalis.devicemanager.audio

import android.bluetooth.BluetoothProfile
import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.augmentalis.devicemanager.network.BluetoothManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Handles audio device routing and path management with Bluetooth integration
 * 
 * COT: This class manages audio routing decisions by:
 * 1. Detecting available audio devices (built-in, wired, Bluetooth)
 * 2. Managing Bluetooth audio profiles (SCO for voice, A2DP for media)
 * 3. Providing intelligent routing based on device capabilities and user preferences
 * 4. Maintaining state synchronization between audio system and Bluetooth stack
 */
class AudioRouting(
    private val context: Context,
    private val bluetoothManager: BluetoothManager? = null
) {
    
    companion object {
        private const val TAG = "AudioRouting"
    }
    
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _connectedDevices = MutableStateFlow<List<AudioDevice>>(emptyList())
    val connectedDevices: StateFlow<List<AudioDevice>> = _connectedDevices
    
    private val _activeDevice = MutableStateFlow<AudioDevice?>(null)
    val activeDevice: StateFlow<AudioDevice?> = _activeDevice
    
    // Bluetooth-specific state management
    private val _bluetoothAudioState = MutableStateFlow(BluetoothAudioState())
    val bluetoothAudioState: StateFlow<BluetoothAudioState> = _bluetoothAudioState
    
    private val _routingState = MutableStateFlow(AudioRoutingState())
    val routingState: StateFlow<AudioRoutingState> = _routingState
    
    private val deviceCallback = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        object : AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
                scanDevices()
                Log.d(TAG, "Devices added: ${addedDevices.size}")
            }
            
            override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
                scanDevices()
                Log.d(TAG, "Devices removed: ${removedDevices.size}")
            }
        }
    } else null
    
    init {
        scanDevices()
        registerCallback()
        initializeBluetoothIntegration()
    }
    
    /**
     * COT: Initialize Bluetooth integration to synchronize audio routing with BT device state.
     * This ensures we can intelligently route audio based on connected BT devices and their capabilities.
     */
    private fun initializeBluetoothIntegration() {
        bluetoothManager?.let { btManager ->
            scope.launch {
                // Monitor connected Bluetooth devices for audio routing decisions
                btManager.connectedDevices.collect { devices ->
                    updateBluetoothAudioDevices(devices)
                }
            }
        }
    }
    
    /**
     * Get all available audio devices
     */
    fun getDevices(includeInputs: Boolean = true, includeOutputs: Boolean = true): List<AudioDevice> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val type = when {
                includeInputs && includeOutputs -> AudioManager.GET_DEVICES_ALL
                includeInputs -> AudioManager.GET_DEVICES_INPUTS
                includeOutputs -> AudioManager.GET_DEVICES_OUTPUTS
                else -> return emptyList()
            }
            
            audioManager.getDevices(type).map { device ->
                AudioDevice(
                    id = device.id,
                    name = device.productName?.toString() ?: "Unknown",
                    type = getDeviceTypeString(device.type),
                    isInput = device.isSource,
                    isOutput = device.isSink,
                    channelMasks = device.channelMasks.toList(),
                    channelCounts = device.channelCounts.toList(),
                    sampleRates = device.sampleRates.toList(),
                    encodings = device.encodings.toList(),
                    address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        device.address ?: ""
                    } else "",
                    isBluetooth = isBluetoothDevice(device.type),
                    isUsb = isUsbDevice(device.type),
                    isBuiltIn = isBuiltInDevice(device.type)
                )
            }
        } else {
            emptyList()
        }
    }
    
    /**
     * Route audio to specific device
     */
    fun routeToDevice(deviceId: Int): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            val targetDevice = devices.firstOrNull { it.id == deviceId }
            
            return if (targetDevice != null) {
                audioManager.setCommunicationDevice(targetDevice)
                Log.d(TAG, "Routed to: ${targetDevice.productName}")
                true
            } else {
                Log.w(TAG, "Device not found: $deviceId")
                false
            }
        }
        return false
    }
    
    /**
     * Clear audio routing preference
     */
    fun clearRouting() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
            Log.d(TAG, "Routing cleared")
        }
    }
    
    /**
     * Enable/disable speakerphone
     */
    fun setSpeakerphone(enabled: Boolean) {
        try {
            @Suppress("DEPRECATION")
            audioManager.isSpeakerphoneOn = enabled
            Log.d(TAG, "Speakerphone: $enabled")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set speakerphone", e)
        }
    }
    
    /**
     * Start Bluetooth SCO for voice with enhanced device selection
     * 
     * COT: SCO (Synchronous Connection-Oriented) is the Bluetooth profile used for voice calls.
     * We check for available SCO devices and route audio appropriately, with fallback handling.
     */
    fun startBluetoothSco(preferredDeviceAddress: String? = null): Boolean {
        return try {
            // COT: First check if SCO is available and if we have suitable BT devices
            if (!audioManager.isBluetoothScoAvailableOffCall) {
                Log.w(TAG, "Bluetooth SCO not available off-call")
                updateBluetoothAudioState(
                    scoState = BluetoothProfile.SCO_UNAVAILABLE,
                    errorMessage = "SCO not available off-call"
                )
                return false
            }
            
            // COT: If a specific device is preferred, try to route to it first
            preferredDeviceAddress?.let { address ->
                val targetDevice = getBluetoothAudioDevice(address)
                if (targetDevice != null && targetDevice.supportsVoice) {
                    Log.d(TAG, "Routing SCO to preferred device: ${targetDevice.name}")
                }
            }
            
            updateBluetoothAudioState(scoState = BluetoothProfile.SCO_CONNECTING)
            audioManager.startBluetoothSco()
            
            // COT: Monitor SCO connection state
            updateBluetoothAudioState(
                scoState = BluetoothProfile.SCO_AUDIO_CONNECTED,
                activeProfile = BluetoothProfile.SCO
            )
            
            Log.d(TAG, "Bluetooth SCO started successfully")
            updateRoutingState(activeRoute = AudioRoute.BLUETOOTH_SCO)
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Bluetooth SCO", e)
            updateBluetoothAudioState(
                scoState = BluetoothProfile.SCO_DISCONNECTED,
                errorMessage = e.message
            )
            false
        }
    }
    
    /**
     * Stop Bluetooth SCO with proper state cleanup
     */
    fun stopBluetoothSco() {
        try {
            updateBluetoothAudioState(scoState = BluetoothProfile.SCO_DISCONNECTING)
            audioManager.stopBluetoothSco()
            
            updateBluetoothAudioState(
                scoState = BluetoothProfile.SCO_DISCONNECTED,
                activeProfile = null
            )
            
            Log.d(TAG, "Bluetooth SCO stopped")
            
            // COT: After stopping SCO, determine the next best audio route
            determineOptimalAudioRoute()
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop Bluetooth SCO", e)
            updateBluetoothAudioState(errorMessage = e.message)
        }
    }
    
    /**
     * Check if wired headset is connected
     */
    fun isWiredHeadsetConnected(): Boolean {
        @Suppress("DEPRECATION")
        return audioManager.isWiredHeadsetOn
    }
    
    /**
     * Check if Bluetooth audio is connected with profile details
     */
    @Suppress("DEPRECATION") // No non-deprecated alternative below API 31
    fun isBluetoothConnected(): Boolean {
        return audioManager.isBluetoothScoOn || audioManager.isBluetoothA2dpOn
    }
    
    /**
     * Route audio to Bluetooth A2DP for media playback
     * 
     * COT: A2DP (Advanced Audio Distribution Profile) is used for high-quality stereo audio.
     * This is typically used for music playback to Bluetooth headphones or speakers.
     */
    fun routeToBluetoothA2dp(deviceAddress: String? = null): Boolean {
        return try {
            val a2dpDevice = deviceAddress?.let { address ->
                getBluetoothAudioDevice(address)
            } ?: getBestBluetoothA2dpDevice()
            
            if (a2dpDevice == null) {
                Log.w(TAG, "No suitable A2DP device found")
                return false
            }
            
            // COT: For newer Android versions, use the proper device routing API
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
                val targetAudioDevice = audioDevices.firstOrNull { device ->
                    device.address == a2dpDevice.address && 
                    device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
                }
                
                if (targetAudioDevice != null) {
                    audioManager.setCommunicationDevice(targetAudioDevice)
                    updateBluetoothAudioState(
                        a2dpState = BluetoothProfile.A2DP_AUDIO_CONNECTED,
                        activeProfile = BluetoothProfile.A2DP,
                        activeDevice = a2dpDevice
                    )
                    updateRoutingState(activeRoute = AudioRoute.BLUETOOTH_A2DP)
                    Log.d(TAG, "Routed audio to A2DP device: ${a2dpDevice.name}")
                    return true
                }
            }
            
            // COT: Fallback for older Android versions - rely on system to route to A2DP
            updateBluetoothAudioState(
                activeProfile = BluetoothProfile.A2DP,
                activeDevice = a2dpDevice
            )
            updateRoutingState(activeRoute = AudioRoute.BLUETOOTH_A2DP)
            
            Log.d(TAG, "A2DP routing initiated for device: ${a2dpDevice.name}")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to route to Bluetooth A2DP", e)
            updateBluetoothAudioState(errorMessage = e.message)
            false
        }
    }
    
    /**
     * Switch between Bluetooth profiles (SCO <-> A2DP)
     * 
     * COT: This allows dynamic switching between voice calls (SCO) and media (A2DP).
     * Useful for apps that need both voice input and media output capabilities.
     */
    @Suppress("DEPRECATION") // No non-deprecated alternative below API 31
    fun switchBluetoothProfile(
        targetProfile: BluetoothProfile.Profile,
        deviceAddress: String? = null
    ): Boolean {
        return when (targetProfile) {
            BluetoothProfile.Profile.A2DP -> {
                // COT: When switching to A2DP, stop SCO first to avoid conflicts
                if (audioManager.isBluetoothScoOn) {
                    stopBluetoothSco()
                }
                routeToBluetoothA2dp(deviceAddress)
            }
            BluetoothProfile.Profile.HEADSET -> {
                // COT: For voice calls, ensure A2DP doesn't interfere with SCO
                startBluetoothSco(deviceAddress)
            }
            else -> {
                Log.w(TAG, "Unsupported profile for switching: $targetProfile")
                false
            }
        }
    }
    
    /**
     * Get the best available Bluetooth device for voice (SCO)
     */
    private fun getBestBluetoothScoDevice(): BluetoothAudioDevice? {
        return bluetoothManager?.connectedDevices?.value
            ?.filter { it.profiles.contains(BluetoothManager.BluetoothProfileType.HEADSET) }
            ?.map { device ->
                BluetoothAudioDevice(
                    address = device.device.address,
                    name = device.device.name ?: "Unknown",
                    supportsVoice = true,
                    supportsMedia = device.profiles.contains(BluetoothManager.BluetoothProfileType.A2DP),
                    codec = device.audioCodec,
                    rssi = device.connectionQuality?.rssi ?: -60
                )
            }
            ?.maxByOrNull { it.rssi } // COT: Choose device with best signal strength
    }
    
    /**
     * Get the best available Bluetooth device for media (A2DP)
     */
    private fun getBestBluetoothA2dpDevice(): BluetoothAudioDevice? {
        return bluetoothManager?.connectedDevices?.value
            ?.filter { it.profiles.contains(BluetoothManager.BluetoothProfileType.A2DP) }
            ?.map { device ->
                BluetoothAudioDevice(
                    address = device.device.address,
                    name = device.device.name ?: "Unknown",
                    supportsVoice = device.profiles.contains(BluetoothManager.BluetoothProfileType.HEADSET),
                    supportsMedia = true,
                    codec = device.audioCodec,
                    rssi = device.connectionQuality?.rssi ?: -60
                )
            }
            ?.maxByOrNull { device ->
                // COT: Prioritize based on codec quality and signal strength
                val codecScore = when (device.codec?.type) {
                    BluetoothManager.CodecType.LDAC -> 100
                    BluetoothManager.CodecType.APTX_HD -> 80
                    BluetoothManager.CodecType.APTX -> 60
                    BluetoothManager.CodecType.AAC -> 40
                    BluetoothManager.CodecType.SBC -> 20
                    else -> 10
                }
                codecScore + (device.rssi + 100) // Normalize RSSI to positive score
            }
    }
    
    /**
     * Get specific Bluetooth audio device by address
     */
    private fun getBluetoothAudioDevice(address: String): BluetoothAudioDevice? {
        return bluetoothManager?.connectedDevices?.value
            ?.find { it.device.address == address }
            ?.let { device ->
                BluetoothAudioDevice(
                    address = device.device.address,
                    name = device.device.name ?: "Unknown",
                    supportsVoice = device.profiles.contains(BluetoothManager.BluetoothProfileType.HEADSET),
                    supportsMedia = device.profiles.contains(BluetoothManager.BluetoothProfileType.A2DP),
                    codec = device.audioCodec,
                    rssi = device.connectionQuality?.rssi ?: -60
                )
            }
    }
    
    /**
     * Update Bluetooth devices when BluetoothManager state changes
     */
    private fun updateBluetoothAudioDevices(devices: List<BluetoothManager.ConnectedBluetoothDevice>) {
        val audioDevices = devices
            .filter { device ->
                // COT: Only include devices that support audio profiles
                device.profiles.any { profile ->
                    profile == BluetoothManager.BluetoothProfileType.A2DP ||
                    profile == BluetoothManager.BluetoothProfileType.HEADSET
                }
            }
            .map { device ->
                BluetoothAudioDevice(
                    address = device.device.address,
                    name = device.device.name ?: "Unknown",
                    supportsVoice = device.profiles.contains(BluetoothManager.BluetoothProfileType.HEADSET),
                    supportsMedia = device.profiles.contains(BluetoothManager.BluetoothProfileType.A2DP),
                    codec = device.audioCodec,
                    rssi = device.connectionQuality?.rssi ?: -60
                )
            }
        
        updateBluetoothAudioState(availableDevices = audioDevices)
    }
    
    /**
     * Determine the optimal audio route based on available devices and current context
     * 
     * COT: This implements intelligent routing logic that considers:
     * 1. Device capabilities (voice vs media)
     * 2. Signal quality and codec support  
     * 3. User preferences and current app context
     * 4. Battery and performance considerations
     */
    @Suppress("DEPRECATION") // No non-deprecated alternative below API 31
    private fun determineOptimalAudioRoute() {
        // COT: Priority order for audio routing decisions
        when {
            // 1. Bluetooth SCO for active voice calls
            audioManager.isBluetoothScoOn -> {
                updateRoutingState(activeRoute = AudioRoute.BLUETOOTH_SCO)
            }
            
            // 2. Bluetooth A2DP for media if available and suitable
            audioManager.isBluetoothA2dpOn && getBestBluetoothA2dpDevice() != null -> {
                updateRoutingState(activeRoute = AudioRoute.BLUETOOTH_A2DP)
            }
            
            // 3. Wired headset takes priority over built-in speakers
            isWiredHeadsetConnected() -> {
                updateRoutingState(activeRoute = AudioRoute.WIRED_HEADSET)
            }
            
            // 4. Built-in speaker as fallback
            else -> {
                updateRoutingState(activeRoute = AudioRoute.SPEAKER)
            }
        }
    }
    
    /**
     * Update Bluetooth audio state
     */
    private fun updateBluetoothAudioState(
        scoState: Int? = null,
        a2dpState: Int? = null,
        activeProfile: Int? = null,
        activeDevice: BluetoothAudioDevice? = null,
        availableDevices: List<BluetoothAudioDevice>? = null,
        errorMessage: String? = null
    ) {
        _bluetoothAudioState.update { current ->
            current.copy(
                scoConnectionState = scoState ?: current.scoConnectionState,
                a2dpConnectionState = a2dpState ?: current.a2dpConnectionState,
                activeProfile = activeProfile ?: current.activeProfile,
                activeDevice = activeDevice ?: current.activeDevice,
                availableDevices = availableDevices ?: current.availableDevices,
                lastError = errorMessage
            )
        }
    }
    
    /**
     * Update audio routing state
     */
    private fun updateRoutingState(
        activeRoute: AudioRoute? = null,
        routingReason: String? = null
    ) {
        _routingState.update { current ->
            current.copy(
                activeRoute = activeRoute ?: current.activeRoute,
                routingReason = routingReason ?: current.routingReason,
                lastUpdated = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * Get current Bluetooth audio devices
     */
    fun getBluetoothAudioDevices(): List<BluetoothAudioDevice> {
        return _bluetoothAudioState.value.availableDevices
    }
    
    /**
     * Check if specific Bluetooth profile is active
     */
    @Suppress("DEPRECATION") // No non-deprecated alternative below API 31
    fun isBluetoothProfileActive(profile: Int): Boolean {
        return when (profile) {
            BluetoothProfile.A2DP -> audioManager.isBluetoothA2dpOn
            BluetoothProfile.HEADSET -> audioManager.isBluetoothScoOn
            else -> false
        }
    }
    
    private fun scanDevices() {
        _connectedDevices.value = getDevices()
    }
    
    private fun registerCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            audioManager.registerAudioDeviceCallback(deviceCallback, null)
        }
    }
    
    private fun getDeviceTypeString(type: Int): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            when (type) {
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "Earpiece"
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "Speaker"
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired Headset"
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "Headphones"
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth SCO"
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "Bluetooth A2DP"
                AudioDeviceInfo.TYPE_HDMI -> "HDMI"
                AudioDeviceInfo.TYPE_USB_DEVICE -> "USB Device"
                AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB Accessory"
                AudioDeviceInfo.TYPE_USB_HEADSET -> "USB Headset"
                else -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && type == 23) {
                        "Hearing Aid"
                    } else {
                        "Unknown"
                    }
                }
            }
        } else {
            "Unknown"
        }
    }
    
    private fun isBluetoothDevice(type: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO || 
            type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP
        } else {
            false
        }
    }
    
    private fun isUsbDevice(type: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            type == AudioDeviceInfo.TYPE_USB_DEVICE || 
            type == AudioDeviceInfo.TYPE_USB_ACCESSORY || 
            type == AudioDeviceInfo.TYPE_USB_HEADSET
        } else {
            false
        }
    }
    
    private fun isBuiltInDevice(type: Int): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE || 
            type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER ||
            type == AudioDeviceInfo.TYPE_BUILTIN_MIC
        } else {
            false
        }
    }
    
    fun release() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && deviceCallback != null) {
            audioManager.unregisterAudioDeviceCallback(deviceCallback)
        }
        scope.cancel()
    }
    
    // ========== DATA MODELS ==========
    
    /**
     * Bluetooth audio device representation
     */
    data class BluetoothAudioDevice(
        val address: String,
        val name: String,
        val supportsVoice: Boolean,
        val supportsMedia: Boolean,
        val codec: BluetoothManager.AudioCodec?,
        val rssi: Int
    )
    
    /**
     * Bluetooth audio state tracking
     */
    data class BluetoothAudioState(
        val scoConnectionState: Int = BluetoothProfile.STATE_DISCONNECTED,
        val a2dpConnectionState: Int = BluetoothProfile.STATE_DISCONNECTED,
        val activeProfile: Int? = null,
        val activeDevice: BluetoothAudioDevice? = null,
        val availableDevices: List<BluetoothAudioDevice> = emptyList(),
        val lastError: String? = null
    )
    
    /**
     * Audio routing state tracking
     */
    data class AudioRoutingState(
        val activeRoute: AudioRoute = AudioRoute.SPEAKER,
        val routingReason: String = "Default",
        val lastUpdated: Long = System.currentTimeMillis()
    )
    
    /**
     * Audio route types
     */
    enum class AudioRoute {
        SPEAKER,
        EARPIECE,
        WIRED_HEADSET,
        BLUETOOTH_SCO,
        BLUETOOTH_A2DP,
        USB_HEADSET,
        UNKNOWN
    }
    
    /**
     * Bluetooth profile constants for easier reference
     */
    object BluetoothProfile {
        const val A2DP = android.bluetooth.BluetoothProfile.A2DP
        const val HEADSET = android.bluetooth.BluetoothProfile.HEADSET
        const val SCO = 99 // Custom constant for SCO profile reference
        
        // Connection states
        const val STATE_DISCONNECTED = android.bluetooth.BluetoothProfile.STATE_DISCONNECTED
        const val STATE_CONNECTING = android.bluetooth.BluetoothProfile.STATE_CONNECTING
        const val STATE_CONNECTED = android.bluetooth.BluetoothProfile.STATE_CONNECTED
        const val STATE_DISCONNECTING = android.bluetooth.BluetoothProfile.STATE_DISCONNECTING
        
        // SCO-specific states
        const val SCO_AUDIO_CONNECTED = 12
        const val SCO_AUDIO_CONNECTING = 11
        const val SCO_AUDIO_DISCONNECTED = 10
        const val SCO_DISCONNECTING = 0
        const val SCO_CONNECTING = 2
        const val SCO_CONNECTED = 1
        const val SCO_DISCONNECTED = 0
        const val SCO_UNAVAILABLE = -1
        
        // A2DP-specific states  
        const val A2DP_AUDIO_CONNECTED = 22
        const val A2DP_AUDIO_CONNECTING = 21
        const val A2DP_AUDIO_DISCONNECTED = 20
        
        enum class Profile {
            A2DP, HEADSET, HID, PAN, PBAP, MAP, SAP, GATT, HEALTH
        }
    }
}
