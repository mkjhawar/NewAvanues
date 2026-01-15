// Author: Manoj Jhawar
// Purpose: Cache management for device information to reduce overhead

package com.augmentalis.devicemanager.deviceinfo.cache

import android.content.Context
import android.content.SharedPreferences
import com.augmentalis.devicemanager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.io.File
import android.util.Log

/**
 * Device Info Cache Manager
 * Caches static device information to reduce scanning overhead
 * Only dynamic connections (USB, displays) are monitored in real-time
 */
class DeviceInfoCache(private val context: Context) {
    
    companion object {
        private const val TAG = "DeviceInfoCache"
        private const val CACHE_FILE = "device_info_cache.json"
        private const val PREFS_NAME = "device_info_prefs"
        private const val KEY_LAST_SCAN = "last_full_scan"
        private const val KEY_DEVICE_FINGERPRINT = "device_fingerprint"
        private const val KEY_AUTO_RESCAN = "auto_rescan_on_usb"
        private const val KEY_CACHE_VERSION = "cache_version"
        private const val CURRENT_CACHE_VERSION = 1
        
        // Cache validity duration (7 days by default)
        private const val CACHE_VALIDITY_DAYS = 7
        private const val CACHE_VALIDITY_MS = CACHE_VALIDITY_DAYS * 24 * 60 * 60 * 1000L
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    private val cacheFile = File(context.filesDir, CACHE_FILE)
    
    /**
     * Cached static device information
     */
    @Serializable
    data class CachedDeviceInfo(
        val timestamp: Long,
        val deviceProfile: DeviceProfileCache,
        val hardwareInfo: HardwareInfoCache,
        val scalingProfile: ScalingProfileCache,
        val deviceCapabilities: DeviceCapabilitiesCache,
        val sensors: List<String>,
        val cameras: List<CameraInfoCache>,
        val cacheVersion: Int = CURRENT_CACHE_VERSION
    )
    
    @Serializable
    data class DeviceProfileCache(
        val manufacturer: String,
        val model: String,
        val brand: String,
        val device: String,
        val product: String,
        val androidVersion: String,
        val apiLevel: Int,
        val buildId: String,
        val buildType: String,
        val fingerprint: String
    )
    
    @Serializable
    data class HardwareInfoCache(
        val board: String,
        val hardware: String,
        val supportedAbis: List<String>,
        val hasBluetooth: Boolean,
        val hasNfc: Boolean,
        val hasFingerprint: Boolean,
        val hasTelephony: Boolean
    )
    
    @Serializable
    data class ScalingProfileCache(
        val scaleFactor: Float,
        val fontScale: Float,
        val displayScale: Float,
        val uiMode: String
    )
    
    @Serializable
    data class DeviceCapabilitiesCache(
        val isTablet: Boolean,
        val isFoldable: Boolean,
        val isWearable: Boolean,
        val isXR: Boolean,
        val isTV: Boolean,
        val isAutomotive: Boolean,
        val has6DOFTracking: Boolean,
        val hasWirelessDisplaySupport: Boolean
    )
    
    @Serializable
    data class CameraInfoCache(
        val id: String,
        val facing: String,
        val megapixels: Float,
        val hasFlash: Boolean,
        val hasOIS: Boolean,
        val hasDepthSensor: Boolean,
        val hasRawSupport: Boolean,
        val isExternal: Boolean
    )
    
    /**
     * Check if cache exists and is valid
     */
    fun isCacheValid(): Boolean {
        if (!cacheFile.exists()) {
            Log.d(TAG, "Cache file does not exist")
            return false
        }
        
        val lastScan = prefs.getLong(KEY_LAST_SCAN, 0)
        val cacheAge = System.currentTimeMillis() - lastScan
        
        // Check if cache is too old
        if (cacheAge > CACHE_VALIDITY_MS) {
            Log.d(TAG, "Cache is too old: ${cacheAge / (1000 * 60 * 60)} hours")
            return false
        }
        
        // Check if device has been updated (OS update, factory reset, etc.)
        val currentFingerprint = android.os.Build.FINGERPRINT
        val cachedFingerprint = prefs.getString(KEY_DEVICE_FINGERPRINT, "")
        if (currentFingerprint != cachedFingerprint) {
            Log.d(TAG, "Device fingerprint changed, cache invalid")
            return false
        }
        
        // Check cache version
        val cacheVersion = prefs.getInt(KEY_CACHE_VERSION, 0)
        if (cacheVersion != CURRENT_CACHE_VERSION) {
            Log.d(TAG, "Cache version mismatch, cache invalid")
            return false
        }
        
        Log.d(TAG, "Cache is valid")
        return true
    }
    
    /**
     * Load cached device information
     */
    suspend fun loadCache(): CachedDeviceInfo? = withContext(Dispatchers.IO) {
        try {
            if (!isCacheValid()) {
                return@withContext null
            }
            
            val jsonString = cacheFile.readText()
            val cached = json.decodeFromString<CachedDeviceInfo>(jsonString)
            
            Log.d(TAG, "Loaded cache from ${(System.currentTimeMillis() - cached.timestamp) / 1000} seconds ago")
            cached
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load cache", e)
            null
        }
    }
    
    /**
     * Save device information to cache
     */
    suspend fun saveCache(deviceInfo: DeviceInfo) = withContext(Dispatchers.IO) {
        try {
            val profile = deviceInfo.getDeviceProfile()
            val hardware = deviceInfo.getHardwareInfo()
            val scaling = deviceInfo.getScalingProfile()
            val cameras = deviceInfo.getCameraInfo()
            
            val cached = CachedDeviceInfo(
                timestamp = System.currentTimeMillis(),
                deviceProfile = DeviceProfileCache(
                    manufacturer = profile.manufacturer,
                    model = profile.model,
                    brand = profile.brand,
                    device = profile.device,
                    product = profile.product,
                    androidVersion = profile.androidVersion,
                    apiLevel = profile.apiLevel,
                    buildId = profile.buildId,
                    buildType = profile.buildType,
                    fingerprint = profile.fingerprint
                ),
                hardwareInfo = HardwareInfoCache(
                    board = hardware.board,
                    hardware = hardware.hardware,
                    supportedAbis = hardware.supportedAbis,
                    hasBluetooth = hardware.hasBluetooth,
                    hasNfc = hardware.hasNfc,
                    hasFingerprint = hardware.hasFingerprint,
                    hasTelephony = hardware.hasTelephony
                ),
                scalingProfile = ScalingProfileCache(
                    scaleFactor = scaling.scaleFactor,
                    fontScale = scaling.fontScale,
                    displayScale = scaling.displayScale,
                    uiMode = scaling.uiMode
                ),
                deviceCapabilities = DeviceCapabilitiesCache(
                    isTablet = deviceInfo.isTablet(),
                    isFoldable = deviceInfo.isFoldable(),
                    isWearable = deviceInfo.isWearable(),
                    isXR = deviceInfo.isXR(),
                    isTV = deviceInfo.isTV(),
                    isAutomotive = deviceInfo.isAutomotive(),
                    has6DOFTracking = deviceInfo.has6DOFTracking(),
                    hasWirelessDisplaySupport = deviceInfo.hasWirelessDisplaySupport()
                ),
                sensors = hardware.sensors,
                cameras = cameras.map { camera ->
                    CameraInfoCache(
                        id = camera.id,
                        facing = camera.facing.name,
                        megapixels = camera.megapixels,
                        hasFlash = camera.hasFlash,
                        hasOIS = camera.hasOIS,
                        hasDepthSensor = camera.hasDepthSensor,
                        hasRawSupport = camera.hasRawSupport,
                        isExternal = camera.isExternal
                    )
                },
                cacheVersion = CURRENT_CACHE_VERSION
            )
            
            val jsonString = json.encodeToString(cached)
            cacheFile.writeText(jsonString)
            
            // Update preferences
            prefs.edit().apply {
                putLong(KEY_LAST_SCAN, System.currentTimeMillis())
                putString(KEY_DEVICE_FINGERPRINT, android.os.Build.FINGERPRINT)
                putInt(KEY_CACHE_VERSION, CURRENT_CACHE_VERSION)
                apply()
            }
            
            Log.d(TAG, "Cache saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save cache", e)
            false
        }
    }
    
    /**
     * Clear the cache
     */
    fun clearCache() {
        try {
            if (cacheFile.exists()) {
                cacheFile.delete()
            }
            prefs.edit().clear().apply()
            Log.d(TAG, "Cache cleared")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear cache", e)
        }
    }
    
    /**
     * Get auto-rescan on USB setting
     */
    fun isAutoRescanOnUSBEnabled(): Boolean {
        return prefs.getBoolean(KEY_AUTO_RESCAN, true)
    }
    
    /**
     * Set auto-rescan on USB setting
     */
    fun setAutoRescanOnUSB(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_RESCAN, enabled).apply()
    }
    
    /**
     * Get last scan timestamp
     */
    fun getLastScanTime(): Long {
        return prefs.getLong(KEY_LAST_SCAN, 0)
    }
    
    /**
     * Check if rescan is needed based on time
     */
    fun isRescanNeeded(): Boolean {
        val lastScan = getLastScanTime()
        if (lastScan == 0L) return true
        
        val timeSinceLastScan = System.currentTimeMillis() - lastScan
        return timeSinceLastScan > CACHE_VALIDITY_MS
    }
}
