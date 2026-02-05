/**
 * GazeTrackerPlatform.kt - Android implementation
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */

package com.augmentalis.gaze

/**
 * Android GazeTracker factory
 */
actual object GazeTrackerFactory {
    /**
     * Create Android gaze tracker instance
     * TODO: Implement with CameraX and ML Kit face detection
     */
    actual fun create(): IGazeTracker {
        // Return stub for now - real implementation uses CameraX + ML Kit
        return StubGazeTracker()
    }

    /**
     * Check if gaze tracking is available on Android
     * Requires front camera and sufficient API level
     */
    actual fun isAvailable(): Boolean {
        // TODO: Check camera availability and ML Kit support
        return false
    }
}
