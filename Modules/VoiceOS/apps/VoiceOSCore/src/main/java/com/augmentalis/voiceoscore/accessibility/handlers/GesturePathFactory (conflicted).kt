/**
 * GesturePathFactory.kt - VoiceOS component
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-12-22
 */
package com.augmentalis.voiceoscore.accessibility.handlers

import android.accessibilityservice.GestureDescription
import android.graphics.Path

/**
 * Gesture Path Factory
 *
 * Factory interface for creating gesture paths and strokes
 */
interface GesturePathFactory {
    /**
     * Create a new path
     */
    fun createPath(): Path

    /**
     * Create a stroke description
     *
     * @param path The path for the stroke
     * @param startTime Start time in milliseconds
     * @param duration Duration in milliseconds
     * @param willContinue Whether the stroke will continue
     * @return Stroke description
     */
    fun createStroke(
        path: Path,
        startTime: Long,
        duration: Long,
        willContinue: Boolean
    ): GestureDescription.StrokeDescription

    /**
     * Create a gesture description
     *
     * @param strokes List of stroke descriptions
     * @return Gesture description
     */
    fun createGesture(strokes: List<GestureDescription.StrokeDescription>): GestureDescription
}

/**
 * Real Gesture Path Factory
 *
 * Real implementation of GesturePathFactory
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
