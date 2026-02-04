// Author: Manoj Jhawar
// Purpose: API compatibility layer for Android 9-17 + XR

package com.augmentalis.devicemanager.compatibility

import android.os.Build
import android.content.Context
import android.hardware.camera2.CameraManager
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.biometric.BiometricManager as AndroidXBiometricManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telephony.TelephonyManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import android.util.Log

/**
 * API Compatibility layer providing fallback mechanisms for different Android versions
 * Supports: Android 9 (API 28) through Android 17 (API 34+) and Android XR
 */
object ApiCompatibility {
    
    // API Level constants for clarity
    const val API_28_PIE = Build.VERSION_CODES.P
    const val API_29_Q = Build.VERSION_CODES.Q
    const val API_30_R = Build.VERSION_CODES.R
    const val API_31_S = Build.VERSION_CODES.S
    const val API_32_S_V2 = Build.VERSION_CODES.S_V2
    const val API_33_TIRAMISU = Build.VERSION_CODES.TIRAMISU
    const val API_34_UPSIDE_DOWN_CAKE = Build.VERSION_CODES.UPSIDE_DOWN_CAKE
    const val API_35_VANILLA_ICE_CREAM = 35 // Android 15
    
    /**
     * Check if running on Android XR device
     */
    fun isXRDevice(context: Context): Boolean {
        return try {
            // Check for XR-specific features
            context.packageManager.hasSystemFeature("android.hardware.vr.high_performance") ||
            context.packageManager.hasSystemFeature("android.hardware.vr.headtracking") ||
            context.packageManager.hasSystemFeature("android.software.xr.immersive")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Get vibrator service with fallback for older APIs
     */
    fun getVibrator(context: Context): Vibrator? {
        return when {
            Build.VERSION.SDK_INT >= API_31_S -> {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            }
            else -> {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        }
    }
    
    /**
     * Vibrate with pattern, handling API differences
     */
    @RequiresPermission(Manifest.permission.VIBRATE)
    fun vibrate(context: Context, pattern: LongArray, repeat: Int = -1) {
        val vibrator = getVibrator(context) ?: return
        
        when {
            Build.VERSION.SDK_INT >= API_29_Q -> {
                val effects = pattern.mapIndexed { index, duration ->
                    if (index % 2 == 0) {
                        VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE)
                    } else {
                        VibrationEffect.createOneShot(duration, 0)
                    }
                }
                // For API 29+, use predefined effects
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            }
            Build.VERSION.SDK_INT >= API_28_PIE -> {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            }
            else -> {
                @Suppress("DEPRECATION")
                vibrator.vibrate(pattern, repeat)
            }
        }
    }
    
    /**
     * Check biometric availability with fallback
     */
    @RequiresPermission(Manifest.permission.USE_BIOMETRIC)
    fun getBiometricStatus(context: Context): BiometricStatus {
        return when {
            Build.VERSION.SDK_INT >= API_30_R -> {
                val biometricManager = context.getSystemService(Context.BIOMETRIC_SERVICE) as? BiometricManager
                when (biometricManager?.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                    BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                    BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                    BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
                    BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                    else -> BiometricStatus.UNKNOWN
                }
            }
            Build.VERSION.SDK_INT >= API_29_Q -> {
                val biometricManager = AndroidXBiometricManager.from(context)
                when (biometricManager.canAuthenticate(AndroidXBiometricManager.Authenticators.BIOMETRIC_STRONG)) {
                    AndroidXBiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
                    AndroidXBiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
                    AndroidXBiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> BiometricStatus.UNAVAILABLE
                    AndroidXBiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NOT_ENROLLED
                    else -> BiometricStatus.UNKNOWN
                }
            }
            else -> {
                // Fallback for API 28
                val packageManager = context.packageManager
                when {
                    !packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT) -> BiometricStatus.NO_HARDWARE
                    else -> BiometricStatus.AVAILABLE
                }
            }
        }
    }
    
    /**
     * Check network connectivity with fallback
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        
        return when {
            Build.VERSION.SDK_INT >= API_29_Q -> {
                val network = connectivityManager.activeNetwork ?: return false
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            }
            else -> {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo?.isConnected == true
            }
        }
    }
    
    /**
     * Get network type with fallback
     */
    @RequiresPermission(Manifest.permission.ACCESS_NETWORK_STATE)
    fun getNetworkType(context: Context): NetworkType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkType.NONE
        
        return when {
            Build.VERSION.SDK_INT >= API_29_Q -> {
                val network = connectivityManager.activeNetwork ?: return NetworkType.NONE
                val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.NONE
                
                when {
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                        if (Build.VERSION.SDK_INT >= API_30_R) {
                            // Check for 5G
                            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                            @Suppress("DEPRECATION")
                            when (telephonyManager?.dataNetworkType) {
                                TelephonyManager.NETWORK_TYPE_NR -> NetworkType.CELLULAR_5G
                                else -> NetworkType.CELLULAR_4G
                            }
                        } else {
                            NetworkType.CELLULAR_4G
                        }
                    }
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> NetworkType.ETHERNET
                    capabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> NetworkType.BLUETOOTH
                    else -> NetworkType.OTHER
                }
            }
            else -> {
                @Suppress("DEPRECATION")
                val networkInfo = connectivityManager.activeNetworkInfo ?: return NetworkType.NONE
                when (networkInfo.type) {
                    ConnectivityManager.TYPE_WIFI -> NetworkType.WIFI
                    ConnectivityManager.TYPE_MOBILE -> NetworkType.CELLULAR_4G
                    ConnectivityManager.TYPE_ETHERNET -> NetworkType.ETHERNET
                    ConnectivityManager.TYPE_BLUETOOTH -> NetworkType.BLUETOOTH
                    else -> NetworkType.OTHER
                }
            }
        }
    }
    
    /**
     * Check for camera availability with API fallback
     */
    fun getCameraInfo(context: Context): CameraInfo {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
            ?: return CameraInfo(false, 0, false)
        
        return try {
            val cameraIds = cameraManager.cameraIdList
            val hasMultipleCameras = cameraIds.size > 1
            
            // Check for advanced camera features based on API level
            val hasAdvancedFeatures = when {
                Build.VERSION.SDK_INT >= API_31_S -> {
                    // Check for camera2 concurrent streams
                    cameraManager.concurrentCameraIds.isNotEmpty()
                }
                Build.VERSION.SDK_INT >= API_30_R -> {
                    // Check for camera2 extensions
                    true // Simplified - would check for actual extensions
                }
                else -> false
            }
            
            CameraInfo(
                available = cameraIds.isNotEmpty(),
                cameraCount = cameraIds.size,
                hasAdvancedFeatures = hasAdvancedFeatures
            )
        } catch (e: Exception) {
            CameraInfo(false, 0, false)
        }
    }
    
    /**
     * Request permissions with fallback handling
     */
    fun checkPermission(context: Context, permission: String): Boolean {
        return when {
            Build.VERSION.SDK_INT >= API_33_TIRAMISU && permission == Manifest.permission.POST_NOTIFICATIONS -> {
                // Special handling for notification permission in Android 13+
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= API_31_S && permission.startsWith("android.permission.BLUETOOTH") -> {
                // Bluetooth permissions changed in Android 12
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
            Build.VERSION.SDK_INT >= API_30_R && permission == Manifest.permission.MANAGE_EXTERNAL_STORAGE -> {
                // All files access in Android 11+
                android.os.Environment.isExternalStorageManager()
            }
            Build.VERSION.SDK_INT >= API_29_Q && permission == Manifest.permission.ACCESS_BACKGROUND_LOCATION -> {
                // Background location in Android 10+
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
            else -> {
                ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
            }
        }
    }
    
    /**
     * Get location provider with fallback
     */
    fun getBestLocationProvider(context: Context): String? {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            ?: return null
        
        return when {
            Build.VERSION.SDK_INT >= API_31_S -> {
                // Use fused provider if available
                if (locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)) {
                    LocationManager.FUSED_PROVIDER
                } else {
                    locationManager.getBestProvider(android.location.Criteria(), true)
                }
            }
            Build.VERSION.SDK_INT >= API_28_PIE -> {
                // Try GPS first, then network
                when {
                    locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
                    else -> locationManager.getBestProvider(android.location.Criteria(), true)
                }
            }
            else -> {
                locationManager.getBestProvider(android.location.Criteria(), true)
            }
        }
    }
    
    // Data classes for API responses
    enum class BiometricStatus {
        AVAILABLE, NO_HARDWARE, UNAVAILABLE, NOT_ENROLLED, UNKNOWN
    }
    
    enum class NetworkType {
        NONE, WIFI, CELLULAR_4G, CELLULAR_5G, ETHERNET, BLUETOOTH, OTHER
    }
    
    data class CameraInfo(
        val available: Boolean,
        val cameraCount: Int,
        val hasAdvancedFeatures: Boolean
    )
}

/**
 * Extension functions for easier API compatibility
 */
fun Context.vibrateCompat(pattern: LongArray, repeat: Int = -1) {
    ApiCompatibility.vibrate(this, pattern, repeat)
}

fun Context.isNetworkAvailableCompat(): Boolean {
    return ApiCompatibility.isNetworkAvailable(this)
}

fun Context.checkPermissionCompat(permission: String): Boolean {
    return ApiCompatibility.checkPermission(this, permission)
}

fun Context.getBiometricStatusCompat(): ApiCompatibility.BiometricStatus {
    return ApiCompatibility.getBiometricStatus(this)
}
