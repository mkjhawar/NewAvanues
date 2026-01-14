package com.augmentalis.nlu.voiceos.detection

import android.content.Context
import android.content.pm.PackageManager

/**
 * VoiceOS installation detection
 *
 * Checks if VoiceOS is installed on the device
 * Uses PackageManager queries only
 */
class VoiceOSDetector(private val context: Context) {

    companion object {
        private const val VOICEOS_PACKAGE = "com.avanues.voiceos"
        private const val VOICEOS_LAUNCHER_PACKAGE = "com.avanues.launcher"
        private const val VOICEOS_FRAMEWORK_PACKAGE = "com.ideahq.voiceos"
    }

    /**
     * Check if VoiceOS is installed
     */
    fun isVoiceOSInstalled(): Boolean {
        val packages = listOf(
            VOICEOS_PACKAGE,
            VOICEOS_LAUNCHER_PACKAGE,
            VOICEOS_FRAMEWORK_PACKAGE
        )

        return packages.any { packageName ->
            isPackageInstalled(packageName)
        }
    }

    /**
     * Get list of installed VoiceOS packages
     */
    fun getInstalledVoiceOSPackages(): List<String> {
        val packages = listOf(
            VOICEOS_PACKAGE,
            VOICEOS_LAUNCHER_PACKAGE,
            VOICEOS_FRAMEWORK_PACKAGE
        )

        return packages.filter { packageName ->
            isPackageInstalled(packageName)
        }
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
}
