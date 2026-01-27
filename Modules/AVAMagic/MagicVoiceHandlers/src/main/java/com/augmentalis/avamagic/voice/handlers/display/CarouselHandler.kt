/**
 * CarouselHandler.kt
 *
 * Created: 2026-01-27 PST
 * Last Modified: 2026-01-27 PST
 * Author: VOS4 Development Team
 * Version: 1.0.0
 *
 * Purpose: Voice command handler for carousel/slider UI components
 * Features: Navigation, slide jumping, auto-play control
 * Location: CommandManager module handlers
 *
 * Changelog:
 * - v1.0.0 (2026-01-27): Initial implementation for carousel voice control
 */

package com.augmentalis.avamagic.voice.handlers.display

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.commandmanager.CommandHandler
import com.augmentalis.commandmanager.CommandRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

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
 * - Implements CommandHandler interface for CommandRegistry integration
 * - Uses accessibility APIs to interact with carousel components
 * - Thread-safe singleton pattern
 * - Auto-registers with CommandRegistry on initialization
 *
 * @since 1.0.0
 */
class CarouselHandler private constructor(
    private val context: Context
) : CommandHandler {

    companion object {
        private const val TAG = "CarouselHandler"
        private const val MODULE_ID = "carousel"

        @Volatile
        private var instance: CarouselHandler? = null

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): CarouselHandler {
            return instance ?: synchronized(this) {
                instance ?: CarouselHandler(context.applicationContext).also {
                    instance = it
                }
            }
        }

        // Command pattern constants
        private const val NEXT_PREFIX = "next"
        private const val PREVIOUS_PREFIX = "previous"
        private const val BACK_PREFIX = "back"
        private const val GO_TO_PREFIX = "go to slide"
        private const val SLIDE_PREFIX = "slide"
        private const val FIRST_PREFIX = "first"
        private const val LAST_PREFIX = "last"
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

    // CommandHandler interface implementation
    override val moduleId: String = MODULE_ID

    override val supportedCommands: List<String> = listOf(
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

    private val handlerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // State tracking
    private var isInitialized = false
    private var currentSlideIndex = 0
    private var totalSlides = 0
    private var isAutoPlaying = false

    // Cached accessibility service reference
    @Volatile
    private var accessibilityService: AccessibilityService? = null

    init {
        initialize()
        // Register with CommandRegistry automatically
        CommandRegistry.registerHandler(moduleId, this)
    }

    /**
     * Initialize the carousel handler
     */
    fun initialize(): Boolean {
        if (isInitialized) {
            Log.w(TAG, "Already initialized")
            return true
        }

        return try {
            isInitialized = true
            Log.d(TAG, "CarouselHandler initialized with ${supportedCommands.size} commands")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize", e)
            false
        }
    }

    /**
     * Set the accessibility service reference for carousel interactions
     */
    fun setAccessibilityService(service: AccessibilityService?) {
        accessibilityService = service
        Log.d(TAG, "Accessibility service ${if (service != null) "set" else "cleared"}")
    }

    /**
     * CommandHandler interface: Check if this handler can process the command
     * Commands are already normalized (lowercase, trimmed) by CommandRegistry
     */
    override fun canHandle(command: String): Boolean {
        return when {
            // Navigation commands
            command == "next slide" || command == "next" -> true
            command == "previous slide" || command == "previous" || command == "back" -> true

            // Jump commands
            command.startsWith(GO_TO_PREFIX) -> true
            command.startsWith(SLIDE_PREFIX) && command != SLIDE_PREFIX -> true
            command == "first slide" || command == "last slide" -> true

            // Playback commands
            command == PAUSE_COMMAND || command == STOP_COMMAND -> true
            command == PLAY_COMMAND || command == RESUME_COMMAND -> true

            else -> false
        }
    }

    /**
     * CommandHandler interface: Execute the command
     * Commands are already normalized (lowercase, trimmed) by CommandRegistry
     */
    override suspend fun handleCommand(command: String): Boolean {
        if (!isInitialized) {
            Log.w(TAG, "Handler not initialized")
            return false
        }

        Log.d(TAG, "Processing carousel command: '$command'")

        return try {
            when {
                // Next slide
                command == "next slide" || command == "next" -> {
                    navigateToNext()
                }

                // Previous slide
                command == "previous slide" || command == "previous" || command == "back" -> {
                    navigateToPrevious()
                }

                // Go to specific slide: "go to slide N"
                command.startsWith(GO_TO_PREFIX) -> {
                    val slideNumber = extractSlideNumber(command.removePrefix(GO_TO_PREFIX).trim())
                    if (slideNumber != null) {
                        navigateToSlide(slideNumber)
                    } else {
                        Log.w(TAG, "Could not parse slide number from: $command")
                        false
                    }
                }

                // Slide N shorthand: "slide N"
                command.startsWith(SLIDE_PREFIX) && command != SLIDE_PREFIX -> {
                    val slideNumber = extractSlideNumber(command.removePrefix(SLIDE_PREFIX).trim())
                    if (slideNumber != null) {
                        navigateToSlide(slideNumber)
                    } else {
                        Log.w(TAG, "Could not parse slide number from: $command")
                        false
                    }
                }

                // First slide
                command == "first slide" -> {
                    navigateToFirst()
                }

                // Last slide
                command == "last slide" -> {
                    navigateToLast()
                }

                // Pause/stop auto-play
                command == PAUSE_COMMAND || command == STOP_COMMAND -> {
                    pauseAutoPlay()
                }

                // Play/resume auto-play
                command == PLAY_COMMAND || command == RESUME_COMMAND -> {
                    resumeAutoPlay()
                }

                else -> {
                    Log.d(TAG, "Unrecognized carousel command: $command")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing carousel command: $command", e)
            false
        }
    }

    /**
     * Navigate to the next slide
     */
    private suspend fun navigateToNext(): Boolean {
        Log.d(TAG, "Navigating to next slide")

        val service = accessibilityService ?: run {
            Log.w(TAG, "No accessibility service available")
            return false
        }

        // Find carousel node and perform scroll forward
        val carouselNode = findCarouselNode(service)
        if (carouselNode != null) {
            val result = performScrollForward(carouselNode)
            carouselNode.recycle()
            if (result) {
                currentSlideIndex++
                Log.d(TAG, "Navigated to next slide (index: $currentSlideIndex)")
            }
            return result
        }

        // Fallback: try swipe left gesture for horizontal carousels
        return performSwipeGesture(service, SwipeDirection.LEFT)
    }

    /**
     * Navigate to the previous slide
     */
    private suspend fun navigateToPrevious(): Boolean {
        Log.d(TAG, "Navigating to previous slide")

        val service = accessibilityService ?: run {
            Log.w(TAG, "No accessibility service available")
            return false
        }

        // Find carousel node and perform scroll backward
        val carouselNode = findCarouselNode(service)
        if (carouselNode != null) {
            val result = performScrollBackward(carouselNode)
            carouselNode.recycle()
            if (result) {
                currentSlideIndex = maxOf(0, currentSlideIndex - 1)
                Log.d(TAG, "Navigated to previous slide (index: $currentSlideIndex)")
            }
            return result
        }

        // Fallback: try swipe right gesture for horizontal carousels
        return performSwipeGesture(service, SwipeDirection.RIGHT)
    }

    /**
     * Navigate to a specific slide by number (1-indexed for user convenience)
     */
    private suspend fun navigateToSlide(slideNumber: Int): Boolean {
        Log.d(TAG, "Navigating to slide $slideNumber")

        if (slideNumber < 1) {
            Log.w(TAG, "Invalid slide number: $slideNumber")
            return false
        }

        val service = accessibilityService ?: run {
            Log.w(TAG, "No accessibility service available")
            return false
        }

        // Convert to 0-indexed
        val targetIndex = slideNumber - 1

        // Try to find carousel and navigate
        val carouselNode = findCarouselNode(service)
        if (carouselNode != null) {
            val result = navigateToIndex(carouselNode, targetIndex)
            carouselNode.recycle()
            if (result) {
                currentSlideIndex = targetIndex
                Log.d(TAG, "Navigated to slide $slideNumber (index: $targetIndex)")
            }
            return result
        }

        // Fallback: sequential navigation
        return navigateSequentially(service, targetIndex)
    }

    /**
     * Navigate to the first slide
     */
    private suspend fun navigateToFirst(): Boolean {
        Log.d(TAG, "Navigating to first slide")

        val service = accessibilityService ?: run {
            Log.w(TAG, "No accessibility service available")
            return false
        }

        // Try to scroll to beginning
        val carouselNode = findCarouselNode(service)
        if (carouselNode != null) {
            // Scroll backward multiple times to reach beginning
            var scrolledToStart = false
            repeat(20) { // Max 20 attempts to prevent infinite loop
                if (!performScrollBackward(carouselNode)) {
                    scrolledToStart = true
                    return@repeat
                }
            }
            carouselNode.recycle()
            currentSlideIndex = 0
            Log.d(TAG, "Navigated to first slide")
            return true
        }

        // Fallback: multiple right swipes
        repeat(20) {
            performSwipeGesture(service, SwipeDirection.RIGHT)
        }
        currentSlideIndex = 0
        return true
    }

    /**
     * Navigate to the last slide
     */
    private suspend fun navigateToLast(): Boolean {
        Log.d(TAG, "Navigating to last slide")

        val service = accessibilityService ?: run {
            Log.w(TAG, "No accessibility service available")
            return false
        }

        // Try to scroll to end
        val carouselNode = findCarouselNode(service)
        if (carouselNode != null) {
            // Scroll forward multiple times to reach end
            var lastScrollSucceeded = true
            var scrollCount = 0
            while (lastScrollSucceeded && scrollCount < 100) { // Safety limit
                lastScrollSucceeded = performScrollForward(carouselNode)
                if (lastScrollSucceeded) scrollCount++
            }
            carouselNode.recycle()
            currentSlideIndex = scrollCount
            Log.d(TAG, "Navigated to last slide (approximately slide ${scrollCount + 1})")
            return true
        }

        // Fallback: multiple left swipes
        repeat(100) {
            if (!performSwipeGesture(service, SwipeDirection.LEFT)) {
                return@repeat
            }
        }
        return true
    }

    /**
     * Pause auto-play functionality
     */
    private suspend fun pauseAutoPlay(): Boolean {
        Log.d(TAG, "Pausing carousel auto-play")

        val service = accessibilityService ?: run {
            Log.w(TAG, "No accessibility service available")
            return false
        }

        // Try to find and click pause button
        val rootNode = service.rootInActiveWindow ?: return false
        val pauseButton = findPauseButton(rootNode)

        return if (pauseButton != null) {
            val result = pauseButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            pauseButton.recycle()
            rootNode.recycle()
            if (result) {
                isAutoPlaying = false
                Log.d(TAG, "Auto-play paused")
            }
            result
        } else {
            rootNode.recycle()
            // Even if no button found, set state
            isAutoPlaying = false
            Log.d(TAG, "No pause button found, but state set to paused")
            true
        }
    }

    /**
     * Resume auto-play functionality
     */
    private suspend fun resumeAutoPlay(): Boolean {
        Log.d(TAG, "Resuming carousel auto-play")

        val service = accessibilityService ?: run {
            Log.w(TAG, "No accessibility service available")
            return false
        }

        // Try to find and click play button
        val rootNode = service.rootInActiveWindow ?: return false
        val playButton = findPlayButton(rootNode)

        return if (playButton != null) {
            val result = playButton.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            playButton.recycle()
            rootNode.recycle()
            if (result) {
                isAutoPlaying = true
                Log.d(TAG, "Auto-play resumed")
            }
            result
        } else {
            rootNode.recycle()
            // Even if no button found, set state
            isAutoPlaying = true
            Log.d(TAG, "No play button found, but state set to playing")
            true
        }
    }

    // ============== Helper Methods ==============

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

    /**
     * Find carousel/ViewPager node in the accessibility tree
     */
    private fun findCarouselNode(service: AccessibilityService): AccessibilityNodeInfo? {
        val rootNode = service.rootInActiveWindow ?: return null

        // Look for common carousel class names
        val carouselClassNames = listOf(
            "androidx.viewpager2.widget.ViewPager2",
            "androidx.viewpager.widget.ViewPager",
            "android.support.v4.view.ViewPager",
            "androidx.recyclerview.widget.RecyclerView",
            "android.widget.HorizontalScrollView"
        )

        for (className in carouselClassNames) {
            val node = findNodeByClassName(rootNode, className)
            if (node != null) {
                rootNode.recycle()
                return node
            }
        }

        // Also try finding by scrollable property with horizontal scrolling
        val scrollableNode = findHorizontallyScrollableNode(rootNode)
        if (scrollableNode != null) {
            rootNode.recycle()
            return scrollableNode
        }

        rootNode.recycle()
        return null
    }

    /**
     * Find node by class name
     */
    private fun findNodeByClassName(
        node: AccessibilityNodeInfo,
        className: String
    ): AccessibilityNodeInfo? {
        if (node.className?.toString() == className) {
            return AccessibilityNodeInfo.obtain(node)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findNodeByClassName(child, className)
            if (found != null) {
                child.recycle()
                return found
            }
            child.recycle()
        }

        return null
    }

    /**
     * Find horizontally scrollable node
     */
    private fun findHorizontallyScrollableNode(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        if (node.isScrollable) {
            // Check if it supports horizontal scrolling actions
            val actions = node.actionList
            val hasScrollForward = actions.any { it.id == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD }
            val hasScrollBackward = actions.any { it.id == AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD }

            if (hasScrollForward || hasScrollBackward) {
                return AccessibilityNodeInfo.obtain(node)
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findHorizontallyScrollableNode(child)
            if (found != null) {
                child.recycle()
                return found
            }
            child.recycle()
        }

        return null
    }

    /**
     * Find pause button in the accessibility tree
     */
    private fun findPauseButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val pauseLabels = listOf("pause", "stop", "halt")
        return findButtonByLabels(rootNode, pauseLabels)
    }

    /**
     * Find play button in the accessibility tree
     */
    private fun findPlayButton(rootNode: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val playLabels = listOf("play", "resume", "start")
        return findButtonByLabels(rootNode, playLabels)
    }

    /**
     * Find button by content description or text
     */
    private fun findButtonByLabels(
        node: AccessibilityNodeInfo,
        labels: List<String>
    ): AccessibilityNodeInfo? {
        val nodeText = node.text?.toString()?.lowercase() ?: ""
        val contentDesc = node.contentDescription?.toString()?.lowercase() ?: ""

        for (label in labels) {
            if (nodeText.contains(label) || contentDesc.contains(label)) {
                if (node.isClickable) {
                    return AccessibilityNodeInfo.obtain(node)
                }
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val found = findButtonByLabels(child, labels)
            if (found != null) {
                child.recycle()
                return found
            }
            child.recycle()
        }

        return null
    }

    /**
     * Perform scroll forward action on a node
     */
    private fun performScrollForward(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
    }

    /**
     * Perform scroll backward action on a node
     */
    private fun performScrollBackward(node: AccessibilityNodeInfo): Boolean {
        return node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
    }

    /**
     * Navigate to specific index using accessibility actions
     */
    private fun navigateToIndex(node: AccessibilityNodeInfo, targetIndex: Int): Boolean {
        // For ViewPager2, we can try to use setCurrentItem via accessibility
        // This is typically done through sequential scrolling

        val currentIndex = currentSlideIndex
        val direction = if (targetIndex > currentIndex) 1 else -1
        val steps = kotlin.math.abs(targetIndex - currentIndex)

        repeat(steps) {
            if (direction > 0) {
                if (!performScrollForward(node)) return false
            } else {
                if (!performScrollBackward(node)) return false
            }
        }

        return true
    }

    /**
     * Navigate sequentially when no carousel node is found
     */
    private fun navigateSequentially(service: AccessibilityService, targetIndex: Int): Boolean {
        val currentIndex = currentSlideIndex
        val direction = if (targetIndex > currentIndex) SwipeDirection.LEFT else SwipeDirection.RIGHT
        val steps = kotlin.math.abs(targetIndex - currentIndex)

        repeat(steps) {
            performSwipeGesture(service, direction)
        }

        currentSlideIndex = targetIndex
        return true
    }

    /**
     * Perform swipe gesture using GestureDescription
     */
    private fun performSwipeGesture(
        service: AccessibilityService,
        direction: SwipeDirection
    ): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Log.w(TAG, "Gesture dispatch requires API 24+")
            return false
        }

        val displayMetrics = context.resources.displayMetrics
        val width = displayMetrics.widthPixels.toFloat()
        val height = displayMetrics.heightPixels.toFloat()
        val centerY = height / 2f

        val (startX, endX) = when (direction) {
            SwipeDirection.LEFT -> Pair(width * 0.8f, width * 0.2f)
            SwipeDirection.RIGHT -> Pair(width * 0.2f, width * 0.8f)
        }

        val path = android.graphics.Path().apply {
            moveTo(startX, centerY)
            lineTo(endX, centerY)
        }

        val gesture = android.accessibilityservice.GestureDescription.Builder()
            .addStroke(
                android.accessibilityservice.GestureDescription.StrokeDescription(
                    path,
                    0,
                    300L
                )
            )
            .build()

        return service.dispatchGesture(gesture, null, null)
    }

    /**
     * Get current carousel status
     */
    fun getStatus(): CarouselStatus {
        return CarouselStatus(
            isInitialized = isInitialized,
            currentSlideIndex = currentSlideIndex,
            totalSlides = totalSlides,
            isAutoPlaying = isAutoPlaying,
            hasAccessibilityService = accessibilityService != null
        )
    }

    /**
     * Check if handler is ready for operation
     */
    fun isReady(): Boolean = isInitialized && accessibilityService != null

    /**
     * Cleanup resources
     */
    fun dispose() {
        CommandRegistry.unregisterHandler(moduleId)
        handlerScope.cancel()
        accessibilityService = null
        isInitialized = false
        instance = null
        Log.d(TAG, "CarouselHandler disposed")
    }
}

/**
 * Swipe direction enum
 */
private enum class SwipeDirection {
    LEFT,
    RIGHT
}

/**
 * Carousel status data class
 */
data class CarouselStatus(
    val isInitialized: Boolean,
    val currentSlideIndex: Int,
    val totalSlides: Int,
    val isAutoPlaying: Boolean,
    val hasAccessibilityService: Boolean
)
