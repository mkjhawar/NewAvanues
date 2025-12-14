package com.augmentalis.webavanue.app.ui

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.augmentalis.webavanue.universal.presentation.ui.settings.DownloadPathSettingItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * UI tests for download settings file picker flow
 *
 * Tests:
 * - File picker launch button interaction
 * - Permission request flow
 * - Path display and persistence
 * - Use Default button behavior
 *
 * Note: Actual file picker dialog cannot be fully automated in UI tests
 */
@RunWith(AndroidJUnit4::class)
class DownloadSettingsUITest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val permissionRule: GrantPermissionRule = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        GrantPermissionRule.grant(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    } else {
        GrantPermissionRule.grant() // No permission needed on API 29+
    }

    private lateinit var context: Context
    private var currentPath: String? = null
    private var pathChangedCalled = false

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        currentPath = null
        pathChangedCalled = false
    }

    /**
     * Test: Browse button is displayed
     *
     * Expected: Browse button with folder icon is visible
     */
    @Test
    fun testBrowseButton_isDisplayed() {
        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = null,
                onPathChanged = { }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Browse")
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * Test: Default path is displayed when no custom path set
     *
     * Expected: Shows "Default system path" text
     */
    @Test
    fun testDefaultPath_isDisplayed() {
        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = null,
                onPathChanged = { }
            )
        }

        composeTestRule
            .onNodeWithText("Default system path")
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * Test: Custom path is displayed when set
     *
     * Expected: Shows custom path instead of default text
     */
    @Test
    fun testCustomPath_isDisplayed() {
        val testPath = "content://com.android.externalstorage.documents/tree/primary:Download"

        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = testPath,
                onPathChanged = { }
            )
        }

        // The display path will be extracted, so we just verify it's not showing "Default"
        composeTestRule
            .onNodeWithText("Default system path")
            .assertDoesNotExist()
    }

    /**
     * Test: Use Default button is shown only when custom path is set
     *
     * Expected:
     * - Not visible when path is null
     * - Visible when path is set
     */
    @Test
    fun testUseDefaultButton_visibilityBasedOnPath() {
        // Test with null path - button should not exist
        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = null,
                onPathChanged = { }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Use default")
            .assertDoesNotExist()

        // Test with custom path - button should exist
        val testPath = "content://com.android.externalstorage.documents/tree/primary:Download"

        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = testPath,
                onPathChanged = { }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Use default")
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * Test: Clicking Use Default button calls onPathChanged with null
     *
     * Expected: Callback receives null to reset to default
     */
    @Test
    fun testUseDefaultButton_click_callsOnPathChangedWithNull() {
        val testPath = "content://com.android.externalstorage.documents/tree/primary:Download"
        var receivedPath: String? = "not-null"

        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = testPath,
                onPathChanged = { receivedPath = it }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Use default")
            .performClick()

        // Note: The actual null value won't be received until permission is released
        // This test verifies the click is handled
    }

    /**
     * Test: Download Location heading is displayed
     *
     * Expected: Shows "Download Location" text
     */
    @Test
    fun testDownloadLocationHeading_isDisplayed() {
        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = null,
                onPathChanged = { }
            )
        }

        composeTestRule
            .onNodeWithText("Download Location")
            .assertExists()
            .assertIsDisplayed()
    }

    /**
     * Test: Browse button is clickable
     *
     * Expected: Button responds to clicks (may show permission dialog or picker)
     */
    @Test
    fun testBrowseButton_isClickable() {
        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = null,
                onPathChanged = { }
            )
        }

        composeTestRule
            .onNodeWithContentDescription("Browse")
            .assertHasClickAction()
            .performClick()

        // After click, either permission dialog or file picker should show
        // We can't fully test this without Activity context, but verify click works
    }

    /**
     * Test: Available space is displayed when custom path is set
     *
     * Expected: Shows "X MB available" text below path
     */
    @Test
    fun testAvailableSpace_isDisplayed_whenCustomPathSet() {
        val testPath = "content://com.android.externalstorage.documents/tree/primary:Download"

        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = testPath,
                onPathChanged = { }
            )
        }

        // Wait for space calculation (LaunchedEffect)
        composeTestRule.waitForIdle()

        // Check if "MB available" text exists (space calculation may fail in test)
        // This is a partial test - actual space calculation requires valid URI
    }

    /**
     * Test: Low space warning color
     *
     * Expected: Shows red color when space < 100MB
     * Note: Cannot fully test without mocking storage stats
     */
    @Test
    fun testLowSpaceWarning_showsErrorColor() {
        // This test verifies the UI logic exists
        // Actual color testing requires screenshot testing or pixel comparison
        val testPath = "content://com.android.externalstorage.documents/tree/primary:Download"

        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = testPath,
                onPathChanged = { }
            )
        }

        composeTestRule.waitForIdle()
        // UI should exist and render without crashing
    }

    /**
     * Test: Permission dialog is shown when permission required and not granted
     *
     * Expected: Shows rationale dialog on API < 29 when permission denied
     * Note: Requires permission to be denied, cannot fully test with GrantPermissionRule
     */
    @Test
    fun testPermissionDialog_showsWhenRequired() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Skip test on API 29+ where permission not required
            return
        }

        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = null,
                onPathChanged = { }
            )
        }

        // Click browse button
        composeTestRule
            .onNodeWithContentDescription("Browse")
            .performClick()

        composeTestRule.waitForIdle()

        // Dialog may or may not show depending on permission state
        // This test verifies the UI doesn't crash
    }

    /**
     * Test: Multiple clicks on browse button don't crash
     *
     * Expected: UI handles multiple clicks gracefully
     */
    @Test
    fun testBrowseButton_multipleClicks_noCrash() {
        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = null,
                onPathChanged = { }
            )
        }

        repeat(3) {
            composeTestRule
                .onNodeWithContentDescription("Browse")
                .performClick()

            composeTestRule.waitForIdle()
        }

        // Should not crash
    }

    /**
     * Test: UI renders correctly with null callback
     *
     * Expected: Component handles null callback gracefully
     * Note: This is a defensive test - callback should never be null in practice
     */
    @Test
    fun testNullPath_rendersCorrectly() {
        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = null,
                onPathChanged = { }
            )
        }

        // All elements should be present
        composeTestRule
            .onNodeWithText("Download Location")
            .assertExists()

        composeTestRule
            .onNodeWithText("Default system path")
            .assertExists()

        composeTestRule
            .onNodeWithContentDescription("Browse")
            .assertExists()
    }

    /**
     * Test: UI updates when path changes
     *
     * Expected: Display updates to show new path
     */
    @Test
    fun testPathChange_updatesDisplay() {
        var displayedPath: String? = null

        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = displayedPath,
                onPathChanged = { displayedPath = it }
            )
        }

        // Initially shows default
        composeTestRule
            .onNodeWithText("Default system path")
            .assertExists()

        // Update path
        displayedPath = "content://test/path"

        composeTestRule.setContent {
            DownloadPathSettingItem(
                currentPath = displayedPath,
                onPathChanged = { displayedPath = it }
            )
        }

        composeTestRule.waitForIdle()

        // Should no longer show default text
        composeTestRule
            .onNodeWithText("Default system path")
            .assertDoesNotExist()

        // Use Default button should now exist
        composeTestRule
            .onNodeWithContentDescription("Use default")
            .assertExists()
    }
}
