package com.augmentalis.avaelements.input

/**
 * No-op VoiceOS adapter for when VoiceOS is not available.
 *
 * Provides null-object pattern implementation to avoid null checks
 * throughout the codebase.
 *
 * SOLID Compliance:
 * - DIP: Implements abstraction (VoiceOSAdapter)
 * - LSP: Can be substituted for any VoiceOSAdapter
 * - ISP: Implements all interface methods (as no-ops)
 */
class NoOpVoiceOSAdapter : VoiceOSAdapter {

    override val isAvailable: Boolean = false

    override fun registerClickTarget(
        targetId: String,
        voiceLabel: String,
        bounds: FloatArray,
        callback: () -> Unit
    ) {
        // No-op
    }

    override fun unregisterClickTarget(targetId: String) {
        // No-op
    }

    override fun updateTargetBounds(targetId: String, bounds: FloatArray) {
        // No-op
    }

    override fun startCursor() {
        // No-op
    }

    override fun stopCursor() {
        // No-op
    }
}
