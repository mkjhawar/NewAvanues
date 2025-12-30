package com.augmentalis.avaelements.phase3

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Android Compose implementations for Phase 3 Input Components
 */

/**
 * Slider renderer for Android
 */
@Composable
fun RenderSlider(slider: Slider, modifier: Modifier = Modifier) {
    var value by remember { mutableStateOf(slider.value) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (slider.label != null) {
            Text(
                text = slider.label,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Slider(
                value = value,
                onValueChange = {
                    value = it
                    slider.onValueChange?.invoke(it)
                },
                valueRange = slider.min..slider.max,
                steps = if (slider.step > 0) ((slider.max - slider.min) / slider.step).toInt() - 1 else 0,
                enabled = slider.enabled,
                modifier = Modifier.weight(1f)
            )

            if (slider.showValue) {
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "%.1f".format(value),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * RangeSlider renderer for Android
 */
@Composable
fun RenderRangeSlider(rangeSlider: RangeSlider, modifier: Modifier = Modifier) {
    var range by remember { mutableStateOf(rangeSlider.valueStart..rangeSlider.valueEnd) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (rangeSlider.label != null) {
            Text(
                text = rangeSlider.label,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            RangeSlider(
                value = range,
                onValueChange = {
                    range = it
                    rangeSlider.onValuesChange?.invoke(it.start, it.endInclusive)
                },
                valueRange = rangeSlider.min..rangeSlider.max,
                steps = if (rangeSlider.step > 0) ((rangeSlider.max - rangeSlider.min) / rangeSlider.step).toInt() - 1 else 0,
                enabled = rangeSlider.enabled,
                modifier = Modifier.weight(1f)
            )

            if (rangeSlider.showValues) {
                Spacer(Modifier.width(16.dp))
                Text(
                    text = "%.1f - %.1f".format(range.start, range.endInclusive),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

/**
 * DatePicker renderer for Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDatePicker(datePicker: DatePicker, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(datePicker.selectedDate) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (datePicker.label != null) {
            Text(
                text = datePicker.label,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { showDialog = true },
            enabled = datePicker.enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.CalendarToday, contentDescription = "Calendar")
            Spacer(Modifier.width(8.dp))
            Text(
                text = selectedDate?.format(datePicker.format) ?: datePicker.placeholder
            )
        }
    }

    if (showDialog) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = Date.fromTimestamp(millis)
                        selectedDate = date
                        datePicker.onDateSelected?.invoke(date)
                    }
                    showDialog = false
                }) {
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
 * TimePicker renderer for Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderTimePicker(timePicker: TimePicker, modifier: Modifier = Modifier) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedTime by remember { mutableStateOf(timePicker.selectedTime) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (timePicker.label != null) {
            Text(
                text = timePicker.label,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        OutlinedButton(
            onClick = { showDialog = true },
            enabled = timePicker.enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.AccessTime, contentDescription = "Time")
            Spacer(Modifier.width(8.dp))
            Text(
                text = selectedTime?.format(timePicker.is24Hour) ?: timePicker.placeholder
            )
        }
    }

    if (showDialog) {
        val timePickerState = rememberTimePickerState()
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    val time = Time(
                        hour = timePickerState.hour,
                        minute = timePickerState.minute
                    )
                    selectedTime = time
                    timePicker.onTimeSelected?.invoke(time)
                    showDialog = false
                }) {
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
 * RadioButton renderer for Android
 */
@Composable
fun RenderRadioButton(radioButton: RadioButton, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        RadioButton(
            selected = radioButton.selected,
            onClick = { radioButton.onSelected?.invoke() },
            enabled = radioButton.enabled
        )

        if (radioButton.label != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                text = radioButton.label,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * RadioGroup renderer for Android
 */
@Composable
fun RenderRadioGroup(radioGroup: RadioGroup, modifier: Modifier = Modifier) {
    var selectedIndex by remember { mutableStateOf(radioGroup.selectedIndex) }

    val arrangement = if (radioGroup.orientation == Orientation.Vertical) {
        Arrangement.spacedBy(8.dp)
    } else {
        Arrangement.spacedBy(16.dp)
    }

    if (radioGroup.orientation == Orientation.Vertical) {
        Column(
            modifier = modifier,
            verticalArrangement = arrangement
        ) {
            radioGroup.options.forEachIndexed { index, option ->
                RenderRadioButton(
                    radioButton = RadioButton(
                        id = option.id,
                        selected = selectedIndex == index,
                        enabled = radioGroup.enabled && option.enabled,
                        label = option.label,
                        onSelected = {
                            selectedIndex = index
                            radioGroup.onSelectionChanged?.invoke(index)
                        }
                    )
                )
            }
        }
    } else {
        Row(
            modifier = modifier,
            horizontalArrangement = arrangement
        ) {
            radioGroup.options.forEachIndexed { index, option ->
                RenderRadioButton(
                    radioButton = RadioButton(
                        id = option.id,
                        selected = selectedIndex == index,
                        enabled = radioGroup.enabled && option.enabled,
                        label = option.label,
                        onSelected = {
                            selectedIndex = index
                            radioGroup.onSelectionChanged?.invoke(index)
                        }
                    )
                )
            }
        }
    }
}

/**
 * Dropdown renderer for Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderDropdown(dropdown: Dropdown, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf(dropdown.selectedIndex) }
    var searchQuery by remember { mutableStateOf("") }

    Column(modifier = modifier.fillMaxWidth()) {
        if (dropdown.label != null) {
            Text(
                text = dropdown.label,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { if (dropdown.enabled) expanded = !expanded }
        ) {
            OutlinedTextField(
                value = selectedIndex?.let { dropdown.options[it].label } ?: dropdown.placeholder,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = dropdown.enabled
            )

            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                if (dropdown.searchable) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    )
                }

                val filteredOptions = if (dropdown.searchable && searchQuery.isNotEmpty()) {
                    dropdown.options.filterIndexed { _, option ->
                        option.label.contains(searchQuery, ignoreCase = true)
                    }
                } else {
                    dropdown.options
                }

                filteredOptions.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            val originalIndex = dropdown.options.indexOf(option)
                            selectedIndex = originalIndex
                            dropdown.onSelectionChanged?.invoke(originalIndex)
                            expanded = false
                            searchQuery = ""
                        },
                        enabled = option.enabled
                    )
                }
            }
        }
    }
}

/**
 * Autocomplete renderer for Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderAutocomplete(autocomplete: Autocomplete, modifier: Modifier = Modifier) {
    var inputValue by remember { mutableStateOf(autocomplete.inputValue) }
    var expanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val filteredOptions = remember(inputValue) {
        if (inputValue.length >= autocomplete.minChars) {
            val filtered = autocomplete.filterFunction?.invoke(inputValue, autocomplete.options)
                ?: autocomplete.options.filter {
                    it.label.contains(inputValue, ignoreCase = true) ||
                            it.description?.contains(inputValue, ignoreCase = true) == true
                }
            filtered.take(autocomplete.maxResults)
        } else {
            emptyList()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (autocomplete.label != null) {
            Text(
                text = autocomplete.label,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        ExposedDropdownMenuBox(
            expanded = expanded && filteredOptions.isNotEmpty(),
            onExpandedChange = {}
        ) {
            OutlinedTextField(
                value = inputValue,
                onValueChange = { value ->
                    inputValue = value
                    autocomplete.onInputChanged?.invoke(value)
                    expanded = value.length >= autocomplete.minChars
                },
                placeholder = { Text(autocomplete.placeholder) },
                enabled = autocomplete.enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )

            if (expanded && filteredOptions.isNotEmpty()) {
                ExposedDropdownMenu(
                    expanded = true,
                    onDismissRequest = { expanded = false }
                ) {
                    filteredOptions.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Column {
                                    Text(option.label, style = MaterialTheme.typography.bodyMedium)
                                    if (option.description != null) {
                                        Text(
                                            option.description,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            onClick = {
                                inputValue = option.label
                                autocomplete.onOptionSelected?.invoke(option)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * SearchBar renderer for Android
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenderSearchBar(searchBar: SearchBar, modifier: Modifier = Modifier) {
    var query by remember { mutableStateOf(searchBar.query) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(query) {
        if (searchBar.debounceMs > 0) {
            delay(searchBar.debounceMs)
        }
        searchBar.onQueryChanged?.invoke(query)
    }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text(searchBar.placeholder) },
        leadingIcon = if (searchBar.showSearchIcon) {
            { Icon(Icons.Default.Search, contentDescription = "Search") }
        } else null,
        trailingIcon = if (searchBar.showClearButton && query.isNotEmpty()) {
            {
                IconButton(onClick = {
                    query = ""
                    searchBar.onClear?.invoke()
                }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                }
            }
        } else null,
        enabled = searchBar.enabled,
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(
            onSearch = {
                searchBar.onSearch?.invoke(query)
            }
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Rating renderer for Android
 */
@Composable
fun RenderRating(rating: Rating, modifier: Modifier = Modifier) {
    var currentRating by remember { mutableStateOf(rating.rating) }

    Column(modifier = modifier) {
        if (rating.label != null) {
            Text(
                text = rating.label,
                style = MaterialTheme.typography.labelMedium
            )
            Spacer(Modifier.height(8.dp))
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val iconSize = when (rating.size) {
                RatingSize.Small -> 20.dp
                RatingSize.Medium -> 28.dp
                RatingSize.Large -> 36.dp
            }

            for (i in 1..rating.maxRating) {
                val isFilled = if (rating.allowHalf) {
                    i <= currentRating.toInt() || (i == currentRating.toInt() + 1 && currentRating % 1 >= 0.5f)
                } else {
                    i <= currentRating
                }

                IconButton(
                    onClick = {
                        if (rating.enabled) {
                            currentRating = i.toFloat()
                            rating.onRatingChanged?.invoke(i.toFloat())
                        }
                    },
                    modifier = Modifier.size(iconSize)
                ) {
                    Icon(
                        imageVector = if (isFilled) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Star $i",
                        tint = if (isFilled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                    )
                }
            }

            if (rating.showValue) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (rating.allowHalf) "%.1f".format(currentRating) else "%.0f".format(currentRating),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
