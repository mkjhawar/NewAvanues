/**
 * ComponentProvider.kt - Abstraction layer for UI component implementations
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 * Author: VOS4 Development Team
 * Created: 2025-01-02
 * Updated: 2026-01-19 (Migrated to AvaUI/Foundation)
 *
 * Originally from: Avanues/Web/common/webavanue/universal
 *
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
package com.augmentalis.avamagic.ui.foundation

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
 * Icon color variants
 * Maps to OceanDesignTokens.Icon.*
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
