/**
 * ViewModelStateTest.kt - Unit tests for ViewModelState and NullableState
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.foundation.state

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ViewModelStateTest {

    // -------------------------------------------------------------------------
    // ViewModelState
    // -------------------------------------------------------------------------

    @Test
    fun initialValue_isReadableViaValue() {
        val state = ViewModelState(42)
        assertEquals(42, state.value)
    }

    @Test
    fun setValue_updatesValueAndFlow() {
        val state = ViewModelState("hello")
        state.value = "world"
        assertEquals("world", state.value)
        assertEquals("world", state.flow.value)
    }

    @Test
    fun update_transformIsApplied() {
        val state = ViewModelState(10)
        state.update { it + 5 }
        assertEquals(15, state.value)
    }

    @Test
    fun flow_reflectsCurrentValue() {
        val state = ViewModelState(false)
        assertEquals(false, state.flow.value)
        state.value = true
        assertEquals(true, state.flow.value)
    }

    // -------------------------------------------------------------------------
    // NullableState
    // -------------------------------------------------------------------------

    @Test
    fun nullableState_startsNullByDefault() {
        val state = NullableState<String>()
        assertNull(state.value)
        assertFalse(state.hasValue())
    }

    @Test
    fun nullableState_setAndClear() {
        val state = NullableState<Int>()
        state.set(99)
        assertTrue(state.hasValue())
        assertEquals(99, state.value)

        state.clear()
        assertNull(state.value)
        assertFalse(state.hasValue())
    }

    @Test
    fun ifPresent_onlyRunsWhenValueIsNonNull() {
        val state = NullableState<String>()
        var called = false

        state.ifPresent { called = true }
        assertFalse(called, "ifPresent must not run when value is null")

        state.value = "active"
        state.ifPresent { called = true }
        assertTrue(called)
    }
}
