/**
 * RatingHandler.kt
 *
 * Created: 2026-01-27
 * Last Modified: 2026-01-28
 * Author: VOS4 Development Team
 * Version: 2.0.0
 *
 * Purpose: Voice command handler for rating controls - set, adjust, and clear ratings
 * Features: Star rating commands with natural language parsing
 * Location: MagicVoiceHandlers module
 *
 * Changelog:
 * - v2.0.0 (2026-01-28): Migrated to BaseHandler pattern with executor
 * - v1.0.0 (2026-01-27): Initial implementation with full rating command support
 */

package com.augmentalis.avamagic.voice.handlers.input

import android.util.Log
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for rating controls.
 *
 * Routes commands to set, adjust, or clear rating values via executor pattern.
 *
 * Supported Commands:
 * - "rate [N] stars" - set rating to N stars
 * - "rate [N]" - set rating to N
 * - "[N] stars" - set rating to N stars
 * - "clear rating" / "no rating" - clear the rating
 * - "increase rating" / "more stars" - add one star
 * - "decrease rating" / "fewer stars" - remove one star
 * - "maximum rating" / "full stars" - set to max (5 stars)
 * - "minimum rating" - set to minimum (1 star)
 *
 * Thread Safety:
 * - All operations are suspend functions
 * - State modifications are atomic via executor
 *
 * @param executor Platform-specific executor for rating operations
 */
class RatingHandler(
    private val executor: RatingExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "RatingHandler"

        // Rating configuration
        const val MIN_RATING = 1
        const val MAX_RATING = 5
        const val DEFAULT_RATING = 0 // 0 = no rating

        // Word to number mapping for voice recognition
        private val WORD_TO_NUMBER = mapOf(
            "zero" to 0,
            "one" to 1,
            "two" to 2,
            "three" to 3,
            "four" to 4,
            "five" to 5,
            "1" to 1,
            "2" to 2,
            "3" to 3,
            "4" to 4,
            "5" to 5
        )

        // Command patterns
        private val RATE_PATTERN = Regex("""^rate\s+(\w+)(?:\s+stars?)?$""")
        private val STARS_PATTERN = Regex("""^(\w+)\s+stars?$""")
        private val CLEAR_COMMANDS = setOf("clear rating", "no rating", "remove rating", "delete rating")
        private val INCREASE_COMMANDS = setOf("increase rating", "more stars", "add star", "up rating", "rating up")
        private val DECREASE_COMMANDS = setOf("decrease rating", "fewer stars", "less stars", "remove star", "down rating", "rating down")
        private val MAX_COMMANDS = setOf("maximum rating", "full stars", "max rating", "five stars", "5 stars", "full rating")
        private val MIN_COMMANDS = setOf("minimum rating", "min rating", "one star", "1 star", "lowest rating")
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Set rating commands
        "rate [N] stars",
        "rate [N]",
        "[N] stars",

        // Clear rating
        "clear rating",
        "no rating",
        "remove rating",

        // Adjust rating
        "increase rating",
        "more stars",
        "add star",
        "decrease rating",
        "fewer stars",
        "remove star",

        // Extremes
        "maximum rating",
        "full stars",
        "minimum rating"
    )

    /**
     * Callback for voice feedback when rating changes
     */
    var onRatingChanged: ((previousRating: Int, newRating: Int) -> Unit)? = null

    override suspend fun execute(
        command: QuantizedCommand,
        params: Map<String, Any>
    ): HandlerResult {
        val normalizedAction = command.phrase.lowercase().trim()

        Log.d(TAG, "Processing rating command: '$normalizedAction'")

        return try {
            when {
                // Clear rating
                normalizedAction in CLEAR_COMMANDS -> handleClearRating()

                // Increase rating
                normalizedAction in INCREASE_COMMANDS -> handleIncreaseRating()

                // Decrease rating
                normalizedAction in DECREASE_COMMANDS -> handleDecreaseRating()

                // Maximum rating
                normalizedAction in MAX_COMMANDS -> handleSetRating(MAX_RATING)

                // Minimum rating
                normalizedAction in MIN_COMMANDS -> handleSetRating(MIN_RATING)

                // "rate N stars" or "rate N"
                normalizedAction.startsWith("rate ") -> {
                    val match = RATE_PATTERN.find(normalizedAction)
                    if (match != null) {
                        val numberWord = match.groupValues[1]
                        val rating = parseNumber(numberWord)
                        if (rating != null) {
                            handleSetRating(rating)
                        } else {
                            HandlerResult.failure(
                                reason = "Could not parse rating number: $numberWord",
                                recoverable = true
                            )
                        }
                    } else {
                        HandlerResult.failure(
                            reason = "Could not parse rate command: $normalizedAction",
                            recoverable = true
                        )
                    }
                }

                // "N stars"
                STARS_PATTERN.matches(normalizedAction) -> {
                    val match = STARS_PATTERN.find(normalizedAction)
                    if (match != null) {
                        val numberWord = match.groupValues[1]
                        val rating = parseNumber(numberWord)
                        if (rating != null) {
                            handleSetRating(rating)
                        } else {
                            HandlerResult.failure(
                                reason = "Could not parse stars number: $numberWord",
                                recoverable = true
                            )
                        }
                    } else {
                        HandlerResult.notHandled()
                    }
                }

                else -> HandlerResult.notHandled()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing rating command", e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Command Handlers
    // ═══════════════════════════════════════════════════════════════════════════

    private suspend fun handleSetRating(rating: Int): HandlerResult {
        if (rating != 0 && rating !in MIN_RATING..MAX_RATING) {
            return HandlerResult.failure(
                reason = "Invalid rating value: $rating (must be $MIN_RATING-$MAX_RATING)",
                recoverable = true
            )
        }

        val result = executor.setRating(rating)

        return when (result) {
            is RatingResult.Success -> {
                onRatingChanged?.invoke(result.previousRating, result.newRating)
                HandlerResult.Success(
                    message = "Rating set to ${result.newRating} stars",
                    data = mapOf(
                        "previousRating" to result.previousRating,
                        "newRating" to result.newRating
                    )
                )
            }
            is RatingResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            RatingResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            RatingResult.NoRatingWidget -> {
                HandlerResult.Failure(
                    reason = "No rating widget found",
                    recoverable = true,
                    suggestedAction = "Focus on a rating control first"
                )
            }
        }
    }

    private suspend fun handleClearRating(): HandlerResult {
        val result = executor.clearRating()

        return when (result) {
            is RatingResult.Success -> {
                onRatingChanged?.invoke(result.previousRating, result.newRating)
                HandlerResult.Success(
                    message = "Rating cleared",
                    data = mapOf(
                        "previousRating" to result.previousRating,
                        "newRating" to result.newRating
                    )
                )
            }
            is RatingResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            RatingResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            RatingResult.NoRatingWidget -> {
                HandlerResult.Failure(
                    reason = "No rating widget found",
                    recoverable = true,
                    suggestedAction = "Focus on a rating control first"
                )
            }
        }
    }

    private suspend fun handleIncreaseRating(): HandlerResult {
        val result = executor.increaseRating()

        return when (result) {
            is RatingResult.Success -> {
                onRatingChanged?.invoke(result.previousRating, result.newRating)
                HandlerResult.Success(
                    message = "Rating increased to ${result.newRating} stars",
                    data = mapOf(
                        "previousRating" to result.previousRating,
                        "newRating" to result.newRating
                    )
                )
            }
            is RatingResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            RatingResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            RatingResult.NoRatingWidget -> {
                HandlerResult.Failure(
                    reason = "No rating widget found",
                    recoverable = true,
                    suggestedAction = "Focus on a rating control first"
                )
            }
        }
    }

    private suspend fun handleDecreaseRating(): HandlerResult {
        val result = executor.decreaseRating()

        return when (result) {
            is RatingResult.Success -> {
                onRatingChanged?.invoke(result.previousRating, result.newRating)
                HandlerResult.Success(
                    message = "Rating decreased to ${result.newRating} stars",
                    data = mapOf(
                        "previousRating" to result.previousRating,
                        "newRating" to result.newRating
                    )
                )
            }
            is RatingResult.Error -> {
                HandlerResult.failure(
                    reason = result.message,
                    recoverable = true
                )
            }
            RatingResult.NoAccessibility -> {
                HandlerResult.failure(
                    reason = "Accessibility service not available",
                    recoverable = false
                )
            }
            RatingResult.NoRatingWidget -> {
                HandlerResult.Failure(
                    reason = "No rating widget found",
                    recoverable = true,
                    suggestedAction = "Focus on a rating control first"
                )
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // Helper Methods
    // ═══════════════════════════════════════════════════════════════════════════

    /**
     * Parse a number from text (supports both digits and words)
     *
     * @param text The text to parse (e.g., "3", "three")
     * @return The parsed number, or null if parsing failed
     */
    private fun parseNumber(text: String): Int? {
        val normalized = text.lowercase().trim()
        return WORD_TO_NUMBER[normalized]
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// Supporting Types
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Rating handler status
 */
data class RatingHandlerStatus(
    val hasAccessibilityService: Boolean,
    val currentRating: Int,
    val hasRating: Boolean
)

/**
 * Rating operation result
 */
sealed class RatingResult {
    data class Success(val previousRating: Int, val newRating: Int) : RatingResult()
    data class Error(val message: String) : RatingResult()
    object NoAccessibility : RatingResult()
    object NoRatingWidget : RatingResult()
}

// ═══════════════════════════════════════════════════════════════════════════════
// Platform Executor Interface
// ═══════════════════════════════════════════════════════════════════════════════

/**
 * Platform-specific executor for rating operations.
 *
 * Implementations should:
 * 1. Find rating components (RatingBar, SeekBar) in accessibility tree
 * 2. Set rating values (1-5 stars or 0 to clear)
 * 3. Get current rating state
 */
interface RatingExecutor {

    /**
     * Set the rating to a specific value.
     *
     * @param rating The rating value (1-5, or 0 to clear)
     * @return The result of the operation
     */
    suspend fun setRating(rating: Int): RatingResult

    /**
     * Clear the current rating.
     *
     * @return The result of the operation
     */
    suspend fun clearRating(): RatingResult

    /**
     * Increase the rating by one star.
     *
     * @return The result of the operation
     */
    suspend fun increaseRating(): RatingResult

    /**
     * Decrease the rating by one star.
     *
     * @return The result of the operation
     */
    suspend fun decreaseRating(): RatingResult

    /**
     * Get the current rating value.
     *
     * @return Current rating (0 = no rating, 1-5 = stars)
     */
    suspend fun getCurrentRating(): Int

    /**
     * Check if a rating is currently set.
     *
     * @return true if a rating is set
     */
    suspend fun hasRating(): Boolean

    /**
     * Get handler status.
     */
    suspend fun getStatus(): RatingHandlerStatus
}
