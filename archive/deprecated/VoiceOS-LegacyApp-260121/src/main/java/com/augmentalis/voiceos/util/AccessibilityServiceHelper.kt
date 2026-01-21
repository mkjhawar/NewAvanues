/**
 * AccessibilityServiceHelper.kt - Utility for accessibility service management
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-12-18
 */

package com.augmentalis.voiceos.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.text.TextUtils

/**
 * Helper object for managing VoiceOS accessibility service state.
 */
object AccessibilityServiceHelper {

    private const val VOICEOS_SERVICE_CLASS = "com.augmentalis.voiceoscore.accessibility.VoiceOSService"
    private const val VOICEOS_PACKAGE = "com.augmentalis.voiceos"

    /**
     * Check if VoiceOS accessibility service is enabled.
     *
     * @param context Application context
     * @return true if VoiceOS service is enabled in accessibility settings
     */
    fun isVoiceOSServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )

        if (TextUtils.isEmpty(enabledServices)) {
            return false
        }

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)

        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.contains(VOICEOS_SERVICE_CLASS) ||
                componentName.contains("VoiceOSService")) {
                return true
            }
        }

        return false
    }

    /**
     * Open Android Accessibility Settings.
     *
     * @param context Activity context
     */
    fun openAccessibilitySettings(context: Context) {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    /**
     * Open accessibility settings directly to VoiceOS service (Android 11+).
     * Falls back to general accessibility settings on older versions.
     *
     * @param context Activity context
     */
    fun openVoiceOSAccessibilitySettings(context: Context) {
        try {
            // Try direct intent to VoiceOS service settings (Android 11+)
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            val componentName = ComponentName(VOICEOS_PACKAGE, VOICEOS_SERVICE_CLASS)
            intent.putExtra(":settings:fragment_args_key", componentName.flattenToString())
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general accessibility settings
            openAccessibilitySettings(context)
        }
    }

    /**
     * Check if microphone permission is granted.
     *
     * @param context Application context
     * @return true if RECORD_AUDIO permission is granted
     */
    fun isMicrophonePermissionGranted(context: Context): Boolean {
        return context.checkSelfPermission(android.Manifest.permission.RECORD_AUDIO) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}
