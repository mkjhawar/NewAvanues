/**
 * AccessibilityTypesTest.kt - Tests for accessibility type definitions
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-11-17
 *
 * Comprehensive test coverage for all accessibility enums.
 */
package com.augmentalis.voiceos.accessibility

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AccessibilityTypesTest {

    // ========== AnchorPoint Tests ==========

    @Test
    fun `AnchorPoint has all 4 positions`() {
        val values = AnchorPoint.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(AnchorPoint.TOP_LEFT))
        assertTrue(values.contains(AnchorPoint.TOP_RIGHT))
        assertTrue(values.contains(AnchorPoint.BOTTOM_LEFT))
        assertTrue(values.contains(AnchorPoint.BOTTOM_RIGHT))
    }

    @Test
    fun `AnchorPoint valueOf works correctly`() {
        assertEquals(AnchorPoint.TOP_LEFT, AnchorPoint.valueOf("TOP_LEFT"))
        assertEquals(AnchorPoint.TOP_RIGHT, AnchorPoint.valueOf("TOP_RIGHT"))
        assertEquals(AnchorPoint.BOTTOM_LEFT, AnchorPoint.valueOf("BOTTOM_LEFT"))
        assertEquals(AnchorPoint.BOTTOM_RIGHT, AnchorPoint.valueOf("BOTTOM_RIGHT"))
    }

    @Test
    fun `AnchorPoint name property matches enum name`() {
        assertEquals("TOP_LEFT", AnchorPoint.TOP_LEFT.name)
        assertEquals("TOP_RIGHT", AnchorPoint.TOP_RIGHT.name)
        assertEquals("BOTTOM_LEFT", AnchorPoint.BOTTOM_LEFT.name)
        assertEquals("BOTTOM_RIGHT", AnchorPoint.BOTTOM_RIGHT.name)
    }

    @Test
    fun `AnchorPoint ordinal values are sequential`() {
        assertEquals(0, AnchorPoint.TOP_LEFT.ordinal)
        assertEquals(1, AnchorPoint.TOP_RIGHT.ordinal)
        assertEquals(2, AnchorPoint.BOTTOM_LEFT.ordinal)
        assertEquals(3, AnchorPoint.BOTTOM_RIGHT.ordinal)
    }

    @Test
    fun `AnchorPoint can be used in when expression`() {
        val anchor = AnchorPoint.TOP_RIGHT
        val result = when (anchor) {
            AnchorPoint.TOP_LEFT -> "top-left"
            AnchorPoint.TOP_RIGHT -> "top-right"
            AnchorPoint.BOTTOM_LEFT -> "bottom-left"
            AnchorPoint.BOTTOM_RIGHT -> "bottom-right"
        }
        assertEquals("top-right", result)
    }

    @Test
    fun `AnchorPoint equality works`() {
        assertEquals(AnchorPoint.TOP_LEFT, AnchorPoint.valueOf("TOP_LEFT"))
        assertTrue(AnchorPoint.TOP_RIGHT == AnchorPoint.TOP_RIGHT)
        assertTrue(AnchorPoint.BOTTOM_LEFT != AnchorPoint.BOTTOM_RIGHT)
    }

    // ========== BadgeStyle Tests ==========

    @Test
    fun `BadgeStyle has all 4 variants`() {
        val values = BadgeStyle.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(BadgeStyle.FILLED_CIRCLE))
        assertTrue(values.contains(BadgeStyle.OUTLINED_CIRCLE))
        assertTrue(values.contains(BadgeStyle.SQUARE))
        assertTrue(values.contains(BadgeStyle.ROUNDED_RECT))
    }

    @Test
    fun `BadgeStyle valueOf works correctly`() {
        assertEquals(BadgeStyle.FILLED_CIRCLE, BadgeStyle.valueOf("FILLED_CIRCLE"))
        assertEquals(BadgeStyle.OUTLINED_CIRCLE, BadgeStyle.valueOf("OUTLINED_CIRCLE"))
        assertEquals(BadgeStyle.SQUARE, BadgeStyle.valueOf("SQUARE"))
        assertEquals(BadgeStyle.ROUNDED_RECT, BadgeStyle.valueOf("ROUNDED_RECT"))
    }

    @Test
    fun `BadgeStyle name property matches enum name`() {
        assertEquals("FILLED_CIRCLE", BadgeStyle.FILLED_CIRCLE.name)
        assertEquals("OUTLINED_CIRCLE", BadgeStyle.OUTLINED_CIRCLE.name)
        assertEquals("SQUARE", BadgeStyle.SQUARE.name)
        assertEquals("ROUNDED_RECT", BadgeStyle.ROUNDED_RECT.name)
    }

    @Test
    fun `BadgeStyle ordinal values are sequential`() {
        assertEquals(0, BadgeStyle.FILLED_CIRCLE.ordinal)
        assertEquals(1, BadgeStyle.OUTLINED_CIRCLE.ordinal)
        assertEquals(2, BadgeStyle.SQUARE.ordinal)
        assertEquals(3, BadgeStyle.ROUNDED_RECT.ordinal)
    }

    @Test
    fun `BadgeStyle can be used in when expression`() {
        val style = BadgeStyle.OUTLINED_CIRCLE
        val result = when (style) {
            BadgeStyle.FILLED_CIRCLE -> "filled"
            BadgeStyle.OUTLINED_CIRCLE -> "outlined"
            BadgeStyle.SQUARE -> "square"
            BadgeStyle.ROUNDED_RECT -> "rounded"
        }
        assertEquals("outlined", result)
    }

    @Test
    fun `BadgeStyle equality works`() {
        assertEquals(BadgeStyle.FILLED_CIRCLE, BadgeStyle.valueOf("FILLED_CIRCLE"))
        assertTrue(BadgeStyle.SQUARE == BadgeStyle.SQUARE)
        assertTrue(BadgeStyle.OUTLINED_CIRCLE != BadgeStyle.ROUNDED_RECT)
    }

    // ========== ElementVoiceState Tests ==========

    @Test
    fun `ElementVoiceState has all 3 states`() {
        val values = ElementVoiceState.entries
        assertEquals(3, values.size)
        assertTrue(values.contains(ElementVoiceState.ENABLED_WITH_NAME))
        assertTrue(values.contains(ElementVoiceState.ENABLED_NO_NAME))
        assertTrue(values.contains(ElementVoiceState.DISABLED))
    }

    @Test
    fun `ElementVoiceState valueOf works correctly`() {
        assertEquals(ElementVoiceState.ENABLED_WITH_NAME, ElementVoiceState.valueOf("ENABLED_WITH_NAME"))
        assertEquals(ElementVoiceState.ENABLED_NO_NAME, ElementVoiceState.valueOf("ENABLED_NO_NAME"))
        assertEquals(ElementVoiceState.DISABLED, ElementVoiceState.valueOf("DISABLED"))
    }

    @Test
    fun `ElementVoiceState name property matches enum name`() {
        assertEquals("ENABLED_WITH_NAME", ElementVoiceState.ENABLED_WITH_NAME.name)
        assertEquals("ENABLED_NO_NAME", ElementVoiceState.ENABLED_NO_NAME.name)
        assertEquals("DISABLED", ElementVoiceState.DISABLED.name)
    }

    @Test
    fun `ElementVoiceState ordinal values are sequential`() {
        assertEquals(0, ElementVoiceState.ENABLED_WITH_NAME.ordinal)
        assertEquals(1, ElementVoiceState.ENABLED_NO_NAME.ordinal)
        assertEquals(2, ElementVoiceState.DISABLED.ordinal)
    }

    @Test
    fun `ElementVoiceState can be used in when expression`() {
        val state = ElementVoiceState.ENABLED_WITH_NAME
        val result = when (state) {
            ElementVoiceState.ENABLED_WITH_NAME -> "green"
            ElementVoiceState.ENABLED_NO_NAME -> "orange"
            ElementVoiceState.DISABLED -> "grey"
        }
        assertEquals("green", result)
    }

    @Test
    fun `ElementVoiceState equality works`() {
        assertEquals(ElementVoiceState.ENABLED_WITH_NAME, ElementVoiceState.valueOf("ENABLED_WITH_NAME"))
        assertTrue(ElementVoiceState.DISABLED == ElementVoiceState.DISABLED)
        assertTrue(ElementVoiceState.ENABLED_WITH_NAME != ElementVoiceState.ENABLED_NO_NAME)
    }

    @Test
    fun `ElementVoiceState represents correct semantic states`() {
        // ENABLED_WITH_NAME = explicit command (best accessibility)
        val withName = ElementVoiceState.ENABLED_WITH_NAME
        assertNotNull(withName)

        // ENABLED_NO_NAME = generic command (medium accessibility)
        val noName = ElementVoiceState.ENABLED_NO_NAME
        assertNotNull(noName)

        // DISABLED = no voice command (no accessibility)
        val disabled = ElementVoiceState.DISABLED
        assertNotNull(disabled)
    }

    // ========== ConnectionState Tests ==========

    @Test
    fun `ConnectionState has all 4 states`() {
        val values = ConnectionState.entries
        assertEquals(4, values.size)
        assertTrue(values.contains(ConnectionState.CONNECTED))
        assertTrue(values.contains(ConnectionState.DISCONNECTED))
        assertTrue(values.contains(ConnectionState.RECOVERING))
        assertTrue(values.contains(ConnectionState.DEGRADED))
    }

    @Test
    fun `ConnectionState valueOf works correctly`() {
        assertEquals(ConnectionState.CONNECTED, ConnectionState.valueOf("CONNECTED"))
        assertEquals(ConnectionState.DISCONNECTED, ConnectionState.valueOf("DISCONNECTED"))
        assertEquals(ConnectionState.RECOVERING, ConnectionState.valueOf("RECOVERING"))
        assertEquals(ConnectionState.DEGRADED, ConnectionState.valueOf("DEGRADED"))
    }

    @Test
    fun `ConnectionState name property matches enum name`() {
        assertEquals("CONNECTED", ConnectionState.CONNECTED.name)
        assertEquals("DISCONNECTED", ConnectionState.DISCONNECTED.name)
        assertEquals("RECOVERING", ConnectionState.RECOVERING.name)
        assertEquals("DEGRADED", ConnectionState.DEGRADED.name)
    }

    @Test
    fun `ConnectionState ordinal values are sequential`() {
        assertEquals(0, ConnectionState.CONNECTED.ordinal)
        assertEquals(1, ConnectionState.DISCONNECTED.ordinal)
        assertEquals(2, ConnectionState.RECOVERING.ordinal)
        assertEquals(3, ConnectionState.DEGRADED.ordinal)
    }

    @Test
    fun `ConnectionState can be used in when expression`() {
        val state = ConnectionState.RECOVERING
        val result = when (state) {
            ConnectionState.CONNECTED -> "healthy"
            ConnectionState.DISCONNECTED -> "offline"
            ConnectionState.RECOVERING -> "reconnecting"
            ConnectionState.DEGRADED -> "limited"
        }
        assertEquals("reconnecting", result)
    }

    @Test
    fun `ConnectionState equality works`() {
        assertEquals(ConnectionState.CONNECTED, ConnectionState.valueOf("CONNECTED"))
        assertTrue(ConnectionState.RECOVERING == ConnectionState.RECOVERING)
        assertTrue(ConnectionState.CONNECTED != ConnectionState.DISCONNECTED)
    }

    @Test
    fun `ConnectionState represents correct service states`() {
        // CONNECTED = normal operation
        val connected = ConnectionState.CONNECTED
        assertNotNull(connected)

        // DISCONNECTED = no service
        val disconnected = ConnectionState.DISCONNECTED
        assertNotNull(disconnected)

        // RECOVERING = attempting reconnection
        val recovering = ConnectionState.RECOVERING
        assertNotNull(recovering)

        // DEGRADED = partial functionality
        val degraded = ConnectionState.DEGRADED
        assertNotNull(degraded)
    }

    // ========== ScreenEdge Tests ==========

    @Test
    fun `ScreenEdge has all 9 positions`() {
        val values = ScreenEdge.entries
        assertEquals(9, values.size)
        assertTrue(values.contains(ScreenEdge.LEFT))
        assertTrue(values.contains(ScreenEdge.RIGHT))
        assertTrue(values.contains(ScreenEdge.TOP))
        assertTrue(values.contains(ScreenEdge.BOTTOM))
        assertTrue(values.contains(ScreenEdge.TOP_LEFT))
        assertTrue(values.contains(ScreenEdge.TOP_RIGHT))
        assertTrue(values.contains(ScreenEdge.BOTTOM_LEFT))
        assertTrue(values.contains(ScreenEdge.BOTTOM_RIGHT))
        assertTrue(values.contains(ScreenEdge.NONE))
    }

    @Test
    fun `ScreenEdge valueOf works correctly`() {
        assertEquals(ScreenEdge.LEFT, ScreenEdge.valueOf("LEFT"))
        assertEquals(ScreenEdge.RIGHT, ScreenEdge.valueOf("RIGHT"))
        assertEquals(ScreenEdge.TOP, ScreenEdge.valueOf("TOP"))
        assertEquals(ScreenEdge.BOTTOM, ScreenEdge.valueOf("BOTTOM"))
        assertEquals(ScreenEdge.TOP_LEFT, ScreenEdge.valueOf("TOP_LEFT"))
        assertEquals(ScreenEdge.TOP_RIGHT, ScreenEdge.valueOf("TOP_RIGHT"))
        assertEquals(ScreenEdge.BOTTOM_LEFT, ScreenEdge.valueOf("BOTTOM_LEFT"))
        assertEquals(ScreenEdge.BOTTOM_RIGHT, ScreenEdge.valueOf("BOTTOM_RIGHT"))
        assertEquals(ScreenEdge.NONE, ScreenEdge.valueOf("NONE"))
    }

    @Test
    fun `ScreenEdge name property matches enum name`() {
        assertEquals("LEFT", ScreenEdge.LEFT.name)
        assertEquals("RIGHT", ScreenEdge.RIGHT.name)
        assertEquals("TOP", ScreenEdge.TOP.name)
        assertEquals("BOTTOM", ScreenEdge.BOTTOM.name)
        assertEquals("TOP_LEFT", ScreenEdge.TOP_LEFT.name)
        assertEquals("TOP_RIGHT", ScreenEdge.TOP_RIGHT.name)
        assertEquals("BOTTOM_LEFT", ScreenEdge.BOTTOM_LEFT.name)
        assertEquals("BOTTOM_RIGHT", ScreenEdge.BOTTOM_RIGHT.name)
        assertEquals("NONE", ScreenEdge.NONE.name)
    }

    @Test
    fun `ScreenEdge ordinal values are sequential`() {
        assertEquals(0, ScreenEdge.LEFT.ordinal)
        assertEquals(1, ScreenEdge.RIGHT.ordinal)
        assertEquals(2, ScreenEdge.TOP.ordinal)
        assertEquals(3, ScreenEdge.BOTTOM.ordinal)
        assertEquals(4, ScreenEdge.TOP_LEFT.ordinal)
        assertEquals(5, ScreenEdge.TOP_RIGHT.ordinal)
        assertEquals(6, ScreenEdge.BOTTOM_LEFT.ordinal)
        assertEquals(7, ScreenEdge.BOTTOM_RIGHT.ordinal)
        assertEquals(8, ScreenEdge.NONE.ordinal)
    }

    @Test
    fun `ScreenEdge can be used in when expression`() {
        val edge = ScreenEdge.TOP_RIGHT
        val result = when (edge) {
            ScreenEdge.LEFT -> "left"
            ScreenEdge.RIGHT -> "right"
            ScreenEdge.TOP -> "top"
            ScreenEdge.BOTTOM -> "bottom"
            ScreenEdge.TOP_LEFT -> "top-left"
            ScreenEdge.TOP_RIGHT -> "top-right"
            ScreenEdge.BOTTOM_LEFT -> "bottom-left"
            ScreenEdge.BOTTOM_RIGHT -> "bottom-right"
            ScreenEdge.NONE -> "center"
        }
        assertEquals("top-right", result)
    }

    @Test
    fun `ScreenEdge equality works`() {
        assertEquals(ScreenEdge.TOP, ScreenEdge.valueOf("TOP"))
        assertTrue(ScreenEdge.BOTTOM_LEFT == ScreenEdge.BOTTOM_LEFT)
        assertTrue(ScreenEdge.LEFT != ScreenEdge.RIGHT)
    }

    @Test
    fun `ScreenEdge NONE represents center position`() {
        val none = ScreenEdge.NONE
        assertNotNull(none)
        assertEquals("NONE", none.name)
    }

    @Test
    fun `ScreenEdge corners are distinct from edges`() {
        // Corners are composite positions (two edges)
        val topLeft = ScreenEdge.TOP_LEFT
        val topRight = ScreenEdge.TOP_RIGHT
        val bottomLeft = ScreenEdge.BOTTOM_LEFT
        val bottomRight = ScreenEdge.BOTTOM_RIGHT

        // Single edges
        val top = ScreenEdge.TOP
        val bottom = ScreenEdge.BOTTOM
        val left = ScreenEdge.LEFT
        val right = ScreenEdge.RIGHT

        // Verify corners are different from edges
        assertTrue(topLeft != top)
        assertTrue(topLeft != left)
        assertTrue(topRight != top)
        assertTrue(topRight != right)
        assertTrue(bottomLeft != bottom)
        assertTrue(bottomLeft != left)
        assertTrue(bottomRight != bottom)
        assertTrue(bottomRight != right)
    }

    // ========== Cross-Enum Integration Tests ==========

    @Test
    fun `All enums support iteration`() {
        // AnchorPoint
        var count = 0
        for (anchor in AnchorPoint.entries) {
            assertNotNull(anchor)
            count++
        }
        assertEquals(4, count)

        // BadgeStyle
        count = 0
        for (style in BadgeStyle.entries) {
            assertNotNull(style)
            count++
        }
        assertEquals(4, count)

        // ElementVoiceState
        count = 0
        for (state in ElementVoiceState.entries) {
            assertNotNull(state)
            count++
        }
        assertEquals(3, count)

        // ConnectionState
        count = 0
        for (state in ConnectionState.entries) {
            assertNotNull(state)
            count++
        }
        assertEquals(4, count)

        // ScreenEdge
        count = 0
        for (edge in ScreenEdge.entries) {
            assertNotNull(edge)
            count++
        }
        assertEquals(9, count)
    }

    @Test
    fun `All enums support toString`() {
        assertEquals("TOP_RIGHT", AnchorPoint.TOP_RIGHT.toString())
        assertEquals("FILLED_CIRCLE", BadgeStyle.FILLED_CIRCLE.toString())
        assertEquals("ENABLED_WITH_NAME", ElementVoiceState.ENABLED_WITH_NAME.toString())
        assertEquals("CONNECTED", ConnectionState.CONNECTED.toString())
        assertEquals("TOP_LEFT", ScreenEdge.TOP_LEFT.toString())
    }

    @Test
    fun `All enums can be stored in collections`() {
        val anchors = listOf(AnchorPoint.TOP_LEFT, AnchorPoint.BOTTOM_RIGHT)
        assertEquals(2, anchors.size)
        assertTrue(anchors.contains(AnchorPoint.TOP_LEFT))

        val styles = setOf(BadgeStyle.FILLED_CIRCLE, BadgeStyle.OUTLINED_CIRCLE, BadgeStyle.FILLED_CIRCLE)
        assertEquals(2, styles.size) // Set deduplicates

        val states = mutableListOf(ElementVoiceState.ENABLED_WITH_NAME)
        states.add(ElementVoiceState.DISABLED)
        assertEquals(2, states.size)

        val connectionStates = mapOf(
            "primary" to ConnectionState.CONNECTED,
            "backup" to ConnectionState.DEGRADED
        )
        assertEquals(ConnectionState.CONNECTED, connectionStates["primary"])

        val edges = arrayOf(ScreenEdge.TOP, ScreenEdge.BOTTOM, ScreenEdge.LEFT, ScreenEdge.RIGHT)
        assertEquals(4, edges.size)
    }

    @Test
    fun `All enums are serializable by name`() {
        // Verify that valueOf + name round-trips work
        assertEquals(AnchorPoint.BOTTOM_LEFT, AnchorPoint.valueOf(AnchorPoint.BOTTOM_LEFT.name))
        assertEquals(BadgeStyle.SQUARE, BadgeStyle.valueOf(BadgeStyle.SQUARE.name))
        assertEquals(ElementVoiceState.ENABLED_NO_NAME, ElementVoiceState.valueOf(ElementVoiceState.ENABLED_NO_NAME.name))
        assertEquals(ConnectionState.RECOVERING, ConnectionState.valueOf(ConnectionState.RECOVERING.name))
        assertEquals(ScreenEdge.BOTTOM_RIGHT, ScreenEdge.valueOf(ScreenEdge.BOTTOM_RIGHT.name))
    }
}
