/**
 * AppHandler.kt - Handles application-related actions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-01-26
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.voiceoscore.accessibility.IVoiceOSContext

/**
 * Handler for application-related actions
 */
class AppHandler(
    private val context: IVoiceOSContext
) : ActionHandler {

    companion object {
        private const val TAG = "AppHandler"
    }

    private val packageManager: PackageManager = context.getPackageManager()

    override fun execute(
        category: ActionCategory,
        action: String,
        params: Map<String, Any>
    ): Boolean {
        val normalizedAction = action.lowercase().trim()
        val appPackage = context.getAppCommands()[normalizedAction]
        return when {
            appPackage != null -> {
                launchApp(appPackage)
            }

            else -> {
                Log.w(TAG, "Unknown app action: $normalizedAction")
                false
            }
        }
    }

    override fun canHandle(action: String): Boolean {
        val normalized = action.lowercase().trim()
        return context.getAppCommands().containsKey(normalized)
    }

    override fun getSupportedActions(): List<String> {
        return context.getAppCommands().keys.toList()
    }

    private fun launchApp(packageName: String?): Boolean {
        if (packageName == null) {
            Log.w(TAG, "App not found: $packageName")
            return false
        }

        val intent = packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "No launch intent for: $packageName")
            return false
        }

        return try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $packageName", e)
            false
        }
    }

}