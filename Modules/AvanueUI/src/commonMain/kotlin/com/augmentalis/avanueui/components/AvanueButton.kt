/**
 * AvanueButton.kt - Unified button component
 *
 * ONE component, theme decides glass/water/plain.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.components.glass.OceanButton
import com.augmentalis.avanueui.components.water.WaterButton
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode

/**
 * Unified button component for the Avanues ecosystem.
 *
 * Renders as glass, water, or plain Material3 based on [AvanueTheme.materialMode].
 *
 * @param onClick Click handler
 * @param modifier Modifier for customization
 * @param enabled Enable/disable button
 * @param shape Button shape
 * @param contentPadding Button content padding
 * @param content Button content (RowScope)
 */
@Suppress("DEPRECATION")
@Composable
fun AvanueButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = GlassDefaults.shape,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    when (AvanueTheme.materialMode) {
        MaterialMode.GLASS -> OceanButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            glass = true,
            shape = shape,
            contentPadding = contentPadding,
            content = content
        )
        MaterialMode.WATER -> WaterButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            content = content
        )
        MaterialMode.PLAIN -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AvanueTheme.colors.primary,
                    contentColor = AvanueTheme.colors.textOnPrimary
                ),
                content = content
            )
        }
    }
}
