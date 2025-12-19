package com.augmentalis.avaelements.renderer.android.layout

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avaelements.flutter.material.layout.MasonryGrid
import com.augmentalis.avaelements.flutter.material.layout.AspectRatio
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Instrumented tests for Advanced Layout components (2 components)
 *
 * Tests:
 * - MasonryGrid (6 tests)
 * - AspectRatio (6 tests)
 *
 * Total: 12 tests
 */
@RunWith(AndroidJUnit4::class)
class LayoutComponentsAdvancedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== MasonryGrid Component Tests (6 tests) ==========

    @Test
    fun testMasonryGrid_FixedColumns() {
        composeTestRule.setContent {
            MasonryGridMapper(
                MasonryGrid(
                    items = emptyList(),
                    columns = MasonryGrid.Columns.Fixed(3)
                )
            )
        }

        // Grid is rendered (verified by no crashes)
        composeTestRule.waitForIdle()
    }

    @Test
    fun testMasonryGrid_AdaptiveColumns() {
        composeTestRule.setContent {
            MasonryGridMapper(
                MasonryGrid(
                    items = emptyList(),
                    columns = MasonryGrid.Columns.Adaptive(200f)
                )
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun testMasonryGrid_ValidatesSpacings() {
        val grid = MasonryGrid(
            items = emptyList(),
            horizontalSpacing = 16f,
            verticalSpacing = 16f,
            minItemWidth = 180f
        )

        assertTrue(grid.areSpacingsValid())
    }

    @Test
    fun testMasonryGrid_InvalidSpacingsDetected() {
        val grid = MasonryGrid(
            items = emptyList(),
            horizontalSpacing = -5f,
            verticalSpacing = 16f
        )

        assertFalse(grid.areSpacingsValid())
    }

    @Test
    fun testMasonryGrid_FactoryMethods() {
        val twoColumn = MasonryGrid.twoColumn(emptyList())
        val threeColumn = MasonryGrid.threeColumn(emptyList())
        val adaptive = MasonryGrid.adaptive(emptyList())
        val pinterest = MasonryGrid.pinterest(emptyList())

        assertEquals(2, twoColumn.getFixedColumnCount())
        assertEquals(3, threeColumn.getFixedColumnCount())
        assertTrue(adaptive.columns is MasonryGrid.Columns.Adaptive)
        assertTrue(pinterest.columns is MasonryGrid.Columns.Adaptive)
    }

    @Test
    fun testMasonryGrid_AccessibilityDescription() {
        val grid = MasonryGrid(
            items = emptyList(),
            columns = MasonryGrid.Columns.Fixed(2),
            contentDescription = "Image gallery"
        )

        val description = grid.getAccessibilityDescription()
        assertTrue(description.contains("Image gallery"))
        assertTrue(description.contains("2 columns"))
    }

    // ========== AspectRatio Component Tests (6 tests) ==========

    @Test
    fun testAspectRatio_SixteenByNine() {
        composeTestRule.setContent {
            AspectRatioMapper(
                AspectRatio(
                    ratio = AspectRatio.Ratio.SixteenByNine
                )
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun testAspectRatio_Square() {
        composeTestRule.setContent {
            AspectRatioMapper(
                AspectRatio(
                    ratio = AspectRatio.Ratio.Square
                )
            )
        }

        composeTestRule.waitForIdle()
    }

    @Test
    fun testAspectRatio_CustomRatio() {
        val aspectRatio = AspectRatio(
            ratio = AspectRatio.Ratio.Custom(2.5f)
        )

        assertEquals(2.5f, aspectRatio.getRatioValue())
    }

    @Test
    fun testAspectRatio_ValidatesRatio() {
        val validRatio = AspectRatio(
            ratio = AspectRatio.Ratio.SixteenByNine
        )

        assertTrue(validRatio.isRatioValid())
    }

    @Test
    fun testAspectRatio_FactoryMethods() {
        val widescreen = AspectRatio.widescreen()
        val square = AspectRatio.square()
        val standard = AspectRatio.standard()
        val custom = AspectRatio.custom(1.85f)

        assertEquals(16f / 9f, widescreen.getRatioValue())
        assertEquals(1f, square.getRatioValue())
        assertEquals(4f / 3f, standard.getRatioValue())
        assertEquals(1.85f, custom.getRatioValue())
    }

    @Test
    fun testAspectRatio_FromDimensions() {
        val aspectRatio = AspectRatio.fromDimensions(1920f, 1080f)

        assertEquals(1920f / 1080f, aspectRatio.getRatioValue())
    }
}
