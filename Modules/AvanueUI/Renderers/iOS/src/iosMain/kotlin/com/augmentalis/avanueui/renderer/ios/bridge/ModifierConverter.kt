package com.augmentalis.avanueui.renderer.ios.bridge

import com.augmentalis.avanueui.core.*

/**
 * Converts AvaElements modifiers to SwiftUI modifiers
 *
 * Handles the translation of cross-platform modifiers to iOS-specific
 * SwiftUI modifier chains.
 */
object ModifierConverter {

    /**
     * Convert a list of AvaElements modifiers to SwiftUI modifiers
     */
    fun convert(modifiers: List<Modifier>, theme: Theme?): List<SwiftUIModifier> {
        return modifiers.flatMap { convertModifier(it, theme) }
    }

    /**
     * Convert a single modifier (may produce multiple SwiftUI modifiers)
     */
    private fun convertModifier(modifier: Modifier, theme: Theme?): List<SwiftUIModifier> {
        return when (modifier) {
            is Modifier.Padding -> convertPadding(modifier)
            is Modifier.Background -> listOf(convertBackground(modifier))
            is Modifier.BackgroundGradient -> convertBackgroundGradient(modifier)
            is Modifier.Border -> convertBorder(modifier)
            is Modifier.CornerRadius -> listOf(convertCornerRadius(modifier))
            is Modifier.Shadow -> listOf(convertShadow(modifier))
            is Modifier.Opacity -> listOf(SwiftUIModifier.opacity(modifier.value))
            is Modifier.Size -> convertSize(modifier)
            Modifier.FillMaxWidth -> listOf(SwiftUIModifier.fillMaxWidth())
            Modifier.FillMaxHeight -> listOf(SwiftUIModifier.fillMaxHeight())
            Modifier.FillMaxSize -> listOf(SwiftUIModifier.fillMaxSize())
            is Modifier.Clip -> convertClip(modifier)
            is Modifier.Transform -> convertTransform(modifier)
            is Modifier.Align -> convertAlign(modifier)
            is Modifier.Weight -> emptyList() // Handled by parent layout
            is Modifier.ZIndex -> emptyList() // Handled by parent ZStack
            is Modifier.Clickable -> emptyList() // Handled by component itself
            is Modifier.Hoverable -> emptyList() // Handled by component itself
            is Modifier.Focusable -> emptyList() // Handled by component itself
            is Modifier.Animated -> emptyList() // Handled separately
        }
    }

    private fun convertPadding(modifier: Modifier.Padding): List<SwiftUIModifier> {
        val spacing = modifier.spacing
        return if (spacing.top == spacing.right &&
                   spacing.right == spacing.bottom &&
                   spacing.bottom == spacing.left) {
            // Uniform padding
            listOf(SwiftUIModifier.padding(spacing.top))
        } else {
            // Edge-specific padding
            listOf(SwiftUIModifier.padding(
                spacing.top,
                spacing.left,  // leading
                spacing.bottom,
                spacing.right  // trailing
            ))
        }
    }

    private fun convertBackground(modifier: Modifier.Background): SwiftUIModifier {
        val color = convertColor(modifier.color)
        return SwiftUIModifier.background(color)
    }

    private fun convertBackgroundGradient(modifier: Modifier.BackgroundGradient): List<SwiftUIModifier> {
        // For now, use first color as fallback
        // Full gradient support would require additional SwiftUI bridge models
        val firstColor = when (val gradient = modifier.gradient) {
            is Gradient.Linear -> gradient.colors.firstOrNull()?.color
            is Gradient.Radial -> gradient.colors.firstOrNull()?.color
        }
        return if (firstColor != null) {
            listOf(SwiftUIModifier.background(convertColor(firstColor)))
        } else {
            emptyList()
        }
    }

    private fun convertBorder(modifier: Modifier.Border): List<SwiftUIModifier> {
        val border = modifier.border
        val modifiers = mutableListOf<SwiftUIModifier>()

        // Add border
        modifiers.add(SwiftUIModifier.border(
            convertColor(border.color),
            border.width
        ))

        // Add corner radius if specified
        if (border.radius != CornerRadius.Zero) {
            modifiers.add(convertCornerRadius(Modifier.CornerRadius(border.radius)))
        }

        return modifiers
    }

    private fun convertCornerRadius(modifier: Modifier.CornerRadius): SwiftUIModifier {
        val radius = modifier.radius
        // SwiftUI cornerRadius uses a single value, take the maximum
        val maxRadius = maxOf(
            radius.topLeft,
            radius.topRight,
            radius.bottomLeft,
            radius.bottomRight
        )
        return SwiftUIModifier.cornerRadius(maxRadius)
    }

    private fun convertShadow(modifier: Modifier.Shadow): SwiftUIModifier {
        val shadow = modifier.shadow
        return SwiftUIModifier.shadow(
            radius = shadow.blurRadius,
            x = shadow.offsetX,
            y = shadow.offsetY
        )
    }

    private fun convertSize(modifier: Modifier.Size): List<SwiftUIModifier> {
        val width = modifier.width?.let { convertSizeValue(it) }
        val height = modifier.height?.let { convertSizeValue(it) }

        return if (width != null || height != null) {
            listOf(SwiftUIModifier.frame(width, height))
        } else {
            emptyList()
        }
    }

    private fun convertSizeValue(size: Size): SizeValue {
        return when (size) {
            is Size.Fixed -> SizeValue.Fixed(size.value)
            Size.Auto -> SizeValue.Ideal
            Size.Fill -> SizeValue.Infinity
        }
    }

    private fun convertClip(modifier: Modifier.Clip): List<SwiftUIModifier> {
        // SwiftUI clipping is done via clipShape or cornerRadius
        return when (val shape = modifier.shape) {
            is Modifier.ClipShape.Rectangle -> {
                if (shape.radius != CornerRadius.Zero) {
                    listOf(convertCornerRadius(Modifier.CornerRadius(shape.radius)))
                } else {
                    emptyList()
                }
            }
            is Modifier.ClipShape.RoundedRectangle -> {
                listOf(SwiftUIModifier.cornerRadius(shape.radius))
            }
            Modifier.ClipShape.Circle -> {
                // Circle clipping in SwiftUI
                // Would need additional support in SwiftUIModels
                emptyList()
            }
        }
    }

    private fun convertTransform(modifier: Modifier.Transform): List<SwiftUIModifier> {
        // Transforms in SwiftUI require additional modifier types
        // For now, return empty (would need rotationEffect, scaleEffect, offset modifiers)
        return emptyList()
    }

    private fun convertAlign(modifier: Modifier.Align): List<SwiftUIModifier> {
        // Alignment is typically handled by parent container in SwiftUI
        // Could be converted to frame alignment
        return emptyList()
    }

    /**
     * Convert AvaElements Color to SwiftUIColor
     */
    fun convertColor(color: Color): SwiftUIColor {
        // Convert RGB 0-255 to 0-1 range for SwiftUI
        return SwiftUIColor.rgb(
            red = color.red / 255f,
            green = color.green / 255f,
            blue = color.blue / 255f,
            opacity = color.alpha
        )
    }

    /**
     * Convert AvaElements Alignment to SwiftUI alignment
     */
    fun convertAlignment(alignment: Alignment): ZStackAlignment {
        return when (alignment) {
            Alignment.TopStart -> ZStackAlignment.TopLeading
            Alignment.TopCenter -> ZStackAlignment.Top
            Alignment.TopEnd -> ZStackAlignment.TopTrailing
            Alignment.CenterStart -> ZStackAlignment.Leading
            Alignment.Center -> ZStackAlignment.Center
            Alignment.CenterEnd -> ZStackAlignment.Trailing
            Alignment.BottomStart -> ZStackAlignment.BottomLeading
            Alignment.BottomCenter -> ZStackAlignment.Bottom
            Alignment.BottomEnd -> ZStackAlignment.BottomTrailing
        }
    }

    /**
     * Convert Alignment to HorizontalAlignment (for VStack)
     */
    fun toHorizontalAlignment(alignment: Alignment): HorizontalAlignment {
        return when (alignment) {
            Alignment.TopStart, Alignment.CenterStart, Alignment.BottomStart ->
                HorizontalAlignment.Leading
            Alignment.TopCenter, Alignment.Center, Alignment.BottomCenter ->
                HorizontalAlignment.Center
            Alignment.TopEnd, Alignment.CenterEnd, Alignment.BottomEnd ->
                HorizontalAlignment.Trailing
        }
    }

    /**
     * Convert Alignment to VerticalAlignment (for HStack)
     */
    fun toVerticalAlignment(alignment: Alignment): VerticalAlignment {
        return when (alignment) {
            Alignment.TopStart, Alignment.TopCenter, Alignment.TopEnd ->
                VerticalAlignment.Top
            Alignment.CenterStart, Alignment.Center, Alignment.CenterEnd ->
                VerticalAlignment.Center
            Alignment.BottomStart, Alignment.BottomCenter, Alignment.BottomEnd ->
                VerticalAlignment.Bottom
        }
    }

    /**
     * Convert AvaElements Font to SwiftUI FontStyle
     */
    fun convertFontStyle(font: Font): FontStyle {
        // Map based on size ranges
        return when {
            font.size >= 34f -> FontStyle.LargeTitle
            font.size >= 28f -> FontStyle.Title
            font.size >= 22f -> FontStyle.Title2
            font.size >= 20f -> FontStyle.Title3
            font.size >= 17f && font.weight >= Font.Weight.SemiBold -> FontStyle.Headline
            font.size >= 15f -> FontStyle.Subheadline
            font.size >= 14f -> FontStyle.Body
            font.size >= 12f -> FontStyle.Footnote
            font.size >= 11f -> FontStyle.Caption
            else -> FontStyle.Caption2
        }
    }

    /**
     * Convert AvaElements Font.Weight to SwiftUI FontWeight
     */
    fun convertFontWeight(weight: Font.Weight): FontWeight {
        return when (weight) {
            Font.Weight.Thin -> FontWeight.Thin
            Font.Weight.ExtraLight -> FontWeight.UltraLight
            Font.Weight.Light -> FontWeight.Light
            Font.Weight.Regular -> FontWeight.Regular
            Font.Weight.Medium -> FontWeight.Medium
            Font.Weight.SemiBold -> FontWeight.Semibold
            Font.Weight.Bold -> FontWeight.Bold
            Font.Weight.ExtraBold -> FontWeight.Heavy
            Font.Weight.Black -> FontWeight.Black
        }
    }

    /**
     * Convert AvaElements Arrangement to spacing value
     */
    fun convertArrangementToSpacing(arrangement: Arrangement): Float? {
        return when (arrangement) {
            Arrangement.Start, Arrangement.End -> 0f
            Arrangement.Center -> null // Let SwiftUI handle centering
            Arrangement.SpaceBetween, Arrangement.SpaceAround, Arrangement.SpaceEvenly -> null
        }
    }
}
