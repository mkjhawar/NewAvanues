/**
 * GlassmorphicComponents.kt - Glassmorphic UI components for Ocean theme
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Migrated to AvaUI/Foundation)
 *
 * Originally from: Avanues/Web/common/webavanue/universal
 *
 * MIGRATION ARCHITECTURE:
 * ┌─────────────────────────────────────────────────────────────┐
 * │                     Current (Compose)                       │
 * ├─────────────────────────────────────────────────────────────┤
 * │  GlassmorphicComponents.kt    OceanThemeExtensions.kt       │
 * │  ┌─────────────┐              ┌──────────────────┐          │
 * │  │ GlassSurface│              │ Modifier.glass*  │          │
 * │  │ GlassCard   │              │ OceanGlass       │          │
 * │  │ GlassBubble │              │ OceanGradients   │          │
 * │  │ OceanButton │              │ OceanShapes      │          │
 * │  │ GlassChip   │              │ GlassDefaults    │          │
 * │  └──────┬──────┘              └────────┬─────────┘          │
 * │         │                              │                    │
 * │         └──────────┬───────────────────┘                    │
 * │                    ▼                                        │
 * │         ┌──────────────────┐                                │
 * │         │  Material3 +     │                                │
 * │         │  ColorTokens     │                                │
 * │         └──────────────────┘                                │
 * └─────────────────────────────────────────────────────────────┘
 *                           │
 *                           │ Migration (when ready)
 *                           ▼
 * ┌─────────────────────────────────────────────────────────────┐
 * │                     Future (MagicUI)                        │
 * ├─────────────────────────────────────────────────────────────┤
 * │  MagicUI.Surface, MagicUI.Card, MagicUI.ChatBubble, etc.   │
 * │  (Same API surface, cross-platform: Android/iOS/Web)        │
 * └─────────────────────────────────────────────────────────────┘
 *
 * IMPORTANT:
 * - ALL app code uses these components, NEVER Material3 directly
 * - When migrating to MagicUI, only this file changes
 * - App code remains unchanged during migration
 * - 1:1 API mapping ensures seamless transition
 */
package com.augmentalis.avanueui.components.glass

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.glass.GlassBorder
import com.augmentalis.avanueui.glass.GlassLevel
import com.augmentalis.avanueui.theme.AvanueTheme

/**
 * GlassSurface - Base glassmorphic surface component
 *
 * MagicUI equivalent: MagicUI.Surface
 *
 * @param onClick Optional click handler (makes surface interactive)
 * @param modifier Modifier for customization
 * @param enabled Enable/disable interaction (when onClick provided)
 * @param shape Surface shape (default: 12.dp rounded corners)
 * @param color Base color (will be made translucent for glass effect)
 * @param contentColor Content color (text, icons)
 * @param tonalElevation Material3 tonal elevation
 * @param shadowElevation Material3 shadow elevation
 * @param border Border configuration
 * @param glassLevel Glass effect strength (LIGHT, MEDIUM, HEAVY)
 * @param content Composable content
 */
@Deprecated(
    message = "Use AvanueSurface instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueSurface(onClick = onClick, modifier = modifier, shape = shape, content = content)",
        "com.augmentalis.avanueui.components.AvanueSurface"
    )
)
@Composable
fun GlassSurface(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassDefaults.shape,
    color: Color = AvanueTheme.colors.surface,
    contentColor: Color = AvanueTheme.colors.textPrimary,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: GlassBorder? = GlassDefaults.border,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable () -> Unit
) {
    val surfaceModifier = modifier.glass(
        backgroundColor = color,
        glassLevel = glassLevel,
        border = border,
        shape = shape
    )

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            enabled = enabled,
            shape = shape,
            color = Color.Transparent, // Glass effect applied via modifier
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            content = content
        )
    } else {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            color = Color.Transparent, // Glass effect applied via modifier
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            content = content
        )
    }
}

/**
 * GlassCard - Glassmorphic card component
 *
 * MagicUI equivalent: MagicUI.Card
 *
 * @param onClick Optional click handler
 * @param modifier Modifier for customization
 * @param enabled Enable/disable interaction
 * @param shape Card shape
 * @param colors Card colors
 * @param elevation Card elevation
 * @param border Border configuration
 * @param glassLevel Glass effect strength
 * @param content Composable content
 */
@Deprecated(
    message = "Use AvanueCard instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueCard(onClick = onClick, modifier = modifier, shape = shape, content = content)",
        "com.augmentalis.avanueui.components.AvanueCard"
    )
)
@Composable
fun GlassCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassDefaults.shape,
    colors: CardColors = GlassDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: GlassBorder? = GlassDefaults.border,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = modifier.glass(
        backgroundColor = colors.containerColor,
        glassLevel = glassLevel,
        border = border,
        shape = shape
    )

    // Zero elevation prevents shadow artifacts (sharp corners behind rounded clip)
    // Glass effect provides its own visual depth via translucent overlay + border
    val zeroElevation = CardDefaults.cardElevation(
        defaultElevation = 0.dp,
        pressedElevation = 0.dp,
        focusedElevation = 0.dp,
        hoveredElevation = 0.dp,
        draggedElevation = 0.dp,
        disabledElevation = 0.dp
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            enabled = enabled,
            shape = shape,
            colors = colors.copy(containerColor = Color.Transparent), // Glass applied via modifier
            elevation = zeroElevation,
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            shape = shape,
            colors = colors.copy(containerColor = Color.Transparent),
            elevation = zeroElevation,
            content = content
        )
    }
}

/**
 * GlassBubble - Chat bubble / message component with glass effect
 *
 * MagicUI equivalent: MagicUI.ChatBubble
 *
 * @param modifier Modifier for customization
 * @param align Bubble alignment (START, END, CENTER)
 * @param shape Bubble shape (default: rounded with tail)
 * @param color Bubble background color
 * @param contentColor Content color
 * @param border Border configuration
 * @param glassLevel Glass effect strength
 * @param content Composable content
 */
@Deprecated(
    message = "Use AvanueBubble instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueBubble(modifier = modifier, align = align, shape = shape, content = content)",
        "com.augmentalis.avanueui.components.AvanueBubble"
    )
)
@Composable
fun GlassBubble(
    modifier: Modifier = Modifier,
    align: BubbleAlign = BubbleAlign.START,
    shape: Shape = when (align) {
        BubbleAlign.START -> GlassShapes.bubbleStart
        BubbleAlign.END -> GlassShapes.bubbleEnd
        BubbleAlign.CENTER -> GlassDefaults.shape
    },
    color: Color = AvanueTheme.colors.surface,
    contentColor: Color = AvanueTheme.colors.textPrimary,
    border: GlassBorder? = GlassDefaults.border,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.glass(
            backgroundColor = color,
            glassLevel = glassLevel,
            border = border,
            shape = shape
        ),
        shape = shape,
        color = Color.Transparent,
        contentColor = contentColor,
        content = content
    )
}

/**
 * OceanButton - Primary button with glass effect option
 *
 * MagicUI equivalent: MagicUI.Button
 *
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Enable/disable button
 * @param glass Enable glassmorphic styling
 * @param glassLevel Glass effect strength (when glass=true)
 * @param colors Button colors
 * @param elevation Button elevation
 * @param shape Button shape
 * @param contentPadding Button content padding
 * @param content Button content
 */
@Deprecated(
    message = "Use AvanueButton instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueButton(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, content = content)",
        "com.augmentalis.avanueui.components.AvanueButton"
    )
)
@Composable
fun OceanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Boolean = false,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    colors: ButtonColors = ButtonDefaults.buttonColors(
        containerColor = AvanueTheme.colors.primary,
        contentColor = AvanueTheme.colors.textOnPrimary
    ),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    shape: Shape = GlassDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = if (glass) {
            modifier.glass(
                backgroundColor = colors.containerColor,
                glassLevel = glassLevel,
                border = GlassDefaults.border,
                shape = shape
            )
        } else {
            modifier
        },
        enabled = enabled,
        colors = if (glass) colors.copy(containerColor = Color.Transparent) else colors,
        elevation = elevation,
        shape = shape,
        contentPadding = contentPadding,
        content = content
    )
}

/**
 * GlassChip - Chip/tag component with glass effect
 *
 * MagicUI equivalent: MagicUI.Chip
 *
 * @param onClick Click handler
 * @param label Chip label
 * @param modifier Modifier for customization
 * @param enabled Enable/disable chip
 * @param leadingIcon Optional leading icon
 * @param trailingIcon Optional trailing icon
 * @param glass Enable glassmorphic styling
 * @param glassLevel Glass effect strength (when glass=true)
 * @param colors Chip colors
 * @param elevation Chip elevation
 * @param shape Chip shape
 */
@Deprecated(
    message = "Use AvanueChip instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueChip(onClick = onClick, label = label, modifier = modifier, enabled = enabled, leadingIcon = leadingIcon, trailingIcon = trailingIcon)",
        "com.augmentalis.avanueui.components.AvanueChip"
    )
)
@Composable
fun GlassChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    glass: Boolean = false,
    glassLevel: GlassLevel = GlassLevel.LIGHT,
    colors: ChipColors = AssistChipDefaults.assistChipColors(
        containerColor = AvanueTheme.colors.surfaceElevated,
        labelColor = AvanueTheme.colors.textPrimary
    ),
    elevation: ChipElevation? = AssistChipDefaults.assistChipElevation(),
    shape: Shape = GlassShapes.chipShape
) {
    AssistChip(
        onClick = onClick,
        label = label,
        modifier = if (glass) {
            modifier.glass(
                backgroundColor = colors.containerColor,
                glassLevel = glassLevel,
                border = GlassDefaults.borderSubtle,
                shape = shape
            )
        } else {
            modifier
        },
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = if (glass) colors.copy(containerColor = Color.Transparent) else colors,
        elevation = elevation,
        shape = shape,
        border = null // Border handled by glass modifier
    )
}

/**
 * GlassFloatingActionButton - FAB with mandatory glass effect
 *
 * MagicUI equivalent: MagicUI.FAB
 *
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param shape FAB shape
 * @param containerColor FAB background color
 * @param contentColor FAB content color
 * @param elevation FAB elevation
 * @param glassLevel Glass effect strength
 * @param content FAB content (icon)
 */
@Deprecated(
    message = "Use AvanueFAB instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueFAB(onClick = onClick, modifier = modifier, shape = shape, content = content)",
        "com.augmentalis.avanueui.components.AvanueFAB"
    )
)
@Composable
fun GlassFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapes.fabShape,
    containerColor: Color = AvanueTheme.colors.primary,
    contentColor: Color = AvanueTheme.colors.textOnPrimary,
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = modifier
            .clip(shape)  // Clip to shape BEFORE applying glass effect
            .glass(
                backgroundColor = containerColor,
                glassLevel = glassLevel,
                border = GlassDefaults.border
            ),
        shape = shape,
        containerColor = Color.Transparent, // Glass applied via modifier
        contentColor = contentColor,
        elevation = elevation,
        content = content
    )
}

/**
 * GlassIconButton - Icon button with optional glass effect
 *
 * MagicUI equivalent: MagicUI.IconButton
 *
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Enable/disable button
 * @param glass Enable glassmorphic styling
 * @param glassLevel Glass effect strength (when glass=true)
 * @param colors Icon button colors
 * @param content Button content (icon)
 */
@Deprecated(
    message = "Use AvanueIconButton instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueIconButton(onClick = onClick, modifier = modifier, enabled = enabled, content = content)",
        "com.augmentalis.avanueui.components.AvanueIconButton"
    )
)
@Composable
fun GlassIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Boolean = false,
    glassLevel: GlassLevel = GlassLevel.LIGHT,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    val iconShape = RoundedCornerShape(8.dp)
    IconButton(
        onClick = onClick,
        modifier = if (glass) {
            modifier.glass(
                backgroundColor = AvanueTheme.colors.surfaceElevated,
                glassLevel = glassLevel,
                border = GlassDefaults.borderSubtle,
                shape = iconShape
            )
        } else {
            modifier
        },
        enabled = enabled,
        colors = colors,
        content = content
    )
}

/**
 * GlassIndicator - Full-width status indicator bar with glass effect
 *
 * @param modifier Modifier for the indicator
 * @param glassLevel Glass effect strength
 * @param content Indicator content (Row scope)
 */
@Deprecated(
    message = "Use AvanueSurface instead. GlassIndicator is a thin wrapper over GlassSurface.",
    replaceWith = ReplaceWith(
        "AvanueSurface(modifier = modifier, content = { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), content = content) })",
        "com.augmentalis.avanueui.components.AvanueSurface"
    )
)
@Suppress("DEPRECATION")
@Composable
fun GlassIndicator(
    modifier: Modifier = Modifier,
    glassLevel: GlassLevel = GlassLevel.MEDIUM,
    content: @Composable RowScope.() -> Unit
) {
    GlassSurface(
        modifier = modifier,
        shape = GlassShapes.small,
        glassLevel = glassLevel
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

/**
 * Glass effect strength levels - re-exported from DesignSystem for backward compat.
 */
typealias GlassLevel = com.augmentalis.avanueui.glass.GlassLevel

/**
 * Bubble alignment options
 */
enum class BubbleAlign {
    START,   // Left-aligned (incoming message)
    END,     // Right-aligned (outgoing message)
    CENTER   // Center-aligned (system message)
}

/**
 * Glass border configuration - re-exported from DesignSystem for backward compat.
 */
typealias GlassBorder = com.augmentalis.avanueui.glass.GlassBorder

/**
 * CardColors extension to copy with new container color
 */
@Composable
private fun CardColors.copy(containerColor: Color): CardColors {
    // Material3 CardColors is immutable, so we need to create new instance
    return CardDefaults.cardColors(
        containerColor = containerColor,
        contentColor = this.contentColor,
        disabledContainerColor = this.disabledContainerColor,
        disabledContentColor = this.disabledContentColor
    )
}

/**
 * ButtonColors extension to copy with new container color
 */
@Composable
private fun ButtonColors.copy(containerColor: Color): ButtonColors {
    return ButtonDefaults.buttonColors(
        containerColor = containerColor,
        contentColor = this.contentColor,
        disabledContainerColor = this.disabledContainerColor,
        disabledContentColor = this.disabledContentColor
    )
}

/**
 * ChipColors extension to copy with new container color
 */
@Composable
private fun ChipColors.copy(containerColor: Color): ChipColors {
    return AssistChipDefaults.assistChipColors(
        containerColor = containerColor,
        labelColor = this.labelColor,
        leadingIconContentColor = this.leadingIconContentColor,
        trailingIconContentColor = this.trailingIconContentColor,
        disabledContainerColor = this.disabledContainerColor,
        disabledLabelColor = this.disabledLabelColor,
        disabledLeadingIconContentColor = this.disabledLeadingIconContentColor,
        disabledTrailingIconContentColor = this.disabledTrailingIconContentColor
    )
}
