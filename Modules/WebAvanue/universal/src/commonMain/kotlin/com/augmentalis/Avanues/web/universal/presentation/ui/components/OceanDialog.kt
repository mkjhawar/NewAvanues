package com.augmentalis.Avanues.web.universal.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * OceanDialog - Glassmorphism dialog following Ocean Blue theme
 *
 * UI Guidelines Reference: ld-ui-guidelines-v1.md
 *
 * Ocean Theme Tokens:
 * - Surface: Dark slate (#1E293B)
 * - TextPrimary: White 90%
 * - TextSecondary: White 80%
 * - CoralBlue: #3B82F6 (Primary)
 * - Border: White 10-30%
 *
 * @param onDismissRequest Called when dialog should be dismissed
 * @param title Dialog title text
 * @param content Dialog body content
 * @param confirmButton Confirm action button
 * @param dismissButton Optional dismiss action button
 * @param modifier Modifier for customization
 */
@Composable
fun OceanDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    // Ocean Theme colors
    val surfaceColor = Color(0xFF1E293B)       // Dark slate surface
    val textPrimary = Color(0xFFE2E8F0)        // White 90% - for headers
    val textSecondary = Color(0xFFCBD5E1)      // White 80% - for body
    val borderColor = Color(0x33FFFFFF)         // White 20% - border
    val primaryColor = Color(0xFF3B82F6)        // CoralBlue

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            color = surfaceColor,
            shadowElevation = 8.dp,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = textPrimary,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                Box(modifier = Modifier.fillMaxWidth()) {
                    content()
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    dismissButton?.invoke()

                    if (dismissButton != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                    }

                    confirmButton()
                }
            }
        }
    }
}

/**
 * OceanDialogDefaults - Default colors and styles for OceanDialog components
 */
object OceanDialogDefaults {
    // Ocean Theme tokens
    val surfaceColor = Color(0xFF1E293B)
    val textPrimary = Color(0xFFE2E8F0)
    val textSecondary = Color(0xFFCBD5E1)
    val textTertiary = Color(0xFF94A3B8)
    val primaryColor = Color(0xFF3B82F6)
    val borderColor = Color(0x33FFFFFF)
    val inputBackground = Color(0xFF334155)
    val inputBorder = Color(0x4DFFFFFF)
    val inputBorderFocused = Color(0xFF3B82F6)

    /**
     * Colors for OutlinedTextField in Ocean theme
     */
    @Composable
    fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
        focusedTextColor = textPrimary,
        unfocusedTextColor = textPrimary,
        cursorColor = primaryColor,
        focusedBorderColor = inputBorderFocused,
        unfocusedBorderColor = inputBorder,
        focusedContainerColor = inputBackground,
        unfocusedContainerColor = inputBackground,
        focusedPlaceholderColor = textTertiary,
        unfocusedPlaceholderColor = textTertiary,
        focusedLabelColor = textSecondary,
        unfocusedLabelColor = textTertiary
    )

    /**
     * Primary button colors for Ocean theme
     */
    @Composable
    fun primaryButtonColors() = ButtonDefaults.textButtonColors(
        contentColor = primaryColor
    )

    /**
     * Secondary/dismiss button colors for Ocean theme
     */
    @Composable
    fun secondaryButtonColors() = ButtonDefaults.textButtonColors(
        contentColor = textSecondary
    )
}

/**
 * OceanTextButton - Text button styled for Ocean theme
 */
@Composable
fun OceanTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isPrimary: Boolean = false,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        colors = if (isPrimary) {
            OceanDialogDefaults.primaryButtonColors()
        } else {
            OceanDialogDefaults.secondaryButtonColors()
        },
        content = content
    )
}
