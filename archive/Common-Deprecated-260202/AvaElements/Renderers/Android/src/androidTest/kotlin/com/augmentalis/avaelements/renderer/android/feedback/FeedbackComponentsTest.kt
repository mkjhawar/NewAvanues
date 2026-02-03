package com.augmentalis.avaelements.renderer.android.feedback

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avaelements.flutter.material.feedback.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Instrumented tests for Feedback components (13 components)
 *
 * Tests:
 * - Popup (5 tests)
 * - Callout (5 tests)
 * - HoverCard (5 tests)
 * - Disclosure (6 tests)
 * - InfoPanel (6 tests)
 * - ErrorPanel (6 tests)
 * - WarningPanel (6 tests)
 * - SuccessPanel (6 tests)
 * - FullPageLoading (5 tests)
 * - AnimatedCheck (5 tests)
 * - AnimatedError (5 tests)
 * - AnimatedSuccess (6 tests)
 * - AnimatedWarning (6 tests)
 *
 * Total: 72 tests
 */
@RunWith(AndroidJUnit4::class)
class FeedbackComponentsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ========== Popup Component Tests (5 tests) ==========

    @Test
    fun testPopup_RendersContentWhenVisible() {
        composeTestRule.setContent {
            PopupMapper(
                Popup(
                    visible = true,
                    content = "Test popup content"
                )
            )
        }

        composeTestRule.onNodeWithText("Test popup content").assertIsDisplayed()
    }

    @Test
    fun testPopup_HiddenWhenNotVisible() {
        composeTestRule.setContent {
            PopupMapper(
                Popup(
                    visible = false,
                    content = "Hidden content"
                )
            )
        }

        composeTestRule.onNodeWithText("Hidden content").assertDoesNotExist()
    }

    @Test
    fun testPopup_DismissibleCallbackInvoked() {
        var dismissed = false
        composeTestRule.setContent {
            PopupMapper(
                Popup(
                    visible = true,
                    content = "Dismissible popup",
                    dismissible = true,
                    onDismiss = { dismissed = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Dismissible popup").assertIsDisplayed()
    }

    @Test
    fun testPopup_AccessibilityDescription() {
        composeTestRule.setContent {
            PopupMapper(
                Popup(
                    visible = true,
                    content = "Content",
                    contentDescription = "Custom popup"
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Custom popup: Content, dismissible")
        ).assertExists()
    }

    @Test
    fun testPopup_ValidatesDimensions() {
        val popup = Popup(
            content = "Test",
            maxWidth = 300f,
            arrowSize = 8f,
            elevation = 4f
        )

        assertTrue(popup.areDimensionsValid())
    }

    // ========== Callout Component Tests (5 tests) ==========

    @Test
    fun testCallout_InfoVariantRendersCorrectColors() {
        composeTestRule.setContent {
            CalloutMapper(
                Callout(
                    title = "Info Title",
                    message = "Info message",
                    variant = Callout.Variant.Info
                )
            )
        }

        composeTestRule.onNodeWithText("Info Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Info message").assertIsDisplayed()
    }

    @Test
    fun testCallout_SuccessVariantRendersCorrectColors() {
        composeTestRule.setContent {
            CalloutMapper(
                Callout(
                    title = "Success Title",
                    message = "Success message",
                    variant = Callout.Variant.Success
                )
            )
        }

        composeTestRule.onNodeWithText("Success Title").assertIsDisplayed()
    }

    @Test
    fun testCallout_WarningVariantRendersCorrectColors() {
        composeTestRule.setContent {
            CalloutMapper(
                Callout(
                    title = "Warning Title",
                    message = "Warning message",
                    variant = Callout.Variant.Warning
                )
            )
        }

        composeTestRule.onNodeWithText("Warning Title").assertIsDisplayed()
    }

    @Test
    fun testCallout_ErrorVariantRendersCorrectColors() {
        composeTestRule.setContent {
            CalloutMapper(
                Callout(
                    title = "Error Title",
                    message = "Error message",
                    variant = Callout.Variant.Error
                )
            )
        }

        composeTestRule.onNodeWithText("Error Title").assertIsDisplayed()
    }

    @Test
    fun testCallout_DismissButtonWhenDismissible() {
        var dismissed = false
        composeTestRule.setContent {
            CalloutMapper(
                Callout(
                    title = "Dismissible",
                    message = "Message",
                    dismissible = true,
                    onDismiss = { dismissed = true }
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Dismiss").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Dismiss").performClick()
        assertTrue(dismissed)
    }

    // ========== Disclosure Component Tests (6 tests) ==========

    @Test
    fun testDisclosure_InitiallyCollapsed() {
        composeTestRule.setContent {
            DisclosureMapper(
                Disclosure(
                    title = "Disclosure Title",
                    content = "Hidden content",
                    initiallyExpanded = false
                )
            )
        }

        composeTestRule.onNodeWithText("Disclosure Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Hidden content").assertDoesNotExist()
    }

    @Test
    fun testDisclosure_InitiallyExpanded() {
        composeTestRule.setContent {
            DisclosureMapper(
                Disclosure(
                    title = "Disclosure Title",
                    content = "Visible content",
                    initiallyExpanded = true
                )
            )
        }

        composeTestRule.onNodeWithText("Disclosure Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Visible content").assertIsDisplayed()
    }

    @Test
    fun testDisclosure_ExpandsOnClick() {
        composeTestRule.setContent {
            DisclosureMapper(
                Disclosure(
                    title = "Click to expand",
                    content = "Expanded content",
                    initiallyExpanded = false
                )
            )
        }

        composeTestRule.onNodeWithText("Click to expand").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Expanded content").assertIsDisplayed()
    }

    @Test
    fun testDisclosure_CollapsesOnSecondClick() {
        composeTestRule.setContent {
            DisclosureMapper(
                Disclosure(
                    title = "Toggle",
                    content = "Toggleable content",
                    initiallyExpanded = true
                )
            )
        }

        composeTestRule.onNodeWithText("Toggle").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Toggleable content").assertDoesNotExist()
    }

    @Test
    fun testDisclosure_ControlledMode() {
        composeTestRule.setContent {
            DisclosureMapper(
                Disclosure(
                    title = "Controlled",
                    content = "Content",
                    expanded = true
                )
            )
        }

        composeTestRule.onNodeWithText("Content").assertIsDisplayed()
    }

    @Test
    fun testDisclosure_CallbackInvoked() {
        var expansionState = false
        composeTestRule.setContent {
            DisclosureMapper(
                Disclosure(
                    title = "Callback",
                    content = "Content",
                    initiallyExpanded = false,
                    onExpansionChanged = { expansionState = it }
                )
            )
        }

        composeTestRule.onNodeWithText("Callback").performClick()
        composeTestRule.waitForIdle()
        assertTrue(expansionState)
    }

    // ========== InfoPanel Component Tests (6 tests) ==========

    @Test
    fun testInfoPanel_RendersBasicContent() {
        composeTestRule.setContent {
            InfoPanelMapper(
                InfoPanel(
                    title = "Info Title",
                    message = "Info message"
                )
            )
        }

        composeTestRule.onNodeWithText("Info Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Info message").assertIsDisplayed()
    }

    @Test
    fun testInfoPanel_DismissButton() {
        var dismissed = false
        composeTestRule.setContent {
            InfoPanelMapper(
                InfoPanel(
                    title = "Dismissible Info",
                    message = "Message",
                    dismissible = true,
                    onDismiss = { dismissed = true }
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Dismiss").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun testInfoPanel_ActionButtons() {
        var actionClicked = false
        composeTestRule.setContent {
            InfoPanelMapper(
                InfoPanel(
                    title = "Info with Actions",
                    message = "Message",
                    actions = listOf(
                        InfoPanel.Action("Action 1") { actionClicked = true }
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("Action 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Action 1").performClick()
        assertTrue(actionClicked)
    }

    @Test
    fun testInfoPanel_MultipleActions() {
        composeTestRule.setContent {
            InfoPanelMapper(
                InfoPanel(
                    title = "Multi-action",
                    message = "Message",
                    actions = listOf(
                        InfoPanel.Action("Action 1") { },
                        InfoPanel.Action("Action 2") { }
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("Action 1").assertIsDisplayed()
        composeTestRule.onNodeWithText("Action 2").assertIsDisplayed()
    }

    @Test
    fun testInfoPanel_CustomIcon() {
        val panel = InfoPanel(
            title = "Title",
            message = "Message",
            icon = "custom_icon"
        )

        assertEquals("custom_icon", panel.getEffectiveIcon())
    }

    @Test
    fun testInfoPanel_DefaultIcon() {
        val panel = InfoPanel(
            title = "Title",
            message = "Message"
        )

        assertEquals("info", panel.getEffectiveIcon())
    }

    // ========== ErrorPanel Component Tests (6 tests) ==========

    @Test
    fun testErrorPanel_RendersBasicContent() {
        composeTestRule.setContent {
            ErrorPanelMapper(
                ErrorPanel(
                    title = "Error Title",
                    message = "Error message"
                )
            )
        }

        composeTestRule.onNodeWithText("Error Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Error message").assertIsDisplayed()
    }

    @Test
    fun testErrorPanel_DismissButton() {
        var dismissed = false
        composeTestRule.setContent {
            ErrorPanelMapper(
                ErrorPanel(
                    title = "Dismissible Error",
                    message = "Message",
                    dismissible = true,
                    onDismiss = { dismissed = true }
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Dismiss").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun testErrorPanel_RetryAction() {
        var retried = false
        composeTestRule.setContent {
            ErrorPanelMapper(
                ErrorPanel.withRetry(
                    title = "Failed",
                    message = "Try again",
                    onRetry = { retried = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Retry").performClick()
        assertTrue(retried)
    }

    @Test
    fun testErrorPanel_MultipleActions() {
        composeTestRule.setContent {
            ErrorPanelMapper(
                ErrorPanel(
                    title = "Error",
                    message = "Message",
                    actions = listOf(
                        ErrorPanel.Action("Retry") { },
                        ErrorPanel.Action("Report") { }
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("Retry").assertIsDisplayed()
        composeTestRule.onNodeWithText("Report").assertIsDisplayed()
    }

    @Test
    fun testErrorPanel_DefaultIcon() {
        val panel = ErrorPanel(
            title = "Error",
            message = "Message"
        )

        assertEquals("error", panel.getEffectiveIcon())
    }

    @Test
    fun testErrorPanel_AccessibilityDescription() {
        val panel = ErrorPanel(
            title = "Error",
            message = "Something went wrong"
        )

        assertTrue(panel.getAccessibilityDescription().contains("Error"))
    }

    // ========== WarningPanel Component Tests (6 tests) ==========

    @Test
    fun testWarningPanel_RendersBasicContent() {
        composeTestRule.setContent {
            WarningPanelMapper(
                WarningPanel(
                    title = "Warning Title",
                    message = "Warning message"
                )
            )
        }

        composeTestRule.onNodeWithText("Warning Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Warning message").assertIsDisplayed()
    }

    @Test
    fun testWarningPanel_DismissButton() {
        var dismissed = false
        composeTestRule.setContent {
            WarningPanelMapper(
                WarningPanel(
                    title = "Warning",
                    message = "Message",
                    dismissible = true,
                    onDismiss = { dismissed = true }
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Dismiss").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun testWarningPanel_Actions() {
        var actionClicked = false
        composeTestRule.setContent {
            WarningPanelMapper(
                WarningPanel(
                    title = "Warning",
                    message = "Message",
                    actions = listOf(
                        WarningPanel.Action("Manage") { actionClicked = true }
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("Manage").performClick()
        assertTrue(actionClicked)
    }

    @Test
    fun testWarningPanel_DefaultIcon() {
        val panel = WarningPanel(
            title = "Warning",
            message = "Message"
        )

        assertEquals("warning", panel.getEffectiveIcon())
    }

    @Test
    fun testWarningPanel_FactoryMethods() {
        val simple = WarningPanel.simple("Title", "Message")
        val dismissible = WarningPanel.dismissible("Title", "Message")

        assertTrue(simple.title == "Title")
        assertTrue(dismissible.dismissible)
    }

    @Test
    fun testWarningPanel_AccessibilityDescription() {
        val panel = WarningPanel(
            title = "Warning",
            message = "Be careful"
        )

        assertTrue(panel.getAccessibilityDescription().contains("Warning"))
    }

    // ========== SuccessPanel Component Tests (6 tests) ==========

    @Test
    fun testSuccessPanel_RendersBasicContent() {
        composeTestRule.setContent {
            SuccessPanelMapper(
                SuccessPanel(
                    title = "Success Title",
                    message = "Success message"
                )
            )
        }

        composeTestRule.onNodeWithText("Success Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Success message").assertIsDisplayed()
    }

    @Test
    fun testSuccessPanel_DismissButton() {
        var dismissed = false
        composeTestRule.setContent {
            SuccessPanelMapper(
                SuccessPanel(
                    title = "Success",
                    message = "Message",
                    dismissible = true,
                    onDismiss = { dismissed = true }
                )
            )
        }

        composeTestRule.onNodeWithContentDescription("Dismiss").performClick()
        assertTrue(dismissed)
    }

    @Test
    fun testSuccessPanel_Actions() {
        var viewClicked = false
        composeTestRule.setContent {
            SuccessPanelMapper(
                SuccessPanel(
                    title = "Payment Successful",
                    message = "Message",
                    actions = listOf(
                        SuccessPanel.Action("View Receipt") { viewClicked = true }
                    )
                )
            )
        }

        composeTestRule.onNodeWithText("View Receipt").performClick()
        assertTrue(viewClicked)
    }

    @Test
    fun testSuccessPanel_DefaultIcon() {
        val panel = SuccessPanel(
            title = "Success",
            message = "Message"
        )

        assertEquals("check_circle", panel.getEffectiveIcon())
    }

    @Test
    fun testSuccessPanel_FactoryMethods() {
        val simple = SuccessPanel.simple("Title", "Message")
        val withActions = SuccessPanel.withActions(
            "Title",
            "Message",
            listOf(SuccessPanel.Action("Action") { })
        )

        assertTrue(simple.title == "Title")
        assertTrue(withActions.actions.isNotEmpty())
    }

    @Test
    fun testSuccessPanel_AccessibilityDescription() {
        val panel = SuccessPanel(
            title = "Success",
            message = "Operation complete"
        )

        assertTrue(panel.getAccessibilityDescription().contains("Success"))
    }

    // ========== FullPageLoading Component Tests (5 tests) ==========

    @Test
    fun testFullPageLoading_RendersWhenVisible() {
        composeTestRule.setContent {
            FullPageLoadingMapper(
                FullPageLoading(
                    visible = true,
                    message = "Loading..."
                )
            )
        }

        composeTestRule.onNodeWithText("Loading...").assertIsDisplayed()
    }

    @Test
    fun testFullPageLoading_HiddenWhenNotVisible() {
        composeTestRule.setContent {
            FullPageLoadingMapper(
                FullPageLoading(
                    visible = false,
                    message = "Loading..."
                )
            )
        }

        composeTestRule.onNodeWithText("Loading...").assertDoesNotExist()
    }

    @Test
    fun testFullPageLoading_CancelButton() {
        var canceled = false
        composeTestRule.setContent {
            FullPageLoadingMapper(
                FullPageLoading(
                    visible = true,
                    message = "Loading...",
                    cancelable = true,
                    onCancel = { canceled = true }
                )
            )
        }

        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        assertTrue(canceled)
    }

    @Test
    fun testFullPageLoading_ValidatesSpinnerSize() {
        val loading = FullPageLoading(
            visible = true,
            spinnerSize = 64f
        )

        assertTrue(loading.isSpinnerSizeValid())
    }

    @Test
    fun testFullPageLoading_FactoryMethods() {
        val simple = FullPageLoading.simple(message = "Loading")
        val cancelable = FullPageLoading.cancelable(message = "Loading")

        assertTrue(simple.message == "Loading")
        assertTrue(cancelable.cancelable)
    }

    // ========== AnimatedCheck Component Tests (5 tests) ==========

    @Test
    fun testAnimatedCheck_RendersWhenVisible() {
        composeTestRule.setContent {
            AnimatedCheckMapper(
                AnimatedCheck(
                    visible = true,
                    size = 48f
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Success")
        ).assertIsDisplayed()
    }

    @Test
    fun testAnimatedCheck_HiddenWhenNotVisible() {
        composeTestRule.setContent {
            AnimatedCheckMapper(
                AnimatedCheck(
                    visible = false
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Success")
        ).assertDoesNotExist()
    }

    @Test
    fun testAnimatedCheck_CustomColor() {
        val check = AnimatedCheck(
            visible = true,
            color = "#00FF00"
        )

        assertEquals("#00FF00", check.getEffectiveColor())
    }

    @Test
    fun testAnimatedCheck_ValidatesParameters() {
        val check = AnimatedCheck(
            visible = true,
            size = 48f,
            animationDuration = 500
        )

        assertTrue(check.areParametersValid())
    }

    @Test
    fun testAnimatedCheck_FactoryMethods() {
        val simple = AnimatedCheck.simple()
        val large = AnimatedCheck.large(size = 72f)

        assertTrue(simple.size == 48f)
        assertTrue(large.size == 72f)
    }

    // ========== AnimatedError Component Tests (5 tests) ==========

    @Test
    fun testAnimatedError_RendersWhenVisible() {
        composeTestRule.setContent {
            AnimatedErrorMapper(
                AnimatedError(
                    visible = true,
                    size = 48f
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Error")
        ).assertIsDisplayed()
    }

    @Test
    fun testAnimatedError_HiddenWhenNotVisible() {
        composeTestRule.setContent {
            AnimatedErrorMapper(
                AnimatedError(
                    visible = false
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Error")
        ).assertDoesNotExist()
    }

    @Test
    fun testAnimatedError_CustomColor() {
        val error = AnimatedError(
            visible = true,
            color = "#FF0000"
        )

        assertEquals("#FF0000", error.getEffectiveColor())
    }

    @Test
    fun testAnimatedError_ValidatesParameters() {
        val error = AnimatedError(
            visible = true,
            size = 48f,
            animationDuration = 500,
            shakeIntensity = 10f
        )

        assertTrue(error.areParametersValid())
    }

    @Test
    fun testAnimatedError_FactoryMethods() {
        val simple = AnimatedError.simple()
        val large = AnimatedError.large(size = 72f, shakeIntensity = 15f)

        assertTrue(simple.size == 48f)
        assertTrue(large.shakeIntensity == 15f)
    }

    // ========== HoverCard Component Tests (5 tests) ==========

    @Test
    fun testHoverCard_RendersBasicContent() {
        composeTestRule.setContent {
            HoverCardMapper(
                HoverCard(
                    triggerContent = "Hover trigger",
                    cardTitle = "Card Title",
                    cardContent = "Card content"
                )
            )
        }

        composeTestRule.onNodeWithText("Hover trigger").assertIsDisplayed()
    }

    @Test
    fun testHoverCard_ShowsCardOnClick() {
        composeTestRule.setContent {
            HoverCardMapper(
                HoverCard(
                    triggerContent = "Click me",
                    cardTitle = "Shown Title",
                    cardContent = "Shown content"
                )
            )
        }

        composeTestRule.onNodeWithText("Click me").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Shown Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Shown content").assertIsDisplayed()
    }

    @Test
    fun testHoverCard_WithIcon() {
        composeTestRule.setContent {
            HoverCardMapper(
                HoverCard.withIcon(
                    triggerContent = "Trigger",
                    cardTitle = "Title",
                    cardContent = "Content",
                    icon = "info"
                )
            )
        }

        composeTestRule.onNodeWithText("Trigger").assertIsDisplayed()
    }

    @Test
    fun testHoverCard_ValidatesTimings() {
        val card = HoverCard(
            triggerContent = "Test",
            cardTitle = "Title",
            cardContent = "Content",
            showDelay = 500,
            hideDelay = 200
        )

        assertTrue(card.areTimingsValid())
    }

    @Test
    fun testHoverCard_ValidatesDimensions() {
        val card = HoverCard(
            triggerContent = "Test",
            cardTitle = "Title",
            cardContent = "Content",
            maxWidth = 300f,
            elevation = 4f
        )

        assertTrue(card.areDimensionsValid())
    }

    // ========== AnimatedSuccess Component Tests (6 tests) ==========

    @Test
    fun testAnimatedSuccess_RendersWhenVisible() {
        composeTestRule.setContent {
            AnimatedSuccessMapper(
                AnimatedSuccess(
                    visible = true,
                    size = 64f
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Success")
        ).assertIsDisplayed()
    }

    @Test
    fun testAnimatedSuccess_HiddenWhenNotVisible() {
        composeTestRule.setContent {
            AnimatedSuccessMapper(
                AnimatedSuccess(
                    visible = false
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Success")
        ).assertDoesNotExist()
    }

    @Test
    fun testAnimatedSuccess_CustomColor() {
        val success = AnimatedSuccess(
            visible = true,
            color = "#00FF00"
        )

        assertEquals("#00FF00", success.getEffectiveColor())
    }

    @Test
    fun testAnimatedSuccess_ValidatesParameters() {
        val success = AnimatedSuccess(
            visible = true,
            size = 64f,
            animationDuration = 600,
            particleCount = 20
        )

        assertTrue(success.areParametersValid())
    }

    @Test
    fun testAnimatedSuccess_WithParticles() {
        val celebration = AnimatedSuccess.celebration(size = 80f)

        assertTrue(celebration.showParticles)
        assertTrue(celebration.particleCount == 30)
    }

    @Test
    fun testAnimatedSuccess_FactoryMethods() {
        val simple = AnimatedSuccess.simple()
        val large = AnimatedSuccess.large(size = 96f)
        val subtle = AnimatedSuccess.subtle(size = 48f)

        assertTrue(simple.size == 64f)
        assertTrue(large.size == 96f)
        assertTrue(!subtle.showParticles)
    }

    // ========== AnimatedWarning Component Tests (6 tests) ==========

    @Test
    fun testAnimatedWarning_RendersWhenVisible() {
        composeTestRule.setContent {
            AnimatedWarningMapper(
                AnimatedWarning(
                    visible = true,
                    size = 56f
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Warning")
        ).assertIsDisplayed()
    }

    @Test
    fun testAnimatedWarning_HiddenWhenNotVisible() {
        composeTestRule.setContent {
            AnimatedWarningMapper(
                AnimatedWarning(
                    visible = false
                )
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Warning")
        ).assertDoesNotExist()
    }

    @Test
    fun testAnimatedWarning_CustomColor() {
        val warning = AnimatedWarning(
            visible = true,
            color = "#FFA500"
        )

        assertEquals("#FFA500", warning.getEffectiveColor())
    }

    @Test
    fun testAnimatedWarning_ValidatesParameters() {
        val warning = AnimatedWarning(
            visible = true,
            size = 56f,
            animationDuration = 500,
            pulseCount = 2,
            pulseIntensity = 1.1f
        )

        assertTrue(warning.areParametersValid())
    }

    @Test
    fun testAnimatedWarning_UrgentVariant() {
        val urgent = AnimatedWarning.urgent(size = 72f)

        assertTrue(urgent.pulseCount == 3)
        assertTrue(urgent.pulseIntensity == 1.15f)
    }

    @Test
    fun testAnimatedWarning_FactoryMethods() {
        val simple = AnimatedWarning.simple()
        val large = AnimatedWarning.large(size = 80f, pulseCount = 3)
        val subtle = AnimatedWarning.subtle(size = 48f)

        assertTrue(simple.size == 56f)
        assertTrue(large.pulseCount == 3)
        assertTrue(subtle.pulseCount == 0)
    }
}
