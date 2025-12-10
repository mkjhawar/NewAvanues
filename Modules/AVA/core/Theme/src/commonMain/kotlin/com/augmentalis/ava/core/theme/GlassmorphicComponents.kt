// filename: common/core/Theme/src/commonMain/kotlin/com/augmentalis/ava/core/theme/GlassmorphicComponents.kt
// created: 2025-12-03
// author: AVA AI Team
// (c) Augmentalis Inc, Intelligent Devices LLC

package com.augmentalis.ava.core.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Glassmorphic Components for AVA UI
 *
 * This module provides Material3-based components with glassmorphic styling.
 * Designed for easy migration to MagicUI when available.
 *
 * Migration path:
 * - GlassSurface -> MagicUI.Surface
 * - GlassCard -> MagicUI.Card
 * - GlassBubble -> MagicUI.ChatBubble
 * - OceanButton -> MagicUI.Button
 * - GlassTextField -> MagicUI.TextField
 * - GlassChip -> MagicUI.Chip
 * - GlassIndicator -> MagicUI.Indicator
 * - OceanGradientBackground -> MagicUI.GradientBackground
 *
 * Ocean Theme Colors:
 * - Surface: 10-30% white opacity (GlassLight to GlassDense)
 * - Border: 10-20% white opacity
 * - Accent: CoralBlue (#3B82F6), SeafoamGreen (#10B981), CoralRed (#EF4444)
 *
 * @author AVA AI Team
 * @version 1.0.0
 */

// ===== GLASS SURFACE VARIANTS =====

/**
 * Glassmorphic surface intensity levels.
 * Controls opacity and blur effect intensity.
 */
enum class GlassIntensity {
    ULTRA_LIGHT,  // 5% white - very subtle
    LIGHT,        // 10% white - default for surfaces
    MEDIUM,       // 15% white - cards and containers
    HEAVY,        // 20% white - elevated surfaces
    DENSE         // 30% white - prominent elements
}

/**
 * Get the glass color for a given intensity.
 */
@Composable
fun GlassIntensity.toColor(): Color = when (this) {
    GlassIntensity.ULTRA_LIGHT -> ColorTokens.GlassUltraLight
    GlassIntensity.LIGHT -> ColorTokens.GlassLight
    GlassIntensity.MEDIUM -> ColorTokens.GlassMedium
    GlassIntensity.HEAVY -> ColorTokens.GlassHeavy
    GlassIntensity.DENSE -> ColorTokens.GlassDense
}

// ===== GLASS SURFACE =====

/**
 * Glassmorphic surface component.
 *
 * A semi-transparent surface with optional border that follows the Ocean theme.
 * Use this as a base for containers, panels, and backgrounds.
 *
 * @param modifier Modifier for the surface
 * @param intensity Glass opacity level (LIGHT to DENSE)
 * @param shape Shape of the surface corners
 * @param showBorder Whether to show a subtle border
 * @param borderColor Color for the border (defaults to 20% white)
 * @param content Content to display inside the surface
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.LIGHT,
    shape: Shape = RoundedCornerShape(ShapeTokens.Medium),
    showBorder: Boolean = true,
    borderColor: Color = ColorTokens.Outline,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(intensity.toColor())
            .then(
                if (showBorder) {
                    Modifier.border(
                        width = 1.dp,
                        color = borderColor,
                        shape = shape
                    )
                } else Modifier
            ),
        content = content
    )
}

// ===== GLASS CARD =====

/**
 * Glassmorphic card component.
 *
 * A card with glass effect, perfect for content containers, list items,
 * and elevated UI elements.
 *
 * @param modifier Modifier for the card
 * @param intensity Glass opacity level
 * @param shape Corner shape
 * @param showBorder Show subtle border
 * @param onClick Optional click handler (makes card clickable)
 * @param content Card content
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.MEDIUM,
    shape: Shape = RoundedCornerShape(ShapeTokens.Large),
    showBorder: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier
        .clip(shape)
        .background(intensity.toColor())
        .then(
            if (showBorder) {
                Modifier.border(
                    width = 1.dp,
                    color = ColorTokens.Outline,
                    shape = shape
                )
            } else Modifier
        )

    if (onClick != null) {
        Surface(
            modifier = cardModifier,
            onClick = onClick,
            shape = shape,
            color = Color.Transparent
        ) {
            Column(content = content)
        }
    } else {
        Column(
            modifier = cardModifier,
            content = content
        )
    }
}

// ===== GLASS BUBBLE (Chat Messages) =====

/**
 * Glassmorphic chat bubble component.
 *
 * Specialized for chat messages with user/assistant styling variants.
 *
 * @param modifier Modifier for the bubble
 * @param isUser Whether this is a user message (affects styling)
 * @param maxWidth Maximum width of the bubble
 * @param content Bubble content
 */
@Composable
fun GlassBubble(
    modifier: Modifier = Modifier,
    isUser: Boolean = false,
    maxWidth: Dp = SizeTokens.ChatBubbleMaxWidth,
    content: @Composable BoxScope.() -> Unit
) {
    val backgroundColor = if (isUser) {
        // User bubbles: Primary color with slight transparency
        MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
    } else {
        // Assistant bubbles: Glass effect
        ColorTokens.GlassMedium
    }

    val shape = RoundedCornerShape(
        topStart = ShapeTokens.Large,
        topEnd = ShapeTokens.Large,
        bottomStart = if (isUser) ShapeTokens.Large else ShapeTokens.ExtraSmall,
        bottomEnd = if (isUser) ShapeTokens.ExtraSmall else ShapeTokens.Large
    )

    Box(
        modifier = modifier
            .widthIn(max = maxWidth)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (!isUser) {
                    Modifier.border(
                        width = 1.dp,
                        color = ColorTokens.OutlineVariant,
                        shape = shape
                    )
                } else Modifier
            )
            .padding(SpacingTokens.Medium),
        content = content
    )
}

// ===== OCEAN BUTTONS =====

/**
 * Ocean theme button variants.
 */
enum class OceanButtonStyle {
    PRIMARY,      // Teal accent, high emphasis
    SECONDARY,    // Glass background, medium emphasis
    TERTIARY,     // Text only, low emphasis
    ERROR         // Red accent, destructive actions
}

/**
 * Ocean-themed button component.
 *
 * Button with glassmorphic styling following Ocean theme.
 *
 * @param onClick Click handler
 * @param modifier Modifier for the button
 * @param style Button style variant
 * @param enabled Whether button is enabled
 * @param content Button content (text, icon, etc.)
 */
@Composable
fun OceanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OceanButtonStyle = OceanButtonStyle.PRIMARY,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    when (style) {
        OceanButtonStyle.PRIMARY -> {
            Button(
                onClick = onClick,
                modifier = modifier.heightIn(min = SizeTokens.MinTouchTarget),
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.Primary,
                    contentColor = ColorTokens.OnPrimary,
                    disabledContainerColor = ColorTokens.Primary.copy(alpha = 0.38f),
                    disabledContentColor = ColorTokens.OnPrimary.copy(alpha = 0.38f)
                ),
                content = content
            )
        }
        OceanButtonStyle.SECONDARY -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier.heightIn(min = SizeTokens.MinTouchTarget),
                enabled = enabled,
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = ColorTokens.GlassMedium,
                    contentColor = ColorTokens.TextPrimary,
                    disabledContainerColor = ColorTokens.GlassLight,
                    disabledContentColor = ColorTokens.TextDisabled
                ),
                content = content
            )
        }
        OceanButtonStyle.TERTIARY -> {
            TextButton(
                onClick = onClick,
                modifier = modifier.heightIn(min = SizeTokens.MinTouchTarget),
                enabled = enabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = ColorTokens.Primary,
                    disabledContentColor = ColorTokens.TextDisabled
                ),
                content = content
            )
        }
        OceanButtonStyle.ERROR -> {
            Button(
                onClick = onClick,
                modifier = modifier.heightIn(min = SizeTokens.MinTouchTarget),
                enabled = enabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ColorTokens.Error,
                    contentColor = ColorTokens.OnError,
                    disabledContainerColor = ColorTokens.Error.copy(alpha = 0.38f),
                    disabledContentColor = ColorTokens.OnError.copy(alpha = 0.38f)
                ),
                content = content
            )
        }
    }
}

// ===== GLASS TEXT FIELD =====

/**
 * Glassmorphic text field component.
 *
 * Text input with glass background and Ocean theme styling.
 *
 * @param value Current text value
 * @param onValueChange Value change callback
 * @param modifier Modifier for the field
 * @param placeholder Placeholder text
 * @param enabled Whether field is enabled
 * @param singleLine Whether to limit to single line
 * @param maxLines Maximum lines for multiline
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        enabled = enabled,
        singleLine = singleLine,
        maxLines = maxLines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = ColorTokens.GlassLight,
            unfocusedContainerColor = ColorTokens.GlassUltraLight,
            disabledContainerColor = ColorTokens.GlassUltraLight.copy(alpha = 0.5f),
            focusedBorderColor = ColorTokens.Primary,
            unfocusedBorderColor = ColorTokens.Outline,
            focusedTextColor = ColorTokens.TextPrimary,
            unfocusedTextColor = ColorTokens.TextPrimary,
            cursorColor = ColorTokens.Primary,
            focusedPlaceholderColor = ColorTokens.TextHint,
            unfocusedPlaceholderColor = ColorTokens.TextHint
        ),
        shape = RoundedCornerShape(ShapeTokens.Medium)
    )
}

// ===== GLASS CHIP =====

/**
 * Glassmorphic chip/badge component.
 *
 * Small label with glass effect for tags, status indicators, etc.
 *
 * @param modifier Modifier for the chip
 * @param onClick Optional click handler
 * @param leadingIcon Optional leading icon
 * @param label Chip label content
 */
@Composable
fun GlassChip(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    label: @Composable () -> Unit
) {
    if (onClick != null) {
        AssistChip(
            onClick = onClick,
            label = label,
            modifier = modifier.heightIn(min = 32.dp),
            leadingIcon = leadingIcon,
            colors = AssistChipDefaults.assistChipColors(
                containerColor = ColorTokens.GlassMedium,
                labelColor = ColorTokens.TextPrimary,
                leadingIconContentColor = ColorTokens.Primary
            ),
            border = AssistChipDefaults.assistChipBorder(
                enabled = true,
                borderColor = ColorTokens.OutlineVariant
            )
        )
    } else {
        Surface(
            modifier = modifier
                .clip(RoundedCornerShape(ShapeTokens.Full))
                .background(ColorTokens.GlassMedium)
                .border(
                    width = 1.dp,
                    color = ColorTokens.OutlineVariant,
                    shape = RoundedCornerShape(ShapeTokens.Full)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp),
            color = Color.Transparent
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                leadingIcon?.invoke()
                label()
            }
        }
    }
}

// ===== GLASS INDICATOR =====

/**
 * Glassmorphic status indicator bar.
 *
 * Full-width indicator for status messages, RAG mode, etc.
 *
 * @param modifier Modifier for the indicator
 * @param intensity Glass opacity level
 * @param content Indicator content
 */
@Composable
fun GlassIndicator(
    modifier: Modifier = Modifier,
    intensity: GlassIntensity = GlassIntensity.MEDIUM,
    content: @Composable RowScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        intensity = intensity,
        shape = RoundedCornerShape(ShapeTokens.Small),
        showBorder = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SpacingTokens.Medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(SpacingTokens.Small),
            content = content
        )
    }
}

// ===== GRADIENT BACKGROUND =====

/**
 * Ocean theme gradient background.
 *
 * Creates the Ocean Blue gradient background (#0A1929 → #1E293B).
 *
 * @param modifier Modifier for the background
 * @param content Content to display over the gradient
 */
@Composable
fun OceanGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        ColorTokens.GradientStart,
                        ColorTokens.GradientMid,
                        ColorTokens.GradientEnd
                    )
                )
            ),
        content = content
    )
}

// ===== SEMANTIC GLASS COLORS =====

/**
 * Ocean theme semantic colors for glass components.
 */
object OceanColors {
    // Semantic states
    val Success = ColorTokens.Success
    val SuccessGlass = ColorTokens.Success.copy(alpha = 0.15f)

    val Warning = ColorTokens.Warning
    val WarningGlass = ColorTokens.Warning.copy(alpha = 0.15f)

    val Error = ColorTokens.Error
    val ErrorGlass = ColorTokens.Error.copy(alpha = 0.15f)

    val Info = ColorTokens.Info
    val InfoGlass = ColorTokens.Info.copy(alpha = 0.15f)

    // Glass surfaces
    val SurfaceLight = ColorTokens.GlassLight
    val SurfaceMedium = ColorTokens.GlassMedium
    val SurfaceHeavy = ColorTokens.GlassHeavy

    // Borders
    val BorderLight = ColorTokens.OutlineVariant
    val BorderMedium = ColorTokens.Outline

    // Text
    val TextPrimary = ColorTokens.TextPrimary
    val TextSecondary = ColorTokens.TextSecondary
    val TextTertiary = ColorTokens.TextTertiary
    val TextDisabled = ColorTokens.TextDisabled
}
