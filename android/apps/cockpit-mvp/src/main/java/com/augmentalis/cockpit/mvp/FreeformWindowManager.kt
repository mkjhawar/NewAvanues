package com.augmentalis.cockpit.mvp

import android.app.Activity

/**
 * FreeformWindowManager - Stub for Android freeform window support
 *
 * TODO: Implement MediaProjection-based window capture for freeform Android apps
 * This is a placeholder to allow compilation. Full implementation requires:
 * - MediaProjection permission request
 * - Virtual display creation
 * - App window capture and rendering
 */
class FreeformWindowManager(private val activity: Activity) {

    /**
     * Request MediaProjection permission
     * TODO: Implement permission request flow
     */
    fun requestPermission() {
        // TODO: Start MediaProjection permission activity
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        // TODO: Release MediaProjection and virtual displays
    }
}
