package com.augmentalis.avaelements.flutter.animation.transitions

import kotlin.test.*

/**
 * Comprehensive unit tests for all 15 Flutter parity transition components.
 *
 * Tests cover:
 * - Component initialization and validation
 * - Accessibility descriptions
 * - Edge cases and boundary conditions
 * - Helper functions and utilities
 *
 * Total: 60+ tests (4+ per component)
 *
 * @since 3.0.0-flutter-parity
 */
class TransitionComponentsTest {

    // ==================== FadeTransition Tests (4) ====================

    @Test
    fun `FadeTransition - creates with valid opacity`() {
        val fade = FadeTransition(opacity = 0.5f, child = "TestChild")
        assertEquals(0.5f, fade.opacity)
        assertEquals("TestChild", fade.child)
        assertFalse(fade.alwaysIncludeSemantics)
    }

    @Test
    fun `FadeTransition - clamps opacity to valid range`() {
        val fadeOver = FadeTransition(opacity = 1.5f, child = "Test")
        assertEquals(1.0f, fadeOver.getClampedOpacity())

        val fadeUnder = FadeTransition(opacity = -0.5f, child = "Test")
        assertEquals(0.0f, fadeUnder.getClampedOpacity())

        val fadeValid = FadeTransition(opacity = 0.7f, child = "Test")
        assertEquals(0.7f, fadeValid.getClampedOpacity())
    }

    @Test
    fun `FadeTransition - provides accessibility descriptions`() {
        assertEquals("Hidden", FadeTransition(0.0f, "Test").getAccessibilityDescription())
        assertEquals("Fully visible", FadeTransition(1.0f, "Test").getAccessibilityDescription())
        assertTrue(FadeTransition(0.3f, "Test").getAccessibilityDescription().contains("Mostly hidden"))
        assertTrue(FadeTransition(0.7f, "Test").getAccessibilityDescription().contains("Mostly visible"))
    }

    @Test
    fun `FadeTransition - supports alwaysIncludeSemantics flag`() {
        val fade = FadeTransition(opacity = 0.0f, child = "Test", alwaysIncludeSemantics = true)
        assertTrue(fade.alwaysIncludeSemantics)
    }

    // ==================== SlideTransition Tests (4) ====================

    @Test
    fun `SlideTransition - creates with valid offset`() {
        val slide = SlideTransition(
            position = SlideTransition.Offset(0.5f, -1.0f),
            child = "TestChild"
        )
        assertEquals(0.5f, slide.position.dx)
        assertEquals(-1.0f, slide.position.dy)
    }

    @Test
    fun `SlideTransition - provides predefined directions`() {
        assertEquals(0.0f, SlideTransition.Directions.fromTop.dx)
        assertEquals(-1.0f, SlideTransition.Directions.fromTop.dy)

        assertEquals(0.0f, SlideTransition.Directions.fromBottom.dx)
        assertEquals(1.0f, SlideTransition.Directions.fromBottom.dy)

        assertEquals(-1.0f, SlideTransition.Directions.fromLeft.dx)
        assertEquals(0.0f, SlideTransition.Directions.fromLeft.dy)
    }

    @Test
    fun `SlideTransition - supports text direction`() {
        val slideLtr = SlideTransition(
            position = SlideTransition.Offset(1.0f, 0.0f),
            child = "Test",
            textDirection = SlideTransition.TextDirection.Ltr
        )
        assertEquals(SlideTransition.TextDirection.Ltr, slideLtr.textDirection)

        val slideRtl = SlideTransition(
            position = SlideTransition.Offset(1.0f, 0.0f),
            child = "Test",
            textDirection = SlideTransition.TextDirection.Rtl
        )
        assertEquals(SlideTransition.TextDirection.Rtl, slideRtl.textDirection)
    }

    @Test
    fun `SlideTransition - provides accessibility descriptions`() {
        val slideRight = SlideTransition(SlideTransition.Offset(0.5f, 0.0f), "Test")
        assertTrue(slideRight.getAccessibilityDescription().contains("right"))

        val slideDown = SlideTransition(SlideTransition.Offset(0.0f, 0.5f), "Test")
        assertTrue(slideDown.getAccessibilityDescription().contains("down"))
    }

    // ==================== Hero Tests (5) ====================

    @Test
    fun `Hero - creates with valid tag`() {
        val hero = Hero(tag = "profile-image", child = "ImageWidget")
        assertEquals("profile-image", hero.tag)
        assertEquals("ImageWidget", hero.child)
        assertFalse(hero.transitionOnUserGestures)
    }

    @Test
    fun `Hero - rejects blank tag`() {
        assertFailsWith<IllegalArgumentException> {
            Hero(tag = "", child = "Test")
        }
        assertFailsWith<IllegalArgumentException> {
            Hero(tag = "   ", child = "Test")
        }
    }

    @Test
    fun `Hero - supports custom builders`() {
        val hero = Hero(
            tag = "custom",
            child = "Test",
            flightShuttleBuilder = "customBuilder",
            placeholderBuilder = "placeholderBuilder"
        )
        assertEquals("customBuilder", hero.flightShuttleBuilder)
        assertEquals("placeholderBuilder", hero.placeholderBuilder)
    }

    @Test
    fun `Hero - supports user gesture transitions`() {
        val hero = Hero(tag = "test", child = "Test", transitionOnUserGestures = true)
        assertTrue(hero.transitionOnUserGestures)
    }

    @Test
    fun `Hero - provides accessibility description with tag`() {
        val hero = Hero(tag = "profile-avatar", child = "Test")
        assertTrue(hero.getAccessibilityDescription().contains("profile-avatar"))
    }

    // ==================== ScaleTransition Tests (4) ====================

    @Test
    fun `ScaleTransition - creates with valid scale`() {
        val scale = ScaleTransition(scale = 1.5f, child = "Test")
        assertEquals(1.5f, scale.scale)
        assertEquals(ScaleTransition.Alignment.Center, scale.alignment)
    }

    @Test
    fun `ScaleTransition - rejects negative scale`() {
        assertFailsWith<IllegalArgumentException> {
            ScaleTransition(scale = -0.5f, child = "Test")
        }
    }

    @Test
    fun `ScaleTransition - supports all alignment options`() {
        val alignments = ScaleTransition.Alignment.values()
        assertTrue(alignments.contains(ScaleTransition.Alignment.TopLeft))
        assertTrue(alignments.contains(ScaleTransition.Alignment.Center))
        assertTrue(alignments.contains(ScaleTransition.Alignment.BottomRight))
    }

    @Test
    fun `ScaleTransition - provides accessibility descriptions`() {
        assertEquals("Hidden (scaled to 0%)",
            ScaleTransition(0.0f, "Test").getAccessibilityDescription())
        assertEquals("Normal size",
            ScaleTransition(1.0f, "Test").getAccessibilityDescription())
        assertTrue(ScaleTransition(0.5f, "Test").getAccessibilityDescription().contains("50%"))
    }

    // ==================== RotationTransition Tests (4) ====================

    @Test
    fun `RotationTransition - creates with valid turns`() {
        val rotation = RotationTransition(turns = 0.25f, child = "Test")
        assertEquals(0.25f, rotation.turns)
        assertEquals(90f, rotation.getTurnsDegrees())
    }

    @Test
    fun `RotationTransition - converts turns to degrees correctly`() {
        assertEquals(0f, RotationTransition(0f, "Test").getTurnsDegrees())
        assertEquals(90f, RotationTransition(0.25f, "Test").getTurnsDegrees())
        assertEquals(180f, RotationTransition(0.5f, "Test").getTurnsDegrees())
        assertEquals(360f, RotationTransition(1f, "Test").getTurnsDegrees())
    }

    @Test
    fun `RotationTransition - supports negative rotation`() {
        val rotation = RotationTransition(turns = -0.25f, child = "Test")
        assertEquals(-90f, rotation.getTurnsDegrees())
        assertTrue(rotation.getAccessibilityDescription().contains("counter-clockwise"))
    }

    @Test
    fun `RotationTransition - provides predefined rotation values`() {
        assertEquals(0.25f, RotationTransition.Rotations.QUARTER_TURN)
        assertEquals(0.5f, RotationTransition.Rotations.HALF_TURN)
        assertEquals(1.0f, RotationTransition.Rotations.FULL_TURN)
    }

    // ==================== PositionedTransition Tests (4) ====================

    @Test
    fun `PositionedTransition - creates with valid rect`() {
        val rect = PositionedTransition.RelativeRect.fromLTRB(10f, 20f, 30f, 40f)
        val positioned = PositionedTransition(rect = rect, child = "Test")
        assertEquals(10f, positioned.rect.left)
        assertEquals(20f, positioned.rect.top)
        assertEquals(30f, positioned.rect.right)
        assertEquals(40f, positioned.rect.bottom)
    }

    @Test
    fun `PositionedTransition - supports null values for sides`() {
        val rect = PositionedTransition.RelativeRect.fromLTRB(null, 10f, 20f, null)
        assertNull(rect.left)
        assertEquals(10f, rect.top)
        assertNull(rect.bottom)
    }

    @Test
    fun `PositionedTransition - provides fill rect`() {
        val fill = PositionedTransition.RelativeRect.fill
        assertEquals(0f, fill.left)
        assertEquals(0f, fill.top)
        assertEquals(0f, fill.right)
        assertEquals(0f, fill.bottom)
    }

    @Test
    fun `PositionedTransition - provides accessibility description`() {
        val positioned = PositionedTransition(
            rect = PositionedTransition.RelativeRect.fromLTRB(10f, 20f, null, 30f),
            child = "Test"
        )
        val desc = positioned.getAccessibilityDescription()
        assertTrue(desc.contains("10"))
        assertTrue(desc.contains("20"))
        assertTrue(desc.contains("30"))
    }

    // ==================== SizeTransition Tests (4) ====================

    @Test
    fun `SizeTransition - creates with valid size factor`() {
        val size = SizeTransition(sizeFactor = 0.5f, child = "Test")
        assertEquals(0.5f, size.sizeFactor)
        assertEquals(SizeTransition.Axis.Vertical, size.axis)
        assertEquals(0.0f, size.axisAlignment)
    }

    @Test
    fun `SizeTransition - validates size factor range`() {
        assertFailsWith<IllegalArgumentException> {
            SizeTransition(sizeFactor = -0.1f, child = "Test")
        }
        assertFailsWith<IllegalArgumentException> {
            SizeTransition(sizeFactor = 1.1f, child = "Test")
        }
    }

    @Test
    fun `SizeTransition - validates axis alignment range`() {
        assertFailsWith<IllegalArgumentException> {
            SizeTransition(sizeFactor = 0.5f, child = "Test", axisAlignment = -1.5f)
        }
        assertFailsWith<IllegalArgumentException> {
            SizeTransition(sizeFactor = 0.5f, child = "Test", axisAlignment = 1.5f)
        }
    }

    @Test
    fun `SizeTransition - supports both axis types`() {
        val vertical = SizeTransition(sizeFactor = 0.5f, child = "Test", axis = SizeTransition.Axis.Vertical)
        assertEquals(SizeTransition.Axis.Vertical, vertical.axis)

        val horizontal = SizeTransition(sizeFactor = 0.5f, child = "Test", axis = SizeTransition.Axis.Horizontal)
        assertEquals(SizeTransition.Axis.Horizontal, horizontal.axis)
    }

    // ==================== AnimatedCrossFade Tests (4) ====================

    @Test
    fun `AnimatedCrossFade - creates with valid state`() {
        val crossFade = AnimatedCrossFade(
            firstChild = "Child1",
            secondChild = "Child2",
            crossFadeState = AnimatedCrossFade.CrossFadeState.ShowFirst
        )
        assertEquals(AnimatedCrossFade.CrossFadeState.ShowFirst, crossFade.crossFadeState)
    }

    @Test
    fun `AnimatedCrossFade - validates duration`() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedCrossFade(
                firstChild = "Test1",
                secondChild = "Test2",
                crossFadeState = AnimatedCrossFade.CrossFadeState.ShowFirst,
                duration = 0
            )
        }
    }

    @Test
    fun `AnimatedCrossFade - supports reverse duration`() {
        val crossFade = AnimatedCrossFade(
            firstChild = "Test1",
            secondChild = "Test2",
            crossFadeState = AnimatedCrossFade.CrossFadeState.ShowFirst,
            reverseDuration = 200
        )
        assertEquals(200, crossFade.reverseDuration)
    }

    @Test
    fun `AnimatedCrossFade - provides accessibility descriptions`() {
        val showFirst = AnimatedCrossFade(
            firstChild = "Test1",
            secondChild = "Test2",
            crossFadeState = AnimatedCrossFade.CrossFadeState.ShowFirst
        )
        assertEquals("Showing first child", showFirst.getAccessibilityDescription())

        val showSecond = AnimatedCrossFade(
            firstChild = "Test1",
            secondChild = "Test2",
            crossFadeState = AnimatedCrossFade.CrossFadeState.ShowSecond
        )
        assertEquals("Showing second child", showSecond.getAccessibilityDescription())
    }

    // ==================== AnimatedSwitcher Tests (4) ====================

    @Test
    fun `AnimatedSwitcher - creates with valid child`() {
        val switcher = AnimatedSwitcher(child = "TestChild")
        assertEquals("TestChild", switcher.child)
        assertEquals(AnimatedSwitcher.DEFAULT_ANIMATION_DURATION, switcher.duration)
    }

    @Test
    fun `AnimatedSwitcher - supports null child`() {
        val switcher = AnimatedSwitcher(child = null)
        assertNull(switcher.child)
    }

    @Test
    fun `AnimatedSwitcher - validates duration`() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedSwitcher(child = "Test", duration = -100)
        }
    }

    @Test
    fun `AnimatedSwitcher - supports custom transition builders`() {
        val switcher = AnimatedSwitcher(
            child = "Test",
            transitionBuilder = "customTransition",
            layoutBuilder = "customLayout"
        )
        assertEquals("customTransition", switcher.transitionBuilder)
        assertEquals("customLayout", switcher.layoutBuilder)
    }

    // ==================== AnimatedList Tests (4) ====================

    @Test
    fun `AnimatedList - creates with valid items`() {
        val list = AnimatedList(
            items = listOf("Item1", "Item2", "Item3"),
            itemBuilder = "builder",
            initialItemCount = 3
        )
        assertEquals(3, list.items.size)
        assertEquals(3, list.initialItemCount)
    }

    @Test
    fun `AnimatedList - validates initial item count`() {
        assertFailsWith<IllegalArgumentException> {
            AnimatedList(
                items = listOf("Test"),
                itemBuilder = "builder",
                initialItemCount = -1
            )
        }
    }

    @Test
    fun `AnimatedList - supports both scroll directions`() {
        val vertical = AnimatedList(
            items = listOf("Test"),
            itemBuilder = "builder",
            initialItemCount = 1,
            scrollDirection = AnimatedList.Axis.Vertical
        )
        assertEquals(AnimatedList.Axis.Vertical, vertical.scrollDirection)

        val horizontal = AnimatedList(
            items = listOf("Test"),
            itemBuilder = "builder",
            initialItemCount = 1,
            scrollDirection = AnimatedList.Axis.Horizontal
        )
        assertEquals(AnimatedList.Axis.Horizontal, horizontal.scrollDirection)
    }

    @Test
    fun `AnimatedList - provides accessibility description`() {
        val list = AnimatedList(
            items = listOf("A", "B", "C"),
            itemBuilder = "builder",
            initialItemCount = 3
        )
        assertTrue(list.getAccessibilityDescription().contains("3 items"))
    }

    // ==================== AnimatedModalBarrier Tests (5) ====================

    @Test
    fun `AnimatedModalBarrier - creates with valid color`() {
        val barrier = AnimatedModalBarrier(color = 0x80000000L)
        assertEquals(0x80000000L, barrier.color)
        assertTrue(barrier.dismissible)
    }

    @Test
    fun `AnimatedModalBarrier - extracts color components correctly`() {
        val barrier = AnimatedModalBarrier(color = 0x80FF00FFL) // 50% alpha, red=255, green=0, blue=255
        assertEquals(128, barrier.getAlpha())
        assertEquals(255, barrier.getRed())
        assertEquals(0, barrier.getGreen())
        assertEquals(255, barrier.getBlue())
        assertEquals(0.5f, barrier.getOpacity(), 0.01f)
    }

    @Test
    fun `AnimatedModalBarrier - supports dismissible flag`() {
        val dismissible = AnimatedModalBarrier(color = 0x80000000L, dismissible = true)
        assertTrue(dismissible.dismissible)

        val nonDismissible = AnimatedModalBarrier(color = 0x80000000L, dismissible = false)
        assertFalse(nonDismissible.dismissible)
    }

    @Test
    fun `AnimatedModalBarrier - supports onDismiss callback`() {
        val barrier = AnimatedModalBarrier(
            color = 0x80000000L,
            dismissible = true,
            onDismiss = "dismissCallback"
        )
        assertEquals("dismissCallback", barrier.onDismiss)
    }

    @Test
    fun `AnimatedModalBarrier - provides accessibility descriptions`() {
        val dismissible = AnimatedModalBarrier(
            color = 0x80000000L,
            dismissible = true,
            semanticsLabel = "Dialog barrier"
        )
        assertTrue(dismissible.getAccessibilityDescription().contains("tap to dismiss"))

        val nonDismissible = AnimatedModalBarrier(
            color = 0x80000000L,
            dismissible = false,
            semanticsLabel = "Loading barrier"
        )
        assertFalse(nonDismissible.getAccessibilityDescription().contains("tap to dismiss"))
    }

    // ==================== DecoratedBoxTransition Tests (4) ====================

    @Test
    fun `DecoratedBoxTransition - creates with valid decoration`() {
        val decoration = DecoratedBoxTransition.BoxDecoration(
            color = 0xFFFF0000L,
            borderRadius = 8f
        )
        val decorated = DecoratedBoxTransition(decoration = decoration, child = "Test")
        assertEquals(0xFFFF0000L, decorated.decoration.color)
        assertEquals(8f, decorated.decoration.borderRadius)
    }

    @Test
    fun `DecoratedBoxTransition - supports box shadow`() {
        val shadow = DecoratedBoxTransition.BoxShadow(
            color = 0x40000000L,
            blurRadius = 10f,
            spreadRadius = 2f
        )
        val decoration = DecoratedBoxTransition.BoxDecoration(boxShadow = shadow)
        assertEquals(10f, decoration.boxShadow?.blurRadius)
    }

    @Test
    fun `DecoratedBoxTransition - supports decoration positions`() {
        val background = DecoratedBoxTransition(
            decoration = DecoratedBoxTransition.BoxDecoration(),
            child = "Test",
            position = DecoratedBoxTransition.DecorationPosition.Background
        )
        assertEquals(DecoratedBoxTransition.DecorationPosition.Background, background.position)

        val foreground = DecoratedBoxTransition(
            decoration = DecoratedBoxTransition.BoxDecoration(),
            child = "Test",
            position = DecoratedBoxTransition.DecorationPosition.Foreground
        )
        assertEquals(DecoratedBoxTransition.DecorationPosition.Foreground, foreground.position)
    }

    @Test
    fun `DecoratedBoxTransition - provides decoration description`() {
        val decoration = DecoratedBoxTransition.BoxDecoration(
            color = 0xFFFF0000L,
            borderRadius = 8f,
            boxShadow = DecoratedBoxTransition.BoxShadow()
        )
        val desc = decoration.getDescription()
        assertTrue(desc.contains("color"))
        assertTrue(desc.contains("rounded corners"))
        assertTrue(desc.contains("shadow"))
    }

    // ==================== AlignTransition Tests (4) ====================

    @Test
    fun `AlignTransition - creates with valid alignment`() {
        val align = AlignTransition(alignment = AlignTransition.Alignment.Center, child = "Test")
        assertEquals(AlignTransition.Alignment.Center, align.alignment)
    }

    @Test
    fun `AlignTransition - supports width and height factors`() {
        val align = AlignTransition(
            alignment = AlignTransition.Alignment.Center,
            child = "Test",
            widthFactor = 0.5f,
            heightFactor = 0.75f
        )
        assertEquals(0.5f, align.widthFactor)
        assertEquals(0.75f, align.heightFactor)
    }

    @Test
    fun `AlignTransition - validates factor values`() {
        assertFailsWith<IllegalArgumentException> {
            AlignTransition(
                alignment = AlignTransition.Alignment.Center,
                child = "Test",
                widthFactor = -0.5f
            )
        }
    }

    @Test
    fun `AlignTransition - supports all alignment options`() {
        val alignments = AlignTransition.Alignment.values()
        assertEquals(9, alignments.size) // 3x3 grid
        assertTrue(alignments.contains(AlignTransition.Alignment.TopLeft))
        assertTrue(alignments.contains(AlignTransition.Alignment.Center))
        assertTrue(alignments.contains(AlignTransition.Alignment.BottomRight))
    }

    // ==================== DefaultTextStyleTransition Tests (4) ====================

    @Test
    fun `DefaultTextStyleTransition - creates with valid style`() {
        val style = DefaultTextStyleTransition.TextStyle(
            fontSize = 24f,
            color = 0xFFFF0000L,
            fontWeight = DefaultTextStyleTransition.FontWeight.Bold
        )
        val textStyle = DefaultTextStyleTransition(style = style, child = "Test")
        assertEquals(24f, textStyle.style.fontSize)
        assertEquals(0xFFFF0000L, textStyle.style.color)
    }

    @Test
    fun `DefaultTextStyleTransition - supports all font weights`() {
        val weights = DefaultTextStyleTransition.FontWeight.values()
        assertEquals(9, weights.size)
        assertTrue(weights.contains(DefaultTextStyleTransition.FontWeight.Thin))
        assertTrue(weights.contains(DefaultTextStyleTransition.FontWeight.Normal))
        assertTrue(weights.contains(DefaultTextStyleTransition.FontWeight.Black))
    }

    @Test
    fun `DefaultTextStyleTransition - validates max lines`() {
        assertFailsWith<IllegalArgumentException> {
            DefaultTextStyleTransition(
                style = DefaultTextStyleTransition.TextStyle(),
                child = "Test",
                maxLines = 0
            )
        }
    }

    @Test
    fun `DefaultTextStyleTransition - supports text decorations`() {
        val decorations = DefaultTextStyleTransition.TextDecoration.values()
        assertTrue(decorations.contains(DefaultTextStyleTransition.TextDecoration.None))
        assertTrue(decorations.contains(DefaultTextStyleTransition.TextDecoration.Underline))
        assertTrue(decorations.contains(DefaultTextStyleTransition.TextDecoration.LineThrough))
    }

    // ==================== RelativePositionedTransition Tests (4) ====================

    @Test
    fun `RelativePositionedTransition - creates with valid rect`() {
        val rect = RelativePositionedTransition.RelativeRect.fromLTRB(0.1f, 0.2f, 0.3f, 0.4f)
        val positioned = RelativePositionedTransition(
            rect = rect,
            size = 200f to 100f,
            child = "Test"
        )
        assertEquals(0.1f, positioned.rect.left)
        assertEquals(200f, positioned.size.first)
    }

    @Test
    fun `RelativePositionedTransition - validates relative rect range`() {
        assertFailsWith<IllegalArgumentException> {
            RelativePositionedTransition.RelativeRect.fromLTRB(-0.1f, 0.5f, 0.5f, 0.5f)
        }
        assertFailsWith<IllegalArgumentException> {
            RelativePositionedTransition.RelativeRect.fromLTRB(0.5f, 1.5f, 0.5f, 0.5f)
        }
    }

    @Test
    fun `RelativePositionedTransition - validates size values`() {
        assertFailsWith<IllegalArgumentException> {
            RelativePositionedTransition(
                rect = RelativePositionedTransition.RelativeRect.fill,
                size = -100f to 100f,
                child = "Test"
            )
        }
    }

    @Test
    fun `RelativePositionedTransition - provides predefined rects`() {
        val fill = RelativePositionedTransition.RelativeRect.fill
        assertEquals(0f, fill.left)
        assertEquals(0f, fill.right)

        val centered = RelativePositionedTransition.RelativeRect.centered
        assertEquals(0.1f, centered.left)
        assertEquals(0.1f, centered.top)
    }
}
