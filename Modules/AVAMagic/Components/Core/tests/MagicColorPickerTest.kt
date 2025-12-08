package net.ideahq.ideamagic.components.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * MagicColorPicker Component Tests
 *
 * @author Manoj Jhawar, manoj@ideahq.net
 * @version 1.0.0
 */
class MagicColorPickerTest {

    @Test
    fun `color picker creation with default values`() {
        val picker = MagicColorPicker(
            selectedColor = "#000000",
            onColorChange = {}
        )

        assertEquals("#000000", picker.selectedColor)
        assertEquals(ColorPickerMode.PALETTE, picker.mode)
        assertTrue(picker.showAlpha)
        assertEquals(null, picker.presetColors)
        assertEquals(null, picker.label)
    }

    @Test
    fun `color picker with all properties`() {
        val presets = listOf("#FF0000", "#00FF00", "#0000FF")
        val picker = MagicColorPicker(
            selectedColor = "#FF5733",
            onColorChange = {},
            mode = ColorPickerMode.HUE_WHEEL,
            showAlpha = false,
            presetColors = presets,
            label = "Theme Color"
        )

        assertEquals("#FF5733", picker.selectedColor)
        assertEquals(ColorPickerMode.HUE_WHEEL, picker.mode)
        assertFalse(picker.showAlpha)
        assertEquals(presets, picker.presetColors)
        assertEquals("Theme Color", picker.label)
    }

    @Test
    fun `color picker modes`() {
        val palette = MagicColorPicker("#000000", {}, mode = ColorPickerMode.PALETTE)
        val hueWheel = MagicColorPicker("#000000", {}, mode = ColorPickerMode.HUE_WHEEL)
        val spectrum = MagicColorPicker("#000000", {}, mode = ColorPickerMode.SPECTRUM)
        val sliders = MagicColorPicker("#000000", {}, mode = ColorPickerMode.SLIDERS)

        assertEquals(ColorPickerMode.PALETTE, palette.mode)
        assertEquals(ColorPickerMode.HUE_WHEEL, hueWheel.mode)
        assertEquals(ColorPickerMode.SPECTRUM, spectrum.mode)
        assertEquals(ColorPickerMode.SLIDERS, sliders.mode)
    }

    @Test
    fun `color picker with alpha channel`() {
        val picker = MagicColorPicker(
            selectedColor = "#FF000080",
            onColorChange = {},
            showAlpha = true
        )

        assertTrue(picker.showAlpha)
    }

    @Test
    fun `color picker without alpha channel`() {
        val picker = MagicColorPicker(
            selectedColor = "#FF0000",
            onColorChange = {},
            showAlpha = false
        )

        assertFalse(picker.showAlpha)
    }

    @Test
    fun `color picker with preset colors`() {
        val presets = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4"
        )
        val picker = MagicColorPicker(
            selectedColor = "#F44336",
            onColorChange = {},
            presetColors = presets
        )

        assertEquals(8, picker.presetColors?.size)
        assertEquals("#F44336", picker.presetColors?.get(0))
        assertEquals("#00BCD4", picker.presetColors?.get(7))
    }

    @Test
    fun `color picker with label`() {
        val picker = MagicColorPicker(
            selectedColor = "#000000",
            onColorChange = {},
            label = "Background Color"
        )

        assertEquals("Background Color", picker.label)
    }

    @Test
    fun `color picker change handler`() {
        var currentColor = "#000000"
        val picker = MagicColorPicker(
            selectedColor = currentColor,
            onColorChange = { currentColor = it }
        )

        picker.onColorChange("#FF0000")
        assertEquals("#FF0000", currentColor)

        picker.onColorChange("#00FF00")
        assertEquals("#00FF00", currentColor)
    }

    @Test
    fun `hex color format validation`() {
        val validColors = listOf(
            "#000000", "#FFFFFF", "#FF5733",
            "#F00", "#0F0", "#00F"
        )

        validColors.forEach { color ->
            val picker = MagicColorPicker(color, {})
            assertEquals(color, picker.selectedColor)
        }
    }

    @Test
    fun `theme editor use case`() {
        val materialColors = listOf(
            "#F44336", "#E91E63", "#9C27B0", "#673AB7",
            "#3F51B5", "#2196F3", "#03A9F4", "#00BCD4",
            "#009688", "#4CAF50", "#8BC34A", "#CDDC39"
        )

        var primaryColor = "#2196F3"
        val picker = MagicColorPicker(
            selectedColor = primaryColor,
            onColorChange = { primaryColor = it },
            mode = ColorPickerMode.PALETTE,
            presetColors = materialColors,
            label = "Primary Color",
            showAlpha = false
        )

        assertEquals(ColorPickerMode.PALETTE, picker.mode)
        assertEquals(12, picker.presetColors?.size)
        assertFalse(picker.showAlpha)
    }

    @Test
    fun `custom color selection use case`() {
        var selectedColor = "#000000"
        val picker = MagicColorPicker(
            selectedColor = selectedColor,
            onColorChange = { selectedColor = it },
            mode = ColorPickerMode.HUE_WHEEL,
            showAlpha = true,
            label = "Custom Color"
        )

        assertEquals(ColorPickerMode.HUE_WHEEL, picker.mode)
        assertTrue(picker.showAlpha)

        picker.onColorChange("#FF5733CC")
        assertEquals("#FF5733CC", selectedColor)
    }

    @Test
    fun `quick color picker use case`() {
        val basicColors = listOf(
            "#FF0000", "#00FF00", "#0000FF",
            "#FFFF00", "#FF00FF", "#00FFFF",
            "#000000", "#FFFFFF"
        )

        val picker = MagicColorPicker(
            selectedColor = "#FF0000",
            onColorChange = {},
            mode = ColorPickerMode.PALETTE,
            presetColors = basicColors,
            showAlpha = false
        )

        assertEquals(8, picker.presetColors?.size)
        assertEquals(ColorPickerMode.PALETTE, picker.mode)
    }
}
