package com.augmentalis.ava.features.nlu.voiceos.detection

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log

/**
 * VoiceOS Installation and Status Detection
 *
 * Checks if VoiceOS is installed and accessibility service is enabled.
 * Supports multiple package name variations for backwards compatibility.
 *
 * Updated: 2025-12-07 - Updated package names to match current VoiceOS
 */
class VoiceOSDetector(private val context: Context) {

    companion object {
        private const val TAG = "VoiceOSDetector"

        // Current VoiceOS package (primary)
        private const val VOICEOS_PACKAGE = "com.augmentalis.voiceoscore"

        // Legacy package names for backwards compatibility
        private val VOICEOS_PACKAGES = listOf(
            VOICEOS_PACKAGE,                    // Current
            "com.augmentalis.voiceos",          // Alternative
            "com.avanues.voiceos",              // Legacy brand
            "com.ideahq.voiceos"                // Early development
        )

        // Accessibility service identifiers
        private val VOICEOS_ACCESSIBILITY_SERVICES = listOf(
            "$VOICEOS_PACKAGE/.accessibility.VoiceOSService",
            "$VOICEOS_PACKAGE/com.augmentalis.voiceoscore.accessibility.VoiceOSService",
            "com.avanues.voiceos/.accessibility.VoiceOSService"
        )
    }

    /**
     * Check if VoiceOS is installed
     *
     * @return true if any VoiceOS package is installed
     */
    fun isVoiceOSInstalled(): Boolean {
        val installed = VOICEOS_PACKAGES.any { packageName ->
            isPackageInstalled(packageName)
        }
        Log.d(TAG, "VoiceOS installed: $installed")
        return installed
    }

    /**
     * Get primary installed VoiceOS package
     *
     * @return Package name of installed VoiceOS, or null
     */
    fun getInstalledPackage(): String? {
        return VOICEOS_PACKAGES.firstOrNull { packageName ->
            isPackageInstalled(packageName)
        }
    }

    /**
     * Get list of all installed VoiceOS packages
     *
     * @return List of installed package names (usually 1)
     */
    fun getInstalledVoiceOSPackages(): List<String> {
        return VOICEOS_PACKAGES.filter { packageName ->
            isPackageInstalled(packageName)
        }
    }

    /**
     * Check if VoiceOS accessibility service is enabled
     *
     * @return true if accessibility service is running
     */
    fun isAccessibilityServiceEnabled(): Boolean {
        return try {
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false

            // Check for service in enabled list
            val enabled = VOICEOS_ACCESSIBILITY_SERVICES.any { serviceName ->
                enabledServices.contains(serviceName.substringBefore("/"))
            } || VOICEOS_PACKAGES.any { packageName ->
                enabledServices.contains(packageName)
            }

            Log.d(TAG, "Accessibility service enabled: $enabled")
            enabled
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility: ${e.message}")
            false
        }
    }

    /**
     * Check if VoiceOS is fully operational
     *
     * @return true if installed AND accessibility enabled
     */
    fun isVoiceOSReady(): Boolean {
        return isVoiceOSInstalled() && isAccessibilityServiceEnabled()
    }

    /**
     * Get VoiceOS version if installed
     *
     * @return Version name or null
     */
    fun getVoiceOSVersion(): String? {
        val packageName = getInstalledPackage() ?: return null
        return try {
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    /**
     * Get detailed VoiceOS status
     *
     * @return Status object with installation and accessibility info
     */
    fun getStatus(): VoiceOSStatus {
        val installed = isVoiceOSInstalled()
        val packageName = if (installed) getInstalledPackage() else null
        val accessibilityEnabled = if (installed) isAccessibilityServiceEnabled() else false
        val version = if (installed) getVoiceOSVersion() else null

        return VoiceOSStatus(
            installed = installed,
            packageName = packageName,
            accessibilityEnabled = accessibilityEnabled,
            version = version,
            ready = installed && accessibilityEnabled
        )
    }

    /**
     * Check if specific package is installed
     */
    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * VoiceOS status data class
     */
    data class VoiceOSStatus(
        val installed: Boolean,
        val packageName: String?,
        val accessibilityEnabled: Boolean,
        val version: String?,
        val ready: Boolean
    )
}
