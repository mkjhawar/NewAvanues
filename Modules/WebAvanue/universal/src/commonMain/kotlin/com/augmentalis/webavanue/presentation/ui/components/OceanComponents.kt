package com.augmentalis.webavanue.ui.screen.components

import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Ocean-themed components with glassmorphic styling
 *
 * ARCHITECTURE NOTE:
 * These components provide a 1:1 mapping to future MagicUI components.
 * When MagicUI is ready, replace implementations with MagicUI imports
 * and update these to be simple wrappers.
 *
 * Migration path:
 * 1. Current: OceanButton -> Compose Material3 + glassmorphism
 * 2. Future: OceanButton -> MagicUI.Button with Ocean theme
 *
 * All app code uses Ocean* components, never Material3 directly.
 * This ensures zero app code changes during MagicUI migration.
 */

/**
 * Glassmorphic modifier for Ocean theme
 * Applies blur, transparency, and border
 *
 * @param backgroundColor Base color (will be made translucent)
 * @param blurRadius Blur amount for glass effect
 * @param borderColor Border color (optional, will be made translucent)
 * @param opacity Background opacity (default 0.15 for glass effect)
 * @param borderWidth Border width
 */
fun Modifier.oceanGlassmorphic(
    backgroundColor: Color,
    blurRadius: Dp = 12.dp,
    borderColor: Color? = null,
    opacity: Float = 0.15f,
    borderWidth: Dp = 1.dp
): Modifier = this
    .background(backgroundColor.copy(alpha = opacity))
    .blur(blurRadius)
    .then(
        if (borderColor != null) {
            Modifier.border(
                width = borderWidth,
                color = borderColor.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
        } else Modifier
    )

/**
 * Ocean Button (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.Button
 */
@Composable
fun OceanButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glassmorphic: Boolean = false,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = if (glassmorphic) {
            modifier.oceanGlassmorphic(
                backgroundColor = MaterialTheme.colorScheme.primaryContainer,
                borderColor = MaterialTheme.colorScheme.primary
            )
        } else {
            modifier
        },
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        shape = RoundedCornerShape(12.dp),
        content = content
    )
}

/**
 * Ocean Card (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.Card
 */
@Composable
fun OceanCard(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glassmorphic: Boolean = false,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(12.dp)
    val cardModifier = if (glassmorphic) {
        modifier.oceanGlassmorphic(
            backgroundColor = MaterialTheme.colorScheme.surface,
            borderColor = MaterialTheme.colorScheme.surfaceVariant
        )
    } else {
        modifier
    }

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = cardModifier,
            enabled = enabled,
            colors = colors,
            elevation = elevation,
            shape = shape,
            content = content
        )
    } else {
        Card(
            modifier = cardModifier,
            colors = colors,
            elevation = elevation,
            shape = shape,
            content = content
        )
    }
}

/**
 * Ocean Surface (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.Surface
 */
@Composable
fun OceanSurface(
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glassmorphic: Boolean = false,
    shape: Shape = RoundedCornerShape(12.dp),
    color: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(color),
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    content: @Composable () -> Unit
) {
    val surfaceModifier = if (glassmorphic) {
        modifier.oceanGlassmorphic(
            backgroundColor = color,
            borderColor = contentColor
        )
    } else {
        modifier
    }

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = surfaceModifier,
            enabled = enabled,
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            content = content
        )
    } else {
        Surface(
            modifier = surfaceModifier,
            shape = shape,
            color = color,
            contentColor = contentColor,
            tonalElevation = tonalElevation,
            shadowElevation = shadowElevation,
            content = content
        )
    }
}

/**
 * Ocean FloatingActionButton (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.FAB
 */
@Composable
fun OceanFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    glassmorphic: Boolean = true, // FABs are glassmorphic by default in Ocean theme
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    content: @Composable () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        modifier = if (glassmorphic) {
            modifier.oceanGlassmorphic(
                backgroundColor = containerColor,
                borderColor = contentColor
            )
        } else {
            modifier
        },
        containerColor = if (glassmorphic) Color.Transparent else containerColor,
        contentColor = contentColor,
        elevation = elevation,
        shape = RoundedCornerShape(16.dp),
        content = content
    )
}

/**
 * Ocean Dialog (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.Dialog
 */
@Composable
fun OceanDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    glassmorphic: Boolean = false,
    properties: DialogProperties = DialogProperties(),
    content: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {}, // Will be provided in content
        modifier = if (glassmorphic) {
            modifier.oceanGlassmorphic(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            modifier
        },
        shape = RoundedCornerShape(12.dp),
        properties = properties,
        text = content
    )
}

/**
 * Ocean TextField (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.TextField
 */
@Composable
fun OceanTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    glassmorphic: Boolean = false,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = if (glassmorphic) {
            modifier.oceanGlassmorphic(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.outline
            )
        } else {
            modifier
        },
        enabled = enabled,
        readOnly = readOnly,
        label = label,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        supportingText = supportingText,
        isError = isError,
        singleLine = singleLine,
        maxLines = maxLines,
        colors = colors,
        shape = RoundedCornerShape(12.dp)
    )
}

/**
 * Ocean IconButton (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.IconButton
 */
@Composable
fun OceanIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glassmorphic: Boolean = false,
    colors: IconButtonColors = IconButtonDefaults.iconButtonColors(),
    content: @Composable () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = if (glassmorphic) {
            modifier.oceanGlassmorphic(
                backgroundColor = MaterialTheme.colorScheme.surfaceVariant,
                borderColor = MaterialTheme.colorScheme.outline
            )
        } else {
            modifier
        },
        enabled = enabled,
        colors = colors,
        content = content
    )
}

/**
 * Ocean Chip (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.Chip
 */
@Composable
fun OceanChip(
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    glassmorphic: Boolean = false,
    colors: ChipColors = AssistChipDefaults.assistChipColors()
) {
    AssistChip(
        onClick = onClick,
        label = label,
        modifier = if (glassmorphic) {
            modifier.oceanGlassmorphic(
                backgroundColor = MaterialTheme.colorScheme.secondaryContainer,
                borderColor = MaterialTheme.colorScheme.secondary
            )
        } else {
            modifier
        },
        enabled = enabled,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        colors = colors,
        shape = RoundedCornerShape(8.dp),
        border = null // Border handled by glassmorphic modifier
    )
}

/**
 * Ocean BottomSheet (MagicUI-compatible)
 *
 * Future migration: Replace with MagicUI.BottomSheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OceanModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    glassmorphic: Boolean = false,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    content: @Composable ColumnScope.() -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = if (glassmorphic) {
            modifier.oceanGlassmorphic(
                backgroundColor = MaterialTheme.colorScheme.surface,
                borderColor = MaterialTheme.colorScheme.surfaceVariant
            )
        } else {
            modifier
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        dragHandle = dragHandle,
        content = content
    )
}

/**
 * Ocean constants matching future MagicUI design tokens
 */
object OceanTokens {
    val CornerRadius = 12.dp
    val CornerRadiusSmall = 8.dp
    val CornerRadiusLarge = 16.dp

    val BlurRadius = 12.dp
    val GlassOpacity = 0.15f
    val BorderOpacity = 0.3f

    val MinTouchTarget = 48.dp

    val SpacingXSmall = 4.dp
    val SpacingSmall = 8.dp
    val SpacingMedium = 16.dp
    val SpacingLarge = 24.dp
    val SpacingXLarge = 32.dp

    val AnimationDuration = 300 // milliseconds
    val SpringStiffness = spring<Float>()
}
