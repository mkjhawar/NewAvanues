/**
 * ScrollExecutor.kt - Executes scroll actions during exploration
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2025-12-17
 *
 * Executes scroll actions for exploring scrollable content.
 */
package com.augmentalis.voiceoscore.learnapp.exploration

import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.delay

/**
 * Scroll Executor
 *
 * Executes scroll actions for exploring scrollable content.
 */
class ScrollExecutor {

    /**
     * Scroll forward
     */
    suspend fun scrollForward(area: ScrollableArea): ScrollResult {
        return try {
            val success = area.node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD)
            if (success) {
                delay(SCROLL_DELAY_MS)
                ScrollResult.Success
            } else {
                ScrollResult.Failed("Scroll forward action failed")
            }
        } catch (e: Exception) {
            ScrollResult.Failed("Error scrolling: ${e.message}")
        }
    }

    /**
     * Scroll backward
     */
    suspend fun scrollBackward(area: ScrollableArea): ScrollResult {
        return try {
            val success = area.node.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD)
            if (success) {
                delay(SCROLL_DELAY_MS)
                ScrollResult.Success
            } else {
                ScrollResult.Failed("Scroll backward action failed")
            }
        } catch (e: Exception) {
            ScrollResult.Failed("Error scrolling: ${e.message}")
        }
    }

    /**
     * Scroll to beginning
     */
    suspend fun scrollToBeginning(area: ScrollableArea, maxScrolls: Int = 10): ScrollResult {
        var scrollCount = 0
        while (area.canScrollBackward && scrollCount < maxScrolls) {
            val result = scrollBackward(area)
            if (result is ScrollResult.Failed) {
                break
            }
            scrollCount++
        }
        return ScrollResult.Success
    }

    /**
     * Scroll through all content
     */
    suspend fun scrollThroughAll(
        area: ScrollableArea,
        maxScrolls: Int = 20,
        onScroll: suspend (Int) -> Unit
    ): Int {
        var scrollCount = 0
        while (area.canScrollForward && scrollCount < maxScrolls) {
            val result = scrollForward(area)
            if (result is ScrollResult.Failed) {
                break
            }
            scrollCount++
            onScroll(scrollCount)
        }
        return scrollCount
    }

    companion object {
        const val SCROLL_DELAY_MS = 300L
    }
}

/**
 * Scroll Result
 */
sealed class ScrollResult {
    object Success : ScrollResult()
    data class Failed(val reason: String) : ScrollResult()
}
