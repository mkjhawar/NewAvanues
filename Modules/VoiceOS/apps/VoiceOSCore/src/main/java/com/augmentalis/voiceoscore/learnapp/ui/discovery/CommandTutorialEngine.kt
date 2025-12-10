/**
 * CommandTutorialEngine.kt - Interactive voice command tutorial
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VoiceOS Development Team
 * Created: 2025-12-08
 *
 * Interactive tutorial that walks user through voice commands step-by-step:
 * - Highlights each element
 * - Speaks command
 * - Waits for user to try
 * - Provides feedback
 */

package com.augmentalis.voiceoscore.learnapp.ui.discovery

import android.content.Context
import android.graphics.Rect
import android.speech.tts.TextToSpeech
import android.util.Log
import com.augmentalis.database.VoiceOSDatabaseManager
import com.augmentalis.database.dto.GeneratedCommandDTO
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Tutorial step state
 */
enum class TutorialStepState {
    INTRODUCTION,    // Tutorial intro
    DEMONSTRATING,   // Showing command
    WAITING,         // Waiting for user to try
    SUCCESS,         // User succeeded
    RETRY,           // User needs to retry
    COMPLETED        // Tutorial finished
}

/**
 * Tutorial step
 */
data class TutorialStep(
    val stepNumber: Int,
    val totalSteps: Int,
    val command: GeneratedCommandDTO,
    val state: TutorialStepState = TutorialStepState.DEMONSTRATING,
    val attemptCount: Int = 0,
    val maxAttempts: Int = 3
) {
    fun canRetry(): Boolean = attemptCount < maxAttempts
}

/**
 * Command Tutorial Engine
 *
 * Provides interactive step-by-step tutorial for voice commands.
 *
 * ## Tutorial Flow:
 * 1. Introduction: "I found 12 voice commands. Let's try them together."
 * 2. For each command:
 *    a. Highlight element
 *    b. Speak: "Try saying: Tab 1"
 *    c. Wait for user to speak command
 *    d. Provide feedback (success/retry)
 * 3. Completion: "Tutorial complete! You've mastered all commands."
 *
 * ## Usage:
 * ```kotlin
 * val tutorial = CommandTutorialEngine(context, databaseManager, textToSpeech)
 *
 * // Start tutorial for app
 * tutorial.startTutorial("com.example.app")
 *
 * // User spoke command
 * tutorial.onUserSpoke("tab 1")
 *
 * // Skip to next
 * tutorial.skipCurrentStep()
 *
 * // Stop tutorial
 * tutorial.stopTutorial()
 * ```
 */
class CommandTutorialEngine(
    private val context: Context,
    private val databaseManager: VoiceOSDatabaseManager,
    private val textToSpeech: TextToSpeech? = null
) {
    companion object {
        private const val TAG = "CommandTutorial"
        private const val MAX_TUTORIAL_COMMANDS = 10  // Limit tutorial length
        private const val STEP_TIMEOUT_MS = 30000L    // 30 seconds per step
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State
    private val _currentStep = MutableStateFlow<TutorialStep?>(null)
    val currentStep: StateFlow<TutorialStep?> = _currentStep.asStateFlow()

    private val _isActive = MutableStateFlow(false)
    val isActive: StateFlow<Boolean> = _isActive.asStateFlow()

    private var tutorialCommands: List<GeneratedCommandDTO> = emptyList()
    private var currentStepIndex = 0
    private var stepTimeoutJob: Job? = null

    // Overlay for highlighting elements
    private var highlightOverlay: CommandDiscoveryOverlay? = null

    /**
     * Start tutorial for app
     *
     * @param packageName Target app package name
     */
    suspend fun startTutorial(packageName: String) {
        if (_isActive.value) {
            Log.w(TAG, "Tutorial already active")
            return
        }

        try {
            Log.i(TAG, "Starting tutorial for: $packageName")

            // Load commands
            tutorialCommands = loadTutorialCommands(packageName)
            if (tutorialCommands.isEmpty()) {
                Log.w(TAG, "No commands for tutorial")
                speakMessage("No voice commands available for this app.")
                return
            }

            // Initialize
            _isActive.value = true
            currentStepIndex = 0

            // Create highlight overlay
            if (highlightOverlay == null) {
                highlightOverlay = CommandDiscoveryOverlay(context)
            }

            // Start tutorial
            playIntroduction()
            delay(3000)  // Wait for intro
            proceedToNextStep()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to start tutorial", e)
            stopTutorial()
        }
    }

    /**
     * Load commands for tutorial (ranked by usefulness)
     */
    private suspend fun loadTutorialCommands(packageName: String): List<GeneratedCommandDTO> {
        val allCommands = databaseManager.generatedCommands.getAllCommands()

        // Rank by confidence and usage
        return allCommands
            .sortedWith(
                compareByDescending<GeneratedCommandDTO> { it.confidence }
                    .thenByDescending { it.usageCount }
            )
            .take(MAX_TUTORIAL_COMMANDS)
    }

    /**
     * Play tutorial introduction
     */
    private fun playIntroduction() {
        val intro = buildString {
            append("Let's learn these voice commands together. ")
            append("I found ${tutorialCommands.size} commands. ")
            append("I'll say each command, and you try repeating it. ")
            append("Ready? Let's start.")
        }

        speakMessage(intro)
    }

    /**
     * Proceed to next tutorial step
     */
    private fun proceedToNextStep() {
        scope.launch {
            // Check if tutorial complete
            if (currentStepIndex >= tutorialCommands.size) {
                completeTutorial()
                return@launch
            }

            // Get next command
            val command = tutorialCommands[currentStepIndex]

            // Create step
            val step = TutorialStep(
                stepNumber = currentStepIndex + 1,
                totalSteps = tutorialCommands.size,
                command = command,
                state = TutorialStepState.DEMONSTRATING
            )

            _currentStep.value = step

            // Demonstrate step
            demonstrateStep(step)

            // Start timeout
            startStepTimeout()
        }
    }

    /**
     * Demonstrate current step
     */
    private suspend fun demonstrateStep(step: TutorialStep) {
        // Highlight element (if overlay available)
        // TODO: Need element bounds to highlight
        // For now, just speak

        // Speak instruction
        val instruction = buildString {
            append("Step ${step.stepNumber} of ${step.totalSteps}. ")
            append("Try saying: ${step.command.commandText}")
        }

        speakMessage(instruction)

        // Update state
        _currentStep.value = step.copy(state = TutorialStepState.WAITING)
    }

    /**
     * User spoke a command - check if matches current step
     *
     * @param spokenText What user said
     */
    fun onUserSpoke(spokenText: String) {
        val step = _currentStep.value ?: return

        if (step.state != TutorialStepState.WAITING) {
            Log.d(TAG, "Not waiting for input, ignoring: $spokenText")
            return
        }

        // Normalize text for comparison
        val normalized = spokenText.trim().lowercase()
        val expected = step.command.commandText.trim().lowercase()

        // Check if matches (exact or close enough)
        val matches = normalized == expected ||
                normalized.contains(expected) ||
                expected.contains(normalized)

        if (matches) {
            onStepSuccess(step)
        } else {
            onStepFailure(step, spokenText)
        }
    }

    /**
     * Step completed successfully
     */
    private fun onStepSuccess(step: TutorialStep) {
        scope.launch {
            Log.d(TAG, "Step ${step.stepNumber} success")

            // Cancel timeout
            stepTimeoutJob?.cancel()

            // Update state
            _currentStep.value = step.copy(state = TutorialStepState.SUCCESS)

            // Speak feedback
            speakMessage("Great! Let's move to the next one.")

            // Delay before next step
            delay(2000)

            // Move to next
            currentStepIndex++
            proceedToNextStep()
        }
    }

    /**
     * Step failed - user said wrong command
     */
    private fun onStepFailure(step: TutorialStep, spokenText: String) {
        scope.launch {
            Log.d(TAG, "Step ${step.stepNumber} failed: said '$spokenText', expected '${step.command.commandText}'")

            val updatedStep = step.copy(
                state = TutorialStepState.RETRY,
                attemptCount = step.attemptCount + 1
            )

            _currentStep.value = updatedStep

            // Give feedback
            if (updatedStep.canRetry()) {
                speakMessage("Not quite. Try saying: ${step.command.commandText}")

                // Wait and try again
                delay(2000)
                _currentStep.value = updatedStep.copy(state = TutorialStepState.WAITING)

            } else {
                // Max attempts reached, skip
                speakMessage("That's okay. Let's move to the next command.")
                delay(2000)

                currentStepIndex++
                proceedToNextStep()
            }
        }
    }

    /**
     * Skip current step
     */
    fun skipCurrentStep() {
        scope.launch {
            Log.d(TAG, "Skipping step ${currentStepIndex + 1}")

            stepTimeoutJob?.cancel()
            speakMessage("Skipping to next command.")

            currentStepIndex++
            proceedToNextStep()
        }
    }

    /**
     * Start step timeout
     */
    private fun startStepTimeout() {
        stepTimeoutJob?.cancel()
        stepTimeoutJob = scope.launch {
            delay(STEP_TIMEOUT_MS)

            // Timeout reached
            Log.d(TAG, "Step ${currentStepIndex + 1} timed out")
            speakMessage("Let's move to the next command.")

            delay(2000)
            currentStepIndex++
            proceedToNextStep()
        }
    }

    /**
     * Complete tutorial
     */
    private fun completeTutorial() {
        scope.launch {
            Log.i(TAG, "Tutorial completed")

            stepTimeoutJob?.cancel()

            // Speak completion message
            val completion = buildString {
                append("Tutorial complete! ")
                append("You've learned ${tutorialCommands.size} voice commands. ")
                append("Say 'Show commands' anytime to see the full list.")
            }

            speakMessage(completion)

            // Cleanup
            delay(3000)
            stopTutorial()
        }
    }

    /**
     * Stop tutorial
     */
    fun stopTutorial() {
        Log.i(TAG, "Stopping tutorial")

        stepTimeoutJob?.cancel()
        _isActive.value = false
        _currentStep.value = null
        tutorialCommands = emptyList()
        currentStepIndex = 0

        // Hide overlay
        highlightOverlay?.hide()
    }

    /**
     * Speak message using TTS
     */
    private fun speakMessage(message: String) {
        textToSpeech?.speak(message, TextToSpeech.QUEUE_ADD, null, "tutorial_step")
        Log.d(TAG, "Speaking: $message")
    }

    /**
     * Dispose resources
     */
    fun dispose() {
        stopTutorial()
        highlightOverlay?.dispose()
        highlightOverlay = null
        scope.cancel()
    }
}

/**
 * Tutorial UI Overlay - Visual component for tutorial
 *
 * Shows current step, progress, and instructions.
 */
class TutorialOverlay(
    private val context: Context
) {
    companion object {
        private const val TAG = "TutorialOverlay"
    }

    // TODO: Implement Compose-based tutorial overlay
    // Shows:
    // - Progress bar (Step 3 of 12)
    // - Current command to try
    // - Visual feedback (✓ success, ⟳ retry)
    // - Skip button
    // - Stop tutorial button

    fun show(step: TutorialStep) {
        Log.d(TAG, "Would show tutorial step: ${step.stepNumber}/${step.totalSteps}")
        // TODO: Implement
    }

    fun updateState(state: TutorialStepState) {
        Log.d(TAG, "Would update tutorial state: $state")
        // TODO: Implement
    }

    fun hide() {
        Log.d(TAG, "Would hide tutorial overlay")
        // TODO: Implement
    }

    fun dispose() {
        // TODO: Implement cleanup
    }
}
