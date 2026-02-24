/**
 * SmartGlassDetection.kt
 * Path: /libraries/DeviceManager/src/main/java/com/augmentalis/devicemanager/detection/SmartGlassDetection.kt
 * 
 * Created: 2025-09-05
 * Version: 1.0.0
 * 
 * Purpose: Device-specific detection for smart glasses and AR headsets
 * Ported from LegacyAvenue SystemProperties with enhancements
 * Module: DeviceManager
 */

package com.augmentalis.devicemanager.deviceinfo.detection

import android.content.Context
import android.os.Build
import android.util.Log

/**
 * Smart Glass Detection - Comprehensive device identification
 * Ported from Legacy SystemProperties with additional manufacturers
 */
class SmartGlassDetection(private val context: Context) {
    
    companion object {
        private const val TAG = "SmartGlassDetection"
        
        // Manufacturer constants
        const val MANUFACTURER_REALWEAR = "RealWear"
        const val MANUFACTURER_VUZIX = "vuzix"
        const val MANUFACTURER_ROKID = "rokid"
        const val MANUFACTURER_EPSON = "SEIKO EPSON"
        const val MANUFACTURER_GOOGLE = "Google"
        const val MANUFACTURER_MICROSOFT = "Microsoft"
        const val MANUFACTURER_MAGIC_LEAP = "Magic Leap"
        const val MANUFACTURER_XREAL = "XREAL"
        const val MANUFACTURER_NREAL = "Nreal"
        const val MANUFACTURER_TCL = "TCL"
        const val MANUFACTURER_LENOVO = "Lenovo"
        const val MANUFACTURER_DIGILENS = "DIGILENS"
        const val MANUFACTURER_XCRAFT = "xCraft"
        const val MANUFACTURER_PICO = "Pico"
        const val MANUFACTURER_VARJO = "Varjo"
        const val MANUFACTURER_HTC = "HTC"
    }
    
    /**
     * Determines if the device is a Rokid Glass device
     * Ported from Legacy SystemProperties.isRokid()
     */
    fun isRokid(): Boolean {
        // Check if Rokid SDK class exists
        return try {
            Class.forName("com.rokid.axr.phone.glassdevice.RKGlassDevice")
            Build.MANUFACTURER?.equals(MANUFACTURER_ROKID, ignoreCase = true) == true ||
            Build.MODEL?.contains("rokid", ignoreCase = true) == true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
    
    /**
     * Determines if the device is an xCraft device
     * Ported from Legacy SystemProperties.isItXCraft()
     */
    fun isXCraft(): Boolean {
        return Build.DEVICE?.contains("crown") == true &&
               Build.MODEL?.equals("RG-crown", ignoreCase = true) == true &&
               Build.MANUFACTURER?.lowercase()?.startsWith("droidlogic") == true
    }
    
    /**
     * Determines if the device is a RealWear HMT or Navigator
     * Ported from Legacy SystemProperties.isRealWearHMT()
     */
    fun isRealWearHMT(): Boolean {
        return Build.MANUFACTURER?.startsWith(MANUFACTURER_REALWEAR) == true &&
               (Build.DEVICE?.startsWith("HMT") == true || 
                Build.DEVICE?.startsWith("Navigator") == true)
    }
    
    /**
     * Determines if the device is a Vuzix device
     * Ported from Legacy SystemProperties.isVuzix()
     */
    fun isVuzix(): Boolean {
        return Build.MANUFACTURER?.equals(MANUFACTURER_VUZIX, ignoreCase = true) == true &&
               Build.DEVICE?.matches(Regex("[a-zA-Z]+\\d+$")) == true
    }
    
    /**
     * Determines if the device is a Digilens device
     * Ported from Legacy SystemProperties.isDigilens()
     */
    fun isDigilens(): Boolean {
        return Build.MANUFACTURER?.startsWith(MANUFACTURER_DIGILENS, ignoreCase = true) == true
    }
    
    /**
     * Determines if the device is an EPSON Moverio device
     * New addition based on Legacy CursorOrientationProvider imports
     */
    fun isEpsonMoverio(): Boolean {
        return Build.MANUFACTURER?.equals(MANUFACTURER_EPSON) == true &&
               (Build.MODEL?.contains("moverio", ignoreCase = true) == true ||
                Build.MODEL?.matches(Regex("BT-[0-9]+")) == true)
    }
    
    /**
     * Determines if the device is Google Glass
     */
    fun isGoogleGlass(): Boolean {
        return Build.MANUFACTURER?.equals(MANUFACTURER_GOOGLE) == true &&
               Build.MODEL?.contains("glass", ignoreCase = true) == true
    }
    
    /**
     * Determines if the device is XREAL/Nreal glasses
     */
    fun isXRealGlasses(): Boolean {
        val manufacturer = Build.MANUFACTURER?.lowercase() ?: ""
        return manufacturer == "xreal" || manufacturer == "nreal" ||
               Build.MODEL?.contains("air", ignoreCase = true) == true
    }
    
    /**
     * Get the current smart glass type
     */
    fun getSmartGlassType(): SmartGlassType {
        return when {
            isRealWearHMT() -> SmartGlassType.REALWEAR
            isVuzix() -> SmartGlassType.VUZIX
            isRokid() -> SmartGlassType.ROKID
            isEpsonMoverio() -> SmartGlassType.EPSON
            isGoogleGlass() -> SmartGlassType.GOOGLE_GLASS
            isXRealGlasses() -> SmartGlassType.XREAL
            isDigilens() -> SmartGlassType.DIGILENS
            isXCraft() -> SmartGlassType.XCRAFT
            else -> SmartGlassType.UNKNOWN
        }
    }
    
    /**
     * Get device-specific cursor delay time
     * Used for handling voice command cursor stability on certain devices
     */
    fun getCursorDelayTime(): Long {
        return when (getSmartGlassType()) {
            SmartGlassType.REALWEAR -> 800_000_000L // 800ms for RealWear HMT
            SmartGlassType.VUZIX -> 500_000_000L    // 500ms for Vuzix
            SmartGlassType.ROKID -> 600_000_000L    // 600ms for Rokid
            SmartGlassType.EPSON -> 400_000_000L    // 400ms for Epson
            else -> 0L // No delay for regular devices
        }
    }
    
    /**
     * Get device-specific IMU sensor type
     * Some devices need different sensor types for orientation
     */
    fun getPreferredSensorType(): Int {
        return when {
            isXCraft() -> android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR
            isRokid() -> android.hardware.Sensor.TYPE_ROTATION_VECTOR
            isEpsonMoverio() -> android.hardware.Sensor.TYPE_ROTATION_VECTOR
            else -> android.hardware.Sensor.TYPE_ROTATION_VECTOR
        }
    }
    
    /**
     * Check if device needs special IMU handling
     */
    fun needsCustomIMUHandling(): Boolean {
        return isRokid() || isEpsonMoverio() || isXCraft()
    }
    
    /**
     * Get device-specific cursor scaling factors
     */
    fun getCursorScaleFactors(): Triple<Float, Float, Float> {
        return when (getSmartGlassType()) {
            SmartGlassType.REALWEAR -> Triple(2.0f, 3.0f, 2.0f)
            SmartGlassType.VUZIX -> Triple(1.8f, 2.5f, 1.8f)
            SmartGlassType.ROKID -> Triple(2.2f, 3.2f, 2.2f)
            SmartGlassType.EPSON -> Triple(1.5f, 2.0f, 1.5f)
            else -> Triple(2.0f, 3.0f, 2.0f) // Default from Legacy
        }
    }
    
    /**
     * Check if device supports headset attachment
     * Used for EPSON Moverio and similar modular devices
     */
    fun supportsHeadsetAttachment(): Boolean {
        return isEpsonMoverio() || getSmartGlassType() == SmartGlassType.TCL
    }
    
    /**
     * Log device detection information
     */
    fun logDeviceInfo() {
        Log.i(TAG, "Smart Glass Detection:")
        Log.i(TAG, "  Manufacturer: ${Build.MANUFACTURER}")
        Log.i(TAG, "  Model: ${Build.MODEL}")
        Log.i(TAG, "  Device: ${Build.DEVICE}")
        Log.i(TAG, "  Type: ${getSmartGlassType()}")
        Log.i(TAG, "  Cursor Delay: ${getCursorDelayTime()} ns")
        Log.i(TAG, "  Sensor Type: ${getPreferredSensorType()}")
        Log.i(TAG, "  Custom IMU: ${needsCustomIMUHandling()}")
    }
}

/**
 * Smart Glass device types
 */
enum class SmartGlassType {
    REALWEAR,
    VUZIX,
    ROKID,
    EPSON,
    GOOGLE_GLASS,
    XREAL,
    DIGILENS,
    XCRAFT,
    TCL,
    MAGIC_LEAP,
    MICROSOFT_HOLOLENS,
    PICO,
    VARJO,
    HTC,
    UNKNOWN
}