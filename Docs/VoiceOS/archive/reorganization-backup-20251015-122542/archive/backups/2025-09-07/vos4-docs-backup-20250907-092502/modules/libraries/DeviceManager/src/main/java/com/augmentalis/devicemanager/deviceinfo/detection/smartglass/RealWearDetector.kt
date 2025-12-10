/**
 * RealWearDetector.kt
 * Path: /deviceinfo/detection/smartglass/RealWearDetector.kt
 * 
 * Purpose: RealWear HMT device detection
 * Detects RealWear models and their voice-first capabilities
 */

package com.augmentalis.devicemanager.deviceinfo.detection.smartglass

import android.content.Context
import android.os.Build
import android.util.Log

/**
 * RealWear Device Detector
 * Detects RealWear HMT devices and their specific features
 */
object RealWearDetector {
    
    private const val TAG = "RealWearDetector"
    private const val MANUFACTURER = "RealWear"
    
    /**
     * Check if device is RealWear HMT
     */
    fun isRealWear(): Boolean {
        return Build.MANUFACTURER?.startsWith(MANUFACTURER) == true &&
               (Build.DEVICE?.startsWith("HMT") == true || 
                Build.DEVICE?.startsWith("Navigator") == true)
    }
    
    /**
     * Get RealWear model type
     */
    fun getModel(): RealWearModel? {
        if (!isRealWear()) return null
        
        val device = Build.DEVICE ?: return null
        
        return when {
            device.contains("Navigator-500") -> RealWearModel.NAVIGATOR_500
            device.contains("Navigator-520") -> RealWearModel.NAVIGATOR_520
            device.contains("HMT-1Z1") -> RealWearModel.HMT_1Z1
            device.contains("HMT-1") -> RealWearModel.HMT_1
            else -> RealWearModel.UNKNOWN_REALWEAR
        }
    }
    
    /**
     * Get RealWear capabilities
     */
    fun getCapabilities(context: Context): RealWearCapabilities? {
        if (!isRealWear()) return null
        
        val model = getModel() ?: return null
        
        return RealWearCapabilities(
            model = model,
            hasVoiceControl = true, // Core feature of all RealWear devices
            hasNoiseReduction = true, // 4-mic array with noise cancellation
            hasModularDesign = true,
            hasFlipDisplay = true,
            hasCamera = true,
            hasFlashlight = true,
            isRuggedized = true,
            isIntrinsicallySafe = model == RealWearModel.HMT_1Z1,
            batteryLife = getBatteryLife(model),
            displayResolution = getDisplayResolution(model),
            processorSpeed = getProcessorSpeed(model),
            ipRating = getIPRating(model),
            dropRating = 2, // 2 meters for all models
            operatingTemp = getOperatingTemp(model),
            cursorDelayTime = 800_000_000L, // 800ms for stability
            voiceCommands = getVoiceCommands()
        )
    }
    
    /**
     * RealWear model enumeration
     */
    enum class RealWearModel {
        HMT_1,          // Original HMT-1
        HMT_1Z1,        // Intrinsically safe version
        NAVIGATOR_500,   // Latest generation
        NAVIGATOR_520,   // Enhanced Navigator
        UNKNOWN_REALWEAR
    }
    
    /**
     * RealWear device capabilities
     */
    data class RealWearCapabilities(
        val model: RealWearModel,
        val hasVoiceControl: Boolean,
        val hasNoiseReduction: Boolean,
        val hasModularDesign: Boolean,
        val hasFlipDisplay: Boolean,
        val hasCamera: Boolean,
        val hasFlashlight: Boolean,
        val isRuggedized: Boolean,
        val isIntrinsicallySafe: Boolean,
        val batteryLife: Int, // in hours
        val displayResolution: String,
        val processorSpeed: String,
        val ipRating: String,
        val dropRating: Int, // in meters
        val operatingTemp: String,
        val cursorDelayTime: Long,
        val voiceCommands: List<String>
    )
    
    // Private helper methods
    
    private fun getBatteryLife(model: RealWearModel): Int {
        return when (model) {
            RealWearModel.NAVIGATOR_500, RealWearModel.NAVIGATOR_520 -> 10
            RealWearModel.HMT_1, RealWearModel.HMT_1Z1 -> 8
            else -> 8
        }
    }
    
    private fun getDisplayResolution(model: RealWearModel): String {
        return when (model) {
            RealWearModel.NAVIGATOR_500, RealWearModel.NAVIGATOR_520 -> "1280x720"
            RealWearModel.HMT_1, RealWearModel.HMT_1Z1 -> "854x480"
            else -> "854x480"
        }
    }
    
    private fun getProcessorSpeed(model: RealWearModel): String {
        return when (model) {
            RealWearModel.NAVIGATOR_500, RealWearModel.NAVIGATOR_520 -> "Snapdragon 665"
            RealWearModel.HMT_1, RealWearModel.HMT_1Z1 -> "Snapdragon 625"
            else -> "Octa-core"
        }
    }
    
    private fun getIPRating(model: RealWearModel): String {
        return when (model) {
            RealWearModel.HMT_1Z1 -> "IP66" // Intrinsically safe model
            else -> "IP66"
        }
    }
    
    private fun getOperatingTemp(model: RealWearModel): String {
        return when (model) {
            RealWearModel.HMT_1Z1 -> "-20°C to 50°C" // Extended for hazardous environments
            else -> "-20°C to 50°C"
        }
    }
    
    private fun getVoiceCommands(): List<String> {
        return listOf(
            "SELECT ITEM *",
            "NAVIGATE BACK",
            "NAVIGATE HOME",
            "SHOW HELP",
            "MY CONTROLS",
            "ZOOM IN",
            "ZOOM OUT",
            "TAKE PHOTO",
            "START RECORDING",
            "STOP RECORDING",
            "FLASHLIGHT ON",
            "FLASHLIGHT OFF",
            "PAGE UP",
            "PAGE DOWN",
            "SCROLL UP",
            "SCROLL DOWN"
        )
    }
}