package com.augmentalis.avaelements.input

/**
 * iOS VoiceCursor Stub Implementation
 *
 * Currently a no-op implementation. In the future, this could integrate
 * with iOS accessibility features or custom voice control systems.
 *
 * Potential future integrations:
 * - iOS Voice Control (Settings > Accessibility > Voice Control)
 * - Custom Siri Shortcuts integration
 * - ARKit/visionOS gaze tracking
 */

// ═══════════════════════════════════════════════════════════════
// Platform Actual Implementations
// ═══════════════════════════════════════════════════════════════

/**
 * iOS implementation returns a no-op manager.
 * VoiceCursor is primarily an Android VoiceOS feature.
 */
actual fun getVoiceCursorManager(): VoiceCursorManager {
    return NoOpVoiceCursorManager()
}

actual fun currentTimeMillis(): Long {
    // iOS/Kotlin Native - use kotlin.system.getTimeMillis
    return kotlin.system.getTimeMillis()
}
