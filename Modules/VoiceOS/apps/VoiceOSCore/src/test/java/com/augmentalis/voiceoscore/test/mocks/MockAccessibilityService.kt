/**
 * MockAccessibilityService.kt - Mock accessibility service for testing
 *
 * Provides a testable implementation of AccessibilityService.
 * Used in unit tests that need service context.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Created: 2025-11-26
 */

package com.augmentalis.voiceoscore.test.mocks

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Mock implementation of AccessibilityService for testing.
 *
 * Provides:
 * - Event tracking
 * - Service lifecycle simulation
 * - No-op implementations for required methods
 */
class MockAccessibilityService : AccessibilityService() {

    var isServiceConnected = false
        private set

    val receivedEvents = mutableListOf<AccessibilityEvent>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event?.let {
            receivedEvents.add(AccessibilityEvent.obtain(it))
        }
    }

    override fun onInterrupt() {
        receivedEvents.clear()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceConnected = true
    }

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        isServiceConnected = false
        return super.onUnbind(intent)
    }

    /**
     * Clear all tracked events.
     */
    fun clearEvents() {
        receivedEvents.clear()
    }

    /**
     * Get count of events of a specific type.
     */
    fun getEventCount(eventType: Int): Int {
        return receivedEvents.count { it.eventType == eventType }
    }
}
