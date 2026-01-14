package com.augmentalis.avaelements.flutter.material.chips

import kotlin.test.*

/**
 * Comprehensive unit tests for ActionChip component
 *
 * @since 3.0.0-flutter-parity
 */
class ActionChipTest {

    @Test
    fun `create ActionChip with default values`() {
        val chip = MagicAction(label = "Send")

        assertEquals("MagicAction", chip.type)
        assertEquals("Send", chip.label)
        assertTrue(chip.enabled)
        assertNull(chip.avatar)
        assertNull(chip.tooltip)
        assertNull(chip.onPressed)
    }

    @Test
    fun `ActionChip onPressed callback triggers action`() {
        var actionTriggered = false
        val chip = MagicAction(
            label = "Send",
            onPressed = { actionTriggered = true }
        )

        chip.onPressed?.invoke()
        assertTrue(actionTriggered)
    }

    @Test
    fun `ActionChip accessibility description uses contentDescription`() {
        val chip = MagicAction(
            label = "Send",
            contentDescription = "Send message"
        )
        assertEquals("Send message", chip.getAccessibilityDescription())
    }

    @Test
    fun `ActionChip accessibility description falls back to tooltip`() {
        val chip = MagicAction(
            label = "Send",
            tooltip = "Send message to recipient"
        )
        assertEquals("Send message to recipient", chip.getAccessibilityDescription())
    }

    @Test
    fun `ActionChip accessibility description falls back to label`() {
        val chip = MagicAction(label = "Send")
        assertEquals("Send", chip.getAccessibilityDescription())
    }

    @Test
    fun `ActionChip simple factory creates basic chip`() {
        val chip = ActionChip.simple("Delete")

        assertEquals("Delete", chip.label)
        assertNull(chip.avatar)
        assertNull(chip.tooltip)
    }

    @Test
    fun `ActionChip withAvatar factory includes avatar`() {
        val chip = ActionChip.withAvatar(
            label = "Share",
            avatar = "share_icon"
        )

        assertEquals("Share", chip.label)
        assertEquals("share_icon", chip.avatar)
    }

    @Test
    fun `ActionChip withTooltip factory includes tooltip`() {
        val chip = ActionChip.withTooltip(
            label = "Info",
            tooltip = "Show information"
        )

        assertEquals("Info", chip.label)
        assertEquals("Show information", chip.tooltip)
    }

    @Test
    fun `ActionChip disabled state prevents action`() {
        var actionTriggered = false
        val chip = MagicAction(
            label = "Send",
            enabled = false,
            onPressed = { actionTriggered = true }
        )

        assertFalse(chip.enabled)
    }

    @Test
    fun `ActionChip supports custom visual density`() {
        val compact = MagicAction(
            label = "Test",
            visualDensity = ActionChip.VisualDensity.Compact
        )
        assertEquals(ActionChip.VisualDensity.Compact, compact.visualDensity)

        val comfortable = MagicAction(
            label = "Test",
            visualDensity = ActionChip.VisualDensity.Comfortable
        )
        assertEquals(ActionChip.VisualDensity.Comfortable, comfortable.visualDensity)
    }

    @Test
    fun `ActionChip supports material tap target size`() {
        val padded = MagicAction(
            label = "Test",
            materialTapTargetSize = ActionChip.MaterialTapTargetSize.PadOrExpand
        )
        assertEquals(ActionChip.MaterialTapTargetSize.PadOrExpand, padded.materialTapTargetSize)

        val shrinkWrap = MagicAction(
            label = "Test",
            materialTapTargetSize = ActionChip.MaterialTapTargetSize.ShrinkWrap
        )
        assertEquals(ActionChip.MaterialTapTargetSize.ShrinkWrap, shrinkWrap.materialTapTargetSize)
    }

    @Test
    fun `ActionChip supports clip behavior`() {
        val chip = MagicAction(
            label = "Test",
            clipBehavior = ActionChip.ClipBehavior.AntiAlias
        )
        assertEquals(ActionChip.ClipBehavior.AntiAlias, chip.clipBehavior)
    }
}
