package com.augmentalis.avaelements.input

/**
 * Desktop VoiceCursor Stub Implementation
 *
 * Currently a no-op implementation. In the future, this could integrate
 * with desktop voice control systems or accessibility APIs.
 *
 * Potential future integrations:
 * - Windows Speech Recognition API
 * - macOS Voice Control (dictation/accessibility)
 * - Linux Speech Dispatcher
 * - Cross-platform: Vosk/Whisper local speech recognition
 */

// ═══════════════════════════════════════════════════════════════
// Platform Actual Implementations
// ═══════════════════════════════════════════════════════════════

/**
 * Desktop implementation returns a no-op manager.
 * VoiceCursor is primarily an Android VoiceOS feature.
 */
actual fun getVoiceCursorManager(): VoiceCursorManager {
    return NoOpVoiceCursorManager()
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()
