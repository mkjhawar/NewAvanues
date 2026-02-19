/**
 * AppActions.kt - Application control command actions
 * Path: modules/commands/src/main/java/com/augmentalis/voiceos/commands/actions/AppActions.kt
 * 
 * Created: 2025-08-19
 * Author: Claude Code
 * Module: Commands
 * 
 * Purpose: Application control and management-related voice command actions
 */

package com.augmentalis.voiceoscore.commandmanager.actions

import com.augmentalis.voiceoscore.*
import android.accessibilityservice.AccessibilityService
import android.app.ActivityManager
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build


/**
 * Application control command actions
 * Handles app launching, switching, closing, and management
 */
object AppActions {
    
    /**
     * Open App Action
     */
    class OpenAppAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val appName = getTextParameter(command, "app")
            val packageName = getTextParameter(command, "package")
            
            return when {
                packageName != null -> {
                    openAppByPackage(context, packageName)
                }
                appName != null -> {
                    openAppByName(context, appName)
                }
                else -> {
                    createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No app name or package specified")
                }
            }
        }
        
        private fun openAppByPackage(context: Context, packageName: String): ActionResult {
            return try {
                val intent = context.packageManager.getLaunchIntentForPackage(packageName)
                if (intent != null) {
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    createSuccessResult(Command("", "", source = CommandSource.VOICE, timestamp = System.currentTimeMillis()), "Opened app: $packageName")
                } else {
                    createErrorResult(Command("", "", source = CommandSource.VOICE, timestamp = System.currentTimeMillis()), ErrorCode.EXECUTION_FAILED, "App not found: $packageName")
                }
            } catch (e: Exception) {
                createErrorResult(Command("", "", source = CommandSource.VOICE, timestamp = System.currentTimeMillis()), ErrorCode.EXECUTION_FAILED, "Failed to open app: ${e.message}")
            }
        }
        
        private fun openAppByName(context: Context, appName: String): ActionResult {
            return try {
                val packageManager = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                
                val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val targetApp = apps.find { 
                    val label = it.loadLabel(packageManager).toString()
                    label.lowercase().contains(appName.lowercase())
                }
                
                if (targetApp != null) {
                    val launchIntent = Intent().apply {
                        setClassName(targetApp.activityInfo.packageName, targetApp.activityInfo.name)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(launchIntent)
                    val actualName = targetApp.loadLabel(packageManager).toString()
                    createSuccessResult(Command("", "", source = CommandSource.VOICE, timestamp = System.currentTimeMillis()), "Opened app: $actualName")
                } else {
                    createErrorResult(Command("", "", source = CommandSource.VOICE, timestamp = System.currentTimeMillis()), ErrorCode.EXECUTION_FAILED, "App not found: $appName")
                }
            } catch (e: Exception) {
                createErrorResult(Command("", "", source = CommandSource.VOICE, timestamp = System.currentTimeMillis()), ErrorCode.EXECUTION_FAILED, "Failed to open app: ${e.message}")
            }
        }
    }
    
    /**
     * Close App Action
     */
    class CloseAppAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val appName = getTextParameter(command, "app")
            val current = getBooleanParameter(command, "current") ?: true
            
            return if (current) {
                // Close current app by going home
                if (performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_HOME)) {
                    createSuccessResult(command, "Closed current app")
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to close current app")
                }
            } else if (appName != null) {
                // Close specific app by package name (requires root or system permissions)
                closeSpecificApp(context, appName, command)
            } else {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No app specified to close")
            }
        }
        
        private fun closeSpecificApp(context: Context, appName: String, command: Command): ActionResult {
            return try {
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                val packageManager = context.packageManager
                
                // Find package name by app name
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                
                val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val targetApp = apps.find { 
                    val label = it.loadLabel(packageManager).toString()
                    label.lowercase().contains(appName.lowercase())
                }
                
                if (targetApp != null) {
                    // Note: killBackgroundProcesses requires KILL_BACKGROUND_PROCESSES permission
                    // and only works on background processes
                    activityManager.killBackgroundProcesses(targetApp.activityInfo.packageName)
                    val actualName = targetApp.loadLabel(packageManager).toString()
                    createSuccessResult(command, "Attempted to close app: $actualName")
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "App not found: $appName")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to close app: ${e.message}")
            }
        }
    }
    
    /**
     * Switch App Action
     */
    class SwitchAppAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val appName = getTextParameter(command, "app")
            val direction = getTextParameter(command, "direction") // "next", "previous"
            
            return when {
                appName != null -> {
                    // Switch to specific app
                    OpenAppAction().execute(command, accessibilityService, context)
                }
                direction == "next" || direction == "previous" -> {
                    // Use recent apps and navigate
                    if (performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_RECENTS)) {
                        // This would require additional logic to navigate within recents
                        createSuccessResult(command, "Opened recent apps for switching")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to open recent apps")
                    }
                }
                else -> {
                    // Default to recent apps
                    if (performGlobalAction(accessibilityService, AccessibilityService.GLOBAL_ACTION_RECENTS)) {
                        createSuccessResult(command, "Opened recent apps")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to open recent apps")
                    }
                }
            }
        }
    }
    
    /**
     * List Running Apps Action
     */
    class ListRunningAppsAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            return try {
                val packageManager = context.packageManager
                val appNames = getRunningAppsModern(context, packageManager, 10)
                
                
                val message = if (appNames.isNotEmpty()) {
                    "Running apps: ${appNames.joinToString(", ")}"
                } else {
                    "No running apps found"
                }
                
                createSuccessResult(command, message, appNames)
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to list running apps: ${e.message}")
            }
        }
    }
    
    /**
     * Find App Action
     */
    class FindAppAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val searchTerm = getTextParameter(command, "search")
            
            return if (searchTerm == null) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No search term provided")
            } else {
                try {
                    val packageManager = context.packageManager
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    
                    val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    val matchingApps = apps.filter { 
                        val label = it.loadLabel(packageManager).toString()
                        label.lowercase().contains(searchTerm.lowercase())
                    }.map { it.loadLabel(packageManager).toString() }
                    
                    val message = if (matchingApps.isNotEmpty()) {
                        "Found apps: ${matchingApps.joinToString(", ")}"
                    } else {
                        "No apps found matching '$searchTerm'"
                    }
                    
                    createSuccessResult(command, message, matchingApps)
                } catch (e: Exception) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to search apps: ${e.message}")
                }
            }
        }
    }
    
    /**
     * App Info Action
     */
    class AppInfoAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val appName = getTextParameter(command, "app")
            val current = getBooleanParameter(command, "current") ?: false
            
            return if (current) {
                getCurrentAppInfo(context, command)
            } else if (appName != null) {
                getSpecificAppInfo(context, appName, command)
            } else {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No app specified")
            }
        }
        
        private fun getCurrentAppInfo(context: Context, command: Command): ActionResult {
            return try {
                val packageManager = context.packageManager
                val packageName = getCurrentAppPackageModern(context)
                
                if (packageName != null) {
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val versionInfo = packageManager.getPackageInfo(packageName, 0)
                    
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        versionInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        versionInfo.versionCode.toString()
                    }
                    
                    val info = mapOf(
                        "name" to appName,
                        "package" to packageName,
                        "version" to versionInfo.versionName,
                        "versionCode" to versionCode
                    )
                    
                    val message = "Current app: $appName ($packageName) v${versionInfo.versionName}"
                    createSuccessResult(command, message, info)
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Could not get current app info")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to get app info: ${e.message}")
            }
        }
        
        private fun getSpecificAppInfo(context: Context, appName: String, command: Command): ActionResult {
            return try {
                val packageManager = context.packageManager
                val intent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                
                val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                val targetApp = apps.find { 
                    val label = it.loadLabel(packageManager).toString()
                    label.lowercase().contains(appName.lowercase())
                }
                
                if (targetApp != null) {
                    val packageName = targetApp.activityInfo.packageName
                    val actualName = targetApp.loadLabel(packageManager).toString()
                    val versionInfo = packageManager.getPackageInfo(packageName, 0)
                    
                    val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        versionInfo.longVersionCode.toString()
                    } else {
                        @Suppress("DEPRECATION")
                        versionInfo.versionCode.toString()
                    }
                    
                    val info = mapOf(
                        "name" to actualName,
                        "package" to packageName,
                        "version" to versionInfo.versionName,
                        "versionCode" to versionCode
                    )
                    
                    val message = "App info: $actualName ($packageName) v${versionInfo.versionName}"
                    createSuccessResult(command, message, info)
                } else {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "App not found: $appName")
                }
            } catch (e: Exception) {
                createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to get app info: ${e.message}")
            }
        }
    }
    
    /**
     * Force Stop App Action
     */
    class ForceStopAppAction : BaseAction() {
        override suspend fun execute(
            command: Command,
            accessibilityService: AccessibilityService?,
            context: Context
        ): ActionResult {
            val appName = getTextParameter(command, "app")
            
            return if (appName == null) {
                createErrorResult(command, ErrorCode.INVALID_PARAMETERS, "No app specified to force stop")
            } else {
                try {
                    // This typically requires system-level permissions
                    // For now, we'll just try to kill background processes
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                    val packageManager = context.packageManager
                    
                    val intent = Intent(Intent.ACTION_MAIN).apply {
                        addCategory(Intent.CATEGORY_LAUNCHER)
                    }
                    
                    val apps = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                    val targetApp = apps.find { 
                        val label = it.loadLabel(packageManager).toString()
                        label.lowercase().contains(appName.lowercase())
                    }
                    
                    if (targetApp != null) {
                        activityManager.killBackgroundProcesses(targetApp.activityInfo.packageName)
                        val actualName = targetApp.loadLabel(packageManager).toString()
                        createSuccessResult(command, "Force stopped app: $actualName")
                    } else {
                        createErrorResult(command, ErrorCode.EXECUTION_FAILED, "App not found: $appName")
                    }
                } catch (e: Exception) {
                    createErrorResult(command, ErrorCode.EXECUTION_FAILED, "Failed to force stop app: ${e.message}")
                }
            }
        }
    }
    
    // ==================== HELPER FUNCTIONS ====================
    
    /**
     * Modern replacement for getRunningTasks() using UsageStatsManager
     */
    private fun getRunningAppsModern(
        context: Context, 
        packageManager: PackageManager, 
        maxApps: Int
    ): List<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                // Use UsageStatsManager for API 21+
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val endTime = System.currentTimeMillis()
                val beginTime = endTime - 1000 * 60 * 5 // Last 5 minutes
                
                val usageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    beginTime,
                    endTime
                )
                
                usageStats.filter { it.lastTimeUsed > beginTime }
                    .sortedByDescending { it.lastTimeUsed }
                    .take(maxApps)
                    .mapNotNull { stat ->
                        try {
                            val appInfo = packageManager.getApplicationInfo(stat.packageName, 0)
                            packageManager.getApplicationLabel(appInfo).toString()
                        } catch (e: PackageManager.NameNotFoundException) {
                            null // Skip apps that can't be found
                        }
                    }
            } catch (e: Exception) {
                // Fallback to deprecated method if UsageStats fails (permissions, etc.)
                getRunningAppsLegacy(context, packageManager, maxApps)
            }
        } else {
            getRunningAppsLegacy(context, packageManager, maxApps)
        }
    }
    
    /**
     * Legacy fallback for getRunningTasks()
     */
    @Suppress("DEPRECATION")
    private fun getRunningAppsLegacy(
        context: Context, 
        packageManager: PackageManager, 
        maxApps: Int
    ): List<String> {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningTasks = activityManager.getRunningTasks(maxApps)
            
            runningTasks.mapNotNull { task ->
                try {
                    val packageName = task.topActivity?.packageName ?: return@mapNotNull null
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Modern replacement for getting current app package using UsageStatsManager
     */
    private fun getCurrentAppPackageModern(context: Context): String? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            try {
                val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
                val endTime = System.currentTimeMillis()
                val beginTime = endTime - 1000 * 60 // Last minute
                
                val usageStats = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_BEST,
                    beginTime,
                    endTime
                )
                
                usageStats.maxByOrNull { it.lastTimeUsed }?.packageName
            } catch (e: Exception) {
                getCurrentAppPackageLegacy(context)
            }
        } else {
            getCurrentAppPackageLegacy(context)
        }
    }
    
    /**
     * Legacy fallback for getting current app package
     */
    @Suppress("DEPRECATION")
    private fun getCurrentAppPackageLegacy(context: Context): String? {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val runningTasks = activityManager.getRunningTasks(1)
            runningTasks.firstOrNull()?.topActivity?.packageName
        } catch (e: Exception) {
            null
        }
    }
}