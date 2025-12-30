// Author: Manoj Jhawar
// Purpose: Cellular network management for VOS4

package com.augmentalis.devicemanager.network

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cellular Manager Component
 * Handles cellular network state, signal strength, and carrier information
 * 
 * Architecture:
 * - Receives capabilities from DeviceDetector (centralized detection)
 * - Does NOT perform hardware detection directly
 * - Enables conditional loading based on detected cellular capabilities
 * - Provides runtime state management for cellular features
 * 
 * Benefits:
 * - Reduces redundant system calls
 * - Ensures consistent detection logic
 * - Supports conditional instantiation
 * - Improves testability
 * 
 * COT: Following the same architectural pattern as NfcManager to maintain consistency
 * across network managers and reduce code duplication in capability detection.
 */
class CellularManager(
    private val context: Context,
    private val capabilities: com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.DeviceCapabilities
) {
    
    /**
     * Backward compatibility constructor
     * @deprecated Use constructor with DeviceCapabilities parameter
     * 
     * COT: Maintaining backward compatibility while transitioning to centralized detection.
     * This ensures existing code continues to work during the migration phase.
     */
    @Deprecated("Use constructor with DeviceCapabilities parameter for better architecture")
    constructor(context: Context) : this(
        context, 
        com.augmentalis.devicemanager.deviceinfo.detection.DeviceDetector.getCapabilities(context)
    )
    
    companion object {
        private const val TAG = "CellularManager"
    }
    
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
    
    private val _cellularState = MutableStateFlow(CellularInfo())
    val cellularState: StateFlow<CellularInfo> = _cellularState.asStateFlow()
    
    /**
     * Cellular network information
     */
    data class CellularInfo(
        val operator: String? = null,
        val operatorCode: String? = null,
        val simOperator: String? = null,
        val networkType: String = "UNKNOWN",
        val signalStrength: Int = 0,
        val isRoaming: Boolean = false,
        val dataState: Int = TelephonyManager.DATA_DISCONNECTED,
        val simState: Int = TelephonyManager.SIM_STATE_UNKNOWN,
        val phoneType: Int = TelephonyManager.PHONE_TYPE_NONE,
        val is5GAvailable: Boolean = false
    )
    
    init {
        updateCellularInfo()
    }
    
    /**
     * Get current cellular network information
     * 
     * COT: Added capability check before attempting to access telephony services.
     * This prevents unnecessary system calls on devices without cellular hardware.
     */
    @RequiresPermission(android.Manifest.permission.READ_PHONE_STATE)
    fun getCellularInfo(): CellularInfo? {
        if (!capabilities.network.hasCellular) {
            Log.d(TAG, "Cellular capability not available on this device")
            return null
        }
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                CellularInfo(
                    operator = telephonyManager?.networkOperatorName,
                    operatorCode = telephonyManager?.networkOperator,
                    simOperator = telephonyManager?.simOperatorName,
                    networkType = getNetworkTypeName(telephonyManager?.dataNetworkType ?: 0),
                    signalStrength = getSignalStrength(),
                    isRoaming = telephonyManager?.isNetworkRoaming ?: false,
                    dataState = telephonyManager?.dataState ?: TelephonyManager.DATA_DISCONNECTED,
                    simState = telephonyManager?.simState ?: TelephonyManager.SIM_STATE_UNKNOWN,
                    phoneType = telephonyManager?.phoneType ?: TelephonyManager.PHONE_TYPE_NONE,
                    is5GAvailable = is5GAvailable()
                )
            } else {
                null
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to get cellular info - missing permission", e)
            null
        }
    }
    
    /**
     * Update cellular state
     */
    private fun updateCellularInfo() {
        try {
            getCellularInfo()?.let { info ->
                _cellularState.value = info
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating cellular info", e)
        }
    }
    
    /**
     * Check if 5G is available
     * 
     * COT: Using centralized 5G detection capability first, then runtime telephony check.
     * This combines hardware capability detection with current network state.
     */
    private fun is5GAvailable(): Boolean {
        if (!capabilities.network.has5G) {
            return false
        }
        
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            telephonyManager?.dataNetworkType == TelephonyManager.NETWORK_TYPE_NR
        } else {
            false
        }
    }
    
    /**
     * Get human-readable network type name
     */
    private fun getNetworkTypeName(type: Int): String {
        return when (type) {
            TelephonyManager.NETWORK_TYPE_GPRS -> "GPRS"
            TelephonyManager.NETWORK_TYPE_EDGE -> "EDGE"
            TelephonyManager.NETWORK_TYPE_UMTS -> "UMTS"
            TelephonyManager.NETWORK_TYPE_CDMA -> "CDMA"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> "EVDO_0"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> "EVDO_A"
            TelephonyManager.NETWORK_TYPE_1xRTT -> "1xRTT"
            TelephonyManager.NETWORK_TYPE_HSDPA -> "HSDPA"
            TelephonyManager.NETWORK_TYPE_HSUPA -> "HSUPA"
            TelephonyManager.NETWORK_TYPE_HSPA -> "HSPA"
            TelephonyManager.NETWORK_TYPE_IDEN -> "IDEN"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> "EVDO_B"
            TelephonyManager.NETWORK_TYPE_LTE -> "LTE"
            TelephonyManager.NETWORK_TYPE_EHRPD -> "EHRPD"
            TelephonyManager.NETWORK_TYPE_HSPAP -> "HSPAP+"
            TelephonyManager.NETWORK_TYPE_GSM -> "GSM"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> "TD_SCDMA"
            TelephonyManager.NETWORK_TYPE_IWLAN -> "IWLAN"
            19 -> "LTE_CA" // NETWORK_TYPE_LTE_CA constant value
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> "UNKNOWN"
            else -> "UNKNOWN ($type)"
        }
    }
    
    /**
     * Get signal strength level
     */
    private fun getSignalStrength(): Int {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                telephonyManager?.signalStrength?.level ?: 0
            } else {
                0
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot get signal strength - missing permission", e)
            0
        }
    }
    
    /**
     * Check if cellular data is enabled
     * 
     * COT: Adding capability check to prevent unnecessary calls on non-cellular devices.
     * This improves performance and reduces log noise.
     */
    fun isDataEnabled(): Boolean {
        if (!capabilities.network.hasCellular) {
            return false
        }
        
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                telephonyManager?.isDataEnabled ?: false
            } else {
                false
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot check data state - missing permission", e)
            false
        }
    }
    
    /**
     * Get carrier name
     * 
     * COT: Adding capability check to ensure we only attempt carrier lookup on cellular devices.
     */
    fun getCarrierName(): String? = if (capabilities.network.hasCellular) {
        telephonyManager?.networkOperatorName
    } else {
        null
    }
    
    /**
     * Check if device has cellular capability
     * 
     * COT: Now using centralized capability detection instead of direct telephonyManager check.
     * This ensures consistent detection logic across all components and reduces system calls.
     */
    fun hasCellularCapability(): Boolean = capabilities.network.hasCellular
    
    /**
     * Refresh cellular state
     */
    fun refresh() {
        updateCellularInfo()
        Log.d(TAG, "Cellular state refreshed")
    }
    
    /**
     * Cleanup resources
     */
    fun cleanup() {
        // No specific cleanup needed
        Log.d(TAG, "Cellular manager cleanup completed")
    }
}