/**
 * ServiceStateTest.kt — Unit tests for ServiceState
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: Manoj Jhawar
 * Created: 2026-02-22
 *
 * Tests state predicate methods (isActive, canStart, isOperational,
 * needsInitialization) and getDescription coverage for all states.
 */
package com.augmentalis.speechrecognition

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServiceStateTest {

    // ── isActive ──────────────────────────────────────────────────

    @Test
    fun `isActive returns true only for LISTENING and PROCESSING`() {
        assertTrue(ServiceState.LISTENING.isActive())
        assertTrue(ServiceState.PROCESSING.isActive())
    }

    @Test
    fun `isActive returns false for all non-active states`() {
        val nonActive = listOf(
            ServiceState.UNINITIALIZED, ServiceState.INITIALIZING, ServiceState.READY,
            ServiceState.PAUSED, ServiceState.STOPPED, ServiceState.ERROR,
            ServiceState.DESTROYING
        )
        nonActive.forEach { state ->
            assertFalse(state.isActive(), "Expected isActive() == false for $state")
        }
    }

    // ── canStart ──────────────────────────────────────────────────

    @Test
    fun `canStart returns true for READY PAUSED and STOPPED`() {
        assertTrue(ServiceState.READY.canStart())
        assertTrue(ServiceState.PAUSED.canStart())
        assertTrue(ServiceState.STOPPED.canStart())
    }

    @Test
    fun `canStart returns false for states that cannot start`() {
        val cannotStart = listOf(
            ServiceState.UNINITIALIZED, ServiceState.INITIALIZING, ServiceState.LISTENING,
            ServiceState.PROCESSING, ServiceState.ERROR, ServiceState.DESTROYING
        )
        cannotStart.forEach { state ->
            assertFalse(state.canStart(), "Expected canStart() == false for $state")
        }
    }

    // ── isOperational ─────────────────────────────────────────────

    @Test
    fun `isOperational returns true for READY LISTENING PROCESSING and PAUSED`() {
        val operational = listOf(
            ServiceState.READY, ServiceState.LISTENING,
            ServiceState.PROCESSING, ServiceState.PAUSED
        )
        operational.forEach { state ->
            assertTrue(state.isOperational(), "Expected isOperational() == true for $state")
        }
    }

    @Test
    fun `isOperational returns false for UNINITIALIZED STOPPED ERROR DESTROYING`() {
        val notOperational = listOf(
            ServiceState.UNINITIALIZED, ServiceState.INITIALIZING,
            ServiceState.STOPPED, ServiceState.ERROR, ServiceState.DESTROYING
        )
        notOperational.forEach { state ->
            assertFalse(state.isOperational(), "Expected isOperational() == false for $state")
        }
    }

    // ── needsInitialization ───────────────────────────────────────

    @Test
    fun `needsInitialization returns true for UNINITIALIZED ERROR and STOPPED`() {
        assertTrue(ServiceState.UNINITIALIZED.needsInitialization())
        assertTrue(ServiceState.ERROR.needsInitialization())
        assertTrue(ServiceState.STOPPED.needsInitialization())
    }

    @Test
    fun `needsInitialization returns false for INITIALIZING READY LISTENING PROCESSING PAUSED DESTROYING`() {
        val doesNotNeed = listOf(
            ServiceState.INITIALIZING, ServiceState.READY, ServiceState.LISTENING,
            ServiceState.PROCESSING, ServiceState.PAUSED, ServiceState.DESTROYING
        )
        doesNotNeed.forEach { state ->
            assertFalse(state.needsInitialization(), "Expected needsInitialization() == false for $state")
        }
    }

    // ── getDescription ────────────────────────────────────────────

    @Test
    fun `getDescription returns non-blank string for every state`() {
        ServiceState.entries.forEach { state ->
            val desc = state.getDescription()
            assertTrue(desc.isNotBlank(), "Expected non-blank description for $state")
        }
    }

    @Test
    fun `getDescription returns known labels for key states`() {
        assertEquals("Ready", ServiceState.READY.getDescription())
        assertEquals("Listening...", ServiceState.LISTENING.getDescription())
        assertEquals("Error", ServiceState.ERROR.getDescription())
        assertEquals("Not initialized", ServiceState.UNINITIALIZED.getDescription())
    }

    // ── Enum Completeness ─────────────────────────────────────────

    @Test
    fun `ServiceState has exactly 9 members`() {
        assertEquals(9, ServiceState.entries.size)
    }
}
