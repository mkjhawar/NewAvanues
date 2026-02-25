package com.augmentalis.avanueui.renderer.ios

import com.augmentalis.avanueui.ui.core.display.*
import com.augmentalis.avanueui.ui.core.layout.*
import com.augmentalis.avanueui.ui.core.data.*
import com.augmentalis.avanueui.core.*
import kotlin.test.*

/**
 * Comprehensive Unit Tests for iOS Renderer Phase 3 Components
 *
 * Tests all 10 new advanced components added in Phase 3:
 * - Display: Badge, Chip, Avatar, Skeleton, Tooltip (5 components)
 * - Layout: Divider (1 component)
 * - Data: Accordion (1 component)
 * - Advanced: Card, Grid, Popover (3 components)
 *
 * Total: 25+ tests
 *
 * @author Manoj Jhawar
 * @since 2025-11-21
 */
class IOSRendererPhase3Test {

    // ====================
    // DISPLAY COMPONENTS (15 tests)
    // ====================

    @Test
    fun testBadgeRendererCreation() {
        val renderer = IOSBadgeRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testBadgeWithContent() {
        val component = BadgeComponent(content = "NEW", color = Color.Red)
        assertEquals("NEW", component.content)
        assertEquals(Color.Red, component.color)
    }

    @Test
    fun testBadgeNotification() {
        val component = BadgeComponent.notification(count = 5)
        assertEquals("5", component.content)
        assertEquals(Color.Red, component.color)
    }

    @Test
    fun testBadgeNotificationOverflow() {
        val component = BadgeComponent.notification(count = 150)
        assertEquals("99+", component.content)
    }

    @Test
    fun testBadgeDot() {
        val component = BadgeComponent(content = "•", dot = true)
        assertTrue(component.dot)
    }

    @Test
    fun testChipRendererCreation() {
        val renderer = IOSChipRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testChipWithLabel() {
        val component = ChipComponent(label = "Technology")
        assertEquals("Technology", component.label)
        assertFalse(component.deletable)
        assertFalse(component.selected)
    }

    @Test
    fun testChipFilter() {
        val component = ChipComponent.filter(label = "Category", selected = true)
        assertEquals("Category", component.label)
        assertTrue(component.deletable)
        assertTrue(component.selected)
    }

    @Test
    fun testChipToggleSelection() {
        val component = ChipComponent(label = "Test", selected = false)
        val toggled = component.toggleSelection()
        assertTrue(toggled.selected)
    }

    @Test
    fun testAvatarRendererCreation() {
        val renderer = IOSAvatarRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testAvatarWithInitials() {
        val component = AvatarComponent.initials("John Doe")
        assertEquals("JO", component.initials)
        assertEquals(AvatarShape.CIRCLE, component.shape)
    }

    @Test
    fun testAvatarWithIcon() {
        val component = AvatarComponent.icon("person")
        assertEquals("person", component.icon)
        assertNull(component.initials)
    }

    @Test
    fun testSkeletonRendererCreation() {
        val renderer = IOSSkeletonRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testSkeletonTextLine() {
        val component = SkeletonComponent.textLine(width = 200f)
        assertEquals(SkeletonVariant.TEXT, component.variant)
        assertEquals(200f, component.width)
        assertTrue(component.animated)
    }

    @Test
    fun testSkeletonAvatar() {
        val component = SkeletonComponent.avatar(size = 48f)
        assertEquals(SkeletonVariant.CIRCULAR, component.variant)
        assertEquals(48f, component.width)
        assertEquals(48f, component.height)
    }

    // ====================
    // LAYOUT COMPONENTS (3 tests)
    // ====================

    @Test
    fun testDividerRendererCreation() {
        val renderer = IOSDividerRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testDividerHorizontal() {
        val component = DividerComponent.HORIZONTAL
        assertEquals(Orientation.Horizontal, component.orientation)
        assertEquals(1f, component.thickness)
    }

    @Test
    fun testDividerVertical() {
        val component = DividerComponent.VERTICAL
        assertEquals(Orientation.Vertical, component.orientation)
        assertEquals(1f, component.thickness)
    }

    // ====================
    // DATA COMPONENTS (4 tests)
    // ====================

    @Test
    fun testAccordionRendererCreation() {
        val renderer = IOSAccordionRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testAccordionWithItems() {
        val items = listOf(
            AccordionItem("1", "Section 1", "Content 1"),
            AccordionItem("2", "Section 2", "Content 2")
        )
        val component = AccordionComponent(
            items = items,
            expandedIndices = setOf(0)
        )
        assertEquals(2, component.items.size)
        assertTrue(0 in component.expandedIndices)
    }

    @Test
    fun testAccordionAllowMultiple() {
        val items = listOf(
            AccordionItem("1", "Section 1", "Content 1")
        )
        val component = AccordionComponent(
            items = items,
            allowMultiple = true
        )
        assertTrue(component.allowMultiple)
    }

    @Test
    fun testAccordionValidation() {
        assertFails {
            AccordionComponent(items = emptyList())
        }
    }

    // ====================
    // ADVANCED COMPONENTS (3 tests)
    // ====================

    @Test
    fun testCardRendererCreation() {
        val renderer = IOSCardRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testGridRendererCreation() {
        val renderer = IOSGridRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testPopoverRendererCreation() {
        val renderer = IOSPopoverRenderer()
        assertNotNull(renderer)
    }

    // ====================
    // VALIDATION & EDGE CASES (5 tests)
    // ====================

    @Test
    fun testSkeletonCircularValidation() {
        assertFails {
            SkeletonComponent(
                width = 48f,
                height = 64f,  // Not equal to width
                variant = SkeletonVariant.CIRCULAR
            )
        }
    }

    @Test
    fun testSkeletonTextHeightValidation() {
        assertFails {
            SkeletonComponent(
                width = 200f,
                height = 5f,  // Too small for text
                variant = SkeletonVariant.TEXT
            )
        }
    }

    @Test
    fun testDividerThicknessValidation() {
        assertFails {
            DividerComponent(thickness = -1f)
        }
    }

    @Test
    fun testTooltipRendererCreation() {
        val renderer = IOSTooltipRenderer()
        assertNotNull(renderer)
    }

    @Test
    fun testTooltipWithText() {
        val component = TooltipComponent(text = "This is a tooltip")
        assertEquals("This is a tooltip", component.text)
        assertEquals(Position.TOP, component.position)
    }

    // ====================
    // INTEGRATION TESTS (5 tests)
    // ====================

    @Test
    fun testAvatarInitialsLength() {
        assertFails {
            AvatarComponent(initials = "TOOLONG")
        }
    }

    @Test
    fun testAvatarSizes() {
        val small = AvatarComponent(initials = "AB", size = ComponentSize.SM)
        val medium = AvatarComponent(initials = "CD", size = ComponentSize.MD)
        val large = AvatarComponent(initials = "EF", size = ComponentSize.LG)

        assertEquals(ComponentSize.SM, small.size)
        assertEquals(ComponentSize.MD, medium.size)
        assertEquals(ComponentSize.LG, large.size)
    }

    @Test
    fun testDividerWithIndent() {
        val component = DividerComponent.INDENTED
        assertEquals(16f, component.indent)
    }

    @Test
    fun testSkeletonCard() {
        val component = SkeletonComponent.card(width = 300f, height = 200f)
        assertEquals(SkeletonVariant.RECTANGULAR, component.variant)
        assertEquals(300f, component.width)
        assertEquals(200f, component.height)
    }

    @Test
    fun testBadgeStatus() {
        val component = BadgeComponent.status(text = "ACTIVE", color = Color.Green)
        assertEquals("ACTIVE", component.content)
        assertEquals(Color.Green, component.color)
    }
}
