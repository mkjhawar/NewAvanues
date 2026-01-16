// Author: Manoj Jhawar
// Purpose: NFC (Near Field Communication) management for VOS4

package com.augmentalis.devicemanager.network

import android.content.Context
import android.nfc.NfcAdapter
import android.nfc.NfcManager
import android.os.Build
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * NFC Manager Component
 * Handles NFC state management and capabilities
 * 
 * Architecture:
 * - Receives capabilities from DeviceDetector (centralized detection)
 * - Does NOT perform hardware detection directly
 * - Enables conditional loading based on detected NFC capabilities
 * - Provides runtime state management for NFC features
 * 
 * Benefits:
 * - Reduces redundant system calls
 * - Ensures consistent detection logic
 * - Supports conditional instantiation
 * - Improves testability
 */
class NfcManager(
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
        private const val TAG = "NfcManager"
    }
    
    // System services
    private val nfcManager = context.getSystemService(Context.NFC_SERVICE) as? NfcManager
    private val nfcAdapter = nfcManager?.defaultAdapter
    
    // State management
    private val _nfcState = MutableStateFlow(NfcState())
    val nfcState: StateFlow<NfcState> = _nfcState.asStateFlow()
    
    /**
     * NFC state data class
     */
    data class NfcState(
        val isAvailable: Boolean = false,
        val isEnabled: Boolean = false,
        val hasBeamCapability: Boolean = false,
        val hasSecureNfc: Boolean = false,
        val hasCardEmulation: Boolean = false,
        val hasReaderMode: Boolean = false
    )
    
    init {
        updateNfcState()
    }
    
    /**
     * Update NFC state using detected capabilities
     */
    private fun updateNfcState() {
        _nfcState.value = NfcState(
            isAvailable = capabilities.hardware.hasNfc,
            isEnabled = isNfcEnabled(),
            hasBeamCapability = hasNfcBeam(),
            hasSecureNfc = hasSecureNfc(),
            hasCardEmulation = hasCardEmulation(),
            hasReaderMode = hasReaderMode()
        )
    }
    
    /**
     * Check if NFC hardware is available
     */
    fun isNfcAvailable(): Boolean = capabilities.hardware.hasNfc
    
    /**
     * Check if NFC is enabled
     */
    fun isNfcEnabled(): Boolean = nfcAdapter?.isEnabled ?: false
    
    /**
     * Check NFC beam capability (deprecated in Android 10)
     */
    fun hasNfcBeam(): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            capabilities.hardware.hasNfc
        } else {
            false
        }
    }
    
    /**
     * Check if secure NFC is supported (Android 10+)
     */
    fun hasSecureNfc(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            nfcAdapter?.isSecureNfcSupported ?: false
        } else {
            false
        }
    }
    
    /**
     * Check if card emulation is available
     * 
     * Note: Card emulation capability not yet centralized in DeviceDetector
     * TODO: Move this detection to DeviceDetector in future update
     */
    fun hasCardEmulation(): Boolean {
        if (!capabilities.hardware.hasNfc) return false
        
        return context.packageManager.hasSystemFeature("android.hardware.nfc.hce") ||
               context.packageManager.hasSystemFeature("android.hardware.nfc.hcef")
    }
    
    /**
     * Check if reader mode is available
     */
    fun hasReaderMode(): Boolean {
        return capabilities.hardware.hasNfc && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT
    }
    
    /**
     * Get NFC adapter for advanced operations
     */
    fun getNfcAdapter(): NfcAdapter? = if (capabilities.hardware.hasNfc) nfcAdapter else null
    
    /**
     * Refresh NFC state
     */
    fun refresh() {
        updateNfcState()
        Log.d(TAG, "NFC state refreshed: ${_nfcState.value}")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        // No specific cleanup needed for NFC
        Log.d(TAG, "NFC manager cleanup completed")
    }
}