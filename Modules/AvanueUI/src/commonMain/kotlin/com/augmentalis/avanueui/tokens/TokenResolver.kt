package com.augmentalis.avanueui.tokens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.augmentalis.avanueui.theme.AvanueColorScheme

/**
 * Unified resolver facade for AVUDSL runtime.
 * Resolves string token IDs (e.g., "spacing.md") to concrete values.
 */
object TokenResolver {

    fun resolveSpacing(id: String): Dp? = SpacingTokens.resolve(id)
    fun resolveShape(id: String): Dp? = ShapeTokens.resolve(id)
    fun resolveElevation(id: String): Dp? = ElevationTokens.resolve(id)
    fun resolveTypography(id: String): TextStyle? = TypographyTokens.resolve(id)
    fun resolveAnimation(id: String): Int? = AnimationTokens.resolve(id)
    fun resolveColor(id: String, scheme: AvanueColorScheme): Color? = scheme.resolve(id)
    fun resolveSize(id: String): Dp? = SizeTokens.resolve(id)
    fun resolveGlass(id: String): Any? = GlassTokens.resolve(id)
    fun resolveWater(id: String): Any? = WaterTokens.resolve(id)

    fun resolve(fullId: String, colorScheme: AvanueColorScheme): Any? {
        val dotIndex = fullId.indexOf('.')
        if (dotIndex < 0) return null
        val category = fullId.substring(0, dotIndex)
        return when (category) {
            "spacing" -> resolveSpacing(fullId)
            "shape" -> resolveShape(fullId)
            "elevation" -> resolveElevation(fullId)
            "typography" -> resolveTypography(fullId)
            "animation" -> resolveAnimation(fullId)
            "color" -> resolveColor(fullId, colorScheme)
            "size" -> resolveSize(fullId)
            "glass" -> resolveGlass(fullId)
            "water" -> resolveWater(fullId)
            else -> null
        }
    }
}
