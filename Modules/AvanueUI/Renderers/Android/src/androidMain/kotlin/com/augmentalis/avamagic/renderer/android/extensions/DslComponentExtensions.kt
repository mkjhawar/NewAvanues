package com.augmentalis.avamagic.renderer.android.extensions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.augmentalis.avamagic.components.core.Font
import com.augmentalis.avamagic.components.dsl.ButtonComponent
import com.augmentalis.avamagic.components.dsl.ButtonScope
import com.augmentalis.avamagic.components.dsl.CardComponent
import com.augmentalis.avamagic.components.dsl.ImageComponent
import com.augmentalis.avamagic.components.dsl.ImageScope
import com.augmentalis.avamagic.components.dsl.TextComponent
import com.augmentalis.avamagic.components.dsl.TextFieldComponent
import com.augmentalis.avamagic.components.dsl.TextScope
import com.augmentalis.avamagic.renderer.android.ComposeRenderer
import com.augmentalis.avamagic.renderer.android.IconResolver
import com.augmentalis.avamagic.renderer.android.ModifierConverter
import com.augmentalis.avamagic.renderer.android.toComposeColor

/**
 * DSL Component Extensions
 *
 * Extension functions for rendering components created via the AvaUI { } DSL builder.
 * These handle com.augmentalis.avamagic.components.dsl.* types, which are distinct from
 * the com.augmentalis.avamagic.ui.core.* types handled by FoundationExtensions.
 *
 * Named RenderDsl to avoid collision with the ui.core.* Render() extensions.
 */

private val dslModifierConverter = ModifierConverter()

// ==================== Button ====================

/**
 * Renders DSL ButtonComponent to a Material3 Button variant.
 * ButtonStyle.Primary -> filled Button (highest emphasis)
 * ButtonStyle.Secondary -> FilledTonalButton (medium emphasis)
 * ButtonStyle.Tertiary -> ElevatedButton (low emphasis with elevation)
 * ButtonStyle.Text -> TextButton (lowest emphasis)
 * ButtonStyle.Outlined -> OutlinedButton (medium emphasis, outlined)
 */
@Composable
fun ButtonComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    val clickHandler = onClick ?: {}

    when (buttonStyle) {
        ButtonScope.ButtonStyle.Primary -> {
            Button(
                onClick = clickHandler,
                modifier = modifier,
                enabled = enabled
            ) {
                leadingIcon?.let { iconName ->
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                }
                Text(text)
                trailingIcon?.let { iconName ->
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                }
            }
        }

        ButtonScope.ButtonStyle.Secondary -> {
            FilledTonalButton(
                onClick = clickHandler,
                modifier = modifier,
                enabled = enabled
            ) {
                leadingIcon?.let { iconName ->
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                }
                Text(text)
                trailingIcon?.let { iconName ->
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                }
            }
        }

        ButtonScope.ButtonStyle.Tertiary -> {
            ElevatedButton(
                onClick = clickHandler,
                modifier = modifier,
                enabled = enabled
            ) {
                leadingIcon?.let { iconName ->
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                }
                Text(text)
                trailingIcon?.let { iconName ->
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                }
            }
        }

        ButtonScope.ButtonStyle.Text -> {
            TextButton(
                onClick = clickHandler,
                modifier = modifier,
                enabled = enabled
            ) {
                leadingIcon?.let { iconName ->
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                }
                Text(text)
                trailingIcon?.let { iconName ->
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                }
            }
        }

        ButtonScope.ButtonStyle.Outlined -> {
            OutlinedButton(
                onClick = clickHandler,
                modifier = modifier,
                enabled = enabled
            ) {
                leadingIcon?.let { iconName ->
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                }
                Text(text)
                trailingIcon?.let { iconName ->
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    Icon(
                        imageVector = IconResolver.resolve(iconName),
                        contentDescription = null
                    )
                }
            }
        }
    }
}

// ==================== Text ====================

/**
 * Renders DSL TextComponent to a Compose Text.
 * Converts Font (family/size/weight/style) and TextScope enums to Compose equivalents.
 */
@Composable
fun TextComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    val fontWeight = when (font.weight) {
        Font.Weight.Thin -> FontWeight.Thin
        Font.Weight.ExtraLight -> FontWeight.ExtraLight
        Font.Weight.Light -> FontWeight.Light
        Font.Weight.Regular -> FontWeight.Normal
        Font.Weight.Medium -> FontWeight.Medium
        Font.Weight.SemiBold -> FontWeight.SemiBold
        Font.Weight.Bold -> FontWeight.Bold
        Font.Weight.ExtraBold -> FontWeight.ExtraBold
        Font.Weight.Black -> FontWeight.Black
    }

    val fontStyle = when (font.style) {
        Font.Style.Italic, Font.Style.Oblique -> FontStyle.Italic
        Font.Style.Normal -> FontStyle.Normal
    }

    val textAlign = when (textAlign) {
        TextScope.TextAlign.Start -> androidx.compose.ui.text.style.TextAlign.Start
        TextScope.TextAlign.Center -> androidx.compose.ui.text.style.TextAlign.Center
        TextScope.TextAlign.End -> androidx.compose.ui.text.style.TextAlign.End
        TextScope.TextAlign.Justify -> androidx.compose.ui.text.style.TextAlign.Justify
    }

    val overflow = when (overflow) {
        TextScope.TextOverflow.Clip -> androidx.compose.ui.text.style.TextOverflow.Clip
        TextScope.TextOverflow.Ellipsis -> androidx.compose.ui.text.style.TextOverflow.Ellipsis
        TextScope.TextOverflow.Visible -> androidx.compose.ui.text.style.TextOverflow.Visible
    }

    Text(
        text = text,
        modifier = modifier,
        color = color.toComposeColor(),
        style = TextStyle(
            fontSize = font.size.sp,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            lineHeight = font.lineHeight.sp
        ),
        textAlign = textAlign,
        overflow = overflow,
        maxLines = maxLines ?: Int.MAX_VALUE
    )
}

// ==================== Card ====================

/**
 * Renders DSL CardComponent to a Material3 Card.
 * elevation is an Int (from CardScope.elevation) mapped to dp.
 */
@Composable
fun CardComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = elevation.dp)
    ) {
        Column {
            children.forEach { child ->
                renderer.RenderComponent(child)
            }
        }
    }
}

// ==================== TextField (Input) ====================

/**
 * Renders DSL TextFieldComponent to a Material3 OutlinedTextField.
 * Supports label, placeholder, error state, errorMessage, enabled/readOnly, and
 * maxLength (single-line when set). The value is held in local state so the field
 * is editable; onValueChange propagates changes back to the caller.
 */
@Composable
fun TextFieldComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var currentValue by remember { mutableStateOf(value) }

    OutlinedTextField(
        value = currentValue,
        onValueChange = { newValue ->
            if (maxLength == null || newValue.length <= maxLength) {
                currentValue = newValue
                onValueChange?.invoke(newValue)
            }
        },
        modifier = modifier,
        enabled = enabled,
        readOnly = readOnly,
        label = label?.let { labelText -> { Text(labelText) } },
        placeholder = placeholder.takeIf { it.isNotEmpty() }?.let { ph -> { Text(ph) } },
        isError = isError,
        supportingText = if (isError && !errorMessage.isNullOrEmpty()) {
            { Text(errorMessage) }
        } else null,
        singleLine = maxLength != null
    )
}

// ==================== Image ====================

/**
 * Renders DSL ImageComponent to an AsyncImage (Coil).
 * source is a plain String — treated as a URL when it starts with "http/https",
 * otherwise passed directly to Coil which handles local assets and resource names.
 */
@Composable
fun ImageComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    val composeContentScale = when (contentScale) {
        ImageScope.ContentScale.Fit -> ContentScale.Fit
        ImageScope.ContentScale.Fill -> ContentScale.FillBounds
        ImageScope.ContentScale.Crop -> ContentScale.Crop
        ImageScope.ContentScale.None -> ContentScale.None
    }

    AsyncImage(
        model = source,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = composeContentScale,
        alignment = Alignment.Center
    )
}
