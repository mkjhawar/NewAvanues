// Author: Manoj Jhawar
// Purpose: Android XR compatibility with graceful fallback

package com.augmentalis.devicemanager.compatibility

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import kotlin.reflect.KClass

/**
 * XR Compatibility handler for Android XR devices
 * Provides graceful fallback when XR libraries are not available
 */
object XRCompatibility {
    private const val TAG = "XRCompatibility"
    
    // XR feature flags
    private const val FEATURE_XR_IMMERSIVE = "android.software.xr.immersive"
    private const val FEATURE_VR_MODE = "android.software.vr.mode"
    private const val FEATURE_VR_HIGH_PERFORMANCE = "android.hardware.vr.high_performance"
    private const val FEATURE_VR_HEADTRACKING = "android.hardware.vr.headtracking"
    
    /**
     * Check if XR libraries are available at runtime
     */
    fun isXRLibraryAvailable(): Boolean {
        return try {
            // Try to load XR core class
            Class.forName("androidx.xr.core.XrCore")
            true
        } catch (e: ClassNotFoundException) {
            Log.d(TAG, "XR libraries not available: ${e.message}")
            false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking XR availability", e)
            false
        }
    }
    
    /**
     * Check if device supports XR features
     */
    fun isXRSupported(context: Context): Boolean {
        val packageManager = context.packageManager
        
        return try {
            // Check for any XR/VR related features
            packageManager.hasSystemFeature(FEATURE_XR_IMMERSIVE) ||
            packageManager.hasSystemFeature(FEATURE_VR_MODE) ||
            packageManager.hasSystemFeature(FEATURE_VR_HIGH_PERFORMANCE) ||
            packageManager.hasSystemFeature(FEATURE_VR_HEADTRACKING)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking XR support", e)
            false
        }
    }
    
    /**
     * Get XR device capabilities
     */
    fun getXRCapabilities(context: Context): XRCapabilities {
        val packageManager = context.packageManager
        
        return try {
            XRCapabilities(
                hasImmersiveMode = safeCheckFeature(packageManager, FEATURE_XR_IMMERSIVE),
                hasVRMode = safeCheckFeature(packageManager, FEATURE_VR_MODE),
                hasHighPerformanceVR = safeCheckFeature(packageManager, FEATURE_VR_HIGH_PERFORMANCE),
                hasHeadTracking = safeCheckFeature(packageManager, FEATURE_VR_HEADTRACKING),
                isXRLibraryAvailable = isXRLibraryAvailable()
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting XR capabilities", e)
            XRCapabilities()
        }
    }
    
    /**
     * Safe feature check with exception handling
     */
    private fun safeCheckFeature(packageManager: PackageManager, feature: String): Boolean {
        return try {
            packageManager.hasSystemFeature(feature)
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Initialize XR if available
     * Returns true if XR was successfully initialized
     */
    fun initializeXR(context: Context): Boolean {
        if (!isXRSupported(context)) {
            Log.d(TAG, "XR not supported on this device")
            return false
        }
        
        if (!isXRLibraryAvailable()) {
            Log.d(TAG, "XR libraries not available, running in compatibility mode")
            return false
        }
        
        return try {
            // Use reflection to initialize XR if available
            val xrCoreClass = Class.forName("androidx.xr.core.XrCore")
            val initMethod = xrCoreClass.getDeclaredMethod("initialize", Context::class.java)
            initMethod.invoke(null, context)
            
            Log.i(TAG, "XR successfully initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize XR", e)
            false
        }
    }
    
    /**
     * Create XR session if available
     */
    fun createXRSession(context: Context, sessionType: XRSessionType): Any? {
        if (!isXRLibraryAvailable()) {
            Log.w(TAG, "Cannot create XR session: libraries not available")
            return null
        }
        
        return try {
            val sessionClass = when (sessionType) {
                XRSessionType.IMMERSIVE -> Class.forName("androidx.xr.core.ImmersiveSession")
                XRSessionType.SPATIAL -> Class.forName("androidx.xr.spatial.SpatialSession")
                XRSessionType.FOUNDATION -> Class.forName("androidx.xr.foundation.FoundationSession")
            }
            
            val constructor = sessionClass.getConstructor(Context::class.java)
            constructor.newInstance(context)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create XR session", e)
            null
        }
    }
    
    /**
     * Safely invoke XR method with fallback
     */
    fun <T> safeXRCall(
        xrObject: Any?,
        methodName: String,
        paramTypes: Array<KClass<*>> = emptyArray(),
        params: Array<Any?> = emptyArray(),
        fallback: () -> T
    ): T {
        if (xrObject == null) {
            return fallback()
        }
        
        return try {
            val method = xrObject.javaClass.getDeclaredMethod(
                methodName,
                *paramTypes.map { it.java }.toTypedArray()
            )
            @Suppress("UNCHECKED_CAST")
            method.invoke(xrObject, *params) as T
        } catch (e: Exception) {
            Log.w(TAG, "XR method call failed: $methodName", e)
            fallback()
        }
    }
    
    /**
     * XR device capabilities data class
     */
    data class XRCapabilities(
        val hasImmersiveMode: Boolean = false,
        val hasVRMode: Boolean = false,
        val hasHighPerformanceVR: Boolean = false,
        val hasHeadTracking: Boolean = false,
        val isXRLibraryAvailable: Boolean = false
    ) {
        val isFullySupported: Boolean
            get() = hasImmersiveMode && isXRLibraryAvailable
        
        val isPartiallySupported: Boolean
            get() = (hasVRMode || hasHighPerformanceVR || hasHeadTracking) && !isFullySupported
    }
    
    /**
     * XR session types
     */
    enum class XRSessionType {
        IMMERSIVE,
        SPATIAL,
        FOUNDATION
    }
    
    /**
     * XR compatibility mode
     */
    enum class XRMode {
        FULL,      // Full XR support with libraries
        PARTIAL,   // Device supports XR but libraries not available
        FALLBACK,  // No XR support, use fallback rendering
        NONE       // No XR support at all
    }
    
    /**
     * Get current XR mode
     */
    fun getXRMode(context: Context): XRMode {
        val capabilities = getXRCapabilities(context)
        
        return when {
            capabilities.isFullySupported -> XRMode.FULL
            capabilities.isPartiallySupported -> XRMode.PARTIAL
            isXRSupported(context) -> XRMode.FALLBACK
            else -> XRMode.NONE
        }
    }
}

/**
 * Extension functions for XR compatibility
 */
fun Context.isXRSupported(): Boolean {
    return XRCompatibility.isXRSupported(this)
}

fun Context.getXRCapabilities(): XRCompatibility.XRCapabilities {
    return XRCompatibility.getXRCapabilities(this)
}

fun Context.getXRMode(): XRCompatibility.XRMode {
    return XRCompatibility.getXRMode(this)
}

fun Context.initializeXRIfAvailable(): Boolean {
    return XRCompatibility.initializeXR(this)
}
