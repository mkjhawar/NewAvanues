/**
 * AppCommandManager.kt - Manages application-specific commands
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-01-27
 * 
 * Manages app-specific command patterns and routing.
 * VOS4 Direct Implementation - No interfaces.
 */
package com.augmentalis.voiceos.accessibility.managers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages application-specific command patterns
 * Direct implementation following VOS4 patterns
 */
class AppCommandManager(private val service: VoiceAccessibilityService) {
    
    companion object {
        private const val TAG = "AppCommandManager"
        private const val PREFS_NAME = "app_command_prefs"
        private const val KEY_CUSTOM_COMMANDS = "custom_commands"
    }
    
    private val packageManager: PackageManager = service.packageManager
    private val prefs = service.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // App command patterns
    private val appCommands = ConcurrentHashMap<String, AppCommandPattern>()
    
    // Custom user-defined commands
    private val customCommands = ConcurrentHashMap<String, String>()
    
    // Common app package mappings
    private val commonApps = mapOf(
        "chrome" to "com.android.chrome",
        "browser" to "com.android.chrome",
        "camera" to "com.android.camera2",
        "phone" to "com.android.dialer",
        "dialer" to "com.android.dialer",
        "messages" to "com.google.android.apps.messaging",
        "sms" to "com.google.android.apps.messaging",
        "settings" to "com.android.settings",
        "maps" to "com.google.android.apps.maps",
        "youtube" to "com.google.android.youtube",
        "gmail" to "com.google.android.gm",
        "email" to "com.google.android.gm",
        "calendar" to "com.google.android.calendar",
        "photos" to "com.google.android.apps.photos",
        "gallery" to "com.google.android.apps.photos",
        "drive" to "com.google.android.apps.docs",
        "docs" to "com.google.android.apps.docs.editors.docs",
        "sheets" to "com.google.android.apps.docs.editors.sheets",
        "slides" to "com.google.android.apps.docs.editors.slides",
        "play store" to "com.android.vending",
        "store" to "com.android.vending",
        "calculator" to "com.google.android.calculator",
        "clock" to "com.google.android.deskclock",
        "alarm" to "com.google.android.deskclock",
        "files" to "com.google.android.apps.nbu.files",
        "whatsapp" to "com.whatsapp",
        "facebook" to "com.facebook.katana",
        "instagram" to "com.instagram.android",
        "twitter" to "com.twitter.android",
        "spotify" to "com.spotify.music",
        "netflix" to "com.netflix.mediaclient",
        "uber" to "com.ubercab",
        "lyft" to "me.lyft.android"
    )
    
    data class AppCommandPattern(
        val packageName: String,
        val commands: Map<String, CommandAction>,
        val aliases: List<String> = emptyList()
    )
    
    data class CommandAction(
        val action: String,
        val params: Map<String, Any> = emptyMap(),
        val description: String = ""
    )
    
    /**
     * Initialize app command manager
     */
    fun initialize() {
        Log.d(TAG, "Initializing AppCommandManager")
        
        // Load built-in command patterns
        loadBuiltInPatterns()
        
        // Load custom commands from preferences
        loadCustomCommands()
        
        Log.i(TAG, "Loaded ${appCommands.size} app patterns, ${customCommands.size} custom commands")
    }
    
    /**
     * Get package name for app name
     */
    fun getPackageName(appName: String): String? {
        val normalized = appName.lowercase().trim()
        
        // Check common apps first
        commonApps[normalized]?.let { return it }
        
        // Check installed apps
        return findPackageByName(normalized)
    }
    
    /**
     * Register custom command
     */
    fun registerCustomCommand(trigger: String, action: String): Boolean {
        return try {
            customCommands[trigger.lowercase()] = action
            saveCustomCommands()
            Log.d(TAG, "Registered custom command: $trigger -> $action")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register custom command", e)
            false
        }
    }
    
    /**
     * Remove custom command
     */
    fun removeCustomCommand(trigger: String): Boolean {
        val removed = customCommands.remove(trigger.lowercase()) != null
        if (removed) {
            saveCustomCommands()
            Log.d(TAG, "Removed custom command: $trigger")
        }
        return removed
    }
    
    /**
     * Get all custom commands
     */
    fun getCustomCommands(): Map<String, String> {
        return customCommands.toMap()
    }
    
    /**
     * Execute custom command if exists
     */
    fun executeCustomCommand(command: String): String? {
        return customCommands[command.lowercase()]
    }
    
    /**
     * Get app-specific commands for current app
     */
    fun getCurrentAppCommands(): List<String> {
        val packageName = service.rootInActiveWindow?.packageName?.toString() ?: return emptyList()
        return getAppCommands(packageName)
    }
    
    /**
     * Get commands for specific app
     */
    fun getAppCommands(packageName: String): List<String> {
        val pattern = appCommands[packageName] ?: return emptyList()
        return pattern.commands.keys.toList()
    }
    
    /**
     * Check if app has specific commands
     */
    fun hasAppCommands(packageName: String): Boolean {
        return appCommands.containsKey(packageName)
    }
    
    /**
     * Get command action for app
     */
    fun getCommandAction(packageName: String, command: String): CommandAction? {
        return appCommands[packageName]?.commands?.get(command.lowercase())
    }
    
    /**
     * Find app by partial name match
     */
    fun findAppByPartialName(partialName: String): List<Pair<String, String>> {
        val normalized = partialName.lowercase()
        val matches = mutableListOf<Pair<String, String>>()
        
        // Check common apps
        commonApps.forEach { (name, pkg) ->
            if (name.contains(normalized)) {
                matches.add(name to pkg)
            }
        }
        
        // Check installed apps
        val installedApps = packageManager.getInstalledPackages(0)
        installedApps.forEach { packageInfo ->
            val appName = packageInfo.applicationInfo.loadLabel(packageManager)
                .toString().lowercase()
            if (appName.contains(normalized)) {
                matches.add(appName to packageInfo.packageName)
            }
        }
        
        return matches.take(10) // Limit results
    }
    
    /**
     * Load built-in command patterns
     */
    private fun loadBuiltInPatterns() {
        // Chrome/Browser patterns
        appCommands["com.android.chrome"] = AppCommandPattern(
            packageName = "com.android.chrome",
            commands = mapOf(
                "new tab" to CommandAction("ACTION_NEW_TAB", description = "Open new tab"),
                "close tab" to CommandAction("ACTION_CLOSE_TAB", description = "Close current tab"),
                "refresh" to CommandAction("ACTION_REFRESH", description = "Refresh page"),
                "back" to CommandAction("ACTION_NAVIGATE_BACK", description = "Go back"),
                "forward" to CommandAction("ACTION_NAVIGATE_FORWARD", description = "Go forward"),
                "bookmarks" to CommandAction("ACTION_SHOW_BOOKMARKS", description = "Show bookmarks"),
                "history" to CommandAction("ACTION_SHOW_HISTORY", description = "Show history"),
                "incognito" to CommandAction("ACTION_NEW_INCOGNITO_TAB", description = "Open incognito tab")
            ),
            aliases = listOf("browser", "chrome")
        )
        
        // YouTube patterns
        appCommands["com.google.android.youtube"] = AppCommandPattern(
            packageName = "com.google.android.youtube",
            commands = mapOf(
                "play" to CommandAction("ACTION_PLAY", description = "Play video"),
                "pause" to CommandAction("ACTION_PAUSE", description = "Pause video"),
                "next" to CommandAction("ACTION_NEXT", description = "Next video"),
                "previous" to CommandAction("ACTION_PREVIOUS", description = "Previous video"),
                "fullscreen" to CommandAction("ACTION_FULLSCREEN", description = "Toggle fullscreen"),
                "subscribe" to CommandAction("ACTION_SUBSCRIBE", description = "Subscribe to channel"),
                "like" to CommandAction("ACTION_LIKE", description = "Like video"),
                "share" to CommandAction("ACTION_SHARE", description = "Share video")
            ),
            aliases = listOf("youtube")
        )
        
        // Gmail patterns
        appCommands["com.google.android.gm"] = AppCommandPattern(
            packageName = "com.google.android.gm",
            commands = mapOf(
                "compose" to CommandAction("ACTION_COMPOSE", description = "Compose email"),
                "reply" to CommandAction("ACTION_REPLY", description = "Reply to email"),
                "reply all" to CommandAction("ACTION_REPLY_ALL", description = "Reply to all"),
                "forward" to CommandAction("ACTION_FORWARD", description = "Forward email"),
                "archive" to CommandAction("ACTION_ARCHIVE", description = "Archive email"),
                "delete" to CommandAction("ACTION_DELETE", description = "Delete email"),
                "star" to CommandAction("ACTION_STAR", description = "Star email"),
                "mark as read" to CommandAction("ACTION_MARK_READ", description = "Mark as read"),
                "mark as unread" to CommandAction("ACTION_MARK_UNREAD", description = "Mark as unread")
            ),
            aliases = listOf("gmail", "email")
        )
        
        // Maps patterns
        appCommands["com.google.android.apps.maps"] = AppCommandPattern(
            packageName = "com.google.android.apps.maps",
            commands = mapOf(
                "directions" to CommandAction("ACTION_DIRECTIONS", description = "Get directions"),
                "navigate" to CommandAction("ACTION_NAVIGATE", description = "Start navigation"),
                "my location" to CommandAction("ACTION_MY_LOCATION", description = "Show my location"),
                "zoom in" to CommandAction("ACTION_ZOOM_IN", description = "Zoom in"),
                "zoom out" to CommandAction("ACTION_ZOOM_OUT", description = "Zoom out"),
                "satellite" to CommandAction("ACTION_SATELLITE_VIEW", description = "Satellite view"),
                "traffic" to CommandAction("ACTION_TRAFFIC", description = "Show traffic"),
                "explore" to CommandAction("ACTION_EXPLORE", description = "Explore nearby")
            ),
            aliases = listOf("maps", "navigation")
        )
        
        // WhatsApp patterns
        appCommands["com.whatsapp"] = AppCommandPattern(
            packageName = "com.whatsapp",
            commands = mapOf(
                "new chat" to CommandAction("ACTION_NEW_CHAT", description = "Start new chat"),
                "send" to CommandAction("ACTION_SEND", description = "Send message"),
                "voice message" to CommandAction("ACTION_VOICE_MESSAGE", description = "Record voice"),
                "video call" to CommandAction("ACTION_VIDEO_CALL", description = "Start video call"),
                "voice call" to CommandAction("ACTION_VOICE_CALL", description = "Start voice call"),
                "attach photo" to CommandAction("ACTION_ATTACH_PHOTO", description = "Attach photo"),
                "attach file" to CommandAction("ACTION_ATTACH_FILE", description = "Attach file"),
                "status" to CommandAction("ACTION_STATUS", description = "View status")
            ),
            aliases = listOf("whatsapp")
        )
    }
    
    /**
     * Find package by app name
     */
    private fun findPackageByName(appName: String): String? {
        val packages = packageManager.getInstalledPackages(0)
        val normalized = appName.lowercase().replace(" ", "")
        
        // Exact match first
        packages.firstOrNull { packageInfo ->
            val label = packageInfo.applicationInfo.loadLabel(packageManager)
                .toString().lowercase().replace(" ", "")
            label == normalized
        }?.let { return it.packageName }
        
        // Partial match
        packages.firstOrNull { packageInfo ->
            val label = packageInfo.applicationInfo.loadLabel(packageManager)
                .toString().lowercase().replace(" ", "")
            label.contains(normalized) || packageInfo.packageName.contains(normalized)
        }?.let { return it.packageName }
        
        return null
    }
    
    /**
     * Load custom commands from preferences
     */
    private fun loadCustomCommands() {
        try {
            val savedCommands = prefs.getStringSet(KEY_CUSTOM_COMMANDS, emptySet())
            savedCommands?.forEach { entry ->
                val parts = entry.split("|")
                if (parts.size == 2) {
                    customCommands[parts[0]] = parts[1]
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load custom commands", e)
        }
    }
    
    /**
     * Save custom commands to preferences
     */
    private fun saveCustomCommands() {
        try {
            val commandSet = customCommands.map { (trigger, action) ->
                "$trigger|$action"
            }.toSet()
            
            prefs.edit()
                .putStringSet(KEY_CUSTOM_COMMANDS, commandSet)
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save custom commands", e)
        }
    }
    
    /**
     * Launch app by command/name
     */
    suspend fun launchAppByCommand(appName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val packageName = getPackageName(appName)
            if (packageName == null) {
                Log.w(TAG, "Could not find package for app: $appName")
                return@withContext false
            }
            
            val launchIntent = service.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(launchIntent)
                Log.d(TAG, "Launched app: $appName ($packageName)")
                true
            } else {
                Log.w(TAG, "No launch intent for package: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app: $appName", e)
            false
        }
    }

    /**
     * Clear all custom commands
     */
    fun clearCustomCommands() {
        customCommands.clear()
        saveCustomCommands()
        Log.d(TAG, "Cleared all custom commands")
    }
    
    /**
     * Dispose manager
     */
    fun dispose() {
        Log.d(TAG, "Disposing AppCommandManager")
        saveCustomCommands()
    }
}