package com.augmentalis.avaelements.renderers.android.mappers
import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.augmentalis.avaelements.core.Theme
import com.augmentalis.avaelements.components.phase3.input.*

// ============================================
// SLIDER COMPONENTS
// ============================================
@Composable
fun RenderSlider(c: Slider, theme: Theme) = Slider(
    value = c.value,
    onValueChange = c.onValueChange ?: {},
    valueRange = c.min..c.max,
    steps = if (c.step > 0f) ((c.max - c.min) / c.step).toInt() - 1 else 0,
    colors = SliderDefaults.colors(
        thumbColor = theme.colorScheme.primary.toCompose(),
        activeTrackColor = theme.colorScheme.primary.toCompose(),
        inactiveTrackColor = theme.colorScheme.surfaceVariant.toCompose()
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderRangeSlider(c: RangeSlider, theme: Theme) {
    var sliderPosition by remember { mutableStateOf(c.startValue..c.endValue) }

    androidx.compose.material3.RangeSlider(
        value = sliderPosition,
        onValueChange = { range ->
            sliderPosition = range
            c.onRangeChange?.invoke(range.start, range.endInclusive)
        },
        valueRange = c.min..c.max,
        colors = SliderDefaults.colors(
            thumbColor = theme.colorScheme.primary.toCompose(),
            activeTrackColor = theme.colorScheme.primary.toCompose(),
            inactiveTrackColor = theme.colorScheme.surfaceVariant.toCompose()
        )
    )
}

// ============================================
// DATE/TIME PICKERS
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDatePicker(c: DatePicker, theme: Theme) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = c.selectedDate ?: "",
        onValueChange = {},
        label = { Text(text = "Date") },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.colorScheme.primary.toCompose(),
            focusedLabelColor = theme.colorScheme.primary.toCompose()
        )
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    c.onDateChange?.invoke(datePickerState.selectedDateMillis?.toString() ?: "")
                }) { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(text = "Cancel") }
            }
        ) {
            androidx.compose.material3.DatePicker(state = datePickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTimePicker(c: TimePicker, theme: Theme) {
    var showDialog by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = c.selectedTime ?: "",
        onValueChange = {},
        label = { Text(text = "Time") },
        readOnly = true,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = theme.colorScheme.primary.toCompose(),
            focusedLabelColor = theme.colorScheme.primary.toCompose()
        )
    )

    if (showDialog) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    c.onTimeChange?.invoke("${timePickerState.hour}:${timePickerState.minute}")
                }) { Text(text = "OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text(text = "Cancel") }
            },
            text = {
                androidx.compose.material3.TimePicker(state = timePickerState)
            }
        )
    }
}

// ============================================
// RADIO COMPONENTS
// ============================================
@Composable
fun RenderRadioButton(c: RadioButton, theme: Theme) = androidx.compose.material3.RadioButton(
    selected = c.selected,
    onClick = c.onSelect,
    colors = RadioButtonDefaults.colors(
        selectedColor = theme.colorScheme.primary.toCompose()
    )
)

@Composable
fun RenderRadioGroup(c: RadioGroup, theme: Theme) {
    Column(modifier = Modifier.fillMaxWidth()) {
        c.options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { c.onSelectionChange?.invoke(value) }
                    .padding(8.dp)
            ) {
                androidx.compose.material3.RadioButton(
                    selected = value == c.selectedValue,
                    onClick = { c.onSelectionChange?.invoke(value) },
                    colors = RadioButtonDefaults.colors(
                        selectedColor = theme.colorScheme.primary.toCompose()
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = label,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }
    }
}

// ============================================
// DROPDOWN & AUTOCOMPLETE
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDropdown(c: Dropdown, theme: Theme) {
    var expanded by remember { mutableStateOf(false) }
    var selectedText by remember { mutableStateOf(c.selectedValue ?: c.placeholder) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            label = { Text(text = c.placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.colorScheme.primary.toCompose(),
                focusedLabelColor = theme.colorScheme.primary.toCompose()
            ),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            c.options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option) },
                    onClick = {
                        selectedText = option
                        expanded = false
                        c.onSelectionChange?.invoke(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderAutocomplete(c: Autocomplete, theme: Theme) {
    var filteredSuggestions by remember {
        mutableStateOf(c.suggestions.filter { it.contains(c.value, ignoreCase = true) })
    }
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded && filteredSuggestions.isNotEmpty(),
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = c.value,
            onValueChange = { newValue ->
                c.onValueChange?.invoke(newValue)
                filteredSuggestions = c.suggestions.filter { it.contains(newValue, ignoreCase = true) }
                expanded = true
            },
            label = { Text(text = "Search") },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = theme.colorScheme.primary.toCompose(),
                focusedLabelColor = theme.colorScheme.primary.toCompose()
            )
        )

        if (filteredSuggestions.isNotEmpty()) {
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                filteredSuggestions.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(text = suggestion) },
                        onClick = {
                            c.onValueChange?.invoke(suggestion)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

// ============================================
// FILE/IMAGE PICKERS
// ============================================
@Composable
fun RenderFileUpload(c: FileUpload, theme: Theme) {
    Button(
        onClick = { c.onFilesSelected?.invoke(emptyList()) },
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.colorScheme.primary.toCompose()
        )
    ) {
        Text(text = "Upload Files (${c.selectedFiles.size} selected)")
    }
}

@Composable
fun RenderImagePicker(c: ImagePicker, theme: Theme) {
    Button(
        onClick = { c.onImageSelected?.invoke("") },
        colors = ButtonDefaults.buttonColors(
            containerColor = theme.colorScheme.primary.toCompose()
        )
    ) {
        Text(text = c.selectedImage?.let { "Image: $it" } ?: "Pick Image")
    }
}

// ============================================
// RATING
// ============================================
@Composable
fun RenderRating(c: Rating, theme: Theme) {
    Row(modifier = Modifier.fillMaxWidth()) {
        repeat(c.maxRating) { index ->
            val filled = index < c.rating.toInt()
            IconButton(onClick = { c.onRatingChange?.invoke((index + 1).toFloat()) }) {
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Star ${index + 1}",
                    tint = if (filled)
                        theme.colorScheme.primary.toCompose()
                    else
                        theme.colorScheme.outline.toCompose()
                )
            }
        }
    }
}

// ============================================
// SEARCH BAR
// ============================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderSearchBar(c: SearchBar, theme: Theme) {
    androidx.compose.material3.SearchBar(
        query = c.query,
        onQueryChange = c.onQueryChange ?: {},
        onSearch = c.onSearch ?: {},
        active = false,
        onActiveChange = {},
        placeholder = { Text(text = c.placeholder) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search"
            )
        }
    ) {}
}
