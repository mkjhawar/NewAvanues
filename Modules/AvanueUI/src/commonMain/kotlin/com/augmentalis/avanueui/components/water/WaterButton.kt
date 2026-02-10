/**
 * WaterButton.kt - Interactive water-effect button
 *
 * Capsule-shaped by default (Apple convention). Interactive behaviors:
 * - Press scale animation (0.96x over 100ms)
 * - Shimmer highlight on press (touch-point illumination)
 *
 * Usage:
 * ```
 * WaterButton(onClick = { /* ... */ }) {
 *     Icon(Icons.Default.Add, contentDescription = null)
 *     Spacer(Modifier.width(8.dp))
 *     Text("Add Item")
 * }
 * ```
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components.water

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.water.WaterBorder
import com.augmentalis.avanueui.water.WaterDefaults
import com.augmentalis.avanueui.water.WaterLevel
import com.augmentalis.avanueui.water.WaterShapes
import com.augmentalis.avanueui.water.waterEffect

/**
 * Interactive water-effect button with press scale and shimmer.
 *
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Enable/disable button
 * @param waterLevel Effect intensity
 * @param shape Button shape (capsule default)
 * @param border Optional gradient border
 * @param contentPadding Padding inside the button
 * @param content Button content (RowScope)
 */
@Deprecated(
    message = "Use AvanueButton instead. Theme controls glass/water/plain rendering.",
    replaceWith = ReplaceWith(
        "AvanueButton(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape, content = content)",
        "com.augmentalis.avanueui.components.AvanueButton"
    )
)
@Composable
fun WaterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    waterLevel: WaterLevel = WaterLevel.REGULAR,
    shape: Shape = WaterShapes.capsule,
    border: WaterBorder? = WaterDefaults.border,
    contentPadding: PaddingValues = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit
) {
    val buttonModifier = modifier.waterEffect(
        backgroundColor = AvanueTheme.colors.primary,
        waterLevel = waterLevel,
        shape = shape,
        border = border,
        interactive = true
    )

    Surface(
        onClick = onClick,
        modifier = buttonModifier,
        enabled = enabled,
        shape = shape,
        color = Color.Transparent,
        contentColor = AvanueTheme.colors.textOnPrimary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content
        )
    }
}
