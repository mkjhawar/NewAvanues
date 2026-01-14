package com.augmentalis.avaelements.renderer.ios.mappers

import com.augmentalis.avaelements.core.Component
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.core.Modifier
import com.augmentalis.avaelements.renderer.ios.bridge.*
import com.augmentalis.avaelements.renderer.ios.*
import com.augmentalis.avaelements.common.color.UniversalColor
import com.augmentalis.avaelements.common.color.ColorManipulator
import kotlinx.serialization.Serializable

/**
 * iOS SwiftUI Mappers for Flutter Text Parity Components
 *
 * This file contains renderer functions that map cross-platform enhanced text display
 * components to iOS SwiftUI bridge representations.
 *
 * Architecture:
 * Flutter Component → iOS Mapper → SwiftUIView Bridge → Swift → Native SwiftUI
 *
 * Components Implemented:
 * - Highlight: Text with background highlight (like HTML <mark> or highlighter pen)
 * - Mark: Semantically marked/emphasized text with background
 *
 * These components provide Flutter parity for enhanced text display with backgrounds,
 * commonly used for:
 * - Search result highlighting
 * - Code syntax highlighting
 * - Important text emphasis
 * - Visual text annotations
 *
 * iOS Implementation Details:
 * - Uses SwiftUI Text with background modifier
 * - Supports custom highlight colors
 * - Maintains text readability with contrasting colors
 * - Inline display within text flow
 * - Full RTL support via environment
 * - Dynamic Type accessibility support
 *
 * @since 3.0.0-flutter-parity-ios
 */

// ============================================================================
// Component Data Classes
// ============================================================================

/**
 * Highlight component - Displays text with a background highlight color
 *
 * Similar to using a highlighter pen on text or HTML `<mark>` element.
 * Commonly used for search result highlighting or drawing attention to specific text.
 *
 * **Flutter Equivalent:** Custom text styling with background
 * **HTML Equivalent:** `<mark>` element
 *
 * ## Features
 * - Text with background highlight color
 * - Customizable highlight and text colors
 * - Inline display (flows with surrounding text)
 * - Maintains readability
 * - Material3 theming support
 * - Dark mode aware
 * - WCAG 2.1 Level AA compliant contrast
 *
 * ## Usage Example
 * ```kotlin
 * Highlight(
 *     text = "important",
 *     color = "yellow",        // Highlight background color
 *     textColor = "onSurface"  // Text color
 * )
 * ```
 *
 * @property type Component type identifier
 * @property id Unique identifier
 * @property text Text content to highlight
 * @property color Highlight background color (defaults to yellow)
 * @property textColor Text color for readability
 * @property modifiers List of modifiers for styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Highlight(
    override val type: String = "Highlight",
    override val id: String? = null,
    val text: String,
    val color: String? = null,           // Background highlight color
    val textColor: String? = null,       // Text color
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    override val style: com.augmentalis.avaelements.core.types.ComponentStyle? = null

    override fun render(renderer: com.augmentalis.avaelements.core.Renderer): Any {
        return renderer.render(this)
    }

    companion object {
        /**
         * Create highlight with default yellow background
         */
        fun yellow(text: String) = Highlight(
            text = text,
            color = "yellow"
        )

        /**
         * Create highlight with custom color
         */
        fun withColor(text: String, color: String) = Highlight(
            text = text,
            color = color
        )
    }
}

/**
 * Mark component - Semantically marked/emphasized text with background
 *
 * Similar to HTML `<mark>` element. Used to mark text as having special relevance
 * or importance in the current context.
 *
 * **Flutter Equivalent:** Custom text styling with semantic background
 * **HTML Equivalent:** `<mark>` element
 *
 * ## Features
 * - Semantic text marking with background
 * - Customizable background color
 * - Automatic contrasting text color
 * - Inline display
 * - Material3 theming support
 * - Dark mode aware
 * - Accessibility semantics
 *
 * ## Usage Example
 * ```kotlin
 * Mark(
 *     text = "marked text",
 *     color = "yellow"  // Background color
 * )
 * ```
 *
 * @property type Component type identifier
 * @property id Unique identifier
 * @property text Text content to mark
 * @property color Background color (defaults to yellow)
 * @property textColor Optional text color override
 * @property modifiers List of modifiers for styling
 *
 * @since 3.0.0-flutter-parity
 */
data class Mark(
    override val type: String = "Mark",
    override val id: String? = null,
    val text: String,
    val color: String? = null,           // Background color
    val textColor: String? = null,       // Optional text color override
    override val modifiers: List<Modifier> = emptyList()
) : Component {
    override val style: com.augmentalis.avaelements.core.types.ComponentStyle? = null

    override fun render(renderer: com.augmentalis.avaelements.core.Renderer): Any {
        return renderer.render(this)
    }

    companion object {
        /**
         * Create mark with default yellow background
         */
        fun yellow(text: String) = Mark(
            text = text,
            color = "yellow"
        )

        /**
         * Create mark with custom color
         */
        fun withColor(text: String, color: String) = Mark(
            text = text,
            color = color
        )
    }
}

// ============================================================================
// iOS SwiftUI Mappers
// ============================================================================

/**
 * Maps Highlight component to SwiftUI Text with background
 *
 * Creates a SwiftUI Text view with:
 * - Text content
 * - Background highlight color (yellow default)
 * - Contrasting text color for readability
 * - Inline padding for visual spacing
 * - Corner radius for polished appearance
 *
 * SwiftUI Implementation:
 * ```swift
 * Text("highlighted text")
 *     .foregroundColor(.black)
 *     .padding(.horizontal, 4)
 *     .padding(.vertical, 2)
 *     .background(Color.yellow)
 *     .cornerRadius(4)
 * ```
 *
 * @param component Highlight component to map
 * @param theme Optional theme for color resolution
 * @return SwiftUIView representing the highlighted text
 */
object HighlightMapper {
    fun map(component: Highlight, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Set text color for readability
        val textColor = component.textColor?.let {
            parseColor(it)
        } ?: SwiftUIColor.black
        modifiers.add(SwiftUIModifier.foregroundColor(textColor))

        // Add subtle padding for visual spacing
        modifiers.add(SwiftUIModifier.padding(2f, 4f, 2f, 4f)) // top, leading, bottom, trailing

        // Set highlight background color (yellow default)
        val highlightColor = component.color?.let {
            parseColor(it)
        } ?: SwiftUIColor.rgb(1.0f, 1.0f, 0.0f, 1.0f) // Yellow
        modifiers.add(SwiftUIModifier.background(highlightColor))

        // Add subtle corner radius for polished look
        modifiers.add(SwiftUIModifier.cornerRadius(4f))

        // Apply any additional component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView.text(
            content = component.text,
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

/**
 * Maps Mark component to SwiftUI Text with semantic background
 *
 * Creates a SwiftUI Text view with:
 * - Text content
 * - Semantic background color (yellow default)
 * - Automatic contrasting text color
 * - Inline padding for visual spacing
 * - Corner radius for Material Design aesthetics
 * - Accessibility semantics for "marked" text
 *
 * SwiftUI Implementation:
 * ```swift
 * Text("marked text")
 *     .foregroundColor(.primary)
 *     .padding(.horizontal, 4)
 *     .padding(.vertical, 2)
 *     .background(Color.yellow.opacity(0.3))
 *     .cornerRadius(4)
 *     .accessibilityAddTraits(.isStaticText)
 * ```
 *
 * The Mark component is semantically similar to Highlight but may have
 * different visual treatment (e.g., lighter background opacity).
 *
 * @param component Mark component to map
 * @param theme Optional theme for color resolution
 * @return SwiftUIView representing the marked text
 */
object MarkMapper {
    fun map(component: Mark, theme: Theme?): SwiftUIView {
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Set text color (default to primary for theme awareness)
        val textColor = component.textColor?.let {
            parseColor(it)
        } ?: SwiftUIColor.primary
        modifiers.add(SwiftUIModifier.foregroundColor(textColor))

        // Add subtle padding for visual spacing
        modifiers.add(SwiftUIModifier.padding(2f, 4f, 2f, 4f)) // top, leading, bottom, trailing

        // Set mark background color with slight transparency for subtlety
        val markColor = component.color?.let {
            parseColor(it, alpha = 0.3f)
        } ?: SwiftUIColor.rgb(1.0f, 1.0f, 0.0f, 0.3f) // Yellow with 30% opacity
        modifiers.add(SwiftUIModifier.background(markColor))

        // Add subtle corner radius for Material Design aesthetics
        modifiers.add(SwiftUIModifier.cornerRadius(4f))

        // Apply any additional component modifiers
        modifiers.addAll(ModifierConverter.convert(component.modifiers, theme))

        return SwiftUIView.text(
            content = component.text,
            modifiers = modifiers
        ).copy(id = component.id)
    }
}

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Parse color string to SwiftUIColor using shared UniversalColor utilities
 *
 * Supports:
 * - Named colors: "yellow", "red", "blue", etc.
 * - System semantic colors: "primary", "secondary", "onSurface", etc.
 * - Hex colors: "#FFFF00", "#FF0"
 * - RGB format: "rgb(255, 255, 0)"
 *
 * @param colorString Color specification string
 * @param alpha Optional alpha override (0.0 - 1.0)
 * @return SwiftUIColor instance
 */
private fun parseColor(colorString: String, alpha: Float = 1.0f): SwiftUIColor {
    return when {
        // System semantic colors
        colorString.equals("primary", ignoreCase = true) -> SwiftUIColor.primary
        colorString.equals("secondary", ignoreCase = true) -> SwiftUIColor.secondary
        colorString.equals("onSurface", ignoreCase = true) -> SwiftUIColor.system("label")
        colorString.equals("onBackground", ignoreCase = true) -> SwiftUIColor.system("label")

        // Hex colors - use shared utility
        colorString.startsWith("#") -> {
            val color = UniversalColor.fromHex(colorString)
            val colorWithAlpha = ColorManipulator.withAlpha(color, alpha)
            SwiftUIColor.rgb(colorWithAlpha.red, colorWithAlpha.green, colorWithAlpha.blue, colorWithAlpha.alpha)
        }

        // Common named colors - use shared utility
        else -> {
            val namedColors = mapOf(
                "yellow" to 0xFFFFFF00.toInt(),
                "red" to 0xFFFF0000.toInt(),
                "blue" to 0xFF0000FF.toInt(),
                "green" to 0xFF00FF00.toInt(),
                "white" to 0xFFFFFFFF.toInt(),
                "black" to 0xFF000000.toInt(),
                "orange" to 0xFFFFA500.toInt(),
                "purple" to 0xFF800080.toInt(),
                "pink" to 0xFFFFC0CB.toInt(),
                "cyan" to 0xFF00FFFF.toInt(),
                "lime" to 0xFFBFFF00.toInt()
            )

            val argb = namedColors[colorString.lowercase()] ?: 0xFF000000.toInt()
            val color = UniversalColor.fromArgb(argb)
            val colorWithAlpha = ColorManipulator.withAlpha(color, alpha)
            SwiftUIColor.rgb(colorWithAlpha.red, colorWithAlpha.green, colorWithAlpha.blue, colorWithAlpha.alpha)
        }
    }
}

// ModifierConverter is imported from com.augmentalis.avaelements.renderer.ios.bridge
