package com.augmentalis.webavanue

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * ComponentProvider Interface
 *
 * Abstraction layer for UI component implementations.
 * Enables seamless transition between UI frameworks (Material3 → MagicUI).
 *
 * Architecture:
 * ┌─────────────────────────────────────────┐
 * │     Application Code (BrowserScreen)    │
 * │     Uses: componentProvider.Button()    │
 * └────────────────┬────────────────────────┘
 *                  │
 *                  ▼
 * ┌─────────────────────────────────────────┐
 * │       ComponentProvider Interface       │
 * │   (Defines what components exist)       │
 * └────────┬────────────────────────────────┘
 *          │
 *          ├──────────────┬─────────────────┐
 *          ▼              ▼                 ▼
 * ┌──────────────┐ ┌──────────────┐ ┌──────────────┐
 * │    Ocean     │ │   MagicUI    │ │   Future     │
 * │  (Material3) │ │   Provider   │ │   Provider   │
 * └──────────────┘ └──────────────┘ └──────────────┘
 *
 * Migration Strategy:
 * 1. Today: Use OceanComponentProvider (Material3)
 * 2. Tomorrow: Implement MagicUIComponentProvider
 * 3. Switch: Change one line (which provider to use)
 * 4. App code: UNCHANGED (uses interface, not implementation)
 *
 * Feature Flag Example:
 * ```kotlin
 * val provider = if (useMagicUI) {
 *     MagicUIComponentProvider
 * } else {
 *     OceanComponentProvider
 * }
 * ```
 */
interface ComponentProvider {

    /**
     * Icon component
     *
     * @param imageVector Icon image
     * @param contentDescription Accessibility description
     * @param variant Icon color variant (Primary, Secondary, Disabled, etc.)
     * @param modifier Modifier for customization
     */
    @Composable
    fun Icon(
        imageVector: ImageVector,
        contentDescription: String?,
        variant: IconVariant,
        modifier: Modifier
    )

    /**
     * IconButton component
     *
     * @param onClick Click handler
     * @param enabled Enable/disable button
     * @param modifier Modifier for customization
     * @param content Icon content
     */
    @Composable
    fun IconButton(
        onClick: () -> Unit,
        enabled: Boolean,
        modifier: Modifier,
        content: @Composable () -> Unit
    )

    /**
     * Button component
     *
     * @param onClick Click handler
     * @param enabled Enable/disable button
     * @param variant Button style variant
     * @param modifier Modifier for customization
     * @param content Button content
     */
    @Composable
    fun Button(
        onClick: () -> Unit,
        enabled: Boolean,
        variant: ButtonVariant,
        modifier: Modifier,
        content: @Composable RowScope.() -> Unit
    )

    /**
     * Surface component
     *
     * @param modifier Modifier for customization
     * @param variant Surface style variant
     * @param shape Surface shape
     * @param onClick Optional click handler
     * @param content Surface content
     */
    @Composable
    fun Surface(
        modifier: Modifier,
        variant: SurfaceVariant,
        shape: Shape?,
        onClick: (() -> Unit)?,
        content: @Composable () -> Unit
    )

    /**
     * TextField component
     *
     * @param value Current text value
     * @param onValueChange Text change callback
     * @param modifier Modifier for customization
     * @param placeholder Placeholder text
     * @param leadingIcon Optional leading icon
     * @param trailingIcon Optional trailing icon
     * @param enabled Enable/disable field
     */
    @Composable
    fun TextField(
        value: String,
        onValueChange: (String) -> Unit,
        modifier: Modifier,
        placeholder: String?,
        leadingIcon: (@Composable () -> Unit)?,
        trailingIcon: (@Composable () -> Unit)?,
        enabled: Boolean
    )

    /**
     * FloatingActionButton component
     *
     * @param onClick Click handler
     * @param modifier Modifier for customization
     * @param containerColor Button background color
     * @param contentColor Button content color
     * @param content Button content (icon)
     */
    @Composable
    fun FloatingActionButton(
        onClick: () -> Unit,
        modifier: Modifier,
        containerColor: Color?,
        contentColor: Color?,
        content: @Composable () -> Unit
    )
}

/**
 * Icon color variants - maps semantic intent to theme colors.
 * Use [toColor] to resolve to the current theme's color.
 */
enum class IconVariant {
    Primary,    // Always visible blue
    Secondary,  // Less emphasis gray
    Disabled,   // Inactive gray
    Success,    // Positive green
    Warning,    // Caution amber
    Error,      // Error red
    OnPrimary   // White (on blue backgrounds)
}

/**
 * Resolve [IconVariant] to the current theme color.
 * Replaces OceanComponents.Icon(variant=...) pattern.
 */
@Composable
fun IconVariant.toColor(): androidx.compose.ui.graphics.Color = when (this) {
    IconVariant.Primary -> com.augmentalis.avanueui.theme.AvanueTheme.colors.iconPrimary
    IconVariant.Secondary -> com.augmentalis.avanueui.theme.AvanueTheme.colors.iconSecondary
    IconVariant.Disabled -> com.augmentalis.avanueui.theme.AvanueTheme.colors.iconDisabled
    IconVariant.Success -> com.augmentalis.avanueui.theme.AvanueTheme.colors.success
    IconVariant.Warning -> com.augmentalis.avanueui.theme.AvanueTheme.colors.warning
    IconVariant.Error -> com.augmentalis.avanueui.theme.AvanueTheme.colors.error
    IconVariant.OnPrimary -> com.augmentalis.avanueui.theme.AvanueTheme.colors.textOnPrimary
}

/**
 * Button style variants
 */
enum class ButtonVariant {
    Primary,    // Filled blue button
    Secondary,  // Outlined button
    Tertiary,   // Text button
    Ghost       // Transparent button
}

/**
 * Surface style variants
 * Maps to OceanDesignTokens.Surface.*
 */
enum class SurfaceVariant {
    Default,    // Standard surface
    Elevated,   // Elevated surface (cards, dialogs)
    Input,      // Input surface (text fields)
    Glass       // Glassmorphic surface
}

// ========== Extension functions with default values ==========
// These provide backward compatibility for call sites that expect default values

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Composable
fun ComponentProvider.Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    variant: IconVariant = IconVariant.Primary,
    modifier: Modifier = Modifier
) = Icon(imageVector, contentDescription, variant, modifier)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Composable
fun ComponentProvider.IconButton(
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) = IconButton(onClick, enabled, modifier, content)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Composable
fun ComponentProvider.Button(
    onClick: () -> Unit,
    enabled: Boolean = true,
    variant: ButtonVariant = ButtonVariant.Primary,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) = Button(onClick, enabled, variant, modifier, content)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Composable
fun ComponentProvider.Surface(
    modifier: Modifier = Modifier,
    variant: SurfaceVariant = SurfaceVariant.Default,
    shape: Shape? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) = Surface(modifier, variant, shape, onClick, content)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Composable
fun ComponentProvider.TextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) = TextField(value, onValueChange, modifier, placeholder, leadingIcon, trailingIcon, enabled)

@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
@Composable
fun ComponentProvider.FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    contentColor: Color? = null,
    content: @Composable () -> Unit
) = FloatingActionButton(onClick, modifier, containerColor, contentColor, content)
