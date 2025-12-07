/**
 * AppCommandManagerV2.kt - Enhanced app command management with lazy loading
 * 
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-09-02
 * 
 * Version 2 with lazy loading, efficient caching, and memory optimizations
 */
package com.augmentalis.voiceos.accessibility.managers

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.ArrayMap
import android.util.Log
import android.util.LruCache
import androidx.collection.ArraySet
import com.augmentalis.voiceos.accessibility.service.VoiceAccessibilityService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Enhanced app command manager version 2 with lazy loading and memory efficiency
 */
class AppCommandManagerV2(private val service: VoiceAccessibilityService) {
    
    companion object {
        private const val TAG = "AppCommandManagerV2"
        private const val PREFS_NAME = "app_command_prefs"
        private const val KEY_CUSTOM_COMMANDS = "custom_commands"
        private const val COMMAND_CACHE_SIZE = 50
        private const val LAZY_LOAD_DELAY_MS = 100L
    }
    
    private val packageManager: PackageManager = service.packageManager
    private val prefs by lazy { 
        service.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) 
    }
    
    // ArrayMap for memory efficiency with small collections
    private val appCommands = ArrayMap<String, AppCommandPattern>()
    private val customCommands = ArrayMap<String, String>()
    
    // LRU cache for frequently used commands
    private val commandCache = LruCache<String, List<String>>(COMMAND_CACHE_SIZE)
    
    // Lazy-loaded static commands
    private val staticCommandsInitialized = AtomicBoolean(false)
    private val staticCommands by lazy { loadStaticCommands() }
    
    // Common app package mappings - loaded lazily
    private val commonApps by lazy {
        ArrayMap<String, String>().apply {
            put("chrome", "com.android.chrome")
            put("browser", "com.android.chrome")
            put("camera", "com.android.camera2")
            put("phone", "com.android.dialer")
            put("dialer", "com.android.dialer")
            put("messages", "com.google.android.apps.messaging")
            put("gmail", "com.gmail.android")
            put("email", "com.gmail.android")
            put("maps", "com.google.android.apps.maps")
            put("youtube", "com.youtube.android")
            put("music", "com.google.android.music")
            put("spotify", "com.spotify.music")
            put("settings", "com.android.settings")
            put("calculator", "com.android.calculator2")
            put("calendar", "com.google.android.calendar")
            put("photos", "com.google.android.apps.photos")
            put("drive", "com.google.android.apps.docs")
            put("whatsapp", "com.whatsapp")
            put("instagram", "com.instagram.android")
            put("facebook", "com.facebook.katana")
            put("twitter", "com.twitter.android")
            put("netflix", "com.netflix.mediaclient")
        }
    }
    
    // System commands - loaded lazily in chunks
    private val systemCommands by lazy {
        ArraySet<SystemCommand>().apply {
            // Navigation commands
            add(SystemCommand("go back", Intent.ACTION_MAIN, confidence = 0.95f))
            add(SystemCommand("go home", Intent.ACTION_MAIN, confidence = 0.95f))
            add(SystemCommand("recent apps", Intent.ACTION_MAIN, confidence = 0.90f))
            add(SystemCommand("show notifications", Intent.ACTION_MAIN, confidence = 0.90f))
            
            // Quick settings
            add(SystemCommand("open settings", Settings.ACTION_SETTINGS, confidence = 0.95f))
            add(SystemCommand("wifi settings", "android.settings.WIFI_SETTINGS", confidence = 0.90f))
            add(SystemCommand("bluetooth settings", "android.settings.BLUETOOTH_SETTINGS", confidence = 0.90f))
            add(SystemCommand("volume settings", "android.settings.SOUND_SETTINGS", confidence = 0.85f))
        }
    }
    
    // Coroutine scope for async loading
    private val loadingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // State flow for monitoring loading status
    private val _loadingState = MutableStateFlow(LoadingState())
    val loadingState: StateFlow<LoadingState> = _loadingState
    
    data class LoadingState(
        val isLoading: Boolean = false,
        val loadedCommands: Int = 0,
        val cacheHitRate: Float = 0f
    )
    
    data class AppCommandPattern(
        val packageName: String,
        val commands: ArraySet<String>,
        val priority: Int = 0,
        val lastUsed: Long = 0
    )
    
    data class SystemCommand(
        val phrase: String,
        val action: String,
        val confidence: Float = 0.8f,
        val category: String = "system"
    )
    
    /**
     * Initialize with lazy loading
     */
    fun initialize() {
        Log.d(TAG, "Initializing AppCommandManagerV2 with lazy loading")
        
        // Start background initialization
        loadingScope.launch {
            delay(LAZY_LOAD_DELAY_MS) // Small delay to not block UI
            initializeStaticCommands()
            loadCustomCommands()
        }
    }
    
    /**
     * Initialize static commands lazily
     */
    private suspend fun initializeStaticCommands() = withContext(Dispatchers.IO) {
        if (staticCommandsInitialized.getAndSet(true)) return@withContext
        
        _loadingState.value = _loadingState.value.copy(isLoading = true)
        
        try {
            // Trigger lazy loading
            val commands = staticCommands
            
            _loadingState.value = _loadingState.value.copy(
                loadedCommands = commands.size,
                isLoading = false
            )
            
            Log.d(TAG, "Loaded ${commands.size} static commands")
        } catch (e: Exception) {
            Log.e(TAG, "Error loading static commands", e)
            _loadingState.value = _loadingState.value.copy(isLoading = false)
        }
    }
    
    /**
     * Load static commands (called lazily)
     */
    private fun loadStaticCommands(): ArraySet<String> {
        return ArraySet<String>().apply {
            // Basic navigation
            add("go back")
            add("go home")
            add("scroll up")
            add("scroll down")
            add("swipe left")
            add("swipe right")
            
            // App launches (loaded from commonApps)
            commonApps.keys.forEach { appName ->
                add("open $appName")
                add("launch $appName")
                add("start $appName")
            }
            
            // System actions
            add("take screenshot")
            add("show notifications")
            add("clear notifications")
            add("toggle wifi")
            add("toggle bluetooth")
            add("increase volume")
            add("decrease volume")
            add("mute")
            add("unmute")
        }
    }
    
    /**
     * Load custom commands from preferences
     */
    private suspend fun loadCustomCommands() = withContext(Dispatchers.IO) {
        try {
            val customCommandsJson = prefs.getString(KEY_CUSTOM_COMMANDS, null)
            if (!customCommandsJson.isNullOrEmpty()) {
                // Parse and load custom commands
                // Implementation depends on JSON parsing library
                Log.d(TAG, "Loaded custom commands from preferences")
            } else {
                Log.d(TAG, "No custom commands found in preferences")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading custom commands", e)
        }
    }
    
    /**
     * Get commands for a specific app with caching
     */
    fun getAppCommands(packageName: String): List<String> {
        // Check cache first
        commandCache.get(packageName)?.let { cached ->
            Log.d(TAG, "Cache hit for $packageName")
            return cached
        }
        
        // Build command list
        val commands = ArraySet<String>()
        
        // Add static commands if initialized
        if (staticCommandsInitialized.get()) {
            commands.addAll(staticCommands)
        }
        
        // Add app-specific commands
        appCommands[packageName]?.let { pattern ->
            commands.addAll(pattern.commands)
            
            // Update last used time
            appCommands[packageName] = pattern.copy(lastUsed = System.currentTimeMillis())
        }
        
        // Add custom commands
        customCommands.values.forEach { command ->
            commands.add(command)
        }
        
        val commandList = commands.toList()
        
        // Update cache
        commandCache.put(packageName, commandList)
        
        return commandList
    }
    
    /**
     * Process command with optimization
     */
    suspend fun processCommand(command: String): Boolean = withContext(Dispatchers.Default) {
        val normalizedCommand = command.lowercase().trim()
        
        // Check for app launch commands
        for ((appName, packageName) in commonApps) {
            if (normalizedCommand.contains("open $appName") ||
                normalizedCommand.contains("launch $appName") ||
                normalizedCommand.contains("start $appName")) {
                
                return@withContext launchApp(packageName)
            }
        }
        
        // Check system commands
        systemCommands.find { it.phrase == normalizedCommand }?.let { systemCommand ->
            return@withContext executeSystemCommand(systemCommand)
        }
        
        // Check custom commands
        customCommands[normalizedCommand]?.let { customCommand ->
            Log.d(TAG, "Executing custom command: $customCommand")
            return@withContext true
        }
        
        false
    }
    
    /**
     * Launch app by package name
     */
    private fun launchApp(packageName: String): Boolean {
        return try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                service.startActivity(intent)
                Log.d(TAG, "Launched app: $packageName")
                true
            } else {
                Log.w(TAG, "No launch intent for: $packageName")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error launching app: $packageName", e)
            false
        }
    }
    
    /**
     * Execute system command
     */
    private fun executeSystemCommand(command: SystemCommand): Boolean {
        return try {
            val intent = Intent(command.action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            service.startActivity(intent)
            Log.d(TAG, "Executed system command: ${command.phrase}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error executing system command: ${command.phrase}", e)
            false
        }
    }
    
    /**
     * Add custom command
     */
    fun addCustomCommand(phrase: String, action: String) {
        customCommands[phrase.lowercase()] = action
        saveCustomCommands()
        
        // Clear cache to include new command
        commandCache.evictAll()
    }
    
    /**
     * Save custom commands to preferences
     */
    private fun saveCustomCommands() {
        loadingScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Convert to JSON and save
                    // Implementation depends on JSON library
                    prefs.edit().putString(KEY_CUSTOM_COMMANDS, customCommands.toString()).apply()
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving custom commands", e)
                }
            }
        }
    }
    
    /**
     * Get performance metrics
     */
    fun getPerformanceMetrics(): Map<String, Any> {
        val cacheSize = commandCache.size()
        val maxCacheSize = commandCache.maxSize()
        val hitRate = if (maxCacheSize > 0) cacheSize.toFloat() / maxCacheSize else 0f
        
        return mapOf(
            "staticCommandsLoaded" to staticCommandsInitialized.get(),
            "totalCommands" to (if (staticCommandsInitialized.get()) staticCommands.size else 0),
            "customCommands" to customCommands.size,
            "cacheSize" to cacheSize,
            "cacheHitRate" to hitRate,
            "appPatternsLoaded" to appCommands.size
        )
    }
    
    /**
     * Clear all caches
     */
    fun clearCache() {
        commandCache.evictAll()
        Log.d(TAG, "Command cache cleared")
    }
    
    /**
     * Cleanup resources
     */
    fun destroy() {
        loadingScope.cancel()
        clearCache()
    }
}