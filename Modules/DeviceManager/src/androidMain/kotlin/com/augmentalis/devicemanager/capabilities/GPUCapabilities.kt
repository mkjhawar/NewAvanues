/**
 * GPUCapabilities.kt - GPU capability detection for all modules
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-27
 *
 * Detects GPU acceleration availability and provides fallback strategy.
 * RenderEffect requires API 31+ (Android 12/S), devices below use CPU fallback.
 *
 * This is a shared capability - used by MagicUI, NLU, AI, and other modules.
 */
package com.augmentalis.devicemanager.capabilities

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * GPU capability detection utility
 *
 * Provides runtime detection of GPU acceleration support:
 * - API 31+ (Android S): RenderEffect available
 * - API 29-30: CPU-optimized fallback
 * - < API 29: Not supported (minSdk 29)
 *
 * Used by:
 * - MagicUI (state diffing, blur effects)
 * - NLU (ML inference acceleration)
 * - AI (model acceleration)
 */
object GPUCapabilities {

    /**
     * Minimum API level for RenderEffect GPU acceleration
     */
    const val MIN_GPU_API = Build.VERSION_CODES.S // API 31

    /**
     * Check if GPU acceleration via RenderEffect is available
     * RenderEffect was introduced in Android 12 (API 31)
     */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isGpuAccelerationAvailable: Boolean by lazy {
        Build.VERSION.SDK_INT >= MIN_GPU_API
    }

    /**
     * Check if device supports hardware-accelerated blur
     * This is the primary GPU feature used by MagicEngine
     */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isBlurSupported: Boolean by lazy {
        isGpuAccelerationAvailable
    }

    /**
     * Check if device supports hardware-accelerated color filters
     * Used for state diffing visualization
     */
    @get:ChecksSdkIntAtLeast(api = Build.VERSION_CODES.S)
    val isColorFilterSupported: Boolean by lazy {
        isGpuAccelerationAvailable
    }

    /**
     * Get the current acceleration mode as a string
     * Useful for debugging and telemetry
     */
    val accelerationMode: AccelerationMode by lazy {
        when {
            isGpuAccelerationAvailable -> AccelerationMode.GPU_RENDER_EFFECT
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> AccelerationMode.CPU_OPTIMIZED
            else -> AccelerationMode.CPU_BASIC
        }
    }

    /**
     * Get human-readable GPU info for debugging
     */
    val gpuInfo: String by lazy {
        buildString {
            append("Acceleration: ${accelerationMode.displayName}")
            append(" | API: ${Build.VERSION.SDK_INT}")
            append(" | GPU Available: $isGpuAccelerationAvailable")
        }
    }

    /**
     * Get detailed capability report
     */
    fun getCapabilityReport(): Map<String, Any> = mapOf(
        "apiLevel" to Build.VERSION.SDK_INT,
        "accelerationMode" to accelerationMode.name,
        "gpuAvailable" to isGpuAccelerationAvailable,
        "blurSupported" to isBlurSupported,
        "colorFilterSupported" to isColorFilterSupported,
        "device" to Build.MODEL,
        "manufacturer" to Build.MANUFACTURER
    )

    /**
     * Acceleration modes supported
     */
    enum class AccelerationMode(val displayName: String) {
        /**
         * Full GPU acceleration using RenderEffect (API 31+)
         * - Hardware-accelerated blur
         * - GPU-based color filters
         * - Parallel state diffing
         */
        GPU_RENDER_EFFECT("RenderEffect (GPU)"),

        /**
         * CPU-optimized mode for API 29-30
         * - ConcurrentHashMap-based caching
         * - Hash-based state diffing
         * - Background thread optimization
         */
        CPU_OPTIMIZED("CPU Optimized"),

        /**
         * Basic CPU mode (fallback)
         * - Simple in-memory caching
         * - Sequential state updates
         */
        CPU_BASIC("CPU Basic")
    }
}
