package com.augmentalis.avaelements.renderers.android.mappers

import androidx.compose.ui.graphics.Color as ComposeColor
import com.augmentalis.avaelements.core.types.Color as MagicColor

/**
 * Extension function to convert AvaElements Color to Jetpack Compose Color
 *
 * NOTE: For new code, prefer using UniversalColor from shared utilities:
 * - com.augmentalis.avaelements.common.color.UniversalColor
 * - com.augmentalis.avaelements.renderer.android.toCompose()
 *
 * UniversalColor provides additional features:
 * - Color manipulation (lighten, darken, saturate, mix)
 * - WCAG contrast calculations
 * - HSL conversion
 * - Platform-agnostic color handling
 *
 * This function is kept for backward compatibility with existing code.
 */
fun MagicColor.toCompose() = ComposeColor(red / 255f, green / 255f, blue / 255f, alpha)
