package com.augmentalis.avaelements.input

/**
 * Web/JavaScript VoiceCursor Stub Implementation
 *
 * Currently a no-op implementation. In the future, this could integrate
 * with Web Speech API or custom voice control systems.
 *
 * Potential future integrations:
 * - Web Speech API (SpeechRecognition)
 * - WebXR eye/gaze tracking
 * - Custom voice command framework
 */

// ═══════════════════════════════════════════════════════════════
// Platform Actual Implementations
// ═══════════════════════════════════════════════════════════════

/**
 * Web implementation returns a no-op manager.
 * VoiceCursor is primarily an Android VoiceOS feature.
 */
actual fun getVoiceCursorManager(): VoiceCursorManager {
    return NoOpVoiceCursorManager()
}

actual fun currentTimeMillis(): Long {
    return kotlin.js.Date.now().toLong()
}
