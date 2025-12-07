/**
 * VuzixDetector.kt
 * Path: /deviceinfo/detection/smartglass/VuzixDetector.kt
 * 
 * Purpose: Vuzix smart glasses detection
 * Detects Vuzix models and their specific capabilities
 */

package com.augmentalis.devicemanager.deviceinfo.detection.smartglass

import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Vuzix Smart Glasses Detector
 * Detects Vuzix devices and their specific features
 */
object VuzixDetector {
    
    private const val TAG = "VuzixDetector"
    private const val MANUFACTURER = "vuzix"
    
    /**
     * Check if device is Vuzix
     */
    fun isVuzix(): Boolean {
        return Build.MANUFACTURER.equals(MANUFACTURER, ignoreCase = true) &&
               Build.DEVICE?.matches(Regex("[a-zA-Z]+\\d+$")) == true
    }
    
    /**
     * Get Vuzix model type
     */
    fun getModel(): VuzixModel? {
        if (!isVuzix()) return null
        
        val device = Build.DEVICE?.lowercase() ?: return null
        val model = Build.MODEL?.lowercase() ?: return null
        
        return when {
            model.contains("blade") -> VuzixModel.BLADE
            model.contains("m400") -> VuzixModel.M400
            model.contains("m4000") -> VuzixModel.M4000
            model.contains("shield") -> VuzixModel.SHIELD
            model.contains("z100") -> VuzixModel.Z100
            device.contains("blade2") -> VuzixModel.BLADE_2
            else -> VuzixModel.UNKNOWN_VUZIX
        }
    }
    
    /**
     * Get Vuzix capabilities
     */
    fun getCapabilities(context: Context): VuzixCapabilities? {
        if (!isVuzix()) return null
        
        val model = getModel() ?: return null
        
        return VuzixCapabilities(
            model = model,
            hasWaveGuideDisplay = model in listOf(VuzixModel.BLADE, VuzixModel.BLADE_2, VuzixModel.SHIELD),
            hasStereoDisplay = model == VuzixModel.Z100,
            hasCamera = true, // All Vuzix glasses have cameras
            hasVoiceControl = true,
            hasTouchpad = model != VuzixModel.Z100,
            hasGPS = model in listOf(VuzixModel.M400, VuzixModel.M4000),
            batteryLife = getBatteryLife(model),
            displayResolution = getDisplayResolution(model),
            fieldOfView = getFieldOfView(model),
            weight = getWeight(model),
            isRuggedized = model in listOf(VuzixModel.M400, VuzixModel.M4000),
            speechSDK = isPackageInstalled(context, "com.vuzix.speech.sdk"),
            barcodeSDK = isPackageInstalled(context, "com.vuzix.barcode")
        )
    }
    
    /**
     * Vuzix model enumeration
     */
    enum class VuzixModel {
        BLADE,      // Consumer AR glasses
        BLADE_2,    // Blade upgraded version
        M400,       // Enterprise monocular
        M4000,      // Enterprise binocular
        SHIELD,     // Safety glasses with display
        Z100,       // Stereo AR glasses
        UNKNOWN_VUZIX
    }
    
    /**
     * Vuzix device capabilities
     */
    data class VuzixCapabilities(
        val model: VuzixModel,
        val hasWaveGuideDisplay: Boolean,
        val hasStereoDisplay: Boolean,
        val hasCamera: Boolean,
        val hasVoiceControl: Boolean,
        val hasTouchpad: Boolean,
        val hasGPS: Boolean,
        val batteryLife: Int, // in hours
        val displayResolution: String,
        val fieldOfView: Int, // in degrees
        val weight: Int, // in grams
        val isRuggedized: Boolean,
        val speechSDK: Boolean,
        val barcodeSDK: Boolean
    )
    
    // Private helper methods
    
    private fun getBatteryLife(model: VuzixModel): Int {
        return when (model) {
            VuzixModel.BLADE, VuzixModel.BLADE_2 -> 2
            VuzixModel.M400 -> 12
            VuzixModel.M4000 -> 10
            VuzixModel.SHIELD -> 8
            VuzixModel.Z100 -> 6
            else -> 4
        }
    }
    
    private fun getDisplayResolution(model: VuzixModel): String {
        return when (model) {
            VuzixModel.BLADE, VuzixModel.BLADE_2 -> "480x480"
            VuzixModel.M400 -> "640x360"
            VuzixModel.M4000 -> "854x480"
            VuzixModel.SHIELD -> "640x360"
            VuzixModel.Z100 -> "1920x1080" // per eye
            else -> "640x480"
        }
    }
    
    private fun getFieldOfView(model: VuzixModel): Int {
        return when (model) {
            VuzixModel.BLADE, VuzixModel.BLADE_2 -> 28
            VuzixModel.M400 -> 16
            VuzixModel.M4000 -> 28
            VuzixModel.SHIELD -> 16
            VuzixModel.Z100 -> 30
            else -> 20
        }
    }
    
    private fun getWeight(model: VuzixModel): Int {
        return when (model) {
            VuzixModel.BLADE, VuzixModel.BLADE_2 -> 90
            VuzixModel.M400 -> 190
            VuzixModel.M4000 -> 275
            VuzixModel.SHIELD -> 125
            VuzixModel.Z100 -> 180
            else -> 150
        }
    }
    
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }
}