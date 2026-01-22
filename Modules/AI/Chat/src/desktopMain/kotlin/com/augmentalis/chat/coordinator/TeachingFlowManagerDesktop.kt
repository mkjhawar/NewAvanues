/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * Desktop implementation of Teaching Flow Manager.
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Desktop (JVM) implementation of ITeachingFlowManager.
 *
 * Manages the "Teach AVA" flow on desktop:
 * - Low confidence detection
 * - Bottom sheet/dialog state management
 * - Training example creation and persistence
 * - Confidence learning dialog coordination
 *
 * Training examples are stored in-memory and can be extended
 * to use SQLite or file-based persistence.
 *
 * @author Manoj Jhawar
 * @since 2025-01-16
 */
class TeachingFlowManagerDesktop(
    private val nluCoordinator: NLUCoordinatorDesktop
) : ITeachingFlowManager {

    // ==================== State ====================

    private val _teachAvaModeMessageId = MutableStateFlow<String?>(null)
    override val teachAvaModeMessageId: StateFlow<String?> = _teachAvaModeMessageId.asStateFlow()

    private val _showTeachBottomSheet = MutableStateFlow(false)
    override val showTeachBottomSheet: StateFlow<Boolean> = _showTeachBottomSheet.asStateFlow()

    private val _currentTeachMessageId = MutableStateFlow<String?>(null)
    override val currentTeachMessageId: StateFlow<String?> = _currentTeachMessageId.asStateFlow()

    private val _confidenceLearningDialogState = MutableStateFlow<ConfidenceLearningState?>(null)
    override val confidenceLearningDialogState: StateFlow<ConfidenceLearningState?> =
        _confidenceLearningDialogState.asStateFlow()

    private val _isTeaching = MutableStateFlow(false)
    override val isTeaching: StateFlow<Boolean> = _isTeaching.asStateFlow()

    private val _teachingError = MutableStateFlow<String?>(null)
    override val teachingError: StateFlow<String?> = _teachingError.asStateFlow()

    // ==================== Configuration ====================

    // Confidence threshold for showing teach button (default: 0.6)
    private var confidenceThreshold: Float = 0.6f

    // In-memory training example storage
    private val trainingExamples = mutableListOf<TrainingExample>()

    // Mutex for thread-safe operations
    private val teachingMutex = Mutex()

    // ==================== Teach Mode Operations ====================

    override fun shouldShowTeachButton(confidence: Float?, isNLUReady: Boolean): Boolean {
        // Don't show if NLU is not ready
        if (!isNLUReady) return false

        // Don't show if no confidence (no classification happened)
        if (confidence == null) return false

        // Show if confidence is below threshold
        return confidence <= confidenceThreshold
    }

    override fun activateTeachMode(messageId: String) {
        _teachAvaModeMessageId.value = messageId
        println("[TeachingFlowManagerDesktop] Teach mode activated for message: $messageId")
    }

    override fun deactivateTeachMode() {
        _teachAvaModeMessageId.value = null
        println("[TeachingFlowManagerDesktop] Teach mode deactivated")
    }

    override fun openTeachBottomSheet(messageId: String) {
        _currentTeachMessageId.value = messageId
        _showTeachBottomSheet.value = true
        println("[TeachingFlowManagerDesktop] Teach bottom sheet opened for message: $messageId")
    }

    override fun dismissTeachBottomSheet() {
        _showTeachBottomSheet.value = false
        _currentTeachMessageId.value = null
        println("[TeachingFlowManagerDesktop] Teach bottom sheet dismissed")
    }

    // ==================== Training Operations ====================

    override suspend fun handleTeachAva(
        messageId: String,
        intent: String,
        utterance: String
    ): Result<Unit> = teachingMutex.withLock {
        try {
            _isTeaching.value = true
            _teachingError.value = null

            // Validate inputs
            if (intent.isBlank()) {
                return Result.Error(
                    exception = IllegalArgumentException("Intent cannot be empty"),
                    message = "Please select or enter an intent"
                )
            }

            if (utterance.isBlank()) {
                return Result.Error(
                    exception = IllegalArgumentException("Utterance cannot be empty"),
                    message = "Utterance text is missing"
                )
            }

            // Create training example
            val example = TrainingExample(
                id = generateExampleId(),
                utterance = utterance.trim(),
                intent = intent.trim(),
                timestamp = System.currentTimeMillis(),
                source = "user_taught"
            )

            // Check for duplicates
            val isDuplicate = trainingExamples.any { existing ->
                existing.utterance.equals(example.utterance, ignoreCase = true) &&
                        existing.intent == example.intent
            }

            if (isDuplicate) {
                println("[TeachingFlowManagerDesktop] Duplicate training example detected, skipping")
            } else {
                // Add to storage
                trainingExamples.add(example)
                println("[TeachingFlowManagerDesktop] Added training example: ${example.utterance} -> ${example.intent}")

                // Update NLU coordinator with new intent
                nluCoordinator.addUserIntent(example.intent, example.utterance)
            }

            // Dismiss UI and clear state
            dismissTeachBottomSheet()
            deactivateTeachMode()

            _isTeaching.value = false
            Result.Success(Unit)
        } catch (e: Exception) {
            _isTeaching.value = false
            _teachingError.value = "Failed to save training example: ${e.message}"
            Result.Error(
                exception = e,
                message = "Teaching failed: ${e.message}"
            )
        }
    }

    // ==================== Confidence Learning Operations ====================

    override fun showConfidenceLearningDialog(state: ConfidenceLearningState) {
        _confidenceLearningDialogState.value = state
        println("[TeachingFlowManagerDesktop] Confidence learning dialog shown")
        println("  - Input: ${state.userInput}")
        println("  - Interpreted: ${state.interpretedIntent} (${state.confidence})")
        println("  - Alternates: ${state.alternateIntents.size}")
    }

    override suspend fun confirmInterpretation(
        userInput: String,
        confirmedIntent: String
    ): Result<Unit> = teachingMutex.withLock {
        try {
            _isTeaching.value = true

            // Create training example from confirmation
            val example = TrainingExample(
                id = generateExampleId(),
                utterance = userInput.trim(),
                intent = confirmedIntent.trim(),
                timestamp = System.currentTimeMillis(),
                source = "user_confirmed"
            )

            // Add if not duplicate
            val isDuplicate = trainingExamples.any { existing ->
                existing.utterance.equals(example.utterance, ignoreCase = true) &&
                        existing.intent == example.intent
            }

            if (!isDuplicate) {
                trainingExamples.add(example)
                nluCoordinator.addUserIntent(example.intent, example.utterance)
                println("[TeachingFlowManagerDesktop] Confirmed interpretation: ${example.utterance} -> ${example.intent}")
            }

            dismissConfidenceLearningDialog()
            _isTeaching.value = false
            Result.Success(Unit)
        } catch (e: Exception) {
            _isTeaching.value = false
            _teachingError.value = "Failed to confirm interpretation: ${e.message}"
            Result.Error(
                exception = e,
                message = "Confirmation failed: ${e.message}"
            )
        }
    }

    override suspend fun selectAlternateIntent(
        userInput: String,
        selectedIntent: String
    ): Result<Unit> = teachingMutex.withLock {
        try {
            _isTeaching.value = true

            // Create training example from correction
            val example = TrainingExample(
                id = generateExampleId(),
                utterance = userInput.trim(),
                intent = selectedIntent.trim(),
                timestamp = System.currentTimeMillis(),
                source = "user_corrected"
            )

            // Add to training data
            trainingExamples.add(example)
            nluCoordinator.addUserIntent(example.intent, example.utterance)

            println("[TeachingFlowManagerDesktop] Selected alternate intent: ${example.utterance} -> ${example.intent}")

            dismissConfidenceLearningDialog()
            _isTeaching.value = false
            Result.Success(Unit)
        } catch (e: Exception) {
            _isTeaching.value = false
            _teachingError.value = "Failed to save correction: ${e.message}"
            Result.Error(
                exception = e,
                message = "Correction failed: ${e.message}"
            )
        }
    }

    override fun dismissConfidenceLearningDialog() {
        _confidenceLearningDialogState.value = null
        println("[TeachingFlowManagerDesktop] Confidence learning dialog dismissed")
    }

    override fun clearError() {
        _teachingError.value = null
    }

    override fun updateConfidenceThreshold(threshold: Float) {
        confidenceThreshold = threshold.coerceIn(0.0f, 1.0f)
        println("[TeachingFlowManagerDesktop] Confidence threshold updated: $confidenceThreshold")
    }

    // ==================== Desktop-Specific Methods ====================

    /**
     * Get all training examples.
     *
     * @return List of training examples
     */
    fun getAllTrainingExamples(): List<TrainingExample> {
        return trainingExamples.toList()
    }

    /**
     * Get training examples for a specific intent.
     *
     * @param intent Intent identifier
     * @return List of training examples for the intent
     */
    fun getTrainingExamplesForIntent(intent: String): List<TrainingExample> {
        return trainingExamples.filter { it.intent == intent }
    }

    /**
     * Remove a training example.
     *
     * @param id Training example ID
     * @return True if removed, false if not found
     */
    fun removeTrainingExample(id: String): Boolean {
        val removed = trainingExamples.removeIf { it.id == id }
        if (removed) {
            nluCoordinator.clearClassificationCache()
            println("[TeachingFlowManagerDesktop] Removed training example: $id")
        }
        return removed
    }

    /**
     * Clear all training examples.
     */
    fun clearAllTrainingExamples() {
        trainingExamples.clear()
        nluCoordinator.clearClassificationCache()
        println("[TeachingFlowManagerDesktop] Cleared all training examples")
    }

    /**
     * Get count of training examples.
     *
     * @return Total number of training examples
     */
    fun getTrainingExampleCount(): Int {
        return trainingExamples.size
    }

    private fun generateExampleId(): String {
        return "te_${System.currentTimeMillis()}_${(1000..9999).random()}"
    }

    // ==================== Data Classes ====================

    /**
     * Training example data class.
     */
    data class TrainingExample(
        val id: String,
        val utterance: String,
        val intent: String,
        val timestamp: Long,
        val source: String // "user_taught", "user_confirmed", "user_corrected"
    )

    companion object {
        @Volatile
        private var INSTANCE: TeachingFlowManagerDesktop? = null

        /**
         * Get singleton instance of TeachingFlowManagerDesktop.
         *
         * @param nluCoordinator NLU coordinator for intent updates
         * @return Singleton instance
         */
        fun getInstance(
            nluCoordinator: NLUCoordinatorDesktop = NLUCoordinatorDesktop.getInstance()
        ): TeachingFlowManagerDesktop {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TeachingFlowManagerDesktop(nluCoordinator).also {
                    INSTANCE = it
                }
            }
        }
    }
}
