/**
 * ResponsiveModifierConverter.kt
 * Extension to ModifierConverter for ResponsiveModifier support
 *
 * Author: Manoj Jhawar
 * Created: 2025-12-23
 * Version: 1.0.0
 */

package com.augmentalis.avamagic.responsive

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import com.augmentalis.avanues.avamagic.components.core.Size

/**
 * Convert ResponsiveModifier to Compose Modifier
 * Uses current ResponsiveContext to resolve device-specific sizing
 */
@Composable
fun ResponsiveModifier.toComposeModifier(): Modifier {
    val context = currentResponsiveContext() ?: return Modifier

    return when (this) {
        is ResponsiveModifier.WidthByBreakpoint -> {
            val size = resolveFor(context.breakpoint)
            size.toWidthModifier()
        }

        is ResponsiveModifier.HeightByBreakpoint -> {
            val size = resolveFor(context.breakpoint)
            size.toHeightModifier()
        }

        is ResponsiveModifier.WidthByDeviceType -> {
            val size = resolveFor(context.deviceType)
            size.toWidthModifier()
        }

        is ResponsiveModifier.FoldableAware -> {
            val size = if (context.foldState?.isOpen == true) open else closed
            size.toWidthModifier()
        }

        is ResponsiveModifier.OrientationAware -> {
            val size = if (context.isPortrait) portrait else landscape
            size.toWidthModifier()
        }

        is ResponsiveModifier.PaddingByBreakpoint -> {
            val padding = resolveFor(context.breakpoint)
            Modifier.padding(androidx.compose.ui.unit.dp(padding))
        }
    }
}

/**
 * Convert Size to width Modifier
 */
private fun Size.toWidthModifier(): Modifier {
    return when (this) {
        is Size.Fixed -> Modifier.width(value.dp)
        is Size.Fill -> Modifier.fillMaxWidth()
        is Size.Percent -> Modifier.fillMaxWidth(value / 100f)
        is Size.Auto -> Modifier
    }
}

/**
 * Convert Size to height Modifier
 */
private fun Size.toHeightModifier(): Modifier {
    return when (this) {
        is Size.Fixed -> Modifier.height(value.dp)
        is Size.Fill -> Modifier.fillMaxHeight()
        is Size.Percent -> Modifier.fillMaxHeight(value / 100f)
        is Size.Auto -> Modifier
    }
}

// Extension imports
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
