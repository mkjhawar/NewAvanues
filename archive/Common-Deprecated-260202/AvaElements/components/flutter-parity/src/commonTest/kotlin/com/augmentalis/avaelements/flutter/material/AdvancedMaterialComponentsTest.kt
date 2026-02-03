package com.augmentalis.avaelements.flutter.material

import com.augmentalis.avaelements.flutter.material.advanced.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive unit tests for advanced Material Design components
 *
 * Tests cover:
 * - Component creation and defaults
 * - State management
 * - Accessibility features
 * - Helper methods
 * - Factory methods
 * - Dark mode compatibility
 * - Edge cases
 *
 * @since 3.0.0-flutter-parity
 */
class AdvancedMaterialComponentsTest {

    // ============================================================================
    // IndexedStack Tests (5 tests)
    // ============================================================================

    @Test
    fun testIndexedStackDefaultValues() {
        val stack = IndexedStack(
            children = listOf()
        )

        assertEquals(0, stack.index)
        assertEquals(IndexedStack.Alignment.TopStart, stack.alignment)
        assertEquals(IndexedStack.StackFit.Loose, stack.sizing)
        assertEquals(IndexedStack.TextDirection.LTR, stack.textDirection)
        assertEquals("IndexedStack", stack.type)
    }

    @Test
    fun testIndexedStackGetCurrentChild() {
        val child1 = createMockComponent("child1")
        val child2 = createMockComponent("child2")
        val child3 = createMockComponent("child3")

        val stack = IndexedStack(
            index = 1,
            children = listOf(child1, child2, child3)
        )

        assertEquals(child2, stack.getCurrentChild())
    }

    @Test
    fun testIndexedStackValidation() {
        val stack1 = IndexedStack(
            index = 0,
            children = listOf(createMockComponent())
        )
        assertTrue(stack1.isIndexValid())

        val stack2 = IndexedStack(
            index = 5,
            children = listOf(createMockComponent())
        )
        assertFalse(stack2.isIndexValid())

        val stack3 = IndexedStack(
            index = -1,
            children = listOf(createMockComponent())
        )
        assertFalse(stack3.isIndexValid())
    }

    @Test
    fun testIndexedStackAccessibilityDescription() {
        val stack = IndexedStack(
            index = 1,
            children = listOf(
                createMockComponent(),
                createMockComponent(),
                createMockComponent()
            ),
            contentDescription = "Navigation stack"
        )

        assertEquals("Navigation stack", stack.getAccessibilityDescription())

        val stackDefault = IndexedStack(
            index = 0,
            children = listOf(createMockComponent())
        )
        assertEquals("Screen 1 of 1", stackDefault.getAccessibilityDescription())
    }

    @Test
    fun testIndexedStackFactoryMethods() {
        val children = listOf(createMockComponent(), createMockComponent())

        val simple = IndexedStack.simple(1, children)
        assertEquals(1, simple.index)
        assertEquals(2, simple.children.size)

        val withAlignment = IndexedStack.withAlignment(0, children, IndexedStack.Alignment.Center)
        assertEquals(IndexedStack.Alignment.Center, withAlignment.alignment)

        val expanded = IndexedStack.expanded(0, children)
        assertEquals(IndexedStack.StackFit.Expand, expanded.sizing)

        val rtl = IndexedStack.withRTL(0, children)
        assertEquals(IndexedStack.TextDirection.RTL, rtl.textDirection)
    }

    // ============================================================================
    // VerticalDivider Tests (5 tests)
    // ============================================================================

    @Test
    fun testVerticalDividerDefaultValues() {
        val divider = VerticalDivider()

        assertEquals(1f, divider.thickness)
        assertEquals(0f, divider.indent)
        assertEquals(0f, divider.endIndent)
        assertNull(divider.width)
        assertNull(divider.color)
        assertEquals("VerticalDivider", divider.type)
    }

    @Test
    fun testVerticalDividerEffectiveWidth() {
        val divider1 = VerticalDivider(width = 20f)
        assertEquals(20f, divider1.getEffectiveWidth())

        val divider2 = VerticalDivider(thickness = 2f)
        assertEquals(18f, divider2.getEffectiveWidth()) // 2 + 16 (default padding)
    }

    @Test
    fun testVerticalDividerVisibility() {
        val visible = VerticalDivider(thickness = 1f)
        assertTrue(visible.isVisible())

        val invisible = VerticalDivider(thickness = 0f)
        assertFalse(invisible.isVisible())

        val thin = VerticalDivider(thickness = 0.5f)
        assertTrue(thin.isVisible())
    }

    @Test
    fun testVerticalDividerAccessibility() {
        val divider1 = VerticalDivider(contentDescription = "Section divider")
        assertEquals("Section divider", divider1.getAccessibilityDescription())

        val divider2 = VerticalDivider(semanticsLabel = "Separator")
        assertEquals("Separator", divider2.getAccessibilityDescription())

        val divider3 = VerticalDivider()
        assertNull(divider3.getAccessibilityDescription())
    }

    @Test
    fun testVerticalDividerFactoryMethods() {
        val simple = VerticalDivider.simple()
        assertEquals(1f, simple.thickness)

        val thick = VerticalDivider.thick()
        assertEquals(2f, thick.thickness)

        val thin = VerticalDivider.thin()
        assertEquals(0.5f, thin.thickness)

        val withColor = VerticalDivider.withColor("primary")
        assertEquals("primary", withColor.color)

        val withIndents = VerticalDivider.withIndents(8f, 16f)
        assertEquals(8f, withIndents.indent)
        assertEquals(16f, withIndents.endIndent)

        val custom = VerticalDivider.custom(24f, 2f, "outline")
        assertEquals(24f, custom.width)
        assertEquals(2f, custom.thickness)
        assertEquals("outline", custom.color)
    }

    // ============================================================================
    // FadeInImage Tests (5 tests)
    // ============================================================================

    @Test
    fun testFadeInImageDefaultValues() {
        val image = FadeInImage(
            placeholder = "placeholder.png",
            image = "image.jpg"
        )

        assertEquals(300, image.fadeInDuration)
        assertEquals(300, image.fadeOutDuration)
        assertEquals(FadeInImage.BoxFit.Contain, image.fit)
        assertEquals(FadeInImage.Alignment.Center, image.alignment)
        assertEquals(FadeInImage.ImageRepeat.NoRepeat, image.repeat)
        assertFalse(image.matchTextDirection)
        assertFalse(image.excludeFromSemantics)
        assertEquals("FadeInImage", image.type)
    }

    @Test
    fun testFadeInImageNetworkDetection() {
        val network1 = FadeInImage(
            placeholder = "placeholder.png",
            image = "https://example.com/image.jpg"
        )
        assertTrue(network1.isNetworkImage())

        val network2 = FadeInImage(
            placeholder = "placeholder.png",
            image = "http://example.com/image.jpg"
        )
        assertTrue(network2.isNetworkImage())

        val local = FadeInImage(
            placeholder = "placeholder.png",
            image = "assets/image.jpg"
        )
        assertFalse(local.isNetworkImage())
    }

    @Test
    fun testFadeInImageAccessibility() {
        val image1 = FadeInImage(
            placeholder = "placeholder.png",
            image = "image.jpg",
            contentDescription = "User profile photo"
        )
        assertEquals("User profile photo", image1.getAccessibilityDescription())

        val image2 = FadeInImage(
            placeholder = "placeholder.png",
            image = "image.jpg",
            semanticsLabel = "Avatar"
        )
        assertEquals("Avatar", image2.getAccessibilityDescription())

        val image3 = FadeInImage(
            placeholder = "placeholder.png",
            image = "image.jpg",
            excludeFromSemantics = true
        )
        assertNull(image3.getAccessibilityDescription())
    }

    @Test
    fun testFadeInImageFactoryMethods() {
        val simple = FadeInImage.simple("placeholder.png", "image.jpg")
        assertEquals("placeholder.png", simple.placeholder)
        assertEquals("image.jpg", simple.image)

        val network = FadeInImage.network("placeholder.png", "https://example.com/photo.jpg")
        assertTrue(network.isNetworkImage())

        val withDuration = FadeInImage.withDuration("placeholder.png", "image.jpg", 500)
        assertEquals(500, withDuration.fadeInDuration)

        val cover = FadeInImage.cover("placeholder.png", "image.jpg")
        assertEquals(FadeInImage.BoxFit.Cover, cover.fit)

        val sized = FadeInImage.sized("placeholder.png", "image.jpg", 200f, 200f)
        assertEquals(200f, sized.width)
        assertEquals(200f, sized.height)
    }

    @Test
    fun testFadeInImageCallbacks() {
        var loadCompleted = false
        var errorOccurred = false
        var errorMessage = ""

        val image = FadeInImage(
            placeholder = "placeholder.png",
            image = "image.jpg",
            onLoadComplete = { loadCompleted = true },
            onError = { msg ->
                errorOccurred = true
                errorMessage = msg
            }
        )

        assertNotNull(image.onLoadComplete)
        assertNotNull(image.onError)

        image.onLoadComplete?.invoke()
        assertTrue(loadCompleted)

        image.onError?.invoke("Network error")
        assertTrue(errorOccurred)
        assertEquals("Network error", errorMessage)
    }

    // ============================================================================
    // CircleAvatar Tests (5 tests)
    // ============================================================================

    @Test
    fun testCircleAvatarDefaultValues() {
        val avatar = CircleAvatar()

        assertNull(avatar.child)
        assertNull(avatar.backgroundImage)
        assertNull(avatar.backgroundColor)
        assertNull(avatar.foregroundColor)
        assertNull(avatar.radius)
        assertEquals(20f, avatar.getEffectiveRadius()) // Uses default
        assertEquals("CircleAvatar", avatar.type)
    }

    @Test
    fun testCircleAvatarRadiusConstraints() {
        val avatar1 = CircleAvatar(
            radius = 30f,
            minRadius = 20f,
            maxRadius = 40f
        )
        assertEquals(30f, avatar1.getEffectiveRadius())

        val avatar2 = CircleAvatar(
            radius = 10f,
            minRadius = 20f
        )
        assertEquals(20f, avatar2.getEffectiveRadius())

        val avatar3 = CircleAvatar(
            radius = 50f,
            maxRadius = 40f
        )
        assertEquals(40f, avatar3.getEffectiveRadius())
    }

    @Test
    fun testCircleAvatarDiameter() {
        val avatar = CircleAvatar(radius = 25f)
        assertEquals(50f, avatar.getDiameter())
    }

    @Test
    fun testCircleAvatarContentDetection() {
        val withImage = CircleAvatar(backgroundImage = "avatar.jpg")
        assertTrue(withImage.hasImage())
        assertFalse(withImage.hasChild())

        val withChild = CircleAvatar(child = createMockComponent())
        assertFalse(withChild.hasImage())
        assertTrue(withChild.hasChild())

        val empty = CircleAvatar()
        assertFalse(empty.hasImage())
        assertFalse(empty.hasChild())
    }

    @Test
    fun testCircleAvatarFactoryMethods() {
        val fromImage = CircleAvatar.fromImage("avatar.jpg", 30f, "User avatar")
        assertEquals("avatar.jpg", fromImage.backgroundImage)
        assertEquals(30f, fromImage.radius)
        assertEquals("User avatar", fromImage.contentDescription)

        val small = CircleAvatar.small()
        assertEquals(16f, small.radius)

        val large = CircleAvatar.large()
        assertEquals(32f, large.radius)
    }

    // ============================================================================
    // RichText Tests (5 tests)
    // ============================================================================

    @Test
    fun testRichTextDefaultValues() {
        val richText = RichText()

        assertTrue(richText.spans.isEmpty())
        assertEquals(RichText.TextAlign.Start, richText.textAlign)
        assertEquals(RichText.TextDirection.LTR, richText.textDirection)
        assertTrue(richText.softWrap)
        assertEquals(RichText.TextOverflow.Clip, richText.overflow)
        assertEquals(1.0f, richText.textScaleFactor)
        assertNull(richText.maxLines)
        assertEquals("RichText", richText.type)
    }

    @Test
    fun testRichTextPlainTextExtraction() {
        val richText = RichText(
            spans = listOf(
                TextSpan(text = "Hello "),
                TextSpan(text = "World", style = TextSpanStyle(fontWeight = "bold")),
                TextSpan(text = "!")
            )
        )

        assertEquals("Hello World!", richText.getPlainText())
    }

    @Test
    fun testRichTextTruncation() {
        val withMaxLines = RichText(
            maxLines = 2,
            overflow = RichText.TextOverflow.Ellipsis
        )
        assertTrue(withMaxLines.willTruncate())

        val noMaxLines = RichText(
            overflow = RichText.TextOverflow.Clip
        )
        assertFalse(noMaxLines.willTruncate())

        val clipOverflow = RichText(
            maxLines = 1,
            overflow = RichText.TextOverflow.Clip
        )
        assertFalse(clipOverflow.willTruncate())
    }

    @Test
    fun testRichTextAccessibility() {
        val richText1 = RichText(
            spans = listOf(TextSpan(text = "Test")),
            contentDescription = "Custom description"
        )
        assertEquals("Custom description", richText1.getAccessibilityDescription())

        val richText2 = RichText(
            spans = listOf(TextSpan(text = "Test")),
            semanticsLabel = "Semantic label"
        )
        assertEquals("Semantic label", richText2.getAccessibilityDescription())

        val richText3 = RichText(
            spans = listOf(TextSpan(text = "Fallback text"))
        )
        assertEquals("Fallback text", richText3.getAccessibilityDescription())
    }

    @Test
    fun testRichTextFactoryMethods() {
        val withBold = RichText.withBold("Normal ", "Bold")
        assertEquals(2, withBold.spans.size)
        assertEquals("bold", withBold.spans[1].style?.fontWeight)

        val withColor = RichText.withColor("Normal ", "Colored", "primary")
        assertEquals(2, withColor.spans.size)
        assertEquals("primary", withColor.spans[1].style?.color)

        val centered = RichText.centered(listOf(TextSpan(text = "Center")))
        assertEquals(RichText.TextAlign.Center, centered.textAlign)
    }

    // ============================================================================
    // SelectableText Tests (5 tests)
    // ============================================================================

    @Test
    fun testSelectableTextDefaultValues() {
        val text = SelectableText(text = "Hello World")

        assertEquals("Hello World", text.text)
        assertNull(text.style)
        assertEquals(SelectableText.TextAlign.Start, text.textAlign)
        assertEquals(SelectableText.TextDirection.LTR, text.textDirection)
        assertEquals(1.0f, text.textScaleFactor)
        assertFalse(text.showCursor)
        assertFalse(text.autofocus)
        assertTrue(text.enableInteractiveSelection)
        assertEquals(2.0f, text.cursorWidth)
        assertEquals(2.0f, text.cursorRadius)
        assertEquals("SelectableText", text.type)
    }

    @Test
    fun testSelectableTextMultilineDetection() {
        val multiline1 = SelectableText(text = "Test")
        assertTrue(multiline1.isMultiline())

        val multiline2 = SelectableText(text = "Test", maxLines = 5)
        assertTrue(multiline2.isMultiline())

        val singleLine = SelectableText(text = "Test", maxLines = 1)
        assertFalse(singleLine.isMultiline())
    }

    @Test
    fun testSelectableTextAccessibility() {
        val text1 = SelectableText(
            text = "Content",
            contentDescription = "Custom description"
        )
        assertEquals("Custom description", text1.getAccessibilityDescription())
        assertEquals("selectable text", text1.getAccessibilityRole())

        val text2 = SelectableText(
            text = "Content",
            semanticsLabel = "Semantic label"
        )
        assertEquals("Semantic label", text2.getAccessibilityDescription())

        val text3 = SelectableText(text = "Fallback")
        assertEquals("Fallback", text3.getAccessibilityDescription())
    }

    @Test
    fun testSelectableTextFactoryMethods() {
        val simple = SelectableText.simple("Simple text")
        assertEquals("Simple text", simple.text)

        val centered = SelectableText.centered("Centered text")
        assertEquals(SelectableText.TextAlign.Center, centered.textAlign)

        val multiline = SelectableText.multiline("Long text", 10)
        assertEquals(10, multiline.maxLines)

        val withAlignment = SelectableText.withAlignment(
            "Aligned",
            SelectableText.TextAlign.End
        )
        assertEquals(SelectableText.TextAlign.End, withAlignment.textAlign)
    }

    @Test
    fun testSelectableTextCallbacks() {
        var selectionChanged = false
        var selectionStart = -1
        var selectionEnd = -1
        var tapped = false

        val text = SelectableText(
            text = "Test",
            onSelectionChanged = { start, end ->
                selectionChanged = true
                selectionStart = start
                selectionEnd = end
            },
            onTap = { tapped = true }
        )

        assertNotNull(text.onSelectionChanged)
        assertNotNull(text.onTap)

        text.onSelectionChanged?.invoke(0, 4)
        assertTrue(selectionChanged)
        assertEquals(0, selectionStart)
        assertEquals(4, selectionEnd)

        text.onTap?.invoke()
        assertTrue(tapped)
    }

    // ============================================================================
    // EndDrawer Tests (5 tests)
    // ============================================================================

    @Test
    fun testEndDrawerDefaultValues() {
        val drawer = EndDrawer()

        assertNull(drawer.child)
        assertNull(drawer.backgroundColor)
        assertEquals(1f, drawer.elevation)
        assertEquals(280f, drawer.width)
        assertTrue(drawer.enableOpenDragGesture)
        assertEquals(20f, drawer.drawerEdgeDragWidth)
        assertEquals(EndDrawer.ClipBehavior.None, drawer.clipBehavior)
        assertEquals("EndDrawer", drawer.type)
    }

    @Test
    fun testEndDrawerEffectiveWidth() {
        val drawer1 = EndDrawer(width = 300f)
        assertEquals(300f, drawer1.getEffectiveWidth())

        val drawer2 = EndDrawer(width = 200f) // Below min
        assertEquals(240f, drawer2.getEffectiveWidth())

        val drawer3 = EndDrawer(width = 500f) // Above max
        assertEquals(400f, drawer3.getEffectiveWidth())
    }

    @Test
    fun testEndDrawerSwipeEnabled() {
        val enabled = EndDrawer(enableOpenDragGesture = true, drawerEdgeDragWidth = 20f)
        assertTrue(enabled.isSwipeEnabled())

        val disabled1 = EndDrawer(enableOpenDragGesture = false)
        assertFalse(disabled1.isSwipeEnabled())

        val disabled2 = EndDrawer(enableOpenDragGesture = true, drawerEdgeDragWidth = 0f)
        assertFalse(disabled2.isSwipeEnabled())
    }

    @Test
    fun testEndDrawerAccessibility() {
        val drawer1 = EndDrawer(contentDescription = "Settings menu")
        assertEquals("Settings menu", drawer1.getAccessibilityDescription())
        assertEquals("navigation", drawer1.getAccessibilityRole())

        val drawer2 = EndDrawer(semanticsLabel = "Navigation drawer")
        assertEquals("Navigation drawer", drawer2.getAccessibilityDescription())

        val drawer3 = EndDrawer()
        assertEquals("Navigation drawer", drawer3.getAccessibilityDescription())
    }

    @Test
    fun testEndDrawerFactoryMethods() {
        val child = createMockComponent()

        val simple = EndDrawer.simple(child)
        assertEquals(child, simple.child)

        val withWidth = EndDrawer.withWidth(child, 320f)
        assertEquals(320f, withWidth.width)

        val narrow = EndDrawer.narrow(child)
        assertEquals(240f, narrow.width)

        val wide = EndDrawer.wide(child)
        assertEquals(360f, wide.width)

        val noSwipe = EndDrawer.noSwipe(child)
        assertFalse(noSwipe.enableOpenDragGesture)

        val withElevation = EndDrawer.withElevation(child, 4f)
        assertEquals(4f, withElevation.elevation)
    }

    // ============================================================================
    // PopupMenuButton Tests (Bonus - 4 tests)
    // ============================================================================

    @Test
    fun testPopupMenuButtonDefaultValues() {
        val button = PopupMenuButton()

        assertEquals("more_vert", button.icon)
        assertNull(button.child)
        assertTrue(button.items.isEmpty())
        assertTrue(button.enabled)
        assertEquals(PopupMenuButton.PopupMenuPosition.Below, button.position)
        assertEquals("PopupMenuButton", button.type)
    }

    @Test
    fun testPopupMenuButtonAccessibility() {
        val button1 = PopupMenuButton(
            contentDescription = "More options"
        )
        assertEquals("More options", button1.getAccessibilityDescription())

        val button2 = PopupMenuButton(
            tooltip = "Actions menu"
        )
        assertEquals("Actions menu", button2.getAccessibilityDescription())

        val button3 = PopupMenuButton()
        assertEquals("Show menu", button3.getAccessibilityDescription())
    }

    @Test
    fun testPopupMenuItemCreation() {
        val item = PopupMenuItem(
            value = "delete",
            text = "Delete",
            icon = "delete_icon",
            enabled = false
        )

        assertEquals("delete", item.value)
        assertEquals("Delete", item.text)
        assertEquals("delete_icon", item.icon)
        assertFalse(item.enabled)
    }

    @Test
    fun testPopupMenuButtonFactoryMethods() {
        val items = listOf(
            PopupMenuItem("1", "Option 1"),
            PopupMenuItem("2", "Option 2")
        )

        val simple = PopupMenuButton.simple(items)
        assertEquals(2, simple.items.size)

        val withIcon = PopupMenuButton.withIcon("settings", items, "Settings")
        assertEquals("settings", withIcon.icon)
        assertEquals("Settings", withIcon.tooltip)

        val withTooltip = PopupMenuButton.withTooltip("Actions", items)
        assertEquals("Actions", withTooltip.tooltip)
    }

    // ============================================================================
    // RefreshIndicator Tests (Bonus - 3 tests)
    // ============================================================================

    @Test
    fun testRefreshIndicatorDefaultValues() {
        val indicator = RefreshIndicator()

        assertNull(indicator.child)
        assertEquals(40f, indicator.displacement)
        assertEquals(2f, indicator.strokeWidth)
        assertEquals(RefreshIndicator.RefreshIndicatorTriggerMode.OnEdge, indicator.triggerMode)
        assertEquals(0f, indicator.edgeOffset)
        assertEquals("RefreshIndicator", indicator.type)
    }

    @Test
    fun testRefreshIndicatorAccessibility() {
        val indicator1 = RefreshIndicator(
            contentDescription = "Pull to refresh content"
        )
        assertEquals("Pull to refresh content", indicator1.getAccessibilityDescription())

        val indicator2 = RefreshIndicator(
            semanticsLabel = "Refresh trigger"
        )
        assertEquals("Refresh trigger", indicator2.getAccessibilityDescription())

        val indicator3 = RefreshIndicator()
        assertEquals("Pull to refresh", indicator3.getAccessibilityDescription())
    }

    @Test
    fun testRefreshIndicatorFactoryMethods() {
        val child = createMockComponent()

        val simple = RefreshIndicator.simple(child)
        assertEquals(child, simple.child)

        val withColors = RefreshIndicator.withColors(child, "primary", "surface")
        assertEquals("primary", withColors.color)
        assertEquals("surface", withColors.backgroundColor)

        val withDisplacement = RefreshIndicator.withDisplacement(child, 60f)
        assertEquals(60f, withDisplacement.displacement)
    }

    // ============================================================================
    // Helper Methods
    // ============================================================================

    private fun createMockComponent(id: String = "mock"): com.augmentalis.avaelements.core.Component {
        return object : com.augmentalis.avaelements.core.Component {
            override val type = "MockComponent"
            override val id: String? = id
            override val style: com.augmentalis.avaelements.core.types.ComponentStyle? = null
            override val modifiers: List<com.augmentalis.avaelements.core.Modifier> = emptyList()
            override fun render(renderer: com.augmentalis.avaelements.core.Renderer): Any = Unit
        }
    }
}
