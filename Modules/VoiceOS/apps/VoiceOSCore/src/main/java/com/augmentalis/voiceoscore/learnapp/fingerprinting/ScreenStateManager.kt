/**
 * ScreenStateManager.kt - Manages screen states and visited tracking
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-10-08
 *
 * Manages screen state tracking during exploration
 */

package com.augmentalis.voiceoscore.learnapp.fingerprinting

import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import com.augmentalis.voiceoscore.learnapp.models.ScreenState
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Screen State Manager
 *
 * Manages screen state tracking during exploration.
 * Tracks visited screens, detects transitions, and manages state history.
 */
class ScreenStateManager {
    companion object {
        private const val TAG = "ScreenStateManager"
    }

    private val fingerprinter = ScreenFingerprinter()
    private val visitedScreens = mutableSetOf<String>()
    private val screenStates = mutableMapOf<String, ScreenState>()
    private val screenHistory = mutableListOf<String>()
    private val mutex = Mutex()
    private var currentScreenHash: String? = null

    /**
     * Capture screen state
     */
    suspend fun captureScreenState(
        rootNode: AccessibilityNodeInfo?,
        packageName: String,
        depth: Int = 0
    ): ScreenState = mutex.withLock {
        if (rootNode == null) {
            return@withLock createEmptyScreenState(packageName)
        }

        val hash = fingerprinter.calculateFingerprint(rootNode)

        val existingState = screenStates[hash]
        if (existingState != null) {
            currentScreenHash = hash
            return@withLock existingState
        }

        val existingSimilarScreen = findRecentSimilarScreen(hash, packageName)
        if (existingSimilarScreen != null) {
            Log.d(TAG, "Screen $hash is similar to existing ${existingSimilarScreen.hash}")
            currentScreenHash = existingSimilarScreen.hash
            return@withLock existingSimilarScreen.copy(
                timestamp = System.currentTimeMillis(),
                depth = depth
            )
        }

        val elementCount = countElements(rootNode)
        val activityName = extractActivityName(rootNode)

        val state = ScreenState(
            hash = hash,
            packageName = packageName,
            activityName = activityName,
            timestamp = System.currentTimeMillis(),
            elementCount = elementCount,
            isVisited = visitedScreens.contains(hash),
            depth = depth
        )

        screenStates[hash] = state
        currentScreenHash = hash

        if (screenHistory.isEmpty() || screenHistory.last() != hash) {
            screenHistory.add(hash)
        }

        return@withLock state
    }

    fun isVisited(hash: String): Boolean = visitedScreens.contains(hash)

    suspend fun markAsVisited(hash: String) = mutex.withLock {
        visitedScreens.add(hash)
        screenStates[hash]?.let { state ->
            screenStates[hash] = state.markAsVisited()
        }
    }

    fun getScreenState(hash: String): ScreenState? = screenStates[hash]

    fun getCurrentScreenHash(): String? = currentScreenHash

    fun getVisitedScreens(): Set<String> = visitedScreens.toSet()

    fun getAllScreenStates(): Map<String, ScreenState> = screenStates.toMap()

    fun getScreenHistory(): List<String> = screenHistory.toList()

    suspend fun waitForScreenTransition(
        previousHash: String,
        timeoutMs: Long = 5000L
    ): String? {
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val currentHash = currentScreenHash
            if (currentHash != null && currentHash != previousHash) {
                return currentHash
            }
            delay(100)
        }

        return null
    }

    suspend fun hasScreenChanged(rootNode: AccessibilityNodeInfo?): Boolean {
        if (rootNode == null) return false
        val newHash = fingerprinter.calculateFingerprint(rootNode)
        return newHash != currentScreenHash
    }

    fun getStats(): ScreenStateStats {
        return ScreenStateStats(
            totalScreensDiscovered = screenStates.size,
            totalScreensVisited = visitedScreens.size,
            currentDepth = screenHistory.size,
            averageElementsPerScreen = if (screenStates.isEmpty()) 0f else {
                screenStates.values.map { it.elementCount }.average().toFloat()
            }
        )
    }

    suspend fun clear() = mutex.withLock {
        visitedScreens.clear()
        screenStates.clear()
        screenHistory.clear()
        currentScreenHash = null
    }

    fun areScreensSimilar(
        hash1: String,
        hash2: String,
        similarityThreshold: Double = 0.85
    ): Boolean {
        if (hash1 == hash2) return true
        val hash1Prefix = hash1.take(16)
        val hash2Prefix = hash2.take(16)
        return hash1Prefix == hash2Prefix
    }

    private fun findRecentSimilarScreen(
        newHash: String,
        packageName: String
    ): ScreenState? {
        val recentScreens = screenStates.values
            .filter { it.packageName == packageName }
            .sortedByDescending { it.timestamp }
            .take(10)

        for (recentScreen in recentScreens) {
            val isSimilar = areScreensSimilar(newHash, recentScreen.hash, 0.90)
            if (isSimilar) {
                return recentScreen
            }
        }

        return null
    }

    private fun countElements(rootNode: AccessibilityNodeInfo): Int {
        var count = 1

        for (i in 0 until rootNode.childCount) {
            rootNode.getChild(i)?.let { child ->
                count += countElements(child)
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    @Suppress("DEPRECATION")
                    child.recycle()
                }
            }
        }

        return count
    }

    private fun extractActivityName(rootNode: AccessibilityNodeInfo): String? {
        return rootNode.window?.title?.toString()
    }

    private fun createEmptyScreenState(packageName: String): ScreenState {
        return ScreenState(
            hash = "empty",
            packageName = packageName,
            activityName = null,
            timestamp = System.currentTimeMillis(),
            elementCount = 0,
            isVisited = false,
            depth = 0
        )
    }
}

/**
 * Screen State Statistics
 */
data class ScreenStateStats(
    val totalScreensDiscovered: Int,
    val totalScreensVisited: Int,
    val currentDepth: Int,
    val averageElementsPerScreen: Float
)
