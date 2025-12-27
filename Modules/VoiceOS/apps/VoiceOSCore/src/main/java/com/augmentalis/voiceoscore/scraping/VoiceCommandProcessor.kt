/**
 * VoiceCommandProcessor.kt - Voice command execution engine
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Code-Reviewed-By: CCA
 * Created: 2025-10-09
 * Modified: 2025-11-12 (YOLO FIX: Removed fuzzy matching, use real-time element search instead)
 */
package com.augmentalis.voiceoscore.scraping

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandManager
import com.augmentalis.voiceos.command.Command
import com.augmentalis.voiceos.command.CommandSource
import com.augmentalis.voiceoscore.commands.DatabaseCommandHandler
import com.augmentalis.voiceoscore.utils.ConditionalLogger
import com.augmentalis.voiceos.logging.PIILoggingWrapper
import com.augmentalis.voiceoscore.utils.forEachChild
import com.augmentalis.voiceoscore.utils.useNode
import com.augmentalis.voiceoscore.utils.useNodeOrNull
import com.augmentalis.voiceoscore.database.VoiceOSCoreDatabaseAdapter
import com.augmentalis.voiceoscore.database.toScrapedElementEntity
import com.augmentalis.voiceoscore.scraping.entities.ScrapedElementEntity
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.DatabaseDriverFactory
import com.augmentalis.voiceos.hash.HashUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
    private val accessibilityService: AccessibilityService
) {

    companion object {
        private const val TAG = "VoiceCommandProcessor"

        // Command matching thresholds
        private const val EXACT_MATCH_THRESHOLD = 1.0f
        private const val FUZZY_MATCH_THRESHOLD = 0.7f

        // FIX P2 (2025-12-27): Configurable timeout for command processing
        // Default 10 seconds to handle large databases and complex queries
        private const val DEFAULT_COMMAND_TIMEOUT_MS = 10_000L

        /**
         * Configurable timeout for command processing.
         * Can be adjusted based on device performance or database size.
         */
        @Volatile
        var commandTimeoutMs: Long = DEFAULT_COMMAND_TIMEOUT_MS
    }

    // FIX (2025-12-01): Replaced non-existent VoiceOSAppDatabase with VoiceOSCoreDatabaseAdapter
    // VoiceOSCoreDatabaseAdapter provides access to VoiceOSDatabaseManager for all database operations
    private val databaseAdapter: VoiceOSCoreDatabaseAdapter by lazy {
        VoiceOSCoreDatabaseAdapter.getInstance(context)
    }

    // Direct access to SQLDelight database manager for queries
    private val databaseManager: VoiceOSDatabaseManager
        get() = databaseAdapter.databaseManager
    private val packageManager: PackageManager = context.packageManager
    private val commandManager: CommandManager = CommandManager.getInstance(context)

    // CoT: Database command handler for voice commands (v4.1.1)
    // Handles 20 voice commands for database interaction (stats, migration, queries, management)
    // FIX (2025-12-01): Pass databaseAdapter instead of non-existent database
    private val databaseCommandHandler: DatabaseCommandHandler by lazy {
        DatabaseCommandHandler(context, databaseAdapter)
    }

    // FIX (2025-12-01): Tier 3 property-based element search engine
    // Part of Voice Command Element Persistence feature
    private val elementSearchEngine: ElementSearchEngine = ElementSearchEngine(accessibilityService)

    /**
     * Process a voice command
     *
     * FIX P1 (2025-12-27): Added database readiness check before processing
     * FIX P2 (2025-12-27): Using configurable timeout instead of hardcoded value
     * FIX P3 (2025-12-27): Improved error messaging to distinguish database vs command failures
     *
     * @param voiceInput Raw voice input from speech recognition
     * @return CommandResult indicating success/failure and details
     */
    suspend fun processCommand(voiceInput: String): CommandResult = withTimeout(commandTimeoutMs) {
        withContext(Dispatchers.IO) {
        try {
            // FIX P1 (2025-12-27): Verify database is ready before processing
            if (!databaseManager.isReady()) {
                Log.e(TAG, "Database not initialized - cannot process voice command")
                return@withContext CommandResult.databaseError(
                    "Database not ready. Please wait for initialization to complete."
                )
            }

            // PII Redaction: Sanitize user voice input before logging
            PIILoggingWrapper.d(TAG, "Processing voice command: '$voiceInput'")

            // Normalize input
            val normalizedInput = voiceInput.lowercase().trim()

            // Get current app package name
            val currentPackage = getCurrentPackageName()
            if (currentPackage == null) {
                ConditionalLogger.w(TAG) { "Could not determine current app package" }
                return@withContext CommandResult.failure("Could not identify current app")
            }

            ConditionalLogger.d(TAG) { "Current app package: $currentPackage" }

            // Get app hash
            val appInfo = try {
                packageManager.getPackageInfo(currentPackage, 0)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to retrieve package metadata for app '$currentPackage': Could not access PackageManager. " +
                    "Possible causes: app uninstalled, permission denied, or package metadata corrupted. " +
                    "Impact: Unable to determine app version and calculate app hash for command matching.", e)
                return@withContext CommandResult.failure("Could not get app information")
            }

            val appHash = HashUtils.calculateAppHash(currentPackage, appInfo.versionCode)
            ConditionalLogger.d(TAG) { "App hash: $appHash" }

            // Check if app has been scraped (now using SQLDelight)
            // FIX (2025-12-01): Use getById - appHash is the appId
            val scrapedApp = databaseManager.scrapedApps.getById(appHash)
            if (scrapedApp == null) {
                ConditionalLogger.w(TAG) { "App has not been scraped yet: $currentPackage - trying real-time fallback" }

                // YOLO FIX: Try real-time element search instead of failing immediately
                val realtimeResult = tryRealtimeElementSearch(voiceInput)
                if (realtimeResult.success) {
                    return@withContext realtimeResult
                }

                // If real-time search also fails, try database and static commands
                val databaseResult = databaseCommandHandler.handleCommand(normalizedInput)
                if (databaseResult != null) {
                    return@withContext CommandResult.success(
                        message = databaseResult,
                        actionType = "database_command",
                        elementHash = null
                    )
                }

                return@withContext tryStaticCommand(normalizedInput, voiceInput)
            }

            // Find matching command (dynamic app-specific commands)
            // FIX (2025-12-01): scrapedApp is now ScrapedAppDTO, not entity
            val matchedCommand = findMatchingCommand(scrapedApp.appId, normalizedInput)
            if (matchedCommand == null) {
                // PII Redaction: Sanitize normalized input before logging
                PIILoggingWrapper.w(TAG, "No dynamic command found for: '$normalizedInput', trying real-time element search")

                // CoT: Command Priority Order (v4.2.0 - YOLO FIX)
                // 1. Dynamic commands (app-specific, exact match) ✓ already tried above
                // 2. Real-time element search (searches accessibility tree by text/content-desc) ← NEW
                // 3. Database commands (VoiceOS-specific database queries)
                // 4. Static system commands (Android system operations)
                //
                // Rationale: Real-time search is more accurate than fuzzy matching.
                // Example: "Clear history" should find "Clear history" button, not "Clear" button.
                // This fix prevents wrong command execution due to partial fuzzy matches.

                // Try real-time element search (v4.2.0)
                val realtimeResult = tryRealtimeElementSearch(voiceInput)
                if (realtimeResult.success) {
                    ConditionalLogger.i(TAG) { "✓ Real-time element search succeeded" }
                    return@withContext realtimeResult
                }

                // Try database commands (v4.1.1)
                val databaseResult = databaseCommandHandler.handleCommand(normalizedInput)
                if (databaseResult != null) {
                    ConditionalLogger.i(TAG) { "✓ Database command executed successfully" }
                    return@withContext CommandResult.success(
                        message = databaseResult,
                        actionType = "database_command",
                        elementHash = null
                    )
                }

                // Try static commands from CommandManager
                ConditionalLogger.w(TAG) { "No database command found, trying static commands" }
                return@withContext tryStaticCommand(normalizedInput, voiceInput)
            }

            // PII Redaction: Sanitize command text before logging
            PIILoggingWrapper.i(TAG, "Matched command: ${matchedCommand.commandText} (confidence: ${matchedCommand.confidence})")

            // Get associated element by hash (stable across sessions)
            // FIX (2025-12-01): Use databaseManager directly instead of non-existent database.databaseManager
            val elementDto = databaseManager.scrapedElements.getByHash(matchedCommand.elementHash)
            if (elementDto == null) {
                // PII Redaction: Sanitize command text before logging
                PIILoggingWrapper.w(TAG, "Element not found for command '${matchedCommand.commandText}' (hash=${matchedCommand.elementHash})")
                ConditionalLogger.w(TAG) { "Element may no longer exist or UI has changed. Consider re-scraping." }
                return@withContext CommandResult.elementNotFound(
                    commandText = matchedCommand.commandText,
                    elementHash = matchedCommand.elementHash
                )
            }

            // Convert DTO to Entity
            val element = elementDto.toScrapedElementEntity()

            // Execute action on element
            val success = executeAction(element, matchedCommand.actionType)

            if (success) {
                // Update usage statistics
                // FIX (2025-12-01): Use databaseManager directly
                databaseManager.generatedCommands.incrementUsage(matchedCommand.id, System.currentTimeMillis())
                // PII Redaction: Sanitize command text before logging
                PIILoggingWrapper.i(TAG, "✓ Command executed successfully: ${matchedCommand.commandText}")

                CommandResult.success(
                    message = "Executed: ${matchedCommand.commandText}",
                    actionType = matchedCommand.actionType,
                    elementHash = element.elementHash
                )
            } else {
                // PII Redaction: Sanitize command text before logging
                PIILoggingWrapper.w(TAG, "✗ Action execution failed for: ${matchedCommand.commandText}")
                CommandResult.failure("Failed to execute action: ${matchedCommand.actionType}")
            }

        } catch (e: Exception) {
            // FIX P3 (2025-12-27): Distinguish database errors from command failures
            val errorType = when {
                e.message?.contains("database", ignoreCase = true) == true ||
                e.message?.contains("sql", ignoreCase = true) == true ||
                e is IllegalStateException && e.message?.contains("not initialized") == true -> "DATABASE_ERROR"
                e is kotlinx.coroutines.TimeoutCancellationException -> "TIMEOUT_ERROR"
                e.message?.contains("accessibility", ignoreCase = true) == true -> "ACCESSIBILITY_ERROR"
                else -> "COMMAND_ERROR"
            }

            Log.e(TAG, "[$errorType] Voice command processing failed: ${e::class.simpleName}. " +
                "Possible causes: database access failure, accessibility service disruption, timeout, or state inconsistency. " +
                "Impact: Command execution aborted; user will need to retry. Details: ${e.message}", e)

            when (errorType) {
                "DATABASE_ERROR" -> CommandResult.databaseError(
                    "Database error: ${e.message ?: "Unknown database error"}"
                )
                "TIMEOUT_ERROR" -> CommandResult.timeoutError(
                    "Command timed out after ${commandTimeoutMs}ms. Try again or simplify the command."
                )
                "ACCESSIBILITY_ERROR" -> CommandResult.accessibilityError(
                    "Accessibility service error: ${e.message ?: "Service unavailable"}"
                )
                else -> CommandResult.failure("Command failed: ${e.message}")
            }
        }
        }
    }

    /**
     * Find matching command in database using fuzzy matching
     *
     * IMPORTANT: Fuzzy matching can cause incorrect command execution.
     * Example: "Clear history" might match "Clear" if fuzzy matching returns first partial match.
     * Fix: Collect all matches, score them, and return best match. If best match score is low,
     * return null to trigger real-time element search instead.
     */
    // FIX (2025-12-01): Changed return type to GeneratedCommandDTO since we're using SQLDelight
    private suspend fun findMatchingCommand(appId: String, input: String): com.augmentalis.database.dto.GeneratedCommandDTO? {
        // Get all commands for the app via SQLDelight
        // FIX (2025-12-01): Get elements first, then get commands for each element
        // Commands are linked to elements, not directly to apps
        val elements = databaseManager.scrapedElements.getByApp(appId)
        val commands = elements.flatMap { element ->
            databaseManager.generatedCommands.getByElement(element.elementHash)
        }

        ConditionalLogger.d(TAG) { "Searching ${commands.size} commands for match" }

        // Try exact match first
        for (command in commands) {
            if (command.commandText.equals(input, ignoreCase = true)) {
                // PII Redaction: Sanitize command text before logging
                PIILoggingWrapper.d(TAG, "Found exact match: ${command.commandText}")
                return command
            }

            // Check synonyms
            val synonyms = parseSynonyms(command.synonyms ?: "[]")
            for (synonym in synonyms) {
                if (synonym.equals(input, ignoreCase = true)) {
                    // PII Redaction: Sanitize synonym and command text before logging
                    PIILoggingWrapper.d(TAG, "Found synonym match: $synonym -> ${command.commandText}")
                    return command
                }
            }
        }

        // YOLO FIX: Instead of fuzzy matching, return null to trigger real-time element search
        // Real-time search is more accurate than fuzzy matching for partial matches
        // Old behavior: fuzzy match would return "Clear" for "Clear history"
        // New behavior: real-time search finds "Clear history" button directly
        PIILoggingWrapper.d(TAG, "No exact match found for: '$input' - will try real-time element search")
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
            // PII Redaction: Sanitize normalized input before logging
            PIILoggingWrapper.d(TAG, "Trying static command: '$normalizedInput'")

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
                // PII Redaction: Sanitize normalized input before logging
                PIILoggingWrapper.i(TAG, "✓ Static command executed successfully: $normalizedInput")
                CommandResult.success(
                    message = "Executed: $normalizedInput",
                    actionType = "static_command",
                    elementHash = null
                )
            } else {
                Log.w(TAG, "✗ Static command execution failed: ${cmdResult.error?.message}")
                // PII Redaction: Sanitize original voice input before logging
                PIILoggingWrapper.w(TAG, "Command not recognized: '$originalVoiceInput'")
                CommandResult.failure("Command not recognized")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to execute static system command for input '$normalizedInput': CommandManager.executeCommand() threw exception. " +
                "Possible causes: command registry corrupted, permission denied, or service unavailable. " +
                "Impact: System command will not execute; fallback to user feedback or manual action. Details: ${e.message}", e)
            return CommandResult.failure("Command not recognized: '$originalVoiceInput'")
        }
    }

    /**
     * Execute action on UI element
     */
    private suspend fun executeAction(element: ScrapedElementEntity, actionType: String): Boolean = withContext(Dispatchers.Main) {
        try {
            Log.d(TAG, "Executing action: $actionType on element: ${element.elementHash}")

            // Get root node of current window
            val rootNode = accessibilityService.rootInActiveWindow
            if (rootNode == null) {
                Log.e(TAG, "Root node is null, cannot execute action")
                return@withContext false
            }

            // Find target node by hash
            var targetNode = findNodeByHash(rootNode, element.elementHash)
            if (targetNode == null) {
                Log.w(TAG, "Target node not found by hash: ${element.elementHash} - trying text-based fallback")

                // YOLO FIX: Try text-based fallback using element's text or content description
                val searchText = element.text ?: element.contentDescription
                if (!searchText.isNullOrBlank()) {
                    val nodes = findNodesByText(rootNode, searchText)
                    if (nodes.isNotEmpty()) {
                        targetNode = nodes.first()
                        Log.i(TAG, "✓ Found node via text fallback: $searchText")
                        // Cleanup other nodes
                        nodes.drop(1).forEach { it.recycle() }
                    }
                }

                if (targetNode == null) {
                    Log.e(TAG, "Target node not found by hash or text: ${element.elementHash}")
                    rootNode.recycle()
                    return@withContext false
                }
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
            Log.e(TAG, "Failed to execute UI action on element due to accessibility operation error: Unexpected exception during action dispatch. " +
                "Possible causes: node was recycled prematurely, accessibility service stopped, or window changed during action. " +
                "Impact: UI element action will not be performed; command execution returns false. Details: ${e.message}", e)
            return@withContext false
        }
    }

    /**
     * Find node by hash in accessibility tree
     *
     * Uses recursive depth-first search with safe resource management.
     * 
     * CRITICAL FIX (2025-11-13): Ensures all child nodes are recycled even when exceptions occur.
     * Previous implementation would leak AccessibilityNodeInfo instances on exception paths.
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

            // Search children with safe resource management
            // CRITICAL: Child nodes MUST be recycled in all code paths to prevent memory leaks
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue
                try {
                    val found = findNodeByHash(child, targetHash)
                    if (found != null) {
                        return found
                    }
                } finally {
                    // ALWAYS recycle child, even if exception occurred
                    child.recycle()
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Failed to search accessibility tree for node matching hash '$targetHash': Exception during tree traversal. " +
                "Possible causes: node recycled during traversal, tree structure changed, or hash calculation mismatch. " +
                "Impact: Target UI element will not be found; command execution will fail. Details: ${e.message}", e)
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
            Log.e(TAG, "Failed to retrieve foreground app package name from accessibility service: Cannot access root window node. " +
                "Possible causes: accessibility service not initialized, window focus lost, or framework error. " +
                "Impact: Current app cannot be identified; voice command processing cannot begin. Details: ${e.message}", e)
            null
        }
    }

    /**
     * Parse synonyms from JSON array string
     */
    private fun parseSynonyms(synonymsJson: String): List<String> {
        return try {
            com.augmentalis.voiceos.json.JsonConverters.parseSynonyms(synonymsJson)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse synonyms JSON array: Invalid JSON format or structure. " +
                "Possible causes: corrupted database entry, invalid JSON serialization, or schema mismatch. " +
                "Impact: Synonyms will not be available for command matching; fuzzy matching will be less effective. " +
                "Details: ${e.message}", e)
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
                    // FIX (2025-12-01): Use databaseManager directly
                    val element = databaseManager.scrapedElements.getByHash(elementHash)
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

    // ==================== DYNAMIC COMMAND FALLBACK MECHANISM ====================
    // YOLO FIX: Multi-tier fallback for voice commands when hash-based lookup fails

    /**
     * Tier 3: Property-based element search
     *
     * FIX (2025-12-01): New Tier 3 search using ElementSearchEngine.
     * Searches by viewId, bounds+text, class+description before falling back to text-only.
     *
     * @param voiceInput Original voice input (e.g., "click submit button")
     * @return CommandResult indicating success/failure
     */
    private suspend fun tryPropertyBasedSearch(voiceInput: String): CommandResult {
        try {
            val (action, target) = parseVoiceCommand(voiceInput)
            if (target.isBlank()) {
                return CommandResult.failure("Could not extract target from command")
            }

            Log.d(TAG, "Tier 3: Property-based search for target='$target', action='$action'")

            // Create search criteria from target text
            val criteria = ElementSearchCriteria.fromVoiceTarget(target)

            // Search using ElementSearchEngine (priority: viewId > bounds+text > class+desc > text)
            val node = elementSearchEngine.findElement(criteria)

            if (node != null) {
                try {
                    val success = executeActionOnNode(node, action)
                    if (success) {
                        Log.i(TAG, "✓ Tier 3 (property-based) SUCCESS: '$voiceInput'")
                        return CommandResult.success(
                            message = "Executed: $voiceInput (tier-3)",
                            actionType = action
                        )
                    }
                } finally {
                    node.recycle()
                }
            }

            Log.d(TAG, "Tier 3: No match found, falling back to text-based search")
            return CommandResult.failure("No property match found")
        } catch (e: Exception) {
            Log.e(TAG, "Tier 3 search error", e)
            return CommandResult.failure("Tier 3 search failed: ${e.message}")
        }
    }

    /**
     * Try real-time element search when hash-based lookup fails
     *
     * 3-Tier real-time fallback strategy:
     * - Tier 3: Property-based search (viewId > bounds+text > class+desc) - <20ms
     * - Tier 4: Text-based search (Android API + recursive) - <50ms
     *
     * FIX (2025-11-13): Now uses extension functions for safe node lifecycle management.
     * Prevents memory leaks and ensures proper cleanup even on exceptions.
     *
     * FIX (2025-12-01): Now tries Tier 3 property-based search FIRST before text fallback.
     *
     * @param voiceInput Original voice input (e.g., "click submit button")
     * @return CommandResult indicating success/failure
     */
    private suspend fun tryRealtimeElementSearch(voiceInput: String): CommandResult = withContext(Dispatchers.Main) {
        try {
            // FIX (2025-12-01): Try Tier 3 property-based search first (faster, more reliable)
            val tier3Result = tryPropertyBasedSearch(voiceInput)
            if (tier3Result.success) {
                return@withContext tier3Result
            }
            Log.d(TAG, "Tier 3 failed, falling back to Tier 4 text-based search")

            // Parse voice command into action and target first
            val (action, target) = parseVoiceCommand(voiceInput)

            if (target.isBlank()) {
                Log.w(TAG, "Real-time search: Could not extract target from command: $voiceInput")
                return@withContext CommandResult.failure("Could not extract target from command")
            }

            ConditionalLogger.d(TAG) { "Real-time search: action=$action, target=$target" }

            // FIX (2025-11-24): Search ALL windows including dialogs/popups
            // Issue: Previous code only searched rootInActiveWindow, missing dialog windows
            // Solution: Try main window first, then search all windows if not found

            var matchedNodes = mutableListOf<AccessibilityNodeInfo>()

            // Strategy 1: Search main active window first (fastest, most common case)
            accessibilityService.rootInActiveWindow.useNodeOrNull { rootNode ->
                matchedNodes.addAll(findNodesByText(rootNode, target))
            }

            // Strategy 2: If not found in main window, search ALL windows (dialogs, popups, overlays)
            if (matchedNodes.isEmpty()) {
                ConditionalLogger.d(TAG) { "Real-time search: Not found in main window, searching all windows..." }

                val windows = accessibilityService.windows
                ConditionalLogger.d(TAG) { "Real-time search: Found ${windows.size} windows to search" }

                for (window in windows) {
                    try {
                        window.root?.let { windowRoot ->
                            val windowMatches = findNodesByText(windowRoot, target)
                            if (windowMatches.isNotEmpty()) {
                                ConditionalLogger.d(TAG) { "Real-time search: Found ${windowMatches.size} matches in window type=${window.type}, layer=${window.layer}" }
                                matchedNodes.addAll(windowMatches)
                                // Don't recycle windowRoot - it's owned by window
                            }
                        }
                    } catch (e: Exception) {
                        ConditionalLogger.e(TAG, e) { "Error searching window: ${window.type}" }
                    }
                }
            }

            ConditionalLogger.d(TAG) { "Real-time search: Found ${matchedNodes.size} total potential matches for '$target'" }

            // Execute action on best match if found
            if (matchedNodes.isNotEmpty()) {
                val bestMatch = matchedNodes.first()
                ConditionalLogger.d(TAG) { "Real-time search: Attempting action '$action' on best match" }

                val success = executeActionOnNode(bestMatch, action)

                // Cleanup matched nodes (caller's responsibility per findNodesByText contract)
                matchedNodes.forEach { it.recycle() }

                if (success) {
                    Log.i(TAG, "✓ Real-time element search succeeded: $voiceInput")
                    return@withContext CommandResult.success(
                        message = "Executed: $voiceInput (real-time)",
                        actionType = action
                    )
                } else {
                    Log.w(TAG, "✗ Real-time search: Action execution failed for: $voiceInput")
                    return@withContext CommandResult.failure("Action execution failed")
                }
            } else {
                Log.w(TAG, "✗ Real-time search: No matching elements found for '$target'")
                return@withContext CommandResult.failure("Element not found in real-time search")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in real-time element search", e)
            return@withContext CommandResult.failure("Real-time search failed: ${e.message}")
        }
    }

    /**
     * Parse voice command into action and target
     *
     * Examples:
     * - "click submit button" → ("click", "submit button")
     * - "tap login" → ("click", "login")
     * - "long press menu" → ("long_click", "menu")
     */
    private fun parseVoiceCommand(voiceInput: String): Pair<String, String> {
        val normalized = voiceInput.lowercase().trim()

        // Determine action
        val action = when {
            normalized.startsWith("click ") || normalized.startsWith("tap ") -> "click"
            normalized.startsWith("long press ") || normalized.startsWith("hold ") -> "long_click"
            normalized.startsWith("scroll ") -> "scroll"
            normalized.startsWith("focus ") -> "focus"
            else -> "click" // default action
        }

        // Extract target (everything after action keyword)
        val target = normalized
            .removePrefix("click ")
            .removePrefix("tap ")
            .removePrefix("long press ")
            .removePrefix("hold ")
            .removePrefix("scroll ")
            .removePrefix("focus ")
            .trim()

        return Pair(action, target)
    }

    /**
     * Find nodes by text or content description
     *
     * IMPORTANT: Caller is responsible for recycling all nodes in the returned list.
     *
     * Search Strategy:
     * 1. Try Android's built-in exact text search first (fast)
     * 2. If no results, fall back to recursive partial match search (slower but more flexible)
     *
     * FIX (2025-11-13): Added detailed logging for debugging search issues.
     *
     * @param root Root node to search from (NOT recycled by this function)
     * @param searchText Text to search for (case-insensitive)
     * @return List of matching nodes (CALLER MUST RECYCLE THESE)
     */
    private fun findNodesByText(root: AccessibilityNodeInfo, searchText: String): List<AccessibilityNodeInfo> {
        val results = mutableListOf<AccessibilityNodeInfo>()
        val startTime = System.currentTimeMillis()

        try {
            // Try Android's built-in text search first (exact match)
            val exactMatches = root.findAccessibilityNodeInfosByText(searchText)
            if (!exactMatches.isNullOrEmpty()) {
                results.addAll(exactMatches)
                ConditionalLogger.d(TAG) { "Real-time search: Found ${exactMatches.size} exact matches via Android API in ${System.currentTimeMillis() - startTime}ms" }
            }

            // If no exact matches, search recursively for partial matches
            if (results.isEmpty()) {
                ConditionalLogger.d(TAG) { "Real-time search: No exact matches, trying recursive partial search..." }
                searchNodeRecursively(root, searchText, results)
                ConditionalLogger.d(TAG) { "Real-time search: Recursive search found ${results.size} partial matches in ${System.currentTimeMillis() - startTime}ms" }
            }

        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error finding nodes by text: searchText='$searchText'" }
        }

        ConditionalLogger.d(TAG) { "Real-time search: Total ${results.size} nodes found for '$searchText' in ${System.currentTimeMillis() - startTime}ms" }
        return results
    }

    /**
     * Recursively search for nodes matching text
     *
     * CRITICAL FIX (2025-11-13): Now uses forEachChild extension for safe child iteration.
     *
     * Previous Bug: Manual recycling check `if (child !in results)` was always true because
     * child nodes are never directly added to results - only their descendants are.
     * This caused ALL children to be recycled prematurely, breaking tree traversal.
     *
     * New Approach: forEachChild extension handles recycling automatically.
     * Children are recycled after processing regardless of whether descendants were added.
     * This is correct because only leaf nodes (actual matches) are added to results.
     *
     * NOTE: Caller must recycle all nodes in the results list.
     *
     * @param node Current node being searched
     * @param searchText Text to search for (case-insensitive)
     * @param results Mutable list to accumulate matching nodes
     */
    private fun searchNodeRecursively(
        node: AccessibilityNodeInfo,
        searchText: String,
        results: MutableList<AccessibilityNodeInfo>
    ) {
        try {
            val text = node.text?.toString()?.lowercase() ?: ""
            val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""
            val searchLower = searchText.lowercase()

            // Check if this node matches
            if ((text.contains(searchLower) || contentDesc.contains(searchLower)) &&
                (node.isClickable || node.isFocusable)) {
                results.add(node)
                ConditionalLogger.v(TAG) { "Real-time search: Match found - text='$text', contentDesc='$contentDesc', clickable=${node.isClickable}" }
            }

            // Search children with automatic safe recycling
            // FIX: Use forEachChild extension - handles recycling correctly
            // Each child is recycled after recursion, regardless of matches found
            node.forEachChild { child ->
                searchNodeRecursively(child, searchText, results)
            }

        } catch (e: Exception) {
            ConditionalLogger.e(TAG, e) { "Error in recursive node search" }
        }
    }

    /**
     * Execute action on specific node (not by hash)
     *
     * FIX (2025-11-23): Enhanced with fallback strategies for robust real-time commands
     * - Strategy 1: Try direct action on node
     * - Strategy 2: Try focusing first, then action
     * - Strategy 3: Try scrolling into view, then action
     * - Strategy 4: Try action on parent node (if node not clickable)
     * - Strategy 5: Try action on parent's parent (2 levels up)
     *
     * This fixes "display size and text" failing in Settings and bottom nav items
     *
     * @param node The node to perform action on
     * @param actionType Action to perform (click, long_click, etc.)
     * @return true if action succeeded
     */
    private fun executeActionOnNode(node: AccessibilityNodeInfo, actionType: String): Boolean {
        val action = when (actionType) {
            "click" -> AccessibilityNodeInfo.ACTION_CLICK
            "long_click" -> AccessibilityNodeInfo.ACTION_LONG_CLICK
            "focus" -> AccessibilityNodeInfo.ACTION_FOCUS
            "scroll" -> AccessibilityNodeInfo.ACTION_SCROLL_FORWARD
            else -> return false
        }

        return try {
            // Strategy 1: Try direct action
            if (node.performAction(action)) {
                Log.d(TAG, "✓ Real-time: Direct $actionType succeeded")
                return true
            }

            // Strategy 2: Try focusing first, then action
            if (node.isFocusable && !node.isFocused) {
                if (node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)) {
                    Thread.sleep(100) // Brief delay for focus to settle
                    if (node.performAction(action)) {
                        Log.d(TAG, "✓ Real-time: $actionType succeeded after focus")
                        return true
                    }
                }
            }

            // Strategy 3: Try scrolling into view first
            if (node.isVisibleToUser) {
                // FIX: ACTION_SHOW_ON_SCREEN is not a constant on AccessibilityNodeInfo
                // It's an AccessibilityAction introduced in API 23+
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    if (node.performAction(android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction.ACTION_SHOW_ON_SCREEN.id)) {
                        Thread.sleep(100) // Brief delay for scroll animation
                        if (node.performAction(action)) {
                            Log.d(TAG, "✓ Real-time: $actionType succeeded after scroll-into-view")
                            return true
                        }
                    }
                }
            }

            // Strategy 4: Try action on parent node (if current node not clickable)
            val parent = node.parent
            if (parent != null && parent.isClickable) {
                try {
                    if (parent.performAction(action)) {
                        Log.d(TAG, "✓ Real-time: $actionType succeeded on parent node")
                        return true
                    }
                } finally {
                    parent.recycle()
                }
            }

            // Strategy 5: Try action on grandparent node (2 levels up)
            val grandparent = node.parent?.parent
            if (grandparent != null && grandparent.isClickable) {
                try {
                    if (grandparent.performAction(action)) {
                        Log.d(TAG, "✓ Real-time: $actionType succeeded on grandparent node")
                        return true
                    }
                } finally {
                    grandparent.recycle()
                }
            }

            // All strategies failed
            Log.w(TAG, "✗ Real-time: All $actionType strategies failed. Node details: " +
                    "clickable=${node.isClickable}, focusable=${node.isFocusable}, " +
                    "visible=${node.isVisibleToUser}, enabled=${node.isEnabled}, " +
                    "bounds=${node.getBoundsInScreen(android.graphics.Rect())}")
            false

        } catch (e: Exception) {
            Log.e(TAG, "Error executing $actionType on node: ${e.message}", e)
            false
        }
    }

    // ==================== END FALLBACK MECHANISM ====================
}

/**
 * Command execution result
 *
 * FIX P3 (2025-12-27): Added error type classification for better error handling
 */
data class CommandResult(
    val success: Boolean,
    val message: String,
    val actionType: String? = null,
    val elementHash: String? = null,
    val suggestions: String? = null,  // Element suggestions for failed commands
    val errorType: ErrorType? = null  // FIX P3: Error classification
) {
    /**
     * Error type classification for distinguishing failure causes
     */
    enum class ErrorType {
        NONE,              // No error (success)
        COMMAND_NOT_FOUND, // Command not recognized
        ELEMENT_NOT_FOUND, // UI element not found
        DATABASE_ERROR,    // Database initialization or query failure
        TIMEOUT_ERROR,     // Command processing timeout
        ACCESSIBILITY_ERROR, // Accessibility service error
        GENERAL_ERROR      // Other errors
    }

    companion object {
        fun success(message: String, actionType: String? = null, elementHash: String? = null) =
            CommandResult(true, message, actionType, elementHash, null, ErrorType.NONE)

        fun failure(message: String, suggestions: String? = null) =
            CommandResult(false, message, null, null, suggestions, ErrorType.GENERAL_ERROR)

        fun elementNotFound(commandText: String, elementHash: String) =
            CommandResult(
                success = false,
                message = "Element not found for command '$commandText'. UI may have changed.",
                actionType = null,
                elementHash = elementHash,
                suggestions = null,
                errorType = ErrorType.ELEMENT_NOT_FOUND
            )

        // FIX P3 (2025-12-27): New factory methods for specific error types

        /**
         * Create result for database errors (initialization failure, query error)
         */
        fun databaseError(message: String) =
            CommandResult(
                success = false,
                message = message,
                actionType = null,
                elementHash = null,
                suggestions = "Try waiting for database initialization or restart the app.",
                errorType = ErrorType.DATABASE_ERROR
            )

        /**
         * Create result for timeout errors
         */
        fun timeoutError(message: String) =
            CommandResult(
                success = false,
                message = message,
                actionType = null,
                elementHash = null,
                suggestions = "Try a simpler command or check if the app is responding.",
                errorType = ErrorType.TIMEOUT_ERROR
            )

        /**
         * Create result for accessibility service errors
         */
        fun accessibilityError(message: String) =
            CommandResult(
                success = false,
                message = message,
                actionType = null,
                elementHash = null,
                suggestions = "Check if VoiceOS accessibility service is enabled in settings.",
                errorType = ErrorType.ACCESSIBILITY_ERROR
            )
    }
}
