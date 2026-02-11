/**
 * AvanueButton.kt - Unified button component
 *
 * ONE component, theme decides glass/water/cupertino/mountainview.
 * Reads AvanueTheme.materialMode to delegate to the correct implementation.
 *
 * Copyright (C) Manoj Jhawar/Aman Jhawar, Intelligent Devices LLC
 */
package com.augmentalis.avanueui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.augmentalis.avanueui.components.glass.GlassDefaults
import com.augmentalis.avanueui.components.glass.OceanButton
import com.augmentalis.avanueui.components.water.WaterButton
import com.augmentalis.avanueui.theme.AvanueTheme
import com.augmentalis.avanueui.theme.MaterialMode

/**
 * Unified button component for the Avanues ecosystem.
 *
 * Renders as glass, water, cupertino, or mountainview based on [AvanueTheme.materialMode].
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
        MaterialMode.Glass -> OceanButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            glass = true,
            shape = shape,
            contentPadding = contentPadding,
            content = content
        )
        MaterialMode.Water -> WaterButton(
            onClick = onClick,
            modifier = modifier,
            enabled = enabled,
            shape = shape,
            content = content
        )
        MaterialMode.Cupertino -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                contentPadding = contentPadding,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AvanueTheme.colors.primary,
                    contentColor = AvanueTheme.colors.textOnPrimary
                ),
                elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp, 0.dp),
                content = content
            )
        }
        MaterialMode.MountainView -> {
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
