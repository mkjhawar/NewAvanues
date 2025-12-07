package com.augmentalis.Avanues.web.universal.presentation.design

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.augmentalis.Avanues.web.universal.presentation.ui.components.glass
import com.augmentalis.Avanues.web.universal.presentation.ui.components.GlassLevel
import com.augmentalis.Avanues.web.universal.presentation.ui.components.GlassBorder

/**
 * Ocean Components (Material3 Implementation)
 *
 * Concrete implementation of ComponentProvider using Material3.
 * All components use OceanDesignTokens for consistent styling.
 *
 * Architecture:
 * - ComponentProvider interface (what exists)
 * - OceanComponents object (how it's implemented with Material3)
 * - Application code (uses interface, unaware of implementation)
 *
 * MagicUI Migration:
 * When migrating to MagicUI:
 * 1. Create MagicUIComponents object implementing ComponentProvider
 * 2. Replace Material3 calls with MagicUI calls
 * 3. Keep same API surface (parameters, behavior)
 * 4. Application code unchanged
 */
object OceanComponents : ComponentProvider {

    /**
     * OceanIcon - Icon with consistent color system
     *
     * Material3 Implementation:
     * Uses Material3 Icon with OceanDesignTokens colors.
     *
     * MagicUI Migration:
     * Replace with MagicUI.Icon, map variants to MagicUI color system.
     */
    @Composable
    override fun Icon(
        imageVector: ImageVector,
        contentDescription: String?,
        variant: IconVariant,
        modifier: Modifier
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = contentDescription,
            tint = when (variant) {
                IconVariant.Primary -> OceanDesignTokens.Icon.primary
                IconVariant.Secondary -> OceanDesignTokens.Icon.secondary
                IconVariant.Disabled -> OceanDesignTokens.Icon.disabled
                IconVariant.Success -> OceanDesignTokens.Icon.success
                IconVariant.Warning -> OceanDesignTokens.Icon.warning
                IconVariant.Error -> OceanDesignTokens.Icon.error
                IconVariant.OnPrimary -> OceanDesignTokens.Icon.onPrimary
            },
            modifier = modifier
        )
    }

    /**
     * OceanIconButton - IconButton with Ocean styling
     *
     * Material3 Implementation:
     * Uses Material3 IconButton with consistent sizing and colors.
     *
     * Features:
     * - Minimum 48dp touch target (accessibility)
     * - Auto-applies disabled colors when enabled=false
     * - Icons inside automatically get correct color via LocalContentColor
     */
    @Composable
    override fun IconButton(
        onClick: () -> Unit,
        enabled: Boolean,
        modifier: Modifier,
        content: @Composable () -> Unit
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.size(OceanDesignTokens.Spacing.touchTarget),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = OceanDesignTokens.Icon.primary,
                disabledContentColor = OceanDesignTokens.Icon.disabled
            ),
            content = content
        )
    }

    /**
     * OceanButton - Button with Ocean styling
     *
     * Material3 Implementation:
     * Uses Material3 Button with variant-based styling.
     *
     * Variants:
     * - Primary: Filled blue button (main actions)
     * - Secondary: Outlined button (secondary actions)
     * - Tertiary: Text button (tertiary actions)
     * - Ghost: Transparent button (minimal emphasis)
     */
    @Composable
    override fun Button(
        onClick: () -> Unit,
        enabled: Boolean,
        variant: ButtonVariant,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit
    ) {
        when (variant) {
            ButtonVariant.Primary -> {
                Button(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = OceanDesignTokens.Surface.primary,
                        contentColor = OceanDesignTokens.Text.onPrimary,
                        disabledContainerColor = OceanDesignTokens.Icon.disabled,
                        disabledContentColor = OceanDesignTokens.Text.disabled
                    ),
                    shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.lg),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = OceanDesignTokens.Elevation.sm
                    ),
                    content = content
                )
            }
            ButtonVariant.Secondary -> {
                OutlinedButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = OceanDesignTokens.Surface.primary,
                        disabledContentColor = OceanDesignTokens.Text.disabled
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            if (enabled) OceanDesignTokens.Border.primary
                            else OceanDesignTokens.Border.subtle
                        )
                    ),
                    shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.lg),
                    content = content
                )
            }
            ButtonVariant.Tertiary -> {
                TextButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OceanDesignTokens.Surface.primary,
                        disabledContentColor = OceanDesignTokens.Text.disabled
                    ),
                    shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.lg),
                    content = content
                )
            }
            ButtonVariant.Ghost -> {
                TextButton(
                    onClick = onClick,
                    enabled = enabled,
                    modifier = modifier,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = OceanDesignTokens.Text.primary,
                        disabledContentColor = OceanDesignTokens.Text.disabled
                    ),
                    shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.lg),
                    content = content
                )
            }
        }
    }

    /**
     * OceanSurface - Surface with Ocean styling
     *
     * Material3 Implementation:
     * Uses Material3 Surface with variant-based styling.
     *
     * Variants:
     * - Default: Standard surface background
     * - Elevated: Card-like elevated surface
     * - Input: Input field surface
     * - Glass: Glassmorphic blurred surface
     */
    @Composable
    override fun Surface(
        modifier: Modifier,
        variant: SurfaceVariant,
        shape: Shape?,
        onClick: (() -> Unit)?,
        content: @Composable () -> Unit
    ) {
        val surfaceShape = shape ?: RoundedCornerShape(OceanDesignTokens.CornerRadius.lg)

        when (variant) {
            SurfaceVariant.Default -> {
                if (onClick != null) {
                    Surface(
                        onClick = onClick,
                        modifier = modifier,
                        shape = surfaceShape,
                        color = OceanDesignTokens.Surface.default,
                        contentColor = OceanDesignTokens.Text.primary,
                        content = content
                    )
                } else {
                    Surface(
                        modifier = modifier,
                        shape = surfaceShape,
                        color = OceanDesignTokens.Surface.default,
                        contentColor = OceanDesignTokens.Text.primary,
                        content = content
                    )
                }
            }
            SurfaceVariant.Elevated -> {
                if (onClick != null) {
                    Surface(
                        onClick = onClick,
                        modifier = modifier,
                        shape = surfaceShape,
                        color = OceanDesignTokens.Surface.elevated,
                        contentColor = OceanDesignTokens.Text.primary,
                        tonalElevation = OceanDesignTokens.Elevation.md,
                        shadowElevation = OceanDesignTokens.Elevation.lg,
                        content = content
                    )
                } else {
                    Surface(
                        modifier = modifier,
                        shape = surfaceShape,
                        color = OceanDesignTokens.Surface.elevated,
                        contentColor = OceanDesignTokens.Text.primary,
                        tonalElevation = OceanDesignTokens.Elevation.md,
                        shadowElevation = OceanDesignTokens.Elevation.lg,
                        content = content
                    )
                }
            }
            SurfaceVariant.Input -> {
                if (onClick != null) {
                    Surface(
                        onClick = onClick,
                        modifier = modifier,
                        shape = surfaceShape,
                        color = OceanDesignTokens.Surface.input,
                        contentColor = OceanDesignTokens.Text.primary,
                        content = content
                    )
                } else {
                    Surface(
                        modifier = modifier,
                        shape = surfaceShape,
                        color = OceanDesignTokens.Surface.input,
                        contentColor = OceanDesignTokens.Text.primary,
                        content = content
                    )
                }
            }
            SurfaceVariant.Glass -> {
                val glassModifier = modifier.glass(
                    backgroundColor = OceanDesignTokens.Surface.elevated,
                    glassLevel = GlassLevel.MEDIUM,
                    border = GlassBorder(
                        width = 1.dp,
                        color = OceanDesignTokens.Border.subtle
                    )
                )

                if (onClick != null) {
                    Surface(
                        onClick = onClick,
                        modifier = glassModifier,
                        shape = surfaceShape,
                        color = Color.Transparent,  // Glass effect applied via modifier
                        contentColor = OceanDesignTokens.Text.primary,
                        content = content
                    )
                } else {
                    Surface(
                        modifier = glassModifier,
                        shape = surfaceShape,
                        color = Color.Transparent,
                        contentColor = OceanDesignTokens.Text.primary,
                        content = content
                    )
                }
            }
        }
    }

    /**
     * OceanTextField - TextField with Ocean styling
     *
     * Material3 Implementation:
     * Uses Material3 OutlinedTextField with Ocean colors.
     */
    @Composable
    override fun TextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier,
        placeholder: String?,
        leadingIcon: (@Composable () -> Unit)?,
        trailingIcon: (@Composable () -> Unit)?,
        enabled: Boolean
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            enabled = enabled,
            placeholder = placeholder?.let {
                { Text(text = it, color = OceanDesignTokens.Text.secondary) }
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = OceanDesignTokens.Text.primary,
                unfocusedTextColor = OceanDesignTokens.Text.primary,
                disabledTextColor = OceanDesignTokens.Text.disabled,
                focusedBorderColor = OceanDesignTokens.Border.primary,
                unfocusedBorderColor = OceanDesignTokens.Border.default,
                disabledBorderColor = OceanDesignTokens.Border.subtle,
                focusedContainerColor = OceanDesignTokens.Surface.input,
                unfocusedContainerColor = OceanDesignTokens.Surface.input,
                disabledContainerColor = OceanDesignTokens.Surface.default
            ),
            shape = RoundedCornerShape(OceanDesignTokens.CornerRadius.lg)
        )
    }

    /**
     * OceanFloatingActionButton - FAB with Ocean styling
     *
     * Material3 Implementation:
     * Uses Material3 FloatingActionButton with Ocean colors and circular shape.
     *
     * Features:
     * - Always circular (56dp)
     * - Primary blue by default
     * - White content by default
     * - Elevated with shadow
     */
    @Composable
    override fun FloatingActionButton(
        onClick: () -> Unit,
        modifier: Modifier,
        containerColor: Color?,
        contentColor: Color?,
        content: @Composable () -> Unit
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = modifier.size(56.dp),
            shape = CircleShape,
            containerColor = containerColor ?: OceanDesignTokens.Surface.primary,
            contentColor = contentColor ?: OceanDesignTokens.Icon.onPrimary,
            elevation = FloatingActionButtonDefaults.elevation(
                defaultElevation = OceanDesignTokens.Elevation.lg,
                pressedElevation = OceanDesignTokens.Elevation.xl
            ),
            content = content
        )
    }
}
