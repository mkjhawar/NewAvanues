package com.augmentalis.avaelements.input

import android.content.Context

/**
 * Example usage of the VoiceOS adapter pattern.
 *
 * This file demonstrates how the SOLID refactoring enables:
 * 1. Dependency Injection (testing with mocks)
 * 2. Custom VoiceOS implementations
 * 3. Backward compatibility
 */

// ═══════════════════════════════════════════════════════════════
// Example 1: Default Usage (Backward Compatible)
// ═══════════════════════════════════════════════════════════════

/**
 * Standard initialization - works exactly as before
 */
fun example1_DefaultUsage(context: Context) {
    // Simple initialization - auto-detects VoiceOS
    initializeVoiceCursor(context)

    // Use VoiceCursor manager
    val manager = getVoiceCursorManager()
    println("VoiceOS available: ${manager.isAvailable}")
}

// ═══════════════════════════════════════════════════════════════
// Example 2: Testing with Mock Adapter
// ═══════════════════════════════════════════════════════════════

/**
 * Mock VoiceOS adapter for unit testing
 */
class MockVoiceOSAdapter : VoiceOSAdapter {
    override val isAvailable: Boolean = true

    val registeredTargets = mutableListOf<String>()
    val unregisteredTargets = mutableListOf<String>()
    var cursorStarted = false
    var cursorStopped = false

    override fun registerClickTarget(
        targetId: String,
        voiceLabel: String,
        bounds: FloatArray,
        callback: () -> Unit
    ) {
        registeredTargets.add(targetId)
    }

    override fun unregisterClickTarget(targetId: String) {
        unregisteredTargets.add(targetId)
    }

    override fun updateTargetBounds(targetId: String, bounds: FloatArray) {
        // Track if needed
    }

    override fun startCursor() {
        cursorStarted = true
    }

    override fun stopCursor() {
        cursorStopped = true
    }
}

/**
 * Unit test example using mock adapter
 */
fun example2_TestWithMock(context: Context) {
    val mockAdapter = MockVoiceOSAdapter()

    // Initialize with mock adapter
    initializeVoiceCursor(context, mockAdapter)

    val manager = getVoiceCursorManager() as AndroidVoiceCursorManager

    // Register target
    manager.registerTarget(
        VoiceTarget(
            id = "button1",
            label = "submit",
            bounds = Rect(0f, 0f, 100f, 50f),
            onSelect = { println("Button clicked") }
        )
    )

    // Verify mock was called
    assert(mockAdapter.registeredTargets.contains("button1"))
    println("Mock test passed: target registered")
}

// ═══════════════════════════════════════════════════════════════
// Example 3: Custom VoiceOS Implementation
// ═══════════════════════════════════════════════════════════════

/**
 * Custom VoiceOS adapter that logs all operations
 */
class LoggingVoiceOSAdapter(
    private val delegate: VoiceOSAdapter
) : VoiceOSAdapter {

    override val isAvailable: Boolean
        get() = delegate.isAvailable

    override fun registerClickTarget(
        targetId: String,
        voiceLabel: String,
        bounds: FloatArray,
        callback: () -> Unit
    ) {
        println("LOG: Registering target '$targetId' with label '$voiceLabel'")
        delegate.registerClickTarget(targetId, voiceLabel, bounds, callback)
    }

    override fun unregisterClickTarget(targetId: String) {
        println("LOG: Unregistering target '$targetId'")
        delegate.unregisterClickTarget(targetId)
    }

    override fun updateTargetBounds(targetId: String, bounds: FloatArray) {
        println("LOG: Updating bounds for '$targetId'")
        delegate.updateTargetBounds(targetId, bounds)
    }

    override fun startCursor() {
        println("LOG: Starting VoiceCursor")
        delegate.startCursor()
    }

    override fun stopCursor() {
        println("LOG: Stopping VoiceCursor")
        delegate.stopCursor()
    }
}

/**
 * Initialize with logging wrapper
 */
fun example3_CustomAdapter(context: Context) {
    val baseAdapter = ReflectionVoiceOSAdapter.create(context)
    val loggingAdapter = LoggingVoiceOSAdapter(baseAdapter)

    initializeVoiceCursor(context, loggingAdapter)
}

// ═══════════════════════════════════════════════════════════════
// Example 4: Conditional VoiceOS (Enterprise vs Free)
// ═══════════════════════════════════════════════════════════════

/**
 * Adapter that checks license before enabling VoiceOS
 */
class LicensedVoiceOSAdapter(
    private val delegate: VoiceOSAdapter,
    private val hasLicense: Boolean
) : VoiceOSAdapter {

    override val isAvailable: Boolean
        get() = hasLicense && delegate.isAvailable

    override fun registerClickTarget(
        targetId: String,
        voiceLabel: String,
        bounds: FloatArray,
        callback: () -> Unit
    ) {
        if (hasLicense) {
            delegate.registerClickTarget(targetId, voiceLabel, bounds, callback)
        } else {
            println("VoiceOS requires enterprise license")
        }
    }

    override fun unregisterClickTarget(targetId: String) {
        if (hasLicense) delegate.unregisterClickTarget(targetId)
    }

    override fun updateTargetBounds(targetId: String, bounds: FloatArray) {
        if (hasLicense) delegate.updateTargetBounds(targetId, bounds)
    }

    override fun startCursor() {
        if (hasLicense) delegate.startCursor()
    }

    override fun stopCursor() {
        if (hasLicense) delegate.stopCursor()
    }
}

/**
 * Initialize with license check
 */
fun example4_LicenseCheck(context: Context, hasEnterpriseLicense: Boolean) {
    val baseAdapter = ReflectionVoiceOSAdapter.create(context)
    val licensedAdapter = LicensedVoiceOSAdapter(baseAdapter, hasEnterpriseLicense)

    initializeVoiceCursor(context, licensedAdapter)
}
