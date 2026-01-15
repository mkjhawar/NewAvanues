/**
 * SamsungDetector.kt
 * Path: /deviceinfo/detection/manufacturers/SamsungDetector.kt
 * 
 * Purpose: Samsung-specific device detection
 * Detects Samsung features like S-Pen, DeX, Knox, Edge Panel, etc.
 */

package com.augmentalis.devicemanager.deviceinfo.detection.manufacturers

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

/**
 * Samsung Device Detector
 * Detects Samsung-specific features and capabilities
 */
object SamsungDetector {
    
    private const val TAG = "SamsungDetector"
    
    /**
     * Check if device is Samsung
     */
    fun isSamsung(): Boolean = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
    
    /**
     * Detect Samsung device series
     */
    fun detectSeries(): String? {
        if (!isSamsung()) return null
        
        val model = Build.MODEL.lowercase()
        return when {
            model.contains("galaxy s") -> "Galaxy S"
            model.contains("galaxy note") -> "Galaxy Note"
            model.contains("galaxy z fold") -> "Galaxy Z Fold"
            model.contains("galaxy z flip") -> "Galaxy Z Flip"
            model.contains("galaxy a") -> "Galaxy A"
            model.contains("galaxy m") -> "Galaxy M"
            model.contains("galaxy f") -> "Galaxy F"
            model.contains("galaxy tab") -> "Galaxy Tab"
            model.contains("galaxy watch") -> "Galaxy Watch"
            model.contains("galaxy buds") -> "Galaxy Buds"
            else -> null
        }
    }
    
    /**
     * Get all Samsung features
     */
    fun detectFeatures(context: Context): SamsungFeatures? {
        if (!isSamsung()) return null
        
        return SamsungFeatures(
            spen = detectSPen(context),
            dex = detectDex(context),
            knox = detectKnox(context),
            bixby = detectBixby(context),
            edgePanel = detectEdgePanel(context),
            ultraWideband = hasFeature(context, "com.samsung.feature.uwb"),
            secureFolder = isPackageInstalled(context, "com.samsung.knox.securefolder"),
            smartThings = isPackageInstalled(context, "com.samsung.android.oneconnect"),
            samsungHealth = isPackageInstalled(context, "com.samsung.android.service.health"),
            galaxyStore = isPackageInstalled(context, "com.sec.android.app.samsungapps"),
            oneUIVersion = getOneUIVersion()
        )
    }
    
    /**
     * Samsung Features data class
     */
    data class SamsungFeatures(
        val spen: SPenFeatures?,
        val dex: DexFeatures?,
        val knox: KnoxFeatures?,
        val bixby: BixbyFeatures?,
        val edgePanel: EdgePanelFeatures?,
        val ultraWideband: Boolean,
        val secureFolder: Boolean,
        val smartThings: Boolean,
        val samsungHealth: Boolean,
        val galaxyStore: Boolean,
        val oneUIVersion: String?
    )
    
    data class SPenFeatures(
        val supported: Boolean,
        val airActions: Boolean,
        val remoteControl: Boolean,
        val bluetooth: Boolean,
        val pressureLevels: Int = 4096
    )
    
    data class DexFeatures(
        val supported: Boolean,
        val wireless: Boolean,
        val onPC: Boolean,
        val version: String?
    )
    
    data class KnoxFeatures(
        val version: String?,
        val container: Boolean,
        val guard: Boolean,
        val attestation: Boolean
    )
    
    data class BixbyFeatures(
        val version: String?,
        val routines: Boolean,
        val vision: Boolean,
        val voice: Boolean
    )
    
    data class EdgePanelFeatures(
        val supported: Boolean,
        val lighting: Boolean,
        val panels: Int // Number of edge panels
    )
    
    // Private detection methods
    
    private fun detectSPen(context: Context): SPenFeatures? {
        if (!hasFeature(context, "com.sec.feature.spen_usp")) return null
        
        return SPenFeatures(
            supported = true,
            airActions = hasFeature(context, "com.samsung.feature.spen.airactions"),
            remoteControl = hasFeature(context, "com.samsung.feature.spen.remote"),
            bluetooth = hasFeature(context, "com.samsung.feature.spen.bluetooth")
        )
    }
    
    private fun detectDex(context: Context): DexFeatures? {
        if (!hasFeature(context, "com.sec.feature.desktopmode")) return null
        
        return DexFeatures(
            supported = true,
            wireless = hasFeature(context, "com.samsung.feature.samsung_dex_wireless"),
            onPC = true, // Most modern Samsung devices support DeX on PC
            version = getSystemProperty("ro.build.version.dex")
        )
    }
    
    private fun detectKnox(context: Context): KnoxFeatures {
        return KnoxFeatures(
            version = getSystemProperty("ro.config.knox"),
            container = hasFeature(context, "com.sec.feature.knox_container"),
            guard = hasFeature(context, "com.samsung.feature.knox_guard"),
            attestation = hasFeature(context, "com.samsung.feature.knox_attestation")
        )
    }
    
    private fun detectBixby(context: Context): BixbyFeatures {
        return BixbyFeatures(
            version = getPackageVersion(context, "com.samsung.android.bixby.agent"),
            routines = isPackageInstalled(context, "com.samsung.android.app.routines"),
            vision = isPackageInstalled(context, "com.samsung.android.visionintelligence"),
            voice = isPackageInstalled(context, "com.samsung.android.bixby.voiceinput")
        )
    }
    
    private fun detectEdgePanel(context: Context): EdgePanelFeatures? {
        if (!hasFeature(context, "com.samsung.feature.edge")) return null
        
        return EdgePanelFeatures(
            supported = true,
            lighting = hasFeature(context, "com.samsung.android.app.edgelighting"),
            panels = 5 // Default number of panels
        )
    }
    
    private fun getOneUIVersion(): String? {
        return getSystemProperty("ro.build.version.oneui")
    }
    
    // Helper methods
    
    private fun hasFeature(context: Context, feature: String): Boolean {
        return try {
            context.packageManager.hasSystemFeature(feature)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking feature: $feature", e)
            false
        }
    }
    
    private fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    private fun getPackageVersion(context: Context, packageName: String): String? {
        return try {
            val info = context.packageManager.getPackageInfo(packageName, 0)
            info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }
    
    private fun getSystemProperty(key: String): String? {
        return try {
            val systemProperties = Class.forName("android.os.SystemProperties")
            val get = systemProperties.getMethod("get", String::class.java)
            get.invoke(null, key) as? String
        } catch (e: Exception) {
            Log.e(TAG, "Error getting system property: $key", e)
            null
        }
    }
}