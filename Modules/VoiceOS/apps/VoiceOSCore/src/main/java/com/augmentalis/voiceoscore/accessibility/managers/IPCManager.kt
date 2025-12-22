/**
 * IPCManager.kt - Centralized inter-process communication handler
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA (IDEACODE v12.1)
 * Created: 2025-12-22
 *
 * Extracts IPC methods from VoiceOSService to follow Single Responsibility Principle.
 * Handles all external IPC calls from companion apps and services.
 */

package com.augmentalis.voiceoscore.accessibility.managers

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityService.*
import android.util.Log
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.speechrecognition.SpeechMode
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine
import com.augmentalis.voiceoscore.accessibility.extractors.UIScrapingEngine.UIElement
import com.augmentalis.voiceoscore.accessibility.speech.SpeechConfigurationData
import com.augmentalis.voiceoscore.accessibility.speech.SpeechEngineManager
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * IPC Manager
 *
 * Centralizes inter-process communication for VoiceOSService:
 * - Voice recognition control (start/stop)
 * - App learning triggers
 * - Screen scraping
 * - Database queries (learned apps, commands)
 * - Dynamic command registration
 * - Accessibility action execution
 *
 * @param accessibilityService The parent VoiceOSService
 * @param speechEngineManager Speech engine for voice recognition
 * @param uiScrapingEngine UI scraping engine for element extraction
 * @param databaseManager Database manager for command queries
 * @param isServiceReady Supplier function to check service readiness
 */
class IPCManager(
    private val accessibilityService: AccessibilityService,
    private val speechEngineManager: SpeechEngineManager,
    private val uiScrapingEngine: UIScrapingEngine,
    private val databaseManager: DatabaseManager,
    private val isServiceReady: () -> Boolean
) {

    companion object {
        private const val TAG = "IPCManager"
    }

    private val prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val compactGson: Gson = GsonBuilder().create()

    // ============================================================
    // Voice Recognition Control
    // ============================================================

    /**
     * Start voice recognition with specified configuration
     *
     * @param language Language code (e.g., "en-US")
     * @param recognizerType Type of recognizer ("continuous", "command", "static")
     * @return true if started successfully
     */
    fun startVoiceRecognition(language: String, recognizerType: String): Boolean {
        return try {
            Log.i(TAG, "IPC: startVoiceRecognition(language=$language, type=$recognizerType)")
            if (!isServiceReady()) {
                Log.w(TAG, "Service not ready, cannot start voice recognition")
                return false
            }

            val mode = when (recognizerType.lowercase()) {
                "continuous" -> SpeechMode.DYNAMIC_COMMAND  // Use DYNAMIC_COMMAND for continuous
                "command" -> SpeechMode.DYNAMIC_COMMAND
                "system" -> SpeechMode.DYNAMIC_COMMAND
                "static" -> SpeechMode.STATIC_COMMAND
                else -> {
                    Log.w(TAG, "Unknown recognizer type: $recognizerType, using DYNAMIC_COMMAND")
                    SpeechMode.DYNAMIC_COMMAND
                }
            }

            // Update speech configuration with new language
            speechEngineManager.updateConfiguration(
                SpeechConfigurationData(
                    language = language,
                    mode = mode,
                    enableVAD = true,
                    confidenceThreshold = 4000F,
                    maxRecordingDuration = 30000,
                    timeoutDuration = 5000,
                    enableProfanityFilter = false
                )
            )

            // Start listening
            speechEngineManager.startListening()
            Log.i(TAG, "Voice recognition started successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting voice recognition", e)
            false
        }
    }

    /**
     * Stop currently active voice recognition
     *
     * @return true if stopped successfully
     */
    fun stopVoiceRecognition(): Boolean {
        return try {
            Log.i(TAG, "IPC: stopVoiceRecognition()")
            if (!isServiceReady()) {
                Log.w(TAG, "Service not ready")
                return false
            }

            speechEngineManager.stopListening()
            Log.i(TAG, "Voice recognition stopped successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping voice recognition", e)
            false
        }
    }

    // ============================================================
    // App Learning and Screen Scraping
    // ============================================================

    /**
     * Trigger app learning for currently focused app
     *
     * @return JSON response with learning results or error
     */
    fun learnCurrentApp(): String {
        return try {
            Log.i(TAG, "IPC: learnCurrentApp()")
            if (!isServiceReady()) {
                Log.w(TAG, "Service not ready")
                return """{"error": "Service not ready"}"""
            }

            // Scrape current screen using existing engine
            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "No active window available for learning")
                return """{"error": "No active window"}"""
            }

            val packageName = rootNode.packageName?.toString() ?: "unknown"
            val elements = uiScrapingEngine.extractUIElements(null)
            rootNode.recycle()

            // Convert to JSON format
            val result = mapOf(
                "success" to true,
                "packageName" to packageName,
                "elementCount" to elements.size,
                "elements" to elements.take(50).map { element: UIElement ->  // Limit to 50 for performance
                    mapOf(
                        "text" to element.text,
                        "contentDescription" to (element.contentDescription ?: ""),
                        "className" to (element.className ?: ""),
                        "clickable" to element.isClickable,
                        "depth" to element.depth
                    )
                }
            )

            prettyGson.toJson(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error learning current app", e)
            """{"error": "${e.message}"}"""
        }
    }

    /**
     * Scrape current screen and return JSON representation
     *
     * @return JSON response with screen elements or error
     */
    fun scrapeScreen(): String {
        Log.i(TAG, "IPC: scrapeScreen()")
        return try {
            if (!isServiceReady()) {
                Log.w(TAG, "Service not ready")
                return """{"error": "Service not ready"}"""
            }

            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                Log.w(TAG, "No active window available for scraping")
                return """{"error": "No active window"}"""
            }

            val packageName = rootNode.packageName?.toString() ?: "unknown"
            val elements = uiScrapingEngine.extractUIElements(null)
            rootNode.recycle()

            // Convert to simplified JSON format
            val result = mapOf(
                "success" to true,
                "packageName" to packageName,
                "elementCount" to elements.size,
                "elements" to elements.take(100).map { element ->
                    mapOf(
                        "text" to element.text,
                        "normalizedText" to element.normalizedText,
                        "clickable" to element.isClickable,
                        "className" to (element.className ?: "")
                    )
                }
            )

            compactGson.toJson(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error scraping screen", e)
            """{"error": "${e.message}"}"""
        }
    }

    // ============================================================
    // Accessibility Action Execution
    // ============================================================

    /**
     * Execute accessibility action by action type string
     *
     * Maps action type names to GLOBAL_ACTION constants
     *
     * @param actionType Action name (e.g., "back", "home", "recents")
     * @return true if action executed successfully
     */
    fun executeAccessibilityActionByType(actionType: String): Boolean {
        Log.i(TAG, "IPC: executeAccessibilityActionByType(actionType=$actionType)")
        if (!isServiceReady()) {
            Log.w(TAG, "Service not ready")
            return false
        }

        val normalizedAction = actionType.lowercase().trim()
        return when (normalizedAction) {
            "back", "go back" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_BACK)
            "home", "go home" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_HOME)
            "recent", "recents", "recent apps" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_RECENTS)
            "notifications", "notification panel" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "settings", "quick settings" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "power", "power menu" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            "screenshot", "take screenshot" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT)
            "split screen", "split" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
            "lock", "lock screen" -> accessibilityService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
            else -> {
                Log.w(TAG, "Unknown action type: $actionType")
                false
            }
        }
    }

    // ============================================================
    // Database Query Methods
    // ============================================================

    /**
     * Get list of apps that have learned voice commands
     *
     * @return List of package names with learned commands
     */
    fun getLearnedApps(): List<String> {
        Log.i(TAG, "IPC: getLearnedApps()")
        return try {
            val database = databaseManager.scrapingDatabase
            if (database == null) {
                Log.w(TAG, "Scraping database not initialized")
                return emptyList()
            }

            // Query all apps from database
            runBlocking(Dispatchers.IO) {
                val apps = database.getInstalledApps()
                Log.d(TAG, "Found ${apps.size} learned apps")
                apps.map { it.packageName } // Return list of app IDs (package names)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying learned apps", e)
            emptyList()
        }
    }

    /**
     * Get voice commands available for specific app
     *
     * @param packageName Target app package name
     * @return List of command texts for the app
     */
    fun getCommandsForApp(packageName: String): List<String> {
        Log.i(TAG, "IPC: getCommandsForApp(packageName=$packageName)")
        return try {
            val database = databaseManager.scrapingDatabase
            if (database == null) {
                Log.w(TAG, "Scraping database not initialized")
                return emptyList()
            }

            // Query commands for app from database
            // TODO: Implement proper app-command filtering (requires join with elements table)
            runBlocking(Dispatchers.IO) {
                val commands = database.databaseManager.generatedCommands.getAll()
                Log.d(TAG, "Found ${commands.size} total commands (filtering by app not yet implemented)")
                commands.map { it.commandText } // Return list of command strings
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying commands for app $packageName", e)
            emptyList()
        }
    }

    /**
     * Register dynamic voice command at runtime
     *
     * @param commandText Voice command text
     * @param actionJson JSON containing action details (elementHash, actionType)
     * @return true if command registered successfully
     */
    fun registerDynamicCommand(commandText: String, actionJson: String): Boolean {
        return try {
            Log.i(TAG, "IPC: registerDynamicCommand(command=$commandText)")
            if (!isServiceReady()) {
                Log.w(TAG, "Service not ready")
                return false
            }

            val database = databaseManager.scrapingDatabase
            if (database == null) {
                Log.w(TAG, "Scraping database not initialized")
                return false
            }

            // Parse actionJson to extract target element hash and action type
            val actionData = try {
                prettyGson.fromJson(actionJson, Map::class.java) as? Map<String, Any> ?: emptyMap()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse actionJson", e)
                return false
            }

            // Create and insert command DTO
            runBlocking(Dispatchers.IO) {
                val timestamp = System.currentTimeMillis()
                val command = GeneratedCommandDTO(
                    id = 0L, // Auto-generated by SQLDelight
                    commandText = commandText.lowercase(),
                    elementHash = actionData["elementHash"] as? String ?: "",
                    actionType = actionData["actionType"] as? String ?: "click",
                    confidence = 1.0,
                    synonyms = "[]", // Empty JSON array
                    isUserApproved = 1L,
                    usageCount = 0L,
                    lastUsed = null,
                    createdAt = timestamp,
                    appId = ""  // Dynamic commands are global
                )

                database.databaseManager.generatedCommands.insert(command)
                Log.i(TAG, "Dynamic command registered successfully: $commandText")
            }

            // Note: Commands will be reloaded on next service restart
            Log.d(TAG, "Command registered. Restart VoiceOS to reload commands.")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering dynamic command", e)
            false
        }
    }
}
