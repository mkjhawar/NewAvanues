package com.augmentalis.avaelements.flutter.material.lists

import com.augmentalis.avaelements.core.Component
import kotlin.test.*

/**
 * Comprehensive unit tests for ExpansionTile component
 *
 * @since 3.0.0-flutter-parity
 */
class ExpansionTileTest {

    // Mock component for testing children
    private data class MockComponent(
        override val type: String = "Mock",
        override val id: String? = null,
        override val style: com.augmentalis.avaelements.core.types.ComponentStyle? = null,
        override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
    ) : Component {
        override fun render(renderer: com.augmentalis.avaelements.core.Renderer): Any = "mock"
    }

    @Test
    fun `create ExpansionTile with default values`() {
        val tile = ExpansionTile(title = "Settings")

        assertEquals("ExpansionTile", tile.type)
        assertEquals("Settings", tile.title)
        assertNull(tile.subtitle)
        assertFalse(tile.initiallyExpanded)
        assertTrue(tile.maintainState)
        assertTrue(tile.children.isEmpty())
    }

    @Test
    fun `ExpansionTile onExpansionChanged callback triggers on state change`() {
        var expanded = false
        val tile = ExpansionTile(
            title = "Menu",
            onExpansionChanged = { expanded = it }
        )

        tile.onExpansionChanged?.invoke(true)
        assertTrue(expanded)

        tile.onExpansionChanged?.invoke(false)
        assertFalse(expanded)
    }

    @Test
    fun `ExpansionTile accessibility description includes expansion state`() {
        val tile = ExpansionTile(title = "Settings")

        assertEquals("Settings, expanded", tile.getAccessibilityDescription(true))
        assertEquals("Settings, collapsed", tile.getAccessibilityDescription(false))
    }

    @Test
    fun `ExpansionTile accessibility uses custom contentDescription`() {
        val tile = ExpansionTile(
            title = "Settings",
            contentDescription = "App settings"
        )

        assertEquals("App settings, expanded", tile.getAccessibilityDescription(true))
    }

    @Test
    fun `ExpansionTile default animation duration is 200ms`() {
        assertEquals(200, ExpansionTile.DEFAULT_ANIMATION_DURATION)
    }

    @Test
    fun `ExpansionTile simple factory creates basic tile`() {
        val children = listOf(MockComponent())
        val tile = ExpansionTile.simple(
            title = "Options",
            children = children
        )

        assertEquals("Options", tile.title)
        assertEquals(children, tile.children)
        assertFalse(tile.initiallyExpanded)
    }

    @Test
    fun `ExpansionTile withSubtitle factory includes subtitle`() {
        val children = listOf(MockComponent())
        val tile = ExpansionTile.withSubtitle(
            title = "Settings",
            subtitle = "Configure app",
            children = children
        )

        assertEquals("Settings", tile.title)
        assertEquals("Configure app", tile.subtitle)
        assertEquals(children, tile.children)
    }

    @Test
    fun `ExpansionTile withIcon factory includes leading icon`() {
        val children = listOf(MockComponent())
        val tile = ExpansionTile.withIcon(
            title = "Settings",
            leading = "settings_icon",
            children = children
        )

        assertEquals("Settings", tile.title)
        assertEquals("settings_icon", tile.leading)
        assertEquals(children, tile.children)
    }

    @Test
    fun `ExpansionTile supports initially expanded state`() {
        val tile = ExpansionTile(
            title = "Settings",
            initiallyExpanded = true
        )

        assertTrue(tile.initiallyExpanded)
    }

    @Test
    fun `ExpansionTile supports maintain state option`() {
        val tileWithState = ExpansionTile(
            title = "Settings",
            maintainState = true
        )
        assertTrue(tileWithState.maintainState)

        val tileWithoutState = ExpansionTile(
            title = "Settings",
            maintainState = false
        )
        assertFalse(tileWithoutState.maintainState)
    }

    @Test
    fun `ExpansionTile supports multiple children`() {
        val children = listOf(
            MockComponent(id = "child1"),
            MockComponent(id = "child2"),
            MockComponent(id = "child3")
        )

        val tile = ExpansionTile(
            title = "Menu",
            children = children
        )

        assertEquals(3, tile.children.size)
        assertEquals(children, tile.children)
    }

    @Test
    fun `ExpansionTile CrossAxisAlignment enum values`() {
        assertEquals(ExpansionTile.CrossAxisAlignment.Start, ExpansionTile.CrossAxisAlignment.Start)
        assertEquals(ExpansionTile.CrossAxisAlignment.Center, ExpansionTile.CrossAxisAlignment.Center)
        assertEquals(ExpansionTile.CrossAxisAlignment.End, ExpansionTile.CrossAxisAlignment.End)
        assertEquals(ExpansionTile.CrossAxisAlignment.Stretch, ExpansionTile.CrossAxisAlignment.Stretch)
    }

    @Test
    fun `ExpansionTile Alignment enum values`() {
        assertEquals(ExpansionTile.Alignment.Start, ExpansionTile.Alignment.Start)
        assertEquals(ExpansionTile.Alignment.Center, ExpansionTile.Alignment.Center)
        assertEquals(ExpansionTile.Alignment.End, ExpansionTile.Alignment.End)
    }

    @Test
    fun `ExpansionTile supports custom alignment settings`() {
        val tile = ExpansionTile(
            title = "Settings",
            expandedCrossAxisAlignment = ExpansionTile.CrossAxisAlignment.Stretch,
            expandedAlignment = ExpansionTile.Alignment.Center
        )

        assertEquals(ExpansionTile.CrossAxisAlignment.Stretch, tile.expandedCrossAxisAlignment)
        assertEquals(ExpansionTile.Alignment.Center, tile.expandedAlignment)
    }

    @Test
    fun `ExpansionTile supports custom colors`() {
        val tile = ExpansionTile(
            title = "Settings",
            backgroundColor = "#FFFFFF",
            collapsedBackgroundColor = "#F5F5F5",
            textColor = "#000000",
            collapsedTextColor = "#666666"
        )

        assertEquals("#FFFFFF", tile.backgroundColor)
        assertEquals("#F5F5F5", tile.collapsedBackgroundColor)
        assertEquals("#000000", tile.textColor)
        assertEquals("#666666", tile.collapsedTextColor)
    }

    @Test
    fun `ExpansionTile expansion animation should be smooth`() {
        // This test verifies the animation duration constant
        // Actual animation smoothness would be tested in integration tests
        assertTrue(ExpansionTile.DEFAULT_ANIMATION_DURATION >= 150)
        assertTrue(ExpansionTile.DEFAULT_ANIMATION_DURATION <= 300)
    }
}
