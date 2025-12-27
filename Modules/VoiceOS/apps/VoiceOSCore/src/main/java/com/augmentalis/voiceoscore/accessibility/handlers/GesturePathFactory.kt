/**
 * GesturePathFactory.kt - Factory interface for creating gesture paths and strokes
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Code-Reviewed-By: CCA
 * Created: 2025-10-17
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.accessibilityservice.GestureDescription
import android.graphics.Path

/**
 * Factory interface for creating Android gesture primitives
 * Enables testability by allowing mock implementations in unit tests
 */
interface GesturePathFactory {
    /**
     * Create a new Path instance
     */
    fun createPath(): Path

    /**
     * Create a StrokeDescription from a path
     *
     * @param path The gesture path
     * @param startTime Start time of the stroke in milliseconds
     * @param duration Duration of the stroke in milliseconds
     * @param willContinue Whether the stroke will continue in another gesture
     */
    fun createStroke(
        path: Path,
        startTime: Long,
        duration: Long,
        willContinue: Boolean
    ): GestureDescription.StrokeDescription

    /**
     * Create a GestureDescription from strokes
     *
     * @param strokes List of stroke descriptions to include
     */
    fun createGesture(strokes: List<GestureDescription.StrokeDescription>): GestureDescription
}

/**
 * Real implementation using actual Android framework classes
 */
class RealGesturePathFactory : GesturePathFactory {
    override fun createPath(): Path {
        return Path()
    }

    override fun createStroke(
        path: Path,
        startTime: Long,
        duration: Long,
        willContinue: Boolean
    ): GestureDescription.StrokeDescription {
        return GestureDescription.StrokeDescription(path, startTime, duration, willContinue)
    }

    override fun createGesture(strokes: List<GestureDescription.StrokeDescription>): GestureDescription {
        val builder = GestureDescription.Builder()
        strokes.forEach { builder.addStroke(it) }
        return builder.build()
    }
}
