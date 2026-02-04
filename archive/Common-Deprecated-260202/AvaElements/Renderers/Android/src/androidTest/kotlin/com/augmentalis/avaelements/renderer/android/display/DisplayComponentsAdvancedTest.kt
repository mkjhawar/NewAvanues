package com.augmentalis.avaelements.renderer.android.display

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.augmentalis.avaelements.flutter.material.display.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test

/**
 * Comprehensive test suite for Android Display components
 *
 * Tests 5 display components with ~25 test cases total covering:
 * - Component rendering
 * - Content display
 * - User interactions
 * - Accessibility
 * - State management
 * - Animation states
 * - Edge cases
 */
class DisplayComponentsAdvancedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ==================== AvatarGroup Tests (5 tests) ====================

    @Test
    fun testAvatarGroup_renders() {
        val avatars = listOf(
            AvatarGroup.Avatar("1", "John Doe", initials = "JD"),
            AvatarGroup.Avatar("2", "Jane Smith", initials = "JS"),
            AvatarGroup.Avatar("3", "Bob Johnson", initials = "BJ")
        )

        composeTestRule.setContent {
            AvatarGroupMapper(
                AvatarGroup(
                    avatars = avatars,
                    max = 3
                )
            )
        }

        composeTestRule.onNodeWithText("JD").assertIsDisplayed()
        composeTestRule.onNodeWithText("JS").assertIsDisplayed()
        composeTestRule.onNodeWithText("BJ").assertIsDisplayed()
    }

    @Test
    fun testAvatarGroup_overflow() {
        val avatars = listOf(
            AvatarGroup.Avatar("1", "User 1", initials = "U1"),
            AvatarGroup.Avatar("2", "User 2", initials = "U2"),
            AvatarGroup.Avatar("3", "User 3", initials = "U3"),
            AvatarGroup.Avatar("4", "User 4", initials = "U4"),
            AvatarGroup.Avatar("5", "User 5", initials = "U5")
        )

        composeTestRule.setContent {
            AvatarGroupMapper(
                AvatarGroup(
                    avatars = avatars,
                    max = 3
                )
            )
        }

        composeTestRule.onNodeWithText("+2").assertIsDisplayed()
    }

    @Test
    fun testAvatarGroup_clickable() {
        var clicked = false
        val avatars = listOf(
            AvatarGroup.Avatar("1", "User", initials = "U")
        )

        composeTestRule.setContent {
            AvatarGroupMapper(
                AvatarGroup(
                    avatars = avatars,
                    onPressed = { clicked = true }
                )
            )
        }

        composeTestRule.onNodeWithText("+0").performClick()
        // Note: Since we only have 1 avatar, clicking on overflow won't exist,
        // but the component should handle onPressed
    }

    @Test
    fun testAvatarGroup_avatarClick() {
        var clickedId = ""
        val avatars = listOf(
            AvatarGroup.Avatar("user1", "Test User", initials = "TU")
        )

        composeTestRule.setContent {
            AvatarGroupMapper(
                AvatarGroup(
                    avatars = avatars,
                    onAvatarPressed = { clickedId = it }
                )
            )
        }

        composeTestRule.onNodeWithText("TU").performClick()
        assert(clickedId == "user1")
    }

    @Test
    fun testAvatarGroup_accessibility() {
        val avatars = listOf(
            AvatarGroup.Avatar("1", "Alice", initials = "A"),
            AvatarGroup.Avatar("2", "Bob", initials = "B")
        )

        composeTestRule.setContent {
            AvatarGroupMapper(
                AvatarGroup(
                    avatars = avatars,
                    contentDescription = "Team members"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Team members, 2 members"))
            .assertExists()
    }

    // ==================== SkeletonText Tests (5 tests) ====================

    @Test
    fun testSkeletonText_singleLine() {
        composeTestRule.setContent {
            SkeletonTextMapper(
                SkeletonText(
                    variant = SkeletonText.Variant.Body1,
                    lines = 1
                )
            )
        }

        // Skeleton should be displayed (checking for rendered content)
        composeTestRule.onNode(hasContentDescription("Loading text content, 1 lines"))
            .assertExists()
    }

    @Test
    fun testSkeletonText_multiLine() {
        composeTestRule.setContent {
            SkeletonTextMapper(
                SkeletonText(
                    variant = SkeletonText.Variant.Body1,
                    lines = 3,
                    lastLineWidth = 0.7f
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading text content, 3 lines"))
            .assertExists()
    }

    @Test
    fun testSkeletonText_heading() {
        composeTestRule.setContent {
            SkeletonTextMapper(
                SkeletonText(
                    variant = SkeletonText.Variant.H2,
                    lines = 1
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading text content, 1 lines"))
            .assertExists()
    }

    @Test
    fun testSkeletonText_animation() {
        composeTestRule.setContent {
            SkeletonTextMapper(
                SkeletonText(
                    variant = SkeletonText.Variant.Body1,
                    lines = 2,
                    animation = SkeletonText.Animation.Wave
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading text content, 2 lines"))
            .assertExists()
    }

    @Test
    fun testSkeletonText_accessibility() {
        composeTestRule.setContent {
            SkeletonTextMapper(
                SkeletonText(
                    variant = SkeletonText.Variant.Body1,
                    lines = 1,
                    contentDescription = "Loading article title"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading article title"))
            .assertExists()
    }

    // ==================== SkeletonCircle Tests (4 tests) ====================

    @Test
    fun testSkeletonCircle_renders() {
        composeTestRule.setContent {
            SkeletonCircleMapper(
                SkeletonCircle(
                    diameter = 40f
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading circular content"))
            .assertExists()
    }

    @Test
    fun testSkeletonCircle_small() {
        composeTestRule.setContent {
            SkeletonCircleMapper(
                SkeletonCircle.small()
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading circular content"))
            .assertExists()
    }

    @Test
    fun testSkeletonCircle_large() {
        composeTestRule.setContent {
            SkeletonCircleMapper(
                SkeletonCircle.large()
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading circular content"))
            .assertExists()
    }

    @Test
    fun testSkeletonCircle_accessibility() {
        composeTestRule.setContent {
            SkeletonCircleMapper(
                SkeletonCircle(
                    diameter = 48f,
                    contentDescription = "Loading avatar"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading avatar"))
            .assertExists()
    }

    // ==================== ProgressCircle Tests (6 tests) ====================

    @Test
    fun testProgressCircle_indeterminate() {
        composeTestRule.setContent {
            ProgressCircleMapper(
                ProgressCircle.indeterminate()
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading"))
            .assertExists()
    }

    @Test
    fun testProgressCircle_determinate() {
        composeTestRule.setContent {
            ProgressCircleMapper(
                ProgressCircle.determinate(value = 0.5f)
            )
        }

        composeTestRule.onNode(hasContentDescription("Progress 50 percent"))
            .assertExists()
    }

    @Test
    fun testProgressCircle_withLabel() {
        composeTestRule.setContent {
            ProgressCircleMapper(
                ProgressCircle.withLabel(value = 0.75f)
            )
        }

        composeTestRule.onNodeWithText("75%").assertIsDisplayed()
    }

    @Test
    fun testProgressCircle_customLabel() {
        composeTestRule.setContent {
            ProgressCircleMapper(
                ProgressCircle(
                    value = 0.8f,
                    showLabel = true,
                    labelText = "80% Complete"
                )
            )
        }

        composeTestRule.onNodeWithText("80% Complete").assertIsDisplayed()
    }

    @Test
    fun testProgressCircle_zeroProgress() {
        composeTestRule.setContent {
            ProgressCircleMapper(
                ProgressCircle(
                    value = 0f,
                    showLabel = true
                )
            )
        }

        composeTestRule.onNodeWithText("0%").assertIsDisplayed()
    }

    @Test
    fun testProgressCircle_accessibility() {
        composeTestRule.setContent {
            ProgressCircleMapper(
                ProgressCircle(
                    value = 0.65f,
                    contentDescription = "Upload progress"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Upload progress"))
            .assertExists()
    }

    // ==================== LoadingOverlay Tests (5 tests) ====================

    @Test
    fun testLoadingOverlay_renders() {
        composeTestRule.setContent {
            LoadingOverlayMapper(
                LoadingOverlay(
                    visible = true,
                    message = "Loading..."
                )
            )
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun testLoadingOverlay_hidden() {
        composeTestRule.setContent {
            LoadingOverlayMapper(
                LoadingOverlay(
                    visible = false,
                    message = "Loading..."
                )
            )
        }

        composeTestRule.onNodeWithText("Loading...").assertDoesNotExist()
    }

    @Test
    fun testLoadingOverlay_cancelable() {
        var cancelled = false
        composeTestRule.setContent {
            LoadingOverlayMapper(
                LoadingOverlay(
                    visible = true,
                    message = "Processing...",
                    cancelable = true,
                    cancelText = "Cancel",
                    onCancel = { cancelled = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        assert(cancelled)
    }

    @Test
    fun testLoadingOverlay_notCancelable() {
        composeTestRule.setContent {
            LoadingOverlayMapper(
                LoadingOverlay(
                    visible = true,
                    message = "Please wait",
                    cancelable = false
                )
            )
        }

        composeTestRule.onNodeWithText("Cancel").assertDoesNotExist()
    }

    @Test
    fun testLoadingOverlay_accessibility() {
        composeTestRule.setContent {
            LoadingOverlayMapper(
                LoadingOverlay(
                    visible = true,
                    message = "Loading data",
                    contentDescription = "Data loading overlay"
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Data loading overlay, Loading data"))
            .assertExists()
    }

    // ==================== Integration Tests (additional edge cases) ====================

    @Test
    fun testAvatarGroup_emptyList() {
        composeTestRule.setContent {
            AvatarGroupMapper(
                AvatarGroup(
                    avatars = emptyList()
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Avatar group, 0 members"))
            .assertExists()
    }

    @Test
    fun testSkeletonText_customWidth() {
        composeTestRule.setContent {
            SkeletonTextMapper(
                SkeletonText(
                    variant = SkeletonText.Variant.Body1,
                    lines = 1,
                    width = 200f
                )
            )
        }

        composeTestRule.onNode(hasContentDescription("Loading text content, 1 lines"))
            .assertExists()
    }

    @Test
    fun testProgressCircle_fullProgress() {
        composeTestRule.setContent {
            ProgressCircleMapper(
                ProgressCircle(
                    value = 1.0f,
                    showLabel = true
                )
            )
        }

        composeTestRule.onNodeWithText("100%").assertIsDisplayed()
    }

    @Test
    fun testLoadingOverlay_containerMode() {
        composeTestRule.setContent {
            LoadingOverlayMapper(
                LoadingOverlay.container(
                    visible = true,
                    message = "Loading section"
                )
            )
        }

        composeTestRule.onNodeWithText("Loading section").assertIsDisplayed()
    }
}
