/**
 * AppHandler.kt - Handles application-related actions
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-26
 * 
 * Handles app launching, switching, and management.
 * Implements ActionHandler interface (approved VOS4 exception).
 */
package com.augmentalis.voiceos.accessibility.handlers

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService

/**
 * Handler for application-related actions
 */
class AppHandler(
    private val service: VoiceAccessibilityService
) : ActionHandler {
    
    companion object {
        private const val TAG = "AppHandler"
        
        private val SUPPORTED_ACTIONS = listOf(
            "open", "launch", "start",
            "switch to", "go to",
            "close", "kill", "stop"
        )
    }
    
    private val packageManager: PackageManager = service.packageManager
    
    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()
        
        return when {
            normalizedAction.startsWith("open ") ||
            normalizedAction.startsWith("launch ") ||
            normalizedAction.startsWith("start ") -> {
                val appName = normalizedAction
                    .removePrefix("open ")
                    .removePrefix("launch ")
                    .removePrefix("start ")
                    .trim()
                launchApp(appName)
            }
            
            else -> {
                Log.w(TAG, "Unknown app action: $normalizedAction")
                false
            }
        }
    }
    
    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return SUPPORTED_ACTIONS.any { normalized.startsWith(it) }
    }
    
    override fun getSupportedActions(): List<String> {
        return SUPPORTED_ACTIONS.map { "$it <app name>" }
    }
    
    private fun launchApp(appName: String): Boolean {
        // Common app aliases
        val packageName = when(appName.lowercase()) {
            "browser", "chrome" -> "com.android.chrome"
            "camera", "photo" -> findPackageByKeyword("camera")
            "phone", "dialer" -> findPackageByKeyword("dialer", "phone")
            "messages", "sms" -> findPackageByKeyword("messages", "mms")
            "settings" -> "com.android.settings"
            "maps" -> "com.google.android.apps.maps"
            "youtube" -> "com.google.android.youtube"
            "gmail", "email" -> "com.google.android.gm"
            else -> findPackageByName(appName)
        }
        
        if (packageName == null) {
            Log.w(TAG, "App not found: $appName")
            return false
        }
        
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "No launch intent for: $packageName")
            return false
        }
        
        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            service.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
            false
        }
    }
    
    private fun findPackageByName(appName: String): String? {
        val packages = packageManager.getInstalledPackages(0)
        val normalized = appName.lowercase().replace(" ", "")
        
        return packages.firstOrNull { packageInfo ->
            val label = packageInfo.applicationInfo.loadLabel(packageManager)
                .toString().lowercase().replace(" ", "")
            label.contains(normalized) || 
            packageInfo.packageName.contains(normalized)
        }?.packageName
    }
    
    private fun findPackageByKeyword(vararg keywords: String): String? {
        val packages = packageManager.getInstalledPackages(0)
        
        return packages.firstOrNull { packageInfo ->
            keywords.any { keyword ->
                packageInfo.packageName.contains(keyword, ignoreCase = true)
            }
        }?.packageName
    }
}