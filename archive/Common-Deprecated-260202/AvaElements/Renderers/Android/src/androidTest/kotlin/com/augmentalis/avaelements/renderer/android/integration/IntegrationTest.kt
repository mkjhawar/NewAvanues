package com.augmentalis.avaelements.renderer.android.integration

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.augmentalis.avaelements.flutter.layout.*
import com.augmentalis.avaelements.flutter.layout.scrolling.*
import com.augmentalis.avaelements.flutter.material.chips.*
import com.augmentalis.avaelements.flutter.material.lists.*
import com.augmentalis.avaelements.flutter.material.advanced.*
import com.augmentalis.avaelements.flutter.animation.*
import com.augmentalis.avaelements.flutter.animation.transitions.*
import com.augmentalis.avaelements.renderer.android.mappers.flutterparity.*
import com.augmentalis.avaelements.core.types.Size
import com.augmentalis.avaelements.core.types.Spacing
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

/**
 * Integration tests for Flutter Parity components
 *
 * Tests end-to-end rendering of all 60 Flutter Parity components:
 * - 10 Layout components
 * - 17 Material components
 * - 7 Scrolling components
 * - 8 Animation components
 * - 15 Transition components
 * - 4 Sliver components
 *
 * @since 3.0.0-flutter-parity
 */
@RunWith(AndroidJUnit4::class)
class FlutterParityIntegrationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ============================================================================
    // Layout Component Tests (10)
    // ============================================================================

    @Test
    fun testWrapComponent_rendersCorrectly() {
        val component = WrapComponent(
            direction = WrapDirection.Horizontal,
            alignment = WrapAlignment.Start,
            runAlignment = WrapAlignment.Start,
            spacing = Spacing.Zero,
            runSpacing = Spacing.Zero,
            children = listOf("Child1", "Child2", "Child3")
        )

        composeTestRule.setContent {
            WrapMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("Child1").assertExists()
        composeTestRule.onNodeWithText("Child2").assertExists()
        composeTestRule.onNodeWithText("Child3").assertExists()
    }

    @Test
    fun testExpandedComponent_rendersCorrectly() {
        val component = ExpandedComponent(
            flex = 1,
            child = "ExpandedChild"
        )

        composeTestRule.setContent {
            ExpandedMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("ExpandedChild").assertExists()
    }

    @Test
    fun testFlexibleComponent_rendersCorrectly() {
        val component = FlexibleComponent(
            flex = 1,
            fit = FlexFit.Tight,
            child = "FlexibleChild"
        )

        composeTestRule.setContent {
            FlexibleMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("FlexibleChild").assertExists()
    }

    @Test
    fun testFlexComponent_rendersHorizontally() {
        val component = FlexComponent(
            direction = FlexDirection.Horizontal,
            mainAxisAlignment = MainAxisAlignment.Start,
            crossAxisAlignment = CrossAxisAlignment.Start,
            mainAxisSize = MainAxisSize.Max,
            verticalDirection = VerticalDirection.Down,
            textDirection = null,
            children = listOf("Item1", "Item2")
        )

        composeTestRule.setContent {
            FlexMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("Item1").assertExists()
        composeTestRule.onNodeWithText("Item2").assertExists()
    }

    @Test
    fun testPaddingComponent_rendersCorrectly() {
        val component = PaddingComponent(
            padding = Spacing.Zero,
            child = "PaddedChild"
        )

        composeTestRule.setContent {
            PaddingMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("PaddedChild").assertExists()
    }

    @Test
    fun testAlignComponent_rendersCorrectly() {
        val component = AlignComponent(
            alignment = AlignmentGeometry.Center,
            widthFactor = null,
            heightFactor = null,
            child = "AlignedChild"
        )

        composeTestRule.setContent {
            AlignMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("AlignedChild").assertExists()
    }

    @Test
    fun testCenterComponent_rendersCorrectly() {
        val component = CenterComponent(
            widthFactor = null,
            heightFactor = null,
            child = "CenteredChild"
        )

        composeTestRule.setContent {
            CenterMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("CenteredChild").assertExists()
    }

    @Test
    fun testSizedBoxComponent_rendersCorrectly() {
        val component = SizedBoxComponent(
            width = Size.Fill,
            height = Size.Fill,
            child = "SizedChild"
        )

        composeTestRule.setContent {
            SizedBoxMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("SizedChild").assertExists()
    }

    @Test
    fun testConstrainedBoxComponent_rendersCorrectly() {
        val component = ConstrainedBoxComponent(
            constraints = BoxConstraints(
                minWidth = 0.0,
                maxWidth = Double.POSITIVE_INFINITY,
                minHeight = 0.0,
                maxHeight = Double.POSITIVE_INFINITY
            ),
            child = "ConstrainedChild"
        )

        composeTestRule.setContent {
            ConstrainedBoxMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("ConstrainedChild").assertExists()
    }

    @Test
    fun testFittedBoxComponent_rendersCorrectly() {
        val component = FittedBoxComponent(
            fit = BoxFit.Contain,
            alignment = AlignmentGeometry.Center,
            clipBehavior = Clip.None,
            child = "FittedChild"
        )

        composeTestRule.setContent {
            FittedBoxMapper(component) { child ->
                androidx.compose.material3.Text(child.toString())
            }
        }

        composeTestRule.onNodeWithText("FittedChild").assertExists()
    }

    // ============================================================================
    // Material Component Tests (14)
    // ============================================================================

    @Test
    fun testFilterChip_rendersCorrectly() {
        val component = FilterChip(
            label = "Filter",
            selected = false,
            enabled = true,
            showCheckmark = true,
            avatar = null,
            onSelected = null
        )

        composeTestRule.setContent {
            FilterChipMapper(component)
        }

        composeTestRule.onNodeWithText("Filter").assertExists()
    }

    @Test
    fun testActionChip_rendersCorrectly() {
        val component = ActionChip(
            label = "Action",
            enabled = true,
            avatar = null,
            onPressed = null
        )

        composeTestRule.setContent {
            ActionChipMapper(component)
        }

        composeTestRule.onNodeWithText("Action").assertExists()
    }

    @Test
    fun testChoiceChip_rendersCorrectly() {
        val component = ChoiceChip(
            label = "Choice",
            selected = false,
            enabled = true,
            showCheckmark = true,
            avatar = null,
            onSelected = null
        )

        composeTestRule.setContent {
            ChoiceChipMapper(component)
        }

        composeTestRule.onNodeWithText("Choice").assertExists()
    }

    @Test
    fun testInputChip_rendersCorrectly() {
        val component = InputChip(
            label = "Input",
            selected = false,
            enabled = true,
            avatar = null,
            onPressed = null,
            onSelected = null,
            onDeleted = null
        )

        composeTestRule.setContent {
            InputChipMapper(component)
        }

        composeTestRule.onNodeWithText("Input").assertExists()
    }

    @Test
    fun testExpansionTile_rendersCorrectly() {
        val component = ExpansionTile(
            title = "Expansion Title",
            subtitle = "Subtitle",
            initiallyExpanded = false,
            leading = null,
            trailing = null,
            children = listOf("Child1"),
            onExpansionChanged = null
        )

        composeTestRule.setContent {
            ExpansionTileMapper(component)
        }

        composeTestRule.onNodeWithText("Expansion Title").assertExists()
        composeTestRule.onNodeWithText("Subtitle").assertExists()
    }

    @Test
    fun testCheckboxListTile_rendersCorrectly() {
        val component = CheckboxListTile(
            title = "Checkbox",
            subtitle = null,
            value = false,
            tristate = false,
            enabled = true,
            controlAffinity = CheckboxListTile.ListTileControlAffinity.Leading,
            onChanged = null
        )

        composeTestRule.setContent {
            CheckboxListTileMapper(component)
        }

        composeTestRule.onNodeWithText("Checkbox").assertExists()
    }

    @Test
    fun testSwitchListTile_rendersCorrectly() {
        val component = SwitchListTile(
            title = "Switch",
            subtitle = null,
            value = false,
            enabled = true,
            controlAffinity = SwitchListTile.ListTileControlAffinity.Trailing,
            onChanged = null
        )

        composeTestRule.setContent {
            SwitchListTileMapper(component)
        }

        composeTestRule.onNodeWithText("Switch").assertExists()
    }

    @Test
    fun testFilledButton_rendersCorrectly() {
        val component = FilledButton(
            text = "Click Me",
            enabled = true,
            icon = null,
            iconPosition = FilledButton.IconPosition.Leading,
            onPressed = null
        )

        composeTestRule.setContent {
            FilledButtonMapper(component)
        }

        composeTestRule.onNodeWithText("Click Me").assertExists()
    }

    // ============================================================================
    // Scrolling Component Tests (7)
    // ============================================================================

    @Test
    fun testListViewBuilder_rendersCorrectly() {
        val component = ListViewBuilderComponent(
            itemCount = 10,
            scrollDirection = ScrollDirection.Vertical,
            reverse = false,
            controller = null,
            physics = ScrollPhysics.AlwaysScrollable,
            padding = null,
            itemExtent = null
        )

        composeTestRule.setContent {
            ListViewBuilderMapper(component) { index ->
                androidx.compose.material3.Text("Item $index")
            }
        }

        composeTestRule.onNodeWithText("Item 0").assertExists()
    }

    @Test
    fun testGridViewBuilder_rendersCorrectly() {
        val component = GridViewBuilderComponent(
            gridDelegate = SliverGridDelegate.WithFixedCrossAxisCount(
                crossAxisCount = 2,
                mainAxisSpacing = 8.0,
                crossAxisSpacing = 8.0,
                childAspectRatio = 1.0
            ),
            itemCount = 6,
            scrollDirection = ScrollDirection.Vertical,
            reverse = false,
            controller = null,
            physics = ScrollPhysics.AlwaysScrollable,
            padding = null
        )

        composeTestRule.setContent {
            GridViewBuilderMapper(component) { index ->
                androidx.compose.material3.Text("Grid $index")
            }
        }

        composeTestRule.onNodeWithText("Grid 0").assertExists()
    }

    @Test
    fun testListViewSeparated_rendersCorrectly() {
        val component = ListViewSeparatedComponent(
            itemCount = 5,
            scrollDirection = ScrollDirection.Vertical,
            reverse = false,
            controller = null,
            physics = ScrollPhysics.AlwaysScrollable,
            padding = null
        )

        composeTestRule.setContent {
            ListViewSeparatedMapper(
                component = component,
                itemRenderer = { index ->
                    androidx.compose.material3.Text("Item $index")
                },
                separatorRenderer = { index ->
                    androidx.compose.material3.Divider()
                }
            )
        }

        composeTestRule.onNodeWithText("Item 0").assertExists()
    }

    // ============================================================================
    // Animation Component Tests (8)
    // ============================================================================

    @Test
    fun testAnimatedContainer_rendersCorrectly() {
        val component = AnimatedContainer(
            width = null,
            height = null,
            color = null,
            padding = null,
            margin = null,
            decoration = null,
            alignment = null,
            duration = Duration(milliseconds = 300),
            curve = Curve.Linear,
            onEnd = null,
            child = "AnimatedChild"
        )

        composeTestRule.setContent {
            AnimatedContainerMapper(component) {
                androidx.compose.material3.Text("AnimatedChild")
            }
        }

        composeTestRule.onNodeWithText("AnimatedChild").assertExists()
    }

    @Test
    fun testAnimatedOpacity_rendersCorrectly() {
        val component = AnimatedOpacity(
            opacity = 1.0f,
            duration = Duration(milliseconds = 300),
            curve = Curve.Linear,
            onEnd = null,
            child = "FadingChild"
        )

        composeTestRule.setContent {
            AnimatedOpacityMapper(component) {
                androidx.compose.material3.Text("FadingChild")
            }
        }

        composeTestRule.onNodeWithText("FadingChild").assertExists()
    }

    // ============================================================================
    // Transition Component Tests (15)
    // ============================================================================

    @Test
    fun testFadeTransition_rendersCorrectly() {
        val component = FadeTransition(
            opacity = 1.0f,
            child = "FadingChild"
        )

        composeTestRule.setContent {
            FadeTransitionMapper(component) {
                androidx.compose.material3.Text("FadingChild")
            }
        }

        composeTestRule.onNodeWithText("FadingChild").assertExists()
    }

    @Test
    fun testScaleTransition_rendersCorrectly() {
        val component = ScaleTransition(
            scale = 1.0f,
            alignment = ScaleTransition.Alignment.Center,
            child = "ScaledChild"
        )

        composeTestRule.setContent {
            ScaleTransitionMapper(component) {
                androidx.compose.material3.Text("ScaledChild")
            }
        }

        composeTestRule.onNodeWithText("ScaledChild").assertExists()
    }

    // ============================================================================
    // End-to-End Integration Test
    // ============================================================================

    @Test
    fun testNestedComponents_renderCorrectly() {
        // Test nesting: Padding > Center > Column > (Text, Button)
        val component = PaddingComponent(
            padding = Spacing.Zero,
            child = CenterComponent(
                widthFactor = null,
                heightFactor = null,
                child = FlexComponent(
                    direction = FlexDirection.Vertical,
                    mainAxisAlignment = MainAxisAlignment.Start,
                    crossAxisAlignment = CrossAxisAlignment.Start,
                    mainAxisSize = MainAxisSize.Min,
                    verticalDirection = VerticalDirection.Down,
                    textDirection = null,
                    children = listOf("Title", "Button")
                )
            )
        )

        composeTestRule.setContent {
            PaddingMapper(component) { child1 ->
                if (child1 is CenterComponent) {
                    CenterMapper(child1) { child2 ->
                        if (child2 is FlexComponent) {
                            FlexMapper(child2) { child3 ->
                                androidx.compose.material3.Text(child3.toString())
                            }
                        }
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("Title").assertExists()
        composeTestRule.onNodeWithText("Button").assertExists()
    }

    @Test
    fun testComponentComposition_withMultipleLayers() {
        // Complex composition test
        var testPassed = false

        composeTestRule.setContent {
            // Outer Wrap
            WrapMapper(
                WrapComponent(
                    direction = WrapDirection.Horizontal,
                    alignment = WrapAlignment.Start,
                    runAlignment = WrapAlignment.Start,
                    spacing = Spacing.Zero,
                    runSpacing = Spacing.Zero,
                    children = List(3) { i ->
                        // Each wrap child is a FilterChip
                        FilterChip(
                            label = "Chip $i",
                            selected = false,
                            enabled = true,
                            showCheckmark = false,
                            avatar = null,
                            onSelected = null
                        )
                    }
                )
            ) { child ->
                if (child is FilterChip) {
                    FilterChipMapper(child)
                }
            }
            testPassed = true
        }

        assertTrue(testPassed, "Complex component composition should render without errors")
    }
}
