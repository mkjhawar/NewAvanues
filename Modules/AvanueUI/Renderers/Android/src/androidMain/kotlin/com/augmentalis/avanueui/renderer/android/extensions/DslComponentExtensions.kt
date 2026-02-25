package com.augmentalis.avanueui.renderer.android.extensions

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.augmentalis.avanueui.core.Arrangement as DslArrangement
import com.augmentalis.avanueui.core.Alignment as DslAlignment
import com.augmentalis.avanueui.core.Font
import com.augmentalis.avanueui.core.Orientation
import com.augmentalis.avanueui.dsl.AlertComponent
import com.augmentalis.avanueui.dsl.AlertSeverity
import com.augmentalis.avanueui.dsl.BadgeComponent
import com.augmentalis.avanueui.dsl.BadgeSize
import com.augmentalis.avanueui.dsl.BadgeVariant
import com.augmentalis.avanueui.dsl.ButtonComponent
import com.augmentalis.avanueui.dsl.ButtonScope
import com.augmentalis.avanueui.dsl.CardComponent
import com.augmentalis.avanueui.dsl.CheckboxComponent
import com.augmentalis.avanueui.dsl.ColumnComponent
import com.augmentalis.avanueui.dsl.ContainerComponent
import com.augmentalis.avanueui.dsl.DatePickerComponent
import com.augmentalis.avanueui.dsl.DialogActionStyle
import com.augmentalis.avanueui.dsl.DialogComponent
import com.augmentalis.avanueui.dsl.DropdownComponent
import com.augmentalis.avanueui.dsl.FileUploadComponent
import com.augmentalis.avanueui.dsl.IconComponent
import com.augmentalis.avanueui.dsl.ImageComponent
import com.augmentalis.avanueui.dsl.ImageScope
import com.augmentalis.avanueui.dsl.ProgressBarComponent
import com.augmentalis.avanueui.dsl.RadioComponent
import com.augmentalis.avanueui.dsl.RatingComponent
import com.augmentalis.avanueui.dsl.RowComponent
import com.augmentalis.avanueui.dsl.ScrollViewComponent
import com.augmentalis.avanueui.dsl.SearchBarComponent
import com.augmentalis.avanueui.dsl.SpinnerComponent
import com.augmentalis.avanueui.dsl.SpinnerSize
import com.augmentalis.avanueui.dsl.SwitchComponent
import com.augmentalis.avanueui.dsl.TextComponent
import com.augmentalis.avanueui.dsl.TextFieldComponent
import com.augmentalis.avanueui.dsl.TextScope
import com.augmentalis.avanueui.dsl.TimePickerComponent
import com.augmentalis.avanueui.dsl.ToastComponent
import com.augmentalis.avanueui.dsl.ToastSeverity
import com.augmentalis.avanueui.dsl.TooltipComponent
import com.augmentalis.avanueui.renderer.android.ComposeRenderer
import com.augmentalis.avanueui.renderer.android.IconResolver
import com.augmentalis.avanueui.renderer.android.ModifierConverter
import com.augmentalis.avanueui.renderer.android.toComposeColor
import com.augmentalis.avanueui.theme.AvanueTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * DSL Component Extensions
 *
 * Extension functions for rendering components created via the AvaUI { } DSL builder.
 * These handle com.augmentalis.avanueui.dsl.* types, which are distinct from
 * the com.augmentalis.avanueui.ui.core.* types handled by FoundationExtensions.
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

// ==================== Column ====================

/**
 * Renders DSL ColumnComponent as a Compose Column.
 * Maps DSL Arrangement/Alignment enums to Compose equivalents.
 */
@Composable
fun ColumnComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    val verticalArrangement = when (arrangement) {
        DslArrangement.Start -> Arrangement.Top
        DslArrangement.End -> Arrangement.Bottom
        DslArrangement.Center -> Arrangement.Center
        DslArrangement.SpaceBetween -> Arrangement.SpaceBetween
        DslArrangement.SpaceAround -> Arrangement.SpaceAround
        DslArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
    }

    val composeHorizontalAlignment = when (horizontalAlignment) {
        DslAlignment.CenterStart -> Alignment.Start
        DslAlignment.CenterEnd -> Alignment.End
        DslAlignment.Center -> Alignment.CenterHorizontally
        DslAlignment.TopCenter, DslAlignment.TopStart, DslAlignment.TopEnd -> Alignment.Start
        DslAlignment.BottomCenter, DslAlignment.BottomStart, DslAlignment.BottomEnd -> Alignment.Start
    }

    Column(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = composeHorizontalAlignment
    ) {
        children.forEach { child -> renderer.RenderComponent(child) }
    }
}

// ==================== Row ====================

/**
 * Renders DSL RowComponent as a Compose Row.
 * Maps DSL Arrangement/Alignment enums to Compose equivalents.
 */
@Composable
fun RowComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    val horizontalArrangement = when (arrangement) {
        DslArrangement.Start -> Arrangement.Start
        DslArrangement.End -> Arrangement.End
        DslArrangement.Center -> Arrangement.Center
        DslArrangement.SpaceBetween -> Arrangement.SpaceBetween
        DslArrangement.SpaceAround -> Arrangement.SpaceAround
        DslArrangement.SpaceEvenly -> Arrangement.SpaceEvenly
    }

    val composeVerticalAlignment = when (verticalAlignment) {
        DslAlignment.TopStart, DslAlignment.TopCenter, DslAlignment.TopEnd -> Alignment.Top
        DslAlignment.BottomStart, DslAlignment.BottomCenter, DslAlignment.BottomEnd -> Alignment.Bottom
        DslAlignment.CenterStart, DslAlignment.Center, DslAlignment.CenterEnd -> Alignment.CenterVertically
    }

    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = composeVerticalAlignment
    ) {
        children.forEach { child -> renderer.RenderComponent(child) }
    }
}

// ==================== Container ====================

/**
 * Renders DSL ContainerComponent as a Compose Box.
 * Alignment maps to contentAlignment.
 */
@Composable
fun ContainerComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    val contentAlignment = when (alignment) {
        DslAlignment.Center -> Alignment.Center
        DslAlignment.CenterStart -> Alignment.CenterStart
        DslAlignment.CenterEnd -> Alignment.CenterEnd
        DslAlignment.TopStart -> Alignment.TopStart
        DslAlignment.TopCenter -> Alignment.TopCenter
        DslAlignment.TopEnd -> Alignment.TopEnd
        DslAlignment.BottomStart -> Alignment.BottomStart
        DslAlignment.BottomCenter -> Alignment.BottomCenter
        DslAlignment.BottomEnd -> Alignment.BottomEnd
    }

    Box(modifier = modifier, contentAlignment = contentAlignment) {
        child?.let { renderer.RenderComponent(it) }
    }
}

// ==================== ScrollView ====================

/**
 * Renders DSL ScrollViewComponent as a scrollable Box.
 * Orientation.Horizontal uses horizontalScroll; Vertical uses verticalScroll.
 */
@Composable
fun ScrollViewComponent.RenderDsl(renderer: ComposeRenderer) {
    val baseModifier = dslModifierConverter.convert(modifiers)
    val scrollModifier = when (orientation) {
        Orientation.Horizontal -> baseModifier.horizontalScroll(rememberScrollState())
        Orientation.Vertical -> baseModifier.verticalScroll(rememberScrollState())
    }

    Box(modifier = scrollModifier) {
        child?.let { renderer.RenderComponent(it) }
    }
}

// ==================== Checkbox ====================

/**
 * Renders DSL CheckboxComponent as a Material3 Checkbox with a label Row.
 * Local state tracks checked value; onCheckedChange propagates changes.
 */
@Composable
fun CheckboxComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var currentChecked by remember { mutableStateOf(checked) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = currentChecked,
            onCheckedChange = { newValue ->
                currentChecked = newValue
                onCheckedChange?.invoke(newValue)
            },
            enabled = enabled
        )
        Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
        Text(
            text = label,
            color = if (enabled) AvanueTheme.colors.textPrimary else AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
        )
    }
}

// ==================== Switch ====================

/**
 * Renders DSL SwitchComponent as a Material3 Switch.
 */
@Composable
fun SwitchComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var currentChecked by remember { mutableStateOf(checked) }

    Switch(
        checked = currentChecked,
        onCheckedChange = { newValue ->
            currentChecked = newValue
            onCheckedChange?.invoke(newValue)
        },
        modifier = modifier,
        enabled = enabled
    )
}

// ==================== Icon ====================

/**
 * Renders DSL IconComponent as a Material3 Icon.
 * Resolves icon name via IconResolver; applies optional tint or defaults to AvanueTheme.colors.textPrimary.
 */
@Composable
fun IconComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    Icon(
        imageVector = IconResolver.resolve(name),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint?.toComposeColor() ?: AvanueTheme.colors.textPrimary
    )
}

// ==================== Radio ====================

/**
 * Renders DSL RadioComponent as a group of Material3 RadioButton rows.
 * Orientation.Vertical stacks options; Horizontal arranges them in a Row.
 */
@Composable
fun RadioComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var currentSelection by remember { mutableStateOf(selectedValue) }

    if (orientation == Orientation.Vertical) {
        Column(modifier = modifier) {
            options.forEach { option ->
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = option.value == currentSelection,
                            onClick = {
                                if (option.enabled) {
                                    currentSelection = option.value
                                    onValueChange?.invoke(option.value)
                                }
                            },
                            role = Role.RadioButton
                        )
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = option.value == currentSelection,
                        onClick = null,
                        enabled = option.enabled
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(8.dp))
                    Text(
                        text = option.label,
                        color = if (option.enabled) {
                            AvanueTheme.colors.textPrimary
                        } else {
                            AvanueTheme.colors.textPrimary.copy(alpha = 0.38f)
                        }
                    )
                }
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            options.forEach { option ->
                Row(
                    modifier = androidx.compose.ui.Modifier
                        .selectable(
                            selected = option.value == currentSelection,
                            onClick = {
                                if (option.enabled) {
                                    currentSelection = option.value
                                    onValueChange?.invoke(option.value)
                                }
                            },
                            role = Role.RadioButton
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = option.value == currentSelection,
                        onClick = null,
                        enabled = option.enabled
                    )
                    Spacer(modifier = androidx.compose.ui.Modifier.width(4.dp))
                    Text(option.label)
                }
            }
        }
    }
}

// ==================== Slider ====================

/**
 * Renders DSL SliderComponent as a Material3 Slider.
 * Optionally shows the current value label above when showLabel is true.
 */
@Composable
fun SliderComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var currentValue by remember { mutableStateOf(value) }

    Column(modifier = modifier) {
        if (showLabel) {
            Row(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    text = labelFormatter?.invoke(currentValue) ?: "%.1f".format(currentValue),
                    style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                    color = AvanueTheme.colors.textPrimary
                )
            }
        }
        Slider(
            value = currentValue,
            onValueChange = { newValue ->
                currentValue = newValue
                onValueChange?.invoke(newValue)
            },
            valueRange = valueRange,
            steps = steps
        )
    }
}

// ==================== Dropdown ====================

/**
 * Renders DSL DropdownComponent as a Material3 ExposedDropdownMenuBox.
 * Searchable flag enables typing to filter; non-searchable shows read-only field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var expanded by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("") }
    var currentSelection by remember { mutableStateOf(selectedValue) }

    val selectedLabel = options.find { it.value == currentSelection }?.label ?: placeholder

    val visibleOptions = if (searchable && filterText.isNotEmpty()) {
        options.filter { it.label.contains(filterText, ignoreCase = true) && !it.disabled }
    } else {
        options.filter { !it.disabled }
    }

    androidx.compose.material3.ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = if (searchable && expanded) filterText else selectedLabel,
            onValueChange = { if (searchable) filterText = it },
            readOnly = !searchable,
            placeholder = { Text(placeholder) },
            trailingIcon = {
                androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = androidx.compose.ui.Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        androidx.compose.material3.ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                filterText = ""
            }
        ) {
            visibleOptions.forEach { option ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        currentSelection = option.value
                        onValueChange?.invoke(option.value)
                        expanded = false
                        filterText = ""
                    },
                    leadingIcon = option.icon?.let { iconName ->
                        { Icon(imageVector = IconResolver.resolve(iconName), contentDescription = null) }
                    }
                )
            }
        }
    }
}

// ==================== DatePicker ====================

/**
 * Renders DSL DatePickerComponent as an OutlinedTextField trigger that opens
 * a Material3 DatePickerDialog. Respects minDate/maxDate constraints via
 * the selectableDates parameter.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var showDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat(dateFormat.ifEmpty { "MMM dd, yyyy" }, Locale.getDefault()) }

    val displayText = selectedDate?.let { dateFormatter.format(Date(it)) } ?: "Select date"

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        trailingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = "Select date")
        },
        modifier = modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate,
            yearRange = run {
                val minYear = minDate?.let {
                    java.util.Calendar.getInstance().apply { timeInMillis = it }.get(java.util.Calendar.YEAR)
                } ?: 1900
                val maxYear = maxDate?.let {
                    java.util.Calendar.getInstance().apply { timeInMillis = it }.get(java.util.Calendar.YEAR)
                } ?: 2100
                minYear..maxYear
            }
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { onDateChange?.invoke(it) }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ==================== TimePicker ====================

/**
 * Renders DSL TimePickerComponent as an OutlinedTextField trigger that opens
 * a Material3 TimePicker inside an AlertDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var showDialog by remember { mutableStateOf(false) }
    var currentHour by remember { mutableStateOf(hour) }
    var currentMinute by remember { mutableStateOf(minute) }

    val displayText = if (is24Hour) {
        "%02d:%02d".format(currentHour, currentMinute)
    } else {
        val amPm = if (currentHour < 12) "AM" else "PM"
        val displayHour = when {
            currentHour == 0 -> 12
            currentHour > 12 -> currentHour - 12
            else -> currentHour
        }
        "%d:%02d %s".format(displayHour, currentMinute, amPm)
    }

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        trailingIcon = {
            Icon(Icons.Default.Schedule, contentDescription = "Select time")
        },
        modifier = modifier.clickable { showDialog = true }
    )

    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = currentHour,
            initialMinute = currentMinute,
            is24Hour = is24Hour
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    currentHour = timePickerState.hour
                    currentMinute = timePickerState.minute
                    onTimeChange?.invoke(timePickerState.hour, timePickerState.minute)
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            },
            text = { TimePicker(state = timePickerState) }
        )
    }
}

// ==================== FileUpload ====================

/**
 * Renders DSL FileUploadComponent as a dashed-border upload area.
 * Actual file picking requires Activity integration via ActivityResultContracts;
 * this renders the UI affordance and wires the click handler slot.
 */
@Composable
fun FileUploadComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    Column(modifier = modifier) {
        Box(
            modifier = androidx.compose.ui.Modifier
                .fillMaxWidth()
                .height(120.dp)
                .border(
                    width = 2.dp,
                    color = AvanueTheme.colors.border,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable { /* File picker triggered externally via onFilesSelected callback registration */ },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload",
                    modifier = androidx.compose.ui.Modifier.size(32.dp),
                    tint = AvanueTheme.colors.primary
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
                Text(
                    text = placeholder,
                    color = AvanueTheme.colors.primary
                )
                if (accept.isNotEmpty()) {
                    Spacer(modifier = androidx.compose.ui.Modifier.height(4.dp))
                    Text(
                        text = accept.joinToString(", "),
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
                maxSize?.let { max ->
                    val maxMb = max / (1024L * 1024L)
                    Text(
                        text = "Max ${maxMb}MB",
                        style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                        color = AvanueTheme.colors.textSecondary
                    )
                }
            }
        }
    }
}

// ==================== SearchBar ====================

/**
 * Renders DSL SearchBarComponent as a Material3 OutlinedTextField with search
 * icon, optional clear button, and keyboard search action.
 */
@Composable
fun SearchBarComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var currentValue by remember { mutableStateOf(value) }

    OutlinedTextField(
        value = currentValue,
        onValueChange = { newValue ->
            currentValue = newValue
            onValueChange?.invoke(newValue)
        },
        placeholder = { Text(placeholder) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (currentValue.isNotEmpty() && showClearButton) {
                IconButton(onClick = {
                    currentValue = ""
                    onValueChange?.invoke("")
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { onSearch?.invoke(currentValue) }
        ),
        modifier = modifier.fillMaxWidth()
    )
}

// ==================== Rating ====================

/**
 * Renders DSL RatingComponent as a row of star icons.
 * Supports half-star display and click-to-rate when not readonly.
 */
@Composable
fun RatingComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)
    var currentValue by remember { mutableStateOf(value) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..maxRating) {
            val starIcon = when {
                i <= currentValue.toInt() -> Icons.Default.Star
                allowHalf && i - 0.5f <= currentValue -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }

            Icon(
                imageVector = starIcon,
                contentDescription = "$i star",
                tint = if (i <= currentValue) AvanueTheme.colors.primary else AvanueTheme.colors.border,
                modifier = androidx.compose.ui.Modifier
                    .size(24.dp)
                    .then(
                        if (!readonly) {
                            androidx.compose.ui.Modifier.clickable {
                                currentValue = i.toFloat()
                                onRatingChange?.invoke(i.toFloat())
                            }
                        } else {
                            androidx.compose.ui.Modifier
                        }
                    )
            )
        }
    }
}

// ==================== Dialog ====================

/**
 * Renders DSL DialogComponent as a Material3 AlertDialog.
 * Only shown when isOpen is true. Actions are mapped to styled TextButtons.
 */
@Composable
fun DialogComponent.RenderDsl(renderer: ComposeRenderer) {
    if (!isOpen) return

    AlertDialog(
        onDismissRequest = {
            if (dismissible) onDismiss?.invoke()
        },
        title = title?.let { titleText -> { Text(titleText) } },
        text = content?.let { comp -> { renderer.RenderComponent(comp) } },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                actions.forEach { action ->
                    when (action.style) {
                        DialogActionStyle.Primary -> Button(onClick = action.onClick) { Text(action.label) }
                        DialogActionStyle.Secondary -> FilledTonalButton(onClick = action.onClick) { Text(action.label) }
                        DialogActionStyle.Outlined -> OutlinedButton(onClick = action.onClick) { Text(action.label) }
                        DialogActionStyle.Text -> TextButton(onClick = action.onClick) { Text(action.label) }
                    }
                }
            }
        }
    )
}

// ==================== Toast ====================

/**
 * Renders DSL ToastComponent using a SnackbarHost anchored to a Box.
 * The toast auto-dismisses after the specified duration.
 * Severity is reflected in the container color via AvanueTheme.
 */
@Composable
fun ToastComponent.RenderDsl(renderer: ComposeRenderer) {
    val snackbarHostState = remember { SnackbarHostState() }

    val severityColor = when (severity) {
        ToastSeverity.Success -> AvanueTheme.colors.success
        ToastSeverity.Info -> AvanueTheme.colors.primary
        ToastSeverity.Warning -> AvanueTheme.colors.warning
        ToastSeverity.Error -> AvanueTheme.colors.error
    }

    LaunchedEffect(message) {
        snackbarHostState.showSnackbar(
            message = message,
            actionLabel = action?.label,
            duration = if (duration <= 2000L) SnackbarDuration.Short else SnackbarDuration.Long
        ).let { result ->
            if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                action?.onClick?.invoke()
            }
        }
    }

    Box {
        SnackbarHost(hostState = snackbarHostState) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = severityColor,
                contentColor = AvanueTheme.colors.textPrimary
            )
        }
    }
}

// ==================== Alert ====================

/**
 * Renders DSL AlertComponent as a colored Surface banner with title, message,
 * and an optional dismiss IconButton. Severity drives the background tint.
 */
@Composable
fun AlertComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    val backgroundColor = when (severity) {
        AlertSeverity.Success -> AvanueTheme.colors.success.copy(alpha = 0.12f)
        AlertSeverity.Info -> AvanueTheme.colors.primary.copy(alpha = 0.12f)
        AlertSeverity.Warning -> AvanueTheme.colors.warning.copy(alpha = 0.12f)
        AlertSeverity.Error -> AvanueTheme.colors.error.copy(alpha = 0.12f)
    }

    val accentColor = when (severity) {
        AlertSeverity.Success -> AvanueTheme.colors.success
        AlertSeverity.Info -> AvanueTheme.colors.primary
        AlertSeverity.Warning -> AvanueTheme.colors.warning
        AlertSeverity.Error -> AvanueTheme.colors.error
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = androidx.compose.ui.Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            icon?.let { iconName ->
                Icon(
                    imageVector = IconResolver.resolve(iconName),
                    contentDescription = null,
                    tint = accentColor,
                    modifier = androidx.compose.ui.Modifier.size(20.dp)
                )
            }
            Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    color = accentColor
                )
                Spacer(modifier = androidx.compose.ui.Modifier.height(2.dp))
                Text(
                    text = message,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = AvanueTheme.colors.textPrimary
                )
            }
            if (dismissible) {
                IconButton(
                    onClick = { onDismiss?.invoke() },
                    modifier = androidx.compose.ui.Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = "Dismiss", tint = accentColor)
                }
            }
        }
    }
}

// ==================== ProgressBar ====================

/**
 * Renders DSL ProgressBarComponent as a Material3 LinearProgressIndicator.
 * indeterminate=true shows an animated infinite progress bar.
 * showLabel=true renders the formatted value above the bar.
 */
@Composable
fun ProgressBarComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    Column(modifier = modifier) {
        if (showLabel && !indeterminate) {
            Text(
                text = labelFormatter?.invoke(value) ?: "${(value * 100).toInt()}%",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textPrimary,
                modifier = androidx.compose.ui.Modifier.padding(bottom = 4.dp)
            )
        }
        if (indeterminate) {
            LinearProgressIndicator(
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                color = AvanueTheme.colors.primary,
                trackColor = AvanueTheme.colors.primary.copy(alpha = 0.24f)
            )
        } else {
            LinearProgressIndicator(
                progress = { value },
                modifier = androidx.compose.ui.Modifier.fillMaxWidth(),
                color = AvanueTheme.colors.primary,
                trackColor = AvanueTheme.colors.primary.copy(alpha = 0.24f)
            )
        }
    }
}

// ==================== Spinner ====================

/**
 * Renders DSL SpinnerComponent as a Material3 CircularProgressIndicator.
 * Size maps to 24/40/56 dp. Optional label appears below.
 */
@Composable
fun SpinnerComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    val spinnerSize = when (size) {
        SpinnerSize.Small -> 24.dp
        SpinnerSize.Medium -> 40.dp
        SpinnerSize.Large -> 56.dp
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = androidx.compose.ui.Modifier.size(spinnerSize),
            color = AvanueTheme.colors.primary,
            trackColor = AvanueTheme.colors.primary.copy(alpha = 0.24f)
        )
        label?.let { labelText ->
            Spacer(modifier = androidx.compose.ui.Modifier.height(8.dp))
            Text(
                text = labelText,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                color = AvanueTheme.colors.textPrimary
            )
        }
    }
}

// ==================== Badge ====================

/**
 * Renders DSL BadgeComponent as a colored rounded pill.
 * Variant drives the background color; Size drives font and padding.
 */
@Composable
fun BadgeComponent.RenderDsl(renderer: ComposeRenderer) {
    val modifier = dslModifierConverter.convert(modifiers)

    val backgroundColor = when (variant) {
        BadgeVariant.Default -> AvanueTheme.colors.surface
        BadgeVariant.Primary -> AvanueTheme.colors.primary
        BadgeVariant.Secondary -> AvanueTheme.colors.secondary
        BadgeVariant.Success -> AvanueTheme.colors.success
        BadgeVariant.Warning -> AvanueTheme.colors.warning
        BadgeVariant.Error -> AvanueTheme.colors.error
    }

    val contentColor = when (variant) {
        BadgeVariant.Default -> AvanueTheme.colors.textPrimary
        else -> AvanueTheme.colors.onPrimary
    }

    val (fontSize, horizontalPad, verticalPad) = when (size) {
        BadgeSize.Small -> Triple(10.sp, 6.dp, 2.dp)
        BadgeSize.Medium -> Triple(12.sp, 8.dp, 3.dp)
        BadgeSize.Large -> Triple(14.sp, 10.dp, 4.dp)
    }

    Surface(
        modifier = modifier,
        color = backgroundColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = content,
            fontSize = fontSize,
            color = contentColor,
            modifier = androidx.compose.ui.Modifier.padding(horizontal = horizontalPad, vertical = verticalPad)
        )
    }
}

// ==================== Tooltip ====================

/**
 * Renders DSL TooltipComponent using Material3 PlainTooltip.
 * The child component is rendered as the anchor; the tooltip text appears on long-press.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipComponent.RenderDsl(renderer: ComposeRenderer) {
    val tooltipState = androidx.compose.material3.rememberTooltipState()

    androidx.compose.material3.TooltipBox(
        positionProvider = androidx.compose.material3.TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            androidx.compose.material3.PlainTooltip {
                Text(content)
            }
        },
        state = tooltipState
    ) {
        renderer.RenderComponent(child)
    }
}
