package com.augmentalis.voiceos

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager

/**
 * Helper class for accessibility service setup
 */
class AccessibilitySetupHelper(private val context: Context) {
    
    companion object {
        const val SERVICE_PACKAGE = "com.augmentalis.voiceos"
        const val SERVICE_CLASS = "com.ai.voiceaccessibility.service.AccessibilityService"
        val SERVICE_COMPONENT = ComponentName(SERVICE_PACKAGE, SERVICE_CLASS)
    }
    
    /**
     * Check if our accessibility service is enabled
     */
    fun isServiceEnabled(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        
        // Method 1: Check enabled services string
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        val serviceName = SERVICE_COMPONENT.flattenToString()
        if (enabledServices?.contains(serviceName) == true) {
            return true
        }
        
        // Method 2: Check through AccessibilityManager (more reliable)
        val enabledAccessibilityServices = am.getEnabledAccessibilityServiceList(
            AccessibilityServiceInfo.FEEDBACK_ALL_MASK
        )
        for (service in enabledAccessibilityServices) {
            val enabledServiceInfo = service.resolveInfo.serviceInfo
            if (enabledServiceInfo.packageName == SERVICE_PACKAGE &&
                enabledServiceInfo.name == SERVICE_CLASS) {
                return true
            }
        }
        
        return false
    }
    
    /**
     * Open accessibility settings with our service highlighted (Android 11+)
     */
    fun openAccessibilitySettings(): Intent {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        
        // On Android 11+, highlight our specific service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            intent.putExtra(
                Intent.EXTRA_COMPONENT_NAME,
                SERVICE_COMPONENT.flattenToString()
            )
        }
        
        return intent
    }
    
    /**
     * Get instructions for enabling the service
     */
    fun getEnableInstructions(): String {
        return buildString {
            appendLine("To enable VoiceOS Accessibility:")
            appendLine("1. You'll be taken to Accessibility Settings")
            appendLine("2. Find 'VoiceOS Accessibility' in the list")
            appendLine("3. Tap on it to open settings")
            appendLine("4. Toggle the switch to ON")
            appendLine("5. Confirm by tapping 'Allow' in the dialog")
        }
    }
    
    /**
     * Check if accessibility is available on this device
     */
    fun isAccessibilitySupported(): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        return am != null
    }
}