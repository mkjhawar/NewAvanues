/**
 * CarouselHandler.kt
 *
 * Created: 2026-01-27 PST
 * Last Modified: 2026-01-28 PST
 * Version: 2.0.0
 *
 * Purpose: Voice command handler for carousel/slider UI components
 * Features: Navigation, slide jumping, auto-play control
 * Location: VoiceIntegration module handlers
 *
 * Changelog:
 * - v2.0.0 (2026-01-28): Migrated to BaseHandler architecture with executor pattern
 * - v1.0.0 (2026-01-27): Initial implementation for carousel voice control
 */

package com.augmentalis.avanueui.voice.handlers.display

import com.avanues.logging.LoggerFactory
import com.augmentalis.voiceoscore.ActionCategory
import com.augmentalis.voiceoscore.BaseHandler
import com.augmentalis.voiceoscore.HandlerResult
import com.augmentalis.voiceoscore.QuantizedCommand

/**
 * Voice command handler for carousel/slider UI components.
 *
 * Supports ViewPager, ViewPager2, RecyclerView-based carousels, and similar
 * horizontally scrollable content containers.
 *
 * Commands:
 * - Navigation: "next slide", "previous slide", "back"
 * - Jump: "go to slide [N]", "slide [N]", "first slide", "last slide"
 * - Playback: "pause", "stop", "play", "resume"
 *
 * Design:
 * - Implements BaseHandler interface for VoiceOS integration
 * - Delegates platform-specific operations to CarouselExecutor
 * - Stateless command processing
 *
 * @since 2.0.0
 */
class CarouselHandler(
    private val executor: CarouselExecutor
) : BaseHandler() {

    companion object {
        private const val TAG = "CarouselHandler"
        private val Log = LoggerFactory.getLogger(TAG)

        // Command pattern constants
        private const val GO_TO_PREFIX = "go to slide"
        private const val SLIDE_PREFIX = "slide"
        private const val PAUSE_COMMAND = "pause"
        private const val STOP_COMMAND = "stop"
        private const val PLAY_COMMAND = "play"
        private const val RESUME_COMMAND = "resume"

        // Word to number mapping for spoken numbers
        private val WORD_TO_NUMBER = mapOf(
            "one" to 1, "first" to 1, "1st" to 1,
            "two" to 2, "second" to 2, "2nd" to 2,
            "three" to 3, "third" to 3, "3rd" to 3,
            "four" to 4, "fourth" to 4, "4th" to 4,
            "five" to 5, "fifth" to 5, "5th" to 5,
            "six" to 6, "sixth" to 6, "6th" to 6,
            "seven" to 7, "seventh" to 7, "7th" to 7,
            "eight" to 8, "eighth" to 8, "8th" to 8,
            "nine" to 9, "ninth" to 9, "9th" to 9,
            "ten" to 10, "tenth" to 10, "10th" to 10,
            "eleven" to 11, "eleventh" to 11, "11th" to 11,
            "twelve" to 12, "twelfth" to 12, "12th" to 12
        )
    }

    override val category: ActionCategory = ActionCategory.UI

    override val supportedActions: List<String> = listOf(
        // Navigation commands
        "next slide",
        "next",
        "previous slide",
        "previous",
        "back",

        // Jump commands
        "go to slide [N]",
        "slide [N]",
        "first slide",
        "last slide",

        // Playback control
        "pause",
        "stop",
        "play",
        "resume"
    )

    /**
     * Execute carousel command
     */
    override suspend fun execute(command: QuantizedCommand, params: Map<String, Any>): HandlerResult {
        val commandText = command.phrase.lowercase().trim()
        Log.d { "Processing carousel command: '$commandText'" }

        return try {
            when {
                // Next slide
                commandText == "next slide" || commandText == "next" -> {
                    handleResult(executor.navigateToNext(), "Navigated to next slide")
                }

                // Previous slide
                commandText == "previous slide" || commandText == "previous" || commandText == "back" -> {
                    handleResult(executor.navigateToPrevious(), "Navigated to previous slide")
                }

                // Go to specific slide: "go to slide N"
                commandText.startsWith(GO_TO_PREFIX) -> {
                    val slideNumber = extractSlideNumber(commandText.removePrefix(GO_TO_PREFIX).trim())
                    if (slideNumber != null) {
                        handleResult(executor.navigateToSlide(slideNumber), "Navigated to slide $slideNumber")
                    } else {
                        HandlerResult.failure("Could not parse slide number from command", recoverable = true)
                    }
                }

                // Slide N shorthand: "slide N"
                commandText.startsWith(SLIDE_PREFIX) && commandText != SLIDE_PREFIX -> {
                    val slideNumber = extractSlideNumber(commandText.removePrefix(SLIDE_PREFIX).trim())
                    if (slideNumber != null) {
                        handleResult(executor.navigateToSlide(slideNumber), "Navigated to slide $slideNumber")
                    } else {
                        HandlerResult.failure("Could not parse slide number from command", recoverable = true)
                    }
                }

                // First slide
                commandText == "first slide" -> {
                    handleResult(executor.navigateToFirst(), "Navigated to first slide")
                }

                // Last slide
                commandText == "last slide" -> {
                    handleResult(executor.navigateToLast(), "Navigated to last slide")
                }

                // Pause/stop auto-play
                commandText == PAUSE_COMMAND || commandText == STOP_COMMAND -> {
                    handleResult(executor.pauseAutoPlay(), "Carousel auto-play paused")
                }

                // Play/resume auto-play
                commandText == PLAY_COMMAND || commandText == RESUME_COMMAND -> {
                    handleResult(executor.resumeAutoPlay(), "Carousel auto-play resumed")
                }

                else -> {
                    Log.d { "Unrecognized carousel command: $commandText" }
                    HandlerResult.notHandled()
                }
            }
        } catch (e: Exception) {
            Log.e({ "Error processing carousel command: $commandText" }, e)
            HandlerResult.failure("Error: ${e.message}", recoverable = true)
        }
    }

    /**
     * Convert executor result to HandlerResult
     */
    private fun handleResult(result: CarouselResult, successMessage: String): HandlerResult {
        return when (result) {
            is CarouselResult.Success -> HandlerResult.Success(
                message = result.message ?: successMessage,
                data = result.data ?: emptyMap()
            )
            is CarouselResult.Error -> HandlerResult.failure(
                reason = result.message,
                recoverable = result.recoverable
            )
            is CarouselResult.NotFound -> HandlerResult.failure(
                reason = result.message,
                recoverable = true
            )
            is CarouselResult.NoAccessibility -> HandlerResult.failure(
                reason = "Accessibility service not available",
                recoverable = false
            )
        }
    }

    /**
     * Extract slide number from command text
     * Supports both numeric ("5") and word forms ("five", "fifth")
     */
    private fun extractSlideNumber(text: String): Int? {
        val trimmed = text.trim().lowercase()

        // Try direct numeric parsing
        trimmed.toIntOrNull()?.let { return it }

        // Try word-to-number mapping
        WORD_TO_NUMBER[trimmed]?.let { return it }

        // Try to extract number from text like "number 5" or "5th slide"
        val numberPattern = Regex("\\d+")
        numberPattern.find(trimmed)?.value?.toIntOrNull()?.let { return it }

        return null
    }
}

/**
 * Result sealed class for carousel operations
 */
sealed class CarouselResult {
    data class Success(
        val message: String? = null,
        val data: Map<String, Any>? = null
    ) : CarouselResult()

    data class Error(
        val message: String,
        val recoverable: Boolean = true
    ) : CarouselResult()

    data class NotFound(
        val message: String = "Carousel component not found"
    ) : CarouselResult()

    data object NoAccessibility : CarouselResult()
}

/**
 * Executor interface for carousel operations
 * Platform-specific implementations handle accessibility interactions
 */
interface CarouselExecutor {
    /**
     * Navigate to the next slide
     */
    suspend fun navigateToNext(): CarouselResult

    /**
     * Navigate to the previous slide
     */
    suspend fun navigateToPrevious(): CarouselResult

    /**
     * Navigate to a specific slide by number (1-indexed)
     */
    suspend fun navigateToSlide(slideNumber: Int): CarouselResult

    /**
     * Navigate to the first slide
     */
    suspend fun navigateToFirst(): CarouselResult

    /**
     * Navigate to the last slide
     */
    suspend fun navigateToLast(): CarouselResult

    /**
     * Pause carousel auto-play
     */
    suspend fun pauseAutoPlay(): CarouselResult

    /**
     * Resume carousel auto-play
     */
    suspend fun resumeAutoPlay(): CarouselResult

    /**
     * Get current carousel status
     */
    fun getStatus(): CarouselStatus
}

/**
 * Carousel status data class
 */
data class CarouselStatus(
    val currentSlideIndex: Int,
    val totalSlides: Int,
    val isAutoPlaying: Boolean,
    val hasAccessibilityService: Boolean
)
