/**
 * CommandDiscoveryManager.kt - Orchestrates voice command discovery system
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-12-08
 *
 * Coordinates all command discovery features:
 * - Visual overlay after exploration
 * - Audio summaries
 * - Command list UI
 * - Tutorial mode
 * - Contextual hints
 */

package com.augmentalis.voiceoscore.learnapp.ui.discovery

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.speech.tts.TextToSpeech
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import com.augmentalis.voiceoscore.learnapp.models.ElementInfo
import com.augmentalis.voiceoscore.learnapp.settings.LearnAppDeveloperSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

/**
 * Command Discovery Manager
 *
 * Manages the entire command discovery experience after exploration completes.
 *
 * ## Features:
 * 1. Visual overlay with command labels
 * 2. Audio summary of discovered commands
 * 3. Command list UI
 * 4. Interactive tutorial
 * 5. Contextual hints
 *
 * ## Usage:
 * ```kotlin
 * val manager = CommandDiscoveryManager(context, databaseManager)
 *
 * // After exploration completes
 * manager.onExplorationComplete(
 *     packageName = "com.example.app",
 *     sessionId = "session_123",
 *     elements = listOfDiscoveredElements
 * )
 * ```
 */
class CommandDiscoveryManager(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager
) {
    companion object {
        private const val TAG = "CommandDiscoveryMgr"
        private const val DEFAULT_OVERLAY_TIMEOUT_MS = 10_000L
        private const val TUTORIAL_ENABLED_KEY = "tutorial_enabled"
        private const val DISCOVERY_ENABLED_KEY = "discovery_enabled"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val developerSettings by lazy { LearnAppDeveloperSettings(context) }

    // Components
    private var discoveryOverlay: CommandDiscoveryOverlay? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    // Tutorial engine (lazy init)
    private val tutorialEngine by lazy {
        CommandTutorialEngine(context, databaseManager, textToSpeech)
    }

    init {
        initializeTTS()
    }

    /**
     * Initialize Text-to-Speech
     */
    private fun initializeTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ttsInitialized = true
                textToSpeech?.language = Locale.US
                Log.d(TAG, "TTS initialized successfully")
            } else {
                Log.e(TAG, "TTS initialization failed")
            }
        }
    }

    /**
     * Called when exploration completes
     *
     * Triggers the complete command discovery flow:
     * 1. Generate ElementWithCommand list from discovered elements
     * 2. Show visual overlay (auto-hide after timeout)
     * 3. Speak command summary
     * 4. Offer tutorial (if first time user)
     * 5. Show notification with command list link
     *
     * @param packageName Target app package name
     * @param sessionId Exploration session ID
     * @param elements List of discovered elements
     */
    suspend fun onExplorationComplete(
        packageName: String,
        sessionId: String,
        elements: List<ElementInfo>
    ) {
        Log.i(TAG, "Starting command discovery for session: $sessionId")

        try {
            // 1. Load commands from database
            val commands = loadCommandsForSession(sessionId, elements)
            if (commands.isEmpty()) {
                Log.w(TAG, "No commands found for session: $sessionId")
                speakMessage("No voice commands were discovered for this app.")
                return
            }

            Log.d(TAG, "Loaded ${commands.size} commands")

            // 2. Convert to ElementWithCommand for overlay
            val commandElements = convertToCommandElements(elements, commands)

            // 3. Show visual overlay (if enabled)
            if (isDiscoveryEnabled()) {
                showVisualOverlay(commandElements)
            }

            // 4. Speak audio summary
            speakCommandSummary(commands, commandElements)

            // 5. Offer tutorial (if enabled and first time)
            if (isTutorialEnabled() && isFirstTimeUser(packageName)) {
                delay(3000)  // Wait for summary to finish
                offerTutorial(packageName, commandElements)
            }

            // 6. Show notification with command list link
            showCommandListNotification(packageName)

        } catch (e: Exception) {
            Log.e(TAG, "Error in command discovery", e)
        }
    }

    /**
     * Load commands for exploration session
     *
     * Queries database for commands matching the discovered elements.
     */
    private suspend fun loadCommandsForSession(
        sessionId: String,
        elements: List<ElementInfo>
    ): List<GeneratedCommandDTO> {
        // Get all commands from database
        val allCommands = databaseManager.generatedCommands.getAllCommands()

        // Filter to commands matching discovered elements
        // Match by element hash
        val elementHashes = elements.map { calculateElementHash(it) }.toSet()
        return allCommands.filter { it.elementHash in elementHashes }
    }

    /**
     * Convert elements and commands to overlay format
     */
    private fun convertToCommandElements(
        elements: List<ElementInfo>,
        commands: List<GeneratedCommandDTO>
    ): List<ElementWithCommand> {
        // Create map of element hash -> command
        val commandMap = commands.associateBy { it.elementHash }

        // Convert elements with matching commands
        return elements.mapNotNull { element ->
            val hash = calculateElementHash(element)
            val command = commandMap[hash] ?: return@mapNotNull null

            ElementWithCommand(
                bounds = element.bounds,
                voiceCommand = command.commandText,
                confidence = command.confidence.toFloat(),
                elementType = command.actionType,
                isGenerated = true,  // All LearnApp commands are generated
                generationStrategy = determineGenerationStrategy(element, command),
                description = buildCommandDescription(element, command)
            )
        }
    }

    /**
     * Determine generation strategy used for label
     */
    private fun determineGenerationStrategy(
        element: ElementInfo,
        command: GeneratedCommandDTO
    ): String {
        return when {
            // Real label exists
            element.text.isNotBlank() || element.contentDescription.isNotBlank() -> "text"
            // Position-based (Tab 1, Button 2)
            command.commandText.matches(Regex(".*\\d+.*")) -> "position"
            // Context-aware (Top button, Center card)
            command.commandText.contains("top", ignoreCase = true) ||
                    command.commandText.contains("bottom", ignoreCase = true) ||
                    command.commandText.contains("center", ignoreCase = true) -> "spatial"
            else -> "fallback"
        }
    }

    /**
     * Build human-readable description
     */
    private fun buildCommandDescription(
        element: ElementInfo,
        command: GeneratedCommandDTO
    ): String? {
        return when (command.actionType.lowercase()) {
            "click" -> "Tap to activate"
            "type" -> "Text input field"
            "scroll" -> "Scrollable area"
            "long_click" -> "Long press to activate"
            else -> null
        }
    }

    /**
     * Show visual overlay with commands
     */
    private fun showVisualOverlay(commandElements: List<ElementWithCommand>) {
        scope.launch {
            try {
                // Create overlay if not exists
                if (discoveryOverlay == null) {
                    discoveryOverlay = CommandDiscoveryOverlay(context)
                }

                // Show commands
                discoveryOverlay?.showCommands(commandElements)
                discoveryOverlay?.showWithTimeout(DEFAULT_OVERLAY_TIMEOUT_MS)

                Log.d(TAG, "Showed overlay with ${commandElements.size} commands")

            } catch (e: Exception) {
                Log.e(TAG, "Failed to show overlay", e)
            }
        }
    }

    /**
     * Speak audio summary of discovered commands
     */
    private fun speakCommandSummary(
        commands: List<GeneratedCommandDTO>,
        commandElements: List<ElementWithCommand>
    ) {
        if (!ttsInitialized) {
            Log.w(TAG, "TTS not initialized, skipping audio summary")
            return
        }

        // Build summary message
        val summary = buildString {
            append("Learning complete! ")
            append("I found ${commands.size} voice commands. ")

            // Mention top 3 commands
            val topCommands = commandElements
                .sortedByDescending { it.confidence }
                .take(3)

            if (topCommands.isNotEmpty()) {
                append("For example, you can say: ")
                topCommands.forEachIndexed { index, cmd ->
                    append(cmd.voiceCommand)
                    if (index < topCommands.size - 1) {
                        append(", ")
                    }
                }
                append(". ")
            }

            append("Say 'Show commands' to see the full list, ")
            append("or 'Show commands on screen' to see labels on the app.")
        }

        speakMessage(summary)
    }

    /**
     * Speak message using TTS
     */
    private fun speakMessage(message: String) {
        if (ttsInitialized) {
            textToSpeech?.speak(message, TextToSpeech.QUEUE_FLUSH, null, "discovery_summary")
            Log.d(TAG, "Speaking: $message")
        }
    }

    /**
     * Offer interactive tutorial
     */
    private suspend fun offerTutorial(
        packageName: String,
        commandElements: List<ElementWithCommand>
    ) {
        val message = "Would you like to try a tutorial to practice these commands? Say 'Yes' to start."
        speakMessage(message)

        // TODO: Wait for voice response and start tutorial if accepted
        // For now, just log
        Log.d(TAG, "Tutorial offered for $packageName")
    }

    /**
     * Show notification with link to command list
     */
    private fun showCommandListNotification(packageName: String) {
        // TODO: Create notification with action to open CommandListActivity
        Log.d(TAG, "Would show notification for command list: $packageName")
    }

    /**
     * Show command list UI
     */
    fun showCommandList(packageName: String) {
        val intent = CommandListActivity.createIntent(context, packageName)
        context.startActivity(intent)
    }

    /**
     * Show/hide visual overlay (voice commands)
     */
    fun toggleOverlay() {
        discoveryOverlay?.let { overlay ->
            if (overlay.isVisible()) {
                overlay.hide()
                speakMessage("Voice commands hidden")
            } else {
                overlay.show()
                speakMessage("Showing voice commands")
            }
        }
    }

    /**
     * Start interactive tutorial
     */
    fun startTutorial(packageName: String) {
        scope.launch {
            tutorialEngine.startTutorial(packageName)
        }
    }

    /**
     * Get SharedPreferences for command discovery settings
     */
    private fun getPrefs() = context.getSharedPreferences("command_discovery", Context.MODE_PRIVATE)

    /**
     * Check if discovery is enabled
     */
    private fun isDiscoveryEnabled(): Boolean {
        return getPrefs().getBoolean(DISCOVERY_ENABLED_KEY, true)
    }

    /**
     * Check if tutorial is enabled
     */
    private fun isTutorialEnabled(): Boolean {
        return getPrefs().getBoolean(TUTORIAL_ENABLED_KEY, true)
    }

    /**
     * Check if first time user for this app
     */
    private fun isFirstTimeUser(packageName: String): Boolean {
        // Check if user has used commands for this app before
        val key = "tutorial_completed_$packageName"
        return !getPrefs().getBoolean(key, false)
    }

    /**
     * Mark tutorial as completed for app
     */
    fun markTutorialCompleted(packageName: String) {
        val key = "tutorial_completed_$packageName"
        getPrefs().edit().putBoolean(key, true).apply()
    }

    /**
     * Calculate element hash (same as LearnAppCore)
     */
    private fun calculateElementHash(element: ElementInfo): String {
        val fingerprint = buildString {
            append(element.className)
            append("|")
            append(element.resourceId)
            append("|")
            append(element.text)
            append("|")
            append(element.contentDescription)
            append("|")
            append(element.bounds.toString())
        }

        return try {
            val md = java.security.MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(fingerprint.toByteArray())
            hashBytes.joinToString("") { "%02x".format(it) }.take(12)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to calculate hash", e)
            fingerprint.hashCode().toString()
        }
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        discoveryOverlay?.dispose()
        discoveryOverlay = null

        if (ttsInitialized) {
            textToSpeech?.stop()
            textToSpeech?.shutdown()
            textToSpeech = null
            ttsInitialized = false
        }
    }
}
