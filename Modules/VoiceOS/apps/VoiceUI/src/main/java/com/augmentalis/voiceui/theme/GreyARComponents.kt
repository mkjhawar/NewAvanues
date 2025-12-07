package com.augmentalis.voiceui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import com.augmentalis.voiceui.dsl.MagicScope

/**
 * GreyAR Glassmorphic Card Component
 * Recreates the exact card style from the AR interface
 */
@Composable
fun GreyARCard(
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 16.dp,
                shape = GreyARShapes.CardShape,
                ambientColor = Color.Black.copy(alpha = 0.4f),
                spotColor = Color.Black.copy(alpha = 0.4f)
            )
            .clip(GreyARShapes.CardShape)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        GreyARColors.CardBackground.copy(alpha = 0.95f),
                        GreyARColors.CardBackground.copy(alpha = 0.85f)
                    )
                )
            )
            .border(
                width = 0.5.dp,
                color = GreyARColors.Border,
                shape = GreyARShapes.CardShape
            )
    ) {
        // Glassmorphic blur effect background
        Box(
            modifier = Modifier
                .matchParentSize()
                .blur(radius = 10.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Title section
            if (title != null) {
                Text(
                    text = title,
                    style = GreyARTypography.headlineLarge,
                    color = GreyARColors.TextPrimary
                )
            }
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = GreyARTypography.bodyLarge,
                    color = GreyARColors.TextSecondary,
                    lineHeight = 22.sp
                )
            }
            
            // Content
            content()
        }
    }
}

/**
 * GreyAR Button - Matches the "Get started" button style
 */
@Composable
fun GreyARButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true,
    icon: @Composable (() -> Unit)? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = GreyARShapes.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = GreyARColors.AccentBlue,
            contentColor = Color.White,
            disabledContainerColor = GreyARColors.AccentBlue.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 4.dp,
            pressedElevation = 8.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (icon != null) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = GreyARTypography.labelLarge,
            fontSize = 14.sp
        )
    }
}

/**
 * GreyAR Text Button - For secondary actions
 */
@Composable
fun GreyARTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    text: String,
    enabled: Boolean = true
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.textButtonColors(
            contentColor = GreyARColors.AccentBlueLight,
            disabledContentColor = GreyARColors.AccentBlueLight.copy(alpha = 0.5f)
        )
    ) {
        Text(
            text = text,
            style = GreyARTypography.labelLarge
        )
    }
}

/**
 * GreyAR Input Field
 */
@Composable
fun GreyARTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    isError: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = label?.let { { Text(it, color = GreyARColors.TextSecondary) } },
        placeholder = placeholder?.let { { Text(it, color = GreyARColors.TextHint) } },
        isError = isError,
        enabled = enabled,
        singleLine = singleLine,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GreyARColors.AccentBlue,
            unfocusedBorderColor = GreyARColors.Border,
            errorBorderColor = GreyARColors.Error,
            disabledBorderColor = GreyARColors.Border.copy(alpha = 0.5f),
            
            focusedTextColor = GreyARColors.TextPrimary,
            unfocusedTextColor = GreyARColors.TextPrimary,
            disabledTextColor = GreyARColors.TextTertiary,
            
            cursorColor = GreyARColors.AccentBlue,
            errorCursorColor = GreyARColors.Error,
            
            focusedContainerColor = GreyARColors.CardBackground.copy(alpha = 0.3f),
            unfocusedContainerColor = GreyARColors.CardBackground.copy(alpha = 0.2f),
            disabledContainerColor = GreyARColors.CardBackground.copy(alpha = 0.1f),
            errorContainerColor = GreyARColors.CardBackground.copy(alpha = 0.3f)
        ),
        shape = GreyARShapes.InputShape
    )
}

/**
 * GreyAR Footer Text - For copyright and links
 */
@Composable
fun GreyARFooterText(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier,
        style = GreyARTypography.bodySmall,
        color = GreyARColors.TextTertiary,
        textAlign = TextAlign.Center
    )
}

/**
 * GreyAR Link Text - For clickable links
 */
@Composable
fun GreyARLinkText(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TextButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            style = GreyARTypography.bodySmall.copy(
                color = GreyARColors.AccentBlueLight
            ),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * GreyAR Divider
 */
@Composable
fun GreyARDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = 0.5.dp
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = GreyARColors.Divider
    )
}