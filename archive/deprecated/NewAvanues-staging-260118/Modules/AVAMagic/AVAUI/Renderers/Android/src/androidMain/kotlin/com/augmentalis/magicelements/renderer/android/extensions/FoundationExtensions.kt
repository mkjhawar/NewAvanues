package com.augmentalis.avamagic.renderer.android.extensions

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.augmentalis.avaelements.renderer.android.ComposeRenderer
import com.augmentalis.avaelements.renderer.android.IconResolver
import com.augmentalis.avaelements.renderer.android.ModifierConverter
import com.augmentalis.avaelements.renderer.android.toComposeColor
import com.augmentalis.avamagic.ui.core.display.ContentScale as ComponentContentScale
import com.augmentalis.avamagic.ui.core.display.FontWeight as ComponentFontWeight
import com.augmentalis.avamagic.ui.core.display.IconComponent
import com.augmentalis.avamagic.ui.core.display.ImageComponent
import com.augmentalis.avamagic.ui.core.display.ImageSource
import com.augmentalis.avamagic.ui.core.display.TextAlign as ComponentTextAlign
import com.augmentalis.avamagic.ui.core.display.TextComponent
import com.augmentalis.avamagic.ui.core.display.TextOverflow as ComponentTextOverflow
import com.augmentalis.avamagic.ui.core.form.ButtonComponent
import com.augmentalis.avamagic.ui.core.form.ButtonScope
import com.augmentalis.avamagic.ui.core.form.CheckboxComponent
import com.augmentalis.avamagic.ui.core.form.SwitchComponent
import com.augmentalis.avamagic.ui.core.form.TextFieldComponent
import com.augmentalis.avamagic.ui.core.layout.Arrangement as LayoutArrangement
import com.augmentalis.avamagic.ui.core.layout.CardComponent
import com.augmentalis.avamagic.ui.core.layout.ColumnComponent
import com.augmentalis.avamagic.ui.core.layout.ContainerComponent
import com.augmentalis.avamagic.ui.core.layout.HorizontalAlignment
import com.augmentalis.avamagic.ui.core.layout.HorizontalArrangement
import com.augmentalis.avamagic.ui.core.layout.Orientation
import com.augmentalis.avamagic.ui.core.layout.RowComponent
import com.augmentalis.avamagic.ui.core.layout.ScrollViewComponent
import com.augmentalis.avamagic.ui.core.layout.VerticalAlignment

/**
 * Foundation Component Extensions
 *
 * Extension functions for rendering foundation-level MagicUI components.
 * Converted from mapper pattern to extension pattern for improved performance and readability.
 *
 * Components:
 * - Button (5 variants: Filled, Outlined, Text, Elevated, Tonal)
 * - Card
 * - Text
 * - TextField
 * - Checkbox
 * - Switch
 * - Icon
 * - Image
 * - Column
 * - Row
 * - Container
 * - ScrollView
 */

// ==================== Form Components ====================

/**
 * Render ButtonComponent to Material3 Button variants
 */
@Composable
fun ButtonComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    val modifier = modifierConverter.convert(modifiers)
    val onClick = this.onClick ?: {}

    when (buttonStyle) {
        ButtonScope.Filled -> {
            Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                Text(text)
            }
        }
        ButtonScope.Outlined -> {
            OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                Text(text)
            }
        }
        ButtonScope.Text -> {
            TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                Text(text)
            }
        }
        ButtonScope.Elevated -> {
            ElevatedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                Text(text)
            }
        }
        ButtonScope.Tonal -> {
            FilledTonalButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled
            ) {
                Text(text)
            }
        }
    }
}

/**
 * Render TextFieldComponent to Material3 OutlinedTextField
 */
@Composable
fun TextFieldComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    var value by remember { mutableStateOf(this.value) }

    OutlinedTextField(
        value = value,
        onValueChange = { newValue ->
            value = newValue
            onValueChange?.invoke(newValue)
        },
        modifier = modifierConverter.convert(modifiers),
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        isError = isError,
        supportingText = if (isError && errorMessage != null) {
            { Text(errorMessage!!) }
        } else null,
        singleLine = maxLength != null
    )
}

/**
 * Render CheckboxComponent to Material3 Checkbox with label
 */
@Composable
fun CheckboxComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    var checked by remember { mutableStateOf(this.checked) }

    Row(
        modifier = modifierConverter.convert(modifiers),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { newValue ->
                checked = newValue
                onCheckedChange?.invoke(newValue)
            },
            enabled = enabled
        )
        Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
        Text(label)
    }
}

/**
 * Render SwitchComponent to Material3 Switch
 */
@Composable
fun SwitchComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    var checked by remember { mutableStateOf(this.checked) }

    Switch(
        checked = checked,
        onCheckedChange = { newValue ->
            checked = newValue
            onCheckedChange?.invoke(newValue)
        },
        modifier = modifierConverter.convert(modifiers),
        enabled = enabled
    )
}

// ==================== Display Components ====================

/**
 * Render TextComponent to Material3 Text
 */
@Composable
fun TextComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    Text(
        text = text,
        modifier = modifierConverter.convert(modifiers),
        style = TextStyle(
            fontSize = fontSize.sp,
            fontWeight = when (fontWeight) {
                ComponentFontWeight.Thin -> FontWeight.Thin
                ComponentFontWeight.ExtraLight -> FontWeight.ExtraLight
                ComponentFontWeight.Light -> FontWeight.Light
                ComponentFontWeight.Normal -> FontWeight.Normal
                ComponentFontWeight.Medium -> FontWeight.Medium
                ComponentFontWeight.SemiBold -> FontWeight.SemiBold
                ComponentFontWeight.Bold -> FontWeight.Bold
                ComponentFontWeight.ExtraBold -> FontWeight.ExtraBold
                ComponentFontWeight.Black -> FontWeight.Black
            }
        ),
        color = color.toComposeColor(),
        textAlign = when (textAlign) {
            ComponentTextAlign.Start -> androidx.compose.ui.text.style.TextAlign.Start
            ComponentTextAlign.Center -> androidx.compose.ui.text.style.TextAlign.Center
            ComponentTextAlign.End -> androidx.compose.ui.text.style.TextAlign.End
            ComponentTextAlign.Justify -> androidx.compose.ui.text.style.TextAlign.Justify
        },
        overflow = when (overflow) {
            ComponentTextOverflow.Clip -> androidx.compose.ui.text.style.TextOverflow.Clip
            ComponentTextOverflow.Ellipsis -> androidx.compose.ui.text.style.TextOverflow.Ellipsis
            ComponentTextOverflow.Visible -> androidx.compose.ui.text.style.TextOverflow.Visible
        },
        maxLines = maxLines ?: Int.MAX_VALUE
    )
}

/**
 * Render IconComponent to Material3 Icon
 * Uses centralized IconResolver for icon resolution
 */
@Composable
fun IconComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    Icon(
        imageVector = IconResolver.resolve(icon),
        contentDescription = contentDescription,
        modifier = modifierConverter.convert(modifiers),
        tint = color.toComposeColor()
    )
}

/**
 * Render ImageComponent to Compose Image
 * Supports both local resources and network URLs using Coil
 */
@Composable
fun ImageComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    val contentScale = when (this.contentScale) {
        ComponentContentScale.Fit -> ContentScale.Fit
        ComponentContentScale.Fill -> ContentScale.FillBounds
        ComponentContentScale.Crop -> ContentScale.Crop
        ComponentContentScale.Inside -> ContentScale.Inside
        ComponentContentScale.None -> ContentScale.None
    }

    val imageModel = when (val source = this.source) {
        is ImageSource.Url -> source.url
        is ImageSource.Asset -> source.name
        is ImageSource.Resource -> source.id
        is ImageSource.Base64 -> source.data
    }

    AsyncImage(
        model = imageModel,
        contentDescription = contentDescription,
        modifier = modifierConverter.convert(modifiers),
        contentScale = contentScale
    )
}

// ==================== Layout Components ====================

/**
 * Render CardComponent to Material3 Card
 */
@Composable
fun CardComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    Card(
        modifier = modifierConverter.convert(modifiers),
        elevation = CardDefaults.cardElevation(
            defaultElevation = elevation.dp
        )
    ) {
        Column {
            children.forEach { child ->
                renderer.RenderComponent(child)
            }
        }
    }
}

/**
 * Render ColumnComponent to Jetpack Compose Column
 */
@Composable
fun ColumnComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    Column(
        modifier = modifierConverter.convert(modifiers),
        verticalArrangement = when (arrangement) {
            LayoutArrangement.Top -> Arrangement.Top
            LayoutArrangement.Center -> Arrangement.Center
            LayoutArrangement.Bottom -> Arrangement.Bottom
            LayoutArrangement.SpaceBetween -> Arrangement.SpaceBetween
            LayoutArrangement.SpaceAround -> Arrangement.SpaceAround
            LayoutArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
            else -> Arrangement.Top
        },
        horizontalAlignment = when (horizontalAlignment) {
            HorizontalAlignment.Start -> Alignment.Start
            HorizontalAlignment.Center -> Alignment.CenterHorizontally
            HorizontalAlignment.End -> Alignment.End
        }
    ) {
        children.forEach { child ->
            renderer.RenderComponent(child)
        }
    }
}

/**
 * Render RowComponent to Jetpack Compose Row
 */
@Composable
fun RowComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    Row(
        modifier = modifierConverter.convert(modifiers),
        horizontalArrangement = when (horizontalArrangement) {
            HorizontalArrangement.Start -> Arrangement.Start
            HorizontalArrangement.End -> Arrangement.End
            HorizontalArrangement.Center -> Arrangement.Center
            HorizontalArrangement.SpaceBetween -> Arrangement.SpaceBetween
            HorizontalArrangement.SpaceAround -> Arrangement.SpaceAround
            HorizontalArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
        },
        verticalAlignment = when (verticalAlignment) {
            VerticalAlignment.Top -> Alignment.Top
            VerticalAlignment.Center -> Alignment.CenterVertically
            VerticalAlignment.Bottom -> Alignment.Bottom
        }
    ) {
        children.forEach { child ->
            renderer.RenderComponent(child)
        }
    }
}

/**
 * Render ContainerComponent to Jetpack Compose Box
 */
@Composable
fun ContainerComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    Box(
        modifier = modifierConverter.convert(modifiers),
        contentAlignment = modifierConverter.toComposeAlignment(contentAlignment)
    ) {
        child?.let { child ->
            renderer.RenderComponent(child)
        }
    }
}

/**
 * Render ScrollViewComponent to Compose scrollable layouts
 */
@Composable
fun ScrollViewComponent.Render(renderer: ComposeRenderer) {
    val modifierConverter = ModifierConverter()
    val scrollState = rememberScrollState()
    val baseModifier = modifierConverter.convert(modifiers)

    when (orientation) {
        Orientation.Vertical -> {
            Column(
                modifier = baseModifier.verticalScroll(scrollState)
            ) {
                child?.let { child ->
                    renderer.RenderComponent(child)
                }
                children.forEach { child ->
                    renderer.RenderComponent(child)
                }
            }
        }
        Orientation.Horizontal -> {
            Row(
                modifier = baseModifier.horizontalScroll(scrollState)
            ) {
                child?.let { child ->
                    renderer.RenderComponent(child)
                }
                children.forEach { child ->
                    renderer.RenderComponent(child)
                }
            }
        }
    }
}
