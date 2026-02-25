package com.augmentalis.avanueui.renderer.ios.bridge

/**
 * SwiftUI Bridge Data Models
 *
 * These data classes serve as the bridge between Kotlin/Native and Swift.
 * They represent SwiftUI views in a format that can be consumed by Swift code.
 *
 * Architecture:
 * Kotlin AvaElements → SwiftUIView (Bridge) → Swift SwiftUI Views
 */

/**
 * Bridge representation of a SwiftUI view
 * This is consumed by Swift code to render actual SwiftUI views
 */
data class SwiftUIView(
    val type: ViewType,
    val id: String? = null,
    val properties: Map<String, Any>,
    val modifiers: List<SwiftUIModifier> = emptyList(),
    val children: List<SwiftUIView> = emptyList()
) {
    companion object {
        /**
         * Create a VStack (vertical stack)
         */
        fun vStack(
            spacing: Float? = null,
            alignment: HorizontalAlignment = HorizontalAlignment.Center,
            children: List<SwiftUIView> = emptyList(),
            modifiers: List<SwiftUIModifier> = emptyList()
        ) = SwiftUIView(
            type = ViewType.VStack,
            properties = buildMap {
                spacing?.let { put("spacing", it) }
                put("alignment", alignment.name)
            },
            children = children,
            modifiers = modifiers
        )

        /**
         * Create an HStack (horizontal stack)
         */
        fun hStack(
            spacing: Float? = null,
            alignment: VerticalAlignment = VerticalAlignment.Center,
            children: List<SwiftUIView> = emptyList(),
            modifiers: List<SwiftUIModifier> = emptyList()
        ) = SwiftUIView(
            type = ViewType.HStack,
            properties = buildMap {
                spacing?.let { put("spacing", it) }
                put("alignment", alignment.name)
            },
            children = children,
            modifiers = modifiers
        )

        /**
         * Create a ZStack (depth stack)
         */
        fun zStack(
            alignment: ZStackAlignment = ZStackAlignment.Center,
            children: List<SwiftUIView> = emptyList(),
            modifiers: List<SwiftUIModifier> = emptyList()
        ) = SwiftUIView(
            type = ViewType.ZStack,
            properties = mapOf("alignment" to alignment.name),
            children = children,
            modifiers = modifiers
        )

        /**
         * Create a Text view
         */
        fun text(
            content: String,
            modifiers: List<SwiftUIModifier> = emptyList()
        ) = SwiftUIView(
            type = ViewType.Text,
            properties = mapOf("content" to content),
            modifiers = modifiers
        )

        /**
         * Create a Button view
         */
        fun button(
            label: String,
            action: String? = null,
            modifiers: List<SwiftUIModifier> = emptyList()
        ) = SwiftUIView(
            type = ViewType.Button,
            properties = buildMap {
                put("label", label)
                action?.let { put("action", it) }
            },
            modifiers = modifiers
        )
    }
}

/**
 * SwiftUI view types that map to native SwiftUI components
 */
enum class ViewType {
    // Layout containers
    VStack,          // Vertical stack
    HStack,          // Horizontal stack
    ZStack,          // Depth/overlay stack
    ScrollView,      // Scrollable container
    Group,           // Transparent grouping container

    // Content views
    Text,            // Text label
    Button,          // Button with action
    TextField,       // Text input field
    SecureField,     // Password input field
    Toggle,          // Switch/Toggle control
    Image,           // Image view (system or asset)
    Label,           // Text with icon

    // Shapes
    RoundedRectangle,
    Circle,
    Rectangle,
    Capsule,

    // Special views
    Spacer,          // Flexible space
    Divider,         // Visual separator
    EmptyView        // Empty placeholder
}

/**
 * SwiftUI view modifiers
 * These are applied to views to modify their appearance and behavior
 */
data class SwiftUIModifier(
    val type: ModifierType,
    val value: Any
) {
    companion object {
        // Layout modifiers
        fun padding(value: Float) = SwiftUIModifier(ModifierType.Padding, value)
        fun padding(top: Float, leading: Float, bottom: Float, trailing: Float) =
            SwiftUIModifier(ModifierType.PaddingEdges, EdgeInsets(top, leading, bottom, trailing))
        fun frame(width: SizeValue? = null, height: SizeValue? = null, alignment: ZStackAlignment = ZStackAlignment.Center) =
            SwiftUIModifier(ModifierType.Frame, FrameValue(width, height, alignment))

        // Appearance modifiers
        fun background(color: SwiftUIColor) = SwiftUIModifier(ModifierType.Background, color)
        fun foregroundColor(color: SwiftUIColor) = SwiftUIModifier(ModifierType.ForegroundColor, color)
        fun cornerRadius(value: Float) = SwiftUIModifier(ModifierType.CornerRadius, value)
        fun shadow(radius: Float, x: Float = 0f, y: Float = 0f) =
            SwiftUIModifier(ModifierType.Shadow, ShadowValue(radius, x, y))
        fun opacity(value: Float) = SwiftUIModifier(ModifierType.Opacity, value)

        // Font modifiers
        fun font(style: FontStyle) = SwiftUIModifier(ModifierType.Font, style)
        fun fontWeight(weight: FontWeight) = SwiftUIModifier(ModifierType.FontWeight, weight)
        fun fontSize(size: Float) = SwiftUIModifier(ModifierType.FontSize, size)

        // Border modifiers
        fun border(color: SwiftUIColor, width: Float = 1f) =
            SwiftUIModifier(ModifierType.Border, BorderValue(color, width))
        fun overlay(view: SwiftUIView) = SwiftUIModifier(ModifierType.Overlay, view)

        // Interaction modifiers
        fun disabled(value: Boolean) = SwiftUIModifier(ModifierType.Disabled, value)

        // Layout behavior modifiers
        fun fillMaxWidth() = SwiftUIModifier(ModifierType.Frame, FrameValue(SizeValue.Infinity, null, ZStackAlignment.Center))
        fun fillMaxHeight() = SwiftUIModifier(ModifierType.Frame, FrameValue(null, SizeValue.Infinity, ZStackAlignment.Center))
        fun fillMaxSize() = SwiftUIModifier(ModifierType.Frame, FrameValue(SizeValue.Infinity, SizeValue.Infinity, ZStackAlignment.Center))
    }
}

/**
 * Types of modifiers available in SwiftUI
 */
enum class ModifierType {
    // Layout
    Padding,
    PaddingEdges,
    Frame,
    LayoutPriority,

    // Appearance
    Background,
    ForegroundColor,
    CornerRadius,
    ClipShape,
    Shadow,
    Opacity,
    Blur,

    // Typography
    Font,
    FontWeight,
    FontSize,
    FontDesign,
    TextAlignment,
    LineLimit,
    LineSpacing,

    // Border & Stroke
    Border,
    Stroke,
    Overlay,

    // Interaction
    Disabled,
    OnTapGesture,
    OnLongPressGesture,

    // Animation
    Animation,
    Transition,

    // Accessibility
    AccessibilityLabel,
    AccessibilityHint
}

/**
 * SwiftUI color representation
 */
data class SwiftUIColor(
    val type: ColorType,
    val value: Any
) {
    enum class ColorType {
        RGB,           // Custom RGB color
        System,        // System semantic color
        Named          // Asset catalog color
    }

    companion object {
        fun rgb(red: Float, green: Float, blue: Float, opacity: Float = 1.0f) =
            SwiftUIColor(ColorType.RGB, RGBValue(red, green, blue, opacity))

        fun system(name: String) = SwiftUIColor(ColorType.System, name)

        // Common system colors
        val primary = system("primary")
        val secondary = system("secondary")
        val clear = system("clear")
        val white = system("white")
        val black = system("black")
        val red = system("red")
        val blue = system("blue")
        val green = system("green")
    }
}

/**
 * RGB color value (0-1 range for SwiftUI)
 */
data class RGBValue(
    val red: Float,
    val green: Float,
    val blue: Float,
    val opacity: Float = 1.0f
)

/**
 * Edge insets for padding
 */
data class EdgeInsets(
    val top: Float,
    val leading: Float,
    val bottom: Float,
    val trailing: Float
)

/**
 * Frame size values
 */
sealed class SizeValue {
    data class Fixed(val value: Float) : SizeValue()
    object Infinity : SizeValue()
    object Ideal : SizeValue()
}

/**
 * Frame modifier value
 */
data class FrameValue(
    val width: SizeValue?,
    val height: SizeValue?,
    val alignment: ZStackAlignment
)

/**
 * Shadow modifier value
 */
data class ShadowValue(
    val radius: Float,
    val x: Float = 0f,
    val y: Float = 0f,
    val color: SwiftUIColor = SwiftUIColor.black.copy(value = RGBValue(0f, 0f, 0f, 0.33f))
)

/**
 * Border modifier value
 */
data class BorderValue(
    val color: SwiftUIColor,
    val width: Float
)

/**
 * SwiftUI font styles
 */
enum class FontStyle {
    LargeTitle,
    Title,
    Title2,
    Title3,
    Headline,
    Subheadline,
    Body,
    Callout,
    Footnote,
    Caption,
    Caption2,
    System
}

/**
 * SwiftUI font weights
 */
enum class FontWeight {
    UltraLight,
    Thin,
    Light,
    Regular,
    Medium,
    Semibold,
    Bold,
    Heavy,
    Black
}

/**
 * Horizontal alignment for VStack
 */
enum class HorizontalAlignment {
    Leading,
    Center,
    Trailing
}

/**
 * Vertical alignment for HStack
 */
enum class VerticalAlignment {
    Top,
    Center,
    Bottom,
    FirstTextBaseline,
    LastTextBaseline
}

/**
 * ZStack and frame alignment
 */
enum class ZStackAlignment {
    TopLeading,
    Top,
    TopTrailing,
    Leading,
    Center,
    Trailing,
    BottomLeading,
    Bottom,
    BottomTrailing
}

/**
 * Text alignment
 */
enum class TextAlignment {
    Leading,
    Center,
    Trailing
}

/**
 * Button style types
 */
enum class ButtonStyleType {
    Automatic,
    Plain,
    Bordered,
    BorderedProminent,
    Borderless
}
