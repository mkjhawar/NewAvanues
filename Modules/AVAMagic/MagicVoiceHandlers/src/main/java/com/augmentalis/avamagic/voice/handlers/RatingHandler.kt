/**
 * RatingHandler.kt
 *
 * Created: 2026-01-27
 * Last Modified: 2026-01-27
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Voice command handler for rating controls - set, adjust, and clear ratings
 * Features: Star rating commands with natural language parsing
 * Location: CommandManager module
 *
 * Changelog:
 * - v1.0.0 (2026-01-27): Initial implementation with full rating command support
 */

package com.augmentalis.avamagic.voice.handlers

import android.util.Log
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Voice command handler for rating controls.
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
 * Design:
 * - Command parsing and routing only
 * - Delegates execution to RatingActions
 * - Implements CommandHandler for CommandRegistry integration
 *
 * @since 1.0.0
 */
class RatingHandler private constructor() : CommandHandler {

    companion object {
        private const val TAG = "RatingHandler"

        @Volatile
        private var instance: RatingHandler? = null

        /**
         * Get singleton instance
         */
        fun getInstance(): RatingHandler {
            return instance ?: synchronized(this) {
                instance ?: RatingHandler().also {
                    instance = it
                }
            }
        }

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

    // CommandHandler interface implementation
    override val moduleId: String = "rating"

    override val supportedCommands: List<String> = listOf(
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

    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State
    private var isInitialized = false
    private var currentRating = DEFAULT_RATING

    // Listener for rating changes
    private var ratingChangeListener: RatingChangeListener? = null

    init {
        initialize()
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize the rating handler
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            isInitialized = true
            Log.d(TAG, "RatingHandler initialized")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }

    /**
     * Set a listener for rating changes
     */
    fun setRatingChangeListener(listener: RatingChangeListener?) {
        ratingChangeListener = listener
    }

    /**
     * CommandHandler interface: Check if this handler can process the command
     */
    override fun canHandle(command: String): Boolean {
        return when {
            command.startsWith("rate ") -> true
            command in CLEAR_COMMANDS -> true
            command in INCREASE_COMMANDS -> true
            command in DECREASE_COMMANDS -> true
            command in MAX_COMMANDS -> true
            command in MIN_COMMANDS -> true
            STARS_PATTERN.matches(command) -> true
            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     */
    override suspend fun handleCommand(command: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Not initialized for command processing")
            return false
        }

        Log.d(TAG, "Processing rating command: '$command'")

        return try {
            when {
                // Clear rating
                command in CLEAR_COMMANDS -> clearRating()

                // Increase rating
                command in INCREASE_COMMANDS -> increaseRating()

                // Decrease rating
                command in DECREASE_COMMANDS -> decreaseRating()

                // Maximum rating
                command in MAX_COMMANDS -> setRating(MAX_RATING)

                // Minimum rating
                command in MIN_COMMANDS -> setRating(MIN_RATING)

                // "rate N stars" or "rate N"
                command.startsWith("rate ") -> {
                    val match = RATE_PATTERN.find(command)
                    if (match != null) {
                        val numberWord = match.groupValues[1]
                        val rating = parseNumber(numberWord)
                        if (rating != null) {
                            setRating(rating)
                        } else {
                            Log.w(TAG, "Could not parse rating number: $numberWord")
                            false
                        }
                    } else {
                        Log.w(TAG, "Command did not match rate pattern: $command")
                        false
                    }
                }

                // "N stars"
                STARS_PATTERN.matches(command) -> {
                    val match = STARS_PATTERN.find(command)
                    if (match != null) {
                        val numberWord = match.groupValues[1]
                        val rating = parseNumber(numberWord)
                        if (rating != null) {
                            setRating(rating)
                        } else {
                            Log.w(TAG, "Could not parse stars number: $numberWord")
                            false
                        }
                    } else {
                        false
                    }
                }

                else -> false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing command: $command", e)
            false
        }
    }

    /**
     * Set the rating to a specific value
     *
     * @param rating The rating value (1-5, or 0 to clear)
     * @return true if rating was set successfully
     */
    fun setRating(rating: Int): Boolean {
        return if (rating == 0 || rating in MIN_RATING..MAX_RATING) {
            val previousRating = currentRating
            currentRating = rating
            Log.i(TAG, "Rating set to $rating stars")
            notifyRatingChanged(previousRating, currentRating)
            true
        } else {
            Log.w(TAG, "Invalid rating value: $rating (must be $MIN_RATING-$MAX_RATING or 0)")
            false
        }
    }

    /**
     * Clear the current rating
     *
     * @return true if rating was cleared successfully
     */
    fun clearRating(): Boolean {
        val previousRating = currentRating
        currentRating = DEFAULT_RATING
        Log.i(TAG, "Rating cleared")
        notifyRatingChanged(previousRating, currentRating)
        return true
    }

    /**
     * Increase the rating by one star
     *
     * @return true if rating was increased successfully
     */
    fun increaseRating(): Boolean {
        return if (currentRating < MAX_RATING) {
            val previousRating = currentRating
            // If no rating, start at minimum
            currentRating = if (currentRating == DEFAULT_RATING) MIN_RATING else currentRating + 1
            Log.i(TAG, "Rating increased to $currentRating stars")
            notifyRatingChanged(previousRating, currentRating)
            true
        } else {
            Log.w(TAG, "Rating already at maximum ($MAX_RATING stars)")
            false
        }
    }

    /**
     * Decrease the rating by one star
     *
     * @return true if rating was decreased successfully
     */
    fun decreaseRating(): Boolean {
        return if (currentRating > MIN_RATING) {
            val previousRating = currentRating
            currentRating -= 1
            Log.i(TAG, "Rating decreased to $currentRating stars")
            notifyRatingChanged(previousRating, currentRating)
            true
        } else if (currentRating == MIN_RATING) {
            // Decreasing from minimum clears the rating
            clearRating()
        } else {
            Log.w(TAG, "No rating to decrease")
            false
        }
    }

    /**
     * Get the current rating
     *
     * @return Current rating (0 = no rating, 1-5 = stars)
     */
    fun getCurrentRating(): Int = currentRating

    /**
     * Check if there is a current rating
     *
     * @return true if a rating is set
     */
    fun hasRating(): Boolean = currentRating > DEFAULT_RATING

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

    /**
     * Notify listener of rating change
     */
    private fun notifyRatingChanged(previousRating: Int, newRating: Int) {
        ratingChangeListener?.onRatingChanged(previousRating, newRating)
    }

    /**
     * Get handler status
     */
    fun getStatus(): RatingHandlerStatus {
        return RatingHandlerStatus(
            isInitialized = isInitialized,
            currentRating = currentRating,
            commandsSupported = supportedCommands.size
        )
    }

    /**
     * Check if handler is ready
     */
    fun isReady(): Boolean = isInitialized

    /**
     * Cleanup resources
     */
    fun dispose() {
        CommandRegistry.unregisterHandler(moduleId)
        handlerScope.cancel()
        ratingChangeListener = null
        instance = null
        Log.d(TAG, "RatingHandler disposed")
    }
}

/**
 * Listener interface for rating changes
 */
interface RatingChangeListener {
    /**
     * Called when the rating changes
     *
     * @param previousRating The previous rating value
     * @param newRating The new rating value
     */
    fun onRatingChanged(previousRating: Int, newRating: Int)
}

/**
 * Status information for RatingHandler
 */
data class RatingHandlerStatus(
    val isInitialized: Boolean,
    val currentRating: Int,
    val commandsSupported: Int
)
