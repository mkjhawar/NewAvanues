package com.augmentalis.avaelements.renderer.android.buttons

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avaelements.flutter.material.advanced.SplitButton
import com.augmentalis.avaelements.flutter.material.advanced.LoadingButton
import com.augmentalis.avaelements.flutter.material.advanced.CloseButton
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.SplitButtonMapper
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.LoadingButtonMapper
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.CloseButtonMapper
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Integration tests for P0 Button components
 *
 * Tests end-to-end rendering of 3 critical button components:
 * - SplitButton - Button with primary action + dropdown menu
 * - LoadingButton - Button with loading state indicator
 * - CloseButton - Standardized close/dismiss button
 *
 * @since 3.1.0-android-parity
 */
@RunWith(AndroidJUnit4::class)
class ButtonComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============================================================================
    // SplitButton Tests
    // ============================================================================

    @Test
    fun testSplitButton_rendersMainButtonCorrectly() {
        val component = SplitButton(
            text = "Save",
            enabled = true,
            menuItems = listOf(
                SplitButton.MenuItem("draft", "Save as Draft"),
                SplitButton.MenuItem("template", "Save as Template")
            ),
            onPressed = {}
        )

        composeTestRule.setContent {
            SplitButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Save").assertExists()
        composeTestRule.onNodeWithText("Save").assertIsEnabled()
    }

    @Test
    fun testSplitButton_mainButtonClick() {
        var mainButtonClicked = false
        val component = SplitButton(
            text = "Save",
            enabled = true,
            menuItems = listOf(
                SplitButton.MenuItem("draft", "Save as Draft")
            ),
            onPressed = { mainButtonClicked = true }
        )

        composeTestRule.setContent {
            SplitButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Save").performClick()
        assertTrue(mainButtonClicked, "Main button should be clicked")
    }

    @Test
    fun testSplitButton_menuOpensAndItemsVisible() {
        val component = SplitButton(
            text = "Save",
            enabled = true,
            menuItems = listOf(
                SplitButton.MenuItem("draft", "Save as Draft"),
                SplitButton.MenuItem("template", "Save as Template")
            ),
            onPressed = {}
        )

        composeTestRule.setContent {
            SplitButtonMapper(component)
        }

        // Click dropdown button (has ArrowDropDown icon)
        composeTestRule.onNodeWithContentDescription("Show menu options").performClick()

        // Check menu items are visible
        composeTestRule.onNodeWithText("Save as Draft").assertExists()
        composeTestRule.onNodeWithText("Save as Template").assertExists()
    }

    @Test
    fun testSplitButton_menuItemClick() {
        var selectedItem = ""
        val component = SplitButton(
            text = "Save",
            enabled = true,
            menuItems = listOf(
                SplitButton.MenuItem("draft", "Save as Draft"),
                SplitButton.MenuItem("template", "Save as Template")
            ),
            onPressed = {},
            onMenuItemPressed = { value -> selectedItem = value }
        )

        composeTestRule.setContent {
            SplitButtonMapper(component)
        }

        // Open menu
        composeTestRule.onNodeWithContentDescription("Show menu options").performClick()

        // Click menu item
        composeTestRule.onNodeWithText("Save as Draft").performClick()

        assertTrue(selectedItem == "draft", "Menu item should be selected")
    }

    @Test
    fun testSplitButton_disabledState() {
        val component = SplitButton(
            text = "Save",
            enabled = false,
            menuItems = listOf(
                SplitButton.MenuItem("draft", "Save as Draft")
            ),
            onPressed = {}
        )

        composeTestRule.setContent {
            SplitButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Save").assertIsNotEnabled()
    }

    @Test
    fun testSplitButton_withIcon() {
        val component = SplitButton(
            text = "Save",
            icon = "save",
            enabled = true,
            menuItems = listOf(
                SplitButton.MenuItem("draft", "Save as Draft")
            ),
            onPressed = {}
        )

        composeTestRule.setContent {
            SplitButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Save").assertExists()
    }

    // ============================================================================
    // LoadingButton Tests
    // ============================================================================

    @Test
    fun testLoadingButton_rendersCorrectly() {
        val component = LoadingButton(
            text = "Submit",
            enabled = true,
            loading = false,
            onPressed = {}
        )

        composeTestRule.setContent {
            LoadingButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Submit").assertExists()
        composeTestRule.onNodeWithText("Submit").assertIsEnabled()
    }

    @Test
    fun testLoadingButton_click() {
        var buttonClicked = false
        val component = LoadingButton(
            text = "Submit",
            enabled = true,
            loading = false,
            onPressed = { buttonClicked = true }
        )

        composeTestRule.setContent {
            LoadingButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Submit").performClick()
        assertTrue(buttonClicked, "Button should be clicked")
    }

    @Test
    fun testLoadingButton_loadingState() {
        val component = LoadingButton(
            text = "Submit",
            enabled = true,
            loading = true,
            onPressed = {}
        )

        composeTestRule.setContent {
            LoadingButtonMapper(component)
        }

        // Button should be disabled when loading
        composeTestRule.onNodeWithText("Submit").assertIsNotEnabled()
    }

    @Test
    fun testLoadingButton_loadingWithCustomText() {
        val component = LoadingButton(
            text = "Submit",
            loadingText = "Submitting...",
            enabled = true,
            loading = true,
            onPressed = {}
        )

        composeTestRule.setContent {
            LoadingButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Submitting...").assertExists()
    }

    @Test
    fun testLoadingButton_disabledState() {
        val component = LoadingButton(
            text = "Submit",
            enabled = false,
            loading = false,
            onPressed = {}
        )

        composeTestRule.setContent {
            LoadingButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Submit").assertIsNotEnabled()
    }

    @Test
    fun testLoadingButton_notClickableWhenLoading() {
        var buttonClicked = false
        val component = LoadingButton(
            text = "Submit",
            enabled = true,
            loading = true,
            onPressed = { buttonClicked = true }
        )

        composeTestRule.setContent {
            LoadingButtonMapper(component)
        }

        // Try to click - should not trigger callback
        composeTestRule.onNodeWithText("Submit").assertIsNotEnabled()
        assertFalse(buttonClicked, "Button should not be clickable when loading")
    }

    // ============================================================================
    // CloseButton Tests
    // ============================================================================

    @Test
    fun testCloseButton_rendersCorrectly() {
        val component = CloseButton(
            enabled = true,
            onPressed = {}
        )

        composeTestRule.setContent {
            CloseButtonMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun testCloseButton_click() {
        var buttonClicked = false
        val component = CloseButton(
            enabled = true,
            onPressed = { buttonClicked = true }
        )

        composeTestRule.setContent {
            CloseButtonMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Close").performClick()
        assertTrue(buttonClicked, "Close button should be clicked")
    }

    @Test
    fun testCloseButton_disabledState() {
        val component = CloseButton(
            enabled = false,
            onPressed = {}
        )

        composeTestRule.setContent {
            CloseButtonMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Close").assertIsNotEnabled()
    }

    @Test
    fun testCloseButton_customAccessibilityLabel() {
        val component = CloseButton(
            enabled = true,
            contentDescription = "Dismiss Dialog",
            onPressed = {}
        )

        composeTestRule.setContent {
            CloseButtonMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Dismiss Dialog").assertExists()
    }

    @Test
    fun testCloseButton_smallSize() {
        val component = CloseButton(
            enabled = true,
            size = CloseButton.Size.Small,
            onPressed = {}
        )

        composeTestRule.setContent {
            CloseButtonMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun testCloseButton_largeSize() {
        val component = CloseButton(
            enabled = true,
            size = CloseButton.Size.Large,
            onPressed = {}
        )

        composeTestRule.setContent {
            CloseButtonMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }

    @Test
    fun testCloseButton_withEdgePosition() {
        val component = CloseButton(
            enabled = true,
            edge = CloseButton.EdgePosition.End,
            onPressed = {}
        )

        composeTestRule.setContent {
            CloseButtonMapper(component)
        }

        composeTestRule.onNodeWithContentDescription("Close").assertExists()
    }
}
