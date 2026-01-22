// filename: Universal/AVA/Features/RAG/src/androidMain/kotlin/com/augmentalis/ava/features/rag/embeddings/PlatformDetector.kt
// created: 2025-11-04
// author: AVA AI Team
// © Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.rag.embeddings

import android.content.Context
import android.content.pm.PackageManager

/**
 * Detects the Android platform type for determining download source
 *
 * - Google Play: Use Play Feature Delivery (dynamic modules)
 * - AOSP/F-Droid/Custom: Use custom download server
 */
object PlatformDetector {

    /**
     * Android platform type
     */
    enum class PlatformType {
        /** Google Play Services available - use Play Feature Delivery */
        GOOGLE_PLAY,

        /** AOSP/LineageOS/custom ROM - use custom download server */
        AOSP,

        /** F-Droid or other app store */
        OTHER
    }

    /**
     * Detect current platform type
     */
    fun detectPlatform(context: Context): PlatformType {
        return when {
            hasGooglePlayServices(context) -> PlatformType.GOOGLE_PLAY
            isFDroid(context) -> PlatformType.OTHER
            else -> PlatformType.AOSP
        }
    }

    /**
     * Check if Google Play Services is available
     */
    private fun hasGooglePlayServices(context: Context): Boolean {
        return try {
            val packageManager = context.packageManager
            packageManager.getPackageInfo("com.google.android.gms", 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Check if installed from F-Droid
     */
    private fun isFDroid(context: Context): Boolean {
        val installer = try {
            context.packageManager.getInstallerPackageName(context.packageName)
        } catch (e: Exception) {
            null
        }

        return installer == "org.fdroid.fdroid" ||
                installer == "org.fdroid.fdroid.privileged"
    }

    /**
     * Get appropriate download URL based on platform
     *
     * @param modelId Model identifier
     * @param customServerUrl Optional custom server URL for AOSP
     * @return Download URL or null if should use dynamic feature
     */
    fun getDownloadUrl(
        context: Context,
        modelId: String,
        customServerUrl: String? = null
    ): String? {
        val platform = detectPlatform(context)

        return when (platform) {
            PlatformType.GOOGLE_PLAY -> {
                // Use Play Feature Delivery - return null to indicate
                // app should use Google Play's module delivery
                null
            }

            PlatformType.AOSP, PlatformType.OTHER -> {
                // Use custom server or HuggingFace as fallback
                // Note: Custom server uses .AON extension, HuggingFace uses original .onnx
                customServerUrl?.let { "$it/models/$modelId.AON" }
                    ?: getHuggingFaceUrl(modelId)
            }
        }
    }

    /**
     * Get HuggingFace download URL for a model
     */
    private fun getHuggingFaceUrl(modelId: String): String {
        return "https://huggingface.co/sentence-transformers/$modelId/resolve/main/onnx/model.onnx"
    }

    /**
     * Check if platform supports Play Feature Delivery
     */
    fun supportsPlayFeatureDelivery(context: Context): Boolean {
        return detectPlatform(context) == PlatformType.GOOGLE_PLAY
    }

    /**
     * Get platform information for logging/debugging
     */
    fun getPlatformInfo(context: Context): Map<String, String> {
        val platform = detectPlatform(context)
        val hasGPS = hasGooglePlayServices(context)
        val installer = try {
            context.packageManager.getInstallerPackageName(context.packageName) ?: "unknown"
        } catch (e: Exception) {
            "error"
        }

        return mapOf(
            "platform" to platform.name,
            "hasGooglePlayServices" to hasGPS.toString(),
            "installer" to installer,
            "supportsPlayFeatureDelivery" to supportsPlayFeatureDelivery(context).toString()
        )
    }
}
