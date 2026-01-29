package com.augmentalis.voiceos.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * Boot receiver to auto-start OverlayService when device boots.
 *
 * Prerequisites:
 * 1. Accessibility service must be enabled (user grants once)
 * 2. Overlay permission must be granted (user grants once)
 *
 * Once both permissions are granted, the overlay will automatically
 * start on every device reboot without user intervention.
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "VoiceOSBootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            Log.d(TAG, "Boot completed - checking if overlay should start")

            // Check if overlay permission is granted
            if (!Settings.canDrawOverlays(context)) {
                Log.w(TAG, "Overlay permission not granted - skipping auto-start")
                return
            }

            // Check if accessibility service is enabled
            if (!isAccessibilityServiceEnabled(context)) {
                Log.w(TAG, "Accessibility service not enabled - skipping auto-start")
                return
            }

            // Both permissions granted - start overlay service
            Log.d(TAG, "Both permissions granted - starting OverlayService")
            OverlayService.start(context)
        }
    }

    /**
     * Check if VoiceOSAccessibilityService is enabled in accessibility settings.
     */
    private fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val serviceName = "${context.packageName}/${VoiceOSAccessibilityService::class.java.canonicalName}"
        return enabledServices.contains(serviceName)
    }
}
