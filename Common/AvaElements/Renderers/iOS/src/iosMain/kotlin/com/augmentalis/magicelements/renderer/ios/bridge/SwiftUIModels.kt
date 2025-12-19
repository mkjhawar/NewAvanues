package com.augmentalis.avaelements.renderer.ios.bridge

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
            modifiers: List<SwiftUIModifier> = emptyList(),
            id: String? = null
        ) = SwiftUIView(
            type = ViewType.VStack,
            id = id,
            properties = buildMap {
                spacing?.let { put("spacing", it) }
                put("alignment", alignment.name)
            },
            modifiers = modifiers,
            children = children
        )

        /**
         * Create an HStack (horizontal stack)
         */
        fun hStack(
            spacing: Float? = null,
            alignment: VerticalAlignment = VerticalAlignment.Center,
            children: List<SwiftUIView> = emptyList(),
            modifiers: List<SwiftUIModifier> = emptyList(),
            id: String? = null
        ) = SwiftUIView(
            type = ViewType.HStack,
            id = id,
            properties = buildMap {
                spacing?.let { put("spacing", it) }
                put("alignment", alignment.name)
            },
            modifiers = modifiers,
            children = children
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
 * Sealed class to support both standard types and custom named types
 */
sealed class ViewType {
    // Layout containers
    object VStack : ViewType()          // Vertical stack
    object HStack : ViewType()          // Horizontal stack
    object ZStack : ViewType()          // Depth/overlay stack
    object ScrollView : ViewType()      // Scrollable container
    object Group : ViewType()           // Transparent grouping container

    // Content views
    object Text : ViewType()            // Text label
    object Button : ViewType()          // Button with action
    object TextField : ViewType()       // Text input field
    object SecureField : ViewType()     // Password input field
    object Toggle : ViewType()          // Switch/Toggle control
    object Image : ViewType()           // Image view (system or asset)
    object AsyncImage : ViewType()      // Async image loading with placeholder
    object Label : ViewType()           // Text with icon

    // Input controls
    object Slider : ViewType()          // Range slider control
    object DatePicker : ViewType()      // Date selection control
    object Picker : ViewType()          // Selection picker
    object Stepper : ViewType()         // Increment/decrement stepper
    object ColorPicker : ViewType()     // Color selection

    // Presentation
    object Sheet : ViewType()           // Modal sheet presentation
    object Alert : ViewType()           // Alert dialog
    object Menu : ViewType()            // Context menu

    // Shapes
    object RoundedRectangle : ViewType()
    object Circle : ViewType()
    object Rectangle : ViewType()
    object Capsule : ViewType()

    // Special views
    object Spacer : ViewType()          // Flexible space
    object Divider : ViewType()         // Visual separator
    object EmptyView : ViewType()       // Empty placeholder
    object ProgressView : ViewType()    // Loading indicator

    // Flutter Parity - Material Components
    // Chips
    object FilterChip : ViewType()      // Selectable filter chip
    object InputChip : ViewType()       // Input chip with delete action
    object ActionChip : ViewType()      // Action chip (like button)
    object ChoiceChip : ViewType()      // Single-selection choice chip

    // Lists
    object ExpansionTile : ViewType()   // Expandable list tile
    object CheckboxListTile : ViewType() // List tile with checkbox
    object SwitchListTile : ViewType()  // List tile with switch

    // Buttons
    object FilledButton : ViewType()    // Material 3 filled button

    // Advanced
    object PopupMenu : ViewType()       // Popup menu button
    object CircleAvatar : ViewType()    // Circular avatar
    object RichText : ViewType()        // Multi-styled text
    object SelectableText : ViewType()  // Selectable text
    object VerticalDivider : ViewType() // Vertical divider
    object FadeInImage : ViewType()     // Image with fade-in
    object RefreshControl : ViewType()  // Pull to refresh
    object IndexedStack : ViewType()    // Stack showing single child by index

    // Editors
    object RichTextEditor : ViewType()  // Rich text editor with formatting toolbar
    object MarkdownEditor : ViewType()  // Markdown editor with live preview
    object CodeEditor : ViewType()      // Code editor with syntax highlighting

    // Custom view with dynamic type name
    data class Custom(val name: String) : ViewType()
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

        // Extended frame with min/max/ideal constraints (matches SwiftUI's full frame API)
        fun frame(
            minWidth: SizeValue? = null,
            idealWidth: SizeValue? = null,
            maxWidth: SizeValue? = null,
            minHeight: SizeValue? = null,
            idealHeight: SizeValue? = null,
            maxHeight: SizeValue? = null,
            alignment: ZStackAlignment = ZStackAlignment.Center
        ) = SwiftUIModifier(ModifierType.Frame, ExtendedFrameValue(
            minWidth, idealWidth, maxWidth, minHeight, idealHeight, maxHeight, alignment
        ))

        // Frame with Alignment (converts to ZStackAlignment)
        fun frame(width: SizeValue? = null, height: SizeValue? = null, alignment: Alignment) =
            frame(width, height, alignmentToZStack(alignment))

        // Layout priority
        fun layoutPriority(priority: Double) = SwiftUIModifier(ModifierType.Custom, "layoutPriority($priority)")

        // Environment modifier
        fun environment(key: String, value: Any) = SwiftUIModifier(ModifierType.Custom, "environment(\\.$key, $value)")

        // Helper to convert bridge Alignment to ZStackAlignment
        private fun alignmentToZStack(alignment: Alignment): ZStackAlignment = when (alignment) {
            Alignment.TopLeading -> ZStackAlignment.TopLeading
            Alignment.TopCenter -> ZStackAlignment.Top
            Alignment.TopTrailing -> ZStackAlignment.TopTrailing
            Alignment.CenterLeading -> ZStackAlignment.Leading
            Alignment.Center -> ZStackAlignment.Center
            Alignment.CenterTrailing -> ZStackAlignment.Trailing
            Alignment.BottomLeading -> ZStackAlignment.BottomLeading
            Alignment.BottomCenter -> ZStackAlignment.Bottom
            Alignment.BottomTrailing -> ZStackAlignment.BottomTrailing
        }

        // Appearance modifiers
        fun background(color: SwiftUIColor) = SwiftUIModifier(ModifierType.Background, color)
        fun foregroundColor(color: SwiftUIColor) = SwiftUIModifier(ModifierType.ForegroundColor, color)
        fun cornerRadius(value: Float) = SwiftUIModifier(ModifierType.CornerRadius, value)
        fun clipShape(shape: String) = SwiftUIModifier(ModifierType.Custom, "clipShape($shape)")
        fun shadow(radius: Float, x: Float = 0f, y: Float = 0f) =
            SwiftUIModifier(ModifierType.Shadow, ShadowValue(radius, x, y))
        fun shadow(color: SwiftUIColor, radius: Float, x: Float = 0f, y: Float = 0f) =
            SwiftUIModifier(ModifierType.Shadow, ShadowValueWithColor(color, radius, x, y))
        fun opacity(value: Float) = SwiftUIModifier(ModifierType.Opacity, value)

        // Text modifiers
        fun multilineTextAlignment(alignment: String) = SwiftUIModifier(ModifierType.Custom, "multilineTextAlignment($alignment)")
        fun strikethrough(active: Boolean = true, color: SwiftUIColor? = null) =
            if (color != null)
                SwiftUIModifier(ModifierType.Custom, "strikethrough($active, color: .${(color.value as? String) ?: "primary"})")
            else
                SwiftUIModifier(ModifierType.Custom, "strikethrough($active)")

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

        // Accent color (iOS tint color)
        fun accentColor(color: SwiftUIColor) = SwiftUIModifier(ModifierType.AccentColor, color)

        // Text field modifiers
        fun textFieldStyle(style: String) = SwiftUIModifier(ModifierType.TextFieldStyle, style)
        fun keyboardType(type: String) = SwiftUIModifier(ModifierType.KeyboardType, type)

        // Custom SwiftUI modifier (for raw SwiftUI modifier strings)
        fun custom(value: String) = SwiftUIModifier(ModifierType.Custom, value)

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
    AccentColor,
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
    AccessibilityHint,

    // Text input
    TextFieldStyle,
    KeyboardType,

    // Custom (raw SwiftUI modifier string)
    Custom
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

        /**
         * Create color from hex string (e.g., "#FF0000" or "FF0000")
         */
        fun hex(hexString: String): SwiftUIColor {
            val clean = hexString.removePrefix("#")
            val r = clean.substring(0, 2).toInt(16) / 255f
            val g = clean.substring(2, 4).toInt(16) / 255f
            val b = clean.substring(4, 6).toInt(16) / 255f
            val a = if (clean.length >= 8) clean.substring(6, 8).toInt(16) / 255f else 1f
            return rgb(r, g, b, a)
        }

        /**
         * Create color from RGBA values (0-255 range)
         */
        fun rgba(r: Int, g: Int, b: Int, a: Float = 1f) =
            rgb(r / 255f, g / 255f, b / 255f, a)

        // Semantic colors
        val primary = system("primary")
        val secondary = system("secondary")

        // Basic colors
        val clear = system("clear")
        val white = system("white")
        val black = system("black")

        // Standard colors
        val red = system("red")
        val green = system("green")
        val blue = system("blue")
        val orange = system("orange")
        val yellow = system("yellow")
        val purple = system("purple")
        val pink = system("pink")

        // System background colors
        val systemBackground = system("systemBackground")
        val systemGroupedBackground = system("systemGroupedBackground")

        // System separator colors
        val separator = system("separator")

        // System gray colors
        val systemGray = system("systemGray")
        val systemGray2 = system("systemGray2")
        val systemGray3 = system("systemGray3")
        val systemGray4 = system("systemGray4")
        val systemGray5 = system("systemGray5")
        val systemGray6 = system("systemGray6")

        // Accent color
        val accentColor = system("accentColor")

        // Text colors (semantic)
        val text = system("label")
        val secondaryLabel = system("secondaryLabel")
        val secondaryText = secondaryLabel  // Alias for convenience
        val tertiaryText = system("tertiaryLabel")
        val quaternaryText = system("quaternaryLabel")
    }

    /**
     * Create a new SwiftUIColor with modified opacity
     */
    fun withOpacity(opacity: Float): SwiftUIColor {
        return when (type) {
            ColorType.RGB -> {
                val rgbValue = value as? RGBValue
                if (rgbValue != null) {
                    SwiftUIColor(ColorType.RGB, RGBValue(rgbValue.red, rgbValue.green, rgbValue.blue, opacity))
                } else {
                    this
                }
            }
            else -> {
                // For system colors, we need to wrap with opacity modifier
                SwiftUIColor(ColorType.RGB, RGBValue(0f, 0f, 0f, opacity))
            }
        }
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
    data class AspectRatio(val ratio: Float) : SizeValue()
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
 * Extended frame modifier value with min/max/ideal constraints
 */
data class ExtendedFrameValue(
    val minWidth: SizeValue?,
    val idealWidth: SizeValue?,
    val maxWidth: SizeValue?,
    val minHeight: SizeValue?,
    val idealHeight: SizeValue?,
    val maxHeight: SizeValue?,
    val alignment: ZStackAlignment
)

/**
 * Shadow modifier value
 */
data class ShadowValue(
    val radius: Float,
    val x: Float = 0f,
    val y: Float = 0f
)

/**
 * Shadow modifier value with explicit color
 */
data class ShadowValueWithColor(
    val color: SwiftUIColor,
    val radius: Float,
    val x: Float = 0f,
    val y: Float = 0f
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

/**
 * SwiftUI action callback representation
 * Actions are identified by string IDs that map to handlers in Swift code
 */
data class SwiftUIAction(
    val id: String,
    val parameters: Map<String, Any> = emptyMap()
) {
    companion object {
        fun simple(id: String) = SwiftUIAction(id)
        fun withParams(id: String, vararg params: Pair<String, Any>) =
            SwiftUIAction(id, params.toMap())
    }
}

/**
 * SwiftUI styling presets for common visual patterns
 */
data class SwiftUIStyle(
    val name: String,
    val modifiers: List<SwiftUIModifier> = emptyList()
) {
    companion object {
        // Button styles
        val primary = SwiftUIStyle("primary", listOf(
            SwiftUIModifier.background(SwiftUIColor.primary),
            SwiftUIModifier.foregroundColor(SwiftUIColor.white),
            SwiftUIModifier.cornerRadius(8f)
        ))

        val secondary = SwiftUIStyle("secondary", listOf(
            SwiftUIModifier.background(SwiftUIColor.secondary),
            SwiftUIModifier.foregroundColor(SwiftUIColor.white),
            SwiftUIModifier.cornerRadius(8f)
        ))

        val outlined = SwiftUIStyle("outlined", listOf(
            SwiftUIModifier.border(SwiftUIColor.primary, 1f),
            SwiftUIModifier.foregroundColor(SwiftUIColor.primary),
            SwiftUIModifier.cornerRadius(8f)
        ))
    }
}

/**
 * SwiftUI padding helper with edge-specific presets
 */
data class SwiftUIPadding(
    val edges: PaddingEdges,
    val value: Float
) {
    enum class PaddingEdges {
        All,
        Top,
        Leading,
        Bottom,
        Trailing,
        Horizontal,
        Vertical
    }

    companion object {
        fun all(value: Float) = SwiftUIPadding(PaddingEdges.All, value)
        fun top(value: Float) = SwiftUIPadding(PaddingEdges.Top, value)
        fun leading(value: Float) = SwiftUIPadding(PaddingEdges.Leading, value)
        fun bottom(value: Float) = SwiftUIPadding(PaddingEdges.Bottom, value)
        fun trailing(value: Float) = SwiftUIPadding(PaddingEdges.Trailing, value)
        fun horizontal(value: Float) = SwiftUIPadding(PaddingEdges.Horizontal, value)
        fun vertical(value: Float) = SwiftUIPadding(PaddingEdges.Vertical, value)
    }
}

/**
 * SwiftUI frame helper with convenience constructors
 */
data class SwiftUIFrame(
    val width: SizeValue? = null,
    val height: SizeValue? = null,
    val minWidth: SizeValue? = null,
    val maxWidth: SizeValue? = null,
    val minHeight: SizeValue? = null,
    val maxHeight: SizeValue? = null,
    val alignment: ZStackAlignment = ZStackAlignment.Center
) {
    companion object {
        fun fixed(width: Float, height: Float, alignment: ZStackAlignment = ZStackAlignment.Center) =
            SwiftUIFrame(
                width = SizeValue.Fixed(width),
                height = SizeValue.Fixed(height),
                alignment = alignment
            )

        fun fillMaxWidth(alignment: ZStackAlignment = ZStackAlignment.Center) =
            SwiftUIFrame(maxWidth = SizeValue.Infinity, alignment = alignment)

        fun fillMaxHeight(alignment: ZStackAlignment = ZStackAlignment.Center) =
            SwiftUIFrame(maxHeight = SizeValue.Infinity, alignment = alignment)

        fun fillMaxSize(alignment: ZStackAlignment = ZStackAlignment.Center) =
            SwiftUIFrame(
                maxWidth = SizeValue.Infinity,
                maxHeight = SizeValue.Infinity,
                alignment = alignment
            )
    }
}

/**
 * Image content scaling modes (maps to SwiftUI ContentMode)
 */
enum class BoxFit {
    Fill,      // Scales to fill entire view, may clip
    Contain,   // Scales to fit entire content, may letterbox (fit)
    Cover,     // Scales to fill entire view, maintains aspect (fill)
    FitWidth,  // Scales to match width
    FitHeight, // Scales to match height
    None       // Original size, no scaling
}

/**
 * General alignment options (combines common alignments)
 */
enum class Alignment {
    TopLeading,
    TopCenter,
    TopTrailing,
    CenterLeading,
    Center,
    CenterTrailing,
    BottomLeading,
    BottomCenter,
    BottomTrailing
}
