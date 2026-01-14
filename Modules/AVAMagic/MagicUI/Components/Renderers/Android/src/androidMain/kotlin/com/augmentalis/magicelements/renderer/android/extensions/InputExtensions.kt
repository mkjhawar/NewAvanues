package com.augmentalis.magicelements.renderer.android.extensions

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.augmentalis.ideamagic.components.core.Orientation
import com.augmentalis.ideamagic.ui.core.form.*
import com.augmentalis.magicelements.renderer.android.ComposeRenderer
import com.augmentalis.magicelements.renderer.android.ModifierConverter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Input component extension functions for ComposeRenderer
 * Converts MagicUI input components to Material3 Compose components
 */

private val modifierConverter = ModifierConverter()

/**
 * Renders SliderComponent as Material3 Slider
 */
@Composable
fun ComposeRenderer.RenderSlider(component: SliderComponent) {
    Column(modifier = modifierConverter.convert(component.modifiers)) {
        if (component.label != null || component.showValue) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                component.label?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                if (component.showValue) {
                    Text(
                        text = "%.1f".format(component.value),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        val stepsCount = if (component.step > 0) {
            ((component.max - component.min) / component.step).toInt() - 1
        } else {
            0
        }

        Slider(
            value = component.value,
            onValueChange = { component.onValueChange?.invoke(it) },
            valueRange = component.min..component.max,
            steps = if (stepsCount > 0) stepsCount else 0,
            enabled = component.enabled
        )
    }
}

/**
 * Renders RangeSliderComponent as Material3 RangeSlider
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeRenderer.RenderRangeSlider(component: RangeSliderComponent) {
    Column(modifier = modifierConverter.convert(component.modifiers)) {
        if (component.label != null || component.showValues) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                component.label?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall)
                }
                if (component.showValues) {
                    Text(
                        text = "%.1f - %.1f".format(component.minValue, component.maxValue),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        val stepsCount = if (component.step > 0) {
            ((component.max - component.min) / component.step).toInt() - 1
        } else {
            0
        }

        RangeSlider(
            value = component.minValue..component.maxValue,
            onValueChange = { /* Component uses updateMin/updateMax methods */ },
            valueRange = component.min..component.max,
            steps = if (stepsCount > 0) stepsCount else 0,
            enabled = component.enabled
        )
    }
}

/**
 * Renders DatePickerComponent as Material3 DatePicker dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeRenderer.RenderDatePicker(component: DatePickerComponent) {
    var showDialog by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat(component.dateFormat, Locale.getDefault()) }

    val displayText = component.selectedDate?.let {
        dateFormatter.format(Date(it))
    } ?: "Select date"

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        enabled = component.enabled,
        label = component.label?.let { { Text(it) } },
        trailingIcon = {
            Icon(Icons.Default.DateRange, contentDescription = "Select date")
        },
        modifier = modifierConverter.convert(component.modifiers)
            .clickable(enabled = component.enabled) { showDialog = true }
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = component.selectedDate
        )

        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            component.onDateChange?.invoke(it)
                        }
                        showDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

/**
 * Renders TimePickerComponent as Material3 TimePicker dialog
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeRenderer.RenderTimePicker(component: TimePickerComponent) {
    var showDialog by remember { mutableStateOf(false) }

    val displayText = component.selectedTime?.format(component.is24Hour)
        ?: component.placeholder

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        enabled = component.enabled,
        label = component.label?.let { { Text(it) } },
        trailingIcon = {
            Icon(Icons.Default.Schedule, contentDescription = "Select time")
        },
        modifier = modifierConverter.convert(component.modifiers)
            .clickable(enabled = component.enabled) { showDialog = true }
    )

    if (showDialog) {
        val timePickerState = rememberTimePickerState(
            initialHour = component.selectedTime?.hour ?: 0,
            initialMinute = component.selectedTime?.minute ?: 0,
            is24Hour = component.is24Hour
        )

        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        component.onTimeSelected?.invoke(
                            Time(timePickerState.hour, timePickerState.minute)
                        )
                        showDialog = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}

/**
 * Renders DropdownComponent as Material3 ExposedDropdownMenu
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeRenderer.RenderDropdown(component: DropdownComponent) {
    var expanded by remember { mutableStateOf(false) }
    val selectedOption = component.options.find { it.value == component.selectedValue }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (component.enabled) expanded = !expanded },
        modifier = modifierConverter.convert(component.modifiers)
    ) {
        OutlinedTextField(
            value = selectedOption?.label ?: "",
            onValueChange = {},
            readOnly = true,
            enabled = component.enabled,
            label = component.label?.let { { Text(it) } },
            placeholder = { Text(component.placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            component.options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option.label) },
                    onClick = {
                        component.onValueChange?.invoke(option.value)
                        component.onSelectionChanged?.invoke(index)
                        expanded = false
                    },
                    enabled = option.enabled
                )
            }
        }
    }
}

/**
 * Renders RadioGroupComponent as Material3 RadioButtons
 */
@Composable
fun ComposeRenderer.RenderRadioGroup(component: RadioGroupComponent) {
    Column(modifier = modifierConverter.convert(component.modifiers)) {
        if (component.orientation == Orientation.Vertical) {
            Column {
                component.options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = value == component.selectedValue,
                                onClick = { /* No callback in component */ },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == component.selectedValue,
                            onClick = null,
                            enabled = component.enabled
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                component.options.forEach { (value, label) ->
                    Row(
                        modifier = Modifier.selectable(
                            selected = value == component.selectedValue,
                            onClick = { /* No callback in component */ },
                            role = Role.RadioButton
                        ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = value == component.selectedValue,
                            onClick = null,
                            enabled = component.enabled
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(label)
                    }
                }
            }
        }
    }
}

/**
 * Renders AutocompleteComponent as Material3 ExposedDropdownMenu with filtering
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ComposeRenderer.RenderAutocomplete(component: AutocompleteComponent) {
    var expanded by remember { mutableStateOf(false) }
    val suggestions = component.filteredSuggestions

    ExposedDropdownMenuBox(
        expanded = expanded && suggestions.isNotEmpty(),
        onExpandedChange = { expanded = it },
        modifier = modifierConverter.convert(component.modifiers)
    ) {
        OutlinedTextField(
            value = component.value,
            onValueChange = { /* Component handles this via updateValue */ },
            enabled = component.enabled,
            label = component.label?.let { { Text(it) } },
            placeholder = { Text(component.placeholder) },
            singleLine = true,
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded && suggestions.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            suggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(suggestion) },
                    onClick = {
                        // Component uses selectSuggestion method
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Renders FileUploadComponent as file upload UI
 * Note: Actual file picking requires Activity integration
 */
@Composable
fun ComposeRenderer.RenderFileUpload(component: FileUploadComponent) {
    Column(modifier = modifierConverter.convert(component.modifiers)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(8.dp)
                )
                .clickable(enabled = component.enabled) {
                    // File picker would be triggered here via Activity
                    // This requires integration with ActivityResultContracts
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CloudUpload,
                    contentDescription = "Upload",
                    modifier = Modifier.size(32.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = component.label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (component.accept.isNotEmpty()) {
                    Text(
                        text = component.accept.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                component.maxFileSize?.let { maxSize ->
                    val maxSizeMB = maxSize / (1024 * 1024)
                    Text(
                        text = "Max size: ${maxSizeMB}MB",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Show selected files
        if (component.files.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            component.files.forEach { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = file.name,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = file.formattedSize,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Renders SearchBarComponent as Material3 SearchBar
 */
@Composable
fun ComposeRenderer.RenderSearchBar(component: SearchBarComponent) {
    OutlinedTextField(
        value = component.value,
        onValueChange = { component.onValueChange?.invoke(it) },
        placeholder = { Text(component.placeholder) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = "Search")
        },
        trailingIcon = {
            if (component.value.isNotEmpty() && component.showClearButton) {
                IconButton(onClick = { component.onValueChange?.invoke("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = { component.onSearch?.invoke(component.value) }
        ),
        modifier = modifierConverter.convert(component.modifiers).fillMaxWidth()
    )
}

/**
 * Renders RatingComponent as star rating UI
 */
@Composable
fun ComposeRenderer.RenderRating(component: RatingComponent) {
    Row(
        modifier = modifierConverter.convert(component.modifiers),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..component.maxRating) {
            val icon = when {
                i <= component.value.toInt() -> Icons.Default.Star
                i - 0.5f <= component.value && component.allowHalf -> Icons.Default.StarHalf
                else -> Icons.Default.StarBorder
            }

            Icon(
                imageVector = icon,
                contentDescription = "Star $i",
                tint = if (i <= component.value) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
                modifier = Modifier
                    .size(24.dp)
                    .then(
                        if (!component.readonly) {
                            Modifier.clickable {
                                component.onRatingChange?.invoke(i.toFloat())
                            }
                        } else {
                            Modifier
                        }
                    )
            )
        }
    }
}
