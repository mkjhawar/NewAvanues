package com.augmentalis.avaelements.input

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for VoiceOS Adapter pattern.
 *
 * Demonstrates how the SOLID refactoring enables comprehensive testing
 * without requiring actual VoiceOS to be installed.
 */
class VoiceOSAdapterTest {

    private lateinit var context: Context
    private lateinit var mockAdapter: MockVoiceOSAdapter
    private lateinit var manager: AndroidVoiceCursorManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockAdapter = MockVoiceOSAdapter()
        initializeVoiceCursor(context, mockAdapter)
        manager = getVoiceCursorManager() as AndroidVoiceCursorManager
    }

    @After
    fun tearDown() {
        // Clean up any state if needed
    }

    // ═══════════════════════════════════════════════════════════════
    // Adapter Availability Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testMockAdapterIsAvailable() {
        assertTrue("Mock adapter should be available", manager.isAvailable)
    }

    @Test
    fun testNoOpAdapterIsNotAvailable() {
        val noOpAdapter = NoOpVoiceOSAdapter()
        val noOpManager = AndroidVoiceCursorManager.getInstance(context, noOpAdapter)
        assertFalse("NoOp adapter should not be available", noOpManager.isAvailable)
    }

    // ═══════════════════════════════════════════════════════════════
    // Target Registration Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testRegisterTarget() {
        val target = VoiceTarget(
            id = "button1",
            label = "submit",
            bounds = Rect(0f, 0f, 100f, 50f),
            onSelect = {}
        )

        manager.registerTarget(target)

        assertTrue(
            "Target should be registered",
            mockAdapter.registeredTargets.contains("button1")
        )
        assertEquals(
            "Should have one registered target",
            1,
            mockAdapter.registeredTargets.size
        )
    }

    @Test
    fun testRegisterMultipleTargets() {
        val targets = listOf(
            VoiceTarget("button1", "submit", Rect(0f, 0f, 100f, 50f), {}),
            VoiceTarget("button2", "cancel", Rect(0f, 60f, 100f, 110f), {}),
            VoiceTarget("input1", "email field", Rect(0f, 120f, 200f, 160f), {})
        )

        targets.forEach { manager.registerTarget(it) }

        assertEquals(
            "Should have three registered targets",
            3,
            mockAdapter.registeredTargets.size
        )
        assertTrue(mockAdapter.registeredTargets.contains("button1"))
        assertTrue(mockAdapter.registeredTargets.contains("button2"))
        assertTrue(mockAdapter.registeredTargets.contains("input1"))
    }

    @Test
    fun testUnregisterTarget() {
        val target = VoiceTarget("button1", "submit", Rect(0f, 0f, 100f, 50f), {})

        manager.registerTarget(target)
        manager.unregisterTarget("button1")

        assertTrue(
            "Target should be unregistered",
            mockAdapter.unregisteredTargets.contains("button1")
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Bounds Update Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testUpdateTargetBounds() {
        val target = VoiceTarget("button1", "submit", Rect(0f, 0f, 100f, 50f), {})
        manager.registerTarget(target)

        val newBounds = Rect(10f, 10f, 110f, 60f)
        manager.updateTargetBounds("button1", newBounds)

        assertTrue(
            "Bounds should be updated",
            mockAdapter.updatedBounds.containsKey("button1")
        )
        assertArrayEquals(
            "New bounds should match",
            floatArrayOf(10f, 10f, 110f, 60f),
            mockAdapter.updatedBounds["button1"],
            0.001f
        )
    }

    // ═══════════════════════════════════════════════════════════════
    // Cursor Control Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testStartCursor() {
        manager.start()

        assertTrue("Cursor should be started", mockAdapter.cursorStarted)
        assertTrue("Manager should show active", manager.isActive)
    }

    @Test
    fun testStopCursor() {
        manager.start()
        manager.stop()

        assertTrue("Cursor should be stopped", mockAdapter.cursorStopped)
        assertFalse("Manager should not show active", manager.isActive)
    }

    // ═══════════════════════════════════════════════════════════════
    // Voice Command Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testHandleVoiceCommandByLabel() {
        var clicked = false
        val target = VoiceTarget(
            id = "button1",
            label = "submit",
            bounds = Rect(0f, 0f, 100f, 50f),
            onSelect = { clicked = true }
        )

        manager.registerTarget(target)
        val handled = manager.handleVoiceCommand(
            VoiceCommands.CLICK,
            mapOf("target" to "submit")
        )

        assertTrue("Command should be handled", handled)
        assertTrue("Target callback should be invoked", clicked)
    }

    @Test
    fun testHandleVoiceCommandInvalidTarget() {
        val handled = manager.handleVoiceCommand(
            VoiceCommands.CLICK,
            mapOf("target" to "nonexistent")
        )

        assertFalse("Command should not be handled", handled)
    }

    // ═══════════════════════════════════════════════════════════════
    // Listener Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testListenerActivated() {
        var activatedCalled = false
        val listener = object : VoiceCursorListener {
            override fun onActivated() { activatedCalled = true }
            override fun onDeactivated() {}
            override fun onCursorMoved(position: Offset) {}
            override fun onTargetEntered(target: VoiceTarget) {}
            override fun onTargetExited(target: VoiceTarget) {}
            override fun onTargetSelected(target: VoiceTarget) {}
        }

        manager.addListener(listener)
        manager.start()

        assertTrue("Listener should be notified of activation", activatedCalled)
    }

    @Test
    fun testListenerDeactivated() {
        var deactivatedCalled = false
        val listener = object : VoiceCursorListener {
            override fun onActivated() {}
            override fun onDeactivated() { deactivatedCalled = true }
            override fun onCursorMoved(position: Offset) {}
            override fun onTargetEntered(target: VoiceTarget) {}
            override fun onTargetExited(target: VoiceTarget) {}
            override fun onTargetSelected(target: VoiceTarget) {}
        }

        manager.addListener(listener)
        manager.start()
        manager.stop()

        assertTrue("Listener should be notified of deactivation", deactivatedCalled)
    }

    // ═══════════════════════════════════════════════════════════════
    // Custom Adapter Tests
    // ═══════════════════════════════════════════════════════════════

    @Test
    fun testLoggingAdapterDelegates() {
        val loggingAdapter = LoggingVoiceOSAdapter(mockAdapter)
        val loggingManager = AndroidVoiceCursorManager.getInstance(context, loggingAdapter)

        val target = VoiceTarget("button1", "submit", Rect(0f, 0f, 100f, 50f), {})
        loggingManager.registerTarget(target)

        // Verify delegation works
        assertTrue(
            "Logging adapter should delegate to mock",
            mockAdapter.registeredTargets.contains("button1")
        )
    }

    @Test
    fun testLicensedAdapterBlocksWhenNoLicense() {
        val licensedAdapter = LicensedVoiceOSAdapter(mockAdapter, hasLicense = false)
        val licensedManager = AndroidVoiceCursorManager.getInstance(context, licensedAdapter)

        val target = VoiceTarget("button1", "submit", Rect(0f, 0f, 100f, 50f), {})
        licensedManager.registerTarget(target)

        // Verify target NOT registered when no license
        assertFalse(
            "Target should not be registered without license",
            mockAdapter.registeredTargets.contains("button1")
        )
    }

    @Test
    fun testLicensedAdapterAllowsWithLicense() {
        val licensedAdapter = LicensedVoiceOSAdapter(mockAdapter, hasLicense = true)
        val licensedManager = AndroidVoiceCursorManager.getInstance(context, licensedAdapter)

        val target = VoiceTarget("button1", "submit", Rect(0f, 0f, 100f, 50f), {})
        licensedManager.registerTarget(target)

        // Verify target IS registered with license
        assertTrue(
            "Target should be registered with license",
            mockAdapter.registeredTargets.contains("button1")
        )
    }
}

// ═══════════════════════════════════════════════════════════════
// Mock Adapter for Testing
// ═══════════════════════════════════════════════════════════════

class MockVoiceOSAdapter : VoiceOSAdapter {
    override val isAvailable: Boolean = true

    val registeredTargets = mutableListOf<String>()
    val unregisteredTargets = mutableListOf<String>()
    val updatedBounds = mutableMapOf<String, FloatArray>()
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
        updatedBounds[targetId] = bounds
    }

    override fun startCursor() {
        cursorStarted = true
    }

    override fun stopCursor() {
        cursorStopped = true
    }

    fun reset() {
        registeredTargets.clear()
        unregisteredTargets.clear()
        updatedBounds.clear()
        cursorStarted = false
        cursorStopped = false
    }
}
