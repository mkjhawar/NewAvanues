/**
 * ExplorationEngine.kt - Screen exploration engine for VoiceOSCoreNG
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2026-01-08
 *
 * Explores app screens, captures UI elements, and detects frameworks.
 * Used for learning app structure and generating voice commands.
 */
package com.augmentalis.voiceoscoreng.exploration

import com.augmentalis.voiceoscoreng.common.ElementInfo
import com.augmentalis.voiceoscoreng.common.FrameworkDetector
import com.augmentalis.voiceoscoreng.common.FrameworkInfo
import com.augmentalis.voiceoscoreng.common.FrameworkType
import com.augmentalis.voiceoscoreng.features.LearnAppDevToggle
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Screen state captured during exploration.
 *
 * @property packageName The package being explored
 * @property elements UI elements found on the screen
 * @property screenHash Unique hash for this screen state
 * @property timestamp When the capture occurred
 * @property frameworkType Detected framework type
 */
data class ScreenState(
    val packageName: String,
    val elements: List<ElementInfo>,
    val screenHash: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val frameworkType: FrameworkType = FrameworkType.NATIVE
) {
    /**
     * Get count of actionable elements.
     */
    val actionableCount: Int
        get() = elements.count { it.isActionable }

    /**
     * Get count of elements with voice content.
     */
    val voiceContentCount: Int
        get() = elements.count { it.hasVoiceContent }
}

/**
 * Exploration statistics.
 */
data class ExplorationStats(
    val screenCount: Int,
    val totalElements: Int,
    val uniqueElements: Int,
    val actionableElements: Int,
    val durationMs: Long
)

/**
 * Engine for exploring app screens and capturing UI elements.
 *
 * This engine:
 * - Tracks exploration state (running, paused, stopped)
 * - Captures screen states with elements
 * - Detects app frameworks (Flutter, Compose, Native, etc.)
 * - Tracks exploration progress and statistics
 *
 * Usage:
 * ```kotlin
 * val engine = ExplorationEngine()
 * engine.start("com.example.app")
 *
 * // When accessibility event fires
 * val elements = extractElements(rootNode)
 * val screenState = engine.captureScreen(elements)
 *
 * // Check progress
 * val progress = engine.getProgress()
 *
 * // When done
 * engine.stop()
 * ```
 */
class ExplorationEngine {

    // State
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val currentPackage = AtomicReference<String?>(null)

    // Statistics
    private val screenCount = AtomicInteger(0)
    private val totalElements = AtomicInteger(0)
    private val startTime = AtomicLong(0)

    // Screen tracking - stores screen hashes to detect unique screens
    private val screenHashes = ConcurrentHashMap.newKeySet<String>()

    // Element tracking - stores element VUIDs to detect unique elements
    private val elementVuids = ConcurrentHashMap.newKeySet<String>()

    // Actionable element count
    private val actionableElements = AtomicInteger(0)

    // Detected framework
    private val detectedFramework = AtomicReference<FrameworkInfo?>(null)

    /**
     * Check if exploration is currently running.
     */
    fun isRunning(): Boolean = running.get()

    /**
     * Check if exploration is paused.
     */
    fun isPaused(): Boolean = paused.get()

    /**
     * Start exploration for a package.
     *
     * @param packageName The package to explore
     */
    fun start(packageName: String) {
        currentPackage.set(packageName)
        running.set(true)
        paused.set(false)
        startTime.set(System.currentTimeMillis())
    }

    /**
     * Stop exploration.
     */
    fun stop() {
        running.set(false)
        paused.set(false)
    }

    /**
     * Pause exploration temporarily.
     */
    fun pause() {
        if (running.get()) {
            paused.set(true)
        }
    }

    /**
     * Resume paused exploration.
     */
    fun resume() {
        if (running.get()) {
            paused.set(false)
        }
    }

    /**
     * Get the current package being explored.
     *
     * @return Package name if running, null otherwise
     */
    fun getCurrentPackage(): String? = if (running.get()) currentPackage.get() else null

    /**
     * Capture screen with elements.
     *
     * @param elements List of UI elements on the current screen
     * @return ScreenState containing the captured information
     */
    fun captureScreen(elements: List<ElementInfo>): ScreenState {
        val pkg = currentPackage.get() ?: ""

        // Generate screen hash from element composition
        val screenHash = generateScreenHash(elements)

        // Track unique screens
        screenHashes.add(screenHash)

        // Update statistics
        screenCount.incrementAndGet()
        totalElements.addAndGet(elements.size)

        // Track unique and actionable elements
        elements.forEach { element ->
            val vuid = generateElementVuid(element)
            if (elementVuids.add(vuid) && element.isActionable) {
                actionableElements.incrementAndGet()
            }
        }

        // Detect framework if not yet detected
        if (detectedFramework.get() == null && elements.isNotEmpty()) {
            detectedFramework.set(detectFramework(elements))
        }

        return ScreenState(
            packageName = pkg,
            elements = elements,
            screenHash = screenHash,
            frameworkType = detectedFramework.get()?.type ?: FrameworkType.NATIVE
        )
    }

    /**
     * Get number of screens captured.
     */
    fun getScreenCount(): Int = screenCount.get()

    /**
     * Get total elements captured (including duplicates).
     */
    fun getTotalElements(): Int = totalElements.get()

    /**
     * Get unique element count.
     */
    fun getUniqueElements(): Int = elementVuids.size

    /**
     * Get unique screen count.
     */
    fun getUniqueScreens(): Int = screenHashes.size

    /**
     * Detect framework from elements.
     *
     * @param elements List of elements to analyze
     * @return FrameworkInfo with detected type and confidence
     */
    fun detectFramework(elements: List<ElementInfo>): FrameworkInfo {
        val pkg = currentPackage.get() ?: ""
        val classNames = elements.map { it.className }
        return FrameworkDetector.detect(pkg, classNames)
    }

    /**
     * Check if exploration feature is available.
     * Requires DEV tier.
     */
    fun isAvailable(): Boolean {
        return LearnAppDevToggle.getCurrentTier() == LearnAppDevToggle.Tier.DEV
    }

    /**
     * Get exploration progress (0.0 - 1.0).
     *
     * Progress is estimated based on screens captured.
     * More screens = higher progress, capped at 1.0.
     */
    fun getProgress(): Float {
        val screens = screenCount.get()
        if (screens == 0) return 0f

        // Estimate: 10 unique screens = 100% progress
        // Adjust based on unique screens for better accuracy
        val uniqueScreens = screenHashes.size
        return minOf(1f, uniqueScreens / 10f)
    }

    /**
     * Get exploration statistics.
     */
    fun getStats(): ExplorationStats {
        val duration = if (startTime.get() > 0) {
            System.currentTimeMillis() - startTime.get()
        } else {
            0L
        }

        return ExplorationStats(
            screenCount = screenCount.get(),
            totalElements = totalElements.get(),
            uniqueElements = elementVuids.size,
            actionableElements = actionableElements.get(),
            durationMs = duration
        )
    }

    /**
     * Reset all state.
     */
    fun reset() {
        running.set(false)
        paused.set(false)
        currentPackage.set(null)
        screenCount.set(0)
        totalElements.set(0)
        startTime.set(0)
        screenHashes.clear()
        elementVuids.clear()
        actionableElements.set(0)
        detectedFramework.set(null)
    }

    /**
     * Generate a hash for the current screen based on element composition.
     */
    private fun generateScreenHash(elements: List<ElementInfo>): String {
        val hashInput = elements
            .sortedBy { "${it.bounds.left},${it.bounds.top}" }
            .joinToString("|") { "${it.className}:${it.resourceId}:${it.bounds}" }

        return hashInput.hashCode().toString(16)
    }

    /**
     * Generate a unique identifier for an element.
     */
    private fun generateElementVuid(element: ElementInfo): String {
        return "${element.className}:${element.resourceId}:${element.text}:${element.bounds}".hashCode().toString(16)
    }
}
