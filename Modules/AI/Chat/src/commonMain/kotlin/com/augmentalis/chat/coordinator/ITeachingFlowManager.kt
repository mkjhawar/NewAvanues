/*
 * Copyright (c) 2024-2025 Intelligent Devices LLC / Augmentalis
 * All Rights Reserved.
 *
 * SOLID Refactoring: Teaching flow manager interface for cross-platform
 */

package com.augmentalis.chat.coordinator

import com.augmentalis.ava.core.common.Result
import kotlinx.coroutines.flow.StateFlow

/**
 * Teaching Flow Manager Interface - Cross-platform teaching flow coordination
 *
 * Abstracts the "Teach AVA" flow for cross-platform use in KMP.
 * Provides:
 * - Low confidence detection and teach button triggering
 * - Bottom sheet state management
 * - Training example creation and persistence
 * - Confidence learning dialog state
 *
 * SOLID Principle: Single Responsibility
 * - Extracted from ChatViewModel for teaching-specific concerns
 * - Handles all teach mode logic, bottom sheet, and training example creation
 *
 * @see TeachingFlowManager for Android implementation
 *
 * @author Manoj Jhawar / Claude AI
 * @since 2025-01-15
 */
interface ITeachingFlowManager {

    // ==================== State ====================

    /**
     * Message ID currently in teach mode (showing "Teach AVA" button).
     * Null when no message is in teach mode.
     */
    val teachAvaModeMessageId: StateFlow<String?>

    /**
     * Bottom sheet visibility state.
     * True when TeachAvaBottomSheet is shown.
     */
    val showTeachBottomSheet: StateFlow<Boolean>

    /**
     * Message ID being taught in bottom sheet.
     * Used to retrieve utterance text for teaching UI.
     */
    val currentTeachMessageId: StateFlow<String?>

    /**
     * Confidence learning dialog state.
     * When NLU confidence < threshold, this state triggers interactive learning.
     */
    val confidenceLearningDialogState: StateFlow<ConfidenceLearningState?>

    /**
     * Teaching operation in progress state.
     */
    val isTeaching: StateFlow<Boolean>

    /**
     * Last teaching error message.
     */
    val teachingError: StateFlow<String?>

    // ==================== Teach Mode Operations ====================

    /**
     * Check if teach button should be shown for a given confidence.
     *
     * Logic:
     * - If NLU is not ready: don't show teach button
     * - If confidence is null (no classification): don't show teach button
     * - If confidence <= threshold: show teach button
     *
     * @param confidence Confidence score from NLU (0.0 to 1.0), null if no classification
     * @param isNLUReady Whether NLU system is ready
     * @return true if teach button should be shown
     */
    fun shouldShowTeachButton(confidence: Float?, isNLUReady: Boolean): Boolean

    /**
     * Activate teach mode for a specific message.
     * Shows "Teach AVA" button and opens bottom sheet.
     *
     * @param messageId ID of message to teach
     */
    fun activateTeachMode(messageId: String)

    /**
     * Deactivate teach mode (clears the teach button).
     */
    fun deactivateTeachMode()

    /**
     * Open teach bottom sheet for a message.
     *
     * @param messageId ID of message to teach
     */
    fun openTeachBottomSheet(messageId: String)

    /**
     * Dismiss the teach bottom sheet.
     */
    fun dismissTeachBottomSheet()

    // ==================== Training Operations ====================

    /**
     * Handle user selection from teach bottom sheet.
     * Creates a TrainExample and saves to repository.
     *
     * Flow:
     * 1. Retrieve message utterance
     * 2. Generate hash for deduplication
     * 3. Create TrainExample entity
     * 4. Save to repository
     * 5. Invalidate NLU caches
     * 6. Show success/error feedback
     * 7. Dismiss bottom sheet
     *
     * @param messageId ID of message being taught
     * @param intent User-selected intent for this utterance
     * @param utterance The original user utterance text
     * @return Result indicating success or failure
     */
    suspend fun handleTeachAva(
        messageId: String,
        intent: String,
        utterance: String
    ): Result<Unit>

    // ==================== Confidence Learning Operations ====================

    /**
     * Show confidence learning dialog for low-confidence responses.
     *
     * @param state Dialog state with user input, interpreted intent, and alternates
     */
    fun showConfidenceLearningDialog(state: ConfidenceLearningState)

    /**
     * User confirmed NLU's interpretation.
     * Learns from the confirmation.
     *
     * @param userInput Original user input
     * @param confirmedIntent The confirmed intent
     * @return Result indicating success or failure
     */
    suspend fun confirmInterpretation(userInput: String, confirmedIntent: String): Result<Unit>

    /**
     * User selected an alternate intent.
     * Learns from the correction.
     *
     * @param userInput Original user input
     * @param selectedIntent The user-selected alternate intent
     * @return Result indicating success or failure
     */
    suspend fun selectAlternateIntent(userInput: String, selectedIntent: String): Result<Unit>

    /**
     * Dismiss confidence learning dialog without learning.
     */
    fun dismissConfidenceLearningDialog()

    /**
     * Clear teaching error message.
     */
    fun clearError()

    /**
     * Update confidence threshold from preferences.
     *
     * @param threshold New threshold value (0.0 to 1.0)
     */
    fun updateConfidenceThreshold(threshold: Float)
}

/**
 * State for confidence learning dialog.
 *
 * @property userInput Original user input text
 * @property interpretedIntent NLU's interpreted intent
 * @property confidence NLU confidence score
 * @property alternateIntents List of alternate intents with scores
 */
data class ConfidenceLearningState(
    val userInput: String,
    val interpretedIntent: String,
    val confidence: Float,
    val alternateIntents: List<AlternateIntent>
)

/**
 * Alternate intent option for confidence learning dialog.
 *
 * @property intentId Intent identifier
 * @property displayName Human-readable intent name
 * @property confidence Confidence score for this alternate
 */
data class AlternateIntent(
    val intentId: String,
    val displayName: String,
    val confidence: Float
)
