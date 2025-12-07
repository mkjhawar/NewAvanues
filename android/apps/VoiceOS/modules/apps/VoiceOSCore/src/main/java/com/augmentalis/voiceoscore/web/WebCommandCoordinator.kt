package com.augmentalis.voiceoscore.web

import android.content.Context
import com.augmentalis.voiceoscore.accessibility.VoiceOSService

/**
 * Stub for WebCommandCoordinator
 * Original: Coordinates web browser command execution
 *
 * Phase 1 Quick Fix: Stub implementation to unblock compilation
 * Full implementation to be restored by Agent 3
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Agent 1 (Service Layer Restorer)
 * Created: 2025-11-26
 */
class WebCommandCoordinator(
    private val context: Context,
    private val service: VoiceOSService
) {
    fun isCurrentAppBrowser(packageName: String): Boolean {
        return false // Stub - no browser detection
    }

    suspend fun processWebCommand(command: String, packageName: String): Boolean {
        return false // Stub - no web command processing
    }
}
