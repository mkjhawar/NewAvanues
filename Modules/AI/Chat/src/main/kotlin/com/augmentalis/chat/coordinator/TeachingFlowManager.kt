/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Extracted from ChatViewModel (SRP)
 */

package com.augmentalis.chat.coordinator

import android.util.Log
import com.augmentalis.ava.core.common.Result
import com.augmentalis.ava.core.data.prefs.ChatPreferences
import com.augmentalis.ava.core.data.repository.TrainExampleRepositoryImpl
import com.augmentalis.ava.core.domain.model.TrainExample
import com.augmentalis.ava.core.domain.model.TrainExampleSource
import com.augmentalis.ava.core.domain.repository.TrainExampleRepository
import com.augmentalis.nlu.NLUSelfLearner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Teaching Flow Manager - Single Responsibility: Teach-AVA Flow Coordination
 *
 * Extracted from ChatViewModel as part of SOLID refactoring.
 * Handles all teaching-related operations:
 * - Low confidence detection and teach mode activation
 * - Bottom sheet state management
 * - Training example creation and persistence
 * - Confidence learning dialog coordination
 * - NLU self-learning integration
 *
 * Thread-safe: Uses StateFlow for all mutable state.
 *
 * @param trainExampleRepository Repository for training examples
 * @param nluSelfLearner NLU self-learning system
 * @param chatPreferences User preferences for confidence threshold
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-01-15
 */
@Singleton
class TeachingFlowManager @Inject constructor(
    private val trainExampleRepository: TrainExampleRepository,
    private val nluSelfLearner: NLUSelfLearner,
    private val chatPreferences: ChatPreferences
) : ITeachingFlowManager {

    companion object {
        private const val TAG = "TeachingFlowManager"

        /** Default confidence threshold for teach mode trigger */
        const val DEFAULT_CONFIDENCE_THRESHOLD = 0.5f
    }

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

    @Volatile
    private var confidenceThreshold: Float = DEFAULT_CONFIDENCE_THRESHOLD

    init {
        // Initialize threshold from preferences
        confidenceThreshold = chatPreferences.confidenceThreshold.value
    }

    // ==================== Teach Mode Operations ====================

    /**
     * Check if teach button should be shown for a given confidence.
     *
     * Logic:
     * - If NLU is not ready: don't show teach button (user messages go to LLM directly)
     * - If confidence is null (no classification): don't show teach button
     * - If confidence <= threshold: show teach button with unknown template
     * - If confidence > threshold: don't show teach button
     *
     * @param confidence Confidence score from NLU (0.0 to 1.0), null if no classification
     * @param isNLUReady Whether NLU system is ready
     * @return true if teach button should be shown
     */
    override fun shouldShowTeachButton(confidence: Float?, isNLUReady: Boolean): Boolean {
        // Only show teach button if NLU is ready AND confidence is low
        // This prevents teach mode from appearing during NLU initialization
        return isNLUReady &&
               confidence != null &&
               confidence <= confidenceThreshold
    }

    /**
     * Activate teach mode for a specific message.
     * Shows "Teach AVA" button and opens bottom sheet.
     *
     * @param messageId ID of message to teach
     */
    override fun activateTeachMode(messageId: String) {
        _teachAvaModeMessageId.value = messageId
        openTeachBottomSheet(messageId)
        Log.d(TAG, "Teach mode activated for message: $messageId (bottom sheet shown)")
    }

    /**
     * Deactivate teach mode (clears the teach button).
     */
    override fun deactivateTeachMode() {
        _teachAvaModeMessageId.value = null
        Log.d(TAG, "Teach mode deactivated")
    }

    /**
     * Open teach bottom sheet for a message.
     *
     * @param messageId ID of message to teach
     */
    override fun openTeachBottomSheet(messageId: String) {
        _currentTeachMessageId.value = messageId
        _showTeachBottomSheet.value = true
        Log.d(TAG, "Teach bottom sheet opened for message: $messageId")
    }

    /**
     * Dismiss the teach bottom sheet.
     */
    override fun dismissTeachBottomSheet() {
        _showTeachBottomSheet.value = false
        _currentTeachMessageId.value = null
        Log.d(TAG, "Teach bottom sheet dismissed")
    }

    // ==================== Training Operations ====================

    /**
     * Handle user selection from teach bottom sheet.
     * Creates a TrainExample and saves to repository.
     *
     * Flow:
     * 1. Generate hash for deduplication (MD5 of utterance + intent)
     * 2. Create TrainExample entity with user-selected intent
     * 3. Save to TrainExampleRepository
     * 4. Show success/error feedback
     * 5. Dismiss bottom sheet and deactivate teach mode
     *
     * @param messageId ID of message being taught
     * @param intent User-selected intent for this utterance
     * @param utterance The original user utterance text
     * @return Result indicating success or failure
     */
    override suspend fun handleTeachAva(
        messageId: String,
        intent: String,
        utterance: String
    ): Result<Unit> {
        return try {
            _isTeaching.value = true
            _teachingError.value = null
            Log.d(TAG, "handleTeachAva called: messageId=$messageId, intent=$intent")
            Log.d(TAG, "Teaching utterance: '$utterance' -> intent: '$intent'")

            // 1. Generate hash for deduplication
            val exampleHash = TrainExampleRepositoryImpl.generateHash(utterance, intent)

            // 2. Create TrainExample entity
            val trainExample = TrainExample(
                id = 0, // Auto-generated by database
                exampleHash = exampleHash,
                utterance = utterance,
                intent = intent,
                locale = Locale.getDefault().toLanguageTag(),
                source = TrainExampleSource.MANUAL,
                createdAt = System.currentTimeMillis(),
                usageCount = 0,
                lastUsed = null
            )

            // 3. Save to TrainExampleRepository
            when (val result = trainExampleRepository.addTrainExample(trainExample)) {
                is Result.Success -> {
                    Log.d(TAG, "Successfully saved training example: $trainExample")

                    // 4. Dismiss bottom sheet and deactivate teach mode
                    dismissTeachBottomSheet()
                    deactivateTeachMode()

                    Log.i(TAG, "Teach AVA completed successfully")
                    Result.Success(Unit)
                }
                is Result.Error -> {
                    // Handle specific errors
                    val errorMsg = if (result.message?.contains("Duplicate") == true) {
                        "This example already exists in your training data"
                    } else {
                        "Failed to save training example: ${result.message}"
                    }
                    _teachingError.value = errorMsg
                    Log.e(TAG, "Failed to save training example", result.exception)
                    Result.Error(result.exception ?: Exception(errorMsg), errorMsg)
                }
            }

        } catch (e: Exception) {
            val errorMsg = "Failed to teach AVA: ${e.message}"
            _teachingError.value = errorMsg
            Log.e(TAG, "Exception in handleTeachAva", e)
            Result.Error(e, errorMsg)
        } finally {
            _isTeaching.value = false
        }
    }

    // ==================== Confidence Learning Operations ====================

    /**
     * Show confidence learning dialog for low-confidence responses.
     *
     * @param state Dialog state with user input, interpreted intent, and alternates
     */
    override fun showConfidenceLearningDialog(state: ConfidenceLearningState) {
        _confidenceLearningDialogState.value = state
        Log.i(TAG, "Confidence learning dialog shown for: \"${state.userInput}\" -> ${state.interpretedIntent}")
    }

    /**
     * User confirmed NLU's interpretation.
     * Learns from the confirmation using NLUSelfLearner.
     *
     * @param userInput Original user input
     * @param confirmedIntent The confirmed intent
     * @return Result indicating success or failure
     */
    override suspend fun confirmInterpretation(userInput: String, confirmedIntent: String): Result<Unit> {
        return try {
            Log.i(TAG, "User confirmed interpretation: \"$userInput\" -> $confirmedIntent")

            // Use NLUSelfLearner for unified learning
            val success = nluSelfLearner.learnFromLLM(
                utterance = userInput,
                intent = confirmedIntent,
                confidence = 1.0f, // User confirmed = high confidence
                variations = emptyList()
            )

            if (success) {
                Log.i(TAG, "Successfully learned confirmed interpretation via NLUSelfLearner")
            } else {
                Log.w(TAG, "Failed to learn confirmed interpretation (may already exist)")
            }

            // Dismiss dialog
            dismissConfidenceLearningDialog()
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error confirming interpretation", e)
            dismissConfidenceLearningDialog()
            Result.Error(e, "Failed to confirm interpretation: ${e.message}")
        }
    }

    /**
     * User selected an alternate intent.
     * Learns from the correction using NLUSelfLearner.
     *
     * @param userInput Original user input
     * @param selectedIntent The user-selected alternate intent
     * @return Result indicating success or failure
     */
    override suspend fun selectAlternateIntent(userInput: String, selectedIntent: String): Result<Unit> {
        return try {
            Log.i(TAG, "User selected alternate: \"$userInput\" -> $selectedIntent")

            // Use NLUSelfLearner for unified learning
            val success = nluSelfLearner.learnFromLLM(
                utterance = userInput,
                intent = selectedIntent,
                confidence = 1.0f, // User corrected = high confidence
                variations = emptyList()
            )

            if (success) {
                Log.i(TAG, "Successfully learned corrected interpretation via NLUSelfLearner")
            } else {
                Log.w(TAG, "Failed to learn corrected interpretation (may already exist)")
            }

            // Dismiss dialog
            dismissConfidenceLearningDialog()
            Result.Success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Error selecting alternate intent", e)
            dismissConfidenceLearningDialog()
            Result.Error(e, "Failed to learn alternate intent: ${e.message}")
        }
    }

    /**
     * Dismiss confidence learning dialog without learning.
     */
    override fun dismissConfidenceLearningDialog() {
        Log.d(TAG, "Confidence learning dialog dismissed")
        _confidenceLearningDialogState.value = null
    }

    /**
     * Clear teaching error message.
     */
    override fun clearError() {
        _teachingError.value = null
    }

    /**
     * Update confidence threshold from preferences.
     *
     * @param threshold New threshold value (0.0 to 1.0)
     */
    override fun updateConfidenceThreshold(threshold: Float) {
        require(threshold in 0f..1f) { "Threshold must be between 0.0 and 1.0" }
        confidenceThreshold = threshold
        Log.d(TAG, "Confidence threshold updated to: $threshold")
    }
}
