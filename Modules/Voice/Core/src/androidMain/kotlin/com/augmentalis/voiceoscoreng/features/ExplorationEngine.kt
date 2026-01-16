package com.augmentalis.voiceoscoreng.features

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkDetector
import com.augmentalis.voiceoscoreng.common.FrameworkInfo
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle

/**
 * Engine for exploring and learning app UIs.
 *
 * The ExplorationEngine performs batch exploration of apps,
 * capturing screen states and building a comprehensive map
 * of the app's UI elements.
 *
 * Note: This feature is only available in DEV tier.
 */
class ExplorationEngine {

    private var running = false
    private var paused = false
    private var currentPackage: String? = null
    private var screenCount = 0
    private var totalElements = 0
    private val screenStates = mutableListOf<ScreenState>()

    /**
     * Check if exploration feature is available.
     */
    fun isAvailable(): Boolean {
        return LearnAppDevToggle.isEnabled(LearnAppDevToggle.Feature.EXPLORATION_MODE)
    }

    /**
     * Check if exploration is currently running.
     */
    fun isRunning(): Boolean = running

    /**
     * Check if exploration is paused.
     */
    fun isPaused(): Boolean = paused

    /**
     * Start exploring an app.
     */
    fun start(packageName: String) {
        if (!isAvailable()) return

        currentPackage = packageName
        running = true
        paused = false
        screenCount = 0
        totalElements = 0
        screenStates.clear()
    }

    /**
     * Stop the exploration.
     */
    fun stop() {
        running = false
        paused = false
        currentPackage = null
    }

    /**
     * Pause the exploration.
     */
    fun pause() {
        if (running) {
            paused = true
        }
    }

    /**
     * Resume a paused exploration.
     */
    fun resume() {
        if (running) {
            paused = false
        }
    }

    /**
     * Get the current package being explored.
     */
    fun getCurrentPackage(): String? = currentPackage

    /**
     * Capture the current screen state.
     */
    fun captureScreen(elements: List<ElementInfo>): ScreenState {
        val state = ScreenState(
            packageName = currentPackage ?: "",
            screenId = screenCount,
            elements = elements,
            timestamp = System.currentTimeMillis()
        )

        screenStates.add(state)
        screenCount++
        totalElements += elements.size

        return state
    }

    /**
     * Get the number of screens captured.
     */
    fun getScreenCount(): Int = screenCount

    /**
     * Get the total number of elements captured.
     */
    fun getTotalElements(): Int = totalElements

    /**
     * Detect the framework used by the app.
     */
    fun detectFramework(elements: List<ElementInfo>): FrameworkInfo {
        val classNames = elements.map { it.className }
        return FrameworkDetector.detect(currentPackage ?: "", classNames)
    }

    /**
     * Get the exploration progress (0-1).
     */
    fun getProgress(): Float {
        if (!running || screenCount == 0) return 0f
        // Simple progress based on screens captured
        return minOf(screenCount.toFloat() / 10f, 1f)
    }

    /**
     * Reset the engine state.
     */
    fun reset() {
        stop()
        screenCount = 0
        totalElements = 0
        screenStates.clear()
    }

    /**
     * Get all captured screen states.
     */
    fun getScreenStates(): List<ScreenState> = screenStates.toList()
}

/**
 * Represents a captured screen state.
 */
data class ScreenState(
    val packageName: String,
    val screenId: Int,
    val elements: List<ElementInfo>,
    val timestamp: Long
)
