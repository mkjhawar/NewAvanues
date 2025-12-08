package com.augmentalis.avaelements.core

import com.augmentalis.avaelements.core.types.*

import kotlin.test.*

/**
 * Test suite for Component interfaces and base classes
 *
 * Tests component interface implementation, properties, lifecycle, and modifiers.
 * Coverage: Component interface, ComponentStyle, Modifier system, rendering
 */
class ComponentTest {

    // ==================== Component Interface Tests ====================

    @Test
    fun should_createComponent_when_implementingInterface() {
        // Given/When
        val component = TestComponent(
            id = "test1",
            text = "Hello"
        )

        // Then
        assertEquals("test1", component.id)
        assertEquals("Hello", component.text)
        assertNull(component.style)
        assertTrue(component.modifiers.isEmpty())
    }

    @Test
    fun should_renderComponent_when_renderCalled() {
        // Given
        val component = TestComponent(id = "test1", text = "Hello")
        val renderer = TestRenderer()

        // When
        val result = component.render(renderer)

        // Then
        assertEquals("Rendered: Hello", result)
    }

    @Test
    fun should_allowNullId_when_idNotRequired() {
        // Given/When
        val component = TestComponent(id = null, text = "Hello")

        // Then
        assertNull(component.id)
    }

    // ==================== ComponentStyle Tests ====================

    @Test
    fun should_createStyle_when_defaultValues() {
        // Given/When
        val style = ComponentStyle()

        // Then
        assertNull(style.width)
        assertNull(style.height)
        assertNull(style.padding)
        assertNull(style.margin)
        assertNull(style.backgroundColor)
        assertNull(style.border)
        assertNull(style.shadow)
        assertNull(style.opacity)
        // Note: overflow and visibility removed from ComponentStyle
    }

    @Test
    fun should_createStyle_when_customValues() {
        // Given/When
        val style = ComponentStyle(
            width = Size.Fixed(100f),
            height = Size.Fixed(50f),
            padding = Spacing.all(16f),
            margin = Spacing.all(8f),
            backgroundColor = Color.Blue,
            opacity = 0.8f
            // visibility removed - no longer in ComponentStyle
        )

        // Then
        assertEquals(Size.Fixed(100f), style.width)
        assertEquals(Size.Fixed(50f), style.height)
        assertEquals(Spacing.all(16f), style.padding)
        assertEquals(Spacing.all(8f), style.margin)
        assertEquals(Color.Blue, style.backgroundColor)
        assertEquals(0.8f, style.opacity)
        // Note: visibility property removed from ComponentStyle
    }

    @Test
    fun should_throwException_when_opacityInvalid() {
        // When/Then
        assertFailsWith<IllegalArgumentException> {
            ComponentStyle(opacity = 1.5f)
        }

        assertFailsWith<IllegalArgumentException> {
            ComponentStyle(opacity = -0.1f)
        }
    }

    @Test
    fun should_acceptValidOpacity_when_inRange() {
        // Given/When
        val style1 = ComponentStyle(opacity = 0.0f)
        val style2 = ComponentStyle(opacity = 0.5f)
        val style3 = ComponentStyle(opacity = 1.0f)

        // Then
        assertEquals(0.0f, style1.opacity)
        assertEquals(0.5f, style2.opacity)
        assertEquals(1.0f, style3.opacity)
    }

    // ==================== Modifier System Tests ====================

    @Test
    fun should_applyPaddingModifier_when_added() {
        // Given/When
        val modifier = Modifier.Padding(Spacing.all(16f))

        // Then
        assertTrue(modifier is Modifier.Padding)
        assertEquals(Spacing.all(16f), modifier.spacing)
    }

    @Test
    fun should_applyBackgroundModifier_when_added() {
        // Given/When
        val modifier = Modifier.Background(Color.Red)

        // Then
        assertTrue(modifier is Modifier.Background)
        assertEquals(Color.Red, modifier.color)
    }

    @Test
    fun should_applyGradientBackgroundModifier_when_added() {
        // Given
        val gradient = Gradient.Linear(
            colors = listOf(
                Gradient.ColorStop(Color.Red, 0f),
                Gradient.ColorStop(Color.Blue, 1f)
            ),
            angle = 90f
        )

        // When
        val modifier = Modifier.BackgroundGradient(gradient)

        // Then
        assertTrue(modifier is Modifier.BackgroundGradient)
        assertEquals(gradient, modifier.gradient)
    }

    @Test
    fun should_applySizeModifier_when_added() {
        // Given/When
        val modifier = Modifier.Size(
            width = Size.Fixed(100f),
            height = Size.Fixed(200f)
        )

        // Then
        assertTrue(modifier is Modifier.Size)
        assertEquals(Size.Fixed(100f), modifier.width)
        assertEquals(Size.Fixed(200f), modifier.height)
    }

    @Test
    fun should_applyClickableModifier_when_added() {
        // Given
        var clicked = false
        val onClick = { clicked = true }

        // When
        val modifier = Modifier.Clickable(onClick)
        modifier.onClick()

        // Then
        assertTrue(modifier is Modifier.Clickable)
        assertTrue(clicked)
    }

    @Test
    fun should_applyAlignmentModifier_when_added() {
        // Given/When
        val modifier = Modifier.Align(Alignment.Center)

        // Then
        assertTrue(modifier is Modifier.Align)
        assertEquals(Alignment.Center, modifier.alignment)
    }

    @Test
    fun should_applyWeightModifier_when_added() {
        // Given/When
        val modifier = Modifier.Weight(1.5f)

        // Then
        assertTrue(modifier is Modifier.Weight)
        assertEquals(1.5f, modifier.value)
    }

    @Test
    fun should_applyTransformModifiers_when_added() {
        // Given/When
        val rotateModifier = Modifier.Transform(Modifier.Transformation.Rotate(45f))
        val scaleModifier = Modifier.Transform(Modifier.Transformation.Scale(2f, 2f))
        val translateModifier = Modifier.Transform(Modifier.Transformation.Translate(10f, 20f))

        // Then
        assertTrue(rotateModifier is Modifier.Transform)
        assertTrue(rotateModifier.transformation is Modifier.Transformation.Rotate)
        assertEquals(45f, (rotateModifier.transformation as Modifier.Transformation.Rotate).degrees)

        assertTrue(scaleModifier.transformation is Modifier.Transformation.Scale)
        assertEquals(2f, (scaleModifier.transformation as Modifier.Transformation.Scale).x)

        assertTrue(translateModifier.transformation is Modifier.Transformation.Translate)
        assertEquals(10f, (translateModifier.transformation as Modifier.Transformation.Translate).x)
    }

    @Test
    fun should_applyClipModifier_when_added() {
        // Given/When
        val circleClip = Modifier.Clip(Modifier.ClipShape.Circle)
        val rectangleClip = Modifier.Clip(
            Modifier.ClipShape.Rectangle(CornerRadius.all(8f))
        )

        // Then
        assertTrue(circleClip is Modifier.Clip)
        assertTrue(circleClip.shape is Modifier.ClipShape.Circle)

        assertTrue(rectangleClip.shape is Modifier.ClipShape.Rectangle)
        assertEquals(
            CornerRadius.all(8f),
            (rectangleClip.shape as Modifier.ClipShape.Rectangle).radius
        )
    }

    @Test
    fun should_supportFillModifiers_when_applied() {
        // Given/When
        val fillWidth = Modifier.FillMaxWidth
        val fillHeight = Modifier.FillMaxHeight
        val fillSize = Modifier.FillMaxSize

        // Then
        assertTrue(fillWidth is Modifier.FillMaxWidth)
        assertTrue(fillHeight is Modifier.FillMaxHeight)
        assertTrue(fillSize is Modifier.FillMaxSize)
    }

    @Test
    fun should_chainModifiers_when_multipleApplied() {
        // Given/When
        val modifiers = listOf(
            Modifier.Padding(Spacing.all(16f)),
            Modifier.Background(Color.Blue),
            Modifier.CornerRadius(CornerRadius.all(8f)),
            Modifier.Clickable { }
        )

        val component = TestComponent(
            id = "test1",
            text = "Hello",
            modifiers = modifiers
        )

        // Then
        assertEquals(4, component.modifiers.size)
        assertTrue(component.modifiers[0] is Modifier.Padding)
        assertTrue(component.modifiers[1] is Modifier.Background)
        assertTrue(component.modifiers[2] is Modifier.CornerRadius)
        assertTrue(component.modifiers[3] is Modifier.Clickable)
    }

    // ==================== ComponentScope Tests ====================

    @Test
    fun should_buildModifiers_when_usingScope() {
        // Given/When
        val scope = TestComponentScope()
        scope.padding(16f)
        scope.background(Color.Red)
        scope.cornerRadius(8f)

        // Then
        assertEquals(3, scope.allModifiers().size)
        assertTrue(scope.allModifiers()[0] is Modifier.Padding)
        assertTrue(scope.allModifiers()[1] is Modifier.Background)
        assertTrue(scope.allModifiers()[2] is Modifier.CornerRadius)
    }

    @Test
    fun should_supportVariousPaddingMethods_when_usingScope() {
        // Given
        val scope1 = TestComponentScope()
        val scope2 = TestComponentScope()
        val scope3 = TestComponentScope()

        // When
        scope1.padding(Spacing.all(16f))
        scope2.padding(16f)
        scope3.padding(vertical = 8f, horizontal = 16f)

        // Then
        assertEquals(1, scope1.allModifiers().size)
        assertEquals(1, scope2.allModifiers().size)
        assertEquals(1, scope3.allModifiers().size)
    }

    @Test
    fun should_supportSizeModifiers_when_usingScope() {
        // Given/When
        val scope = TestComponentScope()
        scope.size(width = Size.Fixed(100f), height = Size.Fixed(200f))
        scope.fillMaxWidth()
        scope.fillMaxHeight()
        scope.fillMaxSize()

        // Then
        assertEquals(4, scope.allModifiers().size)
        assertTrue(scope.allModifiers()[0] is Modifier.Size)
        assertTrue(scope.allModifiers()[1] is Modifier.FillMaxWidth)
        assertTrue(scope.allModifiers()[2] is Modifier.FillMaxHeight)
        assertTrue(scope.allModifiers()[3] is Modifier.FillMaxSize)
    }

    @Test
    fun should_supportTransformModifiers_when_usingScope() {
        // Given/When
        val scope = TestComponentScope()
        scope.rotate(45f)
        scope.scale(2f, 2f)

        // Then
        assertEquals(2, scope.allModifiers().size)
        assertTrue(scope.allModifiers()[0] is Modifier.Transform)
        assertTrue(scope.allModifiers()[1] is Modifier.Transform)
    }

    // ==================== Renderer Tests ====================

    @Test
    fun should_identifyPlatform_when_rendererCreated() {
        // Given/When
        val androidRenderer = TestRenderer(Renderer.Platform.Android)
        val iosRenderer = TestRenderer(Renderer.Platform.iOS)
        val desktopRenderer = TestRenderer(Renderer.Platform.macOS)

        // Then
        assertEquals(Renderer.Platform.Android, androidRenderer.platform)
        assertEquals(Renderer.Platform.iOS, iosRenderer.platform)
        assertEquals(Renderer.Platform.macOS, desktopRenderer.platform)
    }

    @Test
    fun should_renderComponent_when_platformSpecific() {
        // Given
        val component = TestComponent(id = "test1", text = "Hello")
        val renderer = TestRenderer()

        // When
        val result = renderer.render(component)

        // Then
        assertEquals("Rendered: Hello", result)
    }

    // ==================== Helper Classes ====================

    private data class TestComponent(
        override val type: String = "TestComponent",
        override val id: String?,
        val text: String,
        override val style: ComponentStyle? = null,
        override val modifiers: List<Modifier> = emptyList()
    ) : Component {
        override fun render(renderer: Renderer): Any {
            return "Rendered: $text"
        }
    }

    private class TestComponentScope : ComponentScope() {
        fun allModifiers(): List<Modifier> = modifiers.toList()
    }

    private class TestRenderer(
        override val platform: Renderer.Platform = Renderer.Platform.Android
    ) : Renderer {
        override fun render(component: Component): Any {
            return component.render(this)
        }

        override fun applyTheme(theme: Theme) {
            // No-op for testing
        }
    }
}
