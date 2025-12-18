/**
 * VoiceCommandProcessor.kt - Voice command execution engine
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Modified: 2025-10-18 (Integrated CommandManager for static commands)
 */
package com.augmentalis.voiceoscore.scraping

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.commandmanager.models.Command
import com.augmentalis.commandmanager.models.CommandSource
import com.augmentalis.database.repositories.IScrapedAppRepository
import com.augmentalis.database.repositories.IScrapedElementRepository
import com.augmentalis.database.repositories.IGeneratedCommandRepository
import com.augmentalis.database.dto.ScrapedElementDTO
import com.augmentalis.database.dto.GeneratedCommandDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray

/**
 * Voice Command Processor
 *
 * Processes voice commands by:
 * 1. Identifying current app
 * 2. Matching command to generated commands in database
 * 3. Looking up UI element by hash
 * 4. Executing appropriate action
 * 5. Updating usage statistics
 *
 * Command Flow:
 * User: "click submit button"
 *   ↓
 * 1. Get current app hash (packageName + versionCode)
 * 2. Query database for matching command
 * 3. Retrieve element by ID
 * 4. Find actual UI node by hash
 * 5. Execute click action
 * 6. Increment usage count
 */
class VoiceCommandProcessor(
    private val context: Context,
    private val accessibilityService: AccessibilityService,
    private val scrapedAppRepository: IScrapedAppRepository,
    private val scrapedElementRepository: IScrapedElementRepository,
    private val generatedCommandRepository: IGeneratedCommandRepository
) {

    companion object {
        private const val TAG = "VoiceCommandProcessor"

        // Command matching thresholds
        private const val EXACT_MATCH_THRESHOLD = 1.0f
        private const val FUZZY_MATCH_THRESHOLD = 0.7f
    }

    private val packageManager: PackageManager = context.packageManager
    private val commandManager: CommandManager = CommandManager.getInstance(context)

    /**
     * Process a voice command
     *
     * @param voiceInput Raw voice input from speech recognition
     * @return CommandResult indicating success/failure and details
     */
    suspend fun processCommand(voiceInput: String): CommandResult = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Processing voice command: '$voiceInput'")

            // Normalize input
            val normalizedInput = voiceInput.lowercase().trim()

            // Get current app package name
            val currentPackage = getCurrentPackageName()
            if (currentPackage == null) {
                Log.w(TAG, "Could not determine current app package")
                return@withContext CommandResult.failure("Could not identify current app")
            }

            Log.d(TAG, "Current app package: $currentPackage")

            // Get app hash
            val appInfo = try {
                packageManager.getPackageInfo(currentPackage, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting package info for $currentPackage", e)
                return@withContext CommandResult.failure("Could not get app information")
            }

            val appHash = HashUtils.calculateAppHash(currentPackage, appInfo.versionCode)
            Log.d(TAG, "App hash: $appHash")

            // Check if app has been scraped
            val scrapedApp = scrapedAppRepository.getByPackage(currentPackage)
            if (scrapedApp == null) {
                Log.w(TAG, "App has not been scraped yet: $currentPackage")
                return@withContext CommandResult.failure("App not yet learned. Please wait for learning to complete.")
            }

            // Find matching command (dynamic app-specific commands)
            val matchedCommand = findMatchingCommand(scrapedApp.packageName, normalizedInput)
            if (matchedCommand == null) {
                Log.w(TAG, "No dynamic command found for: '$normalizedInput', trying static commands")

                // Try static commands from CommandManager
                return@withContext tryStaticCommand(normalizedInput, voiceInput)
            }

            Log.i(TAG, "Matched command: ${matchedCommand.commandText} (confidence: ${matchedCommand.confidence})")

            // Get associated element by hash (stable across sessions)
            val element = scrapedElementRepository.getByHash(matchedCommand.elementHash)
            if (element == null) {
                Log.w(TAG, "Element not found for command '${matchedCommand.commandText}' (hash=${matchedCommand.elementHash})")
                Log.w(TAG, "Element may no longer exist or UI has changed. Consider re-scraping.")
                return@withContext CommandResult.elementNotFound(
                    commandText = matchedCommand.commandText,
                    elementHash = matchedCommand.elementHash
                )
            }

            // Execute action on element
            val success = executeAction(element, matchedCommand.actionType)

            if (success) {
                // Update usage statistics
                generatedCommandRepository.incrementUsage(matchedCommand.id, System.currentTimeMillis())
                Log.i(TAG, "✓ Command executed successfully: ${matchedCommand.commandText}")

                CommandResult.success(
                    message = "Executed: ${matchedCommand.commandText}",
                    actionType = matchedCommand.actionType,
                    elementHash = element.elementHash
                )
            } else {
                Log.w(TAG, "✗ Action execution failed for: ${matchedCommand.commandText}")
                CommandResult.failure("Failed to execute action: ${matchedCommand.actionType}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error processing voice command", e)
            CommandResult.failure("Error: ${e.message}")
        }
    }

    /**
     * Find matching command in database using fuzzy matching
     */
    private suspend fun findMatchingCommand(packageName: String, input: String): GeneratedCommandDTO? {
        // Get all commands for the app
        val commands = generatedCommandRepository.getByPackage(packageName)

        Log.d(TAG, "Searching ${commands.size} commands for match")

        // Try exact match first
        for (command in commands) {
            if (command.commandText.equals(input, ignoreCase = true)) {
                Log.d(TAG, "Found exact match: ${command.commandText}")
                return command
            }

            // Check synonyms
            val synonyms = parseSynonyms(command.synonyms ?: "[]")
            for (synonym in synonyms) {
                if (synonym.equals(input, ignoreCase = true)) {
                    Log.d(TAG, "Found synonym match: $synonym -> ${command.commandText}")
                    return command
                }
            }
        }

        // Try fuzzy match (contains)
        for (command in commands) {
            if (input.contains(command.commandText, ignoreCase = true) ||
                command.commandText.contains(input, ignoreCase = true)) {
                Log.d(TAG, "Found fuzzy match: ${command.commandText}")
                return command
            }

            // Check synonyms for fuzzy match
            val synonyms = parseSynonyms(command.synonyms ?: "[]")
            for (synonym in synonyms) {
                if (input.contains(synonym, ignoreCase = true) ||
                    synonym.contains(input, ignoreCase = true)) {
                    Log.d(TAG, "Found fuzzy synonym match: $synonym -> ${command.commandText}")
                    return command
                }
            }
        }

        // No match found
        return null
    }

    /**
     * Try static system command from CommandManager
     *
     * Called when no dynamic app-specific command is found.
     * Attempts to execute global system commands like navigation, volume, etc.
     *
     * @param normalizedInput Normalized voice input
     * @param originalVoiceInput Original voice input for display
     * @return CommandResult from static command execution
     */
    private suspend fun tryStaticCommand(normalizedInput: String, originalVoiceInput: String): CommandResult {
        try {
            Log.d(TAG, "Trying static command: '$normalizedInput'")

            // Create Command object for CommandManager
            val command = Command(
                id = normalizedInput,  // Use normalized input as ID
                text = normalizedInput,
                source = CommandSource.VOICE,  // Voice command source
                confidence = 1.0f,  // Assume high confidence since already recognized by speech engine
                timestamp = System.currentTimeMillis()
            )

            // Execute through CommandManager
            val cmdResult = commandManager.executeCommand(command)

            return if (cmdResult.success) {
                Log.i(TAG, "✓ Static command executed successfully: $normalizedInput")
                CommandResult.success(
                    message = "Executed: $normalizedInput",
                    actionType = "static_command",
                    elementHash = null
                )
            } else {
                Log.w(TAG, "✗ Static command execution failed: ${cmdResult.error?.message}")
                CommandResult.failure("Command not recognized: '$originalVoiceInput'")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error executing static command", e)
            return CommandResult.failure("Command not recognized: '$originalVoiceInput'")
        }
    }

    /**
     * Execute action on UI element
     */
    private suspend fun executeAction(element: ScrapedElementDTO, actionType: String): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Executing action: $actionType on element: ${element.elementHash}")

            // Get root node of current window
            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                Log.e(TAG, "Root node is null, cannot execute action")
                return@withContext false
            }

            // Find target node by hash
            val targetNode = findNodeByHash(rootNode, element.elementHash)
            if (targetNode == null) {
                Log.e(TAG, "Target node not found by hash: ${element.elementHash}")
                rootNode.recycle()
                return@withContext false
            }

            Log.d(TAG, "Found target node: ${targetNode.className}")

            // Execute appropriate action
            val success = when (actionType) {
                "click" -> {
                    Log.d(TAG, "Performing click action")
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                }
                "long_click" -> {
                    Log.d(TAG, "Performing long click action")
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
                }
                "focus" -> {
                    Log.d(TAG, "Performing focus action")
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                }
                "scroll" -> {
                    Log.d(TAG, "Performing scroll forward action")
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
                }
                "type" -> {
                    // Note: Actual text input would require text parameter
                    Log.d(TAG, "Performing focus action for text input")
                    targetNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
                }
                else -> {
                    Log.w(TAG, "Unknown action type: $actionType")
                    false
                }
            }

            // Cleanup
            targetNode.recycle()
            rootNode.recycle()

            Log.d(TAG, "Action execution result: $success")
            return@withContext success

        } catch (e: Exception) {
            Log.e(TAG, "Error executing action", e)
            return@withContext false
        }
    }

    /**
     * Find node by hash in accessibility tree
     *
     * Uses recursive depth-first search
     */
    private fun findNodeByHash(node: AccessibilityNodeInfo, targetHash: String): AccessibilityNodeInfo? {
        try {
            // Calculate hash of current node
            // Extract package info for fingerprinting
            val packageName = node.packageName?.toString() ?: "unknown"
            val appVersion = try {
                packageManager.getPackageInfo(packageName, 0).versionCode.toString()
            } catch (e: Exception) {
                "0"
            }
            val nodeHash = com.augmentalis.uuidcreator.thirdparty.AccessibilityFingerprint.fromNode(node, packageName, appVersion).generateHash()

            if (nodeHash == targetHash) {
                Log.d(TAG, "Found matching node: ${node.className}")
                return node
            }

            // Search children
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                val found = findNodeByHash(child, targetHash)
                if (found != null) {
                    child.recycle()
                    return found
                }
                child.recycle()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error finding node by hash", e)
        }

        return null
    }

    /**
     * Get current foreground app package name
     */
    private fun getCurrentPackageName(): String? {
        return try {
            val rootNode = accessibilityService.rootInActiveWindow
            val packageName = rootNode?.packageName?.toString()
            rootNode?.recycle()
            packageName
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current package name", e)
            null
        }
    }

    /**
     * Parse synonyms from JSON array string
     */
    private fun parseSynonyms(synonymsJson: String): List<String> {
        return try {
            val jsonArray = JSONArray(synonymsJson)
            (0 until jsonArray.length()).map { jsonArray.getString(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing synonyms JSON", e)
            emptyList()
        }
    }

    /**
     * Execute text input action
     *
     * Separate method for text input with actual text parameter
     */
    suspend fun executeTextInput(voiceInput: String, text: String): CommandResult = withContext(Dispatchers.IO) {
        val result = processCommand(voiceInput)
        if (result.success && result.actionType == "type") {
            // Get the element and set text
            val elementHash = result.elementHash
            if (elementHash != null) {
                withContext(Dispatchers.Main) {
                    val element = scrapedElementRepository.getByHash(elementHash)
                    if (element != null) {
                        val rootNode = accessibilityService.rootInActiveWindow
                        val targetNode = rootNode?.let { findNodeByHash(it, element.elementHash) }
                        if (targetNode != null) {
                            val bundle = Bundle().apply {
                                putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
                            }
                            targetNode.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, bundle)
                            targetNode.recycle()
                            rootNode.recycle()
                        }
                    }
                }
            }
        }
        result
    }
}

/**
 * Command execution result
 */
data class CommandResult(
    val success: Boolean,
    val message: String,
    val actionType: String? = null,
    val elementHash: String? = null
) {
    companion object {
        fun success(message: String, actionType: String? = null, elementHash: String? = null) =
            CommandResult(true, message, actionType, elementHash)

        fun failure(message: String) =
            CommandResult(false, message)

        fun elementNotFound(commandText: String, elementHash: String) =
            CommandResult(
                success = false,
                message = "Element not found for command '$commandText'. UI may have changed.",
                actionType = null,
                elementHash = elementHash
            )
    }
}
