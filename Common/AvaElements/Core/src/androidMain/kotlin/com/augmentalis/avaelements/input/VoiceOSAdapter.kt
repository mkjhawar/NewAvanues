package com.augmentalis.avaelements.input

/**
 * Adapter interface for VoiceOS integration.
 *
 * Abstracts the reflection-based calls for testability and DIP compliance.
 * Allows injection of different implementations (real VoiceOS, mocks, no-ops).
 *
 * SOLID Compliance:
 * - DIP: High-level VoiceCursorManager depends on abstraction, not concrete reflection
 * - ISP: Interface focused on VoiceOS-specific operations only
 * - OCP: Can add new adapters without modifying existing code
 */
interface VoiceOSAdapter {
    /**
     * Whether VoiceOS is available on this device
     */
    val isAvailable: Boolean

    /**
     * Register a clickable target with VoiceOS
     *
     * @param targetId Unique identifier for the target
     * @param voiceLabel Voice-accessible label (e.g., "submit button")
     * @param bounds Screen bounds [left, top, right, bottom]
     * @param callback Action to invoke when target is selected
     */
    fun registerClickTarget(
        targetId: String,
        voiceLabel: String,
        bounds: FloatArray,
        callback: () -> Unit
    )

    /**
     * Unregister a previously registered target
     *
     * @param targetId Identifier of the target to remove
     */
    fun unregisterClickTarget(targetId: String)

    /**
     * Update the screen bounds of an existing target
     *
     * @param targetId Identifier of the target to update
     * @param bounds New screen bounds [left, top, right, bottom]
     */
    fun updateTargetBounds(targetId: String, bounds: FloatArray)

    /**
     * Start the VoiceOS cursor system
     */
    fun startCursor()

    /**
     * Stop the VoiceOS cursor system
     */
    fun stopCursor()
}
