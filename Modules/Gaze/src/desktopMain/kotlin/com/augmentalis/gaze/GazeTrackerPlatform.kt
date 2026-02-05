/**
 * GazeTrackerPlatform.kt - Desktop (JVM) implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.gaze

/**
 * Desktop GazeTracker factory
 */
actual object GazeTrackerFactory {
    /**
     * Create Desktop gaze tracker instance
     * TODO: Implement with Tobii SDK or webcam-based tracking
     */
    actual fun create(): IGazeTracker {
        // Return stub for now
        return StubGazeTracker()
    }

    /**
     * Check if gaze tracking is available on Desktop
     */
    actual fun isAvailable(): Boolean {
        // TODO: Check for Tobii tracker or webcam
        return false
    }
}
